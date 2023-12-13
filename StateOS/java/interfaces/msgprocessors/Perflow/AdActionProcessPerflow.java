package interfaces.msgprocessors.Perflow;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.ProcessReceiveMsg;
import proto.MyActionMessageProto;
import proto.MyConnMessageProto;

public abstract class AdActionProcessPerflow implements ProcessReceiveMsg {
    public void receiveConnStatePerflow(MyConnMessageProto.ConnState connState){}

    public void getConnPerflowAck(MyConnMessageProto.ConnGetPerflowAckMsg connGetPerflowAckMsg){}

    public void putConnPerflowAck(MyConnMessageProto.ConnPutPerflowAckMsg connPutPerflowAckMsg){}

    public void sendConnGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters, int mode) {}

    public void sendActionGetMultiflow(NetworkFunction nf){};
    public void getActionMultiflowAck(MyActionMessageProto.ActionGetMultiflowAckMsg actionGetMultiflowAckMsg){};
    public void receiveActionStateMultiflow(MyActionMessageProto.ActionMultiState actionMultiState){};
    public void putActionMultiflowAck(MyActionMessageProto.ActionPutMultiflowAckMsg actionPutMultiflowAckMsg){};

    public void sendActionGetAllflow(NetworkFunction nf){};
    public void getActionAllflowAck(MyActionMessageProto.ActionGetAllflowAckMsg actionGetAllflowAckMsg){};
    public void receiveActionStateAllflow(MyActionMessageProto.ActionAllState actionAllState){};
    public void putActionAllflowAck(MyActionMessageProto.ActionPutAllflowAckMsg actionPutAllflowAckMsg){};

    public void receiveShareStatePerflow(MyActionMessageProto.ShareState shareState){};

    public void sendActionGetDirektPerflow(NetworkFunction nf,byte protoParameters, int srcIp, int dstIp,int srcPort, int dstPort, int share){};
    public void deleteDirectPerflow(NetworkFunction nf, String key){};

    public void sendActionGetCxidPerflow(NetworkFunction nf, long cxid ,int hash,int share){};
}
