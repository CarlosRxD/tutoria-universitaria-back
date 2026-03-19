package mx.unpa.tutoria.infrastructure.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "configuraciones")
public class ConfiguracionEntity {
    @Id
    private String clave;
    private String valor;
}