package org.jeecg.modules.ietm.ietmattachment.service;

import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 附件表
 * @Author: jeecg-boot
 * @Date:   2026-03-03
 * @Version: V1.0
 */
public interface IIetmAttachmentService extends IService<IetmAttachment> {
    void removeTempFileById(String id);
}
