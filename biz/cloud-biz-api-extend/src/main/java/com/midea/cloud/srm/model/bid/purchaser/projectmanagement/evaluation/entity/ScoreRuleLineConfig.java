package com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;
import com.midea.cloud.srm.model.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
*  <pre>
 *  招标评分规则明细模板表 模型
 * </pre>
*
* @author fengdc3@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-03-29 09:39:07
 *  修改内容:
 * </pre>
*/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scc_bid_score_rule_line_config")
public class ScoreRuleLineConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID 评分规则明细模板ID
     */
    @TableId("RULE_LINE_CONFIG_ID")
    private Long ruleLineConfigId;

    /**
     * 招标评分规则模板ID
     */
    @TableField("RULE_CONFIG_ID")
    private Long ruleConfigId;

    /**
     * 评分维度
     */
    @TableField("SCORE_DIMENSION")
    private String scoreDimension;

    /**
     * 评分项
     */
    @TableField("SCORE_ITEM")
    private String scoreItem;

    /**
     * 评分标准
     */
    @TableField("SCORE_STANDARD")
    private String scoreStandard;

    /**
     * 取值来源
     */
    @TableField("SCORE_SOURCE")
    private String scoreSource;

    /**
     * 评分权重
     */
    @TableField("SCORE_WEIGHT")
    private Integer scoreWeight;

    /**
     * 满分值
     */
    @TableField("FULL_SCORE")
    private BigDecimal fullScore;

    /**
     * 备注
     */
    @TableField("COMMENTS")
    private String comments;

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
     * 拥有者/租户/所属组等
     */
    @TableField("TENANT_ID")
    private String tenantId;

    /**
     * 版本号
     */
    @TableField("VERSION")
    private Long version;


}
