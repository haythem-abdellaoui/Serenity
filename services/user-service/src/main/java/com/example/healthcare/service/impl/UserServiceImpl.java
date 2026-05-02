package com.example.healthcare.service.impl;

import com.example.healthcare.dto.*;
import com.example.healthcare.entity.BanDuration;
import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import com.example.healthcare.entity.UserProfile;
import com.example.healthcare.exception.EmailAlreadyExistsException;
import com.example.healthcare.exception.InvalidCredentialsException;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.exception.UserBannedException;
import com.example.healthcare.mapper.UserMapper;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.jwt.JwtTokenProvider;
import com.example.healthcare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private static final DateTimeFormatter BAN_UNTIL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    @Override
    public AuthResponseDTO registerUser(UserRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setIsPermanentlyBanned(false);
        user.setBannedUntil(null);

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(Role.valueOf(request.getRole()));
        } else {
            user.setRole(Role.PATIENT);
        }

        UserProfile profile = UserProfile.builder()
                .user(user)
                .preferredLanguage("en")
                .isAnonymous(false)
                .build();
        user.setProfile(profile);

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        return AuthResponseDTO.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public UserResponseDTO updateUserRole(String email, String role) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Role requestedRole = parseRole(role);
        if (requestedRole == Role.PHARMACIST) {
            throw new IllegalArgumentException("PHARMACIST role cannot be self-assigned");
        }

        user.setRole(requestedRole);
        userRepository.save(user);

        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO assignRoleInternally(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setRole(parseRole(role));
        userRepository.save(user);
        return userMapper.toResponseDTO(user);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(InvalidCredentialsException::new);
            ensureNotBanned(user);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            String token = jwtTokenProvider.generateToken(authentication);

            return AuthResponseDTO.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .build();
        } catch (DisabledException e) {
            throw new InvalidCredentialsException("Your account has been deactivated. Please contact an administrator.");
        } catch (LockedException e) {
            throw new UserBannedException("Your account is currently banned.");
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userMapper.toResponseDTOList(userRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateProfile(String email, ProfileUpdateDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getInsuranceCompany() != null) user.setInsuranceCompany(request.getInsuranceCompany());

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().user(user).build();
            user.setProfile(profile);
        }
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getAvatar() != null) profile.setAvatar(request.getAvatar());
        if (request.getPreferredLanguage() != null) profile.setPreferredLanguage(request.getPreferredLanguage());
        if (request.getIsAnonymous() != null) profile.setIsAnonymous(request.getIsAnonymous());

        userRepository.save(user);
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO uploadAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is required");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are allowed");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String original = file.getOriginalFilename();
        String safeName = StringUtils.hasText(original)
                ? original.replace("\\", "_").replace("/", "_").replace("..", "_")
                : "avatar";
        String filename = UUID.randomUUID() + "_" + safeName;

        Path dir = Paths.get(uploadDir, "avatars", String.valueOf(user.getId()));
        Path target = dir.resolve(filename).normalize();
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toAbsolutePath().toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save avatar");
        }

        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String avatarUrl = base + "/uploads/avatars/" + user.getId() + "/" + filename;

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().user(user).build();
            user.setProfile(profile);
        }
        profile.setAvatar(avatarUrl);
        userRepository.save(user);
        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        userMapper.updateEntityFromDTO(request, user);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(Role.valueOf(request.getRole()));
        }

        userRepository.save(user);
        return userMapper.toResponseDTO(user);
    }

    @Override
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    public void banUser(Long id, BanDuration duration) {
        if (duration == null) {
            throw new IllegalArgumentException("Ban duration is required");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        Date now = new Date();
        user.setIsPermanentlyBanned(duration.isPermanent());
        user.setBannedUntil(duration.resolveBannedUntil(now));
        userRepository.save(user);
    }

    @Override
    public void unbanUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsPermanentlyBanned(false);
        user.setBannedUntil(null);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
    }

    public List<UserDTO> searchUsers(String query) {
        return userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query)
                .stream()
                .map(user -> new UserDTO(user.getId(), user.getFirstName(), user.getLastName()))
                .toList();
    }

    public List<UserDTO> getUsersNamesByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .map(user -> new UserDTO(user.getId(), user.getFirstName(), user.getLastName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getDoctors() {
        List<User> doctors = userRepository.findByRole(Role.DOCTOR);
        return userMapper.toResponseDTOList(doctors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getPatients() {
        List<User> patients = userRepository.findByRole(Role.PATIENT);
        return userMapper.toResponseDTOList(patients);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLookupDTO> lookupDoctors() {
        return userRepository.findByRoleAndIsActiveTrueOrderByLastNameAscFirstNameAsc(Role.DOCTOR).stream()
                .map(this::toLookupDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLookupDTO> lookupPatients(String firstName, String lastName) {
        return userRepository.findByRoleAndIsActiveTrueOrderByLastNameAscFirstNameAsc(Role.PATIENT).stream()
                .filter(u -> matchesOptionalNameFilter(u.getFirstName(), firstName))
                .filter(u -> matchesOptionalNameFilter(u.getLastName(), lastName))
                .map(this::toLookupDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLookupDTO> lookupUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<User> found = userRepository.findAllById(ids);
        Map<Long, User> byId = found.stream().collect(Collectors.toMap(User::getId, u -> u));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(this::toLookupDto)
                .toList();
    }

    private boolean matchesOptionalNameFilter(String fieldValue, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        if (query.length() < 2) {
            return true;
        }
        if (fieldValue == null) {
            return false;
        }
        return fieldValue.toLowerCase().contains(query.toLowerCase());
    }

    private UserLookupDTO toLookupDto(User user) {
        return UserLookupDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid role value: " + role);
        }
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
            String formatted = BAN_UNTIL_FORMATTER.format(bannedUntil.toInstant());
            throw new UserBannedException("Your account is banned until " + formatted + ".");
        }

        user.setBannedUntil(null);
        user.setIsPermanentlyBanned(false);
        userRepository.save(user);
    }
}
