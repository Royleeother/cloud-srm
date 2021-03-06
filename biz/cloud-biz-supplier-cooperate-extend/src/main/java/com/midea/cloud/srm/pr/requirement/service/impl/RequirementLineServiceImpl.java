package com.midea.cloud.srm.pr.requirement.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.constants.SpecialBusinessConstant;
import com.midea.cloud.common.enums.MainType;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingApprovalStatus;
import com.midea.cloud.common.enums.order.JITOrder;
import com.midea.cloud.common.enums.order.ResponseStatus;
import com.midea.cloud.common.enums.pm.po.OrderTypeEnum;
import com.midea.cloud.common.enums.pm.po.PriceSourceTypeEnum;
import com.midea.cloud.common.enums.pm.po.PurchaseOrderEnum;
import com.midea.cloud.common.enums.pm.po.SourceSystemEnum;
import com.midea.cloud.common.enums.pm.pr.requirement.*;
import com.midea.cloud.common.enums.pm.ps.BudgetUseStatus;
import com.midea.cloud.common.enums.pm.ps.CategoryEnum;
import com.midea.cloud.common.enums.pm.ps.FSSCResponseCode;
import com.midea.cloud.common.enums.rbac.RoleEnum;
import com.midea.cloud.common.enums.review.CategoryStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.component.filter.HttpServletHolder;
import com.midea.cloud.srm.feign.bargaining.BargainClient;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.bid.BidClient;
import com.midea.cloud.srm.feign.contract.ContractClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.inq.InqClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.material.MaterialOrg;
import com.midea.cloud.srm.model.base.material.dto.ItemCodeUserPurchaseDto;
import com.midea.cloud.srm.model.base.material.vo.MaterialMaxCategoryVO;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseCategory;
import com.midea.cloud.srm.model.base.purchase.entity.PurchaseTax;
import com.midea.cloud.srm.model.base.quotaorder.QuotaHead;
import com.midea.cloud.srm.model.base.quotaorder.QuotaLine;
import com.midea.cloud.srm.model.base.quotaorder.dto.QuotaLineDto;
import com.midea.cloud.srm.model.base.quotaorder.dto.QuotaParamDto;
import com.midea.cloud.srm.model.base.soap.DataSourceEnum;
import com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.cm.contract.dto.ContractItemDto;
import com.midea.cloud.srm.model.cm.contract.entity.ContractHead;
import com.midea.cloud.srm.model.cm.contract.entity.ContractPartner;
import com.midea.cloud.srm.model.cm.contract.vo.ContractVo;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.inq.price.entity.ApprovalBiddingItem;
import com.midea.cloud.srm.model.inq.price.entity.ApprovalHeader;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibrary;
import com.midea.cloud.srm.model.pm.po.dto.NetPriceQueryDTO;
import com.midea.cloud.srm.model.pm.pr.documents.entity.SubsequentDocuments;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.*;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementHead;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementLine;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.VendorDistDesc;
import com.midea.cloud.srm.model.pm.pr.requirement.param.FollowNameParam;
import com.midea.cloud.srm.model.pm.pr.requirement.param.OrderQuantityParam;
import com.midea.cloud.srm.model.pm.pr.requirement.param.SourceBusinessGenParam;
import com.midea.cloud.srm.model.pm.pr.requirement.vo.RecommendVendorVO;
import com.midea.cloud.srm.model.pm.pr.requirement.vo.RequirementLineVO;
import com.midea.cloud.srm.model.pm.pr.requirement.vo.VendorAndEffectivePriceVO;
import com.midea.cloud.srm.model.pm.ps.http.BgtCheckReqParamDto;
import com.midea.cloud.srm.model.pm.ps.http.FSSCResult;
import com.midea.cloud.srm.model.pm.ps.http.Line;
import com.midea.cloud.srm.model.rbac.role.entity.Role;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.info.entity.FinanceInfo;
import com.midea.cloud.srm.model.supplier.info.entity.OrgCategory;
import com.midea.cloud.srm.model.supplier.info.entity.OrgInfo;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.OrderSaveRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.Order;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderDetail;
import com.midea.cloud.srm.pr.documents.service.ISubsequentDocumentsService;
import com.midea.cloud.srm.pr.requirement.mapper.RequirementHeadMapper;
import com.midea.cloud.srm.pr.requirement.mapper.RequirementLineMapper;
import com.midea.cloud.srm.pr.requirement.service.IRequirementHeadService;
import com.midea.cloud.srm.pr.requirement.service.IRequirementLineService;
import com.midea.cloud.srm.pr.vendordistdesc.service.IVendorDistDescService;
import com.midea.cloud.srm.ps.http.fssc.service.IFSSCReqService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ?????????????????? ???????????????
 * </pre>
 *
 * @author fengdc3@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-05-12 18:46:40
 *  ????????????:
 *          </pre>
 */
@Slf4j
@Service
public class RequirementLineServiceImpl extends ServiceImpl<RequirementLineMapper, RequirementLine> implements IRequirementLineService {

    @Autowired
    private InqClient inqClient;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private BidClient bidClient;
    @Autowired
    private SupplierClient supplierClient;
    @Autowired
    private IRequirementHeadService iRequirementHeadService;
    @Resource
    private IOrderService scIOrderService;
    @Autowired
    private IRequirementLineService iRequirementLineService;
    @Autowired
    private IFSSCReqService iFSSCReqService;

    @Resource
    private RequirementLineMapper requirementLineMapper;

    @Resource
    private FileCenterClient fileCenterClient;
    @Autowired
    private ContractClient contractClient;
    @Autowired
    private ISubsequentDocumentsService subsequentDocumentsService;
    @Autowired
    private BargainClient bargainClient;
    @Resource
    private IVendorDistDescService iVendorDistDescService;

    /**
     * ???????????????Dao
     */
    @Resource
    private RequirementHeadMapper requirementHeadMapper;


    @Override
    @Transactional
    public void addRequirementLineBatch(LoginAppUser loginAppUser, RequirementHead requirementHead, List<RequirementLine> requirementLineList) {
//		Assert.isTrue(CollectionUtils.isNotEmpty(requirementLineList), "???????????????????????????");

        List<Long> categoryIds = requirementLineList.stream().map(r -> r.getCategoryId()).collect(Collectors.toList());
        Map<Long, PurchaseCategory> categoryMap = baseClient.listCategoryByIds(categoryIds).
                stream().collect(Collectors.toMap(
                r -> r.getCategoryId(), r -> r, (o, o2) -> o
        ));
        if (CollectionUtils.isNotEmpty(requirementLineList)) {
            List<String> uomCodeList = new ArrayList<>();
            for (int i = 0; i < requirementLineList.size(); i++) {
                RequirementLine line = requirementLineList.get(i);
                PurchaseCategory category = categoryMap.get(line.getCategoryId());
                String categoryCode = Objects.nonNull(category) ? category.getCategoryCode() : null;
//			checkAddBatchParam(line);
                uomCodeList.add(line.getUnitCode());
                Long id = IdGenrator.generate();
                line.setRequirementLineId(id)
                        .setApplyStatus(RequirementApplyStatus.UNASSIGNED.getValue())
                        .setRequirementHeadId(requirementHead.getRequirementHeadId())
                        .setRowNum(i + 1)
                        .setRequirementHeadNum(requirementHead.getRequirementHeadNum())
                        .setOrderQuantity(line.getRequirementQuantity())
                        .setCategoryCode(categoryCode)
                        .setRequirementSource(RequirementSourceSystem.SRM.value)
                        .setOrderQuantity(line.getRequirementQuantity());//??????????????????????????????????????????
            }

            // ????????????
//			List<PurchaseUnit> purchaseUnitList = baseClient.listPurchaseUnitByCodeList(uomCodeList);
//			Map<String, String> purchaseUnitMap = new HashMap<>();
//			if(CollectionUtils.isNotEmpty(purchaseUnitList)){
//				for(PurchaseUnit purchaseUnit :purchaseUnitList){
//					if (StringUtil.notEmpty(purchaseUnit.getUnitCode()) && StringUtil.notEmpty(purchaseUnit.getUnitName())) {
//						purchaseUnitMap.put(purchaseUnit.getUnitCode(),purchaseUnit.getUnitName());
//						break;
//					}
//				}
//			}
//			log.debug("?????????????????????" + JSON.toJSONString(purchaseUnitMap));
//			requirementLineList.forEach(line -> {
//				line.setUnit(purchaseUnitMap.get(line.getUnitCode()));
//			});

            this.saveBatch(requirementLineList);
        }
    }

    @Override
    @Transactional
    public void updateBatch(RequirementHead requirementHead, List<RequirementLine> requirementLineList) {
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        RequirementLine requirementLine = new RequirementLine();
        requirementLine.setRequirementHeadId(requirementHead.getRequirementHeadId());
        QueryWrapper<RequirementLine> queryWrapper = new QueryWrapper<>(requirementLine);
        List<RequirementLine> oldLineList = this.list(queryWrapper);
        List<Long> oldLineIdList = oldLineList.stream().map(RequirementLine::getRequirementLineId).collect(Collectors.toList());
        List<Long> newLineIdList = new ArrayList<>();
        for (int i = 0; i < requirementLineList.size(); i++) {
            RequirementLine line = requirementLineList.get(i);
            Long requirementLineId = line.getRequirementLineId();
            line.setRowNum(i + 1);
            //???????????????????????????????????????????????????
            line.setOrderQuantity(line.getRequirementQuantity());

            // ??????
            if (requirementLineId == null) {
                line.setRequirementLineId(IdGenrator.generate()).setApplyStatus(RequirementApplyStatus.UNASSIGNED.getValue()).setRequirementHeadId(requirementHead.getRequirementHeadId()).setRequirementHeadNum(requirementHead.getRequirementHeadNum()).setOrderQuantity(line.getRequirementQuantity()).setRequirementSource(RequirementSourceSystem.SRM.value).setCreatedFullName(loginAppUser.getNickname());
                if (line.getNotaxPrice() != null) {
                    if (line.getRequirementQuantity() != null) {
                        line.setTotalAmount(line.getNotaxPrice().multiply(line.getRequirementQuantity()));
                    }
                    line.setHaveEffectivePrice(RequirementEffectivePrice.HAVE.value);
                    line.setHaveSupplier(RequirementSourcingSupplier.HAVE.value);
                } else {
                    line.setHaveEffectivePrice(RequirementEffectivePrice.NOT.value);
                    line.setHaveSupplier(RequirementSourcingSupplier.NOT.value);
                }
                this.save(line);
            } else {
                newLineIdList.add(requirementLineId);
                // ??????
                this.updateById(line);
            }
        }
        // ??????
        for (Long oldId : oldLineIdList) {
            if (!newLineIdList.contains(oldId)) {
                this.removeById(oldId);
            }
        }
    }

    @Override
    @Transactional
    public BaseResult<String> bachUpdateRequirementLine(List<RequirementLine> requirementLineList) {
        if (CollectionUtils.isEmpty(requirementLineList)) {
            Assert.isNull(ResultCode.MISSING_SERVLET_REQUEST_PART.getMessage());
        }
        BaseResult<String> result = BaseResult.build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());

        // ??????????????????
        try {
            getBaseMapper().bachUpdateRequirementLine(requirementLineList);
        } catch (Exception e) {
            log.error("??????ID?????????????????????????????????????????????", e);
            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
        }

//		/**
//		 * ?????????????????????????????????????????? 1. ????????????????????????????????????, ????????????????????? 2. ?????????????????????, ??????????????????????????? 3. ???????????????,
//		 * ???????????????????????? ????????????: RequirementHeadHandleStatus.PARTIAL
//		 */
//		if (CollectionUtils.isNotEmpty(requirementLineList)) {
//			// ?????????????????????id
//			ArrayList<Long> requirementHeadIdList = new ArrayList<>();
//			requirementLineList.forEach(requirementLine -> {
//				RequirementLine requirementLine1 = this.getById(requirementLine.getRequirementLineId());
//				requirementHeadIdList.add(requirementLine1.getRequirementHeadId());
//			});
//			// ??????
//			List<Long> requirementHeadIds = requirementHeadIdList.stream().distinct().collect(Collectors.toList());
//
//			if (CollectionUtils.isNotEmpty(requirementHeadIds)) {
//				requirementHeadIds.forEach(requirementHeadId -> {
//					// ???????????????
//					HashMap<String, Object> param = new HashMap<>();
//					param.put("requirementHeadId", requirementHeadId);
//					Long sunNum = this.baseMapper.queryLineNum(param);
//					// ?????????????????????
//					param.put("applyStatus", RequirementApplyStatus.UNASSIGNED.getValue());
//					Long num = this.baseMapper.queryLineNum(param);
//
//					if (num == sunNum) {
//						// ??????
//						iRequirementHeadService.updateById(new RequirementHead().setRequirementHeadId(requirementHeadId).setHandleStatus(RequirementHeadHandleStatus.EDIT.getValue()));
//					} else if (num == 0) {
//						// ?????????
//						iRequirementHeadService.updateById(new RequirementHead().setRequirementHeadId(requirementHeadId).setHandleStatus(RequirementHeadHandleStatus.ALL.getValue()));
//					} else {
//						// ??????
//						iRequirementHeadService.updateById(new RequirementHead().setRequirementHeadId(requirementHeadId).setHandleStatus(RequirementHeadHandleStatus.PARTIAL.getValue()));
//					}
//				});
//			}
//		}
        return result;
    }

    @Override
    @Transactional
    public BaseResult<String> bachRejectRequirement(Long[] requirementLineIds, String rejectReason) {
        Assert.notNull(requirementLineIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        Assert.notNull(rejectReason, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());

        /** ?????????????????????????????? */
        Long userId = null;
        String userName = "";
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        if (null != user) {
            userId = user.getUserId();
            userName = user.getUsername();
        }
        String loginIp = IPUtil.getRemoteIpAddr(HttpServletHolder.getRequest());

        /** ????????????????????????????????? */
        List<Long> requirementLineIdList = Arrays.asList(requirementLineIds);
        List<RequirementLine> requirementLineList = getBaseMapper().selectBatchIds(requirementLineIdList);
        boolean flag = true;
        // ????????????????????????????????????????????????
        if (CollectionUtils.isNotEmpty(requirementLineList)) {
            for (RequirementLine requirementLine : requirementLineList) {
                String applyStatus = requirementLine.getApplyStatus();
                if (!RequirementApplyStatus.UNASSIGNED.getValue().equals(applyStatus)) {
                    flag = false;
                    break;
                }
            }
        }

        if (flag) {
            List<RequirementLine> updateLineList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(requirementLineList)) {
                for (RequirementLine line : requirementLineList) {
                    if (null != line) {
                        RequirementLine requirementLine = new RequirementLine();
                        requirementLine.setRequirementLineId(line.getRequirementLineId());
                        requirementLine.setApplyStatus(RequirementApplyStatus.REJECTED.getValue());
                        requirementLine.setRejectReason(rejectReason);
                        requirementLine.setLastUpdatedId(userId);
                        requirementLine.setLastUpdatedBy(userName);
                        requirementLine.setLastUpdatedByIp(loginIp);
                        requirementLine.setLastUpdateDate(new Date());
                        updateLineList.add(requirementLine);
                    }
                }
            }
            Map<Long, List<RequirementLine>> map = requirementLineList.stream().collect(Collectors.groupingBy(RequirementLine::getRequirementHeadId));
            Map<Long, RequirementHead> headMap = iRequirementHeadService.listByIds(map.keySet()).stream().collect(Collectors.toMap(RequirementHead::getRequirementHeadId, Function.identity()));
            List<RequirementLine> updateList=new LinkedList<>();
            map.forEach((k, v) -> {
                BgtCheckReqParamDto bgtCheckReqParamDto = new BgtCheckReqParamDto();
                List<Line> lineList = new ArrayList<>();
                FSSCResult fsscResult = new FSSCResult();
                RequirementHead requirementHead = headMap.get(k);
                for (RequirementLine requirementLine : v) {
                    if (requirementLine == null) continue;
                    //???????????????
                    checkBeforeReturn(requirementLine);
                    //?????????????????????
                    BigDecimal ceeaExecutedQuantity = requirementLine.getCeeaExecutedQuantity();
                    //??????????????????????????????
                    BigDecimal requirementQuantity = requirementLine.getRequirementQuantity();
                    updateList.add(requirementLine);
                    //???????????????????????????,??????????????????????????????
                    if (CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode().equals(requirementHead.getCategoryCode())) {
                        //??????????????????   (????????????-???????????????)*????????????
                        String documentLineAmount = (requirementQuantity.subtract(ceeaExecutedQuantity))
                                .multiply(requirementLine.getNotaxPrice() == null ? BigDecimal.ZERO : requirementLine.getNotaxPrice()).toString();
                        //???????????????
                        convertRequirementLine(user, requirementHead, lineList, requirementLine, documentLineAmount);
                    }
                }
                if (CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode().equals(requirementHead.getCategoryCode())) {
                    fsscResult = applyRelease(user, requirementHead, bgtCheckReqParamDto, lineList);
                }
                if (FSSCResponseCode.ERROR.getCode().equals(fsscResult.getCode())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg(fsscResult.getMsg()));
                }
            });

            return this.bachUpdateRequirementLine(updateLineList);
        } else {
            return BaseResult.build("R001", "??????????????????????????????");
        }
    }

//	@Override
//	@Transactional
//	public BaseResult<String> bachAssigned(Long[] requirementLineIds, String applyStatus, String buyerId, String buyer, String buyerName) {
//		Assert.notNull(requirementLineIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
//		Assert.notNull(applyStatus, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
//		if (RequirementApplyStatus.ASSIGNED.getValue().equals(applyStatus)) { // ??????????????????????????????????????????ID????????????????????????????????????????????????
//			Assert.notNull(buyerId, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
//			Assert.notNull(buyer, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
//			Assert.notNull(buyerName, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
//		}
//
//		/** ?????????????????????????????? */
//		Long userId = null;
//		String userName = "";
//		LoginAppUser user = AppUserUtil.getLoginAppUser();
//		if (null != user) {
//			userId = user.getUserId();
//			userName = user.getUsername();
//		}
//		String loginIp = IPUtil.getRemoteIpAddr(HttpServletHolder.getRequest());
//
//		/** ????????????????????????????????? */
//		List<Long> requirementLineIdList = Arrays.asList(requirementLineIds);
//		List<RequirementLine> requirementLineList = getBaseMapper().selectBatchIds(requirementLineIdList);
//		List<RequirementLine> updateLineList = new ArrayList<>();
//		if (CollectionUtils.isNotEmpty(requirementLineList)) {
//			for (RequirementLine line : requirementLineList) {
//				if (null != line) {
//					if (RequirementApplyStatus.ASSIGNED.getValue().equals(applyStatus) && applyStatus.equals(line.getApplyStatus())) {
//						Assert.isTrue(false, "???????????????????????????????????????");
//					} else if (RequirementApplyStatus.UNASSIGNED.getValue().equals(applyStatus) && applyStatus.equals(line.getApplyStatus())) {
//						Assert.isTrue(false, "???????????????????????????????????????");
//					}
//					RequirementLine requirementLine = new RequirementLine();
//					requirementLine.setRequirementLineId(line.getRequirementLineId());
//					requirementLine.setApplyStatus(applyStatus);
//					// ????????????????????????ID?????????????????????????????????????????????ID????????????????????????
//					if (RequirementApplyStatus.ASSIGNED.getValue().equals(applyStatus)) {
//						requirementLine.setBuyerId(Long.parseLong(buyerId));
//						requirementLine.setBuyerName(buyerName);
//						requirementLine.setBuyer(buyer);
//					} else if (RequirementApplyStatus.UNASSIGNED.getValue().equals(applyStatus)) {
//						requirementLine.setBuyerId(null);
//						requirementLine.setBuyerName(null);
//						requirementLine.setBuyer(null);
//						requirementLine.setEnableUnAssigned(true); // ????????????????????????true
//					}
//
//					requirementLine.setLastUpdatedId(userId);
//					requirementLine.setLastUpdatedBy(userName);
//					requirementLine.setLastUpdatedByIp(loginIp);
//					requirementLine.setLastUpdateDate(new Date());
//					updateLineList.add(requirementLine);
//				}
//			}
//		}
//		return this.bachUpdateRequirementLine(updateLineList);
//	}

//  ToDo longi???????????????????????????
//	@Override
//	@Transactional
//	public BaseResult<String> isMergeRequirement(Long[] requirementLineIds) {
//		Assert.notNull(requirementLineIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
//
//		BaseResult<String> result = BaseResult.build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
//		/** ????????????????????????????????? */
//		List<Long> requirementLineIdList = Arrays.asList(requirementLineIds);
//		List<RequirementLine> requirementLineList = getBaseMapper().selectBatchIds(requirementLineIdList);
//		if (CollectionUtils.isEmpty(requirementLineList)) {
//			Assert.isTrue(false, "??????ID?????????????????????????????????");
//		}
//		if (CollectionUtils.isNotEmpty(requirementLineList)) {
//			for (RequirementLine line : requirementLineList) {
//				if (null != line) {
//					String applyStatus = line.getApplyStatus();
//					if (!RequirementApplyStatus.ASSIGNED.getValue().equals(applyStatus)) {
//						Assert.isTrue(false, "??????????????????????????????,????????????????????????");
//					}
//
//					// ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
//					Long lineId = line.getRequirementLineId();
//					String orgCode = (StringUtils.isNotBlank(line.getOrgCode()) ? line.getOrgCode() : "");
//					String purchaseOrg = (StringUtils.isNotBlank(line.getPurchaseOrganization()) ? line.getPurchaseOrganization() : "");
//					String receivedFactory = (StringUtils.isNotBlank(line.getReceivedFactory()) ? line.getReceivedFactory() : "");
//					String requirementDepart = (StringUtils.isNotBlank(line.getRequirementDepartment()) ? line.getRequirementDepartment() : "");
//					Long categoryId = (null != line.getCategoryId() ? line.getCategoryId() : 0L);
//					Long itemId = (null != line.getItemId() ? line.getItemId() : 0L);
//					BigDecimal notaxPrice = (null != line.getNotaxPrice() ? line.getNotaxPrice() : BigDecimal.ZERO);
//					BigDecimal taxRate = (null != line.getTaxRate() ? line.getTaxRate() : BigDecimal.ZERO);
//					String inventoryPlace = (StringUtils.isNotBlank(line.getInventoryPlace()) ? line.getInventoryPlace() : "");
//					String costType = (StringUtils.isNotBlank(line.getCostType()) ? line.getCostType() : "");
//					String costNum = (StringUtils.isNotBlank(line.getCostNum()) ? line.getCostNum() : "");
//					for (RequirementLine lineEntity : requirementLineList) {
//						if (null != lineEntity && !lineId.equals(lineEntity.getRequirementLineId())) {
//							String applyStatusCompare = (StringUtils.isNoneBlank(lineEntity.getApplyStatus()) ? lineEntity.getApplyStatus() : "");
//							String orgCodeCompare = (StringUtils.isNotBlank(lineEntity.getOrgCode()) ? lineEntity.getOrgCode() : "");
//							String purchaseOrgCompare = (StringUtils.isNotBlank(lineEntity.getPurchaseOrganization()) ? lineEntity.getPurchaseOrganization() : "");
//							String requirementDepartCompare = (StringUtils.isNotBlank(lineEntity.getRequirementDepartment()) ? lineEntity.getRequirementDepartment() : "");
//							Long categoryIdCompare = (null != line.getCategoryId() ? lineEntity.getCategoryId() : 0L);
//							Long itemIdCompare = (null != line.getItemId() ? lineEntity.getItemId() : 0L);
//							BigDecimal notaxPriceCompare = (null != line.getNotaxPrice() ? line.getNotaxPrice() : BigDecimal.ZERO);
//							BigDecimal taxRateCompare = (null != line.getTaxRate() ? line.getTaxRate() : BigDecimal.ZERO);
//							String inventoryPlaceCompare = (StringUtils.isNotBlank(lineEntity.getInventoryPlace()) ? lineEntity.getInventoryPlace() : "");
//							String costTypeCompare = (StringUtils.isNotBlank(lineEntity.getCostType()) ? lineEntity.getCostType() : "");
//							String costNumCompare = (StringUtils.isNotBlank(lineEntity.getCostNum()) ? lineEntity.getCostNum() : "");
//							String receivedFactoryCompare = (StringUtils.isNotBlank(lineEntity.getReceivedFactory()) ? lineEntity.getReceivedFactory() : "");
//							if (!(applyStatus.equals(applyStatusCompare) && orgCode.equals(orgCodeCompare) && purchaseOrg.equals(purchaseOrgCompare) && requirementDepart.equals(requirementDepartCompare) && categoryId.longValue() == categoryIdCompare.longValue() && itemId.longValue() == itemIdCompare.longValue() && inventoryPlace.equals(inventoryPlaceCompare) && costNum.equals(costNumCompare) && costType.equals(costTypeCompare) && taxRate.compareTo(taxRateCompare) == 0)
//									&& notaxPrice.compareTo(notaxPriceCompare) == 0 && receivedFactory.equals(receivedFactoryCompare)) {
//								Assert.isTrue(false, "???????????????????????????,????????????????????????");
//							}
//						}
//					}
//				}
//			}
//		}
//		return result;
//	}

    @Override
    @Transactional
    public List<RequirementLine> findRequirementMergeList(Long[] requirementLineIds) {
        Assert.notNull(requirementLineIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        List<RequirementLine> requirementLineList = new ArrayList<>();
        try {
            requirementLineList = getBaseMapper().findRequirementMergeList(Arrays.asList(requirementLineIds));
        } catch (Exception e) {
            log.error("??????" + JsonUtil.entityToJsonStr(requirementLineIds) + "??????????????????????????????????????????" + e);
            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
        }
        return requirementLineList;
    }

    @Override
    @Transactional
    public BaseResult<List<RequirementLine>> bachRequirementMergePreview(Long[] requirementLineIds) {
        Assert.notNull(requirementLineIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());

        BaseResult<List<RequirementLine>> result = BaseResult.build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
        /** ????????????????????????????????? */
        List<RequirementLine> requirementLineList = this.findRequirementMergeList(requirementLineIds);
        result.setData(requirementLineList);
        return result;
    }


//	@Override
//	@Transactional(rollbackFor = Exception.class)
//	public BaseResult<String> bachRequirementMerge(Long[] requirementLineIds) {
//		Assert.notNull(requirementLineIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
//		this.isMergeRequirement(requirementLineIds); // ?????????????????????,????????????????????????
//
//		BaseResult<String> result = BaseResult.build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
//		List<RequirementLine> requirementLineList = this.findRequirementMergeList(requirementLineIds);
//		// ????????????????????????ID???????????????????????????????????????
//		List<RequirementHeadQueryDTO> requirementHeadQueryDTOList = requirementHeadMapper.findRequirementHeadAndLineByLineIds(Arrays.asList(requirementLineIds));
//		if (CollectionUtils.isEmpty(requirementHeadQueryDTOList)) {
//			Assert.isTrue(false, "??????ID??????????????????????????????");
//		}
//		try {
//
//			/*** ??????????????????????????????????????? */
//			// ???????????????Id????????????
//			Long buyerId = null;
//			String buyer = "";
//			String buyerName = "";
//			Long orgId = null;
//			String requirementDepart = "";
//			if (null != requirementHeadQueryDTOList.get(0) && CollectionUtils.isNotEmpty(requirementHeadQueryDTOList.get(0).getRequirementLineList()) && null != requirementHeadQueryDTOList.get(0).getRequirementLineList().get(0)) {
//				RequirementLine requirementLine = requirementHeadQueryDTOList.get(0).getRequirementLineList().get(0);
//				buyerId = requirementLine.getBuyerId();
//				buyer = requirementLine.getBuyer();
//				buyerName = requirementLine.getBuyerName();
//				orgId = requirementLine.getOrgId();
//				requirementDepart = requirementLine.getRequirementDepartment();
//			}
//			// ??????????????????
//			LoginAppUser user = AppUserUtil.getLoginAppUser();
//			String userName = (null != user ? user.getNickname() : "");
//
//			// ?????????????????????????????????????????????????????????????????????????????????
//			String requirementHeadNum = baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM);
//			RequirementHead insertRequirementHead = new RequirementHead();
//			Long headId = IdGenrator.generate();
//			insertRequirementHead.setApplyDate(DateChangeUtil.asLocalDate(new Date()));
//			insertRequirementHead.setAuditStatus(RequirementApproveStatus.APPROVED.getValue());
//			insertRequirementHead.setHandleStatus(RequirementHeadHandleStatus.ALL.getValue());
//			insertRequirementHead.setRequirementHeadNum(requirementHeadNum);
//			insertRequirementHead.setCreatedFullName(userName);
//			insertRequirementHead.setCreateType(RequirementCreateType.MERGE_NEW.value);
//			insertRequirementHead.setRequirementHeadId(headId);
//
//			int insertHeadCount = requirementHeadMapper.insert(insertRequirementHead);
//			if (insertHeadCount == 0 || null == insertRequirementHead.getRequirementHeadId()) {
//				log.error("????????????ID: " + JsonUtil.entityToJsonStr(requirementLineIds) + "???????????????????????????-?????????????????????");
//				throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
//			}
//			for (int i = 0; i < requirementLineList.size(); i++) {
//				RequirementLine requirementLine = requirementLineList.get(i);
//				if (null != requirementLine) {
//					requirementLine.setRequirementLineId(IdGenrator.generate());
//					requirementLine.setBuyerId(buyerId);
//					requirementLine.setBuyer(buyer);
//					requirementLine.setBuyerName(buyerName);
//					requirementLine.setApplyStatus(RequirementApplyStatus.ASSIGNED.getValue());
//					requirementLine.setRequirementHeadId(headId);
//					requirementLine.setRequirementHeadNum(requirementHeadNum);
//					requirementLine.setOrgId(orgId);
//					requirementLine.setRequirementDepartment(requirementDepart);
//					requirementLine.setRowNum(i + 1);
//					requirementLine.setRequirementSource(RequirementSourceSystem.SRM.value);
//					requirementLine.setCreatedFullName(userName);
//					requirementLine.setOrderQuantity(requirementLine.getRequirementQuantity());
//					if (requirementLine.getNotaxPrice() != null) {
//						if (requirementLine.getRequirementQuantity() != null) {
//							requirementLine.setTotalAmount(requirementLine.getNotaxPrice().multiply(requirementLine.getRequirementQuantity()));
//						}
//						requirementLine.setHaveEffectivePrice(RequirementEffectivePrice.HAVE.value);
//						requirementLine.setHaveSupplier(RequirementSourcingSupplier.HAVE.value);
//					} else {
//						requirementLine.setHaveEffectivePrice(RequirementEffectivePrice.NOT.value);
//						requirementLine.setHaveSupplier(RequirementSourcingSupplier.NOT.value);
//					}
//				}
//			}
//			boolean isSave = super.saveBatch(requirementLineList);
//			if (!isSave) {
//				log.error("????????????ID: " + JsonUtil.entityToJsonStr(requirementLineIds) + "???????????????????????????-???????????????");
//				throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
//			}
//
//			/** ????????????????????????????????????????????? */
//			for (RequirementHeadQueryDTO headQueryDTO : requirementHeadQueryDTOList) {
//				List<RequirementLine> updateLineList = new ArrayList<>();
//				if (null != headQueryDTO && CollectionUtils.isNotEmpty(headQueryDTO.getRequirementLineList())) {
//					for (RequirementLine requirementLine : headQueryDTO.getRequirementLineList()) {
//						if (null != requirementLine) {
//							RequirementLine updateLine = new RequirementLine();
//							updateLine.setRequirementLineId(requirementLine.getRequirementLineId());
//							updateLine.setApplyStatus(RequirementApplyStatus.MERGED.getValue());
//							updateLine.setFollowFormCode(requirementHeadNum);
//							updateLineList.add(updateLine);
//						}
//					}
//				}
//				if (CollectionUtils.isNotEmpty(updateLineList)) {
//					getBaseMapper().bachUpdateRequirementLine(updateLineList);
//				}
//			}
//
//		} catch (Exception e) {
//			log.error("????????????ID: " + JsonUtil.entityToJsonStr(requirementLineIds) + "???????????????????????????-???????????????" + e);
//			throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
//		}
//		return result;
//	}

    private void checkAddBatchParam(RequirementLine line) {
        Assert.isTrue(line.getOrganizationId() == null, "????????????????????????");
//		Assert.isTrue((line.getBudget() != null && line.getBudget().compareTo(BigDecimal.ZERO) >= 0), "?????????????????????????????????0");
        Assert.isTrue(line.getCategoryId() == null, "????????????????????????");
        Assert.isTrue(StringUtils.isNotBlank(line.getMaterialCode()), "????????????????????????");
        Assert.isTrue(StringUtils.isNotBlank(line.getMaterialName()), "????????????????????????");
        Assert.isTrue((line.getRequirementQuantity() != null && line.getRequirementQuantity().compareTo(BigDecimal.ZERO) >= 0), "???????????????????????????????????????0");
//		Assert.isTrue(StringUtils.isNotBlank(line.getApplyReason()), "????????????????????????");
//		Assert.isTrue(StringUtils.isNotBlank(line.getInventoryPlace()), "????????????????????????");
//		Assert.isTrue(StringUtils.isNotBlank(line.getCostType()), "????????????????????????");
//		Assert.isTrue(StringUtils.isNotBlank(line.getCostNum()), "????????????????????????");
        // ?????????????????????
    }

//
//	???????????????
//	@Override
//	@Transactional
//	public List<RequirementLine> importExcelInfo(List<Object> list) {
//		List<RequirementLine> requirementLineList = new ArrayList<>();
//		List<NetPriceQueryDTO> netPriceQueryDTOList = new ArrayList<>();
//		List<String> orgNameList = new ArrayList<>();
//		List<String> targetNumList = new ArrayList<>();
//		log.debug("objList: " + JSON.toJSONString(list));
//
//		for (Object obj : list) {
//			RequirementLineImportDTO vo = (RequirementLineImportDTO) obj;
//			log.debug("????????????" + JSON.toJSONString(vo));
//			RequirementLine line = new RequirementLine();
//			BeanCopyUtil.copyProperties(line, vo);
//			line.setRequirementDate(DateChangeUtil.asLocalDate(vo.getRequirementDate()));
//			log.debug("????????????" + JSON.toJSONString(line));
//			checkParam(line);
//
//			orgNameList.add(line.getPurchaseOrganization());
//			targetNumList.add(line.getMaterialCode());
//
//			requirementLineList.add(line);
//		}
//		// ??????????????????????????????????????????map
//		log.debug("??????????????????" + JSON.toJSONString(requirementLineList));
//		List<Organization> organizationList = baseClient.getOrganizationByNameList(orgNameList);
//		Map<String, Organization> organizatioMap = organizationList.stream().collect(Collectors.toMap(Organization::getOrganizationName, org -> org));
//		log.debug("???????????????????????????" + JSON.toJSONString(organizatioMap));
//
//		List<MaterialItem> materialItemList = baseClient.listMaterialByCodeBatch(targetNumList);
//		Map<String, MaterialItem> materialItemMap = materialItemList.stream().collect(Collectors.toMap(MaterialItem::getMaterialCode, materialItem -> materialItem));
//		log.debug("?????????????????????" + JSON.toJSONString(materialItemMap));
//
//		for (int i = 0; i < requirementLineList.size(); i++) {
//			RequirementLine line = requirementLineList.get(i);
//			log.debug("???" + i + "?????????:" + line);
//			// ???????????????????????????
//			Organization organization = organizatioMap.get(line.getOrganizationName());
//			Assert.notNull(organization, LocaleHandler.getLocaleMsg("????????????????????????????????????,???????????????????????????", "" + (i + 1)));
//			line.setOrganizationId(organization.getOrganizationId()).setOrganizationCode(organization.getOrganizationCode());
//
//			MaterialItem materialItem = materialItemMap.get(line.getMaterialCode());
//			Assert.notNull(materialItem, LocaleHandler.getLocaleMsg("??????????????????????????????,?????????????????????", "" + (i + 1)));
//			line.setMaterialId(materialItem.getMaterialId()).setMaterialCode(materialItem.getMaterialCode()).setMaterialName(materialItem.getMaterialName());
//
//			NetPriceQueryDTO netPriceQueryDTO = new NetPriceQueryDTO();
//			netPriceQueryDTO.setMaterialId(line.getMaterialId()).setOrganizationId(line.getOrganizationId()).setRequirementDate(DateChangeUtil.asDate(line.getRequirementDate()));
//			netPriceQueryDTOList.add(netPriceQueryDTO);
//		}
//
//		// ????????????????????????,??????????????????id?????????id??????key,???????????????????????????,???value?????????
//		List<PriceLibrary> priceLibraryList = inqClient.listPriceLibraryByParam(netPriceQueryDTOList);
//		Map<String, List<PriceLibrary>> priceLibrarymMap = new HashMap<>();
//		for (PriceLibrary priceLibrary : priceLibraryList) {
//			StringBuilder sb = new StringBuilder();
//			sb.append(priceLibrary.getOrganizationId()).append(priceLibrary.getItemId());
//			String keyStr = sb.toString();
//
//			List<PriceLibrary> tempList = priceLibrarymMap.get(keyStr);
//			if (tempList == null) {
//				List<PriceLibrary> newList = new ArrayList<>();
//				newList.add(priceLibrary);
//				priceLibrarymMap.put(keyStr, newList);
//			} else {
//				tempList.add(priceLibrary);
//				priceLibrarymMap.put(keyStr, tempList);
//			}
//		}
//		log.debug("???????????????????????????" + JSON.toJSONString(priceLibrarymMap));
//
//		for (RequirementLine line : requirementLineList) {
//			PriceLibrary priceLibrary = new PriceLibrary();
//			StringBuilder sb = new StringBuilder();
//			String orgIdAndMaterialStr = sb.append(line.getOrgId()).append(line.getMaterialId()).toString();
//			log.debug("??????????????????key: " + orgIdAndMaterialStr);
//			List<PriceLibrary> valuesList = priceLibrarymMap.get(orgIdAndMaterialStr);
//			log.debug("??????????????????????????????: " + JSON.toJSONString(valuesList));
//			Date requirementDateTime = DateChangeUtil.asDate(line.getRequirementDate());
//			if (CollectionUtils.isNotEmpty(valuesList)) {
//				if (valuesList.size() == 1) {
//					priceLibrary = valuesList.get(0);
//					log.debug("??????????????????????????????: " + JSON.toJSONString(priceLibrary));
//				} else {
//					// ???????????????????????????????????????: ????????????????????????????????????,??????????????????????????????????????????,??????????????????????????????
//					for (PriceLibrary value : valuesList) {
//						if (requirementDateTime.compareTo(value.getEffectiveDate()) >= 0 && requirementDateTime.compareTo(value.getExpirationDate()) <= 0) {
//							priceLibrary = value;
//							log.debug("????????????????????????: " + JSON.toJSONString(priceLibrary));
//						}
//					}
//				}
//			}
//
//			// ????????????????????????????????????,?????????null
//			line.setCategoryId(priceLibrary.getCategoryId()).setCategoryName(priceLibrary.getCategoryName()).setUnit(priceLibrary.getUnit()).setNotaxPrice(priceLibrary.getNotaxPrice()).setCurrency(priceLibrary.getCurrency()).setTaxRate(priceLibrary.getTaxRate() != null ? new BigDecimal(priceLibrary.getTaxRate().toString()) : null).setTaxKey(priceLibrary.getTaxKey());
//
//			// ?????????????????????null
//			if (line.getNotaxPrice() != null) {
//				line.setTotalAmount(line.getNotaxPrice().multiply(line.getRequirementQuantity()));
//			}
//		}
//
//		return requirementLineList;
//	}


    @Override
    public PageInfo<RequirementLine> listApprovedApplyByPage(RequirementLine requirementLine) {
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        List<RequirementHead> requirementHeadList = iRequirementHeadService.list(new QueryWrapper<>(new RequirementHead().setAuditStatus(RequirementApproveStatus.APPROVED.getValue())));
        if (CollectionUtils.isEmpty(requirementHeadList)) {
            return new PageInfo<>();
        }
        List<Long> headIdList = requirementHeadList.stream().map(RequirementHead::getRequirementHeadId).collect(Collectors.toList());
        PageUtil.startPage(requirementLine.getPageNum(), requirementLine.getPageSize());
        QueryWrapper<RequirementLine> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNoneBlank(requirementLine.getOrgName()), "ORG_NAME", requirementLine.getOrgName());
        wrapper.like(StringUtils.isNoneBlank(requirementLine.getMaterialCode()), "MATERIAL_CODE", requirementLine.getMaterialCode());
        wrapper.like(StringUtils.isNoneBlank(requirementLine.getCategoryName()), "CATEGORY_NAME", requirementLine.getCategoryName());
        wrapper.like(StringUtils.isNoneBlank(requirementLine.getRequirementDepartment()), "REQUIREMENT_DEPARTMENT", requirementLine.getRequirementDepartment());
        wrapper.like(StringUtils.isNoneBlank(requirementLine.getCostNum()), "COST_NUM", requirementLine.getCostNum());
        wrapper.like(StringUtils.isNoneBlank(requirementLine.getRequirementHeadNum()), "REQUIREMENT_HEAD_NUM", requirementLine.getRequirementHeadNum());
        wrapper.eq(StringUtils.isNoneBlank(requirementLine.getCostType()), "COST_TYPE", requirementLine.getCostType());
        wrapper.eq(StringUtils.isNoneBlank(requirementLine.getApplyStatus()), "APPLY_STATUS", requirementLine.getApplyStatus());
        if (StringUtils.equals(UserType.BUYER.name(), user.getUserType())) {
            if (StringUtils.equals(MainType.N.name(), user.getMainType())) {
                wrapper.eq(user.getUserId() != null, "BUYER_ID", user.getUserId());
            }
        }
        wrapper.in("REQUIREMENT_HEAD_ID", headIdList);
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return new PageInfo<>(this.list(wrapper));
    }

    @Override
    @Transactional
    public void updateOrderQuantity(OrderQuantityParam orderQuantityParam) {
        Assert.notNull(orderQuantityParam.getRequirementLineId(), "???????????????ID????????????");
        Assert.notNull(orderQuantityParam.getRevertAmount(), "????????????????????????????????????");

        RequirementLine requirementLine = this.getOne(new QueryWrapper<>(new RequirementLine().setRequirementLineId(orderQuantityParam.getRequirementLineId())));
        BigDecimal oldAmount = requirementLine.getOrderQuantity();
        BigDecimal revertAmount = orderQuantityParam.getRevertAmount();
        BigDecimal newAmount = oldAmount.add(revertAmount);
        this.updateById(new RequirementLine().setRequirementLineId(orderQuantityParam.getRequirementLineId()).setOrderQuantity(newAmount));
    }

    @Override
    @Transactional
    public void updateOrderQuantityBatch(List<OrderQuantityParam> orderQuantityParamList) {
        Assert.isTrue(CollectionUtils.isNotEmpty(orderQuantityParamList), "???????????????????????????????????????????????????????????????");
        orderQuantityParamList.forEach(this::updateOrderQuantity);
    }

    @Override
    public VendorAndEffectivePriceVO getVendorAndEffectivePrice(RequirementLine requirementLine) {
        NetPriceQueryDTO netPriceQueryDTO = new NetPriceQueryDTO();
        netPriceQueryDTO.setMaterialId(requirementLine.getMaterialId());
        netPriceQueryDTO.setOrganizationId(requirementLine.getOrgId());
        netPriceQueryDTO.setRequirementDate(DateChangeUtil.asDate(requirementLine.getRequirementDate()));
        PriceLibrary priceLibraryByParam = inqClient.getPriceLibraryByParam(netPriceQueryDTO);
        if (priceLibraryByParam == null) {
            return new VendorAndEffectivePriceVO().setHaveEffectivePrice(RequirementEffectivePrice.NOT.value).setHaveSupplier(RequirementSourcingSupplier.NOT.value);
        } else {
            return new VendorAndEffectivePriceVO().setHaveEffectivePrice(RequirementEffectivePrice.HAVE.value).setHaveSupplier(RequirementSourcingSupplier.HAVE.value);
        }
    }

//    @Override
//    public List<RecommendVendorVO> listRecommendVendor(List<RequirementLine> requirementLineList) {
//        List<RecommendVendorVO> recommendVendorVOList = new ArrayList<>();
//        if (CollectionUtils.isNotEmpty(requirementLineList)) {
//            // ???????????????????????????
//            Long orgId = requirementLineList.get(0).getOrgId();
//            requirementLineList.forEach(requirementLine -> {
//                if (!orgId.equals(requirementLine.getOrgId())) {
//                    throw new BaseException("?????????????????????????????????");
//                }
//            });
//            for (RequirementLine requirementLine : requirementLineList) {
//                if (!RequirementSourcingSupplier.HAVE.value.equals(requirementLine.getHaveSupplier()) || !RequirementEffectivePrice.HAVE.value.equals(requirementLine.getHaveEffectivePrice())) {
//                    throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????"));
//                }
//                if (!RequirementApplyStatus.ASSIGNED.getValue().equals(requirementLine.getApplyStatus()) && !RequirementApplyStatus.CREATED.getValue().equals(requirementLine.getApplyStatus()) && !RequirementApplyStatus.TRANSFERRED_ORDER.getValue().equals(requirementLine.getApplyStatus())) {
//                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????,????????????????????????"));
//                }
//                String fullPathId = requirementLine.getFullPathId();
//                List<PriceLibrary> priceLibraryList = inqClient.listPriceLibrary(new NetPriceQueryDTO().setMaterialId(requirementLine.getMaterialId()).setOrganizationId(requirementLine.getOrgId()).setRequirementDate(DateChangeUtil.asDate(requirementLine.getRequirementDate())));
//                if (CollectionUtils.isNotEmpty(priceLibraryList)) {
//                    for (PriceLibrary priceLibrary : priceLibraryList) {
//                        // ??????????????????????????????????????????
//                        OrgInfo orgInfo = supplierClient.getOrgInfoByOrgIdAndCompanyId(priceLibrary.getOrganizationId(), priceLibrary.getVendorId());
//                        if (null != orgInfo && StringUtil.notEmpty(orgInfo.getOrgCode())) {
//                            if (StringUtil.notEmpty(orgInfo.getServiceStatus()) && "EFFECTIVE".equals(orgInfo.getServiceStatus())) {
//                                RecommendVendorVO recommendVendorVO = new RecommendVendorVO();
//                                BeanCopyUtil.copyProperties(recommendVendorVO, priceLibrary);
//                                recommendVendorVO.setRequirementQuantity(requirementLine.getRequirementQuantity()).setRequirementLineId(requirementLine.getRequirementLineId()).setBuyerName(requirementLine.getBuyerName()).setRequirementHeadId(requirementLine.getRequirementHeadId()).setRequirementHeadNum(requirementLine.getRequirementHeadNum()).setInventoryPlace(requirementLine.getInventoryPlace()).setRowNum(requirementLine.getRowNum()).setCostNum(requirementLine.getCostNum())
//                                        .setCostType(requirementLine.getCostType()).setRequirementDate(requirementLine.getRequirementDate()).setReceivedFactory(requirementLine.getReceivedFactory()).setOrderQuantity(requirementLine.getOrderQuantity()).setFullPathId(fullPathId);
//                                recommendVendorVOList.add(recommendVendorVO);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return recommendVendorVOList;
//    }

    /**
     * ?????????????????????????????? TODO
     * @param requirementLineList
     * @return
     */
    @Override
    public List<RecommendVendorVO> listRecommendVendor(List<RequirementLine> requirementLineList) {
        List<RecommendVendorVO> recommendVendorVOList = new ArrayList<>();
        List<RecommendVendorVO> recommendVendorVOListNew = new ArrayList<>();
        Map<String, BigDecimal> keyNumVendor = new HashMap<>();
        Map<String, BigDecimal> keyNumItem = new HashMap<>();
        Map<String, BigDecimal> keyNumItemAlready = new HashMap<>();
        if (CollectionUtils.isNotEmpty(requirementLineList)) {
            // ???????????????????????????
            Long orgId = requirementLineList.get(0).getOrgId();
            requirementLineList.forEach(requirementLine -> {
                if(!orgId.equals(requirementLine.getOrgId())){
                    throw new BaseException("?????????????????????????????????");
                }
            });
            Map<String, QuotaHead> quotaHeadMap = getQuotaHeadMap(requirementLineList);
            for(RequirementLine requirementLine : requirementLineList){
                if (!RequirementSourcingSupplier.HAVE.value.equals(requirementLine.getHaveSupplier()) || !RequirementEffectivePrice.HAVE.value.equals(requirementLine.getHaveEffectivePrice())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????"));
                }
                if (!RequirementApplyStatus.ASSIGNED.getValue().equals(requirementLine.getApplyStatus()) && !RequirementApplyStatus.CREATED.getValue().equals(requirementLine.getApplyStatus()) && !RequirementApplyStatus.TRANSFERRED_ORDER.getValue().equals(requirementLine.getApplyStatus())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????,????????????????????????"));
                }
                // ????????????????????????
                String ifDistributionVendor = requirementLine.getIfDistributionVendor();
                Assert.isTrue(!IfDistributionVendor.ALLOCATING.name().equals(ifDistributionVendor),"????????????????????????\"????????????\"???????????????????????????");
                if (IfDistributionVendor.N.name().equals(ifDistributionVendor) ||
                        IfDistributionVendor.ALLOCATING_FAILED.name().equals(ifDistributionVendor)) {
                    /**
                     * ??????????????????????????????????????????????????????????????????????????????????????????????????????
                     */
                    QuotaHead quotaHead = quotaHeadMap.get(String.valueOf(requirementLine.getOrgId()) + requirementLine.getMaterialId());
                    Assert.isNull(quotaHead,requirementLine.getRequirementHeadNum()+":????????????????????????????????????");
                    String fullPathId = requirementLine.getFullPathId();
                    // ???????????????+?????? ???????????????????????????
                    List<PriceLibrary> priceLibraryList = inqClient.listPriceLibrary(new NetPriceQueryDTO().setMaterialId(requirementLine.getMaterialId()).setOrganizationId(requirementLine.getOrgId()).setRequirementDate(DateChangeUtil.asDate(requirementLine.getRequirementDate())));
                    if (CollectionUtils.isNotEmpty(priceLibraryList)) {
                        for (PriceLibrary priceLibrary : priceLibraryList) {
                            // ??????????????????????????????????????????
                            OrgInfo orgInfo = supplierClient.getOrgInfoByOrgIdAndCompanyId(priceLibrary.getOrganizationId(), priceLibrary.getVendorId());
                            if (null != orgInfo && StringUtil.notEmpty(orgInfo.getOrgCode())) {
                                if (StringUtil.notEmpty(orgInfo.getServiceStatus()) && "EFFECTIVE".equals(orgInfo.getServiceStatus())) {
                                    RecommendVendorVO recommendVendorVO = new RecommendVendorVO();
                                    BeanCopyUtil.copyProperties(recommendVendorVO, priceLibrary);
                                    recommendVendorVO.setRequirementQuantity(requirementLine.getRequirementQuantity()).setRequirementLineId(requirementLine.getRequirementLineId()).setBuyerName(requirementLine.getBuyerName()).setRequirementHeadId(requirementLine.getRequirementHeadId()).setRequirementHeadNum(requirementLine.getRequirementHeadNum()).setInventoryPlace(requirementLine.getInventoryPlace()).setRowNum(requirementLine.getRowNum()).setCostNum(requirementLine.getCostNum())
                                            .setVendorId(priceLibrary.getVendorId()).setAllocationQuotaFlag(0).setCostType(requirementLine.getCostType()).setRequirementDate(requirementLine.getRequirementDate()).setReceivedFactory(requirementLine.getReceivedFactory()).setOrderQuantity(requirementLine.getOrderQuantity()).setFullPathId(fullPathId);
                                    recommendVendorVOList.add(recommendVendorVO);
                                }
                            }
                        }
                    }
                }else {
                    Assert.notNull(requirementLine.getOrgId(),"??????????????????");
                    Assert.notNull(requirementLine.getMaterialId(),"??????????????????");
                    Assert.notNull(requirementLine.getRequirementDate(),"????????????????????????");
                    // ??????????????????
                    QuotaHead quotaHead = baseClient.getQuotaHeadByOrgIdItemIdDate(QuotaParamDto.builder().
                            orgId(requirementLine.getOrgId()).
                            itemId(requirementLine.getMaterialId()).
                            requirementDate(DateUtil.localDateToDate(requirementLine.getRequirementDate())).build());
                    Assert.notNull(quotaHead,requirementLine.getRequirementHeadNum()+":???????????????????????????;");
                    List<QuotaLine> quotaLineList = quotaHead.getQuotaLineList();
                    Assert.notNull(quotaLineList,requirementLine.getRequirementHeadNum()+":???????????????????????????;");
                    // ?????????????????????
                    BigDecimal sumNumber = quotaLineList.stream().map(quotaLine -> null != quotaLine.getAllocatedAmount() ? quotaLine.getAllocatedAmount() : BigDecimal.ZERO).reduce(BigDecimal::add).orElseGet(() -> BigDecimal.ZERO);
                    double num = sumNumber.doubleValue();

                    String keyItem = String.valueOf(requirementLine.getOrgId())+requirementLine.getMaterialId();
                    keyNumItemAlready.put(keyItem,sumNumber);

                    Map<Long, QuotaLine> quotaLineMap = quotaLineList.stream().collect(Collectors.toMap(QuotaLine::getCompanyId, Function.identity(),(k1,k2)->k1));
                    // ?????????????????????????????????
                    List<VendorDistDesc> vendorDistDescs = iVendorDistDescService.list(new QueryWrapper<>(new VendorDistDesc().setRequirementLineId(requirementLine.getRequirementLineId())));
                    if(CollectionUtils.isNotEmpty(vendorDistDescs)){
//						vendorDistDescs = vendorDistDescs.stream().filter(vendorDistDesc -> null != vendorDistDesc.getPlanAmount() && vendorDistDesc.getPlanAmount().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
                        // ???????????????+?????? ???????????????????????????
                        List<PriceLibrary> priceLibraryList = inqClient.listPriceLibrary(new NetPriceQueryDTO().setMaterialId(requirementLine.getMaterialId()).setOrganizationId(requirementLine.getOrgId()).setRequirementDate(DateUtil.localDateToDate(requirementLine.getRequirementDate())));
                        Assert.notNull(priceLibraryList,requirementLine.getOrgName()+"+"+requirementLine.getMaterialName()+",???????????????????????????????????????????????????");
                        Map<Long, PriceLibrary> libraryMap = priceLibraryList.stream().collect(Collectors.toMap(PriceLibrary::getVendorId, Function.identity(), (k1, k2) -> k1));
                        vendorDistDescs.forEach(vendorDistDesc -> {
                            Long companyId = vendorDistDesc.getCompanyId();
                            PriceLibrary priceLibrary = libraryMap.get(companyId);
                            Assert.notNull(priceLibrary,requirementLine.getRequirementHeadNum()+"+"+vendorDistDesc.getCompanyName()+",???????????????????????????????????????");
                            RecommendVendorVO recommendVendorVO = new RecommendVendorVO();
                            BeanCopyUtil.copyProperties(recommendVendorVO, priceLibrary);
                            // ????????????????????????
                            QuotaLine quotaLine = quotaLineMap.get(vendorDistDesc.getCompanyId());
                            // ???????????????
                            BigDecimal orderQuantity = requirementLine.getOrderQuantity();
                            recommendVendorVO.setRequirementQuantity(requirementLine.getRequirementQuantity()).
                                    setRequirementLineId(requirementLine.getRequirementLineId()).
                                    setBuyerName(requirementLine.getBuyerName()).
                                    setRequirementHeadId(requirementLine.getRequirementHeadId()).
                                    setRequirementHeadNum(requirementLine.getRequirementHeadNum()).
                                    setInventoryPlace(requirementLine.getInventoryPlace()).
                                    setRowNum(requirementLine.getRowNum()).
                                    setCostNum(requirementLine.getCostNum()).
                                    setCostType(requirementLine.getCostType()).
                                    setRequirementDate(requirementLine.getRequirementDate()).
                                    setReceivedFactory(requirementLine.getReceivedFactory()).
                                    setOrderQuantity(requirementLine.getOrderQuantity()).
                                    setOrganizationName(requirementLine.getOrgName()).
                                    setOrganizationCode(requirementLine.getOrgCode()).
                                    setFullPathId(requirementLine.getFullPathId()).
                                    setOrganizationId(requirementLine.getOrgId()).
                                    setItemId(requirementLine.getMaterialId()).

                                    setQuota(vendorDistDesc.getQuota().divide(BigDecimal.valueOf(100))).
                                    setAlreadyNum(null != quotaLine.getAllocatedAmount()?quotaLine.getAllocatedAmount():BigDecimal.ZERO).
                                    setOrderQuota(vendorDistDesc.getPlanAmount().subtract(vendorDistDesc.getActualAmount())).

                                    setQuota(null != vendorDistDesc.getPlanAmount() &&
                                            vendorDistDesc.getPlanAmount().compareTo(BigDecimal.ZERO) >0 &&
                                            null != orderQuantity &&
                                            orderQuantity.compareTo(BigDecimal.ZERO) > 0?
                                            BigDecimal.valueOf(recommendVendorVO.getOrderQuota().doubleValue()/orderQuantity.doubleValue()):BigDecimal.ZERO).
                                    setAllocationQuotaFlag(1)
                            ;
                            String key = String.valueOf(requirementLine.getOrgId())+requirementLine.getMaterialId()+vendorDistDesc.getCompanyId();
                            BigDecimal keyVuale = keyNumVendor.get(key);
                            keyNumVendor.put(key,null != keyVuale?keyVuale.add(null !=vendorDistDesc.getPlanAmount()?vendorDistDesc.getPlanAmount():BigDecimal.ZERO):vendorDistDesc.getPlanAmount());

                            BigDecimal keyVualeItem = keyNumItem.get(keyItem);
                            keyNumItem.put(keyItem,null != keyVualeItem?keyVualeItem.add(null !=vendorDistDesc.getPlanAmount()?vendorDistDesc.getPlanAmount():BigDecimal.ZERO):vendorDistDesc.getPlanAmount());

                            // ??????????????????(?????????????????????????????????????????????????????????)
                            BigDecimal allocatedAmount = quotaLine.getAllocatedAmount();
                            // ????????????
                            double num1 = null != allocatedAmount?allocatedAmount.doubleValue():0;
                            // ????????????????????????
                            if (0 != num) {
                                recommendVendorVO.setAlreadyQuota(BigDecimal.valueOf(num1/num));
                            }else {
                                recommendVendorVO.setAlreadyQuota(BigDecimal.ZERO);
                            }
                            recommendVendorVOList.add(recommendVendorVO);
                        });
                    }
                }
            }
            // ????????????????????????/???????????????
            if(CollectionUtils.isNotEmpty(recommendVendorVOList)){
                recommendVendorVOList.forEach(recommendVendorVO -> {
                    if(1 == recommendVendorVO.getAllocationQuotaFlag()){
                        String key = String.valueOf(recommendVendorVO.getOrganizationId())+recommendVendorVO.getItemId()+recommendVendorVO.getVendorId();
                        String keyItem = String.valueOf(recommendVendorVO.getOrganizationId())+recommendVendorVO.getItemId();
                        BigDecimal bigDecimal1 = keyNumVendor.get(key);
                        // ???????????????????????????
                        double num3 = null != bigDecimal1?bigDecimal1.doubleValue():0;
                        recommendVendorVO.setTotalDistribution(BigDecimal.valueOf(num3));
                        // ????????????????????????
                        BigDecimal keyVualeItem = keyNumItem.get(keyItem);
                        double num1 = null !=keyVualeItem? keyVualeItem.doubleValue():0;
                        BigDecimal bigDecimal = keyNumItemAlready.get(keyItem);
                        // ?????????????????????
                        double num2 = null != bigDecimal?bigDecimal.doubleValue():0;
                        BigDecimal alreadyNum = recommendVendorVO.getAlreadyNum();
                        // ????????????????????????
                        double num4 = null != alreadyNum?alreadyNum.doubleValue():0;
                        // ???????????????=?????????????????????????????????+??????????????????????????????/????????????)
                        double num5 = (num4+num3)/(num1+num2);
                        recommendVendorVO.setAfterQuota(BigDecimal.valueOf(num5));

                    }
                });
                // ?????? ???????????????+?????????
                recommendVendorVOList.sort((o1, o2) -> o1.getRequirementHeadNum().hashCode()-o2.getRequirementHeadNum().hashCode());
                Map<String, List<RecommendVendorVO>> collect = recommendVendorVOList.stream().collect(Collectors.groupingBy(RecommendVendorVO::getRequirementHeadNum));
                collect.forEach((s, recommendVendorVOS) -> {
                    recommendVendorVOS.sort((o1, o2) -> o1.getRowNum()-o2.getRowNum());
                    recommendVendorVOListNew.addAll(recommendVendorVOS);
                });
            }
        }
        return recommendVendorVOListNew;
    }

    public Map<String, QuotaHead> getQuotaHeadMap(List<RequirementLine> requirementLines) {
        Map<String, QuotaHead> quotaHeadMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(requirementLines)) {
            List<Long> orgIds = new ArrayList<>();
            List<Long> itemIds = new ArrayList<>();

            requirementLines.forEach(requirementLine -> {
                orgIds.add(requirementLine.getOrgId());
                itemIds.add(requirementLine.getMaterialId());
            });
            quotaHeadMap = baseClient.queryQuotaHeadByOrgIdItemId(QuotaParamDto.builder().orgIds(orgIds).itemIds(itemIds).build());
        }
        return quotaHeadMap;
    }

    /**
     * 1. ?????????????????????????????????1, ????????????
     * 2. ??????????????????????????????, ????????????????????????
     */
    public void checkRecommendVendorVOList(List<RecommendVendorVO> recommendVendorVOList) {
        Assert.isTrue(CollectionUtils.isNotEmpty(recommendVendorVOList), "?????????????????????????????????");
        // ???????????????????????????????????????
        if (CollectionUtils.isNotEmpty(recommendVendorVOList)) {
            // ??????????????????
            HashMap<Long, List<RecommendVendorVO>> hashMap = new HashMap<>();
            // ????????????
            Long organizationId = recommendVendorVOList.get(0).getOrganizationId();
            for (RecommendVendorVO recommendVendorVO : recommendVendorVOList) {
                Assert.notNull(recommendVendorVO.getOrganizationId(), "????????????????????????");
                Assert.notNull(recommendVendorVO.getOrganizationName(), "??????????????????????????????");
                Assert.notNull(recommendVendorVO.getVendorId(), "?????????????????????");
                Assert.notNull(recommendVendorVO.getVendorCode(), "???????????????????????????");
                Assert.notNull(recommendVendorVO.getVendorName(), "???????????????????????????");
                if (!organizationId.equals(recommendVendorVO.getOrganizationId())) {
                    throw new BaseException("?????????????????????????????????????????????");
                }
                Long requirementLineId = recommendVendorVO.getRequirementLineId();
                if (hashMap.containsKey(requirementLineId)) {
                    List<RecommendVendorVO> recommendVendorVOS = hashMap.get(requirementLineId);
                    recommendVendorVOS.add(recommendVendorVO);
                    hashMap.put(requirementLineId, recommendVendorVOS);
                } else {
                    ArrayList<RecommendVendorVO> recommendVendorVOS = new ArrayList<>();
                    recommendVendorVOS.add(recommendVendorVO);
                    hashMap.put(requirementLineId, recommendVendorVOS);
                }
            }
            if (!hashMap.isEmpty()) {
                for (List<RecommendVendorVO> vendorVOList : hashMap.values()) {
                    if (CollectionUtils.isNotEmpty(vendorVOList)) {
                        // ??????????????????
                        RecommendVendorVO tempVO = vendorVOList.get(0);
                        RequirementLine tempLine = this.getById(tempVO.getRequirementLineId());
                        if (tempLine == null) {
                            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
                        }
                        if (RequirementApplyStatus.COMPLETED.getValue().equals(tempLine.getApplyStatus())) {
                            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????,????????????????????????"));
                        }
                        // ????????????
                        BigDecimal sum = new BigDecimal(0);
                        for (RecommendVendorVO recommendVendorVO : vendorVOList) {
                            BigDecimal quota = recommendVendorVO.getQuota();
                            Assert.isTrue(quota.doubleValue() >= 0, "??????????????????0");
                            sum.add(quota);
                        }
                        Assert.isTrue(sum.doubleValue() <= 1, "?????????????????????????????????????????????100");
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public void genOrder(List<RecommendVendorVO> recommendVendorVOList) {
        // ??????
        checkRecommendVendorVOList(recommendVendorVOList);
        // ????????????????????????
        Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationId(recommendVendorVOList.get(0).getOrganizationId()));
        Assert.notNull(organization, "??????????????????ID??????????????????????????????");
        // ??????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        /**
         * ????????????, ????????????????????????
         */
        Map<String, List<RecommendVendorVO>> hashMap = new HashMap<>();
        for (RecommendVendorVO recommendVendorVO : recommendVendorVOList) {
            String vendorCode = recommendVendorVO.getVendorCode();
            if (hashMap.containsKey(vendorCode)) {
                List<RecommendVendorVO> vendorVOList = hashMap.get(vendorCode);
                vendorVOList.add(recommendVendorVO);
                hashMap.put(vendorCode, vendorVOList);
            } else {
                ArrayList<RecommendVendorVO> vendorVOS = new ArrayList<>();
                vendorVOS.add(recommendVendorVO);
                hashMap.put(vendorCode, vendorVOS);
            }
        }
        // ???????????????
        HashMap<Long, RequirementLine> lineHashMap = new HashMap<>();
        if (!hashMap.isEmpty()) {
            for (List<RecommendVendorVO> recommendVendorVOS : hashMap.values()) {
                // ???????????????
                String orderCode = baseClient.seqGen(SequenceCodeConstant.SEQ_SSC_ORDER_NUM);
                RecommendVendorVO recommend = recommendVendorVOS.get(0);
                String fullPathId = recommendVendorVOS.get(0).getFullPathId();
                // ????????????????????????????????????
                // ???????????????
                // ??????????????????
                Order order = new Order();
                // ?????????????????????????????????
                order.setOrganizationId(organization.getOrganizationId());
                order.setOrganizationCode(organization.getOrganizationCode());
                order.setOrganizationName(organization.getOrganizationName());
                order.setFullPathId(fullPathId);
                order.setOrderId(IdGenrator.generate());
                order.setJitOrder(JITOrder.N.name());
                order.setOrderNumber(orderCode);
                order.setSourceSystem(SourceSystemEnum.PURCHASE_REQUIREMENT.getValue());
                /*order.setOrderStatus(PurchaseOrderEnum.UNISSUED.getValue());*/
                order.setSubmittedId(loginAppUser.getUserId());
                order.setSubmittedBy(loginAppUser.getUsername());
                order.setSubmittedTime(new Date());
                /*order.setOrderType(OrderTypeEnum.STANDARD.getValue());*/
                order.setVendorId(recommend.getVendorId());
                order.setVendorCode(recommend.getVendorCode());
                order.setVendorName(recommend.getVendorName());
                order.setTaxRate(new BigDecimal(recommend.getTaxRate()));
                order.setTaxKey(recommend.getTaxKey());
                order.setRfqSettlementCurrency(recommend.getCurrency());
                order.setBuyerName(recommend.getBuyerName());
                order.setResponseStatus(ResponseStatus.UNCOMFIRMED.name());
                order.setTel(loginAppUser.getPhone());
                // ??????????????????
                FinanceInfo financeInfo = supplierClient.getFinanceInfoByCompanyIdAndOrgId(recommend.getVendorId(), recommend.getOrganizationId());
                if (financeInfo != null) {
                    order.setPaymentMethod(financeInfo.getPaymentMethod());
                    order.setTermOfPayment(financeInfo.getPaymentTerms());
                }

                // ????????????
                List<OrderDetail> orderDetailList = new ArrayList<>();
                int lineNum = 1;
                BigDecimal orderAmountSum = new BigDecimal(0);
                for (RecommendVendorVO vendorVO : recommendVendorVOS) {
                    // ???????????????
                    RequirementLine tempLine = this.getById(vendorVO.getRequirementLineId());
                    // ??????????????????
                    BigDecimal orderAmount = vendorVO.getNotaxPrice().multiply(tempLine.getOrderQuantity()).multiply(vendorVO.getQuota());
                    // ??????????????????
                    orderAmountSum.add(orderAmount);

                    // ??????????????????
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    // ???????????????
                    BigDecimal oldTotalAmount = tempLine.getOrderQuantity();
                    // ??????????????????
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setOrderId(order.getOrderId());
                    orderDetail.setOrderDetailId(IdGenrator.generate());
                    /*orderDetail.setOrderDetailStatus(PurchaseOrderEnum.UNISSUED.getValue());*/
                    orderDetail.setOrderNum(oldTotalAmount.multiply(vendorVO.getQuota()));
                    orderDetail.setLineNum(lineNum);
                    orderDetail.setExternalType("REQUIREMENT");
                    orderDetail.setExternalId(vendorVO.getRequirementHeadId());
                    orderDetail.setExternalNum(vendorVO.getRequirementHeadNum());
                    orderDetail.setExternalRowId(vendorVO.getRequirementLineId());
                    orderDetail.setExternalRowNum(vendorVO.getRowNum().longValue());
                    orderDetail.setReceiveSum(BigDecimal.ZERO);
                    orderDetail.setCategoryId(vendorVO.getCategoryId());
                    orderDetail.setCategoryName(vendorVO.getCategoryName());
                    orderDetail.setMaterialId(vendorVO.getItemId());
                    orderDetail.setMaterialName(vendorVO.getItemDesc());
                    orderDetail.setMaterialCode(vendorVO.getItemCode());
                    orderDetail.setCeeaUnitTaxPrice(vendorVO.getNotaxPrice().multiply((BigDecimal.ONE.add(new BigDecimal(vendorVO.getTaxRate()).divide(BigDecimal.valueOf(100), 5, BigDecimal.ROUND_HALF_UP)))));
                    orderDetail.setCeeaUnitNoTaxPrice(vendorVO.getNotaxPrice());
                    orderDetail.setUnit(vendorVO.getUnit());
                    orderDetail.setRequirementDate(vendorVO.getRequirementDate());
                    orderDetail.setRequirementQuantity(vendorVO.getQuota().multiply(oldTotalAmount));
                    orderDetail.setInventoryPlace(vendorVO.getInventoryPlace());
                    orderDetail.setPriceUnit(vendorVO.getPriceUnit());
                    orderDetail.setCostNum(vendorVO.getCostNum());
                    orderDetail.setCostType(vendorVO.getCostType());
                    orderDetail.setCurrency(vendorVO.getCurrency());
                    orderDetail.setReceivedFactory(vendorVO.getReceivedFactory());
                    orderDetailList.add(orderDetail);
                    totalAmount = totalAmount.add(vendorVO.getQuota().multiply(oldTotalAmount));

                    Long requirementLineId = vendorVO.getRequirementLineId();
                    if (lineHashMap.containsKey(requirementLineId)) {
                        RequirementLine requirementLine = lineHashMap.get(requirementLineId);
                        // ??????????????????
                        BigDecimal orderQuantity = requirementLine.getOrderQuantity().add(totalAmount);
                        requirementLine.setOrderQuantity(orderQuantity);
                        // ?????????????????????
                        String followFormCode = requirementLine.getFollowFormCode();
                        String newFollowFormCode = followFormCode + "," + orderCode;
                        requirementLine.setFollowFormCode(newFollowFormCode);
                        lineHashMap.put(requirementLineId, requirementLine);
                    } else {
                        // ???
                        RequirementLine requirementLine = new RequirementLine();
                        requirementLine.setRequirementLineId(requirementLineId);
                        // ?????????????????????
                        requirementLine.setFollowFormCode(orderCode);
                        // ??????????????????
                        requirementLine.setOrderQuantity(totalAmount);
                        //
                        lineHashMap.put(requirementLineId, requirementLine);
                    }
                    lineNum++;
                }
                // ????????????
                order.setOrderAmount(orderAmountSum);
                // ???????????????
                scIOrderService.saveOrUpdate(new OrderSaveRequestDTO().setDetailList(orderDetailList).setOrder(order));
            }
            if (!lineHashMap.isEmpty()) {
                lineHashMap.values().forEach(requirementLine -> {
                    Long requirementLineId = requirementLine.getRequirementLineId();
                    RequirementLine line = this.getById(requirementLineId);
                    // ??????????????????
                    BigDecimal orderQuantitySum = requirementLine.getOrderQuantity();
                    // ???????????????
                    BigDecimal orderQuantity = line.getOrderQuantity();
                    // ?????????????????????
                    BigDecimal newTotalAmount = orderQuantity.subtract(orderQuantitySum);
                    requirementLine.setOrderQuantity(newTotalAmount);
                    // ??????,?????????????????????
                    if (newTotalAmount.doubleValue() > 0) {
                        requirementLine.setApplyStatus(RequirementApplyStatus.TRANSFERRED_ORDER.getValue());
                    } else {
                        requirementLine.setApplyStatus(RequirementApplyStatus.COMPLETED.getValue());
                    }
                    String followFormCode = line.getFollowFormCode();
                    if (StringUtil.notEmpty(followFormCode)) {
                        String followForm = requirementLine.getFollowFormCode();
                        String code = followFormCode + "," + followForm;
                        requirementLine.setFollowFormCode(code);
                    }
                    this.updateById(requirementLine);
                });
            }

            // recommendVendorVOList
            /**
             * ????????????, ?????????????????????????????????
             */
            // ???????????????????????????
            List<QuotaLineDto> quotaLineDtos = new ArrayList<>();
            Map<String, List<RecommendVendorVO>> recommendVendorVOMap = recommendVendorVOList.stream().
                    filter(recommendVendorVO -> 1 == recommendVendorVO.getAllocationQuotaFlag()).
                    collect(Collectors.groupingBy(recommendVendorVO ->
                            String.valueOf(recommendVendorVO.getOrganizationId()) + recommendVendorVO.getItemId()));

            recommendVendorVOMap.values().forEach(recommendVendorVOS -> {
                // ????????????????????????
                Map<Long, List<RecommendVendorVO>> collect = recommendVendorVOS.stream().collect(Collectors.groupingBy(RecommendVendorVO::getVendorId));
                collect.values().forEach(recommendVendorVOS1 -> {
                    // ????????????????????????
                    BigDecimal sunNumber = recommendVendorVOS1.stream().map(recommendVendorVO -> null != recommendVendorVO.getOrderQuota() ? recommendVendorVO.getOrderQuota() : BigDecimal.ZERO)
                            .reduce(BigDecimal::add).orElseGet(() -> BigDecimal.ZERO);
                    Long requirementLineId = recommendVendorVOS1.get(0).getRequirementLineId();
                    Long vendorId = recommendVendorVOS1.get(0).getVendorId();
                    // ????????????????????????????????????
                    VendorDistDesc vendorDistDesc = iVendorDistDescService.list(new QueryWrapper<>(new VendorDistDesc().setRequirementLineId(requirementLineId).setCompanyId(vendorId))).get(0);
                    BigDecimal actualAmount = vendorDistDesc.getActualAmount();
                    vendorDistDesc.setActualAmount(null != actualAmount?actualAmount.add(sunNumber):sunNumber);

                    Long quotaLineId = vendorDistDesc.getQuotaLineId();
                    // ?????????????????????
                    QuotaLineDto quotaLineDto = QuotaLineDto.builder().
                            quotaLineId(quotaLineId).
                            companyId(vendorId).
                            amount(sunNumber).build();
                    quotaLineDtos.add(quotaLineDto);
                    iVendorDistDescService.updateById(vendorDistDesc);
                });
            });
            baseClient.updateQuotaLineByQuotaLineDtos(quotaLineDtos);
        }
    }

    @Override
    @Transactional
    public void genSourceBusiness(SourceBusinessGenParam param) {
        String businessGenType = param.getBusinessGenType();
        List<RequirementLine> requirementLineList = param.getRequirementLineList();
        /**
         * 1. ?????????????????????????????????, ??????????????????
         * 2. ???????????????????????????, ????????????????????????????????????
         */
        HashMap<String, RequirementLine> requirementLines = new HashMap<>();
        if (CollectionUtils.isNotEmpty(requirementLineList)) {
            Long orgId = requirementLineList.get(0).getOrgId();
            requirementLineList.forEach(requirement -> {
                if (RequirementApplyStatus.COMPLETED.getValue().equals(requirement.getApplyStatus())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????,????????????????????????"));
                }
                if (!orgId.equals(requirement.getOrgId())) {
                    throw new BaseException("??????????????????????????????????????????");
                }
                // ???????????????????????????????????????????????????
                String itemCode = requirement.getMaterialCode();
                if (!requirementLines.containsKey(itemCode)) {
                    requirementLines.put(itemCode, requirement);
                } else {
                    RequirementLine requirementLine1 = requirementLines.get(itemCode);
                    BigDecimal bigDecimal = requirementLine1.getRequirementQuantity().add(requirement.getRequirementQuantity());
                    requirementLine1.setRequirementQuantity(bigDecimal);
                    requirementLines.put(itemCode, requirementLine1);
                }
            });
            if (!requirementLines.isEmpty()) {
                List<RequirementLine> list = new ArrayList<>();
                for (RequirementLine requirementLine : requirementLines.values()) {
                    list.add(requirementLine);
                }

                if (CollectionUtils.isNotEmpty(list)) {
                    String code = null;
                    if (BusinessGenType.BID.value.equals(businessGenType)) {
                        // ???????????????
                        code = bidClient.requirementGenBiding(list);
                    } else if (BusinessGenType.INQ.value.equals(businessGenType)) {
                        // ??????????????????
                        code = inqClient.requirementGenInquiry(list);
                    }

                    // ???????????????
                    for (RequirementLine temp : requirementLineList) {
                        Long requirementLineId = temp.getRequirementLineId();
                        RequirementLine requirementLine = this.getById(requirementLineId);
                        if (null != requirementLine) {
                            RequirementLine line = new RequirementLine();
                            line.setRequirementLineId(requirementLine.getRequirementLineId());
                            // ?????????????????????
                            line.setApplyStatus(RequirementApplyStatus.CREATED.getValue());
                            // ??????????????????
                            String followFormCode = requirementLine.getFollowFormCode();
                            if (StringUtil.notEmpty(followFormCode)) {
                                String newFollowFormCode = followFormCode + "," + code;
                                line.setFollowFormCode(newFollowFormCode);
                            } else {
                                line.setFollowFormCode(code);
                            }
                            this.updateById(line);
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public void updateIfExistRequirementLine(FollowNameParam followNameParam) {
        FollowNameParam.SourceForm sourceForm = Optional.ofNullable(followNameParam.getSourceForm())
                .orElseThrow(() -> new BaseException("????????????????????????"));
        updateFollowFormName(sourceForm.getFormNum(), sourceForm.getFormTitle());
    }

    private void updateFollowFormName(String code, String name) {
        RequirementLine tempLine = this.getOne(new QueryWrapper<>(new RequirementLine().setFollowFormCode(code)));
        if (tempLine != null) {
            this.updateById(new RequirementLine().setRequirementLineId(tempLine.getRequirementLineId()).setFollowFormName(name));
        }
    }

    private void checkParam(RequirementLine line) {
        Assert.notNull(line, "????????????????????????");
        Assert.notNull(line.getOrgName(), "????????????????????????");
        Assert.notNull(line.getRequirementDepartment(), "????????????????????????");
        Assert.notNull(line.getBudget(), "??????????????????");
        Assert.notNull(line.getMaterialCode(), "????????????????????????");
        Assert.notNull(line.getRequirementQuantity(), "????????????????????????");
        Assert.notNull(line.getRequirementDate(), "????????????????????????");
//		Assert.notNull(line.getApplyReason(), "????????????????????????");
        Assert.notNull(line.getInventoryPlace(), "????????????????????????");
        Assert.notNull(line.getCostType(), "????????????????????????");
        Assert.notNull(line.getCostNum(), "????????????????????????");
    }

    @Override
    public void orderReturn(List<OrderDetail> detailList) {
        // ???????????????
        if (CollectionUtils.isNotEmpty(detailList)) {
            //requirementQuantity
            for (OrderDetail orderDetail : detailList) {
                // ????????????
                BigDecimal requirementQuantity = orderDetail.getRequirementQuantity();
                // ???id
                Long externalRowId = orderDetail.getExternalRowId();
                RequirementLine requirementLine = this.getById(externalRowId);
                // ?????????????????????
                if (null != requirementLine) {
                    RequirementLine line = new RequirementLine();
                    // ???????????????
                    BigDecimal orderQuantity = requirementLine.getOrderQuantity();
                    if (null != orderQuantity) {
                        line.setOrderQuantity(orderQuantity.add(requirementQuantity));
                    } else {
                        line.setOrderQuantity(requirementQuantity);
                    }
                    // ??????,?????????????????????
                    if (line.getOrderQuantity().doubleValue() > 0) {
                        requirementLine.setApplyStatus(RequirementApplyStatus.TRANSFERRED_ORDER.getValue());
                    } else {
                        requirementLine.setApplyStatus(RequirementApplyStatus.COMPLETED.getValue());
                    }
                    this.updateById(requirementLine);
                }

            }
        }
    }

    @Override
    @Transactional
    public void bachAssigned(RequirementManageDTO requirementManageDTO) {
        List<Long> requirementLineIds = requirementManageDTO.getRequirementLineIds();
        Assert.notEmpty(requirementLineIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        List<RequirementLine> requirementLines = this.listByIds(requirementLineIds);
        for (RequirementLine requirementLine : requirementLines) {
            if (requirementLine == null) continue;
            requirementLine.setCeeaStrategyUserId(requirementManageDTO.getCeeaStrategyUserId() == null ? requirementLine.getCeeaStrategyUserId() : requirementManageDTO.getCeeaStrategyUserId())
                    .setCeeaStrategyUserName(requirementManageDTO.getCeeaStrategyUserName() == null ? requirementLine.getCeeaStrategyUserName() : requirementManageDTO.getCeeaStrategyUserName())
                    .setCeeaStrategyUserNickname(requirementManageDTO.getCeeaStrategyUserNickname() == null ? requirementLine.getCeeaStrategyUserNickname() : requirementManageDTO.getCeeaStrategyUserNickname())
                    .setCeeaPerformUserId(requirementManageDTO.getCeeaPerformUserId() == null ? requirementLine.getCeeaPerformUserId() : requirementManageDTO.getCeeaPerformUserId())
                    .setCeeaPerformUserName(requirementManageDTO.getCeeaPerformUserName() == null ? requirementLine.getCeeaPerformUserName() : requirementManageDTO.getCeeaPerformUserName())
                    .setCeeaPerformUserNickname(requirementManageDTO.getCeeaPerformUserNickname() == null ? requirementLine.getCeeaPerformUserNickname() : requirementManageDTO.getCeeaPerformUserNickname());
            if (!RequirementApplyStatus.UNASSIGNED.getValue().equals(requirementLine.getApplyStatus())
                    && !RequirementApplyStatus.ASSIGNED.getValue().equals(requirementLine.getApplyStatus())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????,?????????????????????????????????,?????????!"));
            }
            if (requirementLine.getCeeaStrategyUserId() != null
                    && requirementLine.getCeeaPerformUserId() != null) {
                //??????????????????????????????,????????????????????????
                requirementLine.setApplyStatus(RequirementApplyStatus.ASSIGNED.getValue());
                this.updateById(requirementLine);
            } else {
                //??????,????????????????????????
                requirementLine.setApplyStatus(RequirementApplyStatus.UNASSIGNED.getValue());
                this.updateById(requirementLine);
            }
        }
    }

    @Override
    @Transactional
    public void batchReceive(RequirementManageDTO requirementManageDTO) {
        List<Long> requirementLineIds = requirementManageDTO.getRequirementLineIds();
        Assert.notEmpty(requirementLineIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        List<RequirementLine> requirementLines = this.listByIds(requirementLineIds);
        for (RequirementLine requirementLine : requirementLines) {
            if (requirementLine == null) continue;
            if (RequirementApplyStatus.ASSIGNED.getValue().equals(requirementLine.getApplyStatus())) {
                requirementLine.setApplyStatus(RequirementApplyStatus.COMPLETED.getValue());
                this.updateById(requirementLine);
            } else {
                throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????,?????????!"));
            }
        }
    }

    @Override
    public void resubmit(List<Long> requirementLineIds) {
        List<RequirementLine> requirementLines = this.listByIds(requirementLineIds);
        if (CollectionUtils.isNotEmpty(requirementLines)) {
            for (RequirementLine requirementLine : requirementLines) {
                if (requirementLine == null) continue;
                if (!RequirementApplyStatus.RETURNING.getValue().equals(requirementLine.getApplyStatus())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????,??????????????????"));
                } else {
                    if (requirementLine.getCeeaSupUserId() != null
                            && requirementLine.getCeeaStrategyUserId() != null &&
                            requirementLine.getCeeaPerformUserId() != null) {
                        requirementLine.setApplyStatus(RequirementApplyStatus.ASSIGNED.getValue());
                        this.updateById(requirementLine);
                    } else {
                        requirementLine.setApplyStatus(RequirementApplyStatus.UNASSIGNED.getValue());
                        this.updateById(requirementLine);
                    }
                }
            }
            //????????????,????????????????????????????????????
            Long requirementHeadId = requirementLines.get(0).getRequirementHeadId();
            RequirementHead byId = iRequirementHeadService.getById(requirementHeadId);
            iRequirementHeadService.assignByDivisionCategory(byId, requirementLines);
        }
    }

    //todo ?????????????????????????????????????????????????????????????????????
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReturn(RequirementManageDTO requirementManageDTO) {
        List<Long> longList = requirementManageDTO.getRequirementLineIds();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        String rejectReason = requirementManageDTO.getRejectReason();
        List<RequirementLine> requirementLineList = listByIds(longList);
        if (CollectionUtils.isNotEmpty(requirementLineList)) {
            Map<Long, List<RequirementLine>> map = requirementLineList.stream().collect(Collectors.groupingBy(RequirementLine::getRequirementHeadId));
            Map<Long, RequirementHead> headMap = iRequirementHeadService.listByIds(map.keySet()).stream().collect(Collectors.toMap(RequirementHead::getRequirementHeadId, Function.identity()));
            List<RequirementLine> updateList=new LinkedList<>();
            map.forEach((k, v) -> {
                BgtCheckReqParamDto bgtCheckReqParamDto = new BgtCheckReqParamDto();
                List<Line> lineList = new ArrayList<>();
                FSSCResult fsscResult = new FSSCResult();
                RequirementHead requirementHead = headMap.get(k);
                for (RequirementLine requirementLine : v) {
                    if (requirementLine == null) continue;
                    //???????????????
                    checkBeforeReturn(requirementLine);
                    //?????????????????????
                    BigDecimal ceeaExecutedQuantity = requirementLine.getCeeaExecutedQuantity();
                    //??????????????????????????????
                    BigDecimal requirementQuantity = requirementLine.getRequirementQuantity();

                    //?????????????????????????????????
                    requirementLine.setApplyStatus(RequirementApplyStatus.RETURNING.getValue())
                            .setRejectReason(rejectReason)
                            .setReturnOperator(loginAppUser.getNickname());
                    updateList.add(requirementLine);
                    //???????????????????????????,??????????????????????????????
                    if (CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode().equals(requirementHead.getCategoryCode())) {
                        //??????????????????   (????????????-???????????????)*????????????
                        String documentLineAmount = (requirementQuantity.subtract(ceeaExecutedQuantity))
                                .multiply(requirementLine.getNotaxPrice() == null ? BigDecimal.ZERO : requirementLine.getNotaxPrice()).toString();
                        //???????????????
                        convertRequirementLine(loginAppUser, requirementHead, lineList, requirementLine, documentLineAmount);
                    }
                }
                if (CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode().equals(requirementHead.getCategoryCode())) {
                    fsscResult = applyRelease(loginAppUser, requirementHead, bgtCheckReqParamDto, lineList);
                }
                if (FSSCResponseCode.ERROR.getCode().equals(fsscResult.getCode())) {
                    throw new BaseException(LocaleHandler.getLocaleMsg(fsscResult.getMsg()));
                }
            });
           updateBatchById(updateList);
        }
    }

    private void checkBeforeReturn(RequirementLine requirementLine) {
        if (StringUtils.isNotBlank(requirementLine.getFollowFormCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????,????????????????????????,?????????!"));
        }
    }

    @Override
    @Transactional
    public FSSCResult cancel(RequirermentLineQueryDTO requirermentLineQueryDTO) {
        List<Long> requirementLineIds = requirermentLineQueryDTO.getRequirementLineIds();
        Assert.notEmpty(requirementLineIds, LocaleHandler.getLocaleMsg("?????????????????????????????????,?????????!"));
        //2020-12-24 ???????????? bugfix-??????
        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        QueryWrapper<SubsequentDocuments> subsequentDocumentsQueryWrapper = new QueryWrapper<>();
        subsequentDocumentsQueryWrapper.in("REQUIREMENT_LINE_ID",requirementLineIds);
        List<SubsequentDocuments> subsequentDocumentsList = subsequentDocumentsService.list(subsequentDocumentsQueryWrapper);
        if(CollectionUtils.isNotEmpty(subsequentDocumentsList)){
            //??????????????????
            List<String> purchaseNumList = subsequentDocumentsList.stream().filter(item -> RelatedDocumentsEnum.PURCHASE.getName().equals(item.getIsubsequentDocumentssType())).map(item -> item.getSubsequentDocumentsNumber()).collect(Collectors.toList());
            //???????????????
            List<String> tenderNumList = subsequentDocumentsList.stream().filter(item -> RelatedDocumentsEnum.TENDER.getName().equals(item.getIsubsequentDocumentssType())).map(item -> item.getSubsequentDocumentsNumber()).collect(Collectors.toList());
            //????????????
            List<String> enquiryNumList = subsequentDocumentsList.stream().filter(item -> RelatedDocumentsEnum.ENQUIRY.getName().equals(item.getIsubsequentDocumentssType())).map(item -> item.getSubsequentDocumentsNumber()).collect(Collectors.toList());
            //??????????????????
            List<Order> orderList = scIOrderService.listByOrderNumbers(purchaseNumList);
            //???????????????
            List<Biding> tenderList = bidClient.listByNumbers(tenderNumList);
            //???????????????
            List<com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.Biding> enquiryList = bargainClient.listByNumbers(enquiryNumList);
            long count1 = orderList.stream().filter(item -> !PurchaseOrderEnum.ABANDONED.getValue().equals(item.getOrderStatus())).count();
            long count2 = tenderList.stream().filter(item -> !BiddingApprovalStatus.ABANDONED.getValue().equals(item.getAuditStatus())).count();
            long count3 = enquiryList.stream().filter(item -> !com.midea.cloud.common.enums.bargaining.projectmanagement.projectpublish.BiddingApprovalStatus.ABANDONED.getValue().equals(item.getAuditStatus())).count();
            if(count1 > 0 || count2 > 0 || count3 > 0){
                throw new BaseException("?????????????????????????????????????????????????????????");
            }
        }
        //2020-12-24 ????????????bugfix-??????

        Long requirementHeadId = requirermentLineQueryDTO.getRequirementHeadId();
        List<RequirementLine> requirementLines = this.listByIds(requirementLineIds);
        FSSCResult fsscResult = new FSSCResult();
        if (CollectionUtils.isNotEmpty(requirementLines)) {
            LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
            RequirementHead requirementHead = iRequirementHeadService.getById(requirementHeadId);
            Assert.notNull(requirementHead, LocaleHandler.getLocaleMsg("requirementHead????????????"));
            BgtCheckReqParamDto bgtCheckReqParamDto = new BgtCheckReqParamDto();
            List<Line> lineList = new ArrayList<>();
            BigDecimal ceeaTotalBudget = requirementHead.getCeeaTotalBudget();
            for (RequirementLine requirementLine : requirementLines) {
                if (requirementLine == null) continue;
                //???????????????
                checkBeforeCancel(requirementHead, requirementLine);
                //?????????????????????
                BigDecimal ceeaExecutedQuantity = requirementLine.getCeeaExecutedQuantity();
                //??????????????????????????????
                BigDecimal requirementQuantity = requirementLine.getRequirementQuantity();
                //????????????????????????=???????????????
                requirementLine.setRequirementQuantity(ceeaExecutedQuantity);
                //??????????????????????????????0
                requirementLine.setOrderQuantity(BigDecimal.ZERO);
                //?????????????????????????????????
                this.updateById(requirementLine.setApplyStatus(RequirementApplyStatus.CANCELED.getValue()));

                //kuangzm  ?????????????????????
                //???????????????????????????,??????????????????????????????
//                if (CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode().equals(requirementHead.getCategoryCode())) {
                    //??????????????????   (????????????-???????????????)*????????????
                	//??????????????????
                	String documentLineAmount = null;
                	if (null != requirementQuantity && null != ceeaExecutedQuantity) {
                		documentLineAmount = (requirementQuantity.subtract(ceeaExecutedQuantity))
                            .multiply(requirementLine.getNotaxPrice() == null ? BigDecimal.ZERO : requirementLine.getNotaxPrice()).toString();
                    
                	}
                    //kuangzm ?????????????????????????????????  ??????????????????
                    if (null != ceeaTotalBudget && null != requirementQuantity && null != ceeaExecutedQuantity) {
                    	ceeaTotalBudget = ceeaTotalBudget.subtract((requirementQuantity.subtract(ceeaExecutedQuantity))
                            .multiply(requirementLine.getNotaxPrice() == null ? BigDecimal.ZERO : requirementLine.getNotaxPrice()));
                    }
                    //???????????????
                    convertRequirementLine(loginAppUser, requirementHead, lineList, requirementLine, documentLineAmount);
//                }
            }

            //?????????????????????
            requirementHead.setCeeaTotalBudget(ceeaTotalBudget);
            requirementHeadMapper.updateById(requirementHead);

            fsscResult = new FSSCResult();
            fsscResult.setCode(FSSCResponseCode.SUCCESS.getCode());
            //kuangzm ????????????????????????
            //???????????????????????????,??????????????????
//            if (CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode().equals(requirementHead.getCategoryCode())) {
//                fsscResult = applyRelease(loginAppUser, requirementHead, bgtCheckReqParamDto, lineList);
//            }
//            if (FSSCResponseCode.ERROR.getCode().equals(fsscResult.getCode())) {
//                throw new BaseException(LocaleHandler.getLocaleMsg(fsscResult.getMsg()));
//            }
        }
        return fsscResult;
    }

    private void checkBeforeCancel(RequirementHead requirementHead, RequirementLine requirementLine) {
        String applyStatus = requirementLine.getApplyStatus();
        String auditStatus = requirementHead.getAuditStatus();
        if (!RequirementApproveStatus.APPROVED.getValue().equals(auditStatus)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,?????????????????????,?????????!"));
        }
//		if (!RequirementApplyStatus.RETURNING.getValue().equals(applyStatus)) {
//			throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????"));
//		}
        if (StringUtils.isNotBlank(requirementLine.getFollowFormCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????,????????????????????????,?????????!"));
        }
    }

    public FSSCResult applyRelease(LoginAppUser loginAppUser, RequirementHead requirementHead, BgtCheckReqParamDto bgtCheckReqParamDto, List<Line> lineList) {
        FSSCResult fsscResult;//??????????????????????????????
        setBgtCheckReqParamDto(loginAppUser, requirementHead, bgtCheckReqParamDto, lineList);
        //??????????????????
        fsscResult = iFSSCReqService.applyRelease(bgtCheckReqParamDto);
        log.info("??????????????????:===============================================>" + JsonUtil.entityToJsonStr(fsscResult));
        if ("500".equals(fsscResult.getCode())) {
            throw new BaseException(fsscResult.getMsg());
        }
        return fsscResult;
    }

    /**
     * 1.?????????????????????
     * todo 2.??????????????????????????????????????????????????????????????????
     *
     * @param requirementLineUpdateDTO
     * @return
     */
    @Override
    @Transactional
    public void ceeaUpdateNum(RequirementLineUpdateDTO requirementLineUpdateDTO) {
        checkIfRequirementLineUpdate(requirementLineUpdateDTO);

        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        RequirementLine requirementLine = iRequirementLineService.getById(requirementLineUpdateDTO.getRequirementLineId());
        RequirementHead requirementHead = iRequirementHeadService.getById(requirementLine.getRequirementHeadId());

        Assert.notNull(requirementHead, LocaleHandler.getLocaleMsg("??????????????????????????? requirementHeadId = " + requirementLineUpdateDTO.getRequirementHeadId()));
        if (requirementLine.getCeeaFirstQuantity() == null) {
            /*????????????*/
            BigDecimal requirementQuantity = requirementLine.getRequirementQuantity();
            iRequirementLineService.updateById(
                    new RequirementLine()
                            .setRequirementLineId(requirementLineUpdateDTO.getRequirementLineId())
                            .setCeeaFirstQuantity(requirementQuantity)
                            .setRequirementQuantity(requirementLineUpdateDTO.getThisUpdateNum()) //????????????
                            .setOrderQuantity(requirementLineUpdateDTO.getThisUpdateNum().subtract(requirementLine.getCeeaExecutedQuantity())) //???????????????
                            .setTotalAmount(requirementLineUpdateDTO.getThisUpdateNum().multiply(requirementLine.getNotaxPrice())) //????????????
            );
            //?????????????????????
            QueryWrapper<RequirementLine> wrapper = new QueryWrapper<>();
            wrapper.eq("REQUIREMENT_HEAD_ID", requirementHead.getRequirementHeadId());
            List<RequirementLine> requirementLineList = iRequirementLineService.list(wrapper);

            //????????????
            BigDecimal totalBudget = BigDecimal.ZERO;
            for (RequirementLine r : requirementLineList) {
                totalBudget = totalBudget.add(r.getTotalAmount());
            }
            //??????????????????
            iRequirementHeadService.updateById(
                    new RequirementHead()
                            .setRequirementHeadId(requirementHead.getRequirementHeadId())
                            .setCeeaTotalBudget(totalBudget)
            );

        } else {
            /*?????????*/
            iRequirementLineService.updateById(
                    new RequirementLine()
                            .setRequirementLineId(requirementLineUpdateDTO.getRequirementLineId())
                            .setRequirementQuantity(requirementLineUpdateDTO.getThisUpdateNum()) //????????????
                            .setOrderQuantity(requirementLineUpdateDTO.getThisUpdateNum().subtract(requirementLine.getCeeaExecutedQuantity())) //???????????????
                            .setTotalAmount(requirementLineUpdateDTO.getThisUpdateNum().multiply(requirementLine.getNotaxPrice())) //????????????
            );
            //?????????????????????
            QueryWrapper<RequirementLine> wrapper = new QueryWrapper<>();
            wrapper.eq("REQUIREMENT_HEAD_ID", requirementHead.getRequirementHeadId());
            List<RequirementLine> requirementLineList = iRequirementLineService.list(wrapper);

            //????????????
            BigDecimal totalBudget = BigDecimal.ZERO;
            for (RequirementLine r : requirementLineList) {
                totalBudget = totalBudget.add(r.getTotalAmount());
            }
            //??????????????????
            iRequirementHeadService.updateById(
                    new RequirementHead()
                            .setRequirementHeadId(requirementHead.getRequirementHeadId())
                            .setCeeaTotalBudget(totalBudget)
            );

        }
        /*PurchaseCategory purchaseCategory = baseClient.queryMaxLevelCategory(new PurchaseCategory().setCategoryId(requirementLine.getCategoryId()));*/
        if (StringUtils.isBlank(requirementHead.getCategoryCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????,requirementHeadId = " + requirementHead.getRequirementHeadId()));
        }
//        if (CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode().equals(requirementHead.getCategoryCode())) {
//            BgtCheckReqParamDto bgtCheckReqParamDto = new BgtCheckReqParamDto();
//            bgtCheckReqParamDto.setPriKey(StringUtil.StringValue(requirementHead.getRequirementHeadId()));  //???id
//            bgtCheckReqParamDto.setSourceSystem("SRM");
//            bgtCheckReqParamDto.setGroupCode("LGi");
//            bgtCheckReqParamDto.setTemplateCode("Budget2020");
//            bgtCheckReqParamDto.setDocumentNum(requirementHead.getRequirementHeadNum());
//            bgtCheckReqParamDto.setBudgetUseStatus("P");
//            bgtCheckReqParamDto.setDocumentCreateBy(loginAppUser.getUsername());
//            bgtCheckReqParamDto.setTransferTime(DateUtils.format(new Date(), DateUtils.DATE_FORMAT_19));
//            List<Line> lineList = new ArrayList<>();
//            lineList.add(
//                    new Line().setLineId(StringUtil.StringValue(requirementLine.getRequirementLineId()))
//                            .setDocumentLineNum(StringUtil.StringValue(requirementHead.getRequirementHeadId()) + StringUtil.StringValue(requirementLine.getRequirementLineId()))
//                            .setDocumentLineAmount(StringUtil.StringValue(requirementLine.getRequirementQuantity().subtract(requirementLineUpdateDTO.getThisUpdateNum())))
//                            .setTemplateCode("Budget2020")
//                            .setSegment29(loginAppUser.getCeeaDeptId())
//                            .setSegment30(requirementLine.getCeeaBusinessSmallCode())
//            );
//            bgtCheckReqParamDto.setLineList(lineList);
//            FSSCResult fsscResult = iFSSCReqService.applyFreeze(bgtCheckReqParamDto);
//            if (fsscResult.getCode().equals("500")) {
//                throw new BaseException(LocaleHandler.getLocaleMsg(fsscResult.getMsg()));
//            }
//        }
    }


    /**
     * ??????????????????-????????????????????????
     * ????????????????????????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????
     * ???????????????????????????????????????
     * ?????????????????????0?????????????????????????????????
     * ????????????????????????????????????????????? ????????????????????????????????????????????????????????????????????????
     * <p>
     * ?????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????
     * ?????????????????????0
     * ??????????????????????????????
     * <p>
     * ?????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????
     * ???????????????????????????????????????????????????/?????????????????????????????????????????????????????????????????????????????????
     * ????????????????????????????????????
     * <p>
     * ?????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????
     * ???????????????????????????????????????????????????/?????????????????????????????????????????????????????????????????????????????????
     * ????????????????????????????????????
     * 2020-11-5??????
     * ????????????????????????????????? - ????????????????????? todo
     *
     * @param params
     * @return
     */
    @Override
    public PageInfo<RequirementLineVO> listPageForOrder(RequirermentLineQueryDTO params) {
        Assert.notNull(params.getVendorId(), LocaleHandler.getLocaleMsg("?????????id????????????"));
        Assert.notNull(params.getOrgId(), LocaleHandler.getLocaleMsg("????????????id????????????"));
        if (StringUtil.isEmpty(params.getPurchaseType())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
        }
        /*???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????*/
        if (OrderTypeEnum.ZERO_PRICE.getValue().equals(params.getPurchaseType())) {
            params.setPurchaseTypeList(new ArrayList<String>() {{
                add(OrderTypeEnum.NORMAL.getValue());
                add(OrderTypeEnum.APPOINT.getValue());
                add(OrderTypeEnum.DEVELOP.getValue());
                add(OrderTypeEnum.URGENT.getValue());
            }});
        }
        /*???????????????????????????????????????????????????????????????*/
        if (OrderTypeEnum.CONVENIENT.getValue().equals(params.getPurchaseType())) {
            params.setPurchaseTypeList(new ArrayList<String>() {{
                add(OrderTypeEnum.NORMAL.getValue());
                add(OrderTypeEnum.URGENT.getValue());
                add(OrderTypeEnum.DEVELOP.getValue());
                add(OrderTypeEnum.ZERO_PRICE.getValue());
                add(OrderTypeEnum.CONVENIENT.getValue());
                add(OrderTypeEnum.CONSIGNMENT.getValue());
                add(OrderTypeEnum.SERVICE.getValue());
                add(OrderTypeEnum.APPOINT.getValue());
                add(OrderTypeEnum.OUT.getValue());
            }});
        }

        /*????????????????????????limit??????,??????????????????*/
        Integer pageSize = params.getPageSize();
        Integer pageNum = params.getPageNum();
        params.setPageNum(1);
        params.setPageSize(10000);

        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        params.setCeeaPerformUserId(loginAppUser.getUserId());  //???????????????????????????id
        params.setIfHold(YesOrNo.NO.getValue());    //????????????????????? ????????????????????????
        List<RequirementLine> requirementLineList = requirementLineMapper.ceeaListForOrder(params);
        List<RequirementLineVO> result = new ArrayList<>();
        /*??????????????????????????????????????????id?????????*/
        List<PriceLibrary> priceLibraryAll = inqClient.listAllEffective(new PriceLibrary().setVendorId(params.getVendorId()));
        /*????????????????????????????????????????????????????????????????????????*/
        List<OrgCategory> orgCategoryList = supplierClient.getOrgCategoryByOrgCategory(new OrgCategory().setOrgId(params.getOrgId()).setCompanyId(params.getVendorId()));
        /*?????????????????????????????????????????????id?????????*/
        List<Long> orgIds = new ArrayList<>();
        orgIds.add(params.getOrgId());
        List<ContractVo> contractVoList = contractClient.listAllEffectiveCM(new ContractItemDto().setMaterialIds(orgIds));
        //?????????????????????
        List<Long> controlHeadIds = contractVoList.stream().map(c -> c.getContractHeadId()).collect(Collectors.toList());
        List<ContractPartner> contractPartnerList = contractClient.listAllEffectiveCP(controlHeadIds);

        for (int i = 0; i < requirementLineList.size(); i++) {
            RequirementLine item = requirementLineList.get(i);
            /*??????????????????0?????????*/
            if (item.getOrderQuantity() == null || item.getOrderQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            /*????????????????????????*/
            Assert.notNull(item.getMaterialName(), LocaleHandler.getLocaleMsg("requirementLineId??????" + item.getRequirementLineId() + "materialName??????"));
            Assert.notNull(item.getMaterialCode(), LocaleHandler.getLocaleMsg("requirementLineId??????" + item.getRequirementLineId() + "materialCode??????"));
            Assert.notNull(item.getOrgId(), LocaleHandler.getLocaleMsg("requirementLineId??????" + item.getRequirementLineId() + "orgId??????"));
            Assert.notNull(item.getCategoryId(), LocaleHandler.getLocaleMsg("requirementLineId??????" + item.getRequirementLineId() + "orgId??????"));

            /*?????????????????????????????????????????? - ????????????????????????????????????????????????????????????????????????????????????*/
            OrgCategory orgCategoryItem = null;
            for (OrgCategory orgCategory : orgCategoryList) {
                if (null != orgCategory
                        && null != item.getOrgId() && Objects.equals(orgCategory.getOrgId(), item.getOrgId())
                        && Objects.equals(orgCategory.getCompanyId(), params.getVendorId())
                        && null != item.getCategoryId() && Objects.equals(orgCategory.getCategoryId(), item.getCategoryId())
                        && null != orgCategory.getServiceStatus()
                        && (orgCategory.getServiceStatus().equals(CategoryStatus.VERIFY.name()) || orgCategory.getServiceStatus().equals(CategoryStatus.ONE_TIME.name()) || orgCategory.getServiceStatus().equals(CategoryStatus.GREEN.name()) || orgCategory.getServiceStatus().equals(CategoryStatus.YELLOW.name()))
                ) {
                    orgCategoryItem = orgCategory;
                    break;
                }
            }
            if (orgCategoryItem == null) {
                log.info("??????????????????" + JsonUtil.entityToJsonStr(item) + " ??????????????????????????????");
                continue;
            }
            log.info("materialCode = " + item.getMaterialCode() + "  materialName = " + item.getMaterialName() + "  vendorName = " + item.getVendorName() + "????????????????????????????????????" + JsonUtil.entityToJsonStr(orgCategoryItem));

            if (params.getPurchaseType().equals(OrderTypeEnum.ZERO_PRICE.getValue()) || OrderTypeEnum.CONVENIENT.getValue().equals(params.getPurchaseType())) {
                /*??????????????? - ????????????????????????????????? ???????????????????????????*/
                RequirementLineVO requirementLineVO = new RequirementLineVO();
                BeanUtils.copyProperties(item, requirementLineVO);
                requirementLineVO.setNoTaxPrice(BigDecimal.ZERO);
                requirementLineVO.setTaxPrice(BigDecimal.ZERO);
				/*requirementLineVO.setTaxKey("IN 11");
				requirementLineVO.setTaxRate(new BigDecimal(11.00));
				requirementLineVO.setCurrencyId(7007437216088064L);
				requirementLineVO.setCurrencyCode("CNY");
				requirementLineVO.setCurrencyName("?????????");*/
                result.add(requirementLineVO);
            } else if (params.getPurchaseType().equals(OrderTypeEnum.URGENT.getValue()) || params.getPurchaseType().equals(OrderTypeEnum.DEVELOP.getValue())) {
                /*???????????? ????????????*/
                /*?????????????????????????????? (??????????????????????????????????????????????????????????????????????????????????????????)*/
                RequirementLineVO requirementLineVO = new RequirementLineVO();
                BeanUtils.copyProperties(item, requirementLineVO);
                PriceLibrary priceEntity = null;
                for (PriceLibrary priceLibrary : priceLibraryAll) {
                    //itemDesc,itemCode,orgId,vendorId,orgId,orgnizationId,priceType
                    if (null != priceLibrary && StringUtils.isNotBlank(item.getMaterialName()) && item.getMaterialName().equals(priceLibrary.getItemDesc()) &&
                            StringUtils.isNotBlank(item.getMaterialCode()) && item.getMaterialCode().equals(priceLibrary.getItemCode()) &&
                            item.getOrgId() != null && !ObjectUtils.notEqual(item.getOrgId(), priceLibrary.getCeeaOrgId()) &&
                            Objects.equals(priceLibrary.getVendorId(), params.getVendorId()) &&
                            item.getOrganizationId() != null && Objects.equals(item.getOrganizationId(), priceLibrary.getCeeaOrganizationId()) &&
                            StringUtils.isNotBlank(priceLibrary.getPriceType()) && "STANDARD".equals(priceLibrary.getPriceType())

                    ) {
                        /*??????????????????????????????????????????????????????????????????*/
                        if (priceLibrary.getNotaxPrice() == null ||
                                priceLibrary.getTaxPrice() == null ||
                                StringUtils.isBlank(priceLibrary.getTaxKey()) ||
                                StringUtils.isBlank(priceLibrary.getTaxRate()) ||
                                priceLibrary.getCurrencyId() == null ||
                                StringUtils.isBlank(priceLibrary.getCurrencyCode()) ||
                                StringUtils.isBlank(priceLibrary.getCurrencyName())
                        ) {
                            log.info("requirementHeadNum = " + item.getRequirementHeadNum() + "  rowNum:" + item.getRowNum() + " ???????????????????????????:" + JsonUtil.entityToJsonStr(priceLibrary));
                        } else {
                            priceEntity = priceLibrary;
                            requirementLineVO.setNoTaxPrice(priceEntity.getNotaxPrice());
                            requirementLineVO.setTaxPrice(priceEntity.getTaxPrice());
                            requirementLineVO.setTaxKey(priceEntity.getTaxKey());
                            requirementLineVO.setTaxRate(new BigDecimal(priceEntity.getTaxRate()));
                            requirementLineVO.setCurrencyId(priceEntity.getCurrencyId());
                            requirementLineVO.setCurrencyCode(priceEntity.getCurrencyCode());
                            requirementLineVO.setCurrencyName(priceEntity.getCurrencyName());
                            /*?????????????????? ????????????id???????????????id????????????id????????????????????????*/

//
//							for(ContractVo contractVo:contractVoList){
//								if(null != contractVo && contractVo.getBuId() != null && !ObjectUtils.notEqual(contractVo.getBuId(),item.getOrgId())
//										&& contractVo.getInvId() != null && Objects.equals(contractVo.getInvId(),item.getOrganizationId())
//										&& contractVo.getHeadVendorId() != null && Objects.equals(contractVo.getHeadVendorId(),priceEntity.getVendorId())
//										&& StringUtils.isNotBlank(contractVo.getIsFrameworkAgreement()) && contractVo.getIsFrameworkAgreement().equals("Y")
//								){
//									/*???????????????????????????????????????????????????????????????*/
//									if(StringUtils.isNotBlank(contractVo.getContractCode())){
//										requirementLineVO.setContractCode(contractVo.getContractCode());
//										break;
//									}else{
//										log.info("requirementHeadNum = " + item.getRequirementHeadNum() + "  rowNum:" + item.getRowNum() + " ???????????????????????????:" + JsonUtil.entityToJsonStr(contractVo));
//									}
//								}
//							}

                            if (!StringUtil.isEmpty(priceLibrary.getContractCode())) {
                                requirementLineVO.setContractCode(priceLibrary.getContractCode());
                            } else {
                                //???????????????????????? ??????2020???11???10???
                                for (ContractVo contractVo : contractVoList) {
                                    boolean hitContractFlag =
//                                          null != contractVo &&
//                                          contractVo.getBuId() != null &&
//                                          !ObjectUtils.notEqual(contractVo.getBuId(), item.getOrgId())&&
                                            contractVo.getHeadVendorId() != null &&
//                                          contractVo.getInvId() != null &&
//                                          Objects.equals(contractVo.getInvId(), item.getOrganizationId()) &&
                                                    Objects.equals(contractVo.getHeadVendorId(), priceLibrary.getVendorId()) &&
                                                    StringUtils.isNotBlank(contractVo.getIsFrameworkAgreement()) &&
                                                    StringUtils.isNotBlank(contractVo.getContractCode()) &&
                                                    contractVo.getIsFrameworkAgreement().equals("Y");
                                    log.info("requirementHeadNum = " + item.getRequirementHeadNum() + "  rowNum:" + item.getRowNum() + " ???????????????" + JsonUtil.entityToJsonStr(contractVo));

                                    boolean findContractNum = false;
                                    for (ContractPartner contractPartner : contractPartnerList) {
                                        if (!contractPartner.getContractHeadId().equals(contractVo.getContractHeadId())) {
                                            continue;
                                        }
                                        if (findContractNum) {
                                            break;
                                        }

                                        Long contractPartnerOUId = contractPartner.getOuId();
                                        //todo
                                        if (hitContractFlag && Objects.equals(contractVo.getBuId(), contractPartnerOUId)) {
                                            /*???????????????????????????????????????????????????????????????*/
                                            requirementLineVO.setContractCode(contractVo.getContractCode());
                                            List<ContractVo> list = requirementLineVO.getContractVoList();
                                            if (null == list || list.isEmpty()) {
                                                list = new ArrayList<>();
                                            }
                                            list.add(contractVo);
                                            requirementLineVO.setContractVoList(list);
                                            findContractNum = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
                /*????????????????????????????????????????????????*/
                if (priceEntity == null) {
                    List<ContractVo> contractVos = new ArrayList<>();
                    int index = 0;
                    log.info("requirementHeadNum = " + item.getRequirementHeadNum() + "  rowNum:" + item.getRowNum() + "  materialCode:" + item.getMaterialCode() + " materialName:" + item.getMaterialName() + " ???????????????");
                    /*??????????????????????????? ????????? ?????????????????????????????????????????????????????????????????????????????? ?????????????????? ??????????????????*/
                    for (ContractVo contractVo : contractVoList) {
                        if (null != contractVo && contractVo.getBuId() != null && !ObjectUtils.notEqual(contractVo.getBuId(), item.getOrgId())
                                && contractVo.getMaterialCode() != null && contractVo.getMaterialCode().equals(item.getMaterialCode())
                                && contractVo.getMaterialName() != null && contractVo.getMaterialName().equals(item.getMaterialName())
                                && Objects.equals(contractVo.getHeadVendorId(), params.getVendorId())
                                && contractVo.getInvId() != null && Objects.equals(contractVo.getInvId(), item.getOrganizationId())
                            /*&& StringUtils.isNotBlank(contractVo.getIsFrameworkAgreement()) && contractVo.getIsFrameworkAgreement().equals("N")*/
                        ) {
                            /*???????????????????????????????????????????????????????????????*/
                            if (contractVo.getTaxedPrice() == null ||
                                    StringUtils.isBlank(contractVo.getTaxKey()) ||
                                    contractVo.getTaxRate() == null ||
                                    contractVo.getCurrencyId() == null ||
                                    StringUtils.isBlank(contractVo.getCurrencyCode()) ||
                                    StringUtils.isBlank(contractVo.getCurrencyName())
                            ) {
                                log.info("requirementHeadNum = " + item.getRequirementHeadNum() + "  rowNum:" + item.getRowNum() + " ????????????????????????:" + JsonUtil.entityToJsonStr(contractVo));
                            } else {
                                /*??????????????????*/
                                BigDecimal taxRate = contractVo.getTaxRate().divide(new BigDecimal(100), 8, BigDecimal.ROUND_HALF_UP);
                                BigDecimal untaxedPrice = contractVo.getTaxedPrice().divide((new BigDecimal(1).add(taxRate)), 2, BigDecimal.ROUND_HALF_UP);
                                contractVo.setUntaxedPrice(untaxedPrice);
                                index++;
                                contractVos.add(contractVo);
                            }
                        } else {
                            log.info("requirementHeadNum = " + item.getRequirementHeadNum() + "  rowNum:" + item.getRowNum() + "  materialCode:" + item.getMaterialCode() + " materialName:" + item.getMaterialName() + " ????????????????????????" + JsonUtil.entityToJsonStr(contractVo));
                        }

                    }
                    System.out.println("?????????" + index + "???");
                    if (CollectionUtils.isNotEmpty(contractVos)) {
                        ContractVo contractVo = contractVos.get(0);
                        requirementLineVO.setNoTaxPrice(contractVo.getUntaxedPrice());
                        requirementLineVO.setTaxPrice(contractVo.getTaxedPrice());
                        requirementLineVO.setTaxKey(contractVo.getTaxKey());
                        requirementLineVO.setTaxRate(contractVo.getTaxRate());
                        requirementLineVO.setCurrencyId(contractVo.getCurrencyId());
                        requirementLineVO.setCurrencyCode(contractVo.getCurrencyCode());
                        requirementLineVO.setCurrencyName(contractVo.getCurrencyName());
                        requirementLineVO.setContractCode(contractVo.getContractCode());
                        requirementLineVO.setContractVoList(contractVos);
                    } else {
                        log.info("requirementHeadNum = " + item.getRequirementHeadNum() + "  rowNum:" + item.getRowNum() + "  materialCode:" + item.getMaterialCode() + " materialName:" + item.getMaterialName() + " ?????????????????????");
                    }
                }
                result.add(requirementLineVO);
            } else {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????????????????????????????????????????"));
            }
        }
        PageInfo<RequirementLineVO> requirementLineResult = PageUtil.pagingByFullData(pageNum, pageSize, result);

        /*?????????????????????????????????(??????????????????????????????)*/
		/*List<Long> materialIds = requirementLineResult.getList().stream().map(item -> item.getMaterialId()).collect(Collectors.toList());
		if(CollectionUtils.isNotEmpty(materialIds)){
			List<MaterialItem> materialItemList = baseClient.listMaterialByIdBatch(materialIds);
			if(CollectionUtils.isNotEmpty(materialItemList)){
				for(MaterialItem materialItem:materialItemList){
					if(Objects.isNull(materialItem.getCategoryId())){
						throw new BaseException(LocaleHandler.getLocaleMsg("[" + materialItem.getMaterialCode() + "][" + materialItem.getMaterialName() + "]???????????????????????????????????????"));
					}
				}
			}
		}*/

        /*??????????????????*/
        List<Long> materialIds = requirementLineResult.getList().stream().map(item -> item.getMaterialId()).collect(Collectors.toList());
        List<MaterialMaxCategoryVO> materialMaxCategoryVOList = baseClient.queryCategoryMaxCodeByMaterialIds(materialIds);
        Map<Long, MaterialMaxCategoryVO> map = materialMaxCategoryVOList.stream().collect(Collectors.toMap(e -> e.getMaterialId(), e -> e));
        for (int i = 0; i < requirementLineResult.getList().size(); i++) {
            RequirementLineVO requirementLineVO = requirementLineResult.getList().get(i);
            MaterialMaxCategoryVO materialMaxCategoryVO = map.get(requirementLineVO.getMaterialId());
            if (materialMaxCategoryVO != null) {
                requirementLineVO.setBigCategoryId(materialMaxCategoryVO.getCategoryId());
                requirementLineVO.setBigCategoryCode(materialMaxCategoryVO.getCategoryCode());
            }
        }

        /*??????????????????*/
        for (int i = 0; i < requirementLineResult.getList().size(); i++) {
            RequirementLineVO requirementLineVO = requirementLineResult.getList().get(i);
            List<PurchaseTax> purchaseTaxList = baseClient.queryTaxByItemForOrder(requirementLineVO.getMaterialId());
            requirementLineVO.setPurchaseTaxList(purchaseTaxList);
        }

        /*??????????????????*/
        for (int i = 0; i < requirementLineResult.getList().size(); i++) {
            RequirementLineVO requirementLineVO = requirementLineResult.getList().get(i);
            requirementLineVO.setCeeaPriceSourceType(PriceSourceTypeEnum.MANUAL.getValue());
        }
        return requirementLineResult;
    }


    private void checkIfRequirementLineUpdate(RequirementLineUpdateDTO requirementLineUpdateDTO) {
        RequirementLine requirementLine = iRequirementLineService.getById(requirementLineUpdateDTO.getRequirementLineId());
        Assert.notNull(requirementLine, LocaleHandler.getLocaleMsg("???????????????????????????"));
        RequirementHead requirementHead = iRequirementHeadService.getById(requirementLine.getRequirementHeadId());
        Assert.notNull(requirementHead, LocaleHandler.getLocaleMsg("????????????????????????"));
        if (!requirementHead.getAuditStatus().equals(RequirementApproveStatus.APPROVED.getValue())) {
            Assert.notNull(null, LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        }
        if (requirementLineUpdateDTO.getThisUpdateNum().compareTo(requirementLine.getRequirementQuantity()) == 1) {
            Assert.notNull(null, LocaleHandler.getLocaleMsg("?????????????????????????????????????????????"));
        }
        if (requirementLineUpdateDTO.getThisUpdateNum().compareTo(requirementLine.getCeeaExecutedQuantity()) == -1) {
            Assert.notNull(null, LocaleHandler.getLocaleMsg("?????????????????????????????????????????????"));
        }
        /*??????????????????????????????????????????????????????????????????*/
        if (StringUtils.isNoneEmpty(requirementLine.getFollowFormCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????????????????"));
        }
    }

    private void setBgtCheckReqParamDto(LoginAppUser loginAppUser, RequirementHead requirementHead, BgtCheckReqParamDto bgtCheckReqParamDto, List<Line> lineList) {
        bgtCheckReqParamDto.setPriKey(requirementHead.getRequirementHeadId().toString());
        bgtCheckReqParamDto.setSourceSystem(DataSourceEnum.NSRM_SYS.getKey());
        bgtCheckReqParamDto.setGroupCode("LGi");
        bgtCheckReqParamDto.setTemplateCode("Budget2020");
        bgtCheckReqParamDto.setDocumentNum(requirementHead.getRequirementHeadNum());
        bgtCheckReqParamDto.setBudgetUseStatus(BudgetUseStatus.P.name());
        bgtCheckReqParamDto.setDocumentCreateBy(StringUtil.StringValue(loginAppUser.getCeeaEmpNo()));
        bgtCheckReqParamDto.setLineList(lineList);
        bgtCheckReqParamDto.setTransferTime(new Date().toString());
    }

    private void convertRequirementLine(LoginAppUser loginAppUser, RequirementHead requirementHead, List<Line> lineList, RequirementLine requirementLine, String documentLineAmount) {
        Line line = new Line();
        line.setLineId(requirementLine.getRequirementLineId().toString());
        line.setDocumentLineNum(requirementHead.getRequirementHeadId().toString() + requirementLine.getRequirementLineId().toString());
        line.setTemplateCode("Budget2020");
        line.setDocumentLineAmount(documentLineAmount);
        line.setSegment29(loginAppUser.getCeeaDeptId());
        line.setSegment30(requirementHead.getCeeaBusinessSmallCode());
        lineList.add(line);
    }

    @Override
    public void importMaterialItemModelDownload(HttpServletResponse response) throws Exception {
        String fileName = "????????????????????????????????????";
        List<RequirementLineImport> requirementLineImports = new ArrayList<>();
        OutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        List<Integer> column = Arrays.asList(0, 1);
        HashMap<Integer, String> titleConfig = new HashMap();
        titleConfig.put(0, "???????????????ERP????????????????????????");
        titleConfig.put(1, "????????????????????????????????????????????????");
        titleConfig.put(2, "??????????????????????????????");
        titleConfig.put(3, "??????????????????????????????YYYY-MM-DD???YYYY/MM/DD");
        titleConfig.put(4, "???????????????????????????????????????????????????????????????");
        titleConfig.put(5, "???????????????????????????????????????????????????????????????");
        titleConfig.put(6, "?????????");
        titleConfig.put(7, "?????????");
        //???????????????
        HashMap<Integer, String[]> dropDownMap = new HashMap<>();
        // ???????????????
        String requestManCode = "DMAND_LINE_REQUEST";    //?????????????????????
        List<String> requestList = baseClient.listAllByDictCode(requestManCode)
                .stream().map(DictItemDTO::getDictItemName).distinct().collect(Collectors.toList());

        // ?????????sheel??????
        ExcelWriter excelWriter = EasyExcel.write(outputStream).build();
        // ????????????
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        // ???????????????
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        // ?????????????????????
        WriteSheet test1 = EasyExcel.writerSheet(0, fileName).head(RequirementLineImport.class).build();
        List<List<String>> head = new ArrayList<>();
        HorizontalCellStyleStrategy horizontalCellStyleStrategy = new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
        WriteSheet test2 = EasyExcel.writerSheet(1, "???????????????").head(head).
                registerWriteHandler(horizontalCellStyleStrategy).build();

        List<List<String>> requestManData = new ArrayList<>();
        for (int i = 0; i < requestList.size(); i++) {
            List<String> temp = new ArrayList<>();
            temp.add(requestList.get(i));
            requestManData.add(temp);
        }
        excelWriter.write(requirementLineImports, test1).write(requestManData, test2);
        excelWriter.finish();

//		EasyExcelUtil.writeExcelWithModel(outputStream, requirementLineImports, RequirementLineImport.class, fileName ,titleHandler);
    }


    @Override
    @Transactional
    public Map<String, Object> importExcel(String requirementHeadId, MultipartFile file, Fileupload fileupload) throws Exception {
        RequirementHead requirementHead = iRequirementHeadService.getById(requirementHeadId);
        //?????????????????????????????????????????????
        EasyExcelUtil.checkParam(file, fileupload);
        //????????????????????????
        checkImportParam(requirementHead);
        // ??????excel???????????????
        List<RequirementLineImport> requirementLineImports = this.readData(file);
        List<RequirementLine> requirementLines = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        if (CollectionUtils.isNotEmpty(requirementLineImports)) {
            Long d1 = System.currentTimeMillis();
            boolean errorFlag = importCheckData(requirementHead, requirementLineImports, requirementLines);
            log.info("==============????????????????????????:" + (System.currentTimeMillis() - d1) + "ms===============");
            if (errorFlag) {
                // ?????????
                fileupload.setFileSourceName("????????????????????????????????????");
                Fileupload fileuploadResult = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                        requirementLineImports, RequirementLineImport.class, file.getName(), file.getOriginalFilename(), file.getContentType());
                result.put("status", YesOrNo.NO.getValue());
                result.put("message", "error");
                result.put("fileuploadId", fileuploadResult.getFileuploadId());
                result.put("fileName", fileuploadResult.getFileSourceName());
                return result;
            }
            // ????????????
            if (CollectionUtils.isNotEmpty(requirementLines)) {
                for (RequirementLine requirementLine : requirementLines) {
                    save(requirementLine);
                }
            }
        }
        result.put("status", YesOrNo.YES.getValue());
        result.put("message", "success");
        return result;
    }


    @Override
    @Transactional
    public Map<String, Object> importExcelNew(RequirementHead requirementHead, MultipartFile file, Fileupload fileupload) throws Exception {
        //?????????????????????????????????????????????
        EasyExcelUtil.checkParam(file, fileupload);

        // ??????excel???????????????
        List<RequirementLineImport> requirementLineImports = this.readData(file);
        List<RequirementLine> requirementLines = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        if (CollectionUtils.isNotEmpty(requirementLineImports)) {
            Long d1 = System.currentTimeMillis();
            boolean errorFlag = importCheckData(requirementHead, requirementLineImports, requirementLines);
            log.info("==============????????????????????????:" + (System.currentTimeMillis() - d1) + "ms===============");
            if (errorFlag) {
                // ?????????
                fileupload.setFileSourceName("????????????????????????????????????");
                Fileupload fileuploadResult = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                        requirementLineImports, RequirementLineImport.class, file.getName(), file.getOriginalFilename(), file.getContentType());
                result.put("status", YesOrNo.NO.getValue());
                result.put("message", "error");
                result.put("fileuploadId", fileuploadResult.getFileuploadId());
                result.put("fileName", fileuploadResult.getFileSourceName());
                return result;
            }
            // ????????????
//            if (CollectionUtils.isNotEmpty(requirementLines)) {
//                for (RequirementLine requirementLine : requirementLines) {
//                    save(requirementLine);
//                }
//            }
        }
        result.put("status", YesOrNo.YES.getValue());
        result.put("data", requirementLines);
        result.put("message", "success");
        return result;
    }

    /**
     * ????????????????????????
     *
     * @param requirementHead
     */
    private void checkImportParam(RequirementHead requirementHead) {
        Assert.notNull(requirementHead.getOrgId(), "????????????ID????????????");
        Assert.notNull(requirementHead.getOrganizationId(), "????????????ID????????????");
        Assert.notNull(requirementHead.getCategoryId(), "????????????ID????????????");
        if (CategoryEnum.BIG_CATEGORY_SERVER.getCategoryCode().equals(requirementHead.getCategoryCode())) {
            Assert.notNull(requirementHead.getCeeaBusinessSmallCode(), "???????????????????????????????????????????????????");
        }
    }


    /**
     * ????????????-????????????-????????????
     *
     * @param requirementHead        ????????????????????????
     * @param requirementLineImports excel??????
     * @param requirementLines       ??????????????????????????????
     * @return
     */
    public boolean importCheckData(RequirementHead requirementHead,
                                   List<RequirementLineImport> requirementLineImports,
                                   List<RequirementLine> requirementLines) {
        boolean errorFlag = false;
        int roundSize = 1000;
        int importSize = requirementLineImports.size();
        int round = importSize / roundSize;
        if (importSize % roundSize > 0) {
            round += 1;
        }
        round = round == 0 ? 1 : round;

        Set<Long> itCategoryIds = new HashSet<>();
        boolean asITGroup = loadITGroupCategoryIds(itCategoryIds);
        //????????????????????????
        Organization organization = baseClient.get(requirementHead.getOrganizationId());
        //???????????????
        String requestManCode = "DMAND_LINE_REQUEST";    //?????????????????????
        Set<String> requestManSet = baseClient.listAllByDictCode(requestManCode)
                .stream().map(DictItemDTO::getDictItemName).collect(Collectors.toSet());

        for (int i = 0; i < round; i++) {
            int startIndex = i * roundSize;
            int endIndex = (i + 1) * roundSize > importSize ? importSize : (i + 1) * roundSize;
            List<RequirementLineImport> curLineImport = requirementLineImports.subList(startIndex, endIndex);

            //?????????????????????
            List<String> companyCodes = curLineImport.stream().map(o -> o.getVendorCode())
                    .collect(Collectors.toList());
            List<CompanyInfo> companyInfoList = supplierClient.listCompanyByCodes(companyCodes);
            Map<String, CompanyInfo> companyInfoMap = companyInfoList.stream().collect(
                    Collectors.toMap(
                            c -> c.getCompanyCode(),
                            c -> c,
                            (c1, c2) -> c1
                    )
            );
            //??????????????????
            List<String> materialCodes = curLineImport.stream().map(o -> o.getMaterialCode())
                    .collect(Collectors.toList());
            ItemCodeUserPurchaseDto purchaseDto = new ItemCodeUserPurchaseDto();
            purchaseDto.setOrgId(requirementHead.getOrgId());
            purchaseDto.setInvId(requirementHead.getOrganizationId());
            purchaseDto.setItemCodes(materialCodes);
            Map<String, String> userPurchaseMap = baseClient.queryItemIdUserPurchase(purchaseDto);
            List<MaterialItem> materialItemList = baseClient.listMaterialByCodeBatch(materialCodes);
            Map<String, MaterialItem> materialItemMap = materialItemList.stream().collect(
                    Collectors.toMap(
                            m -> m.getMaterialCode(),
                            m -> m,
                            (m1, m2) -> m1
                    )
            );
            //????????????????????????
            List<Long> materialIds = materialItemList.stream().map(o -> o.getMaterialId()).collect(Collectors.toList());
            Map<String, MaterialOrg> materialOrgMap = baseClient.listMaterialOrgByMaterialIds(materialIds)
                    .stream().collect(
                            Collectors.toMap(
                                    m -> new StringBuilder().append(m.getMaterialId()).append(m.getOrganizationId()).toString(),
                                    m -> m,
                                    (m1, m2) -> m1
                            )
                    );
            //??????????????????
            NetPriceQueryDTO priceDto = new NetPriceQueryDTO().setOrganizationId(requirementHead.getOrganizationId());
            Map<String, PriceLibrary> priceLibraryMap = inqClient.listPriceLibrary(priceDto).stream().collect(
                    Collectors.toMap(
                            p -> new StringBuilder()
                                    .append(p.getItemCode())
                                    .append(p.getCeeaOrgId())
                                    .append(p.getCeeaOrganizationId()).toString(),
                            p -> p,
                            (p1, p2) -> p1
                    )
            );
            //????????????????????????
            List<Long> companyIds = companyInfoList.stream().map(o -> o.getCompanyId()).collect(Collectors.toList());
            Map<String, OrgCategory> orgCategoryMap = supplierClient.listOrgCategoryByCompanyIds(companyIds).stream().collect(
                    Collectors.toMap(
                            o -> new StringBuilder().append(o.getCategoryId()).append(o.getCompanyId()).toString(),
                            o -> o,
                            (o1, o2) -> o1
                    )
            );

            for (int row = 0; row < curLineImport.size(); row++) {
                RequirementLineImport requirementLineImport = curLineImport.get(row);
                RequirementLine requirementLine = new RequirementLine();
                boolean rowErrorFlag = false;
                StringBuffer errorMsg = new StringBuffer();
                BeanUtils.copyProperties(requirementHead, requirementLine);
                //??????????????????
                if (StringUtils.isBlank(requirementLineImport.getMaterialCode())) {
                    errorFlag = true;
                    rowErrorFlag = true;
                    errorMsg.append("????????????????????????;");
                }
                if (Objects.isNull(requirementLineImport.getOrderQuantity())) {
                    errorFlag = true;
                    rowErrorFlag = true;
                    errorMsg.append("????????????????????????;");
                }
                if (Objects.isNull(requirementLineImport.getRequirementDate()) ||
                        StringUtils.isBlank(requirementLineImport.getRequirementDate())) {
                    errorFlag = true;
                    rowErrorFlag = true;
                    errorMsg.append("????????????????????????;");
                }
                // ????????????
                String materialCode = requirementLineImport.getMaterialCode();
                if (StringUtil.notEmpty(materialCode)) {
                    materialCode = materialCode.trim();
                    String purchase = userPurchaseMap.get(materialCode);
                    if (!YesOrNo.YES.getValue().equals(purchase)) {
                        errorFlag = true;
                        rowErrorFlag = true;
                        errorMsg.append("???????????????????????????????????????????????????;");
                    }
                }

                if (rowErrorFlag) {
                    requirementLineImport.setErrorMsg(errorMsg.toString());
                    continue;
                }

                MaterialItem materialItem = materialItemMap.get(requirementLineImport.getMaterialCode());
                if (StringUtil.isEmpty(materialItem.getCategoryId())) {
                    errorFlag = true;
                    errorMsg.append("???????????????SRM???????????????ERP??????;");
                    requirementLineImport.setErrorMsg(errorMsg.toString());
                    continue;
                }
                if (Objects.isNull(materialItem)) {
                    errorFlag = true;
                    errorMsg.append("?????????????????????;");
                    requirementLineImport.setErrorMsg(errorMsg.toString());
                    continue;
                }

                //?????????
                if (StringUtils.isNotBlank(requirementLineImport.getDmandLineRequest()) &&
                        !requestManSet.contains(requirementLineImport.getDmandLineRequest())) {
                    errorFlag = true;
                    errorMsg.append("?????????????????????");
                }

                //????????????????????? ???????????????(????????????)???????????????????????????
                if (materialItem.getStruct().indexOf(requirementHead.getCategoryId().toString()) < 0) {
                    errorFlag = true;
                    errorMsg.append("??????????????????????????????????????????????????????????????????");
                }
                //IT???????????????it???
                if (asITGroup && !itCategoryIds.contains(materialItem.getCategoryId())) {
                    errorFlag = true;
                    errorMsg.append("???????????????[??????-IT]??????????????????IT?????????");
                }
                //???it???????????????it???
                if (!asITGroup && itCategoryIds.contains(materialItem.getCategoryId())) {
                    errorFlag = true;
                    errorMsg.append("?????????????????????[??????-IT]???????????????IT?????????");
                }
                // ????????????????????????????????????
                requirementLine.setCategoryId(materialItem.getCategoryId());
                requirementLine.setCategoryCode(materialItem.getCategoryCode());
                requirementLine.setCategoryName(materialItem.getCategoryName());
                requirementLine.setCeeaIfDirectory(YesOrNo.NO.getValue());
                MaterialOrg materialOrg = materialOrgMap.get(new StringBuilder().append(materialItem.getMaterialId()).append(requirementHead.getOrganizationId()).toString());
                boolean materialInOrgFlag = Objects.nonNull(materialOrg);
                if (materialInOrgFlag) {
                    boolean noPurchase = YesOrNo.NO.getValue().equals(materialOrg.getUserPurchase());
                    if (noPurchase) {
                        errorFlag = true;
                        errorMsg.append("??????????????????????????????;");
                    }

                    //??????????????????+????????????+????????????+????????????+???????????????????????????+???????????????????????????????????????????????????
                    // ?????????????????????????????????????????????????????????????????????????????????--2020-10-30?????????????????????
                    PriceLibrary priceParam = priceLibraryMap.get(
                            new StringBuilder().append(materialItem.getMaterialCode())
                                    .append(requirementHead.getOrgId())
                                    .append(requirementHead.getOrganizationId()
                                    ).toString()
                    );
                    if (Objects.nonNull(priceParam)) {

                        requirementLineImport.setVendorCode(priceParam.getVendorCode());
                        requirementLineImport.setNotaxPrice(priceParam.getTaxPrice());
                        requirementLineImport.setVendorName(priceParam.getVendorName());
                    }
                } else {
                    errorFlag = true;
                    errorMsg.append("?????????????????????????????????????????????????????????????????????;");
                }

                //???????????????????????????????????????????????????????????????????????????????????????????????????
                if (StringUtils.isNotBlank(requirementLineImport.getVendorCode())) {
                    CompanyInfo companyInfo = companyInfoMap.get(requirementLineImport.getVendorCode());
                    //supplierClient.getCompanyInfoByParam(new CompanyInfo().setCompanyCode(requirementLineImport.getVendorCode()));
                    AtomicBoolean redCardFlag = new AtomicBoolean(false);
                    if (Objects.nonNull(companyInfo)) {
                        //?????????????????????
                        requirementLineImport.setVendorName(companyInfo.getCompanyName());
                        OrgCategory orgCategory = orgCategoryMap.get(new StringBuilder()
                                .append(materialItem.getCategoryId())
                                .append(companyInfo.getCompanyId()));
                        if (Objects.nonNull(orgCategory) && CategoryStatus.RED.toString().equals(orgCategory.getServiceStatus())) {
                            redCardFlag.set(true);
                        }
                    }
                    if (redCardFlag.get()) {
                        errorFlag = true;
                        errorMsg.append("??????????????????????????????????????????????????????");
                    }
                }

                //????????????????????????
                LocalDate requirementDate = null;
                if (StringUtils.isNotBlank(requirementLineImport.getRequirementDate())) {
                    try {
                        Date date = DateUtil.parseDate(requirementLineImport.getRequirementDate());
                        requirementDate = DateUtil.dateToLocalDate(date);
                    } catch (Exception e) {
                        errorFlag = true;
                        errorMsg.append("???????????????????????????");
                    }
                }
                //????????????????????????????????????
                if (requirementDate.compareTo(LocalDate.now()) < 0) {
                    errorFlag = true;
                    errorMsg.append("????????????????????????????????????");
                }
                //??????????????? ????????????????????????
                //baseClient.getOrganizationByParam(new Organization().setOrganizationId(requirementHead.getOrganizationId()));
                if (Objects.isNull(organization)) {
                    errorFlag = true;
                    errorMsg.append("????????????????????????");
                }
                if (!errorFlag) {
                    requirementLine.setMaterialCode(materialItem.getMaterialCode())
                            .setMaterialId(materialItem.getMaterialId())
                            .setMaterialName(materialItem.getMaterialName())
                            .setApplyStatus(RequirementApplyStatus.UNASSIGNED.getValue())
                            // ????????????
//							.setCeeaDeliveryPlace(Objects.isNull(organization)? "" : organization.getOrganizationSite())
                            .setOrderQuantity(requirementLineImport.getOrderQuantity())
                            .setRequirementQuantity(requirementLineImport.getOrderQuantity())
                            .setUnitCode(materialItem.getUnit())
                            .setUnit(materialItem.getUnitName())
                            .setComments(requirementLineImport.getComments())
                            //?????????????????????
                            .setVendorCode(requirementLineImport.getVendorCode())
                            .setVendorName(requirementLineImport.getVendorName())
                            .setDmandLineRequest(requirementLineImport.getDmandLineRequest())
                            .setRequirementDate(requirementDate);

                    if (Objects.isNull(requirementLineImport.getNotaxPrice())) {
                        requirementLine.setTotalAmount(new BigDecimal(0));
                        requirementLine.setNotaxPrice(new BigDecimal(0));
                    } else {
                        requirementLine.setTotalAmount(requirementLineImport.getNotaxPrice().multiply(requirementLineImport.getOrderQuantity()));
                        requirementLine.setNotaxPrice(requirementLineImport.getNotaxPrice());
                    }
                }
                requirementLineImport.setErrorMsg(errorMsg.toString());
                requirementLines.add(requirementLine);
            }
        }
        return errorFlag;
    }

    private boolean loadITGroupCategoryIds(Set<Long> itCategoryIds) {
        int showBit = 0b00;
        int notShowIT = 0b10;
        int showIT = 0b01;
        List<Role> notShowITRoles = AppUserUtil.getLoginAppUser().getRolePermissions().stream().filter(
                role -> RoleEnum.LONGI_DG.getRoleCode().equals(role.getRoleCode()) ||
                        RoleEnum.LONGI_DD.getRoleCode().equals(role.getRoleCode())
        ).collect(Collectors.toList());
        if (!notShowITRoles.isEmpty()) {
            showBit = showBit | notShowIT;
        }
        //?????????-it????????????it??????
        List<Role> showITRoles = AppUserUtil.getLoginAppUser().getRolePermissions().stream()
                .filter(role -> RoleEnum.LONGI_IT.getRoleCode().equals(role.getRoleCode())).collect(Collectors.toList());
        if (!showITRoles.isEmpty()) {
            showBit = showBit | showIT;
        }
        //it??????
        Map<String, PurchaseCategory> itCategoryData = baseClient.getCategoryByCodes(Arrays.asList(SpecialBusinessConstant.IT_CATEGORY));
        for (String categoryCode : itCategoryData.keySet()) {
            PurchaseCategory category = itCategoryData.get(categoryCode);
            itCategoryIds.add(category.getCategoryId());
        }
        if (showBit == showIT) {
            return true;
        } else if (showBit == notShowIT) {
            return false;
        } else {
            itCategoryIds = new HashSet<>();
            return false;
        }
    }

    /**
     * ??????excel??????
     *
     * @param file
     * @return
     */
    private List<RequirementLineImport> readData(MultipartFile file) {
        List<RequirementLineImport> requirementLineImports = null;
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<RequirementLineImport> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream, listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(RequirementLineImport.class).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            requirementLineImports = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return requirementLineImports;
    }

    @Override
    public List<OrderDetail> getRequirementQuantityForOrderCheck(List<OrderDetail> detailList) {
        for (OrderDetail orderDetail : detailList) {
            if (Objects.nonNull(orderDetail.getCeeaRequirementLineId())) {
                RequirementLine requirementLine = requirementLineMapper.selectById(orderDetail.getCeeaRequirementLineId());
                if (Objects.nonNull(requirementLine)) {
                    orderDetail.setRequirementLineTotalQuantity(requirementLine.getRequirementQuantity());
                }
            }
        }
        return detailList;
    }

    @Override
    public PageInfo<RequirementLine> listPageRequirementChart(RequirementHeadQueryDTO requirementHeadQueryDTO) {
        PageHelper.startPage(requirementHeadQueryDTO.getPageNum(), requirementHeadQueryDTO.getPageSize());
        List<RequirementLine> result = requirementLineMapper.listPageRequirementChart(requirementHeadQueryDTO);

        //???????????????????????? ????????????
        List<Long> requirementLineIds = result.stream().map(item -> item.getRequirementLineId()).collect(Collectors.toList());
        QueryWrapper<SubsequentDocuments> wrapper = new QueryWrapper<>();
        wrapper.in("REQUIREMENT_LINE_ID", requirementLineIds);
        List<SubsequentDocuments> subsequentDocumentsList = subsequentDocumentsService.list(wrapper);
        Map<Long, List<SubsequentDocuments>> subsequentDocumentsMap = getSubsequentDocumentsMap(subsequentDocumentsList);


        //?????????????????? ???????????????
        List<String> subsequentDocumentsNo = subsequentDocumentsList.stream().map(item -> item.getSubsequentDocumentsNumber()).collect(Collectors.toList());
        List<ApprovalHeader> approvalHeaderList = inqClient.listApprovalHeaderBysourceNo(subsequentDocumentsNo);

        //?????????????????? ???????????????-?????????
        List<Long> approvalHeadIds = approvalHeaderList.stream().map(item -> item.getApprovalHeaderId()).collect(Collectors.toList());
        List<ApprovalBiddingItem> approvalBiddingItemList = inqClient.listApprovalBiddingItemByApprovalHeadIds(approvalHeadIds);

        //?????????????????? ?????????
        List<Long> contractHeadIds = approvalBiddingItemList.stream().map(item -> item.getFromContractId()).collect(Collectors.toList());
        List<ContractHead> contractHeadList = contractClient.listContractHeadByContractHeadIds(contractHeadIds);


        for (RequirementLine r : result) {
            //????????????
            if (StringUtils.isNotBlank(r.getApplyStatus())) {
                r.setApplyStatus(RequirementApplyStatus.get(r.getApplyStatus()).getName());
            }
            if (StringUtils.isNotBlank(r.getCeeaPurchaseType())) {
                r.setCeeaPurchaseType(PurchaseType.get(r.getCeeaPurchaseType()).getName());
            }
            if (CollectionUtils.isNotEmpty(subsequentDocumentsMap.get(r.getRequirementLineId()))) {
                List<SubsequentDocuments> documentsList = subsequentDocumentsMap.get(r.getRequirementLineId());

                StringBuffer orderNumber = new StringBuffer();
                StringBuffer sourceNo = new StringBuffer();
                for (SubsequentDocuments subsequentDocuments : documentsList) {
                    if (RelatedDocumentsEnum.PURCHASE.getName().equals(subsequentDocuments.getIsubsequentDocumentssType())) {
                        r.setOrderNumber(orderNumber
                                .append(subsequentDocuments.getSubsequentDocumentsNumber())
                                .append("/")
                                .toString());
                    } else {
                        r.setSourceNo(sourceNo
                                .append(subsequentDocuments.getSubsequentDocumentsNumber())
                                .append("/")
                                .toString()
                        );
                    }
                }
            }
            List<ContractHead> contractHeads = getContracts(r, subsequentDocumentsMap, approvalHeaderList, approvalBiddingItemList, contractHeadList);
            if (CollectionUtils.isNotEmpty(contractHeads)) {
                String contractNo = "";
                String contractCode = "";
                for (ContractHead contractHead : contractHeads) {
                    StringBuffer contractCodeSB = new StringBuffer(contractCode)
                            .append(contractHead.getContractCode())
                            .append("/");
                    contractCode = contractCodeSB.toString();
                    StringBuffer contractNoSB = new StringBuffer(contractNo)
                            .append(contractHead.getContractNo())
                            .append("/");
                    contractNo = contractNoSB.toString();
                }
                r.setContractCode(contractCode);
                r.setContractNo(contractNo);

            }

        }

        return new PageInfo<>(result);
    }

    private Map<Long, List<SubsequentDocuments>> getSubsequentDocumentsMap(List<SubsequentDocuments> subsequentDocumentsList) {
        Map<Long, List<SubsequentDocuments>> result = new HashMap<>();
        for (int i = 0; i < subsequentDocumentsList.size(); i++) {
            SubsequentDocuments subsequentDocuments = subsequentDocumentsList.get(i);
            if (CollectionUtils.isEmpty(result.get(subsequentDocuments.getRequirementLineId()))) {
                result.put(subsequentDocuments.getRequirementLineId(), new LinkedList<SubsequentDocuments>() {{
                    add(subsequentDocuments);
                }});
            } else {
                result.get(subsequentDocuments.getRequirementLineId()).add(subsequentDocuments);
            }
        }
        return result;
    }

    public Map<Long, RequirementHead> getRequirementHeadMap(List<Long> requirementHeadIds) {
        if (CollectionUtils.isEmpty(requirementHeadIds)) {
            return Collections.EMPTY_MAP;
        }
        QueryWrapper<RequirementHead> wrapper = new QueryWrapper<>();
        wrapper.in("REQUIREMENT_HEAD_ID", requirementHeadIds);
        List<RequirementHead> requirementHeadList = iRequirementHeadService.list(wrapper);
        Map<Long, RequirementHead> map = requirementHeadList.stream()
                .collect(Collectors.toMap(item -> item.getRequirementHeadId(), item -> item));
        return map;
    }


    /**
     * ?????? requirementLineId ?????? ?????????????????????????????????
     * ?????? ?????????????????? ?????? ??????????????????????????????
     * ????????????????????? ?????? ????????????????????????
     * ??????????????? ?????? ??????(?????????)
     *
     * @param requirementLine
     * @param subsequentDocumentsMap
     * @param approvalHeaderList
     * @param approvalBiddingItemList
     * @param contractHeadList
     * @return
     */
    public List<ContractHead> getContracts(RequirementLine requirementLine,
                                           Map<Long, List<SubsequentDocuments>> subsequentDocumentsMap,
                                           List<ApprovalHeader> approvalHeaderList,
                                           List<ApprovalBiddingItem> approvalBiddingItemList,
                                           List<ContractHead> contractHeadList) {
        Long requirementLineId = requirementLine.getRequirementLineId();
        //requirementLineId ?????????????????????
        List<SubsequentDocuments> subsequentDocumentsList = subsequentDocumentsMap.get(requirementLineId);
        if (CollectionUtils.isEmpty(subsequentDocumentsList)) {
            return Collections.EMPTY_LIST;
        }

        // requirementLineId ???????????????????????? list1
        List<String> ceeaSourceNo = subsequentDocumentsList.stream().map(item -> item.getSubsequentDocumentsNumber()).collect(Collectors.toList());
        List<ApprovalHeader> list1 = new ArrayList<>();
        for (ApprovalHeader approvalHeader : approvalHeaderList) {
            if (ceeaSourceNo.contains(approvalHeader.getCeeaSourceNo())) {
                list1.add(approvalHeader);
            }
        }
        if (CollectionUtils.isEmpty(list1)) {
            return Collections.EMPTY_LIST;
        }

        //requirementLineId ?????????????????? list2
        List<Long> approvalHeadIds = list1.stream().map(item -> item.getApprovalHeaderId()).collect(Collectors.toList());
        List<ApprovalBiddingItem> list2 = new ArrayList<>();
        for (ApprovalBiddingItem approvalBiddingItem : approvalBiddingItemList) {
            if (approvalHeadIds.contains(approvalBiddingItem.getApprovalHeaderId())) {
                list2.add(approvalBiddingItem);
            }
        }
        if (CollectionUtils.isEmpty(list2)) {
            return Collections.EMPTY_LIST;
        }

        //requirementLineId ??????????????? list3
        List<Long> fromContractIds = list2.stream().map(item -> item.getFromContractId()).collect(Collectors.toList());
        List<ContractHead> list3 = new ArrayList<>();
        for (ContractHead contractHead : contractHeadList) {
            list3.add(contractHead);
        }

        return list3;

    }


}
