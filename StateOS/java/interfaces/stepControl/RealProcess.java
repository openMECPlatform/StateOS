package interfaces.stepControl;

import Server.OperationManager;
import interfaces.NetworkFunction;

import traceload.TraceLoad;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class RealProcess {
    public final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public int numInstances;
    public Map<String, NetworkFunction> runNFs;
    public OperationManager operationManager;
    public static boolean isFirstRecv = false;
    public static CountDownLatch latch;
    public static CyclicBarrier cyclicBarrier;

    public TraceLoad traceLoad;
    protected short traceSwitchPort;
    public String traceHost;
    public short tracePort;
    public String traceFile;
    public int traceNumPkts;
    public int traceRate;
    public int operationDelay;
    public int stopDelay;
    public int replayPort;
    public String switchid;

    public RealProcess(OperationManager operationManager){
        this.operationManager = operationManager;
        runNFs = new HashMap<>();

    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
