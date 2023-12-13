package zsfcacceleratesharstate;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.sfc.ActionProcessShareStateSfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;
import sun.nio.ch.Net;


public class ActionMsgProcessor extends ActionProcessShareStateSfc {
    protected static Logger logger = LoggerFactory.getLogger(ActionMsgProcessor.class);

    private ActionStateStorage actionStateStorage;
    private volatile int totalnum ;
    private volatile int receiveCount;
    public static boolean setAck = false;

    @Override
    public void getActionPerflowAck(MyActionMessageProto.ActionGetPerflowAckMsg actionGetPerflowAckMsg) {
        logger.info("sharestate getPerflow ack");
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
        //logger.info("send shareState getperflow");
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
    public void receiveShareStatePerflow(MyActionMessageProto.ShareState shareState) {
        //receiveCxidList.add(actionState.getHash());
        //logger.info("receive a sharestate perflow");
        if(!AccelerateSFCControl.isFirstRecv){
            AccelerateSFCControl.isFirstRecv = true;
            AccelerateSFCControl.getstart = System.currentTimeMillis();
            logger.info("move start from action processor"+ AccelerateSFCControl.getstart);
        }

        receiveCount++;
        if(shareState.hasStartTime()){
            //logger.info("receive a sharestate normal");
            long totalPkts = shareState.getDTotalPkts()+shareState.getSTotalPkts();
            //logger.info("cxid"+shareState.getCxid()+"total pkts: "+totalPkts);
            if(totalPkts > AccelerateSFCControl.threshold){
                logger.info("cxid"+shareState.getCxid()+"total pkts: "+totalPkts);
                actionStateStorage.getShareStateMap().put(shareState.getCxid(), shareState);
            }
            if(totalnum == receiveCount){
                if(!setAck) {
                    logger.info("recieve count = total num,need to setAck");
                    setActionStateStorageAck();
                }
            }

        }else if(shareState.hasNatHash()){
            logger.info("receive a sharestate NAT");
            String key = shareState.getSIp() +"," +shareState.getSPort();
            logger.info("store key:"+key);
            actionStateStorage.getShareStateNATMap().put(key,shareState);
            if(!setAck) {
                logger.info("receive NATstate,need to setAck");
                setActionStateStorageAck();
            }
        }else if(shareState.hasFwstate()){
            logger.info("receive a sharestate Firewall");
            String key = shareState.getSIp() +"," +shareState.getSPort();
            logger.info("store key:"+key);
            actionStateStorage.getShareStateFirewallMap().put(key,shareState);
            if(!setAck) {
                logger.info("receive Firewallstate,need to setAck");
                setActionStateStorageAck();
            }
        }


    }

    public void sendActionGetDirektPerflow(NetworkFunction nf, byte protoParameters, int srcIp, int dstIp,int srcPort, int dstPort,int share) {
        logger.info("send shareState direct getperflow");
        MyActionMessageProto.MyActionMessage myMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionGetPerflowMsgType)
                .setActionGetPerflowMsg(MyActionMessageProto.ActionGetPerflowMsg.newBuilder()
                        .setProto(protoParameters)
                        .setSIp(srcIp)
                        .setDIp(dstIp)
                        .setSPort(srcPort)
                        .setDPort(dstPort)
                        .setShare(1)
                        .build())
                .build();
        nf.getActionChannel().sendMessage(myMessage);
    }

    @Override
    public void deleteDirectPerflow(NetworkFunction nf, String key) {
        logger.info("send delete sharestate msg");

        MyActionMessageProto.MyActionMessage myMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionDeleteMsgType)
                .setActionDeleteMsg(MyActionMessageProto.ActionDeleteMsg.newBuilder()
                    .setSIp(Integer.parseInt(key.split(",")[0])).setSPort(Integer.parseInt(key.split(",")[1])).build()
                ).build();
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
