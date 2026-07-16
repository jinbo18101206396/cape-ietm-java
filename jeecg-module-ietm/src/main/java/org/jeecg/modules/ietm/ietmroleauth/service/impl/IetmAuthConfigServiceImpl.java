package org.jeecg.modules.ietm.ietmroleauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmAuthConfig;
import org.jeecg.modules.ietm.ietmroleauth.mapper.IetmAuthConfigMapper;
import org.jeecg.modules.ietm.ietmroleauth.service.IIetmAuthConfigService;
import org.springframework.stereotype.Service;

/**
 * @Description: 授权配置
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
@Service
public class IetmAuthConfigServiceImpl extends ServiceImpl<IetmAuthConfigMapper, IetmAuthConfig> implements IIetmAuthConfigService {

    @Override
    public String getConfigValue(String configKey) {
        LambdaQueryWrapper<IetmAuthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IetmAuthConfig::getConfigKey, configKey);
        IetmAuthConfig config = this.getOne(queryWrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public boolean saveOrUpdateConfig(String configKey, String configValue) {
        LambdaQueryWrapper<IetmAuthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IetmAuthConfig::getConfigKey, configKey);
        IetmAuthConfig config = this.getOne(queryWrapper);

        if (config == null) {
            config = new IetmAuthConfig();
            config.setConfigKey(configKey);
        }
        config.setConfigValue(configValue);
        return this.saveOrUpdate(config);
    }
}
