package com.midea.cloud.srm.supcooperate.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.midea.cloud.srm.model.suppliercooperate.deliveryappoint.entry.CarInfo;

import java.util.List;

/**
 * <pre>
 *  车辆信息表 服务类
 * </pre>
 *
 * @author huangbf3
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/4/18 11:23
 *  修改内容:
 * </pre>
 */
public interface ICarInfoService extends IService<CarInfo> {

    void submitBatch(List<Long> ids);

    List<CarInfo> findList(CarInfo carInfo);
}
