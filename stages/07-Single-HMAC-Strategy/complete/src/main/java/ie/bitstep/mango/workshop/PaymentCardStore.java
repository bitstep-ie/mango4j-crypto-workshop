package ie.bitstep.mango.workshop;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A tiny stand-in for a real table/column: this store never sees
 * cardNumber, only cardNumberHmac. That's the whole trick behind using an
 * HMAC for search and uniqueness - equality comparisons run against a
 * deterministic value, without the store ever holding plaintext.
 */
public class PaymentCardStore {

    private final List<PaymentCardEntity> records = new ArrayList<>();

    // --8<-- [start:find-by-hmac]
    public Optional<PaymentCardEntity> findByHmac(String hmac) {
        // The previous stages had no HMAC to search by at all.
        Optional<PaymentCardEntity> match = Optional.empty();

        match = records.stream()
                .filter(record -> record.getCardNumberHmac().equals(hmac))
                .findFirst();

        return match;
    }
    // --8<-- [end:find-by-hmac]

    // --8<-- [start:save]
    /**
     * Enforces uniqueness the same way search works: by HMAC equality.
     * Returns false (and doesn't save) if a record with the same HMAC
     * already exists.
     */
    public boolean save(PaymentCardEntity entity) {
        if (findByHmac(entity.getCardNumberHmac()).isPresent()) {
            return false;
        }
        records.add(entity);
        return true;
    }
    // --8<-- [end:save]
}
