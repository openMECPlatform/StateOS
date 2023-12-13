package zadconnaccopy;

import interfaces.NetworkFunction;
import interfaces.StateStorage;
import interfaces.stepControl.RealProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zconnaccopy.ConnStateStorage;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ActionStateStorage extends StateStorage {
    private static volatile ActionStateStorage actionStateStorage;
    private  int advanced;
    private ExecutorService threadPool;
    protected static Logger logger = LoggerFactory.getLogger(ActionStateStorage.class);

    public ActionStateStorage(NetworkFunction dst, RealProcess realProcess) {
        super(dst, realProcess);

    }

    public ActionStateStorage(Map<String, NetworkFunction> runNFs, RealProcess realProcess) {
        super(runNFs, realProcess);
    }



    @Override
    public void setAck() {
        try {
            if(!this.ack)
            {
                this.ack = true;
                this.realProcess.getLatch().countDown();
                logger.info("set a action stateStorage ack");
                //this.ack = false;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ActionStateStorage getInstance(NetworkFunction dst, CopyProcessControl copyProcessControl){
        if(actionStateStorage == null){
            synchronized (ActionStateStorage.class){
                if(actionStateStorage == null){
                    actionStateStorage = new ActionStateStorage(dst, copyProcessControl);
                }
            }
        }
        return actionStateStorage;
    }

    public static ActionStateStorage getInstance(Map<String, NetworkFunction> runNFs, CopyProcessControl copyProcessControl){
        if(actionStateStorage == null){
            synchronized (ConnStateStorage.class){
                if(actionStateStorage == null){
                    actionStateStorage = new ActionStateStorage(runNFs, copyProcessControl);
                }
            }
        }
        return actionStateStorage;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void setProcessAck(boolean ack){
        this.ack = ack;
    }
}
