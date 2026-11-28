card_checks() {
    expect_present "the build is a Quarkus application" '<artifactId>quarkus-maven-plugin</artifactId>' pom.xml
    expect_absent "no WAR packaging or Liberty left" '<packaging>war</packaging>|liberty-maven-plugin' pom.xml
    expect_absent "no EJB left" 'jakarta\.ejb' src
}
card_gates() {
    gate_tests 25
    gate_smoke 25
}
