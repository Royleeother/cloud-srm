package com.midea.cloud.srm.bid.purchaser.projectmanagement.techscore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.BidConstant;
import com.midea.cloud.common.enums.bid.projectmanagement.evaluation.OrderStatusEnum;
import com.midea.cloud.common.enums.bid.projectmanagement.techscore.ScoreStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.IBidVendorService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.IBidingService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.IGroupService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techproposal.service.IOrderHeadService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techscore.mapper.TechScoreHeadMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techscore.mapper.TechScoreLineMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techscore.service.ITechScoreHeadService;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.BidVendor;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.Group;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.techproposal.entity.OrderHead;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.techscore.entity.TechScoreHead;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.techscore.entity.TechScoreLine;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.techscore.param.TechScoreQueryParam;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.techscore.param.TechScoreSaveHeadParam;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.techscore.param.TechScoreSaveLineParam;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.techscore.vo.TechScoreHeadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 * ??????????????????
 * </pre>
 *
 * @author zhizhao1.fan@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020???3???19??? ??????7:15:45
 *  ????????????:
 *          </pre>
 */
@Service
public class TechScoreHeadServiceImpl extends ServiceImpl<TechScoreHeadMapper, TechScoreHead> implements ITechScoreHeadService {

    @Autowired
    private TechScoreLineMapper techScoreLineMapper;

    @Autowired
    private IOrderHeadService iOrderHeadService;

    @Autowired
    private IBidVendorService iBidVendorService;

    @Autowired
    private IGroupService iGroupService;

    @Autowired
    private IBidingService iBidingService;

    @Override
    public PageInfo<TechScoreHeadVO> listPage(TechScoreQueryParam queryParam) {
        if (Objects.isNull(queryParam.getOperateUserId())) {
            queryParam.setOperateUserId(AppUserUtil.getLoginAppUser().getUserId());
        }
        Assert.notNull(queryParam.getBidingId(), "?????????ID????????????");
        List<TechScoreHeadVO> techScoreHeadVoList = new ArrayList<TechScoreHeadVO>();
        // ???????????????????????????
        List<OrderHead> orderHeadList = iOrderHeadService.list(new QueryWrapper<OrderHead>(new OrderHead().setBidingId(queryParam.getBidingId()).setRound(BidConstant.FIRST_ROUND).setOrderStatus(OrderStatusEnum.SUBMISSION.getValue())));
        if (orderHeadList.size() > 0) {
            List<Long> bidVendorIdList = orderHeadList.stream().map(OrderHead::getBidVendorId).collect(Collectors.toList());
            Assert.notEmpty(bidVendorIdList, "?????????????????????ID????????????");
            List<BidVendor> vendorList = iBidVendorService.listByIds(bidVendorIdList);
            // ??????????????????????????????????????????
            List<TechScoreHead> techScoreHeadList = this.list(new QueryWrapper<>(new TechScoreHead().setBidingId(queryParam.getBidingId()).setCreatedId(queryParam.getOperateUserId()).setScoreStatus(ScoreStatusEnum.FINISHED.getValue())));
            Map<Long, TechScoreHead> techScoreHeadMap = techScoreHeadList.stream().collect(Collectors.toMap(TechScoreHead::getBidVendorId, Function.identity(), (k1, k2) -> k1));
            for (BidVendor vendor : vendorList) {
                TechScoreHeadVO techScoreHeadVO = new TechScoreHeadVO();
                techScoreHeadVO.setBidVendorId(vendor.getBidVendorId()).setVendorId(vendor.getVendorId()).setVendorCode(vendor.getVendorCode()).setVendorName(vendor.getVendorName()).setPhone(vendor.getPhone()).setEmail(vendor.getEmail()).setLinkManName(vendor.getLinkManName());
                techScoreHeadVO.setScoreStatus(techScoreHeadMap.containsKey(vendor.getBidVendorId()) ? ScoreStatusEnum.FINISHED.getValue() : ScoreStatusEnum.UNFINISHED.getValue());
                techScoreHeadVoList.add(techScoreHeadVO);
            }
        }
        return PageUtil.pagingByFullData(queryParam.getPageNum(), queryParam.getPageSize(), techScoreHeadVoList);
    }

    @Override
    public void saveOrUpdateTechScore(TechScoreSaveHeadParam param) {

        // ?????? - ?????????????????????????????????
        validateOperateTechScore(param);

        TechScoreHead techScoreHead = this.getOne(new QueryWrapper<>(new TechScoreHead().setBidingId(param.getBidingId()).setCreatedId(param.getOperateUserId()).setBidVendorId(param.getBidVendorId())));
        if (techScoreHead == null) {
            techScoreHead = new TechScoreHead().setTechScoreHeadId(IdGenrator.generate());
            techScoreHead.setCreatedId(param.getOperateUserId());
            techScoreHead.setCreatedBy(param.getOperateUsername());
            techScoreHead.setCreationDate(new Date());
            techScoreHead.setCreatedByIp("");
            techScoreHead.setLastUpdateDate(new Date());
        }
//        Assert.isTrue(ScoreStatusEnum.get(techScoreHead.getScoreStatus()) != ScoreStatusEnum.FINISHED, "????????????????????????????????????");
        if ("SAVE".equals(param.getType())) {
            techScoreHead.setScoreStatus(ScoreStatusEnum.UNFINISHED.getValue());
        } else if ("SUBMIT".equals(param.getType())) {
            techScoreHead.setScoreStatus(ScoreStatusEnum.FINISHED.getValue());
        }
        techScoreHead.setBidingId(param.getBidingId());
        techScoreHead.setBidVendorId(param.getBidVendorId());
        techScoreHead.setTechComments(param.getTechComments());
        this.saveOrUpdate(techScoreHead);
        techScoreLineMapper.delete(new QueryWrapper<>(new TechScoreLine().setTechScoreHeadId(techScoreHead.getTechScoreHeadId())));// ????????????????????????
        for (TechScoreSaveLineParam line : param.getLineList()) {
            TechScoreLine techScoreLine = new TechScoreLine();
            techScoreLine.setTechScoreLineId(IdGenrator.generate()).setBidingId(param.getBidingId()).setTechScoreHeadId(techScoreHead.getTechScoreHeadId()).setRuleLineId(line.getRuleLineId()).setScore(line.getScore());
            techScoreLineMapper.insert(techScoreLine);
        }
    }

    /**
     * ?????? - ?????????????????????????????????
     *
     * @param parameter ????????????
     */
    protected void validateOperateTechScore(TechScoreSaveHeadParam parameter) {
        if (Objects.isNull(parameter.getOperateUserId())) {
            parameter.setOperateUserId(AppUserUtil.getLoginAppUser().getUserId());
        }
        Assert.notNull(parameter.getBidingId(), "?????????ID????????????");
        Assert.notNull(parameter.getBidVendorId(), "?????????ID?????????");

        Biding biding = iBidingService.getById(parameter.getBidingId());
        Assert.notNull(biding, "?????????ID?????????");

//        Assert.isTrue("Y".equals(biding.getTechOpenBid()), "??????????????????????????????");

        // ??????????????????????????????????????????????????????
        if (parameter.getProxyOperateUserId() != null && parameter.getProxyOperateUserId().equals(biding.getCreatedId()))
            return;

        // ?????? ????????????????????????????????????
        Group group = iGroupService.getOne(
                Wrappers.lambdaQuery(Group.class)
                        .eq(Group::getBidingId, parameter.getBidingId())
                        .eq(Group::getUserId, parameter.getOperateUserId())
        );
        if (group == null)
            throw new BaseException("????????????????????????????????????");

        // ???????????????????????????????????????
        if ("Y".equals(group.getIsFirstResponse()))
            return;

        if (!"Y".equals(group.getJudgeFlag()))
            throw new BaseException("??????????????????????????????????????????????????????");
    }
}
