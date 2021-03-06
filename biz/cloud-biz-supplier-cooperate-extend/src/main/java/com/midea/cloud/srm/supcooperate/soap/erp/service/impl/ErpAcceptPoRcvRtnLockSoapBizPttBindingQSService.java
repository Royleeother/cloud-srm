
package com.midea.cloud.srm.supcooperate.soap.erp.service.impl;

import com.midea.cloud.srm.supcooperate.SupCmSaopUrl;
import com.midea.cloud.srm.supcooperate.soap.erp.service.ErpAcceptPoRcvRtnLockSoapBizPtt;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * OSB Service
 * 
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "ErpAcceptPoRcvRtnLockSoapBiz_ptt-bindingQSService", targetNamespace = "http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/SRMSB/ErpAcceptPoRcvRtnLockSoapBiz"
//        , wsdlLocation = "http://soatest.longi.com:8011/SRMSB/Erp/PoRcvRtnLock/ProxyServices/ErpAcceptPoRcvRtnLockSoapProxy?wsdl"
)
public class ErpAcceptPoRcvRtnLockSoapBizPttBindingQSService
    extends Service
{

    private final static URL ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION;
    private final static WebServiceException ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION;
    private final static QName ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_QNAME = new QName("http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/SRMSB/ErpAcceptPoRcvRtnLockSoapBiz", "ErpAcceptPoRcvRtnLockSoapBiz_ptt-bindingQSService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL(SupCmSaopUrl.acceptPoLockUrl);
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION = url;
        ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION = e;
    }

    public ErpAcceptPoRcvRtnLockSoapBizPttBindingQSService() {
        super(__getWsdlLocation(), ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_QNAME);
    }

    public ErpAcceptPoRcvRtnLockSoapBizPttBindingQSService(WebServiceFeature... features) {
        super(__getWsdlLocation(), ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_QNAME, features);
    }

    public ErpAcceptPoRcvRtnLockSoapBizPttBindingQSService(URL wsdlLocation) {
        super(wsdlLocation, ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_QNAME);
    }

    public ErpAcceptPoRcvRtnLockSoapBizPttBindingQSService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_QNAME, features);
    }

    public ErpAcceptPoRcvRtnLockSoapBizPttBindingQSService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ErpAcceptPoRcvRtnLockSoapBizPttBindingQSService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns ErpAcceptPoRcvRtnLockSoapBizPtt
     */
    @WebEndpoint(name = "ErpAcceptPoRcvRtnLockSoapBiz_ptt-bindingQSPort")
    public ErpAcceptPoRcvRtnLockSoapBizPtt getErpAcceptPoRcvRtnLockSoapBizPttBindingQSPort() {
        return super.getPort(new QName("http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/SRMSB/ErpAcceptPoRcvRtnLockSoapBiz", "ErpAcceptPoRcvRtnLockSoapBiz_ptt-bindingQSPort"), ErpAcceptPoRcvRtnLockSoapBizPtt.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ErpAcceptPoRcvRtnLockSoapBizPtt
     */
    @WebEndpoint(name = "ErpAcceptPoRcvRtnLockSoapBiz_ptt-bindingQSPort")
    public ErpAcceptPoRcvRtnLockSoapBizPtt getErpAcceptPoRcvRtnLockSoapBizPttBindingQSPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/SRMSB/ErpAcceptPoRcvRtnLockSoapBiz", "ErpAcceptPoRcvRtnLockSoapBiz_ptt-bindingQSPort"), ErpAcceptPoRcvRtnLockSoapBizPtt.class, features);
    }

    private static URL __getWsdlLocation() {
        if (ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION!= null) {
            throw ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION;
        }
        return ERPACCEPTPORCVRTNLOCKSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION;
    }

}
