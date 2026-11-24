package com.example.pokemedoc.account;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService implements UserDetailsService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AccountService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(u.getUsername())   // canonical casing from the database
                .password(u.getPasswordHash())
                .roles("PLAYER")
                .build();
    }

    /** @throws UsernameTakenException if the name is already used (case-insensitive) */
    public void register(String username, String rawPassword) {
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new UsernameTakenException();
        }
        try {
            users.saveAndFlush(new AppUser(username, encoder.encode(rawPassword)));
        } catch (DataIntegrityViolationException e) {   // lost a race with another registration
            throw new UsernameTakenException();
        }
    }

    public static class UsernameTakenException extends RuntimeException {
        public UsernameTakenException() {
            super("Username already taken");
        }
    }
}
