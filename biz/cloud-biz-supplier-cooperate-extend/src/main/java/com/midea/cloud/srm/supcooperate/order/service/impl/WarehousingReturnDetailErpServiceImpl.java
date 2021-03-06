package com.midea.cloud.srm.supcooperate.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.enums.pm.po.CeeaWarehousingReturnDetailEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.model.base.soap.erp.suppliercooperate.dto.WarehousingReturnDetailEntity;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.PurchaseRequirementDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementHead;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementLine;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.Order;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderDetail;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.WarehousingReturnDetail;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.WarehousingReturnDetailErp;
import com.midea.cloud.srm.supcooperate.order.mapper.WarehousingReturnDetailErpMapper;
import com.midea.cloud.srm.supcooperate.order.service.IOrderDetailService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderService;
import com.midea.cloud.srm.supcooperate.order.service.IWarehousingReturnDetailErpService;
import com.midea.cloud.srm.supcooperate.order.service.IWarehousingReturnDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
*  <pre>
 *  ??????????????????erp??? ???????????????
 * </pre>
*
* @author chenwj92@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-08-25 14:30:44
 *  ????????????:
 * </pre>
*/
@Service
@Slf4j
public class WarehousingReturnDetailErpServiceImpl extends ServiceImpl<WarehousingReturnDetailErpMapper, WarehousingReturnDetailErp> implements IWarehousingReturnDetailErpService {

    @Autowired
    private IWarehousingReturnDetailService warehousingReturnDetailService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderDetailService orderDetailService;

    @Autowired
    private PmClient pmClient;

    @Autowired
    private WarehousingReturnDetailErpMapper warehousingReturnDetailErpMapper;

    /**
     * ????????????????????????????????????RECEIVE??????RECEIVE_STANDARD??????RETURN TO VENDOR??????RETURN_TO_VENDOR???,IF_HANDLE??????N????????????
     * @return
     */
    @Override
    @Transactional
    public BaseResult transferErpToSrm() {
        long startTime = System.currentTimeMillis();
        int count = warehousingReturnDetailErpMapper.countWarehousingReturnDetailErp();
        List<WarehousingReturnDetailErp> warehousingReturnDetailErpList = null;
        if(count < 1000){
            warehousingReturnDetailErpList = warehousingReturnDetailErpMapper.listWarehousingReturnDetailErp(0);
            handle(warehousingReturnDetailErpList);
        }else{
            int time = (int)Math.ceil(count/1000.0);
            for(int i=0;i<time;i++){
                warehousingReturnDetailErpList = warehousingReturnDetailErpMapper.listWarehousingReturnDetailErp(i);
                handle(warehousingReturnDetailErpList);
            }
        }

        Long totalTime = System.currentTimeMillis() - startTime;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("????????????????????????" + count + "???.????????????" + totalTime + "??????");

        return BaseResult.build(ResultCode.SUCCESS,stringBuilder.toString());
    }

    private void handle(List<WarehousingReturnDetailErp> warehousingReturnDetailErpList) {
        List<WarehousingReturnDetail> warehousingReturnDetailAddOrUpdates = new LinkedList<>();
        List<WarehousingReturnDetailErp> warehousingReturnDetailErpUpdates = new LinkedList<>();
        /*???????????????*/
        List<String> orderNumbers = warehousingReturnDetailErpList.stream().map(item -> item.getPoNumber()).collect(Collectors.toList());
        QueryWrapper<Order> orderWrapper = new QueryWrapper();
        orderWrapper.and(w -> w.in("ERP_ORDER_NUMBER",orderNumbers).or().in("ORDER_NUMBER",orderNumbers));
        List<Order> orderList = orderService.list(orderWrapper);
        /*???????????????*/
        List<Long> orderIds = orderList.stream().map(item -> item.getOrderId()).collect(Collectors.toList());
        QueryWrapper<OrderDetail> orderDetailWrapper = new QueryWrapper<>();
        orderDetailWrapper.in("ORDER_ID",orderIds);
        List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailWrapper);
        /*?????????????????????*/
        List<Long> requirementLineIds = orderDetailList.stream().map(item -> item.getCeeaRequirementLineId()).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(requirementLineIds)){
            log.info("???????????????????????????id");
            return;
        }
        List<RequirementLine> requirementLineList = pmClient.getRequirementLineByIdsForAnon(requirementLineIds);
        /*?????????????????????*/
        List<Long> requirementHeadIds = requirementLineList.stream().map(item -> item.getRequirementHeadId()).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(requirementHeadIds)){
            log.info("???????????????????????????id");
            return;
        }
        List<RequirementHead> requirementHeadList = pmClient.getRequirementHeadByIdsForAnon(requirementHeadIds);

        for(WarehousingReturnDetailErp item:warehousingReturnDetailErpList){
            checkData(item);
            /*???????????????txn_id???????????????????????????*/
            /*QueryWrapper<WarehousingReturnDetailErp> repeatWrapper = new QueryWrapper<>();
            repeatWrapper.eq("TXN_ID",item.getTxnId());
            List<WarehousingReturnDetailErp> erpList = this.list(repeatWrapper);
            if(!CollectionUtils.isEmpty(warehousingReturnDetailErpList)){
                log.info("txd_id??????");
                continue;
            }*/
            /*???????????????????????????????????????????????????????????????,???????????????*/
            if(item.getTxnType().equals("RECEIVE") ||
                    item.getTxnType().equals("RECEIVE_STANDARD") ||
                    item.getTxnType().equals("RETURN TO VENDOR") ||
                    item.getTxnType().equals("RETRUN_TO_VENDOR")
            ){
                Order order = null;
                OrderDetail orderDetail = null;
                RequirementHead requirementHead = null;
                RequirementLine requirementLine = null;

                for(Order orderItem:orderList){
                    if(item.getPoNumber().equals(orderItem.getOrderNumber()) || item.getPoNumber().equals(orderItem.getEprOrderNumber())){
                        order = orderItem;
                        break;
                    }
                }
                if(order == null){
                    continue;
                }

                for(OrderDetail orderDetailItem:orderDetailList){
                    if(Objects.equals(order.getOrderId(),orderDetailItem.getOrderId()) &&
                        Objects.equals(item.getPoLineNum(),orderDetailItem.getLineNum())
                    ){
                        orderDetail = orderDetailItem;
                        break;
                    }
                }

                if(orderDetail == null){
                    continue;
                }

                String requirementHeadNum = null;
                Integer rowNum = null;
                String projectNum = null;
                String projectName = null;
                if("Y".equals(orderDetail.getCeeaIfRequirement())){
                    for(RequirementLine requirementLineItem:requirementLineList){
                        if(Objects.equals(requirementLineItem.getRequirementLineId(),orderDetail.getCeeaRequirementLineId())){
                            requirementLine = requirementLineItem;
                            break;
                        }
                    }
                    if(requirementLine == null){
                        continue;
                    }

                    for(RequirementHead requirementHeadItem: requirementHeadList){
                        if(Objects.equals(requirementHeadItem.getRequirementHeadId(),requirementLine.getRequirementHeadId())){
                            requirementHead = requirementHeadItem;
                            break;
                        }
                    }
                    if(requirementHead == null){
                        continue;
                    }
                    requirementHeadNum = requirementLine.getRequirementHeadNum();
                    rowNum = requirementLine.getRowNum();
                    projectNum = requirementHead.getCeeaProjectNum();
                    projectName = requirementHead.getCeeaProjectName();
                }
                Date date = new Date();
                WarehousingReturnDetail warehousingReturnDetail = new WarehousingReturnDetail();
                warehousingReturnDetail.setReceiveOrderNo(item.getReceiveNum())  //????????????
                        .setReceiveOrderLineNo(Integer.parseInt(item.getReceiveLineNum()))  //????????????
                        .setTxnId(item.getTxnId())
                        .setErpOrgId(item.getOperationUnitId())  //erp????????????id
                        .setErpOrganizationId(item.getOrganizationId())  //erp????????????id
                        .setErpOrganizationCode(item.getOrganizationCode())  //erp??????????????????
                        .setErpVendorId(item.getVendorId())  //erp?????????id
                        .setErpVendorCode(item.getVendorNumber())   //erp???????????????
                        .setOrgId(order.getCeeaOrgId())  //????????????id
                        .setOrgName(item.getOperationUnit())       //??????????????????
                        .setOrganizationId(orderDetail.getCeeaOrganizationId()) //????????????id
                        .setOrganizationCode(orderDetail.getCeeaOrganizationCode())  //??????????????????
                        .setOrganizationName(orderDetail.getCeeaOrganizationName()) //??????????????????
                        .setVendorId(order.getVendorId())  //?????????id
                        .setVendorCode(order.getVendorCode())   //???????????????
                        .setItemCode(item.getItemNumber())      //????????????
                        .setItemName(item.getItemDescr())    //????????????
                        .setUnit(item.getUnitOfMeasure())   //????????????
                        .setUnitCode(item.getUnitOfMeasure()) //??????????????????
                        .setOrderNumber(item.getPoNumber())  //???????????????
                        .setLineNum(item.getPoLineNum().intValue())  //????????????
                        .setAsnNumber(item.getAsnNumber())
                        .setAsnLineNum(item.getAsnLineNum())
                        .setCreatedBy("erp??????")
                        .setCreationDate(date)
                        .setUnitPriceExcludingTax(item.getPrice()) //???????????????
                        .setPoLineId(item.getPoLineId())  //???????????????id
                        .setShipLineId(item.getShipLineId())    //?????????id
                        .setShipLineNum(item.getShipLineNum())   //???????????????
                        /*order??????*/
                        .setOrgCode(order.getCeeaOrgCode())   //??????????????????
                        .setVendorName(order.getVendorName())  //???????????????
                        /*orderDetail??????*/
                        .setCurrencyId(orderDetail.getCurrencyId())  //??????id
                        .setCurrencyCode(orderDetail.getCurrencyCode())   //????????????
                        .setCurrencyName(orderDetail.getCurrencyName())   //????????????
                        .setOrganizationName(orderDetail.getCeeaOrganizationName())  //??????????????????
                        .setCategoryId(orderDetail.getCategoryId())  //????????????id
                        .setCategoryName(orderDetail.getCategoryName())  //??????????????????
                        .setCategoryCode(orderDetail.getCategoryCode()) //??????????????????
                        .setItemId(orderDetail.getMaterialId())  //??????id
                        .setContractNo(orderDetail.getCeeaContractNo())  //???????????????
                        .setUnitPriceContainingTax(orderDetail.getCeeaUnitTaxPrice()) //????????????
                        .setTaxRate(orderDetail.getCeeaTaxRate())  //??????
                        .setTaxKey(orderDetail.getCeeaTaxKey())  //??????
                        /*requirement??????*/
                        .setRequirementHeadNum(requirementHeadNum)  //??????????????????
                        .setRowNum(rowNum)   //????????????
                        .setProjectNum(projectNum)  //????????????
                        .setProjectName(projectName);  //????????????
                /*todo ??????????????????*/
                /*todo ???????????? ???????????? */
                /*todo ??????(??????)*/
                /*??????warehousingReturnDetailErp(????????????????????????????????? BeanUtils.copyProperties()????????????)*/
                WarehousingReturnDetailErp warehousingReturnDetailErpUpdate = new WarehousingReturnDetailErp();
                Date txnDate = item.getTxnDate();
                Date exchangeDate = item.getExchangeDate();


                /*20201013???????????????????????????????????????????????????*/
                if(item.getTxnType().equals("RECEIVE") || item.getTxnType().equals("RECEIVE_STANDARD")){
                    /*??????*/
                    warehousingReturnDetail.setWarehousingReturnDetailId(IdGenrator.generate())
                            .setType(CeeaWarehousingReturnDetailEnum.RECEIVE.getValue())
                            .setReceiveNum(item.getQuantity())
                            .setDealNum(1)
                            .setParentTxnId(item.getTxnId())
                            .setEnable("Y")
                            .setReceiveDate(txnDate);
                    warehousingReturnDetailErpUpdate.setInsertSequence(1)
                            .setIfHandle("Y")
                            .setWarehousingReturnDetailId(warehousingReturnDetail.getWarehousingReturnDetailId());
                    /*?????????????????????*/
                    warehousingReturnDetail.setNotInvoiceQuantity(warehousingReturnDetail.getReceiveNum());//???????????????????????????????????????(??????????????????????????????)
                    /*??????????????????????????????*/
                    if(orderDetail.getReceivedQuantity() == null){
                        orderDetail.setReceivedQuantity(BigDecimal.ZERO);
                    }
                    orderDetailService.updateById(new OrderDetail()
                            .setOrderDetailId(orderDetail.getOrderDetailId())
                            .setReceivedQuantity(orderDetail.getReceivedQuantity().add(item.getQuantity()))
                    );
                }else if(item.getTxnType().equals("RETURN TO VENDOR") || item.getTxnType().equals("RETRUN_TO_VENDOR")){
                    /*??????????????????*/
                    warehousingReturnDetail.setWarehousingReturnDetailId(IdGenrator.generate())
                            .setType(CeeaWarehousingReturnDetailEnum.RETURN.getValue())
                            .setReturnToSupplierNum(item.getQuantity())
                            .setReceiveNum(BigDecimal.ZERO.subtract(item.getQuantity()))
                            .setDealNum(1)
                            .setReturnToSupplierDate(txnDate);
                    warehousingReturnDetailErpUpdate.setInsertSequence(1)
                            .setWarehousingReturnDetailId(warehousingReturnDetail.getWarehousingReturnDetailId());
                    /*?????????????????????*/
                    warehousingReturnDetail.setNotInvoiceQuantity(warehousingReturnDetail.getReceiveNum());//???????????????????????????????????????(??????????????????????????????)

                    /*?????????????????? ??????????????? parentTxnId*/
                    WarehousingReturnDetailErp erpEntity = new WarehousingReturnDetailErp();
                    BeanUtils.copyProperties(item,erpEntity);

                    while (erpEntity != null && !"RECEIVE".equals(erpEntity.getTxnType()) && !"RECEIVE_STANDARD".equals(erpEntity.getTxnType())){
                        if(Objects.isNull(erpEntity.getParentTxnId())){
                            erpEntity = null;
                        }else{
                            WarehousingReturnDetailErp param = new WarehousingReturnDetailErp()
                                    .setTxnId(erpEntity.getParentTxnId());
                            List<WarehousingReturnDetailErp> list = this.list(new QueryWrapper<>(param));
                            if(CollectionUtils.isNotEmpty(list)){
                                erpEntity = list.get(0);
                            }else{
                                erpEntity = null;
                            }

                        }
                    }
                    if(erpEntity == null){
                        /*???????????????parentTxnId */
                        warehousingReturnDetailErpUpdate.setIfHandle("N");
                        warehousingReturnDetail.setEnable("N");
                    }else{
                        warehousingReturnDetailErpUpdate.setIfHandle("Y");
                        warehousingReturnDetail.setParentTxnId(erpEntity.getTxnId())
                                .setEnable("Y");
                    }
                }else{
                    Assert.notNull(null,LocaleHandler.getLocaleMsg("???????????????????????????TxnType = " + item.getTxnType()));
                }
                warehousingReturnDetailAddOrUpdates.add(warehousingReturnDetail);
                warehousingReturnDetailErpUpdates.add(warehousingReturnDetailErpUpdate);


            }else{
                log.info("????????????????????????????????????????????????");
            }

        }

        warehousingReturnDetailService.saveBatch(warehousingReturnDetailAddOrUpdates);
        this.updateBatchById(warehousingReturnDetailErpUpdates);

    }

    public void checkData(WarehousingReturnDetailErp warehousingReturnDetailErp){
        Assert.hasText(warehousingReturnDetailErp.getTxnType(), LocaleHandler.getLocaleMsg("????????????????????????????????????"));
        Assert.notNull(warehousingReturnDetailErp.getTxnDate(), LocaleHandler.getLocaleMsg("??????????????????????????????"));
        Assert.notNull(warehousingReturnDetailErp.getQuantity(),LocaleHandler.getLocaleMsg("??????????????????"));
        Assert.hasText(warehousingReturnDetailErp.getReceiveNum(),LocaleHandler.getLocaleMsg("?????????????????????"));
        Assert.hasText(warehousingReturnDetailErp.getReceiveLineNum(),LocaleHandler.getLocaleMsg("????????????????????????"));
        Assert.hasText(warehousingReturnDetailErp.getPoNumber(),LocaleHandler.getLocaleMsg("??????????????????????????????"));
        Assert.notNull(warehousingReturnDetailErp.getPoLineNum(), LocaleHandler.getLocaleMsg("??????????????????????????????"));
    }
}
