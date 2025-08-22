package ${basePackage}.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

<#if useBaseEntity>
import ${basePackage}.BaseEntity;
</#if>

@Data
<#if useBaseEntity>
@@EqualsAndHashCode(callSuper = true)
public class ${entityName} extends BaseEntity {
<#else>
public class ${entityName} {
</#if>
    <#list columns as field>
    /**
     * ${field.comment}
     */
    private ${field.javaType} ${field.javaName};
    </#list>
}
