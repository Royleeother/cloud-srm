//package com.midea.cloud.srm.pr.soap.bpm.service.impl;
//
//
//import com.midea.cloud.common.enums.pm.po.PurchaseOrderEnum;
//import com.midea.cloud.common.enums.pm.po.StatusForBpmEnum;
//import com.midea.cloud.common.enums.pm.pr.requirement.RequirementApproveStatus;
//import com.midea.cloud.common.exception.BaseException;
//import com.midea.cloud.common.utils.DateUtil;
//import com.midea.cloud.srm.model.base.organization.entity.Position;
//import com.midea.cloud.srm.model.base.soap.bpm.pr.Entity.Header;
//import com.midea.cloud.srm.model.base.soap.bpm.pr.dto.FormForBpmRequest;
//import com.midea.cloud.srm.model.base.soap.bpm.pr.dto.FormForBpmResponse;
//import com.midea.cloud.srm.model.base.soap.bpm.pr.dto.PurchaseRequirementRequest;
//import com.midea.cloud.srm.model.base.soap.erp.base.dto.EsbInfoRequest;
//import com.midea.cloud.srm.model.base.soap.erp.base.dto.PositionEntity;
//import com.midea.cloud.srm.model.base.soap.erp.base.dto.PositionRequest;
//import com.midea.cloud.srm.model.base.soap.erp.dto.SoapResponse;
//import com.midea.cloud.srm.model.pm.pr.requirement.dto.PurchaseRequirementDTO;
//import com.midea.cloud.srm.model.pm.pr.requirement.vo.RequirementApplyRejectVO;
//import com.midea.cloud.srm.pr.erp.service.IErpService;
//import com.midea.cloud.srm.pr.requirement.controller.RequirementHeadController;
//import com.midea.cloud.srm.pr.requirement.service.IRequirementHeadService;
//import com.midea.cloud.srm.pr.requirement.service.impl.RequirementHeadServiceImpl;
//import com.midea.cloud.srm.pr.soap.bpm.service.IPurchaseWsService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.Assert;
//import org.springframework.util.ObjectUtils;
//
//import javax.annotation.Resource;
//import javax.jws.WebService;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
///**
// * <pre>
// *  ????????????????????????????????????
// * </pre>
// *
// * @author chenwt24@meicloud.com
// * @version 1.00.00
// *
// * <pre>
// *  ????????????
// *  ???????????????:
// *  ?????????:
// *  ????????????: 2020-10-10
// *  ????????????:
// * </pre>
// */
//@Slf4j
//@WebService(targetNamespace = "http://www.aurora-framework.org/schema",
//        endpointInterface = "com.midea.cloud.srm.pr.soap.bpm.service.IPurchaseWsService")
//@Component("iPurchaseWsService")
//public class PurchaseWsServiceImpl implements IPurchaseWsService {
//    @Autowired
//    private RequirementHeadServiceImpl requirementHeadService;
//
//    @Override
//    public FormForBpmResponse execute(FormForBpmRequest request) {
//        FormForBpmResponse response = new FormForBpmResponse();
//        FormForBpmResponse.RESPONSE responses = new FormForBpmResponse.RESPONSE();
//
//        FormForBpmRequest.EsbInfo esbInfo = request.getEsbInfo();
//
//        String instId = "";
//        String requestTime = "";
//        if (null != esbInfo) {
//            instId = esbInfo.getInstId();
//            requestTime = esbInfo.getRequestTime();
//        }
//
//        FormForBpmRequest.RequestInfo requestInfo = request.getRequestInfo();
//
//        log.info("bpm????????????????????????????????????: " + (null != request ? request.toString() : "???"));
//        //????????????
//        Header header = requestInfo.getContext().getApprove().getHeader();
//        String formId = header.getFormId();
//        String formInstanceId = header.getFormInstanceId();
//        String status = header.getStatus();
//        String opinion = header.getOpinion();
//
//        //??????????????????
//        String responseMessage= "SUCCESSED";
//        String returnStatus= "S";
//        String returnCode= "S";
//        String returnMsg= "????????????";
//
//        try {
//            //????????????
//            Assert.isTrue(StringUtils.isNotBlank(formInstanceId), "????????????DOCUMENT_TYPE,????????????");
//            Assert.isTrue(StringUtils.isNotEmpty(formId),"??????ID????????????");
//            Assert.isTrue(StringUtils.isNotBlank(status), "????????????????????????");
//            Long requirementHeadId;
//            try {
//                requirementHeadId = Long.parseLong(formInstanceId);
//            } catch (Exception e) {
//                throw new BaseException("????????????formInstanceId????????????");
//            }
//            PurchaseRequirementDTO byHeadId = requirementHeadService.getByHeadId(requirementHeadId);
//            Assert.isTrue(!ObjectUtils.isEmpty(byHeadId.getRequirementHead()),"????????????????????????????????????id???"+requirementHeadId);
////
//
////            String orderStatus = byHeadId.getRequirementHead().getAuditStatus();
////            boolean approveStatus = PurchaseOrderEnum.UNDER_APPROVAL.getValue().equals(orderStatus);
////            Assert.isTrue(approveStatus, "??????????????????");
//
//            String srmStatus = StatusForBpmEnum.getValueByCode(status);
//            System.out.println(srmStatus);
//
//                    //?????????????????????????????????????????????
//            if(StringUtils.equals(srmStatus,RequirementApproveStatus.APPROVED.getValue())){
//                requirementHeadService.updateApproved(requirementHeadId,RequirementApproveStatus.APPROVED.getValue());
//            }else if(StringUtils.equals(srmStatus,RequirementApproveStatus.REJECTED.getValue())){
//                requirementHeadService.reject(requirementHeadId,opinion);
//            }else if(StringUtils.equals(srmStatus,RequirementApproveStatus.WITHDRAW.getValue())){
//                requirementHeadService.withdraw(requirementHeadId,opinion);
//            }else {
//                throw new BaseException("??????????????????");
//            }
//
//        }catch (Exception e){
//            responseMessage= "FAILURE";
//            returnStatus= "N";
//            returnCode= "N";
//            returnMsg="??????????????????????????????"+e.getMessage();
//        }
//
//        String responseTime = DateUtil.format(new Date());
//
//        //???????????????
//        FormForBpmResponse.ESBINFO esbinfo = new FormForBpmResponse.ESBINFO();
//        esbinfo.setInstId(instId);
//        esbinfo.setReturnStatus(returnStatus);
//        esbinfo.setReturnCode(returnCode);
//        esbinfo.setReturnMsg(returnMsg);
//        esbinfo.setRequestTime(requestTime);
//        esbinfo.setResponseTime(responseTime);
//        responses.setEsbInfo(esbinfo);
//
//        //???????????????
//        FormForBpmResponse.RESULTINFO resultInfo = new FormForBpmResponse.RESULTINFO();
//        resultInfo.setResponseMessage(responseMessage);
//        resultInfo.setResponseStatus(responseMessage);
//        responses.setResultInfo(resultInfo);
//        response.setResponse(responses);
//
//        return response;
//    }
//}
