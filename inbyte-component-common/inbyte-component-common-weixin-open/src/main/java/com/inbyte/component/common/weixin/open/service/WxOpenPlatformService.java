package com.inbyte.component.common.weixin.open.service;

import com.inbyte.component.common.weixin.open.config.RedisProperies;
import com.inbyte.component.common.weixin.open.config.WechatOpenProperties;
import me.chanjar.weixin.open.api.impl.WxOpenInRedisConfigStorage;
import me.chanjar.weixin.open.api.impl.WxOpenMessageRouter;
import me.chanjar.weixin.open.api.impl.WxOpenServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

/**
 * @author <a href="https://github.com/007gzs">007</a>
 */
@Service
@EnableConfigurationProperties({WechatOpenProperties.class, RedisProperies.class})
public class WxOpenPlatformService extends WxOpenServiceImpl implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private WechatOpenProperties wechatOpenProperties;
    @Autowired
    private RedisProperies redisProperies;
    private volatile static JedisPool pool;
    private WxOpenMessageRouter wxOpenMessageRouter;

    @Override
    public void afterPropertiesSet() throws Exception {
        WxOpenInRedisConfigStorage inRedisConfigStorage = new WxOpenInRedisConfigStorage(getJedisPool());
        inRedisConfigStorage.setComponentAppId(wechatOpenProperties.getComponentAppId());
        inRedisConfigStorage.setComponentAppSecret(wechatOpenProperties.getComponentSecret());
        inRedisConfigStorage.setComponentToken(wechatOpenProperties.getComponentToken());
        inRedisConfigStorage.setComponentAesKey(wechatOpenProperties.getComponentAesKey());
        setWxOpenConfigStorage(inRedisConfigStorage);
        wxOpenMessageRouter = new WxOpenMessageRouter(this);
        wxOpenMessageRouter.rule().handler((wxMpXmlMessage, map, wxMpService, wxSessionManager) -> {
            logger.info("\n接收到 {} 公众号请求消息，内容：{}", wxMpService.getWxMpConfigStorage().getAppId(), wxMpXmlMessage);
            return null;
        }).next();
    }

    public WxOpenMessageRouter getWxOpenMessageRouter(){
        return wxOpenMessageRouter;
    }

    private JedisPool getJedisPool() {
        if (pool == null) {
            synchronized (WxOpenPlatformService.class) {
                if (pool == null) {
                    pool = new JedisPool(redisProperies, redisProperies.getHost(),
                            redisProperies.getPort(), redisProperies.getConnectionTimeout(),
                            redisProperies.getSoTimeout(), redisProperies.getPassword(),
                            redisProperies.getDatabase(), redisProperies.getClientName(),
                            redisProperies.isSsl(), redisProperies.getSslSocketFactory(),
                            redisProperies.getSslParameters(), redisProperies.getHostnameVerifier());
                }
            }
        }
        return pool;
    }
}
