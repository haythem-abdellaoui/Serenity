package com.example.healthcare.security;

import com.example.healthcare.security.userdetails.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    public boolean isOwner(Authentication authentication, Long userId) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId().equals(userId);
    }
}
