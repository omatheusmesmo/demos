package dev.omatheusmesmo.demo.postgres;

/**
 * Every title, after PostgreSQL sleeps for 200 ms, so each query holds its connection for a measurable time.
 */
final class SlowQuery {

    static final String TITLES = "select b.title from Book b, pg_sleep(0.2) order by b.title";

    private SlowQuery() {
    }
}
