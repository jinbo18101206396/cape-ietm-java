package org.jeecg.modules.ietm.ietmimport.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmimport.vo.DmImportResultVO;
import org.jeecg.modules.ietm.ietmimport.vo.DmValidateResultVO;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 数据模块导入Service接口
 *
 * @author IETM Team
 * @date 2026-09-03
 */
public interface IIetmDmImportService extends IService<IetmDataModule> {

    /**
     * 校验文件（第一阶段）
     *
     * @param file 上传的文件（.xml或.zip）
     * @param request HTTP请求（用于获取项目上下文）
     * @return 校验结果
     */
    DmValidateResultVO validateFile(MultipartFile file, HttpServletRequest request) throws Exception;

    /**
     * 导入文件（第二阶段）
     *
     * @param files 校验通过的文件列表
     * @param request HTTP请求（用于获取项目上下文）
     * @return 导入结果
     */
    DmImportResultVO importFiles(List<ImportFileItemVO> files, HttpServletRequest request) throws Exception;

    /**
     * ICN文件名校验（轻量级）
     *
     * 只传文件名到后端，不传二进制数据，执行3条校验规则：
     * - 规则-11：ICN文件名格式校验
     * - 规则-12：ICN的SNS是否在构型中
     * - 规则-13：ICN是否已存在
     *
     * @param fileName ICN文件名
     * @param request HTTP请求（用于获取项目上下文）
     * @return 校验结果
     */
    ImportFileItemVO validateIcnByFileName(String fileName, HttpServletRequest request) throws Exception;

    /**
     * 资源文件名校验（轻量级API）
     *
     * @param fileName 资源文件名
     * @param request HTTP请求（用于获取项目上下文）
     * @return 校验结果
     */
    ImportFileItemVO validateResourceByFileName(String fileName, HttpServletRequest request) throws Exception;
}
