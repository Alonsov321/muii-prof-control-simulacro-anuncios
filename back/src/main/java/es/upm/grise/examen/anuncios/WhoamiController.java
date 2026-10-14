package es.upm.grise.examen.anuncios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoint de comprobación del control. Permite verificar desde fuera, sin credenciales de AWS,
 * que el despliegue usa el secreto de Secrets Manager y el token de Cognito.
 * Nunca devuelve la password, solo su huella (SHA-256).
 */
@RestController
public class WhoamiController {

    private final String examId;
    private final String password;

    public WhoamiController(@Value("${exam.id}") String examId, @Value("${password:}") String password) {
        this.examId = examId;
        this.password = password;
    }

    @GetMapping("/api/whoami")
    public Map<String, Object> whoami(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("examId", examId);
        respuesta.put("huella", huella(password));
        respuesta.put("usuario", jwt == null ? null : jwt.getClaimAsString("username"));
        List<String> grupos = jwt == null ? null : jwt.getClaimAsStringList("cognito:groups");
        respuesta.put("grupos", grupos == null ? List.of() : grupos);
        return respuesta;
    }

    static String huella(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
