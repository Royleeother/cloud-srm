package com.midea.cloud.srm.base.purchase.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.Enable;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.base.BigCategoryTypeEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.utils.EasyExcelUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.base.material.service.IMaterialItemService;
import com.midea.cloud.srm.base.purchase.mapper.PurchaseCategoryMapper;
import com.midea.cloud.srm.base.purchase.service.IPurchaseCategoryService;
import com.midea.cloud.srm.base.serviceconfig.service.IBaseServiceConfigService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.material.vo.MaterialMaxCategoryVO;
import com.midea.cloud.srm.model.base.purchase.dto.*;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.serviceconfig.entity.ServiceConfig;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ???????????? ???????????????
 * </pre>
 *
 * @author chensl26@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-03-06 15:38:44
 *  ????????????:
 * </pre>
 */
@Service
public class PurchaseCategoryServiceImpl extends ServiceImpl<PurchaseCategoryMapper, PurchaseCategory> implements IPurchaseCategoryService {

    @Autowired
    IBaseServiceConfigService baseServiceConfigService;
    @Resource
    PurchaseCategoryMapper purchaseCategoryMapper;
    @Autowired
    IBaseServiceConfigService iBaseServiceConfigService;
    @Resource
    private FileCenterClient fileCenterClient;
    @Resource
    private BaseClient baseClient;
    @Resource
    private IMaterialItemService iMaterialItemService;

    @Override
    public List<PurchaseCategory> queryPurchaseCategoryByMiddleCode(String middleCode) {
        List<PurchaseCategory> purchaseCategories = null;
        List<PurchaseCategory> categories = this.list(Wrappers.lambdaQuery(PurchaseCategory.class).
                eq(PurchaseCategory::getCategoryCode, middleCode).eq(PurchaseCategory::getLevel,2).
                eq(PurchaseCategory::getEnabled,YesOrNo.YES.getValue()));
        if(CollectionUtils.isNotEmpty(categories)){
            Long categoryId = categories.get(0).getCategoryId();
            purchaseCategories = this.list(Wrappers.lambdaQuery(PurchaseCategory.class).
                    like(PurchaseCategory::getStruct, categoryId).
                    eq(PurchaseCategory::getLevel, 3).
                    eq(PurchaseCategory::getEnabled, YesOrNo.YES.getValue()));

        }
        return purchaseCategories;
    }

    @Override
    public Map<String, String> queryCategoryFullNameByLevelIds(List<Long> ids) {
        Map<String, String> categoryFullMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)){
            ids = ids.stream().distinct().collect(Collectors.toList());
            List<PurchaseCategory> purchaseCategories = this.listByIds(ids);
            if(CollectionUtils.isNotEmpty(purchaseCategories)){
                purchaseCategories.forEach(purchaseCategory -> {
                    String struct = purchaseCategory.getStruct();
                    List<Long> categoryIds = StringUtil.stringConvertNumList(struct, "-");
                    if (!CollectionUtils.isEmpty(categoryIds)) {
                        QueryWrapper<PurchaseCategory> queryWrapper = new QueryWrapper<>();
                        queryWrapper.in("CATEGORY_ID", categoryIds);
                        List<PurchaseCategory> list = purchaseCategoryMapper.selectList(queryWrapper);
                        String[] categoryFullNameArr = new String[list.size()];
                        for (int i = 0; i < list.size(); i++) {
                            categoryFullNameArr[i] = list.get(i).getCategoryName();
                        }
                        String categoryFullName = String.join("-", categoryFullNameArr);
                        categoryFullMap.put(String.valueOf(purchaseCategory.getCategoryId()),categoryFullName);
                    }
                });
            }
        }
        return categoryFullMap;
    }

    @Override
    public List<PurchaseCategory> listLogisticsCategoryByLevel(PurchaseCategory purchaseCategory) {
        List<PurchaseCategory> purchaseCategories = new ArrayList<>();
        purchaseCategory.setEnabled(YesOrNo.YES.getValue());
        purchaseCategory.setParentCode(BigCategoryTypeEnum.LOGISTICS.getCode());
        List<ServiceConfig> serviceConfigs = iBaseServiceConfigService.list();
        if(CollectionUtils.isNotEmpty(serviceConfigs)){
            String level = serviceConfigs.get(0).getServiceLevel();
            if (StringUtils.isNotBlank(level)) {
                purchaseCategory.setLevel(Integer.valueOf(level));
                purchaseCategories = purchaseCategoryMapper.listByParam(purchaseCategory);
                //?????????????????????????????????
                setCategoryFullName(purchaseCategories);
            }
        }
        return purchaseCategories;
    }

    private void setCategoryFullName(List<PurchaseCategory> purchaseCategories){
        //??????????????????
        List<Long> idParams = new LinkedList<>();
        for(PurchaseCategory p : purchaseCategories){
            String struct = p.getStruct();
            List<Long> categoryIds = StringUtil.stringConvertNumList(struct, "-");
            if(CollectionUtils.isNotEmpty(categoryIds)){
                idParams.addAll(categoryIds);
            }
        }
        //????????????
        if(CollectionUtils.isEmpty(idParams)){
            return;
        }
        QueryWrapper<PurchaseCategory> wrapper = new QueryWrapper<>();
        wrapper.in("CATEGORY_ID",idParams);
        List<PurchaseCategory> purchaseCategoryList = this.list(wrapper);
        if(CollectionUtils.isEmpty(purchaseCategoryList)){
            return;
        }
        Map<Long,PurchaseCategory> map = purchaseCategoryList.stream().collect(Collectors.toMap(item -> item.getCategoryId(),item -> item));
        //???????????????????????????
        for(PurchaseCategory purchaseCategory : purchaseCategories){
            String struct = purchaseCategory.getStruct();
            List<Long> categoryIds = StringUtil.stringConvertNumList(struct, "-");
            String[] categoryFullNameArr = new String[categoryIds.size()];
            for(int i=0;i<categoryIds.size();i++){
                PurchaseCategory p = map.get(categoryIds.get(i));
                if(Objects.nonNull(p)){
                    categoryFullNameArr[i] = p.getCategoryName();
                }
            }
            String categoryFullName = String.join("-", categoryFullNameArr);
            purchaseCategory.setCategoryFullName(categoryFullName);
        }

    }

    @Override
    public PurchaseCategory getByParm(PurchaseCategory purchaseCategory) {
        List<PurchaseCategory> purchaseCategories = this.list(new QueryWrapper<>(purchaseCategory));
        if(CollectionUtils.isNotEmpty(purchaseCategories)){
            PurchaseCategory purchaseCategory1 = purchaseCategories.get(0);
            setCategoryFullName(purchaseCategory1);
            return purchaseCategory1;
        }else {
            return null;
        }
    }

    /**
     * ???????????????????????????????????????
     * @param categoryFullNameList
     * @return
     */
    @Override
    public List<PurchaseCategory> queryPurchaseByCategoryFullName(List<String> categoryFullNameList) {
        List<PurchaseCategory> purchaseCategories = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(categoryFullNameList)){
            categoryFullNameList.forEach(categoryFullName -> {
                categoryFullName = categoryFullName.trim();
                if(categoryFullName.contains("-")){
                    List<String> categoryNames = Arrays.asList(categoryFullName.split("-"));
                    if (CollectionUtils.isNotEmpty(categoryNames)){
                        int size = categoryNames.size();
                        int sizeTemp = 0;
                        String struct = "";
                        for(String categoryName : categoryNames){
                            List<PurchaseCategory> categories = this.list(new QueryWrapper<>(new PurchaseCategory().setCategoryName(categoryName)));
                            if(CollectionUtils.isNotEmpty(categories)){
                                Long categoryId = categories.get(0).getCategoryId();
                                struct = "".equals(struct) ? categoryId + "-" : struct + categoryId + "-";
                                sizeTemp ++;
                            }
                        }
                        if(size == sizeTemp){
                            struct = StringUtils.left(struct, struct.length()-1);
                            List<PurchaseCategory> categories = this.list(new QueryWrapper<>(new PurchaseCategory().setStruct(struct)));
                            if(CollectionUtils.isNotEmpty(categories)){
                                PurchaseCategory purchaseCategory = categories.get(0);
                                purchaseCategory.setCategoryFullName(categoryFullName);
                                purchaseCategories.add(purchaseCategory);
                            }
                        }
                    }
                }else {
                    List<PurchaseCategory> categories = this.list(new QueryWrapper<>(new PurchaseCategory().setCategoryName(categoryFullName)));
                    if(CollectionUtils.isNotEmpty(categories)){
                        PurchaseCategory purchaseCategory = categories.get(0);
                        purchaseCategory.setCategoryFullName(categoryFullName);
                        purchaseCategories.add(purchaseCategory);
                    }
                }

            });
        }
        return purchaseCategories;
    }

    /**
     * ?????????????????????????????????50
     * @param materialIds
     * @return
     */
    @Override
    public boolean checkBigClassCodeIsContain50(List<Long> materialIds) {
        AtomicBoolean flag = new AtomicBoolean(false);
        if (CollectionUtils.isNotEmpty(materialIds)) {
            List<MaterialItem> materialItems = iMaterialItemService.list(new QueryWrapper<MaterialItem>().in("MATERIAL_ID", materialIds));
            if(CollectionUtils.isNotEmpty(materialItems)){
                materialItems.forEach(materialItem -> {
                    String struct = materialItem.getStruct();
                    if (StringUtil.notEmpty(struct)){
                        List<String> ids = Arrays.asList(struct.split("-"));
                        PurchaseCategory purchaseCategory = this.getById(ids.get(0));
                        if("50".equals(purchaseCategory.getCategoryCode())){
                            flag.set(true);
                        }
                    }
                });
            }
        }
        return flag.get();
    }


    @Override
    public List<PurchaseCategory> listParent() {
        ServiceConfig serviceConfig = baseServiceConfigService.list().get(0);
        Integer serviceLevel = Integer.valueOf(serviceConfig.getServiceLevel());
        //???????????????1???,???????????????
        if (serviceLevel == 1) {
            return null;
        }
        QueryWrapper<PurchaseCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("LEVEL", serviceLevel);
        return purchaseCategoryMapper.selectList(queryWrapper);
    }

    @Override
    public void batchSaveOrUpdate(List<PurchaseCategory> purchaseCategories) {
        if (!CollectionUtils.isEmpty(purchaseCategories)) {
            for (PurchaseCategory purchaseCategory : purchaseCategories) {
                if (purchaseCategory == null) continue;
                checkBeforeSaveOrUpdate(purchaseCategory);
                if (purchaseCategory.getCategoryId() == null) {
                    long categoryId = IdGenrator.generate();
                    //??????level???struct
                    setLevelAndStruct(purchaseCategory, categoryId);
                    purchaseCategory.setCategoryId(categoryId);
                    purchaseCategoryMapper.insert(purchaseCategory);
                } else {
                    Long categoryId = purchaseCategory.getCategoryId();

                    /**?????????????????????????????????????????????????????????????????????????????????????????????*/
                    String enable = purchaseCategory.getEnabled();
                    if (Enable.N.toString().equals(enable)) {
                        QueryWrapper<PurchaseCategory> purchaseCategoryQueryWrapper = new QueryWrapper<>();
                        purchaseCategoryQueryWrapper.eq("ENABLED", Enable.Y.toString());
                        purchaseCategoryQueryWrapper.likeRight("STRUCT", categoryId);
                        List<PurchaseCategory> purchaseCategorySonList = purchaseCategoryMapper.selectList(purchaseCategoryQueryWrapper);
                        if (!CollectionUtils.isEmpty(purchaseCategorySonList) && purchaseCategorySonList.size() > 1) {
                            throw new BaseException("???????????????????????????????????????,???????????????????????????????????????????????????");
                        }
                    }

                    //??????level???struct
                    setLevelAndStruct(purchaseCategory, categoryId);
                    purchaseCategoryMapper.updateById(purchaseCategory);
                }
            }
        }
    }

    private void checkBeforeSaveOrUpdate(PurchaseCategory purchaseCategory) {
        if (StringUtils.isBlank(purchaseCategory.getCategoryCode())) {
            throw new BaseException("??????????????????");
        } else if (StringUtils.isBlank(purchaseCategory.getCategoryName())) {
            throw new BaseException("??????????????????");
        } else if (StringUtils.isBlank(purchaseCategory.getEnabled())) {
            throw new BaseException("?????????????????????");
        }
    }

    //??????level???struct
    private void setLevelAndStruct(PurchaseCategory purchaseCategory, Long categoryId) {
        if (purchaseCategory.getParentId() != null && purchaseCategory.getParentId() > 0) {
            PurchaseCategory selectById = purchaseCategoryMapper.selectById(purchaseCategory.getParentId());
            Integer level = selectById.getLevel();
            purchaseCategory.setLevel(level + 1);
            purchaseCategory.setStruct(selectById.getStruct() + "-" + categoryId);
        } else {
            purchaseCategory.setStruct("" + categoryId);
        }
    }

    @Override
    public PageInfo<PurchaseCategory> listPageByParm(PurchaseCategory purchaseCategory) {
        PageUtil.startPage(purchaseCategory.getPageNum(), purchaseCategory.getPageSize());
        List<PurchaseCategory> purchaseCategories = purchaseCategoryMapper.listPageByParm(purchaseCategory);
        //???????????????????????????
        for (PurchaseCategory category : purchaseCategories) {
            setCategoryFullName(category);
        }
        return new PageInfo<>(purchaseCategories);
    }

    @Override
    public List<PurchaseCategory> listChildren(Long categoryId) {
        /**??????????????????????????????*/
        PurchaseCategory queryPurchaseCategory = new PurchaseCategory();
        queryPurchaseCategory.setParentId(categoryId);
        queryPurchaseCategory.setEnabled(Enable.Y.toString());
        List<PurchaseCategory> purchaseCategories = purchaseCategoryMapper.selectList(new QueryWrapper<>(queryPurchaseCategory));
        for (PurchaseCategory purchaseCategory : purchaseCategories) {
            setCategoryFullName(purchaseCategory);
        }
        return purchaseCategories;
    }

    @Override
    public List<PurchaseCategory> listByLevel(PurchaseCategory purchaseCategory) {
        List<PurchaseCategory> purchaseCategories = new ArrayList<>();
        purchaseCategory.setEnabled(YesOrNo.YES.getValue());
        List<ServiceConfig> serviceConfigs = iBaseServiceConfigService.list();
        if (!CollectionUtils.isEmpty(serviceConfigs)) {
            String level = serviceConfigs.get(0).getServiceLevel();
            if (StringUtils.isNotBlank(level)) {
                purchaseCategory.setLevel(Integer.valueOf(level));
                purchaseCategories = purchaseCategoryMapper.listPageByParm(purchaseCategory);
                //???????????????????????????
                for (PurchaseCategory category : purchaseCategories) {
                    setCategoryFullName(category);
                }
            }
        }
        return purchaseCategories;
    }

    @Override
    public List<PurchaseCategory> listByNameBatch(List<String> purchaseCategoryNameList) {
        QueryWrapper<PurchaseCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("CATEGORY_NAME", purchaseCategoryNameList);
        return this.list(queryWrapper);
    }

    /**
     * ??????????????????
     *
     * @param response
     * @throws IOException
     */
    @Override
    public void importModelDownload(HttpServletResponse response) throws IOException {
        String fileName = "????????????????????????";
        ArrayList<PurchaseCategoryDto> purchaseCategoryDtos = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        String maxLevel = this.getMaxLevel();
        PurchaseCategoryDto purchaseCategoryDto = this.getPurchaseCategoryDto(maxLevel);
        EasyExcelUtil.writeExcelWithModel(outputStream, fileName, purchaseCategoryDtos, purchaseCategoryDto.getClass());
    }

    /**
     * ????????????????????????
     *
     * @param maxLevel
     * @return
     */
    public PurchaseCategoryDto getPurchaseCategoryDto(String maxLevel) {
        PurchaseCategoryDto purchaseCategoryDto = null;
        switch (maxLevel) {
            case "1":
                purchaseCategoryDto = new PurchaseCategoryOneDto();
                break;
            case "2":
                purchaseCategoryDto = new PurchaseCategoryTwoDto();
                break;
            case "3":
                purchaseCategoryDto = new PurchaseCategoryThreeDto();
                break;
            case "4":
                purchaseCategoryDto = new PurchaseCategoryFourDto();
                break;
            default:
                purchaseCategoryDto = new PurchaseCategoryFiveDto();
        }
        return purchaseCategoryDto;
    }

    /**
     * ??????????????????????????????
     */
    public String getMaxLevel() {
        // ??????????????????????????????
        List<ServiceConfig> list = iBaseServiceConfigService.list();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(list) && StringUtil.notEmpty(list.get(0).getServiceLevel())) {
            return list.get(0).getServiceLevel();
        } else {
            throw new BaseException("??????: ????????????->??????????????????->????????????????????? , ??????????????????????????????");
        }
    }

    /**
     * ????????????
     *
     * @param file
     * @param fileupload
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload) throws Exception {
        // ??????????????????
        String maxLevel = getMaxLevel();
        PurchaseCategoryDto purchaseCategoryDto = getPurchaseCategoryDto(maxLevel);
        // ????????????
        List<PurchaseCategoryDto> purchaseCategoryDtoList = getPurchaseCategoryDtoList(file, fileupload, purchaseCategoryDto);
        // ????????????
        return this.classificationProcessing(purchaseCategoryDto, purchaseCategoryDtoList, fileupload, file);
    }

    /**
     * ??????excel
     *
     * @throws Exception
     */
    @Override
    public void exportExcel(HttpServletResponse response) throws Exception {
        // ??????????????????
        String maxLevel = getMaxLevel();
        switch (maxLevel) {
            case "1":
                // ????????????
                this.exportExcelOne(maxLevel, response);
                break;
            case "2":
                this.exportExcelTwo(maxLevel, response);
                break;
            case "3":
                this.exportExcelThree(maxLevel, response);
                break;
            case "4":
                this.exportExcelFour(maxLevel, response);
                break;
            default:
                this.exportExcelFive(maxLevel, response);
        }

    }

    public void exportExcelFive(String maxLevel, HttpServletResponse response) throws IOException {
        List<PurchaseCategory> purchaseCategoryList = this.list(new QueryWrapper<>(new PurchaseCategory().setLevel(Integer.parseInt(maxLevel))));
        List<PurchaseCategoryFiveExportDto> purchaseCategoryFiveExportDtos = new ArrayList<>();

        // ????????????
        for (PurchaseCategory purchaseCategory : purchaseCategoryList) {
            PurchaseCategoryFiveExportDto purchaseCategoryFiveExportDto = new PurchaseCategoryFiveExportDto();

            // ????????????
            purchaseCategoryFiveExportDto.setCategoryCodeFive(purchaseCategory.getCategoryCode());
            purchaseCategoryFiveExportDto.setCategoryNameFive(purchaseCategory.getCategoryName());
            purchaseCategoryFiveExportDto.setEnabledThree(purchaseCategory.getEnabled());

            // ????????????
            PurchaseCategory categoryFour = this.getById(purchaseCategory.getParentId());
            purchaseCategoryFiveExportDto.setCategoryNameFour(categoryFour.getCategoryName());
            purchaseCategoryFiveExportDto.setCategoryCodeFour(categoryFour.getCategoryCode());
            purchaseCategoryFiveExportDto.setEnabledFour(categoryFour.getEnabled());

            // ????????????
            PurchaseCategory categoryThree = this.getById(categoryFour.getParentId());
            purchaseCategoryFiveExportDto.setCategoryCodeThree(categoryThree.getCategoryCode());
            purchaseCategoryFiveExportDto.setCategoryNameThree(categoryThree.getCategoryName());
            purchaseCategoryFiveExportDto.setEnabledThree(categoryThree.getEnabled());

            // ????????????
            PurchaseCategory categoryTwo = this.getById(categoryThree.getParentId());
            purchaseCategoryFiveExportDto.setCategoryNameTwo(categoryTwo.getCategoryName());
            purchaseCategoryFiveExportDto.setCategoryCodeTwo(categoryTwo.getCategoryCode());
            purchaseCategoryFiveExportDto.setEnabledTwo(categoryTwo.getEnabled());

            // ????????????
            PurchaseCategory categoryOne = this.getById(categoryTwo.getParentId());
            purchaseCategoryFiveExportDto.setCategoryCodeOne(categoryOne.getCategoryCode());
            purchaseCategoryFiveExportDto.setCategoryNameOne(categoryOne.getCategoryName());
            purchaseCategoryFiveExportDto.setEnabledOne(categoryOne.getEnabled());

            purchaseCategoryFiveExportDtos.add(purchaseCategoryFiveExportDto);
        }

        // ????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryFiveExportDtos)) {
            String fileName = "??????????????????";
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
            EasyExcelUtil.writeExcelWithModel(outputStream, fileName, purchaseCategoryFiveExportDtos, PurchaseCategoryFiveExportDto.class);
        }
    }

    public void exportExcelFour(String maxLevel, HttpServletResponse response) throws IOException {
        List<PurchaseCategory> purchaseCategoryList = this.list(new QueryWrapper<>(new PurchaseCategory().setLevel(Integer.parseInt(maxLevel))));
        List<PurchaseCategoryFourExportDto> purchaseCategoryFourExportDtos = new ArrayList<>();

        // ????????????
        for (PurchaseCategory purchaseCategory : purchaseCategoryList) {
            PurchaseCategoryFourExportDto purchaseCategoryFourExportDto = new PurchaseCategoryFourExportDto();

            // ????????????
            purchaseCategoryFourExportDto.setCategoryNameFour(purchaseCategory.getCategoryName());
            purchaseCategoryFourExportDto.setCategoryCodeFour(purchaseCategory.getCategoryCode());
            purchaseCategoryFourExportDto.setEnabledFour(purchaseCategory.getEnabled());

            // ????????????
            PurchaseCategory categoryThree = this.getById(purchaseCategory.getParentId());
            purchaseCategoryFourExportDto.setCategoryCodeThree(categoryThree.getCategoryCode());
            purchaseCategoryFourExportDto.setCategoryNameThree(categoryThree.getCategoryName());
            purchaseCategoryFourExportDto.setEnabledThree(categoryThree.getEnabled());

            // ????????????
            PurchaseCategory categoryTwo = this.getById(categoryThree.getParentId());
            purchaseCategoryFourExportDto.setCategoryNameTwo(categoryTwo.getCategoryName());
            purchaseCategoryFourExportDto.setCategoryCodeTwo(categoryTwo.getCategoryCode());
            purchaseCategoryFourExportDto.setEnabledTwo(categoryTwo.getEnabled());

            // ????????????
            PurchaseCategory categoryOne = this.getById(categoryTwo.getParentId());
            purchaseCategoryFourExportDto.setCategoryCodeOne(categoryOne.getCategoryCode());
            purchaseCategoryFourExportDto.setCategoryNameOne(categoryOne.getCategoryName());
            purchaseCategoryFourExportDto.setEnabledOne(categoryOne.getEnabled());

            purchaseCategoryFourExportDtos.add(purchaseCategoryFourExportDto);
        }

        // ????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryFourExportDtos)) {
            String fileName = "??????????????????";
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
            EasyExcelUtil.writeExcelWithModel(outputStream, fileName, purchaseCategoryFourExportDtos, PurchaseCategoryFourExportDto.class);
        }
    }

    public void exportExcelThree(String maxLevel, HttpServletResponse response) throws IOException {
        List<PurchaseCategory> purchaseCategoryList = this.list(new QueryWrapper<>(new PurchaseCategory().setLevel(Integer.parseInt(maxLevel))));
        List<PurchaseCategoryThreeExportDto> purchaseCategoryThreeExportDtos = new ArrayList<>();

        // ????????????
        for (PurchaseCategory purchaseCategory : purchaseCategoryList) {
            PurchaseCategoryThreeExportDto purchaseCategoryThreeExportDto = new PurchaseCategoryThreeExportDto();

            // ????????????
            purchaseCategoryThreeExportDto.setMainMaterial(purchaseCategory.getMainMaterial());
            purchaseCategoryThreeExportDto.setCategoryCodeThree(purchaseCategory.getCategoryCode());
            purchaseCategoryThreeExportDto.setCategoryNameThree(purchaseCategory.getCategoryName());
            purchaseCategoryThreeExportDto.setEnabledThree(purchaseCategory.getEnabled());

            // ????????????
            PurchaseCategory categoryTwo = this.getById(purchaseCategory.getParentId());
            purchaseCategoryThreeExportDto.setCategoryNameTwo(categoryTwo.getCategoryName());
            purchaseCategoryThreeExportDto.setCategoryCodeTwo(categoryTwo.getCategoryCode());
            purchaseCategoryThreeExportDto.setEnabledTwo(categoryTwo.getEnabled());

            // ????????????
            PurchaseCategory categoryOne = this.getById(categoryTwo.getParentId());
            purchaseCategoryThreeExportDto.setCategoryCodeOne(categoryOne.getCategoryCode());
            purchaseCategoryThreeExportDto.setCategoryNameOne(categoryOne.getCategoryName());
            purchaseCategoryThreeExportDto.setEnabledOne(categoryOne.getEnabled());

            purchaseCategoryThreeExportDtos.add(purchaseCategoryThreeExportDto);
        }

        // ????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryThreeExportDtos)) {
            String fileName = "??????????????????";
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
            EasyExcelUtil.writeExcelWithModel(outputStream, fileName, purchaseCategoryThreeExportDtos, PurchaseCategoryThreeExportDto.class);
        }
    }

    public void exportExcelTwo(String maxLevel, HttpServletResponse response) throws IOException {
        List<PurchaseCategory> purchaseCategoryList = this.list(new QueryWrapper<>(new PurchaseCategory().setLevel(Integer.parseInt(maxLevel))));
        List<PurchaseCategoryTwoExportDto> purchaseCategoryTwoExportDtos = new ArrayList<>();

        // ????????????
        for (PurchaseCategory purchaseCategory : purchaseCategoryList) {
            PurchaseCategoryTwoExportDto purchaseCategoryTwoExportDto = new PurchaseCategoryTwoExportDto();
            // ????????????
            purchaseCategoryTwoExportDto.setCategoryNameTwo(purchaseCategory.getCategoryName());
            purchaseCategoryTwoExportDto.setCategoryCodeTwo(purchaseCategory.getCategoryCode());
            purchaseCategoryTwoExportDto.setEnabledTwo(purchaseCategory.getEnabled());
            // ????????????
            Long parentId = purchaseCategory.getParentId();
            PurchaseCategory category = this.getById(parentId);
            purchaseCategoryTwoExportDto.setCategoryCodeOne(category.getCategoryCode());
            purchaseCategoryTwoExportDto.setCategoryNameOne(category.getCategoryName());
            purchaseCategoryTwoExportDto.setEnabledOne(category.getEnabled());
            purchaseCategoryTwoExportDtos.add(purchaseCategoryTwoExportDto);
        }

        // ????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryTwoExportDtos)) {
            String fileName = "??????????????????";
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
            EasyExcelUtil.writeExcelWithModel(outputStream, fileName, purchaseCategoryTwoExportDtos, PurchaseCategoryTwoExportDto.class);
        }
    }

    public void exportExcelOne(String maxLevel, HttpServletResponse response) throws IOException {
        List<PurchaseCategory> purchaseCategoryList = this.list(new QueryWrapper<>(new PurchaseCategory().setLevel(Integer.parseInt(maxLevel))));
        List<PurchaseCategoryOneExportDto> purchaseCategoryOneExportDtos = new ArrayList<>();

        // ????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryList)) {
            for (PurchaseCategory purchaseCategory : purchaseCategoryList) {
                PurchaseCategoryOneExportDto purchaseCategoryOneExportDto = new PurchaseCategoryOneExportDto();
                purchaseCategoryOneExportDto.setCategoryNameOne(purchaseCategory.getCategoryName());
                purchaseCategoryOneExportDto.setCategoryCodeOne(purchaseCategory.getCategoryCode());
                purchaseCategoryOneExportDto.setEnabledOne(purchaseCategory.getEnabled());
                purchaseCategoryOneExportDtos.add(purchaseCategoryOneExportDto);
            }
        }

        // ????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryOneExportDtos)) {
            String fileName = "??????????????????";
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
            EasyExcelUtil.writeExcelWithModel(outputStream, fileName, purchaseCategoryOneExportDtos, PurchaseCategoryOneExportDto.class);
        }
    }

    /**
     * ????????????????????????
     *
     * @param purchaseCategoryDto
     * @param purchaseCategoryDtoList
     */
    @Transactional
    public Map<String, Object> classificationProcessing(PurchaseCategoryDto purchaseCategoryDto,
                                                        List<PurchaseCategoryDto> purchaseCategoryDtoList, Fileupload fileupload, MultipartFile file) {
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryDtoList)) {
            HashSet<String> hashSet = new HashSet<>();
            if (purchaseCategoryDto instanceof PurchaseCategoryOneDto) {
                ArrayList<PurchaseCategoryOneDto> purchaseCategoryOneDtos = new ArrayList<>();
                // ????????????
                boolean errorFlag = this.checkPurchaseCategory1(purchaseCategoryDtoList, hashSet, purchaseCategoryOneDtos);
                if (errorFlag) {
                    // ?????????, ????????????
                    Map<String, Object> result = this.handleError(fileupload, file, purchaseCategoryOneDtos, PurchaseCategoryOneDto.class);
                    return result;
                } else {
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryOneDtos)) {
                        // ????????????
                        this.savePurchaseCategoryOne(purchaseCategoryOneDtos);
                    }
                    return success();
                }
            } else if (purchaseCategoryDto instanceof PurchaseCategoryTwoDto) {
                ArrayList<PurchaseCategoryTwoDto> purchaseCategoryTwoDtos = new ArrayList<>();
                // ????????????
                boolean errorFlag = this.checkPurchaseCategory2(purchaseCategoryDtoList, hashSet, purchaseCategoryTwoDtos);
                if (errorFlag) {
                    // ?????????, ????????????
                    Map<String, Object> result = this.handleError(fileupload, file, purchaseCategoryTwoDtos, PurchaseCategoryTwoDto.class);
                    return result;
                } else {
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryTwoDtos)) {
                        // ????????????
                        this.savePurchaseCategoryTwo(purchaseCategoryTwoDtos);
                    }
                    return success();
                }
            } else if (purchaseCategoryDto instanceof PurchaseCategoryThreeDto) {
                ArrayList<PurchaseCategoryThreeDto> purchaseCategoryThreeDtos = new ArrayList<>();
                // ????????????
                boolean errorFlag = this.checkPurchaseCategory3(purchaseCategoryDtoList, hashSet, purchaseCategoryThreeDtos);
                if (errorFlag) {
                    // ?????????, ????????????
                    Map<String, Object> result = this.handleError(fileupload, file, purchaseCategoryThreeDtos, PurchaseCategoryThreeDto.class);
                    return result;
                } else {
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryThreeDtos)) {
                        // ????????????
                        this.savePurchaseCategoryThree(purchaseCategoryThreeDtos);
                    }
                    return success();
                }
            } else if (purchaseCategoryDto instanceof PurchaseCategoryFourDto) {
                ArrayList<PurchaseCategoryFourDto> purchaseCategoryFourDtos = new ArrayList<>();
                // ????????????
                boolean errorFlag = this.checkPurchaseCategory4(purchaseCategoryDtoList, hashSet, purchaseCategoryFourDtos);
                if (errorFlag) {
                    // ?????????, ????????????
                    Map<String, Object> result = this.handleError(fileupload, file, purchaseCategoryFourDtos, PurchaseCategoryFourDto.class);
                    return result;
                } else {
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryFourDtos)) {
                        this.savePurchaseCategoryFour(purchaseCategoryFourDtos);
                    }
                    return success();
                }
            } else if (purchaseCategoryDto instanceof PurchaseCategoryFiveDto) {
                ArrayList<PurchaseCategoryFiveDto> purchaseCategoryFiveDtos = new ArrayList<>();
                // ????????????
                boolean errorFlag = this.checkPurchaseCategory5(purchaseCategoryDtoList, hashSet, purchaseCategoryFiveDtos);
                if (errorFlag) {
                    // ?????????, ????????????
                    Map<String, Object> result = this.handleError(fileupload, file, purchaseCategoryFiveDtos, PurchaseCategoryFiveDto.class);
                    return result;
                } else {
                    if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryFiveDtos)) {
                        this.savePurchaseCategoryFive(purchaseCategoryFiveDtos);
                    }
                    return success();
                }
            }
        }
        return success();
    }

    public void savePurchaseCategoryFive(ArrayList<PurchaseCategoryFiveDto> purchaseCategoryFiveDtos) {
        HashMap<String, PurchaseCategory> categoryHashMapOne = new HashMap<>();
        HashMap<String, PurchaseCategory> categoryHashMapTwo = new HashMap<>();
        HashMap<String, PurchaseCategory> categoryHashMapThree = new HashMap<>();
        HashMap<String, PurchaseCategory> categoryHashMapFour = new HashMap<>();

        // ???????????????,
        ArrayList<PurchaseCategoryFiveDto> collectOne = purchaseCategoryFiveDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryFiveDto::getCategoryNameOne))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectOne)) {
            for (PurchaseCategoryFiveDto purchaseCategoryFiveDto : collectOne) {
                String categoryCodeOne = purchaseCategoryFiveDto.getCategoryCodeOne();
                if (StringUtil.notEmpty(categoryCodeOne)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeOne)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeOne);
                        purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameOne());
                        purchaseCategory.setLevel(1);
                        purchaseCategory.setParentId(-1L);
                        purchaseCategory.setStruct(String.valueOf(id));
                        purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledOne());
                        this.save(purchaseCategory);
                        categoryHashMapOne.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFiveDto.getCategoryNameOne());
                        category.setEnabled(purchaseCategoryFiveDto.getEnabledOne());
                        this.updateById(category);
                        categoryHashMapOne.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameOne());
                    purchaseCategory.setLevel(1);
                    purchaseCategory.setParentId(-1L);
                    purchaseCategory.setStruct(String.valueOf(id));
                    purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledOne());
                    this.save(purchaseCategory);
                    categoryHashMapOne.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ????????????
        ArrayList<PurchaseCategoryFiveDto> collectTwo = purchaseCategoryFiveDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryFiveDto::getCategoryNameTwo))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectTwo)) {
            for (PurchaseCategoryFiveDto purchaseCategoryFiveDto : collectTwo) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapOne.get(purchaseCategoryFiveDto.getCategoryNameOne());
                String categoryCodeTwo = purchaseCategoryFiveDto.getCategoryCodeTwo();
                if (StringUtil.notEmpty(categoryCodeTwo)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeTwo)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeTwo);
                        purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameTwo());
                        purchaseCategory.setLevel(2);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledTwo());
                        this.save(purchaseCategory);
                        categoryHashMapTwo.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFiveDto.getCategoryNameTwo());
                        category.setEnabled(purchaseCategoryFiveDto.getEnabledTwo());
                        this.updateById(category);
                        categoryHashMapTwo.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameTwo());
                    purchaseCategory.setLevel(2);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledTwo());
                    this.save(purchaseCategory);
                    categoryHashMapTwo.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ????????????
        ArrayList<PurchaseCategoryFiveDto> collectThree = purchaseCategoryFiveDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryFiveDto::getCategoryNameThree))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectThree)) {
            for (PurchaseCategoryFiveDto purchaseCategoryFiveDto : collectThree) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapTwo.get(purchaseCategoryFiveDto.getCategoryNameTwo());
                String categoryCodeThree = purchaseCategoryFiveDto.getCategoryCodeThree();
                if (StringUtil.notEmpty(categoryCodeThree)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeThree)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeThree);
                        purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameThree());
                        purchaseCategory.setLevel(3);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledThree());
                        this.save(purchaseCategory);
                        categoryHashMapThree.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFiveDto.getCategoryNameThree());
                        category.setEnabled(purchaseCategoryFiveDto.getEnabledThree());
                        this.updateById(category);
                        categoryHashMapThree.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameThree());
                    purchaseCategory.setLevel(3);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledThree());
                    this.save(purchaseCategory);
                    categoryHashMapThree.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ????????????
        ArrayList<PurchaseCategoryFiveDto> collectFour = purchaseCategoryFiveDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryFiveDto::getCategoryNameFour))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectFour)) {
            for (PurchaseCategoryFiveDto purchaseCategoryFiveDto : collectFour) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapThree.get(purchaseCategoryFiveDto.getCategoryNameThree());
                String categoryCodeFour = purchaseCategoryFiveDto.getCategoryCodeFour();
                if (StringUtil.notEmpty(categoryCodeFour)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeFour)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeFour);
                        purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameFour());
                        purchaseCategory.setLevel(4);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledFour());
                        this.save(purchaseCategory);
                        categoryHashMapFour.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFiveDto.getCategoryNameFour());
                        category.setEnabled(purchaseCategoryFiveDto.getEnabledFour());
                        this.updateById(category);
                        categoryHashMapFour.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameFour());
                    purchaseCategory.setLevel(4);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledFour());
                    this.save(purchaseCategory);
                    categoryHashMapFour.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ???????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryFiveDtos)) {
            for (PurchaseCategoryFiveDto purchaseCategoryFiveDto : purchaseCategoryFiveDtos) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapFour.get(purchaseCategoryFiveDto.getCategoryNameFour());
                String categoryCodeFive = purchaseCategoryFiveDto.getCategoryCodeFive();
                if (StringUtil.notEmpty(categoryCodeFive)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeFive)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeFive);
                        purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameFive());
                        purchaseCategory.setLevel(5);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledFive());
                        this.save(purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFiveDto.getCategoryNameFive());
                        category.setEnabled(purchaseCategoryFiveDto.getEnabledFive());
                        this.updateById(category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFiveDto.getCategoryNameFive());
                    purchaseCategory.setLevel(5);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryFiveDto.getEnabledFive());
                    this.save(purchaseCategory);
                }
            }
        }
    }

    public void savePurchaseCategoryFour(ArrayList<PurchaseCategoryFourDto> purchaseCategoryFourDtos) {
        HashMap<String, PurchaseCategory> categoryHashMapOne = new HashMap<>();
        HashMap<String, PurchaseCategory> categoryHashMapTwo = new HashMap<>();
        HashMap<String, PurchaseCategory> categoryHashMapThree = new HashMap<>();

        // ???????????????,
        ArrayList<PurchaseCategoryFourDto> collectOne = purchaseCategoryFourDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryFourDto::getCategoryNameOne))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectOne)) {
            for (PurchaseCategoryFourDto purchaseCategoryFourDto : collectOne) {
                String categoryCodeOne = purchaseCategoryFourDto.getCategoryCodeOne();
                if (StringUtil.notEmpty(categoryCodeOne)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeOne)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeOne);
                        purchaseCategory.setCategoryName(purchaseCategoryFourDto.getCategoryNameOne());
                        purchaseCategory.setLevel(1);
                        purchaseCategory.setParentId(-1L);
                        purchaseCategory.setStruct(String.valueOf(id));
                        purchaseCategory.setEnabled(purchaseCategoryFourDto.getEnabledOne());
                        this.save(purchaseCategory);
                        categoryHashMapOne.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFourDto.getCategoryNameOne());
                        category.setEnabled(purchaseCategoryFourDto.getEnabledOne());
                        this.updateById(category);
                        categoryHashMapOne.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFourDto.getCategoryNameOne());
                    purchaseCategory.setLevel(1);
                    purchaseCategory.setParentId(-1L);
                    purchaseCategory.setStruct(String.valueOf(id));
                    purchaseCategory.setEnabled(purchaseCategoryFourDto.getEnabledOne());
                    this.save(purchaseCategory);
                    categoryHashMapOne.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ????????????
        ArrayList<PurchaseCategoryFourDto> collectTwo = purchaseCategoryFourDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryFourDto::getCategoryNameTwo))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectTwo)) {
            for (PurchaseCategoryFourDto purchaseCategoryFourDto : collectTwo) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapOne.get(purchaseCategoryFourDto.getCategoryNameOne());
                String categoryCodeTwo = purchaseCategoryFourDto.getCategoryCodeTwo();
                if (StringUtil.notEmpty(categoryCodeTwo)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeTwo)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeTwo);
                        purchaseCategory.setCategoryName(purchaseCategoryFourDto.getCategoryNameTwo());
                        purchaseCategory.setLevel(2);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryFourDto.getEnabledTwo());
                        this.save(purchaseCategory);
                        categoryHashMapTwo.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFourDto.getCategoryNameTwo());
                        category.setEnabled(purchaseCategoryFourDto.getEnabledTwo());
                        this.updateById(category);
                        categoryHashMapTwo.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFourDto.getCategoryNameTwo());
                    purchaseCategory.setLevel(2);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryFourDto.getEnabledTwo());
                    this.save(purchaseCategory);
                    categoryHashMapTwo.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ????????????
        ArrayList<PurchaseCategoryFourDto> collectThree = purchaseCategoryFourDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryFourDto::getCategoryNameThree))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectThree)) {
            for (PurchaseCategoryFourDto purchaseCategoryFourDto : collectThree) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapTwo.get(purchaseCategoryFourDto.getCategoryNameTwo());
                String categoryCodeThree = purchaseCategoryFourDto.getCategoryCodeThree();
                if (StringUtil.notEmpty(categoryCodeThree)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeThree)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeThree);
                        purchaseCategory.setCategoryName(purchaseCategoryFourDto.getCategoryNameThree());
                        purchaseCategory.setLevel(3);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryFourDto.getEnabledThree());
                        this.save(purchaseCategory);
                        categoryHashMapThree.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFourDto.getCategoryNameThree());
                        category.setEnabled(purchaseCategoryFourDto.getEnabledThree());
                        this.updateById(category);
                        categoryHashMapThree.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFourDto.getCategoryNameThree());
                    purchaseCategory.setLevel(3);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryFourDto.getEnabledThree());
                    this.save(purchaseCategory);
                    categoryHashMapThree.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ???????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryFourDtos)) {
            for (PurchaseCategoryFourDto purchaseCategoryFourDto : purchaseCategoryFourDtos) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapThree.get(purchaseCategoryFourDto.getCategoryNameThree());
                String categoryCodeFour = purchaseCategoryFourDto.getCategoryCodeFour();
                if (StringUtil.notEmpty(categoryCodeFour)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeFour)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeFour);
                        purchaseCategory.setCategoryName(purchaseCategoryFourDto.getCategoryNameFour());
                        purchaseCategory.setLevel(4);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryFourDto.getEnabledFour());
                        this.save(purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryFourDto.getCategoryNameFour());
                        category.setEnabled(purchaseCategoryFourDto.getEnabledFour());
                        this.updateById(category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryFourDto.getCategoryNameFour());
                    purchaseCategory.setLevel(4);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryFourDto.getEnabledFour());
                    this.save(purchaseCategory);
                }
            }
        }
    }

    @Transactional
    public void savePurchaseCategoryThree(ArrayList<PurchaseCategoryThreeDto> purchaseCategoryThreeDtos) {
        HashMap<String, PurchaseCategory> categoryHashMapOne = new HashMap<>();
        HashMap<String, PurchaseCategory> categoryHashMapTwo = new HashMap<>();

        // ???????????????,
        ArrayList<PurchaseCategoryThreeDto> collectOne = purchaseCategoryThreeDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryThreeDto::getCategoryNameOne))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectOne)) {
            for (PurchaseCategoryThreeDto purchaseCategoryThreeDto : collectOne) {
                String categoryCodeOne = purchaseCategoryThreeDto.getCategoryCodeOne();
                if (StringUtil.notEmpty(categoryCodeOne)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeOne).setLevel(1)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeOne);
                        purchaseCategory.setCategoryName(purchaseCategoryThreeDto.getCategoryNameOne());
                        purchaseCategory.setLevel(1);
                        purchaseCategory.setParentId(-1L);
                        purchaseCategory.setStruct(String.valueOf(id));
                        purchaseCategory.setEnabled(purchaseCategoryThreeDto.getEnabledOne());
                        this.save(purchaseCategory);
                        categoryHashMapOne.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryThreeDto.getCategoryNameOne());
                        category.setEnabled(purchaseCategoryThreeDto.getEnabledOne());
                        this.updateById(category);
                        categoryHashMapOne.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryThreeDto.getCategoryNameOne());
                    purchaseCategory.setLevel(1);
                    purchaseCategory.setParentId(-1L);
                    purchaseCategory.setStruct(String.valueOf(id));
                    purchaseCategory.setEnabled(purchaseCategoryThreeDto.getEnabledOne());
                    this.save(purchaseCategory);
                    categoryHashMapOne.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ????????????
        ArrayList<PurchaseCategoryThreeDto> collectTwo = purchaseCategoryThreeDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryThreeDto::getCategoryNameTwo))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectTwo)) {
            for (PurchaseCategoryThreeDto purchaseCategoryThreeDto : collectTwo) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapOne.get(purchaseCategoryThreeDto.getCategoryNameOne());
                // ??????id??????
                String categoryCodeTwo = purchaseCategoryThreeDto.getCategoryCodeTwo();
                if (StringUtil.notEmpty(categoryCodeTwo)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeTwo).setLevel(2)).like("STRUCT",purchase.getStruct()));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeTwo);
                        purchaseCategory.setCategoryName(purchaseCategoryThreeDto.getCategoryNameTwo());
                        purchaseCategory.setLevel(2);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryThreeDto.getEnabledTwo());
                        this.save(purchaseCategory);
                        categoryHashMapTwo.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryThreeDto.getCategoryNameTwo());
                        category.setEnabled(purchaseCategoryThreeDto.getEnabledTwo());
                        this.updateById(category);
                        categoryHashMapTwo.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryThreeDto.getCategoryNameTwo());
                    purchaseCategory.setLevel(2);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryThreeDto.getEnabledTwo());
                    this.save(purchaseCategory);
                    categoryHashMapTwo.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ???????????????
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategoryThreeDtos)) {
            for (PurchaseCategoryThreeDto purchaseCategoryThreeDto : purchaseCategoryThreeDtos) {
                // ????????????
                PurchaseCategory purchase = categoryHashMapTwo.get(purchaseCategoryThreeDto.getCategoryNameTwo());
                String categoryCodeThree = purchaseCategoryThreeDto.getCategoryCodeThree();
                if (StringUtil.notEmpty(categoryCodeThree)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeThree).setLevel(3)).like("STRUCT",purchase.getStruct()));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeThree);
                        purchaseCategory.setCategoryName(purchaseCategoryThreeDto.getCategoryNameThree());
                        purchaseCategory.setLevel(3);
                        purchaseCategory.setParentId(purchase.getCategoryId());
                        purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                        purchaseCategory.setEnabled(purchaseCategoryThreeDto.getEnabledThree());
                        purchaseCategory.setMainMaterial(purchaseCategoryThreeDto.getMainMaterial());
                        this.save(purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryThreeDto.getCategoryNameThree());
                        category.setEnabled(purchaseCategoryThreeDto.getEnabledThree());
                        category.setMainMaterial(purchaseCategoryThreeDto.getMainMaterial());
                        this.updateById(category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryThreeDto.getCategoryNameThree());
                    purchaseCategory.setLevel(3);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getStruct() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryThreeDto.getEnabledThree());
                    purchaseCategory.setMainMaterial(purchaseCategoryThreeDto.getMainMaterial());
                    this.save(purchaseCategory);
                }
            }
        }
    }

    public void savePurchaseCategoryTwo(ArrayList<PurchaseCategoryTwoDto> purchaseCategoryTwoDtos) {
        HashMap<String, PurchaseCategory> categoryHashMapOne = new HashMap<>();

        // ???????????????,
        ArrayList<PurchaseCategoryTwoDto> collectOne = purchaseCategoryTwoDtos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(PurchaseCategoryTwoDto::getCategoryNameOne))), ArrayList::new));
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(collectOne)) {
            for (PurchaseCategoryTwoDto purchaseCategoryTwoDto : collectOne) {
                String categoryCodeOne = purchaseCategoryTwoDto.getCategoryCodeOne();
                if (StringUtil.notEmpty(categoryCodeOne)) {
                    // ??????????????????, ???????????????, ??????????????????
                    PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeOne)));
                    if (null == category) {
                        PurchaseCategory purchaseCategory = new PurchaseCategory();
                        long id = IdGenrator.generate();
                        purchaseCategory.setCategoryId(id);
                        purchaseCategory.setCategoryCode(categoryCodeOne);
                        purchaseCategory.setCategoryName(purchaseCategoryTwoDto.getCategoryNameOne());
                        purchaseCategory.setLevel(1);
                        purchaseCategory.setParentId(-1L);
                        purchaseCategory.setStruct(String.valueOf(id));
                        purchaseCategory.setEnabled(purchaseCategoryTwoDto.getEnabledOne());
                        this.save(purchaseCategory);
                        categoryHashMapOne.put(purchaseCategory.getCategoryName(), purchaseCategory);
                    } else {
                        // ??????
                        category.setCategoryName(purchaseCategoryTwoDto.getCategoryNameOne());
                        category.setEnabled(purchaseCategoryTwoDto.getEnabledOne());
                        this.updateById(category);
                        categoryHashMapOne.put(category.getCategoryName(), category);
                    }
                } else {
                    // ??????
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                    purchaseCategory.setCategoryName(purchaseCategoryTwoDto.getCategoryNameOne());
                    purchaseCategory.setLevel(1);
                    purchaseCategory.setParentId(-1L);
                    purchaseCategory.setStruct(String.valueOf(id));
                    purchaseCategory.setEnabled(purchaseCategoryTwoDto.getEnabledOne());
                    this.save(purchaseCategory);
                    categoryHashMapOne.put(purchaseCategory.getCategoryName(), purchaseCategory);
                }
            }
        }

        // ????????????
        for (PurchaseCategoryTwoDto purchaseCategoryTwoDto : purchaseCategoryTwoDtos) {
            PurchaseCategory purchase = categoryHashMapOne.get(purchaseCategoryTwoDto.getCategoryNameOne());
            String categoryCodeTwo = purchaseCategoryTwoDto.getCategoryCodeTwo();
            if (StringUtil.notEmpty(categoryCodeTwo)) {
                // ??????????????????, ???????????????, ??????????????????
                PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeTwo)));
                if (null == category) {
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(categoryCodeTwo);
                    purchaseCategory.setCategoryName(purchaseCategoryTwoDto.getCategoryNameTwo());
                    purchaseCategory.setLevel(2);
                    purchaseCategory.setParentId(purchase.getCategoryId());
                    purchaseCategory.setStruct(purchase.getCategoryId() + "-" + id);
                    purchaseCategory.setEnabled(purchaseCategoryTwoDto.getEnabledTwo());
                    this.save(purchaseCategory);
                } else {
                    // ??????
                    category.setCategoryName(purchaseCategoryTwoDto.getCategoryNameTwo());
                    category.setEnabled(purchaseCategoryTwoDto.getEnabledTwo());
                    this.updateById(category);
                }
            } else {
                // ??????
                PurchaseCategory purchaseCategory = new PurchaseCategory();
                long id = IdGenrator.generate();
                purchaseCategory.setCategoryId(id);
                purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                purchaseCategory.setCategoryName(purchaseCategoryTwoDto.getCategoryNameTwo());
                purchaseCategory.setLevel(2);
                purchaseCategory.setParentId(purchase.getCategoryId());
                purchaseCategory.setStruct(purchase.getCategoryId() + "-" + id);
                purchaseCategory.setEnabled(purchaseCategoryTwoDto.getEnabledTwo());
                this.save(purchaseCategory);
            }
        }
    }

    public Map<String, Object> success() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", YesOrNo.YES.getValue());
        result.put("message", "success");
        return result;
    }

    // ????????????
    public Map<String, Object> handleError(Fileupload fileupload, MultipartFile file, ArrayList<? extends Object> dataList, Class<? extends Object> clazz) {
        Fileupload fileupload1 = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                dataList, clazz, file.getName(), file.getOriginalFilename(), file.getContentType());
        Map<String, Object> result = new HashMap<>();
        result.put("status", YesOrNo.NO.getValue());
        result.put("message", "error");
        result.put("fileuploadId", fileupload1.getFileuploadId());
        result.put("fileName", fileupload1.getFileSourceName());
        return result;
    }

    @Transactional
    public void savePurchaseCategoryOne(ArrayList<PurchaseCategoryOneDto> purchaseCategoryOneDtos) {

        for (PurchaseCategoryOneDto purchaseCategoryOneDto : purchaseCategoryOneDtos) {
            String categoryCodeOne = purchaseCategoryOneDto.getCategoryCodeOne().trim();
            if (StringUtil.notEmpty(categoryCodeOne)) {
                // ??????????????????, ???????????????, ??????????????????
                PurchaseCategory category = this.getOne(new QueryWrapper<>(new PurchaseCategory().setCategoryCode(categoryCodeOne)));
                if (null == category) {
                    PurchaseCategory purchaseCategory = new PurchaseCategory();
                    long id = IdGenrator.generate();
                    purchaseCategory.setCategoryId(id);
                    purchaseCategory.setCategoryCode(categoryCodeOne);
                    purchaseCategory.setCategoryName(purchaseCategoryOneDto.getCategoryNameOne().trim());
                    purchaseCategory.setLevel(1);
                    purchaseCategory.setParentId(-1L);
                    purchaseCategory.setStruct(String.valueOf(id));
                    purchaseCategory.setEnabled(purchaseCategoryOneDto.getEnabledOne().trim());
                    this.save(purchaseCategory);
                } else {
                    // ??????
                    category.setCategoryName(purchaseCategoryOneDto.getCategoryNameOne().trim());
                    category.setEnabled(purchaseCategoryOneDto.getEnabledOne().trim());
                    this.updateById(category);
                }
            } else {
                // ??????
                PurchaseCategory purchaseCategory = new PurchaseCategory();
                long id = IdGenrator.generate();
                purchaseCategory.setCategoryId(id);
                purchaseCategory.setCategoryCode(baseClient.seqGen(SequenceCodeConstant.SEQ_PURCHASE_CATEGORY_CODE));
                purchaseCategory.setCategoryName(purchaseCategoryOneDto.getCategoryNameOne().trim());
                purchaseCategory.setLevel(1);
                purchaseCategory.setParentId(-1L);
                purchaseCategory.setStruct(String.valueOf(id));
                purchaseCategory.setEnabled(purchaseCategoryOneDto.getEnabledOne().trim());
                this.save(purchaseCategory);
            }
        }
    }

    public boolean checkPurchaseCategory5(List<PurchaseCategoryDto> purchaseCategoryDtoList, HashSet<String> hashSet, ArrayList<PurchaseCategoryFiveDto> purchaseCategoryFiveDtos) {
        boolean errorFlag = false;
        HashMap<String, String> hashMapOne = new HashMap<>();
        HashMap<String, String> hashMapTwo = new HashMap<>();
        HashMap<String, String> hashMapThree = new HashMap<>();
        HashMap<String, String> hashMapFour = new HashMap<>();
        for (PurchaseCategoryDto purchaseCategoryTemp : purchaseCategoryDtoList) {
            StringBuffer errorMag = new StringBuffer();
            PurchaseCategoryFiveDto purchaseCategoryFiveDto = (PurchaseCategoryFiveDto) purchaseCategoryTemp;
            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFiveDto.getCategoryNameOne())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFiveDto.setCategoryNameOne(purchaseCategoryFiveDto.getCategoryNameOne().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryCodeOne())) {
                purchaseCategoryFiveDto.setCategoryCodeOne(purchaseCategoryFiveDto.getCategoryCodeOne().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getEnabledOne())) {
                String enabledOne = purchaseCategoryFiveDto.getEnabledOne().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFiveDto.setEnabledOne(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryNameOne())) {
                    if (hashMapOne.containsKey(purchaseCategoryFiveDto.getCategoryNameOne())) {
                        if (!purchaseCategoryFiveDto.getEnabledOne().equals(hashMapOne.get(purchaseCategoryFiveDto.getCategoryNameOne()))) {
                            errorMag.append("???????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapOne.put(purchaseCategoryFiveDto.getCategoryNameOne(), purchaseCategoryFiveDto.getEnabledOne());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFiveDto.getCategoryNameTwo())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFiveDto.setCategoryNameTwo(purchaseCategoryFiveDto.getCategoryNameTwo().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryCodeTwo())) {
                purchaseCategoryFiveDto.setCategoryCodeTwo(purchaseCategoryFiveDto.getCategoryCodeTwo().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getEnabledTwo())) {
                String enabledOne = purchaseCategoryFiveDto.getEnabledTwo().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFiveDto.setEnabledTwo(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryNameTwo())) {
                    if (hashMapTwo.containsKey(purchaseCategoryFiveDto.getCategoryNameTwo())) {
                        if (!purchaseCategoryFiveDto.getEnabledTwo().equals(hashMapOne.get(purchaseCategoryFiveDto.getCategoryNameTwo()))) {
                            errorMag.append("???????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapTwo.put(purchaseCategoryFiveDto.getCategoryNameTwo(), purchaseCategoryFiveDto.getEnabledTwo());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFiveDto.getCategoryNameThree())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFiveDto.setCategoryNameThree(purchaseCategoryFiveDto.getCategoryNameThree().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryCodeThree())) {
                purchaseCategoryFiveDto.setCategoryCodeThree(purchaseCategoryFiveDto.getCategoryCodeThree());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getEnabledThree())) {
                String enabledOne = purchaseCategoryFiveDto.getEnabledThree().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFiveDto.setEnabledThree(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryNameThree())) {
                    if (hashMapThree.containsKey(purchaseCategoryFiveDto.getCategoryNameThree())) {
                        if (!purchaseCategoryFiveDto.getEnabledThree().equals(hashMapOne.get(purchaseCategoryFiveDto.getCategoryNameThree()))) {
                            errorMag.append("??????????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapThree.put(purchaseCategoryFiveDto.getCategoryNameThree(), purchaseCategoryFiveDto.getEnabledThree());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFiveDto.getCategoryNameFour())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFiveDto.setCategoryNameFour(purchaseCategoryFiveDto.getCategoryNameFour().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryCodeFour())) {
                purchaseCategoryFiveDto.setCategoryCodeFour(purchaseCategoryFiveDto.getCategoryCodeFour().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getEnabledFour())) {
                String enabledOne = purchaseCategoryFiveDto.getEnabledFour().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFiveDto.setEnabledFour(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryNameFour())) {
                    if (hashMapFour.containsKey(purchaseCategoryFiveDto.getCategoryNameFour())) {
                        if (!purchaseCategoryFiveDto.getEnabledFour().equals(hashMapOne.get(purchaseCategoryFiveDto.getCategoryNameFour()))) {
                            errorMag.append("??????????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapFour.put(purchaseCategoryFiveDto.getCategoryNameFour(), purchaseCategoryFiveDto.getEnabledFour());
                    }
                }
            } else {
                errorMag.append("????????????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFiveDto.getCategoryNameFive())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFiveDto.setCategoryNameFive(purchaseCategoryFiveDto.getCategoryNameFive().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryCodeFive())) {
                purchaseCategoryFiveDto.setCategoryNameFive(purchaseCategoryFiveDto.getCategoryCodeFive().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getEnabledFive())) {
                String enabledOne = purchaseCategoryFiveDto.getEnabledFive().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFiveDto.setEnabledFive(enabledOne);
            } else {
                errorMag.append("????????????????????????????????????; ");
                errorFlag = true;
            }
            if (StringUtil.notEmpty(purchaseCategoryFiveDto.getCategoryCodeFive()) && !hashSet.add(purchaseCategoryFiveDto.getCategoryCodeFive())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            if (errorMag.length() > 1) {
                purchaseCategoryFiveDto.setErrorMsg(errorMag.toString());
            }
            purchaseCategoryFiveDtos.add(purchaseCategoryFiveDto);
        }
        hashSet.clear();
        return errorFlag;
    }

    public boolean checkPurchaseCategory4(List<PurchaseCategoryDto> purchaseCategoryDtoList, HashSet<String> hashSet, ArrayList<PurchaseCategoryFourDto> purchaseCategoryFourDtos) {
        boolean errorFlag = false;
        HashMap<String, String> hashMapOne = new HashMap<>();
        HashMap<String, String> hashMapTwo = new HashMap<>();
        HashMap<String, String> hashMapThree = new HashMap<>();
        for (PurchaseCategoryDto purchaseCategoryTemp : purchaseCategoryDtoList) {
            StringBuffer errorMag = new StringBuffer();
            PurchaseCategoryFourDto purchaseCategoryFourDto = (PurchaseCategoryFourDto) purchaseCategoryTemp;
            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFourDto.getCategoryNameOne())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFourDto.setCategoryNameOne(purchaseCategoryFourDto.getCategoryNameOne().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getCategoryCodeOne())) {
                purchaseCategoryFourDto.setCategoryCodeOne(purchaseCategoryFourDto.getCategoryCodeOne().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getEnabledOne())) {
                String enabledOne = purchaseCategoryFourDto.getEnabledOne().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFourDto.setEnabledOne(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryFourDto.getCategoryNameOne())) {
                    if (hashMapOne.containsKey(purchaseCategoryFourDto.getCategoryNameOne())) {
                        if (!purchaseCategoryFourDto.getEnabledOne().equals(hashMapOne.get(purchaseCategoryFourDto.getCategoryNameOne()))) {
                            errorMag.append("???????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapOne.put(purchaseCategoryFourDto.getCategoryNameOne(), purchaseCategoryFourDto.getEnabledOne());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFourDto.getCategoryNameTwo())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFourDto.setCategoryNameTwo(purchaseCategoryFourDto.getCategoryNameTwo().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getCategoryCodeTwo())) {
                purchaseCategoryFourDto.setCategoryCodeTwo(purchaseCategoryFourDto.getCategoryCodeTwo().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getEnabledTwo())) {
                String enabledOne = purchaseCategoryFourDto.getEnabledTwo().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFourDto.setEnabledTwo(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryFourDto.getCategoryNameTwo())) {
                    if (hashMapTwo.containsKey(purchaseCategoryFourDto.getCategoryNameTwo())) {
                        if (!purchaseCategoryFourDto.getEnabledTwo().equals(hashMapOne.get(purchaseCategoryFourDto.getCategoryNameTwo()))) {
                            errorMag.append("???????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapTwo.put(purchaseCategoryFourDto.getCategoryNameTwo(), purchaseCategoryFourDto.getEnabledTwo());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFourDto.getCategoryNameThree())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFourDto.setCategoryNameThree(purchaseCategoryFourDto.getCategoryNameThree().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getCategoryCodeThree())) {
                purchaseCategoryFourDto.setCategoryCodeThree(purchaseCategoryFourDto.getCategoryCodeThree().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getEnabledThree())) {
                String enabledOne = purchaseCategoryFourDto.getEnabledThree().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFourDto.setEnabledThree(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryFourDto.getCategoryNameThree())) {
                    if (hashMapThree.containsKey(purchaseCategoryFourDto.getCategoryNameThree())) {
                        if (!purchaseCategoryFourDto.getEnabledThree().equals(hashMapOne.get(purchaseCategoryFourDto.getCategoryNameThree()))) {
                            errorMag.append("???????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapThree.put(purchaseCategoryFourDto.getCategoryNameThree(), purchaseCategoryFourDto.getEnabledThree());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryFourDto.getCategoryNameFour())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryFourDto.setCategoryNameFour(purchaseCategoryFourDto.getCategoryNameFour().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getCategoryCodeFour())) {
                purchaseCategoryFourDto.setCategoryCodeFour(purchaseCategoryFourDto.getCategoryCodeFour().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getEnabledFour())) {
                String enabledOne = purchaseCategoryFourDto.getEnabledFour().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryFourDto.setEnabledFour(enabledOne);
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (StringUtil.notEmpty(purchaseCategoryFourDto.getCategoryCodeFour()) && !hashSet.add(purchaseCategoryFourDto.getCategoryCodeFour())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            if (errorMag.length() > 1) {
                purchaseCategoryFourDto.setErrorMsg(errorMag.toString());
            }
            purchaseCategoryFourDtos.add(purchaseCategoryFourDto);
        }
        hashSet.clear();
        return errorFlag;
    }

    public boolean checkPurchaseCategory3(List<PurchaseCategoryDto> purchaseCategoryDtoList, HashSet<String> hashSet, ArrayList<PurchaseCategoryThreeDto> purchaseCategoryThreeDtos) {
        boolean errorFlag = false;
        HashMap<String, String> hashMapOne = new HashMap<>();
        HashMap<String, String> hashMapTwo = new HashMap<>();
        HashSet<String> code = new HashSet<>();
        StringBuffer onlyCode = new StringBuffer();
        for (PurchaseCategoryDto purchaseCategoryTemp : purchaseCategoryDtoList) {
            StringBuffer errorMag = new StringBuffer();
            PurchaseCategoryThreeDto purchaseCategoryThreeDto = (PurchaseCategoryThreeDto) purchaseCategoryTemp;
            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryThreeDto.getCategoryNameOne())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryThreeDto.setCategoryNameOne(purchaseCategoryThreeDto.getCategoryNameOne().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryThreeDto.getCategoryCodeOne())) {
                String categoryCodeOne = purchaseCategoryThreeDto.getCategoryCodeOne().trim();
                purchaseCategoryThreeDto.setCategoryCodeOne(categoryCodeOne);
                onlyCode.append(categoryCodeOne);
            }else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (StringUtil.notEmpty(purchaseCategoryThreeDto.getEnabledOne())) {
                String enabledOne = purchaseCategoryThreeDto.getEnabledOne().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryThreeDto.setEnabledOne(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryThreeDto.getCategoryNameOne())) {
                    if (hashMapOne.containsKey(purchaseCategoryThreeDto.getCategoryNameOne())) {
                        if (!purchaseCategoryThreeDto.getEnabledOne().equals(hashMapOne.get(purchaseCategoryThreeDto.getCategoryNameOne()))) {
                            errorMag.append("???????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapOne.put(purchaseCategoryThreeDto.getCategoryNameOne(), purchaseCategoryThreeDto.getEnabledOne());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryThreeDto.getCategoryNameTwo())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryThreeDto.setCategoryNameTwo(purchaseCategoryThreeDto.getCategoryNameTwo().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryThreeDto.getCategoryCodeTwo())) {
                String categoryCodeTwo = purchaseCategoryThreeDto.getCategoryCodeTwo().trim();
                purchaseCategoryThreeDto.setCategoryCodeTwo(categoryCodeTwo);
                onlyCode.append(categoryCodeTwo);
            }else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (StringUtil.notEmpty(purchaseCategoryThreeDto.getEnabledTwo())) {
                String enabledOne = purchaseCategoryThreeDto.getEnabledTwo().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryThreeDto.setEnabledTwo(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryThreeDto.getCategoryNameTwo())) {
                    if (hashMapTwo.containsKey(purchaseCategoryThreeDto.getCategoryNameTwo())) {
                        if (!purchaseCategoryThreeDto.getEnabledTwo().equals(hashMapTwo.get(purchaseCategoryThreeDto.getCategoryNameTwo()))) {
                            errorMag.append("???????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapTwo.put(purchaseCategoryThreeDto.getCategoryNameTwo(), purchaseCategoryThreeDto.getEnabledTwo());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryThreeDto.getCategoryNameThree())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryThreeDto.setCategoryNameThree(purchaseCategoryThreeDto.getCategoryNameThree().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryThreeDto.getCategoryCodeThree())) {
                String categoryCodeThree = purchaseCategoryThreeDto.getCategoryCodeThree().trim();
                purchaseCategoryThreeDto.setCategoryCodeThree(categoryCodeThree);
                onlyCode.append(categoryCodeThree);
            }else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (!StringUtil.isEmpty(purchaseCategoryThreeDto.getMainMaterial())) {
                purchaseCategoryThreeDto.setMainMaterial(purchaseCategoryThreeDto.getMainMaterial().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryThreeDto.getEnabledThree())) {
                String enabledOne = purchaseCategoryThreeDto.getEnabledThree().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryThreeDto.setEnabledThree(enabledOne);
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (!hashSet.add(onlyCode.toString())) {
                errorMag.append("?????????????????????????????????; ");
                errorFlag = true;
            }

            if (errorMag.length() > 1) {
                purchaseCategoryThreeDto.setErrorMsg(errorMag.toString());
            }
            purchaseCategoryThreeDtos.add(purchaseCategoryThreeDto);
        }
        hashSet.clear();
        return errorFlag;
    }

    public boolean checkPurchaseCategory2(List<PurchaseCategoryDto> purchaseCategoryDtoList, HashSet<String> hashSet, ArrayList<PurchaseCategoryTwoDto> purchaseCategoryTwoDtos) {
        boolean errorFlag = false;
        HashMap<String, String> hashMapOne = new HashMap<>();
        for (PurchaseCategoryDto purchaseCategoryTemp : purchaseCategoryDtoList) {
            StringBuffer errorMag = new StringBuffer();
            PurchaseCategoryTwoDto purchaseCategoryTwoDto = (PurchaseCategoryTwoDto) purchaseCategoryTemp;
            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryTwoDto.getCategoryNameOne())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryTwoDto.setCategoryNameOne(purchaseCategoryTwoDto.getCategoryNameOne().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryTwoDto.getCategoryCodeOne())) {
                purchaseCategoryTwoDto.setCategoryCodeOne(purchaseCategoryTwoDto.getCategoryCodeOne().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryTwoDto.getEnabledOne())) {
                String enabledOne = purchaseCategoryTwoDto.getEnabledOne().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryTwoDto.setEnabledOne(enabledOne);
                if (StringUtil.notEmpty(purchaseCategoryTwoDto.getCategoryNameOne())) {
                    if (hashMapOne.containsKey(purchaseCategoryTwoDto.getCategoryNameOne().trim())) {
                        if (!purchaseCategoryTwoDto.getEnabledOne().equals(hashMapOne.get(purchaseCategoryTwoDto.getCategoryNameOne().trim()))) {
                            errorMag.append("???????????????????????????????????????); ");
                            errorFlag = true;
                        }
                    } else {
                        hashMapOne.put(purchaseCategoryTwoDto.getCategoryNameOne().trim(), purchaseCategoryTwoDto.getEnabledOne().trim());
                    }
                }
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }

            // ????????????
            if (StringUtil.isEmpty(purchaseCategoryTwoDto.getCategoryNameTwo())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryTwoDto.setCategoryNameTwo(purchaseCategoryTwoDto.getCategoryNameTwo().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryTwoDto.getCategoryCodeTwo())) {
                purchaseCategoryTwoDto.setCategoryCodeTwo(purchaseCategoryTwoDto.getCategoryCodeTwo().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryTwoDto.getEnabledTwo())) {
                String enabledOne = purchaseCategoryTwoDto.getEnabledTwo().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryTwoDto.setEnabledTwo(enabledOne);
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (StringUtil.notEmpty(purchaseCategoryTwoDto.getCategoryCodeTwo()) && !hashSet.add(purchaseCategoryTwoDto.getCategoryCodeTwo().trim())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (errorMag.length() > 1) {
                purchaseCategoryTwoDto.setErrorMsg(errorMag.toString());
            }
            purchaseCategoryTwoDtos.add(purchaseCategoryTwoDto);
        }
        hashSet.clear();
        return errorFlag;
    }

    public boolean checkPurchaseCategory1(List<PurchaseCategoryDto> purchaseCategoryDtoList, HashSet<String> hashSet, ArrayList<PurchaseCategoryOneDto> purchaseCategoryOneDtos) {
        boolean errorFlag = false;
        for (PurchaseCategoryDto purchaseCategoryTemp : purchaseCategoryDtoList) {

            StringBuffer errorMag = new StringBuffer();
            PurchaseCategoryOneDto purchaseCategoryOneDto = (PurchaseCategoryOneDto) purchaseCategoryTemp;
            if (StringUtil.isEmpty(purchaseCategoryOneDto.getCategoryNameOne())) {
                errorMag.append("????????????????????????; ");
                errorFlag = true;
            } else {
                purchaseCategoryOneDto.setCategoryNameOne(purchaseCategoryOneDto.getCategoryNameOne().trim());
            }
            if (StringUtil.notEmpty(purchaseCategoryOneDto.getCategoryCodeOne())) {
                purchaseCategoryOneDto.setCategoryCodeOne(purchaseCategoryOneDto.getCategoryCodeOne().trim());
            }
            if (!hashSet.add(purchaseCategoryOneDto.getCategoryNameOne())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (StringUtil.notEmpty(purchaseCategoryOneDto.getCategoryCodeOne()) && !hashSet.add(purchaseCategoryOneDto.getCategoryCodeOne().trim())) {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (StringUtil.notEmpty(purchaseCategoryOneDto.getEnabledOne())) {
                String enabledOne = purchaseCategoryOneDto.getEnabledOne().trim();
                if (!"N".equals(enabledOne) && !"Y".equals(enabledOne)) {
                    errorMag.append("???????????????????????????(N/Y); ");
                    errorFlag = true;
                }
                purchaseCategoryOneDto.setEnabledOne(purchaseCategoryOneDto.getEnabledOne().trim());
            } else {
                errorMag.append("??????????????????????????????; ");
                errorFlag = true;
            }
            if (errorMag.length() > 0) {
                purchaseCategoryOneDto.setErrorMsg(errorMag.toString());
            }
            purchaseCategoryOneDtos.add(purchaseCategoryOneDto);
        }
        hashSet.clear();
        return errorFlag;
    }

    /**
     * ???????????????excel??????
     *
     * @param file
     * @param fileupload
     * @return
     * @throws IOException
     */
    public List<PurchaseCategoryDto> getPurchaseCategoryDtoList(MultipartFile file, Fileupload fileupload, PurchaseCategoryDto purchaseCategoryDto) throws IOException {
        // ????????????
        EasyExcelUtil.checkParam(file, fileupload);
        List<PurchaseCategoryDto> purchaseCategoryDtos = null;
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<PurchaseCategoryDto> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream, listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(purchaseCategoryDto.getClass()).headRowNumber(2).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            purchaseCategoryDtos = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return purchaseCategoryDtos;
    }

    @Override
    public List<PurchaseCategory> queryByParam(String param, String enabled) {
        return this.getBaseMapper().queryByParam(param, enabled);
    }

    @Override
    public void SaveOrUpdateByCategoryFullCode(String categoryFullCode, String categoryFullName) {
        String fullName[] = categoryFullName.split("-");
        int len = fullName.length;
        for (int i = 0; i < len; i++) {
            String subCode = categoryFullCode.substring(0, (i + 1) * 2);
            PurchaseCategory subCategory = new PurchaseCategory();
            subCategory.setCategoryCode(subCode);
            List<PurchaseCategory> subCategoryList = this.list(new QueryWrapper<>(subCategory));
            if (CollectionUtils.isEmpty(subCategoryList)) {
                //?????????????????????????????????
                PurchaseCategory saveSubPurchaseCategory = new PurchaseCategory();
                Long subCategoryId = IdGenrator.generate();
                saveSubPurchaseCategory.setCategoryId(subCategoryId);
                saveSubPurchaseCategory.setCategoryCode(subCode);
                String subCategoryName = fullName[i];
                saveSubPurchaseCategory.setCategoryName(subCategoryName);
                saveSubPurchaseCategory.setLevel(i + 1);
                if (i == 0) {
                    saveSubPurchaseCategory.setParentId(Long.valueOf(-1));
                    saveSubPurchaseCategory.setStruct(String.valueOf(subCategoryId));
                } else {
                    String parentCategoryCode = categoryFullCode.substring(0, i * 2);
                    PurchaseCategory parentCategory = new PurchaseCategory();
                    parentCategory.setCategoryCode(parentCategoryCode);
                    QueryWrapper<PurchaseCategory> parentCategoryqueryWrapper = new QueryWrapper<>(parentCategory);
                    PurchaseCategory queryedParentCategory = this.getOne(parentCategoryqueryWrapper);
                    Long parentCategoryId = queryedParentCategory.getCategoryId();
                    saveSubPurchaseCategory.setParentId(parentCategoryId);
                    String struct = queryedParentCategory.getStruct() + "-" + String.valueOf(subCategoryId);
                    saveSubPurchaseCategory.setStruct(struct);
                }
                saveSubPurchaseCategory.setEnabled("Y");
                this.saveOrUpdate(saveSubPurchaseCategory);
            }
        }
    }

    //???????????????????????????
    public void setCategoryFullName(PurchaseCategory purchaseCategory) {
        String struct = purchaseCategory.getStruct();
        List<Long> categoryIds = StringUtil.stringConvertNumList(struct, "-");
        if (!CollectionUtils.isEmpty(categoryIds)) {
            QueryWrapper<PurchaseCategory> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("CATEGORY_ID", categoryIds);
            List<PurchaseCategory> list = purchaseCategoryMapper.selectList(queryWrapper);
            String[] categoryFullNameArr = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                categoryFullNameArr[i] = list.get(i).getCategoryName();
            }
            String categoryFullName = String.join("-", categoryFullNameArr);
            purchaseCategory.setCategoryFullName(categoryFullName);
        }
    }

    @Override
    public List<PurchaseCategory> queryCategoryByType(String name, Integer level, String enabled) {
        return this.getBaseMapper().queryCategoryByType(name, level, enabled);
    }

    @Override
    public List<Integer> getCategoryLevel() {
        String maxLevel = getMaxLevel();
        List<Integer> levelList = new ArrayList<>();
        if (StringUtil.notEmpty(maxLevel)) {
            int num = Integer.parseInt(maxLevel);
            for (int i = num; i > 0; i--) {
                levelList.add(i);
            }
        }
        return levelList;
    }

    @Override
    public List<PurchaseCategory> queryMinLevelCategory(List<PurchaseCategory> purchaseCategories) {
        String maxLevel = getMaxLevel();
        List<PurchaseCategory> categories = new ArrayList<>();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategories)) {
            for (PurchaseCategory purchaseCategory : purchaseCategories) {
                Long categoryId = purchaseCategory.getCategoryId();
                QueryWrapper<PurchaseCategory> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("LEVEL", maxLevel);
                queryWrapper.like("STRUCT", categoryId);
                List<PurchaseCategory> list = this.list(queryWrapper);
                categories.addAll(list);
            }
        }
        return categories;
    }

    /**
     * ??????????????????
     *
     * @param purchaseCategory
     * @return
     */
    @Override
    public PurchaseCategory queryMaxLevelCategory(PurchaseCategory purchaseCategory) {
        PurchaseCategory item = this.getById(purchaseCategory.getCategoryId());
        checkIfCorrect(item);
        while (item.getLevel() != 1) {
            item = this.getById(item.getParentId());
            checkIfCorrect(item);
        }
        return item;
    }

    private void checkIfCorrect(PurchaseCategory item) {
        Assert.notNull(item, LocaleHandler.getLocaleMsg("????????????????????????,??????id??????" + item.getCategoryId()));
        if (item.getLevel() != 1 &&
                item.getLevel() != 2 &&
                item.getLevel() != 3
        ) {
            Assert.notNull(null, LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        }
    }


    //??????????????????????????????????????????????????????????????????????????????
    @Override
    @Transactional(propagation = Propagation.SUPPORTS )
    public List<MaterialMaxCategoryVO> queryCategoryMaxCodeByMaterialIds(Collection<Long> materialIds) {
        List<MaterialMaxCategoryVO> max = new ArrayList<>(materialIds.size());
        //?????????????????????????????????????????????id
        List<MaterialItem> materialItems = iMaterialItemService.list(Wrappers.lambdaQuery(MaterialItem.class)
                .select(MaterialItem::getCategoryId, MaterialItem::getMaterialId)
                .in(MaterialItem::getMaterialId, materialIds)
                .isNotNull(MaterialItem::getCategoryId)
        );
        if (CollectionUtils.isEmpty(materialItems)) {
            return Collections.emptyList();
        }
        //??????????????????id????????????????????????
        Map<Long, Long> materialAndSubCategoryInfo = materialItems.stream().collect(Collectors.toMap(MaterialItem::getMaterialId, MaterialItem::getCategoryId));
        //????????????????????????
        setMaxLevelCategoryId(materialAndSubCategoryInfo);
        //????????????????????????code
        List<PurchaseCategory> list = list(Wrappers.lambdaQuery(PurchaseCategory.class)
                .select(PurchaseCategory::getCategoryId, PurchaseCategory::getCategoryCode)
                .in(PurchaseCategory::getCategoryId, materialAndSubCategoryInfo.values()));
        materialAndSubCategoryInfo.forEach((k, v) -> {
            MaterialMaxCategoryVO categoryVO = new MaterialMaxCategoryVO();
            for (PurchaseCategory purchaseCategory : list) {
                //??????code???????????????
                if (Objects.equals(purchaseCategory.getCategoryId(), v)) {
                    categoryVO.setCategoryCode(purchaseCategory.getCategoryCode());
                    categoryVO.setMaterialId(k);
                    categoryVO.setCategoryId(purchaseCategory.getCategoryId());
                    max.add(categoryVO);
                    break;
                }
            }
        });
        return max;
    }
    private void setMaxLevelCategoryId(Map<Long, Long> materialAndSubCategoryInfo) {
        if(CollectionUtils.isEmpty(materialAndSubCategoryInfo.values())){
            return;
        }
        //????????????????????????
        List<PurchaseCategory> list = list(Wrappers.lambdaQuery(PurchaseCategory.class)
                .select(PurchaseCategory::getCategoryId, PurchaseCategory::getParentId)
                .in(PurchaseCategory::getCategoryId, materialAndSubCategoryInfo.values()));
        //??????????????????-1???????????????
        for (int i = list.size() - 1; i >= 0; i--) {
            PurchaseCategory current = list.get(i);
            if (Objects.isNull(current.getParentId())||Objects.equals(current.getParentId(), Long.valueOf(-1))) {
                list.remove(i);
            }
        }
        //????????????????????????????????????????????????
        while (list.size() != 0) {
            for (Map.Entry<Long, Long> entry : materialAndSubCategoryInfo.entrySet()) {
                for (PurchaseCategory purchaseCategory : list) {
                    //???????????????????????????id????????????????????????id????????????
                    if (Objects.equals(entry.getValue(), purchaseCategory.getCategoryId())) {
                        //??????????????????????????????-1??????-1???????????????
                        if (!Objects.equals(purchaseCategory.getParentId(), Long.valueOf(-1))) {
                            entry.setValue(purchaseCategory.getParentId());
                        }
                        break;
                    }
                }
            }
            //???????????????????????????????????????????????????????????????
            list = list(Wrappers.lambdaQuery(PurchaseCategory.class)
                    .select(PurchaseCategory::getCategoryId, PurchaseCategory::getParentId)
                    .in(PurchaseCategory::getCategoryId, materialAndSubCategoryInfo.values()));
            //???????????????-1???????????????
            for (int i = list.size() - 1; i >= 0; i--) {
                PurchaseCategory current = list.get(i);
                if (Objects.isNull(current.getParentId())||Objects.equals(current.getParentId(), Long.valueOf(-1))) {
                    list.remove(i);
                }
            }
        }

    }
}
