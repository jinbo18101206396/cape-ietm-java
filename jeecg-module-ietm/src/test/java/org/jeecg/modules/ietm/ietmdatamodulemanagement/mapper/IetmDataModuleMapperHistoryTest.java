package org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 历史版本修复验证 - Mapper层测试
 *
 * 测试目标：验证 selectHistoryVersions 查询返回的对象包含 dmContent 字段
 */
@SpringBootTest
public class IetmDataModuleMapperHistoryTest {

    @Autowired
    private IetmDataModuleMapper ietmDataModuleMapper;

    @Test
    public void testSelectHistoryVersions_DmContentNotNull() {
        System.out.println("========================================");
        System.out.println("测试：历史版本查询返回dmContent字段");
        System.out.println("========================================\n");

        // 测试多组参数
        String[][] testCases = {
            {"DEMO", "001", "A"},
            {"DEMO", "001", null},
            {"ZB1", "A", "02"}
        };

        boolean hasData = false;
        boolean allHaveDmContent = true;
        int totalRecords = 0;
        int recordsWithContent = 0;
        int recordsWithoutContent = 0;

        for (String[] testCase : testCases) {
            String sns = testCase[0];
            String infoCode = testCase[1];
            String infoCodeVariant = testCase[2];

            System.out.println("测试参数: sns=" + sns + ", infoCode=" + infoCode + ", variant=" + infoCodeVariant);

            try {
                List<IetmDataModule> results = ietmDataModuleMapper.selectHistoryVersions(
                    null, sns, infoCode, infoCodeVariant, null, false
                );

                if (results != null && !results.isEmpty()) {
                    hasData = true;
                    System.out.println("  返回记录数: " + results.size());

                    for (int i = 0; i < Math.min(results.size(), 3); i++) {
                        IetmDataModule dm = results.get(i);
                        totalRecords++;

                        System.out.println("\n  记录 #" + (i + 1) + ":");
                        System.out.println("    ID: " + dm.getId());
                        System.out.println("    版本号: " + dm.getIssueNo() + "-" + dm.getInWork());
                        System.out.println("    DMC: " + dm.getDmcCode());

                        // 关键验证
                        String dmContent = dm.getDmContent();
                        if (dmContent != null && !dmContent.isEmpty()) {
                            System.out.println("    dmContent: ✅ 存在 (长度: " + dmContent.length() + " 字符)");
                            recordsWithContent++;
                        } else if (dmContent != null) {
                            System.out.println("    dmContent: ⚠️  空字符串");
                            recordsWithoutContent++;
                            allHaveDmContent = false;
                        } else {
                            System.out.println("    dmContent: ❌ null");
                            recordsWithoutContent++;
                            allHaveDmContent = false;
                        }
                    }
                } else {
                    System.out.println("  返回记录数: 0 (无数据)");
                }
                System.out.println();
            } catch (Exception e) {
                System.err.println("  ❌ 查询失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("========================================");
        System.out.println("测试结果汇总");
        System.out.println("========================================");
        System.out.println("总记录数: " + totalRecords);
        System.out.println("有内容: " + recordsWithContent + " ✅");
        System.out.println("无内容: " + recordsWithoutContent + (recordsWithoutContent > 0 ? " ❌" : ""));
        System.out.println();

        // 断言
        if (!hasData) {
            System.out.println("⚠️  警告：数据库中没有测试数据");
            System.out.println("建议：创建测试数据后重新运行");
            // 不让测试失败，因为可能确实没有数据
        } else {
            System.out.println("✅ 找到测试数据");

            if (allHaveDmContent) {
                System.out.println("✅ 所有记录的 dmContent 字段都有值");
                System.out.println("✅ 修复验证成功！");
            } else {
                System.out.println("❌ 部分记录的 dmContent 字段为空或null");
                System.out.println("❌ 修复可能未生效或数据本身为空");
                fail("部分记录的 dmContent 为空");
            }
        }

        assertTrue(hasData, "数据库应该有历史版本数据用于测试");
        assertTrue(allHaveDmContent || !hasData, "所有有数据的记录应该包含 dmContent 字段");
    }

    @Test
    public void testResultMapMapping() {
        System.out.println("========================================");
        System.out.println("测试：验证 ResultMap 映射正确性");
        System.out.println("========================================\n");

        // 直接查询第一条记录
        try {
            List<IetmDataModule> results = ietmDataModuleMapper.selectHistoryVersions(
                null, "DEMO", "001", null, null, false
            );

            if (results != null && !results.isEmpty()) {
                IetmDataModule dm = results.get(0);

                System.out.println("第一条记录：");
                System.out.println("  ID: " + dm.getId());

                // 验证基本字段（ListResultMap应该有的）
                assertNotNull(dm.getId(), "id 字段应该存在");
                assertNotNull(dm.getIssueNo(), "issueNo 字段应该存在");
                assertNotNull(dm.getInWork(), "inWork 字段应该存在");
                System.out.println("  ✅ 基本字段映射正确（继承自ListResultMap）");

                // 验证新增字段（HistoryResultMap新增的）
                String dmContent = dm.getDmContent();
                assertNotNull(dmContent, "dmContent 字段应该存在（HistoryResultMap新增）");
                System.out.println("  ✅ dmContent 字段映射正确（HistoryResultMap新增）");

                if (dmContent != null && !dmContent.isEmpty()) {
                    System.out.println("  ✅ dmContent 有值：" + dmContent.length() + " 字符");
                    System.out.println("\n✅ ResultMap 映射验证成功！");
                } else {
                    System.out.println("  ⚠️  dmContent 为空字符串（可能数据库本身为空）");
                }
            } else {
                System.out.println("⚠️  无测试数据，跳过映射验证");
            }
        } catch (Exception e) {
            fail("ResultMap映射失败: " + e.getMessage());
        }
    }
}
