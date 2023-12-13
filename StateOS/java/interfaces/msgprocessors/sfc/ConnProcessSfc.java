package interfaces.msgprocessors.sfc;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.ProcessReceiveMsg;
import proto.MyActionMessageProto;
import proto.MyConnMessageProto;

public abstract class ConnProcessSfc implements ProcessReceiveMsg {
    public void sendConnGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters, int mode){};
    public void getConnPerflowAck(MyConnMessageProto.ConnGetPerflowAckMsg connGetPerflowAckMsg){};

    public void getActionPerflowAck(MyActionMessageProto.ActionGetPerflowAckMsg actionGetPerflowAckMsg){}

    public void receiveActionStatePerflow(MyActionMessageProto.ActionState actionState){}

    public void putActionPerflowAck(MyActionMessageProto.ActionPutPerflowAckMsg actionPutPerflowAckMsg){}

    public void sendActionGetMultiflow(NetworkFunction nf){};
    public void getActionMultiflowAck(MyActionMessageProto.ActionGetMultiflowAckMsg actionGetMultiflowAckMsg){};
    public void receiveActionStateMultiflow(MyActionMessageProto.ActionMultiState actionMultiState){};
    public void putActionMultiflowAck(MyActionMessageProto.ActionPutMultiflowAckMsg actionPutMultiflowAckMsg){};

    public void sendActionGetAllflow(NetworkFunction nf){};
    public void getActionAllflowAck(MyActionMessageProto.ActionGetAllflowAckMsg actionGetAllflowAckMsg){};
    public void receiveActionStateAllflow(MyActionMessageProto.ActionAllState actionAllState){};
    public void putActionAllflowAck(MyActionMessageProto.ActionPutAllflowAckMsg actionPutAllflowAckMsg){};

    public void receiveShareStatePerflow(MyActionMessageProto.ShareState shareState){};

    public void sendActionGetPerflow(NetworkFunction nf,short hwParameters, byte protoParameters, int share){}
    public void sendActionGetDirektPerflow(NetworkFunction nf, byte protoParameters,int srcIp, int dstIp,int srcPort, int dstPort, int share){};
    public void deleteDirectPerflow(NetworkFunction nf, String key){};

    public void sendActionGetCxidPerflow(NetworkFunction nf, long cxid ,int hash,int share){};
}
