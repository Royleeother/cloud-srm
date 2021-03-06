package com.midea.cloud.srm.perf.template.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.midea.cloud.common.enums.perf.scoreproject.ScoreItemsApproveStatusEnum;
import com.midea.cloud.common.enums.perf.scoreproject.ScoreItemsProjectStatusEnum;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItems;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemsMan;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemsSup;
import com.midea.cloud.srm.model.supplier.info.entity.CompanyInfo;
import com.midea.cloud.srm.perf.common.ScoreItemsConst;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsService;
import org.apache.commons.collections4.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.Enable;
import com.midea.cloud.common.enums.perf.template.PerfTemplateStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.model.perf.template.dto.*;
import com.midea.cloud.srm.model.perf.template.entity.*;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.perf.template.mapper.PerfTemplateHeaderMapper;
import com.midea.cloud.srm.perf.template.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  <pre>
 *  ?????????????????? ???????????????
 * </pre>
 *
 * @author wuwl18@meiCloud.com
 * @version 1.00.00
 *
 *  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-05-28 16:41:07
 *  ????????????:
 * </pre>
 */
@Service
public class PerfTemplateHeaderServiceImpl extends ServiceImpl<PerfTemplateHeaderMapper, PerfTemplateHeader>
        implements IPerfTemplateHeaderService {

    /**??????????????????Service??????*/
    @Autowired
    private IPerfTemplateCategoryService iPerfTemplateCategoryService;

    /**???????????????????????????Service*/
    @Autowired
    private IPerfTemplateDimWeightService iPerfTemplateDimWeightService;

    /**???????????????????????? Service*/
    @Autowired
    private IPerfTemplateLineService iPerfTemplateLineService;

    /**???????????????????????????Service*/
    @Autowired
    private IPerfTemplateIndsLineService iPerfTemplateIndsLineService;

    /**??????????????????Service*/
    @Resource
    private IPerfScoreItemsService iPerfScoreItemsService;

    @Autowired
    private SupplierClient supplierClient;

    @Autowired
    private BaseClient baseClient;

    @Override
    public List<PerfTemplateHeaderDTO> findPerTemplateHeadList(PerfTemplateHeaderQueryDTO pefTemplateHeader) throws BaseException {
        List<PerfTemplateHeaderDTO> pefTemplateList = new ArrayList<>();
        try{
            pefTemplateHeader.setDeleteFlag(Enable.N.toString());
            pefTemplateList = getBaseMapper().findPerTemplateHeadList(pefTemplateHeader);
        }catch (Exception e){
            log.error("????????????(?????????????????????????????????)?????????????????????????????????",e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }
        return pefTemplateList;
    }

    @Override
    public PerfTemplateDTO findPerTemplateByTemplateHeadId(Long perfTemplateHeaderId) throws BaseException {
        Assert.notNull(perfTemplateHeaderId, "id????????????");
        PerfTemplateDTO perfTemplateDTO = new PerfTemplateDTO();

        /**??????????????????ID???????????????????????????????????????*/
        try{
            PerfTemplateHeader perfTemplateHeader = getBaseMapper().selectById(perfTemplateHeaderId);
            perfTemplateDTO.setPerfTemplateHeader(perfTemplateHeader);
            PerfTemplateCategory perfTemplateCategory = new PerfTemplateCategory();
            perfTemplateCategory.setTemplateHeadId(perfTemplateHeaderId);
            List<PerfTemplateCategory> perfTemplateLineList = iPerfTemplateCategoryService.list(new QueryWrapper<>(perfTemplateCategory));
            perfTemplateDTO.setPerfTemplateCategoryList(perfTemplateLineList);
        }catch (Exception e){
            log.error("??????ID???????????????????????????????????????????????????",e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }

        /**??????????????????ID????????????????????????????????????????????????*/
        PerfTemplateDimWeight perfTemplateDimWeight = new PerfTemplateDimWeight();
        perfTemplateDimWeight.setTemplateHeadId(perfTemplateHeaderId);
        perfTemplateDimWeight.setDeleteFlag(Enable.N.toString());
        List<PerfTemplateDimWeightDTO> perfTemplateDimWeightList = iPerfTemplateDimWeightService.findPerTemplateDimWeightAndLine(perfTemplateDimWeight);
        perfTemplateDTO.setPerfTemplateDimWeightList(perfTemplateDimWeightList);
        return perfTemplateDTO;
    }

    @Override
    public List<PerfTemplateDTO> findPerTemplateHeadAndOrgCateGory(PerfTemplateHeaderQueryDTO pefTemplateHeader) throws BaseException {
        List<PerfTemplateDTO> pefTemplateList = new ArrayList<>();
        try{
            pefTemplateHeader.setDeleteFlag(Enable.N.toString());
//            pefTemplateList = getBaseMapper().findPerTemplateHeadAndOrgCateGory(pefTemplateHeader);
        }catch (Exception e){
            log.error("???????????????????????????????????????????????????????????????",e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }
        return pefTemplateList;
    }

    @Override
    public String enablePefTemplateHeader(Long pefTempateHeadId, String templateStatus) throws BaseException {
        Assert.notNull(pefTempateHeadId, "id????????????");
        Assert.notNull(templateStatus, "??????/??????????????????");
        PerfTemplateHeader pefTemplateHeader = getBaseMapper().selectById(pefTempateHeadId);
        if(null != pefTemplateHeader){
            pefTemplateHeader.setTemplateStatus(templateStatus);
            try {
                getBaseMapper().updateById(pefTemplateHeader);
            }catch (Exception e){
                log.error("??????????????????/??????????????????",e);
                throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
            }
        }
        return ResultCode.SUCCESS.getMessage();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPefTemplateHeader(Long pefTemplateHeadId) throws BaseException {
        Assert.notNull(pefTemplateHeadId, "id????????????");
        PerfTemplateHeader pefTemplateHeader = this.getById(pefTemplateHeadId);
        Optional.ofNullable(pefTemplateHeader).orElseThrow(() -> new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????")));
        // ???????????????????????????????????????????????????(??????????????????????????????)
        checkIsDelete(pefTemplateHeadId);
        List<PerfTemplateDimWeight> perfTemplateDimWeights = iPerfTemplateDimWeightService.list(Wrappers.lambdaQuery(PerfTemplateDimWeight.class)
                .eq(PerfTemplateDimWeight::getTemplateHeadId, pefTemplateHeadId));
        if (CollectionUtils.isNotEmpty(perfTemplateDimWeights)) {
            // ?????????????????????????????????
            perfTemplateDimWeights.forEach(perfTemplateDimWeight -> {
                List<PerfTemplateLine> perfTemplateLines = iPerfTemplateLineService.list(Wrappers.lambdaQuery(PerfTemplateLine.class)
                        .eq(PerfTemplateLine::getTemplateDimWeightId, perfTemplateDimWeight.getDimWeightId()));
                if (CollectionUtils.isNotEmpty(perfTemplateLines)) {
                    List<Long> templateLineIds = perfTemplateLines.stream().map(PerfTemplateLine::getTemplateLineId).distinct().collect(Collectors.toList());
                    iPerfTemplateLineService.removeByIds(templateLineIds);
                }
                // ??????????????????
                iPerfTemplateDimWeightService.removeById(perfTemplateDimWeight.getDimWeightId());
            });
        }
        // ???????????????????????????
        this.removeById(pefTemplateHeadId);
    }

    /**
     * Description ???????????????????????????????????????????????????(??????????????????????????????)
     * @Param
     * @return
     * @Author wuwl18@meicloud.com
     * @Date 2020.07.08
     * @throws
     **/
    private boolean checkIsDelete(Long templateHeadId){
        boolean isDetele = false;
        Assert.notNull(templateHeadId, "id????????????");
        PerfScoreItems queryScoreItems = new PerfScoreItems();
        queryScoreItems.setTemplateHeadId(templateHeadId);
        List<PerfScoreItems> scoreItemsList = iPerfScoreItemsService.list(new QueryWrapper<>(queryScoreItems));
        if(CollectionUtils.isNotEmpty(scoreItemsList) && null != scoreItemsList.get(0)){
            Assert.isTrue(false, ScoreItemsConst.IS_NOT_DELETE_TEMPALTE_HEADER);
        }
        isDetele = true;
        return isDetele;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrUpdatePerfTemplate(PerfTemplateDTO perfTemplateDTO) throws BaseException{
        Assert.notNull(perfTemplateDTO, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        Long templeateHeadId = null;
        PerfTemplateHeader perfTemplateHeader = perfTemplateDTO.getPerfTemplateHeader();
        if(null != perfTemplateHeader){
            //???????????????(??????yyyyMM0001)
            Long versionByLong = null;
            //????????????????????????
            String nickName = "";
            LoginAppUser user = AppUserUtil.getLoginAppUser();
            if(null != user){
                nickName = user.getNickname();
            }

            if(null == perfTemplateHeader.getTemplateHeadId()){ //??????
                String version = baseClient.seqGen(SequenceCodeConstant.SEQ_PER_TEMPLATE_VERSION);
                Assert.notNull(version, "?????????????????????");
                versionByLong = Long.parseLong(version);
                perfTemplateHeader.setDeleteFlag(Enable.N.toString());
                perfTemplateHeader.setVersion(versionByLong);
                //??????????????????????????????
                perfTemplateHeader.setTemplateStatus(PerfTemplateStatusEnum.DRAFT.getValue());
                perfTemplateHeader.setCreatedFullName(nickName);
                perfTemplateHeader.setLastUpdatedFullName(nickName);
            }else {
                versionByLong = perfTemplateHeader.getVersion();
                perfTemplateHeader.setLastUpdatedFullName(nickName);
            }
            /**??????????????????????????????*/
            templeateHeadId = (null != perfTemplateHeader.getTemplateHeadId() ? perfTemplateHeader.getTemplateHeadId() : IdGenrator.generate());
            boolean updateHeaderCount = false ; //???????????????/??????????????????????????????false
            try {
                perfTemplateHeader.setTemplateHeadId(templeateHeadId);
                updateHeaderCount = super.saveOrUpdate(perfTemplateHeader);
            }catch (Exception e){
                log.error("???????????????????????????????????????: "+e);
                throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
            }

            if(updateHeaderCount){
                /**??????/???????????????????????????????????????*/
                try {
                    List<PerfTemplateCategory> TemplateCategoryList = perfTemplateDTO.getPerfTemplateCategoryList();
                    if (CollectionUtils.isNotEmpty(TemplateCategoryList)) {
                        for (PerfTemplateCategory perfTemplateCategory : TemplateCategoryList) {
                            if (null != perfTemplateCategory) {
                                if(null == perfTemplateCategory.getTemplateCategoryId()){ //????????????
                                    perfTemplateCategory.setDeleteFlag(Enable.N.toString());
                                }
                                perfTemplateCategory.setVersion(versionByLong);
                                Long TemplateCategoryId = (null != perfTemplateCategory.getTemplateCategoryId() ?
                                        perfTemplateCategory.getTemplateCategoryId() : IdGenrator.generate());
                                perfTemplateCategory.setTemplateCategoryId(TemplateCategoryId);
                                perfTemplateCategory.setTemplateHeadId(templeateHeadId);
                            }
                        }
                        iPerfTemplateCategoryService.saveOrUpdateBatch(TemplateCategoryList);
                    }
                }catch (Exception e){
                    log.error("??????/????????????????????????????????????????????????: "+e);
                    throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                }

                /**??????/???????????????????????????????????????????????????????????????*/
                List<PerfTemplateDimWeightDTO> perfTemplateDimWeightDTOList = perfTemplateDTO.getPerfTemplateDimWeightList();
                if(CollectionUtils.isNotEmpty(perfTemplateDimWeightDTOList)){
                    for(PerfTemplateDimWeightDTO dimWeightDTO : perfTemplateDimWeightDTOList){
                        if(null != dimWeightDTO){

                            /**??????/??????????????????????????????*/
                            PerfTemplateDimWeight perfTemplateDimWeight = dimWeightDTO.getPerfTemplateDimWeight();
                            if(null == perfTemplateDimWeight.getDimWeightId()) { //??????
                                perfTemplateDimWeight.setDeleteFlag(Enable.N.toString());
                            }
                            perfTemplateDimWeight.setVersion(versionByLong);
                            boolean isUpdateDimWeight = (null != perfTemplateDimWeight.getDimWeightId() ? true : false); //???????????????????????????????????????true
                            Long dimWeightId = (null != perfTemplateDimWeight.getDimWeightId() ? perfTemplateDimWeight.getDimWeightId()
                                    : IdGenrator.generate());
                            perfTemplateDimWeight.setDimWeightId(dimWeightId);
                            perfTemplateDimWeight.setTemplateHeadId(templeateHeadId);
                            boolean isDimWeight = false;

                            try {
                                isDimWeight = iPerfTemplateDimWeightService.saveOrUpdate(perfTemplateDimWeight);
                            }catch (Exception e){
                                log.error("??????/???????????????????????????????????????: "+e);
                                throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                            }

                            if(isDimWeight){
                                /**??????/???????????????????????????????????????????????????????????????????????????????????????????????????*/
                                if(isUpdateDimWeight){
                                    PerfTemplateLine queryPerfTemplateLine = new PerfTemplateLine();
                                    queryPerfTemplateLine.setTemplateDimWeightId(dimWeightId);
                                    List<PerfTemplateLine> delPerfTemplateLineList = iPerfTemplateLineService.list(new QueryWrapper<>(queryPerfTemplateLine));
                                    List<Long> delPerfTemplateLineIds = new ArrayList<>();      //?????????????????????????????????Id??????
                                    List<Long> delPerfTemplateIndsLineIds = new ArrayList<>();  //????????????????????????????????????Id??????
                                    if(CollectionUtils.isNotEmpty(delPerfTemplateLineList)){
                                        for(PerfTemplateLine perfTemplateLine : delPerfTemplateLineList){
                                            if(null != perfTemplateLine && null != perfTemplateLine.getTemplateLineId()){
                                                delPerfTemplateLineIds.add(perfTemplateLine.getTemplateLineId());
                                                PerfTemplateIndsLine queryPerfTemplateIndsLine = new PerfTemplateIndsLine();
                                                queryPerfTemplateIndsLine.setTemplateLineId(perfTemplateLine.getTemplateLineId());
                                                List<PerfTemplateIndsLine> perfTemplateIndsLineList =
                                                        iPerfTemplateIndsLineService.list(new QueryWrapper<>(queryPerfTemplateIndsLine));
                                                for(PerfTemplateIndsLine perfTemplateIndsLine : perfTemplateIndsLineList){
                                                    if(null != perfTemplateIndsLine && null != perfTemplateIndsLine.getTemplateIndsLineId()){
                                                        delPerfTemplateIndsLineIds.add(perfTemplateIndsLine.getTemplateIndsLineId());
                                                    }
                                                }

                                            }
                                        }
                                        try {
                                            if(CollectionUtils.isNotEmpty(delPerfTemplateLineIds)) {
                                                iPerfTemplateLineService.removeByIds(delPerfTemplateLineIds);
                                            }
                                        }catch (Exception e){
                                            log.error("????????????????????????-??????????????????,???????????????????????????: "+e);
                                            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                        }
                                        try {
                                            if(CollectionUtils.isNotEmpty(delPerfTemplateIndsLineIds)) {
                                                iPerfTemplateIndsLineService.removeByIds(delPerfTemplateIndsLineIds);
                                            }
                                        }catch (Exception e){
                                            log.error("????????????????????????-??????????????????,??????????????????????????????: "+e);
                                            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                        }
                                    }
                                }

                                List<PerfTemplateLineDTO> perfTemplateLinesList = dimWeightDTO.getPerfTemplateLineList();
                                if(CollectionUtils.isNotEmpty(perfTemplateLinesList)){
                                    for(PerfTemplateLineDTO perfTemplateLineDTO : perfTemplateLinesList){
                                        if(null != perfTemplateLineDTO){
                                            PerfTemplateLine perfTemplateLine = perfTemplateLineDTO.getPerfTemplateLine();
                                            if(null != perfTemplateLine){
                                                Long templateLineId = (null != perfTemplateLine.getTemplateLineId()
                                                        ? perfTemplateLine.getTemplateLineId() : IdGenrator.generate());
                                                perfTemplateLine.setTemplateLineId(templateLineId);
                                                perfTemplateLine.setTemplateDimWeightId(dimWeightId);
                                                perfTemplateLine.setDeleteFlag(Enable.N.toString());
                                                perfTemplateLine.setVersion(versionByLong);
                                                boolean updateLineCount = false;
                                                try{
                                                    updateLineCount = iPerfTemplateLineService.saveOrUpdate(perfTemplateLine);
                                                }catch (Exception e){
                                                    log.error("??????/?????????????????????????????????: "+e);
                                                    throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                                }

                                                try {
                                                    if (updateLineCount && CollectionUtils.isNotEmpty(perfTemplateLineDTO.getPerfTemplateIndsLineList())) {
                                                        List<PerfTemplateIndsLine> perfTemplateIndsLineList = perfTemplateLineDTO.getPerfTemplateIndsLineList();
                                                        for (PerfTemplateIndsLine perfTemplateIndsLine : perfTemplateIndsLineList) {
                                                            if (null != perfTemplateIndsLine) {
                                                                Long teplateIndsLineId = (null != perfTemplateIndsLine.getTemplateIndsLineId() ?
                                                                        perfTemplateIndsLine.getTemplateIndsLineId() : IdGenrator.generate());
                                                                perfTemplateIndsLine.setTemplateIndsLineId(teplateIndsLineId);
                                                                perfTemplateIndsLine.setTemplateLineId(templateLineId);
                                                                perfTemplateIndsLine.setDeleteFlag(Enable.N.toString());
                                                                perfTemplateIndsLine.setVersion(versionByLong);
                                                            }
                                                        }
                                                        iPerfTemplateIndsLineService.saveOrUpdateBatch(perfTemplateIndsLineList);
                                                    }
                                                }catch (Exception e){
                                                    log.error("??????/????????????????????????????????????: "+e);
                                                    throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return String.valueOf(templeateHeadId);
    }

    /**
     * ?????????????????????????????????
     * ??????????????????????????????????????????????????????????????????
     * ???????????? 1.??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param perfTemplateHeader
     * @return
     */
    @Override
    public List<CompanyInfo> listCompanysByPerfTemplateHeader(PerfTemplateHeader perfTemplateHeader) {
        PerfTemplateHeader head = this.getById(perfTemplateHeader.getTemplateHeadId());
        List<PerfTemplateCategory> templateCategories = iPerfTemplateCategoryService.list(Wrappers.lambdaQuery(PerfTemplateCategory.class)
                .eq(PerfTemplateCategory::getTemplateHeadId, head.getTemplateHeadId())
        );
        List<CompanyInfo> companyInfos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(templateCategories)) {
            List<Long> categoryIds = templateCategories.stream().filter(x -> null != x.getCategoryId()).map(PerfTemplateCategory::getCategoryId).collect(Collectors.toList());
            companyInfos = supplierClient.listCompanyInfosByCategoryIds(categoryIds);
        }
        return companyInfos;
    }

    /**
     * ???????????????id???????????????????????????
     * @param templateHeaderId
     * @return
     */
    @Override
    public List<PerfTemplateLine> listTemplateLinesByTemplateHeaderId(Long templateHeaderId) {
        // ???????????????id???????????????????????????
        List<PerfTemplateDimWeight> templateDimWeights = iPerfTemplateDimWeightService.list(Wrappers.lambdaQuery(PerfTemplateDimWeight.class)
                .eq(PerfTemplateDimWeight::getTemplateHeadId, templateHeaderId));
        if (CollectionUtils.isNotEmpty(templateDimWeights)) {
            List<Long> templateDimWeightIds = templateDimWeights.stream().map(PerfTemplateDimWeight::getDimWeightId).distinct().collect(Collectors.toList());
            return CollectionUtils.isEmpty(templateDimWeightIds) ? Collections.emptyList() :
                    iPerfTemplateLineService.list(Wrappers.lambdaQuery(PerfTemplateLine.class).in(PerfTemplateLine::getTemplateDimWeightId, templateDimWeightIds));
        }
        return Collections.emptyList();
    }

    /**
     * ??????????????????
     * @param queryDTO
     * @return
     */
    @Override
    public List<PerfTemplateHeader> listPefTemplateHeaderPage(PerfTemplateHeaderQueryDTO queryDTO) {
        // ???????????????????????????
        QueryWrapper<PerfTemplateHeader> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(queryDTO.getTemplateName()), "TEMPLATE_NAME", queryDTO.getTemplateName());
        wrapper.eq(StringUtils.isNotEmpty(queryDTO.getTemplateStatus()), "TEMPLATE_STATUS", queryDTO.getTemplateStatus());
        wrapper.eq(Objects.nonNull(queryDTO.getOrganizationId()), "ORGANIZATION_ID", queryDTO.getOrganizationId());
        wrapper.eq(Objects.nonNull(queryDTO.getVersion()), "VERSION", queryDTO.getVersion());
        // ???????????????????????????
        QueryWrapper<PerfTemplateCategory> lineWrapper = new QueryWrapper<>();
        if (Objects.nonNull(queryDTO.getCategoryId())) {
            lineWrapper.eq("CATEGORY_ID", queryDTO.getCategoryId());
        }
        List<PerfTemplateCategory> perfTemplateCategories = iPerfTemplateCategoryService.list(Wrappers.lambdaQuery(PerfTemplateCategory.class)
                .eq(PerfTemplateCategory::getCategoryId, queryDTO.getCategoryId()));
        if (CollectionUtils.isNotEmpty(perfTemplateCategories)) {
            List<Long> headIds = perfTemplateCategories.stream().map(PerfTemplateCategory::getTemplateHeadId).distinct().collect(Collectors.toList());
            wrapper.in("TEMPLATE_HEAD_ID", headIds);
        }
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return this.list(wrapper);
    }

    /**
     * ??????????????????
     * @param perfTemplateHeaderId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyPerfTemplateHeader(Long perfTemplateHeaderId) {
        PerfTemplateHeader perfTemplateHeader = this.getById(perfTemplateHeaderId);
        // ??????m?????????id
        Long newHeaderId = IdGenrator.generate();
        //???????????????(??????yyyyMM0001)
        Long versionByLong = null;
        String version = baseClient.seqGen(SequenceCodeConstant.SEQ_PER_TEMPLATE_VERSION);
        Assert.notNull(version, "?????????????????????");
        versionByLong = Long.parseLong(version);
        perfTemplateHeader.setTemplateHeadId(newHeaderId)
                .setTemplateStatus(PerfTemplateStatusEnum.DRAFT.getValue())
                .setVersion(versionByLong);
        this.save(perfTemplateHeader);

        // ?????????????????????
        List<PerfTemplateCategory> perfTemplateCategories = iPerfTemplateCategoryService.list(Wrappers.lambdaQuery(PerfTemplateCategory.class)
                .eq(PerfTemplateCategory::getTemplateHeadId, perfTemplateHeaderId));
        if (CollectionUtils.isNotEmpty(perfTemplateCategories)) {
            perfTemplateCategories.forEach(perfTemplateCategory -> {
                perfTemplateCategory.setTemplateCategoryId(IdGenrator.generate())
                        .setTemplateHeadId(newHeaderId);
            });
            // ??????????????????
            iPerfTemplateCategoryService.saveBatch(perfTemplateCategories);
        }

        // ????????????????????????
        List<PerfTemplateDimWeight> oldPerfTemplateDimWeights = iPerfTemplateDimWeightService.list(Wrappers.lambdaQuery(PerfTemplateDimWeight.class)
                .eq(PerfTemplateDimWeight::getTemplateHeadId, perfTemplateHeaderId));
        if (CollectionUtils.isNotEmpty(oldPerfTemplateDimWeights)) {
            List<PerfTemplateDimWeight> newPerfTemplateDimWeights = new ArrayList<>();
            oldPerfTemplateDimWeights.forEach(oldPerfTemplateDimWeight -> {
                PerfTemplateDimWeight newPerfTemplateDimWeight = new PerfTemplateDimWeight();
                BeanUtils.copyProperties(oldPerfTemplateDimWeight, newPerfTemplateDimWeight);
                long newDimWeightId = IdGenrator.generate();
                newPerfTemplateDimWeight.setDimWeightId(newDimWeightId)
                        .setTemplateHeadId(newHeaderId);

                // ?????? ?????????????????????
                List<PerfTemplateLine> oldPerfTemplateLines = iPerfTemplateLineService.list(Wrappers.lambdaQuery(PerfTemplateLine.class)
                        .eq(PerfTemplateLine::getTemplateDimWeightId, oldPerfTemplateDimWeight.getDimWeightId()));
                if (CollectionUtils.isNotEmpty(oldPerfTemplateLines)) {
                    List<PerfTemplateLine> newPerfTemplateLines = new ArrayList<>();
                    oldPerfTemplateLines.forEach(oldPerfTemplateLine -> {
                        PerfTemplateLine newPerfTemplateLine = new PerfTemplateLine();
                        BeanUtils.copyProperties(oldPerfTemplateLine, newPerfTemplateLine);
                        newPerfTemplateLine.setTemplateLineId(IdGenrator.generate())
                                .setTemplateDimWeightId(newDimWeightId);
                        newPerfTemplateLines.add(newPerfTemplateLine);
                    });
                    iPerfTemplateLineService.saveBatch(newPerfTemplateLines);
                }
                // ?????? ?????????????????????

                iPerfTemplateDimWeightService.save(newPerfTemplateDimWeight);
            });
        }
    }

    /**
     * ??????????????????????????????????????????
     * @param perfTemplateHeader
     */
    @Override
    public void checkTemplateHeader(PerfTemplateHeader perfTemplateHeader) {
        // ?????????id?????????????????????????????????
        if (Objects.nonNull(perfTemplateHeader.getTemplateHeadId())) {
            List<PerfTemplateHeader> perfTemplateHeaders = this.list(Wrappers.lambdaQuery(PerfTemplateHeader.class)
                    .ne(PerfTemplateHeader::getTemplateHeadId, perfTemplateHeader.getTemplateHeadId())
                    .eq(PerfTemplateHeader::getTemplateName, perfTemplateHeader.getTemplateName())
            );
            if (CollectionUtils.isNotEmpty(perfTemplateHeaders)) {
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????"));
            }
        }
        // ?????????id??????
        else {
            List<PerfTemplateHeader> templateHeaders = this.list(Wrappers.lambdaQuery(PerfTemplateHeader.class)
                    .eq(PerfTemplateHeader::getTemplateName, perfTemplateHeader.getTemplateName())
            );
            if (CollectionUtils.isNotEmpty(templateHeaders)) {
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????"));
            }
        }
    }

}
