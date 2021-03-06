package com.midea.cloud.srm.cm.contract.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.googlecode.aviator.AviatorEvaluator;
import com.midea.cloud.common.enums.ImportStatus;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.contract.CalculatingSigns;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.srm.cm.contract.mapper.LevelMaintainMapper;
import com.midea.cloud.srm.cm.contract.service.ILevelMaintainService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.cm.contract.dto.LevelMaintainExportDto;
import com.midea.cloud.srm.model.cm.contract.dto.LevelMaintainImportDto;
import com.midea.cloud.srm.model.cm.contract.dto.LevelMaintainModelImportDto;
import com.midea.cloud.srm.model.cm.contract.entity.LevelMaintain;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
*  <pre>
 *  ????????????????????? ???????????????
 * </pre>
*
* @author yourname@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-10-06 10:59:25
 *  ????????????:
 * </pre>
*/
@Service
public class LevelMaintainServiceImpl extends ServiceImpl<LevelMaintainMapper, LevelMaintain> implements ILevelMaintainService {
    @Resource
    private BaseClient baseClient;
    @Resource
    private FileCenterClient fileCenterClient;

    private static final Map<String,String> calculatingSigns;
    static {
        calculatingSigns = new HashMap<>();
        calculatingSigns.put("MORE_THAN",">");
        calculatingSigns.put("MORE_THAN_EQUAL",">=");
        calculatingSigns.put("LESS_THAN","<");
        calculatingSigns.put("LESS_THAN_EQUAL","<=");
        calculatingSigns.put("EQUAL","==");
    }

    /**
     * ??????
     * @param levelMaintain
     */
    @Override
    public Long add(LevelMaintain levelMaintain) {
        // ????????????
        checkParam(levelMaintain);
        levelMaintain.setLevelMaintainId(IdGenrator.generate());
        this.save(levelMaintain);
        return levelMaintain.getLevelMaintainId();
    }

    private void checkParam(LevelMaintain levelMaintain) {
        Assert.notNull(levelMaintain.getCategoryId(),"????????????????????????");
        Assert.notNull(levelMaintain.getAmount(),"??????????????????????????????");
        Assert.notNull(levelMaintain.getOperational(),"????????????????????????");
        Assert.notNull(levelMaintain.getLevel(),"????????????????????????");
        if (StringUtil.isEmpty(levelMaintain.getLevelMaintainId())) {
            List<LevelMaintain> levelMaintains = this.list(new QueryWrapper<>(new LevelMaintain().setCategoryId(levelMaintain.getCategoryId()).
                    setAmount(levelMaintain.getAmount()).setOperational(levelMaintain.getOperational())));
            Assert.isTrue(CollectionUtils.isEmpty(levelMaintains),"????????????+??????+????????????{}?????????");
        }else {
            List<LevelMaintain> levelMaintains = this.list(new QueryWrapper<>(new LevelMaintain().setCategoryId(levelMaintain.getCategoryId()).
                    setAmount(levelMaintain.getAmount()).setOperational(levelMaintain.getOperational())));
            Assert.isTrue(CollectionUtils.isEmpty(levelMaintains) || levelMaintains.get(0).getLevelMaintainId().compareTo(levelMaintain.getLevelMaintainId()) == 0,"????????????+??????+????????????{}?????????");
        }
        if(StringUtil.isEmpty(levelMaintain.getStartData())){
            levelMaintain.setStartData(LocalDate.now());
        }
        if(StringUtil.notEmpty(levelMaintain.getStruct())){
            String struct = levelMaintain.getStruct();
            PurchaseCategory purchaseCategoryByParm = baseClient.getPurchaseCategoryByParm(new PurchaseCategory().setStruct(struct));
            if(null != purchaseCategoryByParm){
                levelMaintain.setCategoryFullName(purchaseCategoryByParm.getCategoryFullName());
            }
        }
        // ????????????????????????
//        if (StringUtil.notEmpty(levelMaintain.getStruct())) {
//            List<LevelMaintain> levelMaintains = this.list(new QueryWrapper<>(new LevelMaintain().setStruct(levelMaintain.getStruct())));
//            boolean intervalRepeat = isIntervalRepeat(levelMaintain, levelMaintains);
//            Assert.isTrue(!intervalRepeat,"????????????????????????????????????");
//        }


        // ????????????
        StringBuffer formula = new StringBuffer("${value}");
        formula.append(" ").append(calculatingSigns.get(levelMaintain.getOperational()));
        formula.append(" ").append(levelMaintain.getAmount());
        levelMaintain.setFormula(formula.toString());
    }

    /**
     * ????????????????????????
     * @return
     */
    public boolean isIntervalRepeat(LevelMaintain levelMaintain,List<LevelMaintain> levelMaintains){
        AtomicBoolean flag = new AtomicBoolean(false);
        // ????????????
        BigDecimal amount = levelMaintain.getAmount();
        String operational = levelMaintain.getOperational();
        if (CollectionUtils.isNotEmpty(levelMaintains)) {
            // ??????
            if(CalculatingSigns.EQUAL.getCode().equals(operational)){
                    levelMaintains.forEach(levelMaintain1 -> {
                        // ????????????
                        String formula1 = levelMaintain1.getFormula();
                        String replace = StringUtils.replace(formula1, "${value}", String.valueOf(amount.doubleValue()));
                        boolean flag1 = (boolean) AviatorEvaluator.execute(replace);
                        flag.set(flag1);
                    });
            } else if (CalculatingSigns.LESS_THAN.getCode().equals(operational) ||
                    CalculatingSigns.LESS_THAN_EQUAL.getCode().equals(operational)){
                    for(LevelMaintain levelMaintain1 : levelMaintains){
                        // ????????????
                        String operational1 = levelMaintain1.getOperational();
                        BigDecimal amount1 = levelMaintain1.getAmount();
                        // ?????? ??? ????????????
                        if(CalculatingSigns.LESS_THAN.getCode().equals(operational1) ||
                                CalculatingSigns.LESS_THAN_EQUAL.getCode().equals(operational1)){
                            // <  <=
                            flag.set(true);
                            break;
                        }
                        // ??????
                        else if(CalculatingSigns.EQUAL.getCode().equals(operational1)){
                            // ==
                            if(CalculatingSigns.LESS_THAN.getCode().equals(operational) &&
                                    amount1.compareTo(amount) < 0){
                                flag.set(true);
                                break;
                            }else if (CalculatingSigns.LESS_THAN_EQUAL.getCode().equals(operational) &&
                                    amount1.compareTo(amount) <= 0){
                                flag.set(true);
                                break;
                            }
                        } else {
                            if(CalculatingSigns.LESS_THAN.getCode().equals(operational)){
                                // <
                                if(amount1.compareTo(amount) < 0){
                                    flag.set(true);
                                    break;
                                }
                            }else if(CalculatingSigns.LESS_THAN_EQUAL.getCode().equals(operational)){
                                // <=
                                if(amount1.compareTo(amount) <= 0){
                                    // >
                                    flag.set(true);
                                    break;
                                }
                            }

                        }

                    }
            } else { // >   >=
                for(LevelMaintain levelMaintain1 : levelMaintains){
                    // ????????????
                    String operational1 = levelMaintain1.getOperational();
                    BigDecimal amount1 = levelMaintain1.getAmount();
                    if(CalculatingSigns.MORE_THAN.getCode().equals(operational1) ||
                            CalculatingSigns.MORE_THAN_EQUAL.getCode().equals(operational1)){
                        // >  >=
                        flag.set(true);
                        break;
                    }else if(CalculatingSigns.EQUAL.getCode().equals(operational1)){
                        // ==
                        if(CalculatingSigns.MORE_THAN.getCode().equals(operational) &&
                                amount1.compareTo(amount) > 0){
                            flag.set(true);
                            break;
                        }else if (CalculatingSigns.MORE_THAN_EQUAL.getCode().equals(operational) &&
                                amount1.compareTo(amount) >= 0){
                            flag.set(true);
                            break;
                        }
                    }else {
                        // <  ??? <=               < 30
                        if(CalculatingSigns.MORE_THAN.getCode().equals(operational)){
                            // >
                            if(amount1.compareTo(amount) > 0){
                                flag.set(true);
                                break;
                            }
                        }else if(CalculatingSigns.MORE_THAN_EQUAL.getCode().equals(operational)){
                            // <=
                            if(amount1.compareTo(amount) >= 0){
                                // >
                                flag.set(true);
                                break;
                            }
                        }

                    }

                }

            }
        }
        return flag.get();
    }

    @Override
    public Long modify(LevelMaintain levelMaintain) {
        checkParam(levelMaintain);
        this.updateById(levelMaintain);
        return levelMaintain.getLevelMaintainId();
    }

    @Override
    public PageInfo<LevelMaintain> listPage(LevelMaintain levelMaintain) {
        PageUtil.startPage(levelMaintain.getPageNum(), levelMaintain.getPageSize());
        List<LevelMaintain> levelMaintains = getLevelMaintains(levelMaintain);
        return new PageInfo<>(levelMaintains);
    }

    private List<LevelMaintain> getLevelMaintains(LevelMaintain levelMaintain) {
        QueryWrapper<LevelMaintain> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtil.notEmpty(levelMaintain.getStruct()),"STRUCT", levelMaintain.getStruct());
        wrapper.eq(StringUtil.notEmpty(levelMaintain.getLevel()),"LEVEL", levelMaintain.getLevel());

        if(YesOrNo.YES.getValue().equals(levelMaintain.getIsValid())){
            wrapper.and(queryWrapper ->queryWrapper.isNull("END_DATA").or().ge("END_DATA",LocalDate.now()));
        }else if (YesOrNo.NO.getValue().equals(levelMaintain.getIsValid())){
            wrapper.isNotNull("END_DATA");
            wrapper.lt("END_DATA",LocalDate.now());
        }
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        List<LevelMaintain> levelMaintains = this.list(wrapper);

        // ?????????????????????
        if(CollectionUtils.isNotEmpty(levelMaintains)){
            List<Long> ids = new ArrayList<>();
            levelMaintains.forEach(levelMaintain1 -> {
                Long categoryId = levelMaintain1.getCategoryId();
                ids.add(categoryId);
            });
            Map<String, String> idMap = baseClient.queryCategoryFullNameByLevelIds(ids);
            levelMaintains.forEach(levelMaintain1 -> {
                Long categoryId = levelMaintain1.getCategoryId();
                String s = idMap.get(String.valueOf(categoryId));
                levelMaintain1.setCategoryFullName(s);
            });

        }
        return levelMaintains;
    }

    @Override
    public void importModelDownload(HttpServletResponse response) throws IOException {
        String fileName = "??????????????????????????????";
        ArrayList<LevelMaintainModelImportDto> levelMaintainImportDtos = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        EasyExcelUtil.writeExcelWithModel(outputStream,fileName,levelMaintainImportDtos,LevelMaintainModelImportDto.class);
    }

    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload) throws Exception {
        // ????????????
        EasyExcelUtil.checkParam(file,fileupload);
        // ????????????
        List<LevelMaintainImportDto> levelMaintainImportDtos = readData(file);
        List<LevelMaintain> levelMaintains = new ArrayList<>();
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        checkData(errorFlag,levelMaintainImportDtos, levelMaintains);
        if(errorFlag.get()){
            // ?????????
            fileupload.setFileSourceName("??????????????????????????????");
            Fileupload fileupload1 = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    levelMaintainImportDtos, LevelMaintainImportDto.class, file.getName(), file.getOriginalFilename(), file.getContentType());
            return ImportStatus.importError(fileupload1.getFileuploadId(),fileupload1.getFileSourceName());
        }else {
            if(CollectionUtils.isNotEmpty(levelMaintains)){
                levelMaintains.forEach(levelMaintain -> {
                    // ??????????????????
                    LevelMaintain maintain = this.getOne(new QueryWrapper<>(new LevelMaintain().setCategoryId(levelMaintain.getCategoryId()).setAmount(levelMaintain.getAmount()).setOperational(levelMaintain.getOperational())));
                    if(null != maintain){
                        // ??????
                        maintain.setLevel(levelMaintain.getLevel()).setStartData(levelMaintain.getStartData()).
                                setEndData(levelMaintain.getEndData());
                        this.updateById(maintain);
                    }else {
                        // ??????
                        add(levelMaintain);
                    }
                });
            }
        }
        return ImportStatus.importSuccess();
    }

    private void checkData(AtomicBoolean errorFlag,List<LevelMaintainImportDto> levelMaintainImportDtos, List<LevelMaintain> levelMaintains) {
        if(CollectionUtils.isNotEmpty(levelMaintainImportDtos)){
            Map<String, String> dicValueKey = setMapValueKey();
            Set<String> onlyCode = new HashSet<>();
            // ????????????
            HashMap<String, List<LevelMaintain>> levelMaintainImportMap = new HashMap<>();
            // ???????????????
            HashMap<String, List<LevelMaintain>> levelMaintainDateMap = new HashMap<>();

            // ????????????????????????
            List<String> categoryNameList = new ArrayList<>();
            for(LevelMaintainImportDto levelMaintainImportDto : levelMaintainImportDtos){
                String categoryName = levelMaintainImportDto.getCategoryName();
                if(StringUtil.notEmpty(categoryName)){
                    categoryName = categoryName.trim();
                    categoryNameList.add(categoryName);
                }
            }
            Map<String, PurchaseCategory> purchaseCategoryMap = baseClient.queryPurchaseCategoryByLevelNames(categoryNameList);

            // ????????????????????????
            Map<String, List<LevelMaintain>> levelMaintainMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(categoryNameList)) {
                categoryNameList = categoryNameList.stream().distinct().collect(Collectors.toList());
                List<LevelMaintain> levelMaintainList = this.list(new QueryWrapper<LevelMaintain>().in("CATEGORY_NAME", categoryNameList));
                levelMaintainMap = levelMaintainList.stream().filter(levelMaintain -> StringUtil.notEmpty(levelMaintain.getStruct())).collect(Collectors.groupingBy(LevelMaintain::getStruct));
            }


            for(LevelMaintainImportDto levelMaintainImportDto : levelMaintainImportDtos) {
                StringBuffer errorMsg = new StringBuffer();
                LevelMaintain levelMaintain = new LevelMaintain();
                StringBuffer code = new StringBuffer();
                boolean flag = true;
                // ??????????????????
                String categoryName = levelMaintainImportDto.getCategoryName();
                if(StringUtil.notEmpty(categoryName)){
                    categoryName = categoryName.trim();
                    code.append(categoryName);
                    PurchaseCategory purchaseCategory = purchaseCategoryMap.get(categoryName);
                    if(null != purchaseCategory){
                        levelMaintain.setCategoryId(purchaseCategory.getCategoryId());
                        levelMaintain.setCategoryCode(purchaseCategory.getCategoryCode());
                        levelMaintain.setCategoryName(purchaseCategory.getCategoryName());
                        levelMaintain.setStruct(purchaseCategory.getStruct());
                        levelMaintain.setCategoryFullName(purchaseCategory.getCategoryFullName());
                    }else {
                        flag = false;
                        errorMsg.append("??????????????????????????????; ");
                        errorFlag.set(true);
                    }
                }else {
                    flag = false;
                    errorMsg.append("????????????????????????; ");
                    errorFlag.set(true);
                }

                // ????????????????????????????????????
                String amount = levelMaintainImportDto.getAmount();
                if(StringUtil.notEmpty(amount)){
                    amount = amount.trim();
                    code.append(amount);
                    if(StringUtil.isDigit(amount)){
                        levelMaintain.setAmount(new BigDecimal(amount));
                    }else {
                        flag = false;
                        errorMsg.append("????????????????????????????????????????????????; ");
                        errorFlag.set(true);
                    }
                }else{
                    flag = false;
                    errorMsg.append("????????????????????????????????????????????????; ");
                    errorFlag.set(true);
                }

                // ????????????
                String operational = levelMaintainImportDto.getOperational();
                if(StringUtil.notEmpty(operational)){
                    operational = operational.trim();
                    code.append(operational);
                    if(StringUtil.notEmpty(dicValueKey.get(operational))){
                        levelMaintain.setOperational(dicValueKey.get(operational));
                    }else {
                        flag = false;
                        errorMsg.append("??????????????????????????????; ");
                        errorFlag.set(true);
                    }
                }else {
                    flag = false;
                    errorMsg.append("????????????????????????; ");
                    errorFlag.set(true);
                }
                /**
                 * ?????????/?????????/?????????????????????????????????????????????
                 * 1. ????????????, ??????????????????????????????????????????????????????
                 * 2. ????????????????????????????????????
                 */
                if (flag){
                    StringBuffer formula = new StringBuffer("${value}");
                    formula.append(" ").append(calculatingSigns.get(levelMaintain.getOperational()));
                    formula.append(" ").append(levelMaintain.getAmount());
                    levelMaintain.setFormula(formula.toString());
                    // ??????????????????????????????
//                    if(CollectionUtils.isNotEmpty(levelMaintainImportMap.get(levelMaintain.getStruct()))){
//                        List<LevelMaintain> maintainList = levelMaintainImportMap.get(levelMaintain.getStruct());
//                        boolean intervalRepeat = isIntervalRepeat(levelMaintain, maintainList);
//                        if(intervalRepeat){
//                            errorFlag.set(true);
//                            errorMsg.append("???????????????????????????????????????????????????????????????; ");
//                        }
//                        maintainList.add(levelMaintain);
//                        levelMaintainImportMap.put(levelMaintain.getStruct(),maintainList);
//                    }else {
//                        ArrayList<LevelMaintain> maintains = new ArrayList<>();
//                        maintains.add(levelMaintain);
//                        levelMaintainImportMap.put(levelMaintain.getStruct(),maintains);
//                    }
                    // ?????????????????????????????????????????????
//                    if(CollectionUtils.isNotEmpty(levelMaintainMap.get(levelMaintain.getStruct()))){
//                        List<LevelMaintain> maintainList = levelMaintainMap.get(levelMaintain.getStruct());
//                        boolean intervalRepeat = isIntervalRepeat(levelMaintain, maintainList);
//                        if(intervalRepeat){
//                            errorFlag.set(true);
//                            errorMsg.append("??????????????????????????????????????????????????????????????????; ");
//                        }
//                        maintainList.add(levelMaintain);
//                    }
                }

                // ????????????
                String level = levelMaintainImportDto.getLevel();
                if(StringUtil.notEmpty(level)){
                    level = level.trim();
                    if(StringUtil.notEmpty(dicValueKey.get(level))){
                        levelMaintain.setLevel(dicValueKey.get(level));
                    }else {
                        errorMsg.append("??????????????????????????????; ");
                        errorFlag.set(true);
                    }
                }else {
                    errorMsg.append("????????????????????????; ");
                    errorFlag.set(true);
                }

                // ????????????
                String startData = levelMaintainImportDto.getStartData();
                if(StringUtil.notEmpty(startData)){
                    startData = startData.trim();
                    try {
                        Date date = DateUtil.parseDate(startData);
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        levelMaintain.setStartData(localDate);
                    } catch (Exception e) {
                        errorMsg.append("????????????????????????; ");
                        errorFlag.set(true);
                    }
                }else {
                    levelMaintain.setStartData(LocalDate.now());
                }

                // ????????????
                String endData = levelMaintainImportDto.getEndData();
                if(StringUtil.notEmpty(endData)){
                    endData = endData.trim();
                    try {
                        Date date = DateUtil.parseDate(endData);
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        levelMaintain.setEndData(localDate);
                    } catch (Exception e) {
                        errorMsg.append("????????????????????????; ");
                        errorFlag.set(true);
                    }
                }

                if(!onlyCode.add(code.toString())){
                    errorMsg.append("????????????+??????+????????????????????????; ");
                    errorFlag.set(true);
                }

                if(errorMsg.length() > 0){
                    levelMaintainImportDto.setErrorMsg(errorMsg.toString());
                }else {
                    levelMaintainImportDto.setErrorMsg(null);
                }

                levelMaintains.add(levelMaintain);
            }
        }
    }

    /**
     * ??????????????????
     */
    public Map<String, String> setMapValueKey() {
        List<DictItemDTO> dictItemDTOS = baseClient.listByDictCode(Arrays.asList("OPERATOR","CONTARCT_LEVEL"));
        Map<String, String> map = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dictItemDTOS)) {
            dictItemDTOS.forEach(dictItemDTO -> {
                map.put(dictItemDTO.getDictItemName().trim(),dictItemDTO.getDictItemCode().trim());
            });
        }
        return map;
    }



    private List<LevelMaintainImportDto> readData(MultipartFile file) {
        List<LevelMaintainImportDto> levelMaintainImportDtos = new ArrayList<>();
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<LevelMaintainImportDto> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream,listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(LevelMaintainImportDto.class).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            levelMaintainImportDtos = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return levelMaintainImportDtos;
    }

    @Override
    public void exportExcel(LevelMaintain levelMaintain, HttpServletResponse response) throws Exception {
        levelMaintain.setPageNum(null);
        levelMaintain.setPageSize(null);
        List<LevelMaintain> levelMaintains = getLevelMaintains(levelMaintain);
        List<LevelMaintainExportDto> levelMaintainExportDtos = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(levelMaintains)){
            Map<String, String> dicKeyValue = setMapKeyValue();
            levelMaintains.forEach(levelMaintain1 -> {
                LevelMaintainExportDto levelMaintainExportDto = new LevelMaintainExportDto();
                BeanCopyUtil.copyProperties(levelMaintainExportDto,levelMaintain1);
                // ????????????,????????????
                levelMaintainExportDto.setOperational(dicKeyValue.get(levelMaintain1.getOperational()));
                levelMaintainExportDto.setLevel(dicKeyValue.get(levelMaintain1.getLevel()));
                if(StringUtil.notEmpty(levelMaintain1.getStartData())){
                    String dateToStr = DateUtil.localDateToStr(levelMaintain1.getStartData());
                    levelMaintainExportDto.setStartData(dateToStr);
                }
                if(StringUtil.notEmpty(levelMaintain1.getEndData())){
                    String dateToStr = DateUtil.localDateToStr(levelMaintain1.getEndData());
                    levelMaintainExportDto.setEndData(dateToStr);
                }
                levelMaintainExportDtos.add(levelMaintainExportDto);
            });
            String fileName = "????????????????????????";
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
            EasyExcelUtil.writeExcelWithModel(outputStream,fileName,levelMaintainExportDtos,LevelMaintainExportDto.class);
        }
    }

    /**
     * ??????????????????
     */
    public Map<String, String> setMapKeyValue() {
        List<DictItemDTO> dictItemDTOS = baseClient.listByDictCode(Arrays.asList("OPERATOR","CONTARCT_LEVEL"));
        Map<String, String> map = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dictItemDTOS)) {
            dictItemDTOS.forEach(dictItemDTO -> {
                map.put(dictItemDTO.getDictItemCode().trim(),dictItemDTO.getDictItemName().trim());
            });
        }
        return map;
    }
}
