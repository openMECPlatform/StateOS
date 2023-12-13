package Server;

import channel.ActionChannel;
import channel.BaseChannel;
import channel.ConnectionChannel;
import interfaces.NetworkFunction;
import interfaces.msgprocessors.ProcessReceiveMsg;
import interfaces.stepControl.ProcessCondition;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:16
 * @Description:
 */
public class OperationManager {
    private ConcurrentHashMap<String, NetworkFunction> nfs;
    private ProcessReceiveMsg connMsgProcessors;
    private ProcessReceiveMsg actionMsgProcessors;
    private ProcessCondition processCondition;
    private int onlyFramework;
    protected static Logger logger = LoggerFactory.getLogger(OperationManager.class);


    public int port1;
    public int port2;
    public  static  int serverSet;


    public OperationManager(){
        parseConfigFile();
        nfs = new ConcurrentHashMap<String, NetworkFunction>();
        port1 = 18080;
        port2 = 18081;
    }

    public OperationManager(ProcessReceiveMsg actionMsgProcessors ) {
        parseConfigFile();
        this.actionMsgProcessors = actionMsgProcessors;
        port2 = 18081;
        nfs = new ConcurrentHashMap<String, NetworkFunction>();
    }

    public OperationManager(ProcessReceiveMsg connMsgProcessors, ProcessReceiveMsg actionMsgProcessors ) {
        parseConfigFile();
        this.connMsgProcessors = connMsgProcessors;
        this.actionMsgProcessors = actionMsgProcessors;
        port1 = 18080;
        port2 = 18081;
        nfs = new ConcurrentHashMap<String, NetworkFunction>();
    }


    public void parseConfigFile(){
        Properties prop = new Properties();
        try {
            FileInputStream fileInputStream = new FileInputStream("/home/sharestate/config.properties");
            prop.load(fileInputStream);
            this.onlyFramework = Integer.parseInt(prop.getProperty("OnlyFramework"));
            logger.info("only framework"+this.onlyFramework);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void run1() throws Exception
    {
        //process clients' connections
        EventLoopGroup connBossGroup=new NioEventLoopGroup();

        //process messages I/O
        EventLoopGroup connWorkerGroup=new NioEventLoopGroup();
        try {
            ServerBootstrap first = new ServerBootstrap();//start NIO service
            first.group(connBossGroup,connWorkerGroup)
                    .channel(NioServerSocketChannel.class) 
                    //.localAddress(new InetSocketAddress(port1))
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    .childHandler(new InitializerServerConn(this));

            bind(first, port1);

            
            EventLoopGroup actionBossGroup=new NioEventLoopGroup();

            
            EventLoopGroup actionWorkerGroup=new NioEventLoopGroup();

                ServerBootstrap second = new ServerBootstrap();
                second.group(actionBossGroup,actionWorkerGroup)
                        .channel(NioServerSocketChannel.class) 
                        //.localAddress(new InetSocketAddress(port1))
                        .option(ChannelOption.SO_BACKLOG,128)
                        .childOption(ChannelOption.SO_KEEPALIVE,true)
                        .childOption(ChannelOption.TCP_NODELAY,true)
                        .childHandler(new InitializerServerAction(this));

                bind(second, port2);



        } finally {

        }



    }

    public void run2() throws Exception
    {
        //receive the connections from clients
        EventLoopGroup connBossGroup=new NioEventLoopGroup();

        //process message I/O
        EventLoopGroup connWorkerGroup=new NioEventLoopGroup();
        try {
            ServerBootstrap first = new ServerBootstrap();//start NIO service
            first.group(connBossGroup,connWorkerGroup)
                    .channel(NioServerSocketChannel.class) //create instance channel using factory model
                    //.localAddress(new InetSocketAddress(port1))//set listen port
                    .option(ChannelOption.SO_BACKLOG,128)//max connections 128，for boss threads
                    .childOption(ChannelOption.SO_KEEPALIVE,true)//childOption for worker threads
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    .childHandler(new InitializerServerAction(this));

            bind(first, port2);

        } finally {

        }

    }



    public void bind(ServerBootstrap serverBootstrap, int port) {
        ChannelFuture channelFuture = serverBootstrap.bind(port);
        channelFuture.addListener(new ServerListener(port, this));

    }



    public NetworkFunction obtainNetworkFunction(String host, int pid){
        //System.out.println("find a NF");
        logger.info("a new NF is found");
        NetworkFunction nf;
        synchronized (this.nfs){
            String id = NetworkFunction.constructId(host , pid );
            System.out.println(id);
            if(nfs.containsKey(id)){
                logger.info("contains this id");
                nf = nfs.get(id);
            }else{
                nf = new NetworkFunction(host , pid);
                logger.info("not contains this id");
                nfs.put(id , nf);
                //System.out.println("set a NF successful");
                logger.info("set a NF successful");
            }
        }
        return nf;
    }

    /**
     * use syn message to check whether two channels are connected
     * if the coming NF is fully connected(receive the syn messages from the conn and action channel )
     * tell the process, a new NF is added
     * @param channel
     */
    public void channelConnected(BaseChannel channel){
        //System.out.println("channel try to connect");
        logger.info("channel try to connect");
        NetworkFunction nf = obtainNetworkFunction(channel.getHost(),  channel.getPid());
        boolean connected = false;
        synchronized (nf){
            channel.setNetworkFunction(nf);
            if(channel instanceof ConnectionChannel){
                //System.out.println("try to set a connection channel");
                logger.info("try to set a connection channel");
                nf.setConnectionChannel((ConnectionChannel) channel);
            }
            else if(channel instanceof ActionChannel){
                //System.out.println("try to set a action channel");
                logger.info("try to set a action channel");
                nf.setActionChannel((ActionChannel)channel);
                if(this.onlyFramework == 1){
                    connected = true;
                }
            }

            if(nf.isFullyConnected()){
                connected = true;
            }
        }
        if(connected){
            //System.out.println(NetworkFunction.constructId(nf.getHost(), nf.getPid())+"has already fully connected");
            logger.info(NetworkFunction.constructId(nf.getHost(), nf.getPid())+"has already fully connected");
        }
        else{
            if(nf.hasConnectionChannel()){
                //System.out.println("connection channel has connected");
                logger.info("connection channel has connected");
            }
            else{
                //System.out.println("action channel has connected");
                logger.info("action channel has connected");
            }
        }

        if(connected){
            processCondition.NFConnected(nf);
        }

    }

    //Prcoess will use this function to add itself
    public void addProcessCondition(ProcessCondition processCondition){
        this.processCondition = processCondition;
        //System.out.println("a new condition is added");
        logger.info("a new condition is added");
    }

    public ProcessReceiveMsg getConnMsgProcessors() {
        return connMsgProcessors;
    }

    public void setConnMsgProcessors(ProcessReceiveMsg connMsgProcessors) {
        this.connMsgProcessors = connMsgProcessors;
    }

    public ProcessReceiveMsg getActionMsgProcessors() {
        return actionMsgProcessors;
    }

    public void setActionMsgProcessors(ProcessReceiveMsg actionMsgProcessors) {
        this.actionMsgProcessors = actionMsgProcessors;
    }


}
