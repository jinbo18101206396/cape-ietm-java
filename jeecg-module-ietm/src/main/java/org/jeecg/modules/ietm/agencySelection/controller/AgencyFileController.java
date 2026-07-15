package org.jeecg.modules.ietm.agencySelection.controller;

import javax.servlet.http.HttpServletResponse;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.agencySelection.entity.AgencyFile;
import org.jeecg.modules.ietm.agencySelection.service.IAgencyFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Description: 机构遴选-附件管理
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Api(tags = "机构遴选-附件管理")
@RestController
@RequestMapping("/agencySelection/file")
@Slf4j
public class AgencyFileController {

    @Autowired
    private IAgencyFileService agencyFileService;

    @ApiOperation(value = "机构遴选-附件管理-上传附件")
    @PostMapping(value = "/upload")
    public Result<AgencyFile> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam("bizId") String bizId,
                                     @RequestParam("bizType") String bizType) {
        try {
            AgencyFile af = agencyFileService.uploadFile(file, bizId, bizType);
            return Result.OK(af);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "机构遴选-附件管理-获取附件列表")
    @GetMapping(value = "/list")
    public Result<List<AgencyFile>> list(@RequestParam("bizId") String bizId,
                                         @RequestParam("bizType") String bizType) {
        return Result.OK(agencyFileService.getFiles(bizId, bizType));
    }

    @ApiOperation(value = "机构遴选-附件管理-删除附件")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        try {
            agencyFileService.deleteFile(id);
            return Result.OK("删除成功!");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "机构遴选-附件管理-打包下载全部附件")
    @GetMapping(value = "/downloadAll")
    public void downloadAll(HttpServletResponse response) {
        try {
            agencyFileService.downloadAllFiles(response);
        } catch (Exception e) {
            log.error("打包下载失败", e);
        }
    }

    @ApiOperation(value = "机构遴选-附件管理-下载单个附件")
    @GetMapping(value = "/download")
    public void download(@RequestParam(name = "id", required = true) String id,
                         HttpServletResponse response) {
        try {
            agencyFileService.downloadFile(id, response);
        } catch (Exception e) {
            log.error("下载附件失败", e);
        }
    }
}
