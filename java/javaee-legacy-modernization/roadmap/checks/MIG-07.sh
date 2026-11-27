card_checks() {
    expect_absent "no JUnit 4 API in the tests" 'import (static )?org\.junit\.(Test|Assert|Before|BeforeClass|After|Rule|rules|runner)' src/test/java
    expect_present "tests use JUnit Jupiter" 'org\.junit\.jupiter' src/test/java
    expect_absent "no JUnit 4 or Mockito 1 dependency" '<artifactId>junit</artifactId>|<version>1\.10\.19</version>' pom.xml
}
card_gates() {
    gate_tests "$(declared_jdk)"
    gate_smoke "$(declared_jdk)"
}
