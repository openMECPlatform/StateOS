package channel;

import Server.OperationManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:14
 * @Description:
 */
public class ActionChannelHandler extends BaseChannelHandler {
    protected static Logger logger = LoggerFactory.getLogger(ActionChannelHandler.class);

    public ActionChannelHandler(OperationManager operationManager) {
        super(operationManager);
    }


    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel incoming=ctx.channel();
        ActionChannel actionChannel = new ActionChannel(incoming, this.operationManager);
        Attribute<BaseChannel> attr = ctx.attr(AttributeMapConstant.NETTY_CHANNEL_KEY);
        attr.setIfAbsent(actionChannel);
       
        logger.info("client："+incoming.remoteAddress()+"connected action channel");

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
