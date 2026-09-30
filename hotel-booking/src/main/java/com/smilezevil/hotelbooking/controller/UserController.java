package com.smilezevil.hotelbooking.controller;

import com.smilezevil.hotelbooking.annotation.CurrentUsername;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class UserController {

    @GetMapping
    public Map<String, String> whoAmI(@CurrentUsername String username) {
        return Map.of(
                "username", username,
                "greeting", "Вітаємо, " + username + "!"
        );
    }
}