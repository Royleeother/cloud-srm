package com.midea.cloud.srm.pr.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.pm.pr.requirement.DUTY;
import com.midea.cloud.common.enums.pm.pr.requirement.PurchaseType;
import com.midea.cloud.common.enums.pm.pr.requirement.RequirementApproveStatus;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.component.handler.InjectDefMetaObjectHelper;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.base.dept.entity.Dept;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCurrency;
import com.midea.cloud.srm.model.base.soap.DataSourceEnum;
import com.midea.cloud.srm.model.base.soap.erp.dto.SoapResponse;
import com.midea.cloud.srm.model.pm.pr.division.entity.DivisionCategory;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.RequisitionDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.*;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.pr.division.mapper.DivisionCategoryMapper;
import com.midea.cloud.srm.pr.erp.service.IErpService;
import com.midea.cloud.srm.pr.erp.service.IRequirementLineNonAutoService;
import com.midea.cloud.srm.pr.requirement.mapper.RequirementHeadMapper;
import com.midea.cloud.srm.pr.requirement.service.IRequirementHeadService;
import com.midea.cloud.srm.pr.requirement.service.IRequirementLineService;
import com.midea.cloud.srm.pr.requirement.service.IRequisitionDetailService;
import com.midea.cloud.srm.pr.requirement.service.IRequisitionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  Erp????????????????????? pmp??????
 * </pre>
 *
 * @author xiexh12@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/8/28 14:53
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class ErpServiceImpl implements IErpService {

    /**
     * erp?????????????????????Service
     */
    @Resource
    private IRequisitionService iRequisitionService;

    @Resource
    private DivisionCategoryMapper divisionCategoryMapper;

    /**
     * erp???????????????????????????Service
     */
    @Resource
    private IRequisitionDetailService iRequisitionDetailService;

    /**
     * srm????????????????????????Service
     */
    @Resource
    private IRequirementHeadService iRequirementHeadService;

    /**
     * srm????????????????????????Service
     */
    @Resource
    private IRequirementLineService iRequirementLineService;

    /**
     * feign???????????????baseClient?????????Service
     * ???????????????????????????????????????????????????Id?????????
     */
    @Resource
    private BaseClient baseClient;

    @Resource
    private RbacClient rbacClient;

    @Resource
    private IRequirementLineNonAutoService iRequirementLineNonAutoService;
    @Resource
    private RequirementHeadMapper  requirementHeadMapper;

    //private Map<String, MaterialItem> materialItemMap;
    //private Map<String, PurchaseCategory> purchaseCategorieMAXMap;
    //private Map<Long, PurchaseCategory> purchaseCategorieMINMap;
    private Map<String, PurchaseCurrency> purchaseCurrencieMap;
    //private Map<String, InvOrganization> invOrganizationMap;


    /**
     * ??????/?????? erp????????????(????????????)
     *
     * @param requisitionsList
     * @param instId           ?????????id????????????i???
     * @param requestTime      ??????????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SoapResponse saveOrUpdateRequisitions(List<RequisitionDTO> requisitionsList, String instId, String requestTime) {

        SoapResponse soapResponse = new SoapResponse();
        soapResponse.setSuccess("true");
        SoapResponse.RESPONSE.EsbInfo esbInfo = new SoapResponse.RESPONSE.EsbInfo();

        //????????????????????????
        String returnStatus = "S";//???????????? :S?????? E ??????
        String returnMsg = "";//????????????  ??????????????????????????????????????????
        String returnCode = "200";//????????????

        try {
            Assert.isTrue(StringUtils.isNotEmpty(instId), "?????????????????????????????????????????????");
            Assert.isTrue(StringUtils.isNotEmpty(requestTime), "????????????????????????????????????????????????");
            Assert.isTrue(CollectionUtils.isNotEmpty(requisitionsList), "????????????????????????????????????????????????");
            //??????????????????
            esbInfo.setInstId(instId);//?????????
            esbInfo.setRequestTime(requestTime);//???????????????

            // ----------------------------------------------------------------TODO ??????????????????????????????????????????????????????---------------------------------------------------------------------

            // ---------------------------------------------------------TODO ??????????????????---------------------------------------------------------------
            //????????????
            Map<Long, PurchaseCategory> purchaseCategorieMINMap = new HashMap<>();
            //????????????
            Map<String, PurchaseCategory> purchaseCategorieMAXMap = new HashMap<>();
            //????????????
            //Map<String, InvOrganization> invOrganizationMap=new HashMap<>();
            //????????????
            Map<String, Organization> orgMap = new HashMap<>();
            //????????????
            Map<String, Organization> organizationMap = new HashMap<>();
            //???????????????
            Map<String, User> usersMap = new HashMap<>();
            //???????????????
            List<String> users = new ArrayList<>();
            // ????????????
            List<String> itemCodes = new ArrayList<>();
            // ???????????????ID
            List<Long> requestHeaderIds = new ArrayList<>();
            // ????????????
            List<Long> requisitionLineIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(requisitionsList)) {
                requisitionsList.forEach(requisitionDTO -> {
                    // ?????????id
                    Optional.ofNullable(requisitionDTO.getRequestHeaderId()).ifPresent(id -> requestHeaderIds.add(id));
                    List<RequisitionDetail> requisitionDetailList = requisitionDTO.getRequisitionDetailList();
                    if (CollectionUtils.isNotEmpty(requisitionDetailList)) {
                        // ???????????????id
                        List<Long> lineIds = requisitionDetailList.stream().filter(requisitionDetail -> StringUtil.notEmpty(requisitionDetail.getRequisitionLineId())).
                                map(RequisitionDetail::getRequisitionLineId).collect(Collectors.toList());
                        Optional.ofNullable(lineIds).ifPresent(lines -> requisitionLineIds.addAll(lines));

                        // ??????????????????
                        List<String> itemNumbers = requisitionDetailList.stream().filter(requisitionDetail -> StringUtil.notEmpty(requisitionDetail.getItemNumber())).map(
                                RequisitionDetail::getItemNumber).collect(Collectors.toList());
                        Optional.ofNullable(itemNumbers).ifPresent(coeds -> itemCodes.addAll(coeds));
                        //?????????????????????
                        List<String> useridList = requisitionDetailList.stream().filter(requisitionDetail -> StringUtil.notEmpty(requisitionDetail.getRequestorNumber())).map(
                                RequisitionDetail::getRequestorNumber).collect(Collectors.toList());
                        Optional.ofNullable(useridList).ifPresent(userid -> users.addAll(userid));
                    }
                });
            }

            // ?????????????????????
            Map<Long, Requisition> queryRequisitionMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(requestHeaderIds)) {
                List<Requisition> requisitions = iRequisitionService.list(Wrappers.lambdaQuery(Requisition.class)
                        .in(Requisition::getRequestHeaderId, requestHeaderIds));
                if (CollectionUtils.isNotEmpty(requisitions)) {
                    queryRequisitionMap = requisitions.stream().collect(Collectors.toMap(Requisition::getRequestHeaderId, Function.identity()));
                }
            }

            // ?????????????????????
            Map<Long, RequisitionDetail> requisitionDetailMap = new HashMap<>();
            List<RequisitionDetail> queryRequisitionDetailList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(requisitionLineIds)) {
                queryRequisitionDetailList = iRequisitionDetailService.list(Wrappers.lambdaQuery(RequisitionDetail.class)
                        .in(RequisitionDetail::getRequisitionLineId, requisitionLineIds));
                if (CollectionUtils.isNotEmpty(queryRequisitionDetailList)) {
                    requisitionDetailMap = queryRequisitionDetailList.stream().collect(Collectors.toMap(RequisitionDetail::getRequisitionLineId, Function.identity()));
                }
            }


            /* ????????????????????????????????????????????????????????? */
            Map<String, MaterialItem> materialItemMap=new HashMap<>();
            //  ------------------------------------------------------------------TODO ??????????????????-------------------------------------------------------------------


            //erp??????????????????
            //srm??????????????? TODO ?????????????????????????????????
//            List<Requisition> queryRequisitionList = iRequisitionService.list();
//            Map<Long, Requisition> queryRequisitionMap = queryRequisitionList.stream().collect(Collectors.toMap(Requisition::getRequestHeaderId, Function.identity()));

//            // TODO
//            List<RequirementHead> srmQueryRequirementHeadList = iRequirementHeadService.list();
//            Map<Long, RequirementHead> srmQueryRequirementHeadMap = srmQueryRequirementHeadList.stream().collect(Collectors.toMap(RequirementHead::getRequirementHeadId, Function.identity()));

            //erp??????????????????????????????
            //srm???????????????????????????
//            // TODO ?????????????????????????????????????????????
//            List<RequisitionDetail> queryRequisitionDetailList = iRequisitionDetailService.list();
//            Map<Long, RequisitionDetail> requisitionDetailMap = queryRequisitionDetailList.stream().collect(Collectors.toMap(RequisitionDetail::getRequisitionLineId, Function.identity()));

            // TODO
//            List<RequirementLine> srmQueryRequirementLineList = iRequirementLineService.list();
//            this.requirementLineMap = srmQueryRequirementLineList.stream().collect(Collectors.toMap(RequirementLine::getRequirementLineId, Function.identity()));

//            //?????????????????? TODO ??????????????????????????????
//            List<MaterialItem> materialItemList = baseClient.MaterialItemListAlls();
//            //List<MaterialItem> materialItemList = new ArrayList<>();
//            this.materialItemMap = materialItemList.stream().collect(Collectors.toMap(MaterialItem::getMaterialCode, Function.identity()));


            // --------------------------------------------------------------------TODO ??????????????????????????????????????????????????????----------------------------------------------------------------
            try {
                /* ????????????????????????????????????????????????????????? */
                materialItemMap = baseClient.listMaterialItemsByCodes(itemCodes);
                //?????????????????????
                List<PurchaseCategory> purchaseCategorieList = baseClient.listParamCategoryMin();
                //??????
                purchaseCategorieMAXMap = purchaseCategorieList.stream().filter(purchaseCategory -> 1 == purchaseCategory.getLevel()).collect(Collectors.toMap(PurchaseCategory::getCategoryCode, Function.identity()));
                //??????
                purchaseCategorieMINMap = purchaseCategorieList.stream().filter(purchaseCategory -> 3 == purchaseCategory.getLevel()).collect(Collectors.toMap(PurchaseCategory::getCategoryId, Function.identity()));

                //??????
                List<PurchaseCurrency> purchaseCurrencieList = baseClient.PurchaseCurrencyListAll();
                this.purchaseCurrencieMap = purchaseCurrencieList.stream().collect(Collectors.toMap(PurchaseCurrency::getCurrencyCode, Function.identity()));
                //????????????
                List<Organization> organizations = baseClient.iOrganizationServiceListAll();
                organizationMap = organizations.stream().filter(x -> StringUtils.isNotEmpty(x.getOrganizationCode()) && "INV".equals(x.getOrganizationTypeCode())).collect(Collectors.toMap(Organization::getOrganizationCode, Function.identity(), (O1, O2) -> O1));
                //????????????
                orgMap = organizations.stream().filter(x -> StringUtils.isNotEmpty(x.getErpOrgId()) && "OU".equals(x.getOrganizationTypeCode())).collect(Collectors.toMap(Organization::getErpOrgId, Function.identity(), (O1, O2) -> O1));

                //???????????????
                List<User> usersList = rbacClient.listUsersByUsersParamCode(users);
                usersMap = usersList.stream().collect(Collectors.toMap(User::getCeeaEmpNo, Function.identity(), (O1, O2) -> O1));
            } catch (Exception e) {
                Assert.isTrue(false, e.getMessage() + "???????????????????????????????????????");
            }

            /** ??????RequisitionWsService????????????requisitionList */
            //List<Requisition> copyRequisitionList = requisitionsList;

            //????????????
            List<Requisition> saveRequisitionList = new ArrayList<>();
            List<Requisition> updateRequisitionList = new ArrayList<>();
            //????????????
            List<RequisitionDetail> saveRequisitionDetailList = new ArrayList<>();
            List<RequisitionDetail> updateRequisitionDetailList = new ArrayList<>();

            //srm?????????
            List<RequirementHead> saveSrmRequirementHeadList = new ArrayList<>();
            List<RequirementHead> updateSrmRequirementHeadList = new ArrayList<>();
            //srm?????????
            List<RequirementLine> saveSrmRequirementLineList = new ArrayList<>();
            List<RequirementLine> updateSrmRequirementLineList = new ArrayList<>();


            //????????????(??????????????????)
            Map<Long, RequisitionDetail> EsaveRequisitionDetailMap = new HashMap<>();
            Map<Long, RequisitionDetail> EupdateRequisitionDetailMap = new HashMap<>();


            outter:
            for (RequisitionDTO requisitionDTO : requisitionsList) {
                //???????????????????????????????????????
                Requisition byId1 = iRequisitionService.getById(requisitionDTO.getRequestHeaderId());
                if (byId1!=null){
                    continue outter;
                }
                //????????????????????????????????????????????????
                if (StringUtils.isNotEmpty(requisitionDTO.getRequestNumber())){
                    Integer requestNumber = requirementHeadMapper.getRequestNumber(requisitionDTO.getRequestNumber());
                    if (null!=requestNumber&&0<requestNumber){
                        requisitionDTO.setHeadImportStatus(2);
                        saveRequisitionList.add(requisitionDTO);
                        List<RequisitionDetail> requisitionDetailList = requisitionDTO.getRequisitionDetailList();
                        if (CollectionUtils.isNotEmpty(requisitionDetailList)){
                            for (RequisitionDetail requisitionDetail:requisitionDetailList){
                                requisitionDetail.setRequestHeaderId(requisitionDTO.getRequestHeaderId());
                                requisitionDetail.setLineImportStatus(2);
                                saveRequisitionDetailList.add(requisitionDetail);
                            }
                        }
                        continue outter;
                    }
                }
                //?????????????????????
                Organization organization = orgMap.get("" + requisitionDTO.getOperationUnitId());
                if (organization != null) {
                    requisitionDTO.setOrgId(organization.getOrganizationId());
                    requisitionDTO.setOrgCode(organization.getOrganizationCode());
                    requisitionDTO.setOrgName(organization.getOrganizationName());
                }
//???????????????????????????????????????????????????????????????,??????????????????????????????????????????-------------------------------------------------------------------------------------------------------
                if (CollectionUtils.isEmpty(requisitionDTO.getRequisitionDetailList())) {
                    if (queryRequisitionMap.get(requisitionDTO.getRequestHeaderId()) != null) {
                        updateRequisitionList.add(requisitionDTO);
                        //??????srm?????????????????????????????????
                        List<RequirementHead> requirementHeads = saveAndUpdateRequisition(queryRequisitionDetailList, requisitionDTO, true, purchaseCategorieMAXMap, organizationMap);
                        updateSrmRequirementHeadList.addAll(requirementHeads);
                    } else {
                        //????????????????????????????????????srm???????????????????????????????????????
                        requisitionDTO.setRequirementHeadId(IdGenrator.generate());
                        requisitionDTO.setRequirementHeadNum(baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM));
                        saveRequisitionList.add(requisitionDTO);
                        //??????srm????????????????????????
                        List<RequirementHead> requirementHeads = saveAndUpdateRequisition(null, requisitionDTO, false, purchaseCategorieMAXMap, organizationMap);
                        saveSrmRequirementHeadList.addAll(requirementHeads);
                    }
                    continue outter;
                }
//????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????--------------------------------------------------------------------------------------
                //??????????????????
                List<RequisitionDetail> requisitionDetailLists = requisitionDTO.getRequisitionDetailList();
                ArrayList<String> stringList = new ArrayList<>();

                List<RequisitionDetail> requisitionDetailList = new ArrayList<>();
                //????????????
                Map<String, DivisionCategory> OrgByPurchaseCategoryMap = new HashMap<>();


                //==================================================================??????????????????????????????????????????======================================================================
                //????????????
                HashMap<String, Long> stringLongHashMap = new HashMap<>();
                //???????????????????????????????????????
                for (RequisitionDetail requisitionDetail : requisitionDetailLists) {
                    try {
                        stringList.add(baseClient.seqGenForAnon(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM));
                    } catch (Exception e) {
                        Assert.isTrue(false, "??????????????????????????????????????????");
                    }
                    //================================?????????????????????????????????????????????????????????????????????????????????=====================================
                    MaterialItem materialItem = null;
                    PurchaseCategory purchaseCategory = null;
                    PurchaseCategory purchaseCategoryMix = null;
                    try {
                        Assert.isTrue(StringUtils.isNotEmpty(requisitionDetail.getNeedByDate()), "????????????????????????");
                        materialItem = materialItemMap.get(requisitionDetail.getItemNumber());
                        Assert.notNull(materialItem, "????????????????????????.");
                        Assert.notNull(materialItem.getCategoryId(), "???????????????"+requisitionDetail.getItemNumber()+"????????????????????????????????????");
                        purchaseCategory = purchaseCategorieMINMap.get(materialItem.getCategoryId());
                        Assert.notNull(purchaseCategory, "????????????????????????");
                        String categoryMixCode = purchaseCategory.getCategoryCode().substring(0, 2);
                        purchaseCategoryMix = purchaseCategorieMAXMap.get(categoryMixCode);
                        Assert.isTrue(StringUtils.isNotEmpty(requisitionDetail.getUnitOfMeasure()), "?????????????????????");
                    } catch (Exception e) {
                        log.info(e.getMessage());
                        requisitionDetail.setRequestHeaderId(requisitionDTO.getRequestHeaderId());
                        requisitionDetail.setLineImportStatus(1);
                        RequisitionDetail byId = iRequisitionDetailService.getById(requisitionDetail.getRequisitionLineId());
                        if (byId == null) {
                            EsaveRequisitionDetailMap.put(requisitionDetail.getRequisitionLineId(), requisitionDetail);
                        } else {
                            EupdateRequisitionDetailMap.put(requisitionDetail.getRequisitionLineId(), requisitionDetail);
                        }
                        continue;
                    }
                    //=============================================?????????????????????????????????????????????=================================
                    //????????????????????????
                    requisitionDetail.setCategoryId(purchaseCategory.getCategoryId())
                            .setCategoryCode(purchaseCategory.getCategoryCode())
                            .setCategoryName(purchaseCategory.getCategoryName());
                    //??????????????????
                    if (purchaseCategoryMix!=null){
                        requisitionDetail.setCategoryMixId(purchaseCategoryMix.getCategoryId())
                                .setCategoryMixCode(purchaseCategoryMix.getCategoryCode())
                                .setCategoryMixName(purchaseCategoryMix.getCategoryName());
                    }

                    if (StringUtils.isNotEmpty(requisitionDetail.getRequireOrgCode()) && materialItem.getCategoryId() != null) {
                        stringLongHashMap.put(requisitionDetail.getRequireOrgCode(), materialItem.getCategoryId());
                    }
                    requisitionDetailList.add(requisitionDetail);
                }
                if (!stringLongHashMap.isEmpty()) {
                    List<DivisionCategory> listOrgByPurchaseCategory = divisionCategoryMapper.getListOrgByPurchaseCategory(stringLongHashMap);
                    OrgByPurchaseCategoryMap = listOrgByPurchaseCategory.stream().collect(Collectors.toMap(x -> x.getOrganizationCode() + x.getCategoryId() + x.getIfMainPerson() + x.getDuty(), part -> part, (O1, O2) -> O1));

                }

                //============================================????????????????????????????????????????????????????????????==============================================================
                if (CollectionUtils.isEmpty(requisitionDetailList)) {
                    Requisition byId = iRequisitionService.getById(requisitionDTO.getRequestHeaderId());
                    requisitionDTO.setHeadImportStatus(1);
                    if (byId == null) {
                        saveRequisitionList.add(requisitionDTO);
                    } else {
                        updateRequisitionList.add(requisitionDTO);
                    }
                    continue;
                }
                //????????????????????????
                Collections.sort(requisitionDetailList, new Comparator<RequisitionDetail>() {
                    @Override
                    public int compare(RequisitionDetail o1, RequisitionDetail o2) {
                        Assert.isTrue(StringUtils.isNotEmpty(o1.getRequireOrgCode()), "????????????????????????");
                        if (o1.getRequireOrgCode().compareTo(o2.getRequireOrgCode()) == 0) {
                            return o1.getCategoryCode().compareTo(o2.getCategoryCode());
                        }
                        return o1.getRequireOrgCode().compareTo(o2.getRequireOrgCode());
                    }
                });
                //????????????
                int requisitionDetailSize = requisitionDetailList.size();
                boolean falg = true;
                outter2:
                for (int i = 0; i < requisitionDetailSize; i++) {
                    RequisitionDetail requisitionDetail = requisitionDetailList.get(i);
                    //???????????????
                    RequisitionDetail requisitionDetails = requisitionDetailMap.get(requisitionDetail.getRequisitionLineId());
                    if (null != requisitionDetails) {
                        //??????erp??????
                        updateRequisitionDetailList.add(requisitionDetail
                                .setRequirementHeadId(requisitionDetails.getRequirementHeadId())
                                .setRequirementLineId(requisitionDetails.getRequirementLineId())
                                .setRequirementHeadNum(requisitionDetails.getRequirementHeadNum()));
                        //??????srm??????
                        requisitionDetail
                                .setOperationUnitId(requisitionDTO.getOperationUnitId())
                                .setOperationName(requisitionDTO.getOperationName());
                        updateSrmRequirementLineList.add(saveAndUpdateRequisitionDetail(requisitionDetail, true, materialItemMap, organizationMap, usersMap, OrgByPurchaseCategoryMap));
                        continue outter2;
                    }
                    falg = false;
                    //??????????????????
                    //??????????????????????????????????????????????????????srm??????
                    if (i > 0) {
                        //??????????????????????????????????????????erp?????????erp????????????srm?????????srm?????????
                        if (!requisitionDetail.getRequireOrgCode().equals(requisitionDetailList.get(i - 1).getRequireOrgCode())) {
                            //??????erp??????????????????id
                            requisitionDetail.setRequestHeaderId(requisitionDTO.getRequestHeaderId());
                            //?????????srm????????????
                            requisitionDTO.setCategoryMixCode(requisitionDetail.getCategoryMixCode());
                            //??????srm??????id
                            long headId = IdGenrator.generate();
                            requisitionDetail.setRequirementHeadId(headId);
                            requisitionDTO.setRequirementHeadId(headId);
                            //??????srm??????id
                            requisitionDetail.setRequirementLineId(IdGenrator.generate());
                            //??????srm??????????????????
                            requisitionDetail.setOperationUnitId(requisitionDTO.getOrgId());
                            requisitionDetail.setOperationUnitCode(requisitionDTO.getOrgCode());
                            requisitionDetail.setOperationName(requisitionDTO.getOrgName());


                            //????????????????????????
                            User user = usersMap.get(requisitionDetail.getRequestorNumber());
                            if (user != null) {

                                if (StringUtils.isNotEmpty(user.getCeeaDeptId())){
                                    QueryWrapper<Dept> wrapper = new QueryWrapper<>();
                                    wrapper.select("ID,DEPTID,DESCR");
                                    wrapper.eq("DEPTID",user.getCeeaDeptId());
                                    List<Dept> deptList = baseClient.getDeptListbyWrapper(wrapper);
                                    if (CollectionUtils.isNotEmpty(deptList)){
                                        requisitionDTO.setCeeaDepartmentId(deptList.get(0).getDeptid());
                                    }
                                }
                                requisitionDTO.setCeeaDepartmentCode(user.getCeeaDeptId());
                                requisitionDTO.setCeeaDepartmentName(user.getDepartment());
                            }

                            // ??????srm????????????
                            String seqPmpPrApplyNum = stringList.get(i);
                            requisitionDetail.setRequirementHeadNum(seqPmpPrApplyNum);
                            requisitionDTO.setRequirementHeadNum(seqPmpPrApplyNum);
                            //????????????????????????
                            requisitionDTO.setOrganizationCode(requisitionDetail.getRequireOrgCode());
                            //??????erp??????
                            saveRequisitionDetailList.add(requisitionDetail);
                            //??????srm??????
                            saveSrmRequirementLineList.add(saveAndUpdateRequisitionDetail(requisitionDetail, false, materialItemMap, organizationMap, usersMap, OrgByPurchaseCategoryMap));
                            //??????srm??????
                            List<RequirementHead> requirementHeads = saveAndUpdateRequisition(null, requisitionDTO, false, purchaseCategorieMAXMap, organizationMap);
                            saveSrmRequirementHeadList.addAll(requirementHeads);
                            //???????????????????????????erp?????????srm??????
                        } else {
                            RequisitionDetail requisitionDetailCopy = requisitionDetailList.get(i - 1);
                            //?????????srm????????????
                            requisitionDTO.setCategoryMixCode(requisitionDetailCopy.getCategoryMixCode());
                            //??????erp??????????????????id
                            requisitionDetail.setRequestHeaderId(requisitionDTO.getRequestHeaderId());
                            //????????????????????????
                            requisitionDTO.setOrganizationCode(requisitionDetail.getRequireOrgCode());
                            //??????srm??????id
                            long headId = requisitionDetailCopy.getRequirementHeadId();
                            requisitionDetail.setRequirementHeadId(headId);
                            //??????srm??????id
                            requisitionDetail.setRequirementLineId(IdGenrator.generate());
                            //????????????????????????
                            User user = usersMap.get(requisitionDetail.getRequestorNumber());
                            if (user != null) {
                                if (StringUtils.isNotEmpty(user.getCeeaDeptId())){
                                    QueryWrapper<Dept> wrapper = new QueryWrapper<>();
                                    wrapper.select("ID,DEPTID,DESCR");
                                    wrapper.eq("DEPTID",user.getCeeaDeptId());
                                    List<Dept> deptList = baseClient.getDeptListbyWrapper(wrapper);
                                    if (CollectionUtils.isNotEmpty(deptList)){
                                        requisitionDTO.setCeeaDepartmentId(deptList.get(0).getDeptid());
                                    }
                                }
                                requisitionDTO.setCeeaDepartmentCode(user.getCeeaDeptId());
                                requisitionDTO.setCeeaDepartmentName(user.getDepartment());
                            }
                            //??????srm??????????????????
                            requisitionDetail.setOperationUnitId(requisitionDTO.getOrgId());
                            requisitionDetail.setOperationUnitCode(requisitionDTO.getOrgCode());
                            requisitionDetail.setOperationName(requisitionDTO.getOrgName());
                            //??????srm????????????
                            String seqPmpPrApplyNum = requisitionDetailCopy.getRequirementHeadNum();
                            requisitionDetail.setRequirementHeadNum(seqPmpPrApplyNum);
                            //??????erp??????
                            saveRequisitionDetailList.add(requisitionDetail);
                            //??????srm??????
                            saveSrmRequirementLineList.add(saveAndUpdateRequisitionDetail(requisitionDetail, false, materialItemMap, organizationMap, usersMap, OrgByPurchaseCategoryMap));
                        }
                        continue outter2;
                    } else {
                        //??????erp??????????????????id
                        requisitionDetail.setRequestHeaderId(requisitionDTO.getRequestHeaderId());
                        //?????????srm????????????
                        requisitionDTO.setCategoryMixCode(requisitionDetail.getCategoryMixCode());
                        //??????srm??????id
                        long headId = IdGenrator.generate();
                        requisitionDetail.setRequirementHeadId(headId);
                        requisitionDTO.setRequirementHeadId(headId);
                        //??????srm??????id
                        requisitionDetail.setRequirementLineId(IdGenrator.generate());
                        //??????srm????????????
                        String seqPmpPrApplyNum = stringList.get(i);
                        requisitionDetail.setRequirementHeadNum(seqPmpPrApplyNum);
                        requisitionDTO.setRequirementHeadNum(seqPmpPrApplyNum);
                        //????????????????????????
                        User user = usersMap.get(requisitionDetail.getRequestorNumber());
                        if (user != null) {
                            if (StringUtils.isNotEmpty(user.getCeeaDeptId())){
                                QueryWrapper<Dept> wrapper = new QueryWrapper<>();
                                wrapper.select("ID,DEPTID,DESCR");
                                wrapper.eq("DEPTID",user.getCeeaDeptId());
                                List<Dept> deptList = baseClient.getDeptListbyWrapper(wrapper);
                                if (CollectionUtils.isNotEmpty(deptList)){
                                    requisitionDTO.setCeeaDepartmentId(deptList.get(0).getDeptid());
                                }
                            }
                            requisitionDTO.setCeeaDepartmentCode(user.getCeeaDeptId());
                            requisitionDTO.setCeeaDepartmentName(user.getDepartment());
                        }
                        //??????srm??????????????????
                        requisitionDetail.setOperationUnitId(requisitionDTO.getOrgId());
                        requisitionDetail.setOperationUnitCode(requisitionDTO.getOrgCode());
                        requisitionDetail.setOperationName(requisitionDTO.getOrgName());
                        //????????????????????????
                        requisitionDTO.setOrganizationCode(requisitionDetail.getRequireOrgCode());
                        //??????erp??????
                        saveRequisitionDetailList.add(requisitionDetail);
                        //??????srm??????
                        saveSrmRequirementLineList.add(saveAndUpdateRequisitionDetail(requisitionDetail, false, materialItemMap, organizationMap, usersMap, OrgByPurchaseCategoryMap));
                        //??????erp?????????
                        Requisition byId = iRequisitionService.getById(requisitionDTO.getRequestHeaderId());
                        if (byId==null){
                            saveRequisitionList.add(requisitionDTO);
                        }
                        //??????srm??????
                        List<RequirementHead> requirementHeads = saveAndUpdateRequisition(null, requisitionDTO, false, purchaseCategorieMAXMap, organizationMap);
                        saveSrmRequirementHeadList.addAll(requirementHeads);
                        continue outter2;
                    }
                }
                //???????????????????????????????????????????????????
                if (falg) {
                    //??????rep?????????
                    //updateRequisitionList.add(requisitionDTO);
                    //??????srm?????????????????????????????????
                    List<RequirementHead> requirementHeads = saveAndUpdateRequisition(queryRequisitionDetailList, requisitionDTO, true, purchaseCategorieMAXMap, organizationMap);
                    updateSrmRequirementHeadList.addAll(requirementHeads);
                }
            }

            if (!EsaveRequisitionDetailMap.isEmpty()) {
                List<RequisitionDetail> collect = EsaveRequisitionDetailMap.entrySet().stream().map(et -> et.getValue()).collect(Collectors.toList());
                saveRequisitionDetailList.addAll(collect);
            }
            if (!EupdateRequisitionDetailMap.isEmpty()) {
                List<RequisitionDetail> collect = EsaveRequisitionDetailMap.entrySet().stream().map(et -> et.getValue()).collect(Collectors.toList());
                updateRequisitionDetailList.addAll(collect);
            }


            iRequisitionService.saveBatch(saveRequisitionList);
            iRequisitionService.updateBatchById(updateRequisitionList);
            iRequisitionDetailService.saveBatch(saveRequisitionDetailList);
            iRequisitionDetailService.updateBatchById(updateRequisitionDetailList);
            iRequirementHeadService.saveBatch(saveSrmRequirementHeadList);
            iRequirementHeadService.updateBatchById(updateSrmRequirementHeadList);
            //??????????????????
            if (CollectionUtils.isNotEmpty(saveSrmRequirementLineList)) {
                ArrayList<RequirementLineNonAuto> RequirementLineNonAutoList = new ArrayList<>();
                for (RequirementLine requirementLine : saveSrmRequirementLineList) {
                    RequirementLineNonAuto requirementLineNonAuto = new RequirementLineNonAuto();
                    //??????????????????
                    InjectDefMetaObjectHelper.setDefCreateAndUpdateFieldValue(requirementLineNonAuto);
                    if (requirementLine.getCreatedId() == null) {
                        requirementLine.setCreatedId(-1L);
                        requirementLine.setCreatedBy("???????????????");
                        requirementLine.setCreatedFullName("???????????????");
                    }
                    requirementLine.setCreatedByIp("10.0.3.217");
                    requirementLine.setLastUpdatedId(-1L);
                    requirementLine.setLastUpdatedBy("???????????????");
                    requirementLine.setLastUpdatedByIp("10.0.3.217");

                    BeanCopyUtil.copyProperties(requirementLineNonAuto,requirementLine);
                    RequirementLineNonAutoList.add(requirementLineNonAuto);
                }
                iRequirementLineNonAutoService.saveBatch(RequirementLineNonAutoList);
            }
            //iRequirementLineService.saveBatch(saveSrmRequirementLineList);
            iRequirementLineService.updateBatchById(updateSrmRequirementLineList);

        } catch (Exception e) {
            log.error("????????????", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            returnStatus = "E";//???????????? :S?????? E ??????
            returnMsg = StringUtils.isEmpty(e.getMessage()) ? "" : e.getMessage();//????????????  ??????????????????????????????????????????
            returnCode = "500";//????????????
        }

        esbInfo.setReturnStatus(returnStatus);//????????????
        esbInfo.setReturnCode(returnCode);//????????????
        esbInfo.setReturnMsg(returnMsg);//????????????
        esbInfo.setResponseTime(DateUtil.format(new Date()));//???????????????

        SoapResponse.RESPONSE response = new SoapResponse.RESPONSE();
        response.setEsbInfo(esbInfo);
        soapResponse.setResponse(response);

        return soapResponse;
    }


    /**
     * ???????????????srm???????????????
     *
     * @param requisitionDetailList
     * @param requisitionDTO
     * @param falg
     * @return
     */
    public List<RequirementHead> saveAndUpdateRequisition(List<RequisitionDetail> requisitionDetailList, RequisitionDTO requisitionDTO, Boolean falg, Map<String, PurchaseCategory> purchaseCategorieMAXMap, Map<String, Organization> organizationMap) throws Exception {
        //srm???list
        List<RequirementHead> SrmRequirementHeadList = new ArrayList<>();
        //??????true?????????
        if (falg) {
            //??????erp???????????????
            if (CollectionUtils.isEmpty(requisitionDetailList)) {
                return SrmRequirementHeadList;
            }
            //???????????????
            for (RequisitionDetail requisitionDetail : requisitionDetailList) {
                //???????????????????????????srm??????id
                if (requisitionDTO.getRequestHeaderId().equals(requisitionDetail.getRequestHeaderId())) {
                    //???????????????,???????????????????????????
                    RequirementHead byId = iRequirementHeadService.getById(requisitionDTO.getRequirementHeadId());
                    //????????????????????????srm????????????
                    if (null != byId) {
                        //????????????
                        SrmRequirementHeadList.add(RequisitionTurnRequirementHead(requisitionDTO, byId, purchaseCategorieMAXMap, organizationMap));
                    }
                }
            }
            return SrmRequirementHeadList;
        }
        //false????????????
        SrmRequirementHeadList.add(RequisitionTurnRequirementHead(requisitionDTO, null, purchaseCategorieMAXMap, organizationMap));
        return SrmRequirementHeadList;
    }

    /**
     * ???????????????
     *
     * @param requisitionDTO
     * @param requirementHead
     * @return
     */
    public RequirementHead RequisitionTurnRequirementHead(RequisitionDTO requisitionDTO, RequirementHead requirementHead, Map<String, PurchaseCategory> purchaseCategorieMAXMap, Map<String, Organization> organizationMap) throws Exception {
        if (requirementHead == null) {
            requirementHead = new RequirementHead();
            //????????????
            requirementHead.setApplyDate(DateUtil.dateToLocalDate(new Date()));
            //??????????????????
            requirementHead.setCeeaPurchaseType("PURCHASE".equals(requisitionDTO.getDocumentType()) ? PurchaseType.NORMAL.getValue() : requisitionDTO.getDocumentType());
            //erp????????????
            requirementHead.setEsRequirementHeadNum(requisitionDTO.getRequestNumber());
            //srm???????????????id
            requirementHead.setRequirementHeadId(requisitionDTO.getRequirementHeadId());
            //??????????????????(????????????)
            requirementHead.setRequirementHeadNum(requisitionDTO.getRequirementHeadNum());
            //????????????
            requirementHead.setCeeaProjectName(requisitionDTO.getProjectName());
            //????????????
            requirementHead.setCeeaProjectNum(requisitionDTO.getProjectNumber());
            //CANCELLED+APPROVED??????
            requirementHead.setAuditStatus(RequirementApproveStatus.APPROVED.getValue().equals(requisitionDTO.getAuthStatus()) ? requisitionDTO.getAuthStatus() : RequirementApproveStatus.ABANDONED.getValue());

            //???????????????????????????Id????????? ?????????)
            requirementHead.setOrgId(requisitionDTO.getOrgId())
                    .setOrgCode(requisitionDTO.getOrgCode())
                    .setOrgName(requisitionDTO.getOrgName());
            //????????????
            Organization organization = organizationMap.get(requisitionDTO.getOrganizationCode());
            if (organization != null) {
                requirementHead.setOrganizationId(Long.valueOf(organization.getOrganizationId()))
                        .setOrganizationCode(organization.getOrganizationCode())
                        .setOrganizationName(organization.getOrganizationName());
            }
            PurchaseCategory purchaseCategory = purchaseCategorieMAXMap.get(requisitionDTO.getCategoryMixCode());
            if (null!=purchaseCategory){
                //??????????????????
                requirementHead.setCategoryId(purchaseCategory.getCategoryId())
                        .setCategoryCode(purchaseCategory.getCategoryCode())
                        .setCategoryName(purchaseCategory.getCategoryName());
            }
        }
        //????????????
        requirementHead.setAuditStatus(requisitionDTO.getAuthStatus());
        //????????????
        requirementHead.setCeeaPrType(requisitionDTO.getDocumentType());
        //??????-->??????
        requirementHead.setComments(requisitionDTO.getDescription());
        //??????????????????
        requirementHead.setSourceSystem(DataSourceEnum.ERP_SYS.getValue());

        return requirementHead;
    }

    /**
     * ??????????????????srm????????????
     *
     * @param
     * @return
     */
    public RequirementLine saveAndUpdateRequisitionDetail(RequisitionDetail requisitionDetail, Boolean falg, Map<String, MaterialItem> materialItemMap, Map<String, Organization> organizationMap, Map<String, User> usersMap, Map<String, DivisionCategory> OrgByPurchaseCategoryMap) throws Exception {
        //?????????true??????
        if (falg) {
            //????????????srm???????????????????????????????????????
            if (null == requisitionDetail.getRequisitionLineId()) {
                throw new Exception(LocaleHandler.getLocaleMsg("????????????????????????id?????????????????????;"));
            }
            RequirementLine requirementLine = iRequirementLineService.getById(requisitionDetail.getRequisitionLineId());

            if (requirementLine != null) {
                //??????????????????
                return this.requirementLineTurnRequirementLine(requisitionDetail, requirementLine, materialItemMap, organizationMap, usersMap, OrgByPurchaseCategoryMap);
            }
        }
        //??????????????????
        return this.requirementLineTurnRequirementLine(requisitionDetail, null, materialItemMap, organizationMap, usersMap, OrgByPurchaseCategoryMap);
    }

    /**
     * srm???????????????
     *
     * @param requisitionDetail
     * @param requirementLine
     * @return
     */
    public RequirementLine requirementLineTurnRequirementLine(RequisitionDetail requisitionDetail, RequirementLine requirementLine, Map<String, MaterialItem> materialItemMap, Map<String, Organization> organizationMap, Map<String, User> usersMap, Map<String, DivisionCategory> OrgByPurchaseCategoryMap) throws Exception {
        if (requirementLine == null) {
            requirementLine = new RequirementLine();

            //??????---???????????????????????????
            requirementLine.setRowNum(requisitionDetail.getLineNumber() != null ? requisitionDetail.getLineNumber().intValue() : 0);
            //????????????????????????
            User user = usersMap.get(requisitionDetail.getRequestorNumber());
            if (user != null) {
                requirementLine.setCreatedId(user.getUserId());
                requirementLine.setCreatedBy(user.getUsername());
                requirementLine.setCreatedFullName(user.getNickname());
                requirementLine.setRequirementDepartment(user.getDepartment());
            }
            //????????????
            DivisionCategory purchaseStrategy = OrgByPurchaseCategoryMap.get(requisitionDetail.getRequireOrgCode() + requisitionDetail.getCategoryId() + "Y" + DUTY.Purchase_Strategy.toString());
            //????????????
            DivisionCategory carryOut = OrgByPurchaseCategoryMap.get(requisitionDetail.getRequireOrgCode() + requisitionDetail.getCategoryId() + "Y" + DUTY.Carry_Out.toString());
            if (purchaseStrategy != null) {
                requirementLine.setCeeaStrategyUserId(purchaseStrategy.getPersonInChargeUserId());
                requirementLine.setCeeaStrategyUserNickname(purchaseStrategy.getPersonInChargeNickname());
                requirementLine.setCeeaStrategyUserName(purchaseStrategy.getPersonInChargeUsername());
            }
            if (carryOut != null) {
                requirementLine.setCeeaPerformUserId(carryOut.getPersonInChargeUserId());
                requirementLine.setCeeaPerformUserNickname(carryOut.getPersonInChargeNickname());
                requirementLine.setCeeaPerformUserName(carryOut.getPersonInChargeUsername());

            }
            //????????????
            if (purchaseStrategy != null && carryOut != null) {
                requirementLine.setApplyStatus("ASSIGNED");
            } else {
                requirementLine.setApplyStatus("UNASSIGNED");
            }
            //??????
            requirementLine.setProjectId(StringUtils.isNotEmpty(requisitionDetail.getProjectId())?Long.valueOf(requisitionDetail.getProjectId()):null);
            requirementLine.setProjectNumber(requisitionDetail.getProjectNumber());
            requirementLine.setProjectName(requisitionDetail.getProjectName());
            //??????
            requirementLine.setTaskId(StringUtils.isNotEmpty(requisitionDetail.getTaskId())?Long.valueOf(requisitionDetail.getTaskId()):null);
            requirementLine.setTaskNumber(requisitionDetail.getTaskNumber());
            requirementLine.setTaskName(requisitionDetail.getTaskName());

            //srm???????????????id
            requirementLine.setRequirementHeadId(requisitionDetail.getRequirementHeadId());
            //??????????????????(????????????)
            requirementLine.setRequirementHeadNum(requisitionDetail.getRequirementHeadNum());
            //??????srm??????id
            requirementLine.setRequirementLineId(requisitionDetail.getRequirementLineId());
            //???????????????
            requirementLine.setCeeaExecutedQuantity(new BigDecimal(0));
            //????????????
            //requirementLine.setRequirementQuantity(new BigDecimal(0));
            //???????????????
            requirementLine.setOrderQuantity(new BigDecimal(0));
            //????????????
            requirementLine.setOrgId(requisitionDetail.getOperationUnitId())
                    .setOrgCode(requisitionDetail.getOperationUnitCode())
                    .setOrgName(requisitionDetail.getOperationName());
            //????????????
            Organization organization = organizationMap.get(requisitionDetail.getRequireOrgCode());
            Assert.isTrue(organization != null, "??????????????????????????????srm???????????????????????????????????????????????????????????????");
            requirementLine.setOrganizationId(organization.getOrganizationId())
                    .setOrganizationCode(organization.getOrganizationCode())
                    .setOrganizationName(organization.getOrganizationName());
            //??????
            MaterialItem materialItem = materialItemMap.get(requisitionDetail.getItemNumber());
            Assert.isTrue(!StringUtil.isEmpty(materialItem), "???????????????????????????");
            requirementLine.setMaterialId(materialItem.getMaterialId())
                    .setMaterialCode(materialItem.getMaterialCode())
                    .setMaterialName(requisitionDetail.getItemDescr());
            //????????????
            requirementLine.setCategoryId(requisitionDetail.getCategoryId())
                    .setCategoryCode(requisitionDetail.getCategoryCode())
                    .setCategoryName(requisitionDetail.getCategoryName());
            //??????
            PurchaseCurrency purchaseCurrency = this.purchaseCurrencieMap.get(requisitionDetail.getCurrencyCode());
            requirementLine.setCurrencyId(purchaseCurrency.getCurrencyId())
                    .setCurrency(purchaseCurrency.getCurrencyCode())
                    .setCurrencyName(purchaseCurrency.getCurrencyName());
            //???????????????????????????
            requirementLine.setNotaxPrice(requisitionDetail.getPrice().equals(BigDecimal.ZERO) ? BigDecimal.ZERO : requisitionDetail.getPrice());
            //??????????????????????????????
            requirementLine.setRequirementQuantity(requisitionDetail.getQuantity().equals(BigDecimal.ZERO) ? BigDecimal.ZERO : requisitionDetail.getQuantity());
            //?????????????????????
            requirementLine.setOrderQuantity(requisitionDetail.getQuantity().equals(BigDecimal.ZERO) ? BigDecimal.ZERO : requisitionDetail.getQuantity());
            //??????
            requirementLine.setUnitCode(requisitionDetail.getUnitOfMeasure())
                    .setUnit(requisitionDetail.getUnitOfMeasure());
            //??????
            requirementLine.setRequirementSource(requisitionDetail.getSource());
            //??????
            requirementLine.setRequirementDepartment(requisitionDetail.getDepartName());
            //????????????
            requirementLine.setRequirementDate(DateUtil.parseLocalDate(requisitionDetail.getNeedByDate()));
        }

        //????????????
        requirementLine.setRejectReason(requisitionDetail.getCancelReason());
        if (!StringUtil.isEmpty(requisitionDetail.getCancelFlag())) {
            requirementLine.setRejectReason(requisitionDetail.getCancelFlag());
        }
        //??????????????????
        requirementLine.setSourceSystem(DataSourceEnum.ERP_SYS.getValue());

        return requirementLine;
    }

    public List<String> getMaterialItemCodesFromRequisition(List<RequisitionDTO> requisitionsList) {
        List<String> itemCodes = new LinkedList<>();
        if (CollectionUtils.isNotEmpty(requisitionsList)) {
            requisitionsList.forEach(requisitionDTO -> {
                List<RequisitionDetail> requisitionDetailList = requisitionDTO.getRequisitionDetailList();
                if (CollectionUtils.isNotEmpty(requisitionDetailList)) {
                    List<String> itemNumbers = requisitionDetailList.stream().filter(requisitionDetail -> StringUtil.notEmpty(requisitionDetail.getItemNumber())).map(
                            RequisitionDetail::getItemNumber).collect(Collectors.toList());
                    Optional.ofNullable(itemNumbers).ifPresent(coeds -> itemCodes.addAll(coeds));
                }
            });
        }
        return itemCodes;
    }


    /**
     * ??????erp??????????????????
     *
     * @param id
     * @throws Exception
     */
    @Transactional
    @Override
    public void getResetError(Long id) throws Exception {
        //=======================================================??????????????????============================================================================
        //??????????????????
        List<RequisitionDetail> errorDetailList = iRequisitionDetailService.getErrorDetail(id);
        if (CollectionUtils.isEmpty(errorDetailList)) {
            return;
        }
        //???????????????????????????
        Map<Long, List<RequisitionDetail>> errorDetailMap = errorDetailList.stream().collect(Collectors.groupingBy(RequisitionDetail::getRequestHeaderId));
        //?????????????????????????????????
        Set<Long> getRequestHeaderIdList = errorDetailMap.keySet();
        //List<Long> getRequestHeaderIdList = errorDetailList.stream().map(RequisitionDetail::getRequestHeaderId).distinct().collect(Collectors.toList());
        List<Requisition> resetError = iRequisitionService.getResetError(getRequestHeaderIdList);

        Map<Long, Requisition> resetErrorMap = resetError.stream().collect(Collectors.toMap(Requisition::getRequestHeaderId, Function.identity()));
        //=========================================================????????????????????????==================================================================
        //??????
        List<PurchaseCurrency> purchaseCurrencieList = baseClient.PurchaseCurrencyListAll();
        this.purchaseCurrencieMap = purchaseCurrencieList.stream().collect(Collectors.toMap(PurchaseCurrency::getCurrencyCode, Function.identity()));
        //????????????
        Map<String, MaterialItem> materialItem = getMaterialItem(errorDetailList);
        //?????????????????????
        List<PurchaseCategory> purchaseCategorieList = baseClient.listParamCategoryMin();
        //??????
        Map<String, PurchaseCategory> purchaseCategorieMAXMap = purchaseCategorieList.stream().filter(purchaseCategory -> 1 == purchaseCategory.getLevel()).collect(Collectors.toMap(PurchaseCategory::getCategoryCode, Function.identity()));
        //??????
        Map<Long, PurchaseCategory> purchaseCategorieMINMap = purchaseCategorieList.stream().filter(purchaseCategory -> 3 == purchaseCategory.getLevel()).collect(Collectors.toMap(PurchaseCategory::getCategoryId, Function.identity()));
        //???????????????
        Map<String, User> usersMap = getUserMap(errorDetailList);
        //????????????
        List<Organization> organizations = baseClient.iOrganizationServiceListAll();
        Map<String, Organization> organizationMap = organizations.stream().filter(x -> StringUtils.isNotEmpty(x.getOrganizationCode()) && "INV".equals(x.getOrganizationTypeCode())).collect(Collectors.toMap(Organization::getOrganizationCode, Function.identity(), (O1, O2) -> O1));
        //????????????
        Map<String, Organization> orgMap = organizations.stream().filter(x -> StringUtils.isNotEmpty(x.getErpOrgId()) && "OU".equals(x.getOrganizationTypeCode())).collect(Collectors.toMap(Organization::getErpOrgId, Function.identity(), (O1, O2) -> O1));
        //????????????
        Map<String, DivisionCategory> orgByPurchaseCategoryMap = getOrgByPurchaseCategoryMap(errorDetailList, materialItem, organizationMap);

        //========================================================?????????????????????==========================================================================
        //????????????
        List<Requisition> updateRequisitionList = new ArrayList<>();
        //????????????
        List<RequisitionDetail> updateRequisitionDetailList = new ArrayList<>();
        //srm?????????
        List<RequirementHead> saveSrmRequirementHeadList = new ArrayList<>();
        //srm?????????
        List<RequirementLine> saveSrmRequirementLineList = new ArrayList<>();


        //========================================================??????????????????============================================================================
        //???????????????
        for (Long headerId : getRequestHeaderIdList) {

            //????????????
            List<Requisition> updateRequisitionListCopy = new ArrayList<>();
            //????????????
            List<RequisitionDetail> updateRequisitionDetailListCopy = new ArrayList<>();
            //srm?????????
            List<RequirementHead> saveSrmRequirementHeadListCopy = new ArrayList<>();
            //srm?????????
            List<RequirementLine> saveSrmRequirementLineListCopy = new ArrayList<>();

            //?????????????????????????????????
            List<RequisitionDetail> requisitionDetails = errorDetailMap.get(headerId);
            //(????????????)??????
            Map<String, List<RequisitionDetail>> stringListMap = separateRequisitionDetailByOrg(requisitionDetails);
            Requisition requisition = resetErrorMap.get(headerId);
            for (String orgIdCode : stringListMap.keySet()) {
                //???????????????
                List<RequisitionDetail> requisitionDetailist = stringListMap.get(orgIdCode);
                //??????srm?????????
                RequirementHead requirementHead = getRequirementHead(orgIdCode, requisition, orgMap, organizationMap, purchaseCategorieMAXMap, usersMap, requisitionDetailist.get(0));
                //??????erp??????????????????????????????
                RequirementHead byId = iRequirementHeadService.getById(requirementHead.getRequirementHeadId());
                if (null == byId) {
                    saveSrmRequirementHeadListCopy.add(requirementHead);
                }
                for (RequisitionDetail x:requisitionDetailist){
                    //??????????????????????????????,???????????????
                    Integer rowNumByOne = requirementHeadMapper.getRowNumByOne(new RequirementLine().setRowNum(x.getLineNumber().intValue()).setRequirementHeadNum(requisition.getRequestNumber()));
                    if (rowNumByOne>0){
                        updateRequisitionDetailListCopy.add(x.setLineImportStatus(2));
                        continue;
                    }
                    //????????????
                    RequisitionDetail requisitionDetail=null;
                    try {
                         requisitionDetail = packageRequisitionDetail(x, requisition, requirementHead, materialItem, purchaseCategorieMINMap);
                    }catch (Exception e){
                        log.error(e.getMessage());
                        continue;
                    }
                    requisitionDetail.setRequirementHeadNum(requirementHead.getRequirementHeadNum());
                    requisitionDetail.setRequirementHeadId(requirementHead.getRequirementHeadId());
                    //????????????
                    try {
                        RequirementLine requirementLine = requirementLineTurnRequirementLine(requisitionDetail, null, materialItem, organizationMap, usersMap, orgByPurchaseCategoryMap);
                        saveSrmRequirementLineListCopy.add(requirementLine);
                        updateRequisitionDetailListCopy.add(x.setLineImportStatus(0));
                    } catch (Exception e) {
                        log.info("??????????????????" + x.getRequisitionLineId());
                    }
                }
            }
            //???????????????erp?????????
            updateRequisitionListCopy.add(requisition.setHeadImportStatus(0));
            //????????????erp????????????????????????????????????????????????????????????
            updateRequisitionDetailList.addAll(updateRequisitionDetailListCopy);
            if (CollectionUtils.isNotEmpty(updateRequisitionDetailListCopy)){
                updateRequisitionList.addAll(updateRequisitionListCopy);
            }

            //???????????????srm?????????
            saveSrmRequirementLineList.addAll(saveSrmRequirementLineListCopy);
            //????????????srm????????????????????????????????????????????????????????????
            if (CollectionUtils.isNotEmpty(saveSrmRequirementLineListCopy)){
                saveSrmRequirementHeadList.addAll(saveSrmRequirementHeadListCopy);
            }
        }
        boolean a = true;
        //????????????
        if (a) {
            iRequisitionService.updateBatchById(updateRequisitionList);
            iRequisitionDetailService.updateBatchById(updateRequisitionDetailList);
            iRequirementHeadService.saveBatch(saveSrmRequirementHeadList);
            //??????????????????
            if (CollectionUtils.isNotEmpty(saveSrmRequirementLineList)) {
                ArrayList<RequirementLineNonAuto> RequirementLineNonAutoList = new ArrayList<>();
                for (RequirementLine requirementLine : saveSrmRequirementLineList) {
                    RequirementLineNonAuto requirementLineNonAuto = new RequirementLineNonAuto();
                    //??????????????????
                    InjectDefMetaObjectHelper.setDefCreateAndUpdateFieldValue(requirementLineNonAuto);
                    if (requirementLine.getCreatedId() == null) {
                        requirementLine.setCreatedId(-1L);
                        requirementLine.setCreatedBy("???????????????");
                        requirementLine.setCreatedFullName("???????????????");
                    }
                    requirementLine.setCreatedByIp("10.0.3.217");
                    requirementLine.setLastUpdatedId(-1L);
                    requirementLine.setLastUpdatedBy("???????????????");
                    requirementLine.setLastUpdatedByIp("10.0.3.217");

                    BeanCopyUtil.copyProperties(requirementLineNonAuto, requirementLine);
                    RequirementLineNonAutoList.add(requirementLineNonAuto);
                }
                iRequirementLineNonAutoService.saveBatch(RequirementLineNonAutoList);
            }
        }
    }

    /**
     * (????????????)??????
     *
     * @param requisitionDetailList
     * @return
     */
    public Map<String, List<RequisitionDetail>> separateRequisitionDetailByOrg(List<RequisitionDetail> requisitionDetailList) {
        return requisitionDetailList.stream().collect(Collectors.groupingBy(RequisitionDetail::getRequireOrgCode));
    }

    /**
     * ????????????????????????
     *
     * @param errorDetailList
     * @return
     */
    public Map<String, MaterialItem> getMaterialItem(List<RequisitionDetail> errorDetailList) {
        // ??????????????????
        List<String> itemNumbers = errorDetailList.stream().filter(requisitionDetail -> StringUtil.notEmpty(requisitionDetail.getItemNumber())).map(
                RequisitionDetail::getItemNumber).collect(Collectors.toList());
        return baseClient.listMaterialItemsByCodes(itemNumbers);
    }

    /**
     * ?????????????????????
     *
     * @param errorDetailList
     * @return
     */
    public Map<String, User> getUserMap(List<RequisitionDetail> errorDetailList) {
        //?????????????????????
        List<String> useridList = errorDetailList.stream().filter(requisitionDetail -> StringUtil.notEmpty(requisitionDetail.getRequestorNumber())).map(
                RequisitionDetail::getRequestorNumber).collect(Collectors.toList());
        //???????????????
        List<User> usersList = rbacClient.listUsersByUsersParamCode(useridList);
        //???????????????
        return usersList.stream().collect(Collectors.toMap(User::getCeeaEmpNo, Function.identity(), (O1, O2) -> O1));
    }

    /**
     * ??????????????????
     *
     * @param errorDetailList
     * @param materialItemMap
     * @param organizationMap
     * @return
     */
    public Map<String, DivisionCategory> getOrgByPurchaseCategoryMap(List<RequisitionDetail> errorDetailList, Map<String, MaterialItem> materialItemMap, Map<String, Organization> organizationMap) {
        //????????????
        HashMap<String, Long> stringLongHashMap = new HashMap<>();
        for (RequisitionDetail requisitionDetail : errorDetailList) {
            MaterialItem materialItem = materialItemMap.get(requisitionDetail.getItemNumber());
            Organization organization = organizationMap.get(requisitionDetail.getRequireOrgCode());
            if (StringUtils.isNotEmpty(organization.getOrganizationCode()) && materialItem.getCategoryId() != null) {
                stringLongHashMap.put(organization.getOrganizationCode(), materialItem.getCategoryId());
            }
        }
        if (!stringLongHashMap.isEmpty()) {
            List<DivisionCategory> listOrgByPurchaseCategory = divisionCategoryMapper.getListOrgByPurchaseCategory(stringLongHashMap);
            return listOrgByPurchaseCategory.stream().collect(Collectors.toMap(x -> x.getOrganizationCode() + x.getCategoryId() + x.getIfMainPerson() + x.getDuty(), part -> part, (O1, O2) -> O1));
        }
        return new HashMap<>();
    }

    //????????????????????????????????????/???????????????????????????
    public RequirementHead getRequirementHead(String orgIdCode, Requisition requisition, Map<String, Organization> orgMap, Map<String, Organization> organizationMap, Map<String, PurchaseCategory> purchaseCategorieMAXMap, Map<String, User> usersMap, RequisitionDetail requisitionDetail) throws Exception {
        RequirementHead requirementHead = new RequirementHead();
        //??????erp??????????????????????????????
        //????????????????????????????????????????????????
        //?????????????????????????????????????????????es??????
        Organization org = orgMap.get("" + requisition.getOperationUnitId());
        Organization organization = organizationMap.get(orgIdCode);
        if (null == organization || null == org) {
            Assert.isTrue(false, "?????????????????????/?????????????????????");
        }
        requirementHead.setOrgId(org.getOrganizationId())
                .setOrganizationId(organization.getOrganizationId())
                .setEsRequirementHeadNum(requisition.getRequestNumber());
        requirementHead = iRequirementHeadService.getListByOrgByEs(requirementHead);

        //???????????????????????????????????????
        if (requirementHead == null) {
            //????????????????????????
            RequisitionDTO requisitionDTO = new RequisitionDTO();
            BeanCopyUtil.copyProperties(requisitionDTO, requisition);
            //?????????id
            requisitionDTO.setRequirementHeadId(IdGenrator.generate());
            //????????????
            requisitionDTO.setRequirementHeadNum(baseClient.seqGenForAnon(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM));
            //?????????????????????
            if (org != null) {
                requisitionDTO.setOrgId(org.getOrganizationId());
                requisitionDTO.setOrgCode(org.getOrganizationCode());
                requisitionDTO.setOrgName(org.getOrganizationName());
            }
            //???????????????
            //?????????srm????????????
            //requisitionDTO.setCategoryMixCode(requisitionDetailCopy.getCategoryMixCode());
            //????????????????????????
            Assert.notNull(organization, "??????????????????");
            requisitionDTO.setOrganizationCode(organization.getOrganizationCode());
            //????????????????????????
            User user = usersMap.get(requisitionDetail.getRequestorNumber());
            if (user != null) {
                if (StringUtils.isNotEmpty(user.getCeeaDeptId())) {
                    QueryWrapper<Dept> wrapper = new QueryWrapper<>();
                    wrapper.select("ID,DEPTID,DESCR");
                    wrapper.eq("DEPTID", user.getCeeaDeptId());
                    List<Dept> deptList = baseClient.getDeptListbyWrapper(wrapper);
                    if (CollectionUtils.isNotEmpty(deptList)) {
                        requisitionDTO.setCeeaDepartmentId(deptList.get(0).getDeptid());
                    }
                }
                requisitionDTO.setCeeaDepartmentCode(user.getCeeaDeptId());
                requisitionDTO.setCeeaDepartmentName(user.getDepartment());
            }
            requirementHead = RequisitionTurnRequirementHead(requisitionDTO, null, purchaseCategorieMAXMap, organizationMap);
        }
        return requirementHead;

    }

    /**
     * ?????????
     *
     * @param requisitionDetail
     * @param requisition
     * @param requirementHead
     * @return
     */
    public RequisitionDetail packageRequisitionDetail(RequisitionDetail requisitionDetail, Requisition requisition, RequirementHead requirementHead,Map<String, MaterialItem> materialItemMap,Map<Long, PurchaseCategory> purchaseCategorieMINMap) {
        //??????erp??????????????????id
        requisitionDetail.setRequestHeaderId(requisition.getRequestHeaderId());
        //??????srm??????id
        requisitionDetail.setRequirementHeadId(requirementHead.getRequirementHeadId());
        //??????srm??????id
        requisitionDetail.setRequirementLineId(IdGenrator.generate());
        //??????srm??????????????????
        requisitionDetail.setOperationUnitId(requirementHead.getOrgId());
        requisitionDetail.setOperationUnitCode(requirementHead.getOrgCode());
        requisitionDetail.setOperationName(requirementHead.getOrgName());
        //?????????????????????
        MaterialItem materialItem = materialItemMap.get(requisitionDetail.getItemNumber());
        PurchaseCategory purchaseCategory = purchaseCategorieMINMap.get(materialItem.getCategoryId());
        Assert.notNull(purchaseCategory,"???????????????"+requisitionDetail.getItemNumber()+"???????????????????????????");
        //????????????????????????
        requisitionDetail.setCategoryId(purchaseCategory.getCategoryId())
                .setCategoryCode(purchaseCategory.getCategoryCode())
                .setCategoryName(purchaseCategory.getCategoryName());
        // ??????srm????????????
        requisitionDetail.setRequirementHeadNum(requirementHead.getRequirementHeadNum());
        //???????????????????????????erp?????????srm??????
        return requisitionDetail;
    }


}
