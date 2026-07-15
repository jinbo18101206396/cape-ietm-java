package org.jeecg.modules.ietm.agencySelection.service;

import org.jeecg.modules.ietm.agencySelection.entity.AgencyFile;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Description: 机构遴选-附件表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
public interface IAgencyFileService extends IService<AgencyFile> {

    AgencyFile uploadFile(MultipartFile file, String bizId, String bizType) throws Exception;

    List<AgencyFile> getFiles(String bizId, String bizType);

    void deleteFile(String fileId);

    void deleteByBizId(String bizId, String bizType);

    void downloadAllFiles(HttpServletResponse response) throws Exception;

    void downloadFile(String fileId, HttpServletResponse response) throws Exception;
}
