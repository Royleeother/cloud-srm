package com.midea.cloud.srm.supcooperate.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.enums.supcooperate.MaterialItemType;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.suppliercooperate.material.dto.CeeaMaterialItemDTO;
import com.midea.cloud.srm.model.suppliercooperate.material.entity.CeeaMaterialDetail;
import com.midea.cloud.srm.model.suppliercooperate.material.entity.CeeaMaterialItem;
import com.midea.cloud.srm.supcooperate.material.mapper.MaterialItemMapper;
import com.midea.cloud.srm.supcooperate.material.service.IMaterialDetailService;
import com.midea.cloud.srm.supcooperate.material.service.IMaterialItemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <pre>
 *  ????????????????????? ???????????????
 * </pre>
 *
 * @author yourname@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-08-21 23:38:18
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class MaterialItemServiceImpl extends ServiceImpl<MaterialItemMapper, CeeaMaterialItem> implements IMaterialItemService {
    @Autowired
    private IMaterialDetailService iMaterialDetailService;

    @Resource
    private BaseClient baseClient;

    @Resource
    private FileCenterClient fileCenterClient;

    @Resource
    private SupplierClient supplierClient;


    private static final List<String> fixedTitle;

    private static final List<String> fixedTitleDetail;

    static {
        fixedTitle = new ArrayList<>();
        fixedTitle.addAll(Arrays.asList("*????????????", "*????????????", "*????????????", "*???????????????", "*????????????", "*??????"));
        fixedTitleDetail = new ArrayList<>();
        fixedTitleDetail.addAll(Arrays.asList("*???????????????"));
    }

    /**
     * ??????????????????
     *
     * @param materialItemDTO
     * @return
     */
    @Override
    public PageInfo<CeeaMaterialItem> getMaterialItemList(CeeaMaterialItemDTO materialItemDTO) {
        PageUtil.startPage(materialItemDTO.getPageNum(), materialItemDTO.getPageSize());
        List<CeeaMaterialItem> ceeaMaterialItems = getCeeaMaterialItems(materialItemDTO);
        return new PageInfo<>(ceeaMaterialItems);
    }

    @AuthData(module = MenuEnum.MATERIAL_PLAN)
    private List<CeeaMaterialItem> getCeeaMaterialItems(CeeaMaterialItemDTO materialItemDTO) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        QueryWrapper<CeeaMaterialItem> wrapper = new QueryWrapper<>();
        //??????????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(materialItemDTO.getOrgIds()), "ORG_ID", materialItemDTO.getOrgIds());
        //???????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(materialItemDTO.getOrganizationIds()), "ORGANIZATION_ID", materialItemDTO.getOrganizationIds());
        //????????????????????????
        wrapper.like(StringUtils.isNotEmpty(materialItemDTO.getOrganizationSite()), "ORGANIZATION_SITE", materialItemDTO.getOrganizationSite());
        //???????????????????????????
        wrapper.like(StringUtils.isNotEmpty(materialItemDTO.getMaterialSchNum()), "MATERIAL_SCH_NUM", materialItemDTO.getMaterialSchNum());
        //??????????????????????????????
        wrapper.like(StringUtils.isNotEmpty(materialItemDTO.getMaterialCode()), "MATERIAL_CODE", materialItemDTO.getMaterialCode());
        //??????????????????????????????
        wrapper.like(StringUtils.isNotEmpty(materialItemDTO.getMaterialName()), "MATERIAL_NAME", materialItemDTO.getMaterialName());
        //???????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(materialItemDTO.getCategoryIds()), "CATEGORY_ID", materialItemDTO.getCategoryIds());
        if (loginAppUser!=null){
            String userType = loginAppUser.getUserType();
            wrapper.eq(StringUtils.isNotEmpty(userType)&&userType.equals("VENDOR"), "SCH_TYPE", MaterialItemType.get("ISSUED"));
        }else {
            //????????????????????????
            wrapper.eq(StringUtils.isNotEmpty(materialItemDTO.getSchType()), "SCH_TYPE", materialItemDTO.getSchType());
        }
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(materialItemDTO.getMonthlySchDate()), "MONTHLY_SCH_DATE", materialItemDTO.getMonthlySchDate());
        return this.list(wrapper);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param id
     * @return
     */
    @Override
    public CeeaMaterialItemDTO getMaterialItemDetail(Long id) {
        CeeaMaterialItemDTO materialItemDTO = new CeeaMaterialItemDTO();
        CeeaMaterialItem byId = this.getById(id);
        materialItemDTO.setMaterialItem(byId);
        QueryWrapper<CeeaMaterialDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("MATERIAL_ITEM_ID", id);
        List<CeeaMaterialDetail> list = iMaterialDetailService.list(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            materialItemDTO.setMaterialDetailList(iMaterialDetailService.list(wrapper));
        }
        return materialItemDTO;
    }



    @Override
    public void importModelDownload(String monthlySchDate,HttpServletResponse response) throws IOException, ParseException {
        Assert.notNull(monthlySchDate,"????????????????????????: monthlySchDate");
        // ????????????
        Workbook workbook = crateWorkbookModel(monthlySchDate);
        // ???????????????
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????????????????");
        // ??????
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();

    }

    @Override
    public void importModelDetailDownload(String materialItemId, HttpServletResponse response) throws IOException, ParseException {
        Assert.notNull(materialItemId, "???????????????");
        // ????????????
        Workbook workbook = crateWorkbookModelDetail(materialItemId);
        // ???????????????
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????????????????");
        // ??????
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    public Workbook crateWorkbookModelDetail(String materialItemId) throws ParseException {
        // ???????????????
        XSSFWorkbook workbook = new XSSFWorkbook();
        // ???????????????:???????????????
        XSSFSheet sheet = workbook.createSheet("sheet");
        // ?????????????????????
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        // ????????????????????????:????????????
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        // ????????????????????????:????????????
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // ??????????????????
        Font font = workbook.createFont();
        font.setBold(true);
        cellStyle.setFont(font);

        cellStyle.setBorderBottom(BorderStyle.THIN); //?????????
        cellStyle.setBorderLeft(BorderStyle.THIN);//?????????
        cellStyle.setBorderTop(BorderStyle.THIN);//?????????
        cellStyle.setBorderRight(BorderStyle.THIN);//?????????

        // ???????????????
        XSSFRow row = sheet.createRow(0);

        // ?????????????????????
        int cellIndex = 0;

        // ?????????????????????
        for (int i = 0; i < fixedTitleDetail.size(); i++) {
            XSSFCell cell1 = row.createCell(cellIndex);
                cell1.setCellValue(fixedTitleDetail.get(i));
                cell1.setCellStyle(cellStyle);
                cellIndex++;
        }

        CeeaMaterialItem ceeaMaterialItem = this.getById(materialItemId);
        if(ceeaMaterialItem !=null && StringUtils.isNotEmpty(ceeaMaterialItem.getMonthlySchDate())) {
            Date date = DateUtil.parseDate(ceeaMaterialItem.getMonthlySchDate());
            LocalDate localDate = DateUtil.dateToLocalDate(date);
            List<String> dayBetween = DateUtil.getDayBetween(localDate, "yyyy-MM-dd");
            for (int i = 0; i < dayBetween.size(); i++) {
                XSSFCell cell1 = row.createCell(cellIndex);
                cell1.setCellValue(dayBetween.get(i));
                cell1.setCellStyle(cellStyle);
                cellIndex++;
            }
        }else {
            throw new BaseException("???????????????????????????");
        }
        return workbook;
    }

    /**
     * ??????????????????
     */
    public Workbook crateWorkbookModel(String monthlySchDate) throws ParseException {
        // ???????????????
        XSSFWorkbook workbook = new XSSFWorkbook();
        // ???????????????:???????????????
        XSSFSheet sheet = workbook.createSheet("sheet");
        // ?????????????????????
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        // ????????????????????????:????????????
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        // ????????????????????????:????????????
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // ??????????????????
        Font font = workbook.createFont();
        font.setBold(true);
        cellStyle.setFont(font);

        cellStyle.setBorderBottom(BorderStyle.THIN); //?????????
        cellStyle.setBorderLeft(BorderStyle.THIN);//?????????
        cellStyle.setBorderTop(BorderStyle.THIN);//?????????
        cellStyle.setBorderRight(BorderStyle.THIN);//?????????

        // ???????????????
        XSSFRow row = sheet.createRow(0);

        // ?????????????????????
        int cellIndex = 0;

        // ?????????????????????
        for (int i = 0; i < fixedTitle.size(); i++) {
            XSSFCell cell1 = row.createCell(cellIndex);
            if (i != fixedTitle.size() - 1) {
                cell1.setCellValue(fixedTitle.get(i));
                cell1.setCellStyle(cellStyle);
            } else {
                String msg = "1??????????????????\"???????????????????????????????????????";
                EasyExcelUtil.setCellStyle(workbook, cell1, sheet, msg, fixedTitle.get(i));
            }
            cellIndex++;
        }


        // ?????????????????????
        Date date = DateUtil.parseDate(monthlySchDate);
        LocalDate localDate = DateUtil.dateToLocalDate(date);
        List<String> dayBetween = DateUtil.getDayBetween(localDate, "yyyy-MM-dd");
        for(int i = 0;i < dayBetween.size();i++){
            XSSFCell cell1 = row.createCell(cellIndex);
            cell1.setCellValue(dayBetween.get(i));
            cell1.setCellStyle(cellStyle);
            cellIndex ++;
        }
        return workbook;
    }

    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload,HttpServletResponse response) throws IOException, ParseException {
        // ??????????????????
        EasyExcelUtil.checkParam(file,fileupload);
        // ?????????????????????
        InputStream inputStream = file.getInputStream();
        // ???????????????????????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ???????????????
        List<CeeaMaterialItem> ceeaMaterialItems = new ArrayList<>();
        // ??????????????????
        List<String> errorList = new ArrayList<>();
        // ????????????
        Map<String, Object> result = new HashMap<>();
        result.put("status", YesOrNo.YES.getValue());
        result.put("message","success");
        // ??????????????????
        Workbook workbook = this.getImportData(inputStream, ceeaMaterialItems, errorList, errorFlag);
        if(errorFlag.get()){
            // ?????????,??????????????????
            this.uploadErrorFile(file, fileupload, errorList, result, workbook);
        }else {
            if(CollectionUtils.isNotEmpty(ceeaMaterialItems)){
                HashSet<Long> ids = new HashSet<>();
                ceeaMaterialItems.forEach(ceeaMaterialItem -> {
                    List<CeeaMaterialDetail> ceeaMaterialDetails = ceeaMaterialItem.getCeeaMaterialDetails();
                    Date schMonthlyDate = ceeaMaterialDetails.get(0).getSchMonthlyDate();
                    String monthlySchDate = DateUtil.format(schMonthlyDate, "yyyy-MM");
                    ceeaMaterialItem.setMonthlySchDate(monthlySchDate);
                    // ??????????????????: ???????????? + ???????????? + ???????????? + ???????????? + ???????????? + ???????????? + ???????????????
                    List<CeeaMaterialItem> materialItems = this.list(new QueryWrapper<>(new CeeaMaterialItem().setMonthlySchDate(monthlySchDate).setOrgId(ceeaMaterialItem.getOrgId()).
                            setOrganizationId(ceeaMaterialItem.getOrganizationId()).setOrganizationSite(ceeaMaterialItem.getOrganizationSite()).
                            setCategoryId(ceeaMaterialItem.getCategoryId()).setMaterialId(ceeaMaterialItem.getMaterialId()).setVendorName(ceeaMaterialItem.getVendorName())));
                    if(CollectionUtils.isNotEmpty(materialItems)){
                        // ????????????
                        CeeaMaterialItem ceeaMaterialItem1 = materialItems.get(0);
                        ids.add(ceeaMaterialItem1.getMaterialItemId());
                        ceeaMaterialDetails.forEach(ceeaMaterialDetail -> {
                            // ?????????????????????, ??????????????? : materialItemId + ??????
                            List<CeeaMaterialDetail> ceeaMaterialDetailList = iMaterialDetailService.list(new QueryWrapper<>(new CeeaMaterialDetail().
                                    setMaterialItemId(ceeaMaterialItem1.getMaterialItemId()).setSchMonthlyDate(ceeaMaterialDetail.getSchMonthlyDate())));
                            if(CollectionUtils.isNotEmpty(ceeaMaterialDetailList)){
                                CeeaMaterialDetail ceeaMaterialDetail1 = ceeaMaterialDetailList.get(0);
                                ceeaMaterialDetail1.setRequirementQuantity(ceeaMaterialDetail.getRequirementQuantity());
                                iMaterialDetailService.updateById(ceeaMaterialDetail1);
                            }else {
                                ceeaMaterialDetail.setMaterialItemId(ceeaMaterialItem1.getMaterialItemId());
                                ceeaMaterialDetail.setMaterialSchNum(ceeaMaterialItem1.getMaterialSchNum());
                                iMaterialDetailService.save(ceeaMaterialDetail);
                            }
                        });
                    }else {
                        // ??????
                        Long materialItemId = IdGenrator.generate();
                        ids.add(materialItemId);
                        ceeaMaterialItem.setMaterialItemId(materialItemId);
                        String materialSchNum = baseClient.seqGen(SequenceCodeConstant.SEQ_CEEA_MATERIAL_ITEM_CODE);
                        ceeaMaterialItem.setMaterialSchNum(materialSchNum);
                        ceeaMaterialItem.setSchType(MaterialItemType.HASNEW.getName());
                        this.save(ceeaMaterialItem);
                        // ????????????
                        ceeaMaterialDetails.forEach(ceeaMaterialDetail -> {
                            ceeaMaterialDetail.setId(IdGenrator.generate());
                            ceeaMaterialDetail.setMaterialItemId(materialItemId);
                            ceeaMaterialDetail.setMaterialSchNum(materialSchNum);
                        });
                        iMaterialDetailService.saveBatch(ceeaMaterialDetails);
                    }
                });
                ids.forEach(id->{
                    this.baseMapper.updateSchTotalQuantity(id);
                });
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> importDetailExcel(String materialItemId,MultipartFile file, Fileupload fileupload, HttpServletResponse response) throws IOException, ParseException {
        // ??????????????????
        // EasyExcelUtil.checkParam(file,fileupload);
        // ?????????????????????
        InputStream inputStream = file.getInputStream();
        // ???????????????????????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ???????????????
        List<CeeaMaterialItem> ceeaMaterialItems = new ArrayList<>();
        // ??????????????????
        List<String> errorList = new ArrayList<>();
        // ????????????
        Map<String, Object> result = new HashMap<>();
        result.put("status", YesOrNo.YES.getValue());
        result.put("message","success");

        // ??????????????????
        Workbook workbook = this.getImportDetailData(materialItemId,inputStream, ceeaMaterialItems, errorList, errorFlag);

        if(errorFlag.get()){
            // ?????????,??????????????????
            this.uploadErrorFile(file, fileupload, errorList, result, workbook);
        }else {
            if(CollectionUtils.isNotEmpty(ceeaMaterialItems)){
                ceeaMaterialItems.forEach(ceeaMaterialItem->{
                    BigDecimal total = new BigDecimal(0);
                    List<CeeaMaterialDetail> ceeaMaterialDetails = ceeaMaterialItem.getCeeaMaterialDetails();
                        for (CeeaMaterialDetail ceeaMaterialDetail:ceeaMaterialDetails) {
                            QueryWrapper<CeeaMaterialDetail> wrapper = new QueryWrapper<>();
                            wrapper.eq("MATERIAL_ITEM_ID", ceeaMaterialDetail.getMaterialItemId());
                            wrapper.eq("MATERIAL_SCH_NUM", ceeaMaterialDetail.getMaterialSchNum());
                            wrapper.eq("SCH_MONTHLY_DATE", ceeaMaterialDetail.getSchMonthlyDate());
                            CeeaMaterialDetail materialDetail = iMaterialDetailService.getOne(wrapper);
                            // ??????
                            if (materialDetail != null) {
                                materialDetail.setRequirementQuantity(ceeaMaterialDetail.getRequirementQuantity());
                                iMaterialDetailService.updateById(materialDetail);
                            } else { // ??????
                                ceeaMaterialDetail.setMaterialItemId(ceeaMaterialItem.getMaterialItemId());
                                ceeaMaterialDetail.setId(IdGenrator.generate());
                                iMaterialDetailService.save(ceeaMaterialDetail);
                            }
                            total = total.add(ceeaMaterialDetail.getRequirementQuantity());
                        }
                         UpdateWrapper<CeeaMaterialItem> updateWrapper =new UpdateWrapper<>();
                         updateWrapper.eq("MATERIAL_ITEM_ID",ceeaMaterialItem.getMaterialItemId());
                         updateWrapper.eq("MATERIAL_SCH_NUM",ceeaMaterialItem.getMaterialSchNum());
                         updateWrapper.set("SCH_TOTAL_QUANTITY",total);
                         this.update(updateWrapper);
                    });
            }
        }
        return result;
    }

    private void uploadErrorFile(MultipartFile file, Fileupload fileupload, List<String> errorList, Map<String, Object> result, Workbook workbook) {
        // ?????????????????????
        Sheet sheet = workbook.getSheetAt(0);

        // ???????????????????????????(0??????)
        int totalRows = sheet.getLastRowNum();
        // ?????????????????????, ????????????????????????
        int totalCells = sheet.getRow(0).getLastCellNum();
        sheet.setColumnWidth(totalCells, sheet.getColumnWidth(totalCells) * 17 / 5);

        // ??????"????????????"??????
        this.setErrorTitle(workbook, sheet, totalCells);
        for(int i =1;i<=totalRows;i++){
            Cell cell = sheet.getRow(i).createCell(totalCells);
            cell.setCellValue(errorList.get(i-1));
        }
        Fileupload fileupload1 = ExcelUtil.uploadErrorFile(fileCenterClient, fileupload, workbook, file);
        result.put("status", YesOrNo.NO.getValue());
        result.put("message","error");
        result.put("fileuploadId",fileupload1.getFileuploadId());
        result.put("fileName",fileupload1.getFileSourceName());
    }

    private void setErrorTitle(Workbook workbook, Sheet sheet, int totalCells) {
        Row row0 = sheet.getRow(0);
        // ?????????????????????
        CellStyle cellStyle = workbook.createCellStyle();
        // ????????????????????????:????????????
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        // ????????????????????????:????????????
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // ??????????????????
        Font font = workbook.createFont();
        font.setBold(true);
        cellStyle.setFont(font);
        cellStyle.setBorderBottom(BorderStyle.THIN); //?????????
        cellStyle.setBorderLeft(BorderStyle.THIN);//?????????
        cellStyle.setBorderTop(BorderStyle.THIN);//?????????
        cellStyle.setBorderRight(BorderStyle.THIN);//?????????
        Cell cell1 = row0.createCell(totalCells);
        cell1.setCellValue("????????????");
        cell1.setCellStyle(cellStyle);
    }

    /**
     * ?????????????????????, ????????????????????????
     * @param inputStream
     * @param ceeaMaterialItems
     * @param errorList
     * @throws IOException
     * @throws ParseException
     */
    private Workbook getImportData(InputStream inputStream, List<CeeaMaterialItem> ceeaMaterialItems, List<String> errorList,AtomicBoolean errorFlag) throws IOException, ParseException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        // ?????????????????????
        Sheet sheet = workbook.getSheetAt(0);
        // ???????????????????????????(0??????)
        int totalRows = sheet.getLastRowNum();
        // ?????????????????????, ????????????????????????
        int totalCells = sheet.getRow(0).getLastCellNum();
        // ?????????????????????
        List<String> head = new ArrayList<>();
        // ????????????
        Row headRow = sheet.getRow(0);
        // ???????????????????????????
        for (int i = 0; i < totalCells; i++) {
            Cell cell = headRow.getCell(i);
            head.add(ExcelUtil.getCellValue(cell));
        }
        HashSet<String> hashSet = new HashSet<>();
        // ???????????????????????????,???2?????????,????????????????????????1
        for (int r = 1; r <= totalRows; r++) {
            log.info("???"+r+"?????????");
            Row row = sheet.getRow(r);
            if (null == row) {
                // ????????????,????????????????????????????????????
                sheet.shiftRows(r + 1, totalRows, -1);
                r--;
                totalRows --;
                continue;
            }

            // ????????????, ??????????????????????????????????????????, ???????????????????????????????????????
            int count = 0;
            for (int i = 0; i < totalCells; i++) {
                // ?????????????????????
                Cell cell = row.getCell(i);
                // ????????????????????????
                String cellValue = ExcelUtil.getCellValue(cell);
                if (null == cellValue || "".equals(cellValue)) {
                    count++;
                }
            }
            if (count == totalCells) {
                if (r + 1 > totalRows) {
                    break;
                }
                sheet.shiftRows(r + 1, totalRows, -1);
                r--;
                totalRows --;
                continue;

            }
// <------------------------------------????????????????????????????????????-------------------------------------------->
            // ????????????????????????
            StringBuffer errorMsg = new StringBuffer();
            // ??????????????????????????????
            CeeaMaterialItem ceeaMaterialItem = new CeeaMaterialItem();
            List<CeeaMaterialDetail> ceeaMaterialDetails = new ArrayList<>();
            StringBuffer only = new StringBuffer();

            // ??????????????????
            Cell cell0 = row.getCell(0);
            String orgName = ExcelUtil.getCellValue(cell0);
            if(StringUtil.notEmpty(orgName)){
                orgName = orgName.trim();
                only.append(orgName);
                Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgName));
                if(null != organization && StringUtil.notEmpty(organization.getOrganizationId())){
                    ceeaMaterialItem.setOrgId(organization.getOrganizationId());
                    ceeaMaterialItem.setOrgCode(organization.getOrganizationCode());
                    ceeaMaterialItem.setOrgName(organization.getOrganizationCode());
                }else {
                    errorFlag.set(true);
                    errorMsg.append("?????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorMsg.append("????????????????????????; ");
            }

            // ??????????????????
            Cell cell1 = row.getCell(1);
            String organizationName = ExcelUtil.getCellValue(cell1);
            if(StringUtil.notEmpty(organizationName)){
                organizationName = organizationName.trim();
                only.append(organizationName);
                Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(organizationName));
                if(null != organization && StringUtil.notEmpty(organization.getOrganizationId())){
                    if(StringUtil.notEmpty(organization.getParentOrganizationIds())){
                        if(StringUtil.notEmpty(ceeaMaterialItem.getOrgId()) && organization.getParentOrganizationIds().contains(ceeaMaterialItem.getOrgId().toString())){
                            ceeaMaterialItem.setOrganizationId(organization.getOrganizationId());
                            ceeaMaterialItem.setOrganizationName(organization.getOrganizationName());
                            ceeaMaterialItem.setOrganizationCode(organization.getOrganizationCode());
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("?????????????????????????????????????????????; ");
                        }
                    }else {
                        errorFlag.set(true);
                        errorMsg.append("???????????????????????????; ");
                    }
                }else {
                    errorFlag.set(true);
                    errorMsg.append("?????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorMsg.append("????????????????????????; ");
            }

            // ????????????
            Cell cell2 = row.getCell(2);
            String organizationSite = ExcelUtil.getCellValue(cell2);
            if(StringUtil.notEmpty(organizationSite)){
                organizationSite = organizationSite.trim();
                only.append(organizationSite);
                ceeaMaterialItem.setOrganizationSite(organizationSite);
            }else {
                errorFlag.set(true);
                errorMsg.append("????????????????????????; ");
            }

            // ?????????
            Cell cell3 = row.getCell(3);
            String vendorName = ExcelUtil.getCellValue(cell3);
            if (StringUtil.notEmpty(vendorName)) {
                vendorName = vendorName.trim();
                only.append(vendorName);
                ceeaMaterialItem.setVendorName(vendorName);
            } else {
                errorFlag.set(true);
                errorMsg.append("???????????????????????????; ");
            }

            // ????????????
            Cell cell4 = row.getCell(4);
            String materialCode = ExcelUtil.getCellValue(cell4);
            if(StringUtil.notEmpty(materialCode)){
                materialCode = materialCode.trim();
                only.append(materialCode);
                List<MaterialItem> materialItems = baseClient.listMaterialByParam(new MaterialItem().setMaterialCode(materialCode));
                if(CollectionUtils.isNotEmpty(materialItems)){
                    MaterialItem materialItem = materialItems.get(0);
                    ceeaMaterialItem.setMaterialId(materialItem.getMaterialId());
                    ceeaMaterialItem.setMaterialCode(materialItem.getMaterialCode());
                    ceeaMaterialItem.setMaterialName(materialItem.getMaterialName());
                    ceeaMaterialItem.setCategoryId(materialItem.getCategoryId());
                    ceeaMaterialItem.setCategoryName(materialItem.getCategoryName());
                    ceeaMaterialItem.setUnit(materialItem.getUnit());
                }else {
                    errorFlag.set(true);
                    errorMsg.append("?????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorMsg.append("????????????????????????; ");
            }

            // ??????
            Cell cell5 = row.getCell(5);
            String project = ExcelUtil.getCellValue(cell5);
            if(StringUtil.notEmpty(project)){
                project = project.trim();
                if(!"??????".equals(project)) {
                    errorFlag.set(true);
                    errorMsg.append("???????????????\"??????\"; ");
                }
            }else {
                errorFlag.set(true);
                errorMsg.append("??????????????????; ");
            }

            if(only.length() > 0 && !hashSet.add(only.toString())){
                errorFlag.set(true);
                errorMsg.append("????????????+????????????+????????????+????????????+???????????????,????????????; ");
            }

            // ????????????????????????
            for (int c = 6; c < totalCells; c++) {
                Cell cell = row.getCell(c);
                String cellValue = ExcelUtil.getCellValue(cell);
                if(StringUtil.notEmpty(cellValue)){
                    cellValue = cellValue.trim();
                }else {
                    cellValue = "0";
                }
                if(StringUtil.isDigit(cellValue)){
                    CeeaMaterialDetail ceeaMaterialDetail = new CeeaMaterialDetail();
                    Date date = DateUtil.parseDate(head.get(c));
                    ceeaMaterialDetail.setSchMonthlyDate(date);
                    ceeaMaterialDetail.setRequirementQuantity(new BigDecimal(cellValue));
                    ceeaMaterialDetails.add(ceeaMaterialDetail);
                }else {
                    errorFlag.set(true);
                    errorMsg.append(cellValue+"?????????; ");
                }
            }

            ceeaMaterialItem.setCeeaMaterialDetails(ceeaMaterialDetails);
            ceeaMaterialItems.add(ceeaMaterialItem);
            errorList.add(errorMsg.toString());
        }
        return workbook;
    }


    /**
     * ???????????????????????????????????????, ????????????????????????
     * @param inputStream
     * @param ceeaMaterialItems
     * @param errorList
     * @throws IOException
     * @throws ParseException
     */
    private Workbook getImportDetailData(String materialItemId,InputStream inputStream, List<CeeaMaterialItem> ceeaMaterialItems, List<String> errorList,AtomicBoolean errorFlag) throws IOException, ParseException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        // ?????????????????????
        Sheet sheet = workbook.getSheetAt(0);
        // ???????????????????????????(0??????)
        int totalRows = sheet.getLastRowNum();
        // ?????????????????????, ????????????????????????
        int totalCells = sheet.getRow(0).getLastCellNum();
        // ?????????????????????
        List<String> head = new ArrayList<>();
        // ????????????
        Row headRow = sheet.getRow(0);
        // ???????????????????????????
        for (int i = 0; i < totalCells; i++) {
            Cell cell = headRow.getCell(i);
            head.add(ExcelUtil.getCellValue(cell));
        }
        HashSet<String> hashSet = new HashSet<>();
        // ???????????????????????????,???2?????????,????????????????????????1
        for (int r = 1; r <= totalRows; r++) {
            log.info("???"+r+"?????????");
            Row row = sheet.getRow(r);
            if (null == row) {
                // ????????????,????????????????????????????????????
                sheet.shiftRows(r + 1, totalRows, -1);
                r--;
                totalRows --;
                continue;
            }

            // ????????????, ??????????????????????????????????????????, ???????????????????????????????????????
            int count = 0;
            for (int i = 0; i < totalCells; i++) {
                // ?????????????????????
                Cell cell = row.getCell(i);
                // ????????????????????????
                String cellValue = ExcelUtil.getCellValue(cell);
                if (null == cellValue || "".equals(cellValue)) {
                    count++;
                }
            }
            if (count == totalCells) {
                if (r + 1 > totalRows) {
                    break;
                }
                sheet.shiftRows(r + 1, totalRows, -1);
                r--;
                totalRows --;
                continue;

            }
// <------------------------------------????????????????????????????????????-------------------------------------------->
            // ????????????????????????
            StringBuffer errorMsg = new StringBuffer();
            // ??????????????????????????????
            CeeaMaterialItem ceeaMaterialItem = new CeeaMaterialItem();
            List<CeeaMaterialDetail> ceeaMaterialDetails = new ArrayList<>();
            StringBuffer only = new StringBuffer();

            // ???????????????
            Cell cell0 = row.getCell(0);
            String materialSchNum = ExcelUtil.getCellValue(cell0);
            if(StringUtil.notEmpty(materialSchNum)){
                materialSchNum = materialSchNum.trim();
                only.append(materialSchNum);
                QueryWrapper<CeeaMaterialItem> wrapper = new QueryWrapper<>();
                wrapper.eq("MATERIAL_ITEM_ID",materialItemId);
                wrapper.eq("MATERIAL_SCH_NUM",materialSchNum);
                CeeaMaterialItem materialItem = this.getOne(wrapper);
                if(null != materialItem){
                    ceeaMaterialItem.setMaterialSchNum(materialSchNum);
                    ceeaMaterialItem.setMaterialItemId(materialItem.getMaterialItemId());
                }else {
                    errorFlag.set(true);
                    errorMsg.append("????????????????????????; ");
                }
            }else {
                errorFlag.set(true);
                errorMsg.append("???????????????????????????; ");
            }

            if(only.length() > 0 && !hashSet.add(only.toString())){
                errorFlag.set(true);
                errorMsg.append("??????????????????????????????");
            }

            // ????????????????????????
            for (int c = 1; c < totalCells; c++) {
                Cell cell = row.getCell(c);
                String cellValue = ExcelUtil.getCellValue(cell);
                if(StringUtil.notEmpty(cellValue)){
                    cellValue = cellValue.trim();
                }else {
                    cellValue = "0";
                }
                if(StringUtil.isDigit(cellValue)){
                    CeeaMaterialDetail ceeaMaterialDetail = new CeeaMaterialDetail();
                    Date date = DateUtil.parseDate(head.get(c));
                    ceeaMaterialDetail.setSchMonthlyDate(date);
                    ceeaMaterialDetail.setRequirementQuantity(new BigDecimal(cellValue));
                    if(StringUtils.isNotEmpty(ceeaMaterialItem.getMaterialSchNum())){
                        ceeaMaterialDetail.setMaterialSchNum(ceeaMaterialItem.getMaterialSchNum());
                        ceeaMaterialDetail.setMaterialItemId(ceeaMaterialItem.getMaterialItemId());
                    }
                    ceeaMaterialDetails.add(ceeaMaterialDetail);
                }else {
                    errorFlag.set(true);
                    errorMsg.append(cellValue+"?????????; ");
                }
            }
            ceeaMaterialItem.setCeeaMaterialDetails(ceeaMaterialDetails);
            ceeaMaterialItems.add(ceeaMaterialItem);
            errorList.add(errorMsg.toString());
        }
        return workbook;
    }

    @Override
    public void export(CeeaMaterialItemDTO materialItemDTO, HttpServletResponse response) throws ParseException, IOException {
        String monthlySchDate = materialItemDTO.getMonthlySchDate();
        Assert.notNull(materialItemDTO,"??????????????????:????????????");
        // ????????????
        Workbook workbook = crateWorkbookModel(monthlySchDate);
        // ????????????
        List<CeeaMaterialItem> ceeaMaterialItems = getCeeaMaterialItems(materialItemDTO);
        if(CollectionUtils.isNotEmpty(ceeaMaterialItems)){
            ceeaMaterialItems.forEach(ceeaMaterialItem -> {
                HashMap<String, String> ceeaMaterialDetailMap = new HashMap<>();
                List<CeeaMaterialDetail> ceeaMaterialDetails = iMaterialDetailService.list(new QueryWrapper<>(new CeeaMaterialDetail().setMaterialItemId(ceeaMaterialItem.getMaterialItemId())));
                if(CollectionUtils.isNotEmpty(ceeaMaterialDetails)){
                    ceeaMaterialDetails.forEach(ceeaMaterialDetail -> {
                        String schMonthlyDate = DateUtil.format(ceeaMaterialDetail.getSchMonthlyDate(), "yyyy-MM-dd");
                        BigDecimal requirementQuantity = ceeaMaterialDetail.getRequirementQuantity();
                        if(StringUtil.notEmpty(requirementQuantity)){
                            ceeaMaterialDetailMap.put(schMonthlyDate,StringUtil.subZeroAndDot(String.valueOf(requirementQuantity.doubleValue())));
                        }else {
                            ceeaMaterialDetailMap.put(schMonthlyDate,"0");
                        }
                    });
                }
                ceeaMaterialItem.setCeeaMaterialDetailMap(ceeaMaterialDetailMap);
            });

            // ?????????????????????
            Sheet sheet = workbook.getSheetAt(0);
            // ?????????????????????, ????????????????????????
            int totalCells = sheet.getRow(0).getLastCellNum();
            // ?????????????????????
            List<String> heads = new ArrayList<>();
            // ????????????
            Row headRow = sheet.getRow(0);
            // ???????????????????????????
            for (int i = 0; i < totalCells; i++) {
                Cell cell = headRow.getCell(i);
                heads.add(ExcelUtil.getCellValue(cell));
            }

            int rowIndex = 1;
            for (int i = 0;i < ceeaMaterialItems.size();i++){
                CeeaMaterialItem ceeaMaterialItem = ceeaMaterialItems.get(i);
                Map<String, String> ceeaMaterialDetailMap = ceeaMaterialItem.getCeeaMaterialDetailMap();
                Row row = sheet.createRow(rowIndex);
                // *????????????","*????????????","*????????????","*????????????
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(ceeaMaterialItem.getOrgName());
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(ceeaMaterialItem.getOrganizationName());
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(ceeaMaterialItem.getOrganizationSite());
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(ceeaMaterialItem.getVendorName());
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(ceeaMaterialItem.getMaterialCode());
                Cell cell5 = row.createCell(5);
                cell5.setCellValue("??????");

                // ????????????
                for(int j=6;j<totalCells;j++){
                    Cell cell = row.createCell(j);
                    String title = heads.get(j);
                    String s = ceeaMaterialDetailMap.get(title);
                    if(StringUtil.notEmpty(s)){
                        cell.setCellValue(s);
                    }else {
                        cell.setCellValue("0");
                    }
                }
                rowIndex ++;
            }
        }
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????");
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    @Override
    public void exportDetail(String materialItemId, HttpServletResponse response) throws ParseException, IOException {
        Assert.notNull(materialItemId,"??????????????????:materialItemId;");
        Workbook workbook = crateWorkbookModelDetail(materialItemId);
        HashMap<String, String> ceeaMaterialDetailMap = new HashMap<>();
        if(StringUtil.isDigit(materialItemId)){
            List<CeeaMaterialDetail> ceeaMaterialDetails = iMaterialDetailService.list(new QueryWrapper<>(new CeeaMaterialDetail().setMaterialItemId(Long.parseLong(materialItemId))));
            if(CollectionUtils.isNotEmpty(ceeaMaterialDetails)){
                ceeaMaterialDetails.forEach(ceeaMaterialDetail -> {
                    String schMonthlyDate = DateUtil.format(ceeaMaterialDetail.getSchMonthlyDate(), "yyyy-MM-dd");
                    BigDecimal requirementQuantity = ceeaMaterialDetail.getRequirementQuantity();
                    if(StringUtil.notEmpty(requirementQuantity)){
                        ceeaMaterialDetailMap.put(schMonthlyDate,StringUtil.subZeroAndDot(String.valueOf(requirementQuantity.doubleValue())));
                    }else {
                        ceeaMaterialDetailMap.put(schMonthlyDate,"0");
                    }
                });
                // ?????????????????????
                Sheet sheet = workbook.getSheetAt(0);
                // ?????????????????????, ????????????????????????
                int totalCells = sheet.getRow(0).getLastCellNum();
                // ?????????????????????
                List<String> heads = new ArrayList<>();
                // ????????????
                Row headRow = sheet.getRow(0);
                // ???????????????????????????
                for (int i = 0; i < totalCells; i++) {
                    Cell cell = headRow.getCell(i);
                    heads.add(ExcelUtil.getCellValue(cell));
                }
                Row row = sheet.createRow(1);
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(ceeaMaterialDetails.get(0).getMaterialSchNum());
                for(int j=1;j<totalCells;j++){
                    Cell cell = row.createCell(j);
                    String title = heads.get(j);
                    String s = ceeaMaterialDetailMap.get(title);
                    if(StringUtil.notEmpty(s)){
                        cell.setCellValue(s);
                    }else {
                        cell.setCellValue("0");
                    }
                }
                ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????????????????");
                workbook.write(outputStream);
                outputStream.flush();
                outputStream.close();
            }else {
                ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????????????????");
                workbook.write(outputStream);
                outputStream.flush();
                outputStream.close();
            }
        }else {
            throw new BaseException("materialItemId???????????????");
        }
    }
}
