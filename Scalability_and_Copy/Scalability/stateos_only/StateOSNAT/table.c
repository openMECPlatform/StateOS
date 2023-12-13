/*
   Copyright (C) 2010  Infertux <infertux@infertux.com>

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
   */

#include "table.h"

table_record *bucket[BUCKET_SIZE];
extern uint32_t *hash_nat_array[BUCKET_SIZE];
extern struct timeval start_serialize;
uint32_t cxtrackerid = 0;

extern pthread_mutex_t ActionEntryLock;
static pthread_t action_thread;



int local_action_put_perflow(ShareState* recv_state){
	
    printf("receive action state, begin to putPerFlow\n");
    pthread_mutex_lock(&ActionEntryLock);
    table_record *action_state = NULL;
    action_state =(table_record*)malloc(sizeof(table_record));


    int i;
    for(i =0; i< 6;i++){
	action_state->internal_mac[i]=recv_state->ether_src[i];
	//printf("conn_state->ether_dst[i] %u",conn_state->ether_dst[i]);

    }
    //memcpy(conn_state->internal_mac, recv_state->ether_src, ETH_ALEN);
    action_state-> internal_ip = recv_state->s_ip;
    action_state-> external_ip = recv_state->external_ip;
    action_state->internal_port = recv_state->s_port;
    action_state->proto = recv_state->proto;
    
    uint32_t cxid = recv_state->cxid;
    action_state->cxid = cxid;

    //set a right number of cxtrackerid
    if(cxid > cxtrackerid){
	cxtrackerid = cxid;
    }

    action_state->external_port = recv_state->external_port;
    action_state->touch = recv_state->last_pkt_time;
    
    uint32_t hash = recv_state->hash;
    action_state->hash = hash;
    uint32_t nat_hash = recv_state->nat_hash;
    action_state->nat_hash = nat_hash;

//TODO
//    uint32_t nat_hash = CXT_HASH4(ex_ip, IP4ADDR(&ipd), htons(key->tp_src),
//                htons(key->tp_dst), key->nw_proto);

    

    table_record* head = bucket[hash];

    // Add to linked list
    action_state->prev = NULL;
    if (head != NULL)
    {
        head->prev = action_state;
        action_state->next = head;
    }
    else
    { action_state->next = NULL; }
    bucket[hash] = action_state;

//+++
    connac_notify_flow_created();
//+++

    //showPutActionState();
    pthread_mutex_unlock(&ActionEntryLock);
    
  
    return 1;
}


int local_action_get_one_perflow(table_record* action_state){
        printf("local get one action per flow\n");	
        pthread_mutex_lock(&ActionEntryLock);
	
	ShareState *share_state = (ShareState *)malloc(sizeof(ShareState));;
	share_state__init(share_state);

	share_state->n_ether_src = 6;
        share_state->ether_src = malloc(sizeof(uint32_t)*6);

        int m;

	for(m = 0; m < 6; m++){
		share_state->ether_src[m]=action_state->internal_mac[m];
	}
   

	share_state->has_s_ip = 1;
	share_state->s_ip = action_state->internal_ip;
	share_state->has_s_port = 1;
        share_state->s_port = action_state->internal_port;
	share_state->has_proto=1;
	share_state->proto = action_state->proto;
	   	
	share_state->has_last_pkt_time=1;
	share_state->last_pkt_time = action_state->touch;

	share_state->has_external_ip=1;
	share_state->external_ip = action_state->external_ip;
	share_state->has_external_port=1;
	share_state->external_port = action_state->external_port;

	share_state->has_cxid=1;
	share_state->cxid = action_state->cxid;		
	share_state->has_hash = 1;
	share_state->hash = action_state->hash;
	share_state->has_nat_hash = 1;
	share_state->nat_hash = action_state->nat_hash;
	
	MyActionMessage mes;
        my_action_message__init(&mes);	
        mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionShareStateType;
        mes.message_case = MY_ACTION_MESSAGE__MESSAGE_SHARE_STATE;
        mes.sharestate = share_state;
 
        int len = my_action_message__get_packed_size(&mes);
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
	//printf("start a action sender\n");
	table_record* action_state = (table_record*)arg;	

	int send_action = local_action_get_one_perflow(action_state);
	if(send_action < 0){
		printf("send failed");
	}

}

int local_action_get_perflow(Key key){
     printf("start conn get perflow\n");
     //printf("local  key.dl_type %x\n", key.dl_type);
     //printf("key.nw_proto %u\n",key.nw_proto);
 
    int count = 0;
    int h = 0;
    for (h = 0; h < BUCKET_SIZE; h++)
    {        
        pthread_mutex_lock(&ActionEntryLock);
        table_record *action_state = bucket[h];
        while (action_state != NULL)
        {

		
	    // Check nw_proto
	if ((!(key.wildcards & WILDCARD_NW_PROTO)) &&((action_state->proto) != key.nw_proto))
			{
		                action_state = action_state->next;
		                continue;
		        }


		
	    int err;
		
	     //create a thread to send action state
	     if((err = pthread_create(&action_thread, NULL, action_sender, (void*)action_state))!=0)
             {
                 perror("pthread_create error");
             }
		
	 	
		count++;	
	
	        
           // Move on to next connection
           action_state = action_state->next;
        }
        pthread_mutex_unlock(&ActionEntryLock);
    }
    return count;

}
		



//+++
void showActionState(table_record* action_state){

    struct in6_addr ips;
    struct in6_addr ipd;

    ips.s6_addr32[0] = action_state->internal_ip;
    ipd.s6_addr32[0] = action_state->external_ip;

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
    printf("dst_ip %s\n", dst_str);
    printf("internal_port %u\n",action_state->internal_port);
    printf("mac_src %u",action_state->internal_mac[0]);
    printf(" %u",action_state->internal_mac[1]);
    printf(" %u",action_state->internal_mac[2]);
    printf(" %u",action_state->internal_mac[3]);
    printf(" %u",action_state->internal_mac[4]);
    printf(" %u\n",action_state->internal_mac[5]);
    printf("cxid  %d\n",action_state->cxid);
    printf("hash  %d\n",action_state->hash);
   
    printf("external_port %u\n",action_state->external_port);
    printf("last_pkt_time %lu\n", action_state->touch);
    printf("---------------actionState------------------\n");

}



void showAllState(){
 int h;
 uint64_t action[100];
 memset(action, 0, sizeof(action));

 int count_action =0;
 for (h = 0; h < BUCKET_SIZE; h++)
    {        
        table_record *action_state = bucket[h];
 
	while (action_state != NULL)
        {     
            printf("-----------action---h%d------------\n",h);
	    showActionState(action_state); 
          
            action[count_action] = action_state->hash;
            count_action++;     
            // Move on to next connection
            action_state = action_state->next;
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

/**
 * Print each records of the natting table
 *
 * @param mode Verbosity level (see enum print_mode)
 */
void table_print(enum print_mode mode)
{
    table_record *record;

    if (mode == PRINT_ALL)
        printf("     internal MAC |");

    printf("  internal IP | in. port | ex. port |   external IP");

    if (mode == PRINT_ALL)
        printf(" | touch");

    printf("\n");

    int iter;
    for (iter = 0; iter < BUCKET_SIZE; iter++) {
        record = bucket[iter];
        while (record != NULL) {
            if (mode == PRINT_ALL)
            	printf("%17s |",
                    ether_ntoa((struct ether_addr *)&(record->internal_mac)));

        	printf("%13s | %8u | ",
                    	inet_ntoa(*(struct in_addr *)&(record->internal_ip)),
                	record->internal_port);
        	printf("%8u | %13s",
                	record->external_port,
                	inet_ntoa(*(struct in_addr *)&(record->external_ip)));

            if (mode == PRINT_ALL)
            	printf(" | %lu", (long unsigned)record->touch);

            printf("\n");
	    record = record->prev;
        } // end while cxt
    } // end for buckets


    printf("\n");
}

/**
 * Get the mapped external port for a record
 *
 * @param internal_ip   The internal IP address
 * @param internal_port The internal port
 * @param external_ip   The external IP address
 * @return The mapped port (or 0 if not found)
 */
uint16_t table_get_external_port(uint32_t hash, uint32_t cxid)
{
    uint16_t external_port = 0;
    table_record *record;

    do {
        external_port = (hash+cxid) % (MAX_EXTERNAL_PORT - MIN_EXTERNAL_PORT)
            + MIN_EXTERNAL_PORT;
        for (record = bucket[hash]; record && record->external_port != external_port;
                record = record->prev);
    } while (record);

    return external_port;
}

/**
 * Adds a record in the table
 *
 * @param internal_ip   The internal IP address
 * @param internal_mac  The internal MAC address
 * @param internal_port The internal port
 * @param external_ip   The external IP address
 * @return The new added record (beginning of the table)
 */
table_record *table_add(uint32_t internal_ip, uint8_t *internal_mac,
        uint16_t internal_port, uint32_t external_ip, uint32_t hash, packetinfo* pi)
{
    cxtrackerid++;

    
//+++
    connac_notify_flow_created();
//+++

    //printf("table_add\n");
    table_record *record;

    if ((record = (table_record *)malloc(sizeof(table_record)))
            == NULL) {
        perror("Unable to allocate a new record");
        return NULL;
    }

    memcpy(record->internal_mac, internal_mac, ETH_ALEN); /* broadcast */
    record->internal_ip = internal_ip;
    record->internal_port = internal_port;
    //printf("record->internal_port %u\n",record->internal_port);

    record->external_ip = external_ip;
    
    //printf("record->external_port %u\n",record->external_port);

    record->proto = pi->ip->protocol;
    record->cxid = cxtrackerid;

    pi->cxid = cxtrackerid;
    record->external_port = table_get_external_port(cxtrackerid, hash);    

    record->touch = time(NULL); /* current timestamp */
    record->hash = hash;

    table_record* head = bucket[hash];

    /* * New connections are pushed on to the head of bucket[s_hash] */
    record->next = head;
    if (head != NULL) {
        // are we doubly linked?
        head->prev = record;
    }
    bucket[hash] = record;
   
    //printf("table_add finish\n");
    return record;
}

void remove_state_node(table_record * record, table_record ** bucket_ptr)
{
    //printf("remove-state-node function\n");
    table_record * prev = record->prev;       /* OLDER connections */
    table_record * next = record->next;       /* NEWER connections */

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
    free(record);
    record = NULL;
}

void remove_hash_node(table_record * record, int hash)
{
	//printf("remove-hash-node function\n");
	/* remove from the hash */
        if (record->prev)
        	record->prev->next = record->next;
        if (record->next)
        	record->next->prev = record->prev;
        table_record *tmp = record;

        record = record->prev;

        remove_state_node(tmp, &bucket[hash]);
	if (record == NULL) {
        	bucket[hash] = NULL;
        }

}

void state_expunge_expired()
{
    //printf("state_expunge_expired function\n");
    time_t current = time(NULL);
    table_record * record;
    int iter;


    for (iter = 0; iter < BUCKET_SIZE; iter++) {
        record = bucket[iter];
        while (record != NULL) {
            if (difftime(record->touch, current) > RECORD_TIMEOUT) {
                remove_hash_node(record, iter);
            } else {
                record = record->prev;
            }
        } // end while cxt
    } // end for buckets
}


/**
 * Proccess an outcomming packet and delete old records
 *
 * @param internal_ip   The internal IP address
 * @param internal_mac  The internal MAC address
 * @param internal_port The internal port
 * @param external_ip   The external IP address
 * @return The corresponding record
 */
table_record *table_outbound(packetinfo* pi)
{
    //printf("table_outbound\n");
    u_int16_t src_port, dst_port;
    src_port = pi->src_port;
    dst_port = pi->dst_port;
    //record = table_outbound(ip->saddr, eth->h_source, htons(*src_port),
    //        ip->daddr);

    uint32_t internal_ip;
    internal_ip = pi->ip->saddr;
    uint8_t *internal_mac;
    internal_mac = pi->eth->h_source;

    uint16_t internal_port;
    internal_port = src_port;
    //printf("outbound internal_port %d\n",internal_port);

    uint32_t external_ip;
    external_ip = pi->ip->daddr;

    uint32_t hash;
    hash= pi->hash;
    //printf("outbound hash %d\n",hash);

	
    table_record *record = bucket[hash];
    //table_record *before = NULL;

    while (record) {
        if (record->internal_ip == internal_ip &&
                record->internal_port == internal_port &&
                record->external_ip == external_ip) {
            record->touch = time(NULL); /* touch! */

            return record;
        }

        /* obsolete record */
        //if (before && record->touch < time(NULL) + RECORD_TIMEOUT) { 
        //    before->next = record->next;
        //    free(record);
        //}
	

        record = record->prev;
    }
    //printf("table out bound finish\n");
    return table_add(internal_ip, internal_mac, internal_port, external_ip,hash, pi);
}

/**
 * Proccess an incomming packet
 *
 * @param external_ip   The external IP address
 * @param external_port The external port
 * @return The corresponding record
 */
table_record *table_inbound(packetinfo* pi)
{
    //printf("table inbound---------------\n");
    uint32_t external_ip = pi->ip->saddr;
    uint16_t external_port = pi->dst_port;

    uint32_t hash = hash_nat_array[pi->nat_hash];
    table_record *record = bucket[hash];

    //printf("hash %d\n",pi->hash);
    //if(record == NULL){
    //	printf("record == NULL\n");
    //	}

    while (record) {
        //printf("record :%8u | %13s \n", record->external_port, inet_ntoa(*(struct in_addr *)&(record->external_ip)));       
        //printf("parameters :%8u | %13s \n",external_port, inet_ntoa(*(struct in_addr *)&(external_ip)));

        if (record->external_ip == external_ip &&
                record->external_port == external_port ) {
            record->touch = time(NULL); /* touch! */
	    //printf("find record\n");
            return record;
        }
	//printf("next record\n");
        record = record->prev;
    }

#ifdef DEBUG
    fprintf(stderr, 
            "Warning: incomming packet from unknown tuple (IP, port)\n");
#endif
    return NULL; /* packet should be ignored or returned to sender */
}

