
package com.midea.cloud.srm.supcooperate.deliverynote.soap.erpacceptvendorshipmentsoapbiz.impl;
import com.midea.cloud.srm.supcooperate.SupCmSaopUrl;
import com.midea.cloud.srm.supcooperate.deliverynote.soap.erpacceptvendorshipmentsoapbiz.ErpAcceptVendorShipmentSoapBizPtt;



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
@WebServiceClient(name = "ErpAcceptVendorShipmentSoapBiz_ptt-bindingQSService", targetNamespace = "http://xmlns.oracle.com/pcbpel/adapter/db/ServiceBusApplication/NSrm/ErpAcceptVendorShipmentSoapBiz"
        //, wsdlLocation = "http://soatest.longi.com:8011/NSrm/Erp/VendorShipment/ProxyServices/ErpAcceptVendorShipmentSoapProxy?wsdl"
)
public class ErpAcceptVendorShipmentSoapBizPttBindingQSService
    extends Service
{

    private final static URL ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION;
    private final static WebServiceException ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION;
    private final static QName ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_QNAME = new QName("http://xmlns.oracle.com/pcbpel/adapter/db/ServiceBusApplication/NSrm/ErpAcceptVendorShipmentSoapBiz", "ErpAcceptVendorShipmentSoapBiz_ptt-bindingQSService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL(SupCmSaopUrl.acceptSoapUrl);
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION = url;
        ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION = e;
    }

    public ErpAcceptVendorShipmentSoapBizPttBindingQSService() {
        super(__getWsdlLocation(), ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_QNAME);
    }

    public ErpAcceptVendorShipmentSoapBizPttBindingQSService(WebServiceFeature... features) {
        super(__getWsdlLocation(), ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_QNAME, features);
    }

    public ErpAcceptVendorShipmentSoapBizPttBindingQSService(URL wsdlLocation) {
        super(wsdlLocation, ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_QNAME);
    }

    public ErpAcceptVendorShipmentSoapBizPttBindingQSService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_QNAME, features);
    }

    public ErpAcceptVendorShipmentSoapBizPttBindingQSService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ErpAcceptVendorShipmentSoapBizPttBindingQSService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns ErpAcceptVendorShipmentSoapBizPtt
     */
    @WebEndpoint(name = "ErpAcceptVendorShipmentSoapBiz_ptt-bindingQSPort")
    public ErpAcceptVendorShipmentSoapBizPtt getErpAcceptVendorShipmentSoapBizPttBindingQSPort() {
        return super.getPort(new QName("http://xmlns.oracle.com/pcbpel/adapter/db/ServiceBusApplication/NSrm/ErpAcceptVendorShipmentSoapBiz", "ErpAcceptVendorShipmentSoapBiz_ptt-bindingQSPort"), ErpAcceptVendorShipmentSoapBizPtt.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ErpAcceptVendorShipmentSoapBizPtt
     */
    @WebEndpoint(name = "ErpAcceptVendorShipmentSoapBiz_ptt-bindingQSPort")
    public ErpAcceptVendorShipmentSoapBizPtt getErpAcceptVendorShipmentSoapBizPttBindingQSPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://xmlns.oracle.com/pcbpel/adapter/db/ServiceBusApplication/NSrm/ErpAcceptVendorShipmentSoapBiz", "ErpAcceptVendorShipmentSoapBiz_ptt-bindingQSPort"), ErpAcceptVendorShipmentSoapBizPtt.class, features);
    }

    private static URL __getWsdlLocation() {
        if (ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION!= null) {
            throw ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_EXCEPTION;
        }
        return ERPACCEPTVENDORSHIPMENTSOAPBIZPTTBINDINGQSSERVICE_WSDL_LOCATION;
    }

}
