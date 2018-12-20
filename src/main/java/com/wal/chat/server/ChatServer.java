package com.wal.chat.server;

import com.wal.chat.protocol.IMDecoder;
import com.wal.chat.protocol.IMEncoder;
import com.wal.chat.server.handler.HttpHandler;
import com.wal.chat.server.handler.SocketHandler;
import com.wal.chat.server.handler.WebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author wal
 * @date 2018/12/12
 */
public class ChatServer {
    private int port = 9999;
    public void start(){
        //BOSS线程
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //Worker线程
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //启动引擎
            ServerBootstrap b = new ServerBootstrap();
            //主从模型
            b.group(bossGroup, workerGroup)
                    //主线程处理类
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)    //针对主线程的配置
                    //子线程处理，Handler
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel client) throws Exception {
                            //所有自定义的业务从这开始
                            ChannelPipeline pipeline = client.pipeline();

                            //===========   支持自定义Socket协议  ==============
                            pipeline.addLast(new IMDecoder());
                            pipeline.addLast(new IMEncoder());
                            pipeline.addLast(new SocketHandler());

                            //===========   这里他是用来支持HTTP协议  ==============
                            /* 解码和编码HTTP请求的 */
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                            //用于处理文件流的一个Handler
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new HttpHandler());

                            //===========   支持WebSocket协议  ==============
                            pipeline.addLast(new WebSocketServerProtocolHandler("/im"));
                            pipeline.addLast(new WebSocketHandler());



                        }
                    });

                    //配置信息
                    //.option(ChannelOption.SO_BACKLOG, 128)    //针对主线程的配置
                    //.childOption(ChannelOption.SO_KEEPALIVE, true); //子线程的配置

            // 等待客户端连接
            ChannelFuture f = b.bind(this.port).sync();

            System.out.println("服务已启动，监听端口" + this.port);

            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}
