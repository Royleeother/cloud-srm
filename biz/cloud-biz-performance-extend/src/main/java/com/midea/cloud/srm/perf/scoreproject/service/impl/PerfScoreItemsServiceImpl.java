package com.midea.cloud.srm.perf.scoreproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.util.StringUtil;
import com.midea.cloud.common.enums.Enable;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.flow.CbpmFormTemplateIdEnum;
import com.midea.cloud.common.enums.perf.scoreproject.ScoreItemsApproveStatusEnum;
import com.midea.cloud.common.enums.perf.scoreproject.ScoreItemsProjectStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.result.ResultCode;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.flow.WorkFlowFeign;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.flow.process.dto.CbpmRquestParamDTO;
import com.midea.cloud.srm.model.perf.scoreproject.dto.PerfScoreItemSupIndDTO;
import com.midea.cloud.srm.model.perf.scoreproject.dto.PerfScoreItemsDTO;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemManSupInd;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItems;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemsMan;
import com.midea.cloud.srm.model.perf.scoreproject.entity.PerfScoreItemsSup;
import com.midea.cloud.srm.model.perf.scoring.PerfScoreManScoring;
import com.midea.cloud.srm.model.perf.scoring.ScoreManScoringV1;
import com.midea.cloud.srm.model.perf.template.entity.PerfTemplateDimWeight;
import com.midea.cloud.srm.model.perf.template.entity.PerfTemplateIndsLine;
import com.midea.cloud.srm.model.perf.template.entity.PerfTemplateLine;
import com.midea.cloud.srm.model.rbac.permission.entity.Permission;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.perf.common.ScoreItemsConst;
import com.midea.cloud.srm.perf.scoreproject.mapper.PerfScoreItemsMapper;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemManSupIndService;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsManService;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsService;
import com.midea.cloud.srm.perf.scoreproject.service.IPerfScoreItemsSupService;
import com.midea.cloud.srm.perf.scoring.service.IPerfOverallScoreService;
import com.midea.cloud.srm.perf.scoring.service.IPerfScoreManScoringService;
import com.midea.cloud.srm.perf.scoring.service.IScoreManScoringV1Service;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateDimWeightService;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateHeaderService;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateIndsLineService;
import com.midea.cloud.srm.perf.template.service.IPerfTemplateLineService;
import feign.FeignException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ?????????????????????????????? ???????????????
 * </pre>
 *
 * @author wuwl18@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-06-05 17:33:40
 *  ????????????:
 * </pre>
 */
@Service
public class PerfScoreItemsServiceImpl extends ServiceImpl<PerfScoreItemsMapper, PerfScoreItems> implements IPerfScoreItemsService {

    /**
     * ??????????????????Service
     */
    @Resource
    private IPerfTemplateDimWeightService iPerfTemplateDimWeightService;

    /**
     * ?????????Service
     */
    @Autowired
    private IPerfTemplateHeaderService iPerfTemplateHeaderService;

    /**
     * ??????????????????Service
     */
    @Resource
    private IPerfTemplateLineService iPerfTemplateLineService;

    /**
     * ???????????????????????????Service
     */
    @Resource
    private IPerfScoreItemsSupService iPerfScoreItemsSupService;

    /**
     * ???????????????????????????Service
     */
    @Resource
    private IPerfScoreItemsManService iPerfScoreItemsManService;

    /**???????????????????????????-?????????Service*/
//    @Resource
//    private IPerfScoreItemsManSupService iPerfScoreItemsManSupService;
    /**???????????????????????????-????????????Service*/
//    @Resource
//    private IPerfScoreItemsManIndicatorService iPerfScoreItemsManIndicatorService;
    /**
     * ???????????????????????????-???????????????Service
     */
    @Resource
    private IPerfScoreItemManSupIndService iPerfScoreItemManSupIndService;
    /**
     * ?????????????????????Service
     */
    @Resource
    private IPerfScoreManScoringService iPerfScoreManScoringService;

    @Autowired
    private IScoreManScoringV1Service iScoreManScoringV1Service;

    /**
     * ????????????????????????Service
     */
    @Resource
    private IPerfOverallScoreService iPerfOverallScoreService;

    /**
     * ?????????????????????Service
     */
    @Resource
    private IPerfTemplateIndsLineService iPerfTemplateIndsLineService;

    @Resource
    private WorkFlowFeign workFlowFeign;

    @Resource
    private RbacClient rbacClient;


    public static final Map<String, String> INDICATOR_DIMENSION_MAP;

    static  {
        INDICATOR_DIMENSION_MAP = new HashMap<>();
        INDICATOR_DIMENSION_MAP.put("QUALITY", "??????");
        INDICATOR_DIMENSION_MAP.put("SERVICE", "??????");
        INDICATOR_DIMENSION_MAP.put("DELIVER", "??????");
        INDICATOR_DIMENSION_MAP.put("TECHNOLOGY", "??????");
    }



    @Override
    public List<PerfTemplateLine> getPerfTemplateLineByHeaderId(Long templateHeadId) throws BaseException {
        Assert.notNull(templateHeadId, "id????????????");
        List<PerfTemplateLine> resultPerfTemplateLineList = new ArrayList<>();
        PerfTemplateDimWeight templateDimWeight = new PerfTemplateDimWeight();
        templateDimWeight.setTemplateHeadId(templateHeadId);
        templateDimWeight.setDeleteFlag(Enable.N.toString());
        List<PerfTemplateDimWeight> templateHeaderList = null;
        try {
            templateHeaderList = iPerfTemplateDimWeightService.list(new QueryWrapper<>(templateDimWeight));
        } catch (Exception e) {
            log.error("??????????????????-??????ID??????????????????????????????????????????", e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }

        try {
            if (CollectionUtils.isNotEmpty(templateHeaderList)) {
                for (PerfTemplateDimWeight dimWeight : templateHeaderList) {
                    if (null != dimWeight && null != dimWeight.getDimWeightId()) {
                        Long dimWeightId = dimWeight.getDimWeightId();
                        PerfTemplateLine queryTemplateLine = new PerfTemplateLine();
                        queryTemplateLine.setDeleteFlag(Enable.N.toString());
                        queryTemplateLine.setTemplateDimWeightId(dimWeightId);
                        List<PerfTemplateLine> perfTemplateLineList = iPerfTemplateLineService.list(new QueryWrapper<>(queryTemplateLine));
                        if (CollectionUtils.isNotEmpty(perfTemplateLineList)) {
                            for (PerfTemplateLine line : perfTemplateLineList) {
                                if (null != line) {
                                    Long templateLineId = line.getTemplateLineId();
                                    PerfTemplateIndsLine templateIndsLine = new PerfTemplateIndsLine();
                                    templateIndsLine.setTemplateLineId(templateLineId);
                                    List<PerfTemplateIndsLine> templateIndsLineList = iPerfTemplateIndsLineService.list(new QueryWrapper<>(templateIndsLine));
                                    if (CollectionUtils.isNotEmpty(templateIndsLineList)) {
                                        line.setPerfTemplateIndsLineList(templateIndsLineList);
                                    }
                                    resultPerfTemplateLineList.add(line);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("??????????????????-????????????ID??????????????????????????????????????????", e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }
        return resultPerfTemplateLineList;
    }

    @Override
    public PerfScoreItemsDTO findScoreItemsAndSonList(Long scoreItemsId) {
        PerfScoreItemsDTO scoreItemsDTO = new PerfScoreItemsDTO();

        // ??????????????????
        PerfScoreItems scoreItems = this.getById(scoreItemsId);
        BeanUtils.copyProperties(scoreItems, scoreItemsDTO);

        // ???????????????????????????
        List<PerfScoreItemsSup> scoreItemsSupList = iPerfScoreItemsSupService.list(Wrappers.lambdaQuery(PerfScoreItemsSup.class)
                .eq(PerfScoreItemsSup::getScoreItemsId, scoreItemsId)
        );
        scoreItemsDTO.setPerfScoreItemsSupList(scoreItemsSupList);

        // ?????????????????????
        List<PerfScoreItemsMan> scoreItemsManList = iPerfScoreItemsManService.list(Wrappers.lambdaQuery(PerfScoreItemsMan.class)
                .eq(PerfScoreItemsMan::getScoreItemsId, scoreItemsId)
        );
        // ??????????????????????????????
        if (CollectionUtils.isNotEmpty(scoreItemsManList)) {
            scoreItemsManList.forEach(scoreItemsMan -> {
                List<PerfScoreItemManSupInd> scoreItemManSupIndList = iPerfScoreItemManSupIndService.list(Wrappers.lambdaQuery(PerfScoreItemManSupInd.class)
                        .eq(PerfScoreItemManSupInd::getScoreItemsManId, scoreItemsMan.getScoreItemsManId())
                );
                scoreItemsMan.setPerfScoreItemManSupIndList(scoreItemManSupIndList);
            });
        }
        scoreItemsDTO.setPerfScoreItemsManList(scoreItemsManList);

        return scoreItemsDTO;
    }

    /**
     * ????????????????????????
     *
     * @param scoreItemsDTO
     * @return
     * @throws BaseException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrUpdateScoreItemsAndSon(PerfScoreItemsDTO scoreItemsDTO) throws BaseException {
        Assert.notNull(scoreItemsDTO, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        Long scoreItemsId = scoreItemsDTO.getScoreItemsId();
        PerfScoreItems perfScoreItems = new PerfScoreItems();
        BeanUtils.copyProperties(scoreItemsDTO, perfScoreItems);
        perfScoreItems.setScorePeopleCount(Long.valueOf(scoreItemsDTO.getPerfScoreItemsManList().size()));
        if (null == scoreItemsId) {   //????????????
            scoreItemsId = IdGenrator.generate();
            perfScoreItems.setApproveStatus(ScoreItemsApproveStatusEnum.DRAFT.getValue());

        }

        //??????????????????
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        String nickName = "";   //????????????
        if (null != user) {
            nickName = user.getNickname();
        }
        /** ????????????????????????????????????**/
        boolean isUpdateScoreItems = false;
        // ????????????????????????????????????
        this.checkIfMonthOverLap(scoreItemsDTO);
        // ??????????????????/????????????????????????,?????????????????????Assert??????????????????
        this.checkIsSaveOrUpdateScoreItems(null, scoreItemsDTO);
        try {
            perfScoreItems.setScoreItemsId(scoreItemsId);
            perfScoreItems.setCreatedFullName(nickName);
            isUpdateScoreItems = super.saveOrUpdate(perfScoreItems);
        } catch (Exception e) {
            log.error("??????/??????????????????????????????????????????", e);
            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
        }

        if (isUpdateScoreItems) {
            /**???????????????????????????????????????????????????*/
            List<PerfScoreItemsMan> scoreItemsManList = scoreItemsDTO.getPerfScoreItemsManList();

            /**??????/??????????????????????????????????????????*/
            List<PerfScoreItemsSup> scoreItemsSupList = scoreItemsDTO.getPerfScoreItemsSupList();
            if (CollectionUtils.isNotEmpty(scoreItemsSupList)) {
                for (PerfScoreItemsSup scoreItemsSup : scoreItemsSupList) {
                    if (null != scoreItemsSup) {
                        Long scoreItemsSupId = scoreItemsSup.getScoreItemsSupId();
                        if (null == scoreItemsSupId) {
                            scoreItemsSupId = IdGenrator.generate();
                        }
                        scoreItemsSup.setScoreItemsSupId(scoreItemsSupId);
                        scoreItemsSup.setScoreItemsId(scoreItemsId);

                        //???????????????ID????????????Code????????????????????????????????????ID????????????????????????????????????-??????????????????ID
                        Long companyId = scoreItemsSup.getCompanyId();
                        String companyCode = scoreItemsSup.getCompanyCode();
                        if (null != companyId && StringUtil.isNotEmpty(companyCode) && CollectionUtils.isNotEmpty(scoreItemsManList)) {
                            for (PerfScoreItemsMan scoreItemsMan : scoreItemsManList) {
                                if (null != scoreItemsMan) {
                                    /**?????????????????????????????????-??????????????????**/
                                    List<PerfScoreItemManSupInd> scoreItemsManSupList = scoreItemsMan.getPerfScoreItemManSupIndList();
                                    if (CollectionUtils.isNotEmpty(scoreItemsManSupList)) {
                                        for (PerfScoreItemManSupInd scoreItemsManSup : scoreItemsManSupList) {
                                            if (null != scoreItemsManSup && null != scoreItemsManSup.getCompanyId() && companyId.longValue() ==
                                                    scoreItemsManSup.getCompanyId().longValue() && companyCode.equals(scoreItemsManSup.getCompanyCode())) {
                                                scoreItemsManSup.setScoreItemsSupId(scoreItemsSupId);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                try {
                    iPerfScoreItemsSupService.saveOrUpdateBatch(scoreItemsSupList);
                } catch (Exception e) {
                    log.error("??????/????????????????????????????????????????????????", e);
                    throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                }
            }

            /**??????/??????????????????????????????????????????*/
            if (CollectionUtils.isNotEmpty(scoreItemsManList)) {
                for (PerfScoreItemsMan scoreItemsMan : scoreItemsManList) {
                    boolean isUpdateItemsMan = false;   //???????????????????????????????????????????????????false
                    if (null != scoreItemsMan) {
                        Long scoreItemsManId = scoreItemsMan.getScoreItemsManId();
                        if (null == scoreItemsManId) {
                            scoreItemsManId = IdGenrator.generate();
                        } else {
                            isUpdateItemsMan = true;
                        }
                        scoreItemsMan.setScoreItemsManId(scoreItemsManId);
                        scoreItemsMan.setScoreItemsId(scoreItemsId);
                        boolean isUpdateScoreItemsMan = false;
                        try {
                            isUpdateScoreItemsMan = iPerfScoreItemsManService.saveOrUpdate(scoreItemsMan);
                        } catch (Exception e) {
                            log.error("??????/????????????????????????????????????????????????", e);
                            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                        }
                        if (isUpdateScoreItemsMan) {
                            /**??????/?????????????????????????????????-??????????????????**/
                            //????????????????????????????????????????????????????????????????????????????????????????????????
                            if (null != scoreItemsManId) {
                                PerfScoreItemManSupInd queryScoreItemsManSupInd = new PerfScoreItemManSupInd();
                                queryScoreItemsManSupInd.setScoreItemsManId(scoreItemsManId);
                                List<PerfScoreItemManSupInd> delScoreItemManSupIndList = iPerfScoreItemManSupIndService.list(new QueryWrapper<>(queryScoreItemsManSupInd));
                                List<Long> scoreItemsManSupIndIds = new ArrayList<>();    //??????????????????????????????????????????-???????????????ID??????
                                if (CollectionUtils.isNotEmpty(delScoreItemManSupIndList)) {
                                    for (PerfScoreItemManSupInd manSupInd : delScoreItemManSupIndList) {
                                        if (null != manSupInd && null != manSupInd.getScoreItemManSupIndId()) {
                                            scoreItemsManSupIndIds.add(manSupInd.getScoreItemManSupIndId());
                                        }
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(scoreItemsManSupIndIds)) {
                                    try {
                                        iPerfScoreItemManSupIndService.removeByIds(scoreItemsManSupIndIds);
                                    } catch (Exception e) {
                                        log.error("?????????????????????????????????,?????????????????????????????????-??????????????????????????????", e);
                                        throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                    }
                                }

                            }
                            List<PerfScoreItemManSupInd> scoreItemsManSupList = scoreItemsMan.getPerfScoreItemManSupIndList();
                            if (CollectionUtils.isNotEmpty(scoreItemsManSupList)) {
                                for (PerfScoreItemManSupInd scoreItemsManSup : scoreItemsManSupList) {
                                    if (null != scoreItemsManSup) {
                                        Long scoreItemManSupIndId = scoreItemsManSup.getScoreItemManSupIndId();
                                        Long scoreItemsSupId = scoreItemsManSup.getScoreItemsSupId();
                                        if (null == scoreItemManSupIndId) {
                                            scoreItemManSupIndId = IdGenrator.generate();
                                            //?????????????????????????????????????????????????????????
                                            if (StringUtils.isBlank(scoreItemsManSup.getEnableFlag())) {
                                                scoreItemsManSup.setEnableFlag(Enable.N.toString());
                                            }
                                        }

                                        if (null == scoreItemsSupId) {
                                            scoreItemsSupId = IdGenrator.generate();
                                        }
                                        scoreItemsManSup.setCreatedFullName(nickName);
                                        scoreItemsManSup.setScoreItemManSupIndId(scoreItemManSupIndId);
                                        scoreItemsManSup.setScoreItemsManId(scoreItemsManId);
                                        scoreItemsManSup.setScoreItemsId(scoreItemsId);
                                        scoreItemsManSup.setScoreItemsSupId(scoreItemsSupId);
                                    }
                                }

                                try {
                                    iPerfScoreItemManSupIndService.saveOrUpdateBatch(scoreItemsManSupList);
                                } catch (Exception e) {
                                    log.error("??????/?????????????????????????????????-????????????????????????", e);
                                    throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                }
                            }

                            /**??????/?????????????????????????????????-?????????????????????*/
/*                            List<PerfScoreItemsManIndicator> itemsManIndicatorList = scoreItemsMan.getPerfScoreItemsManIndicatorList();
                            if(CollectionUtils.isNotEmpty(itemsManIndicatorList)){
                                for(PerfScoreItemsManIndicator itemsManIndicator : itemsManIndicatorList){
                                    if(null != itemsManIndicator){
                                        Long itemsManIndicatorId = itemsManIndicator.getScoreItemsManIndicatorId();
                                        if(null == itemsManIndicatorId){
                                            itemsManIndicatorId = IdGenrator.generate();
                                            //?????????????????????????????????????????????????????????
                                            if(StringUtils.isBlank(itemsManIndicator.getEnableFlag())){
                                                itemsManIndicator.setEnableFlag(Enable.N.toString());
                                            }
                                        }
                                        itemsManIndicator.setCreatedFullName(nickName);
                                        itemsManIndicator.setScoreItemsManIndicatorId(itemsManIndicatorId);
                                        itemsManIndicator.setScoreItemsManId(scoreItemsManId);
                                        itemsManIndicator.setScoreItemsId(scoreItemsId);
                                    }
                                }

                                try{
                                    iPerfScoreItemsManIndicatorService.saveOrUpdateBatch(itemsManIndicatorList);
                                }catch (Exception e){
                                    log.error("??????/?????????????????????????????????-????????????????????????",e);
                                    throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                }
                            }*/
                        }
                    }
                }
            }
        }
        return String.valueOf(scoreItemsId);
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param scoreItemsDTO
     * @throws BaseException
     * @author xiexh12@meicloud.com
     */
    @Override
    public Long saveOrUpdatePerfScoreItems(PerfScoreItemsDTO scoreItemsDTO) throws BaseException {
        Assert.notNull(scoreItemsDTO, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        // ????????????????????????????????????
        this.checkIfMonthOverLap(scoreItemsDTO);
        // ??????????????????????????????
        this.checkIfProjectNameDuplicate(scoreItemsDTO);
        // ?????????????????????????????????
        this.checkScoreItemsSup(scoreItemsDTO.getPerfScoreItemsSupList());
        // ???????????????????????????
        this.checkScoreItemsMan(scoreItemsDTO.getPerfScoreItemsManList());

        Long scoreItemsId = scoreItemsDTO.getScoreItemsId();
        PerfScoreItems perfScoreItems = new PerfScoreItems();
        BeanUtils.copyProperties(scoreItemsDTO, perfScoreItems);
        perfScoreItems.setScorePeopleCount(Long.valueOf(scoreItemsDTO.getPerfScoreItemsManList().size()));
        // ????????????
        if (null == scoreItemsId) {
            scoreItemsId = IdGenrator.generate();
            perfScoreItems.setApproveStatus(ScoreItemsApproveStatusEnum.DRAFT.getValue());
        }

        //??????????????????
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        String nickName = "";
        // ????????????
        if (Objects.nonNull(user)) {
            nickName = user.getNickname();
        }
        /** ????????????????????????????????????**/
        boolean isUpdateScoreItems = false;
        try {
            perfScoreItems.setScoreItemsId(scoreItemsId);
            perfScoreItems.setCreatedFullName(nickName);
            isUpdateScoreItems = super.saveOrUpdate(perfScoreItems);
        } catch (Exception e) {
            log.error("??????/??????????????????????????????????????????", e);
            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
        }

        if (isUpdateScoreItems) {
            /**???????????????????????????????????????????????????*/
            List<PerfScoreItemsMan> scoreItemsManList = scoreItemsDTO.getPerfScoreItemsManList();

            /**??????/??????????????????????????????????????????*/
            List<PerfScoreItemsSup> scoreItemsSupList = scoreItemsDTO.getPerfScoreItemsSupList();
            if (CollectionUtils.isNotEmpty(scoreItemsSupList)) {
                for (PerfScoreItemsSup scoreItemsSup : scoreItemsSupList) {
                    if (null != scoreItemsSup) {
                        Long scoreItemsSupId = scoreItemsSup.getScoreItemsSupId();
                        if (null == scoreItemsSupId) {
                            scoreItemsSupId = IdGenrator.generate();
                        }
                        scoreItemsSup.setScoreItemsSupId(scoreItemsSupId);
                        scoreItemsSup.setScoreItemsId(scoreItemsId);

                        // ???????????????ID????????????Code????????????????????????????????????ID????????????????????????????????????-??????????????????ID
                        Long companyId = scoreItemsSup.getCompanyId();
                        String companyCode = scoreItemsSup.getCompanyCode();
                        if (null != companyId && StringUtil.isNotEmpty(companyCode) && CollectionUtils.isNotEmpty(scoreItemsManList)) {
                            for (PerfScoreItemsMan scoreItemsMan : scoreItemsManList) {
                                if (null != scoreItemsMan) {
                                    /**?????????????????????????????????-??????????????????**/
                                    List<PerfScoreItemManSupInd> scoreItemsManSupList = scoreItemsMan.getPerfScoreItemManSupIndList();
                                    if (CollectionUtils.isNotEmpty(scoreItemsManSupList)) {
                                        for (PerfScoreItemManSupInd scoreItemsManSup : scoreItemsManSupList) {
                                            if (null != scoreItemsManSup && null != scoreItemsManSup.getCompanyId() && companyId.longValue() ==
                                                    scoreItemsManSup.getCompanyId().longValue() && companyCode.equals(scoreItemsManSup.getCompanyCode())) {
                                                scoreItemsManSup.setScoreItemsSupId(scoreItemsSupId);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                try {
                    iPerfScoreItemsSupService.saveOrUpdateBatch(scoreItemsSupList);
                } catch (Exception e) {
                    log.error("??????/????????????????????????????????????????????????", e);
                    throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                }
            }

            /**??????/??????????????????????????????????????????*/
            if (CollectionUtils.isNotEmpty(scoreItemsManList)) {
                for (PerfScoreItemsMan scoreItemsMan : scoreItemsManList) {
                    // ???????????????????????????????????????????????????false
                    boolean isUpdateItemsMan = false;
                    if (null != scoreItemsMan) {
                        Long scoreItemsManId = scoreItemsMan.getScoreItemsManId();
                        if (Objects.isNull(scoreItemsManId)) {
                            scoreItemsManId = IdGenrator.generate();
                        } else {
                            isUpdateItemsMan = true;
                        }
                        scoreItemsMan.setScoreItemsManId(scoreItemsManId);
                        scoreItemsMan.setScoreItemsId(scoreItemsId);
                        boolean isUpdateScoreItemsMan = false;
                        try {
                            isUpdateScoreItemsMan = iPerfScoreItemsManService.saveOrUpdate(scoreItemsMan);
                        } catch (Exception e) {
                            log.error("??????/????????????????????????????????????????????????", e);
                            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                        }
                        if (isUpdateScoreItemsMan) {
                            /**??????/?????????????????????????????????-??????????????????**/
                            //????????????????????????????????????????????????????????????????????????????????????????????????
                            if (Objects.nonNull(scoreItemsManId)) {
                                PerfScoreItemManSupInd queryScoreItemsManSupInd = new PerfScoreItemManSupInd();
                                queryScoreItemsManSupInd.setScoreItemsManId(scoreItemsManId);
                                List<PerfScoreItemManSupInd> delScoreItemManSupIndList = iPerfScoreItemManSupIndService.list(new QueryWrapper<>(queryScoreItemsManSupInd));
                                // ??????????????????????????????????????????-???????????????ID??????
                                List<Long> scoreItemsManSupIndIds = new ArrayList<>();
                                if (CollectionUtils.isNotEmpty(delScoreItemManSupIndList)) {
                                    for (PerfScoreItemManSupInd manSupInd : delScoreItemManSupIndList) {
                                        if (null != manSupInd && null != manSupInd.getScoreItemManSupIndId()) {
                                            scoreItemsManSupIndIds.add(manSupInd.getScoreItemManSupIndId());
                                        }
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(scoreItemsManSupIndIds)) {
                                    try {
                                        iPerfScoreItemManSupIndService.removeByIds(scoreItemsManSupIndIds);
                                    } catch (Exception e) {
                                        log.error("?????????????????????????????????,?????????????????????????????????-??????????????????????????????", e);
                                        throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                    }
                                }

                            }
                            List<PerfScoreItemManSupInd> scoreItemsManSupList = scoreItemsMan.getPerfScoreItemManSupIndList();
                            if (CollectionUtils.isNotEmpty(scoreItemsManSupList)) {
                                for (PerfScoreItemManSupInd scoreItemsManSup : scoreItemsManSupList) {
                                    if (null != scoreItemsManSup) {
                                        Long scoreItemManSupIndId = scoreItemsManSup.getScoreItemManSupIndId();
                                        Long scoreItemsSupId = scoreItemsManSup.getScoreItemsSupId();
                                        if (null == scoreItemManSupIndId) {
                                            scoreItemManSupIndId = IdGenrator.generate();
                                            // ?????????????????????????????????????????????????????????
                                            if (StringUtils.isBlank(scoreItemsManSup.getEnableFlag())) {
                                                scoreItemsManSup.setEnableFlag(Enable.N.toString());
                                            }
                                        }

                                        if (null == scoreItemsSupId) {
                                            scoreItemsSupId = IdGenrator.generate();
                                        }
                                        scoreItemsManSup.setCreatedFullName(nickName);
                                        scoreItemsManSup.setScoreItemManSupIndId(scoreItemManSupIndId);
                                        scoreItemsManSup.setScoreItemsManId(scoreItemsManId);
                                        scoreItemsManSup.setScoreItemsId(scoreItemsId);
                                        scoreItemsManSup.setScoreItemsSupId(scoreItemsSupId);
                                    }
                                }

                                try {
                                    iPerfScoreItemManSupIndService.saveOrUpdateBatch(scoreItemsManSupList);
                                } catch (Exception e) {
                                    log.error("??????/?????????????????????????????????-????????????????????????", e);
                                    throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }
        return scoreItemsId;
    }

    /**
     * ???????????????
     *
     * @param scoreItems
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String notifyScorers(PerfScoreItems scoreItems) {
        Assert.notNull(scoreItems, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        String result = ResultCode.OPERATION_FAILED.getMessage();
        // ???????????????id
        Long scoreItemsId = scoreItems.getScoreItemsId();
        Assert.notNull(scoreItemsId, "???????????????id???????????????");
        String projectStatus = scoreItems.getProjectStatus();
        PerfScoreItems oldScoreItems = this.getById(scoreItemsId);
        if (Objects.nonNull(oldScoreItems)) {
            String oldProjectStatus = oldScoreItems.getProjectStatus();
            // ?????????????????????
            if (ScoreItemsProjectStatusEnum.SCORE_NOTIFIED.getValue().equals(projectStatus)) {
                if (!ScoreItemsProjectStatusEnum.SCORE_DRAFT.getValue().equals(oldProjectStatus)) {
                    throw new BaseException(LocaleHandler.getLocaleMsg(ScoreItemsConst.SCORE_ITEMS_NOTIFY_SCORERS));
                }
            }
            int isUpdateCount = 0;
            // ??????????????????????????????
            try {
                UpdateWrapper<PerfScoreItems> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("SCORE_ITEMS_ID", scoreItems.getScoreItemsId())
                        .set("PROJECT_STATUS",scoreItems.getProjectStatus());
                this.update(updateWrapper);
            } catch (Exception e) {
                log.error("??????????????????-??????????????????????????????: ", e);
                throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
            }
            // ??????????????????/????????????????????????????????????????????????Assert??????????????????
            //this.checkIsSaveOrUpdateScoreItems(scoreItemsId, null);
            // ???????????????????????? ?????????????????????????????????????????????????????????????????????????????????
            this.checkIfAllIndicatorsAllocateNotifiers(scoreItemsId);
            // ?????????????????????????????????
            result = iScoreManScoringV1Service.saveScoreManScoringByScoreItemsId(scoreItemsId);
        }
        return result;
    }

    /**
     * ???????????????????????? ????????????????????????????????????
     *
     * @param scoreItemsId
     */
    public void checkIfAllIndicatorsAllocateNotifiers(Long scoreItemsId) {

        PerfScoreItems scoreItems = this.getById(scoreItemsId);
        Long templateHeadId = scoreItems.getTemplateHeadId();

        // ???????????????????????????
        List<PerfScoreItemsSup> scoreItemsSupList = iPerfScoreItemsSupService.list(Wrappers.lambdaQuery(PerfScoreItemsSup.class)
                .eq(PerfScoreItemsSup::getScoreItemsId, scoreItemsId));
        // ?????????????????????????????????
        if (CollectionUtils.isEmpty(scoreItemsSupList)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????"));
        }

        // ?????????????????????
        List<PerfScoreItemsMan> scoreItemsManList = iPerfScoreItemsManService.list(Wrappers.lambdaQuery(PerfScoreItemsMan.class)
                .eq(PerfScoreItemsMan::getScoreItemsId, scoreItemsId));
        // ???????????????????????????
        if (CollectionUtils.isEmpty(scoreItemsManList)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????"));
        }

        // ?????????????????????????????????????????????
        List<PerfScoreItemManSupInd> scoreItemsSupIndicatorList = iPerfScoreItemManSupIndService.list(Wrappers.lambdaQuery(PerfScoreItemManSupInd.class)
                .eq(PerfScoreItemManSupInd::getScoreItemsId, scoreItemsId));
        if (CollectionUtils.isEmpty(scoreItemsSupIndicatorList)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????????????????"));
        }

        List<PerfTemplateLine> perfTemplateLines = iPerfTemplateHeaderService.listTemplateLinesByTemplateHeaderId(templateHeadId);

        // ???????????????????????????????????????????????????????????????????????????
        Map<String, String> indicatorDimensionMap = new HashMap<>();
        indicatorDimensionMap.put("QUALITY", "??????");
        indicatorDimensionMap.put("SERVICE", "??????");
        indicatorDimensionMap.put("DELIVER", "??????");
        indicatorDimensionMap.put("TECHNOLOGY", "??????");

        perfTemplateLines.forEach(x -> {
            String indicatorDimension = x.getIndicatorDimension();
            String indicatorName = x.getIndicatorName();
            QueryWrapper<PerfScoreItemManSupInd> countWrapper = new QueryWrapper<>();
            countWrapper.eq("SCORE_ITEMS_ID", scoreItemsId);
            countWrapper.eq("INDICATOR_DIMENSION", indicatorDimension);
            countWrapper.eq("INDICATOR_NAME", indicatorName);
            if (iPerfScoreItemManSupIndService.count(countWrapper) == 0) {
                StringBuffer sb = new StringBuffer();
                sb.append("??????????????????????????????????????????????????????[").append(indicatorDimensionMap.get(indicatorDimension))
                        .append("]??????????????????[").append(indicatorName).append("]???????????????????????????????????????");
                throw new BaseException(LocaleHandler.getLocaleMsg(sb.toString()));
            }
        });
    }

    /**
     * ????????????????????????
     *
     * @param scoreItems
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateScoreItems(PerfScoreItems scoreItems) {
        // ???????????????????????????
        Assert.notNull(scoreItems, ResultCode.MISSING_SERVLET_REQUEST_PARAMETER.getMessage());
        String result = ResultCode.OPERATION_FAILED.getMessage();
        Long scoreItemsId = scoreItems.getScoreItemsId();
        Assert.notNull(scoreItemsId, "????????????id????????????");

        // ????????????????????????????????????????????????????????????????????????
        Map<String, String> connotCalculateStatusMap = new HashMap<>();
        connotCalculateStatusMap.put(ScoreItemsProjectStatusEnum.OBSOLETE.getValue(), ScoreItemsProjectStatusEnum.OBSOLETE.getName());
        connotCalculateStatusMap.put(ScoreItemsProjectStatusEnum.SCORE_CALCULATED.getValue(), ScoreItemsProjectStatusEnum.SCORE_CALCULATED.getName());
        connotCalculateStatusMap.put(ScoreItemsProjectStatusEnum.RESULT_NO_PUBLISHED.getValue(), ScoreItemsProjectStatusEnum.RESULT_NO_PUBLISHED.getName());
        connotCalculateStatusMap.put(ScoreItemsProjectStatusEnum.RESULT_PUBLISHED.getValue(), ScoreItemsProjectStatusEnum.RESULT_PUBLISHED.getName());

        String projectStatus = scoreItems.getProjectStatus();
        if (!connotCalculateStatusMap.containsKey(projectStatus)) {
            throw new BaseException(ScoreItemsConst.SCORE_ITEMS_CAN_NOT_OPERATION);
        }

        LocalDate nowLocalDate = null;
        try {
            //???????????????????????????yyyy-MM-dd
            Date nowDate = DateUtil.getDate(new Date(), DateUtil.DATE_FORMAT_10);
            nowLocalDate = DateUtil.dateToLocalDate(nowDate);
        } catch (Exception e) {
            log.error("????????????????????????????????????-???????????????????????????: ", e);
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }

        PerfScoreItems oldScoreItems = new PerfScoreItems();
        // ??????ID??????????????????????????????
        oldScoreItems = this.getById(scoreItemsId);

        if (Objects.nonNull(oldScoreItems)) {
            String oldProjectStatus = oldScoreItems.getProjectStatus();
            LocalDate scoreStartTime = oldScoreItems.getScoreStartTime();
//            LocalDate scoreEndTime = oldScoreItems.getScoreEndTime();

            if (ScoreItemsProjectStatusEnum.OBSOLETE.getValue().equals(projectStatus) && ScoreItemsProjectStatusEnum.OBSOLETE.getValue().equals(oldProjectStatus)) {
                // ????????????(???????????????????????????????????????????????????)
                Assert.isTrue(false, ScoreItemsConst.SCORE_ITEMS_ABANDON);
                Assert.isTrue(ScoreItemsProjectStatusEnum.SCORE_DRAFT.getValue().equals(projectStatus), ScoreItemsConst.IS_NOT_OBSOLETE);
            } else if (ScoreItemsProjectStatusEnum.SCORE_CALCULATED.getValue().equals(projectStatus)) {
                // ????????????(??????????????????????????????????????????????????????)
                if (!ScoreItemsProjectStatusEnum.SCORE_NOTIFIED.getValue().equals(oldProjectStatus)) {
                    Assert.isTrue(false, "?????????????????????????????????????????????????????????");
                } else if (nowLocalDate.isBefore(scoreStartTime)) {
                    Assert.isTrue(false, ScoreItemsConst.SCORE_ITEMS_CALCULATE_START_TIME);
                }/*else if(nowLocalDate.isAfter(scoreEndTime)){     //??????????????????????????????
                    Assert.isTrue(false, ScoreItemsConst.SCORE_ITEMS_CALCULATE_END_TIME);
                }*/

                // ??????????????????????????????????????????????????????
                convertMutiScoringToSingleScoringData(scoreItemsId);

                // ??????????????????
                boolean isCalculated = iPerfOverallScoreService.generateScoreInfo(scoreItemsId);
                // ?????????????????????????????????????????????????????? ???????????????????????????
                iPerfOverallScoreService.publishScoreItemsUpdateStatus(scoreItemsId);
                if (!isCalculated) {
                    throw new BaseException("???????????????????????????");
                }

                // ????????????????????????ID??????????????????????????????????????????????????????
                iPerfScoreManScoringService.updateScoreManScoringStatus(scoreItemsId, ScoreItemsProjectStatusEnum.SCORE_CALCULATED.getValue());
                // ????????????????????????????????? ??????????????????????????????
                iScoreManScoringV1Service.lockIfEndScored(scoreItemsId);
            } else if (ScoreItemsProjectStatusEnum.RESULT_NO_PUBLISHED.getValue().equals(projectStatus)) {
                //??????????????????(????????????????????????????????????)
                if (!ScoreItemsProjectStatusEnum.SCORE_CALCULATED.getValue().equals(oldProjectStatus)) {
                    Assert.isTrue(false, ScoreItemsConst.SCORE_ITEMS_SUBMIT_PROCESS);
                }
                //???????????????????????????????????????????????????
                scoreItems.setApproveStatus(ScoreItemsApproveStatusEnum.APPROVED.getValue());
            } else if (ScoreItemsProjectStatusEnum.RESULT_PUBLISHED.getValue().equals(projectStatus)) {    //????????????
                if (!ScoreItemsProjectStatusEnum.RESULT_NO_PUBLISHED.getValue().equals(oldProjectStatus)) {
                    Assert.isTrue(false, ScoreItemsConst.SCORE_ITEMS_PUBLISH);
                }

                // ??????????????????????????????????????????????????????
                iPerfOverallScoreService.publishScoreItemsUpdateStatus(scoreItemsId);
            }
        }
        try {
            int isUpdateCount = getBaseMapper().updateById(scoreItems);
            if (0 < isUpdateCount) {
                result = ResultCode.SUCCESS.getMessage();
            }
        } catch (Exception e) {
            log.error("?????????????????????????????????????????????: ", e);
            throw new BaseException(ResultCode.OPERATION_FAILED.getMessage());
        }
        return result;
    }

    @Override
    public List<PerfScoreItems> findCalculatedScoreItemsList() throws BaseException {
        try {
            QueryWrapper queryWrapper = new QueryWrapper();
            List<String> projectStatusList = new ArrayList<>();
            projectStatusList.add(ScoreItemsProjectStatusEnum.SCORE_CALCULATED.getValue());
            projectStatusList.add(ScoreItemsProjectStatusEnum.RESULT_NO_PUBLISHED.getValue());
            projectStatusList.add(ScoreItemsProjectStatusEnum.RESULT_PUBLISHED.getValue());
            queryWrapper.in("PROJECT_STATUS", projectStatusList);
            return super.list(queryWrapper);
        } catch (Exception e) {
            log.error("??????????????????????????????????????????????????????");
            throw new BaseException(ResultCode.UNKNOWN_ERROR.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String delScoreItemsAndSon(Long scoreItemsId) throws BaseException {
        Assert.notNull(scoreItemsId, "id????????????");
        String result = ResultCode.OPERATION_FAILED.getMessage();

        /**????????????????????????ID????????????????????????*/
        PerfScoreItems scoreItems = getBaseMapper().selectById(scoreItemsId);
        if (null != scoreItems) {

            /**????????????????????????ID??????????????????????????????ID??????*/
            List<Long> delScoreItemsSupIds = new ArrayList<>(); //????????????????????????????????????ID??????
            PerfScoreItemsSup queryScoreItemsSup = new PerfScoreItemsSup();
            queryScoreItemsSup.setScoreItemsId(scoreItemsId);
            List<PerfScoreItemsSup> scoreItemsSupList = iPerfScoreItemsSupService.list(new QueryWrapper<>(queryScoreItemsSup));
            if (CollectionUtils.isNotEmpty(scoreItemsSupList)) {
                for (PerfScoreItemsSup scoreItemsSup : scoreItemsSupList) {
                    if (null != scoreItemsSup && null != scoreItemsSup.getScoreItemsSupId()) {
                        delScoreItemsSupIds.add(scoreItemsSup.getScoreItemsSupId());
                    }
                }
            }

            /**????????????????????????ID??????????????????????????????ID??????*/
            List<Long> delScoreItemsManIds = new ArrayList<>(); //????????????????????????????????????ID??????
            List<Long> delScoreItemsManSupIds = new ArrayList<>(); //????????????????????????????????????-?????????ID??????
            List<Long> delScoreItemsManIndicatorIds = new ArrayList<>(); //????????????????????????????????????-?????????ID??????
            PerfScoreItemsMan queryScoreItemsMan = new PerfScoreItemsMan();
            queryScoreItemsMan.setScoreItemsId(scoreItemsId);
            List<PerfScoreItemsMan> scoreItemsManList = iPerfScoreItemsManService.list(new QueryWrapper<>(queryScoreItemsMan));
            if (CollectionUtils.isNotEmpty(scoreItemsManList)) {
                for (PerfScoreItemsMan scoreItemsMan : scoreItemsManList) {
                    if (null != scoreItemsMan && null != scoreItemsMan.getScoreItemsManId()) {
                        Long scoreItemsManId = scoreItemsMan.getScoreItemsManId();
                        delScoreItemsManIds.add(scoreItemsManId);

                        PerfScoreItemManSupInd queryScoreItemsManSup = new PerfScoreItemManSupInd();
                        queryScoreItemsManSup.setScoreItemsManId(scoreItemsManId);
                        List<PerfScoreItemManSupInd> scoreItemsManSupList = iPerfScoreItemManSupIndService.list(new QueryWrapper<>(queryScoreItemsManSup));
                        if (CollectionUtils.isNotEmpty(scoreItemsManSupList)) {
                            for (PerfScoreItemManSupInd scoreItemsManSup : scoreItemsManSupList) {
                                if (null != scoreItemsManSup && null != scoreItemsManSup.getScoreItemManSupIndId()) {
                                    delScoreItemsManSupIds.add(scoreItemsManSup.getScoreItemManSupIndId());
                                }
                            }
                        }

/*                        PerfScoreItemsManIndicator queryScoreItemsManIndicator = new PerfScoreItemsManIndicator();
                        queryScoreItemsManIndicator.setScoreItemsManId(scoreItemsManId);
                        List<PerfScoreItemsManIndicator> itemsManIndicatorList = iPerfScoreItemsManIndicatorService.list(new QueryWrapper<>(queryScoreItemsManIndicator));
                        if(CollectionUtils.isNotEmpty(itemsManIndicatorList)){
                            for(PerfScoreItemsManIndicator itemsManIndicator : itemsManIndicatorList){
                                if(null != itemsManIndicator && null != itemsManIndicator.getScoreItemsManIndicatorId()){
                                    delScoreItemsManIndicatorIds.add(itemsManIndicator.getScoreItemsManIndicatorId());
                                }
                            }
                        }*/

                    }
                }
            }

/*            if(CollectionUtils.isNotEmpty(delScoreItemsManIndicatorIds)){
                try {
                    iPerfScoreItemsManIndicatorService.removeByIds(delScoreItemsManIndicatorIds);
                }catch (Exception e){
                    log.error(ScoreItemsConst.DELETE_SCORE_ITEMS_MAN_INDICATOR_ERROR);
                    throw new BaseException(ScoreItemsConst.DELETE_SCORE_ITEMS_MAN_INDICATOR_ERROR);
                }
            }*/
            if (CollectionUtils.isNotEmpty(delScoreItemsManSupIds)) {
                try {
                    iPerfScoreItemManSupIndService.removeByIds(delScoreItemsManSupIds);
                } catch (Exception e) {
                    log.error(ScoreItemsConst.DELETE_SCORE_ITEMS_MAN_SUP_ERROR);
                    throw new BaseException(ScoreItemsConst.DELETE_SCORE_ITEMS_MAN_SUP_ERROR);
                }
            }
            if (CollectionUtils.isNotEmpty(delScoreItemsManIds)) {
                try {
                    iPerfScoreItemsManService.removeByIds(delScoreItemsManIds);
                } catch (Exception e) {
                    log.error(ScoreItemsConst.DELETE_SCORE_ITEMS_MAN_ERROR);
                    throw new BaseException(ScoreItemsConst.DELETE_SCORE_ITEMS_MAN_ERROR);
                }
            }
            if (CollectionUtils.isNotEmpty(delScoreItemsSupIds)) {
                try {
                    iPerfScoreItemsSupService.removeByIds(delScoreItemsSupIds);
                } catch (Exception e) {
                    log.error(ScoreItemsConst.DELETE_SCORE_ITEMS_SUP_ERROR);
                    throw new BaseException(ScoreItemsConst.DELETE_SCORE_ITEMS_SUP_ERROR);
                }
            }
            if (null != scoreItemsId) {
                try {
                    getBaseMapper().deleteById(scoreItemsId);
                } catch (Exception e) {
                    log.error(ScoreItemsConst.DELETE_SCORE_ITEMS_ERROR);
                    throw new BaseException(ScoreItemsConst.DELETE_SCORE_ITEMS_ERROR);
                }
            }
        }
        result = ResultCode.SUCCESS.getMessage();
        return result;
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

            // ??????????????????????????????
            // ????????????pefScore???null?????????????????????????????????????????????????????????
            List<ScoreManScoringV1> filterdV1 = v.stream().filter(x -> Objects.nonNull(x.getPefScore())).collect(Collectors.toList());

            // ?????????????????????????????????????????????????????????pefScore???null??????????????????0
            if (v.stream().allMatch(e -> Objects.isNull(e.getPefScore()))) {
                scoreManScoring.setPefScore(new BigDecimal(0));
                scoreManScoring.setScore(new BigDecimal(0));
            } else if (filterdV1.size() > 1) {
                int n = filterdV1.size();
                BigDecimal totalScore = filterdV1.stream().map(ScoreManScoringV1::getPefScore).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal averageScore = totalScore.divide(BigDecimal.valueOf(n), 1, BigDecimal.ROUND_HALF_UP);
                scoreManScoring.setPefScore(averageScore);
                scoreManScoring.setScore(averageScore);
            } else if (filterdV1.size() == 1) {
                ScoreManScoringV1 v2 = filterdV1.get(0);
                BeanUtils.copyProperties(v2, scoreManScoring);
                // ????????????id
                scoreManScoring.setScoreManScoringId(IdGenrator.generate());
            }

            saveOrUpdateList.add(scoreManScoring);
        });
        if (CollectionUtils.isNotEmpty(saveOrUpdateList)) {
            iPerfScoreManScoringService.saveBatch(saveOrUpdateList);
        }
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param scoreManScoringsList
     * @return
     * @throws BaseException
     */
    @Override
    @Transactional(rollbackFor = BaseException.class)
    public String updateScoreItemsScorePeople(List<PerfScoreManScoring> scoreManScoringsList) throws BaseException {
        String result = ResultCode.OPERATION_FAILED.getMessage();
        // ???????????? ?????????????????????????????????????????????+1
        List<Long> scoreItemsIds = scoreManScoringsList.stream().map(PerfScoreManScoring::getScoreItemsId).filter(x -> Objects.nonNull(x)).collect(Collectors.toList());

        Map<Long, List<PerfScoreManScoring>> perfScoreManScoringMap = CollectionUtils.isEmpty(scoreItemsIds) ? Collections.emptyMap() :
                iPerfScoreManScoringService.list(Wrappers.lambdaQuery(PerfScoreManScoring.class).in(PerfScoreManScoring::getScoreItemsId, scoreItemsIds))
                        .stream().collect(Collectors.groupingBy(PerfScoreManScoring::getScoreItemsId));

        if (MapUtils.isNotEmpty(perfScoreManScoringMap)) {
            perfScoreManScoringMap.forEach((scoreItemsId, scoreManScoringList) -> {

                PerfScoreItems updateScoreItems = this.getById(scoreItemsId);
                // ?????????????????????
                Map<String, List<PerfScoreManScoring>> scoreManScoringByUserMap = scoreManScoringList.stream()
                        .collect(Collectors.groupingBy(PerfScoreManScoring::getScoreUserName));
                // ?????????
                AtomicInteger scoredCount = new AtomicInteger(0);
                if (MapUtils.isNotEmpty(scoreManScoringByUserMap)) {
                    // ?????????????????????????????????????????????????????????????????????????????????????????????????????????
                    scoreManScoringByUserMap.forEach((userName, scoringList) -> {
                        if (scoringList.stream().allMatch(e -> Objects.equals(YesOrNo.YES.getValue(), e.getIfScored()))) {
                            scoredCount.addAndGet(1);
                        }
                    });
                }

                // ??????????????????????????????????????????
                List<PerfScoreItemsMan> scoreItemsManList = CollectionUtils.isEmpty(scoreItemsIds) ? Collections.emptyList() :
                        iPerfScoreItemsManService.list(Wrappers.lambdaQuery(PerfScoreItemsMan.class).eq(PerfScoreItemsMan::getScoreItemsId, scoreItemsId));
                // ??????????????????????????????????????????
                int count = scoredCount.get();
                if (count <= scoreItemsManList.size()) {
                    updateScoreItems.setScorePeople(Long.valueOf(count));
                    this.updateById(updateScoreItems);
                }

            });
        }

        result = ResultCode.SUCCESS.getMessage();
        return result;
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param scoreManScoringV1List
     * @return
     * @throws BaseException
     */
    @Override
    @Transactional(rollbackFor = BaseException.class)
    public String updateScoreItemsAndScorePeople(List<ScoreManScoringV1> scoreManScoringV1List) throws BaseException {
        String result = ResultCode.OPERATION_FAILED.getMessage();
        // ???????????? ?????????????????????????????????????????????+1
        List<Long> scoreItemsIds = scoreManScoringV1List.stream().map(ScoreManScoringV1::getScoreItemsId).filter(x -> Objects.nonNull(x)).collect(Collectors.toList());

        Map<Long, List<ScoreManScoringV1>> perfScoreManScoringMap = CollectionUtils.isEmpty(scoreItemsIds) ? Collections.emptyMap() :
                iScoreManScoringV1Service.list(Wrappers.lambdaQuery(ScoreManScoringV1.class).in(ScoreManScoringV1::getScoreItemsId, scoreItemsIds))
                        .stream().collect(Collectors.groupingBy(ScoreManScoringV1::getScoreItemsId));

        if (MapUtils.isNotEmpty(perfScoreManScoringMap)) {
            perfScoreManScoringMap.forEach((scoreItemsId, scoreManScoringList) -> {

                PerfScoreItems updateScoreItems = this.getById(scoreItemsId);
                // ?????????????????????
                Map<String, List<ScoreManScoringV1>> scoreManScoringByUserMap = scoreManScoringList.stream()
                        .collect(Collectors.groupingBy(ScoreManScoringV1::getScoreUserName));
                // ?????????
                AtomicInteger scoredCount = new AtomicInteger(0);
                if (MapUtils.isNotEmpty(scoreManScoringByUserMap)) {
                    // ?????????????????????????????????????????????????????????????????????????????????????????????????????????
                    scoreManScoringByUserMap.forEach((userName, scoringList) -> {
                        if (scoringList.stream().allMatch(e -> Objects.equals(YesOrNo.YES.getValue(), e.getIfScored()))) {
                            scoredCount.addAndGet(1);
                        }
                    });
                }

                // ??????????????????????????????????????????
                List<PerfScoreItemsMan> scoreItemsManList = CollectionUtils.isEmpty(scoreItemsIds) ? Collections.emptyList() :
                        iPerfScoreItemsManService.list(Wrappers.lambdaQuery(PerfScoreItemsMan.class).eq(PerfScoreItemsMan::getScoreItemsId, scoreItemsId));
                // ??????????????????????????????????????????
                int count = scoredCount.get();
                if (count <= scoreItemsManList.size()) {
                    updateScoreItems.setScorePeople(Long.valueOf(count));
                    this.updateById(updateScoreItems);
                }

            });
        }

        result = ResultCode.SUCCESS.getMessage();
        return result;
    }

    @Override
    public List<PerfScoreItemSupIndDTO> getScoreItemsSupIndicatorList(List<PerfScoreItemsSup> scoreItemsSupList) {
        Assert.isTrue(CollectionUtils.isNotEmpty(scoreItemsSupList) && null != scoreItemsSupList.get(0),
                ScoreItemsConst.PER_SCORE_ITEMS_SUP_NOT_NULL);
        Long templateHeadId = scoreItemsSupList.get(0).getTemplateHeadId();
        List<PerfTemplateLine> templateLineList = this.getPerfTemplateLineByHeaderId(templateHeadId);   //?????????????????????ID??????????????????????????????

        List<PerfScoreItemSupIndDTO> scoreItemsSupIndicatorList = new ArrayList<>();
        for (PerfScoreItemsSup scoreItemsSup : scoreItemsSupList) {
            if (null != scoreItemsSup) {
                if (CollectionUtils.isNotEmpty(templateLineList)) {
                    for (PerfTemplateLine templateLine : templateLineList) {
                        if (null != templateLine) {
                            PerfScoreItemSupIndDTO scoreItemsSupIndicatorDTO = new PerfScoreItemSupIndDTO();
                            BeanUtils.copyProperties(scoreItemsSup, scoreItemsSupIndicatorDTO);
                            scoreItemsSupIndicatorDTO.setTemplateLineId(templateLine.getTemplateLineId());
                            scoreItemsSupIndicatorDTO.setDimensionWeight(templateLine.getDimensionWeight());
                            scoreItemsSupIndicatorDTO.setEvaluation(templateLine.getEvaluation());
                            scoreItemsSupIndicatorDTO.setIndicatorDimension(templateLine.getIndicatorDimension());
                            scoreItemsSupIndicatorDTO.setIndicatorLineType(templateLine.getIndicatorLineType());
                            scoreItemsSupIndicatorDTO.setIndicatorLogic(templateLine.getIndicatorLogic());
                            scoreItemsSupIndicatorDTO.setIndicatorName(templateLine.getIndicatorName());
                            scoreItemsSupIndicatorDTO.setIndicatorType(templateLine.getIndicatorType());
                            scoreItemsSupIndicatorDTO.setMarkLimit(templateLine.getMarkLimit());
                            scoreItemsSupIndicatorDTO.setQuoteMode(templateLine.getQuoteMode());
                            scoreItemsSupIndicatorDTO.setTemplateDimWeightId(templateLine.getTemplateDimWeightId());
                            scoreItemsSupIndicatorList.add(scoreItemsSupIndicatorDTO);
                        }
                    }
                }
            }
        }
        return scoreItemsSupIndicatorList;
    }

    /**
     * ??????????????????????????????
     *
     * @param scoreItemsDTO
     */
    public void checkIfMonthOverLap(PerfScoreItemsDTO scoreItemsDTO) {
        // ??????????????????????????????????????????
        String startMonth = scoreItemsDTO.getPerStartMonth().toString();
        String endMonth = scoreItemsDTO.getPerEndMonth().toString();
        if (startMonth.compareTo(endMonth) > 0) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????????????????????????????"));
        }
        Long scoreItemsId = scoreItemsDTO.getScoreItemsId();
        // ????????????id
        Assert.notNull(scoreItemsDTO.getTemplateHeadId(), "????????????????????????????????????????????????");
        Long templateHeadId = scoreItemsDTO.getTemplateHeadId();
        List<PerfScoreItems> perfScoreItemsList = this.list(Wrappers.lambdaQuery(PerfScoreItems.class)
                .eq(PerfScoreItems::getTemplateHeadId, templateHeadId));
        if (Objects.nonNull(scoreItemsId)) {
            perfScoreItemsList = perfScoreItemsList.stream().filter(x -> !Objects.equals(x.getScoreItemsId(), scoreItemsId)).collect(Collectors.toList());
        }
        for (PerfScoreItems perfScoreItems : perfScoreItemsList) {
            LocalDate perStartMonth = perfScoreItems.getPerStartMonth();
            LocalDate perEndMonth = perfScoreItems.getPerEndMonth();
            if (Objects.isNull(perStartMonth) || Objects.isNull(perEndMonth)) {
                continue;
            }
            String dbPerStartMonthString = perStartMonth.toString();
            String dbPerEndMonthString = perEndMonth.toString();
            // ?????????????????????????????????
            boolean case1 = (startMonth.compareTo(dbPerStartMonthString) > 0) && (startMonth.compareTo(dbPerEndMonthString) < 0);
            // ?????????????????????????????????
            boolean case2 = (dbPerStartMonthString.compareTo(startMonth) > 0) && (dbPerStartMonthString.compareTo(endMonth) < 0);
            // ??????????????????
            boolean case3 = (startMonth.compareTo(dbPerStartMonthString) == 0);
            // ??????????????????
            boolean case4 = (endMonth.compareTo(dbPerEndMonthString) == 0);
            if (case1 || case2 || case3 || case4) {
                int dbYear1 = perStartMonth.getYear();
                int dbMonth1 = perStartMonth.getMonthValue();
                String dbStartMonthString = dbYear1 + "-" + dbMonth1;
                int dbYear2 = perEndMonth.getYear();
                int dbMonth2 = perEndMonth.getMonthValue();
                String dbEndMonthString = dbYear2 + "-" + dbMonth2;
                StringBuffer sb = new StringBuffer();
                sb.append("????????????????????????????????????[").append(perfScoreItems.getProjectName()).append("]????????????????????????[")
                        .append(dbStartMonthString).append("???").append(dbEndMonthString).append("]????????????????????????");
                throw new BaseException(LocaleHandler.getLocaleMsg(sb.toString()));
            }
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param scoreItemsDTO
     */
    public void checkIfProjectNameDuplicate(PerfScoreItemsDTO scoreItemsDTO) {
        Long scoreItemsId = scoreItemsDTO.getScoreItemsId();
        String projectName = scoreItemsDTO.getProjectName();
        List<PerfScoreItems> perfScoreItemsList = new ArrayList<>();
        // ??????????????????????????????????????????id
        if (Objects.isNull(scoreItemsId)) {
            perfScoreItemsList = this.list(Wrappers.lambdaQuery(PerfScoreItems.class)
                    .eq(PerfScoreItems::getProjectName, projectName));
            if (perfScoreItemsList.size() > 0) {
                StringBuffer sb = new StringBuffer();
                String existProjectName = perfScoreItemsList.get(0).getProjectName();
                sb.append("?????????????????????????????????????????????[").append(existProjectName).append("]????????????????????????????????????");
                throw new BaseException(LocaleHandler.getLocaleMsg(sb.toString()));
            }
        }
        // ????????????????????????????????????????????????????????????
        else {
            perfScoreItemsList = this.list(Wrappers.lambdaQuery(PerfScoreItems.class)
                    .ne(PerfScoreItems::getScoreItemsId, scoreItemsId)
                    .eq(PerfScoreItems::getProjectName, projectName));
            if (perfScoreItemsList.size() > 0) {
                StringBuffer sb = new StringBuffer();
                String existProjectName = perfScoreItemsList.get(0).getProjectName();
                sb.append("?????????????????????????????????????????????[").append(existProjectName).append("]????????????????????????????????????");
                throw new BaseException(LocaleHandler.getLocaleMsg(sb.toString()));
            }
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param scoreItemsSupList
     */
    public void checkScoreItemsSup(List<PerfScoreItemsSup> scoreItemsSupList) {
        if (CollectionUtils.isEmpty(scoreItemsSupList)) {
            return;
        }
        Map<Long, List<PerfScoreItemsSup>> scoreItemsSupMap = scoreItemsSupList.stream().collect(Collectors.groupingBy(PerfScoreItemsSup::getCompanyId));
        scoreItemsSupMap.forEach((k, v) -> {
            if (v.size() > 1) {
                StringBuffer sb = new StringBuffer();
                sb.append("????????????????????????????????????????????????[").append(v.get(0).getCompanyName()).append("]????????????????????????");
                throw new BaseException(LocaleHandler.getLocaleMsg(sb.toString()));
            }
        });
    }

    /**
     * ???????????????????????????
     *
     * @param perfScoreItemsManList
     */
    public void checkScoreItemsMan(List<PerfScoreItemsMan> perfScoreItemsManList) {
        if (CollectionUtils.isEmpty(perfScoreItemsManList)) {
            return;
        }
        Map<String, List<PerfScoreItemsMan>> scoreItemsManMap = perfScoreItemsManList.stream().collect(Collectors.groupingBy(PerfScoreItemsMan::getScoreUserName));
        scoreItemsManMap.forEach((k, v) -> {
            if (v.size() > 1) {
                StringBuffer sb = new StringBuffer();
                sb.append("??????????????????????????????????????????[").append(v.get(0).getScoreUserName()).append("]????????????????????????");
                throw new BaseException(LocaleHandler.getLocaleMsg(sb.toString()));
            }
        });
    }

    @Override
    public String checkIsSaveOrUpdateScoreItems(Long scoreItemsId, PerfScoreItemsDTO scoreItemsDTO) {
        String result = "";
//        if(null == scoreItemsDTO && null != scoreItemsId) {
//            PerfScoreItems queryScoreItems = new PerfScoreItems();
//            queryScoreItems.setScoreItemsId(scoreItemsId);
//            scoreItemsDTO = this.findScoreItemsAndSonList(queryScoreItems);
//        }
//        Assert.isTrue(null != scoreItemsDTO && CollectionUtils.isNotEmpty(scoreItemsDTO.getPerfScoreItemsManList()),
//                ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN);
//
//        boolean isRepeat = false;   //????????????????????????
//        if(null != scoreItemsDTO && CollectionUtils.isNotEmpty(scoreItemsDTO.getPerfScoreItemsManList())) {
//            List<PerfScoreItemsMan> scoreItemsManList = scoreItemsDTO.getPerfScoreItemsManList();
//            Assert.isTrue(null != scoreItemsManList.get(0), ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN);
//
//            /**1????????????????????????????????????????????????????????????????????????*/
//            if (1 == scoreItemsManList.size() && null != scoreItemsManList.get(0)) {  //???????????????????????????
//                PerfScoreItemsMan scoreItemsManOne = scoreItemsManList.get(0);
//                if (null != scoreItemsManOne) {
//                    List<PerfScoreItemsManSup> scoreItemsManSupList = scoreItemsManOne.getPerfScoreItemsManSupList();
//                    Assert.isTrue(CollectionUtils.isNotEmpty(scoreItemsManSupList) || null != scoreItemsManSupList.get(0), ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN);
//                    List<PerfScoreItemsManIndicator> scoreItemsManIndicatorList = scoreItemsManOne.getPerfScoreItemsManIndicatorList();
//                    Assert.isTrue(CollectionUtils.isNotEmpty(scoreItemsManIndicatorList) || null != scoreItemsManIndicatorList.get(0), ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                    for (PerfScoreItemsManSup itemsManSup : scoreItemsManSupList) {
//                        if (Enable.N.toString().equals(itemsManSup.getEnableFlag()) || null == itemsManSup.getCompanyId()) {
//                            log.error("??????????????????/???????????????????????????,???????????????ID: "+String.valueOf(itemsManSup.getScoreItemsManSupId())
//                                    +"???????????????:"+itemsManSup.getCompanyName()+","+ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                            Assert.isTrue(false, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                        }
//                    }
//                    for (PerfScoreItemsManIndicator itemsManIndicator : scoreItemsManIndicatorList) {
//                        if (Enable.N.toString().equals(itemsManIndicator.getEnableFlag()) || null == itemsManIndicator.getTemplateLineId()) {
//                            log.error("??????????????????/???????????????????????????,????????????ID: "+String.valueOf(itemsManIndicator.getScoreItemsManIndicatorId())
//                                    +"???????????????:"+itemsManIndicator.getIndicatorName()+","+ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                            Assert.isTrue(false, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                        }
//                    }
//                }
//            } else {
//                /**???????????????????????????????????????????????????????????????????????????*/
//                for (PerfScoreItemsMan scoreItemsMan : scoreItemsManList) {   //???????????????
//                    Assert.isTrue(null != scoreItemsMan, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN);
//                    if (null != scoreItemsMan) {
//                        String scoreUserName = scoreItemsMan.getScoreUserName();
//                        Assert.isTrue(null != scoreUserName, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN);
//                        List<PerfScoreItemsManSup> scoreItemsManSupList = scoreItemsMan.getPerfScoreItemsManSupList();
//                        Assert.isTrue(CollectionUtils.isNotEmpty(scoreItemsManSupList), ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                        List<PerfScoreItemsManIndicator> scoreItemsManIndicatorList = scoreItemsMan.getPerfScoreItemsManIndicatorList();
//                        Assert.isTrue(CollectionUtils.isNotEmpty(scoreItemsManIndicatorList), ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                        /**?????????????????????*/
//                        if (1 == scoreItemsManSupList.size()) {
//                            PerfScoreItemsManSup itemsManSup = scoreItemsManSupList.get(0);
//                            if (Enable.N.toString().equals(itemsManSup.getEnableFlag()) || null == itemsManSup.getCompanyId()) {
//                                log.error("??????????????????/???????????????????????????,???????????????ID: "+String.valueOf(itemsManSup.getScoreItemsManSupId())
//                                        +"???????????????:"+itemsManSup.getCompanyName()+","+ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                                Assert.isTrue(false, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                            }
//                        }else{  //???????????????
//
//                            /**?????????????????????????????????????????????????????????????????????????????????*/
//                            for(PerfScoreItemsManSup manSup : scoreItemsManSupList){
//                                Assert.isTrue(null == manSup, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                                boolean isManSup = Enable.Y.toString().equals(manSup.getEnableFlag()) ? true : false;   //??????????????????????????????
//                                if(!isManSup){
//                                    Long companyId = manSup.getCompanyId();
//                                    for(PerfScoreItemsManSup itemsManSup : scoreItemsManSupList){
//                                        if(null != companyId && companyId.compareTo(itemsManSup.getCompanyId()) == 0
//                                                && Enable.Y.toString().equals(itemsManSup.getEnableFlag()) ){
//                                            isManSup = true;
//                                            break;
//                                        }
//                                    }
//                                }
//                                Assert.isTrue(isManSup, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//
//                                //??????????????????,???????????????????????????
//                                if(1 == scoreItemsManIndicatorList.size()){
//                                    PerfScoreItemsManIndicator itemsManIndicator = scoreItemsManIndicatorList.get(0);
//                                    if (Enable.N.toString().equals(itemsManIndicator.getEnableFlag()) || null == itemsManIndicator.getDimWeightId()) {
//                                        log.error("??????????????????/???????????????????????????,????????????ID: "+String.valueOf(itemsManIndicator.getScoreItemsManIndicatorId())
//                                                +"????????????:"+itemsManIndicator.getIndicatorName()+","+ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                        Assert.isTrue(false, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                    }else{  //??????????????????
//                                        for(PerfScoreItemsManIndicator scoreItemsManIndicator : scoreItemsManIndicatorList){
//                                            Assert.isTrue(null != scoreItemsManIndicator.getDimWeightId(), ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                            boolean isIndicator = false;    //???????????????????????????????????????false
//                                           /* if (null != scoreItemsManIndicator) {
//                                                Long templateLineId = scoreItemsManIndicator.getTemplateLineId();
//                                                String indicatorEnableFlag = scoreItemsManIndicator.getEnableFlag();
//                                                isIndicator = Enable.Y.toString().equals(indicatorEnableFlag) ? true : false;
//                                                for (PerfScoreItemsManIndicator itemsManIndicator : itemsManIndicatorList) {
//                                                    if (null != itemsManIndicator && isIndicator
//                                                            && templateLineId.compareTo(itemsManIndicator.getTemplateLineId()) == 0
//                                                            && Enable.Y.toString().equals(itemsManIndicator.getEnableFlag())) {
//                                                        log.error("??????????????????/???????????????????????????,????????????ID: "+String.valueOf(scoreItemsManIndicator.getScoreItemsManIndicatorId())
//                                                                +"????????????:"+scoreItemsManIndicator.getIndicatorName()+","+ScoreItemsConst.REPEAT_SCORE_ITEMS_MAN_INDICATORS);
//                                                        Assert.isTrue(false, ScoreItemsConst.REPEAT_SCORE_ITEMS_MAN_INDICATORS);
//                                                    }else if (!isIndicator && null != itemsManIndicator && templateLineId.compareTo(itemsManIndicator.getTemplateLineId()) == 0
//                                                            && Enable.Y.toString().equals(itemsManIndicator.getEnableFlag())) {
//                                                        isIndicator = true;
//                                                        break;
//                                                    }
//                                                }
//                                                if(!isIndicator){
//                                                    log.error("??????????????????/???????????????????????????,????????????ID: "+String.valueOf(scoreItemsManIndicator.getScoreItemsManIndicatorId())
//                                                            +"????????????:"+scoreItemsManIndicator.getIndicatorName()+","+ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                                    Assert.isTrue(false, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                                }
//                                            }*/
//                                        }
//                                    }
//                                }
//                            }
//
//
//                            for (PerfScoreItemsMan itemsMan : scoreItemsManList) {   //???????????????
//                                if(!scoreUserName.equals(itemsMan.getScoreUserName())){
//                                    List<PerfScoreItemsManSup> itemsManSupList = itemsMan.getPerfScoreItemsManSupList();
//                                    Assert.isTrue(CollectionUtils.isNotEmpty(itemsManSupList), ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                                    List<PerfScoreItemsManIndicator> itemsManIndicatorList = itemsMan.getPerfScoreItemsManIndicatorList();
//                                    for (PerfScoreItemsManSup scoreItemsManSup : scoreItemsManSupList) {
//                                        Long companyId = scoreItemsManSup.getCompanyId();
//                                        String supEnableFlag = scoreItemsManSup.getEnableFlag();
//                                        for (PerfScoreItemsManSup itemsManSup : itemsManSupList) {
//                                            if (null != companyId && companyId.compareTo(itemsManSup.getCompanyId()) == 1
//                                                    && Enable.N.toString().equals(supEnableFlag)
//                                                    && Enable.N.toString().equals(itemsManSup.getEnableFlag())) {  //??????????????????????????????
//                                                log.error("??????????????????/???????????????????????????,???????????????ID: "+String.valueOf(itemsManSup.getScoreItemsManSupId())
//                                                        +"???????????????:"+itemsManSup.getCompanyName()+","+ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                                                Assert.isTrue(false, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_SUP);
//                                            }
//                                        }
//
//                                        //??????????????????,???????????????????????????
//                                        if(1 == scoreItemsManIndicatorList.size()){
//                                            PerfScoreItemsManIndicator itemsManIndicator = scoreItemsManIndicatorList.get(0);
//                                            if (Enable.N.toString().equals(itemsManIndicator.getEnableFlag()) || null == itemsManIndicator.getDimWeightId()) {
//                                                log.error("??????????????????/???????????????????????????,????????????ID: "+String.valueOf(itemsManIndicator.getScoreItemsManIndicatorId())
//                                                        +"????????????:"+itemsManIndicator.getIndicatorName()+","+ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                                Assert.isTrue(false, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                            }
//                                        }else{  //??????????????????
//                                            for(PerfScoreItemsManIndicator scoreItemsManIndicator : scoreItemsManIndicatorList){
//                                                Assert.isTrue(null != scoreItemsManIndicator.getDimWeightId(), ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                                boolean isIndicator = false;    //???????????????????????????????????????false
//                                                if (null != scoreItemsManIndicator) {
//                                                    Long templateLineId = scoreItemsManIndicator.getTemplateLineId();
//                                                    String indicatorEnableFlag = scoreItemsManIndicator.getEnableFlag();
//                                                    isIndicator = Enable.Y.toString().equals(indicatorEnableFlag) ? true : false;
//                                                    for (PerfScoreItemsManIndicator itemsManIndicator : itemsManIndicatorList) {
//                                                        if (null != itemsManIndicator && isIndicator
//                                                                && templateLineId.compareTo(itemsManIndicator.getTemplateLineId()) == 0
//                                                                && Enable.Y.toString().equals(itemsManIndicator.getEnableFlag())) {
//                                                            log.error("??????????????????/???????????????????????????,????????????ID: "+String.valueOf(scoreItemsManIndicator.getScoreItemsManIndicatorId())
//                                                                    +"????????????:"+scoreItemsManIndicator.getIndicatorName()+","+ScoreItemsConst.REPEAT_SCORE_ITEMS_MAN_INDICATORS);
//                                                            Assert.isTrue(false, ScoreItemsConst.REPEAT_SCORE_ITEMS_MAN_INDICATORS);
//                                                        }else if (!isIndicator && null != itemsManIndicator && templateLineId.compareTo(itemsManIndicator.getTemplateLineId()) == 0
//                                                                && Enable.Y.toString().equals(itemsManIndicator.getEnableFlag())) {
//                                                            isIndicator = true;
//                                                            break;
//                                                        }
//                                                    }
//                                                    if(!isIndicator){
//                                                        log.error("??????????????????/???????????????????????????,????????????ID: "+String.valueOf(scoreItemsManIndicator.getScoreItemsManIndicatorId())
//                                                                +"????????????:"+scoreItemsManIndicator.getIndicatorName()+","+ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                                        Assert.isTrue(false, ScoreItemsConst.NOT_SET_SCORE_ITEMS_MAN_INDICATORS);
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
        return result;
    }

    @Override
    public Map<String, Object> updateScoreItemsWithFlow(PerfScoreItems scoreItems) {
        Map<String, Object> map = new HashMap();
        PerfScoreItems source = this.getById(scoreItems.getScoreItemsId());
        if (enableFlowWork(scoreItems)) {
            map.put("businessId", source.getScoreItemsId());
            map.put("fdId", source.getCbpmInstaceId());
            map.put("subject", source.getOrganizationName());
            if (StringUtil.isEmpty(source.getCbpmInstaceId())) {
                CbpmRquestParamDTO request = buildCbpmRquest(source);
                map = workFlowFeign.initProcess(request);
            }

        }
        return map;
    }

    /**
     * ??????????????????
     *
     * @param scoreItemsId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyScoreItems(Long scoreItemsId) {
        PerfScoreItems scoreItems = this.getById(scoreItemsId);
        // ????????????id
        Long newScoreItemsId = IdGenrator.generate();
        scoreItems.setScoreItemsId(newScoreItemsId)
                .setProjectStatus(ScoreItemsProjectStatusEnum.SCORE_DRAFT.getValue())
                .setApproveStatus(ScoreItemsApproveStatusEnum.DRAFT.getValue())
                .setPerStartMonth(null)
                .setPerEndMonth(null)
                .setScoreStartTime(null)
                .setScoreEndTime(null)
                .setScorePeople(Long.valueOf(0));
        this.save(scoreItems);

        // ?????????????????????
        List<PerfScoreItemsSup> scoreItemsSups = iPerfScoreItemsSupService.list(Wrappers.lambdaQuery(PerfScoreItemsSup.class)
                .eq(PerfScoreItemsSup::getScoreItemsId, scoreItemsId));
        if (CollectionUtils.isNotEmpty(scoreItemsSups)) {
            scoreItemsSups.forEach(scoreItemsSup -> {
                scoreItemsSup.setScoreItemsSupId(IdGenrator.generate());
                scoreItemsSup.setScoreItemsId(newScoreItemsId);
            });
            // ??????????????????
            iPerfScoreItemsSupService.saveBatch(scoreItemsSups);
        }

        // ???????????????
        List<PerfScoreItemsMan> scoreItemsMans = iPerfScoreItemsManService.list(Wrappers.lambdaQuery(PerfScoreItemsMan.class)
                .eq(PerfScoreItemsMan::getScoreItemsId, scoreItemsId));
        if (CollectionUtils.isNotEmpty(scoreItemsMans)) {
            scoreItemsMans.forEach(scoreItemsMan -> {
                scoreItemsMan.setScoreItemsManId(IdGenrator.generate());
                scoreItemsMan.setScoreItemsId(newScoreItemsId);
            });
            // ??????????????????
            iPerfScoreItemsManService.saveBatch(scoreItemsMans);
        }
    }

    /**
     * ??????????????????????????????????????????????????????
     * @param scoreItems
     * @return
     */
    @Override
    public BaseResult<String> confirmBeforeCalculate(PerfScoreItems scoreItems) {
        List<ScoreManScoringV1> scoreManScoringV1s = iScoreManScoringV1Service.list(Wrappers.lambdaQuery(ScoreManScoringV1.class)
                .eq(ScoreManScoringV1::getScoreItemsId, scoreItems.getScoreItemsId()));
        // ????????????????????????
        List<ScoreManScoringV1> isNullScoreManScoringV1s = scoreManScoringV1s.stream().filter(x -> Objects.isNull(x.getScore())).collect(Collectors.toList());
        BaseResult baseResult = BaseResult.build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
        int isNullSize = isNullScoreManScoringV1s.size();
        if (isNullSize == 0) {
            baseResult.setData(Collections.EMPTY_LIST);
            return baseResult;
        }
        List<String> resultMsg = new ArrayList<>();
        isNullScoreManScoringV1s.forEach(scoreManScoringV1 -> {
            StringBuffer sb = new StringBuffer();
            // ?????????
            String companyName = scoreManScoringV1.getCompanyName();
            // ????????????
            String indicatorDimensionType = scoreManScoringV1.getIndicatorDimensionType();
            String indicatorDimension = INDICATOR_DIMENSION_MAP.get(indicatorDimensionType);
            // ????????????
            String indicatorName = scoreManScoringV1.getIndicatorName();
            // ?????????
            String scoreNickName = scoreManScoringV1.getScoreNickName();
            sb.append("????????????[").append(companyName).append("]??????????????????[").append(indicatorName).append("]???????????????[").append(scoreNickName).append("]????????????");
            resultMsg.add(sb.toString());
        });
        baseResult.setData(resultMsg);
        return baseResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandonAlready(Long scoreItemId) {
        Assert.notNull(scoreItemId,"ID????????????");
        PerfScoreItems perfScoreItems = this.getById(scoreItemId);
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (!Objects.equals(loginAppUser.getUserId(),perfScoreItems.getCreatedId())) {
            throw new BaseException("???????????????????????????????????????");
        }
        //?????????????????????
        this.updateById(new PerfScoreItems()
                .setApproveStatus(ScoreItemsApproveStatusEnum.DRAFT.getValue())
                .setProjectStatus("OBSOLETE").setScoreItemsId(scoreItemId));
        iScoreManScoringV1Service.remove(Wrappers.lambdaQuery(ScoreManScoringV1.class)
                .eq(ScoreManScoringV1::getScoreItemsId,scoreItemId));
    }

    private Boolean enableFlowWork(PerfScoreItems source) {
        Long menuId = source.getMenuId();
        Permission menu = rbacClient.getMenu(menuId);
        Boolean flowEnable;
        try {
            flowEnable = workFlowFeign.getFlowEnable(menuId, menu.getFunctionId(), CbpmFormTemplateIdEnum.PERF_SCORE_ITEMS.getKey());
        } catch (FeignException e) {
            log.error("?????????????????????????????????,??????????????????????????????,?????? menuId???" + menuId + ",functionId" + menu.getFunctionId()
                    + ",templateCode" + CbpmFormTemplateIdEnum.PERF_SCORE_ITEMS.getKey() + "?????????", e);
            throw new BaseException("???????????????????????????????????????????????????????????????");
        }
        return flowEnable;
    }

    private CbpmRquestParamDTO buildCbpmRquest(PerfScoreItems source) {
        CbpmRquestParamDTO cbpmRquestParamDTO = new CbpmRquestParamDTO();
        cbpmRquestParamDTO.setBusinessId(String.valueOf(source.getScoreItemsId()));
        cbpmRquestParamDTO.setTemplateCode(CbpmFormTemplateIdEnum.PERF_SCORE_ITEMS.getKey());
        cbpmRquestParamDTO.setSubject(source.getProjectName());
        cbpmRquestParamDTO.setFdId(source.getCbpmInstaceId());
        return cbpmRquestParamDTO;
    }
}

