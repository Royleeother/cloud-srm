package com.midea.cloud.srm.bid.purchaser.projectmanagement.bidingresult.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.enums.bid.projectmanagement.evaluation.SelectionStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.redis.RedisUtil;
import com.midea.cloud.component.entity.EntityManager;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidingresult.service.ISourcingResultReportService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.mapper.BidRequirementLineMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.mapper.BidingMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.evaluation.service.IEvaluationService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.inq.InqClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuDetailVO;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuGroupDetailVO;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.enums.QuotaDistributeType;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.param.EvaluationQueryParam;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.vo.EvaluationResult;
import com.midea.cloud.srm.model.inq.price.entity.InquiryHeaderReport;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibrary;
import com.midea.cloud.srm.model.inq.price.enums.TransferStatus;
import com.midea.cloud.srm.model.inq.sourcingresult.vo.*;
import com.midea.cloud.srm.model.supplier.info.entity.OrgCategory;
import com.midea.cloud.srm.model.supplier.vendororgcategory.vo.FindVendorOrgCateRelParameter;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implement of {@link ISourcingResultReportService}.
 *
 * @author zixuan.yan@meicloud.com
 */
@Service
public class SourcingResultReportServiceImpl implements ISourcingResultReportService {

    private final EntityManager<Biding> biddingDao
            = EntityManager.use(BidingMapper.class);
    private final EntityManager<BidRequirementLine> demandLineDao
            = EntityManager.use(BidRequirementLineMapper.class);

    @Resource
    private IEvaluationService evaluationService;
    @Resource
    private BaseClient baseClient;
    @Resource
    private SupplierClient vendorOrgCateRelClient;
    @Resource
    private InqClient inqClient;
    @Resource
    private RedisUtil redisUtil;

    private final static String key = "lock-generateReport-bid";

    @Override

    public SourcingResultReport generate(String biddingNum, Long inquiryId) {

        //???????????????
        if (biddingNum.startsWith("BID") || biddingNum.startsWith("RFQ") || biddingNum.startsWith("RFA")) {
            return null;
        }
        // ?????? ?????????
        Biding bidding = Optional.ofNullable(biddingNum)
                .map(num -> biddingDao.findOne(Wrappers.lambdaQuery(Biding.class).eq(Biding::getBidingNum, num)))
                .orElseThrow(() -> new BaseException("???????????????????????? | biddingNum: [" + biddingNum + "]"));
        long judge=System.currentTimeMillis();
        InquiryHeaderReport result = inqClient.judegeIsFinishReport(bidding.getBidingId());
        //????????????????????????????????????
        if (Objects.nonNull(result)) {
            if (Objects.equals(result.getChangeStatus(), TransferStatus.FINISH.getCode())) {
                SourcingResultReport sourcingResultReport = JSON.parseObject(result.getReportJson(), SourcingResultReport.class);
                return sourcingResultReport;
            }
            if (Objects.equals(result.getChangeStatus(), TransferStatus.FAIL.getCode())) {
                throw new BaseException(String.format("????????????????????????????????????????????????,??????id:%s", result.getReportId()));
            }
        }
        //???????????????????????????
        Boolean isLock = redisUtil.tryLock(key + biddingNum, 1, TimeUnit.HOURS);
        if (!isLock) {
            throw new BaseException("???????????????????????????????????????????????????!");
        }
        try {
            String quotaDistributeType = bidding.getQuotaDistributeType();
            boolean isEmpty = Objects.equals(QuotaDistributeType.NULL_ALLOCATION.getCode(), quotaDistributeType);
            // ?????? ???????????????
            List<BidRequirementLine> demandLines = demandLineDao
                    .findAll(Wrappers.lambdaQuery(BidRequirementLine.class)
                            .eq(BidRequirementLine::getBidingId, bidding.getBidingId())
                    );

            // ?????? ????????????????????????
            List<EvaluationResult> evaluationResults = evaluationService
                    .findEvaluationResults(EvaluationQueryParam.builder()
                            .filterQuit(false)
                            .bidingId(bidding.getBidingId())
                            .round(bidding.getCurrentRound())
                            .build()
                    );
            // ?????????????????????????????????????????????
            evaluationResults.stream()
                    .filter(evaluationResult -> SelectionStatusEnum.WIN.getValue().equals(evaluationResult.getSelectionStatus()))
                    .forEach(evaluationResult -> {
                        if (evaluationResult.getQuotaQuantity() == null && isEmpty) {
                            evaluationResult.setQuotaQuantity(BigDecimal.ZERO);
                        } else if (evaluationResult.getQuotaQuantity() == null) {
                            throw new BaseException(String.format("???????????????????????????%s,??????????????????", QuotaDistributeType.valueOf(quotaDistributeType).getName()));
                        }
                    });

            // ????????? OU?????????????????????
            List<Long> ouIds = evaluationResults.stream()
                    .filter(evaluationResult -> evaluationResult.getOuId() != null)
                    .map(EvaluationResult::getOuId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, List<BaseOuDetailVO>> ouGroups = ouIds.isEmpty()
                    ? Collections.emptyMap()
                    : baseClient.queryOuInfoDetailByIds(ouIds).stream()
                    .collect(Collectors.toMap(BaseOuGroupDetailVO::getOuGroupId, BaseOuGroupDetailVO::getDetails));

            // ????????? ???????????????????????????
            List<FindVendorOrgCateRelParameter.OrgCateCompose> orgCateComposes = evaluationResults.stream()
                    .flatMap(evaluationResult -> {
                        if (evaluationResult.getOuId() != null)
                            return ouGroups.getOrDefault(evaluationResult.getOuId(), Collections.emptyList()).stream()
                                    .map(baseOuDetail -> new FindVendorOrgCateRelParameter.OrgCateCompose(
                                            baseOuDetail.getOuId(),
                                            evaluationResult.getCategoryId()
                                    ));
                        else
                            return Stream.of(new FindVendorOrgCateRelParameter.OrgCateCompose(
                                    evaluationResult.getOrgId(),
                                    evaluationResult.getCategoryId()
                            ));
                    })
                    .collect(Collectors.toList());
            List<OrgCategory> vendorOrgCateRels = vendorOrgCateRelClient
                    .findVendorOrgCateRels(
                            FindVendorOrgCateRelParameter.builder()
                                    .orgCateComposes(orgCateComposes)
                                    .build()
                    )
                    .stream()
                    .flatMap(orgCateRels -> orgCateRels.getOrgCategories().stream())
                    .collect(Collectors.toList());


            // generate.
            return this.generate(GenerateParameter.builder()
                    .bidding(bidding)
                    .demandLines(demandLines)
                    .evaluationResults(evaluationResults)
                    .relatedOuGroups(ouGroups)
                    .relatedVendorOrgCateRels(vendorOrgCateRels)
                    .build(), inquiryId);
        } catch (Throwable e) {
            inqClient.callWhenFail(GenerateSourcingResultReportParameter.builder()
                    .sourcingFormId(bidding.getBidingId())
                    .sourcingFormNo(bidding.getBidingNum())
                    .inquiryId(inquiryId)
                    .failMsg(e.getMessage())
                    .build());
            throw new BaseException(e.getMessage());
        } finally {
            redisUtil.unLock(key + biddingNum);
        }


    }

    /**
     * ?????? ??????????????????
     *
     * @param parameter ????????????
     * @return ??????????????????
     */
    protected SourcingResultReport generate(GenerateParameter parameter, Long inquiryId) {
        List<PriceLibrary> paramList = new LinkedList<>();
        for (BidRequirementLine e : parameter.getDemandLines()) {
            if (Objects.nonNull(e.getOuId())) {
                for (Map.Entry<Long, List<BaseOuDetailVO>> entry : parameter.getRelatedOuGroups().entrySet()) {
                    List<BaseOuDetailVO> value = entry.getValue();
                    for (BaseOuDetailVO baseOuDetailVO : value) {
                        PriceLibrary temp = new PriceLibrary();
                        temp.setCeeaOrgId(baseOuDetailVO.getOuId());
                        temp.setItemId(e.getTargetId());
                        paramList.add(temp);
                    }
                }
            } else {
                PriceLibrary temp = new PriceLibrary();
                temp.setCeeaOrgId(e.getOuId());
                temp.setItemId(e.getTargetId());
                paramList.add(temp);
            }
        }
        Map<Long, Map<Long, List<PriceLibrary>>> orgAndItemMap = inqClient.getLatestForAnonBatch(paramList).stream()
                .collect(Collectors.groupingBy(PriceLibrary::getCeeaOrgId, Collectors.groupingBy(PriceLibrary::getItemId)));
        // ?????????
        List<SourcingDemandLine> demandLines = parameter.getDemandLines().stream()
                .map(demandLine -> SourcingDemandLine.builder()
                        .id(demandLine.getRequirementLineId())
                        .biddingId(demandLine.getBidingId())
                        .orgId(demandLine.getOrgId())
                        .orgCode(demandLine.getOrgCode())
                        .orgName(demandLine.getOrgName())
                        .ouGroupId(demandLine.getOuId())
                        .ouGroupCode(demandLine.getOuNumber())
                        .ouGroupName(demandLine.getOuName())
                        .itemId(demandLine.getTargetId())
                        .itemCode(demandLine.getTargetNum())
                        .itemName(demandLine.getTargetDesc())
                        .categoryId(demandLine.getCategoryId())
                        .categoryCode(demandLine.getCategoryCode())
                        .categoryName(demandLine.getCategoryName())
                        .groupKey(demandLine.getItemGroup())
                        .groupQuantityRatio(
                                Optional.ofNullable(demandLine.getMaterialMatching())
                                        .filter(StringUtils::hasText)
                                        .map(x -> BigDecimal.valueOf(Double.parseDouble(x.contains("%") ? x.replace("%", "") : x)))
                                        .orElse(BigDecimal.ZERO)
                        )
                        .demandQuantity(BigDecimal.valueOf(demandLine.getQuantity()))
                        .historyTaxPrice(this.findHistoryTaxPrice(orgAndItemMap, demandLine, parameter))
                        .build())
                .collect(Collectors.toList());

        // ???????????????????????????
        List<VendorBiddingLine> lastRoundVendorBiddingLines = parameter.getEvaluationResults().stream()
                .map(evaluationResult -> VendorBiddingLine.builder()
                        .id(evaluationResult.getOrderLineId())
                        .demandLineId(evaluationResult.getRequirementLineId())
                        .bidVendorId(evaluationResult.getBidVendorId())
                        .vendorId(evaluationResult.getVendorId())
                        .biddingId(evaluationResult.getBidingId())
                        .isWinning(SelectionStatusEnum.WIN.getValue().equals(evaluationResult.getSelectionStatus()))
                        .vendorCode(evaluationResult.getVendorCode())
                        .vendorName(evaluationResult.getVendorName())
                        .priceScore(evaluationResult.getPriceScore())
                        .technologyScore(evaluationResult.getTechScore())
                        .performanceScore(evaluationResult.getPerfScore())
                        .compositeScore(evaluationResult.getCompositeScore())
                        .rank(evaluationResult.getRank())
                        .quotaQuantity(evaluationResult.getQuotaQuantity())
                        .quotaRatio(evaluationResult.getQuotaRatio())
                        .leadTime(evaluationResult.getLeadTime())
                        .taxKey(evaluationResult.getTaxKey())
                        .taxRate(evaluationResult.getTaxRate())
                        .warrantyPeriod(evaluationResult.getWarrantyPeriod())
                        .deliverDate(evaluationResult.getDeliverDate())
                        .quotePrice(evaluationResult.getPrice())
                        .discountPrice(evaluationResult.getDiscountPrice())
                        .paymentTerms(evaluationResult.getPaymentTerms().stream()
                                .map(paymentTerm -> VendorBiddingLinePaymentTerm.builder()
                                        .id(paymentTerm.getPaymentTermId())
                                        .biddingLineId(paymentTerm.getOrderLineId())
                                        .bidVendorId(evaluationResult.getBidVendorId())
                                        .vendorId(evaluationResult.getVendorId())
                                        .biddingId(evaluationResult.getBidingId())
                                        .paymentTerm(paymentTerm.getPaymentTerm())
                                        .paymentWay(paymentTerm.getPaymentWay())
                                        .paymentDay(paymentTerm.getPaymentDay())
                                        .paymentDayCode(paymentTerm.getPaymentDayCode())
                                        .build())
                                .collect(Collectors.toList())
                        )
                        .vendorStatus(this.findVendorOrgCateStatus(evaluationResult, parameter))
                        .build())
                .collect(Collectors.toList());


        // ?????? ????????????
        return inqClient.generateForAnon(GenerateSourcingResultReportParameter.builder()
                .sourcingFormId(parameter.getBidding().getBidingId())
                .sourcingFormNo(parameter.getBidding().getBidingNum())
                .demandLines(demandLines)
                .inquiryId(inquiryId)
                .lastRoundVendorBiddingLines(lastRoundVendorBiddingLines)
                .build());
    }

    /**
     * ?????? ???????????????????????????
     *
     * @param evaluationResult ????????????
     * @param parameter        ??????????????????
     * @return ???????????????????????????
     */
    protected String findVendorOrgCateStatus(EvaluationResult evaluationResult, GenerateParameter parameter) {
        return Optional.ofNullable(evaluationResult.getOuId())
                .map(ouGroupId -> parameter.getRelatedOuGroups()
                        .getOrDefault(ouGroupId, Collections.emptyList())
                        .stream()
                        .map(BaseOuDetailVO::getOuId)
                        .distinct()
                        .collect(Collectors.toList())
                )
                .orElseGet(() -> Collections.singletonList(evaluationResult.getOrgId()))
                .stream()
                .flatMap(orgId -> parameter.getRelatedVendorOrgCateRels().stream()
                        .filter(orgCateRel -> orgCateRel.getCompanyId().equals(evaluationResult.getVendorId())
                                && orgCateRel.getCategoryId().equals(evaluationResult.getCategoryId())
                                && orgCateRel.getOrgId().equals(orgId)))
                .findFirst()
                .map(OrgCategory::getServiceStatus)
                .orElse("");
    }

    /**
     * ?????? ????????????????????????
     *
     * @param orgAndItemMap
     * @param demandLine    ?????????
     * @param parameter     ??????????????????
     * @return ????????????????????????
     */
    protected BigDecimal findHistoryTaxPrice(Map<Long, Map<Long, List<PriceLibrary>>> orgAndItemMap, BidRequirementLine demandLine, GenerateParameter parameter) {
        BigDecimal temp = BigDecimal.valueOf(Integer.MAX_VALUE);
        Set<Long> orgIds = new HashSet<>();
        if (Objects.nonNull(demandLine.getOuId())) {
            for (Map.Entry<Long, List<BaseOuDetailVO>> entry : parameter.getRelatedOuGroups().entrySet()) {
                List<BaseOuDetailVO> value = entry.getValue();
                for (BaseOuDetailVO baseOuDetailVO : value) {
                    orgIds.add(baseOuDetailVO.getOuId());
                }
            }
        } else {
            orgIds.add(demandLine.getOrgId());
        }
        BigDecimal min = temp;
        for (Long orgId : orgIds) {
            Map<Long, List<PriceLibrary>> itemMap = orgAndItemMap.get(orgId);
            if (Objects.nonNull(itemMap)) {
                List<PriceLibrary> priceLibraries = itemMap.get(demandLine.getTargetId());
                if (!CollectionUtils.isEmpty(priceLibraries)) {
                    for (PriceLibrary priceLibrary : priceLibraries) {
                        if (Objects.nonNull(priceLibrary.getTaxPrice())) {
                            min = min.min(priceLibrary.getTaxPrice());
                        }
                    }
                }
            }
        }
        return min.compareTo(temp) < 0 ? min : null;
    }


    @Builder
    @Data
    static class GenerateParameter implements Serializable {
        private Biding bidding;
        private List<BidRequirementLine> demandLines;
        private List<EvaluationResult> evaluationResults;
        private Map<Long, List<BaseOuDetailVO>> relatedOuGroups;
        private List<OrgCategory> relatedVendorOrgCateRels;
    }
}
