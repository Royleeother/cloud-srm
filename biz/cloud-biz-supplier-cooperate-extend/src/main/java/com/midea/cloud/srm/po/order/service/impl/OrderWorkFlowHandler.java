package com.midea.cloud.srm.po.order.service.impl;

import com.midea.cloud.common.enums.ApproveStatusType;
import com.midea.cloud.common.enums.flow.CbpmOperationTypeEnum;
import com.midea.cloud.common.workflow.WorkFlowFunctionHandler;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.Order;
import com.midea.cloud.srm.supcooperate.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <pre>
 * 订单回调类
 * </pre>
 *
 * @author tanjl11@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/8/13 10:29
 *  修改内容:
 * </pre>
 */
@Component
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class OrderWorkFlowHandler extends WorkFlowFunctionHandler {

    @Autowired
    private IOrderService orderService;

    @Override
    public Map<String, Object> getFormData(Long businessId, Map<String, Object> mapEventData) throws Exception {
        log.info(this.getClass() + "获取表单事件");
        Map<String, Object> formDataMap = new HashMap();
        if (!Objects.isNull(businessId)) {
//            Order sample = client.getOrderById(businessId);
            Order sample = orderService.getById(businessId);
            if (!Objects.isNull(sample)) {
                formDataMap.put("number", sample.getOrderId());
                formDataMap.put("createdFullName", sample.getCreatedBy());
            }

        }
        return formDataMap;
    }

    @Override
    public Map<String, Object> draftEvent(Long businessId, Map<String, Object> mapEventData) throws Exception {
        return null;
    }

    @Override
    public String draftSubmitEvent(Long businessId, Map<String, Object> mapEventData) throws Exception {
        log.info(this.getClass() + "起草提交事件");
        Boolean result = false;
        if (null != businessId) {
//            Order order = client.getOrderByIdFromIner(businessId);
            Order order = orderService.getById(businessId);
            result = getParamAndUpdateStatus(mapEventData, ApproveStatusType.SUBMITTED, order);
        }
        return result ? "success" : "fail";
    }

    @Override
    public String processEndEvent(Long businessId, Map<String, Object> mapEventData) throws Exception {
        log.info(this.getClass() + "流程结束事件");
        Boolean result = false;
//        Order order = client.getOrderByIdFromIner(businessId);
        Order order = orderService.getById(businessId);
        if (order != null) {
            result = getParamAndUpdateStatus(mapEventData, ApproveStatusType.APPROVED, order);
        }
        return result ? "success" : "fail";
    }

    @Override
    public String handleRefuseEvent(Long businessId, Map<String, Object> mapEventData) throws Exception {
        if (businessId == null) {
            return "fail";
        }
//        Order order = client.getOrderByIdFromIner(businessId);
        Order order = orderService.getById(businessId);
        return getParamAndUpdateStatus(mapEventData, ApproveStatusType.SUBMITTED, order) ?
                "success" : "fail";
    }

    @Override
    public String draftAbandonEvent(Long businessId, Map<String, Object> mapEventData) throws Exception {
        if (businessId == null) {
            return "fail";
        }
//        Order order = client.getOrderByIdFromIner(businessId);
        Order order = orderService.getById(businessId);
        return getParamAndUpdateStatus(mapEventData, ApproveStatusType.ABANDONED, order) ?
                "success" : "fail";
    }

    @Override
    public String activityStartEvent(Long businessId, Map<String, Object> mapEventData) throws Exception {
        String operationtype = String.valueOf(mapEventData.get("operationtype"));
//        Order order = client.getOrderByIdFromIner(businessId);
        Order order = orderService.getById(businessId);
        //驳回到起草人节点
        if (CbpmOperationTypeEnum.HANDLER_REFUSE.getKey().equals(operationtype)) {
            return getParamAndUpdateStatus(mapEventData, ApproveStatusType.REJECTED, order) ? "success" : "fail";
        }
        return "fail";

    }

    private Boolean getParamAndUpdateStatus(Map<String, Object> mapEventData, ApproveStatusType type, Order sample) {
        if (sample != null) {
            sample.setApproveStatus(type.getValue());
            Map<String, Object> bodyMap = (null != mapEventData.get("body") ? (Map<String, Object>) mapEventData.get("body") : null);
            if (bodyMap != null && null != bodyMap.get("processParam")) {
                Map<String, Object> processParamMap = (Map<String, Object>) bodyMap.get("processParam");
                sample.setCbpmInstaceId(String.valueOf(processParamMap.get("processId")))
                        .setLastUpdateDate(new Date())
                        .setLastUpdatedBy(String.valueOf(processParamMap.get("handlerId")))
                        .setLastUpdatedByIp(String.valueOf(processParamMap.get("handlerIp")));
//                client.updateOrderByIdFromIner(sample);
                orderService.updateById(sample);
            }
            return true;
        }
        return false;
    }
}
