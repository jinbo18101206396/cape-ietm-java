package org.jeecg.modules.ietm.ietmroleauth.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmRoleauth;

/**
 * @Description: 手册授权管理VO
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IetmRoleauthVO extends IetmRoleauth {

    /**角色名称*/
    private String roleName;

    /**项目名称*/
    private String projectName;

    /**对象名称*/
    private String objName;
}
