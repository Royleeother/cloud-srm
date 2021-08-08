package com.midea.cloud.srm.model.supplier.demotion.entity;

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

/**
*  <pre>
 *  供应商升降级附件行表 模型
 * </pre>
*
* @author xiexh12@meicloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2021-01-05 17:51:16
 *  修改内容:
 * </pre>
*/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("ceea_sup_demotion_file")
public class DemotionFile extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 附件ID
     */
    @TableId("ATTACH_FILE_ID")
    private Long attachFileId;

    /**
     * 升降级主键ID
     */
    @TableField("COMPANY_DEMOTION_ID")
    private Long companyDemotionId;

    /**
     * 附件名称
     */
    @TableField("ATTACHMENT_NAME")
    private String attachmentName;

    /**
     * 附件描述
     */
    @TableField("ATTACHMENT_DISCRIPTION")
    private String attachmentDiscription;

    /**
     * 附件名称（上传到fastdf的文件名）
     */
    @TableField("ATTACHMENT_PIC")
    private String attachmentPic;

    /**
     * 附件FileId
     */
    @TableField("ATTACHMENT_PIC_FILE_ID")
    private Long attachmentPicFileId;

    /**
     * 附件有效期
     */
    @TableField("ATTACHMENT_VALID_DATE")
    private LocalDate attachmentValidDate;

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
    @TableField(value = "LAST_UPDATED_ID", fill = FieldFill.INSERT_UPDATE)
    private Long lastUpdatedId;

    /**
     * 最后更新人
     */
    @TableField(value = "LAST_UPDATED_BY", fill = FieldFill.INSERT_UPDATE)
    private String lastUpdatedBy;

    /**
     * 最后更新时间
     */
    @TableField(value = "LAST_UPDATE_DATE", fill = FieldFill.INSERT_UPDATE)
    private Date lastUpdateDate;

    /**
     * 最后更新人IP
     */
    @TableField(value = "LAST_UPDATED_BY_IP", fill = FieldFill.INSERT_UPDATE)
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


}
