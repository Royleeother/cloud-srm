package com.midea.cloud.srm.sup.info.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.ApproveStatusType;
import com.midea.cloud.common.enums.SupplierDataSourceType;
import com.midea.cloud.common.enums.review.CategoryStatus;
import com.midea.cloud.common.enums.review.FormType;
import com.midea.cloud.common.enums.sup.DueDate;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationUser;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.model.supplier.change.entity.OrgCategoryChange;
import com.midea.cloud.srm.model.supplier.info.dto.*;
import com.midea.cloud.srm.model.supplier.info.entity.*;
import com.midea.cloud.srm.model.supplier.statuslog.entity.CompanyStatusLog;
import com.midea.cloud.srm.sup.info.mapper.VendorInformationMapper;
import com.midea.cloud.srm.sup.info.service.*;
import com.midea.cloud.srm.sup.quest.mapper.QuestSupplierMapper;
import com.midea.cloud.srm.sup.statuslog.service.ICompanyStatusLogService;
import com.netflix.discovery.converters.Auto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ???????????????ServiceImpl
 * </pre>
 *
 * @author xiexh12@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/9/9 20:24
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class VendorInformationServiceImpl extends ServiceImpl<VendorInformationMapper, VendorInformation> implements IVendorInformationService {

    @Autowired
    private ICompanyInfoService iCompanyInfoService;

    @Autowired
    private ICompanyInfoDetailService iCompanyInfoDetailService;

    @Autowired
    private IManagementInfoService iManagementInfoService;

    @Autowired
    private IManagementAttachService iManagementAttachService;

    @Autowired
    private IBankInfoService iBankInfoService;

    @Autowired
    private IContactInfoService iContactInfoService;

    @Autowired
    private IFinanceInfoService iFinanceInfoService;

    @Autowired
    private IOperationInfoService iOperationInfoService;

    @Autowired
    private IOperationQualityService iOperationQualityService;

    @Autowired
    private IOperationProductService iOperationProductService;

    @Autowired
    private IOperationEquipmentService iOperationEquipmentService;

    @Autowired
    private IOrgCategoryService iOrgCategoryService;

    @Autowired
    private IOrgInfoService iOrgInfoService;

    @Autowired
    private IBusinessInfoService iBusinessInfoService;

    @Autowired
    private RbacClient rbacClient;

    @Autowired
    private ICompanyStatusLogService iCompanyStatusLogService;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private BaseClient baseClient;
    @Autowired
    private QuestSupplierMapper questSupplierMapper;

    @Override
    public PageInfo<CompanyInfo> listByDTO(CompanyRequestDTO companyRequestDTO) {
        List<CompanyInfo> companyInfoList = iCompanyInfoService.listByDTO(companyRequestDTO);
        if (CollectionUtils.isNotEmpty(companyInfoList)) {
            LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
            List<Long> companyIdList = companyInfoList.stream().map(CompanyInfo::getCompanyId).collect(Collectors.toList());
            List<String> approvalStatusList = companyRequestDTO.getApprovalStatusList();
            if(CollectionUtils.isEmpty(approvalStatusList)) {
                approvalStatusList= Arrays.asList("APPROVED");
            }
            List<OrganizationUser> organizationUsers = loginAppUser.getOrganizationUsers();
            List<Long> orgIdList = null;
            if (CollectionUtils.isNotEmpty(organizationUsers)) {
                orgIdList = organizationUsers.stream().map(OrganizationUser::getOrganizationId).collect(Collectors.toList());
            } else {
                orgIdList = new ArrayList<>();
            }
            List<Long> companyIdTempList = questSupplierMapper.countQuestByCompanyId(approvalStatusList, companyIdList, orgIdList);
            for (CompanyInfo companyInfo : companyInfoList) {
                if (companyIdTempList.contains(companyInfo.getCompanyId())) {
                    companyInfo.setShowQuestSupplier(true);
                }
            }
        }
        return new PageInfo<CompanyInfo>(companyInfoList);
    }

    @Override
    public List<CategoryDTO> getCategoryListByCompanyId(Long companyId, Long categoryId) {
        Assert.notNull(companyId, "????????????????????????");
        List<CategoryDTO> categoryDTOList = this.baseMapper.getCategoryListByCompanyId(companyId, categoryId);
        return categoryDTOList;
    }

    /**
     * ????????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????????????????
     * @param infoDTO
     */
    @Override
    public void saveOrUpdateInformation(InfoDTO infoDTO) {
        CompanyInfo companyInfo = infoDTO.getCompanyInfo();
        iCompanyInfoService.updateById(companyInfo);
        List<OrgCategory> orgCategoryList = infoDTO.getOrgCategorys();
        if (!orgCategoryList.isEmpty()){
            iOrgCategoryService.updateBatchById(orgCategoryList);
        }
    }

    @Override
    @Transactional
    public void deleteVendorInformation(Long companyId) {
        /** ???????????????????????? companyInfoDetail ?????? **/
        QueryWrapper<CompanyInfoDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("COMPANY_ID", companyId);
        iCompanyInfoDetailService.remove(detailWrapper);
        /** ???????????????????????? companyInfoDetail ?????? **/

        /** ???????????????????????? managementInfo ?????? **/
        QueryWrapper<ManagementInfo> managementInfoWrapper = new QueryWrapper<>();
        managementInfoWrapper.eq("COMPANY_ID", companyId);
        iManagementInfoService.remove(managementInfoWrapper);
        /** ???????????????????????? managementInfo ?????? **/

        /** ???????????????????????????????????? managementAttaches List ?????? **/
        QueryWrapper<ManagementAttach> managementAttachWrapper = new QueryWrapper<>();
        managementAttachWrapper.eq("COMPANY_ID", companyId);
        iManagementAttachService.remove(managementAttachWrapper);
        /** ???????????????????????????????????? managementAttaches List ?????? **/

        /** ???????????????????????????????????? bankInfos List ?????? **/
        QueryWrapper<BankInfo> bankInfoWrapper = new QueryWrapper<>();
        bankInfoWrapper.eq("COMPANY_ID", companyId);
        iBankInfoService.remove(bankInfoWrapper);
        /** ???????????????????????????????????? bankInfos List ?????? **/

        /** ????????????????????? contactInfos List ?????? **/
        QueryWrapper<ContactInfo> contactInfoWrapper = new QueryWrapper<>();
        contactInfoWrapper.eq("COMPANY_ID", companyId);
        iContactInfoService.remove(contactInfoWrapper);
        /** ????????????????????? contactInfos List ?????? **/

        /** ?????????????????? financeInfos List ?????? **/
        QueryWrapper<FinanceInfo> financeInfoWrapper = new QueryWrapper<>();
        financeInfoWrapper.eq("COMPANY_ID", companyId);
        iFinanceInfoService.remove(financeInfoWrapper);
        /** ?????????????????? financeInfos List ?????? **/

        /** ?????????????????? operationInfo ?????? **/
        QueryWrapper<OperationInfo> operationInfoWrapper = new QueryWrapper<>();
        operationInfoWrapper.eq("COMPANY_ID", companyId);
        iOperationInfoService.remove(operationInfoWrapper);
        /** ?????????????????? operationInfo ?????? **/

        /** ???????????????????????????????????? operationQualities List ?????? **/
        QueryWrapper<OperationQuality> operationQualityWrapper = new QueryWrapper<>();
        operationQualityWrapper.eq("COMPANY_ID", companyId);
        iOperationQualityService.remove(operationQualityWrapper);
        /** ???????????????????????????????????? operationQualities List ?????? **/

        /** ?????????????????????????????? operationProducts List ?????? **/
        QueryWrapper<OperationProduct> operationProductWrapper = new QueryWrapper<>();
        operationProductWrapper.eq("COMPANY_ID", companyId);
        iOperationProductService.remove(operationProductWrapper);
        /** ?????????????????????????????? operationProducts List ?????? **/

        /** ?????????????????????????????? operationEquipments List ?????? **/
        QueryWrapper<OperationEquipment> OperationEquipmentWrapper = new QueryWrapper<>();
        OperationEquipmentWrapper.eq("COMPANY_ID", companyId);
        iOperationEquipmentService.remove(OperationEquipmentWrapper);
        /** ?????????????????????????????? operationEquipments List ?????? **/

        /** ??????????????????????????? orgCategorys List ?????? **/
        QueryWrapper<OrgCategory> orgCategoryWrapper = new QueryWrapper<>();
        orgCategoryWrapper.eq("COMPANY_ID", companyId);
        iOrgCategoryService.remove(orgCategoryWrapper);
        /** ??????????????????????????? orgCategorys List ?????? **/

        /** ???????????????????????? orgInfos List ?????? **/
        QueryWrapper<OrgInfo> orgInfoWrapper = new QueryWrapper<>();
        orgInfoWrapper.eq("COMPANY_ID", companyId);
        iOrgInfoService.remove(orgInfoWrapper);
        /** ???????????????????????? orgInfos List ?????? **/

        /** ?????????????????? businessInfos List ?????? **/
        QueryWrapper<BusinessInfo> businessInfoWrapper = new QueryWrapper<>();
        businessInfoWrapper.eq("COMPANY_ID", companyId);
        iBusinessInfoService.remove(businessInfoWrapper);
        /** ?????????????????? orgInfos List ?????? **/

        /** ?????????????????? userInfo ?????? **/
        rbacClient.deleteByCompanyId(companyId);
        /** ?????????????????? userInfo ?????? **/

        /** ???????????????????????? fileUploads List ?????? **/
        Fileupload fileupload = new Fileupload();
        fileupload.setBusinessId(companyId);
        fileCenterClient.deleteByParam(fileupload);
        /** ???????????????????????? fileUploads List ?????? **/

        /** ???????????????????????? companyStatusLogs List ?????? **/
        QueryWrapper<CompanyStatusLog> companyStatusLogWrapper = new QueryWrapper<>();
        companyStatusLogWrapper.eq("COMPANY_ID", companyId);
        iCompanyStatusLogService.remove(companyStatusLogWrapper);
        /** ???????????????????????? companyStatusLogs List ?????? **/

        /** ?????????????????????????????? companyInfo ?????? **/
        iCompanyInfoService.removeById(companyId);
        /** ?????????????????????????????? companyInfo ?????? **/
    }

    @Override
    public void vendorInformationApprove(Long companyId) {
        CompanyInfo companyInfo = iCompanyInfoService.getById(companyId);
        String oldStatus = companyInfo.getStatus();
        Assert.isTrue(oldStatus.equals(ApproveStatusType.SUBMITTED.getValue()), "??????????????????????????????????????????");
        companyInfo.setStatus(ApproveStatusType.APPROVED.getValue());
        companyInfo.setStatusName(ApproveStatusType.APPROVED.getName());
        // ??????????????? ?????????????????????
        if (StringUtils.isEmpty(companyInfo.getCompanyCode())) {
            companyInfo.setCompanyCode(baseClient.seqGenForAnon(SequenceCodeConstant.SEQ_SUP_COMPANY_CODE));
        }
        iCompanyInfoService.updateById(companyInfo);

        //????????????????????????
        List<OrgCategory> orgCategories = iOrgCategoryService.list(new QueryWrapper<>(new OrgCategory().setCompanyId(companyId)));
        String orgCategoryStatus = "";
        String formType = "";
        if(SupplierDataSourceType.MANUALLY_CREATE.name().equals(companyInfo.getDataSources()) && ApproveStatusType.APPROVED.getValue().equals(companyInfo.getStatus())) {
            orgCategoryStatus = CategoryStatus.GREEN.name();
            formType = FormType.GREEN_CHANNEL.name();
        }
        if (SupplierDataSourceType.ONESELF_REGISTER.name().equals(companyInfo.getDataSources())) {
            orgCategoryStatus = CategoryStatus.REGISTERED.name();
            formType = FormType.SUPPLIER_REGISTRATION.name();
        }
        if (CollectionUtils.isNotEmpty(orgCategories)) {
            for (OrgCategory orgCategory : orgCategories) {
                if (orgCategory == null) continue;
                OrgCategorySaveDTO orgCategorySaveDTO = new OrgCategorySaveDTO();
                orgCategorySaveDTO.setOrgCategory(new OrgCategory()
                        .setCompanyId(companyInfo.getCompanyId())
                        .setOrgId(orgCategory.getOrgId())
                        .setOrgName(orgCategory.getOrgName())
                        .setOrgCode(orgCategory.getOrgCode())
                        .setCategoryId(orgCategory.getCategoryId())
                        .setCategoryCode(orgCategory.getCategoryCode())
                        .setCategoryName(orgCategory.getCategoryName())
                        .setCategoryFullName(orgCategory.getCategoryFullName())
                        .setCategoryFullId(orgCategory.getCategoryFullId())
                        .setServiceStatus(orgCategoryStatus));//ToDo
                List<OrgCategoryChange> orgCategoryChanges = new ArrayList<>();
                orgCategoryChanges.add(new OrgCategoryChange().setFormType(formType).setFormNum(companyInfo.getCompanyCode()));
                orgCategorySaveDTO.setOrgCategoryChanges(orgCategoryChanges);
                iOrgCategoryService.collectOrgCategory(orgCategorySaveDTO);
            }
        }
    }

    @Override
    public PageInfo<ManagementAttach> listManagementAttachPageByDTO(ManagementAttachRequestDTO managementAttachRequestDTO) {
        PageUtil.startPage(managementAttachRequestDTO.getPageNum(), managementAttachRequestDTO.getPageSize());
//        String dueDate = managementAttachRequestDTO.getDueDate();
//        Date nowdate = new Date();
//        Calendar futureCalendar = Calendar.getInstance();
//        Calendar pastCalendar = Calendar.getInstance();
//        futureCalendar.setTime(nowdate);
//        pastCalendar.setTime(nowdate);
//
//        Date newFutureDueDate = nowdate;
//        Date newPastDueDate = nowdate;
//        if (dueDate != null){
//            switch (dueDate){
//                case "ONE_MONTH" : {
//                    futureCalendar.add(Calendar.MONTH, 1);
//                    pastCalendar.add(Calendar.MONTH, -1);
//                    newFutureDueDate = futureCalendar.getTime();
//                    newPastDueDate = pastCalendar.getTime();
//                    break;
//                }
//                case "THREE_MONTHS": {
//                    futureCalendar.add(Calendar.MONTH, 3);
//                    pastCalendar.add(Calendar.MONTH, -3);
//                    newFutureDueDate = futureCalendar.getTime();
//                    newPastDueDate = pastCalendar.getTime();
//                    break;
//                }
//                case "SIX_MONTHS": {
//                    futureCalendar.add(Calendar.MONTH, 6);
//                    pastCalendar.add(Calendar.MONTH, -6);
//                    newFutureDueDate = futureCalendar.getTime();
//                    newPastDueDate = pastCalendar.getTime();
//                    break;
//                }
//            }
//        }
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        String dueDateFutureString = simpleDateFormat.format(newFutureDueDate);
//        String dueDatePastString = simpleDateFormat.format(newPastDueDate);
//        managementAttachRequestDTO.setFutureDate(dueDateFutureString);
//        managementAttachRequestDTO.setPastDate(dueDatePastString);
//        List<ManagementAttach> managementAttachList = this.baseMapper.listAttachByDTO(managementAttachRequestDTO);
        List<ManagementAttach> managementAttachList = this.baseMapper.listAttachMix(managementAttachRequestDTO);
        return new PageInfo<ManagementAttach>(managementAttachList);
    }

    @Override
    public List<ManagementAttach> listAllManagementAttachByDTO(ManagementAttach managementAttach) {
        Map<String,Object> map =new HashMap<>();
        map.put("COMPANY_ID",managementAttach.getCompanyId());
        return iManagementAttachService.listByMap(map);
    }

    /**
     * ????????????????????? ????????????SUBMITTED?????????DRAFT
     * @param companyId
     */
    @Override
    public void rejectVendorInformation(Long companyId) {
        CompanyInfo companyInfo = iCompanyInfoService.getById(companyId);
        String oldStatus = companyInfo.getStatus();
        Assert.isTrue(oldStatus.equals(ApproveStatusType.SUBMITTED.getValue()), "??????????????????????????????????????????");

        // ???????????????????????????????????????
        iOrgCategoryService.remove(Wrappers.lambdaQuery(OrgCategory.class).eq(OrgCategory::getCompanyId, companyId));

        // ???????????????????????????DRAFT
        companyInfo.setStatus(ApproveStatusType.DRAFT.getValue());
        companyInfo.setStatusName(ApproveStatusType.DRAFT.getName());
        iCompanyInfoService.updateById(companyInfo);
    }
}
