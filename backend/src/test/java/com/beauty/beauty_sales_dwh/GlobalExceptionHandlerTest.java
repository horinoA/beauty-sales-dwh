package com.beauty.beauty_sales_dwh;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.beauty.beauty_sales_dwh.common.exception.GlobalExceptionHandler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// GlobalExceptionHandler と、テスト用コントローラーだけをロードしてテストする
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class) // テスト対象のハンドラーを取り込む
// addFilters = false で、Securityフィルター(認証やCSRF)を無効化します
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("型違反: 数値項目に文字列を入れると400エラーと適切なメッセージが返る")
    void testTypeMismatch() throws Exception {
        // value は Integer なのに "abc" を送る
        String json = """
            { "value": "abc" }
        """;

        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(print()) // ログにリクエスト/レスポンスを出す
                .andExpect(status().isBadRequest()) // 400 Check
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("入力形式が正しくありません。フィールド: value, 値: abc"));
    }

    @Test
    @DisplayName("バリデーション違反: 必須項目がnullだと400エラーと詳細が返る")
    void testValidationNull() throws Exception {
        // value は @NotNull なのに null を送る
        String json = """
            { "value": null }
        """;

        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.details.value").exists()); // 詳細マップに項目があるか
    }

    @Test
    @DisplayName("予期せぬエラー: NullPointerExceptionが発生すると500エラーになる")
    void testNpe() throws Exception {
        mockMvc.perform(get("/test/npe")) // NPEを投げるエンドポイントを叩く
                .andDo(print())
                .andExpect(status().isInternalServerError()) // 500 Check
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("サーバー内部で予期せぬエラーが発生しました。"));
                // スタックトレースが漏れていないことも確認できるとBest
    }

    // --- テスト専用のダミーコントローラー ---
    // わざとエラーを起こすためだけのクラス
    @TestConfiguration
    static class Config {
        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        
        // バリデーションと型チェック用
        @PostMapping("/test/validation")
        public void validationCheck(@RequestBody @Valid TestRequest request) {
            // 何もしない（ここまで到達する過程でエラーが起きるかテストする）
        }

        // NPE発生用
        @GetMapping("/test/npe")
        public void throwNpe() {
            throw new NullPointerException("Oops!");
        }
    }

    // バリデーション用のDTO
    record TestRequest(
        @NotNull
        Integer value
    ) {}
}