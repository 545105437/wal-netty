package com.wal.chat.server.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author wal
 * @date 2018/12/12
 */
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    //classPath
    private URL baseURL = HttpHandler.class.getProtectionDomain().getCodeSource().getLocation();

    private final String WEB_ROOT = "webroot";

    private File getFileFromRoot(String fileName) throws URISyntaxException {
        String path = baseURL.toURI() + WEB_ROOT + "/" + fileName;
        path = !path.startsWith("file:") ? path : path.substring(5);
        path = path.replaceAll("//","/");
        return new File(path);
    }

    @Override
    //read0  netty源码里面  只要后面加了个0的，都是实现类的方法，不是接口
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //获取到了客户端请求的url
        String uri = request.getUri();

        RandomAccessFile file = null;
        try {
            String page = uri.equals("/") ? "chat.html" : uri;
            file = new RandomAccessFile(getFileFromRoot(page), "r");
        }catch (Exception e){
            ctx.fireChannelRead(request.retain());
            return;
        }

        HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
        String contextType = "text/html;";

        if(uri.endsWith(".css")){
            contextType = "text/css;";
        }else if(uri.endsWith(".js")){
            contextType = "text/javascript;";
        }else if(uri.toLowerCase().matches("(jpg|png|gif)$")){
            String ext = uri.substring(uri.lastIndexOf("."));
            contextType = "images/"+ ext ;
        }

        response.headers().set(HttpHeaders.Names.CONTENT_TYPE,contextType + "charset=utf-8;");

        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length());
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        ctx.write(response);

        ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
//        ctx.write(new ChunkedNioFile(file.getChannel()));

        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

        file.close();


        //System.out.println(file);
    }
}
