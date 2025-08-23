package ${generator.package.basePackage}.${generator.package.baseEntity};

import lombok.Data;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * 公共基础实体,基类不参与RPC调用，无需序列化
 * @author CodeGenerator by Shawn Wang
 * @since ${.now?string("yyyy-MM-dd HH:mm:ss")}
 */
@Data
public class BaseEntity{
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)     
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private String createdByName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedByName;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableLogic(value = "0", delval = "1") 
    private Integer isDeleted;

    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
