package ${generator.package.basePackage}.${generator.package.serviceImpl};

import ${generator.package.basePackage}.${generator.package.entity}.${table.entityName};
import ${generator.package.basePackage}.${generator.package.mapper}.${table.entityName}Mapper;
import ${generator.package.basePackage}.${generator.package.service}.I${table.entityName}Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * ${table.entityName} 服务实现类
 * </p>
 *
 * @author CodeGenerator  by Shawn Wang
 * @since ${.now?string("yyyy-MM-dd HH:mm:ss")}
 */
@Service
public class ${table.entityName}ServiceImpl extends ServiceImpl<${table.entityName}Mapper, ${table.entityName}> implements I${table.entityName}Service {

}