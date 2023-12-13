package zsfcacceleratesharstate;

import interfaces.NetworkFunction;
import interfaces.StateStorage;
import interfaces.stepControl.RealProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActionStateStorage extends StateStorage {
    private static volatile ActionStateStorage actionStateStorage;
    private ConcurrentHashMap<String, MyActionMessageProto.ShareState> shareStateNATMap;
    private ConcurrentHashMap<String, MyActionMessageProto.ShareState> shareStateFirewallMap;

    protected static Logger logger = LoggerFactory.getLogger(ActionStateStorage.class);


    private ActionStateStorage(Map<String, NetworkFunction> runNFs, RealProcess realProcess) {
        super(runNFs, realProcess);
        this.shareStateNATMap = new ConcurrentHashMap<>();
    }

    public static ActionStateStorage getInstance(Map<String, NetworkFunction> runNFs, AccelerateSFCControl accelerateSFCControl){
        if(actionStateStorage == null){
            synchronized (ActionStateStorage.class){
                if(actionStateStorage == null){
                    actionStateStorage = new ActionStateStorage(runNFs, accelerateSFCControl);
                }
            }
        }
        return actionStateStorage;
    }

    @Override
    public void setAck() {
        try {
            //if(!this.ack)
            //{
                //this.ack = true;
                logger.info("count down latch num  before: "+this.realProcess.getLatch().getCount());
                ActionMsgProcessor.setAck = false;
                this.realProcess.getLatch().countDown();

                //logger.info("set a action stateStorage ack succesful");

                //this.ack = false;
            //}
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public ConcurrentHashMap<String, MyActionMessageProto.ShareState> getShareStateNATMap() {
        return shareStateNATMap;
    }

    public ConcurrentHashMap<String, MyActionMessageProto.ShareState> getShareStateFirewallMap() {
        return shareStateFirewallMap;
    }
}
