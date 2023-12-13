package zadconnaccopy;

import interfaces.NetworkFunction;
import interfaces.StateChunk;
import proto.MyConnMessageProto;

import java.util.concurrent.Callable;

public class ConnStateChunk extends StateChunk implements Callable<Boolean> {
    public ConnStateChunk(NetworkFunction dst, MyConnMessageProto.ConnState connState) {
        super(dst, connState);
    }

    @Override
    public Boolean call() throws Exception {
        MyConnMessageProto.MyConnMessage putPerflowMessage = null;
        putPerflowMessage = MyConnMessageProto.MyConnMessage.newBuilder()
                .setDataType(MyConnMessageProto.MyConnMessage.DataType.ConnPutPerflowMsgType)
                .setConnPutPerflowMsg(MyConnMessageProto.ConnPutPerflowMsg
                        .newBuilder().setState(super.connState).build())
                .build();
        super.dst.getConnectionChannel().sendMessage(putPerflowMessage);
        //logger.info("send a connection putPerFlowMsg send finish"+connState.getData());

        return true;
    }
}
