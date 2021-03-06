package com.midea.cloud.srm.supcooperate.deliver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.pm.pr.requirement.RequirementApproveStatus;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.enums.supcooperate.DeliverPlanLineStatus;
import com.midea.cloud.common.enums.supcooperate.DeliverPlanStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.srm.feign.api.ApiClient;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.OrgCategory;
import com.midea.cloud.srm.model.suppliercooperate.deliver.dto.DeliverPlanDTO;
import com.midea.cloud.srm.model.suppliercooperate.deliver.entity.DeliverPlan;
import com.midea.cloud.srm.model.suppliercooperate.deliver.entity.DeliverPlanDetail;
import com.midea.cloud.srm.supcooperate.deliver.mapper.DeliverPlanMapper;
import com.midea.cloud.srm.supcooperate.deliver.service.IDeliverPlanDetailService;
import com.midea.cloud.srm.supcooperate.deliver.service.IDeliverPlanService;
import com.midea.cloud.srm.supcooperate.job.DeliverPlanJob;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.BindException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ????????????????????? ???????????????
 * </pre>
 *
 * @author zhi1772778785@163.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-08-27 14:42:31
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class DeliverPlanServiceImpl extends ServiceImpl<DeliverPlanMapper, DeliverPlan> implements IDeliverPlanService {
    @Resource
    DeliverPlanJob deliverPlanJob;

    @Autowired
    IDeliverPlanDetailService iDeliverPlanDetailService;

    @Resource
    private BaseClient baseClient;

    @Resource
    private SupplierClient supplierClient;

    @Resource
    private FileCenterClient fileCenterClient;

    @Resource
    private  DeliverPlanMapper deliverPlanMapper;
    @Autowired
    private ApiClient apiClient;

    private static final List<String> fixedTitle;

    private static final List<String> fixedLineTitle;

    private final ThreadPoolExecutor ioThreadPool;

    private final ForkJoinPool calculateThreadPool;


    //???????????????
    public DeliverPlanServiceImpl() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        ioThreadPool = new ThreadPoolExecutor(cpuCount * 2 + 1, cpuCount * 2 + 1,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue(),
                new NamedThreadFactory("??????????????????-http-sender", true), new ThreadPoolExecutor.CallerRunsPolicy());
        calculateThreadPool = new ForkJoinPool(cpuCount + 1);
    }

    static {
        fixedTitle = new ArrayList<>();
        fixedTitle.addAll(Arrays.asList("*????????????", "*????????????", "*????????????", "*???????????????", "*????????????", "*??????"));
        fixedLineTitle = new ArrayList<>();
        fixedLineTitle.addAll(Arrays.asList("*???????????????","*??????"));
    }

    /**
     * ????????????
     *
     * @param deliverPlanDTO
     * @return
     */
    @Override
    public PageInfo<DeliverPlan> getdeliverPlanListPage(DeliverPlanDTO deliverPlanDTO) {
        PageUtil.startPage(deliverPlanDTO.getPageNum(), deliverPlanDTO.getPageSize());
        List<DeliverPlan> deliverPlans = getDeliverPlans(deliverPlanDTO);
        return new PageInfo<>(deliverPlans);
    }

    @AuthData(module = {MenuEnum.VENDOR_DELIVER_PLAN  , MenuEnum.SUPPLIER_SIGN})
    private List<DeliverPlan> getDeliverPlans(DeliverPlanDTO deliverPlanDTO) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (ObjectUtils.isEmpty(loginAppUser)){
            return new ArrayList<>();
        }
        QueryWrapper<DeliverPlan> wrapper = new QueryWrapper<>();
        //??????????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(deliverPlanDTO.getOrgIds()), "ORG_ID", deliverPlanDTO.getOrgIds());
        //???????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(deliverPlanDTO.getOrganizationIds()), "ORGANIZATION_ID", deliverPlanDTO.getOrganizationIds());
        //????????????????????????
        wrapper.like(StringUtils.isNotEmpty(deliverPlanDTO.getDeliveryAddress()), "DELIVERY_ADDRESS", deliverPlanDTO.getDeliveryAddress());
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        if (user.getUserType() != null && user.getUserType().equals("VENDOR")) {
            Assert.notNull(user.getCompanyId(),"?????????????????????????????????????????????????????????????????????");
                //?????????ID??????
            wrapper.eq("VENDOR_ID", user.getCompanyId());
            wrapper.eq("DELIVER_PLAN_STATUS", DeliverPlanStatus.APPROVAL.toString());
        }else {
            //??????????????????????????????
            wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getDeliverPlanStatus()), "DELIVER_PLAN_STATUS", deliverPlanDTO.getDeliverPlanStatus());

        }
        //?????????????????????
        wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getVendorCode()), "VENDOR_CODE", deliverPlanDTO.getVendorCode());
        //?????????????????????
        wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getVendorName()), "VENDOR_NAME", deliverPlanDTO.getVendorName());
        //??????????????????????????????
        wrapper.like(StringUtils.isNotEmpty(deliverPlanDTO.getMaterialCode()), "MATERIAL_CODE", deliverPlanDTO.getMaterialCode());
        //??????????????????????????????
        wrapper.like(StringUtils.isNotEmpty(deliverPlanDTO.getMaterialName()), "MATERIAL_NAME", deliverPlanDTO.getMaterialName());
        //???????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(deliverPlanDTO.getCategoryIds()), "CATEGORY_ID", deliverPlanDTO.getCategoryIds());
        //????????????????????????????????????????????????????????????
/*        if (loginAppUser != null) {
            String userType = loginAppUser.getUserType();
            wrapper.eq(StringUtils.isNotEmpty(userType) && userType.equals("VENDOR"), "DELIVER_PLAN_STATUS", DeliverPlanStatus.APPROVAL.toString());
        } else {
            //??????????????????????????????
            wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getDeliverPlanLineStatus()), "DELIVER_PLAN_STATUS", deliverPlanDTO.getDeliverPlanLineStatus());
        }*/
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getMonthlySchDate()), "MONTHLY_SCH_DATE", deliverPlanDTO.getMonthlySchDate());
        //???????????????????????????
        wrapper.like(StringUtils.isNotEmpty(deliverPlanDTO.getDeliverPlanNum()), "DELIVER_PLAN_NUM", deliverPlanDTO.getDeliverPlanNum());
        //???????????????
        wrapper.eq(deliverPlanDTO.getVersion() != null, "VERSION", deliverPlanDTO.getVersion());

        if (StringUtils.isNotEmpty(deliverPlanDTO.getDeliverPlanLineStatus())) {
            //?????????????????????
            wrapper.gt("(SELECT count(b.DELIVER_PLAN_DETAIL_ID) FROM ceea_sc_deliver_plan_detail b \n" +
                    "WHERE DELIVER_PLAN_ID=b.DELIVER_PLAN_ID and b.DELIVER_PLAN_STATUS='"+deliverPlanDTO.getDeliverPlanLineStatus()+"')","0");
        }
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return this.list(wrapper);
    }
    /**
     * ????????????
     *
     * @param id
     * @return
     */
    @Override
    public DeliverPlanDTO getDeliverPlan(Long id) {
        DeliverPlanDTO deliverPlanDTO = new DeliverPlanDTO();
        deliverPlanDTO.setDeliverPlan(this.getById(id));
        QueryWrapper<DeliverPlanDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("DELIVER_PLAN_ID", id);
        wrapper.orderByAsc("SCH_MONTHLY_DATE");
        List<DeliverPlanDetail> list = iDeliverPlanDetailService.list(wrapper);
        deliverPlanDTO.setDeliverPlanDetailList(list);
        return deliverPlanDTO;
    }

    /**
     * ??????????????????
     *
     * @param deliverPlanDTO
     */
    @Transactional
    @Override
    public void modifyDeliverPlan(DeliverPlanDTO deliverPlanDTO) {
        DeliverPlan deliverPlan = deliverPlanDTO.getDeliverPlan();
        Assert.notNull(deliverPlan, "????????????????????????");
        List<DeliverPlanDetail> deliverPlanDetailList = deliverPlanDTO.getDeliverPlanDetailList();
        Assert.notEmpty(deliverPlanDetailList, "???????????????????????????");
        //??????????????????????????????????????????id
        PurchaseCategory purchaseCategory = baseClient.MinByIfBeyondDeliver(deliverPlan.getCategoryId());
        BigDecimal i = new BigDecimal(0);
        ArrayList<Long> longs = new ArrayList<>();
        boolean falg=false;
        //????????????==??????????????????????????????????????????????????????
        for (DeliverPlanDetail deliverPlanDetail : deliverPlanDetailList) {
            //?????????????????????????????????
            Assert.isTrue(deliverPlanDetail.getDeliverPlanLock() != "1", "??????????????????????????????????????????????????????");
            //????????????
            BigDecimal bigDecimal1 = deliverPlanDetail.getRequirementQuantity();
            //??????????????????
            BigDecimal bigDecimal2 = deliverPlanDetail.getQuantityPromised();
            if (purchaseCategory==null){
                Assert.isTrue(bigDecimal2.compareTo(bigDecimal1) != 1,"?????????????????????????????????????????????????????????????????????????????????????????????????????????");
            }
            //????????????
            if (bigDecimal1.compareTo(bigDecimal2) == 0) {
                deliverPlanDetail.setDeliverPlanStatus(DeliverPlanLineStatus.COMFIRM.toString());
                falg=true;
            } else {
                deliverPlanDetail.setDeliverPlanStatus(DeliverPlanLineStatus.UNCOMFIRMED.toString());
            }
            //?????????????????????????????????????????????
            i = i.add(bigDecimal1);
            //???????????????????????????id
            longs.add(deliverPlanDetail.getDeliverPlanDetailId());
            iDeliverPlanDetailService.updateById(deliverPlanDetail);
        }
        QueryWrapper<DeliverPlanDetail> wrapper = new QueryWrapper<>();
        wrapper.select("SUM(REQUIREMENT_QUANTITY)AS REQUIREMENT_QUANTITY");
        wrapper.eq("DELIVER_PLAN_ID", deliverPlan.getDeliverPlanId());
        wrapper.notIn("DELIVER_PLAN_DETAIL_ID", longs);
        List<DeliverPlanDetail> list = iDeliverPlanDetailService.list(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            i = i.add(list.get(0).getRequirementQuantity());
        }
        deliverPlan.setSchTotalQuantity(i);
        this.updateById(deliverPlan);
        //???????????????mrp
        getAffirmByMrp(falg);
    }

    @Override
    public void importModelDownload(String monthlySchDate, HttpServletResponse response) throws IOException, ParseException {
        Assert.notNull(monthlySchDate, "????????????????????????: monthlySchDate");
        // ????????????
        Workbook workbook = crateWorkbookModel(monthlySchDate);
        // ???????????????
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????????????????");
        // ??????
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
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
                String msg = "???????????????\"??????\"???\"??????\"??????????????????????????????????????????????????????";
                LoginAppUser user = AppUserUtil.getLoginAppUser();
                if (null != user && UserType.VENDOR.name().equals(user.getUserType())) {
                    msg = "???????????????????????????\"??????\"???????????????????????????????????????";
                }
                EasyExcelUtil.setCellStyle(workbook, cell1, sheet, msg, fixedTitle.get(i));
            }
            cellIndex++;
        }

        // ?????????????????????
        Date date = DateUtil.parseDate(monthlySchDate);
        LocalDate localDate = DateUtil.dateToLocalDate(date);
        List<String> dayBetween = DateUtil.getDayBetween(localDate, "yyyy-MM-dd");
        for (int i = 0; i < dayBetween.size(); i++) {
            Cell cell1 = row.createCell(cellIndex);
            cell1.setCellValue(dayBetween.get(i));
            cell1.setCellStyle(cellStyle);
            cellIndex++;
        }
        return workbook;
    }

    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload) throws Exception {
        // ??????????????????
        EasyExcelUtil.checkParam(file, fileupload);
        // ?????????????????????
        InputStream inputStream = file.getInputStream();
        // ???????????????????????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ???????????????
        List<DeliverPlan> deliverPlans = new ArrayList<>();
        // ??????????????????
        List<String> errorList = new ArrayList<>();
        // ????????????
        Map<String, Object> result = new HashMap<>();
        result.put("status", YesOrNo.YES.getValue());
        result.put("message", "success");
        // ??????????????????
        Workbook workbook = this.getImportData(inputStream, deliverPlans, errorList, errorFlag);
        if (errorFlag.get()) {
            // ?????????,??????????????????
            this.uploadErrorFile(file, fileupload, errorList, result, workbook);
        } else {
            if (CollectionUtils.isNotEmpty(deliverPlans)) {
                HashSet<Long> deliverPlanIdUpdate = new HashSet<>();
                HashSet<Long> deliverPlanIdAdd = new HashSet<>();
                deliverPlans.forEach(deliverPlan -> {
                    // ?????????????????? ????????????+????????????+????????????+?????????+????????????
                    List<DeliverPlan> deliverPlanList = this.list(new QueryWrapper<>(new DeliverPlan().setOrgId(deliverPlan.getOrgId()).
                            setOrganizationId(deliverPlan.getOrganizationId()).setDeliveryAddress(deliverPlan.getDeliveryAddress()).
                            setVendorId(deliverPlan.getVendorId()).setMaterialId(deliverPlan.getMaterialId())));
                    if(CollectionUtils.isNotEmpty(deliverPlanList)){
                        // ??????
                        DeliverPlan deliverPlan1 = deliverPlanList.get(0);
                        // ???id
                        Long deliverPlanId = deliverPlan1.getDeliverPlanId();
                        String projectFlag = deliverPlan.getProjectFlag();
                        deliverPlanIdUpdate.add(deliverPlanId);
                        this.updateById(deliverPlan1);

                        List<DeliverPlanDetail> deliverPlanDetails = deliverPlan.getDeliverPlanDetails();
                        deliverPlanDetails.forEach(deliverPlanDetail -> {
                            // ??????
                            List<DeliverPlanDetail> planDetails = iDeliverPlanDetailService.list(new QueryWrapper<>(new DeliverPlanDetail().setDeliverPlanId(deliverPlanId).setSchMonthlyDate(deliverPlanDetail.getSchMonthlyDate())));
                            if(CollectionUtils.isNotEmpty(planDetails)){
                                // ??????
                                DeliverPlanDetail planDetail = planDetails.get(0);
                                // ??????????????????
                                if (!"1".equals(planDetail.getDeliverPlanLock())) {
                                    if ("0".equals(projectFlag)) {
                                        planDetail.setRequirementQuantity(deliverPlanDetail.getRequirementQuantity());
                                    }else {
                                        planDetail.setQuantityPromised(deliverPlanDetail.getQuantityPromised());
                                    }
                                    // ????????????
                                    if(planDetail.getQuantityPromised().compareTo(BigDecimal.ZERO) != 0 && planDetail.getRequirementQuantity().compareTo(BigDecimal.ZERO) != 0
                                            && planDetail.getRequirementQuantity().compareTo(planDetail.getQuantityPromised()) == 0){
                                        planDetail.setDeliverPlanStatus(DeliverPlanLineStatus.COMFIRM.name());
                                    }
                                    iDeliverPlanDetailService.updateById(planDetail);
                                }
                            }else {
                                // ??????
                                deliverPlanDetail.setDeliverPlanDetailId(IdGenrator.generate());
                                deliverPlanDetail.setDeliverPlanId(deliverPlanId);
                                deliverPlanDetail.setDeliverPlanStatus(DeliverPlanLineStatus.UNCOMFIRMED.name());
                                iDeliverPlanDetailService.save(deliverPlanDetail);
                            }
                        });
                    }else {
                        // ??????
                        LocalDate localDate = deliverPlan.getDeliverPlanDetails().get(0).getSchMonthlyDate();
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
                        // ????????????
                        String monthlySchDate = localDate.format(dateTimeFormatter);
                        deliverPlan.setMonthlySchDate(monthlySchDate);
                        Long deliverPlanId = IdGenrator.generate();
                        deliverPlanIdAdd.add(deliverPlanId);
                        deliverPlan.setDeliverPlanId(deliverPlanId);
                        // ???????????????
                        deliverPlan.setDeliverPlanNum(baseClient.seqGen(SequenceCodeConstant.SEQ_CEEA_DELIVER_PLANNUM_CODE));
                        deliverPlan.setDeliverPlanStatus("DRAFT");
                        deliverPlan.setVersion(0L);
                        this.save(deliverPlan);
                        // ????????????
                        List<DeliverPlanDetail> deliverPlanDetails = deliverPlan.getDeliverPlanDetails();
                        deliverPlanDetails.forEach(deliverPlanDetail -> {
                            deliverPlanDetail.setDeliverPlanId(deliverPlanId);
                            deliverPlanDetail.setDeliverPlanDetailId(IdGenrator.generate());
                            deliverPlanDetail.setDeliverPlanLock("2");
                            deliverPlanDetail.setDeliverPlanStatus(DeliverPlanLineStatus.UNCOMFIRMED.name());
                            iDeliverPlanDetailService.save(deliverPlanDetail);
                        });
                    }
                });
                // ????????????
                if(CollectionUtils.isNotEmpty(deliverPlanIdUpdate)){
                    deliverPlanIdUpdate.forEach(id->{
                        this.baseMapper.updateSchTotalQuantity(id);
                    });
                    QueryWrapper<DeliverPlan> queryWrapper = new QueryWrapper<>();
                    queryWrapper.in("DELIVER_PLAN_ID",deliverPlanIdUpdate);
                    List<DeliverPlan> list = this.list(queryWrapper);
                    if(CollectionUtils.isNotEmpty(list)){
                        list.forEach(deliverPlan -> {
                            deliverPlan.setVersion(deliverPlan.getVersion() + 1);
                        });
                        this.updateBatchById(list);
                    }
                }
                if(CollectionUtils.isNotEmpty(deliverPlanIdAdd)){
                    deliverPlanIdUpdate.forEach(id->{
                        this.baseMapper.updateSchTotalQuantity(id);
                    });
                    QueryWrapper<DeliverPlan> queryWrapper = new QueryWrapper<>();
                    queryWrapper.in("DELIVER_PLAN_ID",deliverPlanIdAdd);
                    List<DeliverPlan> list = this.list(queryWrapper);
                    if(CollectionUtils.isNotEmpty(list)){
                        list.forEach(deliverPlan -> {
                            deliverPlan.setVersion(0L);
                        });
                        this.updateBatchById(list);
                    }
                }
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
        for (int i = 1; i <= totalRows; i++) {
            Cell cell = sheet.getRow(i).createCell(totalCells);
            cell.setCellValue(errorList.get(i - 1));
        }
        Fileupload fileupload1 = ExcelUtil.uploadErrorFile(fileCenterClient, fileupload, workbook, file);
        result.put("status", YesOrNo.NO.getValue());
        result.put("message", "error");
        result.put("fileuploadId", fileupload1.getFileuploadId());
        result.put("fileName", fileupload1.getFileSourceName());
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
     *
     * @param inputStream
     * @param deliverPlans
     * @param errorList
     * @throws IOException
     * @throws ParseException
     */
    private Workbook getImportData(InputStream inputStream, List<DeliverPlan> deliverPlans, List<String> errorList, AtomicBoolean errorFlag) throws IOException, ParseException {
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
        Map<String, Map<LocalDate,BigDecimal>> deliverPlanMap = new HashMap<>();


        // ???????????????????????????,???2?????????,????????????????????????1
        for (int r = 1; r <= totalRows; r++) {
            log.info("???"+r+"?????????");
            Row row = sheet.getRow(r);
            if (null == row) {
                // ????????????,????????????????????????????????????
                sheet.shiftRows(r + 1, totalRows, -1);
                r--;
                totalRows--;
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
                totalRows--;
                continue;

            }
// <------------------------------------????????????????????????????????????-------------------------------------------->
            // ????????????????????????
            StringBuffer errorMsg = new StringBuffer();
            // ??????????????????????????????
            DeliverPlan deliverPlan = new DeliverPlan();
            List<DeliverPlanDetail> deliverPlanDetails = new ArrayList<>();
            StringBuffer uniKey = new StringBuffer();
            StringBuffer uniKeyQuery = new StringBuffer();

            // ???????????????????????????
            String ceeaIfBeyondDeliver = "N";

            boolean lineErrorFlag = true;

            // ??????????????????
            Cell cell0 = row.getCell(0);
            String orgName = ExcelUtil.getCellValue(cell0);
            if (StringUtil.notEmpty(orgName)) {
                orgName = orgName.trim();
                uniKey.append(orgName);
                uniKeyQuery.append(orgName);
                Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgName));
                if (null != organization && StringUtil.notEmpty(organization.getOrganizationId())) {
                    deliverPlan.setOrgId(organization.getOrganizationId());
                    deliverPlan.setOrgCode(organization.getOrganizationCode());
                    deliverPlan.setOrgName(organization.getOrganizationCode());
                } else {
                    errorFlag.set(true);
                    lineErrorFlag= false;
                    errorMsg.append("?????????????????????; ");
                }
            } else {
                errorFlag.set(true);
                lineErrorFlag= false;
                errorMsg.append("????????????????????????; ");
            }

            // ??????????????????
            Cell cell1 = row.getCell(1);
            String organizationName = ExcelUtil.getCellValue(cell1);
            if (StringUtil.notEmpty(organizationName)) {
                organizationName = organizationName.trim();
                uniKey.append(organizationName);
                uniKeyQuery.append(organizationName);
                Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(organizationName));
                if (null != organization && StringUtil.notEmpty(organization.getOrganizationId())) {
                    if (StringUtil.notEmpty(organization.getParentOrganizationIds())) {
                        if (StringUtil.notEmpty(deliverPlan.getOrgId()) && organization.getParentOrganizationIds().contains(deliverPlan.getOrgId().toString())) {
                            deliverPlan.setOrganizationId(organization.getOrganizationId());
                            deliverPlan.setOrganizationName(organization.getOrganizationName());
                            deliverPlan.setOrganizationCode(organization.getOrganizationCode());
                        } else {
                            errorFlag.set(true);
                            lineErrorFlag= false;
                            errorMsg.append("?????????????????????????????????????????????; ");
                        }
                    } else {
                        errorFlag.set(true);
                        lineErrorFlag= false;
                        errorMsg.append("???????????????????????????; ");
                    }
                } else {
                    errorFlag.set(true);
                    lineErrorFlag= false;
                    errorMsg.append("?????????????????????; ");
                }
            } else {
                errorFlag.set(true);
                lineErrorFlag= false;
                errorMsg.append("????????????????????????; ");
            }

            // ????????????
            Cell cell2 = row.getCell(2);
            String deliveryAddress = ExcelUtil.getCellValue(cell2);
            if (StringUtil.notEmpty(deliveryAddress)) {
                deliveryAddress = deliveryAddress.trim();
                uniKey.append(deliveryAddress);
                uniKeyQuery.append(deliveryAddress);
                deliverPlan.setDeliveryAddress(deliveryAddress);
            } else {
                errorFlag.set(true);
                lineErrorFlag= false;
                errorMsg.append("????????????????????????; ");
            }

            // ???????????????
            Cell cell3 = row.getCell(3);
            String vendorName = ExcelUtil.getCellValue(cell3);
            if (StringUtil.notEmpty(vendorName)) {
                vendorName = vendorName.trim();
                uniKey.append(vendorName);
                uniKeyQuery.append(vendorName);
                CompanyInfo companyInfo = supplierClient.getCompanyInfoByParam(new CompanyInfo().setCompanyName(vendorName));
                if (null != companyInfo && StringUtil.notEmpty(companyInfo.getCompanyId())) {
                    if (StringUtil.notEmpty(companyInfo.getCompanyCode())) {
                        deliverPlan.setVendorId(companyInfo.getCompanyId());
                        deliverPlan.setVendorCode(companyInfo.getCompanyCode());
                        deliverPlan.setVendorName(companyInfo.getCompanyName());
                    }else {
                        errorFlag.set(true);
                        lineErrorFlag= false;
                        errorMsg.append("????????????????????????; ");
                    }
                } else {
                    errorFlag.set(true);
                    lineErrorFlag= false;
                    errorMsg.append("??????????????????; ");
                }
            } else {
                errorFlag.set(true);
                lineErrorFlag= false;
                errorMsg.append("???????????????????????????; ");
            }


            PurchaseCategory purchaseCategorycopy=null;
            // ????????????
            Cell cell4 = row.getCell(4);
            String materialCode = ExcelUtil.getCellValue(cell4);
            if (StringUtil.notEmpty(materialCode)) {
                materialCode = materialCode.trim();
                uniKey.append(materialCode);
                uniKeyQuery.append(materialCode);
                List<MaterialItem> materialItems = baseClient.listMaterialByParam(new MaterialItem().setMaterialCode(materialCode));
                if (CollectionUtils.isNotEmpty(materialItems)) {
                    MaterialItem materialItem = materialItems.get(0);
                    deliverPlan.setMaterialId(materialItem.getMaterialId());
                    deliverPlan.setMaterialCode(materialItem.getMaterialCode());
                    deliverPlan.setMaterialName(materialItem.getMaterialName());
                    if (materialItem.getStruct() == null){
                        errorFlag.set(true);
                        lineErrorFlag= false;
                        errorMsg.append("?????????????????????; ");
                    }else {
                        //?????????????????????????????????
                        PurchaseCategory purchaseCategory = baseClient.checkByIfDeliverPlan(materialItem.getStruct());
                        if (purchaseCategory==null){
                            errorFlag.set(true);
                            lineErrorFlag= false;
                            errorMsg.append("?????????????????????????????????????????????????????????; ");
                        }else {
                            deliverPlan.setCategoryId(purchaseCategory.getCategoryId());
                            deliverPlan.setCategoryName(purchaseCategory.getCategoryName());
                            deliverPlan.setUnit(materialItem.getUnit());
                            deliverPlan.setStruct(purchaseCategory.getStruct());
                            ceeaIfBeyondDeliver = StringUtil.notEmpty(purchaseCategory.getCeeaIfBeyondDeliver()) ? purchaseCategory.getCeeaIfBeyondDeliver() : "N";
                        }
                    }
                } else {
                    errorFlag.set(true);
                    lineErrorFlag= false;
                    errorMsg.append("?????????????????????; ");
                }
            } else {
                errorFlag.set(true);
                lineErrorFlag= false;
                errorMsg.append("????????????????????????; ");
            }

            /**
             * ?????? ????????? + ???????????? + ??????  ??????  ??????????????????????????????
             */
            if(StringUtil.notEmpty(deliverPlan.getVendorId())
                    && StringUtil.notEmpty(deliverPlan.getOrgId())
                    && StringUtil.notEmpty(deliverPlan.getStruct())){
                List<OrgCategory> orgCategoryList = supplierClient.getOrgCategoryByOrgCategory(new OrgCategory().
                        setCompanyId(deliverPlan.getVendorId()).
                        setCategoryFullId(deliverPlan.getStruct()).
                        setOrgId(deliverPlan.getOrgId()));
                if(CollectionUtils.isNotEmpty(orgCategoryList)){
                    String serviceStatus = orgCategoryList.get(0).getServiceStatus();
                    /**
                     * ?????????????????????????????? GREEN???VERIFY???ONE_TIME???YELLOW ????????????????????????
                     */
                    if(!"GREEN".equals(serviceStatus) &&
                       !"VERIFY".equals(serviceStatus) &&
                       !"ONE_TIME".equals(serviceStatus) &&
                       !"YELLOW".equals(serviceStatus)){
                        errorFlag.set(true);
                        lineErrorFlag= false;
                        errorMsg.append("??????????????????????????????????????????; ");
                    }
                }else {
                    errorFlag.set(true);
                    lineErrorFlag= false;
                    errorMsg.append("?????????+????????????+????????????:?????????????????????; ");
                }

            }
            String userType = AppUserUtil.getLoginAppUser().getUserType();
            // ?????? : ??????-????????????  ??????-??????????????????
            Cell cell5 = row.getCell(5);
            String project = ExcelUtil.getCellValue(cell5);
            if (StringUtil.notEmpty(project)) {
                project = project.trim();
                uniKey.append(project);
                if ("BUYER".equals(userType)) {
                    if ("??????".equals(project) || "??????".equals(project)) {
                        if ("??????".equals(project)) {
                            deliverPlan.setProjectFlag("1");
                        } else {
                            deliverPlan.setProjectFlag("0");
                        }
                    } else {
                        errorFlag.set(true);
                        lineErrorFlag= false;
                        errorMsg.append("???????????????????????????\"??????\"???\"??????\"; ");
                    }
                } else if ("VENDOR".equals(userType)) {
                    if ("??????".equals(project)) {
                        deliverPlan.setProjectFlag("1");
                    } else {
                        errorFlag.set(true);
                        lineErrorFlag= false;
                        errorMsg.append("?????????:???????????????\"??????\"; ");
                    }
                }
            } else {
                errorFlag.set(true);
                lineErrorFlag= false;
                errorMsg.append("??????????????????; ");
            }

            if (!hashSet.add(uniKey.toString())) {
                errorFlag.set(true);
                lineErrorFlag= false;
                errorMsg.append("??????????????????????????????; ");
            }else {
                // ????????????????????????
                for (int c = 6; c < totalCells; c++) {
                    Cell cell = row.getCell(c);
                    String cellValue = ExcelUtil.getCellValue(cell);
                    if (StringUtil.notEmpty(cellValue)) {
                        cellValue = cellValue.trim();
                    }else {
                        cellValue = "0";
                    }
                    if (StringUtil.isDigit(cellValue)) {
                        DeliverPlanDetail deliverPlanDetail = new DeliverPlanDetail();
                        Date date = DateUtil.parseDate(head.get(c));
                        deliverPlanDetail.setSchMonthlyDate(DateUtil.dateToLocalDate(date));
                        if ("0".equals(deliverPlan.getProjectFlag())) {
                            deliverPlanDetail.setRequirementQuantity(new BigDecimal(cellValue));
                        } else {
                            deliverPlanDetail.setQuantityPromised(new BigDecimal(cellValue));
                        }
                        deliverPlanDetails.add(deliverPlanDetail);
                    } else {
                        errorFlag.set(true);
                        errorMsg.append(cellValue + "?????????; ");
                    }
                }
                // ????????????+????????????+????????????+?????????+????????????
                if(lineErrorFlag && YesOrNo.NO.getValue().equals(ceeaIfBeyondDeliver)){
                    if("1".equals(deliverPlan.getProjectFlag())){
                        if ("BUYER".equals(userType)) {
                            // ??????
                            uniKeyQuery.append("??????");
                            Map<LocalDate, BigDecimal> decimalMap = deliverPlanMap.get(uniKeyQuery.toString());
                            if(null != decimalMap && !decimalMap.isEmpty()){
                                if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
                                    for(DeliverPlanDetail deliverPlanDetail : deliverPlanDetails){
                                        LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                        BigDecimal quantityPromised = deliverPlanDetail.getQuantityPromised();
                                        BigDecimal decimal = null != decimalMap.get(schMonthlyDate) ? decimalMap.get(schMonthlyDate) : BigDecimal.ZERO;
                                        if(quantityPromised.compareTo(decimal) > 0){
                                            errorFlag.set(true);
                                            errorMsg.append("????????????????????????????????????; ");
                                            break;
                                        }
                                    }
                                }
                            }else {
                                // ?????????????????????
                                // ?????????????????? ????????????+????????????+????????????+?????????+????????????
                                List<DeliverPlan> deliverPlanList = this.list(new QueryWrapper<>(new DeliverPlan().setOrgId(deliverPlan.getOrgId()).
                                        setOrganizationId(deliverPlan.getOrganizationId()).setDeliveryAddress(deliverPlan.getDeliveryAddress()).
                                        setVendorId(deliverPlan.getVendorId()).setMaterialId(deliverPlan.getMaterialId())));
                                if(CollectionUtils.isNotEmpty(deliverPlanList)){
                                    Long deliverPlanId = deliverPlanList.get(0).getDeliverPlanId();
                                    List<DeliverPlanDetail> deliverPlanDetailList = iDeliverPlanDetailService.list(new QueryWrapper<>(new DeliverPlanDetail().setDeliverPlanId(deliverPlanId)));
                                    if(CollectionUtils.isNotEmpty(deliverPlanDetailList)){
                                        Map<LocalDate, BigDecimal> bigDecimalMap = deliverPlanDetailList.stream().collect(Collectors.toMap(DeliverPlanDetail::getSchMonthlyDate, DeliverPlanDetail::getRequirementQuantity));
                                        if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
                                            for(DeliverPlanDetail deliverPlanDetail : deliverPlanDetails){
                                                LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                                BigDecimal quantityPromised = deliverPlanDetail.getQuantityPromised();
                                                BigDecimal decimal = null != bigDecimalMap.get(schMonthlyDate) ? bigDecimalMap.get(schMonthlyDate) : BigDecimal.ZERO;
                                                if(quantityPromised.compareTo(decimal) > 0){
                                                    errorFlag.set(true);
                                                    errorMsg.append("????????????????????????????????????; ");
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }else {
                                    errorFlag.set(true);
                                    errorMsg.append("??????????????????,???????????????\"??????\"?????????\"??????\"?????????,??????????????????????????????????????????; ");
                                }
                            }
                        }else {
                            // ?????????????????? ????????????+????????????+????????????+?????????+????????????
                            List<DeliverPlan> deliverPlanList = this.list(new QueryWrapper<>(new DeliverPlan().setOrgId(deliverPlan.getOrgId()).
                                    setOrganizationId(deliverPlan.getOrganizationId()).setDeliveryAddress(deliverPlan.getDeliveryAddress()).
                                    setVendorId(deliverPlan.getVendorId()).setMaterialId(deliverPlan.getMaterialId())));
                            if(CollectionUtils.isNotEmpty(deliverPlanList)){
                                Long deliverPlanId = deliverPlanList.get(0).getDeliverPlanId();
                                List<DeliverPlanDetail> deliverPlanDetailList = iDeliverPlanDetailService.list(new QueryWrapper<>(new DeliverPlanDetail().setDeliverPlanId(deliverPlanId)));
                                if(CollectionUtils.isNotEmpty(deliverPlanDetailList)){
                                    Map<LocalDate, BigDecimal> bigDecimalMap = deliverPlanDetailList.stream().collect(Collectors.toMap(DeliverPlanDetail::getSchMonthlyDate, DeliverPlanDetail::getRequirementQuantity));
                                    if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
                                        for(DeliverPlanDetail deliverPlanDetail : deliverPlanDetails){
                                            LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                            BigDecimal quantityPromised = deliverPlanDetail.getQuantityPromised();
                                            BigDecimal decimal = null != bigDecimalMap.get(schMonthlyDate) ? bigDecimalMap.get(schMonthlyDate) : BigDecimal.ZERO;
                                            if(quantityPromised.compareTo(decimal) > 0){
                                                errorFlag.set(true);
                                                errorMsg.append("????????????????????????????????????; ");
                                                break;
                                            }
                                        }
                                    }
                                }
                            }else {
                                errorFlag.set(true);
                                errorMsg.append("??????????????????,????????????; ");
                            }
                        }
                    }else {
                        // ??????
                        if (CollectionUtils.isNotEmpty(deliverPlanDetails)){
                            Map<LocalDate, BigDecimal> map = new HashMap<>();
                            deliverPlanDetails.forEach(deliverPlanDetail -> {
                                map.put(deliverPlanDetail.getSchMonthlyDate(),deliverPlanDetail.getRequirementQuantity());
                            });
                            deliverPlanMap.put(uniKey.toString(),map);
                        }
                    }
                }
            }

            deliverPlan.setDeliverPlanDetails(deliverPlanDetails);
            deliverPlans.add(deliverPlan);

            errorList.add(errorMsg.toString());
        }
        return workbook;
    }

    @Override
    public void importLineModelDownload(Long deliverPlanId, HttpServletResponse response) throws Exception {
        Assert.notNull(deliverPlanId, "??????????????????ID: deliverPlanId");
        // ????????????
        Workbook workbook = crateWorkbookLineModel(deliverPlanId);
        // ???????????????
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????????????????");
        // ??????
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * ??????????????????
     */
    public Workbook crateWorkbookLineModel(Long deliverPlanId) throws ParseException {
        DeliverPlan deliverPlan = this.getById(deliverPlanId);
        Assert.notNull(deliverPlan,"?????????????????????,Id:"+deliverPlanId);
        String monthlySchDate = deliverPlan.getMonthlySchDate();
        Assert.notNull(monthlySchDate,"????????????????????????");
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

        boolean isVendor = AppUserUtil.userIsVendor();

        // ?????????????????????
        for (int i = 0; i < fixedLineTitle.size(); i++) {
            XSSFCell cell1 = row.createCell(cellIndex);
            if (i != fixedLineTitle.size() - 1) {
                cell1.setCellValue(fixedLineTitle.get(i));
                cell1.setCellStyle(cellStyle);
            } else {
                String msg = "1??????????????????\"??????\"???\"??????\"??????????????????????????????????????????????????????";
                if(isVendor){
                    msg = "1??????????????????\"??????\"?????????????????????????????????";
                }
                EasyExcelUtil.setCellStyle(workbook, cell1, sheet, msg, fixedLineTitle.get(i));
            }
            cellIndex++;
        }

        // ?????????????????????
        Date date = DateUtil.parseDate(monthlySchDate);
        LocalDate localDate = DateUtil.dateToLocalDate(date);
        List<String> dayBetween = DateUtil.getDayBetween(localDate, "yyyy-MM-dd");
        for (int i = 0; i < dayBetween.size(); i++) {
            Cell cell1 = row.createCell(cellIndex);
            cell1.setCellValue(dayBetween.get(i));
            cell1.setCellStyle(cellStyle);
            cellIndex++;
        }
        return workbook;
    }

    @Override
    public Map<String, Object> importLineExcel(MultipartFile file, Long deliverPlanId, Fileupload fileupload) throws Exception {
        Assert.notNull(deliverPlanId,"????????????");
        // ??????id
        DeliverPlan deliverPlan = this.getById(deliverPlanId);
        Assert.notNull(deliverPlan,"????????????????????????:"+deliverPlanId);
        //??????????????????????????????
        // ????????????
        String deliverPlanNum = deliverPlan.getDeliverPlanNum();
        Assert.notNull(deliverPlanNum,"??????????????????");
        // ??????????????????
        EasyExcelUtil.checkParam(file, fileupload);
        // ?????????????????????
        InputStream inputStream = file.getInputStream();
        // ???????????????????????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ???????????????
        Map<String,List<DeliverPlanDetail>> deliverPlanDetailTyppeMap = new HashMap<>();
        // ??????????????????
        List<String> errorList = new ArrayList<>();
        // ????????????
        Map<String, Object> result = new HashMap<>();
        result.put("status", YesOrNo.YES.getValue());
        result.put("message", "success");
        // ??????????????????
        Workbook workbook = this.getImportLineData(deliverPlan,inputStream, errorList, errorFlag,deliverPlanDetailTyppeMap);
        if (errorFlag.get()) {
            // ?????????,??????????????????
            this.uploadErrorFile(file, fileupload, errorList, result, workbook);
        }else {
            List<DeliverPlanDetail> deliverPlanDetails0 = deliverPlanDetailTyppeMap.get("0");
            List<DeliverPlanDetail> deliverPlanDetails1 = deliverPlanDetailTyppeMap.get("1");
            HashMap<LocalDate, DeliverPlanDetail> planDetailHashMap = new HashMap<>();
            if(CollectionUtils.isNotEmpty(deliverPlanDetails0)){
                deliverPlanDetails0.forEach(deliverPlanDetail -> {
                    LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                    planDetailHashMap.put(schMonthlyDate,deliverPlanDetail);
                });
            }
            if(CollectionUtils.isNotEmpty(deliverPlanDetails1)){
                deliverPlanDetails1.forEach(deliverPlanDetail -> {
                    LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                    DeliverPlanDetail planDetail = planDetailHashMap.get(schMonthlyDate);
                    if(null != planDetail){
                        planDetail.setQuantityPromised(deliverPlanDetail.getQuantityPromised());
                        planDetailHashMap.put(schMonthlyDate,planDetail);
                    }
                });
            }
            ArrayList<DeliverPlanDetail> deliverPlanDetails = new ArrayList<>();
            if(!planDetailHashMap.isEmpty()){
                planDetailHashMap.forEach((localDate, deliverPlanDetail) -> {
                    deliverPlanDetails.add(deliverPlanDetail);
                });
            }

            if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
                deliverPlanDetails.forEach(deliverPlanDetail -> {
                    /**
                     * ?????????????????????, ???????????????, ??????????????????
                     * ?????? ???id + ??????
                     */
                    DeliverPlanDetail planDetail = iDeliverPlanDetailService.getOne(new QueryWrapper<>(new DeliverPlanDetail().
                            setDeliverPlanId(deliverPlanId).setSchMonthlyDate(deliverPlanDetail.getSchMonthlyDate())));
                    if(null != planDetail){
                        // ??????
                        // ??????????????????
                        if (!"1".equals(planDetail.getDeliverPlanLock())) {
                            if(StringUtil.notEmpty(deliverPlanDetail.getRequirementQuantity())){
                                planDetail.setRequirementQuantity(deliverPlanDetail.getRequirementQuantity());
                            }
                            if(StringUtil.notEmpty(deliverPlanDetail.getQuantityPromised())){
                                planDetail.setQuantityPromised(deliverPlanDetail.getQuantityPromised());
                            }
                            // ????????????
                            if(planDetail.getQuantityPromised().compareTo(BigDecimal.ZERO) != 0 && planDetail.getRequirementQuantity().compareTo(BigDecimal.ZERO) != 0
                                    && planDetail.getRequirementQuantity().compareTo(planDetail.getQuantityPromised()) == 0){
                                planDetail.setDeliverPlanStatus(DeliverPlanLineStatus.COMFIRM.name());
                            }
                            iDeliverPlanDetailService.updateById(planDetail);
                        }
                    }else {
                        // ??????
                        deliverPlanDetail.setDeliverPlanId(deliverPlanId);
                        deliverPlanDetail.setDeliverPlanDetailId(IdGenrator.generate());
                        deliverPlanDetail.setDeliverPlanLock("2");
                        deliverPlanDetail.setDeliverPlanStatus(DeliverPlanLineStatus.UNCOMFIRMED.name());
                        iDeliverPlanDetailService.save(deliverPlanDetail);
                    }
                });
            }
        }
        this.baseMapper.updateSchTotalQuantity(deliverPlanId);
        return result;
    }

    /**
     * ?????????????????????
     * @param inputStream
     * @param errorList
     * @param errorFlag
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private Workbook getImportLineData(DeliverPlan deliverPlan,InputStream inputStream, List<String> errorList, AtomicBoolean errorFlag,Map<String,List<DeliverPlanDetail>> deliverPlanDetailTyppeMap) throws IOException, ParseException {
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

        // ?????????????????????
        boolean isVendor = AppUserUtil.userIsVendor();

        String ceeaIfBeyondDeliver = YesOrNo.NO.getValue();

        HashSet<String> hashSet = new HashSet<>();

        List<MaterialItem> materialItems = baseClient.listMaterialByParam(new MaterialItem().setMaterialId(deliverPlan.getMaterialId()));
        // ??????????????????
        StringBuffer errorHead = new StringBuffer();
        if(CollectionUtils.isNotEmpty(materialItems)){
            String struct = materialItems.get(0).getStruct();
            if (StringUtil.notEmpty(struct)) {
                PurchaseCategory purchaseCategory = baseClient.checkByIfDeliverPlan(struct);
                if(null != purchaseCategory){
                    ceeaIfBeyondDeliver = StringUtil.notEmpty(purchaseCategory.getCeeaIfBeyondDeliver()) ? purchaseCategory.getCeeaIfBeyondDeliver() : "N";
                }else {
                    errorHead.append("????????????????????????????????????????????????; ");
                }
            }else {
                errorHead.append("?????????????????????????????????; ");
            }
        }else {
            errorHead.append("????????????????????????; ");
        }

        int sum = 0;
        // ???????????????????????????,???2?????????,????????????????????????1
        for (int r = 1; r <= totalRows; r++) {
            log.info("???"+r+"?????????");
            if (errorHead.length() < 1) {
                // ????????????, 0-?????? 1-??????
                String type = "0";

                Row row = sheet.getRow(r);
                if (null == row) {
                    // ????????????,????????????????????????????????????
                    sheet.shiftRows(r + 1, totalRows, -1);
                    r--;
                    totalRows--;
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
                    totalRows--;
                    continue;

                }
// <------------------------------------????????????????????????????????????-------------------------------------------->
                // ????????????????????????
                StringBuffer errorMsg = new StringBuffer();
                // ????????????
                Cell cell0 = row.getCell(0);
                String deliverCode = ExcelUtil.getCellValue(cell0);
                if(StringUtil.notEmpty(deliverCode)){
                    deliverCode = deliverCode.trim();
                    if(!deliverPlan.getDeliverPlanNum().equals(deliverCode)){
                        errorFlag.set(true);
                        errorMsg.append("??????????????????????????????????????????; ");
                    }
                }else {
                    errorFlag.set(true);
                    errorMsg.append("???????????????????????????; ");
                }

                // ?????? : ??????-????????????  ??????-??????????????????
                Cell cell1 = row.getCell(1);
                String project = ExcelUtil.getCellValue(cell1);
                if (StringUtil.notEmpty(project)) {
                    project = project.trim();
                    if(!hashSet.add(project)){
                        errorFlag.set(true);
                        errorMsg.append("?????????????????????????????????????????????; ");
                    }
                    if (!isVendor) {
                        if ("??????".equals(project) || "??????".equals(project)) {
                            if ("??????".equals(project)) {
                                type = "1";
                            }
                        } else {
                            errorFlag.set(true);
                            errorMsg.append("???????????????: \"??????\"???\"??????\"; ");
                        }
                    }else {
                        if ("??????".equals(project)) {
                            type = "1";
                        } else {
                            errorFlag.set(true);
                            errorMsg.append("???????????????: \"??????\"; ");
                        }
                    }
                } else {
                    errorFlag.set(true);
                    errorMsg.append("??????????????????; ");
                }

                sum +=1;

                // ????????????????????????
                ArrayList<DeliverPlanDetail> planDetailArrayList = new ArrayList<>();
                for (int c = 2; c < totalCells; c++) {
                    Cell cell = row.getCell(c);
                    String cellValue = ExcelUtil.getCellValue(cell);
                    if (StringUtil.notEmpty(cellValue)) {
                        cellValue = cellValue.trim();
                    }else {
                        cellValue = "0";
                    }
                    if (StringUtil.isDigit(cellValue)) {
                        // ????????????
                        String dateStr = head.get(c);
                        dateStr = dateStr.trim();
                        Date date = DateUtil.parseDate(dateStr);
                        LocalDate localDate = DateUtil.dateToLocalDate(date);
                        DeliverPlanDetail deliverPlanDetail = new DeliverPlanDetail();;
                        deliverPlanDetail.setSchMonthlyDate(localDate);
                        if ("0".equals(type)) {
                            // ??????
                            deliverPlanDetail.setRequirementQuantity(new BigDecimal(cellValue));
                            planDetailArrayList.add(deliverPlanDetail);
                        } else {
                            // ??????
                            deliverPlanDetail.setQuantityPromised(new BigDecimal(cellValue));
                            planDetailArrayList.add(deliverPlanDetail);
                        }
                    } else {
                        errorFlag.set(true);
                        errorMsg.append(cellValue + "?????????; ");
                    }
                }
                if(CollectionUtils.isNotEmpty(planDetailArrayList)){
                    deliverPlanDetailTyppeMap.put(type,planDetailArrayList);
                }

                if(!errorFlag.get() && YesOrNo.NO.getValue().equals(ceeaIfBeyondDeliver)){
                    if(isVendor){
                        // ?????????
                        List<DeliverPlanDetail> deliverPlanDetails = deliverPlanDetailTyppeMap.get("1");
                        // ????????????????????????
                        List<DeliverPlanDetail> deliverPlanDetailList = iDeliverPlanDetailService.list(new QueryWrapper<>(new DeliverPlanDetail().setDeliverPlanId(deliverPlan.getDeliverPlanId())));
                        if(CollectionUtils.isNotEmpty(deliverPlanDetailList)){
                            HashMap<LocalDate, DeliverPlanDetail> deliverPlanDetailMap = new HashMap<>();
                            deliverPlanDetailList.forEach(deliverPlanDetail -> {
                                LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                deliverPlanDetailMap.put(schMonthlyDate,deliverPlanDetail);
                            });
                            if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
                                for(DeliverPlanDetail deliverPlanDetail : deliverPlanDetails){
                                    LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                    DeliverPlanDetail deliverPlanDetail1 = deliverPlanDetailMap.get(schMonthlyDate);
                                    if(null != deliverPlanDetail1){
                                        BigDecimal requirementQuantity = deliverPlanDetail1.getRequirementQuantity();
                                        if(deliverPlanDetail.getQuantityPromised().compareTo(requirementQuantity) > 0){
                                            errorFlag.set(true);
                                            errorMsg.append("????????????????????????????????????");
                                            break;
                                        }
                                    }else {
                                        errorFlag.set(true);
                                        errorMsg.append("????????????????????????????????????");
                                        break;
                                    }
                                }
                            }
                        }else {
                            errorFlag.set(true);
                            errorMsg.append("????????????????????????????????????????????????");
                        }
                    }else {
                        // ?????????
                        if("1".equals(type)){
                            // ??????
                            /**
                             * ?????????????????????????????????,????????????????????????,??????????????????????????????, ??????????????????
                             *
                             */
                            if(1 == sum){
                                // ?????????????????????, ???????????????????????????
                                if(totalRows == 1){
                                    // ????????????
                                    List<DeliverPlanDetail> deliverPlanDetails = deliverPlanDetailTyppeMap.get("1");
                                    // ????????????????????????
                                    List<DeliverPlanDetail> deliverPlanDetailList = iDeliverPlanDetailService.list(new QueryWrapper<>(new DeliverPlanDetail().setDeliverPlanId(deliverPlan.getDeliverPlanId())));
                                    if(CollectionUtils.isNotEmpty(deliverPlanDetailList)){
                                        HashMap<LocalDate, DeliverPlanDetail> deliverPlanDetailMap = new HashMap<>();
                                        deliverPlanDetailList.forEach(deliverPlanDetail -> {
                                            LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                            deliverPlanDetailMap.put(schMonthlyDate,deliverPlanDetail);
                                        });
                                        if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
                                            for(DeliverPlanDetail deliverPlanDetail : deliverPlanDetails){
                                                LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                                DeliverPlanDetail deliverPlanDetail1 = deliverPlanDetailMap.get(schMonthlyDate);
                                                if(null != deliverPlanDetail1){
                                                    BigDecimal requirementQuantity = deliverPlanDetail1.getRequirementQuantity();
                                                    if(deliverPlanDetail.getQuantityPromised().compareTo(requirementQuantity) > 0){
                                                        errorFlag.set(true);
                                                        errorMsg.append("????????????????????????????????????");
                                                        break;
                                                    }
                                                }else {
                                                    errorFlag.set(true);
                                                    errorMsg.append("????????????????????????????????????");
                                                    break;
                                                }
                                            }
                                        }
                                    }else {
                                        errorFlag.set(true);
                                        errorMsg.append("????????????????????????????????????????????????");
                                    }
                                }else if(totalRows == 2){
                                    // ???????????????
                                    // ??????
                                    errorFlag.set(true);
                                    errorMsg.append("?????????????????????,??????????????????????????????,??????????????????????????????; ");
                                }
                            }else if(sum == 2){
                                List<DeliverPlanDetail> deliverPlanDetails0 = deliverPlanDetailTyppeMap.get("0");
                                List<DeliverPlanDetail> deliverPlanDetails1 = deliverPlanDetailTyppeMap.get("1");
                                if(CollectionUtils.isNotEmpty(deliverPlanDetails0)){
                                    HashMap<LocalDate, DeliverPlanDetail> deliverPlanDetailMap = new HashMap<>();
                                    deliverPlanDetails0.forEach(deliverPlanDetail -> {
                                        LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                        deliverPlanDetailMap.put(schMonthlyDate,deliverPlanDetail);
                                    });
                                    if(CollectionUtils.isNotEmpty(deliverPlanDetails1)){
                                        for(DeliverPlanDetail deliverPlanDetail : deliverPlanDetails1){
                                            LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                                            DeliverPlanDetail deliverPlanDetail1 = deliverPlanDetailMap.get(schMonthlyDate);
                                            if(null != deliverPlanDetail1){
                                                BigDecimal requirementQuantity = deliverPlanDetail1.getRequirementQuantity();
                                                if(deliverPlanDetail.getQuantityPromised().compareTo(requirementQuantity) > 0){
                                                    errorFlag.set(true);
                                                    errorMsg.append("????????????????????????????????????");
                                                    break;
                                                }
                                            }else {
                                                errorFlag.set(true);
                                                errorMsg.append("????????????????????????????????????");
                                                break;
                                            }
                                        }
                                    }
                                }else {
                                    errorFlag.set(true);
                                    errorMsg.append("????????????????????????????????????????????????");
                                }
                            }
                        }
                    }
                }
                // ??????????????????
                errorList.add(errorMsg.toString());
            }else {
                // ??????????????????
                errorFlag.set(true);
                errorList.add(errorHead.toString());
            }
        }
        return workbook;
    }

    @Override
    public void export(DeliverPlanDTO deliverPlanDTO, HttpServletResponse response) throws Exception {
        if(StringUtil.isEmpty(deliverPlanDTO.getMonthlySchDate())){
            throw new BaseException("?????????????????????");
        }
        String monthlySchDate = deliverPlanDTO.getMonthlySchDate();
        Workbook workbook = crateWorkbookModel(monthlySchDate);
        List<DeliverPlan> deliverPlans = getDeliverPlans(deliverPlanDTO);
        if(CollectionUtils.isNotEmpty(deliverPlans)){
            deliverPlans.forEach(deliverPlan -> {
                // ??????
                HashMap<String, String> demandNumMap = new HashMap<>();
                // ??????
                HashMap<String, String> supplyNumMap = new HashMap<>();
                List<DeliverPlanDetail> deliverPlanDetails = iDeliverPlanDetailService.list(new QueryWrapper<>(new DeliverPlanDetail().setDeliverPlanId(deliverPlan.getDeliverPlanId())));
                if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
                    deliverPlanDetails.forEach(deliverPlanDetail -> {
                        // ??????
                        LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                        String schMonthly = DateUtil.localDateToStr(schMonthlyDate);
                        // ??????
                        BigDecimal quantityPromised = deliverPlanDetail.getQuantityPromised();
                        if(StringUtil.isEmpty(quantityPromised)){
                            supplyNumMap.put(schMonthly,"0");
                        }else if (quantityPromised.compareTo(BigDecimal.ZERO) == 0){
                            supplyNumMap.put(schMonthly,"0");
                        }else {
                            supplyNumMap.put(schMonthly,StringUtil.subZeroAndDot(String.valueOf(quantityPromised.doubleValue())));
                        }
                        // ??????
                        BigDecimal requirementQuantity = deliverPlanDetail.getRequirementQuantity();
                        if(StringUtil.isEmpty(requirementQuantity)){
                            demandNumMap.put(schMonthly,"0");
                        }else if (requirementQuantity.compareTo(BigDecimal.ZERO) == 0){
                            demandNumMap.put(schMonthly,"0");
                        }else {
                            demandNumMap.put(schMonthly,StringUtil.subZeroAndDot(String.valueOf(requirementQuantity.doubleValue())));
                        }
                    });
                }
                deliverPlan.setDemandNumMap(demandNumMap);
                deliverPlan.setSupplyNumMap(supplyNumMap);
            });

            // ?????????????????????
            Sheet sheet = workbook.getSheetAt(0);
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

            // ????????????
            int rowIndex = 1;
            for(int i=0; i < deliverPlans.size();i++){
                DeliverPlan deliverPlan = deliverPlans.get(i);
                // ??????????????????
                setContenx(deliverPlan.getDemandNumMap(),sheet, totalCells, head, rowIndex, deliverPlan,"??????");
                rowIndex += 1;
                // ??????????????????
                setContenx(deliverPlan.getSupplyNumMap(),sheet, totalCells, head, rowIndex, deliverPlan,"??????");
                rowIndex += 1;
            }
        }

        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????");
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    private void setContenx(Map<String, String> map,Sheet sheet, int totalCells, List<String> head, int rowIndex, DeliverPlan deliverPlan,String project) {
        Row row = sheet.createRow(rowIndex);
        // "*????????????", "*????????????", "*????????????", "*???????????????", "*????????????", "*??????"
        Cell cell0 = row.createCell(0);
        cell0.setCellValue(deliverPlan.getOrgName());
        Cell cell1 = row.createCell(1);
        cell1.setCellValue(deliverPlan.getOrganizationName());
        Cell cell2 = row.createCell(2);
        cell2.setCellValue(deliverPlan.getDeliveryAddress());
        Cell cell3 = row.createCell(3);
        cell3.setCellValue(deliverPlan.getVendorName());
        Cell cell4 = row.createCell(4);
        cell4.setCellValue(deliverPlan.getMaterialCode());
        Cell cell5 = row.createCell(5);
        cell5.setCellValue(project);
        for(int j = 6;j < totalCells;j++){
            String title = head.get(j);
            Cell cell = row.createCell(j);
            if(StringUtil.notEmpty(map.get(title))){
                cell.setCellValue(map.get(title));
            }else {
                cell.setCellValue("0");
            }
        }
    }

    @Override
    public void exportLine(Long deliverPlanId, HttpServletResponse response) throws Exception {
        Assert.notNull(deliverPlanId,"????????????:"+deliverPlanId);
        // ????????????
        Workbook workbook = crateWorkbookLineModel(deliverPlanId);
        DeliverPlan deliverPlan = this.getById(deliverPlanId);
        // ??????
        HashMap<String, String> demandNumMap = new HashMap<>();
        // ??????
        HashMap<String, String> supplyNumMap = new HashMap<>();
        List<DeliverPlanDetail> deliverPlanDetails = iDeliverPlanDetailService.list(new QueryWrapper<>(new DeliverPlanDetail().setDeliverPlanId(deliverPlanId)));
        if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
            deliverPlanDetails.forEach(deliverPlanDetail -> {
                // ??????
                LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                String schMonthly = DateUtil.localDateToStr(schMonthlyDate);
                // ??????
                BigDecimal quantityPromised = deliverPlanDetail.getQuantityPromised();
                if(StringUtil.isEmpty(quantityPromised)){
                    supplyNumMap.put(schMonthly,"0");
                }else if (quantityPromised.compareTo(BigDecimal.ZERO) == 0){
                    supplyNumMap.put(schMonthly,"0");
                }else {
                    supplyNumMap.put(schMonthly,StringUtil.subZeroAndDot(String.valueOf(quantityPromised.doubleValue())));
                }
                // ??????
                BigDecimal requirementQuantity = deliverPlanDetail.getRequirementQuantity();
                if(StringUtil.isEmpty(requirementQuantity)){
                    demandNumMap.put(schMonthly,"0");
                }else if (requirementQuantity.compareTo(BigDecimal.ZERO) == 0){
                    demandNumMap.put(schMonthly,"0");
                }else {
                    demandNumMap.put(schMonthly,StringUtil.subZeroAndDot(String.valueOf(requirementQuantity.doubleValue())));
                }
            });
        }

        // ?????????????????????
        Sheet sheet = workbook.getSheetAt(0);
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

        // ????????????
        int rowIndex = 1;
        for(int i=0; i < 1;i++){
            // ??????????????????
            setContentLine(deliverPlan, demandNumMap, sheet, totalCells, head, rowIndex,"??????");
            rowIndex += 1;

            // ??????????????????
            setContentLine(deliverPlan, supplyNumMap, sheet, totalCells, head, rowIndex,"??????");
            rowIndex += 1;
        }

        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????????????????");
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();

    }

    @Override
    public void exportLineCopy(Long deliverPlanId, HttpServletResponse response) throws Exception {
        Assert.notNull(deliverPlanId,"????????????:"+deliverPlanId);
        // ????????????
        Workbook workbook = crateWorkbookLineModel(deliverPlanId);
        DeliverPlan deliverPlan = this.getById(deliverPlanId);
        // ??????
        HashMap<String, String> supplyNumMap = new HashMap<>();
        List<DeliverPlanDetail> deliverPlanDetails = iDeliverPlanDetailService.list(new QueryWrapper<>(new DeliverPlanDetail().setDeliverPlanId(deliverPlanId)));
        if(CollectionUtils.isNotEmpty(deliverPlanDetails)){
            deliverPlanDetails.forEach(deliverPlanDetail -> {
                // ??????
                LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                String schMonthly = DateUtil.localDateToStr(schMonthlyDate);
                // ??????
                BigDecimal quantityPromised = deliverPlanDetail.getQuantityPromised();
                if(StringUtil.isEmpty(quantityPromised)){
                    supplyNumMap.put(schMonthly,"0");
                }else if (quantityPromised.compareTo(BigDecimal.ZERO) == 0){
                    supplyNumMap.put(schMonthly,"0");
                }else {
                    supplyNumMap.put(schMonthly,StringUtil.subZeroAndDot(String.valueOf(quantityPromised.doubleValue())));
                }
            });
        }

        // ?????????????????????
        Sheet sheet = workbook.getSheetAt(0);
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

        // ????????????
        int rowIndex = 1;
        for(int i=0; i < 1;i++){
            // ??????????????????
            setContentLine(deliverPlan, supplyNumMap, sheet, totalCells, head, rowIndex,"??????");
            rowIndex += 1;
        }

        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????????????????");
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();

    }


    private void setContentLine(DeliverPlan deliverPlan, HashMap<String, String> demandNumMap, Sheet sheet, int totalCells, List<String> head, int rowIndex,String project) {
        Row row = sheet.createRow(rowIndex);
        // *???????????????","*??????
        Cell cell0 = row.createCell(0);
        cell0.setCellValue(deliverPlan.getDeliverPlanNum());
        Cell cell1 = row.createCell(1);
        cell1.setCellValue(project);

        for(int j = 2;j < totalCells;j++){
            String title = head.get(j);
            Cell cell = row.createCell(j);
            if(StringUtil.notEmpty(demandNumMap.get(title))){
                cell.setCellValue(demandNumMap.get(title));
            }else {
                cell.setCellValue("0");
            }
        }
    }

@Override
    public List<DeliverPlanDetail> getDeliverPlanList(DeliverPlanDTO deliverPlanDTO) {
/*    QueryWrapper<DeliverPlanDTO> wrapper = new QueryWrapper<>();
        //??????
        wrapper.ne(StringUtils.isNotEmpty(deliverPlanDTO.getDeliverPlanStatus()), "a.DELIVER_PLAN_STATUS", DeliverPlanStatus.DRAFT.toString());
        //???????????????????????????
        //wrapper.in(CollectionUtils.isNotEmpty(deliverPlanDTO.getCategoryIds()), "a.CATEGORY_ID", deliverPlanDTO.getCategoryIds());
        //??????????????????????????????
        //wrapper.in("(a.CATEGORY_ID,b.SCH_MONTHLY_DATE)",deliverPlanDTO.getDeliverPlanVOS());
        //??????????????????????????????
        wrapper.eq("b.DELIVER_PLAN_STATUS",DeliverPlanLineStatus.COMFIRM.toString());
        //??????????????????????????????
        wrapper.ne("b.DELIVER_PLAN_LOCK","1");
        wrapper.orderByDesc("b.SCH_MONTHLY_DATE");*/
        return deliverPlanMapper.getDeliverPlanList(deliverPlanDTO);
    }


    /**
     *??????????????????????????????????????????
     * @param deliverPlanDTO
     */
    @Override
    @Transactional
    public DeliverPlanDTO getDeliverPlanMRPList(DeliverPlanDTO deliverPlanDTO) throws Exception{
        //?????????????????????????????????????????????????????????????????????????????????
        //????????????-->???????????????????????????erpID--->??????srm????????????
        String organizationCode = deliverPlanDTO.getOrganizationCode();
        Organization organization = getOrganization(organizationCode);

        Organization orgids = getOrgids(Long.valueOf(organization.getParentOrganizationIds()));

        //?????????ERPcode---->SRM?????????code
        String vendorCode = deliverPlanDTO.getVendorCode();
        CompanyInfo erpCodes = getErpCodes(vendorCode);
        //??????
        String deliveryAddress = deliverPlanDTO.getDeliveryAddress();
        //??????code
        String materialCode = deliverPlanDTO.getMaterialCode();
        MaterialItem materialItem = getMaterialItemMap(materialCode);
        //????????????
        Long categoryId = materialItem.getCategoryId();
        PurchaseCategory purchaseCategory = baseClient.MinByIfDeliverPlan(categoryId);
        Assert.notNull(purchaseCategory,"????????????????????????????????????????????????????????????");
        Assert.notNull(purchaseCategory.getCategoryId(),"????????????????????????????????????????????????????????????");

        //???????????????????????????
        List<DeliverPlanDetail> deliverPlanDetailList = deliverPlanDTO.getDeliverPlanDetailList();
        //???????????????---??? 2020-09 ???2020-10??????
        Map<String, List<DeliverPlanDetail>> DeliverPlanDetailMap = deliverPlanDetailList.stream().collect(Collectors.groupingBy(o -> ""+o.getSchMonthlyDate().getYear()+"-"+String.format("%02d",o.getSchMonthlyDate().getMonthValue())));
        log.info("=========================??????????????????===============================================");
        ArrayList<String> numberList = new ArrayList<>();

        //???????????????
        for (String dataString:DeliverPlanDetailMap.keySet()){
            //???????????????
            List<DeliverPlanDetail> deliverPlanDetails = DeliverPlanDetailMap.get(dataString);
            //?????????????????????????????????????????????
            DeliverPlanDTO deliverPlanDTOCopy = new DeliverPlanDTO();
            deliverPlanDTOCopy.setOrganizationId(organization.getOrganizationId())
                    .setOrganizationName(organization.getOrganizationName())
                    .setOrganizationCode(organization.getOrganizationCode())
                    .setVendorId(erpCodes.getCompanyId())
                    .setVendorCode(erpCodes.getCompanyCode())
                    .setVendorName(erpCodes.getCompanyName())
                    .setMaterialId(materialItem.getMaterialId())
                    .setMaterialCode(materialItem.getMaterialCode())
                    .setMaterialName(materialItem.getMaterialName())
                    .setCategoryId(purchaseCategory.getCategoryId())
                    .setCategoryCode(purchaseCategory.getCategoryCode())
                    .setCategoryName(purchaseCategory.getCategoryName())
                    .setDeliveryAddress(deliveryAddress)
                    .setOrgId(orgids.getOrganizationId())
                    .setOrgName(orgids.getOrganizationName())
                    .setOrgCode(orgids.getOrganizationCode())
                    .setMonthlySchDate(dataString);
            DeliverPlanDTO deliverPlanCopy = getDeliverPlanCopy(deliverPlanDTOCopy);
            //??????DTO?????????????????????
            numberList.add(packageDeliverPlanDTO(deliverPlanDTOCopy, deliverPlanCopy, deliverPlanDetails));
        }
        deliverPlanDTO.setDeliverPlanNum(numberList.get(0));
        return deliverPlanDTO;
    }
    /**
     *????????????(????????????)
     * @param deliverPlanDTO
     */
    @Override
    public DeliverPlanDTO getDeliverPlanMessageMRP(DeliverPlanDTO deliverPlanDTO)throws Exception{
        //?????????????????????
        String deliverPlanNum = deliverPlanDTO.getDeliverPlanNum();
        Assert.isTrue(StringUtils.isNotEmpty(deliverPlanNum),"srm????????????????????????");
        DeliverPlanDTO deliverPlanDTO1 = new DeliverPlanDTO();
        deliverPlanDTO1.setDeliverPlanNum(deliverPlanNum);
        List<DeliverPlan> deliverPlanListCopy = getDeliverPlanListCopy(deliverPlanDTO1);
        Assert.isTrue(CollectionUtils.isNotEmpty(deliverPlanListCopy),"srm????????????????????????????????????????????????"+deliverPlanNum);
        //??????srm??????????????????
        Long deliverPlanId = deliverPlanListCopy.get(0).getDeliverPlanId();
        DeliverPlanDTO deliverPlan = getDeliverPlan(deliverPlanId);
        //??????srm????????????????????????
        List<DeliverPlanDetail> srmDeliverPlanDetailList = deliverPlan.getDeliverPlanDetailList();
        Map<LocalDate, DeliverPlanDetail> srmDeliverPlanDetailMap = srmDeliverPlanDetailList.stream().collect(Collectors.toMap(DeliverPlanDetail::getSchMonthlyDate, Function.identity()));

        srmDeliverPlanDetailList=new ArrayList<>();

        //BRP???????????????????????????
        List<DeliverPlanDetail> brpDeliverPlanDetailList = deliverPlanDTO.getDeliverPlanDetailList();
        //???????????????????????????
        for (DeliverPlanDetail x:brpDeliverPlanDetailList){
            DeliverPlanDetail deliverPlanDetail = srmDeliverPlanDetailMap.get(x.getSchMonthlyDate());
            if (null!=deliverPlanDetail){
                srmDeliverPlanDetailList.add(deliverPlanDetail);
            }
        }
        deliverPlanJob.getDeliverPlanLock(srmDeliverPlanDetailList,deliverPlanId);
        return deliverPlanDTO;
    }

    public Organization getOrganization(String OrganizationCode)throws Exception{
        ArrayList<String> longs = new ArrayList<>();
        longs.add(OrganizationCode);
        List<Organization> organizations = baseClient.listByErpOrgId(longs);
        if (CollectionUtils.isEmpty(organizations)){
            Assert.isTrue(false,"???????????????"+OrganizationCode+"????????????");
        }
        return organizations.get(0);
    }

    public CompanyInfo  getErpCodes(String erpCode) {
        ArrayList<String> longs = new ArrayList<>();
        longs.add(erpCode);
        List<CompanyInfo> erpCodes = supplierClient.getErpCodes(longs);
        if (CollectionUtils.isEmpty(erpCodes)){
            Assert.isTrue(false,"????????????"+erpCode+"????????????");
        }
        return erpCodes.get(0);
    }
    public MaterialItem getMaterialItemMap(String itemCode){
        ArrayList<String> longs = new ArrayList<>();
        longs.add(itemCode);
        /* ????????????????????????????????????????????????????????? */
        Map<String, MaterialItem> materialItemMap = baseClient.listMaterialItemsByCodes(longs);
        if(materialItemMap.isEmpty()){
            Assert.isTrue(false,"?????????"+itemCode+"????????????");
        }
        for (String s:materialItemMap.keySet()){
            return  materialItemMap.get(s);
        }
        return null;
    }

    public DeliverPlanDTO getDeliverPlanCopy(DeliverPlanDTO deliverPlanDTO){
        List<DeliverPlan> deliverPlanListCopy = getDeliverPlanListCopy(deliverPlanDTO);
        if (CollectionUtils.isNotEmpty(deliverPlanListCopy)){
            return getDeliverPlan(deliverPlanListCopy.get(0).getDeliverPlanId());
        }else {
           return null;
        }


    }
    public List<DeliverPlan> getDeliverPlanListCopy(DeliverPlanDTO deliverPlanDTO) {
        QueryWrapper<DeliverPlan> wrapper = new QueryWrapper<>();
        //???????????????????????????
        wrapper.eq(null!=deliverPlanDTO.getOrganizationId(), "ORGANIZATION_ID", deliverPlanDTO.getOrganizationId());
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getDeliveryAddress()), "DELIVERY_ADDRESS", deliverPlanDTO.getDeliveryAddress());
        //?????????ID??????
        wrapper.eq(null!=deliverPlanDTO.getVendorId(),"VENDOR_ID", deliverPlanDTO.getVendorId());
        //?????????????????????
        wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getVendorCode()), "VENDOR_CODE", deliverPlanDTO.getVendorCode());
        //??????????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getMaterialCode()), "MATERIAL_CODE", deliverPlanDTO.getMaterialCode());
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(deliverPlanDTO.getMonthlySchDate()), "MONTHLY_SCH_DATE", deliverPlanDTO.getMonthlySchDate());
        //???????????????????????????
        wrapper.like(StringUtils.isNotEmpty(deliverPlanDTO.getDeliverPlanNum()), "DELIVER_PLAN_NUM", deliverPlanDTO.getDeliverPlanNum());
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return this.list(wrapper);
    }

    /**
     *
     * @param deliverPlanDTOCopy ???????????????
     * @param deliverPlanDTOs ?????????
     * @param deliverPlanDetails ?????????????????????
     * @return
     */
    @Transactional
    public String packageDeliverPlanDTO(DeliverPlanDTO deliverPlanDTOCopy,DeliverPlanDTO deliverPlanDTOs,List<DeliverPlanDetail> deliverPlanDetails){
        String number=null;
        //??????????????????????????????????????????id???code
        if (null==deliverPlanDTOs){
            number = baseClient.seqGenForAnon(SequenceCodeConstant.SEQ_CEEA_DELIVER_PLANNUM_CODE);
            deliverPlanDTOCopy.setDeliverPlanId(IdGenrator.generate());
            deliverPlanDTOCopy.setDeliverPlanNum(number)
                    .setDeliverPlanStatus("APPROVAL")
                    .setVersion(0L);
            packagedeliverPlanDetailDTO(deliverPlanDTOCopy,deliverPlanDetails);
            this.save(deliverPlanDTOCopy);
        }else {
            packagedeliverPlanDetailDTO(deliverPlanDTOs,deliverPlanDetails);
            number=deliverPlanDTOs.getDeliverPlan().getDeliverPlanNum();
        }

        return number;
    }
    //?????????
    @Transactional
    public void packagedeliverPlanDetailDTO(DeliverPlanDTO deliverPlanDTOs,List<DeliverPlanDetail> deliverPlanDetails){
        //???????????????
        Map<LocalDate, DeliverPlanDetail>  saveDeliverPlanDetailMap=new HashMap<>();
        //???????????????
        Map<LocalDate, DeliverPlanDetail>  updateDeliverPlanDetailMap=new HashMap<>();

        List<DeliverPlanDetail> deliverPlanDetailList = deliverPlanDTOs.getDeliverPlanDetailList();
        Map<LocalDate, DeliverPlanDetail> DeliverPlanDetailMap=new HashMap<>();
        if (CollectionUtils.isNotEmpty(deliverPlanDetailList)) {
            DeliverPlanDetailMap = deliverPlanDetailList.stream().collect(Collectors.toMap(DeliverPlanDetail::getSchMonthlyDate, Function.identity()));
        }
            for (DeliverPlanDetail deliverPlanDetail:deliverPlanDetails){
                LocalDate schMonthlyDate = deliverPlanDetail.getSchMonthlyDate();
                DeliverPlanDetail planDetail = DeliverPlanDetailMap.get(schMonthlyDate);
                if(null != planDetail){
                    // ??????
                    // ??????????????????
                    if (!"1".equals(planDetail.getDeliverPlanLock())) {
                        if(StringUtil.notEmpty(deliverPlanDetail.getRequirementQuantity())){
                            planDetail.setRequirementQuantity(deliverPlanDetail.getRequirementQuantity());
                        }
                        if(StringUtil.notEmpty(deliverPlanDetail.getQuantityPromised())){
                            planDetail.setQuantityPromised(deliverPlanDetail.getQuantityPromised());
                        }
                        // ????????????
                        if(planDetail.getQuantityPromised().compareTo(BigDecimal.ZERO) != 0 && planDetail.getRequirementQuantity().compareTo(BigDecimal.ZERO) != 0
                                && planDetail.getRequirementQuantity().compareTo(planDetail.getQuantityPromised()) == 0){
                            planDetail.setDeliverPlanStatus(DeliverPlanLineStatus.COMFIRM.name());
                        }
                        updateDeliverPlanDetailMap.put(schMonthlyDate,planDetail);
                    }
                }else {
                    // ??????
                    deliverPlanDetail.setDeliverPlanId(deliverPlanDTOs.getDeliverPlanId());
                    deliverPlanDetail.setDeliverPlanDetailId(IdGenrator.generate());
                    deliverPlanDetail.setDeliverPlanLock("2");
                    deliverPlanDetail.setDeliverPlanStatus(DeliverPlanLineStatus.UNCOMFIRMED.name());
                    saveDeliverPlanDetailMap.put(schMonthlyDate,deliverPlanDetail);
                }
            }
        iDeliverPlanDetailService.saveBatch(saveDeliverPlanDetailMap.values());
        iDeliverPlanDetailService.updateBatchById(updateDeliverPlanDetailMap.values());
    }
    public Organization getOrgids(Long OrgId)throws Exception{
        ArrayList<Long> longs = new ArrayList<>();
        longs.add(OrgId);
        List<Organization> organizations = baseClient.getOrganizationsByIds(longs);
        if (CollectionUtils.isEmpty(organizations)){
            Assert.isTrue(false,"???????????????"+OrgId+"????????????");
        }
        return organizations.get(0);
    }
    //???????????????mrp
    @Override
    public void getAffirmByMrp(Boolean falg){
        if (falg){
            //????????????
            CompletableFuture.runAsync(() -> {
                try {
                    apiClient.DeliverPlanJob();
                }catch (Exception e){
                    log.error("??????mrp????????????????????????"+e.getMessage());
                }
            }, ioThreadPool);
        }
    }
}
