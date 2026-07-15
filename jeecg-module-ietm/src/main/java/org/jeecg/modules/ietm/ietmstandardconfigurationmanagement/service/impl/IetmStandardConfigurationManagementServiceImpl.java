package org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.entity.IetmStandardConfigurationManagement;
import org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.mapper.IetmStandardConfigurationManagementMapper;
import org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.service.IIetmStandardConfigurationManagementService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 预制模板-构型管理
 * @Author: jeecg-boot
 * @Date:   2026-01-07
 * @Version: V1.0
 */
@Service
public class IetmStandardConfigurationManagementServiceImpl extends ServiceImpl<IetmStandardConfigurationManagementMapper, IetmStandardConfigurationManagement> implements IIetmStandardConfigurationManagementService {

    private String codeRule ="A-00-0-0-00-00-A";

	@Override
	public void addIetmStandardConfigurationManagement(IetmStandardConfigurationManagement ietmStandardConfigurationManagement) {
	   //新增时设置hasChild为0
	    ietmStandardConfigurationManagement.setHasChild(IIetmStandardConfigurationManagementService.NOCHILD);
		if(oConvertUtils.isEmpty(ietmStandardConfigurationManagement.getPid())){
			ietmStandardConfigurationManagement.setPid(IIetmStandardConfigurationManagementService.ROOT_PID_VALUE);
		}else{
			//如果当前节点父ID不为空 则设置父节点的hasChildren 为1
			IetmStandardConfigurationManagement parent = baseMapper.selectById(ietmStandardConfigurationManagement.getPid());
			if(parent!=null && !"1".equals(parent.getHasChild())){
				parent.setHasChild("1");
				baseMapper.updateById(parent);
			}
		}
		baseMapper.insert(ietmStandardConfigurationManagement);
	}

	@Override
	public void updateIetmStandardConfigurationManagement(IetmStandardConfigurationManagement ietmStandardConfigurationManagement) {
		IetmStandardConfigurationManagement entity = this.getById(ietmStandardConfigurationManagement.getId());
		if(entity==null) {
			throw new JeecgBootException("未找到对应实体");
		}
		String old_pid = entity.getPid();
		String new_pid = ietmStandardConfigurationManagement.getPid();
		if(!old_pid.equals(new_pid)) {
			updateOldParentNode(old_pid);
			if(oConvertUtils.isEmpty(new_pid)){
				ietmStandardConfigurationManagement.setPid(IIetmStandardConfigurationManagementService.ROOT_PID_VALUE);
			}
			if(!IIetmStandardConfigurationManagementService.ROOT_PID_VALUE.equals(ietmStandardConfigurationManagement.getPid())) {
				baseMapper.updateTreeNodeStatus(ietmStandardConfigurationManagement.getPid(), IIetmStandardConfigurationManagementService.HASCHILD);
			}
		}
		baseMapper.updateById(ietmStandardConfigurationManagement);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteIetmStandardConfigurationManagement(String id) throws JeecgBootException {
		//查询选中节点下所有子节点一并删除
        id = this.queryTreeChildIds(id);
        if(id.indexOf(",")>0) {
            StringBuffer sb = new StringBuffer();
            String[] idArr = id.split(",");
            for (String idVal : idArr) {
                if(idVal != null){
                    IetmStandardConfigurationManagement ietmStandardConfigurationManagement = this.getById(idVal);
                    String pidVal = ietmStandardConfigurationManagement.getPid();
                    //查询此节点上一级是否还有其他子节点
                    List<IetmStandardConfigurationManagement> dataList = baseMapper.selectList(new QueryWrapper<IetmStandardConfigurationManagement>().eq("pid", pidVal).notIn("id",Arrays.asList(idArr)));
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
            IetmStandardConfigurationManagement ietmStandardConfigurationManagement = this.getById(id);
            if(ietmStandardConfigurationManagement==null) {
                throw new JeecgBootException("未找到对应实体");
            }
            updateOldParentNode(ietmStandardConfigurationManagement.getPid());
            baseMapper.deleteById(id);
        }
	}

	@Override
    public List<IetmStandardConfigurationManagement> queryTreeListNoPage(QueryWrapper<IetmStandardConfigurationManagement> queryWrapper) {
        List<IetmStandardConfigurationManagement> dataList = baseMapper.selectList(queryWrapper);
        List<IetmStandardConfigurationManagement> mapList = new ArrayList<>();
        for(IetmStandardConfigurationManagement data : dataList){
            String pidVal = data.getPid();
            //递归查询子节点的根节点
            if(pidVal != null && !IIetmStandardConfigurationManagementService.NOCHILD.equals(pidVal)){
                IetmStandardConfigurationManagement rootVal = this.getTreeRoot(pidVal);
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
            LambdaQueryWrapper<IetmStandardConfigurationManagement> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IetmStandardConfigurationManagement::getPid, parentCode);
            List<IetmStandardConfigurationManagement> list = baseMapper.selectList(queryWrapper);
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
		if(!IIetmStandardConfigurationManagementService.ROOT_PID_VALUE.equals(pid)) {
			Long count = baseMapper.selectCount(new QueryWrapper<IetmStandardConfigurationManagement>().eq("pid", pid));
			if(count==null || count<=1) {
				baseMapper.updateTreeNodeStatus(pid, IIetmStandardConfigurationManagementService.NOCHILD);
			}
		}
	}

	/**
     * 递归查询节点的根节点
     * @param pidVal
     * @return
     */
    private IetmStandardConfigurationManagement getTreeRoot(String pidVal){
        IetmStandardConfigurationManagement data =  baseMapper.selectById(pidVal);
        if(data != null && !IIetmStandardConfigurationManagementService.ROOT_PID_VALUE.equals(data.getPid())){
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
        List<IetmStandardConfigurationManagement> dataList = baseMapper.selectList(new QueryWrapper<IetmStandardConfigurationManagement>().eq("pid", pidVal));
        if(dataList != null && dataList.size()>0){
            for(IetmStandardConfigurationManagement tree : dataList) {
                if(!sb.toString().contains(tree.getId())){
                    sb.append(",").append(tree.getId());
                }
                this.getTreeChildIds(tree.getId(),sb);
            }
        }
        return sb;
    }
    @Override
    public void setWay(List<IetmStandardConfigurationManagement> list) {
        list.stream().forEach(a->{
            ArrayList<String>  codeList = new ArrayList<>();
            this.getWay(a,codeList);
            //反转一下
            Collections.reverse(codeList);
            String[] split = this.codeRule.split("-");
            for (int i = 0; i < codeList.size(); i++) {
                split[i]=codeList.get(i).trim();
            }
            String way = String.join("-", split);
            a.setWay(way);
        });
    }

    @Override
    public Integer getCodeTemp(String pid) {
        IetmStandardConfigurationManagement ietmStandardConfigurationManagement = baseMapper.selectById(pid);
        Integer index = 0;
        Integer num ;
        while (ietmStandardConfigurationManagement!=null){
            index ++ ;
            pid=ietmStandardConfigurationManagement.getPid();
            ietmStandardConfigurationManagement = baseMapper.selectById(pid);
        }
        String[] split = this.codeRule.split("-");
        try {
            String s = split[index];
            num=s.length();
        }catch (IndexOutOfBoundsException e){
            num= -1 ;
        }
        return num;
    }

    private void getWay(IetmStandardConfigurationManagement ietmStandardConfigurationManagement, ArrayList<String> codeList) {
        codeList.add(ietmStandardConfigurationManagement.getCode());
        QueryWrapper<IetmStandardConfigurationManagement> ietmStandardConfigurationManagementQueryWrapper = new QueryWrapper<>();
        ietmStandardConfigurationManagementQueryWrapper.eq("id", ietmStandardConfigurationManagement.getPid());
        IetmStandardConfigurationManagement parent = baseMapper.selectOne(ietmStandardConfigurationManagementQueryWrapper);
        if(parent!=null){
            getWay(parent,codeList);
        }
    }
}
