package com.midea.cloud.srm.perf.vendorasses.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.constants.VendorAssesFormConstant;
import com.midea.cloud.common.enums.VendorAssesFormStatus;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.flow.CbpmFormTemplateIdEnum;
import com.midea.cloud.common.enums.perf.indicators.EvaluationResults;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.flow.WorkFlowFeign;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationRelation;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationUser;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.common.ExportExcelParam;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.flow.process.dto.CbpmRquestParamDTO;
import com.midea.cloud.srm.model.perf.vendorasses.VendorAssesForm;
import com.midea.cloud.srm.model.perf.vendorasses.dto.VendorAssesFormDto;
import com.midea.cloud.srm.model.perf.vendorasses.dto.VendorAssesFormOV;
import com.midea.cloud.srm.model.rbac.permission.entity.Permission;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.model.supplier.info.dto.AssesFormDto;
import com.midea.cloud.srm.model.supplier.info.dto.CompanyRequestDTO;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.FinanceInfo;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import com.midea.cloud.srm.model.workflow.service.IFlowBusinessCallbackService;
import com.midea.cloud.srm.perf.vendorasses.mapper.VendorAssesFormMapper;
import com.midea.cloud.srm.perf.vendorasses.service.IVendorAssesFormService;
import com.midea.cloud.srm.perf.vendorasses.utils.ExportUtils;
import com.midea.cloud.srm.perf.vendorasses.workflow.VendorAssesFlow;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * <pre>
 *  ????????????????????? ???????????????
 * </pre>
 *
 * @author wangpr@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-05-27 09:55:47
 *  ????????????:
 * </pre>
 */

@Slf4j
@Service
public class VendorAssesFormServiceImpl extends ServiceImpl<VendorAssesFormMapper, VendorAssesForm> implements IVendorAssesFormService, IFlowBusinessCallbackService {
    @Resource
    private BaseClient baseClient;
    @Resource
    private SupplierClient supplierClient;
    @Resource
    private FileCenterClient fileCenterClient;
    @Resource
    private RbacClient rbacClient;
    @Resource
    private WorkFlowFeign workFlowFeign;
    @Resource
    private VendorAssesFormMapper vendorAssesFormMapper;

    @Autowired
    private VendorAssesFlow vendorAssesFlow;

    @Override
    public PageInfo<VendorAssesFormOV> listPage(VendorAssesForm vendorAssesForm) {
        // ????????????
        PageUtil.startPage(vendorAssesForm.getPageNum(), vendorAssesForm.getPageSize());
        List<VendorAssesFormOV> vendorAssesForms = getVendorAssesForms(vendorAssesForm);
        // ??????????????????
        return new PageInfo<VendorAssesFormOV>(vendorAssesForms);
    }

    private List<VendorAssesFormOV> getVendorAssesForms(VendorAssesForm vendorAssesForm) {
        // ??????????????????
        QueryWrapper<VendorAssesForm> wrappers = new QueryWrapper<>();
        wrappers.like(StringUtils.isNotEmpty(vendorAssesForm.getAssessmentNo()), "ASSESSMENT_NO", vendorAssesForm.getAssessmentNo());
        wrappers.eq(StringUtils.isNotEmpty(vendorAssesForm.getCategoryName()), "CATEGORY_NAME", vendorAssesForm.getCategoryName());
        wrappers.eq(StringUtils.isNotEmpty(vendorAssesForm.getStatus()), "STATUS", vendorAssesForm.getStatus());
        wrappers.eq(StringUtils.isNotEmpty(vendorAssesForm.getIndicatorDimension()), "INDICATOR_DIMENSION", vendorAssesForm.getIndicatorDimension());
        wrappers.eq(!StringUtil.isEmpty(vendorAssesForm.getOrganizationId()), "ORGANIZATION_ID", vendorAssesForm.getOrganizationId());
        wrappers.ge(!StringUtil.isEmpty(vendorAssesForm.getAssessmentDateStart()), "ASSESSMENT_DATE", vendorAssesForm.getAssessmentDateStart());
        wrappers.le(!StringUtil.isEmpty(vendorAssesForm.getAssessmentDateEnd()), "ASSESSMENT_DATE", vendorAssesForm.getAssessmentDateEnd());
        wrappers.gt("ACTUAL_ASSESSMENT_AMOUNT_Y",0);
        wrappers.gt("ACTUAL_ASSESSMENT_AMOUNT_N",0);
        //????????????
        wrappers.eq(StringUtils.isNotEmpty(vendorAssesForm.getCeeaAssociatedStates()),"CEEA_ASSOCIATED_STATES",vendorAssesForm.getCeeaAssociatedStates());
        if(StringUtils.isEmpty(vendorAssesForm.getCeeaAssociatedStates())){
            if (AppUserUtil.userIsVendor()) {
                // ????????????????????????
                //wrappers.eq("CEEA_VENDOR_ID",AppUserUtil.getLoginAppUser().getCompanyId());
                wrappers.eq("VENDOR_CODE",AppUserUtil.getLoginAppUser().getCompanyCode());
//                wrappers.eq("STATUS",VendorAssesFormStatus.IN_FEEDBACK.getKey());
/*            wrappers.like("VENDOR_CODE", AppUserUtil.getVendorCode());
            wrappers.and(wrapper -> wrapper.in("status", Arrays.asList(VendorAssesFormStatus.IN_FEEDBACK.getKey(),
                    VendorAssesFormStatus.OBSOLETE.getKey(), VendorAssesFormStatus.ASSESSED.getKey())));*/
            } else {
                wrappers.like(StringUtils.isNotEmpty(vendorAssesForm.getVendorName()), "VENDOR_NAME", vendorAssesForm.getVendorName());
            }
        }

        wrappers.eq(StringUtils.isNotEmpty(vendorAssesForm.getVendorCode()),"VENDOR_CODE",vendorAssesForm.getVendorCode());
//        wrappers.orderByDesc("VENDOR_ASSES_ID");
        wrappers.orderByDesc("CREATION_DATE");
        return vendorAssesFormMapper.ListCopy(wrappers);
    }

    @Override
    public Map<String, Object> add(VendorAssesForm vendorAssesForm) {
        Long id = IdGenrator.generate();
        vendorAssesForm.setVendorAssesId(id);
        // ?????????????????????
        vendorAssesForm.setStatus(VendorAssesFormStatus.DRAFT.getKey());
        // ????????????
        vendorAssesForm.setAssessmentNo(baseClient.seqGen(SequenceCodeConstant.SEQ_SCC_PERF_VENDOR_ASSES_FORM));
        this.save(vendorAssesForm);
        HashMap<String, Object> result = new HashMap<>();
        result.put("vendorAssesId", id);
        return result;
    }

    @Override
    public void delete(Long vendorAssesId) {
        Assert.notNull(vendorAssesId, "??????????????????:vendorAssesId");
        this.removeById(vendorAssesId);
    }

    @Transactional
    @Override
    public void notifySupplier(VendorAssesForm vendorAssesForm) {
        // ????????????????????????
        vendorAssesForm.setStatus(VendorAssesFormStatus.IN_FEEDBACK.getKey());
        vendorAssesForm.setCeeaVendorId(StringUtil.isEmpty(vendorAssesForm.getCeeaVendorId())?null:vendorAssesForm.getCeeaVendorId());
        if (null != vendorAssesForm.getVendorAssesId() && vendorAssesForm.getVendorAssesId() > 0) {
            // ??????????????????
            // ???????????????????????????
            vendorAssesForm.setMFeedbackTime(null);
            vendorAssesForm.setMIsFeedback(VendorAssesFormConstant.M_IS_FEEDBACK_N);
            vendorAssesForm.setMFeedbackExplanation(null);
            vendorAssesForm.setVIsFeedback(VendorAssesFormConstant.V_IS_FEEDBACK_N);

            this.updateById(vendorAssesForm);
        } else {
            // ???????????????
            Long id = IdGenrator.generate();
            vendorAssesForm.setVendorAssesId(id);
            this.save(vendorAssesForm);
        }
    }
    @Transactional
    @Override
    public void notifySupplier(List<VendorAssesForm> vendorAssesForms) {
        if (CollectionUtils.isNotEmpty(vendorAssesForms)) {
            vendorAssesForms.forEach((vendorAssesForm) -> {
                Assert.isTrue(VendorAssesFormStatus.REVIEWED.getKey().equals(vendorAssesForm.getStatus()),"????????????????????????????????????????????????");
                if (isNotifySupplier(vendorAssesForm.getVendorAssesId())) {
                    this.notifySupplier(vendorAssesForm);
                }
            });
        }
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param vendorAssesId
     * @return
     */
    public boolean isNotifySupplier(Long vendorAssesId) {
        VendorAssesForm vendorAssesForm = this.getById(vendorAssesId);
        String status = vendorAssesForm.getStatus();
//        return null == status || VendorAssesFormStatus.REVIEWED.getKey().equals(status) || VendorAssesFormStatus.WITHDRAWN.getKey().equals(status);
        return null == status || VendorAssesFormStatus.REVIEWED.getKey().equals(status);
    }

    /**
     * ????????????
     * @param vendorAssesId
     */
    @GetMapping("/abandon")
    public void abandon(Long vendorAssesId){
        VendorAssesForm vendorAssesForm = this.getById(vendorAssesId);
        Assert.notNull(vendorAssesForm,"?????????????????????????????????");
        String status = vendorAssesForm.getStatus();
        Assert.isTrue(VendorAssesFormStatus.REJECTED.getValue().equals(status)|| VendorAssesFormStatus.WITHDRAW.getValue().equals(status),"?????????????????????????????????");
        vendorAssesForm.setStatus(VendorAssesFormStatus.OBSOLETE.getValue());
        this.updateById(vendorAssesForm);
        SrmFlowBusWorkflow srmworkflowForm = baseClient.getSrmFlowBusWorkflow(vendorAssesId);
        if (srmworkflowForm!=null) {
            try {
                vendorAssesForm.setProcessType("N");
                vendorAssesFlow.submitVendorAssesConfFlow(vendorAssesForm);
            } catch (Exception e) {
                Assert.isTrue(false, "??????????????????????????????");
            }
        }
    }



    /**
     * ?????????????????????
     * @param vendorAssesForm
     */
    @Override
    @Transactional
    public void vendorFeedback(VendorAssesForm vendorAssesForm) {
        // ??????????????????
        Assert.notNull(vendorAssesForm.getVendorAssesId(), "??????????????????:vendorAssesId");
        // ??????????????????
        UpdateWrapper<VendorAssesForm> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("V_FEEDBACK_TIME", StringUtil.getValue(vendorAssesForm.getVFeedbackTime(), new Date()));
        updateWrapper.set("V_FEEDBACK_FILE_UPLOAD_ID", vendorAssesForm.getVFeedbackFileUploadId());
        updateWrapper.set("V_FEEDBACK_FILE_SOURCE_NAME", vendorAssesForm.getVFeedbackFileSourceName());
        updateWrapper.set("V_FEEDBACK_EXPLANATION", vendorAssesForm.getVFeedbackExplanation());
        updateWrapper.set("V_IS_FEEDBACK", VendorAssesFormConstant.V_IS_FEEDBACK_Y);
        updateWrapper.set("STATUS", VendorAssesFormStatus.VENDOR_FEEDBACK.getKey());
        // ????????????
        updateWrapper.eq("VENDOR_ASSES_ID", vendorAssesForm.getVendorAssesId());
        this.update(updateWrapper);
    }

    /**
     * ???????????????
     * @param vendorAssesForm
     */
    @Override
    @Transactional
    public void buyersProcess(VendorAssesForm vendorAssesForm) {
        // ??????????????????
        Assert.notNull(vendorAssesForm.getVendorAssesId(), "??????????????????:vendorAssesId");
        Assert.isTrue(StringUtils.isNotEmpty(vendorAssesForm.getStatus()),"????????????????????????");
        // ??????????????????
        UpdateWrapper<VendorAssesForm> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("M_FEEDBACK_TIME", StringUtil.getValue(vendorAssesForm.getMFeedbackTime(), new Date()));
        updateWrapper.set("M_FEEDBACK_EXPLANATION", vendorAssesForm.getMFeedbackExplanation());
        updateWrapper.set("M_IS_FEEDBACK", VendorAssesFormConstant.M_IS_FEEDBACK_Y);
        updateWrapper.set("STATUS", vendorAssesForm.getStatus());
        updateWrapper.eq("VENDOR_ASSES_ID", vendorAssesForm.getVendorAssesId());
        this.update(updateWrapper);
    }

    @Override
    public List<List<Object>> queryExportData(ExportExcelParam<VendorAssesForm> vendorAssesFormDto) {
        // ????????????
        VendorAssesForm queryParam = vendorAssesFormDto.getQueryParam();
        boolean flag = StringUtil.notEmpty(queryParam.getPageSize()) && StringUtil.notEmpty(queryParam.getPageNum());
        if (flag) {
            // ????????????
            PageUtil.startPage(queryParam.getPageNum(), queryParam.getPageSize());
        }
        List<VendorAssesFormOV> vendorAssesForms = this.getVendorAssesForms(queryParam);
        // ???Map
        List<Map<String, Object>> mapList = BeanMapUtils.objectsToMaps(vendorAssesForms);
        ArrayList<String> titleList = vendorAssesFormDto.getTitleList();
        List<List<Object>> results = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(mapList)) {
            mapList.forEach((map) -> {
                ArrayList<Object> list = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(titleList)) {
                    titleList.forEach((title) -> {
                        // ???????????????
                        if ("indicatorDimension".equals(title) || "status".equals(title)) {
                            Object key = map.get(title);
                            if (null != key) {
                                list.add(ExportUtils.keyValue.get(key.toString()));
                            }else {
                                list.add("");
                            }
                        } else if ("assessmentDate".equals(title)) {
                            Object date = map.get(title);
                            if(null != date){
                                LocalDate assessmentDate = (LocalDate)date;
                                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                                list.add(assessmentDate.format(dateTimeFormatter));
                            }else {
                                list.add("");
                            }
                        } else {
                            Object value = map.get(title);
                            if (null != value) {
                                list.add(value);
                            }else {
                                list.add("");
                            }
                        }
                    });
                }
                results.add(list);
            });
        }
        return results;
    }

    @Override
    public List<String> getMultilingualHeader(ExportExcelParam<VendorAssesForm> vendorAssesFormDto) {
        ArrayList<String> titleList = vendorAssesFormDto.getTitleList();
        LinkedHashMap<String, String> vendorAssesFormTitles = ExportUtils.getVendorAssesFormTitles();
        ArrayList<String> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(titleList)) {
            titleList.forEach((title) -> {
                result.add(vendorAssesFormTitles.get(title));
            });
        }
        return result;
    }

    @Override
    public void exportStart(ExportExcelParam<VendorAssesForm> vendorAssesFormDto, HttpServletResponse response) throws IOException {
        // ?????????????????????
        List<List<Object>> dataList = queryExportData(vendorAssesFormDto);
        // ??????
        List<String> head = getMultilingualHeader(vendorAssesFormDto);
        // ?????????
        String fileName = vendorAssesFormDto.getFileName();
        // ????????????
        EasyExcelUtil.exportStart(response, dataList, head, fileName);
    }

    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file,Fileupload fileupload) throws Exception {
        // ????????????
        EasyExcelUtil.checkParam(file, fileupload);

        HashMap<String,Object> result = new HashMap<>();
        InputStream inputStream = file.getInputStream();
        ArrayList<VendorAssesForm> vendorAssesForms = new ArrayList<>();
        ArrayList<VendorAssesFormDto> vendorAssesFormDtos = new ArrayList<>();

        // ??????????????????
        List<Object> objects = EasyExcelUtil.readExcelWithModel(inputStream, VendorAssesFormDto.class);
        if(CollectionUtils.isNotEmpty(objects)){
            objects.forEach((object -> {
                if(null != object){
                    VendorAssesFormDto vendorAssesFormDto = (VendorAssesFormDto)object;
                    VendorAssesForm vendorAssesForm = new VendorAssesForm();
                    StringBuffer errorMsg = new StringBuffer();
                    // ????????????
                    checkParam(vendorAssesFormDto, vendorAssesForm, errorMsg);
                    vendorAssesForms.add(vendorAssesForm);
                    vendorAssesFormDtos.add(vendorAssesFormDto);
                }
            }));
        }

        // ???????????????????????????
        boolean flag = true;
        if(CollectionUtils.isNotEmpty(vendorAssesFormDtos)){
            for(VendorAssesFormDto vendorAssesFormDto : vendorAssesFormDtos){
                String errorMsg = vendorAssesFormDto.getErrorMsg();
                if(errorMsg.length() > 1){
                    flag = false;
                    break;
                }
            }
        }
        if(flag){
            // ??????????????????????????????
            if(CollectionUtils.isNotEmpty(vendorAssesForms)){
                vendorAssesForms.forEach(vendorAssesForm -> add(vendorAssesForm));
            }
            result.put("status",YesOrNo.YES.getValue());
            result.put("message","success");
            return result;
        }else {
            Fileupload fileupload1 = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    vendorAssesFormDtos, VendorAssesFormDto.class, file.getName(), file.getOriginalFilename(), file.getContentType());
            result.put("status",YesOrNo.NO.getValue());
            result.put("message","error");
            result.put("fileuploadId",fileupload1.getFileuploadId());
            result.put("fileName",fileupload1.getFileSourceName());
            return result;
        }
    }

    public void checkParam(VendorAssesFormDto vendorAssesFormDto, VendorAssesForm vendorAssesForm, StringBuffer errorMsg) {
        // ??????????????????
        vendorAssesFormDto.setErrorMsg(null);

        // ??????????????????
        String date = vendorAssesFormDto.getAssessmentDate();
        if (StringUtil.notEmpty(date)) {
            try {
                Date assessmentDate = DateUtil.parseDate(date);
                LocalDate localDate = DateUtil.dateToLocalDate(assessmentDate);
                vendorAssesForm.setAssessmentDate(localDate);
            } catch (Exception e) {
                errorMsg.append("??????????????????????????????; ") ;
            }
        }else {
            errorMsg.append("????????????????????????; ") ;
        }

        // ??????????????????
        String indicatorName = vendorAssesFormDto.getIndicatorName();
        if (StringUtil.notEmpty(indicatorName)) {
            List<VendorAssesForm> vendorAssesFormList = this.baseMapper.queryIndicator(indicatorName);
            if (CollectionUtils.isNotEmpty(vendorAssesFormList)) {
                // ????????????
                String indicatorLineDes = vendorAssesFormDto.getIndicatorLineDes();
                if (StringUtil.notEmpty(indicatorLineDes)) {
                    boolean flag = false;
                    for(VendorAssesForm temp : vendorAssesFormList){
                        if(indicatorLineDes.equals(temp.getIndicatorLineDes())){
                            flag = true;
                            vendorAssesForm.setIndicatorHeadId(temp.getIndicatorHeadId());
                            vendorAssesForm.setIndicatorLineDes(temp.getIndicatorLineDes());
                            vendorAssesForm.setIndicatorDimension(temp.getIndicatorDimension());
                            vendorAssesForm.setIndicatorName(temp.getIndicatorName());
                            vendorAssesForm.setAssessmentPenalty(temp.getAssessmentPenalty());
                            break;
                        }
                    }
                    if(!flag){
                        errorMsg.append("?????????????????????????????????; ");
                    }
                }else {
                    errorMsg.append("????????????????????????; ");
                }
            }else {
                errorMsg.append("?????????????????????; ");
            }
        }else {
            errorMsg.append("????????????????????????; ");
        }
        //??????
        String taxRate = null;

        // ???????????????
        String orgName = vendorAssesFormDto.getOrganizationName();
        if(StringUtil.notEmpty(orgName)){
            if(StringUtil.checkStringNoSlash(orgName)){
                errorMsg.append("?????????????????????????????????:/;");
            }else if (orgName.contains("/")){
                StringBuffer error = new StringBuffer();
                // ??????
                List<String> orgNames = Arrays.asList(orgName.split("/"));
                // ????????????????????????
                ArrayList<Organization> organizations = new ArrayList<>();
                orgNames.forEach(orgNameTemp->{
                    Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgNameTemp.trim()));
                    if(null != organization && organization.getOrganizationId() > 1){
                        organizations.add(organization);
                    }else {
                        error.append(orgNameTemp+"?????????;");
                    }
                });
                if(error.length()>1){
                    errorMsg.append(error);
                }else {
                    if (CollectionUtils.isNotEmpty(organizations)) {
                        int size = organizations.size();
                        if(size == 1){
                            Organization organization = organizations.get(0);
                            Long organizationId = organization.getOrganizationId();
                            // ?????????????????????
                            List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                            if(CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())){
                                errorMsg.append("????????????????????????,??????????????????;");
                            }else {
                                String md5 = EncryptUtil.getMD5("-1" + organizationId);
                                vendorAssesForm.setOrganizationId(organizationId);
                                vendorAssesForm.setOrganizationCode(organization.getOrganizationCode());
                                vendorAssesForm.setOrganizationName(organization.getOrganizationName());
                                vendorAssesForm.setFullPathId(md5);
                            }
                        }else {
                            Organization organization = organizations.get(0);
                            Long organizationId = organization.getOrganizationId();
                            String md5 = EncryptUtil.getMD5("-1" + organizationId);
                            for(int i= 1;i< size;i++){
                                Long organizationId1 = organizations.get(i).getOrganizationId();
                                md5 = EncryptUtil.getMD5(md5 + organizationId1);
                            }
                            List<OrganizationUser> organizationUsers = baseClient.queryByFullPathId(md5);
                            if (CollectionUtils.isNotEmpty(organizationUsers)) {
                                Organization organization1 = organizations.get(size - 1);
                                vendorAssesForm.setOrganizationId(organization1.getOrganizationId());
                                vendorAssesForm.setOrganizationCode(organization1.getOrganizationCode());
                                vendorAssesForm.setOrganizationName(organization1.getOrganizationName());
                                vendorAssesForm.setFullPathId(md5);
                            }else {
                                errorMsg.append("???????????????????????????;");
                            }
                        }
                    }
                }
            }else {
                // ????????????????????????
                Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(orgName.trim()));
                if(null != organization && organization.getOrganizationId() > 1){
                    Long organizationId = organization.getOrganizationId();
                    // ?????????????????????
                    List<OrganizationRelation> organizationRelations = baseClient.queryByOrganizationId(organizationId);
                    if(CollectionUtils.isEmpty(organizationRelations) || !"-1".equals(organizationRelations.get(0).getParentOrganizationId())){
                        errorMsg.append("????????????????????????,??????????????????;");
                    }else {
                        String md5 = EncryptUtil.getMD5("-1" + organizationId);
                        vendorAssesForm.setOrganizationId(organizationId);
                        vendorAssesForm.setOrganizationCode(organization.getOrganizationCode());
                        vendorAssesForm.setOrganizationName(organization.getOrganizationName());
                        vendorAssesForm.setFullPathId(md5);
                    }
                }else {
                    errorMsg.append("??????????????????;");
                }
            }
        }else {
            errorMsg.append("???????????????????????????;");
        }

        Long organizationId = vendorAssesForm.getOrganizationId();

        // ?????????????????????
        String vendorName = vendorAssesFormDto.getVendorName();
        if (StringUtil.notEmpty(vendorName) && StringUtil.notEmpty(organizationId)) {
            CompanyRequestDTO companyRequestDTO = new CompanyRequestDTO();
            companyRequestDTO.setCompanyName(vendorName.trim());
            companyRequestDTO.setOrgId(organizationId);
            CompanyInfo companyInfo = supplierClient.queryVendorByNameAndOrgId(companyRequestDTO);
            if(null != companyInfo && StringUtil.notEmpty(companyInfo.getCompanyName())){
                vendorAssesForm.setVendorCode(companyInfo.getCompanyCode());
                vendorAssesForm.setVendorName(companyInfo.getCompanyName());
                // ??????????????????
                FinanceInfo financeInfo = supplierClient.getFinanceInfoByCompanyIdAndOrgId(companyInfo.getCompanyId(), organizationId);
                if (null != financeInfo) {
                    // ?????????????????????
                    taxRate = financeInfo.getTaxRate();
                    String clearCurrency = financeInfo.getClearCurrency();
                    vendorAssesForm.setTaxCode(new BigDecimal(taxRate));
                    vendorAssesForm.setCurrencyCode(clearCurrency);
                }else {
                    errorMsg.append("????????????????????????????????????????????????; ");
                }
            }else {
                errorMsg.append("??????????????????????????????????????????; ");
            }
        }else {
            errorMsg.append("???????????????????????????; ");
        }

        // ??????????????????
        String actualAssessmentAmountY = vendorAssesFormDto.getActualAssessmentAmountY();
        if(StringUtil.notEmpty(actualAssessmentAmountY)){
            if(StringUtil.isDigit(actualAssessmentAmountY)){
                double actualAssessmentAmountY1 = StringUtil.toDouble(actualAssessmentAmountY);
                vendorAssesForm.setActualAssessmentAmountY(NumberUtil.doubleToBigDecimal(actualAssessmentAmountY1));
                if (StringUtil.notEmpty(taxRate)) {
                    double doubleValue = Double.parseDouble(taxRate);
                    // ??????????????????(?????????)
                    double num = NumberUtil.div(actualAssessmentAmountY1,(doubleValue/100)+1);
                    double actualAssessmentAmountN = NumberUtil.formatDoubleByScale(num, 4);
                    vendorAssesForm.setActualAssessmentAmountN(NumberUtil.doubleToBigDecimal(actualAssessmentAmountN));
                }
            }else {
                errorMsg.append("??????????????????(??????)?????????????????????; ");
            }
        }else {
            errorMsg.append("??????????????????(??????)????????????; ");
        }

        // ??????????????????
        String categoryName = vendorAssesFormDto.getCategoryName();
        if(StringUtil.notEmpty(categoryName)){
            PurchaseCategory category = baseClient.getPurchaseCategoryByParm(new PurchaseCategory().setCategoryName(categoryName));
            if(null != category && StringUtil.notEmpty(category.getCategoryCode())){
                vendorAssesForm.setCategoryId(category.getCategoryId());
                vendorAssesForm.setCategoryCode(category.getCategoryCode());
                vendorAssesForm.setCategoryName(category.getCategoryName());
            }else {
                errorMsg.append("?????????????????????; ");
            }
        }else {
            errorMsg.append("????????????????????????; ");
        }

        // ??????????????????
        String materialCode = vendorAssesFormDto.getMaterialCode();
        if(StringUtil.notEmpty(materialCode)){
            List<MaterialItem> materialItems = baseClient.listMaterialByParam(new MaterialItem().setMaterialCode(materialCode));
            if (CollectionUtils.isNotEmpty(materialItems) && StringUtil.notEmpty(materialItems.get(0).getMaterialName())){
                MaterialItem materialItem = materialItems.get(0);
                vendorAssesForm.setMaterialCode(materialItem.getMaterialCode());
                vendorAssesForm.setMaterialName(materialItem.getMaterialName());
            }else {
                errorMsg.append("??????????????????????????????; ");
            }
        }

        // ???????????????
        String respFullName = vendorAssesFormDto.getRespFullName();
        if (StringUtil.notEmpty(respFullName)) {
            //???????????????
            List<User> userList = rbacClient.checkByUsername(respFullName.trim());
            if(CollectionUtils.isNotEmpty(userList)){
                User user = userList.get(0);
                vendorAssesForm.setRespUserName(user.getUsername());
                vendorAssesForm.setRespFullName(user.getNickname());
            }else {
                errorMsg.append("????????????????????????; ");
            }
        }else {
            errorMsg.append("???????????????????????????; ");
        }

        // ????????????
        String explanation = vendorAssesFormDto.getExplanation();
        if(StringUtil.notEmpty(explanation)){
            vendorAssesForm.setExplanation(explanation);
        }else {
            errorMsg.append("????????????????????????; ");
        }

        // ??????????????????
        vendorAssesFormDto.setErrorMsg(errorMsg.toString());
    }

    @Override
    public void importModelDownload(HttpServletResponse response) throws IOException {
        String fileName = "???????????????????????????";
        ArrayList<VendorAssesFormDto> vendorAssesFormDtos = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        EasyExcelUtil.writeExcelWithModel(outputStream,fileName,vendorAssesFormDtos,VendorAssesFormDto.class);
    }

    @Override
    public List<VendorAssesForm> queryAmerceInfo(VendorAssesForm vendorAssesForm) {
        String assessmentNo = vendorAssesForm.getAssessmentNo();
        vendorAssesForm.setAssessmentNo(null);
        QueryWrapper<VendorAssesForm> queryWrapper = new QueryWrapper<>(vendorAssesForm);
        queryWrapper.like(StringUtil.notEmpty(assessmentNo),"ASSESSMENT_NO",assessmentNo);
        return this.list(queryWrapper);
    }

    @Override
    public Map<String,Object> submitWithFlow(VendorAssesForm vendorAssesForm) {
        Map<String,Object> map=new HashMap();
        if(vendorAssesForm.getVendorAssesId()==null){
            map = add(vendorAssesForm);
        }
        Boolean flowEnable=enableFlowWork(vendorAssesForm);
        if(flowEnable){
            map.put("businessId", vendorAssesForm.getVendorAssesId());
            map.put("fdId", vendorAssesForm.getCbpmInstaceId());
            map.put("subject", vendorAssesForm.getVendorName());
            if(StringUtil.isEmpty(vendorAssesForm.getCbpmInstaceId())){
                CbpmRquestParamDTO request = buildCbpmRquest(vendorAssesForm);
                map=workFlowFeign.initProcess(request);
            }
        }
        return map;
    }

    private Boolean enableFlowWork(VendorAssesForm form) {
        Long menuId = form.getMenuId();
        Permission menu = rbacClient.getMenu(menuId);
        Boolean flowEnable;
        try {
            flowEnable = workFlowFeign.getFlowEnable(menuId, menu.getFunctionId(), CbpmFormTemplateIdEnum.VENDOR_ASSES_FORM.getKey());
        } catch (FeignException e) {
            log.error("??????????????????,??????????????????????????????,?????? menuId???" + menuId + ",functionId" + menu.getFunctionId()
                    + ",templateCode" + CbpmFormTemplateIdEnum.VENDOR_ASSES_FORM.getKey() + "?????????", e);
            throw new BaseException("?????????????????????????????????????????????????????????");
        }
        return flowEnable;
    }

    private CbpmRquestParamDTO buildCbpmRquest(VendorAssesForm source) {
        CbpmRquestParamDTO cbpmRquestParamDTO = new CbpmRquestParamDTO();
        cbpmRquestParamDTO.setBusinessId(String.valueOf(source.getVendorAssesId()));
        cbpmRquestParamDTO.setTemplateCode(CbpmFormTemplateIdEnum.VENDOR_ASSES_FORM.getKey());
        cbpmRquestParamDTO.setSubject(source.getVendorName());
        cbpmRquestParamDTO.setFdId(source.getCbpmInstaceId());
        return cbpmRquestParamDTO;
    }

    @Override
    public List<AssesFormDto> getAssesFormDtoByVendorId(Long vendorId) {
        CompanyInfo companyInfo = supplierClient.getCompanyInfo(vendorId);
        List<AssesFormDto> assesFormDtos = new ArrayList<>();
        if(null != companyInfo && StringUtil.notEmpty(companyInfo.getCompanyCode())){
            String companyCode = companyInfo.getCompanyCode();
            List<VendorAssesForm> vendorAssesForms = this.list(new QueryWrapper<>(new VendorAssesForm().setVendorCode(companyCode)));
            if(CollectionUtils.isNotEmpty(vendorAssesForms)){
                vendorAssesForms.forEach(vendorAssesForm -> {
                    AssesFormDto assesFormDto = new AssesFormDto();
                    BeanCopyUtil.copyProperties(assesFormDto,vendorAssesForm);
                    assesFormDtos.add(assesFormDto);
                });
            }
        }
        return assesFormDtos;
    }

    /**
     * ??????????????????+????????????+????????????+????????????-->??????????????????
     *  ??????????????????????????????
     * @param assesForm
     * @return
     */
    @Override
    public BigDecimal getAssessmentPenalty(VendorAssesForm assesForm) {
        //????????????
        String indicatorDimension = assesForm.getIndicatorDimension();
        String ceeaIndicatorLineDes = assesForm.getCeeaIndicatorLineDes();
        switch (indicatorDimension){
            case "COST":
            case "QUALITY":
                if (EvaluationResults.BAD.getValue().equals(ceeaIndicatorLineDes)){
                    return new BigDecimal(11000);
                }else {
                    return new BigDecimal(12000);
                }
            default:
                if (EvaluationResults.BAD.getValue().equals(ceeaIndicatorLineDes)){
                    return new BigDecimal(18000);
                }else {
                    return new BigDecimal(20000);
                }

        }
    }

    @Override
    public PageInfo<VendorAssesFormOV> listPageForInvoice(VendorAssesForm vendorAssesForm) {
        // ????????????
        PageUtil.startPage(vendorAssesForm.getPageNum(), vendorAssesForm.getPageSize());
        // ??????????????????
        QueryWrapper<VendorAssesForm> wrappers = new QueryWrapper<>();
        wrappers.like(StringUtils.isNotEmpty(vendorAssesForm.getAssessmentNo()), "ASSESSMENT_NO", vendorAssesForm.getAssessmentNo());
        wrappers.eq(StringUtils.isNotEmpty(vendorAssesForm.getCategoryName()), "CATEGORY_NAME", vendorAssesForm.getCategoryName());
        wrappers.eq(StringUtils.isNotEmpty(vendorAssesForm.getIndicatorDimension()), "INDICATOR_DIMENSION", vendorAssesForm.getIndicatorDimension());
        wrappers.eq(!StringUtil.isEmpty(vendorAssesForm.getOrganizationId()), "ORGANIZATION_ID", vendorAssesForm.getOrganizationId());
        wrappers.ge(!StringUtil.isEmpty(vendorAssesForm.getAssessmentDateStart()), "ASSESSMENT_DATE", vendorAssesForm.getAssessmentDateStart());
        wrappers.le(!StringUtil.isEmpty(vendorAssesForm.getAssessmentDateEnd()), "ASSESSMENT_DATE", vendorAssesForm.getAssessmentDateEnd());
        wrappers.gt("ACTUAL_ASSESSMENT_AMOUNT_Y",0);
        wrappers.gt("ACTUAL_ASSESSMENT_AMOUNT_N",0);
        //????????????(?????????Y)
        wrappers.eq("CEEA_ASSOCIATED_STATES", YesOrNo.YES.getValue());
        if(StringUtils.isEmpty(vendorAssesForm.getCeeaAssociatedStates())){
            if (AppUserUtil.userIsVendor()) {
                // ????????????????????????
                //wrappers.eq("CEEA_VENDOR_ID",AppUserUtil.getLoginAppUser().getCompanyId());
                wrappers.eq("VENDOR_CODE",AppUserUtil.getLoginAppUser().getCompanyCode());
/*            wrappers.like("VENDOR_CODE", AppUserUtil.getVendorCode());
            wrappers.and(wrapper -> wrapper.in("status", Arrays.asList(VendorAssesFormStatus.IN_FEEDBACK.getKey(),
                    VendorAssesFormStatus.OBSOLETE.getKey(), VendorAssesFormStatus.ASSESSED.getKey())));*/
            } else {
                wrappers.like(StringUtils.isNotEmpty(vendorAssesForm.getVendorName()), "VENDOR_NAME", vendorAssesForm.getVendorName());
            }
        }
        wrappers.eq(StringUtils.isNotEmpty(vendorAssesForm.getVendorCode()),"VENDOR_CODE",vendorAssesForm.getVendorCode());
        wrappers.nested(q -> q.eq( "IF_QUOTE", YesOrNo.NO.getValue()).or().eq("IF_QUOTE", null));//???????????????(Y)???
        wrappers.eq( "STATUS", VendorAssesFormStatus.ASSESSED.getKey());
        wrappers.orderByDesc("LAST_UPDATE_DATE");
        List<VendorAssesFormOV> vendorAssesForms = vendorAssesFormMapper.ListCopy(wrappers);
        // ??????????????????
        return new PageInfo<VendorAssesFormOV>(vendorAssesForms);
    }

    /**
     * ????????????????????? ????????????????????????
     * @param vendorAssesForm
     */
    @Override
    public void vendorAffirm(VendorAssesForm vendorAssesForm) {

        Assert.notNull(vendorAssesForm.getVendorAssesId(), "??????????????????:vendorAssesId");
        // ??????????????????
        UpdateWrapper<VendorAssesForm> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("M_FEEDBACK_TIME", StringUtil.getValue(vendorAssesForm.getMFeedbackTime(), new Date()));
        updateWrapper.set("M_IS_FEEDBACK", VendorAssesFormConstant.M_IS_FEEDBACK_N);
        updateWrapper.set("V_IS_FEEDBACK", VendorAssesFormConstant.V_IS_FEEDBACK_Y);
        updateWrapper.set("STATUS", VendorAssesFormStatus.AFFIRM.getKey());
        updateWrapper.eq("VENDOR_ASSES_ID", vendorAssesForm.getVendorAssesId());
        this.update(updateWrapper);

    }



    //-------------------------------------------------------------------------?????????---------------------------------------------------------------------------------
    //?????????oa??????
    @Override
    public void submitFlow(Long businessId, String param) throws Exception {
        log.info("VendorAssesFormServiceImpl---OASubmitFlow", businessId, param);
        VendorAssesForm vendorAssesForm = new VendorAssesForm()
                .setVendorAssesId(businessId)
                .setStatus(VendorAssesFormStatus.UNDER_APPROVAL.getKey());
        this.updateById(vendorAssesForm);
    }

    @Override
    public void passFlow(Long businessId, String param) throws Exception {
        log.info("VendorAssesFormServiceImpl---OAPassFlow", businessId, param);
        VendorAssesForm vendorAssesForm = new VendorAssesForm()
                .setVendorAssesId(businessId)
                .setMIsFeedback( VendorAssesFormConstant.M_IS_FEEDBACK_Y)
                .setStatus(VendorAssesFormStatus.ASSESSED.getKey());
        this.updateById(vendorAssesForm);

    }

    @Override
    public void rejectFlow(Long businessId, String param) throws Exception {
        log.info("VendorAssesFormServiceImpl---OARejectFlow", businessId, param);
        VendorAssesForm vendorAssesForm = new VendorAssesForm()
                .setVendorAssesId(businessId)
                .setMIsFeedback(VendorAssesFormConstant.M_IS_FEEDBACK_Y)
                .setStatus(VendorAssesFormStatus.REJECTED.getKey());
        this.updateById(vendorAssesForm);

    }

    @Override
    public void withdrawFlow(Long businessId, String param) throws Exception {
        log.info("VendorAssesFormServiceImpl---OAWithdrawFlow", businessId, param);
        VendorAssesForm vendorAssesForm = new VendorAssesForm()
                .setVendorAssesId(businessId)
                .setMIsFeedback( VendorAssesFormConstant.M_IS_FEEDBACK_Y)
                .setStatus(VendorAssesFormStatus.WITHDRAW.getKey());
        this.updateById(vendorAssesForm);

    }

    @Override
    public void destoryFlow(Long businessId, String param) throws Exception {
        log.info("VendorAssesFormServiceImpl---OADestoryFlow", businessId, param);
        VendorAssesForm vendorAssesForm = new VendorAssesForm().setVendorAssesId(businessId).setStatus(VendorAssesFormStatus.OBSOLETE.getKey());
        this.updateById(vendorAssesForm);
    }

    @Override
    public String getVariableFlow(Long businessId, String param) throws Exception {
        log.info("getVariableFlow: {}, {}", businessId, param);
        return null;
    }

    @Override
    public String getDataPushFlow(Long businessId, String param) throws Exception {
        log.info("getDataPushFlow: {}, {}", businessId, param);
        return null;
    }
}
