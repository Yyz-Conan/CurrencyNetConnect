package connect.network.xhttp.config;

import connect.network.xhttp.entity.XRequest;
import connect.network.xhttp.entity.XResponse;
import connect.network.xhttp.joggle.IXHttpResponseConvert;
import connect.network.xhttp.utils.ByteCacheStream;
import connect.network.xhttp.utils.XHttpProtocol;
import connect.network.xhttp.utils.XResponseHelper;
import json.JsonEnvoy;
import storage.GZipUtils;
import util.StringEnvoy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class XHttpDefaultResponseConvert implements IXHttpResponseConvert {

    @Override
    public void handlerEntity(XRequest request, XResponse response) {
        String encode = response.getHeadForKey(XHttpProtocol.XY_CONTENT_ENCODING);
        String transfer = response.getHeadForKey(XHttpProtocol.XY_TRANSFER_ENCODING);
        if (StringEnvoy.isNotEmpty(transfer) && "chunked".equals(transfer)) {
            //有分段,数据交给上层处理
            byte[] httpData = response.getHttpData();
            int chunkedIndex = 0;
            int startIndex = 0;
            int tagIndex;
            ByteCacheStream result = new ByteCacheStream();
            do {
                tagIndex = XResponseHelper.findChunkedTag(httpData, chunkedIndex);
                if (tagIndex != -1) {
                    String strSize = new String(httpData, chunkedIndex, tagIndex - chunkedIndex - 2);
                    int size = Integer.parseInt(strSize, 16);
                    chunkedIndex += size + 4 + strSize.length();
                    startIndex += strSize.length() + 2;

                    byte[] tmp = new byte[size];
                    System.arraycopy(httpData, startIndex, tmp, 0, size);
                    startIndex += size + 2;
                    try {
                        result.write(tmp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } while (tagIndex != -1);
            if (StringEnvoy.isNotEmpty(encode) && encode.contains("gzip")) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(result.getBuf(), 0, result.size());
                ByteCacheStream unZipResult = new ByteCacheStream();
                GZIPInputStream unzip = null;
                byte[] cache = new byte[1024];
                int len;
                try {
                    unzip = new GZIPInputStream(inputStream, result.size());
                    do {
                        len = unzip.read(cache);
                        if (len > 0) {
                            unZipResult.write(cache, 0, len);
                        }
                    } while (len > 0);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (unzip != null) {
                        try {
                            unzip.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    response.setHttpData(unZipResult.toByteArray());
                }
            } else {
                response.setHttpData(result.toByteArray());
            }
        } else {
            if (StringEnvoy.isNotEmpty(encode) && encode.contains("gzip")) {
                //需要解压
                response.setHttpData(GZipUtils.unCompress(response.getHttpData()));
            }
        }
        Object resultType = request.getResultType();
        if (resultType instanceof Class && !isBasicDataType((Class) resultType)) {
            response.setResult(JsonEnvoy.toEntity((Class) resultType, new String(response.getHttpData())));
        }
    }

    private boolean isBasicDataType(Class clx) {
        return clx == Integer.class || clx == int.class || clx == int[].class
                || clx == Long.class || clx == long.class || clx == long[].class
                || clx == Double.class || clx == double.class || clx == double[].class
                || clx == Float.class || clx == float.class || clx == float[].class
                || clx == Boolean.class || clx == boolean.class || clx == boolean[].class
                || clx == Character.class || clx == char.class || clx == char[].class
                || clx == Byte.class || clx == byte.class || clx == byte[].class
                || clx == String.class;
    }
}
