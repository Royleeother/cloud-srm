package com.midea.cloud.srm.bid.purchaser.projectmanagement.businessproposal.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.constants.BidConstant;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.bid.projectmanagement.bidinitiating.BidType;
import com.midea.cloud.common.enums.bid.projectmanagement.evaluation.OrderStatusEnum;
import com.midea.cloud.common.enums.bid.projectmanagement.evaluation.SelectionStatusEnum;
import com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingProjectStatus;
import com.midea.cloud.common.enums.bid.projectmanagement.techscore.ScoreStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.BeanCopyUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.component.entity.EntityManager;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.IBidRequirementLineService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.IBidVendorService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.IBidingService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.IGroupService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.businessproposal.service.IBusinessProposalService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.businessproposal.service.IRoundService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.evaluation.service.IEvaluationService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techproposal.mapper.OrderHeadFileMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techproposal.mapper.OrderLineMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techproposal.service.IOrderHeadService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techproposal.service.IOrderLineService;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.techscore.service.ITechScoreHeadService;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.BidVendor;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.Group;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.businessproposal.entity.Round;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.businessproposal.vo.BusinessItemVO;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.businessproposal.vo.CancelBidParam;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.businessproposal.vo.OrderDetailVO;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.enums.BidingFileTypeEnum;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.param.EvaluationQueryParam;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.evaluation.vo.EvaluationResult;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.techproposal.entity.OrderHead;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.techproposal.entity.OrderHeadFile;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.techproposal.entity.OrderLine;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.techscore.entity.TechScoreHead;
import com.midea.cloud.srm.model.bid.suppliercooperate.vo.BidOrderHeadVO;
import com.midea.cloud.srm.model.bid.suppliercooperate.vo.BidOrderLineVO;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 * ????????????
 * </pre>
 *
 * @author zhizhao1.fan@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020???3???25??? ??????11:01:06
 *  ????????????:
 *          </pre>
 */
@Service
public class BusinessProposalServiceImpl implements IBusinessProposalService {

    @Autowired
    private IOrderHeadService iOrderHeadService;

    @Autowired
    private IOrderLineService iOrderLineService;

    @Autowired
    private IBidingService iBidingService;

    @Autowired
    private IRoundService iRoundService;

    @Autowired
    private IBidVendorService iBidVendorService;

    @Autowired
    private ITechScoreHeadService iTechScoreHeadService;

    @Autowired
    private IGroupService iGroupService;

    @Autowired
    private IBidRequirementLineService iBidRequirementLineService;
    @Autowired
    private OrderHeadFileMapper orderHeadFileMapper;

    @Resource
    private IEvaluationService evaluationService;

    private final EntityManager<OrderLine> orderLineDao = EntityManager.use(OrderLineMapper.class);

    @Override
    public String openBidCheck(Long bidingId) {
        Biding biding = iBidingService.getById(bidingId);
        Assert.notNull(biding, "?????????ID?????????");
        Integer currentRound = biding.getCurrentRound();
        Assert.notNull(currentRound, "??????????????????????????????????????????");
        Date currentDate = new Date();
        ///todo ??????????????????????????????
       /* boolean after = biding.getEnrollEndDatetime().after(currentDate);
        if(after){
            throw new BaseException("?????????????????????????????????????????????");
        }*/
        Round round = iRoundService.getOne(new QueryWrapper<Round>(new Round().setBidingId(bidingId).setRound(currentRound)));
        Assert.notNull(round, "?????????????????????");
//		Assert.isTrue(!"Y".equals(round.getBusinessOpenBid()), "??????????????????????????????????????????");
        // ??????????????????????????????????????????????????????????????????????????????
        if (round.getEndTime().after(currentDate)) {
            int notSubmitPriceCount = 0;// ??????????????????????????????
            List<Long> hadJoinVendorId = iBidVendorService.list(new QueryWrapper<BidVendor>(new BidVendor().setBidingId(bidingId).setJoinFlag("Y")).select("VENDOR_ID")).stream().map(BidVendor::getVendorId).collect(Collectors.toList());
            if (currentRound == BidConstant.FIRST_ROUND) {
                List<OrderHead> orderHeadList = iOrderHeadService.list(new QueryWrapper<OrderHead>(new OrderHead().setBidingId(bidingId).setRound(currentRound).setOrderStatus(OrderStatusEnum.SUBMISSION.getValue())).select("BID_VENDOR_ID", "ORDER_HEAD_ID"));
                notSubmitPriceCount = hadJoinVendorId.size() - orderHeadList.size();
            } else {
                Function<OrderHead, Optional<Long>> getVendorIdByOrderHeader = e -> Optional.of(iBidVendorService.getOne(Wrappers.lambdaQuery(BidVendor.class)
                        .select(BidVendor::getVendorId)
                        .eq(BidVendor::getBidVendorId, e.getBidVendorId())).getVendorId());
                //????????????????????????????????????,?????????????????????
                List<OrderHead> orderLastHeadList = iOrderHeadService.list(Wrappers.lambdaQuery(OrderHead.class)
                        .select(OrderHead::getBidVendorId, OrderHead::getOrderHeadId, OrderHead::getPricingType)
                        .eq(OrderHead::getBidingId, bidingId)
                        .eq(OrderHead::getRound, currentRound - 1)
                        .eq(OrderHead::getOrderStatus, OrderStatusEnum.SUBMISSION.getValue()));
                Map<Long, Optional<Long>> currentBidVendorAndVendorIdMap = iOrderHeadService.list(Wrappers.lambdaQuery(OrderHead.class)
                        .select(OrderHead::getBidVendorId, OrderHead::getOrderHeadId)
                        .eq(OrderHead::getBidingId, bidingId)
                        .eq(OrderHead::getRound, currentRound)
                        .eq(OrderHead::getOrderStatus, OrderStatusEnum.SUBMISSION.getValue()))
                        .stream().collect(Collectors.toMap(OrderHead::getBidVendorId,
                                getVendorIdByOrderHeader,
                                (v1, v2) -> v1.isPresent() ? v1 : v2));
                //?????????????????????bidvendor???????????????????????????id
                for (Long joinVendorId : hadJoinVendorId) {
                    boolean isSubmit = false;
                    for (Map.Entry<Long, Optional<Long>> bidVendorIdAndVendorId : currentBidVendorAndVendorIdMap.entrySet()) {
                        Optional<Long> value = bidVendorIdAndVendorId.getValue();
                        if (value.isPresent() && joinVendorId.equals(value.get())) {
                            isSubmit = true;
                            break;
                        }
                    }
                    //??????????????????????????????????????????????????????bidVendorId
                    if (!isSubmit) {
                        BidVendor bidVendor = iBidVendorService.getOne(Wrappers.lambdaQuery(BidVendor.class)
                                .select(BidVendor::getBidVendorId)
                                .eq(BidVendor::getBidingId, bidingId)
                                .eq(BidVendor::getVendorId, joinVendorId));
                        if (Objects.nonNull(bidVendor)) {
                            for (OrderHead lastHead : orderLastHeadList) {
                                if (lastHead.getBidVendorId().equals(bidVendor.getBidVendorId())&&!Objects.equals(lastHead.getOrderStatus(),OrderStatusEnum.INVALID.getValue())) {
                                    iOrderHeadService.submitOrderForSupplier(joinVendorId, lastHead, bidingId, round);
                                }
                            }
                        }
                    }
                }


            }
            if (notSubmitPriceCount > 0) {
                return LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????");
            }
        }
        return null;
    }

    @Override
    public void openBid(Long bidingId) {
        Biding biding = iBidingService.getById(bidingId);
        Assert.notNull(biding, "?????????ID?????????");
        Assert.notNull(biding.getCurrentRound(), "??????????????????????????????????????????");
        Round round = iRoundService.getOne(new QueryWrapper<Round>(new Round().setBidingId(bidingId).setRound(biding.getCurrentRound())));
        Assert.notNull(round, "?????????????????????");
        Assert.isTrue(!"Y".equals(round.getBusinessOpenBid()), "??????????????????????????????????????????");
        if (BidType.get(biding.getBidingType()) == BidType.TECHNOLOGY_BUSINESS) {
            Assert.isTrue("Y".equals(biding.getTechOpenBid()), "????????????????????????????????????????????????");
            Integer notReviewCount = 0;
            List<TechScoreHead> techScoreHeadList = iTechScoreHeadService.list(new QueryWrapper<TechScoreHead>(new TechScoreHead().setBidingId(bidingId).setScoreStatus(ScoreStatusEnum.FINISHED.getValue())));
            Map<Long, List<TechScoreHead>> techScoreHeadMap = techScoreHeadList.stream().collect(Collectors.groupingBy(TechScoreHead::getBidVendorId));
            List<Group> groupList = iGroupService.list(new QueryWrapper<Group>(new Group().setBidingId(bidingId).setJudgeFlag("Y")));
            Set<Long> groupUserIdSet = groupList.stream().map(Group::getUserId).collect(Collectors.toSet());
            for (Long bidVendorId : techScoreHeadMap.keySet()) {
                Set<Long> techScoreHeadIdSet = techScoreHeadMap.get(bidVendorId).stream().map(TechScoreHead::getCreatedId).collect(Collectors.toSet());
                if (groupUserIdSet.size() != techScoreHeadIdSet.size() || !groupUserIdSet.containsAll(techScoreHeadIdSet)) {
                    notReviewCount++;
                }
            }
            Assert.isTrue(notReviewCount == 0, "?????????????????????????????????????????????");
        }
        biding.setBidingStatus(BiddingProjectStatus.BUSINESS_EVALUATION.getValue());
        round.setBusinessOpenBid("Y");
        round.setBusinessOpenBidTime(new Date());
        iRoundService.updateById(round);
        iBidingService.updateById(biding);

        // ???????????????????????????????????????????????????
        this.defaultSetOrderLinesNextRound(biding);
    }

    protected void defaultSetOrderLinesNextRound(Biding bidding) {
        // ??????.
        iOrderLineService.update(Wrappers.lambdaUpdate(OrderLine.class)
                .set(OrderLine::getWin, "Y")
                .eq(OrderLine::getBidingId, bidding.getBidingId())
                .eq(OrderLine::getRound, bidding.getCurrentRound())
                .ne(OrderLine::getWin, "Q")
                .ne(OrderLine::getWin,"Y")
        );
    }

    @Override
    public List<BusinessItemVO> queryBusinessItemList(Long bidingId) {
        List<BusinessItemVO> result = new ArrayList<BusinessItemVO>();
        List<OrderHead> orderHeadList = iOrderHeadService.list(new QueryWrapper<OrderHead>(new OrderHead().setBidingId(bidingId)).in("ORDER_STATUS", Arrays.asList(OrderStatusEnum.SUBMISSION.getValue(), OrderStatusEnum.INVALID.getValue())).orderByDesc("ROUND").orderByDesc("SUBMIT_TIME"));
        if (orderHeadList.size() == 0) {
            return result;
        }
        List<Long> bidVendorIdList = orderHeadList.stream().map(OrderHead::getBidVendorId).collect(Collectors.toList());
        if (bidVendorIdList.size() == 0) {
            return result;
        }
        List<BidVendor> bidVendorList = iBidVendorService.listByIds(bidVendorIdList);
        Map<Long, BidVendor> vendorMap = bidVendorList.stream().collect(Collectors.toMap(BidVendor::getBidVendorId, Function.identity()));
        for (OrderHead orderHead : orderHeadList) {
            BidVendor vendor = vendorMap.get(orderHead.getBidVendorId());
            if (vendor == null) {
                continue;
            }
            BusinessItemVO vo = new BusinessItemVO();
            vo.setBidDetail(orderHead.getBidOrderNum());
            vo.setOrderHeadId(orderHead.getOrderHeadId());
            vo.setOrderStatus(orderHead.getOrderStatus());
            vo.setRejectReason(orderHead.getRejectReason());
            vo.setRound(orderHead.getRound());
            vo.setSubmitTime(orderHead.getSubmitTime());
            vo.setVendorCode(vendor.getVendorCode());
            vo.setVendorName(vendor.getVendorName());
            vo.setBidVendorId(vendor.getBidVendorId());
            result.add(vo);
        }
        return result;
    }

    @Override
    public void cancelBid(CancelBidParam cancelBidParam) {
        Assert.notNull(cancelBidParam.getOrderHeadId(), "?????????ID????????????");
        OrderHead orderHead = iOrderHeadService.getById(cancelBidParam.getOrderHeadId());
        Assert.notNull(orderHead, "?????????ID?????????");
        if (OrderStatusEnum.get(orderHead.getOrderStatus()) == OrderStatusEnum.INVALID) {
            return;
        }
        Round round = iRoundService.getOne(new QueryWrapper<Round>(new Round().setRoundId(orderHead.getBidRoundId())));
        Assert.notNull(round, "?????????????????????");
        Assert.isTrue(!"Y".equals(round.getBusinessOpenBid()), "??????????????????????????????");
        orderHead.setOrderStatus(OrderStatusEnum.INVALID.getValue());
        orderHead.setRejectReason(cancelBidParam.getRejectReason());
        orderHead.setRejectTime(new Date());
        iOrderHeadService.updateById(orderHead);
        List<OrderLine> orderLineList = iOrderLineService.list(new QueryWrapper<OrderLine>(new OrderLine().setOrderHeadId(cancelBidParam.getOrderHeadId())));
        orderLineList.forEach(l -> l.setOrderStatus(OrderStatusEnum.INVALID.getValue()));
        iOrderLineService.updateBatchById(orderLineList);
    }

    @Override
    public List<OrderDetailVO> queryOrderDetailList(Long orderHeadId) {
        List<OrderDetailVO> result = new ArrayList<OrderDetailVO>();
        OrderHead orderHead = iOrderHeadService.getById(orderHeadId);
        Assert.notNull(orderHead, "??????????????????ID?????????");
        List<OrderLine> orderLineList = iOrderLineService.list(new QueryWrapper<OrderLine>(new OrderLine().setOrderHeadId(orderHeadId)));
        if (orderLineList.size() == 0) {
            return result;
        }
        Round round = iRoundService.getOne(new QueryWrapper<Round>(new Round().setBidingId(orderHead.getBidingId()).setRound(orderHead.getRound())));
        List<BidRequirementLine> requirementLineList = iBidRequirementLineService.list(new QueryWrapper<BidRequirementLine>(new BidRequirementLine().setBidingId(orderHead.getBidingId())));
        Map<Long, BidRequirementLine> requirementLineMap = requirementLineList.stream().collect(Collectors.toMap(BidRequirementLine::getRequirementLineId, Function.identity()));
        Map<Long, OrderLine> lastRoundInfoMap = new HashMap<Long, OrderLine>();
        if (orderHead.getRound() > BidConstant.FIRST_ROUND) {
            // ??????????????????
            List<OrderLine> lastRoundOrderLineList = iOrderLineService.list(new QueryWrapper<OrderLine>(new OrderLine().setBidingId(orderHead.getBidingId()).setBidVendorId(orderHead.getBidVendorId()).setRound(orderHead.getRound() - 1).setWin("Y").setOrderStatus(OrderStatusEnum.SUBMISSION.getValue())));
            lastRoundInfoMap = lastRoundOrderLineList.stream().collect(Collectors.toMap(OrderLine::getRequirementLineId, Function.identity()));
        }
        for (OrderLine orderLine : orderLineList) {
            BidRequirementLine requirementLine = requirementLineMap.get(orderLine.getRequirementLineId());
            if (requirementLine != null) {
                OrderDetailVO vo = new OrderDetailVO().setOrgName(requirementLine.getOrgName()).setItemGroup(requirementLine.getItemGroup()).setTargetNum(requirementLine.getTargetNum()).setTargetDesc(requirementLine.getTargetDesc());
                if ("Y".equals(round.getBusinessOpenBid())) {
                    vo.setPrice(orderLine.getPrice());// ????????????????????????????????????
                }
                vo.setPurchaseCategory(requirementLine.getCategoryName()).setQuantity(requirementLine.getQuantity()).setAmount(requirementLine.getAmount()).setComments(requirementLine.getComments()).setTargetPrice(requirementLine.getTargetPrice());
                OrderLine lastRoundOrderLine = lastRoundInfoMap.get(orderLine.getRequirementLineId());
                if (lastRoundOrderLine != null) {
                    vo.setLastRoundPrice(lastRoundOrderLine.getPrice());
                    vo.setLastRoundMinPrice(lastRoundOrderLine.getCurrentRoundMinPrice());
                    vo.setLastRoundRank(lastRoundOrderLine.getRank());
                }
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public BidOrderHeadVO queryNewOrderDetailList(Long orderHeadId) {
        OrderHead byId = iOrderHeadService.getById(orderHeadId);
        int count = iRoundService.count(Wrappers.lambdaQuery(Round.class)
                .eq(Round::getRoundId, byId.getBidRoundId())
                .eq(Round::getBusinessOpenBid, YesOrNo.YES.getValue())
        );
        List<BidOrderLineVO> bidOrderLineVOs = iOrderHeadService.getOrderLineVOS(byId.getBidingId(), orderHeadId, byId.getBidVendorId());
        for (BidOrderLineVO vo : bidOrderLineVOs) {
            //?????????????????????????????????????????????,????????????????????????
            if(count==0){
                vo.setPrice(null);
            }
          /*  OrderLine line = iOrderLineService.getOne(Wrappers.lambdaQuery(OrderLine.class)
                    .select(OrderLine::getPrice)
                    .eq(OrderLine::getOrderLineId, vo.getOrderLineId())
                    .eq(Objects.nonNull(vo.getRound()) && vo.getRound() > 0, OrderLine::getRound, vo.getRound()));
            if(Objects.nonNull(line)){
                vo.setLastPrice(line.getPrice());
            }
            BigDecimal currentPrice = vo.getPrice();
            if (Objects.nonNull(currentPrice) && Objects.nonNull(vo.getLastPrice())) {
                if(!Objects.equals(vo.getLastPrice(),BigDecimal.ZERO)){
                    vo.setCompareRate(currentPrice.divide(vo.getLastPrice(), 8, RoundingMode.HALF_DOWN).multiply(BigDecimal.valueOf(100)));
                }
            }*/

        }
        BidOrderHeadVO vo = BeanCopyUtil.copyProperties(byId, BidOrderHeadVO::new);
        vo.setOrderLines(bidOrderLineVOs);
        List<OrderHeadFile> files = orderHeadFileMapper.selectList(Wrappers.lambdaQuery(OrderHeadFile.class)
                .eq(OrderHeadFile::getOrderHeadId, orderHeadId));
        vo.setOrderLines(bidOrderLineVOs);
        vo.setOrderHeadFiles(files);
        return vo;
    }

    @Override
    public List<OrderHeadFile> getOrderHeadFileByOrderHeadId(Long orderHeadId) {
        List<OrderHeadFile> files = orderHeadFileMapper.selectList(Wrappers.lambdaQuery(OrderHeadFile.class)
                .eq(OrderHeadFile::getOrderHeadId, orderHeadId));
        if (CollectionUtils.isEmpty(files)) {
            return files;
        }
        return files.stream().filter(e -> e.getVendorReferenceFileType().equals(BidingFileTypeEnum.COMMERCIA_BID.getCode())).collect(Collectors.toList());
    }

    @Override
    public List<OrderHeadFile> getOrderHeadFileByVendorIdAndBidingId(Long biddingId, Long vendorId) {
        Biding biding = iBidingService.getById(biddingId);
        Integer currentRound = biding.getCurrentRound();
        Long bidVendorId = iBidVendorService.getOne(Wrappers.lambdaQuery(BidVendor.class)
                .select(BidVendor::getBidVendorId)
                .eq(BidVendor::getBidingId, biddingId).eq(BidVendor::getVendorId, vendorId)).getBidVendorId();
        OrderHead head = iOrderHeadService.getOne(Wrappers.lambdaQuery(OrderHead.class)
                .eq(OrderHead::getBidingId, biddingId)
                .eq(OrderHead::getRound, currentRound)
                .eq(OrderHead::getBidVendorId,bidVendorId)
        );
        if (Objects.isNull(head)) {
            return Collections.emptyList();
        }
        List<OrderHeadFile> files = orderHeadFileMapper.selectList(Wrappers.lambdaQuery(OrderHeadFile.class)
                .eq(OrderHeadFile::getOrderHeadId, head.getOrderHeadId()));
        if (CollectionUtils.isEmpty(files)) {
            return files;
        }
        return files.stream().filter(e -> e.getVendorReferenceFileType().equals(BidingFileTypeEnum.TECHNICAL_BID.getCode())).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> generateBidReport(Long bidingId) throws Exception {
        Assert.notNull(bidingId, "?????????ID????????????");
        Biding biding = iBidingService.getById(bidingId);
        Assert.notNull(biding, "?????????ID?????????");
        Assert.notNull(biding.getCurrentRound(), "???????????????");
        List<BidRequirementLine> requirementLineList = iBidRequirementLineService.list(new QueryWrapper<BidRequirementLine>(new BidRequirementLine().setBidingId(bidingId)));
        Assert.notEmpty(requirementLineList, "??????????????????");
        List<OrderLine> orderLineList = iOrderLineService.list(new QueryWrapper<OrderLine>(new OrderLine().setBidingId(bidingId).setOrderStatus(OrderStatusEnum.SUBMISSION.getValue())));
        Assert.notEmpty(orderLineList, "???????????????");
        List<BidVendor> vendorList = iBidVendorService.list(new QueryWrapper<BidVendor>(new BidVendor().setBidingId(bidingId)));
        Assert.notEmpty(vendorList, "???????????????");
        Map<Long, BidVendor> vendorMap = vendorList.stream().collect(Collectors.toMap(BidVendor::getBidVendorId, Function.identity()));
        String fileName = UUID.randomUUID().toString() + ".xls";
        ExcelWriter excelWriter = EasyExcel.write(fileName).build();
        WriteSheet sheet1 = EasyExcel.writerSheet(0, LocaleHandler.getLocaleMsg("???????????????????????????")).build();
        sheet1.setHead(head1(orderLineList, vendorMap));
        excelWriter.write(data1(biding, requirementLineList, orderLineList, vendorMap), sheet1);
        WriteSheet sheet2 = EasyExcel.writerSheet(1, LocaleHandler.getLocaleMsg("????????????")).build();
        sheet2.setHead(head2(orderLineList, vendorMap));
        excelWriter.write(data2(biding, requirementLineList, orderLineList, vendorMap), sheet2);
        excelWriter.finish();
        File file = new File(fileName);
        byte[] buffer = FileUtils.readFileToByteArray(file);
        file.delete();
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("buffer", buffer);
        result.put("fileName", biding.getBidingNum() + ".xls");
        return result;
    }


    private List<List<String>> head1(List<OrderLine> orderLineList, Map<Long, BidVendor> vendorMap) {
        List<List<String>> head = new ArrayList<List<String>>();
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("??????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("????????????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("????????????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("????????????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("?????????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("????????????(???????????????)")));
        // ??????????????????????????????ID??????
        Map<Integer, Map<Long, OrderLine>> orderLineMap = orderLineList.stream().collect(Collectors.groupingBy(OrderLine::getRound, Collectors.toMap(OrderLine::getBidVendorId, Function.identity(), (k1, k2) -> k1)));
        for (Integer round : orderLineMap.keySet()) {
            String msg = LocaleHandler.getLocaleMsg("???N?????????", round.toString());
            for (Long bidVendorId : orderLineMap.get(round).keySet()) {
                if (vendorMap.containsKey(bidVendorId)) {
                    head.add(Arrays.asList(msg, vendorMap.get(bidVendorId).getVendorName()));
                }
            }
            head.add(Arrays.asList(msg, LocaleHandler.getLocaleMsg("???N????????????", round.toString())));
//			head.add(Arrays.asList(msg, LocaleHandler.getLocaleMsg("?????????")));
        }
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("??????????????????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("???????????????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("??????")));
        for (Integer i = 1; i <= 10; i++) {
            head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("??????", i.toString())));
        }
        return head;
    }

    private List<List<String>> head2(List<OrderLine> orderLineList, Map<Long, BidVendor> vendorMap) {
        List<List<String>> head = new ArrayList<List<String>>();
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("??????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("????????????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("????????????")));
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("????????????")));
        // ??????????????????????????????ID??????
        Map<Integer, Map<Long, OrderLine>> orderLineMap = orderLineList.stream().collect(Collectors.groupingBy(OrderLine::getRound, Collectors.toMap(OrderLine::getBidVendorId, Function.identity(), (k1, k2) -> k1)));
        for (Integer round : orderLineMap.keySet()) {
            String msg = LocaleHandler.getLocaleMsg("???N?????????", round.toString());
            for (Long bidVendorId : orderLineMap.get(round).keySet()) {
                if (vendorMap.containsKey(bidVendorId)) {
                    head.add(Arrays.asList(msg, vendorMap.get(bidVendorId).getVendorName()));
                }
            }
            head.add(Arrays.asList(msg, LocaleHandler.getLocaleMsg("???N????????????", round.toString())));
//			head.add(Arrays.asList(msg, LocaleHandler.getLocaleMsg("?????????")));
        }
        head.add(Arrays.asList("", LocaleHandler.getLocaleMsg("??????????????????")));
        return head;
    }

    private List<List<String>> data1(Biding biding, List<BidRequirementLine> requirementLineList, List<OrderLine> orderLineList, Map<Long, BidVendor> vendorMap) {
        List<List<String>> data = new ArrayList<List<String>>();
        Map<Integer, List<OrderLine>> roundOrderLineMap = orderLineList.stream().collect(Collectors.groupingBy(OrderLine::getRound));
        Map<Long, BidRequirementLine> requirementLineMap = requirementLineList.stream().collect(Collectors.toMap(BidRequirementLine::getRequirementLineId, Function.identity()));
        Map<Integer, Map<Long, List<OrderLine>>> orderLineMap = orderLineList.stream().collect(Collectors.groupingBy(OrderLine::getRound, Collectors.groupingBy(OrderLine::getRequirementLineId)));
        Map<Long, List<OrderLine>> winBidMap = orderLineList.stream().filter(l -> SelectionStatusEnum.get(l.getSelectionStatus()) == SelectionStatusEnum.WIN).collect(Collectors.groupingBy(OrderLine::getRequirementLineId));
        Map<Long, List<OrderLine>> firstRoundOrderLineMap = orderLineMap.get(BidConstant.FIRST_ROUND);
        Assert.notNull(firstRoundOrderLineMap, "?????????????????????????????????");
        int index = 1;
        // ?????????????????????????????????????????????????????????
        for (Long requirementLineId : firstRoundOrderLineMap.keySet()) {
            BidRequirementLine line = requirementLineMap.get(requirementLineId);
            if (line == null) {
                continue;
            }
            List<String> dataItem = new ArrayList<String>();
            dataItem.add(String.valueOf(index++));
            dataItem.add(line.getOrgName());
            dataItem.add(line.getTargetNum());
            dataItem.add(line.getTargetDesc());
            dataItem.add(line.getQuantity() + "");
            dataItem.add(line.getAmount().setScale(2, RoundingMode.HALF_DOWN).toPlainString());
            // ????????????
            for (int round = BidConstant.FIRST_ROUND; round <= biding.getCurrentRound(); round++) {
                if (!orderLineMap.containsKey(round)) {
                    continue;
                }
                List<OrderLine> list = orderLineMap.get(round).get(requirementLineId);
                if (list == null || list.size() == 0) {
                    continue;
                }
                // ????????????ID??????
                Set<Long> bidVendorIdSet = roundOrderLineMap.get(round).stream().map(OrderLine::getBidVendorId).collect(Collectors.toSet());
                Map<Long, OrderLine> priceMap = list.stream().collect(Collectors.toMap(OrderLine::getBidVendorId, Function.identity(), (k1, k2) -> k1));
                for (Long bidVendorId : bidVendorIdSet) {
                    dataItem.add(priceMap.get(bidVendorId) == null ? null : priceMap.get(bidVendorId).getPrice().setScale(biding.getDecimalAccuracy(), RoundingMode.HALF_DOWN).toPlainString());
                }
                dataItem.add(list.get(0).getCurrentRoundMinPrice() == null ? null : list.get(0).getCurrentRoundMinPrice().setScale(biding.getDecimalAccuracy(), RoundingMode.HALF_DOWN).toPlainString());
                dataItem.add(line.getTargetPrice() == null ? null : line.getTargetPrice().setScale(biding.getDecimalAccuracy(), RoundingMode.HALF_DOWN).toPlainString());
            }
            String winBidPrice = "";
            String winBidVendorName = "";
            List<OrderLine> winBidOrderLineList = winBidMap.get(requirementLineId);
            if (winBidOrderLineList != null) {
                for (OrderLine o : winBidOrderLineList) {
                    if (vendorMap.get(o.getBidVendorId()) == null) {
                        continue;
                    }
                    winBidPrice = winBidPrice + o.getPrice().setScale(biding.getDecimalAccuracy(), RoundingMode.HALF_DOWN).toPlainString() + ",";
                    winBidVendorName = winBidVendorName + vendorMap.get(o.getBidVendorId()).getVendorName() + ",";
                }
            }
            dataItem.add(StringUtils.isNotBlank(winBidPrice) ? winBidPrice.substring(0, winBidPrice.length() - 1) : "");
            dataItem.add(StringUtils.isNotBlank(winBidVendorName) ? winBidVendorName.substring(0, winBidVendorName.length() - 1) : "");
            dataItem.add(line.getComments());
            Map<Long, List<OrderLine>> lastRoundMap = orderLineMap.get(biding.getCurrentRound());
            if (lastRoundMap != null && lastRoundMap.get(requirementLineId) != null) {
                for (OrderLine orderLine : lastRoundMap.get(requirementLineId).stream().sorted((r1, r2) -> r1.getBidVendorId().compareTo(r2.getBidVendorId())).collect(Collectors.toList())) {
                    if (SelectionStatusEnum.get(orderLine.getSelectionStatus()) == SelectionStatusEnum.FOLLOW) {
                        dataItem.add(vendorMap.get(orderLine.getBidVendorId()).getVendorName());
                    }
                }
            }
            data.add(dataItem);
        }
        return data;
    }

    private List<List<String>> data2(Biding biding, List<BidRequirementLine> requirementLineList, List<OrderLine> orderLineList, Map<Long, BidVendor> vendorMap) {
        List<List<String>> data = new ArrayList<List<String>>();
        Map<Integer, List<OrderLine>> roundOrderLineMap = orderLineList.stream().collect(Collectors.groupingBy(OrderLine::getRound));
        Map<Long, BidRequirementLine> requirementLineMap = requirementLineList.stream().collect(Collectors.toMap(BidRequirementLine::getRequirementLineId, Function.identity()));
        Map<Integer, Map<Long, List<OrderLine>>> orderLineMap = orderLineList.stream().collect(Collectors.groupingBy(OrderLine::getRound, Collectors.groupingBy(OrderLine::getRequirementLineId)));
        Map<Long, List<OrderLine>> winBidMap = orderLineList.stream().filter(l1 -> SelectionStatusEnum.get(l1.getSelectionStatus()) == SelectionStatusEnum.WIN).collect(Collectors.groupingBy(OrderLine::getRequirementLineId));
        Map<Long, List<OrderLine>> firstRoundOrderLineMap = orderLineMap.get(BidConstant.FIRST_ROUND);
        Assert.notNull(firstRoundOrderLineMap, "?????????????????????????????????");
        int index = 1;
        // ?????????????????????????????????????????????????????????
        for (Long requirementLineId : firstRoundOrderLineMap.keySet()) {
            BidRequirementLine line = requirementLineMap.get(requirementLineId);
            if (line == null) {
                continue;
            }
            List<String> dataItem = new ArrayList<String>();
            dataItem.add(String.valueOf(index++));
            dataItem.add(line.getOrgName());
            dataItem.add(line.getTargetNum());
            dataItem.add(line.getTargetDesc());
            for (Integer round = BidConstant.FIRST_ROUND; round <= biding.getCurrentRound(); round++) {
                if (!orderLineMap.containsKey(round)) {
                    continue;
                }
                List<OrderLine> list = orderLineMap.get(round).get(requirementLineId);
                if (list == null || list.size() == 0) {
                    continue;
                }
                // ????????????ID??????
                Set<Long> bidVendorSet = roundOrderLineMap.get(round).stream().map(OrderLine::getBidVendorId).collect(Collectors.toSet());
                Map<Long, OrderLine> priceMap = list.stream().collect(Collectors.toMap(OrderLine::getBidVendorId, Function.identity(), (k1, k2) -> k1));
                for (Long bidVendorId : bidVendorSet) {
                    dataItem.add(priceMap.get(bidVendorId) == null ? null : priceMap.get(bidVendorId).getPrice().setScale(biding.getDecimalAccuracy(), RoundingMode.HALF_DOWN).toPlainString());
                }
                dataItem.add(list.get(0).getCurrentRoundMinPrice() == null ? null : list.get(0).getCurrentRoundMinPrice().setScale(biding.getDecimalAccuracy(), RoundingMode.HALF_DOWN).toPlainString());
                dataItem.add(line.getTargetPrice() == null ? null : line.getTargetPrice().setScale(biding.getDecimalAccuracy(), RoundingMode.HALF_DOWN).toPlainString());
            }
            String winBidPrice = "";
            List<OrderLine> winBidOrderLineList = winBidMap.get(requirementLineId);
            if (winBidOrderLineList != null) {
                for (OrderLine o : winBidOrderLineList) {
                    if (vendorMap.get(o.getBidVendorId()) == null) {
                        continue;
                    }
                    winBidPrice = winBidPrice + o.getPrice().setScale(biding.getDecimalAccuracy(), RoundingMode.HALF_DOWN).toPlainString() + ",";
                }
            }
            dataItem.add(StringUtils.isNotBlank(winBidPrice) ? winBidPrice.substring(0, winBidPrice.length() - 1) : "");
            data.add(dataItem);
        }
        return data;
    }

}
