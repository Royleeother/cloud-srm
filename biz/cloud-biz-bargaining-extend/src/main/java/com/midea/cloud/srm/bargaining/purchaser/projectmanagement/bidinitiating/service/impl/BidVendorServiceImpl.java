package com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.bargaining.projectmanagement.bidinitiating.BidingScope;
import com.midea.cloud.common.enums.bargaining.projectmanagement.signupmanagement.SignUpStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.BeanCopyUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.component.check.PreCheck;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.component.entity.EntityManager;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.mapper.BidVendorMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidRequirementLineService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidVendorService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidingService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.quoteauthorize.mapper.QuoteAuthorizeMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.quoteauthorize.service.IQuoteAuthorizeService;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidVendor;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.quoteauthorize.entity.QuoteAuthorize;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.quoteauthorize.vo.QuoteAuthorizeVO;
import com.midea.cloud.srm.model.bargaining.suppliercooperate.vo.BidSignUpVO;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.ContactInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ??????-???????????? ???????????????
 * </pre>
 *
 * @author fengdc3@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-03-27 17:36:33
 *  ????????????:
 *          </pre>
 */
@Service
public class BidVendorServiceImpl extends ServiceImpl<BidVendorMapper, BidVendor> implements IBidVendorService {

    @Autowired
    private SupplierClient supplierClient;
    @Autowired
    private IQuoteAuthorizeService iQuoteAuthorizeService;
    @Autowired
    private IBidingService iBidingService;
    @Autowired
    private IBidRequirementLineService iBidRequirementLineService;

    private final EntityManager<BidVendor> bidVendorDao
            = EntityManager.use(BidVendorMapper.class);
    private final EntityManager<QuoteAuthorize> quoteAuthorizeDao
            = EntityManager.use(QuoteAuthorizeMapper.class);


    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param bidSignUpVO
     * @return
     */
    @Override
    @Transactional
    public Long saveBidVendorWhenSignUp(BidSignUpVO bidSignUpVO) {
        Long bidingId = bidSignUpVO.getBidingId();
        Long vendorId = bidSignUpVO.getVendorId();
        BidVendor bidVendor = getBidVendorByBidingAndVerdonId(bidingId, vendorId);
        boolean addBidVendor = null == bidVendor;
        String join = YesOrNo.NO.getValue();
        if (SignUpStatus.SIGNUPED.getValue().equals(bidSignUpVO.getSignUpStatus())) {
            join = YesOrNo.YES.getValue();
        }
        if (addBidVendor) {
            bidVendor = new BidVendor();
            Long id = IdGenrator.generate();
            BeanCopyUtil.copyProperties(bidVendor, bidSignUpVO);
            bidVendor.setBidVendorId(id);
            bidVendor.setJoinFlag(join);
            bidVendor.setBidingScope(BidingScope.OPEN_TENDER.getValue());
            // ??????????????????????????????
            LoginAppUser user = AppUserUtil.getLoginAppUser();
            CompanyInfo companyInfo = supplierClient.getCompanyInfo(vendorId);
            bidVendor.setVendorCode(companyInfo.getCompanyCode());
            bidVendor.setVendorName(companyInfo.getCompanyName());
            // ?????????????????????
            ContactInfo info = new ContactInfo();
            info.setCompanyId(vendorId);
            ContactInfo contactInfo = supplierClient.getContactInfoByParam(info);
            if (null != contactInfo) {
                bidVendor.setEmail(contactInfo.getEmail());
                bidVendor.setPhone(contactInfo.getPhoneNumber());
                bidVendor.setLinkManName(contactInfo.getContactName());
            }
            this.save(bidVendor);
        } else {
            bidVendor.setJoinFlag(join);
            this.updateById(bidVendor);
        }
        return bidVendor.getBidVendorId();
    }

    /**
     * ????????????ID????????????ID??????????????????????????????????????????
     *
     * @param bidingId
     * @param vendorId
     * @return
     */
    public BidVendor getBidVendorByBidingAndVerdonId(Long bidingId, Long vendorId) {
        return this.getOne(new QueryWrapper<>(new BidVendor().setBidingId(bidingId).setVendorId(vendorId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreCheck(checkMethod = "preCheck")
    public void saveBidVendorList(List<BidVendor> bidVendorList) {
//        Assert.isTrue(CollectionUtils.isNotEmpty(bidVendorList), "?????????????????????????????????");
        if (CollectionUtils.isEmpty(bidVendorList))
            return;
        for (BidVendor vendor : bidVendorList) {
            if (StringUtils.isEmpty(vendor.getLinkManName())) {
                throw new BaseException(String.format("??????????????????%s????????????%s????????????", vendor.getVendorName(), vendor.getLinkManName()));
            }
            Long id = IdGenrator.generate();
           /* CompanyInfo companyInfo = supplierClient.getCompanyInfoByParam(new CompanyInfo().setCompanyCode(vendor.getVendorCode()));
            if (companyInfo == null) {
                throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????,??????", vendor.getVendorCode()));
            }*/
            vendor.setBidVendorId(id).setBidingScope(BidingScope.INVITE_TENDER.getValue()).setJoinFlag("N");
            /*.setVendorId(companyInfo.getCompanyId())*/
            ;
        }
        this.saveBatch(bidVendorList);
        iQuoteAuthorizeService.saveQuoteAuthorize(bidVendorList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
//    @PreCheck(checkMethod = "preCheck")
    public void updateBatch(List<BidVendor> bidVendors) {
        // ????????????
        preCheck(bidVendors);
     /*   if (CollectionUtils.isEmpty(bidVendors))
            throw new BaseException("??????????????????????????????");*/
        if (CollectionUtils.isEmpty(bidVendors))
            return;
        for (BidVendor vendor : bidVendors) {
            if (StringUtils.isEmpty(vendor.getLinkManName())) {
                throw new BaseException(String.format("??????????????????%s????????????%s????????????", vendor.getVendorName(), vendor.getLinkManName()));
            }
        }
     /*   // ?????? ?????????ID
        Long biddingId = bidVendors.stream()
                .filter(bidVendor -> bidVendor.getBidingId() != null)
                .map(BidVendor::getBidingId)
                .findAny()
                .orElseThrow(() -> new BaseException("?????????ID??????"));

        // ?????? ?????????ID
        bidVendors.forEach(bidVendor -> bidVendor.setBidingId(biddingId));*/
        Long biddingId = bidVendors.get(0).getBidingId();
        // ?????????????????? ?????????[bidVendors]??????????????????????????? & ??????????????????
        bidVendorDao.findAll(Wrappers.lambdaQuery(BidVendor.class).eq(BidVendor::getBidingId, biddingId)).stream()
                .filter(existed -> bidVendors.stream().noneMatch(bidVendor -> existed.getBidVendorId().equals(bidVendor.getBidVendorId())))
                .forEach(existed -> {
                    bidVendorDao.delete(existed.getBidVendorId());
                    quoteAuthorizeDao.delete(Wrappers.lambdaQuery(QuoteAuthorize.class).eq(QuoteAuthorize::getBidVendorId, existed.getBidVendorId()));
                });

        // ?????? ????????????????????? & ????????????
        bidVendorDao.useInterceptor()
                .beforeCreate(parameter -> {
                    BidVendor bidVendor = parameter.getPrepareCreateEntity();
                    /*CompanyInfo companyInfo = Optional
                            .ofNullable(supplierClient.getCompanyInfoByParam(new CompanyInfo().setCompanyCode(bidVendor.getVendorCode())))
                            .orElseThrow(() -> new BaseException("?????????????????????????????? | vendorCode: [" + bidVendor.getVendorCode() + "]"));*/
                    bidVendor.setBidVendorId(IdGenrator.generate())
                            .setBidingScope(BidingScope.INVITE_TENDER.getValue())
//                            .setVendorId(companyInfo.getCompanyId())
                            .setJoinFlag("N");
                })
                .save(bidVendors);
        iQuoteAuthorizeService.saveQuoteAuthorize(bidVendors);
    }

    //???????????????vendorId
    private void preCheck(List<BidVendor> bidVendors) {
        Assert.isTrue(CollectionUtils.isNotEmpty(bidVendors),"???????????????????????????!");
        Long biddingId = bidVendors.stream()
                .filter(bidVendor -> bidVendor.getBidingId() != null)
                .map(BidVendor::getBidingId)
                .findAny()
                .orElseThrow(() -> new BaseException("???????????????:?????????ID??????"));
        bidVendors.forEach(bidVendor -> {
            bidVendor.setBidingId(biddingId);
            List<QuoteAuthorizeVO> quoteAuthorizes = iQuoteAuthorizeService.findQuoteAuthorizes(biddingId, bidVendor.getVendorId());
            if (CollectionUtils.isEmpty(quoteAuthorizes)) {
                throw new BaseException(String.format("?????????%s???????????????????????????", bidVendor.getVendorName()));
            }
        });

        Assert.isTrue(CollectionUtils.isNotEmpty(bidVendors), "?????????????????????????????????");
        for (BidVendor bidVendor : bidVendors) {
            CompanyInfo companyInfo = Optional
                    .ofNullable(supplierClient.getCompanyInfoByParam(new CompanyInfo().setCompanyCode(bidVendor.getVendorCode())))
                    .orElseThrow(() -> new BaseException("?????????????????????????????? | vendorCode: [" + bidVendor.getVendorCode() + "]"));
            bidVendor.setVendorId(companyInfo.getCompanyId());
        }

    }

//    @Override
//    public List<IntelligentRecommendVO> listIntelligentRecommendInfo(Long bidingId) {
//        Assert.notNull(bidingId, "?????????id????????????");
//        Biding biding = iBidingService.getById(bidingId);
//        List<BidRequirementLine> bidRequirementLineList = iBidRequirementLineService.list(new QueryWrapper<>(
//                new BidRequirementLine().setBidingId(bidingId)));
//        if (CollectionUtils.isEmpty(bidRequirementLineList)) {
//            return null;
//        } else {
//            List<Long> categoryIdList = bidRequirementLineList.stream().map(BidRequirementLine::getCategoryId).collect(Collectors.toList());
//            IntelligentRecommendParam param = new IntelligentRecommendParam();
//            param.setOrgId(biding.getOrgId()).setCategoryIdList(categoryIdList);
//            return supplierClient.listIntelligentRecommendInfo(param);
//        }
//    }

    @Override
    public List<BidVendor> listVendorContactInfo(List<Long> vendorIdList) {

        // ?????? ?????????????????? & ???[?????????ID]??????
        Map<Long, List<ContactInfo>> vendorContractInfoGroups = supplierClient.listContactInfoByParam(vendorIdList).stream()
                .collect(Collectors.groupingBy(ContactInfo::getCompanyId));

        // ?????? ??????????????? & ???[?????????ID]??????
        Map<Long, CompanyInfo> vendorInfos = supplierClient.getComponyByIds(vendorIdList).stream()
                .collect(Collectors.toMap(CompanyInfo::getCompanyId, x -> x));

        return vendorInfos.keySet().stream()
                .map(vendorId -> {

                    // ?????? ???????????????
                    CompanyInfo companyInfo = vendorInfos.get(vendorId);

                    // ?????? ??????????????????[???]
                    List<ContactInfo> contactInfos = vendorContractInfoGroups.getOrDefault(vendorId, Collections.emptyList());

                    if (contactInfos.isEmpty())
                        return new BidVendor()
                                .setVendorId(companyInfo.getCompanyId())
                                .setVendorCode(companyInfo.getCompanyCode())
                                .setVendorName(companyInfo.getCompanyName());

                    // ?????? ?????????????????? - ?????????[???????????????]????????????????????????????????????
                    ContactInfo contactInfo = contactInfos.stream()
                            .filter(info -> YesOrNo.YES.getValue().equals(info.getCeeaDefaultContact()))
                            .findAny()
                            .orElseGet(() -> contactInfos.get(0));

                    return new BidVendor().setEmail(contactInfo.getEmail())
                            .setPhone(contactInfo.getMobileNumber())
                            .setLinkManName(contactInfo.getContactName())
                            .setVendorId(companyInfo.getCompanyId())
                            .setVendorCode(companyInfo.getCompanyCode())
                            .setVendorName(companyInfo.getCompanyName());

                })
                .collect(Collectors.toList());
    }

}
