package com.midea.cloud.srm.pr.requirement.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.pm.po.PriceSourceTypeEnum;
import com.midea.cloud.common.enums.pm.pr.requirement.IfDistributionVendor;
import com.midea.cloud.common.enums.pm.pr.requirement.RequirementApplyStatus;
import com.midea.cloud.common.enums.review.CategoryStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.BeanCopyUtil;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.common.utils.NamedThreadFactory;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.contract.ContractClient;
import com.midea.cloud.srm.feign.inq.InqClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.dict.entity.DictItem;
import com.midea.cloud.srm.model.base.formula.dto.calculate.CalculatePriceForOrderResult;
import com.midea.cloud.srm.model.base.formula.dto.calculate.SeaFoodFormulaCalculateParam;
import com.midea.cloud.srm.model.base.quotaorder.QuotaHead;
import com.midea.cloud.srm.model.base.quotaorder.QuotaLine;
import com.midea.cloud.srm.model.base.quotaorder.dto.QuotaParamDto;
import com.midea.cloud.srm.model.cm.contract.dto.ContractQueryDTO;
import com.midea.cloud.srm.model.cm.contract.entity.PayPlan;
import com.midea.cloud.srm.model.cm.contract.vo.ContractVo;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibrary;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibraryPaymentTerm;
import com.midea.cloud.srm.model.pm.po.dto.NetPriceQueryDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.RequirementManageDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.VendorDistDesc;
import com.midea.cloud.srm.model.supplier.info.entity.OrgCategory;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderPaymentProvision;
import com.midea.cloud.srm.po.order.service.IOrderService;
import com.midea.cloud.srm.pr.division.service.IDivisionCategoryService;
import com.midea.cloud.srm.pr.documents.service.ISubsequentDocumentsService;
import com.midea.cloud.srm.pr.requirement.dto.TransferOrderVerifyDTO;
import com.midea.cloud.srm.pr.requirement.mapper.RequirementLineMapper;
import com.midea.cloud.srm.pr.requirement.service.IRequirementAttachService;
import com.midea.cloud.srm.pr.requirement.service.IRequirementLineService;
import com.midea.cloud.srm.pr.requirement.service.IRequirementTransferOrderService;
import com.midea.cloud.srm.pr.shopcart.service.IShopCartService;
import com.midea.cloud.srm.pr.vendordistdesc.service.IVendorDistDescService;
import com.midea.cloud.srm.pr.workflow.PurchaseRequirementFlow;
import com.midea.cloud.srm.ps.http.fssc.service.IFSSCReqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ????????????
 * </pre>
 *
 * @author chenjj120@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/11/23 ?????? 03:25
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class RequirementTransferOrderServiceImpl implements IRequirementTransferOrderService {

    private static final String CONTRACT_CUT_SIGN = "|??????|"; //????????????????????????bug????????????
    @Autowired
    private BaseClient baseClient;
    @Resource
    private RbacClient rbacClient;
    @Autowired
    private InqClient inqClient;
    @Autowired
    private IRequirementLineService iRequirementLineService;
    @Resource
    private RequirementLineMapper requirementLineMapper;
    @Autowired
    private IRequirementAttachService iRequirementAttachService;
    @Autowired
    private IDivisionCategoryService iDivisionCategoryService;
    @Autowired
    private SupplierClient supplierClient;
    @Autowired
    private IFSSCReqService iFSSCReqService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IShopCartService iShopCartService;
    @Autowired
    private ContractClient contractClient;
    @Autowired
    private ISubsequentDocumentsService iSubsequentDocumentsService;
    @Resource
    private PurchaseRequirementFlow purchaseRequirementFlow;
    @Resource
    private IVendorDistDescService iVendorDistDescService;

    private final ThreadPoolExecutor ioThreadPool;
    private final ForkJoinPool calculateThreadPool;

    public RequirementTransferOrderServiceImpl() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        ioThreadPool = new ThreadPoolExecutor(cpuCount * 2 + 1, cpuCount * 2 + 1,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue(),
                new NamedThreadFactory("?????????-http-sender", true), new ThreadPoolExecutor.CallerRunsPolicy());
        calculateThreadPool = new ForkJoinPool(cpuCount + 1);
    }

    /************************************************step 1.???????????????-???????????????-????????????*****************************************************/
    @Override
    public Collection<RequirementManageDTO> createPurchaseOrderNew(List<RequirementManageDTO> requirementManageDTOS, LocalDate from, LocalDate to) {
        Map<String, BigDecimal> keyNumVendor = new HashMap<>();
        Map<String, BigDecimal> keyNumItem = new HashMap<>();
        Map<String, BigDecimal> keyNumItemAlready = new HashMap<>();
        List<RequirementManageDTO> resultNew = new ArrayList<>();

        //1.???????????????????????????
        checkRequirementLineStatus(requirementManageDTOS);
        //2.??????????????????
        TransferOrderVerifyDTO verifyDTO = syncLoadVerifyData(requirementManageDTOS, from, to);

        Map<String, Map<String, List<OrgCategory>>> orgCatMap = verifyDTO.getOrgCatMapT();
        Map<String, Map<String, Map<String, List<PriceLibrary>>>> orgInvItemMap = verifyDTO.getPriceLibraryMapT();
        //?????????????????????,?????????????????????????????????
        List<RequirementManageDTO> resultT = new LinkedList<>();
        if (!CollectionUtils.isEmpty(verifyDTO.getDirectRequirement())) {
            resultT = verifyDirectData(verifyDTO);
        }
        if (CollectionUtils.isEmpty(requirementManageDTOS)) {
            return resultT;
        }
//        List<RequirementManageDTO> result = resultT;
        List<RequirementManageDTO> result = new LinkedList<>();
        //???????????????????????????????????????0
        requirementManageDTOS = requirementManageDTOS.stream()
                .filter(item -> Objects.nonNull(item.getOrderQuantity()) && item.getOrderQuantity().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        //???????????????-????????????map
        Map<String, Set<String>> buCodeMap = new HashMap<>();
        verifyDTO.getRequireEntity().stream().map(DictItem::getDictItemCode).map(e -> e.split("-"))
                .forEach(l -> {
                    if (buCodeMap.containsKey(l[0])) {
                        buCodeMap.get(l[0]).add(l[1]);
                    } else {
                        HashSet<String> set = new HashSet<>();
                        set.add(l[1]);
                        buCodeMap.put(l[0], set);
                    }
                });
        verifyDTO.setBuCodeMap(buCodeMap);
        for (RequirementManageDTO requirementLine : requirementManageDTOS) {
            /**
             * ????????????????????????????????????????????????, ??????????????????
             */
            // ????????????????????????
            String ifDistributionVendor = requirementLine.getIfDistributionVendor();
            Assert.isTrue(!IfDistributionVendor.ALLOCATING.name().equals(ifDistributionVendor),"????????????????????????\"????????????\"???????????????????????????");
            if (IfDistributionVendor.N.name().equals(ifDistributionVendor) ||
                    IfDistributionVendor.ALLOCATING_FAILED.name().equals(ifDistributionVendor)) {
                // ?????????????????????
                oldDistributionVendor(verifyDTO, orgCatMap, orgInvItemMap, result, buCodeMap, requirementLine);
            }else {
                Assert.notNull(requirementLine.getOrgId(),"??????????????????");
                Assert.notNull(requirementLine.getMaterialId(),"??????????????????");
                Assert.notNull(requirementLine.getRequirementDate(),"????????????????????????");
                // ??????????????????
                QuotaHead quotaHead = baseClient.getQuotaHeadByOrgIdItemIdDate(QuotaParamDto.builder().
                        orgId(requirementLine.getOrgId()).
                        itemId(requirementLine.getMaterialId()).
                        requirementDate(DateUtil.localDateToDate(requirementLine.getRequirementDate())).build());
                Assert.notNull(quotaHead,requirementLine.getRequirementHeadNum()+":???????????????????????????;");
                List<QuotaLine> quotaLineList = quotaHead.getQuotaLineList();
                Assert.notNull(quotaLineList,requirementLine.getRequirementHeadNum()+":???????????????????????????;");
                // ?????????????????????
                BigDecimal sumNumber = quotaLineList.stream().map(quotaLine -> null != quotaLine.getAllocatedAmount() ? quotaLine.getAllocatedAmount() : BigDecimal.ZERO).reduce(BigDecimal::add).orElseGet(() -> BigDecimal.ZERO);
                double num = sumNumber.doubleValue();

                String keyItem = String.valueOf(requirementLine.getOrgId())+requirementLine.getMaterialId();
                keyNumItemAlready.put(keyItem,sumNumber);

                Map<Long, QuotaLine> quotaLineMap = quotaLineList.stream().collect(Collectors.toMap(QuotaLine::getCompanyId, Function.identity(),(k1,k2)->k1));
                // ?????????????????????????????????
                List<VendorDistDesc> vendorDistDescs = iVendorDistDescService.list(new QueryWrapper<>(new VendorDistDesc().setRequirementLineId(requirementLine.getRequirementLineId())));
                if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(vendorDistDescs)){
//						vendorDistDescs = vendorDistDescs.stream().filter(vendorDistDesc -> null != vendorDistDesc.getPlanAmount() && vendorDistDesc.getPlanAmount().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
                    // ???????????????+?????? ???????????????????????????
                    List<PriceLibrary> priceLibraryList = inqClient.listPriceLibrary(new NetPriceQueryDTO().setMaterialId(requirementLine.getMaterialId()).setOrganizationId(requirementLine.getOrgId()).setRequirementDate(DateUtil.localDateToDate(requirementLine.getRequirementDate())));
                    Assert.notNull(priceLibraryList,requirementLine.getOrgName()+"+"+requirementLine.getMaterialName()+",???????????????????????????????????????????????????");
                    Map<Long, PriceLibrary> libraryMap = priceLibraryList.stream().collect(Collectors.toMap(PriceLibrary::getVendorId, Function.identity(), (k1, k2) -> k1));
                    vendorDistDescs.forEach(vendorDistDesc -> {
                        Long companyId = vendorDistDesc.getCompanyId();
                        // ????????????
                        PriceLibrary priceLibrary = libraryMap.get(companyId);
                        Assert.notNull(priceLibrary,requirementLine.getRequirementHeadNum()+"+"+vendorDistDesc.getCompanyName()+",???????????????????????????????????????");
                        RequirementManageDTO requirementManageDTO = new RequirementManageDTO();
                        BeanCopyUtil.copyProperties(requirementManageDTO, requirementLine);
                        // ????????????
                        requirementManageDTO.
                                setTaxPrice(priceLibrary.getTaxPrice()).
                                setTaxRate(priceLibrary.getTaxPrice()).
                                setTaxKey(priceLibrary.getTaxKey()).
                                setTaxRate(new BigDecimal(priceLibrary.getTaxRate())).
                                setUnit(priceLibrary.getUnit()).
                                setUnitCode(priceLibrary.getUnitCode()).
                                setCurrencyId(priceLibrary.getCurrencyId()).
                                setCurrencyCode(priceLibrary.getCurrencyCode()).
                                setCurrencyName(priceLibrary.getCurrencyName()).
                                setCeeaDeliveryPlace(priceLibrary.getCeeaArrivalPlace()).
                                setContractNum(priceLibrary.getContractCode()).
                                setNotaxPrice(priceLibrary.getNotaxPrice());
                        // ????????????????????????
                        QuotaLine quotaLine = quotaLineMap.get(vendorDistDesc.getCompanyId());
                        // ???????????????
                        BigDecimal orderQuantity = requirementLine.getOrderQuantity();
                        requirementManageDTO.setRequirementQuantity(requirementLine.getRequirementQuantity()).
                                setVendorId(vendorDistDesc.getCompanyId()).
                                setVendorCode(vendorDistDesc.getCompanyCode()).
                                setVendorName(vendorDistDesc.getCompanyName()).
                                setRequirementLineId(requirementLine.getRequirementLineId()).
                                setRequirementHeadId(requirementLine.getRequirementHeadId()).
                                setRequirementHeadNum(requirementLine.getRequirementHeadNum()).
                                setRowNum(requirementLine.getRowNum()).
                                setRequirementDate(requirementLine.getRequirementDate()).
                                setReceivedFactory(requirementLine.getReceivedFactory()).
                                setOrderQuantity(requirementLine.getOrderQuantity()).
                                setFullPathId(requirementLine.getFullPathId()).
                                setMaterialId(requirementLine.getMaterialId()).
                                setQuotaProportion(vendorDistDesc.getQuota().divide(BigDecimal.valueOf(100))).
                                setAlreadyNum(null != quotaLine.getAllocatedAmount()?quotaLine.getAllocatedAmount():BigDecimal.ZERO).
                                setOrderQuota(vendorDistDesc.getPlanAmount().subtract(vendorDistDesc.getActualAmount())).
                                setThisOrderQuantity(requirementManageDTO.getOrderQuota()).
                                setQuotaProportion(null != vendorDistDesc.getPlanAmount() &&
                                        vendorDistDesc.getPlanAmount().compareTo(BigDecimal.ZERO) >0 &&
                                        null != orderQuantity &&
                                        orderQuantity.compareTo(BigDecimal.ZERO) > 0?
                                        BigDecimal.valueOf(requirementManageDTO.getOrderQuota().doubleValue()/orderQuantity.doubleValue()):BigDecimal.ZERO).
                                setAllocationQuotaFlag(1)
                        ;
                        String key = String.valueOf(requirementLine.getOrgId())+requirementLine.getMaterialId()+vendorDistDesc.getCompanyId();
                        BigDecimal keyVuale = keyNumVendor.get(key);
                        keyNumVendor.put(key,null != keyVuale?keyVuale.add(null !=vendorDistDesc.getPlanAmount()?vendorDistDesc.getPlanAmount():BigDecimal.ZERO):vendorDistDesc.getPlanAmount());

                        BigDecimal keyVualeItem = keyNumItem.get(keyItem);
                        keyNumItem.put(keyItem,null != keyVualeItem?keyVualeItem.add(null !=vendorDistDesc.getPlanAmount()?vendorDistDesc.getPlanAmount():BigDecimal.ZERO):vendorDistDesc.getPlanAmount());

                        // ??????????????????(?????????????????????????????????????????????????????????)
                        BigDecimal allocatedAmount = quotaLine.getAllocatedAmount();
                        // ????????????
                        double num1 = null != allocatedAmount?allocatedAmount.doubleValue():0;
                        // ????????????????????????
                        if (0  != num) {
                            requirementManageDTO.setAlreadyQuota(BigDecimal.valueOf(num1/num));
                        }else {
                            requirementManageDTO.setAlreadyQuota(BigDecimal.ZERO);
                        }
                        result.add(requirementManageDTO);
                    });
                }
            }
        }
        // ????????????????????????/???????????????
        if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(result)){
            result.forEach(recommendVendorVO -> {
                if(1 == recommendVendorVO.getAllocationQuotaFlag()){
                    String key = String.valueOf(recommendVendorVO.getOrgId())+recommendVendorVO.getMaterialId()+recommendVendorVO.getVendorId();
                    String keyItem = String.valueOf(recommendVendorVO.getOrgId())+recommendVendorVO.getMaterialId();
                    BigDecimal bigDecimal1 = keyNumVendor.get(key);
                    // ???????????????????????????
                    double num3 = null != bigDecimal1?bigDecimal1.doubleValue():0;
                    recommendVendorVO.setTotalDistribution(BigDecimal.valueOf(num3));
                    // ????????????????????????
                    BigDecimal keyVualeItem = keyNumItem.get(keyItem);
                    double num1 = null !=keyVualeItem? keyVualeItem.doubleValue():0;
                    BigDecimal bigDecimal = keyNumItemAlready.get(keyItem);
                    // ?????????????????????
                    double num2 = null != bigDecimal?bigDecimal.doubleValue():0;
                    BigDecimal alreadyNum = recommendVendorVO.getAlreadyNum();
                    // ????????????????????????
                    double num4 = null != alreadyNum?alreadyNum.doubleValue():0;
                    // ???????????????=?????????????????????????????????+??????????????????????????????/????????????)
                    double num5 = (num4+num3)/(num1+num2);
                    recommendVendorVO.setAfterQuota(BigDecimal.valueOf(num5));
                }
            });
        }

        //??????????????????????????????
        calcSeaFoodFormula(result);

        // ?????? ???????????????+?????????
        result.sort(Comparator.comparingInt(o -> o.getRequirementHeadNum().hashCode()));
        Map<String, List<RequirementManageDTO>> collect = result.stream().collect(Collectors.groupingBy(RequirementManageDTO::getRequirementHeadNum));
        collect.forEach((s, recommendVendorVOS) -> {
            recommendVendorVOS.sort(Comparator.comparingInt(RequirementManageDTO::getRowNum));
            resultNew.addAll(recommendVendorVOS);
        });

        return resultNew;
    }

    public void oldDistributionVendor(TransferOrderVerifyDTO verifyDTO, Map<String, Map<String, List<OrgCategory>>> orgCatMap, Map<String, Map<String, Map<String, List<PriceLibrary>>>> orgInvItemMap, List<RequirementManageDTO> result, Map<String, Set<String>> buCodeMap, RequirementManageDTO requirementLine) {
        //????????????code
        boolean hasSupplier = false;
        Map<String, List<OrgCategory>> categoryMap = orgCatMap.get(requirementLine.getOrgCode());
        if (Objects.nonNull(categoryMap)) {
            List<OrgCategory> categories = categoryMap.get(requirementLine.getCategoryCode());
            hasSupplier = categories.stream().anyMatch(e -> {
                        boolean vendorEquals = true;
                        if (StringUtils.isNotBlank(requirementLine.getVendorCode())) {
                            vendorEquals = Objects.equals(requirementLine.getVendorCode(), e.getCompanyCode());
                        }
                        boolean effectVendor = Objects.equals(e.getServiceStatus(), CategoryStatus.YELLOW.name())
                                || Objects.equals(e.getServiceStatus(), CategoryStatus.GREEN.name())
                                || Objects.equals(e.getServiceStatus(), CategoryStatus.ONE_TIME.name())
                                || Objects.equals(e.getServiceStatus(), CategoryStatus.VERIFY.name());
                        return effectVendor && vendorEquals;
                    }
            );
        }
        if (!hasSupplier) {
            throw new BaseException(String.format("????????????[%s]??????-??????????????????????????????????????????", requirementLine.getRowNum()));
        }
        Map<String, List<PriceLibrary>> priceLibraryMap = getPriceMap(verifyDTO, orgInvItemMap, buCodeMap, requirementLine);
        boolean hasEffectPrice = false;
        List<PriceLibrary> effectPriceList = new ArrayList<>();
        if (Objects.nonNull(priceLibraryMap)) {
            hasEffectPrice = isHasEffectPrice(priceLibraryMap, requirementLine, effectPriceList, verifyDTO.getCodeSet());
        }
        //??????????????????-
        if (hasEffectPrice) {
            List<RequirementManageDTO> effectivePriceProcessResult = processWithEffectivePriceNew(verifyDTO, requirementLine, effectPriceList);
            if (!effectivePriceProcessResult.isEmpty()) {
                result.addAll(effectivePriceProcessResult);
            }
        } else {
            List<RequirementManageDTO> noEffectivePriceProcessResult = processWithNotEffectivePrice(verifyDTO, requirementLine);
            if (!noEffectivePriceProcessResult.isEmpty()) {
                result.addAll(noEffectivePriceProcessResult);
            }
        }
    }


    /**
     * ?????????????????????????????????
     *
     * @param requirementManageDTOS
     */
    private void calcSeaFoodFormula(List<RequirementManageDTO> requirementManageDTOS) {
        List<RequirementManageDTO> seaFoodFormula = requirementManageDTOS.stream()
                .filter(r -> YesOrNo.YES.getValue().equals(r.getIsSeaFoodFormula()))
                .collect(Collectors.toList());
        if (seaFoodFormula.size() == 0) {
            return;
        }

        //???????????????
        List<SeaFoodFormulaCalculateParam> seaFoodFormulaCalculateParams = requirementManageDTOS.stream()
                .map(requirementManageDTO -> {
                    String calculateId = UUID.randomUUID().toString();
                    requirementManageDTO.setCalculateId(calculateId);
                    SeaFoodFormulaCalculateParam param = new SeaFoodFormulaCalculateParam();
                    param.setEssentialFactorValues(requirementManageDTO.getEssentialFactorValues());
                    param.setFormulaId(requirementManageDTO.getCeeaFormulaId());
                    param.setPriceJSON(requirementManageDTO.getPriceJson());
                    param.setMaterialId(requirementManageDTO.getMaterialId());
                    param.setCalculateId(requirementManageDTO.getCalculateId());
                    return param;
                }).collect(Collectors.toList());

        Map<String, CalculatePriceForOrderResult> calcResult = baseClient.calculatePriceForOrderBatch(seaFoodFormulaCalculateParams);
        for (RequirementManageDTO requirementManageDTO : requirementManageDTOS) {
            //???????????????
            CalculatePriceForOrderResult tempResult = calcResult.get(requirementManageDTO.getCalculateId());
            BigDecimal seaPrice = tempResult.getCalcResult();
            requirementManageDTO.setTaxPrice(seaPrice);
            requirementManageDTO.setCeeaFormulaResult(JSONObject.toJSONString(tempResult.getFormulaAndParam()));
        }
    }

    /**
     * ???????????????--????????????????????????
     */
    private void checkRequirementLineStatus(List<RequirementManageDTO> requirementManageDTOS) {
        if (CollectionUtils.isEmpty(requirementManageDTOS)) {
            throw new BaseException("???????????????????????????");
        }
        for (int i = requirementManageDTOS.size() - 1; i >= 0; i--) {
            RequirementManageDTO requirementManageDTO = requirementManageDTOS.get(i);
            if (Objects.equals(requirementManageDTO.getCeeaIfDirectory(), "Y")) {
                continue;
            }
            if (!Objects.equals(requirementManageDTO.getApplyStatus(), RequirementApplyStatus.ASSIGNED.getValue())) {
                throw new BaseException(String.format("????????????[%s]?????????[?????????]???????????????????????????", requirementManageDTO.getRowNum()));
            }
            if (Objects.equals(requirementManageDTO.getHaveEffectivePrice(), YesOrNo.NO.getValue())) {
                throw new BaseException(String.format("????????????[%s]??????-????????????????????????????????????", requirementManageDTO.getRowNum()));
            }
            if (Objects.equals(requirementManageDTO.getHaveSupplier(), YesOrNo.NO.getValue())) {
                throw new BaseException(String.format("????????????[%s]??????-??????????????????????????????????????????", requirementManageDTO.getRowNum()));
            }
            if (Objects.isNull(requirementManageDTO.getCeeaPerformUserId())) {
                throw new BaseException(String.format("????????????[%s]??????-???????????????????????????????????????", requirementManageDTO.getRowNum()));
            }
            if (Objects.equals(requirementManageDTO.getIfHold(), "Y")) {
                throw new BaseException(String.format("????????????[%s]?????????????????????????????????", requirementManageDTO.getRowNum()));
            }
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param requirementManageDTOS
     */
    private TransferOrderVerifyDTO syncLoadVerifyData(List<RequirementManageDTO> requirementManageDTOS, LocalDate from, LocalDate to) {
        //1.????????????
        List<DictItem> priceUpdateCategary = baseClient.listDictItemByDictCode("PRICE_UPDATE_CATEGARY");
        Set<String> codeSet = priceUpdateCategary.stream().map(DictItem::getDictItemCode).collect(Collectors.toSet());

        List<OrgCategory> orgCategoryParam = new LinkedList<>();
        List<PriceLibrary> priceLibraryParams = new LinkedList<>();
        List<ContractQueryDTO> contractQueryParams = new LinkedList<>();
        Set<String> orgCodes = new HashSet<>();
        requirementManageDTOS.forEach(r -> {
            ContractQueryDTO queryDTO = new ContractQueryDTO();
            queryDTO.setInvCode(r.getOrganizationCode());
            queryDTO.setMaterialCode(r.getMaterialCode());
            queryDTO.setOrgCode(r.getOrgCode());
            contractQueryParams.add(queryDTO);
            orgCodes.add(r.getOrgCode());
            orgCategoryParam.add(new OrgCategory()
                    .setOrgId(r.getOrgId())
                    .setCategoryId(r.getCategoryId()));

            PriceLibrary e = new PriceLibrary()
                    .setCeeaOrgCode(r.getOrgCode())
                    .setCeeaOrganizationCode(r.getOrganizationCode())
                    .setItemCode(r.getMaterialCode());
            //??????code???????????????????????????
            if (codeSet.contains(r.getCategoryCode())) {
                e.setItemDesc(r.getMaterialName());
            }
            priceLibraryParams.add(e);
        });
        List<RequirementManageDTO> shouldCheckDirect = truncatedDirectory(requirementManageDTOS);

        Map<String, Object> paramContractMap = new HashMap<>();
        paramContractMap.put("queryList", contractQueryParams);
        paramContractMap.put("frame", shouldCheckDirect.isEmpty() ? YesOrNo.NO.getValue() : "");
        paramContractMap.put("from", from);
        paramContractMap.put("to", to);
        Map<String, Object> paramPriceMap = new HashMap<>();
        paramPriceMap.put("priceList", priceLibraryParams);
        paramPriceMap.put("from", from);
        paramPriceMap.put("to", to);
        CountDownLatch countDownLatch = new CountDownLatch(5);
        //??????????????????
        Map<String, Map<String, List<OrgCategory>>> orgCatMapT = null;
        //??????????????????????????????????????????????????????
        Map<String, Map<String, List<ContractVo>>> orgSupplierMapT = null;
        //???????????????????????????????????????????????????????????????
        Map<String, Map<String, Map<String, List<PriceLibrary>>>> invItemMapT = null;
        //?????????????????????????????????
        Map<String, Map<String, Map<String, List<ContractVo>>>> contractInvAndMaterialMapT = null;
        //???????????????????????????
        List<DictItem> requireEntity = null;

        try {
            orgCatMapT = CompletableFuture.supplyAsync(() -> supplierClient.listOrgCategory(orgCategoryParam), ioThreadPool)
                    .exceptionally(e -> Collections.emptyList())
                    .thenApplyAsync(e -> {
                        Map<String, Map<String, List<OrgCategory>>> result = new HashMap<>();
                        if (!CollectionUtils.isEmpty(e)) {
                            result = e.stream().collect(Collectors.groupingBy(OrgCategory::getOrgCode, Collectors.groupingBy(OrgCategory::getCategoryCode)));
                        }
                        countDownLatch.countDown();
                        return result;
                    }, calculateThreadPool).exceptionally(throwable -> {
                        Map<String, Map<String, List<OrgCategory>>> result = Collections.emptyMap();
                        countDownLatch.countDown();
                        return result;
                    }).get();


            //????????????????????? ?????????????????????????????????????????????????????????????????????
            invItemMapT = CompletableFuture.supplyAsync(() -> inqClient.listPriceLibraryWithPaymentTerm(paramPriceMap), ioThreadPool)
                    .thenApplyAsync(e -> {
                        Map<String, Map<String, Map<String, List<PriceLibrary>>>> result = new HashMap<>();
                        if (!CollectionUtils.isEmpty(e)) {
                            result = e.stream().collect(Collectors.groupingBy(PriceLibrary::getCeeaOrgCode, Collectors.groupingBy(PriceLibrary::getCeeaOrganizationCode, Collectors.groupingBy(PriceLibrary::getItemCode))));
                        }
                        countDownLatch.countDown();
                        return result;
                    }, calculateThreadPool).exceptionally(throwable -> {
                        Map<String, Map<String, Map<String, List<PriceLibrary>>>> result = Collections.emptyMap();
                        countDownLatch.countDown();
                        return result;
                    }).get();

            //??????????????????????????????????????????????????????
            contractInvAndMaterialMapT = CompletableFuture.supplyAsync(() -> contractClient.listEffectiveContractByInvCodeAndMaterialCodeWithPartner(paramContractMap), ioThreadPool).thenApplyAsync(e -> {
                Map<String, Map<String, Map<String, List<ContractVo>>>> result = new HashMap<>();
                if (!CollectionUtils.isEmpty(e)) {
                    result = e.stream().collect(Collectors.groupingBy(ContractVo::getBuCode, Collectors.groupingBy(ContractVo::getInvCode, Collectors.groupingBy(ContractVo::getMaterialCode))));
                }
                countDownLatch.countDown();
                return result;
            }, calculateThreadPool).exceptionally(throwable -> {
                Map<String, Map<String, Map<String, List<ContractVo>>>> result = Collections.emptyMap();
                countDownLatch.countDown();
                return result;
            }).get();

            orgSupplierMapT = CompletableFuture.supplyAsync(() -> contractClient.listEffectiveContractByOrgCodesWithPartner(orgCodes), ioThreadPool)
                    .exceptionally(e -> Collections.emptyList())
                    .thenApplyAsync(e -> {
                        Map<String, Map<String, List<ContractVo>>> result = new HashMap<>();
                        if (!CollectionUtils.isEmpty(e)) {
                            result = e.stream().collect(Collectors.groupingBy(ContractVo::getBuCode, Collectors.groupingBy(ContractVo::getHeadVendorCode)));
                        }
                        countDownLatch.countDown();
                        return result;
                    }).get();
            requireEntity = CompletableFuture.supplyAsync(() -> {
                List<DictItem> result = baseClient.listDictItemByDictCode("REQUIR_ENTITY");
                countDownLatch.countDown();
                return result;
            }).get();
            countDownLatch.await(30, TimeUnit.SECONDS);//30?????????
        } catch (Exception e) {
            log.error("????????????????????????", e);
            throw new BaseException("????????????????????????:" + e.getLocalizedMessage());
        }
        TransferOrderVerifyDTO verifyDTO = new TransferOrderVerifyDTO();
        verifyDTO.setNotFrameContractMapT(contractInvAndMaterialMapT);
        verifyDTO.setPriceLibraryMapT(invItemMapT);
        verifyDTO.setOrgCatMapT(orgCatMapT);
        verifyDTO.setFrameContractMapT(orgSupplierMapT);
        verifyDTO.setDirectRequirement(shouldCheckDirect);          //????????????????????????
        verifyDTO.setNotDirectRequirement(requirementManageDTOS);   //???????????????????????????
        verifyDTO.setCodeSet(codeSet);
        verifyDTO.setRequireEntity(requireEntity);
        return verifyDTO;
    }

    /**
     * ???????????????--????????????????????????
     *
     * @param priceLibraryMap
     * @param requirementLine
     * @param effectPriceList
     * @return
     */
    private boolean isHasEffectPrice(Map<String, List<PriceLibrary>> priceLibraryMap,
                                     RequirementManageDTO requirementLine,
                                     List<PriceLibrary> effectPriceList, Set<String> codeSet) {
        boolean hasEffectPrice = false;
        List<PriceLibrary> priceLibraries = priceLibraryMap.get(requirementLine.getMaterialCode());
        if (!CollectionUtils.isEmpty(priceLibraries)) {
            for (PriceLibrary priceLibrary : priceLibraries) {
                ///???????????????????????????????????????
                //???????????????????????????
                boolean nameEquals = true;
                if (StringUtils.isNotBlank(requirementLine.getMaterialName()) && codeSet.contains(requirementLine.getCategoryCode())) {
                    nameEquals = Objects.equals(priceLibrary.getItemDesc(), requirementLine.getMaterialName());
                }
                //???????????????id??????????????????
                boolean vendorEquals = true;
                if (StringUtils.isNotBlank(requirementLine.getVendorCode())) {
                    vendorEquals = Objects.equals(priceLibrary.getVendorCode(), requirementLine.getVendorCode());
                }
                //????????????-????????????????????????
                boolean notEmptyPrice = Objects.nonNull(priceLibrary.getNotaxPrice())
                        && Objects.nonNull(priceLibrary.getTaxPrice())
                        && StringUtils.isNotBlank(priceLibrary.getTaxKey())
                        && StringUtils.isNotBlank(priceLibrary.getTaxRate())
                        && StringUtils.isNotBlank(priceLibrary.getCurrencyCode());
                if (vendorEquals && nameEquals && notEmptyPrice) {
                    //?????????????????????
                    effectPriceList.add(priceLibrary);
                }
            }
        }
        //??????????????????,?????????????????????????????????????????????????????????????????????
        hasEffectPrice = !CollectionUtils.isEmpty(effectPriceList);

        return hasEffectPrice;
    }

    /**
     * ????????????--????????? ?????????????????????????????????
     *
     * @param verifyDTO
     * @param requirementLine
     * @param effectPriceList
     * @return
     */
    private List<RequirementManageDTO> processWithEffectivePrice(TransferOrderVerifyDTO verifyDTO,
                                                                 RequirementManageDTO requirementLine,
                                                                 List<PriceLibrary> effectPriceList) {
        List<RequirementManageDTO> result = new ArrayList<>();
        Map<String, Map<String, List<ContractVo>>> frameContractMapT = verifyDTO.getFrameContractMapT();
        //?????????????????????
        Function<PriceLibrary, String> getPlace = s -> Objects.isNull(s.getCeeaArrivalPlace()) ? "" : s.getCeeaArrivalPlace();
        //?????????vendorCode??????????????????????????????
        Map<String, Map<String, List<PriceLibrary>>> vendorPlaceMap = effectPriceList.stream().collect(Collectors.groupingBy(PriceLibrary::getVendorCode
                , Collectors.groupingBy(getPlace)
        ));
        for (Map.Entry<String, Map<String, List<PriceLibrary>>> placeMap : vendorPlaceMap.entrySet()) {
            for (Map.Entry<String, List<PriceLibrary>> place : placeMap.getValue().entrySet()) {
                for (PriceLibrary effect : place.getValue()) {
                    RequirementManageDTO requirementManageDTO = assignValueFromEffectPrice(requirementLine, effect);
                    //?????????????????????????????????????????????
                    if (Objects.isNull(frameContractMapT)) {
                        throw new BaseException(String.format("????????????[%s]?????????????????????????????????????????????-[%s]?????????????????????,????????????????????????", requirementLine.getRowNum(), requirementLine.getOrgName()));
                    }
                    Map<String, List<ContractVo>> vendorMap = getFrameWorkMap(frameContractMapT, verifyDTO, requirementManageDTO.getOrgCode());
                    if (Objects.isNull(vendorMap)) {
                        throw new BaseException(String.format("????????????[%s]?????????????????????????????????????????????-[%s]?????????????????????,????????????????????????", requirementLine.getRowNum(), requirementLine.getOrgName()));
                    }
                    List<ContractVo> contractVos = vendorMap.get(effect.getVendorCode());
                    if (!CollectionUtils.isEmpty(contractVos)) {
                        //??????
                        if (!CollectionUtils.isEmpty(contractVos)) {
                            Set<String> contractCodeSet = new HashSet<>();
                            List<ContractVo> list = new LinkedList<>();
                            for (ContractVo contractVo : contractVos) {
                                if (!contractCodeSet.contains(contractVo.getContractCode())) {
                                    contractVo.setTaxedPrice(requirementManageDTO.getTaxPrice());
                                    contractVo.setTaxRate(requirementManageDTO.getTaxRate());
                                    contractVo.setTaxKey(requirementManageDTO.getTaxKey());
                                    contractVo.setUntaxedPrice(requirementManageDTO.getNotaxPrice());
                                    contractVo.setCurrencyCode(requirementManageDTO.getCurrencyCode());
                                    contractVo.setCurrencyName(requirementManageDTO.getCurrencyName());
                                    contractCodeSet.add(contractVo.getContractCode());
                                    list.add(contractVo);
                                }
                            }
                            //?????????????????? ??????????????????
                            List<ContractVo> newContract = getContractVosCopyAndSort(list);
                            requirementManageDTO.setContractVoList(newContract);
                            requirementManageDTO.setContractNum(newContract.get(newContract.size() - 1).getContractCode());
                            requirementManageDTO.setContractNo(newContract.get(newContract.size() - 1).getContractNo());
                        }
                    }
                    requirementManageDTO.setFromPrice("Y");
                    requirementManageDTO.setCeeaPriceSourceId(effect.getPriceLibraryId());
                    requirementManageDTO.setCeeaPriceSourceType(PriceSourceTypeEnum.PRICE_LIBRARY.getValue());
                    result.add(requirementManageDTO);
                }
            }
        }
        return result;
    }

    private List<RequirementManageDTO> processWithEffectivePriceNew(TransferOrderVerifyDTO verifyDTO,
                                                                 RequirementManageDTO requirementLine,
                                                                 List<PriceLibrary> effectPriceList) {
        List<RequirementManageDTO> result = new ArrayList<>();
        Map<String, Map<String, List<ContractVo>>> frameContractMapT = verifyDTO.getFrameContractMapT();
        //?????????????????????
        Function<PriceLibrary, String> getPlace = s -> Objects.isNull(s.getCeeaArrivalPlace()) ? "" : s.getCeeaArrivalPlace();
        //?????????vendorCode??????????????????????????????
        Map<String, Map<String, List<PriceLibrary>>> vendorPlaceMap = effectPriceList.stream().collect(Collectors.groupingBy(PriceLibrary::getVendorCode
                , Collectors.groupingBy(getPlace)
        ));
        for (Map.Entry<String, Map<String, List<PriceLibrary>>> placeMap : vendorPlaceMap.entrySet()) {
            for (Map.Entry<String, List<PriceLibrary>> place : placeMap.getValue().entrySet()) {
                for (PriceLibrary effect : place.getValue()) {
                    RequirementManageDTO requirementManageDTO = assignValueFromEffectPrice(requirementLine, effect);
                    //????????????????????????????????????????????????????????????????????????
                    List<OrgCategory> orgCategoryList = supplierClient.getOrgCategoryByOrgCategory(new OrgCategory().setCompanyId(requirementManageDTO.getVendorId()).setServiceStatus("GREEN"));
                    if(orgCategoryList==null || orgCategoryList.size()==0)
                        continue;
                    //?????????????????????????????????????????????
                    if (Objects.isNull(frameContractMapT)) {
                        throw new BaseException(String.format("????????????[%s]?????????????????????????????????????????????-[%s]?????????????????????,????????????????????????", requirementLine.getRowNum(), requirementLine.getOrgName()));
                    }
                    Map<String, List<ContractVo>> vendorMap = getFrameWorkMap(frameContractMapT, verifyDTO, requirementManageDTO.getOrgCode());
                    if (Objects.isNull(vendorMap)) {
                        throw new BaseException(String.format("????????????[%s]?????????????????????????????????????????????-[%s]?????????????????????,????????????????????????", requirementLine.getRowNum(), requirementLine.getOrgName()));
                    }
                    List<ContractVo> contractVos = vendorMap.get(effect.getVendorCode());
                    if (!CollectionUtils.isEmpty(contractVos)) {
                        //??????
                        if (!CollectionUtils.isEmpty(contractVos)) {
                            Set<String> contractCodeSet = new HashSet<>();
                            List<ContractVo> list = new LinkedList<>();
                            for (ContractVo contractVo : contractVos) {
                                if (!contractCodeSet.contains(contractVo.getContractCode())) {
                                    contractVo.setTaxedPrice(requirementManageDTO.getTaxPrice());
                                    contractVo.setTaxRate(requirementManageDTO.getTaxRate());
                                    contractVo.setTaxKey(requirementManageDTO.getTaxKey());
                                    contractVo.setUntaxedPrice(requirementManageDTO.getNotaxPrice());
                                    contractVo.setCurrencyCode(requirementManageDTO.getCurrencyCode());
                                    contractVo.setCurrencyName(requirementManageDTO.getCurrencyName());
                                    contractCodeSet.add(contractVo.getContractCode());
                                    list.add(contractVo);
                                }
                            }
                            //?????????????????? ??????????????????
                            List<ContractVo> newContract = getContractVosCopyAndSort(list);
                            requirementManageDTO.setContractVoList(newContract);
                            requirementManageDTO.setContractNum(newContract.get(newContract.size() - 1).getContractCode());
                            requirementManageDTO.setContractNo(newContract.get(newContract.size() - 1).getContractNo());
                        }
                    }
                    requirementManageDTO.setFromPrice("Y");
                    requirementManageDTO.setCeeaPriceSourceId(effect.getPriceLibraryId());
                    requirementManageDTO.setCeeaPriceSourceType(PriceSourceTypeEnum.PRICE_LIBRARY.getValue());
                    result.add(requirementManageDTO);
                }
            }
        }
        return result;
    }

    /**
     * ????????????--????????? ??????????????????????????????
     *
     * @param verifyDTO
     * @param requirementLine
     * @return
     */
    private List<RequirementManageDTO> processWithNotEffectivePrice(TransferOrderVerifyDTO verifyDTO,
                                                                    RequirementManageDTO requirementLine) {
        List<RequirementManageDTO> result = new ArrayList<>();
        Map<String, Map<String, Map<String, List<ContractVo>>>> notFrameContractMapT = verifyDTO.getNotFrameContractMapT();
        //??????????????????????????????????????????????????????
        Map<String, List<ContractVo>> materialMap = getContractMap(verifyDTO, requirementLine, notFrameContractMapT);
        if (Objects.nonNull(materialMap)) {
            List<ContractVo> contractVos = materialMap.get(requirementLine.getMaterialCode());
            if (CollectionUtils.isEmpty(contractVos)) {
                throw new BaseException(String.format("????????????[%s]?????????????????????????????????", requirementLine.getRowNum()));
            }
            //???????????????????????????????????????????????????????????????????????????????????????
            List<ContractVo> notPriceContractVO = contractVos.stream().filter(e -> Objects.equals(e.getIsFrameworkAgreement(), "N")
                    && Objects.nonNull(e.getTaxedPrice())
                    && StringUtils.isNotBlank(e.getTaxKey())
                    && Objects.nonNull(e.getTaxRate())
                    && Objects.equals(e.getMaterialCode(), requirementLine.getMaterialCode())
                    && StringUtils.isBlank(requirementLine.getVendorCode()) || Objects.equals(e.getHeadVendorCode(), requirementLine.getVendorCode())
                    && StringUtils.isNotBlank(e.getCurrencyCode())
            ).peek(e -> {   //peek?????????????????????
                BigDecimal taxRate = e.getTaxRate().divide(new BigDecimal(100), 8, BigDecimal.ROUND_HALF_UP);
                BigDecimal untaxedPrice = e.getTaxedPrice().divide((new BigDecimal(1).add(taxRate)), 2, BigDecimal.ROUND_HALF_UP);
                e.setUntaxedPrice(untaxedPrice);
            }).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(notPriceContractVO)) {
                throw new BaseException(String.format("????????????[%s]?????????????????????????????????", requirementLine.getRowNum()));
            }
            //?????????????????????
            Map<String, List<ContractVo>> contractMap = notPriceContractVO.stream().collect(Collectors.groupingBy(ContractVo::getHeadVendorCode));
            for (Map.Entry<String, List<ContractVo>> vendorMap : contractMap.entrySet()) {
                List<ContractVo> partnerContract = vendorMap.getValue();
                RequirementManageDTO requirementManageDTO = BeanCopyUtil.copyProperties(requirementLine, RequirementManageDTO::new);
                List<ContractVo> newContract = getContractVosCopyAndSort(partnerContract);
                ContractVo contractVo = newContract.get(partnerContract.size() - 1);
                requirementManageDTO.setCurrencyId(contractVo.getCurrencyId());
                requirementManageDTO.setCurrencyCode(contractVo.getCurrencyCode());
                requirementManageDTO.setCurrencyName(contractVo.getCurrencyName());
                requirementManageDTO.setNotaxPrice(contractVo.getUntaxedPrice());
                requirementManageDTO.setTaxPrice(contractVo.getTaxedPrice());
                requirementManageDTO.setTaxKey(contractVo.getTaxKey());
                requirementManageDTO.setTaxRate(contractVo.getTaxRate());
                requirementManageDTO.setContractNum(contractVo.getContractCode());
                requirementManageDTO.setContractNo(contractVo.getContractNo());
                requirementManageDTO.setIsSeaFoodFormula(contractVo.getIsSeaFoodFormula());
                requirementManageDTO.setEssentialFactorValues(contractVo.getEssentialFactorValues());
                requirementManageDTO.setPriceJson(contractVo.getPriceJson());
                requirementManageDTO.setCeeaFormulaValue(contractVo.getFormulaValue());
                requirementManageDTO.setCeeaFormulaId(contractVo.getFormulaId());
                requirementManageDTO.setContractVoList(partnerContract);
                requirementManageDTO.setFromPrice("N");
                requirementManageDTO.setCeeaPriceSourceType(PriceSourceTypeEnum.CONTRACT.getValue());
                requirementManageDTO.setCeeaPriceSourceId(contractVo.getContractMaterialId());
                requirementManageDTO.setVendorCode(contractVo.getHeadVendorCode());
                requirementManageDTO.setVendorId(contractVo.getHeadVendorId());
                requirementManageDTO.setVendorName(contractVo.getHeadVendorName());
                List<OrderPaymentProvision> paymentProvisions = contractVo.getPayPlanList().stream().map(e -> {
                    OrderPaymentProvision orderPaymentProvision = new OrderPaymentProvision()
                            .setPaymentPeriod(StringUtil.StringValue(e.getDateNum()))  //????????????
                            .setPaymentWay(e.getPayMethod())  //????????????
                            .setPaymentTerm(e.getPayExplain());  //????????????
                    return orderPaymentProvision;
                }).collect(Collectors.toList());
                requirementManageDTO.setOrderPaymentProvisionList(paymentProvisions);
                requirementManageDTO.setContractVoList(newContract);
                result.add(requirementManageDTO);
            }
        }
        return result;
    }


    /**
     * ??????????????????????????????
     * ?????????????????????????????????
     * 1?????????????????????????????????????????????????????????????????????????????????ID+????????????+????????????+?????????+?????????+????????????????????????????????????????????????????????????????????????????????????
     * 2???????????????????????????????????????????????????????????????
     * ????????????????????????????????????????????????????????????ID+????????????+????????????+????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param verifyDTO
     * @return
     */
    private List<RequirementManageDTO> verifyDirectData(TransferOrderVerifyDTO verifyDTO) {
        List<RequirementManageDTO> result = new ArrayList<>();
        Map<String, Map<String, Map<String, List<PriceLibrary>>>> invItemMap = verifyDTO.getPriceLibraryMapT();
        Map<String, Map<String, Map<String, List<ContractVo>>>> contractInvAndMaterialMap = verifyDTO.getNotFrameContractMapT();

        for (RequirementManageDTO item : verifyDTO.getDirectRequirement()) {
            Map<String, Map<String, List<PriceLibrary>>> itemMap = invItemMap.get(item.getOrgCode());
            if (Objects.isNull(itemMap)) {
                throw new BaseException(String.format("?????????????????????[%s]????????????????????????????????????[%s]???????????????", item.getRowNum(), item.getOrgName()));
            }
            List<PriceLibrary> priceLibraries = itemMap.getOrDefault(item.getOrganizationCode(), Collections.emptyMap()).get(item.getMaterialCode());
            if (Objects.isNull(priceLibraries)) {
                throw new BaseException(String.format("?????????????????????[%s]????????????????????????????????????[%s]?????????[%s]????????????", item.getRowNum(), item.getOrganizationName(), item.getMaterialName()));
            }
            //???????????????????????????
            List<PriceLibrary> effectPriceList = priceLibraries.stream().filter(e -> {
                boolean vendorEqual = true;
                if (StringUtils.isNotBlank(item.getVendorCode())) {
                    vendorEqual = Objects.equals(item.getVendorCode(), e.getVendorCode());
                }
                boolean nameEquals = true;
                if (StringUtils.isNotBlank(item.getMaterialName()) && verifyDTO.getCodeSet().contains(e.getCategoryCode())) {
                    nameEquals = Objects.equals(e.getItemDesc(), item.getMaterialName());
                }
                return vendorEqual && Objects.equals(e.getCeeaIfUse(), YesOrNo.YES.getValue()) && nameEquals;
            }).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(effectPriceList)) {
                throw new BaseException(String.format("?????????????????????[%s]???????????????????????????????????????[%s]?????????[%s]??????????????????", item.getRowNum(), item.getOrganizationName(), item.getMaterialName()));
            }
            //????????????
            Function<PriceLibrary, String> getPlace = s -> Objects.isNull(s.getCeeaArrivalPlace()) ? "" : s.getCeeaArrivalPlace();
            //?????????vendorCode??????????????????????????????
            Map<String, Map<String, List<PriceLibrary>>> vendorPlaceMap = effectPriceList.stream()
                    .collect(Collectors.groupingBy(PriceLibrary::getVendorCode, Collectors.groupingBy(getPlace)));

            for (Map.Entry<String, Map<String, List<PriceLibrary>>> placeMap : vendorPlaceMap.entrySet()) { //?????????????????????
                for (Map.Entry<String, List<PriceLibrary>> vendorMap : placeMap.getValue().entrySet()) {    //??????????????????
                    for (PriceLibrary effect : vendorMap.getValue()) {
                        if (StringUtils.isBlank(effect.getContractCode())) {
                            throw new BaseException(String.format("?????????????????????[%s]???????????????[%s]?????????[%s]????????????????????????????????????", item.getRowNum(), item.getOrganizationName(), item.getMaterialName()));
                        }
                        //?????????????????????????????????????????????,???????????????
                        ContractVo contractVo = contractInvAndMaterialMap.get(item.getOrgCode()).get(item.getOrganizationCode()).get(item.getMaterialCode())
                                .stream().filter(e -> {
                                    if (e.getContractCode().contains(CONTRACT_CUT_SIGN)) {
                                        int i = e.getContractCode().lastIndexOf(CONTRACT_CUT_SIGN);
                                        String temp;
                                        if ((temp = e.getContractCode().substring(0, i)).contains(CONTRACT_CUT_SIGN)) {
                                            int i1 = temp.lastIndexOf(CONTRACT_CUT_SIGN);
                                            e.setContractCode(temp.substring(i1 + 4));
                                        } else {
                                            e.setContractCode(e.getContractCode().substring(i + 4));
                                        }
                                    }
                                    return Objects.equals(e.getContractCode(), effect.getContractCode());
                                }).findAny().orElseThrow(() -> new BaseException(String.format("?????????????????????[%s]???????????????[%s]?????????[%s]???????????????????????????[%s]-??????????????????????????????"
                                        , item.getRowNum(), item.getOrganizationName(), item.getMaterialName(), effect.getContractCode())));

                        RequirementManageDTO requirementManageDTO = assignValueFromEffectPrice(item, effect);
                        contractVo.setTaxedPrice(requirementManageDTO.getTaxPrice());
                        contractVo.setTaxRate(requirementManageDTO.getTaxRate());
                        contractVo.setTaxKey(requirementManageDTO.getTaxKey());
                        contractVo.setUntaxedPrice(requirementManageDTO.getNotaxPrice());
                        contractVo.setCurrencyCode(requirementManageDTO.getCurrencyCode());
                        contractVo.setCurrencyName(requirementManageDTO.getCurrencyName());
                        ContractVo resultContract = BeanCopyUtil.copyProperties(contractVo, ContractVo::new);
                        requirementManageDTO.setContractVoList(Collections.singletonList(resultContract));
                        requirementManageDTO.setFromPrice("Y");
                        requirementManageDTO.setCeeaPriceSourceId(effect.getPriceLibraryId());
                        requirementManageDTO.setCeeaPriceSourceType(PriceSourceTypeEnum.PRICE_LIBRARY.getValue());
                        requirementManageDTO.setContractNum(resultContract.getContractCode());
                        requirementManageDTO.setContractNo(resultContract.getContractNo());
                        result.add(requirementManageDTO);
                    }
                }
            }
        }

        return result;
    }

    /**
     * ???????????????--?????????????????????????????????
     *
     * @param requirementManageDTOS
     * @return
     */
    private List<RequirementManageDTO> truncatedDirectory(List<RequirementManageDTO> requirementManageDTOS) {
        List<RequirementManageDTO> directList = new LinkedList<>();
        for (int i = requirementManageDTOS.size() - 1; i >= 0; i--) {
            RequirementManageDTO requirementManageDTO = requirementManageDTOS.get(i);
            if (Objects.equals(requirementManageDTO.getCeeaIfDirectory(), YesOrNo.YES.getValue())) {
                RequirementManageDTO remove = requirementManageDTOS.remove(i);
                directList.add(remove);
                continue;
            }
        }
        return directList;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param item
     * @param effect
     * @return
     */
    private RequirementManageDTO assignValueFromEffectPrice(RequirementManageDTO item, PriceLibrary effect) {
        if (Objects.isNull(effect.getCeeaQuotaProportion())) {
            effect.setCeeaQuotaProportion(BigDecimal.ZERO);
        }
        RequirementManageDTO requirementManageDTO = BeanCopyUtil.copyProperties(item, RequirementManageDTO::new);
        BigDecimal quotaProportion = effect.getCeeaQuotaProportion().divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);
        effect.setCeeaQuotaProportion(quotaProportion);
        requirementManageDTO.setThisOrderQuantity(quotaProportion.multiply(item.getRequirementQuantity()))
                .setQuotaProportion(effect.getCeeaQuotaProportion())
                .setVendorId(effect.getVendorId())
                .setVendorName(effect.getVendorName())
                .setVendorCode(effect.getVendorCode())
                .setCurrencyId(effect.getCurrencyId())
                .setCurrencyCode(effect.getCurrencyCode())
                .setCurrencyName(effect.getCurrencyName())
                .setOrganizationCode(effect.getCeeaOrganizationCode())
                .setOrganizationName(effect.getCeeaOrganizationName())
                .setOrganizationId(effect.getCeeaOrganizationId())
                .setOrgCode(effect.getCeeaOrgCode())
                .setOrgId(effect.getCeeaOrgId())
                .setOrgName(effect.getCeeaOrgName())
                .setTaxRate(new BigDecimal(effect.getTaxRate()))
                .setTaxKey(effect.getTaxKey())
                .setCeeaDeliveryPlace(effect.getCeeaArrivalPlace())
                .setTaxPrice(effect.getTaxPrice())
                .setNotaxPrice(effect.getNotaxPrice())
                .setIsSeaFoodFormula(effect.getIsSeaFoodFormula())
                .setPriceJson(effect.getPriceJson())
                .setCeeaFormulaValue(effect.getFormulaValue())
                .setCeeaFormulaId(effect.getFormulaId())
                .setEssentialFactorValues(effect.getEssentialFactorValues())
                .setMinOrderQuantity(effect.getMinOrderQuantity());
        requirementManageDTO.setTaxRate(new BigDecimal(effect.getTaxRate()));
        List<OrderPaymentProvision> paymentProvisions = effect.getPaymentTerms().stream().map(e -> {
            OrderPaymentProvision orderPaymentProvision = new OrderPaymentProvision()
                    .setPaymentPeriod(StringUtil.StringValue(e.getPaymentDay()))  //????????????
                    .setPaymentWay(e.getPaymentWay())  //????????????
                    .setPaymentTerm(e.getPaymentTerm());  //????????????
            return orderPaymentProvision;
        }).collect(Collectors.toList());
        requirementManageDTO.setOrderPaymentProvisionList(paymentProvisions);
        return requirementManageDTO;
    }

    /**
     * ?????????????????? ??????????????????
     *
     * @param partnerContract
     * @return
     */
    private List<ContractVo> getContractVosCopyAndSort(List<ContractVo> partnerContract) {
        Map<String, List<ContractVo>> collect = partnerContract.stream().collect(Collectors.groupingBy(ContractVo::getContractCode));
        //????????????????????????
        for (Map.Entry<String, List<ContractVo>> entry : collect.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (int i = 1; i < entry.getValue().size(); i++) {
                    ContractVo contractVo = entry.getValue().get(i);
                    contractVo.setContractCode(contractVo.getContractCode() + i);
                }
            }
        }
        //?????????
        List<ContractVo> newContract = partnerContract.stream().map(e -> {
            ContractVo temp = BeanCopyUtil.copyProperties(e, ContractVo::new);
            List<PayPlan> list = e.getPayPlanList().stream().map(p -> BeanCopyUtil.copyProperties(p, PayPlan::new)).collect(Collectors.toList());
            temp.setPayPlanList(list);
            return temp;
        }).collect(Collectors.toList());
        //??????
        newContract.sort(Comparator.comparing(ContractVo::getCreationDate));
        return newContract;
    }


    /**********************************************step 2.???????????????-???????????????-??????????????????************************************/
    @Override
    public void submitPurchaseOrderNew(List<RequirementManageDTO> requirementManageDTOS) {

    }

    private Map<String, List<PriceLibrary>> getPriceMap(TransferOrderVerifyDTO verifyDTO, Map<String, Map<String, Map<String, List<PriceLibrary>>>> orgInvItemMap, Map<String, Set<String>> buCodeMap, RequirementManageDTO requirementLine) {
        //???????????????????????????
        boolean buPrice = false;
        for (DictItem dictItem : verifyDTO.getRequireEntity()) {
            if (Objects.equals(dictItem.getDictItemName(), requirementLine.getOrgCode())) {
                buPrice = true;
                break;
            }
        }
        //???????????????????????????
        if (buPrice) {
            Map<String, List<PriceLibrary>> temp = new HashMap<>();
            buCodeMap.forEach((k, v) -> {
                if (v.contains(requirementLine.getOrgCode())) {
                    for (String orgCode : v) {
                        //??????????????????-??????
                        Map<String, Map<String, List<PriceLibrary>>> invMaterialMap = orgInvItemMap.get(orgCode);
                        if (!MapUtils.isEmpty(invMaterialMap)) {
                            invMaterialMap.forEach((inv, materialCodeMap) -> materialCodeMap.forEach((mc, l) -> {
                                temp.compute(mc, (s, priceLibraries) -> {
                                    if (Objects.isNull(priceLibraries)) {
                                        return copyPriceLibararyList(l);
                                    } else {
                                        priceLibraries.addAll(copyPriceLibararyList(l));
                                    }
                                    return priceLibraries;
                                });
                            }));
                        }

                    }
                }
            });
            return temp;
        }
        return orgInvItemMap.getOrDefault(requirementLine.getOrgCode(), Collections.emptyMap()).getOrDefault(requirementLine.getOrganizationCode(), Collections.emptyMap());

    }

    private List<PriceLibrary> copyPriceLibararyList(List<PriceLibrary> l) {
        return l.stream().map(e -> {
            PriceLibrary t = BeanCopyUtil.copyProperties(e, PriceLibrary::new);
            List<PriceLibraryPaymentTerm> list = e.getPaymentTerms().stream().map(p -> BeanCopyUtil.copyProperties(p, PriceLibraryPaymentTerm::new)).collect(Collectors.toList());
            t.setPaymentTerms(list);
            return t;
        }).collect(Collectors.toList());
    }

    private Map<String, List<ContractVo>> getContractMap(TransferOrderVerifyDTO verifyDTO, RequirementManageDTO requirementLine, Map<String, Map<String, Map<String, List<ContractVo>>>> notFrameContractMapT) {
        boolean buPrice = false;
        for (DictItem dictItem : verifyDTO.getRequireEntity()) {
            if (Objects.equals(dictItem.getDictItemName(), requirementLine.getOrgCode())) {
                buPrice = true;
                break;
            }
        }
        Map<String, Set<String>> buCodeMap = verifyDTO.getBuCodeMap();
        //???????????????????????????
        if (buPrice) {
            Map<String, List<ContractVo>> temp = new HashMap<>();
            buCodeMap.forEach((k, v) -> {
                if (v.contains(requirementLine.getOrgCode())) {
                    for (String orgCode : v) {
                        //??????????????????-??????
                        Map<String, Map<String, List<ContractVo>>> invMaterialMap = notFrameContractMapT.get(orgCode);
                        if (!MapUtils.isEmpty(invMaterialMap)) {
                            invMaterialMap.forEach((inv, materialCodeMap) -> materialCodeMap.forEach((mc, l) -> {
                                temp.compute(mc, (s, contractVO) -> {
                                    if (Objects.isNull(contractVO)) {
                                        return getContractVosCopyAndSort(l);
                                    } else {
                                        contractVO.addAll(getContractVosCopyAndSort(l));
                                        return contractVO;
                                    }
                                });
                            }));
                        }
                    }
                }
            });
            return temp;
        }
        return notFrameContractMapT.getOrDefault(requirementLine.getOrgCode(), Collections.emptyMap()).getOrDefault(requirementLine.getOrganizationCode(), Collections.emptyMap());

    }


    private Map<String, List<ContractVo>> getFrameWorkMap(Map<String, Map<String, List<ContractVo>>> orgSupplierMap, TransferOrderVerifyDTO verifyDTO, String orgCode) {
        boolean buPrice = false;
        for (DictItem dictItem : verifyDTO.getRequireEntity()) {
            if (Objects.equals(dictItem.getDictItemName(), orgCode)) {
                buPrice = true;
                break;
            }
        }
        if (buPrice) {
            Map<String, List<ContractVo>> temp = new HashMap<>();
            verifyDTO.getBuCodeMap().forEach((k, v) -> {
                if (v.contains(orgCode)) {
                    for (String o : v) {
                        //??????????????????-??????
                        Map<String, List<ContractVo>> materialCodeMap = orgSupplierMap.get(o);
                        if (!MapUtils.isEmpty(materialCodeMap)) {
                            materialCodeMap.forEach((mc, l) -> {
                                temp.compute(mc, (s, contractVO) -> {
                                    if (Objects.isNull(contractVO)) {
                                        return l;
                                    } else {
                                        contractVO.addAll(l);
                                        return contractVO;
                                    }
                                });
                            });
                        }
                    }
                }
            });
            return temp;
        }
        return orgSupplierMap.get(orgCode);
    }
}
