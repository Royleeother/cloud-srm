package com.midea.cloud.srm.model.inq.quote.dto;

import lombok.Data;

/**
 * <pre>
 *  功能名称
 * </pre>
 *
 * @author yourname@meicloud.com
 * @version 1.00.00
 * <p>
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-4-14 14:39
 *  修改内容:
 * </pre>
 */
@Data
public class QuoteRollbackDTO {

    /**
     * 报价id
     */
    private Long quoteId;

    /**
     * 撤回说明
     */
    private String rejectReason;
}