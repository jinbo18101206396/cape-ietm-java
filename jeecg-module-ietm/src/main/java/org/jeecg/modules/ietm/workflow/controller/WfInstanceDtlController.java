package org.jeecg.modules.ietm.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Description: 工作流节点明细控制器
 * @Author: IETM Team
 * @Date: 2026-08-20
 * @Version: V1.0
 */
@Slf4j
@Api(tags = "工作流节点管理")
@RestController
@RequestMapping("/ietm/workflow/dtl")
public class WfInstanceDtlController {

    @Autowired
    private WfInstanceDtlMapper wfInstanceDtlMapper;

    @Autowired
    private WfInstanceMapper wfInstanceMapper;

    /**
     * 查询节点列表
     */
    @ApiOperation(value = "查询节点列表", notes = "根据实例ID查询节点列表")
    @GetMapping("/list")
    public Result<?> list(@ApiParam("实例ID") @RequestParam String instid) {
        try {
            List<WfInstanceDtl> nodes = wfInstanceDtlMapper.selectByInstId(instid);
            return Result.OK(nodes);
        } catch (Exception e) {
            log.error("查询节点列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 批量保存节点
     */
    @ApiOperation(value = "批量保存节点", notes = "批量保存节点配置（支持新增和更新）")
    @PostMapping("/saveBatch")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> saveBatch(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodesData = (List<Map<String, Object>>) params.get("nodes");

            if (nodesData == null || nodesData.isEmpty()) {
                return Result.error("节点数据不能为空");
            }

            // 获取实例ID（从第一个节点获取）
            String instid = (String) nodesData.get(0).get("instid");
            if (instid == null || instid.trim().isEmpty()) {
                return Result.error("实例ID不能为空");
            }

            // 验证流程实例是否存在
            WfInstance instance = wfInstanceMapper.selectById(instid);
            if (instance == null) {
                return Result.error("流程实例不存在");
            }

            // P0-11: 预先校验顺序号唯一性（防止重复）
            java.util.Set<Integer> seqnoSet = new java.util.HashSet<>();
            for (Map<String, Object> nodeData : nodesData) {
                Integer seqno = safeParseInt(nodeData.get("seqno"), "节点顺序号");
                if (seqno != null && !seqnoSet.add(seqno)) {
                    return Result.error("顺序号不能重复：" + seqno);
                }
            }

            // 校验节点数据
            for (int i = 0; i < nodesData.size(); i++) {
                Map<String, Object> nodeData = nodesData.get(i);

                // 必填字段校验
                String nodename = (String) nodeData.get("nodename");
                if (nodename == null || nodename.trim().isEmpty()) {
                    return Result.error("节点名称不能为空");
                }

                String userid = (String) nodeData.get("userid");
                if (userid == null || userid.trim().isEmpty()) {
                    return Result.error("处理人不能为空");
                }

                // 顺序号校验
                Integer seqno = safeParseInt(nodeData.get("seqno"), "节点顺序号");
                if (seqno == null || seqno < 0) {
                    return Result.error("顺序号必须是非负整数");
                }

                // 校验顺序号等于索引（0,1,2,3...）
                if (seqno != i) {
                    return Result.error("顺序号必须连续（如0,1,2,3），当前缺少：" + i);
                }

                // 处理人格式校验（支持rol_/dpt_/grp_/pst_前缀）
                String[] userIds = userid.split(",");
                for (String uid : userIds) {
                    String trimmedUid = uid.trim();
                    if (!trimmedUid.matches("^[0-9a-zA-Z_]+$") &&
                        !trimmedUid.matches("^(rol|dpt|grp|pst)_[0-9]+$")) {
                        return Result.error("处理人格式错误：" + trimmedUid);
                    }
                }

                // 节点类型校验
                String nodetype = (String) nodeData.get("nodetype");
                if (nodetype != null && !Arrays.asList("0", "1", "2").contains(nodetype)) {
                    return Result.error("节点类型必须是0/1/2");
                }
            }

            // P0-12: 校验不能在已处理节点之前插入新节点
            Integer execedSeqno = wfInstanceDtlMapper.selectExecutedMaxSeqno(instid);
            if (execedSeqno != null) {
                for (Map<String, Object> nodeData : nodesData) {
                    String id = (String) nodeData.get("id");
                    Integer seqno = safeParseInt(nodeData.get("seqno"), "节点顺序号");

                    // 新增节点且顺序号小于已处理最大顺序号
                    if ((id == null || id.startsWith("new_")) && seqno > 0 && seqno <= execedSeqno) {
                        return Result.error("不能在已处理节点（顺序号≤" + execedSeqno + "）之前插入新节点");
                    }
                }
            }

            // P0-07: 分阶段流程校验 - 已结束阶段不能新增节点
            if (instance.getStagenames() != null && !instance.getStagenames().trim().isEmpty()) {
                // 分阶段流程
                for (Map<String, Object> nodeData : nodesData) {
                    String id = (String) nodeData.get("id");
                    String stagename = (String) nodeData.get("stagename");

                    if ((id == null || id.startsWith("new_")) && stagename != null && !stagename.trim().isEmpty()) {
                        // 新增节点，查询该阶段最后一个节点状态
                        WfInstanceDtl lastNodeInStage = wfInstanceDtlMapper.selectLastNodeByStage(instid, stagename);
                        if (lastNodeInStage != null && "Y".equals(lastNodeInStage.getIfexec())) {
                            return Result.error("阶段【" + stagename + "】已结束，不能新增节点");
                        }
                    }
                }
            }

            // 保存节点
            for (Map<String, Object> nodeData : nodesData) {
                String id = (String) nodeData.get("id");
                String nodename = (String) nodeData.get("nodename");
                String userid = (String) nodeData.get("userid");
                Integer seqno = safeParseInt(nodeData.get("seqno"), "节点顺序号");
                String nodetype = (String) nodeData.get("nodetype");
                String stagename = (String) nodeData.get("stagename");
                String ifgetback = (String) nodeData.get("ifgetback");
                String ifnoopinion = (String) nodeData.get("ifnoopinion");

                WfInstanceDtl node = new WfInstanceDtl();
                node.setInstanceid(instid);
                node.setNodename(nodename);
                node.setUserid(userid);
                node.setSeqno(seqno);
                node.setNodetype(nodetype != null ? nodetype : "0");
                node.setStagename(stagename);
                node.setIfgetback(ifgetback);
                node.setIfnoopinion(ifnoopinion != null ? ifnoopinion : "N");
                node.setIfexec("N");

                if (id == null || id.startsWith("new_")) {
                    // 新增节点
                    wfInstanceDtlMapper.insert(node);
                } else {
                    // 更新节点
                    node.setId(id);
                    wfInstanceDtlMapper.updateById(node);
                }
            }

            return Result.OK("保存成功");
        } catch (Exception e) {
            log.error("批量保存节点失败", e);
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 单节点保存（新增/更新，不重置已处理状态）
     * 与 saveBatch 的区别：只影响本节点，不动其它节点的 ifexec，适用于运行中流程的行内编辑。
     */
    @ApiOperation(value = "单节点保存", notes = "新增或更新单个节点，保留已处理状态")
    @PostMapping("/saveNode")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> saveNode(@RequestBody Map<String, Object> nodeData) {
        try {
            String instid = (String) nodeData.get("instid");
            if (instid == null || instid.trim().isEmpty()) {
                return Result.error("实例ID不能为空");
            }

            WfInstance instance = wfInstanceMapper.selectById(instid);
            if (instance == null) {
                return Result.error("流程实例不存在");
            }

            // 必填校验（与 saveBatch 一致）
            String nodename = (String) nodeData.get("nodename");
            if (nodename == null || nodename.trim().isEmpty()) {
                return Result.error("节点名称不能为空");
            }
            String userid = (String) nodeData.get("userid");
            if (userid == null || userid.trim().isEmpty()) {
                return Result.error("处理人不能为空");
            }
            Integer seqno = safeParseInt(nodeData.get("seqno"), "节点顺序号");
            if (seqno == null || seqno < 0) {
                return Result.error("顺序号必须是非负整数");
            }
            String nodetype = (String) nodeData.get("nodetype");
            if (nodetype != null && !Arrays.asList("0", "1", "2").contains(nodetype)) {
                return Result.error("节点类型必须是0/1/2");
            }

            // 处理人格式校验（与 saveBatch 一致，支持 rol_/dpt_/grp_/pst_ 前缀）
            for (String uid : userid.split(",")) {
                String t = uid.trim();
                if (!t.matches("^[0-9a-zA-Z_]+$") && !t.matches("^(rol|dpt|grp|pst)_[0-9]+$")) {
                    return Result.error("处理人格式错误：" + t);
                }
            }

            String id = (String) nodeData.get("id");
            String stagename = (String) nodeData.get("stagename");
            String ifgetback = (String) nodeData.get("ifgetback");
            String ifnoopinion = (String) nodeData.get("ifnoopinion");
            String useridname = (String) nodeData.get("useridname");

            if (id == null || id.trim().isEmpty() || id.startsWith("new_")) {
                // 🔧 新需求：新增节点时，处理人必须是当前用户
                try {
                    LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
                    if (loginUser != null) {
                        String currentUserId = loginUser.getId();
                        String currentUsername = loginUser.getUsername();

                        // 校验处理人是否为当前用户
                        if (!userid.equals(currentUserId) && !userid.equals(currentUsername)) {
                            return Result.error("新增节点的处理人必须是当前用户");
                        }
                    }
                } catch (Exception e) {
                    log.warn("[saveNode] 获取当前用户失败，跳过新增节点处理人校验", e);
                }

                // 新增：ifexec 置 N
                WfInstanceDtl node = new WfInstanceDtl();
                node.setInstanceid(instid);
                node.setNodename(nodename);
                node.setUserid(userid);

                // P1修复简化版：如果前端未传 useridname，使用 userid（前端已在新增节点时自动填充）
                if (useridname == null || useridname.trim().isEmpty()) {
                    log.info("[saveNode] 前端未传useridname，使用userid作为兜底，userid={}", userid);
                    useridname = userid;
                }
                node.setUseridname(useridname);

                node.setSeqno(seqno);
                node.setNodetype(nodetype != null ? nodetype : "0");
                node.setStagename(stagename);
                node.setIfgetback(ifgetback);
                node.setIfnoopinion(ifnoopinion != null ? ifnoopinion : "N");
                node.setIfexec("N");
                wfInstanceDtlMapper.insert(node);
            } else {
                // 更新：先取原节点，禁止改动已处理节点，且不覆盖 ifexec
                WfInstanceDtl existing = wfInstanceDtlMapper.selectById(id);
                if (existing == null) {
                    return Result.error("节点不存在");
                }
                if (!"N".equals(existing.getIfexec()) && !"R".equals(existing.getIfexec())) {
                    return Result.error("只能编辑未处理或退回的节点");
                }
                existing.setNodename(nodename);
                existing.setUserid(userid);

                // P1修复简化版：如果前端未传 useridname，使用 userid（前端已自动填充）
                if (useridname == null || useridname.trim().isEmpty()) {
                    log.info("[saveNode] 更新节点时前端未传useridname，使用userid作为兜底，userid={}", userid);
                    useridname = userid;
                }
                existing.setUseridname(useridname);

                existing.setSeqno(seqno);
                existing.setNodetype(nodetype != null ? nodetype : existing.getNodetype());
                existing.setStagename(stagename);
                existing.setIfgetback(ifgetback);
                if (ifnoopinion != null) existing.setIfnoopinion(ifnoopinion);
                // 不动 ifexec / ifjump
                wfInstanceDtlMapper.updateById(existing);
            }

            return Result.OK("保存成功");
        } catch (Exception e) {
            log.error("单节点保存失败", e);
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 删除节点
     */
    @ApiOperation(value = "删除节点", notes = "删除指定节点")
    @DeleteMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(@ApiParam("节点ID") @RequestParam String id) {
        try {
            WfInstanceDtl node = wfInstanceDtlMapper.selectById(id);
            if (node == null) {
                return Result.error("节点不存在");
            }

            // 校验节点状态（只能删除未处理的节点）
            if (!"N".equals(node.getIfexec())) {
                return Result.error("只能删除未处理的节点");
            }

            // 逻辑删除
            wfInstanceDtlMapper.deleteById(id);

            return Result.OK("删除成功");
        } catch (Exception e) {
            log.error("删除节点失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 查询节点详情
     */
    @ApiOperation(value = "查询节点详情", notes = "根据节点ID查询详情")
    @GetMapping("/get")
    public Result<?> get(@ApiParam("节点ID") @RequestParam String id) {
        try {
            WfInstanceDtl node = wfInstanceDtlMapper.selectById(id);
            if (node == null) {
                return Result.error("节点不存在");
            }
            return Result.OK(node);
        } catch (Exception e) {
            log.error("查询节点详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 查询已处理节点的最大顺序号
     */
    @ApiOperation(value = "查询已处理节点的最大顺序号", notes = "用于计算新增节点的插入位置")
    @GetMapping("/getExecutedMaxSeqno")
    public Result<?> getExecutedMaxSeqno(@ApiParam("实例ID") @RequestParam String instid) {
        try {
            Integer maxSeqno = wfInstanceDtlMapper.selectExecutedMaxSeqno(instid);
            return Result.OK(maxSeqno != null ? maxSeqno : -1);
        } catch (Exception e) {
            log.error("查询已处理节点的最大顺序号失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * P0-3修复：安全的整数解析工具方法
     *
     * @param obj 待解析的对象
     * @param fieldName 字段名称（用于错误提示）
     * @return 解析后的整数，如果obj为null则返回null
     * @throws JeecgBootException 当解析失败时抛出，包含友好的错误提示
     */
    private Integer safeParseInt(Object obj, String fieldName) {
        if (obj == null) {
            return null;
        }
        try {
            String str = obj.toString().trim();
            if (str.isEmpty()) {
                return null;
            }
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            log.warn("{}格式错误，期望整数，实际值：{}", fieldName, obj);
            throw new JeecgBootException(fieldName + "格式错误，必须为整数（实际值：" + obj + "）");
        }
    }
}
