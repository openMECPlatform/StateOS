package zadconnaccopy;

import Server.OperationManager;
import interfaces.HwProtoParameters;
import interfaces.NetworkFunction;
import interfaces.ProtoParameters;
import interfaces.stepControl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traceload.TraceLoad;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CopyProcessControl extends RealProcess implements ProcessControl, ProcessCondition, Runnable {
    private int srcPort;
    private int dstPort;
    public static long copyStart;
    protected static Logger logger = LoggerFactory.getLogger(CopyProcessControl.class);

    private int copyCount;
    private int realCopyCount = 1;
    private String timeWait;
    private int[]  slots;

    @Override
    public void parseConfigFile() {
        Properties prop = new Properties();
        try {
            FileInputStream fileInputStream = new FileInputStream("/home/sharestate/config.properties");
            prop.load(fileInputStream);
            this.traceSwitchPort = Short.parseShort(prop.getProperty("TraceReplaySwitchPort"));
            this.traceHost = prop.getProperty("TraceReplayHost");
            this.traceFile = prop.getProperty("TraceReplayFile");
            this.traceRate = Short.parseShort(prop.getProperty("TraceReplayRate"));
            this.traceNumPkts  = Integer.parseInt(prop.getProperty("TraceReplayNumPkts"));
            this.operationDelay= Integer.parseInt(prop.getProperty("OperationDelay"));
            logger.info("operationdelay"+operationDelay);
            this.stopDelay= Integer.parseInt(prop.getProperty("StopDelay"));
            this.switchid = prop.getProperty("Switchid");
            logger.info("switchid"+switchid);
            this.replayPort = Integer.parseInt(prop.getProperty("TraceReplaySwitchPort"));
            this.srcPort = Integer.parseInt(prop.getProperty("TraceReplaySrcPort"));
            this.dstPort = Integer.parseInt(prop.getProperty("TraceReplayDstPort"));
            //this.replayNF = prop.getProperty("TraceReplayNF");
            this.copyCount = Integer.parseInt(prop.getProperty("copyCount"));
            this.timeWait = prop.getProperty("timeWait");
            logger.info(timeWait);
            String [] time_slot = timeWait.split(" ");
            slots = new int[time_slot.length];
            for(int i =0; i< time_slot.length;i++){
                //logger.info(time_slot[i]);
                slots[i] = Integer.parseInt(time_slot[i])*1000;
            }
            //for(int slot: slots){
            //    logger.info("slot"+slot);
            //}


        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public CopyProcessControl(OperationManager operationManager) {
        super(operationManager);
        numInstances = 2;
        latch = new CountDownLatch(2);
        parseConfigFile();
    }

    @Override
    public void NFConnected(NetworkFunction nf) {
        if(!this.runNFs.containsValue(nf)){
            this.addNetworkFunction(nf);
        }
    }

    @Override
    public void addNetworkFunction(NetworkFunction nf) {
        if (this.runNFs.containsValue(nf))
        { return; }

        for (int i = 1; i <= numInstances; i++)
        {
            String nfID = "nf".concat(Integer.toString(i));
            if (!this.runNFs.containsKey(nfID))
            {
                this.runNFs.put(nfID, nf);
                break;
            }
        }

        if (numInstances == this.runNFs.size())
        { this.executeStep(0); }
    }


    @Override
    public void startMove() {
        ConnStateStorage connStateStorage = ConnStateStorage.getInstance(runNFs.get("nf2"), this);
        ((ConnMsgProcessor)operationManager.getConnMsgProcessors()).addConnStateStorage(connStateStorage);
        ActionStateStorage actionStateStorage = ActionStateStorage.getInstance(runNFs, this);
        ((ActionMsgProcessor)operationManager.getActionMsgProcessors()).addActionStateStorage(actionStateStorage);
        //movestart = System.currentTimeMillis();
        //receiveDoubleAck();



        receiveFirstAck();
        connStateStorage.setProcessAck(false);
        actionStateStorage.setProcessAck(false);


        for(;realCopyCount < copyCount;realCopyCount++){
            try {
                Thread.sleep(slots[realCopyCount-1]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            receiveFirstAck();
            connStateStorage.setProcessAck(false);
            actionStateStorage.setProcessAck(false);
        }

    }


    public void receiveFirstAck(){
        try {

            new Thread(new Runnable() {
                public void run() {
                    operationManager.getConnMsgProcessors().sendConnGetPerflow(runNFs.get("nf1"),
                            HwProtoParameters.TYPE_IPv4, ProtoParameters.PROTOCOL_TCP, 2);
                }
            }).start();


            new Thread(new Runnable() {
                public void run() {
                    logger.info("send a getMultiflow");
                    operationManager.getActionMsgProcessors().sendActionGetMultiflow(runNFs.get("nf1"));
                }
            }).start();


            new Thread(new Runnable() {
                public void run() {
                    logger.info("send a getAllflow");
                    operationManager.getActionMsgProcessors().sendActionGetAllflow(runNFs.get("nf1"));
                }
            }).start();
            latch.await();
            changeForwarding();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void initialForwarding() {

        String curl_cmd = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-1\",\"in_port\":\""+this.replayPort+"\",\"active\":\"true\", \"actions\":\"output="+this.srcPort+"\"}";
        String[] cmd={"curl","-X", "POST","-d", curl_cmd,"http://127.0.0.1:8080/wm/staticflowpusher/json"};
        System.out.println(ProcessUtils.execCurl(cmd));

    }

    @Override
    public void changeForwarding() {
        long copytime = System.currentTimeMillis() - copyStart;
        logger.info(String.format("[COPY_TIME] elapse=%d ", copytime));
        isFirstRecv = false;
        latch = new CountDownLatch(2);




    }


    @Override
    public void executeStep(int step) {
        this.traceLoad = new TraceLoad(this.traceHost, this.traceRate , this.traceNumPkts);
        int startNextAfter;
        switch(step)
        {
            case 0:
                startNextAfter = 2;
                break;
            case 1:
                initialForwarding();
                boolean started = this.traceLoad.startTrace(this.traceFile);
                if (started)
                { logger.info("Started replaying trace"); }
                else
                { logger.error("Failed to start replaying trace"); }
                startNextAfter = this.operationDelay;
                break;
            case 2:
                //deleteForwarding();
                logger.info("a simulation of move start");
                startMove();
                startNextAfter = 0;
                break;
            default:
                return;
        }
        this.scheduler.schedule(new NextStepTask(step+1, this), startNextAfter,
                TimeUnit.SECONDS);


    }


    @Override
    public void run() {
        operationManager.addProcessCondition(this);
    }


}
















