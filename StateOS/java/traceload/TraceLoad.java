package traceload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @Author: Chenglin Ding
 * @Date: 01.02.2021 21:05
 * @Description:
 */
public class TraceLoad {
    protected static Logger log = LoggerFactory.getLogger(TraceLoad.class);

    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    private static final short REQUEST_SERVER_PORT = (short)8080;

    /** Address of the service */
    private InetSocketAddress serverAddr;

    /** Socket connection to the service */
    private Socket sock;

    /** The rate at which packets should be replayed */
    private int rate;

    /** The number of packets to be replayed**/
    private int numPkts;

    public TraceLoad(String ip, int rate, int numPkts)
    { this(ip, REQUEST_SERVER_PORT, rate, numPkts); }


    public TraceLoad(String ip, short port, int rate, int numPkts)
    {
        this.rate = rate;
        this.numPkts = numPkts;
        this.sock = new Socket();
        this.serverAddr = new InetSocketAddress(ip, port);
    }


    private boolean issueCommand(String action, String trace)
    {
        if (!this.sock.isConnected())
        {
            try
            { this.sock.connect(this.serverAddr);}
            catch (IOException e)
            {
                log.error("Failed to connect to traceload sever");
                return false;
            }
        }

        try
        {
            PrintWriter out = new PrintWriter(this.sock.getOutputStream());
            out.println(String.format("%s %s %d %d", action, trace, this.rate, this.numPkts));
            out.flush();
        }
        catch (IOException e)
        {
            log.error(String.format("Failed to issue command '%s %s %d %d'", action, trace, this.rate, this.numPkts));
            return false;
        }

        return true;
    }


    public boolean startTrace(String trace)
    { return this.issueCommand(ACTION_START, trace); }


    public boolean stopTrace(String trace)
    { return this.issueCommand(ACTION_STOP, trace); }
}
