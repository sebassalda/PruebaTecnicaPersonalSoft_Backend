package com.jssv.brp_offer_management.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuario_modulo")
@IdClass(UserModuleId.class)
public class UserModuleEntity {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Id
    @Column(name = "modulo_id", nullable = false)
    private Long moduloId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            insertable = false,
            updatable = false
    )
    private UserEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "modulo_id",
            insertable = false,
            updatable = false
    )
    private ModuleEntity modulo;
}
