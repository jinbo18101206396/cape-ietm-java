package org.jeecg.modules.ietm.agencySelection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.agencySelection.entity.Agency;
import org.jeecg.modules.ietm.agencySelection.entity.AgencyFile;
import org.jeecg.modules.ietm.agencySelection.entity.Project;
import org.jeecg.modules.ietm.agencySelection.mapper.AgencyFileMapper;
import org.jeecg.modules.ietm.agencySelection.mapper.AgencyMapper;
import org.jeecg.modules.ietm.agencySelection.mapper.ProjectMapper;
import org.jeecg.modules.ietm.agencySelection.service.IProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 机构遴选-项目表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Service
@Slf4j
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements IProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private AgencyMapper agencyMapper;

    @Autowired
    private AgencyFileMapper agencyFileMapper;

    private static final String FILES_BASE = System.getProperty("user.dir") + "/src/main/resources/files/project/";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProject(Project project) {
        doSaveProject(project);
    }

    private void doSaveProject(Project project) {
        project.setId(null); // 强制MyBatis-Plus生成新ID，避免主键冲突
        if (project.getQuantity() == null || project.getQuantity() < 1) {
            throw new JeecgBootException("项目数量必须为大于0的整数");
        }
        project.setStatus("待分配");
        projectMapper.insert(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(Project project) {
        if (project.getQuantity() == null || project.getQuantity() < 1) {
            throw new JeecgBootException("项目数量必须为大于0的整数");
        }
        Project old = projectMapper.selectById(project.getId());
        if (old == null) throw new JeecgBootException("项目不存在");

        project.setStatus(old.getStatus());
        project.setAgencyId(old.getAgencyId());
        project.setAgencyName(old.getAgencyName());
        project.setExtractTime(old.getExtractTime());
        project.setExtractUser(old.getExtractUser());
        projectMapper.updateById(project);

        if ("已分配".equals(old.getStatus()) && !old.getQuantity().equals(project.getQuantity())) {
            recalculateAgencyUndertaken(old.getAgencyId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(String id) {
        Project project = projectMapper.selectById(id);
        if (project == null) return;

        String agencyId = project.getAgencyId();
        boolean wasAssigned = "已分配".equals(project.getStatus());

        agencyFileMapper.deleteByBizId(id, "project");
        String fileDir = FILES_BASE + id;
        File dir = new File(fileDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) for (File f : files) f.delete();
            dir.delete();
        }

        projectMapper.deleteById(id);

        if (wasAssigned && agencyId != null) {
            recalculateAgencyUndertaken(agencyId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchProject(List<String> ids) {
        for (String id : ids) {
            deleteProject(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importExcel(MultipartFile file) {
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int totalRows = sheet.getLastRowNum();
            if (totalRows < 1) {
                throw new JeecgBootException("Excel 文件无数据行，请使用下载的模板填写数据后导入");
            }
            validateProjectHeader(sheet.getRow(0));

            StringBuilder errors = new StringBuilder();
            List<Project> toSave = new ArrayList<>();

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = getCellString(row, 0);
                Integer quantity = getCellInt(row, 1);

                if (name == null || name.trim().isEmpty()) {
                    errors.append("第 ").append(i + 1).append(" 行：项目名称必填；");
                    continue;
                }
                if (quantity == null || quantity < 1) {
                    errors.append("第 ").append(i + 1).append(" 行：项目数量格式错误或缺失；");
                    continue;
                }

                QueryWrapper<Project> qw = new QueryWrapper<>();
                qw.eq("project_name", name.trim());
                qw.eq("del_flag", 0);
                if (projectMapper.selectCount(qw) > 0) {
                    errors.append("第 ").append(i + 1).append(" 行：项目名称重复；");
                    continue;
                }

                Project project = new Project();
                project.setProjectName(name.trim());
                project.setQuantity(quantity);
                toSave.add(project);
            }

            if (errors.length() > 0) {
                throw new JeecgBootException(errors.toString());
            }
            if (toSave.isEmpty()) {
                throw new JeecgBootException("未导入任何有效数据");
            }

            for (Project project : toSave) {
                doSaveProject(project);
            }
            return "导入成功，共 " + toSave.size() + " 条记录";
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new JeecgBootException("导入失败：" + e.getMessage());
        }
    }

    private void validateProjectHeader(Row header) {
        if (header == null) throw new JeecgBootException("Excel 缺少表头行，请使用下载的模板");
        String[] expected = {"项目名称", "项目数量"};
        for (int i = 0; i < expected.length; i++) {
            String cell = getCellString(header, i);
            if (cell == null || !cell.trim().equals(expected[i])) {
                throw new JeecgBootException("Excel 表头第 " + (i + 1) + " 列应为「" + expected[i] + "」，当前为「" + (cell != null ? cell.trim() : "空") + "」。请使用下载的模板文件");
            }
        }
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("项目列表");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("项目名称");
            header.createCell(1).setCellValue("项目数量");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("项目导入模板.xlsx", "UTF-8"));
            ServletOutputStream out = response.getOutputStream();
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            log.error("下载模板失败", e);
        }
    }

    private void recalculateAgencyUndertaken(String agencyId) {
        if (agencyId == null) return;
        Integer count = projectMapper.countUndertakenByAgency(agencyId);
        Integer total = projectMapper.countAllUndertaken();

        Agency agency = agencyMapper.selectById(agencyId);
        if (agency != null) {
            agency.setProjectCount(count);
            BigDecimal utRatio = BigDecimal.ZERO;
            if (total != null && total > 0 && count != null && count > 0) {
                utRatio = new BigDecimal(count)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(total), 2, java.math.RoundingMode.HALF_UP);
            }
            agency.setUtRatio(utRatio);
            agencyMapper.updateById(agency);

            List<Agency> all = agencyMapper.selectList(new QueryWrapper<Agency>().eq("del_flag", 0));
            for (Agency a : all) {
                if (!a.getId().equals(agencyId)) {
                    BigDecimal otherRatio = BigDecimal.ZERO;
                    if (total != null && total > 0 && a.getProjectCount() != null && a.getProjectCount() > 0) {
                        otherRatio = new BigDecimal(a.getProjectCount())
                                .multiply(new BigDecimal("100"))
                                .divide(new BigDecimal(total), 2, java.math.RoundingMode.HALF_UP);
                    }
                    agencyMapper.updateUtRatio(a.getId(), otherRatio);
                }
            }
        }
    }

    private String getCellString(Row row, int col) {
        if (row.getCell(col) == null) return null;
        try {
            return row.getCell(col).getStringCellValue();
        } catch (Exception e) {
            double v = row.getCell(col).getNumericCellValue();
            return String.valueOf((int) v);
        }
    }

    private Integer getCellInt(Row row, int col) {
        if (row.getCell(col) == null) return null;
        try {
            return (int) row.getCell(col).getNumericCellValue();
        } catch (Exception e) {
            try {
                return Integer.parseInt(row.getCell(col).getStringCellValue().trim());
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
