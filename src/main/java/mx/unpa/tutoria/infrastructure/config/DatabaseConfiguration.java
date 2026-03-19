package mx.unpa.tutoria.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de dual datasource:
 *
 *  PRIMARY  → sga_tutorias   (local, R/W, JPA + Hibernate)
 *  ACADEMICO → escolares SICE (remoto/túnel, ReadOnly, solo JdbcTemplate)
 *
 * El datasource ACADEMICO NO usa JPA. Solo JdbcTemplate para leer las 4 vistas del SICE.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "mx.unpa.tutoria.infrastructure.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef   = "transactionManager"
)
public class DatabaseConfiguration {

    // =========================================================================
    // DATASOURCE 1: sga_tutorias (PRIMARY — R/W)
    // =========================================================================

    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("mx.unpa.tutoria.infrastructure.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        // VALIDATE: Hibernate verifica el schema pero NUNCA lo modifica.
        // Si el schema no coincide, la app falla al arrancar — error visible antes de producción.
        properties.put("hibernate.hbm2ddl.auto",    "validate");
        properties.put("hibernate.show_sql",        false);
        properties.put("hibernate.format_sql",      true);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }

    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // =========================================================================
    // DATASOURCE 2: escolares SICE (ACADEMICO — ReadOnly)
    // Solo se usa con JdbcTemplate para leer las 4 vistas del SICE.
    // NO tiene EntityManager ni repositorios JPA.
    // Si el SICE no está disponible al arrancar, initialization-fail-timeout=-1
    // hace que HikariCP no falle inmediatamente.
    // =========================================================================

    @Bean(name = "academicoDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.academico")
    public DataSource academicoDataSource() {
        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
        ds.setReadOnly(true);
        return ds;
    }

    @Bean(name = "academicoJdbcTemplate")
    public JdbcTemplate academicoJdbcTemplate(
            @Qualifier("academicoDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}