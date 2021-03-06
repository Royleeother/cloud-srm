package com.midea.cloud.srm.base.categorydv.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.base.categorydv.mapper.CategoryDvMapper;
import com.midea.cloud.srm.base.categorydv.service.ICategoryDvService;
import com.midea.cloud.srm.base.categorydv.utils.ExportUtils;
import com.midea.cloud.srm.base.organization.service.IOrganizationService;
import com.midea.cloud.srm.base.organization.service.IOrganizationUserService;
import com.midea.cloud.srm.base.purchase.service.IPurchaseCategoryService;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.base.categorydv.dto.CategoryDvDTO;
import com.midea.cloud.srm.model.base.categorydv.dto.CategoryDvImport;
import com.midea.cloud.srm.model.base.categorydv.dto.DvRequestDTO;
import com.midea.cloud.srm.model.base.categorydv.entity.CategoryDv;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationUser;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.common.ExportExcelParam;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
*  <pre>
 *  ???????????? ???????????????
 * </pre>
*
* @author zhuwl7@meicloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-03-06 10:04:24
 *  ????????????:
 * </pre>
*/
@Service
public class CategoryDvServiceImpl extends ServiceImpl<CategoryDvMapper, CategoryDv> implements ICategoryDvService {
    @Autowired
    private IPurchaseCategoryService iPurchaseCategoryService;
    @Resource
    private CategoryDvMapper categoryDvMapper;
    @Resource
    private IOrganizationService organizationService;
    @Resource
    private IOrganizationUserService organizationUserService;
    @Resource
    private RbacClient rbacClient;
    @Resource
    private FileCenterClient fileCenterClient;

    @Override
    @Transactional
    public void saveOrUpdateDv(CategoryDv categoryDv) {
        if(categoryDv.getCategoryId() == null
                || categoryDv.getUserId() == null
                || categoryDv.getStartDate() == null){
            throw  new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????"));
        }
        if(categoryDv.getEndDate() != null){
            if(DateChangeUtil.asDate(categoryDv.getEndDate()).before(new Date())){
                throw  new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????"));
            }
        }
        List<CategoryDv> dvs = this.checkExist(categoryDv);
        if(!CollectionUtils.isEmpty(dvs)){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????,?????????!"));
        }
        PurchaseCategory category = iPurchaseCategoryService.getById(categoryDv.getCategoryId());
        if(category == null ||  YesOrNo.NO.getValue().equals(category.getEnabled())){
            throw  new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????"));
        }
        if(categoryDv.getCategoryDvId() != null){
            categoryDv.setLastUpdateDate(new Date());
        }else{
            Long id = IdGenrator.generate();
            categoryDv.setCategoryDvId(id);
            categoryDv.setCreationDate(new Date());
        }
        this.saveOrUpdate(categoryDv);
    }

    private List<CategoryDv> checkExist(CategoryDv categoryDv) {
        CategoryDv query = new CategoryDv();
        query.setCategoryId(categoryDv.getCategoryId());
        query.setStartDate(categoryDv.getStartDate());
        query.setUserId(categoryDv.getUserId());
        QueryWrapper<CategoryDv> wrapper = new QueryWrapper<CategoryDv>(query);
        if(categoryDv.getCategoryDvId() != null){
            wrapper.ne("CATEGORY_DV_ID",categoryDv.getCategoryDvId());
        }
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public void saveOrUpdateDvBatch(List<CategoryDv> categoryDvs) {
        if(!CollectionUtils.isEmpty(categoryDvs)){
            for(CategoryDv categoryDv:categoryDvs){
                 this.saveOrUpdateDv(categoryDv);
            }
        }
    }

    @Override
    public PageInfo<CategoryDv> listPageByParam(DvRequestDTO requestDTO) {
        PageUtil.startPage(requestDTO.getPageNum(), requestDTO.getPageSize());
        List<CategoryDv> list = getCategoryDvs(requestDTO);
        return new PageInfo<>(list);
    }

    public List<CategoryDv> getCategoryDvs(DvRequestDTO requestDTO) {
        CategoryDv query = new CategoryDv();
        if(requestDTO.getCategoryId()!= null){
            query.setCategoryId(requestDTO.getCategoryId());
        }
        if(requestDTO.getUserId() != null){
            query.setUserId(requestDTO.getUserId());
        }
        QueryWrapper<CategoryDv> wrapper = new QueryWrapper<CategoryDv>(query);
        wrapper.like(StringUtils.isNotBlank(requestDTO.getCategoryName()),
                "CATEGORY_NAME",requestDTO.getCategoryName());
        wrapper.like(StringUtils.isNotBlank(requestDTO.getFullName()),
                "FULL_NAME", requestDTO.getFullName());
        if(StringUtils.isNotBlank(requestDTO.getIsActive())){
            if(YesOrNo.YES.getValue().equals(requestDTO.getIsActive())){
                wrapper.le("START_DATE",new Date());
                wrapper.ge("END_DATE",new Date());
            }else if(YesOrNo.NO.getValue().equals(requestDTO.getIsActive())){
                wrapper.gt("START_DATE",new Date())
                        .or()
                        .lt("END_DATE",new Date());
            }
        }
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return this.list(wrapper);
    }

    @Override
    public List<CategoryDvDTO> listByParam(DvRequestDTO requestDTO) {
        return categoryDvMapper.listByParam(requestDTO);
    }

    /**
     * ??????????????????
     * @param response
     * @throws IOException
     */
    @Override
    public void importModelDownload(HttpServletResponse response) throws IOException {
        String fileName = "????????????????????????";
        ArrayList<CategoryDvImport> categoryDvImports = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        EasyExcelUtil.writeExcelWithModel(outputStream,fileName,categoryDvImports,CategoryDvImport.class);
    }

    @Override
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload) throws Exception {
        // ????????????
        EasyExcelUtil.checkParam(file,fileupload);
        // ??????excel??????
        List<CategoryDvImport> categoryDvImports = this.readData(file);
        List<CategoryDv> categoryDvs = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();

        if(CollectionUtils.isNotEmpty(categoryDvImports)){
            boolean errorFlag = checkData(categoryDvImports,categoryDvs);
            if(errorFlag){
                // ?????????
                fileupload.setFileSourceName("????????????????????????");
                Fileupload fileupload1 = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                        categoryDvImports, CategoryDvImport.class, file.getName(), file.getOriginalFilename(), file.getContentType());
                result.put("status", YesOrNo.NO.getValue());
                result.put("message","error");
                result.put("fileuploadId",fileupload1.getFileuploadId());
                result.put("fileName",fileupload1.getFileSourceName());
            }else {
                // ????????????
                if(CollectionUtils.isNotEmpty(categoryDvs)){
                    for (CategoryDv categoryDv : categoryDvs){
                        saveOrUpdateDv(categoryDv);
                    }
                }
                result.put("status", YesOrNo.YES.getValue());
                result.put("message","success");
            }
        }else {
            result.put("status", YesOrNo.YES.getValue());
            result.put("message","success");
        }

        return result;
    }

    public boolean checkData(List<CategoryDvImport> categoryDvImports,List<CategoryDv> categoryDvs) {
        boolean errorFlag = false;
        for(CategoryDvImport categoryDvImport : categoryDvImports){
            StringBuffer errorMsg = new StringBuffer();
            CategoryDv categoryDv = new CategoryDv();

            // ????????????????????????
            String categoryName = categoryDvImport.getCategoryName();
            if(StringUtil.notEmpty(categoryName)){
                List<PurchaseCategory> purchaseCategories = iPurchaseCategoryService.list(
                        new QueryWrapper<>(new PurchaseCategory().setCategoryName(categoryName.trim())));
                if(CollectionUtils.isNotEmpty(purchaseCategories)){
                    PurchaseCategory purchaseCategory = purchaseCategories.get(0);
                    iPurchaseCategoryService.setCategoryFullName(purchaseCategory);
                    categoryDv.setCategoryFullName(purchaseCategory.getCategoryFullName());
                    categoryDv.setCategoryId(purchaseCategory.getCategoryId());
                    categoryDv.setCategoryName(purchaseCategory.getCategoryName());
                }else {
                    errorMsg.append("??????????????????; ");
                    errorFlag = true;
                }
            }else {
                errorMsg.append("????????????????????????; ");
                errorFlag = true;
            }

            // ????????????????????????
            String orgName = categoryDvImport.getOrgName();
            if(StringUtil.notEmpty(orgName)){
                List<Organization> organizations = organizationService.list(new QueryWrapper<>(new Organization().setOrganizationName(orgName.trim())));
                if(CollectionUtils.isNotEmpty(organizations)){
                    Organization organization = organizations.get(0);
                    List<OrganizationUser> organizationUsers = organizationUserService.list(
                            new QueryWrapper<>(new OrganizationUser().setOrganizationId(organization.getOrganizationId())));
                    if(CollectionUtils.isNotEmpty(organizationUsers)){
                        categoryDv.setOrgId(organization.getOrganizationId());
                        categoryDv.setOrgName(organization.getOrganizationName());
                        categoryDv.setFullPathId(organizationUsers.get(0).getFullPathId());
                    }else {
                        errorMsg.append("??????????????????????????????; ");
                        errorFlag = true;
                    }
                }else {
                    errorMsg.append("??????????????????; ");
                    errorFlag = true;
                }
            }

            // ??????????????????
            String userName = categoryDvImport.getUserName();
            if(StringUtil.notEmpty(userName)){
                LoginAppUser byUsername = rbacClient.findByUsername(userName.trim());
                if(null != byUsername && StringUtil.notEmpty(byUsername.getUsername())){
                    List<OrganizationUser> organizationUsers = byUsername.getOrganizationUsers();
                    // ????????????????????????????????????
                    if(CollectionUtils.isNotEmpty(organizationUsers)){
                        boolean flag = true;
                        if (StringUtil.notEmpty(categoryDv.getOrgId())) {
                            for(OrganizationUser organizationUser : organizationUsers){
                                if(categoryDv.getOrgId().equals(organizationUser.getOrganizationId())){
                                    flag = false;
                                }
                            }
                        }
                        if(flag){
                            errorMsg.append("??????????????????????????????; ");
                            errorFlag = true;
                        }
                    }else if (StringUtil.notEmpty(categoryDv.getOrgId())){
                        errorMsg.append("??????????????????????????????; ");
                        errorFlag = true;
                    }
                    categoryDv.setUserId(byUsername.getUserId());
                    categoryDv.setUserName(byUsername.getUsername());
                    categoryDv.setFullName(byUsername.getNickname());
                }
            }else {
                errorMsg.append("????????????????????????; ");
                errorFlag = true;
            }

            // ??????????????????
            String startDate = categoryDvImport.getStartDate();
            if (StringUtil.notEmpty(startDate)){
                try {
                    Date date = DateUtil.parseDate(startDate);
                    LocalDate localDate = DateUtil.dateToLocalDate(date);
                    categoryDv.setStartDate(localDate);
                } catch (ParseException e) {
                    errorMsg.append("??????????????????????????????; ");
                    errorFlag = true;
                }
            }else {
                errorMsg.append("????????????????????????; ");
                errorFlag = true;
            }

            // ??????????????????
            String endDate = categoryDvImport.getEndDate();
            if (StringUtil.notEmpty(endDate)){
                try {
                    Date date = DateUtil.parseDate(endDate);
                    LocalDate localDate = DateUtil.dateToLocalDate(date);
                    categoryDv.setEndDate(localDate);
                } catch (ParseException e) {
                    errorMsg.append("??????????????????????????????; ");
                    errorFlag = true;
                }
            }

            if (StringUtil.notEmpty(categoryDv.getCategoryId()) &&
                    StringUtil.notEmpty(categoryDv.getUserId()) &&
                    StringUtil.notEmpty(categoryDv.getStartDate())) {
                if(null != categoryDv.getEndDate()){
                    if(DateChangeUtil.asDate(categoryDv.getEndDate()).before(new Date())){
                        errorMsg.append("????????????????????????????????????; ");
                        errorFlag = true;
                    }
                }

                List<CategoryDv> dvs = this.checkExist(categoryDv);
                if(!CollectionUtils.isEmpty(dvs)){
                    errorMsg.append("???????????????,?????????!; ");
                    errorFlag = true;
                }

                PurchaseCategory category = iPurchaseCategoryService.getById(categoryDv.getCategoryId());
                if(category == null ||  YesOrNo.NO.getValue().equals(category.getEnabled())){
                    errorMsg.append("???????????????????????????; ");
                    errorFlag = true;
                }
            }

            if(errorMsg.length() > 1){
                categoryDvImport.setErrorMsg(errorMsg.toString());
            }

            categoryDvs.add(categoryDv);
        }
        return errorFlag;
    }

    public List<CategoryDvImport> readData(MultipartFile file) {
        List<CategoryDvImport> categoryDvImports;
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<CategoryDvImport> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream,listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(CategoryDvImport.class).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            categoryDvImports = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return categoryDvImports;
    }

    @Override
    public List<List<Object>> queryExportData(ExportExcelParam<CategoryDv> param) {
        CategoryDv queryParam = param.getQueryParam();
        // ???????????????????????????
        boolean flag = StringUtil.notEmpty(queryParam.getPageSize()) && StringUtil.notEmpty(queryParam.getPageNum());
        if (flag) {
            // ????????????
            PageUtil.startPage(queryParam.getPageNum(), queryParam.getPageSize());
        }
        DvRequestDTO dto = new DvRequestDTO();
        BeanCopyUtil.copyProperties(dto,queryParam);
        // ????????????
        List<CategoryDv> categoryDvs = getCategoryDvs(dto);

        List<List<Object>> dataList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(categoryDvs)) {
            List<Map<String, Object>> mapList = BeanMapUtils.objectsToMaps(categoryDvs);
            ArrayList<String> titleList = param.getTitleList();
            if (CollectionUtils.isNotEmpty(titleList)) {
                for(Map<String, Object> map : mapList){
                    ArrayList<Object> objects = new ArrayList<>();
                    for(String key : titleList){
                        if("startDate".equals(key) || "endDate".equals(key)){
                            setDate(map,objects,key);
                        }else {
                            objects.add(map.get(key));
                        }
                    }
                    dataList.add(objects);
                }
            }
        }
        return dataList;
    }

    private void setDate(Map<String, Object> map, ArrayList<Object> list, String title) {
        Object date = map.get(title);
        if(null != date){
            LocalDate assessmentDate = (LocalDate)date;
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            list.add(assessmentDate.format(dateTimeFormatter));
        }else {
            list.add("");
        }
    }

    @Override
    public List<String> getMultilingualHeader(ExportExcelParam<CategoryDv> param) {
        LinkedHashMap<String, String> categoryDvTitles = ExportUtils.getCategoryDvTitles();
        return param.getMultilingualHeader(param,categoryDvTitles);
    }

    @Override
    public void exportStart(ExportExcelParam<CategoryDv> param, HttpServletResponse response) throws IOException {
        // ?????????????????????
        List<List<Object>> dataList = queryExportData(param);
        // ??????
        List<String> head = getMultilingualHeader(param);
        // ?????????
        String fileName = param.getFileName();
        // ????????????
        EasyExcelUtil.exportStart(response, dataList, head, fileName);
    }
}
