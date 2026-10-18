package mg.emit.tadytrano.presentation.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.application.dto.AuthResponse;
import mg.emit.tadytrano.application.dto.LoginRequest;
import mg.emit.tadytrano.application.dto.ProfilResponse;
import mg.emit.tadytrano.application.dto.RegisterRequest;
import mg.emit.tadytrano.application.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/inscription")
    public ResponseEntity<AuthResponse> inscrire(@Valid @RequestBody RegisterRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.inscrire(demande));
    }

    @PostMapping("/connexion")
    public AuthResponse connecter(@Valid @RequestBody LoginRequest demande) {
        return authService.connecter(demande);
    }

    @GetMapping("/me")
    public ProfilResponse moi(@AuthenticationPrincipal UserDetails principal) {
        return authService.profil(principal.getUsername());
    }
}
