package com.wal.chat.server.handler;

import com.wal.chat.process.IMProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author wal
 * @date 2018/12/12
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame>{

    private IMProcessor processor = new IMProcessor();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        processor.process(ctx.channel(),msg.text());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("有人退出");
        processor.logout(ctx.channel());
    }
}
