package com.midea.cloud.srm.model.base.questionairesurvey.entity;

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
 *  问卷调查 模型
 * </pre>
*
* @author yancj9@meicloud.com
* @version 1.00.00
*
* <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: Apr 18, 2021 2:57:24 PM
 *  修改内容:
 * </pre>
*/

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scc_base_survey_scope_job")
public class SurveyScopeJob extends BaseEntity {
private static final long serialVersionUID = 697471L;
 /**
 * 岗位范围ID
 */
 @TableId("JOB_SCOPE_ID")
 private Long jobScopeId;
 /**
 * 员工范围ID：SCC_BASE_SURVEY_SCOPE_EMPLOYEE.SCOPE_ID
 */
 @TableField("SCOPE_ID")
 private Long scopeId;
 /**
 * 问卷ID；SCC_BASE_SURVEY_HEADER.SURVEY_ID
 */
 @TableField("SURVEY_ID")
 private Long surveyId;
 /**
 * 问题ID；SCC_BASE_SURVEY_QUESTION.QUESTION_ID
 */
 @TableField("QUESTION_ID")
 private Long questionId;
 /**
 * 岗位：字典 CATEGORY_CLASSIFICATION_POSITION
 */
 @TableField("JOB")
 private String job;
  /**
  * 创建人ID
  */
  @TableField(value ="CREATED_ID",fill = FieldFill.INSERT)
  private Long createdId;
 /**
 * 创建人
 */
 @TableField(value = "CREATED_BY",fill = FieldFill.INSERT)
 private String createdBy;
 /**
 * 创建时间
 */
 @TableField(value = "CREATION_DATE",fill = FieldFill.INSERT)
 private Date creationDate;
 /**
 * 创建人IP
 */
 @TableField(value ="CREATED_BY_IP" ,fill = FieldFill.INSERT)
 private String createdByIp;
 /**
 * 最后更新人ID
 */
 @TableField(value = "LAST_UPDATED_ID",fill = FieldFill.INSERT_UPDATE)
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