package com.example.healthcare.service;

import com.example.healthcare.dto.AuthResponseDTO;
import com.example.healthcare.entity.*;
import com.example.healthcare.exception.InvalidCredentialsException;
import com.example.healthcare.exception.UserBannedException;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.jwt.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OAuth2AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.google.client-id}")
    private String googleClientId;

    @Value("${app.oauth2.facebook.app-id}")
    private String facebookAppId;

    @Value("${app.oauth2.facebook.app-secret}")
    private String facebookAppSecret;

    public AuthResponseDTO loginWithGoogle(String idTokenString) {
        GoogleIdToken.Payload payload = verifyGoogleToken(idTokenString);

        String email = payload.getEmail();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");

        if (firstName == null) firstName = email.split("@")[0];
        if (lastName == null) lastName = "";

        User user = findOrCreateOAuthUser(email, firstName, lastName, AuthProvider.GOOGLE);
        return buildAuthResponse(user);
    }

    public AuthResponseDTO loginWithFacebook(String accessToken) {
        Map<String, Object> fbUser = verifyFacebookToken(accessToken);

        String email = (String) fbUser.get("email");
        String firstName = (String) fbUser.get("first_name");
        String lastName = (String) fbUser.get("last_name");

        if (email == null) {
            throw new RuntimeException("Facebook account must have an email address");
        }
        if (firstName == null) firstName = email.split("@")[0];
        if (lastName == null) lastName = "";

        User user = findOrCreateOAuthUser(email, firstName, lastName, AuthProvider.FACEBOOK);
        return buildAuthResponse(user);
    }

    GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidCredentialsException();
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidCredentialsException();
        }
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> verifyFacebookToken(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://graph.facebook.com/me?fields=id,email,first_name,last_name&access_token=" + accessToken;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Facebook token", e);
        }
    }

    private User findOrCreateOAuthUser(String email, String firstName, String lastName, AuthProvider provider) {
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.getIsActive()) {
                throw new InvalidCredentialsException("Your account has been deactivated. Please contact an administrator.");
            }
            ensureNotBanned(user);
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                user.setAuthProvider(provider);
                userRepository.save(user);
            }
            return user;
        }

        User user = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password(null)
                .role(Role.PATIENT)
                .authProvider(provider)
                .isActive(true)
                .isPermanentlyBanned(false)
                .bannedUntil(null)
                .build();

        UserProfile profile = UserProfile.builder()
                .user(user)
                .preferredLanguage("en")
                .isAnonymous(false)
                .build();
        user.setProfile(profile);

        return userRepository.save(user);
    }

    private AuthResponseDTO buildAuthResponse(User user) {
        String role = "ROLE_" + user.getRole().name();
        String token = jwtTokenProvider.generateToken(user.getEmail(), role, user.getId());

        return AuthResponseDTO.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private void ensureNotBanned(User user) {
        if (Boolean.TRUE.equals(user.getIsPermanentlyBanned())) {
            throw new UserBannedException("Your account has been permanently banned.");
        }

        Date bannedUntil = user.getBannedUntil();
        if (bannedUntil == null) {
            return;
        }

        Date now = new Date();
        if (bannedUntil.after(now)) {
            throw new UserBannedException("Your account is currently banned.");
        }

        user.setBannedUntil(null);
        user.setIsPermanentlyBanned(false);
        userRepository.save(user);
    }
}
