package com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.vo;

import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLineTemplatePrice;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLineTemplatePriceLine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * VO for {@link BidRequirementLineTemplatePrice} and {@link BidRequirementLineTemplatePriceLine}.
 *
 * @author zixuan.yan@meicloud.com
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BidRequirementLineTemplatePriceVO implements Serializable {

    private BidRequirementLineTemplatePrice templatePrice;
    private List<BidRequirementLineTemplatePriceLine>   templatePriceLines;
}