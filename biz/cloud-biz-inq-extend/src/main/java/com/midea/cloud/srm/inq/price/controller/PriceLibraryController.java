package com.midea.cloud.srm.inq.price.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.annotation.AuthData;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.enums.rbac.MenuEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.inq.price.mapper.PriceLibraryMapper;
import com.midea.cloud.srm.inq.price.service.IPriceLibraryPaymentTermService;
import com.midea.cloud.srm.inq.price.service.IPriceLibraryService;
import com.midea.cloud.srm.model.base.dict.dto.DictItemDTO;
import com.midea.cloud.srm.model.base.quotaorder.dto.QuotaParamDto;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.inq.price.domain.PriceLibraryAddParam;
import com.midea.cloud.srm.model.inq.price.domain.PriceLibraryLadderPriceAddParam;
import com.midea.cloud.srm.model.inq.price.dto.*;
import com.midea.cloud.srm.model.inq.price.entity.PriceLibrary;
import com.midea.cloud.srm.model.inq.price.vo.PriceLibraryExcelVO;
import com.midea.cloud.srm.model.inq.price.vo.PriceLibraryVO;
import com.midea.cloud.srm.model.pm.po.dto.NetPriceQueryDTO;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.supplier.info.dto.BidFrequency;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ??????-??????????????? ???????????????
 * </pre>
 *
 * @author huangbf3
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020/4/13 15:11
 *  ????????????:
 *          </pre>
 */
@RestController
@RequestMapping("/price/priceLibrary")
public class PriceLibraryController extends BaseController {

    @Autowired
    private IPriceLibraryService iPriceLibraryService;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private IPriceLibraryPaymentTermService iPriceLibraryPaymentTermService;
    @Resource
    private PriceLibraryMapper priceLibraryMapper;

    /**
     * ???????????????_?????? ??????????????????????????????
     *
     * @param priceLibraryParamDto
     * @return
     */
    @PostMapping("/queryValidByVendorItem")
    public boolean queryValidByVendorItem(@RequestBody PriceLibraryParamDto priceLibraryParamDto) {
        boolean flag = false;
        List<PriceLibrary> priceLibraries = iPriceLibraryService.list(Wrappers.lambdaQuery(PriceLibrary.class).
                eq(PriceLibrary::getVendorId, priceLibraryParamDto.getVendorId()).
                eq(PriceLibrary::getItemId, priceLibraryParamDto.getItemId()).
                eq(PriceLibrary::getCeeaOrgId, priceLibraryParamDto.getOrgId()).
                ge(PriceLibrary::getExpirationDate, LocalDate.now()).
                le(PriceLibrary::getEffectiveDate, LocalDate.now()));
        if (CollectionUtils.isNotEmpty(priceLibraries)) {
            flag = true;
        }
        return flag;
    }

    /**
     * ???????????????_??????_?????? ??????????????????????????????
     * @param priceLibraryParamDto
     * @return
     */
    @PostMapping("/queryValidByVendorItemDate")
    public boolean queryValidByVendorItemDate(@RequestBody PriceLibraryParamDto priceLibraryParamDto){
        boolean flag = false;
        List<PriceLibrary> priceLibraries = iPriceLibraryService.list(Wrappers.lambdaQuery(PriceLibrary.class).
                eq(PriceLibrary::getVendorId, priceLibraryParamDto.getVendorId()).
                eq(PriceLibrary::getItemId, priceLibraryParamDto.getItemId()).
                eq(PriceLibrary::getCeeaOrgId, priceLibraryParamDto.getOrgId()).
                ge(PriceLibrary::getExpirationDate, priceLibraryParamDto.getRequirementDate()).
                le(PriceLibrary::getEffectiveDate, priceLibraryParamDto.getRequirementDate()));
        if (CollectionUtils.isNotEmpty(priceLibraries)) {
            flag = true;
        }
        return flag;
    }

    /**
     * ?????????????????????_??????_?????? ??????????????????????????????
     *
     * @param quotaParamDto
     * @return
     */
    @PostMapping("/queryValidByBatchVendorItem")
    public boolean queryValidByBatchVendorItem(@RequestBody QuotaParamDto quotaParamDto) {
        boolean flag = false;
        if (CollectionUtils.isNotEmpty(quotaParamDto.getVendorIds()) &&
                null != quotaParamDto.getOrgId() && null != quotaParamDto.getItemId()) {
            List<PriceLibrary> priceLibraries = iPriceLibraryService.list(Wrappers.lambdaQuery(PriceLibrary.class).
                    in(PriceLibrary::getVendorId, quotaParamDto.getVendorIds()).
                    eq(PriceLibrary::getItemId, quotaParamDto.getItemId()).
                    eq(PriceLibrary::getCeeaOrgId, quotaParamDto.getOrgId()).
                    ge(PriceLibrary::getExpirationDate, null != quotaParamDto.getRequirementDate()?quotaParamDto.getRequirementDate():LocalDate.now()).
                    le(PriceLibrary::getEffectiveDate, null != quotaParamDto.getRequirementDate()?quotaParamDto.getRequirementDate():LocalDate.now()));
            if (CollectionUtils.isNotEmpty(priceLibraries) && priceLibraries.size() >= quotaParamDto.getVendorIds().size()) {
                flag = true;
            }
        }
        return flag;
    }

    /**
     * ????????????
     *
     * @param priceLibrary
     * @return
     */
    @PostMapping("/listPage")
    public PageInfo<PriceLibraryVO> listPage(@RequestBody PriceLibrary priceLibrary) {
        Assert.notNull(priceLibrary, "???????????????????????????");
        return iPriceLibraryService.PriceLibraryListPage(priceLibrary);
    }

    /**
     * ????????????????????????
     *
     * @param priceLibraryParam
     * @return
     */
    @PostMapping("/queryByContract")
    public List<PriceLibrary> queryByContract(@RequestBody PriceLibraryParam priceLibraryParam) {
        return iPriceLibraryService.queryByContract(priceLibraryParam);
    }

    @PostMapping("/excel")
    @AuthData(module = MenuEnum.PRICE_CATALOG)
    public void exportExcel(@RequestBody PriceLibrary priceLibrary, HttpServletResponse response) throws Exception {
        QueryWrapper<PriceLibrary> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getVendorName()), "VENDOR_NAME", priceLibrary.getVendorName());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getItemCode()), "ITEM_CODE", priceLibrary.getItemCode());
        wrapper.eq(priceLibrary.getCeeaOrgId() != null,"CEEA_ORG_ID",priceLibrary.getCeeaOrgId());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getCeeaOrgName()), "CEEA_ORG_NAME", priceLibrary.getCeeaOrgName());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getApprovalNo()), "APPROVAL_NO", priceLibrary.getApprovalNo());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getCeeaOrganizationName()), "CEEA_ORGANIZATION_NAME", priceLibrary.getOrganizationName());
        wrapper.like(StringUtils.isNotBlank(priceLibrary.getCategoryName()), "CATEGORY_NAME", priceLibrary.getCategoryName());
        wrapper.eq(StringUtils.isNotBlank(priceLibrary.getCeeaIfUse()),"CEEA_IF_USE",priceLibrary.getCeeaIfUse());
        wrapper.orderByDesc("CREATION_DATE");
        List<PriceLibrary> priceLibraryList = iPriceLibraryService.list(wrapper);

        if(CollectionUtils.isNotEmpty(priceLibraryList)){
            List<PriceLibraryExportDto> priceLibraryExportDtos = new ArrayList<>();
            priceLibraryList.forEach(priceLibrary1 -> {
                PriceLibraryExportDto priceLibraryExportDto = new PriceLibraryExportDto();
                BeanCopyUtil.copyProperties(priceLibraryExportDto,priceLibrary1);
                priceLibraryExportDtos.add(priceLibraryExportDto);
            });
            OutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, "????????????");
            EasyExcel.write(outputStream).head(PriceLibraryExportDto.class).sheet(0,"????????????").
                    doWrite(priceLibraryExportDtos);
        }

    }

    /**
     * ???????????????excel
     */
    private List<PriceLibraryExcelVO> convertToPriceLibraryExcel(List<PriceLibrary> libraries) {

        List<PriceLibraryExcelVO> excel = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(libraries)) {
            // ????????????
            List<DictItemDTO> sourceDict = baseClient.listAllByDictCode("SOURCE_FIND_MODE");
            libraries.forEach(priceLibrary -> {
                PriceLibraryExcelVO excelVO = new PriceLibraryExcelVO();
                BeanUtils.copyProperties(priceLibrary, excelVO);
                DictItemDTO item = sourceDict.stream().filter(f -> f.getDictItemCode().equals(priceLibrary.getSourceType())).findFirst().orElse(null);
                excelVO.setSourceType(item == null ? priceLibrary.getSourceType() : item.getDictItemName());
                excelVO.setEffectiveDate(DateUtil.parseDateToStr(priceLibrary.getEffectiveDate(), DateUtil.YYYY_MM_DD));
                excelVO.setExpirationDate(DateUtil.parseDateToStr(priceLibrary.getExpirationDate(), DateUtil.YYYY_MM_DD));
                excelVO.setCreationDate(DateUtil.parseDateToStr(priceLibrary.getCreationDate(), DateUtil.YYYY_MM_DD));
                excelVO.setLastUpdateDate(DateUtil.parseDateToStr(priceLibrary.getLastUpdateDate(), DateUtil.YYYY_MM_DD));
                excel.add(excelVO);
            });
        }

        return excel;
    }

    /**
     * ????????????????????????
     */
    @PutMapping
    public void saveBatch(@RequestBody List<PriceLibraryAddRequestDTO> request) {
        Assert.notNull(request, "??????????????????");
        iPriceLibraryService.generatePriceLibrary(convertPriceLibraryAddParam(request));
    }

    /**
     * ??????????????????
     */
    @DeleteMapping
    public void delete(Long priceLibraryId) {
        Assert.notNull(priceLibraryId, "priceLibraryId????????????");
        iPriceLibraryService.delete(priceLibraryId);
    }

    /**
     * ????????????????????????
     */
    @PostMapping
    public void updateBatch(@RequestBody List<PriceLibraryUpdateRequestDTO> request) {
        Assert.notNull(request, "??????????????????");
        iPriceLibraryService.updateBatch(convertPriceLibraryUpdateParam(request));
    }

    /**
     * ?????????????????????
     */
    @PostMapping("/updateBatch")
    private void ceeaUpdateBatch(@RequestBody List<PriceLibrary> priceLibraryList) {
        Assert.notNull(priceLibraryList, "??????????????????");
        iPriceLibraryService.ceeaUpdateBatch(priceLibraryList);
    }

    /**
     * ??????????????????
     */
    private List<PriceLibraryAddParam> convertPriceLibraryUpdateParam(List<PriceLibraryUpdateRequestDTO> request) {
        List<PriceLibraryAddParam> priceLibraryAddParams = new ArrayList<>();
        request.forEach(priceLibraryUpdateRequestDTO -> {
            PriceLibraryAddParam priceLibraryAddParam = new PriceLibraryAddParam();
            BeanUtils.copyProperties(priceLibraryUpdateRequestDTO, priceLibraryAddParam);

            if (CollectionUtils.isNotEmpty(priceLibraryUpdateRequestDTO.getLadderPrices())) {
                List<PriceLibraryLadderPriceAddParam> ladderPrices = new ArrayList<>();
                priceLibraryUpdateRequestDTO.getLadderPrices().forEach(priceLibraryLadderPriceUpdateDTO -> {
                    PriceLibraryLadderPriceAddParam ladderPriceAddParam = new PriceLibraryLadderPriceAddParam();
                    BeanUtils.copyProperties(priceLibraryLadderPriceUpdateDTO, ladderPriceAddParam);
                    ladderPrices.add(ladderPriceAddParam);
                });
                priceLibraryAddParam.setLadderPrices(ladderPrices);
            }
            priceLibraryAddParams.add(priceLibraryAddParam);
        });
        return priceLibraryAddParams;
    }

    /**
     * ??????????????????
     */
    private List<PriceLibraryAddParam> convertPriceLibraryAddParam(List<PriceLibraryAddRequestDTO> request) {

        List<PriceLibraryAddParam> priceLibraryAddParams = new ArrayList<>();
        request.forEach(priceLibraryAddRequestDTO -> {
            PriceLibraryAddParam priceLibraryAddParam = new PriceLibraryAddParam();
            BeanUtils.copyProperties(priceLibraryAddRequestDTO, priceLibraryAddParam);

            if (CollectionUtils.isNotEmpty(priceLibraryAddRequestDTO.getLadderPrices())) {
                List<PriceLibraryLadderPriceAddParam> ladderPrices = new ArrayList<>();
                priceLibraryAddRequestDTO.getLadderPrices().forEach(priceLibraryLadderPriceDTO -> {
                    PriceLibraryLadderPriceAddParam ladderPriceAddParam = new PriceLibraryLadderPriceAddParam();
                    BeanUtils.copyProperties(priceLibraryLadderPriceDTO, ladderPriceAddParam);
                    ladderPrices.add(ladderPriceAddParam);
                });
                priceLibraryAddParam.setLadderPrices(ladderPrices);
            }
            priceLibraryAddParams.add(priceLibraryAddParam);
        });
        return priceLibraryAddParams;
    }

    /**
     * ???????????? ??????????????????
     */
    @PostMapping("/getPriceLibraryByParam")
    PriceLibrary getPriceLibraryByParam(@RequestBody NetPriceQueryDTO netPriceQueryDTO) {
        return iPriceLibraryService.getPriceLibraryByParam(netPriceQueryDTO);
    }

    /**
     * @return
     * @Description ??????????????????
     * @Param [priceLibrary]
     * @Author haiping2.li@meicloud.com
     * @Date 2020.09.25 16:53
     **/
    @PostMapping("/getOnePriceLibrary")
    public PriceLibrary getOnePriceLibrary(@RequestBody PriceLibrary priceLibrary) {
        Assert.notNull(priceLibrary, "priceLibrary????????????");
        return iPriceLibraryService.getOnePriceLibrary(priceLibrary);
    }

    /**
     * ???????????? ??????????????????????????????
     */
    @PostMapping("listPriceLibraryByParam")
    List<PriceLibrary> listPriceLibraryByParam(@RequestBody List<NetPriceQueryDTO> netPriceQueryDTOList) {
        return iPriceLibraryService.listPriceLibraryByParam(netPriceQueryDTOList);
    }

    /**
     * ???????????? ????????????????????????
     */
    @PostMapping("/listPriceLibrary")
    List<PriceLibrary> listPriceLibrary(@RequestBody NetPriceQueryDTO netPriceQueryDTO) {
        return iPriceLibraryService.listPriceLibrary(netPriceQueryDTO);
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????
     *
     * @param priceLibrary
     * @return
     */
    @PostMapping("/ifHasPrice")
    String ifHasPrice(@RequestBody PriceLibrary priceLibrary) {
        return iPriceLibraryService.ifHasPrice(priceLibrary);
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    @PostMapping("/listEffectivePrice")
    List<PriceLibrary> listEffectivePrice(@RequestBody PriceLibrary priceLibrary) {
        return iPriceLibraryService.listEffectivePrice(priceLibrary);
    }

    /**
     * ????????????????????????????????????
     *
     * @param vendorId
     * @return
     */
    @PostMapping("/getThreeYearsBidFrequency")
    List<BidFrequency> getThreeYearsBidFrequency(@RequestParam("vendorId") Long vendorId) throws ParseException {
        return iPriceLibraryService.getThreeYearsBidFrequency(vendorId);
    }


    @PostMapping("/putOnShelves")
    public void putOnShelves(@RequestBody PriceLibraryPutOnShelvesDTO priceLibraryPutOnShelvesDTO) {
        iPriceLibraryService.putOnShelves(priceLibraryPutOnShelvesDTO);
    }

    @PostMapping("/pullOffShelves")
    public void pullOffShelves(@RequestBody List<PriceLibrary> priceLibraryList) {
        iPriceLibraryService.pullOffShelves(priceLibraryList);
    }

    /**
     * @Description ??????????????????
     * @Param: [response]
     * @Return: void
     * @Author: dengyl23@meicloud.com
     * @Date: 2020/9/29 14:57
     */
    @RequestMapping("/importModelDownload")
    public void importModelDownload(HttpServletResponse response) throws Exception {
        iPriceLibraryService.importModelDownload(response);
    }

    /**
     * @Description ??????????????????excel??????
     * @Param: [file]
     * @Return: void
     * @Author: dengyl23@meicloud.com
     * @Date: 2020/9/29 16:02
     */
    @RequestMapping("/importExcel")
    public void importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        iPriceLibraryService.importExcel(file);
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    @PostMapping("/listAllEffective")
    public List<PriceLibrary> listAllEffective(@RequestBody PriceLibrary priceLibrary) {
        return iPriceLibraryService.listAllEffective(priceLibrary);
    }

    /**
     * ????????????????????????
     * @return
     */
    @RequestMapping("/importModelDownloadNew")
    public void importModelDownloadNew (HttpServletResponse response) throws IOException {
        String fileName = "?????????????????????";
        ArrayList<PriceLibraryImportDTO> priceLibraryImportDTOS = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        EasyExcelUtil.writeExcelWithModel(outputStream,fileName,priceLibraryImportDTOS,PriceLibraryImportDTO.class);
    }

    /**
     * ???????????????????????????
     *
     * @param file
     * @throws Exception
     */
    @RequestMapping("/importInitDataExcel")
    public Map<String, Object> importInitDataExcel(@RequestParam("file") MultipartFile file) throws Exception {
        return iPriceLibraryService.importInitDataExcel(file);
    }

    /**
     * ????????????????????????????????????
     *
     * @param priceLibrary
     * @return
     */
    @PostMapping("/getLatest")
    public PriceLibrary getLatest(@RequestBody PriceLibrary priceLibrary) {
        System.out.println(JsonUtil.entityToJsonStr(priceLibrary));
        return iPriceLibraryService.getLatest(priceLibrary);
    }

    /**
     * ??????????????????id(??????)?????????id(??????)
     */
    @PostMapping("/getLatestFive")
    public List<PriceLibrary> getLatestFive(@RequestBody PriceLibraryRequestDTO priceLibraryRequestDTOList) {
        return iPriceLibraryService.getLatestFive(priceLibraryRequestDTOList);
    }

    @PostMapping("/batchSetContractCode")
    public void batchSetContractCode(@RequestBody Map<String, Object> param) {
        Object contractCode = param.get("contractCode");
        if (Objects.isNull(contractCode)) {
            throw new BaseException("?????????????????????");
        }
        Object contractName = param.get("contractName");
        if(Objects.isNull(contractName)){
            throw new BaseException("????????????????????????");
        }
        List<Long> priceLibraryIds = (List<Long>) param.get("priceLibraryIds");
        if (CollectionUtils.isEmpty(priceLibraryIds)) {
            return;
        }
        iPriceLibraryService.update(Wrappers.lambdaUpdate(PriceLibrary.class)
                .set(PriceLibrary::getContractName, contractName.toString())
                .set(PriceLibrary::getContractCode, contractCode.toString())
                .in(PriceLibrary::getPriceLibraryId, priceLibraryIds)
        );
    }

    /**
     * ?????????????????????????????????"???????????????"?????????????????????????????????????????????
     * ??????????????????+????????????+???????????????
     * @return
     */
    @PostMapping("/listForMaterialSecByBuyer")
    public PageInfo<PriceLibrary> listForMaterialSecByBuyer(@RequestBody PriceLibrary priceLibrary){
        return iPriceLibraryService.listForMaterialSecByBuyer(priceLibrary);
    }

    /**
     * ????????????-???????????? ??????????????? + ???????????? + ??????????????? ??????
     * ??????????????????+????????????+???????????????
     * @return
     */
    @PostMapping("/listForMaterialSecByVendor")
    public PageInfo<PriceLibrary> listForMaterialSecByVendor(@RequestBody PriceLibrary priceLibrary){
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if(loginAppUser.getCompanyId() == null){
            throw new BaseException("??????????????????????????????");
        }
        priceLibrary.setVendorId(loginAppUser.getCompanyId());
        return iPriceLibraryService.listForMaterialSecByVendor(priceLibrary);
    }

    /**
     * ??????????????????,????????????,???????????????,?????????,??????,??????????????????
     * @param priceLibraryCheckDto
     * @return
     */
    @PostMapping("/queryPriceLibraryByItemCodeNameVendorCode")
    public List<String> queryPriceLibraryByItemCodeNameVendorCode(@RequestBody PriceLibraryCheckDto priceLibraryCheckDto){
        List<String> result = new ArrayList<>();
        List<String> itemCodes = priceLibraryCheckDto.getItemCodes();
        List<String> itemNames = priceLibraryCheckDto.getItemNames();
        List<String> vendorCodes = priceLibraryCheckDto.getVendorCodes();
        if(CollectionUtils.isNotEmpty(itemCodes) &&
        CollectionUtils.isNotEmpty(itemNames) &&
        CollectionUtils.isNotEmpty(vendorCodes)){
            QueryWrapper<PriceLibrary> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("ITEM_CODE",itemCodes);
            queryWrapper.in("ITEM_DESC",itemNames);
            queryWrapper.in("VENDOR_CODE",vendorCodes);
            queryWrapper.le("EFFECTIVE_DATE",new Date());
            queryWrapper.ge("EXPIRATION_DATE",new Date());
            queryWrapper.eq("CEEA_IF_USE", YesOrNo.YES.getValue());
            List<PriceLibrary> priceLibraries = iPriceLibraryService.list(queryWrapper);
            if(CollectionUtils.isNotEmpty(priceLibraries)){
                List<PriceLibrary> libraries = ListUtil.listDeduplication(priceLibraries, o -> o.getItemCode() + o.getItemDesc() + o.getVendorCode());
                result = libraries.stream().map(priceLibrary -> priceLibrary.getItemCode() + priceLibrary.getItemDesc() + priceLibrary.getVendorCode()).collect(Collectors.toList());
            }
        }
        return result;
    }

    /**
     * ?????????????????????,??????????????????
     * @param
     * @return
     */
    @GetMapping("/queryItemIdByOuAndInv")
    public List<Long> queryItemIdByOuAndInv(@RequestParam("ouId") Long ouId,@RequestParam("invId") Long invId){
        return priceLibraryMapper.queryItemIdByOuAndInv(ouId,invId);
    }

}
