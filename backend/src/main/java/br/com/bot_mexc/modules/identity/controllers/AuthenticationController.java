package br.com.bot_mexc.modules.identity.controllers;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.modules.identity.entities.*;
import br.com.bot_mexc.modules.identity.dtos.*;
import br.com.bot_mexc.modules.identity.repositories.*;
import br.com.bot_mexc.modules.identity.services.*;
import br.com.bot_mexc.modules.identity.configs.*;

import br.com.bot_mexc.modules.identity.entities.Usuario;
import br.com.bot_mexc.modules.identity.dtos.AuthenticationResponse;
import br.com.bot_mexc.modules.identity.dtos.LoginRequest;
import br.com.bot_mexc.modules.identity.dtos.RegisterRequest;
import br.com.bot_mexc.modules.identity.dtos.UsuarioDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(service.register(request, response));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(service.authenticate(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        service.logout(response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> getCurrentUser(@AuthenticationPrincipal Usuario userDetails) {
        return ResponseEntity.ok(UsuarioDTO.fromEntity(userDetails));
    }
}