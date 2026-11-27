card_checks() {
    expect_absent "no finalize() left" 'void finalize\(' src/main/java
    expect_present "AuditLog is AutoCloseable" 'class AuditLog[^{]*implements[^{]*(AutoCloseable|Closeable)' src/main/java
}
card_gates() {
    gate_tests 8
    gate_smoke 8
}
