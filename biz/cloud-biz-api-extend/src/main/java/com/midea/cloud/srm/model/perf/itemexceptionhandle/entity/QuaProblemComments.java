package com.midea.cloud.srm.model.perf.itemexceptionhandle.entity;

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
 *  来料异常处理单 模型
 * </pre>
*
* @author chenjw90@meicloud.com
* @version 1.00.00
*
* <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: Jan 27, 2021 7:51:28 PM
 *  修改内容:
 * </pre>
*/

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("scc_perf_qua_problem_comments")
public class QuaProblemComments extends BaseEntity {
private static final long serialVersionUID = 631290L;
 /**
 * 问题备注行表ID
 */
 @TableId("PROBLEM_COMMENTS_LINE_ID")
 private Long problemCommentsLineId;
 /**
  * 头行表异常单号ID
  */
 @TableField("ITEM_EXCEPTION_HEAD_ID")
 private Long itemExceptionHeadId;
 /**
 * 判定结论
 */
 @TableField("JUDGE_CONCLUSION")
 private String judgeConclusion;
 /**
 * 判定人
 */
 @TableField("JUDGE_NAME")
 private String judgeName;
 /**
 * 判定日期
 */
 @TableField("JUDGE_DATE")
 private Date judgeDate;
 /**
 * 创建人ID
 */
 @TableField(value = "CREATED_ID",fill = FieldFill.INSERT)
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
 @TableField(value ="CREATED_BY_IP" , fill = FieldFill.INSERT)
 private String createdByIp;
 /**
 * 最后更新人ID
 */
 @TableField(value = "LAST_UPDATED_ID",fill = FieldFill.UPDATE)
 private Long lastUpdatedId;
 /**
 * 最后更新人
 */
 @TableField(value = "LAST_UPDATED_BY", fill = FieldFill.UPDATE)
 private String lastUpdatedBy;
 /**
 * 最后更新时间
 */
 @TableField(value = "LAST_UPDATED_DATE", fill = FieldFill.UPDATE)
 private Date lastUpdatedDate;
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
}