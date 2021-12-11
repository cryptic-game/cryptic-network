package net.cryptic_game.microservice;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;

public class MicroServiceInitializer extends ChannelInitializer<SocketChannel> {

    private final SimpleChannelInboundHandler<String> handler;

    MicroServiceInitializer(SimpleChannelInboundHandler<String> handler) {
        this.handler = handler;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast(new JsonObjectDecoder());
        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
        pipeline.addLast(this.handler);
    }

}
