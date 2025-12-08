package com.beauty.beauty_sales_dwh.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import lombok.extern.slf4j.Slf4j; // ログ出力用（Lombok）

@Slf4j // ログを出力できるようにする
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 型変換エラー（JSONパースエラー）
     * 例: Integer項目に "abc" を送った場合
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(HttpMessageNotReadableException ex) {
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Bad Request");

        if (ex.getCause() instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                                  .map(ref -> ref.getFieldName())
                                  .findFirst()
                                  .orElse("unknown");
            body.put("message", "入力形式が正しくありません。フィールド: " + fieldName + ", 値: " + ife.getValue());
        } else {
            body.put("message", "リクエストJSONの形式が不正、またはリクエストボディが空です。");
        }

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 2. バリデーションエラー（@NotNull, @Min, @Size など）
     * 例: 必須項目が null だった場合
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Validation Error");

        // 発生したすべてのフィールドエラーをリスト化して返す
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "エラーがあります",
                        (msg1, msg2) -> msg1 // キー重複時は最初のメッセージを採用
                ));

        body.put("message", "入力内容に誤りがあります。");
        body.put("details", errors); // どの項目がダメだったか詳細を返す

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 3. 予期せぬ NullPointerException
     * 例: サービス層などで .get() などを呼んで落ちた場合
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointer(NullPointerException ex) {
        // NPEはバグの可能性が高いので、必ずサーバーログにスタックトレースを吐く
        log.error("NullPointerException occurred: ", ex);

        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        body.put("message", "サーバー内部で予期せぬエラーが発生しました。");
        // セキュリティのため、外部には詳細なスタックトレースを返さないのが定石

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 4. その他の予期せぬエラー（安全策）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        log.error("Unexpected error occurred: ", ex);

        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        body.put("message", "システムエラーが発生しました。管理者に問い合わせてください。");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- 共通メソッド ---
    private Map<String, Object> createErrorBody(HttpStatus status, String errorTitle) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", errorTitle);
        return body;
    }
}