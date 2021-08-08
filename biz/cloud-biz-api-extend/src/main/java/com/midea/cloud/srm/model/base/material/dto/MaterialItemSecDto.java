package com.midea.cloud.srm.model.base.material.dto;

import com.midea.cloud.srm.model.base.material.MaterialItem;
import com.midea.cloud.srm.model.base.material.MaterialItemSec;
import com.midea.cloud.srm.model.base.material.entity.ItemSceImage;
import com.midea.cloud.srm.model.file.upload.entity.Fileupload;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <pre>
 *  功能名称
 * </pre>
 *
 * @author chenwj92@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人: chenwj92
 *  修改日期: 2020/10/27 16:32
 *  修改内容:
 * </pre>
 */
@Data
@Accessors(chain = true)
public class MaterialItemSecDto {
    private MaterialItemSec materialItemSec;

    private List<Fileupload> fileuploadList;

    private List<ItemSceImage> itemSceImages;
}
