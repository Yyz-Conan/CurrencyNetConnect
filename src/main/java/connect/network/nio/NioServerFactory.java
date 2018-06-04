package connect.network.nio;


import task.utils.Logcat;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * nio服务端工厂(单线程管理多个ServerSocket)
 *
 * @author yyz
 * @version 1.0
 */
public class NioServerFactory extends AbstractNioFactory<NioServerTask> {

    private static NioServerFactory mFactory = null;

    public static NioServerFactory getFactory() throws IOException {
        if (mFactory == null) {
            synchronized (NioClientFactory.class) {
                if (mFactory == null) {
                    mFactory = new NioServerFactory();
                }
            }
        }
        return mFactory;
    }

    private NioServerFactory() throws IOException {
        super();
    }

    @Override
    public void addTask(NioServerTask task) {
        super.addTask(task);
        Logcat.d("==##> NioServerFactory addTask mConnectCache.size = " + mConnectCache.size());
    }

    @Override
    public void close() {
        super.close();
        mFactory = null;
    }


    @Override
    protected void onConnectTask(Selector selector, NioServerTask task) {
        //创建服务，并注册到selector，监听所有的事件
        Logcat.d("==##> NioServerFactory onConnectTask NioServerTask = " + task.getSocketAddress().toString());
        ServerSocketChannel serverSocketChannel = task.getSocketChannel();
        if (serverSocketChannel == null) {
            try {
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(task.getSocketAddress());
                task.setSocketChannel(serverSocketChannel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            if (serverSocketChannel.isBlocking()) {
                serverSocketChannel.configureBlocking(false);
            }
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            serverSocketChannel.keyFor(selector).attach(task);
            task.onOpenServer(true);
        } catch (Exception e) {
            e.printStackTrace();
            task.onOpenServer(false);
            mDestroyCache.add(task);
        }
    }


    @Override
    protected void onSelectorTask(Selector selector) {

        Logcat.d("==##> NioServerFactory onSelectorTask");

        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            NioServerTask task = (NioServerTask) selectionKey.attachment();

            if (selectionKey.isValid() && selectionKey.isAcceptable()) {
                Logcat.d("==##> selectionKey isAcceptable ");
                try {
                    SocketChannel channel = serverSocketChannel.accept();
                    task.onAccept(channel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        iterator.remove();// 删除已处理过的事件
    }

    @Override
    protected void onDisconnectTask(NioServerTask task) {
        task.onCloseServer();
    }
}
