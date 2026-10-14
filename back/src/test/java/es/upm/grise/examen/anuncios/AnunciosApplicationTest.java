package es.upm.grise.examen.anuncios;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnunciosApplicationTest {

    private static final String CUERPO = "{\"texto\":\"hola\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listarSinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/mensajes")).andExpect(status().isUnauthorized());
    }

    @Test
    void listarConCualquierUsuarioAutenticadoDa200() throws Exception {
        mockMvc.perform(get("/api/mensajes").with(jwt().jwt(j -> j.claim("username", "corrector"))))
            .andExpect(status().isOk());
    }

    @Test
    void crearSinTokenDa401() throws Exception {
        mockMvc.perform(post("/api/mensajes").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void crearAutenticadoPeroFueraDelGrupoAdminDa403() throws Exception {
        mockMvc.perform(post("/api/mensajes")
                .with(jwt().jwt(j -> j.claim("username", "corrector")))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
            .andExpect(status().isForbidden());
    }

    @Test
    void crearDentroDelGrupoAdminDa201() throws Exception {
        mockMvc.perform(post("/api/mensajes")
                .with(jwt().jwt(j -> j.claim("username", "corrector"))
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.autor").value("corrector"));
    }

    @Test
    void elGrupoAdminDeCognitoSeConvierteEnRolAdmin() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "none")
            .claim("username", "corrector").claim("cognito:groups", List.of("admin")).build();
        var autoridades = SecurityConfig.jwtAuthenticationConverter().convert(token).getAuthorities();
        assertThat(autoridades).extracting(Object::toString).contains("ROLE_ADMIN");
    }

    @Test
    void sinGruposNoHayRoles() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "none").claim("username", "corrector").build();
        var autoridades = SecurityConfig.jwtAuthenticationConverter().convert(token).getAuthorities();
        assertThat(autoridades).extracting(Object::toString).noneMatch(a -> a.startsWith("ROLE_"));
    }

    @Test
    void whoamiEsPublicoYDevuelveExamIdYHuella() throws Exception {
        String esperada = WhoamiController.huella("contrasena-de-test");
        mockMvc.perform(get("/api/whoami"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.examId").value("test-id"))
            .andExpect(jsonPath("$.huella").value(esperada));
    }

    @Test
    void whoamiMuestraUsuarioYGrupos() throws Exception {
        mockMvc.perform(get("/api/whoami")
                .with(jwt().jwt(j -> j.claim("username", "corrector").claim("cognito:groups", List.of("admin")))))
            .andExpect(jsonPath("$.usuario").value("corrector"))
            .andExpect(jsonPath("$.grupos", hasItem("admin")));
    }
}
