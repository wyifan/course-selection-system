package ${generator.package.basePackage}.${generator.package.mapper};

import ${generator.package.basePackage}.${generator.package.entity}.${table.entityName};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * ${table.entityName} Mapper 接口
 * </p>
 * 加上mapper注解，自动扫描，不加时，需要手动在配置类上添加@MapperScan
 * @author CodeGenerator by Shawn Wang
 * @since ${.now?string("yyyy-MM-dd HH:mm:ss")}
 */
 @Mapper
public interface ${table.entityName}Mapper extends BaseMapper<${table.entityName}> {

}