package com.midea.cloud.srm.ps.anon.external.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.cm.PaymentStatus;
import com.midea.cloud.common.enums.pm.ps.AdvanceApplyStatus;
import com.midea.cloud.common.enums.pm.ps.BoeTypeCode;
import com.midea.cloud.common.enums.pm.ps.FsscStatusCode;
import com.midea.cloud.common.enums.pm.ps.InvoiceStatus;
import com.midea.cloud.common.enums.supcooperate.InvoiceImportStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.JsonUtil;
import com.midea.cloud.srm.feign.api.ApiClient;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.supcooperate.SupcooperateClient;
import com.midea.cloud.srm.model.api.interfacelog.dto.InterfaceLogDTO;
import com.midea.cloud.srm.model.base.external.entity.ExternalInterfaceLog;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.pm.ps.advance.entity.AdvanceApplyHead;
import com.midea.cloud.srm.model.pm.ps.anon.external.FsscStatus;
import com.midea.cloud.srm.model.pm.ps.http.FSSCResult;
import com.midea.cloud.srm.model.pm.ps.payment.entity.CeeaPaymentApplyHead;
import com.midea.cloud.srm.model.suppliercooperate.invoice.entity.OnlineInvoiceAdvance;
import com.midea.cloud.srm.ps.advance.service.IAdvanceApplyHeadService;
import com.midea.cloud.srm.ps.anon.external.mapper.FsscStatusMapper;
import com.midea.cloud.srm.ps.anon.external.service.IFsscStatusService;
import com.midea.cloud.srm.ps.payment.service.ICeeaPaymentApplyHeadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * <pre>
 *  FSSC??????????????? ???????????????
 * </pre>
 *
 * @author chensl26@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-09-02 08:47:44
 *  ????????????:
 * </pre>
 */
@Slf4j
@Service
public class FsscStatusServiceImpl extends ServiceImpl<FsscStatusMapper, FsscStatus> implements IFsscStatusService {

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private IAdvanceApplyHeadService iAdvanceApplyHeadService;

    @Autowired
    private ICeeaPaymentApplyHeadService iCeeaPaymentApplyHeadService;

    @Autowired
    private SupcooperateClient supcooperateClient;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private ApiClient apiClient;


    private static final String KEY_STATUS = "fstatus";
    private static final String KEY_MESSAGE = "message";

    private void logFSSC(Object requestBody ,
                         Map<String,Object> responseBody ,
                         String interfaceName ,
                         String url,
                         String businessNo){
        // ???????????????
        InterfaceLogDTO interfaceLogDTO = new InterfaceLogDTO();
        interfaceLogDTO.setServiceName(interfaceName); // ??????
        interfaceLogDTO.setServiceType("HTTP"); // ????????????
        interfaceLogDTO.setType("RECEIVE"); // ??????
        interfaceLogDTO.setDealTime(1L);
        interfaceLogDTO.setBillId(businessNo);
        interfaceLogDTO.setServiceInfo(JSON.toJSONString(requestBody)); // ??????
        interfaceLogDTO.setCreationDateBegin(new Date()); // ????????????
        interfaceLogDTO.setCreationDateEnd(new Date()); // ????????????
        interfaceLogDTO.setUrl(url);
        interfaceLogDTO.setReturnInfo(JSON.toJSONString(responseBody)); // ??????
        interfaceLogDTO.setFinishDate(new Date()); // ????????????
        if(Objects.nonNull(responseBody.get(KEY_STATUS)) && YesOrNo.YES.getValue().equals(responseBody.get(KEY_STATUS))){
            interfaceLogDTO.setStatus("SUCCESS"); // ??????
        }else {
            interfaceLogDTO.setStatus("FAIL");
            interfaceLogDTO.setErrorInfo(JSON.toJSONString(responseBody));
        }
        apiClient.createInterfaceLog(interfaceLogDTO);
    }

    @Override
    @Transactional
    public Map<String, Object> receiveData(FsscStatus fsscStatus) {
        log.info("??????????????????????????????:==========>" + JsonUtil.entityToJsonStr(fsscStatus));
        Map<String, Object> resultMap = new HashMap<>();
        String businessNo = fsscStatus.getFsscNo();
        try {
            boolean error = false;
            if (fsscStatus == null) {
                resultMap.put(KEY_STATUS, YesOrNo.NO.getValue());
                resultMap.put(KEY_MESSAGE, "????????????????????????");
                error = true;
            }
            if (StringUtils.isBlank(fsscStatus.getFsscNo())) {
                resultMap.put(KEY_STATUS, YesOrNo.NO.getValue());
                resultMap.put(KEY_MESSAGE, "FSSC??????????????????");
                error = true;
            }
            if (StringUtils.isBlank(fsscStatus.getPeripheralSystemNum())) {
                resultMap.put(KEY_STATUS, YesOrNo.NO.getValue());
                resultMap.put(KEY_MESSAGE, "???????????????????????????");
                error = true;
            }
            if (StringUtils.isBlank(fsscStatus.getStatus())) {
                resultMap.put(KEY_STATUS, YesOrNo.NO.getValue());
                resultMap.put(KEY_MESSAGE, "??????????????????");
                error = true;
            }
            if(!error){
                //???????????????????????????,??????srm?????????????????? ToDo
                updateSrmStatus(fsscStatus, resultMap);
                log.info("????????????srm??????????????????:===============>" + JsonUtil.entityToJsonStr(fsscStatus));
                //??????????????????????????????
                this.save(fsscStatus.setFsscStatusId(IdGenrator.generate()));
                log.info("????????????????????????????????????:===============>" + JsonUtil.entityToJsonStr(fsscStatus));
                resultMap.put(KEY_STATUS , YesOrNo.YES.getValue());
                resultMap.put(KEY_MESSAGE , "????????????????????????!");
            }
        }catch (Exception e){
            log.error("???????????????????????????????????????" , e);
            resultMap.put(KEY_STATUS , YesOrNo.NO.getValue());
            resultMap.put(KEY_MESSAGE , "????????????????????????!");
            resultMap.put("errorMsg",e.getStackTrace());
        }finally {
            try {
                logFSSC(fsscStatus ,
                        resultMap ,
                        "?????????????????????????????????",
                        "/api-pm/ps-anon/external/fsscStatus/receiveData" ,
                        businessNo);
                resultMap.put("errorMsg" , "");
            }catch (Exception e){
                log.error("??????????????????????????????",e);
                resultMap.put(KEY_STATUS , "N");
                resultMap.put(KEY_MESSAGE , "????????????????????????SRM?????????");
            }
        }
        return resultMap;
    }

    @Override
    public Map<String, Object> download(String fileuploadId) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            log.info("--------------------------------------??????????????????-------------------------------------------");
            fileCenterClient.downloadFileByParamForAnon(new Fileupload().setFileuploadId(Long.valueOf(fileuploadId)));
            resultMap.put(KEY_STATUS, YesOrNo.YES.getValue());
            resultMap.put(KEY_MESSAGE, "SUCCESS");
            log.info("--------------------------------------??????????????????---------------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("??????????????????:==============================>", e);
            resultMap.put(KEY_STATUS, YesOrNo.NO.getValue());
            resultMap.put(KEY_MESSAGE, "??????????????????,????????????????????????!");
        }
        return resultMap;
    }

    private void updateSrmStatus(FsscStatus fsscStatus, Map<String, Object> resultMap) {
        if (BoeTypeCode.FOREIGN_ADVANCE_PAYMENT.name().equals(fsscStatus.getBoeTypeCode())) {
            //1.??????????????????????????????
            //???????????????????????????,????????????????????????
            String status = fsscStatus.getStatus();
            String fsscNo = fsscStatus.getFsscNo();
            if (FsscStatusCode.FSSC_STATUS_10.getValue().equals(status)) {
                UpdateWrapper<AdvanceApplyHead> updateWrapper = new UpdateWrapper<>(new AdvanceApplyHead().setBoeNo(fsscNo));
                iAdvanceApplyHeadService.update(new AdvanceApplyHead().setAdvanceApplyStatus(AdvanceApplyStatus.DRAFT.getValue()), updateWrapper);
            } else if (FsscStatusCode.FSSC_STATUS_11.getValue().equals(status)) {
                UpdateWrapper<AdvanceApplyHead> updateWrapper = new UpdateWrapper<>(new AdvanceApplyHead().setBoeNo(fsscNo));
                iAdvanceApplyHeadService.update(new AdvanceApplyHead().setAdvanceApplyStatus(AdvanceApplyStatus.REJECTED.getValue()), updateWrapper);
            } else if (FsscStatusCode.FSSC_STATUS_30.getValue().equals(status)) {
                UpdateWrapper<AdvanceApplyHead> updateWrapper = new UpdateWrapper<>(new AdvanceApplyHead().setBoeNo(fsscNo));
                AdvanceApplyHead advanceApplyHead = iAdvanceApplyHeadService.getOne(updateWrapper);
                BigDecimal applyPayAmount = advanceApplyHead.getApplyPayAmount();
                iAdvanceApplyHeadService.update(new AdvanceApplyHead()
                        .setAdvanceApplyStatus(AdvanceApplyStatus.APPROVAL.getValue())
                        .setUsableAmount(applyPayAmount), updateWrapper);
            } else if (FsscStatusCode.FSSC_STATUS_100.getValue().equals(status)) {

            }
            log.info("----------------------------------????????????????????????????????????---------------------");
        }
        if (BoeTypeCode.PURCHASE_BOE_LGi.name().equals(fsscStatus.getBoeTypeCode())) {
            //2.?????????????????????
            //???????????????????????????,????????????????????????
            try {
                updatePurchaseBoeLgi(fsscStatus);
                log.info("-------------------------------?????????????????????????????????-----------------------------");
            } catch (Exception e) {
                e.printStackTrace();
                resultMap.put(KEY_STATUS, YesOrNo.NO.getValue());
                resultMap.put(KEY_MESSAGE, "???????????????????????????????????????,????????????????????????!");
                log.error("??????????????????????????????????????????:====================>", e);
            }
        }
        if (BoeTypeCode.FOREIGN_PAYMENT_BOE.name().equals(fsscStatus.getBoeTypeCode()) ||
                BoeTypeCode.AGENCY_PAYMENT_BOE.name().equals(fsscStatus.getBoeTypeCode())) {
            //3.??????????????? ToDo
            //???????????????????????????,???????????????????????? ToDo
            String status = fsscStatus.getStatus();
            String fsscNo = fsscStatus.getFsscNo();
            if (FsscStatusCode.FSSC_STATUS_10.getValue().equals(status)) {
                UpdateWrapper<CeeaPaymentApplyHead> updateWrapper = new UpdateWrapper<>(new CeeaPaymentApplyHead().setBoeNo(fsscNo));
                iCeeaPaymentApplyHeadService.update(new CeeaPaymentApplyHead().setReceiptStatus(PaymentStatus.DRAFT.getKey()), updateWrapper);
            } else if (FsscStatusCode.FSSC_STATUS_11.getValue().equals(status)) {
                UpdateWrapper<CeeaPaymentApplyHead> updateWrapper = new UpdateWrapper<>(new CeeaPaymentApplyHead().setBoeNo(fsscNo));
                iCeeaPaymentApplyHeadService.update(new CeeaPaymentApplyHead().setReceiptStatus(PaymentStatus.REJECT.getKey()), updateWrapper);
            } else if (FsscStatusCode.FSSC_STATUS_30.getValue().equals(status)) {
                UpdateWrapper<CeeaPaymentApplyHead> updateWrapper = new UpdateWrapper<>(new CeeaPaymentApplyHead().setBoeNo(fsscNo));
                iCeeaPaymentApplyHeadService.update(new CeeaPaymentApplyHead().setReceiptStatus(PaymentStatus.APPROVAL.getKey()), updateWrapper);
            } else if (FsscStatusCode.FSSC_STATUS_100.getValue().equals(status)) {

            }
        }
    }

    private void updatePurchaseBoeLgi(FsscStatus fsscStatus) {
        String status = fsscStatus.getStatus();
        String fsscNo = fsscStatus.getFsscNo();
        if (FsscStatusCode.FSSC_STATUS_10.getValue().equals(status)) {//????????????,?????????????????????????????????
            supcooperateClient.statusReturnForAnon(fsscNo, InvoiceStatus.DRAFT.name());
        } else if (FsscStatusCode.FSSC_STATUS_11.getValue().equals(status)) {//????????????,????????????????????????????????????
            List<OnlineInvoiceAdvance> onlineInvoiceAdvances = supcooperateClient.listOnlineInvoiceAdvanceByFsscNoForAnon(fsscNo);
            if (CollectionUtils.isNotEmpty(onlineInvoiceAdvances)) {
                for (OnlineInvoiceAdvance onlineInvoiceAdvance : onlineInvoiceAdvances) {
                    if (onlineInvoiceAdvance == null) continue;
                    //???????????????,????????????????????????????????????????????????
                    AdvanceApplyHead advanceApplyHead = iAdvanceApplyHeadService.getById(onlineInvoiceAdvance.getAdvanceApplyHeadId());
                    BigDecimal usableAmount = advanceApplyHead.getUsableAmount();//?????????????????????
                    BigDecimal chargeOffAmount = onlineInvoiceAdvance.getChargeOffAmount();//??????????????????
                    onlineInvoiceAdvance.setUsableAmount(usableAmount.add(chargeOffAmount));
                    iAdvanceApplyHeadService.setQuote(onlineInvoiceAdvance, YesOrNo.YES.getValue());
                }
                supcooperateClient.updateOnlineInvoiceAdvanceByParamForAnon(onlineInvoiceAdvances);
            }
            supcooperateClient.statusReturnForAnon(fsscNo, InvoiceStatus.REJECTED.name());
        } else if (FsscStatusCode.FSSC_STATUS_30.getValue().equals(status)) {//????????????,????????????????????????????????????
            //?????????????????????????????????
            List<OnlineInvoiceAdvance> onlineInvoiceAdvances = supcooperateClient.listOnlineInvoiceAdvanceByFsscNoForAnon(fsscNo);
            if (CollectionUtils.isNotEmpty(onlineInvoiceAdvances)) {
                for (OnlineInvoiceAdvance onlineInvoiceAdvance : onlineInvoiceAdvances) {
                    if (onlineInvoiceAdvance == null) continue;
                    if (BigDecimal.ZERO.compareTo(onlineInvoiceAdvance.getUsableAmount()) != 0) {
                        iAdvanceApplyHeadService.setQuote(onlineInvoiceAdvance, YesOrNo.NO.getValue());
                    }
                }
            }
            supcooperateClient.statusReturnForAnon(fsscNo, InvoiceStatus.REVIEWED.name());
            log.info("----------------------------------?????????????????????????????????------------------------------");
        } else if (FsscStatusCode.FSSC_STATUS_100.getValue().equals(status)) {
            supcooperateClient.importStatusReturnForAnon(fsscNo, InvoiceImportStatus.IMPORTED.name());
            log.info("----------------------------------???????????????????????????----------------------------------");
        }
    }

}
