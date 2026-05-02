package com.example.healthcare.service;

import com.example.healthcare.dto.DoctorResponseDTO;
import com.example.healthcare.dto.UserResponseDTO;
import com.example.healthcare.entity.Doctor;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.mapper.DoctorMapper;
import com.example.healthcare.repository.DoctorRepository;
import com.example.healthcare.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private UserRepository userRepository;
    @Mock private RedisPublisher redisPublisher;
    @Mock private DoctorMapper doctorMapper;
    @Mock private UserService userService;

    @InjectMocks private DoctorService doctorService;

    @AfterEach
    void cleanupUploads() throws IOException {
        Path uploads = Paths.get("uploads");
        if (!Files.exists(uploads)) return;
        try (Stream<Path> walk = Files.walk(uploads)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        }
        Files.deleteIfExists(uploads);
    }

    @Test
    void createDoctorForExistingUser_whenUserNotDoctor_throws() {
        User u = new User();
        u.setId(1L);
        u.setRole(Role.PATIENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        MultipartFile img = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> doctorService.createDoctorForExistingUser(1L, "cardio", img))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not have the DOCTOR role");
    }

    @Test
    void createDoctorForExistingUser_happyPath_publishesEvent() throws IOException {
        User u = new User();
        u.setId(2L);
        u.setEmail("doc@x.com");
        u.setPassword("pw");
        u.setRole(Role.DOCTOR);
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));

        Doctor saved = new Doctor();
        saved.setId(2L);
        saved.setEmail("doc@x.com");
        when(doctorRepository.save(any(Doctor.class))).thenReturn(saved);

        DoctorResponseDTO dto = DoctorResponseDTO.builder()
                .id(2L)
                .email("doc@x.com")
                .build();
        when(doctorMapper.toDTO(saved)).thenReturn(dto);
        when(userService.uploadAvatar(eq("doc@x.com"), any(MultipartFile.class))).thenReturn(new UserResponseDTO());

        MultipartFile img = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});

        DoctorResponseDTO result = doctorService.createDoctorForExistingUser(2L, "cardio", img);

        assertThat(result).isSameAs(dto);
        verify(userRepository).delete(u);
        verify(redisPublisher).publishDoctorEvent(dto);
        assertThat(Files.exists(Paths.get("uploads"))).isTrue();
    }
}

