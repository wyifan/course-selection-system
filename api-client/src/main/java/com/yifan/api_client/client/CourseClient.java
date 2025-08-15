package com.yifan.api_client.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.yifan.api_client.dto.CourseDTO;

@FeignClient(name = "course-service")
public interface CourseClient {
    @GetMapping("/courses/{id}")
    CourseDTO getCourseById(@PathVariable("id") Long id);
}
