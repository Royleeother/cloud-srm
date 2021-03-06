package com.midea.cloud.srm.supcooperate.reconciliation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.neworder.WarehouseReceiptStatus;
import com.midea.cloud.common.enums.neworder.WarehousingReturnDetailStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.service.impl.BaseServiceImpl;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.supcooperate.SupcooperateClient.SaveOrUpdateWarehouseReceiptAndUpdateOrderDetail;
import com.midea.cloud.srm.model.pm.po.dto.WarehouseReceiptPageQueryDTO;
import com.midea.cloud.srm.model.pm.ps.statement.dto.StatementReceiptDTO;
import com.midea.cloud.srm.model.suppliercooperate.deliverynote.entry.DeliveryNote;
import com.midea.cloud.srm.model.suppliercooperate.deliverynote.entry.DeliveryNoteDetail;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.WarehouseReceiptDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.WarehousingReturnDetail;
import com.midea.cloud.srm.model.suppliercooperate.order.vo.WarehouseReceiptDetailVO;
import com.midea.cloud.srm.model.suppliercooperate.order.vo.WarehouseReceiptVO;
import com.midea.cloud.srm.model.suppliercooperate.reconciliation.entry.WarehouseReceipt;
import com.midea.cloud.srm.model.suppliercooperate.reconciliation.entry.WarehouseReceiptDetail;
import com.midea.cloud.srm.supcooperate.order.service.*;
import com.midea.cloud.srm.supcooperate.reconciliation.mapper.WarehouseReceiptMapper;
import com.midea.cloud.srm.supcooperate.reconciliation.service.IWarehouseReceiptService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ???????????? ???????????????
 * </pre>
 *
 * @author huangbf3
 * @version 1.00.00
 *
 *          <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/4/2 11:33
 *  ????????????:
 *          </pre>
 */
@Service
public class WarehouseReceiptServiceImpl extends BaseServiceImpl<WarehouseReceiptMapper, WarehouseReceipt> implements IWarehouseReceiptService {

	@Autowired
	private IOrderDetailService iOrderDetailService;

	@Autowired
	private IWarehouseReceiptDetailService warehouseReceiptDetailService;

	@Autowired
	private BaseClient baseClient;

	@Autowired
	private IDeliveryNoteDetailService deliveryNoteDetailService;

	@Autowired
	private IDeliveryNoteService deliveryNoteService;

	@Autowired
	private IWarehousingReturnDetailService warehousingReturnDetailService;

	@Override
	public PageInfo<WarehouseReceiptVO> listPage(WarehouseReceiptPageQueryDTO warehouseReceiptPageQueryDTO) {
		PageUtil.startPage(warehouseReceiptPageQueryDTO.getPageNum(), warehouseReceiptPageQueryDTO.getPageSize());
		List<WarehouseReceiptVO> list = getBaseMapper().listPage(warehouseReceiptPageQueryDTO);
		return new PageInfo<WarehouseReceiptVO>(list);
	}

	@Override
	public void saveOrUpdateWarehouseReceiptAndUpdateOrderDetail(SaveOrUpdateWarehouseReceiptAndUpdateOrderDetail param) {
		this.saveOrUpdateBatch(param.getWarehouseReceiptList());
		iOrderDetailService.saveOrUpdateBatch(param.getOrderDetailList());
	}

	@Override
	public PageInfo<StatementReceiptDTO> listStatementReceiptDTOPage(WarehouseReceiptPageQueryDTO warehouseReceiptPageQueryDTO) {
		PageUtil.startPage(warehouseReceiptPageQueryDTO.getPageNum(), warehouseReceiptPageQueryDTO.getPageSize());
		return new PageInfo<StatementReceiptDTO>(getBaseMapper().listStatementReceiptDTOPage(warehouseReceiptPageQueryDTO));
	}

	/**
	 * ??????
	 * @param warehouseReceiptDTO
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long temporarySave(WarehouseReceiptDTO warehouseReceiptDTO) {
		return this.temporarySaveOrUpdate(warehouseReceiptDTO, WarehouseReceiptStatus.DRAFT);
	}

	/**
	 * ??????
	 * @param warehouseReceiptDTO
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long submit(WarehouseReceiptDTO warehouseReceiptDTO) {
		return this.temporarySaveOrUpdate(warehouseReceiptDTO, WarehouseReceiptStatus.WAITING_CONFIRM);
	}

	private Long temporarySaveOrUpdate(WarehouseReceiptDTO warehouseReceiptDTO, WarehouseReceiptStatus warehouseReceiptStatus) {
		//??????
		checkIfAdd(warehouseReceiptDTO);
		WarehouseReceipt warehouseReceipt = warehouseReceiptDTO.getWarehouseReceipt();
		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDTO.getWarehouseReceiptDetailList();

		Set<Long> deliveryNoteDetailIdList = warehouseReceiptDetailList.stream().map(WarehouseReceiptDetail::getDeliveryNoteDetailId).collect(Collectors.toSet());
		List<DeliveryNoteDetail> deliveryNoteDetailList = deliveryNoteDetailService.listByIds(deliveryNoteDetailIdList);

		//??????/???????????????
		warehouseReceipt.setWarehouseReceiptStatus(warehouseReceiptStatus.getValue());
		Long id = null;
		if(Objects.isNull(warehouseReceipt.getWarehouseReceiptId())){
			//??????
			id = add(warehouseReceiptDTO);
		}else{
			//??????
			id = update(warehouseReceiptDTO);
		}

		//?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????,??????????????????????????????????????????
		this.updateDeliveryNoteDetail(deliveryNoteDetailList, deliveryNoteDetailIdList);

		return id;

	}

	/**
	 * ?????????????????????????????????
	 * @param deliveryNoteDetailIdList
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateDeliveryNoteDetail(List<DeliveryNoteDetail> deliveryNoteDetailList, Set<Long> deliveryNoteDetailIdList){
		Map<Long, BigDecimal> detailQuantityMap = this.getWarehouseQuantity(deliveryNoteDetailIdList);
		for(DeliveryNoteDetail deliveryNoteDetail : deliveryNoteDetailList){
			BigDecimal totalQuantity = detailQuantityMap.getOrDefault(deliveryNoteDetail.getDeliveryNoteDetailId(), BigDecimal.ZERO);

			// ????????????????????????
			if (deliveryNoteDetail.getDeliveryQuantity().compareTo(totalQuantity) < 0) {
				throw new BaseException(LocaleHandler.getLocaleMsg(String.format("????????????[id=%s]????????????,?????????", deliveryNoteDetail.getDeliveryNoteDetailId())));
			}
		}

		for(DeliveryNoteDetail deliveryNoteDetail : deliveryNoteDetailList){
			BigDecimal totalQuantity = detailQuantityMap.getOrDefault(deliveryNoteDetail.getDeliveryNoteDetailId(), BigDecimal.ZERO);
			deliveryNoteDetail.setWarehouseQuantity(totalQuantity);

			//????????????
			LambdaQueryWrapper<DeliveryNoteDetail> deliveryNoteDetailWrapper = Wrappers.lambdaQuery(DeliveryNoteDetail.class)
					.eq(DeliveryNoteDetail::getDeliveryNoteDetailId, deliveryNoteDetail.getDeliveryNoteDetailId())
					.eq(DeliveryNoteDetail::getVersion, deliveryNoteDetail.getVersion())
					.ge(DeliveryNoteDetail::getDeliveryQuantity, totalQuantity); // ??????????????????????????????

			if(Objects.isNull(deliveryNoteDetail.getVersion())){
				deliveryNoteDetail.setVersion(0L);
			}
			deliveryNoteDetail.setVersion(deliveryNoteDetail.getVersion() + 1);
			boolean ifUpdate = deliveryNoteDetailService.update(deliveryNoteDetail, deliveryNoteDetailWrapper);
			if(!ifUpdate){
				throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,???????????????"));
			}

		}
	}



	private Map<Long, BigDecimal> getWarehouseQuantity(Set<Long> deliveryNoteDetailIdList) {
		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDetailService.listByDeliveryNoteDetailId(deliveryNoteDetailIdList);

		if (CollectionUtils.isEmpty(warehouseReceiptDetailList)) {
			return Collections.emptyMap();
		}

		// ??????????????????????????????
		List<Long> warehouseReceiptIdList = warehouseReceiptDetailList.stream().map(WarehouseReceiptDetail::getWarehouseReceiptId).distinct().collect(Collectors.toList());
		List<WarehouseReceipt> receiptList = this.listUnWriteOff(warehouseReceiptIdList);
		List<Long> activeReceiptIdList = receiptList.stream().map(WarehouseReceipt::getWarehouseReceiptId).collect(Collectors.toList());

		// ???????????????????????????
		Map<Long, List<WarehouseReceiptDetail>> detailMap = warehouseReceiptDetailList.stream()
				.filter(item -> item.getWarehouseQuantity() != null && activeReceiptIdList.contains(item.getWarehouseReceiptId()))
				.collect(Collectors.groupingBy(WarehouseReceiptDetail::getDeliveryNoteDetailId));
		Map<Long, BigDecimal> detailQuantityMap = new HashMap<>();
		detailMap.forEach((deliveryNoteDetailId, list) -> {
			BigDecimal totalQuantity = list.stream().map(WarehouseReceiptDetail::getWarehouseQuantity).reduce(BigDecimal::add).get();

			detailQuantityMap.put(deliveryNoteDetailId, totalQuantity);
		});

		return detailQuantityMap;
	}

	/**
	 * ?????????????????????
	 * @param ids
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void batchDelete(List<Long> ids) {
		//??????
		checkIfDelete(ids);

		// ?????????????????????????????????
		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDetailService.listByWarehouseReceiptId(ids);

		//??????????????????
		this.removeByIds(ids);
		//??????????????????
		warehouseReceiptDetailService.deleteByWarehouseReceiptId(ids);

		// ???????????????????????????
		Set<Long> deliveryNoteDetailIdList = warehouseReceiptDetailList.stream().map(WarehouseReceiptDetail::getDeliveryNoteDetailId).collect(Collectors.toSet());
		List<DeliveryNoteDetail> deliveryNoteDetailList = deliveryNoteDetailService.listByIds(deliveryNoteDetailIdList);
		this.updateDeliveryNoteDetail(deliveryNoteDetailList, deliveryNoteDetailIdList);

	}

	/**
	 * ?????????????????????
	 * @param id
	 * @return
	 */
	@Override
	public WarehouseReceiptDTO detailWarehouseReceipt(Long id) {
		if(Objects.isNull(id)){
			throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
		}

		WarehouseReceipt warehouseReceipt = this.getById(id);

		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDetailService.list(id);
		return new WarehouseReceiptDTO().setWarehouseReceipt(warehouseReceipt).setWarehouseReceiptDetailList(warehouseReceiptDetailList);
	}

	private void checkIfDelete(List<Long> ids){
		if(CollectionUtils.isEmpty(ids)){
			throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
		}
		List<WarehouseReceipt> warehouseReceiptList = this.listByIds(ids);
		if(CollectionUtils.isNotEmpty(warehouseReceiptList)){
			for(WarehouseReceipt warehouseReceipt : warehouseReceiptList){
				if(!WarehouseReceiptStatus.DRAFT.getValue().equals(warehouseReceipt.getWarehouseReceiptStatus()) && !WarehouseReceiptStatus.WAITING_CONFIRM.getValue().equals(warehouseReceipt.getWarehouseReceiptStatus())){
					throw new BaseException(LocaleHandler.getLocaleMsg(String.format("?????????[??????=%s]??????%s??????,?????????", warehouseReceipt.getWarehouseReceiptNumber(), warehouseReceipt.getWarehouseReceiptStatus())));
				}
			}
		}
	}

	private void checkIfAdd(WarehouseReceiptDTO warehouseReceiptDTO){
		WarehouseReceipt warehouseReceipt = warehouseReceiptDTO.getWarehouseReceipt();
		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDTO.getWarehouseReceiptDetailList();
		if(StringUtils.isBlank(warehouseReceipt.getOrgCode())){
			throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
		}
		if(StringUtils.isBlank(warehouseReceipt.getVendorCode())){
			throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
		}
		if(CollectionUtils.isEmpty(warehouseReceiptDetailList)){
			throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
		}
		for(WarehouseReceiptDetail warehouseReceiptDetail : warehouseReceiptDetailList){
			if(Objects.nonNull(warehouseReceiptDetail.getWarehouseQuantity()) && warehouseReceiptDetail.getWarehouseQuantity().compareTo(BigDecimal.ZERO) <= 0){
				throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????"));
			}
		}

	}

	/**
	 * ???????????????
	 * @param warehouseReceiptDTO
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long add(WarehouseReceiptDTO warehouseReceiptDTO){
		WarehouseReceipt warehouseReceipt = warehouseReceiptDTO.getWarehouseReceipt();
		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDTO.getWarehouseReceiptDetailList();
		//?????????????????????
		Long id = IdGenrator.generate();
		warehouseReceipt.setWarehouseReceiptId(id);
		warehouseReceipt.setWarehouseReceiptNumber(baseClient.seqGen(SequenceCodeConstant.SEQ_WAREHOUSE_RECEIPT_NUM));
		this.save(warehouseReceipt);
		//?????????????????????
		long lineNum = 1;
		for(WarehouseReceiptDetail warehouseReceiptDetail : warehouseReceiptDetailList){
			warehouseReceiptDetail.setWarehouseReceiptDetailId(IdGenrator.generate());
			warehouseReceiptDetail.setWarehouseReceiptId(id);
			warehouseReceiptDetail.setWarehouseReceiptRowNum(lineNum ++);
		}
		warehouseReceiptDetailService.saveBatch(warehouseReceiptDetailList);
		return id;
	}

	/**
	 * ???????????????
	 * @param warehouseReceiptDTO
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long update(WarehouseReceiptDTO warehouseReceiptDTO){
		WarehouseReceipt warehouseReceipt = warehouseReceiptDTO.getWarehouseReceipt();
		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDTO.getWarehouseReceiptDetailList();
		// ?????????????????????
		Long id = warehouseReceipt.getWarehouseReceiptId();
		this.updateById(warehouseReceipt);
		// ?????????????????????
		warehouseReceiptDetailService.deleteByWarehouseReceiptId(id);

		long lineNum = 1;
		for(WarehouseReceiptDetail warehouseReceiptDetail : warehouseReceiptDetailList){
			warehouseReceiptDetail.setWarehouseReceiptDetailId(IdGenrator.generate());
			warehouseReceiptDetail.setWarehouseReceiptId(id);
			warehouseReceiptDetail.setWarehouseReceiptRowNum(lineNum ++);
		}
		warehouseReceiptDetailService.saveBatch(warehouseReceiptDetailList);
		return id;
	}

	/**
	 * ????????????
	 * @param ids
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void batchConfirm(List<Long> ids) {
		// ??????
		checkIfConfirm(ids);
		// ????????????
		List<WarehouseReceipt> warehouseReceiptList = this.listByIds(ids);
		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDetailService.listByWarehouseReceiptId(ids);

		// ?????????????????????
		this.updateWarehouseReceiptStatus(ids, WarehouseReceiptStatus.CONFIRM);

		//??????????????????????????????
		List<WarehousingReturnDetail> warehousingReturnDetailList = build(warehouseReceiptList,warehouseReceiptDetailList);
		warehousingReturnDetailService.saveBatch(warehousingReturnDetailList);

	}

	/**
	 * ????????????????????????
	 * @param ids
	 */
	private void checkIfConfirm(List<Long> ids){
		if(CollectionUtils.isEmpty(ids)){
			throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
		}
		List<WarehouseReceipt> warehouseReceiptList = this.listByIds(ids);
		if(CollectionUtils.isEmpty(warehouseReceiptList)){
			throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,???????????????"));
		}

		for(WarehouseReceipt warehouseReceipt : warehouseReceiptList){
			if(!WarehouseReceiptStatus.WAITING_CONFIRM.getValue().equals(warehouseReceipt.getWarehouseReceiptStatus())){
				throw new BaseException(LocaleHandler.getLocaleMsg(String.format("?????????[%s]?????????????????????",warehouseReceipt.getWarehouseReceiptNumber())));
			}
		}
	}

	/**
	 * ????????????????????????
	 * @param warehouseReceiptList
	 * @param warehouseReceiptDetailList
	 * @return
	 */
	private List<WarehousingReturnDetail> build(List<WarehouseReceipt> warehouseReceiptList, List<WarehouseReceiptDetail> warehouseReceiptDetailList){
		//????????????
		List<Long> warehouseReceiptDetailIdList = warehouseReceiptDetailList.stream().map(WarehouseReceiptDetail::getWarehouseReceiptDetailId).collect(Collectors.toList());

		List<WarehouseReceiptDetailVO> warehouseReceiptDetailVOList = warehouseReceiptDetailService.list(warehouseReceiptDetailIdList);
		Map<Long, WarehouseReceiptDetailVO> warehouseReceiptDetailVOMap = warehouseReceiptDetailVOList.stream().collect(Collectors.toMap(WarehouseReceiptDetail::getWarehouseReceiptDetailId, item -> item));

		Map<Long, WarehouseReceipt> receiptMap = warehouseReceiptList.stream().collect(Collectors.toMap(WarehouseReceipt::getWarehouseReceiptId, a -> a));

		List<WarehousingReturnDetail> result = new LinkedList<>();

		for(WarehouseReceiptDetail warehouseReceiptDetail : warehouseReceiptDetailList){
			WarehouseReceiptDetailVO warehouseReceiptDetailVO = warehouseReceiptDetailVOMap.get(warehouseReceiptDetail.getWarehouseReceiptDetailId());

			WarehouseReceipt warehouseReceipt = receiptMap.get(warehouseReceiptDetail.getWarehouseReceiptId());

			Date date = new Date();
			Long txnId = IdGenrator.generate();
			WarehousingReturnDetail warehousingReturnDetail = new WarehousingReturnDetail()
					.setWarehousingReturnDetailId(IdGenrator.generate())
					.setWarehouseReceiptDetailId(warehouseReceiptDetail.getWarehouseReceiptDetailId())
					.setType(WarehousingReturnDetailStatus.RECEIVE.getValue())

//					.setReceiveOrderNo(baseClient.seqGen(SequenceCodeConstant.SEQ_WAREHOUSE_RETURN_NUM))
//					.setReceiveOrderLineNo(1)
					.setReceiveOrderNo(warehouseReceipt.getWarehouseReceiptNumber())
					.setReceiveOrderLineNo(warehouseReceiptDetail.getWarehouseReceiptRowNum().intValue())
					.setDeliveryLineNum(warehouseReceiptDetailVO.getDeliveryLineNum())
					.setDeliveryNumber(warehouseReceiptDetailVO.getDeliveryNumber())

					.setOrgId(warehouseReceiptDetailVO.getOrgId())
					.setOrgCode(warehouseReceiptDetailVO.getOrgCode())
					.setOrgName(warehouseReceiptDetailVO.getOrgName())
					.setOrganizationId(warehouseReceiptDetailVO.getOrganizationId())
					.setOrganizationCode(warehouseReceiptDetailVO.getOrganizationCode())
					.setOrganizationName(warehouseReceiptDetailVO.getOrganizationName())
					.setVendorId(warehouseReceiptDetailVO.getVendorId())
					.setVendorCode(warehouseReceiptDetailVO.getVendorCode())
					.setVendorName(warehouseReceiptDetailVO.getVendorName())
					.setCategoryId(warehouseReceiptDetailVO.getCategoryId())
					.setCategoryCode(warehouseReceiptDetailVO.getCategoryCode())
					.setCategoryName(warehouseReceiptDetailVO.getCategoryName())
					.setItemId(warehouseReceiptDetailVO.getMaterialId())
					.setItemCode(warehouseReceiptDetailVO.getMaterialCode())
					.setItemName(warehouseReceiptDetailVO.getMaterialName())
					.setUnit(warehouseReceiptDetailVO.getUnit())
					.setUnitCode(warehouseReceiptDetailVO.getUnitCode())
					.setReceiveNum(warehouseReceiptDetail.getWarehouseQuantity())
//					.setRequirementHeadNum()
//					.setRowNum()
					.setOrderNumber(warehouseReceiptDetailVO.getOrderNumber())
					.setSrmOrderNumber(warehouseReceiptDetailVO.getOrderNumber())
					.setSettlementOrderNumber(warehouseReceiptDetailVO.getOrderNumber())
					.setLineNum(warehouseReceiptDetailVO.getLineNum())
					.setContractNo(warehouseReceiptDetailVO.getContractNo())
					.setContractCode(warehouseReceiptDetailVO.getContractCode())
//					.setContractHeadId()
					.setTaxKey(warehouseReceiptDetailVO.getTaxKey())
					.setTaxRate(warehouseReceiptDetailVO.getTaxRate())
//					.setProjectName()
//					.setProjectNum()
//					.setTaskName()
//					.setTaskNum()
					.setNotInvoiceQuantity(warehouseReceiptDetail.getWarehouseQuantity())
					.setStorageQuantity(warehouseReceiptDetail.getWarehouseQuantity()) // ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
					.setUnitPriceContainingTax(warehouseReceiptDetailVO.getUnitPriceContainingTax())
					.setUnitPriceExcludingTax(warehouseReceiptDetailVO.getUnitPriceExcludingTax())
					.setCurrencyId(warehouseReceiptDetailVO.getCurrencyId())
					.setCurrencyCode(warehouseReceiptDetailVO.getCurrencyCode())
					.setCurrencyName(warehouseReceiptDetailVO.getCurrencyName())
					.setReceiveDate(date)
					.setWarehousingDate(date)
					.setPoLineId(warehouseReceiptDetailVO.getPoLineId())
					.setTxnId(txnId)
					.setParentTxnId(txnId);
			result.add(warehousingReturnDetail);
		}
		return result;
	}

	/**
	 * ????????????
	 * @param ids
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void batchWriteOff(List<Long> ids) {
		//??????
		checkIfWriteOff(ids);
		//????????????
		List<WarehouseReceipt> warehouseReceiptList = this.listByIds(ids);
		List<WarehouseReceiptDetail> warehouseReceiptDetailList = warehouseReceiptDetailService.listByWarehouseReceiptId(ids);

		List<Long> warehouseReceiptDetailIdList = warehouseReceiptDetailList.stream().map(WarehouseReceiptDetail::getWarehouseReceiptDetailId).collect(Collectors.toList());

		// ???????????????????????????????????????????????????
		QueryWrapper<WarehousingReturnDetail> warehousingReturnDetailWrapper = new QueryWrapper<>();
		warehousingReturnDetailWrapper.in("WAREHOUSE_RECEIPT_DETAIL_ID", warehouseReceiptDetailIdList);
		warehousingReturnDetailWrapper.apply(" NOT_INVOICE_QUANTITY != STORAGE_QUANTITY");
		long existAmount = warehousingReturnDetailService.count(warehousingReturnDetailWrapper);

		if (existAmount > 0) {
			throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????"));
		}

		//???????????????
		for(WarehouseReceipt warehouseReceipt : warehouseReceiptList){
			warehouseReceipt.setWarehouseReceiptStatus(WarehouseReceiptStatus.WRITEOFF.getValue());
		}
		this.updateBatchById(warehouseReceiptList);

		//??????????????????????????????
		QueryWrapper<WarehousingReturnDetail> delete = new QueryWrapper<>();
		delete.in("WAREHOUSE_RECEIPT_DETAIL_ID", warehouseReceiptDetailIdList);
		warehousingReturnDetailService.remove(delete);

		// ???????????????????????????
		Set<Long> deliveryNoteDetailIdList = warehouseReceiptDetailList.stream().map(WarehouseReceiptDetail::getDeliveryNoteDetailId).collect(Collectors.toSet());
		List<DeliveryNoteDetail> deliveryNoteDetailList = deliveryNoteDetailService.listByIds(deliveryNoteDetailIdList);
		this.updateDeliveryNoteDetail(deliveryNoteDetailList, deliveryNoteDetailIdList);
	}

	@Override
	public int updateWarehouseReceiptStatus(List<Long> ids, WarehouseReceiptStatus warehouseReceiptStatus) {
		return super.updateByIds(ids, new WarehouseReceipt().setWarehouseReceiptStatus(warehouseReceiptStatus.getValue()));
	}

	@Override
	public List<WarehouseReceipt> listUnWriteOff(List<Long> ids) {
		return this.list(Wrappers.lambdaQuery(WarehouseReceipt.class)
				.in(WarehouseReceipt::getWarehouseReceiptId, ids)
				.ne(WarehouseReceipt::getWarehouseReceiptStatus, WarehouseReceiptStatus.WRITEOFF.getValue())
		);
	}

	/**
	 * ????????????????????????
	 * @param ids
	 */
	private void checkIfWriteOff(List<Long> ids){
		if(CollectionUtils.isEmpty(ids)){
			throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
		}
		List<WarehouseReceipt> warehouseReceiptList = this.listByIds(ids);
		if(CollectionUtils.isEmpty(warehouseReceiptList)){
			throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,???????????????"));
		}

		for(WarehouseReceipt warehouseReceipt : warehouseReceiptList){
			if(!WarehouseReceiptStatus.CONFIRM.getValue().equals(warehouseReceipt.getWarehouseReceiptStatus())){
				throw new BaseException(LocaleHandler.getLocaleMsg(String.format("?????????[%s]??????????????????",warehouseReceipt.getWarehouseReceiptNumber())));
			}
		}
	}

	@Override
	public SFunction<WarehouseReceipt, Long> getKeyWrapper() {
		return WarehouseReceipt::getWarehouseReceiptId;
	}
}
