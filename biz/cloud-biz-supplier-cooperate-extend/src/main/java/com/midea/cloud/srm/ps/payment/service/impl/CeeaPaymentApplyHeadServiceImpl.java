package com.midea.cloud.srm.ps.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.cm.PaymentStatus;
import com.midea.cloud.common.enums.pm.ps.FSSCResponseCode;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.contract.ContractClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.soap.DataSourceEnum;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.pm.ps.anon.external.FsscStatus;
import com.midea.cloud.srm.model.pm.ps.http.*;
import com.midea.cloud.srm.model.pm.ps.payment.dto.CeeaPaymentApplyDTO;
import com.midea.cloud.srm.model.pm.ps.payment.dto.CeeaPaymentApplyHeadQueryDTO;
import com.midea.cloud.srm.model.pm.ps.payment.dto.CeeaPaymentApplySaveDTO;
import com.midea.cloud.srm.model.pm.ps.payment.entity.CeeaPaymentApplyHead;
import com.midea.cloud.srm.model.pm.ps.payment.entity.CeeaPaymentApplyHeadExcelDTO;
import com.midea.cloud.srm.model.pm.ps.payment.entity.CeeaPaymentApplyLine;
import com.midea.cloud.srm.model.pm.ps.payment.entity.CeeaPaymentApplyPlan;
import com.midea.cloud.srm.model.pm.ps.payment.vo.CeeaPaymentApplyVo;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.OnlineInvoice;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.OnlineInvoiceDetail;
import com.midea.cloud.srm.ps.http.fssc.service.IFSSCReqService;
import com.midea.cloud.srm.ps.payment.mapper.CeeaPaymentApplyHeadMapper;
import com.midea.cloud.srm.ps.payment.service.ICeeaPaymentApplyHeadService;
import com.midea.cloud.srm.ps.payment.service.ICeeaPaymentApplyLineService;
import com.midea.cloud.srm.ps.payment.service.ICeeaPaymentApplyPlanService;
import com.midea.cloud.srm.supcooperate.invoice.service.IOnlineInvoiceDetailService;
import com.midea.cloud.srm.supcooperate.invoice.service.IOnlineInvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
*  <pre>
 *  ????????????-???????????????????????? ???????????????
 * </pre>
*
* @author xiexh12@midea.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-08-25 21:04:34
 *  ????????????:
 * </pre>
*/
@Service
@Slf4j
public class CeeaPaymentApplyHeadServiceImpl extends ServiceImpl<CeeaPaymentApplyHeadMapper, CeeaPaymentApplyHead> implements ICeeaPaymentApplyHeadService {

    @Autowired
    private ICeeaPaymentApplyPlanService iCeeaPaymentApplyPlanService;
    @Autowired
    private CeeaPaymentApplyHeadMapper ceeaPaymentApplyHeadMapper;
    @Autowired
    private ICeeaPaymentApplyLineService ceeaPaymentApplyLineService;
    @Autowired
    private FileCenterClient fileCenterClient;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private IFSSCReqService ifsscReqService;
    @Autowired
    private ContractClient contractClient;
    @Autowired
    private SupplierClient supplierClient;
    @Resource
    private IOnlineInvoiceService scIOnlineInvoiceService;
    @Resource
    private IOnlineInvoiceDetailService scIOnlineInvoiceDetailService;

    @Override
    public CeeaPaymentApplyDTO getPaymentApplyByPayPlanId(Long payPlanId) {
        Assert.notNull(payPlanId, "????????????ID???????????????");
        CeeaPaymentApplyPlan ceeaPaymentApplyPlan = new CeeaPaymentApplyPlan();
        ceeaPaymentApplyPlan.setPayPlanId(payPlanId);
        QueryWrapper<CeeaPaymentApplyPlan> ceeaPaymentApplyPlanQueryWrapper = new QueryWrapper<>(ceeaPaymentApplyPlan);
        List<CeeaPaymentApplyPlan> ceeaPaymentApplyPlanList = iCeeaPaymentApplyPlanService.list(ceeaPaymentApplyPlanQueryWrapper);
        CeeaPaymentApplyDTO dto = new CeeaPaymentApplyDTO();
        if(!ceeaPaymentApplyPlanList.isEmpty() && null != ceeaPaymentApplyPlanList.get(0)){
            dto.setCeeaPaymentApplyPlans(ceeaPaymentApplyPlanList);
            Long paymentApplyHeadId = ceeaPaymentApplyPlanList.get(0).getPaymentApplyHeadId();
            CeeaPaymentApplyHead head = this.getById(paymentApplyHeadId);
            if(null != head){
                dto.setCeeaPaymentApplyHead(head);
            }
        }
        return dto;
    }

    @Override
    @AuthData(module = MenuEnum.PUR_PAYMENT_APPLY)
    public PageInfo<CeeaPaymentApplyHead> listPage(CeeaPaymentApplyHeadQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPageNum(),queryDTO.getPageSize());
        return new PageInfo<CeeaPaymentApplyHead>(ceeaPaymentApplyHeadMapper.findList(queryDTO));
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePaymentApply(CeeaPaymentApplySaveDTO ceeaPaymentApplySaveDTO) {
        CeeaPaymentApplyHead paymentApplyHead = ceeaPaymentApplySaveDTO.getPaymentApplyHead();
        List<CeeaPaymentApplyLine> paymentApplyLineList = ceeaPaymentApplySaveDTO.getPaymentApplyLineList();
        List<CeeaPaymentApplyPlan> paymentApplyPlanList = ceeaPaymentApplySaveDTO.getPaymentApplyPlanList();
        List<Fileupload> fileuploadList = ceeaPaymentApplySaveDTO.getFileuploadList();
        //???????????????
        boolean createFlag = false;
        if(paymentApplyHead.getPaymentApplyHeadId() == null){
            createFlag = true;
            initPaymentApplyHead(ceeaPaymentApplySaveDTO);
        }
        this.saveOrUpdate(paymentApplyHead);
        Map<String , BigDecimal> currentPayMap = paymentApplyLineList.stream().collect(Collectors.toMap(
                o -> o.getInvoiceNum(),
                o -> o.getApplyPayAmount(),
                (o, o2) -> o
        ));
        List<String> invoiceNums = paymentApplyLineList.stream().map(o -> o.getInvoiceNum()).distinct()
                                    .collect(Collectors.toList());
        /*modify by chensl26 2021-3-2*/
//        Map<String ,OnlineInvoice> onlineInvoiceMap = supcooperateClient.listOnlineInvoiceByNumbers(invoiceNums)
//                        .stream().collect(Collectors.toMap(o -> o.getOnlineInvoiceNum() , o -> o , (o, o2) -> o));
        Map<String ,OnlineInvoice> onlineInvoiceMap = (invoiceNums.isEmpty() ? new ArrayList<OnlineInvoice>() : scIOnlineInvoiceService
                .list(Wrappers.lambdaQuery(OnlineInvoice.class)
                .in(OnlineInvoice::getOnlineInvoiceNum , invoiceNums)))
                .stream().collect(Collectors.toMap(o -> o.getOnlineInvoiceNum() , o -> o , (o, o2) -> o));
        //??????????????????????????????????????????????????????????????????????????????????????????
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("PAYMENT_APPLY_HEAD_ID",paymentApplyHead.getPaymentApplyHeadId());
        ceeaPaymentApplyLineService.remove(queryWrapper);
        //????????????????????????10??????????????????
        for(CeeaPaymentApplyLine item : paymentApplyLineList){
            OnlineInvoice onlineInvoice = onlineInvoiceMap.get(item.getInvoiceNum());
            if (Objects.nonNull(onlineInvoice) && Objects.nonNull(onlineInvoice.getOnlineInvoiceId())){
                List<OnlineInvoiceDetail> onlineInvoiceDetails = scIOnlineInvoiceDetailService.list(Wrappers.lambdaQuery(OnlineInvoiceDetail.class)
                        .eq(OnlineInvoiceDetail::getOnlineInvoiceId , onlineInvoice.getOnlineInvoiceId()));
                if(onlineInvoiceDetails.isEmpty() || Objects.isNull(onlineInvoiceDetails.get(0))){
                    //?????????-????????????????????????????????????????????????
                    item.setProjectNum("");
                    item.setProjectName("");
                }else{
                    item.setProjectNum(onlineInvoiceDetails.get(0).getProjectNum());
                    item.setProjectName(onlineInvoiceDetails.get(0).getProjectName());
                }

            }
            item.setPaymentApplyLineId(IdGenrator.generate());
            item.setPaymentApplyHeadId(paymentApplyHead.getPaymentApplyHeadId());
            ceeaPaymentApplyLineService.save(item);
        }

        //????????????????????????
        List<OnlineInvoice> updateOnlineInvoiceList = new ArrayList<>();
        for(String invoiceNum : invoiceNums){
            if(Objects.isNull(invoiceNum)){
                continue;
            }
            OnlineInvoice onlineInvoice = onlineInvoiceMap.get(invoiceNum);
            BigDecimal currentPayAmount = currentPayMap.get(invoiceNum);
            if(Objects.nonNull(onlineInvoice) && createFlag){
                BigDecimal dbPayAmount = onlineInvoice.getPaidAmount() == null ? BigDecimal.ZERO : onlineInvoice.getPaidAmount();
                //??????????????? = ?????????????????? + ?????????????????????(???????????????????????????????????????????????????????????????)
                BigDecimal newPayAmount = currentPayAmount.add(dbPayAmount);
                //????????????????????? = ???????????? - ?????????????????????
                BigDecimal newUnPayAmount = onlineInvoice.getActualInvoiceAmountY().subtract(dbPayAmount);
                //???????????????????????? ??????????????????????????????????????? ??????????????????????????????????????????
                log.info("??????????????????{}???????????????????????????{}????????????????????????{}, ???????????????{}??????????????????{}",
                        invoiceNum ,
                        onlineInvoice.getPaidAmount(),
                        newPayAmount,
                        onlineInvoice.getActualInvoiceAmountY(),
                        onlineInvoice.getUnPaidAmount());
                if(newPayAmount.abs().compareTo(onlineInvoice.getActualInvoiceAmountY().abs()) == 1){
                    throw new BaseException("??????"+invoiceNum+",????????????"+currentPayAmount+
                            "??????????????????"+newPayAmount+"????????????????????????"+onlineInvoice.getActualInvoiceAmountY()+"???????????????");
                }
                if(currentPayAmount.compareTo(newUnPayAmount.abs()) == 1){
                    throw new BaseException("??????"+invoiceNum+"?????????"+onlineInvoice.getPaidAmount()+",????????????"+currentPayAmount+
                            "???????????????????????????"+newUnPayAmount+"???????????????");

                }
                //?????????????????? = ?????????????????? - ????????????
                onlineInvoice.setUnPaidAmount(newUnPayAmount.subtract(currentPayAmount));
                //????????????
                onlineInvoice.setPaidAmount(newPayAmount);
                updateOnlineInvoiceList.add(onlineInvoice);
//                    supcooperateClient.updateOnlineInvoice(onlineInvoice);
            }
        }

        //???????????????-??????????????????
        for(OnlineInvoice onlineInvoice : updateOnlineInvoiceList){
            if(Objects.isNull(onlineInvoice)){
                continue;
            }
            UpdateWrapper<OnlineInvoice> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("PAID_AMOUNT" , onlineInvoice.getPaidAmount());
            updateWrapper.set("UN_PAID_AMOUNT" , onlineInvoice.getUnPaidAmount());
            updateWrapper.eq("ONLINE_INVOICE_NUM" , onlineInvoice.getOnlineInvoiceNum());
            scIOnlineInvoiceService.update(updateWrapper);
        }


        iCeeaPaymentApplyPlanService.remove(queryWrapper);
        paymentApplyPlanList.forEach(item -> {
            item.setPaymentApplyPlanId(IdGenrator.generate());
            item.setPaymentApplyHeadId(paymentApplyHead.getPaymentApplyHeadId());
            iCeeaPaymentApplyPlanService.save(item);
        });

        if(!CollectionUtils.isEmpty(fileuploadList)){
            fileCenterClient.bindingFileupload(fileuploadList , paymentApplyHead.getPaymentApplyHeadId());
        }
        return paymentApplyHead.getPaymentApplyHeadId();
    }




    private void initPaymentApplyHead(CeeaPaymentApplySaveDTO ceeaPaymentApplySaveDTO){
        CeeaPaymentApplyHead paymentApplyHead = ceeaPaymentApplySaveDTO.getPaymentApplyHead();
        List<CeeaPaymentApplyLine> paymentApplyLineList = ceeaPaymentApplySaveDTO.getPaymentApplyLineList();
        Long id = IdGenrator.generate();
        //????????????????????????
        Organization organization = new Organization();
        organization.setOrganizationId(paymentApplyHead.getOrgId());
        organization = baseClient.getOrganizationByParam(organization);

        //????????????
        String onlineInvoiceNum = paymentApplyLineList.get(0).getInvoiceNum();
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("ONLINE_INVOICE_NUM" , onlineInvoiceNum);
        List<OnlineInvoice> onlineInvoices = scIOnlineInvoiceService.list(wrapper);
        if(!CollectionUtils.isEmpty(onlineInvoices) &&
                Objects.nonNull(onlineInvoices.get(0)) &&
                Objects.nonNull(organization)){
            //?????????????????????????????????????????????????????????;
            OnlineInvoice onlineInvoice = onlineInvoices.get(0);
            if(organization.getCeeaCompanyCode().equals(onlineInvoice.getCurrencyCode())){
                paymentApplyHead.setGidailyRate(new BigDecimal(1));
            }else{
                BigDecimal ceeaExchangeRate = baseClient.getRateByFromTypeAndToType(onlineInvoice.getCurrencyCode() , organization.getCeeaCurrencyCode());
                paymentApplyHead.setGidailyRate(ceeaExchangeRate);
            }
        }
        //???????????? end

        paymentApplyHead.setApplyDate(new Date())
                .setPaymentApplyNumber(baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PS_PAYMENT_APPLY_NUMBER))
                .setApplyUserId(AppUserUtil.getLoginAppUser().getUserId())
                .setApplyUserNickname(AppUserUtil.getLoginAppUser().getNickname())
                .setApplyUserName(AppUserUtil.getLoginAppUser().getUsername())
                .setPaymentApplyHeadId(id)
                .setReceiptStatus(PaymentStatus.DRAFT.getKey())
                .setOrgCode(organization.getOrganizationCode())
                .setOrgName(organization.getOrganizationName());
    }

    @Override
    @Transactional
    public FSSCResult submitPaymentApply(CeeaPaymentApplySaveDTO ceeaPaymentApplySaveDTO) {
        checkIfLegalSubmit(ceeaPaymentApplySaveDTO);
        CeeaPaymentApplyHead paymentApplyHead = ceeaPaymentApplySaveDTO.getPaymentApplyHead();
        List<CeeaPaymentApplyLine> paymentApplyLineList = ceeaPaymentApplySaveDTO.getPaymentApplyLineList();
        List<CeeaPaymentApplyPlan> paymentApplyPlanList = ceeaPaymentApplySaveDTO.getPaymentApplyPlanList();
        List<Fileupload> fileuploadList = ceeaPaymentApplySaveDTO.getFileuploadList();
        Date date = new Date();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        this.saveOrUpdate(paymentApplyHead
                .setReceiptStatus(PaymentStatus.UNDER_APPROVAL.getKey())
        );
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("PAYMENT_APPLY_HEAD_ID",paymentApplyHead.getPaymentApplyHeadId());
        ceeaPaymentApplyLineService.remove(queryWrapper);
        paymentApplyLineList.forEach(item -> {
            item.setPaymentApplyLineId(IdGenrator.generate());
            item.setPaymentApplyHeadId(paymentApplyHead.getPaymentApplyHeadId());
            ceeaPaymentApplyLineService.save(item);
        });
        iCeeaPaymentApplyPlanService.remove(queryWrapper);
        paymentApplyPlanList.forEach(item -> {
            item.setPaymentApplyPlanId(IdGenrator.generate());
            item.setPaymentApplyHeadId(paymentApplyHead.getPaymentApplyHeadId());
            iCeeaPaymentApplyPlanService.save(item);
        });
        List<Fileupload> fileuploads = new ArrayList<>();
        fileuploadList.forEach(item -> {
            fileuploads.add(new Fileupload().setFileuploadId(item.getFileuploadId()));
        });
        if(!CollectionUtils.isEmpty(fileuploads)){
            Long businessId = paymentApplyHead.getPaymentApplyHeadId();
            fileCenterClient.bindingFileupload(fileuploads,businessId);
        }

//        /*????????????????????????*/
//        List<Fileupload> fileuploadsTemp = fileCenterClient.listPage(new Fileupload().setBusinessId(paymentApplyHead.getPaymentApplyHeadId()),null).getList();
//        ceeaPaymentApplySaveDTO.setFileuploadList(fileuploadsTemp);
//        FSSCResult fsscResult = ifsscReqService.submitPaymentApply(buildPaymentApply(ceeaPaymentApplySaveDTO));
//        //?????????WARNING??????,????????????????????????,????????????????????????????????????????????????
//        if(FSSCResponseCode.WARN.getCode().equals(fsscResult.getCode())){
//            //??????????????????
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return fsscResult;
//        }
//        if (FSSCResponseCode.ERROR.getCode().equals(fsscResult.getCode())) {
//            throw new BaseException(fsscResult.getMsg());
//        }
//        this.updateById(new CeeaPaymentApplyHead()
//                .setPaymentApplyHeadId(paymentApplyHead.getPaymentApplyHeadId())
//                .setBoeNo(fsscResult.getBoeNo())    //??????????????????
//                .setSourceBoeNo(paymentApplyHead.getSourceBoeNo())
//                .setPrintUrl(paymentApplyHead.getPrintUrl())
//        );
        /*????????????????????????*/
        /*List<PayPlan> updates = new ArrayList<>();
        paymentApplyPlanList.forEach(item -> {
            PayPlan payPlan = contractClient.ceeaGetById(item.getPayPlanId());
            PayPlan update = new PayPlan()
                    .setPaidAmount(payPlan.getPaidAmount().add(item.getActualPaymentAmountNoTax()))
                    .setPayPlanId(payPlan.getPayPlanId());
            updates.add(update);
        });
        contractClient.ceeaUpdateBatch(updates);
        FSSCResult fsscResult = new FSSCResult();*/
//        return fsscResult;
        FSSCResult fsscResult = new FSSCResult();
        fsscResult.setCode(FSSCResponseCode.SUCCESS.getCode());

        /**
         * ??????????????????, ????????????????????????
         */
        this.saveOrUpdate(paymentApplyHead
                .setReceiptStatus(PaymentStatus.APPROVAL.getKey())
        );
        return fsscResult;
    }

    private PaymentApplyDto buildPaymentApply(CeeaPaymentApplySaveDTO ceeaPaymentApplySaveDTO){
        CeeaPaymentApplyHead paymentApplyHead = ceeaPaymentApplySaveDTO.getPaymentApplyHead();
        Date date = new Date();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        CompanyInfo companyInfo = supplierClient.getCompanyInfo(paymentApplyHead.getVendorId());
        if(companyInfo == null){
            throw new BaseException("????????????????????????");
        }
        String vendorCode = companyInfo.getErpVendorCode();
        BoeHeader boeHeader = new BoeHeader()
                .setSourceSystem(DataSourceEnum.NSRM_SYS.getKey())
                .setSourceSystemNum(paymentApplyHead.getPaymentApplyNumber())  //??????????????????
                .setBoeDate(new SimpleDateFormat("yyyy-MM-dd").format(date))
                .setCreateByCode(paymentApplyHead.getApplyUserName()) //?????????????????????
                .setEmployeeCode(paymentApplyHead.getApplyUserName()) //?????????????????????
                .setApprovalDeptCode(paymentApplyHead.getApplyDeptId())  //????????? ??????????????????
                .setExpenseDeptCode(paymentApplyHead.getApplyDeptId())  //????????? ??????????????????
                .setBoeDeptCode("") //???????????? ?????????
                .setLeCode("") //???????????? ?????????
                .setContractIgnore("Y")     //??????????????????????????????????????????
                .setOperationTypeCode(paymentApplyHead.getBusinessType())
                .setPaperAccessories(paymentApplyHead.getIfPaperAttach())
                .setRateValue(paymentApplyHead.getGidailyRate().toString())
                .setPaymentCurrency(paymentApplyHead.getCurrencyCode())  //????????????
                .setApplyAmount(paymentApplyHead.getApplyPaymentTotalAmount().toString())
                .setBoeAbstract(paymentApplyHead.getComments()) //???????????????
                .setComment(paymentApplyHead.getComments())
                .setVendorCode(companyInfo.getErpVendorCode())  //???????????????
                .setIsManuallyPushPlan(paymentApplyHead.getIfManualOutPlan())
                .setIsAgencyPayment(paymentApplyHead.getIfPayAgent())
                .setLeCodeLG(paymentApplyHead.getPayAgentOrgCode()) // ???????????????????????????
                .setEmpCodeLG(paymentApplyHead.getPayAgentCode())  //??????????????????
                .setDeptCodeLG(Objects.isNull(paymentApplyHead.getPayAgentDeptId())? "" : paymentApplyHead.getPayAgentDeptId().toString())  //?????????????????????
                .setGroupCode("LGi")
                .setProjectCode("")
                .setProjectName("")
                .setBoeTypeCode("FOREIGN_PAYMENT_BOE")
                .setBpCount(paymentApplyHead.getBpCount())
                .setContractCode(paymentApplyHead.getContractCode())//????????????
                .setFundsPlanIgnore(paymentApplyHead.getFundsPlanIgnore())//????????????????????????
                .setContractIgnore(YesOrNo.YES.getValue());//?????????????????????????????????????????? ?????????Y

        List<BoeExpenseCavDto> boeExpenseCavDtos = new ArrayList<>();
        //2020-11-17 ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
        //????????????????????????????????????????????????????????????????????????
        List<String> invoiceNums = ceeaPaymentApplySaveDTO.getPaymentApplyLineList().stream().map(o -> o.getInvoiceNum()).distinct()
                .collect(Collectors.toList());
        Map<String ,OnlineInvoice> onlineInvoiceMap = (invoiceNums.isEmpty() ? new ArrayList<OnlineInvoice>() : scIOnlineInvoiceService.list(Wrappers.lambdaQuery(OnlineInvoice.class)
                .in(OnlineInvoice::getOnlineInvoiceNum , invoiceNums)))
                .stream().collect(Collectors.toMap(o -> o.getOnlineInvoiceNum() , o -> o , (o, o2) -> o));
        ceeaPaymentApplySaveDTO.getPaymentApplyLineList().forEach(item -> {
            OnlineInvoice onlineInvoice = onlineInvoiceMap.get(item.getInvoiceNum());
            //??????????????????
            if (Objects.nonNull(onlineInvoice) && Objects.nonNull(onlineInvoice.getOnlineInvoiceId())){
                List<OnlineInvoiceDetail> onlineInvoiceDetails = scIOnlineInvoiceDetailService.list(Wrappers.lambdaQuery(OnlineInvoiceDetail.class)
                        .eq(OnlineInvoiceDetail::getOnlineInvoiceId , onlineInvoice.getOnlineInvoiceId()));
                if(!onlineInvoiceDetails.isEmpty() && Objects.nonNull(onlineInvoiceDetails.get(0))){
                    OnlineInvoiceDetail onlineInvoiceDetail = onlineInvoiceDetails.get(0);
                    //??????????????????????????? ?????????????????????
                    boeHeader.setProjectCode(onlineInvoiceDetail.getProjectNum());
                    boeHeader.setProjectName(onlineInvoiceDetail.getProjectName());
                    boeHeader.setProjectTaskCode(onlineInvoiceDetail.getTaskNum());
                    boeHeader.setProjectTaskName(onlineInvoiceDetail.getTaskName());
                }else{
                    //????????????????????????????????????,?????????????????????????????????
                    boeHeader.setProjectCode(item.getProjectNum());
                    boeHeader.setProjectName(item.getProjectName());
//                    boeHeader.setProjectTaskCode("");//?????????????????????
//                    boeHeader.setProjectTaskName("");//?????????????????????

                }
            }

            boeExpenseCavDtos.add(new BoeExpenseCavDto()
                    .setSourceSystemNum(item.getInvoiceNum())  //???????????????
                    .setCavAmount(StringUtil.StringValue(item.getApplyPayAmount()))
            );
        });

        List<ZfsBoePayment> zfsBoePayments = new ArrayList<>();
        ceeaPaymentApplySaveDTO.getPaymentApplyLineList().forEach(item -> {
            zfsBoePayments.add(new ZfsBoePayment()
                    .setPayDetailsLineNum(item.getRowNum().toString())
                    /*todo ????????? Longi online*/
                    .setPaymentModeCode(item.getPayMethod()) //????????????
                    .setAccountName(item.getBankAccountName())
                    .setVendorSitesID(paymentApplyHead.getCostTypeCode())
                    .setBankBranchName(item.getBankName())
                    .setBankAccountNum(item.getBankAccount())  //?????????????????????
                    .setBankUnitedCode(item.getUnionCode()) //?????????????????????
                    .setPaymentType("0")
                    .setStandardCurrencyAmount(StringUtil.StringValue(item.getApplyPayAmount()))
                    .setPaymentCurrencyCode(paymentApplyHead.getCurrencyCode())
                    .setPaymentAmount(item.getApplyPayAmount())
            );
        });
        //???????????????????????????????????????; ???????????????????????????
        Map<String , ZfsBoePayment> payMap = new HashMap<>();
        for(int i = 0 ; i < zfsBoePayments.size() ; i++){
            ZfsBoePayment zfsBoePayment = zfsBoePayments.get(i);
            String key = zfsBoePayment.getBankAccountNum()+zfsBoePayment.getPaymentType();
            ZfsBoePayment boePaymentInMap = payMap.get(key);
            if(Objects.nonNull(boePaymentInMap)){
                boePaymentInMap.setPaymentAmount(
                    boePaymentInMap.getPaymentAmount().add(zfsBoePayment.getPaymentAmount()));
                    BigDecimal left = new BigDecimal(boePaymentInMap.getStandardCurrencyAmount());
                    BigDecimal righ = new BigDecimal(zfsBoePayment.getStandardCurrencyAmount());
                    BigDecimal sum = left.add(righ);
                    boePaymentInMap.setStandardCurrencyAmount(sum.setScale(2,BigDecimal.ROUND_HALF_UP).toString());
            }else{
                payMap.put(key , zfsBoePayment);
            }
        }
        List<ZfsBoePayment> zfsBoePaymentsOutput = new ArrayList<>();
        for(String key : payMap.keySet()){
            zfsBoePaymentsOutput.add(payMap.get(key));
        }

        List<AttachmentDto> attachmentDtos = new ArrayList<>();
        if(ceeaPaymentApplySaveDTO.getFileuploadList() != null){
            ceeaPaymentApplySaveDTO.getFileuploadList().forEach(item -> {
                attachmentDtos.add(new AttachmentDto()
                        .setAttachFileName(item.getFileSourceName())
                        .setAttachFilePath(StringUtil.StringValue(item.getFileuploadId()))
                        .setAttachFileType(item.getFileType())
                        .setAttachUploader(item.getCreatedBy())
                        .setAttachUploadTime(item.getCreationDate().toString())
                );
            });
        }
        return new PaymentApplyDto()
                .setBoeHeader(boeHeader)
                .setBoeExpenseCavDtos(boeExpenseCavDtos)
                .setZfsBoePayments(zfsBoePaymentsOutput)
                .setAttachmentDtos(attachmentDtos);
    }

    @Override
    @Transactional
    public void delete(Long paymentApplyHeadId) {
        checkIfLegalDelete(paymentApplyHeadId);
        this.removeById(paymentApplyHeadId);
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("PAYMENT_APPLY_HEAD_ID",paymentApplyHeadId);
        ceeaPaymentApplyLineService.remove(queryWrapper);
        iCeeaPaymentApplyPlanService.remove(queryWrapper);
        fileCenterClient.deleteByParam(new Fileupload().setBusinessId(paymentApplyHeadId));
        /*todo ???????????????????????????*/

    }

    @Override
    public CeeaPaymentApplyVo detail(Long paymentApplyHeadId) {
        CeeaPaymentApplyVo result = new CeeaPaymentApplyVo();
        CeeaPaymentApplyHead ceeaPaymentApplyHead = this.getById(paymentApplyHeadId);
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("PAYMENT_APPLY_HEAD_ID",paymentApplyHeadId);
        List<CeeaPaymentApplyLine> ceeaPaymentApplyLineList = ceeaPaymentApplyLineService.list(queryWrapper);
        List<CeeaPaymentApplyPlan> ceeaPaymentApplyPlanList = iCeeaPaymentApplyPlanService.list(queryWrapper);
        Fileupload params = new Fileupload();
        params.setBusinessId(paymentApplyHeadId);
        params.setPageNum(1);
        params.setPageSize(50);
        List<Fileupload> fileuploadList = fileCenterClient.listPage(params,null).getList();
        result.setPaymentApplyHead(this.getById(paymentApplyHeadId))
                .setPaymentApplyLineList(ceeaPaymentApplyLineList)
                .setPaymentApplyPlanList(ceeaPaymentApplyPlanList)
                .setFileuploadList(fileuploadList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FSSCResult abandon(Long paymentApplyHeadId) {
        CeeaPaymentApplyHead ceeaPaymentApplyHead = this.getById(paymentApplyHeadId);

        //???????????????????????????????????????????????????????????????
        Assert.notNull(ceeaPaymentApplyHead, LocaleHandler.getLocaleMsg("??????????????????,?????????!"));
        if (!PaymentStatus.DRAFT.getKey().equals(ceeaPaymentApplyHead.getReceiptStatus()) && !PaymentStatus.REJECT.getKey().equals(ceeaPaymentApplyHead.getReceiptStatus())) {
            throw new BaseException("????????????????????????????????????,????????????!");
        }

        //?????????????????????????????????????????????????????????????????????
        FSSCResult fsscResult = new FSSCResult();
        if (StringUtils.isNotBlank(ceeaPaymentApplyHead.getBoeNo())) {
            ifsscReqService.abandon(new FsscStatus()
                    .setFsscNo(ceeaPaymentApplyHead.getBoeNo())
                    .setPeripheralSystemNum(ceeaPaymentApplyHead.getPaymentApplyNumber())
                    .setSourceSysteCode(DataSourceEnum.NSRM_SYS.getKey()));
        }
        if (FSSCResponseCode.ERROR.equals(fsscResult.getCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg(fsscResult.getMsg()));
        }

        //??????????????????????????????
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("PAYMENT_APPLY_HEAD_ID",ceeaPaymentApplyHead.getPaymentApplyHeadId());
        List<CeeaPaymentApplyLine> paymentApplyLineList =  ceeaPaymentApplyLineService.list(queryWrapper);
        paymentApplyLineList.forEach(item -> {
            QueryWrapper wrapper = new QueryWrapper();
            wrapper.eq("ONLINE_INVOICE_NUM" , item.getInvoiceNum());
            List<OnlineInvoice> onlineInvoiceList = scIOnlineInvoiceService.list(wrapper);
            //??????????????????
            if(CollectionUtils.isEmpty(onlineInvoiceList) || Objects.isNull(onlineInvoiceList.get(0))){
                return ;
            }

            OnlineInvoice onlineInvoice = onlineInvoiceList.get(0);
            boolean dirtyData = BigDecimal.ZERO.equals(onlineInvoice.getPaidAmount()) &&
                                BigDecimal.ZERO.equals(onlineInvoice.getUnPaidAmount());
            //????????????????????????????????????
            if(!dirtyData && !PaymentStatus.DROP.getKey().equals(ceeaPaymentApplyHead.getReceiptStatus())){
                //?????????????????? =??????????????? + ??????????????????
                onlineInvoice.setUnPaidAmount(onlineInvoice.getUnPaidAmount().add(item.getApplyPayAmount()));
                //?????????????????? = ???????????? - ??????????????????
                onlineInvoice.setPaidAmount(onlineInvoice.getPaidAmount().subtract(item.getApplyPayAmount()));
                UpdateWrapper<OnlineInvoice> updateWrapper = new UpdateWrapper<>();
                updateWrapper.set("PAID_AMOUNT" , onlineInvoice.getPaidAmount());
                updateWrapper.set("UN_PAID_AMOUNT" , onlineInvoice.getUnPaidAmount());
                updateWrapper.eq("ONLINE_INVOICE_NUM" , onlineInvoice.getOnlineInvoiceNum());
                scIOnlineInvoiceService.update(updateWrapper);
            }
        });
        //???????????? ????????????
        this.updateById(ceeaPaymentApplyHead.setReceiptStatus(PaymentStatus.DROP.getKey()));

        return fsscResult;
    }

    private void checkIfLegalDelete(Long paymentApplyHeadId) {
        CeeaPaymentApplyHead ceeaPaymentApplyHead = this.getById(paymentApplyHeadId);
        Assert.notNull(ceeaPaymentApplyHead, LocaleHandler.getLocaleMsg("??????????????????????????????????????????????????????"));
        if(!ceeaPaymentApplyHead.getReceiptStatus().equals(PaymentStatus.DRAFT.getKey())){
            Assert.notNull(null,LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????"));
        }
    }

    private void checkIfLegalSubmit(CeeaPaymentApplySaveDTO ceeaPaymentApplySaveDTO) {
        CeeaPaymentApplyHead paymentApplyHead = ceeaPaymentApplySaveDTO.getPaymentApplyHead();
        List<CeeaPaymentApplyLine> paymentApplyLineList = ceeaPaymentApplySaveDTO.getPaymentApplyLineList();
        List<CeeaPaymentApplyPlan> paymentApplyPlanList = ceeaPaymentApplySaveDTO.getPaymentApplyPlanList();

        CeeaPaymentApplyHead ceeaPaymentApplyHead = this.getById(paymentApplyHead.getPaymentApplyHeadId());
        Assert.notNull(ceeaPaymentApplyHead, LocaleHandler.getLocaleMsg("??????????????????????????????????????????????????????"));
        Assert.notNull(ceeaPaymentApplyHead.getGidailyRate() , LocaleHandler.getLocaleMsg("??????????????????"));
        if(!ceeaPaymentApplyHead.getReceiptStatus().equals(PaymentStatus.DRAFT.getKey()) &&
            !ceeaPaymentApplyHead.getReceiptStatus().equals(PaymentStatus.REJECT.getKey())){
            Assert.notNull(null,LocaleHandler.getLocaleMsg("??????????????????????????????"));
        }
        /*?????? ???????????????????????? ?????????????????????????????? */
        Assert.notNull(paymentApplyHead.getApplyPaymentTotalAmount(),LocaleHandler.getLocaleMsg("??????????????????????????????"));
        BigDecimal lineSum = new BigDecimal(0L);
        for(CeeaPaymentApplyLine item:paymentApplyLineList){
            Assert.notNull(item.getApplyPayAmount(),LocaleHandler.getLocaleMsg("??????????????????????????????"));
            lineSum = lineSum.add(item.getApplyPayAmount());
        }
        if(lineSum.compareTo(paymentApplyHead.getApplyPaymentTotalAmount()) == 1){
            Assert.notNull(null,LocaleHandler.getLocaleMsg("????????????-??????????????????????????????????????????????????????"));
        }
        /*???????????? ?????????????????????????????????????????? ????????????????????????????????????????????? */
        paymentApplyPlanList.forEach(item -> {
            Assert.notNull(item.getPeriodPaymentAmountNoTax(),LocaleHandler.getLocaleMsg("??????????????????????????????"));
            Assert.notNull(item.getPaidAmountNoTax(),LocaleHandler.getLocaleMsg("????????????????????????"));
            Assert.notNull(item.getActualPaymentAmountNoTax(),LocaleHandler.getLocaleMsg("??????????????????????????????"));
            if(item.getActualPaymentAmountNoTax().compareTo(item.getPeriodPaymentAmountNoTax().subtract(item.getPaidAmountNoTax())) == 1){
                Assert.notNull(null,LocaleHandler.getLocaleMsg("?????????????????????????????????????????????"));
            }
        });

    }

    @Override
    public void export(CeeaPaymentApplyHeadQueryDTO queryDTO, HttpServletResponse response) throws ExecutionException, InterruptedException, IOException {
        List<CeeaPaymentApplyHead> list = this.listPage(queryDTO).getList();
        CompletableFuture<List<DictItemDTO>> cfStatus = CompletableFuture.supplyAsync(() -> baseClient.listAllByParam(new DictItemDTO().setDictCode("PAYMENT_STATUS")));
        CompletableFuture<List<DictItemDTO>> cfBussinessType = CompletableFuture.supplyAsync(() -> baseClient.listAllByParam(new DictItemDTO().setDictCode("BUSINESS_TYPE")));
        CompletableFuture<List<DictItemDTO>> cfYesNo = CompletableFuture.supplyAsync(() -> baseClient.listAllByParam(new DictItemDTO().setDictCode("YES_OR_NO")));
        CompletableFuture.allOf(cfStatus, cfBussinessType, cfYesNo).join();
        List<DictItemDTO> paymentStatusList = cfStatus.get();
        List<DictItemDTO> paymentImportStatusList = cfBussinessType.get();
        List<DictItemDTO> yesNoList = cfYesNo.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ArrayList<CeeaPaymentApplyHeadExcelDTO> paymentExcel= new ArrayList<>();

        list.stream().forEach(x -> {
            CeeaPaymentApplyHeadExcelDTO excelDTO = new CeeaPaymentApplyHeadExcelDTO();
            BeanUtils.copyProperties(x, excelDTO);
            if(Objects.nonNull(x.getGidailyRate())){
                excelDTO.setGidailyRate(x.getGidailyRate().toString());
            }
            excelDTO.setApplyDate(DateUtil.parseDateToStr(x.getApplyDate(), DateUtil.YYYY_MM_DD));
            excelDTO.setReceiptStatus(getDicName(paymentStatusList, x.getReceiptStatus()));
            excelDTO.setBusinessType(getDicName(paymentImportStatusList, x.getBusinessType()));
            excelDTO.setIfPayAgent(getDicName(yesNoList, x.getIfPayAgent()));
            excelDTO.setIfPowerStationBusiness(getDicName(yesNoList, x.getIfPowerStationBusiness()));
            paymentExcel.add(excelDTO);
        });
        OutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "payment_apply");
        EasyExcelUtil.writeExcelWithModel(outputStream, "????????????", paymentExcel, CeeaPaymentApplyHeadExcelDTO.class);
    }

    public String getDicName(List<DictItemDTO> list, String code) {
        return list.stream().filter(y -> Objects.equals(y.getDictItemCode(), code)).findFirst().orElse(new DictItemDTO()).getDictItemName();
    }
}
