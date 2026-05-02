package tn.esprit.arctic.derbelmicroservice.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceIdentityClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.user-service.base-url:http://localhost:8081}")
    private String userServiceBaseUrl;

    public Long resolveUserIdFromBearer(String bearerHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerHeader);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<UserMeResponse> response = restTemplate.exchange(
                userServiceBaseUrl + "/api/users/me",
                HttpMethod.GET,
                request,
                UserMeResponse.class
        );

        UserMeResponse body = response.getBody();
        if (body == null || body.getId() == null) {
            throw new IllegalArgumentException("Cannot resolve user id from user-service /api/users/me");
        }
        return body.getId();
    }

    @Getter
    @Setter
    public static class UserMeResponse {
        private Long id;
    }
}
