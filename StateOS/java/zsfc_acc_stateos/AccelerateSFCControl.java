package zsfcacceleratesharstate;

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
import traceload.TraceLoad;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccelerateSFCControl extends RealProcess implements ProcessControl, ProcessCondition, Runnable{

    protected static Logger logger = LoggerFactory.getLogger(AccelerateSFCControl.class);
    ActionStateStorage actionStateStorage;
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
    //This parameter is used for FLoodlight controller
    public static Map<String, String> flowModMap;
    //This parameter is used for Ryu controller
    public static Map<String, String> ryuflowModMap;
    private int timeout;
    private int flowModNum;

    public AccelerateSFCControl(OperationManager operationManager) {
        super(operationManager);
        latch = new CountDownLatch(1);
        this.operationDelay=5;
        parseConfigFile();
        sendList = new ArrayList<>();
        flowModMap = new HashMap<>();
    }

    public void parseConfigFile(){
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
            this.timeout = Integer.parseInt(prop.getProperty("Timeout"));
            logger.info("timeout:"+timeout);


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
        actionStateStorage = ActionStateStorage.getInstance(runNFs, this);
        ((ActionMsgProcessor)operationManager.getActionMsgProcessors()).addActionStateStorage(actionStateStorage);

        new Thread(new Runnable() {
            public void run() {
                logger.info("send a getPerflow");
                operationManager.getActionMsgProcessors().sendActionGetPerflow(runNFs.get("nf1"),
                        HwProtoParameters.TYPE_IPv4, ProtoParameters.PROTOCOL_TCP, 1);
            }
        }).start();

        receiveAck();

        while(true){
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new Thread(new Runnable() {
                public void run() {
                multipleGetState();
            }
            }).start();

        }

    }

    public void receiveAck(){
        try {
            latch.await();
            logger.info("receive ack in control");

            //used for floodlight controller,delete the timeout state
            String[] cmd = {"curl","http://127.0.0.1:8080/wm/staticflowpusher/list/00:00:00:00:00:00:00:01/json"};
            String result = execCurl(cmd);
            Pattern p1= Pattern.compile("\"(flow.*?)\"");
            Matcher m = p1.matcher(result);
            StringBuilder stringBuilder = new StringBuilder();

            ArrayList<String> flowList = new ArrayList<String>();
            while (m.find()) {
                flowList.add(m.group());
            }

            for(String key: flowModMap.keySet()){
                if(!flowList.contains(key)){
                    String flowKey = flowModMap.get(key);
                    //delete this state
                    deleteRule(runNFs.get("nf2"),runNFs.get("nf3"),key);

                    flowModMap.remove(flowKey);
                }
            }
            //until this point

            /*
            //delete state used for Ryu controller, haven't test it in test-bed before
            //get tcp_src and ipv4_src from flow stats result
            String [] douhaos = result.split(",");
            List<String> matchs = new ArrayList<>();
            int beforematch = 0;
            for(String match : douhaos){
                if(match.contains("match")){
                    beforematch = 1;
                }
                if(beforematch== 1 && (match.contains("tcp_src") || match.contains("ipv4_src"))){
                    matchs.add(match);
                }
                if(match.contains("}")){
                    beforematch = 0;
                }
            }
            System.out.println(matchs.toString());
            List<String> keys = new ArrayList<>();
            for(int i=0; i < matchs.size();i=i+2){
                keys.add(matchs.get(i+1).split("\"")[3]+","+matchs.get(i).split("\"")[3]);
            }
            for(String ryuKey: ryuflowModMap.keySet()){
                String newRyuKey = int2Ip(Integer.parseInt(ryuKey.split(",")[0]))+","+ryuKey.split(",")[1];
                if(!keys.contains(newRyuKey)){
                    ryuflowModMap.remove(ryuKey);
                    //delete related state
                }
            }
            */

            for (MyActionMessageProto.ShareState shareState : actionStateStorage.getShareStateMap().values()) {
                logger.info("Cxid = " + shareState.getCxid());
                if(sendList.contains(shareState.getCxid())){
                    logger.info("don't send Cxid = " + shareState.getCxid());
                    continue;
                }


                new Thread(new Runnable() {
                    public void run() {
                        setNewRule(shareState);
                    }
                }).start();

            }

            long gettime = System.currentTimeMillis() - getstart;
            logger.info(String.format("[COPY_TIME] elapse=%d ", gettime));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setNewRule(MyActionMessageProto.ShareState shareState){
        operationManager.getActionMsgProcessors().sendActionGetDirektPerflow(runNFs.get("nf3"), ProtoParameters.PROTOCOL_TCP,
                shareState.getSIp(),shareState.getDIp(),shareState.getSPort(),shareState.getDPort(),1);
        operationManager.getActionMsgProcessors().sendActionGetDirektPerflow(runNFs.get("nf2"), ProtoParameters.PROTOCOL_TCP,
                shareState.getSIp(),shareState.getDIp(),shareState.getSPort(),shareState.getDPort(),1);
        latch = new CountDownLatch(2);
        logger.info("wait end before"+latch.getCount());
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        String key = shareState.getSIp() +"," +byteArrayToInt(toHH(shareState.getSPort()));
        System.out.println("get key"+key);
        MyActionMessageProto.ShareState currentNatState = actionStateStorage.getShareStateNATMap().get(key);
        for(MyActionMessageProto.ShareState shareState1 : actionStateStorage.getShareStateNATMap().values()){
            System.out.println("sharestate from nat cxid ="+shareState1.getCxid());
        }


        String etherSrc = getMac(currentNatState.getEtherSrcList());
        //logger.info("etherSrc"+etherSrc);
        String etherExternal = getMac(currentNatState.getEtherExternalList());
        //logger.info("etherExternal"+etherExternal);
        String etherGateway = getMac(currentNatState.getEtherGatewayList());
        //logger.info("etherGateway"+etherGateway);
        String ipv4Src = int2Ip(shareState.getSIp());
        //logger.info("ipv4Src"+ipv4Src);
        String ipv4Dst = int2Ip(shareState.getDIp());
        //logger.info("ipv4Dst"+ipv4Dst);
        String ipv4External = int2Ip(currentNatState.getExternalIp());
        //logger.info("ipv4External"+ipv4External);

        //This command is used for floodlight controller
        String curl_cmd4 = "{\"switch\":\"" + this.switchid + "\",\"name\":\"flow-mod-" + flowMod + "\",\"cookie\":\"0\", \"idle_timeout\":"+this.timeout+",\"priority\":\"32768\",\"in_port\":\"" +
                this.NF1Output + "\",\"eth_src\":\"" + etherSrc + "\"" + ",\"eth_type\":\"0x0800\",\"ip_proto\":\"0x06\"," +
                "\"ipv4_src\":\"" + ipv4Src + "\",\"ipv4_dst\":\"" + ipv4Dst + "\"" +
                " ,\"tcp_src\":\"" + byteArrayToInt(toHH(shareState.getSPort())) + "\",\"active\":\"true\", \"actions\":\"set_eth_src=" + etherExternal +
                ",set_eth_dst=" + etherGateway + ",eth_type=0x0800," +
                "set_ipv4_src=" + ipv4External + ",ip_proto=0x06,set_tp_src=" + currentNatState.getExternalPort() + ",output=" + this.NF4Input + "\"}";

        logger.info(curl_cmd4);




        System.out.println(curl_cmd4);
        String[] cmd4 = {"curl", "-X", "POST", "-d", curl_cmd4, "http://127.0.0.1:8080/wm/staticflowpusher/json"};
        System.out.println(execCurl(cmd4));

        flowModMap.put("flow-mod-"+flowMod, key);
        flowMod++;
        //until this line


        /*
        this curl command is used for Ryu controller
        String curl_cmd4 = "{ \"dpid\":549769839358672,\"table_id\":200,\"priority\": 11111," +
                "\"match\":{\"eth_src\":\""+etherSrc+"\",\"eth_type\":2048,\"ipv4_src\":\""+ipv4Src+"\",\"ipv4_dst\":\""+ipv4Dst+"\"," +
                "\"ip_proto\":6,\"tcp_src\":"+byteArrayToInt(toHH(shareState.getSPort()))+",\"in_port\":"+NF1Output+"},\"actions\":[" +
                "{\"type\": \"SET_FIELD\",\"field\": \"eth_src\",\"value\": \""+etherExternal+"\"}," +
                "{\"type\": \"SET_FIELD\",\"field\": \"eth_dst\",\"value\": \""+etherGateway+"\"}," +
                "{\"type\": \"SET_FIELD\",\"field\": \"ipv4_src\", \"value\": \""+ipv4External+"\"}," +
                "{\"type\": \"SET_FIELD\",\"field\": \"tcp_src\",\"value\": "+currentNatState.getExternalPort()+"}," +
                "{\"type\":\"OUTPUT\",\"port\": "+NF4Input+"}] }";
        logger.info(curl_cmd4);
        ryuflowModMap.put(key,curl_cmd4);

        String[] cmd4 = {"curl", "-X", "POST", "-d", curl_cmd4, "http://localhost:8080/stats/flowentry/add"};
        System.out.println(execCurl(cmd4));

         */


        sendList.add(shareState.getCxid());

    }


    public void multipleGetState(){
        latch = new CountDownLatch(1);
        //logger.info("continue get state");
        //logger.info("send a getPerflow");
        operationManager.getActionMsgProcessors().sendActionGetPerflow(runNFs.get("nf1"),
                        HwProtoParameters.TYPE_IPv4, ProtoParameters.PROTOCOL_TCP, 1);


        receiveAck();
    }

    public void deleteRule(NetworkFunction nf1, NetworkFunction nf2, String key){

        operationManager.getActionMsgProcessors().deleteDirectPerflow(nf1, key);
        operationManager.getActionMsgProcessors().deleteDirectPerflow(nf2,key);

        /*
        int flowNum = flowModMap.get(key);
        String curl_cmd = "{\"name\":\"flow-mod-"+flowNum+"\"}";
        System.out.println(curl_cmd);
        String[] cmd4 = {"curl", "-X", "DELETE", "-d", curl_cmd, "http://127.0.0.1:8080/wm/staticflowpusher/json"};
        System.out.println(execCurl(cmd4));
        */

        /*
        following commands are used for Ryu Controller
        String flowMatch = ryuflowModMap.get(key);
        String[] cmd4 = {"curl", "-X", "POST", "-d", flowMatch, "http://localhost:8080/stats/flowentry/delete_strict"};
        System.out.println(execCurl(cmd4));
        */


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
    public void initialForwarding() {
        logger.info("initial forwarding");
        List<String []> cmd_list = new ArrayList();

        /*
        these command are used for ryu controller
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
        */

        //These commands are used for floodlight controller
        String curl_cmd1 = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-1\",\"cookie\":\"0\", \"priority\":\"11110\",\"in_port\":\""+this.replayPort+"\",\"active\":\"true\", " +
                "\"actions\":\"output="+this.NF1Input+"\"}";
        String[] cmd1={"curl","-X", "POST","-d", curl_cmd1,"http://127.0.0.1:8080/wm/staticflowpusher/json"};

        cmd_list.add(cmd1);

        String curl_cmd2 = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-2\",\"cookie\":\"0\", \"priority\":\"11110\",\"in_port\":\""+this.NF1Output+"\",\"active\":\"true\", " +
                "\"actions\":\"output="+this.NF2Input+"\"}";
        String[] cmd2={"curl","-X", "POST","-d", curl_cmd2,"http://127.0.0.1:8080/wm/staticflowpusher/json"};
        cmd_list.add(cmd2);

        String curl_cmd3 = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-3\",\"cookie\":\"0\", \"priority\":\"11110\",\"in_port\":\""+this.NF2Output+"\",\"active\":\"true\", " +
                "\"actions\":\"output="+this.NF3Input+"\"}";
        String[] cmd3={"curl","-X", "POST","-d", curl_cmd3,"http://127.0.0.1:8080/wm/staticflowpusher/json"};
        cmd_list.add(cmd3);



        String curl_cmd4 = "{\"switch\":\""+this.switchid+"\",\"name\":\"flow-mod-4\",\"cookie\":\"0\", \"priority\":\"11110\",\"in_port\":\""+this.NF3Output+"\",\"active\":\"true\", " +
                "\"actions\":\"output="+this.NF4Input+"\"}";
        String[] cmd4={"curl","-X", "POST","-d", curl_cmd4,"http://127.0.0.1:8080/wm/staticflowpusher/json"};
        cmd_list.add(cmd4);




        String[] cmdDelete={"curl", "http://127.0.0.1:8080/wm/staticflowpusher/clear/all/json"};
        execCurl(cmdDelete);
        //until this line

        for(int i = 0; i< numInstances;i++){
            System.out.println(execCurl(cmd_list.get(i)));
        }
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
