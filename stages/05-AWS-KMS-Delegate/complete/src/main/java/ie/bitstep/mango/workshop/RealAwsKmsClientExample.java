package ie.bitstep.mango.workshop;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

/**
 * Reference only - nothing in this class is ever called from {@link Main}.
 * It's here so it compiles (proving the construction is wired correctly),
 * without ever being run: actually calling {@link #realKmsClient()} needs
 * real AWS credentials and network access to a real KMS endpoint, which
 * this workshop deliberately doesn't require. CI compiles this class but
 * never executes it, for exactly that reason.
 */
public final class RealAwsKmsClientExample {

    private RealAwsKmsClientExample() {
    }

    // --8<-- [start:real-kms-client]
    /**
     * This is the only line that changes to go from this stage's
     * {@link FakeKmsClient} to a real deployment - everything else
     * ({@code AwsKmsEncryptionServiceDelegate}, the {@code CryptoKey}
     * configuration, the {@code encrypt()}/{@code decrypt()} call sites)
     * stays identical. {@code KmsClient.builder().build()} resolves
     * credentials and region from the standard AWS SDK default chain
     * (environment variables, {@code ~/.aws/credentials}, an EC2/ECS/Lambda
     * IAM role, and so on) - override either explicitly when that default
     * isn't what you want, as shown here.
     */
    static KmsClient realKmsClient() {
        return KmsClient.builder()
                .region(Region.EU_WEST_1)
                .build();
    }
    // --8<-- [end:real-kms-client]
}
