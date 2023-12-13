package zadconnacmove;

import interfaces.NetworkFunction;
import interfaces.StateChunk;
import proto.MyActionMessageProto;

import java.util.concurrent.Callable;

public class ActionStateChunk extends StateChunk implements Callable<Boolean> {

    public ActionStateChunk(NetworkFunction dst, MyActionMessageProto.ActionState actionState) {
        super(dst, actionState);
    }


    @Override
    public Boolean call() throws Exception {
        //logger.info("ActionStateChuck call"+System.currentTimeMillis()+" actionState"+actionState.getData());
        MyActionMessageProto.MyActionMessage putPerflowMessage = null;
        putPerflowMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionPutPerflowMsgType)
                .setActionPutPerflowMsg(MyActionMessageProto.ActionPutPerflowMsg.
                        newBuilder().setState(this.actionState).build())
                .build();

        //logger.info("send a action putPerflowMsg actionState"+actionState.getHash());
        this.dst.getActionChannel().sendMessage(putPerflowMessage);

        return true;
    }
}
