package com.midea.cloud.srm.inq.price.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.BaseConst;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.FileUploadType;
import com.midea.cloud.common.enums.ImportStatus;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.handler.TitleColorSheetWriteHandler;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.common.utils.redis.RedisUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.contract.ContractClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.inq.price.mapper.PriceLibraryMapper;
import com.midea.cloud.srm.inq.price.mapper.PriceLibraryPaymentTermMapper;
import com.midea.cloud.srm.inq.price.service.IPriceLadderPriceService;
import com.midea.cloud.srm.inq.price.service.IPriceLibraryPaymentTermService;
import com.midea.cloud.srm.inq.price.service.IPriceLibraryService;
import com.midea.cloud.srm.model.base.dict.entity.DictItem;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.material.enums.CeeaMaterialStatus;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCurrency;
import com.midea.cloud.srm.model.cm.contract.dto.ContractDTO;
import com.midea.cloud.srm.model.cm.contract.entity.ContractHead;
import com.midea.cloud.srm.model.cm.contract.entity.ContractMaterial;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.inq.price.domain.PriceLibraryAddParam;
import com.midea.cloud.srm.model.inq.price.dto.*;
import com.midea.cloud.srm.model.inq.price.entity.PriceLadderPrice;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibrary;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibraryPaymentTerm;
import com.midea.cloud.srm.model.inq.price.vo.PriceLibraryVO;
import com.midea.cloud.srm.model.pm.po.dto.NetPriceQueryDTO;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.dto.BidFrequency;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
*  <pre>
 *  ??????-??????????????? ???????????????
 * </pre>
*
* @author linxc6@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-03-25 11:41:48
 *  ????????????:
 * </pre>
*/
@Slf4j
@Service
public class PriceLibraryServiceImpl extends ServiceImpl<PriceLibraryMapper, PriceLibrary> implements IPriceLibraryService {

    private final IPriceLadderPriceService iPriceLadderPriceService;

    @Resource
    private BaseClient baseClient;

    @Resource
    private PriceLibraryMapper priceLibraryMapper;

    @Autowired
    private IPriceLibraryPaymentTermService priceLibraryPaymentTermService;

    @Autowired
    private PriceLibraryPaymentTermMapper priceLibraryPaymentTermMapper;

    @Autowired
    private ContractClient contractClient;

    @Autowired
    private SupplierClient supplierClient;
    @Autowired
    public PriceLibraryServiceImpl(IPriceLadderPriceService iPriceLadderPriceService) {
        this.iPriceLadderPriceService = iPriceLadderPriceService;
    }

    @Autowired
    private FileCenterClient fileCenterClient;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<PriceLibrary> getQuotePrice(Long organizationId, Long vendorId, Long itemId) {

        Date now = new Date();
        QueryWrapper<PriceLibrary> wrapper = new QueryWrapper<>();
        wrapper.eq("ORGANIZATION_ID", organizationId)
                .eq("VENDOR_ID", vendorId)
                .eq("ITEM_ID", itemId)
                .le("EFFECTIVE_DATE", now).gt("EXPIRATION_DATE", now);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generatePriceLibrary(List<PriceLibraryAddParam> params) {
        //??????????????????
        List<PriceLibrary> libraryEntities = new ArrayList<>();
        //???????????????
        List<PriceLadderPrice> ladderEntities = new ArrayList<>();
        params.forEach(priceLibraryAddParam -> {
            PriceLibrary entity = new PriceLibrary();
            BeanUtils.copyProperties(priceLibraryAddParam, entity);
            entity.setPriceLibraryId(IdGenrator.generate());
            libraryEntities.add(entity);
            /*?????????*/
            if (YesOrNo.YES.getValue().equals(priceLibraryAddParam.getIsLadder())) {
                priceLibraryAddParam.getLadderPrices().forEach(priceLibraryLadderPrice -> {
                    PriceLadderPrice priceLadderPrice = new PriceLadderPrice();
                    BeanUtils.copyProperties(priceLibraryLadderPrice, priceLadderPrice);
                    priceLadderPrice.setPriceLadderPriceId(IdGenrator.generate());
                    priceLadderPrice.setPriceLibraryId(entity.getPriceLibraryId());
                    ladderEntities.add(priceLadderPrice);
                });
            }
        });

        saveBatch(libraryEntities);
        if (CollectionUtils.isNotEmpty(ladderEntities)) {
            iPriceLadderPriceService.saveBatch(ladderEntities);
        }
    }

    /**
     *
     * @param ceeaBuildPriceLibraryParam
     * 2020-11-6?????????????????????????????????
     * ????????????2020-11-05 ??? 2020-11-30 ??????????????? 2020-11-10 ??? 2020-11-30 ????????????????????????????????????
     * ????????????2020-11-05 ??? 2020-11-30 ??????????????? 2020-11-05 ??? 2020-11-10 ????????????????????????????????????
     *
     */
//    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ceeaGeneratePriceLibraryOld(List<PriceLibraryDTO> ceeaBuildPriceLibraryParam) {
        /**
         * 2020-11-17 ???????????????????????? STANDARD
         */
        ceeaBuildPriceLibraryParam.forEach(item -> {
            item.setPriceType("STANDARD");
        });
        String approvalNo = ceeaBuildPriceLibraryParam.stream().map(PriceLibraryDTO::getApprovalNo).findAny().orElseThrow(() -> new BaseException("?????????????????????"));
        /*??????????????????????????????????????????*/
        /*checkIfFormatCorrect(ceeaBuildPriceLibraryParam);*/
        /*??????????????????????????????????????????????????????*/
        /*checkIfOverlapping(ceeaBuildPriceLibraryParam);*/
        Date date = new Date();
        List<PriceLibrary> adds = new ArrayList<>();

        Boolean isLock = redisUtil.tryLock("ceeaGeneratePriceLibrary" + approvalNo, 20, TimeUnit.MINUTES);
        if(!isLock){
            throw new BaseException("??????????????????????????????????????????^_^");
        }
        try{
            List<DictItem> priceUpdateCategary = baseClient.listDictItemByDictCode("PRICE_UPDATE_CATEGARY");
            Set<String> updateCategorySet = priceUpdateCategary.stream().map(DictItem::getDictItemCode).collect(Collectors.toSet());
            ceeaBuildPriceLibraryParam.forEach(item -> {
                /*?????? ???????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ?????????????????? ????????????*/
                String temp = item.getItemDesc();
                if(!updateCategorySet.contains(item.getCategoryCode())){
                    item.setItemDesc(null);
                }
                List<PriceLibrary> priceLibraryList = priceLibraryMapper.ceeaFindListByParams(item);
                item.setItemDesc(temp);
                if(CollectionUtils.isNotEmpty(priceLibraryList)){
                    PriceLibrary priceLibrary = priceLibraryList.get(0);
                    Long priceLibraryId = priceLibrary.getPriceLibraryId();
                    BeanUtils.copyProperties(item,priceLibrary);
                    priceLibrary.setLastUpdateDate(date)
                            .setLastUpdatedBy(priceLibrary.getLastUpdatedBy())
                            .setLastUpdatedId(priceLibrary.getLastUpdatedId())
                            .setPriceLibraryId(priceLibraryId);
                    priceLibraryMapper.updateById(priceLibrary);
                    /*?????????????????????????????????????????????,???????????????????????????*/
                }else{
                    /*?????? ???????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? ???????????? */
                    String temp1 = item.getItemDesc();
                    if(!updateCategorySet.contains(item.getCategoryCode())){
                        item.setItemDesc(null);
                    }
                    List<PriceLibrary> priceLibraryList2 = priceLibraryMapper.ceeaFindListByParams2(item);
                    item.setItemDesc(temp1);
                    /*????????? (??????(??????) ??? ??????????????? ?????????????????????)  ??????????????????????????????????????????????????????*/
                    checkIfOverlapping(priceLibraryList2,item);
                    if(CollectionUtils.isNotEmpty(priceLibraryList2)){
                        boolean ifCoincidence = false;
                        for(PriceLibrary oldItem:priceLibraryList2){
                            /*?????????????????????6.1-6.30 ???????????????6-15-7-30  (?????????)*/
                            if(isBefore(DateUtil.dateToLocalDate(oldItem.getEffectiveDate()),DateUtil.dateToLocalDate(item.getEffectiveDate())) &&
                                    isBeforeAndEquals(DateUtil.dateToLocalDate(item.getEffectiveDate()),DateUtil.dateToLocalDate(oldItem.getExpirationDate())) &&
                                    isBefore(DateUtil.dateToLocalDate(oldItem.getExpirationDate()),DateUtil.dateToLocalDate(item.getExpirationDate()))
                            ){
                                priceLibraryMapper.insert(item.setPriceLibraryId(IdGenrator.generate()));
                                priceLibraryMapper.updateById(new PriceLibrary()
                                        .setPriceLibraryId(oldItem.getPriceLibraryId())
                                        .setExpirationDate(sub(item.getEffectiveDate()))
                                );
                                /*??????????????????????????? - */
                                item.getPriceLibraryPaymentTermList().forEach(paymentTermItem -> {
                                    paymentTermItem.setPriceLibraryId(item.getPriceLibraryId())
                                            .setPriceLibraryPaymentTermId(IdGenrator.generate());
                                });
                                priceLibraryPaymentTermService.saveBatch(item.getPriceLibraryPaymentTermList());
                                ifCoincidence = true;
                                break;
                            }
                            /*????????????????????????6.1-6.30 ???????????????6???15-6???20  (??????) */
                            if(isBeforeAndEquals(DateUtil.dateToLocalDate(oldItem.getEffectiveDate()),DateUtil.dateToLocalDate(item.getEffectiveDate())) &&
                                    isBeforeAndEquals(DateUtil.dateToLocalDate(item.getExpirationDate()),DateUtil.dateToLocalDate(oldItem.getExpirationDate()))
                            ){
                                /**
                                 * ???????????????
                                 * ????????????2020-11-05 ??? 2020-11-30 ??????????????? 2020-11-10 ??? 2020-11-30 ????????????????????????????????????
                                 */
                                if(DateUtil.dateToLocalDate(oldItem.getEffectiveDate()).isBefore(DateUtil.dateToLocalDate(item.getEffectiveDate())) &&
                                        DateUtil.dateToLocalDate(oldItem.getExpirationDate()).isEqual(DateUtil.dateToLocalDate(item.getExpirationDate()))
                                ){
                                    /*????????????????????????,????????????????????????*/
                                    Long id = IdGenrator.generate();
                                    PriceLibrary p1 = new PriceLibrary();
                                    BeanUtils.copyProperties(oldItem,p1);
                                    p1.setPriceLibraryId(oldItem.getPriceLibraryId())
                                            .setExpirationDate(sub(item.getEffectiveDate()));
                                    priceLibraryMapper.insert(item.setPriceLibraryId(id));
                                    priceLibraryMapper.updateById(p1);
                                    /*??????????????????????????????*/
                                    QueryWrapper<PriceLibraryPaymentTerm> priceLibraryPaymentTermQueryWrapper = new QueryWrapper<>();
                                    priceLibraryPaymentTermQueryWrapper.eq("PRICE_LIBRARY_ID",oldItem.getPriceLibraryId());
                                    List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = item.getPriceLibraryPaymentTermList();
                                    priceLibraryPaymentTermList.forEach(paymentTerm -> {
                                        paymentTerm.setPriceLibraryPaymentTermId(IdGenrator.generate())
                                                .setPriceLibraryId(id);
                                    });
                                    priceLibraryPaymentTermService.saveBatch(priceLibraryPaymentTermList);
                                    ifCoincidence = true;
                                    break;
                                }

                                /**
                                 * ???????????????
                                 * ????????????2020-11-05 ??? 2020-11-30 ??????????????? 2020-11-05 ??? 2020-11-10 ????????????????????????????????????
                                 */
                                if(DateUtil.dateToLocalDate(oldItem.getEffectiveDate()).equals(DateUtil.dateToLocalDate(item.getEffectiveDate())) &&
                                        DateUtil.dateToLocalDate(item.getExpirationDate()).isBefore(DateUtil.dateToLocalDate(oldItem.getExpirationDate()))
                                ){
                                    /*???????????????????????????????????????????????????*/
                                    Long id = IdGenrator.generate();
                                    PriceLibrary p1 = new PriceLibrary();
                                    BeanUtils.copyProperties(oldItem,p1);
                                    p1.setPriceLibraryId(oldItem.getPriceLibraryId())
                                            .setEffectiveDate(add(item.getExpirationDate()));
                                    priceLibraryMapper.insert(item.setPriceLibraryId(id));
                                    priceLibraryMapper.updateById(p1);

                                    /*??????????????????????????????*/
                                    QueryWrapper<PriceLibraryPaymentTerm> priceLibraryPaymentTermQueryWrapper = new QueryWrapper<>();
                                    priceLibraryPaymentTermQueryWrapper.eq("PRICE_LIBRARY_ID",oldItem.getPriceLibraryId());
                                    List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = item.getPriceLibraryPaymentTermList();
                                    priceLibraryPaymentTermList.forEach(paymentTerm -> {
                                        paymentTerm.setPriceLibraryPaymentTermId(IdGenrator.generate())
                                                .setPriceLibraryId(id);
                                    });
                                    priceLibraryPaymentTermService.saveBatch(priceLibraryPaymentTermList);
                                    ifCoincidence = true;
                                    break;
                                }


                                /*???????????????????????????*/
                                if(DateUtil.dateToLocalDate(oldItem.getEffectiveDate()).isEqual(DateUtil.dateToLocalDate(item.getEffectiveDate())) ||
                                        DateUtil.dateToLocalDate(oldItem.getExpirationDate()).isEqual(DateUtil.dateToLocalDate(item.getExpirationDate()))
                                ){
                                    log.info("???????????????????????????????????????????????????????????????:["
                                            + new SimpleDateFormat("yyyyMMdd").format(oldItem.getEffectiveDate())
                                            + "-" + new SimpleDateFormat("yyyyMMdd").format(oldItem.getExpirationDate())
                                            + "] ??????????????? [" + new SimpleDateFormat("yyyyMMdd").format(item.getEffectiveDate()) + "-" + new SimpleDateFormat("yyyyMMdd").format(item.getExpirationDate()) + "]");
                                    throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????????????????["
                                            + new SimpleDateFormat("yyyyMMdd").format(oldItem.getEffectiveDate())
                                            + "-" + new SimpleDateFormat("yyyyMMdd").format(oldItem.getExpirationDate())
                                            + "] ???????????????[" + new SimpleDateFormat("yyyyMMdd").format(item.getEffectiveDate()) + "-" + new SimpleDateFormat("yyyyMMdd").format(item.getExpirationDate()) + "]"));
                                }


                                priceLibraryMapper.insert(item.setPriceLibraryId(IdGenrator.generate()));
                                PriceLibrary p1 = new PriceLibrary();
                                PriceLibrary p2 = new PriceLibrary();
                                BeanUtils.copyProperties(oldItem,p1);
                                BeanUtils.copyProperties(oldItem,p2);
                                p1.setPriceLibraryId(IdGenrator.generate())
                                        .setExpirationDate(sub(item.getEffectiveDate()));
                                p2.setPriceLibraryId(IdGenrator.generate())
                                        .setEffectiveDate(add(item.getExpirationDate()));
                                priceLibraryMapper.deleteById(oldItem);
                                priceLibraryMapper.insert(p1);
                                priceLibraryMapper.insert(p2);

                                /*??????????????????????????? - item*/
                                item.getPriceLibraryPaymentTermList().forEach(paymentTermItem -> {
                                    paymentTermItem.setPriceLibraryId(item.getPriceLibraryId())
                                            .setPriceLibraryPaymentTermId(IdGenrator.generate());
                                });
                                priceLibraryPaymentTermService.saveBatch(item.getPriceLibraryPaymentTermList());
                                /*??????????????????????????? p1 p2*/
                                QueryWrapper<PriceLibraryPaymentTerm> wrapper = new QueryWrapper();
                                wrapper.eq("PRICE_LIBRARY_ID",oldItem.getPriceLibraryId());
                                List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = priceLibraryPaymentTermService.list(wrapper);
                                List<PriceLibraryPaymentTerm> priceLibraryPaymentTermP1s = new ArrayList<>();
                                List<PriceLibraryPaymentTerm> priceLibraryPaymentTermP2s = new ArrayList<>();
                                priceLibraryPaymentTermList.forEach(paymentTermItem -> {
                                    PriceLibraryPaymentTerm pt1 = new PriceLibraryPaymentTerm();
                                    PriceLibraryPaymentTerm pt2 = new PriceLibraryPaymentTerm();
                                    BeanUtils.copyProperties(paymentTermItem,pt1);
                                    BeanUtils.copyProperties(paymentTermItem,pt2);
                                    pt1.setPriceLibraryId(p1.getPriceLibraryId())
                                            .setPriceLibraryPaymentTermId(IdGenrator.generate());
                                    pt2.setPriceLibraryId(p2.getPriceLibraryId())
                                            .setPriceLibraryPaymentTermId(IdGenrator.generate());
                                    priceLibraryPaymentTermP1s.add(pt1);
                                    priceLibraryPaymentTermP2s.add(pt2);
                                });
                                priceLibraryPaymentTermService.saveBatch(priceLibraryPaymentTermP1s);
                                priceLibraryPaymentTermService.saveBatch(priceLibraryPaymentTermP2s);

                                /*????????????????????????*/
                                priceLibraryPaymentTermService.remove(wrapper);
                                ifCoincidence = true;
                                break;
                            }
                            /* ????????? 6.3-6.4 ,??????????????? 6.2-6.5 (?????????) (????????? ??? ????????? ??????)*/
                            if(isBeforeAndEquals(DateUtil.dateToLocalDate(item.getEffectiveDate()),DateUtil.dateToLocalDate(oldItem.getEffectiveDate())) &&
                                    isBeforeAndEquals(DateUtil.dateToLocalDate(oldItem.getExpirationDate()),DateUtil.dateToLocalDate(item.getExpirationDate()))
                            ){

                            /*if(DateUtil.dateToLocalDate(oldItem.getEffectiveDate()).isEqual(DateUtil.dateToLocalDate(item.getEffectiveDate())) &&
                                    DateUtil.dateToLocalDate(oldItem.getExpirationDate()).isBefore(DateUtil.dateToLocalDate(item.getExpirationDate()))
                            ){
                                *//*???????????????????????????????????? ???????????????*//*
                                priceLibraryMapper.deleteById(oldItem.getPriceLibraryId());
                                QueryWrapper<PriceLibraryPaymentTerm> wrapper = new QueryWrapper();
                                wrapper.eq("PRICE_LIBRARY_ID",oldItem.getPriceLibraryId());
                                priceLibraryPaymentTermService.remove(wrapper);
                                *//*????????????????????????????????? ???????????????*//*
                                priceLibraryMapper.insert(item.setPriceLibraryId(IdGenrator.generate()));
                                priceLibraryPaymentTermService.saveBatch(item.getPriceLibraryPaymentTermList());
                                ifCoincidence = true;
                                break;
                            }*/

                                log.info("????????????????????????????????????????????????????????????:["
                                        + new SimpleDateFormat("yyyyMMdd").format(oldItem.getEffectiveDate())
                                        + "-" + new SimpleDateFormat("yyyyMMdd").format(oldItem.getExpirationDate())
                                        + "] ???????????????[" + new SimpleDateFormat("yyyyMMdd").format(item.getEffectiveDate()) + "-" + new SimpleDateFormat("yyyyMMdd").format(item.getExpirationDate()) + "]");
                                throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????????????????:["
                                        + new SimpleDateFormat("yyyyMMdd").format(oldItem.getEffectiveDate())
                                        + "-" + new SimpleDateFormat("yyyyMMdd").format(oldItem.getExpirationDate())
                                        + "] ???????????????[" + new SimpleDateFormat("yyyyMMdd").format(item.getEffectiveDate()) + "-" + new SimpleDateFormat("yyyyMMdd").format(item.getExpirationDate()) + "]"));
                            }

                            /*?????????????????????6.15-6.30  ??????????????? 6.1-6.16 (?????????)*/
                            if(isBefore(DateUtil.dateToLocalDate(item.getEffectiveDate()),DateUtil.dateToLocalDate(oldItem.getEffectiveDate())) &&
                                    isBeforeAndEquals(DateUtil.dateToLocalDate(oldItem.getEffectiveDate()),DateUtil.dateToLocalDate(item.getExpirationDate())) &&
                                    isBefore(DateUtil.dateToLocalDate(item.getExpirationDate()),DateUtil.dateToLocalDate(oldItem.getExpirationDate()))
                            ){
                                log.info("???????????????????????????????????????????????????????????????:["
                                        + new SimpleDateFormat("yyyyMMdd").format(oldItem.getEffectiveDate())
                                        + "-" + new SimpleDateFormat("yyyyMMdd").format(oldItem.getExpirationDate())
                                        + "] ???????????????[" + new SimpleDateFormat("yyyyMMdd").format(item.getEffectiveDate()) + "-" + new SimpleDateFormat("yyyyMMdd").format(item.getExpirationDate()) + "]");
                                throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????????????????:["
                                        + new SimpleDateFormat("yyyyMMdd").format(oldItem.getEffectiveDate())
                                        + "-" + new SimpleDateFormat("yyyyMMdd").format(oldItem.getExpirationDate())
                                        + "] ???????????????[" + new SimpleDateFormat("yyyyMMdd").format(item.getEffectiveDate()) + "-" + new SimpleDateFormat("yyyyMMdd").format(item.getExpirationDate()) + "]"));
                            }
                        }
                        /*???????????????*/
                        if(!ifCoincidence){
                            log.info("??????????????????????????????????????????????????????: item = " + JsonUtil.entityToJsonStr(item));
                            /*???????????????*/
                            priceLibraryMapper.insert(item.setPriceLibraryId(IdGenrator.generate()));
                            /*??????????????????*/
                            List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = item.getPriceLibraryPaymentTermList();
                            for(PriceLibraryPaymentTerm priceLibraryPaymentTerm:priceLibraryPaymentTermList){
                                priceLibraryPaymentTerm.setPriceLibraryId(item.getPriceLibraryId());
                            }
                            if(CollectionUtils.isNotEmpty(priceLibraryPaymentTermList)){
                                priceLibraryPaymentTermService.saveBatch(priceLibraryPaymentTermList);
                            }
                        }
                    }else{
                        log.info("???????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? ??????????????????????????????????????????????????????");
                        priceLibraryMapper.insert(item.setPriceLibraryId(IdGenrator.generate()));
                        /*??????????????????*/
                        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = item.getPriceLibraryPaymentTermList();
                        for(PriceLibraryPaymentTerm priceLibraryPaymentTerm:priceLibraryPaymentTermList){
                            priceLibraryPaymentTerm.setPriceLibraryId(item.getPriceLibraryId());
                        }
                        if(CollectionUtils.isNotEmpty(priceLibraryPaymentTermList)){
                            priceLibraryPaymentTermService.saveBatch(priceLibraryPaymentTermList);
                        }

                    }
                }

            });
        }finally {
            redisUtil.unLock("ceeaGeneratePriceLibrary" + approvalNo);
        }

    }

    public static enum CROSSTYPE {
        LEFTCROSS(0),   //?????????
        RIGHTCROSS(1),  //?????????
        CONTAINS(2),   //??????
        INCLUDED(3),   //?????????
        NOCROSS(4);  //?????????

        private final Integer value;

        CROSSTYPE(Integer value) {
            this.value = value;
        }
    }

    /**
     *
     * @param ceeaBuildPriceLibraryParam
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ceeaGeneratePriceLibrary(List<PriceLibraryDTO> ceeaBuildPriceLibraryParam){
        //???????????????????????? PurchaseCurrency CategoryCode
        List<String> currencyCodeList = ceeaBuildPriceLibraryParam
                .stream()
                .filter(item -> StringUtils.isNotBlank(item.getCurrencyCode()))
                .map(item -> item.getCurrencyCode())
                .collect(Collectors.toList());
        List<Long> categoryIdList = ceeaBuildPriceLibraryParam
                .stream()
                .filter(item -> Objects.nonNull(item.getCategoryId()))
                .map(item -> item.getCategoryId())
                .collect(Collectors.toList());
        List<PurchaseCurrency> purchaseCurrencyList = baseClient.listPurchaseCurrencyAnon(currencyCodeList);
        List<PurchaseCategory> purchaseCategoryList = baseClient.listPurchaseCategoryAnon(categoryIdList);

        Map<String,PurchaseCurrency> purchaseCurrencyMap = purchaseCurrencyList.stream().collect(Collectors.toMap(item -> item.getCurrencyCode(),item -> item));
        Map<Long,PurchaseCategory> purchaseCategoryMap = purchaseCategoryList.stream().collect(Collectors.toMap(item -> item.getCategoryId(),item -> item));

        ceeaBuildPriceLibraryParam.forEach(item -> {
            PurchaseCurrency purchaseCurrency = purchaseCurrencyMap.get(item.getCurrencyCode());
            if(Objects.nonNull(purchaseCurrency) && Objects.nonNull(purchaseCurrency.getCurrencyId())){
                item.setCurrencyId(purchaseCurrency.getCurrencyId());
            }
            if(Objects.nonNull(purchaseCurrency) && StringUtils.isNotBlank(purchaseCurrency.getCurrencyName())){
                item.setCurrencyName(purchaseCurrency.getCurrencyName());
            }
            PurchaseCategory purchaseCategory = purchaseCategoryMap.get(item.getCategoryId());
            if(Objects.nonNull(purchaseCategory) && StringUtils.isNotBlank(purchaseCategory.getCategoryCode())){
                item.setCategoryCode(purchaseCategory.getCategoryCode());
            }
        });


        //???????????????????????? STANDARD
        ceeaBuildPriceLibraryParam.forEach(item -> {
            item.setPriceType("STANDARD");
        });

        //????????????????????????????????????key???
        String approvalNo = ceeaBuildPriceLibraryParam.stream().map(PriceLibraryDTO::getApprovalNo).findAny().orElseThrow(() -> new BaseException("?????????????????????"));
        Boolean isLock = redisUtil.tryLock("ceeaGeneratePriceLibrary" + approvalNo, 20, TimeUnit.MINUTES);
        if(!isLock){
            throw new BaseException("??????????????????????????????????????????^_^");
        }
        try {
            //???????????????????????????????????????????????????
            List<DictItem> priceUpdateCategory = baseClient.listDictItemByDictCode("PRICE_UPDATE_CATEGARY");
            Set<String> updateCategorySet = priceUpdateCategory.stream().map(DictItem::getDictItemCode).collect(Collectors.toSet());
            List<PriceLibraryDTO> updateCategoryList = new LinkedList<>();
            List<PriceLibraryDTO> notUpdateCategoryList = new LinkedList<>();

            //?????????????????????????????????????????????????????????????????????????????????????????????????????????
            for(int i=0;i<ceeaBuildPriceLibraryParam.size();i++){
                PriceLibraryDTO p = ceeaBuildPriceLibraryParam.get(i);
                if(updateCategorySet.contains(p.getCategoryCode())){
                    updateCategoryList.add(p);
                }else{
                    notUpdateCategoryList.add(p);
                }
            }

            //?????????????????????????????? ?????? ???????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ??????????????????????????????
            for(int i=0;i<updateCategoryList.size();i++){
                PriceLibraryDTO item = updateCategoryList.get(i);
                List<PriceLibrary> priceLibraryList = priceLibraryMapper.ceeaFindListByParams2(
                        new PriceLibrary().setItemCode(item.getItemCode())
                            .setItemDesc(item.getItemDesc())
                            .setVendorCode(item.getVendorCode())
                            .setCeeaOrgId(item.getCeeaOrgId())
                            .setCeeaOrganizationId(item.getCeeaOrganizationId())
                            .setCeeaArrivalPlace(item.getCeeaArrivalPlace())
                            .setPriceType(item.getPriceType())
                );
                //????????????????????????????????????
                if(CollectionUtils.isEmpty(priceLibraryList)){
                    savePriceLibraryAndPaymentItems(item);
                    continue;
                }

                //??????????????????Map,??????????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ?????????????????? ???key???
                Map<String,PriceLibrary> map1 = getUpdateCategoryPriceLibraryMap1(priceLibraryList);
                String startTime = String.valueOf(DateUtil.dateToLocalDate(item.getEffectiveDate()));
                String endTime = String.valueOf(DateUtil.dateToLocalDate(item.getExpirationDate()));
                String key = new StringBuffer().append(item.getItemCode())
                                .append(item.getItemDesc())
                                .append(item.getVendorCode())
                                .append(item.getCeeaOrgId())
                                .append(item.getCeeaOrganizationId())
                                .append(item.getCeeaArrivalPlace())
                                .append(item.getPriceType())
                                .append(startTime)
                                .append(endTime).toString();

                //??????
                if(Objects.nonNull(map1.get(key))){
                    //?????????????????? ,????????????????????????
                    PriceLibrary priceLibrary = map1.get(key);
                    item.setPriceLibraryId(priceLibrary.getPriceLibraryId());
                    updatePriceLibraryAndPaymentItems(item);
                    continue;
                }

                //???????????????????????????
                List<PriceLibraryDTO> addPriceLibrary = new LinkedList<>();
                List<PriceLibraryDTO> updatePriceLibrary = new LinkedList<>();
                List<PriceLibraryDTO> removePriceLibrary = new LinkedList<>();

                for(PriceLibrary p : priceLibraryList){
                    Integer crossType = whichCrossType(item,p);
                    switch (crossType){
                        case 0 :
                            Map<String,List<PriceLibraryDTO>> map = leftCross(item,p);
                            addPriceLibrary.addAll(map.get("ADD"));
                            updatePriceLibrary.addAll(map.get("UPDATE"));
                            removePriceLibrary.addAll(map.get("REMOVE"));
                            break;
                        case 1 :
                            Map<String,List<PriceLibraryDTO>> map2 = rightCross(item,p);
                            addPriceLibrary.addAll(map2.get("ADD"));
                            updatePriceLibrary.addAll(map2.get("UPDATE"));
                            removePriceLibrary.addAll(map2.get("REMOVE"));
                            break;
                        case 2 :
                            Map<String,List<PriceLibraryDTO>> map3 = contains(item,p);
                            addPriceLibrary.addAll(map3.get("ADD"));
                            updatePriceLibrary.addAll(map3.get("UPDATE"));
                            removePriceLibrary.addAll(map3.get("REMOVE"));
                            break;
                        case 3 :
                            Map<String,List<PriceLibraryDTO>> map4 = included(item,p);
                            addPriceLibrary.addAll(map4.get("ADD"));
                            updatePriceLibrary.addAll(map4.get("UPDATE"));
                            removePriceLibrary.addAll(map4.get("REMOVE"));
                            break;
                        case 4 :
                            Map<String,List<PriceLibraryDTO>> map5 = noCross(item,p);
                            addPriceLibrary.addAll(map5.get("ADD"));
                            updatePriceLibrary.addAll(map5.get("UPDATE"));
                            removePriceLibrary.addAll(map5.get("REMOVE"));
                            break;
                    }

                }

                //??????map??????
                Map<String,PriceLibraryDTO> addMap = getUpdateCategoryPriceLibraryMap2(addPriceLibrary);
                Map<String,PriceLibraryDTO> updateMap = getUpdateCategoryPriceLibraryMap2(updatePriceLibrary);
                Map<String,PriceLibraryDTO> removeMap = getUpdateCategoryPriceLibraryMap2(removePriceLibrary);

                if(!addMap.isEmpty()){
                    List<PriceLibraryDTO> priceLibraryDTOList = new LinkedList<PriceLibraryDTO>(addMap.values());
                    saveBatchPriceLibraryAndPaymentItems(priceLibraryDTOList);
                }
                if(!updateMap.isEmpty()){
                    List<PriceLibraryDTO> priceLibraryDTOList = new LinkedList<PriceLibraryDTO>(updateMap.values());
                    updateBatchPriceLibraryAndPaymentItems(priceLibraryDTOList);
                }
                if(!removeMap.isEmpty()){
                    List<PriceLibraryDTO> priceLibraryDTOList = new LinkedList<PriceLibraryDTO>(removeMap.values());
                    removeBatchPriceLibraryAndPaymentItems(priceLibraryDTOList);
                }


                //????????????
                log.info("?????????????????????" + approvalNo);
                log.info("?????????(????????????)???" + JsonUtil.entityToJsonStr(item));
                log.info("??????????????????" + JsonUtil.arrayToJsonStr(new ArrayList<PriceLibraryDTO>(removeMap.values())));
                log.info("??????????????????" + JsonUtil.arrayToJsonStr(new ArrayList<PriceLibraryDTO>(updateMap.values())));
                log.info("??????????????????" + JsonUtil.arrayToJsonStr(new ArrayList<PriceLibraryDTO>(addMap.values())));


            }

            //????????????????????????????????? ?????? ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ??????????????????
            for(int i=0;i<notUpdateCategoryList.size();i++){
                PriceLibraryDTO item = notUpdateCategoryList.get(i);
                List<PriceLibrary> priceLibraryList = priceLibraryMapper.ceeaFindListByParams2(
                        new PriceLibrary().setItemCode(item.getItemCode())
                            .setVendorCode(item.getVendorCode())
                            .setCeeaOrgId(item.getCeeaOrgId())
                            .setCeeaOrganizationId(item.getCeeaOrganizationId())
                            .setCeeaArrivalPlace(item.getCeeaArrivalPlace())
                            .setPriceType(item.getPriceType())
                );
                //????????????????????????????????????
                if(CollectionUtils.isEmpty(priceLibraryList)){
                    savePriceLibraryAndPaymentItems(item);
                    continue;
                }
                //??????????????????Map,??????????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ?????????????????? ???key???
                Map<String,PriceLibrary> map1 = getNotUpdateCategoryPriceLibraryMap1(priceLibraryList);
                String startTime = String.valueOf(DateUtil.dateToLocalDate(item.getEffectiveDate()));
                String endTime = String.valueOf(DateUtil.dateToLocalDate(item.getExpirationDate()));
                String key = new StringBuffer().append(item.getItemCode())
                        .append(item.getVendorCode())
                        .append(item.getCeeaOrgId())
                        .append(item.getCeeaOrganizationId())
                        .append(item.getCeeaArrivalPlace())
                        .append(item.getPriceType())
                        .append(startTime)
                        .append(endTime).toString();

                //??????
                if(Objects.nonNull(map1.get(key))){
                    //?????????????????? ,????????????????????????
                    PriceLibrary priceLibrary = map1.get(key);
                    item.setPriceLibraryId(priceLibrary.getPriceLibraryId());
                    updatePriceLibraryAndPaymentItems(item);
                    continue;
                }

                List<PriceLibraryDTO> addPriceLibrary = new LinkedList<>();
                List<PriceLibraryDTO> updatePriceLibrary = new LinkedList<>();
                List<PriceLibraryDTO> removePriceLibrary = new LinkedList<>();

                //???????????????????????????
                for(PriceLibrary p : priceLibraryList){
                    Integer crossType = whichCrossType(item,p);
                    switch (crossType){
                        case 0 :
                            Map<String,List<PriceLibraryDTO>> map = leftCross(item,p);
                            addPriceLibrary.addAll(map.get("ADD"));
                            updatePriceLibrary.addAll(map.get("UPDATE"));
                            removePriceLibrary.addAll(map.get("REMOVE"));
                            break;
                        case 1 :
                            Map<String,List<PriceLibraryDTO>> map2 = rightCross(item,p);
                            addPriceLibrary.addAll(map2.get("ADD"));
                            updatePriceLibrary.addAll(map2.get("UPDATE"));
                            removePriceLibrary.addAll(map2.get("REMOVE"));
                            break;
                        case 2 :
                            Map<String,List<PriceLibraryDTO>> map3 = contains(item,p);
                            addPriceLibrary.addAll(map3.get("ADD"));
                            updatePriceLibrary.addAll(map3.get("UPDATE"));
                            removePriceLibrary.addAll(map3.get("REMOVE"));
                            break;
                        case 3 :
                            Map<String,List<PriceLibraryDTO>> map4 = included(item,p);
                            addPriceLibrary.addAll(map4.get("ADD"));
                            updatePriceLibrary.addAll(map4.get("UPDATE"));
                            removePriceLibrary.addAll(map4.get("REMOVE"));
                            break;
                        case 4 :
                            Map<String,List<PriceLibraryDTO>> map5 = noCross(item,p);
                            addPriceLibrary.addAll(map5.get("ADD"));
                            updatePriceLibrary.addAll(map5.get("UPDATE"));
                            removePriceLibrary.addAll(map5.get("REMOVE"));
                            break;
                    }
                }
                //??????map??????
                Map<String,PriceLibraryDTO> addMap = getNotUpdateCategoryPriceLibraryMap2(addPriceLibrary);
                Map<String,PriceLibraryDTO> updateMap = getNotUpdateCategoryPriceLibraryMap2(updatePriceLibrary);
                Map<String,PriceLibraryDTO> removeMap = getNotUpdateCategoryPriceLibraryMap2(removePriceLibrary);

                if(!addMap.isEmpty()){
                    List<PriceLibraryDTO> priceLibraryDTOList = new LinkedList<PriceLibraryDTO>(addMap.values());
                    saveBatchPriceLibraryAndPaymentItems(priceLibraryDTOList);
                }
                if(!updateMap.isEmpty()){
                    List<PriceLibraryDTO> priceLibraryDTOList = new LinkedList<PriceLibraryDTO>(updateMap.values());
                    updateBatchPriceLibraryAndPaymentItems(priceLibraryDTOList);
                }
                if(!removeMap.isEmpty()){
                    List<PriceLibraryDTO> priceLibraryDTOList = new LinkedList<PriceLibraryDTO>(removeMap.values());
                    removeBatchPriceLibraryAndPaymentItems(priceLibraryDTOList);
                }
                //????????????
                log.info("?????????????????????" + approvalNo);
                log.info("?????????(???????????????)???" + JsonUtil.entityToJsonStr(item));
                log.info("??????????????????" + JsonUtil.arrayToJsonStr(new ArrayList<PriceLibraryDTO>(removeMap.values())));
                log.info("??????????????????" + JsonUtil.arrayToJsonStr(new ArrayList<PriceLibraryDTO>(updateMap.values())));
                log.info("??????????????????" + JsonUtil.arrayToJsonStr(new ArrayList<PriceLibraryDTO>(addMap.values())));
            }

        }finally {
            redisUtil.unLock("ceeaGeneratePriceLibrary" + approvalNo);
        }

    }


    /**
     * ????????????????????????
     * @param item
     * @param p
     */
    private Map<String,List<PriceLibraryDTO>> leftCross(PriceLibraryDTO item,PriceLibrary p){
        List<PriceLibraryDTO> addList = new LinkedList<PriceLibraryDTO>(){{
            add(item);
        }};

        List<PriceLibraryDTO> updateList = new LinkedList<PriceLibraryDTO>();
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("PRICE_LIBRARY_ID",p.getPriceLibraryId());
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = priceLibraryPaymentTermService.list(wrapper);
        p.setExpirationDate(sub(item.getEffectiveDate()));
        PriceLibraryDTO priceLibraryDTO = new PriceLibraryDTO();
        BeanUtils.copyProperties(p,priceLibraryDTO);
        priceLibraryDTO.setPriceLibraryPaymentTermList(priceLibraryPaymentTermList);
        updateList.add(priceLibraryDTO);

        Map<String,List<PriceLibraryDTO>> result = new HashMap<>();
        result.put("ADD",addList);
        result.put("UPDATE",updateList);
        result.put("REMOVE",Collections.EMPTY_LIST);
        return result;
    }

    /**
     * ????????????????????????
     * @param item
     * @param p
     */
    private Map<String,List<PriceLibraryDTO>> rightCross(PriceLibraryDTO item,PriceLibrary p){
        List<PriceLibraryDTO> addList = new LinkedList<PriceLibraryDTO>(){{
            add(item);
        }};

        List<PriceLibraryDTO> updateList = new LinkedList<PriceLibraryDTO>();
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("PRICE_LIBRARY_ID",p.getPriceLibraryId());
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = priceLibraryPaymentTermService.list(wrapper);
        p.setEffectiveDate(add(item.getExpirationDate()));
        PriceLibraryDTO priceLibraryDTO = new PriceLibraryDTO();
        BeanUtils.copyProperties(p,priceLibraryDTO);
        priceLibraryDTO.setPriceLibraryPaymentTermList(priceLibraryPaymentTermList);
        updateList.add(priceLibraryDTO);

        Map<String,List<PriceLibraryDTO>> result = new HashMap<>();
        result.put("ADD",addList);
        result.put("UPDATE",updateList);
        result.put("REMOVE",Collections.EMPTY_LIST);
        return result;

    }

    /**
     * ??????
     * @param item
     * @param p
     */
    private Map<String,List<PriceLibraryDTO>> contains(PriceLibraryDTO item,PriceLibrary p){
        List<PriceLibraryDTO> addList = new LinkedList<PriceLibraryDTO>(){{
            add(item);
        }};

        List<PriceLibraryDTO> removeList = new LinkedList<PriceLibraryDTO>();
        PriceLibraryDTO pRemove = new PriceLibraryDTO();
        BeanUtils.copyProperties(p,pRemove);
        removeList.add(pRemove);

        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("PRICE_LIBRARY_ID",p.getPriceLibraryId());
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = priceLibraryPaymentTermService.list(wrapper);
        PriceLibraryDTO p1 = new PriceLibraryDTO();
        BeanUtils.copyProperties(p,p1);
        p1.setExpirationDate(sub(item.getEffectiveDate()));
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList1 = deepCopy(priceLibraryPaymentTermList);
        p1.setPriceLibraryPaymentTermList(priceLibraryPaymentTermList1);

        PriceLibraryDTO p2 = new PriceLibraryDTO();
        BeanUtils.copyProperties(p,p2);
        p2.setEffectiveDate(add(item.getExpirationDate()));
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList2 = deepCopy(priceLibraryPaymentTermList);
        p2.setPriceLibraryPaymentTermList(priceLibraryPaymentTermList2);

        LocalDate p1EffectiveDate = DateUtil.dateToLocalDate(p1.getEffectiveDate());
        LocalDate p1ExpirationDate = DateUtil.dateToLocalDate(p1.getExpirationDate());
        if(isBeforeAndEquals(p1EffectiveDate,p1ExpirationDate)){
            addList.add(p1);
        }

        LocalDate p2EffectiveDate = DateUtil.dateToLocalDate(p2.getEffectiveDate());
        LocalDate p2ExpirationDate = DateUtil.dateToLocalDate(p2.getExpirationDate());
        if(isBeforeAndEquals(p2EffectiveDate,p2ExpirationDate)){
            addList.add(p2);
        }

        Map<String,List<PriceLibraryDTO>> result = new HashMap<>();
        result.put("ADD",addList);
        result.put("UPDATE",Collections.EMPTY_LIST);
        result.put("REMOVE",removeList);
        return result;

    }

    private List<PriceLibraryPaymentTerm> deepCopy(List<PriceLibraryPaymentTerm> paymentTermList){
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = new LinkedList<>();
        for(int i=0;i<paymentTermList.size();i++){
            PriceLibraryPaymentTerm p = new PriceLibraryPaymentTerm();
            BeanUtils.copyProperties(paymentTermList.get(i),p);
            priceLibraryPaymentTermList.add(p);
        }
        return priceLibraryPaymentTermList;
    }

    /**
     * ?????????
     * @param item
     * @param p
     */
    private Map<String,List<PriceLibraryDTO>> included(PriceLibraryDTO item,PriceLibrary p){
        List<PriceLibraryDTO> addList = new LinkedList<PriceLibraryDTO>(){{
            add(item);
        }};

        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("PRICE_LIBRARY_ID",p.getPriceLibraryId());
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = priceLibraryPaymentTermService.list(wrapper);

        PriceLibraryDTO rmPriceLibraryDTO = new PriceLibraryDTO();
        BeanUtils.copyProperties(p,rmPriceLibraryDTO);
        rmPriceLibraryDTO.setPriceLibraryPaymentTermList(priceLibraryPaymentTermList);
        List<PriceLibraryDTO> removeList = new LinkedList<>();
        removeList.add(rmPriceLibraryDTO);

        Map<String,List<PriceLibraryDTO>> result = new HashMap<>();
        result.put("ADD",addList);
        result.put("REMOVE",removeList);
        result.put("UPDATE",Collections.EMPTY_LIST);
        return result;
    }

    /**
     * ?????????
     * @param item
     * @param p
     */
    private Map<String,List<PriceLibraryDTO>> noCross(PriceLibraryDTO item,PriceLibrary p){
        List<PriceLibraryDTO> addList = new LinkedList<PriceLibraryDTO>(){{
            add(item);
        }};

        Map<String,List<PriceLibraryDTO>> result = new HashMap<>();
        result.put("ADD",addList);
        result.put("REMOVE",Collections.EMPTY_LIST);
        result.put("UPDATE",Collections.EMPTY_LIST);
        return result;
    }


    /**
     * ???????????????????????????
     * @param item
     * @param priceLibrary
     * @return
     */
    private Integer whichCrossType(PriceLibraryDTO item, PriceLibrary priceLibrary) {
        LocalDate effectiveDate1 = DateUtil.dateToLocalDate(priceLibrary.getEffectiveDate());
        LocalDate expirationDate1 = DateUtil.dateToLocalDate(priceLibrary.getExpirationDate());

        LocalDate effectiveDate2 = DateUtil.dateToLocalDate(item.getEffectiveDate());
        LocalDate expirationDate2 = DateUtil.dateToLocalDate(item.getExpirationDate());

        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
                isBeforeAndEquals(effectiveDate2,expirationDate1) &&
                isBeforeAndEquals(expirationDate1,expirationDate2)
        ){
            //?????????
            return CROSSTYPE.LEFTCROSS.value;
        }else if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
                isBeforeAndEquals(effectiveDate1,expirationDate2) &&
                isBeforeAndEquals(expirationDate2,expirationDate1)
        ){
            //?????????
            return CROSSTYPE.RIGHTCROSS.value;
        }else if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
                isBeforeAndEquals(expirationDate2,expirationDate1)
        ){
            //??????
            return CROSSTYPE.CONTAINS.value;
        }else if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
                isBeforeAndEquals(expirationDate1,expirationDate2)
        ){
            //?????????
            return CROSSTYPE.INCLUDED.value;
        }else {
            //?????????
            return CROSSTYPE.NOCROSS.value;
        }

    }

    /**
     * ?????????????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ?????????????????? ???key???
     * @param priceLibraryList
     * @return
     */
    private Map<String,PriceLibrary> getUpdateCategoryPriceLibraryMap1(List<PriceLibrary> priceLibraryList){
        Map<String,PriceLibrary> map = new HashMap<>();
        for(int i=0;i<priceLibraryList.size();i++){
            PriceLibrary priceLibrary = priceLibraryList.get(i);
            String startTime = String.valueOf(DateUtil.dateToLocalDate(priceLibrary.getEffectiveDate()));
            String endTime = String.valueOf(DateUtil.dateToLocalDate(priceLibrary.getExpirationDate()));
            StringBuffer key = new StringBuffer();
            key.append(priceLibrary.getItemCode())
                    .append(priceLibrary.getItemDesc())
                    .append(priceLibrary.getVendorCode())
                    .append(priceLibrary.getCeeaOrgId())
                    .append(priceLibrary.getCeeaOrganizationId())
                    .append(priceLibrary.getCeeaArrivalPlace())
                    .append(priceLibrary.getPriceType())
                    .append(startTime)
                    .append(endTime);
            map.put(key.toString(),priceLibrary);
        }
        return map;
    }

    /**
     * (???????????????)
     * ?????????????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ?????????????????? ???key???
     * @param priceLibraryList
     * @return
     */
    private Map<String,PriceLibraryDTO> getUpdateCategoryPriceLibraryMap2(List<PriceLibraryDTO> priceLibraryList){
        Map<String,PriceLibraryDTO> map = new HashMap<>();
        for(int i=0;i<priceLibraryList.size();i++){
            PriceLibraryDTO priceLibrary = priceLibraryList.get(i);
            String startTime = String.valueOf(DateUtil.dateToLocalDate(priceLibrary.getEffectiveDate()));
            String endTime = String.valueOf(DateUtil.dateToLocalDate(priceLibrary.getExpirationDate()));
            StringBuffer key = new StringBuffer();
            key.append(priceLibrary.getItemCode())
                    .append(priceLibrary.getItemDesc())
                    .append(priceLibrary.getVendorCode())
                    .append(priceLibrary.getCeeaOrgId())
                    .append(priceLibrary.getCeeaOrganizationId())
                    .append(priceLibrary.getCeeaArrivalPlace())
                    .append(priceLibrary.getPriceType())
                    .append(startTime)
                    .append(endTime);
            map.put(key.toString(),priceLibrary);
        }
        return map;
    }

    /**
     * ?????????????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ?????????????????? ???key???
     * @param priceLibraryList
     * @return
     */
    private Map<String,PriceLibrary> getNotUpdateCategoryPriceLibraryMap1(List<PriceLibrary> priceLibraryList){
        Map<String,PriceLibrary> map = new HashMap<>();
        for(int i=0;i<priceLibraryList.size();i++){
            PriceLibrary priceLibrary = priceLibraryList.get(i);
            String startTime = String.valueOf(DateUtil.dateToLocalDate(priceLibrary.getEffectiveDate()));
            String endTime = String.valueOf(DateUtil.dateToLocalDate(priceLibrary.getExpirationDate()));
            StringBuffer key = new StringBuffer();
            key.append(priceLibrary.getItemCode())
                    .append(priceLibrary.getVendorCode())
                    .append(priceLibrary.getCeeaOrgId())
                    .append(priceLibrary.getCeeaOrganizationId())
                    .append(priceLibrary.getCeeaArrivalPlace())
                    .append(priceLibrary.getPriceType())
                    .append(startTime)
                    .append(endTime);
            map.put(key.toString(),priceLibrary);
        }
        return map;
    }

    /**
     * (???????????????)
     * ?????????????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? + ?????????????????? + ?????????????????? ???key???
     * @param priceLibraryList
     * @return
     */
    private Map<String,PriceLibraryDTO> getNotUpdateCategoryPriceLibraryMap2(List<PriceLibraryDTO> priceLibraryList){
        Map<String,PriceLibraryDTO> map = new HashMap<>();
        for(int i=0;i<priceLibraryList.size();i++){
            PriceLibraryDTO priceLibrary = priceLibraryList.get(i);
            String startTime = String.valueOf(DateUtil.dateToLocalDate(priceLibrary.getEffectiveDate()));
            String endTime = String.valueOf(DateUtil.dateToLocalDate(priceLibrary.getExpirationDate()));
            StringBuffer key = new StringBuffer();
            key.append(priceLibrary.getItemCode())
                    .append(priceLibrary.getVendorCode())
                    .append(priceLibrary.getCeeaOrgId())
                    .append(priceLibrary.getCeeaOrganizationId())
                    .append(priceLibrary.getCeeaArrivalPlace())
                    .append(priceLibrary.getPriceType())
                    .append(startTime)
                    .append(endTime);
            map.put(key.toString(),priceLibrary);
        }
        return map;
    }


    /**
     * ??????????????????????????????
     * @param priceLibraryDTO
     */
    public void savePriceLibraryAndPaymentItems(PriceLibraryDTO priceLibraryDTO){
        LocalDate startDate = DateUtil.dateToLocalDate(priceLibraryDTO.getEffectiveDate());
        LocalDate endDate = DateUtil.dateToLocalDate(priceLibraryDTO.getExpirationDate());
        if(!isBeforeAndEquals(startDate,endDate)){
            return;
        }

        Long priceLibraryId = IdGenrator.generate();
        priceLibraryMapper.insert(priceLibraryDTO.setPriceLibraryId(priceLibraryId));
        if(CollectionUtils.isNotEmpty(priceLibraryDTO.getPriceLibraryPaymentTermList())){
            priceLibraryDTO.getPriceLibraryPaymentTermList().forEach(item -> {
                Long priceLibraryPaymentTermId = IdGenrator.generate();
                item.setPriceLibraryId(priceLibraryId)
                        .setPriceLibraryPaymentTermId(priceLibraryPaymentTermId);
            });
            priceLibraryPaymentTermService.saveBatch(priceLibraryDTO.getPriceLibraryPaymentTermList());
        }
    }

    /**
     * ????????????????????????????????????
     * @param priceLibraryDTOList
     */
    public void saveBatchPriceLibraryAndPaymentItems(List<PriceLibraryDTO> priceLibraryDTOList){
        //????????? ????????????????????????????????????????????????
        priceLibraryDTOList = priceLibraryDTOList
                .stream()
                .filter(item -> isBeforeAndEquals(DateUtil.dateToLocalDate(item.getEffectiveDate()),DateUtil.dateToLocalDate(item.getExpirationDate())))
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty(priceLibraryDTOList)){
            return;
        }


        List<PriceLibrary> priceLibraryList = new LinkedList<>();
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = new LinkedList<>();
        for(PriceLibraryDTO priceLibraryDTO : priceLibraryDTOList){
            Long priceLibraryId = IdGenrator.generate();
            priceLibraryDTO.setPriceLibraryId(priceLibraryId);
            if(CollectionUtils.isNotEmpty(priceLibraryDTO.getPriceLibraryPaymentTermList())){
                priceLibraryDTO.getPriceLibraryPaymentTermList().forEach(item -> {
                    Long priceLibraryPaymentTermId = IdGenrator.generate();
                    item.setPriceLibraryId(priceLibraryId)
                            .setPriceLibraryPaymentTermId(priceLibraryPaymentTermId);
                });
            }

            PriceLibrary p = new PriceLibrary();
            BeanUtils.copyProperties(priceLibraryDTO,p);
            priceLibraryList.add(p);
            priceLibraryPaymentTermList.addAll(priceLibraryDTO.getPriceLibraryPaymentTermList());
        }
        this.saveBatch(priceLibraryList);
        priceLibraryPaymentTermService.saveBatch(priceLibraryPaymentTermList);

    }

    /**
     * ??????????????????????????????
     * @param priceLibraryDTO
     */
    public void updatePriceLibraryAndPaymentItems(PriceLibraryDTO priceLibraryDTO){
        //????????? ????????????????????????????????????????????????
        LocalDate startDate = DateUtil.dateToLocalDate(priceLibraryDTO.getEffectiveDate());
        LocalDate endDate = DateUtil.dateToLocalDate(priceLibraryDTO.getExpirationDate());
        if(!isBeforeAndEquals(startDate,endDate)){
            return;
        }

        priceLibraryMapper.updateById(priceLibraryDTO);
        Long priceLibraryId = priceLibraryDTO.getPriceLibraryId();
        QueryWrapper<PriceLibraryPaymentTerm> wrapper = new QueryWrapper();
        wrapper.eq("PRICE_LIBRARY_ID",priceLibraryId);
        priceLibraryPaymentTermService.remove(wrapper);
        if(CollectionUtils.isNotEmpty(priceLibraryDTO.getPriceLibraryPaymentTermList())){
            priceLibraryDTO.getPriceLibraryPaymentTermList().forEach(item -> {
                Long priceLibraryPaymentTermId = IdGenrator.generate();
                item.setPriceLibraryId(priceLibraryId)
                        .setPriceLibraryPaymentTermId(priceLibraryPaymentTermId);
            });
            priceLibraryPaymentTermService.saveBatch(priceLibraryDTO.getPriceLibraryPaymentTermList());
        }
    }

    public void updateBatchPriceLibraryAndPaymentItems(List<PriceLibraryDTO> priceLibraryDTOList){
        //????????? ????????????????????????????????????????????????
        priceLibraryDTOList = priceLibraryDTOList
                .stream()
                .filter(item -> isBeforeAndEquals(DateUtil.dateToLocalDate(item.getEffectiveDate()),DateUtil.dateToLocalDate(item.getExpirationDate())))
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty(priceLibraryDTOList)){
            return;
        }

        List<Long> priceLibraryIds = new LinkedList<>();
        List<PriceLibrary> priceLibraryList = new LinkedList<>();
        List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = new LinkedList<>();
        for(PriceLibraryDTO priceLibraryDTO : priceLibraryDTOList){
            Long priceLibraryId = priceLibraryDTO.getPriceLibraryId();
            priceLibraryIds.add(priceLibraryId);

            PriceLibrary p = new PriceLibrary();
            BeanUtils.copyProperties(priceLibraryDTO,p);
            priceLibraryList.add(p);

            if(CollectionUtils.isNotEmpty(priceLibraryDTO.getPriceLibraryPaymentTermList())){
                priceLibraryDTO.getPriceLibraryPaymentTermList().forEach(item -> {
                    Long priceLibraryPaymentTermId = IdGenrator.generate();
                    item.setPriceLibraryId(priceLibraryId)
                            .setPriceLibraryPaymentTermId(priceLibraryPaymentTermId);
                });
                priceLibraryPaymentTermList.addAll(priceLibraryDTO.getPriceLibraryPaymentTermList());
            }
        }
        this.updateBatchById(priceLibraryList);
        QueryWrapper<PriceLibraryPaymentTerm> wrapper = new QueryWrapper();
        wrapper.in("PRICE_LIBRARY_ID",priceLibraryIds);
        priceLibraryPaymentTermService.remove(wrapper);
        priceLibraryPaymentTermService.saveBatch(priceLibraryPaymentTermList);

    }


    /**
     * ??????????????????????????????
     * @param priceLibraryDTO
     */
    public void removePriceLibraryAndPaymentItems(PriceLibraryDTO priceLibraryDTO){
        Long priceLibraryId = priceLibraryDTO.getPriceLibraryId();
        priceLibraryMapper.deleteById(priceLibraryId);
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("PRICE_LIBRARY_ID",priceLibraryId);
        priceLibraryPaymentTermService.remove(wrapper);
    }

    /**
     * ????????????????????????????????????
     * @param priceLibraryDTOList
     */
    public void removeBatchPriceLibraryAndPaymentItems(List<PriceLibraryDTO> priceLibraryDTOList){
        List<Long> priceLibraryIds = new LinkedList<>();
        for(PriceLibraryDTO priceLibraryDTO : priceLibraryDTOList){
            priceLibraryIds.add(priceLibraryDTO.getPriceLibraryId());
        }
        this.removeByIds(priceLibraryIds);
        QueryWrapper<PriceLibraryPaymentTerm> wrapper = new QueryWrapper<>();
        wrapper.in("PRICE_LIBRARY_ID",priceLibraryIds);
        priceLibraryPaymentTermService.remove(wrapper);

    }


    /**
     * ??????????????????????????????
     * @param priceLibraryList
     */
    public void checkIfFormatCorrect(List<PriceLibraryDTO> priceLibraryList){
        priceLibraryList.forEach(item -> {
            LocalDate effective = DateUtil.dateToLocalDate(item.getEffectiveDate());
            LocalDate expiration = DateUtil.dateToLocalDate(item.getExpirationDate());
            if(effective.isAfter(expiration)){
                throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????"));
            }

        });
    }

    /**
     * ??????list????????????????????????????????????????????????
     * @param priceLibraryList
     * @param priceLibrary
     */
    public void checkIfOverlapping(List<PriceLibrary> priceLibraryList,PriceLibrary priceLibrary){
        int index = 0;
        for(PriceLibrary pl:priceLibraryList){
            if(ifDateOverlapping(pl,priceLibrary)){
                index ++;
            }
        }
        if(index >= 2){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????"));
        }
    }

    /**
     * ??????list???????????????????????????????????????
     * @param priceLibraryList
     */
    public void checkIfOverlapping(List<PriceLibraryDTO> priceLibraryList){
        /*?????? ???????????? + ???????????? + ??????????????? + ????????????id + ????????????id + ???????????? + ???????????? */
        List<List<PriceLibrary>> lists = new ArrayList<>();
        priceLibraryList.forEach(insertItem -> {
            boolean add = true;
            for(int i=0;i<lists.size();i++){
                List<PriceLibrary> list = lists.get(i);
                if(list.size() > 0){
                    PriceLibrary priceLibrary = list.get(0);
                    if(ifCommonGroup(priceLibrary,insertItem)){
                        list.add(insertItem);
                        add = false;
                        break;
                    }
                }
            }
            if(add){
                List<PriceLibrary> list = new ArrayList<>();
                list.add(insertItem);
                lists.add(list);
            }
        });
        log.info("lists.size:" + lists.size());
        log.info("lists:" + lists);

        lists.forEach(list -> {
            for(int i=0;i<list.size();i++){
                PriceLibrary priceLibrary1 = list.get(i);
                for(int j=i+1;j<list.size();j++){
                    PriceLibrary priceLibrary2 = list.get(j);
                    if(ifDateOverlapping(priceLibrary1,priceLibrary2)){
                        throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????????????????"));
                    }
                }
            }
        });
    }

    /**
     * ??????????????????????????????
     * @param priceLibrary1
     * @param priceLibrary2
     * @return
     */
    public boolean ifDateOverlapping(PriceLibrary priceLibrary1,PriceLibrary priceLibrary2){
        LocalDate effectiveDate1 = DateUtil.dateToLocalDate(priceLibrary1.getEffectiveDate());
        LocalDate expirationDate1 = DateUtil.dateToLocalDate(priceLibrary1.getExpirationDate());

        LocalDate effectiveDate2 = DateUtil.dateToLocalDate(priceLibrary2.getEffectiveDate());
        LocalDate expirationDate2 = DateUtil.dateToLocalDate(priceLibrary2.getExpirationDate());
        /*?????????*/
        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
                isBeforeAndEquals(effectiveDate2,expirationDate1) &&
                isBeforeAndEquals(expirationDate1,expirationDate2)
        ){
            log.info("priceLibrary1:" + priceLibrary1);
            log.info("priceLibrary2:" + priceLibrary2);
            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
            return true;
        }
        /*?????????*/
        if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
                isBeforeAndEquals(effectiveDate1,expirationDate2) &&
                isBeforeAndEquals(expirationDate2,expirationDate1)
        ){
            log.info("priceLibrary1:" + priceLibrary1);
            log.info("priceLibrary2:" + priceLibrary2);
            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
            return true;
        }
        /*??????*/
        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
                isBeforeAndEquals(expirationDate2,expirationDate1)
        ){
            log.info("priceLibrary1:" + priceLibrary1);
            log.info("priceLibrary2:" + priceLibrary2);
            log.info("???????????????-??????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
            return true;
        }
        /*?????????*/
        if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
                isBeforeAndEquals(expirationDate1,expirationDate2)
        ){
            log.info("priceLibrary1:" + priceLibrary1);
            log.info("priceLibrary2:" + priceLibrary2);
            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
            return true;
        }
        return false;
    }

    /**
     * @param localDate1
     * @param localDate2
     * @return
     */
    public boolean isBeforeAndEquals(LocalDate localDate1,LocalDate localDate2){
        return localDate1.isBefore(localDate2) || localDate1.isEqual(localDate2);
    }

    public boolean isBefore(LocalDate localDate1,LocalDate localDate2){
        return localDate1.isBefore(localDate2);
    }

    /**
     * ????????????????????????????????????
     * @param priceLibrary1
     * @param priceLibrary2
     * @return
     */
    public boolean ifCommonGroup(PriceLibrary priceLibrary1,PriceLibrary priceLibrary2){
        if(!priceLibrary1.getItemCode().equals(priceLibrary2.getItemCode())){
            return false;
        }
        if(!priceLibrary1.getItemDesc().equals(priceLibrary2.getItemDesc())){
            return false;
        }
        if(!priceLibrary1.getVendorCode().equals(priceLibrary2.getVendorCode())){
            return false;
        }
        if(Long.compare(priceLibrary1.getCeeaOrgId(),priceLibrary2.getCeeaOrgId()) != 0){
            return false;
        }
        if(Long.compare(priceLibrary1.getCeeaOrganizationId(),priceLibrary2.getCeeaOrganizationId()) != 0){
            return false;
        }
        if(Objects.nonNull(priceLibrary1.getCeeaArrivalPlace()) &&
                !priceLibrary1.getCeeaArrivalPlace().equals(priceLibrary2.getCeeaArrivalPlace())){
            return false;
        }
        return !Objects.nonNull(priceLibrary1.getPriceType()) ||
                priceLibrary1.getPriceType().equals(priceLibrary2.getPriceType());
    }

    @Override
    public String ifHasPrice(PriceLibrary priceLibrary) {
        Integer count = priceLibraryMapper.findEffectivePriceCount(priceLibrary);
        if(count != 0){
            return "Y";
        }else{
            return "N";
        }
    }

    @Override
    public List<PriceLibrary> listEffectivePrice(PriceLibrary priceLibrary) {
        return priceLibraryMapper.findEffectivePrice(priceLibrary);
    }

    @Override
    public void ceeaUpdateBatch(List<PriceLibrary> priceLibraryList) {
        this.updateBatchById(priceLibraryList);
    }

    public Date sub(Date date){
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.add(Calendar.DAY_OF_MONTH, -1);
        Date dt1 = rightNow.getTime();
        return dt1;
    }

    public Date add(Date date){
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.add(Calendar.DAY_OF_MONTH, 1);
        Date dt1 = rightNow.getTime();
        return dt1;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long priceLibraryId) {
        removeById(priceLibraryId);
        iPriceLadderPriceService.deleteByPriceLibraryId(priceLibraryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(List<PriceLibraryAddParam> params) {

        List<PriceLibrary> priceLibraryUpdates = new ArrayList<>();
        List<PriceLadderPrice> ladderPriceUpdates = new ArrayList<>();
        params.forEach(priceLibraryAddParam -> {
            PriceLibrary library = new PriceLibrary();
            BeanUtils.copyProperties(priceLibraryAddParam, library);

            if (CollectionUtils.isNotEmpty(priceLibraryAddParam.getLadderPrices())) {
                priceLibraryAddParam.getLadderPrices().forEach(ladder -> {
                    PriceLadderPrice ladderPrice = new PriceLadderPrice();
                    BeanUtils.copyProperties(ladder, ladderPrice);
                    ladderPriceUpdates.add(ladderPrice);
                });
            }
            priceLibraryUpdates.add(library);
        });

        updateBatchById(priceLibraryUpdates);
        if (CollectionUtils.isNotEmpty(ladderPriceUpdates)) {
            iPriceLadderPriceService.updateBatchById(ladderPriceUpdates);
        }
    }

    @Override
    public PriceLibrary getPriceLibraryByParam(NetPriceQueryDTO netPriceQueryDTO) {
        QueryWrapper<PriceLibrary> queryWrapper = new QueryWrapper<PriceLibrary>(new PriceLibrary().setItemId(netPriceQueryDTO.getMaterialId()).
                setOrganizationId(netPriceQueryDTO.getOrganizationId()).setVendorId(netPriceQueryDTO.getVendorId()));
        if (netPriceQueryDTO.getRequirementDate() != null) {
            queryWrapper.le("EFFECTIVE_DATE", netPriceQueryDTO.getRequirementDate());
            queryWrapper.ge("EXPIRATION_DATE", netPriceQueryDTO.getRequirementDate());
        }
        queryWrapper.orderByAsc("NOTAX_PRICE");
        List<PriceLibrary> priceLibraryList = this.list(queryWrapper);
        if (CollectionUtils.isNotEmpty(priceLibraryList)) {
            return priceLibraryList.get(0);
        }
        return null;
    }

    /**
     * @Description ??????????????????
     * @Param [priceLibrary]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.25 16:50
     */
    @Override
    public PriceLibrary getOnePriceLibrary(PriceLibrary priceLibrary) {
    	List<PriceLibrary> list = this.list(new QueryWrapper<>(priceLibrary));
    	if (CollectionUtils.isNotEmpty(list)) {
    		for (PriceLibrary pl : list) {
    			if (pl.getEffectiveDate() != null && pl.getExpirationDate() != null) {
    				long ms = System.currentTimeMillis();
					if (ms >= pl.getEffectiveDate().getTime() && ms <= pl.getExpirationDate().getTime()) { // ???????????????
						return pl;
					}
				}
			}
		}
        return null;
    }

    @Override
    public List<PriceLibrary> listPriceLibraryByParam(List<NetPriceQueryDTO> netPriceQueryDTOList) {
        List<PriceLibrary> priceLibraryList = new ArrayList<>();
        netPriceQueryDTOList.forEach(dto -> {
            PriceLibrary priceLibrary = this.getPriceLibraryByParam(dto);
            if(priceLibrary != null) {
                priceLibraryList.add(priceLibrary);
            }
        });
        return priceLibraryList;
    }

    @Override
    public List<PriceLibrary> listPriceLibrary(NetPriceQueryDTO netPriceQueryDTO) {
        QueryWrapper<PriceLibrary> queryWrapper = new QueryWrapper<PriceLibrary>(new PriceLibrary().setItemId(netPriceQueryDTO.getMaterialId()).
                setCeeaOrgId(netPriceQueryDTO.getOrganizationId()).setVendorId(netPriceQueryDTO.getVendorId()));
        if (netPriceQueryDTO.getRequirementDate() != null) {
            queryWrapper.lt("EFFECTIVE_DATE", netPriceQueryDTO.getRequirementDate());
            queryWrapper.gt("EXPIRATION_DATE", netPriceQueryDTO.getRequirementDate());
        }
        return this.list(queryWrapper);
    }

    @Override
    public List<PriceLibrary> queryByContract(PriceLibraryParam priceLibraryParam) {
        // ????????????ID
        Long organizationId = priceLibraryParam.getOrganizationId();
        String sourceNo = priceLibraryParam.getSourceNo();
        Long vendorId = priceLibraryParam.getVendorId();
        List<Long> longs = new ArrayList<>();
        if (null != organizationId) {
            List<Organization> organizations = baseClient.queryIvnByOuId(vendorId);
            if(CollectionUtils.isNotEmpty(organizations)){
                organizations.forEach(organization -> longs.add(organization.getOrganizationId()));
            }else {
                return null;
            }
        }
        QueryWrapper<PriceLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtil.notEmpty(sourceNo),"SOURCE_NO",sourceNo);
        queryWrapper.eq(StringUtil.notEmpty(vendorId),"VENDOR_ID",vendorId);
        queryWrapper.in(CollectionUtils.isNotEmpty(longs),"ORGANIZATION_ID",longs);
        return this.list(queryWrapper);
    }

    @Override
    public List<BidFrequency> getThreeYearsBidFrequency(Long vendorId) throws ParseException {
        Assert.notNull(vendorId,"vendorId????????????");
        int year = LocalDate.now().getYear();
        ArrayList<BidFrequency> frequencies = new ArrayList<>();
        for(int i =0;i < 3 ;i++){
            Map<String, Object> param = getBetweenDate(year);
            param.put("vendorId",vendorId);
            Integer sum = this.baseMapper.getThreeYearsBidFrequency(param);
            BidFrequency bidFrequency = new BidFrequency();
            bidFrequency.setYear(year);
            bidFrequency.setFrequency(sum);
            frequencies.add(bidFrequency);
            year --;
        }
        return frequencies;
    }

    @Transactional
    @Override
    public void putOnShelves(PriceLibraryPutOnShelvesDTO priceLibraryPutOnShelvesDTO) {
        Boolean flag = false;
        List<PriceLibrary> priceLibraryList = priceLibraryPutOnShelvesDTO.getPriceLibraryList();
        Long contractHeadId = priceLibraryPutOnShelvesDTO.getContractHeadId();
        ContractDTO contractDTO = contractClient.getContractDetail(contractHeadId);
        //??????????????????
        List<ContractMaterial> contractMaterialList = contractDTO.getContractMaterials();
        ContractHead contractHead =  contractDTO.getContractHead();
        String vendorCode = contractHead.getVendorCode();
        String contractCode = contractHead.getContractCode();
        for(PriceLibrary priceLibrary :priceLibraryList){
            String itemCode = priceLibrary.getItemCode();
            String ceeaOrgCode = priceLibrary.getCeeaOrgCode();
            String ceeaOrganizationCode = priceLibrary.getCeeaOrganizationCode();
            String itemDesc = priceLibrary.getItemDesc();
            //??????--???????????????+????????????+?????????+????????????+???????????????????????????????????????????????????????????????
            if(vendorCode.equals(priceLibrary.getVendorCode())){
                if(CollectionUtils.isNotEmpty(contractMaterialList)){
                    for(ContractMaterial contractMaterial:contractMaterialList){
                        if(itemDesc.equals(contractMaterial.getMaterialName())&&itemCode.equals(contractMaterial.getMaterialCode())&&ceeaOrgCode.equals(contractMaterial.getBuCode())&&ceeaOrganizationCode.equals(contractMaterial.getInvCode())){
                            //????????????????????????
                            flag = true;
                            priceLibrary.setCeeaIfUse("Y");
                            int row = this.getBaseMapper().updateById(priceLibrary);
                            if(row > 0){
                                updateMaterialItemInfo(priceLibrary,contractCode);
                            }
                        }
                    }
                }
            }
        }
        // ??????????????????
        if(!flag){
            throw new BaseException("R015", "????????????????????????????????????????????????????????????????????????");
        }
    }

    @Transactional
    @Override
    public void pullOffShelves(List<PriceLibrary> priceLibraryList) {
        for(int i = 0; i < priceLibraryList.size(); i++){
            PriceLibrary priceLibrary = priceLibraryList.get(i);
            if(!StringUtil.isEmpty(priceLibrary.getCeeaIfUse())&&priceLibrary.getCeeaIfUse().equals("Y")){
                priceLibrary.setCeeaIfUse("N");
                int row = this.getBaseMapper().updateById(priceLibrary);
                if(row > 0){
                    //??????????????????????????????????????????????????????
                    MaterialItem materialItem = baseClient.findMaterialItemByMaterialCode(priceLibrary.getItemCode());
                    materialItem.setCeeaIfCatalogMaterial("N");
                    baseClient.updateMaterialItemById(materialItem);
                }
            }
        }
    }

    @Override
    public void importModelDownload(HttpServletResponse httpServletResponse) throws Exception{
        String fileName = "????????????????????????";
        ArrayList<PriceLibraryModelDto> priceLibraryModelDtos = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(httpServletResponse, fileName);
        List<Integer> rows = Arrays.asList(0);
        List<Integer> columns = Arrays.asList(0,1,2,4,5,7,8,9,10,13,14,15,16,17,18,19);
        //?????????????????????????????????
        PriceLibraryModelDto priceLibraryModelDto = new PriceLibraryModelDto();
        priceLibraryModelDto.setItemCode("??????");
        priceLibraryModelDto.setItemDesc("??????");
        priceLibraryModelDto.setVendorCode("????????????????????????ERP???????????????");
        priceLibraryModelDto.setVendorName("?????????");
        priceLibraryModelDto.setCeeaOrgCode("??????????????????????????????");
        priceLibraryModelDto.setCeeaOrganizationCode("??????????????????????????????");
        priceLibraryModelDto.setCeeaArrivalPlace("????????????????????????????????????????????????????????????????????????????????????????????????");
        priceLibraryModelDto.setTaxPrice("????????????????????????");
        priceLibraryModelDto.setTaxKey("???????????????????????????XXX");
        priceLibraryModelDto.setTaxRate("???????????????????????????????????????13???16???17???????????????????????????%?????????");
        priceLibraryModelDto.setCurrencyCode("??????????????????????????????CNY,USD,HKD???");
        priceLibraryModelDto.setCeeaAllocationType("???????????????????????????????????????\n" + "1??????????????????\n" + "2??????????????????");
        priceLibraryModelDto.setEffectiveDate("????????????????????????2020-09-04????????????????????????????????????????????????");
        priceLibraryModelDto.setExpirationDate("????????????????????????2020-09-04????????????????????????????????????????????????");
        priceLibraryModelDto.setPaymentTerms("???????????????????????????????????????????????????????????????");
        priceLibraryModelDto.setPaymentType("??????????????????");
        priceLibraryModelDto.setPaymentDays("??????????????????");
        priceLibraryModelDto.setCeeaLt("??????????????????????????????");
        priceLibraryModelDto.setFrameworkAgreement("???????????????????????????????????????????????????");
        priceLibraryModelDtos.add(priceLibraryModelDto);
        TitleColorSheetWriteHandler titleColorSheetWriteHandler = new TitleColorSheetWriteHandler(rows,columns, IndexedColors.RED.index);
        EasyExcelUtil.writeExcelWithModel(outputStream,priceLibraryModelDtos,PriceLibraryModelDto.class,fileName,titleColorSheetWriteHandler);
    }

    @Override
    public void importExcel(MultipartFile file) throws Exception {
        try {
            //??????????????????
            String originalFilename = file.getOriginalFilename();
            if (!EasyExcelUtil.isExcel(originalFilename)) {
                throw new BaseException("??????????????????Excel??????");
            }
            InputStream inputStream = file.getInputStream();
            List<PriceLibrary> updatePriceLibrarys = new ArrayList<>();
            List<PriceLibrary> addPriceLibrarys = new ArrayList<>();
            List<Object> objectList = EasyExcelUtil.readExcelWithModel(inputStream, PriceLibraryModelDto.class);
            objectList.forEach(object ->{
                try {
                    if(object != null){
                        PriceLibraryModelDto priceLibraryModelDto = (PriceLibraryModelDto) object;
                        checkRequireParam(priceLibraryModelDto);
                        PriceLibrary priceLibrary = checkImportRowIfExist(priceLibraryModelDto);
                        if(null != priceLibrary){
                            priceLibrary.setVendorName(priceLibraryModelDto.getVendorName());
                            priceLibrary.setTaxPrice(new BigDecimal(priceLibraryModelDto.getTaxPrice()));
                            priceLibrary.setTaxKey(priceLibraryModelDto.getTaxKey());
                            priceLibrary.setTaxRate(priceLibraryModelDto.getTaxRate());
                            priceLibrary.setCurrencyCode(priceLibraryModelDto.getCurrencyCode());
                            priceLibrary.setCeeaAllocationType(priceLibraryModelDto.getCeeaAllocationType());
                            priceLibrary.setCeeaQuotaProportion(new BigDecimal(priceLibraryModelDto.getCeeaQuotaProportion()));
                            priceLibrary.setCeeaLt(priceLibraryModelDto.getCeeaLt());
                            updatePriceLibrarys.add(priceLibrary);
                        }else{
                            addPriceLibrarys.add(buildAddPriceLibrary(priceLibraryModelDto));
                        }
                    }
                } catch (Exception e) {
                    log.info("import PriceCatalog error:{}",e);
                    throw new BaseException("????????????");
                }
            });
            saveBatch(addPriceLibrarys);
            updateBatchById(updatePriceLibrarys);
        } catch (BaseException be) {
            throw be;
        }catch (Exception e){
            log.info("import PriceCatalog error:{}",e);
            throw e;
        }
    }

    /**
     * ??????????????????????????????????????????
     * ???????????????????????? ??????????????????id,??????????????????????????????????????????
     * ???????????????????????? ??????????????????id,??????????????????????????????????????????
     * ???????????????????????????????????????
     * ???????????????????????????????????????????????????????????????????????????
     * ????????? ???????????????????????????
     *
     * @param file
     * @throws IOException
     */
    @Override
    @Transactional
    public Map<String,Object> importInitDataExcel(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (!EasyExcelUtil.isExcel(filename)) {
            throw new BaseException("??????????????????Excel??????");
        }
        log.info("??????????????????");
        InputStream inputStream = file.getInputStream();
        // ??????excel??????
        List<PriceLibraryImportDTO> priceLibraryImportDTOList = EasyExcelUtil.readExcelWithModel(PriceLibraryImportDTO.class,inputStream);
        if(CollectionUtils.isEmpty(priceLibraryImportDTOList)){
            throw new BaseException("??????????????????????????????");
        }
        log.info("??????????????????");

        List<PriceLibrary> priceLIbraryAll = priceLibraryMapper.listAllPriceForImport();

        AtomicBoolean errorFlag = new AtomicBoolean(false);
        List<PriceLibrary> priceLibraryUpdates = new ArrayList<>();
        List<PriceLibrary> priceLibraryAdds = new ArrayList<>();

        checkAndSetPriceLibraryData(priceLibraryImportDTOList,
                errorFlag,priceLibraryUpdates,priceLibraryAdds,priceLIbraryAll);
        log.info("????????????");
        if(errorFlag.get()){
            /*???????????????????????????*/
            log.info("??????????????????");
            String type = filename.substring(filename.lastIndexOf(".") + 1);
            Fileupload fileupload = new Fileupload()
                    .setFileModular("?????????")
                    .setFileFunction("???????????????")
                    .setSourceType("???????????????")
                    .setUploadType(FileUploadType.FASTDFS.name())
                    .setFileType(type);
            fileupload.setFileSourceName(new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "?????????????????????." + type);
            Fileupload wrongFile = EasyExcelUtil.uploadErrorFile(fileCenterClient,fileupload,priceLibraryImportDTOList,PriceLibraryImportDTO.class,file);
            return ImportStatus.importError(wrongFile.getFileuploadId(),wrongFile.getFileSourceName());

        }else{
            log.info("------------------------------------???????????????????????????-------------------------------------------");
            if(CollectionUtils.isNotEmpty(priceLibraryUpdates)){
                this.updateBatchById(priceLibraryUpdates);
            }
            log.info("------------------------------------???????????????????????????-------------------------------------------");
            log.info("------------------------------------???????????????????????????-------------------------------------------");
            if(CollectionUtils.isNotEmpty(priceLibraryAdds)){
                this.saveBatch(priceLibraryAdds);
            }
            log.info("------------------------------------???????????????????????????-------------------------------------------");

        }
        return ImportStatus.importSuccess();

    }

    /**
     * ????????????+????????????+???????????????+????????????+????????????+????????????+????????????+??????????????????+??????????????????
     * @return
     */
    public Map<String, PriceLibrary> getPriceLibraryMap1(List<PriceLibrary> priceLIbraryAll) {
        Map<String, PriceLibrary> priceLibraryMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(priceLIbraryAll)){
            priceLibraryMap = priceLIbraryAll.stream().
                    filter(priceLibrary -> StringUtil.notEmpty(priceLibrary.getPriceType())
                            && "STANDARD".equals(priceLibrary.getPriceType().trim())
                            && null != priceLibrary.getEffectiveDate()
                            && null != priceLibrary.getExpirationDate()).
                    collect(Collectors.toMap(k -> k.getItemCode() + k.getItemDesc() + k.getVendorCode() +
                                    k.getCeeaOrgCode() + k.getCeeaOrganizationCode() + k.getCeeaArrivalPlace() +
                                    DateUtil.format(k.getEffectiveDate(),DateUtil.DATE_FORMAT_14) + DateUtil.format(k.getExpirationDate(),DateUtil.DATE_FORMAT_14),
                            v -> v, (k1, k2) -> k1));
        }
        return priceLibraryMap;
    }

    /**
     * ????????????+????????????+???????????????+????????????+????????????+????????????+????????????
     * @return
     */
    public Map<String, List<PriceLibrary>> getPriceLibraryMap2(List<PriceLibrary> priceLIbraryAll) {
        Map<String, List<PriceLibrary>> priceLibraryMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(priceLIbraryAll)){
            priceLibraryMap = priceLIbraryAll.stream().
                    filter(priceLibrary -> StringUtil.notEmpty(priceLibrary.getPriceType())
                            && "STANDARD".equals(priceLibrary.getPriceType().trim())
                            && null != priceLibrary.getEffectiveDate()
                            && null != priceLibrary.getExpirationDate()).
                    collect(Collectors.groupingBy(k -> k.getItemCode() + k.getItemDesc() + k.getVendorCode() +
                                    k.getCeeaOrgCode() + k.getCeeaOrganizationCode() + k.getCeeaArrivalPlace()));
        }
        return priceLibraryMap;
    }

    /**
     * ????????????????????????
     * @param priceLibraryList
     */
    private void saveBatchPriceLibrary(List<PriceLibrary> priceLibraryList){

    }


    /**
     * ????????????????????????????????????
     * @param priceLibrary
     * @return
     */
    @Override
    public PriceLibrary getLatest(PriceLibrary priceLibrary) {
        System.out.println(JsonUtil.entityToJsonStr(priceLibrary));
        return priceLibraryMapper.getLatest(priceLibrary);
    }

    /**
     *  ??????????????? = 0 ???????????????????????????????????????????????????
     *  ??????????????? <5???>0 ???
     *  ????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *  ??????????????? > 5 ???
     *  ?????????????????????????????????????????????
     *  ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param priceLibraryRequestDTO
     * @return
     */
    @Override
    public List<PriceLibrary> getLatestFive(PriceLibraryRequestDTO priceLibraryRequestDTO) {
        List<PriceLibrary> priceLibraryList = priceLibraryMapper.getLatestFive(priceLibraryRequestDTO);
        List<PriceLibrary> result = new ArrayList<>();
        //???????????? ????????????
        Map<LocalDate,PriceLibrary> map = new HashMap<>();

        for(PriceLibrary p : priceLibraryList){
            LocalDate localDate = DateUtil.dateToLocalDate(p.getCreationDate());
            if(map.get(localDate) == null){
                map.put(localDate,p);
            }else{
                PriceLibrary priceLibrary = map.get(localDate);
                if(p.getTaxPrice().compareTo(priceLibrary.getTaxPrice()) == -1){
                    map.put(localDate,p);
                }
            }
        }

        for(Map.Entry<LocalDate, PriceLibrary> entry:map.entrySet()){
            result.add(entry.getValue());
        }

        result.stream().sorted((p1,p2) -> {
            return p2.getCreationDate().compareTo(p1.getCreationDate());
        });
        if(result.size() <=5){
            return result;
        }else{
            return result.subList(0,5);
        }

    }

    /**
     * ????????????-????????? ?????????????????? + ???????????? + ??????????????? ??????
     * @param priceLibrary
     * @return
     */
    @Override
    public PageInfo<PriceLibrary> listForMaterialSecByBuyer(PriceLibrary priceLibrary) {
        PageUtil.startPage(priceLibrary.getPageNum(),priceLibrary.getPageSize());
        return new PageInfo<PriceLibrary>(priceLibraryMapper.listForMaterialSecByBuyer(priceLibrary));
    }

    /**
     * ????????????-???????????? ??????????????? + ???????????? + ??????????????? ??????
     * @param priceLibrary
     * @return
     */
    @Override
    public PageInfo<PriceLibrary> listForMaterialSecByVendor(PriceLibrary priceLibrary) {
        PageUtil.startPage(priceLibrary.getPageNum(),priceLibrary.getPageSize());
        return new PageInfo<PriceLibrary>(priceLibraryMapper.listForMaterialSecByVendor(priceLibrary));
    }

    private List<PriceLibrary> checkAndSetPriceLibraryData(List<PriceLibraryImportDTO> priceLibraryImportDTOList,AtomicBoolean errorFlag,
                                                           List<PriceLibrary> priceLibraryUpdates,List<PriceLibrary> priceLibraryAdds
            ,List<PriceLibrary> priceLIbraryAll){
        // ???????????????????????????????????????(key->????????????+????????????+???????????????+????????????+????????????+????????????+????????????+??????????????????+??????????????????)
        Map<String, PriceLibrary> priceLibraryMap1 = getPriceLibraryMap1(priceLIbraryAll);
        // ????????????+????????????+???????????????+????????????+????????????+????????????+????????????
        Map<String, List<PriceLibrary>> priceLibraryMap2 = getPriceLibraryMap2(priceLIbraryAll);

        Map<String, List<PriceLibrary>> priceLibraryMap3 = new HashMap<>();

        // ????????????
        Map<String, List<Organization>> orgNameMap = new HashMap<>();
        List<Organization> organizationList = baseClient.listAllOrganizationForImport();
        if(CollectionUtils.isNotEmpty(organizationList)){
            orgNameMap = organizationList.stream().collect(Collectors.groupingBy(Organization::getOrganizationName));
        }

        // ????????????
        Map<String, List<MaterialItem>> materialItemMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(priceLibraryImportDTOList)) {
            List<String> itemCodeList = new ArrayList<>();
            for(PriceLibraryImportDTO priceLibraryImportDTO : priceLibraryImportDTOList) {
                String itemCode = priceLibraryImportDTO.getItemCode();
                if(StringUtil.notEmpty(itemCode)){
                    itemCode = itemCode.trim();
                    itemCodeList.add(itemCode);
                }
            }
            if (CollectionUtils.isNotEmpty(itemCodeList)) {
                itemCodeList = itemCodeList.stream().distinct().collect(Collectors.toList());
                List<MaterialItem> materialItemList = baseClient.listMaterialByCodeBatch(itemCodeList);
                if(CollectionUtils.isNotEmpty(materialItemList)){
                    materialItemMap = materialItemList.stream().collect(Collectors.groupingBy(MaterialItem::getMaterialCode));
                }
            }
        }
        // ???????????????
        Map<String, List<CompanyInfo>> companyInfoMap = new HashMap<>();
        List<CompanyInfo> companyInfoList = supplierClient.listAllForImport();
        if(CollectionUtils.isNotEmpty(companyInfoList)){
            companyInfoMap = companyInfoList.stream().collect(Collectors.groupingBy(CompanyInfo::getCompanyCode));
        }
        // ????????????
        Map<String, List<PurchaseCurrency>> purchaseCurrencyMap = new HashMap<>();
        List<PurchaseCurrency> purchaseCurrencyList = baseClient.listAllPurchaseCurrency();
        if(CollectionUtils.isNotEmpty(purchaseCurrencyList)){
            purchaseCurrencyMap = purchaseCurrencyList.stream().collect(Collectors.groupingBy(PurchaseCurrency::getCurrencyCode));
        }
        // ??????????????? ???????????? + ???????????? + ??????????????? + ???????????? + ???????????? + ????????????(excel?????????????????????) + ????????????(excel???????????????STANDARD) + ?????????????????? + ??????????????????

        List<PriceLibrary> result = new ArrayList<>();
        HashSet<String> hashSet = new HashSet<>();
        log.info("??????1??????");
        int k = 0;
        for(PriceLibraryImportDTO priceLibraryImportDTO:priceLibraryImportDTOList){
            StringBuffer onlyCode = new StringBuffer();
            boolean errorLine = true;
            k++;
            log.info("???"+k+"???????????????");

            StringBuffer errorMsg = new StringBuffer();
            PriceLibrary priceLibrary = new PriceLibrary();
            // ????????????
            String sourceNo = priceLibraryImportDTO.getSourceNo();
            if(StringUtil.notEmpty(sourceNo)){
                sourceNo = sourceNo.trim();
                priceLibrary.setSourceNo(sourceNo);
            }

            // ?????????ID
            String quotationLineId = priceLibraryImportDTO.getQuotationLineId();
            if(StringUtil.notEmpty(quotationLineId)){
                quotationLineId = quotationLineId.trim();
                priceLibrary.setQuotationLineId(quotationLineId);
            }

            // ????????????
            String itemCode = priceLibraryImportDTO.getItemCode();
            if(StringUtil.notEmpty(itemCode)){
                itemCode = itemCode.trim();
                onlyCode.append(itemCode);
                if (CollectionUtils.isNotEmpty(materialItemMap.get(itemCode))){
                    List<MaterialItem> materialItems = materialItemMap.get(itemCode);
                    MaterialItem materialItem = materialItems.get(0);
                    priceLibrary.setItemId(materialItem.getMaterialId());
                    priceLibrary.setItemCode(materialItem.getMaterialCode());
                    priceLibrary.setItemDesc(materialItem.getMaterialName());
                    priceLibrary.setCategoryId(materialItem.getCategoryId());
                    priceLibrary.setCategoryCode(materialItem.getCategoryCode());
                    priceLibrary.setCategoryName(materialItem.getCategoryName());
                    priceLibrary.setUnit(materialItem.getUnitName());
                    priceLibrary.setUnitCode(materialItem.getUnit());
                }else {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("??????????????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorLine = false;
                errorMsg.append("????????????????????????; ");
            }

            // ????????????
            String itemDesc = priceLibraryImportDTO.getItemDesc();
            if(StringUtil.notEmpty(itemDesc)){
                itemDesc = itemDesc.trim();
                onlyCode.append(itemDesc);
                priceLibrary.setItemDesc(itemDesc);
            }else {
                onlyCode.append(priceLibrary.getItemDesc());
            }

            // ???????????????
            String vendorCode = priceLibraryImportDTO.getVendorCode();
            if(StringUtil.notEmpty(vendorCode)){
                vendorCode = vendorCode.trim();
                List<CompanyInfo> companyInfos = companyInfoMap.get(vendorCode);
                if(CollectionUtils.isNotEmpty(companyInfos)){
                    CompanyInfo companyInfo = companyInfos.get(0);
                    priceLibrary.setVendorId(companyInfo.getCompanyId());
                    priceLibrary.setVendorCode(companyInfo.getCompanyCode());
                    priceLibrary.setVendorName(companyInfo.getCompanyName());
                    onlyCode.append(companyInfo.getCompanyCode());
                }else {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("?????????????????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorLine = false;
                errorMsg.append("???????????????????????????; ");
            }

            // ????????????
            String ceeaOrgName = priceLibraryImportDTO.getCeeaOrgName();
            if(StringUtil.notEmpty(ceeaOrgName)){
                ceeaOrgName = ceeaOrgName.trim();
                List<Organization> organizations = orgNameMap.get(ceeaOrgName);
                if(CollectionUtils.isNotEmpty(organizations)){
                    Organization organization = organizations.get(0);
                    priceLibrary.setCeeaOrgId(organization.getOrganizationId());
                    priceLibrary.setCeeaOrgCode(organization.getOrganizationCode());
                    priceLibrary.setCeeaOrgName(organization.getOrganizationName());
                    onlyCode.append(organization.getOrganizationCode());
                }else {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("??????????????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorLine = false;
                errorMsg.append("????????????????????????????????????; ");
            }

            // ????????????
            String ceeaOrganizationCode = priceLibraryImportDTO.getCeeaOrganizationCode();
            if(StringUtil.notEmpty(ceeaOrganizationCode)){
                ceeaOrganizationCode = ceeaOrganizationCode.trim();
                List<Organization> organizations = orgNameMap.get(ceeaOrganizationCode);
                if(CollectionUtils.isNotEmpty(organizations)){
                    Organization organization = organizations.get(0);
                    priceLibrary.setCeeaOrganizationId(organization.getOrganizationId());
                    priceLibrary.setCeeaOrganizationCode(organization.getOrganizationCode());
                    priceLibrary.setCeeaOrganizationName(organization.getOrganizationName());
                    onlyCode.append(organization.getOrganizationCode());
                }else {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("??????????????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorLine = false;
                errorMsg.append("????????????????????????; ");
            }

            // ????????????
            String ceeaArrivalPlace = priceLibraryImportDTO.getCeeaArrivalPlace();
            if(StringUtil.notEmpty(ceeaArrivalPlace)){
                ceeaArrivalPlace = ceeaArrivalPlace.trim();
                priceLibrary.setCeeaArrivalPlace(ceeaArrivalPlace);
                onlyCode.append(ceeaArrivalPlace);
            }else {
                onlyCode.append(ceeaArrivalPlace);
            }

            // ????????????, ????????? STANDARD
            priceLibrary.setPriceType("STANDARD");

            // ?????????
            String taxPrice = priceLibraryImportDTO.getTaxPrice();
            if(StringUtil.notEmpty(taxPrice)){
                taxPrice = taxPrice.trim();
                if(StringUtil.isDigit(taxPrice)){
                    priceLibrary.setTaxPrice(new BigDecimal(taxPrice));
                }else {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("?????????????????????; ");
                }
            }

            // ??????
            String taxKey = priceLibraryImportDTO.getTaxKey();
            if(StringUtil.notEmpty(taxKey)){
                taxKey = taxKey.trim();
                priceLibrary.setTaxKey(taxKey);
            }

            // ??????
            String taxRate = priceLibraryImportDTO.getTaxRate();
            if(StringUtil.notEmpty(taxRate)){
                if (StringUtil.isDigit(taxRate)) {
                    taxRate = taxRate.trim();
                    priceLibrary.setTaxRate(taxRate);
                }else {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("??????????????????; ");
                }
            }

            // ??????????????????
            if(StringUtil.notEmpty(priceLibrary.getTaxRate()) && StringUtil.notEmpty(priceLibrary.getTaxPrice())){
                BigDecimal ceeaTaxRate = new BigDecimal(priceLibrary.getTaxRate()).divide(new BigDecimal(100),4,BigDecimal.ROUND_HALF_UP).add(new BigDecimal(1));
                priceLibrary.setNotaxPrice(priceLibrary.getTaxPrice().divide(ceeaTaxRate,2,BigDecimal.ROUND_HALF_UP));
            }

            // ??????????????????
            priceLibrary.setCeeaIfUse("N");
            priceLibrary.setMinOrderQuantity(new BigDecimal(0));

            // ??????
            String currencyCode = priceLibraryImportDTO.getCurrencyCode();
            if(StringUtil.notEmpty(currencyCode)){
                currencyCode = currencyCode.trim();
                List<PurchaseCurrency> purchaseCurrencies = purchaseCurrencyMap.get(currencyCode);
                if(CollectionUtils.isNotEmpty(purchaseCurrencies)){
                    PurchaseCurrency purchaseCurrency = purchaseCurrencies.get(0);
                    priceLibrary.setCurrencyId(purchaseCurrency.getCurrencyId());
                    priceLibrary.setCurrencyCode(purchaseCurrency.getCurrencyCode());
                    priceLibrary.setCurrencyName(purchaseCurrency.getCurrencyName());
                }else {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("????????????????????????; ");
                }
            }

            // ??????????????????
            String effectiveDateStr = priceLibraryImportDTO.getEffectiveDateStr();
            if(StringUtil.notEmpty(effectiveDateStr)){
                effectiveDateStr = effectiveDateStr.trim();
                try {
                    Date date = DateUtil.parseDate(effectiveDateStr);
                    priceLibrary.setEffectiveDate(date);
                    onlyCode.append(DateUtil.parseDateToStr(date,DateUtil.DATE_FORMAT_14));
                } catch (Exception e) {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("??????????????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorLine = false;
                errorMsg.append("??????????????????????????????; ");
            }

            // ????????????
            String expirationDateStr = priceLibraryImportDTO.getExpirationDateStr();
            if(StringUtil.notEmpty(expirationDateStr)){
                expirationDateStr = expirationDateStr.trim();
                try {
                    Date date = DateUtil.parseDate(expirationDateStr);
                    priceLibrary.setExpirationDate(date);
                    onlyCode.append(DateUtil.parseDateToStr(date,DateUtil.DATE_FORMAT_14));
                } catch (Exception e) {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("??????????????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorLine = false;
                errorMsg.append("??????????????????????????????; ");
            }

            // L/T
            String ceeaLt = priceLibraryImportDTO.getCeeaLt();
            if(StringUtil.notEmpty(ceeaLt)){
                ceeaLt = ceeaLt.trim();
                priceLibrary.setCeeaLt(ceeaLt);
            }

            // ??????????????????
            String contractCode = priceLibraryImportDTO.getContractCode();
            if(StringUtil.notEmpty(contractCode)){
                contractCode = contractCode.trim();
                priceLibrary.setContractCode(contractCode);
            }

            // ???????????????
            String ceeaIfUse = priceLibraryImportDTO.getCeeaIfUse();
            if(StringUtil.notEmpty(ceeaIfUse)){
                ceeaIfUse = ceeaIfUse.trim();
                if(YesOrNo.YES.getValue().equals(ceeaIfUse) || YesOrNo.NO.getValue().equals(ceeaIfUse)){
                    priceLibrary.setCeeaIfUse(ceeaIfUse);
                }else {
                    errorFlag.set(true);
                    errorLine = false;
                    errorMsg.append("????????????????????????\"Y\"???\"N\"; ");
                }
            }


            if(errorLine){
                // ????????????????????????
                if(hashSet.add(onlyCode.toString())){
                    // ????????????????????????
                    PriceLibrary library = priceLibraryMap1.get(onlyCode.toString());
                    /**
                     * ????????????+????????????+???????????????+????????????+????????????+????????????+????????????+??????????????????+?????????????????????????????????????????????????????????????????????????????????????????????
                     */
                    if(null != library){
                        // ??????
                        library.setSourceNo(priceLibrary.getSourceNo());
                        library.setQuotationLineId(priceLibrary.getQuotationLineId());
                        library.setItemId(priceLibrary.getItemId());
                        library.setItemCode(priceLibrary.getItemCode());
                        library.setItemDesc(priceLibrary.getItemDesc());
                        library.setCategoryId(priceLibrary.getCategoryId());
                        library.setCategoryCode(priceLibrary.getCategoryCode());
                        library.setCategoryName(priceLibrary.getCategoryName());
                        library.setUnit(priceLibrary.getUnit());
                        library.setUnitCode(priceLibrary.getUnitCode());
                        library.setVendorId(priceLibrary.getVendorId());
                        library.setVendorCode(priceLibrary.getVendorCode());
                        library.setVendorName(priceLibrary.getVendorName());
                        library.setCeeaOrgId(priceLibrary.getCeeaOrgId());
                        library.setCeeaOrgCode(priceLibrary.getCeeaOrgCode());
                        library.setCeeaOrgName(priceLibrary.getCeeaOrgName());
                        library.setCeeaOrganizationId(priceLibrary.getCeeaOrganizationId());
                        library.setCeeaOrganizationCode(priceLibrary.getCeeaOrganizationCode());
                        library.setCeeaOrganizationName(priceLibrary.getCeeaOrganizationName());
                        library.setTaxPrice(priceLibrary.getTaxPrice());
                        library.setTaxKey(priceLibrary.getTaxKey());
                        library.setTaxRate(priceLibrary.getTaxRate());
                        library.setNotaxPrice(priceLibrary.getNotaxPrice());
                        library.setCurrencyId(priceLibrary.getCurrencyId());
                        library.setCurrencyCode(priceLibrary.getCurrencyCode());
                        library.setCurrencyName(priceLibrary.getCurrencyName());
                        library.setCeeaLt(priceLibrary.getCeeaLt());
                        library.setContractCode(priceLibrary.getContractCode());
                        library.setCeeaIfUse(priceLibrary.getCeeaIfUse());
                        library.setCeeaArrivalPlace(priceLibrary.getCeeaArrivalPlace());
                        priceLibraryUpdates.add(library);
                    }else {
                        /**
                         * ???????????????????????????????????????+????????????+???????????????+????????????+????????????+????????????+???????????????????????????????????????
                         * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         */

                        AtomicBoolean addFlag = new AtomicBoolean(true);
                        String str = priceLibrary.getItemCode() + priceLibrary.getItemDesc() + priceLibrary.getVendorCode() + priceLibrary.getCeeaOrgCode() + priceLibrary.getCeeaOrganizationCode() + priceLibrary.getCeeaArrivalPlace();
                        List<PriceLibrary> priceLibraries = priceLibraryMap2.get(str);
                        if(CollectionUtils.isNotEmpty(priceLibraries)){
                            for(PriceLibrary priceLibrary1: priceLibraries){
                                boolean flag = ifIntersect1(priceLibrary, priceLibrary1);
                                if(flag){
                                    errorFlag.set(true);
                                    addFlag.set(false);
                                    errorMsg.append("????????????????????????????????????????????????; ");
                                    break;
                                }
                            }
                        }

                        // ???????????????????????????????????????
                        List<PriceLibrary> priceLibraryList = priceLibraryMap3.get(str);
                        if(CollectionUtils.isNotEmpty(priceLibraryList)){
                            for(PriceLibrary priceLibrary1: priceLibraryList){
                                boolean flag = ifIntersect1(priceLibrary, priceLibrary1);
                                if(flag){
                                    errorFlag.set(true);
                                    addFlag.set(false);
                                    errorMsg.append("???????????????????????????????????????????????????; ");
                                    break;
                                }
                            }
                            priceLibraryList.add(priceLibrary);
                            priceLibraryMap3.put(str,priceLibraryList);
                        }else {
                            ArrayList<PriceLibrary> arrayList = new ArrayList<>();
                            arrayList.add(priceLibrary);
                            priceLibraryMap3.put(str,arrayList);
                        }

                        if(addFlag.get()){
                            priceLibrary.setPriceLibraryId(IdGenrator.generate());
                            priceLibrary.setCeeaPriceLibraryNo(baseClient.seqGen(SequenceCodeConstant.SEQ_INQ_PRICE_LIBRARY_NO));
                            priceLibraryAdds.add(priceLibrary);
                        }
                    }
                }else {
                    errorFlag.set(true);
                    errorMsg.append("????????????+????????????+???????????????+????????????+????????????+????????????+??????????????????+??????????????????:???????????????; ");
                }
            }

            if(errorMsg.length() > 0){
                priceLibraryImportDTO.setError(errorMsg.toString());
            }else {
                priceLibraryImportDTO.setError(null);
            }

        }
        return result;
    }

//    /**
//     * ???????????????????????????????????????????????????
//     * @param priceLibraryImportDTOList
//     * @return
//     */
//    private boolean checkIntersect(List<PriceLibraryImportDTO> priceLibraryImportDTOList){
//        boolean result = false;
//        for(int i=0;i<priceLibraryImportDTOList.size();i++){
//            for(int j=i+1;j<priceLibraryImportDTOList.size();j++){
//                if(ifIntersect(priceLibraryImportDTOList.get(i),priceLibraryImportDTOList.get(j))){
//                    String error1 = priceLibraryImportDTOList.get(i).getError();
//                    StringBuilder stringBuilder = new StringBuilder(error1 == null ? "" : error1);
//                    stringBuilder.append("[??????{"+priceLibraryImportDTOList.get(i).getLocation()+"}?????????{"+priceLibraryImportDTOList.get(j).getLocation()+"}?????????????????????]");
//                    priceLibraryImportDTOList.get(i).setError(stringBuilder.toString());
//
//                    String error2 = priceLibraryImportDTOList.get(j).getError();
//                    StringBuilder stringBuilder2 = new StringBuilder(error2 == null ? "" : error2);
//                    stringBuilder.append("[??????{"+priceLibraryImportDTOList.get(j).getLocation()+"}?????????{"+priceLibraryImportDTOList.get(i).getLocation()+"}?????????????????????]");
//                    priceLibraryImportDTOList.get(j).setError(stringBuilder2.toString());
//                    result = true;
//                    break;
//                }
//            }
//        }
//        return result;
//    }

//    private boolean ifCommonGroup(PriceLibraryImportDTO priceLibraryImportDTO1,PriceLibraryImportDTO priceLibraryImportDTO2){
//        /*????????????*/
//        if(!Objects.equals(priceLibraryImportDTO1.getCeeaOrgName(),priceLibraryImportDTO2.getCeeaOrgName())){
//            return false;
//        }
//        /*????????????*/
//        if(!Objects.equals(priceLibraryImportDTO1.getCeeaOrganizationCode(),priceLibraryImportDTO2.getCeeaOrganizationCode())){
//            return false;
//        }
//        /*????????????*/
//        if(!Objects.equals(priceLibraryImportDTO1.getItemCode(),priceLibraryImportDTO2.getItemCode())){
//            return false;
//        }
//
//        /*????????????*/
//        if(!Objects.equals(priceLibraryImportDTO1.getItemDesc(),priceLibraryImportDTO2.getItemDesc())){
//            return false;
//        }
//
//        /*???????????????*/
//        if(!Objects.equals(priceLibraryImportDTO1.getVendorCode(),priceLibraryImportDTO2.getVendorCode())){
//            return false;
//        }
//        /*????????????(????????????????????????)*/
//        /*????????????(????????????????????????)*/
//        return true;
//    }

    public static void main(String[] args) {
        /*List<PriceLibrary> priceLibraryList = new ArrayList<>();
        PriceLibrary priceLibrary1 = new PriceLibrary()
                .setPriceLibraryId(1L)
                .setPriceNumber("P123");
        PriceLibrary priceLibrary2 = new PriceLibrary()
                .setPriceLibraryId(2L)
                .setPriceNumber("P234");
        priceLibraryList.add(priceLibrary1);
        priceLibraryList.add(priceLibrary2);
        Boolean a = new Boolean(true);
        testList(priceLibraryList,a);
        for(PriceLibrary priceLibrary:priceLibraryList){
            System.out.println(priceLibrary.getPriceLibraryId());
            System.out.println(priceLibrary.getPriceNumber());
            System.out.println("------------");
        }
        System.out.println(a);*/

        /*????????????????????????*/
//        List<PriceLibrary> priceLibraryList = new ArrayList<>();
//        PriceLibrary priceLibrary1 = new PriceLibrary()
//                .setPriceLibraryId(1L)
//                .setPriceNumber("P123");
//        PriceLibrary priceLibrary2 = new PriceLibrary()
//                .setPriceLibraryId(2L)
//                .setPriceNumber("P234");
//        PriceLibrary priceLibrary3 = new PriceLibrary()
//                .setPriceLibraryId(3L)
//                .setPriceNumber("P234");
//        PriceLibrary priceLibrary4 = new PriceLibrary()
//                .setPriceLibraryId(4L)
//                .setPriceNumber("P234");
//        PriceLibrary priceLibrary5 = new PriceLibrary()
//                .setPriceLibraryId(5L)
//                .setPriceNumber("P234");
//        PriceLibrary priceLibrary6 = new PriceLibrary()
//                .setPriceLibraryId(6L)
//                .setPriceNumber("P234");
//        PriceLibrary priceLibrary7 = new PriceLibrary()
//                .setPriceLibraryId(7L)
//                .setPriceNumber("P234");
//        PriceLibrary priceLibrary8 = new PriceLibrary()
//                .setPriceLibraryId(8L)
//                .setPriceNumber("P234");
//        PriceLibrary priceLibrary9 = new PriceLibrary()
//                .setPriceLibraryId(9L)
//                .setPriceNumber("P234");
//        PriceLibrary priceLibrary10 = new PriceLibrary()
//                .setPriceLibraryId(10L)
//                .setPriceNumber("P234");
//
//        priceLibraryList.add(priceLibrary1);
//        priceLibraryList.add(priceLibrary2);
//        priceLibraryList.add(priceLibrary3);
//        priceLibraryList.add(priceLibrary4);
//        priceLibraryList.add(priceLibrary5);
//        priceLibraryList.add(priceLibrary6);
//        priceLibraryList.add(priceLibrary7);
//        priceLibraryList.add(priceLibrary8);
//        priceLibraryList.add(priceLibrary9);
//        priceLibraryList.add(priceLibrary10);
//
//        List<PriceLibrary> subList = new ArrayList<>();
//        int size = 8;
//        if(priceLibraryList.size() <= size){
//        }else{
//            int times = (int) Math.ceil(priceLibraryList.size() / new Double(size));
//            log.info("???????????????times:" + times);
//            for(int i=0;i<times;i++){
//
//                System.out.println("???????????????" + (i * size));
//                System.out.println("???????????????" +  Math.min((i + 1) * size, priceLibraryList.size()));
//                subList = priceLibraryList.subList(i * size, Math.min((i + 1) * size, priceLibraryList.size()));
//                for(PriceLibrary priceLibrary:subList){
//                    System.out.println(priceLibrary.getPriceLibraryId());
//                }
//                System.out.println("----------------");
//
//            }
//        }

        LocalDate localDate = LocalDate.now();
        System.out.println(localDate.toString());




    }
    public static boolean testList(List<PriceLibrary> priceLibraryList,Boolean flag){
        for(PriceLibrary priceLibrary:priceLibraryList){
            priceLibrary.setPriceNumber("P345");
        }
        flag = new Boolean(false);
        return false;
    }

//    private boolean ifIntersect(PriceLibraryImportDTO priceLibraryImportDTO1,PriceLibraryImportDTO priceLibraryImportDTO2){
//        LocalDate effectiveDate1 = DateUtil.dateToLocalDate(priceLibraryImportDTO1.getEffectiveDate());
//        LocalDate expirationDate1 = DateUtil.dateToLocalDate(priceLibraryImportDTO1.getExpirationDate());
//
//        LocalDate effectiveDate2 = DateUtil.dateToLocalDate(priceLibraryImportDTO2.getEffectiveDate());
//        LocalDate expirationDate2 = DateUtil.dateToLocalDate(priceLibraryImportDTO2.getExpirationDate());
//        /*?????????*/
//        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
//                isBeforeAndEquals(effectiveDate2,expirationDate1) &&
//                isBeforeAndEquals(expirationDate1,expirationDate2)
//        ){
//            log.info("priceLibrary:" + priceLibraryImportDTO1);
//            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO2);
//            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
//            return true;
//        }
//        /*?????????*/
//        if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
//                isBeforeAndEquals(effectiveDate1,expirationDate2) &&
//                isBeforeAndEquals(expirationDate2,expirationDate1)
//        ){
//            log.info("priceLibrary:" + priceLibraryImportDTO1);
//            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO2);
//            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
//            return true;
//        }
//        /*??????*/
//        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
//                isBeforeAndEquals(expirationDate2,expirationDate1)
//        ){
//            log.info("priceLibrary:" + priceLibraryImportDTO1);
//            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO2);
//            log.info("???????????????-??????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
//            return true;
//        }
//        /*?????????*/
//        if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
//                isBeforeAndEquals(expirationDate1,expirationDate2)
//        ){
//            log.info("priceLibrary:" + priceLibraryImportDTO1);
//            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO2);
//            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
//            return true;
//        }
//        return false;
//    }

    private boolean ifIntersect1(PriceLibrary priceLibraryImportDTO1,PriceLibrary priceLibraryImportDTO2){
        LocalDate effectiveDate1 = DateUtil.dateToLocalDate(priceLibraryImportDTO1.getEffectiveDate());
        LocalDate expirationDate1 = DateUtil.dateToLocalDate(priceLibraryImportDTO1.getExpirationDate());

        LocalDate effectiveDate2 = DateUtil.dateToLocalDate(priceLibraryImportDTO2.getEffectiveDate());
        LocalDate expirationDate2 = DateUtil.dateToLocalDate(priceLibraryImportDTO2.getExpirationDate());
        /*?????????*/
        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
                isBeforeAndEquals(effectiveDate2,expirationDate1) &&
                isBeforeAndEquals(expirationDate1,expirationDate2)
        ){
            log.info("priceLibrary:" + priceLibraryImportDTO1);
            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO2);
            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
            return true;
        }
        /*?????????*/
        if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
                isBeforeAndEquals(effectiveDate1,expirationDate2) &&
                isBeforeAndEquals(expirationDate2,expirationDate1)
        ){
            log.info("priceLibrary:" + priceLibraryImportDTO1);
            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO2);
            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
            return true;
        }
        /*??????*/
        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
                isBeforeAndEquals(expirationDate2,expirationDate1)
        ){
            log.info("priceLibrary:" + priceLibraryImportDTO1);
            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO2);
            log.info("???????????????-??????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
            return true;
        }
        /*?????????*/
        if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
                isBeforeAndEquals(expirationDate1,expirationDate2)
        ){
            log.info("priceLibrary:" + priceLibraryImportDTO1);
            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO2);
            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
            return true;
        }
        return false;
    }


//    private boolean ifIntersect(PriceLibrary priceLibrary,PriceLibraryImportDTO priceLibraryImportDTO){
//        LocalDate effectiveDate1 = DateUtil.dateToLocalDate(priceLibrary.getEffectiveDate());
//        LocalDate expirationDate1 = DateUtil.dateToLocalDate(priceLibrary.getExpirationDate());
//
//        LocalDate effectiveDate2 = DateUtil.dateToLocalDate(priceLibraryImportDTO.getEffectiveDate());
//        LocalDate expirationDate2 = DateUtil.dateToLocalDate(priceLibraryImportDTO.getExpirationDate());
//        /*?????????*/
//        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
//                isBeforeAndEquals(effectiveDate2,expirationDate1) &&
//                isBeforeAndEquals(expirationDate1,expirationDate2)
//        ){
//            log.info("priceLibrary:" + priceLibrary);
//            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO);
//            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
//            return true;
//        }
//        /*?????????*/
//        if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
//                isBeforeAndEquals(effectiveDate1,expirationDate2) &&
//                isBeforeAndEquals(expirationDate2,expirationDate1)
//        ){
//            log.info("priceLibrary:" + priceLibrary);
//            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO);
//            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
//            return true;
//        }
//        /*??????*/
//        if(isBeforeAndEquals(effectiveDate1,effectiveDate2) &&
//                isBeforeAndEquals(expirationDate2,expirationDate1)
//        ){
//            log.info("priceLibrary:" + priceLibrary);
//            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO);
//            log.info("???????????????-??????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
//            return true;
//        }
//        /*?????????*/
//        if(isBeforeAndEquals(effectiveDate2,effectiveDate1) &&
//                isBeforeAndEquals(expirationDate1,expirationDate2)
//        ){
//            log.info("priceLibrary:" + priceLibrary);
//            log.info("priceLibraryImportDTO:" + priceLibraryImportDTO);
//            log.info("???????????????-?????????,[" + effectiveDate1 + "," +expirationDate1 + "] [" + effectiveDate2 + "," + expirationDate2 + "]");
//            return true;
//        }
//        return false;
//    }


    @Override
    public List<PriceLibrary> listAllEffective(PriceLibrary priceLibrary) {
        return priceLibraryMapper.listAllEffective(priceLibrary);
    }


    public PriceLibrary buildAddPriceLibrary(PriceLibraryModelDto priceLibraryModelDto) throws Exception{
        Long id = IdGenrator.generate();
        PriceLibrary addPriceLibrary = new PriceLibrary();
        addPriceLibrary.setPriceLibraryId(id);
        //????????????id???????????????
        MaterialItem param = new MaterialItem();
        param.setMaterialCode(priceLibraryModelDto.getItemCode());
        List<MaterialItem> materialItemList = baseClient.listMaterialByParam(param);
        if(CollectionUtils.isEmpty(materialItemList)){
            throw new BaseException("???????????????????????????");
        }else{
            MaterialItem materialItem = materialItemList.get(0);
            addPriceLibrary.setCategoryId(materialItem.getCategoryId());
            addPriceLibrary.setCategoryName(materialItem.getCategoryName());
            addPriceLibrary.setItemId(materialItem.getMaterialId());
        }
        addPriceLibrary.setItemCode(priceLibraryModelDto.getItemCode());
        addPriceLibrary.setItemDesc(priceLibraryModelDto.getItemDesc());
        addPriceLibrary.setVendorCode(priceLibraryModelDto.getVendorCode());
        addPriceLibrary.setVendorName(priceLibraryModelDto.getVendorName());
        //????????????????????????
        Organization orgInfo = baseClient.getOrganizationByParam(new Organization().setOrganizationCode(priceLibraryModelDto.getCeeaOrgCode()).setOrganizationTypeCode(BaseConst.ORG_OU));
        addPriceLibrary.setCeeaOrgId(orgInfo.getOrganizationId());
        addPriceLibrary.setCeeaOrgName(orgInfo.getOrganizationName());
        addPriceLibrary.setCeeaOrgCode(priceLibraryModelDto.getCeeaOrgCode());
        //????????????????????????
        Organization organizationInfo = baseClient.getOrganizationByParam(new Organization().setOrganizationCode(priceLibraryModelDto.getCeeaOrganizationCode()).setOrganizationTypeCode(BaseConst.ORG_INV));
        addPriceLibrary.setCeeaOrganizationId(organizationInfo.getOrganizationId());
        addPriceLibrary.setCeeaOrganizationName(organizationInfo.getOrganizationName());
        addPriceLibrary.setCeeaOrganizationCode(priceLibraryModelDto.getCeeaOrganizationCode());
        addPriceLibrary.setCeeaArrivalPlace(priceLibraryModelDto.getCeeaArrivalPlace());
        addPriceLibrary.setTaxPrice(new BigDecimal(priceLibraryModelDto.getTaxPrice()));
        addPriceLibrary.setTaxKey(priceLibraryModelDto.getTaxKey());
        addPriceLibrary.setTaxRate(priceLibraryModelDto.getTaxRate());
        addPriceLibrary.setCurrencyCode(priceLibraryModelDto.getCurrencyCode());
        addPriceLibrary.setCeeaAllocationType(priceLibraryModelDto.getCeeaAllocationType());
        addPriceLibrary.setCeeaQuotaProportion(new BigDecimal(priceLibraryModelDto.getCeeaQuotaProportion()));
        addPriceLibrary.setExpirationDate(DateUtil.parseDate(priceLibraryModelDto.getExpirationDate()));
        addPriceLibrary.setEffectiveDate(DateUtil.parseDate(priceLibraryModelDto.getEffectiveDate()));
        addPriceLibrary.setCeeaLt(priceLibraryModelDto.getCeeaLt());
        return addPriceLibrary;
    }

    private PriceLibrary checkImportRowIfExist(PriceLibraryModelDto priceLibraryModelDto) throws Exception{
        QueryWrapper<PriceLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PriceLibrary::getItemCode,priceLibraryModelDto.getItemCode())
                .eq(PriceLibrary::getItemDesc,priceLibraryModelDto.getItemDesc())
                .eq(PriceLibrary::getVendorCode,priceLibraryModelDto.getVendorCode())
                .eq(PriceLibrary::getCeeaOrgCode,priceLibraryModelDto.getCeeaOrgCode())
                .eq(PriceLibrary::getCeeaOrganizationCode,priceLibraryModelDto.getCeeaOrganizationCode())
                .eq(StringUtils.isNotBlank(priceLibraryModelDto.getCeeaArrivalPlace()),PriceLibrary::getCeeaArrivalPlace,priceLibraryModelDto.getCeeaArrivalPlace())
                .eq(PriceLibrary::getEffectiveDate,DateUtil.parseDate(priceLibraryModelDto.getEffectiveDate()))
                .eq(PriceLibrary::getExpirationDate,DateUtil.parseDate(priceLibraryModelDto.getExpirationDate()));
       return this.getBaseMapper().selectOne(queryWrapper);
    }

    private void checkRequireParam(PriceLibraryModelDto priceLibraryModelDto){
        if(StringUtils.isBlank(priceLibraryModelDto.getItemCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getItemDesc())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getVendorCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????ERP???????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getVendorName())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getCeeaOrgCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getCeeaOrganizationCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getTaxPrice())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getTaxRate())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getTaxKey())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getCurrencyCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getEffectiveDate())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getExpirationDate())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getCeeaLt())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("L/T?????????????????????????????????????????????????????????!"));
        }
        //TODO ?????????????????????
     /*   if(StringUtils.isBlank(priceLibraryModelDto.getPaymentTerms())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getPaymentType())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getPaymentDays())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????!"));
        }
        if(StringUtils.isBlank(priceLibraryModelDto.getFrameworkAgreement())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????!"));
        }*/
    }


    /**
     * ??????????????????????????????
     * @param priceLibrary
     * @param contractCode
     */
    public void updateMaterialItemInfo(PriceLibrary priceLibrary,String contractCode){
        //????????????????????????????????????
        MaterialItem materialItem = baseClient.findMaterialItemByMaterialCode(priceLibrary.getItemCode());
        materialItem.setCeeaSupplierCode(priceLibrary.getVendorCode());
        materialItem.setCeeaSupplierName(priceLibrary.getVendorName());
        materialItem.setCeeaIfCatalogMaterial("Y");
        materialItem.setCeeaContractNo(contractCode);
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        materialItem.setCeeaNickname(loginAppUser.getNickname());
        materialItem.setCeeaUserId(loginAppUser.getUserId());
        materialItem.setCeeaEmpNo(loginAppUser.getUsername());
        materialItem.setCeeaOnShelfTime(new Date());
        materialItem.setCeeaMaterialStatus(CeeaMaterialStatus.NOT_NOTIFIED.getCode());
        baseClient.updateMaterialItemById(materialItem);
    }

    public Map<String, Object> getBetweenDate(int year) throws ParseException {
        String dateModel1 = "$date-01-01 00:00:00";
        String dateModel2 = "$date-12-31 23:59:59";
        Map<String, Object> betweenDate = new HashMap<>();
        betweenDate.put("startDate", DateUtil.parseDate(dateModel1.replace("$date",String.valueOf(year))));
        betweenDate.put("endDate",DateUtil.parseDate(dateModel2.replace("$date",String.valueOf(year))));
        return betweenDate;
    }

    @Override
    public PageInfo<PriceLibraryVO> PriceLibraryListPage(PriceLibrary priceLibrary) {
        PageUtil.startPage(priceLibrary.getPageNum(), priceLibrary.getPageSize());
        QueryWrapper<PriceLibrary> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getVendorName()), "VENDOR_NAME", priceLibrary.getVendorName());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getItemCode()), "ITEM_CODE", priceLibrary.getItemCode());
        wrapper.eq(priceLibrary.getCeeaOrgId() != null,"CEEA_ORG_ID",priceLibrary.getCeeaOrgId());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getCeeaOrgName()), "CEEA_ORG_NAME", priceLibrary.getCeeaOrgName());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getApprovalNo()), "APPROVAL_NO", priceLibrary.getApprovalNo());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getCeeaOrganizationName()), "CEEA_ORGANIZATION_NAME", priceLibrary.getOrganizationName());
        wrapper.eq(null != priceLibrary.getCeeaOrganizationId(), "CEEA_ORGANIZATION_ID", priceLibrary.getCeeaOrganizationId());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getCategoryName()), "CATEGORY_NAME", priceLibrary.getCategoryName());
        wrapper.eq(StringUtils.isNotBlank(priceLibrary.getCeeaIfUse()),"CEEA_IF_USE",priceLibrary.getCeeaIfUse());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getContractCode()), "CONTRACT_CODE",priceLibrary.getContractCode());
        wrapper.orderByDesc("CREATION_DATE");
        //???????????????????????????
        PageInfo<PriceLibraryVO> priceLibraryVOPageInfo = priceLibraryListFilterAuth(wrapper);
        if (CollectionUtils.isNotEmpty(priceLibraryVOPageInfo.getList())){
            //????????????????????????
//            for(PriceLibraryVO item:priceLibraryVOPageInfo.getList()){
//                /*?????????????????????????????????*/
//                QueryWrapper<PriceLibraryPaymentTerm> priceLibraryPaymentTermQueryWrapper = new QueryWrapper<>();
//                priceLibraryPaymentTermQueryWrapper.eq("PRICE_LIBRARY_ID",item.getPriceLibraryId());
//                List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = priceLibraryPaymentTermService.list(priceLibraryPaymentTermQueryWrapper);
//                item.setPriceLibraryPaymentTermList(priceLibraryPaymentTermList);
//            }
            // ??????????????????
            List<Long> priceLibraryIdList = priceLibraryVOPageInfo.getList().stream().map(PriceLibraryVO::getPriceLibraryId).collect(Collectors.toList());
            List<PriceLibraryPaymentTerm> priceLibraryPaymentTermList = priceLibraryPaymentTermService.listByPriceLibraryIdCollection(priceLibraryIdList);
            Map<Long, List<PriceLibraryPaymentTerm>> priceLibraryPaymentTermMap = priceLibraryPaymentTermList.stream().collect(Collectors.groupingBy(PriceLibraryPaymentTerm::getPriceLibraryId));

            for(PriceLibraryVO item: priceLibraryVOPageInfo.getList()){
                item.setPriceLibraryPaymentTermList(priceLibraryPaymentTermMap.get(item.getPriceLibraryId()));
            }
        }
        return priceLibraryVOPageInfo;
    }

    /**
     * ?????????????????????????????????
     * @param wrapper
     * @return
     */
    @AuthData(module = MenuEnum.PRICE_CATALOG)
    private PageInfo<PriceLibraryVO>  priceLibraryListFilterAuth(QueryWrapper<PriceLibrary> wrapper ){
        return new PageInfo<>(priceLibraryMapper.listPageCopy(wrapper));
    }

}
