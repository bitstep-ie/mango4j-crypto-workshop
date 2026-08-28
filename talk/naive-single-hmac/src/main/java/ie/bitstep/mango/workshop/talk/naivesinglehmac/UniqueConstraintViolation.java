package ie.bitstep.mango.workshop.talk.naivesinglehmac;

/** Simulates a DB-level unique constraint violation on the single HMAC column. */
public final class UniqueConstraintViolation extends RuntimeException {

    public UniqueConstraintViolation(String message) {
        super(message);
    }
}
