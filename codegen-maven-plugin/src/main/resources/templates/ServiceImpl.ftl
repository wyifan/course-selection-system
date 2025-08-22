package ${basePackage}.service.impl;

import ${basePackage}.dto.${entityName}DTO;
import ${basePackage}.entity.${entityName};
import ${basePackage}.mapper.${entityName}Mapper;
import ${basePackage}.service.${entityName}Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ${entityName}ServiceImpl implements ${entityName}Service {

    @Autowired
    private ${entityName}Mapper mapper;

    @Override
    public ${entityName}DTO create(${entityName}DTO dto) {
        // TODO: convert DTO to Entity
        return dto;
    }

    @Override
    public ${entityName}DTO update(Long id, ${entityName}DTO dto) {
        // TODO: implement update
        return dto;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public ${entityName}DTO getById(Long id) {
        // TODO: convert Entity to DTO
        return new ${entityName}DTO();
    }

    @Override
    public List<${entityName}DTO> listAll() {
        // TODO: implement listAll
        return List.of();
    }
}
