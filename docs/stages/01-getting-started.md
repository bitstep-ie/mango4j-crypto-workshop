# 1. Getting Started

This stage sets up a Maven project with the `mango4j-crypto` dependency already in place, ready to start encrypting entity fields in the next stage.

!!! tip "Follow along"
    ```bash
    cd stages/stage-01
    ```

## The dependency

`stages/stage-01/pom.xml` already has `mango4j-crypto` declared:

```xml
--8<-- "stage-01/pom.xml:dependency"
```

That's it — this project can now use the `@Encrypt`, `@Hmac`, `@EncryptedData`, `@EncryptionKeyId` and `@HmacKeyId` annotations that `mango4j-crypto` provides.

Feel free to experiment here — each stage is its own standalone folder, so nothing you change in `stage-01` affects `stage-02`. When you're ready, just move on:

```bash
cd ../stage-02
```

Next: *2. Encrypting Your First Field (coming soon)*.
