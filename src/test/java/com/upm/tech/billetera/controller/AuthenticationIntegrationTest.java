package com.upm.tech.billetera.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upm.tech.billetera.model.Usuario;
import com.upm.tech.billetera.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationIntegrationTest {

    private static final String PASSWORD = "IntegrationPassword12!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void rutasPrivadasRechazanSolicitudesAnonimas() throws Exception {
        mockMvc.perform(get("/api/cuentas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroCreaUsuarioCuentaCeroEIniciaSesionYUsuarioDuplicadoSeRechaza() throws Exception {
        String username = nuevoUsuario();
        Sesion sesion = csrfInicial();
        String body = """
                {"usuario":"%s","contrasena":"%s","titular":"Cuenta de prueba"}
                """.formatted(username, PASSWORD);

        MvcResult registro = mockMvc.perform(post("/api/auth/register")
                        .session(sesion.session())
                        .header("X-CSRF-TOKEN", sesion.token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.usuario").value(username))
                .andReturn();

        MockHttpSession authenticated = (MockHttpSession) registro.getRequest().getSession(false);
        Usuario usuarioGuardado = usuarioRepository.findByNombreUsuario(username).orElseThrow();
        assertThat(usuarioGuardado.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, usuarioGuardado.getPasswordHash())).isTrue();
        mockMvc.perform(get("/api/auth/me").session(authenticated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value(username));
        mockMvc.perform(get("/api/cuentas").session(authenticated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titular").value("Cuenta de prueba"))
                .andExpect(jsonPath("$[0].saldo").value(0));

        Sesion token = csrfInicial();
        mockMvc.perform(post("/api/auth/register")
                        .session(token.session())
                        .header("X-CSRF-TOKEN", token.token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void usuariosSoloVenSusCuentasPeroPuedenTransferirAOtraCuenta() throws Exception {
        MockHttpSession alice = registrar(nuevoUsuario(), "Alice cuenta");
        MockHttpSession bob = registrar(nuevoUsuario(), "Bob cuenta");
        long idAlice = idPrimeraCuenta(alice);
        long idBob = idPrimeraCuenta(bob);

        mockMvc.perform(get("/api/cuentas/{id}", idBob).session(alice))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/cuentas/{id}/transacciones", idBob).session(alice))
                .andExpect(status().isNotFound());

        postConCsrf(alice, "/api/cuentas", """
                {"titular":"Segunda cuenta Alice"}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.saldo").value(0));
        mockMvc.perform(get("/api/cuentas").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        postConCsrf(alice, "/api/cuentas/depositar", """
                {"idCuenta":%d,"monto":25.00}
                """.formatted(idAlice)).andExpect(status().isOk());

        postConCsrf(alice, "/api/cuentas/transferir", """
                {"idOrigen":%d,"idDestino":%d,"monto":10.00}
                """.formatted(idAlice, idBob)).andExpect(status().isOk());

        mockMvc.perform(get("/api/cuentas/{id}/transacciones", idBob).session(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].direccion").value("ENTRADA"))
                .andExpect(jsonPath("$.content[0].monto").value(10));

        postConCsrf(alice, "/api/cuentas/retirar", """
                {"idCuenta":%d,"monto":1.00}
                """.formatted(idBob)).andExpect(status().isNotFound());

        postConCsrf(alice, "/api/cuentas/transferir", """
                {"idOrigen":%d,"idDestino":%d,"monto":1.00}
                """.formatted(idBob, idAlice)).andExpect(status().isNotFound());
    }

    @Test
    void loginYLogoutGestionanLaSesionYLasCredencialesSeValidan() throws Exception {
        String username = nuevoUsuario();
        MockHttpSession registrada = registrar(username, "Titular login");
        Sesion csrfLogout = csrfCon(registrada);
        MvcResult logout = mockMvc.perform(post("/api/auth/logout")
                        .session(registrada)
                        .header("X-CSRF-TOKEN", csrfLogout.token()))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession sesionNueva = csrfInicial().session();
        String loginBody = ("{\"usuario\":\"%s\",\"contrasena\":\"%s\"}").formatted(username, PASSWORD);
        MvcResult login = postConCsrf(sesionNueva, "/api/auth/login", loginBody)
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession sesionAutenticada = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(sesionAutenticada))
                .andExpect(status().isOk());

        Sesion csrfInvalido = csrfInicial();
        mockMvc.perform(post("/api/auth/login")
                        .session(csrfInvalido.session())
                        .header("X-CSRF-TOKEN", csrfInvalido.token())
                        .contentType("application/json")
                        .content(("{\"usuario\":\"%s\",\"contrasena\":\"wrong-password\"}").formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuario o contraseña incorrectos"));

        assertThat(logout.getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void solicitudesQueCambianDatosRequierenTokenCsrf() throws Exception {
        String username = nuevoUsuario();
        Sesion csrf = csrfInicial();
        String body = """
                {"usuario":"%s","contrasena":"%s","titular":"Cuenta protegida"}
                """.formatted(username, PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepository.existsByNombreUsuario(username)).isFalse();
    }

    private MockHttpSession registrar(String username, String titular) throws Exception {
        Sesion csrf = csrfInicial();
        String body = """
                {"usuario":"%s","contrasena":"%s","titular":"%s"}
                """.formatted(username, PASSWORD, titular);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long idPrimeraCuenta(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cuentas").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode cuentas = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return cuentas.get(0).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions postConCsrf(
            MockHttpSession session, String path, String body) throws Exception {
        Sesion token = csrfCon(session);
        return mockMvc.perform(post(path).session(session)
                .header("X-CSRF-TOKEN", token.token())
                .contentType("application/json")
                .content(body));
    }

    private Sesion csrfInicial() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        return new Sesion((MockHttpSession) result.getRequest().getSession(false),
                objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText());
    }

    private Sesion csrfCon(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return new Sesion(session, objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText());
    }

    private String nuevoUsuario() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record Sesion(MockHttpSession session, String token) {
    }
}
