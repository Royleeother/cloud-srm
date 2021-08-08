package com.midea.cloud.srm.sup.soap.erp.vendor.service;

import com.midea.cloud.srm.model.supplier.info.entity.ContactInfo;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorContact.VendorContactInputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorContact.VendorContactOutputParameters;

/**
 * <pre>
 *  erp的供应商联系人信息Service接口
 * </pre>
 *
 * @author xiexh12@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/9/4
 *  修改内容:
 * </pre>
 */

public interface IVendorContactService {

    /**
     * Description 调用erp保存供应商基本信息
     *
     * @return
     * @Param vendorContact
     * @Author xiexh12@meicloud.com
     * @Date 2020.09.04
     **/
    VendorContactOutputParameters sendVendorContact(ContactInfo contactInfo);
}
