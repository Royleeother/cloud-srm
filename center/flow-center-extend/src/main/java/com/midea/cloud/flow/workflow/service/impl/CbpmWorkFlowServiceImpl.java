package com.midea.cloud.flow.workflow.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.midea.cloud.common.enums.flow.CbpmOperationTypeEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.common.utils.idaas.Pbkdf2Encoder;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.component.filter.HttpServletHolder;
import com.midea.cloud.flow.common.constant.FlowCommonConst;
import com.midea.cloud.flow.common.constant.WorkFlowConst;
import com.midea.cloud.flow.common.method.WorkFLowCommonMethod;
import com.midea.cloud.flow.utils.CommonReflectMethod;
import com.midea.cloud.flow.workflow.service.ICbpmWorkFlowService;
import com.midea.cloud.flow.workflow.service.ITemplateHeaderService;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.common.BaseDTO;
import com.midea.cloud.srm.model.flow.cbpm.CbpmProcessNodesDTO;
import com.midea.cloud.srm.model.flow.cbpm.CbpmProcessNodesListDTO;
import com.midea.cloud.srm.model.flow.cbpm.CbpmProcessNodesManualDTO;
import com.midea.cloud.srm.model.flow.process.dto.*;
import com.midea.cloud.srm.model.flow.process.entity.TemplateHeader;
import com.midea.cloud.srm.model.flow.process.entity.TemplateLines;
import com.midea.cloud.srm.model.flow.query.dto.FlowProcessQueryDTO;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 *  cbpm Service???????????????
 * </pre>
 *
 * @author wuwl18@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/3/10 18:01
 *  ????????????:
 * </pre>
 */
@Slf4j
@Service
public class CbpmWorkFlowServiceImpl implements ICbpmWorkFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CbpmWorkFlowServiceImpl.class);

    /**??????????????????Client*/
    @Autowired
    private RbacClient rbacClient;

    /**????????????**/
    @Value("${work-flow.sourceAddr}")
    private String sourceAddr;
    /**????????????*/
    @Value("${work-flow.forwardTo}")
    private String forwardTo;
    /**??????????????????**/
    @Value("${work-flow.serviceName}")
    private String serviceName;
    /**?????????url**/
    @Value("${work-flow.url}")
    private String url;
    /**appId**/
//    @Value("${work-flow.appId}")
//    private String appId = StartPingServiceHandler.APP_ID;
    /**??????*/
//    @Value("${work-flow.salt}")
//    private String salt = StartPingServiceHandler.SALT;
    /**moduleId*/
//    @Value("${work-flow.sysId}")
//    private String sysId = StartPingServiceHandler.SYS_ID;

    /**????????????idssToken??????*/
    @Value("${work-flow.idaasToken}")
    public boolean idaasToken;

    @Autowired
    private RestTemplate restTemplate; // rest????????????

    /**?????????Service??????*/
    @Autowired
    private ITemplateHeaderService iTemplateHeaderService;

    /**Idaas?????????accessKeyId???*/
//    private String accessKeyId = "4JWDHGyOL8MgtC6h7MuRqbPt";
    /**Idaas?????????accessKeySecret???*/
//    private String accessKeySecret = "qFA83z9oCRCcP0eKGIrBlQgQvYcHrt";
    /**Idaas?????????????????????URL???(uat: http://10.16.148.47:8002???sit???http://10.16.81.158:8000)*/
    @Value("${work-flow.idaasUrl}")
    private String idaasUrl;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public Map<String, Object> approveProcess(ApproveProcessDTO approveProcessDTO) throws Exception{
        return this.getBody(WorkFlowConst.APPROVE_PROCESS, approveProcessDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveDraftDirectly(CbpmRquestParamDTO cbpmRquestParamDTO, Map<String, Object> formParam) throws Exception {
        Map<String, Object> processListMap = new HashMap<>();
        processListMap.put("message", ResultCode.OPERATION_FAILED.getMessage());

        //????????????
        if (null == cbpmRquestParamDTO) {
            Assert.isNull(ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        }
        String fdId = cbpmRquestParamDTO.getFdId();
        String subject = cbpmRquestParamDTO.getSubject(); //????????????
        String businessId = cbpmRquestParamDTO.getBusinessId(); //??????ID
        Assert.notNull(subject, WorkFlowConst.SUBUJECT_NOT_NULL);
        Assert.notNull(fdId, WorkFlowConst.TEMPLATE_INSTANCE_NOT_NULL);
        Assert.notNull(businessId, WorkFlowConst.BUSINESS_ID_NOT_NULL);

        String loginName = AppUserUtil.getUserName();
        Assert.notNull(loginName, WorkFlowConst.USER_ID_NOT_NULL);

        //??????fromParam Map??????
        Map<String, Object> newFormParam = new HashMap<>();
        if(null == formParam){
            formParam = new HashMap<>();
        }else if(null != formParam.get("formParam")){
            newFormParam = (Map<String, Object>)formParam.get("formParam");
        }

        //???????????????
        String fdTemplateId = cbpmRquestParamDTO.getBusinessKey();
        //??????????????????ID??????functionCode
        String functionCode = "";
        TemplateHeader queryTemplateHeader = new TemplateHeader();
        queryTemplateHeader.setDeleteFlag(FlowCommonConst.DELETE_FLAG_NO);
        queryTemplateHeader.setModelId(fdTemplateId);
        List<TemplateHeader> templateHeaderList = iTemplateHeaderService.list(new QueryWrapper<>(queryTemplateHeader));
        if(!CollectionUtils.isEmpty(templateHeaderList) && null != templateHeaderList.get(0)){
            functionCode = templateHeaderList.get(0).getTemplateCode();
        }
        Assert.notNull(functionCode, WorkFlowConst.FORM_TEMPLATE_ID);

        if(StringUtils.isNotBlank(fdId)){
            ApproveProcessDTO approveProcess = new ApproveProcessDTO();
            approveProcess.setFdId(fdId);
            approveProcess.setFdTemplateId(fdTemplateId);
            approveProcess.setLoginName(loginName);
//            approveProcess.setFdTemplateCode(fdTemplateId); //???????????????????????????????????????????????????
            //??????????????????
            Map<String, Object> processMap = new HashMap<>();
            processMap.put("operationType", CbpmOperationTypeEnum.DRAFT.getKey());
            if(!CollectionUtils.isEmpty(cbpmRquestParamDTO.getChangeNodeHandler())){
                processMap.put("changeNodeHandler", cbpmRquestParamDTO.getChangeNodeHandler());
            }

            processMap.put("auditNote", cbpmRquestParamDTO.getAuditNote()); //????????????
            approveProcess.setProcessParam(processMap);

            //??????????????????id(?????????????????????????????????,???????????????????????????SUPPLIERAUTH)
            newFormParam.put("fdModuleId", WorkFLowCommonMethod.getModuleIdKeyByFormTemplateId(functionCode));
            newFormParam.put("formTemplateId", functionCode);//??????????????????id(?????????????????????????????????????????????-quaOfReview)
            newFormParam.put("formInstanceId", businessId);  //??????????????????id(??????????????????????????????????????????????????????)
            newFormParam.put("fdUrl","");  //?????????????????????url(?????????????????????????????????url)
            newFormParam.put("subject", subject);  //??????????????????

            //??????????????????(???????????????)
            if(cbpmRquestParamDTO.getSubjectForMultiLanguages() != null){
                newFormParam.put("subjectForMultiLanguages", cbpmRquestParamDTO.getSubjectForMultiLanguages());
            }else{
                String localKey = StringUtil.getUnderlineConverBar(LocaleHandler.getLocaleKey());
                newFormParam.put("mainLanguageType4Subject",localKey);
                List<Map<String, Object>> subjectForMultiLanguages = new ArrayList<>();
                Map<String, Object> reallyTitleMap = new HashMap<>();
                reallyTitleMap.put("languageType", localKey);
                reallyTitleMap.put("subject", subject);
                subjectForMultiLanguages.add(reallyTitleMap);
                newFormParam.put("subjectForMultiLanguages", subjectForMultiLanguages);
            }
            approveProcess.setFormParam(newFormParam);

            Map<String, Object> mapResultBody = this.approveProcess(approveProcess);
            String resultCode = (null != mapResultBody.get("resultCode") ? String.valueOf(mapResultBody.get("resultCode")) : "");
            if(!FlowCommonConst.SUCCESS_CODE_010.equals(resultCode)){
                throw new Exception(mapResultBody.get("resultMsg")+"");
            }

            //????????????????????????????????????????????????ID
            this.getProcessCustomCallBack(Long.parseLong(businessId), fdTemplateId, CbpmOperationTypeEnum.DRAFT_EVENT.getKey(), fdId);

            //??????????????????????????????????????????????????????
            List<Map> processListJson = new ArrayList<>();
            FlowProcessQueryDTO flowProcessQuery = new FlowProcessQueryDTO();
            flowProcessQuery.setLoginName(loginName);
            flowProcessQuery.setFdId(fdId);
            List processList = this.getOperationList(flowProcessQuery);
            if(!CollectionUtils.isEmpty(processList)){
                JSONArray processListArray = (JSONArray) processList;
                processListJson = JSONObject.parseArray(processListArray.toJSONString(), Map.class);
            }
            processListMap.put("operationJsonList", processListJson);
            processListMap.put("message", ResultCode.SUCCESS.getMessage());
            processListMap.put("businessKey", fdTemplateId);
            processListMap.put("businessId", cbpmRquestParamDTO.getBusinessId());
            processListMap.put("fdId", fdId);
            processListMap.put("subject", subject);
            processListMap.put("fdModuleId", cbpmRquestParamDTO.getFdModuleId());
        }

        return processListMap;
    }

    @Override
    public Map<String, Object> getProcessCustomCallBack(Long businessId, String businessKey, String eventType,String fdId) throws Exception {
        Long getFormDataStart = System.currentTimeMillis();
        if(businessId == null || StringUtils.isBlank(businessKey) || StringUtils.isBlank(eventType)) {
            return null;
        }
        Map<String, Object> resultMap = null;

        //???????????????????????????????????????
        TemplateHeaderDTO queryTemplateHeaderDTO = new TemplateHeaderDTO();
        queryTemplateHeaderDTO.setModelId(businessKey);
        queryTemplateHeaderDTO.setBussinessFunction(eventType);
        queryTemplateHeaderDTO.setDeleteFlag(FlowCommonConst.DELETE_FLAG_NO);
        TemplateHeaderDTO templateHeader = iTemplateHeaderService.queryProcessTemplateByParam(queryTemplateHeaderDTO);
        TemplateLines templateLines = null;
        String bussinessFunction = "";
        if (null != templateHeader && !CollectionUtils.isEmpty(templateHeader.getTemplateLinesList())
                && null != templateHeader.getTemplateLinesList().get(0)) {
            templateLines = templateHeader.getTemplateLinesList().get(0);
            bussinessFunction = templateLines.getBussinessFunction();
        }
        if (templateHeader != null && StringUtils.isNotBlank(templateHeader.getFeignClient()) && StringUtils.isNotBlank(templateHeader.getBussinessClass())
                && StringUtils.isNotBlank(bussinessFunction)) {
            Map<String, Object> paramMap = new HashMap<>();
            Map<String, Object> mapEventData = new HashMap<>();
            LoginAppUser user = AppUserUtil.getLoginAppUser();
            String loginName = "";
            String nickName = "";
            if(null != user){
                loginName = user.getUsername();
                nickName = user.getNickname();
            }
            mapEventData.put("processId", fdId);
            mapEventData.put("handlerId", loginName);
            mapEventData.put("handlerName", nickName);
            mapEventData.put("eventType", eventType);
            mapEventData.put("handlerIp", IPUtil.getRemoteIpAddr(HttpServletHolder.getRequest()));

            paramMap.put("mapEventData", mapEventData);
            paramMap.put("businessId", businessId);
            paramMap.put("businessKey", businessKey);
            paramMap.put("classPath", templateHeader.getBussinessClass());
            paramMap.put("methodName", bussinessFunction);
            Object[] objs = new Object[1];
            Class[] clss = new Class[1];
            objs[0] = paramMap;
            clss[0] = Map.class;
            try {
                Object obj = CommonReflectMethod.commonReflectMethod(templateHeader.getFeignClient(), FlowCommonConst.FUNCTION_HANDLER, objs, clss);
                if (obj != null) {
                    Map<String, Object> responseMap = (Map<String, Object>) obj;
                    if (responseMap.containsKey("data")) {
                        resultMap = (null != responseMap.get("data") ? (Map<String, Object>) responseMap.get("data") : null);
                    }
                }
            } catch (Exception e) {
                throw new Exception("??????ID???" + businessId + ",????????????ID???" + businessKey + "?????????????????????????????????" + eventType + "??????: ", e);
            }

        }
        if (resultMap == null) {
            resultMap = new HashMap<>();
        }

        resultMap.put("businessId", businessId);
        resultMap.put("businessKey", businessKey);
        Long getFormDataEnd = System.currentTimeMillis();
        LOGGER.info("???????????????????????????,??????ID???" + businessId + ",????????????ID???" + businessKey + "??????" + eventType+" hours{}???", (getFormDataEnd - getFormDataStart) / 1000);
        return resultMap;
    }

    @Override
    public CbpmProcessNodesListDTO getProcessTableInfoAndManualNodesList(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        CbpmProcessNodesListDTO processNodesList = new CbpmProcessNodesListDTO();
        Map<String, Object> processTableInfoMap = this.getProcessTableInfoAndManualNodes(flowProcessQuery);
        JSONArray manualNodesJson = (null != processTableInfoMap.get("manualNodes")) ? (JSONArray) processTableInfoMap.get("manualNodes") : null;
        if(null != manualNodesJson){
            List<CbpmProcessNodesManualDTO> processNodesManualList = JSONObject.parseArray(manualNodesJson.toJSONString(), CbpmProcessNodesManualDTO.class);
            processNodesList.setManualNodes(processNodesManualList);
        }

        //???????????????????????????????????????????????????
        JSONArray processNodesJson = (null != processTableInfoMap.get("processNodes") ?
                (JSONArray) processTableInfoMap.get("processNodes") : null );
        List<CbpmProcessNodesDTO> processNodesDTOList = JSONObject.parseArray(processNodesJson.toJSONString(), CbpmProcessNodesDTO.class);
        if(!CollectionUtils.isEmpty(processNodesDTOList)  ) {
            User queryUser = new User();
            queryUser.buildPage(1, 9999);
            queryUser.setUserType(FlowCommonConst.USER_TYPE_BUYER);
            List<User> userList = rbacClient.listByUser(queryUser);
            if(!CollectionUtils.isEmpty(processNodesDTOList) && !CollectionUtils.isEmpty(userList)) {
                for(CbpmProcessNodesDTO processNodesDTO : processNodesDTOList){
                    if(null != processNodesDTO && null != processNodesDTO.getHandlerIds()){
                        List<Map<String, Object>> handlerIdsMapList = processNodesDTO.getHandlers();
                        for(Map<String, Object> handlerIdsMap : handlerIdsMapList) {
                            if(null != handlerIdsMapList) {
                                String userName = (null != handlerIdsMap.get("id") ? String.valueOf(handlerIdsMap.get("id")) : "");
                                String type = (null != handlerIdsMap.get("type") ? String.valueOf(handlerIdsMap.get("type")) : "");
                                if (FlowCommonConst.USER_TYPE.equals(type) && StringUtils.isNotBlank(userName)) {
                                    for (User user : userList) {
                                        if (null != user && userName.equals(user.getUsername())) {
                                            handlerIdsMap.put("name", user.getNickname());
                                            break;
                                        }else{
                                            handlerIdsMap.put("name", "");
                                        }
                                    }
                                }else{
                                    handlerIdsMap.put("name", "");
                                }
                            }
                        }
                    }
                }

            }
        }
        processNodesList.setProcessNodes(processNodesDTOList);
        return processNodesList;
    }

    @Override
    public String approveProcess(Long fdId, Long fdTemplateId, String loginName, Map<String, Object> processParam,
                                 Map<String, Object> formParam) throws Exception {

        Map<String, Object> body = new HashMap<>();
        body.put("fdId", fdId);
        body.put("loginName", loginName);
        if (null != fdTemplateId) {
            body.put("fdTemplateId", fdTemplateId);
        }
        body.put("processParam", processParam);
        if (formParam != null) {
            formParam.put("fdModuleId", StartPingServiceHandler.SYS_ID);
            body.put("formParam", formParam);
        }

        //???????????????
/*        if(formParam == null){
            formParam = new HashMap<>();
            formParam.put("day",5);
            formParam.put("fdModuleId", this.sysId);
            body.put("formParam", formParam);
        }*/

        Map<String, Object> mapResult = postForObject(WorkFlowConst.APPROVE_PROCESS, body);
        Map<String, Object> mapResultBody = (Map) mapResult.get("body");
        String resultCode = (null != mapResultBody.get("resultCode") ? String.valueOf(mapResultBody.get("resultCode")) : "");
        if (!FlowCommonConst.SUCCESS_CODE_010.equals(resultCode)) {
            throw new Exception((null != mapResultBody.get("resultMsg") ? String.valueOf(mapResultBody.get("resultMsg")) : ""));
        }
        return "success";
    }

    @Override
    public Map<String, Object> deleteProcess(FlowProcessQueryDTO flowProcessQuery) throws Exception {
        return this.getBodyData(WorkFlowConst.DELETE_PROCESS, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getFlowTemplate(FlowProcessQueryDTO flowProcessQuery) throws Exception {
        return this.getBodyData(WorkFlowConst.GET_FLOW_TEMPLATE, flowProcessQuery);
    }

    @Override
    public Map<String, Object> isExistProcess(FlowProcessQueryDTO flowProcessQuery) throws Exception {
        return this.getBodyData(WorkFlowConst.IS_EXIST_PROCESS, flowProcessQuery);
    }

    @Override
    public Boolean isExistProcessReturnBoolean(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        Map<String, Object> existMap = this.getBodyData(WorkFlowConst.IS_EXIST_PROCESS, flowProcessQuery);
        if(null != existMap && null != existMap.get("exists")){
            return (Boolean) existMap.get("exists");
        }
        return false;
    }

    @Override
    public String initProcess(FlowProcessQueryDTO flowProcessQuery) throws Exception {
        String fdId = "";
        Map<String, Object> initProcessMap = this.getBodyData(WorkFlowConst.INIT_PROCESS, flowProcessQuery);
        if(null != initProcessMap){
            fdId = null != initProcessMap.get("fdId") ? String.valueOf(initProcessMap.get("fdId")) : "";
        }
        return fdId;
    }

    @Override
    public Map<String, Object> getTemplateInfo(FlowProcessQueryDTO flowProcessQuery) throws Exception {
        return this.getBodyData(WorkFlowConst.GET_TEMPLATE_INFO, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getProcessNodesInfo(FlowProcessQueryDTO flowProcessQuery) throws Exception {
        return this.getBodyData(WorkFlowConst.GET_PROCESS_NODES_INFO, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getMyRunningProcess(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return  this.getBodyData(WorkFlowConst.GET_MY_RUNNING_PROCESS, flowProcessQuery);
    }

    @Override
    public List getProcessApproveInfo(List<String> processIdList) throws Exception {
        return this.getBodyData(WorkFlowConst.GET_PROCESS_APPROVE_INFO, processIdList, "processIdList");
    }

    @Override
    public List listFlowNodeInfo(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.LIST_FLOW_NODE_INFO, flowProcessQuery, "");
    }

    @Override
    public Map<String, Object> getProcessStatus(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_STATUS, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getProcessList(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_LIST, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getProcessCount(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_COUNT, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getProcessInfo(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_INFO, flowProcessQuery);
    }

    @Override
    public List getAuditeNote(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_AUDITE_NOTE, flowProcessQuery, "");
    }

    @Override
    public List getFlowLog(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_FLOW_LOG, flowProcessQuery, "");
    }

    @Override
    public Map<String, Object> selectUserProcessCount(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.SELECT_USER_PROCESS_COUNT, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getMyWorkedProcess(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_MY_WORKED_PROCESS, flowProcessQuery);
    }

    @Override
    public List getProcessRefuseNode(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_REFUSE_NODE, flowProcessQuery, "");
    }

    @Override
    public List getAuditeNoteInAllNodes(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_AUDITE_NOTE_IN_ALL_NODES, flowProcessQuery, "");
    }

    @Override
    public Map<String, Object> getPrevProcessApprovers(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PREV_PROCESS_APPROVERS, flowProcessQuery);
    }

    @Override
    public List listTemplateNodeEnvents(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.LIST_TEMPLATE_NODE_EVENTS, flowProcessQuery, "");
    }

    @Override
    public Map<String, Object> getMyStartProcess(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_MY_START_PROCESS, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getSendNodesToMe(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_SEND_NODES_TO_ME, flowProcessQuery);
    }

    @Override
    public List listCirculateNote(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.LIST_CIRCULATE_NOTE, flowProcessQuery, "");
    }

    @Override
    public Map<String, Object> getCircularizeNodesToMe(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_CIRCULARIZE_NODES_TO_ME, flowProcessQuery);
    }

    @Override
    public Map<String, Object> getProcessTableInfoAndManualNodes(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_TABLE_INFO_AND_MANUAL_NODES, flowProcessQuery);
    }

/*    @Override
    public Map<String, Object> getProcessTableInfoAndManualNodes(String fdId, String loginName,String roleId,
                                                                 String roleName) throws Exception {
        Map<String, Object> processTableInfoMap = new HashMap<>();

        FlowProcessQueryDTO flowProcessQuery = new FlowProcessQueryDTO();
        flowProcessQuery.setFdId(fdId);
        flowProcessQuery.setLoginName(loginName);
        processTableInfoMap = this.getProcessTableInfoAndManualNodes(flowProcessQuery);
        if(null != processTableInfoMap){
            //??????????????????
            User queryUser = new User();
            queryUser.setUserType(FlowCommonConst.USER_TYPE_BUYER);
            List<User> userList = rbacClient.listByUser(queryUser);

            List<List<Map<String, Object>>> processNodesList = null != processTableInfoMap.get("processNodes") ?
                    (List<List<Map<String, Object>>>) processTableInfoMap.get("processNodes") : null ;
            if(!CollectionUtils.isEmpty(processNodesList) && null != processNodesList.get(0) && !CollectionUtils.isEmpty(userList)){
                List<Map<String, Object>> processNodesMapList = processNodesList.get(0);
                if(!CollectionUtils.isEmpty(processNodesMapList)){
                    for(Map<String, Object> processNodesMap : processNodesMapList) {
                        Map<String, Object> handlersMap = (Map<String, Object>) processNodesMap.get("handlers");
                        if (null != handlersMap && null != handlersMap.get("id") && null != handlersMap.get("type")
                                && "USER".equals(String.valueOf(handlersMap.get("type"))) && !CollectionUtils.isEmpty(userList)) {
                            Long userId = Long.parseLong(handlersMap.get("userId").toString());
                            for (User user : userList) {
                                if (null != user && null != user.getUserId() && userId.intValue() == user.getUserId().intValue()) {
                                    handlersMap.put("name", user.getNickname());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        //?????????????????????????????? ????????????????????????????????????????????????
        Object subject = "";
        Object fdStatus = "";
        Object fdStatusCode = "";
        Map<String, Object> currNodeMap = new HashMap<>();
        flowProcessQuery = new FlowProcessQueryDTO();
        flowProcessQuery.setLoginName(loginName);
        flowProcessQuery.setFdId(fdId);
        Map<String, Object> processNodesInfoMap = this.getProcessInfo(flowProcessQuery);
        if(null != processNodesInfoMap){
            subject = processTableInfoMap.get("subject") ;
            fdStatus = processTableInfoMap.get("fdStatus");
            fdStatusCode = processTableInfoMap.get("fdStatusCode");
            if(null != fdStatusCode && CbpmProcessStatuseEnum.DRAFT_STATUS.getKey().equals(fdStatusCode.toString())) { //?????????????????????????????????
                fdStatus = CbpmProcessStatuseEnum.DRAW_UP_STATUS.getValue();
                fdStatusCode = CbpmProcessStatuseEnum.DRAW_UP_STATUS.getKey();
            }
            currNodeMap = (null != processTableInfoMap.get("currNodes") ? (Map<String, Object>) processTableInfoMap.get("currNodes") : null);
        }
        processTableInfoMap.put("subject", subject);
        processTableInfoMap.put("fdStatus", fdStatus);
        processTableInfoMap.put("fdStatusCode", fdStatusCode);
        processTableInfoMap.put("currNodes", currNodeMap);

        processTableInfoMap.put("roleId", roleId);
        processTableInfoMap.put("roleName", roleName);
        return processTableInfoMap;
    }*/


    @Override
    public List getProcessInfoById(List<String> idList) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_INFO_BY_ID, idList, "list");
    }

    @Override
    public List getProcessApproveInfoList(List<String> idList) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_APPROVE_INFO_LIST, idList, "processIdList");
    }

    @Override
    public List getOperationList(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_OPERATION_LIST, flowProcessQuery, "");
    }

    public List updateTemplateAuth(ChangeTemplateEditorDTO chanceTemplateEditor) throws Exception{
        return this.getBodyData(WorkFlowConst.UPDATE_TEMPLATE_AUTH, chanceTemplateEditor, "");
    }

    @Override
    public List getProcessSuperRefuseNode(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.GET_PROCESS_SUPER_REFUSE_NODE, flowProcessQuery, "");
    }

    @Override
    public List selectCommonReply(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.SELECT_COMMON_REPLY, flowProcessQuery, "");
    }

    @Override
    public List updateProcessSubject(ChangeProcessSubjectDTO chanceProcessSubject) throws Exception{
        return this.getBodyData(WorkFlowConst.UPDATE_PROCESS_SUBJECT, chanceProcessSubject, "");
    }

    @Override
    public Map<String, Object> replaceProcessHandler(FlowProcessQueryDTO flowProcessQuery) throws Exception{
        return this.getBodyData(WorkFlowConst.REPLACE_PROCESS_HANDLER, flowProcessQuery);
    }

    /**
     * Description ????????????????????????????????????????????????Body???Map??????,???????????????????????????????????????????????????
     * @Param apiName ???????????? flowProcessQuery???????????????
     * @return
     * @Author wuwl18@meicloud.com
     * @Date 2020.03.11
     * @throws
     **/
    private Map<String, Object> getBody(String apiName, Object object) throws Exception {
        Map<String, Object> result = new HashMap<>();
        if(null != object){
            Map<String, Object> body = new HashMap<>();
            if(object instanceof BaseDTO) {
                body = JsonUtil.parseObjectToMap(object);
                if(null != body) {
                    Map<String, Object> mapResult = postForObject(apiName, body);
                    result = (null != mapResult) ? (Map)mapResult.get("body") : new HashMap<>();
                }
            }
        }
        return result;
    }

    /**
     * Description ????????????????????????????????????????????????data???Map??????,???????????????????????????????????????????????????
     * @Param apiName ???????????? flowProcessQuery???????????????
     * @return
     * @Author wuwl18@meicloud.com
     * @Date 2020.03.11
     * @throws
     **/
    private Map<String, Object> getBodyData(String apiName, Object object) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result = this.getBody(apiName, object);
        if(null != result) {
            String resultCode = String.valueOf(result.get("resultCode"));
            if (!FlowCommonConst.SUCCESS_CODE_010.equals(resultCode)) {
                LOGGER.error("??????cbpm?????????"+apiName+"???????????????: "+String.valueOf(result.get("resultMsg")));
                throw new Exception(String.valueOf(result.get("resultMsg")));
            }
            result = (Map) result.get("data");
        }
        return result;
    }

    /**
     * Description ???????????????????????????????????????????????????????????????data???List??????,???????????????????????????????????????????????????
     * @Param apiName ???????????? flowProcessQuery??????????????? listName ???????????????
     * @return List
     * @Author wuwl18@meicloud.com
     * @Date 2020.03.11
     * @throws
     **/
    private List getBodyData(String apiName, Object object,String listName) throws Exception {
        List resultList = new ArrayList();
        if(null != object) {
            Map<String, Object> body = new HashMap<>();
            if(object instanceof FlowProcessQueryDTO  || object instanceof ChangeProcessSubjectDTO
                || object instanceof ChangeTemplateEditorDTO) { //?????????????????????
                body = JsonUtil.parseObjectToMap(object);
            }else if(object instanceof  List<?>){   //????????????
                List<String> initList = new ArrayList<String>();
                for(Object o : (List<String>) object ){
                    initList.add(String.class.cast(o));
                }
                StringBuilder strJson = new StringBuilder("{"+listName+":");
                strJson.append(JsonUtil.arrayToJsonStr(initList)+"}");
                body = JsonUtil.parseJsonStrToMap(strJson.toString());
            }

            if(null != body) {
                Map<String, Object> mapResult = postForObject(apiName, body);
                Map<String, Object> result = (null != mapResult) ? (Map)mapResult.get("body") : new HashMap<>();
                if(null != result) {
                    String resultCode = String.valueOf(result.get("resultCode"));
                    if (!FlowCommonConst.SUCCESS_CODE_010.equals(resultCode)) {
                        throw new Exception(String.valueOf(result.get("resultMsg")));
                    }
                    resultList = (List) result.get("data");
                }
            }
        }
        return resultList;
    }

    /**
     * Description ??????http????????????cbpm????????????????????????
     * @Param methodName ????????? body????????????
     * @return Map<String,Object>
     * @Author wuwl18@meicloud.com
     * @Date 2020.03.11
     **/
    private Map<String,Object> postForObject(String methodName,Map<String, Object> body){
//        JSONObject body = new JSONObject();
//        body.put("loginName", "chenchao17");

        //???????????????header ?????????
        JSONObject reqParam = this.getReqHeader(true);
        //??????????????????????????????reqParam???????????????
        reqParam.put("body", body);
        System.out.println("body?????????"+JsonUtil.entityToJsonStr(body));
        //????????????,?????????????????????,??????????????????token??????
        String token = null;
        try {
            token = this.getToken(reqParam);
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        System.out.println("=============token================");
        System.out.println(token);
        //??????URL
        String url = this.getUrl(token,methodName);
        //??????result,???????????????????????????
        String result = null;
        try {
            //?????? ?????????
            HttpHeaders headers =  this.getreqHeaders();
            LOGGER.info("=============headers================"+JsonUtil.entityToJsonStr(headers));
            //????????????????????????
            HttpEntity<String> entity = new HttpEntity<String>(reqParam.toJSONString(), headers);
            LOGGER.info("??????URL???" + url);
            LOGGER.info("????????????:=={}==",reqParam);
            Long start = System.currentTimeMillis();
            result = restTemplate.postForObject(new URI(url), entity, String.class);
            Long end = System.currentTimeMillis();
/*            Long diff = end - start;
            if(diff.compareTo(1500L) == 1 ){
                logger.info("??????Cbpm???????????????" + (end - start) + " ms; ??????URL???" + url);
            }*/
            LOGGER.info("??????Cbpm???????????????" + (end - start));
        } catch (RestClientException e) {
            // TODO Auto-generated catch block
            log.error("????????????????????????",e);
            if("403 Forbidden".equals(e.getMessage())) {
                throw new BaseException(ResultCode.NEED_PERMISSION.getMessage());
            }
        } catch (URISyntaxException e) {
            log.error("????????????????????????",e);
            // TODO Auto-generated catch block
        }

        System.out.println("======================????????????????????????======================");
        System.out.println(result);
        Map<String,Object> mapData = JsonUtil.parseJsonStrToMap(result);
        return mapData;
    }

    //?????????????????????
    private JSONObject getReqHeader(boolean b) {

        JSONObject reqParam = new JSONObject();

        //header????????????
        JSONObject header = new JSONObject();
        //sourceAddr??????
        try {
            header.put("sourceAddr", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            header.put("sourceAddr", sourceAddr);
        }
        //forwardTo??????
        header.put("forwardTo", forwardTo);
        //timestamp??????     ?????????????????????????????????
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        //sn?????? ??????????????????????????????????????????
        header.put("sn", UUID.randomUUID());
        //timeZone????????????
        header.put("timeZone", "UTC");
        //serviceName??????
        header.put("serviceName", serviceName);
        //???????????????,????????????header????????????????????????,??????????????????body??????
        reqParam.put("header", header);
        return reqParam;
    }

    //???????????????
    private HttpHeaders getreqHeaders() {
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(mediaType);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        //???????????????????????????
        headers.add("Accept-Language","zh-CN");

        /**??????????????????????????????idass_token*/
        if(idaasToken) {
            String key = "x_idass_token_value";
            Object idaasToken = redisTemplate.opsForValue().get(key);
            if (null == idaasToken) {
                idaasToken = this.getIdaasToken();
                redisTemplate.opsForValue().set(key, idaasToken, 110, TimeUnit.MINUTES);
            }
            headers.add("X-IDaaS-Token", String.valueOf(idaasToken));
        }
        return headers;
    }

    //??????Url
    private String getUrl(String token, String methedName) {

        //url????????????????????? uat?????????http://cbpmuat.midea.com/mbpmEngineCloud/data/ sit:http://10.17.162.216:60090/mbpmEngineCloud/data/
        String uatURL = url;
        //????????????
//        String methedName = methed;
        //?????????appid MSE4MIP
        //??????URL
//        appId = "1";      //appId???1????????????getProcessList???getProcessCount?????????
//        appId = "asdf";      //appId???asdf????????????selectUserProcessCount???getMyWorkedProcess?????????
        String url = uatURL + methedName + "?appId="+StartPingServiceHandler.APP_ID + "&token=" + token;

        return url;
    }

    //??????token
    private String getToken(JSONObject reqParam) throws UnsupportedEncodingException {
        //?????????????????????????????????????????? -->  580758d8-fd4a-439e-8866-fa3f39640a0e
        //DataEncrypt ?????????,???????????????,????????????????????????,????????????????????????
        String token = DataEncrypt.getEncryptStr(reqParam.toString(), StartPingServiceHandler.SALT);
        token = URLEncoder.encode(token, "UTF-8");
        return token;
    };

    /**
     * Description ??????accessKeyId???accessKeySecret????????????IdassToken
     * @Param
     * @return
     * @Author wuwl18@meicloud.com
     * @Date 2020.05.09
     * @throws
     **/
    @Override
    public String getIdaasToken() {
        //??????uat???????????????url idaasUrl= http://10.16.148.47:8002/apis/v1/paas/security/getAccessToken ????????? https://apiuat.midea.com/iam/v1/security
//        String urlFormat = idaasUrl + "/apis/v1/paas/security/getAccessToken?grantType=client_credential&accessKeyId=%s&timestamp=%s&signature=%s";

        //https://apiuat.midea.com/iam/v1/security/getAccessToken?grantType=client_credential&accessKeyId=5yLOYMvhRhw0305glDFBiuAI&timestamp=1592471198688&signature=be061037da19aa521549ffb599bb3c00638867c9eb5599abccdc493d97cc5100034f8e51d3ee7f51
        String urlFormat = idaasUrl + "/getAccessToken?grantType=client_credential&accessKeyId=%s&timestamp=%s&signature=%s";
        Pbkdf2Encoder encoder = new Pbkdf2Encoder(StartPingServiceHandler.CBPM_ACCESS_KEY_SECERT);
        String grantType = "client_credential";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = encoder.encode(StringUtils.join(grantType, StartPingServiceHandler.CBPM_ACCESS_KEY_ID, timestamp));
        String url = String.format(urlFormat, StartPingServiceHandler.CBPM_ACCESS_KEY_ID, timestamp, signature);
        LOGGER.debug("??????IdassToken?????????timestamp: {},signature: {},url: {}",timestamp,signature,url);
        String idassReturn = restTemplate.getForObject(url, String.class);
        JSONObject json = JSON.parseObject(idassReturn);
        if(json.getString("code").equals("0")) {
            return json.getJSONObject("result").getString("accessToken");
        } else {
            throw new BaseException("??????idaas access token ??????");
        }
    }
}
