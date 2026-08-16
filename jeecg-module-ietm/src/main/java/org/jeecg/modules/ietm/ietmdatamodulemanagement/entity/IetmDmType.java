package org.jeecg.modules.ietm.ietmdatamodulemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/** DM类型与XSD Schema映射表（对标 legacy IETM_DMTYPE） */
@Data
@Accessors(chain = true)
@TableName("ietm_dm_type")
public class IetmDmType implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String typeCode;
    private String typeName;
    private String typeNameEn;
    private String ietmStandard;
    private String xsdFile;
    private String contentRootElem;
    private String templateFile;
    private Integer sortNo;
    private String status;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String sysOrgCode;
}
