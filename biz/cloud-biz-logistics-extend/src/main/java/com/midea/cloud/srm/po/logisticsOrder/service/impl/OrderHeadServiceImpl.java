package com.midea.cloud.srm.po.logisticsOrder.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.bid.projectmanagement.evaluation.SelectionStatusEnum;
import com.midea.cloud.common.enums.logistics.LogisticsOrderStatus;
import com.midea.cloud.common.enums.logistics.LogisticsOrderTmsStatus;
import com.midea.cloud.common.enums.logistics.OrderSourceFrom;
import com.midea.cloud.common.enums.logistics.TransportModeEnum;
import com.midea.cloud.common.enums.logistics.pr.requirement.LogisticsApplyProcessStatus;
import com.midea.cloud.common.enums.pm.ps.BusinessType;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.BeanCopyUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.bid.service.ILgtBidingService;
import com.midea.cloud.srm.feign.api.ApiClient;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.logistics.template.service.ILogisticsTemplateLineService;
import com.midea.cloud.srm.model.api.interfacelog.dto.InterfaceLogDTO;
import com.midea.cloud.srm.model.base.soap.erp.dto.SoapResponse;
import com.midea.cloud.srm.model.logistics.bid.entity.*;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.bidinitiating.enums.SourceFrom;
import com.midea.cloud.srm.model.logistics.bid.vo.LgtBidInfoVO;
import com.midea.cloud.srm.model.logistics.bid.vo.LgtVendorQuotedInfoVO;
import com.midea.cloud.srm.model.logistics.bid.vo.LgtVendorVO;
import com.midea.cloud.srm.model.logistics.po.order.dto.LogisticsOrderDTO;
import com.midea.cloud.srm.model.logistics.po.order.entity.*;
import com.midea.cloud.srm.model.logistics.po.order.vo.LogisticsOrderVO;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementHead;
import com.midea.cloud.srm.model.logistics.soap.order.entity.*;
import com.midea.cloud.srm.model.logistics.soap.tms.request.LogisticsOrderRequest;
import com.midea.cloud.srm.model.logistics.template.entity.LogisticsTemplateLine;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.po.logisticsOrder.mapper.LogisticsOrderHeadMapper;
import com.midea.cloud.srm.po.logisticsOrder.service.*;
import com.midea.cloud.srm.po.logisticsOrder.soap.service.ITmsLogisticsOrderWsService;
import com.midea.cloud.srm.po.logisticsOrder.soap.service.LogisticsContractRatePtt;
import com.midea.cloud.srm.pr.logisticsRequirement.service.IRequirementHeadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 *  <pre>
 *  ???????????????????????? ???????????????
 * </pre>
 *
 * @author xiexh12@meicloud.com
 * @version 1.00.00
 *
 *  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-12-05 18:50:05
 *  ????????????:
 * </pre>
 */
@Service(value = "LogisticsOrderHeadServiceImpl")
@Slf4j
public class OrderHeadServiceImpl extends ServiceImpl<LogisticsOrderHeadMapper, OrderHead> implements IOrderHeadService {


    @Autowired
    private BaseClient baseClient;

    @Autowired
    private IOrderLineService iOrderLineService;

    @Autowired
    private IOrderFileService iOrderFileService;

    @Autowired
    private IOrderLineContractService iOrderLineContractService;

    @Autowired
    private IOrderLineShipService iOrderLineShipService;

    @Autowired
    private ILogisticsTemplateLineService iLogisticsTemplateLineService;

    @Autowired
    private IRequirementHeadService requirementHeadService;

    @Autowired
    private ILgtBidingService lgtBidingService;

    @Autowired
    private ApiClient apiClient;

    @Value("${SOAP_URL.LGT_ORDER_URL}")
    private String orderToTmsUrl;

    @Autowired
    private ThreadPoolExecutor requestExecutors;

    @Autowired
    private IOrderLineFeeService iOrderLineFeeService;

    @Autowired
    private LogisticsOrderHeadMapper orderHeadMapper;

    @Autowired
    private IOrderRejectRecordService orderRejectRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addOrder(LogisticsOrderDTO orderDTO) {
        log.info("????????????????????????,?????????{}", orderDTO);
        OrderHead orderHead = orderDTO.getOrderHead();
        List<OrderLine> orderLineList = orderDTO.getOrderLineList();
        List<OrderLineContract> orderLineContractList = orderDTO.getOrderLineContractList();
        List<OrderLineShip> orderLineShipList = orderDTO.getOrderLineShipList();
        List<OrderFile> orderFileList = orderDTO.getOrderFileList();

        //?????????????????????
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        //????????????
        String purchaseDepartmentCode = loginAppUser.getCeeaDeptId();
        //????????????
        String purchaseDepartmentName = loginAppUser.getDepartment();

        long id = IdGenrator.generate();
        String status = orderHead.getOrderStatus();
        if(Objects.isNull(status)){
            status = LogisticsOrderStatus.DRAFT.getValue();
        }

        orderHead.setOrderHeadId(id)
                .setOrderHeadNum(baseClient.seqGen(SequenceCodeConstant.SEQ_LOGISTICS_ORDER))
                .setTmsStatus(LogisticsOrderTmsStatus.NOT_SYNC.getValue())
                .setApplyDepartmentCode(purchaseDepartmentCode)
                .setApplyDepartmentName(purchaseDepartmentName)
                .setOrderStatus(status);
        if(StringUtils.isBlank(orderHead.getOrderSourceFrom())){
            orderHead.setOrderSourceFrom(OrderSourceFrom.MANUAL.getItemValue());
        }
        this.save(orderHead);
        // ???????????????????????????
        iOrderLineService.addOrderLineBatch(orderHead, orderLineList);
        // ???????????????????????????
        iOrderFileService.addOrderFileBatch(orderHead, orderFileList);
        // ??????????????????????????????
        iOrderLineContractService.addOrderContractBatch(orderHead, orderLineContractList);
        // ????????????????????????
        iOrderLineShipService.addOrderShipBatch(orderHead, orderLineShipList);
        // ???????????????????????????????????????????????????????????????
        if (!SourceFrom.MANUAL.name().equals(orderHead.getSourceFrom())) {
            requirementHeadService.updateById(new LogisticsRequirementHead()
                    .setRequirementHeadId(Long.parseLong(orderHead.getRequirementHeadId()))
                    .setApplyProcessStatus(LogisticsApplyProcessStatus.FINISHED.getValue())
            );
        }
        return id;
    }

    /**
     * ??????????????????????????????
     * @param orderDTOList
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchSubmitOrder(List<LogisticsOrderDTO> orderDTOList){
        log.info("??????????????????????????????,?????????{}", orderDTOList);
        //??????
        for(LogisticsOrderDTO orderDTO : orderDTOList){
            OrderHead orderHead = orderDTO.getOrderHead();
            List<OrderLine> orderLineList = orderDTO.getOrderLineList();
            checkIfSubmit(orderHead, orderLineList);
        }

        //????????????????????????
        List<Long> result = new LinkedList<>();
        for(LogisticsOrderDTO orderDTO : orderDTOList){
            Long id = submit(orderDTO);
            result.add(id);
        }
        return result;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long modifyOrder(LogisticsOrderDTO orderDTO) {
        OrderHead orderHead = orderDTO.getOrderHead();
        List<OrderLine> orderLineList = orderDTO.getOrderLineList();
        List<OrderLineContract> orderLineContractList = orderDTO.getOrderLineContractList();
        List<OrderFile> orderFileList = orderDTO.getOrderFileList();
        List<OrderLineShip> orderLineShipList = orderDTO.getOrderLineShipList();

        OrderHead head = this.getById(orderHead.getOrderHeadId());
        this.updateById(orderHead);
        // ???????????????????????????
        iOrderLineService.updateBatch(orderHead, orderLineList);
        // ??????????????????????????????
        iOrderLineContractService.updateBatch(orderHead, orderLineContractList);
        // ??????????????????????????????
        iOrderFileService.updateOrderAttachBatch(orderHead, orderFileList);
        //??????????????????????????????
        iOrderLineShipService.updateOrderLineShipBatch(orderHead,orderLineShipList);
        return head.getOrderHeadId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByHeadId(Long orderHeadId) {
        log.info("????????????????????????????????????{}",orderHeadId);
        OrderHead orderHead = this.getById(orderHeadId);
        if(Objects.isNull(orderHead)){
            throw new BaseException(String.format("????????????????????????????????????id???%s",orderHead));
        }
        //???????????????????????????
        if (LogisticsOrderStatus.DRAFT.getValue().equals(orderHead.getOrderStatus())) {
            this.removeById(orderHeadId);
            List<OrderLine> rls = iOrderLineService.list(new QueryWrapper<>(new OrderLine().setOrderHeadId(orderHeadId)));
            iOrderLineService.removeBatch(orderHeadId);
            iOrderLineContractService.remove(new QueryWrapper<>(new OrderLineContract().setOrderHeadId(orderHeadId)));
            iOrderFileService.remove(new QueryWrapper<>(new OrderFile().setOrderHeadId(orderHeadId)));
        } else {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????"));
        }
    }

    @Override
    public PageInfo<OrderHead> listPage(OrderHead orderHead) {
        PageUtil.startPage(orderHead.getPageNum(), orderHead.getPageSize());
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if(UserType.VENDOR.name().equals(loginAppUser.getUserType())){
            //?????????????????????????????????
            orderHead.setLoginCompanyId(loginAppUser.getCompanyId());
        }
        return new PageInfo<OrderHead>(orderHeadMapper.list(orderHead));
    }


    @Override
    public LogisticsOrderVO getByHeadId(Long orderHeadId) {
        Assert.notNull(orderHeadId, "???????????????ID????????????");
        LogisticsOrderVO vo = new LogisticsOrderVO();
        vo.setOrderHead(this.getById(orderHeadId));
        //???????????????
        List<OrderLine> orderLineList = iOrderLineService.listBatch(orderHeadId);
        vo.setOrderLineList(orderLineList);
        //??????????????????
        List<OrderFile> orderFileList = iOrderFileService.list(new QueryWrapper<>(new OrderFile()
                .setOrderHeadId(orderHeadId)));
        vo.setOrderFileList(orderFileList);
        //?????????????????????
        List<OrderLineContract> orderLineContractList = iOrderLineContractService.list(new QueryWrapper<>(new OrderLineContract()
                .setOrderHeadId(orderHeadId)));
        vo.setOrderLineContractList(orderLineContractList);
        //??????????????????
        List<OrderLineShip> orderLineShipList = iOrderLineShipService.list(new QueryWrapper<>(new OrderLineShip().setOrderHeadId(orderHeadId)));
        vo.setOrderLineShipList(orderLineShipList);
        //????????????????????????
        List<OrderRejectRecord> orderRejectRecordList = orderRejectRecordService.list(new QueryWrapper<>(new OrderRejectRecord().setOrderHeadId(orderHeadId)));
        vo.setOrderRejectRecordList(orderRejectRecordList);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitApproval(LogisticsOrderDTO orderDTO) {
        log.info("????????????????????????????????????{}",orderDTO.toString());
        OrderHead orderHead = orderDTO.getOrderHead();
        List<OrderLine> orderLineList = orderDTO.getOrderLineList();
        //??????
        checkIfSubmit(orderHead,orderLineList);
        //????????????????????????
        Long id = submit(orderDTO);
        return id;

    }

    /**
     * ????????????????????????
     * @param orderDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(LogisticsOrderDTO orderDTO){
        OrderHead orderHead = orderDTO.getOrderHead();

        String status = null;

        //????????????????????????
        String ifContractMaintain = null;
        //????????????????????????
        String ifNeedVendorComfirm = null;

        List<OrderLineContract> orderLineContractList = orderDTO.getOrderLineContractList();
        if(CollectionUtils.isEmpty(orderLineContractList)){
            ifContractMaintain = "N";
        }else{
            ifContractMaintain = "Y";
        }
        ifNeedVendorComfirm = orderHead.getIfNeedVendorComfirm();

        if("N".equals(ifContractMaintain)){
            status = LogisticsOrderStatus.WAITING_CONFIRM.getValue();
        }else {
            if("Y".equals(ifNeedVendorComfirm)){
                status = LogisticsOrderStatus.WAITING_VENDOR_CONFIRM.getValue();
            }else{
                status = LogisticsOrderStatus.COMPLETED.getValue();
            }
        }

        orderHead.setOrderStatus(status);
        if (orderHead.getOrderHeadId() == null) {
            //??????
            this.addOrder(orderDTO);
        } else {
            //??????
            this.modifyOrder(orderDTO);
        }

        //?????????????????????????????????????????????
        if(SourceFrom.PURCHASE_REQUEST.name().equals(orderHead.getSourceFrom())){
            if(LogisticsOrderStatus.COMPLETED.getValue().equals(status)){
                LogisticsRequirementHead requirementHead = requirementHeadService.getById(orderHead.getRequirementHeadId());
                requirementHeadService.updateById(requirementHead.setApplyProcessStatus(LogisticsApplyProcessStatus.FINISHED.getValue()));
            }
        }

        //??????Tms
        if(LogisticsOrderStatus.COMPLETED.getValue().equals(status)){
            requestExecutors.submit(new Thread(() -> {
                syncTmsLongi(orderHead.getOrderHeadId());
            }));
        }
        return orderHead.getOrderHeadId();
    }


    /**
     * ?????????
     * 1.???????????????
     * (1)???????????????????????????
     *   ????????????id
     *   ????????????
     *   ????????????
     *   ????????????
     *   ????????????
     *   ????????????
     *   ????????????????????????
     *   ????????????????????????
     *   ????????????????????????
     *   ????????????id
     * (2)???????????????????????????
     * 2.?????????????????????
     * (1)????????????????????????
     * (2)????????????????????????????????????
     * 3.????????????
     * (1)???????????????????????????,??????????????????????????????
     * @param orderHead
     * @param orderLineList
     */
    public void checkIfSubmit(OrderHead orderHead,List<OrderLine> orderLineList){
        Assert.notNull(orderHead, LocaleHandler.getLocaleMsg("???????????????????????????"));
        Assert.notEmpty(orderLineList, LocaleHandler.getLocaleMsg("???????????????????????????????????????"));
        String errorMsg = "%s??????*?????????????????????????????????????????????";
        //??????????????????????????????????????????
        Assert.notNull(orderHead.getTemplateHeadId(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.hasText(orderHead.getBusinessModeCode(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.hasText(orderHead.getTransportModeCode(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.hasText(orderHead.getBusinessType(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????????????????")));
        Assert.hasText(orderHead.getUnit(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"??????")));
        Assert.notNull(orderHead.getProjectTotal(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????")));
        Assert.notNull(orderHead.getPriceStartDate(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????????????????")));
        Assert.notNull(orderHead.getPriceEndDate(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????????????????")));
        Assert.hasText(orderHead.getIfNeedVendorComfirm(), LocaleHandler.getLocaleMsg(String.format(errorMsg,"????????????????????????")));
//        Assert.hasText(orderHead.getNumberComment(),"??????????????????");
        //??????????????????????????????
        if(SourceFrom.PURCHASE_REQUEST.name().equals(orderHead.getSourceFrom())){
            Assert.notNull(orderHead.getRequirementHeadId(), LocaleHandler.getLocaleMsg("????????????id??????"));
            //????????????
            LogisticsRequirementHead requirementHead = requirementHeadService.getById(orderHead.getRequirementHeadId());
            if(Objects.isNull(requirementHead)){
                throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????,requirementHeadId = [%s]",orderHead.getRequirementHeadId()));
            }

            //???????????????????????????????????????
            checkIfRequirementUsed(requirementHead);
        }

        //ii.	???????????????????????????????????????????????????????????????
        if(BusinessType.BUSINESS_TYPE_40.getValue().equals(orderHead.getBusinessType()) && StringUtils.isBlank(orderHead.getServiceProjectCode())){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????????????????????????????????????????"));
        }

        //??????????????????
        checkLineByTemplate(orderHead,orderLineList);

    }

    /**
     * ????????????????????????????????????
     * @param requirementHead
     */
    private void checkIfRequirementUsed(LogisticsRequirementHead requirementHead){
        Long requirementHeadId = requirementHead.getRequirementHeadId();
        QueryWrapper<OrderHead> orderHeadQueryWrapper = new QueryWrapper<>();
        orderHeadQueryWrapper.eq("REQUIREMENT_HEAD_ID",requirementHeadId);
        int count = this.count(orderHeadQueryWrapper);
        if(count > 0){
            throw new BaseException(LocaleHandler.getLocaleMsg(String.format("requirementHeadNum=[%s]???????????????????????????,?????????????????????",requirementHead.getRequirementHeadNum())));
        }
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
    public void checkLineByTemplate(OrderHead requirementHead,List<OrderLine> requirementLineList){
        String templateName = requirementHead.getTemplateName();
        Assert.notNull(templateName,"??????????????????????????????!");

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

    public Object doCheck(String fieldCode,OrderLine line) throws IllegalAccessException {

        Field[] declaredFields = OrderLine.class.getDeclaredFields();
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
    public void updateApproved(Long orderHeadId, String auditStatus) {
        // ????????????
        this.updateById(new OrderHead().setOrderHeadId(orderHeadId)
                .setOrderStatus(auditStatus));

        //TODO:????????????????????????????????????????????????????????????TMS

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitVendorConfirm(Long orderHeadId) {
        //i.	????????????????????????????????????????????????
        //ii.	????????????????????????????????????????????????
        //iii.	????????????????????????????????????????????????
        //iv.	???????????????????????????????????????????????????????????????????????????????????????????????????
//        Objects.requireNonNull(orderHeadId);
        OrderHead byId = this.getById(orderHeadId);
        if(Objects.isNull(byId)){
            throw new BaseException(String.format("????????????????????????????????????id???%s",orderHeadId));
        }
//        if(Objects.equals(byId.getOrderStatus(),LogisticsOrderStatus.COMPLETED.getValue()) && Objects.equals(byId.getTmsStatus(), LogisticsOrderTmsStatus.SUCCESS_SYNC)){
//            throw new BaseException(String.format("???????????????????????????????????????????????????????????????????????????????????????????????????"));
//        }

        // ????????????
        this.updateById(new OrderHead().setOrderHeadId(orderHeadId)
                .setOrderStatus(LogisticsOrderStatus.COMPLETED.getValue()));

        //?????????????????????????????????????????????
        LogisticsRequirementHead requirementHead = requirementHeadService.getById(byId.getRequirementHeadId());
        requirementHeadService.updateById(requirementHead.setApplyProcessStatus(LogisticsApplyProcessStatus.FINISHED.getValue()));
        //??????tms
        requestExecutors.submit(new Thread(() -> {
            syncTmsLongi(orderHeadId);
        }));
//        syncTms(orderHeadId);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refuse(OrderHead orderHead) {
        Long orderHeadId = orderHead.getOrderHeadId();
        String rejectReason = orderHead.getRejectReason();
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        //?????????????????????
        checkIfRefuse(orderHead);
        // ????????????
        this.updateById(new OrderHead().setOrderHeadId(orderHeadId)
                .setIfRejected("Y")
                .setOrderStatus(LogisticsOrderStatus.DRAFT.getValue()));
        //??????????????????
        orderRejectRecordService.save(new OrderRejectRecord()
                .setOrderRejectRecordId(IdGenrator.generate())
                .setOrderHeadId(orderHeadId)
                .setRejectReason(rejectReason)
                .setRejectNickname(loginAppUser.getNickname())
                .setRejectUsername(loginAppUser.getUsername())
        );
    }

    /**
     * ?????????????????????
     * @param orderHead
     */
    private void checkIfRefuse(OrderHead orderHead){
        Long orderHeadId = orderHead.getOrderHeadId();

        //??????
        if(Objects.isNull(orderHead.getOrderHeadId())){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        if(StringUtils.isBlank(orderHead.getRejectReason())){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????"));
        }
        OrderHead oh = this.getById(orderHeadId);
        if(Objects.isNull(oh)){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????,???????????????"));
        }
        if(!LogisticsOrderStatus.WAITING_VENDOR_CONFIRM.getValue().equals(oh.getOrderStatus())){
            throw new BaseException(LocaleHandler.getLocaleMsg(String.format("????????????[%s]?????????[%s],????????????",oh.getOrderHeadNum(),oh.getOrderStatus())));
        }
    }

    /**
     * ??????
     * @param orderDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long temporarySave(LogisticsOrderDTO orderDTO) {
        //?????????????????????
        checkIfTemporarySave(orderDTO);

        Long orderHeadId = null;
        OrderHead orderHead = orderDTO.getOrderHead();
        if(Objects.isNull(orderHead.getOrderHeadId())){
            //??????
            orderHeadId = addOrder(orderDTO);
        }else{
            //??????
            orderHeadId = modifyOrder(orderDTO);
        }
        return orderHeadId;
    }

    /**
     * ??????????????????tms
     * @param orderHeadId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTms(Long orderHeadId) {

        LogisticsOrderVO logisticsOrderVO = this.getByHeadId(orderHeadId);

        //?????????????????????
        checkIfSync(logisticsOrderVO);

        //????????????
        LogisticsOrderRequest logisticsOrderRequest = null;
        //????????????
        SoapResponse response = null;
        //????????????
        Map<String, String> errorMsg = null;
        try {
            logisticsOrderRequest = buildRequest(logisticsOrderVO);
            log.info("?????????????????????" + logisticsOrderRequest);
//            orderToTmsUrl = "http://10.16.87.99:8842/registerService/TmsLogisticsOrderWsServiceImpl?wsdl";
            //???????????????????????????
            String address = orderToTmsUrl;
            //????????????
            JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
            jaxWsProxyFactoryBean.setAddress(address);
            //??????????????????
            jaxWsProxyFactoryBean.setServiceClass(ITmsLogisticsOrderWsService.class);
            //??????????????????????????????
            ITmsLogisticsOrderWsService service = (ITmsLogisticsOrderWsService) jaxWsProxyFactoryBean.create();
            //??????tms
            response = service.execute(logisticsOrderRequest);
        } catch (Exception e) {
            // ??????????????????
            String stackTrace = Arrays.toString(e.getStackTrace());
            // ????????????
            String message = e.getMessage();
            errorMsg = new HashMap<>();
            errorMsg.put("message", e.getClass().getName() + ": " + message);
            errorMsg.put("stackTrace", stackTrace);
            log.error(e.getMessage());
            throw e;

        } finally {
            try {
                //??????????????????
                saveInterfaceLog(logisticsOrderRequest, response, errorMsg, logisticsOrderVO);
                //??????????????????tms,????????????
                updateAfterTmsPush(response,orderHeadId);
            } catch (Exception e){
                log.error(e.getMessage());
            }

        }
    }

    /**
     * ??????????????????tms(????????????)
     * @param orderHeadId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTmsLongi(Long orderHeadId){
        LogisticsOrderVO logisticsOrderVO = this.getByHeadId(orderHeadId);
        Assert.notNull(logisticsOrderVO,"?????????????????????!!!");
        /**
         * ???????????????, ??????????????????
         */
        String transportModeCode = logisticsOrderVO.getOrderHead().getTransportModeCode();
        if(TransportModeEnum.LAND_TRANSPORT.name().equals(transportModeCode) ||
                TransportModeEnum.RAILWAY_TRANSPORT.name().equals(transportModeCode)){
            logisticsOrderVO.setOrderLineShipList(null);
        }

        //?????????????????????
        checkIfSync(logisticsOrderVO);

        //????????????
        GetSrmTariffInfo srmTariffInfo = null;
        //????????????
        GetSrmTariffInfoResponse responseBody = null;
        //????????????
        Map<String, String> errorMsg = null;
        try {
            srmTariffInfo = buildRequestLongi(logisticsOrderVO);
            log.info("?????????????????????" + srmTariffInfo);
//            orderToTmsUrl = "http://soatest.longi.com:8011/TMSSB/Srm/LogisticsContractRate/ProxyServices/TmsAcceptLogisticsContractRateSoapProxy?wsdl";
            //???????????????????????????
            String address = orderToTmsUrl;
            //????????????
            JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
            jaxWsProxyFactoryBean.setAddress(address);
            //??????????????????
            jaxWsProxyFactoryBean.setServiceClass(LogisticsContractRatePtt.class);
            //??????????????????????????????
            LogisticsContractRatePtt logisticsContractRatePtt = (LogisticsContractRatePtt) jaxWsProxyFactoryBean.create();
            //??????tms
            responseBody = logisticsContractRatePtt.logisticsContractRate(srmTariffInfo);

        } catch (Exception e) {
            // ??????????????????
            String stackTrace = Arrays.toString(e.getStackTrace());
            // ????????????
            String message = e.getMessage();
            errorMsg = new HashMap<>();
            errorMsg.put("message", e.getClass().getName() + ": " + message);
            errorMsg.put("stackTrace", stackTrace);
            log.error(e.getMessage());
            throw e;
        } finally {
            try {
                //??????????????????
                saveInterfaceLogLongi(srmTariffInfo, responseBody, errorMsg, logisticsOrderVO);
                //??????????????????tms,????????????
                updateAfterTmsPushLongi(responseBody,orderHeadId);
            } catch (Exception e){
                log.error(e.getMessage());
            }
        }
    }

    /**
     * ????????????????????????tms?????????
     * @param logisticsOrderVO
     * @return
     */
    private GetSrmTariffInfo buildRequestLongi(LogisticsOrderVO logisticsOrderVO){
        OrderHead orderHead = logisticsOrderVO.getOrderHead();
        List<OrderLine> orderLineList = logisticsOrderVO.getOrderLineList();
        List<OrderLineShip> orderLineShipList = logisticsOrderVO.getOrderLineShipList();
        List<OrderLineContract> orderLineContractList = logisticsOrderVO.getOrderLineContractList();

        //???????????????????????????  LogisticsRequirementHead(?????????????????????????????????,?????????????????????id)
        //????????????
        String contractType = null;
        if(Objects.nonNull(orderHead.getRequirementHeadId())){
            LogisticsRequirementHead logisticsRequirementHead = requirementHeadService.getById(orderHead.getRequirementHeadId());
            if(Objects.isNull(logisticsRequirementHead)){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("orderHeadId=[%s],????????????????????????,requirementHeadId=[%s]",orderHead.getOrderHeadId(),orderHead.getRequirementHeadId())));
            }
            contractType = logisticsRequirementHead.getContractType();
        }

        String settlementMethod = null;
        if(!CollectionUtils.isEmpty(orderLineContractList)){
            settlementMethod = orderLineContractList.stream().map(item -> item.getPaymentMethod()).collect(Collectors.joining("/"));
        }

        //?????????????????????
        Customerinfo customerinfo = new Customerinfo();
        customerinfo.setOrderHeadNum(orderHead.getOrderHeadNum());
        customerinfo.setVendorCode(orderHead.getVendorCode());
        customerinfo.setPriceStartDate(String.valueOf(orderHead.getPriceStartDate()));
        customerinfo.setPriceEndDate(String.valueOf(orderHead.getPriceEndDate()));
        customerinfo.setSettlementMethod(settlementMethod); //????????????
        customerinfo.setContractType(contractType);   //????????????
        customerinfo.setBusinessModeCode(orderHead.getBusinessModeCode());
        customerinfo.setTransportModeCode(orderHead.getTransportModeCode());
        customerinfo.setStatus("Y"); //?????????
        customerinfo.setSourceSystem("SRM");

        //??????????????????
        List<Road> roadList = new LinkedList<>();
        if(!CollectionUtils.isEmpty(orderLineList)){
            for(OrderLine orderLine : orderLineList){
                Road road = new Road();

                road.setOrderHeadNum(orderLine.getOrderHeadNum());
                road.setRowLineNum(String.valueOf(orderLine.getRowNum()));
                road.setFromCountry(orderLine.getFromCountry());
                road.setFromProvince(orderLine.getFromProvince());
                road.setFromCity(orderLine.getFromCity());
                road.setFromCounty(orderLine.getFromCounty());
                road.setFromPlace(orderLine.getFromPlace());
                road.setToCountry(orderLine.getToCountry());
                road.setToProvinceCode(orderLine.getToProvinceCode());
                road.setToCountyCode(orderLine.getToCountyCode());
                road.setToPlace(orderLine.getToPlace());
                road.setVendorOrderNum("1"); //????????????
                road.setTransportDistance(orderLine.getTransportDistance());
                road.setTransportModeCode(orderHead.getTransportModeCode()); //????????????
                road.setServiceProjectCode(orderHead.getServiceProjectCode());  //??????
                road.setStatus("Y");
                road.setRealTime(null); //????????????
                road.setPriceStartDate(String.valueOf(orderHead.getPriceStartDate())); //?????????
                road.setPriceEndDate(String.valueOf(orderHead.getPriceEndDate())); //?????????
                road.setWholeArk(orderLine.getWholeArk());
                road.setFromPort(orderLine.getFromPortCode());
                road.setToPort(orderLine.getToPortCode());
                road.setUnProjectFlag("N"); //??????????????????
                // todo ???????????????????????????

                //???????????????
                List<Rate> rateList = new LinkedList<>();
                List<OrderLineFee> orderLineFeeList = orderLine.getOrderLineFeeList();
                if(!CollectionUtils.isEmpty(orderLineFeeList)){
                    for(OrderLineFee orderLineFee : orderLineFeeList){
                        Rate rate = new Rate();
                        rate.setOrderHeadNum(orderLineFee.getOrderHeadNum());
                        rate.setRowLineNum(String.valueOf(orderLine.getRowNum()));
                        rate.setRowNum(String.valueOf(orderLineFee.getRowNum()));
                        rate.setExpenseItem(orderLineFee.getExpenseItem());
                        rate.setChargeMethod(orderLineFee.getChargeMethod());
                        rate.setChargeUnit(orderLineFee.getChargeUnit());
                        rate.setMinCost(orderLineFee.getMinCost());
                        rate.setMaxCost(orderLineFee.getMaxCost());
                        rate.setExpense(String.valueOf(orderLineFee.getExpense()));
                        rate.setCurrency(orderLineFee.getCurrency());
                        rate.setIfBack(orderLine.getIfBack()); //????????????
                        rate.setLeg(orderLineFee.getLeg());
                        rate.setTaxRate(String.valueOf(orderHead.getTaxRate()));
                        rateList.add(rate);
                    }
                }
                road.setRate(rateList);
                roadList.add(road);
            }
        }

        //??????????????????
        List<Boat> boatList = new LinkedList<>();
        if(!CollectionUtils.isEmpty(orderLineShipList)){
            for(OrderLineShip orderLineShip : orderLineShipList){
                Boat boat = new Boat();
                boat.setOrderHeadNum(orderLineShip.getOrderHeadNum());
                boat.setRowNum(String.valueOf(orderLineShip.getRowNum()));
                boat.setFromPort(orderLineShip.getFromPortCode());
                boat.setToPort(orderLineShip.getToPortCode());
                boat.setWholeArk(orderLineShip.getWholeArk());
                boat.setMon(String.valueOf(orderLineShip.getMon()));
                boat.setTue(String.valueOf(orderLineShip.getTue()));
                boat.setWed(String.valueOf(orderLineShip.getWed()));
                boat.setThu(String.valueOf(orderLineShip.getThu()));
                boat.setFri(String.valueOf(orderLineShip.getFri()));
                boat.setSat(String.valueOf(orderLineShip.getSat()));
                boat.setSun(String.valueOf(orderLineShip.getSun()));
                boat.setTransitTime(String.valueOf(orderLineShip.getTransitTime()));
                boat.setCompany(orderLineShip.getShipCompanyName());
                boat.setTransferPort(orderLineShip.getTransferPort());
                boatList.add(boat);
            }
        }

        customerinfo.setRoad(roadList);
        customerinfo.setBoat(boatList);

        GetSrmTariffInfo srmTariffInfo = new GetSrmTariffInfo();
        srmTariffInfo.setEsbInfo(new EsbInfo());
        srmTariffInfo.setHead(new Head());
        srmTariffInfo.setRequestInfo(new LinkedList<Customerinfo>(){{
            add(customerinfo);
        }});
        return srmTariffInfo;
    }

    /**
     * ??????????????????
     * @param srmTariffInfo
     * @param response
     * @param errorMsg
     * @param logisticsOrderVO
     */
    private void saveInterfaceLogLongi(GetSrmTariffInfo srmTariffInfo,GetSrmTariffInfoResponse response,Map<String, String> errorMsg,LogisticsOrderVO logisticsOrderVO){
        List<Customerinfo> requestInfo = srmTariffInfo.getRequestInfo();
        InterfaceLogDTO interfaceLogDTO = new InterfaceLogDTO();
        if(!CollectionUtils.isEmpty(requestInfo)){
            try {
                interfaceLogDTO.setServiceInfo(JSON.toJSONString(requestInfo));
            } catch (Exception e) {
                log.error(String.format("????????????????????????TMS??????????????????????????????[%s]",logisticsOrderVO.getOrderHead().getOrderHeadNum()));
            }
        }
        interfaceLogDTO.setCreationDateBegin(new Date());
        interfaceLogDTO.setServiceName("??????????????????ERP????????????");
        interfaceLogDTO.setServiceType("WEBSERVICE"); //????????????
        interfaceLogDTO.setType("SEND"); // ????????????
        interfaceLogDTO.setBillType("??????????????????"); // ????????????
        interfaceLogDTO.setBillId(logisticsOrderVO.getOrderHead().getOrderHeadNum());
        interfaceLogDTO.setTargetSys("TMS");
        interfaceLogDTO.setReturnInfo(JSON.toJSONString(response));
        interfaceLogDTO.setStatus("SUCCESS");

        if(Objects.nonNull(errorMsg)){
            interfaceLogDTO.setErrorInfo(JSON.toJSONString(errorMsg));
            interfaceLogDTO.setStatus("FAIL");
        }

        if(Objects.isNull(response) ||
                Objects.isNull(response.getGetSrmTariffInfo()) ||
                Objects.isNull(response.getGetSrmTariffInfo().getEsbInfo()) ||
                !"S".equals(response.getGetSrmTariffInfo().getEsbInfo().getReturnStatus())
        ){
            if(Objects.nonNull(response) &&
                    Objects.nonNull(response.getGetSrmTariffInfo()) &&
                    Objects.nonNull(response.getGetSrmTariffInfo().getEsbInfo()) &&
                    StringUtils.isNotBlank(response.getGetSrmTariffInfo().getEsbInfo().getReturnMsg())
            ){
                interfaceLogDTO.setErrorInfo(response.getGetSrmTariffInfo().getEsbInfo().getReturnMsg());
            }
            interfaceLogDTO.setStatus("FAIL");
        }
        interfaceLogDTO.setCreationDateEnd(new Date());
        interfaceLogDTO.setFinishDate(new Date());
        try {
            apiClient.createInterfaceLogForAnon(interfaceLogDTO);
        } catch (Exception e){
            log.error("??????<????????????????????????TMS>????????????{}" + e);
        }

    }

    /**
     * ????????????????????????
     * @param response
     * @param orderHeadId
     */
    private void updateAfterTmsPushLongi(GetSrmTariffInfoResponse response,Long orderHeadId){
        String tmsInfo = null;
        if(Objects.nonNull(response) &&
                Objects.nonNull(response.getGetSrmTariffInfo()) &&
                Objects.nonNull(response.getGetSrmTariffInfo().getEsbInfo())
        ){
            tmsInfo = response.getGetSrmTariffInfo().getEsbInfo().getReturnMsg();
        }
        if (Objects.isNull(response) ||
                Objects.isNull(response.getGetSrmTariffInfo()) ||
                Objects.isNull(response.getGetSrmTariffInfo().getEsbInfo()) ||
                !"S".equals(response.getGetSrmTariffInfo().getEsbInfo().getReturnStatus())
        ) {
            this.updateById(new OrderHead()
                    .setOrderHeadId(orderHeadId)
                    .setTmsInfo(tmsInfo) //todo ?????????????????????
                    .setTmsStatus(LogisticsOrderTmsStatus.FAIL_SYNC.getValue()));
        } else {
            this.updateById(new OrderHead()
                    .setOrderHeadId(orderHeadId)
                    .setTmsInfo(tmsInfo) //todo ?????????????????????
                    .setTmsStatus(LogisticsOrderTmsStatus.SUCCESS_SYNC.getValue()));
        }
    }



    /**
     * ??????????????????????????????
     * @param bidingId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderHead> bidingToOrders(Long bidingId) {
        //????????????
        if(Objects.isNull(bidingId)){
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????[bidingId]"));
        }
        //??????????????????
        LgtBidInfoVO lgtBidInfoVO = lgtBidingService.detail(bidingId);
        //????????? -> ??????????????????
        List<LogisticsOrderDTO> orderDTOList = buildOrders(lgtBidInfoVO);
        //???????????? todo ??????????????????,????????????????????????????????????
        batchSubmitOrder(orderDTOList);

        return orderDTOList.stream().map(item -> item.getOrderHead()).collect(Collectors.toList());
    }

    /**
     * ??????????????????
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCancel(List<Long> ids) {
        //????????????
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????[ids]"));
        }
        List<OrderHead> orderHeadList = this.listByIds(ids);
        if(CollectionUtils.isEmpty(orderHeadList)){
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????,?????????"));
        }
        //???????????????????????????????????????????????????????????????????????????
        for(OrderHead orderHead : orderHeadList){
            boolean cancelFlag = LogisticsOrderStatus.COMPLETED.getValue().equals(orderHead.getOrderStatus()) &&
                    LogisticsOrderTmsStatus.FAIL_SYNC.getValue().equals(orderHead.getTmsStatus());
            if(!cancelFlag){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("?????????[%s]????????????,?????????",orderHead.getOrderHeadNum())));
            }
        }

        //??????????????????
        for(OrderHead orderHead : orderHeadList){
            orderHead.setOrderStatus(LogisticsOrderStatus.CANCEL.getValue());
        }
        this.updateBatchById(orderHeadList);
    }

    /**
     * ??????????????????
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        //????????????
        if(CollectionUtils.isEmpty(ids)){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????[ids]"));
        }
        List<OrderHead> orderHeadList = this.listByIds(ids);
        if(CollectionUtils.isEmpty(orderHeadList)){
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????,?????????"));
        }
        //??????????????????????????????
        for(OrderHead orderHead : orderHeadList){
            boolean deleteFlag = LogisticsOrderStatus.DRAFT.getValue().equals(orderHead.getOrderStatus());
            if(!deleteFlag){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("?????????[%s]????????????,?????????",orderHead.getOrderHeadNum())));
            }
        }
        //??????????????????
        deleteByHeadIds(ids);
    }

    /**
     * ??????id??????????????????
     * @param ids
     */
    public void deleteByHeadIds(List<Long> ids){
        //?????????????????????
        this.removeByIds(ids);
        //?????????????????????
        QueryWrapper<OrderLine> orderLineWrapper = new QueryWrapper<>();
        orderLineWrapper.in("ORDER_HEAD_ID",ids);
        iOrderLineService.remove(orderLineWrapper);
        //??????????????????
        QueryWrapper<OrderLineContract> orderLineContractWrapper = new QueryWrapper<>();
        orderLineContractWrapper.in("ORDER_HEAD_ID",ids);
        iOrderLineContractService.remove(orderLineContractWrapper);
        //??????????????????
        QueryWrapper<OrderLineShip> orderLineShipWrapper = new QueryWrapper<>();
        orderLineShipWrapper.in("ORDER_HEAD_ID",ids);
        iOrderLineShipService.remove(orderLineShipWrapper);
        //?????????????????????
        QueryWrapper<OrderLineFee> orderLineFeeWrapper = new QueryWrapper<>();
        orderLineFeeWrapper.in("ORDER_HEAD_ID",ids);
        iOrderLineFeeService.remove(orderLineFeeWrapper);
        //??????????????????
        QueryWrapper<OrderFile> orderFileWrapper = new QueryWrapper<>();
        orderFileWrapper.in("ORDER_HEAD_ID",ids);
        iOrderFileService.remove(orderFileWrapper);
    }

    /**
     * ?????????????????????????????????????????????
     * @param lgtBidInfoVO
     * @return
     */
    private List<LogisticsOrderDTO> buildOrders(LgtBidInfoVO lgtBidInfoVO){
        List<LogisticsOrderDTO> result = new LinkedList<>();
        //?????????????????????
        LgtBiding biding = lgtBidInfoVO.getBiding();
        //????????????
        List<LgtVendorQuotedSum> vendorQuotedSumList = lgtBidInfoVO.getVendorQuotedSumList();
        //???????????????????????????
        List<LgtVendorQuotedSum> vendorQuotedSums = vendorQuotedSumList.stream().filter(item -> SelectionStatusEnum.WIN.getValue().equals(item.getBidResult()))
                .collect(Collectors.toList());

        List<Long> quotedHeadIds = vendorQuotedSumList.stream()
                .filter(item -> SelectionStatusEnum.WIN.getValue().equals(item.getBidResult()))
                .map(item -> item.getQuotedHeadId())
                .collect(Collectors.toList());
        //????????????????????????????????????
        List<LgtVendorQuotedInfoVO> vendorQuotedInfoList = lgtBidInfoVO.getVendorQuotedInfoList().stream().filter(item -> quotedHeadIds.contains(item.getVendorQuotedHead().getQuotedHeadId())).collect(Collectors.toList());
        //??????????????????????????? ???????????????????????????
        Map<String,LgtVendorQuotedInfoVO> map = vendorQuotedInfoList.stream().collect(Collectors.toMap(item -> item.getVendorQuotedHead().getVendorCode(),item -> item));

        //???????????????????????????
        List<LgtVendorVO> vendorList = lgtBidInfoVO.getVendorList();
        //??????????????????????????? ??????????????????
        Map<String,LgtVendorVO> map2 = vendorList.stream().collect(Collectors.toMap(item -> item.getBidVendor().getVendorCode(),item -> item));

        for(Map.Entry<String,LgtVendorQuotedInfoVO> entry : map.entrySet()){
            //???????????????
            String vendorCode = entry.getKey();
            //?????????????????????
            LgtVendorQuotedInfoVO vendorQuotedInfoVO = entry.getValue();
            //????????????????????????????????????
            removeNotQuoted(vendorQuotedInfoVO, vendorQuotedSums);
            //?????????????????????
            LgtVendorVO vendorVO = map2.get(vendorCode);
            result.add(buildOrder(biding,vendorQuotedInfoVO,vendorVO));
        }
        return result;
    }

    /**
     * ????????????????????????????????????
     * @param vendorQuotedInfoVO ???????????????
     * @param vendorQuotedSums ?????????????????????
     */
    private void removeNotQuoted(LgtVendorQuotedInfoVO vendorQuotedInfoVO, List<LgtVendorQuotedSum> vendorQuotedSums){
        //????????????????????????????????????id + ????????? + ????????? ??? ?????? ????????????????????????
        List<String> lineKeyList = vendorQuotedSums
                .stream()
                .map(vendorQuotedSum -> new StringBuffer()
                        .append(vendorQuotedSum.getVendorId())
                        .append(vendorQuotedSum.getStartAddress())
                        .append(vendorQuotedSum.getEndAddress())
                        .toString())
                .distinct()
                .collect(Collectors.toList());

        List<LgtVendorQuotedLine> vendorQuotedLineList = vendorQuotedInfoVO.getVendorQuotedLineList();
        //????????????
        List<LgtVendorQuotedLine> vendorQuotedLineListFilter = vendorQuotedLineList
                .stream()
                .filter(item -> lineKeyList.contains(
                        new StringBuffer()
                                .append(item.getVendorId())
                                .append(item.getStartAddress())
                                .append(item.getEndAddress())
                                .toString())
                ).collect(Collectors.toList());
        vendorQuotedInfoVO.setVendorQuotedLineList(vendorQuotedLineListFilter);
    }

    /**
     * ????????? -> ??????????????????
     * @param biding ??????????????????
     * @param vendorQuotedInfoVO ?????????????????????
     * @param vendorVO ???????????????(???????????????????????????,????????????)
     * @return
     */
    private LogisticsOrderDTO buildOrder(LgtBiding biding, LgtVendorQuotedInfoVO vendorQuotedInfoVO, LgtVendorVO vendorVO){
        //?????????????????????
        OrderHead orderHead = new OrderHead();
        orderHead.setSourceFrom(biding.getSourceFrom())
                .setVendorId(vendorVO.getBidVendor().getVendorId())
                .setVendorCode(vendorVO.getBidVendor().getVendorCode())
                .setVendorName(vendorVO.getBidVendor().getVendorName())
                .setTemplateHeadId(biding.getTemplateHeadId())
                .setTemplateCode(biding.getTemplateCode())
                .setTemplateName(biding.getTemplateName())
                .setBusinessModeCode(biding.getBusinessModeCode())
                .setTransportModeCode(biding.getTransportModeCode())
                .setBusinessType(biding.getBusinessType())
                .setServiceProjectName(biding.getServiceProjectName())
                .setServiceProjectCode(biding.getServiceProjectCode())
                .setIfNeedVendorComfirm(biding.getIfNeedVendorComfirm())
//                .setIfNeedShepDate()        //??????????????????
                .setIfVendorSubmitShipDate(biding.getIfVendorSubmitShipDate())
                .setOrderTitle(biding.getBidingName())             //????????????
//                .setBiddingSequence()         //????????????
                .setPriceStartDate(biding.getPriceTimeStart())
                .setPriceEndDate(biding.getPriceTimeEnd())
//                .setCeeaApplyUserName()   //???????????????
//                .setCeeaApplyUserNickname()  //??????????????????
//                .setCeeaApplyUserId()   //?????????id
//                .setOrderDate()      //????????????
                .setApplyDepartmentId(String.valueOf(biding.getApplyDepartmentId()))  //??????id
                .setApplyDepartmentCode(biding.getApplyDepartmentCode())  //????????????
                .setApplyDepartmentName(biding.getApplyDepartmentName())  //????????????
                .setApplyCode(biding.getApplyCode())     //???????????????
                .setApplyBy(biding.getApplyBy())        //??????????????????
                .setApplyId(biding.getApplyId())       //?????????id
                .setUnit(biding.getUnitCode())          //??????
                .setProjectTotal(biding.getProjectTotal())      //????????????
                .setTaxId(biding.getTaxId())            //??????id
                .setTaxRate(biding.getTaxCode())          //?????????
                .setTaxCode(biding.getTaxKey())           //????????????
                .setRequirementHeadNum(biding.getRequirementHeadNum())   //??????????????????
                .setRequirementHeadId(String.valueOf(biding.getRequirementHeadId()))   //????????????id
                .setComments(biding.getComments())          //??????
//                .setDrafterOpinion()    //???????????????
                .setPhone(biding.getPhone())             //????????????
                .setTmsStatus(LogisticsOrderTmsStatus.NOT_SYNC.getValue())
                .setOrderSourceFrom(OrderSourceFrom.LOGISTICS_BID.getItemValue());

        //?????????
        List<OrderLine> orderLineList = new LinkedList<>();
        List<LgtVendorQuotedLine> vendorQuotedLineList = vendorQuotedInfoVO.getVendorQuotedLineList();
        //???????????????+??????????????????
        Map<String,List<LgtVendorQuotedLine>> map = groupVendorQuotedLine(vendorQuotedLineList);

        for(Map.Entry<String,List<LgtVendorQuotedLine>> entry : map.entrySet()){
            List<LgtVendorQuotedLine> value = entry.getValue();
            LgtVendorQuotedLine vendorQuotedLine = value.get(0);

            //????????????????????????
            OrderLine orderLine = new OrderLine();
            BeanUtils.copyProperties(vendorQuotedLine,orderLine);
            orderLine.setSingleKmCost(vendorQuotedLine.getSingleKmCost())
                    .setSingleDragCost(vendorQuotedLine.getSingleDragCost())
                    .setImportExportMethod(vendorQuotedLine.getImportExportMethod())
                    .setLeg(vendorQuotedLine.getLeg())
                    .setBeyondBoxCost(vendorQuotedLine.getBeyondBoxCost())
                    .setBeyondStorageCost(vendorQuotedLine.getBeyondStorageCost())
                    .setFreeBoxPeriod(vendorQuotedLine.getFreeBoxPeriod())
                    .setFreeStoragePeriod(vendorQuotedLine.getFreeStoragePeriod())
                    .setShipDateFrequency(vendorQuotedLine.getShipDateFrequency())
                    .setWholeArk(vendorQuotedLine.getWholeArk())
                    .setTradeTerm(vendorQuotedLine.getTradeTerm())
                    .setSpecifiedVendor(vendorQuotedLine.getSpecifiedVendor())
                    .setExpense(vendorQuotedLine.getExpense())
                    .setChargeUnit(vendorQuotedLine.getChargeUnit())
                    .setChargeMethod(vendorQuotedLine.getChargeMethod())
                    .setExpenseItem(vendorQuotedLine.getExpenseItem())
                    .setFreeStayPeriod(vendorQuotedLine.getFreeStayPeriod())
                    .setShipDate(vendorQuotedLine.getShipDate())
                    .setFullRealTime(vendorQuotedLine.getFullRealTime())
                    .setReserveTime(vendorQuotedLine.getReserveTime())
                    .setToPort(vendorQuotedLine.getToPort())
                    .setFromPort(vendorQuotedLine.getFromPort())
                    .setRealTime(vendorQuotedLine.getRealTime())
                    .setComments(vendorQuotedLine.getComments())
                    .setTransportDistance(vendorQuotedLine.getTransportDistance())
                    .setCurrency(vendorQuotedLine.getCurrency())
                    .setMaxCost(vendorQuotedLine.getMaxCost())
                    .setMinCost(vendorQuotedLine.getMinCost())
                    .setIfBack(vendorQuotedLine.getIfBack())
                    .setToCounty(vendorQuotedLine.getToCounty())
                    .setToCity(vendorQuotedLine.getToCity())
                    .setToProvince(vendorQuotedLine.getToProvince())
                    .setToPlace(vendorQuotedLine.getToPlace())
                    .setFromCity(vendorQuotedLine.getFromCity())
                    .setFromProvince(vendorQuotedLine.getFromProvince())
                    .setFromPlace(vendorQuotedLine.getFromPlace())
                    .setFromCountry(vendorQuotedLine.getFromCountry())
                    .setToCountry(vendorQuotedLine.getToCountry())
                    .setToCountyCode(vendorQuotedLine.getToCountyCode())
                    .setToCityCode(vendorQuotedLine.getToCityCode())
                    .setToProvinceCode(vendorQuotedLine.getToProvinceCode())
                    .setToPlaceCode(vendorQuotedLine.getToPlaceCode())
                    .setFromCountryCode(vendorQuotedLine.getFromCountryCode())
                    .setFromCityCode(vendorQuotedLine.getFromCityCode())
                    .setFromProvinceCode(vendorQuotedLine.getFromProvinceCode())
                    .setFromPlaceCode(vendorQuotedLine.getFromPlaceCode())
                    .setFromCountryCode(vendorQuotedLine.getFromCountryCode())
                    .setToCountryCode(vendorQuotedLine.getToCountryCode())
                    .setLogisticsCategoryId(vendorQuotedLine.getLogisticsCategoryId())
                    .setLegName(vendorQuotedLine.getLegName())
                    .setChargeUnitName(vendorQuotedLine.getChargeUnitName())
                    .setChargeMethodName(vendorQuotedLine.getChargeMethodName())
                    .setExpenseItemName(vendorQuotedLine.getExpenseItemName())
                    .setLogisticsCategoryCode(vendorQuotedLine.getLogisticsCategoryCode())
                    .setLogisticsCategoryName(vendorQuotedLine.getLogisticsCategoryName());

            //????????????????????????
            List<OrderLineFee> orderLineFeeList = new LinkedList<>();
            for(LgtVendorQuotedLine v : value){
                OrderLineFee orderLineFee = new OrderLineFee()
                        .setLeg(v.getLeg())
                        .setExpenseItem(v.getExpenseItem())
                        .setChargeMethod(v.getChargeMethod())
                        .setChargeUnit(v.getChargeUnit())
                        .setMaxCost(v.getMaxCost())
                        .setMinCost(v.getMinCost())
                        .setExpense(v.getExpense())
                        .setCurrency(v.getCurrency());
                orderLineFeeList.add(orderLineFee);
            }
            orderLine.setOrderLineFeeList(orderLineFeeList);
            orderLineList.add(orderLine);
        }

        //????????????
        List<OrderLineShip> orderLineShipList = new LinkedList<>();
        List<LgtBidShipPeriod> bidShipPeriodList = vendorQuotedInfoVO.getBidShipPeriodList();
        for(LgtBidShipPeriod bidShipPeriod : bidShipPeriodList){
            OrderLineShip orderLineShip = new OrderLineShip();
            BeanCopyUtil.copyProperties(orderLineShip,bidShipPeriod);
            orderLineShip.setShipId(bidShipPeriod.getShipId())
                    .setFromPortId(bidShipPeriod.getFromPortId())
                    .setFromPortCode(bidShipPeriod.getFromPortCode())
                    .setFromPort(bidShipPeriod.getFromPort())
                    .setToPortId(bidShipPeriod.getToPortId())
                    .setToPortCode(bidShipPeriod.getToPortCode())
                    .setToPort(bidShipPeriod.getToPort())
                    .setWholeArk(bidShipPeriod.getWholeArk())
                    .setMon(bidShipPeriod.getMon())
                    .setTue(bidShipPeriod.getTue())
                    .setWed(bidShipPeriod.getWed())
                    .setThu(bidShipPeriod.getThu())
                    .setFri(bidShipPeriod.getFri())
                    .setSat(bidShipPeriod.getSat())
                    .setSun(bidShipPeriod.getSun())
                    .setTransitTime(bidShipPeriod.getTransitTime())
                    .setShipCompanyName(bidShipPeriod.getShipCompanyName())
                    .setTransferPort(bidShipPeriod.getTransferPort());
            orderLineShipList.add(orderLineShip);
        }

        //????????????
        List<OrderLineContract> orderLineContractList = new LinkedList<>();
        List<LgtPayPlan> payPlanList = vendorVO.getPayPlanList();
        for(LgtPayPlan payPlan : payPlanList){
            OrderLineContract orderLineContract = new OrderLineContract()
                    .setContractId(payPlan.getContractHeadId())
                    .setContractName(payPlan.getContractName())
                    .setContractCode(payPlan.getContractCode())
                    .setPaymentMethod(payPlan.getPayMethod())
                    .setPaymentStage(payPlan.getPayExplain())
                    .setAccountDate(payPlan.getDateNum());
            orderLineContractList.add(orderLineContract);
        }

        return new LogisticsOrderDTO()
                .setOrderHead(orderHead)
                .setOrderLineContractList(orderLineContractList)
                .setOrderLineList(orderLineList)
                .setOrderLineShipList(orderLineShipList);

    }

    /**
     * ???????????????+???????????????key???????????????
     * @param vendorQuotedLineList
     * @return
     */
    private Map<String,List<LgtVendorQuotedLine>> groupVendorQuotedLine(List<LgtVendorQuotedLine> vendorQuotedLineList){
        Map<String,List<LgtVendorQuotedLine>> map = new HashMap<>();
        for(LgtVendorQuotedLine vendorQuotedLine : vendorQuotedLineList){
            String key = new StringBuffer()
                    .append(vendorQuotedLine.getStartAddress())
                    .append(vendorQuotedLine.getEndAddress())
                    .toString();
            if(CollectionUtils.isEmpty(map.get(key))){
                map.put(key,new LinkedList<LgtVendorQuotedLine>(){{
                    add(vendorQuotedLine);
                }});
            }else{
                map.get(key).add(vendorQuotedLine);
            }
        }
        return map;
    }


    /**
     * ??????????????????????????? ?????????????????????
     * @param vendorQuotedInfoList
     * @return
     */
    private Map<String,List<LgtVendorQuotedInfoVO>> groupVendorQuotedByVendorCode(List<LgtVendorQuotedInfoVO> vendorQuotedInfoList){
        Map<String,List<LgtVendorQuotedInfoVO>> map = new HashMap<>();
        for(LgtVendorQuotedInfoVO lgtVendorQuotedInfoVO : vendorQuotedInfoList){
            String key = lgtVendorQuotedInfoVO.getVendorQuotedHead().getVendorCode();
            if(CollectionUtils.isEmpty(map.get(key))){
                map.put(key,new LinkedList<LgtVendorQuotedInfoVO>(){{
                    add(lgtVendorQuotedInfoVO);
                }});
            }else{
                map.get(key).add(lgtVendorQuotedInfoVO);
            }
        }
        return map;
    }


    /**
     * ??????????????????TMS ????????????
     * @param response
     * @param orderHeadId
     */
    private void updateAfterTmsPush(SoapResponse response, Long orderHeadId){
        String tmsInfo = null;
        if(Objects.nonNull(response)){
            tmsInfo = response.getSuccess();
        }
        if (Objects.isNull(response) ||
                Objects.isNull(response.getSuccess()) ||
                !"S".equals(response.getSuccess())
        ) {
            this.updateById(new OrderHead()
                    .setOrderHeadId(orderHeadId)
                    .setTmsInfo(tmsInfo) //todo ?????????????????????
                    .setTmsStatus(LogisticsOrderTmsStatus.FAIL_SYNC.getValue()));
        } else {
            this.updateById(new OrderHead()
                    .setOrderHeadId(orderHeadId)
                    .setTmsInfo(tmsInfo) //todo ?????????????????????
                    .setTmsStatus(LogisticsOrderTmsStatus.SUCCESS_SYNC.getValue()));
        }
    }



    /**
     * ??????????????????
     * @param logisticsOrderRequest
     * @param response
     * @param errorMsg
     * @param logisticsOrderVO
     */
    private void saveInterfaceLog(LogisticsOrderRequest logisticsOrderRequest, SoapResponse response, Map<String,String> errorMsg, LogisticsOrderVO logisticsOrderVO){
        InterfaceLogDTO interfaceLogDTO = new InterfaceLogDTO();
        if(Objects.nonNull(logisticsOrderRequest)){
            try {
                interfaceLogDTO.setServiceInfo(JSON.toJSONString(logisticsOrderRequest));
            } catch (Exception e) {
                log.error(String.format("????????????????????????TMS??????????????????????????????[%s]",logisticsOrderVO.getOrderHead().getOrderHeadNum()));
            }
        }
        interfaceLogDTO.setCreationDateBegin(new Date());
        interfaceLogDTO.setServiceName("??????????????????ERP????????????");
        interfaceLogDTO.setServiceType("WEBSERVICE"); //????????????
        interfaceLogDTO.setType("SEND"); // ????????????
        interfaceLogDTO.setBillType("??????????????????"); // ????????????
        interfaceLogDTO.setBillId(logisticsOrderVO.getOrderHead().getOrderHeadNum());
        interfaceLogDTO.setTargetSys("TMS");
        interfaceLogDTO.setReturnInfo(JSON.toJSONString(response));
        interfaceLogDTO.setStatus("SUCCESS");

        if(Objects.nonNull(errorMsg)){
            interfaceLogDTO.setErrorInfo(JSON.toJSONString(errorMsg));
            interfaceLogDTO.setStatus("FAIL");
        }

        if(Objects.isNull(response) ||
                Objects.isNull(response.getSuccess()) ||
                !"S".equals(response.getSuccess())
        ){
            if(Objects.nonNull(response) &&
                    Objects.nonNull(response.getResponse()) &&
                    Objects.nonNull(response.getResponse().getResultInfo()) &&
                    StringUtils.isNotBlank(response.getResponse().getResultInfo().getRESPONSEMESSAGE())
            ){
                interfaceLogDTO.setErrorInfo(response.getResponse().getResultInfo().getRESPONSEMESSAGE());
            }
            interfaceLogDTO.setStatus("FAIL");
        }
        interfaceLogDTO.setCreationDateEnd(new Date());
        interfaceLogDTO.setFinishDate(new Date());
        try {
            apiClient.createInterfaceLog(interfaceLogDTO);
        } catch (Exception e){
            log.error("??????<????????????????????????TMS>????????????{}" + e);
        }
    }

    private void checkIfSync(LogisticsOrderVO logisticsOrderVO){
        OrderHead orderHead = logisticsOrderVO.getOrderHead();
        if(!LogisticsOrderStatus.COMPLETED.getValue().equals(orderHead.getOrderStatus())){
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????[??????]????????????TMS"));
        }
        if(LogisticsOrderTmsStatus.SUCCESS_SYNC.getValue().equals(orderHead.getTmsStatus())){
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????,??????????????????"));
        }
    }

    /**
     * ????????????????????????tms?????????
     * @param logisticsOrderVO
     * @return
     */
    private LogisticsOrderRequest buildRequest(LogisticsOrderVO logisticsOrderVO){
        OrderHead orderHead = logisticsOrderVO.getOrderHead();
        List<OrderLine> orderLineList = logisticsOrderVO.getOrderLineList();
        List<OrderLineShip> orderLineShipList = logisticsOrderVO.getOrderLineShipList();
        List<OrderLineContract> orderLineContractList = logisticsOrderVO.getOrderLineContractList();

        //??????????????????????????? LogisticsRequirementHead
        LogisticsRequirementHead logisticsRequirementHead = requirementHeadService.getById(orderHead.getRequirementHeadId());
        if(Objects.isNull(logisticsRequirementHead)){
            throw new BaseException(LocaleHandler.getLocaleMsg(String.format("orderHeadId=[%s],????????????????????????,requirementHeadId=[%s]",orderHead.getOrderHeadId(),orderHead.getRequirementHeadId())));
        }
        String settlementMethod = null;
        if(!CollectionUtils.isEmpty(orderLineContractList)){
            settlementMethod = orderLineContractList.stream().map(item -> item.getPaymentMethod()).collect(Collectors.joining("/"));
        }

        //?????????????????????
        LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderHead tmsOrderHead = new LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderHead();
        tmsOrderHead.setOrderHeadNum(orderHead.getOrderHeadNum());
        tmsOrderHead.setVendorCode(orderHead.getVendorCode());
        tmsOrderHead.setPriceStartDate(String.valueOf(orderHead.getPriceStartDate()));
        tmsOrderHead.setPriceEndDate(String.valueOf(orderHead.getPriceEndDate()));
        tmsOrderHead.setSettlementMethod(settlementMethod);// ????????????
        tmsOrderHead.setContractType(logisticsRequirementHead.getContractType());  //????????????
        tmsOrderHead.setBusinessModeCode(orderHead.getBusinessModeCode());
        tmsOrderHead.setTransportModeCode(orderHead.getTransportModeCode());
        tmsOrderHead.setStatus("Y"); //?????????
        tmsOrderHead.setSourceSystem("SRM");

        //?????????????????????
        List<LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLine> tmsOrderLineList = new LinkedList<>();
        if(!CollectionUtils.isEmpty(orderLineList)){
            for(OrderLine orderLine : orderLineList){
                LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLine tmsOrderLine = new LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLine();
                tmsOrderLine.setOrderHeadNum(orderLine.getOrderHeadNum());
                tmsOrderLine.setRowLineNum(String.valueOf(orderLine.getRowNum()));
                tmsOrderLine.setFromCountry(orderLine.getFromCountry());
                tmsOrderLine.setFromProvince(orderLine.getFromProvince());
                tmsOrderLine.setFromCity(orderLine.getFromCity());
                tmsOrderLine.setFromCounty(orderLine.getFromCounty());
                tmsOrderLine.setFromPlace(orderLine.getFromPlace());
                tmsOrderLine.setToCountry(orderLine.getToCountry());
                tmsOrderLine.setToProvinceCode(orderLine.getToProvinceCode());
                tmsOrderLine.setToCountyCode(orderLine.getToCountyCode());
                tmsOrderLine.setToPlace(orderLine.getToPlace());
                tmsOrderLine.setVendorOrderNum("1"); //????????????
                tmsOrderLine.setTransportDistance(orderLine.getTransportDistance());
                tmsOrderLine.setTransportModeCode(orderHead.getTransportModeCode()); //????????????
                tmsOrderLine.setServiceProjectCode(orderHead.getServiceProjectCode());  //??????
                tmsOrderLine.setStatus("Y");
                tmsOrderLine.setRealTime(null); //????????????
                tmsOrderLine.setPriceStartDate(String.valueOf(orderHead.getPriceStartDate())); //?????????
                tmsOrderLine.setPriceEndDate(String.valueOf(orderHead.getPriceEndDate())); //?????????
                tmsOrderLine.setWholeArk(orderLine.getWholeArk());
                tmsOrderLine.setFromPort(orderLine.getFromPort());
                tmsOrderLine.setToPort(orderLine.getToPort());
                tmsOrderLine.setUnProjectFlag("N"); //??????????????????
                tmsOrderLine.setImportExportMethod(orderLine.getImportExportMethod());
                tmsOrderLineList.add(tmsOrderLine);
            }
        }


        //??????????????????
        List<LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLineShip> tmsOrderLineShipList = new LinkedList<>();
        if(!CollectionUtils.isEmpty(orderLineShipList)){
            for(OrderLineShip orderLineShip : orderLineShipList){
                LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLineShip tmsOrderLineShip = new LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLineShip();
                tmsOrderLineShip.setOrderHeadNum(orderLineShip.getOrderHeadNum());
                tmsOrderLineShip.setRowNum(String.valueOf(orderLineShip.getRowNum()));
                tmsOrderLineShip.setFromPort(orderLineShip.getFromPortCode());
                tmsOrderLineShip.setToPort(orderLineShip.getToPortCode());
                tmsOrderLineShip.setWholeArk(orderLineShip.getWholeArk());
                tmsOrderLineShip.setMon(String.valueOf(orderLineShip.getMon()));
                tmsOrderLineShip.setTue(String.valueOf(orderLineShip.getTue()));
                tmsOrderLineShip.setWed(String.valueOf(orderLineShip.getWed()));
                tmsOrderLineShip.setThu(String.valueOf(orderLineShip.getThu()));
                tmsOrderLineShip.setFri(String.valueOf(orderLineShip.getFri()));
                tmsOrderLineShip.setSat(String.valueOf(orderLineShip.getSat()));
                tmsOrderLineShip.setSun(String.valueOf(orderLineShip.getSun()));
                tmsOrderLineShip.setTransitTime(String.valueOf(orderLineShip.getTransitTime()));
                tmsOrderLineShip.setCompany(orderLineShip.getShipCompanyName());
                tmsOrderLineShip.setTransferPort(orderLineShip.getTransferPort());
                tmsOrderLineShipList.add(tmsOrderLineShip);
            }
        }

        //???????????????
        List<LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLineFee> tmsOrderLineFeeList = new LinkedList<>();
        if(!CollectionUtils.isEmpty(orderLineList)){
            for(OrderLine orderLine : orderLineList){
                List<OrderLineFee> orderLineFeeList = orderLine.getOrderLineFeeList();
                if(!CollectionUtils.isEmpty(orderLineFeeList)){
                    for(OrderLineFee orderLineFee : orderLineFeeList){
                        LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLineFee tmsOrderLineFee = new LogisticsOrderRequest.RequestInfo.LogisticsOrders.OrderLineFee();
                        tmsOrderLineFee.setOrderHeadNum(orderLineFee.getOrderHeadNum());
                        tmsOrderLineFee.setRowLineNum(String.valueOf(orderLine.getRowNum()));
                        tmsOrderLineFee.setRowNum(String.valueOf(orderLineFee.getRowNum()));
                        tmsOrderLineFee.setExpenseItem(orderLineFee.getExpenseItem());
                        tmsOrderLineFee.setChargeMethod(orderLineFee.getChargeMethod());
                        tmsOrderLineFee.setChargeUnit(orderLineFee.getChargeUnit());
                        tmsOrderLineFee.setMinCost(orderLineFee.getMinCost());
                        tmsOrderLineFee.setMaxCost(orderLineFee.getMaxCost());
                        tmsOrderLineFee.setExpense(String.valueOf(orderLineFee.getExpense()));
                        tmsOrderLineFee.setCurrency(orderLineFee.getCurrency());
                        tmsOrderLineFee.setIfBack(orderLine.getIfBack()); //????????????
                        tmsOrderLineFee.setLeg(orderLineFee.getLeg());
                        tmsOrderLineFee.setTaxRate(String.valueOf(orderHead.getTaxRate()));
                        tmsOrderLineFeeList.add(tmsOrderLineFee);
                    }
                }
            }
        }

        //???????????????
        LogisticsOrderRequest request = new LogisticsOrderRequest();
        LogisticsOrderRequest.RequestInfo requestInfo = new LogisticsOrderRequest.RequestInfo();
        LogisticsOrderRequest.RequestInfo.LogisticsOrders logisticsOrders = new LogisticsOrderRequest.RequestInfo.LogisticsOrders();
        logisticsOrders.setOrderHead(tmsOrderHead);
        logisticsOrders.setOrderLine(tmsOrderLineList);
        logisticsOrders.setOrderLineFee(tmsOrderLineFeeList);
        logisticsOrders.setOrderLineShip(tmsOrderLineShipList);
        List<LogisticsOrderRequest.RequestInfo.LogisticsOrders> logisticsOrdersList = new LinkedList<LogisticsOrderRequest.RequestInfo.LogisticsOrders>(){{
            add(logisticsOrders);
        }};
        requestInfo.setLogisticsOrders(logisticsOrdersList);
        request.setRequestInfo(requestInfo);
        return request;
    }


    /**
     * ????????????????????????
     * @param orderDTO
     */
    private void checkIfTemporarySave(LogisticsOrderDTO orderDTO){

        Long orderHeadId = orderDTO.getOrderHead().getOrderHeadId();
        if(Objects.nonNull(orderHeadId)){
            OrderHead orderHead = this.getById(orderHeadId);
            if(Objects.isNull(orderHead)){
                throw new BaseException(LocaleHandler.getLocaleMsg(String.format("???????????????????????????,??????id[%s]",orderHead.getOrderHeadId())));
            }

            if(Objects.nonNull(orderHead) && !LogisticsOrderStatus.DRAFT.getValue().equals(orderHead.getOrderStatus())){
                throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????"));
            }

        }

        OrderHead orderHead = orderDTO.getOrderHead();
        if (!SourceFrom.MANUAL.name().equals(orderHead.getOrderSourceFrom())) {
            //????????????
            if(Objects.isNull(orderHead.getRequirementHeadId())){
                throw new BaseException(LocaleHandler.getLocaleMsg("[requirementHeadId]??????"));
            }
            LogisticsRequirementHead requirementHead = requirementHeadService.getById(orderHead.getRequirementHeadId());
            if(Objects.isNull(requirementHead)){
                throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????,requirementHeadId = [%s]",orderHead.getRequirementHeadId()));
            }
            //???????????????????????????????????????
            checkIfRequirementUsed(requirementHead);
        }

    }
}
