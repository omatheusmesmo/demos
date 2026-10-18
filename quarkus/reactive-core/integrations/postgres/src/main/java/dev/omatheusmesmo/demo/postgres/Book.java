package dev.omatheusmesmo.demo.postgres;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * A plain entity, without a Panache base class, so that both the Hibernate ORM and the Hibernate Reactive
 * repositories can manage it.
 */
@Entity
public class Book {

    @Id
    @GeneratedValue
    public Long id;

    public String title;
}
