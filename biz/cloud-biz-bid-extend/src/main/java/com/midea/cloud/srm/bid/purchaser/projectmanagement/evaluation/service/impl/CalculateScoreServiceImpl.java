package com.midea.cloud.srm.bid.purchaser.projectmanagement.evaluation.service.impl;

import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.component.context.container.SpringContextHolder;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.evaluation.service.*;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.enums.BiddingAwardWay;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.enums.CalculatePriceScorePolicy;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.enums.EvaluateMethod;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.techproposal.entity.OrderLine;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implement of {@link ICalculateScoreService}.
 *
 * @author zixuan.yan@meicloud.com
 */
@Service
public class CalculateScoreServiceImpl implements ICalculateScoreService {

    private final Supplier<Collection<ICalculatePriceScoreService>>     calculatePriceScoreServiceSupplier
            = () -> SpringContextHolder.getApplicationContext().getBeansOfType(ICalculatePriceScoreService.class).values();
    private final Supplier<ICalculateTechnologyScoreService>            calculateTechnologyScoreServiceSupplier
            = () -> SpringContextHolder.getBean(ICalculateTechnologyScoreService.class);
    private final Supplier<ICalculatePerformanceScoreService>           calculatePerformanceScoreServiceSupplier
            = () -> SpringContextHolder.getBean(ICalculatePerformanceScoreService.class);
    private final Supplier<ICalculateCompositeScoreService>             calculateCompositeScoreServiceSupplier
            = () -> SpringContextHolder.getBean(ICalculateCompositeScoreService.class);


    @Override
    public void calculateAndSet(Biding bidding, List<OrderLine> orderLines) {

        // ?????????????????????/?????????
        BiddingAwardWay bidingAwardWay = BiddingAwardWay.get(bidding.getBidingAwardWay());
        // ?????????????????????/??????/?????????
        EvaluateMethod evaluateMethod = EvaluateMethod.get(bidding.getEvaluateMethod());

        // ?????? ????????????
        calculatePriceScoreServiceSupplier.get().stream()
                .filter(service -> service.getPolicy().equals(CalculatePriceScorePolicy.get(bidingAwardWay, evaluateMethod)))   // ??????????????????????????????????????????
                .findAny()
                .orElseThrow(() -> new BaseException("?????????????????????????????????"))
                .calculateAndSet(orderLines);

        // ?????? ????????????
        calculateTechnologyScoreServiceSupplier.get().calculateAndSet(orderLines);

        // ?????? ????????????
        calculatePerformanceScoreServiceSupplier.get().calculateAndSet(orderLines);


        // ??????[????????????????????????]?????? ????????????
        calculateCompositeScoreServiceSupplier.get().calculateAndSet(orderLines);
    }

    @Override
    public void rankAndSet(List<OrderLine> orderLines) {
        orderLines.stream()
                .collect(Collectors.groupingBy(OrderLine::getRequirementLineId))    // ??????????????????????????????
                .forEach(((demandLineId, orderLinesGroup) -> {

                    // ?????? [????????????]
                    List<OrderLine> sortedOrderLines = orderLinesGroup.stream()
                            .sorted(Comparator.comparing(OrderLine::getCompositeScore).reversed())
                            .collect(Collectors.toList());

                    // ????????????
                    Stream.iterate(0, index -> index + 1)
                            .limit(sortedOrderLines.size())
                            .forEach(index -> {

                                // ?????????????????????????????????
                                OrderLine orderLine = sortedOrderLines.get(index);
                                if (index == 0) {
                                    orderLine.setRank(index + 1);
                                    return;
                                }

                                // ????????????????????????????????????????????????????????????
                                OrderLine lastOrderLine = sortedOrderLines.get(index - 1);
                                if (orderLine.getCompositeScore().compareTo(lastOrderLine.getCompositeScore()) == 0) {
                                    orderLine.setRank(lastOrderLine.getRank());
                                }
                                else {
                                    orderLine.setRank(lastOrderLine.getRank() + 1);
                                }

                            });

                }));
    }
}
