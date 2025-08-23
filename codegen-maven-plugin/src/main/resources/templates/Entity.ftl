package ${generator.package.basePackage}.${generator.package.entity};

import com.baomidou.mybatisplus.annotation.TableName;
import ${generator.package.basePackage}.${generator.package.baseEntity}.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

<#list table.columns as column>
    <#if column.javaType == "BigDecimal">
import java.math.BigDecimal;
        <#break>
    </#if>
</#list>
<#list table.columns as column>
    <#if column.javaType == "LocalDate" || column.javaType == "LocalDateTime">
import java.time.*;
        <#break>
    </#if>
</#list>

/**
 * <p>
 * ${table.entityName} 实体类, 不参与网络传输，不需要实现 Serializable 接口
 * </p>
 *
 * @author CodeGenerator by Shawn Wang
 * @since ${.now?string("yyyy-MM-dd HH:mm:ss")}
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
