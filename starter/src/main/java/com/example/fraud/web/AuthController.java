package com.example.fraud.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import com.example.fraud.repo.UserRepository;
import com.example.fraud.model.User;
import org.springframework.security.crypto.password.*;

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
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userRepo.findByUsername(username);
        return user;
    }
}