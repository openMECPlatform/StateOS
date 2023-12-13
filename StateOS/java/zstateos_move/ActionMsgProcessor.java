package zsharestatemove;

import interfaces.NetworkFunction;
import interfaces.msgprocessors.Perflow.ActionProcessPerflow;
import interfaces.msgprocessors.Perflow.ActionProcessShareStatePerflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proto.MyActionMessageProto;
import proto.MyConnMessageProto;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActionMsgProcessor extends ActionProcessShareStatePerflow {
    private volatile int receiveCount;
    private volatile int count;
    private volatile int totalnum ;
    private ActionStateStorage actionStateStorage;
    private ExecutorService threadPool;
    protected static Logger logger = LoggerFactory.getLogger(ActionMsgProcessor.class);

    private ConcurrentLinkedQueue<Integer> ackCxidList;
    private ConcurrentLinkedQueue<Integer> receiveCxidList;

    public ActionMsgProcessor(){
        this.threadPool = Executors.newCachedThreadPool();
        this.count = 0;
        //this.statesList = new ConcurrentLinkedQueue<ActionStateChunk>();
        this.ackCxidList = new ConcurrentLinkedQueue<Integer>();
        this.receiveCxidList = new ConcurrentLinkedQueue<Integer>();
    }



    public void addActionStateStorage(ActionStateStorage actionStateStorage){
        //logger.info("添加了connection storage");
        this.actionStateStorage = actionStateStorage;
    }


    @Override
    public void getActionPerflowAck(MyActionMessageProto.ActionGetPerflowAckMsg actionGetPerflowAckMsg) {
        totalnum = actionGetPerflowAckMsg.getCount();
        //logger.info("getPerflowAck action totalnum:"+ totalnum);
        //logger.info("getPerflowAck action count:"+ count);
        if(totalnum == count){
            setActionStateStorageAck();
        }
    }

    @Override
    public void putActionPerflowAck(MyActionMessageProto.ActionPutPerflowAckMsg actionPutPerflowAckMsg) {
        count++;
        //ackCxidList.add(actionPutPerflowAckMsg.getHash());
        //showAckList();
        //logger.info("putPerflowAck count:"+ count);
        if(totalnum == count){
            setActionStateStorageAck();
        }
    }

    @Override
    public void sendActionGetPerflow(NetworkFunction nf, short hwParameters, byte protoParameters, int share) {
        logger.info("send shareState getperflow");
        MyActionMessageProto.MyActionMessage myMessage = MyActionMessageProto.MyActionMessage.newBuilder()
                .setDataType(MyActionMessageProto.MyActionMessage.DataType.ActionGetPerflowMsgType)
                .setActionGetPerflowMsg(MyActionMessageProto.ActionGetPerflowMsg.newBuilder()
                        .setProto(protoParameters)
                        .setHwProto(hwParameters)
                        .setShare(share)
                        .build())
                .build();
        nf.getActionChannel().sendMessage(myMessage);
    }

    @Override
    public void receiveShareStatePerflow(MyActionMessageProto.ShareState shareState) {
        //receiveCxidList.add(actionState.getHash());
        if(!MoveProcessControl.isFirstRecv){
            MoveProcessControl.isFirstRecv = true;
            MoveProcessControl.movestart = System.currentTimeMillis();
            logger.info("move start from action processor"+ MoveProcessControl.movestart);
        }

        receiveCount++;
        //logger.info("action receive:"+ receiveCount);

        //showNATActionState(actionState);
        //showFWActionState(actionState);
        //showReceiveList();
        //showShareState(shareState);
        if(totalnum == receiveCount){
            logger.info("receive all the states"+ (System.currentTimeMillis() - MoveProcessControl.movestart));
        }

        //if((actionState.getHash() == 7074) || (actionState.getHash() == 7330)) {
        ShareStateChunk shareStateChunk = new ShareStateChunk(actionStateStorage.getDst(),shareState);
        threadPool.submit(shareStateChunk);

        //}
    }

    public void setActionStateStorageAck(){
        //logger.info("set a ActionStorage Ack");
        this.actionStateStorage.setAck();
    }

    public void showShareState(MyActionMessageProto.ShareState shareState){
        logger.info("connstate sip"+ shareState.getSIp());
        logger.info("connstate dip"+ shareState.getDIp());
        logger.info("connstate s_port"+ shareState.getSPort());
        logger.info("connstate d_port"+ shareState.getDPort());
        logger.info("connstate hw_proto"+ shareState.getHwProto());
        logger.info("connstate proto"+ shareState.getProto());
        logger.info("connstate cxid"+ shareState.getCxid());

        logger.info("actionstate start time"+ shareState.getStartTime());
        logger.info("action last packet time"+ shareState.getLastPktTime());
        logger.info("action cxid"+ shareState.getCxid());
        logger.info("action reversed"+ shareState.getReversed());
        logger.info("action af"+ shareState.getAf());
        logger.info("action s_total_pkts"+ shareState.getSTotalPkts());
        logger.info("action s_total_bytes"+ shareState.getSTotalBytes());
        logger.info("action d_total_pkts"+ shareState.getDTotalPkts());
        logger.info("action d_total_pkts"+ shareState.getDTotalBytes());
        logger.info("action s_tcp_flas"+ shareState.getSTcpFlags());
        logger.info("action pad"+ shareState.getPad());
        logger.info("action d_tcp_flags" + shareState.getDTcpFlags());
        logger.info("action check"+ shareState.getCheck());
        logger.info("action hash"+ shareState.getHash());
        if( shareState.hasCAsset()){
            logger.info("c asset exits");
            MyActionMessageProto.Asset c_asset = shareState.getCAsset();
            showAsset(c_asset);
        }
        if(shareState.hasSAsset()){
            logger.info("s asset exits");
            MyActionMessageProto.Asset s_asset = shareState.getSAsset();
            showAsset(s_asset);
        }
    }

    public void showAsset(MyActionMessageProto.Asset asset){
        logger.info("asset  firstSeen"+ asset.getFirstSeen());
        logger.info("asset  lastSeen"+ asset.getLastSeen());
        logger.info("asset  Iattempts"+ asset.getIAttempts());
        logger.info("asset  af"+ asset.getAf());
        logger.info("asset  vlan"+ asset.getVlan());
        logger.info("asset  sip"+ asset.getSIp());
        if(asset.hasServices()){
            showServAsset(asset.getServices());
        }
        if(asset.hasOs()){
            showOSAsset(asset.getOs());
        }
    }

    public void showServAsset(MyActionMessageProto.ServAsset servAsset){
        logger.info("servAsset first_seen"+servAsset.getFirstSeen());
        logger.info("servAsset last_seen"+servAsset.getLastSeen());
        logger.info("servAsset i_attempts"+servAsset.getIAttempts());
        logger.info("servAsset proto"+servAsset.getProto());
        logger.info("servAsset port"+servAsset.getPort());
        logger.info("servAsset ttl"+servAsset.getTtl());
        logger.info("servAsset bservice"+servAsset.getBservice().getData());
        logger.info("servAsset bapplication"+servAsset.getBapplication().getData());
        logger.info("servAsset role"+servAsset.getRole());
        logger.info("servAsset unknown"+servAsset.getUnknown());
    }

    public void showOSAsset(MyActionMessageProto.OsAsset osAsset){
        logger.info("OsAsset first_seen"+osAsset.getFirstSeen());
        logger.info("OsAsset last_seen"+osAsset.getLastSeen());
        logger.info("OsAsset itempts"+osAsset.getIAttempts());
        logger.info("OsAsset bvendor"+osAsset.getBvendor().getData());
        logger.info("OsAsset bos"+osAsset.getBos().getData());
        logger.info("OsAsset detection"+osAsset.getDetection());
        logger.info("OsAsset raw_fp"+osAsset.getRawFp().getData());
        logger.info("OsAsset matched_fp"+osAsset.getMatchedFp().getData());
        logger.info("OsAsset mactch_os"+osAsset.getMatchOs());
        logger.info("OsAsset mactch_desc"+osAsset.getMatchDesc());
        logger.info("OsAsset port"+osAsset.getPort());
        logger.info("OsAsset mtu"+osAsset.getMtu());
        logger.info("OsAsset ttl"+osAsset.getTtl());
        logger.info("OsAsset uptime"+osAsset.getUptime());

    }
}
