package ie.bitstep.mango.workshop.talk.naivesinglehmac;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Companion demo for {@code docs/talk/single-hmac.md}: one HMAC column, one key, and
 * what happens to search and to the unique constraint the moment that key rotates.
 *
 * <p>Run with {@code mvn -f talk/naive-single-hmac/pom.xml exec:java}.
 */
public final class Main {

    public static void main(String[] args) {
        HmacService hmacService = new HmacService();
        UserStore store = new UserStore();
        String username = "john.doe@test.com";

        System.out.println("== Naive single HMAC demo ==");
        System.out.println();

        SecretKey keyV1 = HmacService.newKey();
        String hmacUnderV1 = hmacService.hmac(username, keyV1);
        store.createUser(username, hmacUnderV1);
        System.out.println("Created user '" + username + "' hashed under keyV1.");
        System.out.println();

        System.out.println("-- The HMAC key rotates: keyV2 is now current --");
        SecretKey keyV2 = HmacService.newKey();
        System.out.println();

        System.out.println("-- The search problem --");
        String searchHmac = hmacService.hmac(username, keyV2);
        List<UserStore.UserRecord> naiveResults = store.findByUsernameHmac(searchHmac);
        System.out.println("Searching for '" + username + "' by hashing with only the current key (keyV2): "
                + naiveResults.size() + " result(s) found.");
        System.out.println("PITFALL: the existing record is invisible to search until a background job");
        System.out.println("rekeys it, even though it's sitting right there in the table.");
        System.out.println();

        System.out.println("-- The unique constraint problem --");
        System.out.println("A request arrives to create ANOTHER user with the same username: '"
                + username + "'.");
        try {
            store.createUser(username, searchHmac);
            System.out.println("createUser() succeeded a SECOND time for the same username.");
            System.out.println("PITFALL: the unique constraint never fired, because the two HMAC values");
            System.out.println("genuinely differ (old key vs. new key). The database now has "
                    + store.allRows().size() + " rows for one logical username.");
        } catch (UniqueConstraintViolation e) {
            System.out.println("Unique constraint correctly rejected the duplicate: " + e.getMessage());
        }
    }
}
