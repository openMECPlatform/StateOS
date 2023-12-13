package zsharestatecopy;

import interfaces.NetworkFunction;
import interfaces.StateChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;

import java.util.concurrent.Callable;

public class ShareStateChunk extends StateChunk implements Callable<Boolean> {
    protected static Logger logger = LoggerFactory.getLogger(zconnaccopy.ActionStateChunk.class);

    public ShareStateChunk(NetworkFunction dst, MyActionMessageProto.ShareState shareState) {
        super(dst, shareState);
    }

    public ShareStateChunk(NetworkFunction dst, MyActionMessageProto.ActionMultiState actionMultiState) {
        super(dst, actionMultiState);
    }

    public ShareStateChunk(NetworkFunction dst,MyActionMessageProto.ActionAllState actionAllState ) {
        super(dst, actionAllState);
    }

    @Override
    public Boolean call() throws Exception {
        //logger.info("ActionStateChuck call"+System.currentTimeMillis()+" actionState"+actionState.getData());
        MyActionMessageProto.MyActionMessage putPerflowMessage = null;
        if(this.shareState != null) {
            putPerflowMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                    .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionPutPerflowMsgType)
                    .setActionPutPerflowMsg(MyActionMessageProto.ActionPutPerflowMsg.
                            newBuilder().setShareState(this.shareState).build())
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
