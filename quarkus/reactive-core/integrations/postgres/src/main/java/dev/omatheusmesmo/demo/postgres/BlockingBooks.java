package dev.omatheusmesmo.demo.postgres;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class BlockingBooks implements PanacheRepository<Book> {

    public List<String> slowTitles() {
        return getEntityManager().createNativeQuery(SlowQuery.TITLES, String.class).getResultList();
    }
}
