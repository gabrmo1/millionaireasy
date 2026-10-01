package br.com.bot_mexc.modules.oms.controllers;

import br.com.bot_mexc.modules.oms.dtos.KillSwitchResponseDTO;
import br.com.bot_mexc.modules.oms.services.KillSwitchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/operacoes")
@RequiredArgsConstructor
public class KillSwitchController {

    private final KillSwitchService killSwitchService;

    @PostMapping("/kill-switch")
    public ResponseEntity<KillSwitchResponseDTO> acionarKillSwitch() {
        return ResponseEntity.ok(killSwitchService.acionarKillSwitchGlobal());
    }
}
