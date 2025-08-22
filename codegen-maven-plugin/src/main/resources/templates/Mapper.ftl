package ${basePackage}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ${basePackage}.entity.${entityName};
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ${entityName}Mapper extends BaseMapper<${entityName}> {
}
