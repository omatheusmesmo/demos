package dev.omatheusmesmo.demo.threading.context;

/**
 * The request tag as seen through each propagation mechanism, plus the thread the request filter ran on.
 */
public record Tags(String contextLocal, String threadLocal, String filterThread) {

    static Tags read() {
        return RequestTagFilter.REGULAR.read();
    }

    static Tags readPreMatching() {
        return RequestTagFilter.PRE_MATCHING.read();
    }
}
