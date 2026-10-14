package es.upm.grise.examen.anuncios;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Despliegue sin cognito.sh: no hay emisor de tokens, pero la aplicación debe arrancar y responder. */
@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=")
@AutoConfigureMockMvc
class SinCognitoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whoamiRespondeSinCognito() throws Exception {
        mockMvc.perform(get("/api/whoami"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.examId").value("test-id"));
    }

    @Test
    void protegidoDa401SinCognito() throws Exception {
        mockMvc.perform(get("/api/mensajes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/mensajes").contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"hola\"}"))
            .andExpect(status().isUnauthorized());
    }
}
