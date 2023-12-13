package zadconnacmove;

import interfaces.NetworkFunction;
import interfaces.StateStorage;
import interfaces.stepControl.RealProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

public class ActionStateStorage extends StateStorage {
    private static volatile ActionStateStorage actionStateStorage;
    private  int advanced;
    private ExecutorService threadPool;
    protected static Logger logger = LoggerFactory.getLogger(ActionStateStorage.class);

    public ActionStateStorage(NetworkFunction dst, RealProcess realProcess) {
        super(dst, realProcess);

    }



    @Override
    public void setAck() {
        try {
            if(!this.ack)
            {
                this.ack = true;
                this.realProcess.getLatch().countDown();
                logger.info("set a action stateStorage ack");
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ActionStateStorage getInstance(NetworkFunction dst, MoveProcessControl moveProcessControl){
        if(actionStateStorage == null){
            synchronized (ActionStateStorage.class){
                if(actionStateStorage == null){
                    actionStateStorage = new ActionStateStorage(dst, moveProcessControl);
                }
            }
        }
        return actionStateStorage;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }
}
