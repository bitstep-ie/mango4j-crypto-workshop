package ie.bitstep.mango.workshop.talk.naivesinglehmac;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The naive "one HMAC column" design from {@code docs/talk/single-hmac.md}: a
 * {@code USERNAME_HMAC} column sitting next to the encrypted {@code userName}, with a
 * DB-level unique constraint on that single HMAC value. Encryption itself is beside the
 * point here, so {@code username} stands in for what would really be a ciphertext blob.
 */
public final class UserStore {

    private final List<UserRecord> rows = new ArrayList<>();
    private final Set<String> uniqueHmacIndex = new HashSet<>();

    // --8<-- [start:unique-constraint]
    public void createUser(String username, String usernameHmac) {
        // Mirrors a DB unique index on USERNAME_HMAC: it can only ever see the hash
        // value itself, so two different hashes for the same underlying username
        // look, to the constraint, like two entirely unrelated rows.
        if (!uniqueHmacIndex.add(usernameHmac)) {
            throw new UniqueConstraintViolation("username_hmac already exists: " + usernameHmac);
        }
        rows.add(new UserRecord(username, usernameHmac));
    }
    // --8<-- [end:unique-constraint]

    // --8<-- [start:naive-search]
    /** Naive search: hash the term with whatever key is current right now, and look it up. */
    public List<UserRecord> findByUsernameHmac(String usernameHmac) {
        return rows.stream().filter(r -> r.usernameHmac().equals(usernameHmac)).toList();
    }
    // --8<-- [end:naive-search]

    public List<UserRecord> allRows() {
        return List.copyOf(rows);
    }

    public record UserRecord(String username, String usernameHmac) {
    }
}
