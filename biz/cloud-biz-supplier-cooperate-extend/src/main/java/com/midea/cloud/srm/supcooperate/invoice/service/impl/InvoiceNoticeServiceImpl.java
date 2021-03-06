package com.midea.cloud.srm.supcooperate.invoice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.VendorAssesFormStatus;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.neworder.OrderDetailStatus;
import com.midea.cloud.common.enums.pm.po.PurchaseOrderEnum;
import com.midea.cloud.common.enums.pm.ps.InvoiceStatus;
import com.midea.cloud.common.enums.pm.ps.StatementStatusEnum;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.enums.supcooperate.InvoiceNoticeStatus;
import com.midea.cloud.common.enums.supcooperate.OnlineOrNotice;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.common.utils.redis.RedisUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.perf.PerformanceClient;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.dict.entity.DictItem;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.perf.vendorasses.VendorAssesForm;
import com.midea.cloud.srm.model.pm.ps.statement.entity.StatementHead;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.suppliercooperate.invoice.dto.InvoiceDetailDto;
import com.midea.cloud.srm.model.suppliercooperate.invoice.dto.InvoiceNoticeDTO;
import com.midea.cloud.srm.model.suppliercooperate.invoice.dto.InvoiceNoticeQueryDTO;
import com.midea.cloud.srm.model.suppliercooperate.invoice.dto.InvoiceNoticeSaveDTO;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.InvoiceDetail;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.InvoiceNotice;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.InvoicePunish;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.WarehousingReturnDetailRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.WarehousingReturnDetail;
import com.midea.cloud.srm.model.workflow.service.IFlowBusinessCallbackService;
import com.midea.cloud.srm.supcooperate.invoice.mapper.InvoiceNoticeMapper;
import com.midea.cloud.srm.supcooperate.invoice.service.IInvoiceDetailService;
import com.midea.cloud.srm.supcooperate.invoice.service.IInvoiceNoticeService;
import com.midea.cloud.srm.supcooperate.invoice.service.IInvoicePunishService;
import com.midea.cloud.srm.supcooperate.order.service.IOrderDetailService;
import com.midea.cloud.srm.supcooperate.order.service.IWarehousingReturnDetailService;
import com.midea.cloud.srm.supcooperate.statement.service.IStatementHeadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ??????????????? ???????????????
 * </pre>
 *
 * @author chensl26@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-06-27 11:46:26
 *  ????????????:
 * </pre>
 */
@Slf4j
@Service
public class InvoiceNoticeServiceImpl extends ServiceImpl<InvoiceNoticeMapper, InvoiceNotice> implements IInvoiceNoticeService, IFlowBusinessCallbackService {


    @Autowired
    private BaseClient baseClient;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private IInvoiceDetailService iInvoiceDetailService;

    @Autowired
    private IInvoicePunishService iInvoicePunishService;

    @Autowired
    private IStatementHeadService iStatementHeadService;

    @Autowired
    private IWarehousingReturnDetailService iWarehousingReturnDetailService;

    @Autowired
    private IOrderDetailService iOrderDetailService;

    @Autowired
    private PerformanceClient performanceClient;

    @Autowired
    private RedisUtil redisUtil;

    private static final String INVOICE_NOTICE_LOCK = "invoice_notice_lock";

    @Override
    @Transactional
    public Long saveTemporary(InvoiceNoticeSaveDTO invoiceNoticeSaveDTO) {
        checkBeforeSaveTemporary(invoiceNoticeSaveDTO);
        Long invoiceNoticeId = saveOrUpdateInvoiceNoticeSaveDTO(invoiceNoticeSaveDTO, InvoiceNoticeStatus.DRAFT.name());
        return invoiceNoticeId;
    }

    private void checkBeforeSaveTemporary(InvoiceNoticeSaveDTO invoiceNoticeSaveDTO) {
        InvoiceNotice invoiceNotice = invoiceNoticeSaveDTO.getInvoiceNotice();
        Assert.notNull(invoiceNotice, LocaleHandler.getLocaleMsg("invoiceNotice????????????"));
        InvoiceNotice dbInvoiceNotice = this.getById(invoiceNotice.getInvoiceNoticeId());
        Optional.ofNullable(dbInvoiceNotice).ifPresent(o -> {
            String invoiceNoticeStatus = o.getInvoiceNoticeStatus();
            if (InvoiceNoticeStatus.ABANDONED.name().equals(invoiceNoticeStatus)) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,?????????!"));
            }
        });

        // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        List<InvoiceDetail> invoiceDetails = invoiceNoticeSaveDTO.getInvoiceDetails();
        Set<Object> hashSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(invoiceDetails)) {
            invoiceDetails.forEach(invoiceDetail -> {

                boolean flag = hashSet.add(String.valueOf(invoiceDetail.getOrderNumber()) + invoiceDetail.getItemCode() + invoiceDetail.getReceiveNum() + invoiceDetail.getNotInvoiceQuantity() + invoiceDetail.getInvoiceQuantity());
                if (!flag) throw new BaseException("???????????????+????????????+????????????+???????????????+??????????????????,???????????????");

                List<InvoiceDetail> list = null;
                if (ObjectUtils.isEmpty(invoiceDetail.getInvoiceDetailId())) {
                    list = iInvoiceDetailService.list(Wrappers.lambdaQuery(InvoiceDetail.class).
                            select(InvoiceDetail::getInvoiceDetailId).
                            eq(InvoiceDetail::getOrderNumber, invoiceDetail.getOrderNumber()).
                            eq(InvoiceDetail::getItemCode, invoiceDetail.getItemCode()).
                            eq(InvoiceDetail::getReceiveNum, invoiceDetail.getReceiveNum()).
                            eq(InvoiceDetail::getNotInvoiceQuantity, invoiceDetail.getNotInvoiceQuantity()).
                            eq(InvoiceDetail::getInvoiceQuantity, invoiceDetail.getInvoiceQuantity())
                    );
                } else {
                    list = iInvoiceDetailService.list(Wrappers.lambdaQuery(InvoiceDetail.class).
                            select(InvoiceDetail::getInvoiceDetailId).
                            eq(InvoiceDetail::getOrderNumber, invoiceDetail.getOrderNumber()).
                            eq(InvoiceDetail::getItemCode, invoiceDetail.getItemCode()).
                            eq(InvoiceDetail::getReceiveNum, invoiceDetail.getReceiveNum()).
                            eq(InvoiceDetail::getNotInvoiceQuantity, invoiceDetail.getNotInvoiceQuantity()).
                            eq(InvoiceDetail::getInvoiceQuantity, invoiceDetail.getInvoiceQuantity()).
                            ne(InvoiceDetail::getInvoiceDetailId, invoiceDetail.getInvoiceDetailId())
                    );
                }
                if (CollectionUtils.isNotEmpty(list))
                    throw new BaseException("???????????????+????????????+????????????+???????????????+??????????????????,????????????????????????");
            });
        }
    }

    @Override
    @AuthData(module = {MenuEnum.PUR_INVOICE, MenuEnum.SUPPLIER_SIGN})
    public PageInfo<InvoiceNoticeDTO> listPageByParm(InvoiceNoticeQueryDTO invoiceNoticeQueryDTO) {
        PageUtil.startPage(invoiceNoticeQueryDTO.getPageNum(), invoiceNoticeQueryDTO.getPageSize());
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (UserType.VENDOR.name().equals(loginAppUser.getUserType())) {
            //???????????????????????????????????????????????????????????????
            if (Objects.isNull(loginAppUser.getCompanyId())) {
                return new PageInfo<>(new ArrayList<>());
            }
            invoiceNoticeQueryDTO.setVendorId(loginAppUser.getCompanyId()).setUserType(UserType.VENDOR.name());
        }
        invoiceNoticeQueryDTO.setInvoiceNoticeNumber(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getInvoiceNoticeNumber()) ? invoiceNoticeQueryDTO.getInvoiceNoticeNumber() : null);
        invoiceNoticeQueryDTO.setOrgIds(!CollectionUtils.isEmpty(invoiceNoticeQueryDTO.getOrgIds()) ? invoiceNoticeQueryDTO.getOrgIds() : null);
        invoiceNoticeQueryDTO.setVendorParam(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getVendorParam()) ? invoiceNoticeQueryDTO.getVendorParam() : null);
        invoiceNoticeQueryDTO.setCeeaCostType(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getCeeaCostType()) ? invoiceNoticeQueryDTO.getCeeaCostType() : null);
        invoiceNoticeQueryDTO.setCeeaCostTypeCode(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getCeeaCostTypeCode()) ? invoiceNoticeQueryDTO.getCeeaCostTypeCode() : null);
        invoiceNoticeQueryDTO.setInvoiceNoticeStatus(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getInvoiceNoticeStatus()) ? invoiceNoticeQueryDTO.getInvoiceNoticeStatus() : null);
        invoiceNoticeQueryDTO.setOrderNumber(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getOrderNumber()) ? invoiceNoticeQueryDTO.getOrderNumber() : null);
        invoiceNoticeQueryDTO.setReceiveOrderNo(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getReceiveOrderNo()) ? invoiceNoticeQueryDTO.getReceiveOrderNo() : null);
        invoiceNoticeQueryDTO.setUserType(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getUserType()) ? invoiceNoticeQueryDTO.getUserType() : null);
        invoiceNoticeQueryDTO.setContractNo(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getContractNo()) ? invoiceNoticeQueryDTO.getContractNo() : null);
        invoiceNoticeQueryDTO.setMaterialParam(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getMaterialParam()) ? invoiceNoticeQueryDTO.getMaterialParam() : null);
        invoiceNoticeQueryDTO.setCategoryName(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getCategoryName()) ? invoiceNoticeQueryDTO.getCategoryName() : null);
        invoiceNoticeQueryDTO.setProjectName(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getProjectName()) ? invoiceNoticeQueryDTO.getProjectName() : null);
        invoiceNoticeQueryDTO.setProjectNum(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getProjectNum()) ? invoiceNoticeQueryDTO.getProjectNum() : null);
        //????????????????????????????????????,????????????????????????????????????
        if (OnlineOrNotice.ONLINE_INVOICE.name().equals(invoiceNoticeQueryDTO.getOnlineOrNotice())) {
            //????????????,???????????????????????????
            if (YesOrNo.YES.getValue().equals(invoiceNoticeQueryDTO.getIfService())) {
                //??????????????????????????????
                filterBusinessSmallCode(invoiceNoticeQueryDTO);

                invoiceNoticeQueryDTO.setOrderStatus(PurchaseOrderEnum.APPROVED.getValue());
                invoiceNoticeQueryDTO.setOrderDetailStatus(OrderDetailStatus.ACCEPT.getValue());
                List<InvoiceNoticeDTO> invoiceNoticeDTOS = this.baseMapper.orderListPageByParam(invoiceNoticeQueryDTO);
                for (InvoiceNoticeDTO invoiceNoticeDTO : invoiceNoticeDTOS) {
                    if (invoiceNoticeDTO == null) continue;
                    BigDecimal invoiceQuantity = invoiceNoticeDTO.getInvoiceQuantity() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getInvoiceQuantity();//??????????????????
                    BigDecimal notInvoiceQuantity = invoiceNoticeDTO.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getNotInvoiceQuantity();//???????????????
                    BigDecimal unitPriceExcludingTax = invoiceNoticeDTO.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getUnitPriceExcludingTax();//?????????????????????(????????????)
                    BigDecimal unitPriceContainingTax = invoiceNoticeDTO.getUnitPriceContainingTax() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getUnitPriceContainingTax();//???????????????)
                    BigDecimal noTaxAmount = notInvoiceQuantity.multiply(unitPriceExcludingTax).setScale(2, BigDecimal.ROUND_HALF_UP) == null ? BigDecimal.ZERO : notInvoiceQuantity.multiply(unitPriceExcludingTax);//??????????????????
                    BigDecimal taxAmount = notInvoiceQuantity.multiply(unitPriceContainingTax).setScale(2, BigDecimal.ROUND_HALF_UP) == null ? BigDecimal.ZERO : notInvoiceQuantity.multiply(unitPriceContainingTax);//????????????
                    invoiceNoticeDTO.setNoTaxAmount(noTaxAmount)//??????????????????
                            .setTaxAmount(taxAmount)
                            .setTax(taxAmount.subtract(noTaxAmount));

                    //2020-12-29 ?????????????????? ????????????????????????????????????????????????
                    invoiceNoticeDTO.setInvoiceQuantity(notInvoiceQuantity);

                }
                return new PageInfo<>(invoiceNoticeDTOS);
            }
            //???????????????,????????????????????????????????????
            if (YesOrNo.NO.getValue().equals(invoiceNoticeQueryDTO.getIfService())) {
                invoiceNoticeQueryDTO.setInvoiceNoticeStatus(InvoiceNoticeStatus.CONFIRMED.name());
            }
        }
        List<InvoiceNoticeDTO> invoiceNoticeDTOS = this.baseMapper.listPageByParm(invoiceNoticeQueryDTO);
        for (InvoiceNoticeDTO invoiceNoticeDTO : invoiceNoticeDTOS) {
            if (invoiceNoticeDTO == null) continue;
            BigDecimal invoiceQuantity = invoiceNoticeDTO.getInvoiceQuantity() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getInvoiceQuantity();//??????????????????
            BigDecimal notInvoiceQuantity = invoiceNoticeDTO.getNotInvoiceQuantity() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getNotInvoiceQuantity();//???????????????
            BigDecimal unitPriceExcludingTax = invoiceNoticeDTO.getUnitPriceExcludingTax() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getUnitPriceExcludingTax();//?????????????????????(????????????)
            BigDecimal unitPriceContainingTax = invoiceNoticeDTO.getUnitPriceContainingTax() == null ? BigDecimal.ZERO : invoiceNoticeDTO.getUnitPriceContainingTax();//???????????????)
            BigDecimal noTaxAmount = notInvoiceQuantity.multiply(unitPriceExcludingTax).setScale(2, BigDecimal.ROUND_HALF_UP) == null ? BigDecimal.ZERO : notInvoiceQuantity.multiply(unitPriceExcludingTax);//??????????????????
            BigDecimal taxAmount = notInvoiceQuantity.multiply(unitPriceContainingTax).setScale(2, BigDecimal.ROUND_HALF_UP) == null ? BigDecimal.ZERO : notInvoiceQuantity.multiply(unitPriceContainingTax);//????????????
            invoiceNoticeDTO.setNoTaxAmount(noTaxAmount)//??????????????????
                    .setTaxAmount(taxAmount)
                    .setTax(taxAmount.subtract(noTaxAmount));
        }
        return new PageInfo<>(invoiceNoticeDTOS);
    }

    private void filterBusinessSmallCode(InvoiceNoticeQueryDTO invoiceNoticeQueryDTO) {
        List<DictItem> dictItems = baseClient.listDictItemsByParam(new DictItemDTO().setDictItemMark(YesOrNo.YES.getValue()));
        List<String> businessSmallCodes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dictItems)) {
            businessSmallCodes = dictItems.stream().map(dictItem -> dictItem.getDictItemCode()).collect(Collectors.toList());
        }
        invoiceNoticeQueryDTO.setBusinessSmallCodes(businessSmallCodes);
    }

    public Long saveOrUpdateInvoiceNoticeSaveDTO(InvoiceNoticeSaveDTO invoiceNoticeSaveDTO, String invoiceNoticeStatus) {

        InvoiceNotice invoiceNotice = invoiceNoticeSaveDTO.getInvoiceNotice();
        List<InvoiceDetail> invoiceDetails = invoiceNoticeSaveDTO.getInvoiceDetails();
        List<InvoicePunish> invoicePunishes = invoiceNoticeSaveDTO.getInvoicePunishes();
        List<Fileupload> fileuploads = invoiceNoticeSaveDTO.getFileuploads();
//        List<InvoiceNoticeReceipt> invoiceNoticeReceipts = invoiceNoticeSaveDTO.getInvoiceNoticeReceipts();
//        List<InvoiceNoticeReturn> invoiceNoticeReturns = invoiceNoticeSaveDTO.getInvoiceNoticeReturns();
//        List<InvoiceTaxControl> invoiceTaxControls = invoiceNoticeSaveDTO.getInvoiceTaxControls();

        //??????????????????????????????
        saveOrUpdateInvoiceNotice(invoiceNotice, invoiceNoticeStatus);

        //???????????????????????????
        saveOrUpdateInvoiceDetail(invoiceNotice, invoiceDetails, invoiceNoticeStatus);

        //??????????????????????????????????????????
        saveOrUpdateInvoicePunish(invoiceNotice, invoicePunishes);

        //????????????
        if (!CollectionUtils.isEmpty(fileuploads)) {
            fileCenterClient.bindingFileupload(fileuploads, invoiceNotice.getInvoiceNoticeId());
        }

//        //?????????????????????????????????
//        saveOrUpdateInvoiceNoticeReceipts(invoiceNotice, invoiceNoticeReceipts);
//        //?????????????????????????????????
//        saveOrUpdateInvoiceNoticeReturns(invoiceNotice, invoiceNoticeReturns);
//        //??????????????????????????????
//        saveOrUpdateInvoiceTaxControls(invoiceNotice, invoiceTaxControls);

        return invoiceNotice.getInvoiceNoticeId();
    }

    private void saveOrUpdateInvoicePunish(InvoiceNotice invoiceNotice, List<InvoicePunish> invoicePunishes) {
        if (!CollectionUtils.isEmpty(invoicePunishes)) {
            for (InvoicePunish invoicePunish : invoicePunishes) {
                if (invoicePunish == null) continue;
                if (invoicePunish.getInvoicePunishId() == null) {
                    invoicePunish.setInvoicePunishId(IdGenrator.generate()).setInvoiceNoticeId(invoiceNotice.getInvoiceNoticeId());
                    iInvoicePunishService.save(invoicePunish);
                } else {
                    iInvoicePunishService.updateById(invoicePunish);
                }

                /*???????????????????????????*/
                updateVendorAssesFormStatus(invoiceNotice, invoicePunish);
            }
        }
    }

    private void updateVendorAssesFormStatus(InvoiceNotice invoiceNotice, InvoicePunish invoicePunish) {
        //1.????????????,??????????????????
        if (InvoiceNoticeStatus.DRAFT.name().equals(invoiceNotice.getInvoiceNoticeStatus())) {
            performanceClient.modify(new VendorAssesForm()
                    .setVendorAssesId(invoicePunish.getVendorAssesId()).setIfQuote(YesOrNo.YES.getValue()));
        }
        //2.????????????,?????????????????????
        if (InvoiceNoticeStatus.SUBMITTED.name().equals(invoiceNotice.getInvoiceNoticeStatus())) {
            performanceClient.modify(new VendorAssesForm()
                    .setVendorAssesId(invoicePunish.getVendorAssesId())
                    .setStatus(VendorAssesFormStatus.SETTLED.getKey()));
        }
        //3.????????????,?????????????????????
        if (InvoiceNoticeStatus.REJECTED.name().equals(invoiceNotice.getInvoiceNoticeStatus())) {
            performanceClient.modify(new VendorAssesForm()
                    .setVendorAssesId(invoicePunish.getVendorAssesId())
                    .setStatus(VendorAssesFormStatus.ASSESSED.getKey()));
        }
        //4.????????????,??????????????????????????????????????????
        if (InvoiceNoticeStatus.ABANDONED.name().equals(invoiceNotice.getInvoiceNoticeStatus())) {
            performanceClient.modify(new VendorAssesForm()
                    .setVendorAssesId(invoicePunish.getVendorAssesId())
                    .setStatus(VendorAssesFormStatus.ASSESSED.getKey())
                    .setIfQuote(YesOrNo.NO.getValue()));
        }
    }

    private void saveOrUpdateInvoiceDetail(InvoiceNotice invoiceNotice, List<InvoiceDetail> invoiceDetails, String invoiceNoticeStatus) {
        if (!CollectionUtils.isEmpty(invoiceDetails)) {
            int row = 1;
            for (InvoiceDetail invoiceDetail : invoiceDetails) {
                if (invoiceDetail == null) continue;
                if (invoiceDetail.getInvoiceDetailId() == null) {
                    invoiceDetail.setInvoiceDetailId(IdGenrator.generate())
                            .setInvoiceNoticeId(invoiceNotice.getInvoiceNoticeId())
                            .setVendorId(invoiceNotice.getVendorId())
                            .setVendorCode(invoiceNotice.getVendorCode())
                            .setVendorName(invoiceNotice.getVendorName());
                    iInvoiceDetailService.save(invoiceDetail);
                    String key = INVOICE_NOTICE_LOCK + invoiceDetail.getWarehousingReturnDetailId();
                    Boolean isLock = redisUtil.tryLock(key, 30, TimeUnit.SECONDS);
                    if (isLock) {
                        try {
                            //???????????????????????????
                            writeBackWarehousingReturnDetail(invoiceNoticeStatus, invoiceDetail, row);
                        } finally {
                            redisUtil.unLock(key);
                        }
                    } else {
                        throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????"));
                    }
                } else {
                    iInvoiceDetailService.updateById(invoiceDetail);
//                    //???????????????????????????????????????????????????????????????
//                    writeBackWarehousingReturnDetail(invoiceNoticeStatus, invoiceDetail);
                }
                row++;
            }
        }
    }

    private void writeBackWarehousingReturnDetail(String invoiceNoticeStatus, InvoiceDetail invoiceDetail, int row) {
        //???????????????????????????????????????????????????????????????
        if (InvoiceStatus.DRAFT.name().equals(invoiceNoticeStatus)) {
            WarehousingReturnDetail warehousingReturnDetail = iWarehousingReturnDetailService.getById(invoiceDetail.getWarehousingReturnDetailId());
            warehousingReturnDetail.getNotInvoiceQuantity();
            if (null != warehousingReturnDetail && warehousingReturnDetail.getNotInvoiceQuantity().compareTo(BigDecimal.ZERO) == 0) {
                throw new BaseException(LocaleHandler.getLocaleMsg("???" + row + "???????????????????????????????????????????????????,?????????!"));
            }
            BigDecimal notInvoiceQuantity = warehousingReturnDetail.getNotInvoiceQuantity();
            iWarehousingReturnDetailService.updateById(new WarehousingReturnDetail()
                    .setWarehousingReturnDetailId(invoiceDetail.getWarehousingReturnDetailId())
                    .setNotInvoiceQuantity(notInvoiceQuantity.subtract(invoiceDetail.getInvoiceQuantity())));
        }
    }

//    @Override
//    public PageInfo<InvoiceNotice> listPageByParm(InvoiceNoticeQueryDTO invoiceNoticeQueryDTO) {
//        PageUtil.startPage(invoiceNoticeQueryDTO.getPageNum(), invoiceNoticeQueryDTO.getPageSize());
//        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
//        InvoiceNotice invoiceNotice = new InvoiceNotice();
//        if (UserType.VENDOR.name().equals(loginAppUser.getUserType())) {
//            invoiceNotice.setVendorId(loginAppUser.getCompanyId());
//        }
//        if (StringUtils.isNotBlank(invoiceNoticeQueryDTO.getInvoiceNoticeStatus())) {
//            invoiceNotice.setInvoiceNoticeStatus(invoiceNoticeQueryDTO.getInvoiceNoticeStatus());
//        }
//        QueryWrapper<InvoiceNotice> queryWrapper = new QueryWrapper<>(invoiceNotice
//                .setOrgId(invoiceNoticeQueryDTO.getOrgId()));
//        if (invoiceNoticeQueryDTO.getCreationDate() != null) {
//        	queryWrapper.apply(invoiceNoticeQueryDTO.getCreationDate() != null, "DATE_FORMAT(CREATION_DATE,'%Y-%m-%d') = {0}", invoiceNoticeQueryDTO.getCreationDate());
//        }
//        queryWrapper.like(StringUtils.isNotBlank(invoiceNoticeQueryDTO.getInvoiceNoticeNumber()), "INVOICE_NOTICE_NUMBER", invoiceNoticeQueryDTO.getInvoiceNoticeNumber());
//        queryWrapper.in(!CollectionUtils.isEmpty(invoiceNoticeQueryDTO.getInvoiceNoticeStatuses()), "INVOICE_NOTICE_STATUS", invoiceNoticeQueryDTO.getInvoiceNoticeStatuses());
//        queryWrapper.orderByDesc("LAST_UPDATE_DATE");
//        return new PageInfo<>(this.list(queryWrapper));
//    }

    @Override
    public List<StatementHead> listStatementHeadByParm(InvoiceNoticeQueryDTO invoiceNoticeQueryDTO) {
        //???????????????,????????????:??????????????? ?????????ID ????????????ID ????????????
        String statementNumber = invoiceNoticeQueryDTO.getStatementNumber();
        QueryWrapper<StatementHead> statementHeadQueryWrapper = new QueryWrapper<>(new StatementHead()
                .setVendorId(invoiceNoticeQueryDTO.getVendorId())
                .setOrganizationId(invoiceNoticeQueryDTO.getOrgId())
                .setStatementStatus(StatementStatusEnum.PASS.getValue()));
        statementHeadQueryWrapper.like(StringUtils.isNotBlank(statementNumber), "STATEMENT_NUMBER", statementNumber);
        List<StatementHead> statementHeads = iStatementHeadService.list(statementHeadQueryWrapper);

        //????????????????????????
        InvoiceNotice invoiceNotice = new InvoiceNotice();
        String fullPathId = StringUtils.isNotBlank(invoiceNoticeQueryDTO.getFullPathId()) ? invoiceNoticeQueryDTO.getFullPathId() : null;
        QueryWrapper<InvoiceNotice> invoiceNoticeQueryWrapper = new QueryWrapper<>(invoiceNotice
                .setVendorId(invoiceNoticeQueryDTO.getVendorId())
                .setOrgId(invoiceNoticeQueryDTO.getOrgId())
                .setFullPathId(fullPathId));
        invoiceNoticeQueryWrapper.ne("INVOICE_NOTICE_STATUS", InvoiceNoticeStatus.ABANDONED.name());
        invoiceNoticeQueryWrapper.ne(invoiceNoticeQueryDTO.getInvoiceNoticeId() != null, "INVOICE_NOTICE_ID", invoiceNoticeQueryDTO.getInvoiceNoticeId());
        List<InvoiceNotice> invoiceNotices = this.list(invoiceNoticeQueryWrapper);

        //??????????????????????????????
        List<StatementHead> newStatementHeads = excludeRepeatedStatementHeads(statementHeads, invoiceNotices);
        return newStatementHeads;
    }

    @Override
    @Transactional
    public void submit(InvoiceNoticeSaveDTO invoiceNoticeSaveDTO) {
        checkBefore(invoiceNoticeSaveDTO);
        saveOrUpdateInvoiceNoticeSaveDTO(invoiceNoticeSaveDTO, invoiceNoticeSaveDTO.getInvoiceNotice().getInvoiceNoticeStatus());
    }

    @Override
    public InvoiceNoticeSaveDTO getInvoiceNoticeSaveDTO(Long invoiceNoticeId) {
        InvoiceNoticeSaveDTO invoiceNoticeSaveDTO = new InvoiceNoticeSaveDTO();
        InvoiceNotice invoiceNotice = this.getById(invoiceNoticeId);
        List<InvoiceDetail> invoiceDetails = iInvoiceDetailService.list(new QueryWrapper<>(new InvoiceDetail()
                .setInvoiceNoticeId(invoiceNoticeId)));
        List<InvoicePunish> invoicePunishes = iInvoicePunishService.list(new QueryWrapper<>(new InvoicePunish()
                .setInvoiceNoticeId(invoiceNoticeId)));
        List<Fileupload> fileuploads = fileCenterClient.listPage(new Fileupload().setBusinessId(invoiceNoticeId), "").getList();
        invoiceNoticeSaveDTO.setInvoiceDetails(invoiceDetails).setInvoicePunishes(invoicePunishes).setFileuploads(fileuploads).setInvoiceNotice(invoiceNotice);
//        List<InvoiceNoticeReceipt> invoiceNoticeReceipts = iInvoiceNoticeReceiptService.list(new QueryWrapper<>(new InvoiceNoticeReceipt()
//                .setInvoiceNoticeId(invoiceNoticeId)));
//        List<InvoiceNoticeReturn> invoiceNoticeReturns = iInvoiceNoticeReturnService.list(new QueryWrapper<>(new InvoiceNoticeReturn()
//                .setInvoiceNoticeId(invoiceNoticeId)));
//        List<InvoiceTaxControl> invoiceTaxControls = iInvoiceTaxControlService.list(new QueryWrapper<>(new InvoiceTaxControl().setInvoiceNoticeId(invoiceNoticeId)));
//        invoiceNoticeSaveDTO
//                .setInvoiceNotice(invoiceNotice)
//                .setInvoiceNoticeReceipts(invoiceNoticeReceipts)
//                .setInvoiceNoticeReturns(invoiceNoticeReturns)
//                .setInvoiceTaxControls(invoiceTaxControls);
        return invoiceNoticeSaveDTO;
    }

    @Override
    public void exportExcel(Long invoiceNoticeId) {
        Assert.notNull(invoiceNoticeId, "????????????: invoiceNoticeId");
    }

    @Override
    @Transactional
    public void deleteByInvoiceNoticeId(Long invoiceNoticeId) {
        this.removeById(invoiceNoticeId);
        iInvoiceDetailService.remove(new QueryWrapper<>(new InvoiceDetail().setInvoiceNoticeId(invoiceNoticeId)));
        iInvoicePunishService.remove(new QueryWrapper<>(new InvoicePunish().setInvoiceNoticeId(invoiceNoticeId)));
        fileCenterClient.deleteByParam(new Fileupload().setBusinessId(invoiceNoticeId));
//        iInvoiceNoticeReceiptService.remove(new QueryWrapper<>(new InvoiceNoticeReceipt().setInvoiceNoticeId(invoiceNoticeId)));
//        iInvoiceNoticeReturnService.remove(new QueryWrapper<>(new InvoiceNoticeReturn().setInvoiceNoticeId(invoiceNoticeId)));
//        iInvoiceTaxControlService.remove(new QueryWrapper<>(new InvoiceTaxControl().setInvoiceNoticeId(invoiceNoticeId)));
    }

    @Override
    @Transactional
    public void batchDeleteByStatementHeadId(List<InvoiceNoticeQueryDTO> invoiceNoticeQueryDTOs) {
        if (!CollectionUtils.isEmpty(invoiceNoticeQueryDTOs)) {
            for (InvoiceNoticeQueryDTO invoiceNoticeQueryDTO : invoiceNoticeQueryDTOs) {
                if (invoiceNoticeQueryDTO == null) continue;
                Assert.notNull(invoiceNoticeQueryDTO.getStatementHeadId(), LocaleHandler.getLocaleMsg("statementHeadId??????"));
                InvoiceNotice invoiceNotice = this.getById(invoiceNoticeQueryDTO.getInvoiceNoticeId());
                if (invoiceNotice != null) {
                    removeSourceNumber(invoiceNoticeQueryDTO, invoiceNotice);

//                    iInvoiceNoticeReceiptService.remove(new QueryWrapper<>(new InvoiceNoticeReceipt().setStatementHeadId(invoiceNoticeQueryDTO.getStatementHeadId())));
//                    iInvoiceNoticeReturnService.remove(new QueryWrapper<>(new InvoiceNoticeReturn().setStatementHeadId(invoiceNoticeQueryDTO.getStatementHeadId())));
                }
            }
        }
    }

    @Override
    public void confirm(Long invoiceNoticeId) {
        InvoiceNotice byId = this.getById(invoiceNoticeId);
        if (!InvoiceNoticeStatus.SUBMITTED.name().equals(byId.getInvoiceNoticeStatus())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,?????????!"));
        }
        this.updateById(byId.setInvoiceNoticeStatus(InvoiceNoticeStatus.CONFIRMED.name()));
    }

    @Override
    @Transactional
    public void reject(Long invoiceNoticeId) {
        InvoiceNotice byId = this.getById(invoiceNoticeId);
        if (byId != null) {
            if (!InvoiceNoticeStatus.SUBMITTED.name().equals(byId.getInvoiceNoticeStatus())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,?????????!"));
            }
            this.updateById(byId.setInvoiceNoticeStatus(InvoiceNoticeStatus.VENDOR_REJECTED.name()));
            //???????????????????????????
            List<InvoicePunish> invoicePunishes = iInvoicePunishService.list(new QueryWrapper<>(new InvoicePunish().setInvoiceNoticeId(byId.getInvoiceNoticeId())));
            if (!CollectionUtils.isEmpty(invoicePunishes)) {
                for (InvoicePunish invoicePunish : invoicePunishes) {
                    if (invoicePunish == null) continue;
                    updateVendorAssesFormStatus(byId, invoicePunish);
                }
            }
        }
    }

    @Override
    public PageInfo<WarehousingReturnDetail> warehousingReturnlistPageByParam(WarehousingReturnDetailRequestDTO warehousingReturnDetailRequestDTO) {
        return iWarehousingReturnDetailService.warehousingReturnlistPageByParam(warehousingReturnDetailRequestDTO);
    }

    @Override
    @Transactional
    public void abandon(InvoiceNotice invoiceNotice) {
        InvoiceNotice byId = this.getById(invoiceNotice.getInvoiceNoticeId());
        if (byId != null) {
            this.updateById(byId.setInvoiceNoticeStatus(InvoiceNoticeStatus.ABANDONED.name()));
            List<InvoiceDetail> invoiceDetails = iInvoiceDetailService.list(new QueryWrapper<>(new InvoiceDetail().setInvoiceNoticeId(byId.getInvoiceNoticeId())));
            if (!CollectionUtils.isEmpty(invoiceDetails)) {
                for (InvoiceDetail invoiceDetail : invoiceDetails) {
                    BigDecimal invoiceQuantity = invoiceDetail.getInvoiceQuantity();//??????????????????????????????
                    //????????????????????????????????????
                    WarehousingReturnDetail warehousingReturnDetail = iWarehousingReturnDetailService.getById(invoiceDetail.getWarehousingReturnDetailId());
                    BigDecimal notInvoiceQuantity = warehousingReturnDetail.getNotInvoiceQuantity();//??????????????????????????????
                    BigDecimal receiveNum = warehousingReturnDetail.getReceiveNum();//???????????????????????????
                    Integer lineNum = warehousingReturnDetail.getLineNum();//????????????
                    BigDecimal newNotInvoiceQuantity = notInvoiceQuantity.add(invoiceQuantity);
                    //??????????????????????????????????????????(????????????????????????)
                    if (newNotInvoiceQuantity.compareTo(receiveNum) == 1) {
                        throw new BaseException(LocaleHandler.getLocaleMsg("?????????,????????????:" + lineNum + "????????????????????????????????????,??????????????????!"));
                    }
                    iWarehousingReturnDetailService.updateById(warehousingReturnDetail.setNotInvoiceQuantity(newNotInvoiceQuantity));
                }
            }
            List<InvoicePunish> invoicePunishes = iInvoicePunishService.list(new QueryWrapper<>(new InvoicePunish().setInvoiceNoticeId(byId.getInvoiceNoticeId())));
            if (!CollectionUtils.isEmpty(invoicePunishes)) {
                for (InvoicePunish invoicePunish : invoicePunishes) {
                    if (invoicePunish == null) continue;
                    updateVendorAssesFormStatus(byId, invoicePunish);
                }
            }
            fileCenterClient.deleteByParam(new Fileupload().setBusinessId(byId.getInvoiceNoticeId()));
        }
    }

    @Override
    public void withdraw(Long invoiceNoticeId) {
        InvoiceNotice invoiceNotice = this.getById(invoiceNoticeId);
        //?????????????????????,???????????????
        if (YesOrNo.YES.getValue().equals(invoiceNotice.getCeeaIfSupplierConfirm()) && !InvoiceNoticeStatus.CONFIRMED.name().equals(invoiceNotice.getInvoiceNoticeStatus())) {
            this.updateById(invoiceNotice.setInvoiceNoticeStatus(InvoiceStatus.DRAFT.name()));
        }
    }

    private void removeSourceNumber(InvoiceNoticeQueryDTO invoiceNoticeQueryDTO, InvoiceNotice invoiceNotice) {
        String sourceNumber = invoiceNotice.getSourceNumber();
        if (!StringUtil.isEmpty(sourceNumber)) {
            List<String> strings = Arrays.asList(StringUtil.split(sourceNumber, ","));
            List<String> collect = strings.stream().filter(s -> !s.equals(invoiceNoticeQueryDTO.getStatementHeadId().toString())).collect(Collectors.toList());
            String newSourceNumber = null;
            if (collect.size() > 1) {
                newSourceNumber = String.join(",", collect);
            } else if (collect.size() == 1) {
                newSourceNumber = collect.get(0);
            }
            this.updateById(invoiceNotice.setSourceNumber(newSourceNumber));
        }
    }

    private void checkBefore(InvoiceNoticeSaveDTO invoiceNoticeSaveDTO) {
        InvoiceNotice invoiceNotice = invoiceNoticeSaveDTO.getInvoiceNotice();
        List<InvoiceDetail> invoiceDetails = invoiceNoticeSaveDTO.getInvoiceDetails();
        if (CollectionUtils.isEmpty(invoiceDetails)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????,?????????!"));
        }
        if (!InvoiceStatus.REJECTED.name().equals(invoiceNotice.getInvoiceNoticeStatus())
                && !InvoiceStatus.DRAFT.name().equals(invoiceNotice.getInvoiceNoticeStatus())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????,??????????????????!"));
        }
        if (YesOrNo.NO.getValue().equals(invoiceNotice.getCeeaIfSupplierConfirm())) {
            invoiceNotice.setInvoiceNoticeStatus(InvoiceNoticeStatus.CONFIRMED.name());
        } else {
            invoiceNotice.setInvoiceNoticeStatus(InvoiceNoticeStatus.SUBMITTED.name());
        }
        //TODO ?????????????????????????????? ???????????????????????????0
    }

    private List<StatementHead> excludeRepeatedStatementHeads(List<StatementHead> statementHeads, List<InvoiceNotice> invoiceNotices) {
        if (!CollectionUtils.isEmpty(invoiceNotices)) {
            List<String> sourceNumbers = new ArrayList<>();
            invoiceNotices.forEach(invoiceNotice1 -> {
                if (invoiceNotice1 != null && StringUtils.isNotBlank(invoiceNotice1.getSourceNumber())) {
                    String[] strings = invoiceNotice1.getSourceNumber().split(",");
                    sourceNumbers.addAll(Arrays.asList(strings));
                }
            });
            sourceNumbers.forEach(s -> {
                Iterator<StatementHead> iterator = statementHeads.iterator();
                while (iterator.hasNext()) {
                    if (StringUtils.equals(iterator.next().getStatementNumber().toString(), s)) {
                        iterator.remove();
                    }
                }
            });
            return statementHeads;
        }
        return statementHeads;
    }

//    private void saveOrUpdateInvoiceTaxControls(InvoiceNotice invoiceNotice, List<InvoiceTaxControl> invoiceTaxControls) {
//        if (!CollectionUtils.isEmpty(invoiceTaxControls)) {
//            invoiceTaxControls.forEach(invoiceTaxControl -> {
//                if (invoiceTaxControl != null) {
//                    Long taxControlId = invoiceTaxControl.getTaxControlId();
//                    if (taxControlId == null) {
//                        long id = IdGenrator.generate();
//                        invoiceTaxControl.setTaxControlId(id)
//                                .setInvoiceNoticeId(invoiceNotice.getInvoiceNoticeId());
//                        iInvoiceTaxControlService.save(invoiceTaxControl);
//                    }else {
//                        iInvoiceTaxControlService.updateById(invoiceTaxControl);
//                    }
//                }
//            });
//        }
//    }

//    private void saveOrUpdateInvoiceNoticeReturns(InvoiceNotice invoiceNotice, List<InvoiceNoticeReturn> invoiceNoticeReturns) {
//        if (!CollectionUtils.isEmpty(invoiceNoticeReturns)) {
//            invoiceNoticeReturns.forEach(invoiceNoticeReturn -> {
//                if (invoiceNoticeReturn != null) {
//                    Long noticeReturnId = invoiceNoticeReturn.getNoticeReturnId();
//                    if (noticeReturnId == null) {
//                        long id = IdGenrator.generate();
//                        invoiceNoticeReturn.setNoticeReturnId(id)
//                                .setInvoiceNoticeId(invoiceNotice.getInvoiceNoticeId());
//                        iInvoiceNoticeReturnService.save(invoiceNoticeReturn);
//                    }else {
//                        iInvoiceNoticeReturnService.updateById(invoiceNoticeReturn);
//                    }
//                }
//            });
//        }
//    }

//    private void saveOrUpdateInvoiceNoticeReceipts(InvoiceNotice invoiceNotice, List<InvoiceNoticeReceipt> invoiceNoticeReceipts) {
//        if (!CollectionUtils.isEmpty(invoiceNoticeReceipts)) {
//            invoiceNoticeReceipts.forEach(invoiceNoticeReceipt -> {
//                if (invoiceNoticeReceipt != null) {
//                    Long noticeReceiptId = invoiceNoticeReceipt.getNoticeReceiptId();
//                    if (noticeReceiptId == null) {
//                        long id = IdGenrator.generate();
//                        invoiceNoticeReceipt.setNoticeReceiptId(id)
//                                .setInvoiceNoticeId(invoiceNotice.getInvoiceNoticeId());
//                        iInvoiceNoticeReceiptService.save(invoiceNoticeReceipt);
//                    }else {
//                        iInvoiceNoticeReceiptService.updateById(invoiceNoticeReceipt);
//                    }
//                }
//            });
//        }
//    }

    private void saveOrUpdateInvoiceNotice(InvoiceNotice invoiceNotice, String invoiceNoticeStatus) {
        Assert.notNull(invoiceNotice, LocaleHandler.getLocaleMsg("invoiceNotice????????????"));

        if (invoiceNotice.getInvoiceNoticeId() == null) {
            long id = IdGenrator.generate();
            invoiceNotice.setInvoiceNoticeId(id)
                    .setInvoiceNoticeStatus(invoiceNoticeStatus)
                    .setInvoiceNoticeNumber(baseClient.seqGen(SequenceCodeConstant.SEQ_SSC_INVOICE_NOTICE_NUM));
            this.save(invoiceNotice);
        } else {
            invoiceNotice.setInvoiceNoticeStatus(invoiceNoticeStatus);
            this.updateById(invoiceNotice);
        }
    }

    @Override
    public void exportInvoiceDetails(Long invoiceNoticeId, HttpServletResponse response) throws IOException {
        Assert.notNull(invoiceNoticeId, "???????????????invoiceNoticeId");
        List<InvoiceDetail> invoiceDetails = iInvoiceDetailService.list(new QueryWrapper<>(new InvoiceDetail()
                .setInvoiceNoticeId(invoiceNoticeId)));
        Map<String, String> dicCodeName = EasyExcelUtil.getDicCodeName("WAREHOURING_RETURN_DETAIL", baseClient);
        List<InvoiceDetailDto> invoiceDetailDtos = new ArrayList<>();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(invoiceDetails)) {
            invoiceDetails.forEach(invoiceDetail -> {
                InvoiceDetailDto invoiceDetailDto = new InvoiceDetailDto();
                BeanCopyUtil.copyProperties(invoiceDetailDto, invoiceDetail);
                String type = invoiceDetailDto.getType();
                if (StringUtil.notEmpty(type)) {
                    String s = dicCodeName.get(type);
                    invoiceDetailDto.setType(s);
                }
                invoiceDetailDtos.add(invoiceDetailDto);
            });
        }

        OutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "??????????????????");
        EasyExcelUtil.writeExcelWithModel(outputStream, "????????????", invoiceDetailDtos, InvoiceDetailDto.class);

    }

    //-------------------------------------------------------------OA???????????????---------------------------------------------------

    @Override
    public void submitFlow(Long businessId, String param) throws Exception {

        log.info("AdvanceApplyHeadServiceImpl---OASubmitFlow", businessId, param);
        InvoiceNotice invoiceNotice = new InvoiceNotice().setInvoiceNoticeId(businessId)
                .setInvoiceNoticeStatus(InvoiceNoticeStatus.SUBMITTED.name());
        this.updateById(invoiceNotice);

    }

    @Override
    public void passFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OASubmitFlow", businessId, param);
        InvoiceNotice OldInvoiceNotice = this.getById(businessId);
        InvoiceNotice invoiceNotice = new InvoiceNotice()
                .setInvoiceNoticeId(businessId);
        if (null != OldInvoiceNotice &&
                !InvoiceNoticeStatus.FIRST_REVIEW_APPROVED.name().equals(OldInvoiceNotice.getInvoiceNoticeStatus())) {
            invoiceNotice.setInvoiceNoticeStatus(InvoiceNoticeStatus.FIRST_REVIEW_APPROVED.name());
        } else {
            invoiceNotice.setInvoiceNoticeStatus(InvoiceNoticeStatus.FINAL_REVIEW_APPROVED.name());
        }
        this.updateById(invoiceNotice);
    }

    @Override
    public void rejectFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OASubmitFlow", businessId, param);
        InvoiceNotice invoiceNotice = new InvoiceNotice().setInvoiceNoticeId(businessId)
                .setInvoiceNoticeStatus(InvoiceNoticeStatus.REJECTED.name());
        this.updateById(invoiceNotice);
    }

    @Override
    public void withdrawFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OASubmitFlow", businessId, param);
        InvoiceNotice invoiceNotice = new InvoiceNotice().setInvoiceNoticeId(businessId)
                .setInvoiceNoticeStatus(InvoiceNoticeStatus.WITHDRAW.name());
        this.updateById(invoiceNotice);
    }

    @Override
    public void destoryFlow(Long businessId, String param) throws Exception {
        log.info("AdvanceApplyHeadServiceImpl---OASubmitFlow", businessId, param);
        InvoiceNotice invoiceNotice = new InvoiceNotice().setInvoiceNoticeId(businessId)
                .setInvoiceNoticeStatus(InvoiceNoticeStatus.ABANDONED.name());
        this.updateById(invoiceNotice);
    }

    @Override
    public String getVariableFlow(Long businessId, String param) throws Exception {
        return null;
    }

    @Override
    public String getDataPushFlow(Long businessId, String param) throws Exception {
        return null;
    }
}
