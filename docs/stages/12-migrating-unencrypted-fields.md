Migrating Unencrypted Fields

!!! abstract "Overview"
    Every previous stage's `cardNumber` has been `transient` - CryptoShield requires that of every `@Encrypt` field, since a non-transient field risks the plaintext getting persisted right alongside its own ciphertext. This stage covers what happens when that requirement collides with reality: some other part of the system (a legacy batch export, a report, a second service) still reads the field directly and isn't ready for it to become transient yet. `@EnableMigrationSupport` is mango4j-crypto's answer - not a runtime safety mechanism, but a tracked, dated exception with a paper trail. Facilitators should open with the naive failure mode `talk/naive-migration/` demonstrates - a table mid-backfill has rows in both states at once - as the broader migration story this annotation is one small, narrowly-scoped piece of, not a replacement for it.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles, but throws immediately on startup: both entities have a non-transient `@Encrypt` field with no `@EnableMigrationSupport` to excuse it. Look for the `// TODO` comments.
- **`complete/`** - the finished reference, where both fields build successfully and log exactly what's expected of them.

!!! tip "Follow along"
    ```bash
    cd stages/12-Migrating-Unencrypted-Fields/starter
    ```
    Using an IDE instead? Open `stages/12-Migrating-Unencrypted-Fields/starter` as its own project.

## The rule this stage is about

Every `@Encrypt` field in every previous stage has been declared `transient`, without comment - it was just always already that way. `AnnotatedEntityManager` enforces it: build a `CryptoShield` over an entity with a non-transient `@Encrypt` field and nothing else, and it throws immediately, before any encryption ever happens.

```
InProgressMigrationEntity has a field named cardNumber marked with @Encrypt but it is not transient.
Please mark any fields annotated with @Encrypt as transient
```

That's what `starter/` does, unmodified - this is the exception you're fixing.

## The escape hatch

```java
--8<-- "12-Migrating-Unencrypted-Fields/complete/src/main/java/ie/bitstep/mango/workshop/InProgressMigrationEntity.java:in-progress"
```

`@EnableMigrationSupport` doesn't change how `cardNumber` behaves at runtime - `encrypt()`/`decrypt()` read and write it by reflection either way, `transient` or not. What it changes is registration: instead of throwing, `AnnotatedEntityManager` logs a message naming the field, the justification, and a ticket reference, and moves on.

**Your turn:** in `starter/.../InProgressMigrationEntity.java`, add `@EnableMigrationSupport` above `cardNumber`, with a `completedBy` date in the future.

```java
--8<-- "12-Migrating-Unencrypted-Fields/complete/src/main/java/ie/bitstep/mango/workshop/OverdueMigrationEntity.java:overdue"
```

Same fix, but with `completedBy` already in the past - a migration that should have been finished by now, and wasn't.

**Your turn:** in `starter/.../OverdueMigrationEntity.java`, add `@EnableMigrationSupport` above its `cardNumber` too, with a `completedBy` date already behind you.

## What changes at the deadline

```java
--8<-- "12-Migrating-Unencrypted-Fields/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:build-shield"
```

Building the shield is where both annotations actually get evaluated - watch the console, not the program's `System.out` lines, since these come from mango4j-crypto's own logger:

```
WARNING: Field InProgressMigrationEntity.cardNumber is marked with @EnableMigrationSupport. Justification: ... Expected completion: 2027-01-01. Ticket: WORKSHOP-12
SEVERE: Field OverdueMigrationEntity.cardNumber is marked with @EnableMigrationSupport. Justification: ... Expected completion: 2025-01-01. Ticket: WORKSHOP-13 - MIGRATION DEADLINE HAS PASSED!
```

Before the deadline: a warning. After it: an error - but note what doesn't happen either way. The build still succeeds, and encryption still works:

```java
--8<-- "12-Migrating-Unencrypted-Fields/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:still-works"
```

Neither log level blocks anything. `@EnableMigrationSupport` is a paper trail for whoever's watching application logs or alerting on `SEVERE`-level messages, not a circuit breaker - the deadline passing is a signal for a person to act on, not a safety mechanism the library enforces on its own.

## Backfilling what's already there

`@EnableMigrationSupport` only covers records going through `CryptoShield` right now. Everything already sitting in the table - rows fetched, until this point, straight off the old plaintext column - still needs `encryptedData` populated at least once:

```java
--8<-- "12-Migrating-Unencrypted-Fields/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:backfill-sweep"
```

Unlike [Rekeying: Encryption](10-rekeying-encryption.md)'s sweep, there's nothing to decrypt first - these records only ever had a plaintext value, never any ciphertext, so backfilling one is a single `encrypt()` call.

**Your turn:** in `starter/.../Main.java`, backfill `legacyRecords`: call `cryptoShield.encrypt()` on each one.

## Completing the cutover

```java
--8<-- "12-Migrating-Unencrypted-Fields/complete/src/main/java/ie/bitstep/mango/workshop/MigratedEntity.java"
```

Once every record in the system - not just these three - has `encryptedData` populated, the migration is actually done, and `@EnableMigrationSupport` has served its purpose. `MigratedEntity` is what `cardNumber` looks like on the other side of that cutover: `transient` again, no migration annotation, identical in shape to every entity since [Real Encryption](03-real-encryption.md). Two things happen together at cutover, in code and in the schema:

1. The field goes back to plain `@Encrypt private transient String cardNumber;` - `@EnableMigrationSupport` comes off entirely, not just its deadline pushed out.
2. The now-unused plaintext column gets dropped from the database. Nothing in this workshop's plain Java entities has an actual schema to alter, but the code change is the signal that it's safe to: once nothing maps `cardNumber` to a persisted column anymore, nothing is reading that column either, and keeping a dropped field's data around is pure liability with no upside.

```java
--8<-- "12-Migrating-Unencrypted-Fields/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:cutover"
```

## Running it

`starter/` throws before any `System.out` line is ever reached - the exception shown above, on the first entity `CryptoShield` tries to register.

After all three changes:

```
in-progress field still encrypts fine: {"cryptoKeyId":"workshop-encryption-key",...}
overdue field still encrypts fine:     {"cryptoKeyId":"workshop-encryption-key",...}
legacy records backfilled: 3 / 3
post-cutover field still encrypts fine: {"cryptoKeyId":"workshop-encryption-key",...}
```

Plus the two log lines from earlier, printed during `CryptoShield.Builder().build()` before any of those. The last two lines are this addition: the backfill count confirms every legacy record picked up its ciphertext, and `MigratedEntity` - built with a completely separate `CryptoShield` that's never even heard of `@EnableMigrationSupport` - proves the field works exactly like any other stage's once the migration is behind it.

## The rest of the migration story

This annotation buys a legacy code path time - it doesn't do the migration itself. Actually moving a genuinely unencrypted field to encrypted, field by field, across records already in production, is what `talk/naive-migration/` walks through: a table mid-backfill has rows in both states at once, and a naive `load()` that assumes everything's already ciphertext throws on the rows the backfill hasn't reached yet. A real migration needs all three pieces demonstrated on this page - a tracked, dated relaxation for whatever still needs direct access, a backfill sweep that reaches every record, and a cutover once it has - plus the piece only `talk/naive-migration/` covers: tolerating a table in a mixed state for as long as the backfill is still in progress.

---

This is the last stage in the hands-on arc. Together with [Key Rotation](09-key-rotation.md), [Rekeying: Encryption](10-rekeying-encryption.md), and [Rekeying: HMACs](11-rekeying-hmacs.md), it completes the promise made all the way back in [Introduction](00-intro.md): pluggable encryption providers, multiple HMAC strategies, rekeying support, and migration of existing unencrypted fields, each demonstrated with real, runnable code rather than just described.
