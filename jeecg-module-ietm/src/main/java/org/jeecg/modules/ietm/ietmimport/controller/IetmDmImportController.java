package org.jeecg.modules.ietm.ietmimport.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmimport.service.IIetmDmImportService;
import org.jeecg.modules.ietm.ietmimport.vo.DmImportRequestVO;
import org.jeecg.modules.ietm.ietmimport.vo.DmImportResultVO;
import org.jeecg.modules.ietm.ietmimport.vo.DmValidateResultVO;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 数据模块导入控制器
 *
 * @author IETM Team
 * @date 2026-09-03
 */
@Api(tags = "数据模块导入")
@RestController
@RequestMapping("/ietm/ietmimport")
@Slf4j
public class IetmDmImportController extends JeecgController<IetmDataModule, IIetmDmImportService> {

    @Autowired
    private IIetmDmImportService dmImportService;

    /**
     * 校验接口（第一阶段）
     * 对应旧系统的 validdm() 函数
     */
    @PostMapping("/validate")
    @ApiOperation(value = "校验DM文件", notes = "执行14种校验规则，返回校验结果")
    public Result<DmValidateResultVO> validate(
            @ApiParam(value = "DM文件（.xml或.zip）", required = true) @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            log.info("开始校验DM文件：{}", file.getOriginalFilename());

            // 调用Service执行校验
            DmValidateResultVO result = dmImportService.validateFile(file, request);

            log.info("校验完成：总{}个文件，成功{}个，失败{}个",
                    result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());

            return Result.OK(result);

        } catch (Exception e) {
            log.error("校验DM文件失败", e);
            return Result.error("校验失败：" + e.getMessage());
        }
    }

    /**
     * 导入接口（第二阶段）
     * 对应旧系统的 importdm() 函数
     */
    @PostMapping("/import")
    @ApiOperation(value = "导入DM文件", notes = "将校验通过的文件导入数据库，包含DDN信息")
    public Result<DmImportResultVO> importDm(
            @ApiParam(value = "导入请求（包含文件列表和DDN信息）", required = true) @RequestBody DmImportRequestVO importRequest,
            HttpServletRequest request) {

        try {
            log.info("开始导入DM文件：共{}个，DDN型号：{}",
                    importRequest.getFiles().size(),
                    importRequest.getDdnInfo().getModelic());

            // 调用Service执行导入
            DmImportResultVO result = dmImportService.importFiles(importRequest.getFiles(), request);

            log.info("导入完成：DM{}个，ICN{}个，失败{}个",
                    result.getDmSuccessCount(), result.getIcnSuccessCount(), result.getFailureCount());

            return Result.OK(result);

        } catch (Exception e) {
            log.error("导入DM文件失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * ICN文件名校验接口（轻量级）
     *
     * 只传文件名到后端，不传二进制数据，执行3条校验规则：
     * - 规则-11：ICN文件名格式校验
     * - 规则-12：ICN的SNS是否在构型中
     * - 规则-13：ICN是否已存在
     *
     * @param fileName 文件名（例如：ICN-MODEL-SNS-00001.png）
     * @param request HttpServletRequest（用于获取当前项目ID）
     * @return 校验结果
     */
    @GetMapping("/validateIcnByName")
    @ApiOperation(value = "ICN文件名校验", notes = "只传文件名，执行规则-11、-12、-13校验")
    public Result<ImportFileItemVO> validateIcnByName(
            @ApiParam(value = "ICN文件名", required = true) @RequestParam("fileName") String fileName,
            HttpServletRequest request) {

        try {
            log.info("开始校验ICN文件名：{}", fileName);

            // 调用Service执行校验
            ImportFileItemVO result = dmImportService.validateIcnByFileName(fileName, request);

            log.info("ICN文件名校验完成：{} -> {}", fileName, result.getResultMessage());

            return Result.OK(result);

        } catch (Exception e) {
            log.error("ICN文件名校验失败：{}", fileName, e);
            return Result.error("校验失败：" + e.getMessage());
        }
    }

    @GetMapping("/validateResourceByName")
    @ApiOperation(value = "资源文件名校验", notes = "只传文件名，检查关联的DM是否存在")
    public Result<ImportFileItemVO> validateResourceByName(
            @ApiParam(value = "资源文件名", required = true) @RequestParam("fileName") String fileName,
            HttpServletRequest request) {

        try {
            // 【P1修复】输入验证：防止空值和路径遍历攻击
            if (fileName == null || fileName.trim().isEmpty()) {
                return Result.error("文件名不能为空");
            }
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return Result.error("文件名格式不合法");
            }

            log.info("开始校验资源文件名：{}", fileName);

            // 调用Service执行校验
            ImportFileItemVO result = dmImportService.validateResourceByFileName(fileName, request);

            log.info("资源文件名校验完成：{} -> {}", fileName, result.getResultMessage());

            return Result.OK(result);

        } catch (Exception e) {
            log.error("资源文件名校验失败：{}", fileName, e);
            return Result.error("校验失败：" + e.getMessage());
        }
    }
}
