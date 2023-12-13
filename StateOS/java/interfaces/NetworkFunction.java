package interfaces;

import channel.ActionChannel;
import channel.ConnectionChannel;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:16
 * @Description:
 */
public class NetworkFunction {
    private String host;
    private  int pid;
    private ConnectionChannel connectionChannel;
    private ActionChannel actionChannel;

    public NetworkFunction(String host, int pid) {
        this.host = host;
        this.pid = pid;
        this.connectionChannel = null;
        this.actionChannel = null;
    }

    public String getId()
    { return constructId(this.host,this.pid); }

    public static String constructId(String host, int pid)
    { return host+"."+pid;	}

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public ConnectionChannel getConnectionChannel() {
        return connectionChannel;
    }

    public void setConnectionChannel(ConnectionChannel connectionChannel) {
        this.connectionChannel = connectionChannel;
    }

    public ActionChannel getActionChannel() {
        return actionChannel;
    }

    public void setActionChannel(ActionChannel actionChannel) {
        this.actionChannel = actionChannel;
    }

    public boolean hasConnectionChannel()
    { return (this.connectionChannel != null); }

    public boolean hasActionChannel()
    { return (this.actionChannel != null); }

    public boolean isFullyConnected()
    {
        return (this.hasActionChannel() && this.hasConnectionChannel());
    }

    @Override
    public String toString() {
        return this.getId();
    }
}
