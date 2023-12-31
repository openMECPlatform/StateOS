#include "CONNAC.h"
#include "connac_ac.h"
#include "debug.h"
#include "protoObject.h"
#include "myActionMessage.pb-c.h"

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
int connac_action_state = -1;

static pthread_t multi_thread;

//from connac_state.c
extern int drop;


int action_send_getPerflowAck(int count){
        printf("send action getPerflowAck\n");

	ActionGetPerflowAckMsg	actionGetPerflowAckMsg;
	action_get_perflow_ack_msg__init(&actionGetPerflowAckMsg);

        actionGetPerflowAckMsg.has_count=1;
	actionGetPerflowAckMsg.count = count;
	MyActionMessage mes;
        my_action_message__init(&mes);
   
        mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionGetPerflowAckMsgType;
        mes.message_case = MY_ACTION_MESSAGE__MESSAGE_ACTION_GET_PERFLOW_ACK_MSG;
        mes.actiongetperflowackmsg = &actionGetPerflowAckMsg;
 
        int len = my_action_message__get_packed_size(&mes);
        //printf("size of getPerflowAck : %u\n", len);
        void *buf = malloc(len);
        my_action_message__pack(&mes, buf);

	int result;
	result = send_conn_proto_object(connac_action_state, buf, len );
	if(result < 0){
		return -1;
	}


	return result;

}

int action_send_putPerflowAck(int hash, int cxid){
	//printf("send action putPerflowAck\n");

	ActionPutPerflowAckMsg	actionPutPerflowAckMsg;
	action_put_perflow_ack_msg__init(&actionPutPerflowAckMsg);

	
        actionPutPerflowAckMsg.has_hash=1;
	actionPutPerflowAckMsg.hash = hash;
	
	actionPutPerflowAckMsg.has_cxid=1;
	actionPutPerflowAckMsg.cxid = cxid;

	MyActionMessage mes;
        my_action_message__init(&mes);
   
        mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionPutPerflowAckMsgType;
        mes.message_case = MY_ACTION_MESSAGE__MESSAGE_ACTION_PUT_PERFLOW_ACK_MSG;
        mes.actionputperflowackmsg = &actionPutPerflowAckMsg;
 
        int len = my_action_message__get_packed_size(&mes);
        //printf("size of getPerflowAck : %u\n", len);
        void *buf = malloc(len);
        my_action_message__pack(&mes, buf);

	int result;
	result = send_conn_proto_object(connac_action_state, buf, len );
	if(result < 0){
		return -1;
	}

	return result;
}

int action_send_perflow(uint8_t* buf, int len){
	int result;
	result = send_conn_proto_object(connac_action_state, buf, len );
	if(result < 0){
		return -1;
	}
	return result;
}



static int handle_get_perflow(ActionGetPerflowMsg* actionGetPerflow_recv)
{
     printf("handle getPerflow\n");
    //set drop flag == 1;
    if(connac_config.ctrl_copy == 0 && connac_config.ctrl_sfc == 0 ){
    	drop = 1;
    }
    int count;
    Key key;
    if(actionGetPerflow_recv->has_hw_proto){
	key.dl_type = actionGetPerflow_recv->hw_proto;
	printf("handle get perflow hw_proto %x\n",key.dl_type);
	}
    else{ key.wildcards |= WILDCARD_DL_TYPE;  }
    if(actionGetPerflow_recv->has_proto){
	key.nw_proto = actionGetPerflow_recv->proto;
	printf("handle get perflow proto %u\n",key.nw_proto);
	}
    else{  key.wildcards |= WILDCARD_NW_PROTO; }

    int share = 0;
    if(actionGetPerflow_recv->has_share == 1){
    	share = actionGetPerflow_recv->share;
    }

    pthread_mutex_lock(&connac_action_lock_get);

    //share == 1, send one completed state instead of conn-action
    if(share == 1){
	if (NULL == connac_locals->action_get_sharestate_perflow)
	{
		printf("this function is not supported");
        	return -1; 
    	}
	count = connac_locals->action_get_sharestate_perflow(key);     
     }
    else{
	if (NULL == connac_locals->action_get_perflow)
	{
		printf("this function is not supported");
        	return -1; 
    	}
	count = connac_locals->action_get_perflow(key);    	
    }  

    if(connac_config.ctrl_adnatfire == 0){
    	action_send_getPerflowAck(count);
    }
    pthread_mutex_unlock(&connac_action_lock_get);


}

static int handle_get_direct_perflow(ActionGetPerflowMsg* actionGetPerflow_recv)
{
  
    printf("handle direct getPerflow\n");

    int share = 0;
    if(actionGetPerflow_recv->has_share == 1){
    	share = actionGetPerflow_recv->share;
    }

    
    int count;

    pthread_mutex_lock(&connac_action_lock_get);

    //share == 1, send one completed state instead of conn-action
    if(share == 1){
	DirectKey key;
        key.src_ip = actionGetPerflow_recv->s_ip;
        key.dst_ip = actionGetPerflow_recv->d_ip;
        key.src_port = actionGetPerflow_recv->s_port;
        key.dst_port = actionGetPerflow_recv->d_port;
        key.nw_proto = actionGetPerflow_recv->proto;

	if (NULL == connac_locals->action_get_direct_sharestate_perflow)
	{
		printf("this function is not supported");
        	return -1; 
    	}
	count = connac_locals->action_get_direct_sharestate_perflow(key);     
     }
    else{
	DirectKey key;
        key.cxid = actionGetPerflow_recv->cxid;
	key.hash = actionGetPerflow_recv->hash;

	if (NULL == connac_locals->action_get_direct_perflow)
	{
		printf("this function is not supported");
        	return -1; 
    	}
	count = connac_locals->action_get_direct_perflow(key);    	
    }  
    if(count == 0){
    	action_send_getPerflowAck(count);
    }
    pthread_mutex_unlock(&connac_action_lock_get);
}


static int handle_put_perflow(ActionPutPerflowMsg* actionPutPerflow_recv)
{
    //printf("handle put perflow\n");

    //share == 1, send one completed state instead of conn-action
    if(connac_config.ctrl_share == 1){
	if (NULL == connac_locals->action_put_sharestate_perflow)
    	{
		printf("this function is not supported");
        	return -1; 
    	}
    	ShareState* share_state = actionPutPerflow_recv->share_state;
        connac_locals->action_put_sharestate_perflow(share_state);
	action_send_putPerflowAck(share_state->hash, share_state->cxid);

    }
    else{
	if (NULL == connac_locals->action_put_perflow)
    	{
		printf("this function is not supported");
        	return -1; 
    	}
    	ActionState* state = actionPutPerflow_recv->state;
        connac_locals->action_put_perflow(state);
	action_send_putPerflowAck(state->hash, state->cxid);
    }
    

}


int action_send_getMultiflowAck(int count){
        printf("send action getMultiflowAck\n");

	ActionGetMultiflowAckMsg    actionGetMultiflowAckMsg;
	action_get_multiflow_ack_msg__init(&actionGetMultiflowAckMsg);

        actionGetMultiflowAckMsg.has_count=1;
	actionGetMultiflowAckMsg.count = count;
	MyActionMessage mes;
        my_action_message__init(&mes);
   
        mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionGetMultiflowAckMsgType;
        mes.message_case = MY_ACTION_MESSAGE__MESSAGE_ACTION_GET_MULTIFLOW_ACK_MSG;
        mes.actiongetmultiflowackmsg = &actionGetMultiflowAckMsg;
 
        int len = my_action_message__get_packed_size(&mes);
        //printf("size of getPerflowAck : %u\n", len);
        void *buf = malloc(len);
        my_action_message__pack(&mes, buf);

	int result;
	result = send_conn_proto_object(connac_action_state, buf, len );
	if(result < 0){
		return -1;
	}


	return result;

}

int action_send_putMultiflowAck(){
	printf("send action putMultiflowAck\n");

	ActionPutMultiflowAckMsg	actionPutMultiflowAckMsg;
	action_put_multiflow_ack_msg__init(&actionPutMultiflowAckMsg);

	MyActionMessage mes;
        my_action_message__init(&mes);
   
        mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionPutMultiflowAckMsgType;
        mes.message_case = MY_ACTION_MESSAGE__MESSAGE_ACTION_PUT_MULTIFLOW_ACK_MSG;
        mes.actionputmultiflowackmsg = &actionPutMultiflowAckMsg;
 
        int len = my_action_message__get_packed_size(&mes);
        //printf("size of getPerflowAck : %u\n", len);
        void *buf = malloc(len);
        my_action_message__pack(&mes, buf);

	int result;
	result = send_conn_proto_object(connac_action_state, buf, len );
	if(result < 0){
		return -1;
	}

	return result;
}

static int handle_get_multiflow(ActionGetMultiflowMsg* actionGetMultiflow_recv)
{
    if (NULL == connac_locals->action_get_multiflow)
    {
	printf("this function is not supported");
        return -1; 
    }

    int count;

    printf("receive getMultiflow\n");
    pthread_mutex_lock(&connac_action_lock_get);

    //INFO_PRINT("receive getPerflow msg and try to get states");
    count = connac_locals->action_get_multiflow();
    //printf("perflow count %d",count);
    action_send_getMultiflowAck(count);    

    pthread_mutex_unlock(&connac_action_lock_get);


}


static int handle_put_multiflow(ActionPutMultiflowMsg* actionPutMultiflow_recv)
{

    if (NULL == connac_locals->action_put_multiflow)
    {
	printf("this function is not supported");
        return -1; 
    }

    printf("receive Multiflow state, begin to putMultiflow\n");
    //pthread_mutex_lock(&connac_action_lock_get);

    ActionMultiState *multi_state = actionPutMultiflow_recv->multi_state;
    
    connac_locals->action_put_multiflow(multi_state);
    
    action_send_putMultiflowAck();

    //pthread_mutex_unlock(&connac_action_lock_get);
}

/*
static void *put_multiflow(void *arg){
	printf("start put multiflow\n");
        
	ActionPutMultiflowMsg* actionPutMultiflow_recv = (ActionPutMultiflowMsg*)arg;
	handle_put_multiflow(actionPutMultiflow_recv);
	
}
*/

int action_send_getAllflowAck(int count){
        printf("send action getAllflowAck\n");

	ActionGetAllflowAckMsg    actionGetAllflowAckMsg;
	action_get_allflow_ack_msg__init(&actionGetAllflowAckMsg);

        actionGetAllflowAckMsg.has_count=1;
	actionGetAllflowAckMsg.count = count;
	MyActionMessage mes;
        my_action_message__init(&mes);
   
        mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionGetAllflowAckMsgType;
        mes.message_case = MY_ACTION_MESSAGE__MESSAGE_ACTION_GET_ALLFLOW_ACK_MSG;
        mes.actiongetallflowackmsg = &actionGetAllflowAckMsg;
 
        int len = my_action_message__get_packed_size(&mes);
        //printf("size of getPerflowAck : %u\n", len);
        void *buf = malloc(len);
        my_action_message__pack(&mes, buf);

	int result;
	result = send_conn_proto_object(connac_action_state, buf, len );
	if(result < 0){
		return -1;
	}


	return result;

}

int action_send_putAllflowAck(){
	printf("send action putAllflowAck\n");

	ActionPutAllflowAckMsg	actionPutAllflowAckMsg;
	action_put_allflow_ack_msg__init(&actionPutAllflowAckMsg);

	MyActionMessage mes;
        my_action_message__init(&mes);
   
        mes.data_type = MY_ACTION_MESSAGE__DATA_TYPE__ActionPutAllflowAckMsgType;
        mes.message_case = MY_ACTION_MESSAGE__MESSAGE_ACTION_PUT_ALLFLOW_ACK_MSG;
        mes.actionputallflowackmsg = &actionPutAllflowAckMsg;
 
        int len = my_action_message__get_packed_size(&mes);
        //printf("size of getPerflowAck : %u\n", len);
        void *buf = malloc(len);
        my_action_message__pack(&mes, buf);

	int result;
	result = send_conn_proto_object(connac_action_state, buf, len );
	if(result < 0){
		return -1;
	}

	return result;
}


static int handle_get_allflow(ActionGetAllflowMsg* actionGetAllflow_recv)
{
    if (NULL == connac_locals->action_get_allflows)
    {
	printf("this function is not supported");
        return -1; 
    }


    int count;

    printf("receive getAllflow\n");

    pthread_mutex_lock(&connac_action_lock_get);

    //INFO_PRINT("receive getPerflow msg and try to get states");
    count = connac_locals->action_get_allflows();
    //printf("perflow count %d",count);
    action_send_getAllflowAck(count);    

    pthread_mutex_unlock(&connac_action_lock_get);


}


static int handle_put_allflow(ActionPutAllflowMsg* actionPutAllflow_recv)
{
    if (NULL == connac_locals->action_put_allflows)
    {
	printf("this function is not supported");
        return -1; 
    }

    printf("receive Allflow state, begin to putAllflow\n");

    //pthread_mutex_lock(&connac_action_lock_get);
    ActionAllState *all_state = actionPutAllflow_recv->all_state;
    
    connac_locals->action_put_allflows(all_state);
    
    action_send_putAllflowAck();
    //pthread_mutex_unlock(&connac_action_lock_get);
}



static void *state_handler(void *arg)
{
    INFO_PRINT("Action handling thread started");

    while (1)
    {
        //INFO_PRINT("while.....action....\n");        
	// Attempt to read a JSON string


	ProtoObject protoObject;

        protoObject = action_read(connac_action_state);

	MyActionMessage myActionMessage = *my_action_message__unpack(NULL,protoObject.length,protoObject.object);




       
	//int data_type = myActionMessage.data_type;
        //printf("data_type : %d\n",data_type );
 	
	
	if(myActionMessage.data_type == MY_ACTION_MESSAGE__DATA_TYPE__ActionGetPerflowMsgType){
		ActionGetPerflowMsg* actionGetPerflowMsg = myActionMessage.actiongetperflowmsg;
		if(actionGetPerflowMsg->has_s_ip || actionGetPerflowMsg->has_cxid){
			handle_get_direct_perflow(actionGetPerflowMsg);
		}
		else{
			handle_get_perflow(actionGetPerflowMsg);
		}
	}else if(myActionMessage.data_type ==MY_ACTION_MESSAGE__DATA_TYPE__ActionPutPerflowMsgType){
		ActionPutPerflowMsg* actionPutPerflowMsg = myActionMessage.actionputperflowmsg;
		handle_put_perflow(actionPutPerflowMsg);

	}else if(myActionMessage.data_type == MY_ACTION_MESSAGE__DATA_TYPE__ActionGetMultiflowMsgType){
		ActionGetMultiflowMsg* actionGetMultiflowMsg = myActionMessage.actiongetmultiflowmsg;
		handle_get_multiflow(actionGetMultiflowMsg);

	}else if(myActionMessage.data_type ==MY_ACTION_MESSAGE__DATA_TYPE__ActionPutMultiflowMsgType){
		ActionPutMultiflowMsg* actionPutMultiflowMsg = myActionMessage.actionputmultiflowmsg;
		//ActionMultiState *multi_state = actionPutMultiflowMsg->multi_state;
		handle_put_multiflow(actionPutMultiflowMsg);
		//int err;
	     	//if((err = pthread_create(&multi_thread, NULL, put_multiflow,(void *)actionPutMultiflowMsg))!=0)
             	//{
             	//	perror("pthread_create error");
             	//}

	}else if(myActionMessage.data_type == MY_ACTION_MESSAGE__DATA_TYPE__ActionGetAllflowMsgType){
		ActionGetAllflowMsg* actionGetAllflowMsg = myActionMessage.actiongetallflowmsg;
		handle_get_allflow(actionGetAllflowMsg);

	}else if(myActionMessage.data_type ==MY_ACTION_MESSAGE__DATA_TYPE__ActionPutAllflowMsgType){
		ActionPutAllflowMsg* actionPutAllflowMsg = myActionMessage.actionputallflowmsg;
		handle_put_allflow(actionPutAllflowMsg);

	}
	else{ 
            ERROR_PRINT("Unknown type!!!!!");
        }
        
	
	

        // Get message type
        
            

        // Free JSON object
        
    }

    INFO_PRINT("State handling thread finished");
    pthread_exit(NULL);
}



int action_init()
{
    // Open state communication channel
    connac_action_state = conn_active_open(connac_config.ctrl_ip, 
            connac_config.ctrl_port_action);
    if (connac_action_state < 0)
    { 
        ERROR_PRINT("Failed to open action communication channel");
        return connac_action_state; 
    }
   

    int send_success = send_action_syn_message(connac_action_state);
    if(send_success < 0){
	INFO_PRINT("send message failed");
	return -1;
     }
    

    // Create SDMBN state handling thread
    pthread_create(&state_thread, NULL, state_handler, NULL);
    return 1;
}

int action_cleanup()
{
    // Close state communication channel
    if (conn_close(connac_action_state) >= 0)
    { connac_action_state = -1; }
    else
    { ERROR_PRINT("Failed to close action communication channel"); }

    // Destroy state thread
    //pthread_kill(state_thread, SIGKILL);

    return 1;
}
