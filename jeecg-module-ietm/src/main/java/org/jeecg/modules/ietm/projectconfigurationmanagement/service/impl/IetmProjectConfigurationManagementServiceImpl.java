package org.jeecg.modules.ietm.projectconfigurationmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import lombok.extern.slf4j.Slf4j;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import utils.TreeUtil;

/**
 * @Description: 项目管理-项目构型管理（增强版）
 * @Author: jeecg-boot
 * @Date:   2026-02-10
 * @Version: V1.0
 */
@Service
@Slf4j
public class IetmProjectConfigurationManagementServiceImpl extends ServiceImpl<IetmProjectConfigurationManagementMapper, IetmProjectConfigurationManagement> implements IIetmProjectConfigurationManagementService {

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addIetmProjectConfigurationManagement(IetmProjectConfigurationManagement entity) {
		log.info("============================================================");
		log.info("添加构型节点开始");
		log.info("编码: {}, 技术名称: {}, 父节点ID: {}, 项目ID: {}",
				entity.getCode(), entity.getTitle(), entity.getPid(), entity.getProjectId());
		log.info("============================================================");

		// 1. 验证编码长度
		String lengthError = validateCodeLength(entity.getCode(), entity.getPid(), entity.getProjectId());
		if (lengthError != null) {
			log.error("编码长度验证失败: {}", lengthError);
			throw new JeecgBootException(lengthError);
		}

		// 2. 验证编码是否重复
		if (checkCodeDuplicate(entity.getCode(), entity.getPid(), null)) {
			throw new JeecgBootException("同级节点下编码【" + entity.getCode() + "】已存在！");
		}

		// 3. 生成路径
		String path = generatePath(entity.getPid(), entity.getCode(), entity.getProjectId());
		entity.setPath(path);
		log.info("生成路径: {}", path);

		// 3. 设置默认值
		entity.setHasChild(IIetmProjectConfigurationManagementService.NOCHILD);
		if(oConvertUtils.isEmpty(entity.getPid())){
			entity.setPid(IIetmProjectConfigurationManagementService.ROOT_PID_VALUE);
		}else{
			//如果当前节点父ID不为空 则设置父节点的hasChildren 为1
			IetmProjectConfigurationManagement parent = baseMapper.selectById(entity.getPid());
			if(parent!=null && !"1".equals(parent.getHasChild())){
				parent.setHasChild("1");
				baseMapper.updateById(parent);
			}
		}

		// 4. 保存节点
		baseMapper.insert(entity);
		log.info("构型节点保存成功: id={}", entity.getId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateIetmProjectConfigurationManagement(IetmProjectConfigurationManagement entity) {
		log.info("更新构型节点: id={}, code={}, title={}", entity.getId(), entity.getCode(), entity.getTitle());

		IetmProjectConfigurationManagement oldEntity = this.getById(entity.getId());
		if(oldEntity==null) {
			throw new JeecgBootException("未找到对应实体");
		}

		// 如果编码改变，验证长度和重复性
		if (!oldEntity.getCode().equals(entity.getCode())) {
			// ✅ 修复：编辑时也使用 validateCodeLength，现在它正确计算层级了
			String lengthError = validateCodeLength(entity.getCode(), entity.getPid(), entity.getProjectId());
			if (lengthError != null) {
				throw new JeecgBootException(lengthError);
			}

			// 验证编码是否重复
			if (checkCodeDuplicate(entity.getCode(), entity.getPid(), entity.getId())) {
				throw new JeecgBootException("同级节点下编码【" + entity.getCode() + "】已存在！");
			}
		}

		// 如果编码改变，重新生成路径并更新子节点
		if (!oldEntity.getCode().equals(entity.getCode())) {
			String newPath = generatePath(entity.getPid(), entity.getCode(), entity.getProjectId());
			entity.setPath(newPath);
			log.info("路径已更新: {} -> {}", oldEntity.getPath(), newPath);

			// 递归更新所有子节点的路径
			updateChildrenPath(entity.getId(), newPath, entity.getProjectId());
		}

		String old_pid = oldEntity.getPid();
		String new_pid = entity.getPid();
		if(!old_pid.equals(new_pid)) {
			updateOldParentNode(old_pid);
			if(oConvertUtils.isEmpty(new_pid)){
				entity.setPid(IIetmProjectConfigurationManagementService.ROOT_PID_VALUE);
			}
			if(!IIetmProjectConfigurationManagementService.ROOT_PID_VALUE.equals(entity.getPid())) {
				baseMapper.updateTreeNodeStatus(entity.getPid(), IIetmProjectConfigurationManagementService.HASCHILD);
			}
		}
		baseMapper.updateById(entity);
		log.info("构型节点更新成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteIetmProjectConfigurationManagement(String id) throws JeecgBootException {
		log.info("删除构型节点: id={}", id);

		// 检查是否可删除
		String errorMsg = checkCanDelete(id);
		if (errorMsg != null) {
			throw new JeecgBootException(errorMsg);
		}

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
        log.info("构型节点删除成功");
	}

	@Override
    public List<IetmProjectConfigurationManagement> queryTreeListNoPage(QueryWrapper<IetmProjectConfigurationManagement> queryWrapper) {
        // 直接返回查询结果，不再递归查找根节点
        // 原逻辑会导致跨项目数据泄露：如果某个节点的pid指向其他项目，会把其他项目数据查出来
        List<IetmProjectConfigurationManagement> dataList = baseMapper.selectList(queryWrapper);
        log.debug("queryTreeListNoPage查询结果数量: {}", dataList.size());
        return dataList;
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

    // ==================== 新增方法 ====================

    /**
     * 验证编码是否重复
     */
    @Override
    public boolean checkCodeDuplicate(String code, String pid, String excludeId) {
        LambdaQueryWrapper<IetmProjectConfigurationManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IetmProjectConfigurationManagement::getCode, code);
        wrapper.eq(IetmProjectConfigurationManagement::getPid, pid);
        if (excludeId != null) {
            wrapper.ne(IetmProjectConfigurationManagement::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    /**
     * 检查节点是否可删除
     */
    @Override
    public String checkCanDelete(String id) {
        // 1. 检查是否有子节点
        LambdaQueryWrapper<IetmProjectConfigurationManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IetmProjectConfigurationManagement::getPid, id);
        long childCount = this.count(wrapper);
        if (childCount > 0) {
            return "该节点还有 " + childCount + " 个子节点，不能删除！请先删除子节点。";
        }

        // 2. 检查是否有DM引用
        try {
            int dmCount = baseMapper.countDmReference(id);
            if (dmCount > 0) {
                return "该构型节点下还有 " + dmCount + " 个数据模块，不能删除！";
            }
        } catch (Exception e) {
            log.warn("检查DM引用时出错，跳过检查: {}", e.getMessage());
        }

        // 3. 检查是否有ICN引用
        try {
            int icnCount = baseMapper.countIcnReference(id);
            if (icnCount > 0) {
                return "该构型节点下还有 " + icnCount + " 个实体，不能删除！";
            }
        } catch (Exception e) {
            log.warn("检查ICN引用时出错，跳过检查: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 更新父节点的hasChild标记
     */
    @Override
    public void updateParentHasChild(String pid) {
        if (pid == null || "0".equals(pid)) {
            return;
        }

        IetmProjectConfigurationManagement parent = this.getById(pid);
        if (parent != null) {
            LambdaQueryWrapper<IetmProjectConfigurationManagement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(IetmProjectConfigurationManagement::getPid, pid);
            long childCount = this.count(wrapper);

            String hasChild = childCount > 0 ? "1" : "0";
            if (!hasChild.equals(parent.getHasChild())) {
                parent.setHasChild(hasChild);
                this.updateById(parent);
                log.info("更新父节点hasChild标记: id={}, hasChild={}", pid, hasChild);
            }
        }
    }

    /**
     * 编码规则
     * 格式：装备编码-A-00-0-0-00-00-A（共8段）
     * 第1段：项目装备编码
     * 第2-8段：7级节点编码规则
     */
    private String codeRule = "A-00-0-0-00-00-A";

    /**
     * 生成路径
     * 路径格式：装备编码-A-00-0-0-00-00-A（固定8段）
     * 示例：
     *   根节点(0级): ZB001-A-00-0-0-00-00-A
     *   1级节点(code=B): ZB001-B-00-0-0-00-00-A (第2段替换为B)
     *   2级节点(code=01): ZB001-B-01-0-0-00-00-A (第2-3段替换)
     *   3级节点(code=C): ZB001-B-01-C-0-00-00-A (第2-4段替换)
     *
     * 规则：根据实际编码链替换模板中对应位置的占位符，未使用的层级保留模板占位符
     */
    private String generatePath(String pid, String code, String projectId) {
        // 获取项目的装备编码作为路径第一段
        String equipmentCode = getEquipmentCodeByProjectId(projectId);

        // 使用编码规则模板（7段）
        String[] pathSegments = this.codeRule.split("-");

        // 如果是根节点（pid='0'），返回完整模板路径
        if ("0".equals(pid)) {
            String rootPath = equipmentCode + "-" + this.codeRule;
            log.info("生成根节点路径: equipmentCode={}, path={}", equipmentCode, rootPath);
            return rootPath;
        }

        // 收集从当前节点到根节点的所有编码（不包括根节点的code）
        ArrayList<String> codeList = new ArrayList<>();
        codeList.add(code);

        String currentPid = pid;
        int depth = 1; // 当前节点深度（1级节点深度为1）

        while (currentPid != null && !"0".equals(currentPid)) {
            IetmProjectConfigurationManagement parent = this.getById(currentPid);
            if (parent == null) {
                log.warn("未找到父节点: parentId={}", currentPid);
                break;
            }

            // 如果父节点是根节点（pid='0'），不添加其code（根节点的code不参与路径生成）
            if ("0".equals(parent.getPid())) {
                log.debug("到达根节点，停止收集编码");
                break;
            }

            // 添加父节点编码
            codeList.add(parent.getCode());
            depth++;
            currentPid = parent.getPid();
        }

        // 反转列表（从1级节点到当前节点）
        Collections.reverse(codeList);

        log.debug("收集到的编码链: {}, 节点深度: {}", codeList, depth);

        // 用实际编码替换模板中对应位置的占位符
        for (int i = 0; i < codeList.size() && i < pathSegments.length; i++) {
            pathSegments[i] = codeList.get(i).trim();
        }

        // 拼接完整路径：装备编码 + 7段编码（共8段）
        String path = equipmentCode + "-" + String.join("-", pathSegments);
        log.info("生成路径: code={}, pid={}, depth={}, codeList={}, path={}",
                 code, pid, depth, codeList, path);
        return path;
    }

    /**
     * 根据项目ID获取装备编码
     */
    private String getEquipmentCodeByProjectId(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            log.warn("项目ID为空，无法获取装备编码");
            return "UNKNOWN";
        }

        try {
            // 使用mapper方法查询项目信息
            org.jeecg.modules.ietm.projectmanagement.entity.IetmProject project =
                baseMapper.selectProjectById(projectId);

            if (project != null && project.getEquipmentCode() != null) {
                return project.getEquipmentCode();
            }

            log.warn("未找到项目ID为 {} 的装备编码", projectId);
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("查询项目装备编码失败: projectId={}, error={}", projectId, e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * 递归更新子节点路径
     */
    private void updateChildrenPath(String parentId, String parentPath, String projectId) {
        LambdaQueryWrapper<IetmProjectConfigurationManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IetmProjectConfigurationManagement::getPid, parentId);
        List<IetmProjectConfigurationManagement> children = this.list(wrapper);

        log.info("递归更新子节点路径，父节点: {}, 子节点数: {}", parentId, children.size());

        for (IetmProjectConfigurationManagement child : children) {
            // 使用新的路径生成逻辑
            String newPath = generatePath(child.getPid(), child.getCode(), projectId);
            child.setPath(newPath);
            this.updateById(child);
            log.info("更新子节点路径: id={}, path={}", child.getId(), newPath);

            // 递归更新孙节点
            updateChildrenPath(child.getId(), newPath, projectId);
        }
    }

    /**
     * 计算子节点层级（根据父节点ID）
     * 注意：这个方法返回的是"以pid为父节点的子节点应该是第几级"
     * 例如：pid指向根节点(pid='0') → 返回1（子节点是第1级）
     *       pid指向第1级节点 → 返回2（子节点是第2级）
     *
     * @param pid 父节点ID
     * @param projectId 项目ID（用于验证节点归属）
     * @return 子节点层级（1-7）
     */
    private int calculateNodeLevel(String pid, String projectId) {
        return calculateNodeLevelWithDepth(pid, projectId, 0);
    }

    /**
     * 计算子节点层级（带递归深度检测和项目ID验证）
     *
     * 方法语义：
     * - 传入父节点ID（pid）
     * - 返回"以该ID为父节点的子节点"应该是第几级
     *
     * 例如：
     * - pid='0'（根节点） → 返回1（子节点是第1级）
     * - pid=第1级节点ID → 返回2（子节点是第2级）
     * - pid=第3级节点ID → 返回4（子节点是第4级）
     *
     * 计算逻辑：
     * 1. 查询pid对应的父节点
     * 2. 计算父节点自身是第几级
     * 3. 返回 父节点层级 + 1
     */
    private int calculateNodeLevelWithDepth(String pid, String projectId, int depth) {
        log.info("[calculateNodeLevel] 深度={}, 计算pid={}的子节点层级", depth, pid);

        // 防止无限递归
        if (depth > 10) {
            log.error("[calculateNodeLevel] 递归深度超过10层，停止递归");
            return 1;
        }

        // ✅ 特殊情况：pid='0'表示根节点，根节点的子节点是第1级
        if (pid == null || "0".equals(pid)) {
            log.info("[calculateNodeLevel] pid='0'（根节点），子节点是第1级");
            return 1;
        }

        // ✅ 查询父节点
        IetmProjectConfigurationManagement parentNode = this.getById(pid);
        if (parentNode == null) {
            log.warn("[calculateNodeLevel] 未找到id={}的节点，返回1", pid);
            return 1;
        }

        log.info("[calculateNodeLevel] 查到父节点: id={}, code='{}', pid={}, projectId={}",
                  parentNode.getId(), parentNode.getCode(), parentNode.getPid(), parentNode.getProjectId());

        // ✅ 检查项目ID
        if (projectId != null && !projectId.equals(parentNode.getProjectId())) {
            log.error("[calculateNodeLevel] ⚠️ 父节点属于不同项目！当前={}, 父节点={}", projectId, parentNode.getProjectId());
            return 1;
        }

        // ✅ 计算父节点自身的层级
        int parentNodeLevel;

        // 优先使用level字段
        if (parentNode.getLevel() != null && parentNode.getLevel() > 0) {
            parentNodeLevel = parentNode.getLevel();
            log.info("[calculateNodeLevel] 使用父节点level字段: {}", parentNodeLevel);
        } else {
            // 递归计算：父节点的层级 = 父节点的父节点的子节点层级
            log.info("[calculateNodeLevel] level字段为null，递归计算父节点层级");
            parentNodeLevel = calculateNodeLevelWithDepth(parentNode.getPid(), projectId, depth + 1);
            log.info("[calculateNodeLevel] 递归返回：父节点层级={}", parentNodeLevel);
        }

        // ✅ 子节点层级 = 父节点层级 + 1
        int childLevel = parentNodeLevel + 1;
        log.info("[calculateNodeLevel] 结果：父节点层级={}，子节点层级={}", parentNodeLevel, childLevel);

        return childLevel;
    }

    /**
     * 根据层级获取编码应有的长度（根节点=0级，A=1级，00=2级，以此类推）
     * @param level 节点层级
     * @return 编码长度 (如果层级超出范围返回-1)
     */
    private int getCodeLengthByLevel(int level) {
        switch (level) {
            case 1: return 1; // 1级节点(A): 1位
            case 2: return 2; // 2级节点(00): 2位
            case 3: return 1; // 3级节点(0): 1位
            case 4: return 1; // 4级节点(0): 1位
            case 5: return 2; // 5级节点(00): 2位
            case 6: return 2; // 6级节点(00): 2位
            case 7: return 1; // 7级节点(A): 1位
            default: return -1; // 不支持的层级
        }
    }

    /**
     * 根据层级获取编码规则说明
     * @param level 节点层级
     * @return 编码规则说明
     */
    private String getCodeRuleByLevel(int level) {
        switch (level) {
            case 1: return "1位（如：A）";
            case 2: return "2位（如：00）";
            case 3: return "1位（如：0）";
            case 4: return "1位（如：0）";
            case 5: return "2位（如：00）";
            case 6: return "2位（如：00）";
            case 7: return "1位（如：A）";
            default: return "未知";
        }
    }

    /**
     * 从path字段解析节点层级
     * path格式固定8段：装备编码-A-00-0-0-00-00-A
     * 模板（第2-8段）：A, 00, 0, 0, 00, 00, A
     * 层级 = 最后一个与模板不同的段的位置（1-7）
     * 例：ZBBM33-A-00-0-0-00-00-A → 全部匹配模板 → level=0（根节点）
     *     ZBBM33-B-00-0-0-00-00-A → 第2段B≠A → level=1
     *     ZBBM33-B-01-0-0-00-00-A → 第2段B≠A,第3段01≠00 → level=2
     *     ZBBM33-B-01-C-0-00-00-A → 第2,3,4段不同 → level=3
     */
    private int getLevelFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        String[] segments = path.split("-");
        // path必须是8段（装备编码 + 7个层级编码）
        if (segments.length != 8) {
            log.warn("[getLevelFromPath] path段数不是8，实际={}, path={}", segments.length, path);
            return 0;
        }
        // 模板：第2-8段的默认值
        String[] template = {"A", "00", "0", "0", "00", "00", "A"};
        int level = 0;
        for (int i = 1; i <= 7; i++) {
            if (!segments[i].equals(template[i - 1])) {
                level = i; // 记录最后一个不同的位置
            }
        }
        return level;
    }

    /**
     * 验证编码长度是否符合规则
     * @param code 编码
     * @param pid 父节点ID
     * @param projectId 项目ID
     * @return 验证结果,如果验证通过返回null,否则返回错误消息
     */
    private String validateCodeLength(String code, String pid, String projectId) {
        if (code == null || code.isEmpty()) {
            return "编码不能为空";
        }

        int level;
        if (pid == null || "0".equals(pid)) {
            level = 1;
            log.info("[validateCodeLength] pid='0'，子节点是第1级");
        } else {
            IetmProjectConfigurationManagement parentNode = this.getById(pid);
            if (parentNode == null) {
                log.warn("[validateCodeLength] 未找到父节点 pid={}", pid);
                level = 1;
            } else {
                String parentPath = parentNode.getPath();
                log.info("[validateCodeLength] 父节点: id={}, code={}, path={}", parentNode.getId(), parentNode.getCode(), parentPath);
                if (parentPath != null && !parentPath.isEmpty()) {
                    int parentLevel = getLevelFromPath(parentPath);
                    level = parentLevel + 1;
                    log.info("[validateCodeLength] 父节点层级={}, 子节点层级={}", parentLevel, level);
                } else {
                    // path为空时降级到递归计算
                    level = calculateNodeLevel(pid, projectId);
                    log.warn("[validateCodeLength] 父节点path为空，递归计算层级={}", level);
                }
            }
        }

        log.info("[validateCodeLength] 编码={}, 层级={}", code, level);

        if (level > 7) {
            return "已达到最大层级(7级)，不能继续添加子节点。编码规则：A-00-0-0-00-00-A";
        }

        int requiredLength = getCodeLengthByLevel(level);
        if (requiredLength == -1) {
            return "不支持的节点层级: " + level + "。编码规则：A-00-0-0-00-00-A";
        }

        int actualLength = code.length();
        if (actualLength != requiredLength) {
            String ruleDesc = getCodeRuleByLevel(level);
            return String.format("第%d级节点编码必须是%s，当前为%d位。编码规则：A-00-0-0-00-00-A",
                level, ruleDesc, actualLength);
        }

        return null;
    }


    /**
     * 批量生成路径
     * @param projectId 项目ID
     * @return 更新的节点数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchGeneratePaths(String projectId) {
        log.info("开始批量生成路径，项目ID: {}", projectId);

        if (projectId == null || projectId.isEmpty()) {
            throw new JeecgBootException("项目ID不能为空");
        }

        // 查询该项目下的所有构型节点
        QueryWrapper<IetmProjectConfigurationManagement> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId);
        queryWrapper.orderByAsc("pid", "seq");
        List<IetmProjectConfigurationManagement> allNodes = this.list(queryWrapper);

        if (allNodes.isEmpty()) {
            log.warn("项目 {} 下没有构型节点", projectId);
            return 0;
        }

        log.info("查询到 {} 个构型节点", allNodes.size());

        // 构建节点映射，便于快速查找
        Map<String, IetmProjectConfigurationManagement> nodeMap = new HashMap<>();
        for (IetmProjectConfigurationManagement node : allNodes) {
            nodeMap.put(node.getId(), node);
        }

        // 找到所有根节点（pid='0'）
        List<IetmProjectConfigurationManagement> rootNodes = allNodes.stream()
            .filter(node -> "0".equals(node.getPid()))
            .collect(Collectors.toList());

        int updateCount = 0;

        // 从根节点开始递归生成路径
        for (IetmProjectConfigurationManagement rootNode : rootNodes) {
            updateCount += batchGeneratePathsRecursive(rootNode, nodeMap, projectId);
        }

        log.info("批量生成路径完成，共更新 {} 个节点", updateCount);
        return updateCount;
    }

    /**
     * 递归生成并更新节点路径
     * @param node 当前节点
     * @param nodeMap 节点映射
     * @param projectId 项目ID
     * @return 更新的节点数量
     */
    private int batchGeneratePathsRecursive(IetmProjectConfigurationManagement node,
                                            Map<String, IetmProjectConfigurationManagement> nodeMap,
                                            String projectId) {
        int count = 0;

        // 生成当前节点的路径（包括根节点）
        String newPath = generatePath(node.getPid(), node.getCode(), projectId);

        // 只有路径发生变化时才更新
        if (newPath != null && !newPath.equals(node.getPath())) {
            node.setPath(newPath);
            this.updateById(node);
            count++;
            log.info("更新节点路径: id={}, code={}, pid={}, path={}", node.getId(), node.getCode(), node.getPid(), newPath);
        }

        // 递归处理子节点
        for (IetmProjectConfigurationManagement childNode : nodeMap.values()) {
            if (node.getId().equals(childNode.getPid())) {
                count += batchGeneratePathsRecursive(childNode, nodeMap, projectId);
            }
        }

        return count;
    }

    /**
     * 计算节点层级（通过递归查询父节点）
     * @param node 节点
     * @param nodeCache 节点缓存（用于批量查询时减少数据库查询）
     * @return 节点层级（0-7）
     */
    private int calculateNodeLevel(IetmProjectConfigurationManagement node, Map<String, IetmProjectConfigurationManagement> nodeCache) {
        if (node == null) {
            return 0;
        }

        // 如果是根节点（pid为0或null），返回0
        if (node.getPid() == null || "0".equals(node.getPid()) || node.getPid().trim().isEmpty()) {
            return 0;
        }

        // 先从缓存获取父节点
        IetmProjectConfigurationManagement parent = nodeCache.get(node.getPid());
        if (parent == null) {
            // 缓存未命中，从数据库查询
            parent = this.getById(node.getPid());
            if (parent == null) {
                log.warn("未找到父节点: nodeId={}, code={}, pid={}, 将其视为1级节点",
                        node.getId(), node.getCode(), node.getPid());
                return 1; // 如果找不到父节点，假设为1级
            }
            nodeCache.put(parent.getId(), parent);
        }

        // 递归计算：当前层级 = 父节点层级 + 1
        int parentLevel = calculateNodeLevel(parent, nodeCache);
        int currentLevel = parentLevel + 1;

        // 防止层级超出范围（0-7）
        if (currentLevel > 7) {
            log.warn("节点层级超出范围: nodeId={}, code={}, calculatedLevel={}, 限制为7",
                    node.getId(), node.getCode(), currentLevel);
            return 7;
        }

        return currentLevel;
    }

    /**
     * 为节点填充层级信息
     * @param node 节点
     */
    public void fillNodeLevel(IetmProjectConfigurationManagement node) {
        if (node != null) {
            Map<String, IetmProjectConfigurationManagement> nodeCache = new HashMap<>();
            int level = calculateNodeLevel(node, nodeCache);
            node.setLevel(level);
            log.debug("填充节点层级: id={}, code={}, pid={}, level={}",
                     node.getId(), node.getCode(), node.getPid(), level);
        }
    }

    /**
     * 为节点列表填充层级信息（批量优化版本）
     * @param nodes 节点列表
     */
    public void fillNodeLevels(List<IetmProjectConfigurationManagement> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 构建节点缓存，减少数据库查询
        Map<String, IetmProjectConfigurationManagement> nodeCache = new HashMap<>();
        for (IetmProjectConfigurationManagement node : nodes) {
            nodeCache.put(node.getId(), node);
        }

        // 迭代查询所有祖先节点，直到没有新的祖先需要查询
        Set<String> allQueriedIds = new HashSet<>();
        Set<String> currentLevelIds = new HashSet<>();

        // 第一轮：收集所有节点的直接父节点ID
        for (IetmProjectConfigurationManagement node : nodes) {
            if (node.getPid() != null && !"0".equals(node.getPid()) && !nodeCache.containsKey(node.getPid())) {
                currentLevelIds.add(node.getPid());
            }
        }

        // 迭代查询祖先节点，直到到达根节点
        int iteration = 0;
        while (!currentLevelIds.isEmpty() && iteration < 10) {  // 最多10级，防止无限循环
            iteration++;

            // 批量查询当前层级的节点
            List<String> idsToQuery = currentLevelIds.stream()
                    .filter(id -> !nodeCache.containsKey(id) && !allQueriedIds.contains(id))
                    .collect(Collectors.toList());

            if (!idsToQuery.isEmpty()) {
                log.debug("第{}轮查询祖先节点，数量: {}", iteration, idsToQuery.size());
                List<IetmProjectConfigurationManagement> ancestors = this.listByIds(idsToQuery);
                for (IetmProjectConfigurationManagement ancestor : ancestors) {
                    nodeCache.put(ancestor.getId(), ancestor);
                }
                allQueriedIds.addAll(idsToQuery);
            }

            // 准备下一轮：收集当前层级节点的父节点ID
            Set<String> nextLevelIds = new HashSet<>();
            for (String id : currentLevelIds) {
                IetmProjectConfigurationManagement node = nodeCache.get(id);
                if (node != null && node.getPid() != null && !"0".equals(node.getPid())) {
                    if (!nodeCache.containsKey(node.getPid()) && !allQueriedIds.contains(node.getPid())) {
                        nextLevelIds.add(node.getPid());
                    }
                }
            }

            currentLevelIds = nextLevelIds;
        }

        // 填充每个节点的层级
        for (IetmProjectConfigurationManagement node : nodes) {
            int level = calculateNodeLevel(node, nodeCache);
            node.setLevel(level);
            log.debug("填充节点层级: id={}, code={}, pid={}, level={}",
                     node.getId(), node.getCode(), node.getPid(), level);
        }
    }

    /**
     * 递归收集节点的所有祖先ID（已废弃，改用迭代方式）
     * @param node 当前节点
     * @param nodeCache 节点缓存
     * @param ancestorIds 收集的祖先ID集合
     */
    @Deprecated
    private void collectAncestorIds(IetmProjectConfigurationManagement node,
                                     Map<String, IetmProjectConfigurationManagement> nodeCache,
                                     Set<String> ancestorIds) {
        if (node == null || node.getPid() == null || "0".equals(node.getPid())) {
            return;
        }

        // 添加父节点ID
        ancestorIds.add(node.getPid());

        // 如果父节点在缓存中，继续向上递归收集
        IetmProjectConfigurationManagement parent = nodeCache.get(node.getPid());
        if (parent != null) {
            collectAncestorIds(parent, nodeCache, ancestorIds);
        }
    }

}
