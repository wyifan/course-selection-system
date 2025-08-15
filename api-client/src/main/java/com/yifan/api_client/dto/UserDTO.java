package com.yifan.api_client.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String tenantId; // Multi-tenant support
    private String name;
    private String email;
}
