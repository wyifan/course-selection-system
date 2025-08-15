package com.yifan.api_client.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.yifan.api_client.dto.OrganizationDTO;

@FeignClient(name = "organization-service")
public interface OrganizationClient {
    @GetMapping("/organizations/{id}")
    OrganizationDTO getOrganizationById(@PathVariable("id") Long id);
}
