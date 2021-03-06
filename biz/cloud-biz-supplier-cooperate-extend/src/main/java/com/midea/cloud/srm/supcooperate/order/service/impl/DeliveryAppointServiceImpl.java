package com.midea.cloud.srm.supcooperate.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.neworder.DeliveryAppointStatus;
import com.midea.cloud.common.enums.neworder.DeliveryNoticeStatus;
import com.midea.cloud.common.enums.order.DeliveryApponintStatus;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.dto.DeliveryAppointDetailDTO;
import com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.dto.DeliveryAppointRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.dto.DeliveryAppointSaveRequestDTO;
import com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.dto.DeliveryAppointVO;
import com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.entry.AppointDeliveryNote;
import com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.entry.DeliveryAppoint;
import com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.entry.DeliveryAppointVisitor;
import com.midea.cloud.srm.model.suppliercooperate.deliverynote.entry.DeliveryNote;
import com.midea.cloud.srm.supcooperate.order.mapper.AppointDeliveryNoteMapper;
import com.midea.cloud.srm.supcooperate.order.mapper.DeliveryAppointMapper;
import com.midea.cloud.srm.supcooperate.order.mapper.DeliveryAppointVisitorMapper;
import com.midea.cloud.srm.supcooperate.order.service.IAppointDeliveryNoteService;
import com.midea.cloud.srm.supcooperate.order.service.IDeliveryAppointService;
import com.midea.cloud.srm.supcooperate.order.service.IDeliveryAppointVisitorService;
import com.midea.cloud.srm.supcooperate.order.service.IDeliveryNoteService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ??????????????? ???????????????
 * </pre>
 *
 * @author huangbf3
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/3/28 13:58
 *  ????????????:
 * </pre>
 */
@Service
public class DeliveryAppointServiceImpl extends ServiceImpl<DeliveryAppointMapper, DeliveryAppoint> implements IDeliveryAppointService {
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private DeliveryAppointMapper deliveryAppointMapper;
    @Autowired
    private IDeliveryAppointVisitorService iDeliveryAppointVisitorService;
    @Autowired
    private IAppointDeliveryNoteService iAppointDeliveryNoteService;
    @Autowired
    private AppointDeliveryNoteMapper appointDeliveryNoteMapper;
    @Autowired
    private DeliveryAppointVisitorMapper deliveryAppointVisitorMapper;
    @Resource
    private IDeliveryNoteService deliveryNoteService;

    @Transactional
    @Override
    public void saveOrUpdate(String opt,DeliveryAppoint deliveryAppoint, List<DeliveryAppointVisitor> deliveryAppointVisitors, List<AppointDeliveryNote> appointDeliveryNotes) {
        if(StringUtils.equals("add",opt)){
            Long id = IdGenrator.generate();

            deliveryAppoint.setDeliveryAppointId(id);
            deliveryAppoint.setDeliveryAppointNumber(baseClient.seqGen(SequenceCodeConstant.SEQ_SSC_DELIVERY_APPOINT_NUM));
            deliveryAppoint.setDeliveryAppointStatus(DeliveryApponintStatus.CREATE.name());
        }else{
            DeliveryAppoint checkDeliveryAppoint = deliveryAppointMapper.selectById(deliveryAppoint.getDeliveryAppointId());
            Assert.notNull(checkDeliveryAppoint, "????????????????????????");

            if(!StringUtils.equals(DeliveryApponintStatus.CREATE.name(), checkDeliveryAppoint.getDeliveryAppointStatus())
                    &&!StringUtils.equals(DeliveryApponintStatus.REFUSE.name(), checkDeliveryAppoint.getDeliveryAppointStatus())){
                Assert.isTrue(false, "??????????????????????????????????????????"+(StringUtils.equals(DeliveryApponintStatus.SUBMIT.name(),opt)?"??????":"??????"));
            }
            if(StringUtils.equals(DeliveryApponintStatus.SUBMIT.name(),opt)){
                deliveryAppoint.setDeliveryAppointStatus(DeliveryApponintStatus.SUBMIT.name());
//                deliveryAppoint.se
            }else{//??????????????????????????????????????????????????????
                deliveryAppoint.setDeliveryAppointStatus(DeliveryApponintStatus.CREATE.name());
            }

            QueryWrapper<DeliveryAppointVisitor> deliveryAppointVisitorQueryWrapper = new QueryWrapper<>();
            deliveryAppointVisitorQueryWrapper.eq("DELIVERY_APPOINT_ID", deliveryAppoint.getDeliveryAppointId());
            deliveryAppointVisitorMapper.delete(deliveryAppointVisitorQueryWrapper);

            QueryWrapper<AppointDeliveryNote> appointDeliveryNoteQueryWrapper = new QueryWrapper<>();
            appointDeliveryNoteQueryWrapper.eq("DELIVERY_APPOINT_ID", deliveryAppoint.getDeliveryAppointId());
            appointDeliveryNoteMapper.delete(appointDeliveryNoteQueryWrapper);
        }
        this.saveOrUpdate(deliveryAppoint);

        deliveryAppointVisitors.forEach(item -> {
            item.setDeliveryAppointVisitorId(IdGenrator.generate());
            item.setDeliveryAppointId(deliveryAppoint.getDeliveryAppointId());
        });

        iDeliveryAppointVisitorService.saveBatch(deliveryAppointVisitors);

        appointDeliveryNotes.forEach(item->{
            item.setAppointDeliveryNoteId(IdGenrator.generate());
            item.setDeliveryAppointId(deliveryAppoint.getDeliveryAppointId());
        });
        iAppointDeliveryNoteService.saveBatch(appointDeliveryNotes);
    }

    @Override
    public List<DeliveryAppoint> listPage(DeliveryAppointRequestDTO deliveryAppointRequestDTO) {
        return deliveryAppointMapper.findList(deliveryAppointRequestDTO);
    }

    @Override
    public DeliveryAppointVO getDeliveryAppointById(Long deliveryAppointId) {

        //?????????????????????
        DeliveryAppoint deliveryAppoint = this.getById(deliveryAppointId);
        //??????????????????
        List<DeliveryAppointVisitor> visitorList = iDeliveryAppointVisitorService.list(new QueryWrapper<DeliveryAppointVisitor>(new DeliveryAppointVisitor().setDeliveryAppointId(deliveryAppointId)));
        //???????????????
        List<AppointDeliveryNote> appointDeliveryNoteList = iAppointDeliveryNoteService.getByDeliveryAppointId(deliveryAppointId);

        return new DeliveryAppointVO().setDeliveryAppoint(deliveryAppoint)
                .setAppointDeliveryNotes(appointDeliveryNoteList)
                .setVisitors(visitorList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitBatch(List<Long> ids) {
        //??????
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        for(Long id : ids){
            DeliveryAppointDetailDTO detail = detail(id);
            DeliveryAppointSaveRequestDTO request = transfer(detail);
            checkIfSubmit(request);
        }
        //????????????
        List<DeliveryAppoint> deliveryAppointList = this.listByIds(ids);
        for(DeliveryAppoint deliveryAppoint : deliveryAppointList){
            if(!DeliveryAppointStatus.DRAFT.getValue().equals(deliveryAppoint.getDeliveryAppointStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("[deliveryAppointNumber=%s]????????????????????????,????????????",deliveryAppoint.getDeliveryAppointNumber())));
            }
        }
        //??????????????????
        for(DeliveryAppoint deliveryAppoint : deliveryAppointList){
            deliveryAppoint.setDeliveryAppointStatus(DeliveryAppointStatus.WAITING_CONFIRM.getValue());
        }
        this.updateBatchById(deliveryAppointList);
    }






    /**
     * ??????
     * @param requestDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long temporarySave(DeliveryAppointSaveRequestDTO requestDTO) {
        //??????
        checkIfTemporarySave(requestDTO);
        DeliveryAppoint deliveryAppoint = requestDTO.getDeliveryAppoint();
        if(Objects.isNull(deliveryAppoint.getDeliveryAppointId())){
            //??????
            return add(requestDTO);
        }else{
            //??????
            requestDTO.getDeliveryAppoint().setDeliveryAppointStatus(DeliveryAppointStatus.DRAFT.getValue());
            return update(requestDTO);
        }

    }

    /**
     * ?????????????????????
     * @param requestDTO
     */
    private void checkIfTemporarySave(DeliveryAppointSaveRequestDTO requestDTO){
        DeliveryAppoint deliveryAppoint = requestDTO.getDeliveryAppoint();
        List<AppointDeliveryNote> appointDeliveryNotes = requestDTO.getAppointDeliveryNotes();
        List<DeliveryAppointVisitor> deliveryAppointVisitors = requestDTO.getDeliveryAppointVisitors();
        if(StringUtils.isNotBlank(deliveryAppoint.getDeliveryAppointStatus())){
            if(!DeliveryNoticeStatus.DRAFT.getValue().equals(deliveryAppoint.getDeliveryAppointStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????"));
            }
        }
    }

    /**
     * ??????
     * @param requestDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(DeliveryAppointSaveRequestDTO requestDTO) {
        //??????
        checkIfSubmit(requestDTO);

        DeliveryAppoint deliveryAppoint = requestDTO.getDeliveryAppoint();
        if(Objects.isNull(deliveryAppoint.getDeliveryAppointId())){
            //??????
            deliveryAppoint.setDeliveryAppointStatus(DeliveryAppointStatus.WAITING_CONFIRM.getValue());
            return add(requestDTO);
        }else{
            //??????
            deliveryAppoint.setDeliveryAppointStatus(DeliveryAppointStatus.WAITING_CONFIRM.getValue());
            return update(requestDTO);
        }
    }

    /**
     * ?????????????????????
     * @param requestDTO
     */
    private void checkIfSubmit(DeliveryAppointSaveRequestDTO requestDTO){
        DeliveryAppoint deliveryAppoint = requestDTO.getDeliveryAppoint();
        List<DeliveryAppointVisitor> deliveryAppointVisitors = requestDTO.getDeliveryAppointVisitors();
        List<AppointDeliveryNote> appointDeliveryNotes = requestDTO.getAppointDeliveryNotes();

        //???????????????
        if(StringUtils.isBlank(deliveryAppoint.getVendorCode())){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????"));
        }
        if(StringUtils.isBlank(deliveryAppoint.getOrgCode())){
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????"));
        }
        if(StringUtils.isBlank(deliveryAppoint.getReceiveAddress())){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
        }
        if(StringUtils.isBlank(deliveryAppoint.getReceiveOrderAddress())){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
        }
        if(StringUtils.isBlank(deliveryAppoint.getRespondents())){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
        }
        if(StringUtils.isBlank(deliveryAppoint.getLicensePlate())){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
        }
        if(Objects.isNull(deliveryAppoint.getEntryTime())){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
        }
        if(StringUtils.isBlank(deliveryAppoint.getDeliveryLocation())){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????"));
        }

        //????????????????????????
        if(CollectionUtils.isEmpty(appointDeliveryNotes)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
        }
        //????????????????????????
        if(CollectionUtils.isEmpty(deliveryAppointVisitors)){
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????????????????"));
        }

    }

    /**
     * ??????????????????
     * @param requestDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public Long add(DeliveryAppointSaveRequestDTO requestDTO) {
        DeliveryAppoint deliveryAppoint = requestDTO.getDeliveryAppoint();
        List<DeliveryAppointVisitor> deliveryAppointVisitors = requestDTO.getDeliveryAppointVisitors();
        List<AppointDeliveryNote> appointDeliveryNotes = requestDTO.getAppointDeliveryNotes();

        Long id = IdGenrator.generate();
        String deliveryAppointNumber = baseClient.seqGen(SequenceCodeConstant.SEQ_SSC_DELIVERY_APPOINT_NUM);

        //?????????????????????
        deliveryAppoint.setDeliveryAppointId(id)
                .setDeliveryAppointNumber(deliveryAppointNumber);
        if(StringUtils.isBlank(deliveryAppoint.getDeliveryAppointStatus())){
            deliveryAppoint.setDeliveryAppointStatus(DeliveryAppointStatus.DRAFT.getValue());
        }
        this.save(deliveryAppoint);

        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(appointDeliveryNotes)) {
            //????????????-???????????????
            for(AppointDeliveryNote appointDeliveryNote : appointDeliveryNotes){
                appointDeliveryNote.setAppointDeliveryNoteId(IdGenrator.generate());
                appointDeliveryNote.setDeliveryAppointId(id);
            }
            iAppointDeliveryNoteService.saveBatch(appointDeliveryNotes);
            // ????????????????????? deliveryNoteService
            writebackDeliveryNoteService(appointDeliveryNotes,YesOrNo.YES.getValue());
        }

        //????????????????????????
        for(DeliveryAppointVisitor deliveryAppointVisitor : deliveryAppointVisitors){
            deliveryAppointVisitor.setDeliveryAppointVisitorId(IdGenrator.generate());
            deliveryAppointVisitor.setDeliveryAppointId(id);
        }
        iDeliveryAppointVisitorService.saveBatch(deliveryAppointVisitors);

        return id;
    }

    @Transactional
    public void writebackDeliveryNoteService(List<AppointDeliveryNote> appointDeliveryNotes,String flag) {
        if (!CollectionUtils.isEmpty(appointDeliveryNotes)) {
            List<Long> ids = appointDeliveryNotes.stream().map(AppointDeliveryNote::getDeliveryNoteId).collect(Collectors.toList());
            deliveryNoteService.update(Wrappers.lambdaUpdate(DeliveryNote.class).
                    set(DeliveryNote::getIfCreateDeliveryAppointment, flag).
                    in(DeliveryNote::getDeliveryNoteId,ids));
        }
    }

    /**
     * ??????????????????
     * @param requestDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public Long update(DeliveryAppointSaveRequestDTO requestDTO) {
        DeliveryAppoint deliveryAppoint = requestDTO.getDeliveryAppoint();
        List<DeliveryAppointVisitor> deliveryAppointVisitors = requestDTO.getDeliveryAppointVisitors();
        List<AppointDeliveryNote> appointDeliveryNotes = requestDTO.getAppointDeliveryNotes();

        //?????????????????????
        this.updateById(deliveryAppoint);

        //????????????-???????????????
        QueryWrapper<AppointDeliveryNote> appointDeliveryNoteWrapper = new QueryWrapper<>();
        appointDeliveryNoteWrapper.eq("DELIVERY_APPOINT_ID",deliveryAppoint.getDeliveryAppointId());
        List<AppointDeliveryNote> deliveryNotes = iAppointDeliveryNoteService.list(appointDeliveryNoteWrapper);
        writebackDeliveryNoteService(deliveryNotes,YesOrNo.NO.getValue());
        // // ?????????????????????
        iAppointDeliveryNoteService.remove(appointDeliveryNoteWrapper);
        for(AppointDeliveryNote appointDeliveryNote : appointDeliveryNotes){
            appointDeliveryNote.setAppointDeliveryNoteId(IdGenrator.generate());
            appointDeliveryNote.setDeliveryAppointId(deliveryAppoint.getDeliveryAppointId());
        }
        // ?????????????????????
        writebackDeliveryNoteService(appointDeliveryNotes,YesOrNo.NO.getValue());
        iAppointDeliveryNoteService.saveBatch(appointDeliveryNotes);

        //????????????????????????
        QueryWrapper<DeliveryAppointVisitor> deliveryAppointVisitorWrapper = new QueryWrapper<>();
        deliveryAppointVisitorWrapper.eq("DELIVERY_APPOINT_ID",deliveryAppoint.getDeliveryAppointId());
        iDeliveryAppointVisitorService.remove(deliveryAppointVisitorWrapper);
        for(DeliveryAppointVisitor deliveryAppointVisitor : deliveryAppointVisitors){
            deliveryAppointVisitor.setDeliveryAppointVisitorId(IdGenrator.generate());
            deliveryAppointVisitor.setDeliveryAppointId(deliveryAppoint.getDeliveryAppointId());
        }
        iDeliveryAppointVisitorService.saveBatch(deliveryAppointVisitors);

        return deliveryAppoint.getDeliveryAppointId();
    }

    /**
     * ????????????
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        //??????
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<DeliveryAppoint> deliveryAppointList = this.listByIds(ids);
        for(DeliveryAppoint deliveryAppoint : deliveryAppointList){
            if(!DeliveryAppointStatus.DRAFT.getValue().equals(deliveryAppoint.getDeliveryAppointStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
            }
        }
        //????????????
        this.removeByIds(ids);

        QueryWrapper<AppointDeliveryNote> appointDeliveryNoteWrapper = new QueryWrapper<>();
        appointDeliveryNoteWrapper.in("DELIVERY_APPOINT_ID",ids);
        List<AppointDeliveryNote> deliveryNotes = iAppointDeliveryNoteService.list(appointDeliveryNoteWrapper);
        // ?????????????????????
        writebackDeliveryNoteService(deliveryNotes,YesOrNo.NO.getValue());
        iAppointDeliveryNoteService.remove(appointDeliveryNoteWrapper);

        QueryWrapper<DeliveryAppointVisitor> deliveryAppointVisitorWrapper = new QueryWrapper<>();
        deliveryAppointVisitorWrapper.in("DELIVERY_APPOINT_ID",ids);
        iDeliveryAppointVisitorService.remove(deliveryAppointVisitorWrapper);

    }



    private DeliveryAppointSaveRequestDTO transfer(DeliveryAppointDetailDTO detail){
        DeliveryAppoint deliveryAppoint = new DeliveryAppoint();
        BeanUtils.copyProperties(detail,deliveryAppoint);
        List<AppointDeliveryNote> appointDeliveryNotes = detail.getAppointDeliveryNotes();
        List<DeliveryAppointVisitor> visitors = detail.getVisitors();
        return new DeliveryAppointSaveRequestDTO()
                .setDeliveryAppoint(deliveryAppoint)
                .setAppointDeliveryNotes(appointDeliveryNotes)
                .setDeliveryAppointVisitors(visitors);

    }

    /**
     * ????????????
     * @param id
     * @return
     */
    private DeliveryAppointDetailDTO detail(Long id){
        //?????????????????????
        DeliveryAppoint deliveryAppoint = this.getById(id);
        //?????????????????????
        List<AppointDeliveryNote> appointDeliveryNoteList = iAppointDeliveryNoteService.list(new QueryWrapper<>(new AppointDeliveryNote().setDeliveryAppointId(id)));
        //????????????????????????
        List<DeliveryAppointVisitor> deliveryAppointVisitorList = iDeliveryAppointVisitorService.list(new QueryWrapper<>(new DeliveryAppointVisitor().setDeliveryAppointId(id)));

        DeliveryAppointDetailDTO deliveryAppointDetailDTO = new DeliveryAppointDetailDTO();
        BeanUtils.copyProperties(deliveryAppoint,deliveryAppointDetailDTO);
        deliveryAppointDetailDTO.setAppointDeliveryNotes(appointDeliveryNoteList);
        deliveryAppointDetailDTO.setVisitors(deliveryAppointVisitorList);
        return deliveryAppointDetailDTO;

    }

    /**
     * ????????????????????????
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBatch(List<Long> ids) {
        //??????
        if(CollectionUtils.isEmpty(ids)){
           throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<DeliveryAppoint> deliveryAppointList = this.listByIds(ids);
        for(DeliveryAppoint deliveryAppoint : deliveryAppointList){
            if(!DeliveryAppointStatus.WAITING_CONFIRM.getValue().equals(deliveryAppoint.getDeliveryAppointStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("[deliveryAppointNumber=%s]?????????????????????,?????????",deliveryAppoint.getDeliveryAppointNumber())));
            }
        }
        //????????????
        for(DeliveryAppoint deliveryAppoint : deliveryAppointList){
            deliveryAppoint.setDeliveryAppointStatus(DeliveryAppointStatus.ACCEPT.getValue());
        }
        this.updateBatchById(deliveryAppointList);
    }

    /**
     * ????????????????????????
     * @param requestDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refuseBatch(DeliveryAppointRequestDTO requestDTO) {
        List<Long> ids = requestDTO.getIds();
        String refusedReason = requestDTO.getRefusedReason();
        //??????
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        List<DeliveryAppoint> deliveryAppointList = this.listByIds(ids);
        for(DeliveryAppoint deliveryAppoint : deliveryAppointList){
            if(!DeliveryAppointStatus.WAITING_CONFIRM.getValue().equals(deliveryAppoint.getDeliveryAppointStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("[deliveryAppointNumber=%s]?????????????????????,?????????",deliveryAppoint.getDeliveryAppointNumber())));
            }
        }
        //????????????
        for(DeliveryAppoint deliveryAppoint : deliveryAppointList){
            deliveryAppoint.setDeliveryAppointStatus(DeliveryAppointStatus.REJECT.getValue());
            deliveryAppoint.setRefusedReason(refusedReason);
        }
        this.updateBatchById(deliveryAppointList);
        //todo ????????????
    }
}
