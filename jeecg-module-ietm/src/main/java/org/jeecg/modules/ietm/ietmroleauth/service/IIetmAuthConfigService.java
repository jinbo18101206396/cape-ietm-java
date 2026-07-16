package org.jeecg.modules.ietm.ietmroleauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmAuthConfig;

/**
 * @Description: 授权配置
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
public interface IIetmAuthConfigService extends IService<IetmAuthConfig> {

    /**
     * 根据配置键获取配置值
     * @param configKey 配置键
     * @return
     */
    String getConfigValue(String configKey);

    /**
     * 保存或更新配置
     * @param configKey 配置键
     * @param configValue 配置值
     * @return
     */
    boolean saveOrUpdateConfig(String configKey, String configValue);
}
