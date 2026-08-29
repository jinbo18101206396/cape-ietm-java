package org.jeecg.modules.ietm.workflow.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.ietm.workflow.service.IWfInstanceService;
import org.jeecg.modules.ietm.workflow.vo.BatchRestartFlowVO;
import org.jeecg.modules.ietm.workflow.vo.BatchStartFlowVO;
import org.jeecg.modules.ietm.workflow.vo.TodoItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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

    /**
     * 根据formid查询流程实例
     *
     * @param formid 业务表单ID
     * @return 流程实例信息
     */
    @AutoLog(value = "工作流-根据formid查询流程实例")
    @ApiOperation(value = "根据formid查询流程实例", notes = "根据业务表单ID查询对应的流程实例")
    @GetMapping(value = "/instance/getByFormid")
    public Result<?> getByFormid(@RequestParam String formid) {
        try {
            return Result.OK(wfInstanceService.getByFormid(formid));
        } catch (Exception e) {
            log.error("查询流程实例失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 查询当前用户待办任务
     *
     * @param formid 业务表单ID
     * @return 待办节点信息
     */
    @AutoLog(value = "工作流-查询待办任务")
    @ApiOperation(value = "查询待办任务", notes = "查询当前用户在指定流程中的待办任务节点")
    @GetMapping(value = "/instance/getTodo")
    public Result<?> getTodo(@RequestParam String formid) {
        try {
            return Result.OK(wfInstanceService.getTodoByFormid(formid));
        } catch (Exception e) {
            log.error("查询待办任务失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 修改紧急程度
     *
     * @param id 流程实例ID
     * @param ifurgent 紧急程度
     * @return 操作结果
     */
    @AutoLog(value = "工作流-修改紧急程度")
    @ApiOperation(value = "修改紧急程度", notes = "修改流程实例的紧急程度")
    @PostMapping(value = "/instance/updateUrgent")
    public Result<?> updateUrgent(@RequestParam String id, @RequestParam String ifurgent) {
        try {
            wfInstanceService.updateUrgent(id, ifurgent);
            return Result.OK("修改成功");
        } catch (Exception e) {
            log.error("修改紧急程度失败", e);
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 终止流程
     *
     * @param id 流程实例ID
     * @param reason 终止原因
     * @return 操作结果
     */
    @AutoLog(value = "工作流-终止流程")
    @ApiOperation(value = "终止流程", notes = "终止流程实例")
    @PostMapping(value = "/instance/terminate")
    public Result<?> terminate(@RequestParam String id, @RequestParam String reason) {
        try {
            wfInstanceService.terminate(id, reason);
            return Result.OK("终止成功");
        } catch (Exception e) {
            log.error("终止流程失败", e);
            return Result.error("终止失败：" + e.getMessage());
        }
    }

    /**
     * 查询我的待办列表
     *
     * 前端调用场景：
     * 1. 首页Dashboard加载时自动调用
     * 2. 用户切换项目时触发
     * 3. 用户点击刷新按钮
     * 4. 用户执行搜索操作
     *
     * 业务逻辑：
     * - 只显示当前项目（projectId）的待办
     * - 5种权限匹配（用户/部门/角色/用户组/岗位）- v1.3前缀编码模式
     * - 排除当前用户已审批的节点
     * - 支持4个维度的模糊搜索
     *
     * @param projectId   当前项目ID（必填）
     * @param searchField 搜索字段（可选：title/nodename/created_name/creation_date_str）
     * @param searchValue 搜索值（可选）
     * @return 待办列表
     */
    @AutoLog(value = "工作流-查询我的待办")
    @ApiOperation(value = "查询我的待办", notes = "查询当前用户在指定项目中的待办事项")
    @GetMapping(value = "/myTodoList")
    public Result<List<TodoItemVO>> getMyTodoList(
            @RequestParam(required = true) String projectId,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {
        try {
            List<TodoItemVO> todoList = wfInstanceService.getMyTodoList(
                    projectId, searchField, searchValue);
            return Result.OK(todoList);
        } catch (Exception e) {
            log.error("查询我的待办失败", e);
            return Result.error("查询待办列表失败：" + e.getMessage());
        }
    }
}
