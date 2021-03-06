package com.midea.cloud.srm.inq.price.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.*;
import com.midea.cloud.common.enums.contract.ContractType;
import com.midea.cloud.common.enums.flow.CbpmFormTemplateIdEnum;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.common.utils.redis.RedisUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.contract.ContractClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.flow.WorkFlowFeign;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.inq.inquiry.service.IHeaderService;
import com.midea.cloud.srm.inq.price.mapper.ApprovalBiddingItemMapper;
import com.midea.cloud.srm.inq.price.mapper.ApprovalFileMapper;
import com.midea.cloud.srm.inq.price.mapper.ApprovalHeaderMapper;
import com.midea.cloud.srm.inq.price.mapper.ApprovalItemMapper;
import com.midea.cloud.srm.inq.price.service.*;
import com.midea.cloud.srm.inq.price.service.impl.convertor.ApprovalHeaderConvertor;
import com.midea.cloud.srm.inq.price.service.impl.convertor.ApprovalHeaderDetailConvertor;
import com.midea.cloud.srm.inq.price.workflow.PriceFlow;
import com.midea.cloud.srm.inq.quote.service.IQuoteLadderPriceService;
import com.midea.cloud.srm.inq.quote.service.IQuoteSelectionService;
import com.midea.cloud.srm.model.base.dict.entity.DictItem;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.material.vo.MaterialMaxCategoryVO;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationUser;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuDetailVO;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuGroupDetailVO;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCurrency;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseTax;
import com.midea.cloud.srm.model.cm.contract.dto.ContractDTO;
import com.midea.cloud.srm.model.cm.contract.dto.LevelMaintainImportDto;
import com.midea.cloud.srm.model.cm.contract.dto.LevelMaintainModelImportDto;
import com.midea.cloud.srm.model.cm.contract.entity.ContractHead;
import com.midea.cloud.srm.model.cm.contract.entity.ContractMaterial;
import com.midea.cloud.srm.model.cm.contract.entity.PayPlan;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.flow.process.dto.CbpmRquestParamDTO;
import com.midea.cloud.srm.model.inq.inquiry.entity.Header;
import com.midea.cloud.srm.model.inq.price.domain.*;
import com.midea.cloud.srm.model.inq.price.dto.*;
import com.midea.cloud.srm.model.inq.price.entity.*;
import com.midea.cloud.srm.model.inq.price.enums.ContractAgreement;
import com.midea.cloud.srm.model.inq.price.enums.TransferStatus;
import com.midea.cloud.srm.model.inq.price.vo.ApprovalAllVo;
import com.midea.cloud.srm.model.inq.price.vo.ApprovalBiddingItemVO;
import com.midea.cloud.srm.model.inq.price.vo.ApprovalVo;
import com.midea.cloud.srm.model.inq.quote.entity.QuoteLadderPrice;
import com.midea.cloud.srm.model.inq.quote.entity.QuoteSelection;
import com.midea.cloud.srm.model.pm.po.dto.NetPriceQueryDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementHead;
import com.midea.cloud.srm.model.rbac.permission.entity.Permission;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;


import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ????????????????????? ???????????????
 * </pre>
 *
 * @author linxc6@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-04-08 15:28:50
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class ApprovalHeaderServiceImpl extends ServiceImpl<ApprovalHeaderMapper, ApprovalHeader> implements IApprovalHeaderService {

    @Autowired
    private IQuoteLadderPriceService iQuoteLadderPriceService;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private IApprovalItemService iApprovalItemService;
    @Autowired
    private IApprovalLadderPriceService iApprovalLadderPriceService;
    @Autowired
    private IHeaderService iHeaderService;
    @Autowired
    private PriceLibraryServiceImpl iPriceLibraryService;
    @Autowired
    private WorkFlowFeign workFlowFeign;
    @Autowired
    private RbacClient rbacClient;
    @Autowired
    private IApprovalFileService iApprovalFileService;
    @Autowired
    private IQuoteSelectionService iQuoteSelectionService;
    @Resource
    private ApprovalItemMapper approvalItemMapper;
    @Autowired
    private IApprovalBiddingItemService iApprovalBiddingItemService;
    @Autowired
    private IApprovalHeaderService approvalHeaderService;
    @Resource
    private ApprovalHeaderMapper approvalHeaderMapper;
    @Resource
    private ApprovalFileMapper approvalFileMapper;
    @Resource
    private ApprovalBiddingItemMapper approvalBiddingItemMapper;
    @Autowired
    private IApprovalBiddingItemPaymentTermService approvalBiddingItemPaymentTermService;
    @Autowired
    private ContractClient contractClient;
    @Autowired
    private PmClient pmClient;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private PriceFlow priceFlow;
    @Resource
    private SupplierClient supplierClient;
    @Resource
    private FileCenterClient fileCenterClient;

    private static final String APPROVAL_TO_CONTRACT_LOCK = "approval_to_contract_lock";

    @Override
    public void importModelDownload(HttpServletResponse response) throws IOException {
        String fileName = "?????????????????????????????????";
        ArrayList<ApprovalBiddingItemImport> approvalBiddingItemImports = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        EasyExcelUtil.writeExcelWithModel(outputStream, fileName, approvalBiddingItemImports, ApprovalBiddingItemImport.class);
    }

    @Override
    public Map<String, Object> importExcel(MultipartFile file, Long approvalHeaderId, Fileupload fileupload) throws Exception {
        // ????????????
        EasyExcelUtil.checkParam(file, fileupload);
//        Assert.notNull(approvalHeaderId, "????????????: approvalHeaderId");
        // ??????excel??????
        List<ApprovalBiddingItemImport> approvalBiddingItemImports = EasyExcelUtil.readExcelWithModel(file, ApprovalBiddingItemImport.class);
        // ???????????????
        List<ApprovalBiddingItem> approvalBiddingItems = new ArrayList<>();
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ????????????
        checkData(approvalBiddingItemImports, approvalBiddingItems, errorFlag);
        if (errorFlag.get()) {
            // ?????????
            fileupload.setFileSourceName("?????????????????????????????????");
            Fileupload fileupload1 = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    approvalBiddingItemImports, ApprovalBiddingItemImport.class, file.getName(), file.getOriginalFilename(), file.getContentType());
            return ImportStatus.importError(fileupload1.getFileuploadId(), fileupload1.getFileSourceName());
        } else {
//            if (CollectionUtils.isNotEmpty(approvalBiddingItems)) {
//                approvalBiddingItems.forEach(approvalBiddingItem -> {
//                    approvalBiddingItem.setApprovalHeaderId(approvalHeaderId);
//                });
//                iApprovalBiddingItemService.saveBatch(approvalBiddingItems);
//            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", YesOrNo.YES.getValue());
        result.put("data", approvalBiddingItems);
        result.put("message","success");
        return result;
    }

    /**
     * ??????????????????
     *
     * @param approvalBiddingItemImports
     * @param approvalBiddingItems
     * @param errorFlag
     */
    public void checkData(List<ApprovalBiddingItemImport> approvalBiddingItemImports,
                          List<ApprovalBiddingItem> approvalBiddingItems, AtomicBoolean errorFlag) {
        /**
         * ??????????????????????????????
         * ??????(??????-??????)/??????(??????-??????)/?????????(??????-?????????)/??????(??????-??????)/??????(????????????-??????)/
         */
        if (CollectionUtils.isNotEmpty(approvalBiddingItemImports)) {
            List<String> orgNames = new ArrayList<>(); // ??????
            List<String> itemCodes = new ArrayList<>(); // ??????
            List<String> vendorCodes = new ArrayList<>(); // ?????????
            List<String> currencyCodes = new ArrayList<>(); // ??????
            for (ApprovalBiddingItemImport approvalBiddingItemImport : approvalBiddingItemImports) {
                if (StringUtil.notEmpty(approvalBiddingItemImport.getOrgName())) {
                    orgNames.add(approvalBiddingItemImport.getOrgName().trim());
                }
                if (StringUtil.notEmpty(approvalBiddingItemImport.getOrganizationName())) {
                    orgNames.add(approvalBiddingItemImport.getOrganizationName().trim());
                }
                if (StringUtil.notEmpty(approvalBiddingItemImport.getItemCode())) {
                    itemCodes.add(approvalBiddingItemImport.getItemCode().trim());
                }
                if (StringUtil.notEmpty(approvalBiddingItemImport.getVendorCode())) {
                    vendorCodes.add(approvalBiddingItemImport.getVendorCode().trim());
                }
                if (StringUtil.notEmpty(approvalBiddingItemImport.getCurrencyCode())) {
                    currencyCodes.add(approvalBiddingItemImport.getCurrencyCode().trim());
                }
            }

            Map<String, Organization> orgMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(orgNames)) {
                orgNames = orgNames.stream().distinct().collect(Collectors.toList());
                List<Organization> organizationByNameList = baseClient.getOrganizationByNameList(orgNames);
                if (CollectionUtils.isNotEmpty(organizationByNameList)) {
                    orgMap = organizationByNameList.stream().collect(Collectors.toMap(Organization::getOrganizationName, Function.identity(), (k1, k2) -> k1));
                }
            }

            Map<String, MaterialItem> itemMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(itemCodes)) {
                itemCodes = itemCodes.stream().distinct().collect(Collectors.toList());
                List<MaterialItem> materialItems = baseClient.listMaterialByCodeBatch(itemCodes);
                if (CollectionUtils.isNotEmpty(materialItems)) {
                    itemMap = materialItems.stream().collect(Collectors.toMap(MaterialItem::getMaterialCode, Function.identity(), (k1, k2) -> k1));
                }
            }

            Map<String, CompanyInfo> companyInfoMap = supplierClient.getComponyByCodeList(vendorCodes);

            Map<String, PurchaseTax> taxMap = new HashMap<>();
            List<PurchaseTax> purchaseTaxes = baseClient.listTaxAll();
            if (CollectionUtils.isNotEmpty(purchaseTaxes)) {
                taxMap = purchaseTaxes.stream().collect(Collectors.toMap(PurchaseTax::getTaxKey, Function.identity(), (k1, k2) -> k1));
            }

            Map<String, PurchaseCurrency> currencyMap = new HashMap<>();
            List<PurchaseCurrency> purchaseCurrencies = baseClient.listAllPurchaseCurrency();
            if (CollectionUtils.isNotEmpty(purchaseCurrencies)) {
                currencyMap = purchaseCurrencies.stream().collect(Collectors.toMap(PurchaseCurrency::getCurrencyCode, Function.identity(), (k1, k2) -> k1));
            }

            for (ApprovalBiddingItemImport approvalBiddingItemImport : approvalBiddingItemImports) {
                StringBuffer errorMsg = new StringBuffer();
                ApprovalBiddingItem approvalBiddingItem = new ApprovalBiddingItem();

                // ???????????? ?????????STANDARD
                approvalBiddingItem.setPriceType("STANDARD");

                // ????????????
                String orgName = approvalBiddingItemImport.getOrgName();
                if (StringUtil.notEmpty(orgName)) {
                    orgName = orgName.trim();
                    Organization organization = orgMap.get(orgName);
                    if (null != organization) {
                        approvalBiddingItem.setOrgId(organization.getOrganizationId());
                        approvalBiddingItem.setOrgCode(organization.getOrganizationCode());
                        approvalBiddingItem.setOrgName(organization.getOrganizationName());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ????????????
                String organizationName = approvalBiddingItemImport.getOrganizationName();
                if (StringUtil.notEmpty(organizationName)) {
                    organizationName = organizationName.trim();
                    Organization organization = orgMap.get(organizationName);
                    if (null != organization) {
                        if (StringUtil.notEmpty(approvalBiddingItem.getOrgId())) {
                            if (organization.getParentOrganizationIds().contains(String.valueOf(approvalBiddingItem.getOrgId()))) {
                                approvalBiddingItem.setOrganizationId(organization.getOrganizationId());
                                approvalBiddingItem.setOrganizationCode(organization.getOrganizationCode());
                                approvalBiddingItem.setOrganizationName(organization.getOrganizationName());
                            } else {
                                errorFlag.set(true);
                                errorMsg.append("????????????????????????????????????????????????; ");
                            }
                        } else {
                            approvalBiddingItem.setOrganizationId(organization.getOrganizationId());
                            approvalBiddingItem.setOrganizationCode(organization.getOrganizationCode());
                            approvalBiddingItem.setOrganizationName(organization.getOrganizationName());
                        }
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ????????????
                String itemCode = approvalBiddingItemImport.getItemCode();
                if (StringUtil.notEmpty(itemCode)) {
                    itemCode = itemCode.trim();
                    MaterialItem materialItem = itemMap.get(itemCode);
                    if (null != materialItem) {
                        approvalBiddingItem.setItemId(materialItem.getMaterialId());
                        approvalBiddingItem.setItemCode(materialItem.getMaterialCode());
                        approvalBiddingItem.setItemName(materialItem.getMaterialName());
                        approvalBiddingItem.setItemDescription(materialItem.getMaterialName());
                        approvalBiddingItem.setCategoryId(materialItem.getCategoryId());
                        approvalBiddingItem.setCategoryCode(materialItem.getCategoryCode());
                        approvalBiddingItem.setCategoryName(materialItem.getCategoryName());
                        approvalBiddingItem.setUnit(materialItem.getUnit());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ???????????????
                String vendorCode = approvalBiddingItemImport.getVendorCode();
                if (StringUtil.notEmpty(vendorCode)) {
                    CompanyInfo companyInfo = companyInfoMap.get(vendorCode);
                    if (null != companyInfo) {
                        approvalBiddingItem.setVendorId(companyInfo.getCompanyId());
                        approvalBiddingItem.setVendorCode(companyInfo.getCompanyCode());
                        approvalBiddingItem.setVendorName(companyInfo.getCompanyName());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("?????????????????????????????????; ");
                    }
                }

                // ????????????
                approvalBiddingItem.setArrivalPlace(approvalBiddingItemImport.getArrivalPlace());

                // ?????????
                String taxPrice = approvalBiddingItemImport.getTaxPrice();
                if (StringUtil.notEmpty(taxPrice)) {
                    taxPrice = taxPrice.trim();
                    if (StringUtil.isDigit(taxPrice)) {
                        approvalBiddingItem.setTaxPrice(new BigDecimal(taxPrice));
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("?????????????????????; ");
                    }
                }

                // ??????
                String taxKey = approvalBiddingItemImport.getTaxKey();
                if (StringUtil.notEmpty(taxKey)) {
                    taxKey = taxKey.trim();
                    PurchaseTax purchaseTax = taxMap.get(taxKey);
                    if (null != purchaseTax) {
                        approvalBiddingItem.setTaxKey(purchaseTax.getTaxKey());
                        approvalBiddingItem.setTaxRate(purchaseTax.getTaxCode());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("????????????????????????; ");
                    }
                }

                // ????????????
                String currencyCode = approvalBiddingItemImport.getCurrencyCode();
                if (StringUtil.notEmpty(currencyCode)) {
                    currencyCode = currencyCode.trim();
                    PurchaseCurrency purchaseCurrency = currencyMap.get(currencyCode);
                    if (null != purchaseCurrency) {
                        approvalBiddingItem.setCurrencyId(purchaseCurrency.getCurrencyId());
                        approvalBiddingItem.setCurrencyCode(purchaseCurrency.getCurrencyCode());
                        approvalBiddingItem.setCurrencyName(purchaseCurrency.getCurrencyName());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ????????????
                String startTime = approvalBiddingItemImport.getStartTime();
                if (StringUtil.notEmpty(startTime)) {
                    startTime = startTime.trim();
                    try {
                        Date date = DateUtil.parseDate(startTime);
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        approvalBiddingItem.setStartTime(localDate);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("???????????????????????????????????????; ");
                    }

                }

                // ??????????????????
                String endTime = approvalBiddingItemImport.getEndTime();
                if (StringUtil.notEmpty(endTime)) {
                    endTime = endTime.trim();
                    try {
                        Date date = DateUtil.parseDate(endTime);
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        approvalBiddingItem.setEndTime(localDate);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("???????????????????????????????????????; ");
                    }

                }

                // ????????????
                String needNum = approvalBiddingItemImport.getNeedNum();
                if (StringUtil.notEmpty(needNum)) {
                    needNum = needNum.trim();
                    if (StringUtil.isDigit(needNum)) {
                        approvalBiddingItem.setNeedNum(new BigDecimal(needNum));
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("????????????????????????; ");
                    }
                }

                // ???????????????
                String minOrderQuantity = approvalBiddingItemImport.getMinOrderQuantity();
                if (StringUtil.notEmpty(minOrderQuantity)) {
                    minOrderQuantity = minOrderQuantity.trim();
                    if (StringUtil.isDigit(minOrderQuantity)) {
                        approvalBiddingItem.setMinOrderQuantity(new BigDecimal(minOrderQuantity));
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("???????????????????????????; ");
                    }
                }

                // L/T
                String lAndT = approvalBiddingItemImport.getLAndT();
                if (StringUtil.notEmpty(lAndT)) {
                    lAndT = lAndT.trim();
                    approvalBiddingItem.setLAndT(lAndT);
                }

                if (errorMsg.length() > 0) {
                    approvalBiddingItemImport.setErrorMsg(errorMsg.toString());
                } else {
                    approvalBiddingItem.setApprovalBiddingItemId(IdGenrator.generate());
                    approvalBiddingItems.add(approvalBiddingItem);
                    approvalBiddingItemImport.setErrorMsg(null);
                }
            }

        }
    }

    @Override
    public void insertPriceApproval(InsertPriceApprovalDTO insertPriceApprovalDTO) {
        this.save(insertPriceApprovalDTO.getApprovalHeader());
        iApprovalItemService.saveBatch(insertPriceApprovalDTO.getApprovalItemList());
    }

    @Override
    public BigDecimal getApprovalNetPrice(NetPriceQueryDTO netPriceQueryDTO) {
        List<ApprovalItem> approvalItemList = approvalItemMapper.queryApprovalItemList(netPriceQueryDTO);
        return approvalItemList.size() > 0 ? approvalItemList.get(0).getNotaxCurrentPrice() : null;
    }

    /**
     * ??????
     *
     * @param insertPriceApprovalDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePriceApproval(InsertPriceApprovalDTO insertPriceApprovalDTO) {
        log.info("savePriceApproval");
        ApprovalHeader approvalHeader = insertPriceApprovalDTO.getApprovalHeader();
        if (Objects.isNull(approvalHeader)) {
            throw new BaseException("???????????????????????????");
        }
        List<ApprovalBiddingItemVO> approvalBiddingItemList = insertPriceApprovalDTO.getApprovalBiddingItemList();
        /*???????????????*/
        approvalHeader.setStatus(PriceApprovalStatus.DRAFT.getValue());
        boolean isNew = Objects.isNull(approvalHeader.getApprovalHeaderId());
        if (isNew) {
            approvalHeader.setApprovalHeaderId(IdGenrator.generate());
            approvalHeader.setApprovalNo(baseClient.seqGen(SequenceCodeConstant.SEQ_INQ_APPROVAL_NO));
            this.save(approvalHeader);
        } else {
            this.updateById(approvalHeader);
        }

        Long approvalHeaderId = approvalHeader.getApprovalHeaderId();
        /*????????????*/
        List<ApprovalFile> approvalFiles1 = insertPriceApprovalDTO.getApprovalFiles();
        List<ApprovalFile> approvalFiles = approvalFiles1.stream().filter(e -> Objects.nonNull(e.getFileRelationId())).collect(Collectors.toList());
        List<ApprovalFile> saveFileList = new LinkedList<>();
        List<ApprovalFile> updateFileList = new LinkedList<>();
        if (!isNew) {
            List<Long> waitToDeleteFiles = iApprovalFileService.list(Wrappers.lambdaQuery(ApprovalFile.class)
                    .select(ApprovalFile::getApprovalFileId)
                    .eq(ApprovalFile::getApprovalHeaderId, approvalHeaderId))
                    .stream().map(ApprovalFile::getApprovalFileId).collect(Collectors.toList());
            for (int i = waitToDeleteFiles.size() - 1; i >= 0; i--) {
                boolean shouldDelete = true;
                Long currentId = waitToDeleteFiles.get(i);
                for (ApprovalFile approvalFile : approvalFiles) {
                    if (Objects.equals(approvalFile.getApprovalFileId(), currentId)) {
                        shouldDelete = false;
                        break;
                    }
                }
                if (!shouldDelete) {
                    waitToDeleteFiles.remove(i);
                }
            }
            if (CollectionUtils.isNotEmpty(waitToDeleteFiles)) {
                iApprovalFileService.removeByIds(waitToDeleteFiles);
            }
        }


        if (CollectionUtils.isNotEmpty(approvalFiles)) {
            for (ApprovalFile approvalFile : approvalFiles) {
                approvalFile.setApprovalHeaderId(approvalHeaderId);
                if (Objects.isNull(approvalFile.getApprovalFileId())) {
                    approvalFile.setApprovalFileId(IdGenrator.generate());
                    saveFileList.add(approvalFile);
                } else {
                    updateFileList.add(approvalFile);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(saveFileList)) {
            iApprovalFileService.saveBatch(saveFileList);
        }
        if (CollectionUtils.isNotEmpty(updateFileList)) {
            iApprovalFileService.updateBatchById(updateFileList);
        }


        /*???????????????*/
        if (!isNew) {
            List<Long> waitToDeleteApprovalBidingItemIds = iApprovalBiddingItemService.list(Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                    .select(ApprovalBiddingItem::getApprovalBiddingItemId)
                    .eq(ApprovalBiddingItem::getApprovalHeaderId, approvalHeaderId))
                    .stream().map(ApprovalBiddingItem::getApprovalBiddingItemId).collect(Collectors.toList());
            for (int i = waitToDeleteApprovalBidingItemIds.size() - 1; i >= 0; i--) {
                boolean shouldDelete = true;
                Long currentId = waitToDeleteApprovalBidingItemIds.get(i);
                for (ApprovalBiddingItemVO approvalBiddingItemVO : approvalBiddingItemList) {
                    if (Objects.equals(approvalBiddingItemVO.getApprovalBiddingItemId(), currentId)) {
                        shouldDelete = false;
                        break;
                    }
                }
                if (!shouldDelete) {
                    waitToDeleteApprovalBidingItemIds.remove(i);
                }
            }
            //????????????????????????
            if (CollectionUtils.isNotEmpty(waitToDeleteApprovalBidingItemIds)) {
                approvalBiddingItemMapper.deleteBatchIds(waitToDeleteApprovalBidingItemIds);
                //???????????????????????????
                approvalBiddingItemPaymentTermService.remove(Wrappers.lambdaQuery(ApprovalBiddingItemPaymentTerm.class)
                        .in(ApprovalBiddingItemPaymentTerm::getApprovalBiddingItemId, waitToDeleteApprovalBidingItemIds));
            }
        }
        //??????&??????
        List<ApprovalBiddingItem> saveApprovalBidingItemList = new LinkedList<>();
        List<ApprovalBiddingItem> updateApprovalBidingItemList = new LinkedList<>();
        List<ApprovalBiddingItemPaymentTerm> savePaymentTermList = new LinkedList<>();
        List<Long> updateApprovalBidingIds = new LinkedList<>();
        //???????????????
        Set<Long> ouIds = approvalBiddingItemList.stream().map(ApprovalBiddingItemVO::getOuId).collect(Collectors.toSet());
        Map<Long, BaseOuGroupDetailVO> ouGroupMap = baseClient.queryOuInfoDetailByIds(ouIds).stream().collect(Collectors.toMap(BaseOuGroupDetailVO::getOuGroupId, Function.identity()));
        for (ApprovalBiddingItemVO currentItem : approvalBiddingItemList) {
            //??????????????????
            if (Objects.isNull(currentItem.getNotaxPrice())) {
                if (Objects.nonNull(currentItem.getTaxPrice()) && Objects.nonNull(currentItem.getTaxRate())) {
                    currentItem.setNotaxPrice(calculateNoTaxPrice(currentItem.getTaxPrice(), currentItem.getTaxRate().multiply(BigDecimal.valueOf(0.01))));
                }
            }
//????????????ou??????????????????

            List<ApprovalBiddingItemPaymentTerm> terms = currentItem.getApprovalBiddingItemPaymentTermList();

            if (Objects.isNull(currentItem.getApprovalBiddingItemId())) {
                long itemId = IdGenrator.generate();
                currentItem.setApprovalBiddingItemId(itemId);
                currentItem.setApprovalHeaderId(approvalHeader.getApprovalHeaderId());
                //???ou???????????????
                if (Objects.nonNull(currentItem.getOuId())) {
                    BaseOuGroupDetailVO baseOuGroupDetailVOS = ouGroupMap.get(currentItem.getOuId());
                    List<ApprovalBiddingItem> items = new LinkedList<>();
                    for (BaseOuDetailVO baseOuGroupDetailVO : baseOuGroupDetailVOS.getDetails()) {
                        ApprovalBiddingItemVO sub = BeanCopyUtil.copyProperties(currentItem, ApprovalBiddingItemVO::new);
                        sub.setOrgId(baseOuGroupDetailVO.getOuId())
                                .setOrgCode(baseOuGroupDetailVO.getOuCode())
                                .setOrgName(baseOuGroupDetailVO.getOuName())
                                .setOrganizationId(baseOuGroupDetailVO.getInvId())
                                .setOrganizationCode(baseOuGroupDetailVO.getInvCode())
                                .setOrganizationName(baseOuGroupDetailVO.getInvName());
                        sub.setOuId(null);
                        sub.setOuName(null);
                        sub.setOuNumber(null);
                        List<ApprovalBiddingItemPaymentTerm> tempTerms = terms.stream().map(e -> BeanCopyUtil.copyProperties(e, ApprovalBiddingItemPaymentTerm::new)).collect(Collectors.toList());
                        sub.setApprovalBiddingItemPaymentTermList(tempTerms);
                        items.add(sub);
                    }
                    String subJson = JSON.toJSONString(items);
                    currentItem.setOuGroupJson(subJson);
                }
                saveApprovalBidingItemList.add(currentItem);
            } else {
                updateApprovalBidingItemList.add(currentItem);
                updateApprovalBidingIds.add(currentItem.getApprovalBiddingItemId());
            }
            if (CollectionUtils.isNotEmpty(terms)) {
                terms.forEach(e -> {
                    e.setApprovalBiddingItemPaymentTermId(IdGenrator.generate());
                    e.setApprovalBiddingItemId(currentItem.getApprovalBiddingItemId());
                });
                savePaymentTermList.addAll(terms);
            }

        }
        //????????????
        Set<Long> categoryID = approvalBiddingItemList.stream().map(ApprovalBiddingItemVO::getCategoryId).collect(Collectors.toSet());

        //??????
        Map<Long, PurchaseCategory> map = baseClient.listCategoryByIds(categoryID.stream().collect(Collectors.toList())).stream().collect(Collectors.toMap(PurchaseCategory::getCategoryId, Function.identity()));
        Map<String, PurchaseCurrency> codeCurrency = baseClient.listCurrencyAll().stream().collect(Collectors.toMap(PurchaseCurrency::getCurrencyCode, Function.identity()));
        if (CollectionUtils.isNotEmpty(saveApprovalBidingItemList)) {
            for (ApprovalBiddingItem approvalBiddingItem : saveApprovalBidingItemList) {
                PurchaseCurrency purchaseCurrency = codeCurrency.get(approvalBiddingItem.getCurrencyCode());
                approvalBiddingItem.setCurrencyId(purchaseCurrency.getCurrencyId())
                        .setCurrencyName(purchaseCurrency.getCurrencyName());
                PurchaseCategory purchaseCategory = map.get(approvalBiddingItem.getCategoryId());
                approvalBiddingItem.setCategoryCode(purchaseCategory.getCategoryCode())
                        .setCurrencyName(purchaseCurrency.getCurrencyName());
            }
            iApprovalBiddingItemService.saveBatch(saveApprovalBidingItemList);
        }

        if (CollectionUtils.isNotEmpty(updateApprovalBidingItemList)) {
            for (ApprovalBiddingItem approvalBiddingItem : saveApprovalBidingItemList) {
                PurchaseCurrency purchaseCurrency = codeCurrency.get(approvalBiddingItem.getCurrencyCode());
                approvalBiddingItem.setCurrencyId(purchaseCurrency.getCurrencyId())
                        .setCurrencyName(purchaseCurrency.getCurrencyName());
                PurchaseCategory purchaseCategory = map.get(approvalBiddingItem.getCategoryId());
                approvalBiddingItem.setCategoryCode(purchaseCategory.getCategoryCode())
                        .setCurrencyName(purchaseCurrency.getCurrencyName());
            }
            iApprovalBiddingItemService.updateBatchById(updateApprovalBidingItemList);
        }

        //?????????????????????????????????
        if (CollectionUtils.isNotEmpty(updateApprovalBidingIds)) {
            approvalBiddingItemPaymentTermService.remove(Wrappers.lambdaQuery(ApprovalBiddingItemPaymentTerm.class)
                    .in(ApprovalBiddingItemPaymentTerm::getApprovalBiddingItemId, updateApprovalBidingIds));
        }

        if (CollectionUtils.isNotEmpty(savePaymentTermList)) {
            //?????????
            approvalBiddingItemPaymentTermService.saveBatch(savePaymentTermList);
        }
        return approvalHeaderId;
    }


    /*?????????????????????*/
    public void checkIfSave(InsertPriceApprovalDTO insertPriceApprovalDTO) {
        List<ApprovalBiddingItemVO> items = insertPriceApprovalDTO.getApprovalBiddingItemList();
        if (CollectionUtils.isNotEmpty(items)) {
            for (int i = 0; i < items.size(); i++) {
                ApprovalBiddingItemVO current = items.get(i);
                String prefix = String.format("???%d??????", i + 1);
                ObjectUtil.validate(prefix, current);
                if (Objects.isNull(current.getOrgId()) && Objects.isNull(current.getOuId())) {
                    throw new BaseException(prefix + "????????????id???ou??????????????????");
                }
                if (CollectionUtils.isEmpty(current.getApprovalBiddingItemPaymentTermList())) {
                    throw new BaseException(prefix + "????????????????????????");
                }
                if (Objects.isNull(current.getOrgName()) && Objects.isNull(current.getOuName())) {
                    throw new BaseException(prefix + "????????????id???ou??????????????????");
                }
                if (current.getEndTime().isBefore(current.getStartTime())) {
                    throw new BaseException(prefix + "??????????????????????????????????????????");
                }
            }
        }
    }

    /**
     * ????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ceeaSubmit(InsertPriceApprovalDTO insertPriceApprovalDTO) {
        List<ApprovalBiddingItemVO> approvalBiddingItemList = insertPriceApprovalDTO.getApprovalBiddingItemList();
        if (CollectionUtils.isNotEmpty(approvalBiddingItemList)) {
            approvalBiddingItemList.forEach(approvalBiddingItemVO -> {
                LocalDate startTime = approvalBiddingItemVO.getStartTime();
                LocalDate endTime = approvalBiddingItemVO.getEndTime();
                if (null != startTime && null != endTime) {
                    if (startTime.isAfter(endTime)) {
                        throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????"));
                    }
                } else {
                    throw new BaseException("?????????????????????????????????");
                }
            });
        }
        log.debug("submitPriceApproval");
        /*???????????????????????????*/
        checkIfSubmit(insertPriceApprovalDTO);
        //??????????????????
        savePriceApproval(insertPriceApprovalDTO);
        //????????????,????????????????????????id,?????????????????????
        update(Wrappers.lambdaUpdate(ApprovalHeader.class)
                .set(ApprovalHeader::getStatus, PriceApprovalStatus.RESULT_NOT_APPROVED.getValue())
                .eq(ApprovalHeader::getApprovalHeaderId, insertPriceApprovalDTO.getApprovalHeader().getApprovalHeaderId()));

        // ?????????????????????????????????
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //todo????????????OA??????

                /* Begin by chenwt24@meicloud.com   2020-10-16 */

                String formId = null;
                try {
                    formId = priceFlow.submitPriceConfFlow(insertPriceApprovalDTO);
                } catch (Exception e) {
                    throw new BaseException(e.getMessage());
                }
                if (StringUtils.isEmpty(formId)) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("??????OA????????????"));

                }
            }
        });
    }


    /**
     * ?????????????????????(??????????????????????????????)
     *
     * @param approvalHeadId
     * @return
     */
    @Override
    public ApprovalAllVo ceeaGetApprovalDetail(Long approvalHeadId) {
        ApprovalHeader approvalHeader = approvalHeaderMapper.selectById(approvalHeadId);
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("APPROVAL_HEADER_ID", approvalHeadId);
        List<ApprovalFile> approvalFileList = approvalFileMapper.selectList(wrapper);
        List<ApprovalBiddingItem> approvalBiddingItemList = approvalBiddingItemMapper.selectList(wrapper);
        List<ApprovalBiddingItemVO> vos = approvalBiddingItemList.stream().map(e -> BeanCopyUtil.copyProperties(e, ApprovalBiddingItemVO::new)).collect(Collectors.toList());
        Map<Long, ApprovalBiddingItemVO> voMap = vos.stream().collect(Collectors.toMap(ApprovalBiddingItemVO::getApprovalBiddingItemId, Function.identity()));
        if (!org.springframework.util.CollectionUtils.isEmpty(voMap)) {
            Map<Long, List<ApprovalBiddingItemPaymentTerm>> payment = approvalBiddingItemPaymentTermService.list(Wrappers.lambdaQuery(ApprovalBiddingItemPaymentTerm.class)
                    .in(ApprovalBiddingItemPaymentTerm::getApprovalBiddingItemId, voMap.keySet())
            ).stream().collect(Collectors.groupingBy(ApprovalBiddingItemPaymentTerm::getApprovalBiddingItemId));
            //??????????????????
            payment.forEach((k, v) -> {
                ApprovalBiddingItemVO vo = voMap.get(k);
                vo.setApprovalBiddingItemPaymentTermList(v);
            });
        }
        User user = rbacClient.getUser(new User().setUserId(approvalHeader.getCreatedId()));
        return new ApprovalAllVo()
                .setApprovalHeader(approvalHeader)
                .setApprovalFileList(approvalFileList)
                .setApprovalBiddingItemList(vos)
                .setCreateByDept(user.getDepartment())
                .setCreateByName(user.getNickname());
    }

    /**
     * @param ceeaSourceNo
     * @return
     */
    @Override
    public ApprovalVo getApprovalDetails(String ceeaSourceNo) {
        List<ApprovalHeader> headers = approvalHeaderService.list(Wrappers.lambdaQuery(ApprovalHeader.class)
                .eq(ApprovalHeader::getCeeaSourceNo, ceeaSourceNo)
        );
        if (headers.size() > 1) {
            throw new BaseException(String.format("????????????%s????????????????????????????????????????????????", ceeaSourceNo));
        }
        if (CollectionUtils.isNotEmpty(headers)) {
            //?????????
            ApprovalHeader approvalHeader = headers.get(0);

            QueryWrapper wrapper = new QueryWrapper();
            wrapper.eq("APPROVAL_HEADER_ID", approvalHeader.getApprovalHeaderId());
            //?????????
            List<ApprovalBiddingItem> approvalBiddingItemList = approvalBiddingItemMapper.selectList(Wrappers.lambdaQuery(
                    ApprovalBiddingItem.class)
                    .eq(ApprovalBiddingItem::getApprovalHeaderId, approvalHeader.getApprovalHeaderId())
            );
            return new ApprovalVo()
                    .setApprovalHeader(approvalHeader)
                    .setApprovalBiddingItemList(approvalBiddingItemList);
        }
        return new ApprovalVo();
    }


    private void checkIfSubmit(InsertPriceApprovalDTO insertPriceApprovalDTO) {
        Long approvalHeadId = insertPriceApprovalDTO.getApprovalHeader().getApprovalHeaderId();
        Assert.notNull(approvalHeadId, LocaleHandler.getLocaleMsg("???????????????ID????????????"));
        ApprovalHeader header = getById(approvalHeadId);
        Assert.notNull(header, LocaleHandler.getLocaleMsg("???????????????????????????"));
        if (!Objects.equals(header.getStatus(), PriceApprovalStatus.DRAFT.getValue()) &&
                !Objects.equals(header.getStatus(), PriceApprovalStatus.RESULT_REJECTED.getValue()) &&
                !Objects.equals(header.getStatus(), PriceApprovalStatus.WITHDRAW.getValue())) {
            throw new BaseException("?????????????????????????????????");
        }
        //???????????????????????????????????? 2020-11-30
        String priceBlackList = "PRICE_BLACK_LIST";
        Set<String> dictDTOS = baseClient.listDictItemByDictCode(priceBlackList).stream().map(
                d -> d.getDictItemCode()).collect(Collectors.toSet());
        List<ApprovalBiddingItemVO> inputBackMaterial = insertPriceApprovalDTO.getApprovalBiddingItemList()
                .stream().filter(m -> dictDTOS.contains(m.getItemCode())).collect(Collectors.toList());
        if (!inputBackMaterial.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            inputBackMaterial.stream().forEach(m -> sb.append("[").append(m.getItemName()).append("] "));
            throw new BaseException(sb.append("?????????????????????????????????????????????").toString());
        }
        if (Objects.equals(insertPriceApprovalDTO.getApprovalHeader().getCeeaIfUpdatePriceLibrary(), YesOrNo.YES.getValue())) {
            checkIfOverLapping(insertPriceApprovalDTO.getApprovalBiddingItemList());
        }
    }


    /**
     * ?????? ???????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? ????????????
     * ?????????????????????????????????????????????????????????
     *
     * @param approvalBiddingItemList
     */
    private void checkIfOverLapping(List<ApprovalBiddingItemVO> approvalBiddingItemList) {
        Map<String, List<ApprovalBiddingItemVO>> map = getMap(approvalBiddingItemList);
        for (Map.Entry<String, List<ApprovalBiddingItemVO>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<ApprovalBiddingItemVO> approvalBiddingItemVOList = map.get(key);
            checkDateIfOverLapping(approvalBiddingItemVOList);
        }
    }


    /**
     * ??????
     * ???????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ????????????
     * ??????key???
     *
     * @param approvalBiddingItemVOList
     * @return
     */
    private Map<String, List<ApprovalBiddingItemVO>> getMap(List<ApprovalBiddingItemVO> approvalBiddingItemVOList) {
        Long headerId = Optional.ofNullable(approvalBiddingItemVOList.stream().filter(e -> Objects.nonNull(e.getApprovalHeaderId())).findAny().get()).orElseThrow(() -> new BaseException("???????????????????????????id")).getApprovalHeaderId();
        ApprovalHeader header = approvalHeaderMapper.selectById(headerId);
        Set<String> updateCategorySet = new HashSet<>();
        boolean inPrice = Objects.equals(header.getCeeaIfUpdatePriceLibrary(), "Y");
        //?????????????????????
        if (inPrice) {
            List<DictItem> priceUpdateCategary = baseClient.listDictItemByDictCode("PRICE_UPDATE_CATEGARY");
            updateCategorySet = priceUpdateCategary.stream().map(DictItem::getDictItemCode).collect(Collectors.toSet());
        }
        Map<String, List<ApprovalBiddingItemVO>> result = new HashMap();
        for (int i = 0; i < approvalBiddingItemVOList.size(); i++) {
            ApprovalBiddingItemVO approvalBiddingItemVO = approvalBiddingItemVOList.get(i);
            StringBuffer stringBuffer = new StringBuffer();
            if (!inPrice) {
                stringBuffer.append(approvalBiddingItemVO.getItemName());
            }
            String key = stringBuffer.append(inPrice && updateCategorySet.contains(approvalBiddingItemVO.getCategoryCode()) ? approvalBiddingItemVO.getItemName() : "")
                    .append(approvalBiddingItemVO.getItemCode())
                    .append(approvalBiddingItemVO.getVendorCode())
                    .append(approvalBiddingItemVO.getOrgId())
                    .append(approvalBiddingItemVO.getOrganizationId())
                    .append(approvalBiddingItemVO.getArrivalPlace())
                    .append(approvalBiddingItemVO.getPriceType()).toString();
            if (result.get(key) == null) {
                result.put(key, new ArrayList<ApprovalBiddingItemVO>() {{
                    add(approvalBiddingItemVO);
                }});
            } else {
                result.get(key).add(approvalBiddingItemVO);
            }
        }
        return result;
    }

    private void checkDateIfOverLapping(List<ApprovalBiddingItemVO> approvalBiddingItemList) {
        for (int i = 0; i < approvalBiddingItemList.size(); i++) {
            ApprovalBiddingItemVO approvalBiddingItemVO1 = approvalBiddingItemList.get(i);
            for (int j = i + 1; j < approvalBiddingItemList.size(); j++) {
                ApprovalBiddingItemVO approvalBiddingItemVO2 = approvalBiddingItemList.get(j);
                if (ifDateOver(approvalBiddingItemVO1, approvalBiddingItemVO2)) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????"));
                }

            }
        }

    }

    private boolean ifDateOver(ApprovalBiddingItemVO approvalBiddingItemVO1, ApprovalBiddingItemVO approvalBiddingItemVO2) {
        LocalDate effectiveDate1 = approvalBiddingItemVO1.getStartTime();
        LocalDate expirationDate1 = approvalBiddingItemVO1.getEndTime();

        LocalDate effectiveDate2 = approvalBiddingItemVO2.getStartTime();
        LocalDate expirationDate2 = approvalBiddingItemVO2.getEndTime();

        if (effectiveDate1.equals(effectiveDate2) && expirationDate1.equals(expirationDate2)) {
            // ??????
            BigDecimal taxPrice = approvalBiddingItemVO1.getTaxPrice();
            BigDecimal taxPrice1 = approvalBiddingItemVO2.getTaxPrice();
            if (!taxPrice.equals(taxPrice1)) {
                throw new BaseException("?????????????????????,??????????????????");
            }
        } else {
            /*?????????*/
            if (isBeforeAndEquals(effectiveDate1, effectiveDate2) &&
                    isBeforeAndEquals(effectiveDate2, expirationDate1) &&
                    isBeforeAndEquals(expirationDate1, expirationDate2)
            ) {
                log.info("???????????????-?????????,[" + effectiveDate1 + "," + expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
                return true;
            }
            /*?????????*/
            if (isBeforeAndEquals(effectiveDate2, effectiveDate1) &&
                    isBeforeAndEquals(effectiveDate1, expirationDate2) &&
                    isBeforeAndEquals(expirationDate2, expirationDate1)
            ) {
                log.info("???????????????-?????????,[" + effectiveDate1 + "," + expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
                return true;
            }
            /*??????*/
            if (isBeforeAndEquals(effectiveDate1, effectiveDate2) &&
                    isBeforeAndEquals(expirationDate2, expirationDate1)
            ) {
                log.info("???????????????-??????,[" + effectiveDate1 + "," + expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
                return true;
            }
            /*?????????*/
            if (isBeforeAndEquals(effectiveDate2, effectiveDate1) &&
                    isBeforeAndEquals(expirationDate1, expirationDate2)
            ) {
                log.info("???????????????-?????????,[" + effectiveDate1 + "," + expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
                return true;
            }
        }
        return false;

    }

    /**
     * @param localDate1
     * @param localDate2
     * @return
     */
    public boolean isBeforeAndEquals(LocalDate localDate1, LocalDate localDate2) {
        return localDate1.isBefore(localDate2) || localDate1.isEqual(localDate2);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generatePriceApproval(Long inquiryId) {

        QueryWrapper<ApprovalHeader> wrapper = new QueryWrapper<>();

        wrapper.eq("BUSINESS_ID", inquiryId).eq("STATUS", PriceApprovalStatus.RESULT_NOT_APPROVED.getValue());
        ApprovalHeader header = getOne(wrapper);
        if (header != null) {
            throw new BaseException("????????????????????????????????????");
        }

        /*?????????????????????*/
        List<QuoteSelectedResult> quoteItems = this.baseMapper.queryQuoteSelected(inquiryId);
        if (CollectionUtils.isEmpty(quoteItems)) {
            throw new BaseException("?????????????????????");
        }
        /*??????????????????*/
        String approvalNo = baseClient.seqGen(SequenceCodeConstant.SEQ_INQ_APPROVAL_NO);
        if (StringUtils.isBlank(approvalNo)) {
            throw new BaseException("????????????????????????");
        }
        /*???????????????*/
        List<Long> quoteItemIds = new ArrayList<>();
        quoteItems.forEach(quoteSelectedResult -> {
            if (YesOrNo.YES.getValue().equals(quoteSelectedResult.getIsLadder())) {
                quoteItemIds.add(quoteSelectedResult.getQuoteItemId());
            }
        });
        List<QuoteLadderPrice> ladderPrices = null;
        if (CollectionUtils.isNotEmpty(quoteItemIds)) {
            ladderPrices = iQuoteLadderPriceService.getLadderPrice(quoteItemIds);
        }
        /*???????????????*/
        generateApproval(approvalNo, quoteItems, ladderPrices);
        /*??????????????????*/
        updateSelectionStatus(quoteItems);
    }

    @Override
    public PageInfo<ApprovalHeaderQueryResponseDTO> pageQuery(ApprovalHeaderQueryRequestDTO request) {
        ApprovalHeaderParam param = new ApprovalHeaderParam();
        BeanUtils.copyProperties(request, param);
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        PageHelper.startPage(pageNum, pageSize);
        List<ApprovalHeaderResult> results = this.baseMapper.queryByParam(param);
        return ApprovalHeaderConvertor.convert(results);
    }

    @Override
    @AuthData(module = MenuEnum.INQUIRY_APPROVAL_FLOW)
    public PageInfo<ApprovalHeader> ceeaListPage(ApprovalHeaderQueryRequestDTO request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        return new PageInfo<ApprovalHeader>(approvalHeaderMapper.ceeaFindList(request));
    }

    @Override
    public ApprovalHeaderDetailResponseDTO getApprovalDetail(Long approvalHeaderId, Long menuId) {
        ApprovalHeader approvalHeader = getById(approvalHeaderId);
        Header inquiryHeader = null;
        if (SourceType.INQUIRY.getValue().equals(approvalHeader.getSourceType())) {
            inquiryHeader = iHeaderService.getById(approvalHeader.getBusinessId());
        }
        List<ApprovalItem> approvalItems = iApprovalItemService.queryByApprovalHeaderId(approvalHeaderId);

        List<Long> approvalItemIds = new ArrayList<>();
        approvalItems.forEach(approvalItem -> {
            if (YesOrNo.YES.getValue().equals(approvalItem.getIsLadder())) {
                approvalItemIds.add(approvalItem.getApprovalItemId());
            }
        });
        List<ApprovalLadderPrice> ladderPrice = null;
        if (CollectionUtils.isNotEmpty(approvalItemIds)) {
            ladderPrice = iApprovalLadderPriceService.getLadderPrice(approvalItemIds);
        }

        List<ApprovalFile> approvalFiles = iApprovalFileService.getByApprovalHeadId(approvalHeaderId);
        /*?????????????????????????????????????????????*/
        Map<String, Object> initProcess = null;
        if (menuId != null) {
            Permission permission = rbacClient.getMenu(menuId);
            boolean flowEnable = workFlowFeign.getFlowEnable(menuId, permission.getFunctionId(), CbpmFormTemplateIdEnum.INQUIRY_APPROVAL_FLOW.getKey());
            if (flowEnable && StringUtils.isBlank(approvalHeader.getCbpmInstaceId())) {
                initProcess = workFlowFeign.initProcess(buildCbpmRquestParamDTO(CbpmFormTemplateIdEnum.INQUIRY_APPROVAL_FLOW.getKey(),
                        approvalHeader, approvalHeader.getCbpmInstaceId()));
            }
        }

        return ApprovalHeaderDetailConvertor.convert(approvalHeader, inquiryHeader, approvalItems, ladderPrice, approvalFiles, initProcess);
    }

    /**
     * ???????????????????????????
     */
    private CbpmRquestParamDTO buildCbpmRquestParamDTO(String templateCode, ApprovalHeader approvalHeader, String cbpmInstaceId) {
        CbpmRquestParamDTO cbpmRquestParam = new CbpmRquestParamDTO();
        cbpmRquestParam.setTemplateCode(templateCode);
        cbpmRquestParam.setBusinessId(String.valueOf(approvalHeader.getApprovalHeaderId()));
        cbpmRquestParam.setSubject(approvalHeader.getApprovalNo() + "?????????????????????");
        cbpmRquestParam.setFdId(cbpmInstaceId);
        return cbpmRquestParam;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPass(ApprovalHeader header) {
        /*??????????????????*/
        /*iPriceLibraryService.generatePriceLibrary(buildPriceLibraryParam(header));*/
        if (StringUtils.isNotBlank(header.getCeeaIfUpdatePriceLibrary()) && "Y".equals(header.getCeeaIfUpdatePriceLibrary())) {
            iPriceLibraryService.ceeaGeneratePriceLibrary(ceeaBuildPriceLibraryParam(header));
        }
        /*??????????????????*/
        header.setStatus(PriceApprovalStatus.RESULT_PASSED.getValue());
        updateById(header);

        // TODO ????????????????????????????????????, ????????????????????????
        List<ApprovalBiddingItem> approvalBiddingItems = iApprovalBiddingItemService.list(new QueryWrapper<>(new ApprovalBiddingItem().setApprovalHeaderId(header.getApprovalHeaderId())));
        if (CollectionUtils.isNotEmpty(approvalBiddingItems)) {
            List<ApprovalBiddingItem> approvalBiddingItemList = new ArrayList<>();
            approvalBiddingItems.forEach(approvalBiddingItem -> {
                Long fromContractLineId = approvalBiddingItem.getFromContractLineId();
                if (StringUtil.notEmpty(fromContractLineId)) {
                    // ???????????????????????????
                    approvalBiddingItemList.add(approvalBiddingItem);
                }
            });
            contractClient.priceApprovalWriteBackContract(approvalBiddingItemList);
        }
    }

    @Override
    public void reject(Long approvalHeaderId, String ceeaDrafterOpinion) {
        ApprovalHeader header = getById(approvalHeaderId);
        header.setStatus(PriceApprovalStatus.RESULT_REJECTED.getValue());
        header.setCeeaDrafterOpinion(ceeaDrafterOpinion);
        updateById(header);
    }

    /**
     * ????????????
     *
     * @param approvalHeaderId
     */
    @Transactional
    @Override
    public void abandon(Long approvalHeaderId) {
        ApprovalAllVo approvalAllVo = this.ceeaGetApprovalDetail(approvalHeaderId);
        ApprovalHeader approvalHeader = approvalAllVo.getApprovalHeader();
        Assert.notNull(approvalHeader, "?????????????????????????????????");
        String status = approvalHeader.getStatus();
        Assert.isTrue(PriceApprovalStatus.RESULT_REJECTED.getValue().equals(status) || PriceApprovalStatus.WITHDRAW.getValue().equals(status), "?????????????????????????????????");
        approvalHeader.setStatus(PriceApprovalStatus.ABANDONED.getValue());
        this.updateById(approvalHeader);
        SrmFlowBusWorkflow srmworkflowForm = baseClient.getSrmFlowBusWorkflow(approvalHeaderId);
        if (srmworkflowForm != null) {
            try {
                InsertPriceApprovalDTO insertPriceApprovalDTO = new InsertPriceApprovalDTO();
                insertPriceApprovalDTO.setApprovalHeader(approvalHeader).setProcessType("N");
                insertPriceApprovalDTO.setApprovalFiles(approvalAllVo.getApprovalFileList()).setApprovalBiddingItemList(approvalAllVo.getApprovalBiddingItemList());
                priceFlow.submitPriceConfFlow(insertPriceApprovalDTO);
            } catch (Exception e) {
                Assert.isTrue(false, "??????????????????????????????");
            }
        }
    }


    @Override
    public void withdraw(Long approvalHeaderId) {
        ApprovalHeader header = getById(approvalHeaderId);
        header.setStatus(PriceApprovalStatus.WITHDRAW.getValue());
        updateById(header);
    }

    /**
     * ?????????????????????-????????? todo
     *
     * @param approvalBiddingItemList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ceeaQuotaToPriceLibrary(List<ApprovalBiddingItem> approvalBiddingItemList) {
        List<PriceLibraryDTO> priceLibraryList = ceeaQuotaBuildPriceLibrary(approvalBiddingItemList);
        iPriceLibraryService.ceeaGeneratePriceLibrary(priceLibraryList);
    }

    /**
     * ????????????????????????
     */
    private List<PriceLibraryAddParam> buildPriceLibraryParam(ApprovalHeader header) {
        List<PriceLibraryAddParam> priceLibraryAddParams = new ArrayList<>();
        List<ApprovalItem> approvalItems = iApprovalItemService.queryByApprovalHeaderId(header.getApprovalHeaderId());
        approvalItems.forEach(approvalItem -> {
            PriceLibraryAddParam libraryAddParam = buildPriceLibraryAddParam(header, approvalItem);
            /*?????????*/
            List<PriceLibraryLadderPriceAddParam> ladderPriceAddParams = new ArrayList<>();
            if (YesOrNo.YES.getValue().equals(approvalItem.getIsLadder())) {
                List<ApprovalLadderPrice> ladderPrice =
                        iApprovalLadderPriceService.getLadderPrice(Lists.newArrayList(approvalItem.getApprovalItemId()));
                ladderPrice.forEach(approvalLadderPrice -> {
                    PriceLibraryLadderPriceAddParam ladderPriceAddParam = new PriceLibraryLadderPriceAddParam();
                    BeanUtils.copyProperties(approvalLadderPrice, ladderPriceAddParam);
                    ladderPriceAddParams.add(ladderPriceAddParam);
                });
            }

            libraryAddParam.setLadderPrices(ladderPriceAddParams);
            priceLibraryAddParams.add(libraryAddParam);
        });

        return priceLibraryAddParams;
    }

    private List<PriceLibraryDTO> ceeaQuotaBuildPriceLibrary(List<ApprovalBiddingItem> approvalBiddingItemList) {
        List<PriceLibraryDTO> priceLibraryList = new ArrayList<>();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        approvalBiddingItemList.forEach(item -> {
            PriceLibraryDTO priceLibrary = new PriceLibraryDTO();
            priceLibrary.setPriceLibraryId(IdGenrator.generate());
            priceLibrary.setItemId(item.getItemId());
            priceLibrary.setWarrantyPeriod(item.getWarrantyPeriod());
            priceLibrary.setItemCode(item.getItemCode());
            priceLibrary.setItemDesc(item.getItemName());
            priceLibrary.setCategoryId(item.getCategoryId());
            priceLibrary.setCategoryName(item.getCategoryName());
            priceLibrary.setVendorId(item.getVendorId());
            priceLibrary.setVendorCode(item.getVendorCode());
            priceLibrary.setVendorName(item.getVendorName());
            priceLibrary.setApprovalNo(item.getApprovalNo());
            priceLibrary.setPriceType(item.getPriceType());
            priceLibrary.setNotaxPrice(item.getNotaxPrice());
            priceLibrary.setTaxPrice(item.getTaxPrice());
            priceLibrary.setTaxKey(item.getTaxKey());
            priceLibrary.setTaxRate(StringUtil.StringValue(item.getTaxRate()));
            priceLibrary.setCurrencyId(item.getCurrencyId());
            priceLibrary.setCurrencyName(item.getCurrencyName());
            priceLibrary.setCurrencyCode(item.getCurrencyCode());
            priceLibrary.setEffectiveDate(Date.from(item.getStartTime().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            priceLibrary.setExpirationDate(Date.from(item.getEndTime().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            priceLibrary.setUnit(item.getUnit());
            priceLibrary.setUnitCode(item.getUnit()); //todo ??????????????????????????????
            /*priceLibrary.setIsLadder();*/
            priceLibrary.setLadderType(item.getLadderPriceType());
            priceLibrary.setCreatedId(loginAppUser.getCreatedId());
            priceLibrary.setCreatedBy(loginAppUser.getNickname());
            priceLibrary.setCreationDate(loginAppUser.getCreationDate());
            priceLibrary.setVersion(0L);
            priceLibrary.setTenantId(0L);
            /*priceLibrary.setCeeaPriceLibraryNo();*/
            priceLibrary.setCeeaOrgId(item.getOrgId());
            priceLibrary.setCeeaOrgCode(item.getOrgCode());
            priceLibrary.setCeeaOrgName(item.getOrgName());
            priceLibrary.setCeeaOrganizationId(item.getOrganizationId());
            priceLibrary.setCeeaOrganizationCode(item.getOrganizationCode());
            priceLibrary.setCeeaOrganizationName(item.getOrganizationName());
            priceLibrary.setCeeaArrivalPlace(item.getArrivalPlace());
            priceLibrary.setCeeaAllocationType(item.getQuotaDistributionType());
            priceLibrary.setCeeaQuotaProportion(item.getQuotaProportion());
            priceLibrary.setCeeaLt(item.getLAndT());
            priceLibrary.setCeeaIfUse("N");
            priceLibrary.setMinOrderQuantity(item.getMinOrderQuantity());  //???????????????
            /*?????????????????????????????????*/
            QueryWrapper wrapper = new QueryWrapper();
            wrapper.eq("APPROVAL_BIDDING_ITEM_ID", item.getApprovalBiddingItemId());
            List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = new ArrayList<>();
            List<ApprovalBiddingItemPaymentTerm> paymentTermList = approvalBiddingItemPaymentTermService.list(wrapper);
            if (CollectionUtils.isNotEmpty(paymentTermList)) {
                paymentTermList.forEach(paymentTermItem -> {
                    PriceLibraryPaymentTerm priceLibraryPaymentTerm = new PriceLibraryPaymentTerm()
                            .setPriceLibraryPaymentTermId(IdGenrator.generate())
                            .setPriceLibraryId(priceLibrary.getPriceLibraryId())
                            .setPaymentTerm(paymentTermItem.getPaymentTerm())
                            .setPaymentDay(paymentTermItem.getPaymentDay())
                            .setPaymentWay(paymentTermItem.getPaymentWay());
                    priceLibraryPaymentTermList.add(priceLibraryPaymentTerm);
                });
            }
            priceLibrary.setPriceLibraryPaymentTermList(priceLibraryPaymentTermList);
            priceLibraryList.add(priceLibrary);
        });
        return priceLibraryList;
    }


    /**
     * ?????????????????????
     *
     * @param header
     * @return
     */
    private List<PriceLibraryDTO> ceeaBuildPriceLibraryParam(ApprovalHeader header) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        Date date = new Date();
        ApprovalAllVo approvalVo = this.ceeaGetApprovalDetail(header.getApprovalHeaderId());

        ApprovalHeader approvalHeader = approvalVo.getApprovalHeader();
        List<ApprovalFile> approvalFileList = approvalVo.getApprovalFileList();
        List<ApprovalBiddingItemVO> approvalBiddingItemList = approvalVo.getApprovalBiddingItemList();
        /*??????????????????????????????????????????????????????????????????????????????????????????????????????????????????*/
        String createdBy = approvalHeader.getCreatedBy();
        Long createdId = approvalHeader.getCreatedId();

        /*ou?????????*/
        List<ApprovalBiddingItemVO> params = new ArrayList<>();
        approvalBiddingItemList.forEach(item -> {
            if (item.getOuId() == null) {
                params.add(item);
            } else {
                List<Long> ids = new ArrayList<>();
                ids.add(item.getOuId());
                List<BaseOuGroupDetailVO> ouInfoDetails = baseClient.queryOuInfoDetailByIds(ids);
                if (CollectionUtils.isEmpty(ouInfoDetails) ||
                        CollectionUtils.isEmpty(ouInfoDetails.get(0).getDetails())
                ) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("ou???[" + ouInfoDetails.get(0).getOuGroupName() + "]???????????????????????????"));
                }
                for (BaseOuDetailVO baseOuDetailVO : ouInfoDetails.get(0).getDetails()) {
                    ApprovalBiddingItemVO approvalBiddingItem = new ApprovalBiddingItemVO();
                    BeanUtils.copyProperties(item, approvalBiddingItem);
                    approvalBiddingItem.setApprovalBiddingItemPaymentTermList(item.getApprovalBiddingItemPaymentTermList());
                    approvalBiddingItem.setOrgId(baseOuDetailVO.getOuId())
                            .setOrgCode(baseOuDetailVO.getOuCode())
                            .setOrgName(baseOuDetailVO.getOuName())
                            .setOrganizationId(baseOuDetailVO.getInvId())
                            .setOrganizationName(baseOuDetailVO.getInvName())
                            .setOrganizationCode(baseOuDetailVO.getInvCode());
                    params.add(approvalBiddingItem);
                }
            }
        });

        /*???????????????*/
        List<PriceLibraryDTO> priceLibraryList = new ArrayList<>();
        params.forEach(item -> {
            PriceLibraryDTO priceLibrary = new PriceLibraryDTO();
            priceLibrary.setPriceLibraryId(IdGenrator.generate());
            priceLibrary.setCeeaPriceLibraryNo(baseClient.seqGen(SequenceCodeConstant.SEQ_INQ_PRICE_LIBRARY_NO)); //???????????????todo
            priceLibrary.setItemId(item.getItemId());
            priceLibrary.setItemCode(item.getItemCode());
            priceLibrary.setItemDesc(item.getItemName());
            priceLibrary.setCategoryId(item.getCategoryId());
            priceLibrary.setCategoryName(item.getCategoryName());
            priceLibrary.setCategoryCode(item.getCategoryCode());
            priceLibrary.setVendorId(item.getVendorId());
            priceLibrary.setVendorCode(item.getVendorCode());
            priceLibrary.setVendorName(item.getVendorName());
            priceLibrary.setApprovalNo(approvalHeader.getApprovalNo());
            priceLibrary.setPriceType(item.getPriceType());
            priceLibrary.setNotaxPrice(item.getNotaxPrice());
            priceLibrary.setTaxPrice(item.getTaxPrice());
            priceLibrary.setTaxKey(item.getTaxKey());
            priceLibrary.setTaxRate(StringUtil.StringValue(item.getTaxRate()));
            priceLibrary.setCurrencyId(item.getCurrencyId());
            priceLibrary.setCurrencyName(item.getCurrencyName());
            priceLibrary.setCurrencyCode(item.getCurrencyCode());
            //???????????????
            priceLibrary.setIsSeaFoodFormula(item.getIsSeaFoodFormula())
                    .setFormulaId(item.getFormulaId())
                    .setFormulaValue(item.getFormulaValue())
                    .setPriceJson(item.getPriceJson())
                    .setFormulaResult(item.getFormulaResult())
                    .setEssentialFactorValues(item.getEssentialFactorValues());
            if (StringUtil.notEmpty(item.getStartTime())) {
                priceLibrary.setEffectiveDate(DateUtil.getLocalDateByDate(item.getStartTime()));
            }
            if (StringUtil.notEmpty(item.getEndTime())) {
                priceLibrary.setExpirationDate(DateUtil.getLocalDateByDate(item.getEndTime()));
            }
            priceLibrary.setUnit(item.getUnit());
            priceLibrary.setUnitCode(item.getUnit());/*todo ????????????????????????????????????*/
            /*priceLibrary.setIsLadder();*/
            priceLibrary.setLadderType(item.getLadderPriceType());
            priceLibrary.setTradeTerm(item.getTradeTerm());
            priceLibrary.setCreatedId(createdId);
            priceLibrary.setCreatedBy(createdBy);
            priceLibrary.setCreationDate(date);
            priceLibrary.setVersion(0L);
            priceLibrary.setTenantId(0L);
            /*priceLibrary.setCeeaPriceLibraryNo();*/
            priceLibrary.setCeeaOrgId(item.getOrgId());
            priceLibrary.setCeeaOrgCode(item.getOrgCode());
            priceLibrary.setCeeaOrgName(item.getOrgName());
            priceLibrary.setCeeaOrganizationId(item.getOrganizationId());
            priceLibrary.setCeeaOrganizationCode(item.getOrganizationCode());
            priceLibrary.setCeeaOrganizationName(item.getOrganizationName());
            priceLibrary.setCeeaArrivalPlace(item.getArrivalPlace());
            priceLibrary.setCeeaAllocationType(item.getQuotaDistributionType());
            priceLibrary.setCeeaQuotaProportion(item.getQuotaProportion());
            priceLibrary.setCeeaLt(item.getLAndT());
            priceLibrary.setCeeaIfUse("N");
            priceLibrary.setMinOrderQuantity(item.getMinOrderQuantity());  //???????????????

            //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????  add by chenwt24@meicloud.com    2020-10-07
            priceLibrary.setCeeaAllocationType(item.getQuotaDistributionType());//??????????????????
            priceLibrary.setCeeaQuotaProportion(item.getQuotaProportion());//????????????

            List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(item.getApprovalBiddingItemPaymentTermList())) {
                item.getApprovalBiddingItemPaymentTermList().forEach(paymentTermItem -> {
                    PriceLibraryPaymentTerm priceLibraryPaymentTerm = new PriceLibraryPaymentTerm();
                    priceLibraryPaymentTerm.setPriceLibraryPaymentTermId(IdGenrator.generate())
                            .setPriceLibraryId(priceLibrary.getPriceLibraryId())
                            .setPaymentTerm(paymentTermItem.getPaymentTerm())
                            .setPaymentWay(paymentTermItem.getPaymentWay())
                            .setPaymentRatio(paymentTermItem.getPaymentRatio())
                            .setPaymentStage(paymentTermItem.getPaymentStage())
                            .setPaymentDay(paymentTermItem.getPaymentDay());
                    priceLibraryPaymentTermList.add(priceLibraryPaymentTerm);
                });
            }

            priceLibrary.setPriceLibraryPaymentTermList(priceLibraryPaymentTermList);
            priceLibraryList.add(priceLibrary);
        });

        return priceLibraryList;
    }

    /**
     * ????????????????????????
     */
    private PriceLibraryAddParam buildPriceLibraryAddParam(ApprovalHeader header, ApprovalItem approvalItem) {
        PriceLibraryAddParam libraryAddParam = new PriceLibraryAddParam();
        BeanUtils.copyProperties(header, libraryAddParam);
        BeanUtils.copyProperties(approvalItem, libraryAddParam);
        libraryAddParam.setSourceNo(header.getBusinessNo());
        libraryAddParam.setNotaxPrice(approvalItem.getNotaxSelectedPrice());
        libraryAddParam.setPriceType(approvalItem.getItemType());
        libraryAddParam.setEffectiveDate(approvalItem.getFixedPriceBegin());
        libraryAddParam.setExpirationDate(approvalItem.getFixedPriceEnd());
        return libraryAddParam;
    }

    /**
     * ???????????????
     */
    @Transactional(rollbackFor = Exception.class)
    protected void generateApproval(String approvalNo, List<QuoteSelectedResult> quoteItems, List<QuoteLadderPrice> ladderPrices) {
        /*??????????????????*/
        ApprovalHeader header = buildApprovalHeader(approvalNo, quoteItems);
        List<ApprovalItem> approvalItems = new ArrayList<>();
        List<ApprovalLadderPrice> approvalLadderPrices = new ArrayList<>();
        /*??????????????????*/
        buildItemsAndLadderPrices(header, approvalItems, approvalLadderPrices, quoteItems, ladderPrices);
        save(header);
        iApprovalItemService.saveBatch(approvalItems);
        if (CollectionUtils.isNotEmpty(approvalLadderPrices)) {
            iApprovalLadderPriceService.saveBatch(approvalLadderPrices);
        }
    }

    /**
     * ??????????????????
     */
    @Transactional(rollbackFor = Exception.class)
    protected void updateSelectionStatus(List<QuoteSelectedResult> quoteItems) {
        List<Long> quoteItemIds = new ArrayList<>();
        quoteItems.forEach(quoteSelectedResult -> quoteItemIds.add(quoteSelectedResult.getQuoteItemId()));
        List<QuoteSelection> selections = iQuoteSelectionService.getQuoteSelectionByQuoteItemId(quoteItemIds);
        selections.forEach(selection -> selection.setIsSelected(RfqResult.PRICE_APPROVAL_FORM.getKey()));
        iQuoteSelectionService.updateBatchById(selections);
    }

    private void buildItemsAndLadderPrices(ApprovalHeader header, List<ApprovalItem> approvalItems,
                                           List<ApprovalLadderPrice> approvalLadderPrices,
                                           List<QuoteSelectedResult> quoteItems, List<QuoteLadderPrice> ladderPrices) {

        quoteItems.forEach(quoteSelectedResult -> {
            ApprovalItem itemEntity = buildApprovalItem(header, quoteSelectedResult);
            approvalItems.add(itemEntity);

            if (CollectionUtils.isNotEmpty(ladderPrices)) {
                List<ApprovalLadderPrice> ladderPricesEntities = new ArrayList<>();
                List<QuoteLadderPrice> afterFilter = ladderPrices.stream().filter(ladderPrice ->
                        ladderPrice.getQuoteItemId().equals(quoteSelectedResult.getQuoteItemId())).collect(Collectors.toList());
                afterFilter.forEach(ladderPrice -> ladderPricesEntities.add(bulidApprovalLadderPrice(itemEntity, ladderPrice)));
                approvalLadderPrices.addAll(ladderPricesEntities);
            }
        });
    }

    /**
     * ???????????????????????????
     */
    private ApprovalLadderPrice bulidApprovalLadderPrice(ApprovalItem itemEntity, QuoteLadderPrice ladderPrice) {

        ApprovalLadderPrice ladderPricesEntity = new ApprovalLadderPrice();
        BeanUtils.copyProperties(ladderPrice, ladderPricesEntity);
        ladderPricesEntity.setApprovalPriceLadderId(IdGenrator.generate());
        ladderPricesEntity.setApprovalItemId(itemEntity.getApprovalItemId());
        return ladderPricesEntity;
    }

    /**
     * ??????????????????????????????
     */
    private ApprovalItem buildApprovalItem(ApprovalHeader header, QuoteSelectedResult quoteSelectedResult) {
        ApprovalItem itemEntity = new ApprovalItem();
        BeanUtils.copyProperties(quoteSelectedResult, itemEntity);
        itemEntity.setApprovalItemId(IdGenrator.generate());
        itemEntity.setApprovalHeaderId(header.getApprovalHeaderId());
        itemEntity.setApprovalNo(header.getApprovalNo());
        itemEntity.setNotaxCurrentPrice(quoteSelectedResult.getCurrentPrice());
        itemEntity.setNotaxSelectedPrice(quoteSelectedResult.getNotaxPrice());
        return itemEntity;
    }

    /**
     * ????????????????????????
     */
    private ApprovalHeader buildApprovalHeader(String approvalNo, List<QuoteSelectedResult> quoteItems) {

        QuoteSelectedResult quoteItem = quoteItems.get(0);

        ApprovalHeader header = new ApprovalHeader();
        BeanUtils.copyProperties(quoteItem, header);
        header.setBusinessId(quoteItem.getInquiryId());
        header.setBusinessNo(quoteItem.getInquiryNo());
        header.setBusinessTitle(quoteItem.getInquiryTitle());
        header.setApprovalHeaderId(IdGenrator.generate());
        header.setApprovalNo(approvalNo);
        header.setStatus(ApproveStatusType.DRAFT.getValue());
        header.setSourceType(SourceType.INQUIRY.getValue());
        return header;
    }

    /**
     * ????????????????????????
     *
     * @param approvalBiddingItemDto
     * @return
     */
    @Override
    public List<ApprovalBiddingItem> ceeaQueryByCm(ApprovalBiddingItemDto approvalBiddingItemDto) {
        Assert.notNull(approvalBiddingItemDto, "??????????????????");
        Assert.notNull(approvalBiddingItemDto.getVendorId(), "vendorId????????????");
        return this.baseMapper.ceeaQueryByCm(approvalBiddingItemDto);
    }

    /**
     * ?????????????????????-??????????????????
     */
    @Override
    public List<Map<String, Object>> getContractTypeByApprovalHeaderId(Long approvalHeaderId) {
        //?????????????????????????????????????????????????????????
        //????????????-??????????????????
        //????????????-ou????????????????????????
        List<ApprovalBiddingItem> approvalBiddingItems = approvalBiddingItemMapper.selectList(Wrappers.lambdaQuery(
                ApprovalBiddingItem.class)
                .select(ApprovalBiddingItem::getOrgId, ApprovalBiddingItem::getOuId)
                .eq(ApprovalBiddingItem::getApprovalHeaderId, approvalHeaderId));
        boolean isOu = false;
        boolean isPackaging;
        HashSet<Long> set = new HashSet<>();
        for (ApprovalBiddingItem approvalBiddingItem : approvalBiddingItems) {
            if (Objects.nonNull(approvalBiddingItem)) {
                if (Objects.nonNull(approvalBiddingItem.getOuId())) {
                    isOu = true;
                }
                if (Objects.nonNull(approvalBiddingItem.getOrgId())) {
                    set.add(approvalBiddingItem.getOrgId());
                }
            }
        }
        isPackaging = set.size() > 1;

        List<Map<String, Object>> results = new LinkedList<>();
        if (isPackaging || isOu) {
            for (ContractAgreement value : ContractAgreement.values()) {
                Map<String, Object> result = new HashMap<>();
                result.put("value", value.getCode());
                result.put("label", value.getMean());
                results.add(result);
            }
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("value", ContractAgreement.PACKAGINGSIGNING.getCode());
            result.put("label", ContractAgreement.PACKAGINGSIGNING.getMean());
            results.add(result);
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> getOrgInfoByApprovalHeaderId(Long approvalHeaderId) {

        List<Map<String, Object>> info = approvalBiddingItemMapper.selectList(Wrappers.lambdaQuery(
                ApprovalBiddingItem.class)
                .select(ApprovalBiddingItem::getOrgId, ApprovalBiddingItem::getOrgName)
                .eq(ApprovalBiddingItem::getApprovalHeaderId, approvalHeaderId)).stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap();
                    map.put("orgId", e.getOrgId());
                    map.put("orgName", e.getOrgName());
                    return map;
                }).collect(Collectors.toList());
        return info;
    }

    /**
     * ???????????????id?????????id????????????id???????????????
     *
     * @param queryDto
     * @return
     */
    @Override
    public List<ApprovalBiddingItemVO> getItemVoByParam(ApprovalToContractQueryDto queryDto) {
        //??????????????????????????????ou???
        List<ApprovalBiddingItem> approvalBiddingItems = approvalBiddingItemMapper.selectList(Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                .eq(Objects.nonNull(queryDto.getApprovalHeaderId()), ApprovalBiddingItem::getApprovalHeaderId, queryDto.getApprovalHeaderId())
                .eq(Objects.nonNull(queryDto.getVendorId()), ApprovalBiddingItem::getVendorId, queryDto.getVendorId())
                .eq(Objects.nonNull(queryDto.getItemId()), ApprovalBiddingItem::getItemId, queryDto.getItemId())
                .nested(e -> e.isNull(ApprovalBiddingItem::getFromContractId).or().isNotNull(ApprovalBiddingItem::getOuGroupJson))
        );
        List<Long> lineIds = new LinkedList<>();
        List<ApprovalBiddingItemVO> items = new LinkedList<>();
        for (ApprovalBiddingItem approvalBiddingItem : approvalBiddingItems) {
            //ou?????????,???????????????????????????
            if (Objects.nonNull(approvalBiddingItem.getOuId())
                    && Objects.equals(queryDto.getType(), ContractAgreement.SEPERATELYSIGNING.getCode())) {
                String ouGroupJson = approvalBiddingItem.getOuGroupJson();
                List<ApprovalBiddingItemVO> temp = JSON.parseArray(ouGroupJson, ApprovalBiddingItemVO.class);
                if (Objects.nonNull(queryDto.getOrgId())) {
                    temp = temp.stream().filter(e -> Objects.equals(e.getOrgId(), queryDto.getOrgId())).collect(Collectors.toList());
                }
                items.addAll(temp);
                lineIds.add(approvalBiddingItem.getApprovalBiddingItemId());
            } else {
                //??????????????????orOu?????????
                ApprovalBiddingItemVO approvalBiddingItemVO = BeanCopyUtil.copyProperties(approvalBiddingItem, ApprovalBiddingItemVO::new);
                if (Objects.nonNull(queryDto.getOrgId()) && !Objects.equals(approvalBiddingItemVO.getOrgId(), queryDto.getOrgId())) {
                    continue;
                }
                lineIds.add(approvalBiddingItem.getApprovalBiddingItemId());
                items.add(approvalBiddingItemVO);
            }

        }
        if (CollectionUtils.isNotEmpty(lineIds)) {
            List<ApprovalBiddingItemPaymentTerm> paymentTerms = approvalBiddingItemPaymentTermService.list(Wrappers.lambdaQuery(ApprovalBiddingItemPaymentTerm.class)
                    .in(ApprovalBiddingItemPaymentTerm::getApprovalBiddingItemId, lineIds));
            for (ApprovalBiddingItemVO approvalBiddingItem : items) {
                if (CollectionUtils.isEmpty(approvalBiddingItem.getApprovalBiddingItemPaymentTermList())) {
                    List<ApprovalBiddingItemPaymentTerm> itemPaymentTerms = paymentTerms.stream()
                            .filter(e -> Objects.equals(approvalBiddingItem.getApprovalBiddingItemId(), e.getApprovalBiddingItemId()))
                            .collect(Collectors.toList());
                    approvalBiddingItem.setApprovalBiddingItemPaymentTermList(itemPaymentTerms);
                }
            }
        }
        return items;
    }


    private static String getPaymentTermId(ApprovalBiddingItemPaymentTerm term) {
        StringBuilder builder = new StringBuilder();
        builder.append(term.getPaymentDayCode()).append(term.getPaymentWay()).append(term.getPaymentTerm());
        return builder.toString();
    }

    private static String getBidingItemVOId(ApprovalBiddingItemVO vo) {
        List<ApprovalBiddingItemPaymentTerm> terms = vo.getApprovalBiddingItemPaymentTermList();
        Comparator<ApprovalBiddingItemPaymentTerm> comparator = new Comparator<ApprovalBiddingItemPaymentTerm>() {
            @Override
            public int compare(ApprovalBiddingItemPaymentTerm o1, ApprovalBiddingItemPaymentTerm o2) {
                return getPaymentTermId(o1).compareTo(getPaymentTermId(o2));
            }
        };
        terms.sort(comparator);
        StringBuilder id = new StringBuilder();
        terms.forEach(e -> {
            String paymentTermId = getPaymentTermId(e);
            id.append(paymentTermId);
        });
        return id.toString();
    }

    /**
     * ?????????????????????
     *
     * @param itemVOS
     * @return
     */
    @Override
    public List<Long> genContractByApprovalBidingItemVOs(List<ApprovalBiddingItemVO> itemVOS) {
        if (CollectionUtils.isEmpty(itemVOS)) {
            throw new BaseException("?????????????????????????????????");
        }
        /**
         * ????????????????????????????????? ?????????
         */
        itemVOS.forEach(approvalBiddingItemVO -> Assert.notNull(approvalBiddingItemVO.getCeeaIfVirtual(), "??????????????????????????????"));

        Long headerId = itemVOS.stream()
                .filter(e -> Objects.nonNull(e.getApprovalHeaderId()))
                .findAny().orElseThrow(() -> new BaseException("???????????????????????????id"))
                .getApprovalHeaderId();
        Set<Long> itemIds = itemVOS.stream().map(ApprovalBiddingItemVO::getApprovalBiddingItemId).collect(Collectors.toSet());
        ApprovalHeader header = getOne(Wrappers.lambdaQuery(ApprovalHeader.class)
                .select(ApprovalHeader::getTransferStatus, ApprovalHeader::getApprovalHeaderId,
                        ApprovalHeader::getCeeaIfUpdatePriceLibrary
                )
                .eq(ApprovalHeader::getApprovalHeaderId, headerId));
        if (Objects.isNull(header)) {
            throw new BaseException(String.format("???????????????id???%s??????????????????", headerId));
        }
        if (Objects.equals(header.getCeeaIfUpdatePriceLibrary(), YesOrNo.YES.getValue())) {
            throw new BaseException("???????????????????????????????????????????????????????????????!");
        }
        String transferStatus = header.getTransferStatus();
        if (StringUtil.notEmpty(transferStatus) && (Objects.equals(transferStatus, TransferStatus.FINISH.getCode())
                || Objects.equals(transferStatus, TransferStatus.CHANGE.getCode()))) {
            throw new BaseException(String.format("????????????????????????:%s,???????????????", TransferStatus.getMeanByCode(transferStatus)));
        }
        //??????????????????
        HashMap<Long, String> orgIds = new HashMap<>();
        HashSet<Long> ouIds = new HashSet<>();
        for (ApprovalBiddingItemVO itemVO : itemVOS) {
            if (Objects.nonNull(itemVO.getOrgId())) {
                orgIds.put(itemVO.getOrgId(), itemVO.getOrgName());
            }
            if (Objects.nonNull(itemVO.getOuId()) && StringUtil.notEmpty(itemVO.getOuGroupJson())) {
                ouIds.add(itemVO.getOuId());
            }
        }
        if (CollectionUtils.isNotEmpty(ouIds)) {
            List<BaseOuGroupDetailVO> baseOuGroupDetailVOS = baseClient.queryOuInfoDetailByIds(ouIds);
            for (BaseOuGroupDetailVO baseOuGroupDetailVO : baseOuGroupDetailVOS) {
                for (BaseOuDetailVO detail : baseOuGroupDetailVO.getDetails()) {
                    orgIds.put(detail.getOuId(), detail.getOuName());
                }
            }
        }
        Set<Long> containsIds = AppUserUtil.getLoginAppUser().getOrganizationUsers().stream().map(OrganizationUser::getOrganizationId).collect(Collectors.toSet());
        for (Map.Entry<Long, String> entry : orgIds.entrySet()) {
            if (!containsIds.contains(entry.getKey())) {
                throw new BaseException(String.format("??????????????????%s?????????", entry.getValue()));
            }
        }
//????????????????????????????????????
        List<ApprovalBiddingItem> check = approvalBiddingItemMapper.selectList(Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                .select(ApprovalBiddingItem::getApprovalBiddingItemId
                        , ApprovalBiddingItem::getItemId
                        , ApprovalBiddingItem::getOuId
                        , ApprovalBiddingItem::getOuGroupJson
                        , ApprovalBiddingItem::getCategoryId
                        , ApprovalBiddingItem::getItemName
                        , ApprovalBiddingItem::getVendorName
                        , ApprovalBiddingItem::getFromContractId)
                .in(ApprovalBiddingItem::getApprovalBiddingItemId, itemIds));
        StringBuilder errMsg = new StringBuilder("?????????????????????,[");
        boolean error = false;
        Map<Long, String> map = new HashMap<>();
        for (ApprovalBiddingItem item : check) {
            map.put(item.getItemId(), item.getItemName());
            if ((Objects.isNull(item.getOuId()) && Objects.nonNull(item.getFromContractId()))
                    || Objects.nonNull(item.getOuId()) && Objects.isNull(item.getOuGroupJson())) {
                error = true;
                errMsg.append("?????????").append(item.getItemName()).append("-???-")
                        .append("?????????:").append(item.getVendorName()).append(",")
                ;
            }
        }
        if (error) {
            errMsg.deleteCharAt(errMsg.length() - 1).append("]??????????????????????????????");
            throw new BaseException(errMsg.toString());
        }
        List<MaterialMaxCategoryVO> materialMaxCategoryVOS = baseClient.queryCategoryMaxCodeByMaterialIds(map.keySet());
        for (MaterialMaxCategoryVO materialMaxCategoryVO : materialMaxCategoryVOS) {
            if (Objects.equals(materialMaxCategoryVO.getCategoryCode(), "50")) {
                throw new BaseException(String.format("????????????%s????????????????????????50???????????????", map.get(materialMaxCategoryVO.getMaterialId())));
            }
        }
        String key = APPROVAL_TO_CONTRACT_LOCK + headerId;
        //???????????????????????????????????????????????????
        Boolean isLock = redisUtil.tryLock(key, 1, TimeUnit.MINUTES);
        if (isLock) {
            //??????????????????
            updateTransferStatus(headerId, TransferStatus.CHANGE);
            try {
                String approvalNo = approvalHeaderMapper.selectOne(Wrappers.lambdaQuery(ApprovalHeader.class)
                        .select(ApprovalHeader::getApprovalNo)
                        .eq(ApprovalHeader::getApprovalHeaderId, headerId)
                ).getApprovalNo();
                //?????????????????????+List<????????????>???paymentTerm???paymentWay???paymentDayCode???
                itemVOS.forEach(e -> {
                    if (Objects.isNull(e.getVendorId())) {
                        throw new BaseException(String.format("?????????[%s]?????????", e.getVendorName()));
                    }
                    e.setGroupId(getBidingItemVOId(e));
                });

                Map<Long, Map<String, List<ApprovalBiddingItemVO>>> contractGroup = itemVOS.stream()
                        .collect(Collectors.groupingBy(ApprovalBiddingItemVO::getVendorId
                                , Collectors.groupingBy(ApprovalBiddingItemVO::getGroupId)));
                //????????????
                List<Long> contractIds = new LinkedList<>();
                contractGroup.forEach((vendorId, v) -> {
                    v.forEach((groupId, line) -> {

                        ApprovalToContractDetail detail = new ApprovalToContractDetail();
                        detail.setContractLineList(line);
                        detail.setVendorId(vendorId);
                        detail.setApprovalNo(approvalNo);
                        detail.setApprovalHeaderId(headerId);
                        // ??????????????????
                        String ceeaIfVirtual = line.get(0).getCeeaIfVirtual();
                        detail.setCeeaIfVirtual(ceeaIfVirtual);
                        // ????????????????????????
                        detail.setSourceType("PRICE_APPROVAL");
                        try {
                            Long id = contractClient.genContractFromApproval(detail);
                        } catch (Exception e) {
                            throw new BaseException(e.getMessage());
                        }
                        //??????ou??????????????????????????????????????????id??????ou????????????????????????JSON
                        Set<Long> ids = line.stream().map(ApprovalBiddingItemVO::getApprovalBiddingItemId).collect(Collectors.toSet());
                        List<ApprovalBiddingItem> approvalBiddingItems = approvalBiddingItemMapper.selectList(
                                Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                                        .in(ApprovalBiddingItem::getApprovalBiddingItemId, ids)
                                        .isNotNull(ApprovalBiddingItem::getOuId)
                        );
                        //????????????ou????????????
                        for (ApprovalBiddingItem approvalBiddingItem : approvalBiddingItems) {
                            //ou?????????
                            List<ApprovalBiddingItem> temp = JSON.parseArray(approvalBiddingItem.getOuGroupJson(), ApprovalBiddingItem.class);
                            //???????????????????????????
                            for (ApprovalBiddingItemVO approvalBiddingItemVO : line) {
                                for (int i = temp.size() - 1; i >= 0; i--) {
                                    ApprovalBiddingItem item = temp.get(i);
                                    //??????????????????
                                    if (Objects.equals(approvalBiddingItemVO.getApprovalBiddingItemId(), item.getApprovalBiddingItemId())
                                            && orgIds.containsKey(item.getOrgId())
                                    ) {
                                        temp.remove(i);
                                    }
                                }
                            }
                            //??????ou???
                            approvalBiddingItemMapper.update(null, Wrappers.lambdaUpdate(ApprovalBiddingItem.class)
                                    .set(temp.isEmpty(), ApprovalBiddingItem::getOuGroupJson, null)
                                    .set(!temp.isEmpty(), ApprovalBiddingItem::getOuGroupJson, JSON.toJSONString(temp))
                                    .eq(ApprovalBiddingItem::getApprovalBiddingItemId, approvalBiddingItem.getApprovalBiddingItemId())
                            );
                        }
                    });
                });
                return contractIds;
            } catch (Throwable ex) {
                updateTransferStatus(headerId, TransferStatus.FAIL);
                log.error(ex.getMessage(), ex);
                throw new BaseException(ex.getMessage());
            } finally {
                redisUtil.unLock(key);
            }
        } else {
            throw new BaseException("???????????????????????????...???????????????!");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItemsBelongNumber(Collection<ApprovalBiddingItemVO> itemVOS) {
        if (org.springframework.util.CollectionUtils.isEmpty(itemVOS)) {
            return;
        }
        Long headerId = itemVOS.stream()
                .filter(e -> Objects.nonNull(e.getApprovalHeaderId()))
                .findAny().orElseThrow(() -> new BaseException("???????????????????????????id"))
                .getApprovalHeaderId();
        List<ApprovalBiddingItem> items = itemVOS.stream().map(e -> {
            ApprovalBiddingItem item = new ApprovalBiddingItem();
            item.setApprovalBiddingItemId(e.getApprovalBiddingItemId());
            item.setFromContractCode(e.getFromContractCode());
            item.setFromContractId(e.getFromContractId());
            return item;
        }).collect(Collectors.toList());
        iApprovalBiddingItemService.updateBatchById(items);
        int count = iApprovalBiddingItemService.count(Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                .eq(ApprovalBiddingItem::getApprovalHeaderId, headerId)
                .isNull(ApprovalBiddingItem::getFromContractId));
        if (count == 0) {
            updateTransferStatus(headerId, TransferStatus.FINISH);
        } else {
            updateTransferStatus(headerId, TransferStatus.CONTINUE);
        }
    }

    @Override
    public void resetItemsBelongNumber(Collection<Long> contractIds) {
        List<ApprovalBiddingItem> list = iApprovalBiddingItemService.list(Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                .select(ApprovalBiddingItem::getApprovalBiddingItemId, ApprovalBiddingItem::getApprovalHeaderId)
                .in(ApprovalBiddingItem::getFromContractId, contractIds)
        );
        List<Long> approvalIds = new LinkedList<>();
        Set<Long> headIds = new HashSet<>();
        for (ApprovalBiddingItem item : list) {
            approvalIds.add(item.getApprovalBiddingItemId());
            headIds.add(item.getApprovalHeaderId());
        }
        //???????????????
        if (!headIds.isEmpty()) {
            update(Wrappers.lambdaUpdate(ApprovalHeader.class)
                    .set(ApprovalHeader::getTransferStatus, null)
                    .in(ApprovalHeader::getApprovalHeaderId, headIds)
            );
        }

        for (Long contractId : contractIds) {
            List<ApprovalBiddingItem> temp = iApprovalBiddingItemService.list(Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                    .eq(ApprovalBiddingItem::getFromContractId, contractId)
                    .isNotNull(ApprovalBiddingItem::getOuId)
            );
            if (!temp.isEmpty()) {
                Set<Long> ouIds = temp.stream().map(ApprovalBiddingItem::getOuId).collect(Collectors.toSet());
                Map<Long, BaseOuGroupDetailVO> ouGroupMap = baseClient.queryOuInfoDetailByIds(ouIds).stream().collect
                        (Collectors.toMap(BaseOuGroupDetailVO::getOuGroupId, Function.identity()));
                Set<Long> itemIds = temp.stream().map(ApprovalBiddingItem::getApprovalBiddingItemId).collect(Collectors.toSet());
                Map<Long, List<ApprovalBiddingItemPaymentTerm>> paymentTermsMap = approvalBiddingItemPaymentTermService.list(Wrappers.lambdaQuery(ApprovalBiddingItemPaymentTerm.class)
                        .in(ApprovalBiddingItemPaymentTerm::getApprovalBiddingItemId, itemIds)
                ).stream().collect(Collectors.groupingBy(ApprovalBiddingItemPaymentTerm::getApprovalBiddingItemId));
                //???????????????ou???
                List<ApprovalBiddingItem> updateList = new LinkedList<>();
                for (ApprovalBiddingItem currentItem : temp) {
                    //???ou???????????????
                    if (Objects.nonNull(currentItem.getOuId())) {
                        BaseOuGroupDetailVO baseOuGroupDetailVOS = ouGroupMap.get(currentItem.getOuId());
                        List<ApprovalBiddingItem> items = new LinkedList<>();
                        for (BaseOuDetailVO baseOuGroupDetailVO : baseOuGroupDetailVOS.getDetails()) {
                            ApprovalBiddingItemVO sub = BeanCopyUtil.copyProperties(currentItem, ApprovalBiddingItemVO::new);
                            sub.setOrgId(baseOuGroupDetailVO.getOuId())
                                    .setOrgCode(baseOuGroupDetailVO.getOuCode())
                                    .setOrgName(baseOuGroupDetailVO.getOuName())
                                    .setOrganizationId(baseOuGroupDetailVO.getInvId())
                                    .setOrganizationCode(baseOuGroupDetailVO.getInvCode())
                                    .setOrganizationName(baseOuGroupDetailVO.getInvName());
                            sub.setOuId(null);
                            sub.setOuName(null);
                            sub.setOuNumber(null);
                            List<ApprovalBiddingItemPaymentTerm> terms = paymentTermsMap.get(currentItem.getApprovalBiddingItemId());
                            List<ApprovalBiddingItemPaymentTerm> tempTerms = terms.stream().map(e -> BeanCopyUtil.copyProperties(e, ApprovalBiddingItemPaymentTerm::new)).collect(Collectors.toList());
                            sub.setApprovalBiddingItemPaymentTermList(tempTerms);
                            items.add(sub);
                        }
                        String subJson = JSON.toJSONString(items);
                        currentItem.setOuGroupJson(subJson);
                        updateList.add(currentItem);
                    }
                }
                if (!updateList.isEmpty()) {
                    iApprovalBiddingItemService.updateBatchById(updateList);
                }
            }
        }

        //???????????????
        if (!approvalIds.isEmpty()) {
            iApprovalBiddingItemService.update(Wrappers.lambdaUpdate(ApprovalBiddingItem.class)
                    .set(ApprovalBiddingItem::getFromContractId, null)
                    .set(ApprovalBiddingItem::getFromContractCode, null)
                    .in(ApprovalBiddingItem::getApprovalBiddingItemId, approvalIds)
            );
        }

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    void updateTransferStatus(Long headerId, TransferStatus status) {
        update(Wrappers.lambdaUpdate(ApprovalHeader.class)
                .set(ApprovalHeader::getTransferStatus, status.getCode())
                .eq(ApprovalHeader::getApprovalHeaderId, headerId));
    }

    /**
     * ????????????????????????
     *
     * @param approvalHeadId
     */
    @Override
    @Transactional
    public void ceeaApprovalToContract(Long approvalHeadId) {
        Assert.notNull(approvalHeadId, LocaleHandler.getLocaleMsg("?????????id????????????"));
        //???????????????????????????
        ApprovalAllVo approvalAllVo = this.ceeaGetApprovalDetail(approvalHeadId);
        //=================================================????????????==========================================
        // ??????????????????????????????????????????????????????
        //?????????????????????
        List<ApprovalBiddingItemVO> approvalBiddingItemVOList = approvalAllVo.getApprovalBiddingItemList();
        List<List<ApprovalBiddingItemVO>> lists = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(approvalBiddingItemVOList)) {
            approvalBiddingItemVOList.forEach(item -> {
                boolean add = true;
                for (int i = 0; i < lists.size(); i++) {
                    List<ApprovalBiddingItemVO> list = lists.get(i);
                    if (list.size() > 0) {
                        ApprovalBiddingItemVO approvalBiddingItemVO = list.get(0);
                        if (approvalBiddingItemVO.getVendorCode().equals(item.getVendorCode()) &&
                                ifCommonGroup(approvalBiddingItemVO.getApprovalBiddingItemPaymentTermList(), item.getApprovalBiddingItemPaymentTermList())
                        ) {
                            list.add(item);
                            add = false;
                            break;
                        }
                    }
                }
                if (add) {
                    List<ApprovalBiddingItemVO> list = new ArrayList<>();
                    list.add(item);
                    lists.add(list);
                }
            });
        }

        log.info("lists.size:" + lists.size());
        log.info("lists:" + lists);
        /*????????? todo*/
        lists.forEach(list -> {
            ContractDTO contractDTO = new ContractDTO();
            ContractHead contractHead = new ContractHead()
                    .setContractType(ContractType.MIAN_CONTRACT_ADD.name())   //????????????
                    .setBuId(list.get(0).getOrgId())
                    .setBuCode(list.get(0).getOrgCode())
                    .setBuName(list.get(0).getOrgName())
                    .setContractLevel("LEVEL_01")  //????????????
                    .setContractClass("NPM")  // todo
                    .setVendorId(list.get(0).getVendorId())
                    .setVendorCode(list.get(0).getVendorCode())
                    .setVendorName(list.get(0).getVendorName());

            List<PayPlan> payPlans = new ArrayList<>();
            list.get(0).getApprovalBiddingItemPaymentTermList().forEach(item -> {
                PayPlan payPlan = new PayPlan()
                        .setDelayedDays(item.getPaymentDay().longValue())  //????????????
                        .setPayMethod(item.getPaymentWay())  //????????????
                        .setPaymentStage(item.getPaymentTerm());  //????????????
                payPlans.add(payPlan);
            });
            List<ContractMaterial> contractMaterialList = new ArrayList<>();
            list.forEach(approvalBiddingItem -> {
                ContractMaterial contractMaterial = new ContractMaterial();
                contractMaterial.setCeeaSourceLineId(approvalBiddingItem.getApprovalBiddingItemId());
                contractMaterial.setSourceNumber(approvalBiddingItem.getCeeaSourceNo());
                contractMaterial.setSourceType(approvalBiddingItem.getSourceType());
                contractMaterial.setCeeaOuId(approvalBiddingItem.getOuId());
                contractMaterial.setCeeaOuName(approvalBiddingItem.getOuName());
                contractMaterial.setCeeaOuNumber(approvalBiddingItem.getOuNumber());
                contractMaterial.setBuId(approvalBiddingItem.getOrgId());
                contractMaterial.setBuName(approvalBiddingItem.getOrgName());
                contractMaterial.setBuCode(approvalBiddingItem.getOrgCode());
                contractMaterial.setInvId(approvalBiddingItem.getOrganizationId());
                contractMaterial.setInvCode(approvalBiddingItem.getOrganizationCode());
                contractMaterial.setInvName(approvalBiddingItem.getOrganizationName());
                contractMaterial.setTradingLocations(approvalBiddingItem.getArrivalPlace());
                contractMaterial.setMaterialId(approvalBiddingItem.getItemId());
                contractMaterial.setMaterialCode(approvalBiddingItem.getItemCode());
                contractMaterial.setMaterialName(approvalBiddingItem.getItemName());
                contractMaterial.setCategoryId(approvalBiddingItem.getCategoryId());
                contractMaterial.setCategoryCode(approvalBiddingItem.getCategoryCode());
                contractMaterial.setCategoryName(approvalBiddingItem.getCategoryName());
                //??????
                contractMaterial.setUnitCode(approvalBiddingItem.getUnit());
                contractMaterial.setUnitName(approvalBiddingItem.getUnit());
                contractMaterial.setContractQuantity(approvalBiddingItem.getNeedNum()); // ????????????
                contractMaterial.setTaxedPrice(approvalBiddingItem.getTaxPrice()); // ????????????
                if (null != contractMaterial.getContractQuantity() && null != contractMaterial.getTaxedPrice()) {
                    contractMaterial.setAmount(contractMaterial.getContractQuantity().multiply(contractMaterial.getTaxedPrice()));
                }
                contractMaterial.setTaxKey(approvalBiddingItem.getTaxKey());
                contractMaterial.setTaxRate(approvalBiddingItem.getTaxRate());
                if (null != contractMaterial.getAmount() &&
                        contractMaterial.getAmount().compareTo(BigDecimal.ZERO) != 0 &&
                        null != contractMaterial.getTaxRate() &&
                        contractMaterial.getTaxRate().compareTo(BigDecimal.ZERO) != 0
                ) {
                    double taxRate = contractMaterial.getTaxRate().doubleValue();
                    double amount = contractMaterial.getAmount().doubleValue();
                    double unAmount = amount / (taxRate / 100 + 1);
                    contractMaterial.setUnAmount(new BigDecimal(unAmount));
                }
                contractMaterial.setStartDate(approvalBiddingItem.getStartTime());
                contractMaterial.setEndDate(approvalBiddingItem.getEndTime());
                // TODO ???????????? , ????????????
                RequirementHead requirementHead = pmClient.queryByRequirementHead(new RequirementHead().setRequirementHeadNum(approvalBiddingItem.getPurchaseRequestNum()));
                if (null != requirementHead) {
                    contractMaterial.setItemNumber(requirementHead.getCeeaProjectNum());
                    contractMaterial.setItemName(requirementHead.getCeeaProjectName());
                }
                contractMaterialList.add(contractMaterial);

            });

            contractDTO.setContractHead(contractHead)
                    .setContractMaterials(contractMaterialList)
                    .setPayPlans(payPlans);
            try {
                ContractHead ch = contractClient.buyerSaveOrUpdateContractDTOSecond(contractDTO);
                /*???????????????*/
                List<ApprovalBiddingItem> approvalBiddingItemList = new ArrayList<>();
                list.forEach(item -> {
                    ApprovalBiddingItem approvalBiddingItem = new ApprovalBiddingItem()
                            .setApprovalBiddingItemId(item.getApprovalBiddingItemId())
                            .setFromContractCode(ch.getContractCode())
                            .setFromContractId(ch.getContractHeadId());
                    approvalBiddingItemList.add(approvalBiddingItem);
                });
                iApprovalBiddingItemService.updateBatchById(approvalBiddingItemList);

            } catch (Exception e) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                log.error("??????????????????????????????", e);
                throw new BaseException(e.getMessage());
            }
        });
    }


    public boolean ifCommonGroup(List<ApprovalBiddingItemPaymentTerm> paymentTermList1, List<ApprovalBiddingItemPaymentTerm> paymentTermList2) {
        if (paymentTermList1.size() != paymentTermList2.size()) {
            return false;
        }
        if (!paymentTermList1.containsAll(paymentTermList2)) {
            return false;
        }
        return paymentTermList2.containsAll(paymentTermList1);
    }


    /*test*/
    /*public static void main(String[] args) {
        ApprovalBiddingItemPaymentTerm paymentTerm1 = new ApprovalBiddingItemPaymentTerm()
                .setPaymentTerm("term1")
                .setPaymentDayCode("dayCode1")
                .setPaymentWay("way1");
        ApprovalBiddingItemPaymentTerm paymentTerm2 = new ApprovalBiddingItemPaymentTerm()
                .setPaymentTerm("term2")
                .setPaymentDayCode("dayCode2")
                .setPaymentWay("way2");
        ApprovalBiddingItemPaymentTerm paymentTerm3 = new ApprovalBiddingItemPaymentTerm()
                .setPaymentTerm("term3")
                .setPaymentDayCode("dayCode3")
                .setPaymentWay("way3");
        List<ApprovalBiddingItemPaymentTerm> approvalBiddingItemPaymentTermList = new ArrayList<>();
        approvalBiddingItemPaymentTermList.add(paymentTerm1);
        approvalBiddingItemPaymentTermList.add(paymentTerm2);
        approvalBiddingItemPaymentTermList.add(paymentTerm3);

        ApprovalBiddingItemPaymentTerm paymentTerm4 = new ApprovalBiddingItemPaymentTerm()
                .setPaymentTerm("term1")
                .setPaymentDayCode("dayCode1")
                .setPaymentWay("way1");
        ApprovalBiddingItemPaymentTerm paymentTerm5 = new ApprovalBiddingItemPaymentTerm()
                .setPaymentTerm("term2")
                .setPaymentDayCode("dayCode2")
                .setPaymentWay("way2");
        ApprovalBiddingItemPaymentTerm paymentTerm6 = new ApprovalBiddingItemPaymentTerm()
                .setPaymentTerm("term3")
                .setPaymentDayCode("dayCode3")
                .setPaymentWay("way3");
        List<ApprovalBiddingItemPaymentTerm> approvalBiddingItemPaymentTermList2 = new ArrayList<>();
        approvalBiddingItemPaymentTermList2.add(paymentTerm4);
        approvalBiddingItemPaymentTermList2.add(paymentTerm5);
        approvalBiddingItemPaymentTermList2.add(paymentTerm6);

        System.out.println(approvalBiddingItemPaymentTermList.containsAll(approvalBiddingItemPaymentTermList2));


    }*/

    private BigDecimal calculateNoTaxPrice(BigDecimal taxPrice, BigDecimal taxRate) {
        //??????????????????,?????????*???1+?????????=??????
        BigDecimal noTaxPrice = taxPrice.divide(BigDecimal.ONE.add(taxRate), 8, BigDecimal.ROUND_HALF_DOWN);
        return noTaxPrice;
    }
}
