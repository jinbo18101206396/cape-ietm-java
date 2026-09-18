# IETM P1-1修复方案：引入Spring Cache

**问题编号**: P1-1  
**问题描述**: 缺少查询缓存导致性能瓶颈  
**严重程度**: P1（严重）  
**预期收益**: 数据库负载-60%，支持100并发  
**工作量**: 3人日  
**状态**: 🟡 规划中

---

## 📊 问题分析

### 当前状况

**性能瓶颈**:
- DM元数据每次预览都查库
- 用户权限每次校验都查库
- 字典数据（dm_type、标准代码）无缓存

**影响**:
- 100并发时数据库CPU 90%+
- P95响应时间 > 2秒（应 < 500ms）
- 数据库成为系统瓶颈

### 缓存场景分析

| 数据类型 | 查询频率 | 更新频率 | 推荐TTL | 缓存策略 |
|---------|---------|---------|---------|---------|
| DM元数据 | 高（每次预览） | 低（编辑时） | 5分钟 | 本地缓存 |
| 用户权限 | 高（每次操作） | 低（权限变更） | 1分钟 | 本地缓存 |
| 字典数据 | 高（下拉框） | 极低（运维配置） | 30分钟 | 本地缓存 |
| 工作流状态 | 中 | 中 | 30秒 | 本地缓存 |

**结论**: 使用Caffeine作为本地缓存（已有依赖），无需引入Redis

---

## 🎯 修复方案

### 方案选择

**方案A**: Caffeine本地缓存（推荐）
- ✅ 已有依赖（P0-1已引入）
- ✅ 高性能（读取<1ms）
- ✅ 无需额外部署
- ✅ 适合读多写少场景
- ⚠️ 多实例不共享（可接受）

**方案B**: Redis分布式缓存
- ✅ 多实例共享
- ⚠️ 需要部署Redis
- ⚠️ 网络开销（读取5-10ms）
- ⚠️ 复杂度增加

**决策**: 选择方案A（Caffeine本地缓存）

---

## 🛠️ 实施步骤

### 步骤1: 启用Spring Cache（0.5人日）

#### 1.1 添加Spring Cache依赖

**文件**: `jeecg-boot-base-core/pom.xml`

```xml
<!-- Spring Cache抽象 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Caffeine缓存实现（已添加） -->
<dependency>
    <groupId>com.github.benmanes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>2.9.3</version>
</dependency>
```

#### 1.2 创建缓存配置类

**文件**: `jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/config/CacheConfig.java`

```java
package org.jeecg.modules.ietm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring Cache配置
 * 
 * 使用Caffeine作为本地缓存实现，适合读多写少的场景
 * 
 * @author IETM Team
 * @date 2026-09-18
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 默认缓存配置（5分钟过期）
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        
        return cacheManager;
    }

    /**
     * DM元数据缓存（5分钟TTL）
     */
    @Bean
    public Caffeine<Object, Object> dmMetadataCache() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats();
    }

    /**
     * 用户权限缓存（1分钟TTL）
     */
    @Bean
    public Caffeine<Object, Object> userPermissionCache() {
        return Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .recordStats();
    }

    /**
     * 字典数据缓存（30分钟TTL）
     */
    @Bean
    public Caffeine<Object, Object> dictDataCache() {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats();
    }
}
```

---

### 步骤2: 缓存DM元数据（1人日）

#### 2.1 添加缓存注解

**文件**: `IetmDataModuleServiceImpl.java`

```java
/**
 * 根据ID查询DM（带缓存）
 */
@Cacheable(value = "dm:metadata", key = "#id", unless = "#result == null")
@Override
public IetmDataModule queryById(String id) {
    return super.getById(id);
}

/**
 * 更新DM（清除缓存）
 */
@CacheEvict(value = "dm:metadata", key = "#entity.id")
@Override
public boolean updateById(IetmDataModule entity) {
    return super.updateById(entity);
}

/**
 * 删除DM（清除缓存）
 */
@CacheEvict(value = "dm:metadata", key = "#id")
@Override
public boolean removeById(Serializable id) {
    return super.removeById(id);
}

/**
 * 签入时清除缓存
 */
@CacheEvict(value = "dm:metadata", key = "#dmId")
public void checkIn(String dmId, String content) {
    // ... 签入逻辑
}
```

#### 2.2 批量查询缓存

```java
/**
 * 批量查询DM（带缓存）
 */
public List<IetmDataModule> queryByIds(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
        return Collections.emptyList();
    }
    
    List<IetmDataModule> result = new ArrayList<>();
    for (String id : ids) {
        // 逐个查询，自动触发缓存
        IetmDataModule dm = queryById(id);
        if (dm != null) {
            result.add(dm);
        }
    }
    return result;
}
```

---

### 步骤3: 缓存用户权限（0.5人日）

**文件**: `WorkflowPermissionChecker.java`

```java
/**
 * 获取登录用户（带缓存）
 */
@Cacheable(value = "user:permission", key = "#username", unless = "#result == null")
public LoginUser getLoginUser(String username) {
    try {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    } catch (Exception e) {
        log.error("获取登录用户失败: username={}", username, e);
        return null;
    }
}

/**
 * 检查用户权限（带缓存）
 */
@Cacheable(value = "user:permission", key = "'hasRole:' + #username + ':' + #roleCode")
public boolean hasRole(String username, String roleCode) {
    LoginUser user = getLoginUser(username);
    if (user == null) {
        return false;
    }
    
    List<String> roles = user.getRoles();
    return roles != null && roles.contains(roleCode);
}

/**
 * 用户登出或权限变更时清除缓存
 */
@CacheEvict(value = "user:permission", key = "#username")
public void clearUserCache(String username) {
    log.info("清除用户缓存: username={}", username);
}

/**
 * 清除所有用户缓存
 */
@CacheEvict(value = "user:permission", allEntries = true)
public void clearAllUserCache() {
    log.info("清除所有用户缓存");
}
```

---

### 步骤4: 缓存字典数据（0.5人日）

**文件**: `IetmDmTypeServiceImpl.java`

```java
/**
 * 查询所有DM类型（带缓存）
 */
@Cacheable(value = "dict:dmType", unless = "#result.isEmpty()")
@Override
public List<IetmDmType> list() {
    return super.list();
}

/**
 * 根据code查询DM类型（带缓存）
 */
@Cacheable(value = "dict:dmType", key = "#code", unless = "#result == null")
public IetmDmType getByCode(String code) {
    QueryWrapper<IetmDmType> qw = new QueryWrapper<>();
    qw.eq("dm_type_code", code);
    return getOne(qw);
}

/**
 * 更新字典时清除缓存
 */
@CacheEvict(value = "dict:dmType", allEntries = true)
@Override
public boolean updateById(IetmDmType entity) {
    return super.updateById(entity);
}
```

---

### 步骤5: 监控与调优（0.5人日）

#### 5.1 暴露缓存统计接口

**文件**: `CacheController.java`

```java
package org.jeecg.modules.ietm.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存监控接口
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Resource
    private CacheManager cacheManager;

    /**
     * 获取所有缓存统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> result = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
            if (springCache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) springCache;
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();
                
                Map<String, Object> cacheStats = new HashMap<>();
                cacheStats.put("size", nativeCache.estimatedSize());
                cacheStats.put("hitRate", stats.hitRate());
                cacheStats.put("hitCount", stats.hitCount());
                cacheStats.put("missCount", stats.missCount());
                cacheStats.put("evictionCount", stats.evictionCount());
                
                result.put(cacheName, cacheStats);
            }
        });
        
        return result;
    }

    /**
     * 清空指定缓存
     */
    @PostMapping("/clear")
    public String clearCache(@RequestParam String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return "缓存已清空: " + cacheName;
        }
        return "缓存不存在: " + cacheName;
    }

    /**
     * 清空所有缓存
     */
    @PostMapping("/clearAll")
    public String clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        return "所有缓存已清空";
    }
}
```

---

## 📊 预期效果

### 性能提升

| 指标 | 修复前 | 修复后 | 提升 |
|------|--------|--------|------|
| DM查询延迟 | 50ms（DB） | <1ms（缓存） | +98% |
| 权限校验延迟 | 20ms（DB） | <1ms（缓存） | +95% |
| 数据库QPS | 1000 | 400 | -60% |
| 数据库CPU | 90% | 30% | -67% |
| 系统吞吐量 | 800 QPS | 2000 QPS | +150% |

### 缓存命中率预期

| 缓存类型 | 预期命中率 | 说明 |
|---------|-----------|------|
| DM元数据 | 80%+ | 热门DM重复预览 |
| 用户权限 | 90%+ | 同一用户重复操作 |
| 字典数据 | 95%+ | 几乎不变 |

---

## ✅ 测试验证

### 单元测试

**文件**: `CacheConfigTest.java`

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class CacheConfigTest {

    @Resource
    private IetmDataModuleService dmService;
    
    @Resource
    private CacheManager cacheManager;

    @Test
    public void testDmCacheHit() {
        String dmId = "test-dm-001";
        
        // 第一次查询（缓存未命中）
        long start1 = System.currentTimeMillis();
        IetmDataModule dm1 = dmService.queryById(dmId);
        long time1 = System.currentTimeMillis() - start1;
        
        // 第二次查询（缓存命中）
        long start2 = System.currentTimeMillis();
        IetmDataModule dm2 = dmService.queryById(dmId);
        long time2 = System.currentTimeMillis() - start2;
        
        // 验证缓存生效
        assertTrue("第二次查询应该更快", time2 < time1 / 10);
        assertEquals("两次查询结果一致", dm1.getId(), dm2.getId());
    }

    @Test
    public void testCacheEviction() {
        String dmId = "test-dm-002";
        
        // 查询后缓存
        IetmDataModule dm1 = dmService.queryById(dmId);
        
        // 更新（自动清除缓存）
        dm1.setDmName("Updated Name");
        dmService.updateById(dm1);
        
        // 验证缓存已清除
        Cache cache = cacheManager.getCache("dm:metadata");
        assertNull("缓存应该被清除", cache.get(dmId));
    }
}
```

### 压力测试

**JMeter测试计划**:

```
线程组: 100并发
持续时间: 5分钟
操作: DM预览（混合热门和冷门DM）

监控指标:
- 数据库QPS（期望 < 500）
- 数据库CPU（期望 < 40%）
- API响应时间 P95（期望 < 500ms）
- 缓存命中率（期望 > 80%）
```

---

## ⚠️ 注意事项

### 1. 缓存一致性

**问题**: 多实例部署时，一个实例更新数据，其他实例缓存未失效

**解决方案**:
- 短TTL（1-5分钟）保证最终一致性
- 关键操作主动通知其他实例清除缓存（消息队列）
- 或升级为Redis分布式缓存

### 2. 缓存穿透

**问题**: 查询不存在的数据，缓存无法命中，直接打到数据库

**解决方案**:
```java
@Cacheable(value = "dm:metadata", key = "#id", 
           unless = "#result == null")  // null值不缓存
```

或者使用布隆过滤器

### 3. 缓存雪崩

**问题**: 大量缓存同时过期，瞬间流量打到数据库

**解决方案**:
```java
// 添加随机过期时间
.expireAfterWrite(5 + ThreadLocalRandom.current().nextInt(60), TimeUnit.SECONDS)
```

### 4. 内存占用

**监控**: 定期检查Caffeine统计

**限制**:
- DM元数据：最多500项
- 用户权限：最多200项
- 字典数据：最多100项

**总内存**: 约50MB（可接受）

---

## 📝 部署清单

### 代码变更

- [ ] `jeecg-boot-base-core/pom.xml` - 添加spring-boot-starter-cache
- [ ] `CacheConfig.java` - 创建缓存配置类
- [ ] `IetmDataModuleServiceImpl.java` - 添加@Cacheable/@CacheEvict
- [ ] `WorkflowPermissionChecker.java` - 添加@Cacheable/@CacheEvict
- [ ] `IetmDmTypeServiceImpl.java` - 添加@Cacheable/@CacheEvict
- [ ] `CacheController.java` - 创建缓存监控接口
- [ ] `CacheConfigTest.java` - 创建单元测试

### 配置变更

无需配置变更（使用代码配置）

### 测试验证

- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 压力测试通过（100并发）
- [ ] 缓存命中率 > 80%
- [ ] 数据库负载降低 > 50%

### 回滚方案

1. 注释`@EnableCaching`注解
2. 重启服务
3. 系统恢复到无缓存状态

---

## 🎯 里程碑

| 里程碑 | 完成标准 | 预计时间 |
|--------|---------|---------|
| M1: 配置完成 | CacheConfig类创建 | 0.5人日 |
| M2: DM缓存完成 | @Cacheable注解添加 | 1人日 |
| M3: 权限/字典缓存完成 | 全部注解添加 | 1人日 |
| M4: 测试验证完成 | 压力测试通过 | 0.5人日 |

**总计**: 3人日

---

## 📞 后续工作

完成P1-1后，继续：
1. P1-2: 优化事务边界（2人日）
2. P1-3: 异常监控埋点（1人日）
3. P1-4: XSLT缓存驱逐（1人日）

**P1总计**: 7人日

**预期提升**: 代码质量 4.3 → 4.5

---

**文档创建时间**: 2026-09-18  
**状态**: 方案已完成，待实施  
**优先级**: P1（高）
