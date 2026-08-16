package org.jeecg.modules.ietm.ietmdatamodulemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/** IETM元素/属性中英文对照表（对标 legacy IETM_TRANSLATE） */
@Data
@Accessors(chain = true)
@TableName("ietm_translate")
public class IetmTranslate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String transType;
    private String ietmStandard;
    private String enName;
    private String cnName;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String sysOrgCode;
}
