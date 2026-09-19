AWS KMS Delegate

!!! abstract "Overview"
    [Real Encryption](03-real-encryption.md) swapped the fake Base64 delegate for `PBKDF2EncryptionService`, a real but local-only cryptographic provider. This stage swaps again, this time for `AwsKmsEncryptionServiceDelegate` from `mango4j-crypto-aws-kms-delegate`, showing that moving to a production key management service is the same shape of change: a different delegate, a different `CryptoKey.type`/`configuration`, and nothing else. Facilitators should frame this as proof that the delegate abstraction from [Key Aliases & Key Configs](04-key-aliases-and-configs.md) holds up against a real external provider, not just the local PBKDF2 example - and be upfront that this stage runs against `FakeKmsClient`, not real AWS, so it works anywhere with no cloud account or credentials needed.

This stage comes as two projects:

- **`starter/`** - what you work in. It runs, but still on the `PBKDF2EncryptionService` delegate from Real Encryption. Look for the `// TODO` comments.
- **`complete/`** - the finished reference, using the real `AwsKmsEncryptionServiceDelegate`.

!!! tip "Follow along"
    ```bash
    cd stages/05-AWS-KMS-Delegate/starter
    ```
    Using an IDE instead? Open `stages/05-AWS-KMS-Delegate/starter` as its own project.

## Why this stage doesn't need real AWS

`AwsKmsEncryptionServiceDelegate` only ever talks to the `KmsClient` interface it's constructed with - it has no idea whether that's the AWS SDK's real client or something else entirely:

```java
--8<-- "05-AWS-KMS-Delegate/complete/src/main/java/ie/bitstep/mango/workshop/FakeKmsClient.java"
```

`FakeKmsClient` implements that same interface with real AES/GCM underneath, no network calls. Every KMS key id derives its own AES key deterministically (a SHA-256 hash of the id), so the same "key" always decrypts what it encrypted, and a different key id never can - with nothing to pre-register or configure. It even carries the key id inside the returned ciphertext blob, the same way real KMS does, since `Decrypt` requests never specify one (see the class's own comments for why). This is the same trick [Getting Started](01-getting-started.md)'s `Base64EncryptionService` and [Real Encryption](03-real-encryption.md)'s comparison used: real enough to prove the wiring, not real infrastructure behind it.

## Choosing a delegate

```java
--8<-- "05-AWS-KMS-Delegate/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:choose-delegate"
```

`AwsKmsEncryptionServiceDelegate` ships in its own module, `mango4j-crypto-aws-kms-delegate`, separate from `mango4j-crypto` itself - a real deployment only pulls in the KMS SDK dependency if it's actually using KMS.

**Your turn:** in `starter/src/main/java/ie/bitstep/mango/workshop/Main.java`, reassign `delegate` to `new AwsKmsEncryptionServiceDelegate(new FakeKmsClient())`.

## Configuring the key

```java
--8<-- "05-AWS-KMS-Delegate/complete/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java:key-config"
```

Same shape as [Real Encryption](03-real-encryption.md): `type` selects the delegate (`AwsKmsEncryptionServiceDelegate` reports `"AWS_KMS"` from `supportedCryptoKeyType()`), and `configuration` carries whatever that delegate needs - here, a KMS key id/ARN and the encryption algorithm. Still no key material, and still never reaching application code.

**Your turn:** in `starter/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java`, set `keyType` to `"AWS_KMS"` and populate `keyConfiguration` (see the `// TODO`).

## Encrypting and decrypting

The calls are unchanged, again:

```java
--8<-- "05-AWS-KMS-Delegate/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:encrypt"
```

```java
--8<-- "05-AWS-KMS-Delegate/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:decrypt"
```

## Switching from `FakeKmsClient` to a real KMS endpoint

```java
--8<-- "05-AWS-KMS-Delegate/complete/src/main/java/ie/bitstep/mango/workshop/RealAwsKmsClientExample.java:real-kms-client"
```
<!-- link -->

`RealAwsKmsClientExample` isn't called from `Main` - CI compiles it (proving the construction is genuinely valid) but never runs it, since actually calling `realKmsClient()`'s result would need a real AWS account, real credentials, and network access to a real KMS endpoint, none of which this workshop requires. Going from this stage's demo to a real deployment means two edits, both in `Main`:

```java
// this stage
delegate = new AwsKmsEncryptionServiceDelegate(new FakeKmsClient());

// a real deployment
delegate = new AwsKmsEncryptionServiceDelegate(RealAwsKmsClientExample.realKmsClient());
```

Everything else - the delegate class itself, the `CryptoKey` configuration, the `encrypt()`/`decrypt()` call sites - stays untouched either way.

`KmsClient.builder().build()` alone (no `.region(...)`) resolves both region and credentials from the AWS SDK's standard default chain - environment variables, `~/.aws/credentials`, an EC2/ECS/Lambda IAM role, and so on - which is usually the right choice in a real deployment rather than hardcoding either. `.region(Region.EU_WEST_1)` is shown here only to match this stage's example ARN; a real key's ARN already names its own region, so that override is often unnecessary too.

## Running it

`starter/` before your changes still runs on PBKDF2:

```
cardNumber (still in memory): 5111111111111111
encryptedData:                {"cryptoKeyId":"workshop-encryption-key","data":{"algorithm":"AES","iv":"...","cipherText":"...",...}}
decrypted cardNumber:         5111111111111111
```

After both changes:

```
cardNumber (still in memory): 5111111111111111
encryptedData:                {"cryptoKeyId":"workshop-encryption-key","data":{"data":"AAAAOGFybjphd3M6a21zOmV1LXdlc3QtMToxMTExMjIyMjMzMzM6a2V5L3dvcmtzaG9wLWRlbW8ta2V5...","awsKeyId":"arn:aws:kms:eu-west-1:111122223333:key/workshop-demo-key","algorithm":"SYMMETRIC_DEFAULT"}}
decrypted cardNumber:         5111111111111111
```

The ciphertext's shape changed - a single opaque `data` blob (what KMS actually returns) alongside the `awsKeyId`/`algorithm` this stage's key config carries - but nothing about how `cryptoShield.encrypt()`/`decrypt()` are called did.
