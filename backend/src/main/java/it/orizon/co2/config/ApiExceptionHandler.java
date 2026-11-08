package it.orizon.co2.config;

import it.orizon.co2.llm.LlmClient.LlmException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(LlmException.class)
    ResponseEntity<Map<String, String>> llm(LlmException e) {
        log.error("LLM error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "L'assistente non è al momento disponibile, riprova tra poco."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Messaggio vuoto o troppo lungo (max 2000 caratteri)."));
    }
}
