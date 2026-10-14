package es.upm.grise.examen.anuncios;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    public record NuevoMensaje(String texto) { }

    private final MensajeRepository repository;

    public MensajeController(MensajeRepository repository) {
        this.repository = repository;
    }

    /** Cualquier usuario autenticado puede leer los anuncios. */
    @GetMapping
    public List<Mensaje> listar() {
        return repository.findAllByOrderByFechaDesc();
    }

    /** Solo usuarios del grupo admin. El autor sale del token, no del cuerpo. */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody NuevoMensaje nuevo, @AuthenticationPrincipal Jwt jwt) {
        if (nuevo == null || nuevo.texto() == null || nuevo.texto().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El texto es obligatorio"));
        }
        Mensaje mensaje = new Mensaje();
        mensaje.setAutor(jwt.getClaimAsString("username"));
        mensaje.setTexto(nuevo.texto());
        mensaje.setFecha(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(mensaje));
    }
}
