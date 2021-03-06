package com.midea.cloud.srm.perf.scoring.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.enums.perf.indicators.IndicatorsDimensionEnum;
import com.midea.cloud.common.enums.perf.indicators.IndicatorsEvaluetionEnum;
import com.midea.cloud.common.enums.perf.scoreproject.ScoreItemsProjectStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.common.utils.group.SortUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.model.common.ExportExcelParam;
import com.midea.cloud.srm.model.perf.level.entity.PerfLevel;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemsSup;
import com.midea.cloud.srm.model.perf.scoring.*;
import com.midea.cloud.srm.model.perf.template.entity.PerfTemplateCategory;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.dto.PerfOverallScoreDto;
import com.midea.cloud.srm.perf.common.OverallScoreConst;
import com.midea.cloud.srm.perf.common.PerfLevelConst;
import com.midea.cloud.srm.perf.common.ScoreManScoringConst;
import com.midea.cloud.srm.perf.level.service.IPerfLevelService;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsService;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsSupService;
import com.midea.cloud.srm.perf.scoring.constants.VendorFeedbackStatus;
import com.midea.cloud.srm.perf.scoring.mapper.PerfOverallScoreMapper;
import com.midea.cloud.srm.perf.scoring.service.*;
import com.midea.cloud.srm.perf.scoring.utils.OverallScoreExportUtils;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateCategoryService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ???????????????????????? ???????????????
 * </pre>
 *
 * @author wuwl18@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-06-16 11:50:39
 *  ????????????:
 * </pre>
 */
@Service
public class PerfOverallScoreServiceImpl extends ServiceImpl<PerfOverallScoreMapper, PerfOverallScore>
        implements IPerfOverallScoreService {

    /**
     * ?????????????????????Service
     **/
    @Resource
    private IPerfScoreManScoringService iPerfScoreManScoringService;

    /**
     * ?????????????????????????????????Service
     **/
    @Resource
    private IPerfIndDimScoreDetailService iPerfIndDimScoreDetailService;

    /**
     * ???????????????????????????Service
     **/
    @Resource
    private IPerfIndicatorDimScoreService iPerfIndicatorDimScoreService;

    /**
     * ??????????????????????????????Service
     **/
    @Resource
    private IPerfOverallScoreService iPerfOverallScoreService;

    /**
     * ????????????Service
     */
    @Resource
    private IPerfLevelService iPerfLevelService;

    @Autowired
    private IPerfTemplateCategoryService iPerfTemplateCategoryService;

    /**
     * ???????????????
     */
    @Resource
    private SortUtil sortUtil;

    @Autowired
    private FileCenterClient fileCenterClient;

    /**
     * Excel?????????????????????
     */
    @Resource
    private ExportExcelParam exportExcelParam;

    @Autowired
    private IPerfScoreItemsService iPerfScoreItemsService;

    @Autowired
    private IScoreManScoringV1Service iScoreManScoringV1Service;

    @Autowired
    private IPerfScoreItemsSupService iPerfScoreItemsSupService;


    /**
     * ??????????????????
     *
     * @param scoreItemsId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean generateScoreInfo(Long scoreItemsId) {
        boolean result = false;

        // ??????????????????
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        String nickName = "";
        if (Objects.nonNull(user)) {
            nickName = user.getNickname();
        }

        // ??????????????????????????????????????????????????????????????????
//        PerfScoreItems scoreItems = iPerfScoreItemsService.getById(scoreItemsId);
//        if (scoreItems.getScorePeople() != scoreItems.getScorePeopleCount()) {
//            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????????????????????????????"));
//        }

        /** ??????????????????????????????????????????????????? */
        List<PerfIndDimScoreDetail> indDimScoreDetailList = new ArrayList<>();
        PerfScoreManScoring queryScoreManScoring = new PerfScoreManScoring();
        queryScoreManScoring.setScoreItemsId(scoreItemsId);
        List<PerfScoreManScoring> scoreManScoringList = iPerfScoreManScoringService.list(new QueryWrapper<>(queryScoreManScoring));
        if (CollectionUtils.isNotEmpty(scoreManScoringList)) {
            for (PerfScoreManScoring manScoring : scoreManScoringList) {
                if (null != manScoring) {
                    /**????????????????????????????????????????????????????????????*/
                    //Assert.notNull((null != manScoring.getScore()), OverallScoreConst.CHECK_IS_CALCULATE_SCORE);
                    // ?????????????????? (SCORING_SYSTEM_VALUE:??????-????????????,DEDUCTION_SYSTEM_VALUE:??????-????????????,SCORING_MANUAL:??????-??????,DEDUCTION_MANUAL:??????-??????)
                    String evaluation = manScoring.getEvaluation();
                    PerfIndDimScoreDetail copyScoring = new PerfIndDimScoreDetail();
                    BeanUtils.copyProperties(manScoring, copyScoring);
                    copyScoring.setCreatedFullName(nickName);
                    // ?????????????????????????????????????????????100 + (-6)
                    // ??????-???????????????-????????????
                    if (Objects.nonNull(manScoring.getScore()) && (IndicatorsEvaluetionEnum.DEDUCTION_MANUAL.getValue().equals(evaluation)
                            || IndicatorsEvaluetionEnum.DEDUCTION_SYSTEM_VALUE.getValue().equals(evaluation))) {
                        copyScoring.setScore(new BigDecimal(100).add(manScoring.getScore()));
                    }
                    indDimScoreDetailList.add(copyScoring);
                }
            }
        }
        Assert.isTrue(CollectionUtils.isNotEmpty(indDimScoreDetailList), OverallScoreConst.IND_DIM_SCORE_DETAIL_NOT_NULL);
        log.debug("indDimScoreDetailList: " + JsonUtil.arrayToJsonStr(indDimScoreDetailList));
        // ??????????????????????????????????????????
        List<PerfIndicatorDimScore> indicatorDimScoreList = iPerfScoreManScoringService.getSaveIndicatorDimScoreList(queryScoreManScoring);
        Assert.isTrue(CollectionUtils.isNotEmpty(indicatorDimScoreList), OverallScoreConst.INDICATOR_DIM_SCORE_NOT_NULL);

        // ????????????????????????????????????????????????,?????????ID
        List<PerfOverallScore> overallScoreList = iPerfScoreManScoringService.getSaveOverallScoreList(queryScoreManScoring);
        Assert.isTrue(CollectionUtils.isNotEmpty(overallScoreList), OverallScoreConst.OVEALL_SCORE_NOT_NULL);
        for (PerfOverallScore overallScore : overallScoreList) {
            if (Objects.nonNull(overallScore)) {
                Long overallScoreId = IdGenrator.generate();
                overallScore.setOverallScoreId(overallScoreId);
                overallScore.setStatus(ScoreItemsProjectStatusEnum.SCORE_DRAFT.getValue());
                overallScore.setVendorFeedbackStatus(VendorFeedbackStatus.NOT_CONFIRMED.getValue());
                Long scoreItemsIdLong = overallScore.getScoreItemsId();
                String evaluationPeriod = overallScore.getEvaluationPeriod();
                LocalDate perStartMonth = overallScore.getPerStartMonth();
                LocalDate perEndMonth = overallScore.getPerEndMonth();
                Long organizationId = overallScore.getOrganizationId();
                Long categoryId = overallScore.getCategoryId();
                Long companyId = overallScore.getCompanyId();

                if (null != scoreItemsIdLong && StringUtils.isNotBlank(evaluationPeriod) && null != perStartMonth && null != perEndMonth
                        && null != organizationId && null != categoryId && null != companyId) {
                    for (PerfIndicatorDimScore indicatorDimScore : indicatorDimScoreList) {
                        if (null != indicatorDimScore && scoreItemsIdLong.compareTo(indicatorDimScore.getScoreItemsId()) == 0 &&
                                evaluationPeriod.equals(indicatorDimScore.getEvaluationPeriod()) && perStartMonth.isEqual(indicatorDimScore.getPerStartMonth())
                                && perEndMonth.isEqual(indicatorDimScore.getPerEndMonth()) && organizationId.compareTo(indicatorDimScore.getOrganizationId()) == 0
                                && categoryId.compareTo(indicatorDimScore.getCategoryId()) == 0 && companyId.compareTo(indicatorDimScore.getCompanyId()) == 0
                        ) {
                            indicatorDimScore.setIndicatorDimScoreId(IdGenrator.generate());
                            indicatorDimScore.setOverallScoreId(overallScoreId);
                        }
                    }
                }
            }
        }

        // ???????????????????????????????????????????????????,?????????ID????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????Map??????
        // ????????????Map
        Map<String, List<PerfIndicatorDimScore>> sortIndicatorDimScoreListMap = new HashMap<>();
        for (PerfIndicatorDimScore indicatorDimScore : indicatorDimScoreList) {
            Long scoreItemsIdLong = indicatorDimScore.getScoreItemsId();
            Long indicatorDimScoreId = indicatorDimScore.getIndicatorDimScoreId();
            String evaluationPeriod = indicatorDimScore.getEvaluationPeriod();
            LocalDate perStartMonth = indicatorDimScore.getPerStartMonth();
            LocalDate perEndMonth = indicatorDimScore.getPerEndMonth();
            Long organizationId = indicatorDimScore.getOrganizationId();
            Long categoryId = indicatorDimScore.getCategoryId();
            Long companyId = indicatorDimScore.getCompanyId();
            Long dimWeightId = indicatorDimScore.getDimWeightId();
            if (null != scoreItemsIdLong && StringUtils.isNotEmpty(evaluationPeriod) && null != perStartMonth && null != perEndMonth
                    && null != organizationId && null != categoryId && null != companyId && null != dimWeightId) {
                // ???????????????????????????????????????
                BigDecimal dimScoreScore = new BigDecimal(0.00);
                for (PerfIndDimScoreDetail indDimScoreDetail : indDimScoreDetailList) {
                    if (null != indDimScoreDetail && scoreItemsIdLong.compareTo(indDimScoreDetail.getScoreItemsId()) == 0 &&
                            evaluationPeriod.equals(indDimScoreDetail.getEvaluationPeriod()) && perStartMonth.isEqual(indDimScoreDetail.getPerStartMonth())
                            && perEndMonth.isEqual(indDimScoreDetail.getPerEndMonth()) && organizationId.compareTo(indDimScoreDetail.getOrganizationId()) == 0
                            && categoryId.compareTo(indDimScoreDetail.getCategoryId()) == 0 && companyId.compareTo(indDimScoreDetail.getCompanyId()) == 0
                            && dimWeightId.compareTo(indDimScoreDetail.getDimWeightId()) == 0
                    ) {
                        indDimScoreDetail.setIndDimScoreDetailId(IdGenrator.generate());
                        indDimScoreDetail.setIndicatorDimScoreId(indicatorDimScoreId);

                        dimScoreScore = dimScoreScore.add(indDimScoreDetail.getScore().multiply(indDimScoreDetail.getDimensionWeight()).divide(new BigDecimal(100)));
                    }
                }
                indicatorDimScore.setScore(dimScoreScore);

                /** ??????????????????????????????????????????????????????????????????????????? **/
                Long overallScoreId = indicatorDimScore.getOverallScoreId();
                // ??????????????????
                String indicatorDimensionType = indicatorDimScore.getIndicatorDimensionType();
                // ????????????
                String indicatorDimensionWeight = indicatorDimScore.getIndicatorDimensionWeight();
                for (PerfOverallScore overallScore : overallScoreList) {
                    if (null != overallScore && null != overallScoreId && overallScoreId.compareTo(overallScore.getOverallScoreId()) == 0) {
                        // ????????????????????????????????????
                        BigDecimal overallScoreScore = overallScore.getScore().add(dimScoreScore.multiply(new BigDecimal(indicatorDimensionWeight)).divide(new BigDecimal(100)));
                        // ??????
                        if (IndicatorsDimensionEnum.QUALITY.getValue().equals(indicatorDimensionType)) {
                            overallScore.setScoreAttribute1(dimScoreScore);
                        }
                        // ??????
                        else if (IndicatorsDimensionEnum.COST.getValue().equals(indicatorDimensionType)) {
                            overallScore.setScoreAttribute2(dimScoreScore);
                        }
                        // ??????
                        else if (IndicatorsDimensionEnum.DELIVER.getValue().equals(indicatorDimensionType)) {
                            overallScore.setScoreAttribute3(dimScoreScore);
                        }
                        // ??????
                        else if (IndicatorsDimensionEnum.SERVICE.getValue().equals(indicatorDimensionType)) {
                            overallScore.setScoreAttribute4(dimScoreScore);
                        }
                        // ??????
                        else if (IndicatorsDimensionEnum.TECHNOLOGY.getValue().equals(indicatorDimensionType)) {
                            overallScore.setScoreAttribute5(dimScoreScore);
                        }
                        overallScore.setScore(overallScoreScore);
                    }
                }

                // ????????????????????????????????????????????????Map??????
                // ????????????
                List<PerfIndicatorDimScore> sortIndicatorDimScoreList = new ArrayList<>();
                if (StringUtils.isNotEmpty(indicatorDimensionType) && null != categoryId) {
                    for (PerfIndicatorDimScore dimScore : indicatorDimScoreList) {
                        if (null != dimScore && indicatorDimScoreId.compareTo(dimScore.getIndicatorDimScoreId()) != 0
                                && categoryId.compareTo(dimScore.getCategoryId()) == 0
                                && indicatorDimensionType.equals(dimScore.getIndicatorDimensionType())) {
                            sortIndicatorDimScoreList.add(dimScore);
                        }
                    }
                    sortIndicatorDimScoreList.add(indicatorDimScore);
                    if (CollectionUtils.isNotEmpty(sortIndicatorDimScoreList)) {
                        sortIndicatorDimScoreListMap.put(String.valueOf(categoryId) + indicatorDimensionType, sortIndicatorDimScoreList);
                    }
                }
            }
        }

        // ?????????????????????????????????(???????????????????????????????????????????????????)
        // ???????????????????????????????????????????????????
        List<PerfIndicatorDimScore> saveIndicatorDimScoreList = new ArrayList();
        if (null != sortIndicatorDimScoreListMap) {
            for (Map.Entry<String, List<PerfIndicatorDimScore>> dimScoreMap : sortIndicatorDimScoreListMap.entrySet()) {
                if (CollectionUtils.isNotEmpty(dimScoreMap.getValue())) {
                    List<PerfIndicatorDimScore> dimScoreList = dimScoreMap.getValue();
                    sortUtil.sortGeneral(dimScoreList, "score", PerfIndicatorDimScore.class, SortUtil.DESC);
                    sortUtil.sortRank(dimScoreList, PerfIndicatorDimScore.class, "score", "rank", false);
                    for (PerfIndicatorDimScore dimScore : dimScoreList) {
                        if (null != dimScore) {
                            saveIndicatorDimScoreList.add(dimScore);
                        }
                    }
                }
            }
        }

        // ????????????????????????????????????????????????????????????(????????????????????????????????????)
        // ????????????????????????????????????Map
        Map<Long, List<PerfOverallScore>> sortOverallScoreListMap = new HashMap<>();
        //???????????????????????????????????????
        List<PerfOverallScore> saveOverallScoreList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(overallScoreList)) {
            for (PerfOverallScore overallScore : overallScoreList) {
                //????????????????????????????????????
                List<PerfOverallScore> sortOverallScoreList = new ArrayList<>();
                if (null != overallScore && null != overallScore.getOverallScoreId()) {
                    Long overallScoreId = overallScore.getOverallScoreId();
                    Long cogetoryId = overallScore.getCategoryId();
                    for (PerfOverallScore scoreEntity : overallScoreList) {
                        if (null != scoreEntity && overallScoreId.compareTo(scoreEntity.getOverallScoreId()) != 0
                                && cogetoryId.compareTo(scoreEntity.getCategoryId()) == 0) {
                            sortOverallScoreList.add(scoreEntity);
                        }
                    }
                }
                sortOverallScoreList.add(overallScore);
                if (CollectionUtils.isNotEmpty(sortOverallScoreList)) {
                    sortOverallScoreListMap.put(overallScore.getCategoryId(), sortOverallScoreList);
                }
            }
            if (null != sortOverallScoreListMap) {
                for (Map.Entry<Long, List<PerfOverallScore>> overallScoreMap : sortOverallScoreListMap.entrySet()) {
                    if (CollectionUtils.isNotEmpty(overallScoreMap.getValue())) {
                        List<PerfOverallScore> sortOverallScoreList = overallScoreMap.getValue();
                        sortUtil.sortGeneral(sortOverallScoreList, "score", PerfOverallScore.class, SortUtil.DESC);
                        sortUtil.sortRank(sortOverallScoreList, PerfOverallScore.class, "score", "rank", false);
                        for (PerfOverallScore overallScore : sortOverallScoreList) {
                            if (null != overallScore) {
                                //??????????????????
                                PerfLevel perfLevel = this.getLevelNameByOrgIdAndScore(overallScore.getOrganizationId(), overallScore.getScore());
                                if (null != perfLevel) {
                                    overallScore.setLevelId(perfLevel.getLevelId());
                                    overallScore.setLevelName(perfLevel.getLevelName());
                                }
                                saveOverallScoreList.add(overallScore);
                            }
                        }
                    }
                }
            }
        }
        boolean isSaveDetail = iPerfIndDimScoreDetailService.saveBatch(indDimScoreDetailList);
        if (!isSaveDetail) {
            log.error(OverallScoreConst.SAVE_FAIL_IND_DIM_SCORE_DETAILL);
            throw new BaseException(OverallScoreConst.SAVE_FAIL_IND_DIM_SCORE_DETAILL);
        }
        if (CollectionUtils.isNotEmpty(saveIndicatorDimScoreList)) {
            boolean isSaveIndicatorDimScore = iPerfIndicatorDimScoreService.saveBatch(saveIndicatorDimScoreList);
            if (!isSaveIndicatorDimScore) {
                log.error(OverallScoreConst.SAVE_FAIL_INDICATOR_DIM_SCORE);
                throw new BaseException(OverallScoreConst.SAVE_FAIL_INDICATOR_DIM_SCORE);
            }
        }
        if (CollectionUtils.isNotEmpty(saveOverallScoreList)) {
            boolean isSaveOvrallScore = iPerfOverallScoreService.saveBatch(saveOverallScoreList);
            if (!isSaveOvrallScore) {
                log.error(OverallScoreConst.SAVE_FAIL_OVEALL_SCORE);
                throw new BaseException(OverallScoreConst.SAVE_FAIL_OVEALL_SCORE);
            }
        }

        result = true;
        return result;
    }

    @Override
    public List<PerfOverallScore> findOverallScoreAndSonList(PerfOverallScore overallScore) throws BaseException {
        try {
            return getBaseMapper().findOverallScorelList(overallScore);
        } catch (Exception e) {
            log.error("????????????????????????????????????????????????????????????(??????????????????)????????????", e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }
    }

    /**
     * ????????????????????????????????? ???????????????
     * @param scoreItemId
     * @throws BaseException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishScoreItemsUpdateStatus(Long scoreItemId) throws BaseException {
        Assert.notNull(scoreItemId, "????????????id???????????????");
        List<PerfScoreItemsSup> perfScoreItemsSupList = iPerfScoreItemsSupService.list(Wrappers.lambdaQuery(PerfScoreItemsSup.class)
                .eq(PerfScoreItemsSup::getScoreItemsId, scoreItemId));
        List<PerfOverallScore> overallScoreList = iPerfOverallScoreService.list(Wrappers.lambdaQuery(PerfOverallScore.class)
                .eq(PerfOverallScore::getScoreItemsId, scoreItemId));
        if (CollectionUtils.isNotEmpty(overallScoreList)) {
            for (PerfOverallScore overallScore : overallScoreList) {
                if (Objects.nonNull(overallScore)) {
                    overallScore.setStatus(ScoreItemsProjectStatusEnum.RESULT_PUBLISHED.getValue())
                            .setIndicatorCount(perfScoreItemsSupList.size());
                }
            }
            this.updateBatchById(overallScoreList);
        }
    }

    @Override
    public List<PerfOverallScore> findOverallScoreList(PerfOverallScore perfOverallScore) throws BaseException {
        List<PerfOverallScore> overallScoreList = new ArrayList<>();
        QueryWrapper<PerfOverallScore> wrapper = new QueryWrapper<>();
        if (Objects.nonNull(perfOverallScore.getScoreItemsId())) {
            wrapper.eq("SCORE_ITEMS_ID", perfOverallScore.getScoreItemsId());
        }
        if (StringUtils.isNotEmpty(perfOverallScore.getProjectName())) {
            wrapper.like("PROJECT_NAME", perfOverallScore.getProjectName());
        }
        if (Objects.nonNull(perfOverallScore.getCompanyId())) {
            wrapper.eq("COMPANY_ID", perfOverallScore.getCompanyId());
        }
        if (StringUtils.isNotEmpty(perfOverallScore.getCompanyName())) {
            wrapper.like("COMPANY_NAME", perfOverallScore.getCompanyName());
        }
        if (Objects.nonNull(perfOverallScore.getOrganizationId())) {
            wrapper.eq("ORGANIZATION_ID", perfOverallScore.getOrganizationId());
        }
        if (StringUtils.isNotEmpty(perfOverallScore.getOrganizationName())) {
            wrapper.eq("ORGANIZATION_NAME", perfOverallScore.getOrganizationName());
        }
        if (Objects.nonNull(perfOverallScore.getCategoryId())) {
            wrapper.eq("CATEGORY_ID", perfOverallScore.getCategoryId());
        }
        if (StringUtils.isNotEmpty(perfOverallScore.getCategoryName())) {
            wrapper.eq("CATEGORY_NAME", perfOverallScore.getCategoryName());
        }
        if (null != perfOverallScore.getLevelId()) {
            wrapper.eq("LEVEL_ID", perfOverallScore.getLevelId());
        }
        if (StringUtils.isNotEmpty(perfOverallScore.getLevelName())) {
            wrapper.eq("LEVEL_NAME", perfOverallScore.getLevelName());
        }
        if (StringUtils.isNotEmpty(perfOverallScore.getTemplateName())) {
            wrapper.like("TEMPLATE_NAME", perfOverallScore.getTemplateName());
        }

        /** ????????????????????????(???????????????????????????????????????????????????????????????????????????????????????) */
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        // ????????????
        String userType = "";
        // ?????????ID
        Long userCompanyId = null;
        if (null != user) {
            userType = user.getUserType();
            userCompanyId = user.getCompanyId();
        }
        // ??????????????????????????????
        if ("VENDOR".equals(userType)) {
            wrapper.eq("COMPANY_ID", userCompanyId);
            wrapper.eq("STATUS", ScoreItemsProjectStatusEnum.RESULT_PUBLISHED.getValue());
        }
        wrapper.orderByDesc("LAST_UPDATE_DATE");
        overallScoreList = this.list(wrapper);
        // ????????????????????????????????????????????????
        overallScoreList.forEach(overallScore -> {
            Long templateHeadId = overallScore.getTemplateHeadId();
            List<PerfTemplateCategory> templateCategoryList = iPerfTemplateCategoryService.list(Wrappers.lambdaQuery(PerfTemplateCategory.class)
                    .eq(PerfTemplateCategory::getTemplateHeadId, templateHeadId));
            String categoryNames = "";
            if (CollectionUtils.isNotEmpty(templateCategoryList)) {
                List<String> categoryNameList = templateCategoryList.stream().filter(e -> Objects.nonNull(e.getCategoryId()))
                        .map(PerfTemplateCategory::getCategoryName).collect(Collectors.toList());
                categoryNames = StringUtils.join(categoryNameList, ";");
            }
            overallScore.setCategoryNames(categoryNames);
        });
        return overallScoreList;
    }

    /**
     * ?????????????????????
     * @param perfOverallScoreList
     */
    @Override
    public void vendorSubmitFeedback(List<PerfOverallScore> perfOverallScoreList) {
        List<Long> overallScoreIdList = perfOverallScoreList.stream().map(PerfOverallScore::getOverallScoreId).distinct().collect(Collectors.toList());
        List<PerfOverallScore> perfOverallScores = this.listByIds(overallScoreIdList);
        perfOverallScores.forEach(perfOverallScore -> {
            perfOverallScore.setVendorFeedbackStatus(VendorFeedbackStatus.CONFIRMED.getValue());
        });
        this.updateBatchById(perfOverallScores);
    }

    /**
     * Description ????????????????????????
     *
     * @return
     * @Param
     * @Author wuwl18@meicloud.com
     * @Date 2020.06.23
     **/
    private List<List<Object>> queryExportData(ExportExcelParam<PerfOverallScore> scoreExportExcelParam) {
        PerfOverallScore queryParam = scoreExportExcelParam.getQueryParam(); // ????????????
        boolean flag = StringUtil.notEmpty(queryParam.getPageSize()) && StringUtil.notEmpty(queryParam.getPageNum());
        if (flag) {
            // ????????????
            PageUtil.startPage(queryParam.getPageNum(), queryParam.getPageSize());
        }
        List<PerfOverallScore> perfOverallScoreList = this.findOverallScoreList(queryParam);
        // ???Map
        List<Map<String, Object>> mapList = BeanMapUtils.objectsToMaps(perfOverallScoreList);
        List<String> titleList = scoreExportExcelParam.getTitleList();
        List<List<Object>> results = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(mapList)) {
            mapList.forEach((map) -> {
                List<Object> list = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(titleList)) {
                    titleList.forEach((title) -> {
                        Object value = map.get(title);
                        if ("perStartMonth".equals(title)) {
                            LocalDate perStartMonth = (LocalDate) value;
                            list.add(DateUtil.localDateToStr(perStartMonth, DateUtil.DATE_FORMAT_7));
                        } else if ("perEndMonth".equals(title)) {
                            LocalDate perEndMonth = (LocalDate) value;
                            list.add(DateUtil.localDateToStr(perEndMonth, DateUtil.DATE_FORMAT_7));
                        } else {
                            if (null != value) {
                                list.add(value);
                            } else {
                                list.add("");
                            }
                        }
                    });
                }
                results.add(list);
            });
        }
        return results;
    }

    @Override
    public void exportPerfOverallScore(ExportExcelParam<PerfOverallScore> overallScoreDto, HttpServletResponse response) throws IOException {
        // ?????????????????????
        List<List<Object>> dataList = this.queryExportData(overallScoreDto);
        // ??????
        List<String> head = exportExcelParam.getMultilingualHeader(overallScoreDto, OverallScoreExportUtils.getOverallScoreTitles());
        // ?????????
        String fileName = overallScoreDto.getFileName();
        // ????????????
        EasyExcelUtil.exportStart(response, dataList, head, fileName);
    }


    /**
     * Description ????????????ID?????????????????????????????????
     *
     * @return
     * @throws
     * @Param
     * @Author wuwl18@meicloud.com
     * @Date 2020.06.19
     **/
    private PerfLevel getLevelNameByOrgIdAndScore(Long orgId, BigDecimal score) throws BaseException {
        Assert.notNull(orgId, PerfLevelConst.ORGANIZATION_ID_NOT_NULL);
        Assert.notNull(score, ScoreManScoringConst.SCORE_NOT_NULL);
        PerfLevel perfLevel = new PerfLevel();
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("ORGANIZATION_ID", orgId);
        if (null != score) {
            wrapper.le("SCORE_START", score);
            wrapper.ge("SCORE_END", score);
        }
        try {
            List<PerfLevel> perfLevelList = iPerfLevelService.list(wrapper);
            if (CollectionUtils.isNotEmpty(perfLevelList) && null != perfLevelList.get(0)) {
                perfLevel = perfLevelList.get(0);
            }
        } catch (Exception e) {
            log.error("????????????ID???????????????????????????????????????", e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }
        return perfLevel;
    }

    @Override
    public List<PerfOverallScoreDto> getPerfOverallScoreVendorId(Long vendorId) {
        List<PerfOverallScore> overallScoreList = findOverallScoreList(new PerfOverallScore().setCompanyId(vendorId));
        List<PerfOverallScoreDto> perfOverallScoreDtos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(overallScoreList)) {
            // ????????????+??????+????????????+???????????? ??????
            HashSet<String> hashSet = new HashSet<>();
            overallScoreList.forEach(perfOverallScore -> {
                if (hashSet.add(perfOverallScore.getCategoryId() +
                        perfOverallScore.getOrganizationId() +
                        DateUtil.localDateToStr(perfOverallScore.getPerStartMonth()) +
                        DateUtil.localDateToStr(perfOverallScore.getPerEndMonth()))) {
                    PerfOverallScoreDto perfOverallScoreDto = new PerfOverallScoreDto();
                    BeanCopyUtil.copyProperties(perfOverallScoreDto, perfOverallScore);
                    perfOverallScoreDtos.add(perfOverallScoreDto);
                }
            });
        }
        return perfOverallScoreDtos;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param scoreItemsId
     */
    @Override
    public void calculateAverageScore(Long scoreItemsId) {
        // ??????????????????????????????????????????????????????
        convertMutiScoringToSingleScoringData(scoreItemsId);
    }

    /**
     * ???????????????
     * @param perfOverallScores
     */
    @Override
    public void vendorConfirm(List<PerfOverallScore> perfOverallScores) {
        if (CollectionUtils.isNotEmpty(perfOverallScores)) {
            perfOverallScores.forEach(perfOverallScore -> {
                perfOverallScore.setVendorFeedbackStatus(VendorFeedbackStatus.CONFIRMED.getValue());
            });
        }
        try{
            this.updateBatchById(perfOverallScores);
        }
        catch (Exception e) {
            log.error(e.getMessage());
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        }
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param scoreItemsId
     */
    public void convertMutiScoringToSingleScoringData(Long scoreItemsId) {
        // ?????????????????????????????????
        List<ScoreManScoringV1> scoreManScoringV1List = iScoreManScoringV1Service.list(Wrappers.lambdaQuery(ScoreManScoringV1.class)
                .eq(ScoreManScoringV1::getScoreItemsId, scoreItemsId));
        Map<String, List<ScoreManScoringV1>> scoreManScoringMap = scoreManScoringV1List.stream().collect(Collectors.groupingBy(ScoreManScoringV1::getUniqueKey));
        List<PerfScoreManScoring> saveOrUpdateList = new ArrayList<>();
        scoreManScoringMap.forEach((k, v) -> {
            Assert.isTrue(CollectionUtils.isNotEmpty(v), "?????????????????????????????????");
            PerfScoreManScoring scoreManScoring = new PerfScoreManScoring();
            ScoreManScoringV1 v1 = v.get(0);
            // ????????????
            BeanUtils.copyProperties(v1, scoreManScoring);
            // ????????????id
            Long id = IdGenrator.generate();
            scoreManScoring.setScoreManScoringId(id);
            // ???????????????????????????????????????pefScore???null
            if (v.stream().allMatch(e -> Objects.isNull(e.getPefScore()))) {
                scoreManScoring.setPefScore(new BigDecimal(0));
            }
            // ??????????????????????????????
            if (v.size() > 1) {
                // ????????????pefScore???null?????????????????????????????????????????????????????????
                List<ScoreManScoringV1> filterdV1 = v.stream().filter(x -> Objects.nonNull(x.getPefScore())).collect(Collectors.toList());
                int n = filterdV1.size();
                BigDecimal totalScore = filterdV1.stream().map(ScoreManScoringV1::getPefScore).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal averageScore = totalScore.divide(BigDecimal.valueOf(n), 1, BigDecimal.ROUND_HALF_UP);
                scoreManScoring.setPefScore(averageScore);
            }
            saveOrUpdateList.add(scoreManScoring);
        });
        if (CollectionUtils.isNotEmpty(saveOrUpdateList)) {
            iPerfScoreManScoringService.saveBatch(saveOrUpdateList);
        }
    }

    /**
     * ????????????
     *
     * @param perfOverallScore
     */
    @Override
    public void uploadFile(PerfOverallScore perfOverallScore) {
        Assert.notNull(perfOverallScore.getPerfOverallScoreFileId(), "??????????????????????????????");
        Assert.isTrue(StringUtils.isNotEmpty(perfOverallScore.getPerfOverallScoreFileName()), "????????????????????????????????????");
        PerfOverallScore byId = this.getById(perfOverallScore.getOverallScoreId());
        byId.setPerfOverallScoreFileId(perfOverallScore.getPerfOverallScoreFileId())
                .setPerfOverallScoreFileName(perfOverallScore.getPerfOverallScoreFileName());
        this.updateById(byId);
    }

    /**
     * ????????????
     *
     * @param perfOverallScore
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(PerfOverallScore perfOverallScore) {
        PerfOverallScore byId = this.getById(perfOverallScore.getOverallScoreId());
        byId.setPerfOverallScoreFileId(null)
                .setPerfOverallScoreFileName(null);
        this.updateById(byId);
        fileCenterClient.delete(perfOverallScore.getPerfOverallScoreFileId());
    }

}
