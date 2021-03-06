package com.midea.cloud.flow.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.bpm.TempIdToModuleEnum;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.JsonUtil;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.component.context.container.SpringContextHolder;
import com.midea.cloud.flow.common.constant.FlowConstant;
import com.midea.cloud.flow.model.dto.EmpSelectViewDto;
import com.midea.cloud.flow.model.dto.EmployeeBaseDto;
import com.midea.cloud.flow.model.dto.FormField;
import com.midea.cloud.flow.model.dto.FormListDto;
import com.midea.cloud.flow.model.dto.OrgUnitDto;
import com.midea.cloud.flow.model.dto.Result;
import com.midea.cloud.flow.model.dto.TodoDto;
import com.midea.cloud.flow.model.dto.WfCommunicateDto;
import com.midea.cloud.flow.model.dto.WfDiscardParam;
import com.midea.cloud.flow.model.dto.WfOverruleParam;
import com.midea.cloud.flow.model.dto.WfParam;
import com.midea.cloud.flow.model.dto.WfRecallParam;
import com.midea.cloud.flow.model.dto.WfTransferParam;
import com.midea.cloud.flow.model.dto.WorkflowCallbackDto;
import com.midea.cloud.flow.model.dto.WorkflowEventDto;
import com.midea.cloud.flow.model.dto.WorkflowTodoDto;
import com.midea.cloud.flow.service.IWorkflowHandlerService;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.workflow.FlowClient;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.rbac.user.entity.User;

import lombok.extern.slf4j.Slf4j;

/**
 * ???????????????????????????????????????
 * @author lizl7
 *
 */
@Controller
@RequestMapping("/flow-anon/workflowcallback/sis")
@Slf4j
public class WorkflowCallbackSisController extends BaseController  {

    @Value("${oasis.restapi.client.baseUrl:http://10.17.145.73/backend/oasis}")
    private String baseUrl;
    
    @Autowired
    private RbacClient rbacClient;
    
    @Autowired
    private IWorkflowHandlerService iWorkflowHandlerService;
    
	/**
	 * ??????????????????
	 */
	@RequestMapping(value = "callback/{handleType}", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView callback(@PathVariable String handleType,@RequestBody String requestBody,HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws Exception{
		if( !checkAccessAuthorize(httpServletRequest, httpServletResponse) ) {
			return null;
		}
		log.info("callback - "+handleType+" - "+requestBody);

		//????????????????????????????????????????????????
		String paramString =requestBody;
		WorkflowCallbackDto workflowCallbackDto =null;
		if( "start".equals(handleType) ) {
			//??????
			WfParam wfParam =(WfParam)JsonUtil.parseJsonStrToEntity(requestBody, WfParam.class);
			//??????????????????WorkflowCallbackDto
			workflowCallbackDto =new WorkflowCallbackDto();
			workflowCallbackDto.setParam(paramString);
			workflowCallbackDto.setBusinessId(Long.parseLong(wfParam.getBusinessKey()));
			//"procdefId":"BusinessType_QuaReview:9:10004"
			workflowCallbackDto.setBusinessType(wfParam.getProcdefId().split(":")[0]);
			workflowCallbackDto.setFlowStatus(FlowConstant.Flow_Status_START);
			workflowCallbackDto.setHandlerId(Long.parseLong(wfParam.getHandlerId()));
			workflowCallbackDto.setHandlerAccount(null);
		}else if( "approve".equals(handleType) ) {
			//??????
			WfParam wfParam =(WfParam)JsonUtil.parseJsonStrToEntity(requestBody, WfParam.class);
			//??????????????????????????????????????????approve???????????????????????????????????????
			Map<String,Object> mapExtJsonData =JsonUtil.parseJsonStrToMap(wfParam.getExtJsonData());
			if( mapExtJsonData.get("handleLabel")!=null && "??????".equals(mapExtJsonData.get("handleLabel").toString()) ) {
				workflowCallbackDto =new WorkflowCallbackDto();
				workflowCallbackDto.setParam(paramString);
				workflowCallbackDto.setBusinessId(Long.parseLong(wfParam.getBusinessKey()));
				//"procdefId":"BusinessType_QuaReview:9:10004"
				workflowCallbackDto.setBusinessType(wfParam.getProcdefId().split(":")[0]);
				workflowCallbackDto.setFlowStatus(FlowConstant.Flow_Status_START);
				workflowCallbackDto.setHandlerId(Long.parseLong(wfParam.getHandlerId()));
				workflowCallbackDto.setHandlerAccount(null);
			}else if( mapExtJsonData.get("handleLabel")!=null && "??????".equals(mapExtJsonData.get("handleLabel").toString()) ) {
				//?????????????????????????????????????????????????????????????????????
				//???????????? 0?????? 1????????? 2????????? 3?????? 4?????? 5?????? 6??????
				String instStatus =getProcInstStatus(wfParam.getProcInstId());
				log.info("callback - approve - "+instStatus);
				if( "2".equals(instStatus) ) {
					//???????????????????????????????????????
					//??????????????????WorkflowCallbackDto
					workflowCallbackDto =new WorkflowCallbackDto();
					workflowCallbackDto.setParam(paramString);
					workflowCallbackDto.setBusinessId(Long.parseLong(wfParam.getBusinessKey()));
					//"procdefId":"BusinessType_QuaReview:9:10004"
					workflowCallbackDto.setBusinessType(wfParam.getProcdefId().split(":")[0]);
					workflowCallbackDto.setFlowStatus(FlowConstant.Flow_Status_APPROVED);
					workflowCallbackDto.setHandlerId(Long.parseLong(wfParam.getHandlerId()));
					workflowCallbackDto.setHandlerAccount(null);
				}
			}
		}else if( "overrule".equals(handleType) ) {
			//??????
			WfOverruleParam wfOverruleParam =(WfOverruleParam)JsonUtil.parseJsonStrToEntity(requestBody, WfOverruleParam.class);
			//???????????????????????????
			if( "??????".equals(wfOverruleParam.getToTaskName()) ) {
				//??????????????????WorkflowCallbackDto
				workflowCallbackDto =new WorkflowCallbackDto();
				workflowCallbackDto.setParam(paramString);
				workflowCallbackDto.setBusinessId(Long.parseLong(wfOverruleParam.getBusinessKey()));
				//"procdefId":"BusinessType_QuaReview:9:10004"
				workflowCallbackDto.setBusinessType(wfOverruleParam.getProcdefId().split(":")[0]);
				workflowCallbackDto.setFlowStatus(FlowConstant.Flow_Status_REJECTED);
				workflowCallbackDto.setHandlerId(Long.parseLong(wfOverruleParam.getHandlerId()));
				workflowCallbackDto.setHandlerAccount(null);
			}
		}else if( "transfer".equals(handleType) ) {
			//??????
			WfTransferParam wfTransferParam =(WfTransferParam)JsonUtil.parseJsonStrToEntity(requestBody, WfTransferParam.class);
		}else if( "discard".equals(handleType) ) {
			//??????
			WfDiscardParam wfDiscardParam =(WfDiscardParam)JsonUtil.parseJsonStrToEntity(requestBody, WfDiscardParam.class);
			//??????????????????WorkflowCallbackDto
			workflowCallbackDto =new WorkflowCallbackDto();
			workflowCallbackDto.setParam(paramString);
			workflowCallbackDto.setBusinessId(Long.parseLong(wfDiscardParam.getBusinessKey()));
			//"procdefId":"BusinessType_QuaReview:9:10004"
			workflowCallbackDto.setBusinessType(wfDiscardParam.getProcdefId().split(":")[0]);
			workflowCallbackDto.setFlowStatus(FlowConstant.Flow_Status_DESTORY);
			workflowCallbackDto.setHandlerId(Long.parseLong(wfDiscardParam.getHandlerId()));
			workflowCallbackDto.setHandlerAccount(null);
		}else if( "recall".equals(handleType) ) {
			//??????
			WfRecallParam wfRecallParam =(WfRecallParam)JsonUtil.parseJsonStrToEntity(requestBody, WfRecallParam.class);
			//??????????????????WorkflowCallbackDto
			workflowCallbackDto =new WorkflowCallbackDto();
			workflowCallbackDto.setParam(paramString);
			workflowCallbackDto.setBusinessId(Long.parseLong(wfRecallParam.getBusinessKey()));
			//"procdefId":"BusinessType_QuaReview:9:10004"
			workflowCallbackDto.setBusinessType(wfRecallParam.getProcdefId().split(":")[0]);
			workflowCallbackDto.setFlowStatus(FlowConstant.Flow_Status_WITHDRAW);
			workflowCallbackDto.setHandlerId(Long.parseLong(wfRecallParam.getHandlerId()));
			workflowCallbackDto.setHandlerAccount(null);
		}else if( "communicate".equals(handleType) ) {
			//??????
			WfCommunicateDto wfCommunicateDto =(WfCommunicateDto)JsonUtil.parseJsonStrToEntity(requestBody, WfCommunicateDto.class);
		}else if( "reply".equals(handleType) ) {
			//??????
			WfCommunicateDto wfCommunicateDto =(WfCommunicateDto)JsonUtil.parseJsonStrToEntity(requestBody, WfCommunicateDto.class);
		}else {
			WfParam wfParam =(WfParam)JsonUtil.parseJsonStrToEntity(requestBody, WfParam.class);
		}
		//??????????????????????????????????????????????????????????????????

		//??????????????????????????????????????????????????????????????????
		if( workflowCallbackDto!=null ) {
			iWorkflowHandlerService.callback(workflowCallbackDto);
		}

		Map<String,Object> mapResult =new HashMap<String, Object>();
		mapResult.put("code", "100");
		mapResult.put("message", "success");
		this.writeToStreamForJsonObject(httpServletResponse, mapResult);
		return null;
	}
	
	
//	/**
//	 * ???????????????????????????token???????????????????????????
//	 * @param httpServletRequest
//	 * @param httpServletResponse
//	 * @return
//	 */
//	public String getFrontendTokenForBackend() {
//		String hostUrl =PropertiesUtil.getPropertiesMdpBy("oasis.restapi.client.baseUrl");
//		String userId ="0";
//		String userName ="srm";
//		String url =hostUrl+"/access-anon/getFrontendToken?userId="+userId+"&userName="+userName;
//		RemoteContext remoteContext =new RemoteContext();
//		String result =remoteContext.invokeUrlForString(RemoteContext.Method_GET, url, null, null);
//		return result;
//	}
	
	/**
	 * ????????????????????????????????? 0?????? 1????????? 2????????? 3?????? 4?????? 5?????? 6??????
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 */
	public String getProcInstStatus(String procInstId) {
		//String token =getFrontendTokenForBackend();
		String url =baseUrl+"/access-anon/getProcInstStatus?procInstId="+procInstId;
		RestTemplate restTemplate = new RestTemplate();
		String result =restTemplate.getForObject(url, String.class);
		return result;
	}
	
	
	/**
	 * ?????????????????????????????????id
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 */
	public Long getInstanceInfoByProcInstId(String procInstId) {
		String url =baseUrl+"/access-anon/getInstanceInfoByProcInstId?procInstId="+procInstId;
		RestTemplate restTemplate = new RestTemplate();
		String result =restTemplate.getForObject(url, String.class);
		Map<String, Object> mapResult =(Map)JsonUtil.parseJsonStrToMap(result);
		return Long.parseLong(mapResult.get("businessKey").toString());
	}
	
	
	/**
	 * ????????????????????????
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 */
	public boolean checkAccessAuthorize(HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) {
		//this.writeToStreamForJsonObject(httpServletResponse, Result.fail("????????????"));
		return true;
	}
	
	/**
	 * ????????????????????????
	 * @param empId
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "getEmployeeById", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView getEmployeeById(@RequestBody String requestBody, HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws Exception{
		if( !checkAccessAuthorize(httpServletRequest, httpServletResponse) ) {
			return null;
		}
		Map<String, Object> mapParam =JsonUtil.parseJsonStrToMap(requestBody);
		
//		UsersService usersService =(UsersService)ContextUtils.getBeanConsumer(UsersService.class);
//		TspSysUsersDTO tspSysUsersDTO =usersService.findTspSysUsersById(Long.parseLong(mapParam.get("empId").toString()));
		User user =rbacClient.getUserByIdAnon(Long.parseLong(mapParam.get("empId").toString()));
		
		//???????????????????????????
		EmployeeBaseDto employeeBaseDto =new EmployeeBaseDto();
		if( user!=null && user.getUserId()!=null ) {
			employeeBaseDto.setEmployeeId(user.getUserId().toString());
			employeeBaseDto.setEmployeeCode(user.getUsername());
			employeeBaseDto.setEmployeeName(user.getNickname());
			employeeBaseDto.setUnitName(user.getDepartment());
		}
		this.writeToStreamForJsonObject(httpServletResponse, Result.success(employeeBaseDto));
		return null;
	}
	
	/**
	 * ???????????????????????????????????????
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "fetchForms", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView fetchForms(HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws Exception{
		if( !checkAccessAuthorize(httpServletRequest, httpServletResponse) ) {
			return null;
		}
		List<FormListDto> listFormListDto =new ArrayList<FormListDto>();
		FormListDto formListDto =new FormListDto();
		formListDto.setFormId("1001");
		formListDto.setFormName("??????????????????");
		formListDto.setFormUrl("");
		formListDto.setApplication("ihr-corehr");

		listFormListDto.add(formListDto);
		
		this.writeToStreamForJsonObject(httpServletResponse, Result.success(listFormListDto));
		return null;
	}
	
	
	/**
	 * ???????????????????????????????????????
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "getFormDetail", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView getFormDetail(@RequestBody String requestBody, HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws Exception{
		if( !checkAccessAuthorize(httpServletRequest, httpServletResponse) ) {
			return null;
		}
		Map<String, Object> mapParam =JsonUtil.parseJsonStrToMap(requestBody);
		
		List<FormField> listFormFieldParent =new ArrayList<FormField>();
		FormField formFieldParent =new FormField();
		formFieldParent.setFormId(Integer.getInteger(mapParam.get("formId").toString()));
		formFieldParent.setFormFieldId("1001");
		formFieldParent.setFieldName("????????????(??????????????????????????????)");
		formFieldParent.setFieldKey("Field");
		formFieldParent.setApplication("ihr-corehr");
		formFieldParent.setOrderNo("1");
		formFieldParent.setChildFormFieldPojoList(new ArrayList<FormField>());

		FormField formField =new FormField();
		formField.setFormId(Integer.getInteger(mapParam.get("formId").toString()));
		formField.setFormFieldId("1001001");
		formField.setFieldName("????????????(Field01)");
		formField.setFieldKey("Field01");
		formField.setApplication("ihr-corehr");
		formField.setOrderNo("1");
		formFieldParent.getChildFormFieldPojoList().add(formField);
		
		formField =new FormField();
		formField.setFormId(Integer.getInteger(mapParam.get("formId").toString()));
		formField.setFormFieldId("1001002");
		formField.setFieldName("????????????(Field02)");
		formField.setFieldKey("Field02");
		formField.setApplication("ihr-corehr");
		formField.setOrderNo("2");
		formFieldParent.getChildFormFieldPojoList().add(formField);
		
		formField =new FormField();
		formField.setFormId(Integer.getInteger(mapParam.get("formId").toString()));
		formField.setFormFieldId("1001003");
		formField.setFieldName("????????????(Field03)");
		formField.setFieldKey("Field03");
		formField.setApplication("ihr-corehr");
		formField.setOrderNo("3");
		formFieldParent.getChildFormFieldPojoList().add(formField);
		
		formField =new FormField();
		formField.setFormId(Integer.getInteger(mapParam.get("formId").toString()));
		formField.setFormFieldId("1001004");
		formField.setFieldName("????????????(Field04)");
		formField.setFieldKey("Field04");
		formField.setApplication("ihr-corehr");
		formField.setOrderNo("4");
		formFieldParent.getChildFormFieldPojoList().add(formField);
		
		formField =new FormField();
		formField.setFormId(Integer.getInteger(mapParam.get("formId").toString()));
		formField.setFormFieldId("1001005");
		formField.setFieldName("????????????(Field05)");
		formField.setFieldKey("Field05");
		formField.setApplication("ihr-corehr");
		formField.setOrderNo("5");
		formFieldParent.getChildFormFieldPojoList().add(formField);
		
		formField =new FormField();
		formField.setFormId(Integer.getInteger(mapParam.get("formId").toString()));
		formField.setFormFieldId("1001006");
		formField.setFieldName("??????(Amount)");
		formField.setFieldKey("Amount");
		formField.setApplication("ihr-corehr");
		formField.setOrderNo("6");
		formFieldParent.getChildFormFieldPojoList().add(formField);
		
		formField =new FormField();
		formField.setFormId(Integer.getInteger(mapParam.get("formId").toString()));
		formField.setFormFieldId("1001007");
		formField.setFieldName("??????(Qty)");
		formField.setFieldKey("Qty");
		formField.setApplication("ihr-corehr");
		formField.setOrderNo("7");
		formFieldParent.getChildFormFieldPojoList().add(formField);
		
		listFormFieldParent.add(formFieldParent);
		
		this.writeToStreamForJsonObject(httpServletResponse, Result.success(listFormFieldParent));
		return null;
	}

	/**
	 *  ??????????????????????????????????????????????????????
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "addWorkflowTodo", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView addWorkflowTodo(@RequestBody String requestBody, HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws Exception{
		if( !checkAccessAuthorize(httpServletRequest, httpServletResponse) ) {
			return null;
		}
		log.info("addWorkflowTodo - "+requestBody);
		//??????
		TodoDto todoDto =(TodoDto)JsonUtil.parseJsonStrToEntity(requestBody, TodoDto.class);
		//????????????
		WorkflowTodoDto workflowTodoDto =new WorkflowTodoDto();
		workflowTodoDto.setBusinessType(todoDto.getProcdefId().split(":")[0]);
		workflowTodoDto.setHandlerId(Long.parseLong(todoDto.getReceiver()));
		workflowTodoDto.setTitle(todoDto.getTodoTitle());
		workflowTodoDto.setBusinessId(getInstanceInfoByProcInstId(todoDto.getProcInstId()));
		if( workflowTodoDto!=null ) {
			log.info("addWorkflowTodo - workflowTodoDto - "+JsonUtil.entityToJsonStr(workflowTodoDto));
			iWorkflowHandlerService.todo(workflowTodoDto);
		}
		this.writeToStreamForJsonObject(httpServletResponse, Result.success(""));
		return null;
	}

	/**
	 *  ??????????????????????????????????????????
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "completeWorkflowTodo", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView completeWorkflowTodo(String keyword, HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws Exception{
		if( !checkAccessAuthorize(httpServletRequest, httpServletResponse) ) {
			return null;
		}
		log.info("completeWorkflowTodo - "+keyword);
		//??????
		
		this.writeToStreamForJsonObject(httpServletResponse, Result.success(null));
		return null;
	}
	
	/**
	 *  ????????????????????????
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "selectOrgUnitByParentId", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView selectOrgUnitByParentId(String parentId, HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws Exception{
		if( !checkAccessAuthorize(httpServletRequest, httpServletResponse) ) {
			return null;
		}
		log.info("selectOrgUnitByParentId - "+parentId);
		//??????
		List<OrgUnitDto> listOrgUnitDto =new ArrayList<OrgUnitDto>();
		OrgUnitDto orgUnitDto =new OrgUnitDto();
		orgUnitDto.setUnitId(1001);
		orgUnitDto.setUnitCode("ROOT");
		orgUnitDto.setUnitName("?????????");
		listOrgUnitDto.add(orgUnitDto);
		this.writeToStreamForJsonObject(httpServletResponse, Result.success(listOrgUnitDto));
		return null;
	}
	
	/**
	 *  ????????????????????????
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "selectEmp", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView selectEmp(@RequestBody String requestBody, HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse) throws Exception{
		if( !checkAccessAuthorize(httpServletRequest, httpServletResponse) ) {
			return null;
		}
		log.info("selectEmp - "+requestBody);
		Map<String, Object> mapParam =JsonUtil.parseJsonStrToMap(requestBody);
		String userName ="";
		if( mapParam.get("employeeName")!=null ) {
			userName = mapParam.get("employeeName").toString();
		}
		if( mapParam.get("employeeCode")!=null ) {
			userName = mapParam.get("employeeCode").toString();
		}
//        CompanysService companysService = (CompanysService) ContextUtils.getBeanConsumer(CompanysService.class);
//        //userName, department, pageSize, pageNo
//        String result = companysService.queryCompanyUsers(null, userName, null, 10, 1);
//		log.info("selectEmp - "+userName+" - "+result);
//        List<TspSysUsers> listTspSysUsers =JsonUtil.parseJsonStrJustepToEntityArray(result, TspSysUsers.class);
		User userQuery  =new User();
        if( userName!=null && !"".equals(userName) ) {
        	userQuery.setUsername(userName);
        }
        //??????????????????????????????
		userQuery.setUserType("BUYER");
        List<User> listUser =rbacClient.listUsersPage(userQuery, 1, 10);
		
		//??????
		List<EmpSelectViewDto> listEmpSelectViewDto =new ArrayList<EmpSelectViewDto>();
		for( User user : listUser ) {
			EmpSelectViewDto empSelectViewDto =new EmpSelectViewDto();
			empSelectViewDto.setEmployeeId(user.getUserId().toString());
			empSelectViewDto.setEmployeeCode(user.getUsername());
			empSelectViewDto.setEmployeeName(user.getNickname());
			empSelectViewDto.setUnitName(user.getDepartment());
			empSelectViewDto.setUnitId(1001);
			listEmpSelectViewDto.add(empSelectViewDto);
		}
		this.writeToStreamForJsonObject(httpServletResponse, Result.success(listEmpSelectViewDto));
		return null;
	}
	
	
	public void writeToStreamForHtml(HttpServletResponse httpServletResponse,String strHtml){
		try {
			httpServletResponse.setContentType("text/html;charset=UTF-8");
			httpServletResponse.getWriter().print(strHtml);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	public void writeToStreamForJsonString(HttpServletResponse httpServletResponse,String strValue){
		try {
			httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
			httpServletResponse.setContentType("application/json;charset=UTF-8");
			httpServletResponse.getWriter().print(strValue);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void writeToStreamForJsonObject(HttpServletResponse httpServletResponse,Object objIn){
		try {
			httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
			String strTemp =JSON.toJSONString(objIn);
			httpServletResponse.setContentType("application/json;charset=UTF-8");
			httpServletResponse.getWriter().print(strTemp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void writeToStreamForJsonMap(HttpServletResponse httpServletResponse,Map<String,Object> mapResultIn){
		try {
			httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
			JSONObject jSONObject =new JSONObject(mapResultIn);
			httpServletResponse.setContentType("application/json;charset=UTF-8");
			httpServletResponse.getWriter().print(jSONObject.toJSONString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeToStreamForJsonArray(HttpServletResponse httpServletResponse,List listIn){
		try {
			httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
			JSONArray jSONArray =new JSONArray(listIn);
			httpServletResponse.setContentType("application/json;charset=UTF-8");
			httpServletResponse.getWriter().print(jSONArray.toJSONString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
