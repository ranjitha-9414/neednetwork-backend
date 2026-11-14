
package com.example.neednetwork.service;

import com.example.neednetwork.entity.AccountActivity;
import com.example.neednetwork.entity.Resource;
import com.example.neednetwork.entity.User;
import com.example.neednetwork.repository.AccountActivityRepository;
import com.example.neednetwork.repository.ResourceRepository;
import com.example.neednetwork.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final AccountActivityRepository accountActivityRepository;

    public UserService(
            UserRepository userRepository,
            ResourceRepository resourceRepository,
            AccountActivityRepository accountActivityRepository) {

        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.accountActivityRepository = accountActivityRepository;
    }

    // Safe user response: never expose password hashes
    public record UserResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            String role,
            boolean emailVerified,
            boolean active,
            LocalDateTime createdAt
    ) {}

    // Account activity response
    public record AccountActivityResponse(
            Long id,
            Long targetUserId,
            String targetUserName,
            String targetUserEmail,
            String adminEmail,
            boolean previousStatus,
            boolean newStatus,
            String action,
            LocalDateTime createdAt
    ) {}

    // Get all users or search by name/email
    public List<UserResponse> getAllUsers(String search) {

        List<User> users;

        if (search == null || search.isBlank()) {
            users = userRepository.findAll();
        } else {
            String keyword = search.trim();

            users = userRepository
                    .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            keyword,
                            keyword
                    );
        }

        return users.stream()
                .map(this::toUserResponse)
                .toList();
    }

    // Get one user's details
    public UserResponse getUserById(Long id) {

        User user = findUser(id);

        return toUserResponse(user);
    }

    // Get resources posted by a particular user
    public List<Resource> getUserResources(Long id) {

        User user = findUser(id);

        return resourceRepository
                .findByOwnerEmailIgnoreCase(user.getEmail());
    }

    // Activate or deactivate a user's account
    @Transactional
    public UserResponse updateAccountStatus(
            Long id,
            boolean active,
            String adminEmail) {

        User user = findUser(id);

        // Prevent administrator from managing their own account
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You cannot change your own account status."
            );
        }

        // Protect administrator accounts
        if (user.getRole() == User.Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator accounts cannot be managed here."
            );
        }

        boolean previousStatus = user.isActive();

        // Avoid unnecessary database updates and history records
        if (previousStatus == active) {
            return toUserResponse(user);
        }

        // Update account status
        user.setActive(active);

        User savedUser = userRepository.save(user);

        // Record account activity
        AccountActivity activity = new AccountActivity();

        activity.setTargetUserId(savedUser.getId());
        activity.setTargetUserName(savedUser.getFullName());
        activity.setTargetUserEmail(savedUser.getEmail());
        activity.setAdminEmail(adminEmail);
        activity.setPreviousStatus(previousStatus);
        activity.setNewStatus(active);

        activity.setAction(
                active ? "ACTIVATED" : "DEACTIVATED"
        );

        accountActivityRepository.save(activity);

        return toUserResponse(savedUser);
    }

    // Get account activity history for one user
    @Transactional(readOnly = true)
    public List<AccountActivityResponse> getAccountActivityHistory(
            Long userId) {

        findUser(userId);

        return accountActivityRepository
                .findByTargetUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toActivityResponse)
                .toList();
    }

    // Get account activity history for all users
    @Transactional(readOnly = true)
    public List<AccountActivityResponse> getAllAccountActivityHistory() {

        return accountActivityRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toActivityResponse)
                .toList();
    }

    // Find user by ID
    private User findUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));
    }

    // Convert User entity to safe response
    private UserResponse toUserResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    // Convert AccountActivity entity to response
    private AccountActivityResponse toActivityResponse(
            AccountActivity activity) {

        return new AccountActivityResponse(
                activity.getId(),
                activity.getTargetUserId(),
                activity.getTargetUserName(),
                activity.getTargetUserEmail(),
                activity.getAdminEmail(),
                activity.isPreviousStatus(),
                activity.isNewStatus(),
                activity.getAction(),
                activity.getCreatedAt()
        );
    }
}
