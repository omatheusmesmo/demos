card_checks() {
    expect_cmd "the XML export test passes on JDK 25" with_jdk 25 ./mvnw -B -q test -Dtest=InvoiceXmlExporterTest
}
card_gates() {
    gate_tests 8
    gate_smoke 8
}
