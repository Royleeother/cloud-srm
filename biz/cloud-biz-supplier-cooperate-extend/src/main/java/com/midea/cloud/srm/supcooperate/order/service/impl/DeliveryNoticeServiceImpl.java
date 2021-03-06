package com.midea.cloud.srm.supcooperate.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.neworder.DeliveryNoticeStatus;
import com.midea.cloud.common.enums.order.NoteStatus;
import com.midea.cloud.common.enums.supcooperate.DeliveryNoteSource;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.DeliveryNoticeDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.DeliveryNoticeRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.OrderDetailDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.OrderRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.DeliveryNotice;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderDetail;
import com.midea.cloud.srm.supcooperate.order.mapper.DeliveryNoticeMapper;
import com.midea.cloud.srm.supcooperate.order.service.IDeliveryNoticeService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ?????????????????? ???????????????
 * </pre>
 *
 * @author huangbf3
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/3/19 19:13
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class DeliveryNoticeServiceImpl extends ServiceImpl<DeliveryNoticeMapper, DeliveryNotice> implements IDeliveryNoticeService {
    @Autowired
    private IOrderDetailService iOrderDetailService;

    @Autowired
    private DeliveryNoticeMapper deliveryNoticeMapper;

    @Autowired
    private BaseClient baseClient;


    @Override
    public List<DeliveryNoticeDTO> findList(DeliveryNoticeRequestDTO requestDTO) {
//        List<DeliveryNoticeDTO> list = this.getBaseMapper().findList(requestDTO);
//        list.forEach(item->{
//            BigDecimal warehouseReceiptQuantity = this.getBaseMapper().getWarehouseReceiptQuantity(item.getDeliveryNoticeId());
//            item.setReceiveSum(warehouseReceiptQuantity);
//        });
        return list(requestDTO);
    }

    public List<DeliveryNoticeDTO> list(DeliveryNoticeRequestDTO requestDTO){
        List<DeliveryNoticeDTO> deliveryNoticeList = deliveryNoticeMapper.list(requestDTO);
        return deliveryNoticeList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releasedBatch(List<DeliveryNotice> deliveryNotices) {
        //??????
        if(CollectionUtils.isEmpty(deliveryNotices)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<Long> ids = deliveryNotices.stream().map(item -> item.getDeliveryNoticeId()).collect(Collectors.toList());
        List<DeliveryNotice> deliveryNoticeList = this.listByIds(ids);

        //??????????????????
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            deliveryNotice.setDeliveryNoticeStatus(DeliveryNoticeStatus.WAITING_VENDOR_CONFIRM.getValue());
        }
        this.updateBatchById(deliveryNoticeList);
    }

    @Override
    public List<DeliveryNoticeDTO> listCreateDeliveryNoteDetail(List<Long> deliveryNoticeIds) {
        List<DeliveryNoticeDTO> list = this.getBaseMapper().listCreateDeliveryNoteDetail(deliveryNoticeIds);
        Set<Long> organizationIdSet = new HashSet();
        Set<String> receivedFactorySet = new HashSet();
        list.forEach(item->{
            Assert.isTrue(StringUtils.equals(item.getDeliveryNoticeStatus(),NoteStatus.RELEASED.name()),"????????????????????????????????????");
            organizationIdSet.add(item.getOrganizationId());
            receivedFactorySet.add(item.getReceivedFactory());
        });
        Assert.isTrue(organizationIdSet.size()==1,"????????????????????????????????????");
        Assert.isTrue(receivedFactorySet.size()==1,"?????????????????????????????????????????????");
        return list;
    }

    @Override
    public BigDecimal getWarehouseReceiptQuantityByDeliveryNoticeId(Long deliveryNoticeId) {
        return this.getBaseMapper().getWarehouseReceiptQuantity(deliveryNoticeId);
    }

    /**
     * ????????????????????????
     * @param deliveryNoticeList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAdd(List<DeliveryNotice> deliveryNoticeList) {
        //??????
        if(CollectionUtils.isEmpty(deliveryNoticeList)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        //????????????????????????
        checkIfQuantityEnough(deliveryNoticeList,TYPE.ADD.value);

        List<Long> orderDetailIds = deliveryNoticeList.stream().map(item -> item.getOrderDetailId()).collect(Collectors.toList());
        List<OrderDetail> orderDetailList = iOrderDetailService.listByIds(orderDetailIds);
        Map<Long, OrderDetail> orderDetailMap = orderDetailList.stream().collect(Collectors.toMap(item -> item.getOrderDetailId(), item -> item));
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            OrderDetail orderDetail = orderDetailMap.get(deliveryNotice.getOrderDetailId());
            if(Objects.isNull(orderDetail)){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("??????????????????????????????[orderDetailId=%s]",deliveryNotice.getOrderDetailId())));
            }
            if(Objects.isNull(orderDetail.getDeliveryNoticeQuantity())){
                orderDetail.setDeliveryNoticeQuantity(BigDecimal.ZERO);
            }
            if(Objects.isNull(orderDetail.getOrderNum())){
                orderDetail.setOrderNum(BigDecimal.ZERO);
            }
            if(orderDetail.getDeliveryNoticeQuantity().add(deliveryNotice.getNoticeSum())
                    .compareTo(orderDetail.getOrderNum()) > 0
            ) {
                throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????,?????????????????????"));
            }
            // ??????????????????????????????
            OrderDetail detail = new OrderDetail().
                    setOrderDetailId(orderDetail.getOrderDetailId()).
                    setDeliveryNoticeQuantity(orderDetail.getDeliveryNoticeQuantity().add(deliveryNotice.getNoticeSum()));
            orderDetails.add(detail);
        }
        //????????????
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            deliveryNotice.setDeliveryNoticeId(IdGenrator.generate());
            deliveryNotice.setDeliveryNoticeNum(baseClient.seqGen(SequenceCodeConstant.SEQ_SSC_DELIVERY_NOTICE_NUM));
            deliveryNotice.setDeliveryNoticeStatus(DeliveryNoticeStatus.DRAFT.getValue());
            if(Objects.isNull(deliveryNotice.getNoticeSum())){
                deliveryNotice.setNoticeSum(BigDecimal.ZERO);
            }
        }
        this.saveBatch(deliveryNoticeList);
        //????????????????????????
        if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(orderDetails)){
            iOrderDetailService.updateBatchById(orderDetails);
        }
    }

    /**
     * ????????????????????????
     * @param deliveryNoticeList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<DeliveryNotice> deliveryNoticeList) {
        //??????
        if(CollectionUtils.isEmpty(deliveryNoticeList)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        //????????????????????????
        checkIfQuantityEnough(deliveryNoticeList,TYPE.UPDATE.value);

        //??????????????????
        List<Long> orderDetailIds = deliveryNoticeList.stream().map(item -> item.getOrderDetailId()).collect(Collectors.toList());
        List<OrderDetail> orderDetailList = iOrderDetailService.listByIds(orderDetailIds);
        Map<Long, OrderDetail> orderDetailMap = orderDetailList.stream().collect(Collectors.toMap(item -> item.getOrderDetailId(), item -> item));
        //??????????????????
        List<Long> deliveryNoticeIds = deliveryNoticeList.stream().map(item -> item.getDeliveryNoticeId()).collect(Collectors.toList());
        List<DeliveryNotice> deliveryNoticeList2 = this.listByIds(deliveryNoticeIds);
        Map<Long, DeliveryNotice> deliveryNoticeMap = deliveryNoticeList2.stream().collect(Collectors.toMap(item -> item.getDeliveryNoticeId(), item -> item));

        List<OrderDetail> orderDetails = new ArrayList<>();
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            OrderDetail orderDetail = orderDetailMap.get(deliveryNotice.getOrderDetailId());
            if(Objects.isNull(orderDetail)){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("??????????????????????????????[orderDetailId=%s]",deliveryNotice.getOrderDetailId())));
            }
            if(Objects.isNull(orderDetail.getDeliveryNoticeQuantity())){
                orderDetail.setDeliveryNoticeQuantity(BigDecimal.ZERO);
            }
            if(Objects.isNull(orderDetail.getOrderNum())){
                orderDetail.setOrderNum(BigDecimal.ZERO);
            }
            // ???????????????
            DeliveryNotice oldDeliveryNotice = deliveryNoticeMap.get(deliveryNotice.getDeliveryNoticeId());
            BigDecimal quantity = deliveryNotice.getNoticeSum().subtract(oldDeliveryNotice.getNoticeSum());
            if(quantity.compareTo(BigDecimal.ZERO) > 0){
                if(orderDetail.getDeliveryNoticeQuantity().add(quantity)
                        .compareTo(orderDetail.getOrderNum()) > 0
                ) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????,???????????????"));
                }
            }
            // ??????????????????????????????
            OrderDetail detail = new OrderDetail().setOrderDetailId(orderDetail.getOrderDetailId()).
                    setDeliveryNoticeQuantity(orderDetail.getDeliveryNoticeQuantity().add(quantity));
            orderDetails.add(detail);
        }

        //??????????????????
        this.updateBatchById(deliveryNoticeList);

        //????????????????????????
        iOrderDetailService.updateBatchById(orderDetails);

    }

    /**
     * ???????????????????????????
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDeliveryNotice(List<Long> ids) {
        //??????
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        //??????????????????
        List<DeliveryNotice> deliveryNoticeList = this.listByIds(ids);
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(deliveryNoticeList)) {
            List<OrderDetail> orderDetails = new ArrayList<>();
            //??????????????????
            List<Long> orderDetailIds = deliveryNoticeList.stream().map(item -> item.getOrderDetailId()).collect(Collectors.toList());
            List<OrderDetail> orderDetailList = iOrderDetailService.listByIds(orderDetailIds);
            Map<Long, OrderDetail> orderDetailMap = orderDetailList.stream().collect(Collectors.toMap(OrderDetail::getOrderDetailId, Function.identity()));

            //??????
            for(DeliveryNotice deliveryNotice : deliveryNoticeList){
                if(!DeliveryNoticeStatus.DRAFT.getValue().equals(deliveryNotice.getDeliveryNoticeStatus())){
                    throw new BaseException(LocaleHandler.getLocaleMsg(String.format("[deliveryNoticeNum=%s]??????????????????,?????????",deliveryNotice.getDeliveryNoticeNum())));
                }
                OrderDetail orderDetail = orderDetailMap.get(deliveryNotice.getOrderDetailId());
                if (null != orderDetail) {
                    // ?????????????????????
                    BigDecimal deliveryNoticeQuantity = null != orderDetail.getDeliveryNoticeQuantity() ? orderDetail.getDeliveryNoticeQuantity() : BigDecimal.ZERO;
                    // ????????????
                    BigDecimal noticeSum = null != deliveryNotice.getNoticeSum()?deliveryNotice.getNoticeSum():BigDecimal.ZERO;
                    OrderDetail detail = new OrderDetail().setOrderDetailId(orderDetail.getOrderDetailId()).
                            setDeliveryNoticeQuantity(deliveryNoticeQuantity.add(noticeSum));
                    orderDetails.add(detail);
                }
            }
            //????????????????????????
            this.removeByIds(ids);

            // ??????????????????, ??????????????????????????????
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(orderDetails)) {
                iOrderDetailService.updateBatchById(orderDetails);
            }
        }

    }

    /**
     * ???????????????
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void supplierConfirm(List<Long> ids) {
        //??????
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<DeliveryNotice> deliveryNoticeList = this.listByIds(ids);
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            if(!DeliveryNoticeStatus.WAITING_VENDOR_CONFIRM.getValue().equals(deliveryNotice.getDeliveryNoticeStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("[deliveryNoticeNum=%s]??????????????????????????????",deliveryNotice.getDeliveryNoticeNum())));
            }
        }
        //??????????????????
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            deliveryNotice.setDeliveryNoticeStatus(DeliveryNoticeStatus.ACCEPT.getValue());
        }
        this.updateBatchById(deliveryNoticeList);
    }

    /**
     * ???????????????
     * @param deliveryNoticeRequestDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void supplierReject(DeliveryNoticeRequestDTO deliveryNoticeRequestDTO) {
        List<Long> ids = deliveryNoticeRequestDTO.getIds();
        String refusedReason = deliveryNoticeRequestDTO.getRefusedReason();
        //??????
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<DeliveryNotice> deliveryNoticeList = this.listByIds(ids);
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            if(!DeliveryNoticeStatus.WAITING_VENDOR_CONFIRM.getValue().equals(deliveryNotice.getDeliveryNoticeStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("[deliveryNoticeNum=%s]??????????????????????????????",deliveryNotice.getDeliveryNoticeNum())));
            }
        }
        //??????????????????
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            deliveryNotice.setDeliveryNoticeStatus(DeliveryNoticeStatus.REJECT.getValue());
            deliveryNotice.setRefusedReason(refusedReason);
        }
        this.updateBatchById(deliveryNoticeList);

        //???????????????????????? todo
    }

    /**
     * ???????????????-??????????????????
     * @param orderRequestDTO
     * @return
     */
    @Override
    public PageInfo<OrderDetailDTO> listInDeliveryNote(OrderRequestDTO orderRequestDTO) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (ObjectUtils.isEmpty(loginAppUser)){
            return new PageInfo<>(Collections.EMPTY_LIST);
        }
        if (!org.apache.commons.lang3.StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())
                && !org.apache.commons.lang3.StringUtils.equals(UserType.BUYER.name(), loginAppUser.getUserType())) {
            Assert.isTrue(false, "?????????????????????");
        }
        if (org.apache.commons.lang3.StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())) {
            orderRequestDTO.setVendorId(loginAppUser.getCompanyId());
        }
//        List<PurchaseCategory> purchaseCategorieList = baseClient.listMinByIfDeliverPlan();
//        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(purchaseCategorieList)){
//            List<Long> purchaseCategoryIds = purchaseCategorieList.stream().map(PurchaseCategory::getCategoryId).collect(Collectors.toList());
//            orderRequestDTO.setPurchaseCategoryIds(purchaseCategoryIds);
//        }
        PageUtil.startPage(orderRequestDTO.getPageNum(), orderRequestDTO.getPageSize());
        List<OrderDetailDTO> orderDetailDTOS = deliveryNoticeMapper.listInDeliveryNote(orderRequestDTO);
        if(org.apache.commons.collections4.CollectionUtils.isNotEmpty(orderDetailDTOS)){
            orderDetailDTOS.forEach(orderDetailDTO -> orderDetailDTO.setOrderSource(DeliveryNoteSource.DELIVERY_NOTICE.name()));
        }
        return new PageInfo<OrderDetailDTO>(orderDetailDTOS);
    }

    public static enum TYPE {
        ADD(0),   //??????
        UPDATE(1),  //??????
        RELEASE(2); //??????

        private final Integer value;

        TYPE(Integer value) {
            this.value = value;
        }
    }


    /**
     * ????????????????????????
     * @param deliveryNoticeList
     * @param type ADD/UPDATE
     */
    private void checkIfQuantityEnough(List<DeliveryNotice> deliveryNoticeList, Integer type){
        if(CollectionUtils.isEmpty(deliveryNoticeList)){
            return;
        }

        //????????????????????????
        List<Long> orderDetailIdList = deliveryNoticeList.stream().map(item -> item.getDeliveryNoticeId()).collect(Collectors.toList());
        List<OrderDetail> orderDetailList = iOrderDetailService.listByIds(orderDetailIdList);

        List<Long> deliveryNoticeIdList = deliveryNoticeList.stream().map(item -> item.getDeliveryNoticeId()).collect(Collectors.toList());
        List<DeliveryNotice> oldDeliveryNoticeList = this.listByIds(deliveryNoticeIdList);
        Map<Long, DeliveryNotice> oldDeliveryNoticeMap = oldDeliveryNoticeList.stream().collect(Collectors.toMap(item -> item.getDeliveryNoticeId(), item -> item));

        Map<Long, List<DeliveryNotice>> deliveryNoticeMap = getDeliveryNoticeMap(deliveryNoticeList);

        //???null????????????0.0
        for(OrderDetail orderDetail : orderDetailList){
            if(Objects.isNull(orderDetail.getDeliveryNoticeQuantity())){
                orderDetail.setDeliveryNoticeQuantity(BigDecimal.ZERO);
            }
        }
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            if(Objects.isNull(deliveryNotice.getNoticeSum())){
                deliveryNotice.setNoticeSum(BigDecimal.ZERO);
            }
        }
        for(DeliveryNotice deliveryNotice : oldDeliveryNoticeList){
            if(Objects.isNull(deliveryNotice.getNoticeSum())){
                deliveryNotice.setNoticeSum(BigDecimal.ZERO);
            }
        }

        if(TYPE.ADD.value.equals(type)){
            //???????????????
            for(OrderDetail orderDetail : orderDetailList){
                List<DeliveryNotice> list = deliveryNoticeMap.get(orderDetail.getOrderDetailId());
                //??????????????????
                BigDecimal quantity1 = list.stream().map(item -> item.getNoticeSum()).reduce(BigDecimal::add).get();
                //?????????????????????
                BigDecimal quantity2 = orderDetail.getOrderNum().subtract(orderDetail.getDeliveryNoticeQuantity());

                if(quantity1.compareTo(quantity2) > 0){
                    throw new BaseException(LocaleHandler.getLocaleMsg(String.format("????????????[id=%s]????????????,?????????",orderDetail.getOrderDetailId())));
                }
            }

        }else{
            //???????????????
            for(OrderDetail orderDetail : orderDetailList){
                List<DeliveryNotice> list = deliveryNoticeMap.get(orderDetail.getOrderDetailId());
                //quantity1 ??????????????????
                BigDecimal quantity1 = list.stream().map(item -> item.getNoticeSum()).reduce(BigDecimal::add).get();
                //quantity2 ?????????????????????
                BigDecimal quantity2 = BigDecimal.ZERO;
                for(DeliveryNotice deliveryNotice : list) {
                    quantity2 = quantity2.add(oldDeliveryNoticeMap.get(deliveryNotice.getDeliveryNoticeId()).getNoticeSum());
                }
                BigDecimal quantity3 = quantity1.subtract(quantity2);
                //?????????????????????
                BigDecimal quantity4 = orderDetail.getOrderNum().subtract(orderDetail.getDeliveryNoticeQuantity());
                if(quantity3.compareTo(quantity4) > 0){
                    log.info("");
                    throw new BaseException(LocaleHandler.getLocaleMsg(String.format("????????????[id=%s]????????????,?????????",orderDetail.getOrderDetailId())));
                }
            }

        }

    }

    private Map<Long, List<DeliveryNotice>> getDeliveryNoticeMap(List<DeliveryNotice> deliveryNoticeList){
        Map<Long, List<DeliveryNotice>> deliveryNoticeMap = new HashMap<>();
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            if(CollectionUtils.isEmpty(deliveryNoticeMap.get(deliveryNotice.getOrderDetailId()))){
                deliveryNoticeMap.put(deliveryNotice.getOrderDetailId(),new LinkedList<DeliveryNotice>(){{
                    add(deliveryNotice);
                }});
            }else{
                deliveryNoticeMap.get(deliveryNotice.getOrderDetailId()).add(deliveryNotice);
            }
        }
        return deliveryNoticeMap;
    }

    /**
     * ?????????????????? ??????????????????
     * @param deliveryNoticeList
     * @param type ??????/??????/??????
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderDetailQuantity(List<DeliveryNotice> deliveryNoticeList, Integer type){
        //??????????????????
        Map<Long, List<DeliveryNotice>> deliveryNoticeMap = getDeliveryNoticeMap(deliveryNoticeList);

        List<Long> orderDetailIdList = deliveryNoticeList.stream().map(item -> item.getOrderDetailId()).collect(Collectors.toList());
        List<OrderDetail> orderDetailList = iOrderDetailService.listByIds(orderDetailIdList);

        List<Long> deliveryNoticeIdList = deliveryNoticeList.stream().map(item -> item.getDeliveryNoticeId()).collect(Collectors.toList());
        List<DeliveryNotice> oldDeliveryNoticeList = this.listByIds(deliveryNoticeIdList);
        Map<Long, DeliveryNotice> oldDeliveryNoticeMap = oldDeliveryNoticeList.stream().collect(Collectors.toMap(item -> item.getDeliveryNoticeId(), item -> item));

        //???null????????????0.0
        for(OrderDetail orderDetail : orderDetailList){
            if(Objects.isNull(orderDetail.getDeliveryNoticeQuantity())){
                orderDetail.setDeliveryNoticeQuantity(BigDecimal.ZERO);
            }
        }
        for(DeliveryNotice deliveryNotice : deliveryNoticeList){
            if(Objects.isNull(deliveryNotice.getNoticeSum())){
                deliveryNotice.setNoticeSum(BigDecimal.ZERO);
            }
        }
        for(DeliveryNotice deliveryNotice : oldDeliveryNoticeList){
            if(Objects.isNull(deliveryNotice.getNoticeSum())){
                deliveryNotice.setNoticeSum(BigDecimal.ZERO);
            }
        }

        if(TYPE.ADD.value.equals(type)){
            //??????
            updateOrderDetailQuantityAdd(deliveryNoticeMap,oldDeliveryNoticeMap,orderDetailList);
        }else if(TYPE.UPDATE.value.equals(type)){
            //??????
            updateOrderDetailQuantityUpdate(deliveryNoticeMap,oldDeliveryNoticeMap,orderDetailList);
        }else if(TYPE.RELEASE.value.equals(type)){
            //??????
            updateOrderDetailQuantityRelease(deliveryNoticeMap,oldDeliveryNoticeMap,orderDetailList);
        }

    }

    /**
     * ?????????????????????????????????-??????????????????
     * @param deliveryNoticeMap
     * @param oldDeliveryNoticeMap
     * @param orderDetailList
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderDetailQuantityAdd(Map<Long, List<DeliveryNotice>> deliveryNoticeMap,Map<Long, DeliveryNotice> oldDeliveryNoticeMap,List<OrderDetail> orderDetailList){

    }

    /**
     * ?????????????????????????????????-??????????????????
     * @param deliveryNoticeMap
     * @param oldDeliveryNoticeMap
     * @param orderDetailList
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderDetailQuantityUpdate(Map<Long, List<DeliveryNotice>> deliveryNoticeMap,Map<Long, DeliveryNotice> oldDeliveryNoticeMap,List<OrderDetail> orderDetailList){

    }

    /**
     * ?????????????????????????????????-??????????????????
     * @param deliveryNoticeMap
     * @param oldDeliveryNoticeMap
     * @param orderDetailList
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderDetailQuantityRelease(Map<Long, List<DeliveryNotice>> deliveryNoticeMap,Map<Long, DeliveryNotice> oldDeliveryNoticeMap,List<OrderDetail> orderDetailList){

    }
}
