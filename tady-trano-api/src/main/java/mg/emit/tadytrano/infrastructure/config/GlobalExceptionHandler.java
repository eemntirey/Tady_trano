package mg.emit.tadytrano.infrastructure.config;

import mg.emit.tadytrano.application.exception.ApiException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> gererJsonIllisible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(construire(HttpStatus.BAD_REQUEST, "Corps JSON invalide ou incomplet"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> gererParametreInvalide(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(construire(HttpStatus.BAD_REQUEST, "Parametre invalide : " + ex.getName()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> gererViolationContrainte(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(construire(HttpStatus.BAD_REQUEST, "Parametres invalides"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> gererIntegriteDonnees(DataIntegrityViolationException ex) {
        LOG.warn("Conflit d'integrite en base lors du traitement d'une requete API", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(construire(HttpStatus.CONFLICT, "Operation impossible : les donnees entrent en conflit"));
    }

    private ProblemDetail construire(HttpStatus statut, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statut, message);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
