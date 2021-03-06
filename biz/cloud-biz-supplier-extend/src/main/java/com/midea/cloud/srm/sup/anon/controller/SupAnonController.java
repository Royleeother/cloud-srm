package com.midea.cloud.srm.sup.anon.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.enums.ApproveStatusType;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.SessionUtil;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.supplier.change.dto.ChangeInfoDTO;
import com.midea.cloud.srm.model.supplier.demotion.entity.CompanyDemotion;
import com.midea.cloud.srm.model.supplier.info.dto.InfoDTO;
import com.midea.cloud.srm.model.supplier.info.dto.OrgCategorySaveDTO;
import com.midea.cloud.srm.model.supplier.info.entity.*;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorContact.VendorContactOutputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorInfo.VendorInfoOutputParameters;
import com.midea.cloud.srm.model.supplier.soap.erp.vendor.vendorSite.VendorSiteOutputParameters;
import com.midea.cloud.srm.model.supplier.vendorimport.dto.VendorImportSaveDTO;
import com.midea.cloud.srm.model.supplier.vendororgcategory.vo.FindVendorOrgCateRelParameter;
import com.midea.cloud.srm.model.supplier.vendororgcategory.vo.VendorOrgCateRelsVO;
import com.midea.cloud.srm.sup.change.service.IInfoChangeService;
import com.midea.cloud.srm.sup.demotion.service.ICompanyDemotionService;
import com.midea.cloud.srm.sup.info.service.*;
import com.midea.cloud.srm.sup.register.service.IRegisterService;
import com.midea.cloud.srm.sup.responsibility.service.ISupplierLeaderService;
import com.midea.cloud.srm.sup.soap.erp.vendor.service.IVendorContactService;
import com.midea.cloud.srm.sup.soap.erp.vendor.service.IVendorInfoService;
import com.midea.cloud.srm.sup.soap.erp.vendor.service.IVendorSiteService;
import com.midea.cloud.srm.sup.vendorimport.service.IVendorImportService;
import com.midea.cloud.srm.sup.vendororgcategory.service.IVendorOrgCateRelService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ????????????????????????????????????????????????????????????(????????????????????????????????????????????????)
 * </pre>
 *
 * @author huanghb14@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-4-10 16:24
 *  ????????????:
 * </pre>
 */
@RestController
@RequestMapping("/sup-anon/internal")
public class SupAnonController extends BaseController {

    @Autowired
    private ICompanyInfoService iCompanyInfoService;

    @Autowired
    private IOrgCategoryService iOrgCategoryService;

    @Autowired
    private IOrgInfoService iOrgInfoService;

    @Autowired
    private IBankInfoService iBankInfoService;

    @Autowired
    private ISiteInfoService iSiteInfoService;

    @Autowired
    private IFinanceInfoService iFinanceInfoService;

    @Autowired
    private IRegisterService iRegisterService;

    @Autowired
    private IVendorInfoService iVendorInfoService;

    @Autowired
    private IVendorSiteService iVendorSiteService;

    @Autowired
    private IContactInfoService iContactInfoService;

    @Autowired
    private IVendorContactService iVendorContactService;

    @Autowired
    private IInfoChangeService iInfoChangeService;

    @Autowired
    private IVendorImportService iVendorImportService;

    @Autowired
    private IVendorOrgCateRelService relService;

    @Autowired
    private ISupplierLeaderService iSupplierLeaderService;


    @Autowired
    private ICompanyDemotionService iCompanyDemotionService;

    /**
     * ?????????????????????
     * @param companyDemotionId
     */
    @GetMapping("/demotion/demotionPassAnon")
    public void demotionPassAnon(@RequestParam("companyDemotionId") Long companyDemotionId) {
        Assert.notNull(companyDemotionId, "???????????????id???????????????");
        // ???????????????
        CompanyDemotion demotion = iCompanyDemotionService.getById(companyDemotionId);
        Assert.isTrue(Objects.equals(ApproveStatusType.SUBMITTED.getValue(), demotion.getStatus()), "?????????????????????????????????");
        iCompanyDemotionService.approve(demotion);
    }


    /**
     * ????????????????????? ????????????????????????
     *
     * @param email
     */
    @GetMapping("/sendVerifyCodeToEmailNew")
    public void sendVerifyCodeToEmailNew(@RequestParam("email") String email) {
        iRegisterService.sendVerifyCodeToEmailNew(email);
    }

    /**
     * ???????????????????????????(ceea)
     *
     * @param verifyCode
     */
    @GetMapping("/checkVerifyCodeByEmail")
    public void checkVerifyCodeByEmail(@RequestParam("verifyCode") String verifyCode, @RequestParam("email") String email) {
        iRegisterService.checkVerifyCodeByEmail(verifyCode, email);
    }

    /**
     * ??????
     *
     * @param companyId
     */
    @GetMapping("/info/companyInfo/get")
    public CompanyInfo getCompanyInfoById(Long companyId) {
        Assert.notNull(companyId, "id????????????");
        return iCompanyInfoService.getById(companyId);
    }


    /**
     * ??????
     *
     * @param companyInfo
     */
    @PostMapping("/info/companyInfo/modify")
    public void modify(@RequestBody CompanyInfo companyInfo) {
        iCompanyInfoService.updateById(companyInfo);
    }


    /**
     * ????????????ID?????????ID?????????ID?????????????????????
     *
     * @param categoryId
     * @param orgId
     * @param companyId
     * @return
     */
    @GetMapping("/info/orgCategory/getByCategoryIdAndOrgIdAndCompanyId")
    public OrgCategory getByCategoryIdAndOrgIdAndCompanyId(Long categoryId, Long orgId, Long companyId) {
        return iOrgCategoryService.getByCategoryIdAndOrgIdAndCompanyId(categoryId, orgId, companyId);
    }

    /**
     * ????????????ID?????????ID??????????????????
     *
     * @param orgId
     * @param companyId
     * @return
     */
    @GetMapping("/info/orgInfo/getOrgInfoByOrgIdAndCompanyId")
    public OrgInfo getOrgInfoByOrgIdAndCompanyId(Long orgId, Long companyId) {
        return iOrgInfoService.getOrgInfoByOrgIdAndCompanyId(orgId, companyId);
    }


    /**
     * ??????
     *
     * @param orgCategory
     */
    @PostMapping("/info/orgCategory/addOrgCategory")
    public void addOrgCategory(@RequestBody OrgCategory orgCategory) {
        Long id = IdGenrator.generate();
        orgCategory.setOrgCategoryId(id);
        iOrgCategoryService.save(orgCategory);
    }

    /**
     * ?????????????????????????????????
     *
     * @param orgCategory
     */
    @PostMapping("/info/orgCategory/updateOrgCategoryServiceStatus")
    public void updateOrgCategoryServiceStatus(@RequestBody OrgCategory orgCategory) {
        iOrgCategoryService.updateOrgCategoryServiceStatus(orgCategory);
    }

    /**
     * ??????
     *
     * @param orgInfo
     */
    @PostMapping("/info/orgInfo/addOrgInfo")
    public void addOrgInfo(@RequestBody OrgInfo orgInfo) {
        Long id = IdGenrator.generate();
        orgInfo.setOrgInfoId(id);
        iOrgInfoService.save(orgInfo);
    }

    /**
     * ????????????????????????
     *
     * @param orgInfo
     */
    @PostMapping("/info/orgInfo/updateOrgInfoServiceStatus")
    public void updateOrgInfoServiceStatus(@RequestBody OrgInfo orgInfo) {
        iOrgInfoService.updateOrgInfoServiceStatus(orgInfo);
    }

    /**
     * ??????????????????
     *
     * @param bankInfo
     * @return
     */
    @PostMapping("/info/bankInfo/getBankInfoByParm")
    public BankInfo getBankInfoByParm(@RequestBody BankInfo bankInfo) {
        return iBankInfoService.getBankInfoByParm(bankInfo);
    }

    /**
     * ??????????????????????????????
     *
     * @param bankInfo
     * @return
     */
    @PostMapping("/info/bankInfo/getBankInfosByParam")
    public List<BankInfo> getBankInfosByParam(@RequestBody BankInfo bankInfo) {
        return iBankInfoService.getBankInfosByParam(bankInfo);
    }

    /**
     * ?????????????????????????????????
     *
     * @param contactInfo
     * @return
     */
    @PostMapping("/info/contactInfo/getContactInfoByParam")
    public List<ContactInfo> getContactInfosByParam(@RequestBody ContactInfo contactInfo) {
        return iContactInfoService.getContactInfosByParam(contactInfo);
    }

    /**
     * ??????
     *
     * @param bankInfo
     */
    @PostMapping("/info/bankInfo/addBankInfo")
    public void addBankInfo(@RequestBody BankInfo bankInfo) {
        iBankInfoService.saveOrUpdateBank(bankInfo);
    }

    /**
     * ??????????????????
     *
     * @param siteInfo
     * @modifiedBy xiexh12@meicloud.com
     */
    @PostMapping("/info/siteInfo/getSiteInfoByParm")
    public SiteInfo getSiteInfoByParm(@RequestBody SiteInfo siteInfo) {
        return iSiteInfoService.getSiteInfoByParm(siteInfo);
    }

    /**
     * ??????????????????
     *
     * @param siteInfo
     * @return
     */
    @PostMapping("/info/siteInfo/getSiteInfosByParam")
    public List<SiteInfo> getSiteInfosByParam(@RequestBody SiteInfo siteInfo) {
        return iSiteInfoService.getSiteInfosByParam(siteInfo);
    }

    /**
     * ??????/????????????????????????
     *
     * @param siteInfo
     * @modifiedBy xiexh12@meicloud.com
     */
    @PostMapping("/info/siteInfo/addSiteInfo")
    public void addSiteInfo(@RequestBody SiteInfo siteInfo) {
        iSiteInfoService.saveOrUpdateSite(siteInfo);
    }

    /**
     * ??????/???????????????????????????
     *
     * @param contactInfo
     * @modifiedBy xiexh12@meicloud.com
     */
    @PostMapping("/info/contactInfo/addContactInfo")
    public void addContactInfo(@RequestBody ContactInfo contactInfo) {
        iContactInfoService.saveOrUpdateContact(contactInfo);
    }

    /**
     * ????????????ID???????????????ID??????????????????
     *
     * @param companyId
     * @param orgId
     * @return
     */
    @GetMapping("/info/financeInfo/getByCompanyIdAndOrgId")
    public FinanceInfo getByCompanyIdAndOrgId(Long companyId, Long orgId) {
        return iFinanceInfoService.getByCompanyIdAndOrgId(companyId, orgId);
    }

    /**
     * ??????
     *
     * @param financeInfo
     */
    @PostMapping("/info/financeInfo/addFinanceInfo")
    public void addFinanceInfo(@RequestBody FinanceInfo financeInfo) {
        Long id = IdGenrator.generate();
        financeInfo.setFinanceInfoId(id);
        iFinanceInfoService.save(financeInfo);
    }


    /**
     * ???????????????
     *
     * @param verifyCode
     */
    @PostMapping("/register/checkVerifyCode")
    public void checkVerifyCode(String verifyCode) {
        iRegisterService.checkVerifyCode(verifyCode, SessionUtil.getRequest());
    }

    /**
     * ???????????????????????????
     *
     * @param verifyCode
     */
    @PostMapping("/register/checkVerifyCodeByKey")
    public Boolean checkVerifyCodeByKey(String verifyCode, String verifyKey) {
        return iRegisterService.checkVerifyCode(verifyCode, verifyKey, SessionUtil.getRequest());
    }

    //?????????????????????

    /**
     * ??????
     *
     * @param companyId
     */
    @GetMapping("/get")
    public CompanyInfo get(Long companyId) {
        Assert.notNull(companyId, "id????????????");
        return iCompanyInfoService.getById(companyId);
    }

    /**
     * NSrm???Erp???????????????????????????
     *
     * @param companyInfo
     * @return
     */
    @PostMapping("/erp/vendor/sendVendorInfo")
    public VendorInfoOutputParameters sendVendorInfo(@RequestBody CompanyInfo companyInfo) {
        VendorInfoOutputParameters response = iVendorInfoService.sendVendorInfo(companyInfo);
        return response;
    }

    /**
     * NSrm???Erp???????????????????????????
     *
     * @param siteInfo
     * @return
     */
    @RequestMapping("/erp/vendor/sendVendorSite")
    public VendorSiteOutputParameters sendVendorSite(@RequestBody SiteInfo siteInfo) {
        VendorSiteOutputParameters response = iVendorSiteService.sendVendorSite(siteInfo);
        return response;
    }

    /**
     * NSrm???Erp??????????????????????????????
     *
     * @param contactInfo
     * @return
     */
    @PostMapping("/erp/vendor/sendVendorContact")
    public VendorContactOutputParameters sendVendorContact(@RequestBody ContactInfo contactInfo) {
        VendorContactOutputParameters response = iVendorContactService.sendVendorContact(contactInfo);
        return response;
    }

    /**
     * ??????companyId?????????????????????????????? ??????????????????????????????
     *
     * @param companyId
     */
    @PostMapping("/info/contactInfo/getContactInfoByCompanyId")
    public ContactInfo getContactInfoByCompanyId(@RequestParam("companyId") Long companyId) {
        Assert.notNull(companyId, "???????????????Id?????????????????????????????????Id?????????");
        return iContactInfoService.getContactInfoByCompanyId(companyId);
    }

    @PostMapping("/info/companyInfo/getVendorClassificationByCompanyIds")
    public List<CompanyInfo> getVendorClassificationByCompanyIdsForAnon(@RequestBody Collection<Long> companyIds) {
        if (CollectionUtils.isEmpty(companyIds)) {
            return Collections.emptyList();
        }
        return iCompanyInfoService.list(Wrappers.lambdaQuery(CompanyInfo.class)
                .select(CompanyInfo::getCompanyId, CompanyInfo::getVendorClassification)
                .in(CompanyInfo::getCompanyId, companyIds)
        ).stream().collect(Collectors.toList());
    }

    @PostMapping("/info/companyInfo/getOrgRelation")
    public List<OrgCategory> getOrgRelationByCompanyIdsForAnon(@RequestBody Map<String, Collection<Long>> companyIdAndCategoryIds) {
        Collection<Long> companyIds = companyIdAndCategoryIds.get("companyIds");
        Collection<Long> categoryIds = companyIdAndCategoryIds.get("categoryIds");
        if (CollectionUtils.isEmpty(companyIds) || CollectionUtils.isEmpty(categoryIds)) {
            return Collections.emptyList();
        }
        return iOrgCategoryService.list(Wrappers.lambdaQuery(OrgCategory.class)
                .select(OrgCategory::getCompanyId, OrgCategory::getCategoryId, OrgCategory::getOrgCategoryId
                        , OrgCategory::getServiceStatus)
                .in(OrgCategory::getCategoryId, categoryIds)
                .in(OrgCategory::getCompanyId, companyIds)
        );
    }

    /**
     * ????????????????????????
     *
     * @param orgCategoryList
     * @return
     */
    @PostMapping("/orgCategory/listOrgCategory")
    public List<OrgCategory> listOrgCategory(@RequestBody List<OrgCategory> orgCategoryList) {
        if (orgCategoryList.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> orgCodes = new HashSet<>();
        Set<String> categoryCodes = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> orgIds = new HashSet<>();
        for (OrgCategory orgCategory : orgCategoryList) {
            if (!StringUtils.isEmpty(orgCategory.getOrgCode())) {
                orgCodes.add(orgCategory.getOrgCode());
            }
            if (!StringUtils.isEmpty(orgCategory.getCategoryCode())) {
                categoryCodes.add(orgCategory.getCategoryCode());
            }
            if (Objects.nonNull(orgCategory.getCategoryId())) {
                categoryIds.add(orgCategory.getCategoryId());
            }
            if (Objects.nonNull(orgCategory.getOrgId())) {
                orgIds.add(orgCategory.getOrgId());
            }
        }
        List<OrgCategory> list = iOrgCategoryService.list(Wrappers.lambdaQuery(OrgCategory.class)
                .in(!categoryIds.isEmpty(), OrgCategory::getCategoryId, categoryIds)
                .in(!orgIds.isEmpty(), OrgCategory::getOrgId, orgIds)
                .in(!orgCodes.isEmpty(), OrgCategory::getOrgCode, orgCodes)
                .in(!categoryCodes.isEmpty(), OrgCategory::getCategoryCode, categoryCodes)
        );
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        Set<Long> companyIds = list.stream().map(OrgCategory::getCompanyId).collect(Collectors.toSet());
        Function<CompanyInfo,String> getCode= companyInfo -> Optional.ofNullable(companyInfo.getCompanyCode()).orElse("");
        Map<Long, String> resultMap = iCompanyInfoService.list(Wrappers.lambdaQuery(CompanyInfo.class)
                .select(CompanyInfo::getCompanyId, CompanyInfo::getCompanyCode)
                .in(CompanyInfo::getCompanyId, companyIds)
        ).stream().collect(Collectors.toMap(CompanyInfo::getCompanyId,getCode));
        for (OrgCategory orgCategory : list) {
            orgCategory.setCompanyCode(resultMap.get(orgCategory.getCompanyId()));
        }
        return list;
    }

    /**
     * ??????????????????
     *
     * @param changeId
     * @return
     */
    @GetMapping("/change/infoChange/getInfoByChangeId")
    public ChangeInfoDTO getInfoByChangeId(@RequestParam("changeId") Long changeId) {
        return iInfoChangeService.getInfoByChangeId(changeId);
    }

    /**
     * ???????????????????????????
     *
     * @param changeInfo
     */
    @PostMapping("/change/infoChange/InfoChangeApprove")
    public void InfoChangeApprove(@RequestBody ChangeInfoDTO changeInfo) {
        iInfoChangeService.commonCheck(changeInfo, ApproveStatusType.APPROVED.getValue());
        iInfoChangeService.saveOrUpdateChange(changeInfo, ApproveStatusType.APPROVED.getValue());
    }

    /**
     * ?????????
     *
     * @param changeInfo
     */
    @PostMapping("/change/infoChange/InfoChangeRejected")
    public void InfoChangeRejected(@RequestBody ChangeInfoDTO changeInfo) {
        iInfoChangeService.commonCheck(changeInfo, ApproveStatusType.APPROVED.getValue());
        iInfoChangeService.updateChange(changeInfo, ApproveStatusType.REJECTED.getValue());
    }

    /**
     * ?????????
     *
     * @param changeInfo
     */
    @PostMapping("/change/infoChange/InfoChangeWithdraw")
    public void InfoChangeWithdraw(@RequestBody ChangeInfoDTO changeInfo) {
        iInfoChangeService.commonCheck(changeInfo, ApproveStatusType.APPROVED.getValue());
        iInfoChangeService.updateChange(changeInfo, ApproveStatusType.WITHDRAW.getValue());
    }


    /**
     * ????????????Id???????????????????????????
     *
     * @param importId
     * @return
     */
    @GetMapping("/vendorImport/getVendorImportDetail")
    public VendorImportSaveDTO getVendorImportDetail(@RequestParam("importId") Long importId) {
        Assert.notNull(importId, "importId???????????????");
        return iVendorImportService.getVendorImportDetail(importId);
    }

    /**
     * ?????????????????????
     *
     * @param importId
     */
    @GetMapping("/vendorImport/VendorImportApprove")
    public void VendorImportApprove(@RequestParam("importId") Long importId) {
        Assert.notNull(importId, "importId?????????");
        iVendorImportService.approve(importId);
    }

    /**
     * ?????????????????????
     *
     * @param importId
     */
    @GetMapping("/vendorImport/VendorImportReject")
    public void VendorImportReject(Long importId) {
        Assert.notNull(importId, "importId?????????");
        iVendorImportService.reject(importId, null);
    }


    /**
     * ?????????????????????
     *
     * @param importId
     */
    @GetMapping("/vendorImport/VendorImportWithdraw")
    public void VendorImportWithdraw(Long importId) {
        Assert.notNull(importId, "importId?????????");
        iVendorImportService.withdraw(importId, null);
    }

    @PostMapping("/vendorOrgCateRel/findVendorOrgCateRels")
    public List<VendorOrgCateRelsVO> findVendorOrgCateRels(@RequestBody FindVendorOrgCateRelParameter parameter) {
        return relService.findVendorOrgCateRels(parameter);
    }

    /**
     * ??????Info
     */
    @GetMapping("/info/companyInfo/getInfoByParam")
    public InfoDTO getInfoByParam(Long companyId) {
        return iCompanyInfoService.getInfoByParam(companyId);
    }

    /**
     * ????????????????????????
     *
     * @modified xiexh12@meicloud.com
     */
    @PostMapping("/info/companyInfo/companyGreenChannelApprove")
    public Long companyGreenChannelApprove(@RequestBody InfoDTO infoDTO) {
        Assert.isTrue(!ObjectUtils.isEmpty(infoDTO), "?????????????????????????????????");
        return iCompanyInfoService.companyGreenChannelApprove(infoDTO);
    }

    /**
     * ??????????????????
     *
     * @param companyInfo
     */
    @PostMapping("/info/companyInfo/CompanyInfoReject")
    public void CompanyInfoReject(@RequestBody CompanyInfo companyInfo) {
        Assert.isTrue(!ObjectUtils.isEmpty(companyInfo), "???????????????????????????");
        companyInfo.setStatus(ApproveStatusType.REJECTED.getValue());
        iCompanyInfoService.updateById(companyInfo);
    }

    /**
     * ??????????????????
     *
     * @param companyInfo
     */
    @PostMapping("/info/companyInfo/CompanyInfoWithdraw")
    public void CompanyInfoWithdraw(@RequestBody CompanyInfo companyInfo) {
        Assert.isTrue(!ObjectUtils.isEmpty(companyInfo), "???????????????????????????");
        companyInfo.setStatus(ApproveStatusType.WITHDRAW.getValue());
        iCompanyInfoService.updateById(companyInfo);
    }

    /**
     * ????????????????????????supplier leader??????
     *
     * @param companyId
     * @param responsibilityId
     */
    @PostMapping("/supplier-leader/saveOrUpdateSupplierLeaderForAnon")
    public void saveOrUpdateSupplierLeaderForAnon(@RequestParam("companyId") Long companyId,
                                                  @RequestParam("responsibilityId") Long responsibilityId) {
        iSupplierLeaderService.saveOrUpdateSupplierLeader(companyId, responsibilityId);
    }


    @PostMapping("/companyInfo/listCompanyByCodes")
    List<CompanyInfo> listCompanyByCodes(@RequestBody List<String> companyCodes) {
        if (companyCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return iCompanyInfoService.list(Wrappers.lambdaQuery(CompanyInfo.class)
                .in(CompanyInfo::getCompanyCode, companyCodes));
    }


    @PostMapping("orgCategory/listOrgCategoryByCompanyIds")
    List<OrgCategory> listOrgCategoryByCompanyIds(@RequestBody List<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return new ArrayList<>();
        }
        return iOrgCategoryService.list(Wrappers.lambdaQuery(OrgCategory.class)
                .in(OrgCategory::getCompanyId, companyIds));
    }

    /**
     * ????????????????????????
     *
     * @param orgCategorySaveDTO add by chensl26
     */
    @PostMapping("/info/orgCategory/collectOrgCategory")
    public void collectOrgCategory(@RequestBody OrgCategorySaveDTO orgCategorySaveDTO) {
        iOrgCategoryService.collectOrgCategory(orgCategorySaveDTO);
    }


    /**
     * ???????????????erpcode?????????????????????
     *
     * @param erpCodes
     */
    @PostMapping("/companyInfo/getErpCodes")
    public List<CompanyInfo> getErpCodes(@RequestBody List<String> erpCodes) {
        if (!CollectionUtils.isEmpty(erpCodes)) {
            QueryWrapper<CompanyInfo> queryCompanyInfoWrapper = new QueryWrapper<>();
            queryCompanyInfoWrapper.in("ERP_VENDOR_CODE", erpCodes);
            return iCompanyInfoService.list(queryCompanyInfoWrapper);
        }else {
            return null;
        }
    }

}
