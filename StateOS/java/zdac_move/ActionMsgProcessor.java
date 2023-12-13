package zadconnacmove;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.Perflow.AdActionProcessPerflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActionMsgProcessor extends AdActionProcessPerflow {
    private volatile int receiveCount;
    private volatile int count;
    private volatile int totalnum ;
    private ActionStateStorage actionStateStorage;
    private ExecutorService threadPool;
    protected static Logger logger = LoggerFactory.getLogger(ActionMsgProcessor.class);


    public ActionMsgProcessor(){
        this.threadPool = Executors.newCachedThreadPool();
        this.count = 0;
    }


    @Override
    public void receiveActionStatePerflow(MyActionMessageProto.ActionState actionState) {
        if(!MoveProcessControl.isFirstRecv){
            MoveProcessControl.isFirstRecv = true;
            MoveProcessControl.movestart = System.currentTimeMillis();
            logger.info("move start from action processor"+ MoveProcessControl.movestart);
        }

        receiveCount++;

        ActionStateChunk actionStateChunk = new ActionStateChunk(actionStateStorage.getDst(), actionState);
        threadPool.submit(actionStateChunk);
    }

    @Override
    public void getActionPerflowAck(MyActionMessageProto.ActionGetPerflowAckMsg actionGetPerflowAckMsg) {
        totalnum = actionGetPerflowAckMsg.getCount();
        logger.info("getPerflowAck action totalnum:"+ totalnum);
        //logger.info("getPerflowAck action count:"+ count);
        if(totalnum == count){
            setActionStateStorageAck();
        }
    }

    @Override
    public void putActionPerflowAck(MyActionMessageProto.ActionPutPerflowAckMsg actionPutPerflowAckMsg) {
        count++;
        //logger.info("action put perflow count"+ count);
        //logger.info("action put perflow totalnum"+totalnum);
        if(totalnum == count){
            setActionStateStorageAck();
        }
    }

    @Override
    public void sendActionGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters, int share) {
        logger.info("发送了action getperflow");
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

    public void addActionStateStorage(ActionStateStorage actionStateStorage){
        //logger.info("add connection storage");
        this.actionStateStorage = actionStateStorage;
    }

    public void setActionStateStorageAck(){
        //logger.info("set a ActionStorage Ack");
        this.actionStateStorage.setAck();
    }
}
