package com.midea.cloud.srm.bid.purchaser.projectmanagement.businessproposal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.midea.cloud.srm.model.logistics.bid.purchaser.projectmanagement.businessproposal.entity.Round;

/**
 * <pre>
 * 招标轮次
 * </pre>
 *
 * @author zhizhao1.fan@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020年3月25日 下午5:15:01
 *  修改内容:
 *          </pre>
 */
public interface IRoundService extends IService<Round> {

    void publicResult(Long bidingId);
}
