package zadconnaccopy;

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
            synchronized (ConnStateStorage.class){
                if(connStateStorage == null){
                    connStateStorage = new ConnStateStorage(dst, moveProcessControl);
                }
            }
        }
        return connStateStorage;
    }



    @Override
    public void setAck() {
        //logger.info("set a conn stateStorage ack");
        try {
            if(!this.ack)
            {
                this.ack = true;
                this.realProcess.getLatch().countDown();
                logger.info("set a conn stateStorage ack");
                //this.ack = false;
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void setProcessAck(boolean ack){
        this.ack = ack;
    }
}
