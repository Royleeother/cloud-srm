package com.midea.cloud.srm.bid.service.impl;

import com.alibaba.dubbo.common.json.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.bid.projectmanagement.bidinitiating.BidFileType;
import com.midea.cloud.common.enums.bid.projectmanagement.evaluation.SelectionStatusEnum;
import com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingProjectStatus;
import com.midea.cloud.common.enums.logistics.BiddingOrderStates;
import com.midea.cloud.common.enums.logistics.CompanyType;
import com.midea.cloud.common.enums.logistics.LogisticsStatus;
import com.midea.cloud.common.enums.logistics.TransportModeEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.srm.bid.mapper.LgtBidingMapper;
import com.midea.cloud.srm.bid.service.*;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.logistics.baseprice.service.BasePriceService;
import com.midea.cloud.srm.logistics.expense.service.IExpenseItemService;
import com.midea.cloud.srm.logistics.expense.service.IPortService;
import com.midea.cloud.srm.logistics.expense.service.IRegionService;
import com.midea.cloud.srm.model.base.purchase.entity.LatestGidailyRate;
import com.midea.cloud.srm.model.logistics.baseprice.entity.BasePrice;
import com.midea.cloud.srm.model.logistics.bid.dto.*;
import com.midea.cloud.srm.model.logistics.bid.entity.*;
import com.midea.cloud.srm.model.logistics.bid.vo.LgtBidInfoVO;
import com.midea.cloud.srm.model.logistics.expense.entity.Port;
import com.midea.cloud.srm.model.logistics.expense.entity.Region;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *
 * </pre>
 *
 * @author wangpr@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-05-27 09:24:20
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class LgtBidingVendorServiceImpl implements ILgtBidingVendorService {
    @Resource
    private ILgtFileService iLgtFileService;
    @Resource
    private ILgtBidRequirementLineService iLgtBidRequirementLineService;
    @Resource
    private ILgtBidShipPeriodService iLgtBidShipPeriodService;
    @Resource
    private ILgtBidTemplateService iLgtBidTemplateService;
    @Resource
    private ILgtFileConfigService iLgtFileConfigService;
    @Resource
    private ILgtVendorFileService iLgtVendorFileService;
    @Resource
    private BaseClient baseClient;
    @Resource
    private ILgtVendorQuotedHeadService iLgtVendorQuotedHeadService;
    @Resource
    private ILgtVendorQuotedLineService iLgtVendorQuotedLineService;
    @Resource
    private ILgtRoundService iLgtRoundService;
    @Resource
    private ILgtVendorQuotedSumService iLgtVendorQuotedSumService;
    @Resource
    private ILgtBidingService iLgtBidingService;
    @Resource
    private LgtBidingMapper lgtBidingMapper;
    @Resource
    private FileCenterClient fileCenterClient;
    @Resource
    private IPortService iPortService;
    @Resource
    private IRegionService iRegionService;
    @Resource
    private BasePriceService basePriceService;
    @Resource
    private IExpenseItemService iExpenseItemService;

    /**
     * ??????????????????????????????
     * @param bidingId
     * @param vendorId
     */
    @Override
    public void exportLgtVendorQuotedLine(Long bidingId, Long vendorId, HttpServletResponse response) throws IOException {
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        // ????????????????????????
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                eq(LgtVendorQuotedLine::getVendorId,vendorId).
                eq(LgtVendorQuotedLine::getBidingId, lgtBiding.getBidingId()).
                eq(LgtVendorQuotedLine::getRound, lgtBiding.getCurrentRound()));
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtVendorQuotedLines),"????????????????????????!");
        // ?????????????????????????????????
        List<LgtBidTemplate> lgtBidTemplates = iLgtBidTemplateService.list(new QueryWrapper<>(new LgtBidTemplate().setBidingId(lgtBiding.getBidingId()).setVendorVisibleFlag(YesOrNo.YES.getValue())));
        Assert.isTrue(CollectionUtils.isNotEmpty(lgtBidTemplates),"????????????????????????????????????!");
        // ??????????????????
        List<String> fieldCodes = lgtBidTemplates.stream().map(lgtBidTemplate -> StringUtil.toCamelCase(lgtBidTemplate.getFieldCode())).collect(Collectors.toList());
        List<String> fieldNames = lgtBidTemplates.stream().map(LgtBidTemplate::getFieldName).collect(Collectors.toList());
        // ???????????????
        Workbook workbook = ExcelUtil.createWorkbookModel(fieldNames);
        Sheet sheet = workbook.getSheetAt(0);
        AtomicInteger rowNum = new AtomicInteger(1);
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
        Map<String, String> ifBackMap = EasyExcelUtil.getDicCodeName("YES_OR_NO", baseClient);
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
        // ???????????????
        Map<String, String> importExportMethodMap = EasyExcelUtil.getDicCodeName("EXP/IMP", baseClient);
        // ??????
        Map<String, String> currencyCodeName = EasyExcelUtil.getCurrencyCodeName(baseClient);
        lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
            // ?????????
            Row row = sheet.createRow(rowNum.getAndAdd(1));
            AtomicInteger cellNum = new AtomicInteger(0);
            Class<LgtVendorQuotedLine> lgtVendorQuotedLineClass = LgtVendorQuotedLine.class;
            fieldCodes.forEach(fieldCode -> {
                if("logisticsCategoryCode".equals(fieldCode)){
                    fieldCode = "logisticsCategoryName";
                }
                // ???????????????
                Cell cell = row.createCell(cellNum.getAndAdd(1));
                try {
                    Object value = getFieldValue(lgtVendorQuotedLine, lgtVendorQuotedLineClass, fieldCode);
                    if(!ObjectUtils.isEmpty(value)){
                        String str = ExcelUtil.subZeroAndDot(value.toString());

                        // ????????????????????????
                        switch (fieldCode){
                            case "ifBack":
                                str = ifBackMap.get(str);
                                break;
                            case "expenseItem":
                                str = chargeNameMap.get(str);
                                break;
                            case "chargeMethod":
                                str = chargeLevelMap.get(str);
                                break;
                            case "chargeUnit":
                                str = subLevelMap.get(str);
                                break;
                            case "tradeTerm":
                                str = tradeTermMap.get(str);
                                break;
                            case "wholeArk":
                                str = fclLclMap.get(str);
                                break;
                            case "leg":
                                str = legMap.get(str);
                                break;
                            case "importExportMethod":
                                str = importExportMethodMap.get(str);
                                break;
                            case "currency":
                                str = currencyCodeName.get(str);
                                break;
                            default:
                                break;
                        }
                        cell.setCellValue(str);
                    }
                } catch (Exception e) {

                }
            });
        });
        OutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "???????????????");
        workbook.write(outputStream);
        workbook.close();
    }

    @Override
    public Object lgtBidShipPeriodImport(MultipartFile file, Long bidingId, Long vendorId, HttpServletResponse response) throws IOException {
        // ????????????
        EasyExcelUtil.checkExcelIsXlsx(file);
        LgtBiding biding = iLgtBidingService.getById(bidingId);
        // ????????????
        List<LgtBidShipPeriod> lgtBidShipPeriods = new ArrayList<>();
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        List<LgtBidShipPeriodImport> lgtBidShipPeriodImports ;
        List<LgtBidShipPeriodImportNew> lgtBidShipPeriodImportNews ;
        // ??????????????????/??????
        if(TransportModeEnum.RAILWAY_TRANSPORT.name().equals(biding.getTransportModeCode()) ||
                TransportModeEnum.LAND_TRANSPORT.name().equals(biding.getTransportModeCode())){
            // ??????excel??????
            lgtBidShipPeriodImportNews = EasyExcelUtil.readExcelWithModel(file, LgtBidShipPeriodImportNew.class);
            // ????????????
            checkImportDataNew(lgtBidShipPeriodImportNews,errorFlag,lgtBidShipPeriods,bidingId,vendorId);
            if(errorFlag.get()){
                return EasyExcelUtil.uploadErrorFile(fileCenterClient, lgtBidShipPeriodImportNews, LgtBidShipPeriodImportNew.class, "????????????????????????", file);
            }else {
                // ????????????
                saveImportShipPeriod(bidingId, vendorId, lgtBidShipPeriods);
            }
        }else {
            // ??????excel??????
            lgtBidShipPeriodImports = EasyExcelUtil.readExcelWithModel(file, LgtBidShipPeriodImport.class);
            // ????????????
            checkImportData(lgtBidShipPeriodImports,errorFlag,lgtBidShipPeriods,bidingId,vendorId);
            if(errorFlag.get()){
                return EasyExcelUtil.uploadErrorFile(fileCenterClient, lgtBidShipPeriodImports, LgtBidShipPeriodImport.class, "????????????????????????", file);
            }else {
                // ????????????
                saveImportShipPeriod(bidingId, vendorId, lgtBidShipPeriods);
            }
        }
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        return iLgtBidShipPeriodService.list(Wrappers.lambdaQuery(LgtBidShipPeriod.class).
                eq(LgtBidShipPeriod::getBidingId, bidingId).
                eq(LgtBidShipPeriod::getVendorId, vendorId).
                eq(LgtBidShipPeriod::getRound, lgtBiding.getCurrentRound()));
    }

    @Transactional
    public void saveImportShipPeriod(Long bidingId, Long vendorId, List<LgtBidShipPeriod> lgtBidShipPeriods) {
        if(CollectionUtils.isNotEmpty(lgtBidShipPeriods)){
            LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
            // ????????????????????????
            Assert.isTrue(lgtBiding.getEnrollEndDatetime().compareTo(new Date()) > 0,"?????????????????????,??????????????????!");
            LgtVendorQuotedHead vendorQuotedHead = iLgtVendorQuotedHeadService.getOne(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                    eq(LgtVendorQuotedHead::getBidingId, bidingId).
                    eq(LgtVendorQuotedHead::getVendorId, vendorId).
                    eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).
                    last("LIMIT 1"));
            lgtBidShipPeriods.forEach(lgtBidShipPeriod -> {
//                LgtBidShipPeriod periodServiceOne = iLgtBidShipPeriodService.getOne(Wrappers.lambdaQuery(LgtBidShipPeriod.class).
//                        eq(LgtBidShipPeriod::getQuotedHeadId, vendorQuotedHead.getQuotedHeadId()).
//                        eq(LgtBidShipPeriod::getBidRequirementLineId, lgtBidShipPeriod.getBidRequirementLineId()).last("LIMIT 1"));
                lgtBidShipPeriod
                        .setShipPeriodId(IdGenrator.generate())
                        .setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())
                        .setBidingId(bidingId)
                        .setVendorId(vendorQuotedHead.getVendorId())
                        .setVendorCode(vendorQuotedHead.getVendorCode())
                        .setVendorName(vendorQuotedHead.getVendorName())
                        .setRound(lgtBiding.getCurrentRound());
            });
            iLgtBidShipPeriodService.saveBatch(lgtBidShipPeriods);
        }
    }
    public void checkImportDataNew(List<LgtBidShipPeriodImportNew> lgtBidShipPeriodImportNews,AtomicBoolean errorFlag,List<LgtBidShipPeriod> lgtBidShipPeriods,Long bidingId, Long vendorId) {
        if(CollectionUtils.isNotEmpty(lgtBidShipPeriodImportNews)){
            // ??????
            Map<String, String> wholeArkNameCode = EasyExcelUtil.getDicNameCode("FCL /LCL", baseClient);
            // ????????????
            Map<String, Region> regionParentMap = new HashMap<>();
            Map<String, Region> regionMap = new HashMap<>();
            // ??????(??????-??????)
            Map<String, Port> portCodeMap = new HashMap<>();
            // ??????????????????
            List<String> regionNames = new ArrayList<>();
            List<String> portCodes = new ArrayList<>();
            lgtBidShipPeriodImportNews.forEach(lgtBidShipPeriodImport -> {
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromPort())){
                    portCodes.add(lgtBidShipPeriodImport.getFromPort().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToPort())){
                    portCodes.add(lgtBidShipPeriodImport.getToPort().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromCountry())){
                    regionNames.add(lgtBidShipPeriodImport.getFromCountry().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromProvince())){
                    regionNames.add(lgtBidShipPeriodImport.getFromProvince().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromCity())){
                    regionNames.add(lgtBidShipPeriodImport.getFromCity().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromCounty())){
                    regionNames.add(lgtBidShipPeriodImport.getFromCounty().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToCountry())){
                    regionNames.add(lgtBidShipPeriodImport.getToCountry().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToProvince())){
                    regionNames.add(lgtBidShipPeriodImport.getToProvince().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToCity())){
                    regionNames.add(lgtBidShipPeriodImport.getToCity().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToCounty())){
                    regionNames.add(lgtBidShipPeriodImport.getToCounty().trim());
                }
            });

            if (CollectionUtils.isNotEmpty(regionNames)) {
                List<Region> regions = iRegionService.list(Wrappers.lambdaQuery(Region.class).in(Region::getRegionName,regionNames));
                if(CollectionUtils.isNotEmpty(regions)){
                    regionParentMap = regions.stream().collect(Collectors.toMap(region -> region.getRegionName() +region.getParentRegionCode(),Function.identity(),(k1, k2)->k1));
                    regionMap = regions.stream().collect(Collectors.toMap(region -> String.valueOf(region.getRegionName()),Function.identity(),(k1,k2)->k1));
                }
            }

            if (CollectionUtils.isNotEmpty(portCodes)) {
                List<Port> portList = iPortService.list(Wrappers.lambdaQuery(Port.class).in(Port::getPortCode,portCodes));
                if(CollectionUtils.isNotEmpty(portList)){
                    portCodeMap = portList.stream().collect(Collectors.toMap(Port::getPortCode, Function.identity(), (k1, k2) -> k1));
                }
            }

            List<LgtBidRequirementLine> bidRequirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).eq(LgtBidRequirementLine::getBidingId, bidingId));
            Map<String, LgtBidRequirementLine> requirementLineMap = bidRequirementLines.stream().collect(Collectors.toMap(lgtBidRequirementLine -> lgtBidRequirementLine.getStartAddress() + lgtBidRequirementLine.getEndAddress(), Function.identity(),(k1, k2)->k1));
            HashSet<String> hashSet = new HashSet<>();
            for(LgtBidShipPeriodImportNew lgtBidShipPeriodImport:lgtBidShipPeriodImportNews){
                StringBuffer errorMsg = new StringBuffer();
                LgtBidShipPeriod lgtBidShipPeriod = new LgtBidShipPeriod();
                // ?????????
                String fromCountry = lgtBidShipPeriodImport.getFromCountry();
                if(StringUtil.notEmpty(fromCountry)){
                    fromCountry = fromCountry.trim();
                    Region region = regionMap.get(fromCountry);
                    if(null != region){
                        lgtBidShipPeriod.setFromCountry(fromCountry.trim());
                        lgtBidShipPeriod.setFromCountryCode(region.getRegionCode());
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ?????????
                String fromProvince = lgtBidShipPeriodImport.getFromProvince();
                if(StringUtil.notEmpty(fromProvince)){
                    fromProvince = fromProvince.trim();
                    Region region = null;
                    if (ObjectUtils.isEmpty(lgtBidShipPeriod.getFromCountryCode())) {
                        region = regionMap.get(fromProvince);
                    }else {
                        region = regionParentMap.get(fromProvince+lgtBidShipPeriod.getFromCountryCode());
                    }
                    if(null != region){
                        lgtBidShipPeriod.setFromProvince(region.getRegionName());
                        lgtBidShipPeriod.setFromProvinceCode(String.valueOf(region.getRegionCode()));
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ?????????
                String fromCity = lgtBidShipPeriodImport.getFromCity();
                if(StringUtil.notEmpty(fromCity)){
                    fromCity = fromCity.trim();
                    if (StringUtil.notEmpty(lgtBidShipPeriod.getFromProvinceCode())) {
                        Region region = regionParentMap.get(fromCity+lgtBidShipPeriod.getFromProvinceCode());
                        if(null != region){
                            lgtBidShipPeriod.setFromCity(region.getRegionName());
                            lgtBidShipPeriod.setFromCityCode(String.valueOf(region.getRegionCode()));
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("[?????????]???[?????????]?????????????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]???[?????????]?????????????????????; ");
                    }
                }
                // ????????????
                String fromCounty = lgtBidShipPeriodImport.getFromCounty();
                if(StringUtil.notEmpty(fromCounty)){
                    fromCounty = fromCounty.trim();
                    if (StringUtil.notEmpty(lgtBidShipPeriod.getFromCityCode())) {
                        Region region = regionParentMap.get(fromCounty+lgtBidShipPeriod.getFromCityCode());
                        if(null != region){
                            lgtBidShipPeriod.setFromCounty(region.getRegionName());
                            lgtBidShipPeriod.setFromCountyCode(String.valueOf(region.getRegionCode()));
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("[????????????]???[?????????]?????????????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[????????????]???[?????????]?????????????????????; ");
                    }
                }
                // ?????????
                String fromPlace = lgtBidShipPeriodImport.getFromPlace();
                if(StringUtil.notEmpty(fromPlace)){
                    fromPlace = fromPlace.trim();
                    lgtBidShipPeriod.setFromPlace(fromPlace);
                }
                // ?????????
                String toCountry = lgtBidShipPeriodImport.getToCountry();
                if(StringUtil.notEmpty(toCountry)){
                    toCountry = toCountry.trim();
                    Region region = regionMap.get(toCountry);
                    if (null != region) {
                        lgtBidShipPeriod.setToCountry(region.getRegionName());
                        lgtBidShipPeriod.setToCountryCode(region.getRegionCode());
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ?????????
                String toProvince = lgtBidShipPeriodImport.getToProvince();
                if(StringUtil.notEmpty(toProvince)){
                    toProvince = toProvince.trim();
                    Region region = null;
                    if (ObjectUtils.isEmpty(lgtBidShipPeriod.getToCountryCode())) {
                        region = regionMap.get(toProvince);
                    }else {
                        region = regionParentMap.get(toProvince+lgtBidShipPeriod.getToCountryCode());
                    }
                    if(null != region){
                        lgtBidShipPeriod.setToProvince(region.getRegionName());
                        lgtBidShipPeriod.setToProvinceCode(String.valueOf(region.getRegionCode()));
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ?????????
                String toCity = lgtBidShipPeriodImport.getToCity();
                if(StringUtil.notEmpty(toCity)){
                    toCity = toCity.trim();
                    if (StringUtil.notEmpty(lgtBidShipPeriod.getToProvinceCode())) {
                        Region region = regionParentMap.get(toCity+lgtBidShipPeriod.getToProvinceCode());
                        if(null != region){
                            lgtBidShipPeriod.setToCity(region.getRegionName());
                            lgtBidShipPeriod.setToCityCode(String.valueOf(region.getRegionCode()));
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("[?????????]???[?????????]???????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]???[?????????]???????????????; ");
                    }
                }
                // ????????????
                String toCounty = lgtBidShipPeriodImport.getToCounty();
                if(StringUtil.notEmpty(toCounty)){
                    toCounty = toCounty.trim();
                    if (StringUtil.notEmpty(lgtBidShipPeriod.getToCityCode())) {
                        Region region = regionParentMap.get(toCounty+lgtBidShipPeriod.getToCityCode());
                        if(null != region){
                            lgtBidShipPeriod.setToCounty(region.getRegionName());
                            lgtBidShipPeriod.setToCountyCode(String.valueOf(region.getRegionCode()));
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("[????????????]???[?????????]???????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[????????????]???[?????????]???????????????; ");
                    }
                }
                // ?????????
                String toPlace = lgtBidShipPeriodImport.getToPlace();
                if(StringUtil.notEmpty(toPlace)){
                    toPlace = toPlace.trim();
                    lgtBidShipPeriod.setToPlace(toPlace);
                }
                // ???????????????
                String fromPort = lgtBidShipPeriodImport.getFromPort();
                if(StringUtil.notEmpty(fromPort)){
                    fromPort = fromPort.trim();
                    Port port = portCodeMap.get(fromPort);
                    if (null != port) {
                        lgtBidShipPeriod.setFromPort(port.getPortNameZhs());
                        lgtBidShipPeriod.setFromPortCode(port.getPortCode());
                        lgtBidShipPeriod.setFromPortId(port.getPortId());
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[???????????????]????????????; ");
                    }
                }
                // ???????????????
                String toPort = lgtBidShipPeriodImport.getToPort();
                if(StringUtil.notEmpty(toPort)){
                    toPort = toPort.trim();
                    Port port = portCodeMap.get(toPort);
                    if (null != port) {
                        lgtBidShipPeriod.setToPort(port.getPortNameZhs());
                        lgtBidShipPeriod.setToPortCode(port.getPortCode());
                        lgtBidShipPeriod.setToPortId(port.getPortId());
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[???????????????]????????????; ");
                    }
                }

                // ??????
                String charNum = lgtBidShipPeriodImport.getCharNum();
                if(StringUtil.notEmpty(charNum)){
                    charNum = charNum.trim();
                    try {
                        BigDecimal bigDecimal = new BigDecimal(charNum);
                        lgtBidShipPeriod.setCharNum(bigDecimal);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[??????]????????????; ");
                    }
                }
                // ?????????
                String megawatt = lgtBidShipPeriodImport.getMegawatt();
                if(StringUtil.notEmpty(megawatt)){
                    megawatt = megawatt.trim();
                    try {
                        BigDecimal bigDecimal = new BigDecimal(megawatt);
                        lgtBidShipPeriod.setMegawatt(bigDecimal);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ????????????(Y/N)
                String ifSatisfied = lgtBidShipPeriodImport.getIfSatisfied();
                if(StringUtil.notEmpty(ifSatisfied)){
                    ifSatisfied = ifSatisfied.trim();
                    if("Y".equals(ifSatisfied) || "N".equals(ifSatisfied)){
                        lgtBidShipPeriod.setIfSatisfied(ifSatisfied);
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[????????????]?????????[Y]???[N]; ");
                    }
                }
                // ????????????????????????????????????????????????
                String satisfiableSituation = lgtBidShipPeriodImport.getSatisfiableSituation();
                if(StringUtil.notEmpty(satisfiableSituation)){
                    satisfiableSituation = satisfiableSituation.trim();
                    lgtBidShipPeriod.setSatisfiableSituation(satisfiableSituation);
                }else {
                    if("N".equals(lgtBidShipPeriod.getIfSatisfied())){
                        errorFlag.set(true);
                        errorMsg.append("[???????????????]????????????; ");
                    }
                }
                // ????????????
                String specialInstructions = lgtBidShipPeriodImport.getSpecialInstructions();
                if(StringUtil.notEmpty(specialInstructions)){
                    specialInstructions = specialInstructions.trim();
                    lgtBidShipPeriod.setSpecialInstructions(specialInstructions);
                }
                // ??????
                String remarks = lgtBidShipPeriodImport.getRemarks();
                if(StringUtil.notEmpty(remarks)){
                    remarks = remarks.trim();
                    lgtBidShipPeriod.setRemarks(remarks);
                }

                if (errorMsg.length() <= 0) {
                    LgtBidingServiceImpl.setStartEndAddress(lgtBidShipPeriod);
                    String key = lgtBidShipPeriod.getStartAddress() + lgtBidShipPeriod.getEndAddress();
                    LgtBidRequirementLine requirementLine = requirementLineMap.get(key);
                    if(null == requirementLine){
                        errorFlag.set(true);
                        errorMsg.append("????????????,????????????????????????; ");
                    }else {
                        lgtBidShipPeriod.setRowNum(requirementLine.getRowNum());
                        lgtBidShipPeriod.setBidRequirementLineId(requirementLine.getBidRequirementLineId());
                    }
                    if (!hashSet.add(key)) {
                        errorFlag.set(true);
                        errorMsg.append("????????????,????????????; ");
                    }
                }

                if(errorMsg.length() > 0){
                    lgtBidShipPeriodImport.setErrorMsg(errorMsg.toString());
                }else {
                    lgtBidShipPeriodImport.setErrorMsg(null);
                }

                lgtBidShipPeriods.add(lgtBidShipPeriod);

            }
        }
    }


    public void checkImportData(List<LgtBidShipPeriodImport> lgtBidShipPeriodImports,AtomicBoolean errorFlag,List<LgtBidShipPeriod> lgtBidShipPeriods,Long bidingId, Long vendorId) {
        if(CollectionUtils.isNotEmpty(lgtBidShipPeriodImports)){
            // ??????
            Map<String, String> wholeArkNameCode = EasyExcelUtil.getDicNameCode("FCL /LCL", baseClient);
            // ????????????
            Map<String, Region> regionParentMap = new HashMap<>();
            Map<String, Region> regionMap = new HashMap<>();
            // ??????(??????-??????)
            Map<String, Port> portCodeMap = new HashMap<>();
            // ??????????????????
            List<String> regionNames = new ArrayList<>();
            List<String> portCodes = new ArrayList<>();
            lgtBidShipPeriodImports.forEach(lgtBidShipPeriodImport -> {
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromPort())){
                    portCodes.add(lgtBidShipPeriodImport.getFromPort().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToPort())){
                    portCodes.add(lgtBidShipPeriodImport.getToPort().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromCountry())){
                    regionNames.add(lgtBidShipPeriodImport.getFromCountry().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromProvince())){
                    regionNames.add(lgtBidShipPeriodImport.getFromProvince().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromCity())){
                    regionNames.add(lgtBidShipPeriodImport.getFromCity().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getFromCounty())){
                    regionNames.add(lgtBidShipPeriodImport.getFromCounty().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToCountry())){
                    regionNames.add(lgtBidShipPeriodImport.getToCountry().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToProvince())){
                    regionNames.add(lgtBidShipPeriodImport.getToProvince().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToCity())){
                    regionNames.add(lgtBidShipPeriodImport.getToCity().trim());
                }
                if(!ObjectUtils.isEmpty(lgtBidShipPeriodImport.getToCounty())){
                    regionNames.add(lgtBidShipPeriodImport.getToCounty().trim());
                }
            });

            if (CollectionUtils.isNotEmpty(regionNames)) {
                List<Region> regions = iRegionService.list(Wrappers.lambdaQuery(Region.class).in(Region::getRegionName,regionNames));
                if(CollectionUtils.isNotEmpty(regions)){
                    regionParentMap = regions.stream().collect(Collectors.toMap(region -> region.getRegionName() +region.getParentRegionCode(),Function.identity(),(k1, k2)->k1));
                    regionMap = regions.stream().collect(Collectors.toMap(region -> String.valueOf(region.getRegionName()),Function.identity(),(k1,k2)->k1));
                }
            }

            if (CollectionUtils.isNotEmpty(portCodes)) {
                List<Port> portList = iPortService.list(Wrappers.lambdaQuery(Port.class).in(Port::getPortCode,portCodes));
                if(CollectionUtils.isNotEmpty(portList)){
                    portCodeMap = portList.stream().collect(Collectors.toMap(Port::getPortCode, Function.identity(), (k1, k2) -> k1));
                }
            }

            List<LgtBidRequirementLine> bidRequirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).eq(LgtBidRequirementLine::getBidingId, bidingId));
            Map<String, LgtBidRequirementLine> requirementLineMap = bidRequirementLines.stream().collect(Collectors.toMap(lgtBidRequirementLine -> lgtBidRequirementLine.getStartAddress() + lgtBidRequirementLine.getEndAddress(), Function.identity(),(k1, k2)->k1));
            HashSet<String> hashSet = new HashSet<>();
            for(LgtBidShipPeriodImport lgtBidShipPeriodImport:lgtBidShipPeriodImports){
                StringBuffer errorMsg = new StringBuffer();
                LgtBidShipPeriod lgtBidShipPeriod = new LgtBidShipPeriod();
                // ?????????
                String fromCountry = lgtBidShipPeriodImport.getFromCountry();
                if(StringUtil.notEmpty(fromCountry)){
                    fromCountry = fromCountry.trim();
                    Region region = regionMap.get(fromCountry);
                    if(null != region){
                        lgtBidShipPeriod.setFromCountry(fromCountry.trim());
                        lgtBidShipPeriod.setFromCountryCode(region.getRegionCode());
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ?????????
                String fromProvince = lgtBidShipPeriodImport.getFromProvince();
                if(StringUtil.notEmpty(fromProvince)){
                    fromProvince = fromProvince.trim();
                    Region region = null;
                    if (ObjectUtils.isEmpty(lgtBidShipPeriod.getFromCountryCode())) {
                        region = regionMap.get(fromProvince);
                    }else {
                        region = regionParentMap.get(fromProvince+lgtBidShipPeriod.getFromCountryCode());
                    }
                    if(null != region){
                        lgtBidShipPeriod.setFromProvince(region.getRegionName());
                        lgtBidShipPeriod.setFromProvinceCode(String.valueOf(region.getRegionCode()));
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ?????????
                String fromCity = lgtBidShipPeriodImport.getFromCity();
                if(StringUtil.notEmpty(fromCity)){
                    fromCity = fromCity.trim();
                    if (StringUtil.notEmpty(lgtBidShipPeriod.getFromProvinceCode())) {
                        Region region = regionParentMap.get(fromCity+lgtBidShipPeriod.getFromProvinceCode());
                        if(null != region){
                            lgtBidShipPeriod.setFromCity(region.getRegionName());
                            lgtBidShipPeriod.setFromCityCode(String.valueOf(region.getRegionCode()));
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("[?????????]???[?????????]?????????????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]???[?????????]?????????????????????; ");
                    }
                }
                // ????????????
                String fromCounty = lgtBidShipPeriodImport.getFromCounty();
                if(StringUtil.notEmpty(fromCounty)){
                    fromCounty = fromCounty.trim();
                    if (StringUtil.notEmpty(lgtBidShipPeriod.getFromCityCode())) {
                        Region region = regionParentMap.get(fromCounty+lgtBidShipPeriod.getFromCityCode());
                        if(null != region){
                            lgtBidShipPeriod.setFromCounty(region.getRegionName());
                            lgtBidShipPeriod.setFromCountyCode(String.valueOf(region.getRegionCode()));
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("[????????????]???[?????????]?????????????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[????????????]???[?????????]?????????????????????; ");
                    }
                }
                // ?????????
                String fromPlace = lgtBidShipPeriodImport.getFromPlace();
                if(StringUtil.notEmpty(fromPlace)){
                    fromPlace = fromPlace.trim();
                    lgtBidShipPeriod.setFromPlace(fromPlace);
                }
                // ?????????
                String toCountry = lgtBidShipPeriodImport.getToCountry();
                if(StringUtil.notEmpty(toCountry)){
                    toCountry = toCountry.trim();
                    Region region = regionMap.get(toCountry);
                    if (null != region) {
                        lgtBidShipPeriod.setToCountry(region.getRegionName());
                        lgtBidShipPeriod.setToCountryCode(region.getRegionCode());
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ?????????
                String toProvince = lgtBidShipPeriodImport.getToProvince();
                if(StringUtil.notEmpty(toProvince)){
                    toProvince = toProvince.trim();
                    Region region = null;
                    if (ObjectUtils.isEmpty(lgtBidShipPeriod.getToCountryCode())) {
                        region = regionMap.get(toProvince);
                    }else {
                        region = regionParentMap.get(toProvince+lgtBidShipPeriod.getToCountryCode());
                    }
                    if(null != region){
                        lgtBidShipPeriod.setToProvince(region.getRegionName());
                        lgtBidShipPeriod.setToProvinceCode(String.valueOf(region.getRegionCode()));
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]????????????; ");
                    }
                }
                // ?????????
                String toCity = lgtBidShipPeriodImport.getToCity();
                if(StringUtil.notEmpty(toCity)){
                    toCity = toCity.trim();
                    if (StringUtil.notEmpty(lgtBidShipPeriod.getToProvinceCode())) {
                        Region region = regionParentMap.get(toCity+lgtBidShipPeriod.getToProvinceCode());
                        if(null != region){
                            lgtBidShipPeriod.setToCity(region.getRegionName());
                            lgtBidShipPeriod.setToCityCode(String.valueOf(region.getRegionCode()));
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("[?????????]???[?????????]???????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[?????????]???[?????????]???????????????; ");
                    }
                }
                // ????????????
                String toCounty = lgtBidShipPeriodImport.getToCounty();
                if(StringUtil.notEmpty(toCounty)){
                    toCounty = toCounty.trim();
                    if (StringUtil.notEmpty(lgtBidShipPeriod.getToCityCode())) {
                        Region region = regionParentMap.get(toCounty+lgtBidShipPeriod.getToCityCode());
                        if(null != region){
                            lgtBidShipPeriod.setToCounty(region.getRegionName());
                            lgtBidShipPeriod.setToCountyCode(String.valueOf(region.getRegionCode()));
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("[????????????]???[?????????]???????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[????????????]???[?????????]???????????????; ");
                    }
                }
                // ?????????
                String toPlace = lgtBidShipPeriodImport.getToPlace();
                if(StringUtil.notEmpty(toPlace)){
                    toPlace = toPlace.trim();
                    lgtBidShipPeriod.setToPlace(toPlace);
                }
                // ???????????????
                String fromPort = lgtBidShipPeriodImport.getFromPort();
                if(StringUtil.notEmpty(fromPort)){
                    fromPort = fromPort.trim();
                    Port port = portCodeMap.get(fromPort);
                    if (null != port) {
                        lgtBidShipPeriod.setFromPort(port.getPortNameZhs());
                        lgtBidShipPeriod.setFromPortCode(port.getPortCode());
                        lgtBidShipPeriod.setFromPortId(port.getPortId());
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[???????????????]????????????; ");
                    }
                }
                // ???????????????
                String toPort = lgtBidShipPeriodImport.getToPort();
                if(StringUtil.notEmpty(toPort)){
                    toPort = toPort.trim();
                    Port port = portCodeMap.get(toPort);
                    if (null != port) {
                        lgtBidShipPeriod.setToPort(port.getPortNameZhs());
                        lgtBidShipPeriod.setToPortCode(port.getPortCode());
                        lgtBidShipPeriod.setToPortId(port.getPortId());
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[???????????????]????????????; ");
                    }
                }
                // FCL/LCL
                String wholeArk = lgtBidShipPeriodImport.getWholeArk();
                if(StringUtil.notEmpty(wholeArk)){
                    wholeArk = wholeArk.trim();
                    String code = wholeArkNameCode.get(wholeArk);
                    if(StringUtil.notEmpty(code)){
                        lgtBidShipPeriod.setWholeArk(code);
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("[FCL/LCL]??????????????????; ");
                    }
                }
                // Mon
                String mon = lgtBidShipPeriodImport.getMon();
                if(StringUtil.notEmpty(mon)){
                    try {
                        BigDecimal monNum = new BigDecimal(mon);
                        lgtBidShipPeriod.setMon(monNum);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[Mon]?????????; ");
                    }
                }
                // Tue
                String tue = lgtBidShipPeriodImport.getTue();
                if(StringUtil.notEmpty(tue)){
                    try {
                        BigDecimal tueNum = new BigDecimal(tue);
                        lgtBidShipPeriod.setTue(tueNum);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[Tue]?????????; ");
                    }
                }
                // Wed
                String wed = lgtBidShipPeriodImport.getWed();
                if(StringUtil.notEmpty(wed)){
                    try {
                        BigDecimal wedNum = new BigDecimal(wed);
                        lgtBidShipPeriod.setWed(wedNum);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[Wed]?????????; ");
                    }
                }
                // Thu
                String thu = lgtBidShipPeriodImport.getThu();
                if(StringUtil.notEmpty(thu)){
                    try {
                        BigDecimal thuNum = new BigDecimal(thu);
                        lgtBidShipPeriod.setThu(thuNum);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[Thu]?????????; ");
                    }
                }
                // Fri
                String fri = lgtBidShipPeriodImport.getFri();
                if(StringUtil.notEmpty(fri)){
                    try {
                        BigDecimal friNum = new BigDecimal(fri);
                        lgtBidShipPeriod.setFri(friNum);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[Fri]?????????; ");
                    }
                }
                // Sat
                String sat = lgtBidShipPeriodImport.getSat();
                if(StringUtil.notEmpty(sat)){
                    try {
                        BigDecimal satNum = new BigDecimal(sat);
                        lgtBidShipPeriod.setSat(satNum);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[Sat]?????????; ");
                    }
                }
                // Sun
                String sun = lgtBidShipPeriodImport.getSun();
                if(StringUtil.notEmpty(sun)){
                    try {
                        BigDecimal sunNum = new BigDecimal(sun);
                        lgtBidShipPeriod.setSun(sunNum);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[Sun]?????????; ");
                    }
                }
                // Transit Time_PTP???Days???
                String transitTime = lgtBidShipPeriodImport.getTransitTime();
                if(StringUtil.notEmpty(transitTime)){
                    try {
                        BigDecimal transitTimeNum = new BigDecimal(transitTime);
                        lgtBidShipPeriod.setTransitTime(transitTimeNum);
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("[Transit Time_PTP???Days???]?????????; ");
                    }
                }
                // ?????????/????????????
                String shipCompanyName = lgtBidShipPeriodImport.getShipCompanyName();
                if(StringUtil.notEmpty(shipCompanyName)){
                    shipCompanyName = shipCompanyName.trim();
                    lgtBidShipPeriod.setShipCompanyName(shipCompanyName);
                }
                // ?????????/?????????
                String transferPort = lgtBidShipPeriodImport.getTransferPort();
                if(StringUtil.notEmpty(transferPort)){
                    transferPort = transferPort.trim();
                    lgtBidShipPeriod.setTransferPort(transferPort);
                }

                if (errorMsg.length() <= 0) {
                    LgtBidingServiceImpl.setStartEndAddress(lgtBidShipPeriod);
                    String key = lgtBidShipPeriod.getStartAddress() + lgtBidShipPeriod.getEndAddress();
                    LgtBidRequirementLine requirementLine = requirementLineMap.get(key);
                    if(null == requirementLine){
                        errorFlag.set(true);
                        errorMsg.append("????????????,????????????????????????; ");
                    }else {
                        lgtBidShipPeriod.setRowNum(requirementLine.getRowNum());
                        lgtBidShipPeriod.setBidRequirementLineId(requirementLine.getBidRequirementLineId());
                    }
                    if (!hashSet.add(key)) {
                        errorFlag.set(true);
                        errorMsg.append("????????????,????????????; ");
                    }
                }

                if(errorMsg.length() > 0){
                    lgtBidShipPeriodImport.setErrorMsg(errorMsg.toString());
                }else {
                    lgtBidShipPeriodImport.setErrorMsg(null);
                }

                lgtBidShipPeriods.add(lgtBidShipPeriod);

            }
        }
    }

    @Override
    public void lgtBidShipPeriodImportModelDownload(Long bidingId,HttpServletResponse response) throws IOException {
        Assert.notNull(bidingId,"????????????:bidingId");
        /**
         * ??????????????????/??????
         */
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        if(TransportModeEnum.LAND_TRANSPORT.name().equals(lgtBiding.getTransportModeCode()) ||
                TransportModeEnum.RAILWAY_TRANSPORT.name().equals(lgtBiding.getTransportModeCode())){
            List<LgtBidShipPeriodImportNew> lgtBidShipPeriodImportNews = new ArrayList<>();
            EasyExcelUtil.writeExcelWithModel(response,"????????????????????????",lgtBidShipPeriodImportNews,LgtBidShipPeriodImportNew.class);
        }else{
            List<LgtBidShipPeriodImport> lgtBidShipPeriodImports = new ArrayList<>();
            EasyExcelUtil.writeExcelWithModel(response,"????????????????????????",lgtBidShipPeriodImports,LgtBidShipPeriodImport.class);
        }
    }

    @Override
    public void lgtBidShipPeriodExport(Long bidingId, Long vendorId, HttpServletResponse response) throws IOException {
        Assert.notNull(bidingId,"????????????: bidingId");
        Assert.notNull(vendorId,"????????????: vendorId");
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        List<LgtBidShipPeriodExport> periodExports = new ArrayList<>();
        List<LgtBidShipPeriodExportNew> lgtBidShipPeriodExportNews = new ArrayList<>();
        // ??????
        Map<String, String> dicCodeName = EasyExcelUtil.getDicCodeName("FCL /LCL", baseClient);
        // ????????????
        List<LgtBidShipPeriod> shipPeriods = iLgtBidShipPeriodService.list(Wrappers.lambdaQuery(LgtBidShipPeriod.class).
                eq(LgtBidShipPeriod::getBidingId, bidingId).
                eq(LgtBidShipPeriod::getVendorId, vendorId).
                eq(LgtBidShipPeriod::getRound, lgtBiding.getCurrentRound()));
        if(CollectionUtils.isNotEmpty(shipPeriods)){
            shipPeriods.forEach(lgtBidShipPeriod -> {
                if(TransportModeEnum.LAND_TRANSPORT.name().equals(lgtBiding.getTransportModeCode()) ||
                        TransportModeEnum.RAILWAY_TRANSPORT.name().equals(lgtBiding.getTransportModeCode())){
                    // ??????/??????
                    LgtBidShipPeriodExportNew lgtBidShipPeriodExportNew = new LgtBidShipPeriodExportNew();
                    BeanCopyUtil.copyProperties(lgtBidShipPeriodExportNew,lgtBidShipPeriod);
                    lgtBidShipPeriodExportNews.add(lgtBidShipPeriodExportNew);
                }else {
                    LgtBidShipPeriodExport periodExport = new LgtBidShipPeriodExport();
                    BeanCopyUtil.copyProperties(periodExport,lgtBidShipPeriod);
                    periodExport.setWholeArk(!ObjectUtils.isEmpty(periodExport.getWholeArk())?dicCodeName.get(periodExport.getWholeArk()):null);
                    periodExports.add(periodExport);
                }
            });
        }
        // ??????
        if(TransportModeEnum.LAND_TRANSPORT.name().equals(lgtBiding.getTransportModeCode()) ||
                TransportModeEnum.RAILWAY_TRANSPORT.name().equals(lgtBiding.getTransportModeCode())){
            EasyExcelUtil.writeExcelWithModel(response,"????????????",lgtBidShipPeriodExportNews,LgtBidShipPeriodExportNew.class);
        }else {
            EasyExcelUtil.writeExcelWithModel(response,"????????????",periodExports,LgtBidShipPeriodExport.class);
        }
    }

    @Override
    public Object quotedLineImport(MultipartFile file, String param,Long bidingId,Long vendorId, HttpServletResponse response) throws Exception {
        Assert.notNull(param,"????????????????????????!");
        Assert.notNull(bidingId,"????????????: bidingId");
        Assert.isTrue(param.contains("onlyAddress"),"[??????]????????????????????????!");
        Assert.notNull(vendorId,"????????????: vendorId");
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        // ?????????????????????????????????
        List<String> vendorOperatingField = getVendorOperatingField(bidingId);
        vendorOperatingField.add("onlyAddress");
        Assert.isTrue(CollectionUtils.isNotEmpty(vendorOperatingField),"??????????????????????????????????????????,??????????????????!");
        Assert.isTrue(lgtBiding.getEnrollEndDatetime().compareTo(new Date()) > 0,"?????????????????????,??????????????????!");
        // ????????????????????????.xlsx
        EasyExcelUtil.checkExcelIsXlsx(file);
        LgtVendorQuotedHead vendorQuotedHead = iLgtVendorQuotedHeadService.getOne(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, bidingId).
                eq(LgtVendorQuotedHead::getVendorId, vendorId).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).last("LIMIT 1"));
        Assert.isTrue(!BiddingOrderStates.SUBMISSION.getValue().equals(vendorQuotedHead.getStatus()),"??????????????????????????????,??????????????????!");
        Map<String,String> fieldCodeName = JSON.parse(param, Map.class);
        Assert.notNull(fieldCodeName.get("onlyAddress"),"??????????????????Key??????????????????????????????!");

        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ??????????????????
        List<String> errorMsgs = new ArrayList<>();
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = new ArrayList<>();
        // ????????????
        Workbook workbook = readExcelData(file, fieldCodeName, errorFlag, bidingId, errorMsgs, lgtVendorQuotedLines,vendorOperatingField);
        if(errorFlag.get()){
            // ??????????????????
            return ExcelUtil.getUploadErrorFile(file,errorMsgs,workbook,fileCenterClient);
        }else {
            /**
             * ??????key : ??????ID+ LEG +?????? ???+????????????+????????????
             */
            saveImportData(bidingId, lgtBiding, vendorOperatingField, vendorQuotedHead, lgtVendorQuotedLines);
        }

        List<LgtVendorQuotedLine> vendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                eq(LgtVendorQuotedLine::getQuotedHeadId, vendorQuotedHead.getQuotedHeadId()));
        return vendorQuotedLines;
    }

    /**
     * ???????????????????????????
     * @param bidingId
     * @param lgtBiding
     * @param vendorOperatingField
     * @param vendorQuotedHead
     * @param lgtVendorQuotedLines
     */
    @Transactional
    public void saveImportData(Long bidingId, LgtBiding lgtBiding, List<String> vendorOperatingField, LgtVendorQuotedHead vendorQuotedHead, List<LgtVendorQuotedLine> lgtVendorQuotedLines) {
        // ???????????????????????????
        initVendorQuotedLine(vendorQuotedHead,lgtBiding);
        List<String> list = Arrays.asList("chargeUnit", "chargeMethod", "expenseItem", "leg");
        List<String> tempList = new ArrayList<>();
        vendorOperatingField.forEach(s -> {
            if(list.contains(s)){
                tempList.add(s+"Name");
            }
            if(allFieldList.contains(s)){
                tempList.add(s+"Code");
            }
        });

        vendorOperatingField.addAll(tempList);

        if (CollectionUtils.isNotEmpty(lgtVendorQuotedLines)) {
            List<LgtVendorQuotedLine> vendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                    eq(LgtVendorQuotedLine::getQuotedHeadId, vendorQuotedHead.getQuotedHeadId()).
                    eq(LgtVendorQuotedLine::getRound, lgtBiding.getCurrentRound()));
            // ??????
            Map<String, LgtVendorQuotedLine> quotedLineMap = new HashMap<>();
            if(CollectionUtils.isNotEmpty(vendorQuotedLines)){
                quotedLineMap = vendorQuotedLines.stream().collect(Collectors.
                        toMap(lgtVendorQuotedLine ->
                                        lgtVendorQuotedLine.getBidRequirementLineId() +
                                                lgtVendorQuotedLine.getChargeMethod()+
                                                lgtVendorQuotedLine.getChargeUnit()+
                                                lgtVendorQuotedLine.getLeg()+
                                                lgtVendorQuotedLine.getExpenseItem(),
                                Function.identity(),(k1,k2)->k1));
            }
            List<LgtVendorQuotedLine> lgtVendorQuotedLinesSum = new ArrayList<>();
            for(LgtVendorQuotedLine lgtVendorQuotedLine : lgtVendorQuotedLines){
                LgtVendorQuotedLine vendorQuotedLine;
                String key = lgtVendorQuotedLine.getBidRequirementLineId() +
                        lgtVendorQuotedLine.getChargeMethod()+
                        lgtVendorQuotedLine.getChargeUnit()+
                        lgtVendorQuotedLine.getLeg()+
                        lgtVendorQuotedLine.getExpenseItem();
                vendorQuotedLine = quotedLineMap.get(key);

                if(null == vendorQuotedLine){
                    vendorQuotedLine = iLgtVendorQuotedLineService.getOne(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                            eq(LgtVendorQuotedLine::getQuotedHeadId, vendorQuotedHead.getQuotedHeadId()).
                            eq(LgtVendorQuotedLine::getRound, lgtBiding.getCurrentRound()).
                            eq(LgtVendorQuotedLine::getBidRequirementLineId,lgtVendorQuotedLine.getBidRequirementLineId()).
                            and(wrapper-> wrapper.eq(LgtVendorQuotedLine::getChargeMethod,"").or().isNull(LgtVendorQuotedLine::getChargeMethod)).
                            and(wrapper-> wrapper.eq(LgtVendorQuotedLine::getChargeUnit,"").or().isNull(LgtVendorQuotedLine::getChargeUnit)).
                            and(wrapper-> wrapper.eq(LgtVendorQuotedLine::getLeg,"").or().isNull(LgtVendorQuotedLine::getLeg)).
                            and(wrapper-> wrapper.eq(LgtVendorQuotedLine::getExpenseItem,"").or().isNull(LgtVendorQuotedLine::getExpenseItem)).
                            last(" LIMIT 1"));
                }

                Class<? extends LgtVendorQuotedLine> aClass = LgtVendorQuotedLine.class;
                if(null != vendorQuotedLine){
                    // ??????
                    LgtVendorQuotedLine finalVendorQuotedLine = vendorQuotedLine;
                    vendorOperatingField.forEach(field->{
                        try {
                            Field declaredField = aClass.getDeclaredField(field);
                            declaredField.setAccessible(true);
                            Object value = declaredField.get(lgtVendorQuotedLine);
                            declaredField.set(finalVendorQuotedLine,value);
                        } catch (Exception e) {
                            log.error("??????????????????????????????????????????????????????:"+e);
                            throw new BaseException("??????????????????????????????????????????????????????:"+e);
                        }
                    });
                    iLgtVendorQuotedLineService.updateById(finalVendorQuotedLine);
                    lgtVendorQuotedLinesSum.add(finalVendorQuotedLine);
                }else {
                    // ??????
                    LgtBidRequirementLine lgtBidRequirementLine = iLgtBidRequirementLineService.getById(lgtVendorQuotedLine.getBidRequirementLineId());
                    LgtVendorQuotedLine quotedLine = new LgtVendorQuotedLine();
                    BeanCopyUtil.copyProperties(quotedLine,lgtBidRequirementLine);
                    quotedLine.setQuotedLineId(IdGenrator.generate());
                    quotedLine.setQuotedHeadId(vendorQuotedHead.getQuotedHeadId());
                    quotedLine.setBidingId(bidingId);
                    quotedLine.setRound(lgtBiding.getCurrentRound());
                    quotedLine.setPurchaseRemark(lgtBidRequirementLine.getComments());
                    quotedLine.setVendorId(vendorQuotedHead.getVendorId());
                    quotedLine.setVendorCode(vendorQuotedHead.getVendorCode());
                    quotedLine.setVendorName(vendorQuotedHead.getVendorName());
                    quotedLine.setIfCopy(YesOrNo.YES.getValue());
                    vendorOperatingField.forEach(field->{
                        // ??????????????????
                        try {
                            Field declaredField = aClass.getDeclaredField(field);
                            declaredField.setAccessible(true);
                            declaredField.set(quotedLine,null);
                            Object value = declaredField.get(lgtVendorQuotedLine);
                            declaredField.set(quotedLine,value);
                        } catch (Exception e) {
                            log.error("??????????????????????????????????????????????????????:"+e);
                            throw new BaseException("??????????????????????????????????????????????????????:"+e);
                        }
                    });
                    LgtBidingServiceImpl.setStartEndAddress(quotedLine);
                    iLgtVendorQuotedLineService.save(quotedLine);
                    lgtVendorQuotedLinesSum.add(quotedLine);
                }
            }
            if(CollectionUtils.isNotEmpty(lgtVendorQuotedLinesSum)){
                // ??????????????????????????????
                calculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLinesSum);
            }
        }
    }

    // ???????????????????????????
    public void initVendorQuotedLine(LgtVendorQuotedHead lgtVendorQuotedHead,LgtBiding lgtBiding){
        // ??????????????????????????????
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(new QueryWrapper<>(new LgtVendorQuotedLine().
                setQuotedHeadId(lgtVendorQuotedHead.getQuotedHeadId())));
        if(CollectionUtils.isEmpty(lgtVendorQuotedLines)){
            // ???????????????????????????
            List<LgtBidRequirementLine> lgtBidRequirementLines = iLgtBidRequirementLineService.list(new QueryWrapper<>(new LgtBidRequirementLine().
                    setBidingId(lgtBiding.getBidingId())));
            if(CollectionUtils.isNotEmpty(lgtBidRequirementLines)){
                List<LgtVendorQuotedLine> vendorQuotedLines = new ArrayList<>();
                lgtBidRequirementLines.forEach(lgtBidRequirementLine -> {
                    LgtVendorQuotedLine lgtVendorQuotedLine = new LgtVendorQuotedLine();
                    BeanCopyUtil.copyProperties(lgtVendorQuotedLine,lgtBidRequirementLine);
                    lgtVendorQuotedLine.setQuotedLineId(IdGenrator.generate());
                    lgtVendorQuotedLine.setQuotedHeadId(lgtVendorQuotedHead.getQuotedHeadId());
                    lgtVendorQuotedLine.setBidingId(lgtBiding.getBidingId());
                    lgtVendorQuotedLine.setRound(lgtBiding.getCurrentRound());
                    lgtVendorQuotedLine.setPurchaseRemark(lgtBidRequirementLine.getComments());
                    lgtVendorQuotedLine.setVendorId(lgtVendorQuotedHead.getVendorId());
                    lgtVendorQuotedLine.setVendorCode(lgtVendorQuotedHead.getVendorCode());
                    lgtVendorQuotedLine.setVendorName(lgtVendorQuotedHead.getVendorName());
                    vendorQuotedLines.add(lgtVendorQuotedLine);
                });
                iLgtVendorQuotedLineService.saveBatch(vendorQuotedLines);
            }
        }
    }

    // ?????????????????????????????????
    public List<String> getVendorOperatingField(Long bidingId) {
        List<String> operatingFields = new ArrayList<>();
        // ???????????????????????????
        List<LgtBidTemplate> lgtBidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).
                eq(LgtBidTemplate::getBidingId, bidingId).
                eq(LgtBidTemplate::getVendorOperateFlag, YesOrNo.YES.getValue()));
        if(CollectionUtils.isNotEmpty(lgtBidTemplates)){
            operatingFields = lgtBidTemplates.stream().map(lgtBidTemplate -> StringUtil.toCamelCase(lgtBidTemplate.getFieldCode())).collect(Collectors.toList());
        }
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        /**
         * ???????????????????????????: ???????????????????????????
         */
        if(CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
            if(!operatingFields.contains("chargeUnit")){
                operatingFields.add("chargeUnit");
            }
            if(!operatingFields.contains("chargeMethod")){
                operatingFields.add("chargeMethod");
            }
        }
        return operatingFields;
    }
    // ??????????????????
    private final static List<String> startAddList = Arrays.asList("fromCountry", "fromProvince", "fromCity", "fromCounty");
    // ??????????????????
    private final static List<String> endAddList = Arrays.asList("toCountry", "toProvince", "toCity", "toCounty");
    // ????????????
    private final static List<String> placeFieldList = Arrays.asList("fromCountry", "fromProvince", "fromCity", "fromCounty","toCountry", "toProvince", "toCity", "toCounty");
    // ????????????
    private final static List<String> portFieldList = Arrays.asList("fromPort","toPort");
    // ??????????????????
    private final static List<String> allFieldList = Arrays.asList("fromCountry", "fromProvince", "fromCity", "fromCounty","toCountry", "toProvince", "toCity", "toCounty","fromPort","toPort");


    public Workbook readExcelData(MultipartFile file, Map<String,String> fieldCodeName,AtomicBoolean errorFlag,
                                  Long bidingId,List<String> errorMsgs,List<LgtVendorQuotedLine> lgtVendorQuotedLines,List<String> vendorOperatingField) throws Exception {
        log.info("--------------------------------------????????????????????????????????????---------------------------------");
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        // ?????????????????????
        Sheet sheet = workbook.getSheetAt(0);
        // ??????excel?????????
        List<String> head = getExcelHead(sheet);
        // ???????????????????????????(0??????)
        int totalRows = sheet.getLastRowNum();

        List<String> legNames = new ArrayList<>();
        List<String> chargeMethodNames = new ArrayList<>();
        List<String> regionNames = new ArrayList<>(); // ??????????????????  placeFieldList
        List<String> portNames = new ArrayList<>(); // ????????????
        for (int i =1;i <= totalRows;i++){
            // ???????????????
            Row row = sheet.getRow(i);
            String value1 = getCellValue(head, row, fieldCodeName.get("leg"));
            if(StringUtil.notEmpty(value1)){
                legNames.add(value1.trim());
            }
            String value2 = getCellValue(head, row, fieldCodeName.get("chargeMethod"));
            if(StringUtil.notEmpty(value2)){
                chargeMethodNames.add(value2.trim());
            }

            placeFieldList.forEach(key->{
                // ????????????????????????
                String str = fieldCodeName.get(key);
                if (!ObjectUtils.isEmpty(str)) {
                    String value = getCellValue(head, row, str);
                    if(StringUtil.notEmpty(value)){
                        regionNames.add(value.trim());
                    }
                }
            });

            portFieldList.forEach(key->{
                // ????????????????????????
                String str = fieldCodeName.get(key);
                if (!ObjectUtils.isEmpty(str)) {
                    String value = getCellValue(head, row, str);
                    if(StringUtil.notEmpty(value)){
                        portNames.add(value.trim());
                    }
                }
            });
        }

        // ?????? ??????-??????
        Map<String,String> portNameMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(portNames)) {
            /**
             * ????????????, ????????????
             * ??????,????????????
             */
            String portType = null;
            if(TransportModeEnum.AIR_TRANSPORT.name().equals(lgtBiding.getTransportModeCode())){
                // ??????
                portType = "AIR";
            }else if(TransportModeEnum.SEA_TRANSPORT.name().equals(lgtBiding.getTransportModeCode())){
                // ??????
                portType = "OCN";
            }else if(TransportModeEnum.LAND_TRANSPORT.name().equals(lgtBiding.getTransportModeCode())){
                //
                portType = "LAND";
            }
            List<Port> portList = iPortService.list(Wrappers.lambdaQuery(Port.class).
                    in(Port::getPortNameZhs,portNames).
                    eq(!ObjectUtils.isEmpty(portType),Port::getPortType,portType));
            if(CollectionUtils.isNotEmpty(portList)){
                portNameMap = portList.stream().collect(Collectors.toMap(Port::getPortNameEn, Port::getPortCode, (k1, k2) -> k1));
            }
        }

        // ???????????? (??????_?????????)-??????
        Map<String, Region> regionParentMap = new HashMap<>();
        // ???????????? (??????)-??????
        Map<String, Region> regionMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(regionNames)) {
            List<Region> regions = iRegionService.list(Wrappers.lambdaQuery(Region.class).in(Region::getRegionName,regionNames));
            if(CollectionUtils.isNotEmpty(regions)){
                regionParentMap = regions.stream().collect(Collectors.toMap(region -> region.getRegionName() +region.getParentRegionCode(),Function.identity(),(k1, k2)->k1));
                regionMap = regions.stream().collect(Collectors.toMap(region -> String.valueOf(region.getRegionName()),Function.identity(),(k1,k2)->k1));
            }
        }

        // ??????leg??????????????????
        Map<String, Map<String, String>> legChargeMap = iExpenseItemService.queryLegChargeMap(legNames,bidingId);
        // ??????????????????????????????????????????
        Map<String, Map<String, String>> billingCombinationMap = baseClient.queryBillingCombinationMap(chargeMethodNames);
        // ????????????
        Map<String, String> chargeNameMap = EasyExcelUtil.getDicNameCode("CHARGE_NAME", baseClient);
        // ????????????
        Map<String, String> chargeLevelMap = EasyExcelUtil.getDicNameCode("CHARGE_LEVEL", baseClient);
        // ????????????
        Map<String, String> subLevelMap = EasyExcelUtil.getDicNameCode("SUB_LEVEL", baseClient);
        // ??????/??????
        Map<String, String> fclLclMap = EasyExcelUtil.getDicNameCode("FCL/LCL", baseClient);
        // LEG
        Map<String, String> legMap = EasyExcelUtil.getDicNameCode("LEG", baseClient);
        // ??????
        Map<String, String> currencyNameCode = EasyExcelUtil.getCurrencyNameCode(baseClient);
        // ????????????????????? ?????????-???id
        Map<Integer, Long> onlyAddMap = getOnlyAdds(bidingId);
        // ????????????????????????
        List<String> requiredField = getRequiredField(bidingId);
        // ????????????????????????
        Map<String, String> requiredFieldMap = getRequiredFieldMap(bidingId);
        // ??????????????????????????????
        Map<String, String> operabilityFieldMap = getOperabilityFieldMap(bidingId);
        Set<String> hashSet = new HashSet<>();
        Set<Integer> hashSetRowNum = new HashSet<>();

        for (int i =1;i <= totalRows;i++){
            LgtVendorQuotedLine lgtVendorQuotedLine = new LgtVendorQuotedLine();
            StringBuffer errorMsg = new StringBuffer();
            // ???????????????
            Row row = sheet.getRow(i);
            vendorOperatingField.forEach(key -> {
                // ?????????
                String title = fieldCodeName.get(key);
                // ???????????????
                String value = getCellValue(head, row, title);

                // ?????????????????????
                String fieldName = key;
                if (StringUtil.notEmpty(value)) {
                    value = value.trim();
                    // ??????????????????
                    checkForm(errorMsg, title, value, fieldName,errorFlag,lgtVendorQuotedLine);
                    // ??????
                    setFieldValue(errorFlag, chargeNameMap, chargeLevelMap, subLevelMap, fclLclMap, legMap, currencyNameCode, lgtVendorQuotedLine, errorMsg, title, value, fieldName);
                }else {
                    // ??????????????????
                    if(requiredField.contains(fieldName)){
                        errorMsg.append(String.format("[%s]????????????!; ",requiredFieldMap.get(fieldName)));
                        errorFlag.set(true);
                    }
                }
            });

            // ?????????????????????
            Integer onlyAddress = lgtVendorQuotedLine.getOnlyAddress();
            if(ObjectUtils.isEmpty(onlyAddress)){
                errorFlag.set(true);
                errorMsg.append("??????????????????; ");
            }else {
                /**
                 * ??????????????????????????????, ?????????????????????, ?????????????????????
                 */
                // ?????????ID
                Long bidRequirementLineId = onlyAddMap.get(onlyAddress);
                if(null != bidRequirementLineId){
                    lgtVendorQuotedLine.setBidRequirementLineId(bidRequirementLineId);
                    lgtVendorQuotedLine.setRowNum(onlyAddress);
                }else {
                    errorFlag.set(true);
                    errorMsg.append("[??????]???????????????????????????; ");
                }
            }

            if(errorMsg.length() <= 0){
                if(CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
                    Integer rowNum = lgtVendorQuotedLine.getRowNum();
                    // ?????????????????????????????????
                    if(!hashSetRowNum.add(rowNum)){
                        errorFlag.set(true);
                        errorMsg.append("????????????[??????]????????????!; ");
                    }
                }
                // ???????????? LEG +??????+????????????+????????????
                StringBuffer onlyKey = new StringBuffer();
                onlyKey.append(lgtVendorQuotedLine.getRowNum()).
                        append(lgtVendorQuotedLine.getLeg()).
                        append(lgtVendorQuotedLine.getExpenseItem()).
                        append(lgtVendorQuotedLine.getChargeMethod()).
                        append(lgtVendorQuotedLine.getChargeUnit());
                if(!hashSet.add(onlyKey.toString())){
                    errorFlag.set(true);
                    errorMsg.append("??????????????????????????????!; ");
                }
            }

            /**
             * ??????????????????
             */
            if(errorMsg.length() <= 0) {
                Class<LgtVendorQuotedLine> quotedLineClass = LgtVendorQuotedLine.class;
                // ?????????-code???
                Map<String, String> fieldCodeMap = new HashMap<>();
                // ????????????
                processingStartingPoint(startAddList,errorFlag, regionParentMap, regionMap, operabilityFieldMap, lgtVendorQuotedLine, errorMsg, quotedLineClass, fieldCodeMap);
                // ????????????
                processingStartingPoint(endAddList,errorFlag, regionParentMap, regionMap, operabilityFieldMap, lgtVendorQuotedLine, errorMsg, quotedLineClass, fieldCodeMap);
            }

            /**
             * ????????????
             */
            if(errorMsg.length() <= 0){
                // portCodeMap
                Class<LgtVendorQuotedLine> quotedLineClass = LgtVendorQuotedLine.class;
                for(String key : portFieldList){
                    if(errorMsg.length() > 0) break;
                    try {
                        Object value = getFieldValue(lgtVendorQuotedLine, quotedLineClass, key);
                        if(!ObjectUtils.isEmpty(value)){
                            // ????????????
                            String portName = value.toString();
                            // ????????????
                            String portCode = portNameMap.get(portName);
                            if(null != portCode){
                                key = key+"Code";
                                setFieldValue(lgtVendorQuotedLine, quotedLineClass, key,portCode);
                            }else {
                                errorFlag.set(true);
                                errorMsg.append(String.format("[%s]????????????; ",operabilityFieldMap.get(key)));
                            }
                        }
                    } catch (Exception e) {
                        errorFlag.set(true);
                        errorMsg.append("????????????????????????:"+e.getMessage());
                        break;
                    }
                }
            }

            if(errorMsg.length() <= 0) {
                /**
                 * 1. ????????????????????????????????????????????????
                 * 2. ??????leg????????????????????????
                 */
                if (vendorOperatingField.contains("chargeMethod") && vendorOperatingField.contains("chargeUnit")) {
                    String chargeUnit = lgtVendorQuotedLine.getChargeUnit();
                    String chargeMethod = lgtVendorQuotedLine.getChargeMethod();
                    if(!ObjectUtils.isEmpty(chargeUnit) && !ObjectUtils.isEmpty(chargeMethod)){
                        Map<String, String> map = billingCombinationMap.get(chargeMethod);
                        if(null == map){
                            errorMsg.append("?????????????????????????????????????????????!; ");
                            errorFlag.set(true);
                        }else {
                            if(ObjectUtils.isEmpty(map.get(chargeUnit))){
                                errorMsg.append("?????????????????????????????????????????????!; ");
                                errorFlag.set(true);
                            }
                        }
                    }
                }

                if (vendorOperatingField.contains("leg") && vendorOperatingField.contains("expenseItem")) {
                    String leg = lgtVendorQuotedLine.getLeg();
                    String expenseItem = lgtVendorQuotedLine.getExpenseItem();
                    if(!ObjectUtils.isEmpty(leg) && !ObjectUtils.isEmpty(expenseItem)){
                        Map<String, String> map = legChargeMap.get(leg);
                        if(null == map){
                            errorMsg.append("LEG???????????????????????????!; ");
                            errorFlag.set(true);
                        }else {
                            if(ObjectUtils.isEmpty(map.get(expenseItem))){
                                errorMsg.append("LEG???????????????????????????!; ");
                                errorFlag.set(true);
                            }
                        }
                    }
                }
            }
            errorMsgs.add(errorMsg.toString());
            lgtVendorQuotedLines.add(lgtVendorQuotedLine);
        }
        log.info("--------------------------------------????????????????????????????????????---------------------------------");
        return workbook;
    }

    public void processingStartingPoint(List<String> addList,AtomicBoolean errorFlag, Map<String, Region> regionParentMap, Map<String, Region> regionMap, Map<String, String> operabilityFieldMap, LgtVendorQuotedLine lgtVendorQuotedLine, StringBuffer errorMsg, Class<LgtVendorQuotedLine> quotedLineClass, Map<String, String> fieldCodeMap) {
        for(int j = 0; j<addList.size();j++){
            if(errorMsg.length() > 0 ){
                break;
            }
            try {
                String key = addList.get(j);
                Object value = getFieldValue(lgtVendorQuotedLine, quotedLineClass, key);
                if(!ObjectUtils.isEmpty(value)){
                    // ????????????
                    String addName = value.toString();
                    // ??????????????????
                    if(j != 0){
                        String previousKey = addList.get(j - 1);
                        String regionCode = fieldCodeMap.get(previousKey);
                        if(ObjectUtils.isEmpty(regionCode)){
                            // ??????????????????
                            setAddCode(addList,errorFlag, regionMap, operabilityFieldMap, lgtVendorQuotedLine, errorMsg, quotedLineClass, fieldCodeMap, j, key, addName);
                        }else {
                            Region region = regionParentMap.get(addName + regionCode);
                            if(null != region){
                                // ???  ?????????-code??? ??????
                                fieldCodeMap.put(addList.get(j),region.getRegionCode());
                                key = key+"Code";
                                setFieldValue(lgtVendorQuotedLine, quotedLineClass, key,region.getRegionCode());
                            }else {
                                errorFlag.set(true);
                                errorMsg.append(String.format("[%s]???[%s]??????????????????",operabilityFieldMap.get(addList.get(j)),operabilityFieldMap.get(addList.get(j-1))));
                            }
                        }
                    }else {
                        // ??????????????????
                        setAddCode(addList,errorFlag, regionMap, operabilityFieldMap, lgtVendorQuotedLine, errorMsg, quotedLineClass, fieldCodeMap, j, key, addName);
                    }
                }
            } catch (Exception e) {
                errorFlag.set(true);
                errorMsg.append("????????????????????????:"+e.getMessage());
                break;
            }
        }
    }

    public void setAddCode(List<String> addList, AtomicBoolean errorFlag, Map<String, Region> regionMap, Map<String, String> operabilityFieldMap, LgtVendorQuotedLine lgtVendorQuotedLine, StringBuffer errorMsg, Class<LgtVendorQuotedLine> quotedLineClass, Map<String, String> fieldCodeMap, int j, String key, String addName) throws NoSuchFieldException, IllegalAccessException {
        Region region = regionMap.get(addName);
        if(null != region){
            // ???  ?????????-code??? ??????
            fieldCodeMap.put(addList.get(j),region.getRegionCode());
            key = key+"Code";
            setFieldValue(lgtVendorQuotedLine, quotedLineClass, key,region.getRegionCode());
        }else {
            errorFlag.set(true);
            errorMsg.append(String.format("[%s]????????????:",operabilityFieldMap.get(key)));
        }
    }

    public Object getFieldValue(LgtVendorQuotedLine lgtVendorQuotedLine, Class<LgtVendorQuotedLine> quotedLineClass, String key) throws NoSuchFieldException, IllegalAccessException {
        // ??????
        Field field = quotedLineClass.getDeclaredField(key);
        field.setAccessible(true);
        return field.get(lgtVendorQuotedLine);
    }

    public void setFieldValue(LgtVendorQuotedLine lgtVendorQuotedLine, Class<LgtVendorQuotedLine> quotedLineClass, String key,Object value) throws NoSuchFieldException, IllegalAccessException {
        // ??????
        Field field = quotedLineClass.getDeclaredField(key);
        field.setAccessible(true);
        field.set(lgtVendorQuotedLine,value);
    }

    public String getCellValue(List<String> head, Row row, String title) {
        String cellValue = null;
        if (StringUtil.notEmpty(title)) {
            int index = head.indexOf(title);
            if (index >= 0) {
                Cell cell = row.getCell(index);
                // ?????????
                cellValue =  ExcelUtil.getCellValue(cell);
            }
        }
        return cellValue;
    }

    public List<String> getRequiredField(Long bidingId) {
        List<String> requiredField = new ArrayList<>();
        List<LgtBidTemplate> lgtBidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).
                eq(LgtBidTemplate::getBidingId, bidingId).
                eq(LgtBidTemplate::getVendorNotEmptyFlag, YesOrNo.YES.getValue()));
        if(CollectionUtils.isNotEmpty(lgtBidTemplates)){
            requiredField = lgtBidTemplates.stream().map(lgtBidTemplate -> StringUtil.toCamelCase(lgtBidTemplate.getFieldCode())).collect(Collectors.toList());
        }
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        /**
         * ???????????????????????????: ???????????????????????????
         */
        if(CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
            if(!requiredField.contains("chargeUnit")){
                requiredField.add("chargeUnit");
            }
            if(!requiredField.contains("chargeMethod")){
                requiredField.add("chargeMethod");
            }
        }
        return requiredField;
    }

    public Map<String,String> getRequiredFieldMap(Long bidingId) {
        Map<String,String> requiredField = new HashMap<>();
        List<LgtBidTemplate> lgtBidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).
                eq(LgtBidTemplate::getBidingId, bidingId).
                eq(LgtBidTemplate::getVendorNotEmptyFlag, YesOrNo.YES.getValue()));
        if(CollectionUtils.isNotEmpty(lgtBidTemplates)){
            requiredField = lgtBidTemplates.stream().collect(Collectors.toMap(lgtBidTemplate -> StringUtil.toCamelCase(lgtBidTemplate.getFieldCode()), LgtBidTemplate::getFieldName, (k1, k2) -> k1));
        }
        return requiredField;
    }

    public Map<String,String> getOperabilityFieldMap(Long bidingId) {
        Map<String,String> requiredField = new HashMap<>();
        List<LgtBidTemplate> lgtBidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).
                eq(LgtBidTemplate::getBidingId, bidingId).
                eq(LgtBidTemplate::getVendorOperateFlag, YesOrNo.YES.getValue()));
        if(CollectionUtils.isNotEmpty(lgtBidTemplates)){
            requiredField = lgtBidTemplates.stream().collect(Collectors.toMap(lgtBidTemplate -> StringUtil.toCamelCase(lgtBidTemplate.getFieldCode()), LgtBidTemplate::getFieldName, (k1, k2) -> k1));
        }
        return requiredField;
    }

    public Map<Integer,Long> getOnlyAdds(Long bidingId) {
        Map<Integer,Long> onlyAddMap = new HashMap<>();
        List<LgtBidRequirementLine> lgtBidRequirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).
                eq(LgtBidRequirementLine::getBidingId, bidingId));
        if(CollectionUtils.isNotEmpty(lgtBidRequirementLines)){
            lgtBidRequirementLines.forEach(LgtBidingVendorServiceImpl::setStartEndAddress);
            onlyAddMap = lgtBidRequirementLines.stream().collect(Collectors.toMap(LgtBidRequirementLine::getRowNum, LgtBidRequirementLine::getBidRequirementLineId,(k1,k2)->k1));
        }
        return onlyAddMap;
    }

    public Map<String,Long> getOnlyAddsByI(Long bidingId) {
        Map<String,Long> onlyAddMap = new HashMap<>();
        List<LgtBidRequirementLine> lgtBidRequirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).
                eq(LgtBidRequirementLine::getBidingId, bidingId));
        if(CollectionUtils.isNotEmpty(lgtBidRequirementLines)){
            /**
             * ??????????????????
             */
            lgtBidRequirementLines.forEach(LgtBidingVendorServiceImpl::setStartEndAddress);
            onlyAddMap = lgtBidRequirementLines.stream().collect(Collectors.toMap(lgtBidRequirementLine -> lgtBidRequirementLine.getStartAddress() + lgtBidRequirementLine.getEndAddress() + lgtBidRequirementLine.getChargeMethod() + lgtBidRequirementLine.getChargeUnit(), LgtBidRequirementLine::getBidRequirementLineId));
        }
        return onlyAddMap;
    }

    /**
     * ???????????? ?????????/?????????
     * @param obj
     */
    public static <T> void setStartEndAddress(T obj){
        /**
         * ????????? - toCountry-toProvince-toCity-toCounty-toPlace-toPortId
         * ????????? - fromCountry-fromProvince-fromCity-fromCounty-fromPlace-fromPortCode
         */
        List<String> startList = Arrays.asList("fromCountry", "fromProvince", "fromCity", "fromCounty", "fromPlace","fromPortCode");
        List<String> endList = Arrays.asList("toCountry", "toProvince", "toCity", "toCounty", "toPlace","toPortCode");
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

    public void setFieldValue(AtomicBoolean errorFlag, Map<String, String> chargeNameMap, Map<String, String> chargeLevelMap, Map<String, String> subLevelMap, Map<String, String> fclLclMap, Map<String, String> legMap, Map<String, String> currencyNameCode, LgtVendorQuotedLine lgtVendorQuotedLine, StringBuffer errorMsg, String title, String value, String fieldName) {
        try {
            Class<LgtVendorQuotedLine> lgtVendorQuotedLineClass = LgtVendorQuotedLine.class;
            Field field = lgtVendorQuotedLineClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            switch (fieldName){
                case "expenseItem":
                    // ??????
                    setDicValue(errorFlag, chargeNameMap, lgtVendorQuotedLine, errorMsg, value, field,title);
                    Field field1 = lgtVendorQuotedLineClass.getDeclaredField("expenseItemName");
                    field1.setAccessible(true);
                    field1.set(lgtVendorQuotedLine,value);
                    break;
                case "chargeMethod":
                    // ????????????
                    setDicValue(errorFlag, chargeLevelMap, lgtVendorQuotedLine, errorMsg, value, field,title);
                    Field field2 = lgtVendorQuotedLineClass.getDeclaredField("chargeMethodName");
                    field2.setAccessible(true);
                    field2.set(lgtVendorQuotedLine,value);
                    break;
                case "chargeUnit":
                    // ????????????
                    setDicValue(errorFlag, subLevelMap, lgtVendorQuotedLine, errorMsg, value, field,title);
                    Field field3 = lgtVendorQuotedLineClass.getDeclaredField("chargeUnitName");
                    field3.setAccessible(true);
                    field3.set(lgtVendorQuotedLine,value);
                    break;
                case "wholeArk":
                    // ??????/??????
                    setDicValue(errorFlag, fclLclMap, lgtVendorQuotedLine, errorMsg, value, field,title);
                    break;
                case "leg":
                    setDicValue(errorFlag, legMap, lgtVendorQuotedLine, errorMsg, value, field,title);
                    Field field4 = lgtVendorQuotedLineClass.getDeclaredField("legName");
                    field4.setAccessible(true);
                    field4.set(lgtVendorQuotedLine,value);
                    break;
                case "currency":
                    setDicValue(errorFlag, currencyNameCode, lgtVendorQuotedLine, errorMsg, value, field,title);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error(""+e);
        }
    }

    public void setDicValue(AtomicBoolean errorFlag, Map<String, String> dicMap, LgtVendorQuotedLine lgtVendorQuotedLine, StringBuffer errorMsg, String value, Field field,String title) throws IllegalAccessException {
        String dicCode = dicMap.get(value);
        if(StringUtil.notEmpty(dicCode)){
            field.set(lgtVendorQuotedLine,dicCode);
        }else {
            errorFlag.set(true);
            errorMsg.append(String.format("%s??????????????????; ",title));
        }
    }

    public void checkForm(StringBuffer errorMsg, String title, String value, String fieldName,AtomicBoolean errorFlag,LgtVendorQuotedLine lgtVendorQuotedLine) {
        try {
            Class<LgtVendorQuotedLine> lgtVendorQuotedLineClass = LgtVendorQuotedLine.class;
            Field field = lgtVendorQuotedLineClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            String typeName = field.getType().getSimpleName();
            switch (typeName){
                case "Long":
                    long aLong = Long.parseLong(value);
                    field.set(lgtVendorQuotedLine,aLong);
                    break;
                case "Integer":
                    int i1 = Integer.parseInt(value);
                    field.set(lgtVendorQuotedLine,i1);
                    break;
                case "BigDecimal":
                    BigDecimal decimal = new BigDecimal(value);
                    field.set(lgtVendorQuotedLine,decimal);
                    break;
                default:
                    field.set(lgtVendorQuotedLine,value);
            }
        } catch (Exception e) {
            errorMsg.append(String.format("[%s]????????????!; ",title));
            errorFlag.set(true);
        }
    }

    public List<String> getExcelHead(Sheet sheet) {
        // ?????????????????????
        List<String> head = new ArrayList<>();
        // ?????????????????????, ????????????????????????
        int totalCells = sheet.getRow(0).getLastCellNum();
        // ????????????
        Row headRow = sheet.getRow(0);
        // ???????????????????????????
        for (int i = 0; i < totalCells; i++) {
            Cell cell = headRow.getCell(i);
            head.add(ExcelUtil.getCellValue(cell));
        }
        return head;
    }

    @Override
    public LgtQuotedLineImportTitle getExcelTitle(MultipartFile file,Long bidingId) throws IOException {
        // ????????????????????????.xlsx
        EasyExcelUtil.checkExcelIsXlsx(file);
        // ????????????excel??????
        List<String> titles = getTitles(file);
        Map<String, String> fieldCodeName = new HashMap<>();
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        // ???????????????????????????
        List<LgtBidTemplate> lgtBidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).
                eq(LgtBidTemplate::getBidingId, bidingId).
                eq(LgtBidTemplate::getVendorOperateFlag, YesOrNo.YES.getValue()));
        if(CollectionUtils.isNotEmpty(lgtBidTemplates)){
            fieldCodeName.put("onlyAddress","??????");
            /**
             * ????????????,?????????????????????????????????
             */
            if(CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
                fieldCodeName.put("chargeMethod","????????????");
                fieldCodeName.put("chargeUnit","????????????");
            }
            lgtBidTemplates.forEach(lgtBidTemplate -> {
                fieldCodeName.put(StringUtil.toCamelCase(lgtBidTemplate.getFieldCode()),lgtBidTemplate.getFieldName());
            });
        }
        return LgtQuotedLineImportTitle.builder().titles(titles).fieldCodeName(fieldCodeName).build();
    }

    public List<String> getTitles(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        // ?????????????????????
        Sheet sheet = workbook.getSheetAt(0);
        // ???????????????????????????(0??????)
        int totalRows = sheet.getLastRowNum();
        // ?????????????????????, ????????????????????????
        int totalCells = sheet.getRow(0).getLastCellNum();
        // ?????????????????????
        List<String> titles = new ArrayList<>();
        // ????????????
        Row headRow = sheet.getRow(0);
        // ???????????????????????????
        for (int i = 0; i < totalCells; i++) {
            Cell cell = headRow.getCell(i);
            titles.add(ExcelUtil.getCellValue(cell));
        }
        return titles;
    }

    @Override
    public LgtVendorQuotedSumVendorDto getLgtVendorQuotedSumVendorDto(Long bidingId) {
        LgtVendorQuotedSumVendorDto lgtVendorQuotedSumVendorDto = new LgtVendorQuotedSumVendorDto();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if(ObjectUtils.isEmpty(loginAppUser.getCompanyId()) || !UserType.VENDOR.name().equals(loginAppUser.getUserType())){
            return lgtVendorQuotedSumVendorDto;
        }
        // ?????????
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        //?????????????????????????????????????????????????????? VISIBLE_WIN_VENDOR
        if (!YesOrNo.YES.getValue().equals(lgtBiding.getVisibleWinVendor())) {
            return lgtVendorQuotedSumVendorDto;
        }


        // ????????????????????????
        List<LgtRound> lgtRounds = iLgtRoundService.list(Wrappers.lambdaQuery(LgtRound.class).
                eq(LgtRound::getBidingId, bidingId).
                eq(LgtRound::getPublicResult, YesOrNo.YES.getValue()));

        if (CollectionUtils.isNotEmpty(lgtRounds)) {
            List<Integer> rounds = lgtRounds.stream().map(LgtRound::getRound).collect(Collectors.toList());
            List<LgtVendorQuotedSum> lgtVendorQuotedSums = iLgtVendorQuotedSumService.list(Wrappers.lambdaQuery(LgtVendorQuotedSum.class).
                    eq(LgtVendorQuotedSum::getBidingId,bidingId).
                    in(LgtVendorQuotedSum::getRound,rounds).
                    and(wrapper -> wrapper.eq(LgtVendorQuotedSum::getBidResult, SelectionStatusEnum.WIN.getValue()).or().
                            eq(LgtVendorQuotedSum::getBidResult,SelectionStatusEnum.FIRST_WIN.getValue()).or().
                            eq(LgtVendorQuotedSum::getBidResult,SelectionStatusEnum.SECOND_WIN.getValue())).
                    eq(LgtVendorQuotedSum::getVendorId,loginAppUser.getCompanyId()));
            if(CollectionUtils.isNotEmpty(lgtVendorQuotedSums)){
                if(!YesOrNo.YES.getValue().equals(lgtBiding.getVisibleFinalPrice())){
                    lgtVendorQuotedSums.forEach(lgtVendorQuotedSum -> {
                        lgtVendorQuotedSum.setSumPrice(null);
                    });
                }
            }
            lgtVendorQuotedSumVendorDto.setLgtVendorQuotedSums(lgtVendorQuotedSums);
        }
        return lgtVendorQuotedSumVendorDto;
    }

    @Override
    public LgtVendorQuotedHeadVendorDto getLgtVendorQuotedHeadVendorDto(Long bidingId) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if(ObjectUtils.isEmpty(loginAppUser.getCompanyId()) || !UserType.VENDOR.name().equals(loginAppUser.getUserType())){
            return null;
        }
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        // ?????????
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(Wrappers.lambdaQuery(LgtVendorQuotedLine.class).
                eq(LgtVendorQuotedLine::getBidingId, bidingId).
                eq(LgtVendorQuotedLine::getRound, lgtBiding.getCurrentRound()).
                eq(LgtVendorQuotedLine::getVendorId, loginAppUser.getCompanyId()));
        // ??????
        List<LgtBidShipPeriod> lgtBidShipPeriods = iLgtBidShipPeriodService.list(Wrappers.lambdaQuery(LgtBidShipPeriod.class).
                eq(LgtBidShipPeriod::getBidingId, bidingId).
                eq(LgtBidShipPeriod::getRound, lgtBiding.getCurrentRound()).
                eq(LgtBidShipPeriod::getVendorId, loginAppUser.getCompanyId()));
        // ??????
        List<LgtFileConfig> lgtFileConfigs = iLgtFileConfigService.list(Wrappers.lambdaQuery(LgtFileConfig.class).
                eq(LgtFileConfig::getBidingId, bidingId));
        if(CollectionUtils.isNotEmpty(lgtFileConfigs)){
            lgtFileConfigs.forEach(lgtFileConfig -> {
                Long requireId = lgtFileConfig.getRequireId();
                LgtVendorFile lgtVendorFile = iLgtVendorFileService.getOne(Wrappers.lambdaQuery(LgtVendorFile.class).
                        eq(LgtVendorFile::getRequireId, requireId).
                        eq(LgtVendorFile::getVendorId,loginAppUser.getCompanyId()).
                        eq(LgtVendorFile::getRound, lgtBiding.getCurrentRound()).
                        last(" LIMIT 1"));
                if (null != lgtVendorFile) {
                    lgtFileConfig.setVendorDocId(lgtVendorFile.getDocId());
                    lgtFileConfig.setVendorFileName(lgtVendorFile.getFileName());
                }
            });
        }
        return LgtVendorQuotedHeadVendorDto.builder().lgtVendorQuotedLines(lgtVendorQuotedLines).lgtBidShipPeriods(lgtBidShipPeriods).lgtFileConfigs(lgtFileConfigs).build();
    }

    @Override
    public LgtBidRequirementLineVendorDto getLgtBidRequirementLineVendorDto(Long bidingId) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if(ObjectUtils.isEmpty(loginAppUser.getCompanyId()) || !UserType.VENDOR.name().equals(loginAppUser.getUserType())){
            return null;
        }
        List<LgtBidRequirementLine> lgtBidRequirementLines = iLgtBidRequirementLineService.list(new QueryWrapper<>(new LgtBidRequirementLine().setBidingId(bidingId)));
        List<LgtBidTemplate> bidTemplates = iLgtBidTemplateService.list(new QueryWrapper<>(new LgtBidTemplate().setBidingId(bidingId)));
        return LgtBidRequirementLineVendorDto.builder().lgtBidRequirementLines(lgtBidRequirementLines).lgtBidTemplates(bidTemplates).build();
    }

    @Override
    public LgtBidVendorDto getLgtBidVendor(Long bidingId) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if(ObjectUtils.isEmpty(loginAppUser.getCompanyId()) || !UserType.VENDOR.name().equals(loginAppUser.getUserType())){
            return null;
        }
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        List<LgtBidFile> lgtBidFiles = iLgtFileService.list(Wrappers.lambdaQuery(LgtBidFile.class).
                eq(LgtBidFile::getBidingId, bidingId).
                eq(LgtBidFile::getFileType, BidFileType.Supplier.getValue()));
        return LgtBidVendorDto.builder().biding(lgtBiding).fileList(lgtBidFiles).build();
    }

    @Override
    public void withdrawQuotedPrice(Long bidingId, Long vendorId,String withdrawReason) {
        /**
         * 1. ??????????????????????????????
         */
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        Assert.isTrue(YesOrNo.YES.getValue().equals(lgtBiding.getWithdrawBiding()),"??????????????????????????????!");
        Assert.isTrue(lgtBiding.getEnrollEndDatetime().compareTo(new Date()) > 0,"????????????????????????,?????????????????????");
        // ????????????
        LgtVendorQuotedHead lgtVendorQuotedHead = iLgtVendorQuotedHeadService.getOne(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, bidingId).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).
                eq(LgtVendorQuotedHead::getVendorId, vendorId).last(" LIMIT 1"));
        lgtVendorQuotedHead.setStatus(BiddingOrderStates.WITHDRAW.getValue());
        lgtVendorQuotedHead.setWithdrawReason(withdrawReason);
        iLgtVendorQuotedHeadService.updateById(lgtVendorQuotedHead);
        // ??????????????????
        String biddingSuppliers = lgtBiding.getBiddingSuppliers();
        String[] split = biddingSuppliers.split("/");
        biddingSuppliers = (Integer.parseInt(split[0]) - 1) + "/" +split[1];
        lgtBiding.setBiddingSuppliers(biddingSuppliers);
        iLgtBidingService.updateById(lgtBiding);
    }

    @Override
    public LgtBidInfoVO supplierDetails(Long bidingId) {
        return iLgtBidingService.supplierDetails(bidingId);
    }

    // ?????????????????????, ????????????
    @Transactional
    public void checkbasePrice(List<LgtVendorQuotedLine> lgtVendorQuotedLines,LgtBiding lgtBiding){
        /**
         * ??????????????????????????????????????????????????????????????????????????????
         * ??????????????????+????????????+????????????+??????+leg+?????????+????????????+???????????????????????????????????????
         * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
         */
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
            lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                List<BasePrice> basePrices = basePriceService.list(Wrappers.lambdaQuery(BasePrice.class).
                        eq(BasePrice::getBusinessModeCode, lgtBiding.getBusinessModeCode()).
                        eq(BasePrice::getTransportModeCode, lgtBiding.getTransportModeCode()).
                        and(wrapper -> {
                            wrapper.eq(BasePrice::getRegionCode, lgtVendorQuotedLine.getFromCountryCode()).or().
                                    eq(BasePrice::getRegionCode, lgtVendorQuotedLine.getFromProvinceCode()).or().
                                    eq(BasePrice::getRegionCode, lgtVendorQuotedLine.getFromCityCode()).or().
                                    eq(BasePrice::getRegionCode, lgtVendorQuotedLine.getFromCountyCode()).or().
                                    eq(BasePrice::getRegionCode, lgtVendorQuotedLine.getToCountryCode()).or().
                                    eq(BasePrice::getRegionCode, lgtVendorQuotedLine.getToProvinceCode()).or().
                                    eq(BasePrice::getRegionCode, lgtVendorQuotedLine.getToCityCode()).or().
                                    eq(BasePrice::getRegionCode, lgtVendorQuotedLine.getToCountyCode());
                        }).
                        and(wrapper -> {
                            wrapper.eq(BasePrice::getPortCode, lgtVendorQuotedLine.getFromPortCode()).or().
                                    eq(BasePrice::getPortCode, lgtVendorQuotedLine.getToPortCode());
                        }).
                        eq(BasePrice::getLeg, lgtVendorQuotedLine.getLeg()).
                        eq(BasePrice::getExpenseItem, lgtVendorQuotedLine.getExpenseItem()).
                        eq(BasePrice::getChargeMethod, lgtVendorQuotedLine.getChargeMethod()).
                        eq(BasePrice::getChargeUnit, lgtVendorQuotedLine.getChargeUnit()).
                        eq(BasePrice::getStatus, LogisticsStatus.EFFECTIVE.getValue())
                );
                if(CollectionUtils.isNotEmpty(basePrices)){

                }
            });
        }

    }

    @Override
    @Transactional
    public void submitQuotedPrice(LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto) {
        // ??????????????????
        // ????????????
        checkSubmitQuotedPriceParam(lgtVendorQuotedHeadDto);
        // ??????????????????
        quotedPriceSave(lgtVendorQuotedHeadDto);
        // ????????????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        LgtVendorQuotedHead lgtVendorQuotedHead = lgtVendorQuotedHeadDto.getLgtVendorQuotedHead();
        lgtVendorQuotedHead.setStatus(BiddingOrderStates.SUBMISSION.getValue())
                .setIfProxy(YesOrNo.NO.getValue())
                .setSubmitDate(new Date())
                .setSubmitUserId(loginAppUser.getUserId())
                .setSubmitUsername(loginAppUser.getUsername())
                .setSubmitNikeName(loginAppUser.getNickname());
        iLgtVendorQuotedHeadService.updateById(lgtVendorQuotedHead);

        // ????????????
        Long bidingId = lgtVendorQuotedHeadDto.getLgtBiding().getBidingId();

        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        int count = iLgtVendorQuotedHeadService.count(Wrappers.lambdaQuery(LgtVendorQuotedHead.class)
                .eq(LgtVendorQuotedHead::getBidingId, bidingId)
                .eq(LgtVendorQuotedHead::getStatus, BiddingOrderStates.SUBMISSION.getValue())
                .eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound())
        );
        String biddingSuppliers = lgtBiding.getBiddingSuppliers();
        String[] split = biddingSuppliers.split("/");
        biddingSuppliers = count + "/" +split[1];
        lgtBiding.setBiddingSuppliers(biddingSuppliers);
        iLgtBidingService.update(null,Wrappers.lambdaUpdate(LgtBiding.class)
                .eq(LgtBiding::getBidingId,bidingId)
                .set(LgtBiding::getBiddingSuppliers,biddingSuppliers)
        );
    }

    public void checkSubmitQuotedPriceParam(LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto) {
        // ??????????????????????????????
        List<LgtFileConfig> lgtFileConfigs = lgtVendorQuotedHeadDto.getLgtFileConfigs();
        if(CollectionUtils.isNotEmpty(lgtFileConfigs)){
            lgtFileConfigs.forEach(lgtFileConfig -> {
                Assert.isTrue(!ObjectUtils.isEmpty(lgtFileConfig.getVendorDocId()),"????????????????????????!");
            });
        }
        Assert.notNull(lgtVendorQuotedHeadDto.getLgtBiding(),"???????????????????????????!");

        String ifVendorSubmitShipDate = lgtVendorQuotedHeadDto.getLgtBiding().getIfVendorSubmitShipDate();
        if(YesOrNo.YES.getValue().equals(ifVendorSubmitShipDate)){
            Assert.isTrue(CollectionUtils.isNotEmpty(lgtVendorQuotedHeadDto.getLgtBidShipPeriods()),"????????????????????????!");
        }
        // ????????????????????????
        checkLgtVendorQuotedLines(lgtVendorQuotedHeadDto);
    }

    public void checkLgtVendorQuotedLines(LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto) {
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

    /**
     * ??????????????????????????????
     * @param lgtVendorQuotedHead
     */
    @Override
    public void checkLgtVendorQuotedLine(LgtVendorQuotedHeadDto lgtVendorQuotedHead){
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = lgtVendorQuotedHead.getLgtVendorQuotedLines();
        LgtBiding lgtBiding = lgtVendorQuotedHead.getLgtBiding();
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
            List<LgtBidTemplate> lgtBidTemplates = iLgtBidTemplateService.list(Wrappers.lambdaQuery(LgtBidTemplate.class).
                    eq(LgtBidTemplate::getBidingId, lgtBiding.getBidingId()).
                    eq(LgtBidTemplate::getVendorNotEmptyFlag, YesOrNo.YES.getValue()));
            if(CollectionUtils.isNotEmpty(lgtBidTemplates)){
                Class<LgtVendorQuotedLine> quotedLineClass = LgtVendorQuotedLine.class;
                Map<String, String> fieldMap = lgtBidTemplates.stream().collect(Collectors.toMap(lgtBidTemplate -> StringUtil.toCamelCase(lgtBidTemplate.getFieldCode()), LgtBidTemplate::getFieldName, (k1, k2) -> k1));
                lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                    fieldMap.keySet().forEach(field -> {
                        Object o = null;
                        boolean flag = true;
                        try {
                            Field declaredField = quotedLineClass.getDeclaredField(field);
                            declaredField.setAccessible(true);
                            o = declaredField.get(lgtVendorQuotedLine);
                        } catch (Exception e) {
                            flag = false;
                        }
                        if(flag){
                            Assert.isTrue(!ObjectUtils.isEmpty(o),String.format("????????????:[%s]????????????",fieldMap.get(field)));
                        }
                    });
                });
            }
        }

        /**
         * ????????????, ??????????????????????????????????????????
         */
       /* List<LgtBidShipPeriod> lgtBidShipPeriods = lgtVendorQuotedHead.getLgtBidShipPeriods();
        if(CollectionUtils.isNotEmpty(lgtBidShipPeriods)){
            lgtBidShipPeriods.forEach(LgtBidingServiceImpl::setStartEndAddress);
            List<LgtBidRequirementLine> requirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).
                    eq(LgtBidRequirementLine::getBidingId, lgtBiding.getBidingId()));
            List<String> onlyKeys = requirementLines.stream().map(lgtBidRequirementLine -> lgtBidRequirementLine.getStartAddress() + lgtBidRequirementLine.getEndAddress()).collect(Collectors.toList());
            lgtBidShipPeriods.forEach(lgtBidShipPeriod -> {
                StringBuffer key = new StringBuffer().append(lgtBidShipPeriod.getStartAddress()).append(lgtBidShipPeriod.getEndAddress());
                boolean flag = onlyKeys.contains(key.toString());
                Assert.isTrue(flag,String.format("????????????[%s]????????????????????????!",key));
            });
        }*/
    }

    @Override
    @Transactional
    public void quotedPriceSave(LgtVendorQuotedHeadDto lgtVendorQuotedHead) {
        checkLgtVendorQuotedLine(lgtVendorQuotedHead);
        // ????????????,???????????????????????????????????????????????????????????????????????????????????????????????????
        Long bidingId = lgtVendorQuotedHead.getLgtBiding().getBidingId();
        Assert.notNull(bidingId,"????????????: bidingId");
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        LgtVendorQuotedHead vendorQuotedHead = lgtVendorQuotedHead.getLgtVendorQuotedHead();
        Assert.notNull(vendorQuotedHead,"???????????????????????????!");
        Assert.isTrue(BiddingProjectStatus.ACCEPT_BID.getValue().equals(lgtBiding.getBidingStatus()) &&
                        (BiddingOrderStates.DRAFT.getValue().equals(vendorQuotedHead.getStatus()) ||
                                BiddingOrderStates.WITHDRAW.getValue().equals(vendorQuotedHead.getStatus())),
                "?????????????????????\"???????????????\",???????????????\"?????????\"?????????????????????");

        // ??????????????????
        saveLgtVendorQuotedLines(lgtVendorQuotedHead, bidingId, lgtBiding, vendorQuotedHead);

        // ????????????
        saveLgtBidShipPeriods(lgtVendorQuotedHead, bidingId, lgtBiding, vendorQuotedHead);

        // ????????????
        saveVendorFile(lgtVendorQuotedHead, bidingId, lgtBiding, vendorQuotedHead);
    }

    public void saveVendorFile(LgtVendorQuotedHeadDto lgtVendorQuotedHead, Long bidingId, LgtBiding lgtBiding, LgtVendorQuotedHead vendorQuotedHead) {
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

    @Transactional
    public void saveLgtBidShipPeriods(LgtVendorQuotedHeadDto lgtVendorQuotedHead, Long bidingId, LgtBiding lgtBiding, LgtVendorQuotedHead vendorQuotedHead) {
        List<LgtBidRequirementLine> bidRequirementLines = iLgtBidRequirementLineService.list(Wrappers.lambdaQuery(LgtBidRequirementLine.class).eq(LgtBidRequirementLine::getBidingId, lgtBiding.getBidingId()));
        Map<String, LgtBidRequirementLine> requirementLineMap = bidRequirementLines.stream().collect(Collectors.toMap(lgtBidRequirementLine -> lgtBidRequirementLine.getStartAddress() + lgtBidRequirementLine.getEndAddress(), Function.identity(),(k1, k2)->k1));
        List<LgtBidShipPeriod> lgtBidShipPeriods = lgtVendorQuotedHead.getLgtBidShipPeriods();
        iLgtBidShipPeriodService.remove(new QueryWrapper<>(new LgtBidShipPeriod().setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
        if(CollectionUtils.isNotEmpty(lgtBidShipPeriods)){
            HashSet<String> hashSet = new HashSet<>();
            lgtBidShipPeriods.forEach(lgtBidShipPeriod -> {
                // ??????????????????????????????????????????
                LgtBidingServiceImpl.setStartEndAddress(lgtBidShipPeriod);
                // String key = String.valueOf(lgtBidShipPeriod.getStartAddress()) + lgtBidShipPeriod.getEndAddress();
                // Assert.isTrue(hashSet.add(key),String.format("????????????: [%s],????????????!",key));
                // LgtBidRequirementLine requirementLine = requirementLineMap.get(key);
                // Assert.notNull(requirementLine,String.format("????????????: [%s],????????????????????????!",key));

                lgtBidShipPeriod
                        .setShipPeriodId(IdGenrator.generate())
                        .setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())
                        .setBidingId(bidingId)
                        .setVendorId(vendorQuotedHead.getVendorId())
                        .setVendorCode(vendorQuotedHead.getVendorCode())
                        .setVendorName(vendorQuotedHead.getVendorName())
                        //.setBidRequirementLineId(requirementLine.getBidRequirementLineId())
                        .setRound(lgtBiding.getCurrentRound());
            });
            iLgtBidShipPeriodService.saveBatch(lgtBidShipPeriods);
        }
    }

    public void saveLgtVendorQuotedLines(LgtVendorQuotedHeadDto lgtVendorQuotedHead, Long bidingId, LgtBiding lgtBiding, LgtVendorQuotedHead vendorQuotedHead) {
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = lgtVendorQuotedHead.getLgtVendorQuotedLines();
        iLgtVendorQuotedLineService.remove(new QueryWrapper<>(new LgtVendorQuotedLine().setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())));
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
            lgtVendorQuotedLines.forEach(lgtVendorQuotedLine -> {
                LgtBidingServiceImpl.setStartEndAddress(lgtVendorQuotedLine);
                lgtVendorQuotedLine.setQuotedLineId(IdGenrator.generate()).
                        setQuotedHeadId(vendorQuotedHead.getQuotedHeadId())
                        .setBidingId(bidingId)
                        .setRound(lgtBiding.getCurrentRound());
            });
            iLgtVendorQuotedLineService.saveBatch(lgtVendorQuotedLines);
            // ??????????????????????????????
            calculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines);

        }
    }

    public void calculationVendorQuotedLines(LgtBiding lgtBiding, List<LgtVendorQuotedLine> lgtVendorQuotedLines) {
        // ????????????????????????????????????  (???????????????-??????)
        Map<String, LatestGidailyRate> latestGidailyRateMap = iLgtBidingService.getLatestGidailyRateMap(lgtBiding);

        if (CompanyType.INSIDE.getValue().equals(lgtBiding.getBusinessModeCode())){
            // ???????????? ??????????????????????????????,?????????
            iLgtBidingService.insideCalculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines,latestGidailyRateMap);
        }else {
            // ??????????????? ??????????????????????????????,?????????
            iLgtBidingService.noInsideCalculationVendorQuotedLines(lgtBiding, lgtVendorQuotedLines,latestGidailyRateMap);
        }
    }

    @Override
    public PageInfo<LgtBidingDto> listPage(LgtBidingDto lgtBidingDto) {
        PageUtil.startPage(lgtBidingDto.getPageNum(), lgtBidingDto.getPageSize());

        //???????????????ID
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        Assert.notNull(loginAppUser,"???????????????????????????");
        Assert.notNull(loginAppUser.getUserType(),"???????????????????????????");
        if (UserType.VENDOR.name().equals(loginAppUser.getUserType())) {
            if(Objects.isNull(loginAppUser.getCompanyId())){
                return new PageInfo<>(Collections.emptyList());
            }
            lgtBidingDto.setVendorId(loginAppUser.getCompanyId());
        }

        List<LgtBidingDto> lgtBidingDtos = lgtBidingMapper.queryLgtBidingDto(lgtBidingDto);
        if(CollectionUtils.isNotEmpty(lgtBidingDtos)){
            lgtBidingDtos.forEach(this::updateEnrollEndDatetime);
        }
        return new PageInfo<>(lgtBidingDtos);
    }

    /**
     * ????????????????????????????????????
     * @param lgtBidingDtos
     */
    public void updateEnrollEndDatetime(LgtBidingDto lgtBidingDtos){
        if (BiddingProjectStatus.ACCEPT_BID.getValue().equals(lgtBidingDtos.getBidingStatus())) {
            Date enrollEndDatetime = lgtBidingDtos.getEnrollEndDatetime();
            if(!ObjectUtils.isEmpty(enrollEndDatetime) && enrollEndDatetime.compareTo(new Date()) < 0){
                /**
                 * ????????????????????????????????????
                 */
                lgtBidingDtos.setBidingStatus(BiddingProjectStatus.TENDER_ENDING.getValue());
                LgtBiding lgtBiding = new LgtBiding().setBidingId(lgtBidingDtos.getBidingId()).setBidingStatus(BiddingProjectStatus.TENDER_ENDING.getValue());
                iLgtBidingService.updateById(lgtBiding);
            }
        }
    }

    @Override
    public LgtVendorQuotedHeadDto getLgtVendorQuotedHeadByQuotedHeadId(Long quotedHeadId) {
        LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto = new LgtVendorQuotedHeadDto();
        LgtVendorQuotedHead lgtVendorQuotedHead = iLgtVendorQuotedHeadService.getById(quotedHeadId);
        Long bidingId = lgtVendorQuotedHead.getBidingId();
        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        Assert.notNull(lgtBiding,"???????????????????????????!");
        if(!LgtBidingServiceImpl.canSeeResultSet.contains(lgtBiding.getBidingStatus())){
            throw new BaseException("??????????????????????????????????????????");
        }
        // ?????????????????????
        setQuotedLineInfo(bidingId, lgtVendorQuotedHeadDto, lgtBiding, lgtVendorQuotedHead);
        return lgtVendorQuotedHeadDto;
    }

    @Override
    public LgtVendorQuotedHeadDto getLgtVendorQuotedHead(Long bidingId, Long vendorId) {
        LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto = new LgtVendorQuotedHeadDto();

        LgtBiding lgtBiding = iLgtBidingService.getById(bidingId);
        Assert.notNull(lgtBiding,"???????????????????????????!");
        lgtVendorQuotedHeadDto.setLgtBiding(lgtBiding);
        // ???????????????
        LgtVendorQuotedHead vendorQuotedHead = iLgtVendorQuotedHeadService.getOne(Wrappers.lambdaQuery(LgtVendorQuotedHead.class).
                eq(LgtVendorQuotedHead::getBidingId, bidingId).
                eq(LgtVendorQuotedHead::getVendorId,vendorId).
                eq(LgtVendorQuotedHead::getRound, lgtBiding.getCurrentRound()).last(" LIMIT 1"));
        lgtVendorQuotedHeadDto.setLgtVendorQuotedHead(vendorQuotedHead);

        // ?????????????????????
        setQuotedLineInfo(bidingId, lgtVendorQuotedHeadDto, lgtBiding, vendorQuotedHead);

        return lgtVendorQuotedHeadDto;
    }

    public void setQuotedLineInfo(Long bidingId, LgtVendorQuotedHeadDto lgtVendorQuotedHeadDto, LgtBiding lgtBiding, LgtVendorQuotedHead lgtVendorQuotedHead ) {
        // ???????????????
        lgtVendorQuotedHeadDto.setLgtBiding(lgtBiding);
        // ????????????
        List<LgtVendorQuotedLine> lgtVendorQuotedLines = null;
        if (null != lgtVendorQuotedHead) {
            lgtVendorQuotedLines = iLgtVendorQuotedLineService.list(new QueryWrapper<>(new LgtVendorQuotedLine().
                    setQuotedHeadId(lgtVendorQuotedHead.getQuotedHeadId()).
                    setRound(lgtBiding.getCurrentRound())));
        }
        // ????????????
        List<LgtBidRequirementLine> lgtBidRequirementLines = iLgtBidRequirementLineService.list(new QueryWrapper<>(new LgtBidRequirementLine().setBidingId(bidingId)));
        List<Integer> rowNums = lgtBidRequirementLines.stream().map(LgtBidRequirementLine::getRowNum).collect(Collectors.toList());
        rowNums.sort(Integer::compareTo);

        if(CollectionUtils.isEmpty(lgtVendorQuotedLines)){
            // ???????????????????????????
            if(CollectionUtils.isNotEmpty(lgtBidRequirementLines)){
                List<LgtVendorQuotedLine> finalLgtVendorQuotedLines = new ArrayList<>();
                lgtBidRequirementLines.forEach(lgtBidRequirementLine -> {
                    LgtVendorQuotedLine lgtVendorQuotedLine = new LgtVendorQuotedLine();
                    BeanCopyUtil.copyProperties(lgtVendorQuotedLine,lgtBidRequirementLine);
                    lgtVendorQuotedLine.setQuotedLineId(IdGenrator.generate());
                    lgtVendorQuotedLine.setBidingId(bidingId);
                    lgtVendorQuotedLine.setRound(lgtBiding.getCurrentRound());
                    lgtVendorQuotedLine.setPurchaseRemark(lgtBidRequirementLine.getComments());
                    if (null != lgtVendorQuotedHead) {
                        lgtVendorQuotedLine.setQuotedHeadId(lgtVendorQuotedHead.getQuotedHeadId());
                        lgtVendorQuotedLine.setVendorId(lgtVendorQuotedHead.getVendorId());
                        lgtVendorQuotedLine.setVendorCode(lgtVendorQuotedHead.getVendorCode());
                        lgtVendorQuotedLine.setVendorName(lgtVendorQuotedHead.getVendorName());
                    }
                    finalLgtVendorQuotedLines.add(lgtVendorQuotedLine);
                });
                lgtVendorQuotedLines = finalLgtVendorQuotedLines;

                // ???????????????
                if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
                    lgtVendorQuotedLines.forEach(LgtBidingServiceImpl::setStartEndAddress);
                    iLgtVendorQuotedLineService.saveBatch(lgtVendorQuotedLines);
                }

            }
        }
        if(CollectionUtils.isNotEmpty(lgtVendorQuotedLines)){
            List<LgtVendorQuotedLine> quotedLines = new ArrayList<>();
            // ?????????????????????
            Map<Integer, List<LgtVendorQuotedLine>> integerListMap = lgtVendorQuotedLines.stream().collect(Collectors.groupingBy(LgtVendorQuotedLine::getRowNum));
            rowNums.forEach(rowNum->{
                List<LgtVendorQuotedLine> lines = integerListMap.get(rowNum);
                if(CollectionUtils.isNotEmpty(lines)){
                    quotedLines.addAll(lines);
                }
            });
            lgtVendorQuotedHeadDto.setLgtVendorQuotedLines(quotedLines);
        }

        // ??????
        List<LgtBidShipPeriod> lgtBidShipPeriods = null;
        if (null != lgtVendorQuotedHead) {
            lgtBidShipPeriods = iLgtBidShipPeriodService.list(Wrappers.lambdaQuery(LgtBidShipPeriod.class).
                    eq(LgtBidShipPeriod::getQuotedHeadId, lgtVendorQuotedHead.getQuotedHeadId()).
                    eq(LgtBidShipPeriod::getRound,lgtBiding.getCurrentRound()));
            if(CollectionUtils.isEmpty(lgtBidShipPeriods)){
                // ?????????????????????????????????
                List<LgtBidShipPeriod> shipPeriods = new ArrayList<>();
                lgtBidRequirementLines.forEach(lgtBidRequirementLine -> {
                    LgtBidShipPeriod lgtBidShipPeriod = new LgtBidShipPeriod();
                    BeanCopyUtil.copyProperties(lgtBidShipPeriod,lgtBidRequirementLine);
                    lgtBidShipPeriod.setShipPeriodId(IdGenrator.generate());
                    lgtBidShipPeriod.setBidingId(bidingId);
                    lgtBidShipPeriod.setRound(lgtBiding.getCurrentRound());
                    if (null != lgtVendorQuotedHead) {
                        lgtBidShipPeriod.setQuotedHeadId(lgtVendorQuotedHead.getQuotedHeadId());
                        lgtBidShipPeriod.setVendorId(lgtVendorQuotedHead.getVendorId());
                        lgtBidShipPeriod.setVendorCode(lgtVendorQuotedHead.getVendorCode());
                        lgtBidShipPeriod.setVendorName(lgtVendorQuotedHead.getVendorName());
                    }
                    shipPeriods.add(lgtBidShipPeriod);
                });
                iLgtBidShipPeriodService.saveBatch(shipPeriods);
                lgtBidShipPeriods = shipPeriods;
            }
        }
        if(CollectionUtils.isNotEmpty(lgtBidShipPeriods)){
            // ????????????
            List<LgtBidShipPeriod> quotedLines = new ArrayList<>();
            // ?????????????????????
            Map<Integer, List<LgtBidShipPeriod>> integerListMap = lgtBidShipPeriods.stream().collect(Collectors.groupingBy(LgtBidShipPeriod::getRowNum));
            rowNums.forEach(rowNum->{
                List<LgtBidShipPeriod> lines = integerListMap.get(rowNum);
                if(CollectionUtils.isNotEmpty(lines)){
                    quotedLines.addAll(lines);
                }
            });
            lgtVendorQuotedHeadDto.setLgtBidShipPeriods(quotedLines);
        }

        // ????????????
        List<LgtFileConfig> lgtFileConfigs = iLgtFileConfigService.list(Wrappers.lambdaQuery(LgtFileConfig.class).
                eq(LgtFileConfig::getBidingId, bidingId));
        if(CollectionUtils.isNotEmpty(lgtFileConfigs) && null != lgtVendorQuotedHead){
            lgtFileConfigs.forEach(lgtFileConfig -> {
                Long requireId = lgtFileConfig.getRequireId();
                LgtVendorFile lgtVendorFile = iLgtVendorFileService.getOne(Wrappers.lambdaQuery(LgtVendorFile.class).
                        eq(LgtVendorFile::getRequireId, requireId).
                        eq(LgtVendorFile::getQuotedHeadId,lgtVendorQuotedHead.getQuotedHeadId()).
                        eq(LgtVendorFile::getRound, lgtBiding.getCurrentRound()).
                        last(" LIMIT 1"));
                if (null != lgtVendorFile) {
                    lgtFileConfig.setVendorDocId(lgtVendorFile.getDocId());
                    lgtFileConfig.setVendorFileName(lgtVendorFile.getFileName());
                }
            });
        }
        lgtVendorQuotedHeadDto.setLgtFileConfigs(lgtFileConfigs);
    }
}
