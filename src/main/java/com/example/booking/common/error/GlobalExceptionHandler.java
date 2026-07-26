package com.example.booking.common.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates domain and framework exceptions into RFC 7807 {@link ProblemDetail}
 * responses. Deliberately no hand-rolled error POJO.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE = "https://booking.example.com/problems/";

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource not found");
        pd.setType(URI.create(PROBLEM_BASE + "not-found"));
        return pd;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        pd.setType(URI.create(PROBLEM_BASE + "conflict"));
        return pd;
    }

    /** Bean Validation failures on @Valid @RequestBody DTOs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle("Validation error");
        pd.setType(URI.create(PROBLEM_BASE + "validation"));
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }

    /**
     * Malformed/unparseable request body — bad JSON, or a value that doesn't fit the target
     * type (e.g. an unknown enum constant like an invalid {@code role}). This is a client
     * error (400), not a server error; without this handler the catch-all would wrongly 500 it.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        String detail = "Malformed or unreadable request body";
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife
                && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            detail = "Invalid value '" + ife.getValue() + "': must be one of "
                    + java.util.Arrays.toString(ife.getTargetType().getEnumConstants());
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Malformed request");
        pd.setType(URI.create(PROBLEM_BASE + "malformed-request"));
        return pd;
    }

    /** A path/query parameter that can't be converted to its target type (e.g. non-numeric id). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' has an invalid value: " + ex.getValue());
        pd.setTitle("Invalid parameter");
        pd.setType(URI.create(PROBLEM_BASE + "invalid-parameter"));
        return pd;
    }

    /** Validation on @RequestParam / @PathVariable (method-level constraints). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Validation error");
        pd.setType(URI.create(PROBLEM_BASE + "validation"));
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Invalid username or password");
        pd.setTitle("Authentication failed");
        pd.setType(URI.create(PROBLEM_BASE + "authentication"));
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuth(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Authentication failed");
        pd.setType(URI.create(PROBLEM_BASE + "authentication"));
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        pd.setTitle("Access denied");
        pd.setType(URI.create(PROBLEM_BASE + "access-denied"));
        return pd;
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Last-resort handler so unexpected errors still come back as ProblemDetail. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        // Log the real cause — a generic 500 body is safe for clients but useless for debugging.
        log.error("Unhandled exception for [{}]", request.getDescription(false), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setTitle("Internal server error");
        pd.setType(URI.create(PROBLEM_BASE + "internal"));
        return pd;
    }
}

/*
 * ============================================================================
 * FILE ROLE: Central translation of exceptions -> RFC 7807 ProblemDetail JSON.
 * ----------------------------------------------------------------------------
 * WHAT IT DOES
 *   - One place that converts every exception into a consistent error response:
 *       NotFoundException                 -> 404
 *       ConflictException                 -> 409
 *       MethodArgumentNotValidException   -> 400 (+ field->message map)
 *       HttpMessageNotReadableException   -> 400 (malformed body / bad enum)
 *       MethodArgumentTypeMismatchException -> 400 (bad path/query type)
 *       ConstraintViolationException      -> 400
 *       BadCredentials/AuthenticationException -> 401
 *       AccessDeniedException             -> 403
 *       Exception (catch-all)             -> 500 (and LOGS the real cause)
 *
 * TECHNICAL CONCEPTS
 *   - @RestControllerAdvice is a global interceptor for exceptions thrown by any
 *     controller; @ExceptionHandler methods pick the handler by exception type
 *     (most specific wins). It runs in Spring's ExceptionHandlerExceptionResolver,
 *     which is why a catch-all(Exception) here would otherwise mask framework
 *     defaults - hence the explicit 400 handlers for unreadable/mismatched input.
 *   - RFC 7807 ProblemDetail is the STANDARD error shape (type/title/status/
 *     detail/instance). Using Spring's built-in ProblemDetail avoids a bespoke
 *     error POJO and gives clients one predictable format.
 *   - The catch-all logs the stack trace server-side but returns a generic
 *     message to clients (never leak internals).
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * IN SIMPLE WORDS (for a fresher)
 * ----------------------------------------------------------------------------
 * Things go wrong all the time: someone sends bad data, asks for a missing item,
 * or tries something they are not allowed to. Without a plan, the user would see
 * scary, confusing errors.
 *
 * This file is the single "complaints desk" for the entire app. No matter what
 * goes wrong anywhere, it catches the problem and turns it into a clean, tidy
 * message with the correct status number:
 *   - 404 = we could not find it
 *   - 409 = it clashes with current state
 *   - 400 = your input was wrong or unreadable
 *   - 401 = you are not logged in
 *   - 403 = you are logged in but not allowed
 *   - 500 = something unexpected broke on our side
 *
 * For unexpected 500 errors it also writes the real reason into the app's log
 * (so developers can investigate) while showing the user only a safe, generic
 * message. This is exactly the file we fixed so a bad "role" value returns a
 * friendly 400 instead of a scary 500.
 * ============================================================================
 */
