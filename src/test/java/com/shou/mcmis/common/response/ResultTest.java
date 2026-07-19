package com.shou.mcmis.common.response;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void successShouldReturn200WithData() {
        Result<String> result = Result.success("hello");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    void successWithoutDataShouldReturn200WithNullData() {
        Result<Void> result = Result.success();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    void failShouldReturnGivenCodeAndMessage() {
        Result<Void> result = Result.fail(404, "用户不存在");
        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMessage()).isEqualTo("用户不存在");
        assertThat(result.getData()).isNull();
    }

    @Test
    void failWithDefaultCodeShouldReturn500() {
        Result<Void> result = Result.fail("服务器错误");
        assertThat(result.getCode()).isEqualTo(500);
    }
}
