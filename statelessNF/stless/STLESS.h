#ifdef __cplusplus
extern "C" {
#endif

#ifndef _CONNAC_H_
#define _CONNAC_H_

#include "myConnMessage.pb-c.h"
#include "myActionMessage.pb-c.h"
#include <stdint.h>
#include <sys/time.h>
#include <pcap/pcap.h>



//from connac_state.c
extern int drop;

///// FUNCTION PROTOTYPES ////////////////////////////////////////////////////
//shared library export

//TODO
//change name to stless

int conn_send_perflow(uint8_t* buf, int len);
int action_send_perflow(uint8_t* buf, int len);
int connac_init();
int connac_cleanup();



#endif

#ifdef __cplusplus
}
#endif
