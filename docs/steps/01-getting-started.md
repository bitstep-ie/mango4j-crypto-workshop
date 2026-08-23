# 1. Getting Started

This step sets up a Maven project with the `mango4j-crypto` dependency already in place, ready to start encrypting entity fields in the next step.

!!! tip "Follow along"
    ```bash
    git checkout step-01
    ```

## The dependency

Checking out this step gives you a `pom.xml` with `mango4j-crypto` already declared:

```xml
--8<-- "step-01/pom.xml:dependency"
```

That's it — your project can now use the `@Encrypt`, `@Hmac`, `@EncryptedData`, `@EncryptionKeyId` and `@HmacKeyId` annotations that `mango4j-crypto` provides.

!!! tip "Before you move on"
    If you tried anything of your own in this step, discard it before switching to the next step:
    ```bash
    git reset --hard
    git clean -fd
    git checkout step-02
    ```

Next: *2. Encrypting Your First Field (coming soon)*.
