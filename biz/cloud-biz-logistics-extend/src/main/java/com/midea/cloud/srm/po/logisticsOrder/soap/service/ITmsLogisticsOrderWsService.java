
package com.midea.cloud.srm.po.logisticsOrder.soap.service;

import com.midea.cloud.srm.model.base.soap.erp.dto.ObjectFactory;
import com.midea.cloud.srm.model.base.soap.erp.dto.SoapResponse;
import com.midea.cloud.srm.model.logistics.soap.tms.request.LogisticsOrderRequest;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 *
 */
@WebService(name = "iTmsLogisticsOrderWsService", targetNamespace = "http://www.aurora-framework.org/schema")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
    ObjectFactory.class
})
public interface ITmsLogisticsOrderWsService {


    /**
     *
     * @param tmsLogisticsOrderRequestPart
     * @return
     *     returns org.aurora_framework.schema.SoapResponse
     */
    @WebMethod(action = "execute")
    @WebResult(name = "soapResponse", targetNamespace = "http://www.aurora-framework.org/schema", partName = "tmsLogisticsOrderResponse_part")
    public SoapResponse execute(
            @WebParam(name = "logisticsOrderRequest", targetNamespace = "http://www.aurora-framework.org/schema", partName = "tmsLogisticsOrderRequest_part")
                    LogisticsOrderRequest tmsLogisticsOrderRequestPart);

}
