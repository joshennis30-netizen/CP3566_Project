package com.example.fraud.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import com.example.fraud.repo.UserRepository;
import org.springframework.security.crypto.password.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncode;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncode) {
        this.userRepo = userRepo;
        this.passwordEncode = passwordEncode;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        }

        return userRepo.findByUsername(username);
    }
}