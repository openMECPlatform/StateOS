package zadconnaccopy;

import interfaces.NetworkFunction;
import interfaces.StateChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;

import java.util.concurrent.Callable;

public class ActionStateChunk extends StateChunk implements Callable<Boolean> {

    protected static Logger logger = LoggerFactory.getLogger(zconnaccopy.ActionStateChunk.class);

    public ActionStateChunk(NetworkFunction dst,MyActionMessageProto.ActionState actionState ) {
      super(dst, actionState);
    }

    public ActionStateChunk(NetworkFunction dst,MyActionMessageProto.ActionMultiState actionMultiState) {
        super(dst, actionMultiState);
    }

    public ActionStateChunk(NetworkFunction dst,MyActionMessageProto.ActionAllState actionAllState ) {
       super(dst, actionAllState);
    }

    @Override
    public Boolean call() throws Exception {
        //logger.info("ActionStateChuck call"+System.currentTimeMillis()+" actionState"+actionState.getData());
        MyActionMessageProto.MyActionMessage putPerflowMessage = null;
        if(this.actionState != null) {
            //logger.info("send action state perflow");
            putPerflowMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                    .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionPutPerflowMsgType)
                    .setActionPutPerflowMsg(MyActionMessageProto.ActionPutPerflowMsg.
                            newBuilder().setState(this.actionState).build())
                    .build();
        }else if(this.actionMultiState != null){
            //logger.info("send action state multiflow");
            putPerflowMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                    .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionPutMultiflowMsgType)
                    .setActionPutMultiflowMsg(MyActionMessageProto.ActionPutMultiflowMsg.
                            newBuilder().setMultiState(this.actionMultiState).build())
                    .build();
        }else if(this.actionAllState != null){
            //logger.info("send action state allflow");
            putPerflowMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                    .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionPutAllflowMsgType)
                    .setActionPutAllflowMsg(MyActionMessageProto.ActionPutAllflowMsg.
                            newBuilder().setAllState(this.actionAllState).build())
                    .build();
        }else {
            logger.info("they are null");
        }


        //logger.info("send a action putPerflowMsg actionState");
        this.dst.getActionChannel().sendMessage(putPerflowMessage);

        return true;
    }
}
