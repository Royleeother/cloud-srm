package com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.ImportStatus;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.bid.projectmanagement.evaluation.BidingAwardWayEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.exception.BidException;
import com.midea.cloud.common.handler.SpinnerSheetWriteHandler;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.check.PreCheck;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.mapper.BidRequirementLineMapper;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.bidinitiating.service.*;
import com.midea.cloud.srm.bid.purchaser.projectmanagement.quoteauthorize.service.IQuoteAuthorizeService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.base.PricingFormulaCalculateClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.dict.entity.DictItem;
import com.midea.cloud.srm.model.base.formula.dto.calculate.BaseMaterialPriceDTO;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.material.MaterialOrg;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.ou.entity.BaseOuGroup;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuGroupDetailVO;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseTax;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseUnit;
import com.midea.cloud.srm.model.base.region.dto.AreaDTO;
import com.midea.cloud.srm.model.base.region.dto.AreaPramDTO;
import com.midea.cloud.srm.model.base.region.dto.CityParamDto;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.dto.BidRequirementLineDto;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirement;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.entity.OuRelatePrice;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.vo.BidRequirementLineImportVO;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.quoteauthorize.entity.QuoteAuthorize;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ????????????????????? ???????????????
 * </pre>
 *
 * @author fengdc3@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:  tanjl11@meicloud.com
 *  ????????????: 2020-09-03 17:04:28
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class BidRequirementLineServiceImpl extends ServiceImpl<BidRequirementLineMapper, BidRequirementLine> implements IBidRequirementLineService {

    @Autowired
    private BaseClient baseClient;
    @Autowired
    private SupplierClient supplierClient;
    @Autowired
    private IBidingService bidingService;
    @Autowired
    private IBidVendorService iBidVendorService;
    @Autowired
    private IQuoteAuthorizeService iQuoteAuthorizeService;
    @Autowired
    private IOuRelatePriceService relatePriceService;
    @Autowired
    private PricingFormulaCalculateClient pricingFormulaCalculateClient;
    @Autowired
    private FileCenterClient fileCenterClient;
    @Resource
    private IBidRequirementService iBidRequirementService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreCheck(checkMethod = "checkParam")
    public void saveBidRequirementLineList(List<BidRequirementLine> bidRequirementLineList, BidRequirement bidRequirement) {
        Map<Long, String> materialMap = new HashMap<>();
        HashSet<Long> ouIds = new HashSet<>();
        Map<Long, String> invNameMap = new HashMap<>();
        for (BidRequirementLine line : bidRequirementLineList) {
            line.setQuoteStatus(YesOrNo.YES.getValue());
            Long id = line.getRequirementLineId();
            if (null == line.getRequirementLineId()) {
            	id = IdGenrator.generate();
            }
            line.setRequirementLineId(id).setRequirementId(bidRequirement.getRequirementId()).
                    setBidingId(bidRequirement.getBidingId());
            if (StringUtils.isBlank(line.getPurchaseRequestNum())) {
                materialMap.put(line.getTargetId(), line.getTargetDesc());
                if (Objects.nonNull(line.getOuId())) {
                    ouIds.add(line.getOuId());
                } else {
                    invNameMap.put(line.getInvId(), line.getInvName());
                }
            }
            //?????????????????????
            if (!org.springframework.util.CollectionUtils.isEmpty(line.getMaterialPrices())) {
                String nowJSON = JSON.toJSONString(line.getMaterialPrices());
                line.setPriceJson(nowJSON);
            }

        }
        //?????????????????????????????????????????????
        checkMaterialWhetherBelongInv(materialMap, ouIds, invNameMap);
        this.saveOrUpdateBatch(bidRequirementLineList);
    }

    private void checkParam(List<BidRequirementLine> bidRequirementLineList, BidRequirement bidRequirement) {
        Assert.isTrue(CollectionUtils.isNotEmpty(bidRequirementLineList), "????????????????????????");
//        boolean needGroupFlag = false;
        /**
         * ??????????????????
         */
        boolean needGroupFlag = true;
        Biding biding = bidingService.getById(bidRequirement.getBidingId());
//        if (BidingAwardWayEnum.COMBINED_DECISION.getValue().equals(biding.getBidingAwardWay())) {
//            needGroupFlag = true;
//        }
        HashSet<String> codeSet = new HashSet<>();
        for (BidRequirementLine line : bidRequirementLineList) {
            if (Objects.isNull(line.getQuantity())) {
                throw new BidException(String.format("??????[%s]???[????????????]????????????", line.getTargetDesc()));
            }
            if (Objects.nonNull(line.getOuId()) && Objects.nonNull(line.getOrgId())) {
                throw new BaseException(String.format("??????[%s]??????????????????Ou??????????????????!", line.getTargetDesc()));
            }
            boolean orgOrInvNull = Objects.isNull(line.getOrgId()) || Objects.isNull(line.getInvId());
            if (orgOrInvNull && Objects.isNull(line.getOuId())) {
                throw new BaseException(String.format("??????[%s]?????????????????????????????????Ou?????????!", line.getTargetDesc()));
            }
            //?????????????????????????????????????????????????????????????????????????????????
            if (needGroupFlag) {
                if (StringUtils.isBlank(line.getItemGroup())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("???????????????[????????????]?????????????????????[??????]?????????"));
                }
            }
            Assert.notNull(line.getItemGroup(), "????????????????????????");
            Assert.notNull(line.getTargetNum(), "????????????????????????");
            Assert.notNull(line.getTargetDesc(), "????????????????????????");
            Assert.notNull(line.getPriceType(), "????????????????????????");
//            Assert.notNull(line.getTaxCurrentPrice(), "????????????????????????");
            Assert.notNull(line.getQuantity(), "??????????????????????????????");
            if (Objects.isNull(line.getPriceEndTime()) || Objects.isNull(line.getPriceStartTime())) {
                throw new BaseException(String.format("??????[%s]??????????????????????????????", line.getTargetDesc()));
            }
//            long count = biding.getDefaultPriceValidFrom().getTime() - line.getPriceStartTime().getTime();
//            if (count / (1000 * 3600 * 24) != 0) {
//                throw new BaseException(String.format("??????[%s]?????????????????????????????????????????????", line.getTargetDesc()));
//            }
//            count=biding.getDefaultPriceValidTo().getTime()-line.getPriceEndTime().getTime();
//            if (count / (1000 * 3600 * 24) != 0) {
//                throw new BaseException(String.format("??????[%s]?????????????????????????????????????????????", line.getTargetDesc()));
//            }
            long dayCount = (line.getPriceEndTime().getTime() - line.getPriceStartTime().getTime()) / (1000 * 3600 * 24);
            if (dayCount > 365) {
                throw new BaseException(String.format("??????[%s]????????????????????????????????????365???", line.getTargetDesc()));
            }
            codeSet.add(line.getCategoryCode());
        }
//        List<DictItem> priceBlackList = baseClient.listDictItemByDictCode("PRICE_BLACK_LIST");
//        if (!org.springframework.util.CollectionUtils.isEmpty(priceBlackList)) {
//            Set<String> blackCode = priceBlackList.stream().map(DictItem::getDictItemCode).collect(Collectors.toSet());
//            for (String nowCode : codeSet) {
//                if (blackCode.contains(nowCode) && Objects.equals(biding.getIsSyncToPriceLibrary(), "Y")) {
//                    throw new BaseException(String.format("???????????????????????????????????????%s????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????", nowCode));
//                }
//            }
//        }
    }

    private void checkParam(BidRequirementLine line) {
        Assert.notNull(line, "????????????????????????");
        Assert.notNull(line.getItemGroup(), "????????????????????????");
        Assert.notNull(line.getTargetNum(), "????????????????????????");
        Assert.notNull(line.getTargetDesc(), "????????????????????????");
        Assert.notNull(line.getQuantity(), "????????????????????????");
        Assert.notNull(line.getQuantity(), "??????????????????????????????");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreCheck(checkMethod = "checkParam")
    public void updateBatch(List<BidRequirementLine> bidRequirementLineList, BidRequirement bidRequirement) {
        Assert.isTrue(CollectionUtils.isNotEmpty(bidRequirementLineList), "????????????????????????");
        List<BidRequirementLine> addList = new LinkedList<>();
        List<BidRequirementLine> updateList = new LinkedList<>();
        Map<Long, String> materialMap = new HashMap<>();
        HashSet<Long> ouIds = new HashSet<>();
        Map<Long, String> invNameMap = new HashMap<>();
        for (BidRequirementLine line : bidRequirementLineList) {
            Long requirementLineId = line.getRequirementLineId();
            //??????
            if (requirementLineId == null) {
                Long id = IdGenrator.generate();
                line.setRequirementLineId(id).setRequirementId(bidRequirement.getRequirementId()).
                        setBidingId(bidRequirement.getBidingId());
                addList.add(line);
            } else {
                //??????
                updateList.add(line);
            }
            if (StringUtils.isBlank(line.getPurchaseRequestNum())) {
                materialMap.put(line.getTargetId(), line.getTargetDesc());
                if (Objects.nonNull(line.getOuId())) {
                    ouIds.add(line.getOuId());
                } else {
                    invNameMap.put(line.getInvId(), line.getInvName());
                }
            }
        }
        checkMaterialWhetherBelongInv(materialMap, ouIds, invNameMap);

        List<BidRequirementLine> shouldDeleteList = list(Wrappers.lambdaQuery(BidRequirementLine.class)
                .select(BidRequirementLine::getRequirementLineId,
                        BidRequirementLine::getTargetId,
                        BidRequirementLine::getQuantity,
                        BidRequirementLine::getAmount,
                        BidRequirementLine::getPriceStartTime,
                        BidRequirementLine::getPriceEndTime,
                        BidRequirementLine::getOuNumber,
                        BidRequirementLine::getDeliveryPlace,
                        BidRequirementLine::getPriceType,
                        BidRequirementLine::getPurchaseType,
                        BidRequirementLine::getTradeTerm,
                        BidRequirementLine::getTransportType,
                        BidRequirementLine::getShowRequireNum,
                        BidRequirementLine::getWarrantyPeriod,
                        BidRequirementLine::getCategoryCode
                )
                .eq(BidRequirementLine::getRequirementId, bidRequirement.getRequirementId())
        );


        List<BidRequirementLine> waitToJudgeChangeList = new LinkedList<>();
        for (int i = shouldDeleteList.size() - 1; i >= 0; i--) {
            boolean find = false;
            BidRequirementLine shouldDeleteEntity = shouldDeleteList.get(i);
            for (BidRequirementLine bidRequirementLine : updateList) {
                if (Objects.equals(bidRequirementLine.getRequirementLineId(), shouldDeleteEntity.getRequirementLineId())) {
                    find = true;
                    break;
                }
            }
            //????????????????????????????????????????????????????????????
            if (find) {
                waitToJudgeChangeList.add(shouldDeleteList.remove(i));
            }
        }
        if (CollectionUtils.isNotEmpty(shouldDeleteList)) {
            List<Long> shouldDeleteIds = shouldDeleteList.stream().map(BidRequirementLine::getRequirementLineId).collect(Collectors.toList());
            removeByIds(shouldDeleteIds);
            relatePriceService.remove(Wrappers.lambdaQuery(OuRelatePrice.class)
                    .in(OuRelatePrice::getRequirementLineId, shouldDeleteIds));
            iQuoteAuthorizeService.remove(Wrappers.lambdaQuery(QuoteAuthorize.class)
                    .in(QuoteAuthorize::getRequirementLineId, shouldDeleteIds)
            );
        }
        if (CollectionUtils.isNotEmpty(addList)) {
            saveBatch(addList);
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            Map<Long, BidRequirementLine> judgeMap = waitToJudgeChangeList.stream().collect(Collectors.toMap(e -> e.getRequirementLineId(), Function.identity()));
            for (int i = updateList.size() - 1; i >= 0; i--) {
                BidRequirementLine current = updateList.get(i);
                BidRequirementLine temp = judgeMap.get(current.getRequirementLineId());
                boolean isFieldAllEquals = ObjectUtil.isFieldAllEquals(temp, current);
                boolean jsonEquals = true;
                //?????????????????????
                if (!org.springframework.util.CollectionUtils.isEmpty(current.getMaterialPrices())) {
                    Collections.sort(current.getMaterialPrices(), Comparator.comparing(BaseMaterialPriceDTO::getBaseMaterialId));
                    String priceJson = temp.getPriceJson();
                    String nowJSON = JSON.toJSONString(current.getMaterialPrices());
                    jsonEquals = Objects.equals(priceJson, nowJSON);
                    current.setPriceJson(nowJSON);
                }
                if (isFieldAllEquals && jsonEquals) {
                    updateList.remove(i);
                }
            }
            judgeMap = null;
            waitToJudgeChangeList = null;
            if (CollectionUtils.isNotEmpty(updateList)) {
                updateBatchById(updateList);
            }
        }
    }


    @Override
    @Transactional
    public void updateTargetPriceBatch(List<BidRequirementLine> bidRequirementLineList) {
//        Assert.isTrue(CollectionUtils.isNotEmpty(bidRequirementLineList), "???????????????????????????");
        //??????????????????
        Long bidingId = bidRequirementLineList.get(0).getBidingId();
        Integer checkCount = this.count(new QueryWrapper<>(new BidRequirementLine().setBidingId(bidingId)));
        List<String> targetNumList = bidRequirementLineList.stream().map(BidRequirementLine::getTargetNum).collect(Collectors.toList());
        long inputCount = targetNumList.stream().distinct().count();
        if (checkCount.longValue() != inputCount) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????,?????????????????????"));
        }

        for (BidRequirementLine line : bidRequirementLineList) {
//            Assert.notNull(line.getTargetPrice(), "?????????????????????");
            if (StringUtils.isBlank(line.getTargetNum()) || line.getBidingId() == null) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????id????????????"));
            }
            BidRequirementLine oldLine = this.getOne(new QueryWrapper<>(
                    new BidRequirementLine().setTargetNum(line.getTargetNum()).setBidingId(line.getBidingId())));
            if (oldLine == null) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????", line.getTargetNum()));
            }
            if (!line.getBidingId().equals(oldLine.getBidingId())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????", line.getTargetNum()));
            }

            BidRequirementLine setLine = new BidRequirementLine();
            setLine.setTargetPrice(line.getTargetPrice());
            this.update(setLine, new QueryWrapper<>(oldLine));
        }
    }

    @Override
    public PageInfo<BidRequirementLine> listPage(BidRequirementLine bidRequirementLine) {
        PageUtil.startPage(bidRequirementLine.getPageNum(), bidRequirementLine.getPageSize());
        QueryWrapper<BidRequirementLine> wrapper = new QueryWrapper<BidRequirementLine>(bidRequirementLine);
        List<BidRequirementLine> bidRequirementLineList = this.list(wrapper);
        for (BidRequirementLine requirementLine : bidRequirementLineList) {
            if (StringUtils.isNotBlank(requirementLine.getPriceJson())) {
                requirementLine.setMaterialPrices(JSON.parseArray(requirementLine.getPriceJson(), BaseMaterialPriceDTO.class));
            }
        }
        return new PageInfo<BidRequirementLine>(bidRequirementLineList);
    }

    @Override
    public List<BidRequirementLine> importExcelInfo(List<Object> list) {
        List<BidRequirementLine> bidRequirementLineList = new ArrayList<>();
        List<String> orgNameList = new ArrayList<>();
        List<String> categoryNameList = new ArrayList<>();
        List<String> targetNumList = new ArrayList<>();
        List<String> uomCodeList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);

            BidRequirementLineImportVO vo = (BidRequirementLineImportVO) obj;
            BidRequirementLine line = new BidRequirementLine();
            //???????????????

            PurchaseTax purchaseTax = baseClient.getByTaxKeyAndLanguage(vo.getTaxKey(), LocaleHandler.getLocaleKey());
            Assert.notNull(purchaseTax, "???" + (i + 1) + "?????????????????????");

            BeanCopyUtil.copyProperties(line, vo);
            line.setTaxRate(purchaseTax.getTaxCode());

            checkParam(line);

            orgNameList.add(line.getOrgName());
            categoryNameList.add(line.getCategoryName());
            targetNumList.add(line.getTargetNum());
            uomCodeList.add(line.getUomCode());

            bidRequirementLineList.add(line);
        }
        log.debug("??????????????????" + JSON.toJSONString(bidRequirementLineList));

        List<Organization> organizationList = baseClient.getOrganizationByNameList(orgNameList);
        Map<String, Long> organizatioMap = organizationList.stream().collect(
                Collectors.toMap(Organization::getOrganizationName, Organization::getOrganizationId));
        log.debug("????????????????????????" + JSON.toJSONString(organizatioMap));

        List<PurchaseCategory> purchaseCategoryList = baseClient.listPurchaseCategoryByNameBatch(categoryNameList);
        Map<String, Long> purchaseCategoryMap = purchaseCategoryList.stream().collect(
                Collectors.toMap(PurchaseCategory::getCategoryName, PurchaseCategory::getCategoryId));
        log.debug("???????????????????????????" + JSON.toJSONString(purchaseCategoryMap));

        List<MaterialItem> materialItemList = baseClient.listMaterialByCodeBatch(targetNumList);
        Map<String, MaterialItem> materialItemMap = materialItemList.stream().collect(
                Collectors.toMap(MaterialItem::getMaterialCode, materialItem -> materialItem));
        log.debug("?????????????????????" + JSON.toJSONString(materialItemMap));

        List<PurchaseUnit> purchaseUnitList = baseClient.listPurchaseUnitByCodeList(uomCodeList);
        Map<String, String> purchaseUnitMap = purchaseUnitList.stream().collect(
                Collectors.toMap(PurchaseUnit::getUnitCode, PurchaseUnit::getUnitName));
        log.debug("?????????????????????" + JSON.toJSONString(purchaseUnitMap));

        for (int i = 0; i < bidRequirementLineList.size(); i++) {
            BidRequirementLine line = bidRequirementLineList.get(i);
            log.debug("???" + i + "?????????:" + line);
            //??????????????????ID
            Long orgId = organizatioMap.get(line.getOrgName());
            Assert.notNull(orgId, "???" + (i + 1) + "??????????????????????????????,????????????????????????");
            line.setOrgId(orgId);

            //??????????????????ID
            Long categoryId = purchaseCategoryMap.get(line.getCategoryName());
            Assert.notNull(categoryId, "???" + (i + 1) + "?????????????????????????????????,???????????????????????????");
            line.setCategoryId(categoryId);

            //????????????ID?????????
            MaterialItem materialItem = materialItemMap.get(line.getTargetNum());
            Assert.notNull(materialItem, "???" + (i + 1) + "???????????????????????????,?????????????????????");
            line.setTargetId(materialItem.getMaterialId()).setTargetNum(materialItem.getMaterialCode());

            //??????????????????
            String uomDesc = purchaseUnitMap.get(line.getUomCode());
            Assert.isTrue(StringUtils.isNotBlank(uomDesc), "???" + (i + 1) + "???????????????????????????,?????????????????????");
            if (!uomDesc.equals(line.getUomDesc())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????,?????????????????????:", String.valueOf(i)));
            }

        }

        return bidRequirementLineList;
    }

    @Override
    public void excelResponse(HttpServletResponse response, Long bidingId) {
        //?????????????????????????????????
        List<BidRequirementLine> dataList =
                this.list(new QueryWrapper<>(new BidRequirementLine().setBidingId(bidingId)));
        log.debug("dataList:" + JSON.toJSONString(dataList));
        List<BidRequirementLineImportVO> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dataList)) {
            voList = BeanCopyUtil.copyListProperties(dataList, BidRequirementLineImportVO.class);
        }
        log.debug("voList:" + JSON.toJSONString(voList));
        Biding biding = bidingService.getById(bidingId);
        String bidingName = (biding == null ? "" : biding.getBidingName());
        //??????????????????/?????????????????????
        List<PurchaseUnit> purchaseUnitList = baseClient.listAllEnablePurchaseUnit();
        log.debug("??????????????????:" + JSON.toJSONString(purchaseUnitList));
        String[] unitSpinner = purchaseUnitList.stream().map(unit -> new StringBuilder(
                unit.getUnitCode()).append("/").append(unit.getUnitName()).toString())
                .collect(Collectors.toList()).toArray(new String[purchaseUnitList.size()]);
        log.debug("??????????????????" + JSON.toJSONString(unitSpinner));
        String fileName = bidingName + "-??????????????????.xls";
        Map<Integer, String[]> mapDropDown = new HashMap<Integer, String[]>();
        mapDropDown.put(17, unitSpinner);
        File file = new File(fileName);
        EasyExcel.write(file, BidRequirementLineImportVO.class).
                registerWriteHandler(new SpinnerSheetWriteHandler(mapDropDown)).
                sheet().doWrite(voList);

        try {
            byte[] buffer = FileUtils.readFileToByteArray(file);
            file.delete();
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            fileName = URLEncoder.encode(fileName, "UTF-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.getOutputStream().write(buffer);
            response.getOutputStream().close();
        } catch (IOException e) {
            log.error("???????????????:" + e.getMessage());
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????,????????????????????????"));
        }
    }

    @Override
    public void importModelDownload(HttpServletResponse response) throws Exception {
        String fileName = "????????????????????????";
        ArrayList<BidRequirementLineDto> bidRequirementLineDtos = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        EasyExcelUtil.writeExcelWithModel(outputStream, fileName, bidRequirementLineDtos, BidRequirementLineDto.class);
    }

    @Override
    @Transactional
    public Map<String, Object> importExcel(BidRequirement bidRequirement, MultipartFile file, Fileupload fileupload) throws Exception {
        // ??????????????????
        Assert.notNull(bidRequirement.getBidingId(), "??????id????????????");
        // ????????????
        EasyExcelUtil.checkParam(file, fileupload);
        // ????????????
        List<BidRequirementLineDto> bidRequirementLineDtos = readData(file);
        // ?????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ????????????
        List<BidRequirementLine> bidRequirementLines = checkData(bidRequirementLineDtos, errorFlag);
        if (errorFlag.get()) {
            // ??????
            fileupload.setFileSourceName("????????????????????????");
            Fileupload fileupload1 = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    bidRequirementLineDtos, BidRequirementLineDto.class, file.getName(), file.getOriginalFilename(), file.getContentType());
            return ImportStatus.importError(fileupload1.getFileuploadId(), fileupload1.getFileSourceName());
        } else {
            // ??????????????????????????????
            Long requirementId = bidRequirement.getRequirementId();
            if (StringUtil.notEmpty(requirementId)) {
                iBidRequirementService.updateById(bidRequirement);
            } else {
                bidRequirement.setRequirementId(IdGenrator.generate());
                iBidRequirementService.save(bidRequirement);
            }
            // ???????????????
            this.remove(new QueryWrapper<>(new BidRequirementLine().
                    setBidingId(bidRequirement.getBidingId()).setRequirementId(bidRequirement.getRequirementId())));
            if (CollectionUtils.isNotEmpty(bidRequirementLines)) {
                bidRequirementLines.forEach(bidRequirementLine -> {
                    bidRequirementLine.setBidingId(bidRequirement.getBidingId());
                    bidRequirementLine.setRequirementId(bidRequirement.getRequirementId());
                    bidRequirementLine.setRequirementLineId(IdGenrator.generate());
                });
                this.saveBatch(bidRequirementLines);
            }
            return ImportStatus.importSuccess();
        }
    }

    /**
     * ??????????????????
     *
     * @param bidRequirementLineDtos
     * @param errorFlag
     */
    private List<BidRequirementLine> checkData(List<BidRequirementLineDto> bidRequirementLineDtos, AtomicBoolean errorFlag) {
        List<BidRequirementLine> bidRequirementLines = new ArrayList<>();
        /**
         * ??????????????????????????????
         * 1. OU????????????OU???
         * 2. ??????????????????+????????????????????????????????????
         * 3. ??????????????????????????????
         * 4. ??????????????????????????????????????????
         * 5. ????????????????????????????????????
         * 6. ????????????????????????????????????
         */
        Map<String, String> cityProvinceSuccessMap = new HashMap<>();
        Map<String, String> cityProvinceErrorMap = new HashMap<>();


        Map<String, List<BaseOuGroup>> ouCodeMap = new HashMap<>();
        Map<String, List<Organization>> orgMap = new HashMap<>();
        Map<String, List<CompanyInfo>> companyInfoMap = new HashMap<>();
        Map<String, List<MaterialItem>> materialMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(bidRequirementLineDtos)) {
            // ou?????????
            List<String> ouCodeList = new ArrayList<>();
            // ????????????
            List<String> orgList = new ArrayList<>();
            // ???????????????
            List<String> vendorNameList = new ArrayList<>();
            // ????????????
            List<String> targetNumList = new ArrayList<>();
            bidRequirementLineDtos.forEach(bidRequirementLineDto -> {
                String ouCode = bidRequirementLineDto.getOuCode();
                String orgName = bidRequirementLineDto.getOrgName();
                String invName = bidRequirementLineDto.getInvName();
                String supplierName = bidRequirementLineDto.getAwardedSupplierName();
                String targetNum = bidRequirementLineDto.getTargetNum();
                Optional.ofNullable(ouCode).ifPresent(s -> ouCodeList.add(s.trim()));
                Optional.ofNullable(orgName).ifPresent(s -> orgList.add(s.trim()));
                Optional.ofNullable(invName).ifPresent(s -> orgList.add(s.trim()));
                Optional.ofNullable(supplierName).ifPresent(s -> vendorNameList.add(s.trim()));
                Optional.ofNullable(targetNum).ifPresent(s -> targetNumList.add(s.trim()));
            });

            List<BaseOuGroup> baseOuGroups = baseClient.queryOuByOuCodeList(ouCodeList.stream().distinct().collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(baseOuGroups)) {
                ouCodeMap = baseOuGroups.stream().collect(Collectors.groupingBy(BaseOuGroup::getOuGroupCode));
            }

            List<Organization> organizationList = baseClient.getOrganizationByNameList(orgList.stream().distinct().collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(organizationList)) {
                orgMap = organizationList.stream().collect(Collectors.groupingBy(Organization::getOrganizationName));
            }

            List<CompanyInfo> companyInfos = supplierClient.getComponyByNameList(vendorNameList.stream().distinct().collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(companyInfos)) {
                companyInfoMap = companyInfos.stream().collect(Collectors.groupingBy(CompanyInfo::getCompanyName));
            }
            List<MaterialItem> materialItems = baseClient.queryMaterialItemByCodes(targetNumList.stream().distinct().collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(materialItems)) {
                materialMap = materialItems.stream().collect(Collectors.groupingBy(MaterialItem::getMaterialCode));
            }
        }

        if (CollectionUtils.isNotEmpty(bidRequirementLineDtos)) {
            Map<String, String> dicMap = getImportDic();
            // ????????????
            for (BidRequirementLineDto bidRequirementLineDto : bidRequirementLineDtos) {
                BidRequirementLine bidRequirementLine = new BidRequirementLine();
                StringBuffer errorMsg = new StringBuffer();


                // ??????OU?????????
                String ouCode = bidRequirementLineDto.getOuCode();
                if (StringUtil.notEmpty(ouCode)) {
                    ouCode = ouCode.trim();
                    if (null != ouCodeMap.get(ouCode)) {
                        BaseOuGroup baseOuGroup = ouCodeMap.get(ouCode).get(0);
                        bidRequirementLine.setOuId(baseOuGroup.getOuGroupId());
                        bidRequirementLine.setOuName(baseOuGroup.getOuGroupName());
                        bidRequirementLine.setOuNumber(baseOuGroup.getOuGroupCode());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("OU????????????????????????; ");
                    }

                }
//                String ouCode = bidRequirementLineDto.getOuCode();
//                if(StringUtil.notEmpty(ouCode)){
//                    ouCode = ouCode.trim();
//                    BaseOuGroupQueryDTO baseOuGroupQueryDTO = new BaseOuGroupQueryDTO();
//                    baseOuGroupQueryDTO.setOuGroupCode(ouCode);
//                    List<BaseOuGroupDetailVO> baseOuGroupDetailVOS = baseClient.queryOuDetailByDto(baseOuGroupQueryDTO);
//                    if(CollectionUtils.isNotEmpty(baseOuGroupDetailVOS)){
//                        BaseOuGroupDetailVO baseOuGroupDetailVO = baseOuGroupDetailVOS.get(0);
//                        bidRequirementLine.setOuId(baseOuGroupDetailVO.getOuGroupId());
//                        bidRequirementLine.setOuName(baseOuGroupDetailVO.getOuGroupName());
//                        bidRequirementLine.setOuNumber(baseOuGroupDetailVO.getOuGroupCode());
//                    }else {
//                        errorFlag.set(true);
//                        errorMsg.append("OU????????????????????????; ");
//                    }
//
//                }

                // ???????????????ou
                String baseOu = bidRequirementLineDto.getBaseOu();
                if (StringUtil.notEmpty(baseOu)) {
                    baseOu = baseOu.trim();
                    if (YesOrNo.YES.getValue().equals(baseOu) || YesOrNo.NO.getValue().equals(baseOu)) {
                        bidRequirementLine.setBaseOu(baseOu);
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("???????????????ou?????????\"Y\"???\"N\"; ");
                    }
                }

                // ????????????
                String orgName = bidRequirementLineDto.getOrgName();
                if (StringUtil.notEmpty(orgName)) {
                    orgName = orgName.trim();
                    if (null != orgMap.get(orgName)) {
                        Organization organization = orgMap.get(orgName).get(0);
                        bidRequirementLine.setOrgId(organization.getOrganizationId());
                        bidRequirementLine.setOrgCode(organization.getOrganizationCode());
                        bidRequirementLine.setOrgName(organization.getOrganizationName());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("?????????????????????; ");
                    }
                }
//                if(StringUtil.notEmpty(orgName)){
//                    orgName = orgName.trim();
//                    Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgName));
//                    if(null != organization && StringUtil.notEmpty(organization.getOrganizationId())){
//                        bidRequirementLine.setOrgId(organization.getOrganizationId());
//                        bidRequirementLine.setOrgCode(organization.getOrganizationCode());
//                        bidRequirementLine.setOrgName(organization.getOrganizationName());
//                    }else {
//                        errorFlag.set(true);
//                        errorMsg.append("?????????????????????; ");
//                    }
//                }

                // ??????????????????
                String invName = bidRequirementLineDto.getInvName();
                if (StringUtil.notEmpty(invName)) {
                    invName = invName.trim();
                    if (null != orgMap.get(invName)) {
                        Organization organization = orgMap.get(invName).get(0);
                        if (StringUtil.notEmpty(bidRequirementLine.getOrgId()) && organization.getParentOrganizationIds().contains(String.valueOf(bidRequirementLine.getOrgId()))) {
                            bidRequirementLine.setInvId(organization.getOrganizationId());
                            bidRequirementLine.setInvCode(organization.getOrganizationCode());
                            bidRequirementLine.setInvName(organization.getOrganizationName());
                        } else {
                            errorFlag.set(true);
                            errorMsg.append("????????????????????????????????????????????????; ");
                        }
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("?????????????????????; ");
                    }
                }
//                if(StringUtil.notEmpty(invName)){
//                    invName = invName.trim();
//                    Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(invName));
//                    if(null != organization && StringUtil.notEmpty(organization.getOrganizationId())){
//                        if(StringUtil.notEmpty(bidRequirementLine.getOrgId()) && organization.getParentOrganizationIds().contains(String.valueOf(bidRequirementLine.getOrgId()))){
//                            bidRequirementLine.setInvId(organization.getOrganizationId());
//                            bidRequirementLine.setInvCode(organization.getOrganizationCode());
//                            bidRequirementLine.setInvName(organization.getOrganizationName());
//                        }else {
//                            errorFlag.set(true);
//                            errorMsg.append("????????????????????????????????????????????????; ");
//                        }
//                    }else {
//                        errorFlag.set(true);
//                        errorMsg.append("?????????????????????; ");
//                    }
//                }

                if (StringUtil.notEmpty(bidRequirementLine.getOuId()) &&
                        (StringUtil.notEmpty(bidRequirementLine.getOrgId()) || StringUtil.notEmpty(bidRequirementLine.getInvId()))) {
                    errorFlag.set(true);
                    errorMsg.append("ou??????(???????????????????????????)???????????????; ");
                }

                // ????????????
                String deliveryPlace = bidRequirementLineDto.getDeliveryPlace();
                if (StringUtil.notEmpty(deliveryPlace)) {
                    // ??????  (????????????: ?????????/?????????)
                    deliveryPlace = deliveryPlace.trim();


                    if (!cityProvinceErrorMap.containsKey(deliveryPlace)) {
                        if (!cityProvinceSuccessMap.containsKey(deliveryPlace)) {
                            if (deliveryPlace.contains("/")) {
                                List<String> strings = Arrays.asList(deliveryPlace.split("/"));
                                if (CollectionUtils.isNotEmpty(strings) && strings.size() == 2) {
                                    String province = strings.get(0).trim();
                                    String city = strings.get(1).trim();
                                    if (StringUtil.isEmpty(dicMap.get(province))) {
                                        errorMsg.append("???????????????; ");
                                        cityProvinceErrorMap.put(deliveryPlace, "???????????????; ");
                                        errorFlag.set(true);
                                    } else {
                                        // ??????id
                                        String parentId = dicMap.get(province);
                                        CityParamDto cityParamDto = new CityParamDto();
                                        cityParamDto.setParentId(Long.parseLong(parentId));
                                        cityParamDto.setAreaName(city);
                                        List<AreaDTO> areaDTOS = baseClient.checkCity(cityParamDto);
                                        if (CollectionUtils.isEmpty(areaDTOS)) {
                                            cityProvinceErrorMap.put(deliveryPlace, "??????????????????????????????; ");
                                            errorMsg.append("??????????????????????????????; ");
                                            errorFlag.set(true);
                                        } else {
                                            String cityId = areaDTOS.get(0).getCityId().toString();
                                            String deliveryPlaceNew = JSON.toJSONString(Arrays.asList(parentId, cityId));
                                            cityProvinceSuccessMap.put(deliveryPlace, deliveryPlaceNew);
                                            bidRequirementLine.setDeliveryPlace(deliveryPlaceNew);
                                        }
                                    }
                                } else {
                                    errorFlag.set(true);
                                    cityProvinceErrorMap.put(deliveryPlace, "???????????????????????????; ");
                                    errorMsg.append("???????????????????????????; ");
                                }
                            } else {
                                errorFlag.set(true);
                                cityProvinceErrorMap.put(deliveryPlace, "???????????????????????????\"/\"???????????????; ");
                                errorMsg.append("???????????????????????????\"/\"???????????????; ");
                            }
                        } else {
                            bidRequirementLine.setDeliveryPlace(cityProvinceSuccessMap.get(deliveryPlace));
                        }
                    } else {
                        errorFlag.set(true);
                        errorMsg.append(cityProvinceErrorMap.get(deliveryPlace));
                    }
                }

                // ????????????
                String priceType = bidRequirementLineDto.getPriceType();
                if (StringUtil.notEmpty(priceType)) {
                    priceType = priceType.trim();
                    if (StringUtil.notEmpty(dicMap.get(priceType))) {
                        bidRequirementLine.setPriceType(dicMap.get(priceType));
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                } else {
                    errorFlag.set(true);
                    errorMsg.append("????????????????????????; ");
                }

                // ????????????
                String purchaseType = bidRequirementLineDto.getPurchaseType();
                if (StringUtil.notEmpty(purchaseType)) {
                    purchaseType = purchaseType.trim();
                    if (StringUtil.notEmpty(dicMap.get(purchaseType))) {
                        bidRequirementLine.setPurchaseType(dicMap.get(purchaseType));
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ????????????
                String tradeTerm = bidRequirementLineDto.getTradeTerm();
                if (StringUtil.notEmpty(tradeTerm)) {
                    tradeTerm = tradeTerm.trim();
                    if (StringUtil.notEmpty(dicMap.get(tradeTerm))) {
                        bidRequirementLine.setTradeTerm(dicMap.get(tradeTerm));
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ????????????
                String transportType = bidRequirementLineDto.getTransportType();
                if (StringUtil.notEmpty(transportType)) {
                    transportType = transportType.trim();
                    if (StringUtil.notEmpty(dicMap.get(transportType))) {
                        bidRequirementLine.setTransportType(dicMap.get(transportType));
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ?????????????????????
                String showRequireNum = bidRequirementLineDto.getShowRequireNum();
                if (StringUtil.notEmpty(showRequireNum)) {
                    showRequireNum = showRequireNum.trim();
                    if (YesOrNo.YES.getValue().equals(showRequireNum) || YesOrNo.NO.getValue().equals(showRequireNum)) {
                        bidRequirementLine.setShowRequireNum(showRequireNum);
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????\"Y\"???\"N\"; ");
                    }
                }

                // ?????????(???)
                String warrantyPeriod = bidRequirementLineDto.getWarrantyPeriod();
                if (StringUtil.notEmpty(warrantyPeriod)) {
                    warrantyPeriod = warrantyPeriod.trim();
                    try {
                        int i = Integer.parseInt(warrantyPeriod);
                        bidRequirementLine.setWarrantyPeriod(i);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("?????????(???)???????????????; ");
                    }
                }

                // ??????????????????
                String awardedSupplierName = bidRequirementLineDto.getAwardedSupplierName();
                if (StringUtil.notEmpty(awardedSupplierName)) {
                    awardedSupplierName = awardedSupplierName.trim();
                    if (null != companyInfoMap.get(awardedSupplierName)) {
                        CompanyInfo companyInfo = companyInfoMap.get(awardedSupplierName).get(0);
                        bidRequirementLine.setAwardedSupplierId(companyInfo.getCompanyId());
                        bidRequirementLine.setAwardedSupplierName(companyInfo.getCompanyName());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("???????????????????????????; ");
                    }
                }
//                if(StringUtil.notEmpty(awardedSupplierName)){
//                    awardedSupplierName = awardedSupplierName.trim();
//                    CompanyInfo info = supplierClient.getCompanyInfoByParam(new CompanyInfo().setCompanyName(awardedSupplierName));
//                    if(null != info && StringUtil.notEmpty(info.getCompanyId())){
//                        bidRequirementLine.setAwardedSupplierId(info.getCompanyId());
//                        bidRequirementLine.setAwardedSupplierName(info.getCompanyName());
//                    }else {
//                        errorFlag.set(true);
//                        errorMsg.append("???????????????????????????; ");
//                    }
//                }

                // ????????????
                String targetNum = bidRequirementLineDto.getTargetNum();
                if (StringUtil.notEmpty(targetNum)) {
                    targetNum = targetNum.trim();
                    if (CollectionUtils.isNotEmpty(materialMap.get(targetNum))) {
                        MaterialItem materialItem = materialMap.get(targetNum).get(0);
                        bidRequirementLine.setTargetId(materialItem.getMaterialId());
                        bidRequirementLine.setTargetNum(materialItem.getMaterialCode());
                        bidRequirementLine.setTargetDesc(org.springframework.util.StringUtils.isEmpty(bidRequirementLineDto.getTargetDesc()) ? materialItem.getMaterialName() : bidRequirementLineDto.getTargetDesc());
                        bidRequirementLine.setCategoryId(materialItem.getCategoryId());
                        bidRequirementLine.setCategoryCode(materialItem.getCategoryCode());
                        bidRequirementLine.setCategoryName(materialItem.getCategoryName());
                        bidRequirementLine.setUomCode(materialItem.getUnit());
                        bidRequirementLine.setUomDesc(materialItem.getUnitName());
                        bidRequirementLine.setFormulaValue(materialItem.getPricingFormulaValue());
                        bidRequirementLine.setFormulaId(materialItem.getPricingFormulaHeaderId());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("?????????????????????; ");
                    }
                } else if (StringUtil.notEmpty(bidRequirementLineDto.getFormulaValue())) {
                    errorFlag.set(true);
                    errorMsg.append("?????????????????????????????????; ");
                }
//                if(StringUtil.notEmpty(targetDesc)){
//                    targetDesc = targetDesc.trim();
//                    List<MaterialItem> materialItems = baseClient.listMaterialByParam(new MaterialItem().setMaterialName(targetDesc));
//                    if(CollectionUtils.isNotEmpty(materialItems)){
//                        MaterialItem materialItem = materialItems.get(0);
//                        bidRequirementLine.setTargetId(materialItem.getMaterialId());
//                        bidRequirementLine.setTargetNum(materialItem.getMaterialCode());
//                        bidRequirementLine.setTargetDesc(materialItem.getMaterialName());
//                        bidRequirementLine.setCategoryId(materialItem.getCategoryId());
//                        bidRequirementLine.setCategoryCode(materialItem.getCategoryCode());
//                        bidRequirementLine.setCategoryName(materialItem.getCategoryName());
//                        bidRequirementLine.setUomCode(materialItem.getUnit());
//                        bidRequirementLine.setUomDesc(materialItem.getUnitName());
//
//                        // ????????????????????????
//                        List<PricingFormulaHeaderVO> pricingFormulaHeaderVOS = pricingFormulaCalculateClient.getPricingFormulaHeaderByMaterialId(materialItem.getMaterialId());
//                        if(CollectionUtils.isNotEmpty(pricingFormulaHeaderVOS)){
//                            PricingFormulaHeaderVO pricingFormulaHeaderVO = pricingFormulaHeaderVOS.get(0);
//                            bidRequirementLine.setFormulaValue(pricingFormulaHeaderVO.getPricingFormulaValue());
//                            bidRequirementLine.setFormulaId(pricingFormulaHeaderVO.getPricingFormulaHeaderId());
//                        }
//                    }else {
//                        errorFlag.set(true);
//                        errorMsg.append("?????????????????????; ");
//                    }
//                }else if(StringUtil.notEmpty(bidRequirementLineDto.getFormulaValue())){
//                    errorFlag.set(true);
//                    errorMsg.append("?????????????????????????????????; ");
//                }

                // ??????????????????
                String priceStartTime = bidRequirementLineDto.getPriceStartTime();
                if (StringUtil.notEmpty(priceStartTime)) {
                    priceStartTime = priceStartTime.trim();
                    try {
                        Date date = DateUtil.parseDate(priceStartTime);
                        bidRequirementLine.setPriceStartTime(date);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ??????????????????
                String priceEndTime = bidRequirementLineDto.getPriceEndTime();
                if (StringUtil.notEmpty(priceEndTime)) {
                    priceEndTime = priceEndTime.trim();
                    try {
                        Date date = DateUtil.parseDate(priceEndTime);
                        bidRequirementLine.setPriceEndTime(date);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ????????????
                String quantity = bidRequirementLineDto.getQuantity();
                if (StringUtil.notEmpty(quantity)) {
                    quantity = quantity.trim();
                    try {
                        int i = Integer.parseInt(quantity);
                        bidRequirementLine.setQuantity((double) i);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("????????????????????????; ");
                    }
                }

                // ??????????????????(??????)
                String amount = bidRequirementLineDto.getAmount();
                if (StringUtil.notEmpty(amount)) {
                    amount = amount.trim();
                    if (StringUtil.isDigit(amount)) {
                        bidRequirementLine.setAmount(new BigDecimal(amount));
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????; ");
                    }
                }

                // ??????
                String comments = bidRequirementLineDto.getComments();
                if (StringUtil.notEmpty(comments)) {
                    comments = comments.trim();
                    bidRequirementLine.setComments(comments);
                }

                // ????????????
                String ceeaDemandDate = bidRequirementLineDto.getCeeaDemandDate();
                if (StringUtil.notEmpty(ceeaDemandDate)) {
                    ceeaDemandDate = ceeaDemandDate.trim();
                    try {
                        Date date = DateUtil.parseDate(ceeaDemandDate);
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        bidRequirementLine.setCeeaDemandDate(localDate);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("????????????????????????; ");
                    }
                }

                if (errorMsg.length() > 0) {
                    bidRequirementLineDto.setErrorMsg(errorMsg.toString());
                } else {
                    bidRequirementLineDto.setErrorMsg(null);
                }
                bidRequirementLines.add(bidRequirementLine);
            }
        }
        return bidRequirementLines;
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    public Map<String, String> getImportDic() {
        HashMap<String, String> dicMap = new HashMap<>();
        // ???????????????
        AreaPramDTO areaPramDTO = new AreaPramDTO();
        areaPramDTO.setQueryType("province");
        List<AreaDTO> regions = baseClient.queryRegionById(areaPramDTO);
        if (CollectionUtils.isNotEmpty(regions)) {
            regions.forEach(region -> {
                dicMap.put(region.getProvince(), region.getProvinceId().toString());
            });
        }

        // ???????????? purchase/purchaseUnit/listAll
        List<PurchaseUnit> purchaseUnits = baseClient.listAllEnablePurchaseUnit();
        if (CollectionUtils.isNotEmpty(purchaseUnits)) {
            purchaseUnits.forEach(purchaseUnit -> {
                dicMap.put(purchaseUnit.getUnitName(), purchaseUnit.getUnitCode());
            });
        }
        // ????????????
        ArrayList<String> dicCodeList = new ArrayList<>();
        // ????????????
        dicCodeList.add("PRICE_TYPE");
        dicCodeList.add("PURCHASE_TYPE");
        dicCodeList.add("trade_clause");
        dicCodeList.add("TRANSF_TYPE");
        dicCodeList.add("TRANSF_TYPE");
        // ????????????
        List<DictItemDTO> dictItemDTOS = baseClient.listByDictCode(dicCodeList);
        // ???????????????Map???
        this.setMapKeyValue(dicMap, dictItemDTOS);
        return dicMap;
    }

    /**
     * ???????????????Map???
     *
     * @param dicMap
     * @param dictItemDTOS1
     */
    public void setMapKeyValue(HashMap<String, String> dicMap, List<DictItemDTO> dictItemDTOS1) {
        if (CollectionUtils.isNotEmpty(dictItemDTOS1)) {
            dictItemDTOS1.forEach(dictItemDTOS -> {
                dicMap.put(dictItemDTOS.getDictItemName(), dictItemDTOS.getDictItemCode());
            });
        }
    }

    private List<BidRequirementLineDto> readData(MultipartFile file) {
        List<BidRequirementLineDto> bidRequirementLineDtos = null;
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<BidRequirementLineDto> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream, listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(BidRequirementLineDto.class).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            bidRequirementLineDtos = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return bidRequirementLineDtos;
    }

    private void checkMaterialWhetherBelongInv(Map<Long, String> materialMap, HashSet<Long> ouIds, Map<Long, String> invNameMap) {
        //?????????????????????????????????????????????
        if (org.springframework.util.CollectionUtils.isEmpty(materialMap)
                || org.springframework.util.CollectionUtils.isEmpty(ouIds)
                || org.springframework.util.CollectionUtils.isEmpty(invNameMap)
        ) {
            return;
        }

        if (!org.springframework.util.CollectionUtils.isEmpty(ouIds)) {
            List<BaseOuGroupDetailVO> baseOuGroupDetailVOS = baseClient.queryOuInfoDetailByIds(ouIds);
            baseOuGroupDetailVOS.stream()
                    .flatMap(e -> e.getDetails().stream()).forEach(inv -> invNameMap.putIfAbsent(inv.getInvId(), inv.getInvName()));
        }
        Map<String, Collection<Long>> paramMap = new HashMap<>();
        paramMap.put("materialIds", materialMap.keySet());
        paramMap.put("invIds", invNameMap.keySet());
        List<MaterialOrg> materialOrgList = baseClient.listMaterialOrgByMaterialIdsAndInvIds(paramMap);
        //???????????????????????????
        Map<Long, List<MaterialOrg>> materialOrgMap = materialOrgList.stream().collect(Collectors.groupingBy(MaterialOrg::getOrganizationId));
        for (Map.Entry<Long, List<MaterialOrg>> materialOrg : materialOrgMap.entrySet()) {
            List<MaterialOrg> value = materialOrg.getValue();
            value.stream().filter(e -> (Objects.equals(e.getUserPurchase(), "N") || Objects.equals(e.getItemStatus(), "N"))
                    && invNameMap.containsKey(e.getOrganizationId())
            )
                    .findAny().ifPresent(one -> {
                throw new BaseException(String.format("????????????[%s]???????????????[%s]??????", one.getOrganizationName(), materialMap.get(one.getMaterialId())));
            });
        }
    }
}
