package com.midea.cloud.srm.price.estimate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.googlecode.aviator.AviatorEvaluator;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.price.costelement.AttributeType;
import com.midea.cloud.common.enums.price.costelement.Calculation;
import com.midea.cloud.common.enums.price.costelement.CalculationBasis;
import com.midea.cloud.common.enums.price.costelement.ElementType;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.NumberUtil;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.model.price.baseprice.entity.BasePrice;
import com.midea.cloud.srm.model.price.costelement.entity.CostElement;
import com.midea.cloud.srm.model.price.costelement.entity.FeatureFormula;
import com.midea.cloud.srm.model.price.costelement.entity.RateCalculation;
import com.midea.cloud.srm.model.price.estimate.dto.EstimateAttrLineDto;
import com.midea.cloud.srm.model.price.estimate.entity.EstimateAttrHead;
import com.midea.cloud.srm.model.price.estimate.entity.EstimateAttrLine;
import com.midea.cloud.srm.model.price.estimate.entity.EstimateFile;
import com.midea.cloud.srm.model.price.estimate.entity.EstimateHead;
import com.midea.cloud.srm.price.costelement.service.ICostElementService;
import com.midea.cloud.srm.price.costelement.service.IFeatureAttributeService;
import com.midea.cloud.srm.price.estimate.mapper.EstimateHeadMapper;
import com.midea.cloud.srm.price.estimate.service.IEstimateAttrHeadService;
import com.midea.cloud.srm.price.estimate.service.IEstimateAttrLineService;
import com.midea.cloud.srm.price.estimate.service.IEstimateFileService;
import com.midea.cloud.srm.price.estimate.service.IEstimateHeadService;
import com.midea.cloud.srm.price.model.service.IModelElementService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
*  <pre>
 *  ?????????????????? ???????????????
 * </pre>
*
* @author wangpr@meicloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-07-25 14:36:39
 *  ????????????:
 * </pre>
*/
@Service
public class EstimateHeadServiceImpl extends ServiceImpl<EstimateHeadMapper, EstimateHead> implements IEstimateHeadService {
    @Resource
    private BaseClient baseClient;
    @Resource
    private IEstimateFileService iEstimateFileService;
    @Resource
    private IEstimateAttrHeadService iEstimateAttrHeadService;
    @Resource
    private IEstimateAttrLineService iEstimateAttrLineService;
    @Resource
    private IModelElementService iModelElementService;
    @Resource
    private ICostElementService iCostElementService;
    @Resource
    private IFeatureAttributeService iFeatureAttributeService;
    @Override
    public PageInfo<EstimateHead> listPage(EstimateHead estimateHead) {
        PageUtil.startPage(estimateHead.getPageNum(),estimateHead.getPageSize());
        QueryWrapper<EstimateHead> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtil.notEmpty(estimateHead.getEstimateCode()),"ESTIMATE_CODE",estimateHead.getEstimateCode());
        queryWrapper.like(StringUtil.notEmpty(estimateHead.getEstimateName()),"ESTIMATE_NAME",estimateHead.getEstimateName());
        queryWrapper.eq(StringUtil.notEmpty(estimateHead.getMaterialId()),"MATERIAL_ID",estimateHead.getMaterialId());
        queryWrapper.eq(StringUtil.notEmpty(estimateHead.getCategoryId()),"CATEGORY_ID",estimateHead.getCategoryId());
        queryWrapper.eq(StringUtil.notEmpty(estimateHead.getCreationDate()),"CREATION_DATE",estimateHead.getCreationDate());
        queryWrapper.eq(StringUtil.notEmpty(estimateHead.getNuclearUserName()),"NUCLEAR_USER_NAME",estimateHead.getNuclearUserName());
        queryWrapper.orderByDesc("LAST_UPDATE_DATE");
        List<EstimateHead> estimateHeadList = this.list(queryWrapper);
        return new PageInfo<>(estimateHeadList);
    }

    @Override
    @Transactional
    public Long add(EstimateHead estimateHead) {

        Long estimateHeadId = IdGenrator.generate();
        // ????????????????????????
        this.saveEstimateHead(estimateHead, estimateHeadId);
        // ????????????????????????
        this.saveEstimateFile(estimateHead, estimateHeadId);
        // ???????????????????????????
        this.updateOrsaveEstimateAttrHead(estimateHead,estimateHeadId);
        return estimateHeadId;
    }

    @Override
    @Transactional
    public Long modify(EstimateHead estimateHead) {
        Long estimateHeadId = estimateHead.getEstimateHeadId();
        Assert.notNull(estimateHeadId,"estimateHeadId????????????");
        this.updateById(estimateHead);
        // ????????????????????????
        this.saveEstimateFile(estimateHead, estimateHeadId);
        // ???????????????????????????
        this.updateOrsaveEstimateAttrHead(estimateHead,estimateHeadId);
        return estimateHeadId;
    }

    @Transactional
    public void updateOrsaveEstimateAttrHead(EstimateHead estimateHead,Long estimateHeadId) {
        // ??????????????????
        boolean flag = true;
        BigDecimal sumPrice = BigDecimal.ZERO;
        List<EstimateAttrHead> estimateAttrHeadList = estimateHead.getEstimateAttrHeadList();
        // ??????????????????
        ArrayList<EstimateAttrHead> estimateAttrHeadArrayList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(estimateAttrHeadList)){
            // ????????????????????????
            List<EstimateAttrHead> rateCalculationList = new ArrayList<>();
            // ????????????????????????
            for(EstimateAttrHead estimateAttrHead : estimateAttrHeadList){
                // ????????????????????????
                CostElement costElement = iCostElementService.get(estimateAttrHead.getCostElementId());
                if(ElementType.FEE.getKey().equals(costElement.getElementType()) &&
                        Calculation.CALCULATED_BY_RATE.getKey().equals(costElement.getCalculation())){
                    // ??????????????????????????????
                    rateCalculationList.add(estimateAttrHead);
                }else {
                    // ????????????
                    BigDecimal price = this.estimatedUnitPrice(estimateAttrHead, costElement);
                    if(BigDecimal.ZERO.equals(price)){
                        flag = false;
                    }else {
                        sumPrice = sumPrice.add(price);
                    }

                    // ????????????
                    estimateAttrHead.setEstimateHeadId(estimateHeadId);
                    estimateAttrHeadArrayList.add(estimateAttrHead);
                }

            }

            // ?????????????????????????????????
            if(CollectionUtils.isNotEmpty(rateCalculationList)){
                for(EstimateAttrHead estimateAttrHead : rateCalculationList){
                    // ????????????????????????
                    CostElement costElement = iCostElementService.get(estimateAttrHead.getCostElementId());
                    RateCalculation rateCalculation = costElement.getRateCalculation();
                    // ??????????????????
                    BigDecimal basePrice = this.getCalculationBase(estimateAttrHeadList,rateCalculation);
                    // ??????
                    BigDecimal rate = rateCalculation.getRate();
                    if(!BigDecimal.ZERO.equals(basePrice)){
                        BigDecimal price = rate.multiply(basePrice);
                        if(StringUtil.notEmpty(estimateAttrHead.getEnlargeFactor())){
                            // ????????????????????????
                            price = estimateAttrHead.getEnlargeFactor().multiply(price);
                        }
                        sumPrice = sumPrice.add(price);
                        estimateAttrHead.setEstimate(price);
                    }

                    // ????????????
                    estimateAttrHead.setEstimateHeadId(estimateHeadId);
                    estimateAttrHeadArrayList.add(estimateAttrHead);
                }
            }

            if(CollectionUtils.isNotEmpty(estimateAttrHeadList)){
                // ???????????????????????????
                iEstimateAttrHeadService.remove(new QueryWrapper<>(new EstimateAttrHead().setEstimateHeadId(estimateHeadId)));
                estimateAttrHeadList.forEach(estimateAttrHead -> {
                    Long attrHeadId = estimateAttrHead.getAttrHeadId();
                    if(StringUtil.isEmpty(attrHeadId)){
                        // ??????
                        Long id = IdGenrator.generate();
                        estimateAttrHead.setAttrHeadId(id);
                        iEstimateAttrHeadService.save(estimateAttrHead);
                        List<EstimateAttrLine> estimateAttrLines = estimateAttrHead.getEstimateAttrLines();
                        if(CollectionUtils.isNotEmpty(estimateAttrLines)){
                            estimateAttrLines.forEach(estimateAttrLine -> {
                                estimateAttrLine.setAttrHeadId(id);
                                estimateAttrLine.setAttrLineId(IdGenrator.generate());
                                iEstimateAttrLineService.save(estimateAttrLine);
                            });
                        }
                    }else {
                        // ??????
                        iEstimateAttrLineService.remove(new QueryWrapper<>(new EstimateAttrLine().setAttrHeadId(attrHeadId)));
                        // ??????
                        Long id = IdGenrator.generate();
                        estimateAttrHead.setAttrHeadId(id);
                        iEstimateAttrHeadService.save(estimateAttrHead);
                        List<EstimateAttrLine> estimateAttrLines = estimateAttrHead.getEstimateAttrLines();
                        if(CollectionUtils.isNotEmpty(estimateAttrLines)){
                            estimateAttrLines.forEach(estimateAttrLine -> {
                                estimateAttrLine.setAttrHeadId(id);
                                estimateAttrLine.setAttrLineId(IdGenrator.generate());
                                iEstimateAttrLineService.save(estimateAttrLine);
                            });
                        }

                    }
                });

                iEstimateAttrHeadService.remove(new QueryWrapper<>(new EstimateAttrHead().setEstimateHeadId(estimateHeadId)));
                iEstimateAttrHeadService.saveBatch(estimateAttrHeadArrayList);
            }

            // ??????????????????
            if(flag){
                estimateHead.setEstimatedTotalPrice(sumPrice);
                this.updateById(estimateHead);
            }
        }
    }

    /**
     * ????????????
     * @param estimateAttrHead
     * @param costElement
     * @return
     */
    public BigDecimal estimatedUnitPrice(EstimateAttrHead estimateAttrHead, CostElement costElement) {
        BigDecimal finallyPrice = BigDecimal.ZERO;
        // ???????????????
        List<EstimateAttrLineDto> attributeValueList = this.getAttributeValue(estimateAttrHead);
        if (CollectionUtils.isNotEmpty(attributeValueList)) {
            // ?????????????????????????????????
            BigDecimal estimate = this.calculateTheAmount(estimateAttrHead, costElement, attributeValueList);
            // ????????????
//            BigDecimal basePrice = this.getBasePrice(estimateAttrHead, costElement);
            BigDecimal basePrice = estimateAttrHead.getBasePrice();
            // ??????????????????
            BigDecimal price = this.calculateThePrice(estimateAttrHead, costElement, attributeValueList, estimate, basePrice);
            if(!BigDecimal.ZERO.equals(price)){
                estimateAttrHead.setEstimate(price);
                finallyPrice = price;
            }
        }
        return finallyPrice;
    }

    public BigDecimal getCalculationBase(List<EstimateAttrHead> estimateAttrHeadList,RateCalculation rateCalculation) {
        BigDecimal basePrice = BigDecimal.ZERO;
        // ????????????
        String calculationBasis = rateCalculation.getCalculationBasis();
        // ????????????????????????
        if(CalculationBasis.MATERIAL.getKey().equals(calculationBasis)){
            // ????????????
            basePrice = this.getBasrPrice(estimateAttrHeadList, ElementType.MATERIAL.getKey());
        }else if(CalculationBasis.CRAFT.getKey().equals(calculationBasis)){
            // ????????????
            basePrice = this.getBasrPrice(estimateAttrHeadList,ElementType.CRAFT.getKey());
        }else if (CalculationBasis.MATERIAL_CRAFT.getKey().equals(calculationBasis)){
            BigDecimal materialPrice = this.getBasrPrice(estimateAttrHeadList,ElementType.MATERIAL.getKey());
            BigDecimal craftPrice = this.getBasrPrice(estimateAttrHeadList,ElementType.CRAFT.getKey());
            basePrice = craftPrice.add(materialPrice);
        }
        return basePrice;
    }

    public BigDecimal getBasrPrice(List<EstimateAttrHead> estimateAttrHeadList , String elementType) {
        BigDecimal basePrice = BigDecimal.ZERO;
        for (EstimateAttrHead estimateAttrHeadDto : estimateAttrHeadList){
            if(elementType.equals(estimateAttrHeadDto.getElementType())){
                BigDecimal estimateDto = estimateAttrHeadDto.getEstimate();
                if(StringUtil.notEmpty(estimateDto) && !BigDecimal.ZERO.equals(estimateDto)){
                    basePrice = basePrice.add(estimateDto);
                }
            }
        }
        return basePrice;
    }

    public BigDecimal calculateThePrice(EstimateAttrHead estimateAttrHead, CostElement costElement, List<EstimateAttrLineDto> attributeValueList, BigDecimal estimate, BigDecimal basePrice) {
        BigDecimal price = BigDecimal.ZERO;
        EstimateAttrLineDto estimateAttrLineDto1 = new EstimateAttrLineDto();
        estimateAttrLineDto1.setAttributeName("[??????]");
        if (StringUtil.notEmpty(basePrice)) {
            estimateAttrLineDto1.setAttributeValue(basePrice.toString());
        }else {
            estimateAttrLineDto1.setAttributeValue(0+"");
        }
        EstimateAttrLineDto estimateAttrLineDto2 = new EstimateAttrLineDto();
        estimateAttrLineDto2.setAttributeName("[??????]");
        estimateAttrLineDto2.setAttributeValue(estimate.toString());
        attributeValueList.add(estimateAttrLineDto1);
        attributeValueList.add(estimateAttrLineDto2);
        // ????????????
        List<FeatureFormula> priceFormulasList = costElement.getPriceFormulasList();
        if(CollectionUtils.isNotEmpty(priceFormulasList)){
            for(FeatureFormula featureFormula : priceFormulasList){
                // ??????????????????
                String appCondCode = featureFormula.getAppCondCode();
                // ????????????????????????
                appCondCode = this.formulaAttributeToValue(attributeValueList, appCondCode);
                // ??????????????????
                try {
                    if((boolean) AviatorEvaluator.execute(appCondCode)){
                        // ??????????????????, ????????????
                        String formula = featureFormula.getFormula();
                        // ??????????????????
                        List<String> bracketsList = StringUtil.getBracketsList(formula);
                        if(bracketsList.contains("[??????]") && StringUtil.isEmpty(basePrice)){
                            throw new BaseException("???????????????"+estimateAttrHead.getElementCode()+"?????????");
                        }
                        formula = this.formulaAttributeToValue(attributeValueList, formula);
                        // ???????????????
                        try {
                            BigDecimal decimal = NumberUtil.formulaToBigDecimal(formula, 4);
                            if(StringUtil.notEmpty(estimateAttrHead.getEnlargeFactor())){
                                // ????????????????????????
                                price = estimateAttrHead.getEnlargeFactor().multiply(decimal);
                            }else {
                                price = decimal;
                            }
                        } catch (Exception e) {
                            throw new BaseException("????????????????????????: "+featureFormula.getFormula());
                        }
                        break;
                    }
                } catch (Exception e) {
                    throw new BaseException("????????????????????????: "+featureFormula.getAppCondName());
                }
            }
        }
        if(BigDecimal.ZERO.equals(estimate)){
            throw new BaseException(costElement.getElementCode()+"??????????????????????????????");
        }
        return price;
    }

    public BigDecimal getBasePrice(EstimateAttrHead estimateAttrHead, CostElement costElement) {
        // ??????
        BigDecimal basePrice = BigDecimal.ZERO;
        // ????????????
        StringBuffer crucialAttribute = new StringBuffer();
        List<EstimateAttrLine> estimateAttrLines = estimateAttrHead.getEstimateAttrLines();
        if(CollectionUtils.isNotEmpty(estimateAttrLines)){
            // ??????????????????
            for(EstimateAttrLine estimateAttrLine : estimateAttrLines){
                if(StringUtil.notEmpty(estimateAttrLine.getAttributeValue()) && YesOrNo.YES.getValue().equals(estimateAttrLine.getCrucialFlag())){
                    crucialAttribute.append("[").append(estimateAttrLine.getAttributeValue()).append("]");
                }
            }
        }
        // ????????????
        List<BasePrice> basePricesList = costElement.getBasePricesList();
        if(CollectionUtils.isNotEmpty(basePricesList)){
            for(BasePrice basePriceDto : basePricesList){
                if(crucialAttribute.toString().equals(basePriceDto.getAttributeValueCombination())){
                    basePrice = basePriceDto.getBasePrice();
                    break;
                }
            }
        }
        return basePrice;
    }

    public BigDecimal calculateTheAmount(EstimateAttrHead estimateAttrHead, CostElement costElement, List<EstimateAttrLineDto> attributeValueList) {
        BigDecimal estimate = BigDecimal.ZERO;
        List<FeatureFormula> dosageFormulasList = costElement.getDosageFormulasList();
        if(CollectionUtils.isNotEmpty(dosageFormulasList)){
            for(FeatureFormula featureFormula : dosageFormulasList){
                String appCondCode = featureFormula.getAppCondCode();
                // ??????????????????????????????null
                this.checkArreIsNull(attributeValueList, appCondCode);
                // ????????????????????????
                appCondCode = this.formulaAttributeToValue(attributeValueList, appCondCode);
                // ??????????????????
                try {
                    if((boolean) AviatorEvaluator.execute(appCondCode)){
                        // ??????????????????, ????????????
                        String formula = featureFormula.getFormula();
                        // ??????????????????????????????null
                        this.checkArreIsNull(attributeValueList, formula);
                        formula = this.formulaAttributeToValue(attributeValueList, formula);
                        // ???????????????
                        try {
                            estimate = NumberUtil.formulaToBigDecimal(formula,4);
                        } catch (Exception e) {
                            throw new BaseException("????????????????????????: "+featureFormula.getFormula());
                        }
                        break;
                    }
                } catch (Exception e) {
                    throw new BaseException("????????????????????????: "+featureFormula.getAppCondName());
                }
            }
        }
        return estimate;
    }

    public String formulaAttributeToValue(List<EstimateAttrLineDto> attributeValueList, String appCondCode) {
        for(EstimateAttrLineDto estimateAttrLineDto : attributeValueList){
            String attributeName = estimateAttrLineDto.getAttributeName();
            String attributeValue = estimateAttrLineDto.getAttributeValue();
            appCondCode = StringUtils.replace(appCondCode, attributeName, attributeValue);
        }
        return appCondCode;
    }

    /**
     * ??????????????????
     * @param estimateAttrHead
     * @return
     */
    public List<EstimateAttrLineDto> getAttributeValue(EstimateAttrHead estimateAttrHead) {
        List<EstimateAttrLine> estimateAttrLines = estimateAttrHead.getEstimateAttrLines();
        List<EstimateAttrLineDto> estimateAttrLine1 = new ArrayList<>(); // ??????-???
        List<EstimateAttrLineDto> estimateAttrLine2 = new ArrayList<>(); // ??????-??????
        if(CollectionUtils.isNotEmpty(estimateAttrLines)){
            estimateAttrLines.forEach(estimateAttrLine -> {
                String attributeName = estimateAttrLine.getAttributeName();
                StringBuffer stringBuffer = new StringBuffer();
                // ???????????????
                stringBuffer.append("[").append(attributeName).append("]");
                // ?????????
                String attributeValue = estimateAttrLine.getAttributeValue();
                if (StringUtil.notEmpty(attributeValue)) {
                    String attributeType = estimateAttrLine.getAttributeType();
                    EstimateAttrLineDto estimateAttrLineDto = new EstimateAttrLineDto();
                    estimateAttrLineDto.setAttributeType(attributeType);
                    if(!AttributeType.FORMULA.getKey().equals(attributeType)){
                        // ????????????
                        estimateAttrLineDto.setAttributeName(stringBuffer.toString());
                        if (!StringUtil.isDigit(attributeValue)) {
                            int hashCode = attributeValue.hashCode();
                            estimateAttrLineDto.setAttributeValue(String.valueOf(hashCode));
                        }else {
                            estimateAttrLineDto.setAttributeValue(attributeValue);
                        }
                        estimateAttrLine1.add(estimateAttrLineDto);
                    }else {
                        // ???????????????
                        estimateAttrLineDto.setAttributeName(stringBuffer.toString());
                        estimateAttrLineDto.setAttributeValue(attributeValue);
                        estimateAttrLine2.add(estimateAttrLineDto);
                    }
                }
            });
            // ??????????????????
            if(CollectionUtils.isNotEmpty(estimateAttrLine2) && CollectionUtils.isNotEmpty(estimateAttrLine1)){
                // ??????????????????????????????????????????
                estimateAttrLine2.forEach(estimateAttrLineDto -> {
                    // ????????????
                    this.checkArreIsNull(estimateAttrLine1, estimateAttrLineDto.getAttributeValue());
                });

                estimateAttrLine2.forEach(estimateAttrLineDto -> {
                    AtomicReference<String> attributeValue = new AtomicReference<>(estimateAttrLineDto.getAttributeValue());
                    // ????????????????????????
                    estimateAttrLine1.forEach(estimateAttrLineDto1 -> {
                        String attributeName = estimateAttrLineDto1.getAttributeName();
                        String attributeValue1 = estimateAttrLineDto1.getAttributeValue();
                        String str = StringUtils.replace(attributeValue.get(), attributeName, attributeValue1);
                        attributeValue.set(str);
                    });
                    // ??????????????????
                    try {
                        Object result = AviatorEvaluator.execute(attributeValue.get());
                        estimateAttrLineDto.setAttributeValue(result.toString());
                    } catch (Exception e) {
                        throw new BaseException("????????????????????????"+estimateAttrLineDto.getAttributeValue());
                    }
                });
                estimateAttrLine1.addAll(estimateAttrLine2);
            }
        }
        return estimateAttrLine1;
    }

    /**
     * ????????????????????????????????????
     * @param estimateAttrLine1
     * @param attributeValue
     */
    public void checkArreIsNull(List<EstimateAttrLineDto> estimateAttrLine1, String attributeValue) {
        // ??????????????????
        List<String> bracketsList = StringUtil.getBracketsList(attributeValue);
        ArrayList<String> attrList = new ArrayList<>();
        estimateAttrLine1.forEach(temp->attrList.add(temp.getAttributeName()));
        StringBuffer errorMsg = new StringBuffer();
        // ???????????????????????????
        if (CollectionUtils.isNotEmpty(bracketsList)) {
            bracketsList.forEach(bracket->{
                if(!attrList.contains(bracket)){
                    errorMsg.append(bracket);
                }
            });
        }
        if(errorMsg.length() > 1){
            throw new BaseException("?????????????????????: "+ errorMsg.toString());
        }
    }

    public void saveEstimateFile(EstimateHead estimateHead, Long estimateHeadId) {
        List<EstimateFile> estimateFileList = estimateHead.getEstimateFileList();
        if(CollectionUtils.isNotEmpty(estimateFileList)){
            for(EstimateFile estimateFile : estimateFileList){
                estimateFile.setEstimateHeadId(estimateHeadId);
                estimateFile.setEstimateFileId(IdGenrator.generate());
            }
            // ??????????????????????????????
            iEstimateFileService.remove(new QueryWrapper<>(new EstimateFile().setEstimateHeadId(estimateHeadId)));
            iEstimateFileService.saveBatch(estimateFileList);
        }
    }

    public void saveEstimateHead(EstimateHead estimateHead, Long estimateHeadId) {
        estimateHead.setEstimateHeadId(estimateHeadId);
         estimateHead.setEstimateCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PRICE_ESTIMATE_CODE));
        this.save(estimateHead);
    }

    @Override
    public EstimateHead get(Long estimateHeadId) {
        Assert.notNull(estimateHeadId, LocaleHandler.getLocaleMsg("???????????????????????????")+": estimateHeadId");
        EstimateHead estimateHead = this.getById(estimateHeadId);
        List<EstimateFile> estimateFileList = iEstimateFileService.list(new QueryWrapper<>(new EstimateFile().setEstimateHeadId(estimateHeadId)));
        estimateHead.setEstimateFileList(estimateFileList);
        List<EstimateAttrHead> estimateAttrHeadList = iEstimateAttrHeadService.list(new QueryWrapper<>(new EstimateAttrHead().setEstimateHeadId(estimateHeadId)));
        if(CollectionUtils.isNotEmpty(estimateAttrHeadList)){
            estimateAttrHeadList.forEach(estimateAttrHead -> {
                List<EstimateAttrLine> estimateAttrLineList = iEstimateAttrLineService.list(new QueryWrapper<>(new EstimateAttrLine().setAttrHeadId(estimateAttrHead.getAttrHeadId())));
                estimateAttrHead.setEstimateAttrLines(estimateAttrLineList);
            });
        }
        estimateHead.setEstimateAttrHeadList(estimateAttrHeadList);
        return estimateHead;
    }
}
