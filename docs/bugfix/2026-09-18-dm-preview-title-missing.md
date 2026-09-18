# DM预览标题丢失问题修复

**日期**: 2026-09-18  
**问题**: DM预览界面不显示标题和状况信息  
**严重程度**: 高（影响用户体验）

## 问题现象

在DM预览界面中，应该显示的标题、DMC代码、发行日期等状况信息完全不显示，只显示内容正文。

测试DM: `DMC-ZB1-A-04-00-00-00A-007A-A_003-00_zh-CN`（使用限制（总论）-符号清单1）

## 根本原因

**正则表达式匹配错误导致HTML结构被误删**

在 `DmXmlHelper.fixLegacyFunctionCalls()` 方法中，用于清理旧系统遗留script标签的正则表达式存在严重缺陷：

```java
// 错误的正则表达式
private static final Pattern LEGACY_SCRIPT_PATTERN = Pattern.compile(
    "<script[^>]*>.*?(lessonPath|lessonCode).*?</script>",
    Pattern.DOTALL
);
```

### 问题分析

1. **贪婪匹配导致跨标签匹配**: 虽然使用了 `.*?` 非贪婪量词，但在 `Pattern.DOTALL` 模式下，`.` 会匹配换行符
2. **匹配范围过大**: 从**任意一个 `<script>` 标签**开始匹配，直到找到包含 `lessonPath` 或 `lessonCode` 的 `</script>` 结束
3. **误删HTML结构**: 如果HTML中存在：
   ```html
   <script>...</script>
   <div id="dmview">...</div>
   <div id="idstatus">...</div>
   <script>... lessonPath ...</script>
   ```
   正则会匹配并删除从第一个 `<script>` 到最后一个包含 `lessonPath` 的 `</script>` 之间的**所有内容**，包括中间的所有div标签！

### 调试过程

通过逐步保存HTML处理管道的中间结果发现：

| 步骤 | 文件大小 | 包含idstatus div |
|------|---------|-----------------|
| XSLT转换输出 | 3.4KB | ✅ 是 |
| CSS增强后 | 20KB | ✅ 是 |
| ICN路径替换后 | 244KB | ✅ 是 |
| **fixLegacyFunctionCalls后** | **130KB** | **❌ 否** |
| Service返回 | 130KB | ❌ 否 |

明确定位到问题出在 `fixLegacyFunctionCalls` 方法。

## 修复方案

使用**否定前瞻**确保正则只匹配单个script标签内容：

```java
// 修复后的正则表达式
private static final Pattern LEGACY_SCRIPT_PATTERN = Pattern.compile(
    "<script[^>]*>(?:(?!</?script>).)*(?:lessonPath|lessonCode)(?:(?!</?script>).)*</script>",
    Pattern.DOTALL
);
```

### 修复原理

- `(?:(?!</?script>).)*`: 匹配任意字符，但遇到 `<script>` 或 `</script>` 时停止
- `(?:lessonPath|lessonCode)`: 匹配目标关键字
- 确保只匹配单个 `<script>...</script>` 标签对，不会跨越到其他标签

## 影响范围

**修改文件**:
- `jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/DmXmlHelper.java`
  - 行606-609: 修复 `LEGACY_SCRIPT_PATTERN` 正则表达式

**影响功能**:
- 所有DM类型的预览功能
- S1000D 4.0/4.1/4.2 标准
- 所有包含ICN图形的DM

## 测试验证

### 测试脚本
创建了自动化测试脚本 `/tmp/test_preview.sh`：
1. 登录系统
2. 调用预览接口
3. 检查HTML中是否包含 `id="idstatus"` 区域
4. 验证标题内容是否正确显示

### 测试结果
```
✅ 登录成功
✅ 获取到预览HTML，长度: 243412 字节
✅ 找到 idstatus 区域
✅ 标题内容正确！
🎉 测试通过！DM标题已正确显示在预览中
```

### 验证要点
- [x] idstatus区域正确显示
- [x] DMC代码正确显示
- [x] 发行编号/日期正确显示
- [x] DM标题正确显示
- [x] ICN图形正常加载（Base64内联）
- [x] 内容正文正常显示
- [x] 旧系统lessonPath脚本被正确清理

## 经验教训

1. **正则表达式陷阱**: `Pattern.DOTALL` + 非贪婪量词仍可能匹配过多内容
2. **调试策略**: 保存中间处理结果文件，精确定位问题步骤
3. **测试覆盖**: 需要端到端测试验证HTML完整性，而非仅检查是否包含关键字
4. **否定前瞻的价值**: `(?!pattern)` 是限制匹配范围的利器

## 相关问题

- 原问题来源: 旧系统XSLT模板生成的script标签引用了未定义的全局变量
- 历史方案: §18需求要求清理这些遗留脚本
- 本次修复: 确保清理逻辑不破坏HTML结构

## 参考资料

- Java正则表达式文档: https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
- 否定前瞻语法: `(?!pattern)`
- XSLT预览引擎文档: `docs/XSLT预览引擎设计.md`
