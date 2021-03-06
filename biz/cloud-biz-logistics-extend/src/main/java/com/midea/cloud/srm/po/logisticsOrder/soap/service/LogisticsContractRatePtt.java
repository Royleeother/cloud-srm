package com.midea.cloud.srm.po.logisticsOrder.soap.service;

import com.midea.cloud.srm.model.logistics.soap.order.entity.GetSrmTariffInfo;
import com.midea.cloud.srm.model.logistics.soap.order.entity.GetSrmTariffInfoResponse;
import com.midea.cloud.srm.model.logistics.soap.order.entity.ObjectFactory;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 2.2.4
 * Tue Jan 19 11:39:56 CST 2021
 * Generated source version: 2.2.4
 *
 */

@WebService(targetNamespace = "http://www.longi.com/TMSSB/Srm/LogisticsContractRate/WSDLs/v1.0", name = "LogisticsContractRate_ptt")
@XmlSeeAlso({ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface LogisticsContractRatePtt {

    @WebMethod(operationName = "LogisticsContractRate", action = "LogisticsContractRate")
    @WebResult(name = "getSrmTariffInfoResponse", targetNamespace = "http://www.longi.com/TMSSB/Srm/LogisticsContractRate/Schemas/v1.0", partName = "output")
    public GetSrmTariffInfoResponse logisticsContractRate(
            @WebParam(partName = "input", name = "getSrmTariffInfo", targetNamespace = "http://www.longi.com/TMSSB/Srm/LogisticsContractRate/Schemas/v1.0")
                    GetSrmTariffInfo input
    );
}
