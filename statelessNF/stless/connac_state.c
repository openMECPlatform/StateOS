#include "STLESS.h"
#include "connac_state.h"
#include "debug.h"
#include "protoObject.h"
#include "myConnMessage.pb-c.h"

#include "connac_core.h"
#include "config.h"
#include "conn.h"

#include <pthread.h>
#include <sys/time.h>
#include <string.h>
#include <assert.h>
#include <errno.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdint.h>



// Thread for handling state messages
static pthread_t state_thread;

// Connection for state messages
int connac_conn_state = -1;
int drop = 0;



int conn_send_perflow(uint8_t* buf, int len){
	int result;
	result = send_conn_proto_object(connac_conn_state, buf, len );
	if(result < 0){
		return -1;
	}
	return result;
}



static void *state_handler(void *arg)
{
    INFO_PRINT("State handling thread started");

    while (1)
    {
        //INFO_PRINT("while....conn.....\n");        
	// Attempt to read a JSON string
	ProtoObject protoObject;

        protoObject = conn_read(connac_conn_state);

	//MyConnMessage myConnMessage = *my_conn_message__unpack(NULL,protoObject.length,protoObject.object);

        
        
    }

    INFO_PRINT("State handling thread finished");
    pthread_exit(NULL);
}



int state_init()
{
    // Open state communication channel
    connac_conn_state = conn_active_open(connac_config.ctrl_ip, connac_config.ctrl_port_conn);
    if (connac_conn_state < 0)
    { 
        ERROR_PRINT("Failed to open state communication channel");
        return connac_conn_state; 
    }
    printf("state init\n");


   	
    int send_success = send_conn_syn_message(connac_conn_state);
    
    if(send_success < 0){
	INFO_PRINT("send message failed");
	return -1;
     }
   
  
    // Create SDMBN state handling thread
    pthread_create(&state_thread, NULL, state_handler, NULL);
    return 1;
}

int state_cleanup()
{
    // Close state communication channel
    if (conn_close(connac_conn_state) >= 0)
    { connac_conn_state = -1; }
    else
    { ERROR_PRINT("Failed to close state communication channel"); }

    // Destroy state thread
    //pthread_kill(state_thread, SIGKILL);

    return 1;
}
