# 1. Getting Started

In this chapter you'll create a Maven project and add the `mango4j-crypto` dependency, ready to start encrypting entity fields in the next chapter.

!!! tip "Follow along"
    ```bash
    git checkout -b my-workshop
    git merge chapter-01
    ```

## Add the dependency

Add `mango4j-crypto` to your `pom.xml`:

```xml
--8<-- "chapter-01/pom.xml:dependency"
```

That's it — your project can now use the `@Encrypt`, `@Hmac`, `@EncryptedData`, `@EncryptionKeyId` and `@HmacKeyId` annotations that `mango4j-crypto` provides.

Next: *2. Encrypting Your First Field (coming soon)*.
