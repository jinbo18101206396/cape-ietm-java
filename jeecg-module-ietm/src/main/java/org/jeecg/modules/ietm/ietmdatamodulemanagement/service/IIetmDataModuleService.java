package org.jeecg.modules.ietm.ietmdatamodulemanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmEditPropVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmProjectInfoVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @Description: 数据模块管理Service接口
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 * @Version: V2.0
 */
public interface IIetmDataModuleService extends IService<IetmDataModule> {

    /**
     * 获取项目信息（包含SNS编码）
     * @param cmNodeId 构型节点ID
     * @return 项目信息VO
     */
    DmProjectInfoVO getProjectInfo(String cmNodeId);

    /**
     * 保存DM
     * @param dataModule 数据模块对象
     * @return 保存结果
     */
    boolean saveDm(IetmDataModule dataModule);

    /**
     * 更新DM
     * @param dataModule 数据模块对象
     * @return 更新结果
     */
    boolean updateDm(IetmDataModule dataModule);

    /**
     * 删除DM
     * @param id DM主键
     * @return 删除结果
     */
    boolean deleteDm(String id);

    /**
     * 根据ID查询DM
     * @param id DM主键
     * @return DM对象
     */
    IetmDataModule queryById(String id);

    /**
     * 签出DM
     * @param id DM主键
     * @param username 签出用户
     * @return 签出结果
     */
    boolean checkOut(String id, String username);

    /**
     * 取消签出DM
     * @param id DM主键
     * @param username 当前用户
     * @return 取消签出结果
     */
    boolean cancelCheckOut(String id, String username);

    /**
     * 签入DM
     * @param id DM主键
     * @param username 签入用户
     * @param comment 签入备注
     * @return 签入结果
     */
    boolean checkIn(String id, String username, String comment);

    /**
     * 发布DM
     * @param id DM主键
     * @param username 发布用户
     * @return 发布结果
     */
    boolean publishDm(String id, String username);

    /**
     * 批量签出
     * @param ids DM主键列表
     * @param username 签出用户
     * @return 签出结果
     */
    Map<String, Object> batchCheckOut(List<String> ids, String username);

    /**
     * 批量签入
     * @param ids DM主键列表
     * @param username 签入用户
     * @param comment 签入备注
     * @return 签入结果
     */
    Map<String, Object> batchCheckIn(List<String> ids, String username, String comment);

    /**
     * 批量删除
     * @param ids DM主键列表
     * @return 删除结果
     */
    Map<String, Object> batchDelete(List<String> ids);

    /**
     * 根据项目ID查询DM列表
     * @param projectId 项目ID
     * @return DM列表
     */
    List<IetmDataModule> queryByProjectId(String projectId);

    /**
     * 根据构型节点ID查询DM列表
     * @param cmNodeId 构型节点ID
     * @param includeChildren 是否包含子节点
     * @return DM列表
     */
    List<IetmDataModule> queryByCmNodeId(String cmNodeId, boolean includeChildren);

    /**
     * 查询历史版本
     * @param sns SNS编号
     * @param infoCode 信息代码
     * @param infoCodeVariant 信息代码变体
     * @return 历史版本列表
     */
    List<IetmDataModule> queryHistoryVersions(String sns, String infoCode, String infoCodeVariant);

    /**
     * 查询引用关系树
     * @param dmId DM主键
     * @param refType 引用类型（out-出引用，in-入引用）
     * @return 引用关系树
     */
    List<Map<String, Object>> queryReferenceTree(String dmId, String refType);

    /**
     * 更新引用计数
     * @param dmId DM主键
     */
    void updateReferenceCount(String dmId);

    /**
     * 导入XML文件
     * @param file XML文件
     * @param projectId 项目ID
     * @return 导入结果
     */
    Map<String, Object> importXml(MultipartFile file, String projectId);

    /**
     * 导出XML文件
     * @param id DM主键
     * @param response HTTP响应对象
     */
    void exportXml(String id, HttpServletResponse response);

    /**
     * 导入ZIP压缩包（批量导入）
     * @param file ZIP文件
     * @param projectId 项目ID
     * @return 导入结果
     */
    Map<String, Object> importZip(MultipartFile file, String projectId);

    /**
     * DMC唯一性校验
     * @param dataModule 数据模块对象
     * @return true-已存在，false-不存在
     */
    boolean validateDmc(IetmDataModule dataModule);

    /**
     * 校验DM内容（XML格式）
     * @param content DM内容
     * @return 校验结果
     */
    Map<String, Object> validateContent(String content);

    /**
     * 生成DMC编码
     * @param dataModule 数据模块对象
     * @return DMC完整编码
     */
    String generateDmc(IetmDataModule dataModule);

    /**
     * 版本号计算
     * @param currentInwork 当前inwork编号
     * @param currentIssueno 当前发行编号
     * @param versionType 版本类型（inwork-升级inwork，issue-升级issueno）
     * @return 新版本号Map（包含newInwork和newIssueno）
     */
    Map<String, String> calculateVersion(String currentInwork, String currentIssueno, String versionType);

    /**
     * 复制DM
     * @param id 源DM主键
     * @param targetProjectId 目标项目ID（可为null，表示同项目）
     * @param copyType 复制类型（0=仅复制属性，1=创建新版本链）
     * @param username 操作用户
     * @return 新DM ID
     */
    String copyDm(String id, String targetProjectId, Integer copyType, String username);

    /**
     * 启动工作流
     * @param id DM主键
     * @param processKey 流程定义Key
     * @param username 启动用户
     * @return 工作流实例ID
     */
    String startWorkflow(String id, String processKey, String username);

    /**
     * 完成工作流任务
     * @param id DM主键
     * @param taskId 任务ID
     * @param approved 是否通过
     * @param comment 审批意见
     * @param username 审批用户
     * @return 是否成功
     */
    boolean completeWorkflowTask(String id, String taskId, boolean approved, String comment, String username);

    /**
     * 预览DM
     * @param id DM主键
     * @param response HTTP响应对象
     */
    void previewDm(String id, HttpServletResponse response);

    /**
     * 搜索DM
     * @param keyword 关键词
     * @param projectId 项目ID（可选）
     * @return DM列表
     */
    List<IetmDataModule> searchDm(String keyword, String projectId);

    /**
     * 查询DM资源列表
     * @param dmId DM模块ID
     * @return 资源列表
     */
    List<Map<String, Object>> queryDmResources(String dmId);

    /**
     * 添加DM资源
     * @param dmId DM模块ID
     * @param fileId 文件ID
     * @param resourceName 资源名称
     * @param fileSize 文件大小（字节）
     * @param comment 说明
     * @return 是否成功
     */
    boolean saveDmResource(String dmId, String fileId, String resourceName, Long fileSize, String comment);

    /**
     * 更新DM资源
     * @param id 资源ID
     * @param comment 说明
     * @return 是否成功
     */
    boolean updateDmResource(String id, String comment);

    /**
     * 删除DM资源
     * @param id 资源ID
     * @return 是否成功
     */
    boolean deleteDmResource(String id);

    /**
     * 删除DM资源及文件
     * @param id 资源ID
     * @return 是否成功
     */
    boolean deleteDmResourceFile(String id);

    /**
     * 编辑DM属性（仅修改技术名称和信息名称）
     * 已签出（本人）：直接更新，inWork不变；未签出：自动签出并更新，inWork+1
     *
     * @param id          DM主键ID
     * @param vo          包含techName和infoName
     * @param currentUser 当前登录用户名
     * @return 操作结果
     */
    Result<?> editProp(String id, DmEditPropVO vo, String currentUser);
}
