package com.midea.cloud.srm.supcooperate.invoice.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.VendorAssesFormStatus;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.pm.po.CeeaWarehousingReturnDetailEnum;
import com.midea.cloud.common.enums.pm.ps.InvoiceStatus;
import com.midea.cloud.common.enums.pm.ps.OnlineInvoiceType;
import com.midea.cloud.common.enums.soap.AcceptPoLockOperTypeEnum;
import com.midea.cloud.common.enums.supcooperate.InvoiceImportStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.api.ApiClient;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.perf.PerformanceClient;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.api.interfacelog.dto.InterfaceLogDTO;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.soap.DataSourceEnum;
import com.midea.cloud.srm.model.common.ExportExcelParam;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.perf.vendorasses.VendorAssesForm;
import com.midea.cloud.srm.model.pm.ps.anon.external.FsscStatus;
import com.midea.cloud.srm.model.pm.ps.http.*;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.suppliercooperate.invoice.dto.*;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.*;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.Order;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderDetail;
import com.midea.cloud.srm.model.suppliercooperate.soap.erp.invoice.*;
import com.midea.cloud.srm.model.workflow.service.IFlowBusinessCallbackService;
import com.midea.cloud.srm.supcooperate.SupCmSaopUrl;
import com.midea.cloud.srm.supcooperate.invoice.mapper.OnlineInvoiceMapper;
import com.midea.cloud.srm.supcooperate.invoice.service.*;
import com.midea.cloud.srm.supcooperate.invoice.utils.OnlineInvoiceExportUtils;
import com.midea.cloud.srm.supcooperate.order.service.IOrderDetailService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderService;
import com.midea.cloud.srm.supcooperate.soap.erp.service.ErpAcceptPoRcvRtnLockSoapBizPtt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ??????????????? ???????????????
 * </pre>
 *
 * @author chensl26@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-08-31 16:47:48
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class OnlineInvoiceServiceImpl extends ServiceImpl<OnlineInvoiceMapper, OnlineInvoice> implements IOnlineInvoiceService , IFlowBusinessCallbackService {
    @Autowired
    private OnlineInvoiceMapper onlineInvoiceMapper;

    @Autowired
    private IInvoiceNoticeService iInvoiceNoticeService;

    @Autowired
    private IOnlineInvoicePunishService iOnlineInvoicePunishService;

    @Autowired
    private IInvoicePunishService iInvoicePunishService;

    @Autowired
    private IOnlineInvoiceDetailService iOnlineInvoiceDetailService;

    @Autowired
    private IOnlineInvoiceAdvanceService iOnlineInvoiceAdvanceService;

    @Autowired
    private SupplierClient supplierClient;

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private PmClient pmClient;

    @Autowired
    private IInvoiceDetailService iInvoiceDetailService;

    @Autowired
    private IOrderDetailService iOrderDetailService;

    @Autowired
    private PerformanceClient performanceClient;

    @Autowired
    private IOrderService iOrderService;

    @Autowired
    private ApiClient apiClient;

    /* ????????????????????? */
    public static final LinkedHashMap<String, String> businessTypeMap;

    static {
        businessTypeMap = new LinkedHashMap<>();

        businessTypeMap.put("105", "?????????-HR??????");
        businessTypeMap.put("108", "?????????");
        businessTypeMap.put("109", "????????????");
        businessTypeMap.put("113", "?????????-??????");
        businessTypeMap.put("122", "?????????");
        businessTypeMap.put("135", "???????????????");
        businessTypeMap.put("137", "???????????????");
        businessTypeMap.put("144", "????????????");
        businessTypeMap.put("145", "?????????-??????");
        businessTypeMap.put("147", "????????????");
        businessTypeMap.put("149", "?????????");
        businessTypeMap.put("130", "???????????????");
        businessTypeMap.put("150", "??????????????????");
        businessTypeMap.put("157", "????????????");
        businessTypeMap.put("159", "?????????");
        businessTypeMap.put("163", "?????????-????????????");
        businessTypeMap.put("164", "?????????");
        businessTypeMap.put("165", "?????????????????????");
        businessTypeMap.put("166", "?????????????????????");
        businessTypeMap.put("167", "?????????");
        businessTypeMap.put("173", "???????????????");
        businessTypeMap.put("176", "IT???????????????");
        businessTypeMap.put("284", "?????????????????????????????????????????????????????????");
        businessTypeMap.put("183", "????????????-????????????-??????");
        businessTypeMap.put("184", "????????????-????????????-??????");
        businessTypeMap.put("185", "????????????-??????(BC?????????&????????????)-??????");
        businessTypeMap.put("186", "????????????-??????(BC?????????&????????????)-??????");
        businessTypeMap.put("187", "????????????-??????");
        businessTypeMap.put("202", "???????????????????????????(EPC)");
        businessTypeMap.put("203", "??????????????????????????????(EPC)");
        businessTypeMap.put("248", "???????????????????????????");
        businessTypeMap.put("368", "????????????-??????-??????");
        businessTypeMap.put("194", "????????????-??????");
        businessTypeMap.put("215", "????????????-?????????????????????-??????");
        businessTypeMap.put("217", "???????????????-????????????-??????");
        businessTypeMap.put("218", "???????????????-????????????-??????");
        businessTypeMap.put("219", "????????????-???????????????-??????");
        businessTypeMap.put("220", "????????????-???????????????-??????");
        businessTypeMap.put("221", "????????????-????????????????????????-??????");
        businessTypeMap.put("369", "????????????-??????-??????");
        businessTypeMap.put("110", "?????????");
        businessTypeMap.put("114", "????????????");
        businessTypeMap.put("138", "?????????");
        businessTypeMap.put("142", "???????????????");
        businessTypeMap.put("174", "IT?????????");
        businessTypeMap.put("175", "IT?????????");
        businessTypeMap.put("223", "???????????????-????????????");
        businessTypeMap.put("226", "???????????????-????????????");
        businessTypeMap.put("230", "???????????????-?????????");
        businessTypeMap.put("246", "?????????????????????");
        businessTypeMap.put("247", "?????????????????????");
        businessTypeMap.put("270", "IT???");
        businessTypeMap.put("271", "???????????????");
        businessTypeMap.put("282", "????????????");
    }

    private void logFSSC(Object requestBody,
                         OnlineInvoiceOutputParameters responseBody,
                         String interfaceName,
                         String url,
                         String businessNum) {
        // ???????????????
        InterfaceLogDTO interfaceLogDTO = new InterfaceLogDTO();
        interfaceLogDTO.setServiceName(interfaceName); // ??????
        interfaceLogDTO.setServiceType("HTTP"); // ????????????
        interfaceLogDTO.setType("SEND"); // ??????
        interfaceLogDTO.setDealTime(1L);
        interfaceLogDTO.setBillId(businessNum);
        interfaceLogDTO.setServiceInfo(JSON.toJSONString(requestBody)); // ??????
        interfaceLogDTO.setCreationDateBegin(new Date()); // ????????????
        interfaceLogDTO.setCreationDateEnd(new Date()); // ????????????
        interfaceLogDTO.setUrl(url);
        interfaceLogDTO.setReturnInfo(JSON.toJSONString(responseBody)); // ??????
        if (responseBody.getXESBRESULTINFOREC() != null && "S".equals(responseBody.getXESBRESULTINFOREC().getRETURNSTATUS())) {
            interfaceLogDTO.setStatus("SUCCESS"); // ??????
            interfaceLogDTO.setFinishDate(new Date()); // ????????????
        } else {
            interfaceLogDTO.setStatus("FAIL");
            interfaceLogDTO.setErrorInfo(JSON.toJSONString(responseBody));
        }
        apiClient.createInterfaceLog(interfaceLogDTO);
    }

    //??????????????????
    @Value("${acceptPoLock.acceptPoLockUsername}")
    private String acceptPoLockUsername;
    //??????????????????
    @Value("${acceptPoLock.acceptPoLockPassword}")
    private String acceptPoLockPassword;
    //??????erp?????????????????????????????????????????????
    @Value("${acceptPoLock.cxfClientConnectTimeout}")
    private int cxfClientConnectTimeout;

    @Override
    @Transactional
    public OnlineInvoiceSaveDTO createOnlineInvoice(List<InvoiceNoticeDTO> invoiceNoticeDTOS) {
        checkParam(invoiceNoticeDTOS);//ToDo
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        OnlineInvoiceSaveDTO onlineInvoiceSaveDTO = new OnlineInvoiceSaveDTO();
        OnlineInvoice onlineInvoice = new OnlineInvoice();
        List<OnlineInvoiceDetail> onlineInvoiceDetails = new ArrayList<>();
        List<OnlineInvoicePunish> onlineInvoicePunishes = new ArrayList<>();
        List<InvoicePunish> invoicePunishes = new ArrayList<>();
        InvoiceNoticeDTO dto = invoiceNoticeDTOS.get(0);
        CompanyInfo companyInfo = supplierClient.getCompanyInfo(dto.getVendorId());
        BeanUtils.copyProperties(dto, onlineInvoice);
        if (loginAppUser.getUserType().equals(UserType.BUYER.name())) {
            onlineInvoice.setApproverUsername(loginAppUser.getUsername())
                    .setApproverNickname(loginAppUser.getNickname())
                    .setApproverId(loginAppUser.getUserId())
                    .setApproverDeptid(loginAppUser.getCeeaDeptId())
                    .setApproverDept(loginAppUser.getDepartment());
        }
        onlineInvoice.setVirtualInvoice(YesOrNo.NO.getValue())
                .setInvoiceDate(LocalDate.now())
                .setCostTypeName(dto.getCeeaCostType())
                .setContractCode(dto.getContractNo())
                .setCostTypeCode(dto.getCeeaCostTypeCode())
                .setErpVendorCode(companyInfo.getErpVendorCode());
        //1.???????????????????????????,???????????????????????????  N.
        //2.?????????????????????????????????,?????????????????? Y.
        if (dto != null && StringUtils.isBlank(dto.getInvoiceNoticeNumber())) {
            onlineInvoice.setIsService(YesOrNo.YES.getValue());
            //??????????????????
            setBusinessType(onlineInvoice, dto);
        } else {
            onlineInvoice.setIsService(YesOrNo.NO.getValue());
        }
        if (CollectionUtils.isNotEmpty(invoiceNoticeDTOS)) {
            for (InvoiceNoticeDTO invoiceNoticeDTO : invoiceNoticeDTOS) {
                if (invoiceNoticeDTO == null) continue;
                Long invoiceDetailId = invoiceNoticeDTO.getInvoiceDetailId();
                BigDecimal invoiceQuantity = invoiceNoticeDTO.getInvoiceQuantity() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getInvoiceQuantity();//??????????????????
                BigDecimal notInvoiceQuantity = invoiceNoticeDTO.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getNotInvoiceQuantity();//???????????????
                BigDecimal unitPriceExcludingTax = invoiceNoticeDTO.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getUnitPriceExcludingTax();//?????????????????????(????????????)
                BigDecimal unitPriceContainingTax = invoiceNoticeDTO.getUnitPriceContainingTax() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getUnitPriceContainingTax();//???????????????)
                //??????????????????????????????
                OnlineInvoiceDetail onlineInvoiceDetail = new OnlineInvoiceDetail();
                BeanUtils.copyProperties(invoiceNoticeDTO, onlineInvoiceDetail);
                //?????????????????????????????? ToDo
                // 2020-12-29??? ?????????????????? ????????????????????????????????????????????????bugfix
                Long orderDetailId = invoiceNoticeDTO.getOrderDetailId();
//                InvoiceDetail invoiceDetail = iInvoiceDetailService.getById(invoiceDetailId);
//                String contractCode = invoiceDetail.getContractCode();
//                Long contractHeadId = invoiceDetail.getContractHeadId();
//                String contractNo = invoiceDetail.getContractNo();
//                if (StringUtils.isNotBlank(contractCode)) {
//                    onlineInvoiceDetail.setContractCode(contractCode);
//                    onlineInvoice.setContractCode(contractCode);
//                }else {
//                    onlineInvoiceDetail.setContractCode(contractNo);
//                    onlineInvoice.setContractCode(contractNo);
//                }
                InvoiceDetail invoiceDetail = iInvoiceDetailService.getById(invoiceDetailId);
                OrderDetail orderDetail = iOrderDetailService.getById(orderDetailId);
                String contractCode = "";
                Long contractHeadId = null;
                String contractNo = "";
                if (null != invoiceDetail) {
                    contractCode = invoiceDetail.getContractCode();
                    contractHeadId = invoiceDetail.getContractHeadId();
                    contractNo = invoiceDetail.getContractNo();
                }
                if (null != orderDetail) {
                    contractCode = orderDetail.getCeeaContractNo();
                }
                if (StringUtils.isNotBlank(contractCode)) {
                    onlineInvoiceDetail.setContractCode(contractCode);
                    onlineInvoice.setContractCode(contractCode);
                }else {
                    onlineInvoiceDetail.setContractCode(contractNo);
                    onlineInvoice.setContractCode(contractNo);
                }





                onlineInvoiceDetail.setInvoiceQuantity(invoiceQuantity)
                        .setNotInvoiceQuantity(notInvoiceQuantity);
                BigDecimal noTaxAmount = invoiceQuantity.multiply(unitPriceExcludingTax).setScale(2, BigDecimal.ROUND_HALF_UP);//????????????(??????)
                onlineInvoiceDetail.setNoTaxAmount(noTaxAmount);
                BigDecimal taxAmount = invoiceQuantity.multiply(unitPriceContainingTax).setScale(2, BigDecimal.ROUND_HALF_UP);//????????????
                onlineInvoiceDetail.setTaxAmount(taxAmount);
                BigDecimal tax = taxAmount.subtract(noTaxAmount);
                onlineInvoiceDetail.setTax(tax);
                onlineInvoiceDetail.setOrderUnitPriceTaxN(unitPriceExcludingTax);//????????????(?????????)
                if (YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {
                    onlineInvoiceDetail.setOrderDetailId(invoiceNoticeDTO.getOrderDetailId());
                }
                onlineInvoiceDetail.setTxnId(invoiceNoticeDTO.getTxnId())//????????????ID
                        .setShipLineNum(invoiceNoticeDTO.getShipLineNum());//??????????????????
                onlineInvoiceDetails.add(onlineInvoiceDetail);
                //??????????????????????????????????????????????????????????????????(???????????????????????????,????????????????????????)
//                if (YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {//??????????????????????????????????????????
//                    iOrderDetailService.updateById(new OrderDetail()
//                            .setOrderDetailId(invoiceNoticeDTO.getOrderDetailId())
//                            .setInvoiceQuantity(notInvoiceQuantity.subtract(invoiceQuantity))
//                            .setNotInvoiceQuantity(notInvoiceQuantity.subtract(invoiceQuantity))
//                            .setNoTaxAmount((notInvoiceQuantity.subtract(invoiceQuantity)).multiply(unitPriceExcludingTax)));//????????????,?????????????????????????????????*???????????????
//                }
//                if (YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {//?????????????????????????????????????????????
//                    iInvoiceDetailService.updateById(new InvoiceDetail()
//                            .setInvoiceDetailId(invoiceDetailId)
//                            .setInvoiceQuantity(notInvoiceQuantity.subtract(invoiceQuantity))
//                            .setNotInvoiceQuantity(notInvoiceQuantity.subtract(invoiceQuantity))
//                            .setNoTaxAmount((notInvoiceQuantity.subtract(invoiceQuantity)).multiply(unitPriceExcludingTax)));//????????????,?????????????????????????????????*???????????????
//                }
            }

            //??????????????????????????????(???????????????,??????)
//            InvoiceNoticeDTO invoiceNoticeDTO = invoiceNoticeDTOS.get(0);
//            if (invoiceNoticeDTO != null && invoiceNoticeDTO.getInvoiceNoticeId() != null) {
//                invoicePunishes = iInvoicePunishService.list(new QueryWrapper<>(new InvoicePunish().setInvoiceNoticeId(invoiceNoticeDTO.getInvoiceNoticeId())));
//            }
//            if (CollectionUtils.isNotEmpty(invoicePunishes)) {
//                for (InvoicePunish invoicePunish : invoicePunishes) {
//                    if (invoicePunish == null) continue;
//                    OnlineInvoicePunish onlineInvoicePunish = new OnlineInvoicePunish();
//                    BeanUtils.copyProperties(invoicePunish, onlineInvoicePunish);
//                    onlineInvoicePunishes.add(onlineInvoicePunish);
//                }
//            }
        }
        //1.??????????????????
        /*????????????????????????,???????????????????????????????????????????????????*/
        BigDecimal taxTotalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        if (YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
            List<OnlineInvoiceDetail> returnOnlineInvoiceDetails = onlineInvoiceDetails.stream().filter(item -> CeeaWarehousingReturnDetailEnum.RETURN.getValue().equals(item.getType())).map(e->BeanCopyUtil.copyProperties(e,OnlineInvoiceDetail::new)).collect(Collectors.toList());//??????????????????
            List<OnlineInvoiceDetail> receiveOnlineInvoiceDetails = onlineInvoiceDetails.stream().filter(item -> CeeaWarehousingReturnDetailEnum.RECEIVE.getValue().equals(item.getType())).map(e->BeanCopyUtil.copyProperties(e,OnlineInvoiceDetail::new)).collect(Collectors.toList());//??????????????????
            if (CollectionUtils.isNotEmpty(returnOnlineInvoiceDetails) && CollectionUtils.isNotEmpty(receiveOnlineInvoiceDetails)) {
                for (OnlineInvoiceDetail returnOnlineInvoiceDetail : returnOnlineInvoiceDetails) {
                    if (returnOnlineInvoiceDetail == null) continue;
                    for (OnlineInvoiceDetail receiveOnlineInvoiceDetail : receiveOnlineInvoiceDetails) {
                        if (receiveOnlineInvoiceDetail == null) continue;
                        if (returnOnlineInvoiceDetail.getParentTxnId().compareTo(receiveOnlineInvoiceDetail.getTxnId()) == 0) {
                            BigDecimal receiveNum = returnOnlineInvoiceDetail.getReceiveNum();//??????????????????
                            BigDecimal invoiceQuantity = returnOnlineInvoiceDetail.getInvoiceQuantity();//????????????????????????
                            BigDecimal notInvoiceQuantity = returnOnlineInvoiceDetail.getNotInvoiceQuantity();//?????????????????????
                            BigDecimal noTaxAmount = returnOnlineInvoiceDetail.getNoTaxAmount();//??????????????????
                            BigDecimal tax = returnOnlineInvoiceDetail.getTax();//????????????
                            BigDecimal taxAmount = returnOnlineInvoiceDetail.getTaxAmount();//??????????????????
                            BigDecimal receiveNum1 = receiveOnlineInvoiceDetail.getReceiveNum();//??????????????????
                            BigDecimal invoiceQuantity1 = receiveOnlineInvoiceDetail.getInvoiceQuantity();//????????????????????????
                            BigDecimal notInvoiceQuantity1 = receiveOnlineInvoiceDetail.getNotInvoiceQuantity();//?????????????????????
                            BigDecimal noTaxAmount1 = receiveOnlineInvoiceDetail.getNoTaxAmount();//??????????????????
                            BigDecimal tax1 = receiveOnlineInvoiceDetail.getTax();//????????????
                            BigDecimal taxAmount1 = receiveOnlineInvoiceDetail.getTaxAmount();//??????????????????
                            //?????????????????????????????????????????????????????????
                            if(receiveNum == null){
                                receiveNum = BigDecimal.ZERO;
                            }
                            if(invoiceQuantity == null){
                                invoiceQuantity = BigDecimal.ZERO;
                            }
                            if(notInvoiceQuantity == null){
                                notInvoiceQuantity = BigDecimal.ZERO;
                            }
                            if(noTaxAmount == null){
                                noTaxAmount = BigDecimal.ZERO;
                            }
                            if(tax == null){
                                tax = BigDecimal.ZERO;
                            }
                            if(taxAmount == null){
                                taxAmount = BigDecimal.ZERO;
                            }
                            if(receiveNum1 == null){
                                receiveNum1 = BigDecimal.ZERO;
                            }
                            if(invoiceQuantity1 == null){
                                invoiceQuantity1 = BigDecimal.ZERO;
                            }
                            if(notInvoiceQuantity1 == null){
                                notInvoiceQuantity1 = BigDecimal.ZERO;
                            }
                            if(noTaxAmount1 == null){
                                noTaxAmount1 = BigDecimal.ZERO;
                            }
                            if(tax1 == null){
                                tax1 = BigDecimal.ZERO;
                            }
                            if(taxAmount1 == null){
                                taxAmount1 = BigDecimal.ZERO;
                            }
                            BigDecimal newReceiveNum = receiveNum.add(receiveNum1);
                            BigDecimal newInvoiceQuantity = invoiceQuantity.add(invoiceQuantity1);
                            BigDecimal newNotInvoiceQuantity = notInvoiceQuantity.add(notInvoiceQuantity1);
                            BigDecimal unitPriceContainingTax = receiveOnlineInvoiceDetail.getUnitPriceContainingTax();//????????????
                            BigDecimal unitPriceExcludingTax = receiveOnlineInvoiceDetail.getUnitPriceExcludingTax();//???????????????
                            BigDecimal newNoTaxAmount = newInvoiceQuantity.multiply(unitPriceExcludingTax).setScale(2, BigDecimal.ROUND_HALF_UP);//???????????????????????????
                            BigDecimal newTaxAmount = newInvoiceQuantity.multiply(unitPriceContainingTax).setScale(2, BigDecimal.ROUND_HALF_UP);//????????????????????????
                            BigDecimal newTax = newTaxAmount.subtract(newNoTaxAmount).setScale(2, BigDecimal.ROUND_HALF_UP);//??????????????????
                            receiveOnlineInvoiceDetail
                                    .setReceiveNum(newReceiveNum)
                                    .setInvoiceQuantity(newInvoiceQuantity)
                                    .setNotInvoiceQuantity(newNotInvoiceQuantity)
                                    .setNoTaxAmount(newNoTaxAmount)
                                    .setTax(newTax)
                                    .setTaxAmount(newTaxAmount);
                            break;
                        }
                    }
                }
            }
            //???return??????:
            if (CollectionUtils.isEmpty(receiveOnlineInvoiceDetails) && CollectionUtils.isNotEmpty(returnOnlineInvoiceDetails)) {
                taxTotalAmount = returnOnlineInvoiceDetails.stream().map(x ->
                        new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
                totalTax = returnOnlineInvoiceDetails.stream().map(x ->
                        new BigDecimal(StringUtil.StringValue(x.getTax().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
                //???receive??????receive,???return??????:
            } else {
                taxTotalAmount = receiveOnlineInvoiceDetails.stream().map(x ->
                        new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
                totalTax = receiveOnlineInvoiceDetails.stream().map(x ->
                        new BigDecimal(StringUtil.StringValue(x.getTax().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        //2.???????????????
        if (YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {
            taxTotalAmount = onlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalTax = onlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getTax().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        onlineInvoiceSaveDTO.setOnlineInvoiceDetails(onlineInvoiceDetails)
                .setOnlineInvoice(onlineInvoice.setTaxTotalAmount(taxTotalAmount).setTotalTax(totalTax));
        return onlineInvoiceSaveDTO;
    }

    private void setBusinessType(OnlineInvoice onlineInvoice, InvoiceNoticeDTO dto) {
        String ceeaBusinessSmallCode = dto.getCeeaBusinessSmallCode();
        if (StringUtils.isNotBlank(ceeaBusinessSmallCode)) {
            onlineInvoice.setBusinessType(ceeaBusinessSmallCode.substring(0, 3));
        }
    }

    private void checkParam(List<InvoiceNoticeDTO> invoiceNoticeDTOS) {
        Assert.notEmpty(invoiceNoticeDTOS, LocaleHandler.getLocaleMsg("??????????????????????????????????????????,?????????!"));
//        ToDo
//        1???????????????????????????????????????+?????????+????????????+??????+??????+??????+?????????????????????
        for (InvoiceNoticeDTO invoiceNoticeDTO : invoiceNoticeDTOS) {
            if (invoiceNoticeDTO == null) continue;
            Long orgId = invoiceNoticeDTO.getOrgId() == null ? 0 : invoiceNoticeDTO.getOrgId();
            Long vendorId = invoiceNoticeDTO.getVendorId() == null ? 0 : invoiceNoticeDTO.getVendorId();
            Long organizationId = invoiceNoticeDTO.getOrganizationId() == null ? 0 : invoiceNoticeDTO.getOrganizationId();
            String ceeaCostTypeCode = StringUtils.isBlank(invoiceNoticeDTO.getCeeaCostTypeCode()) ? "" : invoiceNoticeDTO.getCeeaCostTypeCode();
            String currencyCode = StringUtils.isBlank(invoiceNoticeDTO.getCurrencyCode()) ? "" : invoiceNoticeDTO.getCurrencyCode();
            String taxKey = StringUtils.isBlank(invoiceNoticeDTO.getTaxKey()) ? "" : invoiceNoticeDTO.getTaxKey();
            String projectNum = StringUtils.isBlank(invoiceNoticeDTO.getProjectNum()) ? "" : invoiceNoticeDTO.getProjectNum();
            String contractNo = StringUtils.isBlank(invoiceNoticeDTO.getContractNo()) ? "" : invoiceNoticeDTO.getContractNo();
            for (InvoiceNoticeDTO noticeDTO : invoiceNoticeDTOS) {
                if (noticeDTO == null) continue;
                Long orgIdCopy = noticeDTO.getOrgId() == null ? 0 : noticeDTO.getOrgId();
                Long vendorIdCopy = noticeDTO.getVendorId() == null ? 0 : noticeDTO.getVendorId();
                Long organizationIdCopy = noticeDTO.getOrganizationId() == null ? 0 : noticeDTO.getOrganizationId();
                String ceeaCostTypeCodeCopy = StringUtils.isBlank(noticeDTO.getCeeaCostTypeCode()) ? "" : noticeDTO.getCeeaCostTypeCode();
                String currencyCodeCopy = StringUtils.isBlank(noticeDTO.getCurrencyCode()) ? "" : noticeDTO.getCurrencyCode();
                String taxKeyCopy = StringUtils.isBlank(noticeDTO.getTaxKey()) ? "" : noticeDTO.getTaxKey();
                String projectNumCopy = StringUtils.isBlank(noticeDTO.getProjectNum()) ? "" : noticeDTO.getProjectNum();
                String contractNoCopy = StringUtils.isBlank(noticeDTO.getContractNo()) ? "" : noticeDTO.getContractNo();
                if (!(orgId.compareTo(orgIdCopy) == 0
                        && vendorId.compareTo(vendorIdCopy) == 0
                        && organizationId.compareTo(organizationIdCopy) == 0
                        && ceeaCostTypeCode.equals(ceeaCostTypeCodeCopy)
                        && currencyCode.equals(currencyCodeCopy)
                        && taxKey.equals(taxKeyCopy)
                        && projectNum.equals(projectNumCopy)
                        && contractNo.equals(contractNoCopy))) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????+?????????+????????????+??????+??????+??????+?????????????????????,?????????!"));
                }
            }
        }
//        2???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
//        3??????????????????????????????????????????????????? ??
//        4???????????????0???????????????????????????

    }

    @Override
    public PageInfo<OnlineInvoice> listPage(OnlineInvoiceQueryDTO onlineInvoiceQueryDTO) {
        PageUtil.startPage(onlineInvoiceQueryDTO.getPageNum(), onlineInvoiceQueryDTO.getPageSize());
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String userType = loginAppUser.getUserType();
        if (OnlineInvoiceType.VENDOR_INVOICE.name().equals(onlineInvoiceQueryDTO.getOnlineInvoiceType())) {
            if (UserType.VENDOR.name().equals(userType)) {
                //???????????????????????????????????????????????????????????????
                if (Objects.isNull(loginAppUser.getCompanyId())) {
                    return new PageInfo<>(new ArrayList<>());
                }
                onlineInvoiceQueryDTO.setVendorId(loginAppUser.getCompanyId());
            }
            if (UserType.BUYER.name().equals(userType)) {
                onlineInvoiceQueryDTO.setUserType(userType);
            }
        }
        onlineInvoiceQueryDTO.setOrgIds(CollectionUtils.isNotEmpty(onlineInvoiceQueryDTO.getOrgIds()) ? onlineInvoiceQueryDTO.getOrgIds() : null);
        onlineInvoiceQueryDTO.setBusinessType(StringUtils.isNotBlank(onlineInvoiceQueryDTO.getBusinessType()) ? onlineInvoiceQueryDTO.getBusinessType() : null);
        onlineInvoiceQueryDTO.setTaxInvoiceNum(StringUtils.isNotBlank(onlineInvoiceQueryDTO.getTaxInvoiceNum()) ? onlineInvoiceQueryDTO.getTaxInvoiceNum() : null);
        onlineInvoiceQueryDTO.setFsscNo(StringUtils.isNotBlank(onlineInvoiceQueryDTO.getFsscNo()) ? onlineInvoiceQueryDTO.getFsscNo() : null);
        onlineInvoiceQueryDTO.setPayMethod(StringUtils.isNotBlank(onlineInvoiceQueryDTO.getPayMethod()) ? onlineInvoiceQueryDTO.getPayMethod() : null);
        List<OnlineInvoice> result = new ArrayList<>();
        //????????????-??????????????????????????????,  ????????? ?????????????????????????????????;
        if (UserType.VENDOR.name().equals(userType)) {
            //?????????
            result = listonlineinvoiceVendor(onlineInvoiceQueryDTO);
        } else if (UserType.BUYER.name().equals(userType)) {
            //?????????
            result = listOnlineInvoiceBuyer(onlineInvoiceQueryDTO);
        }
        return new PageInfo<OnlineInvoice>(result);
    }

    //2020-12-24 ?????????????????? ????????????(??????????????????????????????????????????)
    @Override
    public PageInfo<OnlineInvoice> listPageNew(OnlineInvoiceQueryDTO onlineInvoiceQueryDTO) {
        PageUtil.startPage(onlineInvoiceQueryDTO.getPageNum(), onlineInvoiceQueryDTO.getPageSize());
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String userType = loginAppUser.getUserType();
        if (OnlineInvoiceType.VENDOR_INVOICE.name().equals(onlineInvoiceQueryDTO.getOnlineInvoiceType())) {
            if (UserType.VENDOR.name().equals(userType)) {
                //???????????????????????????????????????????????????????????????
                if (Objects.isNull(loginAppUser.getCompanyId())) {
                    return new PageInfo<>(new ArrayList<>());
                }
                onlineInvoiceQueryDTO.setVendorId(loginAppUser.getCompanyId());
            }
            if (UserType.BUYER.name().equals(userType)) {
                onlineInvoiceQueryDTO.setUserType(userType);
            }
        }
        onlineInvoiceQueryDTO.setOrgIds(CollectionUtils.isNotEmpty(onlineInvoiceQueryDTO.getOrgIds()) ? onlineInvoiceQueryDTO.getOrgIds() : null);
        onlineInvoiceQueryDTO.setBusinessType(StringUtils.isNotBlank(onlineInvoiceQueryDTO.getBusinessType()) ? onlineInvoiceQueryDTO.getBusinessType() : null);
        onlineInvoiceQueryDTO.setTaxInvoiceNum(StringUtils.isNotBlank(onlineInvoiceQueryDTO.getTaxInvoiceNum()) ? onlineInvoiceQueryDTO.getTaxInvoiceNum() : null);
        onlineInvoiceQueryDTO.setFsscNo(StringUtils.isNotBlank(onlineInvoiceQueryDTO.getFsscNo()) ? onlineInvoiceQueryDTO.getFsscNo() : null);
        onlineInvoiceQueryDTO.setPayMethod(StringUtils.isNotBlank(onlineInvoiceQueryDTO.getPayMethod()) ? onlineInvoiceQueryDTO.getPayMethod() : null);
        List<OnlineInvoice> result = onlineInvoiceMapper.findListNew(onlineInvoiceQueryDTO);
        return new PageInfo<OnlineInvoice>(result);
    }

    //    @AuthData(module = {MenuEnum.ONLINE_INVOICE , MenuEnum.SUPPLIER_SIGN})
    public List<OnlineInvoice> listonlineinvoiceVendor(OnlineInvoiceQueryDTO onlineInvoiceQueryDTO) {
        return onlineInvoiceMapper.findList(onlineInvoiceQueryDTO);
    }

    //    @AuthData(module = MenuEnum.AGENT_ONLINE_INVOICE)
    private List<OnlineInvoice> listOnlineInvoiceBuyer(OnlineInvoiceQueryDTO onlineInvoiceQueryDTO) {
        return onlineInvoiceMapper.findList(onlineInvoiceQueryDTO);
    }

    @Override
    @Transactional
    public BaseResult saveTemporary(OnlineInvoiceSaveDTO onlineInvoiceSaveDTO, String invoiceStatus) {
        OnlineInvoice onlineInvoice = onlineInvoiceSaveDTO.getOnlineInvoice();
        List<OnlineInvoiceDetail> onlineInvoiceDetails = onlineInvoiceSaveDTO.getOnlineInvoiceDetails();
        List<OnlineInvoiceAdvance> onlineInvoiceAdvances = onlineInvoiceSaveDTO.getOnlineInvoiceAdvances();
        List<OnlineInvoicePunish> onlineInvoicePunishes = onlineInvoiceSaveDTO.getOnlineInvoicePunishes();
        List<Fileupload> fileuploads = onlineInvoiceSaveDTO.getFileuploads();
        //???????????????
        checkBeforeSaveTemporary(onlineInvoice);
        //?????????????????????,???????????????????????????????????????????????????
        if (onlineInvoice.getOnlineInvoiceId() == null) {
            checkNotInvoiceQuantity(onlineInvoice, onlineInvoiceDetails);
        }
        //?????????????????????,?????????????????????????????????
        if (InvoiceStatus.REJECTED.name().equals(onlineInvoice.getInvoiceStatus())) {
            invoiceStatus = InvoiceStatus.REJECTED.name();
        }
        BaseResult baseResult = saveOrUpdateOnlineInvoiceSaveDTO(invoiceStatus, onlineInvoice, onlineInvoiceDetails, onlineInvoiceAdvances, onlineInvoicePunishes, fileuploads);
        //??????,??????????????????????????????,?????????????????????????????????
        if (ResultCode.UNKNOWN_ERROR.getCode().equals(baseResult.getCode())) {
            //????????????
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return baseResult;
        }
        return baseResult;
        //????????????(Erp)
//        BaseResult<Object> baseResult2 = BaseResult.buildSuccess();
//        Long onlineInvoiceId = (Long) baseResult.getData();
//        OnlineInvoice byId = this.getById(onlineInvoiceId);
//        OnlineInvoiceOutputParameters onlineInvoiceOutputParameters = new OnlineInvoiceOutputParameters();
//        if (byId != null && StringUtils.isBlank(byId.getInstid()) && YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
//            //???return????????????????????????????????????
//            List<OnlineInvoiceDetail> collect = onlineInvoiceDetails.stream().filter(onlineInvoiceDetail -> (CeeaWarehousingReturnDetailEnum.RECEIVE.getValue().equals(onlineInvoiceDetail.getType()))).collect(Collectors.toList());
//            if (CollectionUtils.isNotEmpty(collect)) {
//                baseResult2 = acceptPoLock(onlineInvoice, onlineInvoiceDetails, AcceptPoLockOperTypeEnum.VERIFY.name());
//                onlineInvoiceOutputParameters = (OnlineInvoiceOutputParameters) baseResult2.getData();
//            }
//            if (ResultCode.UNKNOWN_ERROR.getCode().equals(baseResult2.getCode())) {
//                //????????????
//                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                return baseResult2;
//            }
//            if (onlineInvoiceOutputParameters != null && onlineInvoiceOutputParameters.getXESBRESULTINFOREC() != null && "E".equals(onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getRETURNSTATUS())) {
//                //????????????
//                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                baseResult2.setCode(ResultCode.UNKNOWN_ERROR.getCode());
//                baseResult2.setMessage(onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getRETURNMSG());
//                return baseResult2;
//            }
//            if (onlineInvoiceOutputParameters != null && onlineInvoiceOutputParameters.getXESBRESULTINFOREC() != null && "S".equals(onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getRETURNSTATUS())) {
//                String instid = onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getINSTID();
//                this.updateById(new OnlineInvoice().setOnlineInvoiceId(onlineInvoiceId).setInstid(instid));
//            }
//        }
//        baseResult2.setCode(ResultCode.SUCCESS.getCode());
//        baseResult2.setMessage(ResultCode.SUCCESS.getMessage());
//        baseResult2.setData(onlineInvoiceId);
//        return baseResult2;
    }

    private void checkBeforeSaveTemporary(OnlineInvoice onlineInvoice) {
        Assert.notNull(onlineInvoice, "onlineInvoice????????????");
        OnlineInvoice dbOnlineInvoice = this.getById(onlineInvoice.getOnlineInvoiceId());
        Optional.ofNullable(dbOnlineInvoice).ifPresent(o -> {
            String invoiceStatus = o.getInvoiceStatus();
            if (InvoiceStatus.DROP.name().equals(invoiceStatus)) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????!"));
            }
            if (InvoiceStatus.UNDER_APPROVAL.name().equals(invoiceStatus)) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????!"));
            }
        });
    }

    public BaseResult acceptPoLock(OnlineInvoice onlineInvoice, List<OnlineInvoiceDetail> onlineInvoiceDetails, String operType) {
        long startTime = System.currentTimeMillis() / 1000;
        log.info("-----------------------------??????????????????-------------------------------------------------" + new Date());
        //????????????
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        //??????????????????
        jaxWsProxyFactoryBean.setAddress(SupCmSaopUrl.acceptPoLockUrl);
        jaxWsProxyFactoryBean.setUsername(acceptPoLockUsername);
        jaxWsProxyFactoryBean.setPassword(acceptPoLockPassword);
        //??????????????????
        jaxWsProxyFactoryBean.setServiceClass(ErpAcceptPoRcvRtnLockSoapBizPtt.class);
        //??????????????????????????????
        ErpAcceptPoRcvRtnLockSoapBizPtt service = (ErpAcceptPoRcvRtnLockSoapBizPtt) jaxWsProxyFactoryBean.create();
        //???????????????????????????????????????
        Client proxy = ClientProxy.getClient(service);
        // ??????????????????????????? ??????????????????
        HTTPConduit conduit = (HTTPConduit) proxy.getConduit();
        // ?????????????????????HTTP???????????????
        HTTPClientPolicy policy = new HTTPClientPolicy();
        // ???????????? ?????? : ??????
        policy.setConnectionTimeout(60 * 60 * 1000);
        policy.setReceiveTimeout(60 * 60 * 1000);
        conduit.setClient(policy);
        //???????????????
        OnlineInvoiceInputParameters onlineInvoiceInputParameters = new OnlineInvoiceInputParameters();
        APPSCUXPORECEIVELX1139141X1X1 esbInfo = new APPSCUXPORECEIVELX1139141X1X1();
        Date nowDate = new Date();
        esbInfo.setINSTID("");
        esbInfo.setREQUESTTIME(String.valueOf(nowDate.getTime()));
        onlineInvoiceInputParameters.setPESBINFOREC(esbInfo);

        //erp????????????
        APPSCUXPORECEIVELX1139141X1X7 requestInfos = new APPSCUXPORECEIVELX1139141X1X7();
        List<APPSCUXPORECEIVELX1139141X1X8> ppoinfotblitem = new ArrayList<>();
        APPSCUXPORECEIVELX1139141X1X8 erpOnlineInvoice = new APPSCUXPORECEIVELX1139141X1X8();
        //erp??????????????????
        APPSCUXPORECEIVEX1139141X1X11 rcvdetails = new APPSCUXPORECEIVEX1139141X1X11();
        List<APPSCUXPORECEIVEX1139141X1X12> rcvdetailsitem = new ArrayList<>();
        //??????ERP??????
        //1.??????erp????????????
        erpOnlineInvoice.setINVOICENUM(onlineInvoice.getOnlineInvoiceNum());//????????????
        erpOnlineInvoice.setOPERTYPE(operType);//????????????
        erpOnlineInvoice.setSOURCESYSCODE(DataSourceEnum.NSRM_SYS.getKey());//????????????
        erpOnlineInvoice.setIFACECODE("LOCK_RCV");//????????????
        erpOnlineInvoice.setIFACEMEAN("????????????");
        //2.??????erp??????????????????
        for (OnlineInvoiceDetail onlineInvoiceDetail : onlineInvoiceDetails) {
            if (onlineInvoiceDetail == null || CeeaWarehousingReturnDetailEnum.RETURN.getValue().equals(onlineInvoiceDetail.getType()))
                continue;
            APPSCUXPORECEIVEX1139141X1X12 erpOnlineInvoiceDetail = new APPSCUXPORECEIVEX1139141X1X12();
            erpOnlineInvoiceDetail.setRECEIPTNUM(onlineInvoiceDetail.getReceiveOrderNo());//????????????
            erpOnlineInvoiceDetail.setRECEIPTLINENUM(StringUtil.StringValue(onlineInvoiceDetail.getTxnId()));//????????????(????????????ID)
            String sourceLineId = StringUtil.StringValue(onlineInvoiceDetail.getOnlineInvoiceDetailId());
            //?????????????????????,?????????ID?????????????????????,??????????????????
            if (AcceptPoLockOperTypeEnum.CANCEL.name().equals(operType)) {
                sourceLineId = StringUtil.StringValue(IdGenrator.generate());
            }
            erpOnlineInvoiceDetail.setSOURCELINEID(sourceLineId);//???????????????ID
            erpOnlineInvoiceDetail.setINVOICEQTY(onlineInvoiceDetail.getInvoiceQuantity());//????????????
            rcvdetailsitem.add(erpOnlineInvoiceDetail);
        }
        rcvdetails.setRcvdetailsitem(rcvdetailsitem);
        //3.????????????erp??????
        erpOnlineInvoice.setRCVDETAILS(rcvdetails);
        ppoinfotblitem.add(erpOnlineInvoice);
        requestInfos.setPpoinfotblitem(ppoinfotblitem);
        onlineInvoiceInputParameters.setPPOINFOTBL(requestInfos);
        log.info("erp??????????????????:" + JsonUtil.entityToJsonStr(onlineInvoiceInputParameters));
        OnlineInvoiceOutputParameters response = null;
        BaseResult baseResult = new BaseResult<>();
        try {
            log.info(JsonUtil.entityToJsonStr(onlineInvoiceInputParameters));
            response = service.erpAcceptPoRcvRtnLockSoapBiz(onlineInvoiceInputParameters);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("????????????:", e);
            baseResult.setCode(ResultCode.UNKNOWN_ERROR.getCode());
            baseResult.setMessage(LocaleHandler.getLocaleMsg("erp????????????????????????,??????????????????!"));
            return baseResult;
        } finally {
            //ToDo ??????????????????
            try {
                logFSSC(JsonUtil.entityToJsonStr(onlineInvoiceInputParameters), response, "????????????-erp??????????????????",
                        SupCmSaopUrl.acceptPoLockUrl, onlineInvoice.getOnlineInvoiceNum());
            } catch (Exception e) {
                log.error("??????????????????????????????", e);
            }
        }
        baseResult.setData(response);
        log.info("erp????????????????????????:" + JsonUtil.entityToJsonStr(response));
        long endTime = System.currentTimeMillis() / 1000 - startTime;
        log.info("----------------------------------??????????????????----------------------------------------------" + endTime + "s");
        return baseResult;
    }

    private void checkNotInvoiceQuantity(OnlineInvoice onlineInvoice, List<OnlineInvoiceDetail> onlineInvoiceDetails) {
        if (CollectionUtils.isNotEmpty(onlineInvoiceDetails)) {
            for (OnlineInvoiceDetail onlineInvoiceDetail : onlineInvoiceDetails) {
                if (onlineInvoiceDetail == null) continue;
                if (YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {//??????????????????????????????????????????
                    OrderDetail byId = iOrderDetailService.getById(onlineInvoiceDetail.getOrderDetailId());
                    if (byId != null) {
                        BigDecimal result = byId.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : byId.getNotInvoiceQuantity();
                        int r = result.compareTo(BigDecimal.ZERO);
                        if (r == 0) {
                            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????!"));
                        }
                    }
                }
                if (YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {//?????????????????????????????????????????????
                    InvoiceDetail byId = iInvoiceDetailService.getById(onlineInvoiceDetail.getInvoiceDetailId());
                    if (byId != null) {
                        BigDecimal result = byId.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : byId.getNotInvoiceQuantity();
                        int r = result.compareTo(BigDecimal.ZERO);
                        if (r == 0) {
                            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????!"));
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public OnlineInvoiceSaveDTO submit(OnlineInvoiceSaveDTO onlineInvoiceSaveDTO, String invoiceStatus) {
        OnlineInvoice onlineInvoice = onlineInvoiceSaveDTO.getOnlineInvoice();
        List<OnlineInvoiceDetail> onlineInvoiceDetails = onlineInvoiceSaveDTO.getOnlineInvoiceDetails();
        List<OnlineInvoiceAdvance> onlineInvoiceAdvances = onlineInvoiceSaveDTO.getOnlineInvoiceAdvances();
        List<OnlineInvoicePunish> onlineInvoicePunishes = onlineInvoiceSaveDTO.getOnlineInvoicePunishes();
        List<Fileupload> fileuploads = onlineInvoiceSaveDTO.getFileuploads();

        checkBeforeSubmit(onlineInvoice, onlineInvoiceDetails, onlineInvoiceAdvances, onlineInvoicePunishes, fileuploads);//ToDo
        //????????????????????????
        saveOrUpdateOnlineInvoiceSaveDTO(invoiceStatus, onlineInvoice, onlineInvoiceDetails, onlineInvoiceAdvances, onlineInvoicePunishes, fileuploads);
        return onlineInvoiceSaveDTO;
    }

    @Override
    @Transactional
    public FSSCResult vendorAbandon(Long onlineInvoiceId) {
        OnlineInvoice onlineInvoice = this.getById(onlineInvoiceId);
        List<OnlineInvoiceDetail> onlineInvoiceDetails = iOnlineInvoiceDetailService.list(new QueryWrapper<>(new OnlineInvoiceDetail().setOnlineInvoiceId(onlineInvoiceId)));
        Assert.notNull(onlineInvoice, LocaleHandler.getLocaleMsg("??????????????????,?????????!"));
        if (!InvoiceStatus.DRAFT.name().equals(onlineInvoice.getInvoiceStatus()) && !InvoiceStatus.REJECTED.name().equals(onlineInvoice.getInvoiceStatus())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????,?????????!"));
        }
        //?????????,?????????????????????,
        if (onlineInvoice != null && YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {
            restoreOnlineInvoiceDetail(onlineInvoiceDetails);
        }
        //???????????????????????????????????????
        List<OnlineInvoicePunish> onlineInvoicePunishes = iOnlineInvoicePunishService.list(new QueryWrapper<>(new OnlineInvoicePunish().setOnlineInvoiceId(onlineInvoice.getOnlineInvoiceId())));
        if (CollectionUtils.isNotEmpty(onlineInvoicePunishes)) {
            for (OnlineInvoicePunish onlineInvoicePunish : onlineInvoicePunishes) {
                if (onlineInvoicePunish == null) continue;
                performanceClient.modify(new VendorAssesForm()
                        .setVendorAssesId(onlineInvoicePunish.getVendorAssesId())
                        .setStatus(VendorAssesFormStatus.ASSESSED.getKey())
                        .setIfQuote(YesOrNo.NO.getValue()));
            }
        }
        //????????????,????????????????????????
        if (onlineInvoice != null && YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
            for (OnlineInvoiceDetail onlineInvoiceDetail : onlineInvoiceDetails) {
                if (onlineInvoiceDetail == null) continue;
                Long invoiceDetailId = onlineInvoiceDetail.getInvoiceDetailId();//??????????????????ID
                BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getInvoiceQuantity();//????????????????????????????????????
                /*????????????????????????????????????*/
                InvoiceDetail invoiceDetail = iInvoiceDetailService.getById(invoiceDetailId);
                Integer lineNum = invoiceDetail.getLineNum();//????????????
                BigDecimal receiveNum = invoiceDetail.getReceiveNum() == null ? BigDecimal.ZERO : invoiceDetail.getReceiveNum();//??????????????????????????????
                BigDecimal unitPriceContainingTax = invoiceDetail.getUnitPriceContainingTax() == null ? BigDecimal.ZERO : invoiceDetail.getUnitPriceContainingTax();//?????????????????????????????????)
                BigDecimal noTaxAmount = invoiceDetail.getNoTaxAmount() == null ? BigDecimal.ZERO : invoiceDetail.getNoTaxAmount();//??????????????????????????????
                BigDecimal notInvoiceQuantity = invoiceDetail.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : invoiceDetail.getNotInvoiceQuantity();//?????????????????????????????????
                BigDecimal unitPriceExcludingTax = invoiceDetail.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : invoiceDetail.getUnitPriceExcludingTax();//????????????????????????(?????????)(????????????)
                BigDecimal newNotInvoiceQuantity = notInvoiceQuantity.add(invoiceQuantity);//????????????????????????????????????
                BigDecimal newInvoiceQuantity = newNotInvoiceQuantity;//?????????????????????????????????????????????????????????
                BigDecimal newNoTaxAmount = newNotInvoiceQuantity.multiply(unitPriceExcludingTax);//???????????????????????????????????????
                BigDecimal newTaxAmount = newNotInvoiceQuantity.multiply(unitPriceContainingTax);//?????????????????????????????????
                BigDecimal newTax = newTaxAmount.subtract(newNoTaxAmount);//???????????????????????????
                if (newNotInvoiceQuantity.compareTo(receiveNum) == 1) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????,????????????:" + lineNum + "????????????????????????????????????,??????????????????!"));
                }
                log.info("???????????????????????????,????????????????????????,{" + onlineInvoice.getOnlineInvoiceId() + "},start:-----------------------------");
                InvoiceDetail newInvoiceDetail = new InvoiceDetail()
                        .setInvoiceDetailId(invoiceDetailId)
                        .setNotInvoiceQuantity(newNotInvoiceQuantity)
                        .setInvoiceQuantity(newInvoiceQuantity)
                        .setNoTaxAmount(newNoTaxAmount)
                        .setTaxAmount(newTaxAmount)
                        .setTax(newTax);
                iInvoiceDetailService.updateById(newInvoiceDetail);
                log.info("???????????????????????????,????????????????????????," + JsonUtil.entityToJsonStr(newInvoiceDetail) + ",end:-----------------------------");
            }
        }
        this.updateById(onlineInvoice.setInvoiceStatus(InvoiceStatus.DROP.name()));
        //????????????????????????(????????????????????????,??????????????????????????????)(??????,????????????????????????)
        FSSCResult fsscResult = new FSSCResult();
        if (StringUtils.isNotBlank(onlineInvoice.getBoeNo())) {
            fsscResult = pmClient.fsscAbandon(new FsscStatus()
                    .setFsscNo(onlineInvoice.getBoeNo())
                    .setPeripheralSystemNum(onlineInvoice.getOnlineInvoiceNum())
                    .setSourceSysteCode(DataSourceEnum.NSRM_SYS.getKey()));
        }
        if ("500".equals(fsscResult.getCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg(fsscResult.getMsg()));
        }
        //????????????(Erp)
        OnlineInvoice byId = this.getById(onlineInvoiceId);
        OnlineInvoiceOutputParameters onlineInvoiceOutputParameters = new OnlineInvoiceOutputParameters();
        BaseResult baseResult = new BaseResult();
        if (byId != null && StringUtils.isNotBlank(byId.getInstid()) && YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
            //???return????????????????????????????????????
            List<OnlineInvoiceDetail> collect = onlineInvoiceDetails.stream().filter(onlineInvoiceDetail -> (CeeaWarehousingReturnDetailEnum.RECEIVE.getValue().equals(onlineInvoiceDetail.getType()))).collect(Collectors.toList());
//            if (CollectionUtils.isNotEmpty(collect)) {
//                baseResult = acceptPoLock(onlineInvoice, onlineInvoiceDetails, AcceptPoLockOperTypeEnum.CANCEL.name());
//            }
//            if (ResultCode.UNKNOWN_ERROR.getCode().equals(baseResult.getCode())) {
//                throw new BaseException(baseResult.getMessage());
//            }
            onlineInvoiceOutputParameters = (OnlineInvoiceOutputParameters) baseResult.getData();
            if (onlineInvoiceOutputParameters != null && onlineInvoiceOutputParameters.getXESBRESULTINFOREC() != null && "E".equals(onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getRETURNSTATUS())) {
                throw new BaseException(onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getRETURNMSG());
            }
        }
        return fsscResult;
    }

    @Override
    public OnlineInvoiceSaveDTO get(Long onlineInvoiceId) {
        OnlineInvoice onlineInvoice = this.getById(onlineInvoiceId);
        List<OnlineInvoiceDetail> onlineInvoiceDetails = iOnlineInvoiceDetailService.list(new QueryWrapper<>(new OnlineInvoiceDetail().setOnlineInvoiceId(onlineInvoiceId)));
        List<OnlineInvoicePunish> onlineInvoicePunishes = iOnlineInvoicePunishService.list(new QueryWrapper<>(new OnlineInvoicePunish().setOnlineInvoiceId(onlineInvoiceId)));
        List<OnlineInvoiceAdvance> onlineInvoiceAdvances = iOnlineInvoiceAdvanceService.list(new QueryWrapper<>(new OnlineInvoiceAdvance().setOnlineInvoiceId(onlineInvoiceId)));
        Fileupload fileupload = new Fileupload().setBusinessId(onlineInvoiceId);
        fileupload.setPageSize(100);
        List<Fileupload> fileuploads = fileCenterClient.listPage(fileupload, null).getList();
        OnlineInvoiceSaveDTO onlineInvoiceSaveDTO = new OnlineInvoiceSaveDTO();
        onlineInvoiceSaveDTO.setOnlineInvoiceDetails(onlineInvoiceDetails).setOnlineInvoicePunishes(onlineInvoicePunishes).setOnlineInvoiceAdvances(onlineInvoiceAdvances).setOnlineInvoice(onlineInvoice).setFileuploads(fileuploads);
        return onlineInvoiceSaveDTO;
    }

    @Override
    @Transactional
    public OnlineInvoiceSaveDTO audit(OnlineInvoiceSaveDTO onlineInvoiceSaveDTO, String invoiceStatus) {
        OnlineInvoice onlineInvoice = onlineInvoiceSaveDTO.getOnlineInvoice();
        Long onlineInvoiceId = onlineInvoice.getOnlineInvoiceId();
        Assert.notNull(onlineInvoiceId, LocaleHandler.getLocaleMsg("onlineInvoiceId????????????"));
        List<OnlineInvoiceAdvance> onlineInvoiceAdvances = onlineInvoiceSaveDTO.getOnlineInvoiceAdvances();
        List<OnlineInvoicePunish> onlineInvoicePunishes = onlineInvoiceSaveDTO.getOnlineInvoicePunishes();
        List<OnlineInvoiceDetail> onlineInvoiceDetails = onlineInvoiceSaveDTO.getOnlineInvoiceDetails();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        //???????????????
        checkBeforeAudit(onlineInvoice);
//        adjustInvoiceAmount(onlineInvoiceDetails, onlineInvoice, onlineInvoicePunishes);//????????????
        //???????????????????????????????????????????????????
        this.saveOrUpdateOnlineInvoice(invoiceStatus, onlineInvoice, loginAppUser);
//        this.updateById(onlineInvoice.setInvoiceStatus(invoiceStatus));
        //????????????????????????
        if (CollectionUtils.isNotEmpty(onlineInvoiceAdvances)) {
            for (OnlineInvoiceAdvance onlineInvoiceAdvance : onlineInvoiceAdvances) {
                if (onlineInvoiceAdvance == null) continue;
                if (onlineInvoiceAdvance.getOnlineInvoiceAdvanceId() == null) {
                    iOnlineInvoiceAdvanceService.save(onlineInvoiceAdvance
                            .setOnlineInvoiceAdvanceId(IdGenrator.generate()).setOnlineInvoiceId(onlineInvoice.getOnlineInvoiceId()));
                } else {
                    iOnlineInvoiceAdvanceService.updateById(onlineInvoiceAdvance);
                }
            }
        }
        //?????????????????????
        saveOrUpdateOnlineInvoicePunishes(onlineInvoicePunishes, onlineInvoice);
        return onlineInvoiceSaveDTO;
    }

    private void checkBeforeAudit(OnlineInvoice onlineInvoice) {
        Assert.notNull(onlineInvoice, "onlineInvoice????????????");
        if (InvoiceStatus.UNDER_APPROVAL.name().equals(onlineInvoice.getInvoiceStatus()) ||
                InvoiceStatus.REJECTED.name().equals(onlineInvoice.getInvoiceStatus())) {
            log.info("????????????{}????????????????????????????????????????????????" , onlineInvoice.getOnlineInvoiceNum());
        }else{
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????,??????????????????"));
        }
    }

    @Override
    @Transactional
    public void buyerAbandon(Long onlineInvoiceId) {
        OnlineInvoice onlineInvoice = this.getById(onlineInvoiceId);
        List<OnlineInvoiceDetail> onlineInvoiceDetails = iOnlineInvoiceDetailService.list(new QueryWrapper<>(new OnlineInvoiceDetail().setOnlineInvoiceId(onlineInvoiceId)));
        this.updateById(new OnlineInvoice()
                .setOnlineInvoiceId(onlineInvoiceId)
                .setInvoiceStatus(InvoiceStatus.DROP.name()));
        //????????? ToDo ????????????
        if (onlineInvoice != null && YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {
            //?????????????????????????????????
            restoreOnlineInvoiceDetail(onlineInvoiceDetails);
            //???????????????????????????????????????
            List<OnlineInvoicePunish> onlineInvoicePunishes = iOnlineInvoicePunishService.list(new QueryWrapper<>(new OnlineInvoicePunish().setOnlineInvoiceId(onlineInvoice.getOnlineInvoiceId())));
            if (CollectionUtils.isNotEmpty(onlineInvoicePunishes)) {
                for (OnlineInvoicePunish onlineInvoicePunish : onlineInvoicePunishes) {
                    if (onlineInvoicePunish == null) continue;
                    performanceClient.modify(new VendorAssesForm()
                            .setVendorAssesId(onlineInvoicePunish.getVendorAssesId())
                            .setStatus(VendorAssesFormStatus.ASSESSED.getKey())
                            .setIfQuote(YesOrNo.NO.getValue()));
                }
            }

        }
        //????????????
        if (onlineInvoice != null && YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
            //????????????????????????
            for (OnlineInvoiceDetail onlineInvoiceDetail : onlineInvoiceDetails) {
                if (onlineInvoiceDetail == null) continue;
                Long invoiceDetailId = onlineInvoiceDetail.getInvoiceDetailId();//??????????????????ID
                BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getInvoiceQuantity();//????????????????????????????????????
                /*????????????????????????????????????*/
                InvoiceDetail invoiceDetail = iInvoiceDetailService.getById(invoiceDetailId);
                Integer lineNum = invoiceDetail.getLineNum();//????????????
                BigDecimal receiveNum = invoiceDetail.getReceiveNum() == null ? BigDecimal.ZERO : invoiceDetail.getReceiveNum();//??????????????????????????????
                BigDecimal unitPriceContainingTax = invoiceDetail.getUnitPriceContainingTax() == null ? BigDecimal.ZERO : invoiceDetail.getUnitPriceContainingTax();//?????????????????????????????????)
                BigDecimal noTaxAmount = invoiceDetail.getNoTaxAmount() == null ? BigDecimal.ZERO : invoiceDetail.getNoTaxAmount();//??????????????????????????????
                BigDecimal notInvoiceQuantity = invoiceDetail.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : invoiceDetail.getNotInvoiceQuantity();//?????????????????????????????????
                BigDecimal unitPriceExcludingTax = invoiceDetail.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : invoiceDetail.getUnitPriceExcludingTax();//????????????????????????(?????????)(????????????)
                BigDecimal newNotInvoiceQuantity = notInvoiceQuantity.add(invoiceQuantity);//????????????????????????????????????
                if (newNotInvoiceQuantity.compareTo(receiveNum) == 1) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????,????????????:" + lineNum + "????????????????????????????????????,??????????????????!"));
                }
                BigDecimal newInvoiceQuantity = newNotInvoiceQuantity;//?????????????????????????????????????????????????????????
                BigDecimal newNoTaxAmount = newNotInvoiceQuantity.multiply(unitPriceExcludingTax);//???????????????????????????????????????
                BigDecimal newTaxAmount = newNotInvoiceQuantity.multiply(unitPriceContainingTax);//?????????????????????????????????
                BigDecimal newTax = newTaxAmount.subtract(newNoTaxAmount);//???????????????????????????
                iInvoiceDetailService.updateById(new InvoiceDetail()
                        .setInvoiceDetailId(invoiceDetailId)
                        .setNotInvoiceQuantity(newNotInvoiceQuantity)
                        .setInvoiceQuantity(newInvoiceQuantity)
                        .setNoTaxAmount(newNoTaxAmount)
                        .setTaxAmount(newTaxAmount)
                        .setTax(newTax));
            }

            //??????????????????
            List<OnlineInvoicePunish> onlineInvoicePunishes = iOnlineInvoicePunishService.list(new QueryWrapper<>(new OnlineInvoicePunish().setOnlineInvoiceId(onlineInvoice.getOnlineInvoiceId())));
            if (CollectionUtils.isNotEmpty(onlineInvoicePunishes)) {
                for (OnlineInvoicePunish onlineInvoicePunish : onlineInvoicePunishes) {
                    if (onlineInvoicePunish == null) continue;
                    iInvoicePunishService.updateById(new InvoicePunish().setInvoicePunishId(onlineInvoicePunish.getInvoicePunishId())
                            .setIfQuote(YesOrNo.NO.getValue()));
                }
            }
        }
        /*????????????,????????????????????? modify by chensl26 2021-2-24*/
        //????????????(Erp)
//        OnlineInvoiceOutputParameters onlineInvoiceOutputParameters = new OnlineInvoiceOutputParameters();
//        BaseResult baseResult = new BaseResult();
//        if (onlineInvoice != null && StringUtils.isNotBlank(onlineInvoice.getInstid()) && YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
//            //???return????????????????????????????????????
//            List<OnlineInvoiceDetail> collect = onlineInvoiceDetails.stream().filter(onlineInvoiceDetail -> (CeeaWarehousingReturnDetailEnum.RECEIVE.getValue().equals(onlineInvoiceDetail.getType()))).collect(Collectors.toList());
////            if (CollectionUtils.isNotEmpty(collect)) {
////                baseResult = acceptPoLock(onlineInvoice, onlineInvoiceDetails, AcceptPoLockOperTypeEnum.CANCEL.name());
////            }
////            if (ResultCode.UNKNOWN_ERROR.getCode().equals(baseResult.getCode())) {
////                //????????????
////                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
////                return baseResult;
////            }
//            onlineInvoiceOutputParameters = (OnlineInvoiceOutputParameters) baseResult.getData();
//            if (onlineInvoiceOutputParameters != null && onlineInvoiceOutputParameters.getXESBRESULTINFOREC() != null && "E".equals(onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getRETURNSTATUS())) {
//                //????????????
//                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                baseResult.setCode(ResultCode.UNKNOWN_ERROR.getCode());
//                baseResult.setMessage(onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getRETURNMSG());
//                return baseResult;
//            }
//            if (onlineInvoiceOutputParameters != null && onlineInvoiceOutputParameters.getXESBRESULTINFOREC() != null && "S".equals(onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getRETURNSTATUS())) {
//                String instid = onlineInvoiceOutputParameters.getXESBRESULTINFOREC().getINSTID();
//                this.updateById(new OnlineInvoice().setOnlineInvoiceId(onlineInvoiceId).setInstid(instid));
//            }
//        }
//        baseResult.setCode(ResultCode.SUCCESS.getCode());
//        baseResult.setMessage(ResultCode.SUCCESS.getMessage());
//        baseResult.setData(onlineInvoiceId);
//        return baseResult;
    }

    private void restoreOnlineInvoiceDetail(List<OnlineInvoiceDetail> onlineInvoiceDetails) {
        //???????????????????????????????????????
        Set<String> collect = onlineInvoiceDetails.stream().map(onlineInvoiceDetail -> (onlineInvoiceDetail.getOrderNumber())).collect(Collectors.toSet());
        List<Order> orders = iOrderService.list(Wrappers.lambdaQuery(Order.class).select(Order::getOrderId, Order::getOrderNumber).in(Order::getOrderNumber, collect));
        Map<String, Long> stringLongMap = orders.stream().collect(Collectors.toMap(Order::getOrderNumber, Order::getOrderId, (key1, key2) -> key2));
        Collection<Long> orderIds = stringLongMap.values();
        List<OrderDetail> orderDetails = iOrderDetailService.list(Wrappers.lambdaQuery(OrderDetail.class).in(OrderDetail::getOrderId, orderIds));
        Map<Long, List<OrderDetail>> longOrderDetailMap = orderDetails.stream().collect(Collectors.groupingBy(OrderDetail::getOrderId));
        List<OrderDetail> newOrderDetails = new ArrayList<>();
        for (OnlineInvoiceDetail onlineInvoiceDetail : onlineInvoiceDetails) {
            if (onlineInvoiceDetail == null) continue;
            BigDecimal noTaxAmount = onlineInvoiceDetail.getNoTaxAmount() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getNoTaxAmount();//????????????
            BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getInvoiceQuantity();//??????????????????
            BigDecimal notInvoiceQuantity = onlineInvoiceDetail.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getNotInvoiceQuantity();//???????????????
            BigDecimal unitPriceExcludingTax = onlineInvoiceDetail.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getUnitPriceExcludingTax();//?????????????????????(????????????)
            String orderNumber = onlineInvoiceDetail.getOrderNumber();//?????????
            Integer lineNum = onlineInvoiceDetail.getLineNum();//????????????
            Long orderId = stringLongMap.get(orderNumber);
            List<OrderDetail> orderDetails1 = longOrderDetailMap.get(orderId);
            OrderDetail orderDetail = orderDetails1.stream().filter(o -> (Integer.compare(o.getLineNum(), lineNum) == 0)).collect(Collectors.toList()).get(0);
            BigDecimal oldNotInvoiceQuantity = orderDetail.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : orderDetail.getNotInvoiceQuantity();
            BigDecimal oldInvoiceQuantity = orderDetail.getInvoiceQuantity() == null ? BigDecimal.ZERO : orderDetail.getInvoiceQuantity();
            BigDecimal oldNoTaxAmount = orderDetail.getNoTaxAmount() == null ? BigDecimal.ZERO : orderDetail.getNoTaxAmount();
            BigDecimal receiveNum = orderDetail.getReceiveNum() == null ? BigDecimal.ZERO : orderDetail.getReceiveNum();
            BigDecimal newNotInvoiceQuantity = oldNotInvoiceQuantity.add(notInvoiceQuantity);
            BigDecimal newInvoiceQuantity = newNotInvoiceQuantity;
            BigDecimal newNoTaxAmount = newNotInvoiceQuantity.multiply(unitPriceExcludingTax);
            orderDetail.setNotInvoiceQuantity(newNotInvoiceQuantity).setInvoiceQuantity(newInvoiceQuantity).setNoTaxAmount(newNoTaxAmount);
            newOrderDetails.add(orderDetail);
            if (newNotInvoiceQuantity.compareTo(receiveNum) == 1) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????,????????????:" + lineNum + "????????????????????????????????????,??????????????????!"));
            }
        }
        iOrderDetailService.updateBatchById(newOrderDetails);
    }

    @Override
    @Transactional
    public void statusReturn(String boeNo, String invoiceStatus) {
        UpdateWrapper<OnlineInvoice> updateWrapper = new UpdateWrapper<>(new OnlineInvoice().setBoeNo(boeNo));
        this.update(new OnlineInvoice().setInvoiceStatus(invoiceStatus), updateWrapper);
        log.info("----------------------??????????????????????????????Nsrm??????????????????----------------");
        OnlineInvoice onlineInvoice = this.getOne(new QueryWrapper<>(new OnlineInvoice().setBoeNo(boeNo)));
        if (onlineInvoice != null) {
            List<OnlineInvoiceDetail> onlineInvoiceDetails = iOnlineInvoiceDetailService.list(new QueryWrapper<>(new OnlineInvoiceDetail().setOnlineInvoiceId(onlineInvoice.getOnlineInvoiceId())));
            if (InvoiceStatus.REVIEWED.name().equals(invoiceStatus)) {
                //???return??????????????????????????????????????????
                List<OnlineInvoiceDetail> collect = onlineInvoiceDetails.stream().filter(onlineInvoiceDetail -> (CeeaWarehousingReturnDetailEnum.RECEIVE.getValue().equals(onlineInvoiceDetail.getType()))).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(collect)) {
                    //ToDo ??????????????????????????????
//                    BaseResult baseResult = this.acceptPoLock(onlineInvoice, onlineInvoiceDetails, AcceptPoLockOperTypeEnum.CANCEL.name());
                }
                log.info("-------------------------???????????????????????????,????????????ERP????????????-------------------------------");
            }
        }
    }

    @Override
    public void importStatusReturn(String boeNo, String importStatus) {
        UpdateWrapper<OnlineInvoice> updateWrapper = new UpdateWrapper<>(new OnlineInvoice().setBoeNo(boeNo));
        this.update(new OnlineInvoice().setImportStatus(importStatus), updateWrapper);
    }

    @Override
    @Transactional
    public BaseResult saveTemporaryBeforeAudit(OnlineInvoiceSaveDTO onlineInvoiceSaveDTO) {
        OnlineInvoice onlineInvoice = onlineInvoiceSaveDTO.getOnlineInvoice();
        List<OnlineInvoiceAdvance> onlineInvoiceAdvances = onlineInvoiceSaveDTO.getOnlineInvoiceAdvances();
        List<OnlineInvoiceDetail> onlineInvoiceDetails = onlineInvoiceSaveDTO.getOnlineInvoiceDetails();
        List<OnlineInvoicePunish> onlineInvoicePunishes = onlineInvoiceSaveDTO.getOnlineInvoicePunishes();
        List<Fileupload> fileuploads = onlineInvoiceSaveDTO.getFileuploads();
        BaseResult baseResult = saveOrUpdateOnlineInvoiceSaveDTO(onlineInvoice.getInvoiceStatus(), onlineInvoice, onlineInvoiceDetails, onlineInvoiceAdvances, onlineInvoicePunishes, fileuploads);
        return baseResult;
    }

    @Override
    public void withdraw(Long onlineInvoiceId) {
        OnlineInvoice onlineInvoice = this.getById(onlineInvoiceId);
        this.updateById(onlineInvoice.setInvoiceStatus(InvoiceStatus.DRAFT.name()));
    }

    @Override
    public void export(OnlineInvoiceQueryDTO onlineInvoiceQueryDTO, HttpServletResponse response) throws Exception {
        List<OnlineInvoice> list = this.listPage(onlineInvoiceQueryDTO).getList();
        CompletableFuture<List<DictItemDTO>> cfInvoiceStatus = CompletableFuture.supplyAsync(() -> baseClient.listAllByParam(new DictItemDTO().setDictCode("INVOICE_STATUS")));
        CompletableFuture<List<DictItemDTO>> cfInvoiceImportStatus = CompletableFuture.supplyAsync(() -> baseClient.listAllByParam(new DictItemDTO().setDictCode("INVOICE_IMPORT_STATUS")));
        CompletableFuture<List<DictItemDTO>> cfBusinessType = CompletableFuture.supplyAsync(() -> baseClient.listAllByParam(new DictItemDTO().setDictCode("BUSINESS_TYPE")));
        CompletableFuture.allOf(cfInvoiceStatus, cfInvoiceImportStatus, cfBusinessType).join();


        List<DictItemDTO> invoiceStatusList = cfInvoiceStatus.get();
        List<DictItemDTO> invoiceImportStatusList = cfInvoiceImportStatus.get();
        List<DictItemDTO> businessTypeList = cfBusinessType.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ArrayList<OnlineInvoiceExcelDTO> onlineInvoiceExcelDTOS = new ArrayList<>();

        list.stream().forEach(x -> {
            OnlineInvoiceExcelDTO excelDTO = new OnlineInvoiceExcelDTO();
            BeanUtils.copyProperties(x, excelDTO);
            if (Objects.nonNull(x.getAccountPayableDealine())) {
                excelDTO.setAccountPayableDealine(x.getAccountPayableDealine().format(fmt));
            }
            excelDTO.setCreationDate(DateUtil.parseDateToStr(x.getCreationDate(), DateUtil.YYYY_MM_DD));
            excelDTO.setInvoiceStatus(getDicName(invoiceStatusList, x.getInvoiceStatus()));
            excelDTO.setImportStatus(getDicName(invoiceImportStatusList, x.getImportStatus()));
            excelDTO.setBusinessType(getDicName(businessTypeList, x.getBusinessType()));
            onlineInvoiceExcelDTOS.add(excelDTO);
        });
        OutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "online_invoice");
        EasyExcelUtil.writeExcelWithModel(outputStream, "????????????", onlineInvoiceExcelDTOS, OnlineInvoiceExcelDTO.class);
    }

    @Override
    @Transactional
    public void restart(List<Long> onlineInvoiceIds) {
        List<OnlineInvoice> onlineInvoices = this.listByIds(onlineInvoiceIds);
        List<Long> collect = onlineInvoices.stream().filter(Objects::nonNull).filter(onlineInvoice -> onlineInvoice.getOnlineInvoiceId() != null).map(OnlineInvoice::getOnlineInvoiceId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(collect)) {
            log.info("--------------------------????????????????????????----------------------------");
            /*??????????????????????????????????????????????????????????????????*/
            OnlineInvoice byId = onlineInvoices.get(0);
            if (byId != null && byId.getOnlineInvoiceType().equals(OnlineInvoiceType.VENDOR_INVOICE.name())) {
                this.baseMapper.updateDealineAndPeriod(byId.getOnlineInvoiceId());
            }
            /*????????????????????????????????????*/
            iOnlineInvoiceDetailService.updateChangeFlag(collect);
            /*????????????????????????????????????????????????*/
            iOnlineInvoiceDetailService.updateUnitPrice(collect);
            /*?????????????????????????????????????????????*/
            iOnlineInvoiceDetailService.updateAmount(collect);
            /*?????????????????????*/
            iOnlineInvoiceDetailService.updateTax(collect);
            /*??????????????????*/
            iOnlineInvoiceDetailService.updateCompareResult(collect);
            log.info("---------------------------????????????????????????----------------------------");
        }
    }

    @Override
    public BaseResult batchAdjust(List<Long> onlineInvoiceIds) {
        log.info("---------------------????????????????????????----------------------------");
        List<OnlineInvoice> onlineInvoices = this.listByIds(onlineInvoiceIds);
        List<OnlineInvoiceDetail> onlineInvoiceDetails = iOnlineInvoiceDetailService.list(Wrappers.lambdaQuery(OnlineInvoiceDetail.class)
                .in(OnlineInvoiceDetail::getOnlineInvoiceId, onlineInvoiceIds));
        Map<Long, List<OnlineInvoiceDetail>> map = onlineInvoiceDetails.stream().collect(Collectors.groupingBy(OnlineInvoiceDetail::getOnlineInvoiceId));
        BaseResult baseResult = BaseResult.buildSuccess();
        int num = 1;
        for (OnlineInvoice onlineInvoice : onlineInvoices) {
            log.info("--------????????????????????????{} ??? ??????????????????{}" , onlineInvoice.getOnlineInvoiceNum() , JSONObject.toJSONString(onlineInvoice));
            OnlineInvoiceSaveDTO onlineInvoiceSaveDTO = new OnlineInvoiceSaveDTO();
            onlineInvoiceSaveDTO.setOnlineInvoice(onlineInvoice);
            List<OnlineInvoiceDetail> onlineInvoiceDetails1 = map.get(onlineInvoice.getOnlineInvoiceId());
            onlineInvoiceSaveDTO.setOnlineInvoiceDetails(onlineInvoiceDetails1);
            try {
                baseResult = batchAdjustAmount(onlineInvoiceSaveDTO);
            }catch (Exception e){
                e.printStackTrace();
                log.error("?????????"+num+"???????????????????????????????????????",e);
            }
            if (ResultCode.SUCCESS.getCode().equals(baseResult.getCode())) {
                log.info("??????????????????" + num + "?????????," + JsonUtil.entityToJsonStr(onlineInvoiceSaveDTO));
            }
            num ++;
        }

        log.info("---------------------????????????????????????-----------------------------");
        return baseResult;
    }

    @Override
    public OnlineInvoiceQueryDTO queryBatchOnlineInvoiceSaveDTO(List<Long> onlineInvoiceIds) {
        List<OnlineInvoice> onlineInvoices = this.listByIds(onlineInvoiceIds);
        List<OnlineInvoiceDetail> onlineInvoiceDetails = iOnlineInvoiceDetailService.list(Wrappers.lambdaQuery(OnlineInvoiceDetail.class)
                .in(OnlineInvoiceDetail::getOnlineInvoiceId, onlineInvoiceIds));
        Map<Long, List<OnlineInvoiceDetail>> mapOnlineInvoiceDetail = onlineInvoiceDetails.stream().collect(Collectors.groupingBy(OnlineInvoiceDetail::getOnlineInvoiceId));
        List<OnlineInvoiceAdvance> onlineInvoiceAdvances = iOnlineInvoiceAdvanceService.list(Wrappers.lambdaQuery(OnlineInvoiceAdvance.class).in(OnlineInvoiceAdvance::getOnlineInvoiceId, onlineInvoiceIds));
        Map<Long, List<OnlineInvoiceAdvance>> mapOnlineInvoiceAdvance = onlineInvoiceAdvances.stream().collect(Collectors.groupingBy(OnlineInvoiceAdvance::getOnlineInvoiceId));
        Map<Long, List<Fileupload>> mapFileupload = new HashMap<>();
        for (Long onlineInvoiceId : onlineInvoiceIds) {
            PageInfo<Fileupload> fileuploadPageInfo = fileCenterClient.listPage(new Fileupload().setBusinessId(onlineInvoiceId), "");
            List<Fileupload> list = fileuploadPageInfo.getList();
            mapFileupload.put(onlineInvoiceId, list);
        }
        OnlineInvoiceQueryDTO onlineInvoiceQueryDTO = new OnlineInvoiceQueryDTO();
        onlineInvoiceQueryDTO.setOnlineInvoices(onlineInvoices)
                .setMapOnlineInvoiceDetail(mapOnlineInvoiceDetail)
                .setMapOnlineInvoiceAdvance(mapOnlineInvoiceAdvance)
                .setMapFileuploads(mapFileupload);
        return onlineInvoiceQueryDTO;
    }

    @Override
    @Transactional
    public void batchRestart(List<Long> onlineInvoiceIds) {
        List<OnlineInvoice> onlineInvoices = this.listByIds(onlineInvoiceIds);
        List<Long> collect = onlineInvoices.stream().filter(Objects::nonNull).filter(onlineInvoice -> onlineInvoice.getOnlineInvoiceId() != null).map(OnlineInvoice::getOnlineInvoiceId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(collect)) {
            log.info("--------------------------????????????????????????----------------------------");
            /*??????????????????????????????????????????????????????????????????*/
            /*if (byId.getOnlineInvoiceType().equals(OnlineInvoiceType.VENDOR_INVOICE.name())) {
                this.baseMapper.updateDealineAndPeriod(onlineInvoiceId);
            }*/
            /*????????????????????????????????????*/
            iOnlineInvoiceDetailService.updateChangeFlag(collect);
            /*????????????????????????????????????????????????*/
            iOnlineInvoiceDetailService.updateUnitPrice(collect);
            /*?????????????????????????????????????????????*/
            iOnlineInvoiceDetailService.updateAmount(collect);
            /*?????????????????????*/
            iOnlineInvoiceDetailService.updateTax(collect);
            /*??????????????????*/
            iOnlineInvoiceDetailService.updateCompareResult(collect);
            log.info("---------------------------????????????????????????----------------------------");
        }
    }



    private BaseResult batchAdjustAmount(OnlineInvoiceSaveDTO onlineInvoiceSaveDTO) {
        OnlineInvoice onlineInvoice = onlineInvoiceSaveDTO.getOnlineInvoice();
        List<OnlineInvoiceDetail> onlineInvoiceDetails = onlineInvoiceSaveDTO.getOnlineInvoiceDetails();
        String invoiceStatus = "";
        if (OnlineInvoiceType.BUYER_INVOICE.name().equals(onlineInvoice.getOnlineInvoiceType())) {
            invoiceStatus = InvoiceStatus.DRAFT.name();
        } else if (OnlineInvoiceType.VENDOR_INVOICE.name().equals(onlineInvoice.getOnlineInvoiceType())){
            invoiceStatus = InvoiceStatus.UNDER_APPROVAL.name();
        } else if (InvoiceStatus.REJECTED.name().equals(onlineInvoice.getInvoiceStatus())) {
            invoiceStatus = InvoiceStatus.REJECTED.name();
        }
        log.info("??????????????????{}?????????{}", onlineInvoice.getOnlineInvoiceNum() , invoiceStatus);
        BaseResult baseResult = saveTemporary(onlineInvoiceSaveDTO, invoiceStatus);
//        BaseResult baseResult = adjustInvoiceAmount(onlineInvoiceDetails, onlineInvoice, null);
        return baseResult;
    }

    private void checkBeforeSubmit(OnlineInvoice onlineInvoice, List<OnlineInvoiceDetail> onlineInvoiceDetails, List<OnlineInvoiceAdvance> onlineInvoiceAdvances, List<OnlineInvoicePunish> onlineInvoicePunishes, List<Fileupload> fileuploads) {
        Assert.notNull(onlineInvoice, "onlineInvoice????????????");
        if (InvoiceStatus.DRAFT.name().equals(onlineInvoice.getInvoiceStatus()) ||
                InvoiceStatus.UNDER_APPROVAL.name().equals(onlineInvoice.getInvoiceStatus()) ||
                InvoiceStatus.REJECTED.name().equals(onlineInvoice.getInvoiceStatus())) {
            log.info("????????????{}????????????????????????????????????????????????" , onlineInvoice.getOnlineInvoiceNum());
        }else{
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????,????????????"));
        }
    }

    public BaseResult  saveOrUpdateOnlineInvoiceSaveDTO(String invoiceStatus, OnlineInvoice onlineInvoice, List<OnlineInvoiceDetail> onlineInvoiceDetails, List<OnlineInvoiceAdvance> onlineInvoiceAdvances, List<OnlineInvoicePunish> onlineInvoicePunishes, List<Fileupload> fileuploads) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        //??????????????????
        String onlineInvoiceType = onlineInvoice.getOnlineInvoiceType();
        //???????????????????????????
        BaseResult<List<OnlineInvoiceDetail>> baseResult = BaseResult.buildSuccess();
        List<OnlineInvoiceDetail> newOnlineInvoiceDetails = new ArrayList<>();
        //????????????,?????????????????????,??????????????????,????????????????????????
        if (CollectionUtils.isNotEmpty(onlineInvoiceDetails)) {
            OnlineInvoiceDetail onlineInvoiceDetail = onlineInvoiceDetails.get(0);
            String poUnitPriceChangeFlag = onlineInvoiceDetail.getPoUnitPriceChangeFlag();
            //???????????????????????????,?????????????????????
            if (StringUtils.isEmpty(poUnitPriceChangeFlag)
                    && UserType.BUYER.name().equals(loginAppUser.getUserType())) {
                /**/
                checkBeforeAdjustInvoiceAmount(onlineInvoiceDetails);
                baseResult = adjustInvoiceAmount(onlineInvoiceDetails, onlineInvoice, onlineInvoicePunishes);
                newOnlineInvoiceDetails = baseResult.getData();
                onlineInvoiceDetails = newOnlineInvoiceDetails;
            }
            //???????????????,???????????????????????????????????????.
            if(ResultCode.UNKNOWN_ERROR.getCode().equals(baseResult.getCode())){
                return baseResult;
            }
        }
        if (onlineInvoice != null) {
            saveOrUpdateOnlineInvoice(invoiceStatus, onlineInvoice, loginAppUser);
            saveOrUpdateOnlineInvoiceDetails(onlineInvoiceDetails, onlineInvoice);
            saveOrUpdateOnlineInvoicePunishes(onlineInvoicePunishes, onlineInvoice);
            saveOrUpdateOnlineInvoiceAdvances(onlineInvoiceAdvances, onlineInvoice);
            if (CollectionUtils.isNotEmpty(fileuploads)) {
                fileCenterClient.bindingFileupload(fileuploads, onlineInvoice.getOnlineInvoiceId());
            }
        }
        BaseResult<Long> baseResult2 = new BaseResult<>();
        BeanUtils.copyProperties(baseResult, baseResult2);
        baseResult2.setData(onlineInvoice.getOnlineInvoiceId());
        return baseResult2;
    }

    private void checkBeforeAdjustInvoiceAmount(List<OnlineInvoiceDetail> onlineInvoiceDetails) {

    }

    private void saveOrUpdateOnlineInvoiceAdvances(List<OnlineInvoiceAdvance> onlineInvoiceAdvances, OnlineInvoice onlineInvoice) {
        if (CollectionUtils.isNotEmpty(onlineInvoiceAdvances)) {
            for (OnlineInvoiceAdvance onlineInvoiceAdvance : onlineInvoiceAdvances) {
                if (onlineInvoiceAdvance == null) continue;
                if (onlineInvoiceAdvance.getOnlineInvoiceAdvanceId() == null) {
                    onlineInvoiceAdvance.setOnlineInvoiceAdvanceId(IdGenrator.generate())
                            .setOnlineInvoiceId(onlineInvoice.getOnlineInvoiceId());
//                            .setTotalChargeOffAmount(onlineInvoiceAdvance.getTotalChargeOffAmount().add(onlineInvoiceAdvance.getChargeOffAmount()))
                    iOnlineInvoiceAdvanceService.save(onlineInvoiceAdvance);
                } else {
//                    OnlineInvoiceAdvance byId = iOnlineInvoiceAdvanceService.getById(onlineInvoiceAdvance.getOnlineInvoiceAdvanceId());
//                    BigDecimal oldChargeOffAmount = byId.getChargeOffAmount();
//                    BigDecimal oldTotalChargeOffAmount = byId.getTotalChargeOffAmount();
//                    BigDecimal newTotalChargeOffAmount = oldTotalChargeOffAmount.subtract(oldChargeOffAmount);
//                    onlineInvoiceAdvance.setTotalChargeOffAmount(newTotalChargeOffAmount.add(onlineInvoiceAdvance.getChargeOffAmount()));
                    iOnlineInvoiceAdvanceService.updateById(onlineInvoiceAdvance);
                }
//                //???????????????????????????
//                if (OnlineInvoiceType.BUYER_INVOICE.name().equals(onlineInvoice.getOnlineInvoiceType())) {
//                    pmClient.setQuote(onlineInvoiceAdvance.getAdvanceApplyHeadId(), YesOrNo.YES.getValue());
//                }
            }
        }
    }

    private void saveOrUpdateOnlineInvoicePunishes(List<OnlineInvoicePunish> onlineInvoicePunishes, OnlineInvoice onlineInvoice) {
        if (CollectionUtils.isNotEmpty(onlineInvoicePunishes)) {
            for (OnlineInvoicePunish onlineInvoicePunish : onlineInvoicePunishes) {
                if (onlineInvoicePunish == null) continue;
                if (onlineInvoicePunish.getOnlineInvoicePunishId() == null) {
                    onlineInvoicePunish.setOnlineInvoicePunishId(IdGenrator.generate())
                            .setOnlineInvoiceId(onlineInvoice.getOnlineInvoiceId());
                    iOnlineInvoicePunishService.save(onlineInvoicePunish);
                } else {
                    iOnlineInvoicePunishService.updateById(onlineInvoicePunish);
                }
                //????????????????????????????????????????????????
                updateVendorPunishStatus(onlineInvoice, onlineInvoicePunish);
            }
        }
    }

    private void updateVendorPunishStatus(OnlineInvoice onlineInvoice, OnlineInvoicePunish onlineInvoicePunish) {
        //?????????
        if (YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {
            if (InvoiceStatus.DRAFT.name().equals(onlineInvoice.getInvoiceStatus())) {
                performanceClient.modify(new VendorAssesForm()
                        .setVendorAssesId(onlineInvoicePunish.getVendorAssesId())
                        .setIfQuote(YesOrNo.YES.getValue()));
            }
            if (InvoiceStatus.APPROVAL.name().equals(onlineInvoice.getInvoiceStatus())) {
                performanceClient.modify(new VendorAssesForm()
                        .setVendorAssesId(onlineInvoicePunish.getVendorAssesId())
                        .setIfQuote(YesOrNo.YES.getValue())
                        .setStatus(VendorAssesFormStatus.SETTLED.getKey()));
            }
            if (InvoiceStatus.UNDER_APPROVAL.name().equals(onlineInvoice.getInvoiceStatus())) {
                performanceClient.modify(new VendorAssesForm()
                        .setVendorAssesId(onlineInvoicePunish.getVendorAssesId())
                        .setIfQuote(YesOrNo.YES.getValue()));
            }
        }
        //????????????
        if (YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
            if (InvoiceStatus.DRAFT.name().equals(onlineInvoice.getInvoiceStatus())) {
                iInvoicePunishService.updateById(new InvoicePunish()
                        .setInvoicePunishId(onlineInvoicePunish.getInvoicePunishId())
                        .setIfQuote(YesOrNo.YES.getValue()));
            }
            if (InvoiceStatus.APPROVAL.name().equals(onlineInvoice.getInvoiceStatus())) {
                iInvoicePunishService.updateById(new InvoicePunish()
                        .setInvoicePunishId(onlineInvoicePunish.getInvoicePunishId())
                        .setIfQuote(YesOrNo.YES.getValue()));
            }
            if (InvoiceStatus.UNDER_APPROVAL.name().equals(onlineInvoice.getInvoiceStatus())) {
                iInvoicePunishService.updateById(new InvoicePunish()
                        .setInvoicePunishId(onlineInvoicePunish.getInvoicePunishId())
                        .setIfQuote(YesOrNo.YES.getValue()));
            }
        }
    }

    private void saveOrUpdateOnlineInvoiceDetails(List<OnlineInvoiceDetail> onlineInvoiceDetails, OnlineInvoice onlineInvoice) {
        if (CollectionUtils.isNotEmpty(onlineInvoiceDetails)) {
//            adjustInvoiceAmount(onlineInvoiceDetails, onlineInvoice);//??????????????????(????????????????????????)
            for (OnlineInvoiceDetail onlineInvoiceDetail : onlineInvoiceDetails) {
                if (onlineInvoiceDetail == null) continue;
                //??????????????????
                if (Objects.nonNull(onlineInvoiceDetail.getContractNo())) {
                    onlineInvoice.setContractCode(onlineInvoiceDetail.getContractNo());
                }
                if (onlineInvoiceDetail.getOnlineInvoiceDetailId() == null) {
                    if (InvoiceStatus.DRAFT.name().equals(onlineInvoice.getInvoiceStatus())) {
                        Long invoiceDetailId = onlineInvoiceDetail.getInvoiceDetailId();
                        BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getInvoiceQuantity();//??????????????????
//                        BigDecimal notInvoiceQuantity = onlineInvoiceDetail.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getNotInvoiceQuantity();//???????????????
                        BigDecimal unitPriceExcludingTax = onlineInvoiceDetail.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getUnitPriceExcludingTax();//?????????????????????(????????????)
                        BigDecimal unitPriceContainingTax = onlineInvoiceDetail.getUnitPriceContainingTax() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getUnitPriceContainingTax();//???????????????)
                        onlineInvoiceDetail.setOnlineInvoiceDetailId(IdGenrator.generate())
                                .setOnlineInvoiceId(onlineInvoice.getOnlineInvoiceId());
//                        BigDecimal noTaxAmount = invoiceQuantity.multiply(unitPriceExcludingTax);//????????????(??????)
//                        onlineInvoiceDetail.setNoTaxAmount(noTaxAmount);
//                        BigDecimal taxAmount = invoiceQuantity.multiply(unitPriceContainingTax);//????????????
//                        onlineInvoiceDetail.setTaxAmount(taxAmount);
//                        BigDecimal tax = taxAmount.subtract(noTaxAmount);
//                        onlineInvoiceDetail.setTax(tax);
                        //??????????????????????????????????????????????????????????????????(???????????????????????????,????????????????????????) ToDo ????????????

                        checkAndReduceNotInvoiceQuantity(onlineInvoice, onlineInvoiceDetail, invoiceDetailId, invoiceQuantity);
                    }
                    iOnlineInvoiceDetailService.save(onlineInvoiceDetail);
                } else {
                    iOnlineInvoiceDetailService.updateById(onlineInvoiceDetail);
                }
            }
        }
    }

    private synchronized void checkAndReduceNotInvoiceQuantity(OnlineInvoice onlineInvoice, OnlineInvoiceDetail onlineInvoiceDetail, Long invoiceDetailId, BigDecimal invoiceQuantity) {
        if (YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {//???????????????????????????
            OrderDetail byId = iOrderDetailService.getById(onlineInvoiceDetail.getOrderDetailId());
            BigDecimal notInvoiceQuantity = BigDecimal.ZERO;
            if (byId != null) {
                notInvoiceQuantity = byId.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : byId.getNotInvoiceQuantity();
                int r = notInvoiceQuantity.compareTo(BigDecimal.ZERO);
                if (r == 0) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????!"));
                }
            }
            iOrderDetailService.updateById(new OrderDetail()
                    .setOrderDetailId(onlineInvoiceDetail.getOrderDetailId())
                    .setInvoiceQuantity(notInvoiceQuantity.subtract(invoiceQuantity))//??????????????????
                    .setNotInvoiceQuantity(notInvoiceQuantity.subtract(invoiceQuantity)));//???????????????
//                                    .setNoTaxAmount((notInvoiceQuantity.subtract(invoiceQuantity)).multiply(unitPriceExcludingTax)));//????????????,?????????????????????????????????*???????????????
        }
        if (YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {//??????????????????????????????  ToDo ????????????
            InvoiceDetail byId = iInvoiceDetailService.getById(onlineInvoiceDetail.getInvoiceDetailId());
            BigDecimal notInvoiceQuantity = BigDecimal.ZERO;
            if (byId != null) {
                notInvoiceQuantity = byId.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : byId.getNotInvoiceQuantity();
                int r = notInvoiceQuantity.compareTo(BigDecimal.ZERO);
                if (r == 0) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????!"));
                }
            }
            BigDecimal newNotInvoiceQuantity = notInvoiceQuantity.subtract(invoiceQuantity);
//                            BigDecimal taxAmount = newNotInvoiceQuantity.multiply(unitPriceContainingTax);
//                            BigDecimal noTaxAmount = newNotInvoiceQuantity.multiply(unitPriceExcludingTax);
            iInvoiceDetailService.updateById(new InvoiceDetail()
                    .setInvoiceDetailId(invoiceDetailId)
                    .setInvoiceQuantity(newNotInvoiceQuantity)
                    .setNotInvoiceQuantity(newNotInvoiceQuantity));
//                                    .setTaxAmount(newNotInvoiceQuantity.multiply(unitPriceContainingTax))
//                                    .setTax(taxAmount.subtract(noTaxAmount))
//                                    .setNoTaxAmount(noTaxAmount));//????????????,?????????????????????????????????*???????????????
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public BaseResult adjustInvoiceAmount(List<OnlineInvoiceDetail> onlineInvoiceDetails, OnlineInvoice onlineInvoice, List<OnlineInvoicePunish> onlineInvoicePunishes) {
        BaseResult<List<OnlineInvoiceDetail>> baseResult = BaseResult.buildSuccess();
        baseResult.setData(onlineInvoiceDetails);
        if (CollectionUtils.isNotEmpty(onlineInvoiceDetails)) {
            if(Objects.isNull(onlineInvoice)){
                log.error("???????????????????????????????????????????????????????????????!!!!!!!!!!!!!!!!");
            }
            BigDecimal actualInvoiceAmountN = onlineInvoice.getActualInvoiceAmountN() == null ? BigDecimal.ZERO : onlineInvoice.getActualInvoiceAmountN();//??????????????????
            BigDecimal taxTotalAmount = onlineInvoice.getTaxTotalAmount() == null ? BigDecimal.ZERO : onlineInvoice.getTaxTotalAmount();//????????????(??????)
            BigDecimal totalTax = onlineInvoice.getTotalTax() == null ? BigDecimal.ZERO : onlineInvoice.getTotalTax();//??????(??????)
            BigDecimal actualInvoiceAmountY = onlineInvoice.getActualInvoiceAmountY() == null ? BigDecimal.ZERO : onlineInvoice.getActualInvoiceAmountY();//????????????
            BigDecimal noTaxTotalAmount = taxTotalAmount.subtract(totalTax);//???????????????(??????)
            BigDecimal differenceTotalAmount = BigDecimal.ZERO;
            //????????????????????????,??????????????????????????????
            if (YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
                differenceTotalAmount = actualInvoiceAmountN.subtract(noTaxTotalAmount);//???????????????????????????
            }
            if (YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())) {
                differenceTotalAmount = actualInvoiceAmountY.subtract(taxTotalAmount);//????????????????????????
            }
            log.info("---------------------????????????????????????-" +
                            "??????????????????:{};????????????:{};????????????????????????:{} ; ??????(??????):{} ; ???????????????(??????):{} ; ???????????????:{}??????????????????{} " ,
                    onlineInvoice.getActualInvoiceAmountY() ,
                    taxTotalAmount,
                    taxTotalAmount ,
                    totalTax ,
                    noTaxTotalAmount,
                    differenceTotalAmount,
                    actualInvoiceAmountN);
            //????????????????????????
            if (YesOrNo.NO.getValue().equals(onlineInvoice.getIsService())) {
                if (noServiceAdjust(onlineInvoiceDetails, onlineInvoice, baseResult, differenceTotalAmount))
                    return baseResult;
            }
            //?????????????????????
            if (YesOrNo.YES.getValue().equals(onlineInvoice.getIsService())
                    && onlineInvoice.getTaxTotalAmount().compareTo(onlineInvoice.getActualInvoiceAmountY()) != 0) {
                if (serviceAdjust(onlineInvoiceDetails, onlineInvoice, baseResult, differenceTotalAmount)) {
                    return baseResult;
                }
            }
            //??????????????????????????????????????????N
            onlineInvoiceDetails.forEach(onlineInvoiceDetail -> {
                if (!onlineInvoiceDetail.getPoUnitPriceChangeFlag().equals(YesOrNo.YES.getValue())) {
                    onlineInvoiceDetail.setPoUnitPriceChangeFlag(YesOrNo.NO.getValue());
                }
            });
        }
        return baseResult;
        //????????????,?????????????????????????????????
//        onlineInvoice.setTotalTax(onlineInvoice.getInvoiceTax())
//                .setTaxTotalAmount(onlineInvoice.getActualInvoiceAmountY())
//                .setUnPaidAmount(onlineInvoice.getActualInvoiceAmountY());
    }

    private boolean serviceAdjust(List<OnlineInvoiceDetail> onlineInvoiceDetails, OnlineInvoice onlineInvoice, BaseResult<List<OnlineInvoiceDetail>> baseResult, BigDecimal differenceTotalAmount) {
        //???????????????,????????????????????????,??????????????????????????????
        Boolean ifAdjust = true;
        if (BigDecimal.ZERO.compareTo(differenceTotalAmount) == 0) {
            ifAdjust = false;
        }
        if (BigDecimal.ZERO.compareTo(differenceTotalAmount) != 0) {
            if (CollectionUtils.isNotEmpty(onlineInvoiceDetails)) {
                for (OnlineInvoiceDetail onlineInvoiceDetail : onlineInvoiceDetails) {
                    log.info("???????????????????????????");
                    BigDecimal adjustAmount = BigDecimal.ZERO;
                    //??????????????????0??????
                    if (BigDecimal.ZERO.compareTo(differenceTotalAmount) == 0){
                        break;
                    }
                    //???????????????????????????0??????
                    adjustAmount = serviceLessThanZeroForNo(differenceTotalAmount, onlineInvoiceDetail, adjustAmount);
                    //???????????????????????????0??????
                    adjustAmount = serviceGreaterThanZeroForNo(differenceTotalAmount, onlineInvoiceDetail, adjustAmount);
                    differenceTotalAmount = adjustAmount;
                }
            }
        }
        //??????????????????????????????????????????N
        onlineInvoiceDetails.forEach(onlineInvoiceDetail -> {
            if (!onlineInvoiceDetail.getPoUnitPriceChangeFlag().equals(YesOrNo.YES.getValue())) {
                onlineInvoiceDetail.setPoUnitPriceChangeFlag(YesOrNo.NO.getValue());
            }
        });
        log.info("--------------------------------??????????????????????????????:" + JsonUtil.entityToJsonStr(onlineInvoiceDetails));

        BigDecimal sumTaxAmount = onlineInvoiceDetails.stream().map(x ->
                new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
        //??????????????????,????????????????????????????????????
        BigDecimal maxTaxAmount = BigDecimal.ZERO;
        if (onlineInvoiceDetails.size() > 1) {
            maxTaxAmount = onlineInvoiceDetails.stream().map(OnlineInvoiceDetail::getTaxAmount).max((x1, x2) -> x1.compareTo(x2)).get();
        } else if (onlineInvoiceDetails.size() == 1) {
            maxTaxAmount = onlineInvoiceDetails.get(0).getTaxAmount();
        }
        BigDecimal actualInvoiceAmountN = onlineInvoice.getActualInvoiceAmountN();
        log.info("--------maxTaxAmount:" + maxTaxAmount + "--------------------------" +
                "sumTaxAmount:" + sumTaxAmount + "------------------------" +
                "actualInvoiceAmountN:" + actualInvoiceAmountN);
        BigDecimal actualInvoiceAmountY = onlineInvoice.getActualInvoiceAmountY() == null ? BigDecimal.ZERO : onlineInvoice.getActualInvoiceAmountY();
        actualInvoiceAmountY = actualInvoiceAmountY.setScale(2, BigDecimal.ROUND_HALF_UP);
        /*???????????????????????????????????????*/
        BigDecimal lastDifferentTaxAmount = actualInvoiceAmountY.subtract(sumTaxAmount);//???????????????
        log.info("--------------????????????:" + lastDifferentTaxAmount);
        if (lastDifferentTaxAmount.compareTo(BigDecimal.ZERO) != 0) {
            for (OnlineInvoiceDetail newOnlineInvoiceDetail : onlineInvoiceDetails) {
                BigDecimal newTaxAmount = newOnlineInvoiceDetail.getTaxAmount().add(lastDifferentTaxAmount);
                if (newOnlineInvoiceDetail.getTaxAmount().compareTo(maxTaxAmount) == 0) {
                    newOnlineInvoiceDetail
                            .setUnitPriceContainingTax(newTaxAmount.divide(newOnlineInvoiceDetail.getInvoiceQuantity(), 8, BigDecimal.ROUND_HALF_UP))
                            .setTaxAmount(newTaxAmount);
                    break;
                }
            }
        }
        /*????????????????????????????????????????????????*/
        BigDecimal newSumNoTaxAmount = onlineInvoiceDetails.stream().map(x ->
                new BigDecimal(StringUtil.StringValue(x.getNoTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
        /*?????????????????????????????????????????????*/
        BigDecimal newSumTaxAmount = onlineInvoiceDetails.stream().map(x ->
                new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
        actualInvoiceAmountY = onlineInvoice.getActualInvoiceAmountY() == null ? BigDecimal.ZERO : onlineInvoice.getActualInvoiceAmountY();
        actualInvoiceAmountY = actualInvoiceAmountY.setScale(2, BigDecimal.ROUND_HALF_UP);
        log.info("newSumNoTaxAmount:" + newSumNoTaxAmount + "------------------actualInvoiceAmountN:" + actualInvoiceAmountN);
        log.info("newSumTaxAmount:" + newSumTaxAmount + "------------------actualInvoiceAmountY:" + actualInvoiceAmountY);
        baseResult.setData(onlineInvoiceDetails);
        if (newSumTaxAmount.compareTo(actualInvoiceAmountY) != 0 && ifAdjust) {
            baseResult.setCode(ResultCode.UNKNOWN_ERROR.getCode());
            log.error("????????????????????????????????????????????????????????????,?????????!");
            baseResult.setMessage(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????????????????,?????????!"));
            return true;
        }
        return false;
    }

    private BigDecimal serviceGreaterThanZeroForNo(BigDecimal differenceTotalAmount, OnlineInvoiceDetail onlineInvoiceDetail, BigDecimal adjustAmount) {
        if (differenceTotalAmount.compareTo(BigDecimal.ZERO) == 1) {
            adjustAmount = differenceTotalAmount;//?????????????????????????????????
            serviceCalculateData(adjustAmount, onlineInvoiceDetail);
            adjustAmount = BigDecimal.ZERO;
        }
        log.info("??????{} ??????????????????{}:====================================>", JSONObject.toJSONString(onlineInvoiceDetail) , adjustAmount);
        return adjustAmount;
    }

    private BigDecimal serviceLessThanZeroForNo(BigDecimal differenceTotalAmount, OnlineInvoiceDetail onlineInvoiceDetail, BigDecimal adjustAmount) {
        if (differenceTotalAmount.compareTo(BigDecimal.ZERO) == -1) {
            BigDecimal taxAmount = onlineInvoiceDetail.getTaxAmount();
            //????????????:???????????????????????????????????????,????????????0
            if (taxAmount.abs().compareTo(differenceTotalAmount.abs()) < 1) {
                serviceCalculateDatasBySetZero(onlineInvoiceDetail, taxAmount);
                //??????????????????????????????
                if (taxAmount.abs().compareTo(differenceTotalAmount.abs()) == -1) {
                    adjustAmount = differenceTotalAmount.add(taxAmount);
                }
                //??????????????????????????????
                if (taxAmount.abs().compareTo(differenceTotalAmount.abs()) == 0) {
                    adjustAmount = BigDecimal.ZERO;
                }
            }
            //????????????,??????????????????????????????
            if (taxAmount.abs().compareTo(differenceTotalAmount.abs()) == 1) {
                adjustAmount = differenceTotalAmount;//???????????????????????????????????????
                serviceCalculateData(adjustAmount, onlineInvoiceDetail);
                adjustAmount = BigDecimal.ZERO;
            }
//            //????????????,??????????????????????????????
//            if (noTaxAmount.compareTo(differenceTotalAmount.abs()) == -1) {
//                BigDecimal adjustAmount = onlineInvoiceDetail.getNoTaxAmount().negate();
//                calculateData(adjustAmount, onlineInvoiceDetail);
//                differenceTotalAmount = differenceTotalAmount.subtract(adjustAmount);
//            }
        }
        log.info("??????{} ?????????????????????:{}====================================>" ,JSONObject.toJSONString(onlineInvoiceDetail) , adjustAmount);
        return adjustAmount;
    }

    private BigDecimal serviceCalculateData(BigDecimal adjustAmount, OnlineInvoiceDetail onlineInvoiceDetail) {
        BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity();//??????????????????
        BigDecimal unitPriceExcludingTax = onlineInvoiceDetail.getUnitPriceExcludingTax();//???????????????,??????????????????)
        BigDecimal compareResult = adjustAmount.divide(invoiceQuantity, 2, BigDecimal.ROUND_HALF_UP);//????????????(????????????????????????)
        onlineInvoiceDetail.setCompareResult(StringUtil.StringValue(compareResult))
                .setPoUnitPriceChangeFlag(adjustAmount.compareTo(BigDecimal.ZERO) == 0 ? YesOrNo.NO.getValue() : YesOrNo.YES.getValue());//??????????????????
        BigDecimal newTaxAmount = adjustAmount.add(onlineInvoiceDetail.getTaxAmount()).setScale(2, BigDecimal.ROUND_HALF_UP);//????????????????????????
        BigDecimal newUnitPriceContainingTax = newTaxAmount.divide(invoiceQuantity, 8, BigDecimal.ROUND_HALF_UP);//????????????????????????
        onlineInvoiceDetail.setUnitPriceContainingTax(newUnitPriceContainingTax);
        BigDecimal taxRate = onlineInvoiceDetail.getTaxRate().divide(new BigDecimal("100"));
        BigDecimal newNoTaxAmount = newTaxAmount.divide((BigDecimal.ONE.add(taxRate)), 2, BigDecimal.ROUND_HALF_UP);//????????????????????????
        BigDecimal newTax = newTaxAmount.subtract(newNoTaxAmount);//????????????????????????
        BigDecimal newUnitPriceExcludingTax = newNoTaxAmount.divide(invoiceQuantity, 8, BigDecimal.ROUND_HALF_UP);//????????????????????????
        onlineInvoiceDetail.setNoTaxAmount(newNoTaxAmount).setTax(newTax).setTaxAmount(newTaxAmount).setUnitPriceExcludingTax(newUnitPriceExcludingTax);
        return adjustAmount;
    }

    private void serviceCalculateDatasBySetZero(OnlineInvoiceDetail onlineInvoiceDetail, BigDecimal taxAmount) {
        onlineInvoiceDetail.setUnitPriceExcludingTax(BigDecimal.ZERO);//???????????????,??????????????????)
        onlineInvoiceDetail.setNoTaxAmount(BigDecimal.ZERO);//????????????
        onlineInvoiceDetail.setTax(BigDecimal.ZERO);//??????
        onlineInvoiceDetail.setUnitPriceContainingTax(BigDecimal.ZERO);//????????????
        onlineInvoiceDetail.setTaxAmount(BigDecimal.ZERO);//????????????
        BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity();
        BigDecimal compareResult = taxAmount.negate().divide(invoiceQuantity, 4, BigDecimal.ROUND_HALF_UP);
        onlineInvoiceDetail.setCompareResult(StringUtil.StringValue(compareResult))
                .setPoUnitPriceChangeFlag(compareResult.compareTo(BigDecimal.ZERO) == 0 ? YesOrNo.NO.getValue() : YesOrNo.YES.getValue());//??????????????????
    }

    private boolean noServiceAdjust(List<OnlineInvoiceDetail> onlineInvoiceDetails, OnlineInvoice onlineInvoice, BaseResult<List<OnlineInvoiceDetail>> baseResult, BigDecimal differenceTotalAmount) {
        //????????????:??????????????????,??????,??????????????????????????????
        //(???????????????????????????:
        // 1.??????????????????????????????+???????????????????????????=???????????????????????????????????????(?????????)
        // 2.??????????????????????????????????????????????????=??????????????????
        // 3.??????????????????+??????????????????=?????????????????????
        // 4.?????????????????????????????????????????????
        // 5.???????????????????????????,??????????????????????????????
        // 6.????????????????????????????????????????????????,??????????????????,?????????????????????????????????????????????????????????????????????)
        Map<String, List<OnlineInvoiceDetail>> collect = onlineInvoiceDetails.stream()
                .filter(onlineInvoiceDetail -> (StringUtils.isNotBlank(onlineInvoiceDetail.getOrderNumber()) && !ObjectUtils.isEmpty(onlineInvoiceDetail.getLineNum()) && StringUtils.isNotBlank(onlineInvoiceDetail.getReceiveOrderNo())))
                .collect(Collectors.groupingBy(onlineInvoiceDetail -> onlineInvoiceDetail.getOrderNumber() + onlineInvoiceDetail.getReceiveOrderNo() + onlineInvoiceDetail.getLineNum()));
        Collection<List<OnlineInvoiceDetail>> values = collect.values();
        List<List<OnlineInvoiceDetail>> nolists = values.stream().filter(list -> (list.size() < 2)).collect(Collectors.toList());//?????????????????????
        List<List<OnlineInvoiceDetail>> yeslists = values.stream().filter(list -> (list.size() > 1)).collect(Collectors.toList());//??????????????????
        List<OnlineInvoiceDetail> noList = new ArrayList<>();
        List<OnlineInvoiceDetail> yesList = new ArrayList<>();
        List<OnlineInvoiceDetail> newOnlineInvoiceDetails = new ArrayList<>();
        List<OnlineInvoiceDetail> allOnlineInvoiceDetails = new ArrayList<>();
        //???????????????,????????????????????????,??????????????????????????????
        Boolean ifAdjust = true;
        if (BigDecimal.ZERO.compareTo(differenceTotalAmount) == 0) {
            newOnlineInvoiceDetails = onlineInvoiceDetails;
            ifAdjust = false;
        }
        if (BigDecimal.ZERO.compareTo(differenceTotalAmount) != 0) {
            //1.?????????????????????????????????
            if (CollectionUtils.isNotEmpty(nolists)) {
                nolists.forEach(o -> {
                    noList.add(o.get(0));
                });
                noList.sort((o1, o2) -> Integer.compare(o1.getInvoiceRow(), o2.getInvoiceRow()));//??????
                for (OnlineInvoiceDetail onlineInvoiceDetail : noList) {
                    log.info("??????????????????????????????");
                    BigDecimal adjustAmount = BigDecimal.ZERO;
                    //??????????????????0??????
                    if (BigDecimal.ZERO.compareTo(differenceTotalAmount) == 0){
                        break;
                    }
                    //??????????????????0??????
                    adjustAmount = lessThanZeroForNo(differenceTotalAmount, onlineInvoiceDetail, adjustAmount);
                    //??????????????????0??????
                    adjustAmount = greaterThanZeroForNo(differenceTotalAmount, onlineInvoiceDetail, adjustAmount);
                    differenceTotalAmount = adjustAmount;
                }
            }
            //2.??????????????????????????????
            if (CollectionUtils.isNotEmpty(yeslists)) {
                //??????????????????
                for (List<OnlineInvoiceDetail> list : yeslists) {
                    BigDecimal adjustAmount = BigDecimal.ZERO;
                    //??????????????????0??????,????????????????????????
                    if (differenceTotalAmount.compareTo(BigDecimal.ZERO) == 0) break;
                    //??????????????????0??????
                    adjustAmount = lessThanZeroForYes(differenceTotalAmount, list, adjustAmount);
                    log.info("??????????????????????????????-??????????????????0?????????adjustAmount={}",adjustAmount);
                    //??????????????????0??????
                    adjustAmount = greaterThanZeroForYes(differenceTotalAmount, list, adjustAmount);
                    log.info("??????????????????????????????-??????????????????0?????????adjustAmount={}",adjustAmount);
                    differenceTotalAmount = adjustAmount;
                    yesList.addAll(list);
                }
                for (List<OnlineInvoiceDetail> list : yeslists) {
                    allOnlineInvoiceDetails.addAll(list);
                }
                Iterator<OnlineInvoiceDetail> iterator = allOnlineInvoiceDetails.iterator();
                while (iterator.hasNext()) {
                    OnlineInvoiceDetail next = iterator.next();
                    if (yesList.contains(next)) {
                        iterator.remove();
                    }
                }
                allOnlineInvoiceDetails.addAll(yesList);
            }
            //?????????????????????????????????0???,??????
//            if (CollectionUtils.isEmpty(nolists)) {
//                log.info("???????????????receive???,?????????!");
//                baseResult.setCode(ResultCode.UNKNOWN_ERROR.getCode());
//                baseResult.setMessage(LocaleHandler.getLocaleMsg("???????????????receive???,?????????!"));
//                baseResult.setData(newOnlineInvoiceDetails);
//                return true;
//            }
        }
        newOnlineInvoiceDetails.addAll(noList);
        newOnlineInvoiceDetails.addAll(allOnlineInvoiceDetails);
        newOnlineInvoiceDetails.sort((o1, o2) -> Integer.compare(o1.getInvoiceRow(), o2.getInvoiceRow()));//??????
        //??????????????????????????????????????????N
        newOnlineInvoiceDetails.forEach(onlineInvoiceDetail -> {
            if (!onlineInvoiceDetail.getPoUnitPriceChangeFlag().equals(YesOrNo.YES.getValue())) {
                onlineInvoiceDetail.setPoUnitPriceChangeFlag(YesOrNo.NO.getValue());
            }
        });
        log.info("--------------------------------??????????????????????????????:" + JsonUtil.entityToJsonStr(newOnlineInvoiceDetails));
//            BigDecimal sumNoTaxAmount = newOnlineInvoiceDetails.stream().map(x ->
//                    new BigDecimal(StringUtil.StringValue(x.getNoTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);

        /*????????????????????????,???????????????????????????????????????*/
        BigDecimal sumNoTaxAmount = getSumNoTaxAmount(onlineInvoiceDetails);
        //??????????????????,????????????????????????????????????
        BigDecimal maxNoTaxAmount = BigDecimal.ZERO;
        if (noList.size() > 1) {
            maxNoTaxAmount = noList.stream().map(OnlineInvoiceDetail::getNoTaxAmount).max((x1, x2) -> x1.compareTo(x2)).get();
        } else if (noList.size() == 1) {
            maxNoTaxAmount = newOnlineInvoiceDetails.get(0).getNoTaxAmount();
        }
        log.info("--------maxNoTaxAmount:" + maxNoTaxAmount + "--------------------------" +
                "sumNoTaxAmount:" + sumNoTaxAmount + "------------------------" +
                "actualInvoiceAmountN:" + onlineInvoice.getActualInvoiceAmountN());
        sumNoTaxAmount = sumNoTaxAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal actualInvoiceAmountN1 = onlineInvoice.getActualInvoiceAmountN() == null ? BigDecimal.ZERO : onlineInvoice.getActualInvoiceAmountN();
        actualInvoiceAmountN1 = actualInvoiceAmountN1.setScale(2, BigDecimal.ROUND_HALF_UP);
        log.info("sumNoTaxAmount:" + sumNoTaxAmount + "------------------actualInvoiceAmountN1:" + actualInvoiceAmountN1);
        /*????????????????????????,????????????????????????????????????*/
        BigDecimal sumTaxAmount = getSumTaxAmount(onlineInvoiceDetails);
        sumTaxAmount = sumTaxAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal actualInvoiceAmountY = onlineInvoice.getActualInvoiceAmountY() == null ? BigDecimal.ZERO : onlineInvoice.getActualInvoiceAmountY();
        actualInvoiceAmountY = actualInvoiceAmountY.setScale(2, BigDecimal.ROUND_HALF_UP);
        log.info("sumTaxAmount:" + sumTaxAmount + "------------------actualInvoiceAmountY:" + actualInvoiceAmountY);

        /*???????????????????????????????????????*/
//            BigDecimal lastDifferentTaxAmount = actualInvoiceAmountY.subtract(sumTaxAmount);//????????????
//            log.info("--------------????????????:" + lastDifferentTaxAmount);
        BigDecimal lastDifferentNoTaxAmount = actualInvoiceAmountN1.subtract(sumNoTaxAmount);//???????????????
        log.info("--------------???????????????:" + lastDifferentNoTaxAmount);
        if (lastDifferentNoTaxAmount.compareTo(BigDecimal.ZERO) != 0) {
            for (OnlineInvoiceDetail newOnlineInvoiceDetail : newOnlineInvoiceDetails) {
                BigDecimal newNoTaxAmount = newOnlineInvoiceDetail.getNoTaxAmount().add(lastDifferentNoTaxAmount);
                BigDecimal newTaxAmount = newOnlineInvoiceDetail.getTaxAmount().add(BigDecimal.ZERO);
                if (newOnlineInvoiceDetail.getNoTaxAmount().compareTo(maxNoTaxAmount) == 0) {
                    newOnlineInvoiceDetail
                            .setNoTaxAmount(newNoTaxAmount)
                            .setUnitPriceExcludingTax(newNoTaxAmount.divide(newOnlineInvoiceDetail.getInvoiceQuantity(), 8, BigDecimal.ROUND_HALF_UP))
                            .setUnitPriceContainingTax(newTaxAmount.divide(newOnlineInvoiceDetail.getInvoiceQuantity(), 8, BigDecimal.ROUND_HALF_UP))
                            .setTaxAmount(newTaxAmount);
                    break;
                }
            }
        }
//            BigDecimal newSumNoTaxAmount = newOnlineInvoiceDetails.stream().map(x ->
//                    new BigDecimal(StringUtil.StringValue(x.getNoTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
//            newSumNoTaxAmount = newSumNoTaxAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
//            BigDecimal newSumTaxAmount = newOnlineInvoiceDetails.stream().map(x ->
//                    new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
//            newSumTaxAmount = newSumTaxAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal newSumNoTaxAmount = getSumNoTaxAmount(newOnlineInvoiceDetails);
        BigDecimal newSumTaxAmount = getSumTaxAmount(newOnlineInvoiceDetails);
        log.info("newSumNoTaxAmount:" + newSumNoTaxAmount + "------------------actualInvoiceAmountN1:" + actualInvoiceAmountN1);
        log.info("newSumTaxAmount:" + newSumTaxAmount + "------------------actualInvoiceAmountY:" + actualInvoiceAmountY);
        baseResult.setData(newOnlineInvoiceDetails);
        for (OnlineInvoiceDetail onlineInvoiceDetail : newOnlineInvoiceDetails) {
            BigDecimal noTaxAmount = onlineInvoiceDetail.getNoTaxAmount() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getNoTaxAmount();//???????????????
            BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getInvoiceQuantity();//??????????????????
            BigDecimal unitPriceExcludingTax = onlineInvoiceDetail.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getUnitPriceExcludingTax();//???????????????
            BigDecimal taxAmount = onlineInvoiceDetail.getTaxAmount() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getTaxAmount();//????????????
            BigDecimal unitPriceContainingTax = onlineInvoiceDetail.getUnitPriceContainingTax() == null ? BigDecimal.ZERO : onlineInvoiceDetail.getUnitPriceContainingTax();//????????????
            boolean noTaxAmountFlag = ((invoiceQuantity.multiply(unitPriceExcludingTax)).setScale(2, BigDecimal.ROUND_HALF_UP)).compareTo(noTaxAmount.setScale(2, BigDecimal.ROUND_HALF_UP)) == 0;
            boolean taxAmountFlag = ((invoiceQuantity.multiply(unitPriceContainingTax)).setScale(2, BigDecimal.ROUND_HALF_UP)).compareTo(taxAmount.setScale(2, BigDecimal.ROUND_HALF_UP)) == 0;
            if (!noTaxAmountFlag && ifAdjust) {
                baseResult.setCode(ResultCode.UNKNOWN_ERROR.getCode());
                baseResult.setMessage(LocaleHandler.getLocaleMsg("???" + onlineInvoiceDetail.getInvoiceRow() + "????????????????????????????????????"));
                return true;
            }
//                if (!taxAmountFlag) {
//                    //????????????
//                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                    baseResult.setCode(ResultCode.UNKNOWN_ERROR.getCode());
//                    baseResult.setMessage(LocaleHandler.getLocaleMsg("???" + onlineInvoiceDetail.getInvoiceRow() + "?????????????????????????????????"));
//                    return baseResult;
//                }
        }
        ;
        if (newSumNoTaxAmount.compareTo(actualInvoiceAmountN1) != 0 && ifAdjust) {
            baseResult.setCode(ResultCode.UNKNOWN_ERROR.getCode());
            log.error("??????????????????????????????????????????????????????,?????????!");
            baseResult.setMessage(LocaleHandler.getLocaleMsg("??????????????????????????????????????????????????????,?????????!"));
            return true;
        }
//            if (newSumTaxAmount.compareTo(actualInvoiceAmountY) != 0 && ifAdjust) {
//                baseResult.setCode(ResultCode.UNKNOWN_ERROR.getCode());
//                log.error("?????????????????????????????????????????????????????????,?????????!");
//                baseResult.setMessage(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????,?????????!"));
//                return baseResult;
//            }
        return false;
    }

    //??????????????????
    private BigDecimal getSumTaxAmount(List<OnlineInvoiceDetail> onlineInvoiceDetails) {
        List<OnlineInvoiceDetail> returnOnlineInvoiceDetails = onlineInvoiceDetails.stream().filter(item -> CeeaWarehousingReturnDetailEnum.RETURN.getValue().equals(item.getType())).map(e-> BeanCopyUtil.copyProperties(e,OnlineInvoiceDetail::new)).collect(Collectors.toList());//??????????????????
        List<OnlineInvoiceDetail> receiveOnlineInvoiceDetails = onlineInvoiceDetails.stream().filter(item -> CeeaWarehousingReturnDetailEnum.RECEIVE.getValue().equals(item.getType())).map(e->BeanCopyUtil.copyProperties(e,OnlineInvoiceDetail::new)).collect(Collectors.toList());//??????????????????
        BigDecimal sumNoTaxAmount = BigDecimal.ZERO;
        BigDecimal sumTax = BigDecimal.ZERO;
        BigDecimal sumTaxAmount = BigDecimal.ZERO;
        if (CollectionUtils.isNotEmpty(returnOnlineInvoiceDetails) && CollectionUtils.isNotEmpty(receiveOnlineInvoiceDetails)) {
            for (OnlineInvoiceDetail returnOnlineInvoiceDetail : returnOnlineInvoiceDetails) {
                if (returnOnlineInvoiceDetail == null) continue;
                for (OnlineInvoiceDetail receiveOnlineInvoiceDetail : receiveOnlineInvoiceDetails) {
                    if (receiveOnlineInvoiceDetail == null) continue;
                    if (returnOnlineInvoiceDetail.getParentTxnId().compareTo(receiveOnlineInvoiceDetail.getTxnId()) == 0) {
                        BigDecimal receiveNum = returnOnlineInvoiceDetail.getReceiveNum();//??????????????????
                        BigDecimal invoiceQuantity = returnOnlineInvoiceDetail.getInvoiceQuantity();//????????????????????????
                        BigDecimal notInvoiceQuantity = returnOnlineInvoiceDetail.getNotInvoiceQuantity();//?????????????????????
                        BigDecimal noTaxAmount = returnOnlineInvoiceDetail.getNoTaxAmount();//??????????????????
                        BigDecimal tax = returnOnlineInvoiceDetail.getTax();//????????????
                        BigDecimal taxAmount = returnOnlineInvoiceDetail.getTaxAmount();//??????????????????
                        BigDecimal receiveNum1 = receiveOnlineInvoiceDetail.getReceiveNum();//??????????????????
                        BigDecimal invoiceQuantity1 = receiveOnlineInvoiceDetail.getInvoiceQuantity();//????????????????????????
                        BigDecimal notInvoiceQuantity1 = receiveOnlineInvoiceDetail.getNotInvoiceQuantity();//?????????????????????
                        BigDecimal noTaxAmount1 = receiveOnlineInvoiceDetail.getNoTaxAmount();//??????????????????
                        BigDecimal tax1 = receiveOnlineInvoiceDetail.getTax();//????????????
                        BigDecimal taxAmount1 = receiveOnlineInvoiceDetail.getTaxAmount();//??????????????????
                        //?????????????????????????????????????????????????????????
                        if(receiveNum == null){
                            receiveNum = BigDecimal.ZERO;
                        }
                        if(invoiceQuantity == null){
                            invoiceQuantity = BigDecimal.ZERO;
                        }
                        if(notInvoiceQuantity == null){
                            notInvoiceQuantity = BigDecimal.ZERO;
                        }
                        if(noTaxAmount == null){
                            noTaxAmount = BigDecimal.ZERO;
                        }
                        if(tax == null){
                            tax = BigDecimal.ZERO;
                        }
                        if(taxAmount == null){
                            taxAmount = BigDecimal.ZERO;
                        }
                        if(receiveNum1 == null){
                            receiveNum1 = BigDecimal.ZERO;
                        }
                        if(invoiceQuantity1 == null){
                            invoiceQuantity1 = BigDecimal.ZERO;
                        }
                        if(notInvoiceQuantity1 == null){
                            notInvoiceQuantity1 = BigDecimal.ZERO;
                        }
                        if(noTaxAmount1 == null){
                            noTaxAmount1 = BigDecimal.ZERO;
                        }
                        if(tax1 == null){
                            tax1 = BigDecimal.ZERO;
                        }
                        if(taxAmount1 == null){
                            taxAmount1 = BigDecimal.ZERO;
                        }
                        BigDecimal newReceiveNum = receiveNum.add(receiveNum1);
                        BigDecimal newInvoiceQuantity = invoiceQuantity.add(invoiceQuantity1);
                        BigDecimal newNotInvoiceQuantity = notInvoiceQuantity.add(notInvoiceQuantity1);
                        BigDecimal unitPriceContainingTax = receiveOnlineInvoiceDetail.getUnitPriceContainingTax();//????????????
                        BigDecimal unitPriceExcludingTax = receiveOnlineInvoiceDetail.getUnitPriceExcludingTax();//???????????????
                        BigDecimal newNoTaxAmount = newInvoiceQuantity.multiply(unitPriceExcludingTax).setScale(2, BigDecimal.ROUND_HALF_UP);//???????????????????????????
                        BigDecimal newTaxAmount = newInvoiceQuantity.multiply(unitPriceContainingTax).setScale(2, BigDecimal.ROUND_HALF_UP);//????????????????????????
                        BigDecimal newTax = newTaxAmount.subtract(newNoTaxAmount).setScale(2, BigDecimal.ROUND_HALF_UP);//??????????????????
                        receiveOnlineInvoiceDetail
                                .setReceiveNum(newReceiveNum)
                                .setInvoiceQuantity(newInvoiceQuantity)
                                .setNotInvoiceQuantity(newNotInvoiceQuantity)
                                .setNoTaxAmount(newNoTaxAmount)
                                .setTax(newTax)
                                .setTaxAmount(newTaxAmount);
                        break;
                    }
                }
            }
        }
        //???return??????:
        if (CollectionUtils.isEmpty(receiveOnlineInvoiceDetails) && CollectionUtils.isNotEmpty(returnOnlineInvoiceDetails)) {
            sumNoTaxAmount = returnOnlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getNoTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
//            sumTax = returnOnlineInvoiceDetails.stream().map(x ->
//                    new BigDecimal(StringUtil.StringValue(x.getTax().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
            sumTaxAmount = returnOnlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
            //???receive??????receive,???return??????:
        } else {
            sumNoTaxAmount = receiveOnlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getNoTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
//            sumTax = receiveOnlineInvoiceDetails.stream().map(x ->
//                    new BigDecimal(StringUtil.StringValue(x.getTax().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
            sumTaxAmount = receiveOnlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return sumTaxAmount;
    }

    //?????????????????????
    private BigDecimal getSumNoTaxAmount(List<OnlineInvoiceDetail> onlineInvoiceDetails) {
        List<OnlineInvoiceDetail> returnOnlineInvoiceDetails = onlineInvoiceDetails.stream().filter(item -> CeeaWarehousingReturnDetailEnum.RETURN.getValue().equals(item.getType())).map(e-> BeanCopyUtil.copyProperties(e,OnlineInvoiceDetail::new)).collect(Collectors.toList());//??????????????????
        List<OnlineInvoiceDetail> receiveOnlineInvoiceDetails = onlineInvoiceDetails.stream().filter(item -> CeeaWarehousingReturnDetailEnum.RECEIVE.getValue().equals(item.getType())).map(e->BeanCopyUtil.copyProperties(e,OnlineInvoiceDetail::new)).collect(Collectors.toList());//??????????????????
        BigDecimal sumNoTaxAmount = BigDecimal.ZERO;
        BigDecimal sumTax = BigDecimal.ZERO;
        BigDecimal sumTaxAmount = BigDecimal.ZERO;

        if (CollectionUtils.isNotEmpty(returnOnlineInvoiceDetails) && CollectionUtils.isNotEmpty(receiveOnlineInvoiceDetails)) {
            for (OnlineInvoiceDetail returnOnlineInvoiceDetail : returnOnlineInvoiceDetails) {
                if (returnOnlineInvoiceDetail == null) continue;
                for (OnlineInvoiceDetail receiveOnlineInvoiceDetail : receiveOnlineInvoiceDetails) {
                    if (receiveOnlineInvoiceDetail == null) continue;
                    if (returnOnlineInvoiceDetail.getParentTxnId().compareTo(receiveOnlineInvoiceDetail.getTxnId()) == 0) {
                        BigDecimal receiveNum = returnOnlineInvoiceDetail.getReceiveNum();//??????????????????
                        BigDecimal invoiceQuantity = returnOnlineInvoiceDetail.getInvoiceQuantity();//????????????????????????
                        BigDecimal notInvoiceQuantity = returnOnlineInvoiceDetail.getNotInvoiceQuantity();//?????????????????????
                        BigDecimal noTaxAmount = returnOnlineInvoiceDetail.getNoTaxAmount();//??????????????????
                        BigDecimal tax = returnOnlineInvoiceDetail.getTax();//????????????
                        BigDecimal taxAmount = returnOnlineInvoiceDetail.getTaxAmount();//??????????????????
                        BigDecimal receiveNum1 = receiveOnlineInvoiceDetail.getReceiveNum();//??????????????????
                        BigDecimal invoiceQuantity1 = receiveOnlineInvoiceDetail.getInvoiceQuantity();//????????????????????????
                        BigDecimal notInvoiceQuantity1 = receiveOnlineInvoiceDetail.getNotInvoiceQuantity();//?????????????????????
                        BigDecimal noTaxAmount1 = receiveOnlineInvoiceDetail.getNoTaxAmount();//??????????????????
                        BigDecimal tax1 = receiveOnlineInvoiceDetail.getTax();//????????????
                        BigDecimal taxAmount1 = receiveOnlineInvoiceDetail.getTaxAmount();//??????????????????
                        //?????????????????????????????????????????????????????????
                        if(receiveNum == null){
                            receiveNum = BigDecimal.ZERO;
                        }
                        if(invoiceQuantity == null){
                            invoiceQuantity = BigDecimal.ZERO;
                        }
                        if(notInvoiceQuantity == null){
                            notInvoiceQuantity = BigDecimal.ZERO;
                        }
                        if(noTaxAmount == null){
                            noTaxAmount = BigDecimal.ZERO;
                        }
                        if(tax == null){
                            tax = BigDecimal.ZERO;
                        }
                        if(taxAmount == null){
                            taxAmount = BigDecimal.ZERO;
                        }
                        if(receiveNum1 == null){
                            receiveNum1 = BigDecimal.ZERO;
                        }
                        if(invoiceQuantity1 == null){
                            invoiceQuantity1 = BigDecimal.ZERO;
                        }
                        if(notInvoiceQuantity1 == null){
                            notInvoiceQuantity1 = BigDecimal.ZERO;
                        }
                        if(noTaxAmount1 == null){
                            noTaxAmount1 = BigDecimal.ZERO;
                        }
                        if(tax1 == null){
                            tax1 = BigDecimal.ZERO;
                        }
                        if(taxAmount1 == null){
                            taxAmount1 = BigDecimal.ZERO;
                        }
                        BigDecimal newReceiveNum = receiveNum.add(receiveNum1);
                        BigDecimal newInvoiceQuantity = invoiceQuantity.add(invoiceQuantity1);
                        BigDecimal newNotInvoiceQuantity = notInvoiceQuantity.add(notInvoiceQuantity1);
                        BigDecimal unitPriceContainingTax = receiveOnlineInvoiceDetail.getUnitPriceContainingTax();//????????????
                        BigDecimal unitPriceExcludingTax = receiveOnlineInvoiceDetail.getUnitPriceExcludingTax();//???????????????
                        BigDecimal newNoTaxAmount = newInvoiceQuantity.multiply(unitPriceExcludingTax).setScale(2, BigDecimal.ROUND_HALF_UP);//???????????????????????????
                        BigDecimal newTaxAmount = newInvoiceQuantity.multiply(unitPriceContainingTax).setScale(2, BigDecimal.ROUND_HALF_UP);//????????????????????????
                        BigDecimal newTax = newTaxAmount.subtract(newNoTaxAmount).setScale(2, BigDecimal.ROUND_HALF_UP);//??????????????????
                        receiveOnlineInvoiceDetail
                                .setReceiveNum(newReceiveNum)
                                .setInvoiceQuantity(newInvoiceQuantity)
                                .setNotInvoiceQuantity(newNotInvoiceQuantity)
                                .setNoTaxAmount(newNoTaxAmount)
                                .setTax(newTax)
                                .setTaxAmount(newTaxAmount);
                        break;
                    }
                }
            }
        }
        //???return??????:
        if (CollectionUtils.isEmpty(receiveOnlineInvoiceDetails) && CollectionUtils.isNotEmpty(returnOnlineInvoiceDetails)) {
            sumNoTaxAmount = returnOnlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getNoTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
//            sumTax = returnOnlineInvoiceDetails.stream().map(x ->
//                    new BigDecimal(StringUtil.StringValue(x.getTax().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
            sumTaxAmount = returnOnlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
            //???receive??????receive,???return??????:
        } else {
            sumNoTaxAmount = receiveOnlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getNoTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
//            sumTax = receiveOnlineInvoiceDetails.stream().map(x ->
//                    new BigDecimal(StringUtil.StringValue(x.getTax().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
            sumTaxAmount = receiveOnlineInvoiceDetails.stream().map(x ->
                    new BigDecimal(StringUtil.StringValue(x.getTaxAmount().setScale(2, BigDecimal.ROUND_HALF_UP)))).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return sumNoTaxAmount;
    }


    private BigDecimal greaterThanZeroForYes(BigDecimal differenceTotalAmount, List<OnlineInvoiceDetail> yeslist, BigDecimal adjustAmount) {
        if (differenceTotalAmount.compareTo(BigDecimal.ZERO) == 1) {
            //???????????????????????????
            BigDecimal totalInvoiceQuantity = BigDecimal.ZERO;//?????????????????????
            BigDecimal totalNoTaxAmount = BigDecimal.ZERO;//????????????????????????
            BigDecimal totalTaxAmount = BigDecimal.ZERO;//????????????????????????
            for (OnlineInvoiceDetail onlineInvoiceDetail : yeslist) {
                totalInvoiceQuantity = totalInvoiceQuantity.add(onlineInvoiceDetail.getInvoiceQuantity());
                totalNoTaxAmount = totalNoTaxAmount.add(onlineInvoiceDetail.getNoTaxAmount());
                totalTaxAmount = totalTaxAmount.add(onlineInvoiceDetail.getTaxAmount());
            }
            //????????????
            adjustAmount = differenceTotalAmount;//???????????????????????????????????????
            BigDecimal totalNoTaxAmountAfterAdjust = totalNoTaxAmount.add(adjustAmount);//????????????????????????????????????(??????????????????????????????)
            BigDecimal newTotalNoTaxAmount = BigDecimal.ZERO;//????????????????????????????????????????????????????????????????????????
            BigDecimal compareResult = adjustAmount.divide(totalInvoiceQuantity, 4, BigDecimal.ROUND_HALF_UP);//????????????(????????????????????????)
            for (OnlineInvoiceDetail onlineInvoiceDetail : yeslist) {
                BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity();//??????????????????
                BigDecimal unitPriceExcludingTax = onlineInvoiceDetail.getUnitPriceExcludingTax();//???????????????,??????????????????)
                onlineInvoiceDetail.setCompareResult(StringUtil.StringValue(compareResult))
                        .setPoUnitPriceChangeFlag(adjustAmount.compareTo(BigDecimal.ZERO) == 0 ? YesOrNo.NO.getValue() : YesOrNo.YES.getValue());//??????????????????
                BigDecimal newUnitPriceExcludingTax = unitPriceExcludingTax.add(compareResult);//????????????????????????
                onlineInvoiceDetail.setUnitPriceExcludingTax(newUnitPriceExcludingTax);
                BigDecimal newNoTaxAmount = invoiceQuantity.multiply(newUnitPriceExcludingTax);//????????????????????????
                BigDecimal taxRate = onlineInvoiceDetail.getTaxRate().divide(new BigDecimal("100"));
                BigDecimal newTax = newNoTaxAmount.multiply(taxRate);//??????????????????
                BigDecimal newTaxAmount = newNoTaxAmount.add(newTax);//????????????????????????
                BigDecimal newUnitPriceContainingTax = newTaxAmount.divide(invoiceQuantity, 8, BigDecimal.ROUND_HALF_UP);//????????????????????????
                onlineInvoiceDetail.setNoTaxAmount(newNoTaxAmount).setTax(newTax).setTaxAmount(newTaxAmount).setUnitPriceContainingTax(newUnitPriceContainingTax);
                newTotalNoTaxAmount = newTotalNoTaxAmount.add(newNoTaxAmount);
            }
            //?????????????????????
            BigDecimal lastDifference = totalNoTaxAmountAfterAdjust.subtract(newTotalNoTaxAmount);
            OnlineInvoiceDetail onlineInvoiceDetail = yeslist.get(0);
            BigDecimal noTaxAmount = onlineInvoiceDetail.getNoTaxAmount();
            BigDecimal taxAmount = onlineInvoiceDetail.getTaxAmount();
            onlineInvoiceDetail.setNoTaxAmount(noTaxAmount.add(lastDifference)).setTaxAmount(taxAmount.add(lastDifference));
            adjustAmount = BigDecimal.ZERO;
        }
        return adjustAmount;
    }

    private BigDecimal lessThanZeroForYes(BigDecimal differenceTotalAmount, List<OnlineInvoiceDetail> yeslist, BigDecimal adjustAmount) {
        if (differenceTotalAmount.compareTo(BigDecimal.ZERO) == -1) {
            //???????????????????????????
            BigDecimal totalInvoiceQuantity = BigDecimal.ZERO;//?????????????????????
            BigDecimal totalNoTaxAmount = BigDecimal.ZERO;//????????????????????????
            BigDecimal totalTaxAmount = BigDecimal.ZERO;//????????????????????????
            for (OnlineInvoiceDetail onlineInvoiceDetail : yeslist) {
                totalInvoiceQuantity = totalInvoiceQuantity.add(onlineInvoiceDetail.getInvoiceQuantity());
                totalNoTaxAmount = totalNoTaxAmount.add(onlineInvoiceDetail.getNoTaxAmount());
                totalTaxAmount = totalTaxAmount.add(onlineInvoiceDetail.getTaxAmount());
            }
            //????????????:?????????????????????????????????????????????????????????,????????????0
            if (totalNoTaxAmount.abs().compareTo(differenceTotalAmount.abs()) < 1) {
                for (OnlineInvoiceDetail onlineInvoiceDetail : yeslist) {
                    BigDecimal noTaxAmount = onlineInvoiceDetail.getNoTaxAmount();//?????????????????????
                    calculateDatasBySetZero(onlineInvoiceDetail, noTaxAmount);
                }
                //??????????????????????????????
                if (totalNoTaxAmount.abs().compareTo(differenceTotalAmount.abs()) == -1) {
                    adjustAmount = differenceTotalAmount.add(totalNoTaxAmount);
                }
                //??????????????????????????????
                if (totalNoTaxAmount.abs().compareTo(differenceTotalAmount.abs()) == 0) {
                    adjustAmount = BigDecimal.ZERO;
                }
            }
            //????????????,?????????????????????????????????????????????
            if (totalNoTaxAmount.abs().compareTo(differenceTotalAmount.abs()) == 1) {
                adjustAmount = differenceTotalAmount;//???????????????????????????????????????
                BigDecimal totalNoTaxAmountAfterAdjust = totalNoTaxAmount.add(adjustAmount);//????????????????????????????????????(??????????????????????????????)
                BigDecimal newTotalNoTaxAmount = BigDecimal.ZERO;//????????????????????????????????????????????????????????????????????????
                BigDecimal compareResult = adjustAmount.divide(totalInvoiceQuantity, 4, BigDecimal.ROUND_HALF_UP);//????????????(????????????????????????)
                for (OnlineInvoiceDetail onlineInvoiceDetail : yeslist) {
                    BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity();//??????????????????
                    BigDecimal unitPriceExcludingTax = onlineInvoiceDetail.getUnitPriceExcludingTax();//???????????????????????????)
                    onlineInvoiceDetail.setCompareResult(StringUtil.StringValue(compareResult))
                            .setPoUnitPriceChangeFlag(adjustAmount.compareTo(BigDecimal.ZERO) == 0 ? YesOrNo.NO.getValue() : YesOrNo.YES.getValue());//??????????????????
                    BigDecimal newNoTaxAmount = adjustAmount.add(onlineInvoiceDetail.getNoTaxAmount()).setScale(2, BigDecimal.ROUND_HALF_UP);//????????????????????????
                    BigDecimal newUnitPriceExcludingTax = newNoTaxAmount.divide(invoiceQuantity, 8, BigDecimal.ROUND_HALF_UP);//????????????????????????
                    onlineInvoiceDetail.setUnitPriceExcludingTax(newUnitPriceExcludingTax);
//                    BigDecimal newUnitPriceExcludingTax = unitPriceExcludingTax.add(compareResult);//????????????????????????
//                    onlineInvoiceDetail.setUnitPriceExcludingTax(newUnitPriceExcludingTax);
                    BigDecimal taxRate = onlineInvoiceDetail.getTaxRate().divide(new BigDecimal("100"));
                    BigDecimal newTax = newNoTaxAmount.multiply(taxRate);//??????????????????
                    BigDecimal newTaxAmount = newNoTaxAmount.add(newTax);//????????????????????????
                    onlineInvoiceDetail.setNoTaxAmount(newNoTaxAmount).setTax(newTax).setTaxAmount(newTaxAmount);
                    newTotalNoTaxAmount = newTotalNoTaxAmount.add(newNoTaxAmount);
                }
                //?????????????????????
                BigDecimal lastDifference = totalNoTaxAmountAfterAdjust.subtract(newTotalNoTaxAmount);
                OnlineInvoiceDetail onlineInvoiceDetail = yeslist.get(0);
                BigDecimal noTaxAmount = onlineInvoiceDetail.getNoTaxAmount();
                BigDecimal taxAmount = onlineInvoiceDetail.getTaxAmount();
                onlineInvoiceDetail.setNoTaxAmount(noTaxAmount.add(lastDifference)).setTaxAmount(taxAmount.add(lastDifference));
                adjustAmount = BigDecimal.ZERO;
            }
        }
        return adjustAmount;
    }

    private void calculateDatasBySetZero(OnlineInvoiceDetail onlineInvoiceDetail, BigDecimal noTaxAmount) {
        onlineInvoiceDetail.setUnitPriceExcludingTax(BigDecimal.ZERO);//???????????????,??????????????????)
        onlineInvoiceDetail.setNoTaxAmount(BigDecimal.ZERO);//????????????
        onlineInvoiceDetail.setTax(BigDecimal.ZERO);//??????
        onlineInvoiceDetail.setUnitPriceContainingTax(BigDecimal.ZERO);//????????????
        onlineInvoiceDetail.setTaxAmount(BigDecimal.ZERO);//????????????
        BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity();
        BigDecimal compareResult = noTaxAmount.negate().divide(invoiceQuantity, 4, BigDecimal.ROUND_HALF_UP);
        onlineInvoiceDetail.setCompareResult(StringUtil.StringValue(compareResult))
                .setPoUnitPriceChangeFlag(compareResult.compareTo(BigDecimal.ZERO) == 0 ? YesOrNo.NO.getValue() : YesOrNo.YES.getValue());//??????????????????
    }

    private BigDecimal greaterThanZeroForNo(BigDecimal differenceTotalAmount, OnlineInvoiceDetail onlineInvoiceDetail, BigDecimal adjustAmount) {
        if (differenceTotalAmount.compareTo(BigDecimal.ZERO) == 1) {
            adjustAmount = differenceTotalAmount;//?????????????????????????????????
            calculateData(adjustAmount, onlineInvoiceDetail);
            adjustAmount = BigDecimal.ZERO;
        }
        log.info("??????{} ??????????????????{}:====================================>", JSONObject.toJSONString(onlineInvoiceDetail) , adjustAmount);
        return adjustAmount;
    }

    private BigDecimal lessThanZeroForNo(BigDecimal differenceTotalAmount, OnlineInvoiceDetail onlineInvoiceDetail, BigDecimal adjustAmount) {
        if (differenceTotalAmount.compareTo(BigDecimal.ZERO) == -1) {
            BigDecimal noTaxAmount = onlineInvoiceDetail.getNoTaxAmount();
            //????????????:???????????????????????????????????????,????????????0
            if (noTaxAmount.abs().compareTo(differenceTotalAmount.abs()) < 1) {
                calculateDatasBySetZero(onlineInvoiceDetail, noTaxAmount);
                //??????????????????????????????
                if (noTaxAmount.abs().compareTo(differenceTotalAmount.abs()) == -1) {
                    adjustAmount = differenceTotalAmount.add(noTaxAmount);
                }
                //??????????????????????????????
                if (noTaxAmount.abs().compareTo(differenceTotalAmount.abs()) == 0) {
                    adjustAmount = BigDecimal.ZERO;
                }
            }
            //????????????,??????????????????????????????
            if (noTaxAmount.abs().compareTo(differenceTotalAmount.abs()) == 1) {
                adjustAmount = differenceTotalAmount;//???????????????????????????????????????
                calculateData(adjustAmount, onlineInvoiceDetail);
                adjustAmount = BigDecimal.ZERO;
            }
//            //????????????,??????????????????????????????
//            if (noTaxAmount.compareTo(differenceTotalAmount.abs()) == -1) {
//                BigDecimal adjustAmount = onlineInvoiceDetail.getNoTaxAmount().negate();
//                calculateData(adjustAmount, onlineInvoiceDetail);
//                differenceTotalAmount = differenceTotalAmount.subtract(adjustAmount);
//            }
        }
        log.info("??????{} ?????????????????????:{}====================================>" ,JSONObject.toJSONString(onlineInvoiceDetail) , adjustAmount);
        return adjustAmount;
    }

    private BigDecimal calculateData(BigDecimal adjustAmount, OnlineInvoiceDetail onlineInvoiceDetail) {
        BigDecimal invoiceQuantity = onlineInvoiceDetail.getInvoiceQuantity();//??????????????????
        BigDecimal unitPriceExcludingTax = onlineInvoiceDetail.getUnitPriceExcludingTax();//???????????????,??????????????????)
        BigDecimal compareResult = adjustAmount.divide(invoiceQuantity, 2, BigDecimal.ROUND_HALF_UP);//????????????(????????????????????????)
        onlineInvoiceDetail.setCompareResult(StringUtil.StringValue(compareResult))
                .setPoUnitPriceChangeFlag(adjustAmount.compareTo(BigDecimal.ZERO) == 0 ? YesOrNo.NO.getValue() : YesOrNo.YES.getValue());//??????????????????
        BigDecimal newNoTaxAmount = adjustAmount.add(onlineInvoiceDetail.getNoTaxAmount()).setScale(2, BigDecimal.ROUND_HALF_UP);//????????????????????????
        BigDecimal newUnitPriceExcludingTax = newNoTaxAmount.divide(invoiceQuantity, 8, BigDecimal.ROUND_HALF_UP);//????????????????????????
        onlineInvoiceDetail.setUnitPriceExcludingTax(newUnitPriceExcludingTax);
//        BigDecimal newUnitPriceExcludingTax = unitPriceExcludingTax.add(compareResult);//????????????????????????
//        onlineInvoiceDetail.setUnitPriceExcludingTax(newUnitPriceExcludingTax);
        BigDecimal taxRate = onlineInvoiceDetail.getTaxRate().divide(new BigDecimal("100"));
        BigDecimal newTax = newNoTaxAmount.multiply(taxRate).setScale(2, BigDecimal.ROUND_HALF_UP);//??????????????????
        BigDecimal newTaxAmount = newNoTaxAmount.add(newTax);//????????????????????????
        BigDecimal newUnitPriceContainingTax = newTaxAmount.divide(invoiceQuantity, 8, BigDecimal.ROUND_HALF_UP);//????????????????????????
        onlineInvoiceDetail.setNoTaxAmount(newNoTaxAmount).setTax(newTax).setTaxAmount(newTaxAmount).setUnitPriceContainingTax(newUnitPriceContainingTax);
        return adjustAmount;
    }

    private void saveOrUpdateOnlineInvoice(String invoiceStatus, OnlineInvoice onlineInvoice, LoginAppUser loginAppUser) {
        if (onlineInvoice.getOnlineInvoiceId() == null) {
            //??????????????????
            LocalDate now = LocalDate.now();
            onlineInvoice.setOnlineInvoiceId(IdGenrator.generate())
                    .setOnlineInvoiceNum(baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PS_ONLINE_INVOICE_CODE))
                    .setInvoiceStatus(invoiceStatus)
                    .setImportStatus(InvoiceImportStatus.NOT_IMPORT.name())
                    .setInvoiceDate(now);

            //????????????????????????
            if (UserType.BUYER.name().equals(loginAppUser.getUserType())) {
                setAccountPayableDealine(onlineInvoice, now);
            }
            this.save(onlineInvoice);
        } else {
            //??????????????????
            LocalDate now = LocalDate.now();
            //????????????????????????
            if (UserType.BUYER.name().equals(loginAppUser.getUserType())) {
                setAccountPayableDealine(onlineInvoice, now);
            }
            this.updateById(onlineInvoice.setInvoiceStatus(invoiceStatus));
        }
    }

    private void setAccountPayableDealine(OnlineInvoice onlineInvoice, LocalDate now) {
        String payAccountPeriodCode = onlineInvoice.getPayAccountPeriodCode();
        if (!ObjectUtils.isEmpty(payAccountPeriodCode)) {
            List<DictItemDTO> dictItemDTOS = baseClient.listAllByParam(new DictItemDTO().setDictItemCode(payAccountPeriodCode));
            if (CollectionUtils.isNotEmpty(dictItemDTOS)) {
                DictItemDTO dictItemDTO = dictItemDTOS.get(0);
                String itemDescription = dictItemDTO.getItemDescription();
                if (StringUtils.isNotEmpty(itemDescription)) {
                    onlineInvoice.setAccountPayableDealine(now.plusDays(Long.valueOf(itemDescription)));
                }
            }
        }

    }

//    ???????????????(??????)
//    private void setAccountPayableDealine(OnlineInvoice onlineInvoice, LocalDate now) {
//        String payAccountPeriodCode = onlineInvoice.getPayAccountPeriodCode();
//        boolean digit = StringUtil.isDigit(payAccountPeriodCode);
//        if (!digit) {
//            throw new BaseException("????????????????????????????????????,?????????!");
//        }
//        if ("0".equals(payAccountPeriodCode)) {
//            onlineInvoice.setAccountPayableDealine(now);
//        } else if ("00".equals(payAccountPeriodCode)) {
//            onlineInvoice.setAccountPayableDealine(now);
//        } else {
//            onlineInvoice.setAccountPayableDealine(now.plusDays(Long.valueOf(payAccountPeriodCode)));
//        }
//    }

    public String getDicName(List<DictItemDTO> list, String code) {
        return list.stream().filter(y -> Objects.equals(y.getDictItemCode(), code)).findFirst().orElse(new DictItemDTO()).getDictItemName();
    }

    public static void main(String[] args) {
//        OnlineInvoiceDetail onlineInvoiceDetail1 = new OnlineInvoiceDetail();
//        onlineInvoiceDetail1.setInvoiceQuantity(new BigDecimal("1"));
//        OnlineInvoiceDetail onlineInvoiceDetail2 = new OnlineInvoiceDetail();
//        onlineInvoiceDetail2.setInvoiceQuantity(new BigDecimal("2"));
//        OnlineInvoiceDetail onlineInvoiceDetail3 = new OnlineInvoiceDetail();
//        onlineInvoiceDetail3.setInvoiceQuantity(new BigDecimal("3"));
//        OnlineInvoiceDetail[] onlineInvoiceDetails = {onlineInvoiceDetail3, onlineInvoiceDetail1, onlineInvoiceDetail2};
//        List<OnlineInvoiceDetail> onlineInvoiceDetailList = Arrays.asList(onlineInvoiceDetails);
//        System.out.println(JsonUtil.arrayToJsonStr(onlineInvoiceDetailList));
//        BigDecimal sum = onlineInvoiceDetailList.stream().map(x ->
//                new BigDecimal(StringUtil.StringValue(x.getInvoiceQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
//        System.out.println(JsonUtil.entityToJsonStr(sum));

        System.out.println(StringUtils.isBlank(""));
    }


    /**
     * @Description ????????????????????????
     * @Author: xiexh12 2020-11-15 14:58
     */
    @Override
    public void exportOnlineInvoiceExcel(ExportExcelParam<OnlineInvoice> onlineInvoiceExportParam, HttpServletResponse response) throws IOException {
        // ?????????????????????
        List<List<Object>> dataList = this.queryExportData(onlineInvoiceExportParam);
        // ??????
        List<String> head = onlineInvoiceExportParam.getMultilingualHeader(onlineInvoiceExportParam, OnlineInvoiceExportUtils.getOnlineInvoiceTitles());
        // ?????????
        String fileName = onlineInvoiceExportParam.getFileName();
        // ????????????
        EasyExcelUtil.exportStart(response, dataList, head, fileName);
    }

    /**
     * @Description ???????????????????????????excel?????????
     * @Author: xiexh12 2020-11-15 15:03
     */
    private List<List<Object>> queryExportData(ExportExcelParam<OnlineInvoice> onlineInvoiceExportParam) {
        OnlineInvoice queryParam = onlineInvoiceExportParam.getQueryParam(); // ????????????
        /**
         * ???????????????????????????????????????, ??????2???????????????????????????????????????????????????,??????????????????2???
         */
        int count = queryCountByParam(queryParam);
        if (count > 20000) {
            /**
             * ????????????????????????20000, ??????????????????????????????
             */
            if (StringUtil.notEmpty(queryParam.getPageSize()) && StringUtil.notEmpty(queryParam.getPageNum())) {
                Assert.isTrue(queryParam.getPageSize() <= 20000, "????????????????????????20000");
            } else {
                throw new BaseException("??????????????????????????????20000???,?????????????????????,????????????20000;");
            }
        }

        boolean flag = null != queryParam.getPageSize() && null != queryParam.getPageNum();
        if (flag) {
            // ????????????
            PageUtil.startPage(queryParam.getPageNum(), queryParam.getPageSize());
        }
        List<OnlineInvoice> onlineInvoiceList = this.queryOnlineInvoiceByParam(queryParam);
        // ???Map(?????????map,key:?????????,value:?????????)
        List<Map<String, Object>> mapList = BeanMapUtils.objectsToMaps(onlineInvoiceList);
        // ??????excel?????????
        List<String> titleList = onlineInvoiceExportParam.getTitleList();

        List<List<Object>> results = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(mapList)) {
            mapList.forEach((map) -> {
                List<Object> list = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(titleList)) {
                    titleList.forEach((title) -> {
                        if (Objects.nonNull(map)) {
                            Object value = map.get(title);
                            if ("accountPayableDealine".equals(title)) {
                                String accountPayableDealine = String.valueOf(value);
                                list.add(accountPayableDealine);
                            } else if ("invoiceStatus".equals(title)) {
                                String invoiceStatus = convertInvoiceStatus(String.valueOf(value));
                                list.add(invoiceStatus);
                            } else if ("importStatus".equals(title)) {
                                String importStatus = convertImportStatus(String.valueOf(value));
                                list.add(importStatus);
                            } else if ("businessType".equals(title)) {
                                String businessType = businessTypeMap.get(String.valueOf(value));
                                list.add(businessType);
                            } else {
                                list.add((String) value);
                            }
                        }
                    });
                }
                results.add(list);
            });
        }
        return results;
    }


    /**
     * ?????????????????????????????????????????????
     *
     * @param queryParam
     * @modified xiexh12 2020-11-15 15:11
     */
    private int queryCountByParam(OnlineInvoice queryParam) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        QueryWrapper<OnlineInvoice> queryWrapper = new QueryWrapper<>();
        String onlineInvoiceNum = queryParam.getOnlineInvoiceNum();
        String startDate = Objects.nonNull(queryParam.getStartDate()) ? formatter.format(queryParam.getStartDate()) : "";
        String endDate = Objects.nonNull(queryParam.getEndDate()) ? formatter.format(queryParam.getEndDate()) : "";
        String orgName = queryParam.getOrgName();
        String vendorName = queryParam.getVendorName();
        String costTypeName = queryParam.getCostTypeName();
        String invoiceStatus = queryParam.getInvoiceStatus();
        String importStatus = queryParam.getImportStatus();
        String businessType = queryParam.getBusinessType();
        String taxInvoiceNum = queryParam.getTaxInvoiceNum();
        String fsscNo = queryParam.getFsscNo();
        String payMethod = queryParam.getPayMethod();

        queryWrapper.lambda().eq(StringUtils.isNotBlank(onlineInvoiceNum), OnlineInvoice::getOnlineInvoiceNum, onlineInvoiceNum)
                .like(StringUtils.isNotEmpty(orgName), OnlineInvoice::getOrgName, orgName)
                .like(StringUtils.isNotEmpty(vendorName), OnlineInvoice::getVendorName, vendorName)
                .eq(StringUtils.isNotEmpty(costTypeName), OnlineInvoice::getCostTypeName, costTypeName)
                .eq(StringUtils.isNotEmpty(invoiceStatus), OnlineInvoice::getInvoiceStatus, invoiceStatus)
                .eq(StringUtils.isNotEmpty(importStatus), OnlineInvoice::getImportStatus, importStatus)
                .eq(StringUtils.isNotEmpty(businessType), OnlineInvoice::getBusinessType, businessType)
                .eq(StringUtils.isNotEmpty(taxInvoiceNum), OnlineInvoice::getTaxInvoiceNum, taxInvoiceNum)
                .eq(StringUtils.isNotEmpty(fsscNo), OnlineInvoice::getFsscNo, fsscNo)
                .eq(StringUtils.isNotEmpty(payMethod), OnlineInvoice::getPayMethod, payMethod);
        if (StringUtils.isNotEmpty(startDate)) {
            queryWrapper.lambda().gt(OnlineInvoice::getAccountPayableDealine, startDate);
        }
        if (StringUtils.isNotEmpty(endDate)) {
            queryWrapper.lambda().lt(OnlineInvoice::getAccountPayableDealine, endDate);
        }
        return this.count(queryWrapper);
    }

    /*
     * @Description ?????????????????????
     * @modified xiexh12 11-15 15:51
     */
    private List<OnlineInvoice> queryOnlineInvoiceByParam(OnlineInvoice queryParam) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        QueryWrapper<OnlineInvoice> queryWrapper = new QueryWrapper<>();
        String onlineInvoiceNum = queryParam.getOnlineInvoiceNum();
        String startDate = Objects.nonNull(queryParam.getStartDate()) ? formatter.format(queryParam.getStartDate()) : "";
        String endDate = Objects.nonNull(queryParam.getEndDate()) ? formatter.format(queryParam.getEndDate()) : "";
        String orgName = queryParam.getOrgName();
        String vendorName = queryParam.getVendorName();
        String costTypeName = queryParam.getCostTypeName();
        String invoiceStatus = queryParam.getInvoiceStatus();
        String importStatus = queryParam.getImportStatus();
        String businessType = queryParam.getBusinessType();
        String taxInvoiceNum = queryParam.getTaxInvoiceNum();
        String fsscNo = queryParam.getFsscNo();
        String payMethod = queryParam.getPayMethod();

        queryWrapper.lambda().eq(StringUtils.isNotBlank(onlineInvoiceNum), OnlineInvoice::getOnlineInvoiceNum, onlineInvoiceNum)
                .like(StringUtils.isNotEmpty(orgName), OnlineInvoice::getOrgName, orgName)
                .like(StringUtils.isNotEmpty(vendorName), OnlineInvoice::getVendorName, vendorName)
                .eq(StringUtils.isNotEmpty(costTypeName), OnlineInvoice::getCostTypeName, costTypeName)
                .eq(StringUtils.isNotEmpty(invoiceStatus), OnlineInvoice::getInvoiceStatus, invoiceStatus)
                .eq(StringUtils.isNotEmpty(importStatus), OnlineInvoice::getImportStatus, importStatus)
                .eq(StringUtils.isNotEmpty(businessType), OnlineInvoice::getBusinessType, businessType)
                .eq(StringUtils.isNotEmpty(taxInvoiceNum), OnlineInvoice::getTaxInvoiceNum, taxInvoiceNum)
                .eq(StringUtils.isNotEmpty(fsscNo), OnlineInvoice::getFsscNo, fsscNo)
                .eq(StringUtils.isNotEmpty(payMethod), OnlineInvoice::getPayMethod, payMethod);
        if (StringUtils.isNotEmpty(startDate)) {
            queryWrapper.lambda().gt(OnlineInvoice::getAccountPayableDealine, startDate);
        }
        if (StringUtils.isNotEmpty(endDate)) {
            queryWrapper.lambda().lt(OnlineInvoice::getAccountPayableDealine, endDate);
        }
        List<OnlineInvoice> OnlineInvoiceList = this.list(queryWrapper);
        return OnlineInvoiceList;
    }

    /**
     * ????????????????????????????????????
     */
    public String convertInvoiceStatus(String status) {
        String result = "";
        if (StringUtils.isNotEmpty(status)) {
            switch (status) {
                case "DRAFT":
                    result = "??????";
                    break;
                case "UNDER_APPROVAL":
                    result = "?????????";
                    break;
                case "APPROVAL":
                    result = "?????????";
                    break;
                case "REVIEWED":
                    result = "?????????";
                    break;
                case "DROP":
                    result = "?????????";
                    break;
                case "REJECTED":
                    result = "?????????";
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    /**
     * ???????????????????????????????????????
     */
    public String convertImportStatus(String importStatus) {
        String result = "";
        if (StringUtils.isNotEmpty(importStatus)) {
            switch (importStatus) {
                case "NOT_IMPORT":
                    result = "?????????";
                    break;
                case "IMPORTED":
                    result = "?????????";
                    break;
                default:
                    break;
            }
        }
        return result;
    }


    //----------------------------------------------------------------OA???????????????-----------------------------------------

    @Override
    public void submitFlow(Long businessId, String param) throws Exception {

    }

    @Override
    public void passFlow(Long businessId, String param) throws Exception {

    }

    @Override
    public void rejectFlow(Long businessId, String param) throws Exception {

    }

    @Override
    public void withdrawFlow(Long businessId, String param) throws Exception {

    }

    @Override
    public void destoryFlow(Long businessId, String param) throws Exception {

    }

    @Override
    public String getVariableFlow(Long businessId, String param) throws Exception {
        return null;
    }

    @Override
    public String getDataPushFlow(Long businessId, String param) throws Exception {
        return null;
    }
}
