package com.example.booking.user;

import com.example.booking.common.error.ConflictException;
import com.example.booking.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;

    UserService service() {
        return new UserService(users, encoder);
    }

    @Test
    void register_encodesPassword_andPersists() {
        lenient().when(encoder.encode("password1")).thenReturn("HASH");
        when(users.existsByEmail("new@x.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new RegisterRequest("new@x.com", "password1", "New User", Role.ATTENDEE);
        User saved = service().register(req);

        assertThat(saved.getPasswordHash()).isEqualTo("HASH");
        assertThat(saved.getRole()).isEqualTo(Role.ATTENDEE);
        assertThat(saved.isEnabled()).isTrue();
        verify(users).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(users.existsByEmail("dup@x.com")).thenReturn(true);
        var req = new RegisterRequest("dup@x.com", "password1", "Dup", Role.ATTENDEE);

        assertThatThrownBy(() -> service().register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_asAdmin_isRejected() {
        var req = new RegisterRequest("boss@x.com", "password1", "Boss", Role.ADMIN);

        assertThatThrownBy(() -> service().register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void getByEmail_missing_throwsNotFound() {
        when(users.findByEmail("ghost@x.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().getByEmail("ghost@x.com"))
                .hasMessageContaining("not found");
    }
}
