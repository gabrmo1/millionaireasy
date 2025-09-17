package br.com.bot_mexc.security.auth;

import br.com.bot_mexc.models.entities.Usuario;
import br.com.bot_mexc.models.enums.Role;
import br.com.bot_mexc.repositories.UsuarioRepository;
import br.com.bot_mexc.security.dtos.AuthenticationResponse;
import br.com.bot_mexc.security.dtos.LoginRequest;
import br.com.bot_mexc.security.dtos.RegisterRequest;
import br.com.bot_mexc.security.services.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public AuthenticationResponse register(RegisterRequest request, HttpServletResponse response) {
        repository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new ValidationException("Este e-mail já está em uso.");
        });

        var user = Usuario.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        repository.save(user);
        var jwtToken = jwtService.generateToken(user);

        addTokenToCookie(jwtToken, response);

        return AuthenticationResponse.builder().build();
    }

    public AuthenticationResponse authenticate(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);

        addTokenToCookie(jwtToken, response);

        return AuthenticationResponse.builder().build();
    }

    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void addTokenToCookie(String token, HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration / 1000));
        response.addCookie(cookie);
    }
}