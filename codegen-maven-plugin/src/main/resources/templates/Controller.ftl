package ${generator.package.basePackage}.${generator.package.controller};

import ${generator.package.basePackage}.${generator.package.dto}.${table.entityName}DTO;
import ${generator.package.basePackage}.${generator.package.service}.I${table.entityName}Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * ${table.entityName} 前端控制器
 * </p>
 *
 * @author CodeGenerator by Shawn Wang
 * @since ${.now?string("yyyy-MM-dd HH:mm:ss")}
 */
@RestController
@RequestMapping("/${table.entityName?uncap_first}")
public class ${table.entityName}Controller {

    @Autowired
    private I${table.entityName}Service ${table.entityName?uncap_first}Service;

    // 更多CRUD方法可以基于DTO进行扩展...
    @GetMapping("/hello")
    public String hello() {
        return "Hello from ${table.entityName}Controller!";
    }
}
