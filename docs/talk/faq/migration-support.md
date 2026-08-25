How do I migrate an existing unencrypted field to encrypted with mango4j-crypto?

The `@EnableMigrationSupport` annotation marks a field as temporarily exempt from the usual rule that `@Encrypt` fields must be transient — letting it stay in its old, unencrypted, persisted form while a backfill completes. It takes a `completedBy` date and a `justification`; the framework logs a warning before that date and an error after it, so the exception doesn't quietly become permanent.
