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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>
#include <netinet/ether.h>
#include <time.h>
#include <linux/types.h>

#include <pcap.h>
#include <libnet.h>
#include <netinet/ip.h>

#ifndef ETH_ALEN
#define ETH_ALEN 6
#endif

#define MIN_EXTERNAL_PORT 1024
#define MAX_EXTERNAL_PORT 65535
#define RECORD_TIMEOUT 60000 /* in seconds */
#define BUCKET_SIZE  31337

#define CXT_HASH4(src,dst,sp,dp,pr) \
   (( src + dst + sp + dp + pr) % BUCKET_SIZE)

/* Verbosity level */
enum print_mode {PRINT_ALL, PRINT_BRIEF};

/* A node in our linked-list table */
typedef struct _table_record {
    uint8_t     internal_mac[ETH_ALEN];
    uint32_t    internal_ip;
    uint16_t    internal_port;
    uint32_t    external_ip;
    uint16_t    external_port;
    uint32_t    hash;
    uint32_t    nat_hash;
    time_t      touch;
    struct _table_record *prev;
    struct _table_record *next;
} table_record;

typedef struct _packetinfo {
	struct ethhdr *eth;
	struct iphdr *ip; 
	u_int16_t src_port;
	u_int16_t dst_port;
	int hash;
	//int cxid;
} packetinfo;

/* Our functions prototypes */
void table_print(enum print_mode mode);
table_record *table_outbound(packetinfo* pi);
table_record *table_inbound(packetinfo* pi);

void state_expunge_expired();





