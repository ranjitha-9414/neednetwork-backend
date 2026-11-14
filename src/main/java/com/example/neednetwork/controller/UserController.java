
package com.example.neednetwork.controller;

import com.example.neednetwork.entity.Resource;
import com.example.neednetwork.service.UserService;
import com.example.neednetwork.service.UserService.UserResponse;
import com.example.neednetwork.service.UserService.AccountActivityResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Request body for account activation/deactivation
    public record AccountStatusRequest(boolean active) {}

    // GET: View all users or search by name/email
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(
                userService.getAllUsers(search)
        );
    }

    // GET: View all account activity history
    @GetMapping("/activity-history")
    public ResponseEntity<List<AccountActivityResponse>>
            getAllAccountActivityHistory() {

        return ResponseEntity.ok(
                userService.getAllAccountActivityHistory()
        );
    }

    // GET: View individual user details
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    // GET: View resources posted by a user
    @GetMapping("/{id}/resources")
    public ResponseEntity<List<Resource>> getUserResources(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                userService.getUserResources(id)
        );
    }

    // PUT: Activate or deactivate an account
    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateAccountStatus(
            @PathVariable Long id,
            @RequestBody AccountStatusRequest request,
            Authentication authentication) {

        UserResponse response = userService.updateAccountStatus(
                id,
                request.active(),
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }

    // GET: View account history for one particular user
    @GetMapping("/{id}/activity-history")
    public ResponseEntity<List<AccountActivityResponse>>
            getAccountActivityHistory(@PathVariable Long id) {

        return ResponseEntity.ok(
                userService.getAccountActivityHistory(id)
        );
    }
}
