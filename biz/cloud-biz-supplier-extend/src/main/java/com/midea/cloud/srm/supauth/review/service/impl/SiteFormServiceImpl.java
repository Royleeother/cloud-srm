package com.midea.cloud.srm.supauth.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.ApproveStatusType;
import com.midea.cloud.common.enums.AssessmentType;
import com.midea.cloud.common.enums.OpType;
import com.midea.cloud.common.enums.OrgCateBillType;
import com.midea.cloud.common.enums.bpm.TempIdToModuleEnum;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.enums.review.FormType;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.JsonUtil;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.feign.workflow.FlowClient;
import com.midea.cloud.srm.model.common.FormResultDTO;
import com.midea.cloud.srm.model.supplierauth.entry.entity.EntryConfig;
import com.midea.cloud.srm.model.supplierauth.entry.entity.FileRecord;
import com.midea.cloud.srm.model.supplierauth.review.dto.LastSiteFormMessageDTO;
import com.midea.cloud.srm.model.supplierauth.review.dto.SiteFormDTO;
import com.midea.cloud.srm.model.supplierauth.review.entity.*;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import com.midea.cloud.srm.model.workflow.service.IFlowBusinessCallbackService;
import com.midea.cloud.srm.supauth.entry.service.IEntryConfigRecordService;
import com.midea.cloud.srm.supauth.entry.service.IEntryConfigService;
import com.midea.cloud.srm.supauth.entry.service.IFileRecordService;
import com.midea.cloud.srm.supauth.review.mapper.SiteFormMapper;
import com.midea.cloud.srm.supauth.review.service.*;
import com.midea.cloud.srm.supauth.workflow.controller.SupplierAuthFlow;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Objects;

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
 *  ????????????: 2020-03-15 17:06:13
 *  ????????????:
 * </pre>
 */
@Service
public class SiteFormServiceImpl extends ServiceImpl<SiteFormMapper, SiteForm> implements ISiteFormService, IFlowBusinessCallbackService {

    @Autowired
    private SiteFormMapper siteFormMapper;

    @Autowired
    private IOrgCateJournalService iOrgCateJournalService;

    @Autowired
    private ISiteFormRecordService iSiteFormRecordService;

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private RbacClient rbacClient;

    @Autowired
    private SupplierClient supplierClient;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private IOrgJournalService iOrgJournalService;

    @Autowired
    private ICateJournalService iCateJournalService;

    @Autowired
    private SupplierAuthFlow supplierAuthFlow;

    @Autowired
    private FlowClient flowClient;

    @Autowired
    private IEntryConfigService iEntryConfigService;
    
    @Autowired
    private IEntryConfigRecordService iEntryConfigRecordService;
    
    @Autowired
    private IFileRecordService iFileRecordService;

    @Override
    @Transactional
    public FormResultDTO saveOrUpdateSiteForm(SiteFormDTO siteFormDTO) {
        //????????????
        SiteForm siteForm = siteFormDTO.getSiteForm();
        if (siteForm == null) throw new BaseException(ResultCode.PARAM_VALID_ERROR);

        if (siteForm.getSiteFormId() != null) {
            SiteForm selectForm = siteFormMapper.selectById(siteForm.getSiteFormId());
            String approveStatus = selectForm.getApproveStatus();
            if (selectForm != null && (ApproveStatusType.SUBMITTED.getValue().equals(approveStatus)
                    || ApproveStatusType.APPROVED.getValue().equals(approveStatus) || ApproveStatusType.ABANDONED.getValue().equals(approveStatus))) {
                throw new BaseException("???????????????????????????????????????????????????????????????");
            }
        }

        //??????opType
        String opType = siteFormDTO.getOpType();
        if (StringUtils.isBlank(opType)) {
            throw new BaseException(ResultCode.PARAM_VALID_ERROR);
        } else {
            try {
                OpType.valueOf(opType);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                throw new BaseException(ResultCode.TYPE_MISMATCH);
            }
        }

        //??????????????????
        FormResultDTO formResultDTO = null;
        if (OpType.TEMPORARY_STORAGE.toString().equals(opType)) {
            //??????
            formResultDTO = saveOrUpdateSiteFormDTO(siteFormDTO, ApproveStatusType.DRAFT.getValue());
            checkAllBeforeTempSave(siteForm);
        } else if (OpType.SUBMISSION.toString().equals(opType)) {
            //????????????
            checkAllBeforeSave(siteFormDTO, siteForm);
            //??????
            formResultDTO = saveOrUpdateSiteFormDTO(siteFormDTO, ApproveStatusType.SUBMITTED.getValue());
        }
        return formResultDTO;
    }

    @Override
    @AuthData(module = MenuEnum.SITE_ASSESSMENT)
    public PageInfo<SiteForm> listPageByParm(SiteForm siteForm) {
        String siteFormNumber = siteForm.getSiteFormNumber();
        String reviewFormNumber = siteForm.getReviewFormNumber();
        String approveStatus = siteForm.getApproveStatus();
        String assessmentType = siteForm.getAssessmentType();
        String reviewResult = siteForm.getReviewResult();
        String vendorName = siteForm.getVendorName();

        PageUtil.startPage(siteForm.getPageNum(), siteForm.getPageSize());
        SiteForm formWrapper = new SiteForm();
        if (!StringUtils.isBlank(approveStatus)) {
            formWrapper.setApproveStatus(approveStatus);
        }
        if (!StringUtils.isBlank(assessmentType)) {
            formWrapper.setAssessmentType(assessmentType);
        }
        if (!StringUtils.isBlank(reviewResult)) {
            formWrapper.setReviewResult(reviewResult);
        }
        QueryWrapper<SiteForm> wrapper = new QueryWrapper<SiteForm>(formWrapper);
        wrapper.like(!StringUtils.isBlank(siteFormNumber), "SITE_FORM_NUMBER", siteFormNumber)
                .like(!StringUtils.isBlank(reviewFormNumber), "REVIEW_FORM_NUMBER", reviewFormNumber)
                .like(!StringUtils.isBlank(vendorName), "VENDOR_NAME", vendorName)
                .orderByDesc("LAST_UPDATE_DATE");
        return new PageInfo<>(siteFormMapper.selectList(wrapper));
    }

    @Override
    public SiteFormDTO getSiteFormDTO(Long siteFormId) {
        Assert.notNull(siteFormId, "siteFormId????????????");
        //??????DTO??????
        SiteFormDTO siteFormDTO = new SiteFormDTO();
        //??????????????????????????????
        SiteForm siteForm = siteFormMapper.selectById(siteFormId);

        //siteForm.setCreatedBy(xxx.getUsername(siteForm.getCreatedId()));
        //siteForm.setDepat(xxx.getDeptname(siteForm.getCreatedId()));

        siteFormDTO.setSiteForm(siteForm);

        //????????????????????????????????????
        QueryWrapper<SiteFormRecord> siteFormRecordQueryWrapperWrapper = new QueryWrapper<>(
                new SiteFormRecord().setSiteFormId(siteFormId));
//        List<SiteFormRecord> siteFormRecords = iSiteFormRecordService.list(siteFormRecordQueryWrapperWrapper);
//        siteFormDTO.setSiteFormRecords(siteFormRecords);
        
        siteFormDTO.setFileRecords(iFileRecordService.getFileRecord(siteForm.getSiteFormId(), FormType.AUTH_FORM.name()));

        //????????????ou????????????
        List<CateJournal> cateJournals = iCateJournalService.list(new QueryWrapper<>(new CateJournal().setFormId(siteFormId).setFormType(FormType.AUTH_FORM.name())));
        siteFormDTO.setCateJournals(cateJournals);

        //??????????????????????????????
        List<OrgJournal> orgJournals = iOrgJournalService.list(new QueryWrapper<>(new OrgJournal().setFormId(siteFormId).setFormType(FormType.AUTH_FORM.name())));
        siteFormDTO.setOrgJournals(orgJournals);

//        //?????????????????????????????????????????????  ceea,???????????????
//        QueryWrapper<OrgCateJournal> orgCateJournalqueryWrapper = new QueryWrapper<>(new OrgCateJournal()
//                .setOrgCateBillId(siteFormId).setOrgCateBillType(OrgCateBillType.SITE_FORM.getValue()));
//        List<OrgCateJournal> orgCateJournals = iOrgCateJournalService.list(orgCateJournalqueryWrapper);
//        siteFormDTO.setOrgCateJournals(orgCateJournals);

        return siteFormDTO;
    }

    @Override
    public LastSiteFormMessageDTO getLastSiteFormMessage(Long vendorId) {
        Assert.notNull(vendorId, "???????????????");
        LastSiteFormMessageDTO lastSiteFormMessageDTO = new LastSiteFormMessageDTO();
        QueryWrapper<SiteForm> queryWrapper = new QueryWrapper<>(new SiteForm().setVendorId(vendorId)
                .setApproveStatus(ApproveStatusType.APPROVED.getValue()));
        queryWrapper.orderByDesc("LAST_UPDATE_DATE");
        List<SiteForm> siteForms = siteFormMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(siteForms)) {
            SiteForm siteForm = siteForms.get(0);
            lastSiteFormMessageDTO.setLastSiteDate(siteForm.getSiteDate());
            lastSiteFormMessageDTO.setLastSiteMember(siteForm.getSiteMember());
        }
        return lastSiteFormMessageDTO;
    }

    @Override
    @Transactional
    public void deleteSiteFormById(Long siteFormId) {
        SiteForm siteForm = siteFormMapper.selectById(siteFormId);
        if (siteForm != null) {
            if (siteForm.getApproveStatus().equals(ApproveStatusType.DRAFT.getValue())
                    || ApproveStatusType.REJECTED.getValue().equals(siteForm.getApproveStatus())) {
                siteFormMapper.deleteById(siteFormId);
                iOrgJournalService.remove(new QueryWrapper<>(new OrgJournal().setFormType(FormType.AUTH_FORM.name()).setFormId(siteFormId)));
                iCateJournalService.remove(new QueryWrapper<>(new CateJournal().setFormType(FormType.AUTH_FORM.name()).setFormId(siteFormId)));
                deleteSiteFormRecord(siteFormId);
//                iOrgCateJournalService.remove(new QueryWrapper<>(new OrgCateJournal().setOrgCateBillType(OrgCateBillType.SITE_FORM.getValue())
//                        .setOrgCateBillId(siteFormId)));
//                fileCenterClient.deleteByParam(new Fileupload().setBusinessId(siteFormId));
            } else {
                throw new BaseException("????????????????????????????????????,????????????");
            }
        }
    }

    /**
     * ????????????
     *
     * @param siteFormId
     */
    @Transactional
    @Override
    public void abandon(Long siteFormId) {
        SiteFormDTO siteFormDTO = this.getSiteFormDTO(siteFormId);
        SiteForm siteForm = siteFormDTO.getSiteForm();
        Assert.notNull(ObjectUtils.isEmpty(siteForm),"????????????????????????");
        String approveStatus = siteForm.getApproveStatus();
        Assert.isTrue(ApproveStatusType.WITHDRAW.getValue().equals(approveStatus)||ApproveStatusType.REJECTED.getValue().equals(approveStatus),"??????????????????????????????????????????????????????");
        siteForm.setApproveStatus(ApproveStatusType.ABANDONED.toString());
        this.updateById(siteForm);
        SrmFlowBusWorkflow srmworkflowForm = baseClient.getSrmFlowBusWorkflow(siteFormId);
        if (srmworkflowForm!=null) {
            try {
                siteFormDTO.setProcessType("N");
                supplierAuthFlow.submitSupplierAuthFlow(siteFormDTO);
            }catch (Exception e){
                Assert.isTrue(false, "??????????????????????????????");
            }
        }
    }


    private void deleteSiteFormRecord(Long siteFormId) {
        List<SiteFormRecord> siteFormRecords = iSiteFormRecordService.list(new QueryWrapper<>(new SiteFormRecord().setSiteFormId(siteFormId)));
        if (!CollectionUtils.isEmpty(siteFormRecords)) {
            for (SiteFormRecord siteFormRecord : siteFormRecords) {
                if (siteFormRecord == null) continue;
                iSiteFormRecordService.deleteBySiteFormRecordId(siteFormRecord.getSiteFormRecordId());
            }
        }
    }

    @Override
    public SiteForm getSiteFormByReviewFormId(Long reviewFormId) {
        Assert.notNull(reviewFormId, "???????????????ID??????");
        QueryWrapper<SiteForm> queryWrapper = new QueryWrapper<>(new SiteForm().setReviewFormId(reviewFormId)
                .setApproveStatus(ApproveStatusType.APPROVED.getValue()));
        return siteFormMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional
    public void pass(SiteForm siteForm) {
        SiteForm byId = this.getById(siteForm.getSiteFormId());
        log.error("siteform-pass:"+byId);
        if (byId != null) {
            byId.setApproveStatus(ApproveStatusType.APPROVED.getValue());
            this.updateById(new SiteForm().setSiteFormId(siteForm.getSiteFormId()).setApproveStatus(ApproveStatusType.APPROVED.getValue()));
            /**
             * kuangzm
             * 1.???????????????????????????????????????????????????????????????
             * 2.??????????????????????????????????????????????????????
             */
            updateVendorMainData(byId);
        }
    }

    private void updateVendorMainData(SiteForm byId) {
        List<OrgJournal> orgJournals = iOrgJournalService.list(new QueryWrapper<>(new OrgJournal().setFormType(FormType.AUTH_FORM.name()).setFormId(byId.getSiteFormId()).setVendorId(byId.getVendorId())));
        List<CateJournal> cateJournals = iCateJournalService.list(new QueryWrapper<>(new CateJournal().setFormId(byId.getSiteFormId()).setFormType(FormType.AUTH_FORM.name()).setVendorId(byId.getVendorId())));
        iOrgCateJournalService.generateOrgCategorys(orgJournals, cateJournals, byId.getReviewFormId(), byId.getSiteFormId(), OrgCateBillType.SITE_FORM.getValue());
    }


    private void checkAllBeforeTempSave(SiteForm siteForm) {
        if (StringUtils.isBlank(siteForm.getAssessmentType())) {
            throw new BaseException("???????????????????????????");
        } else if (!StringUtils.isBlank(siteForm.getAssessmentType())) {
            if (AssessmentType.ACCESS_ASSESSMENT.getValue().equals(siteForm.getAssessmentType())) {
                if (siteForm.getReviewFormId() == null || siteForm.getReviewFormNumber() == null) {
                    throw new BaseException("??????????????????????????????????????????????????????");
                }
            }
        }
    }

    /**
     * ????????????????????????
     *
     * @param siteFormDTO
     * @param siteForm
     */
    private void checkAllBeforeSave(SiteFormDTO siteFormDTO, SiteForm siteForm) {
        /**
         * ????????????:
         * 1???????????????????????????
         * 2????????????ID????????????
         * 3???????????????-???????????????????????????
         */
        // ??????????????????????????????????????????????????????
        // ??????????????????????????????????????????
        if (StringUtils.isBlank(siteForm.getAssessmentType())) {
            throw new BaseException("???????????????????????????");
        } else if (!StringUtils.isBlank(siteForm.getAssessmentType())) {
            if (AssessmentType.ACCESS_ASSESSMENT.getValue().equals(siteForm.getAssessmentType())) {
                if (siteForm.getReviewFormId() == null || siteForm.getReviewFormNumber() == null) {
                    throw new BaseException("??????????????????????????????????????????????????????");
                }
            }
        } else if (siteForm.getVendorId() == null) {
            throw new BaseException("???????????????");
        } else if (siteForm.getSiteAdress() == null) {
            throw new BaseException("??????????????????");
//        } else if (siteForm.getSiteDate() == null) {
//            throw new BaseException("??????????????????");
//        } else if (siteForm.getSiteMember() == null) {
//            throw new BaseException("????????????????????????");
//        } else if (siteForm.getVendorAssessor() == null) {
//            throw new BaseException("????????????????????????");
        }

        //????????????????????????
        List<FileRecord> siteFormRecords = siteFormDTO.getFileRecords();
        List<CateJournal> cateJournals = siteFormDTO.getCateJournals();
        Assert.notEmpty(cateJournals, "??????????????????");
        CateJournal cateJournal = cateJournals.get(0);
        //??????????????????
        EntryConfig entryConfig = iEntryConfigService.getEntryConfigByTypeAndCategoryId(siteForm.getQuaReviewType(), cateJournal.getCategoryId());
        String ifAuthOnSite = entryConfig.getIfAuthOnSite();
        String ifAuthSample = entryConfig.getIfAuthSample();
        String ifQpaQsa = entryConfig.getIfQpaQsa();
        if (CollectionUtils.isEmpty(siteFormRecords)) {
        } else {
//            //????????????????????????????????????
//            if (YesOrNo.YES.getValue().equals(ifAuthOnSite)) {
//                List<String> collect = siteFormRecords.stream().map(SiteFormRecord::getCeeaReviewLink).collect(Collectors.toList());
//                if (!collect.contains(CeeaReviewLink.SITE_REVIEW.name())) {
//                    throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????,????????????????????????"));
//                }
//            }
//            //????????????????????????????????????
//            if (YesOrNo.YES.getValue().equals(ifAuthSample)) {
//                List<String> collect = siteFormRecords.stream().map(SiteFormRecord::getCeeaReviewLink).collect(Collectors.toList());
//                if (!collect.contains(CeeaReviewLink.SAMPLE_VERIFY.name())) {
//                    throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????,????????????????????????"));
//                }
//            }
//            //??????????????????QPA/QSA??????
//            if (YesOrNo.YES.getValue().equals(ifQpaQsa)) {
//                List<String> collect = siteFormRecords.stream().map(SiteFormRecord::getCeeaReviewLink).collect(Collectors.toList());
//                if (!collect.contains(CeeaReviewLink.QPA_QSA.name())) {
//                    throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????,??????QPA/QSA??????"));
//                }
//            }
            for (FileRecord siteFormRecord : siteFormRecords) {

                // ???????????????????????????????????????
//                if (StringUtils.isBlank(siteFormRecord.getReviewField())) {
//                    throw new BaseException("??????????????????");
//                }
//                if (StringUtils.isBlank(siteFormRecord.getReviewPerson())) {
//                    throw new BaseException("???????????????");
//                }
//                if (CeeaReviewLink.SITE_REVIEW.name().equals(siteFormRecord.getCeeaReviewLink()) && YesOrNo.YES.getValue().equals(ifAuthOnSite)) {
//                    Assert.notNull(siteFormRecord.getFileuploadId(), LocaleHandler.getLocaleMsg("?????????????????????,???????????????"));
//                }
//                if (CeeaReviewLink.QPA_QSA.name().equals(siteFormRecord.getCeeaReviewLink()) && YesOrNo.YES.getValue().equals(ifQpaQsa)) {
//                    Assert.notNull(siteFormRecord.getFileuploadId(), LocaleHandler.getLocaleMsg("QPA/QSA?????????,???????????????"));
//                }
//                if (CeeaReviewLink.SAMPLE_VERIFY.name().equals(siteFormRecord.getCeeaReviewLink()) && YesOrNo.YES.getValue().equals(ifAuthSample)) {
//                    Assert.notNull(siteFormRecord.getFileuploadId(), LocaleHandler.getLocaleMsg("?????????????????????,???????????????"));
//                }
//                if (siteFormRecord.getScore() == null) {
//                    throw new BaseException("??????????????????????????????");
//               }
                //???????????????????????????????????????
                if (Objects.isNull(siteFormRecord.getScore())) {
                    siteFormRecord.setScore(0D);
                }
                if (siteFormRecord.getScore() < 0.00 || siteFormRecord.getScore() > 100.00) {
                    throw new BaseException("?????????????????????????????????0~100");
                }
            }
            // ??????????????????
            if (StringUtils.isBlank(siteForm.getReviewResult())) {
                throw new BaseException("?????????????????????");
            }
        }

        // ???????????????????????????  longi?????????
//        List<OrgCateJournal> orgCateJournals = siteFormDTO.getOrgCateJournals();
//        if (CollectionUtils.isEmpty(orgCateJournals)) {
//            throw new BaseException("???????????????????????????");
//        } else {
//            for (OrgCateJournal orgCateJournal : orgCateJournals) {
//                if (orgCateJournal == null) continue;
//                if (orgCateJournal.getOrgCode() == null) {
//                    throw new BaseException("??????????????????");
//                }
//                if (orgCateJournal.getCategoryCode() == null) {
//                    throw new BaseException("??????????????????");
//                }
//            }
//        }
    }

    @Transactional
    public FormResultDTO saveOrUpdateSiteFormDTO(SiteFormDTO siteFormDTO, String approveStatusType) {
        SiteForm siteForm = siteFormDTO.getSiteForm();
        //???????????????????????????????????????
        if (siteForm == null) throw new BaseException("?????????????????????");
        if (siteForm.getSiteFormId() == null) {
            long id = IdGenrator.generate();
            siteForm.setSiteFormNumber(baseClient.seqGen(SequenceCodeConstant.SEQ_SITE_REVIEW_FORM_NUM_CODE))
                    .setApproveStatus(approveStatusType).setSiteFormId(id);
            siteFormMapper.insert(siteForm);
        } else {
            siteForm.setApproveStatus(approveStatusType);
            siteFormMapper.updateById(siteForm);
        }

        //???????????????????????????????????????
//        batchSaveOrUpdateSiteFormRecord(siteFormDTO, siteForm);
        iFileRecordService.batchSaveOrUpdate(siteFormDTO.getFileRecords(), siteForm.getSiteFormId(), FormType.AUTH_FORM.name());
        
        //??????????????????ou????????????
        iOrgJournalService.batchSaveOrUpdateOrgJournal(siteFormDTO.getOrgJournals(), siteForm.getVendorId(), siteForm.getSiteFormId(), FormType.AUTH_FORM.name());
        //????????????????????????????????????
        iCateJournalService.batchSaveOrUpdateCateJournal(siteFormDTO.getCateJournals(),siteForm.getVendorId(), siteForm.getSiteFormId(), FormType.AUTH_FORM.name());

        //????????????
        if (!CollectionUtils.isEmpty(siteForm.getFileUploads())) {
            fileCenterClient.bindingFileupload(siteForm.getFileUploads(), siteForm.getSiteFormId());
        }

        //ceea,???????????????
        //???????????????????????????.
        FormResultDTO formResultDTO = new FormResultDTO();
        formResultDTO.setFormId(siteForm.getSiteFormId());
//        UpdateWrapper<SiteForm> updateWrapper = new UpdateWrapper<>(
//                new SiteForm().setSiteFormId(siteForm.getSiteFormId()));
//        if (!ApproveStatusType.DRAFT.getValue().equals(siteForm.getApproveStatus())) {
//            Long menuId = siteFormDTO.getMenuId();
//            Permission menu = null;
//            if (menuId != null) {
//                menu = rbacClient.getMenu(menuId);
//            }
//            if (menu != null) {
//                //1.??????:???????????????,
//                formResultDTO.setEnableWorkFlow(menu.getEnableWorkFlow());
//                formResultDTO.setFunctionId(menu.getFunctionId());
//                if (YesOrNo.YES.getValue().equals(menu.getEnableWorkFlow())) {
//                    updateApproveStatus(updateWrapper, ApproveStatusType.DRAFT.getValue());
//                    return formResultDTO;
//                }
//            }
//            //2.??????:??????????????????????????????
//            createVendorMainData(siteFormDTO, siteForm);
//            //????????????????????????
//            updateApproveStatus(updateWrapper, ApproveStatusType.APPROVED.getValue());
//        }
        /* Begin by chenwt24@meicloud.com   2020-09-26 */
        //TODO ??????????????????
        if (StringUtils.equals(approveStatusType, ApproveStatusType.SUBMITTED.getValue())) {
//            String formId = null;
//            try {
//                formId = supplierAuthFlow.submitSupplierAuthFlow(siteFormDTO);
//            } catch (Exception e) {
//                throw new BaseException(e.getMessage());
//            }
//            if (StringUtils.isEmpty(formId)) {
//                throw new BaseException(LocaleHandler.getLocaleMsg("??????OA????????????"));
//
//            }

        	TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                	flowClient.submitFlow(siteForm.getSiteFormId(), TempIdToModuleEnum.SUPPLIERAUTH.getValue(), "throughWorkflowService");
                }
            });
        }

        /* End by chenwt24@meicloud.com     2020-09-29 */
        return formResultDTO;
    }

    private void batchSaveOrUpdateCateJournal(SiteFormDTO siteFormDTO, SiteForm siteForm) {
        List<CateJournal> cateJournals = siteFormDTO.getCateJournals();
        if (!CollectionUtils.isEmpty(cateJournals)) {
            for (CateJournal cateJournal : cateJournals) {
                if (cateJournal == null) continue;
                if (cateJournal.getCategoryJournalId() == null) {
                    cateJournal.setCategoryJournalId(IdGenrator.generate())
                            .setFormId(siteForm.getSiteFormId())
                            .setFormType(FormType.AUTH_FORM.name())
                            .setVendorId(siteForm.getVendorId());
                    iCateJournalService.save(cateJournal);
                } else {
                    iCateJournalService.updateById(cateJournal);
                }
            }
        }
    }

    private void batchSaveOrUpdateOrgJournal(SiteFormDTO siteFormDTO, SiteForm siteForm) {
        List<OrgJournal> orgJournals = siteFormDTO.getOrgJournals();
        if (!CollectionUtils.isEmpty(orgJournals)) {
            for (OrgJournal orgJournal : orgJournals) {
                if (orgJournal == null) continue;
                if (orgJournal.getOrgJournalId() == null) {
                    orgJournal.setOrgJournalId(IdGenrator.generate())
                            .setFormId(siteForm.getSiteFormId())
                            .setFormType(FormType.AUTH_FORM.name())
                            .setVendorId(siteForm.getVendorId());
                    iOrgJournalService.save(orgJournal);
                } else {
                    iOrgJournalService.updateById(orgJournal);
                }
            }
        }
    }

    private void updateApproveStatus(UpdateWrapper<SiteForm> updateWrapper, String approveStatusType) {
        updateWrapper.set("APPROVE_STATUS", approveStatusType);
        this.update(updateWrapper);
    }

    @Override
    public void submitFlow(Long businessId, String param) throws Exception {
        SiteForm siteForm =new SiteForm();
        siteForm.setSiteFormId(businessId);
        this.updateById(siteForm.setApproveStatus(ApproveStatusType.SUBMITTED.getValue()));
    }

    @Override
    public void passFlow(Long businessId, String param) throws Exception {
        SiteForm siteForm =new SiteForm();
        siteForm.setSiteFormId(businessId);
        this.pass(siteForm);
    }

    @Override
    public void rejectFlow(Long businessId, String param) throws Exception {
        SiteForm siteForm =new SiteForm();
        siteForm.setSiteFormId(businessId);
        this.updateById(siteForm.setApproveStatus(ApproveStatusType.REJECTED.getValue()));
    }

    @Override
    public void withdrawFlow(Long businessId, String param) throws Exception {
        SiteForm siteForm =new SiteForm();
        siteForm.setSiteFormId(businessId);
        this.updateById(siteForm.setApproveStatus(ApproveStatusType.WITHDRAW.getValue()));
    }

    @Override
    public void destoryFlow(Long businessId, String param) throws Exception {
        SiteForm siteForm =new SiteForm();
        siteForm.setSiteFormId(businessId);
        //???????????????
        this.updateById(siteForm.setApproveStatus(ApproveStatusType.WITHDRAW.getValue()));
    }

    @Override
    public String getVariableFlow(Long businessId, String param) throws Exception {
        SiteForm siteForm =this.getById(businessId);
        return JsonUtil.entityToJsonStr(siteForm);
    }

    @Override
    public String getDataPushFlow(Long businessId, String param) throws Exception {
        SiteForm siteForm =this.getById(businessId);
        return JsonUtil.entityToJsonStr(siteForm);
    }

    //ceea,???????????????
//    private void createVendorMainData(SiteFormDTO siteFormDTO, SiteForm siteForm) {
//        List<OrgCateJournal> orgCateJournals = siteFormDTO.getOrgCateJournals();
//        if (!CollectionUtils.isEmpty(orgCateJournals)) {
//            for (OrgCateJournal orgCateJournal : orgCateJournals) {
//                if (orgCateJournal == null) continue;
//                Long categoryId = orgCateJournal.getCategoryId();
//                Long orgId = orgCateJournal.getOrgId();
//                Long vendorId = siteForm.getVendorId();
//                OrgCategory repeatOrgCategory = supplierClient.getByCategoryIdAndOrgIdAndCompanyId(categoryId, orgId, vendorId);
//                OrgInfo repeatOrgInfo = supplierClient.getOrgInfoByOrgIdAndCompanyId(orgId, vendorId);
//                OrgInfo orgInfo = new OrgInfo();
//                if (repeatOrgCategory != null) continue;
//                if (categoryId != null) {
//                    OrgCategory orgCategory = new OrgCategory();
//                    BeanUtils.copyProperties(orgCateJournal, orgCategory);
//                    orgCategory.setCompanyId(vendorId).setServiceStatus(CategoryStatus.APPLICATION.toString());
//                    supplierClient.addOrgCategory(orgCategory);
//                    if (orgId != null) {
//                        if (repeatOrgInfo != null) continue;
//                        BeanUtils.copyProperties(orgCateJournal, orgInfo);
//                        orgInfo.setCompanyId(vendorId).setServiceStatus(com.midea.cloud.common.enums.review.OrgStatus.INTRODUCTION.toString());
//                        supplierClient.addOrgInfo(orgInfo);
//                    }
//                } else {
//                    if (repeatOrgInfo != null) continue;
//                    BeanUtils.copyProperties(orgCateJournal, orgInfo);
//                    orgInfo.setCompanyId(vendorId).setServiceStatus(com.midea.cloud.common.enums.review.OrgStatus.INTRODUCTION.toString());
//                    supplierClient.addOrgInfo(orgInfo);
//                }
//            }
//        }
//    }

    //ceea,???????????????
//    private void batchSaveOrUpdateOrgCateJournal(SiteFormDTO siteFormDTO, SiteForm siteForm) {
//        List<OrgCateJournal> orgCateJournals = siteFormDTO.getOrgCateJournals();
//        if (!CollectionUtils.isEmpty(orgCateJournals)) {
//            for (OrgCateJournal orgCateJournal : orgCateJournals) {
//                if (orgCateJournal == null) continue;
//                if (orgCateJournal.getOrgCateJournalId() == null) {
//                    long id = IdGenrator.generate();
//                    orgCateJournal.setOrgCateJournalId(id).setVendorId(siteForm.getVendorId())
//                                  .setOrgCateBillId(siteForm.getSiteFormId()).setOrgCateBillType(OrgCateBillType.SITE_FORM.getValue());
//                    iOrgCateJournalService.save(orgCateJournal);
//                } else {
//                    iOrgCateJournalService.updateById(orgCateJournal);
//                }
//            }
//        }
//    }

    
}
