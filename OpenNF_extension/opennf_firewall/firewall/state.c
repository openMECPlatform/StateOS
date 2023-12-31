#include "state.h"

state_node *bucket[BUCKET_SIZE];
int cxtrackerid = 0;
int packet_number = 0;
int flow_number = 0;
extern struct timeval start_serialize;

extern pthread_mutex_t ConnEntryLock;
extern pthread_mutex_t AssetEntryLock;
long overall_pserz_time = 0;
long overall_pdeserz_time = 0;
long overall_pstate_size = 0;
extern ser_tra_t *state_tra;

/*
 * Returns a string with the source and 
 * destination Ips and ports
 *
 */


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
void append_to_list(state_node* sn,uint32_t hash){
    //printf("append to list function\n");
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
    sdmbn_notify_flow_created();
//+++

    state_node* sn = (state_node*)malloc(sizeof(state_node));
    memcpy(sn->src_ip, pi->h_ip->saddr,4);
    sn->src_prt= pi->h_tcp->src_port;
    memcpy(sn->dst_ip, pi->h_ip->daddr,4);
    sn->dst_prt = pi->h_tcp->dst_port;
    sn->proto = pi->h_ip->proto;
    sn->time = time(NULL);
    sn->state = OPEN;
    sn->cxid = cxtrackerid;
   

    //printf("Added to hash\n");
    append_to_list(sn,pi->hash);

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
        state_node* sn = find_state(pi);
            
 
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


///// SDMBN Local Perflow State Handlers /////////////////////////////////////
//+++
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
    printf("cxid  %d\n",state->cxid);
    printf("proto %u\n",state->proto);
   
    printf("last_pkt_time %lu\n", state->time);
    printf("state: %i\n",state->state);
    printf("cxid %u\n",state->cxid);
    printf("gotten %u",state->gotten);
    printf("---------------actionState------------------\n");

}

void showAllState(){
 int h;
 uint64_t action[100];
 memset(action, 0, sizeof(action));
 
 int count_action =0;

 for (h = 0; h < BUCKET_SIZE; h++)
    {        
        state_node *state = bucket[h];
         
	while (state != NULL)
        {     
            printf("-----------action---h%d------------\n",h);
	    showActionState(state); 
            
            action[count_action] = h;
            count_action++;     
            // Move on to next connection
            state = state->next;
        }             
    }
    printf("\n");
    int n;
    
    printf("count_action%d\n",count_action);
    printf("------------------------------------\n");
    
    for(n = 0; n<= count_action; n++){
	printf("%lu,",action[n]);
    }


}

int local_get_perflow(PerflowKey *key, int id, int raiseEvents)
{
    if (NULL == key)
    { return -1; }

    int count = 0;
    int h = 0;
    for (h = 0; h < BUCKET_SIZE; h++)
    {
        pthread_mutex_lock(&ConnEntryLock);
	state_node *state = bucket[h];
	

        while (state != NULL)
        {

	    serialize_node * curr = NULL;
            curr =(serialize_node *)malloc(sizeof(serialize_node));

            curr->src_ip= get_int_ip(state->src_ip);
            curr->dst_ip = get_int_ip(state->dst_ip);
            curr->src_prt = state->src_prt;
            curr->dst_prt = state->dst_prt;
            curr->proto = state->proto;
            //curr->hash =   state->hash ;
            curr->time = state->time;
	    curr->cxid = state->cxid;
            if(state->state == OPEN){
		curr->state = 0;
	    }else{
		curr->state = 1;
	    }

            // Check nw_proto
            if (!(key->wildcards & WILDCARD_NW_PROTO) &&
                    curr->proto != key->nw_proto)
            {
                state = state->next;
                continue;
            }

            // Check tp_src
            if (!(key->wildcards & WILDCARD_TP_SRC) &&
                !(curr->src_prt == key->tp_src ||
                    (key->tp_flip && curr->dst_prt == key->tp_src)))
            {
                state = state->next;
                continue;
            }

            // Check tp_dst
            if (!(key->wildcards & WILDCARD_TP_DST) &&
                !(curr->dst_prt == key->tp_dst ||
                    (key->tp_flip && curr->src_prt == key->tp_dst)))
            {
                state = state->next;
                continue;
            }

            int nw_src_mask = 0xFFFFFFFF;
            if (!(key->wildcards & WILDCARD_NW_SRC_MASK))
            { nw_src_mask = nw_src_mask << (32 - key->nw_src_mask); }

            // Check nw_src
            // ugly hack :(
            // the way we do ip4/6 is DIRTY
            if (!(key->wildcards & WILDCARD_NW_SRC) &&
                    (nw_src_mask & curr->src_ip) != key->nw_src)
            {
                state = state->next;
                continue;
            }

            int nw_dst_mask = 0xFFFFFFFF;
            if (!(key->wildcards & WILDCARD_NW_DST_MASK))
            { nw_dst_mask = nw_dst_mask << (32 - key->nw_dst_mask); }

            // Check nw_dst
            // ugly hack :(
            // the way we do ip4/6 is DIRTY
            if (!(key->wildcards & WILDCARD_NW_DST) &&
                    (nw_dst_mask & curr->dst_ip) != key->nw_dst)
            {
                state = state->next;
                continue;
            }

            // Mark connection as returned with this get call 
            if (raiseEvents)
            	curr->gotten = id;
            else
            	curr->gotten = -1;

      
            // Prepare to send perflow state
            int hashkey = curr->cxid;
                     
            // Serialize conn and asset structure into a single character 
            // stream.
			struct timeval start_serialize, end_serialize;
   			gettimeofday(&start_serialize, NULL);
            
			//char *state = (char *)serialize_conn_asset(curr, id); 
			char *send_state = ser_ialize(state_tra, "serialize_node", curr, NULL, 0);   			

			gettimeofday(&end_serialize, NULL);
			long sec = end_serialize.tv_sec - start_serialize.tv_sec;
			long usec = end_serialize.tv_usec - start_serialize.tv_usec;
			long total = (sec * 1000 * 1000) + usec;
			overall_pserz_time += total;
			overall_pstate_size	+= strlen(send_state);
			printf("STATS: PERFLOW STATE SIZE CURRENT = %zu\n", strlen(send_state));
			printf("STATS: PERFLOW STATE SIZE OVERALL = %zu\n", overall_pstate_size);
			printf("STATS: PERFLOW: TIME TO SERIALIZE CURRENT = %ldus\n", total);
			printf("STATS: PERFLOW: TIME TO SERIALIZE OVERALL = %ldus\n", overall_pserz_time);
            SERIALIZE_PRINT("serializing connection struct with multi flow %s",send_state);
 
            if (NULL == send_state)
            { continue; }

            // Construct key
            PerflowKey connkey;
	    //this firewall only support to process IPV4 packets
            //if (AF_INET == curr->af)
            //{
                connkey.nw_src =  curr->src_ip;
                connkey.nw_src_mask = 32;
                connkey.nw_dst = curr->dst_ip;
                connkey.nw_dst_mask = 32;
            //}
            //else
            //{ /* FIXME: Handle IPv6 */ }
            connkey.tp_src = curr->src_prt;
            connkey.tp_dst = curr->dst_prt;
            //connkey.dl_type = curr->hw_proto;
            connkey.nw_proto = curr->proto;
            connkey.wildcards = WILDCARD_NONE;

            // Send perflow state
            int result = sdmbn_send_perflow(id, &connkey, send_state, hashkey, 
                    count);
            if (result < 0)
            { }

            // Increment count
            count++;

            // Clean-up
            free(send_state);

            // Move on to next connection
            state = state->next;
        }
        pthread_mutex_unlock(&ConnEntryLock);
    }

    return count;
}

int local_put_perflow(int hashkey, PerflowKey *key, char *state)
{
    if (NULL == key || NULL == state)
    { return -1; }

    serialize_node *serialize_state = NULL;
    SERIALIZE_PRINT("deserializing connection struct with multi flow state \n%s", state);

	struct timeval start_deserialize, end_deserialize;
   	gettimeofday(&start_deserialize, NULL);
   			
    
        serialize_state = ser_parse(state_tra, "serialize_node", state, NULL);
	
	gettimeofday(&end_deserialize, NULL);
	long sec = end_deserialize.tv_sec - start_deserialize.tv_sec;
	long usec = end_deserialize.tv_usec - start_deserialize.tv_usec;
	long total = (sec * 1000 * 1000) + usec;
	overall_pdeserz_time += total;
	printf("STATS: PERFLOW: TIME TO DESERIALIZE CURRENT = %ldus\n", total);
	printf("STATS: PERFLOW: TIME TO DESERIALIZE OVERALL = %ldus\n", overall_pdeserz_time);

   

   state_node * cxt = NULL;
   cxt =(state_node *)malloc(sizeof(state_node));

   int m,n;
   for(m = 0; m<4;m++){
	cxt->src_ip[3-m] = (serialize_state->src_ip)>>(24-m*8);
	}
    for(n = 0; n<4;n++){
	cxt->dst_ip[3-n] = (serialize_state->dst_ip)>>(24-n*8);
	}

    cxt->time = serialize_state->time;
    //cxt->hash = serialize_state->hash;
    if(serialize_state->state == 0){
	 cxt->state = OPEN;
	}
    else{
	printf("serialize state = 1");
	cxt->state = CLOSED;
	}
    cxt->proto = serialize_state->proto;
    cxt->src_prt = serialize_state->src_prt;
    cxt->dst_prt = serialize_state->dst_prt; 
    cxt->cxid = serialize_state->cxid;

    
    // Clear any mark of connection being gotten
    cxt->gotten = 0;

    struct in6_addr ips;
    struct in6_addr ipd;

    int af = AF_INET; //TODO Add support for IPv6
    state_node *head = NULL;
    uint32_t hash;

    //if (AF_INET == af)
    //{
        // ugly hack :(
        // the way we do ip4/6 is DIRTY
        ips.s6_addr32[0] = key->nw_src;
        ipd.s6_addr32[0] = key->nw_dst;
    //}
    //else
    //{ return -1; }

    // find the right connection bucket
    //if (af == AF_INET)
    //{
        hash = CXT_HASH4(IP4ADDR(&ips), IP4ADDR(&ipd), key->tp_src,
                key->tp_dst, key->nw_proto);
    //}
    /*else if (af == AF_INET6) 
    { hash = CXT_HASH6(ip_src,ip_dst,src_port,dst_port,pi->proto); }*/
    //else
    //{ return -1; }

    pthread_mutex_lock(&ConnEntryLock);
    head = bucket[hash];

    // allocating a new connection ID
    // TODO add locks
    cxtrackerid++;
    cxt->cxid = cxtrackerid;

    // Add to linked list
    cxt->prev = NULL;
    if (head != NULL)
    {
        head->prev = cxt;
        cxt->next = head;
    }
    else
    { cxt->next = NULL; }
    bucket[hash] = cxt;
    pthread_mutex_unlock(&ConnEntryLock);

    return 1;
}



