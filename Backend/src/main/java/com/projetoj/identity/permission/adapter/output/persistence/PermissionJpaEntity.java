package com.projetoj.identity.permission.adapter.output.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "permissions")
public class PermissionJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleJpaEntity module;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "action_id", nullable = false)
    private ActionJpaEntity action;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ModuleJpaEntity getModule() {
        return module;
    }

    public void setModule(ModuleJpaEntity module) {
        this.module = module;
    }

    public ActionJpaEntity getAction() {
        return action;
    }

    public void setAction(ActionJpaEntity action) {
        this.action = action;
    }
}
