package com.peter.suuq.auth;

import com.peter.suuq.auth.dto.AuthResponse;
import com.peter.suuq.auth.dto.LoginRequest;
import com.peter.suuq.auth.dto.RegisterRequest;
import com.peter.suuq.auth.service.AuthService;
import com.peter.suuq.config.security.JwtService;
import com.peter.suuq.exception.DuplicateResourceException;
import com.peter.suuq.user.entity.Role;
import com.peter.suuq.user.entity.User;
import com.peter.suuq.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldReturnAuthResponse_whenEmailIsNew() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Peter Somto");
        request.setEmail("peter@suuq.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class)))
                .thenReturn("mockToken");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response.getToken()).isEqualTo("mockToken");
        assertThat(response.getEmail()).isEqualTo("peter@suuq.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_shouldThrowDuplicateResourceException_whenEmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Peter Somto");
        request.setEmail("peter@suuq.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("peter@suuq.com");
        request.setPassword("password123");

        User user = User.builder()
                .email("peter@suuq.com")
                .role(Role.CUSTOMER)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user))
                .thenReturn("mockToken");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response.getToken()).isEqualTo("mockToken");
        assertThat(response.getEmail()).isEqualTo("peter@suuq.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
    }
}