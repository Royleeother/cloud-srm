package com.midea.cloud.srm.pr.requirement.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.BaseConst;
import com.midea.cloud.common.enums.pm.pr.requirement.RequirementApproveStatus;
import com.midea.cloud.common.enums.pm.ps.FSSCResponseCode;
import com.midea.cloud.common.enums.pm.ps.FsscResponseType;
import com.midea.cloud.common.enums.rbac.RoleFuncSetTypeEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.StringUtil;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementLine;
import com.midea.cloud.srm.model.pm.ps.http.FSSCResult;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.PurchaseRequirementDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.RequirementHeadQueryDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementHead;
import com.midea.cloud.srm.model.pm.pr.requirement.vo.RequirementApplyRejectVO;
import com.midea.cloud.srm.model.rbac.role.entity.Role;
import com.midea.cloud.srm.model.rbac.role.entity.RoleFuncSet;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.pr.requirement.service.IRequirementHeadService;
import com.midea.cloud.srm.pr.requirement.service.IRequirementLineService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ?????????????????? ???????????????
 * </pre>
 *
 * @author fengdc3@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-05-12 18:46:40
 *  ????????????:
 * </pre>
 */
@RestController
@RequestMapping("/pr/requirementHead")
public class RequirementHeadController extends BaseController {

    @Autowired
    private IRequirementHeadService iRequirementHeadService;

    @Resource
    private RbacClient rbacClient;

    @Resource
    private BaseClient baseClient;
    @Autowired
    private IRequirementLineService lineService;


    /**
     * ????????????
     *
     * @param requirementHeadId
     */
    @GetMapping("/getByHeadId")
    public PurchaseRequirementDTO getByHeadId(Long requirementHeadId) {
        return iRequirementHeadService.getByHeadId(requirementHeadId);
    }

    /**
     * ??????
     *
     * @param purchaseRequirementDTO
     */
//    @PreAuthorize("hasAuthority('pr:requirementApply:add')")
    @PostMapping("/addPurchaseRequirement")
    public Long addPurchaseRequirement(@RequestBody PurchaseRequirementDTO purchaseRequirementDTO) {
        if (!RequirementApproveStatus.DRAFT.getValue().equals(purchaseRequirementDTO.getAuditStatus()) && !RequirementApproveStatus.SUBMITTED.getValue().equals(purchaseRequirementDTO.getAuditStatus())) {
            throw new BaseException("????????????????????????????????????");
        }
        return iRequirementHeadService.addPurchaseRequirement(purchaseRequirementDTO, purchaseRequirementDTO.getAuditStatus());
    }

    /**
     * ??????
     *
     * @param requirementHeadId
     */
//    @PreAuthorize("hasAuthority('pr:requirementApply:delete')")
    @GetMapping("/deleteByHeadId")
    public void deleteByHeadId(Long requirementHeadId) {
        iRequirementHeadService.deleteByHeadId(requirementHeadId);
    }

    /**
     * ??????
     *
     * @param purchaseRequirementDTO
     */
//    @PreAuthorize("hasAuthority('pr:requirementApply:edit')")
    @PostMapping("/modifyPurchaseRequirement")
    public Long modifyPurchaseRequirement(@RequestBody PurchaseRequirementDTO purchaseRequirementDTO) {
        if (!RequirementApproveStatus.DRAFT.getValue().equals(purchaseRequirementDTO.getAuditStatus()) && !RequirementApproveStatus.SUBMITTED.getValue().equals(purchaseRequirementDTO.getAuditStatus())) {
            throw new BaseException("????????????????????????????????????");
        }
        return iRequirementHeadService.modifyPurchaseRequirement(purchaseRequirementDTO, purchaseRequirementDTO.getAuditStatus());
    }

    /**
     * Description ???????????????????????????code?????????????????????Map(key???allQuery????????????????????????buQuery-?????????ID??????,ouQuery-????????????ID??????
     * invQuery-????????????ID)
     *
     * @return
     * @throws
     * @Param
     * @Author wuwl18@meicloud.com
     * @Date 2020.10.25
     **/
    public Map<String, Object> getRoleFunction(LoginAppUser user, String functionCode) {
        Map<String, Object> queryMap = new HashMap<>();
        if (null != user) {
            List<Role> roleList = user.getRolePermissions();
            StringBuilder roleStr = new StringBuilder();
            roleList.forEach(role -> {
                roleStr.append(role.getRoleCode()).append(",");
            });
            //????????????Code????????????-??????????????????
            List<RoleFuncSet> roleFuncSetList = rbacClient.findRoleFuncSetByParam(new RoleFuncSet()
                    .setFunctionCode(functionCode)
                    .setRoleCode(roleStr.toString()));

            //???????????????????????????-?????????
            List<Organization> queryOrgList = baseClient.listAllOrganization();
            //OU-???????????????,BU-????????????,INVENTORY-???????????????,CATEGORY-???????????????,OU_CATEGORY-???OU+??????,ALL-??????
            StringBuilder allQuery = new StringBuilder(); //?????????????????????
            StringBuilder buQuery = new StringBuilder(); //?????????ID????????????
            StringBuilder ouQuery = new StringBuilder(); //????????????ID????????????
            StringBuilder invQuery = new StringBuilder(); //????????????ID????????????
            roleFuncSetList.forEach(roleFuncSet -> {
                if (null != roleFuncSet) {
                    String roleFuncSetType = roleFuncSet.getRoleFuncSetType();
                    if (RoleFuncSetTypeEnum.ALL.getKey().equals(roleFuncSetType)) { //??????
                    } else if (RoleFuncSetTypeEnum.OU.getKey().equals(roleFuncSetType) && CollectionUtils.isNotEmpty(user.getOrganizationUsers())) { //?????????
                        user.getOrganizationUsers().forEach(orgUser -> {
                            if (null != orgUser) {
                                queryOrgList.forEach(queryOrg -> {
                                    if (orgUser.getOrganizationId().compareTo(queryOrg.getOrganizationId()) == 0
                                            && BaseConst.ORG_BU.equals(queryOrg.getOrganizationTypeCode())) {
                                        buQuery.append(queryOrg.getDivisionId());
                                        return;
                                    }
                                });
                            }
                        });
                    } else if (RoleFuncSetTypeEnum.OU.getKey().equals(roleFuncSetType) && CollectionUtils.isNotEmpty(user.getOrganizationUsers())) { //????????????
                        user.getOrganizationUsers().forEach(orgUser -> {
                            if (null != orgUser) {
                                queryOrgList.forEach(queryOrg -> {
                                    if (orgUser.getOrganizationId().compareTo(queryOrg.getOrganizationId()) == 0
                                            && BaseConst.ORG_OU.equals(queryOrg.getOrganizationTypeCode())) {
                                        ouQuery.append(queryOrg.getOrganizationId());
                                        return;
                                    }
                                });
                            }
                        });
                    } else if (RoleFuncSetTypeEnum.INVENTORY.getKey().equals(roleFuncSetType) && CollectionUtils.isNotEmpty(user.getOrganizationUsers())) { //????????????
                        user.getOrganizationUsers().forEach(orgUser -> {
                            if (null != orgUser) {
                                queryOrgList.forEach(queryOrg -> {
                                    if (orgUser.getOrganizationId().compareTo(queryOrg.getOrganizationId()) == 0
                                            && BaseConst.ORG_INV.equals(queryOrg.getOrganizationTypeCode())) {
                                        invQuery.append(queryOrg.getOrganizationId());
                                        return;
                                    }
                                });
                            }
                        });
                    }
                }
            });
            queryMap.put("allQuery", allQuery);
            queryMap.put("buQuery", buQuery);
            queryMap.put("invQuery", invQuery);
            queryMap.put("ouQuery", ouQuery);
        }
        return queryMap;
    }

    /**
     * ??????/?????????-????????????
     *
     * @param requirementHeadQueryDTO
     * @return
     */
    @PostMapping("/listPage")
    public PageInfo<RequirementHead> listPage(@RequestBody RequirementHeadQueryDTO requirementHeadQueryDTO) {
        return iRequirementHeadService.listPage(requirementHeadQueryDTO);
    }

    /**
     * ??????????????????
     *
     * @param requirementHeadQueryDTO
     */
    @PostMapping("/exportExcel")
    public BaseResult exportExcel(@RequestBody RequirementHeadQueryDTO requirementHeadQueryDTO, HttpServletResponse response) throws IOException {
        BaseResult baseResult = BaseResult.buildSuccess();
        try {
            /*iRequirementHeadService.exportExcel(requirementHeadQueryDTO, response);*/
            iRequirementHeadService.exportExcelNew(requirementHeadQueryDTO,response);
        } catch (Exception e) {
            baseResult = BaseResult.build(ResultCode.UNKNOWN_ERROR, e.getMessage());
        }
        return baseResult;
    }

    /**
     * ?????????????????????
     *
     * @param requirementHeadQueryDTO
     * @return
     */
    @PostMapping("/getExportCount")
    public Long getExportCount(@RequestBody RequirementHeadQueryDTO requirementHeadQueryDTO) {
        return iRequirementHeadService.getExportCount(requirementHeadQueryDTO);
    }

//    /**
//     * ????????????(longi)
//     *
//     * @param purchaseRequirementDTO
//     */
////    @PreAuthorize("hasAuthority('pr:requirementApply:submitAudit')")
//    @PostMapping("/submitApproval")
//    public BaseResult submitApproval(@RequestBody PurchaseRequirementDTO purchaseRequirementDTO) {
//        FSSCResult fsscResult = iRequirementHeadService.submitApproval(purchaseRequirementDTO, RequirementApproveStatus.SUBMITTED.getValue());
//        BaseResult baseResult = BaseResult.buildSuccess();
//        if (FSSCResponseCode.WARN.getCode().equals(fsscResult.getCode())) {
//            baseResult.setCode(fsscResult.getCode());
//            baseResult.setMessage(fsscResult.getMsg());
//            baseResult.setData(fsscResult.getType());
//        }
//        return baseResult;
//    }

    /**
     * ??????????????? ????????????????????????????????????
     * @param purchaseRequirementDTO
     * @return
     */
    @PostMapping("/submitPurchaseRequirement")
    public Long submitPurchaseRequirement(@RequestBody PurchaseRequirementDTO purchaseRequirementDTO) {
        return iRequirementHeadService.submitPurchaseRequirement(purchaseRequirementDTO);
    }

    /**
     * ????????????(longi)
     *
     * @param purchaseRequirementDTO
     */
//    @PreAuthorize("hasAuthority('pr:requirementApply:submitAudit')")
    @PostMapping("/submitApproval")
    public BaseResult submitApproval(@RequestBody PurchaseRequirementDTO purchaseRequirementDTO) {
        String project = "??????";
        BaseResult baseResult = BaseResult.buildSuccess();
        FSSCResult fsscResult = iRequirementHeadService.submitApproval(purchaseRequirementDTO, RequirementApproveStatus.SUBMITTED.getValue());
        if (project.equals("??????")) {
            if (FSSCResponseCode.WARN.getCode().equals(fsscResult.getCode())) {
                baseResult.setCode(fsscResult.getCode());
                baseResult.setMessage(fsscResult.getMsg());
                baseResult.setData(fsscResult.getType());
            }
        } else {
            baseResult.setData(fsscResult.getBoeNo());
        }
        return baseResult;
    }

    /**
     * ????????????(??????)
     *
     * @param requirementHeadId
     */
//    @PreAuthorize("hasAuthority('pr:requirementApply:confirm')")
    @GetMapping("/approval")
    public void approval(Long requirementHeadId) {
        iRequirementHeadService.updateApproved(requirementHeadId, RequirementApproveStatus.APPROVED.getValue());
    }

    /**
     * ??????(??????)
     *
     * @param requirementHeadId
     */
//    @PreAuthorize("hasAuthority('pr:requirementApply:cancel')")
    @GetMapping("/abandon")
    public void abandon(Long requirementHeadId) {
        iRequirementHeadService.abandon(requirementHeadId);
    }

    /**
     * ?????????????????????
     *
     * @param requirementHeadQueryDTO
     * @return
     */
    @GetMapping("/getRequirementHeadByParam")
    public RequirementHead getRequirementHeadByParam(@RequestBody RequirementHeadQueryDTO requirementHeadQueryDTO) {
        return iRequirementHeadService.getRequirementHeadByParam(requirementHeadQueryDTO);
    }

    /**
     * ??????(??????)
     *
     * @param requirementApplyRejectVO
     */
//    @PreAuthorize("hasAuthority('pr:requirementApply:refuse')")
    @PostMapping("/reject")
    public void reject(@RequestBody RequirementApplyRejectVO requirementApplyRejectVO) {
        iRequirementHeadService.reject(requirementApplyRejectVO.getRequirementHeadId(),
                requirementApplyRejectVO.getRejectReason());
    }

    /**
     * <pre>
     *  ????????????
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
    @PostMapping("/withdraw")
    public void withdraw(@RequestBody RequirementApplyRejectVO requirementApplyRejectVO) {
        iRequirementHeadService.withdraw(requirementApplyRejectVO.getRequirementHeadId(),
                requirementApplyRejectVO.getRejectReason());
    }

    /**
     * ????????????????????????????????????
     *
     * @param requirementHead
     * @return
     */
    @PostMapping("/getRequirementHeadByParam")
    public RequirementHead queryByRequirementHead(@RequestBody RequirementHead requirementHead) {
        RequirementHead requirementHead1 = null;
        if (StringUtil.notEmpty(requirementHead.getRequirementHeadNum())) {
            List<RequirementHead> requirementHeads = iRequirementHeadService.list(new QueryWrapper<>(requirementHead));
            if (CollectionUtils.isNotEmpty(requirementHeads)) {
                requirementHead1 = requirementHeads.get(0);
            }
        }
        return requirementHead1;
    }

    @PostMapping("/getRequirementHeadByNumber")
    public List<RequirementHead> getRequirementHeadByNumber(@RequestBody Collection<String> number) {
        if (CollectionUtils.isEmpty(number)) {
            return Collections.emptyList();
        }
        return iRequirementHeadService.list(Wrappers.lambdaQuery(RequirementHead.class)
                .select(RequirementHead::getCeeaProjectNum, RequirementHead::getCeeaProjectName)
                .in(RequirementHead::getRequirementHeadNum, number)
        );
    }

    @PostMapping("/holdRequirementLine")
    public void holdRequirementLine(@RequestBody Collection<Long> lineIds) {
        if (org.springframework.util.CollectionUtils.isEmpty(lineIds)) {
            return;
        }
        List<Long> param = lineIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (org.springframework.util.CollectionUtils.isEmpty(param)) {
            return;
        }
        lineService.update(Wrappers.lambdaUpdate(RequirementLine.class)
                .set(RequirementLine::getIfHold, "Y")
                .in(RequirementLine::getRequirementLineId, param)
        );
    }
    @PostMapping("/releaseRequirementLine")
    public void releaseRequirementLine(@RequestBody Collection<Long> lineIds) {
        if (org.springframework.util.CollectionUtils.isEmpty(lineIds)) {
            return;
        }
        List<Long> param = lineIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (org.springframework.util.CollectionUtils.isEmpty(param)) {
            return;
        }
        lineService.update(Wrappers.lambdaUpdate(RequirementLine.class)
                .set(RequirementLine::getIfHold, "N")
                .in(RequirementLine::getRequirementLineId, param)
        );
    }
}
