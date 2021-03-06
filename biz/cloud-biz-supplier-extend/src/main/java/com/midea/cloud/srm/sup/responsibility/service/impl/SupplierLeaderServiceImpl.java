package com.midea.cloud.srm.sup.responsibility.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.enums.ImportStatus;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.utils.EasyExcelUtil;
import com.midea.cloud.common.utils.ExcelUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.base.formula.dto.MaterialAttrFormulaDTO;
import com.midea.cloud.srm.model.base.material.MaterialItemAttribute;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.dto.BidRequirementLineDto;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.responsibility.dto.SupplierLeaderDTO;
import com.midea.cloud.srm.model.supplier.responsibility.entity.SupplierLeader;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.srm.sup.info.service.ICompanyInfoService;
import com.midea.cloud.srm.sup.responsibility.mapper.SupplierLeaderMapper;
import com.midea.cloud.srm.sup.responsibility.service.ISupplierLeaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.ehcache.core.internal.util.CollectionUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.BatchUpdateException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <pre>
 * supplier leader????????? ???????????????
 * </pre>
 *
 * @author xiexh12@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-09-19 14:45:21
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class SupplierLeaderServiceImpl extends ServiceImpl<SupplierLeaderMapper, SupplierLeader> implements ISupplierLeaderService {

    @Resource
    private ICompanyInfoService iCompanyInfoService;

    @Resource
    private RbacClient rbacClient;

    @Resource
    private FileCenterClient fileCenterClient;

    //???????????????
    private static final List<String> fixedTitle;

    static {
        fixedTitle = new ArrayList<>();
        fixedTitle.addAll(Arrays.asList("?????????SRM??????", "???????????????", "???????????????", "???????????????"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSupplierLeader(List<SupplierLeader> supplierLeaderList) {
        Assert.isTrue(!supplierLeaderList.isEmpty(), "categoryResponsibilityList?????????");
        List<SupplierLeader> saveList = new ArrayList<>();
        List<SupplierLeader> updateList = new ArrayList<>();
        //??????????????????????????????????????????
        //????????????????????????????????????????????????????????????
        try {
            if (!saveList.isEmpty()) {
                this.saveBatch(supplierLeaderList);
            }
            if (!updateList.isEmpty()) {
                this.updateBatchById(updateList);
            }
        } catch (DuplicateKeyException e) {
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause instanceof BatchUpdateException) {
                String errMsg = ((BatchUpdateException) cause).getMessage();
                if (StringUtils.isNotBlank(errMsg) && errMsg.indexOf("CATEGORY_ORG_INDEX") != -1) {
                    throw new BaseException("??????????????????????????????????????????????????????????????????????????????");
                }
            }
        }
    }

    /**
     * ??????supplier leader????????????
     *
     * @param response
     * @throws Exception
     */
    @Override
    public void importSupplierLeaderModelDownload(HttpServletResponse response) throws Exception {
        //????????????
        Workbook workbook = crateWorkbookModel();
        //???????????????
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "?????????supplier leader????????????");
        //??????
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload) throws Exception {
        // ????????????
        EasyExcelUtil.checkParam(file, fileupload);
        // ????????????
        List<SupplierLeaderDTO> supplierLeaderDTOs = readData(file);
        // ?????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ????????????
        List<SupplierLeader> supplierLeaderList = getImportData(supplierLeaderDTOs, errorFlag);
        if (errorFlag.get()) {
            //??????
            fileupload.setFileSourceName("?????????supplier leader????????????");
            Fileupload fileupload1 = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    supplierLeaderDTOs, SupplierLeaderDTO.class, file.getName(), file.getOriginalFilename(), file.getContentType());
            return ImportStatus.importError(fileupload1.getFileuploadId(), fileupload1.getFileSourceName());
        } else {
            //???????????????supplier leader???
            saveOrUpdateSupplierLeaders(supplierLeaderList);
        }
        return ImportStatus.importSuccess();
    }

    /**
     * ??????????????????
     */
    public Workbook crateWorkbookModel() throws ParseException {
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

        //????????? ???????????????
        XSSFRow row0 = sheet.createRow(0);

        // ?????????????????????
        int cellIndex = 0;

        // ?????????????????????
        for (int i = 0; i < fixedTitle.size(); i++) {
            XSSFCell cell1 = row0.createCell(cellIndex);
            cell1.setCellValue(fixedTitle.get(i));
            cell1.setCellStyle(cellStyle);
            cellIndex++;
        }
        return workbook;
    }

    /**
     * ???????????????
     *
     * @param supplierLeaderList
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateSupplierLeaders(List<SupplierLeader> supplierLeaderList) {
        Map<String, SupplierLeader> supplierLeaderMap = this.list().stream().collect(Collectors.toMap(x -> x.getCompanyCode() + "-" + x.getResponsibilityCode(), part -> part));
        List<SupplierLeader> updateList = new ArrayList<>();
        List<SupplierLeader> saveList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(supplierLeaderList)) {
            for (SupplierLeader supplierLeader : supplierLeaderList) {
                if (Objects.nonNull(supplierLeader)
                        && StringUtils.isNotEmpty(supplierLeader.getCompanyCode())
                        && StringUtils.isNotEmpty(supplierLeader.getResponsibilityCode())) {
                    String companyCodeResponsibilityCode = supplierLeader.getCompanyCode() + "-" + supplierLeader.getResponsibilityCode();
                    if (supplierLeaderMap.containsKey(companyCodeResponsibilityCode)) {
                        Long id = supplierLeaderMap.get(companyCodeResponsibilityCode).getSupplierLeaderId();
                        supplierLeader.setSupplierLeaderId(id);
                        updateList.add(supplierLeader);
                    } else {
                        supplierLeader.setSupplierLeaderId(IdGenrator.generate());
                        saveList.add(supplierLeader);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(saveList)) {
                log.info("???excel???????????????supplier leader????????????????????????...????????????");
                this.saveBatch(saveList);
            }
            if (CollectionUtils.isNotEmpty(updateList)) {
                log.info("???excel???????????????supplier leader????????????????????????...????????????");
                this.updateBatchById(updateList);
            }
        }
    }

    /**
     * ?????????????????????, ????????????????????????
     */
    private List<SupplierLeader> getImportData(List<SupplierLeaderDTO> supplierLeaderDTOs, AtomicBoolean errorFlag) throws IOException, ParseException {
        List<SupplierLeader> supplierLeaderList = new ArrayList<>();
        /**
         * 1. ???????????????Srm????????????
         * 2. ???????????????Srm??????????????????????????????
         * 3. ????????????????????????????????????????????????????????????
         */
        Map<String, List<CompanyInfo>> companyCodeMap = new HashMap<>();
        Map<String, List<CompanyInfo>> companyNameMap = new HashMap<>();
        Map<String, List<User>> responsibilityCodeMap = new HashMap<>();
        Map<String, List<User>> responsibilityNameMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(supplierLeaderDTOs)) {
            //???????????????
            List<String> companyCodeList = new ArrayList<>();
            //???????????????
            List<String> companyNameList = new ArrayList<>();
            //???????????????
            List<String> responsibilityCodeList = new ArrayList<>();
            List<String> responsibilityNameList = new ArrayList<>();

            supplierLeaderDTOs.forEach(supplierLeaderDTO -> {
                String companyCode = supplierLeaderDTO.getCompanyCode();
                String companyName = supplierLeaderDTO.getCompanyName();
                String responsibilityCode = supplierLeaderDTO.getResponsibilityCode();
                String responsibilityName = supplierLeaderDTO.getResponsibilityName();
                Optional.ofNullable(companyCode).ifPresent(s -> companyCodeList.add(s.trim()));
                Optional.ofNullable(companyName).ifPresent(s -> companyNameList.add(s.trim()));
                Optional.ofNullable(responsibilityCode).ifPresent(s -> responsibilityCodeList.add(s.trim()));
                Optional.ofNullable(responsibilityName).ifPresent(s -> responsibilityNameList.add(s.trim()));
            });

            if (CollectionUtils.isNotEmpty(companyCodeList)) {
                List<CompanyInfo> companyCodes = iCompanyInfoService.list(Wrappers.lambdaQuery(CompanyInfo.class)
                        .in(CompanyInfo::getCompanyCode, companyCodeList.stream().distinct().collect(Collectors.toList())));
                if (CollectionUtils.isNotEmpty(companyCodes)) {
                    companyCodeMap = companyCodes.stream().collect(Collectors.groupingBy(CompanyInfo::getCompanyCode));
                }
            }

            if (CollectionUtils.isNotEmpty(companyNameList)) {
                List<CompanyInfo> companyNames = iCompanyInfoService.list(Wrappers.lambdaQuery(CompanyInfo.class)
                        .in(CompanyInfo::getCompanyName, companyNameList.stream().distinct().collect(Collectors.toList())));
                if (CollectionUtils.isNotEmpty(companyNames)) {
                    companyNameMap = companyNames.stream().collect(Collectors.groupingBy(CompanyInfo::getCompanyName));
                }

            }

            if (CollectionUtils.isNotEmpty(responsibilityCodeList)) {
                List<User> userCodes = rbacClient.listUsersByUsersParamCode(responsibilityCodeList);
                if (CollectionUtils.isNotEmpty(userCodes)) {
                    responsibilityCodeMap = userCodes.stream().collect(Collectors.groupingBy(User::getCeeaEmpNo));
                }
            }

            if (CollectionUtils.isNotEmpty(responsibilityNameList)) {
                List<User> userNickNames = rbacClient.listUsersByUsersParamNickName(responsibilityNameList);
                if ((CollectionUtils.isNotEmpty(userNickNames))) {
                    responsibilityNameMap = userNickNames.stream().collect(Collectors.groupingBy(User::getNickname));
                }
            }

        }

        //???????????? ????????????????????????
        if (CollectionUtils.isNotEmpty(supplierLeaderDTOs)) {

            for (SupplierLeaderDTO supplierLeaderDTO : supplierLeaderDTOs) {
                SupplierLeader supplierLeader = new SupplierLeader();
                StringBuffer errorMsg = new StringBuffer();

                //????????????????????? ????????????????????????
                String companyCode = supplierLeaderDTO.getCompanyCode();
                if (StringUtil.notEmpty(companyCode)) {
                    companyCode = companyCode.trim();
                    if (null != companyCodeMap.get(companyCode)) {
                        CompanyInfo companyInfo = companyCodeMap.get(companyCode).get(0);
                        supplierLeader.setCompanyId(companyInfo.getCompanyId());
                        supplierLeader.setCompanyCode(companyInfo.getCompanyCode());
                        supplierLeader.setCompanyName(companyInfo.getCompanyName());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("???????????????[").append(companyCode).append("]???srm??????????????????");
                    }
                } else {
                    errorFlag.set(true);
                    errorMsg.append("?????????SRM?????????????????????");
                }

                //????????????????????? ???????????????????????? ?????????????????????????????????
                String companyName = supplierLeaderDTO.getCompanyName();
                if (StringUtil.notEmpty(companyName)) {
                    companyName = companyName.trim();
                    if (Objects.nonNull(companyNameMap.get(companyName))) {
                        CompanyInfo companyInfo = companyNameMap.get(companyName).get(0);
                        if (StringUtil.notEmpty(supplierLeader.getCompanyCode())
                                && StringUtil.notEmpty(companyInfo.getCompanyName())
                                && !Objects.equals(supplierLeader.getCompanyName(), companyInfo.getCompanyName())) {
                            errorFlag.set(true);
                            errorMsg.append("excel????????????????????????????????????[").append(companyCode).append("]??????????????????[")
                                    .append(companyName).append("]?????????");
                        }
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("???????????????[").append(companyName).append("]???srm??????????????????");
                    }
                }

                //?????????????????????
                String responsibilityCode = supplierLeaderDTO.getResponsibilityCode();
                if (StringUtil.notEmpty(responsibilityCode)) {
                    responsibilityCode = responsibilityCode.trim();
                    if (Objects.nonNull(responsibilityCodeMap.get(responsibilityCode))) {
                        User user = responsibilityCodeMap.get(responsibilityCode).get(0);
                        supplierLeader.setResponsibilityId(user.getUserId());
                        supplierLeader.setResponsibilityCode(user.getCeeaEmpNo());
                        supplierLeader.setResponsibilityUsername(user.getUsername());
                        supplierLeader.setResponsibilityName(user.getNickname());
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("???????????????[").append(responsibilityCode).append("]???srm??????????????????");
                    }
                } else {
                    errorFlag.set(true);
                    errorMsg.append("??????????????????????????????");
                }

                //?????????????????????
                String responsibilityName = supplierLeaderDTO.getResponsibilityName();
                if (StringUtil.notEmpty(responsibilityName)) {
                    responsibilityName = responsibilityName.trim();
                    if (Objects.nonNull(responsibilityNameMap.get(responsibilityName))) {
                        User user = responsibilityNameMap.get(responsibilityName).get(0);
                        if (StringUtil.notEmpty(supplierLeader.getResponsibilityCode())
                                && StringUtil.notEmpty(user.getNickname())
                                && !Objects.equals(supplierLeader.getResponsibilityName(), user.getNickname())) {
                            errorFlag.set(true);
                            errorMsg.append("excel????????????????????????????????????[").append(responsibilityCode).append("]??????????????????[")
                                    .append(responsibilityName).append("]?????????");
                        }
                    } else {
                        errorFlag.set(true);
                        errorMsg.append("???????????????[").append(responsibilityName).append("]???srm??????????????????");
                    }
                }

                if (errorMsg.length() > 0) {
                    supplierLeaderDTO.setErrorMsg(errorMsg.toString());
                } else {
                    supplierLeaderDTO.setErrorMsg(null);
                }
                supplierLeaderList.add(supplierLeader);
            }
        }
        return supplierLeaderList;
    }

    /**
     * ??????excel???????????????
     *
     * @param file
     * @return
     */
    private List<SupplierLeaderDTO> readData(MultipartFile file) {
        List<SupplierLeaderDTO> supplierLeaderDTOS = null;
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<SupplierLeaderDTO> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream, listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(SupplierLeaderDTO.class).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            supplierLeaderDTOS = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return supplierLeaderDTOS;
    }

    /**
     * ???????????????
     *
     * @param companyId
     * @param responsibilityId
     */
    @Override
    public void saveOrUpdateSupplierLeader(Long companyId, Long responsibilityId) {
        List<SupplierLeader> supplierLeaders = this.list(Wrappers.lambdaQuery(SupplierLeader.class)
                .eq(SupplierLeader::getCompanyId, companyId)
                .eq(SupplierLeader::getResponsibilityId, responsibilityId));
        CompanyInfo companyInfo = iCompanyInfoService.getById(companyId);
        User user = rbacClient.getUserByParmForAnon(new User().setUserId(responsibilityId));
        if (CollectionUtils.isEmpty(supplierLeaders)) {
            SupplierLeader supplierLeader = new SupplierLeader().setSupplierLeaderId(IdGenrator.generate());
            supplierLeader.setCompanyId(companyId)
                    .setCompanyCode(companyInfo.getCompanyCode())
                    .setCompanyName(companyInfo.getCompanyName());
            supplierLeader.setResponsibilityId(responsibilityId)
                    .setResponsibilityCode(user.getCeeaEmpNo())
                    .setResponsibilityName(user.getNickname())
                    .setResponsibilityUsername(user.getUsername());
            this.save(supplierLeader);
        } else {
            SupplierLeader supplierLeader = supplierLeaders.get(0);
            supplierLeader.setCompanyId(companyId)
                    .setCompanyCode(companyInfo.getCompanyCode())
                    .setCompanyName(companyInfo.getCompanyName());
            supplierLeader.setResponsibilityId(responsibilityId)
                    .setResponsibilityCode(user.getCeeaEmpNo())
                    .setResponsibilityName(user.getNickname())
                    .setResponsibilityUsername(user.getUsername());
            this.updateById(supplierLeader);
        }
    }

    /**
     * ??????????????????
     *
     * @param supplierLeader
     * @return
     */
    @Override
    public List<SupplierLeader> listPageByParam(SupplierLeader supplierLeader) {
        List<SupplierLeader> supplierLeaders = this.list(Wrappers.lambdaQuery(SupplierLeader.class)
                .like(StringUtils.isNotEmpty(supplierLeader.getCompanyCode()), SupplierLeader::getCompanyCode, supplierLeader.getCompanyCode())
                .like(StringUtils.isNotEmpty(supplierLeader.getCompanyName()), SupplierLeader::getCompanyName, supplierLeader.getCompanyName())
                .eq(null != supplierLeader.getResponsibilityId(), SupplierLeader::getResponsibilityId, supplierLeader.getResponsibilityId())
                .orderByDesc(SupplierLeader::getLastUpdateDate));
        return supplierLeaders;
    }
}
