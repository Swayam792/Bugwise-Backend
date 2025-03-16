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
    private final OrganizationRepository organizationRepository;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        String token = tokenProvider.generateToken(userDetails);
        log.info("token: {}", token);

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return new AuthResponseDTO(token, user.getUsername(), user.getRole());
    }

    @Transactional
    public AuthResponseDTO signup(SignupRequestDTO signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new ValidationException(Map.of("username", "Username is already taken"));
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new ValidationException(Map.of("email", "Email is already registered"));
        }

//        Organization organization = organizationRepository.findById(signupRequest.getOrganizationId())
//                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
//        user.setOrganization(organization);
        if(signupRequest.getRole() != null){
            user.setRole(signupRequest.getRole());
        }else{
            user.setRole(UserRole.DEVELOPER); // Default role
        }

        user.setActive(true);

        User savedUser = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String token = tokenProvider.generateToken(userDetails);

        return new AuthResponseDTO(token, savedUser.getUsername(), savedUser.getRole());
    }
}