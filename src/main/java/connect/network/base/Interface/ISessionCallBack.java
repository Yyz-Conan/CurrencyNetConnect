package connect.network.base.Interface;


import connect.network.base.NetTaskEntity;

/**
 * 会话回调接口
 * Created by Dell on 8/8/2017.
 *
 * @author yyz
 */

public interface ISessionCallBack {

    /**
     * 获取回调监听者
     *
     * @return 回调监听者
     */
    Object getCallBackTarget();

    /**
     * 设置回调监听者
     *
     * @param target
     */
    void setCallBackTarget(Object target);


    /**
     * 成功通知
     *
     * @param entity
     */
    void notifySuccessMessage(NetTaskEntity entity);


    /**
     * 错误通知
     *
     * @param entity
     */
    void notifyErrorMessage(NetTaskEntity entity);

    /**
     * 回收资源
     */
    void recycle();

}
