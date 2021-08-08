package com.midea.cloud.common.enums.price.costelement;

/**
 * <pre>
 * 成本要输类型
 * </pre>
 *
 * @author wangpr@meiCloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/7/25
 *  修改内容:
 * </pre>
 */
public enum ElementType {
    /**
     * MATERIAL-材质,CRAFT-工艺,FEE-费用; 字典编码-COST_ELEMENT_TYPE
     */
    MATERIAL("MATERIAL","材质"),
    CRAFT("CRAFT","工艺"),
    FEE("FEE","费用");



    private String key;
    private String value;

    ElementType(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
