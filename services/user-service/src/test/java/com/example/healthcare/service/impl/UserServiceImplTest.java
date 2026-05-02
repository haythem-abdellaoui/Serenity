package com.example.healthcare.service.impl;

import com.example.healthcare.dto.AuthResponseDTO;
import com.example.healthcare.dto.LoginRequestDTO;
import com.example.healthcare.dto.UserRequestDTO;
import com.example.healthcare.dto.UserResponseDTO;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
import com.example.healthcare.exception.EmailAlreadyExistsException;
import com.example.healthcare.exception.InvalidCredentialsException;
import com.example.healthcare.exception.UserBannedException;
import com.example.healthcare.mapper.UserMapper;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private UserServiceImpl userService;

    @AfterEach
    void resetReflectionFields() {
        ReflectionTestUtils.setField(userService, "uploadDir", "uploads");
        ReflectionTestUtils.setField(userService, "publicBaseUrl", "http://localhost:8081");
    }

    @Test
    void registerUser_whenEmailAlreadyExists_throws() {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail("a@b.com");
        request.setPassword("pw");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void registerUser_whenRoleBlank_defaultsToPatient() {
        UserRequestDTO request = new UserRequestDTO();
        request.setEmail("a@b.com");
        request.setPassword("pw");
        request.setRole("  ");

        User mapped = new User();
        mapped.setEmail("a@b.com");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(passwordEncoder.encode("pw")).thenReturn("ENC");

        Authentication auth = new UsernamePasswordAuthenticationToken("a@b.com", "pw");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("JWT");

        AuthResponseDTO response = userService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.PATIENT);
        assertThat(userCaptor.getValue().getProfile()).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("JWT");
    }

    @Test
    void login_whenBadCredentials_throwsInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("a@b.com");
        request.setPassword("wrong");

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setIsPermanentlyBanned(false);
        user.setBannedUntil(null);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenUserPermanentlyBanned_throwsUserBanned() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("a@b.com");
        request.setPassword("pw");

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setIsPermanentlyBanned(true);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserBannedException.class);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void uploadAvatar_whenFileMissing_throwsBadRequest() {
        assertThatThrownBy(() -> userService.uploadAvatar("a@b.com", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadAvatar_whenNotImage_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> userService.uploadAvatar("a@b.com", file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadAvatar_whenValidImage_savesAndUpdatesProfile(@TempDir Path tmp) throws IOException {
        ReflectionTestUtils.setField(userService, "uploadDir", tmp.toString());
        ReflectionTestUtils.setField(userService, "publicBaseUrl", "http://localhost:8081/");

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        User user = new User();
        user.setId(10L);
        user.setEmail("a@b.com");
        user.setProfile(UserProfile.builder().user(user).build());

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        UserResponseDTO dto = new UserResponseDTO();
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(dto);

        UserResponseDTO result = userService.uploadAvatar("a@b.com", file);

        assertThat(result).isSameAs(dto);
        verify(userRepository).save(user);
        assertThat(user.getProfile().getAvatar()).contains("/uploads/avatars/10/");

        // Ensure file was written somewhere under tmp/avatars/10/
        Path avatarsDir = tmp.resolve("avatars").resolve("10");
        assertThat(Files.exists(avatarsDir)).isTrue();
        assertThat(Files.list(avatarsDir).count()).isEqualTo(1);
    }
}

