package io.github.Tors_0.networking.client;

import io.github.Tors_0.networking.ResponseData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ResponseDataDecoder extends ReplayingDecoder<ResponseData> {

    private final Charset charset = StandardCharsets.UTF_8;
    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf in, List<Object> out) throws Exception {

        ResponseData data = new ResponseData();
        data.setIntValue(in.readInt());
        int strLen = in.readInt();
        data.setStringValue(
                in.readCharSequence(strLen, charset).toString());
        out.add(data);
    }
}
