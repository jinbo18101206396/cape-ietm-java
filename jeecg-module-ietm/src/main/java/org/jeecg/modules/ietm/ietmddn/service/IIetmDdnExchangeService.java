package org.jeecg.modules.ietm.ietmddn.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.ietmddn.entity.IetmDdn;

import java.util.Map;

/**
 * @Description: 数据交换记录服务接口
 * @Author: jeecg-boot
 * @Date: 2026-09-15
 * @Version: V1.0
 */
public interface IIetmDdnExchangeService extends IService<IetmDdn> {

    /**
     * 获取DDN文件内容
     *
     * @param id DDN记录ID
     * @return DDN文件内容，包含XML字符串和元数据
     * @throws Exception 读取文件失败时抛出异常
     */
    Map<String, Object> getDdnFileContent(String id) throws Exception;
}
