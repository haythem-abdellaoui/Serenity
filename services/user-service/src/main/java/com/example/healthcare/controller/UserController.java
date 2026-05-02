package com.example.healthcare.controller;

import com.example.healthcare.dto.ProfileUpdateDTO;
import com.example.healthcare.dto.UserDTO;
import com.example.healthcare.dto.UserRequestDTO;
import com.example.healthcare.dto.UserResponseDTO;
import com.example.healthcare.entity.AuthResponse;
import com.example.healthcare.security.jwt.JwtTokenProvider;
import com.example.healthcare.dto.BanUserRequestDTO;
import com.example.healthcare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserByEmail(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateProfile(Authentication authentication,
                                                          @Valid @RequestBody ProfileUpdateDTO request) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<UserResponseDTO> uploadAvatar(Authentication authentication,
                                                        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(authentication.getName(), file));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id,
                                                       @Valid @RequestBody UserRequestDTO request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> banUser(@PathVariable Long id, @Valid @RequestBody BanUserRequestDTO request) {
        userService.banUser(id, request.getDuration());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update-role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> updateUserRole(
            Authentication authentication,
            @RequestParam String role) {

        String email = authentication.getName();
        UserResponseDTO updatedUser = userService.updateUserRole(email, role);
        String newToken = jwtTokenProvider.generateToken(updatedUser.getEmail(), "ROLE_" + role, updatedUser.getId());

        // Return AuthResponse with new token
        AuthResponse response = new AuthResponse(
                newToken,           // accessToken with updated role
                "Bearer",           // tokenType
                updatedUser.getId(),
                email,
                role                // Updated role
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public List<UserDTO> searchUsers(@RequestParam String q) {
        return userService.searchUsers(q);
    }

    @GetMapping("/names")
    public ResponseEntity<List<UserDTO>> getUsersNames(@RequestParam List<Long> ids) {
        List<UserDTO> names = userService.getUsersNamesByIds(ids);
        return ResponseEntity.ok(names);
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<UserResponseDTO>> getDoctors() {
        return ResponseEntity.ok(userService.getDoctors());
    }

    @GetMapping("/patients")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<List<UserResponseDTO>> getPatients() {
        return ResponseEntity.ok(userService.getPatients());
    }
}
