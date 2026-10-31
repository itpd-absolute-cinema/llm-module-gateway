package ru.inno.itpd.gateway;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import ru.inno.itpd.gateway.PluginDtos.ErrorResponse;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(GatewayException.class)
    ResponseEntity<ErrorResponse> gateway(GatewayException e) {
        return ResponseEntity.status(e.status())
                .body(ErrorResponse.of(e.code(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> "'%s': %s".formatted(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.joining("; "));

        return badRequest(
                "Request validation failed: " + message,
                "validation_error",
                null
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
        return badRequest(
                describe(e),
                "invalid_request_body",
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(
            MissingServletRequestParameterException e
    ) {
        return badRequest(
                "Required parameter '%s' is missing (expected type: %s)"
                        .formatted(e.getParameterName(), e.getParameterType()),
                "missing_parameter",
                e.getParameterName()
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErrorResponse> missingPart(MissingServletRequestPartException e) {
        return badRequest(
                "Required multipart request part '%s' is missing"
                        .formatted(e.getRequestPartName()),
                "missing_request_part",
                e.getRequestPartName()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(ErrorResponse.of(
                        "plugin_too_large",
                        "Uploaded plugin source exceeds the maximum allowed size of 1 MiB"
                ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        log.error("Unexpected API error", e);

        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(
                        "internal_error",
                        "An unexpected internal error occurred while processing the request"
                ));
    }

    private ResponseEntity<ErrorResponse> badRequest(
            String message,
            String type,
            Object param
    ) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        new PluginDtos.ErrorDetail(message, type, param, "malformed_request")
                ));
    }

    private String describe(HttpMessageNotReadableException e) {
        Throwable cause = e.getMostSpecificCause();

        if (cause instanceof JsonParseException json) {
            return "Malformed JSON at line %d, column %d"
                    .formatted(
                            json.getLocation().getLineNr(),
                            json.getLocation().getColumnNr()
                    );
        }

        if (cause instanceof MismatchedInputException mismatch) {
            String path = mismatch.getPath().stream()
                    .map(ref -> ref.getFieldName() != null
                            ? ref.getFieldName()
                            : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));

            return "Invalid value for expected type '%s'%s"
                    .formatted(
                            mismatch.getTargetType() != null
                                    ? mismatch.getTargetType().getSimpleName()
                                    : "unknown",
                            path.isBlank() ? "" : " at field '" + path + "'"
                    );
        }

        return "Request body is missing or contains invalid values";
    }
}
