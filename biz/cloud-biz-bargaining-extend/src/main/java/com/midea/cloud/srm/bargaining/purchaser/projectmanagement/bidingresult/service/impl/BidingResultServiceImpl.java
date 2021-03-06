package com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidingresult.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.bargaining.projectmanagement.evaluation.SelectionStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidRequirementLineService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidVendorService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidingService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidingresult.mapper.BidResultMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidingresult.service.IBidingResultService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.mapper.OrderLineMapper;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.evaluation.vo.EvaluationResult;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidVendor;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidingresult.entity.BidResult;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techproposal.entity.OrderLine;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techproposal.vo.AbandonmentBidSupVO;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.ContactInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.midea.cloud.common.utils.BeanCopyUtil.getNullPropertyNames;

/**
 * <pre>
 * ???????????? ???????????????
 * </pre>
 *
 * @author fengdc3@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:tanjl11@meicloud.com
 *  ????????????: 2020???4???9??? ??????16:44:37
 *  ????????????:
 *          </pre>
 */
@Service
public class BidingResultServiceImpl extends ServiceImpl<BidResultMapper, BidResult> implements IBidingResultService {

    @Autowired
    private IBidRequirementLineService iBidRequirementLineService;
    @Autowired
    private IBidingService iBidingService;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private SupplierClient supplierClient;
    @Autowired
    private IBidVendorService iBidVendorService;
    @Autowired
    private IBidingResultService iBidingResultService;
    @Autowired
    private OrderLineMapper orderLineMapper;


    /**
     * ??????????????????
     *
     * @param bidingId
     * @return
     */
    @Override
    @Transactional
    public PageInfo<BidResult> listPageBidingResult(Long bidingId) {
        Assert.notNull(bidingId, "??????id????????????");

        Biding biding = iBidingService.getById(bidingId);

        //??????????????????????????????????????????
        List<BidResult> list = iBidingResultService.list(new QueryWrapper<>(new BidResult().setBidingId(bidingId)));
        if (CollectionUtils.isNotEmpty(list)) {
            return new PageInfo<BidResult>(list);
        }

        //????????????????????????????????????????????????
        if (!"Y".equals(biding.getFinalRound()) || !"Y".equals(biding.getEndEvaluation())) {
            return new PageInfo<BidResult>(null);
        }

        List<OrderLine> orderLineList = orderLineMapper.getBidingResultList(bidingId);
        List<BidResult> bidResultList = new ArrayList<>();
        for (OrderLine oldLine : orderLineList) {
            BidResult bidResult = new BidResult();
            BeanUtils.copyProperties(oldLine, bidResult, "comments");
            BidRequirementLine bidRequirementLine = iBidRequirementLineService.getOne(new QueryWrapper<>(
                    new BidRequirementLine().setRequirementLineId(oldLine.getRequirementLineId())));
            BeanUtils.copyProperties(bidRequirementLine, bidResult, "comments");
            //??????????????????
            try {
                Organization organization = baseClient.getOrganizationByParam(new Organization().
                        setOrganizationId(bidRequirementLine.getOrgId()));
                if (organization != null) {
                    bidResult.setOrgCode(organization.getOrganizationCode());
                }
                //??????????????????
                PurchaseCategory purchaseCategory = baseClient.getPurchaseCategoryByParm(new PurchaseCategory().setCategoryId(bidRequirementLine.getCategoryId()));
                if (purchaseCategory != null) {
                    bidResult.setCategoryCode(purchaseCategory.getCategoryCode());
                }
            } catch (Exception e) {
                log.error("??????????????????:" + e.getMessage());
                throw new BaseException("??????????????????");
            }
            bidResult.setPurchaseCategory(bidRequirementLine.getCategoryName());
            //???????????????
            BidVendor bidVendor = iBidVendorService.getById(oldLine.getBidVendorId());
            CompanyInfo companyInfo = supplierClient.getCompanyInfo(bidVendor.getVendorId());
            bidResult.setVendorId(bidVendor.getVendorId());
            bidResult.setVendorName(companyInfo.getCompanyName());
            bidResult.setVendorCode(companyInfo.getCompanyCode());
            bidResult.setTaxKey(bidRequirementLine.getTaxKey());
            bidResult.setTaxRate(bidRequirementLine.getTaxRate());
            //??????????????????
            bidResult.setBidPriceIncludingTax(oldLine.getPrice());
            //?????????????????????
            bidResult.setBidPriceExcludingTax(oldLine.getPrice().divide(BigDecimal.ONE.add(
                    oldLine.getTaxRate().divide(BigDecimal.valueOf(100), 5, BigDecimal.ROUND_HALF_UP)),
                    4, BigDecimal.ROUND_HALF_UP));

            Long id = IdGenrator.generate();
            bidResult.setBidResultId(id).setBidingId(bidingId);
            bidResultList.add(bidResult);
        }
        this.saveBatch(bidResultList);
        return new PageInfo<>(bidResultList);
    }


    @Override
    public void updateBidResultBatchById(List<BidResult> bidResultList) {
        List<BidResult> newBidResultList = new ArrayList<>();
        for (BidResult oldBidResult : bidResultList) {
            BidResult newBidResult = new BidResult();
            newBidResult.setBidResultId(oldBidResult.getBidResultId()).setComments(oldBidResult.getComments()).
                    setBidingProportionQuota(oldBidResult.getBidingProportionQuota());
            newBidResultList.add(newBidResult);
        }
        this.updateBatchById(newBidResultList);
    }


    @Override
    public List<BidResult> getResultByBidingId(Long bidingId) {
        Biding biding = iBidingService.getById(bidingId);
        //?????????????????????????????????????????????????????? VISIBLE_WIN_VENDOR
        if (YesOrNo.NO.getValue().equals(biding.getVisibleWinVendor())) {
            return Collections.EMPTY_LIST;
        }
        List<BidResult> results = this.list(new QueryWrapper<>(new BidResult().setBidingId(bidingId)));
        for (int i = results.size() - 1; i >= 0; i++) {
            BidResult result = results.get(i);
            //??????????????????????????????????????? VISIBLE_TOTAL_RANKING
            if (YesOrNo.NO.getValue().equals(biding.getVisibleRankResult())) {
                result.setRank(null);
            }
            //?????????????????????????????? VISIBLE_FINAL_PRICE
            if (YesOrNo.NO.getValue().equals(biding.getVisibleFinalPrice())) {
                //???????????????
                result.setBidPriceIncludingTax(null);
            }

        }


        return results;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PageInfo<EvaluationResult> getNewResultByBidingId(Map<String, Object> paramMap) {
        Long bidingId = (Long) paramMap.get("bidingId");
        Integer pageSize = (Integer) paramMap.get("pageSize");
        Integer pageNum = (Integer) paramMap.get("pageNum");
        Biding biding = iBidingService.getOne(Wrappers.lambdaQuery(Biding.class).select(
                Biding::getCurrentRound,
                Biding::getVisibleWinVendor,
                Biding::getVisibleRankResult,
                Biding::getVisibleFinalPrice
        ).eq(Biding::getBidingId, bidingId));
        pageSize = 9999;

        //?????????????????????????????????????????????????????? VISIBLE_WIN_VENDOR
        if (YesOrNo.NO.getValue().equals(biding.getVisibleWinVendor())) {
            return new PageInfo<>(Collections.EMPTY_LIST);
        }
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        Boolean isVendor = Objects.equals(user.getUserType(), UserType.VENDOR.name());
        BidVendor one = iBidVendorService.getOne(Wrappers.lambdaQuery(BidVendor.class)
                .select(BidVendor::getBidVendorId, BidVendor::getVendorId)
                .eq(BidVendor::getBidingId, bidingId)
                .eq(BidVendor::getVendorId, user.getCompanyId())
        );
        if (Objects.isNull(one)) {
            return new PageInfo<>(Collections.EMPTY_LIST);
        }
        Long bidVendorId = isVendor ? one.getBidVendorId() : null;
        PageUtil.startPage(pageNum, pageSize);
        List<OrderLine> orderLines = orderLineMapper.selectList(Wrappers.lambdaQuery(OrderLine.class)
                .eq(OrderLine::getBidingId, bidingId)
                .eq(OrderLine::getRound, biding.getCurrentRound())
                .nested(e -> e.eq(OrderLine::getSelectionStatus, SelectionStatusEnum.WIN.getValue())
                        .or()
                        .nested(q -> q.eq(OrderLine::getWin, "Y").eq(isVendor, OrderLine::getBidVendorId, bidVendorId))
                )
        );
        if(CollectionUtils.isEmpty(orderLines)){
            return new PageInfo<>(Collections.EMPTY_LIST);
        }
        Set<Long> requirementId = orderLines.stream().map(OrderLine::getRequirementLineId).collect(Collectors.toSet());
        List<BidRequirementLine> bidRequirementLines = iBidRequirementLineService.listByIds(requirementId);
        List<EvaluationResult> results = new LinkedList<>();
        // ?????????id??????
        List<Long> vendorIds = new ArrayList<>();
        for (OrderLine orderLine : orderLines) {
            for (BidRequirementLine demandLine : bidRequirementLines) {
                if (Objects.equals(demandLine.getRequirementLineId(), orderLine.getRequirementLineId())) {
                    EvaluationResult vo = new EvaluationResult();
                    BeanUtils.copyProperties(demandLine, vo, getNullPropertyNames(demandLine));
                    BeanUtils.copyProperties(orderLine, vo, getNullPropertyNames(orderLine));
                    //??????????????????????????????????????? VISIBLE_TOTAL_RANKING
                    if (YesOrNo.NO.getValue().equals(biding.getVisibleRankResult())) {
                        vo.setRank(null);
                    }
                    //?????????????????????????????? VISIBLE_FINAL_PRICE
                    if (YesOrNo.NO.getValue().equals(biding.getVisibleFinalPrice())) {
                        //???????????????
                        vo.setPrice(null);
                    }
                    if(YesOrNo.YES.getValue().equals(biding.getVisibleWinVendor())){
                        // ?????????????????????
                        if (!ObjectUtils.isEmpty(orderLine.getBidVendorId())) {
                            vendorIds.add(orderLine.getBidVendorId());
                            vo.setVendorId(orderLine.getBidVendorId());
                        }
                    }
                    results.add(vo);
                    break;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(vendorIds)) {
            List<BidVendor> bidVendors = iBidVendorService.list(Wrappers.lambdaQuery(BidVendor.class).in(BidVendor::getBidVendorId, vendorIds));
            if(CollectionUtils.isNotEmpty(bidVendors)){
                Map<Long, BidVendor> contactInfoMap = bidVendors.stream().collect(Collectors.toMap(BidVendor::getBidVendorId, Function.identity(), (o1, o2) -> o1));
                results.forEach(evaluationResult -> {
                    if(!ObjectUtils.isEmpty(evaluationResult.getBidVendorId())){
                        BidVendor bidVendor = contactInfoMap.get(evaluationResult.getBidVendorId());
                        evaluationResult.setVendorId(bidVendor.getVendorId());
                        evaluationResult.setVendorCode(bidVendor.getCompanyCode());
                        evaluationResult.setVendorName(bidVendor.getVendorName());
                    }
                });
            }
        }
        return new PageInfo<>(results);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<AbandonmentBidSupVO> getAbandonBidSup(Long bidingId) {
        List<AbandonmentBidSupVO> result = new LinkedList<>();
        /*???????????????????????????biding????????????????????????bidvendor*/
        Map<Long, BidVendor> signUpVendorInfo = iBidVendorService.list(Wrappers
                .lambdaQuery(BidVendor.class)
                .select(BidVendor::getVendorName, BidVendor::getBidVendorId)
                .eq(BidVendor::getBidingId, bidingId))
                .stream().collect(Collectors.toMap(BidVendor::getBidVendorId, Function.identity()));
        /*??????????????????*/
        Map<Long, Set<Long>> requirementLineIdsAndBidVendorIdInOrderLines = orderLineMapper
                .getDistinctColumnByCondition(
                        Wrappers.lambdaQuery(OrderLine.class)
                                .select(OrderLine::getRequirementLineId, OrderLine::getBidVendorId)
                                .eq(OrderLine::getBidingId, bidingId))
                .stream().collect(Collectors.groupingBy(OrderLine::getRequirementLineId
                        , Collectors.mapping(OrderLine::getBidVendorId, Collectors.toSet())));
        //??????????????????
        List<BidRequirementLine> requirementLines = iBidRequirementLineService.list(Wrappers
                .lambdaQuery(BidRequirementLine.class)
                .select(BidRequirementLine::getItemGroup, BidRequirementLine::getTargetDesc,
                        BidRequirementLine::getRequirementLineId,
                        BidRequirementLine::getOrgName,
                        BidRequirementLine::getTargetNum)
                .eq(BidRequirementLine::getBidingId, bidingId));
        Set<Long> signUpBidVendorIds = signUpVendorInfo.keySet();
        for (BidRequirementLine requirementLine : requirementLines) {
            Set<Long> currentRequirementLineBidVendorIds = requirementLineIdsAndBidVendorIdInOrderLines.get(requirementLine.getRequirementLineId());
            signUpBidVendorIds
                    .stream()
                    .filter(e -> !currentRequirementLineBidVendorIds.contains(e))
                    .forEach(e -> {
                        AbandonmentBidSupVO vo = BeanCopyUtil.copyProperties(requirementLine, AbandonmentBidSupVO::new);
                        vo.setSupplierName(signUpVendorInfo.get(e).getVendorName());
                        result.add(vo);
                    });
        }
        return result;
    }

}
