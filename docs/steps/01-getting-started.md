# 1. Getting Started

In this step you'll create a Maven project and add the `mango4j-crypto` dependency, ready to start encrypting entity fields in the next step.

!!! tip "Follow along"
    ```bash
    git checkout -b my-workshop
    git merge step-01
    ```

## Add the dependency

Add `mango4j-crypto` to your `pom.xml`:

```xml
--8<-- "step-01/pom.xml:dependency"
```

That's it — your project can now use the `@Encrypt`, `@Hmac`, `@EncryptedData`, `@EncryptionKeyId` and `@HmacKeyId` annotations that `mango4j-crypto` provides.

!!! tip "Before you move on"
    If you tried anything of your own in this step, discard it so the next step merges cleanly:
    ```bash
    git reset --hard
    git clean -fd
    ```

Next: *2. Encrypting Your First Field (coming soon)*.
