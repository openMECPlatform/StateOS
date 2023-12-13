package Server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:50
 * @Description:
 */
public class ServerListener implements ChannelFutureListener {
    private int port;
    //ServerBootstrap serverBootstrap;
    private OperationManager operationManager;
    protected static Logger logger = LoggerFactory.getLogger(ServerListener.class);


    public ServerListener(int port, OperationManager operationManager) {
        this.port = port;
        this.operationManager = operationManager;
    }


    public ServerListener(int port) {
        this.port = port;
    }


    /**
     * check whether two ports is ready to listen
     * @param channelFuture
     * @throws Exception
     */
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            //System.out.println("bind success at port: " + this.port);
            logger.info("bind success at port:"+this.port);
            synchronized(operationManager) {
                OperationManager.serverSet++;
                operationManager.notify();
            }

            //System.out.println("Netty setServer"+NettyServer.serverSet);
        } else {
            //NettyServer.bind(this.serverBootstrap, port);
            //System.out.println("port is occupied");
            logger.warn("bind success at port:"+this.port);
        }
    }

}
