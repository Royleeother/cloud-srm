package com.midea.cloud.srm.base.anon.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.common.utils.redis.RedisUtil;
import com.midea.cloud.srm.base.dept.service.IDeptService;
import com.midea.cloud.srm.base.dict.mapper.DictItemMapper;
import com.midea.cloud.srm.base.dict.mapper.DictMapper;
import com.midea.cloud.srm.base.external.service.IExternalOrderService;
import com.midea.cloud.srm.base.material.service.IMaterialItemService;
import com.midea.cloud.srm.base.material.service.IMaterialOrgService;
import com.midea.cloud.srm.base.organization.service.IInvOrganizationsService;
import com.midea.cloud.srm.base.organization.service.IOrganizationService;
import com.midea.cloud.srm.base.organization.service.IOrganizationUserService;
import com.midea.cloud.srm.base.purchase.service.IPurchaseCategoryService;
import com.midea.cloud.srm.base.purchase.service.IPurchaseCurrencyService;
import com.midea.cloud.srm.base.repush.service.RepushService;
import com.midea.cloud.srm.base.seq.service.ISeqDefinitionService;
import com.midea.cloud.srm.base.table.mapper.TableMapper;
import com.midea.cloud.srm.base.workflow.service.ISrmFlowBusWorkflowService;
import com.midea.cloud.srm.model.base.dept.entity.Dept;
import com.midea.cloud.srm.model.base.dict.entity.Dict;
import com.midea.cloud.srm.model.base.dict.entity.DictItem;
import com.midea.cloud.srm.model.base.external.entity.ExternalOrder;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.material.MaterialOrg;
import com.midea.cloud.srm.model.base.material.vo.MaterialMaxCategoryVO;
import com.midea.cloud.srm.model.base.organization.entity.InvOrganization;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationUser;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCurrency;
import com.midea.cloud.srm.model.base.repush.entity.Repush;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.common.TableField;
import com.midea.cloud.srm.model.common.TableInfo;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import com.netflix.discovery.converters.Auto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ????????????????????????????????????????????????????????????(????????????????????????????????????????????????)
 * </pre>
 *
 * @author chensl26@meicloud.com
 * @version 1.00.00
 * <p>
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-4-10 18:01
 *  ????????????:
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/base-anon/internal")
public class BaseAnonController extends BaseController {
    @Autowired
    private ISrmFlowBusWorkflowService workflowService;

    @Autowired
    private ISeqDefinitionService iSeqDefinitionService;

    @Autowired
    private IOrganizationUserService iOrganizationUserService;

    @Autowired
    private IExternalOrderService iExternalOrderService;

    @Autowired
    private IInvOrganizationsService iInvOrganizationsService;
    @Autowired
    private IMaterialItemService iMaterialItemService;
    @Autowired
    private IPurchaseCategoryService iPurchaseCategoryService;
    @Autowired
    private IPurchaseCurrencyService iPurchaseCurrencyService;
    @Autowired
    private IOrganizationService iOrganizationService;
    @Autowired
    private RepushService repushService;
    @Resource
    private TableMapper tableMapper;
    @Autowired
    private IMaterialOrgService iMaterialOrgService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private DictItemMapper dictItemMapper;
    @Autowired
    private DictMapper dictMapper;
    @Autowired
    private IDeptService iDeptService;

    @Autowired
    private ISrmFlowBusWorkflowService iSrmFlowBusWorkflowService;

    /**
     * ????????????????????????????????????????????????????????????????????????Id????????????????????????
     *
     * @update xiexh12@meicloud.com 2020/09/08
     */
    @PostMapping("/organization/invOrganizations/queryInvByInvCode")
    public InvOrganization queryInvByInvCode(@RequestParam("invCode") String invCode) {
        Assert.notNull(invCode, "??????????????????????????????");
        return iInvOrganizationsService.queryInvByInvCode(invCode);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param sequenceCode
     */
    @GetMapping("/seq/get")
    public String getSeq(String sequenceCode) {
        Assert.notNull(sequenceCode, "?????????????????????");
        return iSeqDefinitionService.genSequencesNumBase(sequenceCode);
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param sequenceCode
     */
    @GetMapping("/seq/batchGetSeq")
    public List<String> batchGetSeq(@RequestParam("sequenceCode") String sequenceCode, @RequestParam("sum") int sum) {
        Assert.notNull(sequenceCode, "?????????????????????");
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < sum; i++) {
            codes.add(iSeqDefinitionService.genSequencesNumBase(sequenceCode));
        }
        return codes;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    @PostMapping("/organization-user/listByParam")
    public List<OrganizationUser> listOrganByParam(@RequestBody OrganizationUser organizationUser) {
        return iOrganizationUserService.listByParam(organizationUser);
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    @PostMapping("/externalOrder/listAll")
    public List<ExternalOrder> listExternalOrder() {
        return iExternalOrderService.list();
    }

    /**
     * ????????????????????????????????????
     */
    @GetMapping("/MaterialItemListAll")
    public List<MaterialItem> MaterialItemListAll() {
        QueryWrapper<MaterialItem> wrapper = new QueryWrapper<>();
        wrapper.select("MATERIAL_ID,MATERIAL_CODE,MATERIAL_NAME,CATEGORY_ID");
        wrapper.groupBy("MATERIAL_CODE");
        return iMaterialItemService.list(wrapper);
    }

    /**
     * ????????????codes??????????????????
     */
    @PostMapping("/materialItem/listMaterialItemsByCodes")
    public Map<String, MaterialItem> listMaterialItemsByCodes(@RequestBody List<String> materialCodes) {
        return iMaterialItemService.listMaterialItemsByCodes(materialCodes);
    }

    /**
     * ????????????codes??????????????????
     */
    @PostMapping("/materialItem/ListMaterialItemByCategoryCode")
    public Map<String, MaterialItem> ListMaterialItemByCategoryCode(@RequestBody List<String> materialCodes) {
        return iMaterialItemService.ListMaterialItemByCategoryCode(materialCodes);
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    @GetMapping("/listParamCategoryMin")
    public List<PurchaseCategory> listParamCategoryMin() {
        QueryWrapper<PurchaseCategory> wrapper = new QueryWrapper<>();
        wrapper.select("CATEGORY_ID,CATEGORY_CODE,CATEGORY_NAME,LEVEL");
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(1);
        integers.add(3);
        wrapper.in("LEVEL", integers);
        wrapper.groupBy("CATEGORY_CODE");
        return iPurchaseCategoryService.list(wrapper);
    }

    /**
     * ??????????????????
     *
     * @return
     */
    @GetMapping("/PurchaseCurrencyListAll")
    public List<PurchaseCurrency> PurchaseCurrencyListAll() {
        QueryWrapper<PurchaseCurrency> wrapper = new QueryWrapper<>();
        wrapper.select("CURRENCY_ID,CURRENCY_CODE,CURRENCY_NAME");
        wrapper.groupBy("CURRENCY_CODE");
        return iPurchaseCurrencyService.list(wrapper);
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    @GetMapping("/InvOrganizationListAll")
    public List<InvOrganization> InvOrganizationListAll() {
        QueryWrapper<InvOrganization> wrapper = new QueryWrapper<>();
        wrapper.select("ES_INV_ORGANIZATION_ID,ES_INV_ORGANIZATION_CODE,INV_ORGANIZATION_NAME");
        //wrapper.isNotNull("ES_INV_ORGANIZATION_ID");
        wrapper.eq("1", "1 \tand\tES_INV_ORGANIZATION_ID\tregexp\t\'(^[0-9])\'");
        //wrapper.ne("ES_INV_ORGANIZATION_ID","");
        wrapper.groupBy("ES_INV_ORGANIZATION_CODE");
        return iInvOrganizationsService.list(wrapper);
    }


    /**
     * ????????????????????????
     *
     * @return
     */
    @GetMapping("/iOrganizationServiceListAll")
    public List<Organization> iOrganizationServiceListAll() {
        QueryWrapper<Organization> wrapper = new QueryWrapper<>();
        wrapper.select("ORGANIZATION_ID,ORGANIZATION_TYPE_CODE,ORGANIZATION_NAME,ORGANIZATION_CODE,ERP_ORG_ID");
        return iOrganizationService.list(wrapper);
    }


    @PostMapping("/purchase/purchaseCategory/getCategoryByCodes")
    public Map<String, PurchaseCategory> getCategoryByNames(@RequestBody Collection<String> codes) {
        return iPurchaseCategoryService.list(Wrappers.lambdaQuery(PurchaseCategory.class)
                .in(PurchaseCategory::getCategoryCode, codes))
                .stream().collect(Collectors.toMap(PurchaseCategory::getCategoryCode, Function.identity()));
    }


    @PostMapping("/organization/organization/getOrganizationsByNames")
    public Map<String, Organization> getOrganizationsByNames(@RequestBody Collection<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return Collections.emptyMap();
        }
        return iOrganizationService.list(Wrappers.lambdaQuery(Organization.class)
                .select(Organization::getOrganizationId, Organization::getOrganizationName, Organization::getOrganizationCode, Organization::getErpOrgId, Organization::getDivisionId, Organization::getDivision, Organization::getCompany)
                .in(Organization::getOrganizationName, names))
                .stream().collect(Collectors.toMap(Organization::getOrganizationName, Function.identity()));
    }

    @PostMapping("/organization/organization/getOrganizationsByIds")
    public List<Organization> getOrganizationsByIds(@RequestBody List<Long> orgIds) {
        if (CollectionUtils.isEmpty(orgIds)) {
            return Collections.emptyList();
        }
        return iOrganizationService.list(Wrappers.lambdaQuery(Organization.class)
                .in(Organization::getOrganizationId, orgIds));
    }

    /*
     * @Description  ??????????????????????????????
     * @Date         2020/11/3 ?????? 09:20
     * @Author       chenjj120@meicloud.com
     * @Param 		organization
     * @return
     **/
    @PostMapping("/organization/listOrganizationByParam")
    List<Organization> listOrganizationByParam(@RequestBody Organization organization) {
        if (Objects.isNull(organization)) {
            return Collections.emptyList();
        }
        return iOrganizationService.list(new QueryWrapper<>(organization));
    }

    /**
     * ????????????????????????
     *
     * @param repush
     */
    @PostMapping("/saveRepush")
    void saveRepush(@RequestBody Repush repush) {
        repushService.save(repush);
    }

    /**
     * ????????????????????????
     *
     * @param repush
     */
    @PostMapping("/updateRepush")
    void updateRepush(@RequestBody Repush repush) {
        repushService.updateById(repush);
    }

    /**
     * ??????????????????N??????????????????N+2????????????
     *
     * @param materialIds
     * @return
     */
    @PostMapping("/queryCategoryMaxCodeByMaterialIds")
    List<MaterialMaxCategoryVO> queryCategoryMaxCodeByMaterialIds(@RequestBody Collection<Long> materialIds) {
        if (CollectionUtils.isEmpty(materialIds)) {
            return Collections.emptyList();
        }
        return iPurchaseCategoryService.queryCategoryMaxCodeByMaterialIds(materialIds);
    }


    /**
     * ????????????????????????????????????????????????
     *
     * @return
     */
    @PostMapping("/organization/organization/getOrganizationByNameList")
    List<Organization> getOrganizationByNameListAnon(@RequestBody List<String> orgNameList) {
        return iOrganizationService.getOrganizationByNameList(orgNameList);
    }


    @GetMapping("/listTable")
    List<TableInfo> listTable() {
        List<TableInfo> tableInfoList = redisUtil.get("ALL_TABLE_CACHE");
        if (Objects.isNull(tableInfoList)) {
            tableInfoList = new ArrayList<>();
            List<String> tableNames = tableMapper.listTable().stream().map(
                    tableInfo -> tableInfo.getTableName()).collect(Collectors.toList()
            );
            List<TableField> tableFieldList = tableMapper.listTableColumn(tableNames);
            for (int j = 0; j < tableNames.size(); j++) {
                TableInfo oneTableInfo = new TableInfo();
                oneTableInfo.setTableName(tableNames.get(j));
                List<TableField> oneTableFields = new ArrayList<>();
                for (int i = 0; i < tableFieldList.size(); i++) {
                    TableField tableField = tableFieldList.get(i);
                    if (tableField.getTableName().equals(tableNames.get(j))) {
                        oneTableFields.add(tableField);
                    }
                }
                oneTableInfo.setTableFields(oneTableFields);
                tableInfoList.add(oneTableInfo);
            }
            redisUtil.set("ALL_TABLE_CACHE", tableInfoList, 86400);
        }
        return tableInfoList;
    }

    /**
     * ?????????????????????
     */
    @GetMapping("/getSrmFlowBusWorkflow")
    public SrmFlowBusWorkflow getSrmFlowBusWorkflow(Long formInstanceId) {
        log.info("??????id???" + formInstanceId);
        return workflowService.getOne(Wrappers.lambdaQuery(SrmFlowBusWorkflow.class).eq(SrmFlowBusWorkflow::getFormInstanceId, formInstanceId));

    }

    /**
     * ????????????????????????
     *
     * @param materialIds
     * @return
     */
    @PostMapping("/listMaterialOrgByMaterialIds")
    public List<MaterialOrg> listMaterialOrgByMaterialIds(@RequestBody Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> materialNotNullIds = materialIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(materialNotNullIds)) {
            return iMaterialOrgService.list(Wrappers.lambdaQuery(MaterialOrg.class)
                    .in(MaterialOrg::getMaterialId, materialNotNullIds));
        }
        return Collections.emptyList();
    }

    /**
     * ??????????????????????????????????????????????????????0????????????
     *
     * @return
     */
    @GetMapping("/purchaseCategory/listMinByDPByLockPeriod")
    public List<PurchaseCategory> listMinByDPByLockPeriod() {
        QueryWrapper<PurchaseCategory> wrapper = new QueryWrapper<>();
        wrapper.select("CATEGORY_ID,CATEGORY_CODE,CATEGORY_NAME,LEVEL,CEEA_LOCK_PERIOD");
        wrapper.eq("CEEA_IF_DELIVER_PLAN", "Y");
        wrapper.eq("LEVEL", "3");
        wrapper.gt("CEEA_LOCK_PERIOD", 0);
        wrapper.groupBy("CATEGORY_ID");
        return iPurchaseCategoryService.list(wrapper);
    }

    @PostMapping("/queryTaxByItemBatch")
    public Map<Long, Set<String>> queryTaxByItemBatch(@RequestBody Collection<Long> materialIds) {
        return iMaterialItemService.queryTaxItemBatch(materialIds);
    }

    @GetMapping("/listFormulaMaterialCode")
    public Set<String> listFormulaMaterialCode(@RequestParam("name") String name) {
        Dict dict = dictMapper.selectOne(Wrappers.lambdaQuery(Dict.class).select(Dict::getDictId, Dict::getDictCode)
                .eq(Dict::getDictCode, name)
        );
        if (Objects.isNull(dict)) {
            return Collections.emptySet();
        }
        return dictItemMapper.selectList(Wrappers.lambdaQuery(DictItem.class)
                .select(DictItem::getDictItemCode)
                .eq(DictItem::getDictId, dict.getDictId())
        ).stream().map(DictItem::getDictItemCode).collect(Collectors.toSet());
    }

    @PostMapping("/listMaterialOrgByMaterialIdsAndInvIds")
    public List<MaterialOrg> listMaterialOrgByMaterialIds(@RequestBody Map<String, Collection<Long>> idMap) {
        Collection<Long> invIds = idMap.get("invIds");
        Collection<Long> materialIds = idMap.get("materialIds");
        if (CollectionUtils.isEmpty(materialIds) || CollectionUtils.isEmpty(invIds)) {
            return Collections.emptyList();
        }
        return iMaterialOrgService.list(Wrappers.lambdaQuery(MaterialOrg.class)
                .in(MaterialOrg::getMaterialId, materialIds)
                .in(MaterialOrg::getOrganizationId, invIds)
        );
    }

    @PostMapping("/dept/getDeptListbyWrapper")
    public  List<Dept> getDeptListbyWrapper(@RequestBody QueryWrapper<Dept> wrapper){
        if (wrapper==null){
            return  null;
        }
        return iDeptService.list(wrapper);
    }

    @GetMapping("/dept/getById")
    public Dept getDeptById(@RequestParam("id") Long id){
        if(Objects.isNull(id)){
            return null;
        }
        return iDeptService.getById(id);
    }

    @PostMapping("/dict/listDictItemByDictCode")
    public List<DictItem> listDictItemByDictCode(@RequestParam("dictCode") String dictCode){
        if(StringUtils.isBlank(dictCode)){
            return Collections.EMPTY_LIST;
        }
        List<Dict> dictList = dictMapper.selectList(new QueryWrapper<Dict>(new Dict().setDictCode(dictCode)));
        if(CollectionUtils.isEmpty(dictList)){
            return Collections.EMPTY_LIST;
        }
        Dict dict = dictList.get(0);
        return dictItemMapper.selectList(new QueryWrapper<DictItem>(new DictItem().setDictId(dict.getDictId())));
    }
	    /**
     * ??????orgERPID??????????????????/????????????
     * @return
     */
    @PostMapping("/organization/listByErpOrgId")
    public List<Organization> listByErpOrgId(@RequestBody List<String> orgErpId) {
       if (CollectionUtils.isEmpty(orgErpId)){
           return new ArrayList<Organization>();
       }
        QueryWrapper<Organization> wrapper = new QueryWrapper<>();
        wrapper.in("ERP_ORG_ID",orgErpId);
        return iOrganizationService.list(wrapper);
    }

    /**
     * ??????????????????code???????????????
     * @param currencyCodeList
     * @return
     */
    @PostMapping("/purchaseCurrency/listPurchaseCurrencyAnon")
    public List<PurchaseCurrency> listPurchaseCurrencyAnon(@RequestBody List<String> currencyCodeList){
        if(CollectionUtils.isEmpty(currencyCodeList)){
            return Collections.EMPTY_LIST;
        }
        QueryWrapper<PurchaseCurrency> wrapper = new QueryWrapper<>();
        wrapper.in("CURRENCY_CODE",currencyCodeList);
        return iPurchaseCurrencyService.list(wrapper);
    }

    /**
     * ????????????id???????????????
     * @param categoryIds
     * @return
     */
    @PostMapping("/purchaseCategory/listPurchaseCategoryAnon")
    public List<PurchaseCategory> listPurchaseCategoryAnon(@RequestBody List<Long> categoryIds){
        if(CollectionUtils.isEmpty(categoryIds)){
            return Collections.EMPTY_LIST;
        }
        QueryWrapper<PurchaseCategory> wrapper = new QueryWrapper<>();
        wrapper.in("CATEGORY_ID",categoryIds);
        return iPurchaseCategoryService.list(wrapper);

    }

    /**
     * ????????????id??????OA??????id
     * @param formId
     * @return
     */
    @GetMapping("/workflow/getFlowIdByFormId")
    public String getFlowIdByFormId(@RequestParam("formId") Long formId) {
        return iSrmFlowBusWorkflowService.getFlowIdByFormId(formId);
    }

}
