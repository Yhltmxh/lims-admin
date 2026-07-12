package com.shou.lims.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class CursorPageVO<T> {
    private List<T> records;
    private Long nextCursor;
    private Boolean hasMore;
}
