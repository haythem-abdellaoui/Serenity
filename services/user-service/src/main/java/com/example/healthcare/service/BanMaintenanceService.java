package com.example.healthcare.service;

import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BanMaintenanceService {
    private final UserRepository userRepository;

    @Transactional
    public int unbanExpiredUsers() {
        Date now = new Date();
        List<User> eligibleUsers =
                userRepository.findByIsPermanentlyBannedFalseAndBannedUntilLessThanEqual(now);
        if (eligibleUsers.isEmpty()) {
            return 0;
        }

        eligibleUsers.forEach(user -> {
            user.setBannedUntil(null);
            user.setIsPermanentlyBanned(false);
        });
        userRepository.saveAll(eligibleUsers);
        log.info("Auto-unbanned {} users with expired bans", eligibleUsers.size());
        return eligibleUsers.size();
    }
}
