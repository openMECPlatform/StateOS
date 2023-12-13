package zadconnaccopy;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.Allflows.AdActionProcessAllflows;
import interfaces.msgprocessors.Perflow.AdActionProcessPerflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;



import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActionMsgProcessor extends AdActionProcessAllflows {
    private volatile int receiveCount;
    private volatile int count;
    private volatile int totalnum ;

    private volatile int countMultiflow;
    private volatile int totalMultiflow ;
    private volatile int countAllflow;
    private volatile int totalAllflow ;

    private  boolean perflowAck;
    private boolean multiflowAck;
    private boolean allflowAck;



    private ActionStateStorage actionStateStorage;
    private ExecutorService threadPool;
    protected static Logger logger = LoggerFactory.getLogger(ActionMsgProcessor.class);

    public ActionMsgProcessor(){
        this.threadPool = Executors.newCachedThreadPool();
        this.count = 0;
    }


    @Override
    public void receiveActionStatePerflow(MyActionMessageProto.ActionState actionState) {
        if(!CopyProcessControl.isFirstRecv){
            CopyProcessControl.isFirstRecv = true;
            CopyProcessControl.copyStart = System.currentTimeMillis();
            logger.info("copy start from action processor"+ CopyProcessControl.copyStart);
        }

        receiveCount++;
        //logger.info("receive the actionPerflow"+receiveCount);
        ActionStateChunk actionStateChunk = new ActionStateChunk(actionStateStorage.getRunNFs().get("nf2"), actionState);
        threadPool.submit(actionStateChunk);
    }

    @Override
    public void getActionPerflowAck(MyActionMessageProto.ActionGetPerflowAckMsg actionGetPerflowAckMsg) {
        totalnum = actionGetPerflowAckMsg.getCount();
        logger.info("getPerflowAck action totalnum:"+ totalnum);
        //logger.info("getPerflowAck action count:"+ count);
        if(totalnum == count){
            perflowAck = true;
            setActionStateStorageAck();
        }
    }

    @Override
    public void  putActionPerflowAck(MyActionMessageProto.ActionPutPerflowAckMsg actionPutPerflowAckMsg) {
        count++;
        //logger.info("action put perflow count"+ count);
        //logger.info("action put perflow totalnum"+totalnum);
        if(totalnum == count){
            perflowAck = true;
            setActionStateStorageAck();
        }
    }
    @Override
    public void sendActionGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters, int share) {
        logger.info("send action getperflow");
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
    public void sendActionGetMultiflow(NetworkFunction nf) {
        logger.info("send action getpMultiflow");
        MyActionMessageProto.MyActionMessage myActionMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionGetMultiflowMsgType)
                .setActionGetMultiflowMsg(MyActionMessageProto.ActionGetMultiflowMsg.newBuilder().build())
                .build();

        nf.getActionChannel().sendMessage(myActionMessage);
    }

    @Override
    public void getActionMultiflowAck(MyActionMessageProto.ActionGetMultiflowAckMsg actionGetMultiflowAckMsg) {
        totalMultiflow = actionGetMultiflowAckMsg.getCount();
        logger.info("getMultiflowAck action totalnum:"+ totalMultiflow);
        //logger.info("getPerflowAck action count:"+ count);

        if(totalMultiflow == countMultiflow){
            multiflowAck = true;
            setActionStateStorageAck();
        }
    }

    @Override
    public void receiveActionStateMultiflow(MyActionMessageProto.ActionMultiState actionMultiState) {
        if(!CopyProcessControl.isFirstRecv){
            CopyProcessControl.isFirstRecv = true;
            CopyProcessControl.copyStart = System.currentTimeMillis();
            logger.info("copy start from multiflow state"+ CopyProcessControl.copyStart);
        }
        //logger.info("receive action Multiflow");

        ActionStateChunk actionStateChunk = new ActionStateChunk(actionStateStorage.getRunNFs().get("nf2"), actionMultiState);
        threadPool.submit(actionStateChunk);
    }

    @Override
    public void putActionMultiflowAck(MyActionMessageProto.ActionPutMultiflowAckMsg actionPutMultiflowAckMsg) {
        countMultiflow++;

        //logger.info("action put multiflow num"+countMultiflow);
        if(totalMultiflow == countMultiflow){
            multiflowAck = true;
            setActionStateStorageAck();
        }
    }

    @Override
    public void sendActionGetAllflow(NetworkFunction nf) {
        logger.info("send action getAllflow");
        MyActionMessageProto.MyActionMessage myActionMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionGetAllflowMsgType)
                .setActionGetAllflowMsg(MyActionMessageProto.ActionGetAllflowMsg.newBuilder().build())
                .build();

        nf.getActionChannel().sendMessage(myActionMessage);
    }

    @Override
    public void getActionAllflowAck(MyActionMessageProto.ActionGetAllflowAckMsg actionGetAllflowAckMsg) {
        //Allflow has count?
        totalAllflow = actionGetAllflowAckMsg.getCount();
        logger.info("getAllflowAck action totalnum:"+ totalAllflow);
        //logger.info("getPerflowAck action count:"+ count);

        if(totalAllflow == countAllflow){
            allflowAck = true;
            setActionStateStorageAck();
        }
    }

    @Override
    public void receiveActionStateAllflow(MyActionMessageProto.ActionAllState actionAllState) {
        if(!CopyProcessControl.isFirstRecv){
            CopyProcessControl.isFirstRecv = true;
            CopyProcessControl.copyStart = System.currentTimeMillis();
            logger.info("copy start from all flow state processor"+ CopyProcessControl.copyStart);
        }

        //logger.info("receive the allflow");

        ActionStateChunk actionStateChunk = new ActionStateChunk(actionStateStorage.getRunNFs().get("nf2"), actionAllState);
        threadPool.submit(actionStateChunk);
    }

    @Override
    public void putActionAllflowAck(MyActionMessageProto.ActionPutAllflowAckMsg actionPutAllflowAckMsg) {
        countAllflow++;

        //logger.info("action put ALlflow totalnum"+countAllflow);
        //logger.info("action put perflow num"+count);
        if(totalAllflow == countAllflow){
            allflowAck = true;
            setActionStateStorageAck();
        }
    }


    public void addActionStateStorage(ActionStateStorage actionStateStorage){
        logger.info("add connection storage");
        this.actionStateStorage = actionStateStorage;
    }

    public void setActionStateStorageAck(){
        //logger.info("set a ActionStorage Ack");
        if(multiflowAck && allflowAck && perflowAck) {
            logger.info("get action per flow "+this.totalnum);
            logger.info("put action per flow"+this.count);
            logger.info("get action multi flow"+this.totalMultiflow);
            logger.info("put action multi flow"+this.countMultiflow);
            logger.info("get action all flow"+this.totalAllflow);
            logger.info("put action all flow"+this.countAllflow);
            initialAll();
            this.actionStateStorage.setAck();

        }
    }

    public void initialAll(){
        receiveCount = 0;
        count = 0;
        totalnum = 0;

        countMultiflow = 0;
        totalMultiflow = 0 ;
        countAllflow = 0;
        totalAllflow = 0;
        perflowAck = false;
        multiflowAck = false;
        allflowAck = false;
    }
}
