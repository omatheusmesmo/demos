# java/javaee-legacy-modernization

A legacy Java EE 8 application on Java 8, and a roadmap that migrates it to Java 25 and Jakarta EE 11 one card at a
time, with every card checked by a script instead of by trust. Built for a live demo of
[IBM Bob](https://bob.ibm.com) doing the migration, but nothing here depends on Bob: any person or agent can work
the cards.

## The application

An invoicing back office, written the way it was written around 2018:

| Part | Technology | Why it is on the roadmap |
|---|---|---|
| REST API | JAX-RS, JSON-B, `@EJB` injection | `javax` namespace (MIG-08) |
| Business logic | `@Stateless` EJB, JPA (EclipseLink) on H2 | `javax` namespace and descriptors (MIG-08) |
| Discount rules | JavaScript evaluated by Nashorn | Nashorn was removed in Java 15 (MIG-04) |
| Invoice references | `sun.misc.BASE64Encoder` | internal API, removed in Java 9 (MIG-02) |
| Audit log | a file closed in `finalize()` | deprecated for removal, leaks file handles (MIG-03) |
| XML export | JAXB | removed from the JDK in 11, and not part of `jakartaee-11.0` on Liberty (MIG-05, MIG-08) |
| Back office page | JSF with `@ManagedBean` | removed in Jakarta Faces 4 (MIG-08) |
| Build | `maven-war-plugin` 2.2, Mockito 1 | do not run on a modern JDK (MIG-01, MIG-06) |

It runs on [Open Liberty](https://openliberty.io), IBM's open source runtime, with the `javaee-8.0` feature. The
runtime version stays the same through the whole migration: only the features change, so every difference in the
diff comes from the migration.

```shell
make test     # unit tests on the JDK in .sdkmanrc (8)
make smoke    # start Liberty, drive the API and the page, compare with smoke/golden
make run      # Liberty dev mode on http://localhost:9080
make board    # the roadmap with the live status of every card
```

It needs JDK 8 and JDK 25 (`sdk install java 8.0.514.0+1-tem`, `sdk install java 25.0.5.0+8-tem`, or set
`JDK8_HOME` and `JDK25_HOME`), `curl`, `jq` and `xmllint`.

## What the tests prove

- **Unit tests** pin the legacy behaviour, quirks included: the discount rules compute in doubles, so a SILVER
  invoice of 516.30 gets 25.81 off, not the 25.82 that exact decimal arithmetic gives. Issued invoices carry 25.81.
- **`make smoke`** starts the application on Liberty, creates invoices for every discount rule, reads them back as
  JSON and XML, checks the JSF page, and compares every response with `smoke/golden/`, recorded on the legacy
  baseline. A migration passes only if the application answers exactly as it did on Java 8.
- **`make card CARD=<ID>`** checks one roadmap card: its own assertions (they fail on the baseline) and then the
  gates above. See [roadmap/README.md](roadmap/README.md).

## Working with IBM Bob

Open this folder (not the repository root) as the workspace, so Bob picks up `.bob/rules/migration.md` and
`.bob/.bobignore`. Then ask for one card at a time, with the prompt written in each card. Every card is delivered as
its own branch and pull request, described in its Pull request section; `main` keeps only the legacy application and
the cards.
