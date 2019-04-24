package com.pancm.server;

import com.pancm.protobuf.RequestFile;
import com.pancm.protobuf.ResponseFile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private final Logger log = LoggerFactory.getLogger(NettyServerHandler.class);

    private final String LOGNAME = "netty.log";

    private final String LOGSIZENAME = "netty.log.size";

    /**
     * 空闲次数
     */
    private int IDLECOUNT = 1;

    /**
     * 建立连接时，发送一条消息
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("连接的客户端地址:" + ctx.channel().remoteAddress());
        // UserInfo.UserMsg userMsg = UserInfo.UserMsg.newBuilder().setId(1).setAge(18).setName("xuwujing").setState(0).build();
        // ctx.writeAndFlush(userMsg);
        super.channelActive(ctx);
    }

    /**
     * 超时处理 如果5秒没有接受客户端的心跳，就触发; 如果超过两次，则直接关闭;
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {
        if (obj instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) obj;
            // 如果读通道处于空闲状态，说明没有接收到心跳命令
            if (IdleState.READER_IDLE.equals(event.state())) {
                log.info("已经5秒没有接收到客户端的信息了");
                if (IDLECOUNT > 1) {
                    log.info("关闭这个不活跃的channel");
                    ctx.channel().close();
                }
                IDLECOUNT++;
            }
        } else {
            super.userEventTriggered(ctx, obj);
        }
    }

    /**
     * 业务逻辑处理
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RequestFile.RequestMsg) {
            RequestFile.RequestMsg request = (RequestFile.RequestMsg) msg;
            log.info(request.toString());
            byte[] bytes = request.getBytes().toByteArray();
            int byteRead = request.getEndPos();
            long fileSize = request.getFileSize();
            long start = request.getStarPos();
            String fileName = request.getFileName();
            String filePath = request.getFilePath();

            String newFilePath = filePath.replace(fileName, LOGNAME);
            File file = new File(newFilePath);
            if (start == 0 && file.exists()) { // 只有在文件传输完成时或者client重连时会走到。
                // log.info("file exists:" + request.getFileName() + "--" + "[" + ctx.channel().remoteAddress() + "]");
                long completedByteLength = Long.parseLong(readFile(filePath.replace(fileName, LOGSIZENAME)));
                start = completedByteLength;
                if (completedByteLength == request.getFileSize()) {
                    ResponseFile.ResponseMsg responseFile = ResponseFile.ResponseMsg.newBuilder()
                            .setEnd(true).setStart(start).setCompletedByte(String.valueOf(completedByteLength)).build();
                    ctx.writeAndFlush(responseFile);
                } else {
                    ResponseFile.ResponseMsg responseFile = ResponseFile.ResponseMsg.newBuilder()
                            .setEnd(false).setStart(completedByteLength).setCompletedByte(String.valueOf(completedByteLength)).build();
                    ctx.writeAndFlush(responseFile);
                }
                return;
            }

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            randomAccessFile.seek(start);
            randomAccessFile.write(bytes);
            start = start + byteRead;

            if (byteRead > 0 && (start < fileSize && fileSize != -1)) {
                ResponseFile.ResponseMsg responseFile = ResponseFile.ResponseMsg.newBuilder()
                        .setEnd(false).setStart(start).setCompletedByte(String.valueOf(start)).build();
                ctx.writeAndFlush(responseFile);
                randomAccessFile.close(); //写磁盘
                writeFile(filePath.replace(fileName, LOGSIZENAME), Long.toString(start));
            } else {
                // log.info("create file success:" + request.getFileName() + "--" + "[" + ctx.channel().remoteAddress() + "]");
                ResponseFile.ResponseMsg responseFile = ResponseFile.ResponseMsg.newBuilder()
                        .setEnd(true).setStart(start).setCompletedByte(String.valueOf(fileSize)).build();
                ctx.writeAndFlush(responseFile);

                randomAccessFile.close(); //写磁盘
                writeFile(filePath.replace(fileName, LOGSIZENAME), Long.toString(start));
                // ctx.close();  //这步让客户端来做
            }
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void writeFile(String fileName, String content) {
        try {
            // 打开一个随机访问文件流，按读写方式
            RandomAccessFile randomFile = new RandomAccessFile(fileName, "rw");
            // 将写文件指针移到文件尾。
            randomFile.seek(0);
            randomFile.writeBytes(content);
            randomFile.close();
        } catch (IOException e) {
            log.error("writeFile error: ", e);
        }
    }

    private String readFile(String fileName) {
        String line = null;
        try {
            // 打开一个随机访问文件流，按读写方式
            RandomAccessFile randomFile = new RandomAccessFile(fileName, "rw");
            line = randomFile.readLine();
            randomFile.close();

        } catch (IOException e) {
            log.error("readFile error: ", e);
        }
        return line;
    }
}
