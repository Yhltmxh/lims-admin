package com.shou.mcmis.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class CursorPageVO<T> {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> records;
    private Long nextCursor;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean hasMore;
}
