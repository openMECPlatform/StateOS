#ifdef __cplusplus
extern "C" {
#endif

#ifndef _CONNACCore_H_
#define _CONNACCore_H_

#include "STLESS.h"
#include "config.h"

///// GLOBALS ////////////////////////////////////////////////////////////////
extern ConnacConfig connac_config;
extern pthread_mutex_t connac_conn_lock_get;

#endif

#ifdef __cplusplus
}
#endif
