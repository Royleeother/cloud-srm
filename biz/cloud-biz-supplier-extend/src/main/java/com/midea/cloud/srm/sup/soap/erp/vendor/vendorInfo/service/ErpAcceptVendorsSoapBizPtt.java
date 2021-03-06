
package com.midea.cloud.srm.sup.soap.erp.vendor.vendorInfo.service;

import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorInfo.VendorInfoInputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorInfo.VendorInfoObjectFactory;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorInfo.VendorInfoOutputParameters;

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
@WebService(name = "ErpAcceptVendorsSoapBiz_ptt", targetNamespace = "http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/MdmSB/ErpAcceptVendorsSoapBiz")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
    VendorInfoObjectFactory.class
})
public interface ErpAcceptVendorsSoapBizPtt {


    /**
     * 
     * @param vendorInfoInputParameters
     * @return
     *     returns com.vendor.OutputParameters
     */
    @WebMethod(operationName = "ErpAcceptVendorsSoapBiz", action = "ErpAcceptVendorsSoapBiz")
    @WebResult(name = "OutputParameters", targetNamespace = "http://xmlns.oracle.com/pcbpel/adapter/db/sp/ErpAcceptVendorsSoapBiz", partName = "OutputParameters")
    public VendorInfoOutputParameters erpAcceptVendorsSoapBiz(
            @WebParam(name = "InputParameters", targetNamespace = "http://xmlns.oracle.com/pcbpel/adapter/db/sp/ErpAcceptVendorsSoapBiz", partName = "InputParameters")
                    VendorInfoInputParameters vendorInfoInputParameters);

}
