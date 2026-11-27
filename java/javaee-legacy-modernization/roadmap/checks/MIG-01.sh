card_checks() {
    expect_cmd "the WAR plugin runs on JDK 25" with_jdk 25 ./mvnw -B -q -Dmaven.main.skip -DskipTests war:war
    expect_plugin_at_least maven-compiler-plugin 3.11.0
    expect_plugin_at_least maven-war-plugin 3.4.0
    expect_plugin_at_least maven-surefire-plugin 3.0.0
}
card_gates() {
    gate_tests 8
    gate_smoke 8
}
