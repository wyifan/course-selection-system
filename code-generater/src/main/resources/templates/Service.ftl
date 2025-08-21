package ${basePackage}.service;

import ${basePackage}.dto.${entityName}DTO;
import java.util.List;

public interface ${entityName}Service {
    ${entityName}DTO create(${entityName}DTO dto);
    ${entityName}DTO update(Long id, ${entityName}DTO dto);
    void delete(Long id);
    ${entityName}DTO getById(Long id);
    List<${entityName}DTO> listAll();
}
