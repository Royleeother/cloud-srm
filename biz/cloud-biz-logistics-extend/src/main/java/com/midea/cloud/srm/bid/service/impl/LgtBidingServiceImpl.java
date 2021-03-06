package com.midea.cloud.srm.bid.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.ProcessNodeName;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.bid.projectmanagement.bidinitiating.BidFileType;
import com.midea.cloud.common.enums.bid.projectmanagement.bidinitiating.BidType;
import com.midea.cloud.common.enums.bid.projectmanagement.evaluation.SelectionStatusEnum;
import com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingApprovalStatus;
import com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingProjectStatus;
import com.midea.cloud.common.enums.logistics.*;
import com.midea.cloud.common.enums.logistics.pr.requirement.LogisticsApplyProcessStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.bid.mapper.LgtBidingMapper;
import com.midea.cloud.srm.bid.service.*;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.bid.BidClient;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.logistics.template.service.ILogisticsTemplateHeadService;
import com.midea.cloud.srm.logistics.template.service.ILogisticsTemplateLineService;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidcontrol.vo.BidControlTopInfoVO;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.enums.BidingFileTypeEnum;
import com.midea.cloud.srm.model.base.purchase.entity.LatestGidailyRate;
import com.midea.cloud.srm.model.bid.purchaser.bidprocessconfig.entity.BidProcessConfig;
import com.midea.cloud.srm.model.logistics.bid.dto.*;
import com.midea.cloud.srm.model.logistics.bid.entity.*;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidcontrol.vo.StartOrExtendBidingVO;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.enums.SourceFrom;
import com.midea.cloud.srm.model.logistics.bid.vo.*;
import com.midea.cloud.srm.model.logistics.po.order.entity.OrderHead;
import com.midea.cloud.srm.model.logistics.pr.requirement.dto.LogisticsPurchaseRequirementDTO;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementFile;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementHead;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementLine;
import com.midea.cloud.srm.model.logistics.template.entity.LogisticsTemplateHead;
import com.midea.cloud.srm.model.logistics.template.entity.LogisticsTemplateLine;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.model.supplier.info.dto.VendorDto;
import com.midea.cloud.srm.po.logisticsOrder.service.IOrderHeadService;
import com.midea.cloud.srm.pr.logisticsRequirement.service.IRequirementHeadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
*  <pre>
 *  ??????????????????????????? ???????????????
 * </pre>
*
* @author wangpr@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2021-01-06 09:54:37
 *  ????????????:
 * </pre>
*/
@Slf4j
@Service
public class LgtBidingServiceImpl extends ServiceImpl<LgtBidingMapper, LgtBiding> implements ILgtBidingService {
    @Resource
    private ILgtFileService iLgtFileService;
    @Resource
    private ILgtBidRequirementLineService iLgtBidRequirementLineService;
    @Resource
    private ILgtBidResultSumService iLgtBidResultSumService;
    @Resource
    private ILgtBidShipPeriodService iLgtBidShipPeriodService;
    @Resource
    private ILgtBidTemplateService iLgtBidTemplateService;
    @Resource
    private ILgtFileConfigService iLgtFileConfigService;
    @Resource
    private ILgtGroupService iLgtGroupService;
    @Resource
    private ILgtPayPlanService iLgtPayPlanService;
    @Resource
    private ILgtQuoteAuthorizeService iLgtQuoteAuthorizeService;
    @Resource
    private ILgtVendorFileService iLgtVendorFileService;
    @Resource
    private ILgtProcessNodeService iLgtProcessNodeService;
    @Resource
    private BidClient bidClient;
    @Resource
    private ILogisticsTemplateLineService iLogisticsTemplateLineService;
    @Resource
    private ILogisticsTemplateHeadService iLogisticsTemplateHeadService;
    @Resource
    private BaseClient baseClient;
    @Resource
    private SupplierClient supplierClient;
    @Resource
    private ILgtVendorService iLgtVendorService;
    @Resource
    private ILgtVendorQuotedHeadService iLgtVendorQuotedHeadService;
    @Resource
    private ILgtVendorQuotedLineService iLgtVendorQuotedLineService;
    @Resource
    private ILgtRoundService iLgtRoundService;
    @Resource
    private ILgtVendorQuotedSumService iLgtVendorQuotedSumService;
    @Resource
    private IRequirementHeadService requirementHeadService;
    @Resource
    private ILgtBidingVendorService iLgtBidingVendorService;
    @Resource
    private IOrderHeadService iOrderHeadService;
    @Resource
    private PmClient pmClient;
    @Resource
    private IRequirementHeadService iRequirementHeadService;
    @Resource
    private RbacClient rbacClient;
    public static Set<String> canSeeResultSet=new HashSet<>();
    static {
        canSeeResultSet.add(BiddingProjectStatus.BUSINESS_EVALUATION.getValue());
        canSeeResultSet.add(BiddingProjectStatus.PUBLICITY_OF_RESULT.getValue());
        canSeeResultSet.add(BiddingProjectStatus.PRICED.getValue());
        canSeeResultSet.add(BiddingProjectStatus.PROJECT_END.getValue());
    }

    @Override
    @Transactional
    public void selectionQuotedLineSaveNew(LgtVendorQuotedLineDto lgtVendorQuotedLines) {
        Assert.notNull(lgtVendorQuotedLines,"??????????????????!");
        List<LgtVendorQuotedLine> vendorQuotedLines = lgtVendorQuotedLines.getLgtVendorQuotedLines();
        Assert.isTrue(CollectionUtils.isNotEmpty(vendorQuotedLines),"????????????????????????!");
        // ????????????????????????
        String combinationName = lgtVendorQuotedLines.getCombinationName();
        LgtBiding lgtBiding = this.getById(vendorQuotedLines.get(0).getBidingId());
        // ????????????????????????
        LgtRound lgtRound = iLgtRoundService.getOne(new QueryWrapper<>(new LgtRound().setBidingId(lgtBiding.getBidingId()).setRound(lgtBiding.getCurrentRound())));
        if(!ObjectUtils.isEmpty(combinationName)){
            // ?????????????????????????????????
            List<LgtVendorQuotedLine> quotedLines1 = vendorQuotedLines.stream().filter(lgtVendorQuotedLine -> ObjectUtils.isEmpty(lgtVendorQuotedLine.getTradeTerm())).collect(Collectors.toList());
            quotedLines1.forEach(lgtVendorQuotedLine -> lgtVendorQuotedLine.setTradeTerm(""));
            iLgtVendorQuotedLineService.updateBatchById(quotedLines1);

            // ????????????????????????????????????
            List<LgtVendorQuotedLine> quotedLines = vendorQuotedLines.stream().filter(lgtVendorQuotedLine -> !ObjectUtils.isEmpty(lgtVendorQuotedLine.getTradeTerm())).collect(Collectors.toList());
            // ???????????????
            selectionQuotedLineSave(quotedLines);
            // ???????????????
            iLgtRoundService.updateById(lgtRound.setCombinationName(combinationName));
        }else {
            // ???????????????
            selectionQuotedLineSave(vendorQuotedLines);
            // ???????????????
            iLgtRoundService.updateById(lgtRound.setCombinationName(""));
        }
    }

    @Override
    public void techOpenBiding(Long bidingId) {
        LgtBiding byId = getById(bidingId);
        if(Objects.isNull(byId)){
            throw new BaseException("????????????????????????");
        }
        String bidingStatus = byId.getBidingStatus();
        //?????????????????????????????????????????????????????????
        if(Objects.equals(bidingStatus,BiddingProjectStatus.TENDER_ENDING.getValue())){
            updateById(new LgtBiding().setBidingId(bidingId)
            .setBidingStatus(BiddingProjectStatus.TECHNICAL_EVALUATION.getValue())
            );
        }
    }

    public void techOpenBusiness(Long bidingId){
        LgtBiding lgtBiding = getById(bidingId);
        // ???????????????????????????
        if(BiddingProjectStatus.TENDER_ENDING.getValue().equals(lgtBiding.getBidingStatus())
        ||BiddingProjectStatus.TECHNICAL_EVALUATION.getValue().equals(lgtBiding.getBidingStatus())
        ){
            // ????????????->???????????? ????????????
            updateBusinessEvaluationStatus(lgtBiding);
        }
    }

    @Override
    public List<LgtBidTemplate> getLgtBidTemplate(Long bidingId) {
        return iLgtBidTemplateService.list(new QueryWrapper<>(new LgtBidTemplate().setBidingId(bidingId)));
    }

    /**
     * ????????????????????????
     * todo ?????????????????????????????????
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> requirementToBidings(List<Long> ids) {
        List<Long> result = new LinkedList<>();
        for(Long id : ids){
            result.add(requirementToBiding(id));
        }
        return result;
    }

    /**
     *
     * ??????????????????????????????????????????????????????????????????????????????
     * @param bidingId
     * @return
     */
    @Override
    public List<LgtBidVendor> getLgtBidVendorByBidingIdAndStatus(Long bidingId) {
        if(Objects.isNull(bidingId)){
            return Collections.EMPTY_LIST;
        }
        LgtBiding biding = this.getById(bidingId);
        if(Objects.isNull(biding)){
            return Collections.EMPTY_LIST;
        }
        if(!BiddingApprovalStatus.APPROVED.getValue().equals(biding.getAuditStatus())){
            return Collections.EMPTY_LIST;
        }
        //?????????????????????
        List<LgtBidVendor> bidVendors = iLgtVendorService.list(Wrappers.lambdaQuery(LgtBidVendor.class).eq(LgtBidVendor::getBidingId, bidingId));
        return bidVendors;
    }

    @Override
    public void quotedSumExport(Long bidingId, Integer round, Long vendorId, HttpServletResponse response) throws IOException {
        /**
         * ??????????????????????????????????????????????????????
         */
        LgtBiding lgtBiding = this.getById(bidingId);
        // ????????????????????????
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                eq(LgtVendorQuotedLine::getBidingId, bidingId).
                eq(LgtVendorQuotedLine::getRound, round).
                eq(!ObjectUtils.isEmpty(vendorId), LgtVendorQuotedLine::getVendorId, vendorId));
        if (CollectionUtils.isNotEmpty(lgtVendorQuotedLines)) {
            /**
             * ????????????:
             * ????????????: Y/N
             * ??????:
             * ??????: CHARGE_NAME
             * ????????????: CHARGE_LEVEL
             * ????????????: SUB_LEVEL
             * ????????????: TRADE_TERM
             * ??????/??????: FCL/LCL
             * LEG: LEG
             * ???????????????: EXP/IMP
             */
            // ????????????
            Map<String, String> chargeNameMap = EasyExcelUtil.getDicCodeName("CHARGE_NAME", baseClient);
            // ????????????
            Map<String, String> chargeLevelMap = EasyExcelUtil.getDicCodeName("CHARGE_LEVEL", baseClient);
            // ????????????
            Map<String, String> subLevelMap = EasyExcelUtil.getDicCodeName("SUB_LEVEL", baseClient);
            // ????????????
            Map<String, String> tradeTermMap = EasyExcelUtil.getDicCodeName("TRADE_TERM", baseClient);
            // ??????/??????
            Map<String, String> fclLclMap = EasyExcelUtil.getDicCodeName("FCL /LCL", baseClient);
            // LEG
            Map<String, String> legMap = EasyExcelUtil.getDicCodeName("LEG", baseClient);
            // ??????
            Map<String, String> currencyCodeName = EasyExcelUtil.getCurrencyCodeName(baseClient);
            AtomicInteger no = new AtomicInteger(1);
            List<LgtInsideQuotedSumExport> lgtInsideQuotedSumExports = new ArrayList<>();
            List<LgtNoInsideQuotedSumExport> lgtNoInsideQuotedSumExports = new ArrayList<>();
            lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                Optional.ofNullable(lgtVendorQuotedLine.getExpenseItem()).ifPresent(expenseItem -> lgtVendorQuotedLine.setExpenseItem(chargeNameMap.get(expenseItem)));
                Optional.ofNullable(lgtVendorQuotedLine.getChargeMethod()).ifPresent(chargeMethod -> lgtVendorQuotedLine.setChargeMethod(chargeLevelMap.get(chargeMethod)));
                Optional.ofNullable(lgtVendorQuotedLine.getChargeUnit()).ifPresent(chargeUnit -> lgtVendorQuotedLine.setChargeUnit(subLevelMap.get(chargeUnit)));
                Optional.ofNullable(lgtVendorQuotedLine.getTradeTerm()).ifPresent(tradeTerm -> lgtVendorQuotedLine.setTradeTerm(tradeTermMap.get(tradeTerm)));
                Optional.ofNullable(lgtVendorQuotedLine.getWholeArk()).ifPresent(wholeArk -> lgtVendorQuotedLine.setWholeArk(fclLclMap.get(wholeArk)));
                Optional.ofNullable(lgtVendorQuotedLine.getLeg()).ifPresent(leg -> lgtVendorQuotedLine.setLeg(legMap.get(leg)));
                Optional.ofNullable(lgtVendorQuotedLine.getCurrency()).ifPresent(currency -> lgtVendorQuotedLine.setCurrency(currencyCodeName.get(currency)));
                Optional.ofNullable(lgtVendorQuotedLine.getIfBack()).ifPresent(ifBack -> {
                    if(YesOrNo.YES.getValue().equals(ifBack)){
                        lgtVendorQuotedLine.setIfBack(YesOrNo.YES.getName());
                    }else {
                        lgtVendorQuotedLine.setIfBack(YesOrNo.NO.getName());
                    }
                });


                if(CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
                    LgtInsideQuotedSumExport lgtInsideQuotedSumExport = new LgtInsideQuotedSumExport();
                    BeanCopyUtil.copyProperties(lgtInsideQuotedSumExport,lgtVendorQuotedLine);
                    lgtInsideQuotedSumExport.setNo(no.getAndAdd(1));
                    lgtInsideQuotedSumExports.add(lgtInsideQuotedSumExport);
                }else {
                    LgtNoInsideQuotedSumExport lgtInsideQuotedSumExport = new LgtNoInsideQuotedSumExport();
                    BeanCopyUtil.copyProperties(lgtInsideQuotedSumExport,lgtVendorQuotedLine);
                    lgtInsideQuotedSumExport.setNo(no.getAndAdd(1));
                    lgtNoInsideQuotedSumExports.add(lgtInsideQuotedSumExport);
                }
            });
            OutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????");
            if(CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
                EasyExcelUtil.writeExcelWithModel(outputStream,lgtInsideQuotedSumExports,LgtInsideQuotedSumExport.class);
            }else {
                EasyExcelUtil.writeExcelWithModel(outputStream,lgtNoInsideQuotedSumExports,LgtNoInsideQuotedSumExport.class);
            }
        }
    }

    @Override
    public void saveTechnoSelection(Long bidingId, String technoSelection) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();

        Assert.notNull(technoSelection,"??????????????????????????????!");
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"????????????????????????!");
        List<LgtGroup> lgtGroups = iLgtGroupService.list(new QueryWrapper<>(new LgtGroup().setBidingId(bidingId)));
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtGroups),"???????????????????????????,????????????!");
        List<String> userNames = lgtGroups.stream().map(LgtGroup::getUserName).collect(Collectors.toList());
        Assert.isTrue(userNames.contains(loginAppUser.getUsername()),"???????????????????????????,????????????!");
        LgtRound lgtRound = iLgtRoundService.getOne(Wrappers.lambdaQuery(LgtRound.class).
                eq(LgtRound::getBidingId, bidingId).
                eq(LgtRound::getRound, lgtBiding.getCurrentRound()).last(" LIMIT 1"));
        Assert.notNull(lgtRound,"???????????????????????????!");
        lgtRound.setTechnoSelection(technoSelection);
        iLgtRoundService.updateById(lgtRound);
    }

    @Override
    public LgtApproveInfoDto queryResultApproveInfo(Long bidingId,Long round) {
        LgtApproveInfoDto lgtApproveInfoDto = new LgtApproveInfoDto();
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"????????????????????????!");
        round = null == round ? lgtBiding.getCurrentRound() : round;
        lgtApproveInfoDto.setLgtBiding(lgtBiding);
        // ???????????????????????????,??????????????????????????????
        if(!BiddingApprovalStatus.DRAFT.getValue().equals(lgtBiding.getEndAuditStatus())){
            List<LgtVendorQuotedSum> lgtVendorQuotedSums = iLgtVendorQuotedSumService.list(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                    eq(LgtVendorQuotedSum::getBidingId, bidingId).
                    eq(LgtVendorQuotedSum::getRound, round));
            lgtApproveInfoDto.setLgtVendorQuotedSums(lgtVendorQuotedSums);

            List<LgtVendorQuotedHead> vendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                    eq(LgtVendorQuotedHead::getBidingId, bidingId).
                    eq(LgtVendorQuotedHead::getRound,round).
                    eq(LgtVendorQuotedHead::getStatus, BiddingOrderStates.SUBMISSION.getValue()));

            if (CollectionUtils.isNotEmpty(vendorQuotedHeads)) {
                List<Long> quotedHeadIds = vendorQuotedHeads.stream().map(LgtVendorQuotedHead::getQuotedHeadId).collect(Collectors.toList());

                // ??????????????????
                List<LgtVendorQuotedLine> lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                        in(LgtVendorQuotedLine::getQuotedHeadId, quotedHeadIds));
                lgtApproveInfoDto.setLgtVendorQuotedLines(lgtVendorQuotedLines);
                // ????????????
                List<LgtBidShipPeriod> lgtBidShipPeriods = iLgtBidShipPeriodService.list(Wrappers.lambdaQuery(LgtBidShipPeriod.class).
                        in(LgtBidShipPeriod::getQuotedHeadId, quotedHeadIds));
                lgtApproveInfoDto.setLgtBidShipPeriods(lgtBidShipPeriods);
            }
        }
        return lgtApproveInfoDto;
    }

    @Override
    @Transactional
    public void generateResultApprove(Long bidingId,String summaryDescription) {
        Assert.notNull(summaryDescription,"????????????????????????!");
        /**
         * 1. ????????????
         * 2. ??????????????????????????????
         */
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"????????????????????????!");
        Assert.isTrue(!checkUserIfTechnology(bidingId),"????????????????????????!!!");
        Assert.isTrue(!YesOrNo.YES.getValue().equals(lgtBiding.getGeneratePurchaseApproval()),"?????????????????????????????????!");
        Assert.isTrue(BiddingProjectStatus.PUBLICITY_OF_RESULT.getValue().equals(lgtBiding.getBidingStatus()),"???????????????,???????????????????????????!");
        List<LgtVendorQuotedSum> lgtVendorQuotedSums = iLgtVendorQuotedSumService.list(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                eq(LgtVendorQuotedSum::getBidingId, bidingId).
                eq(LgtVendorQuotedSum::getRound, lgtBiding.getCurrentRound()).
                and(wrapper -> wrapper.
                        eq(LgtVendorQuotedSum::getBidResult, SelectionStatusEnum.FIRST_WIN).or().
                        eq(LgtVendorQuotedSum::getBidResult, SelectionStatusEnum.SECOND_WIN).or().
                        eq(LgtVendorQuotedSum::getBidResult, SelectionStatusEnum.WIN)));
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtVendorQuotedSums),"????????????????????????????????????,???????????????????????????!");
        // ?????????????????????????????????
        lgtBiding.setEndAuditStatus(BiddingApprovalStatus.SUBMITTED.getValue());
        lgtBiding.setSummaryDescription(summaryDescription);

        // ?????????
        try {
            // ????????????
            updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.projectApproval.getValue())
                    .setBidingId(lgtBiding.getBidingId()).setDataFlag(YesOrNo.YES.getValue()));

            List<OrderHead> orderHeads = iOrderHeadService.bidingToOrders(bidingId);
            // ????????????????????????
            lgtBiding.setEndAuditStatus(BiddingApprovalStatus.APPROVED.getValue());
            lgtBiding.setBidingStatus(BiddingProjectStatus.PROJECT_END.getValue());
            lgtBiding.setGeneratePurchaseApproval(YesOrNo.YES.getValue());
            if(CollectionUtils.isNotEmpty(orderHeads)){
                StringBuffer afterOrderId = new StringBuffer(); // ????????????ID
                StringBuffer afterOrderNo = new StringBuffer(); // ??????????????????
                orderHeads.forEach(orderHead -> {
                    afterOrderId.append(orderHead.getOrderHeadId()).append("-");
                    afterOrderNo.append(orderHead.getOrderHeadNum()).append("-");
                });

                lgtBiding.setAfterOrderId(StringUtils.left(afterOrderId.toString(), afterOrderId.length() - 1));
                lgtBiding.setAfterOrderNo(StringUtils.left(afterOrderNo.toString(), afterOrderNo.length() - 1));
                lgtBiding.setGeneratePurchaseApproval(YesOrNo.YES.getValue());
            }
            this.updateById(lgtBiding);
        } catch (Exception e) {
            log.error("???????????????????????????:"+e.getMessage());
            log.error("???????????????????????????:"+e);
            // ??????????????????
            String stackTrace = Arrays.toString(e.getStackTrace());
            // ????????????
            String message = e.getMessage();
            ConcurrentHashMap<String, String> errorMsg = new ConcurrentHashMap<>();
            errorMsg.put("message", e.getClass().getName() + ": " + message);
            errorMsg.put("stackTrace", stackTrace);
            throw new BaseException("???????????????????????????:"+JSON.toJSONString(errorMsg));
        }

    }

    @Override
    public List<LgtFileConfig> queryLgtFileConfig(Long bidingId,Long quotedHeadId) {
        LgtBiding lgtBiding = getById(bidingId);
        if(!canSeeResultSet.contains(lgtBiding.getBidingStatus())){
            throw new BaseException("??????????????????????????????????????????");
        }
        List<LgtFileConfig> lgtFileConfigs = iLgtFileConfigService.list(Wrappers.lambdaQuery(LgtFileConfig.class).
                eq(LgtFileConfig::getBidingId, bidingId).
                eq(LgtFileConfig::getReferenceFileType, BidingFileTypeEnum.COMMERCIA_BID.getCode()));
        if(CollectionUtils.isNotEmpty(lgtFileConfigs)){
            List<Long> requireIds = lgtFileConfigs.stream().map(LgtFileConfig::getRequireId).collect(Collectors.toList());
            List<LgtVendorFile> lgtVendorFiles = iLgtVendorFileService.list(Wrappers.lambdaQuery(LgtVendorFile.class).eq(LgtVendorFile::getQuotedHeadId, quotedHeadId).in(LgtVendorFile::getRequireId, requireIds));
            if(CollectionUtils.isNotEmpty(lgtVendorFiles)){
                Map<Long, LgtVendorFile> vendorFileMap = lgtVendorFiles.stream().collect(Collectors.toMap(LgtVendorFile::getRequireId, Function.identity()));
                lgtFileConfigs.forEach(lgtFileConfig -> {
                    LgtVendorFile lgtVendorFile = vendorFileMap.get(lgtFileConfig.getRequireId());
                    if(null != lgtVendorFile){
                        lgtFileConfig.setVendorDocId(lgtVendorFile.getDocId());
                        lgtFileConfig.setVendorFileName(lgtVendorFile.getFileName());
                    }
                });
            }
        }
        return lgtFileConfigs;
    }

    @Override
    public void invalidBid(Long quotedHeadId,String invalidReason) {
        LgtVendorQuotedHead lgtVendorQuotedHead = iLgtVendorQuotedHeadService.getById(quotedHeadId);
        Assert.notNull(lgtVendorQuotedHead,"?????????????????????????????????");
        Long bidingId = lgtVendorQuotedHead.getBidingId();
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"???????????????????????????!");
        Assert.isTrue(lgtBiding.getEnrollEndDatetime().compareTo(new Date()) > 0,"???????????????????????????,????????????!");
        Assert.isTrue(lgtBiding.getCurrentRound().equals(lgtVendorQuotedHead.getRound()),"???????????????????????????,????????????!");
        Assert.isTrue(!BiddingProjectStatus.PUBLICITY_OF_RESULT.getValue().equals(lgtBiding.getBidingStatus()),"???????????????,????????????!");
        lgtVendorQuotedHead.setStatus(BiddingOrderStates.INVALID.getValue());
        lgtVendorQuotedHead.setInvalidReason(invalidReason);
        iLgtVendorQuotedHeadService.updateById(lgtVendorQuotedHead);
    }

    @Override
    public LgtVendorQuotedHeadDto getQuotedInfoByQuotedHeadId(Long quotedHeadId) {
        return iLgtBidingVendorService.getLgtVendorQuotedHeadByQuotedHeadId(quotedHeadId);
    }

    @Override
    @Transactional
    public void submitQuotedPrice(LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto) {
        iLgtBidingVendorService.checkLgtVendorQuotedLine(lgtVendorQuotedHeadDto);
        // ????????????
        checkSubmitQuotedPriceParam(lgtVendorQuotedHeadDto);
        // ??????????????????
        quotedPriceSave(lgtVendorQuotedHeadDto);
        // ????????????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        LgtVendorQuotedHead lgtVendorQuotedHead = lgtVendorQuotedHeadDto.getLgtVendorQuotedHead();
        lgtVendorQuotedHead.setStatus(BiddingOrderStates.SUBMISSION.getValue())
                .setIfProxy(YesOrNo.YES.getValue())
                .setSubmitDate(new Date())
                .setSubmitUserId(loginAppUser.getUserId())
                .setSubmitUsername(loginAppUser.getUsername())
                .setSubmitNikeName(loginAppUser.getNickname());
        iLgtVendorQuotedHeadService.updateById(lgtVendorQuotedHead);
        // ????????????
        Long bidingId = lgtVendorQuotedHeadDto.getLgtBiding().getBidingId();
        LgtBiding lgtBiding = this.getById(bidingId);
        String biddingSuppliers = lgtBiding.getBiddingSuppliers();
        String[] split = biddingSuppliers.split("/");
        biddingSuppliers = (Integer.parseInt(split[0]) + 1) + "/" +split[1];
        lgtBiding.setBiddingSuppliers(biddingSuppliers);
        this.updateById(lgtBiding);
    }

    public void checkSubmitQuotedPriceParam(LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto) {
        List<LgtFileConfig> lgtFileConfigs = lgtVendorQuotedHeadDto.getLgtFileConfigs();
        if(CollectionUtils.isNotEmpty(lgtFileConfigs)){
            lgtFileConfigs.forEach(lgtFileConfig -> {
                Assert.isTrue(!ObjectUtils.isEmpty(lgtFileConfig.getVendorDocId()),"????????????????????????!");
            });
        }
        Assert.notNull(lgtVendorQuotedHeadDto.getLgtBiding(),"???????????????????????????!");

        if(YesOrNo.YES.getValue().equals(lgtVendorQuotedHeadDto.getLgtBiding().getIfVendorSubmitShipDate())){
            Assert.isTrue(CollectionUtils.isNotEmpty(lgtVendorQuotedHeadDto.getLgtBidShipPeriods()),"????????????????????????!");
        }

        // ???????????????
        checkLgtVendorQuotedLines(lgtVendorQuotedHeadDto);

    }

    public void checkLgtVendorQuotedLines(LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto) {
        LgtBiding lgtBiding = lgtVendorQuotedHeadDto.getLgtBiding();
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = lgtVendorQuotedHeadDto.getLgtVendorQuotedLines();
        Assert.notNull(lgtVendorQuotedLines,"????????????????????????!");
        Set<String> hashSet = new HashSet<>();
        lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
            LgtBidingServiceImpl.setStartEndAddress(lgtVendorQuotedLine);
            // ???????????? LEG +??????+????????????+????????????
            StringBuffer onlyKey = new StringBuffer();
            onlyKey.append(lgtVendorQuotedLine.getRowNum()).
                    append(lgtVendorQuotedLine.getLeg()).
                    append(lgtVendorQuotedLine.getExpenseItem()).
                    append(lgtVendorQuotedLine.getChargeMethod()).
                    append(lgtVendorQuotedLine.getChargeUnit());
            if(!hashSet.add(onlyKey.toString())){
                throw new BaseException(String.format("???????????????:[%s+LEG +??????+????????????+????????????]????????????!",lgtVendorQuotedLine.getStartAddress()+lgtVendorQuotedLine.getEndAddress()));
            }
        });
    }

    @Transactional
    public void quotedPriceSave(LgtVendorQuotedHeadDto lgtVendorQuotedHead) {
        // ????????????,???????????????????????????????????????????????????????????????????????????????????????????????????
        Long bidingId = lgtVendorQuotedHead.getLgtBiding().getBidingId();
        Assert.notNull(bidingId,"????????????: bidingId");
        LgtBiding lgtBiding = this.getById(bidingId);
        LgtVendorQuotedHead vendorQuotedHead = lgtVendorQuotedHead.getLgtVendorQuotedHead();
        Assert.notNull(vendorQuotedHead,"???????????????????????????!");
        Assert.isTrue(BiddingProjectStatus.ACCEPT_BID.getValue().equals(lgtBiding.getBidingStatus()) &&
                        (BiddingOrderStates.DRAFT.getValue().equals(vendorQuotedHead.getStatus()) ||
                                BiddingOrderStates.WITHDRAW.getValue().equals(vendorQuotedHead.getStatus())),
                "?????????????????????\"???????????????\",???????????????\"?????????\"?????????????????????");

        // ??????????????????
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = lgtVendorQuotedHead.getLgtVendorQuotedLines();
        iLgtVendorQuotedLineService.remove(new QueryWrapper<>(new LgtVendorQuotedLine().setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
            lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                LgtBidingServiceImpl.setStartEndAddress(lgtVendorQuotedLine);
                lgtVendorQuotedLine.setQuotedLineId(IdGenrator.generate()).
                        setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())
                        .setBidingId(bidingId)
                        .setIfProxy(YesOrNo.YES.getValue())
                        .setRound(lgtBiding.getCurrentRound());
            });
            iLgtVendorQuotedLineService.saveBatch(lgtVendorQuotedLines);
            // ?????????????????????
            calculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines);
        }

        // ????????????
        List<LgtBidShipPeriod> lgtBidShipPeriods = lgtVendorQuotedHead.getLgtBidShipPeriods();
        iLgtBidShipPeriodService.remove(new QueryWrapper<>(new LgtBidShipPeriod().setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
        if(CollectionUtils.isNotEmpty(lgtBidShipPeriods)){
            List<LgtBidRequirementLine> bidRequirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).eq(LgtBidRequirementLine::getBidingId, lgtBiding.getBidingId()));
            Map<String, LgtBidRequirementLine> requirementLineMap = bidRequirementLines.stream().collect(Collectors.toMap(lgtBidRequirementLine -> lgtBidRequirementLine.getStartAddress() + lgtBidRequirementLine.getEndAddress(), Function.identity(),(k1, k2)->k1));
            HashSet<String> hashSet = new HashSet<>();
            lgtBidShipPeriods.forEach(lgtBidShipPeriod -> {
                // ??????????????????????????????????????????
                LgtBidingServiceImpl.setStartEndAddress(lgtBidShipPeriod);
                // String key = String.valueOf(lgtBidShipPeriod.getStartAddress()) + lgtBidShipPeriod.getEndAddress();
                // Assert.isTrue(hashSet.add(key),String.format("????????????: [%s],????????????!",key));
                // LgtBidRequirementLine requirementLine = requirementLineMap.get(key);
                // Assert.notNull(requirementLine,String.format("????????????: [%s],????????????????????????!",key));

                lgtBidShipPeriod.setShipPeriodId(IdGenrator.generate())
                        .setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())
                        .setBidingId(bidingId)
                        .setVendorId(vendorQuotedHead.getVendorId())
                        .setVendorCode(vendorQuotedHead.getVendorCode())
                        .setVendorName(vendorQuotedHead.getVendorName())
                        .setRound(lgtBiding.getCurrentRound());
            });
            iLgtBidShipPeriodService.saveBatch(lgtBidShipPeriods);
        }

        // ????????????
        List<LgtFileConfig> lgtFileConfigs = lgtVendorQuotedHead.getLgtFileConfigs();
        iLgtVendorFileService.remove(new QueryWrapper<>(new LgtVendorFile().setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
        if(CollectionUtils.isNotEmpty(lgtFileConfigs)){
            List<LgtVendorFile> lgtVendorFiles = new ArrayList<>();
            lgtFileConfigs.forEach(lgtFileConfig -> {
                if(!ObjectUtils.isEmpty(lgtFileConfig.getVendorDocId())){
                    LgtVendorFile lgtVendorFile = new LgtVendorFile()
                            .setVendorFileId(IdGenrator.generate())
                            .setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())
                            .setVendorId(vendorQuotedHead.getVendorId())
                            .setVendorCode(vendorQuotedHead.getVendorCode())
                            .setVendorName(vendorQuotedHead.getVendorName())
                            .setRound(lgtBiding.getCurrentRound())
                            .setRequireId(lgtFileConfig.getRequireId())
                            .setDocId(lgtFileConfig.getVendorDocId())
                            .setFileName(lgtFileConfig.getVendorFileName())
                            .setFileType(lgtFileConfig.getReferenceFileType())
                            .setBidingId(bidingId);
                    lgtVendorFiles.add(lgtVendorFile);
                }
            });
            iLgtVendorFileService.saveBatch(lgtVendorFiles);
        }
    }

    public void calculationVendorQuotedLines(LgtBiding lgtBiding, List<LgtVendorQuotedLine> lgtVendorQuotedLines) {
        // ?????????????????????????????????  (???????????????-??????)
        Map<String, LatestGidailyRate> latestGidailyRateMap = getLatestGidailyRateMap(lgtBiding);

        if (CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
            // ???????????? ??????????????????????????????,?????????
            insideCalculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines,latestGidailyRateMap);
        }else {
            // ??????????????? ??????????????????????????????,?????????
            noInsideCalculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines,latestGidailyRateMap);
        }
    }

    @Override
    public LgtVendorQuotedHeadDto getQuotedInfo(Long bidingId, Long vendorId) {
        return iLgtBidingVendorService.getLgtVendorQuotedHead(bidingId, vendorId);
    }

    @Override
    public List<LgtVendorQuotedHead> agencyQuotationQueryVendor(Long bidingId) {
        /**
         * ??????????????????????????????
         */
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"????????????????????????!");
        Assert.isTrue(lgtBiding.getEnrollEndDatetime().compareTo(new Date()) > 0,"???????????????????????????,??????????????????!");
        Assert.isTrue(!BiddingProjectStatus.PUBLICITY_OF_RESULT.getValue().equals(lgtBiding.getBidingStatus()),"???????????????,??????????????????!");

        List<LgtVendorQuotedHead> quotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, bidingId).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).
                and(wrapper->wrapper.eq(LgtVendorQuotedHead::getStatus,BiddingOrderStates.DRAFT.getValue()).or().
                        eq(LgtVendorQuotedHead::getStatus,BiddingOrderStates.WITHDRAW.getValue())));
        return quotedHeads;
    }

    @Override
    public void publicResult(Long bidingId, Integer round) {
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.isTrue(lgtBiding.getCurrentRound() == round,"??????????????????????????????????????????!");
        Assert.isTrue(!checkUserIfTechnology(bidingId),"????????????????????????!!!");
        /**
         * ???????????????????????????
         * 1. ?????????????????????????????????
         */
        List<LgtVendorQuotedSum> lgtVendorQuotedSums = iLgtVendorQuotedSumService.list(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                eq(LgtVendorQuotedSum::getBidingId, bidingId).
                eq(LgtVendorQuotedSum::getRound, round));
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtVendorQuotedSums),"?????????[??????]???[??????]???????????????");
        long count = lgtVendorQuotedSums.stream().filter(lgtVendorQuotedSum ->
                YesOrNo.YES.getValue().equals(lgtVendorQuotedSum.getShortlisted()) || SelectionStatusEnum.WIN.getValue().equals(lgtVendorQuotedSum.getBidResult())).count();
        Assert.isTrue(count > 0,"?????????[??????]???[??????]???????????????");

        // ???????????????
        LgtRound lgtRound = iLgtRoundService.getOne(Wrappers.lambdaQuery(LgtRound.class).
                eq(LgtRound::getBidingId, bidingId).
                eq(LgtRound::getRound, round));
        lgtRound.setPublicResult(YesOrNo.YES.getValue());
        lgtRound.setPublicResultTime(new Date());
        iLgtRoundService.updateById(lgtRound);

        // ??????????????????
        lgtBiding.setBidingStatus(BiddingProjectStatus.PUBLICITY_OF_RESULT.getValue());
        this.updateById(lgtBiding);

        // ????????????
        updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.bidEvaluation.getValue())
                .setBidingId(lgtBiding.getBidingId()).setDataFlag(YesOrNo.YES.getValue()));

    }

    @Override
    @Transactional
    public void shortlistedOrEliminated(LgtVendorQuotedSumResult lgtVendorQuotedSumResult) {
        List<LgtVendorQuotedSum> lgtVendorQuotedSums = lgtVendorQuotedSumResult.getLgtVendorQuotedSums();
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedSums)){
            // ????????????????????????
            LgtVendorQuotedSum lgtVendorQuotedSum1 = lgtVendorQuotedSums.get(0);
            Integer round = lgtVendorQuotedSum1.getRound();
            Long bidingId = lgtVendorQuotedSum1.getBidingId();
            Assert.isTrue(!checkUserIfTechnology(bidingId),"????????????????????????!!!");
            LgtBiding lgtBiding = this.getById(bidingId);
            Assert.isTrue(BiddingProjectStatus.BUSINESS_EVALUATION.getValue().equals(lgtBiding.getBidingStatus()),"??????????????????[????????????],????????????!");
            Assert.isTrue(lgtBiding.getCurrentRound() == round,"??????????????????????????????????????????!");
            String bidResult = lgtVendorQuotedSumResult.getBidResult();
            String shortlisted = lgtVendorQuotedSumResult.getShortlisted();
            lgtVendorQuotedSums.forEach(lgtVendorQuotedSum -> {
                Optional.ofNullable(bidResult).ifPresent(s -> lgtVendorQuotedSum.setBidResult(s));
                Optional.ofNullable(shortlisted).ifPresent(s -> lgtVendorQuotedSum.setShortlisted(s));
            });
            iLgtVendorQuotedSumService.updateBatchById(lgtVendorQuotedSums);

            // ??????????????????
            List<LgtVendorQuotedSum> quotedSumList = iLgtVendorQuotedSumService.list(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                    eq(LgtVendorQuotedSum::getBidingId, bidingId).
                    eq(LgtVendorQuotedSum::getRound, lgtBiding.getCurrentRound()));
            if(CollectionUtils.isNotEmpty(quotedSumList)){
                quotedSumList.forEach(lgtVendorQuotedSum -> {
                    if(YesOrNo.YES.getValue().equals(lgtVendorQuotedSumResult.getOperateType())){
                        lgtVendorQuotedSum.setBidResult(null);
                    }else {
                        lgtVendorQuotedSum.setShortlisted(null);
                    }
                });
                iLgtVendorQuotedSumService.updateBatchById(quotedSumList);
            }
        }
    }

    /**
     * ?????????????????????????????????
     */
    public Map<String, LatestGidailyRate> getLatestGidailyRateMap(LgtBiding lgtBiding){
        Map<String, LatestGidailyRate> latestGidailyRateMap = new HashMap<>();
        // ????????????
        String standardCurrency = lgtBiding.getStandardCurrency();
        // ????????????
        String exchangeRateType = lgtBiding.getExchangeRateType();
        latestGidailyRateMap = baseClient.getLatestGidailyRate(standardCurrency, exchangeRateType);
        return latestGidailyRateMap;
    }

    /**
     * ??????????????????????????????
     */
    public boolean checkUserIfTechnology(Long bidingId){
        boolean flag = false;
        String userName = AppUserUtil.getUserName();
        List<LgtGroup> lgtGroups = iLgtGroupService.list(new QueryWrapper<>(new LgtGroup().setBidingId(bidingId)));
        if(CollectionUtils.isNotEmpty(lgtGroups)) {
            long count = lgtGroups.stream().filter(lgtGroup -> YesOrNo.YES.getValue().equals(lgtGroup.getJudgeFlag()) && userName.equals(lgtGroup.getUserName())).count();
            if (count > 0) {
                flag = true;
            }
        }
        return flag;
    }

    @Override
    @Transactional
    public void selectionQuotedLineSave(List<LgtVendorQuotedLine> lgtVendorQuotedLines) {
        /**
         * ????????????,???????????????????????????,?????????????????????????????????,????????????????????????
         * ???????????????, ????????????????????????, ???????????????+?????????+?????????
         * ??????????????????,???????????????????????????
         */
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
            Long bidingId = lgtVendorQuotedLines.get(0).getBidingId();
//            Assert.isTrue(checkUserIfTechnology(bidingId),"???????????????????????????!");
            Assert.notNull(bidingId,"??????????????????????????????: bidingId");
            LgtBiding lgtBiding = this.getById(bidingId);
            Assert.notNull(lgtBiding,"????????????????????????!");
            // ?????????????????????????????????  (???????????????-??????)
            Map<String, LatestGidailyRate> latestGidailyRateMap = getLatestGidailyRateMap(lgtBiding);

            if (CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
                // ???????????? ??????????????????????????????,?????????
                insideCalculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines,latestGidailyRateMap);
            }else {
                // ??????????????? ??????????????????????????????,?????????
                noInsideCalculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines,latestGidailyRateMap);
            }

            // ?????????????????????????????????
            saveUpdateQuotedSum(lgtVendorQuotedLines, lgtBiding);
        }
    }

    /**
     * ????????????->???????????? ????????????
     * @param lgtBiding
     */
    @Transactional
    public void updateBusinessEvaluationStatus(LgtBiding lgtBiding) {
        lgtBiding.setBidingStatus(BiddingProjectStatus.BUSINESS_EVALUATION.getValue());
        LgtRound lgtRound = iLgtRoundService.getOne(Wrappers.lambdaQuery(LgtRound.class).
                eq(LgtRound::getBidingId, lgtBiding.getBidingId()).
                eq(LgtRound::getRound, lgtBiding.getCurrentRound()).last(" LIMIT 1"));
        lgtRound.setBusinessOpenBid(YesOrNo.YES.getValue());
        lgtRound.setBusinessOpenBidTime(new Date());

        // ??????????????????????????????
        this.updateById(lgtBiding);
        // ?????? ???????????????
        iLgtRoundService.updateById(lgtRound);
    }


    @Transactional
    public void saveUpdateQuotedSum(List<LgtVendorQuotedLine> lgtVendorQuotedLines,LgtBiding lgtBiding) {
        // ?????????????????????
        lgtVendorQuotedLines.forEach(LgtBidingServiceImpl::setStartEndAddress);
        /**
         * 1. ??????????????????????????????????????????????????????+?????????+??????????????????????????????????????????????????????
         * 2. ???????????????????????????????????????????????????????????????????????????????????????
         * 3. ???????????????????????????????????????????????????????????????????????????????????????
         */
        // ????????????????????????
        List<LgtVendorQuotedSum> lgtVendorQuotedSums = new ArrayList<>();
        Map<String, List<LgtVendorQuotedLine>> listMap = null;
        if (BidingAwardWayEnum.COMBINED_DECISION.name().equals(lgtBiding.getBidingAwardWay())) {
            // ??????????????????
            listMap = lgtVendorQuotedLines.stream().collect(Collectors.groupingBy(
                    lgtVendorQuotedLine -> String.valueOf(lgtVendorQuotedLine.getVendorId())));
        }else {
            // ???????????????+?????????+???????????????
            listMap = lgtVendorQuotedLines.stream().collect(Collectors.groupingBy(
                    lgtVendorQuotedLine -> lgtVendorQuotedLine.getVendorId() + lgtVendorQuotedLine.getStartAddress() + lgtVendorQuotedLine.getEndAddress()));
        }

        listMap.forEach((key, lgtVendorQuotedLineList) -> {
            // ??????????????????????????????
            LgtVendorQuotedLine lgtVendorQuotedLine = lgtVendorQuotedLineList.get(0);
            // ????????????????????????
            BigDecimal sumPrice = lgtVendorQuotedLineList.stream().filter(lgtVendorQuotedLine1 -> !ObjectUtils.isEmpty(lgtVendorQuotedLine1.getTotalAmount())).
                    map(LgtVendorQuotedLine::getTotalAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

            // ????????????????????????
            LgtVendorQuotedSum lgtVendorQuotedSum = new LgtVendorQuotedSum()
                    .setQuotedSumId(IdGenrator.generate())
                    .setBidingId(lgtBiding.getBidingId())
                    .setQuotedHeadId(lgtVendorQuotedLine.getQuotedHeadId())
                    .setVendorId(lgtVendorQuotedLine.getVendorId())
                    .setVendorCode(lgtVendorQuotedLine.getVendorCode())
                    .setVendorName(lgtVendorQuotedLine.getVendorName())
                    .setStartAddress(lgtVendorQuotedLine.getStartAddress())
                    .setEndAddress(lgtVendorQuotedLine.getEndAddress())
                    .setSumPrice(sumPrice)
                    .setIfProxy(lgtVendorQuotedLine.getIfProxy())
                    .setRowNum(lgtVendorQuotedLine.getRowNum())
                    .setBidRequirementLineId(lgtVendorQuotedLine.getBidRequirementLineId())
                    .setRound(lgtVendorQuotedLine.getRound());
            lgtVendorQuotedSums.add(lgtVendorQuotedSum);
        });

        // ??????????????????
//        saveLgtQuoteSum(lgtBiding, lgtVendorQuotedSums);
        saveLgtQuoteSumNew(lgtBiding, lgtVendorQuotedSums);
    }

    @Transactional
    public void saveLgtQuoteSumNew(LgtBiding lgtBiding, List<LgtVendorQuotedSum> lgtVendorQuotedSums){
        // ???????????????????????????
        iLgtVendorQuotedSumService.remove(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                eq(LgtVendorQuotedSum::getBidingId, lgtBiding.getBidingId()).
                eq(LgtVendorQuotedSum::getRound, lgtBiding.getCurrentRound()));
        // ??????
        iLgtVendorQuotedSumService.saveBatch(lgtVendorQuotedSums);
    }

    @Transactional
    public void saveLgtQuoteSum(LgtBiding lgtBiding, List<LgtVendorQuotedSum> lgtVendorQuotedSums) {
        List<LgtVendorQuotedSum> vendorQuotedSums = iLgtVendorQuotedSumService.list(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                eq(LgtVendorQuotedSum::getBidingId, lgtBiding.getBidingId()).
                eq(LgtVendorQuotedSum::getRound, lgtBiding.getCurrentRound()));
        if(CollectionUtils.isNotEmpty(vendorQuotedSums)){
            // ????????????
            Map<String, LgtVendorQuotedSum> quotedSumMap = null;
            if (BidingAwardWayEnum.COMBINED_DECISION.name().equals(lgtBiding.getBidingAwardWay())) {
                // ????????????
                quotedSumMap = lgtVendorQuotedSums.stream().collect(Collectors.toMap(lgtVendorQuotedSum -> String.valueOf(lgtVendorQuotedSum.getVendorId()), Function.identity()));
            }else {
                quotedSumMap = lgtVendorQuotedSums.stream().collect(Collectors.toMap(lgtVendorQuotedSum -> lgtVendorQuotedSum.getVendorId() +lgtVendorQuotedSum.getStartAddress()+lgtVendorQuotedSum.getEndAddress(), Function.identity()));
            }
            Map<String, LgtVendorQuotedSum> finalQuotedSumMap = quotedSumMap;
            vendorQuotedSums.forEach(lgtVendorQuotedSum -> {
                LgtVendorQuotedSum lgtVendorQuotedSum1 = null;
                if (BidingAwardWayEnum.COMBINED_DECISION.name().equals(lgtBiding.getBidingAwardWay())) {
                    // ????????????
                    lgtVendorQuotedSum1 = finalQuotedSumMap.get(String.valueOf(lgtVendorQuotedSum.getVendorId()));
                }else {
                    lgtVendorQuotedSum1 = finalQuotedSumMap.get(lgtVendorQuotedSum.getVendorId() +lgtVendorQuotedSum.getStartAddress()+lgtVendorQuotedSum.getEndAddress());
                }
                if(null != lgtVendorQuotedSum1){
                    lgtVendorQuotedSum.setSumPrice(lgtVendorQuotedSum1.getSumPrice());
                }
            });
            iLgtVendorQuotedSumService.updateBatchById(vendorQuotedSums);
        }else {
            iLgtVendorQuotedSumService.saveBatch(lgtVendorQuotedSums);
        }
    }

    public Map<Long, LgtVendorQuotedHead> getQuotedHeadMap(LgtBiding lgtBiding) {
        List<LgtVendorQuotedHead> lgtVendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, lgtBiding.getBidingId()).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).
                eq(LgtVendorQuotedHead::getStatus, BiddingOrderStates.SUBMISSION.getValue()));
        return lgtVendorQuotedHeads.stream().collect(Collectors.toMap(LgtVendorQuotedHead::getQuotedHeadId, Function.identity()));
    }

    @Override
    @Transactional
    public LgtVendorQuotedSumDto queryLgtVendorQuotedSumDto(Long bidingId,Integer round,Long vendorId,Integer rank,String bidResult) {
        LgtVendorQuotedSumDto lgtVendorQuotedSumDto = new LgtVendorQuotedSumDto();

        LgtBiding lgtBiding = this.getById(bidingId);

        Assert.notNull(lgtBiding,"??????????????????????????????!");
        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if(!canSeeResultSet.contains(lgtBiding.getBidingStatus())){
            return null;
        }
        lgtVendorQuotedSumDto.setTechnoSelection(lgtBiding.getSummaryDescription());

        // ?????????????????????????????????  (???????????????-??????)
        Map<String, LatestGidailyRate> latestGidailyRateMap = getLatestGidailyRateMap(lgtBiding);

        // ??????????????????????????????
        if(lgtBiding.getEnrollEndDatetime().compareTo(new Date()) < 0){
            if(BiddingProjectStatus.TENDER_ENDING.getValue().equals(lgtBiding.getBidingStatus())){
                // ????????????????????????
                initSelectionQuotedLine(lgtBiding, latestGidailyRateMap);
            }

            // ???????????????????????????
            List<LgtVendorQuotedHead> lgtVendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                    eq(LgtVendorQuotedHead::getBidingId, bidingId).
                    eq(LgtVendorQuotedHead::getRound, round).
                    eq(LgtVendorQuotedHead::getStatus, BiddingOrderStates.SUBMISSION.getValue()).
                    eq(!ObjectUtils.isEmpty(vendorId),LgtVendorQuotedHead::getVendorId, vendorId));

            if(CollectionUtils.isNotEmpty(lgtVendorQuotedHeads)) {
                List<Long> quotedHeadIds = lgtVendorQuotedHeads.stream().map(LgtVendorQuotedHead::getQuotedHeadId).collect(Collectors.toList());
                // ???????????????????????????????????????
                List<LgtVendorQuotedLine> lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                        eq(LgtVendorQuotedLine::getBidingId, bidingId).
                        eq(LgtVendorQuotedLine::getRound, round).
                        in(LgtVendorQuotedLine::getQuotedHeadId, quotedHeadIds));
                // ?????????-???
                lgtVendorQuotedLines.forEach(LgtBidingServiceImpl::setStartEndAddress);
                /**
                 * ??????????????????????????????????????????????????????
                 */
                if(TransportModeEnum.LAND_TRANSPORT.name().equals(lgtBiding.getTransportModeCode())){
                    List<LgtVendorQuotedLine> quotedLines = new ArrayList<>();
                    // ??????????????????
                    List<LgtVendorQuotedLine> singleDragCostNulls = lgtVendorQuotedLines.stream().filter(lgtVendorQuotedLine -> ObjectUtils.isEmpty(lgtVendorQuotedLine.getSingleDragCost())).collect(Collectors.toList());
                    // ?????????????????????
                    List<LgtVendorQuotedLine> singleDragCostNoNulls = lgtVendorQuotedLines.stream().filter(lgtVendorQuotedLine -> !ObjectUtils.isEmpty(lgtVendorQuotedLine.getSingleDragCost())).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(singleDragCostNoNulls)) {
                        // ????????????
                        singleDragCostNoNulls.sort(Comparator.comparing(LgtVendorQuotedLine::getSingleDragCost));
                        quotedLines.addAll(singleDragCostNoNulls);
                    }
                    if(CollectionUtils.isNotEmpty(singleDragCostNulls)) quotedLines.addAll(singleDragCostNulls);
                    lgtVendorQuotedSumDto.setLgtVendorQuotedLines(quotedLines);
                }else {
                    lgtVendorQuotedSumDto.setLgtVendorQuotedLines(lgtVendorQuotedLines);
                }

                // ????????????????????????
                List<LgtVendorQuotedSum> lgtVendorQuotedSumsResult = new ArrayList<>();
                // ????????????????????????
                List<LgtVendorQuotedSum> lgtVendorQuotedSums = getLgtVendorQuotedSum(bidingId, round, vendorId, lgtBiding, lgtVendorQuotedLines);
                // ?????????????????????
                if(CollectionUtils.isNotEmpty(lgtVendorQuotedSums)){
                    if(BidingAwardWayEnum.COMBINED_DECISION.name().equals(lgtBiding.getBidingAwardWay())){
                        // ??????
                        AtomicInteger num = new AtomicInteger(1);
                        lgtVendorQuotedSums.sort(Comparator.comparing(LgtVendorQuotedSum::getSumPrice));
                        lgtVendorQuotedSums.forEach(lgtVendorQuotedSum -> lgtVendorQuotedSum.setRank(num.getAndAdd(1)));
                        lgtVendorQuotedSumsResult.addAll(lgtVendorQuotedSums);
                    }else {
                        List<BigDecimal> sumPriceList = lgtVendorQuotedSums.stream().map(LgtVendorQuotedSum::getSumPrice).distinct().collect(Collectors.toList());
                        // ????????????
                        sumPriceList.sort(BigDecimal::compareTo);
                        Map<String, List<LgtVendorQuotedSum>> listMap = lgtVendorQuotedSums.stream().collect(Collectors.groupingBy(lgtVendorQuotedSum -> lgtVendorQuotedSum.getStartAddress() + lgtVendorQuotedSum.getEndAddress()));
                        listMap.forEach((s, lgtVendorQuotedSums1) -> {
                            AtomicInteger integer = new AtomicInteger(1);
                            /** ???????????? */
                            Map<BigDecimal, List<LgtVendorQuotedSum>> map = lgtVendorQuotedSums1.stream().collect(Collectors.groupingBy(LgtVendorQuotedSum::getSumPrice));
                            sumPriceList.forEach(price->{
                                List<LgtVendorQuotedSum> vendorQuotedSums = map.get(price);
                                if(CollectionUtils.isNotEmpty(vendorQuotedSums)){
                                    int num = integer.getAndAdd(1);
                                    vendorQuotedSums.forEach(lgtVendorQuotedSum -> {
                                        lgtVendorQuotedSum.setRank(num);
                                    });
                                    lgtVendorQuotedSumsResult.addAll(vendorQuotedSums);
                                }
                            });
                        });
                    }
                }
                lgtVendorQuotedSumDto.setLgtVendorQuotedSums(lgtVendorQuotedSumsResult);
            }
        }

        /**
         * ?????????????????????????????????????????????
         */
        List<LgtVendorQuotedSum> lgtVendorQuotedSums = lgtVendorQuotedSumDto.getLgtVendorQuotedSums();
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedSums)){
            if(null != rank){
                lgtVendorQuotedSums = lgtVendorQuotedSums.stream().filter(lgtVendorQuotedSum -> rank == lgtVendorQuotedSum.getRank()).collect(Collectors.toList());
            }
            if(null != bidResult){
                lgtVendorQuotedSums = lgtVendorQuotedSums.stream().filter(lgtVendorQuotedSum -> bidResult.equals(lgtVendorQuotedSum.getBidResult())).collect(Collectors.toList());
            }
            lgtVendorQuotedSumDto.setLgtVendorQuotedSums(lgtVendorQuotedSums);
        }
        return lgtVendorQuotedSumDto;
    }

    public List<LgtVendorQuotedSum> getLgtVendorQuotedSum(Long bidingId, Integer round, Long vendorId, LgtBiding lgtBiding, List<LgtVendorQuotedLine> lgtVendorQuotedLines) {
        List<LgtVendorQuotedSum> lgtVendorQuotedSums;
        lgtVendorQuotedSums = iLgtVendorQuotedSumService.list(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                eq(LgtVendorQuotedSum::getBidingId, bidingId).
                eq(LgtVendorQuotedSum::getRound, round).
                eq(!ObjectUtils.isEmpty(vendorId), LgtVendorQuotedSum::getVendorId, vendorId));
        if (CollectionUtils.isEmpty(lgtVendorQuotedSums)) {
            lgtVendorQuotedSums = new ArrayList<>();
            // ??????????????????????????????
            List<LgtVendorQuotedSum> lgtVendorQuotedSumList = new ArrayList<>();
            Map<String, List<LgtVendorQuotedLine>> listMap;
            /**
             * ??????????????????????????????
             * ????????????: ???: ?????????+?????????+?????????  ??????
             * ????????????: ???: ?????????  ??????
             */
            if(BidingAwardWayEnum.COMBINED_DECISION.name().equals(lgtBiding.getBidingAwardWay())){
                // ??????
                 listMap = lgtVendorQuotedLines.stream().collect(Collectors.groupingBy(
                        lgtVendorQuotedLine -> String.valueOf(lgtVendorQuotedLine.getVendorId())));
            }else {
                // ???????????????+?????????+???????????????
                listMap = lgtVendorQuotedLines.stream().collect(Collectors.groupingBy(
                        lgtVendorQuotedLine -> lgtVendorQuotedLine.getVendorId() + lgtVendorQuotedLine.getStartAddress()+lgtVendorQuotedLine.getEndAddress()));
            }

            listMap.forEach((key, lgtVendorQuotedLineList) -> {
                // ??????????????????????????????
                LgtVendorQuotedLine lgtVendorQuotedLine = lgtVendorQuotedLineList.get(0);
                // ????????????????????????
                BigDecimal sumPrice = lgtVendorQuotedLineList.stream().filter(lgtVendorQuotedLine1 -> !ObjectUtils.isEmpty(lgtVendorQuotedLine1.getTotalAmount())).
                        map(LgtVendorQuotedLine::getTotalAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

                // ????????????????????????
                LgtVendorQuotedSum lgtVendorQuotedSum = new LgtVendorQuotedSum()
                        .setQuotedSumId(IdGenrator.generate())
                        .setBidingId(lgtBiding.getBidingId())
                        .setQuotedHeadId(lgtVendorQuotedLine.getQuotedHeadId())
                        .setVendorId(lgtVendorQuotedLine.getVendorId())
                        .setVendorCode(lgtVendorQuotedLine.getVendorCode())
                        .setVendorName(lgtVendorQuotedLine.getVendorName())
                        .setStartAddress(lgtVendorQuotedLine.getStartAddress())
                        .setEndAddress(lgtVendorQuotedLine.getEndAddress())
                        .setSumPrice(sumPrice)
                        .setIfProxy(lgtVendorQuotedLine.getIfProxy())
                        .setRowNum(lgtVendorQuotedLine.getRowNum())
                        .setBidRequirementLineId(lgtVendorQuotedLine.getBidRequirementLineId())
                        .setRound(lgtVendorQuotedLine.getRound());
                lgtVendorQuotedSumList.add(lgtVendorQuotedSum);
            });
            lgtVendorQuotedSums.addAll(lgtVendorQuotedSumList);
            iLgtVendorQuotedSumService.saveBatch(lgtVendorQuotedSums);
        }
        return lgtVendorQuotedSums;
    }

    @Transactional
    public void initSelectionQuotedLine(LgtBiding lgtBiding, Map<String, LatestGidailyRate> latestGidailyRateMap) {
        // ????????????????????????????????????????????????
        List<LgtVendorQuotedHead> lgtVendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, lgtBiding.getBidingId()).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).
                eq(LgtVendorQuotedHead::getStatus, BiddingOrderStates.SUBMISSION.getValue()));
        if (CollectionUtils.isNotEmpty(lgtVendorQuotedHeads)) {
            List<Long> quotedHeadIds = lgtVendorQuotedHeads.stream().map(LgtVendorQuotedHead::getQuotedHeadId).collect(Collectors.toList());
            // ????????????????????????????????????????????????
            List<LgtVendorQuotedLine> lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                    eq(LgtVendorQuotedLine::getBidingId, lgtBiding.getBidingId()).
                    eq(LgtVendorQuotedLine::getRound, lgtBiding.getCurrentRound()).
                    in(LgtVendorQuotedLine::getQuotedHeadId, quotedHeadIds));
            if (CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
                // ???????????? ??????????????????????????????,?????????
                insideCalculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines,latestGidailyRateMap);
            }else {
                // ??????????????? ??????????????????????????????,?????????
                noInsideCalculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines,latestGidailyRateMap);
            }
        }
    }

    /**
     * ??????????????????????????????,?????????
     * @param lgtBiding
     * @param lgtVendorQuotedLines
     */
    @Transactional
    public void noInsideCalculationVendorQuotedLines(LgtBiding lgtBiding, List<LgtVendorQuotedLine> lgtVendorQuotedLines,Map<String, LatestGidailyRate> latestGidailyRateMap) {
        // ????????????
        Integer pricePrecision = lgtBiding.getPricePrecision();
        /**
         * ?????????
         * 1. ????????????????????????????????????????????????????????????????????????????????????????????????=??????*?????????*?????????
         * 2. ?????????????????????????????????*??????<??????????????????????????????
         * 3. ?????????????????????????????????*??????>??????????????????????????????
         * 4. ??????????????????????????????????????????????????????+?????????+??????????????????????????????????????????????????????
         * BigDecimal.ROUND_HALF_UP
         */
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
            lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                // ????????????????????????
                BigDecimal conversionRate = checkCurrencyIfExist(latestGidailyRateMap, lgtVendorQuotedLine,lgtBiding);
                // ????????????????????????
                BigDecimal expense = lgtVendorQuotedLine.getExpense();
                if(!ObjectUtils.isEmpty(expense)){
                    // ????????????1
                    lgtVendorQuotedLine.setNumber(null != lgtVendorQuotedLine.getNumber()?lgtVendorQuotedLine.getNumber():BigDecimal.valueOf(1));
                    // ?????????????????????=??????*?????????*?????????
                    lgtVendorQuotedLine.setTotalAmount(getTotalAmount(pricePrecision, lgtVendorQuotedLine, conversionRate, expense));
                }
            });
            // ?????????????????????
            iLgtVendorQuotedLineService.updateBatchById(lgtVendorQuotedLines);
        }
    }

    /**
     * ??????????????????????????????,?????????
     * @param lgtBiding
     * @param lgtVendorQuotedLines
     */
    @Transactional
    public void insideCalculationVendorQuotedLines(LgtBiding lgtBiding, List<LgtVendorQuotedLine> lgtVendorQuotedLines,Map<String, LatestGidailyRate> latestGidailyRateMap) {
        // ????????????
        Integer pricePrecision = lgtBiding.getPricePrecision();
        /**
         * ???????????????
         * 1. ??????????????????????????????????????????
         * 2. ???????????????=??????/??????????????????
         * 3. ????????????=??????/?????????
         * 4. ????????????1
         * 5. ?????????????????????=??????*?????????*?????????
         * 6. ?????????????????????????????????*??????<??????????????????????????????
         * 7. ?????????????????????????????????*??????>??????????????????????????????
         * 8. ??????????????????????????????????????????????????????+?????????+??????????????????????????????????????????????????????
         * 9. ???????????????????????????????????????????????????????????????????????????????????????
         * 10. ???????????????????????????????????????????????????????????????????????????????????????
         * BigDecimal.ROUND_HALF_UP
         */
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
            lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                // ????????????????????????
                BigDecimal conversionRate = checkCurrencyIfExist(latestGidailyRateMap, lgtVendorQuotedLine,lgtBiding);
                // ????????????????????????
                BigDecimal expense = lgtVendorQuotedLine.getExpense();
                if(!ObjectUtils.isEmpty(expense)){
                    // ?????????????????? = ??????????????????
                    if (ObjectUtils.isEmpty(lgtVendorQuotedLine.getTransportDistanceRevision())) {
                        if (BiddingProjectStatus.ACCEPT_BID.getValue().equals(lgtBiding.getBidingStatus()) ||
                                BiddingProjectStatus.TENDER_ENDING.getValue().equals(lgtBiding.getBidingStatus())) {
                            lgtVendorQuotedLine.setTransportDistanceRevision(lgtVendorQuotedLine.getTransportDistance());
                        }
                    }
                    // ???????????????= ??????/ ??????????????????
                    lgtVendorQuotedLine.setSingleKmCost(null != lgtVendorQuotedLine.getTransportDistance() && !ObjectUtils.isEmpty(lgtVendorQuotedLine.getTransportDistanceRevision())?expense.divide(new BigDecimal(lgtVendorQuotedLine.getTransportDistanceRevision()),pricePrecision,BigDecimal.ROUND_HALF_UP):null);
                    // ????????????=??????/?????????
                    lgtVendorQuotedLine.setSingleDragCost(null != lgtVendorQuotedLine.getLoadNumber()?expense.divide(lgtVendorQuotedLine.getLoadNumber(),pricePrecision,BigDecimal.ROUND_HALF_UP):null);
                    // ????????????1
                    lgtVendorQuotedLine.setNumber(null != lgtVendorQuotedLine.getNumber()?lgtVendorQuotedLine.getNumber():BigDecimal.valueOf(1));
                    // ?????????????????????=??????*?????????*?????????
                    lgtVendorQuotedLine.setTotalAmount(getTotalAmount(pricePrecision, lgtVendorQuotedLine, conversionRate, expense));
                }
            });
            // ?????????????????????
            iLgtVendorQuotedLineService.updateBatchById(lgtVendorQuotedLines);
        }
    }

    /**
     * ????????????
     * @param pricePrecision
     * @param lgtVendorQuotedLine
     * @param conversionRate
     * @param expense
     */
    public BigDecimal getTotalAmount(Integer pricePrecision, LgtVendorQuotedLine lgtVendorQuotedLine, BigDecimal conversionRate, BigDecimal expense) {
        BigDecimal totalAmountTemp = lgtVendorQuotedLine.getNumber().multiply(expense);
        /**
         * ?????????????????????????????????*??????<??????????????????????????????
         * ?????????????????????????????????*??????>??????????????????????????????
         */
        String minCost = lgtVendorQuotedLine.getMinCost();
        if(!ObjectUtils.isEmpty(minCost)){
            if(totalAmountTemp.compareTo(new BigDecimal(minCost)) < 0){
                totalAmountTemp = new BigDecimal(minCost);
            }
        }
        String maxCost = lgtVendorQuotedLine.getMaxCost();
        if(!ObjectUtils.isEmpty(maxCost)){
            if(totalAmountTemp.compareTo(new BigDecimal(maxCost)) > 0){
                totalAmountTemp = new BigDecimal(maxCost);
            }
        }
        return totalAmountTemp.multiply(conversionRate).setScale(pricePrecision,BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal checkCurrencyIfExist(Map<String, LatestGidailyRate> latestGidailyRateMap, LgtVendorQuotedLine lgtVendorQuotedLine,LgtBiding lgtBiding) {
        BigDecimal conversionRate = BigDecimal.valueOf(1);
        if(!ObjectUtils.isEmpty(lgtVendorQuotedLine.getCurrency()) && !lgtBiding.getStandardCurrency().equals(lgtVendorQuotedLine.getCurrency())){
            LatestGidailyRate latestGidailyRate = latestGidailyRateMap.get(lgtVendorQuotedLine.getCurrency());
            Assert.notNull(latestGidailyRate,lgtVendorQuotedLine.getVendorName()+"?????????:"+lgtVendorQuotedLine.getCurrency()+",??????????????????:"+lgtBiding.getStandardCurrency()+"?????????!");
            conversionRate = latestGidailyRate.getConversionRate();
        }
        return conversionRate;
    }

    @Override
    @Transactional
    public void bidOpeningCalculationResult(LgtBiding lgtBiding) {
        /**
         * 1. ???????????????????????????????????????
         * 2. ????????????????????????????????????
         */
        if(BiddingProjectStatus.TENDER_ENDING.getValue().equals(lgtBiding.getBidingStatus())
        && lgtBiding.getEnrollEndDatetime().compareTo(new Date()) < 0){
            // ??????????????????
            List<LgtVendorQuotedSum> lgtVendorQuotedSums = getLgtVendorQuotedSums(lgtBiding);
            iLgtVendorQuotedSumService.saveBatch(lgtVendorQuotedSums);
        }
    }

    @Override
    public LgtVendorQuotedHeadVo getTopInfo(Long bidingId) {
        LgtVendorQuotedHeadVo lgtVendorQuotedHeadVo = new LgtVendorQuotedHeadVo();
        BidControlTopInfoVO bidControlTopInfoVO = new BidControlTopInfoVO();
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"??????????????????????????????!");
        // ????????????????????????
        bidControlTopInfoVO.setEndTime(lgtBiding.getEnrollEndDatetime());
        List<LgtVendorQuotedHead> lgtVendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, bidingId).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()));
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedHeads)){
            /**
             * private Integer currentRoundSupplierCount;//?????????????????????????????????
             *     private Integer submitSupplierCount;//??????????????????????????????
             *     private Date endTime;//????????????????????????
             */
            // ?????????????????????????????????
            bidControlTopInfoVO.setCurrentRoundSupplierCount(lgtVendorQuotedHeads.size());
            // ????????????????????????
            long count = lgtVendorQuotedHeads.stream().filter(lgtVendorQuotedHead -> BiddingOrderStates.SUBMISSION.getValue().equals(lgtVendorQuotedHead.getStatus())).count();
            bidControlTopInfoVO.setSubmitSupplierCount((int)count);
        }else {
            bidControlTopInfoVO.setCurrentRoundSupplierCount(0);
            bidControlTopInfoVO.setSubmitSupplierCount(0);
        }
        // ??????????????????????????????
        LgtRound lgtRound = iLgtRoundService.getOne(Wrappers.lambdaQuery(LgtRound.class).
                eq(LgtRound::getBidingId, bidingId).
                eq(LgtRound::getRound, lgtBiding.getCurrentRound()).last("LIMIT 1"));
        bidControlTopInfoVO.setExtendReason(lgtRound.getExtendReason());

        lgtVendorQuotedHeadVo.setBidControlTopInfoVO(bidControlTopInfoVO);
        lgtVendorQuotedHeadVo.setLgtVendorQuotedHeads(lgtVendorQuotedHeads);
        return lgtVendorQuotedHeadVo;
    }

    public List<LgtVendorQuotedSum> getLgtVendorQuotedSums(LgtBiding lgtBiding) {
        List<LgtVendorQuotedSum> lgtVendorQuotedSums = new ArrayList<>();
        /**
         * ??????????????????
         */
        // ????????????
        String standardCurrency = lgtBiding.getStandardCurrency();
        // ???????????????
        Integer pricePrecision = lgtBiding.getPricePrecision();
        // ??????????????????
        LocalDate currencyChangeDate = lgtBiding.getCurrencyChangeDate();
        // ????????????
        String exchangeRateType = lgtBiding.getExchangeRateType();
        Map<String, LatestGidailyRate> latestGidailyRate = baseClient.getLatestGidailyRate(standardCurrency, exchangeRateType);

        /**
         * ???????????????????????????????????????
         */
        // ???????????????????????????
        List<LgtVendorQuotedHead> vendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, lgtBiding.getBidingId()).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).
                eq(LgtVendorQuotedHead::getStatus, BiddingOrderStates.SUBMISSION.getValue()));
        if (CollectionUtils.isNotEmpty(vendorQuotedHeads)) {
            if(!CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
                List<Long> ids = vendorQuotedHeads.stream().map(LgtVendorQuotedHead::getQuotedHeadId).collect(Collectors.toList());
                Map<Long, LgtVendorQuotedHead> vendorQuotedHeadMap = vendorQuotedHeads.stream().collect(Collectors.toMap(LgtVendorQuotedHead::getQuotedHeadId, Function.identity()));
                /**
                 * ???????????????,?????????,???????????????????????????
                 */
                List<LgtVendorQuotedLine> vendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                        in(LgtVendorQuotedLine::getQuotedHeadId, ids));
                if (CollectionUtils.isNotEmpty(vendorQuotedLines)) {
                    Map<String, List<LgtVendorQuotedLine>> listMap = vendorQuotedLines.stream().collect(Collectors.groupingBy(
                            lgtVendorQuotedLine -> lgtVendorQuotedLine.getQuotedHeadId() + lgtVendorQuotedLine.getStartAddress() + lgtVendorQuotedLine.getEndAddress()));
                    List<LgtVendorQuotedSum> finalLgtVendorQuotedSums = new ArrayList<>();
                    listMap.forEach((s, lgtVendorQuotedLines) -> {
                        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
                            LgtVendorQuotedLine quotedLine = lgtVendorQuotedLines.get(0);
                            LgtVendorQuotedHead vendorQuotedHead = vendorQuotedHeadMap.get(quotedLine.getQuotedHeadId());
                            LgtVendorQuotedSum lgtVendorQuotedSum = new LgtVendorQuotedSum();
                            AtomicReference<BigDecimal> sumPrice = new AtomicReference<>(BigDecimal.ZERO);
                            lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                                // ?????????
                                BigDecimal totalAmount = lgtVendorQuotedLine.getTotalAmount();
                                // ??????
                                String currency = lgtVendorQuotedLine.getCurrency();
                                if(!ObjectUtils.isEmpty(currency)){
                                    /**
                                     * ????????????????????????
                                     */
                                    LatestGidailyRate gidailyRate = latestGidailyRate.get(currency);
                                    if(null == gidailyRate){
                                        throw new BaseException(vendorQuotedHead.getVendorName()+"???????????????:\""+currency+"\"????????????????????????????????????");
                                    }
                                    // ??????????????????????????????
                                    sumPrice.set(sumPrice.get().add(totalAmount.multiply(gidailyRate.getConversionRate()).setScale(pricePrecision,BigDecimal.ROUND_HALF_UP)));
                                }else {
                                    sumPrice.set(sumPrice.get().add(totalAmount.setScale(pricePrecision,BigDecimal.ROUND_HALF_UP)));
                                }
                            });
                            lgtVendorQuotedSum.setQuotedSumId(IdGenrator.generate())
                                    .setBidingId(lgtBiding.getBidingId())
                                    .setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())
                                    .setVendorId(vendorQuotedHead.getVendorId())
                                    .setVendorCode(vendorQuotedHead.getVendorCode())
                                    .setVendorName(vendorQuotedHead.getVendorName())
                                    .setStartAddress(quotedLine.getStartAddress())
                                    .setEndAddress(quotedLine.getEndAddress())
                                    .setSumPrice(sumPrice.get())
                                    .setIfProxy(vendorQuotedHead.getIfProxy())
                                    .setRowNum(quotedLine.getRowNum())
                                    .setRound(vendorQuotedHead.getRound());
                            finalLgtVendorQuotedSums.add(lgtVendorQuotedSum);
                        }
                    });
                    lgtVendorQuotedSums = finalLgtVendorQuotedSums;
                }
            }
            // ??????????????????
            lgtVendorQuotedSums = quotedSumsRank(lgtVendorQuotedSums,lgtBiding);
        }
        return lgtVendorQuotedSums;
    }

    // ??????
    public List<LgtVendorQuotedSum> quotedSumsRank(List<LgtVendorQuotedSum> lgtVendorQuotedSums,LgtBiding lgtBiding) {
//        // ???????????????????????????????????????????????????
//        if(!BiddingProjectStatus.TENDER_ENDING.getValue().equals(lgtBiding.getBidingStatus())){
//            return lgtVendorQuotedSums;
//        }
//
//        List<LgtVendorQuotedSum> quotedSumArrayList = new ArrayList<>();
//        /**
//         * ???????????????????????????????????????????????????????????????????????????????????????
//         * ???????????????????????????????????????????????????????????????????????????????????????
//         */
//        if(CollectionUtils.isNotEmpty(lgtVendorQuotedSums)){
//            if (lgtVendorQuotedSums.size() == 1) {
//                quotedSumArrayList.add(lgtVendorQuotedSums.get(0).setBidResult(SelectionStatusEnum.FIRST_WIN.getValue()));
//            }else {
//                // ?????????????????????
//                Map<BigDecimal, List<LgtVendorQuotedSum>> listMap = lgtVendorQuotedSums.stream().collect(Collectors.groupingBy(LgtVendorQuotedSum::getSumPrice));
//                List<BigDecimal> sumPrices = lgtVendorQuotedSums.stream().map(LgtVendorQuotedSum::getSumPrice).distinct().collect(Collectors.toList());
//                // ??????
//                sumPrices.sort(BigDecimal::compareTo);
//                AtomicInteger integer = new AtomicInteger(0);
//                for (int i = 0;i< sumPrices.size();i++){
//                    integer.set(i);
//                    List<LgtVendorQuotedSum> quotedSums = listMap.get(sumPrices.get(i));
//                    if(CollectionUtils.isNotEmpty(quotedSums)){
//                        quotedSums.forEach(lgtVendorQuotedSum -> {
//                            if (0 == integer.get()) {
//                                lgtVendorQuotedSum.setBidResult(SelectionStatusEnum.FIRST_WIN.getValue());
//                            }else if(1 == integer.get()){
//                                lgtVendorQuotedSum.setBidResult(SelectionStatusEnum.SECOND_WIN.getValue());
//                            }
//                            quotedSumArrayList.add(lgtVendorQuotedSum);
//                        });
//                    }
//                }
//            }
//        }
//        return quotedSumArrayList;
        return lgtVendorQuotedSums;
    }

    @Override
    public void dueImmediately(Long bidingId) {
        Assert.notNull(bidingId,"????????????: bidingId");
        LgtBiding biding = this.getById(bidingId);
        int currentRound = biding.getCurrentRound();
        // ???????????????
        Date endDate = new Date();
        LgtRound lgtRound = iLgtRoundService.getOne(new QueryWrapper<>(new LgtRound().
                setBidingId(bidingId).setRound(currentRound)));
        lgtRound.setEndTime(endDate);
        iLgtRoundService.updateById(lgtRound);
        // ????????????
        biding.setEnrollEndDatetime(endDate);
        biding.setBidingStatus(BiddingProjectStatus.TENDER_ENDING.getValue());
        this.updateById(biding);

        /**
         * ??????????????????
         */
        updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.technicalManagement.getValue())
                .setBidingId(biding.getBidingId()).setDataFlag(YesOrNo.YES.getValue()));
        updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.commercialManagement.getValue())
                .setBidingId(biding.getBidingId()).setDataFlag(YesOrNo.YES.getValue()));
    }

    /**
     * ???????????????????????????
     * @param bidingId
     * @return
     */
    @Override
    public LgtBidInfoVO supplierDetails(Long bidingId) {
        //????????????
        if(Objects.isNull(bidingId)){
            throw new BaseException(LocaleHandler.getLocaleMsg("??????id????????????"));
        }

        //???????????????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if(!UserType.VENDOR.name().equals(loginAppUser.getUserType())){
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????"));
        }
        //???????????????id
        Long companyId = loginAppUser.getCompanyId();
        if(Objects.isNull(companyId)){
            throw new BaseException(LocaleHandler.getLocaleMsg(String.format("??????????????????companyId??????,userName=[%s]",loginAppUser.getUsername())));
        }

        //????????????????????????
        LgtBiding lgtBiding = this.getById(bidingId);

        //????????????????????????
        List<LgtBidFile> bidFileList = iLgtFileService.list(
                new QueryWrapper<LgtBidFile>(new LgtBidFile().setBidingId(bidingId)
        ));

        //???????????????
        List<LgtBidRequirementLine> bidRequirementLineList = iLgtBidRequirementLineService.list(
                new QueryWrapper<LgtBidRequirementLine>(new LgtBidRequirementLine().setBidingId(bidingId))
        );

        //????????????(?????????????????????????????????????????????????????????????????????)
        List<LgtVendorQuotedSum> vendorQuotedSumList = iLgtVendorQuotedSumService.listCurrency(new LgtVendorQuotedSum().setBidingId(bidingId).setVendorId(companyId));

        //?????????????????????
        LgtVendorQuotedInfoVO vendorQuotedInfoVO = new LgtVendorQuotedInfoVO();

        //?????????????????????
        List<LgtVendorQuotedHead> vendorQuotedHeadList = iLgtVendorQuotedHeadService.listCurrency(new LgtVendorQuotedHead().setBidingId(bidingId).setVendorId(companyId));
        LgtVendorQuotedHead vendorQuotedHead = null;
        if(CollectionUtils.isNotEmpty(vendorQuotedHeadList)){
            vendorQuotedHead = vendorQuotedHeadList.get(0);
        }

        //????????????
        List<LgtBidShipPeriod> bidShipPeriodList = new LinkedList<>();
        if(Objects.nonNull(vendorQuotedHead) && Objects.nonNull(vendorQuotedHead.getQuotedHeadId())){
            bidShipPeriodList = iLgtBidShipPeriodService.list(new QueryWrapper<LgtBidShipPeriod>(new LgtBidShipPeriod().setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
        }

        //????????????
        List<LgtVendorFile> vendorFileList = new LinkedList<>();
        if(Objects.nonNull(vendorQuotedHead) && Objects.nonNull(vendorQuotedHead.getQuotedHeadId())){
            vendorFileList = iLgtVendorFileService.list(new QueryWrapper<LgtVendorFile>(new LgtVendorFile().setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
        }

        //?????????????????????
        List<LgtVendorQuotedLine> vendorQuotedLineList = new LinkedList<>();
        if(Objects.nonNull(vendorQuotedHead) && Objects.nonNull(vendorQuotedHead.getQuotedHeadId())){
            vendorQuotedLineList = iLgtVendorQuotedLineService.list(new QueryWrapper<LgtVendorQuotedLine>(new LgtVendorQuotedLine().setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
        }

        vendorQuotedInfoVO.setVendorQuotedHead(vendorQuotedHead)
                .setVendorFileList(vendorFileList)
                .setBidShipPeriodList(bidShipPeriodList)
                .setVendorQuotedLineList(vendorQuotedLineList);

        return new LgtBidInfoVO().setBiding(lgtBiding)
                .setBidfileList(bidFileList)
                .setBidRequirementLineList(bidRequirementLineList)
                .setVendorQuotedInfoList(new LinkedList<LgtVendorQuotedInfoVO>(){{
                    add(vendorQuotedInfoVO);
                }})
                .setVendorQuotedSumList(vendorQuotedSumList);
    }

    /**
     * ???????????????????????????
     * @param bidingId
     * @return
     */
    @Override
    public TechBidVO detailTechBiding(Long bidingId) {
        //??????????????????
        if(Objects.isNull(bidingId)){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????[biddingId]"));
        }
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"????????????????????????!");
        //???????????????????????????????????????
        if(!Objects.equals(lgtBiding.getBidingStatus(),BiddingProjectStatus.TECHNICAL_EVALUATION.getValue())
                && !canSeeResultSet.contains(lgtBiding.getBidingStatus()))
        {
            throw new BaseException("??????????????????????????????????????????");
        }
        //?????????????????????????????????????????????
        List<LgtBidShipPeriod> bidShipPeriodList = iLgtBidShipPeriodService.list(new QueryWrapper<>(new LgtBidShipPeriod().setBidingId(bidingId).setRound(lgtBiding.getCurrentRound())));
        //???????????????????????????
        List<LgtVendorFile> lgtVendorFiles = iLgtVendorFileService.list(new QueryWrapper<>(new LgtVendorFile().
                setBidingId(bidingId).
                setRound(lgtBiding.getCurrentRound()).
                setFileType(BidingFileTypeEnum.TECHNICAL_BID.getCode())));
        // ????????????????????????
        LgtRound lgtRound = iLgtRoundService.getOne(Wrappers.lambdaQuery(LgtRound.class).eq(LgtRound::getBidingId, bidingId).eq(LgtRound::getRound, lgtBiding.getCurrentRound()));
        return new TechBidVO().setLgtVendorFiles(lgtVendorFiles)
                .setBidShipPeriodList(bidShipPeriodList).
                        setTechnoSelection(null != lgtRound?lgtRound.getTechnoSelection():null);
    }

    /**
     * ???????????????????????????
     * @param bidingId
     * @return
     */
    @Override
    public CommercialBidVO detailCommercialBiding(Long bidingId) {
        //??????????????????
        if(Objects.isNull(bidingId)){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????[biddingId]"));
        }
        List<LgtVendorQuotedHead> lgtVendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, bidingId).
                and(wrapper -> wrapper.eq(LgtVendorQuotedHead::getStatus, BiddingOrderStates.SUBMISSION.getValue()).or().
                        eq(LgtVendorQuotedHead::getStatus, BiddingOrderStates.INVALID.getValue())));
        return new CommercialBidVO().setVendorQuotedHeadList(lgtVendorQuotedHeads);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long requirementToBiding(Long id) {
        //????????????
        if(Objects.isNull(id)){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????[requirementHeadId]"));
        }
        LogisticsRequirementHead logisticsRequirementHead = requirementHeadService.getById(id);
        // ??????????????????????????????
        Assert.isTrue(ObjectUtils.isEmpty(Optional.ofNullable(logisticsRequirementHead).orElseThrow(()->new BaseException("????????????????????????")).getBidingId()),String.format("[%s]??????????????????,??????????????????!",logisticsRequirementHead.getRequirementHeadNum()));

        //??????????????????????????????
        LogisticsPurchaseRequirementDTO requirementDTO = requirementHeadService.getByHeadId(id);
        //?????????????????? -> ???????????????
        LgtBidDto lgtBidDto = buildBiding(requirementDTO);
        //???????????????
        Long bidingId = add(lgtBidDto);
        LgtBiding lgtBiding = this.getOne(Wrappers.lambdaQuery(LgtBiding.class).eq(LgtBiding::getRequirementHeadId, id).last("LIMIT 1"));
        // ????????????
        if(null != lgtBiding){
            logisticsRequirementHead
                    .setBidingId(lgtBiding.getBidingId())
                    .setBidingName(lgtBiding.getBidingName())
                    .setBidingCode(lgtBiding.getBidingNum())
                    .setApplyProcessStatus(LogisticsApplyProcessStatus.CONVERT_BID.getValue());
            requirementHeadService.updateById(logisticsRequirementHead);
        }
        return bidingId;
    }

    /**
     * ?????????????????????-???????????????
     * @param lgtBidDto
     * @return
     */
    private Long requirementToBidingAdd(LgtBidDto lgtBidDto, Long requirementHeadId){
        //????????????
        checkParam(lgtBidDto);
        //???????????????
        LgtBiding biding = lgtBidDto.getBiding(); // ??????????????????
        List<LgtFileConfig> bidFileConfigList = lgtBidDto.getBidFileConfigList(); // ????????????????????????
        List<LgtBidFile> fileList = lgtBidDto.getFileList(); // ????????????
        List<LgtGroup> groupList = lgtBidDto.getGroupList(); // ????????????
        List<LgtBidRequirementLine> requirementLineList = lgtBidDto.getRequirementLineList(); //?????????????????????
        // ??????ID
        Long bidingId = IdGenrator.generate();
        biding.setBidingId(bidingId);
        // ??????????????????
        log.info("??????????????????????????????????????????:"+ JSON.toJSONString(biding));
        String requirementNum = getRequirementNum(biding);
        log.info("????????????????????????????????????:"+requirementNum);
        biding.setBidingNum(requirementNum);
        String nickname = AppUserUtil.getLoginAppUser().getNickname();
        // ???????????????
        biding.setAuditStatus(BiddingApprovalStatus.DRAFT.getValue()).
                setBidingStatus(BiddingProjectStatus.DRAW_UP.getValue()).
                setEndAuditStatus(BiddingApprovalStatus.DRAFT.getValue()).
                setReleaseFlag(YesOrNo.NO.getValue()).
                setCreatedByName(nickname).
                setCurrentRound(1);
        // ??????????????????
        if(ObjectUtils.isEmpty(biding.getTemplateFileId())){
            LogisticsTemplateHead templateHead = iLogisticsTemplateHeadService.getById(biding.getTemplateHeadId());
            biding.setTemplateFileId(templateHead.getTemplateFileId());
            biding.setTemplateFileName(templateHead.getTemplateFileName());
        }
        this.save(biding);
        // ??????????????????
        sevaLgtBidFiles(biding, fileList, bidingId);
        // ??????????????????????????????
        saveLgtFileConfigs(biding, bidFileConfigList, bidingId);
        // ??????????????????
        saveLgtGroups(biding, groupList, bidingId);
        // ????????????????????????
        Long processConfigId = biding.getProcessConfigId();
        saveNodes(bidingId,processConfigId);
        // ???????????????????????????
        saveLgtBidTemplates(biding, bidingId);
        // ?????????????????????
        saveLgtBidRequirementLine(biding, requirementLineList);

        //?????????????????????????????????????????????
        LogisticsRequirementHead requirementHead = requirementHeadService.getById(requirementHeadId);
        requirementHeadService.updateById(requirementHead.setApplyProcessStatus(LogisticsApplyProcessStatus.CONVERT_BID.getValue()));
        return bidingId;
    }

    /**
     * ?????????????????????
     * @param biding
     * @param requirementLineList
     */
    private void saveLgtBidRequirementLine(LgtBiding biding, List<LgtBidRequirementLine> requirementLineList){
        Long bidingId = biding.getBidingId();
        iLgtBidRequirementLineService.remove(new QueryWrapper<LgtBidRequirementLine>(new LgtBidRequirementLine().setBidingId(bidingId)));
        if(CollectionUtils.isNotEmpty(requirementLineList)){
            requirementLineList.forEach(item -> {
                item.setBidingId(bidingId);
                item.setBidRequirementLineId(IdGenrator.generate());
            });
            iLgtBidRequirementLineService.saveBatch(requirementLineList);
        }
    }


    @Override
    public LgtBidInfoVO detail(Long id) {
        //????????????
        if(Objects.isNull(id)){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????[bidingId]"));
        }

        //????????????????????????
        LgtBiding biding = this.getById(id);
        //????????????????????????
        List<LgtBidFile> bidFileList = iLgtFileService.list(new QueryWrapper<LgtBidFile>(new LgtBidFile().setBidingId(id)));
        //??????????????????????????????
        List<LgtFileConfig> bidFileConfigList = iLgtFileConfigService.list(new QueryWrapper<LgtFileConfig>(new LgtFileConfig().setBidingId(id)));
        //???????????????
        List<LgtBidRequirementLine> bidRequirementLineList = iLgtBidRequirementLineService.list(new QueryWrapper<LgtBidRequirementLine>(new LgtBidRequirementLine().setBidingId(id)));
        //????????????
        List<LgtVendorQuotedSum> vendorQuotedSumList = iLgtVendorQuotedSumService.list(new QueryWrapper<LgtVendorQuotedSum>(new LgtVendorQuotedSum().setBidingId(id).setRound(biding.getCurrentRound())));
        //?????????????????????
        List<LgtVendorQuotedInfoVO> vendorQuotedInfoList = getVendorQuotedInfoList(id);
        //????????????????????????
        List<LgtVendorVO> vendorList = getVendorList(id);

        return new LgtBidInfoVO().setBiding(biding)
                .setBidfileList(bidFileList)
                .setBidFileConfigList(bidFileConfigList)
                .setBidRequirementLineList(bidRequirementLineList)
                .setVendorQuotedInfoList(vendorQuotedInfoList)
                .setVendorQuotedSumList(vendorQuotedSumList)
                .setVendorList(vendorList);
    }

    /**
     * ???????????????????????????
     * @param id
     * @return
     */
    private List<LgtVendorQuotedInfoVO> getVendorQuotedInfoList(Long id){
        List<LgtVendorQuotedInfoVO> result = new LinkedList<LgtVendorQuotedInfoVO>();
        LgtBiding lgtBiding = this.getById(id);
        List<LgtVendorQuotedHead> vendorQuotedHeadList = iLgtVendorQuotedHeadService.list(new QueryWrapper<LgtVendorQuotedHead>(new LgtVendorQuotedHead().setBidingId(id).setRound(lgtBiding.getCurrentRound())));
        if(CollectionUtils.isNotEmpty(vendorQuotedHeadList)){
            for(LgtVendorQuotedHead vendorQuotedHead : vendorQuotedHeadList){
                List<LgtVendorFile>  vendorFileList = iLgtVendorFileService.list(new QueryWrapper<LgtVendorFile>(new LgtVendorFile().setBidingId(id).setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
                List<LgtBidShipPeriod> bidShipPeriodList = iLgtBidShipPeriodService.list(new QueryWrapper<LgtBidShipPeriod>(new LgtBidShipPeriod().setBidingId(id).setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
                List<LgtVendorQuotedLine> vendorQuotedLineList = iLgtVendorQuotedLineService.list(new QueryWrapper<LgtVendorQuotedLine>(new LgtVendorQuotedLine().setBidingId(id).setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
                result.add(new LgtVendorQuotedInfoVO()
                        .setVendorQuotedHead(vendorQuotedHead)
                        .setVendorFileList(vendorFileList)
                        .setBidShipPeriodList(bidShipPeriodList)
                        .setVendorQuotedLineList(vendorQuotedLineList)
                );
            }
        }
        return result;
    }

    /**
     * ???????????????????????????
     * @param id
     * @return
     */
    private List<LgtVendorVO> getVendorList(Long id){
        List<LgtVendorVO> result = new LinkedList<LgtVendorVO>();
        List<LgtBidVendor> bidVendorList = iLgtVendorService.list(new QueryWrapper<LgtBidVendor>(new LgtBidVendor().setBidingId(id)));
        if(CollectionUtils.isNotEmpty(bidVendorList)){
            for(LgtBidVendor bidVendor : bidVendorList){
                List<LgtQuoteAuthorize> quoteAuthorizeList = iLgtQuoteAuthorizeService.list(new QueryWrapper<LgtQuoteAuthorize>(new LgtQuoteAuthorize().setBidVendorId(bidVendor.getBidVendorId())));
                List<LgtPayPlan> payPlanList = iLgtPayPlanService.list(new QueryWrapper<LgtPayPlan>(new LgtPayPlan().setBidVendorId(bidVendor.getBidVendorId())));
                result.add(new LgtVendorVO()
                        .setBidVendor(bidVendor)
                        .setQuoteAuthorizeList(quoteAuthorizeList)
                        .setPayPlanList(payPlanList)
                );
            }
        }
        return result;
    }

    private LgtBidDto buildBiding(LogisticsPurchaseRequirementDTO requirementDTO){
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();

        //????????????????????????????????????
        List<BidProcessConfig> bidProcessConfigs = bidClient.listBidProcessConfig(new BidProcessConfig().setBidingType(BidType.LOGISTICS.getValue()));
        Long processConfigId = null;
        if(CollectionUtils.isNotEmpty(bidProcessConfigs)){
            processConfigId = bidProcessConfigs.get(0).getProcessConfigId();
        }

        //???????????????
        LogisticsRequirementHead requirementHead = requirementDTO.getRequirementHead();
        //?????????????????????
        LogisticsTemplateHead templateHead = requirementDTO.getLogisticsTemplateHead();

        //????????????????????????
        LgtBiding biding = new LgtBiding()
                .setBidingNum(requirementHead.getRequirementHeadNum())
                .setProcessConfigId(processConfigId)       //??????????????????id
                .setTemplateHeadId(requirementHead.getTemplateHeadId())        //???????????????????????????ID
                .setTemplateCode(requirementHead.getTemplateCode())          //????????????
                .setTemplateName(requirementHead.getTemplateName())          //????????????
                .setBusinessModeCode(requirementHead.getBusinessModeCode())      //????????????
                .setTransportModeCode(requirementHead.getTransportModeCode())     //????????????
                .setBusinessType(requirementHead.getBusinessType())          //????????????
                .setServiceProjectCode(requirementHead.getServiceProjectCode())    //??????????????????
                .setServiceProjectName(requirementHead.getServiceProjectName())    //??????????????????
//                .setUnitId()                //??????ID
                .setUnitCode(requirementHead.getUnit())              //????????????
//                .setUnitName()              //????????????
                .setProjectTotal(requirementHead.getProjectTotal())          //????????????
                .setBidingName(requirementHead.getRequirementTitle())            //????????????
                .setDemandDate(requirementHead.getDemandDate())            //????????????
                .setBudgetAmount(requirementHead.getBudgetAmount())          //????????????
                .setCurrencyId(requirementHead.getCurrencyId())            //??????ID
                .setCurrencyCode(requirementHead.getCurrencyCode())          //????????????
                .setCurrencyName(requirementHead.getCurrencyName())          //????????????
                .setAllowedVehicle(requirementHead.getAllowedVehicle())        //???????????????????????????
                .setPriceTimeStart(requirementHead.getPriceStartDate())        //????????????????????????
                .setPriceTimeEnd(requirementHead.getPriceEndDate())          //????????????????????????
                .setLoadNumber(String.valueOf(requirementHead.getLoadNumber()))            //?????????
//                .setEnrollEndDatetime()     //??????????????????
//                .setTaxId()                 //??????ID
//                .setTaxCode()               //?????????
//                .setTaxKey()                //????????????
                .setIfVendorSubmitShipDate(requirementHead.getIfVendorSubmitShipDate())//??????????????????(N/Y)
                .setContractType(requirementHead.getContractType())          //????????????
                .setCompanyId(requirementHead.getVendorId())             //???????????????ID
                .setCompanyCode(requirementHead.getVendorCode())           //???????????????CODE
                .setCompanyName(requirementHead.getVendorName())           //?????????????????????
                .setSpecifySupReason(requirementHead.getVendorReason())      //?????????????????????
                .setComments(requirementHead.getComments())              //??????
                .setRequirementHeadId(requirementHead.getRequirementHeadId())  // ??????ID
                .setRequirementHeadNum(requirementHead.getRequirementHeadNum())    //????????????
                .setApplyId(requirementHead.getApplyId())               //?????????ID
                .setApplyCode(requirementHead.getApplyCode())             //???????????????
                .setApplyBy(requirementHead.getApplyBy())               //???????????????
                .setApplyDepartmentId(requirementHead.getApplyDepartmentId())     //????????????ID
                .setApplyDepartmentCode(requirementHead.getApplyDepartmentCode())   //??????????????????
                .setApplyDepartmentName(requirementHead.getApplyDepartmentName())   //??????????????????
                .setPhone(requirementHead.getPhone())                 //????????????
//                .setCreatedByName()         //???????????????
//                .setInternalDesc()          //????????????
//                .setSupplierDesc()          //????????????
                .setStandardCurrency(requirementHead.getCurrencyCode())      //?????????
//                .setPricePrecision()        //????????????
//                .setExchangeRateType()      //????????????
//                .setCurrencyChangeDate()    //??????????????????
                .setBidUserName(requirementHead.getCeeaApplyUserNickname())           //???????????????
//                .setBidUserPhone()          //??????
//                .setBidUserEmail()          //??????
                .setWithdrawBiding(YesOrNo.YES.getValue())        //????????????????????????????????????????????????
//                .setPublicTotalRank()       //??????????????????????????????????????????
//                .setPublicLowestPrice()     //???????????????????????????????????????
//                .setVisibleRankResult()     //???????????????????????????????????????
//                .setVisibleFinalPrice()     //??????????????????????????????
//                .setVisibleWinVendor()      //????????????????????????????????????
//                .setRevokeReason()          //????????????
//                .setReleaseDatetime()       //????????????
//                .setBiddingSuppliers()      //??????????????????
                .setIfNeedVendorComfirm(templateHead.getVendorIfSubmitShip())    //????????????????????????
                .setTemplateFileId(templateHead.getTemplateFileId())
                .setTemplateFileName(templateHead.getTemplateFileName())
                .setGeneratePurchaseApproval(YesOrNo.NO.getValue())  //??????????????????(Y/N)
                .setBidUserPhone(loginAppUser.getPhone())
                .setBidUserEmail(loginAppUser.getEmail())
                .setBidUserName(loginAppUser.getNickname())
                //??????????????????
                .setSourceFrom(SourceFrom.PURCHASE_REQUEST.getItemValue())
                //???????????????
                .setStandardCurrency("CNY")
                .setExchangeRateType("COMPANY")
                .setPricePrecision(2)
                .setCurrencyChangeDate(LocalDate.now());


        //??????????????????
        List<LgtBidRequirementLine> requirementLineList = new LinkedList<>();
        List<LogisticsRequirementLine> logisticsRequirementLineList = requirementDTO.getRequirementLineList();
        for(LogisticsRequirementLine item : logisticsRequirementLineList){
            LgtBidRequirementLine requirementLine = new LgtBidRequirementLine();
            BeanUtils.copyProperties(item, requirementLine);
            requirementLine.setRequirementHeadId(requirementHead.getRequirementHeadId());
            requirementLine.setRequirementLineId(item.getRequirementLineId());
            requirementLine.setRequirementLineNum((long)item.getRowNum());
            requirementLineList.add(requirementLine);
        }

        //11954 ????????????????????????????????????
        //????????????????????????
        List<LogisticsRequirementFile> requirementAttaches = requirementDTO.getRequirementAttaches();
        List<LgtBidFile> lgtBidFiles=new ArrayList<>(requirementAttaches.size());
        if(!CollectionUtils.isEmpty(requirementAttaches)){
            for (LogisticsRequirementFile requirementAttach : requirementAttaches) {
                LgtBidFile lgtBidFile=new LgtBidFile()
                        .setFileType(BidFileType.Enterprise.getValue())
                        .setFileName(requirementAttach.getFileName())
                        .setDocId(requirementAttach.getFileRelationId()+"")
                        .setComments(requirementAttach.getRemark());
                lgtBidFiles.add(lgtBidFile);
            }
        }
        User user = rbacClient.getUserByIdAnon(requirementHead.getCreatedId());
        User user1=rbacClient.getUserByIdAnon(loginAppUser.getUserId());
        //??????????????????????????????
        List<LgtGroup> defaultGroup = Arrays.asList(new LgtGroup().setConfirmeDatetime(new Date())
                .setConfirmedFlag("Y")
                .setUserName(user.getUsername())
                .setUserId(user.getUserId())
                .setPhone(user.getPhone())
                .setEmail(user.getEmail())
                .setPosition(user.getCeeaJobcodeDescr())
                .setJudgeFlag("Y")
                .setFullName(user.getNickname())
                .setMaxEvaluateScore(100L),
        new LgtGroup().setConfirmeDatetime(new Date())
                .setConfirmedFlag("Y")
                .setUserName(user1.getUsername())
                .setUserId(user1.getUserId())
                .setPhone(user1.getPhone())
                .setEmail(user1.getEmail())
                .setPosition(user1.getCeeaJobcodeDescr())
                .setFullName(user1.getNickname())
                .setMaxEvaluateScore(100L)
        );
        return new LgtBidDto()
                .setBiding(biding)
                .setRequirementLineList(requirementLineList)
                .setBidFileConfigList(Collections.EMPTY_LIST)
                .setFileList(lgtBidFiles)
                .setGroupList(defaultGroup);

    }

    @Override
    @Transactional
    public void extendBiding(StartOrExtendBidingVO startOrExtendBidingVO) {
        Long bidingId = startOrExtendBidingVO.getBidingId();
        Assert.notNull(bidingId,"????????????: bidingId");
        Assert.notNull(startOrExtendBidingVO.getExtendReason(),"????????????????????????!");
        Date endTime = startOrExtendBidingVO.getEndTime();
        Assert.notNull(endTime,"????????????????????????");
        Assert.isTrue(endTime.compareTo(new Date()) > 0 ,"????????????????????????????????????");
        LgtBiding biding = this.getById(bidingId);
        Assert.isTrue(BiddingProjectStatus.ACCEPT_BID.getValue().equals(biding.getBidingStatus()) ||
                        BiddingProjectStatus.TENDER_ENDING.getValue().equals(biding.getBidingStatus()) ||
                        BiddingProjectStatus.BUSINESS_EVALUATION.getValue().equals(biding.getBidingStatus()),
                "??????????????????\"???????????????\"???\"????????????\",??????????????????????????????!");
        /**
         * ??????????????????
         */
        int currentRound = biding.getCurrentRound();
        // ???????????????
        LgtRound lgtRound = iLgtRoundService.getOne(new QueryWrapper<>(new LgtRound().
                setBidingId(bidingId).setRound(currentRound)));
        lgtRound.setEndTime(endTime);
        lgtRound.setExtendReason(startOrExtendBidingVO.getExtendReason());
        iLgtRoundService.updateById(lgtRound);
        // ????????????
        biding.setEnrollEndDatetime(endTime);
        biding.setBidingStatus(BiddingProjectStatus.ACCEPT_BID.getValue());
        this.updateById(biding);
        // ??????????????????
        iLgtVendorQuotedSumService.remove(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                eq(LgtVendorQuotedSum::getBidingId,bidingId).
                eq(LgtVendorQuotedSum::getRound,currentRound));
    }

    @Override
    @Transactional
    public void startBiding(StartOrExtendBidingVO startOrExtendBidingVO) {
        /**
         * 1. ???????????????????????????????????????????????????????????????????????????????????????
         * 2. ???????????????????????????????????????????????????
         * 3. ?????????????????????????????????????????????????????????
         */
        Long bidingId = startOrExtendBidingVO.getBidingId();
        Date endTime = startOrExtendBidingVO.getEndTime();
        Assert.notNull(bidingId, "??????id????????????");
        Assert.notNull(endTime, "??????????????????????????????");
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"??????????????????????????????");
        Assert.isTrue(endTime.compareTo(new Date()) > 0,"??????????????????????????????????????????!");
        Assert.isTrue(!BiddingApprovalStatus.APPROVED.getValue().equals(lgtBiding.getEndAuditStatus()),"????????????????????????,???????????????????????????!");
        // ???????????????????????? ??????
        LgtRound lgtRound = iLgtRoundService.getOne(Wrappers.lambdaQuery(LgtRound.class).
                eq(LgtRound::getBidingId, bidingId).
                eq(LgtRound::getRound, lgtBiding.getCurrentRound()));
        Assert.notNull(lgtRound,"???????????????????????????");
        Assert.isTrue(YesOrNo.YES.getValue().equals(lgtRound.getPublicResult()),"?????????????????????,???????????????????????????!");
        // ??????????????????????????????????????????
        List<LgtVendorQuotedSum> vendorQuotedSums = iLgtVendorQuotedSumService.list(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                select(LgtVendorQuotedSum::getVendorId).
                eq(LgtVendorQuotedSum::getRound, lgtBiding.getCurrentRound()).
                eq(LgtVendorQuotedSum::getBidingId, bidingId).
                eq(LgtVendorQuotedSum::getShortlisted, YesOrNo.YES.getValue()));
        Assert.isTrue(CollectionUtils.isNotEmpty(vendorQuotedSums),"??????????????????????????????????????????,???????????????????????????!");

        int round = lgtBiding.getCurrentRound() + 1;
        // ????????????????????????
        saveLgtRoundNew(bidingId, endTime, round);
        // ????????????????????????????????????????????????
        saveLgtVendorQuotedNew(bidingId, lgtBiding, vendorQuotedSums, round);
        // ????????????????????????
        updateLgtBidingNew(endTime, lgtBiding, vendorQuotedSums, round);
    }

    public void updateLgtBidingNew(Date endTime, LgtBiding lgtBiding, List<LgtVendorQuotedSum> vendorQuotedSums, int round) {
        int size = vendorQuotedSums.size();
        String biddingSuppliers = "0/"+size;
        lgtBiding.setBidingStatus(BiddingProjectStatus.ACCEPT_BID.getValue())
                .setEnrollEndDatetime(endTime)
                .setCurrentRound(round)
                .setBiddingSuppliers(biddingSuppliers);
        this.updateById(lgtBiding);
    }

    private void saveLgtRoundNew(Long bidingId, Date endTime, int round) {
        iLgtRoundService.remove(Wrappers.lambdaQuery(LgtRound.class).eq(LgtRound::getBidingId,bidingId).eq(LgtRound::getRound,round));
        LgtRound lgtRound = LgtRound.builder()
                .roundId(IdGenrator.generate())
                .bidingId(bidingId)
                .round(round)
                .startTime(new Date())
                .endTime(endTime)
                .build();
        iLgtRoundService.save(lgtRound);
    }

    private void saveLgtVendorQuotedNew(Long bidingId, LgtBiding lgtBiding, List<LgtVendorQuotedSum> vendorQuotedSums, int round) {
        List<LgtVendorFile> lgtVendorFileSave = new ArrayList<>();
        List<LgtBidShipPeriod> lgtBidShipPeriodSave = new ArrayList<>();
        List<LgtVendorQuotedLine> lgtVendorQuotedLineSave = new ArrayList<>();
        List<Long> vendorIds = vendorQuotedSums.stream().map(LgtVendorQuotedSum::getVendorId).collect(Collectors.toList());
        // ????????????????????????
        List<LgtVendorQuotedHead> vendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, bidingId).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).
                in(LgtVendorQuotedHead::getVendorId, vendorIds));
        vendorQuotedHeads.forEach(lgtVendorQuotedHead -> {
            Long oldQuotedHeadId = lgtVendorQuotedHead.getQuotedHeadId();
            Long quotedHeadId = IdGenrator.generate();
            lgtVendorQuotedHead.setQuotedHeadId(quotedHeadId)
                    .setStatus(BiddingOrderStates.DRAFT.getValue())
                    .setQuotedHeadCode(baseClient.seqGen(SequenceCodeConstant.SEQ_BID_BIDING_CODE))
                    .setRound(round);
            // ?????????????????????
            List<LgtVendorFile> lgtVendorFiles = iLgtVendorFileService.list(Wrappers.lambdaQuery(LgtVendorFile.class).
                    eq(LgtVendorFile::getQuotedHeadId, oldQuotedHeadId));
            if (CollectionUtils.isNotEmpty(lgtVendorFiles)) {
                lgtVendorFiles.forEach(lgtVendorFile -> {
                    lgtVendorFile.setQuotedHeadId(quotedHeadId);
                    lgtVendorFile.setRound(round);
                    lgtVendorFile.setVendorFileId(IdGenrator.generate());
                });
                lgtVendorFileSave.addAll(lgtVendorFiles);
            }
            // ?????????????????????
            List<LgtBidShipPeriod> lgtBidShipPeriods = iLgtBidShipPeriodService.list(Wrappers.lambdaQuery(LgtBidShipPeriod.class).
                    eq(LgtBidShipPeriod::getQuotedHeadId, oldQuotedHeadId));
            if (CollectionUtils.isNotEmpty(lgtBidShipPeriods)) {
                lgtBidShipPeriods.forEach(lgtBidShipPeriod -> {
                    lgtBidShipPeriod.setQuotedHeadId(quotedHeadId);
                    lgtBidShipPeriod.setRound(round);
                    lgtBidShipPeriod.setShipPeriodId(IdGenrator.generate());
                });
                lgtBidShipPeriodSave.addAll(lgtBidShipPeriods);
            }
            // ?????????????????????
            List<LgtVendorQuotedLine> lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                    eq(LgtVendorQuotedLine::getQuotedHeadId, oldQuotedHeadId));
            if (CollectionUtils.isNotEmpty(lgtVendorQuotedLines)) {
                lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                    lgtVendorQuotedLine.setQuotedLineId(IdGenrator.generate())
                            .setRound(round)
                            .setQuotedHeadId(quotedHeadId);
                });
                lgtVendorQuotedLineSave.addAll(lgtVendorQuotedLines);
            }

        });
        iLgtVendorQuotedHeadService.saveBatch(vendorQuotedHeads);
        iLgtVendorFileService.saveBatch(lgtVendorFileSave);
        iLgtBidShipPeriodService.saveBatch(lgtBidShipPeriodSave);
        iLgtVendorQuotedLineService.saveBatch(lgtVendorQuotedLineSave);
    }

    @Override
    @Transactional
    public void delete(List<Long> bidingIds) {
        if(CollectionUtils.isNotEmpty(bidingIds)){
            List<LgtBiding> lgtBidings = this.listByIds(bidingIds);
            if(CollectionUtils.isNotEmpty(lgtBidings)){
                List<String> stringList = lgtBidings.stream().map(LgtBiding::getRequirementHeadNum).collect(Collectors.toList());
                iRequirementHeadService.release(stringList);
            }
            this.removeByIds(bidingIds);
        }
    }

    @Override
    public void withdraw(Long bidingId,String reason) {
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"?????????????????????????????????");
        //  ????????????/ ??????-??????
        String revokeReason = lgtBiding.getRevokeReason();
        if(ObjectUtils.isEmpty(revokeReason)){
            List<Map<String, String>> list = new ArrayList<>();
            Map<String, String> hashMap = new HashMap<>();
            hashMap.put("reason",reason);
            hashMap.put("time", DateUtil.format(new Date()));
            list.add(hashMap);
            revokeReason = JSON.toJSONString(list);
        }else {
            List list = JSON.parseObject(revokeReason, List.class);
            Map<String, String> hashMap = new HashMap<>();
            hashMap.put("reason",reason);
            hashMap.put("time",DateUtil.format(new Date()));
            list.add(hashMap);
            revokeReason = JSON.toJSONString(list);
        }
        lgtBiding.setRevokeReason(revokeReason);
//        lgtBiding.setRevokeReason(reason);
        // ??????/??????

        lgtBiding.setAuditStatus(BiddingApprovalStatus.WITHDRAW.getValue()).
                setBidingStatus(BiddingProjectStatus.DRAW_UP.getValue()).
                setEndAuditStatus(BiddingApprovalStatus.DRAFT.getValue()).
                setReleaseDatetime(null).
                setReleaseFlag(YesOrNo.NO.getValue());
        this.updateById(lgtBiding);
    }

    @Override
    @Transactional
    public void release(Long bidingId) {
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"?????????????????????????????????");
        Assert.isTrue(
                !BiddingApprovalStatus.SUBMITTED.getValue().equals(lgtBiding.getAuditStatus()) &&
                        !BiddingApprovalStatus.APPROVED.getValue().equals(lgtBiding.getAuditStatus())
                ,"??????????????????!");
        // ????????????
        releaseCheckParam(bidingId);
        Assert.isTrue(lgtBiding.getEnrollEndDatetime().compareTo(new Date()) > 0,"????????????????????????????????????, ????????????!");
        // ??????????????????
        releaseUpdateStatus(lgtBiding);
        // ????????????????????????
        createLgtVendorQuotedHead(bidingId, lgtBiding);
        // ???????????????
        saveLgtRound(bidingId, lgtBiding);
        // ????????????????????????
        updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.bidingControl.getValue())
                .setBidingId(bidingId).setDataFlag(YesOrNo.YES.getValue()));
    }

    private void saveLgtRound(Long bidingId, LgtBiding lgtBiding) {
        iLgtRoundService.remove(Wrappers.lambdaQuery(LgtRound.class).eq(LgtRound::getBidingId,bidingId).eq(LgtRound::getRound,1));
        LgtRound lgtRound = LgtRound.builder()
                .roundId(IdGenrator.generate())
                .bidingId(bidingId)
                .round(1)
                .startTime(new Date())
                .endTime(lgtBiding.getEnrollEndDatetime())
                .build();
        iLgtRoundService.save(lgtRound);
    }

    private void releaseCheckParam(Long bidingId) {
        int bidRequirementLineCount = iLgtBidRequirementLineService.count(new QueryWrapper<>(new LgtBidRequirementLine().setBidingId(bidingId)));
        Assert.isTrue(bidRequirementLineCount > 0,"????????????????????????!!!");
        List<LgtBidVendor> lgtBidVendors = iLgtVendorService.list(new QueryWrapper<>(new LgtBidVendor().setBidingId(bidingId)));
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtBidVendors),"???????????????????????????!!!");
        lgtBidVendors.forEach(lgtBidVendor -> {
            Assert.isTrue(!ObjectUtils.isEmpty(lgtBidVendor.getEmail()) &&
                    !ObjectUtils.isEmpty(lgtBidVendor.getPhone()) &&
                    !ObjectUtils.isEmpty(lgtBidVendor.getLinkManName()),"???????????????????????????????????????!!!");
        });
    }

    private void releaseUpdateStatus(LgtBiding lgtBiding) {
        lgtBiding.setAuditStatus(BiddingApprovalStatus.APPROVED.getValue()).
                setBidingStatus(BiddingProjectStatus.ACCEPT_BID.getValue()).
                setEndAuditStatus(BiddingApprovalStatus.DRAFT.getValue()).
                setReleaseDatetime(new Date()).
                setReleaseFlag(YesOrNo.YES.getValue());
        this.updateById(lgtBiding);
    }

    private void createLgtVendorQuotedHead(Long bidingId, LgtBiding lgtBiding) {
        List<LgtBidVendor> lgtBidVendors = iLgtVendorService.list(Wrappers.lambdaQuery(LgtBidVendor.class).
                eq(LgtBidVendor::getBidingId, bidingId));
        if(CollectionUtils.isNotEmpty(lgtBidVendors)){
            iLgtVendorQuotedHeadService.remove(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).eq(LgtVendorQuotedHead::getBidingId,bidingId));
            List<LgtVendorQuotedHead> lgtVendorQuotedHeads = new ArrayList<>();
            lgtBidVendors.forEach(lgtBidVendor -> {
                LgtVendorQuotedHead quotedHead = LgtVendorQuotedHead.builder()
                        .quotedHeadId(IdGenrator.generate())
                        .bidingId(bidingId)
                        .round(lgtBiding.getCurrentRound())
                        .quotedHeadCode(baseClient.seqGen(SequenceCodeConstant.SEQ_BID_BIDING_CODE))
                        .vendorId(lgtBidVendor.getVendorId())
                        .vendorCode(lgtBidVendor.getVendorCode())
                        .vendorName(lgtBidVendor.getVendorName())
                        .bidingNum(lgtBiding.getBidingNum())
                        .linkManName(lgtBidVendor.getLinkManName())
                        .phone(lgtBidVendor.getPhone())
                        .email(lgtBidVendor.getEmail())
                        .status(BiddingOrderStates.DRAFT.getValue())
                        .ifProxy(YesOrNo.NO.getValue()).build();
                lgtVendorQuotedHeads.add(quotedHead);
            });
            iLgtVendorQuotedHeadService.saveBatch(lgtVendorQuotedHeads);
        }
    }

    @Override
    public List<LgtVendorQuotedDto> previewVendor(Long bidingId) {
        LgtBiding lgtBiding = this.getById(bidingId);
        Assert.notNull(lgtBiding,"??????????????????????????????");
        /**
         * ???????????????????????????,???????????????????????????
         */
        List<LgtVendorQuotedDto> lgtVendorQuotedDtos = new ArrayList<>();
        if (!Objects.equals(lgtBiding.getAuditStatus(), BiddingApprovalStatus.DRAFT.getValue())) {
            /**
             * ??????????????????, ???????????????????????????
             */
            List<LgtVendorQuotedHead> lgtVendorQuotedHeads = iLgtVendorQuotedHeadService.list(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                    eq(LgtVendorQuotedHead::getBidingId, bidingId).eq(LgtVendorQuotedHead::getRound,lgtBiding.getCurrentRound()));
            if(CollectionUtils.isNotEmpty(lgtVendorQuotedHeads)){
                lgtVendorQuotedHeads.forEach(lgtBidVendor -> {
                    LgtVendorQuotedDto lgtVendorQuotedDto = new LgtVendorQuotedDto();
                    BeanCopyUtil.copyProperties(lgtVendorQuotedDto,lgtBidVendor);
                    lgtVendorQuotedDtos.add(lgtVendorQuotedDto);
                });
            }
        }
//        else {
//            /**
//             * ??????????????????, ???????????????
//             */
//            List<LgtBidVendor> lgtBidVendors = iLgtVendorService.list(Wrappers.lambdaQuery(LgtBidVendor.class).eq(LgtBidVendor::getBidingId, bidingId));
//            if(CollectionUtils.isNotEmpty(lgtBidVendors)){
//                lgtBidVendors.forEach(lgtBidVendor -> {
//                    LgtVendorQuotedDto lgtVendorQuotedDto = new LgtVendorQuotedDto();
//                    BeanCopyUtil.copyProperties(lgtVendorQuotedDto,lgtBidVendor);
//                    lgtVendorQuotedDto.setStatus(BiddingOrderStates.DRAFT.getValue());
//                    lgtVendorQuotedDtos.add(lgtVendorQuotedDto);
//                });
//            }
//        }
        return lgtVendorQuotedDtos;
    }

    @Override
    @Transactional
    public void saveLgtQuoteAuthorize(List<LgtQuoteAuthorize> lgtQuoteAuthorizes) {
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtQuoteAuthorizes),"????????????????????????");
        Long bidVendorId = lgtQuoteAuthorizes.get(0).getBidVendorId();
        Assert.notNull(bidVendorId,"????????????????????????: bidVendorId");
        LgtBidVendor lgtBidVendor = iLgtVendorService.getById(bidVendorId);
        Assert.notNull(lgtBidVendor,"??????????????????????????????");
        LgtBiding lgtBiding = this.getById(lgtBidVendor.getBidingId());
        if(!Objects.equals(lgtBiding.getAuditStatus(), BiddingApprovalStatus.APPROVED.getValue())){
            iLgtQuoteAuthorizeService.updateBatchById(lgtQuoteAuthorizes);
        }
    }

    @Override
    public List<LgtProcessNode> getNodeStatus(Long bidingId) {
        List<LgtProcessNode> lgtProcessNodes = iLgtProcessNodeService.list(Wrappers.lambdaQuery(LgtProcessNode.class).
                eq(LgtProcessNode::getBidingId, bidingId));
        return lgtProcessNodes;
    }

    @Override
    @Transactional
    public void updateNodeStatus(LgtProcessNode processNode) {
        // ProcessNodeName
        LgtProcessNode processNodeEntity = iLgtProcessNodeService.getOne(new QueryWrapper<>(new LgtProcessNode().
                setBidingId(processNode.getBidingId()).
                setNodeCode(processNode.getNodeCode())));
        if (null != processNodeEntity) {
            processNodeEntity.setDataFlag(processNode.getDataFlag());
            iLgtProcessNodeService.updateById(processNodeEntity);
        }
    }

    @Override
    @Transactional
    public List<LgtQuoteAuthorize> getLgtQuoteAuthorizeByBidVendorId(Long bidingId,Long bidVendorId) {
        List<LgtQuoteAuthorize> quoteAuthorizes = new ArrayList<>();
        LgtBiding biding = this.getById(bidingId);
        LgtBidVendor lgtBidVendor = iLgtVendorService.getById(bidVendorId);
        List<LgtBidRequirementLine> requirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).
                eq(LgtBidRequirementLine::getBidingId, bidingId));
        List<LgtQuoteAuthorize> quoteAuthorizesTemp = iLgtQuoteAuthorizeService.list(Wrappers.lambdaUpdate(LgtQuoteAuthorize.class).
                eq(LgtQuoteAuthorize::getBidVendorId, bidVendorId));
        /**
         * ?????????,???????????????????????????
         */
        if(CollectionUtils.isEmpty(quoteAuthorizesTemp)){
            if(CollectionUtils.isNotEmpty(requirementLines)){
                List<LgtQuoteAuthorize> finalQuoteAuthorizes = getLgtQuoteAuthorizes(bidVendorId, biding, lgtBidVendor, requirementLines);
                if (CollectionUtils.isNotEmpty(finalQuoteAuthorizes)) {
                    quoteAuthorizes.addAll(finalQuoteAuthorizes);
                }
                iLgtQuoteAuthorizeService.saveBatch(quoteAuthorizes);
            }
        }else {
            /**
             * ?????????,??????????????????,??????????????????????????????????????????
             */
            if(CollectionUtils.isNotEmpty(requirementLines)){
                Map<Long, String> map = quoteAuthorizesTemp.stream().collect(Collectors.toMap(LgtQuoteAuthorize::getBidRequirementLineId, LgtQuoteAuthorize::getIfProhibit));
                List<LgtQuoteAuthorize> finalQuoteAuthorizes = getLgtQuoteAuthorizes(bidVendorId, biding, lgtBidVendor, requirementLines);
                if(CollectionUtils.isNotEmpty(finalQuoteAuthorizes)){
                    finalQuoteAuthorizes.forEach(lgtQuoteAuthorize -> {
                        Long bidRequirementLineId = lgtQuoteAuthorize.getBidRequirementLineId();
                        lgtQuoteAuthorize.setIfProhibit(null != map.get(bidRequirementLineId)?map.get(bidRequirementLineId):YesOrNo.NO.getValue());
                    });
                    quoteAuthorizes.addAll(finalQuoteAuthorizes);
                }
                iLgtQuoteAuthorizeService.remove(Wrappers.lambdaQuery(LgtQuoteAuthorize.class).eq(LgtQuoteAuthorize::getBidVendorId,bidVendorId));
                iLgtQuoteAuthorizeService.saveBatch(quoteAuthorizes);
            }
        }
        return quoteAuthorizes;
    }

    private List<LgtQuoteAuthorize> getLgtQuoteAuthorizes(Long bidVendorId, LgtBiding biding, LgtBidVendor lgtBidVendor, List<LgtBidRequirementLine> requirementLines) {
        List<LgtQuoteAuthorize> finalQuoteAuthorizes = new ArrayList<>();
        requirementLines.forEach(lgtBidRequirementLine -> {
            LgtQuoteAuthorize lgtQuoteAuthorize = LgtQuoteAuthorize.builder()
                    .quoteAuthorizeId(IdGenrator.generate())
                    .bidVendorId(bidVendorId)
                    .bidRequirementLineId(lgtBidRequirementLine.getRequirementLineId())
                    .rowNum(lgtBidRequirementLine.getRowNum())
                    .vendorId(lgtBidVendor.getVendorId())
                    .vendorCode(lgtBidVendor.getVendorCode())
                    .vendorName(lgtBidVendor.getVendorName())
                    .bidingName(biding.getBidingName())
                    .ifProhibit(YesOrNo.NO.getValue())
                    .fromPlace(lgtBidRequirementLine.getFromPlace())
                    .toPlace(lgtBidRequirementLine.getToPlace())
                    .build();
            finalQuoteAuthorizes.add(lgtQuoteAuthorize);
        });
        return finalQuoteAuthorizes;
    }

    @Override
    @Transactional
    public void saveLgtBidVendor(List<LgtBidVendor> lgtBidVendors) {
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtBidVendors),"???????????????????????????");
        Set<String> vendorNameHash = new HashSet<>();
        lgtBidVendors.forEach(lgtBidVendor -> {
            Assert.isTrue(vendorNameHash.add(lgtBidVendor.getVendorCode()),"???????????????????????????!");
        });
        Long bidingId = lgtBidVendors.get(0).getBidingId();
        Assert.notNull(bidingId,"???????????????????????????: bidingId");
        LgtBiding biding = this.getById(bidingId);
        Assert.notNull(biding,"??????????????????????????????");
        if(!Objects.equals(biding.getAuditStatus(), BiddingApprovalStatus.APPROVED.getValue())){
            // ?????????????????????????????????
            ArrayList<LgtBidVendor> lgtBidVendorsUpdate = new ArrayList<>();
            ArrayList<LgtBidVendor> lgtBidVendorsAdd = new ArrayList<>();
            lgtBidVendors.forEach(lgtBidVendor -> {
                if(ObjectUtils.isEmpty(lgtBidVendor.getBidVendorId())){
                    lgtBidVendorsAdd.add(lgtBidVendor);
                }else {
                    lgtBidVendorsUpdate.add(lgtBidVendor);
                }
            });
            if (CollectionUtils.isNotEmpty(lgtBidVendorsUpdate)) {
                List<LgtBidVendor> bidingIds = iLgtVendorService.list(Wrappers.lambdaQuery(LgtBidVendor.class).select(LgtBidVendor::getBidingId).eq(LgtBidVendor::getBidingId, bidingId));
                if (CollectionUtils.isNotEmpty(bidingIds)) {
                    List<Long> ids = lgtBidVendorsUpdate.stream().map(LgtBidVendor::getBidVendorId).collect(Collectors.toList());
                    List<Long> longs = new ArrayList<>();
                    bidingIds.forEach(lgtBidVendor -> {
                        if(!ids.contains(lgtBidVendor.getBidVendorId())){
                            longs.add(lgtBidVendor.getBidVendorId());
                        }
                    });
                    if(CollectionUtils.isNotEmpty(longs)){
                        iLgtVendorService.remove(Wrappers.lambdaQuery(LgtBidVendor.class).in(LgtBidVendor::getBidVendorId,longs));
                    }
                }

            }else {
                iLgtVendorService.remove(Wrappers.lambdaQuery(LgtBidVendor.class).eq(LgtBidVendor::getBidingId,bidingId));
            }


            List<LgtPayPlan> lgtPayPlansUpdate = new ArrayList<>();
            List<LgtPayPlan> lgtPayPlansAdd = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(lgtBidVendorsUpdate)) {
                // ????????????
                lgtBidVendorsUpdate.forEach(lgtBidVendor -> {
                    // ????????????
                    List<LgtPayPlan> lgtPayPlans = lgtBidVendor.getLgtPayPlans();
                    if(CollectionUtils.isNotEmpty(lgtPayPlans)){
                        lgtPayPlans.forEach(lgtPayPlan -> {
                            if(ObjectUtils.isEmpty(lgtPayPlan.getPayPlanId())){
                                lgtPayPlan.setBidVendorId(lgtBidVendor.getBidVendorId());
                                lgtPayPlan.setPayPlanId(IdGenrator.generate());
                                lgtPayPlansAdd.add(lgtPayPlan);
                            }else {
                                lgtPayPlansUpdate.add(lgtPayPlan);
                            }
                        });
                    }
                    // ????????????
                    List<LgtQuoteAuthorize> lgtQuoteAuthorizes = lgtBidVendor.getLgtQuoteAuthorizes();
                    if (CollectionUtils.isNotEmpty(lgtQuoteAuthorizes)) {
                        iLgtQuoteAuthorizeService.updateBatchById(lgtQuoteAuthorizes);
                    }
                });
                iLgtVendorService.updateBatchById(lgtBidVendorsUpdate);
            }

            // ????????????
            if (CollectionUtils.isNotEmpty(lgtBidVendorsAdd)) {
                lgtBidVendorsAdd.forEach(lgtBidVendor -> {
                    lgtBidVendor.setBidingId(bidingId);
                    lgtBidVendor.setBidVendorId(IdGenrator.generate());
                    lgtBidVendor.setJoinFlag(YesOrNo.NO.getValue());
                    List<LgtPayPlan> lgtPayPlans = lgtBidVendor.getLgtPayPlans();
                    if(CollectionUtils.isNotEmpty(lgtPayPlans)){
                        lgtPayPlans.forEach(lgtPayPlan -> {
                            lgtPayPlan.setBidVendorId(lgtBidVendor.getBidVendorId());
                            lgtPayPlan.setPayPlanId(IdGenrator.generate());
                            lgtPayPlansAdd.add(lgtPayPlan);
                        });
                    }
                    // ????????????
                    List<LgtQuoteAuthorize> lgtQuoteAuthorizes = lgtBidVendor.getLgtQuoteAuthorizes();
                    if (CollectionUtils.isNotEmpty(lgtQuoteAuthorizes)) {
                        iLgtQuoteAuthorizeService.updateBatchById(lgtQuoteAuthorizes);
                    }
                });
                iLgtVendorService.saveBatch(lgtBidVendorsAdd);
            }

            if(CollectionUtils.isNotEmpty(lgtPayPlansUpdate)){
                iLgtPayPlanService.updateBatchById(lgtPayPlansUpdate);
            }

            if(CollectionUtils.isNotEmpty(lgtPayPlansAdd)){
                iLgtPayPlanService.saveBatch(lgtPayPlansAdd);
            }
            // ????????????????????????
            updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.inviteSupplier.getValue())
                    .setBidingId(bidingId).setDataFlag(YesOrNo.YES.getValue()));

            // ??????????????????????????????
            int size = lgtBidVendors.size();
            String biddingSuppliers = "0/" + size;
            biding.setBiddingSuppliers(biddingSuppliers);
            this.updateById(biding);
        }
    }

    @Override
    public List<LgtBidVendor> getLgtBidVendorByBidingId(Long bidingId) {
        List<LgtBidVendor> bidVendors = iLgtVendorService.list(Wrappers.lambdaQuery(LgtBidVendor.class).eq(LgtBidVendor::getBidingId, bidingId));
        if(CollectionUtils.isNotEmpty(bidVendors)){
            bidVendors.forEach(lgtBidVendor -> {
                // ????????????
                Long bidVendorId = lgtBidVendor.getBidVendorId();
                List<LgtPayPlan> lgtPayPlans = iLgtPayPlanService.list(Wrappers.lambdaQuery(LgtPayPlan.class).
                        eq(LgtPayPlan::getBidVendorId, bidVendorId));
                lgtBidVendor.setLgtPayPlans(lgtPayPlans);
            });
        }
        return bidVendors;
    }

    @Override
    public List<VendorDto> intelligentFindVendor(Long bidingId) {
        List<LgtBidRequirementLine> lgtBidRequirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).
                eq(LgtBidRequirementLine::getBidingId, bidingId));
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtBidRequirementLines),"????????????????????????");
        lgtBidRequirementLines.forEach(lgtBidRequirementLine -> Assert.notNull(lgtBidRequirementLine.getLogisticsCategoryId(),
                "?????????????????????????????????"));
        List<Long> categoryIds = lgtBidRequirementLines.stream().map(LgtBidRequirementLine::getLogisticsCategoryId).collect(Collectors.toList());
        List<VendorDto> vendorDtos = supplierClient.queryCompanyByBusinessModeCode(categoryIds);
        return vendorDtos;
    }

    @Override
    @Transactional
    public List<LgtBidRequirementLine> updateLgtBidRequirementLine(List<LgtBidRequirementLine> lgtBidRequirementLines) {
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtBidRequirementLines),"????????????????????????!");
//        lgtBidRequirementLines.forEach(lgtBidRequirementLine -> Assert.notNull(lgtBidRequirementLine.getLogisticsCategoryId(),"???????????????????????????????????????!"));
        Long bidingId = lgtBidRequirementLines.get(0).getBidingId();
        Assert.notNull(bidingId,"????????????????????????:bidingId");
        LgtBiding lgtBiding = this.getById(bidingId);
        checkPurchaseRequirementLine(lgtBidRequirementLines,bidingId);
        String auditStatus = this.getById(bidingId).getAuditStatus();
        //??????????????????
        if (Objects.equals(auditStatus, BiddingApprovalStatus.APPROVED.getValue())) {
            return lgtBidRequirementLines;
        }
        AtomicInteger num = new AtomicInteger(1);
        Set<String> hashSet = new HashSet<>();
        lgtBidRequirementLines.forEach(lgtBidRequirementLine -> {
            lgtBidRequirementLine.setBidRequirementLineId(IdGenrator.generate());
            lgtBidRequirementLine.setBidingId(bidingId);
            lgtBidRequirementLine.setRowNum(num.getAndAdd(1));
            setStartEndAddress(lgtBidRequirementLine);
            if(CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
                // ??????????????????: ???+???+????????????+????????????+LEG+??????
                String onlyKey = new StringBuffer().
                        append(lgtBidRequirementLine.getStartAddress()).
                        append(lgtBidRequirementLine.getEndAddress()).
                        append(lgtBidRequirementLine.getChargeMethod()).
                        append(lgtBidRequirementLine.getChargeUnit()).
                        append(lgtBidRequirementLine.getLeg()).
                        append(lgtBidRequirementLine.getExpenseItem()).
                        toString();
                Assert.isTrue(hashSet.add(onlyKey),String.format("????????????:??????????????????,???:[%s],???:[%s]+????????????:[%s]+????????????[%s]+LEG[%s]+??????[%s]",lgtBidRequirementLine.getStartAddress(),lgtBidRequirementLine.getEndAddress(),lgtBidRequirementLine.getChargeMethod(),lgtBidRequirementLine.getChargeUnit(),lgtBidRequirementLine.getLeg(),lgtBidRequirementLine.getExpenseItem()));
            }else {
                // ??????????????????: ???+???
                String onlyKey = new StringBuffer().append(lgtBidRequirementLine.getStartAddress()).
                        append(lgtBidRequirementLine.getEndAddress()).
                        toString();
                Assert.isTrue(hashSet.add(onlyKey),String.format("???????????????:??????????????????,???:[%s],???:[%s]",lgtBidRequirementLine.getStartAddress(),lgtBidRequirementLine.getEndAddress()));
            }
        });

        iLgtBidRequirementLineService.remove(Wrappers.lambdaQuery(LgtBidRequirementLine.class).eq(LgtBidRequirementLine::getBidingId,bidingId));
        AtomicInteger rowNum = new AtomicInteger(1);
        // ????????????
        lgtBidRequirementLines.forEach(lgtBidRequirementLine -> lgtBidRequirementLine.setRowNum(rowNum.getAndAdd(1)));
        iLgtBidRequirementLineService.saveBatch(lgtBidRequirementLines);
        // ????????????????????????
        updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.projectRequirement.getValue())
                .setBidingId(bidingId).setDataFlag(YesOrNo.YES.getValue()));
        return lgtBidRequirementLines;
    }

    /**
     * ?????????????????????
     * @param lgtBidRequirementLines
     */
    public void checkPurchaseRequirementLine(List<LgtBidRequirementLine> lgtBidRequirementLines,Long bidingId){
        String sourceFrom = lgtBidRequirementLines.get(0).getSourceFrom();
        if (!SourceFrom.PURCHASE_REQUEST.name().equals(sourceFrom) && CollectionUtils.isNotEmpty(lgtBidRequirementLines)) {
            List<LgtBidTemplate> lgtBidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).
                    eq(LgtBidTemplate::getBidingId, bidingId).
                    eq(LgtBidTemplate::getPurchaseNotEmptyFlag, YesOrNo.YES.getValue()));
            if(CollectionUtils.isNotEmpty(lgtBidTemplates)){
                Class<LgtBidRequirementLine> aClass = LgtBidRequirementLine.class;
                Map<String, String> fieldMap = lgtBidTemplates.stream().collect(Collectors.toMap(lgtBidTemplate -> StringUtil.toCamelCase(lgtBidTemplate.getFieldCode()), LgtBidTemplate::getFieldName, (k1, k2) -> k1));
                lgtBidRequirementLines.forEach(lgtBidRequirementLine -> {
                    fieldMap.keySet().forEach(field -> {
                        Object o = null;
                        boolean flag = true;
                        try {
                            Field declaredField = aClass.getDeclaredField(field);
                            declaredField.setAccessible(true);
                            o = declaredField.get(lgtBidRequirementLine);
                        } catch (Exception e) {
                            flag = false;
                        }
                        if (flag) {
                            Assert.isTrue( !ObjectUtils.isEmpty(o),String.format("????????????:[%s]????????????",fieldMap.get(field)));
                        }
                    });
                });
            }
        }
    }

    /**
     * ???????????? ?????????/?????????
     * @param obj
     */
    public static <T> void setStartEndAddress(T obj){
        /**
         * ????????? - toCountry-toProvince-toCity-toCounty-toPlace-toPort
         * ????????? - fromCountry-fromProvince-fromCity-fromCounty-fromPlace-fromPort
         */
        List<String> startList = Arrays.asList("fromCountry", "fromProvince", "fromCity", "fromCounty", "fromPlace","fromPort");
        List<String> endList = Arrays.asList("toCountry", "toProvince", "toCity", "toCounty", "toPlace","toPort");
        StringBuffer startAddress = new StringBuffer();
        StringBuffer endAddress = new StringBuffer();
        endList.forEach(key->{
            getField(obj, endAddress,key);
        });
        startList.forEach(key->{
            getField(obj, startAddress,key);
        });
        Class aClass = obj.getClass();
        try {
            Field startAddress1 = aClass.getDeclaredField("startAddress");
            startAddress1.setAccessible(true);
            Field endAddress1 = aClass.getDeclaredField("endAddress");
            endAddress1.setAccessible(true);
            startAddress1.set(obj,startAddress.toString());
            endAddress1.set(obj,endAddress.toString());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static <T> void getField(T obj, StringBuffer endAddress,String key) {
        try {
            Field field = obj.getClass().getDeclaredField(key);
            if (null != field) {
                field.setAccessible(true);
                Object o = field.get(obj);
                if (!ObjectUtils.isEmpty(o)) {
                    endAddress.append(o);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public List<LgtBidRequirementLine> getLgtBidRequirementLineByBidingId(Long bidingId) {
        List<LgtBidRequirementLine> bidRequirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).
                eq(LgtBidRequirementLine::getBidingId, bidingId));
        return bidRequirementLines;
    }

    @Override
    @Transactional
    public LgtBidInfoDto getLgtBidInfoVo(Long bidingId) {
        LgtBiding biding = this.getById(bidingId);
        updateEnrollEndDatetime(biding);
        List<LgtBidFile> lgtBidFiles = iLgtFileService.list(Wrappers.lambdaQuery(LgtBidFile.class).eq(LgtBidFile::getBidingId, biding.getBidingId()));
        List<LgtFileConfig> lgtFileConfigs = iLgtFileConfigService.list(Wrappers.lambdaQuery(LgtFileConfig.class).eq(LgtFileConfig::getBidingId, bidingId));
        List<LgtGroup> lgtGroups = iLgtGroupService.list(Wrappers.lambdaQuery(LgtGroup.class).eq(LgtGroup::getBidingId, bidingId));
        List<LgtProcessNode> lgtProcessNodes = iLgtProcessNodeService.list(Wrappers.lambdaQuery(LgtProcessNode.class).eq(LgtProcessNode::getBidingId, bidingId));
        List<LgtBidTemplate> bidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).eq(LgtBidTemplate::getBidingId, biding));
        LgtBidInfoDto lgtBidInfoDto = LgtBidInfoDto.builder().
                biding(biding).fileList(lgtBidFiles).bidFileConfigList(lgtFileConfigs).groupList(lgtGroups).
                lgtBidTemplates(bidTemplates).lgtProcessNodes(lgtProcessNodes).build();
        return lgtBidInfoDto;
    }

    @Override
    @Transactional
    public PageInfo<LgtBiding> listPage(LgtBiding lgtBiding) {
        PageUtil.startPage(lgtBiding.getPageNum(), lgtBiding.getPageSize());
        /**
         * ???	?????????????????????????????????????????????????????????????????????????????????
         * ???	?????????????????????????????????????????????
         */
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        List<Long> categoryIds = pmClient.queryCategoryIdByUserId(loginAppUser.getUserId());
        lgtBiding.setCategoryIds(categoryIds);
        lgtBiding.setCurrentUserName(loginAppUser.getUsername());
        lgtBiding.setCurrentUserId(loginAppUser.getUserId());
        List<LgtBiding> lgtBidings = this.baseMapper.listPageByBuyers(lgtBiding);
        if(CollectionUtils.isNotEmpty(lgtBidings)){
            lgtBidings.forEach(this::updateEnrollEndDatetime);
        }
        return new PageInfo<>(lgtBidings);
    }

    /**
     * ????????????????????????????????????
     * @param lgtBiding
     */
    public void updateEnrollEndDatetime(LgtBiding lgtBiding){
        if (BiddingProjectStatus.ACCEPT_BID.getValue().equals(lgtBiding.getBidingStatus())) {
            Date enrollEndDatetime = lgtBiding.getEnrollEndDatetime();
            if(!ObjectUtils.isEmpty(enrollEndDatetime) && enrollEndDatetime.compareTo(new Date()) < 0){
                /**
                 * ????????????????????????????????????
                 */
                lgtBiding.setBidingStatus(BiddingProjectStatus.TENDER_ENDING.getValue());
                this.updateById(lgtBiding);

                /**
                 * ??????????????????
                 */
                updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.technicalManagement.getValue())
                        .setBidingId(lgtBiding.getBidingId()).setDataFlag(YesOrNo.YES.getValue()));
                updateNodeStatus(new LgtProcessNode().setNodeCode(ProcessNodeName.commercialManagement.getValue())
                        .setBidingId(lgtBiding.getBidingId()).setDataFlag(YesOrNo.YES.getValue()));
            }
        }
    }

    @Override
    @Transactional
    public Long updateOrAdd(LgtBidDto lgtBidDto) {
        LgtBiding biding = lgtBidDto.getBiding(); // ??????????????????
        List<LgtFileConfig> bidFileConfigList = lgtBidDto.getBidFileConfigList(); // ????????????????????????
        List<LgtBidFile> fileList = lgtBidDto.getFileList(); // ????????????
        List<LgtGroup> groupList = lgtBidDto.getGroupList(); // ????????????
        List<LgtBidRequirementLine> requirementLineList = lgtBidDto.getRequirementLineList(); //?????????????????????
        // ??????ID
        Long bidingId = biding.getBidingId();
        if(ObjectUtils.isEmpty(bidingId)){
            bidingId = IdGenrator.generate();
            biding.setBidingId(bidingId);
            if(ObjectUtils.isEmpty(biding.getBidingNum())){
                // ??????????????????
                log.info("??????????????????????????????????????????:"+ JSON.toJSONString(biding));
                String requirementNum = getRequirementNum(biding);
                log.info("????????????????????????????????????:"+requirementNum);
                biding.setBidingNum(requirementNum);
            }
            String nickname = AppUserUtil.getLoginAppUser().getNickname();
            // ???????????????
            biding.setAuditStatus(BiddingApprovalStatus.DRAFT.getValue()).
                    setBidingStatus(BiddingProjectStatus.DRAW_UP.getValue()).
                    setEndAuditStatus(BiddingApprovalStatus.DRAFT.getValue()).
                    setReleaseFlag(YesOrNo.NO.getValue()).
                    setCreatedByName(nickname).
                    setCurrentRound(1);
            //????????????????????????
            if(StringUtils.isBlank(biding.getSourceFrom())){
                biding.setSourceFrom(SourceFrom.MANUAL.getItemValue());
            }
            // ??????????????????
            if(ObjectUtils.isEmpty(biding.getTemplateFileId())){
                LogisticsTemplateHead templateHead = iLogisticsTemplateHeadService.getById(biding.getTemplateHeadId());
                biding.setTemplateFileId(templateHead.getTemplateFileId());
                biding.setTemplateFileName(templateHead.getTemplateFileName());
            }
            this.save(biding);
        }else {
            this.updateById(biding);
        }
        // ??????????????????
        sevaLgtBidFiles(biding, fileList, bidingId);
        // ??????????????????????????????
        saveLgtFileConfigs(biding, bidFileConfigList, bidingId);
        // ??????????????????
        saveLgtGroups(biding, groupList, bidingId);
        // ????????????????????????
        Long processConfigId = biding.getProcessConfigId();
        saveNodes(bidingId,processConfigId);
        // ???????????????????????????
        saveLgtBidTemplates(biding, bidingId);
        // ???????????????
        if (CollectionUtils.isNotEmpty(requirementLineList)) {
            Long finalBidingId = bidingId;
            requirementLineList.forEach(lgtBidRequirementLine -> {
                lgtBidRequirementLine.setBidingId(finalBidingId);
                lgtBidRequirementLine.setSourceFrom(SourceFrom.PURCHASE_REQUEST.name());
            });
            updateLgtBidRequirementLine(requirementLineList);
        }
        return bidingId;
    }

    @Override
    @Transactional
    public Long add(LgtBidDto lgtBidDto) {
        // ????????????
        checkParam(lgtBidDto);
        return updateOrAdd(lgtBidDto);
    }

    @Override
    @Transactional
    public Long modify(LgtBidDto lgtBidDto) {
        // ????????????
        checkParam(lgtBidDto);
        //??????????????????
        if (Objects.equals(lgtBidDto.getBiding().getAuditStatus(), BiddingApprovalStatus.APPROVED.getValue())) {
            return lgtBidDto.getBiding().getBidingId();
        }
        return updateOrAdd(lgtBidDto);
    }

    /**
     * <pre>
     *  //????????????????????????????????????D???????????????????????????????????????F???????????????????????????????????????S???
     * </pre>
     *
     * @author chenwt24@meicloud.com
     * @version 1.00.00
     * <p>
     * <pre>
     *  ????????????
     *  ???????????????:
     *  ?????????:
     *  ????????????: 2020-11-27
     *  ????????????:
     * </pre>
     */
    public String getRequirementNum(LgtBiding biding) {
        String code = biding.getBusinessModeCode();
        Assert.isTrue(!ObjectUtils.isEmpty(code),"????????????????????????!");
        AtomicReference<String> reequirementNum = new AtomicReference<>();
        if (BusinessMode.INSIDE.getValue().equals(code)) {
            reequirementNum.set(baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM_D));
        } else if (BusinessMode.OUTSIDE.getValue().equals(code)) {
            reequirementNum.set(baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM_F));
        } else {
            reequirementNum.set(baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM_S));
        }
        return reequirementNum.get();
    }

    private void checkParam(LgtBidDto lgtBidDto) {
        Assert.notNull(lgtBidDto,"??????????????????!");
        LgtBiding biding = lgtBidDto.getBiding();
        Assert.notNull(biding.getTemplateHeadId(),"????????????:???????????????????????????ID(templateHeadId)!");
        Assert.notNull(biding.getProcessConfigId(),"????????????:??????????????????ID(processConfigId)!");
        //???????????????????????????????????????,???????????????????????????????????????
        if(!SourceFrom.PURCHASE_REQUEST.getItemValue().equals(biding.getSourceFrom())){
            Assert.notNull(biding.getEnrollEndDatetime(),"??????????????????????????????!");
            Assert.notNull(biding.getStandardCurrency(),"????????????????????????!");
        }
        List<LgtFileConfig> bidFileConfigList = lgtBidDto.getBidFileConfigList();
        if(CollectionUtils.isNotEmpty(bidFileConfigList)){
            bidFileConfigList.forEach(lgtFileConfig -> {
                Assert.isTrue(StringUtils.isNotBlank(lgtFileConfig.getReferenceFileType()),"????????????????????????-????????????:????????????!");
            });
        }
        List<LgtGroup> groupList = lgtBidDto.getGroupList();
        boolean hasTechnology=false;
        boolean hasBusiness=false;
        for (LgtGroup group : groupList) {
            String judgeFlag = group.getJudgeFlag();
            if(Objects.equals(judgeFlag,"Y")&&!hasTechnology){
                hasTechnology=true;
            }
            if(!Objects.equals(judgeFlag,"Y")&&!hasBusiness){
                hasBusiness=true;
            }
            if(hasBusiness&&hasTechnology){
                break;
            }
        }
        if(!(hasBusiness&&hasTechnology)){
            throw new BaseException("????????????????????????????????????????????????????????????");
        }
    }

    public void saveLgtBidTemplates(LgtBiding biding, Long bidingId) {
        Long templateHeadId = biding.getTemplateHeadId();
        List<LgtBidTemplate> bidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).eq(LgtBidTemplate::getBidingId, biding.getBidingId()));
        if (CollectionUtils.isEmpty(bidTemplates)) {
            List<LogisticsTemplateLine> templateLines = iLogisticsTemplateLineService.list(Wrappers.lambdaQuery(LogisticsTemplateLine.class).eq(LogisticsTemplateLine::getHeadId, templateHeadId));
            if(CollectionUtils.isNotEmpty(templateLines)){
                List<LgtBidTemplate> lgtBidTemplates = new ArrayList<>();
                templateLines.forEach(logisticsTemplateLine -> {
                    LgtBidTemplate lgtBidTemplate = new LgtBidTemplate();
                    BeanCopyUtil.copyProperties(lgtBidTemplate,logisticsTemplateLine);
                    lgtBidTemplate.setBidingId(bidingId);
                    lgtBidTemplate.setBidTemplateId(IdGenrator.generate());
                    lgtBidTemplates.add(lgtBidTemplate);
                });
                iLgtBidTemplateService.saveBatch(lgtBidTemplates);
            }
        }
    }

    public void saveNodes(Long bidingId, Long processConfigId) {
        if (!ObjectUtils.isEmpty(processConfigId)) {
            List<LgtProcessNode> lgtProcessNodes = iLgtProcessNodeService.list(Wrappers.lambdaQuery(LgtProcessNode.class).eq(LgtProcessNode::getBidingId, bidingId));
            if(CollectionUtils.isEmpty(lgtProcessNodes)){
                // ??????????????????????????????
                BidProcessConfig bidProcessConfig = bidClient.getBidProcessConfigById(processConfigId);
                Assert.notNull(bidProcessConfig,"?????????????????????????????????");
                List<LgtProcessNode> processNodes = new ArrayList<>();

                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getProjectInformation())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.projectInformation.getValue());
                    processNode.setDataFlag(YesOrNo.YES.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getTechnologyExchange())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.technologyExchange.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getProjectRequirement())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.projectRequirement.getValue());
                    processNodes.add(processNode);
                }
                /** ??????????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getInviteSupplier())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.inviteSupplier.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getScoringRule())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.scoringRule.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getProcessApproval())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.processApproval.getValue());
                    processNodes.add(processNode);
                }
                /** ??????????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getSupplierPerformance())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.supplierPerformance.getValue());
                    processNodes.add(processNode);
                }
                /** ????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getTargetPrice())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.targetPrice.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getProjectPublish())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.projectPublish.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getEntryManagement())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.entryManagement.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getQuestionClarification())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.questionClarification.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getBidingControl())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.bidingControl.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getTechnicalScore())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.technicalScore.getValue());
                    processNodes.add(processNode);
                }
                /** ??????????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getTechnicalManagement())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.technicalManagement.getValue());
                    processNodes.add(processNode);
                }
                /** ??????????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getCommercialManagement())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.commercialManagement.getValue());
                    processNodes.add(processNode);
                }
                /** ?????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getBidEvaluation())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.bidEvaluation.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getBidingResult())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.bidingResult.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getProjectReport())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.projectReport.getValue());
                    processNodes.add(processNode);
                }
                /** ???????????? */
                if(YesOrNo.YES.getValue().equals(bidProcessConfig.getProjectApproval())){
                    LgtProcessNode processNode = new LgtProcessNode();
                    processNode.setNodeCode(ProcessNodeName.projectApproval.getValue());
                    processNodes.add(processNode);
                }
                processNodes.forEach(node->{
                    Long id = IdGenrator.generate();
                    node.setNodeId(id);
                    node.setBidingId(bidingId);
                    node.setProcessConfigId(processConfigId);
                });
                iLgtProcessNodeService.saveBatch(processNodes);
            }
        }
    }

    public void saveLgtGroups(LgtBiding biding, List<LgtGroup> groupList, Long bidingId) {
        iLgtGroupService.remove(Wrappers.lambdaQuery(LgtGroup.class).eq(LgtGroup::getBidingId,bidingId));
        if(CollectionUtils.isNotEmpty(groupList)){
            groupList.forEach(lgtGroup -> {
                lgtGroup.setBidingId(biding.getBidingId());
                lgtGroup.setGroupId(IdGenrator.generate());
            });
            iLgtGroupService.saveBatch(groupList);
        }
    }

    public void saveLgtFileConfigs(LgtBiding biding, List<LgtFileConfig> bidFileConfigList, Long bidingId) {
        iLgtFileConfigService.remove(Wrappers.lambdaQuery(LgtFileConfig.class).eq(LgtFileConfig::getBidingId,bidingId));
        if(CollectionUtils.isNotEmpty(bidFileConfigList)){
            bidFileConfigList.forEach(lgtFileConfig -> {
                lgtFileConfig.setBidingId(biding.getBidingId());
                lgtFileConfig.setRequireId(IdGenrator.generate());
            });
            iLgtFileConfigService.saveBatch(bidFileConfigList);
        }
    }

    public void sevaLgtBidFiles(LgtBiding biding, List<LgtBidFile> fileList, Long bidingId) {
        iLgtFileService.remove(Wrappers.lambdaQuery(LgtBidFile.class).eq(LgtBidFile::getBidingId,bidingId));
        if(CollectionUtils.isNotEmpty(fileList)){
            fileList.forEach(lgtBidFile -> {
                lgtBidFile.setBidingId(biding.getBidingId());
                lgtBidFile.setFileId(IdGenrator.generate());
            });
            iLgtFileService.saveBatch(fileList);
        }
    }
}
