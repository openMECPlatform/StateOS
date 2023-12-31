#include "state.h"


connState *conn_bucket[BUCKET_SIZE];
actionState *action_bucket[BUCKET_SIZE];
uint32_t *cxid_bucket[BUCKET_SIZE];
//int packet_number = 0;
int cxtrackerid = 0;
//int action_flow = 0;
extern int new_packet;
extern int wait;
//extern cacheState* cache_state;
extern pthread_mutex_t ConnEntryLock;
extern pthread_mutex_t ActionEntryLock;

static pthread_t conn_thread;
static pthread_t action_thread;
int find_action = 0;

extern struct timeval start_serialize;

int local_unlock_packet(){
	printf("unlock the packet\n");
	new_packet = 0;
}


int local_conn_put_perflow(ConnState* recv_state){
	
    printf("receive conn state, begin to putPerFlow\n");
    new_packet = 1;
    wait = 1;
    pthread_mutex_lock(&ConnEntryLock);
    //connState *conn_state = NULL;
    connState *conn_state =(connState*)malloc(sizeof(connState));

    int m,n;
    for(m = 0; m<4;m++){
	conn_state->src_ip[3-m] = (recv_state->s_ip)>>(24-m*8);
	}
    for(n = 0; n<4;n++){
	conn_state->dst_ip[3-n] = (recv_state->d_ip)>>(24-n*8);
	}
    
    conn_state->src_prt = recv_state->s_port;
    conn_state->dst_prt = recv_state->d_port;
    conn_state->proto = recv_state->proto;

    uint32_t cxid = recv_state->cxid;
    conn_state->cxid = cxid;
   

    //set a right number of cxtrackerid
    if(cxid > cxtrackerid){
	cxtrackerid = cxid;
    }
   
    uint32_t hash = recv_state->hash;
    conn_state->hash = hash;

    connState* head = conn_bucket[hash];

    // Add to linked list
    conn_state->prev = NULL;
    if (head != NULL)
    {
        head->prev = conn_state;
        conn_state->next = head;
    }
    else
    { 
	conn_state->next = NULL; 
    }
    conn_bucket[hash] = conn_state;


//find hash more quickly
    cxid_bucket[cxid] = hash;
    //showPutConnState();
//+++
    connac_notify_flow_created();


    //printf("\n--------------check first time--------------------\n");
    //showAllCxid();
    pthread_mutex_unlock(&ConnEntryLock);
  
  return 1;

}



int local_action_put_perflow(ActionState* recv_state){
	
    printf("receive action state, begin to putPerFlow\n");
    pthread_mutex_lock(&ActionEntryLock);
    actionState *action_state = NULL;
    action_state =(actionState*)malloc(sizeof(actionState));

    action_state->time = recv_state->last_pkt_time;
    action_state->cxid = recv_state->cxid;
    uint32_t hash = recv_state->hash;
    action_state->hash = hash;
    action_state->state = recv_state->fwstate;

    actionState* head = action_bucket[hash];

    // Add to linked list
    action_state->prev = NULL;
    if (head != NULL)
    {
        head->prev = action_state;
        action_state->next = head;
    }
    else
    { action_state->next = NULL; }
    action_bucket[hash] = action_state;
    //showPutActionState();
    pthread_mutex_unlock(&ActionEntryLock);
    
  
    return 1;
}



int local_conn_get_one_perflow(connState* conn_state){	
	printf("local get conn one per flow\n");
	ConnState* conn_perflow = (ConnState*)malloc(sizeof(ConnState));
	conn_state__init(conn_perflow);

	//lack proto element
	conn_perflow->has_s_ip = 1;
	conn_perflow->s_ip = get_int_ip(conn_state->src_ip);
	conn_perflow->has_d_ip = 1;
	conn_perflow->d_ip = get_int_ip(conn_state->dst_ip);
	conn_perflow->has_s_port = 1;
        conn_perflow->s_port = conn_state->src_prt;
	conn_perflow->has_d_port = 1;
	conn_perflow->d_port = conn_state->dst_prt;
	int hash = conn_state->hash;			
	conn_perflow->has_hash=1;
	conn_perflow->hash = hash;
	int cxid = conn_state->cxid;
	conn_perflow->has_cxid=1;
	conn_perflow->cxid = cxid;


	//printf("local get conn one per flow------middle\n");
        MyConnMessage mes;
        my_conn_message__init(&mes);
   
        mes.data_type = MY_CONN_MESSAGE__DATA_TYPE__ConnStateType;
        mes.message_case = MY_CONN_MESSAGE__MESSAGE_CONN_STATE;
        mes.connstate =conn_perflow;
 
        int len = my_conn_message__get_packed_size(&mes);
        printf("size of Perflow : %u\n", len);
        void *buf = malloc(len);
        my_conn_message__pack(&mes, buf);

	int result = conn_send_perflow(buf, len);    
       	if(result < 0){
			return -1;
	}
	printf("local get one conn per flow---send successful\n");
	free(buf);
	free(conn_perflow);
			
}



int local_action_get_one_perflow(Match *match){
        printf("local get one action per flow\n");	
        pthread_mutex_lock(&ActionEntryLock);
	int hash = match->hash;
 	int cxid = match->cxid;
	actionState *action_state = action_bucket[hash];
	while(action_state != NULL){
		//printf("local get one action per flow ---while find the right action\n");
		if(action_state->cxid != cxid){
			printf("get one action cxid middle\n ");
			action_state = action_state->next;
			continue;
		}

		//printf("local get one action per flow ---find the action\n");
		//showActionState(action_state);
		ActionState* action_perflow = (ActionState*)malloc(sizeof(ActionState));;
		action_state__init(action_perflow);

        	
		action_perflow->has_last_pkt_time=1;
		action_perflow->last_pkt_time = action_state->time;
		action_perflow->has_fwstate=1;
		action_perflow->fwstate = action_state->state;		

		action_perflow->has_cxid=1;
		action_perflow->cxid = action_state->cxid;		
		action_perflow->has_hash = 1;
		action_perflow->hash = action_state->hash;
		
		MyActionMessage mes;
        	my_action_message__init(&mes);	
        	mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionStateType;
        	mes.message_case = MY_ACTION_MESSAGE__MESSAGE_ACTION_STATE;
        	mes.actionstate = action_perflow;
 
        	int len = my_action_message__get_packed_size(&mes);
        	//printf("size of Perflow : %u\n", len);
        	void *buf = malloc(len);
        	my_action_message__pack(&mes, buf);

		int result = action_send_perflow(buf, len);    
       		 if(result < 0){
			return -1;
		}
		action_state = action_state->next;
	        printf("local get one action per flow ---send successful\n");
		
		break;
		
	}
	//printf("action end----------------------\n");
	pthread_mutex_unlock(&ActionEntryLock);
	//printf("local get one action per flow ---unlock\n");
	free(match);
	return 1;
}





static void *conn_sender(void *arg){
	printf("start a conn sender\n");
	
	connState* conn_state = (connState*)arg;
	int send_conn = local_conn_get_one_perflow(conn_state);
	if(send_conn < 0){
		printf("send failed");
	}

	
}

static void *action_sender(void *arg){
	printf("start a action sender\n");
	Match *match = (Match*)arg;
	
	printf("match hash %d\n", match->hash);
	printf("match cxid %d\n", match->cxid);
	int send_action = local_action_get_one_perflow(match);
	if(send_action < 0){
		printf("send failed");
	}

}

int local_conn_get_perflow(Key key){
     printf("start conn get perflow\n");
     //printf("local  key.dl_type %x\n", key.dl_type);
     //printf("key.nw_proto %u\n",key.nw_proto);
 
    int count = 0;
    int h = 0;
    for (h = 0; h < BUCKET_SIZE; h++)
    {        
        pthread_mutex_lock(&ConnEntryLock);
        connState *conn_state = conn_bucket[h];
        while (conn_state != NULL)
        {
		
		            // Check nw_proto
		if ((!(key.wildcards & WILDCARD_NW_PROTO)) &&((conn_state->proto) != key.nw_proto))
			{
		                conn_state = conn_state->next;
		                continue;
		        }
	

		
	    int err;
	     if((err = pthread_create(&conn_thread, NULL, conn_sender, (void*)conn_state))!=0)
             {
             	perror("pthread_create error");
             }
	     

	     
	     Match* match = (Match*)malloc(sizeof(Match));
	     match->hash = conn_state->hash;
	     match->cxid = conn_state->cxid;
		
	     //create a thread to send action state
	     if((err = pthread_create(&action_thread, NULL, action_sender, (void*)match))!=0)
             {
                 perror("pthread_create error");
             }
		
	 	//return 1;
	   //}
		count++;	
	
	        
           // Move on to next connection
           conn_state = conn_state->next;
        }
        pthread_mutex_unlock(&ConnEntryLock);
    }
    return count;

}
	

//+++
void showConnState(connState* conn_state){
    struct in6_addr ips;
    struct in6_addr ipd;

    ips.s6_addr32[0] = get_int_ip(conn_state->src_ip);
    ipd.s6_addr32[0] = get_int_ip(conn_state->dst_ip);

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
    printf("s_port %u\n",conn_state->src_prt);
    printf("d_port %u\n",conn_state->dst_prt);
    printf("cxid  %d\n",conn_state->cxid);
    printf("hash  %d\n",conn_state->hash);
    printf("proto %u\n",conn_state->proto);
    printf("---------------ConnState------------------\n");

}

//+++

//+++
void showActionState(actionState* action_state){


    printf("---------------actionState------------------\n");
    printf("last_pkt_time %lu\n", action_state->time);
    printf("cxid: %d\n",action_state->cxid);
    printf("state: %i\n",action_state->state);
    printf("hash  %d\n",action_state->hash);
    printf("---------------actionState------------------\n");

}



void showAllState(){
 int h;
 uint64_t action[100];
 memset(action, 0, sizeof(action));
 uint64_t conn[100];
 memset(conn, 0, sizeof(conn));
 int count_action =0;
 int count_conn=0;
 for (h = 0; h < BUCKET_SIZE; h++)
    {        
        actionState *action_state = action_bucket[h];
        connState *conn_state = conn_bucket[h];


        while (conn_state != NULL)
        { 
            printf("-----------conn----h%d------------\n",h);    
            //showConnState(conn_state); 
            //conn[count_conn] = conn_state->cxid; 
	    conn[count_conn] = conn_state->hash;
            count_conn++;     
            // Move on to next connection
            conn_state = conn_state->next;
        }   
	while (action_state != NULL)
        {     
            printf("-----------action---h%d------------\n",h);
	    //showActionState(action_state); 
            //action[count_action] = action_state->cxid; 
            action[count_action] = action_state->hash;
            count_action++;     
            // Move on to next connection
            action_state = action_state->next;
        }             
    }
    printf("\n");
    int m,n;
    printf("count_conn%d\n",count_conn);
    printf("------------------------------------\n");
    for(m = 0; m<= count_conn; m++){
	printf("%lu,",conn[m]);
    }
    printf("\n");
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

    uint32_t int32_saddr = get_int_ip(pi->saddr);
    uint32_t int32_daddr = get_int_ip(pi->daddr);

	
    return CXT_HASH4(int32_saddr,pi->src_port, int32_daddr, pi->dst_port,pi->proto);
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
void append_to_conn_list(connState* conn_state){
    //printf("append to conn list function\n");
    int hash = conn_state->hash;
    connState * head = conn_bucket[hash];

    /* * New connections are pushed on to the head of bucket[s_hash] */
    conn_state->next = head;
    if (head != NULL) {
        // are we doubly linked?
        head->prev = conn_state;
    }
    conn_bucket[hash] = conn_state;

}

//add to bucket
void append_to_action_list(actionState* action_state){
    //printf("append to action list function\n");
    int hash =action_state->hash;
    actionState* head = action_bucket[hash];

    /* * New connections are pushed on to the head of bucket[s_hash] */
    action_state->next = head;
    if (head != NULL) {
        // are we doubly linked?
        head->prev = action_state;
    }
    action_bucket[hash] = action_state;

}

void remove_conn_state_node(connState* conn_state, connState ** bucket_ptr)
{
    //printf("remove-conn-state-node function\n");
    connState* prev = conn_state->prev;       /* OLDER connections */
    connState* next = conn_state->next;       /* NEWER connections */

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
    free(conn_state);
    conn_state = NULL;
}

void remove_action_state_node(actionState* action_state, actionState ** bucket_ptr)
{
    //printf("remove-action-state-node function\n");
    actionState* prev = action_state->prev;       /* OLDER connections */
    actionState* next = action_state->next;       /* NEWER connections */

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
    free(action_state);
    action_state = NULL;
}

void remove_hash_conn_node(int cxid, int hash)
{
	//printf("remove-hash-conn-node function\n");
	connState* conn_state = conn_bucket[hash];
	while (conn_state != NULL) {
	   	if (conn_state->cxid == cxid) {
			break;
            	}
        	conn_state = conn_state->prev;
	}

	/* remove from the hash */
        if (conn_state->prev)
        	conn_state->prev->next = conn_state->next;
        if (conn_state->next)
        	conn_state->next->prev = conn_state->prev;
        connState *tmp = conn_state;

        conn_state = conn_state->prev;

        remove_conn_state_node(tmp, &conn_bucket[hash]);
	if (conn_state == NULL) {
        	conn_bucket[hash] = NULL;
        }

}



void remove_hash_action_node(actionState* action_state, int hash)
{
	
	//printf("remove-hash-action-node function\n");
	/* remove from the hash */
        if (action_state->prev)
        	action_state->prev->next = action_state->next;
        if (action_state->next)
        	action_state->next->prev = action_state->prev;
        actionState *tmp = action_state;

        action_state = action_state->prev;

        remove_action_state_node(tmp, &action_bucket[hash]);
	if (action_state == NULL) {
        	action_bucket[hash] = NULL;
        }

}

//todo
void state_expunge_expired()
{
    //packet_number++;
    //printf("\n\n");
    //printf("packet_number %d\n", packet_number);

    //printf("state_expunge_expired function\n");
    time_t current = time(NULL);
    actionState* action_state;
    int iter;

    int ended, expired = 0;
    time_t check_time = time(NULL);

    for (iter = 0; iter < BUCKET_SIZE; iter++) {
        action_state = action_bucket[iter];
        while (action_state != NULL) {
	    //printf("out time not remove_hash_action_node\n");
            if (difftime(action_state->time, current) > EXPIRE_STATE) {
		//printf("out time real remove_hash_action_node\n");
        	remove_hash_action_node(action_state, iter);
		remove_hash_conn_node(action_state->cxid, iter);
            } else {
                action_state = action_state->prev;
            }
        } // end while cxt
    } // end for buckets
}



connState* create_conn_node(packetinfo *pi){
    //printf("Creating conn Node\n");
    cxtrackerid++;
    //printf("flow_number %d\n",cxtrackerid);

//+++
    connac_notify_flow_created();
//+++

    connState* conn_state = (connState*)malloc(sizeof(connState));
    memcpy(conn_state->src_ip, pi->saddr,4);
    conn_state->src_prt= pi->src_port;
    memcpy(conn_state->dst_ip, pi->daddr,4);
    conn_state->dst_prt = pi->dst_port;
    conn_state->proto = pi->proto;
    
    conn_state->cxid = cxtrackerid;
    pi->cxid = cxtrackerid;
    conn_state->hash = pi->hash;
    

    //printf("Added to hash\n");
    append_to_conn_list(conn_state);

    return conn_state;
}

actionState* create_action_node(packetinfo *pi){
    //printf("Creating action Node\n");
    //action_flow++;
    //printf("action_flow %d\n",action_flow);    

    actionState* action_state = (actionState*)malloc(sizeof(actionState));
    
    action_state->time = time(NULL);
    action_state->cxid = pi->cxid;
//+++
    connac_notify_flow_created();
//+++
    
    action_state->hash = pi->hash;
    

    //printf("Added to hash\n");
    append_to_action_list(action_state);

    return action_state;
}

//Checks the state of the node if available
//and checks with the rules is necessary.
//Returns the action the firewall should take 
//for the packet.
void process_with_conn_state(packetinfo *pi){
        //printf("process with conn state function\n");

        int hash = get_hash(pi);
    
        //printf("HASH %d\n",hash);
	pi->hash = hash;
        //SYN ACK Packet
        connState* conn_state = find_conn_state(pi);
        if(conn_state == NULL){
   	    	conn_state = create_conn_node(pi);
	}
	else{
		pi->cxid = conn_state->cxid;
	} 	  

	//memcpy(cache_state->src_ip, conn_state->src_ip,4);
        //cache_state->src_prt= conn_state->src_prt;
        //memcpy(cache_state->dst_ip, conn_state->dst_ip,4);
        //cache_state->dst_prt = conn_state->dst_prt;  
	    
}

//Checks the state of the node if available
//and checks with the rules is necessary.
//Returns the action the firewall should take 
//for the packet.
rule_type_t process_with_action_state(packetinfo *pi){
        //printf("process with action state function\n");

	actionState* action_state = find_action_state(pi);

	if(0 == find_action){
		action_state = create_action_node(pi);
	    	char* sadr = ip_string(pi->saddr);
            	char* dadr = ip_string(pi->daddr);
            	rule_type_t rt=  get_firewall_action(rule_list, sadr, dadr, ntohs(pi->src_port), ntohs(pi->dst_port));    
            	free(sadr);
            	free(dadr);
	    	if(rt==PASS){
			//printf("update_state OPEN\n");
            		action_state->state = OPEN;
            	}
	    	else{
			//printf("update_state CLOSED\n");
			action_state->state = CLOSED;
	    	}
	    	return rt;
	}    

	state_t s = action_state->state;
        //printf("STATE %i\n", s);
	action_state->time = time(NULL);
	  

	//cache_state->state = s;   
	
	find_action = 0;
        if(s==OPEN){
        	return PASS;
        }
	else{
		return BLOCK;
	} 
    
}


connState* find_conn_state(packetinfo *pi){

	//printf("find conn state\n");
	connState* conn_state = conn_bucket[pi->hash];
	char* sadr = ip_string(pi->saddr);
        char* dadr = ip_string(pi->daddr);
	u_short src_port = pi->src_port;
        u_short dst_port = pi->dst_port;

	if(conn_state == NULL){
		printf("conn_state == NULL\n");

	}

        while (conn_state != NULL) {
	   
	    if ((strcmp(ip_string(conn_state->src_ip), sadr) == 0)&& (strcmp(ip_string(conn_state->dst_ip), dadr) == 0) 
			&& (conn_state->src_prt == src_port) && (conn_state->dst_prt == dst_port)){
		return conn_state;
		//break;
            }else if((strcmp(ip_string(conn_state->src_ip), dadr) == 0)&& (strcmp(ip_string(conn_state->dst_ip), sadr) == 0) 
			&& (conn_state->src_prt == dst_port) && (conn_state->dst_prt == src_port)){
		return conn_state;
		//break;
	    }
           conn_state = conn_state->next;
            
        }
	return NULL;
}

actionState* find_action_state(packetinfo *pi){

	//use get_hash or map, check the speed

	uint32_t hash = cxid_bucket[pi->cxid];
	//printf("find action state %u\n",hash);
	actionState* action_state = action_bucket[hash];

        while (action_state != NULL) {
	   
	    if (action_state->cxid == pi->cxid) {
		find_action = 1;
		return action_state;
		//break;
            }
           action_state = action_state->next;
            
        } 
	return NULL;
}







