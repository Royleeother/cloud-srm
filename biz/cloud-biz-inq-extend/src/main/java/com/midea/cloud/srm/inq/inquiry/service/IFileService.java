package com.midea.cloud.srm.inq.inquiry.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.midea.cloud.srm.model.inq.inquiry.entity.File;

import java.util.List;

/**
*  <pre>
 *  询价-附件表 服务类
 * </pre>
*
* @author zhongbh
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-03-12 18:46:54
 *  修改内容:
 * </pre>
*/
public interface IFileService extends IService<File> {

    List<File> getByHeadId(Long inquiryId, String type);
}
