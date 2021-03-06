package com.midea.cloud.srm.supcooperate.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.enums.neworder.OrderDetailStatus;
import com.midea.cloud.common.enums.order.ResponseStatus;
import com.midea.cloud.common.enums.pm.po.PurchaseOrderEnum;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.enums.review.OrgStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.component.handler.AutoMetaObjContext;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.OrganizationUser;
import com.midea.cloud.srm.model.base.work.dto.WorkCount;
import com.midea.cloud.srm.model.logistics.po.order.entity.OrderHead;
import com.midea.cloud.srm.model.pm.pr.requirement.param.OrderQuantityParam;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.FinanceInfo;
import com.midea.cloud.srm.model.supplier.info.entity.OrgInfo;
import com.midea.cloud.srm.model.suppliercooperate.excel.ErrorCell;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.*;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.Order;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderAttach;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderDetail;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderPaymentProvision;
import com.midea.cloud.srm.supcooperate.order.mapper.OrderDetailMapper;
import com.midea.cloud.srm.supcooperate.order.mapper.OrderMapper;
import com.midea.cloud.srm.supcooperate.order.service.IOrderAttachService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderDetailService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderPaymentProvisionService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.core.Local;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ??????????????? ???????????????
 * </pre>
 *
 * @author huangbf3
 * @version 1.00.00
 *
 *          <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/3/19 19:13
 *  ????????????:
 *          </pre>
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

	@Autowired
	private IOrderDetailService iOrderDetailService;
	@Autowired
	private IOrderAttachService iOrderAttachService;
	@Autowired
	private IOrderPaymentProvisionService iOrderPaymentProvisionService;
	@Resource
	private OrderMapper orderMapper;
	@Autowired
	private BaseClient baseClient;
	@Autowired
	private SupplierClient supplierClient;
	@Autowired
	private PmClient pmClient;
	@Autowired
	private FileCenterClient fileCenterClient;
	@Resource
	private OrderDetailMapper orderDetailMapper;

	@Override
	@AuthData(module = {MenuEnum.BUYER_PURCHASE_ORDER , MenuEnum.SUPPLIER_SIGN} )
	public List<OrderDTO> listPage(OrderRequestDTO orderRequestDTO) {
		return orderMapper.findList(orderRequestDTO);
	}


	private List<OrderDTO> listPageForBuyers(OrderRequestDTO orderRequestDTO){
		return orderMapper.findList(orderRequestDTO);
	}

	@Override
	@AuthData(module = {MenuEnum.PURCHASE_ORDER_CHANGE ,MenuEnum.SUPPLIER_SIGN})
	public List<OrderDTO> listPageUpdates(OrderRequestDTO orderRequestDTO) {
		return orderMapper.findListUpdates(orderRequestDTO);
	}

	@Override
	@Transactional
	public void saveOrUpdate(OrderSaveRequestDTO param) {
		Order order = param.getOrder();
		//??????????????? ??????????????? ???????????????
		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
		if(Objects.isNull(loginAppUser)){
			//?????????????????????
			AutoMetaObjContext.noOp();
//			if(Objects.isNull(order.getCreatedId()) ||
//					StringUtils.isBlank(order.getCreatedBy()) ||
//					StringUtils.isBlank(order.getCreatedByIp()) ||
//					Objects.isNull(order.getCreationDate())
//			){
//				order.setCreatedId(-1L);
//				order.setCreatedBy("???????????????");
//				order.setCreatedByIp("127.0.0.1");
//				order.setCreationDate(new Date());
//			}
//			if(Objects.isNull(order.getLastUpdatedId()) ||
//					StringUtils.isBlank(order.getLastUpdatedBy()) ||
//					StringUtils.isBlank(order.getLastUpdatedByIp()) ||
//					Objects.isNull(order.getLastUpdateDate())
//			){
//				order.setLastUpdatedId(-1L);
//				order.setLastUpdatedBy("???????????????");
//				order.setLastUpdatedByIp("127.0.0.1");
//				order.setLastUpdateDate(new Date());
//			}
		}

		List<OrderDetail> detailList = param.getDetailList();
		List<OrderAttach> orderAttachList = param.getAttachList();
		List<OrderPaymentProvision> orderPaymentProvisionList = param.getPaymentProvisionList();
		long d1 = System.currentTimeMillis();
		this.saveOrUpdate(order);
		long d2 = System.currentTimeMillis();
		/*??????????????????*/
		List<OrderDetail> removeOrderDetailList = orderDetailMapper.selectList(new QueryWrapper(new OrderDetail().setOrderId(order.getOrderId())));
		List<Long> orderDetailIds = removeOrderDetailList.stream().map(item -> item.getOrderDetailId()).collect(Collectors.toList());
		if(!CollectionUtils.isEmpty(orderDetailIds)){
			iOrderDetailService.removeByIds(orderDetailIds);
		}
		long d22 = System.currentTimeMillis();
		for (OrderDetail orderDetail : detailList) {
			if(orderDetail.getCeeaContractNo().contains("|??????|")){
//				int i = orderDetail.getCeeaContractNo().lastIndexOf("|??????|");
//				orderDetail.setCeeaContractNo(orderDetail.getCeeaContractNo().substring(0,i));
				String[] split = orderDetail.getCeeaContractNo().split("\\|??????\\|");
				if(split.length>1){
					orderDetail.setCeeaContractNo(split[1]);
				}

			}
		}
		iOrderDetailService.saveBatch(detailList);
		long d3 = System.currentTimeMillis();
		iOrderAttachService.remove(new QueryWrapper<>(new OrderAttach().setOrderId(order.getOrderId())));
		iOrderAttachService.saveBatch(orderAttachList);
		long d4= System.currentTimeMillis();
		List<Long> fileUploadIds = orderAttachList.stream().map(item -> item.getFileuploadId()).collect(Collectors.toList());
		if(fileUploadIds.size() > 0){
			fileCenterClient.binding(fileUploadIds,order.getOrderId());
		}
		long d5 = System.currentTimeMillis();
		if(!CollectionUtils.isEmpty(orderPaymentProvisionList)){
			iOrderPaymentProvisionService.remove(new QueryWrapper<>(new OrderPaymentProvision().setOrderId(order.getOrderId())));
			iOrderPaymentProvisionService.saveBatch(orderPaymentProvisionList);
		}
		long d6 = System.currentTimeMillis();
		System.out.println("d2-d1:" + (d2 - d1));
		System.out.println("d2~" +(d3-d2));
		System.out.println("d3~"+(d4-d3));
		System.out.println("d4~"+(d5-d4));
		System.out.println("d2_d22" + (d22-d2));
	}

	@Override
	@Transactional
	public List<ErrorCell> saveBatchByExcel(List<ExcelOrderRequestDTO> list) {
		List<ErrorCell> errorCells = new ArrayList<>();

		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
		List<OrganizationUser> organizations = loginAppUser.getOrganizationUsers();
		List<DictItemDTO> deliveryLevelDictItems = baseClient.listAllByDictCode("DELIVERY_LEVEL");
		List<MaterialItem> materialItems = baseClient.listAllMaterialItem();
		for (ExcelOrderRequestDTO eord : list) {
			CompanyInfo companyInfo1 = new CompanyInfo();
			companyInfo1.setCompanyName(eord.getVendorName());
			companyInfo1.setCompanyCode(eord.getVendorCode());
			companyInfo1.setStatus("APPROVED");
			CompanyInfo companyInfo = supplierClient.getCompanyInfoByParam(companyInfo1);
			if (companyInfo == null) {
				eord.getErrorCell("vendorName").setComment("?????????????????????'" + eord.getVendorName() + "'?????????????????????");
				errorCells.add(eord.getErrorCell("vendorName"));
			} else {
				eord.setVendorId(companyInfo.getCompanyId());
				eord.setVendorCode(companyInfo.getCompanyCode());
			}

			OrganizationUser organization = getOrganization(organizations, eord.getOrganizationName());
			if (organization == null) {
				eord.getErrorCell("organizationName").setComment("?????????????????????'" + eord.getOrganizationName() + "'???????????????");
				errorCells.add(eord.getErrorCell("organizationName"));
			} else {
				eord.setOrganizationId(organization.getOrganizationId());
			}

			if (organization != null && companyInfo != null) {
				// ????????????????????????????????????????????????
				OrgInfo orgInfo = supplierClient.getOrgInfoByOrgIdAndCompanyId(organization.getOrganizationId(), companyInfo.getCompanyId());
				if (!checkOrgEffective(orgInfo)) {
					eord.getErrorCell("organizationName").setComment("?????????????????????'" + eord.getOrganizationName() + "'???????????????");
					errorCells.add(eord.getErrorCell("organizationName"));
				}

				FinanceInfo financeInfo = supplierClient.getFinanceInfoByCompanyIdAndOrgId(companyInfo.getCompanyId(), organization.getOrganizationId());
				if (financeInfo == null) {
					eord.getErrorCell("vendorName").setComment("????????????????????????'" + eord.getVendorName() + "',?????????:'" + eord.getOrganizationName() + "'???????????????,???????????????????????????????????????????????????");
					errorCells.add(eord.getErrorCell("vendorName"));
				} else {
					eord.setTaxKey(financeInfo.getTaxKey());
					eord.setTaxRate(new BigDecimal(financeInfo.getTaxRate()));
					eord.setRfqSettlementCurrency(financeInfo.getClearCurrency());
					eord.setPaymentMethod(financeInfo.getPaymentMethod());
					eord.setTermOfPayment(financeInfo.getPaymentTerms());
				}
			}

			String deliveryLevel = getDictItemCode(deliveryLevelDictItems, eord.getDeliveryLevel());
			if (deliveryLevel == null) {
				eord.getErrorCell("deliveryLevel").setComment("?????????????????????'" + eord.getDeliveryLevel() + "'???????????????");
				errorCells.add(eord.getErrorCell("deliveryLevel"));
			} else {
				eord.setDeliveryLevel(deliveryLevel);
			}

			for (OrderDetailDTO orderDetailDTO : eord.getList()) {
				MaterialItem materialItem = getMaterialItem(materialItems, orderDetailDTO);
				if (materialItem == null) {
					eord.getErrorCell("materialCode").setComment("?????????????????????'" + orderDetailDTO.getMaterialCode() + "'?????????");
					errorCells.add(eord.getErrorCell("materialCode"));
				} else {
					orderDetailDTO.setMaterialId(materialItem.getMaterialId());
					orderDetailDTO.setMaterialCode(materialItem.getMaterialCode());
					orderDetailDTO.setMaterialName(materialItem.getMaterialName());
					orderDetailDTO.setUnit(materialItem.getUnit());
					orderDetailDTO.setSpecification(materialItem.getSpecification());
					orderDetailDTO.setCategoryName(materialItem.getSpecification());
				}

				// ???????????? ??????????????????????????????????????????????????????
				orderDetailDTO.setUnitPriceExcludingTax(new BigDecimal(0));

//                "unitPriceContainingTax": 1,
//                        "unitPriceExcludingTax": 2
			}

			eord.setBuyerName(loginAppUser.getUsername());
			eord.setTel(loginAppUser.getPhone());
//
			// ????????????????????????????????????
			eord.setOrderAmount(new BigDecimal(100000));

		}

		if (errorCells.size() > 0) {
			return errorCells;
		}

		for (ExcelOrderRequestDTO eord : list) {
			Order order = JSONObject.toJavaObject((JSON) JSONObject.parse(JSONObject.toJSONString(eord)), Order.class);
			List detailList = new ArrayList();
			for (OrderDetailDTO orderDetailDTO : eord.getList()) {
				OrderDetail orderDetail = JSONObject.toJavaObject((JSON) JSONObject.parse(JSONObject.toJSONString(orderDetailDTO)), OrderDetail.class);
				detailList.add(orderDetail);
			}
			try {
				this.saveOrUpdate(new OrderSaveRequestDTO()
						.setOrder(order)
						.setDetailList(detailList)
						.setAttachList(new ArrayList<OrderAttach>())
						.setPaymentProvisionList(new ArrayList<OrderPaymentProvision>())
				);
			} catch (Exception e) {
				log.error("????????????????????????",e);
				eord.getErrorCell("lineErrorContents").setLineErrorContents("??????????????????????????????????????????????????????");
				errorCells.add(eord.getErrorCell("lineErrorContents"));
			}

		}
		return errorCells;
	}

	/**
	 * ??????????????????????????????
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

	@Override
	@Transactional
	public void submitBatch(List<Long> ids) {
		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
		List list = new ArrayList();

		ids.forEach(item -> {
			Order checkOrder = orderMapper.selectById(item);
			Assert.notNull(checkOrder, "???????????????");
			/*if (!StringUtils.equals(PurchaseOrderEnum.ISSUED.getValue(), checkOrder.getOrderStatus()) && !StringUtils.equals(PurchaseOrderEnum.REFUSED.getValue(), checkOrder.getOrderStatus())) {
				Assert.isTrue(false, "????????????????????????????????????");
			}*/

			Order order = new Order();
			order.setOrderId(item);
			order.setResponseStatus(ResponseStatus.UNCOMFIRMED.name());
			/*order.setOrderStatus(PurchaseOrderEnum.ISSUED.getValue());*/
			order.setSubmittedId(loginAppUser.getUserId());
			order.setSubmittedBy(loginAppUser.getUsername());
			order.setSubmittedTime(new Date());

			list.add(order);
		});
		this.updateBatchById(list);
	}

	@Override
	@Transactional
	public void comfirmBatch(List<Long> ids) {
		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();

		List list = new ArrayList();

		ids.forEach(item -> {
			Order checkOrder = orderMapper.selectById(item);
			Assert.notNull(checkOrder, "???????????????");
			/*if (!StringUtils.equals(PurchaseOrderEnum.ISSUED.getValue(), checkOrder.getOrderStatus()) && !StringUtils.equals(PurchaseOrderEnum.RETURNED.getValue(), checkOrder.getOrderStatus())) {
				Assert.isTrue(false, "?????????????????????????????????????????????");
			}*/


			Order order = new Order();
			order.setOrderId(item);
			order.setResponseStatus(ResponseStatus.COMFIRMED.name());
			order.setOrderStatus(PurchaseOrderEnum.ACCEPT.getValue());
			order.setComfirmId(loginAppUser.getUserId());
			order.setComfirmBy(loginAppUser.getUsername());
			order.setComfirmTime(new Date());

			list.add(order);
		});
		this.updateBatchById(list);
	}

	@Override
	@Transactional
	public void refuseBatch(List<Order> list) {
		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();

		list.forEach(item -> {
			Assert.notNull(item.getRefuseReason(), "????????????????????????");

			Order checkOrder = orderMapper.selectById(item);
			Assert.notNull(checkOrder, "???????????????");
			/*Assert.isTrue(StringUtils.equals(PurchaseOrderEnum.ISSUED.getValue(), checkOrder.getOrderStatus()), "??????????????????????????????");*/


			Order order = new Order();
			order.setOrderId(item.getOrderId());
			order.setResponseStatus(ResponseStatus.REJECTED.name());
			order.setOrderStatus(PurchaseOrderEnum.REFUSED.getValue());
			order.setRefuseId(loginAppUser.getUserId());
			order.setRefuseBy(loginAppUser.getUsername());
			order.setRefuseTime(new Date());
			order.setRefuseReason(item.getRefuseReason());

			List<OrderDetail> orderDetails = iOrderDetailService.list(new QueryWrapper<OrderDetail>().eq("ORDER_ID",order.getOrderId()));

			pmClient.orderReturn(orderDetails);
			updateOrderQuantity(order.getOrderId());

			this.updateById(order);
		});
	}

	/**
	 * ???????????????????????????????????????????????????
	 * @param orderId
	 */
	private void updateOrderQuantity(Long orderId){
		try{
			List<OrderDetail> orderDetails = iOrderDetailService.list(new QueryWrapper<OrderDetail>().eq("ORDER_ID",orderId));
			List<OrderQuantityParam> orderQuantityParams = new ArrayList<>();
			for(OrderDetail item:orderDetails){
				if(item.getExternalId()!=null&&item.getExternalRowNum()!=null&&item.getOrderNum()!=null){
					OrderQuantityParam orderQuantityParam = new OrderQuantityParam();
					orderQuantityParam.setRequirementLineId(item.getExternalRowId());
					orderQuantityParam.setRevertAmount(item.getOrderNum());

					orderQuantityParams.add(orderQuantityParam);
				}
			}
			if(orderQuantityParams.size()>0){
				//????????????????????????????????????????????? updateOrderQuantity
				log.info(JSONObject.toJSONString(orderQuantityParams));
				pmClient.updateOrderQuantityBatch(orderQuantityParams);
			}
		}catch (Exception e){
			log.error("????????????",e);
			Assert.isTrue(false,"??????????????????????????????????????????????????????????????????");
		}
	}

	@Override
	public WorkCount countSubmit(OrderRequestDTO orderRequestDTO) {
		WorkCount workCount = new WorkCount();
		Integer count = orderMapper.countSubmit(orderRequestDTO);
		workCount.setTitle("???????????????");
		workCount.setUrl("/vendorOrderSynergy/vendorPurchaseOrder?from=workCount&orderStatus=" + orderRequestDTO.getOrderStatus());
		workCount.setCount(count);
		return workCount;
	}

	@Override
	@Transactional
	public void comfirm(Order order, List<OrderDetail> detailList) {
		Order checkOrder = orderMapper.selectById(order);
		Assert.notNull(checkOrder, "???????????????");
		/*if (!StringUtils.equals(PurchaseOrderEnum.ISSUED.getValue(), checkOrder.getOrderStatus()) && !StringUtils.equals(PurchaseOrderEnum.RETURNED.getValue(), checkOrder.getOrderStatus())) {
			Assert.isTrue(false, "?????????????????????????????????????????????");
		}

		order.setOrderStatus(PurchaseOrderEnum.COMFIRMED.getValue());*/
		order.setResponseStatus(ResponseStatus.COMFIRMED.name());
		this.updateById(order);
		iOrderDetailService.updateBatchById(detailList);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long orderId) {
		this.removeById(orderId);
		iOrderDetailService.remove(new QueryWrapper<OrderDetail>(new OrderDetail().setOrderId(orderId)));
		iOrderAttachService.remove(new QueryWrapper<OrderAttach>(new OrderAttach().setOrderId(orderId)));
		iOrderPaymentProvisionService.remove(new QueryWrapper<OrderPaymentProvision>(new OrderPaymentProvision().setOrderId(orderId)));
	}

    @Override
    public List<Order> listOrderByOrderNumberOrErpNumber(List<String> numbers) {
		//????????????ERP??????,?????????????????????orderNumber??????(????????????)
		List<Order> orders = new ArrayList<>();
		for (String number : numbers) {
			if (StringUtils.isBlank(number)) continue;
			Order order = this.getOne(Wrappers.lambdaQuery(Order.class).eq(Order::getEprOrderNumber, number));
			if (order == null) {
				order = this.getOne(Wrappers.lambdaQuery(Order.class).eq(Order::getOrderNumber, number));
			}else {
				order.setOrderNumber(order.getEprOrderNumber());
			}
			orders.add(order);
		}
		return orders;
    }

	/**
	 * ?????????????????????????????????
	 * @param orderNumbers
	 * @return
	 */
	@Override
	public List<Order> listByOrderNumbers(List<String> orderNumbers) {
		if(CollectionUtils.isEmpty(orderNumbers)){
			return Collections.EMPTY_LIST;
		}
		QueryWrapper<Order> wrapper = new QueryWrapper<>();
		wrapper.in("ORDER_NUMBER",orderNumbers);
		return this.list(wrapper);
	}

	/**
	 * ??????????????????????????????ID
	 *
	 * @param organizations
	 * @param organizationName
	 * @return
	 */
	private OrganizationUser getOrganization(List<OrganizationUser> organizations, String organizationName) {
		for (OrganizationUser organizationUser : organizations) {
			if (StringUtils.equals(organizationName, organizationUser.getOrganizationName())) {
				return organizationUser;
			}
		}
		return null;
	}

	/**
	 * ??????????????????????????????Id
	 *
	 * @param materialItems
	 * @param orderDetailDTO
	 * @return
	 */
	private MaterialItem getMaterialItem(List<MaterialItem> materialItems, OrderDetailDTO orderDetailDTO) {
		for (MaterialItem materialItem : materialItems) {
			if (StringUtils.equals(orderDetailDTO.getMaterialCode(), materialItem.getMaterialCode())) {
				return materialItem;
			}
		}
		return null;
	}

	/**
	 * ????????????????????????????????????????????????
	 *
	 * @param dictItems
	 * @param dictItemName
	 * @return
	 */
	private String getDictItemCode(List<DictItemDTO> dictItems, String dictItemName) {
		for (DictItemDTO dictItemDTO : dictItems) {
			if (StringUtils.equals(dictItemName, dictItemDTO.getDictItemName())) {
				return dictItemDTO.getDictItemCode();
			}
		}
		return null;
	}


	@Override
	@AuthData(module = {MenuEnum.BUYER_PURCHASE_ORDER , MenuEnum.SUPPLIER_SIGN} )
	public List<OrderDetailDTO> listOrderDetailPage(OrderRequestDTO orderRequestDTO) {
		return orderMapper.findOrderDetailList(orderRequestDTO);
	}


	@Override
	@Transactional
	public void batchAccept(List<Long> ids) {
		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
		List<OrderDetail> list = new ArrayList<OrderDetail>();
		ids.forEach(item -> {
			OrderDetail od = iOrderDetailService.getById(item);
			Assert.notNull(od, "??????????????????");
			Order checkOrder = orderMapper.selectById(od.getOrderId());
			Assert.notNull(checkOrder, "???????????????");
			if(!loginAppUser.getCompanyId().equals(checkOrder.getVendorId())) {
				Assert.notNull(od, "????????????????????????????????????");
			}
			if (null != od.getOrderDetailStatus()) {
				throw new BaseException("????????????????????????????????????");
			}
			od.setOrderDetailStatus("ACCEPT");
			od.setConfirmTime(new Date());
			list.add(od);
		});
		this.iOrderDetailService.updateBatchById(list);
	}

	@Override
	@Transactional
	public void batchRefuse(List<Long> ids,String refusedReason) {
		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
		List<OrderDetail> list = new ArrayList<OrderDetail>();
		ids.forEach(item -> {
			OrderDetail od = iOrderDetailService.getById(item);
			Assert.notNull(od, "??????????????????");
			Order checkOrder = orderMapper.selectById(od.getOrderId());
			Assert.notNull(checkOrder, "???????????????");
			if(!loginAppUser.getCompanyId().equals(checkOrder.getVendorId())) {
				Assert.notNull(od, "????????????????????????????????????");
			}
			if (null != od.getOrderDetailStatus()) {
				throw new BaseException("????????????????????????????????????");
			}
			od.setOrderDetailStatus("REFUSED");
			od.setConfirmTime(new Date());
			od.setRefusedReason(refusedReason);
			list.add(od);
		});
		this.iOrderDetailService.updateBatchById(list);
	}

	/**
	 * ????????????
	 * @param orderId
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void approval(Long orderId) {
		//??????
		Order order = this.getById(orderId);
		if(Objects.isNull(order)){
			throw new BaseException(LocaleHandler.getLocaleMsg(String.format("?????????????????????[orderId=%s]",order.getOrderId())));
		}
		if(!PurchaseOrderEnum.UNDER_APPROVAL.getValue().equals(order.getOrderStatus())){
			throw new BaseException(LocaleHandler.getLocaleMsg(String.format("??????[orderId=%s][%s]????????????",order.getOrderId(),order.getOrderStatus())));
		}
		//????????????
		order.setOrderStatus(PurchaseOrderEnum.APPROVED.getValue());
		this.updateById(order);

		//??????????????????
		List<OrderDetail> orderDetailList = iOrderDetailService.list(new QueryWrapper<OrderDetail>(new OrderDetail().setOrderId(orderId)));
		for(OrderDetail orderDetail : orderDetailList){
			//???????????????????????????????????????
			if("Y".equals(order.getCeeaIfSupplierConfirm())){
				orderDetail.setOrderDetailStatus(OrderDetailStatus.WAITING_VENDOR_CONFIRM.getValue());
			}else{
				orderDetail.setOrderDetailStatus(OrderDetailStatus.ACCEPT.getValue());
			}
			orderDetail.setCeeaApprovedNum(orderDetail.getOrderNum());
			orderDetail.setCeeaFirstApprovedNum(orderDetail.getOrderNum());
		}
		iOrderDetailService.updateBatchById(orderDetailList);

		//?????????????????????
		if(!"Y".equals(order.getCeeaIfSupplierConfirm())){
			supplierClient.modify(new CompanyInfo().setCompanyId(order.getVendorId()).setIfNewCompany("N"));
		}

	}

	/**
	 * ??????????????????
	 * @param ids
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void batchDelete(List<Long> ids) {
		//??????
		List<Order> orderList = this.listByIds(ids);
		if(!CollectionUtils.isEmpty(orderList)){
			for(Order order : orderList){
				if(!PurchaseOrderEnum.DRAFT.getValue().equals(order.getOrderStatus())){
					throw new BaseException(LocaleHandler.getLocaleMsg(String.format("??????[orderNumber=%s]?????????[%s],????????????",order.getOrderNumber(),order.getOrderStatus())));
				}
			}
		}
		//???????????????
		this.removeByIds(ids);
		//??????????????????
		QueryWrapper<OrderDetail> orderDetailWrapper = new QueryWrapper<>();
		orderDetailWrapper.in("ORDER_ID",ids);
		iOrderDetailService.remove(orderDetailWrapper);
		//??????????????????
		QueryWrapper<OrderAttach> orderAttachWrapper = new QueryWrapper<>();
		orderAttachWrapper.in("ORDER_ID",ids);
		iOrderAttachService.remove(orderAttachWrapper);
		//??????????????????
		QueryWrapper<OrderPaymentProvision> orderPaymentProvisionWrapper = new QueryWrapper<>();
		orderPaymentProvisionWrapper.in("ORDER_ID",ids);
		iOrderPaymentProvisionService.remove(orderPaymentProvisionWrapper);
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
		List<Order> orderList = this.listByIds(ids);
		if(CollectionUtils.isEmpty(orderList)){
			throw new BaseException(LocaleHandler.getLocaleMsg(String.format("??????????????????,???????????????")));
		}
		QueryWrapper<OrderDetail> orderDetailWrapper = new QueryWrapper<OrderDetail>();
		orderDetailWrapper.in("ORDER_ID",ids);
		List<OrderDetail> orderDetailList = iOrderDetailService.list(orderDetailWrapper);
		if(CollectionUtils.isEmpty(orderDetailList)){
			throw new BaseException(LocaleHandler.getLocaleMsg(String.format("????????????????????????,???????????????")));
		}
		for(OrderDetail orderDetail : orderDetailList){
			if(!OrderDetailStatus.WAITING_VENDOR_CONFIRM.getValue().equals(orderDetail.getOrderDetailStatus())){
				throw new BaseException(LocaleHandler.getLocaleMsg(String.format("orderDetailId=[%s]????????????????????????????????????,?????????",orderDetail.getOrderDetailId())));
			}
		}

		//??????????????????
		for(OrderDetail orderDetail : orderDetailList){
			orderDetail.setOrderDetailStatus(OrderDetailStatus.ACCEPT.getValue());
		}
		iOrderDetailService.updateBatchById(orderDetailList);

		//???????????????
		for(Order order : orderList){
			order.setIfDetailHandle("Y");
		}
		this.updateBatchById(orderList);
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
		List<Order> orderList = this.listByIds(ids);
		if(CollectionUtils.isEmpty(orderList)){
			throw new BaseException(LocaleHandler.getLocaleMsg(String.format("??????????????????,???????????????")));
		}
		QueryWrapper<OrderDetail> orderDetailWrapper = new QueryWrapper<OrderDetail>();
		orderDetailWrapper.in("ORDER_ID",ids);
		List<OrderDetail> orderDetailList = iOrderDetailService.list(orderDetailWrapper);
		if(CollectionUtils.isEmpty(orderDetailList)){
			throw new BaseException(LocaleHandler.getLocaleMsg(String.format("????????????????????????,???????????????")));
		}
		for(OrderDetail orderDetail : orderDetailList){
			if(!OrderDetailStatus.WAITING_VENDOR_CONFIRM.getValue().equals(orderDetail.getOrderDetailStatus())){
				throw new BaseException(LocaleHandler.getLocaleMsg(String.format("orderDetailId=[%s]????????????????????????????????????,?????????",orderDetail.getOrderDetailId())));
			}
		}

		//??????????????????
		for(OrderDetail orderDetail : orderDetailList){
			orderDetail.setOrderDetailStatus(OrderDetailStatus.REJECT.getValue());
			orderDetail.setRefusedReason(refusedReason);
		}
		iOrderDetailService.updateBatchById(orderDetailList);

		//???????????????
		for(Order order : orderList){
			order.setIfDetailHandle("Y");
		}
		this.updateBatchById(orderList);
	}


}
