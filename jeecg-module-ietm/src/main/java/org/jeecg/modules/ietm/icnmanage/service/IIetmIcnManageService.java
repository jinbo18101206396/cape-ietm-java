package org.jeecg.modules.ietm.icnmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.vo.IcnProjectInfoVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @Description: 项目管理-项目实体管理Service
 * @Author: jeecg-boot
 * @Date: 2026-07-19
 * @Version: V2.0
 */
public interface IIetmIcnManageService extends IService<IetmIcnManage> {

    /**
     * 新增ICN（带文件上传）
     */
    void addWithFiles(IetmIcnManage icnManage, MultipartFile[] files) throws IOException;

    /**
     * 相关文件上传
     */
    void uploadRelatedFiles(String icnId, MultipartFile[] files) throws IOException;

    /**
     * 差异上传（保留原ICN，生成新ICN）
     */
    void uploadDiffFiles(String originalIcnId, MultipartFile[] files, String newUniqueId, String newVariantCode) throws IOException;

    /**
     * 新版上传（删除原ICN文件，重新上传）
     */
    void uploadNewVersion(String icnId, MultipartFile[] files) throws IOException;

    /**
     * 删除ICN（包含附件和物理文件）
     */
    void removeWithAttachments(String icnId);

    /**
     * 批量删除ICN
     */
    void removeBatchWithAttachments(List<String> icnIds);

    /**
     * 获取指定构型节点的下一个唯一识别码
     */
    String getNextUniqueId(String cmnodeId);

    /**
     * 获取项目信息和SNS编码
     */
    IcnProjectInfoVO getProjectInfo(String cmnodeId);

    /**
     * 生成ICN完整编码
     */
    String generateIcnCode(IetmIcnManage icnManage);

    /**
     * 计算SNS编码
     */
    String calculateSns(String cmnodeId, String codeRule);

    /**
     * 查询ICN列表（含附件信息）
     * @param cmnodeId 构型节点ID
     * @param includeChildren 是否包含子节点（1：是，0：否）
     */
    List<IetmIcnManage> listWithAttachments(String cmnodeId, String includeChildren);

    /**
     * 下载实体文件
     */
    void downloadFile(String fileKey, HttpServletResponse response) throws IOException;

    /**
     * 批量导入ICN
     */
    void importExcel(MultipartFile file) throws Exception;

    /**
     * 批量新增ICN
     */
    int batchAddIcn(IetmIcnManage template);
}
