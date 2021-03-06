package com.midea.cloud.srm.supauth.review.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.RepushConst;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.AccessProcessType;
import com.midea.cloud.common.enums.ApproveStatusType;
import com.midea.cloud.common.enums.MainType;
import com.midea.cloud.common.enums.OpType;
import com.midea.cloud.common.enums.OrgCateBillType;
import com.midea.cloud.common.enums.RoleType;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.enums.review.CategoryStatus;
import com.midea.cloud.common.enums.review.FormType;
import com.midea.cloud.common.enums.review.OrgStatus;
import com.midea.cloud.common.enums.review.QuaReviewType;
import com.midea.cloud.common.enums.sup.SiteJournalStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.JsonUtil;
import com.midea.cloud.common.utils.NamedThreadFactory;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.repush.service.RepushHandlerService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.flow.WorkFlowFeign;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.repush.entity.RepushStatus;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.role.entity.Role;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.model.supplier.info.dto.InfoDTO;
import com.midea.cloud.srm.model.supplier.info.dto.OrgCateServiceStatusDTO;
import com.midea.cloud.srm.model.supplier.info.entity.BankInfo;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.ContactInfo;
import com.midea.cloud.srm.model.supplier.info.entity.FinanceInfo;
import com.midea.cloud.srm.model.supplier.info.entity.OrgCategory;
import com.midea.cloud.srm.model.supplier.info.entity.OrgInfo;
import com.midea.cloud.srm.model.supplier.info.entity.SiteInfo;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorBank.VendorBankOutputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorContact.VendorContactOutputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorInfo.VendorInfoOutputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorSite.VendorSiteOutputParameters;
import com.midea.cloud.srm.model.supplierauth.entry.entity.EntryConfig;
import com.midea.cloud.srm.model.supplierauth.entry.entity.EntryConfigRecord;
import com.midea.cloud.srm.model.supplierauth.entry.entity.EntryRelationRecord;
import com.midea.cloud.srm.model.supplierauth.review.dto.ReviewFormDTO;
import com.midea.cloud.srm.model.supplierauth.review.entity.BankJournal;
import com.midea.cloud.srm.model.supplierauth.review.entity.CateJournal;
import com.midea.cloud.srm.model.supplierauth.review.entity.FinanceJournal;
import com.midea.cloud.srm.model.supplierauth.review.entity.OrgCateJournal;
import com.midea.cloud.srm.model.supplierauth.review.entity.OrgJournal;
import com.midea.cloud.srm.model.supplierauth.review.entity.ReviewForm;
import com.midea.cloud.srm.model.supplierauth.review.entity.ReviewFormExp;
import com.midea.cloud.srm.model.supplierauth.review.entity.SiteJournal;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import com.midea.cloud.srm.model.workflow.service.IFlowBusinessCallbackService;
import com.midea.cloud.srm.supauth.entry.service.IEntryConfigRecordService;
import com.midea.cloud.srm.supauth.entry.service.IEntryConfigService;
import com.midea.cloud.srm.supauth.entry.service.IEntryRelationRecordService;
import com.midea.cloud.srm.supauth.entry.service.IFileRecordService;
import com.midea.cloud.srm.supauth.review.mapper.BankJournalMapper;
import com.midea.cloud.srm.supauth.review.mapper.FinanceJournalMapper;
import com.midea.cloud.srm.supauth.review.mapper.OrgCateJournalMapper;
import com.midea.cloud.srm.supauth.review.mapper.ReviewFormExpMapper;
import com.midea.cloud.srm.supauth.review.mapper.ReviewFormMapper;
import com.midea.cloud.srm.supauth.review.mapper.SiteJournalMapper;
import com.midea.cloud.srm.supauth.review.service.IBankJournalService;
import com.midea.cloud.srm.supauth.review.service.ICateJournalService;
import com.midea.cloud.srm.supauth.review.service.IFinanceJournalService;
import com.midea.cloud.srm.supauth.review.service.IOrgCateJournalService;
import com.midea.cloud.srm.supauth.review.service.IOrgJournalService;
import com.midea.cloud.srm.supauth.review.service.IReviewFormExpService;
import com.midea.cloud.srm.supauth.review.service.IReviewFormService;
import com.midea.cloud.srm.supauth.review.service.ISiteJournalService;
import com.midea.cloud.srm.supauth.workflow.controller.ReviewFlow;

import lombok.extern.slf4j.Slf4j;

/**
 * <pre>
 *  ?????????????????? ???????????????
 * </pre>
 *
 * @author chensl26@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-03-10 16:34:39
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class ReviewFormServiceImpl extends ServiceImpl<ReviewFormMapper, ReviewForm> implements IReviewFormService, IFlowBusinessCallbackService {

    @Autowired
    ReviewFormMapper reviewFormMapper;

    @Autowired
    OrgCateJournalMapper orgCateJournalMapper;

    @Autowired
    ReviewFormExpMapper reviewFormExpMapper;

    @Autowired
    BankJournalMapper bankJournalMapper;

    @Autowired
    FinanceJournalMapper financeJournalMapper;

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private IBankJournalService iBankJournalService;

    @Autowired
    private IFinanceJournalService iFinanceJournalService;

    @Autowired
    private SupplierClient supplierClient;

    @Autowired
    private IReviewFormExpService iReviewFormExpService;

    @Autowired
    private RbacClient rbacClient;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private WorkFlowFeign workFlowFeign;

    @Autowired
    private IOrgJournalService iOrgJournalService;

    @Autowired
    private ICateJournalService iCateJournalService;

    @Autowired
    private ISiteJournalService iSiteJournalService;

    @Autowired
    private SiteJournalMapper siteJournalMapper;

    @Autowired
    private RepushHandlerService repushHandlerService;

    @Autowired
    private ReviewFlow reviewFlow;
    private final ThreadPoolExecutor submitExecutor;

    @Autowired
    private IEntryConfigService iEntryConfigService;

    @Autowired
    private IEntryConfigRecordService iEntryConfigRecordService;
    
    @Autowired
    private IEntryRelationRecordService iEntryRelationRecordService;
    
    @Autowired
    private IFileRecordService iFileRecordService;
    
    @Autowired
    private IOrgCateJournalService iOrgCateJournalService;
    
    public ReviewFormServiceImpl() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        submitExecutor = new ThreadPoolExecutor(cpuCount * 2, cpuCount * 2,
                0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),
                new NamedThreadFactory("http-message-sender", true), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * ??????/??????
     *
     * @param reviewFormDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveOrUpdateReviewForm(ReviewFormDTO reviewFormDTO) {
        ReviewForm reviewForm = reviewFormDTO.getReviewForm();
//        List<OrgCateJournal> orgCateJournals = reviewFormDTO.getOrgCateJournals();
//        List<FinanceJournal> financeJournals = reviewFormDTO.getFinanceJournals();
        List<OrgJournal> orgJournals = reviewFormDTO.getOrgJournals();
        List<CateJournal> cateJournals = reviewFormDTO.getCateJournals();
        List<BankJournal> bankJournals = reviewFormDTO.getBankJournals();
        List<SiteJournal> siteJournals = reviewFormDTO.getSiteJournals();
        List<ReviewFormExp> reviewFormExps = reviewFormDTO.getReviewFormExps();
        if (reviewForm == null) throw new BaseException("?????????????????????");
        List<Fileupload> fileUploads = reviewForm.getFileUploads();

        ReviewForm existReviewForm = null;
        Long reviewFormId = reviewForm.getReviewFormId();
        //???????????????????????????
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        String deptName = user.getDepartment();
        reviewForm.setCeeaDeptName(deptName);

        String cbpmInstanceId = "";
        //???????????????Id?????????
        if (reviewFormId != null) {
            //?????????????????????????????????????????????
            existReviewForm = reviewFormMapper.selectById(reviewFormId);
            cbpmInstanceId = existReviewForm.getCbpmInstaceId();
            //????????????????????????????????????
            String approveStatus = existReviewForm.getApproveStatus();
            //???????????????????????????????????????????????????????????????
            if (existReviewForm != null && (ApproveStatusType.SUBMITTED.getValue().equals(approveStatus)
                    || ApproveStatusType.APPROVED.getValue().equals(approveStatus) || ApproveStatusType.ABANDONED.getValue().equals(approveStatus))) {
                throw new BaseException("??????????????????????????????????????????????????????????????????");
            }
        }
        EntryConfig entryConfig = this.checkAllBeforeSave(reviewFormDTO, reviewForm);

        //????????????????????? ????????????
        if (OpType.SUBMISSION.toString().equals(reviewFormDTO.getOpType())) {
            //?????????????????????????????????????????????
            reviewForm.setApproveStatus(ApproveStatusType.SUBMITTED.getValue());
        }
        //????????????
        else {
            reviewForm.setApproveStatus(ApproveStatusType.DRAFT.getValue());
        }
        if (existReviewForm != null) {
            if (StringUtils.isNotBlank(cbpmInstanceId)) {
                reviewForm.setCbpmInstaceId(cbpmInstanceId);
            }
            reviewFormMapper.updateById(reviewForm);
        }
        //????????????????????????????????????Id?????????????????????
        else {
            reviewFormId = IdGenrator.generate();
            reviewForm.setReviewFormId(reviewFormId);
            reviewForm.setReviewFormNumber(baseClient.seqGen(SequenceCodeConstant.SEQ_QUA_REVIEW_FORM_NUM_CODE));
            reviewFormMapper.insert(reviewForm);
        }

        iOrgJournalService.batchSaveOrUpdateOrgJournal(orgJournals, reviewForm.getVendorId(), reviewForm.getReviewFormId(), FormType.REVIEW_FORM.name());
        iCateJournalService.batchSaveOrUpdateCateJournal(cateJournals,reviewForm.getVendorId(), reviewForm.getReviewFormId(), FormType.REVIEW_FORM.name());
        batchSaveOrUpdateBankJournal(bankJournals, reviewForm);
        //??????????????????
        iFileRecordService.batchSaveOrUpdate(reviewFormDTO.getFileRecords(), reviewForm.getReviewFormId(), FormType.REVIEW_FORM.name());
        
        //????????????
        this.saveEntryConfigRecord(entryConfig, reviewForm.getReviewFormId());
        
        //????????????????????? ????????????????????????????????????????????????
        if (OpType.SUBMISSION.toString().equals(reviewFormDTO.getOpType())) {

            //?????????????????????
            checkSitesBeforeSubmit(siteJournals);
        }
        // ??????????????????????????????????????????, ??????????????????????????????erpOrgId, ????????????????????????????????????id??????erpOrgId?????????
        siteJournals = updateErpOrgIds(siteJournals);
        // ?????????????????????, ????????????????????????????????????????????????
        orgSiteDuplicateCheck(siteJournals);
        batchSaveOrUpdateSiteJournal(siteJournals, reviewForm);

        batchSaveOrUpdateReviewFormExp(reviewFormExps, reviewForm);
        //????????????
        if (!CollectionUtils.isEmpty(fileUploads)) {
            fileCenterClient.bindingFileupload(fileUploads, reviewForm.getReviewFormId());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("businessId", reviewFormId);
        //???????????????????????????. ToDo ?????????????????????
//        map.put("fdId", reviewForm.getCbpmInstaceId());
//        map.put("subject", reviewForm.getVendorName());
//        UpdateWrapper<ReviewForm> updateWrapper = new UpdateWrapper<>(
//                new ReviewForm().setReviewFormId(reviewForm.getReviewFormId()));
//        if (!ApproveStatusType.DRAFT.getValue().equals(reviewForm.getApproveStatus())) {
//            Long menuId = reviewFormDTO.getMenuId();
//            Permission menu = rbacClient.getMenu(menuId);
//            if (menu != null) {
//                boolean flowEnable = false;
//                try{
//                    flowEnable = workFlowFeign.getFlowEnable(menuId, menu.getFunctionId(), CbpmFormTemplateIdEnum.QUA_OF_REVIEW.getKey());
//                }catch (FeignException e){
//                    log.error("???????????????????????????,??????????????????????????????,?????? menuId???"+menuId+",functionId"+menu.getFunctionId()
//                            +",templateCode"+CbpmFormTemplateIdEnum.QUA_OF_REVIEW.getKey()+"?????????",e);
//                    throw new BaseException("?????????????????????????????????????????????????????????");
//                }
//                //1.??????:???????????????,
//                if (flowEnable) {
//                    updateApproveStatus(updateWrapper, ApproveStatusType.DRAFT.getValue());
//                    //??????????????????
//                    CbpmRquestParamDTO cbpmRquestParamDTO = new CbpmRquestParamDTO();
//                    cbpmRquestParamDTO.setBusinessId(String.valueOf(reviewFormId));
//                    cbpmRquestParamDTO.setTemplateCode(CbpmFormTemplateIdEnum.QUA_OF_REVIEW.getKey());
//                    cbpmRquestParamDTO.setSubject(reviewForm.getVendorName());
//                    cbpmRquestParamDTO.setFdId(reviewForm.getCbpmInstaceId());
//                    map = workFlowFeign.initProcess(cbpmRquestParamDTO);
//                    return map;
//                }
//            }
//            //2.??????:??????????????????????????????
//            createVendorMainData(orgCateJournals, bankJournals, financeJournals, reviewForm);
//            //?????????????????????????????? ToDo
//            updateApproveStatus(updateWrapper, ApproveStatusType.APPROVED.getValue());
//        }

        //????????????????????? ????????????
//        if (OpType.SUBMISSION.toString().equals(reviewFormDTO.getOpType())) {
//            /* Begin by chenwt24@meicloud.com   2020-09-26 */
//            //TODO ??????????????????
//            String formId = null;
//            try {
//                formId = reviewFlow.submitReviewConfFlow(reviewFormDTO);
//            } catch (Exception e) {
//                throw new BaseException(e.getMessage());
//            }
//            if (StringUtils.isEmpty(formId)) {
//                throw new BaseException(LocaleHandler.getLocaleMsg("??????OA????????????"));
//            }
//            /* End by chenwt24@meicloud.com     2020-09-29 */
//        }
        return map;
    }

    /**
     * ?????????????????????
     * ??????????????????????????????????????????
     *
     * @param siteJournals
     * @modifiedBy xiexh12@meicloud.com 2020-10-07 20:33
     */
    public void orgSiteDuplicateCheck(List<SiteJournal> siteJournals) {
        Set<String> set = new HashSet<>();
        if (!CollectionUtils.isEmpty(siteJournals)) {
            for (SiteJournal siteJournal : siteJournals) {
                Optional.ofNullable(siteJournal.getErpOrgId()).orElseThrow(() -> new BaseException(LocaleHandler.getLocaleMsg("??????????????????{" + siteJournal.getOrgName() + "}???erp????????????id??????.")));
                String orgId = String.valueOf(siteJournal.getErpOrgId());
                String vendorSiteCode = siteJournal.getVendorSiteCode();
                String orgSite = orgId + "-" + vendorSiteCode;
                if (set.contains(orgSite)) {
                    throw new BaseException("???????????????????????????????????????????????????");
                } else {
                    set.add(orgSite);
                }
            }
        }
    }

    /**
     * ?????????????????????????????????erpOrgId, ???erpOrgId???????????????????????????erpOrgId????????????
     *
     * @param siteJournals
     * @return
     */
    public List<SiteJournal> updateErpOrgIds(List<SiteJournal> siteJournals) {
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(siteJournals)) {
            return null;
        }
        siteJournals.forEach(siteJournal -> {
            // ???????????????erpOrgId????????????????????????
            if (null == siteJournal.getErpOrgId()) {
                //Optional.ofNullable(siteJournal.getOrgId()).orElseThrow(() -> new BaseException(LocaleHandler.getLocaleMsg("????????????????????????{" + siteJournal.getOrgName() + "}???id??????.")));
                Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationId(siteJournal.getOrgId()));
                if (null == organization || null == organization.getErpOrgId() || organization.getErpOrgId().isEmpty()) {
                	siteJournal.setErpOrgId(null);
                } else {
                	siteJournal.setErpOrgId(Long.valueOf(organization.getErpOrgId()));
                }
            }
        });
        return siteJournals;
    }

    /**
     * ?????????????????????
     *
     * @param siteJournals
     */
    public void checkSitesBeforeSubmit(List<SiteJournal> siteJournals) {
        if (!CollectionUtils.isEmpty(siteJournals)) {
            for (SiteJournal siteJournal : siteJournals) {
                if (null == siteJournal.getOrgId()) {
                    throw new BaseException("????????????????????????????????????");
                }
                if (StringUtils.isEmpty(siteJournal.getVendorSiteCode())) {
                    throw new BaseException("????????????????????????????????????");
                }
                if (StringUtils.isEmpty(siteJournal.getCountry())) {
                    throw new BaseException("??????????????????????????????");
                }
                if (StringUtils.isEmpty(siteJournal.getAddressDetail())) {
                    throw new BaseException("????????????????????????????????????");
                }
            }
        }
    }

    public void createVendorMainData(List<OrgJournal> orgJournals, List<CateJournal> cateJournals,
                                     List<BankJournal> bankJournals, List<SiteJournal> siteJournals, ReviewForm reviewForm) {

        CompanyInfo companyInfo = supplierClient.getCompanyInfoForAnon(reviewForm.getVendorId());
        if (companyInfo != null && ApproveStatusType.APPROVED.getValue().equals(companyInfo.getStatus()) && StringUtils.isNotEmpty(companyInfo.getCompanyCode())) {
            // ???????????????supplier leader??????
            generateSupplierLeaderRelation(reviewForm);
            // ?????????????????????????????????
            giveVendorMainAccountRole(companyInfo);
        }
        //?????????????????????ou??????,ToDo ????????????????????????????????????,????????????????????????????????????
        if (!CollectionUtils.isEmpty(orgJournals)) {
        	iOrgCateJournalService.generateOrgCategorys(orgJournals, cateJournals, reviewForm.getReviewFormId(), null, OrgCateBillType.REVIEW_FORM.getValue());
        }

        //???????????????????????????
        generateSiteInfos(siteJournals);
        //???????????????????????????
        generateBankInfos(bankJournals);

        //????????????????????????erp?????????Id?????????????????????????????????????????????????????????????????????
        Long erpVendorId = companyInfo.getErpVendorId();

        if (null == erpVendorId) {
            // ??????????????????????????? ??????????????????????????????????????????????????????
//            sendVendorInfo(companyInfo);

            // ????????????????????????erp?????????Id?????????????????????????????????????????????????????????????????????
//            erpVendorId = companyInfo.getErpVendorId();
//            submitExecutor.execute(() -> {
//                List<SiteInfo> sendErpSiteInfoList = supplierClient.getSiteInfosByParamForAnon(
//                        new SiteInfo().setCompanyId(companyInfo.getCompanyId())
//                                .setIfPushErp("N"));
//                //???????????????????????????
//                if (!CollectionUtils.isEmpty(sendErpSiteInfoList)) {
//                    sendVendorSite(sendErpSiteInfoList, erpVendorId);
//                }
//            });
//            submitExecutor.execute(() -> {
//                List<BankInfo> sendErpBankInfoList = supplierClient.getBankInfosByParamForAnon(
//                        new BankInfo().setCompanyId(companyInfo.getCompanyId())
//                                .setIfPushErp("N"));
//                //???????????????????????????
//                if (!CollectionUtils.isEmpty(sendErpBankInfoList)) {
//                    sendVendorBank(sendErpBankInfoList, erpVendorId);
//                }
//            });
//            submitExecutor.execute(() -> {
//                //??????????????????????????????
//                sendVendorContact(companyInfo);
//            });
        }

        // ???????????????????????????
//        sendVendorOtherDatasToErp(companyInfo, erpVendorId);

    }

    /**
     * ??????????????????????????????erp
     *
     * @param companyId
     */
    public void sendVendorOtherDatasToErp(CompanyInfo companyInfo, Long erpVendorId) {

        CompletableFuture.runAsync(() -> {
            try {
                List<SiteInfo> sendErpSiteInfoList = supplierClient.getSiteInfosByParamForAnon(
                        new SiteInfo().setCompanyId(companyInfo.getCompanyId())
                                .setIfPushErp("N"));
                //???????????????????????????
                if (!CollectionUtils.isEmpty(sendErpSiteInfoList)) {
                    sendVendorSite(sendErpSiteInfoList, erpVendorId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                List<BankInfo> sendErpBankInfoList = supplierClient.getBankInfosByParamForAnon(
                        new BankInfo().setCompanyId(companyInfo.getCompanyId())
                                .setIfPushErp("N"));
                //???????????????????????????
                if (!CollectionUtils.isEmpty(sendErpBankInfoList)) {
                    sendVendorBank(sendErpBankInfoList, erpVendorId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                List<ContactInfo> sendErpContactInfoList = supplierClient.getContactInfosByParamForAnon(
                        new ContactInfo().setCompanyId(companyInfo.getCompanyId())
                                .setIfPushErp("N"));
                if (!CollectionUtils.isEmpty(sendErpContactInfoList)) {
                    sendVendorContact(sendErpContactInfoList, erpVendorId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public void generateBankInfos(List<BankJournal> bankJournals) {
        //???????????????????????????
        if (!CollectionUtils.isEmpty(bankJournals)) {
            for (BankJournal bankJournal : bankJournals) {
                if (bankJournal == null) continue;
                BankInfo bankInfo = new BankInfo();
                BeanUtils.copyProperties(bankJournal, bankInfo);
                bankInfo.setCompanyId(bankJournal.getVendorId())
                        .setBankInfoId(bankJournal.getCeeaBankInfoId());
                supplierClient.addBankInfoForAnon(bankInfo);
            }
        }
    }

    public void generateSiteInfos(List<SiteJournal> siteJournals) {
        //???????????????????????????
        if (!CollectionUtils.isEmpty(siteJournals)) {
            for (SiteJournal siteJournal : siteJournals) {
                if (siteJournal == null)
                    continue;
                SiteInfo siteInfo = new SiteInfo();
                BeanUtils.copyProperties(siteJournal, siteInfo);
                siteInfo.setBelongOprId(String.valueOf(siteJournal.getErpOrgId()))
                        .setCompanyId(siteJournal.getVendorId())
                        .setAddressName(siteJournal.getAddressName())
                        .setSiteInfoId(siteJournal.getCeeaSiteInfoId())
                        .setVendorSiteCode(siteJournal.getVendorSiteCode());
                supplierClient.addSiteInfoForAnon(siteInfo);
            }
        }
    }

    /**
     * ??????????????????????????????Erp????????????????????????????????????????????????
     * ????????????Erp?????????erpVendorId????????????????????????
     *
     * @param companyInfo
     * @return
     */
    private void sendVendorInfo(CompanyInfo companyInfo) {
        log.info("?????????????????????????????????...");
        VendorInfoOutputParameters vendorInfoOutput = new VendorInfoOutputParameters();
        String vendorInfoReturnStatus = "";

        try {
            vendorInfoOutput = supplierClient.sendVendorInfo(companyInfo);
            if (null != vendorInfoOutput) {
                vendorInfoReturnStatus = null != vendorInfoOutput.getXesbresultinforec() ? vendorInfoOutput.getXesbresultinforec().getReturnstatus() : "E";
                if ("S".equals(vendorInfoReturnStatus)) {
                    log.info("?????????????????????????????????");
                    //??????erp???????????????????????????
                    repushHandlerService.save("???????????????????????????(??????????????????" + companyInfo.getCompanyCode() + ")", "???????????????:" + companyInfo.getCompanyCode(),
                            SupplierClient.class.getName(), "sendVendorInfo",
                            0, RepushStatus.SUCCESS, RepushConst.NOT_TO_REPUSH, null, null, companyInfo);

                    /** ???????????????erpVendorId???erpVendorCode???????????????????????? **/
                    Long erpVendorId = Long.valueOf((null != vendorInfoOutput.getXesbresultinforec() ? vendorInfoOutput.getXesbresultinforec().getAttr1() : ""));
                    String erpVendorCode = (null != vendorInfoOutput.getXesbresultinforec() ? vendorInfoOutput.getXesbresultinforec().getAttr2() : "");
                    //??????????????????erpVendorId???erpVendorCode
                    companyInfo.setErpVendorId(erpVendorId)
                            .setErpVendorCode(erpVendorCode);
                    supplierClient.modifyForAnon(companyInfo);
                } else {
                    throw new BaseException("????????????????????????????????????????????????????????????[" + companyInfo.getCompanyName() + "]???erp?????????");
                }
            }
        } catch (Exception e) {
            throw new BaseException("????????????????????????????????????????????????????????????[" + companyInfo + "]???erp?????????");
        }
    }

    /**
     * ??????????????????????????????erp
     *
     * @param saveErpBankInfoList
     * @param erpVendorId
     */
    private void sendVendorBank(List<BankInfo> saveErpBankInfoList, Long erpVendorId) {
        /** ??????????????????????????????erp ??????---------**/
        if (!CollectionUtils.isEmpty(saveErpBankInfoList)) {
            log.info("??????????????????????????????????????????" + saveErpBankInfoList.size() + "???");
            String vendorBankReturnStatus = "";

            for (BankInfo bankInfo : saveErpBankInfoList) {
                if (Objects.nonNull(bankInfo)) {
                    bankInfo.setErpVendorId(erpVendorId);
                    try {
                        log.info("?????????????????????????????????...");
                        VendorBankOutputParameters vendorBankOutput = supplierClient.sendVendorBank(bankInfo);
                        if (Objects.nonNull(vendorBankOutput)) {
                            vendorBankReturnStatus = null != vendorBankOutput.getXesbresultinforec() ? vendorBankOutput.getXesbresultinforec().getReturnstatus() : "E";
                            if ("S".equals(vendorBankReturnStatus)) {
                                log.info("????????????????????????????????????");
                                //??????erp?????????????????????????????????????????????????????????erp?????????Y
                                supplierClient.addBankInfoForAnon(bankInfo.setIfPushErp("Y"));
                                repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "?????????/?????????" + bankInfo.getBankName() + "/" + bankInfo.getOpeningBank(), "?????????????????????id:" + bankInfo.getBankInfoId(),
                                        SupplierClient.class.getName(), "sendVendorBank", 0, RepushStatus.SUCCESS, RepushConst.NOT_TO_REPUSH, null, null, bankInfo);
                            } else {
                                log.info("???????????????????????????????????????????????????" + JsonUtil.entityToJsonStr(vendorBankOutput.getXesbresultinforec().getReturnmsg()));
                                //?????????????????????????????????
                                repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "?????????/?????????" + bankInfo.getBankName() + "/" + bankInfo.getOpeningBank(), "?????????????????????id:" + bankInfo.getBankInfoId(),
                                        SupplierClient.class.getName(), "sendVendorBank", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushVendorBankCallBack(), null, bankInfo);
                            }
                        }
                    } catch (Exception e) {
                        log.error("??????????????????????????????erp?????????: " + e);
                        //?????????????????????????????????
                        repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "?????????/?????????" + bankInfo.getBankName() + "/" + bankInfo.getOpeningBank(), "?????????????????????id:" + bankInfo.getBankInfoId(),
                                SupplierClient.class.getName(), "sendVendorBank", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushVendorBankCallBack(), null, bankInfo);
                    }
                }
            }
        }
        /** ??????????????????????????????Erp ??????---------**/
    }

    /**
     * ????????????????????????erp
     *
     * @param sendErpSiteInfoList
     * @param erpVendorId
     */
    private void sendVendorSite(List<SiteInfo> sendErpSiteInfoList, Long erpVendorId) {
        if (!CollectionUtils.isEmpty(sendErpSiteInfoList)) {
            log.info("??????????????????????????????????????????" + sendErpSiteInfoList.size() + "???");
            String vendorSiteReturnStatus = "";

            for (SiteInfo siteInfo : sendErpSiteInfoList) {
                if (!Objects.isNull(siteInfo)) {
                    siteInfo.setErpVendorId(erpVendorId);
                    try {
                        log.info("?????????????????????????????????...");
                        VendorSiteOutputParameters vendorSiteOutput = supplierClient.sendVendorSite(siteInfo);
                        if (Objects.nonNull(vendorSiteOutput)) {
                            vendorSiteReturnStatus = null != vendorSiteOutput.getXesbresultinforec() ? vendorSiteOutput.getXesbresultinforec().getReturnstatus() : "E";
                            if ("S".equals(vendorSiteReturnStatus)) {
                                log.info("????????????????????????????????????");
                                //??????erp?????????????????????????????????????????????????????????erp?????????Y
                                supplierClient.addSiteInfoForAnon(siteInfo.setIfPushErp("Y"));
                                repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "???????????????/???????????????" + siteInfo.getOrgName() + "/" + siteInfo.getVendorSiteCode(), "?????????????????????id:" + siteInfo.getSiteInfoId(),
                                        SupplierClient.class.getName(), "sendVendorSite", 0, RepushStatus.SUCCESS, RepushConst.NOT_TO_REPUSH, null, null, siteInfo);
                            } else {
                                log.info("????????????????????????????????????????????????: " + JsonUtil.entityToJsonStr(vendorSiteOutput.getXesbresultinforec().getReturnmsg()));
                                //?????????????????????????????????
                                repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "???????????????/???????????????" + siteInfo.getOrgName() + "/" + siteInfo.getVendorSiteCode(), "?????????????????????id:" + siteInfo.getSiteInfoId(),
                                        SupplierClient.class.getName(), "sendVendorSite", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushVendorSiteCallBack(), null, siteInfo);
                            }
                        }
                    } catch (Exception e) {
                        log.error("??????????????????????????????erp ?????????: " + e);
                        //?????????????????????????????????
                        repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "???????????????/???????????????" + siteInfo.getOrgName() + "/" + siteInfo.getVendorSiteCode(), "?????????????????????id:" + siteInfo.getSiteInfoId(),
                                SupplierClient.class.getName(), "sendVendorSite", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushVendorSiteCallBack(), null, siteInfo);
                    }
                }
            }
        }
    }

    /**
     * ?????????????????????????????????erp
     *
     * @param companyInfo
     */
    private void sendVendorContact(List<ContactInfo> sendContactInfoList, Long erpVendorId) {
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(sendContactInfoList)) {
            log.info("?????????????????????????????????????????????" + sendContactInfoList.size() + "???");
            String vendorContactReturnStatus = "";

            for (ContactInfo contactInfo : sendContactInfoList) {
                if (!Objects.isNull(contactInfo)) {
                    contactInfo.setErpVendorId(erpVendorId);
                    try {
                        log.info("????????????????????????????????????????????????...");
                        VendorContactOutputParameters vendorContactOutput = supplierClient.sendVendorContact(contactInfo);
                        if (Objects.nonNull(vendorContactOutput)) {
                            vendorContactReturnStatus = null != vendorContactOutput.getXesbresultinforec() ? vendorContactOutput.getXesbresultinforec().getReturnstatus() : "E";
                            if ("S".equals(vendorContactReturnStatus)) {
                                log.info("???????????????????????????????????????");
                                //??????erp?????????????????????????????????????????????????????????erp?????????Y
                                supplierClient.addContactInfoForAnon(contactInfo.setIfPushErp("Y"));
                                repushHandlerService.save("?????????????????????????????????erp?????????Id: " + erpVendorId + "???????????????" + contactInfo.getContactName(), "????????????????????????id:" + contactInfo.getContactInfoId(),
                                        SupplierClient.class.getName(), "sendVendorContact",
                                        0, RepushStatus.SUCCESS, RepushConst.NOT_TO_REPUSH, null, null, contactInfo);
                            } else {
                                log.info("???????????????????????????????????????????????????: " + JsonUtil.entityToJsonStr(vendorContactOutput.getXesbresultinforec().getReturnmsg()));
                                //?????????????????????????????????
                                repushHandlerService.save("?????????????????????????????????erp?????????Id: " + erpVendorId + "???????????????" + contactInfo.getContactName(), "????????????????????????id:" + contactInfo.getContactInfoId(),
                                        SupplierClient.class.getName(), "sendVendorContact", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushVendorContactCallBack(), null, contactInfo);
                            }
                        }
                    } catch (Exception e) {
                        //?????????????????????????????????
                        log.error("?????????????????????????????????erp?????????: " + e);
                        repushHandlerService.save("?????????????????????????????????erp?????????Id: " + erpVendorId + "???????????????" + contactInfo.getContactName(), "????????????????????????id:" + contactInfo.getContactInfoId(),
                                SupplierClient.class.getName(), "sendVendorContact", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushVendorContactCallBack(), null, contactInfo);
                    }
                }
            }

        }
        /** ?????????????????????????????????Erp ??????---------**/
    }

    private void createOrModifyOrgCategory(OrgCateJournal orgCateJournal, Long categoryId, Long orgId, Long vendorId, OrgCategory repeatOrgCategory, OrgInfo repeatOrgInfo, String accessProcess, OrgInfo orgInfo) {
        //????????????????????????????????????,?????????????????????????????????????????????????????????,??????????????????????????????????????????.
        if (repeatOrgCategory != null) {
            if (!CategoryStatus.QUALIFIED.name().equals(repeatOrgCategory.getServiceStatus())
                    && !CategoryStatus.APPLICATION.name().equals(repeatOrgCategory.getServiceStatus())) {
                if (categoryId != null) {
                    OrgCategory orgCategory = assembleOrgCategory(orgCateJournal, vendorId, accessProcess);
                    supplierClient.updateOrgCategoryServiceStatusForAnon(orgCategory);
                }
            }
        }
        //???????????????????????????,?????????????????????????????????????????????????????????,??????????????????????????????????????????.
        if (repeatOrgInfo != null) {
            if (!OrgStatus.INTRODUCTION.name().equals(repeatOrgInfo.getServiceStatus())
                    && !OrgStatus.EFFECTIVE.name().equals(repeatOrgInfo.getServiceStatus())) {
                assembleOrgInfo(orgCateJournal, vendorId, accessProcess, orgInfo);
                supplierClient.updateOrgInfoServiceStatusForAnon(orgInfo);
            }
        }
        //?????????????????????????????????????????????????????????.
        if (repeatOrgCategory == null) {
            if (categoryId != null) {
                OrgCategory orgCategory = assembleOrgCategory(orgCateJournal, vendorId, accessProcess);
                supplierClient.addOrgCategoryForAnon(orgCategory);
                if (orgId != null) {
                    if (repeatOrgInfo == null) {
                        assembleOrgInfo(orgCateJournal, vendorId, accessProcess, orgInfo);
                        supplierClient.addOrgInfoForAnon(orgInfo);
                    }
                }
            } else {
                if (repeatOrgInfo == null) {
                    assembleOrgInfo(orgCateJournal, vendorId, accessProcess, orgInfo);
                    supplierClient.addOrgInfoForAnon(orgInfo);
                }
            }
        }
    }

    /**
     * ???????????????supplier leader
     */
    public void generateSupplierLeaderRelation(ReviewForm reviewForm) {
        Long compoanyId = Objects.nonNull(reviewForm.getVendorId()) ? reviewForm.getVendorId() : null;
        Long responsibilityId = Objects.nonNull(reviewForm.getCreatedId()) ? reviewForm.getCreatedId() : null;
        if (null != compoanyId && null != responsibilityId) {
            supplierClient.saveOrUpdateSupplierLeaderForAnon(compoanyId, responsibilityId);
        }
    }

    private void assembleOrgInfo(OrgCateJournal orgCateJournal, Long vendorId, String accessProcess, OrgInfo orgInfo) {
        BeanUtils.copyProperties(orgCateJournal, orgInfo);
        if (AccessProcessType.ULRA_SIMPLIFY.getValue().equals(accessProcess)) {
            orgInfo.setCompanyId(vendorId).setServiceStatus(OrgStatus.EFFECTIVE.toString())
                    .setStartDate(LocalDate.now());
        } else {
            orgInfo.setCompanyId(vendorId).setServiceStatus(OrgStatus.INTRODUCTION.toString());
        }
    }

    private OrgCategory assembleOrgCategory(OrgCateJournal orgCateJournal, Long vendorId, String accessProcess) {
        OrgCategory orgCategory = new OrgCategory();
        BeanUtils.copyProperties(orgCateJournal, orgCategory);
        if (AccessProcessType.ULRA_SIMPLIFY.getValue().equals(accessProcess)) {
            orgCategory.setCompanyId(vendorId).setServiceStatus(CategoryStatus.QUALIFIED.toString())
                    .setStartDate(LocalDate.now());
        } else {
            orgCategory.setCompanyId(vendorId).setServiceStatus(CategoryStatus.APPLICATION.toString());
        }
        return orgCategory;
    }

    //?????????????????????????????????
    public void giveVendorMainAccountRole(CompanyInfo companyInfo) {
        User user = rbacClient.getUserByParmForAnon(new User()
                .setCompanyId(companyInfo.getCompanyId())
                .setMainType(MainType.Y.name()));
        Long userId = user.getUserId();
        Long roleId = rbacClient.getRoleByParmForAnon(new Role().setRoleType(RoleType.SUPPLIER_INIT.name())).getRoleId();
        rbacClient.modifyRoleByUserIdForAnon(userId, roleId);
    }

    /**
     * ????????????
     *
     * @param reviewForm
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pass(ReviewForm reviewForm) {
        this.updateById(new ReviewForm().setReviewFormId(reviewForm.getReviewFormId()).setApproveStatus(ApproveStatusType.APPROVED.getValue()));
        List<CateJournal> cateJournals = iCateJournalService.list(new QueryWrapper<>(new CateJournal().setFormId(reviewForm.getReviewFormId()).setFormType(FormType.REVIEW_FORM.name()).setVendorId(reviewForm.getVendorId())));
        List<OrgJournal> orgJournals = iOrgJournalService.list(new QueryWrapper<>(new OrgJournal().setFormId(reviewForm.getReviewFormId()).setFormType(FormType.REVIEW_FORM.name()).setVendorId(reviewForm.getVendorId())));
        List<BankJournal> bankJournals = iBankJournalService.list(new QueryWrapper<>(new BankJournal().setFormId(reviewForm.getReviewFormId()).setFormType(FormType.REVIEW_FORM.name()).setVendorId(reviewForm.getVendorId())));
        List<SiteJournal> siteJournals = iSiteJournalService.list(new QueryWrapper<>(new SiteJournal().setFormId(reviewForm.getReviewFormId()).setFormType(FormType.REVIEW_FORM.name()).setVendorId(reviewForm.getVendorId())));
        ReviewForm byId = this.getById(reviewForm.getReviewFormId());
        updateSiteJournals(siteJournals);
        createVendorMainData(orgJournals, cateJournals, bankJournals, siteJournals, byId);
    }

    @Override
    public InfoDTO listOrgAndCategoryByReviewId(Long reviewFormId) {
        List<OrgJournal> orgJournals = iOrgJournalService.list(new QueryWrapper<>(new OrgJournal().setFormId(reviewFormId).setFormType(FormType.REVIEW_FORM.name())));
        List<CateJournal> cateJournals = iCateJournalService.list(new QueryWrapper<>(new CateJournal().setFormId(reviewFormId).setFormType(FormType.REVIEW_FORM.name())));
        List<OrgInfo> orgInfos = new ArrayList<>();
        List<OrgCategory> orgCategories = new ArrayList<>();
        if (!CollectionUtils.isEmpty(orgJournals)) {
            for (OrgJournal orgJournal : orgJournals) {
                if (orgJournal == null) continue;
//                OrgInfo orgInfo = supplierClient.getOrgInfoByOrgIdAndCompanyId(orgJournal.getOrgId(), orgJournal.getVendorId());
                OrgInfo orgInfo = new OrgInfo();
                BeanUtils.copyProperties(orgJournal, orgInfo);
                orgInfos.add(orgInfo);
            }
        }
        if (!CollectionUtils.isEmpty(cateJournals)) {
            for (CateJournal cateJournal : cateJournals) {
                if (cateJournal == null) continue;
//                OrgCategory orgCategory = supplierClient.getByCategoryIdAndOrgIdAndCompanyId(cateJournal.getCategoryId(), null, cateJournal.getVendorId());
                OrgCategory orgCategory = new OrgCategory();
                BeanUtils.copyProperties(cateJournal, orgCategory);
                orgCategories.add(orgCategory);
            }
        }
        return new InfoDTO().setOrgInfos(orgInfos).setOrgCategorys(orgCategories);
    }
    
    @Override
    public List<OrgCateJournal> listOrgCateJournalByReviewId(Long reviewFormId) {
        List<OrgJournal> orgJournals = iOrgJournalService.list(new QueryWrapper<>(new OrgJournal().setFormId(reviewFormId).setFormType(FormType.REVIEW_FORM.name())));
        List<CateJournal> cateJournals = iCateJournalService.list(new QueryWrapper<>(new CateJournal().setFormId(reviewFormId).setFormType(FormType.REVIEW_FORM.name())));
        List<OrgCateJournal> list = new ArrayList<OrgCateJournal>();
        OrgCateJournal orgCate = null;
        if (!CollectionUtils.isEmpty(orgJournals) && !CollectionUtils.isEmpty(cateJournals)) {
            for (OrgJournal orgJournal : orgJournals) {
            	 for (CateJournal cateJournal : cateJournals) {
            		 orgCate = new OrgCateJournal();
            		 orgCate.setVendorId(orgJournal.getVendorId());
            		 orgCate.setOrgId(orgJournal.getOrgId());
            		 orgCate.setOrgName(orgJournal.getOrgName());
            		 orgCate.setOrgCode(orgJournal.getOrgCode());
            		 orgCate.setCategoryId(cateJournal.getCategoryId());
            		 orgCate.setCategoryCode(cateJournal.getCategoryCode());
            		 orgCate.setCategoryName(cateJournal.getCategoryName());
            		 orgCate.setCategoryFullName(cateJournal.getCategoryFullName());
            		 list.add(orgCate);
            	 }
            }
        }
        return list;
    }

    private void updateSiteJournals(List<SiteJournal> siteJournals) {
        if (!CollectionUtils.isEmpty(siteJournals)) {
            siteJournals.forEach(siteJournal -> {
                siteJournal.setCeeaAllowDelete(SiteJournalStatus.N.name());
                iSiteJournalService.updateById(siteJournal);
            });
        }
    }

    private void createFinanceInfoData(List<FinanceJournal> financeJournals, OrgCateJournal orgCateJournal) {
        if (!CollectionUtils.isEmpty(financeJournals)) {
            for (FinanceJournal financeJournal : financeJournals) {
                if (financeJournal == null) continue;
                FinanceInfo repeatFinanceInfo = supplierClient.getFinanceInfoByCompanyIdAndOrgIdForAnon(orgCateJournal.getVendorId(), orgCateJournal.getOrgId());
                if (repeatFinanceInfo != null) continue;
                FinanceInfo financeInfo = new FinanceInfo();
                BeanUtils.copyProperties(financeJournal, financeInfo);
                financeInfo.setCompanyId(orgCateJournal.getVendorId());
                if (orgCateJournal.getOrgId() != null) {
                    financeInfo.setOrgId(orgCateJournal.getOrgId());
                    financeInfo.setOrgCode(orgCateJournal.getOrgCode());
                    financeInfo.setOrgName(orgCateJournal.getOrgName());
                }
                supplierClient.addFinanceInfoForAnon(financeInfo);
            }
        }
    }

    private void createBankInfoData(List<BankJournal> bankJournals, OrgCateJournal orgCateJournal) {
        if (!CollectionUtils.isEmpty(bankJournals)) {
            for (BankJournal bankJournal : bankJournals) {
                if (bankJournal == null) continue;
                BankInfo bankInfo = new BankInfo();
                bankInfo.setCompanyId(bankJournal.getVendorId())
                        .setBankAccount(bankJournal.getBankAccount())
                        .setUnionCode(bankJournal.getUnionCode()).setOrgId(orgCateJournal.getOrgId());
                BankInfo repeatBankInfo = supplierClient.getBankInfoByParmForAnon(bankInfo);
                if (repeatBankInfo == null) {
                    BeanUtils.copyProperties(bankJournal, bankInfo);
                    if (orgCateJournal.getOrgId() != null) {
                        bankInfo.setOrgId(orgCateJournal.getOrgId());
                        bankInfo.setOrgCode(orgCateJournal.getOrgCode());
                        bankInfo.setOrgName(orgCateJournal.getOrgName());
                    }
                    supplierClient.addBankInfoForAnon(bankInfo);
                }
            }
        }
    }

    private void batchSaveOrUpdateFinanceJournal(List<FinanceJournal> financeJournals, ReviewForm reviewForm) {
        if (!CollectionUtils.isEmpty(financeJournals)) {
            for (FinanceJournal financeJournal : financeJournals) {
                if (financeJournal == null) continue;
                if (financeJournal.getFinanceJournalId() == null) {
                    long id = IdGenrator.generate();
                    financeJournal.setFinanceJournalId(id).setVendorId(reviewForm.getVendorId())
                            .setFormId(reviewForm.getReviewFormId()).setFormType(FormType.REVIEW_FORM.toString());
                    financeJournalMapper.insert(financeJournal);
                } else {
                    financeJournalMapper.updateById(financeJournal);
                }
            }
        }
    }

    private void batchSaveOrUpdateBankJournal(List<BankJournal> bankJournals, ReviewForm reviewForm) {
        if (!CollectionUtils.isEmpty(bankJournals)) {
            for (BankJournal bankJournal : bankJournals) {
                if (bankJournal == null) continue;
                if (bankJournal.getBankJournalId() == null) {
                    long id = IdGenrator.generate();
                    bankJournal.setBankJournalId(id).setVendorId(reviewForm.getVendorId())
                            .setFormId(reviewForm.getReviewFormId()).setFormType(FormType.REVIEW_FORM.toString());
                    bankJournalMapper.insert(bankJournal);
                } else {
                    bankJournalMapper.updateById(bankJournal);
                }
            }
        }
    }

    //?????????????????????
    private void batchSaveOrUpdateSiteJournal(List<SiteJournal> siteJournals, ReviewForm reviewForm) {
        if (!CollectionUtils.isEmpty(siteJournals)) {
            for (SiteJournal siteJournal : siteJournals) {
                if (siteJournal == null) continue;
                if (siteJournal.getSiteJournalId() == null) {
                    long id = IdGenrator.generate();
                    siteJournal.setSiteJournalId(id)
                            .setVendorId(reviewForm.getVendorId())
                            .setAddressName(siteJournal.getVendorSiteCode())
                            .setFormId(reviewForm.getReviewFormId())
                            .setFormType(FormType.REVIEW_FORM.toString());
                    Integer count = siteJournalMapper.selectCount(Wrappers.lambdaQuery(SiteJournal.class)
                            .eq(SiteJournal::getFormId, siteJournal.getFormId())
                            .eq(SiteJournal::getErpOrgId, siteJournal.getErpOrgId())
                            .eq(SiteJournal::getVendorId, siteJournal.getVendorId())
                            .eq(SiteJournal::getVendorSiteCode, siteJournal.getVendorSiteCode())
                            .eq(SiteJournal::getAddressName, siteJournal.getAddressName())
                    );
                    if (count > 0) {
                        throw new BaseException("????????????????????????????????????");
                    }
                    siteJournalMapper.insert(siteJournal);
                } else {

                    siteJournalMapper.updateById(siteJournal);
                }
            }

        }
    }

    @Override
    @AuthData(module = MenuEnum.QUA_OF_REVIEW)
    public PageInfo<ReviewForm> listPageByParm(ReviewForm reviewForm) {
        if (reviewForm == null) return new PageInfo<>();
        PageUtil.startPage(reviewForm.getPageNum(), reviewForm.getPageSize());
        ReviewForm wrapperReviewForm = new ReviewForm();
        if (!StringUtil.isEmpty(reviewForm.getApproveStatus())) {
            wrapperReviewForm.setApproveStatus(reviewForm.getApproveStatus());
        }
        if (!StringUtil.isEmpty(reviewForm.getQuaReviewType())) {
            wrapperReviewForm.setQuaReviewType(reviewForm.getQuaReviewType());
        }
        if (!StringUtil.isEmpty(reviewForm.getIfDevelop())) {
            wrapperReviewForm.setIfDevelop(reviewForm.getIfDevelop());
        }
//        if (!StringUtil.isEmpty(reviewForm.getIfSiteForm())) {
//            wrapperReviewForm.setIfSiteForm(reviewForm.getIfSiteForm());
//        }
        if (reviewForm.getVendorId() != null) {
            wrapperReviewForm.setVendorId(reviewForm.getVendorId());
        }
        if (StringUtils.isNotBlank(reviewForm.getCeeaIfVendorAuth())) {
            wrapperReviewForm.setCeeaIfVendorAuth(reviewForm.getCeeaIfVendorAuth());
        }
        QueryWrapper<ReviewForm> queryWrapper = new QueryWrapper<>(wrapperReviewForm);
        queryWrapper.like(!StringUtil.isEmpty(reviewForm.getReviewFormNumber()), "REVIEW_FORM_NUMBER", reviewForm.getReviewFormNumber());
        queryWrapper.like(!StringUtil.isEmpty(reviewForm.getVendorName()), "VENDOR_NAME", reviewForm.getVendorName());
        queryWrapper.orderByDesc("LAST_UPDATE_DATE");
        List<ReviewForm> reviewForms = reviewFormMapper.selectList(queryWrapper);
        return new PageInfo<ReviewForm>(reviewForms);
    }

    @Override
    public ReviewFormDTO getReviewFormDTO(Long reviewFormId) {
        ReviewFormDTO reviewFormDTO = new ReviewFormDTO();
        ReviewForm reviewForm = reviewFormMapper.selectById(reviewFormId);
        if (reviewForm != null) {
            //??????reviewFormId?????????????????????????????????
//        QueryWrapper<OrgCateJournal> orgCateJournalWrapper = new QueryWrapper<>(
//                new OrgCateJournal().setOrgCateBillType(OrgCateBillType.REVIEW_FORM.getValue())
//                                    .setOrgCateBillId(reviewFormId));
//        List<OrgCateJournal> orgCateJournals = orgCateJournalMapper.selectList(orgCateJournalWrapper);

            //??????reviewFormId??????????????????
//        List<FinanceJournal> financeJournals = iFinanceJournalService.listFinanceJournal(reviewFormId, reviewForm.getVendorId());

            //??????reviewFormId??????????????????
            QueryWrapper<ReviewFormExp> formExpQueryWrapper = new QueryWrapper<>(
                    new ReviewFormExp().setReviewFormId(reviewFormId));
            List<ReviewFormExp> reviewFormExps = reviewFormExpMapper.selectList(formExpQueryWrapper);

            //??????reviewFormId??????????????????
            List<BankJournal> bankJournals = iBankJournalService.listBankJournal(reviewFormId, reviewForm.getVendorId());

            //??????reviewFormId???????????????????????????
            List<SiteJournal> siteJournals = iSiteJournalService.listSiteJournal(reviewFormId, reviewForm.getVendorId());
            siteJournals.forEach(e -> e.setCeeaAllowDelete(Objects.isNull(e.getCeeaSiteInfoId()) ? "Y" : "N"));

            //??????reviewFormId????????????ou??????
            List<OrgJournal> orgJournals = iOrgJournalService.list(new QueryWrapper<>(new OrgJournal()
                    .setFormId(reviewForm.getReviewFormId())
                    .setFormType(FormType.REVIEW_FORM.name())));

            //??????reviewFormId??????????????????
            List<CateJournal> cateJournals = iCateJournalService.list(new QueryWrapper<>(new CateJournal().setFormId(reviewForm.getReviewFormId())
                    .setFormType(FormType.REVIEW_FORM.name())));

            //??????ReviewFormDTO
            reviewFormDTO.setReviewForm(reviewForm);
            reviewFormDTO.setReviewFormExps(reviewFormExps);
            reviewFormDTO.setBankJournals(bankJournals);
            reviewFormDTO.setSiteJournals(siteJournals);
            reviewFormDTO.setCateJournals(cateJournals);
            reviewFormDTO.setOrgJournals(orgJournals);
            
            reviewFormDTO.setFileRecords(iFileRecordService.getFileRecord(reviewForm.getReviewFormId(), FormType.REVIEW_FORM.name()));
//        reviewFormDTO.setOrgCateJournals(orgCateJournals);
//        reviewFormDTO.setFinanceJournals(financeJournals);
        }
        return reviewFormDTO;
    }

    @Override
    public List<OrgCateServiceStatusDTO> listOrgCateServiceStatusByReviewId(Long reviewFormId) {
        List<OrgCateServiceStatusDTO> orgCateServiceStatusDTOS = new ArrayList<>();
        //????????????
        List<OrgJournal> orgJournals = iOrgJournalService.list(new QueryWrapper<>(new OrgJournal().setFormId(reviewFormId).setFormType(FormType.REVIEW_FORM.name())));
        //????????????
        List<CateJournal> cateJournals = iCateJournalService.list(new QueryWrapper<>(new CateJournal().setFormId(reviewFormId).setFormType(FormType.REVIEW_FORM.name())));
        
        if (!CollectionUtils.isEmpty(orgJournals) && !CollectionUtils.isEmpty(cateJournals)) {
        	for (OrgJournal org :orgJournals) {
        		for (CateJournal cate : cateJournals) {
        			Long orgId = org.getOrgId();
                    Long categoryId = cate.getCategoryId();
                    Long vendorId = cate.getVendorId();
                    OrgCateServiceStatusDTO orgCateServiceStatusByCode = supplierClient.getOrgCateServiceStatusById(orgId, categoryId, vendorId);
                    orgCateServiceStatusDTOS.add(orgCateServiceStatusByCode);
        		}
        	}
        }
        return orgCateServiceStatusDTOS;
    }

    @Override
    @Transactional
    public void deleteReviewFormById(Long reviewFormId) {
        ReviewForm reviewForm = reviewFormMapper.selectById(reviewFormId);
        if (reviewForm != null) {
            if (ApproveStatusType.DRAFT.getValue().equals(reviewForm.getApproveStatus())
                    || ApproveStatusType.REJECTED.getValue().equals(reviewForm.getApproveStatus())) {
                reviewFormMapper.deleteById(reviewFormId);
                iReviewFormExpService.remove(new QueryWrapper<>(new ReviewFormExp().setReviewFormId(reviewFormId)));
                iBankJournalService.remove(new QueryWrapper<>(new BankJournal().setFormId(reviewFormId)
                        .setFormType(FormType.REVIEW_FORM.toString())));
                iSiteJournalService.remove(new QueryWrapper<>(new SiteJournal().setFormId(reviewFormId)
                        .setFormType(FormType.REVIEW_FORM.toString())));
                iOrgJournalService.remove(new QueryWrapper<>(new OrgJournal().setFormId(reviewFormId).setFormType(FormType.REVIEW_FORM.name())));
                iCateJournalService.remove(new QueryWrapper<>(new CateJournal().setFormId(reviewFormId).setFormType(FormType.REVIEW_FORM.name())));
//                iFinanceJournalService.remove(new QueryWrapper<>(new FinanceJournal().setFormId(reviewFormId)
//                                                                                         .setFormType(FormType.REVIEW_FORM.toString())));
//                iOrgCateJournalService.remove(new QueryWrapper<>(new OrgCateJournal().setOrgCateBillType(OrgCateBillType.REVIEW_FORM.toString())
//                                                                                         .setOrgCateBillId(reviewFormId)));
                fileCenterClient.deleteByParam(new Fileupload().setBusinessId(reviewFormId));
            } else {
                throw new BaseException("????????????????????????????????????,????????????");
            }

        }
    }

    /**
     * ????????????
     *
     * @param reviewFormId
     */
    @Transactional
    @Override
    public void abandon(Long reviewFormId) {
        ReviewFormDTO reviewFormDTO = this.getReviewFormDTO(reviewFormId);
        ReviewForm reviewForm = reviewFormDTO.getReviewForm();
        Assert.isTrue(!ObjectUtils.isEmpty(reviewForm), "??????????????????????????????");
        String approveStatus = reviewForm.getApproveStatus();
        Assert.isTrue(ApproveStatusType.REJECTED.getValue().equals(approveStatus) || ApproveStatusType.WITHDRAW.getValue().equals(approveStatus), "???????????????????????????????????????????????????");
        reviewForm.setApproveStatus(ApproveStatusType.ABANDONED.getValue());
        this.updateById(reviewForm);
        SrmFlowBusWorkflow srmworkflowForm = baseClient.getSrmFlowBusWorkflow(reviewFormId);
        if (srmworkflowForm != null) {
            try {
                reviewFormDTO.setProcessType("N");
                reviewFlow.submitReviewConfFlow(reviewFormDTO);
            } catch (Exception e) {
                Assert.isTrue(false, "??????????????????????????????");
            }
        }
    }


    @Override
    @Transactional
    public void updateReviewFormAfterWorkFlow(ReviewForm reviewForm) {
//        List<OrgCateJournal> orgCateJournals = iOrgCateJournalService.list(new QueryWrapper<>(new OrgCateJournal()
//                .setOrgCateBillId(reviewForm.getReviewFormId()).setOrgCateBillType(OrgCateBillType.REVIEW_FORM.getValue())));
        List<FinanceJournal> financeJournals = iFinanceJournalService.list(new QueryWrapper<>(new FinanceJournal()
                .setFormId(reviewForm.getReviewFormId()).setFormType(FormType.REVIEW_FORM.toString())));
        List<BankJournal> bankJournals = iBankJournalService.list(new QueryWrapper<>(new BankJournal()
                .setFormId(reviewForm.getReviewFormId()).setFormType(FormType.REVIEW_FORM.toString())));
//        //??????????????????????????????
//        createVendorMainData(orgCateJournals, bankJournals, financeJournals, reviewForm);

        //????????????????????????
        this.updateById(new ReviewForm().setReviewFormId(reviewForm.getReviewFormId()).setApproveStatus(ApproveStatusType.APPROVED.getValue()));

    }

    private void batchSaveOrUpdateReviewFormExp(List<ReviewFormExp> reviewFormExps, ReviewForm reviewForm) {
        for (ReviewFormExp reviewFormExp : reviewFormExps) {
            if (reviewFormExp == null) continue;
            if (reviewFormExp.getReviewFormExpId() == null) {
                long id = IdGenrator.generate();
                reviewFormExp.setReviewFormExpId(id);
                reviewFormExp.setReviewFormId(reviewForm.getReviewFormId());
                reviewFormExp.setVendorId(reviewForm.getVendorId());
                reviewFormExpMapper.insert(reviewFormExp);
            } else {
                reviewFormExpMapper.updateById(reviewFormExp);
            }
        }
    }

    /*private void batchSaveOrUpdateReviewAttach(ReviewFormDTO reviewFormDTO, ReviewForm reviewForm) {
        List<ReviewAttach> reviewAttaches = reviewFormDTO.getReviewAttaches();
        for (ReviewAttach reviewAttach : reviewAttaches) {
            if (reviewAttach == null) continue;
            if (reviewAttach.getAttachFileId() == null) {
                long id = IdGenrator.generate();
                reviewAttach.setAttachFileId(id);
                reviewAttach.setReviewFormId(reviewForm.getReviewFormId());
                reviewAttach.setVendorId(reviewForm.getVendorId());
                reviewAttachMapper.insert(reviewAttach);
            } else {
                reviewAttachMapper.updateById(reviewAttach);
            }
        }
    }*/

    private void batchSaveOrUpdateOrgCateJournal(List<OrgCateJournal> orgCateJournals, ReviewForm reviewForm) {
        checkDuplicationOrgCateJournal(orgCateJournals, reviewForm);
        if (!CollectionUtils.isEmpty(orgCateJournals)) {
            for (OrgCateJournal orgCateJournal : orgCateJournals) {
                if (orgCateJournal == null) continue;
                if (orgCateJournal.getOrgCateJournalId() == null) {
                    long id = IdGenrator.generate();
                    orgCateJournal.setOrgCateJournalId(id);
                    orgCateJournal.setOrgCateBillType(OrgCateBillType.REVIEW_FORM.getValue());
                    orgCateJournal.setOrgCateBillId(reviewForm.getReviewFormId());
                    orgCateJournal.setVendorId(reviewForm.getVendorId());
                    orgCateJournalMapper.insert(orgCateJournal);
                } else {
                    orgCateJournalMapper.updateById(orgCateJournal);
                }
            }
        }
    }

    private void checkDuplicationOrgCateJournal(List<OrgCateJournal> orgCateJournals, ReviewForm reviewForm) {
        if (!CollectionUtils.isEmpty(orgCateJournals)) {
            for (int i = 0; i < orgCateJournals.size(); i++) {
                for (int j = orgCateJournals.size() - 1; j > i; j--) {
                    String jOrgCode = orgCateJournals.get(j).getOrgCode();
                    jOrgCode = jOrgCode == null ? "" : jOrgCode;
                    String iOrgCode = orgCateJournals.get(i).getOrgCode();
                    iOrgCode = iOrgCode == null ? "" : iOrgCode;

                    String jParentOrgCode = orgCateJournals.get(j).getParentOrgCode();
                    jParentOrgCode = jParentOrgCode == null ? "" : jParentOrgCode;
                    String iParentOrgCode = orgCateJournals.get(i).getParentOrgCode();
                    iParentOrgCode = iParentOrgCode == null ? "" : iParentOrgCode;

                    String jCategoryCode = orgCateJournals.get(j).getCategoryCode();
                    jCategoryCode = jCategoryCode == null ? "" : jCategoryCode;
                    String iCategoryCode = orgCateJournals.get(i).getCategoryCode();
                    iCategoryCode = iCategoryCode == null ? "" : iCategoryCode;
                    if ("".equals(jOrgCode) && "".equals(jParentOrgCode) && !"".equals(jCategoryCode)) {
                        if (jCategoryCode.equals(iCategoryCode)) {
                            throw new BaseException("????????????????????????");
                        }
                    } else if (!"".equals(jOrgCode) && !"".equals(jParentOrgCode) && "".equals(jCategoryCode)) {
                        if (jOrgCode.equals(iOrgCode) && jParentOrgCode.equals(iParentOrgCode)) {
                            throw new BaseException("????????????????????????");
                        }
                    } else if (!"".equals(jOrgCode) && !"".equals(jParentOrgCode) && !"".equals(jCategoryCode)) {
                        if (jCategoryCode.equals(iCategoryCode) && jOrgCode.equals(iOrgCode) && jParentOrgCode.equals(iParentOrgCode)) {
                            throw new BaseException("?????????????????????????????????");
                        }
                    }
                }
            }
        }

    }

    //?????????????????????????????????????????????
    private EntryConfig checkAllBeforeSave(ReviewFormDTO reviewFormDTO, ReviewForm reviewForm) {
        //?????????????????????????????????
        String quaReviewType = reviewForm.getQuaReviewType();
        if (StringUtil.isEmpty(quaReviewType)) {
            throw new BaseException("????????????????????????");
        }
        //????????????????????????ID
        Long vendorId = reviewForm.getVendorId();
        if (vendorId == null) {
            throw new BaseException("???????????????");
        }
        //????????????????????????
        EntryConfig entryConfig = checkEntryConfig(reviewFormDTO, reviewForm);
        //????????????????????????:
        Assert.notEmpty(reviewFormDTO.getOrgJournals(), "????????????????????????");
        Assert.notEmpty(reviewFormDTO.getCateJournals(), "????????????????????????");
        //??????????????????????????????????????? ceea
//        String ifDevelop = reviewForm.getIfDevelop();
//        if (StringUtil.isEmpty(ifDevelop)) {
//            throw new BaseException("??????????????????????????????");
//        }
        return entryConfig;
    }

    private EntryConfig checkEntryConfig(ReviewFormDTO reviewFormDTO, ReviewForm reviewForm) {
        List<CateJournal> cateJournals = reviewFormDTO.getCateJournals();
        EntryConfig entryConfig = null;
        if (!QuaReviewType.ONETIME_VENDOR.name().equals(reviewForm.getQuaReviewType())) {
            for (CateJournal cateJournal : cateJournals) {
                //?????????????????????????????????ID??????????????????
                entryConfig = iEntryConfigService.getEntryConfigByTypeAndCategoryId(reviewForm.getQuaReviewType(), cateJournal.getCategoryId());
                Assert.notNull(entryConfig.getEntryConfigId(), LocaleHandler.getLocaleMsg("????????????????????????", cateJournal.getCategoryName()));
                for (CateJournal journal : cateJournals) {
                    //?????????????????????????????????ID??????????????????,????????????,???????????????????????????????????????
                    EntryConfig config = iEntryConfigService.getEntryConfigByTypeAndCategoryId(reviewForm.getQuaReviewType(), journal.getCategoryId());
                    Assert.notNull(config.getEntryConfigId(), LocaleHandler.getLocaleMsg("????????????????????????", journal.getCategoryName()));
                    if (entryConfig.getEntryConfigId().compareTo(config.getEntryConfigId()) != 0) {
                        throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????,?????????!"));
                    }
                }
            }
        }
        return entryConfig;
    }

    /**
     * ????????????????????????
     * @param entryConfig
     * @param reviewFormId
     */
    private void saveEntryConfigRecord(EntryConfig entryConfig , Long reviewFormId) {
    	//????????????????????????
    	QueryWrapper qw = new QueryWrapper();
    	qw.eq("REVIEW_FORM_ID", reviewFormId);
    	EntryConfigRecord record = this.iEntryConfigRecordService.getOne(qw);
    	if (null != record) {
    		record.setReviewFormId(reviewFormId);
    		record.setIfAuth(entryConfig.getIfAuth());
    		record.setIfAuthSample(entryConfig.getIfAuthSample());
    		record.setIfMaterial(entryConfig.getIfMaterial());
    		this.iEntryConfigRecordService.updateById(record);
    	} else {
    		record = new EntryConfigRecord();
        	BeanUtils.copyProperties(entryConfig, record);
        	record.setReviewFormId(reviewFormId);
        	record.setRecordId(IdGenrator.generate());
        	iEntryConfigRecordService.save(record);
    	}
    	
    	//????????????????????????????????????
    	qw = new QueryWrapper();
    	qw.eq("RECORD_ID", record.getRecordId());
    	iEntryRelationRecordService.remove(qw);
    	
    	qw = new QueryWrapper();
    	qw.eq("FORM_ID", reviewFormId);
    	qw.eq("FORM_TYPE", "REVIEW_FORM");
    	
    	List<EntryRelationRecord> list = new ArrayList<EntryRelationRecord>();
    	EntryRelationRecord relation = null;
    	//??????????????????
    	List<OrgJournal> orgs = iOrgJournalService.list(qw);
    	//??????????????????
    	List<CateJournal> cates = iCateJournalService.list(qw);
    	
    	if (null != orgs && null != cates) {
    		for (OrgJournal org : orgs) {
    			for (CateJournal cate:cates) {
    				relation = new EntryRelationRecord();
    				relation.setCategoryId(cate.getCategoryId());
    				relation.setCategoryName(cate.getCategoryName());
    				relation.setOrganizationId(org.getOrgId());
    				relation.setOrganizationName(org.getOrgName());
    				relation.setRecordId(record.getRecordId());
    				relation.setRelationId(IdGenrator.generate());
    				list.add(relation);
    			}
    		}
    	}
    	iEntryRelationRecordService.saveBatch(list);
    }

	@Override
	public void submitFlow(Long businessId, String param) throws Exception {
    	ReviewForm reviewForm =new ReviewForm();
    	reviewForm.setReviewFormId(businessId);
    	this.updateById(reviewForm.setApproveStatus(ApproveStatusType.SUBMITTED.getValue()));
	}
	
	@Override
	public void passFlow(Long businessId, String param) throws Exception {
    	ReviewForm reviewForm =new ReviewForm();
    	reviewForm.setReviewFormId(businessId);
    	this.pass(reviewForm);
	}

	@Override
	public void rejectFlow(Long businessId, String param) throws Exception {
    	ReviewForm reviewForm =new ReviewForm();
    	reviewForm.setReviewFormId(businessId);
    	this.updateById(reviewForm.setApproveStatus(ApproveStatusType.REJECTED.getValue()));
	}

	@Override
	public void withdrawFlow(Long businessId, String param) throws Exception {
    	ReviewForm reviewForm =new ReviewForm();
    	reviewForm.setReviewFormId(businessId);
    	this.updateById(reviewForm.setApproveStatus(ApproveStatusType.WITHDRAW.getValue()));
	}

	@Override
	public void destoryFlow(Long businessId, String param) throws Exception {
    	ReviewForm reviewForm =new ReviewForm();
    	reviewForm.setReviewFormId(businessId);
    	//???????????????
    	this.updateById(reviewForm.setApproveStatus(ApproveStatusType.WITHDRAW.getValue()));
	}

	@Override
	public String getVariableFlow(Long businessId, String param) throws Exception {
		ReviewForm reviewForm =this.getById(businessId);
		return JsonUtil.entityToJsonStr(reviewForm);
	}

	@Override
	public String getDataPushFlow(Long businessId, String param) throws Exception {
		ReviewForm reviewForm =this.getById(businessId);
		return JsonUtil.entityToJsonStr(reviewForm);
	}
}
