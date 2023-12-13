package zsfcaccelerateconnac;

import Server.OperationManager;
import interfaces.HwProtoParameters;
import interfaces.NetworkFunction;
import interfaces.ProtoParameters;
import interfaces.stepControl.NextStepTask;
import interfaces.stepControl.ProcessCondition;
import interfaces.stepControl.ProcessControl;
import interfaces.stepControl.RealProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;
import proto.MyConnMessageProto;
import traceload.TraceLoad;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AccelerateSFCControl extends RealProcess implements ProcessControl, ProcessCondition, Runnable{
    protected static Logger logger = LoggerFactory.getLogger(zsfcacceleratesharstate.AccelerateSFCControl.class);
    ActionStateStorage actionStateStorage;
    ConnStateStorage connStateStorage;
    private int NF1Input;
    private int NF1Output;
    private int NF2Input;
    private int NF2Output;
    private int NF3Input;
    private int NF3Output;
    private int NF4Input;
    public static int flowMod = 5;
    public static long getstart;
    public static int threshold;
    private List<Long> sendList;
    public static Map<Long, Integer> flowModMap;

    public AccelerateSFCControl(OperationManager operationManager) {
        super(operationManager);
        latch = new CountDownLatch(2);
        this.operationDelay=5;
        parseConfigFile();
        sendList = new ArrayList<>();
        flowModMap = new HashMap<>();
    }

    @Override
    public void parseConfigFile() {
        Properties prop = new Properties();
        try {
            FileInputStream fileInputStream = new FileInputStream("/home/sharestate/sfc-config.properties");
            prop.load(fileInputStream);
            this.numInstances =  Short.parseShort(prop.getProperty("TraceReplayInstanceNum"));
            this.traceSwitchPort = Short.parseShort(prop.getProperty("TraceReplaySwitchPort"));
            this.traceHost = prop.getProperty("TraceReplayHost");
            this.traceFile = prop.getProperty("TraceReplayFile");
            this.traceRate = Integer.parseInt(prop.getProperty("TraceReplayRate"));
            this.traceNumPkts  = Integer.parseInt(prop.getProperty("TraceReplayNumPkts"));
            this.switchid = prop.getProperty("Switchid");
            logger.info("switchid"+switchid);
            this.replayPort = Integer.parseInt(prop.getProperty("TraceReplaySwitchPort"));
            this.NF1Input = Integer.parseInt(prop.getProperty("TraceReplayNF1Input"));
            logger.info("nf1 input"+NF1Input);
            this.NF1Output = Integer.parseInt(prop.getProperty("TraceReplayNF1Output"));
            logger.info("nf1 output"+NF1Output);
            this.NF2Input= Integer.parseInt(prop.getProperty("TraceReplayNF2Input"));
            logger.info("nf2 input"+NF2Input);
            this.NF2Output = Integer.parseInt(prop.getProperty("TraceReplayNF2Output"));
            logger.info("nf2 output"+NF2Output);
            this.NF3Input = Integer.parseInt(prop.getProperty("TraceReplayNF3Input"));
            logger.info("nf3 input"+NF3Input);
            this.NF3Output = Integer.parseInt(prop.getProperty("TraceReplayNF3Output"));
            logger.info("nf3 output"+NF3Output);
            this.NF4Input = Integer.parseInt(prop.getProperty("TraceReplayNF4Input"));
            logger.info("nf4 input"+NF4Input);
            threshold = Integer.parseInt(prop.getProperty("Threshold"));
            logger.info("threshold:"+threshold);

        }catch (IOException e){
            e.printStackTrace();
        }
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
        logger.info("start move method");

        connStateStorage = ConnStateStorage.getInstance(runNFs,this);
        actionStateStorage = ActionStateStorage.getInstance(runNFs, this);
        ((ConnMsgProcessor)operationManager.getConnMsgProcessors()).addConnStateStorage(connStateStorage);
        ((ActionMsgProcessor)operationManager.getActionMsgProcessors()).addActionStateStorage(actionStateStorage);


        new Thread(new Runnable() {
            public void run() {
                logger.info("send a connection getPerflow");
                operationManager.getConnMsgProcessors().sendConnGetPerflow(runNFs.get("nf1"),
                        HwProtoParameters.TYPE_IPv4, ProtoParameters.PROTOCOL_TCP, 0);
            }
        }).start();

        receiveAck();

        while(true){
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            multipleGetState();

        }
    }


    public void multipleGetState(){
        latch = new CountDownLatch(2);
        logger.info("continue get state");

        new Thread(new Runnable() {
            public void run() {
                logger.info("send a getPerflow");
                operationManager.getConnMsgProcessors().sendConnGetPerflow(runNFs.get("nf1"),
                        HwProtoParameters.TYPE_IPv4, ProtoParameters.PROTOCOL_TCP, 0);
            }
        }).start();

        receiveAck();
    }

    public void receiveAck(){
        try {
            latch.await();
            logger.info("receive ack in control");
            // 迭代值
            for (MyActionMessageProto.ActionState actionState: actionStateStorage.getActionStateMap().values()) {
                logger.info("Cxid = " + actionState.getCxid());
                if(sendList.contains(actionState.getCxid())){
                    logger.info("don't send Cxid = " + actionState.getCxid());
                    continue;
                }


                new Thread(new Runnable() {
                    public void run() {
                        setNewRule(actionState);
                    }
                }).start();



            }

            long gettime = System.currentTimeMillis() - getstart;
            logger.info(String.format("[COPY_TIME] elapse=%d ", gettime));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void setNewRule(MyActionMessageProto.ActionState actionState){
        operationManager.getActionMsgProcessors().sendActionGetCxidPerflow(runNFs.get("nf3"), actionState.getCxid(),actionState.getHash(),0);
        latch = new CountDownLatch(1);
        logger.info("wait end before"+latch.getCount());
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        MyActionMessageProto.ActionState currentNatState = actionStateStorage.getActionStateNATMap().get(actionState.getCxid());
        //for(MyActionMessageProto.ShareState shareState1 : actionStateStorage.getShareStateNATMap().values()){
        //    System.out.println("sharestate from nat cxid ="+shareState1.getCxid());
        //}
        if(currentNatState == null){
            logger.info("currentNATSTate is null");
            return;
        }
        MyConnMessageProto.ConnState currentConnState = connStateStorage.getConnStateMap().get(actionState.getCxid());

        String etherSrc = getMac(currentConnState.getEtherSrcList());
        //logger.info("etherSrc"+etherSrc);
        String etherExternal = getMac(currentNatState.getEtherExternalList());
        //logger.info("etherExternal"+etherExternal);
        String etherGateway = getMac(currentNatState.getEtherGatewayList());
        //logger.info("etherGateway"+etherGateway);
        String ipv4Src = int2Ip(currentConnState.getSIp());
        //logger.info("ipv4Src"+ipv4Src);
        String ipv4Dst = int2Ip(currentConnState.getDIp());
        //logger.info("ipv4Dst"+ipv4Dst);
        String ipv4External = int2Ip(currentNatState.getExternalIp());
        //logger.info("ipv4External"+ipv4External);

        /*
        String curl_cmd4 = "{\"switch\":\"" + this.switchid + "\",\"name\":\"flow-mod-" + flowMod + "\",\"cookie\":\"0\", \"priority\":\"32768\",\"in_port\":\"" +
                this.NF1Output + "\",\"eth_src\":\"" + etherSrc + "\"" + ",\"eth_type\":\"0x0800\",\"ip_proto\":\"0x06\"," +
                "\"ipv4_src\":\"" + ipv4Src + "\",\"ipv4_dst\":\"" + ipv4Dst + "\"" +
                " ,\"tcp_src\":\"" + byteArrayToInt(toHH(currentConnState.getSPort())) + "\",\"active\":\"true\", \"actions\":\"set_eth_src=" + etherExternal +
                ",set_eth_dst=" + etherGateway + ",eth_type=0x0800," +
                "set_ipv4_src=" + ipv4External + ",ip_proto=0x06,set_tp_src=" + currentNatState.getExternalPort() + ",output=" + this.NF4Input + "\"}";


         */

        String curl_cmd4 = "{ \"dpid\":549769839358672,\"table_id\":200,\"priority\": 11111," +
                "\"match\":{\"eth_src\":\""+etherSrc+"\",\"eth_type\":2048,\"ipv4_src\":\""+ipv4Src+"\",\"ipv4_dst\":\""+ipv4Dst+"\"," +
                "\"ip_proto\":6,\"tcp_src\":"+  byteArrayToInt(toHH(currentConnState.getSPort())) +",\"in_port\":"+NF1Output+"},\"actions\":[" +
                "{\"type\": \"SET_FIELD\",\"field\": \"eth_src\",\"value\": \""+etherExternal+"\"}," +
                "{\"type\": \"SET_FIELD\",\"field\": \"eth_dst\",\"value\": \""+etherGateway+"\"}," +
                "{\"type\": \"SET_FIELD\",\"field\": \"ipv4_src\", \"value\": \""+ipv4External+"\"}," +
                "{\"type\": \"SET_FIELD\",\"field\": \"tcp_src\",\"value\": "+currentNatState.getExternalPort()+"}," +
                "{\"type\":\"OUTPUT\",\"port\": "+NF4Input+"}] }";
        logger.info(curl_cmd4);



        flowModMap.put(actionState.getCxid(), flowMod);
        //logger.info(curl_cmd4);
        System.out.println(curl_cmd4);

        String[] cmd4 = {"curl", "-X", "POST", "-d", curl_cmd4, "http://localhost:8080/stats/flowentry/add"};
        //String[] cmd4 = {"curl", "-X", "POST", "-d", curl_cmd4, "http://127.0.0.1:8080/wm/staticflowpusher/json"};
        System.out.println(execCurl(cmd4));
        flowMod++;


        sendList.add(actionState.getCxid());

    }




    @Override
    public void initialForwarding() {
        logger.info("initial forwarding");
        List<String []> cmd_list = new ArrayList();


        String initial1 = "{ \"dpid\":549769839358672,\"table_id\":200,\"priority\": 11110,\"match\":{\"in_port\":"+replayPort+"}," +
                "\"actions\":[{\"type\":\"OUTPUT\",\"port\":"+NF1Input+"}] }";
        String[] cmd1 = {"curl", "-X", "POST", "-d", initial1, "http://localhost:8080/stats/flowentry/add"};

        cmd_list.add(cmd1);


        String initial2 = "{ \"dpid\":549769839358672,\"table_id\":200,\"priority\": 11110,\"match\":{\"in_port\":"+NF1Output+"}," +
                "\"actions\":[{\"type\":\"OUTPUT\",\"port\":"+NF2Input+"}] }";
        String[] cmd2 = {"curl", "-X", "POST", "-d", initial2, "http://localhost:8080/stats/flowentry/add"};
        cmd_list.add(cmd2);


        String initial3 = "{ \"dpid\":549769839358672,\"table_id\":200,\"priority\": 11110,\"match\":{\"in_port\":"+NF2Output+"}," +
                "\"actions\":[{\"type\":\"OUTPUT\",\"port\":"+NF3Input+"}] }";
        String[] cmd3 = {"curl", "-X", "POST", "-d", initial3, "http://localhost:8080/stats/flowentry/add"};
        cmd_list.add(cmd3);

        String[] cmdDelete={"curl",  "-X", "DELETE" ,"http://localhost:8080/stats/flowentry/clear/549769839358672"};
        execCurl(cmdDelete);

        String initial_table1 = "{ \"dpid\":549769839358672,\"table_id\":0," +
                "\"actions\":[{\"type\":\"GOTO_TABLE\",\"table_id\":"+100+"}] }";

        String[] cmd00 = {"curl", "-X", "POST", "-d", initial_table1, "http://localhost:8080/stats/flowentry/add"};
        execCurl(cmd00);

        String initial_table2 = "{ \"dpid\":549769839358672,\"table_id\":100," +
                "\"actions\":[{\"type\":\"GOTO_TABLE\",\"table_id\":"+200+"}] }";

        String[] cmd01 = {"curl", "-X", "POST", "-d", initial_table2, "http://localhost:8080/stats/flowentry/add"};
        execCurl(cmd01);
/*
        String curl_cmd1 = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-1\",\"cookie\":\"0\", \"priority\":\"32760\",\"in_port\":\""+this.replayPort+"\",\"active\":\"true\", " +
                "\"actions\":\"output="+this.NF1Input+"\"}";
        String[] cmd1={"curl","-X", "POST","-d", curl_cmd1,"http://127.0.0.1:8080/wm/staticflowpusher/json"};

        //System.out.println(execCurl(cmd1));
        cmd_list.add(cmd1);

        String curl_cmd2 = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-2\",\"cookie\":\"0\", \"priority\":\"32760\",\"in_port\":\""+this.NF1Output+"\",\"active\":\"true\", " +
                "\"actions\":\"output="+this.NF2Input+"\"}";
        String[] cmd2={"curl","-X", "POST","-d", curl_cmd2,"http://127.0.0.1:8080/wm/staticflowpusher/json"};
        //System.out.println(execCurl(cmd2));
        cmd_list.add(cmd2);

        String curl_cmd3 = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-3\",\"cookie\":\"0\", \"priority\":\"32760\",\"in_port\":\""+this.NF2Output+"\",\"active\":\"true\", " +
                "\"actions\":\"output="+this.NF3Input+"\"}";
        String[] cmd3={"curl","-X", "POST","-d", curl_cmd3,"http://127.0.0.1:8080/wm/staticflowpusher/json"};
        //System.out.println(execCurl(cmd3));
        cmd_list.add(cmd3);



        String curl_cmd4 = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-4\",\"cookie\":\"0\", \"priority\":\"32760\",\"in_port\":\""+this.NF3Output+"\",\"active\":\"true\", " +
                "\"actions\":\"output="+this.NF4Input+"\"}";
        String[] cmd4={"curl","-X", "POST","-d", curl_cmd4,"http://127.0.0.1:8080/wm/staticflowpusher/json"};
        //System.out.println(execCurl(cmd4));
        cmd_list.add(cmd4);




        String[] cmdDelete={"curl", "http://127.0.0.1:8080/wm/staticflowpusher/clear/all/json"};
        execCurl(cmdDelete);

*/
        for(int i = 0; i< numInstances;i++){
            System.out.println(execCurl(cmd_list.get(i)));
        }
    }

    public static String execCurl(String[] cmds) {
        ProcessBuilder process = new ProcessBuilder(cmds);
        Process p;
        try {
            p = process.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            return builder.toString();

        } catch (IOException e) {
            System.out.print("error");
            e.printStackTrace();
        }
        return null;

    }

    public static void deleteRule(Long cxid){
        int flowNum = flowModMap.get(cxid);
        String curl_cmd = "{\"name\":\"flow-mod-"+flowNum+"\"}";
        System.out.println(curl_cmd);
        String[] cmd4 = {"curl", "-X", "DELETE", "-d", curl_cmd, "http://127.0.0.1:8080/wm/staticflowpusher/json"};
        System.out.println(execCurl(cmd4));

    }

    public static String getMac(List<Integer> intList){
        //logger.info("get mac--------------");
        StringBuilder mac = new StringBuilder();
        String s;
        for(int i = 0; i< intList.size() - 1;i++){
            s = Integer.toHexString(intList.get(i));
            if(s.toCharArray().length < 2){
                s = "0"+s;
            }

            mac.append(s+":");
        }
        s = Integer.toHexString(intList.get(intList.size() -1));
        if(s.toCharArray().length < 2){
            s = "0"+s;
        }
        mac.append(s);
        //System.out.println(mac.toString());
        logger.info(mac.toString());
        return mac.toString();

    }

    public static String int2Ip(int ipInt) {
        String[] ipString = new String[4];
        for (int i = 0; i < 4; i++) {
            int pos = i * 8;
            int and = ipInt & (255 << pos);
            ipString[i] = String.valueOf(and >>> pos);
        }
        return String.join(".", ipString);
    }


    public static byte[] toHH(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = 0;
        b[3] = 0;
        return b;
    }

    public static int byteArrayToInt(byte[] b) {
        return   b[1] & 0xFF |
                (b[0] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
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
                //System.out.println("threads start");
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

    @Override
    public void changeForwarding() {

    }

}
