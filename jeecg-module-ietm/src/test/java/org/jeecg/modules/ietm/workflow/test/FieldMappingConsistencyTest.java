package org.jeecg.modules.ietm.workflow.test;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.workflow.entity.WfExecute;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.entity.WfInstanceDtl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字段映射一致性验证测试
 *
 * 目的：系统验证实体类 @TableField 注解与数据库表结构的一致性
 * 防止类似 instid/instanceid 的字段映射错误再次发生
 *
 * @author IETM Team
 * @date 2026-08-20
 */
@Slf4j
@SpringBootTest
@DisplayName("字段映射一致性验证测试")
public class FieldMappingConsistencyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("验证1：WfInstance 实体类字段映射一致性")
    public void testWfInstanceFieldMapping() {
        String tableName = "WF_INSTANCE";
        Class<?> entityClass = WfInstance.class;

        Map<String, String> issues = verifyEntityFieldMapping(tableName, entityClass);

        if (!issues.isEmpty()) {
            log.error("WfInstance 字段映射发现 {} 个问题：", issues.size());
            issues.forEach((field, issue) -> log.error("  - {}: {}", field, issue));
        }

        assertTrue(issues.isEmpty(),
            "WfInstance 存在字段映射问题：" + issues);
    }

    @Test
    @DisplayName("验证2：WfInstanceDtl 实体类字段映射一致性")
    public void testWfInstanceDtlFieldMapping() {
        String tableName = "WF_INSTANCE_DTL";
        Class<?> entityClass = WfInstanceDtl.class;

        Map<String, String> issues = verifyEntityFieldMapping(tableName, entityClass);

        if (!issues.isEmpty()) {
            log.error("WfInstanceDtl 字段映射发现 {} 个问题：", issues.size());
            issues.forEach((field, issue) -> log.error("  - {}: {}", field, issue));
        }

        assertTrue(issues.isEmpty(),
            "WfInstanceDtl 存在字段映射问题：" + issues);
    }

    @Test
    @DisplayName("验证3：WfExecute 实体类字段映射一致性")
    public void testWfExecuteFieldMapping() {
        String tableName = "WF_EXECUTE";
        Class<?> entityClass = WfExecute.class;

        Map<String, String> issues = verifyEntityFieldMapping(tableName, entityClass);

        if (!issues.isEmpty()) {
            log.error("WfExecute 字段映射发现 {} 个问题：", issues.size());
            issues.forEach((field, issue) -> log.error("  - {}: {}", field, issue));
        }

        assertTrue(issues.isEmpty(),
            "WfExecute 存在字段映射问题：" + issues);
    }

    @Test
    @DisplayName("验证4：检查 instid/instanceid 命名一致性")
    public void testInstidNamingConsistency() {
        // 检查 wf_instance_dtl 表中的实际字段名
        String sql = "SELECT COLUMN_NAME FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = 'WF_INSTANCE_DTL' " +
                    "AND (COLUMN_NAME LIKE '%INST%' OR COLUMN_NAME LIKE '%INSTANCE%')";

        List<String> columns = jdbcTemplate.queryForList(sql, String.class);

        log.info("wf_instance_dtl 表中包含 INST 的字段：{}", columns);

        // 应该只有 instid_，不应该有 instanceid_
        boolean hasInstid = columns.stream()
            .anyMatch(c -> c.equalsIgnoreCase("INSTID_"));
        boolean hasInstanceid = columns.stream()
            .anyMatch(c -> c.equalsIgnoreCase("INSTANCEID_"));

        assertTrue(hasInstid, "应该存在 INSTID_ 字段");
        assertFalse(hasInstanceid, "不应该存在 INSTANCEID_ 字段（已统一使用 INSTID_）");

        // 验证实体类使用的是 instid_
        try {
            Field field = WfInstanceDtl.class.getDeclaredField("instanceid");
            com.baomidou.mybatisplus.annotation.TableField annotation =
                field.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);

            assertNotNull(annotation, "instanceid 属性应该有 @TableField 注解");
            assertEquals("instid_", annotation.value(),
                "instanceid 属性应该映射到 instid_ 字段（不是 instanceid_）");

            log.info("✓ WfInstanceDtl.instanceid 正确映射到 instid_ 字段");

        } catch (NoSuchFieldException e) {
            fail("找不到 instanceid 字段：" + e.getMessage());
        }
    }

    /**
     * 验证实体类字段映射一致性的核心方法
     *
     * @param tableName 数据库表名
     * @param entityClass 实体类
     * @return 发现的问题映射（字段名 -> 问题描述）
     */
    private Map<String, String> verifyEntityFieldMapping(String tableName, Class<?> entityClass) {
        Map<String, String> issues = new HashMap<>();

        // 1. 查询数据库表的实际字段
        String sql = "SELECT COLUMN_NAME FROM USER_TAB_COLUMNS " +
                    "WHERE TABLE_NAME = ? ORDER BY COLUMN_ID";
        List<String> dbColumns = jdbcTemplate.queryForList(sql, String.class, tableName);

        log.info("数据库表 {} 包含 {} 个字段", tableName, dbColumns.size());

        // 2. 提取实体类的 @TableField 注解
        Map<String, String> entityFieldMapping = new HashMap<>();
        for (Field field : entityClass.getDeclaredFields()) {
            com.baomidou.mybatisplus.annotation.TableField annotation =
                field.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);

            if (annotation != null && annotation.value() != null && !annotation.value().isEmpty()) {
                entityFieldMapping.put(field.getName(), annotation.value().toUpperCase());
            }
        }

        log.info("实体类 {} 包含 {} 个 @TableField 注解",
            entityClass.getSimpleName(), entityFieldMapping.size());

        // 3. 验证映射的字段是否在数据库中存在
        for (Map.Entry<String, String> entry : entityFieldMapping.entrySet()) {
            String javaField = entry.getKey();
            String dbField = entry.getValue();

            if (!dbColumns.contains(dbField)) {
                issues.put(javaField,
                    String.format("@TableField(\"%s\") 映射的字段在数据库中不存在", dbField));
            }
        }

        // 4. 特别检查已知的易错字段
        if (tableName.equals("WF_INSTANCE_DTL")) {
            // 检查 instid_ 字段的映射
            if (dbColumns.contains("INSTID_")) {
                boolean correctlyMapped = entityFieldMapping.values().stream()
                    .anyMatch(v -> v.equals("INSTID_"));

                if (!correctlyMapped) {
                    issues.put("instid", "INSTID_ 字段未正确映射到实体类");
                }
            }
        }

        return issues;
    }
}
