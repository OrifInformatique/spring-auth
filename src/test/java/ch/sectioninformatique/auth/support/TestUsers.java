package ch.sectioninformatique.auth.support;

/**
 * Reference users created by {@link TestUserSeeder}.
 */
public final class TestUsers {

    public static final String USER = "test.user@test.com";
    public static final String USER_PASSWORD = "Test1234!";

    public static final String MANAGER = "test.manager@test.com";
    public static final String MANAGER_PASSWORD = "ManagerTest123!";

    public static final String ADMIN = "test.admin@test.com";
    public static final String ADMIN_PASSWORD = "AdminTest123!";

    /** Second administrator, target of admin-on-admin operations. */
    public static final String OTHER_ADMIN = "test.admin2@test.com";

    /** Soft-deleted user (USER role, password {@link #USER_PASSWORD}). */
    public static final String DELETED = "deleted.user@test.com";

    private TestUsers() {
    }
}
