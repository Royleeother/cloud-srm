package com.midea.cloud.srm.ps.payment.controller;

import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.pm.ps.payment.entity.CeeaPaymentApplyLine;
import com.midea.cloud.srm.ps.payment.service.ICeeaPaymentApplyLineService;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RestController;

/**
*  <pre>
 *  付款申请-行表（隆基新增） 前端控制器
 * </pre>
*
* @author xiexh12@midea.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-08-25 21:12:58
 *  修改内容:
 * </pre>
*/
@RestController
@RequestMapping("/payment/paymentApplyLine")
public class CeeaPaymentApplyLineController extends BaseController {
    @Autowired
    private ICeeaPaymentApplyLineService iCeeaPaymentApplyLineService;
 
}
