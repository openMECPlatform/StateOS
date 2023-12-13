package zsharestatemove;

import interfaces.NetworkFunction;
import interfaces.StateStorage;
import interfaces.stepControl.RealProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ActionStateStorage extends StateStorage {
    private static volatile ActionStateStorage actionStateStorage;
    private boolean ack;
    protected static Logger logger = LoggerFactory.getLogger(ActionStateStorage.class);


    private ActionStateStorage(NetworkFunction dst, RealProcess realProcess){
        super(dst,realProcess);
        this.ack = false;
    }

    public static ActionStateStorage getInstance(NetworkFunction dst, MoveProcessControl moveProcessControl ){
        if(actionStateStorage == null){
            synchronized (ActionStateStorage.class){
                if(actionStateStorage == null){
                    actionStateStorage = new ActionStateStorage(dst,moveProcessControl );
                }
            }
        }
        return actionStateStorage;
    }


    /**
     * the number of putAcks is equal to the total nums
     */
    public void setAck(){
        logger.info("set a action stateStorage ack");
        try {
            if(!this.ack)
            {
                this.ack = true;
                this.realProcess.getLatch().countDown();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
