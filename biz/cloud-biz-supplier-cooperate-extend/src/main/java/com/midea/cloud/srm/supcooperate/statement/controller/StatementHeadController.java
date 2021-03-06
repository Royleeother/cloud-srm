package com.midea.cloud.srm.supcooperate.statement.controller;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.UserType;
import com.midea.cloud.common.enums.pm.ps.StatementStatusEnum;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.AppUserUtil;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.model.common.BaseController;
import com.midea.cloud.srm.model.pm.ps.statement.dto.PaymentPlanLineQueryDTO;
import com.midea.cloud.srm.model.pm.ps.statement.dto.StatementDTO;
import com.midea.cloud.srm.model.pm.ps.statement.dto.StatementDTO.InsertGroup;
import com.midea.cloud.srm.model.pm.ps.statement.dto.StatementDTO.SubmitGroup;
import com.midea.cloud.srm.model.pm.ps.statement.dto.StatementDTO.UpdateGroup;
import com.midea.cloud.srm.model.pm.ps.statement.entity.StatementHead;
import com.midea.cloud.srm.model.pm.ps.statement.entity.StatementReceipt;
import com.midea.cloud.srm.model.pm.ps.statement.entity.StatementReturn;
import com.midea.cloud.srm.model.rbac.user.entity.LoginAppUser;
import com.midea.cloud.srm.supcooperate.statement.service.IStatementHeadService;
import com.midea.cloud.srm.supcooperate.statement.service.IStatementReceiptService;
import com.midea.cloud.srm.supcooperate.statement.service.IStatementReturnService;

import lombok.Data;

/**
 *
 * <pre>
 * ?????????
 * </pre>
 *
 * @author zhizhao1.fan@meicloud.com
 * @version 1.00.00
 *
 *          <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: Jun 22, 20205:46:01 PM
 *  ????????????:
 *          </pre>
 */
@RestController
@RequestMapping("ps/statementHead")
public class StatementHeadController extends BaseController {

	@Autowired
	private IStatementHeadService iStatementHeadService;

	@Autowired
	private IStatementReturnService iStatementReturnService;

	@Autowired
	private IStatementReceiptService iStatementReceiptService;

	/**
	 * ??????ID???????????????
	 *
	 * @param statementHeadId
	 * @return
	 */
	@GetMapping("/getStatementHeadById")
	public StatementHead getStatementHeadById(@RequestParam("statementHeadId") Long statementHeadId) {
		return iStatementHeadService.getById(statementHeadId);
	}

	/**
	 * ??????ID?????????????????????
	 *
	 * @param statementHeadIds
	 * @return
	 */
	@PostMapping("/getStatementHeadByIds")
	public List<StatementHead> getStatementHeadByIds(@RequestBody List<Long> statementHeadIds) {
		return iStatementHeadService.listByIds(statementHeadIds);
	}

	/**
	 * ??????????????????ID???????????????????????????
	 *
	 * @param statementHeadId
	 * @return
	 */
	@GetMapping("/getStatementReceiptByStatementHeadId")
	public List<StatementReceipt> getStatementReceiptByStatementHeadId(@RequestParam("statementHeadId") Long statementHeadId) {
		return iStatementReceiptService.list(new QueryWrapper<StatementReceipt>(new StatementReceipt().setStatementHeadId(statementHeadId)));
	}

	/**
	 * ??????????????????ID???????????????????????????
	 *
	 * @param statementHeadId
	 * @return
	 */
	@GetMapping("/getStatementReturnByStatementHeadId")
	public List<StatementReturn> getStatementReturnByStatementHeadId(@RequestParam("statementHeadId") Long statementHeadId) {
		return iStatementReturnService.list(new QueryWrapper<StatementReturn>(new StatementReturn().setStatementHeadId(statementHeadId)));
	}

	/**
	 * ???????????????????????????
	 *
	 * @param statementHeadList
	 */
	@PostMapping("/saveOrUpdateBatchStatementHead")
	public void saveOrUpdateBatchStatementHead(@RequestBody List<StatementHead> statementHeadList) {
		iStatementHeadService.saveOrUpdateBatch(statementHeadList);
	}

	/**
	 * ?????????????????????????????????
	 *
	 * @param paymentPlanLineQueryDTO
	 * @return
	 */
	@PostMapping("/getStatementHeadByPaymentPlanLineQueryDTO")
	public PageInfo<StatementHead> getStatementHeadByPaymentPlanLineQueryDTO(@RequestBody PaymentPlanLineQueryDTO paymentPlanLineQueryDTO) {
		PageUtil.startPage(paymentPlanLineQueryDTO.getPageNum(), paymentPlanLineQueryDTO.getPageSize());
		QueryWrapper<StatementHead> queryWrapper = new QueryWrapper<StatementHead>(new StatementHead().setStatementStatus(StatementStatusEnum.PASS.getValue()).setVendorId(paymentPlanLineQueryDTO.getVendorId()));
		queryWrapper.like(StringUtils.isNotBlank(paymentPlanLineQueryDTO.getSourceNumber()), "STATEMENT_NUMBER", paymentPlanLineQueryDTO.getSourceNumber());
		queryWrapper.ne("PAID_FINISHED", "Y").or().isNull("PAID_FINISHED");
		return new PageInfo<StatementHead>(iStatementHeadService.list(queryWrapper));
	}

	/**
	 * ???????????????
	 *
	 * @param statementDTO
	 * @return
	 */
	@PostMapping("/saveStatement")
	public void saveStatement(@Validated(value = { InsertGroup.class }) @RequestBody StatementDTO statementDTO, BindingResult bindingResult) {
		if (bindingResult.getAllErrors() != null && bindingResult.getAllErrors().size() > 0) {
			throw new BaseException(bindingResult.getAllErrors().get(bindingResult.getAllErrors().size() - 1).getDefaultMessage());
		}
		iStatementHeadService.saveOrUpdateStatement(statementDTO);
	}

	/**
	 * ???????????????
	 *
	 * @param statementDTO
	 * @return
	 */
	@PostMapping("/updateStatement")
	public void updateStatement(@Validated(value = { UpdateGroup.class }) @RequestBody StatementDTO statementDTO, BindingResult bindingResult) {
		if (bindingResult.getAllErrors() != null && bindingResult.getAllErrors().size() > 0) {
			throw new BaseException(bindingResult.getAllErrors().get(bindingResult.getAllErrors().size() - 1).getDefaultMessage());
		}
		iStatementHeadService.saveOrUpdateStatement(statementDTO);
	}

	/**
	 * ???????????????
	 *
	 * @param statementDTO
	 * @return
	 */
	@PostMapping("/submitStatement")
	public void submitStatement(@Validated(value = { SubmitGroup.class }) @RequestBody StatementDTO statementDTO, BindingResult bindingResult) {
		if (bindingResult.getAllErrors() != null && bindingResult.getAllErrors().size() > 0) {
			throw new BaseException(bindingResult.getAllErrors().get(bindingResult.getAllErrors().size() - 1).getDefaultMessage());
		}
		iStatementHeadService.saveOrUpdateStatement(statementDTO);
	}

	/**
	 * ???????????????
	 *
	 * @param statementHeadIds
	 */
	@PostMapping("/deleteStatement")
	public void deleteStatement(@RequestBody List<Long> statementHeadIds) {
		iStatementHeadService.deleteStatement(statementHeadIds);
	}

	/**
	 * ???????????????ID????????????
	 *
	 * @param statementHeadId
	 * @return
	 */
	@GetMapping("/getStatementById")
	public StatementDTO getStatementById(@RequestParam(value = "statementHeadId", required = true) Long statementHeadId) {
		return iStatementHeadService.getStatementById(statementHeadId);
	}

	/**
	 * ???????????????
	 *
	 * @param statementHeadId
	 */
	@GetMapping("/cancelStatement")
	public void cancelStatement(@RequestParam(value = "statementHeadId", required = true) Long statementHeadId) {
		iStatementHeadService.cancelStatement(statementHeadId);
	}

	@Data
	public static class PassStatementDTO {
		@NotNull(message = "id????????????")
		private Long statementHeadId;
		private String supplierNote;
	}

	/**
	 * ??????????????????????????????
	 *
	 * @param statementHeadId
	 */
	@PostMapping("/passStatement")
	public void passStatement(@RequestBody @Validated @NotNull PassStatementDTO passStatementDTO, BindingResult bindingResult) {
		if (bindingResult.getAllErrors() != null && bindingResult.getAllErrors().size() > 0) {
			throw new BaseException(bindingResult.getAllErrors().get(bindingResult.getAllErrors().size() - 1).getDefaultMessage());
		}
		iStatementHeadService.passStatement(passStatementDTO);
	}

	@Data
	private static class RejectStatementDTO {
		@NotNull(message = "id????????????")
		private Long statementHeadId;
		@NotNull(message = "????????????????????????")
		private String rejectReason;
	}

	/**
	 * ??????????????????????????????
	 *
	 * @param statementHead
	 */
	@PostMapping("/rejectStatement")
	public void rejectStatement(@RequestBody @Validated @NotNull RejectStatementDTO rejectStatementDTO, BindingResult bindingResult) {
		if (bindingResult.getAllErrors() != null && bindingResult.getAllErrors().size() > 0) {
			throw new BaseException(bindingResult.getAllErrors().get(bindingResult.getAllErrors().size() - 1).getDefaultMessage());
		}
		StatementHead checkHead = iStatementHeadService.getById(rejectStatementDTO.getStatementHeadId());
		Assert.notNull(checkHead, "??????????????????");
		Assert.isTrue(StringUtils.equals(StatementStatusEnum.SUBMITTED.getValue(), checkHead.getStatementStatus()), "??????????????????????????????");
		checkHead.setRejectReason(rejectStatementDTO.getRejectReason()).setStatementStatus(StatementStatusEnum.REJECTED.getValue());
		iStatementHeadService.updateById(checkHead);
	}

	/**
	 * ????????????
	 *
	 * @param statementHead
	 * @return
	 */
	@PostMapping("/listStatementHeadPage")
	public PageInfo<StatementHead> listStatementHeadPage(@RequestBody StatementHead statementHead) {
		LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
		if (!StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType()) && !StringUtils.equals(UserType.BUYER.name(), loginAppUser.getUserType())) {
			Assert.isTrue(false, "?????????????????????");
		}
		StatementHead head = new StatementHead();
		BeanUtils.copyProperties(statementHead, head);
		QueryWrapper<StatementHead> queryWrapper = new QueryWrapper<StatementHead>(head);
		if (StringUtils.equals(UserType.VENDOR.name(), loginAppUser.getUserType())) {
			head.setVendorId(loginAppUser.getCompanyId());
			queryWrapper.ne("STATEMENT_STATUS", StatementStatusEnum.CREATE.getValue());
			queryWrapper.isNotNull("SUBMIT_TIME");
		}
		queryWrapper.like(StringUtils.isNotBlank(statementHead.getVendorCode()), "VENDOR_CODE", statementHead.getVendorCode());
		queryWrapper.like(StringUtils.isNotBlank(statementHead.getVendorName()), "VENDOR_NAME", statementHead.getVendorName());
		queryWrapper.like(StringUtils.isNotBlank(statementHead.getStatementNumber()), "STATEMENT_NUMBER", statementHead.getStatementNumber());
		queryWrapper.like(StringUtils.isNotBlank(statementHead.getOrganizationName()), "ORGANIZATION_NAME", statementHead.getOrganizationName());
		queryWrapper.ge(statementHead.getStatementStartTime() != null, "STATEMENT_START_TIME", statementHead.getStatementStartTime());
		queryWrapper.le(statementHead.getStatementEndTime() != null, "STATEMENT_END_TIME", statementHead.getStatementEndTime());
		queryWrapper.orderByDesc("STATEMENT_HEAD_ID");
		PageUtil.startPage(statementHead.getPageNum(), statementHead.getPageSize());
		return new PageInfo<StatementHead>(iStatementHeadService.list(queryWrapper));
	}

}
