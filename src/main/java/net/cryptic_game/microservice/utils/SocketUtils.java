package net.cryptic_game.microservice.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;
import net.cryptic_game.microservice.error.ServerError;
import org.json.simple.JSONObject;

public class SocketUtils {

    public static void send(Channel channel, JSONObject obj) {
        channel.writeAndFlush(Unpooled.copiedBuffer(obj.toString(), CharsetUtil.UTF_8));
    }

    public static void sendError(Channel channel, ServerError error) {
        send(channel, error.getResponse());
    }

}
