package com.midea.cloud.srm.cm.accept.workflow;

import com.alibaba.fastjson.JSONObject;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.base.WorkflowClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.cm.accept.dto.AcceptDTO;
import com.midea.cloud.srm.model.cm.accept.entity.AcceptOrder;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.workflow.dto.AttachFileDto;
import com.midea.cloud.srm.model.workflow.dto.WorkflowCreateResponseDto;
import com.midea.cloud.srm.model.workflow.dto.WorkflowFormDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author vent
 * @Description
 **/
@RequestMapping("/acceptFlow")
@RestController
public class AcceptFlow {

    @Resource
    private WorkflowClient workflowClient;

    @Resource
    private BaseClient baseClient;

    @Resource
    private RbacClient rbacClient;

    @Resource
    private FileCenterClient fileCenterClient;

    @Value("${template.acceptTempId}")
    private String tempId;

    @Value("${template.acceptSrmLink}")
    private String srmLink;

//    private final String tempId = "1751aecac3c4d73be9421bf4e0fbf494";
//    private final String srmLink = "http://10.0.10.48/#/formDataPreview?funName=inspectionBill&formId=7989302780821504";

    /**
     * <pre>
     *
     * </pre>
     *
     * @author chenwt24@meicloud.com
     * @version 1.00.00
     *
     * <pre>
     *  ????????????
     *  ???????????????:
     *  ?????????:
     *  ????????????: 2020-10-15
     *  ????????????:
     * </pre>
     */
    @PostMapping(value = "/submitAcceptFlow")
    public String submitAcceptFlow(@RequestBody AcceptDTO acceptDTO) throws Exception {
        AcceptOrder acceptOrder = acceptDTO.getAcceptOrder();
        List<Fileupload> assetFile = acceptDTO.getAssetFile();
        List<Fileupload> techFile = acceptDTO.getTechFile();
        String urlFormId = String.valueOf(acceptOrder.getAcceptOrderId());

        //?????????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        Assert.isTrue(StringUtils.isNotEmpty(loginAppUser.getCeeaEmpNo()),"???????????????????????????");

        //?????????????????????
        String ceeaEmpNo = acceptOrder.getCeeaUserEmpNo();
        if (StringUtils.isEmpty(ceeaEmpNo)){
            String ceeaUserName = acceptOrder.getCeeaUserName();
            LoginAppUser byUsername = rbacClient.findByUsername(ceeaUserName);
            ceeaEmpNo = byUsername.getCeeaEmpNo();        }
        Assert.isTrue(StringUtils.isNotEmpty(ceeaEmpNo), "???????????????????????????");

        Organization organization = baseClient.get(acceptOrder.getCeeaOrgId());


        String formId = "";
        String docSubject = "??????????????????";
        String fdTemplateId	=tempId;
        String docContent = "ceshi";

        JSONObject formValues = new JSONObject();
        formValues.put("fd_user_name",loginAppUser.getCeeaEmpNo());
        formValues.put("fd_document_num",acceptOrder.getAcceptNumber());
        formValues.put("fd_asset_class_type",acceptOrder.getCeeaAssetType());
        formValues.put("fd_userno",ceeaEmpNo);
        formValues.put("fd_business_unit_code",ObjectUtils.isEmpty(organization)?"":organization.getOrganizationName());

        formValues.put("fd_srm_link",srmLink.replace("$formId$",urlFormId));
        formValues.put("fd_mobile_link",srmLink.replace("$formId$",urlFormId));
        formValues.put("fd_38ee7f530e0054","");
        formValues.put("fd_38ee7f54169fb4","");
        formValues.put("fd_38ee7f522e1e3a","");


        String docStatus ="20";
        JSONObject docCreator = new JSONObject();
        docCreator.put("PersonNo",loginAppUser.getCeeaEmpNo());
        String fdKeyword ="[\"?????????\", \"??????\"]";
        String docProperty ="";

        WorkflowFormDto workflowForm = new WorkflowFormDto();
        workflowForm.setFormInstanceId(String.valueOf(acceptOrder.getAcceptOrderId()));
        workflowForm.setDocSubject(docSubject);
        workflowForm.setFdTemplateId(fdTemplateId);
        workflowForm.setDocContent(docContent);
        workflowForm.setDocCreator(docCreator);
        workflowForm.setFormValues(formValues);
        workflowForm.setDocStatus(docStatus);

        //??????
        ArrayList<AttachFileDto> fileDtos = new ArrayList<AttachFileDto>();
        if(!ObjectUtils.isEmpty(assetFile)){
            assetFile.stream().forEach(
                    x->{
                        AttachFileDto attachFileDto = new AttachFileDto();
                        attachFileDto.setFdKey("fd_att");
                        attachFileDto.setFileId(x.getFileuploadId());
                        fileDtos.add(attachFileDto);
                    });
        }
        if(!ObjectUtils.isEmpty(techFile)){
            techFile.stream().forEach(
                    x->{
                        AttachFileDto attachFileDto = new AttachFileDto();
                        attachFileDto.setFdKey("fd_att");
                        attachFileDto.setFileId(x.getFileuploadId());
                        fileDtos.add(attachFileDto);
                    });
        }
        workflowForm.setFileDtos(fileDtos);


        WorkflowCreateResponseDto workflowCreateResponse;
        if ("N".equals(acceptDTO.getProcessType())){
            //???????????????
            // ????????????
            JSONObject flowParam = new JSONObject();
            flowParam.put("auditNote", "??????");
            flowParam.put("operationType", "drafter_abandon");
            workflowForm.setFlowParam(flowParam);
            workflowCreateResponse = workflowClient.updateFlow(workflowForm);
        }else {
            //???????????????
            // ????????????
            JSONObject flowParam = new JSONObject();
            String ceeaDraftsmanOpinion = StringUtils.isEmpty(acceptOrder.getCeeaDraftsmanOpinion()) ? "???????????????" : acceptOrder.getCeeaDraftsmanOpinion();
            flowParam.put("auditNote",ceeaDraftsmanOpinion );
            workflowForm.setFlowParam(flowParam);
            workflowCreateResponse = workflowClient.submitFlow(workflowForm);
        }

        formId = workflowCreateResponse.getProcessId();
        return formId;
    }


}
