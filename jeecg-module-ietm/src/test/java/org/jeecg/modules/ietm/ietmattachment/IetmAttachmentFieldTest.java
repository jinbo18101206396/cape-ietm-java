package org.jeecg.modules.ietm.ietmattachment;

import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IetmAttachment 实体类字段验证测试
 *
 * 目的：验证 IetmAttachment 实体类中不存在 business_id 和 business_type 字段，
 * 确保代码中使用的是正确的 pid 字段。
 *
 * @author Kiro
 * @date 2026-09-17
 */
@DisplayName("IetmAttachment 实体类字段验证")
public class IetmAttachmentFieldTest {

    @Test
    @DisplayName("TC-01: 验证IetmAttachment实体类不包含business_id字段")
    void testNoBusinessIdField() {
        // Given: IetmAttachment类
        Class<IetmAttachment> clazz = IetmAttachment.class;

        // When: 获取所有字段名
        List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        // Then: 不应包含business_id
        assertThat(fieldNames)
                .as("IetmAttachment不应包含business_id字段")
                .doesNotContain("businessId", "business_id");
    }

    @Test
    @DisplayName("TC-02: 验证IetmAttachment实体类不包含business_type字段")
    void testNoBusinessTypeField() {
        // Given: IetmAttachment类
        Class<IetmAttachment> clazz = IetmAttachment.class;

        // When: 获取所有字段名
        List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        // Then: 不应包含business_type
        assertThat(fieldNames)
                .as("IetmAttachment不应包含business_type字段")
                .doesNotContain("businessType", "business_type");
    }

    @Test
    @DisplayName("TC-03: 验证IetmAttachment实体类包含pid字段")
    void testHasPidField() {
        // Given: IetmAttachment类
        Class<IetmAttachment> clazz = IetmAttachment.class;

        // When: 获取所有字段名
        List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        // Then: 应包含pid字段（父ID，用于关联业务实体）
        assertThat(fieldNames)
                .as("IetmAttachment应包含pid字段用于关联父实体")
                .contains("pid");
    }

    @Test
    @DisplayName("TC-04: 验证IetmAttachment实体类包含fileType字段")
    void testHasFileTypeField() {
        // Given: IetmAttachment类
        Class<IetmAttachment> clazz = IetmAttachment.class;

        // When: 获取所有字段名
        List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        // Then: 应包含fileType字段（区分实体文件和相关文件）
        assertThat(fieldNames)
                .as("IetmAttachment应包含fileType字段用于区分附件类型")
                .contains("fileType");
    }

    @Test
    @DisplayName("TC-05: 列出IetmAttachment所有字段供参考")
    void testListAllFields() {
        // Given: IetmAttachment类
        Class<IetmAttachment> clazz = IetmAttachment.class;

        // When: 获取所有字段
        List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .sorted()
                .collect(Collectors.toList());

        // Then: 打印所有字段
        System.out.println("=== IetmAttachment 实体类字段列表 ===");
        fieldNames.forEach(name -> System.out.println("  - " + name));

        // 验证核心字段存在
        assertThat(fieldNames)
                .as("应包含核心字段")
                .contains("id", "pid", "fileName", "fileKey", "fileType", "fileSize");
    }

    @Test
    @DisplayName("TC-06: 验证正确的查询字段组合")
    void testCorrectQueryFieldCombination() {
        // Given: 正确的查询应该使用 pid + fileType
        String correctQuery1 = "pid";
        String correctQuery2 = "fileType";

        // When: 错误的查询字段
        String wrongQuery1 = "business_id";
        String wrongQuery2 = "business_type";

        // Then: 获取实体字段验证
        List<String> fieldNames = Arrays.stream(IetmAttachment.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        assertThat(fieldNames)
                .as("正确的查询字段应存在")
                .contains(correctQuery1, correctQuery2);

        assertThat(fieldNames)
                .as("错误的查询字段不应存在")
                .doesNotContain(wrongQuery1, wrongQuery2);
    }
}
