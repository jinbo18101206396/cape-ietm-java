package org.jeecg.modules.ietm.projectconfigurationmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.TreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectGxTreeVo;
import org.jeecg.modules.ietm.projectconfigurationmanagement.mapper.IetmProjectConfigurationManagementMapper;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import utils.TreeUtil;

/**
 * @Description: 项目管理-项目构型管理
 * @Author: jeecg-boot
 * @Date:   2026-02-10
 * @Version: V1.0
 */
@Service
public class IetmProjectConfigurationManagementServiceImpl extends ServiceImpl<IetmProjectConfigurationManagementMapper, IetmProjectConfigurationManagement> implements IIetmProjectConfigurationManagementService {

	@Override
	public void addIetmProjectConfigurationManagement(IetmProjectConfigurationManagement ietmProjectConfigurationManagement) {
	   //新增时设置hasChild为0
	    ietmProjectConfigurationManagement.setHasChild(IIetmProjectConfigurationManagementService.NOCHILD);
		if(oConvertUtils.isEmpty(ietmProjectConfigurationManagement.getPid())){
			ietmProjectConfigurationManagement.setPid(IIetmProjectConfigurationManagementService.ROOT_PID_VALUE);
		}else{
			//如果当前节点父ID不为空 则设置父节点的hasChildren 为1
			IetmProjectConfigurationManagement parent = baseMapper.selectById(ietmProjectConfigurationManagement.getPid());
			if(parent!=null && !"1".equals(parent.getHasChild())){
				parent.setHasChild("1");
				baseMapper.updateById(parent);
			}
		}
		baseMapper.insert(ietmProjectConfigurationManagement);
	}

	@Override
	public void updateIetmProjectConfigurationManagement(IetmProjectConfigurationManagement ietmProjectConfigurationManagement) {
		IetmProjectConfigurationManagement entity = this.getById(ietmProjectConfigurationManagement.getId());
		if(entity==null) {
			throw new JeecgBootException("未找到对应实体");
		}
		String old_pid = entity.getPid();
		String new_pid = ietmProjectConfigurationManagement.getPid();
		if(!old_pid.equals(new_pid)) {
			updateOldParentNode(old_pid);
			if(oConvertUtils.isEmpty(new_pid)){
				ietmProjectConfigurationManagement.setPid(IIetmProjectConfigurationManagementService.ROOT_PID_VALUE);
			}
			if(!IIetmProjectConfigurationManagementService.ROOT_PID_VALUE.equals(ietmProjectConfigurationManagement.getPid())) {
				baseMapper.updateTreeNodeStatus(ietmProjectConfigurationManagement.getPid(), IIetmProjectConfigurationManagementService.HASCHILD);
			}
		}
		baseMapper.updateById(ietmProjectConfigurationManagement);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteIetmProjectConfigurationManagement(String id) throws JeecgBootException {
		//查询选中节点下所有子节点一并删除
        id = this.queryTreeChildIds(id);
        if(id.indexOf(",")>0) {
            StringBuffer sb = new StringBuffer();
            String[] idArr = id.split(",");
            for (String idVal : idArr) {
                if(idVal != null){
                    IetmProjectConfigurationManagement ietmProjectConfigurationManagement = this.getById(idVal);
                    String pidVal = ietmProjectConfigurationManagement.getPid();
                    //查询此节点上一级是否还有其他子节点
                    List<IetmProjectConfigurationManagement> dataList = baseMapper.selectList(new QueryWrapper<IetmProjectConfigurationManagement>().eq("pid", pidVal).notIn("id",Arrays.asList(idArr)));
                    boolean flag = (dataList == null || dataList.size() == 0) && !Arrays.asList(idArr).contains(pidVal) && !sb.toString().contains(pidVal);
                    if(flag){
                        //如果当前节点原本有子节点 现在木有了，更新状态
                        sb.append(pidVal).append(",");
                    }
                }
            }
            //批量删除节点
            baseMapper.deleteBatchIds(Arrays.asList(idArr));
            //修改已无子节点的标识
            String[] pidArr = sb.toString().split(",");
            for(String pid : pidArr){
                this.updateOldParentNode(pid);
            }
        }else{
            IetmProjectConfigurationManagement ietmProjectConfigurationManagement = this.getById(id);
            if(ietmProjectConfigurationManagement==null) {
                throw new JeecgBootException("未找到对应实体");
            }
            updateOldParentNode(ietmProjectConfigurationManagement.getPid());
            baseMapper.deleteById(id);
        }
	}

	@Override
    public List<IetmProjectConfigurationManagement> queryTreeListNoPage(QueryWrapper<IetmProjectConfigurationManagement> queryWrapper) {
        List<IetmProjectConfigurationManagement> dataList = baseMapper.selectList(queryWrapper);
        List<IetmProjectConfigurationManagement> mapList = new ArrayList<>();
        for(IetmProjectConfigurationManagement data : dataList){
            String pidVal = data.getPid();
            //递归查询子节点的根节点
            if(pidVal != null && !IIetmProjectConfigurationManagementService.NOCHILD.equals(pidVal)){
                IetmProjectConfigurationManagement rootVal = this.getTreeRoot(pidVal);
                if(rootVal != null && !mapList.contains(rootVal)){
                    mapList.add(rootVal);
                }
            }else{
                if(!mapList.contains(data)){
                    mapList.add(data);
                }
            }
        }
        return mapList;
    }

    @Override
    public List<SelectTreeModel> queryListByCode(String parentCode) {
        String pid = ROOT_PID_VALUE;
        if (oConvertUtils.isNotEmpty(parentCode)) {
            LambdaQueryWrapper<IetmProjectConfigurationManagement> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IetmProjectConfigurationManagement::getPid, parentCode);
            List<IetmProjectConfigurationManagement> list = baseMapper.selectList(queryWrapper);
            if (list == null || list.size() == 0) {
                throw new JeecgBootException("该编码【" + parentCode + "】不存在，请核实!");
            }
            if (list.size() > 1) {
                throw new JeecgBootException("该编码【" + parentCode + "】存在多个，请核实!");
            }
            pid = list.get(0).getId();
        }
        return baseMapper.queryListByPid(pid, null);
    }

    @Override
    public List<SelectTreeModel> queryListByPid(String pid) {
        if (oConvertUtils.isEmpty(pid)) {
            pid = ROOT_PID_VALUE;
        }
        return baseMapper.queryListByPid(pid, null);
    }

	/**
	 * 根据所传pid查询旧的父级节点的子节点并修改相应状态值
	 * @param pid
	 */
	private void updateOldParentNode(String pid) {
		if(!IIetmProjectConfigurationManagementService.ROOT_PID_VALUE.equals(pid)) {
			Long count = baseMapper.selectCount(new QueryWrapper<IetmProjectConfigurationManagement>().eq("pid", pid));
			if(count==null || count<=1) {
				baseMapper.updateTreeNodeStatus(pid, IIetmProjectConfigurationManagementService.NOCHILD);
			}
		}
	}

	/**
     * 递归查询节点的根节点
     * @param pidVal
     * @return
     */
    private IetmProjectConfigurationManagement getTreeRoot(String pidVal){
        IetmProjectConfigurationManagement data =  baseMapper.selectById(pidVal);
        if(data != null && !IIetmProjectConfigurationManagementService.ROOT_PID_VALUE.equals(data.getPid())){
            return this.getTreeRoot(data.getPid());
        }else{
            return data;
        }
    }

    /**
     * 根据id查询所有子节点id
     * @param ids
     * @return
     */
    private String queryTreeChildIds(String ids) {
        //获取id数组
        String[] idArr = ids.split(",");
        StringBuffer sb = new StringBuffer();
        for (String pidVal : idArr) {
            if(pidVal != null){
                if(!sb.toString().contains(pidVal)){
                    if(sb.toString().length() > 0){
                        sb.append(",");
                    }
                    sb.append(pidVal);
                    this.getTreeChildIds(pidVal,sb);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 递归查询所有子节点
     * @param pidVal
     * @param sb
     * @return
     */
    private StringBuffer getTreeChildIds(String pidVal,StringBuffer sb){
        List<IetmProjectConfigurationManagement> dataList = baseMapper.selectList(new QueryWrapper<IetmProjectConfigurationManagement>().eq("pid", pidVal));
        if(dataList != null && dataList.size()>0){
            for(IetmProjectConfigurationManagement tree : dataList) {
                if(!sb.toString().contains(tree.getId())){
                    sb.append(",").append(tree.getId());
                }
                this.getTreeChildIds(tree.getId(),sb);
            }
        }
        return sb;
    }


    @Override
    public List<TreeModel> queryTreeList() {
        LambdaQueryWrapper<IetmProjectConfigurationManagement> queryWrapper = new LambdaQueryWrapper<IetmProjectConfigurationManagement>();
        queryWrapper.orderByAsc(IetmProjectConfigurationManagement::getSeq);
        List<IetmProjectConfigurationManagement> list = list(queryWrapper);
        for(IetmProjectConfigurationManagement entity : list){
            StringBuilder sb = new StringBuilder(entity.getCode());
            sb.append(" ");
            sb.append(entity.getTitle());
            entity.setTitle(sb.toString());
        }
        return TreeUtil.converListToTree(list,"title",ROOT_PID_VALUE);
    }

    @Override
    public List<IetmProjectGxTreeVo> buildTree(List<IetmProjectConfigurationManagement> list, String projectId) {
        List<IetmProjectGxTreeVo> treeList = new ArrayList<>();
        for(IetmProjectConfigurationManagement node : list){
            IetmProjectGxTreeVo treeVo = new IetmProjectGxTreeVo();
            treeVo.setId(node.getId());
            treeVo.setKey(node.getId());
            treeVo.setValue(node.getId());
            treeVo.setParentId(node.getPid());
            // 格式化节点标题：编码 技术名称
            String title = (node.getCode() != null ? node.getCode() : "") + " " + (node.getTitle() != null ? node.getTitle() : "");
            treeVo.setTitle(title.trim());
            treeList.add(treeVo);
        }

        //id->对象映射，O（1）快速查找父节点
        HashMap<String, IetmProjectGxTreeVo> nodeMap = new HashMap<>();
        for(IetmProjectGxTreeVo treeVo : treeList){
            nodeMap.put(treeVo.getId(), treeVo);
        }

        //挂载子节点
        for(IetmProjectGxTreeVo treeVo : treeList){
            String parentId = treeVo.getParentId();
            if(StringUtils.isBlank(parentId) || StringUtils.equals(parentId, "0")){
                continue;
            }
            IetmProjectGxTreeVo parent = nodeMap.get(parentId);
            if(parent != null){
                if(parent.getChildren() == null){
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(treeVo);
            }
        }
        //找到根节点（树形结构）并返回
        List<IetmProjectGxTreeVo> rootList = treeList.stream()
                .filter(treeVo -> treeVo.getParentId() == null || StringUtils.equals(treeVo.getParentId(), "0"))
                .collect(Collectors.toList());

        // 修改根节点标题为：装备编码 项目名称
        if (!rootList.isEmpty() && projectId != null) {
            IetmProjectGxTreeVo rootNode = rootList.get(0);
            // 查询项目信息
            org.jeecg.modules.ietm.projectmanagement.entity.IetmProject project =
                baseMapper.selectProjectById(projectId);
            if (project != null) {
                String rootTitle = (project.getEquipmentCode() != null ? project.getEquipmentCode() : "") + " " +
                                   (project.getName() != null ? project.getName() : "");
                rootNode.setTitle(rootTitle.trim());
            }
        }

        return rootList;
    }


}
