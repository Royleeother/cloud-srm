package com.midea.cloud.srm.sup.info.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.DictionaryValue;
import com.midea.cloud.common.constants.OpConstant;
import com.midea.cloud.common.constants.RepushConst;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.*;
import com.midea.cloud.common.enums.pm.po.PurchaseOrderEnum;
import com.midea.cloud.common.enums.review.CategoryStatus;
import com.midea.cloud.common.enums.review.FormType;
import com.midea.cloud.common.enums.sup.SupplierListStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.repush.service.RepushHandlerService;
import com.midea.cloud.srm.feign.api.ApiClient;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.inq.InqClient;
import com.midea.cloud.srm.feign.perf.PerformanceClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.supcooperate.SupcooperateClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.organization.dto.OrganizationMapDto;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationRelation;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationUser;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCurrency;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseTax;
import com.midea.cloud.srm.model.base.questionairesurvey.dto.VendorDTO;
import com.midea.cloud.srm.model.base.region.dto.AreaDTO;
import com.midea.cloud.srm.model.base.region.dto.AreaPramDTO;
import com.midea.cloud.srm.model.base.region.dto.CityParamDto;
import com.midea.cloud.srm.model.base.repush.entity.RepushStatus;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.role.entity.Role;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.model.supplier.change.dto.ChangeInfoDTO;
import com.midea.cloud.srm.model.supplier.change.entity.*;
import com.midea.cloud.srm.model.supplier.info.dto.*;
import com.midea.cloud.srm.model.supplier.info.entity.*;
import com.midea.cloud.srm.model.supplier.quest.dto.QuestSupplierDto;
import com.midea.cloud.srm.model.supplier.quest.enums.QuestSupplierApproveStatusEnum;
import com.midea.cloud.srm.model.supplier.quest.vo.QuestSupplierVo;
import com.midea.cloud.srm.model.supplier.responsibility.entity.SupplierLeader;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorBank.VendorBankOutputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorContact.VendorContactOutputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorInfo.VendorInfoOutputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorSite.VendorSiteOutputParameters;
import com.midea.cloud.srm.model.supplier.statuslog.entity.CompanyStatusLog;
import com.midea.cloud.srm.model.supplier.vendorimport.entity.VendorImport;
import com.midea.cloud.srm.model.suppliercooperate.deliverynote.entry.DeliveryNote;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import com.midea.cloud.srm.sup.dim.service.IDimFieldContextService;
import com.midea.cloud.srm.sup.info.mapper.CompanyInfoMapper;
import com.midea.cloud.srm.sup.info.service.*;
import com.midea.cloud.srm.sup.info.workflow.GreenChannelFlow;
import com.midea.cloud.srm.sup.quest.service.IQuestSupplierService;
import com.midea.cloud.srm.sup.responsibility.mapper.SupplierLeaderMapper;
import com.midea.cloud.srm.sup.responsibility.service.ISupplierLeaderService;
import com.midea.cloud.srm.sup.soap.erp.vendor.service.IVendorBankService;
import com.midea.cloud.srm.sup.soap.erp.vendor.service.IVendorContactService;
import com.midea.cloud.srm.sup.soap.erp.vendor.service.IVendorInfoService;
import com.midea.cloud.srm.sup.soap.erp.vendor.service.IVendorSiteService;
import com.midea.cloud.srm.sup.statuslog.service.ICompanyStatusLogService;
import com.midea.cloud.srm.sup.vendorimport.service.IVendorImportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ???????????? ???????????????
 * </pre>
 *
 * @author zhuwl7@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-03-02 14:29:48
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class CompanyInfoServiceImpl extends ServiceImpl<CompanyInfoMapper, CompanyInfo> implements ICompanyInfoService {

    @Resource
    private CompanyInfoMapper companyInfoMapper;

    @Autowired
    private IBankInfoService bankInfoService;

    @Autowired
    private ISiteInfoService siteInfoService;

    @Autowired
    private IContactInfoService contactInfoService;

    @Autowired
    private IFinanceInfoService financeInfoService;

    @Autowired
    private IHolderInfoService holderInfoService;

    @Autowired
    private IOperationInfoService operationInfoService;

    @Autowired
    private IOrgCategoryService orgCategoryService;

    @Autowired
    private IOrgInfoService orgInfoService;

    @Autowired
    private IOtherInfoService otherInfoService;

    @Autowired
    private IHonorInfoService honorInfoService;

    @Autowired
    private IBusinessInfoService businessInfoService;

    @Autowired
    private IDimFieldContextService dimFieldContextService;

    @Autowired
    private IAttachFileService attachFileService;

    @Autowired
    private ICompanyStatusLogService companyStatusLogService;

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private RbacClient rbacClient;

    @Autowired
    private ICompanyStatusLogService iCompanyStatusLogService;

    @Autowired
    private IAttachFileService iAttachFileService;

    @Autowired
    private ICategoryRelService iCategoryRelService;

    @Resource
    private FileCenterClient fileCenterClient;

    @Autowired
    private ICompanyInfoDetailService iCompanyInfoDetailService;

    @Autowired
    private IOperationQualityService iOperationQualityService;

    @Autowired
    private IOperationEquipmentService iOperationEquipmentService;

    @Autowired
    private IOperationProductService iOperationProductService;

    @Autowired
    private IManagementInfoService iManagementInfoService;

    @Autowired
    private IManagementAttachService iManagementAttachService;

    @Autowired
    private IBankInfoService iBankInfoService;

    @Autowired
    private IContactInfoService iContactInfoService;

    @Autowired
    private IVendorInfoService iVendorInfoService;

    @Autowired
    private IVendorBankService iVendorBankService;

    @Autowired
    private IVendorSiteService iVendorSiteService;

    @Autowired
    private IVendorContactService iVendorContactService;

    @Resource
    private SupcooperateClient supcooperateClient;

    @Resource
    private InqClient inqClient;

    @Resource
    private PerformanceClient performanceClient;

    @Resource
    private ApiClient apiClient;

    @Autowired
    private ISiteInfoService iSiteInfoService;

    @Autowired
    private IOrgCategoryService iOrgCategoryService;

    @Resource
    private SupplierLeaderMapper categoryResponsibilityMapper;

    @Autowired
    private ISupplierLeaderService supplierLeaderService;

    @Autowired
    private RepushHandlerService repushHandlerService;

    @Autowired
    private IVendorImportService iVendorImportService;

    /* ???????????? */
    private final static int TO_REPUSH = 1;
    /* ??????????????? */
    private final static int NOT_REPUSH = 0;

    /* ????????? */
    private final ThreadPoolExecutor submitExecutor;
    @Autowired
    private IQuestSupplierService questSupplierService;
    @Autowired
    private GreenChannelFlow greenChannelFlow;

    public CompanyInfoServiceImpl() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        TimeUnit unit;
        BlockingQueue workQueue;
        submitExecutor = new ThreadPoolExecutor(cpuCount * 2, cpuCount * 2,
                0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
    }

    @Override
    public VendorImageDto getVendorImage(Long vendorId) throws ParseException {
        VendorImageDto vendorImageDto = new VendorImageDto();
        // ???????????????????????????
        this.setCompanyBaseInfo(vendorId, vendorImageDto);
        // ??????????????????
        this.setServiceLength(vendorId, vendorImageDto);
        // ??????????????????
        this.setSingleSourceDtos(vendorId, vendorImageDto);
        // TODO ??????????????????, ?????????????????????

        // ????????????
        this.setCategoryRelDtos(vendorId, vendorImageDto);
        // ?????????????????????
        this.setBidFrequencies(vendorId, vendorImageDto);
        // ?????????????????????
        this.setPerfOverallScoreDtos(vendorId, vendorImageDto);
        // ????????????
        this.setAssesFormDtos(vendorId, vendorImageDto);
        // ?????????
        this.setImproveFormDtos(vendorId, vendorImageDto);
        return vendorImageDto;
    }

    @Override
    @Transactional
    public void testCompany(String erpOrgId) {
        Integer pageNum = 1;
        Integer pageSize = 10;
        PageUtil.startPage(pageNum, pageSize);
        Organization organization = baseClient.getOrganizationByParam(new Organization().setErpOrgId(erpOrgId));
        QueryWrapper<SiteInfo> queryWrapper = new QueryWrapper<>(new SiteInfo().setBelongOprId(erpOrgId));
        queryWrapper.orderByAsc("SITE_INFO_ID");
        List<SiteInfo> siteInfos = iSiteInfoService.list(queryWrapper);
        for (SiteInfo siteInfo : siteInfos) {
            if (siteInfo == null) continue;
            CompanyInfo companyInfo = new CompanyInfo();
            BeanUtils.copyProperties(siteInfo, companyInfo);
            long companyId = IdGenrator.generate();
            companyInfo.setCompanyId(companyId).setStatus("APPROVED").setCompanyCode("testFssc");
            siteInfo.setCompanyId(companyId);
            iSiteInfoService.updateById(siteInfo);
            this.save(companyInfo);
            iOrgCategoryService.save(new OrgCategory()
                    .setOrgCategoryId(IdGenrator.generate())
                    .setCompanyId(companyId)
                    .setCompanyName(siteInfo.getCompanyName()).setOrgId(organization.getOrganizationId()).setOrgCode(organization.getOrganizationCode()).setOrgName(organization.getOrganizationName()).setCategoryId(0L).setCategoryCode("").setCategoryName(""));
        }
    }

    @Override
    public PageInfo<CompanyInfo> listPageByOrgId(CompanyRequestDTO companyRequestDTO) {
        List<OrgCategory> orgCategories = iOrgCategoryService.list(new QueryWrapper<>(new OrgCategory()
                .setOrgId(companyRequestDTO.getOrgId())));
        Set<Long> set = orgCategories.stream().map(OrgCategory::getCompanyId).collect(Collectors.toSet());
        List<CompanyInfo> companyInfos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(set)) {
            PageUtil.startPage(companyRequestDTO.getPageNum(), companyRequestDTO.getPageSize());
            companyInfos = this.listByIds(set);
        }
        return new PageInfo<>(companyInfos);
    }

    @Override
    public List<CompanyInfo> listAllForImport() {
        return companyInfoMapper.listAllForImport();
    }

    /**
     * ???????????????????????????(???????????????)
     * (?????????????????????????????????????????????, !!!????????????)
     * @param companyId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCompanyInfoAllData(Long companyId) {
        // ???????????????????????????
        iCompanyInfoDetailService.remove(Wrappers.lambdaQuery(CompanyInfoDetail.class).eq(CompanyInfoDetail::getCompanyId, companyId));
        // ???????????????????????????????????????
        iManagementAttachService.remove(Wrappers.lambdaQuery(ManagementAttach.class).eq(ManagementAttach::getCompanyId, companyId));
        // ??????????????????????????????
        iManagementInfoService.remove(Wrappers.lambdaQuery(ManagementInfo.class).eq(ManagementInfo::getCompanyId, companyId));
        // ???????????????????????????????????????
        iOperationEquipmentService.remove(Wrappers.lambdaQuery(OperationEquipment.class).eq(OperationEquipment::getCompanyId, companyId));
        // ????????????????????????????????????
        iOperationProductService.remove(Wrappers.lambdaQuery(OperationProduct.class).eq(OperationProduct::getCompanyId, companyId));
        // ???????????????????????????????????????
        iOperationQualityService.remove(Wrappers.lambdaQuery(OperationQuality.class).eq(OperationQuality::getCompanyId, companyId));
        // ??????supplier leader?????????
        supplierLeaderService.remove(Wrappers.lambdaQuery(SupplierLeader.class).eq(SupplierLeader::getCompanyId, companyId));
        // ??????????????????????????????
        iVendorImportService.remove(Wrappers.lambdaQuery(VendorImport.class).eq(VendorImport::getVendorId, companyId));
        // ???????????????????????????
        businessInfoService.remove(Wrappers.lambdaQuery(BusinessInfo.class).eq(BusinessInfo::getCompanyId, companyId));
        // ????????????????????????
        iCategoryRelService.remove(Wrappers.lambdaQuery(CategoryRel.class).eq(CategoryRel::getCompanyId, companyId));
        // ????????????????????????
        financeInfoService.remove(Wrappers.lambdaQuery(FinanceInfo.class).eq(FinanceInfo::getCompanyId, companyId));
        // ????????????????????????
        holderInfoService.remove(Wrappers.lambdaQuery(HolderInfo.class).eq(HolderInfo::getCompanyId, companyId));
        // ????????????????????????
        honorInfoService.remove(Wrappers.lambdaQuery(HonorInfo.class).eq(HonorInfo::getCompanyId, companyId));
        // ????????????????????????
        operationInfoService.remove(Wrappers.lambdaQuery(OperationInfo.class).eq(OperationInfo::getCompanyId, companyId));
        // ???????????????????????????
        iOrgCategoryService.remove(Wrappers.lambdaQuery(OrgCategory.class).eq(OrgCategory::getCompanyId, companyId));
        // ????????????????????????
        orgInfoService.remove(Wrappers.lambdaQuery(OrgInfo.class).eq(OrgInfo::getCompanyId, companyId));
        // ??????????????????
        otherInfoService.remove(Wrappers.lambdaQuery(OtherInfo.class).eq(OtherInfo::getCompanyId, companyId));
        // ???????????????????????????
        iBankInfoService.remove(Wrappers.lambdaQuery(BankInfo.class).eq(BankInfo::getCompanyId, companyId));
        // ???????????????????????????
        iSiteInfoService.remove(Wrappers.lambdaQuery(SiteInfo.class).eq(SiteInfo::getCompanyId, companyId));
        // ??????????????????????????????
        iContactInfoService.remove(Wrappers.lambdaQuery(ContactInfo.class).eq(ContactInfo::getCompanyId, companyId));
        // ??????????????????????????????
        iCompanyStatusLogService.remove(Wrappers.lambdaQuery(CompanyStatusLog.class).eq(CompanyStatusLog::getCompanyId, companyId));

        //???????????????????????????
        this.removeById(companyId);
    }

    @Override
    public List<VendorDTO> listCompanyInfosByStringList(List<String> list) {
        return companyInfoMapper.listCompanyInfosByStringList(list);
    }

    @Override
    public CompanyInfo getCompanyInfo(VendorDTO vendorDTO) {
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq(vendorDTO.getVendorCode()!=null,"COMPANY_CODE",vendorDTO.getVendorCode());
        return this.getOne(wrapper);
    }

    @Override
    public List<CompanyInfo> listCompanyInfosByVendorDTO(VendorDTO vendorDTO) {
        QueryWrapper wrapper = new QueryWrapper();
        PageUtil.startPage(vendorDTO.getPageNum(), vendorDTO.getPageSize());
        wrapper.isNotNull("COMPANY_CODE");
        wrapper.like(vendorDTO.getVendorName()!=null,"COMPANY_NAME",vendorDTO.getVendorName());
        wrapper.eq(vendorDTO.getVendorCode()!=null,"COMPANY_CODE",vendorDTO.getVendorCode());
        wrapper.eq(vendorDTO.getSupBusinessType()!=null,"CEEA_SUP_BUSINESS_TYPE",vendorDTO.getSupBusinessType());
        wrapper.notIn(vendorDTO.getVendorCodes() != null && vendorDTO.getVendorCodes().size() != 0,"COMPANY_CODE",vendorDTO.getVendorCodes());
        wrapper.orderByDesc("CREATION_DATE");
        return this.list(wrapper);
    }

    /**
     * ????????????????????????(????????????)
     * @param companyInfoNameList
     * @return
     */
    @Override
    public List<CompanyInfo> listVendorByNameBatch(List<String> companyInfoNameList) {
        if(CollectionUtils.isEmpty(companyInfoNameList)){
            return Collections.EMPTY_LIST;
        }
        QueryWrapper<CompanyInfo> wrapper = new QueryWrapper<>();
        wrapper.in("COMPANY_NAME",companyInfoNameList);
        return this.list(wrapper);
    }

    @Override
    public void importModelDownload(HttpServletResponse response) throws IOException {
        ArrayList<CompanyInfoModelDTO> companyInfoModelDTOS = new ArrayList<>();
        ArrayList<ContactInfoModelDTO> contactInfoModelDTOS = new ArrayList<>();
        ArrayList<EquipmentModelDTO> equipmentModelDTOS = new ArrayList<>();
        ArrayList<BankInfoModelDTO> bankInfoModelDTOS = new ArrayList<>();
        ArrayList<BusinessInfoModelDTO> businessInfoModelDTOS = new ArrayList<>();
        ArrayList<ProductModelDTO> productModelDTOS = new ArrayList<>();
        ArrayList<SiteInfoModelDTO> siteInfoModelDTOS = new ArrayList<>();
        String[] sheetNames = {"??????????????????????????????", "?????????????????????", "?????????????????????", "???????????????", "????????????", "????????????", "??????????????????", "??????????????????"};
        List<List<? extends Object>> dataLists = new ArrayList<>();
        dataLists.add(companyInfoModelDTOS);
        dataLists.add(siteInfoModelDTOS);
        dataLists.add(contactInfoModelDTOS);
        dataLists.add(bankInfoModelDTOS);
        dataLists.add(equipmentModelDTOS);
        dataLists.add(productModelDTOS);
        dataLists.add(businessInfoModelDTOS);
        Class<? extends Object> clazz[] = new Class[]{CompanyInfoModelDTO.class, SiteInfoModelDTO.class, ContactInfoModelDTO.class, BankInfoModelDTO.class, EquipmentModelDTO.class, ProductModelDTO.class, BusinessInfoModelDTO.class};
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, sheetNames[0]);
        EasyExcelUtil.writeExcelWithModel(outputStream, sheetNames, dataLists, clazz);
    }

    @Override
    @Transactional
    public Map<String, Object> importDatasExcel(MultipartFile file) throws IOException {
        long startSeconds = System.currentTimeMillis() / 1000;
        log.info("??????????????????????????????===========================================>" + startSeconds);
        // ????????????
        String filename = file.getOriginalFilename();
        if (!EasyExcelUtil.isExcel(filename)) {
            throw new BaseException("??????????????????Excel??????");
        }
        HashMap<String, Object> result = new HashMap<>();
        // ???????????????
        InputStream inputStream = file.getInputStream();
        ExcelReader excelReader = EasyExcel.read(inputStream).build();

        AnalysisEventListenerImpl<Object> sheet1Listener = new AnalysisEventListenerImpl();
        AnalysisEventListenerImpl<Object> sheet2Listener = new AnalysisEventListenerImpl();
        AnalysisEventListenerImpl<Object> sheet3Listener = new AnalysisEventListenerImpl();
        AnalysisEventListenerImpl<Object> sheet4Listener = new AnalysisEventListenerImpl();
        AnalysisEventListenerImpl<Object> sheet5Listener = new AnalysisEventListenerImpl();
        AnalysisEventListenerImpl<Object> sheet6Listener = new AnalysisEventListenerImpl();
        AnalysisEventListenerImpl<Object> sheet7Listener = new AnalysisEventListenerImpl();

        ReadSheet sheet1 = EasyExcel.readSheet(0).head(CompanyInfoModelDTO.class).registerReadListener(sheet1Listener).build();
        ReadSheet sheet2 = EasyExcel.readSheet(1).head(SiteInfoModelDTO.class).registerReadListener(sheet2Listener).build();
        ReadSheet sheet3 = EasyExcel.readSheet(2).head(ContactInfoModelDTO.class).registerReadListener(sheet3Listener).build();
        ReadSheet sheet4 = EasyExcel.readSheet(3).head(BankInfoModelDTO.class).registerReadListener(sheet4Listener).build();
        ReadSheet sheet5 = EasyExcel.readSheet(4).head(EquipmentModelDTO.class).registerReadListener(sheet5Listener).build();
        ReadSheet sheet6 = EasyExcel.readSheet(5).head(ProductModelDTO.class).registerReadListener(sheet6Listener).build();
        ReadSheet sheet7 = EasyExcel.readSheet(6).head(BusinessInfoModelDTO.class).registerReadListener(sheet7Listener).build();
        excelReader.read(sheet1, sheet2, sheet3, sheet4, sheet5, sheet6, sheet7);

        //?????????????????????
        List<Object> companyInfoModelDTOS = sheet1Listener.getDatas();
        //?????????????????????
        List<Object> siteInfoModelDTOS = sheet2Listener.getDatas();
        //???????????????
        List<Object> contactInfoModelDTOS = sheet3Listener.getDatas();
        //????????????
        List<Object> bankInfoModelDTOS = sheet4Listener.getDatas();
        //????????????
        List<Object> equipmentModelDTOS = sheet5Listener.getDatas();
        //??????????????????
        List<Object> productModelDTOS = sheet6Listener.getDatas();
        //??????????????????
        List<Object> businessInfoModelDTOS = sheet7Listener.getDatas();

        //?????????????????????dataBaseBean
        ArrayList<CompanyInfo> companyInfos = new ArrayList<>();
        //???????????????????????????dataBaseBean
        ArrayList<CompanyInfoDetail> companyInfoDetails = new ArrayList<>();
        //???????????????????????????dataBaseBean
        ArrayList<OperationInfo> operationInfos = new ArrayList<>();
        //?????????????????????Group
        ArrayList<ArrayList<SiteInfo>> siteInfoGroups = new ArrayList<>();
        //???????????????Group
        ArrayList<ArrayList<ContactInfo>> contactinfoGroups = new ArrayList<>();
        //????????????Group
        ArrayList<ArrayList<BankInfo>> bankInfoGroups = new ArrayList<>();
        //????????????Group
        ArrayList<ArrayList<OperationEquipment>> operationEquipmentGroups = new ArrayList<>();
        //??????????????????Group
        ArrayList<ArrayList<OperationProduct>> operationProductGroups = new ArrayList<>();
        //??????????????????Group
        ArrayList<ArrayList<BusinessInfo>> businessInfoGroups = new ArrayList<>();

        //??????????????????????????????
        //?????????
        List<String> dictCodes = Arrays.asList("RELATION",//???????????????
                "COMPANY_NATURE",//????????????
                "GENDER",//??????
                "BUSINESS_MODEL",//????????????
                "SUP_BUSINESS_TYPE",//?????????????????????
                "PLANT_TYPE",//????????????
                "country",//??????
                "SUPPLIER_LIST_STATUS",//?????????????????????
                "VENDOR_SITE_CODE"//???????????????
        );
        //????????????
        List<DictItemDTO> dictItemDTOS = baseClient.listByDictCode(dictCodes);
        Map<String, DictItemDTO> dictItemDTOMap = dictItemDTOS.stream().collect(Collectors.toMap(DictItemDTO::getDictItemName, Function.identity(), (key1, key2) -> key2));
        //??????
        List<PurchaseCurrency> purchaseCurrencies = baseClient.listAllPurchaseCurrency();
        Map<String, PurchaseCurrency> purchaseCurrencyMap = purchaseCurrencies.stream().collect(Collectors.toMap(PurchaseCurrency::getCurrencyName, Function.identity(), (key1, key2) -> key2));
        //????????????
        Set<String> orgNames = new HashSet<>();
        Map<String, Organization> organizationInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(siteInfoModelDTOS)) {
            siteInfoModelDTOS.forEach(s -> {
                SiteInfoModelDTO siteInfoModelDTO = (SiteInfoModelDTO) s;
                orgNames.add(StringUtil.StringValue(siteInfoModelDTO.getOrgName()).trim());
            });
            organizationInfoMap = baseClient.getOrganizationsByNames(orgNames);
        }
        //????????????
        AreaPramDTO province = new AreaPramDTO();
        Map<String, Long> provinceMap = new HashMap<>();
        province.setQueryType("province");
        List<AreaDTO> provinces = baseClient.queryRegionById(province);
        if (CollectionUtils.isNotEmpty(provinces)) {
            provinces.forEach(p -> {
                provinceMap.put(p.getProvince(), p.getProvinceId());
            });
        }
        //????????????
        AreaPramDTO city = new AreaPramDTO();
        Map<String, Long> cityMap = new HashMap<>();
        city.setQueryType("city");
        List<AreaDTO> cities = baseClient.queryRegionById(city);
        if (CollectionUtils.isNotEmpty(cities)) {
            cities.forEach(c -> {
                cityMap.put(c.getCity(), c.getCityId());
            });
        }
        //???????????????
        List<CompanyInfo> allCompanyInfoList = this.list();
        List<Long> companyIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(allCompanyInfoList)) {
            companyIds = allCompanyInfoList.stream().map(allCompanyInfo -> allCompanyInfo.getCompanyId()).collect(Collectors.toList());
        }
        //???????????????????????????
        List<SiteInfo> allSiteInfoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(siteInfoModelDTOS)) {
            allSiteInfoList = iSiteInfoService.list();
        }
        //????????????????????????
        List<ContactInfo> allContactInfoList = iContactInfoService.list();
        //???????????????????????????
        List<BankInfo> allBankInfoList = new ArrayList<>();
        Map<String, String> bankInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(bankInfoModelDTOS)) {
            List<String> openingBanks = bankInfoModelDTOS.stream().filter(o -> StringUtils.isNotBlank(((BankInfoModelDTO) o).getOpeningBank())).map(o -> ((BankInfoModelDTO) o).getOpeningBank()).collect(Collectors.toList());
            allBankInfoList = iBankInfoService.list(Wrappers
                    .lambdaQuery(BankInfo.class)
                    .in(BankInfo::getOpeningBank, openingBanks));
            bankInfoMap = baseClient.getUnionCodeByOpeningBanks(openingBanks);
        }
        //?????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);

        ArrayList<SiteInfo> siteInfos = new ArrayList<>();
        ArrayList<ContactInfo> contactInfos = new ArrayList<>();
        ArrayList<BankInfo> bankInfos = new ArrayList<>();
        ArrayList<OperationEquipment> operationEquipments = new ArrayList<>();
        ArrayList<OperationProduct> operationProducts = new ArrayList<>();
        ArrayList<BusinessInfo> businessInfos = new ArrayList<>();
        //?????????????????????
        if (CollectionUtils.isNotEmpty(companyInfoModelDTOS)) {
            for (Object object : companyInfoModelDTOS) {
                CompanyInfoModelDTO companyInfoModelDTO = (CompanyInfoModelDTO) object;
                CompanyInfo companyInfo = new CompanyInfo();
                CompanyInfoDetail companyInfoDetail = new CompanyInfoDetail();
                OperationInfo operationInfo = new OperationInfo();
                StringBuffer companyInfoModelDTOErrMsg = new StringBuffer();
                errorFlag = checkCompanyInfoModelDTOAndSetValue(
                        companyInfoModelDTO,
                        companyInfo,
                        companyInfoDetail,
                        operationInfo,
                        companyInfoModelDTOErrMsg,
                        dictItemDTOMap,
                        purchaseCurrencies,
                        provinceMap,
                        cityMap,
                        companyInfos,
                        companyIds,
                        companyInfoDetails,
                        operationInfos, errorFlag);
            }
        }

        //?????????????????????
        if (CollectionUtils.isNotEmpty(siteInfoModelDTOS)) {
            for (Object o : siteInfoModelDTOS) {
                if (o == null) continue;
                SiteInfoModelDTO siteInfoModelDTO = (SiteInfoModelDTO) o;
                SiteInfo siteInfo = new SiteInfo();
                StringBuffer siteInfoModelDTOErrMsg = new StringBuffer();
                errorFlag = checkSiteInfoModelDTOAndSetValue(siteInfoModelDTO, siteInfo, siteInfos, organizationInfoMap, dictItemDTOMap, provinceMap, cityMap, siteInfoModelDTOErrMsg, companyIds, errorFlag);
            }
        }
        //???????????????
        if (CollectionUtils.isNotEmpty(contactInfoModelDTOS)) {
            for (Object o : contactInfoModelDTOS) {
                if (o == null) continue;
                ContactInfoModelDTO contactInfoModelDTO = (ContactInfoModelDTO) o;
                ContactInfo contactInfo = new ContactInfo();
                StringBuffer contactInfoModelDTOErrMsg = new StringBuffer();
                errorFlag = checkContactInfoModelDTOAndSetValue(contactInfoModelDTO, contactInfo, contactInfos, dictItemDTOMap, companyIds, contactInfoModelDTOErrMsg, errorFlag);
            }
        }
        //????????????
        if (CollectionUtils.isNotEmpty(bankInfoModelDTOS)) {
            for (Object o : bankInfoModelDTOS) {
                if (o == null) continue;
                BankInfoModelDTO bankInfoModelDTO = (BankInfoModelDTO) o;
                BankInfo bankInfo = new BankInfo();
                StringBuffer bankInfoModelDTOErrMsg = new StringBuffer();
                errorFlag = checkBankInfoModelDTOAndSetValue(bankInfoModelDTO, bankInfo, bankInfos, dictItemDTOMap, companyIds, bankInfoModelDTOErrMsg, purchaseCurrencyMap, errorFlag, allBankInfoList, bankInfoMap);
            }
        }
        //????????????
        if (CollectionUtils.isNotEmpty(equipmentModelDTOS)) {
            for (Object o : equipmentModelDTOS) {
                if (o == null) continue;
                EquipmentModelDTO equipmentModelDTO = (EquipmentModelDTO) o;
                OperationEquipment operationEquipment = new OperationEquipment();
                StringBuffer equipmentModelDTOErrMsg = new StringBuffer();
                errorFlag = checkEquipmentModelDTOAndSetValue(equipmentModelDTO, operationEquipment, operationEquipments, companyIds, equipmentModelDTOErrMsg, errorFlag);
            }
        }
        //??????????????????
        if (CollectionUtils.isNotEmpty(productModelDTOS)) {
            for (Object o : productModelDTOS) {
                if (o == null) continue;
                ProductModelDTO productModelDTO = (ProductModelDTO) o;
                OperationProduct operationProduct = new OperationProduct();
                StringBuffer productModelDTOErrMsg = new StringBuffer();
                errorFlag = checkProductModelDTOAndSetValue(productModelDTO, operationProduct, operationProducts, companyIds, productModelDTOErrMsg, errorFlag);
            }
        }
        //??????????????????
        if (CollectionUtils.isNotEmpty(businessInfoModelDTOS)) {
            for (Object o : businessInfoModelDTOS) {
                if (o == null) continue;
                BusinessInfoModelDTO businessInfoModelDTO = (BusinessInfoModelDTO) o;
                BusinessInfo businessInfo = new BusinessInfo();
                StringBuffer businessInfoModelDTOErrMsg = new StringBuffer();
                errorFlag = checkBusinessInfoModelDTOAndSetValue(businessInfoModelDTO, businessInfo, businessInfos, companyIds, businessInfoModelDTOErrMsg, errorFlag);
            }
        }
        long checkTimes = System.currentTimeMillis() / 1000 - startSeconds;
        log.info("????????????==============================>????????????:" + checkTimes + "s");
        if (errorFlag.get()) {
            String type = filename.substring(filename.lastIndexOf(".") + 1);
            Fileupload fileupload = new Fileupload()
                    .setFileModular("?????????")
                    .setFileFunction("?????????????????????")
                    .setSourceType("???????????????")
                    .setUploadType(FileUploadType.FASTDFS.name())
                    .setFileType(type);
            fileupload.setFileSourceName("?????????????????????." + type);
            String[] sheetNames = {file.getName(), "?????????????????????", "?????????????????????", "???????????????", "????????????", "????????????", "??????????????????", "??????????????????"};
            List<List<? extends Object>> dataLists = new ArrayList<>();
            dataLists.add(companyInfoModelDTOS);
            dataLists.add(siteInfoModelDTOS);
            dataLists.add(contactInfoModelDTOS);
            dataLists.add(bankInfoModelDTOS);
            dataLists.add(equipmentModelDTOS);
            dataLists.add(productModelDTOS);
            dataLists.add(businessInfoModelDTOS);
            Class<? extends Object> clazz[] = new Class[]{CompanyInfoModelDTO.class, SiteInfoModelDTO.class, ContactInfoModelDTO.class, BankInfoModelDTO.class, EquipmentModelDTO.class, ProductModelDTO.class, BusinessInfoModelDTO.class};
            Fileupload wrongFile = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    dataLists, clazz, sheetNames, file.getOriginalFilename(), file.getContentType());
            result.put("status", YesOrNo.NO.getValue());
            result.put("message", "error");
            result.put("fileuploadId", wrongFile.getFileuploadId());
            result.put("fileName", wrongFile.getFileSourceName());
        } else {
            int updatesSize = 1000;//???????????????????????????

            //???????????????
            //???????????????????????????
            if (CollectionUtils.isNotEmpty(companyInfos)) {
                for (int i = 0; i < companyInfos.size(); i++) {
                    Long companyId = companyInfos.get(i).getCompanyId();
                    if (ObjectUtils.isEmpty(companyId)) {
                        companyId = IdGenrator.generate();
                        companyInfos.get(i).setCompanyId(companyId);
                        //?????????????????????(??????????????????),???????????????????????????????????????
                        setCodeAndId(companyInfos.get(i), companyInfoDetails.get(i), operationInfos.get(i), companyId);
                        companyInfoDetails.get(i).setCompanyDetailId(IdGenrator.generate());
                        long opInfoId = IdGenrator.generate();
                        operationInfos.get(i).setOpInfoId(opInfoId);
                        this.save(companyInfos.get(i));
                        iCompanyInfoDetailService.save(companyInfoDetails.get(i));
                        operationInfoService.save(operationInfos.get(i));
                    } else {
                        setCodeAndId(companyInfos.get(i), companyInfoDetails.get(i), operationInfos.get(i), companyId);
                        this.updateById(companyInfos.get(i));
                        iCompanyInfoDetailService.update(companyInfoDetails.get(i), new UpdateWrapper<>(new CompanyInfoDetail().setCompanyId(companyId)));
                        operationInfoService.update(operationInfos.get(i), new UpdateWrapper<>(new OperationInfo().setCompanyId(companyId)));
                    }
                }
            }
            //??????????????????????????????
            if (CollectionUtils.isNotEmpty(siteInfos)) {
                saveOrUpdateSiteInfo(siteInfos, allSiteInfoList);
            }
            //????????????????????????
            if (CollectionUtils.isNotEmpty(contactInfos)) {
                saveOrUpdateContactInfo(contactInfos, allContactInfoList);
            }
            //?????????????????????
            if (CollectionUtils.isNotEmpty(bankInfos)) {
                saveOrUpdateBankInfo(bankInfos, allBankInfoList);
            }
            //??????????????????
            if (CollectionUtils.isNotEmpty(operationEquipments)) {
                for (OperationEquipment operationEquipment : operationEquipments) {
                    operationEquipment.setCompanyId(operationEquipment.getCompanyId()).setOpEquipmentId(IdGenrator.generate());
                }
                iOperationEquipmentService.saveBatch(operationEquipments);
            }
            //????????????????????????
            if (CollectionUtils.isNotEmpty(operationProducts)) {
                for (OperationProduct operationProduct : operationProducts) {
                    operationProduct.setCompanyId(operationProduct.getCompanyId()).setOpProductId(IdGenrator.generate());
                }
                iOperationProductService.saveBatch(operationProducts);
            }
            //????????????????????????
            if (CollectionUtils.isNotEmpty(businessInfos)) {
                for (BusinessInfo businessInfo : businessInfos) {
                    businessInfo.setCompanyId(businessInfo.getCompanyId()).setBusinessInfoId(IdGenrator.generate());
                }
                businessInfoService.saveBatch(businessInfos);
            }
//            for (int i = 0; i < companyInfos.size(); i++) {
//                long startDealDatasTime = System.currentTimeMillis() / 1000;
//                log.info("---------------------???????????????" + (i + 1) + "??????????????????");
//                dealDatas(companyInfos.get(i),
//                        companyInfoDetails.get(i),
//                        operationInfos.get(i),
//                        i <= siteInfoGroups.size() - 1 ? siteInfoGroups.get(i) : null,
//                        i <= contactinfoGroups.size() - 1 ? contactinfoGroups.get(i) : null,
//                        i <= bankInfoGroups.size() - 1 ? bankInfoGroups.get(i) : null,
//                        i <= operationEquipmentGroups.size() - 1 ? operationEquipmentGroups.get(i) : null,
//                        i <= operationProductGroups.size() - 1 ? operationProductGroups.get(i) : null,
//                        i <= businessInfoModelDTOS.size() - 1 ? businessInfoGroups.get(i) : null,
//                        allSiteInfoList,
//                        allContactInfoList,
//                        allBankInfoList);
//                long totalDealOneTime = System.currentTimeMillis() / 1000 - startDealDatasTime;
//                log.info("--------------------------------????????????" + (i + 1) + "??????????????????,??????:" + totalDealOneTime + "s------------------------------------------");
//            }
            result.put("status", YesOrNo.YES.getValue());
            result.put("message", "success");
        }
        long endSeconds = System.currentTimeMillis() / 1000;
        long totalTime = endSeconds - startSeconds;
        log.info("????????????==============================>?????????:" + totalTime + "s");
        return result;
    }

    private void dealDatas(CompanyInfo companyInfo, CompanyInfoDetail companyInfoDetail, OperationInfo operationInfo, ArrayList<SiteInfo> siteInfos, ArrayList<ContactInfo> contactInfos, ArrayList<BankInfo> bankInfos, ArrayList<OperationEquipment> operationEquipments, ArrayList<OperationProduct> operationProducts, ArrayList<BusinessInfo> businessInfos, List<SiteInfo> allSiteInfoList, List<ContactInfo> allContactInfoList, List<BankInfo> allBankInfoList) {
        //?????????????????????????????????
        //???????????????ID(??????????????????)
        Long companyId = companyInfo.getCompanyId();
        if (companyId == null) {
            companyId = IdGenrator.generate();
            companyInfo.setCompanyId(companyId);
            //?????????????????????(??????????????????),???????????????????????????????????????
            setCodeAndId(companyInfo, companyInfoDetail, operationInfo, companyId);
            companyInfoDetail.setCompanyDetailId(IdGenrator.generate());
            long opInfoId = IdGenrator.generate();
            operationInfo.setOpInfoId(opInfoId);
            this.save(companyInfo);
            iCompanyInfoDetailService.save(companyInfoDetail);
            operationInfoService.save(operationInfo);
            //?????????????????????
            if (CollectionUtils.isNotEmpty(siteInfos)) {
                for (SiteInfo siteInfo : siteInfos) {
                    siteInfo.setCompanyId(companyId).setSiteInfoId(IdGenrator.generate());
                }
                iSiteInfoService.saveBatch(siteInfos);
            }
            //??????????????????????????????
            if (CollectionUtils.isNotEmpty(contactInfos)) {
                for (ContactInfo contactInfo : contactInfos) {
                    contactInfo.setCompanyId(companyId).setContactInfoId(IdGenrator.generate());
                }
                iContactInfoService.saveBatch(contactInfos);
            }
            //??????????????????
            if (CollectionUtils.isNotEmpty(bankInfos)) {
                for (BankInfo bankInfo : bankInfos) {
                    bankInfo.setCompanyId(companyId).setBankInfoId(IdGenrator.generate());
                }
                iBankInfoService.saveBatch(bankInfos);
            }
            //??????????????????
            if (CollectionUtils.isNotEmpty(operationEquipments)) {
                for (OperationEquipment operationEquipment : operationEquipments) {
                    operationEquipment.setCompanyId(companyId).setOpEquipmentId(IdGenrator.generate()).setOpInfoId(opInfoId);
                }
                iOperationEquipmentService.saveBatch(operationEquipments);
            }
            //????????????????????????
            if (CollectionUtils.isNotEmpty(operationProducts)) {
                for (OperationProduct operationProduct : operationProducts) {
                    operationProduct.setCompanyId(companyId).setOpProductId(IdGenrator.generate()).setOpInfoId(opInfoId);
                }
                iOperationProductService.saveBatch(operationProducts);
            }
            //????????????????????????
            if (CollectionUtils.isNotEmpty(businessInfos)) {
                for (BusinessInfo businessInfo : businessInfos) {
                    businessInfo.setCompanyId(companyId).setBusinessInfoId(IdGenrator.generate());
                }
                businessInfoService.saveBatch(businessInfos);
            }
        } else {
            setCodeAndId(companyInfo, companyInfoDetail, operationInfo, companyId);
            this.updateById(companyInfo);
            iCompanyInfoDetailService.update(companyInfoDetail, new UpdateWrapper<>(new CompanyInfoDetail().setCompanyId(companyId)));
            operationInfoService.update(operationInfo, new UpdateWrapper<>(new OperationInfo().setCompanyId(companyId)));
            //??????????????????????????????
            if (CollectionUtils.isNotEmpty(siteInfos)) {
                saveOrUpdateSiteInfo(siteInfos, allSiteInfoList);
            }
            //????????????????????????
            if (CollectionUtils.isNotEmpty(contactInfos)) {
                saveOrUpdateContactInfo(contactInfos, allContactInfoList);
            }
            //?????????????????????
            if (CollectionUtils.isNotEmpty(bankInfos)) {
                saveOrUpdateBankInfo(bankInfos, allBankInfoList);
            }
        }
    }

    private void saveOrUpdateBankInfo(ArrayList<BankInfo> bankInfos, List<BankInfo> allBankInfoList) {
        ArrayList<BankInfo> updateBankInfos = new ArrayList<>();//?????????????????????
        Iterator<BankInfo> it = bankInfos.iterator();//????????????????????????????????????
        while (it.hasNext()) {
            BankInfo bankInfo = it.next();
            for (BankInfo allBankInfo : allBankInfoList) {
                if (bankInfo.getCompanyId().compareTo(allBankInfo.getCompanyId()) == 0
                        && bankInfo.getBankAccountName().equals(allBankInfo.getBankAccountName())
                        && bankInfo.getBankAccount().equals(allBankInfo.getBankAccount())) {
                    bankInfo.setCompanyId(bankInfo.getCompanyId()).setBankInfoId(allBankInfo.getBankInfoId());
                    updateBankInfos.add(bankInfo);
                    it.remove();
                    break;
                }
            }
        }
        iBankInfoService.updateBatchById(updateBankInfos);
        if (CollectionUtils.isNotEmpty(bankInfos)) {
            for (BankInfo bankInfo : bankInfos) {
                bankInfo.setCompanyId(bankInfo.getCompanyId()).setBankInfoId(IdGenrator.generate());
            }
            iBankInfoService.saveBatch(bankInfos);
        }
    }

    private void saveOrUpdateContactInfo(ArrayList<ContactInfo> contactInfos, List<ContactInfo> allContactInfoList) {
        ArrayList<ContactInfo> updateContactInfos = new ArrayList<>();//????????????????????????
        Iterator<ContactInfo> it = contactInfos.iterator();//????????????????????????????????????
        while (it.hasNext()) {
            ContactInfo contactInfo = it.next();
            for (ContactInfo allContactInfo : allContactInfoList) {
                if (contactInfo.getCompanyId().compareTo(allContactInfo.getCompanyId()) == 0
                        && contactInfo.getContactName().equals(allContactInfo.getContactName())) {
                    contactInfo.setCompanyId(contactInfo.getCompanyId()).setContactInfoId(allContactInfo.getContactInfoId());
                    updateContactInfos.add(contactInfo);
                    it.remove();
                    break;
                }
            }
        }
        iContactInfoService.updateBatchById(updateContactInfos);
        if (CollectionUtils.isNotEmpty(contactInfos)) {
            for (ContactInfo contactInfo : contactInfos) {
                contactInfo.setCompanyId(contactInfo.getCompanyId()).setContactInfoId(IdGenrator.generate());
            }
            iContactInfoService.saveBatch(contactInfos);
        }
    }

    private void setCodeAndId(CompanyInfo companyInfo, CompanyInfoDetail companyInfoDetail, OperationInfo operationInfo, Long companyId) {
        //?????????????????????(??????????????????)
        if (StringUtils.isBlank(companyInfo.getCompanyCode())) {
            String companyCode = baseClient.seqGen(SequenceCodeConstant.SEQ_SUP_COMPANY_CODE);
            companyInfo.setCompanyCode(companyCode);
        }
        //???????????????????????????????????????
        companyInfoDetail.setCompanyId(companyId);
        operationInfo.setCompanyId(companyId);
    }

    private void saveOrUpdateSiteInfo(ArrayList<SiteInfo> siteInfos, List<SiteInfo> allSiteInfoList) {
        ArrayList<SiteInfo> updateSiteInfos = new ArrayList<>();//????????????????????????
        Iterator<SiteInfo> it = siteInfos.iterator();//??????????????????????????????????????????
        while (it.hasNext()) {
            SiteInfo siteInfo = it.next();
            for (SiteInfo allSiteInfo : allSiteInfoList) {
                if (allSiteInfo == null || allSiteInfo.getCompanyId() == null) continue;
                if (siteInfo.getCompanyId().compareTo(allSiteInfo.getCompanyId()) == 0
                        && siteInfo.getOrgName().equals(allSiteInfo.getOrgName())
                        && siteInfo.getVendorSiteCode().equals(allSiteInfo.getVendorSiteCode())) {
                    siteInfo.setCompanyId(siteInfo.getCompanyId()).setSiteInfoId(allSiteInfo.getSiteInfoId());
                    updateSiteInfos.add(siteInfo);
                    it.remove();
                    break;
                }
            }
        }
        iSiteInfoService.updateBatchById(updateSiteInfos);
        if (CollectionUtils.isNotEmpty(siteInfos)) {
            for (SiteInfo siteInfo : siteInfos) {
                siteInfo.setSiteInfoId(IdGenrator.generate()).setCompanyId(siteInfo.getCompanyId());
            }
            iSiteInfoService.saveBatch(siteInfos);
        }
    }

    private AtomicBoolean checkBusinessInfoModelDTOAndSetValue(BusinessInfoModelDTO businessInfoModelDTO, BusinessInfo businessInfo, ArrayList<BusinessInfo> businessInfos, List<Long> companyIds, StringBuffer businessInfoModelDTOErrMsg, AtomicBoolean errorFlag) {
        //????????????
        String s = ObjectUtil.validateForImport(businessInfoModelDTO, ",");
        if (!Objects.equals(s, YesOrNo.YES.getValue())) {
            businessInfoModelDTOErrMsg.append(s);
            errorFlag.set(true);
        }
        //??????????????????
        if (StringUtils.isNotBlank(businessInfoModelDTO.getCompanyId())) {
            boolean contains = companyIds.contains(Long.valueOf(businessInfoModelDTO.getCompanyId()));
            if (!contains) {
                businessInfoModelDTOErrMsg.append("???????????????ID????????????????????? ");
                errorFlag.set(true);
            }
        }
        //??????
        if (errorFlag.get()) {
            businessInfoModelDTO.setErrorMessage(businessInfoModelDTOErrMsg.toString());
        } else {
            BeanUtils.copyProperties(businessInfoModelDTO, businessInfo, businessInfoModelDTO.getCompanyId());
            businessInfo.setCompanyId(Long.valueOf(businessInfoModelDTO.getCompanyId()));
            businessInfos.add(businessInfo);
        }
        return errorFlag;
    }

    private AtomicBoolean checkProductModelDTOAndSetValue(ProductModelDTO productModelDTO, OperationProduct operationProduct, ArrayList<OperationProduct> operationProducts, List<Long> companyIds, StringBuffer productModelDTOErrMsg, AtomicBoolean errorFlag) {
        //????????????
        String s = ObjectUtil.validateForImport(productModelDTO, ",");
        if (!Objects.equals(s, YesOrNo.YES.getValue())) {
            productModelDTOErrMsg.append(s);
            errorFlag.set(true);
        }
        //??????????????????
        if (StringUtils.isNotBlank(productModelDTO.getCompanyId())) {
            boolean contains = companyIds.contains(Long.valueOf(productModelDTO.getCompanyId()));
            if (!contains) {
                productModelDTOErrMsg.append("???????????????ID????????????????????? ");
                errorFlag.set(true);
            }
        }
        //??????
        if (errorFlag.get()) {
            productModelDTO.setErrorMessage(productModelDTOErrMsg.toString());
        } else {
            BeanUtils.copyProperties(productModelDTO, operationProduct, productModelDTO.getCompanyId());
            operationProduct.setCompanyId(Long.valueOf(productModelDTO.getCompanyId()));
            operationProducts.add(operationProduct);
        }
        return errorFlag;
    }

    private AtomicBoolean checkEquipmentModelDTOAndSetValue(EquipmentModelDTO equipmentModelDTO, OperationEquipment operationEquipment, ArrayList<OperationEquipment> operationEquipments, List<Long> companyIds, StringBuffer equipmentModelDTOErrMsg, AtomicBoolean errorFlag) {
        //????????????
        String s = ObjectUtil.validateForImport(equipmentModelDTO, ",");
        if (!Objects.equals(s, YesOrNo.YES.getValue())) {
            equipmentModelDTOErrMsg.append(s);
            errorFlag.set(true);
        }
        //??????????????????
        if (StringUtils.isNotBlank(equipmentModelDTO.getCompanyId())) {
            boolean contains = companyIds.contains(Long.valueOf(equipmentModelDTO.getCompanyId()));
            if (!contains) {
                equipmentModelDTOErrMsg.append("???????????????ID????????????????????? ");
                errorFlag.set(true);
            }
        }
        //??????
        if (errorFlag.get()) {
            equipmentModelDTO.setErrorMessage(equipmentModelDTOErrMsg.toString());
        } else {
            BeanUtils.copyProperties(equipmentModelDTO, operationEquipment, equipmentModelDTO.getCompanyId(), equipmentModelDTO.getErpVendorId());
            operationEquipment.setCompanyId(Long.valueOf(equipmentModelDTO.getCompanyId()));
            operationEquipments.add(operationEquipment);
        }
        return errorFlag;
    }

    private AtomicBoolean checkBankInfoModelDTOAndSetValue(BankInfoModelDTO bankInfoModelDTO, BankInfo bankInfo, ArrayList<BankInfo> bankInfos, Map<String, DictItemDTO> dictItemDTOMap, List<Long> companyIds, StringBuffer bankInfoModelDTOErrMsg, Map<String, PurchaseCurrency> purchaseCurrencyMap, AtomicBoolean errorFlag, List<BankInfo> allBankInfoList, Map<String, String> bankInfoMap) {
        //????????????
        String s = ObjectUtil.validateForImport(bankInfoModelDTO, ",");
        if (!Objects.equals(s, YesOrNo.YES.getValue())) {
            bankInfoModelDTOErrMsg.append(s);
            errorFlag.set(true);
        }
        //??????????????????
        if (StringUtils.isNotBlank(bankInfoModelDTO.getCompanyId())) {
            boolean contains = companyIds.contains(Long.valueOf(bankInfoModelDTO.getCompanyId()));
            if (!contains) {
                bankInfoModelDTOErrMsg.append("???????????????ID????????????????????? ");
                errorFlag.set(true);
            }
        }
        //????????????
        if (StringUtils.isNotBlank(bankInfoModelDTO.getCurrencyName())) {
            PurchaseCurrency purchaseCurrency = purchaseCurrencyMap.get(bankInfoModelDTO.getCurrencyName());
            if (purchaseCurrency == null) {
                bankInfoModelDTOErrMsg.append("??????????????????????????????,???????????????! ");
                errorFlag.set(true);
            }
        }
        //??????
        if (errorFlag.get()) {
            bankInfoModelDTO.setErrorMessage(bankInfoModelDTOErrMsg.toString());
        } else {
            BeanUtils.copyProperties(bankInfoModelDTO, bankInfo,
                    "companyId",
                    "erpVendorId",
                    "currencyName");
            PurchaseCurrency purchaseCurrency = purchaseCurrencyMap.get(bankInfoModelDTO.getCurrencyName());
            bankInfo.setCompanyId(Long.valueOf(bankInfoModelDTO.getCompanyId()));
            bankInfo.setErpVendorId(StringUtils.isNotBlank(bankInfoModelDTO.getErpVendorId()) ? Long.valueOf(bankInfoModelDTO.getErpVendorId()) : null);
            bankInfo.setUnionCode(StringUtils.isNotBlank(bankInfo.getOpeningBank()) ? bankInfoMap.get(bankInfo.getOpeningBank()) : null);
            if (purchaseCurrency != null) {
                bankInfo.setCurrencyCode(purchaseCurrency.getCurrencyCode());
                bankInfo.setCurrencyName(purchaseCurrency.getCurrencyName());
            }
            bankInfos.add(bankInfo);
        }
        return errorFlag;
    }

    private AtomicBoolean checkContactInfoModelDTOAndSetValue(ContactInfoModelDTO contactInfoModelDTO, ContactInfo contactInfo, ArrayList<ContactInfo> contactInfos, Map<String, DictItemDTO> dictItemDTOMap, List<Long> companyIds, StringBuffer contactInfoModelDTOErrMsg, AtomicBoolean errorFlag) {
        //????????????
        String s = ObjectUtil.validateForImport(contactInfoModelDTO, ",");
        if (!Objects.equals(s, YesOrNo.YES.getValue())) {
            contactInfoModelDTOErrMsg.append(s);
            errorFlag.set(true);
        }
        //??????????????????
        if (StringUtils.isNotBlank(contactInfoModelDTO.getCompanyId())) {
            boolean contains = companyIds.contains(Long.valueOf(contactInfoModelDTO.getCompanyId()));
            if (!contains) {
                contactInfoModelDTOErrMsg.append("???????????????ID????????????????????? ");
                errorFlag.set(true);
            }
        }
        //??????
        if (errorFlag.get()) {
            contactInfoModelDTO.setErrorMessage(contactInfoModelDTOErrMsg.toString());
        } else {
            BeanUtils.copyProperties(contactInfoModelDTO, contactInfo);
            DictItemDTO ceeaGenderDictItemDTO = dictItemDTOMap.get(contactInfoModelDTO.getCeeaGender());
            contactInfo.setCeeaGender(ceeaGenderDictItemDTO == null ? null : ceeaGenderDictItemDTO.getDictItemCode());
            contactInfo.setCompanyId(Long.valueOf(contactInfoModelDTO.getCompanyId()));
            contactInfo.setErpVendorId(StringUtils.isNotBlank(contactInfoModelDTO.getErpVendorId()) ? Long.valueOf(contactInfoModelDTO.getErpVendorId()) : null);
            contactInfos.add(contactInfo);
        }
        return errorFlag;
    }

    private AtomicBoolean checkCompanyInfoModelDTOAndSetValue(CompanyInfoModelDTO companyInfoModelDTO, CompanyInfo companyInfo, CompanyInfoDetail companyInfoDetail, OperationInfo operationInfo, StringBuffer companyInfoModelDTOErrMsg, Map<String, DictItemDTO> dictItemDTOMap, List<PurchaseCurrency> purchaseCurrencies, Map<String, Long> provinceMap, Map<String, Long> cityMap, ArrayList<CompanyInfo> companyInfos, List<Long> companyIds, ArrayList<CompanyInfoDetail> companyInfoDetails, ArrayList<OperationInfo> operationInfos, AtomicBoolean errorFlag) {
        //????????????
        if (StringUtils.isBlank(companyInfoModelDTO.getCompanyId())) {
            String s = ObjectUtil.validateForImport(companyInfoModelDTO, ",");
            if (!Objects.equals(s, YesOrNo.YES.getValue())) {
                companyInfoModelDTOErrMsg.append(s);
                errorFlag.set(true);
            }
        }
        //??????????????????
        if (StringUtils.isNotBlank(companyInfoModelDTO.getCompanyId())) {
            boolean contains = companyIds.contains(Long.valueOf(companyInfoModelDTO.getCompanyId()));
            if (!contains) {
                companyInfoModelDTOErrMsg.append("???????????????ID????????????????????? ");
                errorFlag.set(true);
            }
        }
        //??????????????????
        String companyCreationDate = companyInfoModelDTO.getCompanyCreationDate();
        LocalDate parseCompanyCreationDate = null;
        try {
            if (StringUtils.isNotBlank(companyCreationDate)) {
                Date date = DateUtil.parseDate(companyCreationDate);
                parseCompanyCreationDate = DateUtil.dateToLocalDate(date);
            }
        } catch (ParseException e) {
            companyInfoModelDTOErrMsg.append("?????????????????????????????? ");
            errorFlag.set(true);
        }
        //??????????????????
        String businessStartDate = companyInfoModelDTO.getBusinessStartDate();
        String businessEndDate = companyInfoModelDTO.getBusinessEndDate();
        LocalDate parseBusinessStartDate = null;
        LocalDate parseBusinessEndDate = null;
        try {
            if (StringUtils.isNotBlank(businessStartDate)) {
                Date date = DateUtil.parseDate(businessStartDate);
                parseBusinessStartDate = DateUtil.dateToLocalDate(date);
            }
            if (StringUtils.isNotBlank(businessEndDate)) {
                Date date = DateUtil.parseDate(businessEndDate);
                parseBusinessEndDate = DateUtil.dateToLocalDate(date);
            }
        } catch (ParseException e) {
            companyInfoModelDTOErrMsg.append("??????????????????/???????????????????????????????????? ");
            errorFlag.set(true);
        }
        //??????????????????
        String ceeaListedTime = companyInfoModelDTO.getCeeaListedTime();
        LocalDate parseCeeaListedTime = null;
        try {
            if (StringUtils.isNotBlank(ceeaListedTime)) {
                Date date = DateUtil.parseDate(ceeaListedTime);
                parseCeeaListedTime = DateUtil.dateToLocalDate(date);
            }
        } catch (ParseException e) {
            companyInfoModelDTOErrMsg.append("?????????????????????????????? ");
            errorFlag.set(true);
        }
        //???????????????????????????????????????
        Set<String> dictItemName = dictItemDTOMap.keySet();
        boolean companyTypeContain = dictItemName.contains(StringUtil.StringValue(companyInfoModelDTO.getCompanyTypeName()).trim());//????????????
        boolean businessModelContain = dictItemName.contains(StringUtil.StringValue(companyInfoModelDTO.getCeeaBusinessModel()).trim());//????????????
        boolean supBusinessTypeContain = dictItemName.contains(StringUtil.StringValue(companyInfoModelDTO.getCeeaSupBusinessType()).trim());//?????????????????????
        boolean plantTypeContain = dictItemName.contains(StringUtil.StringValue(companyInfoModelDTO.getCeeaPlantType()).trim());//????????????
        boolean companyCountryContain = dictItemName.contains(StringUtil.StringValue(companyInfoModelDTO.getCompanyCountry()).trim());//??????
        boolean overseasRelationContain = dictItemName.contains(StringUtil.StringValue(companyInfoModelDTO.getOverseasRelation()).trim());//???????????????
        boolean statusContain = dictItemName.contains(StringUtil.StringValue(companyInfoModelDTO.getStatus()).trim());//???????????????
        if (StringUtils.isNotBlank(companyInfoModelDTO.getOverseasRelation()) && !overseasRelationContain) {
            companyInfoModelDTOErrMsg.append("?????????????????????????????????????????? ");
            errorFlag.set(true);
        }
        if (StringUtils.isNotBlank(companyInfoModelDTO.getCompanyTypeName()) && !companyTypeContain) {
            companyInfoModelDTOErrMsg.append("??????????????????????????????????????? ");
            errorFlag.set(true);
        }
        if (StringUtils.isNotBlank(companyInfoModelDTO.getCeeaBusinessModel()) && !businessModelContain) {
            companyInfoModelDTOErrMsg.append("??????????????????????????????????????? ");
            errorFlag.set(true);
        }
        if (StringUtils.isNotBlank(companyInfoModelDTO.getCeeaSupBusinessType()) && !supBusinessTypeContain) {
            companyInfoModelDTOErrMsg.append("???????????????????????????????????????????????? ");
            errorFlag.set(true);
        }
        if (StringUtils.isNotBlank(companyInfoModelDTO.getCeeaPlantType()) && !plantTypeContain) {
            companyInfoModelDTOErrMsg.append("??????????????????????????????????????? ");
            errorFlag.set(true);
        }
        if (StringUtils.isNotBlank(companyInfoModelDTO.getCompanyCountry()) && !companyCountryContain) {
            companyInfoModelDTOErrMsg.append("?????????????????????????????? ");
            errorFlag.set(true);
        }
        if (StringUtils.isNotBlank(companyInfoModelDTO.getStatus()) && !statusContain) {
            companyInfoModelDTOErrMsg.append("?????????????????????????????? ");
            errorFlag.set(true);
        }
        //??????
        if (errorFlag.get()) {
            companyInfoModelDTO.setErrorMessage(companyInfoModelDTOErrMsg.toString());
        } else {
            BeanUtils.copyProperties(companyInfoModelDTO, companyInfo,
                    "companyId",//??????copy????????????
                    "companyCreationDate",
                    "businessStartDate",
                    "businessEndDate",
                    "ceeaListedTime",
                    "erpVendorId",
                    "companyProvince",
                    "companyCity");
            BeanUtils.copyProperties(companyInfoModelDTO, companyInfoDetail, companyInfoModelDTO.getCompanyId());
            BeanUtils.copyProperties(companyInfoModelDTO, operationInfo, companyInfoModelDTO.getCompanyId());
            DictItemDTO overseasRelationDictItemDTO = dictItemDTOMap.get(companyInfoModelDTO.getOverseasRelation());
            DictItemDTO companyTypeNameDictItemDTO = dictItemDTOMap.get(companyInfoModelDTO.getCompanyTypeName());
            DictItemDTO businessModelDictItemDTO = dictItemDTOMap.get(companyInfoModelDTO.getCeeaBusinessModel());
            DictItemDTO supBusinessTypeDictItemDTO = dictItemDTOMap.get(companyInfoModelDTO.getCeeaSupBusinessType());
            DictItemDTO plantTypeDictItemDTO = dictItemDTOMap.get(companyInfoModelDTO.getCeeaPlantType());
            DictItemDTO companyCountryDictItemDTO = dictItemDTOMap.get(companyInfoModelDTO.getCompanyCountry());
            DictItemDTO statusDictItemDTO = dictItemDTOMap.get(StringUtil.StringValue(companyInfoModelDTO.getStatus()).trim());
            companyInfo.setCompanyId(StringUtils.isNotBlank(companyInfoModelDTO.getCompanyId()) ? Long.valueOf(companyInfoModelDTO.getCompanyId()) : null);
            companyInfo.setOverseasRelation(overseasRelationDictItemDTO == null ? null : overseasRelationDictItemDTO.getDictItemCode());
            companyInfo.setCompanyType(companyTypeNameDictItemDTO == null ? null : companyTypeNameDictItemDTO.getDictItemCode());
            companyInfo.setCeeaBusinessModel(companyTypeNameDictItemDTO == null ? null : businessModelDictItemDTO.getDictItemCode());
            companyInfo.setCeeaSupBusinessType(supBusinessTypeDictItemDTO == null ? null : supBusinessTypeDictItemDTO.getDictItemCode());
            companyInfo.setCeeaPlantType(plantTypeDictItemDTO == null ? null : plantTypeDictItemDTO.getDictItemCode());
            companyInfo.setCompanyCountry(companyCountryDictItemDTO == null ? null : companyCountryDictItemDTO.getDictItemCode());
            companyInfo.setStatus(statusDictItemDTO == null ? null : statusDictItemDTO.getDictItemCode());
            companyInfo.setErpVendorId(StringUtils.isNotBlank(companyInfoModelDTO.getErpVendorId()) ? Long.valueOf(companyInfoModelDTO.getErpVendorId()) : null);
            for (String key : provinceMap.keySet()) {
                String companyProvince = companyInfoModelDTO.getCompanyProvince();
                if (StringUtils.isNotBlank(companyProvince) && key.indexOf(companyProvince) > -1) {
                    companyInfo.setCompanyProvince(StringUtil.StringValue(provinceMap.get(key)));
                }
            }
            for (String key : cityMap.keySet()) {
                String companyCity = companyInfoModelDTO.getCompanyCity();
                if (StringUtils.isNotBlank(companyCity) && key.indexOf(companyCity) > -1) {
                    companyInfo.setCompanyCity(StringUtil.StringValue(cityMap.get(key)));
                }
            }
            companyInfo.setCompanyCreationDate(parseCompanyCreationDate)
                    .setBusinessStartDate(parseBusinessStartDate)
                    .setBusinessEndDate(parseBusinessEndDate)
                    .setCeeaListedTime(parseCeeaListedTime);
            companyInfos.add(companyInfo);
            companyInfoDetails.add(companyInfoDetail);
            operationInfos.add(operationInfo);
        }
        return errorFlag;
    }

    private AtomicBoolean checkSiteInfoModelDTOAndSetValue(SiteInfoModelDTO siteInfoModelDTO, SiteInfo siteInfo, ArrayList<SiteInfo> siteInfos, Map<String, Organization> organizationInfoMap, Map<String, DictItemDTO> dictItemDTOMap, Map<String, Long> provinceMap, Map<String, Long> cityMap, StringBuffer siteInfoModelDTOErrMsg, List<Long> companyIds, AtomicBoolean errorFlag) {
        //????????????
        String s = ObjectUtil.validateForImport(siteInfoModelDTO, ",");
        if (!Objects.equals(s, YesOrNo.YES.getValue())) {
            siteInfoModelDTOErrMsg.append(s);
            errorFlag.set(true);
        }
        //??????????????????
        if (StringUtils.isNotBlank(siteInfoModelDTO.getCompanyId())) {
            boolean contains = companyIds.contains(Long.valueOf(siteInfoModelDTO.getCompanyId()));
            if (!contains) {
                siteInfoModelDTOErrMsg.append("???????????????ID????????????????????? ");
                errorFlag.set(true);
            }
        }
        //??????????????????
        Organization organization = organizationInfoMap.get(siteInfoModelDTO.getOrgName());
        if (organization == null) {
            siteInfoModelDTOErrMsg.append("?????????????????????????????? ");
            errorFlag.set(true);
        }
        //?????????????????????
        DictItemDTO dictItemDTO = dictItemDTOMap.get(siteInfoModelDTO.getVendorSiteCode());
        if (dictItemDTO == null) {
            siteInfoModelDTOErrMsg.append("????????????????????????????????? ");
            errorFlag.set(true);
        }
        //????????????
        Set<String> dictItemName = dictItemDTOMap.keySet();
        boolean countryContain = dictItemName.contains(StringUtil.StringValue(siteInfoModelDTO.getCountry()).trim());//??????
        if (StringUtils.isNotBlank(siteInfoModelDTO.getCountry()) && !countryContain) {
            siteInfoModelDTOErrMsg.append("?????????????????????????????? ");
            errorFlag.set(true);
        }
        //??????
        if (errorFlag.get()) {
            siteInfoModelDTO.setErrorMessage(siteInfoModelDTOErrMsg.toString());
        } else {
            DictItemDTO countryDictItemDTO = dictItemDTOMap.get(siteInfoModelDTO.getCountry());
            BeanUtils.copyProperties(siteInfoModelDTO, siteInfo,
                    "companyId",
                    "erpVendorId",
                    "province",
                    "city");
            siteInfo.setOrgId(organization.getOrganizationId())
                    .setOrgCode(organization.getOrganizationCode())
                    .setCountry(countryDictItemDTO == null ? null : countryDictItemDTO.getDictItemCode())
                    .setOrgName(organization.getOrganizationName())
                    .setBelongOprId(organization.getErpOrgId())//??????????????????ErpId
                    .setErpVendorId(StringUtils.isNotBlank(siteInfoModelDTO.getErpVendorId()) ? Long.valueOf(siteInfoModelDTO.getErpVendorId()) : null)
                    .setCompanyId(Long.valueOf(siteInfoModelDTO.getCompanyId()));
            for (String key : provinceMap.keySet()) {
                String companyProvince = siteInfoModelDTO.getProvince();
                if (StringUtils.isNotBlank(companyProvince) && key.indexOf(companyProvince) > -1) {
                    siteInfo.setProvince(StringUtil.StringValue(provinceMap.get(key)));
                }
            }
            for (String key : cityMap.keySet()) {
                String companyCity = siteInfoModelDTO.getCity();
                if (StringUtils.isNotBlank(companyCity) && key.indexOf(companyCity) > -1) {
                    siteInfo.setCity(StringUtil.StringValue(cityMap.get(key)));
                }
            }
            siteInfos.add(siteInfo);
        }
        return errorFlag;
    }


    public void setImproveFormDtos(Long vendorId, VendorImageDto vendorImageDto) {
        List<ImproveFormDto> improveFormDtos = performanceClient.getImproveFormDtoByVendorId(vendorId);
        // ???????????????
        int improveSum = 0;
        // ???????????????
        int closeSum = 0;
        if (CollectionUtils.isNotEmpty(improveFormDtos)) {
            improveSum = improveFormDtos.size();
            for (ImproveFormDto improveFormDto : improveFormDtos) {
                String status = improveFormDto.getStatus();
                if (VendorImproveFormStatus.EVALUATED.getCode().equals(status)) {
                    closeSum++;
                }
            }
        }
        vendorImageDto.setImproveSum(improveSum);
        vendorImageDto.setCloseSum(closeSum);
        vendorImageDto.setImproveFormDtos(improveFormDtos);
    }

    public void setAssesFormDtos(Long vendorId, VendorImageDto vendorImageDto) {
        List<AssesFormDto> assesFormDtos = performanceClient.getAssesFormDtoByVendorId(vendorId);
        vendorImageDto.setExceptionSum(assesFormDtos.size());
        vendorImageDto.setAssesFormDtos(assesFormDtos);
    }

    public void setPerfOverallScoreDtos(Long vendorId, VendorImageDto vendorImageDto) {
        // ???????????????????????????
        List<PerfOverallScoreDto> perfOverallScore = performanceClient.getPerfOverallScoreVendorId(vendorId);
        vendorImageDto.setPerfOverallScoreDtos(perfOverallScore);
    }

    public void setBidFrequencies(Long vendorId, VendorImageDto vendorImageDto) throws ParseException {
        List<BidFrequency> bidFrequency = inqClient.getThreeYearsBidFrequency(vendorId);
        vendorImageDto.setBidFrequencies(bidFrequency);
    }

    public void setCategoryRelDtos(Long vendorId, VendorImageDto vendorImageDto) {
        List<OrgCategory> orgCategories = orgCategoryService.list(new QueryWrapper<>(new OrgCategory().setCompanyId(vendorId)));
        ArrayList<CategoryRelDto> categoryRelDtos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(orgCategories)) {
            orgCategories.forEach(orgCategory -> {
                CategoryRelDto categoryRelDto = new CategoryRelDto();
                BeanCopyUtil.copyProperties(categoryRelDto, orgCategory);
                categoryRelDto.setStatus(orgCategory.getServiceStatus());
                categoryRelDtos.add(categoryRelDto);
            });
        }
        vendorImageDto.setCategoryRelDtos(categoryRelDtos);
    }

    public void setCompanyBaseInfo(Long vendorId, VendorImageDto vendorImageDto) {
        /**
         * ???????????????????????????, ????????????????????????
         */
        CompanyInfo companyInfo = this.getOne(new QueryWrapper<>(new CompanyInfo().setCompanyId(vendorId)));
        Assert.notNull(companyInfo, "???????????????????????????");
        vendorImageDto.setVendorId(vendorId);
        BeanCopyUtil.copyProperties(vendorImageDto, companyInfo);
    }

    public void setServiceLength(Long vendorId, VendorImageDto vendorImageDto) {
        /**
         * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
         * ????????????: ???????????????????????????????????????????????????
         */
        DeliveryNote firstDeliveryNo = supcooperateClient.getFirstDeliveryNo(vendorId);
        if (null != firstDeliveryNo) {
            Date creationDate = firstDeliveryNo.getCreationDate();
            int startYear = DateUtil.dateToLocalDate(creationDate).getYear();
            int nowYear = LocalDate.now().getYear();
            vendorImageDto.setServiceLength(nowYear - startYear + 1);
        } else {
            vendorImageDto.setServiceLength(0);
        }
    }

    public void setSingleSourceDtos(Long vendorId, VendorImageDto vendorImageDto) {
        /**
         * ????????????, ?????????????????????????????????????????????????????????????????????????????????????????????????????????OU???????????????
         * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
         */
        List<OrgCategory> orgCategories = orgCategoryService.querySingleSourceList(vendorId);
        // ????????????????????????+??????
        List<SingleSourceDto> singleSourceDtos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(orgCategories)) {
            // ????????????id
            List<Long> orgIds = new ArrayList<>();

            //
            for (OrgCategory orgCategory : orgCategories) {
                orgIds.add(orgCategory.getOrgId());
                SingleSourceDto singleSourceDto = new SingleSourceDto();
                BeanCopyUtil.copyProperties(singleSourceDto, orgCategory);
                singleSourceDtos.add(singleSourceDto);
            }

            OrganizationMapDto organizationMapDto = baseClient.getFatherByChild(orgIds);
            // ??????id - ???id
            HashMap<String, Long> pcMap = organizationMapDto.getPcMap();
            if (CollectionUtils.isNotEmpty(singleSourceDtos)) {
                /**
                 * ????????????????????????????????????ID
                 */
                ArrayList<SingleSourceDto> singleSourceDtoNew = new ArrayList<>();
                singleSourceDtos.forEach(singleSourceDto -> {
                    Long orgId = singleSourceDto.getOrgId();
                    String key = String.valueOf(orgId);
                    Long fatherId = pcMap.get(key);
                    if (StringUtil.notEmpty(fatherId)) {
                        singleSourceDto.setFatherId(fatherId);
                        singleSourceDtoNew.add(singleSourceDto);
                    }
                });
                singleSourceDtos = singleSourceDtoNew;

                // ???????????????, ???????????????????????????Id??????
                List<Organization> buList = organizationMapDto.getBuList();
                // ??????????????????
                List<Organization> buOrgList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(buList)) {
                    // ???????????????
                    Map<Long, List<SingleSourceDto>> longListMap = singleSourceDtos.stream().collect(Collectors.groupingBy(SingleSourceDto::getCategoryId));
                    // ??????????????????????????????????????????????????????????????????
                    longListMap.forEach((categoryId, orgCate) -> {
                        /**
                         * ??????????????????????????????????????????, ??????????????????????????????
                         */
                        // ??????????????????
                        Map<Long, List<SingleSourceDto>> collect = orgCate.stream().collect(Collectors.groupingBy(SingleSourceDto::getFatherId));
                        collect.forEach((fatherId, orgCategories2) -> {
                            // orgCategories2???????????????????????????, fatherId??????id
                            int size = ListUtil.listDeduplication(orgCategories2, SingleSourceDto::getOrgId).size();
                            for (Organization organization : buList) {
                                if (fatherId.equals(organization.getOrganizationId())) {
                                    int size1 = organization.getOuIds().size();
                                    if (size == size1) {
                                        buOrgList.add(organization);
                                    }
                                    break;
                                }
                            }
                        });
                    });
                }
                if (CollectionUtils.isNotEmpty(buOrgList)) {
                    for (Organization organization : buOrgList) {
                        List<Long> ouIds = organization.getOuIds();
                        for (SingleSourceDto singleSourceDto : singleSourceDtos) {
                            Long orgId = singleSourceDto.getOrgId();
                            if (ouIds.contains(orgId)) {
                                singleSourceDto.setOrgId(organization.getOrganizationId());
                                singleSourceDto.setOrgName(organization.getOrganizationName());
                                singleSourceDto.setOrgCode(organization.getOrganizationCode());
                                singleSourceDto.setOrgType(organization.getOrganizationTypeCode());
                                singleSourceDto.setFullPathId(organization.getFullPathId());
                                singleSourceDto.setFatherId(organization.getFatherId());
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(singleSourceDtos)) {
            vendorImageDto.setSingleSourceDtos(ListUtil.listDeduplication(singleSourceDtos, u -> {
                return u.getOrgId() + u.getCategoryId();
            }));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdateInfo(InfoDTO infoDTO, String orderStatus, String dataSource) {
        //?????????????????????????????????
        LoginAppUser appUser = AppUserUtil.getLoginAppUser();
        Assert.notNull(appUser, "????????????????????????");
        User userInfoData = new User();
        CompanyInfo companyInfoData = infoDTO.getCompanyInfo();
        Assert.notNull(companyInfoData, "????????????????????????!");

        CompanyInfo existCompanyInfo = null;
        companyInfoData = this.commonDealCode(companyInfoData, appUser, dataSource, orderStatus);

        if (companyInfoData.getCompanyId() != null) {
            existCompanyInfo = this.getById(companyInfoData.getCompanyId());
            if (ApproveStatusType.APPROVED.getValue().equals(existCompanyInfo.getStatus())) {
                throw new BaseException("?????????????????????????????????");
            } else if (ApproveStatusType.SUBMITTED.getValue().equals(existCompanyInfo.getStatus())) {
                if (!ApproveStatusType.APPROVED.getValue().equals(orderStatus)
                        && !ApproveStatusType.REJECTED.getValue().equals(orderStatus)) {
                    throw new BaseException("????????????,???????????????");
                }
            }
        }

        infoDTO.setCompanyInfo(companyInfoData);
        Long companyId = this.saveOrUpdateCompanyInfo(infoDTO.getCompanyInfo());
//        log.info("saveOrUpdateInfo?????????????????????id={},???????????????={}", companyId, appUser.getUserId());
//        //2020-10-22 ?????????????????????????????????????????????companyId????????????????????????????????????????????????????????????????????????????????????;
//        User bingUserCompanyId = rbacClient.getUser(new User().setUserId(appUser.getUserId())).setCompanyId(companyId);
//        rbacClient.binding(bingUserCompanyId);
//        log.info("?????????????????????-??????????????????????????????id={}???????????????", companyId, appUser.getUserId());
        this.saveUpdateInfos(infoDTO, companyId, false);
        //???????????????????????????????????????????????????????????????
        //????????????????????????????????????????????????????????????
        if (SupplierDataSourceType.ONESELF_REGISTER.getValue().equals(dataSource)) {
            userInfoData.setUserId(appUser.getUserId());
            //????????????????????????
            userBinding(userInfoData, companyId);
            //???????????????????????????????????????
            if (ApproveStatusType.SUBMITTED.getValue().equals(orderStatus)) {
                String operationMemo = "??????????????????";
                if (existCompanyInfo != null
                        && ApproveStatusType.REJECTED.getValue().
                        equals(existCompanyInfo.getStatus())) {
                    operationMemo = "???????????????????????????";
                }

                companyStatusLogService.saveStatusLog(companyId,
                        appUser.getUserId(),
                        appUser.getUsername(),
                        appUser.getUserType(),
                        companyInfoData.getStatus(),
                        operationMemo,
                        new Date(),
                        "???????????????"
                );
                /* Begin by chenwt24@meicloud.com   2020-09-27 */

                //?????????????????????????????????
                if(infoDTO.getOrgInfos().size() != 0 && infoDTO.getOrgCategorys().size() != 0) {
                    sendEmailForVendorSubmit(infoDTO);
                }

                /* End by chenwt24@meicloud.com     2020-09-27 */
            }

        }

        return companyId;
    }

    private synchronized void userBinding(User userInfoData, Long companyId) {
        User existUser = rbacClient.getUser(userInfoData);
        if (existUser != null && existUser.getCompanyId() == null) {
            userInfoData.setCompanyId(companyId);
            log.info("saveOrUpdateInfo?????????????????????-??????????????????????????????id={}", companyId);
            rbacClient.binding(userInfoData);
        }
    }

    /**
     * ??????????????????????????????erp
     *
     * @param companyId
     */
    public void sendVendorToErp(Long companyId) {
        log.info("?????????????????????????????????....");
        VendorInfoOutputParameters vendorInfoOutput = new VendorInfoOutputParameters();
        CompanyInfo companyInfo = this.getByCompanyId(companyId);
        if (Objects.nonNull(companyInfo) && StringUtils.isNotEmpty(companyInfo.getCompanyCode()) && StringUtils.isNotEmpty(companyInfo.getCompanyName())) {
            String vendorError = "";
            String vendorInfoReturnStatus = "";

            try {
                vendorInfoOutput = iVendorInfoService.sendVendorInfo(companyInfo);
                if (null != vendorInfoOutput) {
                    vendorInfoReturnStatus = null != vendorInfoOutput.getXesbresultinforec() ? vendorInfoOutput.getXesbresultinforec().getReturnstatus() : "E";
                    if (Objects.equals("S", vendorInfoReturnStatus)) {
                        log.info("?????????????????????????????????");
                        //??????erp???????????????????????????
                        repushHandlerService.save("???????????????????????????(??????????????????" + companyInfo.getCompanyCode() + ")", "???????????????:"+companyInfo.getCompanyCode(),
                                IVendorInfoService.class.getName(), "sendVendorInfo",
                                0, RepushStatus.SUCCESS, RepushConst.NOT_TO_REPUSH, null, null, companyInfo);

                        //???????????????erpVendorId???erpVendorCode????????????????????????
                        if (null != vendorInfoOutput.getXesbresultinforec()
                                &&StringUtils.isNotEmpty(vendorInfoOutput.getXesbresultinforec().getAttr1())
                                &&StringUtils.isNotEmpty(vendorInfoOutput.getXesbresultinforec().getAttr2())){
                            Long erpVendorId = Long.valueOf(vendorInfoOutput.getXesbresultinforec().getAttr1());
                            String erpVendorCode = ( vendorInfoOutput.getXesbresultinforec().getAttr2());
                            companyInfo.setErpVendorId(erpVendorId).setErpVendorCode(erpVendorCode);
                            this.updateById(companyInfo);
                        }
                    } else {
                        throw new BaseException("????????????????????????????????????????????????????????????[" + companyInfo.getCompanyName() + "]???erp?????????");
                    }
                }
            } catch (Exception e) {
                throw new BaseException("????????????????????????????????????????????????????????????[" + companyInfo.getCompanyName() + "]???erp?????????");
            }
        } else {
            log.error("?????????????????????????????????????????????erp???");
        }
    }

    /**
     * Description
     *
     * @Param isApprove???????????????????????????????????????????????????erp
     * @modifiedBy xiexh12@meicloud.com 10-08 11:10
     **/
    @Transactional(rollbackFor = Exception.class)
    public void saveUpdateInfos(InfoDTO infoDTO, Long companyId, boolean isApprove) {
        if (companyId != null) {
            if (infoDTO.getCompanyInfoDetail() != null) {
                CompanyInfoDetail companyInfoDetail = infoDTO.getCompanyInfoDetail();
                if (companyInfoDetail.getCompanyDetailId() == null) {
                    companyInfoDetail.setCompanyDetailId(IdGenrator.generate()).setCompanyId(companyId);
                    iCompanyInfoDetailService.save(companyInfoDetail);
                } else {
                    iCompanyInfoDetailService.updateById(companyInfoDetail);
                }
            }
            //??????????????????
            if (!CollectionUtils.isEmpty(infoDTO.getBankInfos())) {
                //??????????????????
                checkBankInfo(infoDTO);
                for (BankInfo bankInfo : infoDTO.getBankInfos()) {
                    bankInfoService.saveOrUpdateBank(bankInfo, companyId);
                }
            }
            //??????????????????
            if (!CollectionUtils.isEmpty(infoDTO.getSiteInfos())) {
                for (SiteInfo siteInfo : infoDTO.getSiteInfos()) {
                    siteInfoService.saveOrUpdateSite(siteInfo, companyId);
                }
            }
            //?????????????????????
            if (CollectionUtils.isEmpty(infoDTO.getContactInfos())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????"));
            }
            if (infoDTO.getContactInfos().size() > 1) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????"));
            }
            for (ContactInfo contactInfo : infoDTO.getContactInfos()) {
                if (contactInfo == null) continue;
                if (!"Y".equals(contactInfo.getCeeaDefaultContact())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????"));
                }
                contactInfoService.saveOrUpdateContact(contactInfo, companyId);
            }

            String dataSource = infoDTO.getCompanyInfo().getDataSources();

            /** ???????????????????????????????????????????????????erp **/
            if (dataSource.equals(SupplierDataSourceType.MANUALLY_CREATE.getValue()) && isApprove) {
                //??????????????????????????? ??????????????????????????????????????????????????????

                //2020-12-25 ???????????? ????????????erp????????????
//                sendVendorToErp(companyId);
//                submitExecutor.submit(new Runnable() {
//                    @Override
//                    public void run() {
//                        sendVendorToErp(companyId);
//                    }
//                });

                //???????????????erpVendorId
                Long erpVendorId = this.getById(companyId).getErpVendorId();

//                CompletableFuture.runAsync(() -> {
//
//                })

                // 2020-12-25 ?????????????????? ????????????erp??????
                //???????????????????????????
//                submitExecutor.execute(() -> {
//                    sendVendorBank(companyId, erpVendorId);
//                });
//                //???????????????????????????
//                submitExecutor.execute(() -> {
//                    sendVendorSite(companyId, erpVendorId);
//                });
//                //??????????????????????????????
//                submitExecutor.execute(() -> {
//                    sendVendorContact(companyId, erpVendorId);
//                });


            }

            if (!CollectionUtils.isEmpty(infoDTO.getFinanceInfos())) {
                for (FinanceInfo financeInfo : infoDTO.getFinanceInfos()) {
                    financeInfoService.saveOrUpdateFinace(financeInfo, companyId);
                }
            }
//            if (!CollectionUtils.isEmpty(infoDTO.getOrgInfos())) {
//                for (OrgInfo orgInfo : infoDTO.getOrgInfos()) {
//                    orgInfoService.saveOrUpdateOrg(orgInfo, companyId);
//                    if (!CollectionUtils.isEmpty(infoDTO.getOrgCategorys())) {
//                        for (OrgCategory orgCategory : infoDTO.getOrgCategorys()) {
//                            orgCategoryService.saveOrUpdateOrgCategory(orgCategory
//                                    .setOrgId(orgInfo.getOrgId())
//                                    .setOrgCode(orgInfo.getOrgCode())
//                                    .setOrgName(orgInfo.getOrgName()), companyId);
//                        }
//                    }
//                }
//            }
            List<OrgInfo> orgInfos = infoDTO.getOrgInfos();
            CompanyInfo companyInfo = infoDTO.getCompanyInfo();
            List<OrgCategory> orgCategories = infoDTO.getOrgCategorys();
            //????????????????????????
            addOrgCategory(companyId, orgInfos, companyInfo, orgCategories);

            if (!CollectionUtils.isEmpty(infoDTO.getBusinessInfos())) {
                List<BusinessInfo> businessInfos = infoDTO.getBusinessInfos();
                for (BusinessInfo businessInfo : businessInfos) {
                    if (businessInfo == null) continue;
                    if (businessInfo.getBusinessInfoId() == null) {
                        businessInfo.setBusinessInfoId(IdGenrator.generate())
                                .setCompanyId(companyId);
                        businessInfoService.save(businessInfo);
                    } else {
                        businessInfoService.updateById(businessInfo);
                    }
                }
            }
            if (infoDTO.getOperationInfo() != null) {
                OperationInfo operationInfo = infoDTO.getOperationInfo();
                long opInfoId = IdGenrator.generate();
                if (operationInfo.getOpInfoId() == null) {
                    operationInfo.setCompanyId(companyId).setOpInfoId(opInfoId);
                    operationInfoService.save(operationInfo);
                } else {
                    operationInfoService.updateById(operationInfo);
                }

                if (CollectionUtils.isNotEmpty(infoDTO.getOperationQualities())) {
                    List<OperationQuality> operationQualities = infoDTO.getOperationQualities();
                    for (OperationQuality operationQuality : operationQualities) {
                        if (operationQuality == null) continue;
                        if (operationQuality.getOpQualityId() == null) {
                            operationQuality.setOpQualityId(IdGenrator.generate()).setCompanyId(companyId).setOpInfoId(opInfoId);
                            iOperationQualityService.save(operationQuality);
                        } else {
                            iOperationQualityService.updateById(operationQuality);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(infoDTO.getOperationEquipments())) {
                    List<OperationEquipment> operationEquipments = infoDTO.getOperationEquipments();
                    for (OperationEquipment operationEquipment : operationEquipments) {
                        if (operationEquipment == null) continue;
                        if (operationEquipment.getOpEquipmentId() == null) {
                            operationEquipment.setOpEquipmentId(IdGenrator.generate()).setCompanyId(companyId).setOpInfoId(opInfoId);
                            iOperationEquipmentService.save(operationEquipment);
                        } else {
                            iOperationEquipmentService.updateById(operationEquipment);
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(infoDTO.getOperationProducts())) {
                    List<OperationProduct> operationProducts = infoDTO.getOperationProducts();
                    for (OperationProduct operationProduct : operationProducts) {
                        if (operationProduct == null) continue;
                        if (operationProduct.getOpProductId() == null) {
                            operationProduct.setOpProductId(IdGenrator.generate()).setCompanyId(companyId).setOpInfoId(opInfoId);
                            iOperationProductService.save(operationProduct);
                        } else {
                            iOperationProductService.updateById(operationProduct);
                        }
                    }
                }
            }
            if (infoDTO.getManagementInfo() != null) {
                ManagementInfo managementInfo = infoDTO.getManagementInfo();
                long managementInfoId = IdGenrator.generate();
                if (managementInfo.getManagementInfoId() == null) {
                    managementInfo.setCompanyId(companyId).setManagementInfoId(managementInfoId);
                    iManagementInfoService.save(managementInfo);
                } else {
                    iManagementInfoService.updateById(managementInfo);
                }
                if (CollectionUtils.isNotEmpty(infoDTO.getManagementAttaches())) {
                    List<ManagementAttach> managementAttaches = infoDTO.getManagementAttaches();
                    for (ManagementAttach managementAttach : managementAttaches) {
                        if (managementAttach == null) continue;
                        if (managementAttach.getManagementAttachId() == null) {
                            managementAttach.setManagementAttachId(IdGenrator.generate()).setManagementInfoId(managementInfoId).setCompanyId(companyId);
                            iManagementAttachService.save(managementAttach);
                        } else {
                            iManagementAttachService.updateById(managementAttach);
                        }
                    }
                }
            }
            if (!CollectionUtils.isEmpty(infoDTO.getFileUploads())) {
                attachFileService.saveOrUpdateAttachs(infoDTO.getFileUploads(), companyId);
            }
//            ceea,???????????????
//            if (infoDTO.getHolderInfo() != null) {
//                holderInfoService.saveOrUpdateHolder(infoDTO.getHolderInfo(), companyId);
//            }
//            if (infoDTO.getHonorInfo() != null) {
//                honorInfoService.saveOrUpdateHonor(infoDTO.getHonorInfo(), companyId);
//            }
//            if (infoDTO.getOtherInfo() != null) {
//                otherInfoService.saveOrUpdateOtherInfo(infoDTO.getOtherInfo(), companyId);
//            }
        }
    }

    private void checkBankInfo(InfoDTO infoDTO) {
        //???????????????????????????????????????
        Map<String, List<BankInfo>> listMap = infoDTO.getBankInfos().stream().collect(Collectors.groupingBy(bankInfo -> bankInfo.getBankAccount() + bankInfo.getCurrencyCode()));
        Collection<List<BankInfo>> values = listMap.values();
        if (CollectionUtils.isNotEmpty(values)) {
            values.forEach(value -> {
                if (value.size() > 1) {
                    BankInfo bankInfo = value.get(0);
                    throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????:" + bankInfo.getBankAccount() + "," + bankInfo.getCurrencyName() + "??????,?????????!"));
                }
            });
        }
        //???????????????????????????????????????
        List<BankInfo> bankInfos = infoDTO.getBankInfos().stream().filter(bankInfo -> (YesOrNo.YES.getValue().equals(bankInfo.getCeeaMainAccount()))).collect(Collectors.toList());
        if (bankInfos.size() == 0 || bankInfos.size() > 1) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????,?????????!"));
        }
    }

    /**
     * ??????????????????????????????Erp
     *
     * @param companyId
     * @param erpVendorId
     */
    @Override
    public void sendVendorSite(Long companyId, Long erpVendorId) {
        List<SiteInfo> sendErpSiteInfoList = iSiteInfoService.list(
                new QueryWrapper<>(new SiteInfo().setCompanyId(companyId).setIfPushErp("N"))
        );

        if (!CollectionUtils.isEmpty(sendErpSiteInfoList)) {
            log.info("????????????????????????" + sendErpSiteInfoList.size() + "???");
            String vendorSiteReturnStatus = "";

            for (SiteInfo siteInfo : sendErpSiteInfoList) {
                if (Objects.nonNull(siteInfo)) {
                    siteInfo.setErpVendorId(erpVendorId);
                    try {
                        log.info("?????????????????????????????????...");
                        VendorSiteOutputParameters vendorSiteOutput = iVendorSiteService.sendVendorSite(siteInfo);
                        if (Objects.nonNull(vendorSiteOutput)) {
                            vendorSiteReturnStatus = null != vendorSiteOutput.getXesbresultinforec() ? vendorSiteOutput.getXesbresultinforec().getReturnstatus() : "E";
                            if ("S".equals(vendorSiteReturnStatus)) {
                                log.info("????????????????????????????????????");
                                //??????erp?????????????????????????????????????????????????????????erp?????????Y
                                iSiteInfoService.updateById(siteInfo.setIfPushErp("Y"));
                                repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "???????????????/???????????????" + siteInfo.getOrgName() + "/" + siteInfo.getVendorSiteCode(), "?????????????????????id:"+siteInfo.getSiteInfoId(),
                                        IVendorSiteService.class.getName(), "sendVendorSite", 0, RepushStatus.SUCCESS, RepushConst.NOT_TO_REPUSH, null, null, siteInfo);
                            } else {
                                log.info("????????????????????????????????????????????????: " + JsonUtil.entityToJsonStr(vendorSiteOutput.getXesbresultinforec().getReturnmsg()));
                                //?????????????????????????????????
                                repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "???????????????/???????????????" + siteInfo.getOrgName() + "/" + siteInfo.getVendorSiteCode(), "?????????????????????id:"+siteInfo.getSiteInfoId(),
                                        IVendorSiteService.class.getName(), "sendVendorSite", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushSiteInfoCallBack(), null, siteInfo);
                            }
                        }
                    } catch (Exception e) {
                        log.error("??????????????????????????????erp ?????????: " + e);
                        //?????????????????????????????????
                        repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "???????????????/???????????????" + siteInfo.getOrgName() + "/" + siteInfo.getVendorSiteCode(), "?????????????????????id:"+siteInfo.getSiteInfoId(),
                                IVendorSiteService.class.getName(), "sendVendorSite", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushSiteInfoCallBack(), null, siteInfo);
                    }
                }
            }
        }
        /** ??????????????????????????????Erp ??????---------**/
    }

    /**
     * ?????????????????????????????????Erp
     *
     * @param companyId
     * @param erpVendorId
     */
    public void sendVendorContact(Long companyId, Long erpVendorId) {
        /** ?????????????????????????????????Erp ??????---------**/
        List<ContactInfo> sendContactInfoList = contactInfoService.list(
                new QueryWrapper<>(new ContactInfo().setCompanyId(companyId).setIfPushErp("N"))
        );
        ContactInfo contactInfo = new ContactInfo();
        if (CollectionUtils.isNotEmpty(sendContactInfoList) && Objects.nonNull(sendContactInfoList.get(0))) {
            contactInfo = sendContactInfoList.get(0);
            contactInfo.setErpVendorId(erpVendorId);
            String vendorContactReturnStatus = "";

            try {
                log.info("????????????????????????????????????...");
                VendorContactOutputParameters vendorContactOutput = iVendorContactService.sendVendorContact(contactInfo);
                if (Objects.nonNull(vendorContactOutput)) {
                    vendorContactReturnStatus = null != vendorContactOutput.getXesbresultinforec() ? vendorContactOutput.getXesbresultinforec().getReturnstatus() : "E";
                    if ("S".equals(vendorContactReturnStatus)) {
                        log.info("???????????????????????????????????????");
                        //??????erp???????????????????????????
                        contactInfoService.updateById(contactInfo.setIfPushErp("Y"));
                        repushHandlerService.save("?????????????????????????????????erp?????????Id: " + erpVendorId + "???????????????" + contactInfo.getContactName(), "????????????????????????id:"+contactInfo.getContactInfoId(),
                                IVendorContactService.class.getName(), "sendVendorContact", 0, RepushStatus.SUCCESS, RepushConst.NOT_TO_REPUSH, null, null, contactInfo);
                    } else {
                        log.info("???????????????????????????????????????????????????: " + JsonUtil.entityToJsonStr(vendorContactOutput.getXesbresultinforec().getReturnmsg()));
                        //?????????????????????????????????
                        repushHandlerService.save("?????????????????????????????????erp?????????Id: " + erpVendorId + "???????????????" + contactInfo.getContactName(), "????????????????????????id:"+contactInfo.getContactInfoId(),
                                IVendorContactService.class.getName(), "sendVendorContact", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushContactInfoCallBack(), null, contactInfo);
                    }
                }
            } catch (Exception e) {
                //?????????????????????????????????
                log.error("?????????????????????????????????erp?????????: " + e);
                repushHandlerService.save("?????????????????????????????????erp?????????Id: " + erpVendorId + "???????????????" + contactInfo.getContactName(), "????????????????????????id:"+contactInfo.getContactInfoId(),
                        IVendorContactService.class.getName(), "sendVendorContact", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushContactInfoCallBack(), null, contactInfo);
            }
        }
        /** ?????????????????????????????????Erp ??????---------**/
    }

    /**
     * ??????????????????????????????erp
     *
     * @param companyId
     * @param erpVendorId
     */
    public void sendVendorBank(Long companyId, Long erpVendorId) {
        List<BankInfo> sendErpBankInfoList = iBankInfoService.list(
                new QueryWrapper<>(new BankInfo().setCompanyId(companyId).setIfPushErp("N"))
        );
        if (CollectionUtils.isNotEmpty(sendErpBankInfoList)) {
            log.info("????????????????????????" + sendErpBankInfoList.size() + "???");
            String vendorBankReturnStatus = "";

            for (BankInfo bankInfo : sendErpBankInfoList) {
                if (Objects.nonNull(bankInfo)) {
                    bankInfo.setErpVendorId(erpVendorId);
                    try {
                        log.info("?????????????????????????????????...");
                        VendorBankOutputParameters vendorBankOutput = iVendorBankService.sendVendorBank(bankInfo);
                        if (Objects.nonNull(vendorBankOutput)) {
                            vendorBankReturnStatus = null != vendorBankOutput.getXesbresultinforec() ? vendorBankOutput.getXesbresultinforec().getReturnstatus() : "E";
                            if (Objects.equals("S", vendorBankReturnStatus)) {
                                log.info("????????????????????????????????????");
                                //??????erp?????????????????????????????????????????????????????????erp?????????Y
                                iBankInfoService.updateById(bankInfo.setIfPushErp("Y"));
                                repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "?????????/?????????" + bankInfo.getBankName() + "/" + bankInfo.getOpeningBank(), "?????????????????????id:"+bankInfo.getBankInfoId(),
                                        IVendorBankService.class.getName(), "sendVendorBank", 0, RepushStatus.SUCCESS, RepushConst.NOT_TO_REPUSH, null, null, bankInfo);
                            } else {
                                log.info("???????????????????????????????????????????????????" + JsonUtil.entityToJsonStr(vendorBankOutput.getXesbresultinforec().getReturnmsg()));
                                //?????????????????????????????????
                                repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "?????????/?????????" + bankInfo.getBankName() + "/" + bankInfo.getOpeningBank(), "?????????????????????id:"+bankInfo.getBankInfoId(),
                                        IVendorBankService.class.getName(), "sendVendorBank", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushBankInfoCallBack(), null, bankInfo);
                            }
                        }
                    } catch (Exception e) {
                        log.error("??????????????????????????????erp?????????: " + e);
                        //?????????????????????????????????
                        repushHandlerService.save("??????????????????????????????erp?????????Id: " + erpVendorId + "?????????/?????????" + bankInfo.getBankName() + "/" + bankInfo.getOpeningBank(), "?????????????????????id:"+bankInfo.getBankInfoId(),
                                IVendorBankService.class.getName(), "sendVendorBank", 10, RepushStatus.FAIL, RepushConst.TO_REPUSH, new PushBankInfoCallBack(), null, bankInfo);

                    }
                }
            }
        }
        /** ??????????????????????????????Erp ??????---------**/
    }

    /**
     * ????????????????????????????????????
     * ??????????????????????????????????????????
     *
     * @param siteInfos
     * @modifiedBy xiexh12@meicloud.com 2020-10-08 11:29
     */
    public void orgSiteDuplicateCheck(List<SiteInfo> siteInfos) {
        Set<String> set = new HashSet<>();
        if (CollectionUtils.isNotEmpty(siteInfos)) {
            for (SiteInfo siteInfo : siteInfos) {
                String orgId = String.valueOf(siteInfo.getBelongOprId());
                String vendorSiteCode = siteInfo.getVendorSiteCode();
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
     * ????????????
     *
     * @param companyId
     */
    @Transactional
    @Override
    public void abandon(Long companyId) {
        InfoDTO infoByParam = this.getInfoByParam(companyId);
        CompanyInfo companyInfo = infoByParam.getCompanyInfo();
        Assert.notNull(companyInfo,"?????????????????????????????????");
        String status = companyInfo.getStatus();
        Assert.isTrue(ApproveStatusType.REJECTED.getValue().equals(status)|| ApproveStatusType.WITHDRAW.getValue().equals(status),"?????????????????????????????????");
        companyInfo.setStatus(PurchaseOrderEnum.ABANDONED.getValue());
        this.updateById(companyInfo);
        SrmFlowBusWorkflow srmworkflowForm = baseClient.getSrmFlowBusWorkflow(companyId);
        if (srmworkflowForm!=null) {
            try {
                infoByParam.setProcessType("N");
                greenChannelFlow.submitGreenChannelConfFlow(infoByParam);
            } catch (Exception e) {
                Assert.isTrue(false, "??????????????????????????????");
            }
        }
    }


    /**
     * ????????????????????????
     * @param companyId
     * @param orgInfos
     * @param companyInfo
     * @param orgCategories
     */
    private void addOrgCategory(Long companyId, List<OrgInfo> orgInfos, CompanyInfo companyInfo, List<OrgCategory> orgCategories) {
        String orgCategoryStatus = "";
        String formType = "";
        if (SupplierDataSourceType.MANUALLY_CREATE.name().equals(companyInfo.getDataSources()) && ApproveStatusType.APPROVED.getValue().equals(companyInfo.getStatus())) {
            orgCategoryStatus = CategoryStatus.GREEN.name();
            formType = FormType.GREEN_CHANNEL.name();
        }
        if (SupplierDataSourceType.ONESELF_REGISTER.name().equals(companyInfo.getDataSources())) {
            orgCategoryStatus = CategoryStatus.REGISTERED.name();
            formType = FormType.SUPPLIER_REGISTRATION.name();
        }
        if (!CollectionUtils.isEmpty(orgInfos)) {
            for (OrgInfo orgInfo : orgInfos) {
                if (orgInfo == null) continue;
                orgInfoService.saveOrUpdateOrg(orgInfo, companyId);
            }
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
                        .setServiceStatus(orgCategoryStatus));
                List<OrgCategoryChange> orgCategoryChanges = new ArrayList<>();
                orgCategoryChanges.add(new OrgCategoryChange().setFormType(formType).setFormNum(companyInfo.getCompanyCode()));
                orgCategorySaveDTO.setOrgCategoryChanges(orgCategoryChanges);
                orgCategoryService.collectOrgCategory(orgCategorySaveDTO);
            }
        }
    }


    @Override
    public Long saveOrUpdateCompanyInfo(CompanyInfo companyInfo) {
        if (companyInfo.getCompanyId() != null) {
            companyInfo.setLastUpdateDate(new Date());
        } else {
            companyInfo.setCreationDate(new Date());
            Long id =AppUserUtil.getLoginAppUser().getCompanyId();
            if(id == null){
                id = IdGenrator.generate();
            }
            companyInfo.setCompanyId(id);
        }
        this.saveOrUpdate(companyInfo);
        return companyInfo.getCompanyId();
    }

    @Override
    public InfoDTO getInfoByParam(Long companyId) {
        InfoDTO infoDTO = new InfoDTO();
        if (companyId != null) {
            CompanyInfo companyInfo = this.getById(companyId);
            List<BankInfo> bankInfos = bankInfoService.list(new QueryWrapper<>(new BankInfo().setCompanyId(companyId)));
            List<SiteInfo> siteInfos = siteInfoService.list(new QueryWrapper<>(new SiteInfo().setCompanyId(companyId)));
            List<ContactInfo> contactInfos = contactInfoService.list(new QueryWrapper<>(new ContactInfo().setCompanyId(companyId)));
            List<FinanceInfo> financeInfos = financeInfoService.list(new QueryWrapper<>(new FinanceInfo().setCompanyId(companyId)));
            List<OrgCategory> orgCategorys = orgCategoryService.list(new QueryWrapper<>(new OrgCategory().setCompanyId(companyId)));
            // ????????????
            List<OrgInfo> orgInfos = orgInfoService.queryOrgInfoByVendorId(companyId);
            List<BusinessInfo> businessInfos = businessInfoService.list(new QueryWrapper<>(new BusinessInfo().setCompanyId(companyId)));
            CompanyInfoDetail companyInfoDetail = iCompanyInfoDetailService.getOne(new QueryWrapper<>(new CompanyInfoDetail().setCompanyId(companyId)).last("LIMIT 1"));
            OperationInfo operationInfo = operationInfoService.getOne(new QueryWrapper<>(new OperationInfo().setCompanyId(companyId)).last("LIMIT 1"));
            List<OperationQuality> operationQualities = iOperationQualityService.list(new QueryWrapper<>(new OperationQuality().setCompanyId(companyId)));
            List<OperationProduct> operationProducts = iOperationProductService.list(new QueryWrapper<>(new OperationProduct().setCompanyId(companyId)));
            List<OperationEquipment> operationEquipments = iOperationEquipmentService.list(new QueryWrapper<>(new OperationEquipment().setCompanyId(companyId)));
            ManagementInfo managementInfo = iManagementInfoService.getOne(new QueryWrapper<>(new ManagementInfo().setCompanyId(companyId)).last("LIMIT 1"));
            List<ManagementAttach> managementAttaches = iManagementAttachService.list(new QueryWrapper<>(new ManagementAttach().setCompanyId(companyId)));
            User user = rbacClient.queryByCompanyId(companyId);
            List<CompanyStatusLog> companyStatusLogs = iCompanyStatusLogService.list(new QueryWrapper<>(new CompanyStatusLog().setCompanyId(companyId)));
            //???????????????????????????????????????????????????????????????supplier leader??????????????????
            List<SupplierLeader> supplierLeaderList = supplierLeaderService.list(Wrappers.lambdaQuery(SupplierLeader.class)
                    .eq(SupplierLeader::getCompanyId, companyId)
            );
            InfoChange infoChange = new InfoChange().setCompanyId(companyId);
            if (CollectionUtils.isNotEmpty(supplierLeaderList) && Objects.nonNull(supplierLeaderList.get(0))){
                infoChange.setNoticeById(supplierLeaderList.get(0).getResponsibilityId());
                infoChange.setNoticeByName(supplierLeaderList.get(0).getResponsibilityName());
            }
            //            ceea,???????????????
//            HolderInfo holderInfo = holderInfoService.getByCompanyId(companyId);
//            HonorInfo honorInfo = honorInfoService.getByCompanyId(companyId);
//            OtherInfo otherInfo = otherInfoService.getByCompanyId(companyId);
//            List<CategoryRel> categoryRels = iCategoryRelService.list(new QueryWrapper<>(new CategoryRel().setCompanyId(companyId)));
//            companyInfo.setCategoryRels(categoryRels);
            //??????companyId??????????????????
            infoDTO.setCompanyInfo(companyInfo);
            infoDTO.setBankInfos(bankInfos);
            infoDTO.setSiteInfos(siteInfos);
            infoDTO.setContactInfos(contactInfos);
            infoDTO.setFinanceInfos(financeInfos);
            infoDTO.setOperationInfo(operationInfo);
            infoDTO.setOrgCategorys(orgCategorys);
            infoDTO.setOrgInfos(orgInfos);
            infoDTO.setUserInfo(user);
            infoDTO.setCompanyInfoDetail(companyInfoDetail);
            infoDTO.setBusinessInfos(businessInfos);
            infoDTO.setOperationQualities(operationQualities);
            infoDTO.setOperationProducts(operationProducts);
            infoDTO.setOperationEquipments(operationEquipments);
            infoDTO.setManagementInfo(managementInfo);
            infoDTO.setManagementAttaches(managementAttaches);
            infoDTO.setCompanyStatusLogs(companyStatusLogs);
            infoDTO.setInfoChange(infoChange);
            QuestSupplierDto questSupplierDto=new QuestSupplierDto();
            questSupplierDto.setCompanyIdForQuery(companyId);
            questSupplierDto.setApprovalStatus(QuestSupplierApproveStatusEnum.APPROVED.getValue());
            //????????????
            LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
            List<OrganizationUser> organizations = loginAppUser.getOrganizationUsers();
            List<String> questTemplateOrgIdList=organizations.stream().map(organizationUser->organizationUser.getOrganizationId().toString()).collect(Collectors.toList());
            questSupplierDto.setQuestTemplateOrgIdList(questTemplateOrgIdList);
            PageInfo<QuestSupplierVo> questSupplierPage=questSupplierService.listPageByParm(questSupplierDto);
            if(questSupplierPage!=null&&CollectionUtils.isNotEmpty(questSupplierPage.getList())) {
                infoDTO.setQuestSupplierList(questSupplierPage.getList());
            }
//            ceea,???????????????
//            infoDTO.setHolderInfo(holderInfo);
//            infoDTO.setHonorInfo(honorInfo);
//            infoDTO.setOtherInfo(otherInfo);
        }
        return infoDTO;
    }

    @Override
    public void startUpOrBlockUpReminder(Long id, String isReminder) {
        CompanyInfo companyInfo =new CompanyInfo();
        companyInfo.setCompanyId(id);
        companyInfo.setIsUseReminder(isReminder);
        this.updateById(companyInfo);
    }

    @Override
    public void updateUpReminder(ManagementAttach managementAttach) {
        CompanyInfo companyInfo =new CompanyInfo();
        //id??????????????????xml
        companyInfo.setCompanyId(managementAttach.getManagementAttachId());
        companyInfo.setBusinessLicense(managementAttach.getAuthType());
        companyInfo.setBusinessLicenseFileId(managementAttach.getFileId().toString());
        companyInfo.setBusinessEndDate(managementAttach.getEndDate());
        this.updateById(companyInfo);
    }

    @Override
    public CompanyInfo getByCompanyId(Long companyId) {
        CompanyInfo companyInfo = this.getById(companyId);
        if (companyInfo != null) {
            Map<String, Object> dimFieldContexts = dimFieldContextService.findByOrderId(companyInfo.getCompanyId());
            companyInfo.setDimFieldContexts(dimFieldContexts);
        }
        return companyInfo;
    }

    @Override
    @Transactional
    public Long saveCompanyGreenChannel(InfoDTO infoDTO) {
        return this.saveOrUpdateGreenChannel(infoDTO, ApproveStatusType.DRAFT.getValue(),
                SupplierDataSourceType.MANUALLY_CREATE.getValue(), false);
    }

    /**
     * ??????????????????
     *
     * @param infoDTO
     */
    @Override
    @Transactional
    public Long companyGreenChannelSubmit(InfoDTO infoDTO) {
        Assert.isTrue(CollectionUtils.isNotEmpty(infoDTO.getContactInfos()), "????????????????????????????????????");
        checkContactsBeforeSubmit(infoDTO.getContactInfos());
        Assert.isTrue(CollectionUtils.isNotEmpty(infoDTO.getBankInfos()), "?????????????????????????????????");
        checkBanksBeforeSubmit(infoDTO.getBankInfos());
        Assert.isTrue(CollectionUtils.isNotEmpty(infoDTO.getSiteInfos()), "?????????????????????????????????");
        checkSitesBeforeSubmit(infoDTO.getSiteInfos());
        //????????????????????? ?????????????????????????????????????????????
        orgSiteDuplicateCheck(infoDTO.getSiteInfos());
        Long aLong = this.saveOrUpdateGreenChannel(infoDTO, ApproveStatusType.SUBMITTED.getValue(),
                SupplierDataSourceType.MANUALLY_CREATE.getValue(), false);

        /* Begin by chenwt24@meicloud.com   2020-10-16 */

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String formId = null;
                try {
                    formId = greenChannelFlow.submitGreenChannelConfFlow(infoDTO);
                } catch (Exception e) {
                    throw new BaseException(e.getMessage());
                }
                if (StringUtils.isEmpty(formId)) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("??????OA????????????"));
                }
            }
        });

        /* End by chenwt24@meicloud.com     2020-10-16 */

        return aLong;

    }

    /**
     * ??????????????????
     *
     * @param infoDTO
     */
    @Override
    public Long companyGreenChannelApprove(InfoDTO infoDTO) {
        return this.saveOrUpdateGreenChannel(infoDTO, ApproveStatusType.APPROVED.getValue(),
                SupplierDataSourceType.MANUALLY_CREATE.getValue(), true);
    }


    private CompanyInfo commonDealCode(CompanyInfo companyInfoData,
                                       LoginAppUser appUser,
                                       String dataSource,
                                       String orderStatus) {
        CompanyInfo existCompanyInfo = new CompanyInfo();
        //??????????????????????????????
        if (Objects.nonNull(companyInfoData) && Objects.nonNull(companyInfoData.getCompanyId())) {
            existCompanyInfo = this.getById(companyInfoData.getCompanyId());
            Assert.notNull(existCompanyInfo, "?????????????????????");
            //??????????????????????????????
            if (StringUtils.isNoneBlank(companyInfoData.getCompanyName())) {
                this.checkSameCompanyName(companyInfoData.getCompanyName(), companyInfoData.getCompanyId());
            }
            //??????????????????????????????????????????
            if (StringUtils.isNoneBlank(companyInfoData.getLcCode())) {
                this.checkSameLcCode(companyInfoData.getLcCode(), companyInfoData.getCompanyId());
            }
            //??????????????????????????????
            if (StringUtils.isNotBlank(existCompanyInfo.getIsBacklist()) &&
                    !existCompanyInfo.getIsBacklist().equals(companyInfoData.getIsBacklist())) {
                companyInfoData.setBacklistUpdatedDate(new Date());
                companyInfoData.setBacklistUpdatedBy(appUser.getUsername());
            }
//            ceea,???????????????
//            //????????????????????????????????????????????????????????????????????????????????????????????????
//            dimFieldContextService.deleteByCompanyId(companyInfoData.getCompanyId());

        } else {
            if (StringUtils.isNoneBlank(companyInfoData.getCompanyName())) {
                this.checkSameCompanyName(companyInfoData.getCompanyName(), null);
            }
            if (StringUtils.isNoneBlank(companyInfoData.getLcCode())) {
                this.checkSameLcCode(companyInfoData.getLcCode(), null);
            }
            //?????????????????????????????????????????????????????????????????????????????????
            if (StringUtils.isBlank(companyInfoData.getIsBacklist())) {
                companyInfoData.setIsBacklist(Enable.N.toString());
            } else if (Enable.Y.toString().equals(companyInfoData.getIsBacklist())) {
                companyInfoData.setBacklistUpdatedDate(new Date());
                companyInfoData.setBacklistUpdatedBy(appUser.getUsername());
            }
        }

        companyInfoData.setDataSources(dataSource);
        companyInfoData.setStatus(orderStatus);
        companyInfoData.setStatusName(ApproveStatusType.get(orderStatus).getName());
        //??????
        if (ApproveStatusType.SUBMITTED.getValue().equals(orderStatus)) {
            if (companyInfoData.getApplicationNumber() == null) {
                companyInfoData.setApplicationDate(new Date());
                companyInfoData.setApplicationNumber(baseClient.seqGen(SequenceCodeConstant.SEQ_SUP_APPLICATION_NUM));
            }
        } else if (ApproveStatusType.APPROVED.getValue().equals(orderStatus)) {
            companyInfoData.setApprovedDate(DateChangeUtil.asLocalDate(new Date()));
            companyInfoData.setApprovedBy(appUser.getUsername());
            companyInfoData.setApprover(appUser.getUsername());

            //???????????????????????????code
            if (StringUtils.isBlank(existCompanyInfo.getCompanyCode())) {
                companyInfoData.setCompanyCode(baseClient.seqGen(SequenceCodeConstant.SEQ_SUP_COMPANY_CODE));
            } else {
                companyInfoData.setCompanyCode(existCompanyInfo.getCompanyCode());
            }
        }
        return companyInfoData;
    }

    /**
     * Description
     *
     * @Param isApprove ?????????????????????true
     **/
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdateGreenChannel(InfoDTO infoDTO, String orderStatus, String dataSource, boolean isApprove) {
        //?????????????????????????????????
        LoginAppUser appUser = AppUserUtil.getLoginAppUser();
        if (isApprove){
            Assert.notNull(infoDTO.getCompanyInfo(),"???????????????????????????");
            QueryWrapper<CompanyStatusLog> companyStatusLogQueryWrapper = new QueryWrapper<>(new CompanyStatusLog().setCompanyId(infoDTO.getCompanyInfo().getCompanyId()));
            List<CompanyStatusLog> list = companyStatusLogService.list(companyStatusLogQueryWrapper);
            Assert.isTrue(CollectionUtils.isNotEmpty(list),"???????????????????????????????????????");
            CompanyStatusLog companyStatusLog = list.get(0);
            appUser=new LoginAppUser();
            appUser.setUsername(companyStatusLog.getOperatorName());
            appUser.setUserId(companyStatusLog.getOperatorId());
            appUser.setUserType(companyStatusLog.getOperationType());

        }else {
            Assert.notNull(appUser, "????????????????????????");
        }
        // ??????????????????
        CompanyInfo companyInfoData = infoDTO.getCompanyInfo();
        // ??????????????????
        User userInfoData = infoDTO.getUserInfo();

        Long companyId = null;

        //??????????????????????????????
        companyInfoData = this.commonDealCode(companyInfoData, appUser, dataSource, orderStatus);

        //??????????????????????????????????????????????????????????????????????????????
        Assert.notNull(userInfoData, "??????????????????????????????");
        Assert.notNull(userInfoData.getPassword(), "??????????????????");
        Assert.notNull(userInfoData.getUsername(), "??????????????????");
        Assert.notNull(userInfoData.getNickname(), "??????????????????");
        Assert.notNull(userInfoData.getEmail(), "??????????????????");
        infoDTO.setCompanyInfo(companyInfoData);
        companyId = this.saveOrUpdateCompanyInfo(infoDTO.getCompanyInfo());

        this.saveUpdateInfos(infoDTO, companyId, isApprove);
        if (userInfoData.getUserId() == null && !isApprove) {
            //??????????????????  ToDo ?????????????????????????????????????
            userInfoData.setCompanyId(companyId);
            Long userId = rbacClient.registerVendor(userInfoData);
            userInfoData.setUserId(userId);
        }

        //???????????????????????????????????????????????????????????????
        //????????????????????????
        if (ApproveStatusType.APPROVED.getValue().equals(orderStatus)) {
            //????????????????????????????????????????????????????????????????????????????????????
            InfoDTO infoByParam = this.getInfoByParam(companyId);
            if (YesOrNo.YES.getValue().equals(infoByParam.getCompanyInfo().getIsBacklist())) {
                List<OrgInfo> orgInfos = infoByParam.getOrgInfos();
                List<OrgCategory> orgCategorys = infoByParam.getOrgCategorys();
                for (OrgInfo orgInfo : orgInfos) {
                    if (!com.midea.cloud.common.enums.review.OrgStatus.INVALID.toString().equals(orgInfo.getServiceStatus())) {
                        orgInfo.setServiceStatus(com.midea.cloud.common.enums.review.OrgStatus.INVALID.toString());
                        orgInfo.setEndDate(DateChangeUtil.asLocalDate(new Date()));
                        orgInfoService.updateById(orgInfo);
                    }
                }
                for (OrgCategory orgCategory : orgCategorys) {
                    if (!com.midea.cloud.common.enums.review.CategoryStatus.TERMINATION.toString().equals(orgCategory.getServiceStatus())) {
                        orgCategory.setServiceStatus(CategoryStatus.TERMINATION.toString());
                        orgCategory.setEndDate(DateChangeUtil.asLocalDate(new Date()));
                        orgCategoryService.updateById(orgCategory);
                    }
                }
            }

            //?????????????????????????????????????????????????????????
            if (isApprove) {
                rbacClient.initUserRole(userInfoData.getUserId(), RoleType.SUPPLIER_INIT.name());
            }
        }
        companyStatusLogService.saveStatusLog(companyId,
                appUser.getUserId(),
                appUser.getUsername(),
                appUser.getUserType(),
                companyInfoData.getStatus(),
                "????????????",
                new Date(),
                "?????????????????????"
        );

        return companyId;
    }

    public void checkSameCompanyName(String companyName, Long companyId) {
        if (StringUtils.isNoneBlank(companyName)) {
            QueryWrapper<CompanyInfo> wrapper = new QueryWrapper<CompanyInfo>();
            wrapper.eq("COMPANY_NAME", companyName);
            if (companyId != null) {
                wrapper.ne("COMPANY_ID", companyId);
            }
            List<CompanyInfo> list = this.list(wrapper);
            if (!CollectionUtils.isEmpty(list)) {
                throw new BaseException("??????????????????");
            }
        }
    }

    public void checkSameLcCode(String lcCode, Long companyId) {
        if (StringUtils.isNoneBlank(lcCode)) {
            QueryWrapper<CompanyInfo> wrapper = new QueryWrapper<CompanyInfo>();
            wrapper.eq("LC_CODE", lcCode);
            if (companyId != null) {
                wrapper.ne("COMPANY_ID", companyId);
            }
            List<CompanyInfo> list = this.list(wrapper);
            if (!CollectionUtils.isEmpty(list)) { throw new BaseException("??????????????????????????????");
            }
        }
    }

    /**
     * ???????????????????????????
     *
     * @param siteInfos
     */
    public void checkSitesBeforeSubmit(List<SiteInfo> siteInfos) {
        if (CollectionUtils.isNotEmpty(siteInfos)) {
            for (SiteInfo siteInfo : siteInfos) {
                if (Objects.nonNull(siteInfo)) {
                    if (null == siteInfo.getOrgId())
                        throw new BaseException("????????????????????????????????????");
                    if (StringUtils.isEmpty(siteInfo.getVendorSiteCode()))
                        throw new BaseException("????????????????????????????????????");
                    if (StringUtils.isEmpty(siteInfo.getCountry()))
                        throw new BaseException("??????????????????????????????");
                    if (StringUtils.isEmpty(siteInfo.getAddressDetail()))
                        throw new BaseException("????????????????????????????????????");
                }
            }
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param contactInfos
     */
    public void checkContactsBeforeSubmit(List<ContactInfo> contactInfos) {
        if (CollectionUtils.isNotEmpty(contactInfos)) {
            for (ContactInfo contactInfo : contactInfos) {
                if (StringUtils.isEmpty(contactInfo.getContactName())) {
                    throw new BaseException("?????????????????????????????????");
                }
            }
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param bankInfos
     */
    public void checkBanksBeforeSubmit(List<BankInfo> bankInfos) {
        if (CollectionUtils.isNotEmpty(bankInfos)) {
            for (BankInfo bankInfo : bankInfos) {
                if (StringUtils.isEmpty(bankInfo.getBankCode()))
                    throw new BaseException("??????????????????????????????????????????");
                if (StringUtils.isEmpty(bankInfo.getBankName()))
                    throw new BaseException("??????????????????????????????????????????");
                if (StringUtils.isEmpty(bankInfo.getOpeningBank()))
                    throw new BaseException("?????????????????????????????????????????????");
                if (StringUtils.isEmpty(bankInfo.getUnionCode()))
                    throw new BaseException("??????????????????????????????????????????");
            }
        }
    }

    @Override
    public List<OrgCateServiceStatusDTO> listOrgCateServiceStatusByCompanyId(Long companyId) {
        Assert.notNull(companyId, "companyId????????????");
        List<OrgCategory> orgCategories = orgCategoryService.listOrgCategoryByCompanyId(companyId);
        List<OrgInfo> orgInfos = orgInfoService.listOrgInfoByCompanyId(companyId);
        List<OrgCateServiceStatusDTO> orgCateServiceStatusDTOs = new ArrayList<>();

        if (!CollectionUtils.isEmpty(orgCategories)) {
            for (OrgCategory orgCategory : orgCategories) {
                OrgCateServiceStatusDTO orgCateServiceStatusDTO = new OrgCateServiceStatusDTO();
                if (orgCategory == null) continue;
                Long vendorId = orgCategory.getCompanyId();
                Long orgId = orgCategory.getOrgId();
                if (orgId != null) {
                    QueryWrapper<OrgInfo> queryWrapper = new QueryWrapper<>(new OrgInfo().setOrgId(orgId)
                            .setCompanyId(vendorId));
                    //TODO ???????????????,??????????????????
                    OrgInfo orgInfo = null;
                    List<OrgInfo> orgInfoList = orgInfoService.list(queryWrapper);
                    if (!CollectionUtils.isEmpty(orgInfoList)) {
                        orgInfo = orgInfoList.get(0);
                    }
                    orgCateServiceStatusDTO.setOrgInfo(orgInfo);
                }
                orgCateServiceStatusDTO.setOrgCategory(orgCategory);
                orgCateServiceStatusDTOs.add(orgCateServiceStatusDTO);
            }
        } else {
            if (!CollectionUtils.isEmpty(orgInfos)) {
                for (OrgInfo orgInfo : orgInfos) {
                    OrgCateServiceStatusDTO orgCateServiceStatusDTO = new OrgCateServiceStatusDTO();
                    orgCateServiceStatusDTO.setOrgInfo(orgInfo);
                    orgCateServiceStatusDTOs.add(orgCateServiceStatusDTO);
                }
            }
        }

        return orgCateServiceStatusDTOs;
    }

    @Override
    public OrgCateServiceStatusDTO getOrgCateServiceStatusById(Long orgId, Long categoryId, Long vendorId) {
        OrgCateServiceStatusDTO orgCateServiceStatusDTO = new OrgCateServiceStatusDTO();
        //?????????????????????
        OrgCategory orgCategoryEntity = new OrgCategory();
        QueryWrapper<OrgCategory> orgCategoryQueryWrapper = new QueryWrapper<>(orgCategoryEntity
                .setOrgId(orgId)
                .setCategoryId(categoryId)
                .setCompanyId(vendorId));
        //TODO  ?????????????????????,???????????????
        OrgCategory orgCategory = null;
        List<OrgCategory> orgCategories = orgCategoryService.list(orgCategoryQueryWrapper);
        if (!CollectionUtils.isEmpty(orgCategories)) {
            orgCategory = orgCategories.get(0);
        }
        //??????????????????
        QueryWrapper<OrgInfo> orgInfoQueryWrapper = new QueryWrapper<>(new OrgInfo()
                .setOrgId(orgId)
                .setCompanyId(vendorId));
        OrgInfo orgInfo = null;
        List<OrgInfo> orgInfos = orgInfoService.list(orgInfoQueryWrapper);
        if (!CollectionUtils.isEmpty(orgInfos)) {
            orgInfo = orgInfos.get(0);
        }
        //??????OrgCateServiceStatusDTO
        if (orgCategory != null) {
            orgCateServiceStatusDTO.setOrgCategory(orgCategory);
            orgCateServiceStatusDTO.setOrgInfo(orgInfo);
        } else {
            orgCateServiceStatusDTO.setOrgInfo(orgInfo);
        }
        return orgCateServiceStatusDTO;
    }

    @Override
    public PageInfo<CompanyInfo> listPageByDTO(CompanyRequestDTO companyRequestDTO) {
        PageUtil.startPage(companyRequestDTO.getPageNum(), companyRequestDTO.getPageSize());//add by zhenggh9 20210419
        QueryWrapper<CompanyInfo> wrapper = new QueryWrapper<CompanyInfo>();
        if (StringUtils.isNoneBlank(companyRequestDTO.getCompanyName())) {
            wrapper.like("COMPANY_NAME", companyRequestDTO.getCompanyName());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getCompanyType())) {
            wrapper.like("COMPANY_TYPE", companyRequestDTO.getCompanyType());
        }
        //???????????????????????????????????????????????????
        if(!StringUtils.equals(companyRequestDTO.getDataSources(),"IMAGE")){
            wrapper.eq("DATA_SOURCES", SupplierDataSourceType.MANUALLY_CREATE.getValue());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getLcCode())) {
            wrapper.like("LC_CODE", companyRequestDTO.getLcCode());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getOverseasRelation())) {
            wrapper.eq("OVERSEAS_RELATION", companyRequestDTO.getOverseasRelation());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getStatus())) {
            wrapper.eq("STATUS", companyRequestDTO.getStatus());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getIsBacklist())) {
            wrapper.eq("IS_BACKLIST", companyRequestDTO.getIsBacklist());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getLegalPerson())) {
            wrapper.like("LEGAL_PERSON", companyRequestDTO.getLegalPerson());
        }
        if (companyRequestDTO.getStartDate() != null) {
            wrapper.ge("APPROVED_DATE", companyRequestDTO.getStartDate());
        }
        if (companyRequestDTO.getEndDate() != null) {
            wrapper.le("APPROVED_DATE", companyRequestDTO.getEndDate());
        }
        if (companyRequestDTO.getKeyWord() != null) {
            wrapper.like("COMPANY_NAME", companyRequestDTO.getKeyWord())
                    .or()
                    .like("COMPANY_CODE", companyRequestDTO.getKeyWord());
        }
        if (companyRequestDTO.getCompanyId() != null) {
            wrapper.eq("COMPANY_ID", companyRequestDTO.getCompanyId());
        }
        if (Objects.equals(YesOrNo.NO.getValue(), companyRequestDTO.getIfDisplayAbandoned())) {
            wrapper.ne("STATUS", ApproveStatusType.ABANDONED.getValue());
        }
        if (StringUtils.isNotBlank(companyRequestDTO.getCreatedBy())) {
            wrapper.eq("CREATED_BY",companyRequestDTO.getCreatedBy());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getGreenWayNumber())) {
            wrapper.eq("GREEN_WAY_NUMBER", companyRequestDTO.getGreenWayNumber());
        }
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        List<CompanyInfo> list = this.list(wrapper);
        Map<Long, String> collect = rbacClient.getByUserIds(list.stream().map(CompanyInfo::getCreatedId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(x -> x.getUserId(), y -> y.getNickname(), (o1, o2) -> o1));
        list.forEach(x->{
            x.setCreatedByName(collect.get(x.getCreatedId()));
        });
        return new PageInfo<CompanyInfo>(list);
    }

    @Override
    public List<CompanyInfo> listByDTO(CompanyRequestDTO companyRequestDTO) {
        // ??????????????????????????????????????????Id
        List<Long> vendorIds = null;
        vendorIds = getLongs();

        PageUtil.startPage(companyRequestDTO.getPageNum(), companyRequestDTO.getPageSize());
        QueryWrapper<CompanyInfo> wrapper = new QueryWrapper<CompanyInfo>();
        if (StringUtils.isNoneBlank(companyRequestDTO.getCompanyName())) {
            wrapper.like("COMPANY_NAME", companyRequestDTO.getCompanyName());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getCompanyType())) {
            wrapper.like("COMPANY_TYPE", companyRequestDTO.getCompanyType());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getDataSources())) {
            wrapper.eq("DATA_SOURCES", companyRequestDTO.getDataSources());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getLcCode())) {
            wrapper.like("LC_CODE", companyRequestDTO.getLcCode());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getOverseasRelation())) {
            wrapper.eq("OVERSEAS_RELATION", companyRequestDTO.getOverseasRelation());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getStatus())) {
            wrapper.eq("STATUS", companyRequestDTO.getStatus());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getIsBacklist())) {
            wrapper.eq("IS_BACKLIST", companyRequestDTO.getIsBacklist());
        }
        if (StringUtils.isNoneBlank(companyRequestDTO.getLegalPerson())) {
            wrapper.like("LEGAL_PERSON", companyRequestDTO.getLegalPerson());
        }
        if (companyRequestDTO.getStartDate() != null) {
            wrapper.ge("APPROVED_DATE", companyRequestDTO.getStartDate());
        }
        if (companyRequestDTO.getEndDate() != null) {
            wrapper.le("APPROVED_DATE", companyRequestDTO.getEndDate());
        }
        if (companyRequestDTO.getKeyWord() != null) {
            wrapper.like("COMPANY_NAME", companyRequestDTO.getKeyWord())
                    .or()
                    .like("COMPANY_CODE", companyRequestDTO.getKeyWord());
        }
        if (companyRequestDTO.getCompanyId() != null) {
            wrapper.eq("COMPANY_ID", companyRequestDTO.getCompanyId());
        }
        List<String> statusList = new ArrayList<>();
        statusList.add(SupplierListStatus.SUBMITTED.getValue());
        statusList.add(SupplierListStatus.APPROVED.getValue());
        wrapper.in("STATUS", statusList);
        wrapper.in(CollectionUtils.isNotEmpty(vendorIds),"COMPANY_ID",vendorIds);
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return this.list(wrapper);
    }

    public List<Long> getLongs() {
        /**
         * 9.???????????????-???????????????????????????????????????????????????????????????   --???????????????
         *   ????????????
         *     LONGI_SCMM  ???????????????????????????&????????????????????????
         *     LONGI_PSDM  ????????????????????????????????????????????????
         *     LONGI_PPDM ????????????????????????????????????????????????
         *     SystemAdmin   ???????????????
         *     ALL  ??????????????????
         */
        AtomicBoolean flag = new AtomicBoolean(true);
        List<String> roleCodes = Arrays.asList("LONGI_SCMM", "LONGI_PSDM", "LONGI_PPDM", "SystemAdmin", "ALL","LONGI_SM");
        // ??????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        List<Long> vendorIds = null;
        if ("BUYER".equals(loginAppUser.getUserType())) {
            List<Role> rolePermissions = loginAppUser.getRolePermissions();
            if(CollectionUtils.isNotEmpty(rolePermissions)){
                // ??????????????????
                List<String> roleCodes1 = rolePermissions.stream().map(Role::getRoleCode).collect(Collectors.toList());
                for(String code : roleCodes1){
                    if(roleCodes.contains(code)){
                        flag.set(false);
                        break;
                    }
                }
            }
            vendorIds = null;
            if(flag.get()){
                vendorIds = this.baseMapper.queryVendorIdByUserId(loginAppUser.getUserId());
            }
        }
        return vendorIds;
    }

    @Override
    @Transactional
    public void companyGreenChannelDelete(List<Long> companyIds, boolean isDelUser) {
        for (Long companyId : companyIds) {
            CompanyInfo CompanyData = this.getByCompanyId(companyId);
            if (CompanyData != null
//                    && ApproveStatusType.DRAFT.getValue().equals(CompanyData.getStatus())
            ) {
                this.removeById(companyId);
                iCategoryRelService.removeByCompanyId(companyId);
                bankInfoService.removeByCompanyId(companyId);
                contactInfoService.removeByCompanyId(companyId);
                financeInfoService.removeByCompanyId(companyId);
                operationInfoService.removeByCompanyId(companyId);
                holderInfoService.removeByCompanyId(companyId);
                honorInfoService.removeByCompanyId(companyId);
                orgCategoryService.removeByCompanyId(companyId);
                orgInfoService.removeByCompanyId(companyId);
                otherInfoService.removeByCompanyId(companyId);
                businessInfoService.removeByCompanyId(companyId);
                attachFileService.removeByOrderId(companyId);
                dimFieldContextService.deleteByCompanyId(companyId);
                //??????????????????
                if(isDelUser) { //
                    rbacClient.deleteByCompanyId(companyId);
                }
            } else {
                throw new BaseException("????????????????????????");
            }
        }
    }

    @Override
    public void rejectCompanyInfo(RejectRequestDTO rejectRequestDTO) {
        //?????????????????????????????????
        LoginAppUser appUser = AppUserUtil.getLoginAppUser();
        Assert.notNull(appUser, "????????????????????????");
        Assert.notNull(rejectRequestDTO, "????????????????????????");
        CompanyInfo existCompanyInfo = this.getById(rejectRequestDTO.getCompanyId());
        if (existCompanyInfo != null
                && ApproveStatusType.SUBMITTED.getValue().equals(existCompanyInfo.getStatus())) {
            existCompanyInfo.setStatus(ApproveStatusType.REJECTED.getValue());
            existCompanyInfo.setStatusName(ApproveStatusType.REJECTED.getName());
            existCompanyInfo.setLastUpdateDate(new Date());
            existCompanyInfo.setLastUpdatedBy(appUser.getUsername());
            this.updateById(existCompanyInfo);
            companyStatusLogService.saveStatusLog(existCompanyInfo.getCompanyId(),
                    appUser.getUserId(),
                    appUser.getUsername(),
                    appUser.getUserType(),
                    existCompanyInfo.getStatus(),
                    rejectRequestDTO.getOperationMemo(),
                    new Date(),
                    "?????????????????????"
            );
        } else {
            throw new BaseException("????????????????????????");
        }
    }

    @Override
    public void saveOrUpdateInfoChange(ChangeInfoDTO changeInfo) {
        if (changeInfo.getCompanyInfoChange() != null) {
            CompanyInfo companyInfo = new CompanyInfo();
            BeanUtils.copyProperties(changeInfo.getCompanyInfoChange(), companyInfo);
            this.updateById(companyInfo);
            List<CategoryRel> categoryRels = new ArrayList<>();
            if (!CollectionUtils.isEmpty(changeInfo.getCompanyInfoChange().getCategoryRelChanges())) {
                changeInfo.getCompanyInfoChange().getCategoryRelChanges().stream().forEach(rel -> {
                    CategoryRel categoryRel = new CategoryRel();
                    BeanUtils.copyProperties(rel, categoryRel);
                    categoryRels.add(categoryRel);
                });
            }
            iCategoryRelService.saveOrUpdateList(categoryRels, companyInfo.getCompanyId());
        }
        if (changeInfo.getHolderInfoChange() != null) {
            HolderInfo holderInfo = new HolderInfo();
            BeanUtils.copyProperties(holderInfo, changeInfo.getHolderInfoChange());
            holderInfoService.updateById(holderInfo);
        }
        if (changeInfo.getOperationInfoChange() != null) {
            OperationInfo operationInfo = new OperationInfo();
            BeanUtils.copyProperties(operationInfo, changeInfo.getOperationInfoChange());
            operationInfoService.updateById(operationInfo);
        }
        if (changeInfo.getOtherInfoChange() != null) {
            OtherInfo otherInfo = new OtherInfo();
            BeanUtils.copyProperties(otherInfo, changeInfo.getOtherInfoChange());
            otherInfoService.updateById(otherInfo);
        }
        if (changeInfo.getHonorInfoChange() != null) {
            HonorInfo honorInfo = new HonorInfo();
            BeanUtils.copyProperties(honorInfo, changeInfo.getHonorInfoChange());
            honorInfoService.updateById(honorInfo);
        }
        if (changeInfo.getBusinessInfoChange() != null) {
            BusinessInfo businessInfo = new BusinessInfo();
            BeanUtils.copyProperties(businessInfo, changeInfo.getBusinessInfoChange());
            businessInfoService.updateById(businessInfo);
        }
        if (!CollectionUtils.isEmpty(changeInfo.getContactInfoChanges())) {
            for (ContactInfoChange contactInfoChange : changeInfo.getContactInfoChanges()) {
                ContactInfo contactInfo = new ContactInfo();
                BeanUtils.copyProperties(contactInfoChange, contactInfo);
                if (OpConstant.add.equals(contactInfoChange.getOpType())) {
                    contactInfoService.save(contactInfo);
                } else if (OpConstant.update.equals(contactInfoChange.getOpType())) {
                    contactInfoService.updateById(contactInfo);
                } else {
                    contactInfoService.removeById(contactInfo);
                }
            }
        }
        if (!CollectionUtils.isEmpty(changeInfo.getBankInfoChanges())) {
            for (BankInfoChange bankInfoChange : changeInfo.getBankInfoChanges()) {
                BankInfo bankInfo = new BankInfo();
                BeanUtils.copyProperties(bankInfoChange, bankInfo);
                if (OpConstant.add.equals(bankInfoChange.getOpType())) {
                    bankInfoService.save(bankInfo);
                } else if (OpConstant.update.equals(bankInfoChange.getOpType())) {
                    bankInfoService.updateById(bankInfo);
                } else {
                    bankInfoService.removeById(bankInfo);
                }
            }
        }
        if (!CollectionUtils.isEmpty(changeInfo.getFinanceInfoChanges())) {
            for (FinanceInfoChange financeInfoChange : changeInfo.getFinanceInfoChanges()) {
                FinanceInfo financeInfo = new FinanceInfo();
                BeanUtils.copyProperties(financeInfo, financeInfoChange);
                if (OpConstant.add.equals(financeInfoChange.getOpType())) {
                    financeInfoService.save(financeInfo);
                } else if (OpConstant.update.equals(financeInfoChange.getOpType())) {
                    financeInfoService.updateById(financeInfo);
                } else {
                    financeInfoService.removeById(financeInfo);
                }
            }
        }
        if (!CollectionUtils.isEmpty(changeInfo.getOrgCategoryChanges())) {
            for (OrgCategoryChange orgCategoryChange : changeInfo.getOrgCategoryChanges()) {
                OrgCategory orgCategory = new OrgCategory();
                BeanUtils.copyProperties(orgCategory, orgCategoryChange);
                if (OpConstant.add.equals(orgCategoryChange.getOpType())) {
                    orgCategoryService.save(orgCategory);
                } else if (OpConstant.update.equals(orgCategoryChange.getOpType())) {
                    orgCategoryService.updateById(orgCategory);
                } else {
                    orgCategoryService.removeById(orgCategory);
                }
            }
        }
        if (!CollectionUtils.isEmpty(changeInfo.getOrgInfoChanges())) {
            for (OrgInfoChange orgInfoChange : changeInfo.getOrgInfoChanges()) {
                OrgInfo orgInfo = new OrgInfo();
                BeanUtils.copyProperties(orgInfo, orgInfoChange);
                if (OpConstant.add.equals(orgInfoChange.getOpType())) {
                    orgInfoService.save(orgInfo);
                } else if (OpConstant.update.equals(orgInfoChange.getOpType())) {
                    orgInfoService.updateById(orgInfo);
                } else {
                    orgInfoService.removeById(orgInfo);
                }
            }
        }
        if (!CollectionUtils.isEmpty(changeInfo.getFileuploadChanges())) {
            List<Fileupload> fileUploads = new ArrayList<>();
            for (FileuploadChange attachFileChange : changeInfo.getFileuploadChanges()) {
                if (attachFileChange.getFileuploadId() != null) {
                    if (OpConstant.add.equals(attachFileChange.getOpType())
                            || OpConstant.update.equals(attachFileChange.getOpType())) {
                        Fileupload addFileUpload = new Fileupload();
                        BeanUtils.copyProperties(addFileUpload, attachFileChange);
                        fileUploads.add(addFileUpload);
                    } else if (OpConstant.delete.equals(attachFileChange.getOpType())) {
                        iAttachFileService.removeByFileId(attachFileChange.getFileuploadId());
                    }
                }
            }

            if (!CollectionUtils.isEmpty(fileUploads)) {
                iAttachFileService.bindingFileupload(fileUploads, changeInfo.getInfoChange().getCompanyId());
            }

        }
    }

    @Override
    public List<CompanyInfo> listPageByOrgCodeAndKeyWord(CompanyRequestDTO companyRequestDTO) {
        Assert.notNull(companyRequestDTO.getOrgId(), "orgId????????????");
        return this.baseMapper.listPageByOrgCodeAndKeyWord(companyRequestDTO);
    }

    @Override
    public CompanyInfo queryVendorByNameAndOrgId(CompanyRequestDTO companyRequestDTO) {
        Assert.notNull(companyRequestDTO.getOrgId(), "orgId????????????");
        Assert.notNull(companyRequestDTO.getCompanyName(), "companyName????????????");
        return this.baseMapper.queryVendorByNameAndOrgId(companyRequestDTO.getCompanyName(), companyRequestDTO.getOrgId());
    }

    @Override
    public void commonCheck(InfoDTO infoDTO, String orderStatus) {
        CompanyInfo existCompanyInfo = new CompanyInfo();
        Assert.notNull(infoDTO, "???????????????");
        Assert.notNull(infoDTO.getCompanyInfo(), "????????????????????????!");
        CompanyInfo companyInfo = infoDTO.getCompanyInfo();
        if (companyInfo.getCompanyId() != null) {
            existCompanyInfo = this.getById(companyInfo.getCompanyId());
            Assert.notNull(existCompanyInfo, "?????????????????????");
            if (SampleStatusType.APPROVED.getValue().equals(existCompanyInfo.getStatus())) {
                throw new BaseException("?????????????????????????????????");
            }
        }
    }

    @Override
    public List<CompanyInfo> getComponyByNameList(List<String> companyNameList) {
        if (CollectionUtils.isNotEmpty(companyNameList)) {
            QueryWrapper<CompanyInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("COMPANY_NAME", companyNameList);
            return this.list(queryWrapper);
        } else {
            return null;
        }
    }

    /**
     * ???????????????,?????????????????????????????????????????????????????????
     *
     * @param file
     * @param fileupload
     * @return
     */
    @Override
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload) throws Exception {
        // ?????????????????????
        InputStream inputStream = file.getInputStream();
        // ????????????
        EasyExcelUtil.checkParam(file, fileupload);
        // ????????????
        Fileupload result = this.readDate(inputStream, fileupload);
        HashMap<String, Object> map = new HashMap<>();
        if (null == result) {
            map.put("message", "success");
            map.put("status", "Y");
        } else {
            map.put("fileuploadId", result.getFileuploadId());
            map.put("fileName", result.getFileSourceName());
            map.put("message", "error");
            map.put("status", "N");
        }
        return map;
    }

    @Override
    public InfoDTO listOrgAndCategoryByCompanyId(Long companyId) {
        List<OrgInfo> orgInfos = orgInfoService.list(new QueryWrapper<>(new OrgInfo().setCompanyId(companyId)));
        List<OrgCategory> orgCategories = orgCategoryService.list(new QueryWrapper<>(new OrgCategory().setCompanyId(companyId)));
        InfoDTO infoDTO = new InfoDTO();
        infoDTO.setOrgInfos(orgInfos).setOrgCategorys(orgCategories);
        return infoDTO;
    }

    @Transactional
    public Fileupload readDate(InputStream inputStream, Fileupload fileupload) throws IOException {
        Fileupload result = null;
        // ??????????????????
        AnalysisEventListenerImpl<Object> listener = new AnalysisEventListenerImpl<>();
        ExcelReader excelReader = EasyExcel.read(inputStream, listener).build();

        // ????????????
        List<UserDto> userDtos = new ArrayList<>();
        // ?????????????????????
        List<CompanyInfoDto> companyInfoDtos = new ArrayList<>();
        // ????????????
        List<FinanceInfoDto> financeInfoDtos = new ArrayList<>();
        // ????????????
        List<BankInfoDto> bankInfoDtos = new ArrayList<>();
        // ?????????
        List<ContactInfoDto> contactInfoDtos = new ArrayList<>();
        // ????????????
        List<OrgInfoDto> orgInfoDtos = new ArrayList<>();
        // ???????????????
        List<OrgCategoryDto> orgCategoryDtos = new ArrayList<>();
        // ????????????
        List<OtherInfoDto> otherInfoDtos = new ArrayList<>();
        // ???????????????
        Map<String, String> dicMap = getImportDic();
        // ?????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(true);

        // ??????????????????
        List<User> users = this.getUserData(listener, excelReader, userDtos, errorFlag);
        // ?????????????????????
        Map<String, CompanyInfo> companyInfoMap = this.getCompanyInfoData(listener, excelReader, dicMap, errorFlag, companyInfoDtos);
        // ??????????????????
        Map<String, List<FinanceInfo>> financeInfoMap = this.getFinanceInfoDtaa(listener, errorFlag, excelReader, dicMap, financeInfoDtos);
        // ??????????????????
        Map<String, List<BankInfo>> bankInfoMap = this.getBankInfoData(listener, errorFlag, excelReader, dicMap, bankInfoDtos);
        // ???????????????
        Map<String, List<ContactInfo>> contactInfoMap = this.getContactInfoData(listener, errorFlag, excelReader, contactInfoDtos);
        // ??????????????????
        Map<String, List<OrgInfo>> orgInfoMap = this.getOrgInfoData(listener, errorFlag, excelReader, dicMap, orgInfoDtos);
        // ?????????????????????
        Map<String, List<OrgCategory>> orgCategoryMap = this.getOrgCategoryDtoData(listener, errorFlag, excelReader, dicMap, orgCategoryDtos);
        // ??????????????????
        Map<String, OtherInfo> otherInfoMap = this.getOtherInfoData(listener, errorFlag, excelReader, dicMap, otherInfoDtos);
        excelReader.finish();
        // ??????????????????(??????????????????????????????????????????)
        result = this.dataProcess(fileupload, result, userDtos, companyInfoDtos, financeInfoDtos, bankInfoDtos, contactInfoDtos, orgInfoDtos, orgCategoryDtos, otherInfoDtos, errorFlag, users, companyInfoMap, financeInfoMap, bankInfoMap, contactInfoMap, orgInfoMap, orgCategoryMap, otherInfoMap);
        return result;
    }

    public Fileupload dataProcess(Fileupload fileupload, Fileupload result, List<UserDto> userDtos, List<CompanyInfoDto> companyInfoDtos, List<FinanceInfoDto> financeInfoDtos, List<BankInfoDto> bankInfoDtos, List<ContactInfoDto> contactInfoDtos, List<OrgInfoDto> orgInfoDtos, List<OrgCategoryDto> orgCategoryDtos, List<OtherInfoDto> otherInfoDtos, AtomicBoolean errorFlag, List<User> users, Map<String, CompanyInfo> companyInfoMap, Map<String, List<FinanceInfo>> financeInfoMap, Map<String, List<BankInfo>> bankInfoMap, Map<String, List<ContactInfo>> contactInfoMap, Map<String, List<OrgInfo>> orgInfoMap, Map<String, List<OrgCategory>> orgCategoryMap, Map<String, OtherInfo> otherInfoMap) throws IOException {
        if (errorFlag.get()) {
            // ????????????????????????????????????
            if (CollectionUtils.isNotEmpty(users)) {
                users.forEach(user -> {
                    String username = user.getUsername();
                    InfoDTO infoDTO = new InfoDTO();
                    infoDTO.setUserInfo(user);
                    infoDTO.setCompanyInfo(companyInfoMap.get(username));
                    infoDTO.setFinanceInfos(financeInfoMap.get(username));
                    infoDTO.setBankInfos(bankInfoMap.get(username));
                    infoDTO.setContactInfos(contactInfoMap.get(username));
                    infoDTO.setOrgInfos(orgInfoMap.get(username));
                    infoDTO.setOrgCategorys(orgCategoryMap.get(username));
//                    infoDTO.setOtherInfo(otherInfoMap.get(username));
                    this.companyGreenChannelSubmit(infoDTO);
                });
            }
        } else {
            // ???????????????
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayInputStream byteArrayInputStream = null;
            // ???????????????, ????????????
            ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
            WriteSheet userDtoSheet = EasyExcel.writerSheet(0, "?????????????????????").head(UserDto.class).build();
            WriteSheet companyInfoDtoSheet = EasyExcel.writerSheet(1, "??????????????????").head(CompanyInfoDto.class).build();
            WriteSheet financeInfoDtoSheet = EasyExcel.writerSheet(2, "????????????").head(FinanceInfoDto.class).build();
            WriteSheet bankInfoDtoSheet = EasyExcel.writerSheet(3, "????????????").head(BankInfoDto.class).build();
            WriteSheet contactInfoDtoSheet = EasyExcel.writerSheet(4, "???????????????").head(ContactInfoDto.class).build();
            WriteSheet orgInfoDtoSheet = EasyExcel.writerSheet(5, "????????????").head(OrgInfoDto.class).build();
            WriteSheet orgCategoryDtoSheet = EasyExcel.writerSheet(6, "???????????????").head(OrgCategoryDto.class).build();
            WriteSheet otherInfoDtoSheet = EasyExcel.writerSheet(7, "????????????").head(OtherInfoDto.class).build();
            excelWriter.write(userDtos, userDtoSheet).write(companyInfoDtos, companyInfoDtoSheet).write(financeInfoDtos, financeInfoDtoSheet).
                    write(bankInfoDtos, bankInfoDtoSheet).write(contactInfoDtos, contactInfoDtoSheet).write(orgInfoDtos, orgInfoDtoSheet).
                    write(orgCategoryDtos, orgCategoryDtoSheet).write(otherInfoDtos, otherInfoDtoSheet);
            excelWriter.finish();
            byteArrayInputStream = new ByteArrayInputStream(outputStream.toByteArray());
            // ????????????
            fileupload.setFileSourceName("?????????????????????.xlsx");
            result = WordUtils.uploadWordFile(fileCenterClient, byteArrayInputStream, fileupload);
        }
        return result;
    }

    public Map<String, OtherInfo> getOtherInfoData(AnalysisEventListenerImpl<Object> listener, AtomicBoolean errorFlag, ExcelReader excelReader,
                                                   Map<String, String> dicMap        // ????????????
            , List<OtherInfoDto> otherInfoDtos) {
        ReadSheet readSheet = EasyExcel.readSheet(7).head(OtherInfoDto.class).build();
        excelReader.read(readSheet);
        List<Object> OtherInfoDtoList = listener.getDatas();
        Map<String, OtherInfo> otherInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(OtherInfoDtoList)) {
            OtherInfoDtoList.forEach(OtherInfoDtoTemp -> {
                OtherInfo otherInfo = new OtherInfo();
                OtherInfoDto otherInfoDto = (OtherInfoDto) OtherInfoDtoTemp;
                StringBuffer errorMsg = new StringBuffer();

                // ?????????
                String username = otherInfoDto.getUsername();
                if (StringUtil.isEmpty(username)) {
                    errorFlag.set(false);
                    errorMsg.append("?????????????????????;");
                } else {
                    //???????????????????????????
                    List<User> userList = rbacClient.checkByUsername(username.trim());
                    if (CollectionUtils.isNotEmpty(userList)) {
                        errorFlag.set(false);
                        errorMsg.append("??????????????????;");
                    } else if (errorFlag.get()) {
                        otherInfo.setUsername(username.trim());
                    }
                }

                // ????????????
                String bizModel = otherInfoDto.getBizModel();
                if (StringUtil.notEmpty(bizModel)) {
                    if (StringUtil.isEmpty(dicMap.get(bizModel.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        otherInfo.setBizModel(dicMap.get(bizModel.trim()));
                    }
                }

                // ????????????
                String factoryType = otherInfoDto.getFactoryType();
                if (StringUtil.notEmpty(factoryType)) {
                    if (StringUtil.isEmpty(dicMap.get(factoryType.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        otherInfo.setFactoryType(dicMap.get(factoryType.trim()));
                    }
                }

                // ????????????(?????????)
                String floorArea = otherInfoDto.getFloorArea();
                if (StringUtil.notEmpty(floorArea)) {
                    try {
                        Integer floor = Integer.parseInt(floorArea);
                        otherInfo.setFloorArea(floor);
                    } catch (NumberFormatException e) {
                        errorMsg.append("????????????????????????");
                        errorFlag.set(false);
                    }
                }

                // ????????????
                String employeeQty = otherInfoDto.getEmployeeQty();
                if (StringUtil.notEmpty(employeeQty)) {
                    if (StringUtil.isEmpty(dicMap.get(employeeQty.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        otherInfo.setEmployeeQty(dicMap.get(employeeQty.trim()));
                    }
                }

                // ????????????
                String companySite = otherInfoDto.getCompanySite();
                if (StringUtil.notEmpty(companySite)) {
                    otherInfo.setCompanySite(companySite.trim());
                }

                // ????????????(?????????)
                String floorSpace = otherInfoDto.getFloorSpace();
                if (StringUtil.notEmpty(floorSpace)) {
                    try {
                        Integer floor = Integer.parseInt(floorSpace);
                        otherInfo.setFloorSpace(floor);
                    } catch (NumberFormatException e) {
                        errorMsg.append("????????????????????????");
                        errorFlag.set(false);
                    }
                }

                if (errorMsg.length() > 1) {
                    otherInfoDto.setErrorMsg(errorMsg.toString());
                } else {
                    otherInfoDto.setErrorMsg(null);
                }
                otherInfoDtos.add(otherInfoDto);

                if (errorFlag.get()) {
                    otherInfoMap.put(username.trim(), otherInfo);
                }
            });
        }
        listener.getDatas().clear();
        return otherInfoMap;
    }

    public Map<String, List<OrgCategory>> getOrgCategoryDtoData(AnalysisEventListenerImpl<Object> listener, AtomicBoolean errorFlag, ExcelReader excelReader,
                                                                Map<String, String> dicMap, List<OrgCategoryDto> orgCategoryDtos) {
        // ???????????????
        ReadSheet readSheet = EasyExcel.readSheet(6).head(OrgCategoryDto.class).build();
        excelReader.read(readSheet);
        List<Object> OrgCategoryDtoList = listener.getDatas();
        List<OrgCategory> orgCategorys = new ArrayList<>();
        Map<String, List<OrgCategory>> orgCategoryMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(OrgCategoryDtoList)) {
            OrgCategoryDtoList.forEach(orgCategoryDtoTemp -> {
                OrgCategory orgCategory = new OrgCategory();
                OrgCategoryDto orgCategoryDto = (OrgCategoryDto) orgCategoryDtoTemp;
                StringBuffer errorMsg = new StringBuffer();

                // ?????????
                String username = orgCategoryDto.getUsername();
                if (StringUtil.isEmpty(username)) {
                    errorFlag.set(false);
                    errorMsg.append("?????????????????????;");
                } else {
                    //???????????????????????????
                    List<User> userList = rbacClient.checkByUsername(username.trim());
                    if (CollectionUtils.isNotEmpty(userList)) {
                        errorFlag.set(false);
                        errorMsg.append("??????????????????;");
                    } else if (errorFlag.get()) {
                        orgCategory.setUsername(username.trim());
                    }
                }

                // ???????????????
                String orgName = orgCategoryDto.getOrgName();
                if (StringUtil.notEmpty(orgName)) {
                    if (StringUtil.checkStringNoSlash(orgName)) {
                        errorMsg.append("?????????????????????????????????:/;");
                        errorFlag.set(false);
                    } else if (orgName.contains("/")) {
                        StringBuffer error = new StringBuffer();
                        // ??????
                        List<String> orgNames = Arrays.asList(orgName.split("/"));
                        // ????????????????????????
                        ArrayList<Organization> organizations = new ArrayList<>();
                        orgNames.forEach(orgNameTemp -> {
                            Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgNameTemp.trim()));
                            if (null != organization && organization.getOrganizationId() > 1) {
                                organizations.add(organization);
                            } else {
                                error.append(orgNameTemp + "?????????;");
                            }
                        });
                        if (error.length() > 1) {
                            errorMsg.append(error);
                            errorFlag.set(false);
                        } else {
                            if (CollectionUtils.isNotEmpty(organizations)) {
                                int size = organizations.size();
                                if (size == 1) {
                                    Organization organization = organizations.get(0);
                                    Long organizationId = organization.getOrganizationId();
                                    // ?????????????????????
                                    List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                                    if (CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())) {
                                        errorMsg.append("????????????????????????,??????????????????;");
                                        errorFlag.set(false);
                                    } else {
                                        String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                        orgCategory.setOrgId(organizationId);
                                        orgCategory.setOrgCode(organization.getOrganizationCode());
                                        orgCategory.setOrgName(organization.getOrganizationName());
                                        orgCategory.setFullPathId(md5);
                                    }
                                } else {
                                    Organization organization = organizations.get(0);
                                    Long organizationId = organization.getOrganizationId();
                                    String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                    for (int i = 1; i < size; i++) {
                                        Long organizationId1 = organizations.get(i).getOrganizationId();
                                        md5 = EncryptUtil.getMD5(md5 + organizationId1);
                                    }
                                    List<OrganizationUser> organizationUsers = baseClient.queryByFullPathId(md5);
                                    if (CollectionUtils.isNotEmpty(organizationUsers)) {
                                        Organization organization1 = organizations.get(size - 1);
                                        orgCategory.setOrgId(organization1.getOrganizationId());
                                        orgCategory.setOrgCode(organization1.getOrganizationCode());
                                        orgCategory.setOrgName(organization1.getOrganizationName());
                                        orgCategory.setFullPathId(md5);
                                    } else {
                                        errorMsg.append("???????????????????????????;");
                                        errorFlag.set(false);
                                    }
                                }
                            }
                        }
                    } else {
                        // ????????????????????????
                        Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgName.trim()));
                        if (null != organization && organization.getOrganizationId() > 1) {
                            Long organizationId = organization.getOrganizationId();
                            // ?????????????????????
                            List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                            if (CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())) {
                                errorMsg.append("????????????????????????,??????????????????;");
                                errorFlag.set(false);
                            } else {
                                String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                orgCategory.setOrgId(organizationId);
                                orgCategory.setOrgCode(organization.getOrganizationCode());
                                orgCategory.setOrgName(organization.getOrganizationName());
                                orgCategory.setFullPathId(md5);
                            }
                        } else {
                            errorMsg.append("??????????????????;");
                            errorFlag.set(false);
                        }
                    }
                }

                // ????????????
                String categoryName = orgCategoryDto.getCategoryName();
                if (StringUtil.notEmpty(categoryName)) {
                    List<PurchaseCategory> purchaseCategories = baseClient.listPurchaseCategoryByNameBatch(Arrays.asList(categoryName));
                    if (CollectionUtils.isEmpty(purchaseCategories)) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        PurchaseCategory purchaseCategory = purchaseCategories.get(0);
                        orgCategory.setCategoryId(purchaseCategory.getCategoryId());
                        orgCategory.setCategoryCode(purchaseCategory.getCategoryCode());
                        orgCategory.setCategoryName(purchaseCategory.getCategoryName());
                        orgCategory.setCategoryFullName(purchaseCategory.getCategoryFullName());
                    }
                }

                // ????????????
                String serviceStatus = orgCategoryDto.getServiceStatus();
                if (StringUtil.notEmpty(serviceStatus)) {
                    if (StringUtil.isEmpty(dicMap.get(serviceStatus.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        orgCategory.setServiceStatus(dicMap.get(serviceStatus.trim()));
                    }
                }

                // ????????????
                String startDate = orgCategoryDto.getStartDate();
                if (StringUtil.notEmpty(startDate)) {
                    try {
                        Date date = DateUtil.parseDate(startDate.trim());
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        orgCategory.setStartDate(localDate);
                    } catch (Exception e) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    }
                }

                // ????????????
                String endDate = orgCategoryDto.getEndDate();
                if (StringUtil.notEmpty(endDate)) {
                    try {
                        Date date = DateUtil.parseDate(endDate.trim());
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        orgCategory.setEndDate(localDate);
                    } catch (Exception e) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    }
                }

                if (errorMsg.length() > 1) {
                    orgCategoryDto.setErrorMsg(errorMsg.toString());
                } else {
                    orgCategoryDto.setErrorMsg(null);
                }
                orgCategoryDtos.add(orgCategoryDto);

                if (errorFlag.get()) {
                    orgCategorys.add(orgCategory);
                }
            });
            if (errorFlag.get() && CollectionUtils.isNotEmpty(orgCategorys)) {
                orgCategoryMap = orgCategorys.stream().collect(Collectors.groupingBy(OrgCategory::getUsername));
            }
        }
        listener.getDatas().clear();
        return orgCategoryMap;
    }

    public Map<String, List<OrgInfo>> getOrgInfoData(AnalysisEventListenerImpl<Object> listener, AtomicBoolean errorFlag, ExcelReader excelReader,
                                                     Map<String, String> dicMap, List<OrgInfoDto> orgInfoDtos) {
        // ????????????
        ReadSheet readSheet = EasyExcel.readSheet(5).head(OrgInfoDto.class).build();
        excelReader.read(readSheet);
        List<Object> OrgInfoDtoList = listener.getDatas();
        List<OrgInfo> orgInfos = new ArrayList<>();
        Map<String, List<OrgInfo>> orgInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(OrgInfoDtoList)) {
            OrgInfoDtoList.forEach(OrgInfoDtoTemp -> {
                OrgInfo orgInfo = new OrgInfo();
                OrgInfoDto orgInfoDto = (OrgInfoDto) OrgInfoDtoTemp;
                StringBuffer errorMsg = new StringBuffer();

                // ?????????
                String username = orgInfoDto.getUsername();
                if (StringUtil.isEmpty(username)) {
                    errorFlag.set(false);
                    errorMsg.append("?????????????????????;");
                } else {
                    //???????????????????????????
                    List<User> userList = rbacClient.checkByUsername(username.trim());
                    if (CollectionUtils.isNotEmpty(userList)) {
                        errorFlag.set(false);
                        errorMsg.append("??????????????????;");
                    } else if (errorFlag.get()) {
                        orgInfo.setUsername(username.trim());
                    }
                }

                // ???????????????
                String orgName = orgInfoDto.getOrgName();
                if (StringUtil.notEmpty(orgName)) {
                    if (StringUtil.checkStringNoSlash(orgName)) {
                        errorMsg.append("?????????????????????????????????:/;");
                        errorFlag.set(false);
                    } else if (orgName.contains("/")) {
                        StringBuffer error = new StringBuffer();
                        // ??????
                        List<String> orgNames = Arrays.asList(orgName.split("/"));
                        // ????????????????????????
                        ArrayList<Organization> organizations = new ArrayList<>();
                        orgNames.forEach(orgNameTemp -> {
                            Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgNameTemp.trim()));
                            if (null != organization && organization.getOrganizationId() > 1) {
                                organizations.add(organization);
                            } else {
                                error.append(orgNameTemp + "?????????;");
                            }
                        });
                        if (error.length() > 1) {
                            errorMsg.append(error);
                            errorFlag.set(false);
                        } else {
                            if (CollectionUtils.isNotEmpty(organizations)) {
                                int size = organizations.size();
                                if (size == 1) {
                                    Organization organization = organizations.get(0);
                                    Long organizationId = organization.getOrganizationId();
                                    // ?????????????????????
                                    List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                                    if (CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())) {
                                        errorMsg.append("????????????????????????,??????????????????;");
                                        errorFlag.set(false);
                                    } else {
                                        String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                        orgInfo.setOrgId(organizationId);
                                        orgInfo.setOrgCode(organization.getOrganizationCode());
                                        orgInfo.setOrgName(organization.getOrganizationName());
                                        orgInfo.setFullPathId(md5);
                                    }
                                } else {
                                    Organization organization = organizations.get(0);
                                    Long organizationId = organization.getOrganizationId();
                                    String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                    for (int i = 1; i < size; i++) {
                                        Long organizationId1 = organizations.get(i).getOrganizationId();
                                        md5 = EncryptUtil.getMD5(md5 + organizationId1);
                                    }
                                    List<OrganizationUser> organizationUsers = baseClient.queryByFullPathId(md5);
                                    if (CollectionUtils.isNotEmpty(organizationUsers)) {
                                        Organization organization1 = organizations.get(size - 1);
                                        orgInfo.setOrgId(organization1.getOrganizationId());
                                        orgInfo.setOrgCode(organization1.getOrganizationCode());
                                        orgInfo.setOrgName(organization1.getOrganizationName());
                                        orgInfo.setFullPathId(md5);
                                    } else {
                                        errorMsg.append("???????????????????????????;");
                                        errorFlag.set(false);
                                    }
                                }
                            }
                        }
                    } else {
                        // ????????????????????????
                        Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgName.trim()));
                        if (null != organization && organization.getOrganizationId() > 1) {
                            Long organizationId = organization.getOrganizationId();
                            // ?????????????????????
                            List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                            if (CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())) {
                                errorMsg.append("????????????????????????,??????????????????;");
                                errorFlag.set(false);
                            } else {
                                String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                orgInfo.setOrgId(organizationId);
                                orgInfo.setOrgCode(organization.getOrganizationCode());
                                orgInfo.setOrgName(organization.getOrganizationName());
                                orgInfo.setFullPathId(md5);
                            }
                        } else {
                            errorMsg.append("??????????????????;");
                            errorFlag.set(false);
                        }
                    }
                }

                // ????????????
                String serviceStatus = orgInfoDto.getServiceStatus();
                if (StringUtil.notEmpty(serviceStatus)) {
                    if (StringUtil.isEmpty(dicMap.get(serviceStatus.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else {
                        orgInfo.setServiceStatus(dicMap.get(serviceStatus.trim()));
                    }
                }

                // ????????????
                String startDate = orgInfoDto.getStartDate();
                if (StringUtil.notEmpty(startDate)) {
                    try {
                        Date date = DateUtil.parseDate(startDate.trim());
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        orgInfo.setStartDate(localDate);
                    } catch (Exception e) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    }
                }

                // ????????????
                String endDate = orgInfoDto.getEndDate();
                if (StringUtil.notEmpty(endDate)) {
                    try {
                        Date date = DateUtil.parseDate(endDate.trim());
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        orgInfo.setEndDate(localDate);
                    } catch (Exception e) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    }
                }

                if (errorMsg.length() > 1) {
                    orgInfoDto.setErrorMsg(errorMsg.toString());
                } else {
                    orgInfoDto.setErrorMsg(null);
                }
                orgInfoDtos.add(orgInfoDto);

                if (errorFlag.get()) {
                    orgInfos.add(orgInfo);
                }
            });
            if (errorFlag.get() && CollectionUtils.isNotEmpty(orgInfos)) {
                // ????????????
                orgInfoMap = orgInfos.stream().collect(Collectors.groupingBy(OrgInfo::getUsername));
            }
        }
        listener.getDatas().clear();
        return orgInfoMap;
    }

    public Map<String, List<ContactInfo>> getContactInfoData(AnalysisEventListenerImpl<Object> listener, AtomicBoolean errorFlag, ExcelReader excelReader,
                                                             List<ContactInfoDto> contactInfoDtos) {
        // ?????????
        ReadSheet readSheet = EasyExcel.readSheet(4).head(ContactInfoDto.class).build();
        excelReader.read(readSheet);
        List<Object> contactInfoDtoList = listener.getDatas();
        List<ContactInfo> contactInfos = new ArrayList<>();
        Map<String, List<ContactInfo>> contactInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(contactInfoDtoList)) {
            contactInfoDtoList.forEach(contactInfoDtoTemp -> {
                ContactInfoDto contactInfoDto = (ContactInfoDto) contactInfoDtoTemp;
                ContactInfo contactInfo = new ContactInfo();
                StringBuffer errorMsg = new StringBuffer();

                // ?????????
                String username = contactInfoDto.getUsername();
                if (StringUtil.isEmpty(username)) {
                    errorFlag.set(false);
                    errorMsg.append("?????????????????????;");
                } else {
                    //???????????????????????????
                    List<User> userList = rbacClient.checkByUsername(username.trim());
                    if (CollectionUtils.isNotEmpty(userList)) {
                        errorFlag.set(false);
                        errorMsg.append("??????????????????;");
                    } else if (errorFlag.get()) {
                        contactInfo.setUsername(username.trim());
                    }
                }

                // ???????????????
                String contactName = contactInfoDto.getContactName();
                if (StringUtil.notEmpty(contactName)) {
                    contactInfo.setContactName(contactName.trim());
                }

                // ????????????
                String mobileNumber = contactInfoDto.getMobileNumber();
                if (StringUtil.notEmpty(mobileNumber)) {
                    contactInfo.setMobileNumber(mobileNumber.trim());
                }

                // ????????????
                String phoneNumber = contactInfoDto.getPhoneNumber();
                if (StringUtil.notEmpty(phoneNumber)) {
                    contactInfo.setPhoneNumber(phoneNumber.trim());
                }

                // ??????
                String email = contactInfoDto.getEmail();
                if (StringUtil.notEmpty(email)) {
                    contactInfo.setEmail(email.trim());
                }

                // ???????????????
                String contactAddress = contactInfoDto.getContactAddress();
                if (StringUtil.notEmpty(contactAddress)) {
                    contactInfo.setContactAddress(contactAddress.trim());
                }

                // ????????????
                String position = contactInfoDto.getPosition();
                if (StringUtil.notEmpty(position)) {
                    contactInfo.setPosition(position.trim());
                }

                // ????????????
                String taxNumber = contactInfoDto.getTaxNumber();
                if (StringUtil.notEmpty(taxNumber)) {
                    contactInfo.setTaxNumber(taxNumber.trim());
                }
                if (errorMsg.length() > 1) {
                    contactInfoDto.setErrorMsg(errorMsg.toString());
                }
                contactInfoDtos.add(contactInfoDto);

                if (errorFlag.get()) {
                    contactInfos.add(contactInfo);
                }
            });
            if (errorFlag.get() && CollectionUtils.isNotEmpty(contactInfos)) {
                contactInfoMap = contactInfos.stream().collect(Collectors.groupingBy(ContactInfo::getUsername));
            }
        }
        listener.getDatas().clear();
        return contactInfoMap;
    }

    public Map<String, List<BankInfo>> getBankInfoData(AnalysisEventListenerImpl<Object> listener, AtomicBoolean errorFlag, ExcelReader excelReader,
                                                       Map<String, String> dicMap, List<BankInfoDto> bankInfoDtos) {
        // ????????????
        ReadSheet readSheet = EasyExcel.readSheet(3).head(BankInfoDto.class).build();
        excelReader.read(readSheet);
        List<Object> bankInfoList = listener.getDatas();
        Map<String, List<BankInfo>> bankInfoMap = new HashMap<>();
        List<BankInfo> bankInfos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bankInfoList)) {
            bankInfoList.forEach(bankInfoDtoTemp -> {
                StringBuffer errorMsg = new StringBuffer();
                BankInfo bankInfo = new BankInfo();
                BankInfoDto bankInfoDto = (BankInfoDto) bankInfoDtoTemp;
                // ?????????
                String username = bankInfoDto.getUsername();
                if (StringUtil.isEmpty(username)) {
                    errorFlag.set(false);
                    errorMsg.append("?????????????????????;");
                } else {
                    //???????????????????????????
                    List<User> userList = rbacClient.checkByUsername(username.trim());
                    if (CollectionUtils.isNotEmpty(userList)) {
                        errorFlag.set(false);
                        errorMsg.append("??????????????????;");
                    } else if (errorFlag.get()) {
                        bankInfo.setUsername(username.trim());
                    }
                }

                // ???????????????
                String orgName = bankInfoDto.getOrgName();
                if (StringUtil.notEmpty(orgName)) {
                    if (StringUtil.checkStringNoSlash(orgName)) {
                        errorMsg.append("?????????????????????????????????:/;");
                        errorFlag.set(false);
                    } else if (orgName.contains("/")) {
                        StringBuffer error = new StringBuffer();
                        // ??????
                        List<String> orgNames = Arrays.asList(orgName.split("/"));
                        // ????????????????????????
                        ArrayList<Organization> organizations = new ArrayList<>();
                        orgNames.forEach(orgNameTemp -> {
                            Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgNameTemp.trim()));
                            if (null != organization && organization.getOrganizationId() > 1) {
                                organizations.add(organization);
                            } else {
                                error.append(orgNameTemp + "?????????;");
                            }
                        });
                        if (error.length() > 1) {
                            errorMsg.append(error);
                            errorFlag.set(false);
                        } else {
                            if (CollectionUtils.isNotEmpty(organizations)) {
                                int size = organizations.size();
                                if (size == 1) {
                                    Organization organization = organizations.get(0);
                                    Long organizationId = organization.getOrganizationId();
                                    // ?????????????????????
                                    List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                                    if (CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())) {
                                        errorMsg.append("????????????????????????,??????????????????;");
                                        errorFlag.set(false);
                                    } else {
                                        String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                        bankInfo.setOrgId(organizationId);
                                        bankInfo.setOrgCode(organization.getOrganizationCode());
                                        bankInfo.setOrgName(organization.getOrganizationName());
                                        bankInfo.setFullPathId(md5);
                                    }
                                } else {
                                    Organization organization = organizations.get(0);
                                    Long organizationId = organization.getOrganizationId();
                                    String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                    for (int i = 1; i < size; i++) {
                                        Long organizationId1 = organizations.get(i).getOrganizationId();
                                        md5 = EncryptUtil.getMD5(md5 + organizationId1);
                                    }
                                    List<OrganizationUser> organizationUsers = baseClient.queryByFullPathId(md5);
                                    if (CollectionUtils.isNotEmpty(organizationUsers)) {
                                        Organization organization1 = organizations.get(size - 1);
                                        bankInfo.setOrgId(organization1.getOrganizationId());
                                        bankInfo.setOrgCode(organization1.getOrganizationCode());
                                        bankInfo.setOrgName(organization1.getOrganizationName());
                                        bankInfo.setFullPathId(md5);
                                    } else {
                                        errorMsg.append("???????????????????????????;");
                                        errorFlag.set(false);
                                    }
                                }
                            }
                        }
                    } else {
                        // ????????????????????????
                        Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgName.trim()));
                        if (null != organization && organization.getOrganizationId() > 1) {
                            Long organizationId = organization.getOrganizationId();
                            // ?????????????????????
                            List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                            if (CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())) {
                                errorMsg.append("????????????????????????,??????????????????;");
                                errorFlag.set(false);
                            } else {
                                String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                bankInfo.setOrgId(organizationId);
                                bankInfo.setOrgCode(organization.getOrganizationCode());
                                bankInfo.setOrgName(organization.getOrganizationName());
                                bankInfo.setFullPathId(md5);
                            }
                        } else {
                            errorMsg.append("??????????????????;");
                            errorFlag.set(false);
                        }
                    }
                }

                // ?????????
                String openingBank = bankInfoDto.getOpeningBank();
                if (StringUtil.notEmpty(openingBank) && errorFlag.get()) {
                    bankInfo.setOpeningBank(openingBank.trim());
                }

                // ????????????
                String unionCode = bankInfoDto.getUnionCode();
                if (StringUtil.notEmpty(unionCode) && errorFlag.get()) {
                    bankInfo.setUnionCode(unionCode.trim());
                }

                // SWIFT CODE
                String swiftCode = bankInfoDto.getSwiftCode();
                if (StringUtil.notEmpty(swiftCode) && errorFlag.get()) {
                    bankInfo.setSwiftCode(swiftCode.trim());
                }

                // ????????????
                String bankAccountName = bankInfoDto.getBankAccountName();
                if (StringUtil.notEmpty(bankAccountName) && errorFlag.get()) {
                    bankInfo.setBankAccountName(bankAccountName.trim());
                }

                // ????????????
                String bankAccount = bankInfoDto.getBankAccount();
                if (StringUtil.notEmpty(bankAccount) && errorFlag.get()) {
                    bankInfo.setBankAccount(bankAccount.trim());
                }

                // ????????????
                String currencyName = bankInfoDto.getCurrencyName();
                if (StringUtil.notEmpty(currencyName)) {
                    if (StringUtil.isEmpty(dicMap.get(currencyName.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        bankInfo.setCurrencyName(dicMap.get(currencyName.trim()));
                    }
                }

                // ????????????
                String accountType = bankInfoDto.getAccountType();
                if (StringUtil.notEmpty(accountType)) {
                    if (StringUtil.isEmpty(dicMap.get(accountType.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        bankInfo.setAccountType(dicMap.get(accountType.trim()));
                    }
                }

                if (errorMsg.length() > 1) {
                    bankInfoDto.setErrorMsg(errorMsg.toString());
                } else {
                    bankInfoDto.setErrorMsg(null);
                }

                bankInfoDtos.add(bankInfoDto);

                if (errorFlag.get()) {
                    bankInfos.add(bankInfo);
                }
            });
            if (errorFlag.get() && CollectionUtils.isNotEmpty(bankInfos)) {
                // ??????????????????
                bankInfoMap = bankInfos.stream().collect(Collectors.groupingBy(BankInfo::getUsername));
            }
        }
        listener.getDatas().clear();
        return bankInfoMap;
    }

    public Map<String, List<FinanceInfo>> getFinanceInfoDtaa(AnalysisEventListenerImpl<Object> listener, AtomicBoolean errorFlag,
                                                             ExcelReader excelReader, Map<String, String> dicMap,
                                                             List<FinanceInfoDto> financeInfoDtos) {
        // ????????????
        ReadSheet readSheet = EasyExcel.readSheet(2).head(FinanceInfoDto.class).build();
        excelReader.read(readSheet);
        List<Object> financeInfoList = listener.getDatas();
        List<FinanceInfo> financeInfos = new ArrayList<>();
        Map<String, List<FinanceInfo>> financeInfosMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(financeInfoList)) {
            financeInfoList.forEach(financeInfoDtoTemp -> {
                FinanceInfo financeInfo = new FinanceInfo();
                FinanceInfoDto financeInfoDto = (FinanceInfoDto) financeInfoDtoTemp;
                StringBuffer errorMsg = new StringBuffer();

                // ?????????
                String username = financeInfoDto.getUsername();
                if (StringUtil.isEmpty(username)) {
                    errorFlag.set(false);
                    errorMsg.append("?????????????????????;");
                } else {
                    //???????????????????????????
                    List<User> userList = rbacClient.checkByUsername(username.trim());
                    if (CollectionUtils.isNotEmpty(userList)) {
                        errorFlag.set(false);
                        errorMsg.append("??????????????????;");
                    } else if (errorFlag.get()) {
                        financeInfo.setUsername(username.trim());
                    }
                }

                // ????????????
                String clearCurrency = financeInfoDto.getClearCurrency();
                if (StringUtil.notEmpty(clearCurrency)) {
                    if (StringUtil.isEmpty(dicMap.get(clearCurrency.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        financeInfo.setClearCurrency(dicMap.get(clearCurrency.trim()));
                    }
                }

                // ????????????
                String paymentMethod = financeInfoDto.getPaymentMethod();
                if (StringUtil.notEmpty(paymentMethod)) {
                    if (StringUtil.isEmpty(dicMap.get(paymentMethod.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        financeInfo.setPaymentMethod(dicMap.get(paymentMethod.trim()));
                    }
                }

                // ????????????
                String paymentTerms = financeInfoDto.getPaymentTerms();
                if (StringUtil.notEmpty(paymentTerms)) {
                    if (StringUtil.isEmpty(dicMap.get(paymentTerms.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        financeInfo.setPaymentTerms(dicMap.get(paymentTerms.trim()));
                    }
                }

                // ????????????
                String taxKey = financeInfoDto.getTaxKey();
                if (StringUtil.notEmpty(taxKey)) {
                    // ???????????????????????????????????????????????????
                    String localeKey = LocaleHandler.getLocaleKey();
                    PurchaseTax purchaseTax = baseClient.getByTaxKeyAndLanguage(taxKey.trim(), localeKey);
                    if (null != purchaseTax && purchaseTax.getTaxId() > 0) {
                        if (errorFlag.get()) {
                            financeInfo.setTaxKey(purchaseTax.getTaxKey());
                            financeInfo.setTaxRate(purchaseTax.getTaxCode().toString());
                        }
                    } else {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    }
                }

                // ????????????(??????:??????)
                String limitAmount = financeInfoDto.getLimitAmount();
                if (StringUtil.notEmpty(limitAmount)) {
                    if (StringUtil.isEmpty(dicMap.get(limitAmount.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        financeInfo.setLimitAmount(new BigDecimal(dicMap.get(limitAmount.trim())));
                    }
                }

                // ???????????????
                String orgName = financeInfoDto.getOrgName();
                if (StringUtil.notEmpty(orgName)) {
                    if (StringUtil.checkStringNoSlash(orgName)) {
                        errorMsg.append("?????????????????????????????????:/;");
                        errorFlag.set(false);
                    } else if (orgName.contains("/")) {
                        StringBuffer error = new StringBuffer();
                        // ??????
                        List<String> orgNames = Arrays.asList(orgName.split("/"));
                        // ????????????????????????
                        ArrayList<Organization> organizations = new ArrayList<>();
                        orgNames.forEach(orgNameTemp -> {
                            Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgNameTemp.trim()));
                            if (null != organization && organization.getOrganizationId() > 1) {
                                organizations.add(organization);
                            } else {
                                error.append(orgNameTemp + "?????????;");
                            }
                        });
                        if (error.length() > 1) {
                            errorMsg.append(error);
                            errorFlag.set(false);
                        } else {
                            if (CollectionUtils.isNotEmpty(organizations)) {
                                int size = organizations.size();
                                if (size == 1) {
                                    Organization organization = organizations.get(0);
                                    Long organizationId = organization.getOrganizationId();
                                    // ?????????????????????
                                    List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                                    if (CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())) {
                                        errorMsg.append("????????????????????????,??????????????????;");
                                        errorFlag.set(false);
                                    } else {
                                        String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                        financeInfo.setOrgId(organizationId);
                                        financeInfo.setOrgCode(organization.getOrganizationCode());
                                        financeInfo.setOrgName(organization.getOrganizationName());
                                        financeInfo.setFullPathId(md5);
                                    }
                                } else {
                                    Organization organization = organizations.get(0);
                                    Long organizationId = organization.getOrganizationId();
                                    String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                    for (int i = 1; i < size; i++) {
                                        Long organizationId1 = organizations.get(i).getOrganizationId();
                                        md5 = EncryptUtil.getMD5(md5 + organizationId1);
                                    }
                                    List<OrganizationUser> organizationUsers = baseClient.queryByFullPathId(md5);
                                    if (CollectionUtils.isNotEmpty(organizationUsers)) {
                                        Organization organization1 = organizations.get(size - 1);
                                        financeInfo.setOrgId(organization1.getOrganizationId());
                                        financeInfo.setOrgCode(organization1.getOrganizationCode());
                                        financeInfo.setOrgName(organization1.getOrganizationName());
                                        financeInfo.setFullPathId(md5);
                                    } else {
                                        errorMsg.append("???????????????????????????;");
                                        errorFlag.set(false);
                                    }
                                }
                            }
                        }
                    } else {
                        // ????????????????????????
                        Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgName.trim()));
                        if (null != organization && organization.getOrganizationId() > 1) {
                            Long organizationId = organization.getOrganizationId();
                            // ?????????????????????
                            List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                            if (CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())) {
                                errorMsg.append("????????????????????????,??????????????????;");
                                errorFlag.set(false);
                            } else {
                                String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                financeInfo.setOrgId(organizationId);
                                financeInfo.setOrgCode(organization.getOrganizationCode());
                                financeInfo.setOrgName(organization.getOrganizationName());
                                financeInfo.setFullPathId(md5);
                            }
                        } else {
                            errorMsg.append("??????????????????;");
                            errorFlag.set(false);
                        }
                    }
                }


                // ????????????
                if (errorMsg.length() > 1) {
                    // ??????????????????
                    financeInfoDto.setErrorMsg(errorMsg.toString());
                } else {
                    financeInfoDto.setErrorMsg(null);
                }
                // ????????????????????????
                financeInfoDtos.add(financeInfoDto);
                if (errorFlag.get()) {
                    // ??????????????????
                    financeInfos.add(financeInfo);
                }
            });

            if (errorFlag.get() && CollectionUtils.isNotEmpty(financeInfos)) {
                // ????????????????????????
                financeInfosMap = financeInfos.stream().collect(Collectors.groupingBy(FinanceInfo::getUsername));
            }
        }
        // ????????????
        listener.getDatas().clear();
        return financeInfosMap;
    }

    public Map<String, CompanyInfo> getCompanyInfoData(AnalysisEventListenerImpl<Object> listener,
                                                       ExcelReader excelReader, Map<String, String> dicMap, AtomicBoolean errorFlag,
                                                       List<CompanyInfoDto> companyInfoDtos) {
        // ?????????????????????
        ReadSheet readSheet = EasyExcel.readSheet(1).head(CompanyInfoDto.class).build();
        excelReader.read(readSheet);
        List<Object> companyInfoList = listener.getDatas();
        Map<String, CompanyInfo> companyInfosMap = new HashMap<>();
        HashSet<String> hashSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(companyInfoList)) {
            companyInfoList.forEach(companyInfoDtoTemp -> {
                StringBuffer errorMsg = new StringBuffer();
                CompanyInfo companyInfo = new CompanyInfo();
                CompanyInfoDto companyInfoDto = (CompanyInfoDto) companyInfoDtoTemp;
                /**
                 * ????????????????????????: ???????????????/????????????/????????????(??????/??????)/????????????/????????????/????????????
                 * 1. ??????????????????, ????????????:      /????????????/????????????/???????????????/???????????????/????????????/????????????
                 * 2. ???????????????-?????????, ????????????:  ????????????/????????????/????????????????????????/???????????????/??????/???/??????/????????????/????????????
                 * 3. ???????????????-???????????????,????????????:????????????/????????????????????????/???????????????/??????/???/??????
                 * 4. ??????, ????????????: ????????????/????????????/????????????/????????????????????????/???????????????/???????????????/???????????????/??????/???/??????/????????????/????????????
                 */
                // ?????????
                String username = companyInfoDto.getUsername();
                if (StringUtil.isEmpty(username)) {
                    errorFlag.set(false);
                    errorMsg.append("?????????????????????;");
                } else {
                    //???????????????????????????
                    List<User> userList = rbacClient.checkByUsername(username.trim());
                    if (CollectionUtils.isNotEmpty(userList)) {
                        errorFlag.set(false);
                        errorMsg.append("??????????????????;");
                    }
                }
                // ???????????????
                String overseasRelation = companyInfoDto.getOverseasRelation();
                if (StringUtil.isEmpty(overseasRelation)) {
                    errorMsg.append("???????????????????????????;");
                    errorFlag.set(false);
                } else if (StringUtil.isEmpty(dicMap.get(overseasRelation.trim()))) {
                    errorMsg.append("?????????????????????????????????;");
                    errorFlag.set(false);
                } else if (errorFlag.get()) {
                    companyInfo.setOverseasRelation(dicMap.get(overseasRelation.trim()));
                }

                // ????????????
                String companyName = companyInfoDto.getCompanyName();
                if (StringUtil.isEmpty(companyName)) {
                    errorMsg.append("????????????????????????;");
                    errorFlag.set(false);
                } else if (hashSet.add(companyName.trim())) {
                    // ?????????????????????????????????
                    QueryWrapper<CompanyInfo> wrapper = new QueryWrapper<CompanyInfo>();
                    wrapper.eq("COMPANY_NAME", companyName);
                    List<CompanyInfo> list = this.list(wrapper);
                    if (!CollectionUtils.isEmpty(list)) {
                        errorMsg.append("?????????????????????;");
                        errorFlag.set(false);
                    } else {
                        companyInfo.setCompanyName(companyName.trim());
                    }
                } else {
                    errorMsg.append("??????????????????;");
                    errorFlag.set(false);
                }

                // ????????????(??????/??????)
                String companyCountry = companyInfoDto.getCompanyCountry();
                if (StringUtil.isEmpty(companyCountry)) {
                    errorMsg.append("????????????(??????/??????)????????????;");
                    errorFlag.set(false);
                } else if (StringUtil.isEmpty(dicMap.get(companyCountry.trim()))) {
                    errorMsg.append("????????????(??????/??????)??????????????????;");
                    errorFlag.set(false);
                } else if (errorFlag.get()) {
                    companyInfo.setCompanyCountry(dicMap.get(companyCountry.trim()));
                }

                //????????????
                String companyAddress = companyInfoDto.getCompanyAddress();
                if (StringUtil.isEmpty(companyAddress)) {
                    errorMsg.append("????????????????????????;");
                    errorFlag.set(false);
                } else if (errorFlag.get()) {
                    companyInfo.setCompanyAddress(companyAddress.trim());
                }

                //????????????
                String approvingDate = companyInfoDto.getApprovingDate();
                if (StringUtil.isEmpty(approvingDate)) {
                    errorMsg.append("????????????????????????;");
                    errorFlag.set(false);
                } else {
                    try {
                        Date date = DateUtil.parseDate(approvingDate.trim());
                        companyInfo.setApplicationDate(date);
                    } catch (Exception e) {
                        errorMsg.append("???????????????????????????;");
                        errorFlag.set(false);
                    }
                }

                // ????????????
                String categoryRel = companyInfoDto.getCategoryRel();
                if (StringUtil.isEmpty(categoryRel)) {
                    errorMsg.append("????????????????????????;");
                    errorFlag.set(false);
                } else {
                    if (categoryRel.contains("???")) {
                        errorMsg.append("????????????????????????????????????;");
                        errorFlag.set(false);
                    } else if (categoryRel.contains(",")) {
                        List<String> categoryRels = new ArrayList<>();
                        String[] split = categoryRel.split(",");
                        // ?????????
                        for (int i = 0; i < split.length; i++) {
                            String trim = split[i].trim();
                            if (StringUtil.notEmpty(trim)) {
                                categoryRels.add(trim);
                            }
                        }
                        // ??????????????????
                        List<PurchaseCategory> purchaseCategories = baseClient.listPurchaseCategoryByNameBatch(categoryRels);
                        if (CollectionUtils.isNotEmpty(purchaseCategories)) {
                            if (errorFlag.get()) {
                                List<CategoryRel> categoryRelList = new ArrayList<>();
                                purchaseCategories.forEach(purchaseCategory -> {
                                    CategoryRel categoryRel1 = new CategoryRel();
                                    categoryRel1.setCategoryId(purchaseCategory.getCategoryId());
                                    categoryRel1.setCategoryName(purchaseCategory.getCategoryName());
                                    categoryRel1.setCategoryCode(purchaseCategory.getCategoryCode());
                                    categoryRel1.setCategoryFullName(purchaseCategory.getCategoryFullName());
                                    categoryRelList.add(categoryRel1);
                                });
                                companyInfo.setCategoryRels(categoryRelList);
                            }
                        } else {
                            errorMsg.append("????????????????????????;");
                            errorFlag.set(false);
                        }
                    } else {
                        PurchaseCategory purchaseCategory = baseClient.getPurchaseCategoryByParm(new PurchaseCategory().setCategoryName(categoryRel.trim()));
                        if (null != purchaseCategory) {
                            if (errorFlag.get()) {
                                List<CategoryRel> categoryRelList = new ArrayList<>();
                                CategoryRel categoryRel1 = new CategoryRel();
                                categoryRel1.setCategoryId(purchaseCategory.getCategoryId());
                                categoryRel1.setCategoryName(purchaseCategory.getCategoryName());
                                categoryRel1.setCategoryCode(purchaseCategory.getCategoryCode());
                                categoryRel1.setCategoryFullName(purchaseCategory.getCategoryFullName());
                                categoryRelList.add(categoryRel1);
                                companyInfo.setCategoryRels(categoryRelList);
                            }
                        } else {
                            errorMsg.append("????????????????????????;");
                            errorFlag.set(false);
                        }
                    }
                }
                if (VendorImportEnum.RELATION_OUT.getValue().equals(companyInfoDto.getOverseasRelation().trim())) {
                    /*??????????????????, ????????????: /????????????/??????????????????/????????????/???????????????/???????????????/????????????/????????????*/

                    // ????????????
                    String registeredCapital = companyInfoDto.getRegisteredCapital();
                    if (StringUtil.isEmpty(registeredCapital)) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    } else {
                        if (errorFlag.get() && StringUtil.isDigit(registeredCapital.trim())) {
                            companyInfo.setRegisteredCapital(registeredCapital.trim());
                        }
                    }

                    // ??????????????????
                    String registCurrency = companyInfoDto.getRegistCurrency();
                    if (StringUtil.isEmpty(registCurrency)) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (StringUtil.isEmpty(dicMap.get(registCurrency.trim()))) {
                        errorMsg.append("????????????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        companyInfo.setRegistCurrency(dicMap.get(registCurrency.trim()));
                    }

                    //????????????
                    String companyCreationDate = companyInfoDto.getCompanyCreationDate();
                    if (StringUtil.isEmpty(companyCreationDate)) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    } else {
                        try {
                            Date date = DateUtil.parseDate(companyCreationDate.trim());
                            LocalDate localDate = DateUtil.dateToLocalDate(date);
                            companyInfo.setCompanyCreationDate(localDate);
                        } catch (Exception e) {
                            errorMsg.append("????????????????????????;");
                            errorFlag.set(false);
                        }
                    }

                    //???????????????
                    String businessStartDate = companyInfoDto.getBusinessStartDate();
                    if (StringUtil.isEmpty(businessStartDate)) {
                        errorMsg.append("???????????????????????????;");
                        errorFlag.set(false);
                    } else {
                        try {
                            Date date = DateUtil.parseDate(businessStartDate.trim());
                            LocalDate localDate = DateUtil.dateToLocalDate(date);
                            companyInfo.setBusinessStartDate(localDate);
                        } catch (Exception e) {
                            errorMsg.append("???????????????????????????;");
                            errorFlag.set(false);
                        }
                    }

                    // ???????????????
                    String businessEndDate = companyInfoDto.getBusinessEndDate();
                    if (StringUtil.isEmpty(businessEndDate)) {
                        errorMsg.append("???????????????????????????;");
                        errorFlag.set(false);
                    } else {
                        try {
                            Date date = DateUtil.parseDate(businessEndDate.trim());
                            LocalDate localDate = DateUtil.dateToLocalDate(date);
                            companyInfo.setBusinessEndDate(localDate);
                        } catch (Exception e) {
                            errorMsg.append("???????????????????????????;");
                            errorFlag.set(false);
                        }
                    }

                    // ????????????
                    String businessScope = companyInfoDto.getBusinessScope();
                    if (StringUtil.isEmpty(businessScope)) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        companyInfo.setBusinessScope(businessScope.trim());
                    }

                    // ????????????
                    String companyStatus = companyInfoDto.getCompanyStatus();
                    if (StringUtil.isEmpty(companyStatus)) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    } else if (StringUtil.isEmpty(dicMap.get(companyStatus.trim()))) {
                        errorMsg.append("???????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        companyInfo.setCompanyStatus(dicMap.get(companyStatus.trim()));
                    }
                } else {
                    /**
                     * 2. ???????????????-???????????????,????????????:????????????/????????????????????????/???????????????/??????/???/??????
                     * 3. ???????????????-?????????, ????????????:  ????????????/????????????/????????????????????????/???????????????/??????/???/??????/????????????/????????????
                     * 4. ??????, ????????????: ????????????/????????????/??????????????????/????????????/????????????????????????/???????????????/???????????????/???????????????/??????/???/??????/????????????/????????????
                     */
                    // ????????????
                    String companyType = companyInfoDto.getCompanyType();
                    if (StringUtil.isEmpty(companyType)) {
                        errorMsg.append("????????????????????????;");
                        errorFlag.set(false);
                    } else if (StringUtil.isEmpty(dicMap.get(companyType.trim()))) {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        companyInfo.setCompanyType(dicMap.get(companyType.trim()));
                    }

                    // ????????????????????????
                    String lcCode = companyInfoDto.getLcCode();
                    if (StringUtil.isEmpty(lcCode)) {
                        errorMsg.append("????????????????????????????????????;");
                        errorFlag.set(false);
                    } else if (hashSet.add(lcCode.trim())) {
                        QueryWrapper<CompanyInfo> wrapper = new QueryWrapper<CompanyInfo>();
                        wrapper.eq("LC_CODE", lcCode.trim());
                        List<CompanyInfo> list = this.list(wrapper);
                        if (!CollectionUtils.isEmpty(list)) {
                            errorMsg.append("??????????????????????????????;");
                            errorFlag.set(false);
                        } else {
                            companyInfo.setLcCode(lcCode.trim());
                        }
                    } else {
                        errorMsg.append("??????????????????????????????;");
                        errorFlag.set(false);
                    }

                    // ???????????????
                    String legalPerson = companyInfoDto.getLegalPerson();
                    if (StringUtil.isEmpty(legalPerson)) {
                        errorMsg.append("???????????????????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        companyInfo.setLegalPerson(legalPerson.trim());
                    }

                    // ??????/???/
                    String companyProvince = companyInfoDto.getCompanyProvince();
                    if (StringUtil.isEmpty(companyProvince)) {
                        errorMsg.append("??????/???????????????;");
                        errorFlag.set(false);
                    } else if (StringUtil.isEmpty(dicMap.get(companyProvince.trim()))) {
                        errorMsg.append("??????/????????????;");
                        errorFlag.set(false);
                    } else if (errorFlag.get()) {
                        String parentId = dicMap.get(companyProvince.trim());
                        companyInfo.setCompanyProvince(parentId);
                        // ??????
                        String companyCity = companyInfoDto.getCompanyCity();
                        if (StringUtil.isEmpty(companyCity)) {
                            errorMsg.append("??????????????????;");
                            errorFlag.set(false);
                        } else {
                            CityParamDto cityParamDto = new CityParamDto();
                            cityParamDto.setParentId(Long.parseLong(parentId));
                            cityParamDto.setAreaName(companyCity.trim());
                            List<AreaDTO> areaDTOS = baseClient.checkCity(cityParamDto);
                            if (CollectionUtils.isEmpty(areaDTOS)) {
                                errorMsg.append("??????????????????????????????;");
                                errorFlag.set(false);
                            } else if (errorFlag.get()) {
                                AreaDTO areaDTO = areaDTOS.get(0);
                                companyInfo.setCompanyCity(areaDTO.getCityId().toString());
                            }
                        }
                    }
                    /* ?????????????????????????????????,?????????????????????????????? */

                    if (StringUtil.notEmpty(companyType) && !(VendorImportEnum.COMPANY_NATURE_FEIYINGLI.getValue().equals(companyType.trim()))) {
                        // ????????????
                        String companyCreationDate = companyInfoDto.getCompanyCreationDate();
                        if (StringUtil.isEmpty(companyCreationDate)) {
                            errorMsg.append("????????????????????????;");
                            errorFlag.set(false);
                        } else {
                            try {
                                Date date = DateUtil.parseDate(companyCreationDate.trim());
                                LocalDate localDate = DateUtil.dateToLocalDate(date);
                                companyInfo.setCompanyCreationDate(localDate);
                            } catch (Exception e) {
                                errorMsg.append("????????????????????????;");
                                errorFlag.set(false);
                            }
                        }

                        // ????????????
                        String businessScope = companyInfoDto.getBusinessScope();
                        if (StringUtil.isEmpty(businessScope)) {
                            errorMsg.append("????????????????????????;");
                            errorFlag.set(false);
                        } else if (errorFlag.get()) {
                            companyInfo.setBusinessScope(businessScope.trim());
                        }

                        // ????????????
                        String companyStatus = companyInfoDto.getCompanyStatus();
                        if (StringUtil.isEmpty(companyStatus)) {
                            errorMsg.append("????????????????????????;");
                            errorFlag.set(false);
                        } else if (StringUtil.isEmpty(dicMap.get(companyStatus.trim()))) {
                            errorMsg.append("???????????????????????????;");
                            errorFlag.set(false);
                        } else if (errorFlag.get()) {
                            companyInfo.setCompanyStatus(dicMap.get(companyStatus.trim()));
                        }
                        /* ???????????????????????????,?????????????????? */

                        if (StringUtil.notEmpty(companyType) && !(VendorImportEnum.COMPANY_NATURE_GETI.getValue().equals(companyType.trim()))) {
                            // ????????????
                            String registeredCapital = companyInfoDto.getRegisteredCapital();
                            if (StringUtil.isEmpty(registeredCapital)) {
                                errorMsg.append("????????????????????????;");
                                errorFlag.set(false);
                            } else {
                                if (errorFlag.get() && StringUtil.isDigit(registeredCapital.trim())) {
                                    companyInfo.setRegisteredCapital(registeredCapital.trim());
                                }
                            }

                            // ??????????????????
                            String registCurrency = companyInfoDto.getRegistCurrency();
                            if (StringUtil.isEmpty(registCurrency)) {
                                errorMsg.append("??????????????????????????????;");
                                errorFlag.set(false);
                            } else if (StringUtil.isEmpty(dicMap.get(registCurrency.trim()))) {
                                errorMsg.append("????????????????????????????????????;");
                                errorFlag.set(false);
                            } else if (errorFlag.get()) {
                                companyInfo.setRegistCurrency(dicMap.get(registCurrency.trim()));
                            }

                            //???????????????
                            String businessStartDate = companyInfoDto.getBusinessStartDate();
                            if (StringUtil.isEmpty(businessStartDate)) {
                                errorMsg.append("???????????????????????????;");
                                errorFlag.set(false);
                            } else {
                                try {
                                    Date date = DateUtil.parseDate(businessStartDate.trim());
                                    LocalDate localDate = DateUtil.dateToLocalDate(date);
                                    companyInfo.setBusinessStartDate(localDate);
                                } catch (Exception e) {
                                    errorMsg.append("???????????????????????????;");
                                    errorFlag.set(false);
                                }
                            }

                            // ???????????????
                            String businessEndDate = companyInfoDto.getBusinessEndDate();
                            if (StringUtil.isEmpty(businessEndDate)) {
                                errorMsg.append("???????????????????????????;");
                                errorFlag.set(false);
                            } else {
                                try {
                                    Date date = DateUtil.parseDate(businessEndDate.trim());
                                    LocalDate localDate = DateUtil.dateToLocalDate(date);
                                    companyInfo.setBusinessEndDate(localDate);
                                } catch (Exception e) {
                                    errorMsg.append("???????????????????????????;");
                                    errorFlag.set(false);
                                }
                            }
                        }
                    }
                }

                /*?????????????????????*/

                // ?????????
                String isBacklist = companyInfoDto.getIsBacklist();
                if (StringUtil.notEmpty(isBacklist)) {
                    if ("N".equals(isBacklist.trim()) || "Y".equals(isBacklist.trim())) {
                        companyInfo.setIsBacklist(isBacklist.trim());
                    } else {
                        errorMsg.append("????????????????????????(Y/N);");
                        errorFlag.set(false);
                    }
                }

                // ?????????????????????
                String backlistUpdatedDate = companyInfoDto.getBacklistUpdatedDate();
                if (StringUtil.notEmpty(backlistUpdatedDate)) {
                    try {
                        Date date = DateUtil.parseDate(backlistUpdatedDate);
                        companyInfo.setBacklistUpdatedDate(date);
                    } catch (Exception e) {
                        errorMsg.append("?????????????????????????????????;");
                        errorFlag.set(false);
                    }
                }

                // ??????????????????
                String backlistUpdatedBy = companyInfoDto.getBacklistUpdatedBy();
                companyInfo.setBacklistUpdatedBy(backlistUpdatedBy);

                // ????????????
                String companyShortName = companyInfoDto.getCompanyShortName();
                companyInfo.setCompanyShortName(companyShortName);

                // DUNS??????
                String dunsCode = companyInfoDto.getDunsCode();
                companyInfoDto.setDunsCode(dunsCode);

                // ????????????
                String registrationAuthority = companyInfoDto.getRegistrationAuthority();
                companyInfo.setRegistrationAuthority(registrationAuthority);

                if (errorMsg.length() > 1) {
                    // ??????????????????
                    companyInfoDto.setErrorMsg(errorMsg.toString());
                } else {
                    companyInfoDto.setErrorMsg(null);
                }
                companyInfoDtos.add(companyInfoDto);
                if (errorFlag.get()) {
                    companyInfosMap.put(username.trim(), companyInfo);
                }
            });
            // ??????????????????
            listener.getDatas().clear();
            hashSet.clear();
        }
        return companyInfosMap;
    }

    public List<User> getUserData(AnalysisEventListenerImpl<Object> listener, ExcelReader excelReader, List<UserDto> userDtos, AtomicBoolean errorFlag) {
        List<User> users = new ArrayList<>();
        // ????????????
        ReadSheet readSheet = EasyExcel.readSheet(0).head(UserDto.class).build();
        excelReader.read(readSheet);
        List<Object> userInfoList = listener.getDatas();
        if (CollectionUtils.isNotEmpty(userInfoList)) {
            // ????????????????????????, ????????????????????????
            userInfoList.forEach(userTemp -> {
                StringBuffer errorMsg = new StringBuffer();
                User user = new User();
                UserDto userDto = (UserDto) userTemp;
                if (StringUtil.isEmpty(userDto.getUsername())) {
                    errorFlag.set(false);
                    errorMsg.append("?????????????????????;");
                } else {
                    String username = userDto.getUsername().trim();
                    //???????????????????????????
                    List<User> userList = rbacClient.checkByUsername(username);
                    if (CollectionUtils.isNotEmpty(userList)) {
                        errorFlag.set(false);
                        errorMsg.append("??????????????????;");
                    } else if (errorFlag.get()) {
                        user.setUsername(username);
                    }
                }

                if (StringUtil.isEmpty(userDto.getPassword())) {
                    errorMsg.append("??????????????????;");
                    errorFlag.set(false);
                } else if (errorFlag.get()) {
                    user.setPassword(userDto.getPassword().trim());
                }
                // ??????????????????
                this.setErrorMag(errorMsg, userDto);
                userDtos.add(userDto);
                if (errorFlag.get()) {
                    users.add(user);
                }
            });
        }
        // ??????????????????
        listener.getDatas().clear();
        return users;
    }

    public void setErrorMag(StringBuffer errorMsg, UserDto userDto) {
        if (errorMsg.length() > 1) {
            userDto.setErrorMsg(errorMsg.toString());
        } else {
            userDto.setErrorMsg(null);
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public Map<String, String> getImportDic() {
        HashMap<String, String> dicMap = new HashMap<>();
        // ??????
        List<PurchaseCurrency> purchaseCurrencies = baseClient.listCurrencyAll();
        if (CollectionUtils.isNotEmpty(purchaseCurrencies)) {
            purchaseCurrencies.forEach(purchaseCurrency -> {
                dicMap.put(purchaseCurrency.getCurrencyName(), purchaseCurrency.getCurrencyCode());
            });
        }
        // ???????????????
        AreaPramDTO areaPramDTO = new AreaPramDTO();
        areaPramDTO.setQueryType("province");
        List<AreaDTO> regions = baseClient.queryRegionById(areaPramDTO);
        if (CollectionUtils.isNotEmpty(regions)) {
            regions.forEach(region -> {
                dicMap.put(region.getProvince(), region.getProvinceId().toString());
            });
        }

        // ????????????
        ArrayList<String> dicCodeList = new ArrayList<>();
        // ???????????????
        dicCodeList.add(DictionaryValue.RELATION);
        // ????????????
        dicCodeList.add(DictionaryValue.COUNTRY);
        // ????????????
        dicCodeList.add(DictionaryValue.COMPANY_NATURE);
        // ????????????
        dicCodeList.add(DictionaryValue.COMPANY_STATUS);
        // ????????????
        dicCodeList.add(DictionaryValue.PAYMENT_METHOD);
        // ????????????
        dicCodeList.add(DictionaryValue.PAYMENT_TERMS);
        // ????????????
        dicCodeList.add(DictionaryValue.INVOICE_LIMIT);
        // ????????????
        dicCodeList.add(DictionaryValue.BANK_ACCOUNT_TYPE);
        //????????????
        dicCodeList.add(DictionaryValue.ORG_STATUS);
        // ????????????
        dicCodeList.add(DictionaryValue.CATEGORY_STATUS);
        // ????????????
        dicCodeList.add(DictionaryValue.BIZ_MODEL);
        // ????????????
        dicCodeList.add(DictionaryValue.FACTORY_TYPE);
        // ????????????
        dicCodeList.add(DictionaryValue.EMPLOYEE_QTY);

        // ????????????
        List<DictItemDTO> dictItemDTOS = baseClient.listByDictCode(dicCodeList);
        // ???????????????Map???
        this.setMapKeyValue(dicMap, dictItemDTOS);
        return dicMap;
    }

    /**
     * ???????????????Map???
     *
     * @param dicMap
     * @param dictItemDTOS1
     */
    public void setMapKeyValue(HashMap<String, String> dicMap, List<DictItemDTO> dictItemDTOS1) {
        if (CollectionUtils.isNotEmpty(dictItemDTOS1)) {
            dictItemDTOS1.forEach(dictItemDTOS -> {
                dicMap.put(dictItemDTOS.getDictItemName(), dictItemDTOS.getDictItemCode());
            });
        }
    }

    /**
     * <pre>
     *  ?????????????????????????????????
     * </pre>
     *
     * @author chenwt24@meicloud.com
     * @version 1.00.00
     *
     * <pre>
     *  ????????????
     *  ???????????????:
     *  ?????????:
     *  ????????????: 2020-09-27
     *  ????????????:
     * </pre>
     */
    public void sendEmailForVendorSubmit(InfoDTO infoDTO) {
        //??????????????????????????????????????????????????????????????????OU?????????????????????????????????,
        // ????????????????????????id??????????????????id??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        List<OrgInfo> orgInfos = infoDTO.getOrgInfos();
        Assert.notEmpty(orgInfos, "??????????????????");
        List<OrgCategory> orgCategorys = infoDTO.getOrgCategorys();
        Assert.notEmpty(orgCategorys, "??????????????????");


        OrgInfo orgInfo = orgInfos.get(0);
        OrgCategory orgCategory = orgCategorys.get(0);


        Long responsibilityId = null;
        //FIXME ???????????????????????????
        try {
            SupplierLeader categoryResponsibility = categoryResponsibilityMapper.selectOne(
                    Wrappers.lambdaQuery(SupplierLeader.class).eq(SupplierLeader::getCategoryId, orgCategory.getCategoryId()).eq(SupplierLeader::getOrgId, orgInfo.getOrgId()));
            responsibilityId = categoryResponsibility.getResponsibilityId();
        } catch (Exception e) {
            //??????????????????????????????????????????????????????????????????????????? -- scc_rbac_user USER_ID 7647858847449088 -- empNo:167491L
//            responsibilityId = 167491L;
        	
        }

        User user = rbacClient.getUser(new User().setCeeaEmpNo(String.valueOf(responsibilityId)));
        if (null != user) {
        	 Assert.notNull(user, "???????????????????????????ceeaEmpNo:" + responsibilityId);
             String companyName = infoDTO.getCompanyInfo().getCompanyName();
             try {
                 baseClient.sendEmail(user.getEmail(), "???????????????-SRM????????????????????????????????????", String.format("<p>%s,????????????????????????!</p>", companyName)); // TODO SRM??????????????????????????????
             } catch (Exception ex) {
                 log.error("????????????????????????????????????????????????", ex);
                 throw new BaseException("????????????????????????????????????????????????");
             }
        }
       

    }

}
