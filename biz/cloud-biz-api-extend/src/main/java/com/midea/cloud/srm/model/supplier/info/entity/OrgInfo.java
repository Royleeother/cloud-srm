package com.midea.cloud.srm.model.supplier.info.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.midea.cloud.srm.model.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

/**
*  <pre>
 *  合作组织信息 模型
 * </pre>
*
* @author zhuwl7@meicloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-03-02 16:21:46
 *  修改内容:
 * </pre>
*/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scc_sup_org_info")
public class OrgInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId("ORG_INFO_ID")
    private Long orgInfoId;

    /**
     * 租户ID
     */
    @TableField("TENANT_ID")
    private Long tenantId;

    /**
     * 供应商ID
     */
    @TableId("COMPANY_ID")
    private Long companyId;

    /**
     * 合作组织ID
     */
    @TableField("ORG_ID")
    private Long orgId;

    /**
     * 事业部名称
     */
    @TableField("BU_NAME")
    private String buName;

    /**
     * 事业部编码
     */
    @TableField("BU_CODE")
    private String buCode;

    /**
     * 组织全路径虚拟ID
     */
    @TableField("FULL_PATH_ID")
    private String fullPathId;

    /**
     * 合作组织
     */
    @TableField("ORG_NAME")
    private String orgName;

    /**
     * 合作组织
     */
    @TableField("ORG_CODE")
    private String orgCode;

    /**
     * 父类合作组织
     */
    @TableField("PARENT_ORG_ID")
    private Long parentOrgId;

    /**
     * 父类合作组织
     */
    @TableField("PARENT_ORG_NAME")
    private String parentOrgName;

    /**
     * 父类合作组织
     */
    @TableField("PARENT_ORG_CODE")
    private String parentOrgCode;

    /**
     * 组织服务状态
     */
    @TableField("SERVICE_STATUS")
    private String serviceStatus;

    /**
     * 生效时间
     */
    @TableField("START_DATE")
    private LocalDate startDate;

    /**
     * 失效时间
     */
    @TableField("END_DATE")
    private LocalDate endDate;

    /**
     * 版本号
     */
    @TableField("VERSION")
    private Long version;

    /**
     * 创建人ID
     */
    @TableField(value = "CREATED_ID", fill = FieldFill.INSERT)
    private Long createdId;

    /**
     * 创建人
     */
    @TableField(value = "CREATED_BY", fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "CREATION_DATE", fill = FieldFill.INSERT)
    private Date creationDate;

    /**
     * 创建人IP
     */
    @TableField(value = "CREATED_BY_IP", fill = FieldFill.INSERT)
    private String createdByIp;

    /**
     * 最后更新人ID
     */
    @TableField(value = "LAST_UPDATED_ID", fill = FieldFill.UPDATE)
    private Long lastUpdatedId;

    /**
     * 最后更新人
     */
    @TableField(value = "LAST_UPDATED_BY", fill = FieldFill.UPDATE)
    private String lastUpdatedBy;

    /**
     * 最后更新时间
     */
    @TableField(value = "LAST_UPDATE_DATE", fill = FieldFill.INSERT_UPDATE)
    private Date lastUpdateDate;

    /**
     * 最后更新人IP
     */
    @TableField(value = "LAST_UPDATED_BY_IP", fill = FieldFill.UPDATE)
    private String lastUpdatedByIp;

    @TableField(exist = false)
    Map<String,Object> dimFieldContexts;

    @TableField(exist = false)
    private String Username;

    @TableField(exist = false)
    private String enabled;
}
