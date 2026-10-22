package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.User;
import com.bach.vetclinic.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final long tokenMinutes;
    private final com.bach.vetclinic.repository.RevokedTokenRepository revokedTokens;
    private final com.bach.vetclinic.repository.UserRepository users;

    public AuthController(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder,
                          @Value("${app.jwt.expiration-minutes:60}") long tokenMinutes, com.bach.vetclinic.repository.RevokedTokenRepository revokedTokens, com.bach.vetclinic.repository.UserRepository users) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.tokenMinutes = tokenMinutes;
        this.revokedTokens = revokedTokens;
        this.users = users;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        User user = ((UserPrincipal) authentication.getPrincipal()).getUser();

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("vetclinic")
                .issuedAt(now)
                .id(java.util.UUID.randomUUID().toString())
                .expiresAt(now.plusSeconds(tokenMinutes * 60))
                .subject(user.getEmail())
                .claim("name", user.getFullName())
                .claim("roles", user.getRole().name())
                .claim("credentials", com.bach.vetclinic.security.CredentialStamp.of(user.getPasswordHash()))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", tokenMinutes, user.getEmail(), user.getFullName(), user.getRole().name());
    }

    @GetMapping("/me")
    public AccountResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User user=users.findByEmailIgnoreCase(jwt.getSubject()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new AccountResponse(user.getId(),user.getEmail(),user.getFullName(),user.getRole().name(),user.getVet()==null?null:user.getVet().getId());
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.transaction.annotation.Transactional
    public void logout(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt){
        revokedTokens.deleteByExpiresAtBefore(Instant.now());
        revokedTokens.save(new com.bach.vetclinic.entity.RevokedToken(com.bach.vetclinic.security.CredentialStamp.of(jwt.getTokenValue()),jwt.getExpiresAt()));
    }
    public record AccountResponse(Long id,String email,String fullName,String role,Long vetId){}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresInMinutes,
                                String email, String fullName, String role) {}
}
