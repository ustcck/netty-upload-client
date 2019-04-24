package com.pancm.client;

import com.google.protobuf.ByteString;
import com.pancm.protobuf.RequestFile;
import com.pancm.protobuf.ResponseFile;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Date;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private final Logger log = LoggerFactory.getLogger(NettyClientHandler.class);

    private int byteRead;
    private RequestFile.RequestMsg request;
    private RandomAccessFile randomAccessFile;
    private final int minReadBufferSize = 8192;

    /**
     * 建立连接时
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("建立连接时：" + new Date());
        sendRequest(ctx);
        ctx.fireChannelActive();
    }

    /**
     * 关闭连接时
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("关闭连接时：" + new Date());
        final EventLoop eventLoop = ctx.channel().eventLoop();
        new NettyClient().doConnect(new Bootstrap(), eventLoop);
        super.channelInactive(ctx);
    }

    /**
     * 业务逻辑处理
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ResponseFile.ResponseMsg) {
            ResponseFile.ResponseMsg response = (ResponseFile.ResponseMsg) msg;
            log.info(response.toString());
            if (response.getEnd()) {
                log.info("文件已经读完");
                sendRequest(ctx);
            } else {
                long start = response.getStart();
                randomAccessFile = new RandomAccessFile(request.getFilePath(), "r");
                randomAccessFile.seek(start);
                int a = (int) (randomAccessFile.length() - start);
                int sendLength = minReadBufferSize;
                if (a < minReadBufferSize) {
                    sendLength = a;
                }
                byte[] bytes = new byte[sendLength];
                if ((byteRead = randomAccessFile.read(bytes)) != -1 && (randomAccessFile.length() - start) > 0) {
                    File file = new File("D://LenovoWork/rrkd-file-server/test3.log");
                    RequestFile.RequestMsg request = RequestFile.RequestMsg.newBuilder()
                            .setFilePath("D://LenovoWork/rrkd-file-server/test3.log").setEndPos(byteRead)
                            .setFileName(file.getName()).setFileType(getSuffix(file.getName())).setStarPos(start)
                            .setBytes(ByteString.copyFrom(bytes)).setFileSize(randomAccessFile.length()).build();
                    try {
                        ctx.writeAndFlush(request);
                    } catch (Exception e) {
                        log.error("channelRead error: ", e);
                    }
                }
            }
        }
    }

    private void sendRequest(ChannelHandlerContext ctx) throws Exception {
        byte[] bytes = new byte[minReadBufferSize];
        File file = new File("D://LenovoWork/rrkd-file-server/test3.log");
        randomAccessFile = new RandomAccessFile(file, "r");
        randomAccessFile.seek(0);
        if ((byteRead = randomAccessFile.read(bytes)) != -1) {
            request = RequestFile.RequestMsg.newBuilder()
                    .setFilePath("D://LenovoWork/rrkd-file-server/test3.log")
                    .setFileName(file.getName()).setFileType(getSuffix(file.getName())).setStarPos(0)
                    .setEndPos(byteRead).setBytes(ByteString.copyFrom(bytes)).setFileSize(randomAccessFile.length()).build();
        }
        ctx.writeAndFlush(request);
    }

    private String getSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

}

