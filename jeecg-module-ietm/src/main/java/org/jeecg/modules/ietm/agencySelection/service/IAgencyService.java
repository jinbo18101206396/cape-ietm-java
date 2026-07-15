package org.jeecg.modules.ietm.agencySelection.service;

import org.jeecg.modules.ietm.agencySelection.entity.Agency;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @Description: 机构遴选-机构表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
public interface IAgencyService extends IService<Agency> {

    void saveAgency(Agency agency);

    void updateAgency(Agency agency);

    void deleteAgency(String id);

    void deleteBatchAgency(List<String> ids);

    void resetAllRatios();

    void recalculateAllRatios();

    Map<String, Object> getStatistics();

    void calculatePerformance(Agency agency);

    int getAssignedProjectCount(String agencyId);

    String importExcel(MultipartFile file);

    void downloadTemplate(HttpServletResponse response);
}
