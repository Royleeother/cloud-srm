package com.midea.cloud.srm.pr.division.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import com.midea.cloud.common.enums.Enable;
import com.midea.cloud.common.exception.BaseException;
import com.midea.cloud.common.utils.DateUtil;
import com.midea.cloud.common.utils.EasyExcelUtil;
import com.midea.cloud.common.utils.IdGenrator;
import com.midea.cloud.common.utils.PageUtil;
import com.midea.cloud.component.context.i18n.LocaleHandler;
import com.midea.cloud.srm.feign.base.BaseClient;
import com.midea.cloud.srm.feign.rbac.RbacClient;
import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.organization.entity.Organization;
import com.midea.cloud.srm.model.pm.pr.division.dto.DivisionMaterialModelDTO;
import com.midea.cloud.srm.model.pm.pr.division.dto.DivisionMaterialQueryDTO;
import com.midea.cloud.srm.model.pm.pr.division.entity.DivisionCategory;
import com.midea.cloud.srm.model.pm.pr.division.entity.DivisionMaterial;
import com.midea.cloud.srm.model.rbac.user.entity.User;
import com.midea.cloud.srm.pr.division.mapper.DivisionMaterialMapper;
import com.midea.cloud.srm.pr.division.service.IDivisionCategoryService;
import com.midea.cloud.srm.pr.division.service.IDivisionMaterialService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
*  <pre>
 *  ?????????????????????  ???????????????
 * </pre>
*
* @author chensl26@meiCloud.com
* @version 1.00.00
*
*  <pre>
 *  ????????????
 *  ???????????????:
 *  ?????????:
 *  ????????????: 2020-07-22 08:45:14
 *  ????????????:
 * </pre>
*/
@Service
public class DivisionMaterialServiceImpl extends ServiceImpl<DivisionMaterialMapper, DivisionMaterial> implements IDivisionMaterialService {

    @Autowired
    BaseClient baseClient;

    @Autowired
    IDivisionCategoryService iDivisionCategoryService;

    @Autowired
    RbacClient rbacClient;

    @Override
        @Transactional
    public void saveOrUpdateDivisionMaterial(List<DivisionMaterial> divisionMaterials) {
        if (!CollectionUtils.isEmpty(divisionMaterials)) {
            for (DivisionMaterial divisionMaterial : divisionMaterials) {
                if (divisionMaterial == null) continue;
                checkParam(divisionMaterial);
                if (divisionMaterial.getDivisionMaterialId() == null) {
                    saveDivisionMaterial(divisionMaterial);
                } else {
                    if (divisionMaterial.getStartDate() == null) {
                        divisionMaterial.setStartDate(LocalDate.now());
                    }
                    this.updateById(divisionMaterial);
                }
            }
        }
    }

    private void saveDivisionMaterial(DivisionMaterial divisionMaterial) {
        divisionMaterial.setDivisionMaterialId(IdGenrator.generate());
        if (divisionMaterial.getStartDate() == null) {
            divisionMaterial.setStartDate(LocalDate.now());
        }
        try {
            this.save(divisionMaterial);
        } catch (DuplicateKeyException e) {
            e.printStackTrace();
            String message = e.getMessage();
            if (StringUtils.isNotBlank(message) && message.indexOf("ceea_pr_division_material_u1") != -1) {
                throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????????????????????????????????????????????????????????????????"));
            }
        }
    }

    @Override
    public PageInfo<DivisionMaterial> listPageByParam(DivisionMaterialQueryDTO divisionMaterialQueryDTO) {
        PageUtil.startPage(divisionMaterialQueryDTO.getPageNum(), divisionMaterialQueryDTO.getPageSize());
        QueryWrapper<DivisionMaterial> queryWrapper = new QueryWrapper<>(new DivisionMaterial());
        queryWrapper.in(!CollectionUtils.isEmpty(divisionMaterialQueryDTO.getOrgIds()), "ORG_ID", divisionMaterialQueryDTO.getOrgIds());
        queryWrapper.in(!CollectionUtils.isEmpty(divisionMaterialQueryDTO.getOrganizationIds()), "ORGANIZATION_ID", divisionMaterialQueryDTO.getOrganizationIds());
        queryWrapper.in(!StringUtils.isEmpty(divisionMaterialQueryDTO.getMaterialCode()), "MATERIAL_CODE", divisionMaterialQueryDTO.getMaterialCode());
        queryWrapper.like(StringUtils.isNotBlank(divisionMaterialQueryDTO.getSupUserNickname()), "SUP_USER_NICKNAME", divisionMaterialQueryDTO.getSupUserNickname());
        queryWrapper.like(StringUtils.isNotBlank(divisionMaterialQueryDTO.getStrategyUserNickname()), "STRATEGY_USER_NICKNAME", divisionMaterialQueryDTO.getStrategyUserNickname());
        queryWrapper.like(StringUtils.isNotBlank(divisionMaterialQueryDTO.getPerformUserNickname()), "PERFORM_USER_NICKNAME", divisionMaterialQueryDTO.getPerformUserNickname());
        if (StringUtils.isNotBlank(divisionMaterialQueryDTO.getEnable()) && Enable.Y.name().equals(divisionMaterialQueryDTO.getEnable())) {
            queryWrapper.le("START_DATE", LocalDate.now());
            queryWrapper.gt("END_DATE", LocalDate.now()).or().isNull("END_DATE");
        }
        else if (StringUtils.isNotBlank(divisionMaterialQueryDTO.getEnable()) && Enable.N.name().equals(divisionMaterialQueryDTO.getEnable())) {
            queryWrapper.le("END_DATE", LocalDate.now());
        }
        return new PageInfo<>(this.list(queryWrapper));
    }

    @Override
    public void importModelDownload(HttpServletResponse response) throws IOException {
        String fileName = "??????????????????????????????";
        List<DivisionMaterialModelDTO> divisionMaterialModelDTOS = new ArrayList<>();
        ServletOutputStream outputStream = EasyExcelUtil.getServletOutputStream(response, fileName);
        EasyExcelUtil.writeExcelWithModel(outputStream, fileName, divisionMaterialModelDTOS,DivisionMaterialModelDTO.class);
    }

    @Override
    @Transactional
    public void importExcel(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        if (!EasyExcelUtil.isExcel(originalFilename)) {
            throw new RuntimeException("??????????????????Excel??????");
        }

        InputStream inputStream = file.getInputStream();
        List<DivisionMaterialModelDTO> divisionMaterialModelDTOS = new ArrayList<>();

        //??????????????????
        List<Object> objects = EasyExcelUtil.readExcelWithModel(inputStream, DivisionMaterialModelDTO.class);
        if (!CollectionUtils.isEmpty(objects)) {
            for (int i = 0; i < objects.size(); i++) {
                Object object = objects.get(i);
                if (object == null) continue;
                DivisionMaterialModelDTO divisionMaterialModelDTO = (DivisionMaterialModelDTO) object;
                divisionMaterialModelDTO.setRow(i + 2);
                DivisionMaterial divisionMaterial = new DivisionMaterial();
                //????????????
                checkParamBeforeImportExcel(divisionMaterialModelDTO, divisionMaterial);
                //????????????
                saveDivisionMaterial(divisionMaterial);
            }

        }else {
            throw new BaseException(LocaleHandler.getLocaleMsg("??????excel????????????"));
        }
    }

    private void checkParamBeforeImportExcel(DivisionMaterialModelDTO divisionMaterialModelDTO, DivisionMaterial divisionMaterial) {
        //??????????????????
        String startDate = divisionMaterialModelDTO.getStartDate();
        String endDate = divisionMaterialModelDTO.getEndDate();
        int row = divisionMaterialModelDTO.getRow();
        LocalDate parseStartDate = null;
        LocalDate parseEndDate = null;
        if (StringUtils.isNotBlank(startDate)) {
            try {
                Date date = DateUtil.parseDate(startDate);
                parseStartDate = DateUtil.dateToLocalDate(date);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,?????????!", "" + row));
            }
        }
        if (StringUtils.isNotBlank(endDate)) {
            try {
                Date date = DateUtil.parseDate(endDate);
                parseEndDate = DateUtil.dateToLocalDate(date);
            } catch (ParseException e) {
                e.printStackTrace();
                throw new BaseException(LocaleHandler.getLocaleMsg("??????????????????????????????,?????????!", "" + row));
            }
        }

        //?????????????????????
        if (StringUtils.isBlank(divisionMaterialModelDTO.getOrgName())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????", "" + row));
        }
        if (StringUtils.isBlank(divisionMaterialModelDTO.getOrganizationName())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????", "" + row));
        }
        if (StringUtils.isBlank(divisionMaterialModelDTO.getMaterialCode())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????", "" + row));
        }
        if (StringUtils.isBlank(divisionMaterialModelDTO.getMaterialName())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????", "" + row));
        }
        if (StringUtils.isBlank(divisionMaterialModelDTO.getSupUserNickname())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("???????????????????????????", "" + row));
        }
        if (StringUtils.isBlank(divisionMaterialModelDTO.getStrategyUserNickname())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????", "" + row));
        }
        if (StringUtils.isBlank(divisionMaterialModelDTO.getPerformUserNickname())) {
            throw new BaseException(LocaleHandler.getLocaleMsg("????????????????????????", "" + row));
        }
        //??????????????????????????????
        Organization org = baseClient.getOrganizationByParam(new Organization().setOrganizationName(divisionMaterialModelDTO.getOrgName()));
        Assert.notNull(org, LocaleHandler.getLocaleMsg("??????????????????????????????", "" + row));

        //??????????????????????????????
        Organization organization = baseClient.getOrganizationByParam(new Organization().setOrganizationName(divisionMaterialModelDTO.getOrganizationName()));
        Assert.notNull(organization, LocaleHandler.getLocaleMsg("??????????????????????????????", "" + row));

        //??????????????????????????????
        List<MaterialItem> materialItems = baseClient.listMaterialByParam(new MaterialItem().setMaterialName(divisionMaterialModelDTO.getMaterialName()));
        Assert.notEmpty(materialItems, LocaleHandler.getLocaleMsg("????????????????????????", "" + row));

        //??????????????????????????????????????????
        List<User> supUsers = rbacClient.listByUser(new User().setNickname(divisionMaterialModelDTO.getSupUserNickname()));
        Assert.notEmpty(supUsers, LocaleHandler.getLocaleMsg("??????????????????????????????????????????", "" + row));

        //???????????????????????????????????????
        List<User> strategyUsers = rbacClient.listByUser(new User().setNickname(divisionMaterialModelDTO.getStrategyUserNickname()));
        Assert.notEmpty(strategyUsers, LocaleHandler.getLocaleMsg("???????????????????????????????????????", "" + row));

        //?????????????????????????????????
        List<User> performUsers = rbacClient.listByUser(new User().setNickname(divisionMaterialModelDTO.getPerformUserNickname()));
        Assert.notEmpty(performUsers, LocaleHandler.getLocaleMsg("???????????????????????????????????????", "" + row));

        BeanUtils.copyProperties(divisionMaterialModelDTO, divisionMaterial, startDate, endDate);
        divisionMaterial.setStartDate(parseStartDate).setEndDate(parseEndDate);

        //????????????????????????
        divisionMaterial.setOrgId(org.getOrganizationId())
                .setOrgCode(org.getOrganizationCode())
                .setOrgName(org.getOrganizationName());

        //????????????????????????
        divisionMaterial.setOrganizationId(organization.getOrganizationId())
                .setOrganizationCode(organization.getOrganizationCode())
                .setOrganizationName(organization.getOrganizationName());

        //????????????????????????
        divisionMaterial.setMaterialId(materialItems.get(0).getMaterialId())
                .setMaterialCode(materialItems.get(0).getMaterialCode())
                .setMaterialName(materialItems.get(0).getMaterialName());

        //????????????????????????????????????
        divisionMaterial.setSupUserId(supUsers.get(0).getUserId())
                .setSupUserNickname(supUsers.get(0).getNickname())
                .setSupUserName(supUsers.get(0).getUsername());

        //?????????????????????????????????
        divisionMaterial.setStrategyUserId(strategyUsers.get(0).getUserId())
                .setStrategyUserNickname(strategyUsers.get(0).getNickname())
                .setStrategyUserName(strategyUsers.get(0).getUsername());

        //?????????????????????????????????
        divisionMaterial.setPerformUserId(performUsers.get(0).getUserId())
                .setPerformUserNickname(performUsers.get(0).getNickname())
                .setPerformUserName(performUsers.get(0).getUsername());

        //????????????
        checkParam(divisionMaterial);
    }

    private void checkParam(DivisionMaterial divisionMaterial) {
        //??????????????????????????????????????????????????????????????????
        Long materialId = divisionMaterial.getMaterialId();
        List<MaterialItem> materialItems = baseClient.listMaterialByParam(new MaterialItem().setMaterialId(materialId));
        if (!CollectionUtils.isEmpty(materialItems)) {
            MaterialItem materialItem = materialItems.get(0);
            if (materialItem != null) {
                Long categoryId = materialItem.getCategoryId();
                //2020-12-24 ????????????????????????bugfix
//                DivisionCategory one = iDivisionCategoryService.getOne(new QueryWrapper<>(new DivisionCategory()
//                        .setCategoryId(categoryId)
//                        .setOrgId(divisionMaterial.getOrgId())
//                        .setOrganizationId(divisionMaterial.getOrganizationId())));
//                if (one != null) {
//                    throw new BaseException(LocaleHandler.getLocaleMsg("???????????????,???????????????,???????????????????????????????????????????????????????????????,?????????!",
//                            divisionMaterial.getOrgName(), divisionMaterial.getOrganizationName(), divisionMaterial.getMaterialName()));
//                }

                List<DivisionCategory> divisionCategoryList = iDivisionCategoryService.list(new QueryWrapper<>(new DivisionCategory()
                        .setCategoryId(categoryId)
                        .setOrgId(divisionMaterial.getOrgId())
                        .setOrganizationId(divisionMaterial.getOrganizationId())));
                if(!CollectionUtils.isEmpty(divisionCategoryList)){
                    throw new BaseException(LocaleHandler.getLocaleMsg("???????????????,???????????????,???????????????????????????????????????????????????????????????,?????????!",
                            divisionMaterial.getOrgName(), divisionMaterial.getOrganizationName(), divisionMaterial.getMaterialName()));
                }
            }
        }
    }
}
