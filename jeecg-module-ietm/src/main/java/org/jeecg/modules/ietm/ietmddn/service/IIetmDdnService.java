package org.jeecg.modules.ietm.ietmddn.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.ietmddn.entity.IetmDdn;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateResultVO;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateVO;
import org.jeecg.modules.ietm.ietmddn.vo.IcnExportVO;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @Description: DDN数据交换凭证Service接口
 * @Author: jeecg-boot
 * @Date: 2026-09-01
 */
public interface IIetmDdnService extends IService<IetmDdn> {

    /**
     * 生成DDN数据包（同步）
     * @param params 请求参数
     * @param projectInfo 当前项目信息（从Redis获取）
     * @return 生成结果（DDN编码、下载路径、统计信息）
     */
    DdnGenerateResultVO generateDdn(DdnGenerateVO params, Map<String, Object> projectInfo) throws Exception;

    /**
     * 生成ICN专用DDN数据包（导出实体功能）
     * @param params ICN导出请求参数
     * @param projectInfo 当前项目信息（从Redis获取）
     * @return 生成结果（DDN编码、下载路径、统计信息）
     */
    DdnGenerateResultVO generateIcnDdn(IcnExportVO params, Map<String, Object> projectInfo) throws Exception;

    /**
     * 生成下一个序列号（当年最大+1）
     * @param year 年份（4位字符串）
     * @return 5位序列号字符串（补零）
     */
    String generateNextSeqNumber(String year);

    /**
     * 下载DDN数据包ZIP
     * @param ddnCode DDN编码
     * @param response HTTP响应
     */
    void downloadDdnPackage(String ddnCode, HttpServletResponse response) throws Exception;
}
