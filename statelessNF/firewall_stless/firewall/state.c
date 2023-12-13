#include "state.h"

state_node *bucket[BUCKET_SIZE];
int cxtrackerid = 0;
int packet_number = 0;
int flow_number = 0;
extern struct timeval start_serialize;

/*
 * Returns a string with the source and 
 * destination Ips and ports
 *
 */

void showActionState(state_node *state){

    struct in6_addr ips;
    struct in6_addr ipd;

    ips.s6_addr32[0] = get_int_ip(state->src_ip);
    ipd.s6_addr32[0] = get_int_ip(state->dst_ip);

    //struct sockaddr_in sa;
    char src_str[INET_ADDRSTRLEN];

   // now get it back and print it
    inet_ntop(AF_INET, &(ips), src_str, INET_ADDRSTRLEN);
   
    //struct sockaddr_in sa;
    char dst_str[INET_ADDRSTRLEN];

   // now get it back and print it
    inet_ntop(AF_INET, &(ipd), dst_str, INET_ADDRSTRLEN);
    


    printf("---------------ConnState------------------\n");

    printf("src_ip %s\n", src_str);
    printf("ds_ip %s\n", dst_str);
    printf("s_port %u\n",state->src_prt);
    printf("d_port %u\n",state->dst_prt);
    //printf("cxid  %d\n",state->cxid);
    //printf("hash  %d\n",state->hash);
    printf("proto %u\n",state->proto);
   
    printf("last_pkt_time %lu\n", state->time);
    if(state->state == OPEN){
	printf("state: open\n");
	}
    else{
	printf("state:close\n");
	}
   
    printf("---------------actionState------------------\n");

}






int get_hash(packetinfo *pi){
    //printf("get_hash function\n");

    uint32_t int32_saddr = get_int_ip(pi->h_ip->saddr);
    uint32_t int32_daddr = get_int_ip(pi->h_ip->daddr);

	
    return CXT_HASH4(int32_saddr,pi->h_tcp->src_port, int32_daddr, pi->h_tcp->dst_port,pi->h_ip->proto);
}


uint32_t get_int_ip(u_char* addr){
    uint8_t int8_addr[4]; 
    int n;
    for(n = 0; n<4;n++){
	 int8_addr[n] = (uint8_t)addr[n];
    }
    uint16_t int16_addr1 =  (uint16_t)(int8_addr[1] << 8) | (uint16_t)(int8_addr[0]);
    uint16_t int16_addr2 =  (uint16_t)(int8_addr[3] << 8) | (uint16_t)(int8_addr[2]);
    uint32_t int32_addr = (uint32_t)(int16_addr2 << 16) | (uint32_t)(int16_addr1);

    return int32_addr;
}

//add to bucket
void append_to_list(state_node* sn){
    //printf("append to list function\n");
    int hash = sn->hash;
    state_node * head = bucket[hash];

    /* * New connections are pushed on to the head of bucket[s_hash] */
    sn->next = head;
    if (head != NULL) {
        // are we doubly linked?
        head->prev = sn;
    }
    bucket[hash] = sn;

}

void remove_state_node(state_node* sn, state_node ** bucket_ptr)
{
    //printf("remove-state-node function\n");
    state_node* prev = sn->prev;       /* OLDER connections */
    state_node* next = sn->next;       /* NEWER connections */

    if (prev == NULL) {
        // beginning of list
        *bucket_ptr = next;
        // not only entry
        if (next)
            next->prev = NULL;
    } else if (next == NULL) {
        // at end of list!
        prev->next = NULL;
    } else {
        // a node.
        prev->next = next;
        next->prev = prev;
    }

    /*
     * Free and set to NULL 
     */
    free(sn);
    sn = NULL;
}

void remove_hash_node(state_node* sn, int hash)
{
	//printf("remove-hash-node function\n");
	/* remove from the hash */
        if (sn->prev)
        	sn->prev->next = sn->next;
        if (sn->next)
        	sn->next->prev = sn->prev;
        state_node *tmp = sn;

        sn = sn->prev;

        remove_state_node(tmp, &bucket[hash]);
	if (sn == NULL) {
        	bucket[hash] = NULL;
        }

}


void update_time(state_node* sn){
    //printf("update_time function\n");
    sn->time = time(NULL);
}



void state_expunge_expired()
{
    //printf("state_expunge_expired function\n");
    time_t current = time(NULL);
    state_node* sn;
    int iter;

    int ended, expired = 0;
    time_t check_time = time(NULL);

    for (iter = 0; iter < BUCKET_SIZE; iter++) {
        sn = bucket[iter];
        while (sn != NULL) {
            if (difftime(sn->time, current) > EXPIRE_STATE) {
                remove_hash_node(sn, iter);
            } else {
                sn = sn->prev;
            }
        } // end while cxt
    } // end for buckets
}



state_node* create_node(packetinfo *pi){
    //printf("Creating Node\n");
    flow_number++;
    //printf("flow_number %d\n",flow_number);
    cxtrackerid++;

//+++
    stless_notify_flow_created();
//+++

    state_node* sn = (state_node*)malloc(sizeof(state_node));
    memcpy(sn->src_ip, pi->h_ip->saddr,4);
    sn->src_prt= pi->h_tcp->src_port;
    memcpy(sn->dst_ip, pi->h_ip->daddr,4);
    sn->dst_prt = pi->h_tcp->dst_port;
    sn->proto = pi->h_ip->proto;
    sn->time = time(NULL);
    sn->state = OPEN;

    uint32_t hash = pi->hash;
    sn->hash = hash;
   

    //printf("Added to hash\n");
    //append_to_list(sn);

    bucketSet(hash,sn);
    showActionState(sn);

    return sn;
}

//Checks the state of the node if available
//and checks with the rules is necessary.
//Returns the action the firewall should take 
//for the packet.
rule_type_t process_with_state(packetinfo *pi){
    //printf("process with state function\n");
    
    int hash = get_hash(pi);
    
        //printf("HASH %d\n",hash);
	pi->hash = hash;
        //SYN ACK Packet
	state_node* sn = NULL;
        //state_node* sn = find_state(pi);
	struct _state_node *tmpcxt = (struct _state_node *)malloc(sizeof(struct _state_node));

	if (bucketGet(hash, tmpcxt) == 1){
		sn = tmpcxt;
		//showActionState(sn);
	}
	else{
		printf("get state failed\n");
	}

   
 
	    if(sn == NULL){
   	    	sn = create_node(pi);

	    	char* sadr = ip_string(pi->h_ip->saddr);
            	char* dadr = ip_string(pi->h_ip->daddr);
            	rule_type_t rt=  get_firewall_action(rule_list, sadr, dadr, ntohs(pi->h_tcp->src_port), ntohs(pi->h_tcp->dst_port));    
            	free(sadr);
            	free(dadr);
	    	if(rt==PASS){
			//printf("update_state OPEN\n");
            		update_state(sn, OPEN);
            	}
	    	else{
			//printf("update_state CLOSED\n");
			update_state(sn, CLOSED);
	    	}
	    	return rt;
	    } 	

	/*gettimeofday(&find_middle, NULL);
        long sec = find_middle.tv_sec - start_serialize.tv_sec;
       	long usec = find_middle.tv_usec - start_serialize.tv_usec;
        long total = (sec * 1000 * 1000) + usec;		
        printf("STATS: PERFLOW: find middle TIME TO process packet = %ldus\n", total);	    
	  */  

	    state_t s = sn->state;
            //printf("STATE %i\n", s);
	    update_time(sn);
            if(s==OPEN){
                return PASS;
            }
	    else{
		return BLOCK;
	    }    
    
    //}
}



state_node* find_state(packetinfo *pi){

	//printf("find state\n");
	state_node* sn = bucket[pi->hash];
	char* sadr = ip_string(pi->h_ip->saddr);
        char* dadr = ip_string(pi->h_ip->daddr);
	u_short src_port = pi->h_tcp->src_port;
        u_short dst_port = pi->h_tcp->dst_port;

        while (sn != NULL) {
	   
	    if ((strcmp(ip_string(sn->src_ip), sadr) == 0)&& (strcmp(ip_string(sn->dst_ip), dadr) == 0) 
			&& (sn->src_prt == src_port) && (sn->dst_prt == dst_port)){
		return sn;
		break;
            }else if((strcmp(ip_string(sn->src_ip), dadr) == 0)&& (strcmp(ip_string(sn->dst_ip), sadr) == 0) 
			&& (sn->src_prt == dst_port) && (sn->dst_prt == src_port)){
		return sn;
		break;
	    }
           sn = sn->prev;
            
        } 
	return NULL;
}


void update_state(state_node* sn,state_t state ){
    //printf("update state\n");
    //if the ip string is in our state hash
    sn->state = state;
    update_time(sn);
}


