package com.midea.cloud.srm.inq.price.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.bargaining.BidClient;
import com.midea.cloud.srm.inq.price.mapper.ApprovalBiddingItemMapper;
import com.midea.cloud.srm.inq.price.mapper.InquiryHeaderReportMapper;
import com.midea.cloud.srm.inq.price.service.IApprovalBiddingItemService;
import com.midea.cloud.srm.inq.price.service.IApprovalHeaderService;
import com.midea.cloud.srm.inq.price.service.IApprovalItemService;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.inq.price.dto.*;
import com.midea.cloud.srm.model.inq.price.entity.ApprovalBiddingItem;
import com.midea.cloud.srm.model.inq.price.entity.ApprovalHeader;
import com.midea.cloud.srm.model.inq.price.entity.InquiryHeaderReport;
import com.midea.cloud.srm.model.inq.price.enums.SourcingType;
import com.midea.cloud.srm.model.inq.price.vo.ApprovalAllVo;
import com.midea.cloud.srm.model.inq.price.vo.ApprovalBiddingItemVO;
import com.midea.cloud.srm.model.inq.price.vo.ApprovalVo;
import com.midea.cloud.srm.model.pm.po.dto.NetPriceQueryDTO;
import io.swagger.models.auth.In;
import jdk.nashorn.internal.objects.annotations.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ????????????????????? ???????????????
 * </pre>
 *
 * @author linxc6@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-04-08 15:28:50
 *  ????????????:
 *          </pre>
 */
@RestController
@RequestMapping("/price/approval")
@Slf4j
public class ApprovalController extends BaseController {

    private final IApprovalHeaderService iApprovalHeaderService;
    @Resource
    private ApprovalBiddingItemMapper biddingItemMapper;
    @Resource
    private BidClient brgClient;
    @Resource
    private com.midea.cloud.srm.feign.bid.BidClient bidClient;
    @Resource
    private InquiryHeaderReportMapper reportMapper;

    @Autowired
    public ApprovalController(IApprovalHeaderService iApprovalHeaderService) {
        this.iApprovalHeaderService = iApprovalHeaderService;
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    @RequestMapping("/importModelDownload")
    public void importModelDownload(HttpServletResponse response) throws IOException {
        iApprovalHeaderService.importModelDownload(response);
    }

    /**
     * ?????????????????????????????????
     *
     * @param file
     */
    @RequestMapping("/importExcel")
    public Map<String, Object> importExcel(@RequestParam("file") MultipartFile file, Long approvalHeaderId, Fileupload fileupload) throws Exception {
        return iApprovalHeaderService.importExcel(file, approvalHeaderId, fileupload);
    }

    /**
     * ???????????????????????????
     *
     * @param insertPriceApprovalDTO
     */
    @PostMapping("/savePriceApproval")
    public Long savePriceApproval(@RequestBody InsertPriceApprovalDTO insertPriceApprovalDTO) {
        return iApprovalHeaderService.savePriceApproval(insertPriceApprovalDTO);
    }

    /**
     * ????????????
     */
    @PostMapping("/submit")
    public void ceeaSubmit(@RequestBody InsertPriceApprovalDTO insertPriceApprovalDTO) {
        iApprovalHeaderService.ceeaSubmit(insertPriceApprovalDTO);
    }

    /**
     * ?????????????????????
     *
     * @param insertPriceApprovalDTO
     */
    @PostMapping("/insertPriceApproval")
    public void insertPriceApproval(@RequestBody InsertPriceApprovalDTO insertPriceApprovalDTO) {
        iApprovalHeaderService.insertPriceApproval(insertPriceApprovalDTO);
    }

    /**
     * ????????????
     *
     * @param netPriceQueryDTO
     * @return
     */
    @PostMapping("/getApprovalNetPrice")
    public BigDecimal getApprovalNetPrice(@RequestBody NetPriceQueryDTO netPriceQueryDTO) {
        return iApprovalHeaderService.getApprovalNetPrice(netPriceQueryDTO);
    }

    /**
     * ?????????????????????
     */
    @PutMapping
    public void generatePriceApproval(Long inquiryId) {
        Assert.notNull(inquiryId, "?????????id????????????");
        iApprovalHeaderService.generatePriceApproval(inquiryId);
    }


    /**
     * ??????????????????????????????
     */
    @PostMapping("/header")
    public PageInfo<ApprovalHeaderQueryResponseDTO> queryHeader(@RequestBody ApprovalHeaderQueryRequestDTO request) {
        return iApprovalHeaderService.pageQuery(request);
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    @PostMapping("/listPage")
    public PageInfo<ApprovalHeader> ceeaListPage(@RequestBody ApprovalHeaderQueryRequestDTO request) {
        return iApprovalHeaderService.ceeaListPage(request);
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    @PostMapping("/ceeaQueryByCm")
    public List<ApprovalBiddingItem> ceeaQueryByCm(@RequestBody ApprovalBiddingItemDto approvalBiddingItemDto) {
        return iApprovalHeaderService.ceeaQueryByCm(approvalBiddingItemDto);
    }

    /**
     * ???????????????????????????menuId??????????????????????????????
     */
    @RequestMapping("/detail")
    public ApprovalHeaderDetailResponseDTO getApprovalDetail(Long approvalHeaderId, Long menuId) {
        return iApprovalHeaderService.getApprovalDetail(approvalHeaderId, menuId);
    }

    /**
     * ????????????????????????
     */
    @GetMapping("/approvalDetail")
    public ApprovalAllVo ceeaGetApprovalDetail(Long approvalHeaderId) {
        return iApprovalHeaderService.ceeaGetApprovalDetail(approvalHeaderId);
    }

    /**
     * ????????????????????????
     */
    @GetMapping("/getApprovalDetails")
    public ApprovalVo getApprovalDetails(@RequestParam("ceeaSourceNo") String ceeaSourceNo) {
        return iApprovalHeaderService.getApprovalDetails(ceeaSourceNo);
    }

    /**
     * ????????????
     */
    @PostMapping("/auditPass")
    public void auditPass(@RequestParam("approvalHeaderId") Long approvalHeaderId) {
        Assert.notNull(approvalHeaderId, LocaleHandler.getLocaleMsg("?????????id????????????"));
        ApprovalHeader header = iApprovalHeaderService.getById(approvalHeaderId);
        Assert.notNull(header, LocaleHandler.getLocaleMsg("????????????????????????"));
        iApprovalHeaderService.auditPass(header);
    }

    /**
     * ????????????
     */
    @RequestMapping("/reject")
    public void reject(Long approvalHeaderId) {
        Assert.notNull(approvalHeaderId, LocaleHandler.getLocaleMsg("?????????id????????????"));
        iApprovalHeaderService.reject(approvalHeaderId, null);
    }

    /**
     * ????????????
     *
     * @param approvalHeaderId
     */
    @GetMapping("/abandon")
    public void abandon(Long approvalHeaderId) {
        Assert.notNull(approvalHeaderId, "????????????id????????????");
        iApprovalHeaderService.abandon(approvalHeaderId);
    }


    /**
     * ????????????????????????
     */
    @PostMapping("/approvalToContract")
    public void ceeaApprovalToContract(@RequestParam("approvalHeaderId") Long approvalHeadId) {
        iApprovalHeaderService.ceeaApprovalToContract(approvalHeadId);
    }


    /**
     * ??????????????????????????????
     *
     * @param itemVOS
     * @return
     */
    @PostMapping("/genContractByApprovalBidingItemVOs")
    List<Long> genContractByApprovalBidingItemVOs(@RequestBody List<ApprovalBiddingItemVO> itemVOS) {
        return iApprovalHeaderService.genContractByApprovalBidingItemVOs(itemVOS);
    }

    /**
     * ?????????????????????????????????
     *
     * @param queryDto
     * @return
     */
    @PostMapping("/getItemVoByParam")
    List<ApprovalBiddingItemVO> getItemVoByParam(@RequestBody ApprovalToContractQueryDto queryDto) {
        return iApprovalHeaderService.getItemVoByParam(queryDto);
    }

    /**
     * ?????????????????????id????????????
     *
     * @param approvalHeaderId
     * @return
     */
    @GetMapping("/getContractTypeByApprovalHeaderId")
    List<Map<String, Object>> getContractTypeByApprovalHeaderId(@RequestParam("approvalHeaderId") Long approvalHeaderId) {
        return iApprovalHeaderService.getContractTypeByApprovalHeaderId(approvalHeaderId);
    }

    /**
     * ???????????????????????????id????????????
     *
     * @param approvalHeaderId
     * @return
     */
    @GetMapping("/getOrgInfoByApprovalHeaderId")
    List<Map<String, Object>> getOrgInfoByApprovalHeaderId(@RequestParam("approvalHeaderId") Long approvalHeaderId) {
        return iApprovalHeaderService.getOrgInfoByApprovalHeaderId(approvalHeaderId);
    }

    /**
     * ??????????????????
     *
     * @param itemVOS
     */
    @PostMapping("/updateItemsBelongNumber")
    public void updateApprovalBidingInfo(@RequestBody Collection<ApprovalBiddingItemVO> itemVOS) {
        iApprovalHeaderService.updateItemsBelongNumber(itemVOS);
    }

    @PostMapping("/resetItemsBelongNumber")
    public void resetItemsBelongNumber(@RequestBody Collection<Long> contractIds) {
        if (CollectionUtils.isEmpty(contractIds)) {
            return;
        }
        iApprovalHeaderService.resetItemsBelongNumber(contractIds);
    }

    /**
     * ???????????????id???????????????????????????????????????
     */
    @GetMapping("/checkWhetherCreateContract")
    public boolean checkWhetherCreateContract(@RequestParam("approvalHeaderId") Long approvalHeaderId) {
        Integer count = biddingItemMapper.selectCount(Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                .eq(ApprovalBiddingItem::getApprovalHeaderId, approvalHeaderId)
                .isNull(ApprovalBiddingItem::getFromContractId)
        );
        return count > 0;
    }

    @GetMapping("/dropApprovalHeader")
    @Transactional(rollbackFor = Exception.class)
    public void dropApprovalHeader(@RequestParam Long approvalHeaderId) {
        ApprovalHeader one = iApprovalHeaderService.getOne(Wrappers.lambdaQuery(ApprovalHeader.class)
                .select(ApprovalHeader::getSourceType, ApprovalHeader::getCeeaSourceNo, ApprovalHeader::getApprovalHeaderId)
                .eq(ApprovalHeader::getApprovalHeaderId, approvalHeaderId)
        );
        if (Objects.isNull(one)) {
            throw new BaseException(String.format("????????????????????????,id:%s", approvalHeaderId));
        }
        if (Objects.isNull(one.getSourceType()) || Objects.isNull(one.getCeeaSourceNo())) {
            return;
        }
        boolean bid = one.getSourceType().equals(SourcingType.TENDER.getItemValue());
        try {
            if (bid) {
                bidClient.changeBidingApprovalStatus("N", one.getCeeaSourceNo());
            } else {
                brgClient.changeBidingApprovalStatus("N", one.getCeeaSourceNo());
            }
        } catch (Exception e) {
            if (bid) {
                bidClient.changeBidingApprovalStatus("Y", one.getCeeaSourceNo());
            } else {
                brgClient.changeBidingApprovalStatus("Y", one.getCeeaSourceNo());
            }
            return;
        }
        iApprovalHeaderService.removeById(approvalHeaderId);
        biddingItemMapper.delete(Wrappers.lambdaQuery(ApprovalBiddingItem.class)
                .eq(ApprovalBiddingItem::getApprovalHeaderId, approvalHeaderId)
        );
        reportMapper.delete(Wrappers.lambdaQuery(InquiryHeaderReport.class)
                .eq(InquiryHeaderReport::getBiddingNo, one.getCeeaSourceNo())
        );


    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     * @param subsequentDocumentsNo
     * @return
     */
    @PostMapping("/listApprovalHeaderBysourceNo")
    public List<ApprovalHeader> listApprovalHeaderBysourceNo(@RequestBody List<String> subsequentDocumentsNo){
        if(CollectionUtils.isEmpty(subsequentDocumentsNo)){
            return Collections.EMPTY_LIST;
        }
        QueryWrapper<ApprovalHeader> wrapper = new QueryWrapper<>();
        wrapper.in("CEEA_SOURCE_NO",subsequentDocumentsNo);
        return iApprovalHeaderService.list(wrapper);
    }


}
