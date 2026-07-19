package com.shou.mcmis.organize.log.controller;

import com.shou.mcmis.common.response.CursorPageVO;
import com.shou.mcmis.common.response.Result;
import com.shou.mcmis.organize.log.dto.LogQueryDTO;
import com.shou.mcmis.organize.log.service.LogService;
import com.shou.mcmis.organize.log.vo.LogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/logs")
@RequiredArgsConstructor
@Tag(name = "操作日志")
public class LogController {

    private final LogService logService;

    @GetMapping
    @Operation(summary = "分页查询日志", operationId = "listOperationLogs")
    @PreAuthorize("hasAuthority('organize:log:list')")
    public Result<CursorPageVO<LogVO>> list(@Valid @ParameterObject LogQueryDTO query) {
        return Result.success(logService.page(query));
    }
}
