package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.auth.AuthRequest;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.entity.UserRole;
import com.ste.restaurant.repository.UserRepository;
import com.ste.restaurant.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController REST endpoints.
 * Uses full Spring Boot context with mocked dependencies.
 * <p>
 * Tests cover:
 * - Login authentication
 * - User registration
 * - JWT token generation
 * - Input validation
 * - Error handling
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
            // Given
            AuthRequest authRequest = new AuthRequest();
            authRequest.setEmail("test@example.com");
            authRequest.setPassword("Password123");

            User mockUser = new User();
            mockUser.setEmail("test@example.com");
            mockUser.setRole(UserRole.CUSTOMER);

            Authentication mockAuth = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(mockUser));
            when(jwtUtil.generateToken(any(User.class)))
                    .thenReturn("mock-jwt-token");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository).findByEmail("test@example.com");
            verify(jwtUtil).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should return unauthorized for invalid credentials")
        void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
            // Given
            AuthRequest authRequest = new AuthRequest();
            authRequest.setEmail("test@example.com");
            authRequest.setPassword("wrong password");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("Invalid email or password"));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository, never()).findByEmail(anyString());
            verify(jwtUtil, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should return bad request for invalid email format")
        void shouldReturnBadRequestForInvalidEmail() throws Exception {
            // Given
            AuthRequest authRequest = new AuthRequest();
            authRequest.setEmail("invalid-email");
            authRequest.setPassword("Password123");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isBadRequest());

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Should return bad request for empty email")
        void shouldReturnBadRequestForEmptyEmail() throws Exception {
            // Given
            AuthRequest authRequest = new AuthRequest();
            authRequest.setEmail("");
            authRequest.setPassword("Password123");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isBadRequest());

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Should return bad request for empty password")
        void shouldReturnBadRequestForEmptyPassword() throws Exception {
            // Given
            AuthRequest authRequest = new AuthRequest();
            authRequest.setEmail("test@example.com");
            authRequest.setPassword("");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isBadRequest());

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Should return internal server error for unexpected exception")
        void shouldReturnInternalServerErrorForUnexpectedException() throws Exception {
            // Given
            AuthRequest authRequest = new AuthRequest();
            authRequest.setEmail("test@example.com");
            authRequest.setPassword("Password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Login failed"));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("john.doe@example.com");
            userDto.setPassword("Password123");

            User savedUser = new User();
            savedUser.setFirstName("John");
            savedUser.setLastName("Doe");
            savedUser.setEmail("john.doe@example.com");
            savedUser.setRole(UserRole.CUSTOMER);

            when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn("mock-jwt-token");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"));

            verify(userRepository).existsByEmail("john.doe@example.com");
            verify(passwordEncoder).encode("Password123");
            verify(userRepository).save(any(User.class));
            verify(jwtUtil).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should return conflict when email already exists")
        void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("Jane");
            userDto.setLastName("Doe");
            userDto.setEmail("existing@example.com");
            userDto.setPassword("Password123");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isConflict())
                    .andExpect(content().string("Email already exists! Please try login."));

            verify(userRepository).existsByEmail("existing@example.com");
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
            verify(jwtUtil, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("Should return bad request for invalid email")
        void shouldReturnBadRequestForInvalidEmail() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("invalid-email");
            userDto.setPassword("Password123");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should return bad request for empty first name")
        void shouldReturnBadRequestForEmptyFirstName() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("");
            userDto.setLastName("Doe");
            userDto.setEmail("test@example.com");
            userDto.setPassword("Password123");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should return bad request for weak password")
        void shouldReturnBadRequestForWeakPassword() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("test@example.com");
            userDto.setPassword("weak");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should return bad request for password without uppercase")
        void shouldReturnBadRequestForPasswordWithoutUppercase() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("test@example.com");
            userDto.setPassword("password123");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should return bad request for password without digit")
        void shouldReturnBadRequestForPasswordWithoutDigit() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("test@example.com");
            userDto.setPassword("Password");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should register user as customer by default")
        void shouldRegisterUserAsCustomerByDefault() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("customer@example.com");
            userDto.setPassword("Password123");

            User savedUser = new User();
            savedUser.setRole(UserRole.CUSTOMER);

            when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn("mock-jwt-token");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());

            verify(userRepository).save(argThat(user -> user.getRole() == UserRole.CUSTOMER));
        }

        @Test
        @DisplayName("Should register user with minimum valid data")
        void shouldRegisterUserWithMinimumValidData() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setEmail("minimal@example.com");
            userDto.setPassword("Password123");

            User savedUser = new User();
            savedUser.setRole(UserRole.CUSTOMER);

            when(userRepository.existsByEmail("minimal@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn("mock-jwt-token");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());

            verify(userRepository).existsByEmail("minimal@example.com");
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(post("/rest/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(""));
        }

        @Test
        @DisplayName("Should allow logout without authentication")
        void shouldAllowLogoutWithoutAuthentication() throws Exception {
            // When & Then
            mockMvc.perform(post("/rest/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(""));
        }
    }

    @Nested
    @DisplayName("Authentication Flow Tests")
    class AuthenticationFlowTests {

        @Test
        @DisplayName("Should complete full authentication flow")
        void shouldCompleteFullAuthenticationFlow() throws Exception {
            // Given - Register user
            UserDtoIO registerDto = new UserDtoIO();
            registerDto.setFirstName("Flow");
            registerDto.setLastName("Test");
            registerDto.setEmail("flow@example.com");
            registerDto.setPassword("FlowPass123");

            User savedUser = new User();
            savedUser.setEmail("flow@example.com");
            savedUser.setRole(UserRole.CUSTOMER);

            when(userRepository.existsByEmail("flow@example.com")).thenReturn(false);
            when(passwordEncoder.encode("FlowPass123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn("register-token");

            // When & Then - Register
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("register-token"));

            // Given - Login with same credentials
            AuthRequest loginRequest = new AuthRequest();
            loginRequest.setEmail("flow@example.com");
            loginRequest.setPassword("FlowPass123");

            Authentication mockAuth = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(userRepository.findByEmail("flow@example.com"))
                    .thenReturn(Optional.of(savedUser));
            when(jwtUtil.generateToken(any(User.class)))
                    .thenReturn("login-token");

            // When & Then - Login
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("login-token"));

            // When & Then - Logout
            mockMvc.perform(post("/rest/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(""));
        }

        @Test
        @DisplayName("Should not allow duplicate registration")
        void shouldNotAllowDuplicateRegistration() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("Duplicate");
            userDto.setLastName("Test");
            userDto.setEmail("duplicate@example.com");
            userDto.setPassword("DupPass123");

            // First registration setup
            User savedUser = new User();
            savedUser.setRole(UserRole.CUSTOMER);

            when(userRepository.existsByEmail("duplicate@example.com"))
                    .thenReturn(false)  // First call returns false
                    .thenReturn(true);  // Second call returns true

            when(passwordEncoder.encode("DupPass123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtil.generateToken(any(User.class))).thenReturn("mock-jwt-token");

            // When & Then - First registration should succeed
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isOk());

            // When & Then - Second registration should fail
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() throws Exception {
            // Given
            UserDtoIO emptyDto = new UserDtoIO();

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyDto)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() throws Exception {
            // Given
            AuthRequest invalidEmailRequest = new AuthRequest();
            invalidEmailRequest.setEmail("not-an-email");
            invalidEmailRequest.setPassword("Password123");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                    .andExpect(status().isBadRequest());

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Should validate password complexity")
        void shouldValidatePasswordComplexity() throws Exception {
            // Given
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("Test");
            userDto.setEmail("test@example.com");
            userDto.setPassword("simple");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("Should validate email required for login")
        void shouldValidateEmailRequiredForLogin() throws Exception {
            // Given
            AuthRequest request = new AuthRequest();
            request.setPassword("Password123");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Should validate password required for login")
        void shouldValidatePasswordRequiredForLogin() throws Exception {
            // Given
            AuthRequest request = new AuthRequest();
            request.setEmail("test@example.com");

            // When & Then
            mockMvc.perform(post("/rest/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authenticationManager, never()).authenticate(any());
        }
    }
}
