package com.midea.cloud.srm.model.logistics.bargaining.purchaser.bidprocessconfig.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import com.midea.cloud.srm.model.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
*  <pre>
 *  招标流程配置表 模型
 * </pre>
*
* @author fengdc3@meicloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-03-16 15:01:13
 *  修改内容:
 * </pre>
*/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scc_brg_process_config")
public class BidProcessConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 招标流程配置ID
     */
    @TableId("PROCESS_CONFIG_ID")
    private Long processConfigId;

    /**
     * 招标流程配置编码
     */
    @TableField("PROCESS_CONFIG_CODE")
    private String processConfigCode;

    /**
     * 招标流程配置名称
     */
    @TableField("PROCESS_CONFIG_NAME")
    private String processConfigName;

    /**
     * 招标范围
     */
    @TableField("BIDING_SCOPE")
    private String bidingScope;

    /**
     * 招标类型
     * 字典编码:BID_TYPE
     * BUSINESS:商务    TECHNOLOGY_BUSINESS:技术+商务
     */
    @TableField("BIDING_TYPE")
    private String bidingType;

    /**
     * 评选方式
     * 字典编码:BID_GRADING
     * LOWER_PRICE:合理低价法    HIGH_PRICE:合理高价法
     * COMPOSITE_SCORE:综合评分法
     */
    @TableField("EVALUATE_METHOD")
    private String evaluateMethod;

    /**
     * 技术交流 Y:启用,N:不启用
     */
    @TableField("TECHNOLOGY_EXCHANGE")
    private String technologyExchange;

    /**
     * 项目信息 Y:启用,N:不启用
     */
    @TableField("PROJECT_INFORMATION")
    private String projectInformation;

    /**
     * 项目需求 Y:启用,N:不启用
     */
    @TableField("PROJECT_REQUIREMENT")
    private String projectRequirement;

    /**
     * 邀请供应商 Y:启用,N:不启用
     */
    @TableField("INVITE_SUPPLIER")
    private String inviteSupplier;

    /**
     * 评分规则 Y:启用,N:不启用
     */
    @TableField("SCORING_RULE")
    private String scoringRule;

    /**
     * 流程审批 Y:启用,N:不启用
     */
    @TableField("PROCESS_APPROVAL")
    private String processApproval;

    /**
     * 供应商绩效 Y:启用,N:不启用
     */
    @TableField("SUPPLIER_PERFORMANCE")
    private String supplierPerformance;

    /**
     * 拦标价 Y:启用,N:不启用
     */
    @TableField("TARGET_PRICE")
    private String targetPrice;

    /**
     * 项目发布 Y:启用,N:不启用
     */
    @TableField("PROJECT_PUBLISH")
    private String projectPublish;

    /**
     * 报名管理 Y:启用,N:不启用
     */
    @TableField("ENTRY_MANAGEMENT")
    private String entryManagement;

    /**
     * 质疑澄清 Y:启用,N:不启用
     */
    @TableField("QUESTION_CLARIFICATION")
    private String questionClarification;

    /**
     * 投标控制 Y:启用,N:不启用
     */
    @TableField("BIDING_CONTROL")
    private String bidingControl;

    /**
     * 技术评分 Y:启用,N:不启用
     */
    @TableField("TECHNICAL_SCORE")
    private String technicalScore;

    /**
     * 技术标管理 Y:启用,N:不启用
     */
    @TableField("TECHNICAL_MANAGEMENT")
    private String technicalManagement;

    /**
     * 商务标管理 Y:启用,N:不启用
     */
    @TableField("COMMERCIAL_MANAGEMENT")
    private String commercialManagement;

    /**
     * 评选 Y:启用,N:不启用
     */
    @TableField("BID_EVALUATION")
    private String bidEvaluation;

    /**
     * 结项报告 Y:启用,N:不启用
     */
    @TableField("PROJECT_REPORT")
    private String projectReport;

    /**
     * 结项审批 Y:启用,N:不启用
     */
    @TableField("PROJECT_APPROVAL")
    private String projectApproval;

    /**
     * 招标结果 Y:启用,N:不启用
     */
    @TableField("BIDING_RESULT")
    private String bidingResult;


    /**
     * 是否有效  Y:有效  N:无效
     */
    @TableField("ENABLE_FLAG")
    private String enableFlag;

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
     * 招标流程配置简述/备注
     */
    @TableField("COMMENTS")
    private String comments;

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
     * 拥有者/租户/所属组等
     */
    @TableField("TENANT_ID")
    private Long tenantId;

    /**
     * 版本号
     */
    @TableField("VERSION")
    private Long version;

}
