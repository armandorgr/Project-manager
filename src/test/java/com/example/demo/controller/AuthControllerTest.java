package com.example.demo.controller;

import com.example.demo.controller.requests.LoginRequest;
import com.example.demo.controller.requests.LogoutRequest;
import com.example.demo.controller.requests.RegisterRequest;
import com.example.demo.controller.requests.TokenRefreshRequest;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.controller.responses.JwtResponse;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthControllerTest {
    private final String USERNAME = "user";
    private final String PASSWORD = "1234";
    private final String EMAIL = "email@gmail.com";
    private final String TEST_USER = "test_user";
    private final String TEST_PASSWORD = "test_password";
    private static final Instant NOW = Instant.now();
    private static final Duration TOKEN_EXPIRATION_TIME = Duration.ofSeconds(600);

    @MockitoBean
    private Clock clock;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepo;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void register_validRequest_thenLogin_ShouldReturn200() {
        HttpEntity<RegisterRequest> request = new HttpEntity<>(
                new RegisterRequest(USERNAME, EMAIL, PASSWORD)
        );
        //User registers
        ResponseEntity<ApiResponse<String>> registerResponse = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<ApiResponse<String>>() {
                }
        );
        assertThat(registerResponse).isNotNull();
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<String> body = registerResponse.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("Success");
        assertThat(body.getMessage()).isEqualTo("Usuario registrado correctamente");

        //User logs in
        HttpEntity<LoginRequest> loginRequest = new HttpEntity<>(
                new LoginRequest(USERNAME, PASSWORD)
        );

        ResponseEntity<ApiResponse<JwtResponse>> authResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                loginRequest,
                new ParameterizedTypeReference<ApiResponse<JwtResponse>>() {
                }
        );
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<JwtResponse> response = authResponse.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("Success");

        JwtResponse jwtResponse = response.getData();
        String accessToken = jwtResponse.accessToken();
        String refreshToken = jwtResponse.refreshToken();

        assertThat(jwtResponse).isNotNull();
        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        //User makes request to protected path
        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                "/api/protected/hello",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(protectedResponse).isNotNull();
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_thenLogout_shouldReturn200() {
        //User logs in
        HttpEntity<LoginRequest> loginRequest = new HttpEntity<>(
                new LoginRequest(TEST_USER, TEST_PASSWORD)
        );

        ResponseEntity<ApiResponse<JwtResponse>> authResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                loginRequest,
                new ParameterizedTypeReference<ApiResponse<JwtResponse>>() {
                }
        );
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<JwtResponse> response = authResponse.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("Success");

        JwtResponse jwtResponse = response.getData();
        String accessToken = jwtResponse.accessToken();
        String refreshToken = jwtResponse.refreshToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        //User logs out
        HttpEntity<LogoutRequest> logoutRequest = new HttpEntity<>(
                new LogoutRequest(accessToken, refreshToken),
                headers
        );
        ResponseEntity<ApiResponse<String>> logoutResponse = restTemplate.exchange(
                "/api/auth/logout",
                HttpMethod.POST,
                logoutRequest,
                new ParameterizedTypeReference<ApiResponse<String>>() {
                }
        );
        assertThat(logoutResponse).isNotNull();
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        //User tries to log out again, error is expected
        ResponseEntity<ApiResponse<String>> errorLogoutResponse = restTemplate.exchange(
                "/api/auth/logout",
                HttpMethod.POST,
                logoutRequest,
                new ParameterizedTypeReference<ApiResponse<String>>() {
                }
        );

        assertThat(errorLogoutResponse).isNotNull();
        assertThat(errorLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_thenAccessTokenExpires_thenPetitionToProtectedPath_shouldReturn403() throws InterruptedException {
        //User logs in
        HttpEntity<LoginRequest> loginRequest = new HttpEntity<>(
                new LoginRequest(TEST_USER, TEST_PASSWORD)
        );

        ResponseEntity<ApiResponse<JwtResponse>> authResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                loginRequest,
                new ParameterizedTypeReference<ApiResponse<JwtResponse>>() {
                }
        );
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ApiResponse<JwtResponse> response = authResponse.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("Success");

        JwtResponse jwtResponse = response.getData();
        String accessToken = jwtResponse.accessToken();
        String refreshToken = jwtResponse.refreshToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        when(clock.instant()).thenReturn(NOW.plus(TOKEN_EXPIRATION_TIME));
        //User makes request to protected path
        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                "/api/protected/hello",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(protectedResponse).isNotNull();
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        //User refreshes token and tries again
        HttpEntity<TokenRefreshRequest> refreshTokenRequest = new HttpEntity<>(
                new TokenRefreshRequest(refreshToken)
        );
        ResponseEntity<ApiResponse<JwtResponse>> refreshTokenResponse = restTemplate.exchange(
                "/api/auth/refresh",
                HttpMethod.POST,
                refreshTokenRequest,
                new ParameterizedTypeReference<ApiResponse<JwtResponse>>() {
                }
        );

        assertThat(refreshTokenResponse).isNotNull();
        assertThat(refreshTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JwtResponse newAccessToken = refreshTokenResponse.getBody().getData();
        headers.setBearerAuth(newAccessToken.accessToken());

        //Sets it back in order to work as expected
        when(clock.instant()).thenReturn(NOW);

        protectedResponse = restTemplate.exchange(
                "/api/protected/hello",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(protectedResponse).isNotNull();
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_badRequest_shouldReturn400() {
        //User tries to log in without body
        ResponseEntity<ApiResponse<JwtResponse>> authResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<ApiResponse<JwtResponse>>() {
                }
        );
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiResponse<JwtResponse> response = authResponse.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ERROR");

        //User tries to log in, but username is not found
        HttpEntity<LoginRequest> request = new HttpEntity<>(
                new LoginRequest("random", "password")
        );
        authResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<ApiResponse<JwtResponse>>() {
                }
        );
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        response = authResponse.getBody();
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ERROR");
    }

    @Test
    void register_badRequest_shouldReturn_400() {
        //User tries to register with no body
        ResponseEntity<ApiResponse<String>> registerResponse = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<ApiResponse<String>>() {
                }
        );
        assertThat(registerResponse).isNotNull();
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<String> body = registerResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("ERROR");

        //user tries to register, but username and password are empty
        HttpEntity<LoginRequest> request = new HttpEntity<>(
                new LoginRequest("", "")
        );
        ResponseEntity<String> bodyRegisterResponse = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                request,
                String.class
        );
        assertThat(bodyRegisterResponse).isNotNull();
        assertThat(bodyRegisterResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @AfterEach
    void cleanUp() {
        userRepo.findByUsername(USERNAME).ifPresent((user) -> {
            userRepo.delete(user);
        });
    }
}
