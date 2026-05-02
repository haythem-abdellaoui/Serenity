package com.example.healthcare.controller;

import com.example.healthcare.dto.LookupIdsRequest;
import com.example.healthcare.dto.UserLookupDTO;
import com.example.healthcare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/lookup")
@RequiredArgsConstructor
public class UserLookupController {

    private final UserService userService;

    /**
     * Active doctors for appointment booking. Lives under /api/users/lookup so the API gateway
     * (Path=/api/users/**) forwards here; a separate /api/lookup/** route was unreliable.
     */
    @GetMapping("/doctors")
    public ResponseEntity<List<UserLookupDTO>> lookupDoctors() {
        return ResponseEntity.ok(userService.lookupDoctors());
    }

    /**
     * Patients for doctor scheduling: omit query params to list all active patients (sorted by name),
     * or pass firstName / lastName (min 2 chars each when used) to filter.
     */
    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<List<UserLookupDTO>> lookupPatients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        return ResponseEntity.ok(userService.lookupPatients(firstName, lastName));
    }

    /** Resolve id → name for appointment-service (and others); authenticated; max 100 ids. */
    @PostMapping("/names")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserLookupDTO>> lookupNames(@Valid @RequestBody LookupIdsRequest request) {
        return ResponseEntity.ok(userService.lookupUsersByIds(request.getIds()));
    }
}
