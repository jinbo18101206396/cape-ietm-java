package org.jeecg.modules.ietm.projectmanagement.service.impl;

import org.jeecg.modules.ietm.ietmprojectcompany.entity.IetmProjectCompany;
import org.jeecg.modules.ietm.ietmprojectcompany.mapper.IetmProjectCompanyMapper;
import org.jeecg.modules.ietm.ietmprojectcompany.service.IIetmProjectCompanyService;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProjectParams;
import org.jeecg.modules.ietm.projectmanagement.mapper.IetmProjectParamsMapper;
import org.jeecg.modules.ietm.projectmanagement.mapper.IetmProjectMapper;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Collection;

/**
 * @Description: 手册管理-手册项目管理列表
 * @Author: jeecg-boot
 * @Date: 2026-01-09
 * @Version: V1.0
 */
@Service
public class IetmProjectServiceImpl extends ServiceImpl<IetmProjectMapper, IetmProject> implements IIetmProjectService {

    @Autowired
    private IetmProjectMapper ietmProjectMapper;
    @Autowired
    private IetmProjectParamsMapper ietmProjectParamsMapper;

    @Autowired
    private IIetmProjectCompanyService projectCompanyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMain(IetmProject ietmProject, IetmProjectParams ietmProjectParam) {
        ietmProjectMapper.insert(ietmProject);
        String id = ietmProject.getId();
        //更新项目的责任单位和创作单位
        List<IetmProjectCompany> projectCompanyList = ietmProject.getProjectCompany();
        if (projectCompanyList != null && projectCompanyList.size() > 0) {
            projectCompanyList.forEach(p -> {
                p.setPid(id);
                p.setSecurity(ietmProject.getSecurity());
            });
            projectCompanyService.saveOrUpdateBatch(projectCompanyList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMain(IetmProject ietmProject) {
        ietmProjectMapper.updateById(ietmProject);
        //更新项目的责任单位和创作单位
        List<IetmProjectCompany> projectCompanyList = ietmProject.getProjectCompany();
        if (projectCompanyList != null && projectCompanyList.size() > 0) {
            projectCompanyList.forEach(p -> {
                p.setSecurity(ietmProject.getSecurity());
            });
            projectCompanyService.saveOrUpdateBatch(projectCompanyList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delMain(String id) {
        ietmProjectParamsMapper.deleteByMainId(id);
        ietmProjectMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            ietmProjectParamsMapper.deleteByMainId(id.toString());
            ietmProjectMapper.deleteById(id);
        }
    }

}
