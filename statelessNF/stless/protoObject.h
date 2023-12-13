#ifdef __cplusplus
extern "C" {
#endif

#ifndef _CONNACProto_H_
#define _CONNACProto_H_

#include <pcap.h>
#include <stdint.h>
#include "STLESS.h"

///// DEFINES ////////////////////////////////////////////////////////////////
///// STRUCTS ///////////////////////////////////////////////////////////////


extern pthread_mutex_t connac_lock_conn;
///// FUNCTION PROTOTYPES ////////////////////////////////////////////////////
//json_object *json_compose_perflow_key(PerflowKey *key)

//json_object* json_compose_discover();

int send_conn_proto_object(int conn, uint8_t* buf, int len );
int send_conn_syn_message(int conn);


#endif

#ifdef __cplusplus
}
#endif
