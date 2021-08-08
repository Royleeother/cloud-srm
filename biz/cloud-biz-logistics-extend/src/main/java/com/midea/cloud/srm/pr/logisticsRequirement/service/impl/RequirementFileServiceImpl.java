package com.midea.cloud.srm.pr.logisticsRequirement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementFile;
import com.midea.cloud.srm.model.logistics.pr.requirement.entity.LogisticsRequirementHead;
import com.midea.cloud.srm.pr.logisticsRequirement.mapper.LogisticsRequirementFileMapper;
import com.midea.cloud.srm.pr.logisticsRequirement.mapper.LogisticsRequirementFileMapper;
import com.midea.cloud.srm.pr.logisticsRequirement.service.IRequirementFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
*  <pre>
 *  物流采购申请附件 服务实现类
 * </pre>
*
* @author chenwt24@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020-11-27 10:59:47
 *  修改内容:
 * </pre>
*/
@Service(value = "LogisticsRequirementFileServiceImpl")
public class RequirementFileServiceImpl extends ServiceImpl<LogisticsRequirementFileMapper, LogisticsRequirementFile> implements IRequirementFileService {

    @Override
    public void addRequirementFileBatch(LogisticsRequirementHead requirementHead, List<LogisticsRequirementFile> requirementAttaches) {
        if (CollectionUtils.isNotEmpty(requirementAttaches)) {
            for (int i = 0; i < requirementAttaches.size(); i++) {
                LogisticsRequirementFile requirementAttach = requirementAttaches.get(i);
                if (requirementAttach == null) continue;
                saveRequirementAttach(requirementHead, requirementAttach,i+1);
            }
        }
    }

    private void saveRequirementAttach(LogisticsRequirementHead requirementHead, LogisticsRequirementFile requirementAttach,int rowNum) {
        requirementAttach.setRequirementFileId(IdGenrator.generate())
                .setRequirementHeaderId(requirementHead.getRequirementHeadId())
                .setRowNum(rowNum);
        this.save(requirementAttach);
    }

    @Override
    public void updateRequirementAttachBatch(LogisticsRequirementHead requirementHead, List<LogisticsRequirementFile> requirementAttaches) {
        if (CollectionUtils.isNotEmpty(requirementAttaches)) {
            List<LogisticsRequirementFile> oldAttaches = this.list(new QueryWrapper<>(new LogisticsRequirementFile().setRequirementHeaderId(requirementHead.getRequirementHeadId())));
            List<Long> oldAttachIds = oldAttaches.stream().map(LogisticsRequirementFile::getRequirementFileId).collect(Collectors.toList());
            List<Long> newAttachIds = new ArrayList<>();
            for (int i = 0; i < requirementAttaches.size(); i++) {
                LogisticsRequirementFile requirementAttach = requirementAttaches.get(i);
                if (requirementAttach == null) continue;
                Long attachId = requirementAttach.getRequirementFileId();
                //新增
                if (attachId == null) {
                    saveRequirementAttach(requirementHead, requirementAttach, i + 1);
                }else {
                    //修改
                    this.updateById(requirementAttach);
                    newAttachIds.add(attachId);
                }
            }
            //删除
            if (CollectionUtils.isNotEmpty(oldAttachIds)) {
                for (Long oldAttachId : oldAttachIds) {
                    if (!newAttachIds.contains(oldAttachId)) {
                        this.removeById(oldAttachId);
                    }
                }
            }
        }
    }
}
