package com.upm.tech.billetera.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "DEMO_USER_PASSWORD=")
class BilleteraE2EIT {

    private static final String PASSWORD = "E2E-Segura-2026!";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("billetera_e2e")
            .withUsername("e2e")
            .withPassword("e2e");

    private static final List<BrowserContext> CONTEXTS = new CopyOnWriteArrayList<>();
    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    private Page page;
    private String baseUrl;

    @RegisterExtension
    final TestWatcher screenshotOnFailure = new TestWatcher() {
        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            Path directory = Path.of("target", "e2e-artifacts");
            try {
                Files.createDirectories(directory);
                int index = 0;
                for (BrowserContext browserContext : CONTEXTS) {
                    for (Page openPage : browserContext.pages()) {
                        String name = context.getRequiredTestMethod().getName()
                                .replaceAll("[^a-zA-Z0-9._-]", "_");
                        openPage.screenshot(new Page.ScreenshotOptions()
                                .setPath(directory.resolve(name + "-" + index++ + ".png"))
                                .setFullPage(true)
                                .setType(ScreenshotType.PNG));
                    }
                }
            } catch (Exception screenshotError) {
                cause.addSuppressed(screenshotError);
            }
        }
    };

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void launchChromium() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        CONTEXTS.forEach(BrowserContext::close);
        CONTEXTS.clear();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void openPanel() {
        baseUrl = "http://localhost:" + port;
        page = newPage();
        page.navigate(baseUrl + "/");
        assertThat(page.locator("#authPanel")).isVisible();
    }

    @Test
    void registroCuentasOperacionesHistorialLogoutYLogin() {
        String username = username("panel");
        register(page, "Titular inicial", username);

        assertThat(page.locator("#cuentaSelector option")).hasCount(1);
        assertThat(page.locator("#saldoDisplay")).hasText("$ 0,00");
        long initialAccount = accountId(page);

        page.locator("#crearCuentaForm [name=titular]").fill("Cuenta secundaria");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Crear cuenta")).click();
        assertThat(page.locator("#alertBox")).containsText("Cuenta creada con saldo cero");
        assertThat(page.locator("#cuentaSelector option")).hasCount(2);
        assertThat(page.locator("#titularDisplay")).hasText("Cuenta secundaria");
        assertThat(page.locator("#saldoDisplay")).hasText("$ 0,00");
        long secondaryAccount = accountId(page);
        assertThat(page.locator("#cuentaIdLabel")).hasText(Long.toString(secondaryAccount));

        page.locator("#depositarForm [name=monto]").fill("25.00");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Depositar")).click();
        assertThat(page.locator("#saldoDisplay")).hasText("$ 25,00");
        page.locator("#retirarForm [name=monto]").fill("5.00");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Retirar")).click();
        assertThat(page.locator("#saldoDisplay")).hasText("$ 20,00");

        assertThat(page.locator("#historialBody tr")).hasCount(2);
        assertThat(page.locator("#historialBody")).containsText("Depósito");
        assertThat(page.locator("#historialBody")).containsText("Retiro");
        assertThat(page.locator("#historialBody")).containsText("+ $ 25,00");
        assertThat(page.locator("#historialBody")).containsText("− $ 5,00");
        page.locator("#historialTipo").selectOption("DEPOSITO");
        page.locator("#aplicarFiltrosBtn").click();
        assertThat(page.locator("#historialBody tr")).hasCount(1);
        assertThat(page.locator("#historialBody")).containsText("+ $ 25,00");
        page.locator("#limpiarFiltrosBtn").click();
        assertThat(page.locator("#historialBody tr")).hasCount(2);

        page.locator("#cuentaSelector").selectOption(Long.toString(initialAccount));
        assertThat(page.locator("#titularDisplay")).hasText("Titular inicial");
        assertThat(page.locator("#saldoDisplay")).hasText("$ 0,00");
        assertThat(page.locator("#historialBody tr")).hasCount(1);
        assertThat(page.locator("#historialBody")).containsText("No hay movimientos");

        page.locator("#logoutBtn").click();
        assertThat(page.locator("#authPanel")).isVisible();
        assertThat(page.locator("#dashboard")).isHidden();
        page.locator("#loginForm [name=usuario]").fill(username);
        page.locator("#loginForm [name=contrasena]").fill(PASSWORD);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Entrar")).click();
        assertThat(page.locator("#dashboard")).isVisible();
        assertThat(page.locator("#usuarioActual")).hasText(username);
        assertThat(page.locator("#cuentaSelector option")).hasCount(2);
        page.locator("#cuentaSelector").selectOption(Long.toString(secondaryAccount));
        assertThat(page.locator("#titularDisplay")).hasText("Cuenta secundaria");
        assertThat(page.locator("#saldoDisplay")).hasText("$ 20,00");
    }

    @Test
    void transferenciaEntreUsuariosMuestraDireccionEnCadaCuenta() {
        String origenUsername = username("origen");
        register(page, "Cuenta origen", origenUsername);
        long cuentaOrigen = accountId(page);

        Page destinatario = newPage();
        destinatario.navigate(baseUrl + "/");
        assertThat(destinatario.locator("#authPanel")).isVisible();
        register(destinatario, "Cuenta destino", username("destino"));
        long cuentaDestino = accountId(destinatario);

        destinatario.locator("#depositarForm [name=monto]").fill("20.00");
        destinatario.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Depositar")).click();
        assertThat(destinatario.locator("#saldoDisplay")).hasText("$ 20,00");

        destinatario.locator("#transferirForm [name=idDestino]").fill(Long.toString(cuentaOrigen));
        destinatario.locator("#transferirForm [name=monto]").fill("7.00");
        destinatario.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Transferir")).click();
        assertThat(destinatario.locator("#alertBox")).containsText("Transferencia realizada correctamente");
        assertThat(destinatario.locator("#saldoDisplay")).hasText("$ 13,00");
        assertThat(destinatario.locator("#historialBody tr")).hasCount(2);
        assertThat(destinatario.locator("#historialBody tr").nth(0)).containsText("Salida");
        assertThat(destinatario.locator("#historialBody tr").nth(0)).containsText("− $ 7,00");
        assertThat(destinatario.locator("#historialBody tr").nth(0))
                .containsText("Cuenta origen (#" + cuentaOrigen + ")");

        page.reload();
        assertThat(page.locator("#dashboard")).isVisible();
        assertThat(page.locator("#cuentaIdLabel")).hasText(Long.toString(cuentaOrigen));
        assertThat(page.locator("#saldoDisplay")).hasText("$ 7,00");
        assertThat(page.locator("#historialBody tr")).hasCount(1);
        assertThat(page.locator("#historialBody tr").nth(0)).containsText("Entrada");
        assertThat(page.locator("#historialBody tr").nth(0)).containsText("+ $ 7,00");
        assertThat(page.locator("#historialBody tr").nth(0))
                .containsText("Cuenta destino (#" + cuentaDestino + ")");

        var foreignAccountResponse = destinatario.navigate(baseUrl + "/api/cuentas/" + cuentaOrigen);
        assertEquals(404, foreignAccountResponse.status());
    }

    private Page newPage() {
        BrowserContext context = browser.newContext();
        CONTEXTS.add(context);
        return context.newPage();
    }

    private void register(Page target, String holder, String username) {
        target.locator("#registerForm [name=titular]").fill(holder);
        target.locator("#registerForm [name=usuario]").fill(username);
        target.locator("#registerForm [name=contrasena]").fill(PASSWORD);
        target.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Registrarme")).click();
        assertThat(target.locator("#dashboard")).isVisible();
        assertThat(target.locator("#usuarioActual")).hasText(username);
        assertThat(target.locator("#saldoDisplay")).hasText("$ 0,00");
    }

    private static String username(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static long accountId(Page target) {
        return accountIdFromText(target.locator("#cuentaIdLabel").innerText());
    }

    private static long accountIdFromText(String text) {
        return Long.parseLong(text.trim());
    }
}
