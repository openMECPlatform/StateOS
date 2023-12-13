#ifndef STATELESS_H_INCLUDED
#define STATELESS_H_INCLUDED

#include <stdio.h>
#include <stdlib.h>
#include <hiredis/hiredis.h>
#include <string.h>
#include "state.h"

typedef struct _STLESSStatistics{
    
    int pkts_received;
    int flows_active;
    
} STLESSStatistics; 

int initRedisConfig();
int bucketSet(uint32_t hash, struct _state_node *bucketValue);
int bucketGet(uint32_t hash, struct _state_node *bucketValue);
int isBackup();
int NoticeSet(const char *content);
int NoticeGet();
void stless_notify_packet_received(char *type, struct timeval *recv_time);
void stless_notify_flow_created();
void stless_notify_flow_destroyed();

int statisticSet();
int statisticGet();




#endif                          // CONFIG_H
