#ifdef __cplusplus
extern "C" {
#endif

#ifndef _state_H_
#define _state_H_

#include "STLESS.h"
#include <pthread.h>


///// GLOBALS ///////////////////////////////////////////////////////////////
extern int connac_conn_state;

///// FUNCTION PROTOTYPES ///////////////////////////////////////////////////
int state_init();
int state_cleanup();

#endif

#ifdef __cplusplus
}
#endif
