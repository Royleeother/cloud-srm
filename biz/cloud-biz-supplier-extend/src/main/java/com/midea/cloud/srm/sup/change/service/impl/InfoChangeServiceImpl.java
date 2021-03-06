package com.midea.cloud.srm.sup.change.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.DimConstant;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.*;
import com.midea.cloud.common.enums.flow.CbpmFormTemplateIdEnum;
import com.midea.cloud.common.enums.sup.InfoChangeStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.flow.WorkFlowFeign;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.common.FormResultDTO;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.flow.process.dto.CbpmRquestParamDTO;
import com.midea.cloud.srm.model.rbac.permission.entity.Permission;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.change.dto.ChangeFlowResultDTO;
import com.midea.cloud.srm.model.supplier.change.dto.ChangeInfoDTO;
import com.midea.cloud.srm.model.supplier.change.dto.ChangeRequestDTO;
import com.midea.cloud.srm.model.supplier.change.dto.InfoChangeDTO;
import com.midea.cloud.srm.model.supplier.change.entity.*;
import com.midea.cloud.srm.model.supplier.dim.entity.Dim;
import com.midea.cloud.srm.model.supplier.info.entity.*;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import com.midea.cloud.srm.sup.change.mapper.InfoChangeMapper;
import com.midea.cloud.srm.sup.change.service.*;
import com.midea.cloud.srm.sup.change.workflow.VendorInfoChangeFlow;
import com.midea.cloud.srm.sup.dim.service.IDimService;
import com.midea.cloud.srm.sup.info.service.*;
import com.midea.cloud.srm.sup.statuslog.service.ICompanyStatusLogService;
import feign.FeignException;
import net.sf.cglib.core.Local;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.util.Utils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

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
 *  ????????????: 2020-03-30 19:57:36
 *  ????????????:
 * </pre>
 */
@Service
public class InfoChangeServiceImpl extends ServiceImpl<InfoChangeMapper, InfoChange> implements IInfoChangeService {

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private InfoChangeMapper infoChangeMapper;

    @Autowired
    private IDimService iDimService;

    @Autowired
    private IDimFieldContextChangeService iDimFieldContextChangeService;

    @Autowired
    private IContactInfoChangeService iContactInfoChangeService;

    @Autowired
    private IOtherInfoChangeService iOtherInfoChangeService;

    @Autowired
    private IBankInfoChangeService iBankInfoChangeService;

    @Autowired
    private ISiteInfoChangeService iSiteInfoChangeService;

    @Autowired
    private IBusinessInfoChangeService iBusinessInfoChangeService;

    @Autowired
    private IHonorInfoChangeService iHonorInfoChangeService;

    @Autowired
    private IHolderInfoChangeService iHolderInfoChangeService;

    @Autowired
    private IFinanceInfoChangeService iFinanceInfoChangeService;

    @Autowired
    private IOrgCategoryChangeService iOrgCategoryChangeService;

    @Autowired
    private IOrgInfoChangeService iOrgInfoChangeService;

    @Autowired
    private IOperationInfoChangeService iOperationInfoChangeService;

    @Autowired
    private ICompanyInfoChangeService iCompanyInfoChangeService;

    @Autowired
    private ICompanyInfoService iCompanyInfoService;

    @Autowired
    private ManagementAttachChangeService managementAttachChangeService;

    @Autowired
    private IFileuploadChangeService iFileuploadChangeService;

    @Autowired
    private ICompanyStatusLogService iCompanyStatusLogService;

    @Autowired
    private RbacClient rbacClient;

    @Autowired
    private WorkFlowFeign workFlowFeign;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IManagementAttachService iManagementAttachService;

    @Autowired
    private IManagementInfoService iManagementInfoService;

    /**
     * ?????????????????????Service
     */
    @Resource
    private IBankInfoService iBankInfoService;

    /**
     * ????????????????????????Service
     */
    @Resource
    private IContactInfoService iContactInfoService;

    /**
     * ?????????????????????Service
     */
    @Resource
    private ISiteInfoService iSiteInfoService;

    private final static String defaultUsername = "admin";

    @Autowired
    private VendorInfoChangeFlow vendorInfoChangeFlow;

    public final ThreadPoolExecutor submitExector;

    public InfoChangeServiceImpl() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        submitExector = new ThreadPoolExecutor(cpuCount * 2, cpuCount * 2,
                0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),
                new NamedThreadFactory("ERP-message-sender", true),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public InfoChange addInfoChange(InfoChange infoChange) {
        Long id = IdGenrator.generate();
        infoChange.setChangeId(id);
        infoChange.setChangeApplyNo(baseClient.seqGen(SequenceCodeConstant.SEQ_SUP_COMPANY_CHANGE_NUM));
        this.save(infoChange);
        return infoChange;
    }

    @Override
    public InfoChange updateInfoChange(InfoChange infoChange) {
        Assert.notNull(infoChange.getChangeId(), LocaleHandler.getLocaleMsg("id????????????"));
        this.updateById(infoChange);
        return infoChange;
    }

    @Override
    public List<InfoChangeDTO> listPageByParam(ChangeRequestDTO changeRequestDTO) {
        PageUtil.startPage(changeRequestDTO.getPageNum(), changeRequestDTO.getPageSize());
        return infoChangeMapper.listPageByParam(changeRequestDTO);
    }

    @Override
    public PageInfo<InfoChangeDTO> listPageByParamPage(ChangeRequestDTO changeRequestDTO) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (loginAppUser != null && UserType.VENDOR.name().equals(loginAppUser.getUserType())) {
            if (loginAppUser.getCompanyId() != null) {
                changeRequestDTO.setCompanyId(loginAppUser.getCompanyId());
            } else {
                return new PageInfo<>(new ArrayList<>());
            }
        }
        PageUtil.startPage(changeRequestDTO.getPageNum(), changeRequestDTO.getPageSize());
        return new PageInfo<InfoChangeDTO>(this.listPageByParam(changeRequestDTO));
    }

    private ChangeFlowResultDTO checkChangeFlow(String orderType, String userType) {
        Assert.notNull(orderType, LocaleHandler.getLocaleMsg("???????????????"));
        Assert.notNull(userType, LocaleHandler.getLocaleMsg("???????????????"));
        ChangeFlowResultDTO changeFlowResultDTO = new ChangeFlowResultDTO();
        Dim companyDim = iDimService.queryByParam(orderType);
        //???????????????????????????????????????
        if (companyDim != null) {
            if (StringUtils.isNotEmpty(userType) && UserType.VENDOR.name().equals(userType)) {
                changeFlowResultDTO.setIsAllowChange(YesOrNo.YES.getValue().equals(companyDim.getIsSupply()));
            } else {
                changeFlowResultDTO.setIsAllowChange(YesOrNo.YES.getValue().equals(companyDim.getIsBuyer()));
            }
            changeFlowResultDTO.setIsFlow(YesOrNo.YES.getValue().equals(companyDim.getIsFlow()));
        } else {
            changeFlowResultDTO.setIsAllowChange(false);
            changeFlowResultDTO.setIsFlow(false);
        }
        return changeFlowResultDTO;
    }

    /**
     * ????????????????????? ??????
     * ???????????????????????????????????????Erp
     *
     * @param changeInfo
     * @param orderStatus
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormResultDTO saveOrUpdateChange(ChangeInfoDTO changeInfo, String orderStatus) {
        FormResultDTO formResultDTO = new FormResultDTO();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        InfoChange infoInfoChange = changeInfo.getInfoChange();
        String userType = "";
        if (loginAppUser != null) {
            userType = loginAppUser.getUserType();
            //Assert.notNull(loginAppUser, "????????????????????????");
            // ???????????????????????????????????????????????????companyId????????????companyId, ?????????????????????????????????
            if (userType.equals(UserType.VENDOR.name())) {
                infoInfoChange.setCompanyId(loginAppUser.getCompanyId());
            }
        }

        infoInfoChange.setChangeStatus(orderStatus)
                .setChangeApplyDate(new Date());
        InfoChange infoChange = this.saveOrUpdateInfoChange(infoInfoChange);

        Long changeId = infoChange.getChangeId();

        // ???????????????????????????
        Long companyId = infoInfoChange.getCompanyId();

        // ???????????????????????? ??????????????????????????????????????????
        if (orderStatus.equals(InfoChangeStatus.SUBMITTED.getValue())) {

            Assert.notNull(companyId, "??????????????????????????????Id?????????");
            String vendorCode = iCompanyInfoService.getByCompanyId(companyId).getCompanyCode();
            Assert.notNull(vendorCode, "?????????????????????????????????????????????");

            // ??????????????????????????? ????????????????????????????????????????????????
            orgSiteDuplicateCheck(changeInfo.getSiteInfoChanges());

            // ??????????????????????????? ???????????????????????????????????????????????????
            bankInfoCheck(changeInfo.getBankInfoChanges());

            // ??????????????????????????????, ???????????????????????????
            contactInfoCheck(changeInfo.getContactInfoChanges());

            //????????????????????????????????????????????????????????????????????????
            //Assert.isTrue(CollectionUtils.isNotEmpty(changeInfo.getContactInfoChanges()), "???????????????????????????????????????");
            //Assert.isTrue(CollectionUtils.isNotEmpty(changeInfo.getBankInfoChanges()), "????????????????????????????????????");
            //Assert.isTrue(CollectionUtils.isNotEmpty(changeInfo.getSiteInfoChanges()), "????????????????????????????????????");
        }

     // ??????????????????????????????
        this.saveOrUpdateChanges(changeInfo, companyId, changeId, userType);
        //??????bussinessId???file upload???
        Long bussinessId =companyId;
        List<Long> fileuploadIds =changeInfo.getFileuploadChanges().stream().collect(Collectors.mapping(FileuploadChange::getFileuploadId,Collectors.toList()));
        if(fileuploadIds.size() != 0) {
            fileCenterClient.binding(fileuploadIds,bussinessId);
        }
        formResultDTO.setFormId(changeId);
        // ??????????????????
        formResultDTO.setEnableWorkFlow(YesOrNo.YES.getValue());

        // ???????????????????????????????????????
        if (InfoChangeStatus.APPROVED.getValue().equals(orderStatus)) {
            this.updateCompanyInfo(changeInfo);

        }

        //2020-12-24 ???????????? bugfix
        /* Begin by chenwt24@meicloud.com   2020-10-29 */
//        if (InfoChangeStatus.SUBMITTED.getValue().equals(orderStatus)) {
//            //?????????????????????
//            String formId = null;
//            try {
//                formId = vendorInfoChangeFlow.submitVendorInfoChangeDTOFlow(changeInfo);
//            } catch (Exception e) {
//                throw new BaseException(e.getMessage());
//            }
//            if (StringUtils.isEmpty(formId)) {
//                throw new BaseException(LocaleHandler.getLocaleMsg("??????OA????????????"));
//            }
//        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (InfoChangeStatus.SUBMITTED.getValue().equals(orderStatus)) {
                    //?????????????????????
                    String formId = null;
                    try {
                        formId = vendorInfoChangeFlow.submitVendorInfoChangeDTOFlow(changeInfo);
                    } catch (Exception e) {
                        throw new BaseException(e.getMessage());
                    }
                    if (StringUtils.isEmpty(formId)) {
                        throw new BaseException(LocaleHandler.getLocaleMsg("??????OA????????????"));
                    }
                }
            }
        });

        /* End by chenwt24@meicloud.com     2020-10-16 */
        return formResultDTO;
    }

    /**
     * Description ?????????????????????????????????????????????????????????
     * (????????????????????????????????????????????????????????????????????????
     *
     * @Param companyInfoChange ???????????????
     * @Author wuwl18@meicloud.com
     * @Date 2020.09.20
     **/
    public void updateCompanyInfo(ChangeInfoDTO changeInfo) {
        CompanyInfoChange companyInfoChange = changeInfo.getCompanyInfoChange();
        Assert.notNull(companyInfoChange, LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        Assert.notNull(companyInfoChange.getCompanyId(), LocaleHandler.getLocaleMsg("???????????????ID????????????"));

        Long companyId = companyInfoChange.getCompanyId();
        /**???????????????????????????*/
        if (null != companyInfoChange.getCompanyId()) {
            CompanyInfo companyInfo = new CompanyInfo();
            companyInfo.setOverseasRelation(companyInfoChange.getOverseasRelation());
            BeanUtils.copyProperties(companyInfoChange, companyInfo);
            String oldCompanyName = iCompanyInfoService.getById(companyId).getCompanyName(); //???????????????????????????
            String newCompanyName = companyInfoChange.getCompanyName(); //???????????????????????????

            iCompanyInfoService.updateById(companyInfo);
            /** ?????? ??????????????????????????????????????????????????? **/
            /** ?????? ???????????????????????? **/
            List<BankInfo> oldBankInfos = iBankInfoService.list(new QueryWrapper<>(new BankInfo().setCompanyId(companyId)));
            List<BankInfoChange> bankInfoChangeList = changeInfo.getBankInfoChanges();

            //?????????????????????????????????Id List
            List<Long> deleteBankInfoIds = new ArrayList<>();

            for (BankInfo oldBankInfo : oldBankInfos) {
                boolean shouldDelete = true;
                for (BankInfoChange bankInfoChange : bankInfoChangeList) {
                    if (Objects.equals(bankInfoChange.getBankInfoId(), oldBankInfo.getBankInfoId())) {
                        shouldDelete = false;
                        break;
                    }
                }
                if (shouldDelete) {
                    deleteBankInfoIds.add(oldBankInfo.getBankInfoId());
                }
            }

            //???????????????????????????????????????
            if (CollectionUtils.isNotEmpty(deleteBankInfoIds)) {
                iBankInfoService.removeByIds(deleteBankInfoIds);
            }
            //????????????????????????????????????
            List<BankInfo> updateBankInfoList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(bankInfoChangeList)) {
                bankInfoChangeList.forEach(bankInfoChange -> {
                    if (null != bankInfoChange) {
                        BankInfo bankInfo = new BankInfo();
                        BeanUtils.copyProperties(bankInfoChange, bankInfo);
                        updateBankInfoList.add(bankInfo);
                    }
                });
                iBankInfoService.saveOrUpdateBatch(updateBankInfoList);
            }
            /** ?????? ????????????????????????????????? **/


            /** ?????? ??????????????????????????????????????????????????? **/
            /** ?????? ???????????????????????? **/
            List<SiteInfo> oldSiteInfos = iSiteInfoService.list(new QueryWrapper<>(new SiteInfo().setCompanyId(companyId)));
            List<SiteInfoChange> siteInfoChangeList = changeInfo.getSiteInfoChanges();

            //?????????????????????????????????Id List
            List<Long> deleteSiteInfoIds = new ArrayList<>();
            for (SiteInfo oldSiteInfo : oldSiteInfos) {
                boolean shouldDelete = true;
                for (SiteInfoChange siteInfoChange : siteInfoChangeList) {
                    if (Objects.equals(siteInfoChange.getSiteInfoId(), oldSiteInfo.getSiteInfoId())) {
                        shouldDelete = false;
                        break;
                    }
                }
                if (shouldDelete) {
                    deleteSiteInfoIds.add(oldSiteInfo.getSiteInfoId());
                }
            }
            //???????????????????????????????????????
            if (CollectionUtils.isNotEmpty(deleteSiteInfoIds)) {
                iSiteInfoService.removeByIds(deleteSiteInfoIds);
            }

            /** ??????????????????????????? **/
            List<SiteInfo> siteInfoList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(siteInfoChangeList)) {
                siteInfoChangeList.forEach(siteInfoChange -> {
                    SiteInfo siteInfo = new SiteInfo();
                    BeanUtils.copyProperties(siteInfoChange, siteInfo);
                    siteInfoList.add(siteInfo);
                });
                iSiteInfoService.saveOrUpdateBatch(siteInfoList);
            }
            /** ?????? ??????????????????????????????????????????????????? **/


            /** ?????? ????????????????????????????????????????????????????????? **/
            /** ?????? ??????????????????????????? **/
            List<ContactInfo> oldContactInfos = iContactInfoService.list(new QueryWrapper<>(new ContactInfo().setCompanyId(companyId)));
            Map<Long, ContactInfo> contactInfoMap = oldContactInfos.stream().collect(Collectors.toMap(ContactInfo::getContactInfoId, Function.identity()));
            List<ContactInfoChange> contactInfoChangeList = changeInfo.getContactInfoChanges();

            //????????????????????????????????????Id List
            List<Long> deleteContactInfoIds = new ArrayList<>();
            for (ContactInfo oldContactInfo : oldContactInfos) {
                boolean shouldDelete = true;
                for (ContactInfoChange contactInfoChange : contactInfoChangeList) {
                    if (Objects.equals(contactInfoChange.getContactInfoId(), oldContactInfo.getContactInfoId())) {
                        shouldDelete = false;
                        break;
                    }
                }
                if (shouldDelete) {
                    deleteContactInfoIds.add(oldContactInfo.getContactInfoId());
                }
            }
            if (CollectionUtils.isNotEmpty(deleteContactInfoIds)) {
                iContactInfoService.removeByIds(deleteContactInfoIds);
            }
            /** ?????? ??????????????????????????? **/

            List<ContactInfo> contactInfoList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(contactInfoChangeList)) {
                contactInfoChangeList.stream().collect(
                        Collectors.collectingAndThen(
                                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getContactInfoId()))), ArrayList::new)
                ).forEach(contactInfoChange -> {
                    ContactInfo contactInfo = new ContactInfo();
                    BeanUtils.copyProperties(contactInfoChange, contactInfo);
                    contactInfoList.add(contactInfo);
                });
                iContactInfoService.saveOrUpdateBatch(contactInfoList);
            }
            /** ?????? ????????????????????????????????????????????????????????? **/

            //???????????? ????????????????????????fileupload??????mangement???
//            List<FileuploadChange> fileuploadChanges =iFileuploadChangeService.getByChangeId(changeInfo.getInfoChange().getChangeId());
            List<ManagementAttachChange> managementAttachChanges =managementAttachChangeService.getByChangeId(changeInfo.getInfoChange().getChangeId());
            List<ManagementAttach> managementAttaches=new ArrayList<>();
            for(ManagementAttachChange managementAttachChange : managementAttachChanges) {
                //????????????
                ManagementAttach managementAttach = new ManagementAttach();
                copyPropertiesByHand(managementAttach, managementAttachChange, companyId);
//                BeanUtils.copyProperties(managementAttachChange,managementAttach);
                //????????????????????????
                ManagementInfo managementInfo =iManagementInfoService.getOne(new QueryWrapper<>(new ManagementInfo().setCompanyId(companyId)));
                if(managementInfo == null) {
                    Long id = IdGenrator.generate();
                    managementInfo = new ManagementInfo();
                    managementInfo.setCompanyId(companyId);
                    managementInfo.setManagementInfoId(id);
                    iManagementInfoService.save(managementInfo);
                }
                managementAttach.setManagementInfoId(managementInfo.getManagementInfoId());
                //??????
                managementAttaches.add(managementAttach);
            }
            //??????
            iManagementAttachService.saveBatch(managementAttaches);
            //??????????????????????????????????????????
            // refreshCompanyInfos(companyId);
            // ??????????????????
            CompletableFuture.runAsync(() -> {
                try {
                    /** ??????????????????????????????erp ???????????????????????????????????????????????? **/
                    if (!oldCompanyName.equals(newCompanyName)) {
                        //??????????????????????????? ??????????????????????????????????????????????????????
                        iCompanyInfoService.sendVendorToErp(companyId);
                    }
                    Long erpVendorId = iCompanyInfoService.getById(companyId).getErpVendorId();
                    //???????????????????????????
                    CompletableFuture.runAsync(() -> {
                        try {
                            sendVendorOtherDatasToErp(companyId, erpVendorId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    /** ?????? ????????????????????????erp **/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }
    /**
     * ??????????????????
     */
    private void copyPropertiesByHand(ManagementAttach managementAttach, ManagementAttachChange managementAttachChange, Long companyId) {
        managementAttach.setManagementAttachId(IdGenrator.generate());
        managementAttach.setCompanyId(companyId);
        managementAttach.setFileuploadId(managementAttachChange.getFileuploadId());
        if(managementAttachChange.getAuthType() != null) {
            managementAttach.setAuthType(managementAttachChange.getAuthType());
        }
        if(managementAttachChange.getAuthDescription() != null) {
            managementAttach.setAuthDescription(managementAttachChange.getAuthDescription());
        }
        if(managementAttachChange.getAuthNum() != null) {
            managementAttach.setAuthNum(managementAttachChange.getAuthNum());
        }
        if(managementAttachChange.getAuthDate() != null) {
            managementAttach.setAuthDate(managementAttachChange.getAuthDate());
        }
        if(managementAttachChange.getAuthOrg() != null) {
            managementAttach.setAuthOrg(managementAttachChange.getAuthOrg());
        }
        if(managementAttachChange.getStartDate() != null) {
            managementAttach.setStartDate(managementAttachChange.getStartDate());
        }
        if(managementAttachChange.getEndDate() != null) {
            managementAttach.setEndDate(managementAttachChange.getEndDate());
        }
    }
    /**
     * ????????????????????????
     * @param changeInfo
     * @param companyId
     * @param changeId
     * @param userType
     */
    @Transactional(rollbackFor = Exception.class)
    private void saveOrUpdateChanges(ChangeInfoDTO changeInfo, Long companyId, Long changeId, String userType) {
        //Boolean result = false;
        ChangeFlowResultDTO changeFlowResultDTO = new ChangeFlowResultDTO();
        if (changeId != null) {
            //????????????????????? companyInfoChange
            if (null != changeInfo.getCompanyInfoChange()) {
                iCompanyInfoChangeService.saveOrUpdateCompany(changeInfo.getCompanyInfoChange(), companyId, changeId);
            }

            //??????????????? contactInfoChanges
            if (CollectionUtils.isNotEmpty(changeInfo.getContactInfoChanges())) {
                for (ContactInfoChange contactInfoChange : changeInfo.getContactInfoChanges()) {
                    iContactInfoChangeService.saveOrUpdateContact(contactInfoChange, companyId, changeId);
                }
            }
            if (changeInfo.getOtherInfoChange() != null) {
                iOtherInfoChangeService.saveOrUpdateOther(changeInfo.getOtherInfoChange(), companyId, changeId);
            }
            if (CollectionUtils.isNotEmpty(changeInfo.getOrgInfoChanges())) {
                for (OrgInfoChange orgInfoChange : changeInfo.getOrgInfoChanges()) {
                    iOrgInfoChangeService.saveOrUpdateOrg(orgInfoChange, companyId, changeId);
                }
            }

            //???????????? bankInfoChanges
            if (CollectionUtils.isNotEmpty(changeInfo.getBankInfoChanges())) {
                for (BankInfoChange bankInfoChange : changeInfo.getBankInfoChanges()) {
                    iBankInfoChangeService.saveOrUpdateBank(bankInfoChange, companyId, changeId);
                }
            }

            //???????????? siteInfoChanges
            if (!CollectionUtils.isEmpty(changeInfo.getSiteInfoChanges())) {
                for (SiteInfoChange siteInfoChange : changeInfo.getSiteInfoChanges()) {
                	//kuangzm ????????????????????????????????? 
                	if (null != siteInfoChange.getOpType() && !siteInfoChange.getOpType().isEmpty() && "Y".equals(siteInfoChange.getOpType())) {
                		iSiteInfoChangeService.removeById(siteInfoChange.getSiteChangeId());
                	} else {
                		iSiteInfoChangeService.saveOrUpdateSite(siteInfoChange, companyId, changeId);
                	}
                }
            }

            if (!CollectionUtils.isEmpty(changeInfo.getFinanceInfoChanges())) {
                for (FinanceInfoChange financeInfoChange : changeInfo.getFinanceInfoChanges()) {
                    iFinanceInfoChangeService.saveOrUpdateFinance(financeInfoChange, companyId, changeId);
                }
            }
            if (changeInfo.getBusinessInfoChange() != null) {
                iBusinessInfoChangeService.saveOrUpdateBusiness(changeInfo.getBusinessInfoChange(), companyId, changeId);
            }
            if (changeInfo.getHolderInfoChange() != null) {
                iHolderInfoChangeService.saveOrUpdateHolder(changeInfo.getHolderInfoChange(), companyId, changeId);
            }
            if (changeInfo.getOperationInfoChange() != null) {
                iOperationInfoChangeService.saveOrUpdateOp(changeInfo.getOperationInfoChange(), companyId, changeId);
            }
            if (changeInfo.getHonorInfoChange() != null) {
                iHonorInfoChangeService.saveOrUpdateHonor(changeInfo.getHonorInfoChange(), companyId, changeId);
            }
            if (!CollectionUtils.isEmpty(changeInfo.getOrgCategoryChanges())) {
                for (OrgCategoryChange orgCategoryChange : changeInfo.getOrgCategoryChanges()) {
                    iOrgCategoryChangeService.saveOrUpdateOrgCategory(orgCategoryChange, companyId, changeId);

                }
            }
            if (!CollectionUtils.isEmpty(changeInfo.getFileuploadChanges())) {
                iFileuploadChangeService.saveOrUpdateAttachs(changeInfo.getFileuploadChanges(), companyId, changeId);
            }
            if(!CollectionUtils.isEmpty(changeInfo.getManagementAttachChanges())) {
                managementAttachChangeService.saveOrUpdateAttachs(changeInfo.getManagementAttachChanges(), companyId, changeId);
            }
        }
    }


    private InfoChange saveOrUpdateInfoChange(InfoChange infoInfoChange) {
        InfoChange resultInfoChange = new InfoChange();
        if (infoInfoChange.getChangeId() != null) {
            resultInfoChange = this.updateInfoChange(infoInfoChange);
        } else {
            resultInfoChange = this.addInfoChange(infoInfoChange);
        }
        return resultInfoChange;
    }

    /**
     * ?????????????????????
     * ??????????????????????????????????????????
     *
     * @param siteInfoChanges
     * @modifiedBy xiexh12@meicloud.com 2020-10-07 20:33
     */
    public void orgSiteDuplicateCheck(List<SiteInfoChange> siteInfoChanges) {
        boolean allowSiteSave = false;
        Set<String> set = new HashSet<>();
        if (CollectionUtils.isNotEmpty(siteInfoChanges)) {
            for (SiteInfoChange siteInfoChange : siteInfoChanges) {
                String orgId = String.valueOf(siteInfoChange.getBelongOprId());
                String vendorSiteCode = siteInfoChange.getVendorSiteCode();
                String orgSite = orgId + "-" + vendorSiteCode;
                if (set.contains(orgSite)) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("???????????????????????????????????????????????????????????????:[").append(siteInfoChange.getOrgName()).append("], ????????????:[").append(siteInfoChange.getVendorSiteCode()).append("]");
                    throw new BaseException(sb.toString());
                } else {
                    set.add(orgSite);
                }
            }
        }
    }

    /**
     * ???????????????????????????
     * ???????????????????????????????????????????????????
     * @param bankInfoChanges
     */
    public void bankInfoCheck(List<BankInfoChange> bankInfoChanges) {

        if (CollectionUtils.isNotEmpty(bankInfoChanges)) {
            Map<String, List<BankInfoChange>> collect = bankInfoChanges.stream().collect(Collectors.groupingBy(BankInfoChange::getCeeaMainAccount));
            if (!collect.containsKey("Y")) {
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????, ?????????1???????????????????????????"));
            }
            if (collect.get("Y").size()>1) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????1????????????"));
            }
            if (Objects.equals(collect.get("Y").get(0).getCeeaEnabled(), "N")) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
            }
        }

    }


    /**
     * ??????????????????????????????
     * ??????????????????
     * @param contactInfoChanges
     */
    public void contactInfoCheck(List<ContactInfoChange> contactInfoChanges) {
        if (CollectionUtils.isNotEmpty(contactInfoChanges)) {
            for (ContactInfoChange contactInfoChange : contactInfoChanges) {
                if (StringUtils.isEmpty(contactInfoChange.getContactName())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????, ?????????????????????????????????"));
                }
            }
        }
    }

    /**
     * ????????????????????????????????????erp??????
     * ???????????????????????????????????????????????????
     */
    public void refreshCompanyInfos(Long companyId) {
        if (Objects.nonNull(companyId)) {
            //??????????????????
            List<BankInfo> bankInfos = iBankInfoService.list(
                    new QueryWrapper<>(new BankInfo().setCompanyId(companyId))
            );
            if (CollectionUtils.isNotEmpty(bankInfos)) {
                bankInfos.forEach(bankInfo -> {
                    bankInfo.setIfPushErp("N");
                });
                iBankInfoService.updateBatchById(bankInfos);
            }
            //????????????
            List<SiteInfo> siteInfos = iSiteInfoService.list(
                    new QueryWrapper<>(new SiteInfo().setCompanyId(companyId))
            );
            if (CollectionUtils.isNotEmpty(siteInfos)) {
                siteInfos.forEach(siteInfo -> {
                    siteInfo.setIfPushErp("N");
                });
                iSiteInfoService.updateBatchById(siteInfos);
            }
            //???????????????
            List<ContactInfo> contactInfos = iContactInfoService.list(
                    new QueryWrapper<>(new ContactInfo().setCompanyId(companyId))
            );
            if (CollectionUtils.isNotEmpty(contactInfos)) {
                contactInfos.forEach(contactInfo -> {
                    contactInfo.setIfPushErp("N");
                });
                iContactInfoService.updateBatchById(contactInfos);
            }
        }
    }

    @Override
    public void commonCheck(ChangeInfoDTO changeInfo, String orderStatus) {
        InfoChange existChange = new InfoChange();
        Assert.notNull(changeInfo, LocaleHandler.getLocaleMsg("????????????????????????"));
        Assert.notNull(changeInfo.getInfoChange(), LocaleHandler.getLocaleMsg("????????????????????????"));
        Assert.notNull(changeInfo.getCompanyInfoChange(), LocaleHandler.getLocaleMsg("????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????"));
        Assert.notNull(changeInfo.getCompanyInfoChange().getCompanyId(), LocaleHandler.getLocaleMsg("??????????????????????????????????????????"));
//        Assert.isTrue( StringUtils.isNotBlank(changeInfo.getInfoChange().getEnable4MChange()) ,
//                LocaleHandler.getLocaleMsg("?????????4M??????????????????"));
        InfoChange infoInfoChange = changeInfo.getInfoChange();
        if (infoInfoChange.getChangeId() != null) {
            existChange = this.getById(infoInfoChange.getChangeId());
            Assert.notNull(existChange, LocaleHandler.getLocaleMsg("?????????????????????"));
            if (InfoChangeStatus.APPROVED.getValue().equals(existChange.getChangeStatus())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
            }
            switch (orderStatus) {
                case "DRAFT":
                    checkBeforesaveTemporary(existChange);
                    break;
                case "VENDOR_SUBMITTED":
                    checkBeforeVendorSubmitted(existChange, infoInfoChange);
                    break;
                case "SUBMITTED":
                    checkBeforeSubmitted(existChange, infoInfoChange);
                    break;
                case "REJECTED":
                    checkBeforeApprove(existChange);
                    break;
                case "APPROVED":
                    checkBeforeApprove(existChange);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public ChangeInfoDTO getInfoByChangeId(Long changeId) {
        ChangeInfoDTO changeInfoDto = new ChangeInfoDTO();
        InfoChange infoChange = this.getById(changeId);
        CompanyInfoChange companyInfoChange = iCompanyInfoChangeService.getByChangeId(changeId);
        List<ContactInfoChange> contactInfoChanges = iContactInfoChangeService.getByChangeId(changeId);
        OtherInfoChange otherInfoChange = iOtherInfoChangeService.getByChangeId(changeId);
        List<BankInfoChange> bankInfoChanges = iBankInfoChangeService.getByChangeId(changeId);
        List<SiteInfoChange> siteInfoChanges = iSiteInfoChangeService.getByChangeId(changeId);
        List<FinanceInfoChange> financeInfoChanges = iFinanceInfoChangeService.getByChangeId(changeId);
        List<OrgCategoryChange> orgCategoryChanges = iOrgCategoryChangeService.getByChangeId(changeId);
        List<OrgInfoChange> orgInfoChanges = iOrgInfoChangeService.getByChangeId(changeId);
        BusinessInfoChange businessInfoChange = iBusinessInfoChangeService.getByChangeId(changeId);
        HonorInfoChange honorInfoChange = iHonorInfoChangeService.getByChangeId(changeId);
        HolderInfoChange holderInfoChange = iHolderInfoChangeService.getByChangeId(changeId);
        OperationInfoChange operationInfoChange = iOperationInfoChangeService.getByChangeId(changeId);
        List<FileuploadChange> attachFileChanges = iFileuploadChangeService.getByChangeId(changeId);
        List<ManagementAttachChange> managementAttachChanges = managementAttachChangeService.getByChangeId(changeId);
        changeInfoDto.setInfoChange(infoChange);
        changeInfoDto.setCompanyInfoChange(companyInfoChange);
        changeInfoDto.setContactInfoChanges(contactInfoChanges);
        changeInfoDto.setOtherInfoChange(otherInfoChange);
        changeInfoDto.setBusinessInfoChange(businessInfoChange);
        changeInfoDto.setBankInfoChanges(bankInfoChanges);
        changeInfoDto.setSiteInfoChanges(siteInfoChanges);
        changeInfoDto.setFinanceInfoChanges(financeInfoChanges);
        changeInfoDto.setHolderInfoChange(holderInfoChange);
        changeInfoDto.setHonorInfoChange(honorInfoChange);
        changeInfoDto.setOperationInfoChange(operationInfoChange);
        changeInfoDto.setOrgCategoryChanges(orgCategoryChanges);
        changeInfoDto.setOrgInfoChanges(orgInfoChanges);
        changeInfoDto.setFileuploadChanges(attachFileChanges);
        changeInfoDto.setManagementAttachChanges(managementAttachChanges);
        return changeInfoDto;
    }

    @Override
    @Transactional
    public void deleteChangeInfo(Long changeId) {
        InfoChange infoChange = this.getById(changeId);
        Assert.notNull(infoChange, LocaleHandler.getLocaleMsg("?????????????????????"));
        if (infoChange.getChangeStatus().equals(InfoChangeStatus.DRAFT.getValue())) {
            this.removeById(changeId);
            iCompanyInfoChangeService.removeByChangeId(changeId);
            iContactInfoChangeService.removeByChangeId(changeId);
            iOtherInfoChangeService.removeByChangeId(changeId);
            iBankInfoChangeService.removeByChangeId(changeId);
            iFinanceInfoChangeService.removeByChangeId(changeId);
            iOrgCategoryChangeService.removeByChangeId(changeId);
            iOrgInfoChangeService.removeByChangeId(changeId);
            iBusinessInfoChangeService.removeByChangeId(changeId);
            iHonorInfoChangeService.removeByChangeId(changeId);
            iHolderInfoChangeService.removeByChangeId(changeId);
            iOperationInfoChangeService.removeByChangeId(changeId);
            iDimFieldContextChangeService.deleteByChangeId(changeId);
            iFileuploadChangeService.deleteByChangeId(changeId);
            managementAttachChangeService.deleteByChangeId(changeId);

        } else {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
        }
    }

    /**
     * ????????????
     *
     * @param changeId
     */
    @Transactional
    @Override
    public void abandon(Long changeId) {
        ChangeInfoDTO changeInfoDTO = this.getInfoByChangeId(changeId);
        InfoChange infoChange = changeInfoDTO.getInfoChange();
        Assert.notNull(ObjectUtils.isEmpty(infoChange), "??????????????????????????????");
        String changeStatus = infoChange.getChangeStatus();
        Assert.isTrue(InfoChangeStatus.WITHDRAW.getValue().equals(changeStatus) || InfoChangeStatus.REJECTED.getValue().equals(changeStatus), "??????????????????????????????????????????????????????");
        infoChange.setChangeStatus(InfoChangeStatus.ABANDONED.getValue());
        this.updateById(infoChange);
        SrmFlowBusWorkflow srmworkflowForm = baseClient.getSrmFlowBusWorkflow(changeId);
        if (srmworkflowForm != null) {
            try {
                changeInfoDTO.setProcessType("N");
                vendorInfoChangeFlow.submitVendorInfoChangeDTOFlow(changeInfoDTO);
            } catch (Exception e) {
                Assert.isTrue(false, "??????????????????????????????");
            }
        }
    }

    private void checkBeforeVendorSubmitted(InfoChange existChange, InfoChange infoInfoChange) {
        String approveStatus = existChange.getChangeStatus();
        if (!InfoChangeStatus.DRAFT.getValue().equals(approveStatus) &&
                !InfoChangeStatus.REJECTED.getValue().equals(approveStatus) &&
                !InfoChangeStatus.WITHDRAW.getValue().equals(approveStatus)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        }
        if (StringUtils.isBlank(infoInfoChange.getChangeExplain())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????"));
        }
        InfoChange query = new InfoChange();
        query.setCompanyId(existChange.getCompanyId());
        query.setChangeStatus(InfoChangeStatus.VENDOR_SUBMITTED.getValue());
        List<InfoChange> companyInfoChanges = this.list(new QueryWrapper<InfoChange>(query));
        if (!CollectionUtils.isEmpty(companyInfoChanges)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????!"));
        }
    }

    private void checkBeforeSubmitted(InfoChange existChange, InfoChange infoInfoChange) {
        String approveStatus = existChange.getChangeStatus();
        if (!InfoChangeStatus.DRAFT.getValue().equals(approveStatus) &&
                !InfoChangeStatus.REJECTED.getValue().equals(approveStatus) &&
                !InfoChangeStatus.WITHDRAW.getValue().equals(approveStatus) &&
                !InfoChangeStatus.VENDOR_SUBMITTED.getValue().equals(approveStatus)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        }
        if (StringUtils.isBlank(infoInfoChange.getChangeExplain())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????"));
        }
        InfoChange query = new InfoChange();
        query.setCompanyId(existChange.getCompanyId());
        query.setChangeStatus(InfoChangeStatus.SUBMITTED.getValue());
        List<InfoChange> companyInfoChanges = this.list(new QueryWrapper<InfoChange>(query));
        if (!CollectionUtils.isEmpty(companyInfoChanges)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????!"));
        }
    }

    private void checkBeforeApprove(InfoChange existChange) {
        String approveStatus = existChange.getChangeStatus();
        if (!InfoChangeStatus.SUBMITTED.getValue().equals(approveStatus)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        }
    }

    private void checkBeforesaveTemporary(InfoChange existChange) {
        String approveStatus = StringUtils.isNotBlank(existChange.getChangeStatus()) ? existChange.getChangeStatus() : "DRAFT";
        if (!InfoChangeStatus.DRAFT.getValue().equals(approveStatus) &&
                !InfoChangeStatus.REJECTED.getValue().equals(approveStatus)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        }
    }

    @Override
    @Transactional
    public void updateChange(ChangeInfoDTO changeInfo, String orderStatus) {
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        if (InfoChangeStatus.APPROVED.getValue().equals(orderStatus)) {
            user = rbacClient.findByUsername(defaultUsername);
            Assert.notNull(user, LocaleHandler.getLocaleMsg("??????????????????"));
        }

        InfoChange changeData = new InfoChange();
        changeData = changeInfo.getInfoChange();
        changeData.setChangeStatus(orderStatus);
        if (InfoChangeStatus.APPROVED.getValue().equals(orderStatus)) {
            changeData.setChangeApprovedDate(new Date());
            changeData.setChangeApprovedBy(user.getUsername());
            changeData.setChangeApprovedById(user.getUserId());
        }
        this.updateById(changeData);
        //??????????????????????????????
        if (InfoChangeStatus.APPROVED.getValue().equals(orderStatus)) {
            this.changeInfoData(changeInfo);

            iCompanyStatusLogService.saveStatusLog(changeData.getCompanyId(),
                    changeData.getChangeApprovedById(),
                    changeData.getChangeApprovedBy(),
                    user.getUserType(),
                    InfoChangeStatus.APPROVED.getValue(),
                    changeData.getChangeExplain(),
                    new Date(),
                    "?????????????????????"
            );
        }
    }

    private void changeInfoData(ChangeInfoDTO changeInfo) {

        iCompanyInfoService.saveOrUpdateInfoChange(changeInfo);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveChangeWithFlow(ChangeInfoDTO dto) {
        saveOrUpdateChange(dto, SampleStatusType.SUBMITTED.getValue());
        InfoChange infoChange = dto.getInfoChange();
        String companyName = dto.getCompanyInfoChange().getCompanyName();
        Long changeId = infoChange.getChangeId();
        Long menuId = dto.getInfoChange().getMenuId();
        //Boolean enableWorkFlow = enableWorkFlow(menuId);
        //TODO
        Boolean enableWorkFlow = false;
        Map<String, Object> map = new HashMap();
        map.put("businessId", infoChange.getChangeId());
        map.put("subject", companyName);
        if (enableWorkFlow) {
            infoChange.setApproveStatus(SampleStatusType.SUBMITTED.getValue());
            infoChange.setChangeStatus(SampleStatusType.SUBMITTED.getValue());
            this.updateById(infoChange);
            if (StringUtil.isEmpty(infoChange.getCbpmInstaceId())) {
                CbpmRquestParamDTO request = buildCbpmRquest(infoChange, companyName);
                map = workFlowFeign.initProcess(request);
                redisTemplate.opsForValue().set("sup-info" + changeId, dto);
            } else {
                map.put("fdId", infoChange.getCbpmInstaceId());
            }
        } else {
            //???????????????????????????????????????
            this.updateChange(dto, InfoChangeStatus.APPROVED.getValue());
        }

        return map;
    }

    private CbpmRquestParamDTO buildCbpmRquest(InfoChange infoChange, String companyName) {
        CbpmRquestParamDTO cbpmRquestParamDTO = new CbpmRquestParamDTO();
        cbpmRquestParamDTO.setBusinessId(String.valueOf(infoChange.getChangeId()));
        cbpmRquestParamDTO.setTemplateCode(CbpmFormTemplateIdEnum.CHANGE_SUP_INFO.getKey());
        cbpmRquestParamDTO.setSubject(companyName);
        cbpmRquestParamDTO.setFdId(infoChange.getCbpmInstaceId());
        return cbpmRquestParamDTO;
    }

    private Boolean enableWorkFlow(Long menuId) {
        Boolean flowEnable;
        Permission menu = rbacClient.getMenu(menuId);
        try {
            flowEnable = workFlowFeign.getFlowEnable(menuId, menu.getFunctionId(), CbpmFormTemplateIdEnum.CHANGE_SUP_INFO.getKey());
        } catch (FeignException e) {
            log.error("???????????????????????????,??????????????????????????????,?????? menuId???" + menuId + ",functionId" + menu.getFunctionId()
                    + ",templateCode" + CbpmFormTemplateIdEnum.CHANGE_SUP_INFO.getKey() + "?????????", e);
            throw new BaseException("?????????????????????????????????????????????????????????");
        }
        return flowEnable;
    }

    @Override
    @Transactional
    public void updateChangeWithFlow(ChangeInfoDTO changeInfo, String orderStatus) {
        InfoChange changeData = new InfoChange();
        changeData = changeInfo.getInfoChange();
        changeData.setChangeStatus(orderStatus);
        if (InfoChangeStatus.APPROVED.getValue().equals(orderStatus)) {

            changeData.setChangeApprovedDate(new Date());
            changeData.setChangeApprovedBy("???????????????feign??????");
            changeData.setChangeApprovedById(-1L);
        }
        this.updateById(changeData);
        //??????????????????????????????
        if (InfoChangeStatus.APPROVED.getValue().equals(orderStatus)) {
            this.changeInfoData(changeInfo);

            iCompanyStatusLogService.saveStatusLog(changeData.getCompanyId(),
                    changeData.getChangeApprovedById(),
                    changeData.getChangeApprovedBy(),
                    "BUYER",
                    InfoChangeStatus.APPROVED.getValue(),
                    changeData.getChangeExplain(),
                    new Date(),
                    "????????????????????????"
            );
        }
    }

    /**
     * ?????????????????????
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    @Override
    public List<CompanyInfo> getVendors() {
        /** ?????????????????????????????????????????????????????????????????? **/
        List<String> statusList = new ArrayList<>();
        statusList.add(InfoChangeStatus.DRAFT.getValue());
        statusList.add(InfoChangeStatus.SUBMITTED.getValue());
        List<InfoChange> infoChanges = this.list(
                new QueryWrapper<InfoChange>().in("CHANGE_STATUS", statusList)
        );
        List<Long> companyIds = new ArrayList<>();
        infoChanges.forEach(infoChange -> {
            Long companyId = infoChange.getCompanyId();
            companyIds.add(companyId);
        });

        /** ??????????????????????????????????????? **/
        List<CompanyInfo> companyInfos = iCompanyInfoService.list(new QueryWrapper<>(new CompanyInfo().setStatus(InfoChangeStatus.APPROVED.getValue())));
        List<CompanyInfo> returnCompanys = new ArrayList<>();
        for (CompanyInfo companyInfo : companyInfos) {
            if (!companyIds.contains(companyInfo.getCompanyId()))
                returnCompanys.add(companyInfo);
        }
        return returnCompanys;
    }

    /**
     * ?????????????????????????????????
     *
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeChangeById(Long id) {

        InfoChange existInfoChange = this.getById(id);
        String changeStatus = existInfoChange.getChangeStatus();
        Assert.isTrue(!changeStatus.equals(InfoChangeStatus.APPROVED.getValue()), "??????????????????????????????");

        /** ???????????????????????? **/
        QueryWrapper<BankInfoChange> bankInfoChangeQueryWrapper = new QueryWrapper<>(
                new BankInfoChange().setChangeId(id)
        );
        iBankInfoChangeService.remove(bankInfoChangeQueryWrapper);
        /** ?????????????????????????????? **/

        /** ???????????????????????? **/
        QueryWrapper<SiteInfoChange> siteInfoChangeQueryWrapper = new QueryWrapper<>(
                new SiteInfoChange().setChangeId(id)
        );
        iSiteInfoChangeService.remove(siteInfoChangeQueryWrapper);
        /** ?????????????????????????????? **/

        /** ??????????????????????????? **/
        QueryWrapper<ContactInfoChange> contactInfoChangeQueryWrapper = new QueryWrapper<>(
                new ContactInfoChange().setChangeId(id)
        );
        iContactInfoChangeService.remove(contactInfoChangeQueryWrapper);
        /** ????????????????????????????????? **/

        /** ????????????????????? **/
        this.removeById(id);
        /** ????????????????????? **/
    }

    /**
     * ???????????????id????????????????????????????????????
     * ????????? ????????? ?????? ????????????????????????????????????????????????
     * @param companyId
     * @return
     */
    @Override
    public InfoChangeDTO ifAddInfoChange(Long companyId) {
        String notChangeList[] = {"DRAFT", "SUBMITTED", "WITHDRAW", "REJECTED"};

        InfoChangeDTO dto = new InfoChangeDTO();
        dto.setChangeStatus("Y");
        if (Objects.isNull(iCompanyInfoService.getById(companyId))) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????,srm?????????id:" + companyId));
        }
        CompanyInfo companyInfo = iCompanyInfoService.getById(companyId);
        // ?????????????????????????????????
        if ("Y".equals(companyInfo.getIsBacklist())) {
            dto.setChangeStatus("N");
        }
        // ????????? ????????? ?????? ???????????????, ????????????????????????????????????
        List<InfoChange> infoChangeList = this.list(Wrappers.lambdaQuery(InfoChange.class)
                .eq(InfoChange::getCompanyId, companyId)
                .in(InfoChange::getChangeStatus, notChangeList));
        if (CollectionUtils.isNotEmpty(infoChangeList)){
            dto.setChangeStatus("N");
        }
        return dto;
    }

    /**
     * ???????????????????????????????????????
     * @param changeId
     */
    @Override
    public void buyerReject(Long changeId) {
        InfoChange infoChange = this.getById(changeId);
        Optional.ofNullable(infoChange.getChangeStatus()).orElseThrow(() -> new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????, ??????????????????.")));
        String changeStatus = infoChange.getChangeStatus();
        Assert.isTrue(InfoChangeStatus.VENDOR_SUBMITTED.getValue().equals(changeStatus), "?????????????????????????????????????????????, ????????????..");
        infoChange.setChangeStatus(InfoChangeStatus.DRAFT.getValue());
        this.updateById(infoChange);
    }

    /**
     * ??????erp??????????????????
     * @param companyId
     * @param erpVendorId
     */
    public void sendVendorOtherDatasToErp(Long companyId, Long erpVendorId){
        /** ?????? ???????????????????????????????????????????????????erp **/
            //???????????????????????????
            submitExector.execute(() -> {
                iCompanyInfoService.sendVendorBank(companyId, erpVendorId);
            });
            //???????????????????????????
            submitExector.execute(() -> {
                iCompanyInfoService.sendVendorSite(companyId, erpVendorId);
            });
            //??????????????????????????????
            submitExector.execute(() -> {
                iCompanyInfoService.sendVendorContact(companyId, erpVendorId);
            });
    }
}
