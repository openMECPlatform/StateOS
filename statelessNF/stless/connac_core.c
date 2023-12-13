#include "STLESS.h"
#include "conn.h"
#include "config.h"
#include "debug.h"
#include "protoObject.h"
#include "connac_state.h"
#include "connac_core.h"
#include <pthread.h>
#include <assert.h>
#include <sys/time.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <pcap.h>

ConnacConfig connac_config;

// Hack for benchmarking
pthread_mutex_t connac_conn_lock_get;

int connac_init()
{
    // Parse configuration
    connac_parse_config();

    assert(0 == pthread_mutex_init(&connac_lock_conn, NULL));
    
    // Initialize state
    if (state_init() < 0)
    { 
        ERROR_PRINT("Failed to initialize state"); 
        return -1;
    }
   
    
    INFO_PRINT("Initialized");
    
    assert(0 == pthread_mutex_init(&connac_conn_lock_get, NULL));
   
}


int connac_cleanup()
{
    // Clean-up state
    if (state_cleanup() < 0)
    { ERROR_PRINT("Failed to cleanup state"); }

   
    INFO_PRINT("Cleaned-up");

    return 1;
}






