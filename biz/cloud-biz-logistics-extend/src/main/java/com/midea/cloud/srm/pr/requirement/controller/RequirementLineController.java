package com.midea.cloud.srm.pr.requirement.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.handler.TitleColorSheetWriteHandler;
import com.midea.cloud.common.result.BaseResult;
import com.midea.cloud.common.utils.*;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.inq.InqClient;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.RequirementLineUpdateDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementHead;
import com.midea.cloud.srm.model.pm.pr.requirement.vo.RequirementLineVO;
import com.midea.cloud.srm.model.pm.ps.http.FSSCResult;
import com.midea.cloud.srm.model.pm.ps.http.BgtCheckReqParamDto;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.RequirementLineImportDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.dto.RequirermentLineQueryDTO;
import com.midea.cloud.srm.model.pm.pr.requirement.entity.RequirementLine;
import com.midea.cloud.srm.model.pm.pr.requirement.param.FollowNameParam;
import com.midea.cloud.srm.model.pm.pr.requirement.param.OrderQuantityParam;
import com.midea.cloud.srm.model.pm.pr.requirement.param.SourceBusinessGenParam;
import com.midea.cloud.srm.model.pm.pr.requirement.vo.RecommendVendorVO;
import com.midea.cloud.srm.model.pm.pr.requirement.vo.VendorAndEffectivePriceVO;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.suppliercooperate.order.entry.OrderDetail;
import com.midea.cloud.srm.pr.requirement.service.IRequirementLineService;
import com.midea.cloud.srm.ps.http.fssc.service.IFSSCReqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 *  ?????????????????? ???????????????
 * </pre>
 *
 * @author fengdc3@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-05-12 18:46:40
 *  ????????????:
 * </pre>
 */
@RestController
@Slf4j
@RequestMapping("/pr/requirementLine")
public class RequirementLineController extends BaseController {
    @Autowired
    private IRequirementLineService iRequirementLineService;
    @Autowired
    private IFSSCReqService ifsscReqService;

    /**
     * ??????
     *
     * @param id
     */
    @GetMapping("/get")
    public RequirementLine get(@RequestParam("id") Long id) {
        Assert.notNull(id, "id????????????");
        return iRequirementLineService.getById(id);
    }

    /**
     * ??????
     *
     * @param requirementLine
     */
    @PostMapping("/add")
    public void add(@RequestBody RequirementLine requirementLine) {
        Long id = IdGenrator.generate();
        requirementLine.setRequirementLineId(id);
        iRequirementLineService.save(requirementLine);
    }

    /**
     * ????????????
     *
     * @param requirementLineIds
     */
    @PostMapping("/deleteBatch")
    public void delete(@RequestBody List<Long> requirementLineIds) {
        if (!CollectionUtils.isEmpty(requirementLineIds)) {
            iRequirementLineService.removeByIds(requirementLineIds);
        }
    }

    /**
     * ????????????
     *
     * @param id
     */
    @GetMapping("/delete")
    public void delete(Long id) {
        Assert.notNull(id, "id????????????");
        iRequirementLineService.removeById(id);
    }

    /**
     * ??????
     *
     * @param requirementLine
     */
    @PostMapping("/modify")
    public void modify(@RequestBody RequirementLine requirementLine) {
        iRequirementLineService.updateById(requirementLine);
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param followNameParam
     */
    @PostMapping("/updateIfExistRequirementLine")
    public void updateIfExistRequirementLine(@RequestBody FollowNameParam followNameParam) {
        iRequirementLineService.updateIfExistRequirementLine(followNameParam);
    }

    /**
     * ???????????????:???????????????,????????????
     *
     * @param requirementLine
     */
    @PostMapping("/getVendorAndEffectivePrice")
    public VendorAndEffectivePriceVO getVendorAndEffectivePrice(@RequestBody RequirementLine requirementLine) {
        return iRequirementLineService.getVendorAndEffectivePrice(requirementLine);
    }

    /**
     * ????????????
     *
     * @param requirementLine
     * @return
     */
    @PostMapping("/listPage")
    public PageInfo<RequirementLine> listPage(@RequestBody RequirementLine requirementLine) {
        PageUtil.startPage(requirementLine.getPageNum(), requirementLine.getPageSize());
        QueryWrapper<RequirementLine> wrapper = new QueryWrapper<RequirementLine>(requirementLine);
        return new PageInfo<RequirementLine>(iRequirementLineService.list(wrapper));
    }

    /**
     * ??????????????????-??????????????????
     * @param params
     * @return
     */
    @PostMapping("/listPageForOrder")
    public PageInfo<RequirementLineVO> listPage(@RequestBody RequirermentLineQueryDTO params){
        return iRequirementLineService.listPageForOrder(params);
    }

    /** "
     * ????????????
     *
     * @return
     */
    @PostMapping("/listAll")
    public List<RequirementLine> listAll() {
        return iRequirementLineService.list();
    }
//
//    ???????????????
//    /**
//     * excel??????
//     */
//    @GetMapping("/excelExport")
//    public void excelEportRequirementLine(Long requirementHeadId, HttpServletResponse response) throws IOException {
//        List<RequirementLineImportDTO> dataList = new ArrayList<>();
//        if (requirementHeadId != null) {
//            //?????????????????????????????????
//            List<RequirementLine> requirementLineList =
//                    iRequirementLineService.list(new QueryWrapper<>(new RequirementLine().setRequirementHeadId(requirementHeadId)));
//            dataList = BeanCopyUtil.copyListProperties(requirementLineList, RequirementLineImportDTO.class);
//            Map<Integer, Date> requirementDateMap = requirementLineList.stream().collect(Collectors.toMap(RequirementLine::getRowNum,
//                    line -> DateChangeUtil.asDate(line.getRequirementDate())));
//            dataList.forEach(data -> {
//                Date requirementDate = requirementDateMap.get(data.getRowNum());
//                data.setRequirementDate(requirementDate);
//            });
//        }
//
//        String fileName = "??????????????????.xls";
//        if (requirementHeadId != null) {
//            File file = new File(fileName);
//            //??????????????????????????????
//            List<Integer> rowIndexs = Arrays.asList(0);
//            List<Integer> columnIndexs = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
//            Short colorIndex = IndexedColors.RED.getIndex();
//            TitleColorSheetWriteHandler handler = new TitleColorSheetWriteHandler(rowIndexs, columnIndexs, colorIndex);
//            EasyExcel.write(file, RequirementLineImportDTO.class).
//                    registerWriteHandler(handler).
//                    sheet().doWrite(dataList);
//
//            try {
//                byte[] buffer = FileUtils.readFileToByteArray(file);
//                file.delete();
//                response.setContentType("application/vnd.ms-excel");
//                response.setCharacterEncoding("utf-8");
//                fileName = URLEncoder.encode(fileName, "UTF-8");
//                response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
//                response.getOutputStream().write((byte[]) buffer);
//                response.getOutputStream().close();
//            } catch (IOException e) {
//                log.error("???????????????:" + e.getMessage());
//                throw new BaseException(LocaleHandler.getLocaleMsg("????????????,????????????????????????"));
//            }
//        }else {
//            // ????????????
//            String fileModelName = "????????????????????????";
//            // ????????????
//            List<String> titleList = Arrays.asList("????????????", "*????????????", "*????????????", "*??????", "*????????????",
//                    "*????????????", "*????????????", "*????????????", "*????????????", "*????????????", "*????????????", "????????????");
//            // ?????????
//            ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileModelName);
//            // ?????????????????????
//            List<Integer> rowIndexs = Arrays.asList(0);
//            // ?????????????????????
//            List<Integer> columnIndexs = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
//            // ??????
//            short red = IndexedColors.RED.index;
//            TitleColorSheetWriteHandler titleColorSheetWriteHandler = new TitleColorSheetWriteHandler(rowIndexs,columnIndexs,red);
//            EasyExcelUtil.writeExcelWithModel(outputStream, dataList, titleList, fileModelName, titleColorSheetWriteHandler);
//        }
//    }

    /**
     * excel?????????????????????
     * ???????????????
     */
//    @PostMapping("/excelImport")
//    public List<RequirementLine> excelImport(@RequestParam("file") MultipartFile file) throws IOException {
//        Assert.notNull(file, "??????????????????");
//        String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
//        Assert.isTrue((org.apache.commons.lang3.StringUtils.equals("xls", suffix.toLowerCase()) || org.apache.commons.lang3.StringUtils.equals("xlsx", suffix.toLowerCase())),
//                "?????????excel??????");
//        List<Object> voList = EasyExcelUtil.readExcelWithModel(file.getInputStream(),
//                RequirementLineImportDTO.class);
//        return iRequirementLineService.importExcelInfo(voList);
//    }




    /**
     * ??????/?????????-????????????
     * @param requirementLine
     * @return
     */
    @PostMapping("/listApprovedApplyByPage")
    public PageInfo<RequirementLine> listApprovedApplyByPage(@RequestBody RequirementLine requirementLine) {
        return iRequirementLineService.listApprovedApplyByPage(requirementLine);
    }

    /**
     * ??????????????????-?????????????????????
     * @param requirementLineList
     * @return
     */
    @PostMapping("/listRecommendVendor")
    public List<RecommendVendorVO> listRecommendVendor(@RequestBody List<RequirementLine> requirementLineList) {
        return iRequirementLineService.listRecommendVendor(requirementLineList);
    }

    /**
     * ??????????????????
     * @param recommendVendorVOList
     * @return
     */
    @PostMapping("/genOrder")
    public void  genOrder(@RequestBody List<RecommendVendorVO> recommendVendorVOList) {
        iRequirementLineService.genOrder(recommendVendorVOList);
    }

    /**
     * ??????????????????
     * @param param
     * @return
     */
    @PostMapping("/genSourceBusiness")
    public void genSourceBusiness(@RequestBody SourceBusinessGenParam param) {
        iRequirementLineService.genSourceBusiness(param);
    }

    /**
     * ???????????????????????????
     * @param orderQuantityParamList
     * @return
     */
    @PostMapping("/updateOrderQuantityBatch")
    public void updateOrderQuantityBatch(@RequestBody List<OrderQuantityParam> orderQuantityParamList) {
        iRequirementLineService.updateOrderQuantityBatch(orderQuantityParamList);
    }

    /**
     * ?????????
     * @param detailList
     */
    @PostMapping("/orderReturn")
    public void orderReturn(@RequestBody List<OrderDetail> detailList){
        iRequirementLineService.orderReturn(detailList);
    }

    /**
     * ????????????(ceea)
     * @param requirementLineIds
     */
    @PostMapping("/resubmit")
    public void resubmit(@RequestBody List<Long> requirementLineIds) {
        iRequirementLineService.resubmit(requirementLineIds);
    }

    /**
     * ??????
     */
    @PostMapping("/cancel")
    public BaseResult cancel(@RequestBody RequirermentLineQueryDTO requirermentLineQueryDTO) {
        FSSCResult fsscResult = iRequirementLineService.cancel(requirermentLineQueryDTO);
        BaseResult baseResult = BaseResult.buildSuccess();
        if (StringUtils.isNotEmpty(fsscResult.getMsg()) && !"success".equals(fsscResult.getMsg())) {
            baseResult.setCode(fsscResult.getCode());
            baseResult.setMessage(fsscResult.getMsg());
        }
        return baseResult;
    }

    /**
     * ????????????
     * @param requirementLineUpdateDTO
     * @return
     */
    @PostMapping("/updateNum")
    public void ceeaUpdateNum(@RequestBody RequirementLineUpdateDTO requirementLineUpdateDTO){
        iRequirementLineService.ceeaUpdateNum(requirementLineUpdateDTO);
    }

    @GetMapping("/testToken")
    public String getToken() {
        String token = ifsscReqService.getToken();
        return token;
    }

    @PostMapping("/testApplyRelease")
    public BaseResult testApplyRelease(@RequestBody BgtCheckReqParamDto bgtCheckReqParamDto) {
        FSSCResult fsscResult = ifsscReqService.applyRelease(bgtCheckReqParamDto);
        return BaseResult.build(fsscResult.getCode(), fsscResult.getMsg());
    }

    @PostMapping("/testApplyFreeze")
    public BaseResult testApplyFreeze(@RequestBody BgtCheckReqParamDto bgtCheckReqParamDto) {
        FSSCResult fsscResult = ifsscReqService.applyFreeze(bgtCheckReqParamDto);
        return BaseResult.build(fsscResult.getCode(), fsscResult.getMsg());
    }

    /**
     * ????????????-????????????-????????????????????????
     * @param response
     * @throws IOException
     */
    @RequestMapping("/importMaterialItemModelDownload")
    public void importModelDownload(HttpServletResponse response) throws Exception {
        iRequirementLineService.importMaterialItemModelDownload(response);
    }


    /**
     * excel?????????????????????
     */
    @PostMapping("/excelImport")
    public Map<String,Object> excelImport( String requirementHeadId , @RequestParam("file") MultipartFile file , Fileupload fileupload) throws Exception {
        Assert.notNull(file, "??????????????????");
        return iRequirementLineService.importExcel(requirementHeadId, file , fileupload );
    }

}
