package ${generator.package.entity};

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.baomidou.mybatisplus.annotation.TableName;

<#if table.useBaseEntity>
import ${generator.package.baseEntity}.BaseEntity;
</#if>

/**
 * <p>
 * ${table.entityName} 实体类, 不参与网络传输，不需要实现 Serializable 接口
 * </p>
 *
 * @author CodeGenerator by Shawn Wang
 * @since ${.now?string("yyyy-MM-dd")}
 */
@Data
<#if table.useBaseEntity>
@@EqualsAndHashCode(callSuper = true)
@TableName("${table.tableName}")
public class ${table.entityName} extends BaseEntity{
<#else>
public class ${table.entityName} {
</#if>

    <#list table.columns as column>
    /**
     * ${column.comment}
     */
    private ${column.javaType} ${column.javaName};
    </#list>
}
