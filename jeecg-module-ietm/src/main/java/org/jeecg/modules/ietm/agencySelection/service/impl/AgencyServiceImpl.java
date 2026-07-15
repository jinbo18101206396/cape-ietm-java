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
import org.jeecg.modules.ietm.agencySelection.service.IAgencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 机构遴选-机构表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Service
@Slf4j
public class AgencyServiceImpl extends ServiceImpl<AgencyMapper, Agency> implements IAgencyService {

    @Autowired
    private AgencyMapper agencyMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private AgencyFileMapper agencyFileMapper;

    private static final String FILES_BASE = System.getProperty("user.dir") + "/src/main/resources/files/agency/";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAgency(Agency agency) {
        doSaveAgency(agency);
    }

    private void doSaveAgency(Agency agency) {
        agency.setId(null); // 强制MyBatis-Plus生成新ID，避免主键冲突
        QueryWrapper<Agency> qw = new QueryWrapper<>();
        qw.eq("agency_name", agency.getAgencyName());
        qw.eq("del_flag", 0);
        if (agencyMapper.selectCount(qw) > 0) {
            throw new JeecgBootException("机构名称已存在");
        }
        calculatePerformance(agency);
        agency.setUtRatio(BigDecimal.ZERO);
        agency.setProjectCount(0);
        agencyMapper.insert(agency);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgency(Agency agency) {
        Agency old = agencyMapper.selectById(agency.getId());
        if (old == null) {
            throw new JeecgBootException("机构不存在");
        }
        QueryWrapper<Agency> qw = new QueryWrapper<>();
        qw.eq("agency_name", agency.getAgencyName());
        qw.eq("del_flag", 0);
        qw.ne("id", agency.getId());
        if (agencyMapper.selectCount(qw) > 0) {
            throw new JeecgBootException("机构名称已存在");
        }
        calculatePerformance(agency);
        agency.setProjectCount(old.getProjectCount());
        agency.setUtRatio(old.getUtRatio());
        agencyMapper.updateById(agency);
        if (!old.getAgencyName().equals(agency.getAgencyName())) {
            projectMapper.updateAgencyName(agency.getId(), agency.getAgencyName());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgency(String id) {
        Agency agency = agencyMapper.selectById(id);
        if (agency == null) return;

        List<Project> projects = projectMapper.selectByAgencyId(id);
        int assignedCount = 0;
        int totalQuantity = 0;
        for (Project p : projects) {
            if (p.getQuantity() != null) totalQuantity += p.getQuantity();
            if ("已分配".equals(p.getStatus())) assignedCount++;
            projectMapper.deleteById(p.getId());
        }

        agencyFileMapper.deleteByBizId(id, "agency");
        String fileDir = FILES_BASE + id;
        File dir = new File(fileDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            dir.delete();
        }

        agencyMapper.deleteById(id);

        if (agency.getProjectCount() != null && agency.getProjectCount() > 0) {
            recalculateAllRatios();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchAgency(List<String> ids) {
        for (String id : ids) {
            deleteAgency(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetAllRatios() {
        agencyMapper.resetAllRatios();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateAllRatios() {
        Integer total = projectMapper.countAllUndertaken();
        List<Agency> agencies = agencyMapper.selectList(new QueryWrapper<Agency>().eq("del_flag", 0));
        for (Agency a : agencies) {
            Integer count = projectMapper.countUndertakenByAgency(a.getId());
            int actualCount = count != null ? count : 0;
            BigDecimal utRatio = BigDecimal.ZERO;
            if (total != null && total > 0 && actualCount > 0) {
                utRatio = new BigDecimal(actualCount)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
            }
            agencyMapper.updateProjectCountAndUtRatio(a.getId(), actualCount, utRatio);
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Agency> all = agencyMapper.selectList(new QueryWrapper<Agency>().eq("del_flag", 0));
        long eligibleCount = all.stream()
                .filter(a -> a.getRatio() != null && a.getRatio() > 0)
                .filter(a -> {
                    BigDecimal ratio = new BigDecimal(a.getRatio());
                    BigDecimal utRatio = a.getUtRatio() != null ? a.getUtRatio() : BigDecimal.ZERO;
                    return utRatio.compareTo(ratio) <= 0;
                })
                .count();
        int ratioSum = all.stream().mapToInt(a -> a.getRatio() != null ? a.getRatio() : 0).sum();
        int pendingCount = projectMapper.countPending();

        stats.put("eligibleCount", eligibleCount);
        stats.put("ratioSum", ratioSum);
        stats.put("pendingCount", pendingCount);
        return stats;
    }

    @Override
    public int getAssignedProjectCount(String agencyId) {
        return projectMapper.countAssignedByAgency(agencyId);
    }

    @Override
    public void calculatePerformance(Agency agency) {
        if (agency.getScore2023() == null || agency.getScore2024() == null || agency.getScore2025() == null) return;

        BigDecimal avg = new BigDecimal(agency.getScore2023() + agency.getScore2024() + agency.getScore2025())
                .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        agency.setAvgScore(avg);

        double avgVal = avg.doubleValue();
        String grade;
        double coefficient;
        if (avgVal >= 95) {
            grade = "A";
            coefficient = 1.5;
        } else if (avgVal >= 90) {
            grade = "B+";
            coefficient = 1.2;
        } else if (avgVal >= 85) {
            grade = "B";
            coefficient = 1.0;
        } else if (avgVal >= 80) {
            grade = "B-";
            coefficient = 0.8;
        } else {
            grade = "C";
            coefficient = 0.5;
        }
        agency.setGrade(grade);
        agency.setWeightCoefficient(new BigDecimal(String.valueOf(coefficient)));
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
            validateAgencyHeader(sheet.getRow(0));

            StringBuilder errors = new StringBuilder();
            List<Agency> toSave = new ArrayList<>();

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = getCellString(row, 0);
                if (name == null || name.trim().isEmpty()) {
                    errors.append("第 ").append(i + 1).append(" 行：机构名称必填；");
                    continue;
                }

                QueryWrapper<Agency> qw = new QueryWrapper<>();
                qw.eq("agency_name", name.trim());
                qw.eq("del_flag", 0);
                if (agencyMapper.selectCount(qw) > 0) {
                    errors.append("第 ").append(i + 1).append(" 行：机构名称已存在；");
                    continue;
                }

                Integer s2023 = getCellInt(row, 1);
                Integer s2024 = getCellInt(row, 2);
                Integer s2025 = getCellInt(row, 3);
                Integer ratio = getCellInt(row, 4);

                boolean rowHasError = false;
                if (s2023 == null || s2023 < 0 || s2023 > 100) {
                    errors.append("第 ").append(i + 1).append(" 行：2023年绩效分超出范围（0-100）；");
                    rowHasError = true;
                }
                if (s2024 == null || s2024 < 0 || s2024 > 100) {
                    errors.append("第 ").append(i + 1).append(" 行：2024年绩效分超出范围（0-100）；");
                    rowHasError = true;
                }
                if (s2025 == null || s2025 < 0 || s2025 > 100) {
                    errors.append("第 ").append(i + 1).append(" 行：2025年绩效分超出范围（0-100）；");
                    rowHasError = true;
                }
                if (ratio == null || ratio < 0 || ratio > 100) {
                    errors.append("第 ").append(i + 1).append(" 行：本次摇号预设比例格式错误或缺失；");
                    rowHasError = true;
                }

                if (rowHasError) continue;

                Agency agency = new Agency();
                agency.setAgencyName(name.trim());
                agency.setScore2023(s2023);
                agency.setScore2024(s2024);
                agency.setScore2025(s2025);
                agency.setRatio(ratio != null ? ratio : 0);
                toSave.add(agency);
            }

            if (errors.length() > 0) {
                throw new JeecgBootException(errors.toString());
            }
            if (toSave.isEmpty()) {
                throw new JeecgBootException("未导入任何有效数据");
            }

            for (Agency agency : toSave) {
                doSaveAgency(agency);
            }

            int ratioSum = agencyMapper.sumAllRatios();
            if (ratioSum != 100) {
                return "导入成功，共 " + toSave.size() + " 条记录。注意：本次摇号预设比例之和为 " + ratioSum + "%，不等于 100%，请调整后方可执行遴选";
            }
            return "导入成功，共 " + toSave.size() + " 条记录";
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入失败", e);
            throw new JeecgBootException("导入失败：" + e.getMessage());
        }
    }

    private void validateAgencyHeader(Row header) {
        if (header == null) throw new JeecgBootException("Excel 缺少表头行，请使用下载的模板");
        String[] expected = {"机构名称", "2023年绩效分", "2024年绩效分", "2025年绩效分", "本次摇号预设比例(%)"};
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
            Sheet sheet = wb.createSheet("机构列表");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("机构名称");
            header.createCell(1).setCellValue("2023年绩效分");
            header.createCell(2).setCellValue("2024年绩效分");
            header.createCell(3).setCellValue("2025年绩效分");
            header.createCell(4).setCellValue("本次摇号预设比例(%)");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("机构导入模板.xlsx", "UTF-8"));
            ServletOutputStream out = response.getOutputStream();
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            log.error("下载模板失败", e);
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
