package ch.sectioninformatique.auth.auth;

/**
 * Data transfer object for user registration.
 * This record class holds the information required to create a new user account.
 *
 * @param firstName The user's first name
 * @param lastName The user's last name
 * @param login The user's login identifier
 * @param password The user's password as a character array
 */
public record SignUpDto (String firstName, String lastName, String login, char[] password) { }
