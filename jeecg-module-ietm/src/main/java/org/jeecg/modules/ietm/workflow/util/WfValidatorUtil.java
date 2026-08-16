package org.jeecg.modules.ietm.workflow.util;

import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;

import java.util.regex.Pattern;

/**
 * @Description: 工作流数据验证工具类
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 */
public class WfValidatorUtil {

    /** 用户ID格式：字母、数字、下划线、逗号、短横线 */
    private static final Pattern USERID_PATTERN = Pattern.compile("^[a-zA-Z0-9_,\\-]+$");

    /** 前缀格式：dpt_/rol_/pst_/grp_ + 字母数字短横线 */
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(dpt|rol|pst|grp)_[a-zA-Z0-9\\-]+$");

    /**
     * 验证处理人ID格式
     * @param userid 处理人ID（逗号分隔，支持前缀）
     * @throws JeecgBootException 格式不合法时抛出异常
     */
    public static void validateUserid(String userid) {
        if (StringUtils.isBlank(userid)) {
            throw new JeecgBootException("处理人ID不能为空");
        }

        // 验证整体格式
        if (!USERID_PATTERN.matcher(userid).matches()) {
            throw new JeecgBootException("处理人ID格式不合法，只能包含字母、数字、下划线、逗号和短横线");
        }

        // 验证每个ID的前缀格式
        for (String id : userid.split(",")) {
            String trimmedId = id.trim();
            if (trimmedId.contains("_")) {
                if (!PREFIX_PATTERN.matcher(trimmedId).matches()) {
                    throw new JeecgBootException("处理人ID前缀格式错误：" + trimmedId +
                        "，支持的前缀：dpt_(部门)、rol_(角色)、pst_(岗位)、grp_(用户组)");
                }
            }
        }
    }

    /**
     * 验证节点名称
     */
    public static void validateNodename(String nodename) {
        if (StringUtils.isBlank(nodename)) {
            throw new JeecgBootException("节点名称不能为空");
        }
        if (nodename.length() > 100) {
            throw new JeecgBootException("节点名称长度不能超过100个字符");
        }
    }

    /**
     * 验证批次ID格式（UUID格式）
     */
    public static void validateBatchId(String batchId) {
        if (StringUtils.isBlank(batchId)) {
            throw new JeecgBootException("批次ID不能为空");
        }
        // UUID格式：8-4-4-4-12
        Pattern uuidPattern = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        if (!uuidPattern.matcher(batchId.toLowerCase()).matches()) {
            throw new JeecgBootException("批次ID格式不正确（应为UUID格式）");
        }
    }
}
