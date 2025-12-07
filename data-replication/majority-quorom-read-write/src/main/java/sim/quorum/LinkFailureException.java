package sim.quorum;

final class LinkFailureException extends RuntimeException {
    LinkFailureException(String message) {
        super(message);
    }
}
