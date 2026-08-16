package org.jeecg.modules.ietm.common.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.common.service.ISnsCalculateService;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SNS编码计算服务实现
 * 对标老系统：
 *   DM算法  → IetmDmAdd.jsp:199-213（coderule补全 + i=4/7位合并）
 *   ICN算法 → IetmIcnAdd.jsp:118-121（取前6段 + i>=3连写）
 * 【方案A】SNS 含 equipname 首段，与老系统一致。
 *
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 */
@Slf4j
@Service
public class SnsCalculateServiceImpl implements ISnsCalculateService {

    @Autowired
    private IIetmProjectConfigurationManagementService configurationService;

    /** 向后兼容入口，默认走 DM 算法 */
    @Override
    public String calculateSns(String cmNodeId) {
        return calculateSnsForDm(cmNodeId);
    }

    /**
     * DM 模块 SNS 计算（对标 IetmDmAdd.jsp:199-213）
     * 路径含 equipname 首段（index 0），i=4/7 不加 "-"
     * （subSubSystem 连 subSystem、disassyCodeVariant 连 disassyCode），末尾 substring(1) 去掉开头 "-"
     */
    @Override
    public String calculateSnsForDm(String cmNodeId) {
        List<String> path = buildConfigPathWithPadding(cmNodeId);
        if (path.size() < 2) {
            log.warn("构型路径不足2层，无法生成DM SNS，cmNodeId={}", cmNodeId);
            return "";
        }
        // 对标 IetmDmAdd.jsp:210: sns += (i==4||i==7?'':'-') + m
        StringBuilder sns = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i == 4 || i == 7) {
                sns.append(path.get(i));
            } else {
                sns.append("-").append(path.get(i));
            }
        }
        return sns.length() > 0 ? sns.substring(1) : "";
    }

    /**
     * ICN 模块 SNS 计算（对标 IetmIcnAdd.jsp:118-121）
     * 取路径前 6 段，i>=3 不加 "-"（subSystem/subSubSystem/assy 连写）
     */
    @Override
    public String calculateSnsForIcn(String cmNodeId) {
        List<String> path = buildConfigPathWithPadding(cmNodeId);
        if (path.size() < 2) {
            log.warn("构型路径不足2层，无法生成ICN SNS，cmNodeId={}", cmNodeId);
            return "";
        }
        // 对标 IetmIcnAdd.jsp:120: if(i<6) sns += (i>=3?'':'-') + m
        StringBuilder sns = new StringBuilder();
        for (int i = 0; i < Math.min(6, path.size()); i++) {
            if (i >= 3) {
                sns.append(path.get(i));
            } else {
                sns.append("-").append(path.get(i));
            }
        }
        return sns.length() > 0 ? sns.substring(1) : "";
    }

    /**
     * 递归构建构型路径并用 coderule 补全（含 equipname 首段）
     * 对标老系统 IetmDmAdd.jsp:199-205
     */
    private List<String> buildConfigPathWithPadding(String cmNodeId) {
        // 1. 获取起始节点（同时取到 projectId，避免重复查库）
        IetmProjectConfigurationManagement startNode = configurationService.getById(cmNodeId);
        if (startNode == null) {
            log.warn("构型节点不存在，无法生成SNS: cmNodeId={}", cmNodeId);
            return new ArrayList<>();
        }
        String projectId = startNode.getProjectId();

        // 2. 递归构建路径（从叶到根，含根节点 code = equipname）
        List<String> path = new ArrayList<>();
        path.add(startNode.getCode());
        if (startNode.getPid() != null && !startNode.getPid().trim().isEmpty()
                && !"0".equals(startNode.getPid())) {
            buildConfigPath(startNode.getPid(), path);
        }
        Collections.reverse(path);   // 反转为 [equipname, level1, level2, ...]

        // 3. coderule 补全：对标老系统 splice(nodePath.length) 取 coderule 剩余段追加
        //    fullCoderule = equipname + "-" + codeRule（共8段），与 path[0]=equipname 对齐
        String codeRule = configurationService.getCodeRuleByProjectId(projectId);
        String equipname = path.get(0);
        String[] coderuleParts = (equipname + "-" + codeRule).split("-");
        if (path.size() < coderuleParts.length) {
            for (int i = path.size(); i < coderuleParts.length; i++) {
                path.add(coderuleParts[i]);
            }
        }
        log.debug("[buildConfigPathWithPadding] cmNodeId={}, path(补全后)={}", cmNodeId, path);
        return path;
    }

    /**
     * 递归构建构型路径（从叶到根，含根节点 code）
     */
    private void buildConfigPath(String cmNodeId, List<String> path) {
        if (path.size() >= 20) {
            log.warn("构型路径深度超过20层，停止递归，cmNodeId={}", cmNodeId);
            return;
        }
        IetmProjectConfigurationManagement config = configurationService.getById(cmNodeId);
        if (config != null) {
            path.add(config.getCode());
            if (config.getPid() != null && !config.getPid().trim().isEmpty()
                    && !"0".equals(config.getPid())) {
                buildConfigPath(config.getPid(), path);
            }
        }
    }
}
