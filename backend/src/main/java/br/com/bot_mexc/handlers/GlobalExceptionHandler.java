package br.com.bot_mexc.handlers;

import br.com.bot_mexc.models.dtos.errors.ErrorResponseDTO;
import jakarta.validation.ValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(ValidationException ex, WebRequest request) {
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String message = "Operação não pôde ser concluída devido a uma restrição de dados. Verifique se o item não está sendo utilizado em outro lugar.";
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(message, LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // 409 Conflict é um bom status para este caso
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(errorMessage, LocalDateTime.now());

        return handleExceptionInternal(ex, errorResponse, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String errorMessage = "Formato da requisição inválido.";
        Throwable cause = ex.getCause();

        // Percorre a cadeia de causas para encontrar a nossa ValidationException
        while (cause != null) {
            if (cause instanceof ValidationException) {
                errorMessage = cause.getMessage();
                break;
            }
            cause = cause.getCause();
        }

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(errorMessage, LocalDateTime.now());
        return handleExceptionInternal(ex, errorResponse, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Ocorreu um erro inesperado: ", ex);
        ErrorResponseDTO errorResponse = new ErrorResponseDTO("Ocorreu um erro inesperado no servidor.", LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}