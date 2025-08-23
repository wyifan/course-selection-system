package ${generator.package.controller};

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * ${table.entityName} 前端控制器
 * </p>
 *
 * @author CodeGenerator by Shawn Wang
 * @since ${.now?string("yyyy-MM-dd")}
 */
@RestController
@RequestMapping("<#if generator.package.entity?contains(".")>${generator.package.entity?substring(generator.package.entity?last_index_of(".") + 1)}</#if>/${table.entityName?uncap_first}")
public class ${table.entityName}Controller {

}
