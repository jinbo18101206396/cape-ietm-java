package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 诊断版本001-00的dm_content状态
 */
@SpringBootTest
public class DiagnoseDmContentTest {

    @Autowired
    private IetmDataModuleMapper mapper;

    @Test
    public void diagnoseVersion001_00() {
        System.out.println("\n========================================");
        System.out.println("诊断版本001-00的dm_content状态");
        System.out.println("========================================\n");

        // 1. 查询版本001-00
        List<IetmDataModule> results = mapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<IetmDataModule>()
                .eq("issue_no", "001")
                .eq("in_work", "00")
                .orderByDesc("create_time")
        );

        if (results == null || results.isEmpty()) {
            System.out.println("❌ 未找到版本001-00的记录");
            return;
        }

        System.out.println("找到 " + results.size() + " 条记录：\n");

        for (int i = 0; i < results.size(); i++) {
            IetmDataModule dm = results.get(i);
            System.out.println("记录 #" + (i + 1) + ":");
            System.out.println("  ID: " + dm.getId());
            System.out.println("  版本号: " + dm.getIssueNo() + "-" + dm.getInWork());
            System.out.println("  状态: " + dm.getStatus() + " (1=当前/2=历史/0=删除)");
            System.out.println("  DMC: " + dm.getDmcCode());

            // 关键诊断：dm_content状态
            String dmContent = dm.getDmContent();
            if (dmContent == null) {
                System.out.println("  dm_content: ❌ NULL");
            } else if (dmContent.isEmpty()) {
                System.out.println("  dm_content: ❌ 空字符串 (长度=0)");
            } else if (dmContent.length() < 100) {
                System.out.println("  dm_content: ⚠️  过短 (长度=" + dmContent.length() + ")");
                System.out.println("  内容预览: " + dmContent);
            } else {
                System.out.println("  dm_content: ✅ 正常 (长度=" + dmContent.length() + ")");
                System.out.println("  前100字符: " + dmContent.substring(0, 100));
            }

            System.out.println("  创建人: " + dm.getCreateBy());
            System.out.println("  创建时间: " + dm.getCreateTime());
            System.out.println();
        }

        // 2. 查询同一DMC的所有版本
        IetmDataModule first = results.get(0);
        System.out.println("========================================");
        System.out.println("查询同一DMC的所有版本");
        System.out.println("========================================\n");

        List<IetmDataModule> allVersions = mapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<IetmDataModule>()
                .eq("sns", first.getSns())
                .eq("info_code", first.getInfoCode())
                .eq(first.getInfoCodeVariant() != null, "info_code_variant", first.getInfoCodeVariant())
                .orderByAsc("issue_no")
                .orderByAsc("in_work")
        );

        System.out.println("找到 " + allVersions.size() + " 个版本：\n");
        System.out.println("版本号\t\t状态\tXML长度\t内容状态");
        System.out.println("-------------------------------------------");

        for (IetmDataModule v : allVersions) {
            String version = v.getIssueNo() + "-" + v.getInWork();
            String status = v.getStatus();
            String contentStatus;
            Integer contentLength;

            if (v.getDmContent() == null) {
                contentStatus = "❌ NULL";
                contentLength = null;
            } else if (v.getDmContent().isEmpty()) {
                contentStatus = "❌ 空串";
                contentLength = 0;
            } else {
                contentStatus = "✅ 正常";
                contentLength = v.getDmContent().length();
            }

            System.out.printf("%s\t\t%s\t%s\t%s\n",
                version,
                status,
                contentLength != null ? contentLength : "NULL",
                contentStatus);
        }

        // 3. 统计历史版本内容状态
        System.out.println("\n========================================");
        System.out.println("统计所有历史版本的内容状态");
        System.out.println("========================================\n");

        List<IetmDataModule> historyVersions = mapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<IetmDataModule>()
                .eq("status", "2")
        );

        int total = historyVersions.size();
        int nullCount = 0;
        int emptyCount = 0;
        int normalCount = 0;

        for (IetmDataModule v : historyVersions) {
            if (v.getDmContent() == null) {
                nullCount++;
            } else if (v.getDmContent().isEmpty()) {
                emptyCount++;
            } else {
                normalCount++;
            }
        }

        System.out.println("历史版本总数: " + total);
        System.out.println("  NULL数量: " + nullCount + " (" + (total > 0 ? String.format("%.1f%%", nullCount * 100.0 / total) : "0%") + ")");
        System.out.println("  空串数量: " + emptyCount + " (" + (total > 0 ? String.format("%.1f%%", emptyCount * 100.0 / total) : "0%") + ")");
        System.out.println("  正常数量: " + normalCount + " (" + (total > 0 ? String.format("%.1f%%", normalCount * 100.0 / total) : "0%") + ")");

        // 4. 诊断结论
        System.out.println("\n========================================");
        System.out.println("诊断结论");
        System.out.println("========================================\n");

        if (results.get(0).getDmContent() == null) {
            System.out.println("🎯 版本001-00的dm_content为NULL");
            System.out.println("\n可能原因：");
            System.out.println("  1. 创建DM时未填充XML内容（dmContent不是必填字段）");
            System.out.println("  2. 数据导入时只导入了元数据，未导入XML");
            System.out.println("  3. 通过签出链条继承了原始版本的NULL值");

            System.out.println("\n建议修复方案：");
            if (allVersions.stream().anyMatch(v -> "1".equals(v.getStatus()) && v.getDmContent() != null)) {
                System.out.println("  ✅ 方案A（推荐）：从当前版本恢复");
                System.out.println("     该DM有当前版本且有内容，可以从当前版本复制dm_content");
            }
            System.out.println("  ⚠️  方案B：标记为无效版本（删除）");
            System.out.println("  ⚠️  方案C：使用模板填充空白内容");
        } else if (results.get(0).getDmContent().isEmpty()) {
            System.out.println("🎯 版本001-00的dm_content为空字符串");
            System.out.println("\n需要同样的修复方案");
        } else {
            System.out.println("✅ 版本001-00的dm_content正常");
            System.out.println("\n如果前端仍提示无内容，可能原因：");
            System.out.println("  1. 代码未重新编译部署");
            System.out.println("  2. 浏览器缓存未清除");
            System.out.println("  3. 后端返回时被过滤");
        }

        System.out.println("\n========================================\n");
    }
}
