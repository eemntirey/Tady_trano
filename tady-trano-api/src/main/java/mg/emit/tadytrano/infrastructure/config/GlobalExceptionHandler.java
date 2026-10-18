package mg.emit.tadytrano.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import mg.emit.tadytrano.application.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> gererApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatut()).body(construire(ex.getStatut(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> gererValidation(MethodArgumentNotValidException ex) {
        List<String> erreurs = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                .toList();
        ProblemDetail problem = construire(HttpStatus.BAD_REQUEST, "Donnees invalides");
        problem.setProperty("erreurs", erreurs);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ProblemDetail> gererIdentifiants(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(construire(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
    }

    private ProblemDetail construire(HttpStatus statut, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statut, message);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
