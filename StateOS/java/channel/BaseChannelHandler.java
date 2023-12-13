package channel;

import Server.OperationManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:14
 * @Description:
 */
public abstract  class BaseChannelHandler extends ChannelInboundHandlerAdapter {
    protected static Logger logger = LoggerFactory.getLogger(BaseChannelHandler.class);

    protected OperationManager operationManager;

    public BaseChannelHandler(OperationManager operationManager){
        this.operationManager =operationManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object)  throws Exception{
        Attribute<BaseChannel> attr = ctx.attr(AttributeMapConstant.NETTY_CHANNEL_KEY);
        BaseChannel baseChannel = attr.get();

        try {
            baseChannel.processMessage(object);

        }catch (Exception e ){
            e.printStackTrace();
        }
    }

    @Override
    public abstract  void handlerAdded(ChannelHandlerContext ctx) throws Exception;
}
