package ch.sectioninformatique.auth.user;

/**
 * Filter of the user list ({@code GET /users?status=...}).
 */
public enum UserStatus {
    /** Users that are not soft-deleted (default). */
    ACTIVE,
    /** Soft-deleted users only. */
    DELETED,
    /** Every user. */
    ALL
}
