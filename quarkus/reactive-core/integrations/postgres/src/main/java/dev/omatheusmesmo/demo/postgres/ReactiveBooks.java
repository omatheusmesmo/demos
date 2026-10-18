package dev.omatheusmesmo.demo.postgres;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ReactiveBooks implements PanacheRepository<Book> {

    public Uni<List<String>> slowTitles() {
        return getSession().chain(session -> session.createNativeQuery(SlowQuery.TITLES, String.class).getResultList());
    }
}
