package channel;

import Server.OperationManager;
import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:15
 * @Description:
 */
public class ConnectionChannelHandler  extends BaseChannelHandler{
    protected static Logger logger = LoggerFactory.getLogger(ConnectionChannelHandler.class);

    public ConnectionChannelHandler(OperationManager operationManager) {
        super(operationManager);
    }


    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel incoming=ctx.channel();
        ConnectionChannel connChannel = new ConnectionChannel(incoming, this.operationManager);
		Attribute<BaseChannel> attr = ctx.attr(AttributeMapConstant.NETTY_CHANNEL_KEY);
        attr.setIfAbsent(connChannel);
        logger.info("client："+incoming.remoteAddress()+"connected Connection channel");

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
		cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {
		Channel incoming=ctx.channel();
		logger.info("client："+incoming.remoteAddress()+"disconnected");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
		Channel incoming=ctx.channel();
        logger.info("client："+incoming.remoteAddress()+"online");


    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
		Channel incoming=ctx.channel();
		logger.info("client："+incoming.remoteAddress()+"offline");
    }

}
