package com.midea.cloud.srm.bargaining.purchaser.projectmanagement.evaluation.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.enums.bargaining.projectmanagement.evaluation.ScoreDimensionEnum;
import com.midea.cloud.common.enums.bargaining.projectmanagement.techscore.ScoreStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.component.entity.EntityManager;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.mapper.BidScoreRuleLineMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.mapper.BidVendorMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.mapper.GroupMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.evaluation.service.ICalculateTechnologyScoreService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techscore.mapper.TechScoreHeadMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techscore.mapper.TechScoreLineMapper;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidVendor;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.Group;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.evaluation.entity.ScoreRuleLine;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techproposal.entity.OrderLine;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techscore.entity.TechScoreHead;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techscore.entity.TechScoreLine;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techscore.vo.CalculateVendorTechnologyScoreParameter;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techscore.vo.VendorTechnologyScore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implement of {@link ICalculateTechnologyScoreService}.
 *
 * @author zixuan.yan@meicloud.com
 */
@Service
public class CalculateTechnologyScoreServiceImpl implements ICalculateTechnologyScoreService {

    private final EntityManager<BidVendor>      bidVendorDao
            = EntityManager.use(BidVendorMapper.class);
    private final EntityManager<ScoreRuleLine>  scoreRuleLineDao
            = EntityManager.use(BidScoreRuleLineMapper.class);
    private final EntityManager<Group>          gradeGroupDao
            = EntityManager.use(GroupMapper.class);
    private final EntityManager<TechScoreHead>  techScoreHeaderDao
            = EntityManager.use(TechScoreHeadMapper.class);
    private final EntityManager<TechScoreLine>  techScoreLineDao
            = EntityManager.use(TechScoreLineMapper.class);


    @Override
    public List<VendorTechnologyScore> calculate(CalculateVendorTechnologyScoreParameter parameter) {
        if (CollectionUtils.isEmpty(parameter.getBidVendorIds()))
            return Collections.emptyList();


        // ?????? ?????????????????????
        List<BidVendor> bidVendors = bidVendorDao.findAll(
                Wrappers.lambdaQuery(BidVendor.class)
                        .in(BidVendor::getBidVendorId, parameter.getBidVendorIds())
        );

        // ?????? ?????????ID
        Long bidding = bidVendors.stream().findAny()
                .map(BidVendor::getBidingId)
                .orElseThrow(() -> new BaseException("???????????????ID?????????"));


        // ?????? [??????]?????????????????????
        List<ScoreRuleLine> scoreRuleLines = scoreRuleLineDao.findAll(
                Wrappers.lambdaQuery(ScoreRuleLine.class)
                        .eq(ScoreRuleLine::getBidingId, bidding)
                        .eq(ScoreRuleLine::getScoreDimension, ScoreDimensionEnum.TECHNOLOGY.getValue())
        );
        // ?????? [??????]???????????????
        Integer totalWeight = scoreRuleLines.stream()
                .map(scoreRuleLine -> Optional.ofNullable(scoreRuleLine.getScoreWeight()).orElse(0))
                .reduce(0, Integer::sum);
        // ????????????[??????]??????????????????????????????0?????????
        if (scoreRuleLines.isEmpty() || totalWeight == 0) {
            return parameter.getBidVendorIds().stream()
                    .map(bidVendorId -> VendorTechnologyScore.builder()
                            .bidVendorId(bidVendorId)
                            .score(BigDecimal.ZERO)
                            .scoreWithoutWeight(BigDecimal.ZERO)
                            .weight(0)
                            .build())
                    .collect(Collectors.toList());
        }


        // ?????? ??????????????????????????????????????????[???]
        List<TechScoreHead> techScoreHeaders = techScoreHeaderDao.findAll(
                Wrappers.lambdaQuery(TechScoreHead.class)
                        .eq(TechScoreHead::getBidingId, bidding)
                        .eq(TechScoreHead::getScoreStatus, ScoreStatusEnum.FINISHED.getValue())
        );
        // ???[??????????????? + ??????]??????
        Map<Long, Map<Long, TechScoreHead>> techScoreHeadersMap = techScoreHeaders.stream()
                .collect(Collectors.groupingBy(
                        TechScoreHead::getBidVendorId,
                        Collectors.toMap(TechScoreHead::getCreatedId, x -> x, (x, y) -> x))
                );
        // ????????????????????????????????????0?????????
        if (techScoreHeaders.isEmpty()) {
            return parameter.getBidVendorIds().stream()
                    .map(bidVendorId -> VendorTechnologyScore.builder()
                            .bidVendorId(bidVendorId)
                            .score(BigDecimal.ZERO)
                            .scoreWithoutWeight(BigDecimal.ZERO)
                            .weight(0)
                            .build())
                    .collect(Collectors.toList());
        }


        // ?????? ??????????????????????????????????????????[???]
        List<TechScoreLine> techScoreLines = techScoreLineDao.findAll(
                Wrappers.lambdaQuery(TechScoreLine.class).in(
                        TechScoreLine::getTechScoreHeadId,
                        techScoreHeaders.stream()
                                .map(TechScoreHead::getTechScoreHeadId)
                                .collect(Collectors.toSet())
                )
        );
        // ???[?????????????????????]??????
        Map<Long, List<TechScoreLine>> techScoreLinesMap = techScoreLines.stream()
                .collect(Collectors.groupingBy(TechScoreLine::getTechScoreHeadId));


        // ?????? ???????????????
        List<Group> gradeGroups = gradeGroupDao.findAll(
                Wrappers.lambdaQuery(Group.class)
                        .eq(Group::getBidingId, bidding)
                        .eq(Group::getJudgeFlag, "Y")
        );

        // ?????? ??????????????????
        BigDecimal maxEvaluateScore = gradeGroups.stream()
                .map(gradeGroup -> BigDecimal.valueOf(gradeGroup.getMaxEvaluateScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // ?????? [???????????????]???????????????
        return bidVendors.stream().map(bidVendor -> {

            // ?????? ???????????????????????????????????????[???]??????[??????]?????????
            Map<Long, TechScoreHead> vendorTechScoreHeaders = techScoreHeadersMap.get(bidVendor.getBidVendorId());

            // ??????????????????????????????????????????0?????????
            if (vendorTechScoreHeaders == null) {
                return VendorTechnologyScore.builder()
                        .bidVendorId(bidVendor.getBidVendorId())
                        .score(BigDecimal.ZERO)
                        .scoreWithoutWeight(BigDecimal.ZERO)
                        .weight(0)
                        .build();
            }


            // ?????? ???????????????????????????????????????[???]
            List<TechScoreLine> vendorTechScoreLines = vendorTechScoreHeaders.values().stream()
                    .flatMap(header -> techScoreLinesMap.get(header.getTechScoreHeadId()).stream())
                    .collect(Collectors.toList());

            // ?????? ??????????????? - ??????????????????????????????[??????????????????]?????????????????????
            BigDecimal techTotalScoreWithWeight = scoreRuleLines.stream()
                    .map(scoreRuleLine -> {

                        // ?????? ????????????????????????
                        BigDecimal sumScore = vendorTechScoreLines.stream()
                                .filter(vendorTechScoreLine -> scoreRuleLine.getRuleLineId().equals(vendorTechScoreLine.getRuleLineId()))   // ?????????????????????????????????
                                .map(TechScoreLine::getScore)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // ?????? ????????????????????????
                        BigDecimal hundredPercentageScore = BigDecimal.valueOf(100).multiply(
                                sumScore.divide(maxEvaluateScore, 2, RoundingMode.HALF_DOWN)
                        );

                        // ????????????????????? * ????????????
                        return BigDecimal.valueOf(scoreRuleLine.getScoreWeight() / 100D).multiply(hundredPercentageScore);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // ????????????????????????????????????
            return VendorTechnologyScore.builder()
                    .bidVendorId(bidVendor.getBidVendorId())
                    .score(techTotalScoreWithWeight)
                    .scoreWithoutWeight(techTotalScoreWithWeight.divide(BigDecimal.valueOf(totalWeight * 0.01), 2, RoundingMode.HALF_UP))
                    .weight(totalWeight)
                    .build();

        }).collect(Collectors.toList());
    }

    @Override
    public void calculateAndSet(Collection<OrderLine> orderLines) {
        if (orderLines.isEmpty())
            return;

        // ?????? ??????????????????[???????????????]?????????
        Map<Long, VendorTechnologyScore> vendorTechnologyScores = this.calculate(
                CalculateVendorTechnologyScoreParameter.builder()
                        .bidVendorIds(orderLines.stream()
                                .map(OrderLine::getBidVendorId)
                                .collect(Collectors.toSet()))
                        .build()
        ).stream().collect(Collectors.toMap(VendorTechnologyScore::getBidVendorId, x -> x));

        // ???[???????????????]???????????????????????????
        orderLines.stream()
                .collect(Collectors.groupingBy(OrderLine::getBidVendorId))
                .forEach((bidVendorId, orderLinesGroup) -> orderLinesGroup.forEach(orderLine -> {

                    // ?????? ???????????????????????????
                    BigDecimal techScore = Optional.ofNullable(vendorTechnologyScores.get(orderLine.getBidVendorId()))
                            .map(VendorTechnologyScore::getScore)
                            .orElse(BigDecimal.ZERO);

                    // ?????? ???????????????????????????
                    orderLine.setTechScore(techScore);
                }));
    }
}
