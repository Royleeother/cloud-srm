package com.midea.cloud.srm.cm.contract.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.googlecode.aviator.AviatorEvaluator;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.ContractHeadConst;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.cm.PaymentStatus;
import com.midea.cloud.common.enums.contract.ContractSourceType;
import com.midea.cloud.common.enums.contract.ContractStatus;
import com.midea.cloud.common.enums.contract.ContractType;
import com.midea.cloud.common.enums.contract.ModelKey;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.cm.CmSaopUrl;
import com.midea.cloud.srm.cm.contract.mapper.ContractHeadMapper;
import com.midea.cloud.srm.cm.contract.service.*;
import com.midea.cloud.srm.cm.contract.workflow.ContractFlow;
import com.midea.cloud.srm.cm.model.service.IModelHeadService;
import com.midea.cloud.srm.cm.model.service.IModelLineService;
import com.midea.cloud.srm.cm.soap.ContractExecutePtt;
import com.midea.cloud.srm.cm.template.service.IPayTypeService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.inq.InqClient;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.signature.SignatureClient;
import com.midea.cloud.srm.feign.supcooperate.SupcooperateClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.dept.dto.DeptDto;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuDetailVO;
import com.midea.cloud.srm.model.base.ou.vo.BaseOuGroupDetailVO;
import com.midea.cloud.srm.model.base.purchase.dto.PurchaseCategoryAllInfo;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCurrency;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseTax;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseUnit;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirement;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.BidVendor;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.enums.SourceFrom;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.vo.GenerateSourceFormResult;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.vo.SourceForm;
import com.midea.cloud.srm.model.cm.annex.Annex;
import com.midea.cloud.srm.model.cm.contract.dto.*;
import com.midea.cloud.srm.model.cm.contract.entity.*;
import com.midea.cloud.srm.model.cm.contract.soap.Request;
import com.midea.cloud.srm.model.cm.contract.soap.Response;
import com.midea.cloud.srm.model.cm.contract.vo.ContractHeadVO;
import com.midea.cloud.srm.model.cm.model.entity.ModelLine;
import com.midea.cloud.srm.model.cm.template.entity.PayType;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.inq.price.dto.ApprovalToContractDetail;
import com.midea.cloud.srm.model.inq.price.dto.PriceLibraryContractRequestDTO;
import com.midea.cloud.srm.model.inq.price.dto.PriceLibraryContractResDTO;
import com.midea.cloud.srm.model.inq.price.entity.ApprovalBiddingItem;
import com.midea.cloud.srm.model.inq.price.entity.ApprovalBiddingItemPaymentTerm;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibrary;
import com.midea.cloud.srm.model.inq.price.enums.SourcingType;
import com.midea.cloud.srm.model.inq.price.vo.ApprovalBiddingItemVO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementHead;
import com.midea.cloud.srm.model.pm.ps.advance.entity.AdvanceApplyHead;
import com.midea.cloud.srm.model.pm.ps.advance.vo.AdvanceApplyHeadVo;
import com.midea.cloud.srm.model.pm.ps.payment.dto.CeeaPaymentApplyDTO;
import com.midea.cloud.srm.model.pm.ps.payment.entity.CeeaPaymentApplyPlan;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.signature.SigningParam;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ???????????? ???????????????
 * </pre>
 *
 * @author chensl26@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-05-27 10:10:46
 *  ????????????:
 * </pre>
 */
@Service
public class ContractHeadServiceImpl extends ServiceImpl<ContractHeadMapper, ContractHead> implements IContractHeadService {

    @Autowired
    FileCenterClient fileCenterClient;

    @Autowired
    IContractLineService iContractLineService;

    @Autowired
    IContractMaterialService iContractMaterialService;

    @Autowired
    IContractPartnerService iContractPartnerService;

    @Autowired
    IPayPlanService iPayPlanService;

    @Autowired
    BaseClient baseClient;

    @Autowired
    InqClient inqClient;

    @Autowired
    IModelLineService iModelLineService;

    @Resource
    IModelHeadService iModelHeadService;

    @Resource
    private SupcooperateClient supcooperateClient;

    @Autowired
    private IPayTypeService iPayTypeService;

    @Resource
    private SupplierClient supplierClient;

    @Resource
    private PmClient pmClient;

    @Resource
    private IAnnexService iAnnexService;

    @Resource
    private RbacClient rbacClient;

    @Resource
    private com.midea.cloud.srm.feign.bid.SourceFormClient sourceFormClientBid;

    @Resource
    private com.midea.cloud.srm.feign.bargaining.SourceFormClient sourceFormClientBargain;

    @Resource
    private ICloseAnnexService iCloseAnnexService;

    @Resource
    private ILevelMaintainService iLevelMaintainService;

    @Autowired
    private ContractFlow contractFlow;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${CM_USER.cmUserName}")
    private String contractUserName;

    @Value("${CM_USER.cmPassword}")
    private String contractPassword;
    @Autowired
    private ContractHeadMapper contractHeadMapper;

    @Resource
    private SignatureClient signatureClient;

    @Override
    public PageInfo<ContractHead> queryContractByVendorId(ContractHead contractHead) {
        Assert.notNull(contractHead,"??????????????????");
        Assert.notNull(contractHead.getVendorId(),"vendorId????????????");
        PageUtil.startPage(contractHead.getPageNum(),contractHead.getPageSize());
        ContractDTO contractDTO = new ContractDTO();
        List<ContractHead> contractHeads = this.list(Wrappers.lambdaQuery(ContractHead.class).
                eq(ContractHead::getVendorId, contractHead.getVendorId()).
                eq(ContractHead::getContractStatus, ContractStatus.ARCHIVED));
        if(CollectionUtils.isNotEmpty(contractHeads)){
            contractHeads.forEach(contractHead1 -> {
                List<PayPlan> payPlans = iPayPlanService.list(Wrappers.lambdaQuery(PayPlan.class).
                        eq(PayPlan::getContractHeadId, contractHead1.getContractHeadId()));
                contractHead1.setPayPlans(payPlans);
            });
        }
        return new PageInfo<>(contractHeads);
    }

    public String getContractLevel(List<ContractMaterial> contractMaterials) {
        String contractLevel = null;
        if (CollectionUtils.isNotEmpty(contractMaterials)) {
            ArrayList<String> levels = new ArrayList<>();
            List<String> codes = new ArrayList<>();
            StringBuilder builder = new StringBuilder();
            contractMaterials.forEach(contractMaterial -> {
                if (StringUtil.isEmpty(contractMaterial.getAmount())) {
                    contractMaterial.setAmount(BigDecimal.ZERO);
                }
                if (StringUtil.notEmpty(contractMaterial.getMaterialCode())) {
                    codes.add(contractMaterial.getMaterialCode());
                } else {
                    builder.append(contractMaterial.getMaterialName()).append(",");
                }
            });
            // ????????????

            if (CollectionUtils.isEmpty(codes)) {
                throw new BaseException(String.format("?????????????????????[%s]???????????????", builder.toString()));
            }
            List<MaterialItem> materialItems = baseClient.listMaterialByCodeBatch(codes);

            if (CollectionUtils.isNotEmpty(materialItems)) {
                /**
                 * ?????????????????????????????????????????????
                 */
                Map<Long, List<MaterialItem>> materialMap = materialItems.stream().filter(materialItem -> StringUtil.notEmpty(materialItem.getStruct())).collect(Collectors.groupingBy(MaterialItem::getMaterialId));
                if (null != materialMap && !materialMap.isEmpty()) {
                    StringBuffer errorMsg = new StringBuffer();
                    contractMaterials.forEach(contractMaterial -> {
                        if (materialMap.containsKey(contractMaterial.getMaterialId())) {
                            String struct = materialMap.get(contractMaterial.getMaterialId()).get(0).getStruct();
                            contractMaterial.setStruct(struct);
                        } else {
                            errorMsg.append(contractMaterial.getMaterialName()).append(";");
                        }
                    });
                    if (errorMsg.length() > 0) {
                        throw new BaseException("??????????????????->????????????:?????????????????????{" + errorMsg.toString() + "}");
                    }
                } else {
                    throw new BaseException("??????????????????->????????????:???????????????????????????");
                }
                Map<String, List<ContractMaterial>> collect = contractMaterials.stream().filter(contractMaterial -> null != contractMaterial.getStruct()).collect(Collectors.groupingBy(ContractMaterial::getStruct));
                collect.forEach((struct, contractMaterialList) -> {
                    BigDecimal sum = contractMaterialList.stream().map(ContractMaterial::getAmount).reduce(BigDecimal::add).get();
                    QueryWrapper<LevelMaintain> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("STRUCT", struct).orderByDesc("LEVEL");
                    queryWrapper.and(wrapper -> wrapper.isNull("END_DATA").or().ge("END_DATA", LocalDate.now()));
                    List<LevelMaintain> levelMaintains = iLevelMaintainService.list(queryWrapper);
                    if (CollectionUtils.isNotEmpty(levelMaintains)) {
                        for (LevelMaintain levelMaintain : levelMaintains) {
                            String formula = levelMaintain.getFormula();
                            String replace = StringUtils.replace(formula, "${value}", String.valueOf(sum.doubleValue() / 10000));
                            boolean flag = (boolean) AviatorEvaluator.execute(replace);
                            if (flag) {
                                levels.add(levelMaintain.getLevel());
                                break;
                            }
                        }
                    }
                });
            }
            if (CollectionUtils.isNotEmpty(levels)) {
                levels.sort((o1, o2) -> o1.compareTo(o2));
                contractLevel = levels.get(0);
            }
        }

        return contractLevel;
    }

    @Override
    @Transactional
    public void buyerSaveOrUpdateContractDTO(ContractDTO contractDTO, String contractStatus) {
        ContractHead contractHead = contractDTO.getContractHead();
        List<ContractLine> contractLines = contractDTO.getContractLines();
        List<ContractMaterial> contractMaterials = contractDTO.getContractMaterials();
        List<PayPlan> payPlans = contractDTO.getPayPlans();
        List<Fileupload> fileuploads = contractDTO.getFileuploads();

        //??????????????????????????????
        saveOrUpdateContractHead(contractHead, contractStatus);
        //????????????????????????????????????
        saveOrUpdateContractLines(contractHead, contractLines);
        //?????????????????????????????????
        saveOrUpdateContractMaterials(contractHead, contractMaterials);
        //?????????????????????????????????
        saveOrUpdatePayPlans(contractHead, payPlans);
        //???????????????????????????
        bindingFileuploads(contractHead, fileuploads);
    }

    @Override
    public PageInfo<ContractHead> listPageByParam(ContractHeadDTO contractHeadDTO) {
        PageUtil.startPage(contractHeadDTO.getPageNum(), contractHeadDTO.getPageSize());
        List<ContractHead> contractHeads = listContractHead(contractHeadDTO);
        return new PageInfo<>(contractHeads);
    }

//    @Override
//    public void export(ContractHeadDTO contractHeadDTO, HttpServletResponse response) throws IOException {
//        Long count = queryCountByList(contractHeadDTO);
//        if (count > 20000){
//            // ???????????????????????????
//            if(StringUtil.notEmpty(contractHeadDTO.getPageNum()) && StringUtil.notEmpty(contractHeadDTO.getPageSize())){
//                Assert.isTrue(contractHeadDTO.getPageSize() <= 20000,"?????????????????????20000");
//            }else {
//                throw new BaseException("??????????????????20000,?????????????????????,??????????????????20000");
//            }
//        }
//        if(StringUtil.notEmpty(contractHeadDTO.getPageNum()) && StringUtil.notEmpty(contractHeadDTO.getPageSize())){
//            PageUtil.startPage(contractHeadDTO.getPageNum(),contractHeadDTO.getPageSize());
//        }
//        Map<String, String> contarctLevelMap = EasyExcelUtil.getDicCodeName("CONTARCT_LEVEL", baseClient);
//        Map<String, String> elemContractTypeMap = EasyExcelUtil.getDicCodeName("ELEM_CONTRACT_TYPE", baseClient);
//        Map<String, String> contractStatusMap = EasyExcelUtil.getDicCodeName("CONTRACT_STATUS", baseClient);
//        Map<String, String> contractTypeMap = EasyExcelUtil.getDicCodeName("CONTRACT_TYPE", baseClient);
//        // ??????????????????
//        ArrayList<ContractListExportDto> contractListExportDtos = new ArrayList<>();
//        List<ContractHead> contractHeads = listContractHeadExport(contractHeadDTO);
//        if(CollectionUtils.isNotEmpty(contractHeads)){
//            contractHeads.forEach(contractHead -> {
//                ContractListExportDto contractListExportDto = new ContractListExportDto();
//                BeanCopyUtil.copyProperties(contractListExportDto,contractHead);
//                // ????????????
//                contractListExportDto.setStartDate(DateUtil.localDateToStr(contractHead.getStartDate()));
//                contractListExportDto.setEffectiveDateFrom(DateUtil.localDateToStr(contractHead.getEffectiveDateFrom()));
//                contractListExportDto.setEffectiveDateTo(DateUtil.localDateToStr(contractHead.getEffectiveDateTo()));
//                // ???????????? : ????????????/????????????/????????????/????????????
//                contractListExportDto.setContractStatus(contractStatusMap.get(contractHead.getContractStatus()));
//                contractListExportDto.setContractLevel(contarctLevelMap.get(contractHead.getContractLevel()));
//                contractListExportDto.setContractType(contractTypeMap.get(contractHead.getContractType()));
//                contractListExportDto.setContractClass(elemContractTypeMap.get(contractHead.getContractClass()));
//                contractListExportDtos.add(contractListExportDto);
//            });
//        }
//        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????");
//        EasyExcelUtil.writeExcelWithModel(outputStream,"sheet",contractListExportDtos,ContractListExportDto.class);
//    }

    @Override
    public void export(ContractHeadDTO contractHeadDTO, HttpServletResponse response) throws IOException {
        Long count = queryCountByList(contractHeadDTO);
        Map<String, String> contarctLevelMap = EasyExcelUtil.getDicCodeName("CONTARCT_LEVEL", baseClient);
        Map<String, String> elemContractTypeMap = EasyExcelUtil.getDicCodeName("ELEM_CONTRACT_TYPE", baseClient);
        Map<String, String> contractStatusMap = EasyExcelUtil.getDicCodeName("CONTRACT_STATUS", baseClient);
        Map<String, String> contractTypeMap = EasyExcelUtil.getDicCodeName("CONTRACT_TYPE", baseClient);

        if(count <= 20000){
            // ??????????????????
            List<ContractListExportDto> contractListExportDtos = getContractListExportDtos(contractHeadDTO, contarctLevelMap, elemContractTypeMap, contractStatusMap, contractTypeMap);
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????");
            EasyExcelUtil.writeExcelWithModel(outputStream,"sheet",contractListExportDtos,ContractListExportDto.class);
        }else {
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????");
            ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
            Long size = count / 20000 + 1;
            for(int i = 0 ; i < size ; i++){
                WriteSheet writeSheet = EasyExcel.writerSheet(i, "sheet" + i).head(ContractListExportDto.class).build();
                // ??????????????????
                List<ContractListExportDto> contractListExportDtos = getContractListExportDtos(contractHeadDTO, contarctLevelMap, elemContractTypeMap, contractStatusMap, contractTypeMap, i);
                excelWriter.write(contractListExportDtos,writeSheet);
            }
            excelWriter.finish();
        }
    }

    @Override
    public void exportLine(ContractHeadDTO contractHeadDTO, HttpServletResponse response) throws IOException {
        Map<String, String> contarctLevelMap = EasyExcelUtil.getDicCodeName("CONTARCT_LEVEL", baseClient);
        Map<String, String> elemContractTypeMap = EasyExcelUtil.getDicCodeName("ELEM_CONTRACT_TYPE", baseClient);
        Map<String, String> contractStatusMap = EasyExcelUtil.getDicCodeName("CONTRACT_STATUS", baseClient);
        Map<String, String> sourceTypeMap = EasyExcelUtil.getDicCodeName("Sourc_Type ", baseClient);
        Map<Long, PurchaseCategoryAllInfo> longPurchaseCategoryAllInfoMap = baseClient.queryPurchaseCategoryAllInfo();

        Long count = this.baseMapper.queryContractDescExportcount(contractHeadDTO);
        if(count <= 50000){
            List<ContractDescExportDto> descExportDtoArrayList = getContractDescExportDtos(-1,contractHeadDTO, contarctLevelMap, elemContractTypeMap, contractStatusMap, sourceTypeMap, longPurchaseCategoryAllInfoMap);
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????");
            EasyExcelUtil.writeExcelWithModel(outputStream,"sheet",descExportDtoArrayList,ContractDescExportDto.class);
        }else {
            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????");
            ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
            count = count <= 500000?count:500000;
            Long size = count / 50000 + 1;
            for(int i = 0 ; i < size ; i++){
                WriteSheet writeSheet = EasyExcel.writerSheet(i, "sheet" + i).head(ContractDescExportDto.class).build();
                // ??????????????????
                List<ContractDescExportDto> descExportDtoArrayList = getContractDescExportDtos(i,contractHeadDTO, contarctLevelMap, elemContractTypeMap, contractStatusMap, sourceTypeMap, longPurchaseCategoryAllInfoMap);
                excelWriter.write(descExportDtoArrayList,writeSheet);
            }
            excelWriter.finish();
        }

    }

    public List<ContractDescExportDto> getContractDescExportDtos(int i,ContractHeadDTO contractHeadDTO, Map<String, String> contarctLevelMap, Map<String, String> elemContractTypeMap, Map<String, String> contractStatusMap, Map<String, String> sourceTypeMap, Map<Long, PurchaseCategoryAllInfo> longPurchaseCategoryAllInfoMap) {
        List<ContractDescExportDto> descExportDtoArrayList = new ArrayList<>();
        // ??????
        if(-1 != i){
            PageUtil.startPage(i+1,50000);
        }
        List<ContractDescExportDto> contractDescExportDtos = listContractDescExport(contractHeadDTO);
        if(CollectionUtils.isNotEmpty(contractDescExportDtos)){
            Map<Long, List<ContractDescExportDto>> collect = contractDescExportDtos.stream().collect(Collectors.groupingBy(ContractDescExportDto::getContractHeadId));
            if(!collect.isEmpty()){
                collect.forEach((contractHeadId, contractDescExportDtoList) -> {
                    AtomicInteger no = new AtomicInteger(1);
                    contractDescExportDtoList.forEach(contractDescExportDto -> {
                        // ???????????????
                        contractDescExportDto.setNo(no.getAndAdd(1));
                        // ????????????
                        contractDescExportDto.setDateTo(DateUtil.localDateToStr(contractDescExportDto.getEffectiveDateFrom()));
                        contractDescExportDto.setDateEnd(DateUtil.localDateToStr(contractDescExportDto.getEffectiveDateTo()));
                        contractDescExportDto.setLineDateTo(DateUtil.localDateToStr(contractDescExportDto.getStartDate()));
                        contractDescExportDto.setLineDateEnd(DateUtil.localDateToStr(contractDescExportDto.getEndDate()));
                        // ????????????
                        contractDescExportDto.setContractLevel(contarctLevelMap.get(contractDescExportDto.getContractLevel()));
                        contractDescExportDto.setContractStatus(contractStatusMap.get(contractDescExportDto.getContractStatus()));
                        contractDescExportDto.setContractClass(elemContractTypeMap.get(contractDescExportDto.getContractClass()));
                        contractDescExportDto.setSourceType(sourceTypeMap.get(contractDescExportDto.getSourceType()));
                        // ??????????????????
                        PurchaseCategoryAllInfo purchaseCategoryAllInfo = longPurchaseCategoryAllInfoMap.get(contractDescExportDto.getCategoryId());
                        if(null != purchaseCategoryAllInfo){
                            contractDescExportDto.setCategoryName1(purchaseCategoryAllInfo.getCategoryName3());
                            contractDescExportDto.setCategoryName2(purchaseCategoryAllInfo.getCategoryName2());
                            contractDescExportDto.setCategoryName3(purchaseCategoryAllInfo.getCategoryName1());
                        }
                    });
                    descExportDtoArrayList.addAll(contractDescExportDtoList);
                });
            }
        }
        return descExportDtoArrayList;
    }

    public ArrayList<ContractListExportDto> getContractListExportDtos(ContractHeadDTO contractHeadDTO, Map<String, String> contarctLevelMap, Map<String, String> elemContractTypeMap, Map<String, String> contractStatusMap, Map<String, String> contractTypeMap, int i) {
        ArrayList<ContractListExportDto> contractListExportDtos = new ArrayList<>();
        PageUtil.startPage(i+1,20000);
        List<ContractHead> contractHeads = listContractHeadExport(contractHeadDTO);
        if(CollectionUtils.isNotEmpty(contractHeads)){
            contractHeads.forEach(contractHead -> {
                ContractListExportDto contractListExportDto = new ContractListExportDto();
                BeanCopyUtil.copyProperties(contractListExportDto,contractHead);
                // ????????????
                contractListExportDto.setStartDate(DateUtil.localDateToStr(contractHead.getStartDate()));
                contractListExportDto.setEffectiveDateFrom(DateUtil.localDateToStr(contractHead.getEffectiveDateFrom()));
                contractListExportDto.setEffectiveDateTo(DateUtil.localDateToStr(contractHead.getEffectiveDateTo()));
                // ???????????? : ????????????/????????????/????????????/????????????
                contractListExportDto.setContractStatus(contractStatusMap.get(contractHead.getContractStatus()));
                contractListExportDto.setContractLevel(contarctLevelMap.get(contractHead.getContractLevel()));
                contractListExportDto.setContractType(contractTypeMap.get(contractHead.getContractType()));
                contractListExportDto.setContractClass(elemContractTypeMap.get(contractHead.getContractClass()));
                contractListExportDtos.add(contractListExportDto);
            });
        }
        return contractListExportDtos;
    }

    public ArrayList<ContractListExportDto> getContractListExportDtos(ContractHeadDTO contractHeadDTO, Map<String, String> contarctLevelMap, Map<String, String> elemContractTypeMap, Map<String, String> contractStatusMap, Map<String, String> contractTypeMap) {
        // ??????????????????
        ArrayList<ContractListExportDto> contractListExportDtos = new ArrayList<>();
        List<ContractHead> contractHeads = listContractHeadExport(contractHeadDTO);
        if(CollectionUtils.isNotEmpty(contractHeads)){
            contractHeads.forEach(contractHead -> {
                ContractListExportDto contractListExportDto = new ContractListExportDto();
                BeanCopyUtil.copyProperties(contractListExportDto,contractHead);
                // ????????????
                contractListExportDto.setStartDate(DateUtil.localDateToStr(contractHead.getStartDate()));
                contractListExportDto.setEffectiveDateFrom(DateUtil.localDateToStr(contractHead.getEffectiveDateFrom()));
                contractListExportDto.setEffectiveDateTo(DateUtil.localDateToStr(contractHead.getEffectiveDateTo()));
                // ???????????? : ????????????/????????????/????????????/????????????
                contractListExportDto.setContractStatus(contractStatusMap.get(contractHead.getContractStatus()));
                contractListExportDto.setContractLevel(contarctLevelMap.get(contractHead.getContractLevel()));
                contractListExportDto.setContractType(contractTypeMap.get(contractHead.getContractType()));
                contractListExportDto.setContractClass(elemContractTypeMap.get(contractHead.getContractClass()));
                contractListExportDtos.add(contractListExportDto);
            });
        }
        return contractListExportDtos;
    }

    @Override
    @AuthData(module = {MenuEnum.CONTRACT_MAINTAIN_LIST, MenuEnum.SUPPLIER_SIGN})
    public List<ContractHead> listContractHead(ContractHeadDTO contractHeadDTO) {
        if (StringUtil.notEmpty(contractHeadDTO.getVendorId())) {
            contractHeadDTO.setVendorIdMan(String.valueOf(contractHeadDTO.getVendorId()));
        }
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String userType = loginAppUser.getUserType();
        if (UserType.VENDOR.name().equals(userType)) {
            if (StringUtil.isEmpty(loginAppUser.getCompanyId())) {
                return null;
            }
            contractHeadDTO.setVendorId(loginAppUser.getCompanyId()).setUserType(UserType.VENDOR.name());
        }
//        else if (UserType.BUYER.name().equals(userType)) {
//            contractHeadDTO.setVendorId(null);
//        }
        //???????????????????????????
        Date creationDate = contractHeadDTO.getCreationDate();
        if (creationDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(creationDate);
            cal.add(Calendar.HOUR, 23);
            cal.add(Calendar.MINUTE, 59);
            cal.add(Calendar.SECOND, 59);
            Date endCreationDate = cal.getTime();
            contractHeadDTO.setStartCreationDate(creationDate).setEndCreationDate(endCreationDate);
        }

        return this.baseMapper.listPageByParam(contractHeadDTO);
    }

    @AuthData(module = {MenuEnum.CONTRACT_MAINTAIN_LIST, MenuEnum.SUPPLIER_SIGN})
    public List<ContractDescExportDto> listContractDescExport(ContractHeadDTO contractHeadDTO) {
        if (StringUtil.notEmpty(contractHeadDTO.getVendorId())) {
            contractHeadDTO.setVendorIdMan(String.valueOf(contractHeadDTO.getVendorId()));
        }
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String userType = loginAppUser.getUserType();
        if (UserType.VENDOR.name().equals(userType)) {
            if (StringUtil.isEmpty(loginAppUser.getCompanyId())) {
                return null;
            }
            contractHeadDTO.setVendorId(loginAppUser.getCompanyId()).setUserType(UserType.VENDOR.name());
        }
//        else if (UserType.BUYER.name().equals(userType)) {
//            contractHeadDTO.setVendorId(null);
//        }
        //???????????????????????????
        Date creationDate = contractHeadDTO.getCreationDate();
        if (creationDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(creationDate);
            cal.add(Calendar.HOUR, 23);
            cal.add(Calendar.MINUTE, 59);
            cal.add(Calendar.SECOND, 59);
            Date endCreationDate = cal.getTime();
            contractHeadDTO.setStartCreationDate(creationDate).setEndCreationDate(endCreationDate);
        }

        return this.baseMapper.queryContractDescExportDate(contractHeadDTO);
    }

    @AuthData(module = {MenuEnum.CONTRACT_MAINTAIN_LIST, MenuEnum.SUPPLIER_SIGN})
    public List<ContractHead> listContractHeadExport(ContractHeadDTO contractHeadDTO) {
        if (StringUtil.notEmpty(contractHeadDTO.getVendorId())) {
            contractHeadDTO.setVendorIdMan(String.valueOf(contractHeadDTO.getVendorId()));
        }
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String userType = loginAppUser.getUserType();
        if (UserType.VENDOR.name().equals(userType)) {
            if (StringUtil.isEmpty(loginAppUser.getCompanyId())) {
                return null;
            }
            contractHeadDTO.setVendorId(loginAppUser.getCompanyId()).setUserType(UserType.VENDOR.name());
        }
//        else if (UserType.BUYER.name().equals(userType)) {
//            contractHeadDTO.setVendorId(null);
//        }
        //???????????????????????????
        Date creationDate = contractHeadDTO.getCreationDate();
        if (creationDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(creationDate);
            cal.add(Calendar.HOUR, 23);
            cal.add(Calendar.MINUTE, 59);
            cal.add(Calendar.SECOND, 59);
            Date endCreationDate = cal.getTime();
            contractHeadDTO.setStartCreationDate(creationDate).setEndCreationDate(endCreationDate);
        }

        return this.baseMapper.listPageByParamExport(contractHeadDTO);
    }

    /**
     * ?????????????????????
     * @param contractHeadDTO
     * @return
     */
    @Override
    @AuthData(module = {MenuEnum.CONTRACT_MAINTAIN_LIST, MenuEnum.SUPPLIER_SIGN})
    public Long queryCountByList(ContractHeadDTO contractHeadDTO){
        if (StringUtil.notEmpty(contractHeadDTO.getVendorId())) {
            contractHeadDTO.setVendorIdMan(String.valueOf(contractHeadDTO.getVendorId()));
        }
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String userType = loginAppUser.getUserType();
        if (UserType.VENDOR.name().equals(userType)) {
            if (StringUtil.isEmpty(loginAppUser.getCompanyId())) {
                return null;
            }
            contractHeadDTO.setVendorId(loginAppUser.getCompanyId()).setUserType(UserType.VENDOR.name());
        }
        //???????????????????????????
        Date creationDate = contractHeadDTO.getCreationDate();
        if (creationDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(creationDate);
            cal.add(Calendar.HOUR, 23);
            cal.add(Calendar.MINUTE, 59);
            cal.add(Calendar.SECOND, 59);
            Date endCreationDate = cal.getTime();
            contractHeadDTO.setStartCreationDate(creationDate).setEndCreationDate(endCreationDate);
        }

        return this.baseMapper.queryCount(contractHeadDTO);
    }

    @Override
    @Transactional
    public void vendorUpdateContractHeadDTO(ContractHeadDTO contractHeadDTO) {
        ContractHead contractHead = new ContractHead();
        BeanUtils.copyProperties(contractHeadDTO, contractHead);
        //??????????????????,??????????????????????????????????????????????????????
        if (ContractStatus.SUPPLIER_CONFIRMED.name().equals(contractHead.getContractStatus())) {
            contractHead.setVendorConfirmDate(LocalDate.now())
                    .setVendorConfirmBy(AppUserUtil.getLoginAppUser().getUsername());
        }
        this.updateById(contractHead);
    }

    @Override
    public ContractDTO getContractDTO(Long contractHeadId) {
        Assert.notNull(contractHeadId, "contractHeadId????????????");
        ContractDTO contractDTO = new ContractDTO();
        ContractHead contractHead = this.getById(contractHeadId);
        List<ContractLine> contractLines = iContractLineService.list(new QueryWrapper<>(
                new ContractLine().setContractHeadId(contractHeadId)));
        List<PayPlan> payPlans = iPayPlanService.list(new QueryWrapper<>(
                new PayPlan().setContractHeadId(contractHeadId)));
        List<ContractMaterial> contractMaterials = iContractMaterialService.list(new QueryWrapper<>(
                new ContractMaterial().setContractHeadId(contractHeadId)));
        List<Fileupload> fileuploads = new ArrayList<>();
        if (contractHead != null) {
            List<Fileupload> templFiles = fileCenterClient.listPage(new Fileupload().setBusinessId(contractHead.getTemplHeadId()), null).getList();
            fileuploads.addAll(templFiles);
            List<Fileupload> contractFiles = fileCenterClient.listPage(new Fileupload().setBusinessId(contractHeadId), null).getList();
            fileuploads.addAll(contractFiles);
        }
        contractDTO.setContractHead(contractHead)
                .setContractLines(contractLines)
                .setContractMaterials(contractMaterials)
                .setPayPlans(payPlans)
                .setFileuploads(fileuploads);
        return contractDTO;
    }

    @Override
    public void buyerUpdateContractStatus(ContractHeadDTO contractHeadDTO, String contractStatus) {
        ContractHead contractHead = this.getById(contractHeadDTO.getContractHeadId());
        Assert.notNull(contractHead, LocaleHandler.getLocaleMsg("contractHead????????????"));

        //?????????,?????????
        if (ContractStatus.SUPPLIER_CONFIRMING.name().equals(contractStatus)) {
            buyerCheckBeforePublish(contractHead);
        }

        //?????????,?????????
        if (ContractStatus.ARCHIVED.name().equals(contractStatus)) {
//            buyerCheckBeforeArchived(contractHead);
            contractHead.setStartDate(LocalDate.now());
        }

        //?????????????????????
        if (ContractStatus.REFUSED.name().equals(contractStatus)) {
            contractHead.setApprovalAdvice(contractHeadDTO.getApprovalAdvice());
        }

        //?????????????????????
        if (ContractStatus.WITHDRAW.name().equals(contractStatus)) {
            contractHead.setApprovalAdvice(contractHeadDTO.getApprovalAdvice());
        }
        //2020-11-02 ????????????????????????????????????
//        if (ContractStatus.ARCHIVED.name().equals(contractStatus)) {
//            try {
//                Response response = pushContractInfo(contractHead.getContractHeadId());
//                if ("E".equals(response.getEsbInfo().getReturnStatus())) {
//                    log.error("????????????????????????????????????" + response.getEsbInfo().getReturnMsg());
//                    throw new BaseException(response.getEsbInfo().getReturnMsg());
//                }
//            } catch (BaseException e) {
//                throw new BaseException("????????????! " + e.getMessage());
//            } catch (Exception e) {
//                log.error("??????????????????", e);
//                throw new BaseException("????????????! ");
//            }
//        }

        contractHead.setContractStatus(contractStatus);
        this.updateById(contractHead);
    }

    private void buyerCheckBeforeArchived(ContractHead contractHead) {
        if (!ContractStatus.SUPPLIER_CONFIRMED.name().equals(contractHead.getContractStatus())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????,????????????"));
        }
    }

    @Override
    public void vendorUpdateContractStatus(Long contractHeadId, String contractStatus) {
        ContractHead contractHead = this.getById(contractHeadId);
        Assert.notNull(contractHead, LocaleHandler.getLocaleMsg("contractHead????????????"));

        //?????????,?????????
        if (ContractStatus.REJECTED.name().equals(contractStatus)) {
            vendorCheckBeforeReject(contractHead);
        }

        //?????????,?????????
        if (ContractStatus.SUPPLIER_CONFIRMED.name().equals(contractStatus)) {
            vendorCheckBeforeSubmit(contractHead);
            contractHead.setVendorConfirmBy(AppUserUtil.getLoginAppUser().getUsername())
                    .setVendorConfirmDate(LocalDate.now());
        }

        contractHead.setContractStatus(contractStatus);
        this.updateById(contractHead);
    }

    @Override
    @Transactional
    public void buyerDelete(Long contractHeadId) {
        this.removeById(contractHeadId);
        iContractLineService.remove(new QueryWrapper<>(new ContractLine().setContractHeadId(contractHeadId)));
        iPayPlanService.remove(new QueryWrapper<>(new PayPlan().setContractHeadId(contractHeadId)));
        iContractMaterialService.remove(new QueryWrapper<>(new ContractMaterial().setContractHeadId(contractHeadId)));
        fileCenterClient.deleteByParam(new Fileupload().setBusinessId(contractHeadId));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //??????????????????????????????????????????
                Boolean contractUpdateApproval = redisTemplate.opsForSet().isMember("contractUpdateApproval", String.valueOf(contractHeadId));
                if (contractUpdateApproval) {
                    do {
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
                    }
                    while (redisTemplate.opsForSet().isMember("contractUpdateApproval", String.valueOf(contractHeadId)));
                }
                inqClient.resetItemsBelongNumber(Collections.singletonList(contractHeadId));
            }
        });
    }

    /**
     * ????????????
     *
     * @param contractHeadId
     */
    @Override
    @Transactional
    public void abandon(Long contractHeadId) {
        ContractDTO contractDTO = this.getContractDTOSecond(contractHeadId, null);
        ContractHead contractHead = contractDTO.getContractHead();
        Assert.notNull(ObjectUtils.isEmpty(contractHead), "????????????????????????");
        String contractStatus = contractHead.getContractStatus();
        Assert.isTrue(ContractStatus.WITHDRAW.toString().equals(contractStatus) || ContractStatus.REJECTED.toString().equals(contractStatus), "??????????????????????????????????????????????????????");
        contractHead.setContractStatus(ContractStatus.ABANDONED.toString());
        this.updateById(contractHead);
        SrmFlowBusWorkflow srmworkflowForm = baseClient.getSrmFlowBusWorkflow(contractHeadId);
        if (srmworkflowForm != null) {
            try {
                contractDTO.setProcessType("N");
                contractFlow.submitContractDTOConfFlow(contractDTO);
            } catch (Exception e) {
                Assert.isTrue(false, "??????????????????????????????");
            }
        }
    }


    @Override
    public List<ContractMaterial> getMaterialsBySourceNumber(String sourceNumber, Long orgId, Long vendorId) {
        List<ContractMaterial> contractMaterials = new ArrayList<>();
        //????????????????????????,????????????????????????
        if (StringUtil.notEmpty(sourceNumber)) {
            contractMaterials = this.getContractMaterialsBySourceNumber(sourceNumber, contractMaterials);
        } else if (StringUtil.notEmpty(orgId) && StringUtil.notEmpty(vendorId)) {
            this.getContractMaterialsByOrgVendor(orgId, vendorId, contractMaterials);
        }
        return contractMaterials;
    }

    @Override
    public List<ContractMaterial> getMaterialsBySourceNumberAndorgIdAndvendorId(String sourceNumber, Long orgId, Long vendorId) {
        List<ContractMaterial> contractMaterials = new ArrayList<>();
        if (StringUtil.notEmpty(sourceNumber) && StringUtil.notEmpty(orgId) && StringUtil.notEmpty(vendorId)) {
            List<PriceLibrary> priceLibraries = inqClient.listPagePriceLibrary(
                    new PriceLibrary().setSourceNo(sourceNumber).setOrganizationId(orgId).setVendorId(vendorId)).getList();
            if (!CollectionUtils.isEmpty(priceLibraries)) {
                for (PriceLibrary priceLibrary : priceLibraries) {
                    ContractMaterial contractMaterial = new ContractMaterial();
                    if (priceLibrary != null) {
                        BeanUtils.copyProperties(priceLibrary, contractMaterial);
                        contractMaterial.setSourceNumber(sourceNumber)
                                .setMaterialCode(priceLibrary.getItemCode())
                                .setMaterialId(priceLibrary.getItemId())
                                .setMaterialName(priceLibrary.getItemDesc())
                                .setSpecification(priceLibrary.getSpecification())
                                .setUntaxedPrice(priceLibrary.getNotaxPrice())
                                .setTaxedPrice(priceLibrary.getTaxPrice())
//                                .setOrderQuantity(priceLibrary.getOrderQuantity())
                                .setContractQuantity(priceLibrary.getOrderQuantity());
                        if (StringUtils.isNumeric(priceLibrary.getTaxRate())) {
                            contractMaterial.setTaxKey(priceLibrary.getTaxKey());
                            contractMaterial.setTaxRate(new BigDecimal(priceLibrary.getTaxRate()));
                        }
                        //????????????????????????
                        String unit = priceLibrary.getUnit();
                        setUnit(contractMaterial, unit);

                        //??????????????????????????????
                        Long categoryId = priceLibrary.getCategoryId();
                        setCategory(contractMaterial, categoryId);

                        if (priceLibrary.getNotaxPrice() != null && priceLibrary.getOrderQuantity() != null) {
                            contractMaterial.setAmount(priceLibrary.getNotaxPrice().multiply(priceLibrary.getOrderQuantity()));
                        }
                    }
                    contractMaterials.add(contractMaterial);
                }
            }
        } else {
            throw new BaseException("??????????????????: sourceNumber,orgId,vendorId ");
        }
        return contractMaterials;
    }

    public void getContractMaterialsByOrgVendor(Long orgId, Long vendorId, List<ContractMaterial> contractMaterials) {
        List<PriceLibrary> priceLibraries = inqClient.listPagePriceLibrary(
                new PriceLibrary().setOrganizationId(orgId).setVendorId(vendorId)).getList();

        if (CollectionUtils.isNotEmpty(priceLibraries)) {
            for (PriceLibrary priceLibrary : priceLibraries) {
                ContractMaterial contractMaterial = new ContractMaterial();
                if (null != priceLibrary) {
                    BeanUtils.copyProperties(priceLibrary, contractMaterial);
                    contractMaterial.setSourceNumber(priceLibrary.getSourceNo())
                            .setMaterialCode(priceLibrary.getItemCode())
                            .setMaterialId(priceLibrary.getItemId())
                            .setMaterialName(priceLibrary.getItemDesc())
                            .setSpecification(priceLibrary.getSpecification())
                            .setUntaxedPrice(priceLibrary.getNotaxPrice())
                            .setTaxedPrice(priceLibrary.getTaxPrice())
//                            .setOrderQuantity(priceLibrary.getOrderQuantity())
                            .setContractQuantity(priceLibrary.getOrderQuantity());
                    if (StringUtils.isNumeric(priceLibrary.getTaxRate())) {
                        contractMaterial.setTaxKey(priceLibrary.getTaxKey());
                        contractMaterial.setTaxRate(new BigDecimal(priceLibrary.getTaxRate()));
                    }
                    //????????????????????????
                    String unit = priceLibrary.getUnit();
                    setUnit(contractMaterial, unit);

                    //??????????????????????????????
                    Long categoryId = priceLibrary.getCategoryId();
                    setCategory(contractMaterial, categoryId);

                    if (priceLibrary.getNotaxPrice() != null && priceLibrary.getOrderQuantity() != null) {
                        contractMaterial.setAmount(priceLibrary.getNotaxPrice().multiply(priceLibrary.getOrderQuantity()));
                    }
                    contractMaterials.add(contractMaterial);
                }
            }
        }
    }

    public List<ContractMaterial> getContractMaterialsBySourceNumber(String sourceNumber, List<ContractMaterial> contractMaterials) {
        List<PriceLibrary> priceLibraries = inqClient.listPagePriceLibrary(new PriceLibrary().setSourceNo(sourceNumber)).getList();
        if (!CollectionUtils.isEmpty(priceLibraries)) {
            contractMaterials = priceLibraries.stream().map(priceLibrary -> {
                ContractMaterial contractMaterial = new ContractMaterial();
                if (priceLibrary != null) {
                    BeanUtils.copyProperties(priceLibrary, contractMaterial);
                    contractMaterial.setSourceNumber(sourceNumber)
                            .setMaterialCode(priceLibrary.getItemCode())
                            .setMaterialId(priceLibrary.getItemId())
                            .setMaterialName(priceLibrary.getItemDesc())
                            .setSpecification(priceLibrary.getSpecification())
                            .setUntaxedPrice(priceLibrary.getNotaxPrice())
                            .setTaxedPrice(priceLibrary.getTaxPrice())
//                            .setOrderQuantity(priceLibrary.getOrderQuantity())
                            .setContractQuantity(priceLibrary.getOrderQuantity());
                    if (StringUtils.isNumeric(priceLibrary.getTaxRate())) {
                        contractMaterial.setTaxKey(priceLibrary.getTaxKey());
                        contractMaterial.setTaxRate(new BigDecimal(priceLibrary.getTaxRate()));
                    }
                    //????????????????????????
                    String unit = priceLibrary.getUnit();
                    setUnit(contractMaterial, unit);

                    //??????????????????????????????
                    Long categoryId = priceLibrary.getCategoryId();
                    setCategory(contractMaterial, categoryId);

                    if (priceLibrary.getNotaxPrice() != null && priceLibrary.getOrderQuantity() != null) {
                        contractMaterial.setAmount(priceLibrary.getNotaxPrice().multiply(priceLibrary.getOrderQuantity()));
                    }
                }
                return contractMaterial;
            }).collect(Collectors.toList());
        }
        return contractMaterials;
    }

    @Override
    @Transactional
    public ContractHead buyerSaveOrUpdateContractDTOSecond(ContractDTO contractDTO, String contractStatus) throws ParseException {
        ContractHead contractHead = contractDTO.getContractHead();
        //??????
        List<Annex> annexes = contractDTO.getAnnexes();
        //????????????
        List<PayPlan> payPlans = contractDTO.getPayPlans();
        //????????????
        List<ContractMaterial> contractMaterials = contractDTO.getContractMaterials();
        //????????????
        List<ContractPartner> contractPartners = contractDTO.getContractPartners();
        //????????????
        List<ModelLine> modelLines = contractDTO.getModelLines();

        // ??????????????????
        this.checkContractData(annexes, contractHead, payPlans, contractMaterials);

//        /**
//         * ??????????????????
//         * ???????????????????????????????????????
//         */
//        if (CollectionUtils.isNotEmpty(contractMaterials)) {
//            if (CollectionUtils.isEmpty(contractPartners) && YesOrNo.NO.getValue().equals(contractHead.getIsFrameworkAgreement())) {
//                // ??????????????????
//                ArrayList<ContractPartner> contractPartnerList = new ArrayList<>();
//                contractMaterials.forEach(contractMaterial -> {
//                    // ???????????????????????????????????????
//                    ContractPartner contractPartner = new ContractPartner();
//                    // ????????????ID
//                    Long buId = contractMaterial.getBuId();
//                    // ou?????????
//                    String ceeaOuNumber = contractMaterial.getCeeaOuNumber();
//                    if (StringUtil.notEmpty(buId)) {
//                        ContractPartner contractPartner1 = baseClient.queryContractPartnerByOuId(buId);
//                        BeanCopyUtil.copyProperties(contractPartner, contractPartner1);
//                        contractPartner.setPartnerName(contractMaterial.getBuName());
//                        contractPartner.setPartnerType("??????");
//                        contractPartner.setOuId(buId);
//                        contractPartner.setMaterialId(contractMaterial.getMaterialId());
//                    } else if (StringUtil.notEmpty(ceeaOuNumber)) {
//                        BaseOuDetail baseOuDetail = baseClient.queryBaseOuDetailByCode(ceeaOuNumber);
//                        if (null != baseOuDetail) {
//                            ContractPartner contractPartner1 = baseClient.queryContractPartnerByOuId(baseOuDetail.getOuId());
//                            BeanCopyUtil.copyProperties(contractPartner, contractPartner1);
//                            contractPartner.setPartnerName(baseOuDetail.getOuName());
//                            contractPartner.setPartnerType("??????");
//                            contractPartner.setOuId(baseOuDetail.getOuId());
//                            contractPartner.setMaterialId(contractMaterial.getMaterialId());
//                        }
//                    }
//                    contractPartnerList.add(contractPartner);
//                });
//                contractPartners = new ArrayList<>();
//                contractPartners.addAll(contractPartnerList);
//                ContractPartner contractPartner = new ContractPartner();
//                contractPartner.setPartnerType("??????");
//                contractPartner.setPartnerName(contractHead.getVendorName());
//                contractPartners.add(contractPartner);
//            }
//        }

        // ??????????????????
        Organization organization = baseClient.get(contractHead.getBuId());
        Assert.notNull(organization, "???????????????????????????:" + contractHead.getBuName());
        if (StringUtil.isEmpty(organization.getCeeaCompanyShort()) || StringUtil.isEmpty(organization.getCeeaCompanyCode())) {
            throw new BaseException("?????????????????????????????????????????????????????????");
        }
        //??????????????????????????????
        Assert.notNull(contractHead, "contractHead????????????");
        if (StringUtil.isEmpty(contractHead.getContractHeadId())) {
            long id = IdGenrator.generate();
            contractHead.setContractHeadId(id)
                    .setContractStatus(contractStatus)
                    .setContractNo(baseClient.seqGen(SequenceCodeConstant.SEQ_CONTRACT_NO));
            if (YesOrNo.NO.getValue().equals(contractHead.getCeeaIfVirtual())) {
                // ??????????????????
                String contractCode = null;
                try {
                    contractCode = getContractCode(contractHead, organization);
                } catch (Exception e) {
                    log.error("????????????????????????:" + e);
                    throw new BaseException(e.getMessage());
                }
                contractHead.setContractCode(contractCode);
            }
            this.save(contractHead);
        } else {
            // ??????????????????
            if (StringUtil.isEmpty(contractHead.getContractCode()) && YesOrNo.NO.getValue().equals(contractHead.getCeeaIfVirtual())) {
                // ??????????????????
                String contractCode = null;
                try {
                    contractCode = getContractCode(contractHead, organization);
                } catch (Exception e) {
                    log.error("????????????????????????:" + e);
                    throw new BaseException(e.getMessage());
                }
                contractHead.setContractCode(contractCode);
            }
            contractHead.setContractStatus(contractStatus);
            this.updateById(contractHead);
        }
        //??????????????????????????????
        if (CollectionUtils.isNotEmpty(modelLines)) {
            Long modelHeadId = contractHead.getModelHeadId();
            Assert.notNull(modelHeadId, "modelHeadId????????????");
            for (ModelLine modelLine : modelLines) {
                if (modelLine == null) continue;
                if (modelLine.getModelLineId() == null) {
                    modelLine.setModelHeadId(modelHeadId);
                    modelLine.setModelLineId(IdGenrator.generate());
                    modelLine.setContractHeadId(contractHead.getContractHeadId());
                    modelLine.setContractNo(contractHead.getContractNo());
                    iModelLineService.save(modelLine);
                } else {
                    iModelLineService.updateById(modelLine);
                }
            }
        }

        // ???????????????????????????
        saveOrUpdatePayPlans(contractHead, payPlans);

        // ?????????????????????????????????
        saveOrUpdateContractMaterials(contractHead, contractMaterials);

        // ???????????????????????????
        saveOrUpdateContractPartners(contractHead, contractPartners);

        // ???????????????????????????
        saveOrUpdateContractAnnex(contractHead, annexes);

        return contractHead;
    }

    private void importSave(ContractDTO contractDTO) {
        // ???????????????
        ContractHead contractHead = contractDTO.getContractHead();
        //????????????
        List<PayPlan> payPlans = contractDTO.getPayPlans();
        //????????????
        List<ContractMaterial> contractMaterials = contractDTO.getContractMaterials();
        //????????????
        List<ContractPartner> contractPartners = contractDTO.getContractPartners();

        long id = IdGenrator.generate();
        contractHead.setContractHeadId(id);
        // ?????????????????????????????????
        if (StringUtils.isEmpty(contractHead.getContractNo())) {
            contractHead.setContractNo(baseClient.seqGen(SequenceCodeConstant.SEQ_CONTRACT_NO));
        }
        if (StringUtils.isEmpty(contractHead.getContractCode())) {
            Organization organization = baseClient.get(contractHead.getBuId());
            Assert.notNull(organization, "???????????????????????????:" + contractHead.getBuName());
            if (StringUtil.notEmpty(organization.getCeeaCompanyShort()) && StringUtil.notEmpty(organization.getCeeaCompanyCode())) {
                // ??????????????????
                String contractCode = null;
                try {
                    contractCode = getContractCode(contractHead, organization);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                contractHead.setContractCode(contractCode);
            } else {
                throw new BaseException("?????????????????????????????????????????????????????????");
            }
        }
        this.save(contractHead);
        // ???????????????????????????
        saveOrUpdatePayPlans(contractHead, payPlans);

        // ?????????????????????????????????
        saveOrUpdateContractMaterials(contractHead, contractMaterials);

        // ???????????????????????????
        saveOrUpdateContractPartners(contractHead, contractPartners);
    }

    public String getContractCode(ContractHead contractHead, Organization organization) throws ParseException {
        String code = null;
        if (ContractType.MIAN_CONTRACT_ADD.name().equals(contractHead.getContractType())) {
            StringBuffer prefix = new StringBuffer();
            StringBuffer suffix = new StringBuffer();

            // ??????
            prefix.append("LGi").append("??");
            prefix.append(organization.getCeeaCompanyShort()).append("-");
//            contractCode.append(contractHead.getContractClass()).append("-");
            prefix.append("Pur").append("-");
            String yyyMM = DateUtil.localDateToStr(LocalDate.now(), "yyMM");
            prefix.append(yyyMM).append("-");
            String prefixDtr = prefix.toString();

            // ?????????????????????????????????
            QueryWrapper<ContractHead> wrapper = new QueryWrapper<>();
            Map<String, Date> dateMap = DateUtil.getFirstAndLastDayOfMonth();
            wrapper.between("CREATION_DATE", dateMap.get("startDate"), dateMap.get("endDate"));
            wrapper.eq("CONTRACT_TYPE", ContractType.MIAN_CONTRACT_ADD.name());
            int count = this.count(wrapper) + 1;
            String num = String.format("%03d", count);

            // ??????
            suffix.append("-");
            suffix.append(contractHead.getContractLevel()).append("/").append(organization.getCeeaCompanyCode()).append("-");
            suffix.append("SRM");
            String suffixStr = suffix.toString();

            while (true) {
                try {
                    num = String.format("%03d", count);
                    code = prefixDtr + num + suffixStr;
                    List<ContractHead> contractHeads = this.list(new QueryWrapper<>(new ContractHead().setContractCode(code)));
                    if (CollectionUtils.isEmpty(contractHeads)) {
                        break;
                    } else {
                        count += 1;
                        num = String.format("%03d", count);
                    }
                } catch (Exception e) {
                    break;
                }
            }
        } else if (StringUtil.notEmpty(contractHead.getContractType())) {
            /**
             * LGi??QJ-Pcm-2009-002-LEVEL_01/1300-SRM
             * ??????????????????LGi??X-Pur-1908-159-C/318-SRM???
             * ????????????/???????????????????????????????????????LGi??X-Pur-1908-159/01-C/318-SRM???
             * ?????????/01???159??????????????????02????????????03
             */
            String contractOldCode = contractHead.getContractOldCode();
            int count = this.count(new QueryWrapper<>(new ContractHead().setContractOldCode(contractOldCode))) + 1;
            String num = "/" + String.format("%02d", count);
            code = getModifyContractCode(contractOldCode, num);
            while (true) {
                try {
                    List<ContractHead> contractHeads = this.list(new QueryWrapper<>(new ContractHead().setContractCode(code)));
                    if (CollectionUtils.isEmpty(contractHeads)) {
                        break;
                    } else {
                        num = "/" + String.format("%02d", count + 1);
                        code = getModifyContractCode(contractOldCode, num);
                    }
                } catch (Exception e) {
                    break;
                }
            }
        } else {
            throw new BaseException("??????????????????");
        }
        return code;
    }

    public String getModifyContractCode(String str, String str2) {
        String[] split = StringUtils.split(str, "-");
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < split.length; i++) {
            if (i != 3) {
                stringBuffer.append(split[i]).append("-");
            } else {
                stringBuffer.append(split[i]);
                stringBuffer.append(str2).append("-");
            }
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        return stringBuffer.toString();
    }

    @Transactional
    public void saveOrUpdateContractAnnex(ContractHead contractHead, List<Annex> annexes) {
        iAnnexService.remove(new QueryWrapper<>(new Annex().setContractHeadId(contractHead.getContractHeadId())));
        if (!CollectionUtils.isEmpty(annexes)) {
            annexes.forEach(annex -> {
                if (StringUtil.notEmpty(annex.getFileuploadId())) {
                    annex.setAnnexId(IdGenrator.generate());
                    annex.setContractHeadId(contractHead.getContractHeadId());
                    iAnnexService.save(annex);
                }
            });
        }
    }

    public void checkContractData(List<Annex> annexes, ContractHead contractHead, List<PayPlan> payPlans, List<ContractMaterial> contractMaterials) {
        /**
         * ????????????????????????????????????????????????????????????2?????????????????????
         * ????????????????????????????????????????????????2????????????/????????????????????????????????????
         * ?????????????????????????????????????????????????????????
         */
        String ceeaIsPortableContract = contractHead.getCeeaIsPortableContract();
        BigDecimal includeTaxAmount = contractHead.getIncludeTaxAmount();
        if (YesOrNo.YES.getValue().equals(ceeaIsPortableContract)) {
            if (null != includeTaxAmount && includeTaxAmount.compareTo(new BigDecimal("20000")) > 0) {
                throw new BaseException("????????????????????????????????????????????????????????????2?????????????????????");
            }
        }
        /**
         * ?????????????????????????????????????????????????????????????????????
         */
        Assert.notNull(contractHead.getCeeaIfVirtual(), "?????????????????????????????????");
        if (YesOrNo.YES.getValue().equals(contractHead.getCeeaIfVirtual())) {
            Optional.ofNullable(contractHead.getFrameworkAgreementCode()).ifPresent(s -> contractHead.setContractCode(s));
        }

        // ????????????????????????
        Assert.notNull(contractHead.getContractClass(), "????????????????????????");

        // ????????????
        if (StringUtil.isEmpty(contractHead.getContractHeadId()) && StringUtil.isEmpty(contractHead.getSourceType())) {
            contractHead.setSourceType(ContractSourceType.MANUALLY_CREATED.name());
        }

        // ?????????????????????50??????????????????????????????
        if (CollectionUtils.isNotEmpty(contractMaterials)) {
            List<Long> materialIds = new ArrayList<>();
            contractMaterials.forEach(contractMaterial -> {
                if (StringUtil.notEmpty(contractMaterial.getMaterialId())) {
                    materialIds.add(contractMaterial.getMaterialId());
                }
                /**
                 * 10_OU_????????????,  ????????????????????????
                 */
                if ("10_OU_????????????".equals(contractMaterial.getBuName())) {
                    Assert.notNull(contractHead.getIsHeadquarters(), "????????????????????????");
                }
            });
            if (CollectionUtils.isNotEmpty(materialIds)) {
                boolean flag = baseClient.checkBigClassCodeIsContain50(materialIds);
                Assert.isTrue(!flag, "???????????????????????????50????????????????????????");
            }
        }

        /**
         * ??????????????????????????????100
         */
        if (CollectionUtils.isNotEmpty(payPlans)) {
            BigDecimal sum = BigDecimal.ZERO;
            for (PayPlan payPlan : payPlans) {
                if (StringUtil.notEmpty(payPlan.getPaymentRatio())) {
                    sum = sum.add(payPlan.getPaymentRatio());
                }
            }
            if (sum.compareTo(new BigDecimal(100)) != 0) {
                throw new BaseException("????????????????????????????????????100;");
            }
        }

        // ??????????????????????????????????????????????????????
        if (StringUtil.notEmpty(contractHead.getContractType()) && !ContractType.MIAN_CONTRACT_ADD.name().equals(contractHead.getContractType())) {
            Assert.isTrue(CollectionUtils.isNotEmpty(annexes), "?????????????????????");
            List<Annex> annexList = annexes.stream().filter(annex -> StringUtil.isEmpty(annex.getSourceId())).collect(Collectors.toList());
            Assert.isTrue(CollectionUtils.isNotEmpty(annexList), "?????????????????????");
        }

        /**
         * 1?????????????????????????????????????????????????????????????????????????????????
         * ???1??????????????????????????????????????????A???????????????Y?????????????????????A????????????
         * ???2??????????????????????????????????????????A???????????????N?????????????????????????????????????????????????????????
         *
         * 2?????????????????????????????????????????????????????????????????????????????????
         * ???1??????????????????????????????????????????A???????????????Y?????????????????????A????????????
         * ???2??????????????????????????????????????????A???????????????N?????????????????????A??????
         */
        // ????????????????????????
        if (StringUtil.isEmpty(contractHead.getContractHeadId()) && StringUtil.isEmpty(contractHead.getContractLevel())) {
            Assert.notNull(contractHead.getIsFrameworkAgreement(), "????????????????????????");
            Assert.notNull(contractHead.getVendorId(), "?????????????????????");
            CompanyInfo companyInfo = supplierClient.getCompanyInfo(contractHead.getVendorId());
            Assert.notNull(companyInfo, "????????????????????????");
            if (YesOrNo.YES.getValue().equals(contractHead.getIsFrameworkAgreement())) {
                // ????????????
                if (YesOrNo.YES.getValue().equals(companyInfo.getCompanyLevel())) {
                    // ???????????????A???????????????Y?????????????????????A????????????
                    contractHead.setContractLevel("SA");
                } else {
                    // ???????????????A???????????????N?????????????????????A??????
                    contractHead.setContractLevel("A");
                }
            } else {
                // ???????????????
                if (YesOrNo.YES.getValue().equals(companyInfo.getCompanyLevel())) {
                    // ???????????????A???????????????Y?????????????????????A????????????
                    contractHead.setContractLevel("SA");
                } else {
                    // ???????????????A???????????????N?????????????????????????????????????????????????????????
                    String contractLevel = getContractLevel(contractMaterials);
                    Assert.notNull(contractLevel, "???????????????????????????????????????????????????,???????????????????????????????????????????????????????????????!!!");
                    contractHead.setContractLevel(contractLevel);
                }
            }
        }
        // ?????????????????????????????????????????????
//        if (StringUtil.isEmpty(contractHead.getContractHeadId()) && StringUtil.isEmpty(contractHead.getContractLevel())) {
//            Assert.notNull(contractHead.getIsFrameworkAgreement(), "????????????????????????");
//            if (YesOrNo.NO.getValue().equals(contractHead.getIsFrameworkAgreement())) {
//                String contractLevel = getContractLevel(contractMaterials);
//                Assert.notNull(contractLevel, "???????????????????????????????????????????????????,???????????????????????????????????????????????????????????????!!!");
//                contractHead.setContractLevel(contractLevel);
//            } else {
//                throw new BaseException("????????????????????????");
//            }
//        }

        Assert.notNull(contractHead.getBuId(), "???????????????????????????");
        if (ContractType.MIAN_CONTRACT_ALTER.name().equals(contractHead.getContractType())) {
            StringBuffer errorMsg = new StringBuffer();
            AtomicBoolean flag1 = new AtomicBoolean(true);
            AtomicBoolean flag2 = new AtomicBoolean(true);
            if (CollectionUtils.isNotEmpty(contractMaterials)) {
                HashSet<String> hashSet = new HashSet<>();
                contractMaterials.forEach(contractMaterial -> {
                    if (StringUtil.notEmpty(contractMaterial.getSourceId())) {
                        /**
                         * TODO ????????????????????????????????????
                         * ???????????????????????????????????????
                         * 1?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         */
                    }

                    /**
                     * 1. ????????????+????????????+????????????+????????????+????????????+????????????+????????????+??????????????????
                     */
                    if (StringUtil.notEmpty(contractMaterial.getSourceNumber())) {
                        String onlyKey = String.valueOf(contractMaterial.getSourceNumber()) + contractMaterial.getBuId() + contractMaterial.getInvId() +
                                contractMaterial.getTradingLocations() + contractMaterial.getMaterialCode() +
                                contractMaterial.getStartDate() + contractMaterial.getEndDate();
                        boolean flag = hashSet.add(onlyKey);
                        if (!flag) {
                            flag1.set(false);
                        }
                    } else {
                        // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        String onlyKey = String.valueOf(contractMaterial.getBuId()) + contractMaterial.getInvId() +
                                contractMaterial.getTradingLocations() + contractMaterial.getMaterialCode() +
                                contractMaterial.getStartDate() + contractMaterial.getEndDate();
                        boolean flag = hashSet.add(onlyKey);
                        if (!flag) {
                            flag2.set(false);
                        }
                    }
                });
                Assert.isTrue(flag1.get(), "???????????????: ????????????+????????????+????????????+????????????+????????????+????????????+?????????,????????????");
                Assert.isTrue(flag2.get(), "?????????????????????: ????????????+????????????+????????????+????????????+????????????+?????????,????????????");

                /**
                 * ????????????+????????????+????????????+????????????+????????????+????????????, ?????????????????????
                 */
//                Map<String, List<ContractMaterial>> map = contractMaterials.stream().collect(Collectors.groupingBy(contractMaterial -> {
//                    return contractMaterial.getSourceNumber() + contractMaterial.getBuId() + contractMaterial.getInvId() +
//                            contractMaterial.getTradingLocations() + contractMaterial.getMaterialCode() + contractMaterial.getContractQuantity();
//                }));
//                map.forEach((s, contractMaterials1) -> {
//                    if(CollectionUtils.isNotEmpty(contractMaterials1)){
//                        // ?????????????????????????????????
//                    }
//                });
            }

            if (CollectionUtils.isNotEmpty(payPlans)) {
                BigDecimal sum = BigDecimal.ZERO;
                for (PayPlan payPlan : payPlans) {

                    /**
                     * 2???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                     */
                    // ?????????????????????id
                    Long sourceId = payPlan.getSourceId();
                    if (StringUtil.notEmpty(sourceId)) {
                        CeeaPaymentApplyDTO ceeaPaymentApplyDTO = pmClient.getPaymentApplyByPayPlanId(sourceId);
                        if (null != ceeaPaymentApplyDTO && null != ceeaPaymentApplyDTO.getCeeaPaymentApplyHead()
                                && null != ceeaPaymentApplyDTO.getCeeaPaymentApplyHead().getReceiptStatus()) {
                            String receiptStatus = ceeaPaymentApplyDTO.getCeeaPaymentApplyHead().getReceiptStatus();
                            if (!PaymentStatus.DRAFT.getKey().equals(receiptStatus) && !PaymentStatus.UNDER_APPROVAL.getKey().equals(receiptStatus)) {
                                // ??????????????????????????????????????????????????????
                                List<CeeaPaymentApplyPlan> ceeaPaymentApplyPlans = ceeaPaymentApplyDTO.getCeeaPaymentApplyPlans();
                                if (CollectionUtils.isNotEmpty(ceeaPaymentApplyPlans)) {
                                    ceeaPaymentApplyPlans.forEach(ceeaPaymentApplyPlan -> {
                                        if (sourceId.equals(ceeaPaymentApplyPlan.getPayPlanId())) {
                                            // ??????????????????
                                            BigDecimal paidAmountNoTax = ceeaPaymentApplyPlan.getPaidAmountNoTax();
                                            if (payPlan.getStagePaymentAmount().compareTo(paidAmountNoTax) < 0) {
                                                errorMsg.append("????????????" + payPlan.getPaymentPeriod() + "?????????????????????????????????????????????????????????").append("\n");
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                    // ???????????????????????????????????????????????????
                    sum = sum.add(payPlan.getStagePaymentAmount());
                }
                /**
                 * ?????????????????????????????????????????????????????????????????????????????????
                 */
                if (sum.compareTo(contractHead.getIncludeTaxAmount()) > 0) {
                    errorMsg.append("???????????????????????????????????????????????????;").append("\n");
                }
            }

            if (errorMsg.length() > 0) {
                throw new BaseException(errorMsg.toString());
            }
        }
    }

    @Override
    public ContractDTO getContractDTOSecond(Long contractHeadId, String sourceId) {
        Assert.notNull(contractHeadId, "contractHeadId????????????");
        ContractDTO contractDTO = new ContractDTO();
        ContractHead contractHead = this.getById(contractHeadId);

        boolean flag = false;
        if (StringUtil.notEmpty(sourceId) && "get".equals(sourceId)) {
            flag = true;
        }

        List<ModelLine> modelLines = iModelLineService.list(new QueryWrapper<>(new ModelLine().setContractHeadId(contractHeadId)));
        // ????????????
        List<PayPlan> payPlans = iPayPlanService.list(new QueryWrapper<>(new PayPlan().setContractHeadId(contractHeadId)));
        if (CollectionUtils.isNotEmpty(payPlans) && flag) {
            payPlans.forEach(payPlan -> {
                Long payPlanId = payPlan.getPayPlanId();
                payPlan.setSourceId(payPlanId);
                payPlan.setIsEdit("Y");
                // ??????????????????
                BigDecimal paidAmountNoTax = BigDecimal.ZERO;
                // ?????????????????????????????????, ??????: ??????????????????
                CeeaPaymentApplyDTO ceeaPaymentApplyDTO = pmClient.getPaymentApplyByPayPlanId(payPlanId);
                if (null != ceeaPaymentApplyDTO && null != ceeaPaymentApplyDTO.getCeeaPaymentApplyHead()
                        && null != ceeaPaymentApplyDTO.getCeeaPaymentApplyHead().getReceiptStatus()) {
                    String receiptStatus = ceeaPaymentApplyDTO.getCeeaPaymentApplyHead().getReceiptStatus();
                    if (PaymentStatus.DRAFT.getKey().equals(receiptStatus) || PaymentStatus.UNDER_APPROVAL.getKey().equals(receiptStatus)) {
                        payPlan.setIsEdit("N");
                    } else {
                        // ??????????????????
                        List<CeeaPaymentApplyPlan> ceeaPaymentApplyPlans = ceeaPaymentApplyDTO.getCeeaPaymentApplyPlans();
                        if (CollectionUtils.isNotEmpty(ceeaPaymentApplyPlans)) {
                            for (CeeaPaymentApplyPlan ceeaPaymentApplyPlan : ceeaPaymentApplyPlans) {
                                if (StringUtil.notEmpty(ceeaPaymentApplyPlan.getPayPlanId()) && payPlanId.compareTo(ceeaPaymentApplyPlan.getPayPlanId()) == 0) {
                                    BigDecimal price = ceeaPaymentApplyPlan.getPaidAmountNoTax();
                                    paidAmountNoTax = paidAmountNoTax.add(price);
                                }
                            }
                        }
                    }
                }
                // ??????????????????
                payPlan.setPaidAmount(paidAmountNoTax);
            });
        }
        // ????????????
        List<ContractMaterial> contractMaterials = iContractMaterialService
                .list(new QueryWrapper<>(new ContractMaterial().setContractHeadId(contractHeadId)));
        if (CollectionUtils.isNotEmpty(contractMaterials) && flag) {
            contractMaterials.forEach(contractMaterial -> {
                Long contractMaterialId = contractMaterial.getContractMaterialId();
                contractMaterial.setSourceId(contractMaterialId);
            });
        }

        // ????????????
        List<ContractPartner> contractPartners = iContractPartnerService
                .list(new QueryWrapper<>(new ContractPartner().setContractHeadId(contractHeadId)));
        if (CollectionUtils.isNotEmpty(contractPartners) && flag) {
            contractPartners.forEach(contractPartner -> {
                Long partnerId = contractPartner.getPartnerId();
                contractPartner.setSourceId(partnerId);
            });
        }

        // ??????
        List<Annex> annexes = iAnnexService.list(new QueryWrapper<>(new Annex().setContractHeadId(contractHeadId)));
        if (CollectionUtils.isNotEmpty(annexes) && flag) {
            annexes.forEach(annex -> {
                if (StringUtil.notEmpty(annex.getFileuploadId())) {
                    Long annexId = annex.getAnnexId();
                    annex.setSourceId(annexId);
                }
            });
        }

        // ????????????????????????
        List<CloseAnnex> closeAnnexes = iCloseAnnexService.list(new QueryWrapper<>(new CloseAnnex().setContractHeadId(contractHeadId)));

        // ????????????
        if (CollectionUtils.isNotEmpty(closeAnnexes)) {
            closeAnnexes.forEach(closeAnnex -> {
                Annex annex = new Annex();
                annex.setFileuploadId(closeAnnex.getFileuploadId());
                annex.setFileSourceName(closeAnnex.getFileSourceName());
                annex.setCreatedBy(closeAnnex.getCreatedBy());
                annex.setCreationDate(closeAnnex.getCreationDate());
                annexes.add(annex);
            });
        }

        contractDTO.setContractHead(contractHead)
                .setModelLines(modelLines)
                .setAnnexes(annexes)
                .setPayPlans(payPlans)
                .setContractMaterials(contractMaterials)
                .setContractPartners(contractPartners)
                .setCloseAnnexes(closeAnnexes);
        return contractDTO;
    }

    @Override
    @Transactional
    public void buyerDeleteSecond(Long contractHeadId) {
        this.removeById(contractHeadId);
        iPayPlanService.remove(new QueryWrapper<>(new PayPlan().setContractHeadId(contractHeadId)));
        iContractMaterialService.remove(new QueryWrapper<>(new ContractMaterial().setContractHeadId(contractHeadId)));
        iModelLineService.remove(new QueryWrapper<>(new ModelLine().setContractHeadId(contractHeadId)));
        iAnnexService.remove(new QueryWrapper<>(new Annex().setContractHeadId(contractHeadId)));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                inqClient.resetItemsBelongNumber(Collections.singletonList(contractHeadId));
            }
        });
    }

    private void setMaterialList(ContractHead contractHead, ModelLine modelLine) {
        if (ModelKey.materialList.name().equals(modelLine.getModelKey())) {
            String materialListJsonString = modelLine.getModelValue();
            List<ContractMaterial> contractMaterials = new ArrayList<>();
            try {
                contractMaterials = JSONArray.parseArray(materialListJsonString, ContractMaterial.class);
            } catch (Exception e) {
                log.error("????????????", e);
                throw new BaseException("????????????????????????????????????");
            }
            //?????????????????????????????????
            saveOrUpdateContractMaterials(contractHead, contractMaterials);
            materialListJsonString = JSON.toJSONString(contractMaterials);
            modelLine.setModelValue(materialListJsonString);
        }
    }

    private void setPayPlan(ContractHead contractHead, ModelLine modelLine) {
        if (ModelKey.payPlan.name().equals(modelLine.getModelKey())) {
            String payPlanJsonString = modelLine.getModelValue();
            List<PayPlan> payPlans = new ArrayList<>();
            try {
                payPlans = JSONArray.parseArray(payPlanJsonString, PayPlan.class);
            } catch (Exception e) {
                log.error("????????????", e);
                throw new BaseException("????????????????????????????????????");
            }
            //?????????????????????????????????
            saveOrUpdatePayPlans(contractHead, payPlans);
            payPlanJsonString = JSON.toJSONString(payPlans);
            modelLine.setModelValue(payPlanJsonString);
        }
    }


    //????????????????????????
    private void setUnit(ContractMaterial contractMaterial, String unitCode) {
        if (StringUtils.isNotBlank(unitCode)) {
            List<PurchaseUnit> purchaseUnits = baseClient.listPurchaseUnitByParam(new PurchaseUnit().setUnitCode(unitCode));
            if (!CollectionUtils.isEmpty(purchaseUnits)) {
                PurchaseUnit purchaseUnit = purchaseUnits.get(0);
                contractMaterial.setUnitName(purchaseUnit.getUnitName());
                contractMaterial.setUnitId(purchaseUnit.getUnitId());
                contractMaterial.setUnitCode(unitCode);
            }
        }
    }


    private void setCategory(ContractMaterial contractMaterial, Long categoryId) {
        if (categoryId != null) {
            PurchaseCategory purchaseCategory = baseClient.getPurchaseCategoryByParm(new PurchaseCategory().setCategoryId(categoryId));
            if (purchaseCategory != null) {
                contractMaterial.setCategoryId(categoryId)
                        .setCategoryCode(purchaseCategory.getCategoryCode())
                        .setCategoryName(purchaseCategory.getCategoryName());
            }
        }
    }

    private void vendorCheckBeforeSubmit(ContractHead contractHead) {
        if (!ContractStatus.SUPPLIER_CONFIRMING.name().equals(contractHead.getContractStatus())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????,????????????"));
        }
    }

    private void vendorCheckBeforeReject(ContractHead contractHead) {
        if (!ContractStatus.SUPPLIER_CONFIRMING.name().equals(contractHead.getContractStatus())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????,????????????"));
        }
        Assert.hasText(contractHead.getVendorRejectReason(), LocaleHandler.getLocaleMsg("??????????????????????????????"));
    }

    private void buyerCheckBeforePublish(ContractHead contractHead) {
        if (!ContractStatus.UNPUBLISHED.name().equals(contractHead.getContractStatus())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????,????????????"));
        }
    }

    private void vendorCheckBeforeSaveOrUpdate(ContractHeadDTO contractHeadDTO) {
        if (!ContractStatus.SUPPLIER_CONFIRMING.name().equals(contractHeadDTO.getContractStatus())) {
            throw new BaseException("???????????????????????????,????????????");
        }
    }

    private void buyerCheckBeforeSaveOrUpdate(ContractHead contractHead) {
        Assert.notNull(contractHead, "contractHead????????????");
        if (StringUtils.isNotBlank(contractHead.getContractStatus())
                && (!ContractStatus.DRAFT.name().equals(contractHead.getContractStatus())
                && !ContractStatus.REJECTED.name().equals(contractHead.getContractStatus()))) {
            throw new BaseException("???????????????????????????,????????????");
        }
    }

    private void bindingFileuploads(ContractHead contractHead, List<Fileupload> fileuploads) {
        if (!CollectionUtils.isEmpty(fileuploads)) {
            List<Fileupload> collect = fileuploads.stream().filter(fileupload -> fileupload.getBusinessId() == null)
                    .collect(Collectors.toList());
            fileCenterClient.bindingFileupload(collect, contractHead.getContractHeadId());
        }
    }

    @Transactional
    public void saveOrUpdatePayPlans(ContractHead contractHead, List<PayPlan> payPlans) {
        // ????????????????????????
        iPayPlanService.remove(new QueryWrapper<>(new PayPlan().setContractHeadId(contractHead.getContractHeadId())));
        if (CollectionUtils.isNotEmpty(payPlans)) {
            payPlans.forEach(payPlan -> {
                payPlan.setPayPlanId(IdGenrator.generate());
                payPlan.setContractHeadId(contractHead.getContractHeadId());
            });
            iPayPlanService.saveBatch(payPlans);
        }
    }

    @Transactional
    public void saveOrUpdateContractMaterials(ContractHead contractHead, List<ContractMaterial> contractMaterials) {
        // ???????????????????????????
        iContractMaterialService.remove(new QueryWrapper<>(new ContractMaterial().setContractHeadId(contractHead.getContractHeadId())));

        if (CollectionUtils.isNotEmpty(contractMaterials)) {
            contractMaterials.forEach(contractMaterial -> {
                contractMaterial.setContractHeadId(contractHead.getContractHeadId());
                contractMaterial.setContractMaterialId(IdGenrator.generate());
            });
            iContractMaterialService.saveBatch(contractMaterials);
        }
    }

    @Transactional
    public void saveOrUpdateContractPartners(ContractHead contractHead, List<ContractPartner> contractPartners) {
        // ???????????????????????????
        iContractPartnerService.remove(new QueryWrapper<>(new ContractPartner().setContractHeadId(contractHead.getContractHeadId())));
        if (CollectionUtils.isNotEmpty(contractPartners)) {
            contractPartners.forEach(contractPartner -> {
                contractPartner.setContractHeadId(contractHead.getContractHeadId());
                contractPartner.setPartnerId(IdGenrator.generate());
            });
            iContractPartnerService.saveBatch(contractPartners);
        }
    }

    private void saveOrUpdateContractLines(ContractHead contractHead, List<ContractLine> contractLines) {
        if (!CollectionUtils.isEmpty(contractLines)) {
            contractLines.forEach(contractLine -> {
                if (contractLine.getContractLineId() == null) {
                    contractLine.setContractLineId(IdGenrator.generate())
                            .setContractHeadId(contractHead.getContractHeadId());
                    iContractLineService.save(contractLine);
                } else {
                    iContractLineService.updateById(contractLine);
                }
            });
        }
    }

    private void saveOrUpdateContractHead(ContractHead contractHead, String contractStatus) {
        Assert.notNull(contractHead, "contractHead????????????");
        if (contractHead.getContractHeadId() == null) {
            long id = IdGenrator.generate();
            contractHead.setContractHeadId(id)
                    .setContractStatus(contractStatus)
                    .setContractNo(baseClient.seqGen(SequenceCodeConstant.SEQ_CONTRACT_NO));
            this.save(contractHead);
        } else {
            contractHead.setContractStatus(contractStatus);
            this.updateById(contractHead);
        }
    }

    @Override
    @Transactional
    public void buyerSubmitApprovalSecond(ContractDTO contractDTO) throws ParseException {
    	
    	this.buyerSaveOrUpdateContractDTOSecond(contractDTO, "DRAFT");
    	
        // ?????????????????????????????????
        Assert.notNull(contractDTO.getContractHead(), "??????????????????????????????");
        
//        Assert.notNull(contractDTO.getContractHead().getContractLevel(), "????????????????????????,????????????");
    
        // ???????????????????????????????????????????????????
        List<ContractPartner> partners = contractDTO.getContractPartners();
        if (CollectionUtils.isNotEmpty(partners) && partners.size() >= 2) {
            Set<String> onlyKeys = new HashSet<>();
            partners.forEach(contractPartner -> {
                if ("??????".equals(contractPartner.getPartnerType()) || "??????".equals(contractPartner.getPartnerType())) {
                    onlyKeys.add(contractPartner.getPartnerType());
                }
            });
            Assert.isTrue(onlyKeys.size() == 2, "?????????????????????????????????????????????????????????");
        } else {
            throw new BaseException("?????????????????????????????????????????????????????????");
        }

        /**
         * ??????????????????
         * 1. ????????????????????????
         */
        if (YesOrNo.YES.getValue().equals(contractDTO.getContractHead().getCeeaIfVirtual())) {
            Assert.notNull(contractDTO.getContractHead().getFrameworkAgreementCode(), "?????????????????????!");
        }
        if (!ContractType.MIAN_CONTRACT_ADD.name().equals(contractDTO.getContractHead().getContractType())) {
            Assert.notNull(contractDTO.getContractHead().getCeeaContractOldId(), "?????????ID??????,????????????");
        }
        // ????????????
        if (!YesOrNo.YES.getValue().equals(contractDTO.getContractHead().getCeeaIfVirtual())) {
            buyerSaveOrUpdateContractDTOSecond(contractDTO, ContractStatus.UNDER_REVIEW.name());
        } else {
            // ????????????????????????
            contractDTO.getContractHead().setStartDate(LocalDate.now());
            buyerSaveOrUpdateContractDTOSecond(contractDTO, ContractStatus.ARCHIVED.name());
        }

        // ????????????
        if (ContractType.MIAN_CONTRACT_ALTER.name().equals(contractDTO.getContractHead().getContractType())) {
            // ?????????ID
            Long contractOldId = contractDTO.getContractHead().getCeeaContractOldId();
            ContractHead contract = this.getById(contractOldId);
            Assert.notNull(contract, "??????????????????");
            // ????????????ID
            Long contractHeadId = contract.getContractHeadId();
            /**
             * ?????????????????????????????????, ????????????????????????????????????, ???????????????????????????
             */
            List<ContractMaterial> contractMaterials = contractDTO.getContractMaterials();
            if (CollectionUtils.isNotEmpty(contractMaterials)) {
                contractMaterials.forEach(contractMaterial -> {
                    Long sourceId = contractMaterial.getSourceId();
                    if (StringUtil.notEmpty(sourceId)) {
                        contractMaterial.setContractMaterialId(sourceId);
                        contractMaterial.setContractHeadId(contractHeadId);
                        contractMaterial.setCreatedBy(null);
                        contractMaterial.setCreationDate(null);
                        contractMaterial.setLastUpdatedBy(null);
                        contractMaterial.setSourceId(null);
                        iContractMaterialService.updateById(contractMaterial);
//                        // ?????????????????????, ??????????????????
//                        ContractMaterial contractMaterial1 = iContractMaterialService.getById(sourceId);
//                        if (null != contractMaterial1) {
//                            // ??????????????????
//                            Map<String, Object> objectOld = BeanMapUtils.beanToMap(contractMaterial1);
//                            Map<String, Object> objectNew = BeanMapUtils.beanToMap(contractMaterial);
//                            List<String> keys = Arrays.asList("sourceNumber", "sourceLineNumber", "materialId",
//                                    "specification", "categoryId", "amount", "contractQuantity", "orderQuantity", "untaxedPrice",
//                                    "taxedPrice", "priceUnit", "peoplePrice", "materialPrice", "taxKey", "unitCode",
//                                    "unitId", "deliveryDate", "isAcceptance", "acceptanceQuantity", "startDate", "endDate",
//                                    "currency", "organizationId", "vendorId", "buId", "tradingLocations", "unAmount",
//                                    "manufacturer", "placeOfOrigin", "isInstallDebug", "shelfLife", "lineRemark",
//                                    "itemNumber", "itemName", "taskNumber", "taskName", "shipFrom", "destination");
//
////                                StringBuffer changeField = new StringBuffer();
//                            for (String key : keys) {
//                                if (null != objectOld.get(key) && !objectOld.get(key).equals(objectNew.get(key))) {
//                                    contractMaterial.setChangeField("Y");
//                                    break;
//                                } else if (null != objectNew.get(key)) {
//                                    contractMaterial.setChangeField("Y");
//                                    break;
//                                }
//                                contractMaterial.setChangeField("N");
//                            }
//
//                            // ?????????????????????
//                            contractMaterial.setContractHeadId(contractMaterial1.getContractHeadId());
//                            contractMaterial.setContractMaterialId(contractMaterial1.getContractMaterialId());
////                                String str = changeField.toString();
////                                contractMaterial.setChangeField(StringUtils.left(str, str.length() - 1));
//                            iContractMaterialService.updateById(contractMaterial);
//                        }
                    } else {
                        contractMaterial.setContractHeadId(contractHeadId);
                        contractMaterial.setContractMaterialId(IdGenrator.generate());
                        contractMaterial.setChangeField("Y");
                        iContractMaterialService.save(contractMaterial);
                    }
                });
            }

            /**
             * ????????????????????????????????????????????????????????????????????????????????????
             */
            List<PayPlan> payPlans = contractDTO.getPayPlans();
            if (CollectionUtils.isNotEmpty(payPlans)) {
                payPlans.forEach(payPlan -> {
                    Long sourceId = payPlan.getSourceId();
                    if (StringUtil.notEmpty(sourceId)) {
                        payPlan.setPayPlanId(sourceId);
                        payPlan.setContractHeadId(contractHeadId);
                        payPlan.setCreatedBy(null);
                        payPlan.setCreationDate(null);
                        payPlan.setLastUpdatedBy(null);
                        payPlan.setSourceId(null);
                        iPayPlanService.updateById(payPlan);
//                        // ???????????????????????????, ??????????????????
//                        PayPlan payPlan1 = iPayPlanService.getById(sourceId);
//                        if (null != payPlan1) {
//
//                            // ??????????????????
//                            Map<String, Object> objectOld = BeanMapUtils.beanToMap(payPlan1);
//                            Map<String, Object> objectNew = BeanMapUtils.beanToMap(payPlan);
//                            List<String> keys = Arrays.asList("payTypeId", "payType", "payStage", "payExplain", "payRatio",
//                                    "deductionRatio", "payMethod", "delayedDays", "excludeTaxPayAmount", "taxKey", "payTax",
//                                    "currencyCode", "payDate", "logicalExplain", "payStatus", "paidAmount", "startDate",
//                                    "endDate", "paymentPeriod", "paymentStage", "dateNum", "stagePaymentAmount", "paymentRatio",
//                                    "plannedPaymentDate");
////                                StringBuffer changeField = new StringBuffer();
//                            for (String key : keys) {
//                                if (null != objectOld.get(key) && !objectOld.get(key).equals(objectNew.get(key))) {
//                                    payPlan.setChangeField("Y");
//                                    break;
//                                } else if (null != objectNew.get(key)) {
//                                    payPlan.setChangeField("Y");
//                                    break;
//                                }
//                                payPlan.setChangeField("N");
//                            }
//                            payPlan.setContractHeadId(payPlan1.getContractHeadId());
//                            payPlan.setPayPlanId(payPlan1.getPayPlanId());
////                                String str = changeField.toString();
////                                payPlan.setChangeField(StringUtils.left(str, str.length() - 1));
//                            iPayPlanService.updateById(payPlan);
//                        }
                    } else {
                        payPlan.setContractHeadId(contractHeadId);
                        payPlan.setPayPlanId(IdGenrator.generate());
                        payPlan.setChangeField("Y");
                        iPayPlanService.save(payPlan);
                    }
                });
            }

            /**
             * ????????????????????????????????????????????????????????????????????????????????????
             */
            List<ContractPartner> contractPartners = contractDTO.getContractPartners();
            if (CollectionUtils.isNotEmpty(contractPartners)) {
                contractPartners.forEach(contractPartner -> {
                    Long sourceId = contractPartner.getSourceId();
                    if (StringUtil.notEmpty(sourceId)) {
                        contractPartner.setPartnerId(sourceId);
                        contractPartner.setContractHeadId(contractHeadId);
                        contractPartner.setCreatedBy(null);
                        contractPartner.setCreationDate(null);
                        contractPartner.setLastUpdatedBy(null);
                        contractPartner.setSourceId(null);
                        iContractPartnerService.updateById(contractPartner);
//                        // ???????????????????????????, ??????????????????
//                        ContractPartner contractPartner1 = iContractPartnerService.getById(sourceId);
//                        if (null != contractPartner1) {
//                            // ??????????????????
//                            Map<String, Object> objectOld = BeanMapUtils.beanToMap(contractPartner1);
//                            Map<String, Object> objectNew = BeanMapUtils.beanToMap(contractPartner);
//                            List<String> keys = Arrays.asList("partnerType", "partnerName", "contactName", "phone",
//                                    "address", "bankName", "bankAccount", "postCode", "taxNumber", "startDate",
//                                    "endDate", "enable");
//                            StringBuffer changeField = new StringBuffer();
//                            for (String key : keys) {
//                                if (null != objectOld.get(key) && !objectOld.get(key).equals(objectNew.get(key))) {
//                                    contractPartner.setChangeField("Y");
//                                    break;
//                                } else if (null != objectNew.get(key)) {
//                                    contractPartner.setChangeField("Y");
//                                    break;
//                                }
//                                contractPartner.setChangeField("N");
//                            }
//                            contractPartner.setContractHeadId(contractPartner1.getContractHeadId());
//                            contractPartner.setPartnerId(contractPartner1.getPartnerId());
////                                String str = changeField.toString();
////                                contractPartner.setChangeField(StringUtils.left(str, str.length() - 1));
//                            iContractPartnerService.updateById(contractPartner);
//                        }
                    } else {
                        contractPartner.setContractHeadId(contractHeadId);
                        contractPartner.setPartnerId(IdGenrator.generate());
                        contractPartner.setChangeField("Y");
                        iContractPartnerService.save(contractPartner);
                    }
                });
            }

            /**
             * 2????????????????????????????????????????????????????????????????????????
             */
            List<Annex> annexes = contractDTO.getAnnexes();
            if (CollectionUtils.isNotEmpty(annexes)) {
                annexes.forEach(annex -> {
                    Long sourceId = annex.getSourceId();
                    if (StringUtil.isEmpty(sourceId)) {
                        annex.setContractHeadId(contractHeadId);
                        annex.setAnnexId(IdGenrator.generate());
                        annex.setChangeField("Y");
                        iAnnexService.save(annex);
                    }
                });
            }
        } else if (ContractType.SUPPLEMENTAL_AGREEMENT.name().equals(contractDTO.getContractHead().getContractType())) {
            Long ceeaContractOldId = contractDTO.getContractHead().getCeeaContractOldId();
            Assert.notNull(ceeaContractOldId, "?????????ID????????????");
            ContractHead head = this.getById(ceeaContractOldId);
            Assert.notNull(head, "??????????????????,ceeaContractOldId:" + ceeaContractOldId);
            if (null != head) {
                ContractHead contractHead = head;
                if (null != contractHead) {
                    Long contractHeadId = contractHead.getContractHeadId();
                    /**
                     * 2????????????????????????????????????????????????????????????????????????
                     */
                    List<Annex> annexes = contractDTO.getAnnexes();
                    if (CollectionUtils.isNotEmpty(annexes)) {
                        annexes.forEach(annex -> {
                            Long sourceId = annex.getSourceId();
                            if (StringUtil.isEmpty(sourceId)) {
                                annex.setContractHeadId(contractHeadId);
                                annex.setAnnexId(IdGenrator.generate());
                                iAnnexService.save(annex);
                            }
                        });
                    }
                }
            }
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                /**
                 * ?????????????????????????????????, ???????????????????????????
                 */
                if (!YesOrNo.YES.getValue().equals(contractDTO.getContractHead().getCeeaIfVirtual())) {
                    /**
                     * ?????????????????????????????????????????????
                     */
                    /* Begin by chenwt24@meicloud.com   2020-10-16 */

                    String formId = null;
                    try {
                        formId = contractFlow.submitContractDTOConfFlow(contractDTO);
                    } catch (Exception e) {
                        throw new BaseException(e.getMessage());
                    }
                    if (StringUtils.isEmpty(formId)) {
                        throw new BaseException(LocaleHandler.getLocaleMsg("??????OA????????????"));
                    }
                } else {
                    // ??????????????????????????????, ???????????????
//                    try {
//                        Response response = pushContractInfo(contractDTO.getContractHead().getContractHeadId());
//                        if ("E".equals(response.getEsbInfo().getReturnStatus())) {
//                            log.error("????????????????????????????????????" + response.getEsbInfo().getReturnMsg());
//                            throw new BaseException(response.getEsbInfo().getReturnMsg());
//                        }
//                    } catch (BaseException e) {
//                        throw new BaseException("????????????! " + e.getMessage());
//                    } catch (Exception e) {
//                        log.error("??????????????????", e);
//                        throw new BaseException("????????????! ");
//                    }

                }
            }
        });
    }

    @Override
    @Transactional
    public void cratePriceChangeSource(List<ContractMaterial> contractMaterials) {
        if (CollectionUtils.isNotEmpty(contractMaterials)) {
            Long contractHeadId = contractMaterials.get(0).getContractHeadId();
            ContractHead contractHead = this.getById(contractHeadId);
            String contractCode = contractHead.getContractCode();
            // ??????????????????
            checkItemInfo(contractMaterials);
            // ????????????
            String sourceType = contractMaterials.get(0).getSourceType();
            // ??????????????????
            String bidingNum = null;
            if (SourcingType.TENDER.getItemValue().equals(sourceType)) {
                // ??????
                SourceForm sourceForm = new SourceForm();
                Biding biding = new Biding();
                biding.setSourceFrom(SourceFrom.CONTRACT.getItemValue());
                BidRequirement bidRequirement = new BidRequirement();
                List<BidRequirementLine> bidRequirementLines = new ArrayList<>();
                List<BidVendor> bidVendors = new ArrayList<>();
                sourceForm.setBidding(biding);
                sourceForm.setBidVendors(bidVendors);
                sourceForm.setDemandHeader(bidRequirement);

                contractMaterials.forEach(contractMaterial -> {
                    BidRequirementLine bidRequirementLine = BidRequirementLine.builder()
                            .targetId(contractMaterial.getMaterialId())
                            .targetNum(contractMaterial.getMaterialCode())
                            .targetDesc(contractMaterial.getMaterialName())
                            .categoryId(contractMaterial.getCategoryId())
                            .categoryCode(contractMaterial.getCategoryCode())
                            .categoryName(contractMaterial.getCategoryName())
                            .quantity(contractMaterial.getContractQuantity().doubleValue())
                            .fullPathId(contractMaterial.getBuFullPathId())
                            .orgId(contractMaterial.getBuId())
                            .orgCode(contractMaterial.getBuCode())
                            .orgName(contractMaterial.getBuName())
                            .invId(contractMaterial.getInvId())
                            .invCode(contractMaterial.getInvCode())
                            .invName(contractMaterial.getInvName())
                            .awardedSupplierId(contractMaterial.getVendorId())
                            .awardedSupplierName(contractMaterial.getVendorName())
                            .deliveryPlace(contractMaterial.getTradingLocations())
                            .fromContractId(contractHeadId)
                            .fromContractCode(contractCode)
                            .fromContractLineId(contractMaterial.getContractMaterialId())
                            .ouId(contractMaterial.getCeeaOuId())
                            .ouName(contractMaterial.getCeeaOuName())
                            .ouNumber(contractMaterial.getCeeaOuNumber())
                            .uomCode(contractMaterial.getUnitCode())
                            .uomDesc(contractMaterial.getUnitName())
                            .build();
                    bidRequirementLines.add(bidRequirementLine);
                });
                sourceForm.setDemandLines(bidRequirementLines);
                // ???????????????
                GenerateSourceFormResult generateSourceFormResult = sourceFormClientBid.generateByCm(sourceForm);
                // ??????????????????
                bidingNum = generateSourceFormResult.getSourceForm().getBidding().getBidingNum();
            } else if (SourcingType.RFQ.getItemValue().equals(sourceType)) {
                // ?????????
                com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.vo.SourceForm sourceForm = new com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.vo.SourceForm();
                com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.Biding biding = new com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.Biding();
                biding.setSourceFrom(SourceFrom.CONTRACT.getItemValue());
                com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidRequirement demandHeader = new com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidRequirement();
                ArrayList<com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine> demandLines = new ArrayList<>();
                ArrayList<com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidVendor> bidVendors = new ArrayList<>();
                sourceForm.setBidding(biding);
                sourceForm.setBidVendors(bidVendors);
                sourceForm.setDemandHeader(demandHeader);

                contractMaterials.forEach(contractMaterial -> {
                    com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine bidRequirementLine = com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidRequirementLine.builder()
                            .targetId(contractMaterial.getMaterialId())
                            .targetNum(contractMaterial.getMaterialCode())
                            .targetDesc(contractMaterial.getMaterialName())
                            .categoryId(contractMaterial.getCategoryId())
                            .categoryCode(contractMaterial.getCategoryCode())
                            .categoryName(contractMaterial.getCategoryName())
                            .quantity(contractMaterial.getContractQuantity().doubleValue())
                            .fullPathId(contractMaterial.getBuFullPathId())
                            .orgId(contractMaterial.getBuId())
                            .orgCode(contractMaterial.getBuCode())
                            .orgName(contractMaterial.getBuName())
                            .invId(contractMaterial.getInvId())
                            .invCode(contractMaterial.getInvCode())
                            .invName(contractMaterial.getInvName())
                            .awardedSupplierId(contractMaterial.getVendorId())
                            .awardedSupplierName(contractMaterial.getVendorName())
                            .deliveryPlace(contractMaterial.getTradingLocations())
                            .fromContractId(contractHeadId)
                            .fromContractCode(contractCode)
                            .fromContractLineId(contractMaterial.getContractMaterialId())
                            .ouId(contractMaterial.getCeeaOuId())
                            .ouName(contractMaterial.getCeeaOuName())
                            .ouNumber(contractMaterial.getCeeaOuNumber())
                            .uomCode(contractMaterial.getUnitCode())
                            .uomDesc(contractMaterial.getUnitName())
                            .build();
                    demandLines.add(bidRequirementLine);
                });
                sourceForm.setDemandLines(demandLines);
                com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.vo.GenerateSourceFormResult generateSourceFormResult = sourceFormClientBargain.generateByCm(sourceForm);
                bidingNum = generateSourceFormResult.getSourceForm().getBidding().getBidingNum();
            }
            String finalBidingNum = bidingNum;
            contractMaterials.forEach(contractMaterial -> {
                UpdateWrapper<ContractMaterial> updateWrapper = new UpdateWrapper<>();
                updateWrapper.set("CEEA_FOLLOW_SOURCE_NUM", finalBidingNum);
                updateWrapper.eq("CONTRACT_MATERIAL_ID", contractMaterial.getContractMaterialId());
                iContractMaterialService.update(updateWrapper);
            });
        }
    }

    /**
     * 1. ???????????????????????????, ????????????????????????????????????
     * 2. ?????????????????????????????????
     */
    private void checkItemInfo(List<ContractMaterial> contractMaterials) {
        contractMaterials.forEach(contractMaterial -> {
            Assert.notNull(contractMaterial.getSourceNumber(), "????????????????????????????????????");
            Assert.notNull(contractMaterial.getSourceType(), "????????????????????????????????????");

        });
        String sourceNumber = contractMaterials.get(0).getSourceNumber();
        contractMaterials.forEach(contractMaterial -> {
            Assert.isTrue(sourceNumber.equals(contractMaterial.getSourceNumber()), "???????????????????????????????????????");
        });
    }

    @Override
    @Transactional
    public void priceApprovalWriteBackContract(List<ApprovalBiddingItem> approvalBiddingItemList) {
        if (CollectionUtils.isNotEmpty(approvalBiddingItemList)) {
            ArrayList<ContractMaterial> contractMaterials = new ArrayList<>();
            approvalBiddingItemList.forEach(approvalBiddingItem -> {
                Long fromContractLineId = approvalBiddingItem.getFromContractLineId();
                ContractMaterial contractMaterial = iContractMaterialService.getById(fromContractLineId);
                contractMaterial.setTaxedPrice(approvalBiddingItem.getTaxPrice()); // ????????????
                if (null != contractMaterial.getContractQuantity() && null != contractMaterial.getTaxedPrice()) {
                    contractMaterial.setAmount(contractMaterial.getContractQuantity().multiply(contractMaterial.getTaxedPrice()));
                }
                if (null != contractMaterial.getAmount() &&
                        contractMaterial.getAmount().compareTo(BigDecimal.ZERO) != 0 &&
                        null != contractMaterial.getTaxRate() &&
                        contractMaterial.getTaxRate().compareTo(BigDecimal.ZERO) != 0
                ) {
                    double taxRate = contractMaterial.getTaxRate().doubleValue();
                    double amount = contractMaterial.getAmount().doubleValue();
                    double unAmount = amount / (taxRate / 100 + 1);
                    contractMaterial.setUnAmount(new BigDecimal(unAmount));
                }
                contractMaterials.add(contractMaterial);
            });
            // ?????????????????????
            iContractMaterialService.updateBatchById(contractMaterials);
            // ?????????????????????
            this.baseMapper.updateContractAmount(contractMaterials.get(0).getContractHeadId());
        }

    }

    @Override
    public ContractHead getContractHeadByParam(ContractHeadDTO contractHeadDTO) {
        ContractHead contractHead = new ContractHead();
        BeanUtils.copyProperties(contractHeadDTO, contractHead);
        return this.getOne(new QueryWrapper<>(contractHead));
    }

    /**
     * ?????????????????????????????????
     * ????????????: ????????????????????????????????????
     *
     * @param contractHeadId
     */
    public void checkOperationAuthority(Long contractHeadId) {
        ContractHead contractHead = this.getById(contractHeadId);
        Assert.notNull(contractHead, "??????????????????:contractHeadId = " + contractHeadId);
        String userName = AppUserUtil.getUserName();
        Assert.isTrue(userName.equals(contractHead.getCreatedBy()), "?????????????????????????????????!!!");
    }

    @Override
    public List<PriceLibraryContractResDTO> getOnShelvesContractList(PriceLibraryContractRequestDTO priceLibraryContractRequestDTO) {
        Map<String, Object> param = new HashMap<>();
        param.put("priceLibraryList", priceLibraryContractRequestDTO.getPriceLibraryList());
        param.put("contractCode", priceLibraryContractRequestDTO.getContractNo());
        param.put("contractName", priceLibraryContractRequestDTO.getContractName());
        param.put("vendorName", priceLibraryContractRequestDTO.getVendorName());
        param.put("orgName", priceLibraryContractRequestDTO.getCeeaOrgName());
        param.put("OrganizationName", priceLibraryContractRequestDTO.getCeeaOrganizationName());
        List<PriceLibraryContractResDTO> priceLibraryContractResList = this.baseMapper.getOnShelvesContractList(param);
        return priceLibraryContractResList;
    }

    @Override
    public AdvanceApplyHeadVo advanceCheckContract(ContractHeadDTO contractHeadDTO) {
        List<ContractPartner> contractPartners = iContractPartnerService.list(new QueryWrapper<>(new ContractPartner().setContractHeadId(contractHeadDTO.getContractHeadId())));
        List<PayPlan> payPlans = iPayPlanService.list(Wrappers.lambdaQuery(PayPlan.class)
                .select(PayPlan::getPaymentStage)
                .eq(PayPlan::getContractHeadId, contractHeadDTO.getContractHeadId()));
        List<String> paymentStages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(payPlans)) {
            paymentStages = payPlans.stream().map(payPlan -> (payPlan.getPaymentStage())).collect(Collectors.toList());
        }
        ContractHead contractHead = this.getById(contractHeadDTO.getContractHeadId());
        List<Long> collect = contractPartners.stream().filter(Objects::nonNull).filter(contractPartner -> ("??????".equals(contractPartner.getPartnerType()))).map(contractPartner -> (contractPartner.getOuId())).collect(Collectors.toList());
        Map<Long, Organization> longOrganizationMap = new HashMap<>();
        Map<Long, List<DeptDto>> longDeptDtoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(collect)) {
            longOrganizationMap = batchSelectOrganization(collect, longOrganizationMap);
            longDeptDtoMap = baseClient.getAllDeptByOrganizations(collect);
        }
        if (CollectionUtils.isNotEmpty(contractPartners)) {
            for (ContractPartner contractPartner : contractPartners) {
                if (contractPartner == null) continue;
                String partnerType = contractPartner.getPartnerType();
                if ("??????".equals(partnerType)) {
                    Long ouId = contractPartner.getOuId();
                    Organization organization = longOrganizationMap.get(ouId);
                    String ceeaBusinessCode = organization.getCeeaBusinessCode();
                    String partnerName = contractPartner.getPartnerName();
                    List<DeptDto> deptDtos = longDeptDtoMap.get(ouId);
                    AdvanceApplyHeadVo advanceApplyHeadVo = new AdvanceApplyHeadVo();
                    advanceApplyHeadVo.setIfPayAgent(YesOrNo.YES.getValue()).setPayAgentOrgId(ouId).setPayAgentOrgName(partnerName).setPayAgentOrgCode(ceeaBusinessCode);//????????????????????????
                    advanceApplyHeadVo.setDeptDtos(deptDtos);//??????????????????(????????????,??????????????????)
                    advanceApplyHeadVo.setPaymentStages(paymentStages);//????????????????????????
                    setBaseByContract(contractHead, advanceApplyHeadVo);//????????????????????????????????????
                    return advanceApplyHeadVo;
                }
            }
        }
        AdvanceApplyHeadVo advanceApplyHeadVo = new AdvanceApplyHeadVo();
        advanceApplyHeadVo.setIfPayAgent(YesOrNo.NO.getValue());
        advanceApplyHeadVo.setPaymentStages(paymentStages);//????????????????????????
        setBaseByContract(contractHead, advanceApplyHeadVo);//????????????????????????????????????
        return advanceApplyHeadVo;
    }

    private Map<Long, Organization> batchSelectOrganization(List<Long> organizationIds, Map<Long, Organization> organizationMap) {
        List<Organization> organizations = baseClient.getOrganizationsByIds(organizationIds);
        if (CollectionUtils.isNotEmpty(organizations)) {
            organizationMap = organizations.stream().collect(Collectors.toMap(Organization::getOrganizationId, Function.identity()));
        }
        return organizationMap;
    }

    @Override
    public PageInfo<ContractHead> listPageEffectiveByParam(ContractHeadDTO contractHeadDTO) {
        PageUtil.startPage(contractHeadDTO.getPageNum(), contractHeadDTO.getPageSize());
        List<ContractHead> contractHeads = this.baseMapper.listPageEffectiveByParam(contractHeadDTO);
        return new PageInfo<>(contractHeads);
    }


    @Override
    public void importModelDownload(HttpServletResponse response) throws Exception {
        ArrayList<ContractHeadModelDTO> contractHeadModelDTOs = new ArrayList<>();
        ArrayList<ContractMaterialModelDTO> contractMaterialModelDTOs = new ArrayList<>();
        ArrayList<PayPlanModelDTO> payPlanModelDTOs = new ArrayList<>();
        ArrayList<ContractPartnerModelDTO> contractPartnerModelDTOs = new ArrayList<>();
        String[] sheetNames = {"??????????????????", "?????????", "????????????", "????????????", "????????????"};
        List<List<? extends Object>> dataLists = new ArrayList<>();
        dataLists.add(contractHeadModelDTOs);
        dataLists.add(contractMaterialModelDTOs);
        dataLists.add(payPlanModelDTOs);
        dataLists.add(contractPartnerModelDTOs);
        Class<? extends Object>[] clazz = new Class[]{ContractHeadModelDTO.class, ContractMaterialModelDTO.class, PayPlanModelDTO.class, ContractPartnerModelDTO.class};
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, sheetNames[0]);
        // ???sheet excel
        EasyExcelUtil.writeExcelWithModel(outputStream, sheetNames, dataLists, clazz);
    }

    /**
     * ??????????????????
     * @param response
     * @throws Exception
     */
    @Override
    public void importContractMaterialDownload(List<Long>  contractHeadIds,HttpServletResponse response) throws Exception {
        // ??????
        ContractHeadVO contractHeadVO = new ContractHeadVO();
        Field[] declaredFields = contractHeadVO.getClass().getDeclaredFields();
        ArrayList<String> head = new ArrayList<>();
        ArrayList<String> headName = new ArrayList<>();
        for (Field field : declaredFields) {
            head.add(field.getName());
            ExcelProperty annotation = field.getAnnotation(ExcelProperty.class);
            if (null != annotation) {
                headName.add(annotation.value()[0]);
            }
        }
        // ?????????????????????
        List<List<Object>> dataList = this.queryExportData(contractHeadIds, head);
        // ?????????
        String fileName = "???????????????";
        // ????????????
        EasyExcelUtil.exportStart(response, dataList, headName, fileName);
    }



    // ?????????????????????
    public List<List<Object>> queryExportData(List<Long>  contractHeadIds, List<String> param) {
        //???????????????????????????
        //???????????????????????????
        //????????????????????????????????????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (null==loginAppUser) {
            return new ArrayList<>();
        }
        QueryWrapper<ContractHead> wrapper = new QueryWrapper<>();
        wrapper.in(CollectionUtils.isNotEmpty(contractHeadIds), "a.CONTRACT_HEAD_ID", contractHeadIds);
        wrapper.eq("a.CREATED_ID",loginAppUser.getUserId());
        List<ContractHeadVO> contractHeadVOList = contractHeadMapper.getContractHeadVOList(wrapper);

        List<List<Object>> dataList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(contractHeadVOList)) {
            List<Map<String, Object>> mapList = BeanMapUtils.objectsToMaps(contractHeadVOList);
            List<String> titleList = param;
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(titleList)) {
                for (Map<String, Object> map : mapList) {
                    ArrayList<Object> objects = new ArrayList<>();
                    for (String key : titleList) {
                        objects.add(map.get(key));
                    }
                    dataList.add(objects);
                }
            }
        }
        return dataList;
    }

    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file, Fileupload fileupload) throws Exception {
        // ????????????
        EasyExcelUtil.checkParam(file, fileupload);

        HashMap<String, Object> result = new HashMap<>();
        // ???????????????
        InputStream inputStream = file.getInputStream();

        ExcelReader excelReader = EasyExcel.read(inputStream).build();

        AnalysisEventListenerImpl<Object> sheet1Listener = new AnalysisEventListenerImpl();

        AnalysisEventListenerImpl<Object> sheet2Listener = new AnalysisEventListenerImpl();

        AnalysisEventListenerImpl<Object> sheet3Listener = new AnalysisEventListenerImpl();

        AnalysisEventListenerImpl<Object> sheet4Listener = new AnalysisEventListenerImpl();

        ReadSheet sheet1 =
                EasyExcel.readSheet(0).head(ContractHeadModelDTO.class).registerReadListener(sheet1Listener).build();

        ReadSheet sheet2 =
                EasyExcel.readSheet(1).head(ContractMaterialModelDTO.class).registerReadListener(sheet2Listener).build();

        ReadSheet sheet3 =
                EasyExcel.readSheet(2).head(PayPlanModelDTO.class).registerReadListener(sheet3Listener).build();

        ReadSheet sheet4 =
                EasyExcel.readSheet(3).head(ContractPartnerModelDTO.class).registerReadListener(sheet4Listener).build();

        excelReader.read(sheet1, sheet2, sheet3, sheet4);

        // ?????????
        List<Object> sheet1s = sheet1Listener.getDatas();
        // ????????????
        List<Object> sheet2s = sheet2Listener.getDatas();
        // ????????????
        List<Object> sheet3s = sheet3Listener.getDatas();
        // ????????????
        List<Object> sheet4s = sheet4Listener.getDatas();

        // ?????????bean
        ArrayList<ContractHead> contractHeads = new ArrayList<>();
        // ??????bean
        ArrayList<ContractHeadModelDTO> contractHeadModelDTOs = new ArrayList<>();
        ArrayList<ContractMaterialModelDTO> contractMaterialModelDTOs = new ArrayList<>();
        ArrayList<PayPlanModelDTO> payPlanModelDTOs = new ArrayList<>();
        ArrayList<ContractPartnerModelDTO> contractPartnerModelDTOs = new ArrayList<>();
        // ??????list
        List<List<ContractMaterial>> contractMaterialGroups = new ArrayList<>();
        List<List<PayPlan>> payPlanGroups = new ArrayList<>();
        List<List<ContractPartner>> contractPartnerGroups = new ArrayList<>();

        // true ?????????????????????????????? false?????????????????????
        Boolean flag = true;
        // ???????????????
        if (CollectionUtils.isNotEmpty(sheet1s)) {
            for (Object contractHeadModel : sheet1s) {
                HashSet only = new HashSet();
                List<ContractMaterial> contractMaterials = new ArrayList<>();
                List<PayPlan> payPlans = new ArrayList<>();
                List<ContractPartner> contractPartners = new ArrayList<>();
                if (null != contractHeadModel) {
                    StringBuffer contractHeadErrorMessage = new StringBuffer();
                    ContractHeadModelDTO contractHeadModelDTO = (ContractHeadModelDTO) contractHeadModel;
                    ContractHead contractHead = new ContractHead();
                    checkContractHeadParam(contractHeadModelDTO, contractHead, contractHeadErrorMessage);
                    if (StringUtils.isNotEmpty(contractHeadModelDTO.getContractNo())) {
                        flag = true;
                    } else if (StringUtils.isNotEmpty(contractHeadModelDTO.getContractName())) {
                        flag = false;
                    } else {
                        throw new BaseException("???????????????????????????????????????????????????????????????");
                    }

                    /*// ??????????????????  ??????????????????????????????????????????
                    if (StringUtils.isNotEmpty(contractHead.getIsFrameworkAgreement()) && StringUtils.isEmpty(contractHeadModelDTO.getErrorMessage())) {
                        if (contractHead.getIsFrameworkAgreement().equals(YesOrNo.NO.getValue())) {
                            ContractPartner contractPartner = new ContractPartner();
                            contractPartner.setPartnerType(ContractHeadConst.PARTY_B);
                            contractPartner.setPartnerName(contractHead.getVendorName());
                            contractPartners.add(contractPartner);
                        }
                    }*/

                    // ??????????????????????????????
                    List<Object> contractMaterialCollect = new ArrayList<>();
                    if (flag) {
                        contractMaterialCollect = sheet2s.stream().filter(s -> contractHeadModelDTO.getContractNo().equals(((ContractMaterialModelDTO) s).getContractNo())).collect(Collectors.toList());
                    } else {
                        contractMaterialCollect = sheet2s.stream().filter(s -> contractHeadModelDTO.getContractName().equals(((ContractMaterialModelDTO) s).getContractName())).collect(Collectors.toList());
                    }
                    if (CollectionUtils.isNotEmpty(contractMaterialCollect)) {
                        BigDecimal amount = new BigDecimal(0);
                        for (Object object1 : contractMaterialCollect) {
                            if (null != object1) {
                                ContractMaterialModelDTO contractMaterialModelDTO = (ContractMaterialModelDTO) object1;
                                ContractMaterial contractMaterial = new ContractMaterial();
                                StringBuffer contractMaterialErrorMessage = new StringBuffer();
                                checkContractMaterialParam(contractMaterialModelDTO, contractMaterial, contractMaterialErrorMessage, contractHead);
                                if (StringUtils.isEmpty(contractMaterialModelDTO.getErrorMessage())) {
                                    // ????????????
                                    if (StringUtils.isNotEmpty(contractMaterialModelDTO.getContractQuantity())) {
                                        BigDecimal number = contractMaterial.getContractQuantity().multiply(contractMaterial.getTaxedPrice());
                                        contractMaterial.setAmount(number);
                                        // ????????????
                                        BigDecimal tax = (contractMaterial.getTaxRate().divide(new BigDecimal(100))).add(new BigDecimal(1));
                                        BigDecimal unAmount = number.divide(tax, 2, RoundingMode.HALF_UP);
                                        contractMaterial.setUnAmount(unAmount);
                                        // ??????????????????
                                        if (StringUtils.isEmpty(contractHeadModelDTO.getIncludeTaxAmount())) {
                                            amount = amount.add(number);
                                        }
                                    }
                                }

                                   /* // ??????????????????  ???????????????????????????????????????????????????
                                    if (StringUtils.isNotEmpty(contractHead.getIsFrameworkAgreement()) && StringUtils.isEmpty(contractMaterialModelDTO.getErrorMessage())) {
                                        if (contractHead.getIsFrameworkAgreement().equals(YesOrNo.NO.getValue())) {
                                            List<Organization> organizationList = baseClient.getOrganizationByNameList(Arrays.asList(contractMaterial.getBuName()));
                                            if (CollectionUtils.isNotEmpty(organizationList)) {
                                                Organization organization = organizationList.get(0);
                                                ContractPartner contractPartner = new ContractPartner();
                                                contractPartner.setPartnerName(organization.getCeeaCompanyName());
                                                contractPartner.setPartnerType(ContractHeadConst.PARTY_A);
                                                contractPartner.setOuId(organization.getOrganizationId());
                                                contractPartner.setMaterialId(contractMaterial.getMaterialId());
                                                // ????????????????????????????????????
                                                List<ContractPartner> list = new ArrayList<ContractPartner>();
                                                for (ContractPartner partner : contractPartners) {
                                                    if (!contractPartner.getPartnerName().equals(partner.getPartnerName())
                                                            && ContractHeadConst.PARTY_A.equals(partner.getPartnerType())) {
                                                        list.add(contractPartner);
                                                    }
                                                }
                                                contractPartners.addAll(list);
                                            }
                                        }
                                    }*/

                                contractMaterialModelDTOs.add(contractMaterialModelDTO);
                                contractMaterials.add(contractMaterial);
                            }
                        }
                        contractHead.setIncludeTaxAmount(amount);
                    } else {
                        contractHeadModelDTO.setErrorMessage(contractHeadModelDTO.getErrorMessage() + "?????????????????????????????????????????????");
                    }
                    // ??????????????????????????????
                    List<Object> payPlanCollect = new ArrayList<>();
                    if (flag) {
                        payPlanCollect = sheet3s.stream().filter(s -> contractHeadModelDTO.getContractNo().equals(((PayPlanModelDTO) s).getContractNo())).collect(Collectors.toList());
                    } else {
                        payPlanCollect = sheet3s.stream().filter(s -> contractHeadModelDTO.getContractName().equals(((PayPlanModelDTO) s).getContractName())).collect(Collectors.toList());
                    }
                    if (CollectionUtils.isNotEmpty(payPlanCollect)) {
                        BigDecimal paymentRatioSum = new BigDecimal(0);
                        BigDecimal includeTaxAmount = contractHead.getIncludeTaxAmount();
                        for (Object object2 : payPlanCollect) {
                            if (null != object2) {
                                PayPlanModelDTO payPlanModelDTO = (PayPlanModelDTO) object2;
                                StringBuffer payPlanErrorMessage = new StringBuffer();
                                PayPlan payPlan = new PayPlan();
                                checkPayPlanParam(payPlanModelDTO, payPlan, payPlanErrorMessage, includeTaxAmount, only);
                                // ??????????????????????????????????????????100
                                if (StringUtils.isEmpty(payPlanModelDTO.getPaymentRatio())) {
                                    paymentRatioSum = paymentRatioSum.add(payPlan.getPaymentRatio());
                                    if (paymentRatioSum.compareTo(new BigDecimal("100")) > 0) {
                                        String errorMessage = payPlanModelDTO.getErrorMessage();
                                        if (StringUtil.notEmpty(errorMessage)) {
                                            payPlanModelDTO.setErrorMessage(errorMessage + " ???????????????????????????????????????100; ");
                                        } else {
                                            payPlanModelDTO.setErrorMessage("???????????????????????????????????????100; ");
                                        }
                                    }
                                }
                                payPlanModelDTOs.add(payPlanModelDTO);
                                payPlan.setPaymentPeriod(payPlans.size() + 1 + "");
                                payPlans.add(payPlan);
                            }
                        }
                    }
//                        else {
//                            contractHeadModelDTO.setErrorMessage(contractHeadModelDTO.getErrorMessage()+"???????????????????????????????????????????????????");
//                        }
                    // ??????????????????????????????
                    List<Object> contractPartnerCollect = new ArrayList<>();
                    if (flag) {
                        contractPartnerCollect = sheet4s.stream().filter(s -> contractHeadModelDTO.getContractNo().equals(((ContractPartnerModelDTO) s).getContractNo())).collect(Collectors.toList());
                    } else {
                        contractPartnerCollect = sheet4s.stream().filter(s -> contractHeadModelDTO.getContractName().equals(((ContractPartnerModelDTO) s).getContractName())).collect(Collectors.toList());
                    }
                    if (CollectionUtils.isNotEmpty(contractPartnerCollect)) {
                        contractPartnerCollect.forEach((object3 -> {
                            if (null != object3) {
                                ContractPartnerModelDTO contractPartnerModelDTO = (ContractPartnerModelDTO) object3;
                                StringBuffer contractPartnerErrorMessage = new StringBuffer();
                                ContractPartner contractPartner = new ContractPartner();
                                checkContractPartnerParam(contractPartnerModelDTO, contractPartner, contractPartnerErrorMessage, contractHeadModelDTO);
                                contractPartnerModelDTOs.add(contractPartnerModelDTO);
                                contractPartners.add(contractPartner);
                            }
                        }));
                    } else {
                        contractHeadModelDTO.setErrorMessage(contractHeadModelDTO.getErrorMessage() + "?????????????????????????????????????????????");
                    }
                    // ????????????????????????????????????
                    contractHeads.add(contractHead);
                    contractHeadModelDTOs.add(contractHeadModelDTO);
                    contractMaterialGroups.add(contractMaterials);
                    payPlanGroups.add(payPlans);
                    if (CollectionUtils.isNotEmpty(contractPartners)) {
                        // ???????????????????????? + ou ??????
                        List<ContractPartner> contractPartnerList = ListUtil.listDeduplication(contractPartners, contractPartner -> contractPartner.getPartnerType() + contractPartner.getOuId());
                    }
                    contractPartnerGroups.add(contractPartners);
                }
            }
        }
        // ????????????sheet???????????????????????????
        Boolean contractHeadFlag = false;
        Boolean contractMaterialFlag = false;
        Boolean payPlanFlag = false;
        Boolean contractPartnerFlag = false;
        contractHeadFlag = isHasErrorMessage(contractHeadModelDTOs, contractHeadFlag, ContractHeadModelDTO.class, ContractHeadConst.ERROR_MESSAGE);
        contractMaterialFlag = isHasErrorMessage(contractMaterialModelDTOs, contractMaterialFlag, ContractMaterialModelDTO.class, ContractHeadConst.ERROR_MESSAGE);
        payPlanFlag = isHasErrorMessage(payPlanModelDTOs, payPlanFlag, PayPlanModelDTO.class, ContractHeadConst.ERROR_MESSAGE);
        contractPartnerFlag = isHasErrorMessage(contractPartnerModelDTOs, contractPartnerFlag, ContractPartnerModelDTO.class, ContractHeadConst.ERROR_MESSAGE);

        // ??????????????? ????????????
        if (contractHeadFlag || contractMaterialFlag || payPlanFlag || contractPartnerFlag) {
            String[] sheetNames = {file.getName(), "?????????", "????????????", "????????????", "????????????"};
            List<List<? extends Object>> dataLists = new ArrayList<>();
            dataLists.add(contractHeadModelDTOs);
            dataLists.add(contractMaterialModelDTOs);
            dataLists.add(payPlanModelDTOs);
            dataLists.add(contractPartnerModelDTOs);
            Class<? extends Object>[] clazz = new Class[]{ContractHeadModelDTO.class, ContractMaterialModelDTO.class, PayPlanModelDTO.class, ContractPartnerModelDTO.class};
            Fileupload errorFileUpload = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    dataLists, clazz, sheetNames, file.getOriginalFilename(), file.getContentType());
            result.put("status", YesOrNo.NO.getValue());
            result.put("message", "error");
            result.put("fileuploadId", errorFileUpload.getFileuploadId());
            result.put("fileName", errorFileUpload.getFileSourceName());
        } else { // ???????????????
            for (int i = 0; i < contractHeads.size(); i++) {
                importSave(new ContractDTO().setContractHead(contractHeads.get(i))
                        .setContractMaterials(i <= contractMaterialGroups.size() - 1 ? contractMaterialGroups.get(i) : null)
                        .setContractPartners(i <= contractPartnerGroups.size() - 1 ? contractPartnerGroups.get(i) : null)
                        .setPayPlans(i <= payPlanGroups.size() - 1 ? payPlanGroups.get(i) : null)
                );
            }
            result.put("status", YesOrNo.YES.getValue());
            result.put("message", "success");
        }

        return result;
    }

    @Override
    public Response pushContractInfo(Long contractHeadId) {
        Response contractResponse = new Response();
        ContractHead contractHead = null;
        List<ContractMaterial> contractMaterials = new ArrayList<>();
        List<PayPlan> payPlans = new ArrayList<>();
        try {
            // ????????????
            JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
            jaxWsProxyFactoryBean.getInInterceptors().add(new LoggingInInterceptor());
            jaxWsProxyFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor());
            // ??????????????????
            /*String contractUrl = "http://soatest.longi.com:8011/CMSSB/Esb/OeBlanket/ProxyServices/CmsSendOeBlanketSoapProxy?wsdl";
            String contractUserName = "longi_xansrm01";
            String contractPassword = "932f8f9cfa6d4bcd8fa8478f697cb227";*/
            jaxWsProxyFactoryBean.setAddress(CmSaopUrl.contractUrl);
            jaxWsProxyFactoryBean.setUsername(this.contractUserName);
            jaxWsProxyFactoryBean.setPassword(this.contractPassword);
            // ??????????????????
            jaxWsProxyFactoryBean.setServiceClass(ContractExecutePtt.class);

            // ??????????????????????????????
            ContractExecutePtt service = (ContractExecutePtt) jaxWsProxyFactoryBean.create();
            Request contractRequest = new Request();

            /**??????EsbInfo*/
            Request.EsbInfo esbInfo = new Request.EsbInfo();
            esbInfo.setInstId(String.valueOf(System.currentTimeMillis()));
            esbInfo.setRequestTime(String.valueOf(System.currentTimeMillis()));
            esbInfo.setAttr1("");
            esbInfo.setAttr2("");
            esbInfo.setAttr3("");
            esbInfo.setSysCode(ContractHeadConst.SYS_CODE);
            contractRequest.setEsbInfo(esbInfo);

            /**??????RequestInfo*/
            Request.RequestInfo requestInfo = new Request.RequestInfo();

            Request.RequestInfo.CmsSiCtHeaders header = new Request.RequestInfo.CmsSiCtHeaders();

            Request.RequestInfo.CmsSiCtHeaders.Items items = new Request.RequestInfo.CmsSiCtHeaders.Items();
            // ????????????
            Request.RequestInfo.CmsSiCtHeaders.CmsPeRtnpayItems listPay = new Request.RequestInfo.CmsSiCtHeaders.CmsPeRtnpayItems();

            List<Request.RequestInfo.CmsSiCtHeaders.CmsPeRtnpayItems.CmsPeRtnpayItem> listPays = new ArrayList<>();
            // ??????
            Request.RequestInfo.CmsSiCtHeaders.CmsSiSubjectMatters listSubject = new Request.RequestInfo.CmsSiCtHeaders.CmsSiSubjectMatters();

            List<Request.RequestInfo.CmsSiCtHeaders.CmsSiSubjectMatters.CmsSiSubjectMatter> listSubjects = new ArrayList<>();

            // ??????
            List<Request.RequestInfo.CmsSiCtHeaders.Items.Item03> listItem03 = new ArrayList<>();
            List<Request.RequestInfo.CmsSiCtHeaders.Items.Item02> listItem02 = new ArrayList<>();
            List<Request.RequestInfo.CmsSiCtHeaders.Items.Item01> listItem01 = new ArrayList<>();
            List<Request.RequestInfo.CmsSiCtHeaders.Items.Item00> listItem00 = new ArrayList<>();

            /*List<ContractPartner> collect = new ArrayList<>();*/
            // ???????????????id??????????????????
            ContractDTO contractDTO = getContractDTOSecond(contractHeadId, null);
            if (null != contractDTO) {
                // ???????????????
                contractHead = contractDTO.getContractHead();
                // ????????????
                contractMaterials = contractDTO.getContractMaterials();
                // ????????????
                payPlans = contractDTO.getPayPlans();

                /*List<ContractPartner> contractPartners = contractDTO.getContractPartners();
                // ??????????????????
                if (CollectionUtils.isNotEmpty(contractPartners)){
                    collect = contractPartners.stream().filter(s -> ContractHeadConst.PARTY_A.equals(s.getPartnerType())).collect(Collectors.toList());
                }*/
            } else {
                log.error("?????????id????????????");
                throw new BaseException("?????????id?????????");
            }
            // ?????????????????????
            if (null != contractHead) {
                Request.RequestInfo.CmsSiCtHeaders.Items.Item03 item03 = new Request.RequestInfo.CmsSiCtHeaders.Items.Item03();
                Request.RequestInfo.CmsSiCtHeaders.Items.Item02 item02 = new Request.RequestInfo.CmsSiCtHeaders.Items.Item02();
                Request.RequestInfo.CmsSiCtHeaders.Items.Item00 item00 = new Request.RequestInfo.CmsSiCtHeaders.Items.Item00();
                Request.RequestInfo.CmsSiCtHeaders.Items.Item01 item01 = new Request.RequestInfo.CmsSiCtHeaders.Items.Item01();

                List<DictItemDTO> dictItemDTOS = baseClient.listAllByDictCode("ELEM_CONTRACT_TYPE");
                String contractClass = contractHead.getContractClass();
                String contractClassName = "";
                if (Objects.nonNull(dictItemDTOS)) {
                    for (DictItemDTO dictItemDTO : dictItemDTOS) {
                        if (dictItemDTO.getDictItemCode().equals(contractHead.getContractClass())) {
                            contractClassName = dictItemDTO.getDictItemName();
                        }
                    }
                }
                //??????????????????
                item01.setLARGETYPECODE(contractClass);
                //??????????????????
                item01.setLARGETYPENAME(contractClassName);

                //??????org??????erp_org_id
                // ????????????id
                Organization contractOrg = baseClient.get(contractHead.getBuId());
                item03.setOUID(Objects.isNull(contractOrg) ? "" : contractOrg.getErpOrgId());
                // ??????????????????
                item03.setISFRAMEWORKAGREEMENT(StringUtils.isNotEmpty(contractHead.getIsFrameworkAgreement()) ? contractHead.getIsFrameworkAgreement() : "");
                // ??????
                item03.setORIGINALCURRENCY(contractHead.getCurrencyCode() == null ? "" : contractHead.getCurrencyCode());
                // ????????????
                item03.setORIGINALCURRENCYTOTALAMOUNT(contractHead.getIncludeTaxAmount() == null ? "" : contractHead.getIncludeTaxAmount() + "");
                // ??????????????????
                item03.setSIGNOTHERTYPE(ContractHeadConst.VENDOR);
                // ??????????????????
                item03.setSIGNOTHERNAME(StringUtils.isNotEmpty(contractHead.getVendorName()) ? contractHead.getVendorName() : "");
                // ??????????????????
                item03.setSIGNOTHERCODE(StringUtils.isNotEmpty(contractHead.getErpVendorCode()) ? contractHead.getErpVendorCode() : "");
                // ????????????
                item03.setINCOMEEXPENDTYPECODE(ContractHeadConst.EXPEND);
                String projectCode = "";
                String projectName = "";
                if (Objects.nonNull(contractMaterials)) {
                    for (int i = 0; i < contractMaterials.size(); i++) {
                        ContractMaterial cm = contractMaterials.get(i);
                        if (Objects.nonNull(cm) && StringUtils.isNotBlank(cm.getItemNumber())) {
                            projectCode = cm.getItemNumber();
                            projectName = cm.getItemName();
                            break;
                        }
                    }
                }
                //????????????
                item03.setBUFFER8(projectCode);
                //????????????
                item03.setBUFFER9(projectName);
                //??????????????????????????????
                item03.setCTSEALSTATUS("30");
                // ????????????
                    /*if(CollectionUtils.isNotEmpty(collect)) {
                        ContractPartner contractPartner = collect.get(0);
                        Organization organization = baseClient.getOrganization(new Organization().setCeeaCompanyName(contractPartner.getPartnerName()));
                        if(organization != null){
                            item03.setSIGNCOMPANYNAME(organization.getCeeaCompanyName());
                            item03.setSIGNCOMPANYCODE(organization.getCeeaCompanyCode());
                        }
                    }*/
                if (StringUtils.isNotEmpty(contractHead.getBuName())) {
                    List<Organization> organizationByNameList = baseClient.getOrganizationByNameListAnon(Arrays.asList(contractHead.getBuName()));
                    if (CollectionUtils.isNotEmpty(organizationByNameList)) {
                        Organization organization = organizationByNameList.get(0);
                        item03.setSIGNCOMPANYNAME(contractHead.getBuName());
                        item03.setSIGNCOMPANYCODE(organization.getCeeaErpUnitCode());
                    }
                }
                LoginAppUser user = rbacClient.findByUsername(contractHead.getCreatedBy());
                if (user != null) {
                    // ?????????????????????
                    item03.setBUSINESSAREACODE(StringUtils.isNotEmpty(user.getCeeaEmpNo()) ? user.getCeeaEmpNo() : "");
                    // ?????????????????????
                    item03.setBUSINESSAREANAME(StringUtils.isNotEmpty(user.getNickname()) ? user.getNickname() : "");
                    //?????????
                    item02.setBUSINESSNEGOTIATORNO(StringUtils.isNotEmpty(user.getCeeaEmpNo()) ? user.getCeeaEmpNo() : "");
                    //???????????????
                    item02.setBUSINESSNEGOTIATORNAME(StringUtils.isNotEmpty(user.getNickname()) ? user.getNickname() : "");
                }
                // ????????????
                item00.setCTNAME(StringUtils.isNotEmpty(contractHead.getContractName()) ? contractHead.getContractName() : "");
                // ????????????
                item00.setCTCODE(StringUtils.isNotEmpty(contractHead.getContractCode()) ? contractHead.getContractCode() : "");

                // ?????????ID
                item00.setCTHEADERID(contractHead.getContractHeadId() != null ? contractHead.getContractHeadId() + "" : "");
                item00.setCTSTATUS("30");

                listItem00.add(item00);
                listItem01.add(item01);
                listItem02.add(item02);
                listItem03.add(item03);
            } else {
                log.error("????????????????????????");
                throw new BaseException("?????????????????????");
            }

            // ??????????????????
            if (CollectionUtils.isNotEmpty(contractMaterials)) {
                contractMaterials.forEach(object -> {
                    Request.RequestInfo.CmsSiCtHeaders.CmsSiSubjectMatters.CmsSiSubjectMatter sub = new Request.RequestInfo.CmsSiCtHeaders.CmsSiSubjectMatters.CmsSiSubjectMatter();
                    // ????????????
                    if (null != object.getAmount()) {
                        sub.setTAXAMOUNT(object.getAmount());
                    } else {
                        if (object.getTaxedPrice() != null && object.getContractQuantity() != null) {
                            sub.setTAXAMOUNT(object.getTaxedPrice().multiply(object.getContractQuantity()));
                        }
                    }
                    // ??????
                    sub.setTAXRATE(object.getTaxRate() == null ? null : object.getTaxRate());
                    // ????????????
                    if (null != object.getUnAmount()) {
                        sub.setNOTAXAMOUNT(object.getUnAmount());
                    } else {
                        if (object.getTaxedPrice() != null && object.getContractQuantity() != null && object.getTaxRate() != null) {
                            BigDecimal tax = (object.getTaxRate().divide(new BigDecimal(100))).add(new BigDecimal(1));
                            // ????????????
                            BigDecimal unAmount = sub.getTAXAMOUNT().divide(tax, 2, RoundingMode.HALF_UP);
                            sub.setNOTAXAMOUNT(unAmount);
                        }
                    }
                    // ?????????
                    sub.setMEMO(StringUtils.isNotEmpty(object.getLineRemark()) ? object.getLineRemark() : "");
                    listSubjects.add(sub);
                });
            }
            // ????????????????????????
            //by ?????????????????????????????? 2020-11-13  lizhipeng  ??????OA?????????????????????????????????--???????????????
            /*if (CollectionUtils.isNotEmpty(payPlans)) {
                // ??????????????????
                String[] dict1 = {"PAYMENT_STAGE"};
                Map<String, String> map = setMapKeyValue(dict1);
                payPlans.forEach(Object -> {
                    Request.RequestInfo.CmsSiCtHeaders.CmsPeRtnpayItems.CmsPeRtnpayItem payItem = new Request.RequestInfo.CmsSiCtHeaders.CmsPeRtnpayItems.CmsPeRtnpayItem();
                    // ?????????id
                    payItem.setRTNPAYITEMID(Object.getPayPlanId() == null ? "" : Object.getPayPlanId() + "");
                    // ??????????????????
                    payItem.setRTNPAYPHASECODE(StringUtils.isNotEmpty(Object.getPaymentStage()) ? Object.getPaymentStage() : "");
                    // ??????????????????
                    payItem.setRTNPAYPHASENAME(map.get(Object.getPaymentStage()));

                    // ?????????????????????
                    payItem.setPLANRTNPAYRATE(Object.getPaymentRatio() == null ? null : Object.getPaymentRatio());
                    if (Object.getPlannedPaymentDate() != null) {
                        // ??????????????????
                        payItem.setPLANRTNPAYDATE(DateUtil.localDateToStr(Object.getPlannedPaymentDate()));
                    } else {
                        log.error("???????????????????????????");
                    }
                    // ??????????????????
                    payItem.setPLANRTNPAYAMOUNT(Object.getStagePaymentAmount() == null ? "" : Object.getStagePaymentAmount() + "");
                    listPays.add(payItem);
                });
            } else {
                log.error("?????????????????????");
                throw new BaseException("??????????????????");
            }*/

            items.setItem00(listItem00);
            items.setItem01(listItem01);
            items.setItem02(listItem02);
            items.setItem03(listItem03);
            header.setItems(items);
            listSubject.setCmsSiSubjectMatter(listSubjects);
            header.setCmsSiSubjectMatters(listSubject);
            listPay.setCmsPeRtnpayItem(listPays);
            header.setCmsPeRtnpayItems(listPay);
            requestInfo.setCmsSiCtHeaders(header);
            contractRequest.setRequestInfo(requestInfo);
            contractResponse = service.execute(contractRequest);
        } catch (Exception e) {
            log.error("???????????????????????????????????????: ", e);
            throw new BaseException(e.getMessage());
        }
        return contractResponse;
    }

    /**
     * Description ?????? ??????????????????????????????
     *
     * @return java.lang.Boolean
     * @Param [list, flag, classzz, paramName]
     * @Author fansb3@meicloud.com
     * @Date 2020/10/15
     **/
    private Boolean isHasErrorMessage(List list, Boolean flag, Class classzz, String paramName) {
        if (CollectionUtils.isNotEmpty(list)) {
            for (Object object : list) {
                String errorMsg = "";
                Method[] m = classzz.getMethods();
                for (int i = 0; i < m.length; i++) {
                    if (("get" + paramName).equalsIgnoreCase(m[i].getName())) {
                        try {
                            String name = m[i].getName();
                            errorMsg = (String) m[i].invoke(object);
                        } catch (Exception e) {
                            log.error("????????????", e);
                        }
                    }
                }
                if (StringUtils.isNotEmpty(errorMsg)) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }

    /**
     * ??????????????????
     */
    public Map<String, String> setMapValueKey(String[] dict) {
        List<DictItemDTO> dictItemDTOS = baseClient.listByDictCode(Arrays.asList(dict));
        Map<String, String> map = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dictItemDTOS)) {
            dictItemDTOS.forEach(dictItemDTO -> {
                map.put(dictItemDTO.getDictItemName().trim(), dictItemDTO.getDictItemCode().trim());
            });
        }
        return map;
    }

    public Map<String, String> setMapKeyValue(String[] dict) {
        List<DictItemDTO> dictItemDTOS = baseClient.listByDictCode(Arrays.asList(dict));
        Map<String, String> map = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dictItemDTOS)) {
            dictItemDTOS.forEach(dictItemDTO -> {
                map.put(dictItemDTO.getDictItemCode().trim(), dictItemDTO.getDictItemName().trim());
            });
        }
        return map;
    }

    /**
     * ?????????????????????
     *
     * @param contractHeadModelDTO
     * @param contractHead
     * @param errorMessage
     */
    private void checkContractHeadParam(ContractHeadModelDTO contractHeadModelDTO, ContractHead contractHead, StringBuffer errorMessage) {
        String[] dict = {"ELEM_CONTRACT_TYPE", "CONTARCT_LEVEL"};
        Map<String, String> dicValueKey = setMapValueKey(dict);
        // ???????????????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getContractNo())) {
            contractHead.setMainContractNo(contractHeadModelDTO.getContractNo());
        }
        // ????????????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getContractCode())) {
            contractHead.setContractCode(contractHeadModelDTO.getContractCode());
        }
        // ????????????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getContractName())) {
            contractHead.setContractName(contractHeadModelDTO.getContractName());
            contractHead.setContractStatus(ContractStatus.DRAFT.name());
        } else {
            errorMessage.append("????????????????????????;");
        }

        // ????????????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getContractClass())) {
            String contractClass = contractHeadModelDTO.getContractClass().trim();
            if (StringUtils.isNotEmpty(dicValueKey.get(contractClass))) {
                contractHead.setContractClass(dicValueKey.get(contractClass));
            } else {
                errorMessage.append("????????????????????????");
            }
        } else {
            errorMessage.append("???????????????????????????");
        }

        // ????????????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getEnable())) {
            if (contractHeadModelDTO.getEnable().equals(YesOrNo.NO.getName())) {
                contractHead.setEnable(YesOrNo.NO.getValue());
            } else if (contractHeadModelDTO.getEnable().equals(YesOrNo.YES.getName())) {
                contractHead.setEnable(YesOrNo.YES.getValue());
            } else {
                errorMessage.append("???????????????????????????\"???\"???\"???\"");
            }
        } else {
            errorMessage.append("?????????????????????????????????");
        }

        // ???????????????
        String startData = contractHeadModelDTO.getEffectiveDateFrom();
        if (StringUtil.notEmpty(startData)) {
            startData = startData.trim();
            try {
                Date date = DateUtil.parseDate(startData);
                LocalDate localDate = DateUtil.dateToLocalDate(date);
                contractHead.setEffectiveDateFrom(localDate);
            } catch (Exception e) {
                errorMessage.append("???????????????????????????; ");
            }
        } else {
            errorMessage.append("???????????????????????????");
        }
        // ???????????????
        String endData = contractHeadModelDTO.getEffectiveDateTo();
        if (StringUtil.notEmpty(endData)) {
            endData = endData.trim();
            try {
                Date date = DateUtil.parseDate(endData);
                LocalDate localDate = DateUtil.dateToLocalDate(date);
                contractHead.setEffectiveDateTo(localDate);
            } catch (Exception e) {
                errorMessage.append("???????????????????????????; ");
            }
        } else {
            errorMessage.append("??????????????????????????????");
        }
        // ????????????
        if (contractHead.getEffectiveDateTo() != null && contractHead.getEffectiveDateFrom() != null) {
            if (contractHead.getEffectiveDateFrom().compareTo(contractHead.getEffectiveDateTo()) > 0) {
                errorMessage.append("??????????????????????????????????????????");
            }
        }
        // ???????????????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getVendorCode())) {
            String vendorCode = contractHeadModelDTO.getVendorCode().trim();
            CompanyInfo companyInfoByParam = supplierClient.getCompanyInfoByParam(new CompanyInfo().setCompanyCode(vendorCode));
            if (companyInfoByParam != null) {
                if (StringUtils.isNotEmpty(contractHeadModelDTO.getVendorName())) {
                    if (contractHeadModelDTO.getVendorName().equals(companyInfoByParam.getCompanyName())) {
                        contractHead.setVendorCode(companyInfoByParam.getCompanyCode());
                        contractHead.setVendorId(companyInfoByParam.getCompanyId());
                        contractHead.setVendorName(companyInfoByParam.getCompanyName());
                        contractHead.setErpVendorCode(companyInfoByParam.getErpVendorCode());
                        contractHead.setErpVendorId(companyInfoByParam.getErpVendorId());
                    } else {
                        errorMessage.append("?????????????????????????????????????????????");
                    }
                } else {
                    errorMessage.append("??????????????????????????????");
                }
            } else {
                errorMessage.append("???????????????????????????");
            }
        } else {
            errorMessage.append("??????????????????????????????");
        }

        // ??????????????????  ???  ?????????????????????  ???  ??????????????????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getIsFrameworkAgreement())) {
            if (contractHeadModelDTO.getIsFrameworkAgreement().equals(YesOrNo.NO.getName())) {
                contractHead.setIsFrameworkAgreement(YesOrNo.NO.getValue());
            } else if (contractHeadModelDTO.getIsFrameworkAgreement().equals(YesOrNo.YES.getName())) {
                contractHead.setIsFrameworkAgreement(YesOrNo.YES.getValue());
                // ????????????
                if (StringUtils.isNotEmpty(contractHeadModelDTO.getContractLevel())) {
                    if (StringUtils.isNotEmpty(dicValueKey.get(contractHeadModelDTO.getContractLevel()))) {
                        contractHead.setContractLevel(dicValueKey.get(contractHeadModelDTO.getContractLevel()));
                    } else {
                        errorMessage.append("????????????????????????");
                    }
                } else {
                    errorMessage.append("???????????????????????????");
                }
                if (StringUtils.isNotEmpty(contractHeadModelDTO.getIncludeTaxAmount())) {
                    if (StringUtil.isDigit(contractHeadModelDTO.getIncludeTaxAmount())) {
                        contractHead.setIncludeTaxAmount(new BigDecimal(contractHeadModelDTO.getIncludeTaxAmount()));
                    } else {
                        errorMessage.append("??????????????????????????????");
                    }
                }
            } else {
                errorMessage.append("???????????????????????????\"???\"???\"???\"???");
            }
        } else {
            errorMessage.append("?????????????????????????????????");
        }

        /*// ????????????
        String isPowerStation = contractHeadModelDTO.getIsPowerStation();
        if (StringUtils.isNotEmpty(isPowerStation)) {
            if (isPowerStation.equals(YesOrNo.NO.getName())) {
                contractHead.setIsPowerStation(YesOrNo.NO.getValue());
            } else if (isPowerStation.equals(YesOrNo.YES.getName())) {
                contractHead.setIsPowerStation(YesOrNo.YES.getValue());
            } else {
                errorMessage.append("?????????????????????\"???\"???\"???\"");
            }
        }*/
        // ??????????????????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getBuName())) {
            String buName = contractHeadModelDTO.getBuName();
            List<Organization> organizationByNameList = baseClient.getOrganizationByNameList(Arrays.asList(buName));
            if (CollectionUtils.isNotEmpty(organizationByNameList)) {
                Organization organization = organizationByNameList.get(0);
                contractHead.setBuCode(organization.getOrganizationCode());
                contractHead.setBuId(organization.getOrganizationId());
                contractHead.setBuName(organization.getOrganizationName());
            } else {
                errorMessage.append("??????????????????????????????");
            }
        } else {
            errorMessage.append("?????????????????????????????????");
        }
        // ??????
        if (StringUtils.isNotEmpty(contractHeadModelDTO.getCurrencyName())) {
            String currencyName = contractHeadModelDTO.getCurrencyName();
            PurchaseCurrency purchaseCurrency = baseClient.getPurchaseCurrencyByParam(new PurchaseCurrency().setCurrencyName(currencyName));
            if (purchaseCurrency != null) {
                contractHead.setCurrencyCode(purchaseCurrency.getCurrencyCode())
                        .setCurrencyId(purchaseCurrency.getCurrencyId()).setCurrencyName(purchaseCurrency.getCurrencyName());
            } else {
                errorMessage.append("??????????????????");
            }
        } else {
            errorMessage.append("?????????????????????");
        }
        /*// ????????????
        String isHeadquarters = contractHeadModelDTO.getIsHeadquarters();
        if (StringUtils.isNotEmpty(isHeadquarters)) {
            if (isHeadquarters.equals(YesOrNo.NO.getName())) {
                contractHead.setIsHeadquarters(YesOrNo.NO.getValue());
            } else if (isHeadquarters.equals(YesOrNo.YES.getName())) {
                contractHead.setIsHeadquarters(YesOrNo.YES.getValue());
            } else {
                errorMessage.append("?????????????????????\"???\"???\"???\"");
            }
        }*/
        contractHead.setContractType("MIAN_CONTRACT_ADD");
        contractHeadModelDTO.setErrorMessage(errorMessage.toString());
    }


    /**
     * ??????????????????
     *
     * @param contractMaterialModelDTO
     * @param contractMaterial
     * @param errorMessage
     */
    private void checkContractMaterialParam(ContractMaterialModelDTO contractMaterialModelDTO, ContractMaterial contractMaterial, StringBuffer errorMessage, ContractHead contractHead) {
        // ????????????
        if (StringUtils.isNotEmpty(contractMaterialModelDTO.getBuName())) {
            String buName = contractMaterialModelDTO.getBuName().trim();
            List<Organization> organizationByNameList = baseClient.getOrganizationByNameList(Arrays.asList(buName));
            if (CollectionUtils.isNotEmpty(organizationByNameList)) {
                Organization organization = organizationByNameList.get(0);
                contractMaterial.setBuCode(organization.getOrganizationCode());
                contractMaterial.setBuId(organization.getOrganizationId());
                contractMaterial.setBuName(organization.getOrganizationName());
                contractMaterial.setBuFullPathId(organization.getFullPathId());
                // ????????????
                if (StringUtils.isNotEmpty(contractMaterialModelDTO.getInvName())) {
                    String invName = contractMaterialModelDTO.getInvName();
                    List<Organization> Organizations = baseClient.queryIvnByOuId(contractMaterial.getBuId());
                    if (CollectionUtils.isNotEmpty(Organizations)) {
                        Organizations.forEach(organization1 -> {
                            if (invName.equals(organization1.getOrganizationName())) {
                                contractMaterial.setInvCode(organization1.getOrganizationCode())
                                        .setInvId(organization1.getOrganizationId())
                                        .setInvName(organization1.getOrganizationName())
                                        .setInvFullPathId(organization1.getFullPathId());
                            }
                        });
                        if (StringUtils.isEmpty(contractMaterial.getInvName())) {
                            errorMessage.append("???????????????????????????");
                        }
                    } else {
                        errorMessage.append("??????????????????????????????????????????");
                    }
                } else {
                    errorMessage.append("???????????????????????????");
                }
            } else {
                errorMessage.append("????????????????????????");
            }
        } else {
            errorMessage.append("???????????????????????????");
        }
        // ????????????
        if (StringUtils.isNotEmpty(contractMaterialModelDTO.getMaterialCode())) {
            String materialCode = contractMaterialModelDTO.getMaterialCode().trim();
            MaterialItem materialItem = baseClient.findMaterialItemByMaterialCode(materialCode);
            if (materialItem != null) {
                if (materialItem.getMaterialName().equals(contractMaterialModelDTO.getMaterialName())) {
                    boolean flag = baseClient.checkBigClassCodeIsContain50(Arrays.asList(materialItem.getMaterialId()));
                    if (!flag) {
                        contractMaterial.setMaterialCode(materialItem.getMaterialCode())
                                .setMaterialId(materialItem.getMaterialId())
                                .setMaterialName(materialItem.getMaterialName())
                                .setCategoryName(materialItem.getCategoryName()).setCategoryCode(materialItem.getCategoryCode())
                                .setCategoryId(materialItem.getCategoryId())
                                .setUnitName(materialItem.getUnitName())
                                .setSpecification(materialItem.getSpecification());
                    } else {
                        errorMessage.append("?????????????????????50???????????????????????????");
                    }
                } else {
                    errorMessage.append("???????????????????????????????????????");
                }
            } else {
                errorMessage.append("????????????????????????");
            }
        } else {
            errorMessage.append("???????????????????????????");
        }
        // ????????????
        if (StringUtils.isNotEmpty(contractMaterialModelDTO.getTradingLocations())) {
            contractMaterial.setTradingLocations(contractMaterialModelDTO.getTradingLocations());
        }
        // ????????????
        if (StringUtils.isNotEmpty(contractMaterialModelDTO.getTaxedPrice())) {
            String taxedPrice = contractMaterialModelDTO.getTaxedPrice().trim();
            if (StringUtil.isDigit(taxedPrice)) {
                contractMaterial.setTaxedPrice(new BigDecimal(taxedPrice));
            } else {
                errorMessage.append("???????????????????????????");
            }
        } else {
            errorMessage.append("???????????????????????????");
        }

        // ??????
        if (StringUtils.isNotEmpty(contractMaterialModelDTO.getContractQuantity())) {
            String contractQuantity = contractMaterialModelDTO.getContractQuantity().trim();
            if (StringUtil.isDigit(contractQuantity)) {
                contractMaterial.setContractQuantity(new BigDecimal(contractQuantity));
            } else {
                errorMessage.append("?????????????????????");
            }
        }

        // ??????
        if (StringUtils.isNotEmpty(contractMaterialModelDTO.getTaxKey())) {
            String taxKey = contractMaterialModelDTO.getTaxKey().trim();
            List<PurchaseTax> purchaseTaxes = baseClient.listTaxAll();
            if (CollectionUtils.isNotEmpty(purchaseTaxes)) {
                purchaseTaxes.forEach(object -> {
                    if (taxKey.equals(object.getTaxKey())) {
                        contractMaterial.setTaxKey(object.getTaxKey());
                        if (StringUtils.isNotEmpty(contractMaterialModelDTO.getTaxRate())) {
                            if (StringUtil.isDigit(contractMaterialModelDTO.getTaxRate())) {
                                if (new BigDecimal(contractMaterialModelDTO.getTaxRate()).compareTo(object.getTaxCode()) == 0) {
                                    contractMaterial.setTaxRate(object.getTaxCode());

                                } else {
                                    errorMessage.append("?????????????????????????????????");
                                }
                            } else {
                                errorMessage.append("?????????????????????");
                            }
                        } else {
                            errorMessage.append("?????????????????????");
                        }
                    }
                });
                if (StringUtils.isEmpty(contractMaterial.getTaxKey())) {
                    errorMessage.append("????????????????????????");
                }
            }
        } else {
            errorMessage.append("????????????????????????");
        }
        // ?????????????????????
        String startDate = contractMaterialModelDTO.getStartDate();
        if (StringUtil.notEmpty(startDate)) {
            startDate = startDate.trim();
            try {
                Date date = DateUtil.parseDate(startDate);
                LocalDate localDate = DateUtil.dateToLocalDate(date);
                contractMaterial.setStartDate(localDate);
                if (contractHead.getEffectiveDateFrom() != null) {
                    if (contractHead.getEffectiveDateFrom().compareTo(localDate) > 0) {
                        errorMessage.append("????????????????????????????????????????????????????????? ");
                    }
                }
            } catch (Exception e) {

            }
        } else {
            errorMessage.append("?????????????????????????????????; ");
        }
        // ?????????????????????
        String endDate = contractMaterialModelDTO.getEndDate();
        if (StringUtil.notEmpty(endDate)) {
            endDate = endDate.trim();
            try {
                Date date = DateUtil.parseDate(endDate);
                LocalDate localDate = DateUtil.dateToLocalDate(date);
                contractMaterial.setEndDate(localDate);
                if (contractHead.getEffectiveDateTo() != null) {
                    if (contractHead.getEffectiveDateTo().compareTo(localDate) < 0) {
                        errorMessage.append("????????????????????????????????????????????????????????? ");
                    }
                }
            } catch (Exception e) {
                errorMessage.append("?????????????????????????????????; ");
            }
        } else {
            errorMessage.append("?????????????????????????????????; ");
        }


        // ???????????????
        if (StringUtils.isNotEmpty(contractMaterialModelDTO.getOrderQuantity())) {
            if (StringUtil.isDigit(contractMaterialModelDTO.getOrderQuantity())) {
                contractMaterial.setOrderQuantity(new BigDecimal(contractMaterialModelDTO.getOrderQuantity()));
            } else {
                errorMessage.append("?????????????????????????????? ");
            }
        }
        contractMaterialModelDTO.setErrorMessage(errorMessage.toString());

    }

    /**
     * ??????????????????
     *
     * @param payPlanModelDTO
     * @param payPlan
     * @param errorMessage
     */
    private void checkPayPlanParam(PayPlanModelDTO payPlanModelDTO, PayPlan payPlan, StringBuffer errorMessage, BigDecimal includeTaxAmount, HashSet only) {
        String[] dict = {"PAYMENT_PERIOD", "PAYMENT_MODE"};
        Map<String, String> dictMap = setMapValueKey(dict);
        // ???????????????????????????????????????????????????
        String[] dict1 = {"PAYMENT_STAGE"};
        Map<String, String> dictMap1 = setMapValueKey(dict1);

        // ????????????
        if (StringUtils.isNotEmpty(payPlanModelDTO.getPaymentStage())) {
            String paymentStage = payPlanModelDTO.getPaymentStage().trim();
            if (StringUtils.isNotEmpty(dictMap1.get(paymentStage))) {
                payPlan.setPaymentStage(dictMap1.get(paymentStage));
            } else {
                errorMessage.append("????????????????????????");
            }
        }
//        else {
//            errorMessage.append("???????????????????????????");
//        }
        // ????????????
        if (StringUtils.isNotEmpty(payPlanModelDTO.getPaymentPeriod())) {
            if (StringUtil.isDigit(payPlanModelDTO.getPaymentPeriod())) {
                payPlan.setPaymentPeriod(payPlanModelDTO.getPaymentPeriod());
                if (!only.add(payPlanModelDTO.getPaymentPeriod())) {
                    errorMessage.append("??????????????????");
                }
            } else {
                errorMessage.append("???????????????????????????");
            }
        }
//        else {
//            errorMessage.append("???????????????????????????");
//        }
        //????????????
        if (StringUtils.isNotEmpty(payPlanModelDTO.getPayExplain())) {
            String payExplain = payPlanModelDTO.getPayExplain().trim();
            List<PayType> activationPaymentTerms = iPayTypeService.getActivationPaymentTerms();
            if (CollectionUtils.isNotEmpty(activationPaymentTerms)) {
                activationPaymentTerms.forEach(object -> {
                    if (payExplain.equals(object.getPayExplain())) {
                        payPlan.setPayExplain(object.getPayTypeId() + "");
                    }
                });
            }
            if (StringUtils.isEmpty(payPlan.getPayExplain())) {
                errorMessage.append("????????????????????????");
            }
        }
//        else {
//            errorMessage.append("???????????????????????????");
//        }
        //??????
        if (StringUtils.isNotEmpty(payPlanModelDTO.getDateNum())) {
            String dateNum = payPlanModelDTO.getDateNum().trim();
            if (StringUtils.isNotEmpty(dictMap.get(dateNum))) {
                payPlan.setDateNum(dictMap.get(dateNum));
            } else {
                errorMessage.append("??????????????????");
            }
        }
//        else {
//            errorMessage.append("?????????????????????");
//        }
        //????????????
        if (StringUtils.isNotEmpty(payPlanModelDTO.getPaymentRatio())) {
            String paymentRatio = payPlanModelDTO.getPaymentRatio().trim();
            if (StringUtil.isDigit(paymentRatio)) {
                payPlan.setPaymentRatio(new BigDecimal(paymentRatio));
                payPlan.setStagePaymentAmount(payPlan.getPaymentRatio().multiply(includeTaxAmount).divide(new BigDecimal(100)));
            } else {
                errorMessage.append("???????????????????????????");
            }
        }
//        else {
//            errorMessage.append("???????????????????????????");
//        }
        //??????????????????
        String plannedPaymentDate = payPlanModelDTO.getPlannedPaymentDate();
        if (StringUtil.notEmpty(plannedPaymentDate)) {
            try {
                Date date = DateUtil.parseDate(plannedPaymentDate);
                LocalDate localDate = DateUtil.dateToLocalDate(date);
                payPlan.setPlannedPaymentDate(localDate);
            } catch (Exception e) {
                errorMessage.append("??????????????????????????????; ");
            }
        }

        //????????????
        if (StringUtils.isNotEmpty(payPlanModelDTO.getPayMethod())) {
            String payMethod = payPlanModelDTO.getPayMethod().trim();
            if (StringUtils.isNotEmpty(dictMap.get(payMethod))) {
                payPlan.setPayMethod(dictMap.get(payMethod));
            } else {
                errorMessage.append("????????????????????????");
            }
        }
//        else {
//            errorMessage.append("???????????????????????????");
//        }
        // ?????????????????????
        if (StringUtils.isNotEmpty(payPlanModelDTO.getStagePaymentAmount())) {
            if (StringUtil.isDigit(payPlanModelDTO.getStagePaymentAmount())) {
                payPlan.setStagePaymentAmount(new BigDecimal(payPlanModelDTO.getStagePaymentAmount()));
            } else {
                errorMessage.append("????????????????????????????????????");
            }
        }
        // ????????????
        if (StringUtils.isNotEmpty(payPlanModelDTO.getPaidAmount())) {
            if (StringUtil.isDigit(payPlanModelDTO.getPaidAmount())) {
                payPlan.setStagePaymentAmount(new BigDecimal(payPlanModelDTO.getPaidAmount()));
            } else {
                errorMessage.append("??????????????????????????????");
            }
        }
        payPlanModelDTO.setErrorMessage(errorMessage.toString());
    }

    /**
     * ??????????????????
     *
     * @param contractPartnerModelDTO
     * @param contractPartner
     * @param errorMessage
     */
    private void checkContractPartnerParam(ContractPartnerModelDTO contractPartnerModelDTO, ContractPartner contractPartner, StringBuffer errorMessage, ContractHeadModelDTO contractHeadModelDTO) {
        //????????????
        if (StringUtils.isNotEmpty(contractPartnerModelDTO.getPartnerType())) {
            String partnerType = contractPartnerModelDTO.getPartnerType().trim();
            if (ContractHeadConst.PARTY_A.equals(partnerType) || ContractHeadConst.PARTY_C.equals(partnerType)) { // ??????
                contractPartner.setPartnerType(partnerType);
                // ????????????????????????
                if (StringUtils.isNotEmpty(contractPartnerModelDTO.getPartnerName())) {
                    String partnerName = contractPartnerModelDTO.getPartnerName().trim();
                    Organization organization = baseClient.getOrganization(new Organization().setCeeaCompanyName(partnerName));
                    if (organization != null) {
                        contractPartner.setPartnerName(organization.getCeeaCompanyName());
                    } else {
                        errorMessage.append("????????????????????????");
                    }
                } else {
                    errorMessage.append("???????????????????????????");
                }
            } else if (ContractHeadConst.PARTY_B.equals(partnerType)) { // ??????
                contractPartner.setPartnerType(ContractHeadConst.PARTY_B);
                if (StringUtils.isNotEmpty(contractPartnerModelDTO.getPartnerName())) {
                    if (contractPartnerModelDTO.getPartnerName().equals(contractHeadModelDTO.getVendorName())) {
                        contractPartner.setPartnerName(contractPartnerModelDTO.getPartnerName());
                    } else {
                        errorMessage.append("???????????????????????????????????????????????????");
                    }
                }
            } else {
                errorMessage.append("????????????????????????");
            }
        } else {
            errorMessage.append("???????????????????????????");
        }
        contractPartnerModelDTO.setErrorMessage(errorMessage.toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long genContractFromApproval(ApprovalToContractDetail detail) {
        List<ApprovalBiddingItemVO> approvalItems = detail.getContractLineList();
        ApprovalBiddingItemVO itemInfo = approvalItems.get(0);
        CompanyInfo tempCompanyInfo = supplierClient.getCompanyInfo(itemInfo.getVendorId());
        Long contractId = IdGenrator.generate();
        ContractHead contractHead = new ContractHead()
                .setContractName("?????????????????????:" + detail.getApprovalNo())
                .setContractNo(baseClient.seqGen(SequenceCodeConstant.SEQ_CONTRACT_NO))
                //???????????????
                .setContractType(ContractType.MIAN_CONTRACT_ADD.name())
                //????????????????????? contract_class
                .setVendorId(tempCompanyInfo.getCompanyId())
                .setVendorCode(tempCompanyInfo.getCompanyCode())
                .setVendorName(tempCompanyInfo.getCompanyName())
                .setErpVendorCode(tempCompanyInfo.getErpVendorCode())
                .setErpVendorId(tempCompanyInfo.getErpVendorId())
                .setContractHeadId(contractId)
                .setEnable(YesOrNo.YES.getValue())
                .setIsFrameworkAgreement(YesOrNo.NO.getValue())
                .setSourceType(detail.getSourceType())
                .setContractStatus(ContractStatus.DRAFT.name())
                .setSourceType(ContractSourceType.PRICE_APPROVAL.name())
                .setCeeaIfVirtual(detail.getCeeaIfVirtual())
                .setSourceNumber(detail.getApprovalNo());

        //?????????
        List<ContractMaterial> contractMaterials = new LinkedList<>();
        Set<Long> ouIds = new HashSet<>();
        Set<String> purchaseNum = new HashSet<>();
        Set<Long> orgIds = new HashSet<>();
        boolean notFind = true;
        for (ApprovalBiddingItemVO e : approvalItems) {
            if (notFind && Objects.equals(e.getStandardCurrency(), e.getCurrencyCode())) {
                notFind = false;
                contractHead
                        .setCurrencyCode(e.getCurrencyCode())
                        .setCurrencyId(e.getCurrencyId())
                        .setCurrencyName(e.getCurrencyName());
            }
            //????????????id
            e.setFromContractId(e.getFromContractId());
            //ou???id
            Long ouId = e.getOuId();
            if (Objects.nonNull(ouId)) {
                ouIds.add(ouId);
            }
            //??????????????????
            String purchaseRequestNum = e.getPurchaseRequestNum();
            if (Objects.nonNull(purchaseRequestNum)) {
                purchaseNum.add(purchaseRequestNum);
            }
            //????????????id
            Long orgId = e.getOrgId();
            if (Objects.nonNull(orgId)) {
                orgIds.add(orgId);
            }
        }
        if (notFind) {
            contractHead.setCurrencyCode("CNY");
            contractHead.setCurrencyName("?????????");
        }


        //?????????????????????????????????
        List<BaseOuGroupDetailVO> baseOuGroupDetailVOS = baseClient.queryOuInfoDetailByIds(ouIds);
        List<RequirementHead> requirementHeadByNumber = pmClient.getRequirementHeadByNumber(purchaseNum);
        //??????????????????,???????????????ou???,????????????????????????
        //???????????????
        boolean isCalculate = true;
        BigDecimal total = null;
        for (ApprovalBiddingItemVO approvalBiddingItem : approvalItems) {
            RequirementHead requirementHead = null;
            if (!org.springframework.util.CollectionUtils.isEmpty(requirementHeadByNumber)) {
                //???????????????????????????
                for (RequirementHead head : requirementHeadByNumber) {
                    if (Objects.nonNull(head) && Objects.equals(head.getRequirementHeadNum(), approvalBiddingItem.getPurchaseRequestNum())) {
                        requirementHead = head;
                        break;
                    }
                }
            }
            if (Objects.isNull(requirementHead)) {
                requirementHead = new RequirementHead();
            }
            List<ContractMaterial> temp = createContractMaterialByItem(approvalBiddingItem, baseOuGroupDetailVOS, requirementHead);
            //??????id??????
            temp.forEach(e -> {
                if (Objects.nonNull(e.getCeeaOuId())) {
                    e.setOrderQuantity(null)
                            .setAmount(null)
                            .setUnAmount(null);
                }
                e.setContractHeadId(contractId).setContractMaterialId(IdGenrator.generate());
            });
            contractMaterials.addAll(temp);
            BigDecimal contractQuantity = approvalBiddingItem.getQuotaQuantity();
            BigDecimal taxedPrice = approvalBiddingItem.getTaxPrice();
            if (Objects.isNull(contractQuantity) || Objects.isNull(taxedPrice)) {
                isCalculate = false;
            } else {
                total = Objects.isNull(total) ? contractQuantity.multiply(taxedPrice) : total.add(contractQuantity.multiply(taxedPrice));
            }
        }
        //??????db
        if (isCalculate) {
            contractHead.setIncludeTaxAmount(total);
        }

        // ???????????????
        CompanyInfo companyInfo = supplierClient.getCompanyInfo(contractHead.getVendorId());
        if (YesOrNo.YES.getValue().equals(companyInfo.getCompanyLevel())) {
            // ???????????????A???????????????Y?????????????????????A????????????
            contractHead.setContractLevel("SA");
        } else {
            // ???????????????A???????????????N?????????????????????????????????????????????????????????
            String contractLevel = getContractLevel(contractMaterials);
            Assert.notNull(contractLevel, "???????????????????????????????????????????????????,???????????????????????????????????????????????????????????????!!!");
            contractHead.setContractLevel(contractLevel);
        }

        this.save(contractHead);
        //???????????????
        iContractMaterialService.saveBatch(contractMaterials);
        //??????????????????
        List<PayPlan> payPlans = new LinkedList<>();
        for (int i = 0; i < itemInfo.getApprovalBiddingItemPaymentTermList().size(); i++) {
            ApprovalBiddingItemPaymentTerm e = itemInfo.getApprovalBiddingItemPaymentTermList().get(i);
            PayPlan payPlan = new PayPlan()
                    .setPayMethod(e.getPaymentWay())
                    .setDateNum(e.getPaymentDayCode())
                    .setPayExplain(String.valueOf(e.getPaymentTerm()))
                    .setDelayedDays(e.getPaymentDay().longValue())
                    .setPayPlanId(IdGenrator.generate())
                    .setPaymentStage(e.getPaymentStage())
                    .setPaymentRatio(e.getPaymentRatio())
                    .setPaymentPeriod(String.valueOf(i + 1))
                    .setContractHeadId(contractId);
            payPlans.add(payPlan);
        }
        iPayPlanService.saveBatch(payPlans);

        //????????????,??????????????????????????????????????????
        ContractPartner contractPartner = new ContractPartner();
        contractPartner.setPartnerType("??????")
                .setPartnerId(IdGenrator.generate())
                .setContractHeadId(contractId)
                .setPartnerName(contractHead.getVendorName());
        //??????????????????
        iContractPartnerService.save(contractPartner);

        //???????????????
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //???????????????????????????????????????????????????????????????
                redisTemplate.opsForSet().add("contractUpdateApproval", String.valueOf(contractId));
                approvalItems.forEach(e -> e.setFromContractId(contractId));
                inqClient.updateApprovalBidingInfo(approvalItems);
                redisTemplate.opsForSet().remove("contractUpdateApproval", String.valueOf(contractId));
            }
        });
        return contractId;
    }

    private AdvanceApplyHead setBaseByContract(ContractHead contractHead, AdvanceApplyHeadVo advanceApplyHeadVo) {
        return advanceApplyHeadVo.setVendorId(contractHead.getVendorId())
                .setErpVendorCode(contractHead.getErpVendorCode())
                .setVendorCode(contractHead.getVendorCode())
                .setVendorName(contractHead.getVendorName())
                .setCurrencyId(contractHead.getCurrencyId())
                .setCurrencyCode(contractHead.getCurrencyCode())
                .setCurrencyName(contractHead.getCurrencyName())
                .setIfPowerStation(contractHead.getIsPowerStation())
                .setIncludeTaxAmount(contractHead.getIncludeTaxAmount());
    }

    private List<ContractMaterial> createContractMaterialByItem(ApprovalBiddingItemVO approvalBiddingItem, List<BaseOuGroupDetailVO> ous, RequirementHead requirementHead) {
        //???????????? , ????????????
        List<ContractMaterial> result = new LinkedList<>();
        Long ouId = approvalBiddingItem.getOuId();
        boolean isOuGroup = Objects.nonNull(ouId);
        if (isOuGroup) {
            BaseOuGroupDetailVO detailVO = ous.stream().filter(e -> e.getOuGroupId().equals(ouId))
                    .findAny().orElseThrow(() -> new BaseException(String.format("ou???id???%d?????????", ouId)));
            List<BaseOuDetailVO> details = detailVO.getDetails();
            for (BaseOuDetailVO detail : details) {
                ContractMaterial contractMaterial = new ContractMaterial()
                        .setBuCode(detail.getOuCode())
                        .setBuName(detail.getOuName())
                        .setBuId(detail.getOuId())
                        .setInvCode(detail.getInvCode())
                        .setInvId(detail.getInvId())
                        .setInvName(detail.getInvName())
                        .setCeeaOuId(detailVO.getOuGroupId())
                        .setCeeaOuName(detailVO.getOuGroupName())
                        .setCeeaOuNumber(detailVO.getOuGroupCode());
                assignValue(contractMaterial, approvalBiddingItem, requirementHead);
                result.add(contractMaterial);
            }
        } else {
            ContractMaterial contractMaterial = new ContractMaterial()
                    .setBuId(approvalBiddingItem.getOrgId())
                    .setBuName(approvalBiddingItem.getOrgName())
                    .setBuCode(approvalBiddingItem.getOrgCode())
                    .setInvId(approvalBiddingItem.getOrganizationId())
                    .setInvCode(approvalBiddingItem.getOrganizationCode());
            contractMaterial.setInvName(approvalBiddingItem.getOrganizationName());
            assignValue(contractMaterial, approvalBiddingItem, requirementHead);
            result.add(contractMaterial);
        }
        return result;
    }

    //??????inv,bu???????????????
    private void assignValue(ContractMaterial contractMaterial, ApprovalBiddingItemVO approvalBiddingItem, RequirementHead requirementHead) {
        contractMaterial.setCeeaSourceLineId(approvalBiddingItem.getApprovalBiddingItemId())
                .setSourceNumber(approvalBiddingItem.getCeeaSourceNo())
                .setSourceType(approvalBiddingItem.getSourceType())
                .setTradingLocations(approvalBiddingItem.getArrivalPlace())
                .setMaterialId(approvalBiddingItem.getItemId())
                .setMaterialCode(approvalBiddingItem.getItemCode())
                .setMaterialName(approvalBiddingItem.getItemName())
                .setCategoryId(approvalBiddingItem.getCategoryId())
                .setCategoryCode(approvalBiddingItem.getCategoryCode())
                .setCategoryName(approvalBiddingItem.getCategoryName())
                .setTradeTerm(approvalBiddingItem.getTradeTerm())
                .setShelfLife(Objects.isNull(approvalBiddingItem.getWarrantyPeriod()) ? null : BigDecimal.valueOf(approvalBiddingItem.getWarrantyPeriod()))
                //????????????
                .setContractQuantity(approvalBiddingItem.getQuotaQuantity())
                //??????
                .setUnitCode(approvalBiddingItem.getUnit())
                .setUnitName(approvalBiddingItem.getUnit())
                // ????????????
                .setTaxedPrice(approvalBiddingItem.getTaxPrice())
                //?????????????????????????????????
                .setIsSeaFoodFormula(approvalBiddingItem.getIsSeaFoodFormula())
                .setFormulaId(approvalBiddingItem.getFormulaId())
                .setFormulaValue(approvalBiddingItem.getFormulaValue())
                .setEssentialFactorValues(approvalBiddingItem.getEssentialFactorValues())
                .setFormulaResult(approvalBiddingItem.getFormulaResult())
                .setPriceJson(approvalBiddingItem.getPriceJson());
        BigDecimal contractQuantity = contractMaterial.getContractQuantity();
        BigDecimal taxedPrice = contractMaterial.getTaxedPrice();
        if (Objects.nonNull(contractQuantity) && Objects.nonNull(taxedPrice)) {
            contractMaterial.setAmount(contractQuantity.multiply(taxedPrice));
        }
        contractMaterial.setTaxKey(approvalBiddingItem.getTaxKey())
                .setTaxRate(approvalBiddingItem.getTaxRate());
        BigDecimal taxRate = contractMaterial.getTaxRate();
        BigDecimal amount = contractMaterial.getAmount();
        if (Objects.nonNull(taxRate) && Objects.nonNull(amount)) {
            contractMaterial.setUnAmount(calculateNoTaxPrice(amount, taxRate.multiply(BigDecimal.valueOf(0.01))));
        }
        contractMaterial.setStartDate(approvalBiddingItem.getStartTime())
                .setEndDate(approvalBiddingItem.getEndTime());
        if (Objects.nonNull(requirementHead)) {
            contractMaterial.setItemNumber(requirementHead.getCeeaProjectNum())
                    .setItemName(requirementHead.getCeeaProjectName());
        }
    }

    private BigDecimal calculateNoTaxPrice(BigDecimal taxPrice, BigDecimal taxRate) {
        //??????????????????,?????????*???1+?????????=??????
        BigDecimal noTaxPrice = taxPrice.divide(BigDecimal.ONE.add(taxRate), 8, BigDecimal.ROUND_HALF_DOWN);
        return noTaxPrice;
    }

    @Override
    public void bulkMaintenanceFramework(BulkMaintenanceFrameworkParamDto bulkMaintenanceFrameworkParamDto) {
        List<Long> contractIds = bulkMaintenanceFrameworkParamDto.getContractIds();
        if (CollectionUtils.isNotEmpty(contractIds)) {
            String contractCode = bulkMaintenanceFrameworkParamDto.getContractCode();
            String contractName = bulkMaintenanceFrameworkParamDto.getContractName();
            Long contractHeadId = bulkMaintenanceFrameworkParamDto.getContractHeadId();
            if (StringUtil.notEmpty(contractCode) && StringUtil.notEmpty(contractName)) {
                List<ContractHead> contractHeads = this.listByIds(contractIds);
                if (CollectionUtils.isNotEmpty(contractHeads)) {
                    // ??????????????????????????????
                    Long vendorId = contractHeads.get(0).getVendorId();
                    Assert.notNull(vendorId, "???????????????????????????");
                    contractHeads.forEach(contractHead -> {
                        if (StringUtil.notEmpty(contractHead.getVendorId())) {
                            Assert.isTrue(vendorId.equals(contractHead.getVendorId()), "???????????????????????????????????????????????????");
                        } else {
                            throw new BaseException("???????????????????????????");
                        }
                        // ????????????????????????????????????????????????????????????????????????
                        if (YesOrNo.YES.getValue().equals(contractHead.getCeeaIfVirtual())) {
                            contractHead.setContractCode(contractCode);
                        }
                        contractHead.setFrameworkAgreementId(contractHeadId);
                        contractHead.setFrameworkAgreementCode(contractCode);
                        contractHead.setFrameworkAgreementName(contractName);
                    });
                }
                this.updateBatchById(contractHeads);
            } else {
                throw new BaseException("?????????????????????");
            }
        }
    }

    @Override
    public void signingCallback(ContractHead head) {
        Long contractHeadId = head.getContractHeadId();
        Assert.notNull(contractHeadId,"????????????ID????????????");
        ContractHead contractHead = this.getById(contractHeadId);
        Assert.notNull(contractHead,"????????????????????????");
        // ??????
        contractHead.setContractStatus(ContractStatus.ARCHIVED.name());
        contractHead.setStampContractFileuploadId(head.getStampContractFileuploadId());
        contractHead.setStampContractFileName(contractHead.getContractName()+".pdf");
        this.updateById(contractHead);
    }

    @Override
    public Map<String, Object> release(PushContractParam pushContractParam) throws Exception {
        Assert.notNull(pushContractParam.getContractHeadId(),"contractHeadId: ????????????");
        Assert.notNull(pushContractParam.getFileuploadId(),"fileuploadId: ????????????");
        ContractHead contractHead = this.getById(pushContractParam.getContractHeadId());
        Assert.notNull(contractHead,"???????????????: contractHeadId = " + pushContractParam.getContractHeadId());
        SigningParam signingParam = new SigningParam();
        signingParam.setContractCode(contractHead.getContractNo());
        signingParam.setContractName(contractHead.getContractName());
        signingParam.setFileuploadId(pushContractParam.getFileuploadId());
        signingParam.setReceiptId(pushContractParam.getContractHeadId().toString());
        signingParam.setPhone(pushContractParam.getPhone());
        signingParam.setEmail(pushContractParam.getEmail());
        signingParam.setName(pushContractParam.getName());

        // ????????????
        Map<String, Object> result = signatureClient.addSigning(signingParam);

        // ??????????????????
        contractHead.setOriginalContractFileuploadId(pushContractParam.getFileuploadId());
//        contractHead.setContractStatus(ContractStatus.SUPPLIER_CONFIRMING.name());
        contractHead.setContractStatus(ContractStatus.ARCHIVED.name());
        this.updateById(contractHead);

        return result;
    }
}
