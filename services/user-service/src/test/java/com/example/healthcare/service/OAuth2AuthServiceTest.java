package com.example.healthcare.service;

import com.example.healthcare.dto.AuthResponseDTO;
import com.example.healthcare.entity.AuthProvider;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.exception.InvalidCredentialsException;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @Spy @InjectMocks private OAuth2AuthService service;

    @Test
    void loginWithFacebook_whenEmailMissing_throws() {
        Map<String, Object> fb = new HashMap<>();
        fb.put("first_name", "A");
        doReturn(fb).when(service).verifyFacebookToken("token");

        assertThatThrownBy(() -> service.loginWithFacebook("token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must have an email");
    }

    @Test
    void loginWithFacebook_whenExistingUserInactive_throwsInvalidCredentials() {
        Map<String, Object> fb = Map.of("email", "a@b.com", "first_name", "A", "last_name", "B");
        doReturn(fb).when(service).verifyFacebookToken("token");

        User existing = new User();
        existing.setEmail("a@b.com");
        existing.setIsActive(false);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.loginWithFacebook("token"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithFacebook_whenExistingLocalUser_updatesProviderAndReturnsToken() {
        Map<String, Object> fb = Map.of("email", "a@b.com", "first_name", "A", "last_name", "B");
        doReturn(fb).when(service).verifyFacebookToken("token");

        User existing = new User();
        existing.setId(7L);
        existing.setEmail("a@b.com");
        existing.setIsActive(true);
        existing.setIsPermanentlyBanned(false);
        existing.setAuthProvider(AuthProvider.LOCAL);
        existing.setRole(Role.PATIENT);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));
        when(jwtTokenProvider.generateToken(eq("a@b.com"), eq("ROLE_PATIENT"), eq(7L))).thenReturn("JWT");

        AuthResponseDTO resp = service.loginWithFacebook("token");

        assertThat(resp.getAccessToken()).isEqualTo("JWT");
        verify(userRepository).save(existing);
        assertThat(existing.getAuthProvider()).isEqualTo(AuthProvider.FACEBOOK);
    }
}

