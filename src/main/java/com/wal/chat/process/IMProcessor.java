package com.wal.chat.process;

import com.alibaba.fastjson.JSONObject;
import com.wal.chat.protocol.IMDecoder;
import com.wal.chat.protocol.IMEncoder;
import com.wal.chat.protocol.IMMessage;
import com.wal.chat.protocol.IMP;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author wal
 * @date 2018/12/19
 */
public class IMProcessor {

    private final static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private IMDecoder decoder = new IMDecoder();

    private IMEncoder encoder = new IMEncoder();

    private final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
    private final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
    private final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");

    public void logout(Channel client){
        onlineUsers.remove(client);
    }

    public void process(Channel client, IMMessage msg){
        process(client,encoder.encode(msg));
    }

    public void process (Channel client, String msg) {

        IMMessage request = decoder.decode(msg);
        if(null == request){ return;}

        String nickName = request.getSender();

        //判断如果是登录动作，就往onlineUsers中加入一条信息
        if(IMP.LOGIN.getName().equals(request.getCmd())){

            client.attr(NICK_NAME).getAndSet(request.getSender());
//            client.attr(IP_ADDR).getAndSet();

            onlineUsers.add(client);

            for(Channel channel : onlineUsers){
                if(channel != client){
                    request = new IMMessage(IMP.SYSTEM.getName(), systTime(), onlineUsers.size(), nickName+"加入聊天室");
                }else{
                    request = new IMMessage(IMP.SYSTEM.getName(), systTime(), onlineUsers.size(), "您已与服务器建立连接");
                }
                String text = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(text));
            }

        }else if(IMP.LOGOUT.getName().equals(request.getCmd())){
            onlineUsers.remove(client);
        }else if(IMP.CHAT.getName().equals(request.getCmd())){
            for(Channel channel : onlineUsers){
                if(channel != client){
                    request.setSender(client.attr(NICK_NAME).get());
                }else{
                    request.setSender("you");
                }
                String text = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(text));
            }
        }else if(IMP.FLOWER.getName().equals(request.getCmd())){

            //客户端发送鲜花命令
            JSONObject attrs = getAttrs(client);
            long currTime = systTime();
            if(null != attrs){
                long lastTime = attrs.getLongValue("lastFlowerTime");
                //60秒之内不允许重复刷鲜花
                int secends = 10;
                //小于10秒不能刷花
                long sub = currTime - lastTime;
                if(sub < 1000 * secends){
                    request.setSender("you");
                    request.setCmd(IMP.SYSTEM.getName());
                    request.setContent("您送鲜花太频繁，"+(secends - Math.round(sub / 1000)) + "秒后再试");
                    String content = encoder.encode(request);
                    client.writeAndFlush(new TextWebSocketFrame(content));
                    return;
                }
            }

            for(Channel channel : onlineUsers){
                if(channel == client){
                    request.setSender("you");
                    request.setContent("你给大家送了一波鲜花雨");
                    //设置最后一次刷鲜花的时间，刷鲜花不能太频繁
                    setAttrs(client,"lastFlowerTime", currTime);
                }else{
                    request.setSender(client.attr(NICK_NAME).get());
                    request.setContent(client.attr(NICK_NAME).get() + "送来一波鲜花雨");
                }
                request.setTime(systTime());

                String content = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }

    }


    /**
     * 获取用户昵称
     * @param client
     * @return
     */
    public String getNickName(Channel client){
        return client.attr(NICK_NAME).get();
    }
    /**
     * 获取用户远程IP地址
     * @param client
     * @return
     */
    public String getAddress(Channel client){
        return client.remoteAddress().toString().replaceFirst("/","");
    }

    private JSONObject getAttrs(Channel client){
        try{
            return client.attr(ATTRS).get();
        }catch (Exception e){
            return null;
        }
    }

    private void setAttrs(Channel client, String key, Object value){
        try{
            JSONObject json = client.attr(ATTRS).get();
            json.put(key,value);
            client.attr(ATTRS).set(json);
        }catch (Exception e){
            JSONObject json = new JSONObject();
            json.put(key,value);
            client.attr(ATTRS).set(json);
        }
    }


    private Long systTime(){
        return System.currentTimeMillis();
    }
}
