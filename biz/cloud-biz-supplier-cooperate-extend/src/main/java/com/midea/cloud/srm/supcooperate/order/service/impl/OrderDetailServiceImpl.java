package com.midea.cloud.srm.supcooperate.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.neworder.OrderDetailStatus;
import com.midea.cloud.common.enums.pm.po.PurchaseOrderEnum;
import com.midea.cloud.common.enums.pm.ps.CategoryEnum;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.enums.supcooperate.DeliveryNoteSource;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.model.base.formula.dto.calculate.BaseMaterialPriceDTO;
import com.midea.cloud.srm.model.base.formula.dto.calculate.CalculatePriceForOrderResult;
import com.midea.cloud.srm.model.base.formula.dto.calculate.SeaFoodFormulaCalculateParam;
import com.midea.cloud.srm.model.base.formula.vo.BaseMaterialPriceTable;
import com.midea.cloud.srm.model.base.formula.vo.BaseMaterialPriceVO;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.cm.accept.dto.AcceptNumVO;
import com.midea.cloud.srm.model.cm.contract.entity.ContractHead;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.OrderDetailBaseMaterialPriceDTO;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.dto.PurchaseAmountDto;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.OrderDetailDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.OrderRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.Order;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderDetail;
import com.midea.cloud.srm.supcooperate.order.mapper.OrderDetailMapper;
import com.midea.cloud.srm.supcooperate.order.mapper.OrderMapper;
import com.midea.cloud.srm.supcooperate.order.service.IDeliveryNoticeService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderDetailService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ????????????????????? ???????????????
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
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements IOrderDetailService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private IOrderService orderService;
    @Resource
    private IDeliveryNoticeService  iDeliveryNoticeService;

    @Override
    public boolean checkOrderDetailIfQuoteContract(ContractHead contractHead) {
        /**
         * ??????????????????0???????????????????????????????????????????????????????????????????????????
         */
        AtomicBoolean flag = new AtomicBoolean(false);
        if(null != contractHead && StringUtil.notEmpty(contractHead.getContractCode())
                && StringUtil.notEmpty(contractHead.getContractNo())){
            // TODO ????????????????????????
            List<OrderDetail> orderDetails = this.list(Wrappers.lambdaQuery(OrderDetail.class).
                    eq(OrderDetail::getCeeaContractNo, contractHead.getContractCode()).
                    eq(OrderDetail::getContractNum,contractHead.getContractNo()).
                    ne(OrderDetail::getOrderNum,0));
            if(CollectionUtils.isNotEmpty(orderDetails)){
                flag.set(true);
            }
        }
        return flag.get();
    }

    /**
     * ??????????????????
     *
     * @param orderRequestDTO
     * @return
     */
    @Override
    public PageInfo<OrderDetailDTO> listPage(OrderRequestDTO orderRequestDTO) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (!StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())
                && !org.apache.commons.lang3.StringUtils.equals(UserType.BUYER.name(), loginAppUser.getUserType())) {
            Assert.isTrue(false, "?????????????????????");
        }
        if (StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())) {
            orderRequestDTO.setVendorId(loginAppUser.getCompanyId());
        }
        PageUtil.startPage(orderRequestDTO.getPageNum(), orderRequestDTO.getPageSize());
        return new PageInfo<OrderDetailDTO>(orderDetailMapper.findList(orderRequestDTO));
    }

    @Override
    public PageInfo<OrderDetailDTO> listMaterialPage(OrderRequestDTO orderRequestDTO) {
//        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
//        if (ObjectUtils.isEmpty(loginAppUser)){
//            return new PageInfo<>(new ArrayList<>());
//        }
//        if (!StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())
//                && !StringUtils.equals(UserType.BUYER.name(), loginAppUser.getUserType())) {
//            Assert.isTrue(false, "?????????????????????");
//        }
//        if (StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())) {
//            orderRequestDTO.setVendorId(loginAppUser.getCompanyId());
//        }
//        PageUtil.startPage(orderRequestDTO.getPageNum(), orderRequestDTO.getPageSize());
//        QueryWrapper<OrderRequestDTO> wrapper = new QueryWrapper<>();
//        //?????????id
//        wrapper.eq(orderRequestDTO.getVendorId() != null, "VENDOR_ID", orderRequestDTO.getVendorId());
//        //<!--????????????????????????-->
//        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getOrderNumber()), "o.ORDER_NUMBER", orderRequestDTO.getOrderNumber());
//        //??????????????????????????????
//        wrapper.ge(orderRequestDTO.getEndTime() != null, "o.SUBMITTED_TIME", orderRequestDTO.getStartTime());
//        wrapper.le(orderRequestDTO.getStartTime() != null, "o.SUBMITTED_TIME", orderRequestDTO.getEndTime());
//        //?????????????????????
//        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getVendorCode()), "o.VENDOR_CODE", orderRequestDTO.getVendorCode());
//        //?????????id???????????????
//        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getUserIds()), "o.SUBMITTED_ID", orderRequestDTO.getUserIds());
//        //????????????????????????
//        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getMaterialIds()), "od.MATERIAL_CODE", orderRequestDTO.getMaterialIds());
//        //????????????????????????
//        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getMaterialCode()), "od.MATERIAL_CODE", orderRequestDTO.getMaterialCode());
//        //????????????????????????
//        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getMaterialName()), "od.MATERIAL_NAME", orderRequestDTO.getMaterialName());
//        //??????id
//        wrapper.eq(orderRequestDTO.getOrderId() != null, "o.ORDER_ID", orderRequestDTO.getOrderId());
//        //??????
//        orderRequestDTO.setOrderStatus("ACCEPT");
//        wrapper.eq(StringUtils.isNotBlank(orderRequestDTO.getOrderStatus()), "o.ORDER_STATUS", orderRequestDTO.getOrderStatus());
//        //????????????????????????
//        wrapper.eq(orderRequestDTO.getCeeaOrgId() != null, "o.CEEA_ORG_ID", orderRequestDTO.getCeeaOrgId());
//        //????????????????????????
//        wrapper.eq(orderRequestDTO.getOrganizationId() != null, "od.CEEA_ORGANIZATION_ID", orderRequestDTO.getOrganizationId());
//        //???????????????
//        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getDeliverPlanNum()),"od.CEEA_PLAN_RECEIVE_NUM",orderRequestDTO.getDeliverPlanNum());
//        //????????????
//        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getCeeaReceiveAddress()), "o.CEEA_RECEIVE_ADDRESS", orderRequestDTO.getCeeaReceiveAddress());
//        //????????????????????????
//        wrapper.notIn(CollectionUtils.isNotEmpty(orderRequestDTO.getOrderDetailIdList()), "od.ORDER_DETAIL_ID", orderRequestDTO.getOrderDetailIdList());
//        //??????????????????????????????
//        List<PurchaseCategory> purchaseCategorieList = baseClient.listMinByIfDeliverPlan();
//        if (CollectionUtils.isNotEmpty(purchaseCategorieList)){
//            List<Long> purchaseCategories = purchaseCategorieList.stream().map(PurchaseCategory::getCategoryId).collect(Collectors.toList());
//            wrapper.notIn("od.CATEGORY_ID",purchaseCategories);
//        }
//        //wrapper.isNotNull("od.CATEGORY_ID");
//        //2020-12-29 ?????????????????? ???????????????????????????????????????0?????????
//        wrapper.gt("IFNULL(od.ORDER_NUM,0)-IFNULL(od.RECEIVE_SUM,0)",BigDecimal.ZERO);
//
//        wrapper.orderByDesc("o.CREATION_DATE", "od.LINE_NUM");
//        return new PageInfo<OrderDetailDTO>(orderDetailMapper.findMaterialListCopy(wrapper));

        return listMaterialPageNew(orderRequestDTO);
    }

    public PageInfo<OrderDetailDTO> listMaterialPageNew(OrderRequestDTO orderRequestDTO){
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (ObjectUtils.isEmpty(loginAppUser)){
            return new PageInfo<>(Collections.EMPTY_LIST);
        }
        if (!StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())
                && !StringUtils.equals(UserType.BUYER.name(), loginAppUser.getUserType())) {
            Assert.isTrue(false, "?????????????????????");
        }
        if (StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())) {
            orderRequestDTO.setVendorId(loginAppUser.getCompanyId());
        }
//        List<PurchaseCategory> purchaseCategorieList = baseClient.listMinByIfDeliverPlan();
//        if (CollectionUtils.isNotEmpty(purchaseCategorieList)){
//            List<Long> purchaseCategoryIds = purchaseCategorieList.stream().map(PurchaseCategory::getCategoryId).collect(Collectors.toList());
//            orderRequestDTO.setPurchaseCategoryIds(purchaseCategoryIds);
//        }
        PageUtil.startPage(orderRequestDTO.getPageNum(), orderRequestDTO.getPageSize());

        List<OrderDetailDTO> inDeliveryOrder = orderDetailMapper.findInDeliveryOrder(orderRequestDTO);
        if(CollectionUtils.isNotEmpty(inDeliveryOrder)){
            inDeliveryOrder.forEach(orderDetailDTO -> orderDetailDTO.setOrderSource(DeliveryNoteSource.PURCHASE_ORDER.name()));
        }
        return new PageInfo<OrderDetailDTO>(inDeliveryOrder);

    }


    /**
     * todo ????????????
     *
     * @param orderId
     * @param ids
     */
    @Override
    public void deleteBatch(Long orderId, List<Long> ids) {
        Order checkOrder = orderMapper.selectById(orderId);
        Assert.notNull(checkOrder, "???????????????");
        /*if(!StringUtils.equals(PurchaseOrderEnum.ISSUED.getValue(), checkOrder.getOrderStatus())
                &&!StringUtils.equals(PurchaseOrderEnum.REFUSED.getValue(), checkOrder.getOrderStatus())){
            Assert.isTrue(false, "???????????????????????????????????????");
    }*/
        orderDetailMapper.deleteBatchIds(ids);
    }

    @Override
    public OrderDetail getJitOrderDetail(OrderRequestDTO orderRequestDTO) {
        return orderDetailMapper.getJitOrderDetail(orderRequestDTO);
    }

    /**
     * ??????????????????2
     *
     * @param orderRequestDTO
     * @return
     */
    @Override
    public List<OrderDetailDTO> listUnDeliveryPage(OrderRequestDTO orderRequestDTO) {
        return orderDetailMapper.listUnDeliveryPage(orderRequestDTO);
    }

    @Override
    public List<PurchaseAmountDto> aggregateAmount(Long vendorId, Long categoryId) throws ParseException {
        Assert.notNull(vendorId, "vendorId????????????");
        Assert.notNull(categoryId, "categoryId????????????");
        int dayOfYear = LocalDate.now().getYear();
        List<PurchaseAmountDto> purchaseAmountDtos = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> map = getBetweenDate(dayOfYear);
            map.put("vendorId", vendorId);
            map.put("categoryId", categoryId);
            BigDecimal price = this.baseMapper.aggregateAmount(map);
            PurchaseAmountDto purchaseAmountDto = new PurchaseAmountDto();
            purchaseAmountDto.setYear(dayOfYear);
            purchaseAmountDtos.add(purchaseAmountDto);
            if (null != price) {
                purchaseAmountDto.setAmount(price);
            } else {
                purchaseAmountDto.setAmount(BigDecimal.ZERO);
            }
            dayOfYear--;
        }
        return purchaseAmountDtos;
    }

    @Override
    public List<OrderDetailDTO> OrderDetailListPage(OrderRequestDTO orderRequestDTO) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (ObjectUtils.isEmpty(loginAppUser)||ObjectUtils.isEmpty(loginAppUser.getUserType())){
            return new ArrayList<>();
        }
        QueryWrapper<OrderRequestDTO> wrapper = new QueryWrapper<>();
        if (CollectionUtils.isEmpty(orderRequestDTO.getOrderDetailIdList())){
            //?????????????????????????????????
            List<PurchaseCategory> purchaseCategorieList = baseClient.listByCategoryCode(CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode());
            //????????????????????????????????????????????????????????????null
            if (CollectionUtils.isNotEmpty(purchaseCategorieList)){
                List<Long> categoryIdList = purchaseCategorieList.stream().map(PurchaseCategory::getCategoryId).collect(Collectors.toList());
                //????????????????????????
                wrapper.notIn("od.CATEGORY_ID",categoryIdList);
            }
        }
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaContractNo()), "CEEA_CONTRACT_NO", orderRequestDTO.getCeeaContractNo());
        //?????????id
        wrapper.eq(orderRequestDTO.getVendorId() != null, "VENDOR_ID", orderRequestDTO.getVendorId());
        //<!--????????????????????????-->
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getOrderNumber()), "o.ORDER_NUMBER", orderRequestDTO.getOrderNumber());
        //??????????????????????????????
        wrapper.ge(orderRequestDTO.getStartTime() != null, "o.CEEA_PURCHASE_ORDER_DATE", orderRequestDTO.getStartTime());
        wrapper.le(orderRequestDTO.getEndTime() != null, "o.CEEA_PURCHASE_ORDER_DATE", orderRequestDTO.getEndTime());
        //?????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getVendorCode()), "o.VENDOR_CODE", orderRequestDTO.getVendorCode());
        //?????????id???????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getUserIds()), "o.SUBMITTED_ID", orderRequestDTO.getUserIds());
        //????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getMaterialIds()), "od.MATERIAL_CODE", orderRequestDTO.getMaterialIds());
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getMaterialCode()), "od.MATERIAL_CODE", orderRequestDTO.getMaterialCode());
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getMaterialName()), "od.MATERIAL_NAME", orderRequestDTO.getMaterialName());
        //??????id
        wrapper.eq(orderRequestDTO.getOrderId() != null, "o.ORDER_ID", orderRequestDTO.getOrderId());
        //??????
        orderRequestDTO.setOrderStatus("ACCEPT");
        wrapper.eq(StringUtils.isNotBlank(orderRequestDTO.getOrderStatus()), "o.ORDER_STATUS", orderRequestDTO.getOrderStatus());
        //????????????????????????
        wrapper.eq(orderRequestDTO.getCeeaOrgId() != null, "o.CEEA_ORG_ID", orderRequestDTO.getCeeaOrgId());
        //????????????????????????
        wrapper.eq(orderRequestDTO.getOrganizationId() != null, "od.CEEA_ORGANIZATION_ID", orderRequestDTO.getOrganizationId());
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getOrganizationIdList()), "od.CEEA_ORGANIZATION_ID", orderRequestDTO.getOrganizationIdList());
        //????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getCeeaReceiveAddress()), "o.CEEA_RECEIVE_ADDRESS", orderRequestDTO.getCeeaReceiveAddress());
        //????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getOrderDetailIdList()), "od.ORDER_DETAIL_ID", orderRequestDTO.getOrderDetailIdList());
        //????????????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCategoryName()), "od.CATEGORY_NAME", orderRequestDTO.getCategoryName());
        //?????????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaEmpUsername()), "o.CEEA_EMP_USERNAME", orderRequestDTO.getCeeaEmpUsername());

        //???????????????????????????
        //??????????????????0
        if ("Y".equals(orderRequestDTO.getViewType())){
            wrapper.ge("(IFNULL(od.RECEIVED_QUANTITY,0) - IFNULL(od.RECEIVE_NUM,0))",0);
        }else {
            wrapper.gt("(IFNULL(od.RECEIVED_QUANTITY,0) - IFNULL(od.RECEIVE_NUM,0))",0);
        }
        //??????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaRequirementHeadNum()),"od.CEEA_REQUIREMENT_HEAD_NUM",orderRequestDTO.getCeeaRequirementHeadNum());
        //??????????????????????????????
        //wrapper.isNotNull("od.CATEGORY_ID");
        wrapper.orderByDesc("o.CREATION_DATE", "od.LINE_NUM");
        return orderDetailMapper.findListCopy(wrapper);
    }

    @Override
    public List<OrderDetailDTO> OrderDetailListPage1(OrderRequestDTO orderRequestDTO) {
        QueryWrapper<OrderRequestDTO> wrapper = new QueryWrapper<>();
        if (CollectionUtils.isEmpty(orderRequestDTO.getOrderDetailIdList())){
            //?????????????????????????????????
            List<PurchaseCategory> purchaseCategorieList = baseClient.listByCategoryCode(CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode());
            //????????????????????????????????????????????????????????????null
            if (CollectionUtils.isNotEmpty(purchaseCategorieList)){
                List<Long> categoryIdList = purchaseCategorieList.stream().map(PurchaseCategory::getCategoryId).collect(Collectors.toList());
                //????????????????????????
                wrapper.notIn("od.CATEGORY_ID",categoryIdList);
            }
        }
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaContractNo()), "CEEA_CONTRACT_NO", orderRequestDTO.getCeeaContractNo());
        //?????????id
        wrapper.eq(orderRequestDTO.getVendorId() != null, "VENDOR_ID", orderRequestDTO.getVendorId());
        //<!--????????????????????????-->
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getOrderNumber()), "o.ORDER_NUMBER", orderRequestDTO.getOrderNumber());
        //??????????????????????????????
        wrapper.ge(orderRequestDTO.getStartTime() != null, "o.CEEA_PURCHASE_ORDER_DATE", orderRequestDTO.getStartTime());
        wrapper.le(orderRequestDTO.getEndTime() != null, "o.CEEA_PURCHASE_ORDER_DATE", orderRequestDTO.getEndTime());
        //?????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getVendorCode()), "o.VENDOR_CODE", orderRequestDTO.getVendorCode());
        //?????????id???????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getUserIds()), "o.SUBMITTED_ID", orderRequestDTO.getUserIds());
        //????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getMaterialIds()), "od.MATERIAL_CODE", orderRequestDTO.getMaterialIds());
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getMaterialCode()), "od.MATERIAL_CODE", orderRequestDTO.getMaterialCode());
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getMaterialName()), "od.MATERIAL_NAME", orderRequestDTO.getMaterialName());
        //??????id
        wrapper.eq(orderRequestDTO.getOrderId() != null, "o.ORDER_ID", orderRequestDTO.getOrderId());
        //??????
        orderRequestDTO.setOrderStatus("ACCEPT");
        wrapper.eq(StringUtils.isNotBlank(orderRequestDTO.getOrderStatus()), "o.ORDER_STATUS", orderRequestDTO.getOrderStatus());
        //????????????????????????
        wrapper.eq(orderRequestDTO.getCeeaOrgId() != null, "o.CEEA_ORG_ID", orderRequestDTO.getCeeaOrgId());
        //????????????????????????
        wrapper.eq(orderRequestDTO.getOrganizationId() != null, "od.CEEA_ORGANIZATION_ID", orderRequestDTO.getOrganizationId());
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getOrganizationIdList()), "od.CEEA_ORGANIZATION_ID", orderRequestDTO.getOrganizationIdList());
        //????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getCeeaReceiveAddress()), "o.CEEA_RECEIVE_ADDRESS", orderRequestDTO.getCeeaReceiveAddress());
        //????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getOrderDetailIdList()), "od.ORDER_DETAIL_ID", orderRequestDTO.getOrderDetailIdList());
        //????????????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCategoryName()), "od.CATEGORY_NAME", orderRequestDTO.getCategoryName());
        //?????????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaEmpUsername()), "o.CEEA_EMP_USERNAME", orderRequestDTO.getCeeaEmpUsername());

        //???????????????????????????
        //??????????????????0
        if ("Y".equals(orderRequestDTO.getViewType())){
            wrapper.ge("(IFNULL(od.RECEIVED_QUANTITY,0) - IFNULL(od.RECEIVE_NUM,0))",0);
        }else {
            wrapper.gt("(IFNULL(od.RECEIVED_QUANTITY,0) - IFNULL(od.RECEIVE_NUM,0))",0);
        }
        //??????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaRequirementHeadNum()),"od.CEEA_REQUIREMENT_HEAD_NUM",orderRequestDTO.getCeeaRequirementHeadNum());
        //??????????????????????????????
        //wrapper.isNotNull("od.CATEGORY_ID");
        wrapper.orderByDesc("o.CREATION_DATE", "od.LINE_NUM");
        return orderDetailMapper.findListCopy(wrapper);
    }

    @Override
    public List<OrderDetailDTO> OrderDetailListPageCopy(OrderRequestDTO orderRequestDTO) {
        QueryWrapper<OrderRequestDTO> wrapper = new QueryWrapper<>();
        //?????????????????????????????????
        List<PurchaseCategory> purchaseCategorieList = baseClient.listByCategoryCode(CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode());
        //????????????????????????????????????????????????????????????null
        if (CollectionUtils.isEmpty(purchaseCategorieList)){
            return null;
        }
        List<Long> categoryIdList = purchaseCategorieList.stream().map(PurchaseCategory::getCategoryId).collect(Collectors.toList());
        //????????????????????????
        wrapper.in("od.CATEGORY_ID",categoryIdList);
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaContractNo()), "CEEA_CONTRACT_NO", orderRequestDTO.getCeeaContractNo());
        //?????????id
        wrapper.eq(orderRequestDTO.getVendorId() != null, "VENDOR_ID", orderRequestDTO.getVendorId());
        //<!--????????????????????????-->
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getOrderNumber()), "o.ORDER_NUMBER", orderRequestDTO.getOrderNumber());
        //??????????????????????????????
        wrapper.ge(orderRequestDTO.getEndTime() != null, "o.CEEA_PURCHASE_ORDER_DATE", orderRequestDTO.getStartTime());
        wrapper.le(orderRequestDTO.getStartTime() != null, "o.CEEA_PURCHASE_ORDER_DATE", orderRequestDTO.getEndTime());
        //?????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getVendorCode()), "o.VENDOR_CODE", orderRequestDTO.getVendorCode());
        //?????????id???????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getUserIds()), "o.SUBMITTED_ID", orderRequestDTO.getUserIds());
        //????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getMaterialIds()), "o.MATERIAL_CODE", orderRequestDTO.getMaterialIds());
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getMaterialCode()), "o.MATERIAL_CODE", orderRequestDTO.getMaterialCode());
        //????????????????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getMaterialName()), "od.MATERIAL_NAME", orderRequestDTO.getMaterialName());
        //??????id
        wrapper.eq(orderRequestDTO.getOrderId() != null, "o.ORDER_ID", orderRequestDTO.getOrderId());
        //??????
        orderRequestDTO.setOrderStatus("ACCEPT");
        wrapper.eq(StringUtils.isNotBlank(orderRequestDTO.getOrderStatus()), "o.ORDER_STATUS", orderRequestDTO.getOrderStatus());
        //????????????????????????
        wrapper.eq(orderRequestDTO.getCeeaOrgId() != null, "o.CEEA_ORG_ID", orderRequestDTO.getCeeaOrgId());
        //????????????????????????
        wrapper.eq(orderRequestDTO.getOrganizationId() != null, "od.CEEA_ORGANIZATION_ID", orderRequestDTO.getOrganizationId());
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getOrganizationIdList()), "od.CEEA_ORGANIZATION_ID", orderRequestDTO.getOrganizationIdList());
        //????????????
        wrapper.eq(StringUtils.isNotEmpty(orderRequestDTO.getCeeaReceiveAddress()), "o.CEEA_RECEIVE_ADDRESS", orderRequestDTO.getCeeaReceiveAddress());
        //????????????????????????
        wrapper.in(CollectionUtils.isNotEmpty(orderRequestDTO.getOrderDetailIdList()), "od.ORDER_DETAIL_ID", orderRequestDTO.getOrderDetailIdList());
        //????????????????????????0
        if ("Y".equals(orderRequestDTO.getViewType())){
            wrapper.ge("(IFNULL(od.ORDER_NUM,0) - IFNULL(od.RECEIVE_NUM,0))",0);
        }else {
            wrapper.gt("(IFNULL(od.ORDER_NUM,0) - IFNULL(od.RECEIVE_NUM,0))",0);
        }
        //????????????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCategoryName()), "od.CATEGORY_NAME", orderRequestDTO.getCategoryName());
        //?????????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaEmpUsername()), "o.CEEA_EMP_USERNAME", orderRequestDTO.getCeeaEmpUsername());
		//??????????????????
        wrapper.like(StringUtils.isNotEmpty(orderRequestDTO.getCeeaRequirementHeadNum()),"od.CEEA_REQUIREMENT_HEAD_NUM",orderRequestDTO.getCeeaRequirementHeadNum());
        wrapper.orderByDesc("o.CREATION_DATE", "od.LINE_NUM");
        return orderDetailMapper.findListByCopy(wrapper);
    }

    /**
     * ??????????????????
     *
     * @param acceptNumVOList
     */
    @Override
    @Transactional
    public void acceptBuyerSubmit(List<AcceptNumVO> acceptNumVOList) {
        //?????????????????????????????????
        List<PurchaseCategory> purchaseCategorieList = baseClient.listByCategoryCode(CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode());
        //????????????????????????????????????????????????????????????null
        Map<Long, PurchaseCategory> purchaseCategorieMap=new HashMap<>();
        if (CollectionUtils.isNotEmpty(purchaseCategorieList)){
            purchaseCategorieMap = purchaseCategorieList.stream().collect(Collectors.toMap(PurchaseCategory::getCategoryId, Function.identity()));
        }
        for (AcceptNumVO acceptNumVO : acceptNumVOList) {
            OrderDetail orderDetail = this.getById(acceptNumVO.getOrderDetailId());
            //?????????????????????????????????????????????
            if (CollectionUtils.isNotEmpty(purchaseCategorieList)){
                //????????????????????????????????????????????????????????????
                PurchaseCategory purchaseCategory = purchaseCategorieMap.get(orderDetail.getCategoryId());
                if (purchaseCategory!=null){
                    orderDetail.setReceivedQuantity(orderDetail.getOrderNum());
                }
            }
            //????????????
            BigDecimal ceeaDamageQuantity = acceptNumVO.getCeeaDamageQuantity()==null?new BigDecimal(0) : acceptNumVO.getCeeaDamageQuantity();
            //??????????????????
            BigDecimal acceptQuantity = acceptNumVO.getAcceptQuantity()==null?new BigDecimal(0) : acceptNumVO.getAcceptQuantity();
            //???????????????
            BigDecimal receiveNum = ceeaDamageQuantity.add(acceptQuantity.add(orderDetail.getReceiveNum() == null ? new BigDecimal(0) : orderDetail.getReceiveNum()));
            Assert.isTrue((orderDetail.getReceivedQuantity()==null?new BigDecimal(0):orderDetail.getReceivedQuantity()).compareTo(receiveNum) >-1, "??????????????????????????????????????????????????????");
            //?????????????????????
            orderDetail.setReceiveNum(receiveNum);
            //???????????????
            orderDetail.setNotInvoiceQuantity(Optional.ofNullable(orderDetail.getNotInvoiceQuantity()).orElse(new BigDecimal(0)).add(acceptQuantity));
            //?????????????????????
            this.updateById(orderDetail);
        }
    }

    @Override
    public List<OrderDetail> getOrderDetailForCheck(Long requirementLineId) {
        return orderDetailMapper.getOrderDetailForCheck(requirementLineId);
    }

    @Override
    public List<OrderDetail> getOrderDetailByOrderStatus() {
        return orderDetailMapper.getOrderDetailByOrderStatus();
    }

    @Override
    public BaseResult calcBaseMaterialPrice(OrderDetailBaseMaterialPriceDTO orderDetailBaseMaterialPriceDTO) {
        List<Long> orderDetailIds = orderDetailBaseMaterialPriceDTO.getOrderDetailIds();
        Map<Long , BigDecimal> orderDetailPrice = new HashMap<>();

        List<BaseMaterialPriceDTO> baseMaterialPriceDTOS = new ArrayList<>();
        for(BaseMaterialPriceTable inputBasePrice : orderDetailBaseMaterialPriceDTO.getBaseMaterialPrices()){
            BaseMaterialPriceDTO calcDto = new BaseMaterialPriceDTO();
            BaseMaterialPriceVO firstPriceVo =  inputBasePrice.getPrices().get(0);
            calcDto.setBaseMaterialId(inputBasePrice.getBaseMaterialId());
            calcDto.setBaseMaterialName(inputBasePrice.getBaseMaterialName());
            calcDto.setEssentialFactorId(firstPriceVo.getEssentialFactorId());
            calcDto.setBaseMaterialPrice(firstPriceVo.getBaseMaterialPrice());
            baseMaterialPriceDTOS.add(calcDto);
        }
        if(baseMaterialPriceDTOS.isEmpty()){
            throw new BaseException("????????????????????????????????????");
        }
        //1.??????????????????
        List<OrderDetail> orderDetailList = listByIds(orderDetailIds);
        if(orderDetailList.isEmpty()){
            throw new BaseException("??????????????????????????????????????????");
        }
        //2.????????????
        List<SeaFoodFormulaCalculateParam> calcParams = new ArrayList<>();
        for(OrderDetail orderDetail : orderDetailList){
            SeaFoodFormulaCalculateParam param = new SeaFoodFormulaCalculateParam();
            baseMaterialPriceDTOS.stream().peek(dto -> {
                dto.setFormulaId(orderDetail.getCeeaFormulaId());
            });
            param.setCalculateId(orderDetail.getOrderDetailId().toString());
            param.setMaterialId(orderDetail.getMaterialId());
            param.setPriceJSON(JSONObject.toJSONString(baseMaterialPriceDTOS));
            param.setFormulaId(orderDetail.getCeeaFormulaId());
            param.setEssentialFactorValues(orderDetail.getEssentialFactorValues());
            calcParams.add(param);
        }
        Map<String , CalculatePriceForOrderResult> orderDetailCalcResult = baseClient.calculatePriceForOrderBatch(calcParams);
        //3.???????????????
        List<OrderDetail> updateOrderDetailList = new ArrayList<>();
        orderDetailList.forEach(orderDetail -> {
            CalculatePriceForOrderResult calcResult = orderDetailCalcResult.get(orderDetail.getOrderDetailId().toString());
            if(Objects.nonNull(calcResult)){
                orderDetail.setCeeaUnitTaxPrice(calcResult.getCalcResult());
                orderDetail.setCeeaFormulaResult(JSONObject.toJSONString(calcResult.getFormulaAndParam()));
                orderDetailPrice.put(orderDetail.getOrderDetailId() , calcResult.getCalcResult());
                updateOrderDetailList.add(orderDetail);
            }
        });
        saveOrUpdateBatch(updateOrderDetailList);
        return BaseResult.buildSuccess(orderDetailPrice);
    }

    @Override
    public List<OrderDetail> listValidOrderDetail(List<OrderDetail> orderDetails) {
        if(orderDetails.isEmpty()){
            return new ArrayList<>();
        }
        //????????????????????????confirm
        List<Long> priceSourceIds =  orderDetails.stream().map(
                o -> Optional.ofNullable(o.getCeeaPriceSourceId()).orElse(-1l)
        ).collect(Collectors.toList());
        OrderDetail orderDetalParam = new OrderDetail()
                .setCeeaPriceSourceType(orderDetails.get(0).getCeeaPriceSourceType());
        return orderDetailMapper.listValidOrderDetail(priceSourceIds , orderDetalParam);
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
        List<OrderDetail> orderDetailList = this.listByIds(ids);
        for(OrderDetail orderDetail : orderDetailList){
            if(!OrderDetailStatus.WAITING_VENDOR_CONFIRM.getValue().equals(orderDetail.getOrderDetailStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????,?????????"));
            }
        }
        List<Long> orderIds = orderDetailList.stream().map(item -> item.getOrderId()).collect(Collectors.toList());
        QueryWrapper<Order> orderWrapper = new QueryWrapper<>();
        orderWrapper.in("ORDER_ID",orderIds);
        List<Order> orderList = orderService.list(orderWrapper);
        for(Order order : orderList){
            if(!PurchaseOrderEnum.APPROVED.getValue().equals(order.getOrderStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,?????????"));
            }
        }

        //??????????????????
        for(OrderDetail orderDetail : orderDetailList){
            orderDetail.setOrderDetailStatus(OrderDetailStatus.ACCEPT.getValue());
        }
        this.updateBatchById(orderDetailList);

        //???????????????
        for(Order order : orderList){
            order.setIfDetailHandle("Y");
        }
        orderService.updateBatchById(orderList);

    }

    /**
     * ???????????????
     * @param param
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void supplierReject(OrderDetail param) {
        //??????
        List<Long> ids = param.getIds();
        String refusedReason = param.getRefusedReason();
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<OrderDetail> orderDetailList = this.listByIds(ids);
        for(OrderDetail orderDetail : orderDetailList){
            if(!OrderDetailStatus.WAITING_VENDOR_CONFIRM.getValue().equals(orderDetail.getOrderDetailStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????,?????????"));
            }
        }
        List<Long> orderIds = orderDetailList.stream().map(item -> item.getOrderId()).collect(Collectors.toList());
        QueryWrapper<Order> orderWrapper = new QueryWrapper<>();
        orderWrapper.in("ORDER_ID",orderIds);
        List<Order> orderList = orderService.list(orderWrapper);
        for(Order order : orderList){
            if(!PurchaseOrderEnum.APPROVED.getValue().equals(order.getOrderStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,?????????"));
            }
        }

        //??????????????????
        for(OrderDetail orderDetail : orderDetailList){
            orderDetail.setOrderDetailStatus(OrderDetailStatus.REJECT.getValue());
            orderDetail.setRefusedReason(refusedReason);
        }
        this.updateBatchById(orderDetailList);

        //???????????????
        for(Order order : orderList){
            order.setIfDetailHandle("Y");
        }
        orderService.updateBatchById(orderList);

    }

    /**
     * ??????????????? - ??????????????????
     * @param orderDetailDTO
     * @return
     */
    @Override
    public PageInfo<OrderDetailDTO> listInDeliveryNotice(OrderDetailDTO orderDetailDTO) {
        PageUtil.startPage(orderDetailDTO.getPageNum(),orderDetailDTO.getPageSize());
        List<OrderDetailDTO> orderDetailList = orderDetailMapper.listInDeliveryNotice(orderDetailDTO);
        for(OrderDetailDTO orderDetail : orderDetailList){
            //??????????????????
            orderDetail.setDeliveryTime(new Date());
            //????????????????????????
//            orderDetail.setNoticeSum(BigDecimal.ZERO);
            // ??????????????????????????????
            BigDecimal deliveryNoticeQuantity = orderDetail.getDeliveryNoticeQuantity();
            // ????????????
            BigDecimal orderNum = orderDetail.getOrderNum();
            // ???????????????????????????
            BigDecimal surplusDeliveryQuantity = orderNum.subtract(deliveryNoticeQuantity);
            surplusDeliveryQuantity = surplusDeliveryQuantity.compareTo(BigDecimal.ZERO) >= 0 ? surplusDeliveryQuantity : BigDecimal.ZERO;
            orderDetail.setSurplusDeliveryQuantity(surplusDeliveryQuantity);
            orderDetail.setNoticeSum(surplusDeliveryQuantity);
        }
        return new PageInfo<OrderDetailDTO>(orderDetailList);
    }

    /**
     * ????????????????????????
     * @param orderRequestDTO
     * @return
     */
    @Override
    @AuthData(module = {MenuEnum.BUYER_PURCHASE_ORDER , MenuEnum.SUPPLIER_SIGN} )
    public PageInfo<OrderDetailDTO> listPageNew(OrderRequestDTO orderRequestDTO) {
        PageUtil.startPage(orderRequestDTO.getPageNum(),orderRequestDTO.getPageSize());
        List<OrderDetailDTO> orderDetailDTOList = orderDetailMapper.list(orderRequestDTO);
        return new PageInfo<OrderDetailDTO>(orderDetailDTOList);
    }

    /**
     * ???????????????????????????
     * @param orderRequestDTO
     * @return
     */
    @Override
    public PageInfo<OrderDetailDTO> listInDeliveryNote(OrderRequestDTO orderRequestDTO) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (ObjectUtils.isEmpty(loginAppUser)){
            return new PageInfo<>(new ArrayList<>());
        }
        if (!StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())
                && !StringUtils.equals(UserType.BUYER.name(), loginAppUser.getUserType())) {
            Assert.isTrue(false, "?????????????????????");
        }
        if (StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())) {
            orderRequestDTO.setVendorId(loginAppUser.getCompanyId());
        }
        PageUtil.startPage(orderRequestDTO.getPageNum(), orderRequestDTO.getPageSize());
        return new PageInfo<OrderDetailDTO>(orderDetailMapper.listInDeliveryNote(orderRequestDTO));
    }

    public Map<String, Object> getBetweenDate(int year) throws ParseException {
        String dateModel1 = "$date-01-01 00:00:00";
        String dateModel2 = "$date-12-31 23:59:59";
        Map<String, Object> betweenDate = new HashMap<>();
        betweenDate.put("startDate", DateUtil.parseDate(dateModel1.replace("$date", String.valueOf(year))));
        betweenDate.put("endDate", DateUtil.parseDate(dateModel2.replace("$date", String.valueOf(year))));
        return betweenDate;
    }
}
