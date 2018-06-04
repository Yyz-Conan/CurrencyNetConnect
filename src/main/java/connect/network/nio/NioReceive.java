package connect.network.nio;


import connect.network.base.Interface.IReceive;
import task.utils.IoUtils;
import task.utils.ThreadAnnotation;

import java.nio.channels.SocketChannel;

public class NioReceive implements IReceive {

    private Object mReceive;
    private String mReceiveMethodName;

    public NioReceive(Object receive, String receiveMethodName) {
        this.mReceive = receive;
        this.mReceiveMethodName = receiveMethodName;
    }


    @Override
    public void setReceive(Object receive, String receiveMethodName) {
        this.mReceive = receive;
        this.mReceiveMethodName = receiveMethodName;
    }

    /**
     * 读取输入流数据
     *
     * @return 成功返回读取到内容的长度, 返回-1或者0说明读取结束或者有异常
     */
    protected void onRead(SocketChannel channel) {
        byte[] data = IoUtils.tryRead(channel);
        if (data != null) {
            ThreadAnnotation.disposeMessage(mReceiveMethodName, mReceive, data);
        }
    }
}
