package org.jeecg.modules.ietm.agencySelection.service;

import org.jeecg.modules.ietm.agencySelection.entity.Project;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Description: 机构遴选-项目表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
public interface IProjectService extends IService<Project> {

    void saveProject(Project project);

    void updateProject(Project project);

    void deleteProject(String id);

    void deleteBatchProject(List<String> ids);

    String importExcel(MultipartFile file);

    void downloadTemplate(HttpServletResponse response);
}
