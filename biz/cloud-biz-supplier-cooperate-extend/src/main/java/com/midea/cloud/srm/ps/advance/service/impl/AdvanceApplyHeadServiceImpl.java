package com.midea.cloud.srm.ps.advance.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.pm.ps.*;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.contract.ContractClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.soap.DataSourceEnum;
import com.midea.cloud.srm.model.cm.contract.dto.ContractHeadDTO;
import com.midea.cloud.srm.model.cm.contract.entity.ContractHead;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.pm.ps.advance.dto.AdvanceApplyQueryDTO;
import com.midea.cloud.srm.model.pm.ps.advance.dto.AdvanceApplySaveDTO;
import com.midea.cloud.srm.model.pm.ps.advance.entity.AdvanceApplyHead;
import com.midea.cloud.srm.model.pm.ps.advance.entity.AdvanceApplyLine;
import com.midea.cloud.srm.model.pm.ps.anon.external.FsscStatus;
import com.midea.cloud.srm.model.pm.ps.http.*;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.suppliercooperate.invoice.dto.OnlineInvoiceSaveDTO;
import com.midea.cloud.srm.model.suppliercooperate.invoice.dto.OnlineInvoiceVo;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.OnlineInvoice;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.OnlineInvoiceAdvance;
import com.midea.cloud.srm.model.workflow.service.IFlowBusinessCallbackService;
import com.midea.cloud.srm.ps.advance.mapper.AdvanceApplyHeadMapper;
import com.midea.cloud.srm.ps.advance.service.IAdvanceApplyHeadService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.srm.ps.advance.service.IAdvanceApplyLineService;
import com.midea.cloud.srm.ps.anon.external.service.IFsscStatusService;
import com.midea.cloud.srm.ps.http.fssc.service.IFSSCReqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * <pre>
 *  ????????????????????? ???????????????
 * </pre>
 *
 * @author chensl26@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-08-20 13:40:24
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class AdvanceApplyHeadServiceImpl extends ServiceImpl<AdvanceApplyHeadMapper, AdvanceApplyHead> implements IAdvanceApplyHeadService, IFlowBusinessCallbackService {

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private IAdvanceApplyLineService iAdvanceApplyLineService;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private IFSSCReqService ifsscReqService;

    @Autowired
    private ContractClient contractClient;

    private static final String LONGI_NEW_ENERGY = "LONGI_NEW-ENERGY";//???????????????

    private static final String CLEAN_ENERGY = "CLEAN_ENERGY";//????????????



    @Override
    @Transactional
    public Long saveTemporary(AdvanceApplySaveDTO advanceApplySaveDTO, String advanceApplyStatus) {
        AdvanceApplyHead advanceApplyHead = advanceApplySaveDTO.getAdvanceApplyHead();
        List<AdvanceApplyLine> advanceApplyLines = advanceApplySaveDTO.getAdvanceApplyLines();
        List<Fileupload> fileuploads = advanceApplySaveDTO.getFileuploads();

        saveOrUpdateAdvanceApplySaveDTO(advanceApplyStatus, advanceApplyHead, advanceApplyLines, fileuploads);
        return advanceApplyHead.getAdvanceApplyHeadId();
    }

//    @Override
//    @Transactional
//    public FSSCResult submit(AdvanceApplySaveDTO advanceApplySaveDTO, String advanceApplyStatus) {
//        AdvanceApplyHead advanceApplyHead = advanceApplySaveDTO.getAdvanceApplyHead();
//        List<AdvanceApplyLine> advanceApplyLines = advanceApplySaveDTO.getAdvanceApplyLines();
//        List<Fileupload> fileuploads = advanceApplySaveDTO.getFileuploads();
//        //????????????
//        checkParam(advanceApplyHead, advanceApplyLines);
//        saveOrUpdateAdvanceApplySaveDTO(advanceApplyStatus, advanceApplyHead, advanceApplyLines, fileuploads);
//        Fileupload param = new Fileupload();
//        param.setBusinessId(advanceApplyHead.getAdvanceApplyHeadId());
//        param.setPageSize(100);
//        List<Fileupload> fileuploadList = fileCenterClient.listPage(param, null).getList();
//        /*srm????????????????????????*/
//        AdvanceApplyDto advanceApplyDto = buildDto(advanceApplySaveDTO.setFileuploads(fileuploadList));
//        /*??????????????????*/
//        FSSCResult fsscResult = null;
////        FSSCResult fsscResult = ifsscReqService.submitAdvanceApply(advanceApplyDto);
////        //?????????WARNING??????,????????????????????????,????????????????????????????????????????????????
////        if(FSSCResponseCode.WARN.getCode().equals(fsscResult.getCode())){
////            //??????????????????
////            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
////            return fsscResult;
////        }
////        if ("500".equals(fsscResult.getCode())) {
////            throw new BaseException(fsscResult.getMsg());
////        }
////        if (FSSCResponseCode.ERROR.getCode().equals(fsscResult.getCode())) {
////            throw new BaseException(fsscResult.getMsg());
////        }
//        /*??????????????????*/
//        this.updateById(new AdvanceApplyHead()
//                .setAdvanceApplyHeadId(advanceApplyHead.getAdvanceApplyHeadId())
////                .setPrintUrl(fsscResult.getPrintUrl())
////                .setBoeNo(fsscResult.getBoeNo())
////                .setSourceBoeNo(fsscResult.getSourceBoeNo())
//        );
//
//        /*????????????,?????????????????????????????????,????????????????????????*/
//        this.updateById(advanceApplyHead
//                .setAdvanceApplyStatus(AdvanceApplyStatus.APPROVAL.getValue())
//                .setUsableAmount(advanceApplyHead.getApplyPayAmount()));
//        return fsscResult;
//    }


    @Override
    @Transactional
    public Long submit(AdvanceApplySaveDTO advanceApplySaveDTO, String advanceApplyStatus) {
        AdvanceApplyHead advanceApplyHead = advanceApplySaveDTO.getAdvanceApplyHead();
        List<AdvanceApplyLine> advanceApplyLines = advanceApplySaveDTO.getAdvanceApplyLines();
        List<Fileupload> fileuploads = advanceApplySaveDTO.getFileuploads();
        //????????????
        checkParam(advanceApplyHead, advanceApplyLines);
        saveOrUpdateAdvanceApplySaveDTO(advanceApplyStatus, advanceApplyHead, advanceApplyLines, fileuploads);
//        Fileupload param = new Fileupload();
//        param.setBusinessId(advanceApplyHead.getAdvanceApplyHeadId());
//        param.setPageSize(100);
//        List<Fileupload> fileuploadList = fileCenterClient.listPage(param, null).getList();
//        /*srm????????????????????????*/
//        AdvanceApplyDto advanceApplyDto = buildDto(advanceApplySaveDTO.setFileuploads(fileuploadList));
//        /*??????????????????*/
//        FSSCResult fsscResult = null;

//        /*??????????????????*/
//        this.updateById(new AdvanceApplyHead()
//                        .setAdvanceApplyHeadId(advanceApplyHead.getAdvanceApplyHeadId())
//        );

        //??????????????? ???????????????
        this.updateById(advanceApplyHead
                .setAdvanceApplyStatus(AdvanceApplyStatus.SUBMITTED.getValue())
                .setUsableAmount(advanceApplyHead.getApplyPayAmount()));

        return advanceApplyHead.getAdvanceApplyHeadId();
    }


    public AdvanceApplyDto buildDto(AdvanceApplySaveDTO advanceApplySaveDTO) {
        AdvanceApplyHead advanceApplyHead = advanceApplySaveDTO.getAdvanceApplyHead();
        Date date = new Date();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        //?????????
        AdvanceApplyHead byId = this.getById(advanceApplyHead.getAdvanceApplyHeadId());
        BoeHeader boeHeader = new BoeHeader()
                .setSourceSystem(DataSourceEnum.NSRM_SYS.getKey())
                .setSourceSystemNum(advanceApplyHead.getAdvanceApplyNum())  //?????????????????????
                .setBoeDate(new SimpleDateFormat("yyyy-MM-dd").format(date))
                .setCreateByCode(StringUtil.StringValue(advanceApplyHead.getApplyUserName())) //(??????)
                .setEmployeeCode(StringUtil.StringValue(advanceApplyHead.getApplyUserName()))  //(??????)
                .setApprovalDeptCode(advanceApplyHead.getApplyDeptId()) //????????????
                .setExpenseDeptCode(advanceApplyHead.getApplyDeptId()) //????????????
                .setBoeDeptCode(null) //????????????(?????????)
                .setLeCode(null) //????????????(?????????)
                .setOperationTypeCode(advanceApplyHead.getBusinessType())
                .setPaperAccessories(advanceApplyHead.getIfPaperAttach())//??????????????????
                .setBpCount(advanceApplyHead.getBpCount())//??????????????????
                .setPaymentCurrency(advanceApplyHead.getCurrencyCode())
                .setRateValue(StringUtil.StringValue(advanceApplyHead.getExchangeRate())) //??????
                .setApplyAmount(StringUtil.StringValue(advanceApplyHead.getApplyPayAmount()))//????????????
                .setBoeAbstract(advanceApplyHead.getComment())//??????????????????
                .setComment(advanceApplyHead.getCostExplain())//???????????????????????????
                .setVendorCode(advanceApplyHead.getErpVendorCode()) //???????????????
                .setContractCode(advanceApplyHead.getContractCode())//????????????
                .setProjectCode(advanceApplyHead.getProjectNum())//?????????,????????????
                .setProjectName(advanceApplyHead.getProjectName())//?????????,????????????
                /*todo ????????????*/
                .setProjectTaskCode(advanceApplyHead.getTaskNum())
                /*todo ????????????*/
                .setProjectTaskName(advanceApplyHead.getTaskName())
                .setIsManuallyPushPlan(advanceApplyHead.getIfOutPlan())
                .setIsAgencyPayment(advanceApplyHead.getIfPayAgent())
                .setLeCodeLG(advanceApplyHead.getPayAgentOrgCode())  //????????????????????????
                .setEmpCodeLG(advanceApplyHead.getPayAgentUsername()) //?????????????????????  zuoshaopeng
                .setDeptCodeLG(StringUtil.StringValue(advanceApplyHead.getPayAgentDeptId())) //?????????????????????(?????????erp?????????ID) 101499
                .setGroupCode("LGi")
                .setBoeTypeCode(BoeTypeCode.FOREIGN_ADVANCE_PAYMENT.name())
                .setBpCount(advanceApplyHead.getBpCount())
                .setFundsPlanIgnore(advanceApplyHead.getFundsPlanIgnore())//????????????????????????
                .setContractIgnore(YesOrNo.YES.getValue());//?????????????????????????????????????????? ?????????Y
        List<ZfsBoeLine> zfsBoeLines = new ArrayList<>();
        zfsBoeLines.add(new ZfsBoeLine()
                .setExpenseAmount(StringUtil.StringValue(advanceApplyHead.getApplyPayAmount()))  //????????????
                .setUseCurrencyCode(advanceApplyHead.getCurrencyCode())
                .setStandardCurrencyAmount(StringUtil.StringValue(advanceApplyHead.getApplyPayAmount()))
        );
        List<ZfsBoePayment> zfsBoePayments = new ArrayList<>();
        advanceApplySaveDTO.getAdvanceApplyLines().forEach(item -> {
            ZfsBoePayment zfsBoePayment = new ZfsBoePayment()
                    .setPaymentModeCode(item.getPayMethod())
                    .setAccountName(item.getBankAccountName())
                    .setVendorSitesID(advanceApplyHead.getCostTypeCode())
                    .setBankBranchName(item.getOpeningBank())
                    .setBankAccountNum(item.getBankAccount())
                    .setBankUnitedCode(item.getUnionCode())
                    .setPaymentType("0") //?????????0????????????
                    .setStandardCurrencyAmount(StringUtil.StringValue(item.getApplyPayAmount()))
                    .setPaymentCurrencyCode(advanceApplyHead.getCurrencyCode())
                    .setPaymentAmount(item.getApplyPayAmount())
                    .setPayType(byId.getPaymentStage());//??????????????????
            zfsBoePayments.add(zfsBoePayment);
        });
        List<AttachmentDto> fileuploadList = new ArrayList<>();
        advanceApplySaveDTO.getFileuploads().forEach(item -> {
            AttachmentDto attachmentDto = new AttachmentDto()
                    .setAttachFileName(item.getFileSourceName())
                    .setAttachFilePath(StringUtil.StringValue(item.getFileuploadId()))
                    .setAttachFileType(item.getFileType())
                    .setAttachUploader(item.getCreatedBy())
                    .setAttachUploadTime(item.getCreationDate().toString());
            fileuploadList.add(attachmentDto);

        });
        return new AdvanceApplyDto().setBoeHeader(boeHeader).setZfsBoeLines(zfsBoeLines).setZfsBoePayments(zfsBoePayments).setAttachmentDtos(fileuploadList);
    }


    @Override
    public AdvanceApplySaveDTO getAdvanceApplySaveDTO(Long advanceApplyHeadId) {
        AdvanceApplySaveDTO advanceApplySaveDTO = new AdvanceApplySaveDTO();
        AdvanceApplyHead advanceApplyHead = this.getById(advanceApplyHeadId);
        List<AdvanceApplyLine> advanceApplyLines = iAdvanceApplyLineService.list(new QueryWrapper<>(new AdvanceApplyLine().setAdvanceApplyHeadId(advanceApplyHeadId)));
        PageInfo<Fileupload> fileuploads = fileCenterClient.listPage(new Fileupload().setBusinessId(advanceApplyHeadId), "");
        advanceApplySaveDTO.setAdvanceApplyHead(advanceApplyHead)
                .setAdvanceApplyLines(advanceApplyLines)
                .setFileuploads(fileuploads.getList());
        return advanceApplySaveDTO;
    }

    @Override
    @AuthData(module = MenuEnum.ADVANCE_PAYMENT)
    public PageInfo<AdvanceApplyHead> listPageByParam(AdvanceApplyQueryDTO advanceApplyQueryDTO) {
        PageUtil.startPage(advanceApplyQueryDTO.getPageNum(), advanceApplyQueryDTO.getPageSize());
        QueryWrapper<AdvanceApplyHead> queryWrapper = new QueryWrapper<>(new AdvanceApplyHead()
                .setCostTypeCode(StringUtils.isNotBlank(advanceApplyQueryDTO.getCostTypeCode()) ? advanceApplyQueryDTO.getCostTypeCode() : null)
                .setBusinessType(StringUtils.isNotBlank(advanceApplyQueryDTO.getBusinessType()) ? advanceApplyQueryDTO.getBusinessType() : null)
                .setIfPayAgent(StringUtils.isNotBlank(advanceApplyQueryDTO.getIfPayAgent()) ? advanceApplyQueryDTO.getIfPayAgent() : null)
                .setIfPowerStation(StringUtils.isNotBlank(advanceApplyQueryDTO.getIfPowerStation()) ? advanceApplyQueryDTO.getIfPayAgent() : null)
                .setAdvanceApplyStatus(StringUtils.isNotBlank(advanceApplyQueryDTO.getAdvanceApplyStatus()) ? advanceApplyQueryDTO.getAdvanceApplyStatus() : null));
        queryWrapper.like(StringUtils.isNotBlank(advanceApplyQueryDTO.getAdvanceApplyNum()), "ADVANCE_APPLY_NUM", advanceApplyQueryDTO.getAdvanceApplyNum());

        queryWrapper.nested(StringUtils.isNotBlank(advanceApplyQueryDTO.getVendorParam()), e -> {
            e.like(StringUtils.isNotBlank(advanceApplyQueryDTO.getVendorParam()),
                    "VENDOR_CODE",
                    advanceApplyQueryDTO.getVendorParam()).or();
            e.like(StringUtils.isNotBlank(advanceApplyQueryDTO.getVendorParam()),
                    "VENDOR_NAME",
                    advanceApplyQueryDTO.getVendorParam());
        });

        queryWrapper.like(StringUtils.isNotBlank(advanceApplyQueryDTO.getContractNum()), "CONTRACT_NUM", advanceApplyQueryDTO.getContractNum());
        queryWrapper.like(StringUtils.isNotBlank(advanceApplyQueryDTO.getContractCode()), "CONTRACT_CODE", advanceApplyQueryDTO.getCurrencyCode());
        queryWrapper.like(StringUtils.isNotBlank(advanceApplyQueryDTO.getProjectNum()), "PROJECT_NUM", advanceApplyQueryDTO.getProjectNum());
        queryWrapper.in(CollectionUtils.isNotEmpty(advanceApplyQueryDTO.getOrgIds()), "ORG_ID", advanceApplyQueryDTO.getOrgIds());
        queryWrapper.in(CollectionUtils.isNotEmpty(advanceApplyQueryDTO.getPayAgentOrgIds()), "PAY_AGENT_ORG_ID", advanceApplyQueryDTO.getPayAgentOrgIds());
        queryWrapper.between(advanceApplyQueryDTO.getApplyStartDate() != null && advanceApplyQueryDTO.getApplyEndDate() != null, "APPLY_DATE", advanceApplyQueryDTO.getApplyStartDate(), advanceApplyQueryDTO.getApplyEndDate());
        queryWrapper.ge(advanceApplyQueryDTO.getApplyStartDate() != null && advanceApplyQueryDTO.getApplyEndDate() == null, "APPLY_DATE", advanceApplyQueryDTO.getApplyStartDate());
        queryWrapper.le(advanceApplyQueryDTO.getApplyEndDate() != null && advanceApplyQueryDTO.getApplyStartDate() == null, "APPLY_DATE", advanceApplyQueryDTO.getApplyEndDate());
        //??????????????????????????????
        if (YesOrNo.YES.getValue().equals(advanceApplyQueryDTO.getIfOnlineInvoice())) {
            queryWrapper.gt("USABLE_AMOUNT", 0);
            queryWrapper.eq("IF_QUOTE", YesOrNo.NO.getValue());
            queryWrapper.eq("ADVANCE_APPLY_STATUS" , AdvanceApplyStatus.APPROVAL.getValue());
        }
        queryWrapper.orderByDesc("LAST_UPDATE_DATE");
        return new PageInfo<>(this.list(queryWrapper));
    }

    @Override
    @Transactional
    public void deleteByAdvanceApplyHeadId(Long advanceApplyHeadId) {
        this.removeById(advanceApplyHeadId);
        iAdvanceApplyLineService.remove(new QueryWrapper<>(new AdvanceApplyLine().setAdvanceApplyHeadId(advanceApplyHeadId)));
        fileCenterClient.deleteByParam(new Fileupload().setBusinessId(advanceApplyHeadId));
    }

    @Override
    public void setQuote(OnlineInvoiceAdvance onlineInvoiceAdvance, String ifQuote) {
        log.info("???????????????UsableAmount ={}" , onlineInvoiceAdvance.getUsableAmount());
        this.updateById(new AdvanceApplyHead().setIfQuote(ifQuote)
                .setAdvanceApplyHeadId(onlineInvoiceAdvance.getAdvanceApplyHeadId())
                .setUsableAmount(onlineInvoiceAdvance.getUsableAmount()));
    }

    @Override
    public OnlineInvoiceSaveDTO advanceReturn(List<AdvanceApplyHead> advanceApplyHeads) {
        checkBeforeAdvanceReturn(advanceApplyHeads);
        OnlineInvoiceSaveDTO onlineInvoiceSaveDTO = new OnlineInvoiceSaveDTO();
        OnlineInvoice onlineInvoice = new OnlineInvoice();
        List<OnlineInvoiceAdvance> onlineInvoiceAdvances = new ArrayList<>();
        AdvanceApplyHead head = advanceApplyHeads.get(0);
        for (AdvanceApplyHead advanceApplyHead : advanceApplyHeads) {
            if (advanceApplyHead == null) continue;
            OnlineInvoiceAdvance onlineInvoiceAdvance = new OnlineInvoiceAdvance();
            BeanUtils.copyProperties(advanceApplyHead, onlineInvoiceAdvance);
            onlineInvoiceAdvance.setChargeOffAmount(advanceApplyHead.getApplyPayAmount());
            onlineInvoiceAdvance.setHangAccountAmount(advanceApplyHead.getApplyPayAmount());
            onlineInvoiceAdvance.setUsableAmount(advanceApplyHead.getApplyPayAmount());
            onlineInvoiceAdvances.add(onlineInvoiceAdvance);
        }
        String payMethod = getPayMethod(head);
        BeanUtils.copyProperties(head, onlineInvoice);
        onlineInvoice.setPayMethod(payMethod);
        onlineInvoice.setVirtualInvoice(YesOrNo.YES.getValue());//????????????????????????(??????????????????)
        onlineInvoice.setIsService(YesOrNo.NO.getValue());//??????????????????????????????
        onlineInvoice.setApproverDept(head.getApplyDeptName())
                .setApproverDeptid(head.getApplyDeptId())
                .setApproverId(head.getApplyUserId())
                .setApproverNickname(head.getApplyUserNickname())
                .setApproverUsername(head.getApplyUserName());
//        String businessType = head.getBusinessType();
//        if (BusinessType.BUSINESS_TYPE_04.getValue().equals(businessType)) {
//            onlineInvoice.setIsService(YesOrNo.YES.getValue());
//        }
//        onlineInvoice.setInvoiceStatus(InvoiceStatus.DRAFT.name()); ????????????????????????,?????????
        onlineInvoiceSaveDTO.setOnlineInvoice(onlineInvoice).setOnlineInvoiceAdvances(onlineInvoiceAdvances);
        return onlineInvoiceSaveDTO;
    }

    @Override
    @Transactional
    public FSSCResult abandon(Long advanceApplyHeadId) {
        AdvanceApplyHead advanceApplyHead = this.getById(advanceApplyHeadId);
        Assert.notNull(advanceApplyHead, LocaleHandler.getLocaleMsg("??????????????????,?????????!"));
        if (!AdvanceApplyStatus.DRAFT.getValue().equals(advanceApplyHead.getAdvanceApplyStatus()) && !AdvanceApplyStatus.REJECTED.getValue().equals(advanceApplyHead.getAdvanceApplyStatus())) {
            throw new BaseException("????????????????????????????????????,????????????!");
        }
        this.updateById(advanceApplyHead.setAdvanceApplyStatus(AdvanceApplyStatus.DROP.getValue()));
        FSSCResult fsscResult = new FSSCResult();
        if (StringUtils.isNotBlank(advanceApplyHead.getBoeNo())) {
            fsscResult = ifsscReqService.abandon(new FsscStatus()
                    .setFsscNo(advanceApplyHead.getBoeNo())
                    .setPeripheralSystemNum(advanceApplyHead.getAdvanceApplyNum())
                    .setSourceSysteCode(DataSourceEnum.NSRM_SYS.getKey()));
        }
        if ("500".equals(fsscResult.getCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg(fsscResult.getMsg()));
        }
        return fsscResult;
    }

    private String getPayMethod(AdvanceApplyHead advanceApplyHead) {
        String payMethod = "";
        List<AdvanceApplyLine> advanceApplyLines = iAdvanceApplyLineService.list(new QueryWrapper<>(new AdvanceApplyLine().setAdvanceApplyHeadId(advanceApplyHead.getAdvanceApplyHeadId())));
        if (CollectionUtils.isNotEmpty(advanceApplyLines)) {
            AdvanceApplyLine advanceApplyLine = advanceApplyLines.get(0);
            payMethod = advanceApplyLine.getPayMethod();
        }
        return payMethod;
    }

    private void checkBeforeAdvanceReturn(List<AdvanceApplyHead> advanceApplyHeads) {
        Assert.notEmpty(advanceApplyHeads, LocaleHandler.getLocaleMsg("???????????????????????????,?????????!"));
        for (AdvanceApplyHead advanceApplyHead : advanceApplyHeads) {
            if (advanceApplyHead == null) continue;
            if (!AdvanceApplyStatus.APPROVAL.getValue().equals(advanceApplyHead.getAdvanceApplyStatus())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,?????????!"));
            }
            for (AdvanceApplyHead head : advanceApplyHeads) {
                if (head == null) continue;
                if (!(advanceApplyHead.getOrgId().compareTo(head.getOrgId()) == 0
                        && advanceApplyHead.getVendorId().compareTo(head.getVendorId()) == 0
                        && advanceApplyHead.getBusinessType().equals(head.getBusinessType())
                        && advanceApplyHead.getContractNum().equals(head.getContractNum())
                        && advanceApplyHead.getCurrencyId().compareTo(head.getCurrencyId()) == 0
                        && advanceApplyHead.getExchangeRate().compareTo(head.getExchangeRate()) == 0
                        && StringUtil.StringValue(advanceApplyHead.getProjectNum()).equals(StringUtil.StringValue(head.getProjectNum())))) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????,?????????,????????????,????????????,??????,??????,??????????????????????????????????????????,?????????!"));
                }
                if (YesOrNo.YES.getValue().equals(head.getIfQuote())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????,?????????!"));
                }
            }
        }
    }

    private void checkParam(AdvanceApplyHead advanceApplyHead, List<AdvanceApplyLine> advanceApplyLines) {
        Assert.notNull(advanceApplyHead.getAdvanceApplyHeadId() , "??????????????????????????????????????????????????????????????????");
        //??????????????????????????????????????????
//        Optional.ofNullable(advanceApplyHead.getOrgId()).ifPresent(o -> {
//            Organization organization = baseClient.getOrganization(new Organization().setOrganizationId(o));
//            if (LONGI_NEW_ENERGY.equals(organization.getDivisionId()) || CLEAN_ENERGY.equals(organization.getDivisionId())) {
//                String projectName = advanceApplyHead.getProjectName();
//                String projectNum = advanceApplyHead.getProjectNum();
//                if (StringUtils.isBlank(projectName) || StringUtils.isBlank(projectNum)) {
//                    throw new BaseException(LocaleHandler.getLocaleMsg(organization.getDivision() + "??????????????????,???????????????????????????????????????,?????????!"));
//                }
//            }
//        });
        BigDecimal includeTaxAmount = advanceApplyHead.getIncludeTaxAmount() == null ? BigDecimal.ZERO : advanceApplyHead.getIncludeTaxAmount();//???????????????(??????)
        BigDecimal applyPayAmount = advanceApplyHead.getApplyPayAmount() == null ? BigDecimal.ZERO : advanceApplyHead.getApplyPayAmount();//???????????????
        //??????????????????????????????????????????????????????
        if (applyPayAmount.compareTo(includeTaxAmount) == 1) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????"));
        }
        //ToDo ????????????????????????,????????????????????????.
//        ContractHead contractHead = contractClient.getContractHeadByParam(new ContractHeadDTO()
//                .setContractNo(advanceApplyHead.getContractNum())
//                .setContractCode(advanceApplyHead.getContractCode()));
//        if (contractHead != null && contractHead.getVendorId().compareTo(advanceApplyHead.getVendorId()) != 0) {
//            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????????????????!"));
//        }
    }

    private void saveOrUpdateAdvanceApplySaveDTO(String advanceApplyStatus, AdvanceApplyHead advanceApplyHead, List<AdvanceApplyLine> advanceApplyLines, List<Fileupload> fileuploads) {
        if (advanceApplyHead != null) {
            //????????????????????????
            LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
            saveOrUpdateAdvanceApplyHead(advanceApplyStatus, advanceApplyHead, loginAppUser);
            saveOrUpdateAdvanceApplyLines(advanceApplyLines, advanceApplyHead);
            if (CollectionUtils.isNotEmpty(fileuploads)) {
                fileCenterClient.bindingFileupload(fileuploads, advanceApplyHead.getAdvanceApplyHeadId());
            }
        }
    }

    private void saveOrUpdateAdvanceApplyHead(String advanceApplyStatus, AdvanceApplyHead advanceApplyHead, LoginAppUser loginAppUser) {
        if (advanceApplyHead.getAdvanceApplyHeadId() == null) {
            advanceApplyHead.setAdvanceApplyHeadId(IdGenrator.generate())
                    .setAdvanceApplyNum(baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PS_ADVANCE_APPLY_CODE))
                    .setAdvanceApplyStatus(advanceApplyStatus)
                    .setApplyUserId(advanceApplyHead.getApplyUserId())
                    .setApplyUserNickname(advanceApplyHead.getApplyUserNickname())
                    .setApplyUserName(advanceApplyHead.getApplyUserName())
                    .setApplyDeptId(advanceApplyHead.getApplyDeptId())
                    .setApplyDeptName(advanceApplyHead.getApplyDeptName());
            this.save(advanceApplyHead);
        } else {
            advanceApplyHead.setAdvanceApplyStatus(advanceApplyStatus);
            this.updateById(advanceApplyHead);
        }
    }

    public static void main(String[] args) {
        BigDecimal bigDecimal2 = new BigDecimal("2");
        BigDecimal bigDecimal1 = new BigDecimal("1");
        if (bigDecimal2.compareTo(bigDecimal1) == 1) {
            System.out.println("bigDecimal1??????bigDecimal2");
        }
    }

    private void saveOrUpdateAdvanceApplyLines(List<AdvanceApplyLine> advanceApplyLines, AdvanceApplyHead advanceApplyHead) {
        if (CollectionUtils.isNotEmpty(advanceApplyLines)) {
            for (AdvanceApplyLine advanceApplyLine : advanceApplyLines) {
                if (advanceApplyLine == null) continue;
                if (advanceApplyLine.getAdvanceApplyLineId() == null) {
                    advanceApplyLine.setAdvanceApplyLineId(IdGenrator.generate())
                            .setAdvanceApplyHeadId(advanceApplyHead.getAdvanceApplyHeadId());
                    iAdvanceApplyLineService.save(advanceApplyLine);
                } else {
                    iAdvanceApplyLineService.updateById(advanceApplyLine);
                }
            }
        }
    }

    //-------------------------------------------------------------------------?????????---------------------------------------------------------------------------------
    //?????????oa??????

    @Override
    public void submitFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OASubmitFlow", businessId, param);
        AdvanceApplyHead advanceApplyHead = new AdvanceApplyHead().setAdvanceApplyHeadId(businessId).setAdvanceApplyStatus(AdvanceApplyStatus.UNDER_APPROVAL.getValue());
        this.updateById(advanceApplyHead);
    }

    @Override
    public void passFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OAPassFlow", businessId, param);
        AdvanceApplyHead advanceApplyHead = new AdvanceApplyHead().setAdvanceApplyHeadId(businessId).setAdvanceApplyStatus(AdvanceApplyStatus.APPROVAL.getValue());
        this.updateById(advanceApplyHead);

    }

    @Override
    public void rejectFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OARejectFlow", businessId, param);
        AdvanceApplyHead advanceApplyHead = new AdvanceApplyHead().setAdvanceApplyHeadId(businessId).setAdvanceApplyStatus(AdvanceApplyStatus.REJECTED.getValue());
        this.updateById(advanceApplyHead);

    }

    @Override
    public void withdrawFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OAWithdrawFlow", businessId, param);
        AdvanceApplyHead advanceApplyHead = new AdvanceApplyHead().setAdvanceApplyHeadId(businessId).setAdvanceApplyStatus(AdvanceApplyStatus.WITHDRAW.getValue());
        this.updateById(advanceApplyHead);

    }

    @Override
    public void destoryFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OADestoryFlow", businessId, param);
        AdvanceApplyHead advanceApplyHead = new AdvanceApplyHead().setAdvanceApplyHeadId(businessId).setAdvanceApplyStatus(AdvanceApplyStatus.ABANDONED.getValue());
        this.updateById(advanceApplyHead);
    }

    @Override
    public String getVariableFlow(Long businessId, String param) throws Exception {
        log.info("getVariableFlow: {}, {}", businessId, param);
        return null;
    }

    @Override
    public String getDataPushFlow(Long businessId, String param) throws Exception {
        log.info("getDataPushFlow: {}, {}", businessId, param);
        return null;
    }
}
