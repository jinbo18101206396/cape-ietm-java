package org.jeecg.modules.ietm.icnmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @Description: 项目管理-项目实体管理
 * @Author: jeecg-boot
 * @Date: 2026-02-06
 * @Version: V1.0
 */
public interface IIetmIcnManageService extends IService<IetmIcnManage> {

    void fileAdd(MultipartFile[] files, String cmnodeId, Integer security, String uniqueId, String sns, String icnType, String variantCode, String issueNo, String originator, String originatorName, String rpc, String rpcName) throws IOException;

    void relatedFilesAdd(MultipartFile[] files, String id) throws IOException;

    void diffFilesAdd(MultipartFile[] files, String id, String uniqueId, String variantCode)throws IOException;

    void removeAllByid(String id);
}
