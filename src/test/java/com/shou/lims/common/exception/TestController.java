package com.shou.lims.common.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
class TestController {

    @GetMapping("/not-found")
    public String notFound() {
        throw new NotFoundException("资源不存在");
    }

    @GetMapping("/unknown-error")
    public String unknownError() {
        throw new RuntimeException("boom");
    }
}
