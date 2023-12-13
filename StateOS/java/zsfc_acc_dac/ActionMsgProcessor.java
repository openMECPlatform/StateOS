package zsfcaccelerateconnac;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.sfc.ActionProcessConnAcSfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;


public class ActionMsgProcessor extends ActionProcessConnAcSfc {
    protected static Logger logger = LoggerFactory.getLogger(zsfcacceleratesharstate.ActionMsgProcessor.class);

    private ActionStateStorage actionStateStorage;
    private volatile int totalnum ;
    private volatile int receiveCount;
    public static boolean setAck = false;


    @Override
    public void receiveActionStatePerflow(MyActionMessageProto.ActionState actionState) {
        if(!AccelerateSFCControl.isFirstRecv){
            AccelerateSFCControl.isFirstRecv = true;
            AccelerateSFCControl.getstart = System.currentTimeMillis();
            logger.info("move start from action processor"+ AccelerateSFCControl.getstart);
        }

        //logger.info("receive a action state");
        if(actionState.hasStartTime()){
            receiveCount++;

            //logger.info("receive a actionState normal");
            long totalPkts = actionState.getDTotalPkts()+actionState.getSTotalPkts();
            //logger.info("cxid"+shareState.getCxid()+"total pkts: "+totalPkts);
            if(totalPkts > AccelerateSFCControl.threshold){
                logger.info("cxid"+actionState.getCxid()+"total pkts: "+totalPkts);
                actionStateStorage.getActionStateMap().put(actionState.getCxid(),actionState);
            }
            if(totalnum == receiveCount){
                if(!setAck) {
                    logger.info("recieve count = total num,need to setAck");
                    setActionStateStorageAck();
                }
            }

        }else if(actionState.hasNatHash()){
            logger.info("receive a sharestate NAT");
            actionStateStorage.getActionStateNATMap().put(actionState.getCxid(),actionState);
            if(!setAck) {
                logger.info("receive NATstate,need to setAck");
                setActionStateStorageAck();
            }
        }
    }

    @Override
    public void getActionPerflowAck(MyActionMessageProto.ActionGetPerflowAckMsg actionGetPerflowAckMsg) {
        logger.info("action getPerflow ack");
        totalnum = actionGetPerflowAckMsg.getCount();
        if(totalnum == 0){
            //if nat_hash = 31338, nat state does not exist, firewall drop it
            if(!setAck) {
                logger.info("total num = 0,need to setAck");
                setActionStateStorageAck();
            }
        }else if(totalnum == receiveCount){
            if(!setAck) {
                logger.info("total num = receive count,need to setAck");
                setActionStateStorageAck();
            }
        }
    }

    @Override
    public void sendActionGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters, int share) {
        logger.info("发送了actionState getperflow");
        MyActionMessageProto.MyActionMessage myMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionGetPerflowMsgType)
                .setActionGetPerflowMsg(MyActionMessageProto.ActionGetPerflowMsg.newBuilder()
                        .setProto(protoParameters)
                        .setHwProto(hwParameters)
                        .setShare(share)
                        .build())
                .build();
        nf.getActionChannel().sendMessage(myMessage);
    }

    @Override
    public void deleteDirectPerflow(NetworkFunction nf, String key) {

    }



    @Override
    public void sendActionGetCxidPerflow(NetworkFunction nf, long cxid, int hash, int share) {
        logger.info("发送了actionState getperflow");
        MyActionMessageProto.MyActionMessage myMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionGetPerflowMsgType)
                .setActionGetPerflowMsg(MyActionMessageProto.ActionGetPerflowMsg.newBuilder()
                        .setCxid(cxid)
                        .setShare(share)
                        .setHash(hash)
                        .build())
                .build();
        nf.getActionChannel().sendMessage(myMessage);
    }


    public void addActionStateStorage(ActionStateStorage actionStateStorage){
        //logger.info("add connection storage");
        this.actionStateStorage = actionStateStorage;
    }

    public void setActionStateStorageAck(){
        //logger.info("set a ActionStorage Ack");
        initialAll();
        this.actionStateStorage.setAck();
    }

    public void initialAll(){
        receiveCount = 0;
        totalnum = 0;
        AccelerateSFCControl.isFirstRecv = false;
    }

    @Override
    public void putActionPerflowAck(MyActionMessageProto.ActionPutPerflowAckMsg actionPutPerflowAckMsg) {

    }
}
