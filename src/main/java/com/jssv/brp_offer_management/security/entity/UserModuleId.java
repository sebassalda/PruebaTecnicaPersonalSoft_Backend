package com.jssv.brp_offer_management.security.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserModuleId implements Serializable {

    private Long usuarioId;
    private Long moduloId;

    public UserModuleId() {
    }

    public UserModuleId(Long usuarioId, Long moduloId) {
        this.usuarioId = usuarioId;
        this.moduloId = moduloId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getModuloId() {
        return moduloId;
    }

    public void setModuloId(Long moduloId) {
        this.moduloId = moduloId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof UserModuleId other)) {
            return false;
        }

        return Objects.equals(usuarioId, other.usuarioId)
                && Objects.equals(moduloId, other.moduloId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, moduloId);
    }
}
