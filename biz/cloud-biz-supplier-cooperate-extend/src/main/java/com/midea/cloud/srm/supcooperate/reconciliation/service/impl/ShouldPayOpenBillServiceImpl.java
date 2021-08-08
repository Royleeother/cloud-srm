package com.midea.cloud.srm.supcooperate.reconciliation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.enums.OrgStatus;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationUser;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCurrency;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.OrgInfo;
import com.midea.cloud.srm.model.suppliercooperate.reconciliation.dto.ShouldPayOpenBillRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.reconciliation.entry.ShouldPayOpenBill;
import com.midea.cloud.srm.supcooperate.reconciliation.mapper.ShouldPayOpenBillMapper;
import com.midea.cloud.srm.supcooperate.reconciliation.service.IShouldPayOpenBillService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * <pre>
 *  应付未开票表 接口实现类
 * </pre>
 *
 * @author huangbf3
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/4/2 21:18
 *  修改内容:
 * </pre>
 */
@Service
public class ShouldPayOpenBillServiceImpl extends ServiceImpl<ShouldPayOpenBillMapper, ShouldPayOpenBill> implements IShouldPayOpenBillService {
    @Autowired
    ShouldPayOpenBillMapper shouldPayOpenBillMapper;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private SupplierClient supplierClient;

    @Override
    @Transactional
    public void saveBatchByExcel(List<ShouldPayOpenBill> list) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        List<MaterialItem> materialItems = baseClient.listAllMaterialItem();
        List<OrganizationUser> organizations = loginAppUser.getOrganizationUsers();
        List<DictItemDTO> billTypeDictItems = baseClient.listAllByDictCode("SPOB_BILL_TYPE");
        List<PurchaseCurrency> purchaseCurrencys = baseClient.listAllPurchaseCurrency();

        for(ShouldPayOpenBill eord:list) {
            MaterialItem materialItem = getMaterialItem(materialItems, eord);
            if (materialItem == null) {
                eord.getErrorCell("materialName").setComment("不存在编号为：'" + eord.getMaterialCode() + "'," +
                        "名称为：'" + eord.getMaterialName() + "'" +
                        "的物料");
            } else {
                eord.setMaterialId(materialItem.getMaterialId());
            }

            CompanyInfo companyInfo1 = new CompanyInfo();
            companyInfo1.setCompanyName(eord.getVendorName());
            companyInfo1.setCompanyCode(eord.getVendorCode());
            companyInfo1.setStatus("APPROVED");
            CompanyInfo companyInfo = supplierClient.getCompanyInfoByParam(companyInfo1);
            if(companyInfo==null){
                eord.getErrorCell("vendorName").setComment("不存在名称为：'"+eord.getVendorName()+"'的已批准供应商");
            }else{
                eord.setVendorId(companyInfo.getCompanyId());
                eord.setVendorCode(companyInfo.getCompanyCode());
            }

            OrganizationUser organization = getOrganization(organizations, eord.getOrganizationName());
            if (organization == null) {
                eord.getErrorCell("organizationName").setComment("不存在名称为：'" + eord.getOrganizationName() + "'的有效组织");
            } else {
                eord.setOrganizationId(organization.getOrganizationId());
            }


            //检验这个供应商有没有这个合作组织
            if (organization != null && companyInfo != null) {
                //检验这个供应商有没有这个合作组织
                OrgInfo orgInfo = supplierClient.getOrgInfoByOrgIdAndCompanyId(organization.getOrganizationId(), companyInfo.getCompanyId());
                if (!checkOrgEffective(orgInfo)) {
                    eord.getErrorCell("organizationName").setComment("不存在名称为：'" + eord.getOrganizationName() + "'的有效组织");
                }
            }


            String billType = getDictItemCode(billTypeDictItems, eord.getBillType());
            String billTypeValue = eord.getBillType();
            if (StringUtils.isBlank(billType)) {
                eord.getErrorCell("billType").setComment("不存在名称为：'" + eord.getBillType() + "'的单据类型");
            } else {
                eord.setBillType(billType);
            }

            String rfq = getCurrencyCode(purchaseCurrencys, eord.getRfqSettlementCurrency());
            if (StringUtils.isBlank(rfq)) {
                eord.getErrorCell("rfqSettlementCurrency").setComment("不存在名称为：'" + eord.getRfqSettlementCurrency() + "'的币种");
            } else {
                eord.setRfqSettlementCurrency(rfq);
            }

            eord.setShouldPayOpenBillId(IdGenrator.generate());
            eord.setVendorId(companyInfo.getCompanyId());
            eord.setVendorCode(companyInfo.getCompanyCode());

            List<ShouldPayOpenBill> spobs = this.checkExist(eord);
            if(!CollectionUtils.isEmpty(spobs)){
                eord.getErrorCell("lineErrorContents").setComment("已存在供应商：'" + eord.getVendorName() + "',单据类型:'"
                        + billTypeValue + "',单据编号:'" + eord.getBillCode() + "',业务日期:'" + DateFormatUtils.format(eord.getBusinessDate(), "yyyy-MM-dd") + "'的数据");
            }
            eord.setShouldPayOpenBillId(IdGenrator.generate());
        }
        if(ExcelUtil.getErrorCells(list).size()>0){
            return;
        }
        for(ShouldPayOpenBill eord:list){
            try{
                this.save(eord);
            }catch(Exception e){
                e.printStackTrace();
                eord.getErrorCell("lineErrorContents").setLineErrorContents("该行上传失败，请重新处理后再导入");
            }
        }
    }

    /**
     * 检验合作组织的有效性
     * @return
     */
    private static boolean checkOrgEffective(OrgInfo orgInfo)  {
        if(orgInfo == null||orgInfo.getStartDate()==null||!StringUtils.equals(orgInfo.getServiceStatus(), OrgStatus.EFFECTIVE.name())){
            return false;
        }
        LocalDate now = LocalDate.now();
        if(orgInfo.getStartDate().isAfter(now)){
            return false;
        }
        if(orgInfo.getEndDate()!=null&&(now.isAfter(orgInfo.getEndDate())||now.isEqual(orgInfo.getEndDate()))){
            return false;
        }
        return true;
    }

    /**
     * 检查数据是否重复
     * @param shouldPayOpenBill
     * @return
     */
    private List<ShouldPayOpenBill> checkExist(ShouldPayOpenBill shouldPayOpenBill) {
        ShouldPayOpenBill query = new ShouldPayOpenBill();
        query.setVendorCode(shouldPayOpenBill.getVendorCode());
        query.setBillType(shouldPayOpenBill.getBillType());
        query.setBillCode(shouldPayOpenBill.getBillCode());
        QueryWrapper<ShouldPayOpenBill> wrapper = new QueryWrapper<ShouldPayOpenBill>(query);
        String businessDateStr = DateUtil.parseDateToStr(shouldPayOpenBill.getBusinessDate(),DateUtil.YYYY_MM_DD);
        wrapper.ge("BUSINESS_DATE",businessDateStr);
        wrapper.le("BUSINESS_DATE",businessDateStr);
        return this.list(wrapper);
    }

    @Override
    public ShouldPayOpenBill getSum(ShouldPayOpenBillRequestDTO requestDTO) {
        PageUtil.startPage(null, 0);
        BigDecimal lastEndingBalance = shouldPayOpenBillMapper.getLastEndingBalance(requestDTO);
        ShouldPayOpenBill shouldPayOpenBill = shouldPayOpenBillMapper.sum(requestDTO);
        if(shouldPayOpenBill!=null){
            shouldPayOpenBill.setEndingBalance(lastEndingBalance);
        }
        return shouldPayOpenBill;
    }

    @Override
    public List<ShouldPayOpenBill> listPage(ShouldPayOpenBillRequestDTO requestDTO) {
        PageUtil.startPage(requestDTO.getPageNum(), requestDTO.getPageSize());

        QueryWrapper<ShouldPayOpenBill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(requestDTO.getVendorId()!=null,"VENDOR_ID",requestDTO.getVendorId());
        queryWrapper.eq(requestDTO.getOrganizationId()!=null,"ORGANIZATION_ID",requestDTO.getOrganizationId());
        queryWrapper.like(StringUtils.isNotBlank(requestDTO.getBillCode()),"BILL_CODE",requestDTO.getBillCode());
        queryWrapper.eq(StringUtils.isNotBlank(requestDTO.getBillType()),"BILL_TYPE",requestDTO.getBillType());
        queryWrapper.ge(!StringUtils.isEmpty(requestDTO.getStartBusinessDate()),"BUSINESS_DATE",requestDTO.getStartBusinessDate())
                .le(!StringUtils.isEmpty(requestDTO.getEndBusinessDate()),"BUSINESS_DATE",requestDTO.getEndBusinessDate());

        queryWrapper.orderByDesc("BUSINESS_DATE","CREATION_DATE","LINE_NUM");
        List<ShouldPayOpenBill> list = this.list(queryWrapper);
        return list;
    }

    /**
     * 通过组织名称获取组织ID
     * @param organizations
     * @param organizationName
     * @return
     */
    private OrganizationUser getOrganization(List<OrganizationUser> organizations, String organizationName) {
        for(OrganizationUser organizationUser:organizations){
            if(StringUtils.equals(organizationName,organizationUser.getOrganizationName())){
                return organizationUser;
            }
        }
        return null;
    }

    /**
     * 通过字典类型名称查询字典类型编号
     * @param dictItems
     * @param dictItemName
     * @return
     */
    private String getDictItemCode(List<DictItemDTO> dictItems, String dictItemName) {
        for(DictItemDTO dictItemDTO:dictItems){
            if(StringUtils.equals(dictItemName,dictItemDTO.getDictItemName())){
                return dictItemDTO.getDictItemCode();
            }
        }
        return null;
    }

    /**
     * 通过物料属性获取物料Id
     * @param materialItems
     * @param shouldPayOpenBill
     * @return
     */
    private MaterialItem getMaterialItem(List<MaterialItem> materialItems, ShouldPayOpenBill shouldPayOpenBill){
        for(MaterialItem materialItem:materialItems){
            if(StringUtils.equals(shouldPayOpenBill.getMaterialCode(),materialItem.getMaterialCode())
                    &&StringUtils.equals(shouldPayOpenBill.getMaterialName(),materialItem.getMaterialName())){
                return materialItem;
            }
        }
        return null;
    }

    /**
     * 通过币种名称查询币种的编码
     * @param purchaseCurrencys
     * @param currencyName
     * @return
     */
    private String getCurrencyCode(List<PurchaseCurrency> purchaseCurrencys, String currencyName) {
        for(PurchaseCurrency item:purchaseCurrencys){
            if(StringUtils.equals(currencyName,item.getCurrencyName())){
                return item.getCurrencyCode();
            }
        }
        return null;
    }
}
