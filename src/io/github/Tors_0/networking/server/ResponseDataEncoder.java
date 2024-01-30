package io.github.Tors_0.networking.server;

import io.github.Tors_0.networking.ResponseData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ResponseDataEncoder extends MessageToByteEncoder<ResponseData> {
    private final Charset charset = StandardCharsets.UTF_8;
    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseData msg, ByteBuf out) {
        out.writeInt(msg.getIntValue());
        out.writeInt(msg.getStringValue().length());
        out.writeCharSequence(msg.getStringValue(), charset);
    }
}
