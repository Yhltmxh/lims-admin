package com.shou.mcmis.common.exception;

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

    @GetMapping("/business-409")
    public String business409() {
        throw new BusinessException(409, "数据冲突");
    }

    @GetMapping("/unauthorized")
    public String unauthorized() {
        throw new UnauthorizedException();
    }

    @GetMapping("/forbidden")
    public String forbidden() {
        throw new ForbiddenException();
    }
}
