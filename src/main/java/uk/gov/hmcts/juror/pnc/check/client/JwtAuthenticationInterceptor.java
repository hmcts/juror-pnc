package uk.gov.hmcts.juror.pnc.check.client;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import uk.gov.hmcts.juror.standard.config.JwtSecurityConfig;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;

import java.io.IOException;
import java.security.Key;
import java.util.Optional;

public class JwtAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private final JwtService jwtService;
    private final JwtSecurityConfig config;

    public JwtAuthenticationInterceptor(JwtService jwtService, JwtSecurityConfig config) {
        this.jwtService = jwtService;
        this.config = config;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        if (!headers.containsHeader(HttpHeaders.AUTHORIZATION)) {
            headers.add(HttpHeaders.AUTHORIZATION,
                Optional.ofNullable(config.getAuthenticationPrefix()).orElse("") + generateJwt());
        }
        return execution.execute(request, body);
    }

    private String generateJwt() {
        return jwtService.generateJwtToken(
            null,
            config.getIssuer(),
            config.getSubject(),
            config.getTokenValidity(),
            getSigningKey(),
            config.getClaims()
        );
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(config.getSecret()));
    }
}
