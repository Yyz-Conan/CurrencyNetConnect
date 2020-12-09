package connect.network.aio;


import connect.network.base.AbsNetFactory;
import connect.network.base.BaseNetWork;
import connect.network.base.joggle.ISSLFactory;
import connect.network.ssl.TLSHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AioNetWork<T extends AioClientTask> extends BaseNetWork<T> implements CompletionHandler<Void, T> {

    private ISSLFactory mSslFactory;
    private AbsNetFactory mNetFactory;

    protected AioNetWork(ISSLFactory sslFactory, AbsNetFactory netFactory) {
        this.mSslFactory = sslFactory;
        this.mNetFactory = netFactory;
    }

    @Override
    protected void onCheckConnectTask() {
        super.onCheckConnectTask();
    }

    @Override
    protected void onConnectTask(T task) {
        try {
            AsynchronousSocketChannel channel = AsynchronousSocketChannel.open(task.getChannelGroup());
            //Disable the Nagle algorithm
            channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            //Keep connection alive
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            //Re-use address
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            task.setSocketChannel(channel);
            task.setFactory(mNetFactory);
            task.onConfigClientChannel(channel);
            channel.connect(new InetSocketAddress(task.getHost(), task.getPort()), task, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initSSLConnect(T task) throws Exception {
        if (task.isTLS()) {
            SSLContext sslContext = mSslFactory.getSSLContext();
            SSLEngine sslEngine = sslContext.createSSLEngine(task.getHost(), task.getPort());
            sslEngine.setUseClientMode(true);
            sslEngine.setEnableSessionCreation(true);
            task.onHandshake(sslEngine, task.getChannel());
        }
    }

    @Override
    protected void onCheckRemoverTask() {
        super.onCheckRemoverTask();
    }

    @Override
    protected void onRecoveryTaskAll() {
        super.onRecoveryTaskAll();
    }

    @Override
    protected void onDisconnectTask(T task) {
        try {
            task.onCloseClientChannel();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                task.getChannel().close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                TLSHandler tlsHandler = task.getTlsHandler();
                if (tlsHandler != null) {
                    tlsHandler.release();
                }
                onRecoveryTask(task);
            }
        }
    }

    @Override
    public void completed(Void result, T task) {
        try {
            initSSLConnect(task);
            task.onConnectCompleteChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void failed(Throwable exc, T task) {
        try {
            task.onConnectError(exc);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            removerTaskImp(task);
        }
    }
}
