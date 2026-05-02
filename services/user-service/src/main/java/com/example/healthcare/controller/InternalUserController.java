package com.example.healthcare.controller;

import com.example.healthcare.dto.InternalRoleAssignmentRequestDTO;
import com.example.healthcare.dto.LookupIdsRequest;
import com.example.healthcare.dto.UserEmailResponse;
import com.example.healthcare.dto.UserResponseDTO;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Service-to-service API (protected by {@link com.example.healthcare.security.InternalApiKeyFilter}).
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping("/emails-by-ids")
    public ResponseEntity<List<UserEmailResponse>> emailsByIds(@Valid @RequestBody LookupIdsRequest request) {
        List<UserEmailResponse> out = new ArrayList<>();
        for (Long id : request.getIds()) {
            userRepository.findById(id).ifPresent(u -> out.add(toDto(u)));
        }
        return ResponseEntity.ok(out);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponseDTO> assignRoleViaPatch(
        @PathVariable Long id,
        @Valid @RequestBody InternalRoleAssignmentRequestDTO request
    ) {
        return assignRoleInternal(id, request);
    }

    @PostMapping("/{id}/role")
    public ResponseEntity<UserResponseDTO> assignRole(
        @PathVariable Long id,
        @Valid @RequestBody InternalRoleAssignmentRequestDTO request
    ) {
        return assignRoleInternal(id, request);
    }

    private ResponseEntity<UserResponseDTO> assignRoleInternal(
        Long id,
        InternalRoleAssignmentRequestDTO request
    ) {
        if (!"PHARMACIST".equalsIgnoreCase(request.getRole())) {
            throw new IllegalArgumentException("Only PHARMACIST role can be assigned through this endpoint");
        }
        return ResponseEntity.ok(userService.assignRoleInternally(id, request.getRole()));
    }

    private static UserEmailResponse toDto(User u) {
        return UserEmailResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .build();
    }
}
