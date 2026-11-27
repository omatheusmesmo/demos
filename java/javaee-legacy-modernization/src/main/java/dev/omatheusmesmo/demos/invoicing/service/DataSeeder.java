package dev.omatheusmesmo.demos.invoicing.service;

import dev.omatheusmesmo.demos.invoicing.domain.Customer;
import dev.omatheusmesmo.demos.invoicing.domain.CustomerTier;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Singleton
@Startup
public class DataSeeder {

    @PersistenceContext(unitName = "invoicing")
    EntityManager em;

    @PostConstruct
    void seed() {
        em.persist(new Customer("ACME", "Acme Corporation", CustomerTier.GOLD));
        em.persist(new Customer("GLOBEX", "Globex Inc.", CustomerTier.SILVER));
        em.persist(new Customer("INITECH", "Initech LLC", CustomerTier.STANDARD));
    }
}
