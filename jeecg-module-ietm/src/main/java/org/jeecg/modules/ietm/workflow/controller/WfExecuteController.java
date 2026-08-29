package org.jeecg.modules.ietm.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.ietm.workflow.entity.WfExecute;
import org.jeecg.modules.ietm.workflow.service.IWfExecuteService;
import org.jeecg.modules.ietm.workflow.vo.BatchApproveVO;
import org.jeecg.modules.ietm.workflow.vo.BatchApproveResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * @Description: 工作流执行记录控制器
 * @Author: IETM Team
 * @Date: 2026-08-20
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "工作流执行管理")
@RestController
@RequestMapping("/ietm/workflow/execute")
public class WfExecuteController extends JeecgController<WfExecute, IWfExecuteService> {

    @Autowired
    private IWfExecuteService wfExecuteService;

    /**
     * 根据明细ID查询执行记录列表
     */
    @ApiOperation(value = "查询执行记录", notes = "根据明细ID查询执行记录列表")
    @GetMapping("/list")
    public Result<List<WfExecute>> list(
            @ApiParam("明细ID") @RequestParam(required = false) String instdtlid,
            @ApiParam("实例ID") @RequestParam(required = false) String instid) {

        List<WfExecute> list;
        if (instdtlid != null && !instdtlid.trim().isEmpty()) {
            list = wfExecuteService.listByDtlId(instdtlid);
        } else if (instid != null && !instid.trim().isEmpty()) {
            list = wfExecuteService.listByInstId(instid);
        } else {
            QueryWrapper<WfExecute> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");
            list = wfExecuteService.list(queryWrapper);
        }
        return Result.OK(list);
    }

    /**
     * 根据实例ID查询执行记录（包含重启前的历史记录）
     */
    @ApiOperation(value = "查询执行记录（含历史）", notes = "根据实例ID查询执行记录，包含重启前的历史审批信息")
    @GetMapping("/listWithHistory")
    public Result<List<WfExecute>> listWithHistory(
            @ApiParam("实例ID") @RequestParam String instid) {
        List<WfExecute> list = wfExecuteService.listByInstIdWithHistory(instid);
        return Result.OK(list);
    }

    /**
     * 查询节点的最新执行记录
     */
    @ApiOperation(value = "查询最新执行记录", notes = "根据明细ID查询最新执行记录")
    @GetMapping("/latest")
    public Result<WfExecute> getLatest(@ApiParam("明细ID") @RequestParam String instdtlid) {
        WfExecute execute = wfExecuteService.getLatestByDtlId(instdtlid);
        return Result.OK(execute);
    }

    /**
     * 批量审批
     */
    @ApiOperation(value = "批量审批", notes = "批量审批待办节点")
    @PostMapping("/batchApprove")
    public Result<BatchApproveResultVO> batchApprove(
            @Valid @RequestBody BatchApproveVO vo,
            HttpServletRequest request) {

        try {
            String userId = getCurrentUser(request).getUsername();
            String ifpass = vo.getApproved() ? "1" : "2";  // 1=通过, 2=不同意

            BatchApproveResultVO result = wfExecuteService.batchApprove(
                vo.getNodeIds(),
                ifpass,
                vo.getOpinion(),
                userId
            );

            return Result.OK("批量审批完成", result);
        } catch (Exception e) {
            log.error("批量审批失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 提交处理（核心API）
     */
    @ApiOperation(value = "提交处理", notes = "执行节点处理：通过/不同意/跳转/终止")
    @PostMapping("/submit")
    public Result<?> submit(
            @ApiParam("明细ID") @RequestParam String instdtlid,
            @ApiParam("处理结果：1=通过,2=不同意,3=跳转,9=终止") @RequestParam String ifpass,
            @ApiParam("目标节点ID（跳转时必填）") @RequestParam(required = false) String targetDtlid,
            @ApiParam("处理意见") @RequestParam(required = false) String opinion,
            @ApiParam("附件") @RequestParam(required = false) MultipartFile file,
            HttpServletRequest request) {

        try {
            String userId = getCurrentUser(request).getUsername();

            // 处理附件
            String filename = null;
            byte[] filecontent = null;
            if (file != null && !file.isEmpty()) {
                filename = file.getOriginalFilename();
                filecontent = file.getBytes();
            }

            // 执行节点处理
            wfExecuteService.executeNode(instdtlid, ifpass, targetDtlid, opinion, filename, filecontent, userId);

            return Result.OK("处理成功");
        } catch (Exception e) {
            log.error("提交处理失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 追加意见
     */
    @ApiOperation(value = "追加意见", notes = "对已处理的节点追加意见")
    @PostMapping("/addOpinion")
    public Result<?> addOpinion(
            @ApiParam("明细ID") @RequestParam String instdtlid,
            @ApiParam("追加意见") @RequestParam String opinion,
            HttpServletRequest request) {

        try {
            String userId = getCurrentUser(request).getUsername();
            wfExecuteService.addOpinion(instdtlid, opinion, userId);
            return Result.OK("追加意见成功");
        } catch (Exception e) {
            log.error("追加意见失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 拿回
     */
    @ApiOperation(value = "拿回", notes = "拿回已处理的节点")
    @PostMapping("/takeBack")
    public Result<?> takeBack(
            @ApiParam("明细ID") @RequestParam String instdtlid,
            HttpServletRequest request) {

        try {
            String userId = getCurrentUser(request).getUsername();
            wfExecuteService.takeBack(instdtlid, userId);
            return Result.OK("拿回成功");
        } catch (Exception e) {
            log.error("拿回失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下载附件
     */
    @ApiOperation(value = "下载附件", notes = "下载执行记录中的附件")
    @GetMapping("/download")
    public void download(
            @ApiParam("执行记录ID") @RequestParam String id,
            HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response) {

        try {
            WfExecute execute = wfExecuteService.getById(id);
            if (execute == null || execute.getFilecontent() == null) {
                response.setStatus(404);
                return;
            }

            String filename = execute.getFilename();
            if (filename == null || filename.trim().isEmpty()) {
                filename = "attachment.dat";
            }

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    new String(filename.getBytes("UTF-8"), "ISO-8859-1") + "\"");

            // 输出文件内容
            response.getOutputStream().write(execute.getFilecontent());
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("下载附件失败", e);
        }
    }

    /**
     * 获取当前用户
     */
    private org.jeecg.common.system.vo.LoginUser getCurrentUser(HttpServletRequest request) {
        return (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
    }
}
