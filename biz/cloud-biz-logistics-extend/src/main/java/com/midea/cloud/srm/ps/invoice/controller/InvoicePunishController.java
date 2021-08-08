package com.midea.cloud.srm.ps.invoice.controller;

import com.midea.cloud.srm.feign.supcooperate.SupcooperateClient;
import com.midea.cloud.srm.model.common.BaseController;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
*  <pre>
 *  扣罚&派利明细 前端控制器
 * </pre>
*
* @author chensl26@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-08-22 17:39:56
 *  修改内容:
 * </pre>
*/
@RestController
@RequestMapping("/invoice/invoicePunish")
public class InvoicePunishController extends BaseController {

    @Autowired
    private SupcooperateClient supcooperateClient;

    /**
     * 批量删除
     * @param invoicePunishIds
     */
    @PostMapping("/batchDelete")
    public void bachDeleteInvoicePunishes(@RequestBody List<Long> invoicePunishIds) {
        if (CollectionUtils.isNotEmpty(invoicePunishIds)) {
            supcooperateClient.bachDeleteInvoicePunishes(invoicePunishIds);
        }
    }

}
