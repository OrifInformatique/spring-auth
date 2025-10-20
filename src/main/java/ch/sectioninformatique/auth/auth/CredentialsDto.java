package ch.sectioninformatique.auth.auth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object for user credentials.
 * This record class holds the login and password information used for user authentication.
 *
 * @param login The user's login identifier
 * @param password The user's password as a character array
 */
public class CredentialsDto {

    @NotBlank(message = "Login is required")
    private String login;

    @NotNull(message = "Password is required")
    private char[] password;

    // Constructors
    public CredentialsDto() {}

    public CredentialsDto(String login, char[] password) {
        this.login = login;
        this.password = password;
    }

    // Getters & setters
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public char[] getPassword() { return password; }
    public void setPassword(char[] password) { this.password = password; }
}
