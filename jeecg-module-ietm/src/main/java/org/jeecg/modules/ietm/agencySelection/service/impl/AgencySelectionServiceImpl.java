package org.jeecg.modules.ietm.agencySelection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.agencySelection.entity.Agency;
import org.jeecg.modules.ietm.agencySelection.entity.Project;
import org.jeecg.modules.ietm.agencySelection.mapper.AgencyMapper;
import org.jeecg.modules.ietm.agencySelection.mapper.ProjectMapper;
import org.jeecg.modules.ietm.agencySelection.service.IAgencySelectionService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 机构遴选-遴选服务
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Service
@Slf4j
public class AgencySelectionServiceImpl implements IAgencySelectionService {

    @Autowired
    private AgencyMapper agencyMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    private static final String LOCK_KEY = "agencySelection:lock";
    private static final long LOCK_TIMEOUT = 30;

    private String lastVersion = null;
    private Date lastDataTimestamp = null;
    private final Set<String> previewingUsers = ConcurrentHashMap.newKeySet();

    @Override
    public synchronized String preview(List<String> projectIds, List<Map<String, Object>> eligibleOut, List<Map<String, Object>> resultsOut) {
        String user = getCurrentUser();
        if (!previewingUsers.add(user)) {
            throw new JeecgBootException("操作进行中，请稍后重试");
        }
        try {
            validatePreconditions();
            List<Agency> eligible = getEligibleList();
            List<Project> pendingProjects = getPendingProjects(projectIds);

            if (pendingProjects.isEmpty()) {
                throw new JeecgBootException("没有待分配的项目");
            }
            if (eligible.isEmpty()) {
                throw new JeecgBootException("当前无符合条件的机构，请调整机构本次摇号预设比例后重试");
            }

            BigDecimal totalWeight = BigDecimal.ZERO;
            for (Agency a : eligible) {
                if (a.getWeightCoefficient() != null) {
                    totalWeight = totalWeight.add(a.getWeightCoefficient());
                }
            }
            if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
                throw new JeecgBootException("机构权重系数异常，请检查绩效数据");
            }

            for (Agency a : eligible) {
                Map<String, Object> item = new HashMap<>();
                item.put("agencyId", a.getId());
                item.put("agencyName", a.getAgencyName());
                item.put("ratio", a.getRatio());
                item.put("utRatio", a.getUtRatio());
                item.put("weightCoefficient", a.getWeightCoefficient());
                eligibleOut.add(item);
            }

            Random random = new Random();
            for (Project project : pendingProjects) {
                double r = random.nextDouble();
                double cumulative = 0;
                Agency selected = eligible.get(eligible.size() - 1);
                for (Agency a : eligible) {
                    double prob = a.getWeightCoefficient().doubleValue() / totalWeight.doubleValue();
                    cumulative += prob;
                    if (r <= cumulative) {
                        selected = a;
                        break;
                    }
                }

                Map<String, Object> result = new HashMap<>();
                result.put("projectId", project.getId());
                result.put("projectName", project.getProjectName());
                result.put("quantity", project.getQuantity());
                result.put("agencyId", selected.getId());
                result.put("agencyName", selected.getAgencyName());
                resultsOut.add(result);
            }

            String version = String.valueOf(System.currentTimeMillis());
            lastVersion = version;
            lastDataTimestamp = agencyMapper.getMaxUpdateTime();
            return version;
        } finally {
            previewingUsers.remove(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(String version, List<Map<String, Object>> results) {
        if (version == null || lastVersion == null || !lastVersion.equals(version)) {
            throw new JeecgBootException("机构数据已变更，请重新遴选");
        }

        Date nowTimestamp = agencyMapper.getMaxUpdateTime();
        if (lastDataTimestamp != null && nowTimestamp != null && nowTimestamp.after(lastDataTimestamp)) {
            lastVersion = null;
            lastDataTimestamp = null;
            throw new JeecgBootException("机构数据已变更，请重新遴选");
        }

        RLock lock = null;
        if (redissonClient != null) {
            lock = redissonClient.getLock(LOCK_KEY);
            try {
                if (!lock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                    throw new JeecgBootException("操作进行中，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JeecgBootException("操作进行中");
            }
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String extractTime = sdf.format(new Date());
            String extractUser = getCurrentUser();
            Map<String, Integer> agencyCountMap = new HashMap<>();

            for (Map<String, Object> result : results) {
                String projectId = (String) result.get("projectId");
                String agencyId = (String) result.get("agencyId");
                String agencyName = (String) result.get("agencyName");

                Project project = projectMapper.selectById(projectId);
                if (project == null || !"待分配".equals(project.getStatus())) continue;

                projectMapper.updateSelectionResult(projectId, agencyId, agencyName, extractTime, extractUser);
                agencyCountMap.merge(agencyId, project.getQuantity() != null ? project.getQuantity() : 1, Integer::sum);
            }

            for (Map.Entry<String, Integer> entry : agencyCountMap.entrySet()) {
                agencyMapper.incrementProjectCount(entry.getKey(), entry.getValue());
            }

            Integer total = agencyMapper.countAllUndertaken();
            List<Agency> all = agencyMapper.selectList(new QueryWrapper<Agency>().eq("del_flag", 0));
            for (Agency a : all) {
                BigDecimal utRatio = BigDecimal.ZERO;
                if (total != null && total > 0 && a.getProjectCount() != null && a.getProjectCount() > 0) {
                    utRatio = new BigDecimal(a.getProjectCount())
                            .multiply(new BigDecimal("100"))
                            .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
                }
                agencyMapper.updateUtRatio(a.getId(), utRatio);
            }

            lastVersion = null;
            lastDataTimestamp = null;
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.error("遴选数据同步失败", e);
            throw new JeecgBootException("数据同步失败，请重试");
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<Map<String, Object>> getEligibleAgencies() {
        List<Agency> eligible = getEligibleList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Agency a : eligible) {
            Map<String, Object> item = new HashMap<>();
            item.put("agencyId", a.getId());
            item.put("agencyName", a.getAgencyName());
            item.put("ratio", a.getRatio());
            item.put("utRatio", a.getUtRatio());
            item.put("weightCoefficient", a.getWeightCoefficient());
            result.add(item);
        }
        return result;
    }

    @Override
    public void validatePreconditions() {
        List<Agency> all = agencyMapper.selectList(new QueryWrapper<Agency>().eq("del_flag", 0));
        if (all.isEmpty()) {
            throw new JeecgBootException("请先在机构列表中维护机构信息");
        }
        int ratioSum = all.stream().mapToInt(a -> a.getRatio() != null ? a.getRatio() : 0).sum();
        int pendingCount = projectMapper.countPending();
        if (ratioSum != 100) {
            throw new JeecgBootException("请将所有机构的本次摇号预设比例之和调整为100%且导入或新增待分配项目后再执行遴选");
        }
        if (pendingCount == 0) {
            throw new JeecgBootException("请先导入或新增项目");
        }
        List<Agency> eligible = getEligibleList();
        if (eligible.isEmpty()) {
            throw new JeecgBootException("当前无符合条件的机构，请调整机构本次摇号预设比例后重试");
        }
    }

    private List<Agency> getEligibleList() {
        List<Agency> all = agencyMapper.selectList(new QueryWrapper<Agency>().eq("del_flag", 0));
        List<Agency> eligible = new ArrayList<>();
        for (Agency a : all) {
            if (a.getRatio() == null || a.getRatio() <= 0) continue;
            BigDecimal ratio = new BigDecimal(a.getRatio());
            BigDecimal utRatio = a.getUtRatio() != null ? a.getUtRatio() : BigDecimal.ZERO;
            if (utRatio.compareTo(ratio) <= 0) {
                eligible.add(a);
            }
        }
        return eligible;
    }

    private List<Project> getPendingProjects(List<String> projectIds) {
        if (projectIds != null && !projectIds.isEmpty()) {
            List<Project> result = new ArrayList<>();
            for (String id : projectIds) {
                Project p = projectMapper.selectById(id);
                if (p != null && "待分配".equals(p.getStatus())) {
                    result.add(p);
                }
            }
            return result;
        }
        return projectMapper.selectList(
                new QueryWrapper<Project>().eq("del_flag", 0).eq("status", "待分配"));
    }

    private String getCurrentUser() {
        try {
            Object principal = org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (principal == null) return "系统";
            if (principal instanceof String) return (String) principal;
            // Try to get username via reflection
            try {
                java.lang.reflect.Method m = principal.getClass().getMethod("getUsername");
                Object result = m.invoke(principal);
                if (result != null) return result.toString();
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Method m = principal.getClass().getMethod("getRealname");
                Object result = m.invoke(principal);
                if (result != null) return result.toString();
            } catch (Exception ignored) {}
            // Fallback to toString, limit length
            String s = principal.toString();
            return s.length() > 50 ? s.substring(0, 50) : s;
        } catch (Exception e) {
            return "系统";
        }
    }
}
