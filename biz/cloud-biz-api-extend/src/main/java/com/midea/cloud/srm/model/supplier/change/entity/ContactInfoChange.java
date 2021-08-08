package com.midea.cloud.srm.model.supplier.change.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import java.util.Map;

import com.midea.cloud.srm.model.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 *  <pre>
 *  联系人信息变更表 模型
 * </pre>
 *
 * @author chensl26@meiCloud.com
 * @version 1.00.00
 *
 *  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-03-27 11:05:56
 *  修改内容:
 * </pre>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scc_sup_contact_info_change")
public class ContactInfoChange extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 合同变更表ID
     */
    @TableId("CONTACT_CHANGE_ID")
    private Long contactChangeId;

    /**
     * 变更ID
     */
    @TableField("CHANGE_ID")
    private Long changeId;

    /**
     * ID
     */
    @TableField("CONTACT_INFO_ID")
    private Long contactInfoId;

    /**
     * 供应商ID
     */
    @TableField("COMPANY_ID")
    private Long companyId;

    /**
     * 性别
     */
    @TableField("CEEA_GENDER")
    private String ceeaGender;

    /**
     * 部门ID
     */
    @TableField("CEEA_DEPT_ID")
    private Long  ceeaDeptId;

    /**
     * 部门名称
     */
    @TableField("CEEA_DEPT_NAME")
    private String  ceeaDeptName;

    /**
     * 联系方式
     */
    @TableField("CEEA_CONTACT_METHOD")
    private String  ceeaContactMethod;

    /**
     * 默认联系人
     */
    @TableField("CEEA_DEFAULT_CONTACT")
    private String  ceeaDefaultContact;

    /**
     * 备注
     */
    @TableField("CEEA_COMMENTS")
    private String  ceeaComments;

    /**
     * 联系人姓名
     */
    @TableField("CONTACT_NAME")
    private String contactName;

    /**
     * 手机号码
     */
    @TableField("MOBILE_NUMBER")
    private String mobileNumber;

    /**
     * 座机号码
     */
    @TableField("PHONE_NUMBER")
    private String phoneNumber;

    /**
     * 邮箱
     */
    @TableField("EMAIL")
    private String email;

    /**
     * 联系人地址
     */
    @TableField("CONTACT_ADDRESS")
    private String contactAddress;

    /**
     * 人员职务
     */
    @TableField("POSITION")
    private String position;

    /**
     * 传真号码
     */
    @TableField("TAX_NUMBER")
    private String taxNumber;

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

    /**
     * 租户ID
     */
    @TableField("TENANT_ID")
    private Long tenantId;

    /**
     * 版本号
     */
    @TableField("VERSION")
    private Long version;

    @TableField(exist = false)
    Map<String,Object> dimFieldContexts;

    @TableField("OP_TYPE")
    private String opType;


}
