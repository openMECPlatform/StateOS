package zsharestatemove;

import interfaces.NetworkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;

import java.util.concurrent.Callable;

public class ShareStateChunk implements Callable<Boolean> {
    private NetworkFunction dst;
    //Todo for Multiflow
    private MyActionMessageProto.ShareState shareState;

    protected static Logger logger = LoggerFactory.getLogger(ShareStateChunk.class);

    public ShareStateChunk(NetworkFunction dst, MyActionMessageProto.ShareState shareState ) {
        this.dst = dst;
        this.shareState = shareState;
    }

    /**
     * Use the putPerflow send the state to the destination
     */
    public Boolean call() throws Exception {
        MyActionMessageProto.MyActionMessage putPerflowMessage = null;
        putPerflowMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionPutPerflowMsgType)
                .setActionPutPerflowMsg(MyActionMessageProto.ActionPutPerflowMsg.
                        newBuilder().setShareState(this.shareState).build())
                .build();

        //logger.info("send a action putPerflowMsg actionState"+ shareState.getHash());
        this.dst.getActionChannel().sendMessage(putPerflowMessage);

        return true;
    }
}
