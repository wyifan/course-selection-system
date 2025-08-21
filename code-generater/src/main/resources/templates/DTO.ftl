package ${basePackage}.dto;

import lombok.Data;

@Data
public class ${entityName}DTO {
    <#list columns as field>
    private ${field.javaType} ${field.javaName};
    </#list>
}
