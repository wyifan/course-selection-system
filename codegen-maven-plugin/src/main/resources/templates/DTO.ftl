package ${generator.package.basePackage}.${generator.package.dto};

import lombok.Data;

import java.io.Serializable;
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
 * ${table.entityName} DTO类, 参与网络传输，需要实现 Serializable 接口
 * </p>
 *
 * @author CodeGenerator by Shawn Wang
 * @since ${.now?string("yyyy-MM-dd HH:mm:ss")}
 */
@Data
public class ${table.entityName}DTO implements Serializable{
    private static final long serialVersionUID = 1L;

    <#list table.columns as column>
    /**
     * ${column.comment}
     */
    private ${column.javaType} ${column.javaName};
    </#list>
}
