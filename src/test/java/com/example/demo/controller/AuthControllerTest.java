package com.example.demo.controller;

import com.example.demo.controller.requests.LoginRequest;
import com.example.demo.controller.requests.RegisterRequest;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String USERNAME = "user";
    private static final String TEST_USER = "test_user";
    private static final String TEST_PASSWORD = "test_password";
    private static final String EMAIL = "email@gmail.com";
    private static final Instant NOW = Instant.now();
    private static final Duration TOKEN_EXPIRATION_TIME = Duration.ofSeconds(90);

    @MockitoBean
    private Clock clock;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepo;

    @BeforeAll
    static void env(){
        Dotenv dotenv = Dotenv.configure().filename(".env.test").load();
        dotenv.entries().forEach((e)->System.setProperty(e.getKey(),e.getValue()));
    }

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
    }

    @AfterEach
    void cleanUp() {
        userRepo.findByUsername(USERNAME)
                .ifPresent(userRepo::delete);
    }

    // ------------------------------------------------------
    // Helpers
    // ------------------------------------------------------

    private <T> ResponseEntity<ApiResponse<T>> postForApiResponse(String url, Object body) {
        return restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<>() {}
        );
    }

    private HttpHeaders buildCookieHeaders(List<String> cookies) {
        HttpHeaders headers = new HttpHeaders();
        cookies.forEach(cookie -> headers.add(HttpHeaders.COOKIE, cookie));
        return headers;
    }

    private List<String> extractCookies(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies)
                .as("Response should contain cookies")
                .isNotEmpty();
        return cookies;
    }

    // ------------------------------------------------------
    // Tests
    // ------------------------------------------------------

    @Test
    void register_validRequest_thenLogin_shouldReturn200() {
        // --- Register user ---
        var registerRequest = new RegisterRequest(USERNAME, EMAIL, "1234");
        var registerResponse = postForApiResponse("/api/auth/register", registerRequest);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var registerBody = registerResponse.getBody();
        assertThat(registerBody).isNotNull();
        assertThat(registerBody.getStatus()).isEqualTo("SUCCESS");
        assertThat(registerBody.getMessage()).isEqualTo("User registered successfully");

        // --- Login user ---
        var loginRequest = new LoginRequest(USERNAME, "1234");
        var loginResponse = postForApiResponse("/api/auth/login", loginRequest);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var cookies = extractCookies(loginResponse);

        // --- Access protected endpoint ---
        HttpHeaders headers = buildCookieHeaders(cookies);
        var protectedResponse = restTemplate.exchange(
                "/api/protected/hello", HttpMethod.GET,
                new HttpEntity<>(headers), String.class
        );

        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Sql("/data.sql")
    @Test
    void login_thenLogout_shouldReturn200() {
        // --- Login user ---
        var loginRequest = new LoginRequest(TEST_USER, TEST_PASSWORD);
        var loginResponse = postForApiResponse("/api/auth/login", loginRequest);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var cookies = extractCookies(loginResponse);
        assertThat(cookies).hasSize(2);

        var body = loginResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo("SUCCESS");

        // --- Logout user ---
        HttpHeaders headers = buildCookieHeaders(cookies);
        var logoutResponse = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // --- Try logout again (should fail) ---
        var newCookies = extractCookies(logoutResponse);
        headers = buildCookieHeaders(newCookies);

        var errorLogoutResponse = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        assertThat(errorLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_thenAccessTokenExpires_thenRefresh_shouldWork() {
        // --- Login user ---
        var loginRequest = new LoginRequest(TEST_USER, TEST_PASSWORD);
        var loginResponse = postForApiResponse("/api/auth/login", loginRequest);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var cookies = extractCookies(loginResponse);
        assertThat(cookies).hasSize(2);

        HttpHeaders headers = buildCookieHeaders(cookies);

        // --- Expire token ---
        when(clock.instant()).thenReturn(NOW.plus(TOKEN_EXPIRATION_TIME));

        var protectedResponse = restTemplate.exchange(
                "/api/protected/hello", HttpMethod.GET,
                new HttpEntity<>(headers), String.class
        );
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // --- Refresh tokens ---
        var refreshResponse = restTemplate.exchange(
                "/api/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var refreshedCookies = extractCookies(refreshResponse);
        headers = buildCookieHeaders(refreshedCookies);

        // --- Back to current time ---
        when(clock.instant()).thenReturn(NOW);

        var finalProtectedResponse = restTemplate.exchange(
                "/api/protected/hello", HttpMethod.GET,
                new HttpEntity<>(headers), String.class
        );

        assertThat(finalProtectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_badRequest_shouldReturn400() {
        // --- Missing body ---
        var badResponse = restTemplate.exchange(
                "/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        assertThat(badResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badResponse.getBody()).isNotNull();
        assertThat(badResponse.getBody().getStatus()).isEqualTo("ERROR");

        // --- Wrong credentials ---
        var wrongLogin = new LoginRequest("random", "password");
        var wrongResponse = postForApiResponse("/api/auth/login", wrongLogin);

        assertThat(wrongResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(wrongResponse.getBody()).isNotNull();
        assertThat(wrongResponse.getBody().getStatus()).isEqualTo("ERROR");
    }

    @Test
    void register_badRequest_shouldReturn400() {
        // --- Missing body ---
        var missingBodyResponse = restTemplate.exchange(
                "/api/auth/register", HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        assertThat(missingBodyResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingBodyResponse.getBody()).isNotNull();
        assertThat(missingBodyResponse.getBody().getStatus()).isEqualTo("ERROR");

        // --- Empty credentials ---
        var emptyRequest = new LoginRequest("", "");
        var emptyResponse = restTemplate.exchange(
                "/api/auth/register", HttpMethod.POST,
                new HttpEntity<>(emptyRequest), String.class
        );

        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
