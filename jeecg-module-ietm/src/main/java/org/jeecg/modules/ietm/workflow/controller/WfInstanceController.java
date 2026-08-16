package org.jeecg.modules.ietm.workflow.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.ietm.workflow.service.IWfInstanceService;
import org.jeecg.modules.ietm.workflow.vo.BatchRestartFlowVO;
import org.jeecg.modules.ietm.workflow.vo.BatchStartFlowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @Description: 工作流实例Controller
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Api(tags = "工作流管理")
@RestController
@RequestMapping("/ietm/workflow")
@Slf4j
public class WfInstanceController {

    @Autowired
    private IWfInstanceService wfInstanceService;

    /**
     * 批量启动流程
     *
     * 前端调用场景：
     * 1. 用户在DM列表勾选多条记录
     * 2. 点击"批量启动流程"按钮
     * 3. 在弹框中配置流程节点和处理人
     * 4. 提交后调用本接口
     *
     * @param vo 批量启动请求参数
     * @return 成功启动的DM数量
     */
    @AutoLog(value = "工作流-批量启动流程")
    @ApiOperation(value = "批量启动流程", notes = "为选中的多条DM批量创建工作流实例")
    @PostMapping(value = "/batchStartFlow")
    public Result<String> batchStartFlow(@Valid @RequestBody BatchStartFlowVO vo) {
        try {
            int successCount = wfInstanceService.batchStartFlow(vo);
            Result<String> result = Result.ok("成功启动 " + successCount + " 条流程");
            result.setResult(String.valueOf(successCount));
            return result;
        } catch (Exception e) {
            log.error("批量启动流程失败", e);
            return Result.error("批量启动失败：" + e.getMessage());
        }
    }

    /**
     * 批量重新启动流程
     *
     * 前端调用场景：
     * 1. 用户在DM列表筛选"已发布且流程已结束"的记录
     * 2. 勾选多条记录
     * 3. 点击"批量重启流程"按钮
     * 4. 在弹框中填写重启原因、配置流程节点
     * 5. 提交后调用本接口
     *
     * 业务逻辑：
     * - 终止旧实例（status_改为'9'）
     * - 创建新实例（seqno统一+100偏移）
     *
     * @param vo 批量重启请求参数
     * @return 成功重启的DM数量
     */
    @AutoLog(value = "工作流-批量重新启动流程")
    @ApiOperation(value = "批量重新启动流程", notes = "终止旧流程并为选中的DM创建新工作流实例")
    @PostMapping(value = "/batchRestartFlow")
    public Result<String> batchRestartFlow(@Valid @RequestBody BatchRestartFlowVO vo) {
        try {
            int successCount = wfInstanceService.batchRestartFlow(vo);
            Result<String> result = Result.ok("成功重启 " + successCount + " 条流程");
            result.setResult(String.valueOf(successCount));
            return result;
        } catch (Exception e) {
            log.error("批量重启流程失败", e);
            return Result.error("批量重启失败：" + e.getMessage());
        }
    }
}
