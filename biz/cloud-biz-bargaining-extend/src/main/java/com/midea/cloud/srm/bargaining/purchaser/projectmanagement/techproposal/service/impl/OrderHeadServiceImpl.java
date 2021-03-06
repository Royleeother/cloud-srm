package com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.midea.cloud.common.constants.SequenceCodeConstant;
import com.midea.cloud.common.enums.FileUploadType;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.bargaining.projectmanagement.evaluation.OrderStatusEnum;
import com.midea.cloud.common.enums.bargaining.projectmanagement.signupmanagement.SignUpStatus;
import com.midea.cloud.common.enums.bargaining.projectmanagement.signupmanagement.VendorFileType;
import com.midea.cloud.common.enums.bargaining.projectmanagement.bidinitiating.BidType;
import com.midea.cloud.common.enums.base.FormulaPriceExportTypeEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.BeanCopyUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.redis.RedisUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.component.entity.EntityManager;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.mapper.BidRequirementLineMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.mapper.BidRequirementMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.mapper.BidVendorMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.mapper.BidingMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidFileConfigService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.mapper.OrderHeadFileMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.mapper.OrderHeadMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.mapper.OrderLineMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.mapper.OrderlinePaymentTermMapper;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.service.IBidOrderLineFormulaPriceDetailService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.service.IBidOrderLineTemplatePriceDetailService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.service.IOrderHeadService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.techproposal.service.IOrderLineService;
import com.midea.cloud.srm.bargaining.suppliercooperate.projectlist.service.IBidVendorFileService;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.base.IMaterialItemAttributeClient;
import com.midea.cloud.srm.feign.file.FileCenterClient;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.*;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.businessproposal.entity.Round;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.enums.RequirementPricingType;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techproposal.entity.*;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.techproposal.vo.BidOrderLineTemplatePriceDetailVO;
import com.midea.cloud.srm.model.bargaining.suppliercooperate.vo.BidOrderHeadVO;
import com.midea.cloud.srm.model.bargaining.suppliercooperate.vo.BidOrderLineVO;
import com.midea.cloud.srm.model.bargaining.suppliercooperate.vo.BidVendorFileVO;
import com.midea.cloud.srm.model.bargaining.suppliercooperate.vo.SupplierBidingVO;
import com.midea.cloud.srm.model.base.formula.dto.FormulaPriceDTO;
import com.midea.cloud.srm.model.base.material.vo.MaterialItemAttributeRelateVO;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.enums.BidingFileTypeEnum;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 * ?????????????????????
 * </pre>
 *
 * @author zhizhao1.fan@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:tanjl11@meicloud.com
 *  ????????????: 2020-09-08
 *  ????????????:
 *          </pre>
 */
@Service
@Slf4j
public class OrderHeadServiceImpl extends ServiceImpl<OrderHeadMapper, OrderHead> implements IOrderHeadService {
    public static final Integer firstRound = 1;
    @Autowired
    private IOrderLineService iOrderLineService;
    @Autowired
    private IBidVendorFileService iBidVendorFileService;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private IBidOrderLineTemplatePriceDetailService templatePriceDetailService;
    @Autowired
    private IBidOrderLineFormulaPriceDetailService formulaPriceDetailService;
    @Autowired
    private OrderHeadFileMapper orderHeadFileMapper;
    @Autowired
    private BidOrderLineFormulaPriceDetailService formulaDetailService;
    @Autowired
    private FileCenterClient fileCenterClient;


    private final EntityManager<Biding> biddingDao
            = EntityManager.use(BidingMapper.class);
    private final EntityManager<BidRequirement> bidRequirementDao
            = EntityManager.use(BidRequirementMapper.class);
    private final EntityManager<BidRequirementLine> bidRequirementLineDao
            = EntityManager.use(BidRequirementLineMapper.class);
    private final EntityManager<BidVendor> bidVendorDao
            = EntityManager.use(BidVendorMapper.class);
    private final EntityManager<OrderLine> orderLineDao
            = EntityManager.use(OrderLineMapper.class);

    @Resource
    private IMaterialItemAttributeClient materialItemAttributeClient;
    @Autowired
    private IBidFileConfigService fileConfigService;
    @Autowired
    private OrderlinePaymentTermMapper paymentTermMapper;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public BidOrderHeadVO getOrderHeadByBidingIdAndBidVerdorId(Long bidingId, Long bidVendorId) {
        return this.getBaseMapper().getOrderHeadByBidingId(bidingId, bidVendorId);
    }

    @Override
    @Transactional
    public Long saveOrderInfo(BidOrderHeadVO bidOrderHeadVO, boolean isSubmit) {
        //1 ??????????????????
        String key = new StringBuilder("brg-saveOrderInfo").append("-")
                .append(bidOrderHeadVO.getBidingId()).append("-").append(bidOrderHeadVO.getBidVendorId())
                .append("-").append(bidOrderHeadVO.getRoundId()).toString();
        Boolean lock = redisUtil.tryLock(key);
        if (!lock) {
            throw new BaseException("???????????????????????????????????????.");
        }
        try {
            OrderHead orderHead = alreadyOrderHeadByBidingAndBidVendorIdAndRoundId(bidOrderHeadVO.getBidingId(), bidOrderHeadVO.getBidVendorId(), bidOrderHeadVO.getRoundId());
            boolean addOrderHead = (null == orderHead);
            if (addOrderHead) {
                orderHead = new OrderHead();
                String bidOrderNum = baseClient.seqGen(SequenceCodeConstant.SEQ_BID_BIDING_CODE);
                BeanCopyUtil.copyProperties(orderHead, bidOrderHeadVO);
                Long id = IdGenrator.generate();
                orderHead.setOrderHeadId(id);
                orderHead.setBidRoundId(bidOrderHeadVO.getRoundId());
                orderHead.setBidOrderNum(bidOrderNum);

                // ?????? ????????????
                BidRequirement bidRequirement = bidRequirementDao.findOne(
                        Wrappers.lambdaQuery(BidRequirement.class).eq(BidRequirement::getBidingId, bidOrderHeadVO.getBidingId())
                );
                if (bidRequirement == null)
                    throw new BaseException("?????????????????????????????? | biddingId: [" + bidOrderHeadVO.getBidingId() + "]");
                orderHead.setPricingType(bidRequirement.getPricingType());
                BidRequirementLine line = bidRequirementLineDao.findOne(
                        Wrappers.lambdaQuery(BidRequirementLine.class)
                                .select(BidRequirementLine::getRequirementLineId, BidRequirementLine::getIsSeaFoodFormula)
                                .eq(BidRequirementLine::getBidingId, bidOrderHeadVO.getBidingId())
                                .last("limit 1")
                );
                bidOrderHeadVO.setIsSeaFoodFormula(line.getIsSeaFoodFormula());
                orderHead.setIsProxyBidding(bidOrderHeadVO.getIsProxyBidding());
                this.save(orderHead);
                bidOrderHeadVO.setOrderHeadId(id);
                bidOrderHeadVO.setBidOrderNum(bidOrderNum);
            } else {
                log.info(orderHead.toString());
                orderHead.setOrderStatus(bidOrderHeadVO.getOrderStatus());
                if (OrderStatusEnum.SUBMISSION.getValue().equals(bidOrderHeadVO.getOrderStatus())) {
                    orderHead.setSubmitTime(new Date());
                }
                log.info(orderHead.toString());
                orderHead.setIsProxyBidding(bidOrderHeadVO.getIsProxyBidding());
                this.updateById(orderHead);
            }

            //2 ??????????????????
            List<BidOrderLineVO> orderLineVOS = bidOrderHeadVO.getOrderLines();
            Set<Long> materialIds = new HashSet<>();
            orderLineVOS.forEach(orderLinevo -> {
                if (addOrderHead) {
                    orderLinevo.setWin(null);
                    orderLinevo.setOrderLineId(null);
                }
                orderLinevo.setOrderHeadId(bidOrderHeadVO.getOrderHeadId());
                orderLinevo.setRound(bidOrderHeadVO.getRound());
                orderLinevo.setOrderStatus(bidOrderHeadVO.getOrderStatus());
                orderLinevo.setBidVendorId(bidOrderHeadVO.getBidVendorId());
                materialIds.add(orderLinevo.getTargetId());
            });
            iOrderLineService.saveBatchOrderLines(orderLineVOS, isSubmit, materialIds);

            Long orderHeadId = orderHead.getOrderHeadId();
            List<OrderHeadFile> orderHeadFiles = bidOrderHeadVO.getOrderHeadFiles();
            List<OrderHeadFile> files = orderHeadFileMapper.selectList(Wrappers.lambdaQuery(OrderHeadFile.class)
                    .eq(OrderHeadFile::getOrderHeadId, orderHeadId));
            List<Long> shouldDel = new LinkedList<>();
            for (OrderHeadFile file : files) {
                boolean find = false;
                for (OrderHeadFile orderHeadFile : orderHeadFiles) {
                    if (Objects.equals(file.getOrderHeadFileId(), orderHeadFile.getOrderHeadFileId())) {
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    shouldDel.add(file.getOrderHeadFileId());
                }
            }

            List<BidFileConfig> configFiles = bidOrderHeadVO.getConfigFiles();
            for (OrderHeadFile orderHeadFile : orderHeadFiles) {
                if (orderHeadFile.getVendorReferenceFileType() == null) {
                    for (BidFileConfig configFile : configFiles) {
                        if (Objects.equals(configFile.getRequireId(), orderHeadFile.getConfigFileId())) {
                            orderHeadFile.setVendorReferenceFileType(configFile.getReferenceFileType());
                            break;
                        }
                    }
                }
                if (orderHeadFile.getOrderHeadFileId() == null) {
                    orderHeadFile.setOrderHeadFileId(IdGenrator.generate());
                    orderHeadFile.setOrderHeadId(orderHeadId);
                    orderHeadFileMapper.insert(orderHeadFile);
                } else {
                    orderHeadFileMapper.updateById(orderHeadFile);
                }
            }
            if (!CollectionUtils.isEmpty(shouldDel)) {
                orderHeadFileMapper.deleteBatchIds(shouldDel);
            }
            return orderHead.getOrderHeadId();
        } finally {
            redisUtil.unLock(key);
        }


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawOrder(BidOrderHeadVO bidOrderHeadVO) {
        Long orderHeadId = bidOrderHeadVO.getOrderHeadId();
        String orderStatus = bidOrderHeadVO.getOrderStatus();
        OrderHead orderHead = this.getById(orderHeadId).setOrderStatus(orderStatus).setWithDrawReason(bidOrderHeadVO.getWithDrawReason()).setWithDrawTime(new Date());
        //????????????????????????
        boolean flag = this.updateById(orderHead);
        return updateOrderLineStatus(orderHeadId, orderStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean submitOrder(BidOrderHeadVO bidOrderHeadVO) throws IOException {

        // ?????? ?????????
        Biding biding = Optional.ofNullable(bidOrderHeadVO.getBidingId())
                .map(biddingDao::findById)
                .orElseThrow(() -> new BaseException("???????????????????????? | biddingId: [" + bidOrderHeadVO.getBidingId() + "]"));
        List<BidOrderLineVO> orderLines = bidOrderHeadVO.getOrderLines();
        boolean isFirstRound = biding.getCurrentRound() == null || biding.getCurrentRound() == 1;

        //????????????????????????+???????????????????????????????????????????????????????????????
        if (isFirstRound) {
            boolean isTechBusiness = BidType.TECHNOLOGY_BUSINESS.getValue().equals(biding.getBidingType());
            if (isTechBusiness) {
                if (CollectionUtils.isEmpty(bidOrderHeadVO.getOrderHeadFiles())) {
                    throw new BaseException("??????+??????????????????????????????????????????????????????????????????????????????");
                }
                bidOrderHeadVO.getOrderHeadFiles().stream()
                        .filter(e -> Objects.equals(e.getVendorReferenceFileType(), BidingFileTypeEnum.TECHNICAL_BID.getCode()))
                        .findAny().orElseThrow(() -> new BaseException("??????+??????????????????????????????????????????????????????????????????????????????"));
            }
        }
        if (!CollectionUtils.isEmpty(bidOrderHeadVO.getOrderHeadFiles())) {
            boolean match = bidOrderHeadVO.getOrderHeadFiles().stream().anyMatch(e -> Objects.isNull(e.getVendorReferenceFileType()));
            if (match) {
                throw new BaseException("?????????????????????????????????");
            }
        }

        // ??????????????????????????????????????????????????????????????????
        this.judgeBidingConditions(bidOrderHeadVO);

        // ??????????????????????????????????????????
        this.judgeSaveOrderInfoPerssion(bidOrderHeadVO);

        if (OrderStatusEnum.SUBMISSION.getValue().equals(bidOrderHeadVO.getOrderStatus()))
            throw new BaseException("????????????????????????????????????");

        // ?????? ?????? - [?????????]
        bidOrderHeadVO.setOrderStatus(OrderStatusEnum.SUBMISSION.getValue());
        bidOrderHeadVO.setSubmitTime(new Date());


        bidOrderHeadVO.setOrderLines(orderLines);
        // ???????????????
        Long orderHeaderId = this.saveOrderInfo(bidOrderHeadVO, true);
        //??????????????????excel?????????????????????
        generateFormulaPricesAndUpload(orderHeaderId,bidOrderHeadVO.getFormulaPriceDTOList());
        return true;
      /*  // ?????? ?????????
        return this.updateOrderLineStatus(orderHeaderId, bidOrderHeadVO.getOrderStatus());*/
    }

    /**
     * ??????????????????excel?????????????????????
     * @param orderHeaderId
     * @param formulaPriceDTOList
     */
    private void generateFormulaPricesAndUpload(Long orderHeaderId, List<FormulaPriceDTO> formulaPriceDTOList) throws IOException {
        if(CollectionUtils.isEmpty(formulaPriceDTOList)){
            return;
        }
        //????????????
        Workbook workbook = createWorkbookModel(formulaPriceDTOList);
        MultipartFile file = tranferFile(orderHeaderId,workbook);
        String sourceType = "??????????????????";
        String uploadType = FileUploadType.FASTDFS.name();
        String fileModular = "bid";
        String fileFunction = "??????????????????";
        String fileType = "xlsx";
        Fileupload fileUpload = fileCenterClient.feignClientUpload(file,sourceType,uploadType,fileModular,fileFunction,fileType);
        fileCenterClient.binding(new LinkedList<Long>(){{
            add(fileUpload.getFileuploadId());
        }},orderHeaderId);
    }

    private MultipartFile tranferFile(Long orderHeaderId,Workbook workbook) throws IOException {
        String name = new StringBuffer()
                .append(new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                .append("-??????????????????")
                .append("-")
                .append(orderHeaderId)
                .append(".xlsx")
                .toString();
        String originalFilename = name;
        String contentType = "";

        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] barray=bos.toByteArray();
        InputStream is=new ByteArrayInputStream(barray);
        MultipartFile multipartFile = new MockMultipartFile("file",originalFilename,contentType,is);
        return multipartFile;
    }

    private Workbook createWorkbookModel(List<FormulaPriceDTO> formulaPriceDTOList){
        // ???????????????
        XSSFWorkbook workbook = new XSSFWorkbook();
        // ???????????????:???????????????
        XSSFSheet sheet = workbook.createSheet("sheet");
        // ?????????????????????
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        // ????????????????????????:????????????
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        // ????????????????????????:????????????
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // ??????????????????
        Font font = workbook.createFont();
        font.setBold(true);
        cellStyle.setFont(font);

        cellStyle.setBorderBottom(BorderStyle.THIN); //?????????
        cellStyle.setBorderLeft(BorderStyle.THIN);//?????????
        cellStyle.setBorderTop(BorderStyle.THIN);//?????????
        cellStyle.setBorderRight(BorderStyle.THIN);//?????????


        List<FormulaPriceDTO> type1 = new ArrayList<>();
        List<FormulaPriceDTO> type2 = new ArrayList<>();
        List<FormulaPriceDTO> type3 = new ArrayList<>();
        for(FormulaPriceDTO formulaPriceDTO : formulaPriceDTOList){
            if(FormulaPriceExportTypeEnum.TYPE1.getValue().equals(formulaPriceDTO.getType())){
                type1.add(formulaPriceDTO);
            }
            if(FormulaPriceExportTypeEnum.TYPE2.getValue().equals(formulaPriceDTO.getType())){
                type2.add(formulaPriceDTO);
            }
            if(FormulaPriceExportTypeEnum.TYPE3.getValue().equals(formulaPriceDTO.getType())){
                type3.add(formulaPriceDTO);
            }
        }

        //???index
        int rowIndex = 0;

        //????????????????????????
        for(int i=0;i<type1.size();i++){
            //?????????????????????
            int cellIndex = 0;
            FormulaPriceDTO formulaPriceDTO = type1.get(i);
            XSSFRow row = sheet.createRow(rowIndex);

            XSSFCell cell1 = row.createCell(cellIndex);
            cell1.setCellStyle(cellStyle);
            cell1.setCellValue(formulaPriceDTO.getContent());

            rowIndex ++;
        }

        //????????????????????????
        for(int i=0;i<type3.size();i++){
            //?????????????????????
            int cellIndex = 0;
            FormulaPriceDTO formulaPriceDTO = type3.get(i);
            XSSFRow row = sheet.createRow(rowIndex);

            XSSFCell cell1 = row.createCell(cellIndex);
            cell1.setCellStyle(cellStyle);
            cell1.setCellValue(formulaPriceDTO.getBaseMaterialName());
            cellIndex ++;

            XSSFCell cell2 = row.createCell(cellIndex);
            cell2.setCellStyle(cellStyle);
            cell2.setCellValue(formulaPriceDTO.getBaseMaterialPrice());
            cellIndex ++;

            rowIndex ++;
        }

        //??????????????????
        for(int i=0;i<type2.size();i++){
            //?????????????????????
            int cellIndex = 0;
            FormulaPriceDTO formulaPriceDTO = formulaPriceDTOList.get(i);
            XSSFRow row = sheet.createRow(rowIndex);

            XSSFCell cell1 = row.createCell(cellIndex);
            cell1.setCellStyle(cellStyle);
            cell1.setCellValue(formulaPriceDTO.getOrgName());
            cellIndex ++;

            XSSFCell cell2 = row.createCell(cellIndex);
            cell2.setCellStyle(cellStyle);
            cell2.setCellValue(formulaPriceDTO.getMaterialItemDesc());
            cellIndex ++;

            JSONArray jsonArray = JSON.parseArray(formulaPriceDTO.getContent());
            for(int j=0;j<jsonArray.size();j++){
                //?????????
                JSONObject jsonObject = (JSONObject) jsonArray.get(j);
                String value = jsonObject.getString("value");
                XSSFCell cell3 = row.createCell(cellIndex);
                cell3.setCellStyle(cellStyle);
                cell3.setCellValue(value);
                cellIndex ++;
            }
            rowIndex ++;

        }
        return workbook;
    }


    private boolean updateOrderLineStatus(Long orderHeadId, String orderStatus) {
        boolean flag = false;
        List<OrderLine> orderLines = iOrderLineService.list(new QueryWrapper<>(new OrderLine().setOrderHeadId(orderHeadId)));
        List<OrderLine> updateLines = new ArrayList<>();
        for (OrderLine line : orderLines) {
            line.setOrderStatus(orderStatus);
            line.setWin(null);
            updateLines.add(line);
        }
        //????????????????????????
        if (updateLines.size() > 0) {
            flag = iOrderLineService.updateBatchById(updateLines);
        }
        return flag;
    }


    /**
     * ???????????????ID??????????????????ID?????????ID????????????????????????
     * ?????????????????????????????????????????????
     *
     * @param bidingId
     * @param bidVendorId
     * @param roundId
     * @return
     */
    private OrderHead alreadyOrderHeadByBidingAndBidVendorIdAndRoundId(long bidingId, long bidVendorId, long roundId) {
        QueryWrapper queryWrapper = new QueryWrapper<>(new OrderHead().setBidingId(bidingId).setBidVendorId(bidVendorId).setBidRoundId(roundId));
        queryWrapper.in("ORDER_STATUS", OrderStatusEnum.DRAFT.getValue(), OrderStatusEnum.SUBMISSION.getValue());
        return this.getOne(queryWrapper);
    }


    /**
     * ?????????????????????????????????
     *
     * @param supplierBidingVO
     * @return
     */
    @Override
    public void judgeBidingConditions(SupplierBidingVO supplierBidingVO) {
        /**
         * ??????bidingId??????????????????????????????
         * 1 ???????????????????????????????????????
         * 2 ??????????????????????????????????????????round?????????????????????
         *      ????????????????????? = Y ??????????????????????????????????????????
         *      ???????????? > ??????/???????????????????????????????????????????????????????????????
         */

        boolean notRoundData = (null == supplierBidingVO.getRoundId());
        if (notRoundData) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????"));
        }
        //????????????????????????
        Date endTime = supplierBidingVO.getEndTime();
        Date currentTime = new Date();
        //date1??????date2??????-1???date1??????date2??????1???????????????0
        // ??????????????? ?????? ???????????????????????????????????????
        int compareTo = currentTime.compareTo(endTime);
        if (compareTo != -1) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????"));
        }
        //????????????
        String businessOpenBid = supplierBidingVO.getBusinessOpenBid();
        if (YesOrNo.YES.getValue().equals(businessOpenBid)) {
            throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????????????????"));
        }

        //2 ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        boolean signUpData = (null != supplierBidingVO.getSignUpId() && !SignUpStatus.REJECTED.equals(supplierBidingVO.getSignUpStatus()));
        if (!signUpData) {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????"));
        }
    }

    @Override
    public BidOrderHeadVO getOrderHeadInfo(BidOrderHeadVO bidOrderHeadVO) {
        Long bidingId = bidOrderHeadVO.getBidingId();
        Long vendorId = bidOrderHeadVO.getVendorId();
        Long bidVendorId = bidOrderHeadVO.getBidVendorId();

        BidOrderHeadVO orderHeadVO = new BidOrderHeadVO();
        BeanCopyUtil.copyProperties(orderHeadVO, bidOrderHeadVO);
        //1 ??????????????????
        Long orderHeadId = bidOrderHeadVO.getOrderHeadId();
        if (null != orderHeadId) {
            OrderHead orderHead = this.getById(orderHeadId);
            BeanCopyUtil.copyProperties(orderHeadVO, orderHead);
        }

        /*
         * ?????? - ???????????? ???????????????
         *
         * @author zixuan.yan@meicloud.com
         */
        Optional.ofNullable(bidVendorDao.findOne(Wrappers.lambdaQuery(BidVendor.class).eq(BidVendor::getBidVendorId, bidVendorId)))
                .ifPresent(bidVendor -> {
                    if (!"Y".equals(bidOrderHeadVO.getIsProxyBidding()) && !vendorId.equals(bidVendor.getVendorId()))   // ????????????????????????????????????????????????
                        throw new BaseException("????????????bidVendorId???[" + bidVendorId + "]?????????");
                    orderHeadVO.setVendorId(bidVendor.getVendorId());
                    orderHeadVO.setVendorCode(bidVendor.getVendorCode());
                });

        //2 ??????????????????
        List<BidOrderLineVO> bidOrderLineVOs = getOrderLineVOS(bidingId, orderHeadId, bidVendorId);
        orderHeadVO.setOrderLines(bidOrderLineVOs);

        //3 ??????????????????
        BidVendorFileVO vendorFileVO = new BidVendorFileVO();
        vendorFileVO.setBusinessId(orderHeadId);
        vendorFileVO.setFileType(VendorFileType.BIDING.getValue());
        vendorFileVO.setBidingId(bidingId);
        vendorFileVO.setVendorId(orderHeadVO.getVendorId());
        List<BidVendorFileVO> vendorFileVOs = iBidVendorFileService.getVendorFileList(vendorFileVO);
        orderHeadVO.setFiles(vendorFileVOs);
        List<BidFileConfig> list = fileConfigService.list(Wrappers.lambdaQuery(BidFileConfig.class)
                .eq(BidFileConfig::getBidingId, bidingId));
        orderHeadVO.setConfigFiles(list);
        List<OrderHeadFile> files = orderHeadFileMapper.selectList(Wrappers.lambdaQuery(OrderHeadFile.class)
                .eq(OrderHeadFile::getOrderHeadId, orderHeadId));
        orderHeadVO.setOrderHeadFiles(files);
        BidRequirement demandHeader = Optional.ofNullable(bidRequirementDao.findOne(
                Wrappers.lambdaQuery(BidRequirement.class)
                        .select(BidRequirement::getRequirementId, BidRequirement::getPricingType)
                        .eq(BidRequirement::getBidingId, bidingId)
        )).orElseThrow(() -> new BaseException("?????????????????????[???]????????? | biddingId: [" + bidingId + "]"));
        orderHeadVO.setPricingType(demandHeader.getPricingType());
        return orderHeadVO;
    }

    @Override
    public List<BidOrderLineVO> getOrderLineVOS(Long biddingId, Long orderHeadId, Long bidVendorId) {

        // ?????? ???????????????[???]
        BidRequirement demandHeader = Optional.ofNullable(bidRequirementDao.findOne(
                Wrappers.lambdaQuery(BidRequirement.class).select(BidRequirement::getRequirementId, BidRequirement::getPricingType).eq(BidRequirement::getBidingId, biddingId)
        )).orElseThrow(() -> new BaseException("?????????????????????[???]????????? | biddingId: [" + biddingId + "]"));

        // ?????? ???????????????[???]??? & ???[?????????ID]??????
        Map<Long, BidRequirementLine> demandLines = bidRequirementLineDao
                .findAll(Wrappers.lambdaQuery(BidRequirementLine.class)
                        .eq(BidRequirementLine::getBidingId, biddingId)
                )
                .stream()
                .collect(Collectors.toMap(BidRequirementLine::getRequirementLineId, x -> x));

        // ??????/?????? ??????????????????
        List<BidOrderLineVO> orderLines = this.findOrderLines(biddingId, orderHeadId, bidVendorId, demandLines);

        // ????????????
        orderLines.forEach(orderLine -> {

            // ?????? ???????????????[???]
            BidRequirementLine demandLine = Optional.ofNullable(demandLines.get(orderLine.getRequirementLineId()))
                    .orElseThrow(() -> new BaseException("?????????????????????????????? | demandLineId: [" + orderLine.getRequirementLineId() + "]"));

            // ?????? ?????????
            orderLine.setItemGroup(demandLine.getItemGroup());
            // ?????? ????????????
            orderLine.setMaterialMatching(demandLine.getMaterialMatching());
            //????????????????????????
            if (!Objects.equals(demandLine.getShowRequireNum(), "Y")) {
                orderLine.setQuantity(null);
            }
            if (StringUtils.isEmpty(orderLine.getUomDesc())) {
                orderLine.setUomDesc(orderLine.getUomCode());
            }
        });

        // ?????????????????????????????????????????????
        if (RequirementPricingType.FORMULA_PURCHASER.getCode().equals(demandHeader.getPricingType())) {

            // ?????? ?????? ????????????
            Map<Long, List<MaterialItemAttributeRelateVO>> materialAttributes = materialItemAttributeClient.getKeyFeatureMaterialAttributes(
                    orderLines.stream()
                            .filter(orderLine -> orderLine.getTargetId() != null)
                            .map(BidOrderLineVO::getTargetId)
                            .distinct()
                            .collect(Collectors.toList())
            );

            orderLines.forEach(orderLine -> Optional.ofNullable(orderLine.getOrderLineId()).ifPresent(orderLineId -> {

                // ????????????????????????
                if (orderLine.getTargetId() == null)
                    return;

                // ?????? ??????????????????
                BidOrderLineFormulaPriceDetail formulaPriceDetail = formulaPriceDetailService.findDetailsByLineId(orderLineId).stream()
                        .findAny()
                        .orElseGet(() -> {
                            Map<Long, String> materialAttributesMap = new HashMap<>();
                            materialAttributes.getOrDefault(orderLine.getTargetId(), Collections.emptyList())
                                    .forEach(materialAttribute ->
                                            materialAttributesMap.put(materialAttribute.getMaterialAttributeId(), materialAttribute.getAttributeValue())
                                    );
                            return BidOrderLineFormulaPriceDetail.builder()
                                    .materialExtValues(JSON.toJSONString(materialAttributesMap, SerializerFeature.WriteNonStringKeyAsString))
                                    .build();
                        });
                orderLine.setFormulaPriceDetailId(formulaPriceDetail.getId());
                orderLine.setEssentialFactorValues(formulaPriceDetail.getEssentialFactorValues());
                BidRequirementLine bidRequirementLine = demandLines.get(orderLine.getRequirementLineId());
                if (Objects.nonNull(formulaPriceDetail.getMaterialExtValues()) && Objects.nonNull(bidRequirementLine.getOrgId())) {
                    Map<String, Object> map = JSON.parseObject(formulaPriceDetail.getMaterialExtValues(), Map.class);
                    map.put("-1", bidRequirementLine.getOrgName());
                    if (Objects.equals(bidRequirementLine.getIsSeaFoodFormula(), "Y")) {
                        map.put("-2", bidRequirementLine.getTargetDesc());
                        map.put("-3", orderLine.getOrderLineId());
                    }
                    String value = JSON.toJSONString(map);
                    orderLine.setMaterialExtValues(value);
                }
            }));
        }

        return orderLines;
    }

    protected List<BidOrderLineVO> findOrderLines(Long biddingId, Long orderHeaderId, Long bidVendorId, Map<Long, BidRequirementLine> demandLines) {

        // ?????? ???????????????
        Biding biding = Optional
                .ofNullable(biddingDao.findOne(Wrappers.lambdaQuery(Biding.class)
                        .select(Biding::getBidingId, Biding::getCurrentRound)
                        .eq(Biding::getBidingId, biddingId)
                ))
                .orElseThrow(() -> new BaseException("???????????????????????? | biddingId: [" + biddingId + "]"));


        /*
         * ?????????????????????????????????
         * 1. ????????????????????????????????????????????????????????????
         * 2. ???????????????????????????????????????????????????????????????
         */
        if (orderHeaderId == null) {

            // ????????????????????????????????????????????????????????????
            if (firstRound.equals(biding.getCurrentRound()))
                return iOrderLineService.getRequirementLineByBidingIdAndBidVendorId(biddingId, bidVendorId);

            // ????????????????????????????????????????????????????????????????????????????????????????????????
            List<BidOrderLineVO> result = iOrderLineService.getWinOrderLineByBidingIdAndVendorId(biddingId, bidVendorId);
            for (BidOrderLineVO bidOrderLineVO : result) {
                BidRequirementLine bidRequirementLine = demandLines.get(bidOrderLineVO.getRequirementLineId());
                BeanUtils.copyProperties(bidOrderLineVO, bidRequirementLine, BeanCopyUtil.getNullPropertyNames(bidOrderLineVO));
            }
            return result;
        }

        /*
         * ??????????????????????????????
         * 1. ???????????????????????????
         */
        return iOrderLineService.getOrderLineByOrderHeadId(orderHeaderId);
    }

    @Override
    public List<BidOrderLineVO> matchOrderLine(Long bidingId, Long orderHeadId, Long bidVendorId, List<Object> list) {
        List<BidOrderLineVO> orderLineVOS = null;
        orderLineVOS = getOrderLineVOS(bidingId, orderHeadId, bidVendorId);
        orderLineVOS.forEach(basic -> {
            for (Object obj : list) {
                BidOrderLineVO excle = (BidOrderLineVO) obj;
                if (basic.getRequirementLineId().equals(excle.getRequirementLineId())) {
                    basic.setPrice(excle.getPrice());
                    basic.setTaxRate(excle.getTaxRate());
                }
            }
        });
        return orderLineVOS;
    }

    @Override
    public void judgeSaveOrderInfoPerssion(BidOrderHeadVO bidOrderHeadVO) {
        /**
         * ???????????????????????????????????????
         * ?????????????????????????????????????????????
         *
         */
        List<BidOrderLineVO> orderLineVOS = bidOrderHeadVO.getOrderLines();
        Set<String> itemGroupSet = new HashSet();
        for (BidOrderLineVO orderLineVO : orderLineVOS) {
            itemGroupSet.add(orderLineVO.getItemGroup());
        }
        log.info("itemGroupSet : {}", itemGroupSet);
        for (String itemGroup : itemGroupSet) {
            if (null == itemGroup || "".equals(itemGroup)) {
                continue;
            }
            Set<BigDecimal> priceSet = new HashSet();
            for (BidOrderLineVO orderLineVO : orderLineVOS) {
                if (itemGroup.equals(orderLineVO.getItemGroup())) {
                    priceSet.add(orderLineVO.getPrice());
                }
            }
            int len = priceSet.size();
            if (len > 1) {
                if (priceSet.contains(null)) {
                    throw new BaseException(LocaleHandler.getLocaleMsg("?????????????????????????????????"));
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOrderForSupplier(Long bidSupplierId, OrderHead lastHead, Long bidingId, Round round) {
        //??????????????????????????????
        List<BidOrderLineVO> bidOrderLineVOs = getOrderLineVOS(bidingId, lastHead.getOrderHeadId(), bidSupplierId);
        for (int i = bidOrderLineVOs.size() - 1; i >= 0; i--) {
            BidOrderLineVO bidOrderLineVO = bidOrderLineVOs.get(i);
            if (!Objects.equals(bidOrderLineVO.getWin(), "Y") || Objects.equals(bidOrderLineVO.getWin(), "Q")) {
                bidOrderLineVOs.remove(i);
            }
        }
        if (CollectionUtils.isEmpty(bidOrderLineVOs)) {
            return;
        }
        OrderHead orderHead = BeanCopyUtil.copyProperties(lastHead, OrderHead::new);
        String bidOrderNum = baseClient.seqGen(SequenceCodeConstant.SEQ_BID_BIDING_CODE);
        Long newOrderHeaderId = IdGenrator.generate();
        orderHead.setOrderHeadId(newOrderHeaderId);
        orderHead.setBidRoundId(round.getRoundId());
        orderHead.setBidOrderNum(bidOrderNum);
        orderHead.setBidingId(bidingId);
        orderHead.setRound(round.getRound());
        orderHead.setSubmitTime(new Date());
        orderHead.setOrderStatus(OrderStatusEnum.SUBMISSION.getValue());
        save(orderHead);
        String pricingType = lastHead.getPricingType();
        Set<Long> materialIds = new HashSet<>();
        List<Long> orderLineIds = bidOrderLineVOs.stream().map(BidOrderLineVO::getOrderLineId).collect(Collectors.toList());
        Map<Long, List<OrderlinePaymentTerm>> paymentMap = paymentTermMapper.selectList(Wrappers.lambdaQuery(OrderlinePaymentTerm.class)
                .in(OrderlinePaymentTerm::getOrderLineId, orderLineIds)
        ).stream().collect(Collectors.groupingBy(OrderlinePaymentTerm::getOrderLineId));
        bidOrderLineVOs.forEach(orderLineVO -> {
            List<OrderlinePaymentTerm> orderLinePaymentTerms = paymentMap.get(orderLineVO.getOrderLineId());
            for (OrderlinePaymentTerm orderlinePaymentTerm : orderLinePaymentTerms) {
                orderlinePaymentTerm.setPaymentTermId(null);
                orderlinePaymentTerm.setOrderLineId(null);
            }
            orderLineVO.setPaymentTermList(orderLinePaymentTerms);
            orderLineVO.setOrderLineId(null);
            orderLineVO.setBidVendorId(orderHead.getBidVendorId());
            orderLineVO.setOrderHeadId(orderHead.getOrderHeadId());
            orderLineVO.setBidingId(bidingId);
            orderLineVO.setRound(round.getRound());
            orderLineVO.setWin(null);
            materialIds.add(orderLineVO.getTargetId());
        });
        iOrderLineService.saveBatchOrderLines(bidOrderLineVOs, true, materialIds);
        if (RequirementPricingType.SIMPLE_PURCHASER.getCode().equals(pricingType)) {
            return;
        }
        List<BidOrderLineFormulaPriceDetail> details = new LinkedList<>();
        List<BidOrderLineFormulaPriceDetail> formulaPriceDetails = null;
        if (RequirementPricingType.FORMULA_PURCHASER.getCode().equals(pricingType)) {
            formulaPriceDetails = formulaPriceDetailService.findDetailsByLineId(bidOrderLineVOs.get(0).getOrderLineId());
        }
        for (BidOrderLineVO bidOrderLineVO : bidOrderLineVOs) {
            //??????????????????
            if (RequirementPricingType.FORMULA_PURCHASER.getCode().equals(pricingType)) {
                for (BidOrderLineFormulaPriceDetail formulaPriceDetail : formulaPriceDetails) {
                    formulaPriceDetail.setId(null);
                    formulaPriceDetail.setHeaderId(bidOrderLineVO.getOrderHeadId());
                    formulaPriceDetail.setLineId(bidOrderLineVO.getOrderLineId());
                    details.add(formulaPriceDetail);
                }
            }
            //????????????????????????
            if (RequirementPricingType.MODEL_PURCHASER.getCode().equals(pricingType)) {
                List<BidOrderLineTemplatePriceDetailVO> templatePriceDetailVOS = templatePriceDetailService.findDetailsByLineId(bidOrderLineVO.getOrderLineId());
                for (BidOrderLineTemplatePriceDetailVO detailVO : templatePriceDetailVOS) {
                    BidOrderLineTemplatePriceDetail templatePriceDetail = detailVO.getTemplatePriceDetail();
                    templatePriceDetail.setId(null);
                    templatePriceDetail.setLineId(bidOrderLineVO.getOrderLineId());
                    templatePriceDetail.setHeaderId(bidOrderLineVO.getOrderHeadId());
                    List<BidOrderLineTemplatePriceDetailLine> templatePriceDetailLines = detailVO.getTemplatePriceDetailLines();
                    for (BidOrderLineTemplatePriceDetailLine line : templatePriceDetailLines) {
                        line.setId(null);
                        line.setHeaderId(null);
                    }
                }
                templatePriceDetailService.saveDetails(templatePriceDetailVOS);
            }

        }
        if (!CollectionUtils.isEmpty(details)) {
            formulaDetailService.saveBatch(details);
        }

    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public BidOrderHeadVO getOrCreateOrderHeadInfo(BidOrderHeadVO queryParam) {

        if (queryParam.getOrderHeadId() == null) {

            // ?????????????????????
            BidOrderHeadVO orderHeader = getOrderHeadInfo(queryParam);

            // ??????
            Long orderHeaderId = saveOrderInfo(orderHeader, false);

            // ???????????????ID
            queryParam.setOrderHeadId(orderHeaderId);
            orderHeader.setOrderHeadId(orderHeaderId);
            List<BidOrderLineVO> orderLineVOS = getOrderLineVOS(queryParam.getBidingId(), orderHeaderId, queryParam.getBidVendorId());
            orderHeader.setOrderLines(orderLineVOS);
            /**
             * ????????????????????????
             */
            Date endTime = orderHeader.getEndTime();
            Long remainingTime = null == endTime ? 0L : endTime.getTime() - new Date().getTime();
            orderHeader.setRemainingTime(remainingTime >= 0 ? remainingTime : 0L);
            return orderHeader;
        }

        BidOrderHeadVO orderHeadInfo = getOrderHeadInfo(queryParam);

        /**
         * ????????????????????????
         */
        Date endTime = orderHeadInfo.getEndTime();
        Long remainingTime = null == endTime ? 0L : endTime.getTime() - new Date().getTime();
        orderHeadInfo.setRemainingTime(remainingTime >= 0 ? remainingTime : 0L);
        return orderHeadInfo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateOrderLinePrice(Long lineId, BigDecimal price, String taxKey ,BigDecimal taxRate) {

        // ?????? ?????????
        OrderLine orderLine = iOrderLineService.getById(lineId);
        Assert.notNull(orderLine, "???????????????????????? | lineId: [" + lineId + "]");

        // ????????????
        orderLine.setPrice(price);
        // ????????????
        orderLine.setTaxKey(taxKey);
        orderLine.setTaxRate(taxRate);
        // persist.
        iOrderLineService.saveOrUpdate(orderLine);
    }

    @Override
    public List<BidVendor> findNotPricingVendors(Long biddingId) {

        // ?????? ?????????
        Biding bidding = Optional.ofNullable(biddingDao.findById(biddingId))
                .orElseThrow(() -> new BaseException("???????????????????????? | biddingId: [" + biddingId + "]"));

        if (bidding.getCurrentRound() == null || bidding.getCurrentRound() == 1) {
            List<BidVendor> firstRoundNotPricingVendors = getBaseMapper().findFirstRoundNotPricingVendors(biddingId);
            firstRoundNotPricingVendors.forEach(e->e.setRound(1));
            return firstRoundNotPricingVendors;
        }


        // ?????? ?????????????????????????????????????????????????????????
        Map<Long, OrderLine> allowNextRoundOrderLines = orderLineDao
                .findAll(Wrappers.lambdaQuery(OrderLine.class)
                        .eq(OrderLine::getBidingId, bidding.getBidingId())
                        .eq(OrderLine::getRound, bidding.getCurrentRound() - 1)
                        .eq(OrderLine::getOrderStatus, OrderStatusEnum.SUBMISSION.getValue())
                )
                .stream()
                .collect(Collectors.toMap(OrderLine::getOrderLineId, x -> x));

        // ?????? ??????????????????????????????ID
        Set<Long> allowNextRoundBidVendorIds = allowNextRoundOrderLines.values().stream()
                .map(OrderLine::getBidVendorId)
                .collect(Collectors.toSet());


        // ?????? ????????????????????????????????????
        Map<Long, OrderLine> currentRoundOrderLines = orderLineDao
                .findAll(Wrappers.lambdaQuery(OrderLine.class)
                        .eq(OrderLine::getBidingId, bidding.getBidingId())
                        .eq(OrderLine::getRound, bidding.getCurrentRound())
                        .eq(OrderLine::getOrderStatus, OrderStatusEnum.SUBMISSION.getValue())
                )
                .stream()
                .collect(Collectors.toMap(OrderLine::getOrderLineId, x -> x));

        // ?????? ???????????????????????????????????????ID
        Set<Long> currentRoundSubmittedBidVendorIds = currentRoundOrderLines.values().stream()
                .map(OrderLine::getBidVendorId)
                .collect(Collectors.toSet());


        // ?????? ???????????????????????????????????????ID
        Set<Long> currentRoundNotSubmitBidVendorIds = allowNextRoundBidVendorIds.stream()
                .filter(id -> !currentRoundSubmittedBidVendorIds.contains(id))
                .collect(Collectors.toSet());

        // ?????? ?????????????????????????????????
        List<BidVendor> all = bidVendorDao.findAll(Wrappers.lambdaQuery(BidVendor.class).in(BidVendor::getBidVendorId, currentRoundNotSubmitBidVendorIds));
        all.forEach(e->e.setRound(bidding.getCurrentRound()));
        return currentRoundNotSubmitBidVendorIds.isEmpty()
                ? Collections.emptyList()
                : all;
    }
}
