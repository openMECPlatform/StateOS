package interfaces.msgprocessors;

import interfaces.NetworkFunction;
import proto.MyActionMessageProto;
import proto.MyConnMessageProto;
import sun.nio.ch.Net;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:21
 * @Description:
 */
public interface ProcessReceiveMsg {

    void sendConnGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters, int mode);
    void getConnPerflowAck(MyConnMessageProto.ConnGetPerflowAckMsg connGetPerflowAckMsg);
    void receiveConnStatePerflow(MyConnMessageProto.ConnState connState);
    void putConnPerflowAck(MyConnMessageProto.ConnPutPerflowAckMsg connPutPerflowAckMsg);


    void receiveActionStatePerflow(MyActionMessageProto.ActionState actionState);
    void getActionPerflowAck(MyActionMessageProto.ActionGetPerflowAckMsg actionGetPerflowAckMsg);
    void putActionPerflowAck(MyActionMessageProto.ActionPutPerflowAckMsg actionPutPerflowAckMsg);

    void sendActionGetMultiflow(NetworkFunction nf);
    void getActionMultiflowAck(MyActionMessageProto.ActionGetMultiflowAckMsg actionGetMultiflowAckMsg);
    void receiveActionStateMultiflow(MyActionMessageProto.ActionMultiState actionMultiState);
    void putActionMultiflowAck(MyActionMessageProto.ActionPutMultiflowAckMsg actionPutMultiflowAckMsg);

    void sendActionGetAllflow(NetworkFunction nf);
    void getActionAllflowAck(MyActionMessageProto.ActionGetAllflowAckMsg actionGetAllflowAckMsg);
    void receiveActionStateAllflow(MyActionMessageProto.ActionAllState actionAllState);
    void putActionAllflowAck(MyActionMessageProto.ActionPutAllflowAckMsg actionPutAllflowAckMsg);

    void sendActionGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters,int share);
    void receiveShareStatePerflow(MyActionMessageProto.ShareState shareState);

    void sendActionGetDirektPerflow(NetworkFunction nf, byte protoParameters,int srcIp, int dstIp,int srcPort, int dstPort, int share);
    void deleteDirectPerflow(NetworkFunction nf, String key);

    void sendActionGetCxidPerflow(NetworkFunction nf, long cxid ,int hash,int share);
}
