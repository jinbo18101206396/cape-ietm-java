package org.jeecg.modules.ietm.ietmdatamodulemanagement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * XML 同步完整验证测试
 *
 * 该测试会：
 * 1. 查询指定 DM 记录
 * 2. 模拟修改 DMC 字段
 * 3. 调用同步方法
 * 4. 验证 XML 是否已更新
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = org.jeecg.JeecgSystemApplication.class)
public class XmlSyncIntegrationTest {

    @Autowired
    private IIetmDataModuleService dataModuleService;

    @Test
    public void testXmlSyncAfterUpdate() throws Exception {
        String testDmId = "2088664648432721921";

        System.out.println("========================================");
        System.out.println("XML 同步集成测试");
        System.out.println("========================================\n");

        // 1. 查询 DM 记录
        System.out.println("步骤1: 查询 DM 记录...");
        IetmDataModule dm = dataModuleService.getById(testDmId);

        if (dm == null) {
            System.out.println("❌ 未找到 ID = " + testDmId + " 的记录");
            System.out.println("提示: 请在数据库中确认该记录是否存在");
            return;
        }

        System.out.println("  ID: " + dm.getId());
        System.out.println("  DMC: " + dm.getDmcCode());
        System.out.println("  子系统码: " + dm.getSubSystemCode());
        System.out.println("  版本: " + dm.getIssueNo() + "-" + dm.getInWork());
        System.out.println("  ✓ 记录查询成功\n");

        // 2. 检查 XML 内容
        System.out.println("步骤2: 检查 XML 内容...");
        if (dm.getDmContent() == null || dm.getDmContent().trim().isEmpty()) {
            System.out.println("⚠️ DM 内容为空，无法测试同步功能");
            return;
        }

        Document doc = DocumentHelper.parseText(dm.getDmContent());
        Element root = doc.getRootElement();
        Element identAndStatus = root.element("identAndStatusSection");

        if (identAndStatus == null) {
            System.out.println("⚠️ XML 缺少 identAndStatusSection 节点");
            return;
        }

        Element dmAddress = identAndStatus.element("dmAddress");
        Element dmIdent = dmAddress.element("dmIdent");
        Element dmCode = dmIdent.element("dmCode");

        String originalSubSystem = dmCode.attributeValue("subSystemCode");
        System.out.println("  原 XML 中的 subSystemCode: " + originalSubSystem);
        System.out.println("  ✓ XML 解析成功\n");

        // 3. 模拟修改字段
        System.out.println("步骤3: 模拟修改子系统码...");
        String newSubSystem = originalSubSystem.equals("1") ? "2" : "1";
        dm.setSubSystemCode(newSubSystem);
        System.out.println("  数据库字段 subSystemCode: " + originalSubSystem + " → " + newSubSystem);
        System.out.println("  ✓ 字段修改完成\n");

        // 4. 调用同步方法
        System.out.println("步骤4: 调用 XML 同步方法...");
        String syncedXml = DmXmlHelper.syncDmIdentToXml(dm.getDmContent(), dm);

        if (syncedXml == null || syncedXml.equals(dm.getDmContent())) {
            System.out.println("⚠️ 同步方法未修改 XML");
        }

        System.out.println("  ✓ 同步方法执行完成\n");

        // 5. 验证同步结果
        System.out.println("步骤5: 验证 XML 同步结果...");
        Document syncedDoc = DocumentHelper.parseText(syncedXml);
        Element syncedRoot = syncedDoc.getRootElement();
        Element syncedIdentAndStatus = syncedRoot.element("identAndStatusSection");
        Element syncedDmAddress = syncedIdentAndStatus.element("dmAddress");
        Element syncedDmIdent = syncedDmAddress.element("dmIdent");
        Element syncedDmCode = syncedDmIdent.element("dmCode");

        String syncedSubSystem = syncedDmCode.attributeValue("subSystemCode");
        System.out.println("  同步后 XML 中的 subSystemCode: " + syncedSubSystem);

        // 验证
        if (syncedSubSystem.equals(newSubSystem)) {
            System.out.println("  ✅ XML 同步成功！");
        } else {
            System.out.println("  ❌ XML 同步失败！");
            System.out.println("     期望: " + newSubSystem);
            System.out.println("     实际: " + syncedSubSystem);
            throw new AssertionError("XML 同步失败");
        }

        System.out.println("\n========================================");
        System.out.println("测试结论: XML 同步功能正常 ✅");
        System.out.println("========================================");
    }
}
