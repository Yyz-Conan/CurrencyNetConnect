package connect.network.udp;

import connect.network.base.AbstractFactory;
import connect.network.tcp.TcpClientFactory;
import connect.network.tcp.TcpReceive;
import connect.network.tcp.TcpSender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class UdpClientFactory extends AbstractFactory<UdpClientTask> {

    private static UdpClientFactory mFactory;
    private InetSocketAddress address = null;

    private UdpClientFactory() {
    }


    public synchronized static UdpClientFactory getFactory() {
        if (mFactory == null) {
            synchronized (UdpClientFactory.class) {
                if (mFactory == null) {
                    mFactory = new UdpClientFactory();
                }
            }
        }
        return mFactory;
    }


    public static void destroy() {
        mFactory = null;
    }

    @Override
    protected boolean onConnectTask(UdpClientTask task) {
        address = new InetSocketAddress(task.getHost(), task.getPort());
        try {
            DatagramSocket socket = null;
            if (task.isServer()) {
                //多播UDP
                if (task.isBroadcast() && task.getLiveTime() != null) {
                    //单播UDP服务端
                    MulticastSocket multicastSocket = new MulticastSocket(address);
                    multicastSocket.setTimeToLive(task.getLiveTime().getTtl());
                    socket = multicastSocket;
                } else {
                    //单播UDP服务端
                    socket = new DatagramSocket(address);
                }
                //0x02 成本低, 0x04 可靠性,0x08 通过输入 ,0x10 低延迟
                socket.setTrafficClass(0x02);
            } else {
                //多播UDP
                if (task.isBroadcast() && task.getLiveTime() != null) {
                    //单播UDP客户端
                    MulticastSocket multicastSocket = new MulticastSocket();
                    multicastSocket.setTimeToLive(task.getLiveTime().getTtl());
                    socket = multicastSocket;
                } else {
                    //单播UDP客户端
                    socket = new DatagramSocket();
                }
                socket.setTrafficClass(0x10);
            }
            socket.setSoTimeout(100);
            task.setSocket(socket);
            task.onConfigSocket(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onExecTask(UdpClientTask task) {
        UdpSender sender = task.getSender();
        UdpReceive receive = task.getReceive();
        if (receive != null) {
            try {
                receive.onRead(task.getSocket());
            } catch (Throwable e) {
                removeTask(task);
                e.printStackTrace();
            }
        }
        if (sender != null) {
            try {
                sender.onWrite(task.getSocket(), address);
            } catch (Throwable e) {
                removeTask(task);
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDisconnectTask(UdpClientTask task) {
        try {
            task.onCloseSocket();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatagramSocket socket = task.getSocket();
            if (socket != null) {
                socket.close();
            }
        }
    }
}