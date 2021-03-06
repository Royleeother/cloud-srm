package com.midea.cloud.srm.base.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SpecialBusinessConstant;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.base.BigCategoryTypeEnum;
import com.midea.cloud.common.enums.pm.po.OrderTypeEnum;
import com.midea.cloud.common.enums.pm.po.PriceSourceTypeEnum;
import com.midea.cloud.common.enums.rbac.RoleEnum;
import com.midea.cloud.common.enums.review.CategoryStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.handler.TitleColorSheetWriteHandler;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.common.utils.redis.RedisUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.base.busiunit.service.IErpService;
import com.midea.cloud.srm.base.job.MaterialCache;
import com.midea.cloud.srm.base.material.mapper.CategoryBusinessMapper;
import com.midea.cloud.srm.base.material.mapper.MaterialItemMapper;
import com.midea.cloud.srm.base.material.service.ICategoryBusinessService;
import com.midea.cloud.srm.base.material.service.IItemImageService;
import com.midea.cloud.srm.base.material.service.IMaterialItemService;
import com.midea.cloud.srm.base.material.service.IMaterialOrgService;
import com.midea.cloud.srm.base.material.utils.MaterialItemExportUtils;
import com.midea.cloud.srm.base.organization.service.IErpMaterialItemService;
import com.midea.cloud.srm.base.organization.service.IOrganizationService;
import com.midea.cloud.srm.base.purchase.mapper.PurchaseTaxMapper;
import com.midea.cloud.srm.base.purchase.service.IPurchaseCategoryService;
import com.midea.cloud.srm.base.purchase.service.IPurchaseUnitService;
import com.midea.cloud.srm.base.quotaorder.service.ICommonsService;
import com.midea.cloud.srm.base.serviceconfig.service.IBaseServiceConfigService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.contract.ContractClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.inq.InqClient;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.material.MaterialOrg;
import com.midea.cloud.srm.model.base.material.dto.*;
import com.midea.cloud.srm.model.base.material.entity.CategoryBusiness;
import com.midea.cloud.srm.model.base.material.entity.ItemImage;
import com.midea.cloud.srm.model.base.material.enums.CeeaMaterialStatus;
import com.midea.cloud.srm.model.base.material.vo.MaterialItemVo;
import com.midea.cloud.srm.model.base.material.vo.MaterialMaxCategoryVO;
import com.midea.cloud.srm.model.base.organization.entity.Category;
import com.midea.cloud.srm.model.base.organization.entity.ErpMaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseTax;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseUnit;
import com.midea.cloud.srm.model.base.serviceconfig.entity.ServiceConfig;
import com.midea.cloud.srm.model.base.soap.DataSourceEnum;
import com.midea.cloud.srm.model.cm.contract.dto.ContractItemDto;
import com.midea.cloud.srm.model.cm.contract.entity.ContractPartner;
import com.midea.cloud.srm.model.cm.contract.vo.ContractVo;
import com.midea.cloud.srm.model.common.ExportExcelParam;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibrary;
import com.midea.cloud.srm.model.pm.pr.shopcart.entity.ShopCart;
import com.midea.cloud.srm.model.pm.pr.shopcart.enums.ShopCartStatus;
import com.midea.cloud.srm.model.rbac.role.entity.Role;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.ContactInfo;
import com.midea.cloud.srm.model.supplier.info.entity.OrgCategory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
 *  ????????????: 2020-03-06 15:22:48
 *  ????????????:
 * </pre>
 */
@Slf4j
@Service
public class MaterialItemServiceImpl extends ServiceImpl<MaterialItemMapper, MaterialItem> implements IMaterialItemService {
    @Autowired
    private IMaterialItemService iMaterialItemService;

    @Resource
    private IPurchaseCategoryService iPurchaseCategoryService;

    @Resource
    private IBaseServiceConfigService iBaseServiceConfigService;

    @Autowired
    private ICategoryBusinessService iCategoryBusinessService;

    /**
     * ??????-????????????Service
     */
    @Resource
    private IMaterialOrgService iMaterialOrgService;
    @Autowired
    private SupplierClient supplierClient;
    @Autowired
    private InqClient inqClient;
    @Autowired
    private PmClient pmClient;
    @Autowired
    FileCenterClient fileCenterClient;
    @Autowired
    private ContractClient contractClient;

    @Autowired
    private IErpMaterialItemService iErpMaterialItemService;

    @Autowired
    private IOrganizationService iOrganizationService;

    @Autowired
    private IPurchaseUnitService iPurchaseUnitService;

    @Resource
    private PurchaseTaxMapper purchaseTaxMapper;

    @Autowired
    private BaseClient baseClient;

    @Resource
    private MaterialItemMapper materialItemMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IPurchaseCategoryService purchaseCategoryService;
    @Resource
    private IItemImageService iItemImageService;
    @Resource
    private ICommonsService iCommonsService;

    /* ??????????????????????????????????????? Y, ?????????????????? N */
    public static final HashSet<String> itemStatusSet;

    static {
        itemStatusSet = new HashSet<String>();

        itemStatusSet.add("Active");
        itemStatusSet.add("??????");
        itemStatusSet.add("??????");

    }

    @Override
    public Map<String, String> queryItemIdUserPurchase(ItemCodeUserPurchaseDto itemCodeUserPurchaseDto) {
        Long invId = itemCodeUserPurchaseDto.getInvId();
        Long orgId = itemCodeUserPurchaseDto.getOrgId();
        List<String> itemCodes = itemCodeUserPurchaseDto.getItemCodes();
        Map<String, String> map = new HashMap<>();
        if(StringUtil.notEmpty(invId) && StringUtil.notEmpty(orgId) && CollectionUtils.isNotEmpty(itemCodes)){
            itemCodes = itemCodes.stream().distinct().collect(Collectors.toList());
            itemCodeUserPurchaseDto.setItemCodes(itemCodes);
            List<ItemCodeUserPurchaseDto> itemCodeUserPurchaseDtos = this.baseMapper.queryItemIdUserPurchase(itemCodeUserPurchaseDto);
            if(CollectionUtils.isNotEmpty(itemCodeUserPurchaseDtos)){
                map = itemCodeUserPurchaseDtos.stream().filter(materialOrg ->
                        StringUtil.notEmpty(materialOrg.getMaterialCode())
                                && StringUtil.notEmpty(materialOrg.getUserPurchase())).
                        collect(Collectors.toMap(ItemCodeUserPurchaseDto::getMaterialCode, k-> "Y".equals(k.getUserPurchase()) && "Y".equals(k.getItemStatus()) ? "Y":"N",(k1,k2)->k1));
            }

        }
        return map;
    }

    @Override
    public Map<String,MaterialItem> ListMaterialItemByCategoryCode(List<String> itemCodes) {
        Map<String, MaterialItem> itemHashMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(itemCodes)) {
            QueryWrapper<MaterialItem> wrapper = new QueryWrapper<>();
            wrapper.in("MATERIAL_CODE", itemCodes);
            List<MaterialItem> materialItems = materialItemMapper.ListMaterialItemByCategoryCode(wrapper);
            if (CollectionUtils.isNotEmpty(materialItems)) {
                itemHashMap = materialItems.stream().collect(Collectors.toMap(MaterialItem::getMaterialCode,Function.identity(), (k1, k2) -> k1));
            }
        }
        return itemHashMap;
    }

    @Override
    public List<MaterialItem> queryMaterialItemByCodes(List<String> materialCodeList) {
        if (CollectionUtils.isNotEmpty(materialCodeList)) {
            return this.baseMapper.queryMaterialItemByCodes(materialCodeList);
        }
        return null;
    }

    @Override
    public PageInfo<MaterialItem> listPage(MaterialItem materialItem) {
        PageUtil.startPage(materialItem.getPageNum(), materialItem.getPageSize());
        QueryWrapper<MaterialItem> wrapper = new QueryWrapper<>();

        // ????????????????????????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String userType = loginAppUser.getUserType();
        if (UserType.VENDOR.name().equals(userType)) {
            List<String> stutus = new ArrayList<>();
            stutus.add(CeeaMaterialStatus.NOTIFIED.getCode());
            stutus.add(CeeaMaterialStatus.MAINTAINED.getCode());

            wrapper.eq("CEEA_SUPPLIER_CODE", loginAppUser.getCompanyCode());
            wrapper.in("CEEA_MATERIAL_STATUS", stutus); // ?????????????????????
        } else {
            if (StringUtils.isNoneBlank(materialItem.getCeeaMaterialStatus())) {
                wrapper.eq("CEEA_MATERIAL_STATUS", materialItem.getCeeaMaterialStatus());
            }
        }

        if (null != materialItem.getMaterialId()) {
            wrapper.eq("MATERIAL_ID", materialItem.getMaterialId());
        }
        if (StringUtils.isNoneBlank(materialItem.getMaterialCode())) {
            wrapper.like("MATERIAL_CODE", materialItem.getMaterialCode());
        }
        if (StringUtils.isNoneBlank(materialItem.getMaterialName())) {
            wrapper.like("MATERIAL_NAME", materialItem.getMaterialName());
        }
        if (StringUtils.isNoneBlank(materialItem.getStatus())) {
            wrapper.eq("STATUS", materialItem.getStatus());
        }
        if (StringUtils.isNoneBlank(materialItem.getCategoryName())) {
            wrapper.like("CATEGORY_NAME", materialItem.getCategoryName());
        }
        if (StringUtils.isNoneBlank(materialItem.getCeeaSupplierName())) {
            wrapper.eq("CEEA_SUPPLIER_NAME", materialItem.getCeeaSupplierName());
        }
        if ("Y".equals(materialItem.getCeeaIfCatalogMaterial())) {
            wrapper.eq("CEEA_IF_CATALOG_MATERIAL", materialItem.getCeeaIfCatalogMaterial());
        }
        if ("N".equals(materialItem.getCeeaIfCatalogMaterial())) {
            wrapper.notIn("CEEA_IF_CATALOG_MATERIAL", "Y").or().isNull("CEEA_IF_CATALOG_MATERIAL");
        }
        if (StringUtils.isNoneBlank(materialItem.getCeeaContractNo())) {
            wrapper.eq("CEEA_CONTRACT_NO", materialItem.getCeeaContractNo());
        }

        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return new PageInfo<MaterialItem>(iMaterialItemService.list(wrapper));
    }

    @Override
    public PageInfo<MaterialItem> listPageMaterialItemChart(MaterialItem materialItem) {
        PageUtil.startPage(materialItem.getPageNum(), materialItem.getPageSize());
        //????????????????????????????????????
        QueryWrapper<MaterialItem> wrapper = new QueryWrapper<>();

        // ????????????????????????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String userType = loginAppUser.getUserType();
        if (UserType.VENDOR.name().equals(userType)) {
            List<String> stutus = new ArrayList<>();
            stutus.add(CeeaMaterialStatus.NOTIFIED.getCode());
            stutus.add(CeeaMaterialStatus.MAINTAINED.getCode());

            wrapper.eq("CEEA_SUPPLIER_CODE", loginAppUser.getCompanyCode());
            wrapper.in("CEEA_MATERIAL_STATUS", stutus); // ?????????????????????
        } else {
            if (StringUtils.isNoneBlank(materialItem.getCeeaMaterialStatus())) {
                wrapper.eq("CEEA_MATERIAL_STATUS", materialItem.getCeeaMaterialStatus());
            }
        }
        if (null != materialItem.getMaterialId()) {
            wrapper.eq("MATERIAL_ID", materialItem.getMaterialId());
        }
        if (StringUtils.isNoneBlank(materialItem.getMaterialCode())) {
            wrapper.like("MATERIAL_CODE", materialItem.getMaterialCode());
        }
        if (StringUtils.isNoneBlank(materialItem.getMaterialName())) {
            wrapper.like("MATERIAL_NAME", materialItem.getMaterialName());
        }
        if (StringUtils.isNoneBlank(materialItem.getStatus())) {
            wrapper.eq("STATUS", materialItem.getStatus());
        }
        if (StringUtils.isNoneBlank(materialItem.getCategoryName())) {
            wrapper.like("CATEGORY_NAME", materialItem.getCategoryName());
        }
        if (StringUtils.isNoneBlank(materialItem.getCeeaSupplierName())) {
            wrapper.eq("CEEA_SUPPLIER_NAME", materialItem.getCeeaSupplierName());
        }
        if ("Y".equals(materialItem.getCeeaIfCatalogMaterial())) {
            wrapper.eq("CEEA_IF_CATALOG_MATERIAL", materialItem.getCeeaIfCatalogMaterial());
        }
        if ("N".equals(materialItem.getCeeaIfCatalogMaterial())) {
            wrapper.notIn("CEEA_IF_CATALOG_MATERIAL", "Y").or().isNull("CEEA_IF_CATALOG_MATERIAL");
        }
        if (StringUtils.isNoneBlank(materialItem.getCeeaContractNo())) {
            wrapper.eq("CEEA_CONTRACT_NO", materialItem.getCeeaContractNo());
        }
        wrapper.orderByDesc("LAST_UPDATE_DATE");

        List<MaterialItem> result = iMaterialItemService.list(wrapper);
        List<Long> materialItemId = result.stream().map(item -> item.getMaterialId()).collect(Collectors.toList());
        List<MaterialOrg> materialOrgList = getMaterialOrgList(materialItemId);
        Map<Long,StringBuffer> map = getMaterialOrgMap(materialOrgList);
        for(MaterialItem m : result){
            if(Objects.nonNull(map.get(m.getMaterialId()))){
                m.setOrgNames(map.get(m.getMaterialId()).toString());
            }
        }

        return new PageInfo<MaterialItem>(result);
    }



    /**
     * ???????????????????????????????????????
     * @param materialItemIds
     * @return
     */
    private List<MaterialOrg> getMaterialOrgList(List<Long> materialItemIds){
        if(CollectionUtils.isEmpty(materialItemIds)){
            return Collections.EMPTY_LIST;
        }
        QueryWrapper<MaterialOrg> wrapper = new QueryWrapper<>();
        wrapper.in("MATERIAL_ID",materialItemIds);
        wrapper.in("ITEM_STATUS","Y");
        List<MaterialOrg> materialOrgList = iMaterialOrgService.list(wrapper);
        return materialOrgList;
    }

    /**
     * ???list?????????map
     * @param materialOrgList
     * @return
     */
    private Map<Long,StringBuffer> getMaterialOrgMap(List<MaterialOrg> materialOrgList){
        Map<Long,StringBuffer> result = new HashMap<>();
        for(MaterialOrg materialOrg : materialOrgList){
            if(Objects.nonNull(result.get(materialOrg.getMaterialId()))){
                result.get(materialOrg.getMaterialId()).append(materialOrg.getOrgName() + ",");
            }else{
                result.put(materialOrg.getMaterialId(),new StringBuffer(materialOrg.getOrgName() + ","));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void saveOrUpdateMBatch(List<MaterialItem> materialItems) {
        if (!CollectionUtils.isEmpty(materialItems)) {
            for (MaterialItem materialItem : materialItems) {
                /**
                 * 1???	???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                 * 2???	?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                 * ????????????????????????????????????????????????????????????????????????????????????
                 */
                iCommonsService.checkMaterialItem(materialItem);

                //????????????code????????????
                QueryWrapper<MaterialItem> wrapper = new QueryWrapper<>();
                wrapper.eq(StringUtils.isNotBlank(materialItem.getMaterialCode()),
                        "MATERIAL_CODE", materialItem.getMaterialCode());
                wrapper.ne(materialItem.getMaterialId() != null,
                        "MATERIAL_ID", materialItem.getMaterialId());
                List<MaterialItem> existslist = this.list(wrapper);
                if (!CollectionUtils.isEmpty(existslist)) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????!"));
                }
                if (materialItem.getMaterialId() != null) {
                    materialItem.setLastUpdateDate(new Date());
                } else {
                    Long id = IdGenrator.generate();
                    materialItem.setMaterialId(id);
                    materialItem.setCreationDate(new Date());
                }
            }
            this.saveOrUpdateBatch(materialItems);
        }
    }

    @Override
    @Transactional
    public void saveOrUpdateM(MaterialItem materialItem) {
        //????????????code????????????
        QueryWrapper<MaterialItem> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(materialItem.getMaterialCode()),
                "MATERIAL_CODE", materialItem.getMaterialCode());
        wrapper.ne(materialItem.getMaterialId() != null,
                "MATERIAL_ID", materialItem.getMaterialId());
        List<MaterialItem> existslist = this.list(wrapper);
        if (!CollectionUtils.isEmpty(existslist)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????!"));
        }

        Long id = null;
        if (materialItem.getMaterialId() != null) {
            materialItem.setLastUpdateDate(new Date());
        } else {
            id = IdGenrator.generate();
            materialItem.setMaterialId(id);
            materialItem.setCreationDate(new Date());
        }

        List<MaterialOrg> materialOrgList = materialItem.getMaterialOrgList();
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        materialItem.setLastUpdatedBy((null != user ? user.getUsername() : ""));
        boolean isSaveOrUpdate = this.saveOrUpdate(materialItem);

        /**????????????????????????????????????????????????*/
        if (isSaveOrUpdate && CollectionUtils.isNotEmpty(materialOrgList)) {
            for (MaterialOrg materialOrg : materialOrgList) {
                if (null != materialOrg && null == materialOrg.getMaterialOrgId()) {
                    materialOrg.setMaterialOrgId(IdGenrator.generate());
                    materialOrg.setMaterialId(materialItem.getMaterialId());
                }
            }
            iMaterialOrgService.saveOrUpdateBatch(materialOrgList);
        }

    }

    @Override
    public List<MaterialItem> listMaterialByCodeBatch(List<String> materialCodeList) {
        QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("MATERIAL_CODE", materialCodeList);
        return this.list(queryWrapper);
    }

    @Override
    public List<MaterialItem> listMaterialByIdBatch(List<Long> materialIds) {
        QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("MATERIAL_ID", materialIds);
        return this.list(queryWrapper);
    }

    @Override
    public PageInfo<MaterialQueryDTO> listPageByParam(MaterialQueryDTO materialQueryDTO) {
        PageUtil.startPage(materialQueryDTO.getPageNum(), materialQueryDTO.getPageSize());
        List<MaterialQueryDTO> materialQueryDTOS = this.baseMapper.listPageByParam(materialQueryDTO);
        return new PageInfo<>(materialQueryDTOS);
    }

    /**
     * ??????????????????-????????????????????????
     * ????????????????????????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????
     * ???????????????????????????????????????
     * ?????????????????????0?????????????????????????????????
     * <p>
     * ?????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????
     * ???????????????????????????????????????????????????/?????????????????????????????????????????????????????????????????????????????????
     * <p>
     * ?????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????
     * ???????????????????????????????????????????????????/?????????????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    @Override
    public PageInfo<MaterialItemVo> listForOrder(MaterialQueryDTO materialQueryDTO) {
        Assert.notNull(materialQueryDTO.getVendorId(), LocaleHandler.getLocaleMsg("?????????id??????"));
        Assert.hasText(materialQueryDTO.getPurchaseType(), LocaleHandler.getLocaleMsg("????????????????????????"));
        Assert.notNull(materialQueryDTO.getCeeaOrgId(), LocaleHandler.getLocaleMsg("????????????id????????????"));
        Assert.notNull(materialQueryDTO.getOrganizationId(), LocaleHandler.getLocaleMsg("????????????id????????????"));

        List<PurchaseCategory> purchaseCategories = materialQueryDTO.getPurchaseCategories();
        String maxLevel = getMaxLevel();

        /*???????????????????????????*/
        ArrayList<PurchaseCategory> categories = new ArrayList<>();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategories)) {
            for (PurchaseCategory purchaseCategory : purchaseCategories) {
                Long categoryId = purchaseCategory.getCategoryId();
                QueryWrapper<PurchaseCategory> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("LEVEL", maxLevel);
                queryWrapper.like("STRUCT", categoryId);
                List<PurchaseCategory> list = iPurchaseCategoryService.list(queryWrapper);
                categories.addAll(list);
            }
        }
        List<MaterialItemVo> materialItemVoList = new ArrayList<>();
        /*????????????*/
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(categories)) {
            List<Long> longs = new ArrayList<>();
            categories.forEach(item -> longs.add(item.getCategoryId()));
            log.info(JsonUtil.arrayToJsonStr(longs));
            materialItemVoList = this.baseMapper.listByParam(materialQueryDTO, longs);
        }

        /*????????????????????????*/
        List<PriceLibrary> priceLibraryAll = inqClient.listAllEffective(new PriceLibrary().setVendorId(materialQueryDTO.getVendorId()).setCeeaOrganizationId(materialQueryDTO.getOrganizationId()));
        /*???????????????????????????????????????*/
        List<OrgCategory> orgCategoryList = supplierClient.getOrgCategoryByOrgCategory(new OrgCategory().setOrgId(materialQueryDTO.getOrgId()).setCompanyId(materialQueryDTO.getVendorId()));

        List<Long> orgIds = new ArrayList<>() ;
        orgIds.add(materialQueryDTO.getOrgId());
        /*???????????????????????????*/
        List<ContractVo> contractVoList = contractClient.listAllEffectiveCM(new ContractItemDto().setMaterialIds(orgIds));
        //?????????????????????
        List<Long> controlHeadIds = contractVoList.stream().map(c->c.getContractHeadId()).collect(Collectors.toList());
        List<ContractPartner> contractPartnerList = contractClient.listAllEffectiveCP(controlHeadIds);


        List<MaterialItemVo> result = new ArrayList<>();
        for (int i = 0; i < materialItemVoList.size(); i++) {
            MaterialItemVo item = materialItemVoList.get(i);

            /*?????????????????????????????????????????? - ???????????????????????????????????????*/
            OrgCategory orgCategoryItem = null;
            for (OrgCategory orgCategory : orgCategoryList) {
                if (null != orgCategory
                        && null != item.getOrgId() && Objects.equals(orgCategory.getOrgId(), item.getOrgId())
                        && Objects.equals(orgCategory.getCompanyId(), materialQueryDTO.getVendorId())
                        && null != item.getCategoryId() && Objects.equals(orgCategory.getCategoryId(), item.getCategoryId())
                        && null != orgCategory.getServiceStatus()
                        && (orgCategory.getServiceStatus().equals(CategoryStatus.VERIFY.name()) || orgCategory.getServiceStatus().equals(CategoryStatus.ONE_TIME.name()) || orgCategory.getServiceStatus().equals(CategoryStatus.GREEN.name()) || orgCategory.getServiceStatus().equals(CategoryStatus.YELLOW.name()))
                ) {
                    orgCategoryItem = orgCategory;
                    break;
                }
            }
            if (orgCategoryItem == null) {
                log.info("?????????" + JsonUtil.entityToJsonStr(item) + " ??????????????????????????????");
                continue;
            }
            log.info("materialCode = " + item.getMaterialCode() + "  materialName = " + item.getMaterialName() + "  vendorId = " + materialQueryDTO.getVendorId() + "????????????????????????????????????" + JsonUtil.entityToJsonStr(orgCategoryItem));

            if (materialQueryDTO.getPurchaseType().equals(OrderTypeEnum.ZERO_PRICE.getValue()) || OrderTypeEnum.CONVENIENT.getValue().equals(materialQueryDTO.getPurchaseType())) {
                item.setNoTaxPrice(BigDecimal.ZERO);
                item.setTaxPrice(BigDecimal.ZERO);
                /*item.setTaxCode("IN 11");
                item.setTaxRate(new BigDecimal(11.00));
                item.setCurrencyId(7007437216088064L);
                item.setCurrencyCode("CNY");
                item.setCurrencyName("????????????");*/
                result.add(item);
            } else if (materialQueryDTO.getPurchaseType().equals(OrderTypeEnum.URGENT.getValue()) ||
                    materialQueryDTO.getPurchaseType().equals(OrderTypeEnum.DEVELOP.getValue())) {
                /*???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????*/
                PriceLibrary priceEntity = null;
                for (PriceLibrary priceLibrary : priceLibraryAll) {
                    if (null != priceLibrary && StringUtils.isNotBlank(item.getMaterialName()) && item.getMaterialName().equals(priceLibrary.getItemDesc()) &&
                            StringUtils.isNotBlank(item.getMaterialCode()) && item.getMaterialCode().equals(priceLibrary.getItemCode()) &&
                            item.getOrgId() != null && !ObjectUtils.notEqual(item.getOrgId(), priceLibrary.getCeeaOrgId()) &&
                            Objects.equals(materialQueryDTO.getVendorId(), priceLibrary.getVendorId()) &&
                            item.getOrganizationId() != null && Objects.equals(item.getOrganizationId(), priceLibrary.getCeeaOrganizationId()) &&
                            StringUtils.isNotBlank(priceLibrary.getPriceType()) && "STANDARD".equals(priceLibrary.getPriceType())
                    ) {
                        /*??????????????????????????????????????????????????????????????????*/
                        if (priceLibrary.getNotaxPrice() == null ||
                                priceLibrary.getTaxPrice() == null ||
                                StringUtils.isBlank(priceLibrary.getTaxKey()) ||
                                StringUtils.isBlank(priceLibrary.getTaxRate()) ||
                                priceLibrary.getCurrencyId() == null ||
                                StringUtils.isBlank(priceLibrary.getCurrencyCode()) ||
                                StringUtils.isBlank(priceLibrary.getCurrencyName())
                        ) {
                            log.info("materialCode = " + item.getMaterialCode() + "  materialName:" + item.getMaterialName() + " ???????????????????????????:" + JsonUtil.entityToJsonStr(priceLibrary));
                        } else {
                            priceEntity = priceLibrary;
                            item.setNoTaxPrice(priceEntity.getNotaxPrice());
                            item.setTaxPrice(priceEntity.getTaxPrice());
                            item.setTaxCode(priceEntity.getTaxKey());
                            item.setTaxRate(new BigDecimal(priceEntity.getTaxRate()));
                            item.setCurrencyId(priceEntity.getCurrencyId());
                            item.setCurrencyCode(priceEntity.getCurrencyCode());
                            item.setCurrencyName(priceEntity.getCurrencyName());
                            /*?????????????????? ????????????id???????????????id????????????id????????????????????????*/
//                            for (ContractVo contractVo : contractVoList) {
//                                if (null != contractVo && contractVo.getBuId() != null && !ObjectUtils.notEqual(contractVo.getBuId(), item.getOrgId())
//                                        && contractVo.getInvId() != null && Objects.equals(contractVo.getInvId(), item.getOrganizationId())
//                                        && contractVo.getHeadVendorId() != null && Objects.equals(contractVo.getHeadVendorId(), priceEntity.getVendorId())
//                                        && StringUtils.isNotBlank(contractVo.getIsFrameworkAgreement()) && contractVo.getIsFrameworkAgreement().equals("Y")
//                                ) {
//                                    /*???????????????????????????????????????????????????????????????*/
//                                    if (StringUtils.isNotBlank(contractVo.getContractCode())) {
//                                        item.setContractCode(contractVo.getContractCode());
//                                        break;
//                                    } else {
//                                        log.info("materialCode = " + item.getMaterialCode() + "  materialName:" + item.getMaterialName() + "?????????????????????????????????????????????" + JsonUtil.entityToJsonStr(contractVo));
//                                    }
//                                }
//                            }


                            if(!StringUtil.isEmpty(priceLibrary.getContractCode())){
                                item.setContractCode(priceLibrary.getContractCode());
                            }else{
                                //???????????????????????? ??????2020???11???10???
                                for (ContractVo contractVo : contractVoList) {
                                    boolean hitContractFlag =
//                                          null != contractVo &&
//                                          contractVo.getBuId() != null &&
//                                          !ObjectUtils.notEqual(contractVo.getBuId(), item.getOrgId())&&
                                            contractVo.getHeadVendorId() != null &&
//                                          contractVo.getInvId() != null &&
//                                          Objects.equals(contractVo.getInvId(), item.getOrganizationId()) &&
                                                    Objects.equals(contractVo.getHeadVendorId(), priceLibrary.getVendorId()) &&
                                                    StringUtils.isNotBlank(contractVo.getIsFrameworkAgreement()) &&
                                                    StringUtils.isNotBlank(contractVo.getContractCode()) &&
                                                    contractVo.getIsFrameworkAgreement().equals("Y");
                                    log.info("materialCode = " + item.getMaterialCode() + "  materialName:" + item.getMaterialName() + "?????????????????????????????????????????????" + JsonUtil.entityToJsonStr(contractVo));

                                    boolean findContractNum = false;
                                    for(ContractPartner contractPartner : contractPartnerList){
                                        if(!contractPartner.getContractHeadId().equals(contractVo.getContractHeadId())) {
                                            continue;
                                        }
                                        if(findContractNum){
                                            break;
                                        }

                                        Long contractPartnerOUId = contractPartner.getOuId();
                                        //todo
                                        if(hitContractFlag && Objects.equals(contractVo.getBuId(), contractPartnerOUId)){
                                            /*???????????????????????????????????????????????????????????????*/
                                            item.setContractCode(contractVo.getContractCode());
                                            List<ContractVo> list = item.getContractVoList();
                                            if(null == list || list.isEmpty()){
                                                list = new ArrayList<>();
                                            }
                                            list.add(contractVo);
                                            item.setContractVoList(list);
                                            findContractNum = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                /*????????????????????????????????????????????????*/
                List<ContractVo> contractVos = new ArrayList<>();
                if (priceEntity == null) {
                    log.info("orgId = " + materialQueryDTO.getOrgId() + "  materialCode:" + item.getMaterialCode() + "  materialName:" + item.getMaterialName() + " ???????????????");
                    /*??????????????????????????? ????????? ????????????????????????????????????????????????????????????????????? ??????????????????*/
                    for (ContractVo contractVo : contractVoList) {
                        if (null != contractVo && contractVo.getBuId() != null && !ObjectUtils.notEqual(contractVo.getBuId(), item.getOrgId())
                                && contractVo.getMaterialCode() != null && contractVo.getMaterialCode().equals(item.getMaterialCode())
                                && contractVo.getMaterialName() != null && contractVo.getMaterialName().equals(item.getMaterialName())
                                && Objects.equals(contractVo.getHeadVendorId(), materialQueryDTO.getVendorId())
                                && contractVo.getInvId() != null && Objects.equals(contractVo.getInvId(), item.getOrganizationId())
                            /*&& StringUtils.isNotBlank(contractVo.getIsFrameworkAgreement()) && contractVo.getIsFrameworkAgreement().equals("N")*/
                        ) {
                            /*???????????????????????????????????????????????????????????????*/
                            if (contractVo.getTaxedPrice() == null ||
                                    StringUtils.isBlank(contractVo.getTaxKey()) ||
                                    contractVo.getTaxRate() == null ||
                                    contractVo.getCurrencyId() == null ||
                                    StringUtils.isBlank(contractVo.getCurrencyCode()) ||
                                    StringUtils.isBlank(contractVo.getCurrencyName())
                            ) {
                                log.info("orgId = " + materialQueryDTO.getOrgId() + "  materialCode:" + item.getMaterialCode() + "  materialName:" + item.getMaterialName() + " ????????????????????????:" + JsonUtil.entityToJsonStr(contractVo));
                            } else {
                                /*??????????????????*/
                                BigDecimal taxRate = contractVo.getTaxRate().divide(new BigDecimal(100), 8, BigDecimal.ROUND_HALF_UP);
                                BigDecimal untaxedPrice = contractVo.getTaxedPrice().divide((new BigDecimal(1).add(taxRate)), 2, BigDecimal.ROUND_HALF_UP);
                                contractVo.setUntaxedPrice(untaxedPrice);

                                contractVos.add(contractVo);
                            }
                        }
                    }
                    if (CollectionUtils.isNotEmpty(contractVos)) {
                        ContractVo contractVo = contractVos.get(0);
                        item.setNoTaxPrice(contractVo.getUntaxedPrice());
                        item.setTaxPrice(contractVo.getTaxedPrice());
                        item.setTaxCode(contractVo.getTaxKey());
                        item.setTaxRate(contractVo.getTaxRate());
                        item.setCurrencyId(contractVo.getCurrencyId());
                        item.setCurrencyCode(contractVo.getCurrencyCode());
                        item.setCurrencyName(contractVo.getCurrencyName());
                        item.setContractCode(contractVo.getContractCode());
                        item.setContractVoList(contractVos);
                    } else {
                        log.info("orgId = " + materialQueryDTO.getOrgId() + "  materialCode:" + item.getMaterialCode() + "  materialName:" + item.getMaterialName() + " ?????????????????????");
                    }
                }
                result.add(item);

            } else {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????????????????"));
            }
        }

        PageInfo<MaterialItemVo> pageInfo = PageUtil.pagingByFullData(materialQueryDTO.getPageNum(), materialQueryDTO.getPageSize(), result);

        /*??????????????????*/
        List<Long> materialIds = pageInfo.getList().stream().map(item -> item.getMaterialId()).collect(Collectors.toList());
        ;
        List<MaterialMaxCategoryVO> materialMaxCategoryVOList = baseClient.queryCategoryMaxCodeByMaterialIds(materialIds);
        Map<Long, MaterialMaxCategoryVO> map = materialMaxCategoryVOList.stream().collect(Collectors.toMap(e -> e.getMaterialId(), e -> e));
        for (int i = 0; i < pageInfo.getList().size(); i++) {
            MaterialItemVo materialItemVo = pageInfo.getList().get(i);
            MaterialMaxCategoryVO materialMaxCategoryVO = map.get(materialItemVo.getMaterialId());
            if (materialMaxCategoryVO != null) {
                materialItemVo.setBigCategoryId(materialMaxCategoryVO.getCategoryId());
                materialItemVo.setBigCategoryCode(materialMaxCategoryVO.getCategoryCode());
            }
        }

        /*??????????????????*/
        for (int i = 0; i < pageInfo.getList().size(); i++) {
            MaterialItemVo materialItemVo = pageInfo.getList().get(i);
            List<PurchaseTax> purchaseTaxList = baseClient.queryTaxByItemForOrder(materialItemVo.getMaterialId());
            materialItemVo.setPurchaseTaxList(purchaseTaxList);
        }

        /*??????????????????*/
        for(int i=0;i<pageInfo.getList().size();i++){
            MaterialItemVo materialItemVo = pageInfo.getList().get(i);
            materialItemVo.setCeeaPriceSourceType(PriceSourceTypeEnum.MANUAL.getValue());
        }

        return pageInfo;
    }

    public PageInfo<MaterialItemVo> listForOrderNew(MaterialQueryDTO materialQueryDTO){
        //????????????
        Assert.notNull(materialQueryDTO.getVendorId(), LocaleHandler.getLocaleMsg("?????????id??????"));
        Assert.hasText(materialQueryDTO.getPurchaseType(), LocaleHandler.getLocaleMsg("????????????????????????"));
        Assert.notNull(materialQueryDTO.getCeeaOrgId(), LocaleHandler.getLocaleMsg("????????????id????????????"));
        Assert.notNull(materialQueryDTO.getOrganizationId(), LocaleHandler.getLocaleMsg("????????????id????????????"));


        //??????????????????????????? MaterialList OrgCategoryList,PriceLibraryAll,
        return null;
    }


    @Override
    public PageInfo<MaterialQueryDTO> listMaterialByPurchaseCategory(MaterialQueryDTO materialQueryDTO) {
        PageUtil.startPage(materialQueryDTO.getPageNum(), materialQueryDTO.getPageSize());
        settingQueryCategoryByRole(materialQueryDTO);
        List<MaterialQueryDTO> materialQueryDTOS = this.baseMapper.listPageByWrapper(materialQueryDTO);
        return new PageInfo<>(materialQueryDTOS);
    }

    /**
     * ??????????????????????????????
     */
    public String getMaxLevel() {
        // ??????????????????????????????
        List<ServiceConfig> list = iBaseServiceConfigService.list();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(list) && StringUtil.notEmpty(list.get(0).getServiceLevel())) {
            return list.get(0).getServiceLevel();
        } else {
            throw new BaseException("??????: ????????????->??????????????????->????????????????????? , ??????????????????????????????");
        }
    }

    @Override
    public PageInfo<MaterialItemVo> ceeaPurchaseMaterialListPage(MaterialQueryDTO materialQueryDTO) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (!StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType()) && !StringUtils.equals(UserType.BUYER.name(), loginAppUser.getUserType())) {
            Assert.isTrue(false, "?????????????????????");
        }
        if (StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())) {
            materialQueryDTO.setVendorId(loginAppUser.getCompanyId());
        } else {
            materialQueryDTO.setUserId(loginAppUser.getUserId());
        }
        PageHelper.startPage(materialQueryDTO.getPageNum(), materialQueryDTO.getPageSize());
        QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper();
        queryWrapper.and(wrapper -> wrapper
                .like(StringUtils.isNoneBlank(materialQueryDTO.getMaterialKey()), "MATERIAL_NAME", materialQueryDTO.getMaterialKey())
                .or()
                .like(StringUtils.isNoneBlank(materialQueryDTO.getMaterialKey()), "MATERIAL_CODE", materialQueryDTO.getMaterialKey())
        );
        queryWrapper.eq(StringUtils.isNoneBlank(materialQueryDTO.getMaterialStatus()), "", materialQueryDTO.getMaterialStatus());
        queryWrapper.and(wrapper -> wrapper
                .like(StringUtils.isNoneBlank(materialQueryDTO.getSupplierKey()), "CEEA_SUPPLIER_CODE", materialQueryDTO.getSupplierKey())
                .or()
                .like(StringUtils.isNoneBlank(materialQueryDTO.getSupplierKey()), "CEEA_SUPPLIER_NAME", materialQueryDTO.getSupplierKey())
        );
        queryWrapper.eq(StringUtils.isNoneBlank(materialQueryDTO.getIfCatalogMaterial()), "CEEA_IF_CATALOG_MATERIAL", materialQueryDTO.getIfCatalogMaterial());
        queryWrapper.eq(materialQueryDTO.getCategoryId() != null, "CATEGORY_ID", materialQueryDTO.getCategoryId());
        queryWrapper.like(StringUtils.isNoneBlank(materialQueryDTO.getContractNo()), "CEEA_CONTRACT_NO", materialQueryDTO.getContractNo());
        List<MaterialItem> materialItemList = this.list(queryWrapper);
        List<MaterialItemVo> result = new ArrayList<>();
        materialItemList.forEach(item -> {
            MaterialItemVo materialItemVo = new MaterialItemVo();
            BeanUtils.copyProperties(item, materialItemVo);
            List<PriceLibrary> priceLibraryList = inqClient.listEffectivePrice(
                    new PriceLibrary().setCeeaOrgId(materialQueryDTO.getOrgId())
                            .setCeeaOrganizationId(materialQueryDTO.getOrganizationId())
                            .setItemCode(item.getMaterialCode())
                            .setItemDesc(item.getMaterialName())
            );
            if (CollectionUtils.isEmpty(priceLibraryList)) {
                Assert.notNull(null, LocaleHandler.getLocaleMsg("???????????????[" + item.getMaterialName() + "] ????????????[???" + item.getMaterialCode() + "]???????????????"));
            }
            LocalDate now = LocalDate.now();
            LocalDate priceLibraryLocalDate = DateUtil.dateToLocalDate(priceLibraryList.get(0).getExpirationDate()).plusDays(1);
            materialItemVo.setYear(now.until(priceLibraryLocalDate).getYears())
                    .setMonth(now.until(priceLibraryLocalDate).getMonths())
                    .setDay(now.until(priceLibraryLocalDate).getDays());
            result.add(materialItemVo);
        });
        return new PageInfo<MaterialItemVo>(result);
    }

    @Override
    public MaterialItem findMaterialItemById(Long materialItemId) {
        MaterialItem materialItem = getBaseMapper().selectById(materialItemId);
        if (null != materialItem) {
            List<MaterialOrg> materialOrgList = iMaterialOrgService.list(new QueryWrapper<>(new MaterialOrg().setMaterialId(materialItem.getMaterialId())));
            materialItem.setMaterialOrgList(materialOrgList);
        }
        return materialItem;
    }

    /**
     * @Description ????????????
     * @Param [materialItemId]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.25 10:04
     */
    @Override
    public MaterialItem getMaterialItemById(Long materialItemId) {
        MaterialItem mi = this.getById(materialItemId);

        // ???????????????
        if (StringUtils.isBlank(mi.getCeeaNames()) || StringUtils.isBlank(mi.getCeeaSecondNames())) {
            if (mi.getCeeaSupplierId() != null) {
				/*List<Long> vendorIds = new ArrayList<>();
				vendorIds.add(mi.getCeeaSupplierId());*/
                List<ContactInfo> contactInfos = supplierClient.listContactInfoByCompanyId(mi.getCeeaSupplierId()); // ?????????
                if (!CollectionUtils.isEmpty(contactInfos)) {
                    StringBuilder firstCont = new StringBuilder();
                    for (ContactInfo ci : contactInfos) {
                        if (StringUtils.isBlank(mi.getCeeaNames())) {
                            if ((ci.getContactName() + "").equals(mi.getCeeaSecondNames())) { // ????????????????????????
                                continue;
                            }
                            mi.setCeeaNames(ci.getContactName());
                            mi.setCeeaTelephones(ci.getMobileNumber());
                            mi.setCeeaEmails(ci.getEmail());
                            firstCont.append(ci.getContactName() + "???" + ci.getMobileNumber() + "???" + ci.getEmail());
                            continue; // ??????????????????????????????????????????????????????????????????
                        }
                        if (StringUtils.isBlank(mi.getCeeaSecondNames())) {
                            if (firstCont.indexOf(ci.getContactName() + "???" + ci.getMobileNumber() + "???" + ci.getEmail()) > -1) {
                                continue; //
                            }
                            mi.setCeeaSecondNames(ci.getContactName());
                            mi.setCeeaSecondTelephones(ci.getMobileNumber());
                            mi.setCeeaEmails(ci.getEmail());
                            break; // ?????????????????????
                        } else {
                            break; // ?????????????????????
                        }
                    }
                }
            }
        }
        return mi;
    }

    /**
     * @param materialItem
     * @return
     * @Description ????????????
     * @Param [materialItem]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.28 10:42
     */
    @Override
    @Transactional
    public void ceeaSaveOrUpdate(MaterialItemDto materialItem) {
        Assert.notNull(materialItem, "??????????????????");
      //kuangzm ????????????????????????????????????
        Assert.notNull(materialItem.getMaterialItem(), "????????????????????????");
        Assert.notNull(materialItem.getMaterialItem().getMaterialCode(), "????????????????????????");
        Assert.notNull(materialItem.getMaterialItem().getMaterialName(), "????????????????????????");
        Assert.notNull(materialItem.getMaterialItem().getCategoryId(), "??????????????????");
        Assert.notNull(materialItem.getMaterialItem().getUnit(), "??????????????????");

        MaterialItem mi = materialItem.getMaterialItem();
        
        //kuangzm ????????????????????????
        if (null != mi.getMaterialId()) {
        	this.updateById(mi);
        } else {
        	Long id = IdGenrator.generate();
        	mi.setMaterialId(id);
        	this.save(mi);
        }
        
        // ????????????
        List<Fileupload> matFiles = materialItem.getMatFiles();
        fileCenterClient.bindingFileupload(matFiles, mi.getMaterialId());

        // ????????????
        List<ItemImage> itemImages = materialItem.getItemImages();
        iItemImageService.remove(Wrappers.lambdaQuery(ItemImage.class).
                eq(ItemImage::getMaterialId,mi.getMaterialId()));
        if(CollectionUtils.isNotEmpty(itemImages)){
            itemImages.forEach(itemImage -> {
                itemImage.setMaterialId(mi.getMaterialId());
                itemImage.setItemImageId(IdGenrator.generate());
            });
            iItemImageService.saveBatch(itemImages);
        }
    }

    /**
     * @param id
     * @return
     * @Description ????????????
     * @Param [id]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.28 11:10
     */
    @Override
    public MaterialItemDto ceeaGet(Long id) {
        Assert.notNull(id,"????????????:??????id");
        MaterialItemDto mid = new MaterialItemDto();
        MaterialItem mi = getMaterialItemById(id);
        PageInfo<Fileupload> matFiles = fileCenterClient.listPage(new Fileupload().setBusinessId(id).setFileFunction("materialMaintenance"), "");

        mid.setMaterialItem(mi);
        if (matFiles != null) {
            mid.setMatFiles(matFiles.getList());
        }

        // ????????????
        List<ItemImage> itemImages = iItemImageService.list(Wrappers.lambdaQuery(ItemImage.class).
                eq(ItemImage::getMaterialId, id));
        mid.setItemImages(itemImages);


        return mid;
    }

    /**
     * @Description ????????????????????????
     * @Param [materialQueryDTO]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.18 10:27
     * ?????????????????????????????????
     * ????????????+???????????????????????????????????????????????? + ??????????????????????????? + ????????????
     * 2020-10-30???????????????????????????????????????????????????????????????
     * <p>
     * ????????????????????????????????????
     * ??????????????? + ????????????????????????????????????????????????????????????????????????????????????????????????
     * ???????????????????????? ????????????+????????????+????????????+???????????? ???????????????????????????+??????????????????
     * ?????????????????????????????????????????????????????????????????????
     * 2020-10-30???????????????????????????????????????????????????????????????
     */
    @Override
    public PageInfo<MaterialQueryDTO> ceeaListPurchaseCatalogPage(MaterialQueryDTO materialQueryDTO) {
        /*????????? ?????????????????????????????????????????????*/
        List<PurchaseCategory> bigCategories = new ArrayList<>();
        /*????????????????????????*/
        PurchaseCategory purchaseCategory1 = purchaseCategoryService.list(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(BigCategoryTypeEnum.SPARE_PARTS.getCode()))).get(0);
        /*?????????????????????*/
        PurchaseCategory purchaseCategory2 = purchaseCategoryService.list(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(BigCategoryTypeEnum.COMPREHENSIVE_MATERIALS.getCode()))).get(0);
        bigCategories.add(purchaseCategory1);
        bigCategories.add(purchaseCategory2);

        /*???????????????????????????*/
        String maxLevel = getMaxLevel();
        List<PurchaseCategory> categories = new ArrayList<>();
        for (PurchaseCategory purchaseCategory : bigCategories) {
            Long categoryId = purchaseCategory.getCategoryId();
            QueryWrapper<PurchaseCategory> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("LEVEL", maxLevel);
            queryWrapper.like("STRUCT", categoryId);
            List<PurchaseCategory> list = iPurchaseCategoryService.list(queryWrapper);
            categories.addAll(list);
        }
        List<Long> longs = categories.stream().map(item -> item.getCategoryId()).collect(Collectors.toList());
        materialQueryDTO.setSmallCategoryIds(longs);

        if (materialQueryDTO == null) return new PageInfo<>();
        PageUtil.startPage(materialQueryDTO.getPageNum(), materialQueryDTO.getPageSize());
        List<MaterialQueryDTO> list = null;
        if ("Y".equals(materialQueryDTO.getIfCatalogMaterial())) {
            /*???????????????*/
            list = baseMapper.ceeaListPurchaseCatalogPage2(materialQueryDTO);
        } else {
            /*?????????????????????*/
            list = baseMapper.ceeaListPurchaseCatalogPage(materialQueryDTO);
        }
        return new PageInfo<MaterialQueryDTO>(list);
    }

    /**
     * @Description ????????????????????????
     * @Param [materialQueryDTO]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.18 10:27
     * ?????????????????????????????????
     * ????????????+???????????????????????????????????????????????? + ??????????????????????????? + ????????????
     * 2020-10-30???????????????????????????????????????????????????????????????
     * <p>
     * ????????????????????????????????????
     * ??????????????? + ????????????????????????????????????????????????????????????????????????????????????????????????
     * ???????????????????????? ????????????+????????????+????????????+???????????? ???????????????????????????+??????????????????
     * ?????????????????????????????????????????????????????????????????????
     * 2020-10-30???????????????????????????????????????????????????????????????
     */
    @Override
    public PageInfo<MaterialQueryDTO> ceeaListPurchaseCatalogPageNew(MaterialQueryDTO materialQueryDTO) {
        Assert.notNull(materialQueryDTO.getCeeaOrgId(), "????????????????????????");
        Assert.notNull(materialQueryDTO.getCeeaOrganizationId(), "????????????????????????");
        // ????????????????????????ID
        List<String> categoryNameList = new ArrayList<>();
        if (StringUtil.isEmpty(materialQueryDTO.getCategoryName())) {
            categoryNameList.add(BigCategoryTypeEnum.SPARE_PARTS.getValue());
            categoryNameList.add(BigCategoryTypeEnum.COMPREHENSIVE_MATERIALS.getValue());
            List<PurchaseCategory> categoryList = purchaseCategoryService.list(new QueryWrapper<PurchaseCategory>().
                    in("CATEGORY_NAME", categoryNameList));
            materialQueryDTO.setCategoryId1(String.valueOf(categoryList.get(0).getCategoryId()));
            materialQueryDTO.setCategoryId2(String.valueOf(categoryList.get(1).getCategoryId()));
        } else {
            categoryNameList.add(materialQueryDTO.getCategoryName());
            List<PurchaseCategory> categoryList = purchaseCategoryService.list(new QueryWrapper<PurchaseCategory>().
                    in("CATEGORY_NAME", categoryNameList));
            materialQueryDTO.setCategoryId1(String.valueOf(categoryList.get(0).getCategoryId()));
        }


        settingQueryCategoryByRole(materialQueryDTO);
        // ???????????????
        String ifCatalogMaterial = materialQueryDTO.getIfCatalogMaterial();
        PageUtil.startPage(materialQueryDTO.getPageNum(), materialQueryDTO.getPageSize());
        List<MaterialQueryDTO> materialQueryDTOS = null;
        if (YesOrNo.YES.getValue().equals(ifCatalogMaterial)) {
            //?????????
            materialQueryDTOS = this.baseMapper.queryMaterialQueryDTOByCatalogingY(materialQueryDTO);

        } else {
            //????????????
            materialQueryDTOS = this.baseMapper.queryMaterialQueryDTOByCatalogingN(materialQueryDTO);
        }

        return new PageInfo<>(materialQueryDTOS);
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????it
     * @param materialQueryDTO
     */
    private void settingQueryCategoryByRole(MaterialQueryDTO materialQueryDTO){
        //???????????????????????????????????????it??????
        int showBit     =   0b00;
        int notShowIT   =   0b10;
        int showIT      =   0b01;
        List<Role> notShowITRoles = AppUserUtil.getLoginAppUser().getRolePermissions().stream().filter(
                role -> RoleEnum.LONGI_DG.getRoleCode().equals(role.getRoleCode()) ||
                        RoleEnum.LONGI_DD.getRoleCode().equals(role.getRoleCode())
        ).collect(Collectors.toList());
        if (!notShowITRoles.isEmpty()) {
            showBit = showBit | notShowIT;
        }
        //?????????-it????????????it??????
        List<Role> showITRoles = AppUserUtil.getLoginAppUser().getRolePermissions().stream()
                .filter(role -> RoleEnum.LONGI_IT.getRoleCode().equals(role.getRoleCode())).collect(Collectors.toList());
        if(!showITRoles.isEmpty()){
            showBit = showBit | showIT;
        }
        //it??????
        Map<String, PurchaseCategory> itCategoryData = baseClient.getCategoryByCodes(Arrays.asList(SpecialBusinessConstant.IT_CATEGORY));
        List<Long> categoryIds = new ArrayList<>();
        for(String categoryCode : itCategoryData.keySet()){
            PurchaseCategory category = itCategoryData.get(categoryCode);
            categoryIds.add(category.getCategoryId());
        }
        if(showBit == showIT){
            materialQueryDTO.setCategoryIds(categoryIds);
            materialQueryDTO.setCategoryFilter("showIT");
        }else if(showBit == notShowIT){
            materialQueryDTO.setCategoryIds(categoryIds);
            materialQueryDTO.setCategoryFilter("notShowIT");
        }
    }

    /**
     * @Description ???????????????????????????
     * @Param [materialId]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.18 14:22
     * <p>
     * 2020-10-30???????????????????????????
     * ??????????????????????????????????????????????????????????????????
     * ???????????????????????? ?????? ???????????????
     * ?????????????????? ????????????
     */
    @Override
    public String ceeaAddToShoppingCart(MaterialQueryDTO mqd) {
        Assert.notNull(mqd.getMaterialId(), "??????Id????????????");

        // ????????????
        MaterialQueryDTO materialQueryDTO = new MaterialQueryDTO();
        materialQueryDTO.setMaterialId(mqd.getMaterialId()); // ??????ID
        materialQueryDTO.setMaterialCode(mqd.getCategoryMaterialCode());
        materialQueryDTO.setMaterialName(mqd.getCategoryMaterialName());
        materialQueryDTO.setCeeaOrgId(mqd.getCeeaOrgId()); // ????????????
        materialQueryDTO.setCeeaOrganizationId(mqd.getCeeaOrganizationId()); // ????????????
        materialQueryDTO.setIfCatalogMaterial(mqd.getIfCatalogMaterial()); // ???????????????
        materialQueryDTO.setCategoryName(mqd.getCategoryName()); // ??????
        materialQueryDTO.setCeeaSupplierId(mqd.getCeeaSupplierId()); //?????????id

        // ????????????????????????ID
        List<String> categoryNameList = new ArrayList<>();
        if (StringUtil.isEmpty(materialQueryDTO.getCategoryName())) {
            categoryNameList.add(BigCategoryTypeEnum.SPARE_PARTS.getValue());
            categoryNameList.add(BigCategoryTypeEnum.COMPREHENSIVE_MATERIALS.getValue());
            List<PurchaseCategory> categoryList = purchaseCategoryService.list(new QueryWrapper<PurchaseCategory>().
                    in("CATEGORY_NAME", categoryNameList));
            materialQueryDTO.setCategoryId1(String.valueOf(categoryList.get(0).getCategoryId()));
            materialQueryDTO.setCategoryId2(String.valueOf(categoryList.get(1).getCategoryId()));
        } else {
            categoryNameList.add(materialQueryDTO.getCategoryName());
            List<PurchaseCategory> categoryList = purchaseCategoryService.list(new QueryWrapper<PurchaseCategory>().
                    in("CATEGORY_NAME", categoryNameList));
            materialQueryDTO.setCategoryId1(String.valueOf(categoryList.get(0).getCategoryId()));
        }
        // ???????????????
        String ifCatalogMaterial = materialQueryDTO.getIfCatalogMaterial();
        PageUtil.startPage(materialQueryDTO.getPageNum(), materialQueryDTO.getPageSize());
        List<MaterialQueryDTO> list = null;
        if (YesOrNo.YES.getValue().equals(ifCatalogMaterial)) {
            /**
             * ????????????
             */
            list = this.baseMapper.queryMaterialQueryDTOByCatalogingY1(materialQueryDTO);

        } else {
            /**
             * ????????????
             * 1. ????????????????????????????????????????????????????????????????????????????????????
             * iMaterialOrgService
             */
            list = this.baseMapper.queryMaterialQueryDTOByCatalogingN(materialQueryDTO);
        }


        if (CollectionUtils.isEmpty(list)) {
            return "?????????????????????????????????";
        }
        MaterialQueryDTO mat = list.get(0);
        if (mat.getCategoryId() == null || StringUtils.isBlank(mat.getCategoryName())) {
            Assert.notNull(null, "??????????????????????????????????????????");
        }
        if (StringUtils.isBlank(mat.getUnit()) || StringUtils.isBlank(mat.getUnitName())) {
            Assert.notNull(null, "??????????????????????????????????????????");
        }

        // ????????????????????????
        ShopCart shopCart = new ShopCart(); // ?????????
        shopCart.setMaterialCode(mat.getMaterialCode()); // ????????????
        shopCart.setMaterialName(mat.getMaterialName()); //????????????
        shopCart.setOrgId(mat.getOrgId()); // ????????????
        shopCart.setOrganizationId(mat.getOrganizationId()); // ????????????
        shopCart.setStatus(ShopCartStatus.DRAFT.getCode()); // ?????????
        shopCart.setCreatedBy(AppUserUtil.getUserName()); // ?????????
        if (!Objects.isNull(mat.getCeeaSupplierId())) {
            shopCart.setSupplierId(mat.getCeeaSupplierId()); // ?????????
        }

        /*todo ???????????????*/
        List<ShopCart> exists = pmClient.ceeaListByShopCart(shopCart);
        if (!CollectionUtils.isEmpty(exists)) {
            return "????????????????????????";
        }
        //????????????Code
        QueryWrapper<CategoryBusiness> wrapper =new QueryWrapper<CategoryBusiness>();
        wrapper.eq(mat.getCategoryId()!=null,"CATEGORY_ID",mat.getCategoryId());
        mat.setCategoryCode(iCategoryBusinessService.getOne(wrapper).getCategoryCode());
        // ???????????????
        pmClient.ceeaAddToShopCart(mat);
        return "??????????????????";
    }

    /**
     * @Description ?????????????????????????????????
     * @Param [materialQueryDTO]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.22 10:01
     */
    @Override
    public String ceeaUpdateSupplier(List<MaterialQueryDTO> materialQueryDTO) {
        if (CollectionUtils.isEmpty(materialQueryDTO)) {
            return "????????????????????????";
        }
        List<Long> ids = new ArrayList<>();
        for (MaterialQueryDTO mat : materialQueryDTO) {
            if (mat.getMaterialId() != null) {
                ids.add(mat.getMaterialId());
            }
        }
        if (ids.size() > 0) {
            List<MaterialItem> mats = this.listByIds(ids);
            for (MaterialItem mat : mats) {
                for (MaterialQueryDTO mi : materialQueryDTO) {
                    if (mat.getMaterialId().equals(mi.getMaterialId())) {
                        BeanCopyUtil.copyProperties(mat, mi);
                        break;
                    }
                }
            }
            this.saveOrUpdateBatch(mats);
        }
        return "????????????";
    }

    @Override
    public MaterialItem findMaterialItemByMaterialCode(String materialCode) {
        QueryWrapper queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MATERIAL_CODE", materialCode);
        MaterialItem materialItem = getOne(queryWrapper);
        return materialItem;
    }

    /**
     * @Description ???????????????
     * @Param [materialIds]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.22 11:46
     */
    @Override
    public String ceeaNotifyVendor(List<Long> materialIds) {
        StringBuilder msg = new StringBuilder();
        if (CollectionUtils.isEmpty(materialIds)) {
            QueryWrapper<MaterialItem> qw = new QueryWrapper();
            qw.isNotNull("CEEA_SUPPLIER_CODE");

            List<MaterialItem> mats = this.list(qw);
            if (!CollectionUtils.isEmpty(mats)) {
                for (MaterialItem mat : mats) {
                    mat.setCeeaMaterialStatus(CeeaMaterialStatus.NOTIFIED.getCode());
                }
                this.saveOrUpdateBatch(mats);
            }
        } else {
            List<MaterialItem> mats = this.listByIds(materialIds);
            for (MaterialItem mat : mats) {
                if (StringUtils.isNoneBlank(mat.getCeeaSupplierCode())) {
                    mat.setCeeaMaterialStatus(CeeaMaterialStatus.NOTIFIED.getCode());
                } else {
                    msg.append("????????????????????????????????????????????????????????????");
                    break;
                }
            }
            if (msg.length() < 1) {
                this.saveOrUpdateBatch(mats);
            }
        }
        return msg.toString();
    }

    /**
     * @Description ??????????????????
     * @Param [response]
     * @Author dengyl23@meicloud.com
     * @Date 2020.09.23 11:17
     */
    @Override
    public void importModelDownload(HttpServletResponse response) throws Exception {
        String fileName = "????????????????????????";
        ArrayList<MaterialItemModelDto> MaterialItemFormDtos = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        List<Integer> rows = Arrays.asList(0);
        List<Integer> columns = Arrays.asList(0, 1, 3, 5);
        TitleColorSheetWriteHandler titleColorSheetWriteHandler = new TitleColorSheetWriteHandler(rows, columns, IndexedColors.RED.index);
        EasyExcelUtil.writeExcelWithModel(outputStream, MaterialItemFormDtos, MaterialItemModelDto.class, fileName, titleColorSheetWriteHandler);
    }

    /**
     * @Description ??????????????????????????????
     * @Param [response errorMessage]
     * @Author dengyl23@meicloud.com
     * @Date 2020.09.23 11:17
     */
    @Override
    public void outputModelDownload(HttpServletResponse response, List<MaterialItemErrorDto> errorDtos) throws Exception {
        String fileName = "????????????????????????";
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        List<Integer> rows = Arrays.asList(0);
        List<Integer> columns = Arrays.asList(0, 1);
        TitleColorSheetWriteHandler titleColorSheetWriteHandler = new TitleColorSheetWriteHandler(rows, columns, IndexedColors.RED.index);
        EasyExcelUtil.writeExcelWithModel(outputStream, errorDtos, MaterialItemErrorDto.class, fileName, titleColorSheetWriteHandler);
    }

    /**
     * @Description ????????????
     * @Param [file]
     * @Author dengyl23@meicloud.com
     * @Date 2020.09.23 11:18
     */
    @Transactional
    @Override
    public void importExcel(MultipartFile file) throws Exception {
        try {
            //??????????????????
            String originalFilename = file.getOriginalFilename();
            if (!EasyExcelUtil.isExcel(originalFilename)) {
                throw new RuntimeException("??????????????????Excel??????");
            }
            InputStream inputStream = file.getInputStream();
            ArrayList<MaterialItem> materialItemArrayList = new ArrayList<>();
            List<Object> objects = EasyExcelUtil.readExcelWithModel(inputStream, MaterialItemModelDto.class);
            List<MaterialItem> updateList = new ArrayList<>();
            List<MaterialItem> addList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(objects)) {
                objects.forEach((object -> {
                    if (null != object) {
                        MaterialItemModelDto materialItemModelDto = (MaterialItemModelDto) object;
                        checkRequireParam(materialItemModelDto);
                        //????????????code????????????
                        QueryWrapper<MaterialItem> wrapper = new QueryWrapper<>();
                        wrapper.eq(StringUtils.isNotBlank(materialItemModelDto.getMaterialCode()),
                                "MATERIAL_CODE", materialItemModelDto.getMaterialCode());
                        wrapper.eq(materialItemModelDto.getMaterialName() != null,
                                "MATERIAL_NAME", materialItemModelDto.getMaterialName());
                        List<MaterialItem> existslist = this.list(wrapper);
                        if (CollectionUtils.isNotEmpty(existslist)) {
                            existslist.forEach(materialItem -> {
                                if (StringUtils.isNotBlank(materialItemModelDto.getCategoryName())) {
                                    materialItem.setCategoryName(materialItemModelDto.getCategoryName());
                                }
                                if (null != materialItemModelDto.getCeeaOrderQuantityMinimum()) {
                                    materialItem.setCeeaOrderQuantityMinimum(materialItemModelDto.getCeeaOrderQuantityMinimum());
                                }
                                if (StringUtils.isNotBlank(materialItemModelDto.getCeeaWeight())) {
                                    materialItem.setCeeaWeight(materialItemModelDto.getCeeaWeight());
                                }
                                if (StringUtils.isNotBlank(materialItemModelDto.getCeeaSize())) {
                                    materialItem.setCeeaSize(materialItemModelDto.getCeeaSize());
                                }
                                if (StringUtils.isNotBlank(materialItemModelDto.getCeeaBrand())) {
                                    materialItem.setCeeaBrand(materialItemModelDto.getCeeaBrand());
                                }
                                if (StringUtils.isNotBlank(materialItemModelDto.getCeeaColor())) {
                                    materialItem.setCeeaColor(materialItemModelDto.getCeeaColor());
                                }
                                if (StringUtils.isNotBlank(materialItemModelDto.getCeeaTexture())) {
                                    materialItem.setCeeaTexture(materialItemModelDto.getCeeaTexture());
                                }
                                if (StringUtils.isNotBlank(materialItemModelDto.getCeeaUsage())) {
                                    materialItem.setCeeaUsage(materialItemModelDto.getCeeaUsage());
                                }
                                materialItem.setCeeaDeliveryCycle(materialItemModelDto.getCeeaDeliveryCycle());
                                materialItem.setSpecification(materialItemModelDto.getSpecification());
                                updateList.add(materialItem);
                            });
                        } else {
                            MaterialItem materialItem = new MaterialItem();
                            BeanUtils.copyProperties(materialItemModelDto, materialItem);
                            Long id = IdGenrator.generate();
                            materialItem.setMaterialId(id);
                            materialItem.setCreationDate(new Date());
                            addList.add(materialItem);
                        }
                    }
                }));
            }
            this.updateBatchById(updateList);
            this.saveBatch(addList);
        } catch (Exception e) {
            log.info("import excel error:{}", e);
            throw new BaseException(ResultCode.IMPORT_EXCEPTIONS);
        }
    }

    @Override
    public MaterialItemDto checkImportExcel(MultipartFile file) throws Exception {
        try {
            //??????????????????
            String originalFilename = file.getOriginalFilename();
            if (!EasyExcelUtil.isExcel(originalFilename)) {
                throw new RuntimeException("??????????????????Excel??????");
            }
            InputStream inputStream = file.getInputStream();
            List<Object> objects = EasyExcelUtil.readExcelWithModel(inputStream, MaterialItemModelDto.class);
            List<MaterialItemErrorDto> errorDtos = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(objects)) {
                for (int i = 0; i < objects.size(); i++) {
                    if (null != objects.get(i)) {
                        MaterialItemModelDto materialItemModelDto = (MaterialItemModelDto) objects.get(i);
                        checkRequireParam(materialItemModelDto);
                        //????????????code????????????
                        QueryWrapper<MaterialItem> wrapper = new QueryWrapper<>();
                        wrapper.eq(StringUtils.isNotBlank(materialItemModelDto.getMaterialCode()),
                                "MATERIAL_CODE", materialItemModelDto.getMaterialCode());
                        wrapper.eq(materialItemModelDto.getMaterialName() != null,
                                "MATERIAL_NAME", materialItemModelDto.getMaterialName());
                        List<MaterialItem> existslist = this.list(wrapper);
                        if (CollectionUtils.isEmpty(existslist)) {
                            MaterialItemErrorDto errorDto = new MaterialItemErrorDto();
                            errorDto.setLineNumber(i + 1);
                            errorDto.setErrorMessage("???????????????");
                            errorDtos.add(errorDto);
                        }
                    }
                }
            }
            MaterialItemDto materialItemDto = new MaterialItemDto();
            materialItemDto.setErrorDtos(errorDtos);
            if (errorDtos.size() > 0) {
                materialItemDto.setNumber(0);
                return materialItemDto;
            }
            MaterialItemErrorDto errorDto = new MaterialItemErrorDto();
            errorDto.setLineNumber(0);
            errorDto.setErrorMessage("?????????????????????");
            materialItemDto.setNumber(1);
            return materialItemDto;
        } catch (Exception e) {
            log.info("import excel error:{}", e);
            throw new BaseException(ResultCode.IMPORT_EXCEPTIONS);
        }
    }

    public void checkRequireParam(MaterialItemModelDto materialItemModelDto) {
        if (StringUtils.isBlank(materialItemModelDto.getMaterialCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????!"));
        }
        if (StringUtils.isBlank(materialItemModelDto.getMaterialName())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????!"));
        }
        if (StringUtils.isBlank(materialItemModelDto.getSpecification())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????!"));
        }
        if (StringUtils.isBlank(materialItemModelDto.getCeeaDeliveryCycle())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????!"));
        }
    }

    /**
     * @Description ????????????????????????
     * @Param: [materialItemExportParam, response]
     * @Return: void
     * @Author: dengyl23@meicloud.com
     * @Date: 2020/9/25 9:28
     */
    @Override
    public void exportMaterialItemExcel(ExportExcelParam<MaterialItem> materialItemExportParam, HttpServletResponse response) throws IOException {
        // ?????????????????????
        List<List<Object>> dataList = this.queryExportData(materialItemExportParam);
        // ??????
        List<String> head = materialItemExportParam.getMultilingualHeader(materialItemExportParam, MaterialItemExportUtils.getMaterItemTitles());
        // ?????????
        String fileName = materialItemExportParam.getFileName();
        // ????????????
        EasyExcelUtil.exportStart(response, dataList, head, fileName);
    }


    /**
     * @Description ???????????????????????????excel?????????
     * @Param: [materialItemExportParam]
     * @Return: java.util.List<java.util.List < java.lang.Object>>
     * @Author: dengyl23@meicloud.com
     * @Date: 2020/9/24 10:30
     */
    private List<List<Object>> queryExportData(ExportExcelParam<MaterialItem> materialItemExportParam) {
        MaterialItem queryParam = materialItemExportParam.getQueryParam(); // ????????????
        /**
         * ???????????????????????????????????????, ??????2???????????????????????????????????????????????????,??????????????????2???
         */
        int count = queryCountByParam(queryParam);
        if(count > 20000){
            /**
             * ????????????????????????20000, ??????????????????????????????
             */
            if(StringUtil.notEmpty(queryParam.getPageSize()) && StringUtil.notEmpty(queryParam.getPageNum())){
                Assert.isTrue(queryParam.getPageSize() <=20000,"????????????????????????20000");
            }else {
                throw new BaseException("??????????????????????????????20000???,?????????????????????,????????????20000;");
            }
        }

        boolean flag = null != queryParam.getPageSize() && null != queryParam.getPageNum();
        if (flag) {
            // ????????????
            PageUtil.startPage(queryParam.getPageNum(), queryParam.getPageSize());
        }
        List<MaterialItem> materialItemList = this.queryMaterialItemByParam(queryParam);
        // ???Map
        List<Map<String, Object>> mapList = BeanMapUtils.objectsToMaps(materialItemList);
        List<String> titleList = materialItemExportParam.getTitleList();
        List<List<Object>> results = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(mapList)) {
            mapList.forEach((map) -> {
                List<Object> list = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(titleList)) {
                    titleList.forEach((title) -> {
                        Object value = map.get(title);
                        if ("ceeaMaterialStatus".equals(title)) {
                            String ceeaMaterialStatus = (String) value;
                            list.add(translationCeeaMaterialStatus(ceeaMaterialStatus));
                        } else if ("ceeaIfCatalogMaterial".equals(title)) {
                            String ceeaIfCatalogMaterial = (String) value;
                            if (StringUtils.isNotBlank(ceeaIfCatalogMaterial)) {
                                list.add(ceeaIfCatalogMaterial.equals("Y") ? "???" : "???");
                            } else {
                                list.add("");
                            }
                        } else {
                            if (null != value) {
                                list.add(value);
                            } else {
                                list.add("");
                            }
                        }
                    });
                }
                results.add(list);
            });
        }
        return results;
    }

    private String translationCeeaMaterialStatus(String type) {
        if (StringUtils.isNotBlank(type)) {
            switch (type) {
                case "NOT_NOTIFIED":
                    return CeeaMaterialStatus.NOT_NOTIFIED.getType();
                case "MAINTAINED":
                    return CeeaMaterialStatus.MAINTAINED.getType();
                case "NOTIFIED":
                    return CeeaMaterialStatus.NOTIFIED.getType();
                default:
                    return "";
            }
        } else {
            return "";
        }

    }

    /*
     * @Description ?????????????????????
     * @Param: [queryParam]
     * @Return: java.util.List<com.midea.cloud.srm.model.base.material.MaterialItem>
     * @Author: dengyl23@meicloud.com
     * @Date: 2020/9/24 10:29
     */
    private List<MaterialItem> queryMaterialItemByParam(MaterialItem queryParam) {
        QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
        String ceeaMaterialStatus = queryParam.getCeeaMaterialStatus();
        String categoryName = queryParam.getCategoryName();
        String materialName = queryParam.getMaterialName();
        String ifCatalog = queryParam.getCeeaIfCatalogMaterial();
        String ceeaSupplierName = queryParam.getCeeaSupplierName();
        String contractNo = queryParam.getCeeaContractNo();
        queryWrapper.lambda().eq(StringUtils.isNotBlank(categoryName), MaterialItem::getCategoryName, categoryName)
                .eq(StringUtils.isNotBlank(ceeaMaterialStatus), MaterialItem::getCeeaMaterialStatus, ceeaMaterialStatus)
                .eq(StringUtils.isNotBlank(materialName), MaterialItem::getMaterialName, materialName)
//                .eq(StringUtils.isNotBlank(ifCatalog), MaterialItem::getCeeaIfCatalogMaterial, ifCatalog)
                .eq(StringUtils.isNotBlank(ceeaSupplierName), MaterialItem::getCeeaSupplierName, ceeaSupplierName)
                .eq(StringUtils.isNotBlank(contractNo), MaterialItem::getCeeaContractNo, contractNo);
        if("Y".equals(ifCatalog)){
            queryWrapper.lambda().eq(MaterialItem::getCeeaIfCatalogMaterial,ifCatalog);
        }else {
            queryWrapper.and(wrapper ->wrapper.ne("CEEA_IF_CATALOG_MATERIAL","Y").
                    or().isNull("CEEA_IF_CATALOG_MATERIAL"));
        }
        List<MaterialItem> materialItemList = list(queryWrapper);
        return materialItemList;
    }

    private int queryCountByParam(MaterialItem queryParam) {
        QueryWrapper<MaterialItem> queryWrapper = new QueryWrapper<>();
        String ceeaMaterialStatus = queryParam.getCeeaMaterialStatus();
        String categoryName = queryParam.getCategoryName();
        String materialName = queryParam.getMaterialName();
        String ifCatalog = queryParam.getCeeaIfCatalogMaterial();
//        if ("N".equals(materialItem.getCeeaIfCatalogMaterial())) {
//            wrapper.notIn("CEEA_IF_CATALOG_MATERIAL", "Y").or().isNull("CEEA_IF_CATALOG_MATERIAL");
//        }
        String ceeaSupplierName = queryParam.getCeeaSupplierName();
        String contractNo = queryParam.getCeeaContractNo();
        queryWrapper.lambda().eq(StringUtils.isNotBlank(categoryName), MaterialItem::getCategoryName, categoryName)
                .eq(StringUtils.isNotBlank(ceeaMaterialStatus), MaterialItem::getCeeaMaterialStatus, ceeaMaterialStatus)
                .eq(StringUtils.isNotBlank(materialName), MaterialItem::getMaterialName, materialName)
//                .eq(StringUtils.isNotBlank(ifCatalog), MaterialItem::getCeeaIfCatalogMaterial, ifCatalog)
                .eq(StringUtils.isNotBlank(ceeaSupplierName), MaterialItem::getCeeaSupplierName, ceeaSupplierName)
                .eq(StringUtils.isNotBlank(contractNo), MaterialItem::getCeeaContractNo, contractNo);
        if("Y".equals(ifCatalog)){
            queryWrapper.lambda().eq(MaterialItem::getCeeaIfCatalogMaterial,ifCatalog);
        }else {
            queryWrapper.and(wrapper -> wrapper.ne("CEEA_IF_CATALOG_MATERIAL", "Y").
                    or().isNull("CEEA_IF_CATALOG_MATERIAL"));
        }
        return this.count(queryWrapper);
    }

    @Override
    public List<PurchaseCatalogQueryDto> listCeeaListPurchaseCatalog(List<PurchaseCatalogQueryDto> purchaseCatalogQueryDtoList) {
        return baseMapper.PurchaseCatalogQueryBatch(purchaseCatalogQueryDtoList);
    }

    /**
     * ??????erp??????Id????????????????????????
     */
    @Override
    @Transactional
    public void setErpMaterialId() {
        List<MaterialItem> materialItems = this.list();
        List<MaterialItem> updateMaterialItems = new ArrayList<>();
        for (MaterialItem materialItem : materialItems) {
            if (materialItem != null) {
                String materialCode = materialItem.getMaterialCode();
                Assert.notNull(materialCode, "??????Id???[" + materialItem.getMaterialId() + "]?????????????????????????????????");
                List<ErpMaterialItem> erpMaterialItems = iErpMaterialItemService.list(
                        new QueryWrapper<>(new ErpMaterialItem().setItemNumber(materialCode)));
                if (CollectionUtils.isEmpty(erpMaterialItems)) {
                    log.info("??????????????????[" + materialCode + "]??????ceea_base_material_item??????????????????????????????");
                } else {
                    Long erpMaterialId = erpMaterialItems.get(0).getItemId();
                    if (erpMaterialId == null) {
                        log.info("??????????????????[" + materialCode + "]??????ceea_base_material_item?????????????????????erp??????Id?????????");
                    }
                    materialItem.setCeeaErpMaterialId(erpMaterialId);
                    updateMaterialItems.add(materialItem);
                }
            }
        }
        //????????????
        this.updateBatchById(updateMaterialItems);
    }


    @Override
    public MaterialItem saveOrUpdateSrmMaterialItem(ErpMaterialItem erpMaterialItem,
                                                    MaterialItem saveMaterialItem,
                                                    MaterialCache materialCache) {
        if (Objects.isNull(saveMaterialItem)) {
            saveMaterialItem = new MaterialItem();
            saveMaterialItem.setMaterialId(IdGenrator.generate());
            saveMaterialItem = setMaterialItemField(erpMaterialItem,
                    saveMaterialItem,
                    materialCache);
            materialCache.getSaveMaterialItemList().add(saveMaterialItem);
        } else {
            saveMaterialItem = setMaterialItemField(erpMaterialItem,
                    saveMaterialItem,
                    materialCache);
            materialCache.getUpdateMaterialItemList().add(saveMaterialItem);
        }
        return saveMaterialItem;
    }

    /**
     * @Description ???????????????????????????(??????)
     */
    @Override
    public void saveOrUpdateSrmMaterialItems(List<ErpMaterialItem> erpMaterialItemList, Map<String, MaterialItem> dbMaterialItemMap, AtomicInteger successCount, AtomicInteger errorCount) {
//        List<MaterialItem> saveMaterialList = new ArrayList<>();
//        List<MaterialItem> updateMaterialList = new ArrayList<>();
//        MaterialItem saveMaterialItem = new MaterialItem();
//        MaterialItem updateMaterialItem = new MaterialItem();
//        for (ErpMaterialItem erpMaterialItem : erpMaterialItemList) {
//            //???????????????
//            if (dbMaterialItemMap.containsKey(erpMaterialItem.getItemNumber())) {
//                MaterialItem material = dbMaterialItemMap.get(erpMaterialItem.getItemNumber());
//                updateMaterialItem.setMaterialId(material.getMaterialId());
//                updateMaterialItem = setMaterialItemField(erpMaterialItem, updateMaterialItem);
//                updateMaterialList.add(updateMaterialItem);
//            } else {
//                Long id = IdGenrator.generate();
//                saveMaterialItem.setMaterialId(id);
//                saveMaterialItem = setMaterialItemField(erpMaterialItem, saveMaterialItem);
//                saveMaterialList.add(saveMaterialItem);
//            }
//
//            try {
//                if (CollectionUtils.isNotEmpty(saveMaterialList)) {
//                    this.saveBatch(saveMaterialList);
//                }
//                if (CollectionUtils.isNotEmpty(updateMaterialList)) {
//                    this.updateBatchById(updateMaterialList);
//                }
//                int success = saveMaterialList.size() + updateMaterialList.size();
//                successCount.addAndGet(success);
//            } catch (Exception e) {
//                e.printStackTrace();
//                errorCount.addAndGet(500);
//            }
//        }
    }

    /**
     * @param erpMaterialItem
     * @description ??????????????????????????????materialId????????????@modifiedBy xiexh12@meicloud.com
     */
    private MaterialItem setMaterialItemField(ErpMaterialItem erpMaterialItem,
                                              MaterialItem materialItem,
                                              MaterialCache materialCache) {
        List<Category> erpCategoryList = erpMaterialItem.getCategoryList();

        materialItem.setMaterialCode(erpMaterialItem.getItemNumber());
        materialItem.setMaterialName(erpMaterialItem.getItemDescZhs());
        if (itemStatusSet.contains(erpMaterialItem.getItemStatus())) {
            materialItem.setStatus("Y");
        } else {
            materialItem.setStatus("N");
        }
        materialItem.setUnit(erpMaterialItem.getPrimaryUnitOfMeasure());
        materialItem.setUnitName(erpMaterialItem.getPrimaryUnitOfMeasure());
        //???????????????erp??????Id
        materialItem.setCeeaErpMaterialId(erpMaterialItem.getItemId());
        //???????????????????????????????????????????????? #?????????????????????DB????????????
        saveOrUpdatePurchaseUnit(erpMaterialItem.getPrimaryUnitOfMeasure(), materialCache);

        /**?????? ????????????????????????????????????????????????????????????????????????????????????????????? **/
        AtomicBoolean hasCategory = new AtomicBoolean(false);
        if (CollectionUtils.isNotEmpty(erpCategoryList)) {
            erpMaterialItem.getCategoryList().forEach(category -> {
                if (IErpService.CATEGORY_SET_CODE.equals(category.getCategorySetCode())
                        && IErpService.CATEGORY_SET_NAME_ZHS.equals(category.getCategorySetNameUs())
                        && category.getSetValue() != null
                        && category.getSetValueDescZhs() != null
                        && !IErpService.CATEGORY_SET_VALUE_DEFAULT.equals(category.getSetValue())
                        && !IErpService.CATEGORY_VALUE_DES_ZHS.equals(category.getSetValueDescZhs())) {
                    hasCategory.set(true);
                }
            });
        }
        //?????????????????????????????????????????????
        if (hasCategory.get()) {
            materialItem = setMaterialPurchaseCategory(materialItem, erpCategoryList, materialCache);
        }
        /**?????? ????????????????????????????????????????????????????????????????????????????????????????????? **/

        /**?????? ?????????????????????????????????????????????????????????????????????????????? **/
        AtomicBoolean hasInventory = new AtomicBoolean(false);
        erpCategoryList.forEach(category -> {
            if (IErpService.INVENTORY_SET_CODE.equals(category.getCategorySetCode())
                    && IErpService.INVENTORY_SET_NAME_ZHS.equals(category.getCategorySetNameZhs())
                    && category.getSetValue() != null
                    && category.getSetValueDescZhs() != null) {
                hasInventory.set(true);
            }
        });
        if (hasInventory.get()) {
            materialItem = setMaterialInventory(materialItem, erpCategoryList);
        }

        return materialItem;
    }

    /**
     * ????????????????????????????????? ???????????????????????????????????????
     *
     * @param saveMaterialItem
     * @param erpCategoryList
     * @return
     */
    private MaterialItem setMaterialInventory(MaterialItem saveMaterialItem, List<Category> erpCategoryList) {
        for (Category category : erpCategoryList) {
            String categorySetCode = category.getCategorySetCode(); //????????????????????? '1'
            String categorySetName = category.getCategorySetNameZhs(); //????????????????????? '??????'

            //?????? ??????????????????????????????
            if (StringUtils.isNotBlank(categorySetCode) && StringUtils.isNotBlank(categorySetName)
                    && categorySetCode.equals("1") && categorySetName.equals("??????")) {
                //?????????????????????????????????????????????
                String inventoryCode = category.getSetValue();
                String inventoryName = category.getSetValueDescZhs();
                saveMaterialItem.setInventoryCode(inventoryCode);
                saveMaterialItem.setInventoryName(inventoryName);
            }
            //?????? ??????????????????????????????
        }
        return saveMaterialItem;
    }


    private final String PURCHASE_CATEGORY_CACHE = "PURCHASE_CATEGORY_CACHE_LIST";//????????????

    /**
     * ???????????????????????????
     * ????????????Id??????????????????????????????struct?????????categoryFullName
     *
     * @param saveMaterialItem
     * @param erpCategoryList
     * @return
     */
    private MaterialItem setMaterialPurchaseCategory(MaterialItem saveMaterialItem,
                                                     List<Category> erpCategoryList,
                                                     MaterialCache materialCache) {
        /** ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????? **/
        List<PurchaseCategory> cacheCategorys = materialCache.getCategorys();
        if (Objects.isNull(cacheCategorys) || cacheCategorys.isEmpty()) {
            //????????????????????????????????????
            cacheCategorys = iPurchaseCategoryService.list();
            materialCache.getCategorys().addAll(cacheCategorys);
        }
        for (Category category : erpCategoryList) {
            String categorySetCode = category.getCategorySetCode(); //????????????????????? '1100000101'
            String categorySetName = category.getCategorySetNameUs(); //????????????????????? 'SRM????????????'

            //?????? ??????????????????SRM????????????
            if (IErpService.CATEGORY_SET_CODE.equals(categorySetCode) &&
                    IErpService.CATEGORY_SET_NAME_ZHS.equals(categorySetName)) {

                //??????????????????????????? ???300615
                String purchaseCategoryCode = category.getSetValue();

                //?????????????????????????????????????????????
                //Assert.notEmpty(categorys, "????????????????????????????????????????????????????????????");
                if (CollectionUtils.isNotEmpty(cacheCategorys)) {
                    PurchaseCategory purchaseCategory = null;
                    for (PurchaseCategory cacheData : cacheCategorys) {
                        if (cacheData.getCategoryCode().equals(purchaseCategoryCode)) {
                            purchaseCategory = cacheData;
                            break;
                        }
                    }
                    //??????????????????????????????????????????????????????????????????
                    if (Objects.isNull(purchaseCategory)) {
                        continue;
                    }

                    saveMaterialItem.setCategoryId(purchaseCategory.getCategoryId());
                    saveMaterialItem.setCategoryName(purchaseCategory.getCategoryName());
                    //????????????????????????srm???????????????3???????????????????????????
                    Assert.notNull(purchaseCategory.getStruct(), "????????????????????????????????????3????????????????????????");
                    //?????? ???????????????????????????3???????????????struct?????????????????????categoryFullName
                    saveMaterialItem.setStruct(purchaseCategory.getStruct());
                    PurchaseCategory cate = new PurchaseCategory().setStruct(purchaseCategory.getStruct());

                    //????????????????????????
                    String struct = cate.getStruct();
                    List<Long> categoryIds = StringUtil.stringConvertNumList(struct, "-");
                    if (!CollectionUtils.isEmpty(categoryIds)) {
                        List<PurchaseCategory> list = materialCache.getCategorys();
                        //????????????????????????
                        String[] categoryFullNames = new String[categoryIds.size()];
                        for (int i = 0; i < categoryIds.size(); i++) {
                            Long categoryId = categoryIds.get(i);
                            for (PurchaseCategory cacheCategory : list) {
                                if (categoryId.equals(cacheCategory.getCategoryId())) {
                                    categoryFullNames[i] = cacheCategory.getCategoryName();
                                    break;
                                }
                            }
                        }
                        //????????? ???-??????????????????
                        String categoryFullName = String.join("-", categoryFullNames);
                        cate.setCategoryFullName(categoryFullName);
                    }
                    //
                    Assert.notNull(cate.getCategoryFullName(), "??????????????????????????????????????????????????????????????????");
                    String saveCategoryFullName = cate.getCategoryFullName();
                    //?????? ???????????????????????????3???????????????struct?????????????????????categoryFullName
                    saveMaterialItem.setCategoryFullName(saveCategoryFullName);
//                    iPurchaseCategoryService.SaveOrUpdateByCategoryFullCode(purchaseCategoryCode, saveCategoryFullName);
                    break;
                }
            }
            //?????? ??????????????????SRM????????????
        }
        return saveMaterialItem;
    }


    private final String PURCHASE_UNIT = "IMPORT_PURCHASE_UNIT";

    /**
     * ????????????????????????????????????????????????
     *
     * @param purchaseUnit
     */
    private void saveOrUpdatePurchaseUnit(String purchaseUnit, MaterialCache materialCache) {
        QueryWrapper<PurchaseUnit> unitQueryWrapper = new QueryWrapper<>();
        unitQueryWrapper.eq("UNIT_CODE", purchaseUnit);
        unitQueryWrapper.eq("UNIT_NAME", purchaseUnit);

        List<PurchaseUnit> purchaseUnits = materialCache.getPurchaseUnits();
        if (Objects.isNull(purchaseUnits) || purchaseUnits.isEmpty()) {
            purchaseUnits = iPurchaseUnitService.list();
            materialCache.getPurchaseUnits().addAll(purchaseUnits);
        }
        AtomicBoolean dbExistFlag = new AtomicBoolean(false);
        purchaseUnits.stream().forEach(e -> {
            if (purchaseUnit.equals(e.getUnitName()) && purchaseUnit.equals(e.getUnitCode())) {
                dbExistFlag.set(true);
            }
        });
        if (!dbExistFlag.get()) {
            PurchaseUnit saveUnit = new PurchaseUnit();
            Long unitId = IdGenrator.generate();
            saveUnit.setUnitId(unitId);
            saveUnit.setUnitCode(purchaseUnit);
            saveUnit.setUnitName(purchaseUnit);
            saveUnit.setEnabled("Y");
            saveUnit.setSourceSystem(DataSourceEnum.ERP_SYS.getValue());
            iPurchaseUnitService.save(saveUnit);
            //??????????????????
            materialCache.getPurchaseUnits().add(saveUnit);
        }
    }


    /**
     * ??????????????????????????????
     *
     * @param erpMaterialItem
     * @param materialItem
     * @param invs
     * @param invOUs
     * @throws BaseException
     */
    @Override
    public void saveOrUpdateSrmMaterialOrg(ErpMaterialItem erpMaterialItem,
                                           MaterialItem materialItem,
                                           List<Organization> invs,
                                           Map<String, Organization> invOUs,
                                           MaterialCache materialCache) throws BaseException {
        /** ??????Srm?????????scc_base_material_item??????material_id **/
        Long materialId = materialItem.getMaterialId();

        //??????????????????
        String organizationCode = erpMaterialItem.getOrgCode();
        List<Organization> organizations = invs.stream().filter(
                organization -> organizationCode.equals(organization.getOrganizationCode())
        ).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(organizations)) {
            throw new BaseException("??????????????????????????????????????????????????????????????????");
        }
        if (Objects.isNull(organizations.get(0).getParentOrganizationIds())) {
            throw new BaseException("????????????????????????????????????????????????????????????????????????????????????Id?????????");
        }

        Long organizationId = organizations.get(0).getOrganizationId();
        String organizationName = organizations.get(0).getOrganizationName();
        Long orgId = Long.valueOf(organizations.get(0).getParentOrganizationIds());

        //?????????????????????????????????????????????????????????
        Organization ou = Objects.nonNull(invOUs.get(organizationCode)) ? invOUs.get(organizationCode) : null;
        if (Objects.isNull(ou)) {
            throw new BaseException("??????????????????????????????????????????????????????????????????");
        }
        if (Objects.isNull(ou.getOrganizationName())) {
            throw new BaseException("????????????????????????????????????????????????????????????");
        }

        String orgName = ou.getOrganizationName();
        MaterialOrg saveMaterialOrg = new MaterialOrg();
        saveMaterialOrg.setMaterialId(materialId); //??????Id
        saveMaterialOrg.setOrganizationId(organizationId); //????????????Id
        saveMaterialOrg.setOrganizationName(organizationName); //??????????????????
        saveMaterialOrg.setOrgId(orgId); //????????????Id
        saveMaterialOrg.setOrgName(orgName); //??????????????????
        //???????????? ???????????????
        if (itemStatusSet.contains(erpMaterialItem.getItemStatus())) {
            saveMaterialOrg.setItemStatus("Y");
        } else {
            saveMaterialOrg.setItemStatus("N");
        }
        saveMaterialOrg.setUserPurchase(erpMaterialItem.getPurchasingEnableFlag()); //?????????
        saveMaterialOrg.setStockEnableFlag(erpMaterialItem.getStockEnableFlag()); //?????????

        //?????????????????????????????????
        boolean isCreate = true;
        for (int i = 0; i < materialCache.getInDBMaterialOrgList().size(); i++) {
            MaterialOrg materialOrg = materialCache.getInDBMaterialOrgList().get(i);
            if (materialOrg.getMaterialId().equals(materialId) &&
                    materialOrg.getOrganizationId().equals(organizationId) &&
                    materialOrg.getOrgId().equals(orgId)) {
                saveMaterialOrg.setMaterialOrgId(materialOrg.getMaterialOrgId());
                isCreate = false;
                break;
            }
        }

        if (isCreate) {
            saveMaterialOrg.setMaterialOrgId(IdGenrator.generate()); //??????Id
            materialCache.getSaveMaterialOrgList().add(saveMaterialOrg);
            materialCache.getInDBMaterialOrgList().add(saveMaterialOrg);
        } else {
            materialCache.getUpdateMaterialOrgList().add(saveMaterialOrg);
        }
    }

    public static final List<String> code1 = Arrays.stream(new String[]{"10", "30", "40", "50", "60"}).collect(Collectors.toList());
    public static final List<String> code2 = Arrays.asList(new String[]{"20"});
    public static final Set<String> code3 = Arrays.stream(new String[]{"70"}).collect(Collectors.toSet());
    public static final Set<String> code4 = Arrays.stream(new String[]{"7001", "7002", "7003", "7004", "7005", "7007"}).collect(Collectors.toSet());

    /**
     * ??????????????????????????????
     *
     * @param materialId
     * @return
     */
    @Override
    public List<PurchaseTax> queryTaxByItem(Long materialId) {
        List<PurchaseTax> purchaseTaxes = null;
        Assert.notNull(materialId, "????????????:materialId");
        /**
         * 1????????????????????????10???30???40???50???60????????????????????????IN?????????????????????IN 13,IN 11 ,?????????IN VAT ???IN-ASSET???
         * 2????????????????????????20???????????????IN-ASSET??????????????????
         * 3????????????????????????70??????????????????7001,7002,7003,7004,7005,7007????????????????????????61?????????????????????IN-ASSET???????????????????????????????????????IN?????????
         */
        /**
         * ??????:
         * ?????????70???????????????????????????61???78?????????????????????IN-ASSET?????????70?????????????????????????????????IN???
         */
        MaterialItem materialItem = this.getById(materialId);
        Assert.notNull(materialItem, "??????????????????");
        // ?????????????????????
        String struct = materialItem.getStruct();
        Assert.notNull(struct, "?????????????????????????????????");
        List<String> idList = Arrays.asList(struct.split("-"));
        // ????????????
        PurchaseCategory purchaseCategory = iPurchaseCategoryService.getById(Long.parseLong(idList.get(0).trim()));
        String categoryCode = purchaseCategory.getCategoryCode();
        if (code1.contains(categoryCode)) {
            /**
             * ????????????IN?????????????????????IN 13,IN 11 ,?????????IN VAT ???IN-ASSET???
             */
            purchaseTaxes = purchaseTaxMapper.queryTaxBy1();
        } else if (code2.contains(categoryCode)) {
            /**
             * ????????????IN-ASSET??????????????????
             */
            purchaseTaxes = purchaseTaxMapper.queryTaxBy2();
        } else if (code3.contains(categoryCode)) {
            /**
             * ?????????70???????????????????????????61???78?????????????????????IN-ASSET?????????70?????????????????????????????????IN???
             */

//            String categoryCode1 = iPurchaseCategoryService.getById(Long.parseLong(idList.get(1).trim())).getCategoryCode();
            if (StringUtils.startsWith(materialItem.getMaterialCode(), "61") || StringUtils.startsWith(materialItem.getMaterialCode(), "78")) {
                purchaseTaxes = purchaseTaxMapper.queryTaxBy2();
            } else {
                purchaseTaxes = purchaseTaxMapper.queryTaxBy3();
            }
        }
        return purchaseTaxes;
    }

    /**
     * ????????????-??????????????????
     * 1????????????????????????10???30???40???60????????????????????????IN?????????????????????IN 13,IN 11 ,?????????IN VAT ???IN-ASSET???
     * 2????????????????????????20???????????????????????????61???78????????????????????????IN-ASSET??????????????????
     * 3????????????????????????70???????????????????????????61???78????????????????????????IN-ASSET???????????????????????????????????????IN?????????
     * 4. ?????????vat0???????????????,?????????
     *
     * @param materialId
     * @return
     */
    @Override
    public List<PurchaseTax> queryTaxByItemForOrder(Long materialId) {
        List<String> bigCategoryCodeList = Arrays.asList("10", "30", "40", "60");
        List<String> materialCodeList = Arrays.asList("61", "78");

        List<PurchaseTax> purchaseTaxes = null;
        MaterialItem materialItem = null;
        String struct = null;
        List<String> idList = null;

        try {
            Assert.notNull(materialId, "????????????:materialId");
            materialItem = this.getById(materialId);
            Assert.notNull(materialItem, "??????????????????");
            // ?????????????????????
            struct = materialItem.getStruct();
            Assert.notNull(struct, "?????????????????????????????????");
            idList = Arrays.asList(struct.split("-"));
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        // ????????????
        PurchaseCategory purchaseCategory = iPurchaseCategoryService.getById(Long.parseLong(idList.get(0).trim()));
        String categoryCode = purchaseCategory.getCategoryCode();

        if (bigCategoryCodeList.contains(categoryCode)) {
            /* ?????????????????????10???30???40???60????????????????????????IN?????????????????????IN 13,IN 11 ,?????????IN VAT ???IN-ASSET???*/
            purchaseTaxes = purchaseTaxMapper.queryTaxBy1();
        } else if ("20".equals(categoryCode) &&
                (StringUtils.startsWith(materialItem.getMaterialCode(), "61") ||
                        StringUtils.startsWith(materialItem.getMaterialCode(), "78"))
        ) {
            /* ?????????????????????20???????????????????????????61???78????????????????????????IN-ASSET??????????????????*/
            purchaseTaxes = purchaseTaxMapper.queryTaxBy2();
        } else if ("70".equals(categoryCode)) {
            /**
             * ?????????????????????70???????????????????????????61???78????????????????????????IN-ASSET??????????????????
             * ?????????????????????IN?????????
             */
            if (StringUtils.startsWith(materialItem.getMaterialCode(), "61") ||
                    StringUtils.startsWith(materialItem.getMaterialCode(), "78")) {
                purchaseTaxes = purchaseTaxMapper.queryTaxBy2();
            } else {
                purchaseTaxes = purchaseTaxMapper.queryTaxBy3();
            }
        } else {
            purchaseTaxes = purchaseTaxMapper.selectList(new QueryWrapper<>());
        }
        /*???????????????VAT 0,????????????VAT 0??????*/
        boolean hasVAT = false;
        for(PurchaseTax purchaseTax : purchaseTaxes){
            if("VAT 0".equals(purchaseTax.getTaxKey())){
                hasVAT = true;
            }
        }
        if(!hasVAT){
            List<PurchaseTax> purchaseTaxList = purchaseTaxMapper.selectList(new QueryWrapper<>(new PurchaseTax().setTaxKey("VAT 0").setEnabled("Y")));
            if(CollectionUtils.isNotEmpty(purchaseTaxList)){
                purchaseTaxes.add(purchaseTaxList.get(0));
            }
        }

        return purchaseTaxes;
    }

    //????????????id???????????????Key
    @Override
    public Map<Long, Set<String>> queryTaxItemBatch(Collection<Long> materialIds) {
        List<String> bigCategoryCodeList = Arrays.asList("10", "30", "40", "60");

        List<MaterialItem> list = list(Wrappers.lambdaQuery(MaterialItem.class)
                .select(MaterialItem::getMaterialId, MaterialItem::getStruct, MaterialItem::getMaterialName, MaterialItem::getMaterialCode)
                .in(MaterialItem::getMaterialId, materialIds)
        );
        for (MaterialItem materialItem : list) {
            if (StringUtil.isEmpty(materialItem.getStruct())) {
                throw new BaseException(String.format("???????????????%s???????????????"));
            }
        }
        Map<Long, Set<String>> result = new HashMap<>();
        Function<MaterialItem, Long> function = materialItem -> Long.valueOf(materialItem.getStruct().split("-")[0]);
        if (Objects.nonNull(list) && !list.isEmpty()) {
            Map<Long, List<MaterialItem>> categoryIdMap = list.stream().map(e -> e.setStruct(e.getStruct().split("-1")[0]))
                    .collect(Collectors.groupingBy(function));
            if (!org.springframework.util.CollectionUtils.isEmpty(categoryIdMap)) {
                Map<Long, PurchaseCategory> categoryMap = iPurchaseCategoryService.list(Wrappers.lambdaQuery(PurchaseCategory.class)
                        .select(PurchaseCategory::getCategoryId, PurchaseCategory::getCategoryCode)
                        .in(PurchaseCategory::getCategoryId, categoryIdMap.keySet())
                ).stream().collect(Collectors.toMap(PurchaseCategory::getCategoryId, Function.identity()));
                List<PurchaseTax> queryTaxBy1 = null;
                List<PurchaseTax> queryTaxBy2 = null;
                List<PurchaseTax> queryTaxBy3 = null;
                List<PurchaseTax> all = null;

                for (Map.Entry<Long, PurchaseCategory> entry : categoryMap.entrySet()) {
                    PurchaseCategory value = entry.getValue();
                    String categoryCode = value.getCategoryCode();
                    Long categoryId = value.getCategoryId();
                    List<MaterialItem> temp = categoryIdMap.get(categoryId);
                    for (MaterialItem materialItem : temp) {
                        if (bigCategoryCodeList.contains(categoryCode)) {
                            /* ?????????????????????10???30???40???60????????????????????????IN?????????????????????IN 13,IN 11 ,?????????IN VAT ???IN-ASSET???*/
                            if (Objects.isNull(queryTaxBy1)) {
                                queryTaxBy1 = purchaseTaxMapper.queryTaxBy1();
                            }
                            Set<String> taxKey = queryTaxBy1.stream().map(PurchaseTax::getTaxKey).collect(Collectors.toSet());
                            result.putIfAbsent(materialItem.getMaterialId(), taxKey);
                        } else if ("20".equals(categoryCode) &&
                                (StringUtils.startsWith(materialItem.getMaterialCode(), "61") ||
                                        StringUtils.startsWith(materialItem.getMaterialCode(), "78"))
                        ) {
                            /* ?????????????????????20???????????????????????????61???78????????????????????????IN-ASSET??????????????????*/
                            if (Objects.isNull(queryTaxBy2)) {
                                queryTaxBy2 = purchaseTaxMapper.queryTaxBy2();
                            }
                            Set<String> taxKey = queryTaxBy2.stream().map(PurchaseTax::getTaxKey).collect(Collectors.toSet());
                            result.putIfAbsent(materialItem.getMaterialId(), taxKey);
                        } else if ("70".equals(categoryCode)) {
                            /**
                             * ?????????????????????70???????????????????????????61???78????????????????????????IN-ASSET??????????????????
                             * ?????????????????????IN?????????
                             */
                            if (StringUtils.startsWith(materialItem.getMaterialCode(), "61") ||
                                    StringUtils.startsWith(materialItem.getMaterialCode(), "78")) {
                                if (Objects.isNull(queryTaxBy2)) {
                                    queryTaxBy2 = purchaseTaxMapper.queryTaxBy2();
                                }
                                Set<String> taxKey = queryTaxBy2.stream().map(PurchaseTax::getTaxKey).collect(Collectors.toSet());
                                result.putIfAbsent(materialItem.getMaterialId(), taxKey);
                            } else {
                                if (Objects.isNull(queryTaxBy3)) {
                                    queryTaxBy3 = purchaseTaxMapper.queryTaxBy3();
                                }
                                Set<String> taxKey = queryTaxBy3.stream().map(PurchaseTax::getTaxKey).collect(Collectors.toSet());
                                result.putIfAbsent(materialItem.getMaterialId(), taxKey);
                            }
                        } else {
                            if (Objects.isNull(all)) {
                                all = purchaseTaxMapper.selectList(null);
                            }
                            Set<String> taxKey = all.stream().map(PurchaseTax::getTaxKey).collect(Collectors.toSet());
                            result.putIfAbsent(materialItem.getMaterialId(), taxKey);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public List<MaterialItem> listAllForTranferOrder(MaterialQueryDTO materialQueryDTO) {
        return materialItemMapper.listAllForTranferOrder(materialQueryDTO);
    }

    @Override
    public List<MaterialItem> listAllForImportPriceLibrary() {
        return materialItemMapper.listAllForImportPriceLibrary();
    }

    /**
     * ????????????????????????????????????
     *
     * @param materialCodes
     * @return
     */
    @Override
    public Map<String,MaterialItem> listMaterialItemsByCodes(List<String> materialCodes) {
        Map<String, MaterialItem> itemHashMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(materialCodes)) {
            materialCodes = materialCodes.stream().distinct().collect(Collectors.toList());
            List<MaterialItem> materialItems = this.list(Wrappers.lambdaQuery(MaterialItem.class)
                    .in(MaterialItem::getMaterialCode, materialCodes));
            if (CollectionUtils.isNotEmpty(materialItems)) {
                itemHashMap = materialItems.stream().collect(Collectors.toMap(MaterialItem::getMaterialCode, v->v, (k1, k2) -> k1));
            }
        }
        return itemHashMap;
    }
}
