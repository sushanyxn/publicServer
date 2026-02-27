package com.slg.robot.core.net;

import com.slg.net.message.core.manager.MessageHandlerManager;
import com.slg.net.message.core.model.MessageHandlerMeta;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import com.slg.robot.SpringContext;
import com.slg.common.executor.Executor;
import com.slg.robot.core.model.Robot;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;

/**
 * 机器人消息处理器
 * 处理从服务器接收到的消息
 *
 * @author yangxunan
 * @date 2026/01/22
 */
@Component("webSocketClientMessageHandler")
public class RobotMessageHandler implements WebSocketMessageHandler {

    @Override
    public void onConnect(ChannelHandlerContext ctx){

    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, Object message){
        MessageHandlerMeta meta = MessageHandlerManager.getInstance().getHandler(message.getClass());
        if (meta != null){
            MethodHandle methodHandle = meta.getMethodHandle();
            Object bean = meta.getHandlerBean();
            NetSession session = NetSession.getSession(ctx.channel());
            Robot robot = SpringContext.getRobotManager().getRobot(session.getPlayerId());
            if (robot != null){
                Executor.Robot.execute(robot.getPlayerId(), () -> {
                    try {
                        methodHandle.invoke(bean, session, message, robot);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @Override
    public void onDisconnect(ChannelHandlerContext ctx){

    }

    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause){

    }
}

