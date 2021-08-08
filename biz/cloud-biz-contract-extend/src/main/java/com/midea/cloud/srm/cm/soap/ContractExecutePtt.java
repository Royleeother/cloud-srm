
package com.midea.cloud.srm.cm.soap;


import com.midea.cloud.srm.model.cm.contract.soap.ObjectFactory;
import com.midea.cloud.srm.model.cm.contract.soap.Request;
import com.midea.cloud.srm.model.cm.contract.soap.Response;

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
@WebService(name = "execute_ptt", targetNamespace = "http://www.longi.com/CMSSB/Esb/OeBlanket/WSDLs/EsbPublishOeBlanketSoapProxy/v1.0")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
    ObjectFactory.class
})
public interface ContractExecutePtt {

    /**
     *
     * @param input
     * @return
     *     returns com.longi.cmssb.esb.oeblanket.schemas.oeblanket.v1.Response
     */
    @WebMethod(action = "execute")
    @WebResult(name = "response", targetNamespace = "http://www.longi.com/CMSSB/Esb/OeBlanket/Schemas/oeBlanket/v1.0", partName = "output")
    public Response execute(
            @WebParam(name = "request", targetNamespace = "http://www.longi.com/CMSSB/Esb/OeBlanket/Schemas/oeBlanket/v1.0", partName = "input")
                    Request input);
}
