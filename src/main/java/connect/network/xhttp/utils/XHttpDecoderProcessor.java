package connect.network.xhttp.utils;

import connect.network.base.joggle.INetReceiver;
import connect.network.xhttp.XMultiplexCacheManger;
import connect.network.xhttp.entity.XReceiverMode;
import connect.network.xhttp.entity.XReceiverStatus;
import connect.network.xhttp.entity.XResponse;
import log.LogDog;
import util.StringEnvoy;

import java.util.Map;

public class XHttpDecoderProcessor implements INetReceiver<MultilevelBuf> {

    /**
     * 接收体（结果）
     */
    protected XResponse response;

    /**
     * 当前模式
     */
    private XReceiverMode mode = XReceiverMode.RESPONSE;
    /**
     * 当前解析状态
     */
    private XReceiverStatus status = XReceiverStatus.HEAD;

    /**
     * body数据是否是分段传输
     */
    private boolean isSubsection = false;
    /**
     * body数据大小
     */
    private int bodySize = -1;
    /**
     * 协议头分隔符的位置
     */
    private int headEndIndex;

    private INetReceiver<XResponse> dataReceiver;

    public XHttpDecoderProcessor() {
        response = new XResponse();
    }

    public void setDataReceiver(INetReceiver<XResponse> receiver) {
        this.dataReceiver = receiver;
    }

    public XReceiverStatus getStatus() {
        return status;
    }

    public XReceiverMode getMode() {
        return mode;
    }

    public void setMode(XReceiverMode mode) {
        this.mode = mode;
    }


    @Override
    public void onReceiveFullData(MultilevelBuf buf, Throwable e) {
        byte[] data = buf.array();
        if (data != null) {
            int len = data.length;
            onHttpReceive(data, len, e);
        } else {
            LogDog.w("## http receiver data is null !!!");
        }
        XMultiplexCacheManger.getInstance().lose(buf);
    }


    protected void onHttpReceive(byte[] data, int len, Throwable e) {
        if (mode == XReceiverMode.REQUEST) {
            onRequest(data, len, e);
        } else {
            onResponse(data, len, e);
        }
    }

    protected void onRequest(byte[] data, int len, Throwable e) {
        response.appendRawData(data);
        processHttpHead(data, len);
        processRequestBody(data, len);
        processNotify(e);
    }

    protected void onResponse(byte[] data, int len, Throwable e) {
        response.appendRawData(data);
        processHttpHead(data, len);
        processResponseBody(data, len);
        processNotify(e);
    }

    protected void onStatusChange(XReceiverStatus status) {
    }

    protected void processHttpHead(byte[] data, int len) {
        if (status == XReceiverStatus.HEAD) {
            headEndIndex = XResponseHelper.findHeadEndTag(data, len);
            if (headEndIndex != -1) {
                //找到协议头结束标志，则开始解析协议头
                analysisHead(data, headEndIndex);
                String length = response.getHeadForKey(XHttpProtocol.XY_CONTENT_LENGTH);
                if (StringEnvoy.isNotEmpty(length)) {
                    bodySize = Integer.parseInt(length);
                }
                String transfer = response.getHeadForKey(XHttpProtocol.XY_TRANSFER_ENCODING);
                isSubsection = StringEnvoy.isNotEmpty(transfer);
                status = XReceiverStatus.BODY;
                callStatusChange();
            }
        }
    }

    /**
     * 处理请求数据里面body数据
     *
     * @param data
     * @param len
     */
    protected void processRequestBody(byte[] data, int len) {
        if (status == XReceiverStatus.BODY) {
            if (bodySize != -1) {
                ByteCacheStream raw = response.getRawData();
                if (raw.size() - headEndIndex == bodySize) {
                    byte[] body = new byte[bodySize];
                    System.arraycopy(raw.getBuf(), headEndIndex, body, 0, body.length);
                    response.setHttpData(body);
                    status = XReceiverStatus.OVER;
                    callStatusChange();
                }
            } else {
                //没有请求体
                int index = XResponseHelper.findHeadEndTag(data, len);
                if (index != -1) {
                    status = XReceiverStatus.OVER;
                    callStatusChange();
                }
            }
        }
    }

    /**
     * 处理接收数据里面的body数据
     *
     * @param data
     * @param len
     */
    protected void processResponseBody(byte[] data, int len) {
        if (status == XReceiverStatus.BODY) {
            ByteCacheStream raw = response.getRawData();
            byte[] body = null;
            //分段传输方式
            if (isSubsection) {
                //查找0\r\n结束标志
                int index = XResponseHelper.findBodyEndTag(data, len);
                if (index != -1) {
                    body = new byte[raw.size() - 5 - headEndIndex];
                }
            } else {
                //有明确的数据大小
                if (raw.size() - headEndIndex == bodySize) {
                    body = new byte[bodySize];
                }
            }
            if (body != null) {
                System.arraycopy(raw.getBuf(), headEndIndex, body, 0, body.length);
                response.setHttpData(body);
                status = XReceiverStatus.OVER;
                callStatusChange();
            }
        }
    }

    private void processNotify(Throwable e) {
        if (status == XReceiverStatus.OVER) {
            if (dataReceiver != null) {
                dataReceiver.onReceiveFullData(response, e);
            }
            status = XReceiverStatus.NONE;
            callStatusChange();
            reset();
        }
    }


    private void reset() {
        headEndIndex = -1;
        status = XReceiverStatus.HEAD;
        response.reset();
    }

    private void callStatusChange() {
        try {
            onStatusChange(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void analysisHead(byte[] data, int index) {
        Map<String, String> headMap = response.getHttpHead();
        String headStr = new String(data, 0, index);
        String[] arrays = headStr.split("\r\n");
        //第一行不是键值对
        headMap.put(XHttpProtocol.XY_FIST_LINE, arrays[0]);
//        LogDog.d("============== head start =====================");
        for (int tmp = 1; tmp < arrays.length; tmp++) {
            String[] kv = arrays[tmp].split(": ");
            if (kv.length == 2) {
                headMap.put(kv[0], kv[1]);
//                LogDog.d(kv[0] + ":" + kv[1]);
            }
        }
//        LogDog.d("=============== head end ====================\r\n");
    }

}
