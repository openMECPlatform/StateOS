package interfaces;

import interfaces.stepControl.ProcessControl;
import interfaces.stepControl.RealProcess;
import proto.MyActionMessageProto;
import proto.MyConnMessageProto;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class StateStorage {
    private NetworkFunction dst;
    private NetworkFunction src;
    private Map<String, NetworkFunction> runNFs;
    public boolean ack;
    public RealProcess realProcess;

    private ConcurrentLinkedQueue<MyConnMessageProto.ConnState> connStatesList;
    private ConcurrentHashMap<Long, MyConnMessageProto.ConnState> connStateMap;

    private ConcurrentLinkedQueue<MyActionMessageProto.ActionState> actionStatesList;
    private ConcurrentHashMap<Long, MyActionMessageProto.ActionState> actionStateMap;

    private ConcurrentHashMap<Long, MyActionMessageProto.ShareState> shareStateMap;

    public StateStorage(NetworkFunction dst, RealProcess realProcess){
        this.realProcess = realProcess;
        this.dst = dst;
        this.ack = false;
    }

    public StateStorage(Map<String, NetworkFunction> runNFs, RealProcess realProcess){
        this.realProcess = realProcess;
        this.runNFs = runNFs;
        this.ack = false;
        this.connStatesList = new ConcurrentLinkedQueue<>();
        this.connStateMap = new ConcurrentHashMap<>();
        this.actionStatesList = new ConcurrentLinkedQueue<>();
        this.actionStateMap = new ConcurrentHashMap<>();
        this.shareStateMap = new ConcurrentHashMap<>();
    }


    public ConcurrentLinkedQueue<MyConnMessageProto.ConnState> getConnStatesList() {
        return connStatesList;
    }


    public ConcurrentHashMap<Long, MyConnMessageProto.ConnState> getConnStateMap() {
        return connStateMap;
    }

    public ConcurrentLinkedQueue<MyActionMessageProto.ActionState> getActionStatesList() {
        return actionStatesList;
    }

    public ConcurrentHashMap<Long, MyActionMessageProto.ActionState> getActionStateMap() {
        return actionStateMap;
    }

    public ConcurrentHashMap<Long, MyActionMessageProto.ShareState> getShareStateMap() {
        return shareStateMap;
    }

    public Map<String, NetworkFunction> getRunNFs() {
        return runNFs;
    }

    public NetworkFunction getDst() {
        return dst;
    }

    public abstract void setAck();

    public boolean getAck(){
        return ack;
    }
}
