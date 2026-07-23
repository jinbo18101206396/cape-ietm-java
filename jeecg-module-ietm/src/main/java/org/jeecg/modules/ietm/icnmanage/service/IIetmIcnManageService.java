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
    String getNextUniqueId(String cmNodeId);

    /**
     * 获取项目信息和SNS编码
     */
    IcnProjectInfoVO getProjectInfo(String cmNodeId);

    /**
     * 生成ICN完整编码
     */
    String generateIcnCode(IetmIcnManage icnManage);

    /**
     * 计算SNS编码
     */
    String calculateSns(String cmNodeId);

    /**
     * 查询ICN列表（含附件信息）
     * @param cmNodeId 构型节点ID
     * @param includeChildren 是否包含子节点（1：是，0：否）
     */
    List<IetmIcnManage> listWithAttachments(String cmNodeId, String includeChildren);

    /**
     * 通过ID查询ICN（含附件信息）
     * @param id ICN ID
     * @return ICN实体（含附件信息）
     */
    IetmIcnManage getByIdWithAttachment(String id);

    /**
     * 下载实体文件
     */
    void downloadFile(String fileKey, HttpServletResponse response) throws IOException;

    /**
     * 预览文件（在线查看，支持解密）
     * @param fileKey 文件Key
     * @param response HTTP响应
     */
    void viewFile(String fileKey, HttpServletResponse response) throws IOException;

    /**
     * 批量导入ICN
     */
    void importExcel(MultipartFile file) throws Exception;

    /**
     * 批量新增ICN
     */
    int batchAddIcn(IetmIcnManage template);

    /**
     * 获取预览信息
     * @param id ICN ID
     * @return 预览信息VO
     */
    org.jeecg.modules.ietm.icnmanage.vo.PreviewInfoVO getPreviewInfo(String id) throws Exception;

    /**
     * 获取引用关系信息
     * @param id ICN ID
     * @return 引用关系信息VO
     */
    org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO getReferenceInfo(String id) throws Exception;

    /**
     * 单文件下载
     * @param id ICN ID
     * @param includeRelated 是否包含相关文件
     * @param response HTTP响应
     */
    void downloadSingle(String id, Boolean includeRelated, HttpServletResponse response) throws Exception;

    /**
     * 批量下载
     * @param ids ICN ID列表
     * @param includeRelated 是否包含相关文件
     * @param response HTTP响应
     */
    void downloadBatch(List<String> ids, Boolean includeRelated, HttpServletResponse response) throws Exception;

    /**
     * 异步批量下载
     * @param ids ICN ID列表
     * @param includeRelated 是否包含相关文件
     * @return 任务ID
     */
    String downloadBatchAsync(List<String> ids, Boolean includeRelated) throws Exception;

    /**
     * 查询下载任务状态
     * @param taskId 任务ID
     * @return 任务状态VO
     */
    org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO getDownloadTaskStatus(String taskId) throws Exception;

    /**
     * 清理过期的下载任务缓存
     */
    void cleanExpiredTasks();

    /**
     * 添加引用关系
     * @param sourceIcnId 源ICN ID
     * @param targetIcnId 目标ICN ID或DM编码
     * @param referenceType 引用类型：ICN_TO_ICN 或 ICN_TO_DM
     * @param remark 备注
     */
    void addReference(String sourceIcnId, String targetIcnId, String referenceType, String remark) throws Exception;

    /**
     * 删除引用关系
     * @param referenceId 引用关系ID
     */
    void deleteReference(String referenceId) throws Exception;
}
