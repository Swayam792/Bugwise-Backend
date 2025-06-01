package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.AuthResponseDTO;
import com.swayam.bugwise.dto.LoginRequestDTO;
import com.swayam.bugwise.dto.SignupRequestDTO;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.jpa.OrganizationRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import com.swayam.bugwise.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
        String token = tokenProvider.generateToken(userDetails);

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return new AuthResponseDTO(token, user.getEmail(), user.getRole());
    }

    @Transactional
    public AuthResponseDTO signup(SignupRequestDTO signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new ValidationException(Map.of("email", "Email is already registered"));
        }

        User user = new User();
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        if(signupRequest.getRole() != null){
            user.setRole(signupRequest.getRole());
        }else{
            user.setRole(UserRole.DEVELOPER);
        }

        user.setActive(true);

        User savedUser = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = tokenProvider.generateToken(userDetails);

        return new AuthResponseDTO(token, savedUser.getEmail(), savedUser.getRole());
    }
}