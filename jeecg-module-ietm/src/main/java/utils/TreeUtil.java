package utils;

import org.hibernate.validator.internal.util.StringHelper;
import org.jeecg.common.system.vo.TreeModel;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author: zxk
 * @description: list转为tree的工具类
 * @date: 2026/1/31 15:44
 * @return:
 * @version: 1.0
 */
public class TreeUtil {
    /**
     * @param dtoList    实体列表
     * @param titleField a-tree要展示的字段
     * @param <T>
     * @return
     */
    public static <T> List<TreeModel> converListToTree(List<T> dtoList, String titleField, String rootPid) {
        if (dtoList == null || CollectionUtils.isEmpty(dtoList)) {
            return Collections.emptyList();
        }
        HashMap<String, TreeModel> voMap = new LinkedHashMap();
        List<TreeModel> rootNodes = new ArrayList<>();
        try {
            //
            for (T dto : dtoList) {
                TreeModel treeModel = buildTreeModel(dto, titleField);
                voMap.put(treeModel.getKey(), treeModel);
            }
            //组装子节点
            for (String id : voMap.keySet()) {
                TreeModel node = voMap.get(id);
                String pid = node.getPid();
                //1.如果是根节点
                if (StringHelper.isNullOrEmptyString(pid) || rootPid.equals(pid)) {
                    rootNodes.add(node);
                    continue;
                }
                //2.非根节点
                TreeModel parentNode = voMap.get(pid);
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rootNodes;
    }

    private static <T> TreeModel buildTreeModel(T t, String titleField) throws Exception {
        TreeModel treeModel = new TreeModel();
        String id = t.getClass().getMethod("getId").invoke(t).toString();
        String pid = t.getClass().getMethod("getPid").invoke(t).toString();
        String title = t.getClass().getMethod("get" + StringUtils.capitalize(titleField)).invoke(t).toString();
        treeModel.setKey(id);
        treeModel.setPid(pid);
        treeModel.setTitle(title);
        treeModel.setChildren(new ArrayList<>());
        return treeModel;
    }
}
