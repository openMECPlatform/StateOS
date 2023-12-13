package zadconnacmove;

import interfaces.NetworkFunction;
import interfaces.StateStorage;
import interfaces.stepControl.RealProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnStateStorage extends StateStorage {
    private static volatile ConnStateStorage connStateStorage;
    protected static Logger logger = LoggerFactory.getLogger(ConnStateStorage.class);

    private ConnStateStorage(NetworkFunction dst, RealProcess realProcess) {
        super(dst, realProcess);
    }

    public static ConnStateStorage getInstance(NetworkFunction dst, RealProcess moveProcessControl){
        if(connStateStorage == null){
            synchronized (zconnacmove.ConnStateStorage.class){
                if(connStateStorage == null){
                    connStateStorage = new ConnStateStorage(dst, moveProcessControl);
                }
            }
        }
        return connStateStorage;
    }

    @Override
    public void setAck() {
        logger.info("set a conn stateStorage ack");
        try {
            this.realProcess.getLatch().countDown();

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
