package ru.inno.itpd.gateway.api.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import ru.inno.itpd.gateway.api.dto.PluginDtos;
import ru.inno.itpd.gateway.api.dto.PluginDtos.ErrorResponse;
import ru.inno.itpd.gateway.core.exception.GatewayException;

import java.util.stream.Collectors;

/**
 * Logging policy: client mistakes (4xx) are one INFO line without a stack trace,
 * server failures (5xx) are ERROR with the stack trace.
 * Only the path is logged, never the query string: it can carry plugin settings.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(GatewayException.class)
    ResponseEntity<ErrorResponse> gateway(GatewayException e, HttpServletRequest request) {
        if (e.status().is5xxServerError()) {
            log.error("{} {} -> {} {}: {}", request.getMethod(), request.getRequestURI(),
                    e.status().value(), e.code(), e.getMessage(), e);
        } else {
            logClientError(request, e.status().value(), e.code(), e.getMessage());
        }

        return ResponseEntity.status(e.status())
                .body(ErrorResponse.of(e.code(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> "'%s': %s".formatted(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.joining("; "));

        return badRequest(
                request,
                "Request validation failed: " + message,
                "validation_error",
                null
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        return badRequest(
                request,
                describe(e),
                "invalid_request_body",
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        return badRequest(
                request,
                "Required parameter '%s' is missing (expected type: %s)"
                        .formatted(e.getParameterName(), e.getParameterType()),
                "missing_parameter",
                e.getParameterName()
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErrorResponse> missingPart(
            MissingServletRequestPartException e,
            HttpServletRequest request
    ) {
        return badRequest(
                request,
                "Required multipart request part '%s' is missing"
                        .formatted(e.getRequestPartName()),
                "missing_request_part",
                e.getRequestPartName()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> tooLarge(
            MaxUploadSizeExceededException e,
            HttpServletRequest request
    ) {
        String message = "Uploaded plugin source exceeds the maximum allowed size of 1 MiB";
        logClientError(request, HttpStatus.CONTENT_TOO_LARGE.value(), "plugin_too_large", message);

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(ErrorResponse.of("plugin_too_large", message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) {
        // Spring MVC's own exceptions (unknown path, wrong method, wrong media type...) know their status
        if (e instanceof org.springframework.web.ErrorResponse mvcError) {
            return frameworkError(mvcError, e, request);
        }

        log.error("{} {} -> 500 internal_error", request.getMethod(), request.getRequestURI(), e);

        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(
                        "internal_error",
                        "An unexpected internal error occurred while processing the request"
                ));
    }

    private ResponseEntity<ErrorResponse> frameworkError(
            org.springframework.web.ErrorResponse mvcError,
            Exception e,
            HttpServletRequest request
    ) {
        HttpStatusCode status = mvcError.getStatusCode();

        String code = switch (status.value()) {
            case 404 -> "not_found";
            case 405 -> "method_not_allowed";
            case 406 -> "not_acceptable";
            case 415 -> "unsupported_media_type";
            default -> status.is4xxClientError() ? "bad_request" : "internal_error";
        };

        String message = status.value() == 404
                ? "No endpoint found for %s %s".formatted(request.getMethod(), request.getRequestURI())
                : mvcError.getBody().getDetail();

        if (status.is5xxServerError()) {
            log.error("{} {} -> {} {}: {}", request.getMethod(), request.getRequestURI(),
                    status.value(), code, message, e);
        } else {
            logClientError(request, status.value(), code, message);
        }

        return ResponseEntity.status(status)
                .headers(mvcError.getHeaders())   // for example Allow on 405, Accept on 415
                .body(ErrorResponse.of(code, message));
    }

    private ResponseEntity<ErrorResponse> badRequest(
            HttpServletRequest request,
            String message,
            String type,
            Object param
    ) {
        logClientError(request, HttpStatus.BAD_REQUEST.value(), type, message);

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        new PluginDtos.ErrorDetail(message, type, param, "malformed_request")
                ));
    }

    private void logClientError(HttpServletRequest request, int status, String code, String message) {
        log.info("{} {} -> {} {}: {}", request.getMethod(), request.getRequestURI(), status, code, message);
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