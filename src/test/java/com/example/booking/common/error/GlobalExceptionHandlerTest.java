package com.example.booking.common.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.example.booking.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the fix for the register 500: an unparseable body (including an invalid enum
 * value like a bad {@code role}) must map to 400, not 500.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void malformedBody_maps_to_400() {
        var ex = new HttpMessageNotReadableException("broken", (org.springframework.http.HttpInputMessage) null);
        ProblemDetail pd = handler.handleUnreadable(ex);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void invalidEnumValue_maps_to_400_withAllowedValues() throws Exception {
        // Simulate Jackson failing to convert "USER" into the Role enum.
        InvalidFormatException ife = InvalidFormatException.from(null,
                "no enum constant", "USER", Role.class);
        var ex = new HttpMessageNotReadableException("bad role", ife,
                (org.springframework.http.HttpInputMessage) null);

        ProblemDetail pd = handler.handleUnreadable(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).contains("ATTENDEE", "ORGANIZER", "ADMIN");
    }
}
