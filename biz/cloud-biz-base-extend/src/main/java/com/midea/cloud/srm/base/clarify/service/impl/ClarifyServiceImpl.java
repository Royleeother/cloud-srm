package com.midea.cloud.srm.base.clarify.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.BeanCopyUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.base.clarify.mapper.SourcingClarifyFileMapper;
import com.midea.cloud.srm.base.clarify.mapper.SourcingClarifyMapper;
import com.midea.cloud.srm.base.clarify.mapper.SourcingClarifyReplyMapper;
import com.midea.cloud.srm.base.clarify.service.IClarifyService;
import com.midea.cloud.srm.feign.bargaining.BargainSourcingClarifyClient;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.base.IGetSourcingRelateInfo;
import com.midea.cloud.srm.feign.bid.BidSourcingClarifyClient;
import com.midea.cloud.srm.model.base.clarify.dto.*;
import com.midea.cloud.srm.model.base.clarify.entity.SourcingClarify;
import com.midea.cloud.srm.model.base.clarify.entity.SourcingClarifyFile;
import com.midea.cloud.srm.model.base.clarify.entity.SourcingClarifyReply;
import com.midea.cloud.srm.model.base.clarify.enums.ClarifySourcingType;
import com.midea.cloud.srm.model.base.clarify.enums.ClarifyStatus;
import com.midea.cloud.srm.model.base.clarify.enums.ClarifyType;
import com.midea.cloud.srm.model.base.clarify.vo.*;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author tanjl11
 * @date 2020/10/07 21:17
 */
@Service
@AllArgsConstructor
public class ClarifyServiceImpl implements IClarifyService {
    private final SourcingClarifyFileMapper fileMapper;
    private final SourcingClarifyMapper clarifyMapper;
    private final SourcingClarifyReplyMapper replyMapper;
    private final BaseClient baseClient;
    private final BidSourcingClarifyClient bidClarifyClient;
    private final BargainSourcingClarifyClient bargainSourcingClarifyClient;

    /**
     * ??????????????????
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SourcingClarifyDetailVO publishSourcingClarify(SourcingClarifyDto dto) {
        //????????????????????????
        SourcingClarifyDetailVO detailVO = saveTemporarySourcingClarify(dto, ClarifyStatus.PUBLISH);
        //????????????
        generateReplyForSourcing(detailVO, ClarifySourcingType.valueOf(dto.getSourcingType()), dto.getSourcingId());
        return detailVO;
    }

    /**
     * ??????id????????????
     *
     * @param clarifyId
     * @return
     */
    @Override
    public Boolean publishSourcingClarifyById(Long clarifyId) {
        String clarifyStatus = getExistsClarifyStatus(clarifyId);
        if (Objects.equals(clarifyStatus, ClarifyStatus.PUBLISH.getCode())) {
            throw new BaseException("?????????????????????!");
        }
        return clarifyMapper.update(null, Wrappers.lambdaUpdate(SourcingClarify.class)
                .set(SourcingClarify::getClarifyStatus, ClarifyStatus.PUBLISH.getCode())
                .eq(SourcingClarify::getClarifyId, clarifyId)
        ) == 1;
    }


    /**
     * ??????????????????
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SourcingClarifyDetailVO saveTemporarySourcingClarify(SourcingClarifyDto dto, ClarifyStatus status) {
        SourcingClarify sourcingClarify = BeanCopyUtil.copyProperties(dto, SourcingClarify::new);
        sourcingClarify.setClarifyStatus(status.getCode());
        boolean isNew = Objects.isNull(dto.getClarifyId());
        if (isNew) {
            //??????clarify??????
            String code = baseClient.seqGen(SequenceCodeConstant.SEQ_SOURCING_CLARIFY);
            long clarifyId = IdGenrator.generate();
            sourcingClarify.setClarifyId(clarifyId);
            sourcingClarify.setClarifyNumber(code);
            clarifyMapper.insert(sourcingClarify);
        } else {
            clarifyMapper.updateById(sourcingClarify);
        }
        SourcingClarifyDetailVO detailVO = new SourcingClarifyDetailVO();
        SourcingClarifyVO clarifyVO = BeanCopyUtil.copyProperties(sourcingClarify, SourcingClarifyVO::new);
        detailVO.setClarify(clarifyVO);
        //????????????????????????????????????
        List<SourcingClarifyFileDto> files = dto.getFiles();
        List<SourcingClarifyFileVO> fileVos = getSourcingClarifyFileVOS(sourcingClarify.getClarifyId(), isNew, files, ClarifyType.NOTICE);
        detailVO.setFiles(fileVos);
        return detailVO;
    }

    /**
     * ??????????????????
     *
     * @param sourcingClarifyId
     * @return
     */
    @Override
    public Boolean deleteSourcingClarifyById(Long sourcingClarifyId) {
        String existsClarifyStatus = getExistsClarifyStatus(sourcingClarifyId);
        if (Objects.equals(existsClarifyStatus, ClarifyStatus.PUBLISH)) {
            throw new BaseException("?????????????????????????????????");
        }
        //????????????
        int count = clarifyMapper.deleteById(sourcingClarifyId);
        //????????????
        fileMapper.delete(Wrappers.lambdaQuery(SourcingClarifyFile.class)
                .eq(SourcingClarifyFile::getClarifyId, sourcingClarifyId));
        return count == 1;
    }

    /**
     * ????????????
     *
     * @param dto
     * @return
     */
    @Override
    public SourcingClarifyReplyDetailVO replySourcingClarify(SourcingClarifyReplyDto dto) {
        SourcingClarifyReply reply = replyMapper.selectOne(Wrappers.lambdaQuery(SourcingClarifyReply.class)
                .select(SourcingClarifyReply::getClarifyReplyId, SourcingClarifyReply::getReplyStatus)
                .eq(SourcingClarifyReply::getClarifyReplyId, dto.getClarifyReplyId()));
        if (Objects.isNull(reply)) {
            throw new BaseException(String.format("id???%s????????????????????????", dto.getClarifyReplyId()));
        }
        if (Objects.equals(reply.getReplyStatus(), ClarifyStatus.ALREADYRESPONSE.getCode())) {
            throw new BaseException(String.format("?????????????????????????????????"));
        }
        return saveTemporarySourcingClarifyReply(dto, ClarifyStatus.ALREADYRESPONSE);
    }

    /**
     * ???????????????????????????????????????
     * ???????????????????????????????????????????????????????????????
     *
     * @param dto
     * @param status
     * @return
     */
    @Override
    public SourcingClarifyReplyDetailVO saveTemporarySourcingClarifyReply(SourcingClarifyReplyDto dto, ClarifyStatus status) {
        SourcingClarifyReplyDetailVO detailVO = new SourcingClarifyReplyDetailVO();
        boolean isNew = Objects.isNull(dto.getClarifyId());
        //???????????????????????????
        SourcingClarify clarify = clarifyMapper.selectById(dto.getClarifyId());
        SourcingClarifyReply reply = BeanCopyUtil.copyProperties(dto, SourcingClarifyReply::new);
        reply.setReplyStatus(status.getCode());
        BeanUtils.copyProperties(clarify, reply, BeanCopyUtil.getNullPropertyNames(reply));
        if (isNew) {
            LoginAppUser user = AppUserUtil.getLoginAppUser();
            reply.setCompanyCode(user.getCompanyCode());
            reply.setCompanyId(user.getCompanyId());
            reply.setCompanyName(user.getCompanyName());
            reply.setVendorId(user.getUserId());
            reply.setVendorName(user.getUsername());
            long replyId = IdGenrator.generate();
            reply.setClarifyReplyId(replyId);
            replyMapper.insert(reply);
        } else {
            replyMapper.updateById(reply);
        }
        detailVO.setReply(BeanCopyUtil.copyProperties(reply, SourcingClarifyReplyVO::new));
        //????????????
        List<SourcingClarifyFileDto> files = dto.getFiles();
        List<SourcingClarifyFileVO> filesVO = getSourcingClarifyFileVOS(reply.getClarifyId(), isNew, files, ClarifyType.REPLY);
        detailVO.setFiles(filesVO);
        return detailVO;
    }

    /**
     * ?????????????????????
     *
     * @param sourcingClarifyId
     * @return
     */
    public List<SourcingClarifyReplyVO> listSourcingReplyBySourcingClarifyId(Long sourcingClarifyId) {
        return replyMapper.selectList(Wrappers.lambdaQuery(SourcingClarifyReply.class)
                .eq(SourcingClarifyReply::getClarifyId, sourcingClarifyId)).stream()
                .map(e -> BeanCopyUtil.copyProperties(e, SourcingClarifyReplyVO::new))
                .collect(Collectors.toList());
    }

    /**
     * ??????????????????
     * ?????????????????????????????????
     *
     * @param queryDto
     * @return
     */
    @Override
    public PageInfo<SourcingClarifyVO> listSourcingClarifyByPage(SourcingClarifyQueryDto queryDto) {
        ///????????????????????????????????????????????????
        /* LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        Long vendorId=null;
        if(Objects.equals(loginAppUser.getUserType(),UserType.VENDOR.name())){
            vendorId=loginAppUser.getUserId();
        }*/
        PageUtil.startPage(queryDto.getPageNum(), queryDto.getPageSize());
        List<SourcingClarifyVO> result = clarifyMapper.selectList(Wrappers.lambdaQuery(SourcingClarify.class)
                .eq(Objects.nonNull(queryDto.getClarifyNumber()), SourcingClarify::getClarifyNumber, queryDto.getClarifyNumber())
                .eq(Objects.nonNull(queryDto.getClarifyStatus()), SourcingClarify::getClarifyStatus, queryDto.getClarifyStatus())
                .eq(Objects.nonNull(queryDto.getClarifyTitle()), SourcingClarify::getClarifyTitle, queryDto.getClarifyTitle())
                .eq(Objects.nonNull(queryDto.getSourcingNumber()), SourcingClarify::getSourcingNumber, queryDto.getClarifyNumber())
                .eq(Objects.nonNull(queryDto.getSourcingProjectName()), SourcingClarify::getSourcingProjectName, queryDto.getSourcingProjectName())
                .eq(Objects.nonNull(queryDto.getSourcingType()), SourcingClarify::getSourcingType, queryDto.getSourcingType()))
                .stream().map(e -> BeanCopyUtil.copyProperties(e, SourcingClarifyVO::new)).collect(Collectors.toList());
        return new PageInfo<>(result);
    }

    /**
     * ????????????????????????id???????????????????????????
     *
     * @param clarifyId
     * @return
     */
    @Override
    public SourcingClarifyReplyDetailVO queryReplyDetailById(Long clarifyId) {
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        if (!Objects.equals(user.getUserType(), UserType.VENDOR.name())) {
            throw new BaseException("??????????????????????????????????????????");
        }
        Long vendorId = user.getUserId();
        SourcingClarifyReplyDetailVO vo = new SourcingClarifyReplyDetailVO();
        SourcingClarifyReply reply = replyMapper.selectOne(Wrappers.lambdaQuery(SourcingClarifyReply.class)
                .eq(SourcingClarifyReply::getClarifyId, clarifyId)
                .eq(SourcingClarifyReply::getVendorId, vendorId)
        );
        assignValue(vo, reply);
        return vo;
    }

    /**
     * ????????????????????????
     *
     * @param replyId
     * @return
     */
    @Override
    public SourcingClarifyReplyDetailVO queryReplyDetailByReplyId(Long replyId) {
        SourcingClarifyReplyDetailVO vo = new SourcingClarifyReplyDetailVO();
        SourcingClarifyReply reply = replyMapper.selectById(replyId);
        assignValue(vo, reply);
        return vo;
    }


    /**
     * ????????????id????????????
     *
     * @param clarifyId
     * @return
     */
    @Override
    public SourcingClarifyVO queryClarifyDetailById(Long clarifyId) {
        SourcingClarifyVO clarifyVO = BeanCopyUtil.copyProperties(clarifyMapper.selectById(clarifyId), SourcingClarifyVO::new);
        List<SourcingClarifyReplyVO> replyVOS = replyMapper.selectList(Wrappers.lambdaQuery(SourcingClarifyReply.class)
                .eq(SourcingClarifyReply::getClarifyId, clarifyId)).stream().map(e -> BeanCopyUtil.copyProperties(e, SourcingClarifyReplyVO::new)).collect(Collectors.toList());
        clarifyVO.setReplys(replyVOS);
        return clarifyVO;
    }


    /**
     * ???????????????????????????????????????
     *
     * @param clarifyId
     * @return
     */
    private String getExistsClarifyStatus(Long clarifyId) {
        SourcingClarify clarify = clarifyMapper.selectOne(
                Wrappers.lambdaQuery(SourcingClarify.class)
                        .select(SourcingClarify::getClarifyStatus)
                        .eq(SourcingClarify::getClarifyId, clarifyId));
        String clarifyStatus = Optional.of(clarify).orElseThrow(() -> new BaseException("????????????????????????"))
                .getClarifyStatus();
        return clarifyStatus;
    }


    private List<SourcingClarifyFileVO> getSourcingClarifyFileVOS(Long clarifyId, boolean isNew,
                                                                  List<SourcingClarifyFileDto> files, ClarifyType type) {
        List<SourcingClarifyFileVO> fileVos = null;
        if (isNew) {
            if (!CollectionUtils.isEmpty(files)) {
                fileVos = files.stream().map(e -> {
                    e.setClarifyFileId(IdGenrator.generate());
                    SourcingClarifyFile sourcingClarifyFile = BeanCopyUtil.copyProperties(e, SourcingClarifyFile::new);
                    sourcingClarifyFile.setClarifyType(type.getCode());
                    sourcingClarifyFile.setClarifyId(clarifyId);
                    fileMapper.insert(sourcingClarifyFile);
                    SourcingClarifyFileVO fileDetail = BeanCopyUtil.copyProperties(sourcingClarifyFile, SourcingClarifyFileVO::new);
                    return fileDetail;
                }).collect(Collectors.toList());
            }
            //????????????
        } else {
            List<Long> waitForDeleteFileIds = fileMapper.selectList(Wrappers.lambdaQuery(SourcingClarifyFile.class)
                    .eq(SourcingClarifyFile::getClarifyId, clarifyId))
                    .stream().map(SourcingClarifyFile::getClarifyFileId).collect(Collectors.toList());
            //?????????????????????????????????
            if (waitForDeleteFileIds.size() > 0) {
                for (int i = waitForDeleteFileIds.size() - 1; i >= 0; i--) {
                    boolean find = false;
                    Long judgeShouldDeleteId = waitForDeleteFileIds.get(i);
                    for (SourcingClarifyFileDto file : files) {
                        if (Objects.equals(file.getClarifyFileId(), judgeShouldDeleteId)) {
                            find = true;
                            break;
                        }
                    }
                    if (!find) {
                        waitForDeleteFileIds.remove(i);
                    }
                }
            }
            if (!CollectionUtils.isEmpty(waitForDeleteFileIds)) {
                fileMapper.deleteBatchIds(waitForDeleteFileIds);
            }
            //??????????????????????????????/???????????????
            if (!CollectionUtils.isEmpty(files)) {
                fileVos = files.stream().map(file -> {
                            if (Objects.isNull(file.getClarifyFileId())) {
                                file.setClarifyFileId(IdGenrator.generate());
                                SourcingClarifyFile sourcingClarifyFile = BeanCopyUtil.copyProperties(file, SourcingClarifyFile::new);
                                sourcingClarifyFile.setClarifyType(ClarifyType.NOTICE.getCode());
                                sourcingClarifyFile.setClarifyId(clarifyId);
                                fileMapper.insert(sourcingClarifyFile);
                            } else {
                                SourcingClarifyFile sourcingClarifyFile = BeanCopyUtil.copyProperties(file, SourcingClarifyFile::new);
                                fileMapper.updateById(sourcingClarifyFile);
                            }
                            return BeanCopyUtil.copyProperties(file, SourcingClarifyFileVO::new);
                        }
                ).collect(Collectors.toList());
            }

        }
        return fileVos;
    }

    /**
     * ?????????????????????????????????
     *
     * @param type
     * @param sourcingId
     */
    @Transactional(rollbackFor = Exception.class)
    protected void generateReplyForSourcing(SourcingClarifyDetailVO detailVO, ClarifySourcingType type, Long sourcingId) {
        IGetSourcingRelateInfo getSourcingVendorInfo = null;
        switch (type) {
            case RFQ:
                getSourcingVendorInfo = bargainSourcingClarifyClient;
                break;
            case TENDER:
                getSourcingVendorInfo = bidClarifyClient;
                break;
            default:
                break;
        }
        if (Objects.isNull(getSourcingVendorInfo)) {
            throw new BaseException("?????????????????????????????????");
        }
        Collection<SourcingVendorInfo> sourcingVendorInfo = getSourcingVendorInfo
                .getSourcingVendorInfo(type.getItemName(), sourcingId);
        if (CollectionUtils.isEmpty(sourcingVendorInfo)) {
            return;
        }
        //????????????
        sourcingVendorInfo.stream()
                .forEach(e -> {
                    SourcingClarifyReply reply = BeanCopyUtil.copyProperties(e, SourcingClarifyReply::new);
                    reply.setClarifyReplyId(IdGenrator.generate());
                    BeanUtils.copyProperties(detailVO.getClarify(), reply, BeanCopyUtil.getNullPropertyNames(reply));
                    replyMapper.insert(reply);
                });
    }

    /**
     * ?????????????????????????????????
     *
     * @param vo
     * @param reply
     */
    private void assignValue(SourcingClarifyReplyDetailVO vo, SourcingClarifyReply reply) {
        vo.setReply(BeanCopyUtil.copyProperties(reply, SourcingClarifyReplyVO::new));
        List<SourcingClarifyFileVO> clarifyFiles = fileMapper.selectList(Wrappers.lambdaQuery(SourcingClarifyFile.class)
                .eq(SourcingClarifyFile::getClarifyId, reply.getClarifyReplyId())
                .eq(SourcingClarifyFile::getClarifyType, ClarifyType.REPLY.getCode())
        ).stream().map(e -> BeanCopyUtil.copyProperties(e, SourcingClarifyFileVO::new)).collect(Collectors.toList());
        vo.setFiles(clarifyFiles);
    }

}
