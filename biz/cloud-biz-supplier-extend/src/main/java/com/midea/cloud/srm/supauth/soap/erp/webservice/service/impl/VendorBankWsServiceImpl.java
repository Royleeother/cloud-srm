package com.midea.cloud.srm.supauth.soap.erp.webservice.service.impl;

import com.midea.cloud.srm.model.base.soap.erp.base.dto.EsbInfoRequest;
import com.midea.cloud.srm.model.supplierauth.soap.erp.dto.VendorBankResponse;
import com.midea.cloud.srm.model.supplierauth.soap.erp.dto.VendorContactResponse;
import com.midea.cloud.srm.model.supplierauth.soap.erp.dto.VendorResquest;
import com.midea.cloud.srm.supauth.soap.erp.service.IErpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jws.WebService;

/**
 * <pre>
 *  功能名称
 * </pre>
 *
 * @author xiexh12@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/9/3 10:44
 *  修改内容:
 * </pre>
 */
@Slf4j
@WebService(targetNamespace = "http://www.aurora-framework.org/schema",
        endpointInterface = "com.midea.cloud.srm.supauth.soap.erp.webservice.service.IVendorBankWsService")
@Component("iVendorBankWsService")
public class VendorBankWsServiceImpl {
    /**erp总线Service*/
    @Resource
    private IErpService iErpService;

    public VendorBankResponse execute(VendorResquest request) {
        VendorBankResponse response = new VendorBankResponse();
        /**获取instId和requestTime*/
        EsbInfoRequest esbInfo = request.getEsbInfo();
        String instId = "";
        String requestTime = "";
        if (null != esbInfo) {
            instId = esbInfo.getInstId();
            requestTime = esbInfo.getRequestTime();
        }
        response = iErpService.getVendorBanks(instId, requestTime);
        return response;
    }
}
