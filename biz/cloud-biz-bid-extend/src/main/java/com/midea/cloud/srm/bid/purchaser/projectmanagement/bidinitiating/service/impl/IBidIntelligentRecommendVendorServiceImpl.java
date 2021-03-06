package com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.component.entity.EntityManager;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.mapper.BidRequirementLineMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.mapper.BidingMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.IBidIntelligentRecommendVendorService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.supplier.VendorOrgCateRelClient;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.vo.SourceForm;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.enums.BiddingAwardWay;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuDetailVO;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuGroupDetailVO;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.vo.FindVendorPricingPermissionParameter;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.vo.VendorPricingPermission;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.vendororgcategory.vo.FindVendorOrgCateRelParameter;
import com.midea.cloud.srm.model.supplier.vendororgcategory.vo.VendorOrgCateRelsVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.midea.cloud.common.utils.Functions.distinctByKey;

/**
 * Implement of {@link IBidIntelligentRecommendVendorService}.
 *
 * @author zixuan.yan@meicloud.com
 */
@Service
public class IBidIntelligentRecommendVendorServiceImpl implements IBidIntelligentRecommendVendorService {

    private final EntityManager<Biding>             biddingDao
            = EntityManager.use(BidingMapper.class);
    private final EntityManager<BidRequirementLine> requirementLineDao
            = EntityManager.use(BidRequirementLineMapper.class);

    @Resource
    private VendorOrgCateRelClient  vendorOrgCateRelClient;
    @Resource
    private BaseClient              baseClient;


    @Override
    public List<CompanyInfo> findVendors(FindVendorPricingPermissionParameter parameter) {
        return findInitialVendorPricingPermissions(parameter).stream()
                .map(VendorPricingPermission::getCompanyInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<VendorPricingPermission> findInitialVendorPricingPermissions(FindVendorPricingPermissionParameter parameter) {
        Assert.notNull(parameter.getBidding(), "?????????ID???????????????");

        // ?????? ?????????
        Biding bidding = Optional.ofNullable(parameter.getBidding())
                .map(biddingDao::findById)
                .orElseThrow(() -> new BaseException("???????????????????????? | biddingId: [" + parameter.getBidding() + "]"));

        // ?????? ??????????????????????????????
        List<BidRequirementLine> demandLines = requirementLineDao.findAll(
                Wrappers.lambdaQuery(BidRequirementLine.class)
                        .eq(BidRequirementLine::getBidingId, bidding.getBidingId())
        );
        if (CollectionUtils.isEmpty(demandLines))
            return Collections.emptyList();

        Long[] vendorIds = parameter.getVendorIds();
        String bidingAwardWay = bidding.getBidingAwardWay();
        return getVendorPricingPermissions(demandLines, vendorIds, bidingAwardWay);
    }


    /**
     * ?????????????????????????????????
     * @param form
     * @return
     */
    @Override
    public List<CompanyInfo> findVendors(SourceForm form) {
        Collection<BidRequirementLine> demandLines = form.getDemandLines();
        Biding bidding = form.getBidding();
        return getVendorPricingPermissions(demandLines,null,bidding.getBidingAwardWay()).stream()
                .map(VendorPricingPermission::getCompanyInfo)
                .collect(Collectors.toList());
    }

    /**
     * ??????OU???[??????]???????????????
     *
     * @param demandLines ???????????????
     * @return [??????]?????????????????????
     */
    protected List<BidRequirementLine> splitDemandLinesWithOuGroup(Collection<BidRequirementLine> demandLines) {

        // ????????? OU?????????????????????
        List<Long> ouIds = demandLines.stream()
                .filter(line -> line.getOuId() != null)
                .map(BidRequirementLine::getOuId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, List<BaseOuDetailVO>> ouGroups = ouIds.isEmpty()
                ? Collections.emptyMap()
                : baseClient.queryOuInfoDetailByIds(ouIds).stream()
                .collect(Collectors.toMap(BaseOuGroupDetailVO::getOuGroupId, BaseOuGroupDetailVO::getDetails));


        // ??????OU????????????????????????
        return demandLines.stream().flatMap(line -> {

            // ???OU???????????????????????????
            if (line.getOuId() == null)
                return Stream.of(line);

            // OU??????????????????
            return ouGroups.getOrDefault(line.getOuId(), Collections.emptyList()).stream()
                    .map(org -> {
                        BidRequirementLine clone = new BidRequirementLine();
                        BeanUtils.copyProperties(line, clone);
                        clone.setOrgId(org.getOuId());
                        clone.setOrgName(org.getOuName());
                        return clone;
                    });

        }).collect(Collectors.toList());
    }

    /**
     * ??????OU???[??????]???????????????
     *
     * @param splitDemandLines [??????]?????????????????????
     * @return [??????]?????????????????????
     */
    protected List<BidRequirementLine> mergeDemandLinesWithOuGroup(List<BidRequirementLine> splitDemandLines) {
        return splitDemandLines.stream()
                .filter(distinctByKey(BidRequirementLine::getRequirementLineId))    // ??????
                .map(line -> {
                    BidRequirementLine clone = new BidRequirementLine();
                    BeanUtils.copyProperties(line, clone);
                    if (clone.getOuId() != null) {
                        clone.setOrgId(null);
                        clone.setOrgName(null);
                    }
                    return clone;
                })
                .collect(Collectors.toList());
    }


    /**
     *
     * @param demandLines ?????????
     * @param vendorIds ?????????????????????id
     * @param bidingAwardWay ??????????????????
     * @return
     */
    @Override
    public List<VendorPricingPermission> getVendorPricingPermissions(Collection<BidRequirementLine> demandLines, Long[] vendorIds, String bidingAwardWay) {
        // ??????OU???[??????]???????????????
        List<BidRequirementLine> splitDemandLines = this.splitDemandLinesWithOuGroup(demandLines);


        // ?????? ???????????????????????????????????????
        List<VendorOrgCateRelsVO> vendorOrgCateRels = vendorOrgCateRelClient.findValidVendorOrgCateRels(
                FindVendorOrgCateRelParameter.builder()
                        .vendorIds(vendorIds)
                        .orgCateComposes(
                                splitDemandLines.stream()
                                        .map(line -> new FindVendorOrgCateRelParameter.OrgCateCompose(line.getOrgId(), line.getCategoryId()))
                                        .collect(Collectors.toList())
                        ).isBargain(false)
                        .build()
        );


        // ???????????? & ??????OU?????????????????????
        return vendorOrgCateRels.stream()
                .map(rel -> {

                    // ?????? [??????]?????????????????????????????????
                    List<BidRequirementLine> permissions = this.findPerInitialVendorPricingPermissions(
                            rel,
                            Objects.equals(bidingAwardWay, BiddingAwardWay.COMBINED_DECISION), demandLines,
                            splitDemandLines
                    );

                    // ????????????
                    return VendorPricingPermission.builder()
                            .companyInfo(rel.getCompanyInfo())
                            .requirementLines(permissions)
                            .build();

                })
                .filter(permission -> !CollectionUtils.isEmpty(permission.getRequirementLines()))   // ?????????????????????????????????
                .collect(Collectors.toList());
    }

    /**
     * ?????? [??????]?????????????????????????????????
     *
     * @param rel               ???????????????????????????
     * @param isCombinedDecision   ??????????????????
     * @param demandLines       [??????]?????????????????????
     * @param splitDemandLines  [??????]?????????????????????
     * @return [??????]?????????????????????????????????
     */
    protected List<BidRequirementLine> findPerInitialVendorPricingPermissions(VendorOrgCateRelsVO rel,
                                                                              Boolean isCombinedDecision, Collection<BidRequirementLine> demandLines,
                                                                              Collection<BidRequirementLine> splitDemandLines) {

        // ???[????????????] ?????????
        Map<String, List<BidRequirementLine>> itemGroupDemandLines = demandLines.stream()
                .filter(demandLine -> StringUtils.hasText(demandLine.getItemGroup()))
                .collect(Collectors.groupingBy(BidRequirementLine::getItemGroup));


        // ???[?????? + ??????]?????? ?????????
        Map<Long, Map<Long, List<BidRequirementLine>>> orgCateSplitDemandLines = splitDemandLines.stream()
                .collect(Collectors.groupingBy(
                        BidRequirementLine::getOrgId,
                        Collectors.groupingBy(BidRequirementLine::getCategoryId)
                ));



        // ?????? ???????????????????????????????????????????????????
        List<BidRequirementLine> permissions = this.mergeDemandLinesWithOuGroup(
                rel.getOrgCategories().stream()
                        .filter(orgCategory -> orgCateSplitDemandLines.containsKey(orgCategory.getOrgId()))
                        .flatMap(orgCategory -> orgCateSplitDemandLines.get(orgCategory.getOrgId())
                                .getOrDefault(orgCategory.getCategoryId(), Collections.emptyList())
                                .stream()
                        )
                        .collect(Collectors.toList())
        );


        // ???[????????????]???????????????????????????????????????????????????
        if (!isCombinedDecision)
            return permissions;


        // ???[??????]?????? ???????????????????????????????????????????????????
        Map<String, List<BidRequirementLine>> itemGroupPermissions = permissions.stream()
                .filter(demandLine -> StringUtils.hasText(demandLine.getItemGroup()))
                .collect(Collectors.groupingBy(BidRequirementLine::getItemGroup));

        // [????????????]?????????????????????????????????????????????????????????
        return itemGroupPermissions.keySet().stream()
                .filter(itemGroupPermissions::containsKey)
                .filter(itemGroupKey -> {

                    // ??????[????????????]??????????????????????????????????????????????????????[ID]
                    List<Long> itemGroupPermissionIds = itemGroupPermissions.get(itemGroupKey).stream()
                            .map(BidRequirementLine::getRequirementLineId)
                            .collect(Collectors.toList());

                    // ??????[????????????]??????????????????[ID]
                    List<Long> itemGroupDemandLineIds = itemGroupDemandLines.get(itemGroupKey).stream()
                            .map(BidRequirementLine::getRequirementLineId)
                            .collect(Collectors.toList());

                    // ?????????????????????????????????
                    return itemGroupPermissionIds.size() == itemGroupDemandLineIds.size()
                            && itemGroupPermissionIds.containsAll(itemGroupDemandLineIds);

                })
                .flatMap(itemGroupKey -> itemGroupPermissions.get(itemGroupKey).stream())
                .collect(Collectors.toList());
    }



}
