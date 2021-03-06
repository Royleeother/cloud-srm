package com.midea.cloud.srm.supcooperate.order.service.impl;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.pm.po.CeeaWarehousingReturnDetailEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.component.handler.AutoMetaObjContext;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.feign.supcooperate.SupcooperateClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseTax;
import com.midea.cloud.srm.model.base.soap.erp.dto.SoapResponse;
import com.midea.cloud.srm.model.base.soap.erp.suppliercooperate.dto.WarehousingReturnDetailEntity;
import com.midea.cloud.srm.model.common.ExportExcelParam;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.PurchaseRequirementDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementHead;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementLine;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.suppliercooperate.deliverynote.entry.DeliveryNoteWms;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.WarehousingReturnDetailFileDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.WarehousingReturnDetailRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.Order;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderDetail;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.WarehousingReturnDetail;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.WarehousingReturnDetailErp;
import com.midea.cloud.srm.supcooperate.order.mapper.WarehousingReturnDetailMapper;
import com.midea.cloud.srm.supcooperate.order.service.IOrderDetailService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderService;
import com.midea.cloud.srm.supcooperate.order.service.IWarehousingReturnDetailErpService;
import com.midea.cloud.srm.supcooperate.order.service.IWarehousingReturnDetailService;
import com.midea.cloud.srm.supcooperate.reconciliation.service.IWarehouseReceiptService;
import net.sf.cglib.core.Local;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <pre>
 *  ????????????
 * </pre>
 *
 * @author chenwj92@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????: chenwj92
 *  ????????????: 2020/8/20 14:16
 *  ????????????:
 * </pre>
 */
@Service
public class WarehousingReturnDetailServiceImpl extends ServiceImpl<WarehousingReturnDetailMapper, WarehousingReturnDetail> implements IWarehousingReturnDetailService {
    @Autowired
    WarehousingReturnDetailMapper warehousingReturnDetailMapper;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderDetailService orderDetailService;

    @Autowired
    private IWarehousingReturnDetailErpService warehousingReturnDetailErpService;

    @Autowired
    private PmClient pmClient;

    @Autowired
    private BaseClient baseClient;

    @Override
    public List<WarehousingReturnDetail> listPage(WarehousingReturnDetailRequestDTO warehousingReturnDetailRequestDTO) {
        return warehousingReturnDetailMapper.findListCopy(warehousingReturnDetailRequestDTO);
    }

    @Override
    public List<WarehousingReturnDetail> list(WarehousingReturnDetailRequestDTO warehousingReturnDetailRequestDTO) {
        return warehousingReturnDetailMapper.findList(warehousingReturnDetailRequestDTO);
    }

    /**
     * ?????????????????????????????????????????????
     * 2020-11-02????????????
     * ?????????????????????????????????erp_order_number??????????????????????????????order_number???????????????
     *
     * @param warehousingReturnDetailEntityList
     * @param instId
     * @param requestTime
     * @return
     */
    @Override
    @Transactional
    public SoapResponse acceptErpData(List<WarehousingReturnDetailEntity> warehousingReturnDetailEntityList, String instId, String requestTime) {
        List<WarehousingReturnDetail> warehousingReturnDetailAddOrUpdates = new ArrayList<>();
        List<WarehousingReturnDetailErp> warehousingReturnDetailErpAdds = new ArrayList<>();
        String returnStatus = "";
        String returnMsg = "";
        try{
            for(WarehousingReturnDetailEntity item:warehousingReturnDetailEntityList){
                checkWarehousingReturnDetailEntity(item);
                /*???????????????txn_id???????????????????????????*/
                QueryWrapper<WarehousingReturnDetailErp> repeatWrapper = new QueryWrapper<>();
                repeatWrapper.eq("TXN_ID",item.getTxnId());
                List<WarehousingReturnDetailErp> warehousingReturnDetailErpList = warehousingReturnDetailErpService.list(repeatWrapper);
                if(!CollectionUtils.isEmpty(warehousingReturnDetailErpList)){
                    insertRepeat(item);
                    continue;
                }
                /*???????????????????????????????????????????????????????????????,???????????????*/
                if(item.getTxnType().equals("RECEIVE") ||
                        item.getTxnType().equals("RECEIVE_STANDARD") ||
                        item.getTxnType().equals("RETURN TO VENDOR") ||
                        item.getTxnType().equals("RETRUN_TO_VENDOR")
                ){
                    QueryWrapper<Order> orderWrapper = new QueryWrapper();
                    if("CONSIGNMENT".equals(item.getConsignType())){
                        /*??????????????????*/
                        orderWrapper.eq("ERP_ORDER_NUMBER",item.getPoNumber());
                    }else{
                        /*?????????????????????*/
                        orderWrapper.eq("ORDER_NUMBER",item.getPoNumber());
                    }
                    List<Order> orderList = orderService.list(orderWrapper);
                    if(CollectionUtils.isEmpty(orderList)){
                        /*????????????????????????????????????*/
                        insertError(item);
                        continue;
                        /*throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????[poNumber]??????" + item.getPoNumber()));*/
                    }
                    QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
                    orderDetailQueryWrapper.eq("ORDER_ID",orderList.get(0).getOrderId());
                    orderDetailQueryWrapper.eq("LINE_NUM",item.getPoLineNum());
                    List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailQueryWrapper);
                    if(CollectionUtils.isEmpty(orderDetailList)){
                        insertError(item);
                        continue;
                        /*throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????[poNumber]??????" + item.getPoNumber() + ",????????????[poLineNum]??????" + item.getPoLineNum() + "?????????"));*/
                    }
                    OrderDetail orderDetail = orderDetailList.get(0);
                    Order order = orderList.get(0);
                    String requirementHeadNum = null;
                    Integer rowNum = null;
                    String projectNum = null;
                    String projectName = null;
                    if("Y".equals(orderDetail.getCeeaIfRequirement())){
                        RequirementLine requirementLine = pmClient.getRequirementLineByIdForAnon(orderDetail.getCeeaRequirementLineId());
                        if(Objects.nonNull(requirementLine)){
                            PurchaseRequirementDTO purchaseRequirementDTO = pmClient.getPurchaseRequirementDTOByHeadIdForAnon(requirementLine.getRequirementHeadId());
                            RequirementHead requirementHead = purchaseRequirementDTO.getRequirementHead();
                            if(Objects.nonNull(requirementHead)){
                                requirementHeadNum = requirementLine.getRequirementHeadNum();
                                rowNum = requirementLine.getRowNum();
                                projectNum = requirementHead.getCeeaProjectNum();
                                projectName = requirementHead.getCeeaProjectName();

                            }
                        }
                    }

                    Date date = new Date();
                    //srm????????????
                    String srmOrderNumber = order.getOrderNumber();
                    //??????????????????????????????
                    String settlementOrderNumber = StringUtils.isNotBlank(order.getEprOrderNumber()) ? order.getEprOrderNumber() : order.getOrderNumber();

                    WarehousingReturnDetail warehousingReturnDetail = new WarehousingReturnDetail();
                    warehousingReturnDetail.setReceiveOrderNo(item.getReceiveNum())  //????????????
                            .setReceiveOrderLineNo(Integer.parseInt(item.getReceiveLineNum()))  //????????????
                            .setTxnId(Long.parseLong(item.getTxnId()))
                            .setErpOrgId(Long.parseLong(item.getOperationUnitId()))  //erp????????????id
                            .setErpOrganizationId(Long.parseLong(item.getOrganizationId()))  //erp????????????id
                            .setErpOrganizationCode(item.getOrganizationCode())  //erp??????????????????
                            .setErpVendorId(Long.parseLong(item.getVendorId()))  //erp?????????id
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
                            .setSrmOrderNumber(srmOrderNumber) //srm????????????
                            .setSettlementOrderNumber(settlementOrderNumber)  //??????????????????????????????
                            .setLineNum(Integer.parseInt(item.getPoLineNum()))  //????????????
                            .setAsnNumber(item.getAsnNumber())
                            .setAsnLineNum(item.getAsnLineNum())
                            .setCreatedBy("erp??????")
                            .setCreationDate(date)
                            .setUnitPriceExcludingTax(new BigDecimal(item.getPrice())) //???????????????
                            .setPoLineId(Long.parseLong(item.getPoLineId()))  //???????????????id
                            .setShipLineId(Long.parseLong(item.getShipLineId()))    //?????????id
                            .setShipLineNum(Long.parseLong(item.getShipLineNum()))   //???????????????
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
                            .setCreatedBy(order.getCeeaEmpUsername())   //???????????????????????????
                            .setCreatedId(order.getCeeaEmpUseId())
                            .setCreationDate(new Date())
                            /*requirement??????*/
                            .setRequirementHeadNum(requirementHeadNum)  //??????????????????
                            .setRowNum(rowNum)   //????????????
                            .setProjectNum(projectNum)  //????????????
                            .setProjectName(projectName);  //????????????
                    /*todo ??????????????????*/
                    /*todo ???????????? ???????????? */
                    /*todo ??????(??????)*/
                    /*??????warehousingReturnDetailErp(????????????????????????????????? BeanUtils.copyProperties()????????????)*/
                    WarehousingReturnDetailErp warehousingReturnDetailErp = new WarehousingReturnDetailErp();
                    BeanUtils.copyProperties(item,warehousingReturnDetailErp);
                    warehousingReturnDetailErp.setWarehousingReturnDetailErpId(IdGenrator.generate());
                    Date txnDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(item.getTxnDate());
                    Date exchangeDate = null;
                    if(StringUtils.isNotBlank(item.getExchangeDate())){
                        exchangeDate = new SimpleDateFormat("yyyy-MM-dd").parse(item.getExchangeDate());
                    }

                    warehousingReturnDetailErp.setTxnId(Long.parseLong(item.getTxnId()))
                            .setTxnDate(txnDate)
                            .setQuantity(StringUtils.isBlank(item.getQuantity()) ? null : new BigDecimal(item.getQuantity()))
                            .setParentTxnId(StringUtils.isBlank(item.getParentTxnId()) ? null : Long.parseLong(item.getParentTxnId()))
                            .setPoHeaderId(StringUtils.isBlank(item.getPoHeaderId()) ? null : Long.parseLong(item.getPoHeaderId()))
                            .setPoLineId(StringUtils.isBlank(item.getPoLineId()) ? null : Long.parseLong(item.getPoLineId()))
                            .setShipLineId(StringUtils.isBlank(item.getShipLineId()) ? null : Long.parseLong(item.getShipLineId()))
                            .setReleaseLineId(StringUtils.isBlank(item.getReleaseLineId()) ? null : Long.parseLong(item.getReleaseLineId()))
                            .setDistId(StringUtils.isBlank(item.getDistId()) ? null : Long.parseLong(item.getDistId()))
                            .setPoLineNum(StringUtils.isBlank(item.getPoLineNum()) ? null : Long.parseLong(item.getPoLineNum()))
                            .setShipLineNum(StringUtils.isBlank(item.getShipLineNum()) ? null : Long.parseLong(item.getShipLineNum()))
                            .setPrice(StringUtils.isBlank(item.getPrice()) ? null : new BigDecimal(item.getPrice()))
                            .setExchangeDate(exchangeDate)
                            .setExchangeRate(StringUtils.isBlank(item.getExchangeRate()) ? null : new BigDecimal(item.getExchangeRate()))
                            .setVendorId(StringUtils.isBlank(item.getVendorId()) ? null : Long.parseLong(item.getVendorId()))
                            .setVendorSiteId(StringUtils.isBlank(item.getVendorSiteId()) ? null : Long.parseLong(item.getVendorSiteId()))
                            .setOrganizationId(StringUtils.isBlank(item.getOrganizationId()) ? null : Long.parseLong(item.getOrganizationId()))
                            .setAmount(StringUtils.isBlank(item.getAmount()) ? null : new BigDecimal(item.getAmount()))
                            .setOperationUnitId(StringUtils.isBlank(item.getOperationUnitId()) ? null : Long.parseLong(item.getOperationUnitId()));

                    /*20201013???????????????????????????????????????????????????*/
                    if(item.getTxnType().equals("RECEIVE") || item.getTxnType().equals("RECEIVE_STANDARD")){
                        /*??????*/
                        warehousingReturnDetail.setWarehousingReturnDetailId(IdGenrator.generate())
                                .setType(CeeaWarehousingReturnDetailEnum.RECEIVE.getValue())
                                .setReceiveNum(new BigDecimal(item.getQuantity()))
                                .setDealNum(1)
                                .setParentTxnId(Long.parseLong(item.getTxnId()))
                                .setEnable("Y")
                                .setReceiveDate(txnDate);
                        warehousingReturnDetailErp.setInsertSequence(1)
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
                                .setReceivedQuantity(orderDetail.getReceivedQuantity().add(new BigDecimal(item.getQuantity())))
                        );
                    }else if(item.getTxnType().equals("RETURN TO VENDOR") || item.getTxnType().equals("RETRUN_TO_VENDOR")){
                        /*??????????????????*/
                        warehousingReturnDetail.setWarehousingReturnDetailId(IdGenrator.generate())
                                .setType(CeeaWarehousingReturnDetailEnum.RETURN.getValue())
                                .setReturnToSupplierNum(new BigDecimal(item.getQuantity()))
                                .setReceiveNum(BigDecimal.ZERO.subtract(new BigDecimal(item.getQuantity())))
                                .setDealNum(1)
                                .setReturnToSupplierDate(txnDate);
                        warehousingReturnDetailErp.setInsertSequence(1)
                                .setWarehousingReturnDetailId(warehousingReturnDetail.getWarehousingReturnDetailId());
                        /*?????????????????????*/
                        warehousingReturnDetail.setNotInvoiceQuantity(warehousingReturnDetail.getReceiveNum());//???????????????????????????????????????(??????????????????????????????)

                        /*?????????????????? ??????????????? parentTxnId*/
                        WarehousingReturnDetailErp erpEntity = new WarehousingReturnDetailErp();
                        BeanUtils.copyProperties(warehousingReturnDetailErp,erpEntity);

                        while (erpEntity != null && !"RECEIVE".equals(erpEntity.getTxnType()) && !"RECEIVE_STANDARD".equals(erpEntity.getTxnType())){
                            if(Objects.isNull(erpEntity.getParentTxnId())){
                                erpEntity = null;
                            }else{
                                WarehousingReturnDetailErp param = new WarehousingReturnDetailErp()
                                        .setTxnId(erpEntity.getParentTxnId());
                                List<WarehousingReturnDetailErp> list = warehousingReturnDetailErpService.list(new QueryWrapper<>(param));
                                if(CollectionUtils.isNotEmpty(list)){
                                    erpEntity = list.get(0);
                                }else{
                                    erpEntity = null;
                                }

                            }
                        }
                        if(erpEntity == null){
                            /*???????????????parentTxnId */
                            warehousingReturnDetailErp.setIfHandle("N");
                            warehousingReturnDetail.setEnable("N");
                        }else{
                            warehousingReturnDetailErp.setIfHandle("Y");
                            warehousingReturnDetail.setParentTxnId(erpEntity.getTxnId())
                                    .setEnable("Y");
                        }
                        /*????????????????????????*/
                        if(Objects.isNull(orderDetail.getReceivedQuantity())){
                            orderDetail.setReceivedQuantity(BigDecimal.ZERO);
                        }
                        orderDetailService.updateById(new OrderDetail()
                                .setOrderDetailId(orderDetail.getOrderDetailId())
                                .setReceivedQuantity(orderDetail.getReceivedQuantity().add(warehousingReturnDetail.getReceiveNum()))
                        );

                    }else{
                        Assert.notNull(null,LocaleHandler.getLocaleMsg("???????????????????????????TxnType = " + item.getTxnType()));
                    }
                    warehousingReturnDetailAddOrUpdates.add(warehousingReturnDetail);
                    warehousingReturnDetailErpAdds.add(warehousingReturnDetailErp);
                }else{
                    insertRepeat(item);
                    continue;
                }

            }
            warehousingReturnDetailErpService.saveBatch(warehousingReturnDetailErpAdds);
            LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
            if(Objects.isNull(loginAppUser)){
                AutoMetaObjContext.noOp(AutoMetaObjContext.MODE.FOREVERY);
            }
            this.saveOrUpdateBatch(warehousingReturnDetailAddOrUpdates);

            returnStatus = "S";
            returnMsg = "????????????";
        }catch (Exception e){
            returnStatus = "E";
            returnMsg = e.getMessage();
            log.error("????????????",e);
            /*????????????????????????*/
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            //????????????AutoMetaObjContext ?????????????????????????????????????????????
            AutoMetaObjContext.manullyRemove();
            return returnResponse(returnStatus, returnMsg,requestTime,instId);
        }
    }

    @Override
    public void export(WarehousingReturnDetailRequestDTO warehousingReturnDetailRequestDTO, HttpServletResponse response) {
        //????????????
        /*Workbook workbook = createWorkbookModel();*/
        //????????????
    }

    /*private Workbook createWorkbookModel(){
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

    }*/

    private void insertRepeat(WarehousingReturnDetailEntity item) throws Exception{
        WarehousingReturnDetailErp warehousingReturnDetailErp = new WarehousingReturnDetailErp();
        BeanUtils.copyProperties(item,warehousingReturnDetailErp);
        warehousingReturnDetailErp.setWarehousingReturnDetailErpId(IdGenrator.generate());
        Date txnDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(item.getTxnDate());
        Date exchangeDate = null;
        if(StringUtils.isNotBlank(item.getExchangeDate())){
            exchangeDate = new SimpleDateFormat("yyyy-MM-dd").parse(item.getExchangeDate());
        }
        warehousingReturnDetailErp.setTxnId(Long.parseLong(item.getTxnId()))
                .setTxnDate(txnDate)
                .setQuantity(StringUtils.isBlank(item.getQuantity()) ? null : new BigDecimal(item.getQuantity()))
                .setParentTxnId(StringUtils.isBlank(item.getParentTxnId()) ? null : Long.parseLong(item.getParentTxnId()))
                .setPoHeaderId(StringUtils.isBlank(item.getPoHeaderId()) ? null : Long.parseLong(item.getPoHeaderId()))
                .setPoLineId(StringUtils.isBlank(item.getPoLineId()) ? null : Long.parseLong(item.getPoLineId()))
                .setShipLineId(StringUtils.isBlank(item.getShipLineId()) ? null : Long.parseLong(item.getShipLineId()))
                .setReleaseLineId(StringUtils.isBlank(item.getReleaseLineId()) ? null : Long.parseLong(item.getReleaseLineId()))
                .setDistId(StringUtils.isBlank(item.getDistId()) ? null : Long.parseLong(item.getDistId()))
                .setPoLineNum(StringUtils.isBlank(item.getPoLineNum()) ? null : Long.parseLong(item.getPoLineNum()))
                .setShipLineNum(StringUtils.isBlank(item.getShipLineNum()) ? null : Long.parseLong(item.getShipLineNum()))
                .setPrice(StringUtils.isBlank(item.getPrice()) ? null : new BigDecimal(item.getPrice()))
                .setExchangeDate(exchangeDate)
                .setExchangeRate(StringUtils.isBlank(item.getExchangeRate()) ? null : new BigDecimal(item.getExchangeRate()))
                .setVendorId(StringUtils.isBlank(item.getVendorId()) ? null : Long.parseLong(item.getVendorId()))
                .setVendorSiteId(StringUtils.isBlank(item.getVendorSiteId()) ? null : Long.parseLong(item.getVendorSiteId()))
                .setOrganizationId(StringUtils.isBlank(item.getOrganizationId()) ? null : Long.parseLong(item.getOrganizationId()))
                .setAmount(StringUtils.isBlank(item.getAmount()) ? null : new BigDecimal(item.getAmount()))
                .setOperationUnitId(StringUtils.isBlank(item.getOperationUnitId()) ? null : Long.parseLong(item.getOperationUnitId()))
                .setIfHandle("Y")
                .setInsertSequence(-1);
        warehousingReturnDetailErpService.save(warehousingReturnDetailErp);

    }

    /**
     * 2020-11-10????????????????????????????????????????????????
     * @param item
     * @throws ParseException
     */
    public void insertError(WarehousingReturnDetailEntity item) throws ParseException {
        WarehousingReturnDetailErp warehousingReturnDetailErp = new WarehousingReturnDetailErp();
        BeanUtils.copyProperties(item,warehousingReturnDetailErp);
        warehousingReturnDetailErp.setWarehousingReturnDetailErpId(IdGenrator.generate());
        Date txnDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(item.getTxnDate());
        Date exchangeDate = null;
        if(StringUtils.isNotBlank(item.getExchangeDate())){
            exchangeDate = new SimpleDateFormat("yyyy-MM-dd").parse(item.getExchangeDate());
        }
        warehousingReturnDetailErp.setTxnId(Long.parseLong(item.getTxnId()))
                .setTxnDate(txnDate)
                .setQuantity(StringUtils.isBlank(item.getQuantity()) ? null : new BigDecimal(item.getQuantity()))
                .setParentTxnId(StringUtils.isBlank(item.getParentTxnId()) ? null : Long.parseLong(item.getParentTxnId()))
                .setPoHeaderId(StringUtils.isBlank(item.getPoHeaderId()) ? null : Long.parseLong(item.getPoHeaderId()))
                .setPoLineId(StringUtils.isBlank(item.getPoLineId()) ? null : Long.parseLong(item.getPoLineId()))
                .setShipLineId(StringUtils.isBlank(item.getShipLineId()) ? null : Long.parseLong(item.getShipLineId()))
                .setReleaseLineId(StringUtils.isBlank(item.getReleaseLineId()) ? null : Long.parseLong(item.getReleaseLineId()))
                .setDistId(StringUtils.isBlank(item.getDistId()) ? null : Long.parseLong(item.getDistId()))
                .setPoLineNum(StringUtils.isBlank(item.getPoLineNum()) ? null : Long.parseLong(item.getPoLineNum()))
                .setShipLineNum(StringUtils.isBlank(item.getShipLineNum()) ? null : Long.parseLong(item.getShipLineNum()))
                .setPrice(StringUtils.isBlank(item.getPrice()) ? null : new BigDecimal(item.getPrice()))
                .setExchangeDate(exchangeDate)
                .setExchangeRate(StringUtils.isBlank(item.getExchangeRate()) ? null : new BigDecimal(item.getExchangeRate()))
                .setVendorId(StringUtils.isBlank(item.getVendorId()) ? null : Long.parseLong(item.getVendorId()))
                .setVendorSiteId(StringUtils.isBlank(item.getVendorSiteId()) ? null : Long.parseLong(item.getVendorSiteId()))
                .setOrganizationId(StringUtils.isBlank(item.getOrganizationId()) ? null : Long.parseLong(item.getOrganizationId()))
                .setAmount(StringUtils.isBlank(item.getAmount()) ? null : new BigDecimal(item.getAmount()))
                .setOperationUnitId(StringUtils.isBlank(item.getOperationUnitId()) ? null : Long.parseLong(item.getOperationUnitId()))
                .setIfHandle("Y")
                .setInsertSequence(-1);
        warehousingReturnDetailErpService.save(warehousingReturnDetailErp);
    }

    @Override
    public PageInfo<WarehousingReturnDetail> warehousingReturnlistPageByParam(WarehousingReturnDetailRequestDTO warehousingReturnDetailRequestDTO) {

        PageUtil.startPage(warehousingReturnDetailRequestDTO.getPageNum(), warehousingReturnDetailRequestDTO.getPageSize());
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (UserType.VENDOR.name().equals(loginAppUser.getUserType())) {
            //???????????????????????????????????????????????????????????????
            if (Objects.isNull(loginAppUser.getCompanyId())) {
                return new PageInfo<>(new ArrayList<>());
            }
            warehousingReturnDetailRequestDTO.setVendorId(loginAppUser.getCompanyId());
        }
        /**
         * ????????????????????????????????????ERP,?????????????????????????????????
         */
        warehousingReturnDetailRequestDTO.setCeeaCostTypeCode(null);
        List<WarehousingReturnDetail> warehousingReturnDetails = this.baseMapper.findInvoiceNoticeList(warehousingReturnDetailRequestDTO);
        for (WarehousingReturnDetail warehousingReturnDetail : warehousingReturnDetails) {
            if (warehousingReturnDetail == null) continue;
            //??????????????????,???????????????,??????
            BigDecimal receiveNum = warehousingReturnDetail.getReceiveNum() == null ? BigDecimal.ZERO : warehousingReturnDetail.getReceiveNum();//????????????
            BigDecimal unitPriceExcludingTax = warehousingReturnDetail.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : warehousingReturnDetail.getUnitPriceExcludingTax();//??????(?????????)
            BigDecimal unitPriceContainingTax = warehousingReturnDetail.getUnitPriceContainingTax() == null ? BigDecimal.ZERO : warehousingReturnDetail.getUnitPriceContainingTax();//??????(??????)
            warehousingReturnDetail.setNoTaxAmount(warehousingReturnDetail.getReceiveNum().multiply(warehousingReturnDetail.getUnitPriceExcludingTax()));
            warehousingReturnDetail.setTaxAmount(warehousingReturnDetail.getReceiveNum().multiply(warehousingReturnDetail.getUnitPriceContainingTax()));
            warehousingReturnDetail.setTax((warehousingReturnDetail.getUnitPriceContainingTax().subtract(warehousingReturnDetail.getUnitPriceExcludingTax())).multiply(warehousingReturnDetail.getReceiveNum()));
            //??????????????????????????????????????????????????????
            warehousingReturnDetail.setOrderNumber(warehousingReturnDetail.getSettlementOrderNumber());
        }
        return new PageInfo<>(warehousingReturnDetails);
    }
    @Override
    public void exportStart(WarehousingReturnDetailRequestDTO warehousingReturnDetail,HttpServletResponse response) throws IOException {
        // ??????
        WarehousingReturnDetailFileDTO warehousingReturnDetailFileDTO = new WarehousingReturnDetailFileDTO();
        Field[] declaredFields = warehousingReturnDetailFileDTO.getClass().getDeclaredFields();
        ArrayList<String> head = new ArrayList<>();
        ArrayList<String> headName = new ArrayList<>();
        for (Field field : declaredFields) {
            head.add(field.getName());
            ExcelProperty annotation = field.getAnnotation(ExcelProperty.class);
            if (null != annotation) {
                headName.add(annotation.value()[0]);
            }
        }
        /*?????????????????????*/
        List<List<Object>> dataList = this.queryExportData(head,warehousingReturnDetail);
        /*?????????*/
        String fileName = "??????????????????";
        /*????????????*/
        EasyExcelUtil.exportStart(response,dataList,headName,fileName);
    }


    // ?????????????????????
    public List<List<Object>> queryExportData(List<String> param,WarehousingReturnDetailRequestDTO warehousingReturnDetail) {
        //???????????????????????????
        /*QueryWrapper<WarehousingReturnDetail> wrapper = new QueryWrapper<>();
        wrapper.select("if(TYPE='RECEIVE','????????????','????????????')TYPE,RECEIVE_ORDER_NO,RECEIVE_ORDER_LINE_NO,ORG_NAME\n" +
                ",if(TYPE='RECEIVE',IFNULL(RECEIVE_DATE,WAREHOUSING_DATE),IFNULL(RETURN_TO_RECEIVING_DATE,RETURN_TO_SUPPLIER_DATE))RECEIVE_DATE\n" +
                ",ORGANIZATION_NAME,VENDOR_CODE,VENDOR_NAME,CATEGORY_NAME,ITEM_CODE,ITEM_NAME,UNIT,RECEIVE_NUM,REQUIREMENT_HEAD_NUM\n" +
                ",ROW_NUM,ORDER_NUMBER,LINE_NUM,CONTRACT_NO,CREATED_BY,CREATION_DATE");
        List<WarehousingReturnDetail> WarehousingReturnDetailList = this.list(wrapper);*/

        List<WarehousingReturnDetail> WarehousingReturnDetailList = warehousingReturnDetailMapper.findListCopy(warehousingReturnDetail);

        List<List<Object>> dataList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(WarehousingReturnDetailList)) {
            List<Map<String, Object>> mapList = BeanMapUtils.objectsToMaps(WarehousingReturnDetailList);
            List<String> titleList = param;
            if (CollectionUtils.isNotEmpty(titleList)) {
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

    public void checkWarehousingReturnDetailEntity(WarehousingReturnDetailEntity warehousingReturnDetailEntity){
        Assert.hasText(warehousingReturnDetailEntity.getTxnType(), LocaleHandler.getLocaleMsg("????????????????????????????????????"));
        Assert.hasText(warehousingReturnDetailEntity.getTxnDate(), LocaleHandler.getLocaleMsg("??????????????????????????????"));
        Assert.notNull(warehousingReturnDetailEntity.getQuantity(),LocaleHandler.getLocaleMsg("??????????????????"));
        Assert.hasText(warehousingReturnDetailEntity.getReceiveNum(),LocaleHandler.getLocaleMsg("?????????????????????"));
        Assert.hasText(warehousingReturnDetailEntity.getReceiveLineNum(),LocaleHandler.getLocaleMsg("????????????????????????"));
        Assert.hasText(warehousingReturnDetailEntity.getPoNumber(),LocaleHandler.getLocaleMsg("??????????????????????????????"));
        Assert.hasText(warehousingReturnDetailEntity.getPoLineNum(), LocaleHandler.getLocaleMsg("??????????????????????????????"));
    }

    public SoapResponse returnResponse(String returnStatus, String returnMsg, String requestTime, String instId){
        SoapResponse returnResponse = new SoapResponse();
        Date nowDate = new Date();
        returnResponse.setSuccess("true");
        SoapResponse.RESPONSE responseValue = new SoapResponse.RESPONSE();
        SoapResponse.RESPONSE.EsbInfo esbInfoValue = new SoapResponse.RESPONSE.EsbInfo();
        esbInfoValue.setReturnStatus(returnStatus);
        esbInfoValue.setReturnMsg(returnMsg);
        esbInfoValue.setResponseTime(requestTime);
        if(StringUtils.isBlank(requestTime)){
            requestTime = DateUtil.parseDateToStr(nowDate, "yyyy-MM-dd HH:mm:ss:SSS");
        }
        esbInfoValue.setInstId(instId);
        esbInfoValue.setRequestTime(requestTime);
        esbInfoValue.setAttr1("");
        esbInfoValue.setAttr2("");
        esbInfoValue.setAttr3("");
        responseValue.setEsbInfo(esbInfoValue);
        returnResponse.setResponse(responseValue);
        return returnResponse;
    }
}
