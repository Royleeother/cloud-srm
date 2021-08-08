package com.midea.cloud.srm.bargaining.purchaser.projectmanagement.evaluation.service.impl;

import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.evaluation.service.ICalculateCombinedPriceScoreService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.evaluation.service.ICalculatePriceScoreService;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.evaluation.enums.CalculatePriceScorePolicy;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techproposal.entity.OrderLine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;

import static com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.evaluation.enums.CalculatePriceScorePolicy.COMBINED_HIGH_PRICE;

/**
 * Implement of {@link ICalculatePriceScoreService} with policy {@code COMBINED_HIGH_PRICE}.
 *
 * @author zixuan.yan@meicloud.com
 */
@Service
public class CalculateCombinedHighPriceScoreServiceImpl implements ICalculateCombinedPriceScoreService {

    @Override
    public void calculateAndSet(Collection<OrderLine> orderLines) {
        if (orderLines.isEmpty())
            return;

        // 获取 招标单ID
        Long bidding = orderLines.stream().findAny()
                .map(OrderLine::getBidingId)
                .orElseThrow(() -> new BaseException("获取招标单ID失败。"));


        // 获取 价格权重
        Integer scoreWeight = this.getPriceScoreWeight(bidding);

        // 计算并设置组合价格
        this.calculateAndSetCombinedPrice(orderLines);

        // 计算得分 - 高价法
        orderLines.forEach(orderLine -> {
            BigDecimal priceScore = this.calculateHighPriceScore(
                    scoreWeight,
                    orderLine.getTotalDiscountAmount(),
                    orderLine.getCurrentRoundMaxDiscountAmount()
            );
            orderLine.setPriceScore(priceScore);
        });
    }

    @Override
    public CalculatePriceScorePolicy getPolicy() {
        return COMBINED_HIGH_PRICE;
    }
}
