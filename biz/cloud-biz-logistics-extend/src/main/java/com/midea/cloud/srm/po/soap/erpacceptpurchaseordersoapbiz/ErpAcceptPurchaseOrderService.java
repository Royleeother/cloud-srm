
package com.midea.cloud.srm.po.soap.erpacceptpurchaseordersoapbiz;

import org.springframework.beans.factory.annotation.Value;

import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.PostConstruct;
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
@WebServiceClient(name = "ErpAcceptPurchaseOrderSoapBiz_ptt-bindingQSService", targetNamespace = "http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/SRMSB/ErpAcceptPurchaseOrderSoapBiz"
//        , wsdlLocation = "http://soatest.longi.com:8011/SRMSB/Erp/PurchaseOrder/ProxyServices/ErpAcceptPurchaseOrderSoapProxy?wsdl"
)
public class ErpAcceptPurchaseOrderService
    extends Service
{

    private static URL ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION;
    private static WebServiceException ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION;
    private static QName ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_QNAME = new QName("http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/SRMSB/ErpAcceptPurchaseOrderSoapBiz", "ErpAcceptPurchaseOrderSoapBiz_ptt-bindingQSService");

    /*static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://soatest.longi.com:8011/SRMSB/Erp/PurchaseOrder/ProxyServices/ErpAcceptPurchaseOrderSoapProxy?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION = url;
        ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION = e;
    }*/

    @Value("${SOAP_URL.ERP_PURCHASE_URL}")
    public String soapUrl;

    @PostConstruct
    public void initUrl(){
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL(soapUrl);
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION = url;
        ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION = e;
    }

    public ErpAcceptPurchaseOrderService() {
        super(__getWsdlLocation(), ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_QNAME);
    }

    public ErpAcceptPurchaseOrderService(WebServiceFeature... features) {
        super(__getWsdlLocation(), ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_QNAME, features);
    }

    public ErpAcceptPurchaseOrderService(URL wsdlLocation) {
        super(wsdlLocation, ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_QNAME);
    }

    public ErpAcceptPurchaseOrderService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_QNAME, features);
    }

    public ErpAcceptPurchaseOrderService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ErpAcceptPurchaseOrderService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     *
     * @return
     *     returns ErpAcceptPurchaseOrderSoapBizPtt
     */
    @WebEndpoint(name = "ErpAcceptPurchaseOrderSoapBiz_ptt-bindingQSPort")
    public ErpAcceptPurchaseOrderSoapBizPttBindingQSService getErpAcceptPurchaseOrderSoapBizPttBindingQSPort() {
        return super.getPort(new QName("http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/SRMSB/ErpAcceptPurchaseOrderSoapBiz", "ErpAcceptPurchaseOrderSoapBiz_ptt-bindingQSPort"), ErpAcceptPurchaseOrderSoapBizPttBindingQSService.class);
    }

    /**
     *
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ErpAcceptPurchaseOrderSoapBizPtt
     */
    @WebEndpoint(name = "ErpAcceptPurchaseOrderSoapBiz_ptt-bindingQSPort")
    public ErpAcceptPurchaseOrderSoapBizPttBindingQSService getErpAcceptPurchaseOrderSoapBizPttBindingQSPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://xmlns.oracle.com/pcbpel/adapter/db/LongiServiceBusApp/SRMSB/ErpAcceptPurchaseOrderSoapBiz", "ErpAcceptPurchaseOrderSoapBiz_ptt-bindingQSPort"), ErpAcceptPurchaseOrderSoapBizPttBindingQSService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION!= null) {
            throw ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION;
        }
        return ERPACCEPTPURCHASEORDERSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION;
    }

}
