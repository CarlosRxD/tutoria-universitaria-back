package mx.unpa.tutoria;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class TutoriaUniversitariaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TutoriaUniversitariaApplication.class, args);
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        log.info("🕒 Zona horaria configurada: {}", TimeZone.getDefault().getID());
    }

    @Bean
    CommandLineRunner diagnosticoInicio(
            @Qualifier("dataSource") DataSource dataSourceLocal, // Agrega el Qualifier
            @Qualifier("academicoJdbcTemplate") JdbcTemplate siceJdbc
    ) {
        return args -> {
            log.info("=================================================");
            log.info("🚀 SISTEMA DE TUTORÍAS UNPA — DIAGNÓSTICO INICIO");
            log.info("=================================================");

            // ── BD LOCAL: sga_tutorias ─────────────────────────────────────
            try (Connection conn = dataSourceLocal.getConnection()) {
                log.info("✅ BD LOCAL [sga_tutorias]: Conectada — {}", conn.getMetaData().getURL());
            } catch (SQLException e) {
                log.error("❌ BD LOCAL [sga_tutorias]: Error crítico de conexión", e);
            }

            // ── BD SICE: escolares ─────────────────────────────────────────
            // Las vistas v_sice_* deben existir en el SICE antes de arrancar.
            // Si no existen, el sistema arranca igual pero la sincronización fallará.
            try {
                Integer docentes = siceJdbc.queryForObject(
                        "SELECT COUNT(*) FROM escolares.v_sice_docentes", Integer.class);

                Integer alumnos = siceJdbc.queryForObject(
                        "SELECT COUNT(*) FROM escolares.v_sice_alumnos_activos", Integer.class);

                log.info("✅ BD SICE [escolares]: Conectada correctamente");
                log.info("   👨‍🏫 Docentes en v_sice_docentes:          {}", docentes);
                log.info("   👨‍🎓 Alumnos activos en v_sice_alumnos:    {}", alumnos);

            } catch (Exception e) {
                // No falla el arranque — la sincronización simplemente no correrá
                log.warn("⚠️  BD SICE [escolares]: No disponible al arrancar.");
                log.warn("   Causa: {}", e.getMessage());
                log.warn("   → La aplicación arranca sin SICE.");
                log.warn("   → Verifica la conexión antes de ejecutar sincronización.");
            }

            log.info("=================================================");
        };
    }
}