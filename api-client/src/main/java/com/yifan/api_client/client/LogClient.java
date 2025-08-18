package com.yifan.api_client.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Created by: yifan
 * Created on: 2025-08-15 10:50:54
 * Description: 
 */ 
@FeignClient(name = "log-service", path = "/logs")
public interface LogClient {
    
}
