import re

# 读取文件
file_path = r'D:\workspace\IETM\cape-ietm-java\jeecg-module-ietm\src\main\java\org\jeecg\modules\ietm\ietmdatamodulemanagement\util\IcnLoader.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 替换 Cache 声明
old_cache = r'private static final Cache<String, IcnCacheEntry> CACHE = Caffeine\.newBuilder\(\).*?\.build\(\);'
new_cache = 'private static final Map<String, IcnCacheEntry> CACHE = new ConcurrentHashMap<>();'
content = re.sub(old_cache, new_cache, content, flags=re.DOTALL)

# 替换 CACHE.get(key, k -> ...) 为 CACHE.computeIfAbsent(key, k -> ...)
content = re.sub(r'CACHE\.get\(', 'CACHE.computeIfAbsent(', content)

# 替换 CACHE.getIfPresent 为 CACHE.get
content = re.sub(r'CACHE\.getIfPresent\(', 'CACHE.get(', content)

# 替换 CACHE.put
# content = re.sub(r'CACHE\.put\(', 'CACHE.put(', content)  # 保持不变

# 替换 CACHE.invalidate 为 CACHE.remove
content = re.sub(r'CACHE\.invalidate\(', 'CACHE.remove(', content)

# 替换 CACHE.invalidateAll 为 CACHE.clear
content = re.sub(r'CACHE\.invalidateAll\(\)', 'CACHE.clear()', content)

# 替换 CACHE.stats() 为注释
content = re.sub(r'CacheStats stats = CACHE\.stats\(\);', '// CacheStats stats = CACHE.stats(); // Caffeine disabled', content)
content = re.sub(r'stats\.hitRate\(\)', '0.0 // stats.hitRate() disabled', content)
content = re.sub(r'stats\.evictionCount\(\)', '0L // stats.evictionCount() disabled', content)

# 写回文件
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("IcnLoader.java 修复完成!")
print("已将 Caffeine Cache 替换为 ConcurrentHashMap")
