package zsharestatecopy;

import interfaces.NetworkFunction;
import interfaces.StateStorage;
import interfaces.stepControl.RealProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ActionStateStorage extends StateStorage {
    private static volatile ActionStateStorage actionStateStorage;
    private ExecutorService threadPool;
    protected static Logger logger = LoggerFactory.getLogger(zadconnaccopy.ActionStateStorage.class);

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
                this.ack = false;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ActionStateStorage getInstance(Map<String, NetworkFunction> runNFs, CopyProcessControl copyProcessControl){
        if(actionStateStorage == null){
            synchronized (ActionStateStorage.class){
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
}
