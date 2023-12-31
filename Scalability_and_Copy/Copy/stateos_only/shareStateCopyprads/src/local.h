#ifndef _Local_H_
#define _Local_H_

#include <CONNAC.h>

///// DEBUGGING MACROS ///////////////////////////////////////////////////////

typedef struct
{
    uint8_t* buf;
    int length;

}ProtoObject;




///// FUNCTION PROTOTYPES ////////////////////////////////////////////////////

int local_get_one_sharestate(connection* cxt);
int local_action_get_perflow();
int local_action_put_perflow(ShareState* recv_state);

int local_action_get_multiflow();
int local_action_put_multiflow(ActionMultiState* recv_state);
int local_action_get_allflows();
int local_action_put_allflows(ActionAllState* recv_state);

ProtoObject* serialize_cxt_state(connection* cxt);


void showAllCxid();
void showAllState();

void showCxtState(connection* cxt);

void showAllAssets();



#endif
