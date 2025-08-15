package com.yifan.api_client.dto;

import lombok.Data;

@Data
public class OrganizationDTO {
    private Long id;
    private String tenantId;
    private String name;
    private String type; // e.g., school, college, dept
    private Long parentId; // For hierarchy
}
