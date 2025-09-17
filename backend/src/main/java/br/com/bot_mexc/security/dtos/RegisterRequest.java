package br.com.bot_mexc.security.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @Email(message = "Por favor, insira um e-mail válido.")
    @NotBlank(message = "O campo e-mail é obrigatório.")
    private String email;
    private String password;
}