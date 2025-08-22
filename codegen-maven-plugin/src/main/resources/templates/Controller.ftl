package ${basePackage}.controller;

import ${basePackage}.dto.${entityName}DTO;
import ${basePackage}.service.${entityName}Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/${tableName}")
public class ${entityName}Controller {

    @Autowired
    private ${entityName}Service service;

    @PostMapping
    public ${entityName}DTO create(@RequestBody ${entityName}DTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ${entityName}DTO update(@PathVariable Long id, @RequestBody ${entityName}DTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}")
    public ${entityName}DTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<${entityName}DTO> list() {
        return service.listAll();
    }
}
