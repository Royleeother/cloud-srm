package com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.entry;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.midea.cloud.srm.model.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * <pre>
 *  预约送货单明细表 模型
 * </pre>
 *
 * @author huangbf3
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/3/27 14:45
 *  修改内容:
 * </pre>
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scc_sc_appoint_delivery_note")
public class AppointDeliveryNote extends BaseEntity {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId("APPOINT_DELIVERY_NOTE_ID")
    private Long appointDeliveryNoteId;

    /**
     * 送货预约ID
     */
    @TableField("DELIVERY_APPOINT_ID")
    private Long deliveryAppointId;

    /**
     * 送货单ID
     */
    @TableField("DELIVERY_NOTE_ID")
    private Long deliveryNoteId;


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
     * 版本号
     */
    @TableField("VERSION")
    private Long version;

    /**
     * 租户ID
     */
    @TableField("TENANT_ID")
    private Long tenantId;

    /**
     * 送货单号
     */
    @TableField(exist = false)
    private String deliveryNumber;

    /**
     * 送货日期
     */
    @TableField(exist = false)
    private Date deliveryDate;
}
