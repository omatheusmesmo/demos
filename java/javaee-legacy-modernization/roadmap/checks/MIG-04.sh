card_checks() {
    expect_absent "no script engine in the code" 'javax\.script|ScriptEngine|nashorn' src/main/java src/test/java
    expect_absent "no JavaScript rules left" '.' --include='*.js' src/main/resources
    expect_absent "no standalone Nashorn dependency" 'nashorn' --include=pom.xml .
}
card_gates() {
    gate_tests 8
    gate_smoke 8
}
