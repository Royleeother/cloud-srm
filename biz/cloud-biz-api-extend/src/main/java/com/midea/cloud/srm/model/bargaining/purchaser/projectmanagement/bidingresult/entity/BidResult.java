package com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidingresult.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.midea.cloud.srm.model.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
*  <pre>
 *  招标结果表 模型
 * </pre>
*
* @author fengdc3@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-04-16 21:21:21
 *  修改内容:
 * </pre>
*/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scc_brg_result")
    public class BidResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId("BID_RESULT_ID")
    private Long bidResultId;

    /**
     * 招标id
     */
    @TableField("BIDING_ID")
    private Long bidingId;

    /**
     * 采购组织ID
     */
    @TableField("ORG_ID")
    private Long orgId;

    /**
     * 组织全路径虚拟ID
     */
    @TableField("FULL_PATH_ID")
    private String fullPathId;

    /**
     * 采购组织编码
     */
    @TableField("ORG_CODE")
    private String orgCode;

    /**
     * 采购组织名称
     */
    @TableField("ORG_NAME")
    private String orgName;

    /**
     * 供应商ID
     */
    @TableField("VENDOR_ID")
    private Long vendorId;

    /**
     * 供应商编号
     */
    @TableField("VENDOR_CODE")
    private String vendorCode;

    /**
     * 供应商名称
     */
    @TableField("VENDOR_NAME")
    private String vendorName;

    /**
     * 物料组(组合)
     */
    @TableField("ITEM_GROUP")
    private String itemGroup;

    /**
     * 标的编号
     */
    @TableField("TARGET_NUM")
    private String targetNum;

    /**
     * 标的描述
     */
    @TableField("TARGET_DESC")
    private String targetDesc;

    /**
     * 含税现价
     */
    @TableField("TAX_CURRENT_PRICE")
    private BigDecimal taxCurrentPrice;

    /**
     * 评选结果
     */
    @TableField("SELECTION_STATUS")
    private String selectionStatus;

    /**
     * 含税中标价格
     */
    @TableField("BID_PRICE_INCLUDING_TAX")
    private BigDecimal bidPriceIncludingTax;

    /**
     * 不含税中标价格
     */
    @TableField("BID_PRICE_EXCLUDING_TAX")
    private BigDecimal bidPriceExcludingTax;

    /**
     * 招标比例配额
     */
    @TableField("BIDING_PROPORTION_QUOTA")
    private BigDecimal bidingProportionQuota;

    /**
     * 招标结果备注
     */
    @TableField("COMMENTS")
    private String comments;

    /**
     * 定价开始时间
     */
    @TableField("PRICE_START_TIME")
    private Date priceStartTime;

    /**
     * 定价结束时间
     */
    @TableField("PRICE_END_TIME")
    private Date priceEndTime;

    /**
     * 税率编码
     */
    @TableField("TAX_KEY")
    private String taxKey;

    /**
     * 税率
     */
    @TableField("TAX_RATE")
    private BigDecimal taxRate;

    /**
     * 采购分类ID
     */
    @TableField("CATEGORY_ID")
    private Long categoryId;

    /**
     * 采购分类编码
     */
    @TableField("CATEGORY_CODE")
    private String categoryCode;

    /**
     * 采购分类名称
     */
    @TableField("PURCHASE_CATEGORY")
    private String purchaseCategory;

    /**
     * 行类型
     */
    @TableField("ROW_TYPE")
    private String rowType;

    /**
     * 企业编码
     */
    @TableField("COMPANY_CODE")
    private String companyCode;

    /**
     * 组织编码
     */
    @TableField("ORGANIZATION_CODE")
    private String organizationCode;

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
     * 更新人
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
    private String tenantId;

    /**
     * 版本号
     */
    @TableField("VERSION")
    private Long version;
    /**
     * 排名
     */
    @TableField("CEEA_RANK")
    private Integer rank;

}
