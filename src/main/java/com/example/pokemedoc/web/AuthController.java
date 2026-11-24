package com.example.pokemedoc.web;

import com.example.pokemedoc.account.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

@Controller
public class AuthController {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,20}");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           Model model) {
        String name = username.trim();
        model.addAttribute("username", name);

        if (!USERNAME.matcher(name).matches()) {
            model.addAttribute("error", "register.error.username");
            return "register";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            model.addAttribute("error", "register.error.password");
            return "register";
        }
        try {
            accounts.register(name, password);
        } catch (AccountService.UsernameTakenException e) {
            model.addAttribute("error", "register.error.taken");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
