package rtp.example.rtp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import rtp.example.rtp.portfolio.PortfolioRepository;
import rtp.example.rtp.user.UserRepository;

import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CriticalFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String randomUsername() {
        return "testuser_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String registerAndLogin() {
        String username = randomUsername();
        Map<String, Object> registerBody = Map.of(
                "username", username,
                "password", "password123",
                "email", username + "@example.com"
        );

        ResponseEntity<Map> reg = rest.postForEntity(
                baseUrl("/api/v1/auth/register"),
                registerBody,
                Map.class
        );

        if (reg.getStatusCode() != HttpStatus.OK) {
            System.err.println("Registration failed: " + reg.getBody());
            throw new RuntimeException("Registration failed");
        }

        assertTrue(reg.getBody().containsKey("accessToken"), "Response missing accessToken: " + reg.getBody());
        return (String) reg.getBody().get("accessToken");
    }

    private Long getPortfolioIdDirectly(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        var portfolio = portfolioRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Portfolio not found for user: " + username));
        return portfolio.getId();
    }

    @Test
    void userCanRegisterAndLogin() {
        String token = registerAndLogin();
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @Transactional
    void portfolioCreatedOnRegistration() {
        String username = randomUsername();
        Map<String, Object> registerBody = Map.of(
                "username", username,
                "password", "password123",
                "email", username + "@example.com"
        );

        ResponseEntity<Map> reg = rest.postForEntity(
                baseUrl("/api/v1/auth/register"),
                registerBody,
                Map.class
        );

        assertEquals(HttpStatus.OK, reg.getStatusCode(), "Registration should succeed");

        // Check directly in database
        var user = userRepository.findByUsername(username);
        assertTrue(user.isPresent(), "User should exist in database");

        var portfolio = portfolioRepository.findByUserId(user.get().getId());
        assertTrue(portfolio.isPresent(), "Portfolio should be auto-created for new user");
        assertNotNull(portfolio.get().getCashBalance(), "Portfolio should have cash balance");
    }

    @Test
    void userCanGetQuote() {
        String token = registerAndLogin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map> quote = rest.exchange(
                baseUrl("/api/v1/trading/quote?stockSymbol=AAPL&quantity=1&orderType=BUY"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        if (quote.getStatusCode() != HttpStatus.OK) {
            System.err.println("Quote failed: " + quote.getBody());
        }

        assertEquals(HttpStatus.OK, quote.getStatusCode());
        assertTrue(quote.getBody().containsKey("currentPrice"));
        assertTrue(quote.getBody().containsKey("totalValue"));
    }

    @Test
    @Transactional
    void userCanBuyStock() {
        String username = randomUsername();
        Map<String, Object> registerBody = Map.of(
                "username", username,
                "password", "password123",
                "email", username + "@example.com"
        );

        ResponseEntity<Map> reg = rest.postForEntity(
                baseUrl("/api/v1/auth/register"),
                registerBody,
                Map.class
        );

        String token = (String) reg.getBody().get("accessToken");
        Long portfolioId = getPortfolioIdDirectly(username);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of(
                "portfolioId", portfolioId,
                "stockSymbol", "AAPL",
                "quantity", 1,
                "priceType", "MARKET"
        );

        ResponseEntity<Map> trade = rest.exchange(
                baseUrl("/api/v1/trading/buy"),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );

        if (trade.getStatusCode() != HttpStatus.OK) {
            System.err.println("Trade failed: " + trade.getBody());
        }

        assertEquals(HttpStatus.OK, trade.getStatusCode());
        assertTrue(trade.getBody().get("message").toString().toLowerCase().contains("success"));
    }

    @Test
    @Transactional
    void cannotBuyWithInsufficientFunds() {
        String username = randomUsername();
        Map<String, Object> registerBody = Map.of(
                "username", username,
                "password", "password123",
                "email", username + "@example.com"
        );

        ResponseEntity<Map> reg = rest.postForEntity(
                baseUrl("/api/v1/auth/register"),
                registerBody,
                Map.class
        );

        String token = (String) reg.getBody().get("accessToken");
        Long portfolioId = getPortfolioIdDirectly(username);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of(
                "portfolioId", portfolioId,
                "stockSymbol", "AAPL",
                "quantity", 999999,
                "priceType", "MARKET"
        );

        ResponseEntity<Map> trade = rest.exchange(
                baseUrl("/api/v1/trading/buy"),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );

        // Should return 400 Bad Request
        assertTrue(
                trade.getStatusCode() == HttpStatus.BAD_REQUEST ||
                        (trade.getBody() != null && trade.getBody().toString().toLowerCase().contains("insufficient")),
                "Expected BAD_REQUEST or insufficient funds message, but got: " + trade.getStatusCode() + " - " + trade.getBody()
        );
    }
}