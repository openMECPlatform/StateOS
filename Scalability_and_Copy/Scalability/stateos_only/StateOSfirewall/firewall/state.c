#include "state.h"

state_node *bucket[BUCKET_SIZE];
int cxtrackerid = 0;
int packet_number = 0;
int flow_number = 0;
extern struct timeval start_serialize;

extern pthread_mutex_t ActionEntryLock;
static pthread_t action_thread;

int local_action_put_perflow(ShareState* recv_state){
	
    printf("receive action state, begin to putPerFlow\n");
    pthread_mutex_lock(&ActionEntryLock);
    state_node *state = NULL;
    state =(state_node *)malloc(sizeof(state_node));

    int m,n;
    for(m = 0; m<4;m++){
	state->src_ip[3-m] = (recv_state->s_ip)>>(24-m*8);
	}
    for(n = 0; n<4;n++){
	state->dst_ip[3-n] = (recv_state->d_ip)>>(24-n*8);
	}

    state->time = recv_state->last_pkt_time;
    uint32_t hash = recv_state->hash;
    state->hash = hash;
    state->state = recv_state->fwstate;
    
    state->src_prt = recv_state->s_port;
    state->dst_prt = recv_state->d_port;
    uint32_t cxid = recv_state->cxid;
    state->cxid = cxid; 

    //set a right number of cxtrackerid
    if(cxid > cxtrackerid){
	cxtrackerid = cxid;
    }
   
//+++
    connac_notify_flow_created();
//+++
    state_node *head = bucket[hash];
    // Add to linked list
    state->prev = NULL;
    if (head != NULL)
    {
        head->prev = state;
        state->next = head;
    }
    else
    {state->next = NULL; }
    bucket[hash] = state;
    //showPutActionState();
    pthread_mutex_unlock(&ActionEntryLock);
    return 1;
}




int local_action_get_one_perflow(state_node *state){
        printf("local get one action per flow\n");	
        pthread_mutex_lock(&ActionEntryLock);

	ShareState *share_state = (ShareState *)malloc(sizeof(ShareState));;
	share_state__init(share_state);

	//lack proto element
	share_state->has_s_ip = 1;
	share_state->s_ip = get_int_ip(state->src_ip);
	share_state->has_d_ip = 1;
	share_state->d_ip = get_int_ip(state->dst_ip);
	share_state->has_s_port = 1;
        share_state->s_port = state->src_prt;
	share_state->has_d_port = 1;
	share_state->d_port = state->dst_prt;
	int hash = state->hash;			
	share_state->has_hash=1;
	share_state->hash = hash;
	int cxid = state->cxid;
	share_state->has_cxid=1;
	share_state->cxid = cxid;

	share_state->has_last_pkt_time=1;
	share_state->last_pkt_time = state->time;
	share_state->has_fwstate=1;
	share_state->fwstate = state->state;		
		
	MyActionMessage mes;
        my_action_message__init(&mes);	
        mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionShareStateType;
        mes.message_case = MY_ACTION_MESSAGE__MESSAGE_SHARE_STATE;
        mes.sharestate = share_state;
 
        int len = my_action_message__get_packed_size(&mes);
        //printf("size of Perflow : %u\n", len);
        void *buf = malloc(len);
        my_action_message__pack(&mes, buf);

	int result = action_send_perflow(buf, len);    
       	 if(result < 0){
		return -1;
	}

	printf("local get one action per flow ---send successful\n");
		
	pthread_mutex_unlock(&ActionEntryLock);
	
	return 1;
}

static void *action_sender(void *arg){
	printf("start a action sender\n");
	state_node *state = (state_node*)arg;
	int send_action = local_action_get_one_perflow(state);
	if(send_action < 0){
		printf("send failed");
	}

}

int local_action_get_perflow(Key key){
     printf("start action get perflow\n");
     printf("local  key.dl_type %x\n", key.dl_type);
     printf("key.nw_proto %u\n",key.nw_proto);
 
    int count = 0;
    int h = 0;
    for (h = 0; h < BUCKET_SIZE; h++)
    {        
        pthread_mutex_lock(&ActionEntryLock);
        state_node *state = bucket[h];
        while (state != NULL)
        {
 	     // Check nw_proto
	     if ((!(key.wildcards & WILDCARD_NW_PROTO)) &&((state->proto) != key.nw_proto))
			{
		                state = state->next;
		                continue;
		        }

	     int err;
	     //create a thread to send action state
	     if((err = pthread_create(&action_thread, NULL, action_sender, (void*)state))!=0)
             {
                 perror("pthread_create error");
             }
	     count++;		        
           // Move on to next connection
           state = state->next;
        }
        pthread_mutex_unlock(&ActionEntryLock);
    }
    return count;

}
		



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
    printf("hash  %d\n",state->hash);
    printf("proto %u\n",state->proto);
   
    printf("last_pkt_time %lu\n", state->time);
    printf("state: %i\n",state->state);
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
            
            action[count_action] = state->hash;
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
    connac_notify_flow_created();
//+++

    state_node* sn = (state_node*)malloc(sizeof(state_node));
    memcpy(sn->src_ip, pi->h_ip->saddr,4);
    sn->src_prt= pi->h_tcp->src_port;
    memcpy(sn->dst_ip, pi->h_ip->daddr,4);
    sn->dst_prt = pi->h_tcp->dst_port;
    sn->proto = pi->h_ip->proto;
    sn->time = time(NULL);
    sn->state = OPEN;
    sn->hash = pi->hash;
   

    //printf("Added to hash\n");
    append_to_list(sn);

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





