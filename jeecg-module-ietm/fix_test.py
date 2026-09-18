import re

# 读取文件
with open('src/test/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/DmPreviewGraphicSizeComprehensiveTest.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. 修复所有 transform 调用
content = re.sub(
    r'DmXsltTransformer\.transform\(xml\)',
    'DmXsltTransformer.transform(xml, "S1000D40", "descript")',
    content
)

# 2. 修复所有方法签名 - 添加 throws Exception
content = re.sub(
    r'public void (test\w+)\(\) \{',
    r'public void \1() throws Exception {',
    content
)

# 写回文件
with open('src/test/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/DmPreviewGraphicSizeComprehensiveTest.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("修复完成!")
