package com.midea.cloud.srm.sup.change.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.srm.model.supplier.change.entity.CategoryRelChange;
import com.midea.cloud.srm.model.supplier.change.entity.OrgCategoryChange;
import com.midea.cloud.srm.model.supplier.info.dto.OrgCategoryQueryDTO;

import java.util.List;

/**
*  <pre>
 *  组织与品类变更 服务类
 * </pre>
*
* @author chensl26@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-03-28 13:59:38
 *  修改内容:
 * </pre>
*/
public interface IOrgCategoryChangeService extends IService<OrgCategoryChange> {

    List<OrgCategoryChange> getByChangeId(Long chanageId);

    void saveOrUpdateOrgCategory(OrgCategoryChange orgCategoryChange, Long companyId, Long changeId);

    void removeByChangeId(Long changeId);

    PageInfo<OrgCategoryChange> listPageChangeByParam(OrgCategoryQueryDTO orgCategoryQueryDTO);

}