<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="${basePackage}.mapper.${entityName}Mapper">
    <!-- 通用查询结果 -->
    <resultMap id="BaseResultMap" type="${basePackage}.entity.${entityName}">
    <#list columns as col>
        <result column="${col.columnName}" property="${col.javaName}" />
    </#list>
    </resultMap>

    <!-- 通用查询字段 -->
    <sql id="Base_Column_List">
    <#list columns as col>
        ${col.columnName}<#if col_has_next>,</#if>
    </#list>
    </sql>
</mapper>
