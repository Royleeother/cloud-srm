package com.midea.cloud.srm.model.base.dict.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.util.Date;
import java.time.LocalDate;

@Data
@ColumnWidth(15) //列宽
public class ImportExcelDictItemDTORight {
    /**
     * 字典编码
     */
    @ColumnWidth(23)
    @ExcelProperty(value = "*字典编码",index =0)
    private String dictCode;

    /**
     * 字典语言名称
     */
    @ColumnWidth(16)
    @ExcelProperty(value = "*字典语言名称",index =1)
    private String languageName;

    /**
     * 字典名称
     */
    @ColumnWidth(16)
    @ExcelProperty(value = "*字典名称",index =2)
    private String dictName;

    /**
     * 条目编码
     */
    @ExcelProperty(value = "*条目编码",index =3)
    private String dictItemCode;

    /**
     * 条目语言名称
     */
    @ColumnWidth(16)
    @ExcelProperty(value = "*条目语言名称",index =4)
    private String itemLanguageName;

    /**
     * 条目语言
     */
    @ExcelIgnore
    private String itemLanguage;
    /**
     * 条目名称
     */
    @ExcelProperty(value = "*条目名称",index =5)
    private String dictItemName;

    /**
     * 条目描述
     */
    @ExcelProperty(value = "条目描述",index =6)
    private String itemDescription;

    /**
     * 条目顺序
     */
    @ExcelProperty(value = "条目顺序",index =7)
    private Integer dictItemNo;

    /**
     * 条目标识
     */
    @ExcelProperty(value = "条目标识",index =8)
    private String dictItemMark;

    /**
     * 生效日期
     */
    @ExcelProperty(value = "*生效日期",index =9)
    private Date activeDate;

    /**
     * 失效日期
     */
    @ExcelProperty(value = "失效日期",index =10)
    private Date inactiveDate;

    /**
     * 错误信息提示
     */
    @ExcelProperty(value = "错误信息提示",index =11)
    private String errorMsg;
}
