package com.midea.cloud.srm.bargaining.suppliercooperate.projectlist.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.bargaining.projectmanagement.signupmanagement.SignUpStatus;
import com.midea.cloud.common.enums.bargaining.projectmanagement.signupmanagement.VendorFileType;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.BeanCopyUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidFileConfigService;
import com.midea.cloud.srm.bargaining.purchaser.projectmanagement.bidinitiating.service.IBidVendorService;
import com.midea.cloud.srm.bargaining.suppliercooperate.projectlist.service.IBidSignUpService;
import com.midea.cloud.srm.bargaining.suppliercooperate.projectlist.service.IBidVendorFileService;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidFileConfig;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.bidinitiating.entity.BidVendor;
import com.midea.cloud.srm.model.bargaining.purchaser.projectmanagement.businessproposal.entity.SignUp;
import com.midea.cloud.srm.model.bargaining.suppliercooperate.vo.BidSignUpVO;
import com.midea.cloud.srm.model.bargaining.suppliercooperate.vo.BidVendorFileVO;
import com.midea.cloud.srm.model.base.clarify.dto.SourcingVendorInfo;
import com.midea.cloud.srm.model.base.clarify.enums.ClarifySourcingType;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
*  <pre>
 *  ???????????????????????? ???????????????
 * </pre>
*
* @author zhuomb1@midea.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:tanjl11@meicloud.com
 *  ????????????: 2020-09-11 13:54:22
 *  ????????????:
 * </pre>
*/
@RestController
@RequestMapping("/supplierCooperate/bidSingUp")
@Slf4j
public class BidSignUpController extends BaseController  {

    @Autowired
    private IBidSignUpService iSignUpService;
    @Autowired
    private IBidFileConfigService bidFileConfigService;
    @Autowired
    private IBidVendorFileService iVendorFileService;
    @Resource
    private IBidVendorService iBidVendorService;
    @Autowired
    private RbacClient rbacClient;

    /**
    * ??????
    * @param id
    */
    @GetMapping("/get")
    public SignUp get(Long id) {
        Assert.notNull(id, "id????????????");
        return iSignUpService.getById(id);
    }


    /**
     * ?????? -- ??????
     * @param bidSignUpVO
     */
    @PostMapping("/saveSignUpInfo")
    public Long saveSignUpInfo(@RequestBody BidSignUpVO bidSignUpVO) {
        //???????????????ID
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        bidSignUpVO.setVendorId(user.getCompanyId());
        iSignUpService.judgeSignUpCondition(bidSignUpVO);
        bidSignUpVO.setFileType(VendorFileType.ENROLL.getValue());
        bidSignUpVO.setSignUpStatus(SignUpStatus.NO_SIGNUP.getValue());
        Long signUpId = iSignUpService.saveSignUpInfo(bidSignUpVO);
        return signUpId;
    }
    /**
     * ?????? -- ??????
     * @param bidSignUpVO
     */
    @PostMapping("/signUp")
    public String signUp(@RequestBody BidSignUpVO bidSignUpVO) {
        //?????????
        Long signUpId = saveSignUpInfo(bidSignUpVO);
        SignUp signUp = iSignUpService.getById(signUpId);
        signUp.setSignUpStatus(SignUpStatus.SIGNUPED.getValue());
        signUp.setReplyDatetime(new Date());
        iSignUpService.updateById(signUp);

        // ???????????????????????????
        Long bidingId = bidSignUpVO.getBidingId();
        UpdateWrapper<BidVendor> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("JOIN_FLAG","Y");
        updateWrapper.eq("BIDING_ID",signUp.getBidingId());
        updateWrapper.eq("VENDOR_ID",signUp.getVendorId());
        iBidVendorService.update(updateWrapper);

        return "????????????";
    }


    /**
     * ??????????????????????????????????????????????????????????????????
     * ????????????bidingId???vendorId
     *
     * @param bidSignUpVO
     */
    @PostMapping("/getSignUpInfo")
    public BidSignUpVO getSignUpInfo(@RequestBody BidSignUpVO bidSignUpVO) {
        iSignUpService.judgeSignUpCondition(bidSignUpVO);
        return signUpInfo(bidSignUpVO);
    }
    /**
     * ??????????????????????????????????????????????????????????????????
     * ????????????bidingId???vendorId
     *
     * @param bidSignUpVO
     */

    @PostMapping("/getSignUpDetail")
    public BidSignUpVO signUpInfo(@RequestBody BidSignUpVO bidSignUpVO) {
        //???????????????ID
        LoginAppUser user = AppUserUtil.getLoginAppUser();
        bidSignUpVO.setVendorId(user.getCompanyId());
        //??????????????????
        bidSignUpVO.setFileType(VendorFileType.ENROLL.getValue());
        //1 ??????????????????
        bidSignUpVO = iSignUpService.getBidSignUpVO(bidSignUpVO);
        //2 ??????????????????
        BidVendorFileVO bidVendorFileVO = new BidVendorFileVO();
        Long bidingId = bidSignUpVO.getBidingId();
        bidVendorFileVO.setBidingId(bidingId);
        bidVendorFileVO.setVendorId(bidSignUpVO.getVendorId());
        bidVendorFileVO.setBusinessId(bidSignUpVO.getSignUpId());
        bidVendorFileVO.setFileType(VendorFileType.ENROLL.getValue());
        List<BidVendorFileVO> vendorFileVOs = iVendorFileService.getVendorFileList(bidVendorFileVO);
        bidSignUpVO.setVendorFileVOs(vendorFileVOs);
       /* List<BidFileConfig> list = bidFileConfigService.list(Wrappers.lambdaQuery(BidFileConfig.class)
                .eq(BidFileConfig::getBidingId, bidingId)
        );
        bidSignUpVO.setFileConfigs(list);*/
        return bidSignUpVO;
    }

    /**
    * ??????
    * @param bidSignUp
    */
    @PostMapping("/add")
    public void add(@RequestBody SignUp bidSignUp) {
        Long id = IdGenrator.generate();
        bidSignUp.setSignUpId(id);
        iSignUpService.save(bidSignUp);
    }

    /**
    * ??????
    * @param bidSignUp
    */
    @PostMapping("/modify")
    public void modify(@RequestBody SignUp bidSignUp) {
        iSignUpService.updateById(bidSignUp);
    }

    /**
    * ????????????
    * @param bidSignUp
    * @return
    */
    @PostMapping("/listPage")
    public PageInfo<BidFileConfig> listPage(@RequestBody SignUp bidSignUp) {
        PageUtil.startPage(bidSignUp.getPageNum(), bidSignUp.getPageSize());
        QueryWrapper<BidFileConfig> queryWrapper = new QueryWrapper();
        return new PageInfo<BidFileConfig>(bidFileConfigService.list(queryWrapper));
    }

    /**
    * ????????????
    * @return
    */
    @PostMapping("/listAll")
    public List<SignUp> listAll() {
        return iSignUpService.list();
    }

    @GetMapping("/getSourcingVendorInfo")
    public Collection<SourcingVendorInfo> getSourcingVendorInfo(@RequestParam("typeCode") String typeCode,@RequestParam("sourcingId") Long sourcingId) {
        if (Objects.equals(ClarifySourcingType.RFQ.getItemName(), typeCode)) {
            List<SignUp> list = iSignUpService.list(Wrappers.lambdaQuery(SignUp.class)
                    .eq(SignUp::getBidingId, sourcingId));
            Set<Long> vendorIds = list.stream().map(SignUp::getVendorId).collect(Collectors.toSet());
            List<User> vendors = rbacClient.getByUserIds(vendorIds);
            if(CollectionUtils.isEmpty(vendors)){
                return Collections.emptyList();
            }
            return vendors.stream().map(e->{
                SourcingVendorInfo vendorInfo = BeanCopyUtil.copyProperties(e, SourcingVendorInfo::new);
                vendorInfo.setVendorId(e.getUserId());
                vendorInfo.setVendorName(e.getUsername());
                return vendorInfo;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
