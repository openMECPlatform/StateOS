package channel;

import Server.OperationManager;
import interfaces.NetworkFunction;
import io.netty.channel.Channel;


/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:14
 * @Description:
 */
public abstract class BaseChannel {
    protected Channel channel;
    protected OperationManager operationManager;
    protected String host;
    protected int pid;
    protected NetworkFunction nf;

    public BaseChannel(Channel channel, OperationManager operationManager) {
        this.channel = channel;
        this.operationManager = operationManager;
        host = null;
    }

    public void setNetworkFunction(NetworkFunction nf)
    { this.nf = nf; }

    public String getIp()
    {
        String ip = channel.remoteAddress().toString();
        ip = (ip.substring(1)).split(":")[0];
        return ip;
    }

    public String getHost()
    { return this.host; }

    public int getPid()
    { return this.pid; }

    public String toString()
    {
        String localPort = channel.localAddress().toString();
        localPort = (localPort.substring(1)).split(":")[1];
        return this.host+"."+this.pid+":"+localPort;
    }

    protected abstract void processMessage(Object msg) ;
    public abstract void sendMessage(Object msg) ;
}
