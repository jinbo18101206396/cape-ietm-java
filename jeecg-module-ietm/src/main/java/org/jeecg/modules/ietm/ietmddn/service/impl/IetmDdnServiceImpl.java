package org.jeecg.modules.ietm.ietmddn.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.ietmddn.constants.DdnConstants;
import org.jeecg.modules.ietm.ietmddn.entity.IetmDdn;
import org.jeecg.modules.ietm.ietmddn.mapper.IetmDdnMapper;
import org.jeecg.modules.ietm.ietmddn.service.IIetmDdnService;
import org.jeecg.modules.ietm.ietmddn.util.DdnPackageBuilder;
import org.jeecg.modules.ietm.ietmddn.vo.*;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Description: DDN数据交换凭证Service实现
 * @Author: jeecg-boot
 * @Date: 2026-09-01
 */
@Slf4j
@Service
public class IetmDdnServiceImpl extends ServiceImpl<IetmDdnMapper, IetmDdn> implements IIetmDdnService {

    @Autowired
    private IetmDdnMapper ietmDdnMapper;

    @Autowired
    private DdnPackageBuilder ddnPackageBuilder;

    @Value("${accessFile.location}")
    private String fileStorageLocation;

    /**
     * 生成下一个序列号（5位补零）
     * 修复P0-3: 使用FOR UPDATE锁防止并发冲突
     * 修复DM8兼容: 两步法（先锁后查）
     * 注意：此方法必须在事务内调用
     */
    @Override
    public String generateNextSeqNumber(String year) {
        // DM8不允许聚合函数 + FOR UPDATE
        // 解决方案：先锁定当年的一条记录，再查询最大值

        // 步骤1: 尝试锁定当年的一条记录（如果存在）
        String lockedId = ietmDdnMapper.lockYearRecord(year);

        // 步骤2: 查询最大序列号（此时已持有锁，或当年无记录）
        Integer maxSeq = ietmDdnMapper.selectMaxSeqNumber(year);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;

        if (nextSeq > 99999) {
            throw new JeecgBootException("当年DDN序列号已达上限99999");
        }

        return String.format("%05d", nextSeq);
    }

    /**
     * 预留序列号并创建初始记录（修复P0-9：小事务，仅获取序列号）
     * 修复P0-3：增加重试机制，解决首次并发冲突问题
     * @return DDN记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String reserveDdnSeqNumber(DdnGenerateVO params, Map<String, Object> projectInfo) throws ParseException {
        int maxRetry = 3;
        Exception lastException = null;

        for (int i = 0; i < maxRetry; i++) {
            try {
                // 1. 获取当前用户
                LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
                String username = loginUser.getUsername();

                // 2. 提取年份
                String year = params.getIssueDate().substring(0, 4);

                // 3. 生成序列号（持有FOR UPDATE锁）
                String seqNumber = generateNextSeqNumber(year);

                // 4. 构建DDN编码
                String ddnCode = String.format("DDN-%s-%s-%s-%s-%s",
                        params.getModelic(),
                        params.getSender(),
                        params.getReceiver(),
                        year,
                        seqNumber);

                // 4.5 清理相同编码的失败记录（用户重试时避免唯一约束冲突）
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDdn> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                wrapper.eq(IetmDdn::getDdnCode, ddnCode)
                       .eq(IetmDdn::getStatus, DdnConstants.Status.GENERATING);  // 只删除"生成中"状态的失败记录
                int deleted = ietmDdnMapper.delete(wrapper);
                if (deleted > 0) {
                    log.warn("用户重试导出，清理了{}条失败的DDN记录：{}", deleted, ddnCode);
                }

                // 5. 创建初始记录（状态=生成中）
                IetmDdn ddn = new IetmDdn();
                ddn.setProjectId((String) projectInfo.get("projectId"));
                ddn.setDdnCode(ddnCode);
                ddn.setModelIdentCode(params.getModelic());
                ddn.setSenderIdent(params.getSender());
                ddn.setReceiverIdent(params.getReceiver());
                ddn.setYearOfDataIssue(year);
                ddn.setSeqNumber(seqNumber);
                ddn.setSecurity(params.getSecurity());
                ddn.setCommercialSecurity(params.getCommercialSecurity());
                ddn.setCaveat(params.getCaveat());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                ddn.setIssueDate(sdf.parse(params.getIssueDate()));

                ddn.setDmIds(String.join(",", params.getDmIds()));
                ddn.setIncludeRefIcn(params.getIncludeRefIcn() ? "1" : "0");
                ddn.setIncludeRefDm(params.getIncludeRefDm() ? "1" : "0");
                ddn.setIncludeDmResource(params.getIncludeDmResource() ? "1" : "0");
                ddn.setStatus(DdnConstants.Status.GENERATING);  // 生成中
                this.save(ddn);

                log.info("用户[]预留DDN序列号：{}", username, ddnCode);
                return ddn.getId();

            } catch (org.springframework.dao.DuplicateKeyException e) {
                lastException = e;
                if (i < maxRetry - 1) {
                    log.warn("DDN序列号冲突，第{}次重试", i + 1);
                    try {
                        Thread.sleep(100 * (long) Math.pow(2, i)); // 指数退避：100ms, 200ms, 400ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new JeecgBootException("DDN生成被中断");
                    }
                }
            }
        }

        throw new JeecgBootException("DDN序列号冲突，已重试" + maxRetry + "次，请稍后再试", lastException);
    }

    /**
     * 更新DDN记录为成功状态（修复P0-9：小事务，仅更新状态；修复P0-10：保存完整的dmIds和icnIds）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDdnSuccess(String ddnId, DdnPackageBuilder.BuildResult buildResult) {
        IetmDdn ddn = this.getById(ddnId);
        if (ddn != null) {
            ddn.setDdnFilePath(buildResult.getZipFilePath());
            ddn.setDmCount(buildResult.getTotalDmCount());
            // 修复P1-5：保存ICN数量
            ddn.setIcnCount(buildResult.getTotalIcnCount());
            // 修复P0-10：保存完整的DM和ICN列表（包含递归引用的）
            if (buildResult.getAllDmIds() != null && !buildResult.getAllDmIds().isEmpty()) {
                ddn.setDmIds(String.join(",", buildResult.getAllDmIds()));
            }
            ddn.setStatus(DdnConstants.Status.SUCCESS);  // 生成成功
            this.updateById(ddn);
            log.info("DDN[{}]生成成功，DM数量：{}，ICN数量：{}",
                    ddn.getDdnCode(), buildResult.getTotalDmCount(), buildResult.getTotalIcnCount());
        }
    }

    @Override
    public DdnGenerateResultVO generateDdn(DdnGenerateVO params, Map<String, Object> projectInfo) throws Exception {
        // 1. 参数校验
        if (params.getDmIds() == null || params.getDmIds().isEmpty()) {
            throw new JeecgBootException("DM列表为空，无法导出");
        }
        if (projectInfo == null || projectInfo.get("projectId") == null) {
            throw new JeecgBootException("未打开项目，请先打开项目后再导出");
        }

        // 2. 事务1：预留序列号并创建初始记录（小事务，快速释放锁）
        String ddnId;
        try {
            ddnId = reserveDdnSeqNumber(params, projectInfo);
        } catch (ParseException e) {
            log.error("日期格式错误：{}", params.getIssueDate(), e);
            throw new JeecgBootException("日期格式错误，请使用yyyy-MM-dd格式");
        }

        IetmDdn ddn = this.getById(ddnId);
        String ddnCode = ddn.getDdnCode();

        // 3. 无事务：执行文件操作（不持有数据库锁）
        DdnPackageBuilder.BuildResult buildResult;
        try {
            buildResult = ddnPackageBuilder.buildDdnPackage(ddnCode, params, projectInfo);
        } catch (Exception e) {
            log.error("DDN数据包生成失败：{}", ddnCode, e);
            // 文件操作失败，标记记录为失败状态
            this.lambdaUpdate()
                    .eq(IetmDdn::getId, ddnId)
                    .set(IetmDdn::getStatus, DdnConstants.Status.FAILED)  // 生成失败
                    .update();
            throw new JeecgBootException("生成DDN数据包失败：" + e.getMessage());
        }

        // 4. 事务2：更新记录为成功状态（小事务）
        updateDdnSuccess(ddnId, buildResult);

        // 5. 返回结果（修复P1-6：包含错误DM提示）
        DdnGenerateResultVO result = new DdnGenerateResultVO();
        result.setDdnCode(ddnCode);
        result.setFileName(ddnCode + ".zip");
        result.setDownloadUrl("/ietm/ddn/download?ddnCode=" + ddnCode);
        result.setDmCount(buildResult.getTotalDmCount());
        result.setIcnCount(buildResult.getTotalIcnCount());

        // 如果有错误DM，添加到结果中
        if (buildResult.getErrorDmList() != null && !buildResult.getErrorDmList().isEmpty()) {
            result.setErrorDmList(buildResult.getErrorDmList());
            log.warn("DDN[{}]包含{}个无法导出的DM：{}", ddnCode, buildResult.getErrorDmList().size(), buildResult.getErrorDmList());
        }

        log.info("DDN[{}]生成完成，DM数量：{}，ICN数量：{}", ddnCode, buildResult.getTotalDmCount(), buildResult.getTotalIcnCount());
        return result;
    }

    /**
     * 生成ICN专用DDN数据包
     * 修复P0-1和P0-2：ICN文件在根目录，使用infoEntityIdent结构
     */
    @Override
    public DdnGenerateResultVO generateIcnDdn(IcnExportVO params, Map<String, Object> projectInfo) throws Exception {
        // 1. 参数校验
        if (params.getIcnIds() == null || params.getIcnIds().isEmpty()) {
            throw new JeecgBootException("ICN列表为空，无法导出");
        }
        if (projectInfo == null || projectInfo.get("projectId") == null) {
            throw new JeecgBootException("未打开项目，请先打开项目后再导出");
        }

        // 2. 构建DdnGenerateVO（复用DDN参数结构）
        DdnGenerateVO ddnParams = new DdnGenerateVO();
        ddnParams.setModelic(params.getModelic());
        ddnParams.setSecurity(params.getSecurity());
        ddnParams.setCommercialSecurity(params.getCommercialSecurity());
        ddnParams.setCaveat(params.getCaveat());
        ddnParams.setSender(params.getSender());
        ddnParams.setReceiver(params.getReceiver());
        ddnParams.setIssueDate(params.getIssueDate());
        ddnParams.setDmIds(Collections.emptyList());  // ICN导出无DM
        ddnParams.setIncludeRefDm(false);
        ddnParams.setIncludeRefIcn(false);
        ddnParams.setIncludeDmResource(false);

        // 3. 事务1：预留序列号并创建初始记录
        String ddnId;
        try {
            ddnId = reserveIcnDdnSeqNumber(params, projectInfo);
        } catch (ParseException e) {
            log.error("日期格式错误：{}", params.getIssueDate(), e);
            throw new JeecgBootException("日期格式错误，请使用yyyy-MM-dd格式");
        }

        IetmDdn ddn = this.getById(ddnId);
        String ddnCode = ddn.getDdnCode();

        // 4. 无事务：执行文件操作
        DdnPackageBuilder.BuildResult buildResult;
        try {
            buildResult = ddnPackageBuilder.buildIcnOnlyPackage(ddnCode, params.getIcnIds(), ddnParams, projectInfo);
        } catch (Exception e) {
            log.error("ICN DDN数据包生成失败：{}", ddnCode, e);
            // 标记为失败状态
            this.lambdaUpdate()
                    .eq(IetmDdn::getId, ddnId)
                    .set(IetmDdn::getStatus, DdnConstants.Status.FAILED)
                    .update();
            throw new JeecgBootException("生成ICN DDN数据包失败：" + e.getMessage());
        }

        // 5. 事务2：更新记录为成功状态
        updateDdnSuccess(ddnId, buildResult);

        // 6. 返回结果
        DdnGenerateResultVO result = new DdnGenerateResultVO();
        result.setDdnCode(ddnCode);
        result.setFileName(ddnCode + ".zip");
        result.setDownloadUrl("/ietm/ddn/download?ddnCode=" + ddnCode);
        result.setDmCount(0);
        result.setIcnCount(buildResult.getTotalIcnCount());

        // 缺失ICN提示
        if (buildResult.getMissingIcnList() != null && !buildResult.getMissingIcnList().isEmpty()) {
            result.setErrorDmList(buildResult.getMissingIcnList());  // 复用errorDmList字段
            log.warn("ICN DDN[{}]包含{}个缺失的ICN：{}", ddnCode, buildResult.getMissingIcnList().size(), buildResult.getMissingIcnList());
        }

        log.info("ICN DDN[{}]生成完成，ICN数量：{}", ddnCode, buildResult.getTotalIcnCount());
        return result;
    }

    /**
     * 预留ICN DDN序列号并创建初始记录
     */
    @Transactional(rollbackFor = Exception.class)
    public String reserveIcnDdnSeqNumber(IcnExportVO params, Map<String, Object> projectInfo) throws ParseException {
        int maxRetry = 3;
        Exception lastException = null;

        for (int i = 0; i < maxRetry; i++) {
            try {
                // 1. 获取当前用户
                LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
                String username = loginUser.getUsername();

                // 2. 提取年份
                String year = params.getIssueDate().substring(0, 4);

                // 3. 生成序列号
                String seqNumber = generateNextSeqNumber(year);

                // 4. 构建DDN编码
                String ddnCode = String.format("DDN-%s-%s-%s-%s-%s",
                        params.getModelic(),
                        params.getSender(),
                        params.getReceiver(),
                        year,
                        seqNumber);

                // 4.5 清理相同编码的失败记录
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IetmDdn> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                wrapper.eq(IetmDdn::getDdnCode, ddnCode)
                       .eq(IetmDdn::getStatus, DdnConstants.Status.GENERATING);
                int deleted = ietmDdnMapper.delete(wrapper);
                if (deleted > 0) {
                    log.warn("用户重试ICN导出，清理了{}条失败的DDN记录：{}", deleted, ddnCode);
                }

                // 5. 创建初始记录
                IetmDdn ddn = new IetmDdn();
                ddn.setProjectId((String) projectInfo.get("projectId"));
                ddn.setDdnCode(ddnCode);
                ddn.setModelIdentCode(params.getModelic());
                ddn.setSenderIdent(params.getSender());
                ddn.setReceiverIdent(params.getReceiver());
                ddn.setYearOfDataIssue(year);
                ddn.setSeqNumber(seqNumber);
                ddn.setSecurity(params.getSecurity());
                ddn.setCommercialSecurity(params.getCommercialSecurity());
                ddn.setCaveat(params.getCaveat());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                ddn.setIssueDate(sdf.parse(params.getIssueDate()));

                // 注意：ietm_ddn表中无ddn_type字段，通过dm_ids为空来区分ICN导出
                ddn.setDmIds("");  // ICN导出无DM（通过此字段区分）
                ddn.setIncludeRefIcn("0");
                ddn.setIncludeRefDm("0");
                ddn.setIncludeDmResource("0");
                ddn.setStatus(DdnConstants.Status.GENERATING);
                this.save(ddn);

                log.info("用户{}预留ICN DDN序列号：{}", username, ddnCode);
                return ddn.getId();

            } catch (org.springframework.dao.DuplicateKeyException e) {
                lastException = e;
                if (i < maxRetry - 1) {
                    log.warn("ICN DDN序列号冲突，第{}次重试", i + 1);
                    try {
                        Thread.sleep(100 * (long) Math.pow(2, i));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new JeecgBootException("ICN DDN生成被中断");
                    }
                }
            }
        }

        throw new JeecgBootException("ICN DDN序列号冲突，已重试" + maxRetry + "次，请稍后再试", lastException);
    }

    @Override
    public void downloadDdnPackage(String ddnCode, HttpServletResponse response) throws Exception {
        // 1. 查询 DDN 记录（修复P0-2：恢复状态校验）
        IetmDdn ddn = this.lambdaQuery()
                .eq(IetmDdn::getDdnCode, ddnCode)
                .eq(IetmDdn::getStatus, DdnConstants.Status.SUCCESS)  // 仅允许下载生成成功的DDN
                .one();

        if (ddn == null) {
            throw new JeecgBootException("DDN数据包不存在、未生成完成或已删除");
        }

        // 2. 构建 ZIP 文件路径
        File zipFile = new File(fileStorageLocation, ddn.getDdnFilePath());
        if (!zipFile.exists()) {
            throw new JeecgBootException("DDN数据包文件已丢失");
        }

        // 3. 设置响应头（防止文件名注入攻击）
        response.setContentType("application/zip");
        String safeFileName = ddnCode.replaceAll("[^a-zA-Z0-9-_]", "_") + ".zip";
        response.setHeader("Content-Disposition",
                "attachment; filename=" + java.net.URLEncoder.encode(safeFileName, "UTF-8"));

        // 4. 输出文件流
        try (InputStream is = new FileInputStream(zipFile);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        }

        log.info("DDN[{}]下载成功", ddnCode);
    }
}
