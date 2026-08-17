package org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmRef;

import java.util.List;
import java.util.Map;

/**
 * @Description: IETM数据模块引用关系Mapper
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 * @Version: V1.0
 */
public interface IetmDmRefMapper extends BaseMapper<IetmDmRef> {

    /**
     * 查询出引用列表（当前DM引用了哪些DM）
     * @param sourceDmId 源DM ID
     * @return 引用列表
     */
    List<Map<String, Object>> selectOutReferences(@Param("sourceDmId") String sourceDmId);

    /**
     * 查询入引用列表（哪些DM引用了当前DM）
     * @param targetDmId 目标DM ID
     * @return 引用列表
     */
    List<Map<String, Object>> selectInReferences(@Param("targetDmId") String targetDmId);

    /**
     * 统计入引用数量（被引用次数）
     * @param targetDmId 目标DM ID
     * @return 引用次数
     */
    Integer countInReferences(@Param("targetDmId") String targetDmId);

    /**
     * 统计出引用数量（引用了多少个其他DM）
     * @param sourceDmId 源DM ID
     * @return 引用次数
     */
    Integer countOutReferences(@Param("sourceDmId") String sourceDmId);

    /**
     * 删除DM的所有引用关系（source 和 target 两侧，用于DM删除场景）
     * @param dmId DM ID
     * @return 删除数量
     */
    Integer deleteByDmId(@Param("dmId") String dmId);

    /**
     * 仅删除指定DM作为出引用方的关系（calcref 专用，不影响其他DM指向本DM的入引用记录）
     * @param sourceDmId 引用方 DM ID
     * @return 删除数量
     */
    Integer deleteBySourceDmId(@Param("sourceDmId") String sourceDmId);

    /**
     * 批量插入引用关系（逐条插入，避免SQL解析器问题）
     * @param refs 引用关系列表
     * @return 插入数量
     */
    Integer batchInsert(@Param("refs") List<IetmDmRef> refs);

    /**
     * 单条插入引用关系
     * @param ref 引用关系
     * @return 插入数量
     */
    Integer insertOne(IetmDmRef ref);

    /**
     * 查询从根DM到目标DM的引用链路径
     * @param rootDmId 根DM ID
     * @param targetDmId 目标DM ID
     * @param refType 引用类型：out-出引用（从root引用到target），in-入引用（从target被引用到root）
     * @return 引用链路径（有序列表，从根到目标）
     */
    List<Map<String, Object>> selectReferenceChain(@Param("rootDmId") String rootDmId, @Param("targetDmId") String targetDmId, @Param("refType") String refType);
}
