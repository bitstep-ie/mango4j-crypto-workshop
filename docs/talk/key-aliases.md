Key Aliases → Crypto Key Configs

## The limits of representing a key as a string

It's tempting to represent an encryption key in your code as a simple String, usually due to the fact that applications rarely deal directly with the actual encryption key itself. 
Often they use a key reference such as a JKS key alias or a HSM slot label; just a String your encrypt/decrypt calls pass around. e.g. 
```java
public class MyBusinessService { 
    
    @Value("${encryption.key.alias}")
    private final String encryptionKey; // probably just something like a JKS key label;

    public void encryptAndStore(String plaintext) {
        String ciphertext = jksService.encrypt(encryptionKey, plaintext);
        database.save(ciphertext);
    }
}
```

That's workable until you need to do things like:

- Switch which cryptographic provider a key uses (HSM today, AWS-KMS tomorrow)
- Run multiple providers side by side (different regions, different regulatory regimes)
- Rotate a key without touching a single line of application code

A raw string can't carry any of that. It's just an opaque label — there's nothing in it saying *how* to use it.

## A key as an object, not a string

A more durable approach is to represent every key as a small object rather than a bare identifier — something carrying not just a label or alias, 
but what the key is used for (encryption vs. HMAC), which cryptographic provider/mechanism should handle it, and whatever configuration that provider needs to actually 
perform the operation (a reference to where the key lives, never the raw key material itself).

```java
public class MyBusinessService {

    @Value("${encryption.key}")
    private final EncryptionKey encryptionKey; // Now it's a proper object, not just a string
	
    public void encryptAndStore(String plaintext) {
        String ciphertext = encryptionService.encrypt(encryptionKey, plaintext);
        database.save(ciphertext);
    }
}
```

We made 2 changes here:
1. The `encryptionKey` is now an `EncryptionKey` object rather than a raw string. It can carry all the information needed to tell the encryptionService code how to perform the operation.
2. The `encryptAndStore` method now calls a generic `encryptionService` rather than a service which uses a specific provider (`jksService`). 
This is essentially just a name change, but we included it here to get the point across that the `encryptionService` is no longer a "**JKS** Service".
It is now just a generic encryption service that can use information in the `EncryptionKey` object to decide *how* it should encrypt the text 
rather than just assuming that it's always dealing with a JKS encryption key.

The simple change of moving from a text based encryption key to an encryption key object now allows our code to use infinitely many methods/providers to carry 
out the encryption without ever having to change the business logic. The MyBusinessService class can remain provider-agnostic, rather than directly call a particular hard-coded provider. 
The EncryptionKey object can carry all the information needed to tell the encryptionService code how perform the operation: What provider to use (HSM, JKS, AWS-KMS, etc.), what type of key it is (encryption vs. HMAC), 
and any other configuration that provider needs to actually perform the operation (algorithm, iv length, padding mode).

Adding or changing a provider or algorithm can then just be a configuration change rather than a change to business logic. Plus the code can now support multiple providers side by side, 
and can rotate keys without touching a single line of application code.

## Key resolution: how application code actually asks for a key

In the code snippet above the EncryptionKey object was statically bound to the class (in this case via a Spring properties configuration). Although there's nothing wrong with this, 
it does mean that the application needs to be re-deployed with a new properties file if it wants to rotate to a different key. The final step is to make this more dynamic by 
introducing a key resolution mechanism that allows the application to ask for a key by role at runtime. This way the application can ask for "the current encryption key" or 
"the current HMAC key" and the resolution mechanism will return the correct EncryptionKey object at runtime. This can allow us to store these EncryptionKey objects in a database 
or other dynamic configuration store and change them without having to redeploy the application. e.g:

```java
public class MyBusinessService {
	
    public void encryptAndStore(String plaintext) {
        EncryptionKey encryptionKey = keyProvider.getCurrentEncryptionKey(); // dynamically resolve the current encryption key at runtime
        String ciphertext = encryptionService.encrypt(encryptionKey, plaintext);
        database.save(ciphertext);
    }
}
```

Then the `keyProvider` getCurrentEncryptionKey() method can be implemented to resolve the current encryption key from a database, a configuration file, or any other dynamic source. 
This allows for key rotation and provider changes without changing the application code.
With something like the following 3 methods supported in the key provider class we finish the process of making this code dynamic and provider-agnostic:

* getCurrentEncryptionKey() - returns the current encryption key object. Used to get the key to encrypt data.
* getCurrentHmacKeys() - returns a list of current HMAC key objects. Used to calculate HMACs for searching and uniqueness checks.
* getKeyById(String keyId) - returns a specific key object by its unique identifier. Used to resolve the correct key for decrypting existing data, 
since the [structured ciphertext](structured-ciphertext.md) (explained in the next section) will record which key encrypted it.

## Why this is the foundation for everything that follows

This indirection — dropping textual alias/labels in favor of concrete key objects containing relevant configuration details — is what makes the rest of the talk possible:

- A [structured ciphertext](structured-ciphertext.md) can record *which* key encrypted it, and that alias resolution is what turns that record back into a usable key at decrypt time.
- [Key rotation](key-rotation.md) is just changing what "the current key" resolves to. Old ciphertext still decrypts correctly, because the key resolver still knows about old keys, not just the current one.
- New keys can use different providers or settings (algorithms, etc.) from the old ones. Business logic can remain provider-agnostic.
