package zsfcaccelerateconnac;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.Perflow.ConnProcessPerflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyConnMessageProto;



public class ConnMsgProcessor extends ConnProcessPerflow {
    protected static Logger logger = LoggerFactory.getLogger(ConnMsgProcessor.class);
    private volatile int receiveCount ;
    public static volatile int totalnum ;
    public static boolean setAck = false;
    private ConnStateStorage connStateStorage;

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
        if(totalnum == receiveCount){
            if(!setAck) {
                logger.info("total num = receive count,need to setAck");
                setConnStateStorageAck();
            }
        }

    }

    @Override
    public void receiveConnStatePerflow(MyConnMessageProto.ConnState connState) {
        if(!AccelerateSFCControl.isFirstRecv){
            AccelerateSFCControl.isFirstRecv = true;

            AccelerateSFCControl.getstart = System.currentTimeMillis();
            logger.info("move start from conn processor"+ AccelerateSFCControl.getstart);
        }

        receiveCount++;
        connStateStorage.getConnStateMap().put(connState.getCxid(), connState);
        logger.info("receive a connstate"+receiveCount);

        if(totalnum == receiveCount){
            if(!setAck) {
                logger.info("recieve count = total num,need to setAck");
                setConnStateStorageAck();
            }
        }
    }

    public void addConnStateStorage(ConnStateStorage connStateStorage){
        //logger.info("add connection storage");
        this.connStateStorage = connStateStorage;
    }


    public void setConnStateStorageAck(){
        //logger.info("set a stateStorage Ack");
        initialAll();
        this.connStateStorage.setAck();
    }

    public void initialAll(){
        receiveCount = 0;
        totalnum = 0;
        AccelerateSFCControl.isFirstRecv = false;
    }

    @Override
    public void putConnPerflowAck(MyConnMessageProto.ConnPutPerflowAckMsg connPutPerflowAckMsg) {

    }
}













