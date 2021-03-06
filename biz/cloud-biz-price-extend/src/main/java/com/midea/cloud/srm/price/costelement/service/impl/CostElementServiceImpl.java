package com.midea.cloud.srm.price.costelement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.googlecode.aviator.AviatorEvaluator;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.price.costelement.*;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.model.price.baseprice.entity.BasePrice;
import com.midea.cloud.srm.model.price.costelement.dto.AttrKeyValue;
import com.midea.cloud.srm.model.price.costelement.entity.CostElement;
import com.midea.cloud.srm.model.price.costelement.entity.FeatureAttribute;
import com.midea.cloud.srm.model.price.costelement.entity.FeatureFormula;
import com.midea.cloud.srm.model.price.costelement.entity.RateCalculation;
import com.midea.cloud.srm.price.baseprice.service.IBasePriceService;
import com.midea.cloud.srm.price.costelement.mapper.CostElementMapper;
import com.midea.cloud.srm.price.costelement.service.ICostElementService;
import com.midea.cloud.srm.price.costelement.service.IFeatureAttributeService;
import com.midea.cloud.srm.price.costelement.service.IFeatureFormulaService;
import com.midea.cloud.srm.price.costelement.service.IRateCalculationService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
*  <pre>
 *  ??????????????? ???????????????
 * </pre>
*
* @author wangpr@meicloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-07-25 13:58:19
 *  ????????????:
 * </pre>
*/
@Service
public class CostElementServiceImpl extends ServiceImpl<CostElementMapper, CostElement> implements ICostElementService {

    @Resource
    private BaseClient baseClient;
    @Resource
    private IFeatureAttributeService iFeatureAttributeService;
    @Resource
    private IFeatureFormulaService iFeatureFormulaService;
    @Resource
    private IRateCalculationService iRateCalculationService;
    @Resource
    private IBasePriceService iBasePriceService;

    @Override
    public PageInfo<CostElement> listPage(CostElement costElement) {
        // ??????
        PageUtil.startPage(costElement.getPageNum(), costElement.getPageSize());
        // ??????
        List<CostElement> costElements = this.baseMapper.listPage(costElement);
        return new PageInfo<>(costElements);
    }

    @Override
    @Transactional
    public Long updateOrSave(CostElement costElement) {
        // ????????????ID
        Long costElementId = null;
        if (StringUtil.notEmpty(costElement.getCostElementId())) {
            costElementId = costElement.getCostElementId();
            // ????????????????????????
            this.updateById(costElement);
        }else {
            costElementId = IdGenrator.generate();
            // ????????????????????????
            this.saveCostElement(costElement, costElementId);
        }
        if(ElementType.FEE.getKey().equals(costElement.getElementType()) &&
                Calculation.CALCULATED_BY_RATE.getKey().equals(costElement.getCalculation())){
            // ??????????????????
            this.saveRateCalculation(costElement, costElementId);
        }else {
            // ??????????????????
            List<String> attributeNameList = new ArrayList<>();
            // ??????????????????
            ArrayList<String> numberAttrList = new ArrayList<>();
            // ?????????Map
            ArrayList<AttrKeyValue> enumKeyValueList = new ArrayList<>();
            // ??????????????????
            this.saveFeatureAttribute(enumKeyValueList,numberAttrList,costElement, costElementId,attributeNameList);
            // ??????????????????
            this.saveDosageFormula(enumKeyValueList,numberAttrList,costElement, costElementId,attributeNameList);
            // ????????????
            this.savePriceFormula(enumKeyValueList,numberAttrList,costElement, costElementId,attributeNameList);
            // ??????
            this.saveBasePrice(costElement);
        }
        return costElementId;
    }

    @Transactional
    public void saveBasePrice(CostElement costElement) {
        List<BasePrice> basePriceList = costElement.getBasePricesList();
        if(CollectionUtils.isNotEmpty(basePriceList)){
            // ????????????
            String elementCode = costElement.getElementCode();
            int i = 1;
            // ???????????????????????????, ?????????
            iBasePriceService.remove(new QueryWrapper<>(new BasePrice().setElementCode(elementCode)));
            for(BasePrice basePrice: basePriceList){
                basePrice.setBasePriceId(IdGenrator.generate());
                // ??????
                basePrice.setElementCode(elementCode);
                basePrice.setPriceVersion(costElement.getElementVersion());
                basePrice.setElementName(costElement.getElementName());
                basePrice.setElementType(costElement.getElementType());
                // ??????????????????
                String num = String.format("%05d", i);
                basePrice.setCombinationCode(elementCode+num);
                basePrice.setStatus(CostElementStatus.DRAFT.getKey());
                basePrice.setDisplayCategory(DisplayCategory.SUB_TABLE.getKey());
                i++;
            }
            iBasePriceService.saveBatch(basePriceList);
        }
    }

    public void savePriceFormula(ArrayList<AttrKeyValue> enumKeyValueList,ArrayList<String> numberAttrList,CostElement costElement, Long costElementId,List<String> attributeNameList) {
        // ????????????
        List<FeatureAttribute> featureAttributeList = costElement.getFeatureAttributeList();
        List<FeatureFormula> priceFormulaList = costElement.getPriceFormulasList();
        if(CollectionUtils.isNotEmpty(priceFormulaList)){
            for (FeatureFormula featureFormula : priceFormulaList){
                // ????????????????????????
                featureFormula.setFormulaType(FormulaType.FORMULA_TYPE_1.getKey());
                featureFormula.setFeatureFormulaId(IdGenrator.generate());
                featureFormula.setCostElementId(costElementId);
                this.saveFormula(featureAttributeList,numberAttrList,costElementId, featureFormula,attributeNameList);
            }
            // ????????????????????????
            QueryWrapper<FeatureFormula> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("COST_ELEMENT_ID",costElementId);
            queryWrapper.eq("FORMULA_TYPE",FormulaType.FORMULA_TYPE_1.getKey());
            iFeatureFormulaService.remove(queryWrapper);
            iFeatureFormulaService.saveBatch(priceFormulaList);
        }
    }

    public void saveDosageFormula(ArrayList<AttrKeyValue> enumKeyValueList, ArrayList<String> numberAttrList,CostElement costElement, Long costElementId,List<String> attributeNameList) {
        // ????????????
        List<FeatureAttribute> featureAttributeList = costElement.getFeatureAttributeList();
        List<FeatureFormula> dosageFormulaList = costElement.getDosageFormulasList();
        if(CollectionUtils.isNotEmpty(dosageFormulaList)){
            for(FeatureFormula featureFormula : dosageFormulaList){
                // ????????????????????????
                featureFormula.setFeatureFormulaId(IdGenrator.generate());
                featureFormula.setCostElementId(costElementId);
                featureFormula.setFormulaType(FormulaType.FORMULA_TYPE_0.getKey());
                this.saveFormula(featureAttributeList,numberAttrList,costElementId, featureFormula,attributeNameList);
            }
            // ????????????????????????
            QueryWrapper<FeatureFormula> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("COST_ELEMENT_ID",costElementId);
            queryWrapper.eq("FORMULA_TYPE",FormulaType.FORMULA_TYPE_0.getKey());
            iFeatureFormulaService.remove(queryWrapper);
            iFeatureFormulaService.saveBatch(dosageFormulaList);
        }
    }

    /**
     *
     * @param featureAttributeList ????????????
     * @param numberAttrList
     * @param costElementId
     * @param featureFormula
     * @param attributeNameList
     */
    public void saveFormula(List<FeatureAttribute> featureAttributeList,ArrayList<String> numberAttrList,Long costElementId, FeatureFormula featureFormula,List<String> attributeNameList) {
        String appCondName = featureFormula.getAppCondName();
        String appCondCode = featureFormula.getAppCondCode();
        String formula = featureFormula.getFormula();
        featureFormula.setCostElementId(costElementId);
        featureFormula.setFeatureFormulaId(IdGenrator.generate());
        featureFormula.setFormula(StringUtils.isNotEmpty(formula)? formula.replaceAll(" ",""):"");
        // ??????????????????
        if (StringUtil.notEmpty(appCondName)) {
            this.checkFormula(appCondCode,appCondName,attributeNameList);
        }
        // ????????????
        if (StringUtil.notEmpty(formula)) {
            String formulaCode = formulaConversion(featureAttributeList, formula);
            this.checkFormula(numberAttrList,formulaCode,formula,attributeNameList);
        }
    }

    public String formulaConversion(List<FeatureAttribute> featureAttributeList, String formula) {
        Map<String, String> featureAttributeMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(featureAttributeList)){
            // ????????????
            featureAttributeMap = featureAttributeList.stream().filter(featureAttribute -> AttributeType.FORMULA.getKey().equals(featureAttribute.getAttributeType())).
                    collect(Collectors.toMap(featureAttribute -> "[" + featureAttribute.getAttributeName() + "]", FeatureAttribute::getAttributeValue, (k1, k2) -> k1));
        }
        AtomicReference<String> formulaAtomicReference = new AtomicReference<String>(formula);
        /**
         * ?????????????????????????????????????????????,?????????????????????
         */
        if(null != featureAttributeMap && !featureAttributeMap.isEmpty()){
            featureAttributeMap.forEach((name, value) -> {
                String replaceAll = StringUtils.replace(formulaAtomicReference.get(),name,value);
                formulaAtomicReference.set(replaceAll);
            });
        }
        formula = formulaAtomicReference.get();
        return formula;
    }

    public static void main(String[] args) {
        String str = "aaffg";
        System.out.println(str.replace("a","b"));
        System.out.println(StringUtils.replace(str,"a","b"));
    }


    public void checkFormula(ArrayList<String> numberAttrList, String appCondCode,String formulaName,List<String> attributeNameList) {
        numberAttrList.add("[??????]");
        numberAttrList.add("[??????]");
        if (StringUtil.notEmpty(appCondCode)) {
            AtomicReference<String> str = new AtomicReference<>(appCondCode);
            // ?????????????????????
            List<String> bracketsList = StringUtil.getBracketsList(appCondCode);
            if(CollectionUtils.isNotEmpty(bracketsList)){
                bracketsList.forEach(temp -> {
                    String s = StringUtils.replace(str.get(), temp, "1");
                    str.set(s);
                    // ?????????????????????????????????
                    if(!numberAttrList.contains(temp)){
                        throw new BaseException(formulaName + ": ?????????????????????????????????");
                    }
                });
            }
            try {
                // ????????????????????????
                AviatorEvaluator.execute(str.get());
            } catch (Exception e) {
                throw new BaseException(formulaName + "??????????????????");
            }
        }
    }

    public void checkFormula(String appCondCode,String formulaName,List<String> attributeNameList) {
        attributeNameList.add("[??????]");
        attributeNameList.add("[??????]");
        if (StringUtil.notEmpty(appCondCode)) {
            AtomicReference<String> str = new AtomicReference<>(appCondCode);
            // ?????????????????????
            List<String> bracketsList = StringUtil.getBracketsList(appCondCode);
            if(CollectionUtils.isNotEmpty(bracketsList)){
                bracketsList.forEach(temp -> {
                    String s = StringUtils.replace(str.get(), temp, "1");
                    str.set(s);
                    // ??????????????????????????????
                    if(!attributeNameList.contains(temp)){
                        throw new BaseException(formulaName + LocaleHandler.getLocaleMsg("????????????????????????"));
                    }
                });
            }
            try {
                // ????????????????????????
                AviatorEvaluator.compile(str.get());
            } catch (Exception e) {
                throw new BaseException(formulaName + "??????????????????");
            }
        }
    }

    public void saveFeatureAttribute(ArrayList<AttrKeyValue> enumKeyValueList, ArrayList<String> numberAttrList,CostElement costElement, Long costElementId,List<String> attributeNameList) {
        // ????????????
        List<FeatureAttribute> featureAttributeList = costElement.getFeatureAttributeList();
        HashSet<Object> hashSet = new HashSet<>();
        StringBuffer crucialAttributes = new StringBuffer();
        if(CollectionUtils.isNotEmpty(featureAttributeList)){
            featureAttributeList.forEach(featureAttribute->{
                // ????????????
                Assert.notNull(featureAttribute.getAttributeName(), LocaleHandler.getLocaleMsg("???????????????????????????")+": AttributeName");
                Assert.notNull(featureAttribute.getAttributeType(), LocaleHandler.getLocaleMsg("???????????????????????????")+": AttributeType");
                // ???????????????????????????
                if(!hashSet.add(featureAttribute.getAttributeName())){
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
                }
                // ????????????
                if(AttributeType.ENUM.getKey().equals(featureAttribute.getAttributeType())){
                    String attributeValue = featureAttribute.getAttributeValue();
                    featureAttribute.setAttributeValue(attributeValue.replaceAll(" ","").replaceAll("???",","));
                    if (featureAttribute.getAttributeValue().contains(",")) {
                        List<String> list = Arrays.asList(featureAttribute.getAttributeValue().split(","));
                        list.forEach(temp->{
                            AttrKeyValue attrKeyValue = new AttrKeyValue();
                            attrKeyValue.setKey(temp);
                            attrKeyValue.setValue(temp.hashCode());
                            enumKeyValueList.add(attrKeyValue);
                        });
                    }else {
                        AttrKeyValue attrKeyValue = new AttrKeyValue();
                        attrKeyValue.setKey(featureAttribute.getAttributeValue());
                        attrKeyValue.setValue(featureAttribute.getAttributeValue().hashCode());
                        enumKeyValueList.add(attrKeyValue);
                    }
                }
                featureAttribute.setFeatureAttributeId(IdGenrator.generate());
                featureAttribute.setCostElementId(costElementId);
                // ????????????
                if(YesOrNo.YES.getValue().equals(featureAttribute.getCrucialFlag())){
                    crucialAttributes.append("[").append(featureAttribute.getAttributeName()).append("]");
                }
                if (StringUtil.notEmpty(featureAttribute.getAttributeName())) {
                    // ???????????????
                    StringBuffer str = new StringBuffer();
                    str.append("[").append(featureAttribute.getAttributeName()).append("]");
                    attributeNameList.add(str.toString());
                    if(AttributeType.NUMBER.getKey().equals(featureAttribute.getAttributeType())){
                        numberAttrList.add(str.toString());
                    }
                    str.setLength(0);
                }
            });
            featureAttributeList.forEach(featureAttribute -> {
                // ????????????
                if(AttributeType.FORMULA.getKey().equals(featureAttribute.getAttributeType())){
                    this.checkFormula(numberAttrList, featureAttribute.getAttributeValue(),featureAttribute.getAttributeValue(),attributeNameList);
                }
            });

            // ???????????????
            iFeatureAttributeService.remove(new QueryWrapper<>(new FeatureAttribute().setCostElementId(costElementId)));
            // ????????????
            iFeatureAttributeService.saveBatch(featureAttributeList);
            // ????????????????????????
            this.updateById(new CostElement().setCostElementId(costElementId).setCrucialAttributes(crucialAttributes.toString()));
        }
    }

    public void saveRateCalculation(CostElement costElement, Long costElementId) {
        // ????????????
        RateCalculation rateCalculation = costElement.getRateCalculation();
        if (StringUtil.isEmpty(rateCalculation.getRateCalculationId())) {
            // ??????
            rateCalculation.setRateCalculationId(IdGenrator.generate());
            rateCalculation.setCostElementId(costElementId);
            iRateCalculationService.save(rateCalculation);
        }else {
            // ??????
            iRateCalculationService.updateById(rateCalculation);
        }
    }

    public void saveCostElement(CostElement costElement, Long costElementId) {
        Assert.notNull(costElement.getElementType(), LocaleHandler.getLocaleMsg("???????????????????????????")+": ElementType");
        Assert.notNull(costElement.getCalculation(), LocaleHandler.getLocaleMsg("???????????????????????????")+": Calculation");
        Assert.notNull(costElement.getElementName(), LocaleHandler.getLocaleMsg("???????????????????????????")+": ElementName()");
        Assert.notNull(costElement.getOrgId(), LocaleHandler.getLocaleMsg("???????????????????????????")+": OrgId");
        costElement.setCostElementId(costElementId);
        costElement.setElementCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PRICE_COST_ELEMENT_CODE));
        // ???????????????
        if (StringUtil.isEmpty(costElement.getElementVersion())) {
            costElement.setElementVersion("V1");
        }
        if (StringUtil.isEmpty(costElement.getStatus())) {
            // ?????????
            costElement.setStatus(CostElementStatus.DRAFT.getKey());
        }
        this.save(costElement);
    }

    @Override
    public CostElement get(Long costElementId) {
        CostElement costElement = null;
        if (StringUtil.notEmpty(costElementId)) {
            // ??????????????????
            costElement = this.getById(costElementId);
            Assert.notNull(costElement,LocaleHandler.getLocaleMsg("?????????????????????"));
            if(ElementType.FEE.getKey().equals(costElement.getElementType()) &&
                    Calculation.CALCULATED_BY_RATE.getKey().equals(costElement.getCalculation())){
                // ??????????????????
                RateCalculation rateCalculation = iRateCalculationService.getOne(new QueryWrapper<>(new RateCalculation().setCostElementId(costElementId)));
                costElement.setRateCalculation(rateCalculation);
            }else {
                // ??????????????????
                QueryWrapper<FeatureAttribute> featureAttributeQueryWrapper = new QueryWrapper<>();
                featureAttributeQueryWrapper.eq("COST_ELEMENT_ID",costElementId);
                List<FeatureAttribute> featureAttributeList = iFeatureAttributeService.list(featureAttributeQueryWrapper);
                costElement.setFeatureAttributeList(featureAttributeList);
                // ????????????????????????
                QueryWrapper<FeatureFormula> dosageFormulasWrapper = new QueryWrapper<>();
                dosageFormulasWrapper.eq("COST_ELEMENT_ID",costElementId);
                dosageFormulasWrapper.eq("FORMULA_TYPE",FormulaType.FORMULA_TYPE_0.getKey());
                List<FeatureFormula> dosageFormulasList = iFeatureFormulaService.list(dosageFormulasWrapper);
                costElement.setDosageFormulasList(dosageFormulasList);
                // ??????????????????
                QueryWrapper<FeatureFormula> priceFormulasWrapper = new QueryWrapper<>();
                priceFormulasWrapper.eq("COST_ELEMENT_ID",costElementId);
                priceFormulasWrapper.eq("FORMULA_TYPE",FormulaType.FORMULA_TYPE_1.getKey());
                List<FeatureFormula> priceFormulasList = iFeatureFormulaService.list(priceFormulasWrapper);
                costElement.setPriceFormulasList(priceFormulasList);
                // ????????????
                QueryWrapper<BasePrice> basePriceQueryWrapper = new QueryWrapper<>();
                basePriceQueryWrapper.eq("ELEMENT_CODE",costElement.getElementCode());
                basePriceQueryWrapper.and(queryWrapper -> {
                    queryWrapper.ne("DISPLAY_CATEGORY",DisplayCategory.SUM_TABLE.getKey()).isNotNull("DISPLAY_CATEGORY");
                });
                List<BasePrice> basePriceList = iBasePriceService.list(basePriceQueryWrapper);
                costElement.setBasePricesList(basePriceList);
            }
        }
        return costElement;
    }

    @Override
    @Transactional
    public void delete(Long costElementId) {
        CostElement costElement = this.getById(costElementId);
        Assert.notNull(costElement,LocaleHandler.getLocaleMsg("?????????????????????"));
        // ??????????????????
        this.removeById(costElementId);
        // ??????????????????
        iFeatureAttributeService.remove(new QueryWrapper<>(new FeatureAttribute().setCostElementId(costElementId)));
        // ????????????
        iFeatureFormulaService.remove(new QueryWrapper<>(new FeatureFormula().setCostElementId(costElementId)));
        // ????????????
        iBasePriceService.remove(new QueryWrapper<>(new BasePrice().setElementCode(costElement.getElementCode())));
        // ??????????????????
        iRateCalculationService.remove(new QueryWrapper<>(new RateCalculation().setCostElementId(costElementId)));
    }

    @Override
    @Transactional
    public void takeEffect(Long costElementId) {
        /**
         * 1. ??????????????????
         * 2. ????????????????????????????????????
         */
        CostElement costElement = this.getById(costElementId);
        Assert.notNull(costElement,LocaleHandler.getLocaleMsg("?????????????????????"));
        costElement.setStatus(CostElementStatus.VALID.getKey());
        this.updateById(costElement);
        // ?????????????????????
        List<BasePrice> basePrices = iBasePriceService.list(new QueryWrapper<>(new BasePrice().setElementCode(costElement.getElementCode())));
        if(CollectionUtils.isNotEmpty(basePrices)){
            basePrices.forEach(basePrice -> {
                basePrice.setStatus(CostElementStatus.VALID.getKey());
                basePrice.setDisplayCategory(DisplayCategory.SUB_SUM_TABLE.getKey());
            });
            iBasePriceService.updateBatchById(basePrices);
        }
    }

    @Override
    public void failure(Long costElementId) {
        CostElement costElement = this.getById(costElementId);
        Assert.notNull(costElement,LocaleHandler.getLocaleMsg("?????????????????????"));
        costElement.setStatus(CostElementStatus.INVALID.getKey());
        this.updateById(costElement);
    }

    @Override
    @Transactional
    public Long createNewVersion(CostElement costElement) {
        Long costElementId1 = costElement.getCostElementId();
        Assert.notNull(costElementId1, LocaleHandler.getLocaleMsg("???????????????????????????")+": costElementId");
        // ???????????????
        this.failure(costElementId1);
        // ????????????ID
        Long costElementId = IdGenrator.generate();
        // ????????????????????????
        this.saveNewCostElement(costElement, costElementId);
        if(ElementType.FEE.getKey().equals(costElement.getElementType()) &&
                Calculation.CALCULATED_BY_RATE.getKey().equals(costElement.getCalculation())){
            // ??????????????????
            this.saveNewRateCalculation(costElement, costElementId);
        }else {
            // ??????????????????
            List<String> attributeNameList = new ArrayList<>();
            // ????????????????????????
            ArrayList<String> numberAttrList = new ArrayList<>();
            // ?????????Map
            ArrayList<AttrKeyValue> enumKeyValueList = new ArrayList<>();
            // ??????????????????
            this.saveFeatureAttribute(enumKeyValueList, numberAttrList,costElement, costElementId,attributeNameList);
            // ??????????????????
            this.saveDosageFormula(enumKeyValueList, numberAttrList,costElement, costElementId ,attributeNameList);
            // ????????????
            this.savePriceFormula(enumKeyValueList,numberAttrList,costElement, costElementId,attributeNameList);
            // ??????
            this.saveNewBasePrice(costElement);
        }
        return costElementId;
    }

    public void saveNewBasePrice(CostElement costElement) {
        List<BasePrice> basePriceList = costElement.getBasePricesList();
        if(CollectionUtils.isNotEmpty(basePriceList)){
            // ????????????
            String elementCode = costElement.getElementCode();
            int i = 1;
            for(BasePrice basePrice: basePriceList){
                basePrice.setBasePriceId(IdGenrator.generate());
                // ??????
                basePrice.setElementCode(elementCode);
                basePrice.setPriceVersion(costElement.getElementVersion());
                basePrice.setElementName(costElement.getElementName());
                basePrice.setElementType(costElement.getElementType());
                // ??????????????????
                String num = String.format("%05d", i);
                basePrice.setCombinationCode(elementCode+num);
                basePrice.setStatus(CostElementStatus.DRAFT.getKey());
                basePrice.setDisplayCategory(DisplayCategory.SUB_TABLE.getKey());
                iBasePriceService.save(basePrice);
                i++;
            }
        }
    }

    public void saveNewRateCalculation(CostElement costElement, Long costElementId) {
        RateCalculation rateCalculation = costElement.getRateCalculation();
        // ??????
        rateCalculation.setRateCalculationId(IdGenrator.generate());
        rateCalculation.setCostElementId(costElementId);
        iRateCalculationService.save(rateCalculation);
    }

    public void saveNewCostElement(CostElement costElement, Long costElementId) {
        Assert.notNull(costElement.getElementType(), LocaleHandler.getLocaleMsg("???????????????????????????")+": ElementType");
        Assert.notNull(costElement.getCalculation(), LocaleHandler.getLocaleMsg("???????????????????????????")+": Calculation");
        Assert.notNull(costElement.getElementName(), LocaleHandler.getLocaleMsg("???????????????????????????")+": ElementName()");
        Assert.notNull(costElement.getOrgId(), LocaleHandler.getLocaleMsg("???????????????????????????")+": OrgId");
        costElement.setCostElementId(costElementId);
        // ???????????????
        if (StringUtil.isEmpty(costElement.getElementVersion())) {
            costElement.setElementVersion("V1");
        }else {
            String elementVersion = costElement.getElementVersion();
            int newVersion= Integer.parseInt(StringUtils.substringAfter(elementVersion, "V")) + 1;
            costElement.setElementVersion("V"+newVersion);
        }
        costElement.setStatus(CostElementStatus.DRAFT.getKey());
        this.save(costElement);
    }

}
