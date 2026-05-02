package tn.esprit.arctic.derbelmicroservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(ApiResponseDTO.<Map<String, String>>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Erreur de validation")
                .data(fieldErrors)
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(ApiResponseDTO.<Void>builder()
                .status(status.value())
                .message(ex.getReason() != null ? ex.getReason() : "Erreur d'authentification/autorisation")
                .build());
    }

    /** Tri / pagination invalide (ex: sortBy sur un champ inexistant). */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        log.warn("Requête JPA / tri invalide : {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Paramètre de tri ou requête invalide : " + ex.getMostSpecificCause().getMessage())
                .build());
    }

    /** Contrainte SQL (clé étrangère, unique, etc.). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violation d'intégrité des données : {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.CONFLICT.value())
                .message("Impossible d'enregistrer : données liées ou contrainte violée.")
                .build());
    }

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleLazyInit(LazyInitializationException ex) {
        log.error("LazyInitializationException (souvent lié aux relations JPA hors transaction)", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Erreur de chargement des données associées. Contactez l'administrateur.")
                .build());
    }

    /**
     * URL inconnue (souvent espace en fin d'URL dans Postman → /records%20 au lieu de /records).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Ressource HTTP introuvable : {}", ex.getMessage());
        String hint = ex.getMessage() != null && ex.getMessage().contains("%20")
                ? " Vérifiez qu'il n'y a pas d'espace en fin d'URL dans Postman (ex: /records au lieu de /records )."
                : "";
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message("Aucun endpoint ne correspond à cette URL." + hint)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGlobalException(Exception ex) {
        log.error("Erreur non gérée", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.<Void>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Erreur interne du serveur : " + ex.getMessage())
                .build());
    }
}
