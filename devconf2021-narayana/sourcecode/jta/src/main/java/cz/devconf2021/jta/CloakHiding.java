package cz.devconf2021.jta;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Cacheable
public class CloakHiding extends PanacheEntity {

    @Column(length = 40, unique = true)
    public String cloakAction;

    public CloakHiding() {
    }

    public CloakHiding(String cloakAction) {
        this.cloakAction = cloakAction;
    }
}
