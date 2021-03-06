package com.midea.cloud.srm.cm.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.contract.MilestoneStatus;
import com.midea.cloud.common.enums.contract.ModelKey;
import com.midea.cloud.common.enums.pm.ps.PaymentPlanSourceTypeEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.JsonUtil;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.cm.contract.service.IContractHeadService;
import com.midea.cloud.srm.cm.model.service.IModelLineService;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.model.cm.contract.dto.ContractPayPlanDTO;
import com.midea.cloud.srm.model.cm.contract.dto.PayPlanRequestDTO;
import com.midea.cloud.srm.model.cm.contract.entity.ContractHead;
import com.midea.cloud.srm.model.cm.contract.entity.PayPlan;
import com.midea.cloud.srm.cm.contract.mapper.PayPlanMapper;
import com.midea.cloud.srm.cm.contract.service.IPayPlanService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.srm.model.cm.contract.vo.PayPlanVo;
import com.midea.cloud.srm.model.cm.model.entity.ModelLine;
import com.midea.cloud.srm.model.pm.ps.statement.dto.PaymentApplyHeadQueryDTO;
import com.midea.cloud.srm.model.pm.ps.statement.dto.PaymentApplySaveDTO;
import com.midea.cloud.srm.model.pm.ps.statement.entity.PaymentApplyHead;
import com.midea.cloud.srm.model.pm.ps.statement.entity.PaymentApplyLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
*  <pre>
 *  ?????????????????? ???????????????
 * </pre>
*
* @author chensl26@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-05-27 10:18:17
 *  ????????????:
 * </pre>
*/
@Service
@Slf4j
public class PayPlanServiceImpl extends ServiceImpl<PayPlanMapper, PayPlan> implements IPayPlanService {

    @Autowired
    IContractHeadService iContractHeadService;

    @Autowired
    PmClient pmClient;

    @Autowired
    IModelLineService iModelLineService;

    @Autowired
    private PayPlanMapper payPlanMapper;

    @Override
    public PageInfo<ContractPayPlanDTO> listPageContractPayPlanDTO(ContractPayPlanDTO contractPayPlanDTO) {
        PageUtil.startPage(contractPayPlanDTO.getPageNum(), contractPayPlanDTO.getPageSize());
        List<ContractPayPlanDTO> contractPayPlanDTOS = this.baseMapper.listPageContractPayPlanDTO(contractPayPlanDTO);
        if (!CollectionUtils.isEmpty(contractPayPlanDTOS)) {
            contractPayPlanDTOS.forEach(dto -> {
                if (dto != null) {
                    dto.setSourceType(PaymentPlanSourceTypeEnum.CONTRACT.getValue());
                    BigDecimal totalAmountNoTax = dto.getTotalAmountNoTax() == null ? new BigDecimal("0") : dto.getTotalAmountNoTax();
                    BigDecimal paidAmount = dto.getPaidAmount() == null ? new BigDecimal("0") : dto.getPaidAmount();
                    dto.setUnpaidAmountNoTax(totalAmountNoTax.subtract(paidAmount));
                }
            });
        }
        return new PageInfo<>(contractPayPlanDTOS);
    }

    @Override
    public PageInfo<PayPlanVo> ceeaListPageForPaymentApply(PayPlanRequestDTO requestDTO) {
        PageHelper.startPage(requestDTO.getPageNum(),requestDTO.getPageSize());
        List<PayPlanVo> payPlanVoList = payPlanMapper.findList(requestDTO);
        if(!CollectionUtils.isEmpty(payPlanVoList)){
            payPlanVoList.forEach(item -> {
                if(item != null){
                    BigDecimal stagePaymentAmount = item.getStagePaymentAmount() == null ? new BigDecimal("0") : item.getStagePaymentAmount();
                    BigDecimal paidAmount = item.getPaidAmount() == null ? new BigDecimal("0") : item.getPaidAmount();
                    item.setUnpaidAmount(stagePaymentAmount.subtract(paidAmount));
                }
            });
        }
        return new PageInfo<PayPlanVo>(payPlanVoList);
    }


    @Override
    @Transactional
    public void startPayApplication(Long payPlanId) {
        PayPlan payPlan = this.getOne(new QueryWrapper<>(new PayPlan().setPayPlanId(payPlanId)));
        if (payPlan != null) {
            if (MilestoneStatus.COMPLETED.name().equals(payPlan.getMilestoneStatus())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????,??????????????????????????????"));
            }
            //?????????????????????????????????
            payPlan.setMilestoneStatus(MilestoneStatus.COMPLETED.name());
            this.updateById(payPlan);
            //??????????????????
            log.debug("????????????????????????..........");
            ContractHead contractHead = iContractHeadService.getById(payPlan.getContractHeadId());
            if (contractHead != null) {
                PaymentApplySaveDTO paymentApplySaveDTO = pmClient.getPaymentApplyByParm(new PaymentApplyHeadQueryDTO()
                        .setOrganizationId(contractHead.getOrganizationId())
                        .setVendorId(contractHead.getVendorId())
                        .setSourceType(PaymentPlanSourceTypeEnum.CONTRACT.getValue()));
                PaymentApplyHead paymentApplyHead = paymentApplySaveDTO.getPaymentApplyHead();
                List<PaymentApplyLine> paymentApplyLines = paymentApplySaveDTO.getPaymentApplyLines();
                //?????????????????????????????????
                if (paymentApplyHead == null) {
                    //?????????????????????
                    //?????????????????????????????????
                    paymentApplyHead = new PaymentApplyHead();
                    getPaymentApplyHead(contractHead, paymentApplyHead);
                    //?????????????????????????????????
                    paymentApplyLines = new ArrayList<>();
                    getPaymentApplyLines(payPlan, contractHead, paymentApplyLines);
                    paymentApplySaveDTO.setPaymentApplyHead(paymentApplyHead)
                            .setPaymentApplyLines(paymentApplyLines);
                    pmClient.savePaymentApply(paymentApplySaveDTO);
                } else {
                    //????????????????????????????????????????????????????????????????????????
                    if (!CollectionUtils.isEmpty(paymentApplyLines)) {
                        List<PaymentApplyLine> collect = paymentApplyLines.stream().filter(paymentApplyLine -> payPlan.getPayStage().compareTo(paymentApplyLine.getPayStage()) == 0
                                && payPlan.getPayType().equals(paymentApplyLine.getPayType())).collect(Collectors.toList());
                        if (collect.isEmpty()) {
                            //?????????????????????????????????
                            getPaymentApplyLines(payPlan, contractHead, paymentApplyLines);
                            paymentApplySaveDTO.setPaymentApplyHead(paymentApplyHead)
                                    .setPaymentApplyLines(paymentApplyLines);
                            pmClient.savePaymentApply(paymentApplySaveDTO);
                        }else {
                            log.info("???????????????????????????:{}",collect);
                        }
                    }
                }
                log.debug("...........????????????????????????");
            }
        }
    }

    @Override
    @Transactional
    public void batchDeleteSecond(List<Long> payPlanIds) {
        if (!CollectionUtils.isEmpty(payPlanIds)) {
            this.removeByIds(payPlanIds);
            //?????????????????????value???payPlan??????
            removePayPlansInModelValue(payPlanIds);
        }

    }

    private void removePayPlansInModelValue(List<Long> payPlanIds) {
        Long payPlanId = payPlanIds.get(0);
        PayPlan payPlan = this.getById(payPlanId);
        if (payPlan != null) {
            Long contractHeadId = payPlan.getContractHeadId();
            ModelLine modelLine = iModelLineService.getOne(new QueryWrapper<>(new ModelLine()
                    .setContractHeadId(contractHeadId)
                    .setModelKey(ModelKey.payPlan.name())));
            String payPlanJsonString = modelLine.getModelValue();
            List<PayPlan> payPlans = JsonUtil.parseJsonStrToList(payPlanJsonString, PayPlan.class);
            Iterator<PayPlan> iterator = payPlans.iterator();
            while (iterator.hasNext()) {
                PayPlan next = iterator.next();
                for (Long planId : payPlanIds) {
                    if (planId.compareTo(next.getPayPlanId()) == 0) {
                        iterator.remove();
                        break;
                    }
                }
            }
            payPlanJsonString = JsonUtil.arrayToJsonStr(payPlans);
            modelLine.setModelValue(payPlanJsonString);
            iModelLineService.updateById(modelLine);
        }
    }

    private void getPaymentApplyHead(ContractHead contractHead, PaymentApplyHead paymentApplyHead) {
        BeanUtils.copyProperties(contractHead, paymentApplyHead);
        paymentApplyHead.setCurrency(contractHead.getCurrencyName());
        paymentApplyHead.setBankAccount(contractHead.getSecondBankAccount());
        paymentApplyHead.setOpeningBank(contractHead.getSecondOpeningBank());
        paymentApplyHead.setBankAccountName(contractHead.getSecondBankAccountName());
    }

    private void getPaymentApplyLines(PayPlan payPlan, ContractHead contractHead, List<PaymentApplyLine> paymentApplyLines) {
        if (!CollectionUtils.isEmpty(paymentApplyLines)) {
            for (PaymentApplyLine paymentApplyLine : paymentApplyLines) {
                paymentApplyLine.setSourceType(PaymentPlanSourceTypeEnum.CONTRACT.getValue())
                        .setSourceNumber(contractHead.getContractNo())
                        .setSourceId(contractHead.getContractHeadId())
                        .setPayStage(payPlan.getPayStage())
                        .setPayType(payPlan.getPayType())
                        .setPayMethod(payPlan.getPayMethod())
                        .setTaxRate(payPlan.getPayTax())
                        .setTotalAmountNoTax(payPlan.getExcludeTaxPayAmount());
            }
        } else {
            paymentApplyLines.add(new PaymentApplyLine()
                    .setSourceType(PaymentPlanSourceTypeEnum.CONTRACT.getValue())
                    .setSourceNumber(contractHead.getContractNo())
                    .setSourceId(contractHead.getContractHeadId())
                    .setPayStage(payPlan.getPayStage())
                    .setPayType(payPlan.getPayType())
                    .setPayMethod(payPlan.getPayMethod())
                    .setTaxRate(payPlan.getPayTax())
                    .setTotalAmountNoTax(payPlan.getExcludeTaxPayAmount()));
        }
    }
}
