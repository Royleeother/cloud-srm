package com.midea.cloud.srm.pr.logisticsRequirement.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.logistics.BusinessMode;
import com.midea.cloud.common.enums.logistics.LogisticsBusinessType;
import com.midea.cloud.common.enums.logistics.pr.requirement.LogisticsApplyAssignStyle;
import com.midea.cloud.common.enums.logistics.pr.requirement.LogisticsApplyProcessStatus;
import com.midea.cloud.common.enums.logistics.pr.requirement.LogisticsApproveStatus;
import com.midea.cloud.common.enums.pm.pr.requirement.RequirementApplyStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.logistics.template.service.ILogisticsTemplateHeadService;
import com.midea.cloud.srm.logistics.template.service.ILogisticsTemplateLineService;
import com.midea.cloud.srm.model.logistics.pr.requirement.dto.LogisticsPurchaseRequirementDTO;
import com.midea.cloud.srm.model.logistics.pr.requirement.dto.LogisticsRequirementHeadQueryDTO;
import com.midea.cloud.srm.model.logistics.pr.requirement.dto.LogisticsRequirementManageDTO;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementFile;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementHead;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementLine;
import com.midea.cloud.srm.model.logistics.template.entity.LogisticsTemplateLine;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.pr.logisticsRequirement.mapper.LogisticsRequirementHeadMapper;
import com.midea.cloud.srm.pr.logisticsRequirement.service.IRequirementFileService;
import com.midea.cloud.srm.pr.logisticsRequirement.service.IRequirementHeadService;
import com.midea.cloud.srm.pr.logisticsRequirement.service.IRequirementLineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ???????????????????????? ???????????????
 *  ???????????????????????????????????????(??????dev??????)
 * </pre>
 *
 * @author chenwt24@meiCloud.com
 * @version 1.00.00
 * <p>
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-11-27 10:59:47
 *  ????????????:
 * </pre>
 */
@Service(value = "LogisticsRequirementHeadServiceImpl")
@Slf4j
public class RequirementHeadServiceImpl extends ServiceImpl<LogisticsRequirementHeadMapper, LogisticsRequirementHead> implements IRequirementHeadService {

    @Resource
    private BaseClient baseClient;
    @Autowired
    private IRequirementLineService iRequirementLineService;
    @Autowired
    private IRequirementFileService iRequirementFileService;

    @Autowired
    private ILogisticsTemplateLineService iLogisticsTemplateLineService;
    @Autowired
    private ILogisticsTemplateHeadService logisticsTemplateHeadService;
    @Autowired
    private LogisticsRequirementHeadMapper requirementHeadMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addPurchaseRequirement(LogisticsPurchaseRequirementDTO purchaseRequirementDTO) {
        log.info("????????????????????????addPurchaseRequirement,?????????{}", purchaseRequirementDTO);
        LogisticsRequirementHead requirementHead = purchaseRequirementDTO.getRequirementHead();
        List<LogisticsRequirementLine> requirementLineList = purchaseRequirementDTO.getRequirementLineList();
        List<LogisticsRequirementFile> requirementFiles = purchaseRequirementDTO.getRequirementAttaches();

        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        //??????????????????
        String createdByName = loginAppUser.getNickname();

        long id = IdGenrator.generate();
        String status = requirementHead.getRequirementStatus();
        if(Objects.isNull(status)){
            status = LogisticsApproveStatus.DRAFT.getValue();
        }

        requirementHead.setRequirementHeadId(id)
                .setRequirementHeadNum(getRequirementNum(requirementHead))
                .setCreatedByName(createdByName)
                .setRequirementStatus(status);
        this.save(requirementHead);
        //???????????????????????????
        iRequirementLineService.addRequirementLineBatch(requirementHead, requirementLineList);
        //???????????????????????????
        iRequirementFileService.addRequirementFileBatch(requirementHead, requirementFiles);
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByHeadId(Long requirementHeadId) {
        log.info("???????????????????????????????????????{}",requirementHeadId);
        LogisticsRequirementHead requirementHead = this.getById(requirementHeadId);
        if(Objects.isNull(requirementHead)){
            throw new BaseException(String.format("???????????????????????????????????????id???%s",requirementHead));
        }
        //???????????????????????????
        if (LogisticsApproveStatus.DRAFT.getValue().equals(requirementHead.getRequirementStatus())) {
            this.removeById(requirementHeadId);
            List<LogisticsRequirementLine> rls = iRequirementLineService.list(new QueryWrapper<>(new LogisticsRequirementLine().setRequirementHeadId(requirementHeadId)));
            iRequirementLineService.remove(new QueryWrapper<>(new LogisticsRequirementLine().setRequirementHeadId(requirementHeadId)));
            iRequirementFileService.remove(new QueryWrapper<>(new LogisticsRequirementFile().setRequirementHeaderId(requirementHeadId)));
        } else {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long modifyPurchaseRequirement(LogisticsPurchaseRequirementDTO purchaseRequirementDTO) {
        LogisticsRequirementHead requirementHead = purchaseRequirementDTO.getRequirementHead();
        List<LogisticsRequirementLine> requirementLineList = purchaseRequirementDTO.getRequirementLineList();
        List<LogisticsRequirementFile> requirementAttaches = purchaseRequirementDTO.getRequirementAttaches();


        LogisticsRequirementHead head = this.getById(requirementHead.getRequirementHeadId());
        if (LogisticsApproveStatus.APPROVED.getValue().equals(head.getRequirementStatus())
                && LogisticsApproveStatus.APPROVING.getValue().equals(head.getRequirementStatus())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????"));
        }
        this.updateById(requirementHead);
        iRequirementLineService.updateBatch(requirementHead, requirementLineList);
        //??????????????????????????? (ceea)
        iRequirementFileService.updateRequirementAttachBatch(requirementHead, requirementAttaches);
        return head.getRequirementHeadId();
    }

    /**
     * <pre>
     *  //????????????????????????????????????D???????????????????????????????????????F???????????????????????????????????????S???
     * </pre>
     *
     * @author chenwt24@meicloud.com
     * @version 1.00.00
     * <p>
     * <pre>
     *  ????????????
     *  ???????????????:
     *  ?????????:
     *  ????????????: 2020-11-27
     *  ????????????:
     * </pre>
     */
    public String getRequirementNum(LogisticsRequirementHead requirementHead) {
        Objects.requireNonNull(requirementHead.getBusinessModeCode());
        String code = requirementHead.getBusinessModeCode();
        String reequirementNum = null;
        if (Objects.equals(BusinessMode.INSIDE.getValue(), code)) {
            reequirementNum = baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM_D);
        } else if (Objects.equals(BusinessMode.OUTSIDE.getValue(), code)) {
            reequirementNum = baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM_F);
        } else if (Objects.equals(BusinessMode.OVERSEA.getValue(), code)) {
            reequirementNum = baseClient.seqGen(SequenceCodeConstant.SEQ_PMP_PR_APPLY_NUM_S);
        }else {
            throw new BaseException(String.format("?????????????????????[%s]???????????????????????????",code));
        }
        return reequirementNum;
    }

    @Override
    public PageInfo<LogisticsRequirementHead> listPage(LogisticsRequirementHeadQueryDTO requirementHeadQueryDTO) {
        PageUtil.startPage(requirementHeadQueryDTO.getPageNum(), requirementHeadQueryDTO.getPageSize());
        List<LogisticsRequirementHead> requirementHeadList = requirementHeadMapper.list(requirementHeadQueryDTO);
        return new PageInfo<>(requirementHeadList);
    }

    @Override
    public LogisticsPurchaseRequirementDTO getByHeadId(Long requirementHeadId) {
        //??????
        Assert.notNull(requirementHeadId, LocaleHandler.getLocaleMsg("???????????????ID????????????"));
        LogisticsRequirementHead requirementHead = this.getById(requirementHeadId);
        Assert.notNull(requirementHead,LocaleHandler.getLocaleMsg(String.format("???????????????????????????,requirementHeadId=[%s]",requirementHeadId)));

        LogisticsPurchaseRequirementDTO vo = new LogisticsPurchaseRequirementDTO();
        vo.setRequirementHead(requirementHead);
        List<LogisticsRequirementLine> requirementLineList = iRequirementLineService.list(new QueryWrapper<>(new LogisticsRequirementLine().
                setRequirementHeadId(requirementHeadId)));
        vo.setRequirementLineList(requirementLineList);
        List<LogisticsRequirementFile> requirementAttaches = iRequirementFileService.list(new QueryWrapper<>(new LogisticsRequirementFile()
                .setRequirementHeaderId(requirementHeadId)));
        vo.setRequirementAttaches(requirementAttaches);
        //?????????????????????
        vo.setLogisticsTemplateHead(logisticsTemplateHeadService.getById(vo.getRequirementHead().getTemplateHeadId()));
        //?????????????????????
        List<LogisticsTemplateLine> logisticsTemplateLineList = iLogisticsTemplateLineService.list(new QueryWrapper<>(new LogisticsTemplateLine().setHeadId(vo.getRequirementHead().getTemplateHeadId())));
        vo.setLogisticsTemplateLineList(logisticsTemplateLineList);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(LogisticsPurchaseRequirementDTO purchaseRequirementDTO) {
        log.info("????????????????????????????????????{}",purchaseRequirementDTO.toString());
        LogisticsRequirementHead requirementHead = purchaseRequirementDTO.getRequirementHead();
        List<LogisticsRequirementLine> requirementLineList = purchaseRequirementDTO.getRequirementLineList();
        List<LogisticsRequirementFile> requirementAttaches = purchaseRequirementDTO.getRequirementAttaches();
        requirementHead.setRequirementStatus(LogisticsApproveStatus.APPROVING.getValue())
                .setApplyProcessStatus(LogisticsApplyProcessStatus.UNPROCESSED.getValue())
                .setApplyAssignStatus(LogisticsApplyAssignStyle.UNASSIGNED.getValue());

        checkForSubmit(requirementHead,requirementLineList);

        Long requirementHeadId = null;
        if (requirementHead.getRequirementHeadId() == null) {
            requirementHeadId = this.addPurchaseRequirement(purchaseRequirementDTO);
        } else {
            requirementHeadId = this.modifyPurchaseRequirement(purchaseRequirementDTO);
            this.updateById(requirementHead);
        }

        //?????????????????????
        approval(requirementHeadId);
    }

    /**
     * ???????????????????????????
     * @param requirementHeadId
     */
    @Transactional
    public void approval(Long requirementHeadId) {
        LogisticsRequirementHead requirementHead = this.getById(requirementHeadId);
        if(Objects.isNull(requirementHead)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????requirementHeadId = [" + requirementHeadId + "]"));
        }
        this.updateById(requirementHead.setRequirementStatus(LogisticsApproveStatus.APPROVED.getValue()));
    }

    public void checkForSubmit(LogisticsRequirementHead requirementHead,List<LogisticsRequirementLine> requirementLineList){
        Assert.notNull(requirementHead, LocaleHandler.getLocaleMsg("???????????????????????????"));
        Assert.notEmpty(requirementLineList, LocaleHandler.getLocaleMsg("????????????????????????????????????"));
        String errorMsg = "%s??????*?????????????????????????????????????????????";
        //??????????????????????????????????????????
        Assert.notNull(requirementHead.getTemplateHeadId(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.hasText(requirementHead.getBusinessModeCode(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.hasText(requirementHead.getTransportModeCode(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.hasText(requirementHead.getBusinessType(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????????????????")));
//        Assert.hasText(requirementHead.getUnit(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"??????")));
//        Assert.notNull(requirementHead.getProjectTotal(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.notNull(requirementHead.getDemandDate(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.notNull(requirementHead.getBudgetAmount(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.notNull(requirementHead.getCurrencyId(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"??????")));
        Assert.notNull(requirementHead.getPriceStartDate(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????????????????")));
        Assert.notNull(requirementHead.getPriceEndDate(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????????????????")));


        //ii.	???????????????????????????????????????????????????????????????
        if(Objects.equals(requirementHead.getBusinessType(), LogisticsBusinessType.PROJECT.getValue())
                && StringUtils.isEmpty(requirementHead.getServiceProjectCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????"));
        }

        //??????????????????
        checkLineByTemplate(requirementHead,requirementLineList);

    }

    /**
     * <pre>
     *  //?????????????????????????????????????????????
     * </pre>
     *
     * @author chenwt24@meicloud.com
     * @version 1.00.00
     *
     * <pre>
     *  ????????????
     *  ???????????????:
     *  ?????????:
     *  ????????????: 2020-11-30
     *  ????????????:
     * </pre>
     */
    public void checkLineByTemplate(LogisticsRequirementHead requirementHead,List<LogisticsRequirementLine> requirementLineList){
        String templateName = requirementHead.getTemplateName();
        Objects.requireNonNull(templateName);

        Long templateHeadId = requirementHead.getTemplateHeadId();
        List<LogisticsTemplateLine> lines = iLogisticsTemplateLineService.list(Wrappers.lambdaQuery(LogisticsTemplateLine.class)
                .eq(LogisticsTemplateLine::getHeadId, templateHeadId));

        List<LogisticsTemplateLine> notEmptyList = lines.parallelStream().filter(x -> Objects.equals(x.getApplyNotEmptyFlag(), "Y")).collect(Collectors.toList());

        for (int i = 0; i < notEmptyList.size(); i++) {
            LogisticsTemplateLine logisticsTemplateLine = notEmptyList.get(i);
            for (int j = 0; j < requirementLineList.size(); j++) {
                Object o = null;
                try {
                    o = doCheck(logisticsTemplateLine.getFieldCode(), requirementLineList.get(j));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if(Objects.isNull(o)){
                    throw new BaseException(String.format("?????????%S,[%S]????????????",templateName,logisticsTemplateLine.getFieldName()));
                }
            }
        }
    }

    public Object doCheck(String fieldCode,LogisticsRequirementLine line) throws IllegalAccessException {

        Field[] declaredFields = LogisticsRequirementLine.class.getDeclaredFields();
        for (Field s:declaredFields){
            boolean annotationPresent = s.isAnnotationPresent(TableField.class);
            if(annotationPresent && StringUtils.isNotBlank(s.getName())){
                TableField annotation = s.getAnnotation(TableField.class);
                String value = annotation.value();
                s.setAccessible(true);
                if(Objects.equals(fieldCode,value)){
                    return s.get(line);
                }
            }
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApproved(Long requirementHeadId, String auditStatus) {
        LogisticsRequirementHead byId = this.getById(requirementHeadId);
        //List<LogisticsRequirementLine> requirementLineList = iRequirementLineService.list(new QueryWrapper<>(new LogisticsRequirementLine().setRequirementHeadId(requirementHeadId)));
        if (byId != null && !LogisticsApproveStatus.APPROVING.getValue().equals(byId.getRequirementStatus())) {
            throw new BaseException("????????????????????????,????????????,?????????!");
        }
        // ???????????? ???????????????????????????(?????????)
        this.updateById(byId.setRequirementStatus(auditStatus).setApplyAssignStatus(LogisticsApplyAssignStyle.UNASSIGNED.getValue()));

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long requirementHeadId, String rejectReason) {
        this.updateById(new LogisticsRequirementHead().setRequirementHeadId(requirementHeadId).
                setRequirementStatus(LogisticsApproveStatus.CANCEL.getValue()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long requirementHeadId, String rejectReason) {
        this.updateById(new LogisticsRequirementHead().setRequirementHeadId(requirementHeadId).
                setRequirementStatus(LogisticsApproveStatus.REJECTED.getValue()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandon(Long requirementHeadId) {
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        LogisticsRequirementHead head = this.getById(requirementHeadId);
        if(!Objects.equals(head.getRequirementStatus(),LogisticsApproveStatus.REJECTED.getValue())
                && !Objects.equals(head.getRequirementStatus(),LogisticsApproveStatus.APPROVED.getValue())){
            throw new BaseException("?????????????????????????????????????????????");
        }
        if(Objects.equals(head.getRequirementStatus(),RequirementApplyStatus.ASSIGNED.getValue())){
            throw new BaseException("??????????????????????????????,???????????????????????????????????????????????????");
        }
        this.updateById(new LogisticsRequirementHead().setRequirementHeadId(requirementHeadId).
                setRequirementStatus(LogisticsApproveStatus.CANCEL.getValue()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bachAssigned(LogisticsRequirementManageDTO requirementManageDTO) {
        List<Long> requirementHeadIds = requirementManageDTO.getRequirementHeadIds();
        Assert.notEmpty(requirementHeadIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        List<LogisticsRequirementHead> requirementHeads = this.listByIds(requirementHeadIds);
        for (LogisticsRequirementHead requirementHead : requirementHeads) {
            if (requirementHead == null) continue;
            if(StringUtils.isNotEmpty(requirementHead.getBidingCode())){
                throw new BaseException("?????????????????????????????????????????????");
            }
            requirementHead.setCeeaApplyUserId(requirementManageDTO.getCeeaApplyUserId());
            requirementHead.setCeeaApplyUserName(requirementManageDTO.getCeeaApplyUserName());
            requirementHead.setCeeaApplyUserNickname(requirementManageDTO.getCeeaApplyUserNickname());

            requirementHead.setApplyAssignStatus(LogisticsApplyAssignStyle.ASSIGNED.getValue());
            this.updateById(requirementHead);
        }
    }

    @Override
    public void bachUnAssigned(LogisticsRequirementManageDTO requirementManageDTO) {
        List<Long> requirementHeadIds = requirementManageDTO.getRequirementHeadIds();
        Assert.notEmpty(requirementHeadIds, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        List<LogisticsRequirementHead> requirementHeads = this.listByIds(requirementHeadIds);
        for (LogisticsRequirementHead requirementHead : requirementHeads) {
            if (requirementHead == null) continue;
            if(StringUtils.isNotEmpty(requirementHead.getBidingCode())){
                throw new BaseException("??????????????????????????????????????????");
            }
            requirementHead.setCeeaApplyUserId(null);
            requirementHead.setCeeaApplyUserName(null);
            requirementHead.setCeeaApplyUserNickname(null);

            requirementHead.setApplyAssignStatus(LogisticsApplyAssignStyle.UNASSIGNED.getValue());
            this.updateById(requirementHead);
        }
    }

    /**
     * ???????????????????????????
     * @param requirementHeadQueryDTO
     * @return
     */
    @Override
    public List<LogisticsRequirementHead> listPageNew(LogisticsRequirementHeadQueryDTO requirementHeadQueryDTO) {

        LogisticsRequirementHead headQuery = new LogisticsRequirementHead();
        QueryWrapper<LogisticsRequirementHead> wrapper = new QueryWrapper<>(headQuery);
        // ??????????????? ?????? ????????????????????????
        wrapper.eq("REQUIREMENT_STATUS", LogisticsApproveStatus.APPROVED.getValue());
        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getRequirementHeadNum()),
                "REQUIREMENT_HEAD_NUM", requirementHeadQueryDTO.getRequirementHeadNum());

        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getRequirementTitle()),
                "REQUIREMENT_TITLE", requirementHeadQueryDTO.getRequirementTitle());

        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getBusinessModeCode()),
                "BUSINESS_MODE_CODE", requirementHeadQueryDTO.getBusinessModeCode());

        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getTransportModeCode()),
                "TRANSPORT_MODE_CODE", requirementHeadQueryDTO.getTransportModeCode());

        wrapper.eq(StringUtils.isNotEmpty(requirementHeadQueryDTO.getBusinessType()),
                "BUSINESS_TYPE", requirementHeadQueryDTO.getBusinessType());

        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getApplyBy()),
                "APPLY_BY", requirementHeadQueryDTO.getApplyBy());

        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getApplyDepartmentName()),
                "APPLY_DEPARTMENT_NAME", requirementHeadQueryDTO.getApplyDepartmentName());

        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getServiceProjectName()),
                "SERVICE_PROJECT_NAME", requirementHeadQueryDTO.getServiceProjectName());


        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getCeeaApplyUserNickname()),
                "CEEA_APPLY_USER_NICKNAME", requirementHeadQueryDTO.getCeeaApplyUserNickname());
        wrapper.like(StringUtils.isNotEmpty(requirementHeadQueryDTO.getTemplateName()),
                "TEMPLATE_NAME", requirementHeadQueryDTO.getTemplateName());

        wrapper.eq(StringUtils.isNotEmpty(requirementHeadQueryDTO.getRequirementStatus()),
                "REQUIREMENT_STATUS", requirementHeadQueryDTO.getRequirementStatus());
        //?????????????????????
        wrapper.eq(StringUtils.isNotEmpty(requirementHeadQueryDTO.getApplyProcessStatus()),
                "APPLY_PROCESS_STATUS", requirementHeadQueryDTO.getApplyProcessStatus());
        //?????????????????????
        wrapper.eq(StringUtils.isNotEmpty(requirementHeadQueryDTO.getApplyAssignStatus()),
                "APPLY_ASSIGN_STATUS",requirementHeadQueryDTO.getApplyAssignStatus());

        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return this.list(wrapper);
    }

    /**
     * ????????????????????????
     * @param requirementHeadIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> requirementHeadIds) {
        //??????
        if(CollectionUtils.isEmpty(requirementHeadIds)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<LogisticsRequirementHead> logisticsRequirementHeads = this.listByIds(requirementHeadIds);
        for(LogisticsRequirementHead requirementHead : logisticsRequirementHeads){
            if(!LogisticsApproveStatus.DRAFT.getValue().equals(requirementHead.getRequirementStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????"));
            }
        }
        //??????
        this.removeByIds(requirementHeadIds);
    }

    /**
     * ????????????????????????
     * @param requirementHeadIds
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCopy(List<Long> requirementHeadIds) {
        //??????
        if(CollectionUtils.isEmpty(requirementHeadIds)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<LogisticsRequirementHead> logisticsRequirementHeads = this.listByIds(requirementHeadIds);

        //????????????
        QueryWrapper<LogisticsRequirementLine> wrapper = new QueryWrapper<>();
        wrapper.in("REQUIREMENT_HEAD_ID",requirementHeadIds);
        List<LogisticsRequirementLine> logisticsRequirementLines = iRequirementLineService.list(wrapper);
        List<LogisticsPurchaseRequirementDTO> requirementDTOList = buildRequirement(logisticsRequirementHeads,logisticsRequirementLines);

        //????????????
        List<LogisticsRequirementHead> requirementHeadAdds = new LinkedList<>();
        List<LogisticsRequirementLine> requirementLineAdds = new LinkedList<>();
        for(LogisticsPurchaseRequirementDTO requirementDTO : requirementDTOList){
            LogisticsRequirementHead requirementHead = requirementDTO.getRequirementHead();
            List<LogisticsRequirementLine> requirementLineList = requirementDTO.getRequirementLineList();

            Long id = IdGenrator.generate();
            String status = LogisticsApproveStatus.DRAFT.getValue();
            String requirementHeadNum = getRequirementNum(requirementHead);
            requirementHead.setRequirementHeadId(id)
                    .setRequirementStatus(status)
                    .setRequirementHeadNum(requirementHeadNum);

            if(!CollectionUtils.isEmpty(requirementLineList)){
                for(LogisticsRequirementLine requirementLine : requirementLineList){
                    requirementLine.setRequirementLineId(IdGenrator.generate())
                            .setRequirementHeadId(id)
                            .setRequirementHeadNum(requirementHeadNum);
                }
            }

            //?????????????????????
//            requirementHead.setOrderHeadNum(null);
            //?????????????????????
            requirementHead.setBidingId(null);
            requirementHead.setBidingCode(null);
            requirementHead.setBidingName(null);
            //????????????????????????
            requirementHead.setCeeaApplyUserId(null);
            requirementHead.setCeeaApplyUserName(null);
            requirementHead.setCeeaApplyUserNickname(null);
            //??????????????????????????????
            requirementHead.setApplyProcessStatus(LogisticsApplyProcessStatus.UNPROCESSED.getValue());
            //??????????????????????????????
            requirementHead.setApplyAssignStatus(LogisticsApplyAssignStyle.UNASSIGNED.getValue());

            requirementHeadAdds.add(requirementHead);
            if(!CollectionUtils.isEmpty(requirementLineList)){
                requirementLineAdds.addAll(requirementLineList);
            }
        }
        if(!CollectionUtils.isEmpty(requirementHeadAdds)){
            this.saveBatch(requirementHeadAdds);
        }
        if(!CollectionUtils.isEmpty(requirementLineAdds)){
            iRequirementLineService.saveBatch(requirementLineAdds);
        }

        return requirementHeadAdds.stream().map(item -> item.getRequirementHeadId()).collect(Collectors.toList());
    }

    /**
     * ????????????????????????
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long id) {
        LogisticsRequirementHead requirementHead = this.getById(id);
        requirementHead.setBidingId(null).setBidingName(null).setBidingCode(null);
        if(!LogisticsApplyProcessStatus.FINISHED.getValue().equals(requirementHead.getApplyProcessStatus())){
            requirementHead.setApplyProcessStatus(LogisticsApplyProcessStatus.UNPROCESSED.getValue());
        }
        this.updateById(requirementHead);
    }

    /**
     * ??????????????????????????????
     * @param requirementHeadNum
     */
    @Override
    @Transactional
    public void release(List<String> requirementHeadNum) {
        if(!CollectionUtils.isEmpty(requirementHeadNum)){
            List<LogisticsRequirementHead> requirementHeads = this.list(Wrappers.lambdaQuery(LogisticsRequirementHead.class).
                    in(LogisticsRequirementHead::getRequirementHeadNum, requirementHeadNum));
            if(!CollectionUtils.isEmpty(requirementHeads)){
                requirementHeads.forEach(item -> {
                    item.setBidingId(null).setBidingName(null).setBidingCode(null);
                    if(!LogisticsApplyProcessStatus.FINISHED.getValue().equals(item.getApplyProcessStatus())){
                        item.setApplyProcessStatus(LogisticsApplyProcessStatus.UNPROCESSED.getValue());
                    }
                    this.updateBatchById(requirementHeads);
                });
            }
        }
    }

    /**
     * ??????????????????DTO
     * @param requirementHeadList
     * @param requirementLineList
     * @return
     */
    List<LogisticsPurchaseRequirementDTO> buildRequirement(List<LogisticsRequirementHead> requirementHeadList,List<LogisticsRequirementLine> requirementLineList){
        List<LogisticsPurchaseRequirementDTO> result = new LinkedList<>();
        Map<Long,LogisticsRequirementHead> requirementHeadMap = requirementHeadList.stream().collect(Collectors.toMap(item -> item.getRequirementHeadId(),item -> item));
        Map<Long,List<LogisticsRequirementLine>> requirementLineMap = requirementLineList.stream().collect(Collectors.groupingBy(LogisticsRequirementLine::getRequirementHeadId));
        for(Map.Entry<Long,LogisticsRequirementHead> entry : requirementHeadMap.entrySet()){
            LogisticsPurchaseRequirementDTO requirementDTO = new LogisticsPurchaseRequirementDTO();
            Long requirementHeadId = entry.getKey();
            requirementDTO.setRequirementHead(entry.getValue()).setRequirementLineList(requirementLineMap.get(entry.getKey()));
            result.add(requirementDTO);
        }
        return result;
    }

}
