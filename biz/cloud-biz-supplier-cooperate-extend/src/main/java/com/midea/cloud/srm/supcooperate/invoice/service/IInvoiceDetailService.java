package com.midea.cloud.srm.supcooperate.invoice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.InvoiceDetail;

import java.util.List;

/**
*  <pre>
 *  开票明细表 服务类
 * </pre>
*
* @author chensl26@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-08-22 17:14:23
 *  修改内容:
 * </pre>
*/
public interface IInvoiceDetailService extends IService<InvoiceDetail> {

    void batchDeleteInvoiceDetails(List<Long> invoiceDetailIds);
}
