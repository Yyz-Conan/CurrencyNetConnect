package connect.network.udp;


import task.executor.BaseConsumerTask;
import task.executor.TaskContainer;
import task.executor.interfaces.IConsumerTaskExecutor;
import task.executor.interfaces.ILoopTaskExecutor;
import task.utils.Logcat;

import java.net.*;

/**
 * UDP通信类
 * Created by dell on 8/22/2017.
 *
 * @author yyz
 */
public class JavUdpConnect {

    private CoreTask coreTask = null;
    private boolean isServer = false;
    private boolean isBroadcast = false;
    private LIVE_TIME ttl = LIVE_TIME.LOCAL_AREA;
    private String ip = null;
    private boolean isShutdownReceive = false;
    private int port = 0;

    public enum LIVE_TIME {
        LOCAL_NETWORK(1), NETWORK_SITE(32), LOCAL_AREA(64), LOCAL_CONTINENT(128), EVERYWHERE(255);

        int ttl = 1;

        LIVE_TIME(int ttl) {
            this.ttl = ttl;
        }

        public int getTtl() {
            return ttl;
        }
    }

    public JavUdpConnect(String ip, int port) {
        init(ip, port, false, false, null);
    }

    public JavUdpConnect(String ip, int port, boolean isServer) {
        init(ip, port, isServer, false, null);
    }

    public JavUdpConnect(boolean isBroadcast, String ip, int port) {
        init(ip, port, isServer, false, null);
    }

    public JavUdpConnect(String ip, int port, boolean isServer, boolean isBroadcast, LIVE_TIME ttl) {
        init(ip, port, isServer, isBroadcast, ttl);
    }

    private void init(String ip, int port, boolean isServer, boolean isBroadcast, LIVE_TIME ttl) {
        this.ip = ip;
        this.port = port;
        this.isBroadcast = isBroadcast;
        this.ttl = ttl;
        coreTask = new CoreTask();
        this.isServer = isServer;
    }

    public int getLocalPort() {
        return coreTask.getLocalPort();
    }

    public void refreshDestAddress(String ip, int port) {
        if (ip != null && port > 0) {
            this.ip = ip;
            this.port = port;
            coreTask.refreshDestAddress();
        }
    }

    public void refreshDestAddress(int port) {
        if (port > 0) {
            this.port = port;
            coreTask.refreshDestAddress();
        }
    }

    public void putSendData(byte[] data) {
        putSendData(data, data.length);
    }

    public void putSendData(byte[] data, int length) {
        IConsumerTaskExecutor executor = coreTask.getContainer().getTaskExecutor();
        executor.pushToCache(new TaskEntity(data, length));
    }

    /**
     * 关闭接收流
     *
     * @param shutdown
     */
    public void setShutdownReceive(boolean shutdown) {
        this.isShutdownReceive = shutdown;
    }

    protected void onReceiveData(DatagramPacket packet) {
        Logcat.i("==> onReceiveData = packet.getLength()" + packet.getLength());
    }

    /**
     * 每次socket建立链接成功后回调一次
     */
    protected void onConnectSuccess() {
    }

    /**
     * 每次socket建立链接失败后回调一次
     */
    protected void onConnectFailure() {
    }

    /**
     * 线程即将结束，在socket没关闭之前的回调
     */
    protected void onCloseSocket() {
    }

    private class TaskEntity {
        public int length;
        public byte[] data;

        TaskEntity(byte[] data, int length) {
            this.data = data;
            this.length = length;
        }
    }

    public void setMaxCache(int count) {
        IConsumerTaskExecutor executor = coreTask.getContainer().getTaskExecutor();
        executor.setCacheMaxCount(count);
    }

    private class CoreTask extends BaseConsumerTask<TaskEntity> {
        private DatagramPacket packet = null;
        private DatagramSocket socket = null;
        private InetSocketAddress address = null;
        private TaskContainer container;
        private IConsumerTaskExecutor consumerTaskExecutor;

        public CoreTask() {
            container = new TaskContainer(this);
            consumerTaskExecutor = container.getTaskExecutor();
        }

        public TaskContainer getContainer() {
            return container;
        }

        public void refreshDestAddress() {
            address = new InetSocketAddress(ip, port);
            if (packet != null) {
                packet.setSocketAddress(address);
            }
        }

        public int getLocalPort() {
            ILoopTaskExecutor executor = container.getTaskExecutor();
            if (executor.isStartState()) {
                while (!executor.getLoopState() && socket == null) {
                    executor.waitTask(0);
                }
            }
            if (socket == null) {
                return 0;
            }
            return socket.getLocalPort();
        }

        @Override
        protected void onInitTask() {
            try {
                address = new InetSocketAddress(ip, port);
                if (isBroadcast && ttl != null) {
                    //多播UDP
                    MulticastSocket multicastSocket;
                    if (isServer) {
                        //单播UDP服务端
                        multicastSocket = new MulticastSocket(address);
                    } else {
                        //单播UDP客户端
                        multicastSocket = new MulticastSocket();
                    }
                    multicastSocket.setTimeToLive(ttl.getTtl());
                    socket = multicastSocket;
                }
                if (isServer) {
                    //单播UDP服务端
                    if (socket == null) {
                        socket = new DatagramSocket(address);
                    }
                    //0x02 成本低, 0x04 可靠性,0x08 通过输入 ,0x10 低延迟
                    socket.setTrafficClass(0x02);
                } else {
                    //单播UDP客户端
                    if (socket == null) {
                        socket = new DatagramSocket();
                    }
                    socket.setTrafficClass(0x10);
                }
                container.getTaskExecutor().resumeTask();
                socket.setSoTimeout(3000);

                if (isShutdownReceive) {
                    consumerTaskExecutor.setIdleStateSleep(true);
                } else {
                    consumerTaskExecutor.startAsyncProcessData();
                }
                onConnectSuccess();
            } catch (Exception e) {
                e.printStackTrace();
                onConnectFailure();
                socket.close();
                container.getTaskExecutor().stopTask();
            }
        }

        @Override
        protected void onCreateData() {
            if (isShutdownReceive) {
                return;
            }
            DatagramPacket receive;
            try {
                int size = socket.getReceiveBufferSize();
                byte[] buffer = new byte[size];
                receive = new DatagramPacket(buffer, buffer.length);
                socket.receive(receive);
            } catch (Exception e) {
                receive = null;
                if (e instanceof SocketTimeoutException == false) {
                    e.printStackTrace();
                    container.getTaskExecutor().stopTask();
                }
            }
            if (receive != null) {
                onReceiveData(receive);
            }
        }

        @Override
        protected void onProcess() {
            TaskEntity entity = (TaskEntity) consumerTaskExecutor.popCacheData();
            if (entity == null) {
                return;
            }
            try {
                if (packet == null) {
                    packet = new DatagramPacket(entity.data, entity.length, address);
                } else {
                    packet.setData(entity.data);
                    packet.setLength(entity.length);
                }
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @Override
        protected void onDestroyTask() {
            onCloseSocket();
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }

    }

    public void pauseConnect() {
        coreTask.getContainer().getTaskExecutor().pauseTask();
    }

    public void continueConnect(boolean isCleanCache) {
        if (isCleanCache) {
            IConsumerTaskExecutor executor = coreTask.getContainer().getTaskExecutor();
            executor.clearCacheData();
        }
        coreTask.getContainer().getTaskExecutor().resumeTask();
    }

    public boolean isPauseConnect() {
        return coreTask.getContainer().getTaskExecutor().getPauseState();
    }

    public void startConnect() {
        coreTask.getContainer().getTaskExecutor().startTask();
    }

    public void stopConnect() {
        coreTask.getContainer().getTaskExecutor().stopTask();
    }
}
