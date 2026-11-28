card_checks() {
    expect_present "the project declares JDK 25" '^java=25\.' .sdkmanrc
    expect_present "the compiler targets release 25" '<maven\.compiler\.release>25</maven\.compiler\.release>' pom.xml
    expect_absent "no source/target 1.8 left" 'maven\.compiler\.(source|target)|<(source|target)>1\.8<' pom.xml
}
card_gates() {
    gate_tests 25
    gate_smoke 25
}
