package com.midea.cloud.srm.supauth.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.YesOrNo;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.model.supplierauth.review.entity.SiteConfigCate;
import com.midea.cloud.srm.supauth.review.mapper.SiteConfigCateMapper;
import com.midea.cloud.srm.supauth.review.service.ISiteConfigCateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;

/**
*  <pre>
 *  现场评审周期设置(按品类) 服务实现类
 * </pre>
*
* @author chensl26@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-04-29 16:38:22
 *  修改内容:
 * </pre>
*/
@Service
public class SiteConfigCateServiceImpl extends ServiceImpl<SiteConfigCateMapper, SiteConfigCate> implements ISiteConfigCateService {

    @Override
    public void saveOrUpdateSiteConfigCate(SiteConfigCate siteConfigCate) {
        checkBeforeSaveOrUpdate(siteConfigCate);
        if (siteConfigCate.getConfigCateId() == null) {
            long id = IdGenrator.generate();
            siteConfigCate.setConfigCateId(id);
            this.save(siteConfigCate);
        }else {
            this.updateById(siteConfigCate);
        }
    }

    @Override
    public PageInfo<SiteConfigCate> listPageByParm(SiteConfigCate siteConfigCate) {
        PageUtil.startPage(siteConfigCate.getPageNum(), siteConfigCate.getPageSize());
        SiteConfigCate siteConfigCateEntity = new SiteConfigCate();
        if (StringUtils.isNotBlank(siteConfigCate.getSiteCycle())) {
            siteConfigCateEntity.setSiteCycle(siteConfigCate.getSiteCycle());
        }
        QueryWrapper<SiteConfigCate> queryWrapper = new QueryWrapper<>(siteConfigCateEntity);
        if (StringUtils.isNotBlank(siteConfigCate.getEnabled()) &&
                YesOrNo.YES.getValue().equals(siteConfigCate.getEnabled())) {
            queryWrapper.le("START_DATE", LocalDate.now());
            queryWrapper.and(wrapper -> wrapper.gt("END_DATE", LocalDate.now()).or().isNull("END_DATE"));
        }
        if (StringUtils.isNotBlank(siteConfigCate.getEnabled())
                && YesOrNo.NO.getValue().equals(siteConfigCate.getEnabled())) {
            queryWrapper.le("END_DATE", LocalDate.now());
        }
        queryWrapper.like(StringUtils.isNotBlank(siteConfigCate.getCategoryName()), "CATEGORY_NAME", siteConfigCate.getCategoryName());
        queryWrapper.like(StringUtils.isNotBlank(siteConfigCate.getSqePerson()), "SQE_PERSON", siteConfigCate.getSqePerson());
        queryWrapper.orderByDesc("LAST_UPDATE_DATE");
        return new PageInfo<>(this.list(queryWrapper));
    }

    private void checkBeforeSaveOrUpdate(SiteConfigCate siteConfigCate) {
        Assert.hasText(siteConfigCate.getCategoryName(), "品类名称不能为空");
        Assert.hasText(siteConfigCate.getSiteCycle(), "现场评审周期不能为空");
        Assert.hasText(siteConfigCate.getSqePerson(), "SQE/负责人不能为空");
    }
}
