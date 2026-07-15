package org.jeecg.modules.ietm.ietmattachment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.base.service.BaseCommonService;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.mapper.IetmAttachmentMapper;
import org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.io.File;
import java.util.List;

/**
 * @Description: 附件表
 * @Author: jeecg-boot
 * @Date:   2026-03-03
 * @Version: V1.0
 */
@Service
public class IetmAttachmentServiceImpl extends ServiceImpl<IetmAttachmentMapper, IetmAttachment> implements IIetmAttachmentService {

    @Value("${accessFile.location}")
    private String location;

    /**
     * 删除附件时通过 location + chain_attachment 表的 file_key 定位到本地的临时文件并进行删除
     *
     * @param id
     */
    @Override
    public void removeTempFileById(String id) {
//        根据 id 查询 chain_attachment 表的 file_key
        QueryWrapper<IetmAttachment> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        List<IetmAttachment> attachmentList = baseMapper.selectList(wrapper);
        deleteTempFilesByAttachmentList(attachmentList);
    }
    private void deleteTempFilesByAttachmentList(List<IetmAttachment> attachmentList) {
        if (attachmentList == null || attachmentList.isEmpty()) {
            return;
        }
        String fileName = "";
        Integer securityMax = 0;
        for (IetmAttachment attachment : attachmentList) {
            QueryWrapper<IetmAttachment> chainAttachmentQueryWrapper = new QueryWrapper<>();
            chainAttachmentQueryWrapper.eq("file_key", attachment.getFileKey());
            List<IetmAttachment> list = baseMapper.selectList(chainAttachmentQueryWrapper);//如果多个数据共用一个附件，只有最后一个共用附件的数据可以删除文件
            if (list.size() == 1) {
                File file = new File(location, attachment.getFileKey());
                try {
                    file.delete();
                    fileName = attachment.getFileName() + ";" + fileName;
                    securityMax = Math.max(attachment.getSecurity(), securityMax);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
