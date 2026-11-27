card_checks() {
    expect_absent "no sun.misc API in the code" 'sun\.misc\.' src
    expect_present "references use java.util.Base64" 'java\.util\.Base64' src/main/java
}
card_gates() {
    gate_tests 8
    gate_smoke 8
}
