card_checks() {
    expect_absent "no SimpleDateFormat or java.util.Date" 'SimpleDateFormat|java\.util\.Date' src/main/java
    expect_present "API DTOs are records" 'public record ' src/main/java/dev/omatheusmesmo/demos/invoicing/api
}
card_gates() {
    gate_tests 25
    gate_smoke 25
}
