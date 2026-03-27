package backend.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import backend.models.Calendar;
import backend.models.CalendarConnectionRequest;

@Service
public class GoogleOAuthService {

    private final RestClient restClient;
    private final CalendarIntegrationService calendarIntegrationService;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;
    private final String authorizationBaseUrl;
    private final String tokenUrl;
    private final Map<String, PendingGoogleConnection> pendingStates = new ConcurrentHashMap<>();

    public GoogleOAuthService(
            CalendarIntegrationService calendarIntegrationService,
            @Value("${google.oauth.client-id:}") String clientId,
            @Value("${google.oauth.client-secret:}") String clientSecret,
            @Value("${google.oauth.redirect-uri:http://localhost:8080/api/oauth/google/callback}") String redirectUri,
            @Value("${google.oauth.scope:https://www.googleapis.com/auth/calendar}") String scope,
            @Value("${google.oauth.authorization-base-url:https://accounts.google.com/o/oauth2/v2/auth}") String authorizationBaseUrl,
            @Value("${google.oauth.token-url:https://oauth2.googleapis.com/token}") String tokenUrl) {
        this(calendarIntegrationService, RestClient.builder().build(), clientId, clientSecret, redirectUri, scope, authorizationBaseUrl, tokenUrl);
    }

    @Autowired
    public GoogleOAuthService(
            CalendarIntegrationService calendarIntegrationService,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            @Value("${google.oauth.client-id:}") String clientId,
            @Value("${google.oauth.client-secret:}") String clientSecret,
            @Value("${google.oauth.redirect-uri:http://localhost:8080/api/oauth/google/callback}") String redirectUri,
            @Value("${google.oauth.scope:https://www.googleapis.com/auth/calendar}") String scope,
            @Value("${google.oauth.authorization-base-url:https://accounts.google.com/o/oauth2/v2/auth}") String authorizationBaseUrl,
            @Value("${google.oauth.token-url:https://oauth2.googleapis.com/token}") String tokenUrl) {
        this(
                calendarIntegrationService,
                restClientBuilderProvider.getIfAvailable(RestClient::builder).build(),
                clientId,
                clientSecret,
                redirectUri,
                scope,
                authorizationBaseUrl,
                tokenUrl);
    }

    GoogleOAuthService(
            CalendarIntegrationService calendarIntegrationService,
            RestClient restClient,
            String clientId,
            String clientSecret,
            String redirectUri,
            String scope,
            String authorizationBaseUrl,
            String tokenUrl) {
        this.calendarIntegrationService = calendarIntegrationService;
        this.restClient = restClient;
        this.clientId = emptyToNull(clientId);
        this.clientSecret = emptyToNull(clientSecret);
        this.redirectUri = emptyToNull(redirectUri);
        this.scope = emptyToNull(scope);
        this.authorizationBaseUrl = emptyToNull(authorizationBaseUrl);
        this.tokenUrl = emptyToNull(tokenUrl);
    }

    public Map<String, Object> beginAuthorization(String user, CalendarConnectionRequest request) {
        ensureConfigured();
        validatePendingRequest(user, request);

        String state = UUID.randomUUID().toString();
        pendingStates.put(state, new PendingGoogleConnection(user.trim().toLowerCase(), request));

        Map<String, Object> response = new HashMap<>();
        response.put("state", state);
        response.put("authorizationUrl", buildAuthorizationUrl(state));
        return response;
    }

    public Calendar completeAuthorization(String code, String state) {
        ensureConfigured();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code OAuth Google manquant");
        }
        PendingGoogleConnection pending = Optional.ofNullable(pendingStates.remove(state))
                .orElseThrow(() -> new IllegalArgumentException("State OAuth Google invalide ou expire"));

        String accessToken = exchangeCodeForAccessToken(code.trim());
        CalendarConnectionRequest request = pending.request();
        request.setProvider("GOOGLE");
        request.setOauthAccessToken(accessToken);
        return calendarIntegrationService.connect(pending.user(), request);
    }

    private String buildAuthorizationUrl(String state) {
        return authorizationBaseUrl
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(scope)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state);
    }

    private String exchangeCodeForAccessToken(String code) {
        String form = "code=" + encode(code)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("access_token") instanceof String accessToken) || accessToken.isBlank()) {
                throw new IllegalArgumentException("Google OAuth n'a pas retourne d'access token");
            }
            return accessToken;
        } catch (RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            throw new IllegalArgumentException(detail == null || detail.isBlank()
                    ? "Erreur OAuth Google: " + ex.getMessage()
                    : "Erreur OAuth Google: " + detail);
        }
    }

    private void validatePendingRequest(String user, CalendarConnectionRequest request) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("Le champ 'user' est requis");
        }
        if (request == null) {
            throw new IllegalArgumentException("Body manquant");
        }
        request.setProvider("GOOGLE");
        request.setOauthAccessToken("oauth-placeholder");
        calendarIntegrationService.connect("__oauth_validation__", request);
        calendarIntegrationService.removeConnection("__oauth_validation__", calendarIntegrationService
                .getConnectionsForUser("__oauth_validation__")
                .stream()
                .findFirst()
                .map(Calendar::getId)
                .orElse(null));
        request.setOauthAccessToken(null);
    }

    private void ensureConfigured() {
        if (clientId == null || clientSecret == null || redirectUri == null || scope == null
                || authorizationBaseUrl == null || tokenUrl == null) {
            throw new IllegalStateException(
                    "Google OAuth n'est pas configure. Definis GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET et GOOGLE_OAUTH_REDIRECT_URI.");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record PendingGoogleConnection(String user, CalendarConnectionRequest request) {
    }
}
