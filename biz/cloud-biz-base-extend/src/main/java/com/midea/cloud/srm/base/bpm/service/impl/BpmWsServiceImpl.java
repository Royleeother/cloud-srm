package com.midea.cloud.srm.base.bpm.service.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.enums.ApproveStatusType;
import com.midea.cloud.common.enums.PriceApprovalStatus;
import com.midea.cloud.common.enums.VendorAssesFormStatus;
import com.midea.cloud.common.enums.bargaining.projectmanagement.projectpublish.BiddingApprovalStatus;
import com.midea.cloud.common.enums.bpm.TempIdToModuleEnum;
import com.midea.cloud.common.enums.contract.ContractStatus;
import com.midea.cloud.common.enums.pm.po.PurchaseOrderEnum;
import com.midea.cloud.common.enums.pm.po.StatusForBpmEnum;
import com.midea.cloud.common.enums.pm.pr.requirement.RequirementApproveStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.common.utils.NamedThreadFactory;
import com.midea.cloud.srm.base.bpm.service.IBpmWsService;
import com.midea.cloud.srm.base.workflow.service.ISrmFlowBusWorkflowService;
import com.midea.cloud.srm.feign.bid.BidClient;
import com.midea.cloud.srm.feign.contract.ContractClient;
import com.midea.cloud.srm.feign.inq.InqClient;
import com.midea.cloud.srm.feign.perf.PerformanceClient;
import com.midea.cloud.srm.feign.pm.PmClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.feign.supplierauth.SupplierAuthClient;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.Biding;
import com.midea.cloud.srm.model.base.soap.bpm.pr.Entity.Header;
import com.midea.cloud.srm.model.base.soap.bpm.pr.dto.FormForBpmRequest;
import com.midea.cloud.srm.model.base.soap.bpm.pr.dto.FormForBpmResponse;
import com.midea.cloud.srm.model.cm.accept.dto.AcceptDTO;
import com.midea.cloud.srm.model.cm.accept.dto.AcceptOrderDTO;
import com.midea.cloud.srm.model.cm.accept.entity.AcceptOrder;
import com.midea.cloud.srm.model.cm.contract.dto.ContractHeadDTO;
import com.midea.cloud.srm.model.cm.contract.entity.ContractHead;
import com.midea.cloud.srm.model.inq.price.entity.ApprovalHeader;
import com.midea.cloud.srm.model.inq.price.vo.ApprovalAllVo;
import com.midea.cloud.srm.model.perf.vendorasses.VendorAssesForm;
import com.midea.cloud.srm.model.pm.pr.requirement.vo.RequirementApplyRejectVO;
import com.midea.cloud.srm.model.supplier.change.dto.ChangeInfoDTO;
import com.midea.cloud.srm.model.supplier.change.entity.InfoChange;
import com.midea.cloud.srm.model.supplier.info.dto.InfoDTO;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.model.supplier.vendorimport.dto.VendorImportSaveDTO;
import com.midea.cloud.srm.model.supplier.vendorimport.entity.VendorImport;
import com.midea.cloud.srm.model.supplierauth.review.entity.ReviewForm;
import com.midea.cloud.srm.model.supplierauth.review.entity.SiteForm;
import com.midea.cloud.srm.model.suppliercooperate.order.dto.OrderSaveRequestDTO;
import com.midea.cloud.srm.model.workflow.entity.SrmFlowBusWorkflow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.jws.WebService;
import java.util.Date;
import java.util.concurrent.*;

//import com.midea.cloud.gateway.controller.TokenController;

/**
 * <pre>
 *  ????????????????????????
 * </pre>
 *
 * @author chenwt24@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-10-10
 *  ????????????:
 * </pre>
 */
@Slf4j
@WebService(targetNamespace = "http://www.aurora-framework.org/schema",
        endpointInterface = "com.midea.cloud.srm.base.bpm.service.IBpmWsService")
@Component("iBpmWsService")
public class BpmWsServiceImpl implements IBpmWsService {

    @Resource
    private PmClient pmClient;

    @Resource
    private ContractClient contractClient;
    @Resource
    private SupplierAuthClient supplierAuthClient;
    @Resource
    private SupplierClient supplierClient;

    @Autowired
    private ISrmFlowBusWorkflowService workflowService;
    @Resource
    private PerformanceClient performanceClient;
    @Resource
    private com.midea.cloud.srm.feign.bargaining.BidClient bidClient;
    @Resource
    private BidClient bidexClient;
    @Resource
    private InqClient InqClient;

    private final ThreadPoolExecutor ioThreadPool;

    private final ForkJoinPool calculateThreadPool;


    //???????????????
    public BpmWsServiceImpl() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        ioThreadPool = new ThreadPoolExecutor(cpuCount * 2 + 1, cpuCount * 2 + 1,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue(),
                new NamedThreadFactory("?????????-http-sender", true), new ThreadPoolExecutor.CallerRunsPolicy());
        calculateThreadPool = new ForkJoinPool(cpuCount + 1);
    }

    @Override
    public FormForBpmResponse execute(FormForBpmRequest request) {
        FormForBpmRequest.RequestInfo requestInfo = request.getRequestInfo();

        FormForBpmResponse response = new FormForBpmResponse();
        FormForBpmResponse.RESPONSE responses = new FormForBpmResponse.RESPONSE();

        FormForBpmRequest.EsbInfo esbInfo = request.getEsbInfo();

        String instId = "";
        String requestTime = "";
        if (null != esbInfo) {
            instId = esbInfo.getInstId();
            requestTime = esbInfo.getRequestTime();
        }
        log.info("============================================================????????????============================================================");
        log.info("bpm????????????????????????: " + (null != request ? request.toString() : "???"));
        //????????????
        Header header = requestInfo.getContext().getApprove().getHeader();
        String formId = header.getFormId();
        String formInstanceId = header.getFormInstanceId();
        String status = header.getStatus();
        //String opinion = header.getOpinion();

        //??????????????????
        String responseMessage = "SUCCESSED";
        String returnStatus = "S";
        String returnCode = "S";
        String returnMsg = "????????????";


        try {
            SrmFlowBusWorkflow one = workflowService.getOne(Wrappers.lambdaQuery(SrmFlowBusWorkflow.class).eq(SrmFlowBusWorkflow::getFlowInstanceId, formInstanceId));
            Assert.isTrue(!ObjectUtils.isEmpty(one), "ID???????????????????????????");
            header.setFormInstanceId(one.getFormInstanceId());
            //????????????
            Assert.isTrue(StringUtils.isNotBlank(formInstanceId), "DOCUMENT_TYPE,????????????");
            Assert.isTrue(StringUtils.isNotEmpty(formId), "??????ID????????????");
            Assert.isTrue(StringUtils.isNotBlank(status), "????????????????????????");

            String valueByCode = TempIdToModuleEnum.getValueByCode(formId);

            //????????????
            String srmStatus = StatusForBpmEnum.getValueByCode(status);
            if (!RequirementApproveStatus.APPROVED.getValue().equals(srmStatus) &&
                    RequirementApproveStatus.REJECTED.getValue().equals(srmStatus) &&
                    RequirementApproveStatus.WITHDRAW.getValue().equals(srmStatus)) {
                throw new BaseException("??????????????????");
            }

            //??????????????????
            header.setStatus(srmStatus);

            if (StringUtils.equals(valueByCode, TempIdToModuleEnum.REQUIREMENT.getValue())) {
                doRequirement(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.ORDER.getValue())) {
                doOrder(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.CONTRACT.getValue())) {
                doContract(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.REVIEW.getValue())) {
                doReview(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.SUPPLIERAUTH.getValue())) {
                doSupplierauth(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.CHANGE.getValue())) {
                doChange(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.IMPORT.getValue())) {
                doImport(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.ACCEPT.getValue())) {
                doAccept(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.COMPANYINFO.getValue())) {
                doCompanyinfo(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.VENDORASSES.getValue())) {
                docVendorasses(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.FORCOMPARISON.getValue())) {
                doBiding(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.BIDING.getValue())) {
                docForComparison(header, one);
            } else if (StringUtils.equals(valueByCode, TempIdToModuleEnum.APPROVAL.getValue())) {
                doApproval(header, one);
            } else {
                Assert.isTrue(false, "ES_RETURN_ID????????????ID???" + formId + "?????????");
            }
            //????????????
            one.setSrmOrderStatus(srmStatus);
            workflowService.updateById(one);

        } catch (Exception e) {
            log.error(e.toString());
            responseMessage = "FAILURE";
            returnStatus = "N";
            returnCode = "N";
            returnMsg = "??????????????????????????????" + e.getMessage();
        }

        String responseTime = DateUtil.format(new Date());

        //???????????????
        FormForBpmResponse.ESBINFO esbinfo = new FormForBpmResponse.ESBINFO();
        esbinfo.setInstId(instId);
        esbinfo.setReturnStatus(returnStatus);
        esbinfo.setReturnCode(returnCode);
        esbinfo.setReturnMsg(returnMsg);
        esbinfo.setRequestTime(requestTime);
        esbinfo.setResponseTime(responseTime);
        responses.setEsbInfo(esbinfo);

        //???????????????
        FormForBpmResponse.RESULTINFO resultInfo = new FormForBpmResponse.RESULTINFO();
        resultInfo.setResponseMessage(responseMessage);
        resultInfo.setResponseStatus(responseMessage);
        responses.setResultInfo(resultInfo);
        response.setResponse(responses);

        return response;
    }

    /**
     * 1???????????????
     *
     * @param header
     * @throws Exception
     */
    public void doRequirement(Header header, SrmFlowBusWorkflow one) throws Exception {
        //????????????
        String formInstanceId = header.getFormInstanceId();
        String srmStatus = header.getStatus();
        //String opinion = header.getOpinion();
        Long requirementHeadId = Long.parseLong(formInstanceId);
        RequirementApplyRejectVO requirementApplyRejectVO = new RequirementApplyRejectVO();
        requirementApplyRejectVO.setRequirementHeadId(requirementHeadId);
        //requirementApplyRejectVO.setRequirementHeadId(requirementHeadId).setRejectReason(opinion);

        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                //?????????????????????????????????????????????
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    pmClient.approval(requirementHeadId);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    pmClient.reject(requirementApplyRejectVO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    pmClient.withdraw(requirementApplyRejectVO);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 2???????????????
     *
     * @param header
     * @throws Exception
     */
    public void doOrder(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long orderId;
        orderId = Long.parseLong(header.getFormInstanceId());
        OrderSaveRequestDTO orderSaveRequestDTO = pmClient.queryOrderById(orderId);
        Assert.isTrue(!ObjectUtils.isEmpty(orderSaveRequestDTO.getOrder()), "?????????????????????????????????id???" + orderId);
        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                Assert.isTrue(PurchaseOrderEnum.UNDER_APPROVAL.getValue().equals(orderSaveRequestDTO.getOrder().getOrderStatus()), "??????????????????????????????????????????????????????");
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    pmClient.approvalInEditStatus(orderSaveRequestDTO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    pmClient.rejectInEditStatus(orderSaveRequestDTO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    pmClient.withdrawInEditStatus(orderSaveRequestDTO);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 3???????????????
     *
     * @param header
     * @throws Exception
     */
    public void doContract(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long contractHeadId;
        contractHeadId = Long.parseLong(header.getFormInstanceId());
        ContractHead contractHead = contractClient.getContractDTOSecond(contractHeadId, null).getContractHead();
        Assert.isTrue(!ObjectUtils.isEmpty(contractHead), "?????????????????????????????????id???" + contractHeadId);
        String srmStatus = header.getStatus();
        ContractHeadDTO contractHeadDTO = new ContractHeadDTO().setContractHeadId(contractHeadId);
        //?????????????????????????????????????????????
        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    contractClient.buyerApprove(contractHeadId);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    contractClient.buyerRefused(contractHeadDTO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    contractClient.buyerWithdraw(contractHeadDTO);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 4????????????????????????
     *
     * @param header
     * @throws Exception
     */
    public void doReview(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long reviewFormId;
        reviewFormId = Long.parseLong(header.getFormInstanceId());
        ReviewForm reviewForm = supplierAuthClient.getReviewFormDTO(reviewFormId).getReviewForm();
        Assert.isTrue(!ObjectUtils.isEmpty(reviewForm), "?????????????????????????????????id???" + reviewFormId);
        String srmStatus = header.getStatus();

        //?????????????????????????????????????????????
        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    supplierAuthClient.ReviewFormPass(reviewForm);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    supplierAuthClient.ReviewFormRejected(reviewForm);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    supplierAuthClient.ReviewFormWithdraw(reviewForm);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 5????????????????????????
     *
     * @param header
     * @throws Exception
     */
    public void doSupplierauth(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long siteFormId;
        siteFormId = Long.parseLong(header.getFormInstanceId());
        SiteForm siteForm = supplierAuthClient.SiteFormGet(siteFormId);
        Assert.isTrue(!ObjectUtils.isEmpty(siteForm), "?????????????????????????????????id???" + siteFormId);
        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                Assert.isTrue(ApproveStatusType.SUBMITTED.getValue().equals(siteForm.getApproveStatus()), "???????????????????????????");
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    supplierAuthClient.SiteFormPass(siteForm);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    supplierAuthClient.SiteFormRejected(siteForm);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    supplierAuthClient.SiteFormWithdraw(siteForm);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 6???????????????????????????????????????????????????????????????????????????
     *
     * @param header
     * @throws Exception
     */
    public void doChange(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long changeId;
        changeId = Long.parseLong(header.getFormInstanceId());
        ChangeInfoDTO changeInfoDTO = supplierClient.getInfoByChangeId(changeId);
        InfoChange infoChange = changeInfoDTO.getInfoChange();
        Assert.isTrue(!ObjectUtils.isEmpty(infoChange), "?????????????????????????????????id???" + changeId);
        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    supplierClient.InfoChangeApprove(changeInfoDTO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    supplierClient.InfoChangeRejected(changeInfoDTO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    supplierClient.InfoChangeWithdraw(changeInfoDTO);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 7???????????????OU??????
     *
     * @param header
     * @throws Exception
     */
    public void doImport(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long importId;
        importId = Long.parseLong(header.getFormInstanceId());
        VendorImportSaveDTO vendorImportDetailDTO = supplierClient.getVendorImportDetail(importId);
        Assert.isTrue(!ObjectUtils.isEmpty(vendorImportDetailDTO), "????????????????????????OU??????????????????id???" + importId);
        VendorImport vendorImport = vendorImportDetailDTO.getVendorImport();
        Assert.isTrue(!ObjectUtils.isEmpty(vendorImport), "?????????????????????OU??????????????????id???" + importId);
        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    supplierClient.VendorImportApprove(importId);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    supplierClient.VendorImportReject(importId);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    supplierClient.VendorImportWithdraw(importId);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 8???????????????&????????????
     *
     * @param header
     * @throws Exception
     */
    public void doAccept(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long acceptOrderId;
        acceptOrderId = Long.parseLong(header.getFormInstanceId());
        AcceptDTO acceptDTO = contractClient.getAcceptDTO(acceptOrderId);
        AcceptOrder acceptOrder = acceptDTO.getAcceptOrder();
        Assert.isTrue(!ObjectUtils.isEmpty(acceptOrder), "??????????????????????????????id???" + acceptOrderId);
        AcceptOrderDTO acceptOrderDTO = new AcceptOrderDTO();
        BeanUtils.copyProperties(acceptOrder, acceptOrderDTO);
        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    contractClient.vendorPass(acceptOrderDTO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    acceptOrderDTO.setRejectReason(header.getOpinion());
                    contractClient.buyerReject(acceptOrderDTO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    contractClient.acceptWithdraw(acceptOrderId);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 9???????????????????????????????????????????????????????????????????????????
     *
     * @param header
     * @throws Exception
     */
    public void doCompanyinfo(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long companyId = Long.parseLong(header.getFormInstanceId());
        InfoDTO infoDTO = supplierClient.getInfoByParam(companyId);
        CompanyInfo companyInfo = infoDTO.getCompanyInfo();
        String status = companyInfo.getStatus();
        Assert.isTrue(!ObjectUtils.isEmpty(companyInfo), "???????????????????????????????????????id???" + companyId);

        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        //????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    supplierClient.companyGreenChannelApprove(infoDTO);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    Assert.isTrue(ApproveStatusType.SUBMITTED.getValue().equals(status), "?????????????????????????????????????????????????????????id???" + companyId);
                    //companyInfo.setCeeaDraftsmanOpinion(header.getOpinion());
                    supplierClient.CompanyInfoReject(companyInfo);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    Assert.isTrue(ApproveStatusType.SUBMITTED.getValue().equals(status), "?????????????????????????????????????????????????????????id???" + companyId);
                    supplierClient.CompanyInfoWithdraw(companyInfo);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 10????????????????????????
     *
     * @param header
     * @throws Exception
     */
    public void docVendorasses(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long vendorAssesId;
        vendorAssesId = Long.parseLong(header.getFormInstanceId());
        VendorAssesForm vendorAssesForm = performanceClient.queryById(vendorAssesId);
        Assert.isTrue(!ObjectUtils.isEmpty(vendorAssesForm), "????????????????????????????????????id???" + vendorAssesId);
        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    performanceClient.VendorAssesFormPass(vendorAssesForm);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    Assert.isTrue(VendorAssesFormStatus.SUBMITTED.getKey().equals(vendorAssesForm.getStatus()), "?????????????????????????????????????????????????????????id???" + vendorAssesId);
                    performanceClient.VendorAssesFormRejected(vendorAssesForm);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    Assert.isTrue(VendorAssesFormStatus.SUBMITTED.getKey().equals(vendorAssesForm.getStatus()), "?????????????????????????????????????????????????????????id???" + vendorAssesId);
                    performanceClient.VendorAssesFormWithdraw(vendorAssesForm);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 11????????????????????????
     *
     * @param header
     * @throws Exception
     */
    public void docForComparison(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long bidingId = Long.parseLong(header.getFormInstanceId());
        Biding forComparison = bidClient.getBargaining(bidingId);
        Assert.isTrue(!ObjectUtils.isEmpty(forComparison), "?????????????????????????????????id???" + bidingId);
        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    forComparison.setAuditStatus(BiddingApprovalStatus.APPROVED.getValue());
                    bidClient.callBackForWorkFlow(forComparison);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    forComparison.setAuditStatus(BiddingApprovalStatus.REJECTED.getValue());
                    //forComparison.setCeeaDrafterOpinion(header.getOpinion());
                    bidClient.callBackForWorkFlow(forComparison);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    forComparison.setAuditStatus(BiddingApprovalStatus.WITHDRAW.getValue());
                    //forComparison.setCeeaDrafterOpinion(header.getOpinion());
                    bidClient.callBackForWorkFlow(forComparison);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 12????????????????????????
     *
     * @param header
     * @throws Exception
     */
    public void doBiding(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long bidingId = Long.parseLong(header.getFormInstanceId());
        com.midea.cloud.srm.model.bid.purchaser.projectmanagement.bidinitiating.entity.Biding forComparison = bidexClient.getBiding(bidingId);
        Assert.isTrue(!ObjectUtils.isEmpty(forComparison), "?????????????????????????????????id???" + bidingId);
        //Assert.isTrue(com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingApprovalStatus.SUBMITTED.getValue().equals(forComparison.getAuditStatus()), "?????????????????????????????????????????????????????????id???" + bidingId);

        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    forComparison.setAuditStatus(com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingApprovalStatus.APPROVED.getValue());
                    bidexClient.callBackForWorkFlow(forComparison);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    forComparison.setAuditStatus(com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingApprovalStatus.REJECTED.getValue());
                    bidexClient.callBackForWorkFlow(forComparison);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    forComparison.setAuditStatus(com.midea.cloud.common.enums.bid.projectmanagement.projectpublish.BiddingApprovalStatus.WITHDRAW.getValue());
                    bidexClient.callBackForWorkFlow(forComparison);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }

    /**
     * 13?????????????????????(???????????????)
     *
     * @param header
     * @throws Exception
     */
    public void doApproval(Header header, SrmFlowBusWorkflow one) throws Exception {
        Long approvalHeaderId = Long.parseLong(header.getFormInstanceId());
        ApprovalAllVo approvalAllVo = InqClient.ceeaGetApprovalDetail(approvalHeaderId);
        ApprovalHeader approvalHeader = approvalAllVo.getApprovalHeader();

        Assert.isTrue(!ObjectUtils.isEmpty(approvalHeader), "????????????????????????????????????id???" + approvalHeaderId);
        String srmStatus = header.getStatus();
        //?????????????????????????????????????????????
        CompletableFuture.runAsync(() -> {
            try {
                if (StringUtils.equals(srmStatus, RequirementApproveStatus.APPROVED.getValue())) {
                    InqClient.auditPass(approvalHeaderId);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.REJECTED.getValue())) {
                    Assert.isTrue(PriceApprovalStatus.RESULT_NOT_APPROVED.getValue().equals(approvalHeader.getStatus()), "?????????????????????????????????????????????????????????id???" + approvalHeaderId);
                    InqClient.reject(approvalHeaderId, null);
                } else if (StringUtils.equals(srmStatus, RequirementApproveStatus.WITHDRAW.getValue())) {
                    Assert.isTrue(PriceApprovalStatus.RESULT_NOT_APPROVED.getValue().equals(approvalHeader.getStatus()), "?????????????????????????????????????????????????????????id???" + approvalHeaderId);
                    InqClient.withdraw(approvalHeaderId);
                }
                one.setSrmOrderStatus("");
                workflowService.updateById(one);
            } catch (Exception e) {
                log.error("?????????????????????:", e);
            }
        }, ioThreadPool);
    }
}
