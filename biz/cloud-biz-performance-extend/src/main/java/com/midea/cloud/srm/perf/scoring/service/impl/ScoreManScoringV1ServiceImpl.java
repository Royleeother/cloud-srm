package com.midea.cloud.srm.perf.scoring.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.enums.ImportStatus;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.perf.scoreproject.ScoreItemsProjectStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.handler.TitleHandler;
import com.midea.cloud.common.listener.AnalysisEventListenerImpl;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.feign.supplier.SupplierClient;
import com.midea.cloud.srm.model.common.ExportExcelParam;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.perf.inditors.entity.IndicatorsLine;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemManSupInd;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItems;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemsMan;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemsSup;
import com.midea.cloud.srm.model.perf.scoring.ScoreManScoringV1;
import com.midea.cloud.srm.model.perf.scoring.dto.ScoreManScoringV1Import;
import com.midea.cloud.srm.model.perf.scoring.dto.ScoreManScoringV1SubmitConfirmDTO;
import com.midea.cloud.srm.model.perf.template.dto.PerfTemplateDTO;
import com.midea.cloud.srm.model.perf.template.dto.PerfTemplateDimWeightDTO;
import com.midea.cloud.srm.model.perf.template.entity.PerfTemplateCategory;
import com.midea.cloud.srm.model.perf.template.entity.PerfTemplateDimWeight;
import com.midea.cloud.srm.model.perf.template.entity.PerfTemplateIndsLine;
import com.midea.cloud.srm.model.perf.template.entity.PerfTemplateLine;
import com.midea.cloud.srm.model.rbac.role.entity.Role;
import com.midea.cloud.srm.model.rbac.role.entity.RoleFuncSet;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.perf.common.IndicatorsConst;
import com.midea.cloud.srm.perf.common.PerfLevelConst;
import com.midea.cloud.srm.perf.common.ScoreItemsConst;
import com.midea.cloud.srm.perf.common.ScoreManScoringConst;
import com.midea.cloud.srm.perf.indicators.mapper.IndicatorsLineMapper;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemManSupIndService;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsManService;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsService;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsSupService;
import com.midea.cloud.srm.perf.scoring.constants.QuoteMode;
import com.midea.cloud.srm.perf.scoring.mapper.ScoreManScoringV1Mapper;
import com.midea.cloud.srm.perf.scoring.service.IScoreManScoringV1Service;
import com.midea.cloud.srm.perf.scoring.utils.ScoreManScoringV1ExportUtils;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateCategoryService;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateDimWeightService;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateHeaderService;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateLineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <pre>
 *  ???????????????????????? ???????????????
 * </pre>
 *
 * @author xiexh12@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2021-01-21 17:26:15
 *  ????????????:
 * </pre>
 */
@Service
@Slf4j
public class ScoreManScoringV1ServiceImpl extends ServiceImpl<ScoreManScoringV1Mapper, ScoreManScoringV1> implements IScoreManScoringV1Service {

    @Autowired
    private IPerfScoreItemsService iPerfScoreItemsService;

    @Autowired
    private IPerfTemplateHeaderService iPerfTemplateHeaderService;

    @Autowired
    private IPerfScoreItemsSupService iPerfScoreItemsSupService;

    @Autowired
    private IPerfScoreItemsManService iPerfScoreItemsManService;

    @Autowired
    private IPerfTemplateLineService iPerfTemplateLineService;

    @Autowired
    private IPerfTemplateCategoryService iPerfTemplateCategoryService;

    @Autowired
    private IPerfScoreItemManSupIndService iPerfScoreItemManSupIndService;

    @Autowired
    private IPerfTemplateDimWeightService iPerfTemplateDimWeightService;

    @Autowired
    private FileCenterClient fileCenterClient;

    @Autowired
    private BaseClient baseClient;

    @Autowired
    private SupplierClient supplierClient;

    @Resource
    private RbacClient rbacClient;

    @Resource
    private IndicatorsLineMapper indicatorsLineMapper;


    public static final Map<String, String> INDICATOR_DIMENSION_MAP;

    public static final Map<String, String> INDICATOR_DIMENSION_MAP_1;

    static {
        INDICATOR_DIMENSION_MAP = new HashMap<>();
        INDICATOR_DIMENSION_MAP.put("QUALITY", "??????");
        INDICATOR_DIMENSION_MAP.put("SERVICE", "??????");
        INDICATOR_DIMENSION_MAP.put("DELIVER", "??????");
        INDICATOR_DIMENSION_MAP.put("TECHNOLOGY", "??????");
        INDICATOR_DIMENSION_MAP.put("COST", "??????");
        INDICATOR_DIMENSION_MAP.put("COMPREHENSIVE", "??????");

        INDICATOR_DIMENSION_MAP_1 = new HashMap<>();
        INDICATOR_DIMENSION_MAP_1.put("??????", "QUALITY");
        INDICATOR_DIMENSION_MAP_1.put("??????", "SERVICE");
        INDICATOR_DIMENSION_MAP_1.put("??????", "DELIVER");
        INDICATOR_DIMENSION_MAP_1.put("??????", "TECHNOLOGY");
        INDICATOR_DIMENSION_MAP_1.put("??????", "COST");
        INDICATOR_DIMENSION_MAP_1.put("??????", "COMPREHENSIVE");

    }


    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param scoreItemsId
     * @return
     * @throws BaseException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveScoreManScoringByScoreItemsId(Long scoreItemsId) throws BaseException {
        String result = ResultCode.OPERATION_FAILED.getMessage();
        // ??????Id??????????????????????????????
        PerfScoreItems scoreItems = iPerfScoreItemsService.getById(scoreItemsId);
        Assert.notNull(scoreItems, ScoreItemsConst.PER_SCORE_ITEMS_NOT_NULL);

        // ????????????????????????ID?????????????????????????????????
        List<PerfScoreItemsSup> scoreItemsSupList = iPerfScoreItemsSupService.list(Wrappers.lambdaQuery(PerfScoreItemsSup.class)
                .eq(PerfScoreItemsSup::getScoreItemsId, scoreItemsId));
        Assert.isTrue(CollectionUtils.isNotEmpty(scoreItemsSupList), ScoreItemsConst.PER_SCORE_ITEMS_SUP_NOT_NULL);

        // ????????????????????????????????????
        List<ScoreManScoringV1> scoreManScoringV1List = new ArrayList<>();
        // ???????????????ID
        Long templateHeadId = scoreItems.getTemplateHeadId();
        // ??????????????????
        String templateName = scoreItems.getTemplateName();
        String projectName = scoreItems.getProjectName();
        String evaluationPeriod = scoreItems.getEvaluationPeriod();
        LocalDate perStartMonth = scoreItems.getPerStartMonth();
        LocalDate perEndMonth = scoreItems.getPerEndMonth();
        Long organizationId = scoreItems.getOrganizationId();
        String organizationName = scoreItems.getOrganizationName();
        String createdByUsername = scoreItems.getCreatedBy();
        String createdNickName = scoreItems.getCreatedFullName();

        // ?????????????????????ID??????????????????
        PerfTemplateDTO templateDTO = iPerfTemplateHeaderService.findPerTemplateByTemplateHeadId(templateHeadId);
        Assert.isTrue(Objects.nonNull(templateDTO), ScoreItemsConst.PER_TEMPLATE_NOT_NULL);
        PerfTemplateCategory category = new PerfTemplateCategory();
        if (CollectionUtils.isNotEmpty(scoreItemsSupList)) {
            // ????????????-??????????????????
            List<PerfTemplateCategory> categoryList = templateDTO.getPerfTemplateCategoryList();
            Assert.isTrue(CollectionUtils.isNotEmpty(categoryList), ScoreItemsConst.PER_TEMPLATE_CATEGORY_NOT_NULL);
            category = categoryList.get(0);
            // ??????????????????????????????
            List<PerfTemplateDimWeightDTO> dimWeightDTOList = templateDTO.getPerfTemplateDimWeightList();
            Assert.isTrue(CollectionUtils.isNotEmpty(dimWeightDTOList), ScoreItemsConst.PER_TEMPLATE_DIM_WEIGHT_NOT_NULL);
        }
        Long categoryId = category.getCategoryId();
        String categoryCode = category.getCategoryCode();
        String categoryName = category.getCategoryName();
        String categoryFullName = category.getCategoryFullName();

        // ??????????????????????????????????????????
        List<PerfScoreItemManSupInd> perfScoreItemManSupInds = iPerfScoreItemManSupIndService.list(Wrappers.lambdaQuery(PerfScoreItemManSupInd.class)
                .eq(PerfScoreItemManSupInd::getScoreItemsId, scoreItemsId));

        if (CollectionUtils.isNotEmpty(perfScoreItemManSupInds)) {
            for (PerfScoreItemManSupInd manSupInd : perfScoreItemManSupInds) {
                ScoreManScoringV1 manScoringV1 = new ScoreManScoringV1();
                manScoringV1.setScoreItemsId(scoreItemsId)
                        .setStatus(ScoreItemsProjectStatusEnum.SCORE_DRAFT.getValue())
                        .setTemplateHeadId(templateHeadId)
                        .setTemplateName(templateName)
                        .setProjectName(projectName)
                        .setEvaluationPeriod(evaluationPeriod)
                        .setPerStartMonth(perStartMonth)
                        .setPerEndMonth(perEndMonth)
                        .setOrganizationId(organizationId)
                        .setOrganizationName(organizationName)
                        .setScoreItemsCreatedUsername(createdByUsername)
                        .setScoreItemsCreatedNickname(createdNickName)
                        .setCategoryId(categoryId)
                        .setCategoryCode(categoryCode)
                        .setCategoryName(categoryName)
                        .setCategoryFullName(categoryFullName);
                Long scoreManScoringId = IdGenrator.generate();
                manScoringV1.setScoreManScoringId(scoreManScoringId);
                manScoringV1.setCompanyId(manSupInd.getCompanyId())
                        .setCompanyCode(manSupInd.getCompanyCode())
                        .setCompanyName(manSupInd.getCompanyName());
                // ?????????????????????
                Long templateDimWeightId = manSupInd.getTemplateDimWeightId();
                PerfTemplateDimWeight perfTemplateDimWeight = iPerfTemplateDimWeightService.getById(templateDimWeightId);
                manScoringV1.setDimWeightId(manSupInd.getTemplateDimWeightId())
                        .setIndicatorType(perfTemplateDimWeight.getIndicatorType())
                        .setIndicatorDimensionType(perfTemplateDimWeight.getIndicatorDimensionType())
                        .setIndicatorDimensionWeight(perfTemplateDimWeight.getIndicatorDimensionWeight());
                // ???????????????
                Long templateLineId = manSupInd.getTemplateLineId();
                PerfTemplateLine perfTemplateLine = iPerfTemplateLineService.getById(templateLineId);
                manScoringV1.setEvaluation(perfTemplateLine.getEvaluation())
                        .setTemplateLineId(templateLineId)
                        .setIndicatorName(perfTemplateLine.getIndicatorName())
                        .setIndicatorLineType(perfTemplateLine.getIndicatorLineType())
                        .setQuoteMode(perfTemplateLine.getQuoteMode())
                        .setDimensionWeight(new BigDecimal(perfTemplateLine.getDimensionWeight()))
                        .setIfScored(YesOrNo.NO.getValue());
                Long scoreItemsManId = manSupInd.getScoreItemsManId();
                PerfScoreItemsMan scoreItemsMan = iPerfScoreItemsManService.getById(scoreItemsManId);
                Assert.notNull(scoreItemsMan, "?????????????????????");
                manScoringV1.setScoreUserName(scoreItemsMan.getScoreUserName())
                        .setScoreNickName(scoreItemsMan.getScoreNickName());
                // ???????????????????????????????????????????????????
                this.checkEditScoreManScoring(manScoringV1);
                scoreManScoringV1List.add(manScoringV1);
            }
        }

        if (CollectionUtils.isNotEmpty(scoreManScoringV1List)) {
            try {
                boolean isSave = super.saveBatch(scoreManScoringV1List);
                if (isSave) {
                    result = ResultCode.SUCCESS.getMessage();
                }
            } catch (Exception e) {
                log.error("??????????????????????????????????????????", e);
                throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
            }
        }

        PerfScoreItemsMan queryScoreItemsMan = new PerfScoreItemsMan();
        queryScoreItemsMan.setScoreItemsId(scoreItemsId);
        List<PerfScoreItemsMan> scoreItemsManList = iPerfScoreItemsManService.list(new QueryWrapper<>(queryScoreItemsMan));
        int scorePeopleCount = scoreItemsManList.size(); //??????????????????????????????
        if (0 < scorePeopleCount) {
            try {
                UpdateWrapper<PerfScoreItems> updateWrapper = new UpdateWrapper<>();
                updateWrapper.set("SCORE_PEOPLE_COUNT",Long.parseLong(String.valueOf(scorePeopleCount)))
                        .eq("SCORE_ITEMS_ID", scoreItems.getScoreItemsId());
                PerfScoreItems updateScoreItems = new PerfScoreItems();
                updateScoreItems.setScoreItemsId(scoreItemsId);
                updateScoreItems.setScorePeopleCount(Long.parseLong(String.valueOf(scorePeopleCount)));
                iPerfScoreItemsService.update(updateWrapper);
            } catch (Exception e) {
                log.error("??????????????????????????????????????????-???????????????????????????????????????????????????", e);
                throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
            }
        }
        return result;
    }

    /**
     * ???????????????????????????
     *
     * @param scoreManScoringV1List
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveScoreManScoring(List<ScoreManScoringV1> scoreManScoringV1List) {
        String result = ResultCode.OPERATION_FAILED.getMessage();
        scoreManScoringV1List.forEach(scoreManScoringV1 -> {
            // ??????????????????????????????????????????????????????????????????
            if (StringUtils.isEmpty(scoreManScoringV1.getComments())) {
                String projectName = scoreManScoringV1.getProjectName();
                String companyName = scoreManScoringV1.getCompanyName();
                String indicatorDimensionType = scoreManScoringV1.getIndicatorDimensionType();
                String indicatorDimension = INDICATOR_DIMENSION_MAP.get(indicatorDimensionType);
                String indicatorName = scoreManScoringV1.getIndicatorName();
                String resultMsg = LocaleHandler.getLocaleMsg("???????????????????????????????????????[x],???????????????[x],????????????[x],????????????[x]",
                        projectName, companyName, indicatorDimension, indicatorName);
                throw new BaseException(resultMsg);
            }
            scoreManScoringV1.setIfScored(YesOrNo.YES.getValue());
        });
        try {
            boolean isUpdate = super.updateBatchById(scoreManScoringV1List);
            if (isUpdate) {
                result = ResultCode.SUCCESS.getMessage();
            }
        } catch (Exception e) {
            log.error("?????????????????????????????????????????????", e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }
        return result;
    }

    /**
     * ????????????
     * @param scoreManScoringV1List
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abstention(List<ScoreManScoringV1> scoreManScoringV1List) {
        // ???????????????????????????
        scoreManScoringV1List.forEach(scoreManScoringV1 -> {
            if (Objects.nonNull(scoreManScoringV1.getPefScore())) {
                StringBuffer sb = new StringBuffer();
                String indicatorDimensionTypeName = INDICATOR_DIMENSION_MAP.get(scoreManScoringV1.getIndicatorDimensionType());
                sb.append("???????????????[").append(scoreManScoringV1.getProjectName()).append("]???????????????[")
                        .append(scoreManScoringV1.getCompanyName()).append("]??????????????????[")
                        .append(indicatorDimensionTypeName).append("]??????????????????[")
                        .append(scoreManScoringV1.getIndicatorName()).append("]??????????????????????????????????????????????????????????????????");
                throw new BaseException(LocaleHandler.getLocaleMsg(sb.toString()));
            }
        });
        scoreManScoringV1List.forEach(scoreManScoringV1 -> {
            scoreManScoringV1.setIfScored(YesOrNo.YES.getValue());
        });
        this.updateBatchById(scoreManScoringV1List);
    }

    /**
     * ????????????
     *
     * @param scoreManScoringV1
     * @return
     */
    @Override
    public List<ScoreManScoringV1> listScoreManScoringPage(ScoreManScoringV1 scoreManScoringV1) {
        // ??????????????????????????????????????????
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        String userName = "";
        if (null != user) {
            // ?????????,??????????????????,??????????????????????????????,?????????????????????
//            Assert.isTrue("BUYER".equals(user.getUserType()), ScoreManScoringConst.IS_BUYER_SCORE);
            userName = user.getUsername();
        }
        Assert.notNull(userName, ScoreManScoringConst.USER_NAME_NOT_NULL);

        List<ScoreManScoringV1> scoreManScoringList = new ArrayList<>();
        if ("BUYER".equals(user.getUserType())) {
            scoreManScoringList = listScoreManScoringV1ByBuyer(scoreManScoringV1, userName);
        } else if ("VENDOR".equals(user.getUserType())) {
            scoreManScoringV1.setCompanyId(user.getCompanyId());
            scoreManScoringV1.setIfScored(YesOrNo.YES.getValue());
            scoreManScoringList = listScoreManScoringV1ByVendor(scoreManScoringV1);
        }

        // ?????????????????????ID??????????????????????????????????????????????????????
        List<PerfTemplateLine> templateLineList = iPerfTemplateLineService.list();

        if (CollectionUtils.isNotEmpty(scoreManScoringList) && CollectionUtils.isNotEmpty(templateLineList)) {
            for (ScoreManScoringV1 manScoringV1 : scoreManScoringList) {
                if (null != manScoringV1 && null != manScoringV1.getTemplateLineId()) {
                    Long temlateLineId = manScoringV1.getTemplateLineId();
                    for (PerfTemplateLine templateLine : templateLineList) {
                        if (null != templateLine && (temlateLineId.longValue() == templateLine.getTemplateLineId().longValue())) { //????????????ID??????
                            List<PerfTemplateIndsLine> templateIndsLineList = (List<PerfTemplateIndsLine>) ObjectUtil.deepCopy(templateLine.getPerfTemplateIndsLineList());
                            manScoringV1.setTemplateIndsLineList(templateIndsLineList);
                            break;
                        }
                    }
                    PerfTemplateLine templateLine = iPerfTemplateLineService.getById(temlateLineId);
                    manScoringV1.setIndicatorLogic(templateLine.getIndicatorLogic());
                    // ??????????????????????????????
                    Long templateHeadId = manScoringV1.getTemplateHeadId();
                    List<PerfTemplateCategory> perfTemplateCategoryList = iPerfTemplateCategoryService.list(Wrappers.lambdaQuery(PerfTemplateCategory.class)
                            .eq(PerfTemplateCategory::getTemplateHeadId, templateHeadId));
                    String categoryNames = "";
                    if (CollectionUtils.isNotEmpty(perfTemplateCategoryList)) {
                        List<String> categoryNameList = perfTemplateCategoryList.stream().filter(e -> Objects.nonNull(e.getCategoryId()))
                                .map(PerfTemplateCategory::getCategoryName).collect(Collectors.toList());
                        categoryNames = StringUtils.join(categoryNameList, ";");
                    }
                    manScoringV1.setCategoryNames(categoryNames);
                }
            }
        }

        if(CollectionUtils.isNotEmpty(scoreManScoringList)){
            scoreManScoringList.forEach(scoreManScoringV11 -> {
                List<IndicatorsLine> indicatorsLines = indicatorsLineMapper.queruIndicatorsLine(scoreManScoringV11.getScoreManScoringId());
                scoreManScoringV11.setIndicatorsLines(indicatorsLines);
            });
        }

        return scoreManScoringList;
    }

    /**
     * ???????????????
     * @param scoreManScoringV1
     * @param userName
     * @return
     */
    public List<ScoreManScoringV1> listScoreManScoringV1ByBuyer(ScoreManScoringV1 scoreManScoringV1, String userName) {
        QueryWrapper<ScoreManScoringV1> wrapper = new QueryWrapper<ScoreManScoringV1>();
        if (null != scoreManScoringV1) {
            if (StringUtils.isNotEmpty(scoreManScoringV1.getProjectName())) {
                wrapper.like("PROJECT_NAME", scoreManScoringV1.getProjectName());
            }
            if (Objects.nonNull(scoreManScoringV1.getCompanyId())) {
                wrapper.eq("COMPANY_ID", scoreManScoringV1.getCompanyId());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getCompanyName())) {
                wrapper.like("COMPANY_NAME", scoreManScoringV1.getCompanyName());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getIndicatorName())) {
                wrapper.like("INDICATOR_NAME", scoreManScoringV1.getIndicatorName());
            }
            if (null != scoreManScoringV1.getOrganizationId()) {
                wrapper.eq("ORGANIZATION_ID", scoreManScoringV1.getOrganizationId());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getOrganizationName())) {
                wrapper.eq("ORGANIZATION_NAME", scoreManScoringV1.getOrganizationName());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getIndicatorDimensionType())) {
                wrapper.eq("INDICATOR_DIMENSION_TYPE", scoreManScoringV1.getIndicatorDimensionType());
            }
            if (null != scoreManScoringV1.getCategoryId()) {
                wrapper.eq("CATEGORY_ID", scoreManScoringV1.getCategoryId());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getCategoryName())) {
                wrapper.eq("CATEGORY_NAME", scoreManScoringV1.getCategoryName());
            }
            // ?????????????????????
            if (StringUtils.isNotEmpty(scoreManScoringV1.getIfEndScored())) {
                wrapper.eq("IF_END_SCORED", scoreManScoringV1.getIfEndScored());
            }
            // ???????????????
            // ???
            if (Objects.equals(YesOrNo.NO.getValue(), scoreManScoringV1.getIfScored())) {
                wrapper.eq("IF_SCORED", YesOrNo.NO.getValue());
            }
            // ???
            if (Objects.equals(YesOrNo.YES.getValue(), scoreManScoringV1.getIfScored())) {
                wrapper.eq("IF_SCORED", YesOrNo.YES.getValue());
            }
            // ??????????????????????????????
            // ??? ????????????????????????
            if (Objects.equals(YesOrNo.YES.getValue(), scoreManScoringV1.getIfQueryByScoreItemsCreatedBy())) {
                wrapper.eq("SCORE_ITEMS_CREATED_USERNAME", userName);
            }
            // ??? ??????????????????????????? ??????????????????????????????????????????
            if (Objects.equals(YesOrNo.NO.getValue(), scoreManScoringV1.getIfQueryByScoreItemsCreatedBy()) || (!isFunSetAll(AppUserUtil.getLoginAppUser(),"graderRating") && StringUtils.isEmpty(scoreManScoringV1.getIfQueryByScoreItemsCreatedBy()))) {
                /**???????????????????????????(???????????????????????????????????????????????????)*/
                wrapper.eq("SCORE_USER_NAME", userName);
            }
            //12449 ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        }

        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return getBaseMapper().selectList(wrapper);
    }

    public boolean isFunSetAll(LoginAppUser user, String functionCode) {
        if(Objects.isNull(user) || StringUtils.isEmpty(functionCode)){
           return false;
        }
        List<Role> roleList = user.getRolePermissions();
        if(CollectionUtils.isEmpty(roleList)){
            return false;
        }
        List<String> roleCodes = roleList.stream().map(x->x.getRoleCode()).collect(Collectors.toList());
        //????????????Code????????????-??????????????????
        List<RoleFuncSet> roleFuncSetList = rbacClient.findRoleFuncSetMoreRole(functionCode,roleCodes);
        return roleFuncSetList.stream().anyMatch(x -> Objects.equals("ALL", x.getRoleFuncSetType()));

    }

            /**
             * ???????????????
             * @param scoreManScoringV1
             * @return
             */
    public List<ScoreManScoringV1> listScoreManScoringV1ByVendor(ScoreManScoringV1 scoreManScoringV1) {
        QueryWrapper<ScoreManScoringV1> wrapper = new QueryWrapper<ScoreManScoringV1>();
        if (null != scoreManScoringV1) {
            if (StringUtils.isNotEmpty(scoreManScoringV1.getProjectName())) {
                wrapper.like("PROJECT_NAME", scoreManScoringV1.getProjectName());
            }
            if (Objects.nonNull(scoreManScoringV1.getCompanyId())) {
                wrapper.eq("COMPANY_ID", scoreManScoringV1.getCompanyId());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getCompanyName())) {
                wrapper.like("COMPANY_NAME", scoreManScoringV1.getCompanyName());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getIndicatorName())) {
                wrapper.like("INDICATOR_NAME", scoreManScoringV1.getIndicatorName());
            }
            if (null != scoreManScoringV1.getOrganizationId()) {
                wrapper.eq("ORGANIZATION_ID", scoreManScoringV1.getOrganizationId());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getOrganizationName())) {
                wrapper.eq("ORGANIZATION_NAME", scoreManScoringV1.getOrganizationName());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getIndicatorDimensionType())) {
                wrapper.eq("INDICATOR_DIMENSION_TYPE", scoreManScoringV1.getIndicatorDimensionType());
            }
            if (null != scoreManScoringV1.getCategoryId()) {
                wrapper.eq("CATEGORY_ID", scoreManScoringV1.getCategoryId());
            }
            if (StringUtils.isNotEmpty(scoreManScoringV1.getCategoryName())) {
                wrapper.eq("CATEGORY_NAME", scoreManScoringV1.getCategoryName());
            }
            // ?????????????????????
            if (StringUtils.isNotEmpty(scoreManScoringV1.getIfEndScored())) {
                wrapper.eq("IF_END_SCORED", scoreManScoringV1.getIfEndScored());
            }
            // ???????????????
            // ???
            if (Objects.equals(YesOrNo.NO.getValue(), scoreManScoringV1.getIfScored())) {
                wrapper.eq("IF_SCORED", YesOrNo.NO.getValue());
            }
            // ???
            if (Objects.equals(YesOrNo.YES.getValue(), scoreManScoringV1.getIfScored())) {
                wrapper.eq("IF_SCORED", YesOrNo.YES.getValue());
            }
        }

        wrapper.orderByDesc("LAST_UPDATE_DATE");
        return getBaseMapper().selectList(wrapper);
    }

    /**
     * Description ???????????????????????????????????????????????????
     *
     * @return
     * @throws Assert
     * @Param
     * @Author wuwl18@meicloud.com
     * @Date 2020.06.11
     **/
    public void checkEditScoreManScoring(ScoreManScoringV1 scoreManScoringV1) {
        Assert.notNull(scoreManScoringV1, ScoreManScoringConst.SCORE_MAN_SCORING_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getScoreManScoringId(), "?????????????????????id????????????");
        Assert.notNull(scoreManScoringV1.getStatus(), ScoreManScoringConst.STAUS_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getScoreItemsId(), ScoreManScoringConst.SCORE_ITEMS_ID_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getTemplateHeadId(), ScoreManScoringConst.TEMPLATE_HEAD_ID_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getTemplateName(), ScoreManScoringConst.TEMPLATE_NAME_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getProjectName(), ScoreManScoringConst.PROJECT_NAME_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getEvaluationPeriod(), ScoreManScoringConst.EVALUATION_PERIOD_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getPerStartMonth(), ScoreManScoringConst.PER_START_MONTH_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getPerEndMonth(), ScoreManScoringConst.PER_END_MONTH_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getOrganizationId(), PerfLevelConst.ORGANIZATION_ID_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getOrganizationName(), PerfLevelConst.ORGANIZATION_NAME_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getCategoryId(), ScoreManScoringConst.CATEGORY_ID_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getCategoryName(), ScoreManScoringConst.CATEGORY_NAME_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getCompanyId(), ScoreManScoringConst.COMPANY_ID_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getCompanyCode(), ScoreManScoringConst.COMPANY_CODE_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getCompanyName(), ScoreManScoringConst.COMPANY_NAME_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getDimWeightId(), ScoreManScoringConst.DIM_WEIGHT_ID_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getIndicatorType(), IndicatorsConst.INDICATOR_TYPE_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getIndicatorDimensionType(), IndicatorsConst.INDICATOR_DIMENSION_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getIndicatorDimensionWeight(), ScoreManScoringConst.INDICATOR_DIMENSION_WEIGHT_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getIndicatorName(), IndicatorsConst.INDICATOR_NAME_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getIndicatorLineType(), ScoreManScoringConst.INDICATOR_LINE_TYPE_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getQuoteMode(), IndicatorsConst.QUOTE_MODE_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getDimensionWeight(), ScoreManScoringConst.DIMENSION_WEIGHT_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getScoreUserName(), ScoreManScoringConst.SCORE_USER_NAME_NOT_NULL);
        Assert.notNull(scoreManScoringV1.getScoreNickName(), ScoreManScoringConst.SCORE_NICK_NAME_NOT_NULL);
    }

    /**
     * ????????????
     *
     * @param scoreManScoringV1
     */
    @Override
    public void uploadFile(ScoreManScoringV1 scoreManScoringV1) {
        Assert.notNull(scoreManScoringV1.getScoreManScoringFileId(), "??????????????????????????????");
        Assert.isTrue(StringUtils.isNotEmpty(scoreManScoringV1.getScoreManScoringFileName()), "????????????????????????????????????");
        ScoreManScoringV1 byId = this.getById(scoreManScoringV1.getScoreManScoringId());
        byId.setScoreManScoringFileId(scoreManScoringV1.getScoreManScoringFileId())
                .setScoreManScoringFileName(scoreManScoringV1.getScoreManScoringFileName());
        this.updateById(byId);
    }

    /**
     * ????????????
     *
     * @param scoreManScoringV1
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(ScoreManScoringV1 scoreManScoringV1) {
        ScoreManScoringV1 byId = this.getById(scoreManScoringV1.getScoreManScoringId());
        byId.setScoreManScoringFileId(null)
                .setScoreManScoringFileName(null);
        this.updateById(byId);
        fileCenterClient.delete(scoreManScoringV1.getScoreManScoringFileId());
    }

    /**
     * ????????????????????????????????? ??????????????????????????????
     *
     * @param scoreItemsId
     */
    @Override
    public void lockIfEndScored(Long scoreItemsId) {
        List<ScoreManScoringV1> scoreManScoringV1List = this.list(Wrappers.lambdaQuery(ScoreManScoringV1.class)
                .eq(ScoreManScoringV1::getScoreItemsId, scoreItemsId));
        if (CollectionUtils.isNotEmpty(scoreManScoringV1List)) {
            scoreManScoringV1List.forEach(scoreManScoringV1 -> {
                scoreManScoringV1.setIfEndScored(YesOrNo.YES.getValue());
            });
            try {
                this.updateBatchById(scoreManScoringV1List);
            } catch (Exception e) {
                StringBuffer sb = new StringBuffer();
                sb.append("???????????????????????????????????????????????????????????????");
                log.error(sb.toString());
                throw new BaseException(LocaleHandler.getLocaleMsg(sb.toString()));
            }
        }
    }

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param scoreManScoringV1List
     * @return
     */
    @Override
    public BaseResult<ScoreManScoringV1SubmitConfirmDTO> confirmBeforeScoreManScoringSubmit
    (List<ScoreManScoringV1> scoreManScoringV1List) {
        List<Long> ids = scoreManScoringV1List.stream()
                .map(ScoreManScoringV1::getScoreManScoringId).collect(Collectors.toList());
        List<ScoreManScoringV1> scoreManScoringV1s = this.listByIds(ids);
        scoreManScoringV1s.forEach(s ->{
            if (StringUtils.equals(s.getIfEndScored(), YesOrNo.YES.getValue())) {
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????[x][x]?????????????????????,??????????????????",s.getCompanyName(),s.getIndicatorName()));
            }
        });
        // ?????????????????????
        ScoreManScoringV1SubmitConfirmDTO submitConfirmDTO = new ScoreManScoringV1SubmitConfirmDTO();
        BaseResult baseResult = BaseResult.build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
        List<String> notScoredList = new ArrayList<>();
        scoreManScoringV1List.forEach(scoreManScoringV1 -> {
            if (Objects.isNull(scoreManScoringV1.getPefScore())) {
                StringBuffer sb = new StringBuffer();
                sb.append("???????????????[").append(scoreManScoringV1.getProjectName()).append("]??????????????????[")
                        .append(INDICATOR_DIMENSION_MAP.get(scoreManScoringV1.getIndicatorDimensionType())).append("]??????????????????[")
                        .append(scoreManScoringV1.getIndicatorName()).append("]?????????????????????[").append(scoreManScoringV1.getCompanyName()).append("]???????????????");
                notScoredList.add(sb.toString());
            }
        });
        submitConfirmDTO.setNotScoredList(notScoredList);
        baseResult.setData(submitConfirmDTO);
        return baseResult;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param scoreManScoringV1List
     * @return
     */
    @Override
    public List<PerfScoreItems> getScoreItemsAfterSubmit(List<ScoreManScoringV1> scoreManScoringV1List) {
        List<Long> scoreItemsIdList = scoreManScoringV1List.stream().map(ScoreManScoringV1::getScoreItemsId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(scoreItemsIdList)) {
            return Collections.EMPTY_LIST;
        }
        return iPerfScoreItemsService.listByIds(scoreItemsIdList);
    }

    /**
     * ?????????????????????
     *
     * @param scoreManScoringV1ExportParam
     * @param response
     * @return
     * @throws IOException
     */
    @Override
    public void exportScoreManScoringV1Excel
    (ExportExcelParam<ScoreManScoringV1> scoreManScoringV1ExportParam, HttpServletResponse response) throws
            IOException {
        // ?????????????????????
        List<List<Object>> dataList = this.queryExportData(scoreManScoringV1ExportParam);
        // ??????
        LinkedHashMap<String, String> scoreManScoringV1Titles = ScoreManScoringV1ExportUtils.getScoreManScoringV1Titles();
        scoreManScoringV1Titles.put("scoreManScoringId", "????????????");
        List<String> head = scoreManScoringV1ExportParam.getMultilingualHeader(scoreManScoringV1ExportParam, scoreManScoringV1Titles);
        // ?????????
        String fileName = scoreManScoringV1ExportParam.getFileName();
        // ????????????
        EasyExcelUtilNotAutoSetWidth.exportStart(response, dataList, head, fileName);
    }

    /**
     * ?????????????????????
     *
     * @param response
     * @throws Exception
     */
    @Override
    public void importScoreManScoringV1Download(HttpServletResponse response) throws Exception {
        String fileName = "?????????????????????????????????";
        List<ScoreManScoringV1Import> scoreManScoringV1Imports = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        List column = Arrays.asList(IntStream.range(0, 11).toArray());
        HashMap titleConfig = new HashMap();
        titleConfig.put(0, "???????????????????????????");
        titleConfig.put(1, "??????????????????????????????");
        titleConfig.put(2, "???????????????????????????");
        titleConfig.put(3, "???????????????????????????");
        titleConfig.put(4, "?????????");
        titleConfig.put(5, "?????????");
        titleConfig.put(6, "?????????");
        titleConfig.put(7, "?????????");
        titleConfig.put(8, "?????????");
        titleConfig.put(9, "?????????");
        titleConfig.put(10, "?????????");
        titleConfig.put(11, "?????????");
        titleConfig.put(12, "?????????");
        titleConfig.put(13, "?????????");
        titleConfig.put(14, "?????????");
        titleConfig.put(15, "???????????????????????????");
        TitleHandler titleHandler = new TitleHandler(column, IndexedColors.RED.index, titleConfig);
        EasyExcelUtil.writeExcelWithModel(outputStream, scoreManScoringV1Imports, ScoreManScoringV1Import.class, fileName, titleHandler);
    }

    /**
     * ????????????????????????????????????
     *
     * @param file
     * @param fileupload
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> importScoreManScoringV1Excel(MultipartFile file, Fileupload fileupload) throws
            Exception {
        // ????????????
        EasyExcelUtil.checkParam(file, fileupload);
        // ????????????
        List<ScoreManScoringV1Import> scoreManScoringV1ImportList = readData(file);
        // ?????????????????????
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        // ????????????
        List<ScoreManScoringV1> scoreManScoringV1List = getImportData(scoreManScoringV1ImportList, errorFlag);
        if (errorFlag.get()) {
            //??????
            fileupload.setFileSourceName("?????????????????????????????????");
            Fileupload fileUpload = EasyExcelUtil.uploadErrorFile(fileCenterClient, fileupload,
                    scoreManScoringV1ImportList, ScoreManScoringV1Import.class, file.getName(), file.getOriginalFilename(), file.getContentType());
            return ImportStatus.importError(fileUpload.getFileuploadId(), fileUpload.getFileSourceName());
        } else {
            // ????????????????????????
            updateScoreManScoringV1s(scoreManScoringV1List);
        }
        return ImportStatus.importSuccess();
    }

    /**
     * ????????????excel???????????????
     *
     * @return
     */
    @Override
    public List<ScoreManScoringV1> listExcelImportData() {
        return this.list(Wrappers.lambdaQuery(ScoreManScoringV1.class)
                .eq(ScoreManScoringV1::getIfExcelImport, YesOrNo.YES.getValue())
                .eq(ScoreManScoringV1::getIfFreshAfterImport, YesOrNo.NO.getValue())
        );
    }

    /**
     * ????????????excel???????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freshExcelImportData() {
        List<ScoreManScoringV1> scoreManScoringV1s = this.listExcelImportData();
        scoreManScoringV1s.forEach(scoreManScoringV1 -> {
            scoreManScoringV1.setIfFreshAfterImport(YesOrNo.YES.getValue());
        });
        this.updateBatchById(scoreManScoringV1s);
    }

    /**
     * ??????excel???????????????
     *
     * @param file
     * @return
     */
    private List<ScoreManScoringV1Import> readData(MultipartFile file) {
        List<ScoreManScoringV1Import> scoreManScoringV1ImportList = null;
        try {
            // ???????????????
            InputStream inputStream = file.getInputStream();
            // ???????????????
            AnalysisEventListenerImpl<ScoreManScoringV1Import> listener = new AnalysisEventListenerImpl<>();
            ExcelReader excelReader = EasyExcel.read(inputStream, listener).build();
            // ?????????sheet????????????
            ReadSheet readSheet = EasyExcel.readSheet(0).head(ScoreManScoringV1Import.class).build();
            // ?????????????????????sheet
            excelReader.read(readSheet);
            scoreManScoringV1ImportList = listener.getDatas();
        } catch (IOException e) {
            throw new BaseException("excel????????????");
        }
        return scoreManScoringV1ImportList;
    }

    /**
     * ?????????????????????, ????????????????????????
     */
    private List<ScoreManScoringV1> getImportData(List<ScoreManScoringV1Import> scoreManScoringV1ImportList, AtomicBoolean errorFlag) throws IOException, ParseException {
        List<ScoreManScoringV1> scoreManScoringV1s = new ArrayList<>();
        BigDecimal zero = new BigDecimal(0);
        BigDecimal hundred = new BigDecimal(100);
        if (CollectionUtils.isNotEmpty(scoreManScoringV1ImportList)) {
            for (ScoreManScoringV1Import scoreManScoringV1Import : scoreManScoringV1ImportList) {
                ScoreManScoringV1 scoreManScoringV1 = new ScoreManScoringV1();

                List<Long> scoreManScoringV1IdList = new ArrayList<>();

                List<String> scoreManScoringV1IdStringList = scoreManScoringV1ImportList.stream().map(ScoreManScoringV1Import::getScoreManScoringId)
                        .filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toList());
                scoreManScoringV1IdStringList.forEach(scoreManScoringV1IdString -> {
                    scoreManScoringV1IdList.add(Long.valueOf(scoreManScoringV1IdString));
                });

                List<ScoreManScoringV1> dbScoreManScoringV1List = this.listByIds(scoreManScoringV1IdList);

                Map<Long, ScoreManScoringV1> dbScoreManScoringMap = dbScoreManScoringV1List.stream().collect(Collectors.toMap(k->k.getScoreManScoringId(), Function.identity(), (o1, o2) -> o2));

                StringBuffer errorMsg = new StringBuffer();
                // ????????????id
                String scoreManScoringIdString = scoreManScoringV1Import.getScoreManScoringId();
                if (StringUtils.isEmpty(scoreManScoringIdString)) {
                    errorFlag.set(true);
                    errorMsg.append("????????????????????????????????????????????????????????????????????????????????????????????????");
                } else {
                    Long scoreManScoringId = Long.valueOf(scoreManScoringIdString);
                    if (!dbScoreManScoringMap.containsKey(scoreManScoringId) && Objects.nonNull(dbScoreManScoringMap.get(scoreManScoringId))) {
                        errorFlag.set(true);
                        errorMsg.append("?????????????????????[").append(scoreManScoringId).append("]??????????????????????????????????????????????????????????????????????????????????????????????????????");
                    } else {
                        ScoreManScoringV1 manScoringV1 = dbScoreManScoringMap.get(scoreManScoringId);
                        String quoteMode = manScoringV1.getQuoteMode();
                        // ???????????????
                        String pefScoreString = scoreManScoringV1Import.getPefScore();

                        if (StringUtils.isNotEmpty(pefScoreString)) {
                            // ???????????? ????????????
                            if (Objects.equals(QuoteMode.DIRECT_QUOTE.getValue(), quoteMode) || Objects.equals(QuoteMode.INTERVAL_CONVERSION.getValue(), quoteMode)) {
                                if (!isNumeric(pefScoreString)) {
                                    errorFlag.set(true);
                                    errorMsg.append("??????????????????[").append(pefScoreString).append("]??????????????????");
                                } else {
                                    BigDecimal pefScore = new BigDecimal(pefScoreString);
                                    if (pefScore.compareTo(zero) < 0 || pefScore.compareTo(hundred) > 0) {
                                        errorFlag.set(true);
                                        errorMsg.append("??????????????????[").append(pefScoreString).append("]??????????????????????????????????????????0-100????????????");
                                    }
                                    scoreManScoringV1.setPefScore(pefScore);
                                }
                            }
                            // ????????????
                            if (Objects.equals(QuoteMode.TEXT_CONVERSION.getValue(), quoteMode)) {
                                BigDecimal pefScore = new BigDecimal(pefScoreString);
                                scoreManScoringV1.setPefScore(pefScore);
                            }
                        }
                        scoreManScoringV1.setComments(scoreManScoringV1Import.getComments());
                    }
                }
                Long scoreManScoringId = Long.valueOf(scoreManScoringIdString);
                scoreManScoringV1.setScoreManScoringId(scoreManScoringId);

                if (errorMsg.length() > 0) {
                    scoreManScoringV1Import.setErrorMsg(errorMsg.toString());
                } else {
                    scoreManScoringV1Import.setErrorMsg(null);
                }
                if (Objects.nonNull(scoreManScoringV1.getPefScore())) {
                    scoreManScoringV1s.add(scoreManScoringV1);
                }
            }
        }
        return scoreManScoringV1s;
    }

    /**
     * ??????????????????
     *
     * @param scoreManScoringV1List
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateScoreManScoringV1s(List<ScoreManScoringV1> scoreManScoringV1List) {
        List<ScoreManScoringV1> updateList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(scoreManScoringV1List)) {
            for (ScoreManScoringV1 scoreManScoringV1 : scoreManScoringV1List) {
                if (Objects.nonNull(scoreManScoringV1)) {
                    ScoreManScoringV1 v1 = this.getById(scoreManScoringV1.getScoreManScoringId());
                    v1.setPefScore(scoreManScoringV1.getPefScore());
                    v1.setIfExcelImport(YesOrNo.YES.getValue())
                            .setIfFreshAfterImport(YesOrNo.NO.getValue())
                            .setComments(scoreManScoringV1.getComments());
                    updateList.add(v1);
                }
            }
            if (CollectionUtils.isNotEmpty(updateList)) {
                log.info("???excel???????????????????????????????????????????????????...????????????");
                this.updateBatchById(updateList);
            }
        }
    }

    /**
     * ???????????????????????????excel?????????
     *
     * @param exportParam
     * @return
     * @Auth xiexh12@meicloud.com 2021-01-28 19:50
     */
    private List<List<Object>> queryExportData(ExportExcelParam<ScoreManScoringV1> exportParam) {
        // ????????????
        ScoreManScoringV1 queryParam = exportParam.getQueryParam();

        /**
         * ???????????????????????????????????????, ??????2???????????????????????????????????????????????????, ??????????????????2???
         */
        int count = this.listScoreManScoringPage(queryParam).size();
        if (count > 50000) {
            /**
             * ????????????????????????50000, ??????????????????????????????
             */
            if (StringUtil.notEmpty(queryParam.getPageSize()) && StringUtil.notEmpty(queryParam.getPageNum())) {
                Assert.isTrue(queryParam.getPageSize() <= 50000, "????????????????????????50000");
            } else {
                throw new BaseException("??????????????????????????????50000???, ?????????????????????, ????????????50000;");
            }
        }
        boolean flag = null != queryParam.getPageSize() && null != queryParam.getPageNum();
        if (flag) {
            // ????????????
            PageUtil.startPage(queryParam.getPageNum(), queryParam.getPageSize());
        }
        List<ScoreManScoringV1> scoreManScoringExportList = this.listScoreManScoringPage(queryParam);

        // ???Map(?????????map???key???????????????value????????????)
        List<Map<String, Object>> mapList = BeanMapUtils.objectsToMaps(scoreManScoringExportList);
        // ??????excel?????????
        List<String> titleList = exportParam.getTitleList();

        // ??????????????????????????????????????? ???
        List<String> perStartMonthAndEndMonth = Arrays.asList("perStartMonth", "perEndMonth");
        // ?????????????????????????????? ???
        List<String> perScoreAndScore = Arrays.asList("perScore", "score");
        // ??? ???
        Map<String, String> yNMap= new HashMap<>();
        yNMap.put("Y", "???");
        yNMap.put("N", "???");

        List<List<Object>> results = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(mapList)) {
            mapList.forEach((map) -> {
                List<Object> list = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(titleList)) {
                    titleList.forEach((title) -> {
                        if (Objects.nonNull(map)) {
                            Object value = map.get(title);
                            // ???????????? ???????????????
                            if ("indicatorDimensionType".equals(title)) {
                                String s = INDICATOR_DIMENSION_MAP.get(String.valueOf(value));
                                list.add(s);
                            }
                            // ???????????????????????????
                            else if (perStartMonthAndEndMonth.contains(title)) {
                                Date date = DateUtil.localDateToDate((LocalDate) value);
                                String s = DateUtil.format(date, "yyyy-MM");
                                list.add(s);
                            } else if (perScoreAndScore.contains(title)) {
                                BigDecimal bigDecimal = (BigDecimal) value;
                                if (null == bigDecimal) {
                                    list.add("");
                                } else if (BigDecimal.ZERO.compareTo(bigDecimal) == 0) {
                                    list.add("0");
                                } else {
                                    list.add(String.valueOf(bigDecimal));
                                }
                            } else if ("scoreNickName".equals(title)) {
                                String scoreNickName = (String) map.get("scoreNickName");
                                String scoreUserName = (String) map.get("scoreUserName");
                                String scorer = scoreNickName + "(" + scoreUserName + ")";
                                list.add(scorer);
                            } else if ("ifEndScored".equals(title)) {
                                String s = yNMap.get(String.valueOf(value));
                                list.add(s);
                            }
                            else {
                                list.add(StringUtil.StringValue(value));
                            }
                        }
                    });
                }
                results.add(list);
            });
        }
        return results;
    }

    /**
     * ????????????????????????
     *
     * @param queryDTO
     * @return
     */
    public QueryWrapper<ScoreManScoringV1> queryByParam(ScoreManScoringV1 queryDTO) {
        QueryWrapper<ScoreManScoringV1> queryWrapper = new QueryWrapper<>();
        // ????????????
        if (StringUtils.isNotEmpty(queryDTO.getProjectName())) {
            queryWrapper.like("PROJECT_NAME", queryDTO.getProjectName());
        }
        // ????????????
        if (Objects.nonNull(queryDTO.getOrganizationId())) {
            queryWrapper.eq("ORGANIZATION_ID", queryDTO.getOrganizationId());
        }
        // ???????????????
        if (StringUtils.isNotEmpty(queryDTO.getCompanyName())) {
            queryWrapper.like("COMPANY_NAME", queryDTO.getCompanyName());
        }
        // ????????????
        if (StringUtils.isNotEmpty(queryDTO.getIndicatorDimensionType())) {
            queryWrapper.eq("INDICATOR_DIMENSION_TYPE", queryDTO.getIndicatorDimensionType());
        }
        // ????????????
        if (StringUtils.isNotEmpty(queryDTO.getIndicatorName())) {
            queryWrapper.eq("INDICATOR_NAME", queryDTO.getIndicatorName());
        }
        return queryWrapper;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param scoreManScoringV1List
     */
    public void setIndicatorLogic(List<ScoreManScoringV1> scoreManScoringV1List) {
        if (CollectionUtils.isNotEmpty(scoreManScoringV1List)) {
            scoreManScoringV1List.forEach(scoreManScoringV1 -> {
                PerfTemplateLine templateLine = iPerfTemplateLineService.getById(scoreManScoringV1.getTemplateLineId());
                scoreManScoringV1.setIndicatorLogic(templateLine.getIndicatorLogic());
            });
        }
    }

    public static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("^(\\-)?[0-9]+(\\.[0-9]*)?$");
        return pattern.matcher(str).matches();
    }


}
