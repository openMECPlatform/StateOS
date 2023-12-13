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
extern table_record *nat_bucket[BUCKET_SIZE];

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
uint16_t table_get_external_port(uint32_t internal_ip, uint16_t internal_port,
        uint16_t external_ip,uint32_t hash)
{
    uint16_t external_port = 0;
    table_record *record;

    srand(time(NULL));

    do {
        external_port = rand() % (MAX_EXTERNAL_PORT - MIN_EXTERNAL_PORT)
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
        uint16_t internal_port, uint32_t external_ip, uint32_t hash)
{
    //printf("table_add\n");

//+++
    connac_notify_flow_created();
//+++

    table_record *record;

    if ((record = (table_record *)malloc(sizeof(table_record)))
            == NULL) {
        perror("Unable to allocate a new record");
        return NULL;
    }

    memcpy(record->internal_mac, internal_mac, ETH_ALEN); /* broadcast */
    record->internal_ip = internal_ip;
    record->internal_port = internal_port;
    printf("record->internal_port %u\n",record->internal_port);

    record->external_ip = external_ip;
    record->external_port = table_get_external_port(internal_ip, internal_port,
            external_ip, hash);
    printf("record->external_port %u\n",record->external_port);
    
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
    return table_add(internal_ip, internal_mac, internal_port, external_ip,hash);
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
    printf("table inbound---------------\n");
    uint32_t external_ip = pi->ip->saddr;
    uint16_t external_port = pi->dst_port;
  
    table_record *record = nat_bucket[pi->hash];

    printf("hash %d\n",pi->hash);
    if(record == NULL){
	printf("record == NULL\n");
	}

    while (record) {
        printf("record :%8u | %13s \n", record->external_port, inet_ntoa(*(struct in_addr *)&(record->external_ip)));       
        printf("parameters :%8u | %13s \n",external_port, inet_ntoa(*(struct in_addr *)&(external_ip)));

        if (record->external_ip == external_ip &&
                record->external_port == external_port ) {
            record->touch = time(NULL); /* touch! */
	    printf("find record\n");
            return record;
        }
	printf("next record\n");
        record = record->prev;
    }

#ifdef DEBUG
    fprintf(stderr, 
            "Warning: incomming packet from unknown tuple (IP, port)\n");
#endif
    return NULL; /* packet should be ignored or returned to sender */
}

