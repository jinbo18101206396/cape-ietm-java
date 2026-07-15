package org.jeecg.modules.ietm.agencySelection.service;

import org.jeecg.modules.ietm.agencySelection.entity.Agency;

import java.util.List;
import java.util.Map;

/**
 * @Description: 机构遴选-遴选服务
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
public interface IAgencySelectionService {

    String preview(List<String> projectIds, List<Map<String, Object>> eligibleAgencies, List<Map<String, Object>> results);

    void confirm(String version, List<Map<String, Object>> results);

    List<Map<String, Object>> getEligibleAgencies();

    void validatePreconditions();
}
