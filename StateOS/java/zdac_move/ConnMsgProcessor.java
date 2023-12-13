package zadconnacmove;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.Perflow.ConnProcessPerflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyConnMessageProto;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ConnMsgProcessor extends ConnProcessPerflow {
    private volatile int count ;
    private volatile int receiveCount ;
    public static volatile int totalnum ;
    private ConnStateStorage connStateStorage;
    private ExecutorService threadPool;
    protected static Logger logger = LoggerFactory.getLogger(ConnMsgProcessor.class);


    public ConnMsgProcessor(){
        this.threadPool = Executors.newCachedThreadPool();
        this.count = 0;
        this.receiveCount = 0;
        //this.statesList = new ConcurrentLinkedQueue<ConnStateChunk>();
    }


    @Override
    public void sendConnGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters, int mode) {
        logger.info("send connection getperflow");
        MyConnMessageProto.MyConnMessage myMessage = MyConnMessageProto.MyConnMessage.newBuilder()
                .setDataType(MyConnMessageProto.MyConnMessage.DataType.ConnGetPerflowMsgType)
                .setConnGetPerflowMsg(MyConnMessageProto.ConnGetPerflowMsg.newBuilder()
                        .setHwProto(hwParameters)
                        .setProto(protoParameters)
                        .setMode(mode)
                        .build())
                .build();

        nf.getConnectionChannel().sendMessage(myMessage);
    }

    @Override
    public void getConnPerflowAck(MyConnMessageProto.ConnGetPerflowAckMsg connGetPerflowAckMsg) {
        totalnum = connGetPerflowAckMsg.getCount();
        logger.info("getPerflowAck connection totalnum:"+ totalnum);
        if(count == totalnum){
            setConnStateStorageAck();
            totalnum = 0;
            count = 0;
        }
    }

    @Override
    public void receiveConnStatePerflow(MyConnMessageProto.ConnState connState) {
        if(!MoveProcessControl.isFirstRecv){
            MoveProcessControl.isFirstRecv = true;
            MoveProcessControl.movestart = System.currentTimeMillis();
            logger.info("move start from action processor"+ MoveProcessControl.movestart);
        }


        //showReceiveList();

        //showConnState(connState);
        ConnStateChunk connStateChunk = null;
        connStateChunk = new ConnStateChunk(connStateStorage.getDst(), connState);
        threadPool.submit(connStateChunk);

    }

    @Override
    public void putConnPerflowAck(MyConnMessageProto.ConnPutPerflowAckMsg connPutPerflowAckMsg) {
        count++;
        //logger.info("connection put perflow count"+count);
        if(count == totalnum){
            setConnStateStorageAck();
            totalnum = 0;
            count = 0;
        }
    }


    public void addConnStateStorage(ConnStateStorage connStateStorage){
        //logger.info("添加了connection storage");
        this.connStateStorage = connStateStorage;
    }


    public void setConnStateStorageAck(){
        //logger.info("set a stateStorage Ack");
        this.connStateStorage.setAck();
    }
}























