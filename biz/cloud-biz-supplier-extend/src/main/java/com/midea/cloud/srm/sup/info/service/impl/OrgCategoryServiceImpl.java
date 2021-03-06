package com.midea.cloud.srm.sup.info.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.ImportStatus;
import com.midea.cloud.common.enums.review.CategoryStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationRelation;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.change.entity.OrgCategoryChange;
import com.midea.cloud.srm.model.supplier.info.dto.OrgCategoryImportDro;
import com.midea.cloud.srm.model.supplier.info.dto.OrgCategoryQueryDTO;
import com.midea.cloud.srm.model.supplier.info.dto.OrgCategorySaveDTO;
import com.midea.cloud.srm.model.supplier.info.dto.VendorDto;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.OrgCategory;
import com.midea.cloud.srm.model.supplierauth.review.entity.ReviewForm;
import com.midea.cloud.srm.sup.change.service.IOrgCategoryChangeService;
import com.midea.cloud.srm.sup.dim.service.IDimFieldContextService;
import com.midea.cloud.srm.sup.info.mapper.OrgCategoryMapper;
import com.midea.cloud.srm.sup.info.service.ICompanyInfoService;
import com.midea.cloud.srm.sup.info.service.IOrgCategoryService;
import com.midea.cloud.srm.sup.vendororgcategory.mapper.VendorOrgCateRelMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ???????????????(???????????????)   ???????????????
 * </pre>
 *
 * @author zhuwl7@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-03-02 16:21:46
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class OrgCategoryServiceImpl extends ServiceImpl<OrgCategoryMapper, OrgCategory> implements IOrgCategoryService {
    @Autowired
    private IDimFieldContextService dimFieldContextService;

    @Resource
    private OrgCategoryMapper orgCategoryMapper;

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private IOrgCategoryChangeService iOrgCategoryChangeService;

    @Resource
    private SupplierClient supplierClient;

    @Resource
    private FileCenterClient fileCenterClient;

    @Autowired
    private ICompanyInfoService iCompanyInfoService;

    @Resource
    private VendorOrgCateRelMapper vendorOrgCateRelDao;

    @Override
    public List<VendorDto> queryCompanyByBusinessModeCode(List<Long> categoryIds) {
//        // ??????????????????????????????
//        String categoryCode = "";
//        String companyType = "";
        List<VendorDto> validVendorOrgCate = null;
//        if(BusinessMode.INSIDE.getValue().equals(businessModeCode)){
//            categoryCode = "6001";
//            companyType = CompanyType.INSIDE.getValue();
//        }else if(BusinessMode.OUTSIDE.getValue().equals(businessModeCode)){
//            categoryCode = "6002";
//            companyType = CompanyType.OUTSIDE.getValue();
//        }else {
//            companyType = CompanyType.OVERSEA.getValue();
//            categoryCode = "6003";
//        }

//        List<Long> categoryIds = new ArrayList<>();
        // ????????????????????????
//        List<PurchaseCategory> purchaseCategories = baseClient.queryPurchaseCategoryByMiddleCode(categoryCode);
//        if(CollectionUtils.isNotEmpty(purchaseCategories)){
//            purchaseCategories.forEach(purchaseCategory -> {
//                categoryIds.add(purchaseCategory.getCategoryId());
//            });
//            // ???????????????????????????
//            if (CollectionUtils.isNotEmpty(categoryIds)) {
//                // ?????? ??????????????????????????????
//                validVendorOrgCate = this.baseMapper.findValidVendorOrgCate(categoryIds);
//                if(CollectionUtils.isNotEmpty(validVendorOrgCate)){
//                    String finalCompanyType = companyType;
//                    validVendorOrgCate.forEach(vendorDto -> {
//                        vendorDto.setCompanyType(finalCompanyType);
//                    });
//                }
//            }
//
//        }
        // ???????????????????????????
        if (CollectionUtils.isNotEmpty(categoryIds)) {
            // ?????? ??????????????????????????????
            validVendorOrgCate = this.baseMapper.findValidVendorOrgCate(categoryIds);
            // ???????????????????????????????????????????????????

        }
        return validVendorOrgCate;
    }

    @Override
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload) throws Exception {
        // ??????????????????
        EasyExcelUtil.checkParam(file, fileupload);
        // ????????????
        List<OrgCategoryImportDro> orgCategoryImportDros = readData(file);
        // ???????????????????????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ???????????????
        List<OrgCategory> orgCategoriesUpdate = new ArrayList<>();
        List<OrgCategory> orgCategoriesAdd = new ArrayList<>();
        // ??????????????????
        List<String> errorList = new ArrayList<>();
        getImportData(orgCategoriesUpdate,orgCategoriesAdd ,orgCategoryImportDros, errorList, errorFlag);
        if (errorFlag.get()) {
            // ?????????
            fileupload.setFileSourceName("???????????????????????????????????????");
            Fileupload fileupload1 = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    orgCategoryImportDros, OrgCategoryImportDro.class, file.getName(), file.getOriginalFilename(), file.getContentType());
            return ImportStatus.importError(fileupload1.getFileuploadId(),fileupload1.getFileSourceName());
        }else {
            if(CollectionUtils.isNotEmpty(orgCategoriesAdd)){
                log.info("----------------------------?????????????????????????????????????????????----------------------------------");
                this.saveBatch(orgCategoriesAdd);
                log.info("----------------------------?????????????????????????????????????????????----------------------------------");
            }
            if(CollectionUtils.isNotEmpty(orgCategoriesUpdate)){
                log.info("----------------------------?????????????????????????????????????????????----------------------------------");
                this.updateBatchById(orgCategoriesUpdate);
                log.info("----------------------------?????????????????????????????????????????????----------------------------------");
            }
        }
        return ImportStatus.importSuccess();
    }

    public List<OrgCategoryImportDro> readData(MultipartFile file) {
        List<OrgCategoryImportDro> orgCategoryImportDros = new ArrayList<>();
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<OrgCategoryImportDro> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream,listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(OrgCategoryImportDro.class).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            orgCategoryImportDros = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return orgCategoryImportDros;
    }

    @Override
    public List<OrgCategory> listForCheck(OrgCategoryQueryDTO orgCategoryQueryDTO) {
        return orgCategoryMapper.listForCheck(orgCategoryQueryDTO);
    }

    /**
     * ???????????????
     * @param orgCategory
     * @return
     */
    @Override
    public List<OrgCategory> listOrgCategoryByParam(OrgCategory orgCategory) {
        return orgCategoryMapper.selectList(new QueryWrapper<>(orgCategory));
    }

    @Override
    public List<Organization> supplierTree(Organization organization) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (loginAppUser == null) {
            return Collections.emptyList();
        }
        Long companyId = loginAppUser.getCompanyId();
        if (companyId == null) {
            return Collections.emptyList();
        }

        // ???????????????????????????????????????????????????????????????????????????????????????????????????
        if (!"OU".equals(organization.getOrganizationTypeCode()) && !"INV".equals(organization.getOrganizationTypeCode())) {
            return Collections.emptyList();
        }

        // ?????????????????????????????????
        OrgCategory orgCategory = new OrgCategory();
        orgCategory.setCompanyId(companyId);
        List<OrgCategory> orgCategoryList = this.listOrgCategoryByParam(orgCategory);
        List<Long> organizationIdList = orgCategoryList.stream().map(OrgCategory::getOrgId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(organizationIdList)) {
            return Collections.emptyList();
        }

        // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????parent??????????????????
        if ("OU".equals(organization.getOrganizationTypeCode())) { // ????????????
            organization.setOrganizationIdList(organizationIdList);

            organization.setParentOrganizationIds(null);
            return baseClient.listOrganization(organization);
        } else if ("INV".equals(organization.getOrganizationTypeCode())) { // ????????????
            String parentOrganizationIds = organization.getParentOrganizationIds();

            if (StringUtils.isNotBlank(parentOrganizationIds)) {
                Long parentOrganizationId = Long.parseLong(parentOrganizationIds);
                if (!organizationIdList.contains(parentOrganizationId)) { // ?????????????????????????????????????????????????????????????????????
                    return Collections.emptyList();
                } else {
                    List<OrganizationRelation> relationList = baseClient.listChildrenOrganization(parentOrganizationId);
                    List<Long> invOrganizationIdList = relationList.stream().map(OrganizationRelation::getOrganizationId).collect(Collectors.toList());
                    organization.setOrganizationIdList(invOrganizationIdList);

                    organization.setParentOrganizationIds(null);
                    return baseClient.listOrganization(organization);
                }
            } else { // ?????????????????????????????????????????????????????????
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * ?????????????????????, ????????????????????????
     *
     * @param orgCategoriesUpdate
     * @param orgCategoriesAdd
     * @param errorList
     * @throws IOException
     * @throws ParseException
     */
    private void getImportData(List<OrgCategory> orgCategoriesUpdate,List<OrgCategory> orgCategoriesAdd, List<OrgCategoryImportDro> orgCategoryImportDros, List<String> errorList, AtomicBoolean errorFlag) throws IOException, ParseException {
        if(CollectionUtils.isNotEmpty(orgCategoryImportDros)){
            // ??????????????????
            List<String> orgName = new ArrayList<>();
            // ???SRM?????????ID
            List<Long> vendorIdList = new ArrayList<>();
            // ???????????????
            List<String> categoryCodeList = new ArrayList<>();
            for(OrgCategoryImportDro orgCategoryImportDro : orgCategoryImportDros) {
                String orgName1 = orgCategoryImportDro.getOrgName();
                if(StringUtil.notEmpty(orgName1)){
                    orgName1 = orgName1.trim();
                    orgName.add(orgName1);
                }
                String categoryCode3 = orgCategoryImportDro.getCategoryCode3();
                if(StringUtil.notEmpty(categoryCode3)){
                    categoryCode3 = categoryCode3.trim();
                    categoryCodeList.add(categoryCode3);
                }
                String vendorId = orgCategoryImportDro.getVendorId();
                if(StringUtil.notEmpty(vendorId)){
                    vendorId = vendorId.trim();
                    Long id = Long.parseLong(vendorId);
                    vendorIdList.add(id);
                }
            }

            // ?????????????????????
            Map<String, Organization> orgMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(orgName)) {
                orgName = orgName.stream().distinct().collect(Collectors.toList());
                List<Organization> orgList = baseClient.getOrganizationByNameList(orgName);
                if (CollectionUtils.isNotEmpty(orgList)) {
                    orgMap = orgList.stream().filter(organization -> StringUtil.notEmpty(organization.getOrganizationName())).
                            collect(Collectors.toMap(Organization::getOrganizationName,v->v,(k1,k2)->k1));
                }
            }

            // ????????????????????????id
            Map<Long, List<CompanyInfo>> companyMap = new HashMap<>();
            List<CompanyInfo> companyInfos = null;
            if (CollectionUtils.isNotEmpty(vendorIdList)) {
                vendorIdList = vendorIdList.stream().distinct().collect(Collectors.toList());
                companyInfos = iCompanyInfoService.list(new QueryWrapper<CompanyInfo>().in("COMPANY_ID",vendorIdList));
            }
            if (CollectionUtils.isNotEmpty(companyInfos)) {
                companyMap = companyInfos.stream().collect(Collectors.groupingBy(CompanyInfo::getCompanyId));
            }

            // ????????????????????????
            Map<String, PurchaseCategory> categoriesMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(categoryCodeList)) {
                categoryCodeList = categoryCodeList.stream().distinct().collect(Collectors.toList());
                categoriesMap = baseClient.queryPurchaseCategoryByLevelCodes(categoryCodeList);
            }

            // ?????????????????????????????????????????????
            Map<String, OrgCategory> orgCategoryMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(vendorIdList) && CollectionUtils.isNotEmpty(orgName) && CollectionUtils.isNotEmpty(categoryCodeList)) {
                QueryWrapper<OrgCategory> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("COMPANY_ID",vendorIdList);
                queryWrapper.in("ORG_NAME",orgName);
                queryWrapper.in("CATEGORY_CODE",categoryCodeList);
                List<OrgCategory> orgCategories = this.list(queryWrapper);
                if(CollectionUtils.isNotEmpty(orgCategories)){
                    orgCategoryMap = orgCategories.stream().filter(orgCategory -> StringUtil.notEmpty(orgCategory.getCompanyId())
                            && StringUtil.notEmpty(orgCategory.getOrgName()) && StringUtil.notEmpty(orgCategory.getCategoryCode())).
                            collect(Collectors.toMap(k -> k.getCompanyId() + k.getCategoryCode() + k.getOrgName(), v -> v, (k1, k2) -> k1));
                }
            }

            // ?????????
            Map<String, String> dicValueKey = getDicValueKey();
            HashSet<String> hashSet = new HashSet<>();
            int i = 1;
            // ???????????????????????????,???2?????????,????????????????????????1
            for (OrgCategoryImportDro orgCategoryImportDro : orgCategoryImportDros) {
                // <------------------------------------????????????????????????????????????-------------------------------------------->
                log.info("???"+i+"?????????");
                i++;
                // ????????????
                StringBuffer errorMsg = new StringBuffer();
                OrgCategory orgCategory = new OrgCategory();
                StringBuffer onlyKey = new StringBuffer();
                boolean lineErrorFlag = true;

                // ???SRM?????????ID
                String vendorId = orgCategoryImportDro.getVendorId();
                if (StringUtil.notEmpty(vendorId)) {
                    vendorId = vendorId.trim();
                    onlyKey.append(vendorId);
                    Long id = 0L;
                    try {
                        id = Long.parseLong(vendorId);
                        if (null != companyMap.get(id)) {
                            CompanyInfo companyInfo = companyMap.get(id).get(0);
                            orgCategory.setCompanyId(companyInfo.getCompanyId());
                            orgCategory.setCompanyCode(companyInfo.getCompanyCode());
                            orgCategory.setCompanyName(companyInfo.getCompanyName());
                        } else {
                            errorFlag.set(true);
                            lineErrorFlag = false;
                            errorMsg.append("???SRM?????????ID?????????; ");
                        }
                    } catch (Exception e) {
                        errorFlag.set(true);
                        lineErrorFlag = false;
                        errorMsg.append("???SRM?????????ID?????????; ");
                    }
                }else {
                    errorFlag.set(true);
                    lineErrorFlag = false;
                    errorMsg.append("???SRM?????????ID????????????; ");
                }

                // ????????????
                String categoryCode3 = orgCategoryImportDro.getCategoryCode3();
                if (StringUtil.notEmpty(categoryCode3)) {
                    categoryCode3 = categoryCode3.trim();
                    onlyKey.append(categoryCode3);
                    if(null != categoriesMap.get(categoryCode3)){
                        PurchaseCategory purchaseCategory = categoriesMap.get(categoryCode3);
                        orgCategory.setCategoryId(purchaseCategory.getCategoryId());
                        orgCategory.setCategoryName(purchaseCategory.getCategoryName());
                        orgCategory.setCategoryCode(purchaseCategory.getCategoryCode());
                        orgCategory.setCategoryFullId(purchaseCategory.getStruct());
                        orgCategory.setCategoryFullName(purchaseCategory.getCategoryFullName());
                    }else {
                        errorFlag.set(true);
                        lineErrorFlag = false;
                        errorMsg.append("??????????????????????????????; ");
                    }
                } else {
                    errorFlag.set(true);
                    lineErrorFlag = false;
                    errorMsg.append("????????????????????????; ");
                }

                // ???????????????????????????????????????
                String theLifeCycle = orgCategoryImportDro.getTheLifeCycle();
                if(StringUtil.notEmpty(theLifeCycle)){
                    theLifeCycle = theLifeCycle.trim();
                    if(StringUtil.isEmpty(dicValueKey.get(theLifeCycle))){
                        errorFlag.set(true);
                        lineErrorFlag = false;
                        errorMsg.append("?????????????????????????????????????????????????????????; ");
                    }else {
                        theLifeCycle = dicValueKey.get(theLifeCycle);
                    }
                }

                // ????????????
                String orgName1 = orgCategoryImportDro.getOrgName();
                if (StringUtil.notEmpty(orgName1)){
                    orgName1 = orgName1.trim();
                    onlyKey.append(orgName1);
                    Organization organization = orgMap.get(orgName1);
                    if (null != organization) {
                        orgCategory.setOrgId(organization.getOrganizationId());
                        orgCategory.setOrgCode(organization.getOrganizationCode());
                        orgCategory.setOrgName(organization.getOrganizationName());
                    }else {
                        errorFlag.set(true);
                        lineErrorFlag = false;
                        errorMsg.append("??????????????????????????????; ");
                    }
                }else {
                    errorFlag.set(true);
                    lineErrorFlag = false;
                    errorMsg.append("????????????????????????; ");
                }

                if(!hashSet.add(onlyKey.toString())){
                    errorFlag.set(true);
                    lineErrorFlag = false;
                    errorMsg.append("?????????+??????+?????????????????????; ");
                }

                // ??????
                String status = orgCategoryImportDro.getServiceStatus();
                if (StringUtil.notEmpty(status)) {
                    status = status.trim();
                    if("SRM????????????".equals(status)){
                        if(StringUtil.notEmpty(theLifeCycle)){
                            orgCategory.setServiceStatus(theLifeCycle);
                        }else {
                            lineErrorFlag = false;
                        }
                    }else {
                        if(StringUtil.notEmpty(dicValueKey.get(status))){
                            orgCategory.setServiceStatus(dicValueKey.get(status));
                        }else {
                            errorFlag.set(true);
                            lineErrorFlag = false;
                            errorMsg.append("????????????????????????; ");
                        }
                    }
                }else {
                    lineErrorFlag = false;
                }

                // ???????????????????????????
                if(lineErrorFlag){
                    StringBuffer key = new StringBuffer();
                    key.append(orgCategory.getCompanyId()).append(orgCategory.getCategoryCode()).append(orgCategory.getOrgName());
                    OrgCategory category = orgCategoryMap.get(key.toString());
                    if(null != category){
                        category.setServiceStatus(orgCategory.getServiceStatus());
                        orgCategoriesUpdate.add(category);
                    }else {
                        orgCategory.setOrgCategoryId(IdGenrator.generate());
                        orgCategoriesAdd.add(orgCategory);
                    }
                }

                if(errorMsg.length() > 0){
                    orgCategoryImportDro.setErrorMsg(errorMsg.toString());
                }else {
                    orgCategoryImportDro.setErrorMsg(null);
                }
            }
        }

    }

    public static final List<String> statusList;
    static {
        statusList = new ArrayList<>();
        statusList.addAll(Arrays.asList("??????","??????","??????","??????","SRM????????????"));
    }

    public Map<String,String> getDicValueKey(){
        HashMap<String, String> map = new HashMap<>();
        map.put("??????","REGISTERED");
        map.put("??????","VERIFY");
        map.put("??????","ONE_TIME");
        map.put("??????","GREEN");
        return map;
    }

    @Override
    @Transactional
    public void saveOrUpdateOrgCategory(OrgCategory orgCategory, Long companyId) {
        orgCategory.setCompanyId(companyId);
        if (orgCategory.getOrgCategoryId() != null) {
            orgCategory.setLastUpdateDate(new Date());
        } else {
            orgCategory.setCreationDate(new Date());
            Long id = IdGenrator.generate();
            orgCategory.setOrgCategoryId(id);
        }
        this.saveOrUpdate(orgCategory);
//        if(null != orgCategory.getDimFieldContexts() && !CollectionUtils.isEmpty(orgCategory.getDimFieldContexts())){
//            dimFieldContextService.saveOrUpdateList(orgCategory.getDimFieldContexts(),orgCategory.getOrgCategoryId(),companyId);
//        }
    }

    @Override
    public List<OrgCategory> getByCompanyId(Long companyId) {
        QueryWrapper<OrgCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("COMPANY_ID", companyId);
        List<OrgCategory> orgCategorys = companyId != null ? this.list(queryWrapper) : null;
//        ceea,???????????????
//        if(!CollectionUtils.isEmpty(orgCategorys)) {
//            for(OrgCategory orgCategory:orgCategorys){
//                Map<String,Object> dimFieldContexts = dimFieldContextService.findByOrderId(orgCategory.getOrgCategoryId());
//                orgCategory.setDimFieldContexts(dimFieldContexts);
//            }
//
//        }
        return orgCategorys;
    }

    @Override
    public void removeByCompanyId(Long companyId) {
        QueryWrapper<OrgCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("COMPANY_ID", companyId);
        this.remove(wrapper);
    }

    @Override
    public List<OrgCategory> listOrgCategoryByCompanyId(Long companyId) {
        Assert.notNull(companyId, "companyId????????????");
        QueryWrapper<OrgCategory> queryWrapper = new QueryWrapper<>(new OrgCategory().setCompanyId(companyId));
        return orgCategoryMapper.selectList(queryWrapper);
    }

    @Override
    public List<OrgCategory> listOrgCategoryByServiceStatusAndCompanyId(Long companyId, String... serviceStatus) {
        Assert.notEmpty(serviceStatus, "serviceStatus????????????");
        Assert.notNull(companyId, "companyId????????????");
        OrgCategory orgCategory = new OrgCategory().setCompanyId(companyId);
        //????????????????????????????????????
//        if (serviceStatus.length == 1) {
//            orgCategory.setServiceStatus(serviceStatus[0]);
//        }
        

        QueryWrapper<OrgCategory> queryWrapper = new QueryWrapper<>(orgCategory);
        queryWrapper.in("SERVICE_STATUS", serviceStatus);
//        if (serviceStatus.length == 2) {
//            queryWrapper.eq("SERVICE_STATUS", serviceStatus[0])
//                    .or().eq("SERVICE_STATUS", serviceStatus[1]);
//        }
        return orgCategoryMapper.selectList(queryWrapper);
    }

    @Override
    public OrgCategory getByCategoryIdAndOrgIdAndCompanyId(Long categoryId, Long orgId, Long companyId) {
        QueryWrapper<OrgCategory> queryWrapper = new QueryWrapper<>(new OrgCategory().setCategoryId(categoryId)
                .setOrgId(orgId).setCompanyId(companyId));
        List<OrgCategory> orgCategories = this.list(queryWrapper);
        OrgCategory orgCategory = null;
        if (!CollectionUtils.isEmpty(orgCategories)) {
            orgCategory = orgCategories.get(0);
        }
        return orgCategory;
    }

    @Override
    public List<OrgCategory> getByCategoryIdAndCompanyId(Long categoryId, Long companyId) {
        QueryWrapper<OrgCategory> wrapper = new QueryWrapper<>(new OrgCategory().setCategoryId(categoryId).setCompanyId(companyId));
        wrapper.eq("COMPANY_ID", companyId);
        wrapper.eq("CATEGORY_ID", categoryId);
        wrapper.groupBy("SERVICE_STATUS");
        return this.list(wrapper);
    }

    /**
     * @return
     */
    @Override
    public List<OrgCategory> getByCategoryAll() {
        QueryWrapper<OrgCategory> wrapper = new QueryWrapper<>();
        wrapper.select("ORG_CATEGORY_ID,COMPANY_ID,CATEGORY_ID,SERVICE_STATUS");
        wrapper.ne("SERVICE_STATUS", "");
        wrapper.isNotNull("SERVICE_STATUS");
        wrapper.groupBy("SERVICE_STATUS");
        return this.list(wrapper);
    }

    @Override
    public void updateOrgCategoryServiceStatus(OrgCategory orgCategory) {
        UpdateWrapper<OrgCategory> updateWrapper = new UpdateWrapper<>(new OrgCategory().setCategoryId(orgCategory.getCategoryId())
                .setOrgId(orgCategory.getOrgId()).setCompanyId(orgCategory.getCompanyId()));
        updateWrapper.set("END_DATE", orgCategory.getEndDate()).set("START_DATE", orgCategory.getStartDate());
        this.update(orgCategory, updateWrapper);
    }

    @Override
    public boolean haveSupplier(OrgCategory orgCategory) {
        QueryWrapper<OrgCategory> queryWrapper = new QueryWrapper<>(new OrgCategory()
                .setOrgId(orgCategory.getOrgId())
                .setCategoryId(orgCategory.getCategoryId()));
        queryWrapper.ne("SERVICE_STATUS", CategoryStatus.RED.name());
        int count = this.count(queryWrapper);
        if (count > 0) {
            //??????????????????
            return true;
        } else {
            //??????????????????
            return false;
        }
    }

    @Override
    public PageInfo<OrgCategory> listPageOrgCategoryByParam(OrgCategoryQueryDTO orgCategoryQueryDTO) {
        PageUtil.startPage(orgCategoryQueryDTO.getPageNum(), orgCategoryQueryDTO.getPageSize());
//        List<PurchaseCategory> purchaseCategories = orgCategoryQueryDTO.getPurchaseCategories();
//        // ???????????????????????????
//        List<PurchaseCategory> categories = baseClient.queryMinLevelCategory(purchaseCategories);
//        // ??????????????????
//        List<OrgCategory> orgCategories = new ArrayList<>();
//        if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(categories)){
//            PageUtil.startPage(orgCategoryQueryDTO.getPageNum(), orgCategoryQueryDTO.getPageSize());
//            List<Long> longs = new ArrayList<>();
//            categories.forEach(temp->longs.add(temp.getCategoryId()));
//        }
        List<OrgCategory> orgCategories = this.baseMapper.listPageOrgCategoryByParam(orgCategoryQueryDTO);
        // ?????????????????????
        if(CollectionUtils.isNotEmpty(orgCategories)){
            List<Long> ids = new ArrayList<>();
            orgCategories.forEach(orgCategory -> {
                Long categoryId = orgCategory.getCategoryId();
                if (StringUtil.notEmpty(categoryId)) {
                    ids.add(categoryId);
                }
            });
            if (CollectionUtils.isNotEmpty(ids)) {
                Map<String, String> idMap = baseClient.queryCategoryFullNameByLevelIds(ids);
                orgCategories.forEach(orgCategory -> {
                    Long categoryId = orgCategory.getCategoryId();
                    if (StringUtil.notEmpty(categoryId)) {
                        String s = idMap.get(String.valueOf(categoryId));
                        orgCategory.setCategoryFullName(s);
                    }
                });
            }

        }
        return new PageInfo<OrgCategory>(orgCategories);
    }

    @Override
    @Transactional
    public void collectOrgCategory(OrgCategorySaveDTO orgCategorySaveDTO) {
        OrgCategory orgCategory = orgCategorySaveDTO.getOrgCategory();
        ReviewForm reviewForm = orgCategorySaveDTO.getReviewForm();
        List<OrgCategoryChange> orgCategoryChanges = orgCategorySaveDTO.getOrgCategoryChanges();
        Assert.notNull(orgCategory, LocaleHandler.getLocaleMsg("??????????????????????????????"));
        Assert.notEmpty(orgCategoryChanges, LocaleHandler.getLocaleMsg("??????????????????????????????????????????"));
        //?????????????????????????????????
        Long orgId = orgCategory.getOrgId();
        Long categoryId = orgCategory.getCategoryId();
        Long companyId = orgCategory.getCompanyId();
        String afterServiceStatus = orgCategory.getServiceStatus();
        OrgCategory onlyOrgCategory = this.getByCategoryIdAndOrgIdAndCompanyId(categoryId, orgId, companyId);
        String beforeServiceStatus = "";
        log.info("------------------??????????????????????????????????????????,????????????????????????:" + JsonUtil.entityToJsonStr(orgCategorySaveDTO) + "-------------------------------");
        //?????????????????????????????????
        if (onlyOrgCategory == null) {
            orgCategory.setOrgCategoryId(IdGenrator.generate());
            //??????????????????????????????????????????????????????
            if (reviewForm != null) {
                orgCategory.setCreatedBy(reviewForm.getCreatedBy());
                orgCategory.setCreatedId(reviewForm.getCreatedId());
                orgCategory.setCreatedByIp(reviewForm.getCreatedByIp());
                orgCategory.setLastUpdateDate(reviewForm.getLastUpdateDate());
                orgCategory.setLastUpdatedBy(reviewForm.getLastUpdatedBy());
                orgCategory.setLastUpdatedByIp(reviewForm.getLastUpdatedByIp());
                orgCategory.setLastUpdatedId(reviewForm.getLastUpdatedId());
            }
            this.save(orgCategory);
            //???????????????????????? ToDo ??????????????????????????????????????????
//            for (OrgCategoryChange orgCategoryChange : orgCategoryChanges) {
//                saveOrgCategoryChange(orgId, categoryId, companyId, beforeServiceStatus, afterServiceStatus, orgCategoryId, orgCategoryChange);
//            }
        } else {
            beforeServiceStatus = onlyOrgCategory.getServiceStatus();
            //??????????????????????????????????????????????????????
            if (reviewForm != null) {
                onlyOrgCategory.setCreatedByIp(reviewForm.getCreatedByIp());
                onlyOrgCategory.setCreatedBy(reviewForm.getCreatedBy());
                onlyOrgCategory.setCreatedId(reviewForm.getCreatedId());
                onlyOrgCategory.setLastUpdatedId(reviewForm.getLastUpdatedId());
                onlyOrgCategory.setLastUpdatedByIp(reviewForm.getLastUpdatedByIp());
                onlyOrgCategory.setLastUpdateDate(reviewForm.getLastUpdateDate());
                onlyOrgCategory.setLastUpdatedBy(reviewForm.getLastUpdatedBy());
            }
            onlyOrgCategory.setServiceStatus(afterServiceStatus);
            this.updateById(onlyOrgCategory);
            //???????????????????????? ToDo ?????????????????????????????? ?????????:???????????????????????????????
//            for (OrgCategoryChange orgCategoryChange : orgCategoryChanges) {
//                OrgCategoryChange onlyOrgCategoryChange = iOrgCategoryChangeService.getOne(new QueryWrapper<>(new OrgCategoryChange()
//                        .setOrgCategoryId(onlyOrgCategory.getOrgCategoryId())
//                        .setOrgId(onlyOrgCategory.getOrgId())
//                        .setCategoryId(onlyOrgCategory.getCategoryId())
//                        .setCompanyId(onlyOrgCategory.getCompanyId())
//                        .setFormType(orgCategoryChange.getFormType())
//                        .setFormNum(orgCategoryChange.getFormNum())));
//                QueryWrapper<OrgCategoryChange> queryWrapper = new QueryWrapper<>(new OrgCategoryChange()
//                        .setOrgCategoryId(onlyOrgCategory.getOrgCategoryId())
//                        .setCompanyId(onlyOrgCategory.getCompanyId())
//                        .setOrgId(onlyOrgCategory.getOrgId())
//                        .setCategoryId(onlyOrgCategory.getCategoryId()));
//                queryWrapper.orderByDesc("LAST_UPDATE_DATE");
//                List<OrgCategoryChange> list = iOrgCategoryChangeService.list(queryWrapper);
//                OrgCategoryChange onlyOrgCategoryChange = null;
//                if (!CollectionUtils.isEmpty(list)) {
//                    onlyOrgCategoryChange = list.get(0);
//                }
//                if (onlyOrgCategoryChange != null) {
//                    String beforeServiceStatus = onlyOrgCategoryChange.getAfterServiceStatus();
//                    iOrgCategoryChangeService.updateById(onlyOrgCategoryChange
//                            .setBeforeServiceStatus(beforeServiceStatus)
//                            .setAfterServiceStatus(afterServiceStatus));
//                } else {
//                    saveOrgCategoryChange(orgId, categoryId, companyId, beforeServiceStatus, afterServiceStatus, orgCategoryId, orgCategoryChange);
//                }
//            }
        }
    }

    private void saveOrgCategoryChange(Long orgId, Long categoryId, Long companyId, String beforeServiceStatus, String afterServiceStatus, long orgCategoryId, OrgCategoryChange orgCategoryChange) {
        iOrgCategoryChangeService.save(orgCategoryChange.setOrgCategoryChangeId(IdGenrator.generate())
                .setOrgCategoryId(orgCategoryId)
                .setOrgId(orgId)
                .setCategoryId(categoryId)
                .setCompanyId(companyId)
                .setAfterServiceStatus(afterServiceStatus)
                .setFormType(orgCategoryChange.getFormType())
                .setFormNum(orgCategoryChange.getFormNum()));
    }

    @Override
    public List<OrgCategory> querySingleSourceList(Long vendorId) {
        return this.baseMapper.querySingleSourceList(vendorId);
    }

    @Override
    @Transactional
    public void delete(Long orgCategoryId) {
        this.delete(orgCategoryId);
    }

    /**
     * ????????????id?????????????????????????????????????????????????????????????????????
     *
     * @param categoryIds
     * @return
     */
    @Override
    public List<CompanyInfo> listCompanyInfosByCategoryIds(List<Long> categoryIds) {
        // ?????????categoryIds??????
        List<Long> duplicateRemoveCategoryIds = categoryIds.stream().distinct().collect(Collectors.toList());

        List<Long> resCompanyIds = new ArrayList<>();
        // ??????categoryIds?????????????????????????????? Controller?????????????????????
        List<OrgCategory> filterOrgCategories = this.list(Wrappers.lambdaQuery(OrgCategory.class)
                .eq(OrgCategory::getServiceStatus, CategoryStatus.GREEN.name())
                .in(OrgCategory::getCategoryId, duplicateRemoveCategoryIds)
        );
        if (CollectionUtils.isNotEmpty(filterOrgCategories)) {
            Map<Long, List<OrgCategory>> map = filterOrgCategories.stream()
                    .collect(Collectors.groupingBy(OrgCategory::getCompanyId));
            map.forEach((companyId, orgs) -> {
                Map<Long, List<OrgCategory>> temp = orgs.stream().collect(Collectors.groupingBy(OrgCategory::getCategoryId));
                boolean allGreen = true;
                for (Map.Entry<Long, List<OrgCategory>> entry : temp.entrySet()) {
                    allGreen = allGreen && entry.getValue().stream().anyMatch(e -> Objects.equals(CategoryStatus.GREEN.name(), e.getServiceStatus()));
                }
                if (allGreen) {
                    resCompanyIds.add(companyId);
                }
            });

        }
        // ????????????????????????companyIds
        return CollectionUtils.isNotEmpty(resCompanyIds) ? iCompanyInfoService.listByIds(resCompanyIds) : new ArrayList<>();
    }
}
