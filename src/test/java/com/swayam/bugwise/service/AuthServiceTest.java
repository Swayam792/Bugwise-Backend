package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.AuthResponseDTO;
import com.swayam.bugwise.dto.LoginRequestDTO;
import com.swayam.bugwise.dto.SignupRequestDTO;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.jpa.UserRepository;
import com.swayam.bugwise.security.JwtTokenProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private LoginRequestDTO loginRequest;
    private SignupRequestDTO signupRequest;
    private User user;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestDTO("test@gmail.com", "test");
        signupRequest = new SignupRequestDTO("test@gmail.com", "test", UUID.randomUUID(), UserRole.DEVELOPER);
        user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("test");
        user.setRole(UserRole.DEVELOPER);
        user.setActive(true);
    }

    @Test
    void login_Success(@Mock Authentication authentication, @Mock UserDetails userDetails){

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(loginRequest.getEmail()))
                .thenReturn(userDetails);
        when(tokenProvider.generateToken(userDetails))
                .thenReturn("token123");

        // act
        AuthResponseDTO authResponseDTO = authService.login(loginRequest);

        // assert
        Assertions.assertNotNull(authResponseDTO);
        Assertions.assertEquals("token123", authResponseDTO.getToken());
        Assertions.assertEquals("test@gmail.com", authResponseDTO.getEmail());
        Assertions.assertEquals(UserRole.DEVELOPER, authResponseDTO.getRole());
    }

    @Test
    void signup_Success(){
        when(userRepository.existsByEmail(signupRequest.getEmail()))
                .thenReturn(false);
        when(passwordEncoder.encode(signupRequest.getPassword()))
                .thenReturn("password");
        when(userRepository.save(any(User.class)))
                .thenReturn(user);
        when(userDetailsService.loadUserByUsername(signupRequest.getEmail())).thenReturn(mock(UserDetails.class));
        when(tokenProvider.generateToken(any(UserDetails.class))).thenReturn("token123");

        AuthResponseDTO authResponseDTO = authService.signup(signupRequest);

        Assertions.assertNotNull(authResponseDTO);
        Assertions.assertEquals("token123", authResponseDTO.getToken());
        Assertions.assertEquals("test@gmail.com", authResponseDTO.getEmail());
        Assertions.assertEquals(UserRole.DEVELOPER, authResponseDTO.getRole());

    }

    @Test
    void signup_EmailAlreadyExists_ThrowsException(){
        when(userRepository.existsByEmail(signupRequest.getEmail()))
                .thenReturn(true);

        ValidationException validationException = Assertions.assertThrows(ValidationException.class, () -> authService.signup(signupRequest));
        Assertions.assertEquals("Email is already registered", validationException.getErrors().get("email"));
    }
}
