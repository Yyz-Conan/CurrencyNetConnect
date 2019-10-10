package connect.network.nio;


import connect.network.base.BaseNetTask;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * nio服务端任务(创建服务端任务)
 *
 * @author yyz
 * @version 1.0
 */
public class NioServerTask extends BaseNetTask {

    private String mHost;
    private int mPort;
    private ServerSocketChannel mChannel = null;
    private int mMaxConnect = 50;

    public NioServerTask() {
    }

    public NioServerTask(String host, int port) {
        setAddress(host, port);
    }

    public NioServerTask(ServerSocketChannel channel) {
        mChannel = channel;
    }

    //---------------------------- set ---------------------------------------

    public void setAddress(String host, int port) {
        this.mHost = host;
        this.mPort = port;
    }

    /**
     * 获取该任务要链接的服务器地址
     *
     * @param channel
     */
    public void setSocketChannel(ServerSocketChannel channel) {
        this.mChannel = channel;
    }

    public void setMaxConnect(int maxConnect) {
        this.mMaxConnect = mMaxConnect;
    }

    //---------------------------- get ---------------------------------------

    public int getServerPort() {
        return mPort;
    }

    public String getServerHost() {
        return mHost;
    }

    public ServerSocketChannel getSocketChannel() {
        return mChannel;
    }

    public int getMaxConnect() {
        return mMaxConnect;
    }

    //---------------------------- on ---------------------------------------

    protected void onConfigServer(ServerSocketChannel channel) {
    }

    protected void onAcceptServerChannel(SocketChannel channel) {
    }

    protected void onOpenServerChannel(boolean isSuccess) {
    }

    protected void onCloseServerChannel() {
    }

    /**
     * 当前状态服务彻底关闭，可以做资源回收工作
     */
    protected void onRecovery() {
    }
}
