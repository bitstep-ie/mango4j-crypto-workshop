# 1. Getting Started

!!! abstract "Overview"
    `mango4j-crypto` works by adding the dependency to your project, then (from the next stage on) annotating the entity fields you want encrypted. This stage doesn't do anything on its own — it's just the starting point: a bare Maven project with the dependency already wired up, so everyone begins from the same place before we start adding the annotations that actually do the work.

!!! tip "Follow along"
    ```bash
    cd stages/01-Getting-Started
    ```
    Using an IDE instead? Just open `stages/01-Getting-Started` as its own project.

## The dependency

`stages/01-Getting-Started/pom.xml` already has `mango4j-crypto` declared:

```xml
--8<-- "01-Getting-Started/pom.xml:dependency"
```

That's it — this project can now use the `@Encrypt`, `@Hmac`, `@EncryptedData`, `@EncryptionKeyId` and `@HmacKeyId` annotations that `mango4j-crypto` provides.

Feel free to experiment here — each stage is its own standalone folder, so nothing you change in `01-Getting-Started` affects `02-Encrypt-a-Field`. When you're ready, just move on:

```bash
cd ../02-Encrypt-a-Field
```

Next: [2. Encrypting a Field](02-encrypting-a-field.md).
