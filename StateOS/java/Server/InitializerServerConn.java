package Server;

import channel.ConnectionChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import proto.MyConnMessageProto;

/**
 * @Author: Chenglin Ding
 * @Date: 27.01.2021 11:35
 * @Description:
 */
public class InitializerServerConn extends ChannelInitializer<SocketChannel> {
    private OperationManager operationManager;

    public InitializerServerConn(OperationManager operationManager) {
        this.operationManager = operationManager;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception
    {
        ChannelPipeline pipeline=socketChannel.pipeline();
        pipeline.addLast("seperate data head",new ProtobufVarint32FrameDecoder());
        pipeline.addLast("proto decoder",  new ProtobufDecoder(MyConnMessageProto.MyConnMessage.getDefaultInstance()));
        pipeline.addLast("add data head",new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast("proto encoder",new ProtobufEncoder());
        pipeline.addLast("handler", new ConnectionChannelHandler(this.operationManager));

    }
}
