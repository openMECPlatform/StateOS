package channel;

import Server.OperationManager;
import interfaces.msgprocessors.Perflow.ActionProcessPerflow;
import interfaces.msgprocessors.ProcessReceiveMsg;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:13
 * @Description:
 */
public class ActionChannel extends BaseChannel{
    protected static Logger logger = LoggerFactory.getLogger(ActionChannel.class);


    public ActionChannel(Channel channel, OperationManager operationManager) {
        super(channel, operationManager);
    }

    protected void processMessage(Object msg) {
        ProcessReceiveMsg actionMsgProcess = operationManager.getActionMsgProcessors();
        MyActionMessageProto.MyActionMessage myMessage = (MyActionMessageProto.MyActionMessage)msg;

        MyActionMessageProto.MyActionMessage.DataType dataType = myMessage.getDataType();
        if(dataType == MyActionMessageProto.MyActionMessage.DataType.SynType){
            logger.info("syn message comes");
            MyActionMessageProto.ActionSyn syn = myMessage.getActionsyn();
            this.host = syn.getHost() ;
            logger.info("host: "+this.host);
            this.pid = syn.getPid();
            logger.info("pid: "+this.pid);
            operationManager.channelConnected(this);
        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionGetPerflowAckMsgType){
            //logger.info("receive a  conn getAck"+System.currentTimeMillis());
            actionMsgProcess.getActionPerflowAck(myMessage.getActionGetPerflowAckMsg());
        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionStateType){
            //logger.info("receive a connstate"+System.currentTimeMillis());
            actionMsgProcess.receiveActionStatePerflow(myMessage.getActionState());

        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionShareStateType){
            actionMsgProcess.receiveShareStatePerflow(myMessage.getShareState());

        } else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionPutPerflowAckMsgType){
            //logger.info("receive a conn putAck"+System.currentTimeMillis());
            actionMsgProcess.putActionPerflowAck(myMessage.getActionPutPerflowAckMsg());
        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionGetMultiflowAckMsgType){
            //logger.info("receive a connstate"+System.currentTimeMillis());
            actionMsgProcess.getActionMultiflowAck(myMessage.getActionGetMultiflowAckMsg());

        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionMultiStateType){
            //logger.info("receive a connstate"+System.currentTimeMillis());
            actionMsgProcess.receiveActionStateMultiflow(myMessage.getActionMultiState());

        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionPutMultiflowAckMsgType){
            //logger.info("receive a conn putAck"+System.currentTimeMillis());
            actionMsgProcess.putActionMultiflowAck(myMessage.getActionPutMultiflowAckMsg());
        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionGetAllflowAckMsgType){
            //logger.info("receive a connstate"+System.currentTimeMillis());
            actionMsgProcess.getActionAllflowAck(myMessage.getActionGetAllflowAckMsg());

        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionAllStateType){
            //logger.info("receive a connstate"+System.currentTimeMillis());
            actionMsgProcess.receiveActionStateAllflow(myMessage.getActionAllState());

        }else if(dataType == MyActionMessageProto.MyActionMessage.DataType.ActionPutAllflowAckMsgType){
            //logger.info("receive a conn putAck"+System.currentTimeMillis());
            actionMsgProcess.putActionAllflowAck(myMessage.getActionPutAllflowAckMsg());
        }

    }


    public void sendMessage(Object msg) {
        channel.writeAndFlush(msg);
    }
}
