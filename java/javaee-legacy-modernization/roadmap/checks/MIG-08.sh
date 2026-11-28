EE_PACKAGES='javax\.(persistence|ws\.rs|ejb|inject|enterprise|faces|xml\.bind|annotation|servlet|json|validation|transaction)'
card_checks() {
    expect_absent "no Java EE javax.* package in the code" "$EE_PACKAGES" src
    expect_absent "no Java EE schema in the descriptors" 'xmlns\.jcp\.org|javax\.persistence' src/main/resources src/main/webapp/WEB-INF
    expect_absent "no over-eager rename of the JDK's javax.sql" 'jakarta\.sql' src
    expect_present "the datasource still names javax.sql.DataSource" 'javax\.sql\.DataSource=' src/main/liberty/config/server.xml
    expect_present "Liberty runs jakartaee-11.0" '<feature>jakartaee-11\.0</feature>' src/main/liberty/config/server.xml
    expect_absent "no javaee-8.0 feature left" 'javaee-8\.0' src/main/liberty/config/server.xml
    expect_absent "no javaee-api dependency left" '<artifactId>javaee-api</artifactId>' pom.xml
}
card_gates() {
    gate_tests 25
    gate_smoke 25
}
