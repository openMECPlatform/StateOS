package zsfcaccelerateconnac;

import interfaces.NetworkFunction;
import interfaces.StateStorage;
import interfaces.stepControl.RealProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;


public class ConnStateStorage  extends StateStorage {
    private static volatile ConnStateStorage connStateStorage;
    protected static Logger logger = LoggerFactory.getLogger(ConnStateStorage.class);


    private ConnStateStorage(Map<String, NetworkFunction> runNFs, RealProcess realProcess) {
        super(runNFs, realProcess);
    }

    public static ConnStateStorage getInstance(Map<String, NetworkFunction> runNFs, AccelerateSFCControl accelerateSFCControl){
        if(connStateStorage == null){
            synchronized (ConnStateStorage.class){
                if(connStateStorage == null){
                    connStateStorage = new ConnStateStorage(runNFs, accelerateSFCControl);
                }
            }
        }
        return connStateStorage;
    }

    @Override
    public void setAck() {
        try {
            //logger.info("count down latch num  before: "+this.realProcess.getLatch().getCount());
            ActionMsgProcessor.setAck = false;
            this.realProcess.getLatch().countDown();

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
 }