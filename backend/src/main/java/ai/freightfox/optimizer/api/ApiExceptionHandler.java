package ai.freightfox.optimizer.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiModels.ErrorResponse> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(new ApiModels.ErrorResponse("BAD_REQUEST",e.getMessage(),Instant.now()));}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiModels.ErrorResponse> validation(MethodArgumentNotValidException e){String message=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).distinct().collect(Collectors.joining("; "));return ResponseEntity.badRequest().body(new ApiModels.ErrorResponse("VALIDATION_ERROR",message,Instant.now()));}
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiModels.ErrorResponse> unreadable(HttpMessageNotReadableException e){return ResponseEntity.badRequest().body(new ApiModels.ErrorResponse("INVALID_JSON","Request body is missing or is not valid JSON",Instant.now()));}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiModels.ErrorResponse> unexpected(Exception e){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiModels.ErrorResponse("INTERNAL_ERROR","An unexpected error occurred",Instant.now()));}
}
