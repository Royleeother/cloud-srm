
package com.midea.cloud.srm.cm.accept.soap.assetsreimbursement;

import com.midea.cloud.srm.model.cm.accept.soap.AssetsReimbursement;
import com.midea.cloud.srm.model.cm.accept.soap.BusinessFormRequest;
import com.midea.cloud.srm.model.cm.accept.soap.BusinessFormResponse;

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
@WebService(name = "BusinessFormInfoService", targetNamespace = "http://soap.f10.vispractice.com")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
        AssetsReimbursement.class
})
public interface BusinessFormInfoService {


    /**
     * 
     * @param businessFormRequest
     * @return
     *     returns com.vispractice.f10.soap.BusinessFormResponse
     */
    @WebMethod(action = "businessFormInfo")
    @WebResult(name = "businessFormResponse", targetNamespace = "http://soap.f10.vispractice.com", partName = "businessFormResponse")
    public BusinessFormResponse form(
            @WebParam(name = "businessFormRequest", targetNamespace = "http://soap.f10.vispractice.com", partName = "businessFormRequest")
                    BusinessFormRequest businessFormRequest);

}
