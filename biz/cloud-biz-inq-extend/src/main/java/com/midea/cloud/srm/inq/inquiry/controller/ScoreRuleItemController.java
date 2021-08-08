package com.midea.cloud.srm.inq.inquiry.controller;

import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.srm.model.common.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import com.midea.cloud.srm.inq.inquiry.service.IScoreRuleItemService;
import com.midea.cloud.srm.model.inq.inquiry.entity.ScoreRuleItem;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RestController;

/**
*  <pre>
 *  招标评分规则表 前端控制器
 * </pre>
*
* @author zhongbh
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-03-12 18:46:55
 *  修改内容:
 * </pre>
*/
@RestController
@RequestMapping("/inquiry/scoreRuleItem")
public class ScoreRuleItemController extends BaseController {

    @Autowired
    private IScoreRuleItemService iScoreRuleItemService;

    /**
    * 获取
    * @param id
    */
    @GetMapping("/get")
    public ScoreRuleItem get(Long id) {
        Assert.notNull(id, "id不能为空");
        return iScoreRuleItemService.getById(id);
    }

    /**
    * 新增
    * @param scoreRuleItem
    */
    @PostMapping("/add")
    public void add(@RequestBody ScoreRuleItem scoreRuleItem) {
        Long id = IdGenrator.generate();
        scoreRuleItem.setScoreRuleId(id);
        iScoreRuleItemService.save(scoreRuleItem);
    }
    
    /**
    * 删除
    * @param id
    */
    @GetMapping("/delete")
    public void delete(Long id) {
        Assert.notNull(id, "id不能为空");
        iScoreRuleItemService.removeById(id);
    }

    /**
    * 修改
    * @param scoreRuleItem
    */
    @PostMapping("/modify")
    public void modify(@RequestBody ScoreRuleItem scoreRuleItem) {
        iScoreRuleItemService.updateById(scoreRuleItem);
    }

    /**
    * 分页查询
    * @param scoreRuleItem
    * @return
    */
    @PostMapping("/listPage")
    public PageInfo<ScoreRuleItem> listPage(@RequestBody ScoreRuleItem scoreRuleItem) {
        PageUtil.startPage(scoreRuleItem.getPageNum(), scoreRuleItem.getPageSize());
        QueryWrapper<ScoreRuleItem> wrapper = new QueryWrapper<ScoreRuleItem>(scoreRuleItem);
        return new PageInfo<ScoreRuleItem>(iScoreRuleItemService.list(wrapper));
    }

    /**
    * 查询所有
    * @return
    */
    @PostMapping("/listAll")
    public List<ScoreRuleItem> listAll() { 
        return iScoreRuleItemService.list();
    }
 
}
