package ch.sectioninformatique.auth.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ch.sectioninformatique.auth.user.User;
import ch.sectioninformatique.auth.user.UserRepository;

/** 
 * Test class for {@link AuthCode}.
 * This class tests the functionality of the AuthCode entity, including:
 * - Register new codes
 * - Getting all codes from a user
 * - Deleting all codes expired
 * - Empty response if no code is found for a user
 */

@SpringBootTest
public class AuthCodeTest {
    
    private final Long TEST_codeId = 1L;
    private final String TEST_code = "ThisIsATestCode";
    private final String TEST_userLogin = "test.admin@test.com";
    private final Instant TEST_expiresAt = Instant.now().plusSeconds(30); // Expires in 30 seconds
    private final Instant TEST_createdAt = Instant.now();

    @Autowired
    private AuthCodeRepository authCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createAuthCode() {
        AuthCode authCode = new AuthCode();
        authCode.setId(TEST_codeId);
        authCode.setCode(TEST_code);
        authCode.setUserLogin(TEST_userLogin);
        authCode.setExpiresAt(TEST_expiresAt);
        authCode.setCreatedAt(TEST_createdAt);

        assert authCode.getId().equals(TEST_codeId);
        assert authCode.getCode().equals(TEST_code);
        assert authCode.getUserLogin().equals(TEST_userLogin);
        assert authCode.getExpiresAt().equals(TEST_expiresAt);
        assert authCode.getCreatedAt().equals(TEST_createdAt);

    }

    @Test
    void getCodeByUserLogin() {

        AuthCode authCode = new AuthCode();
        authCode.setCode(TEST_code);
        authCode.setUserLogin(TEST_userLogin);
        authCode.setExpiresAt(TEST_expiresAt);
        authCode.setCreatedAt(TEST_createdAt);
        authCodeRepository.save(authCode);

        AuthCode secondCode = new AuthCode();
        secondCode.setCode(TEST_code + ".2");
        secondCode.setUserLogin(TEST_userLogin);
        secondCode.setExpiresAt(TEST_expiresAt);
        secondCode.setCreatedAt(TEST_createdAt);
        authCodeRepository.save(secondCode);

        List<AuthCode> retrievedAuthCode = authCodeRepository.findByUserLogin(TEST_userLogin);
        System.out.println("Retrieved auth codes for user " + TEST_userLogin + ": " + retrievedAuthCode.size());

        
        assert !retrievedAuthCode.isEmpty();
    }
assert authCode.getId().equals(TEST_codeId);
    @Test
    void getAuthCodeFromUserWithNoCode() {
        Optional<User> user = userRepository.findByLogin("test.user@test.com");
        List<AuthCode> authCode = authCodeRepository.findByUserLogin(user.get().getLogin());

        assert authCode.isEmpty();
    }

    @Test
    void deleteExpiredCodes() {
        AuthCode authCode = new AuthCode();
        authCode.setCode("ExpiredTestCode");
        authCode.setUserLogin("test.manager@test.com");
        authCode.setExpiresAt(Instant.now().minusSeconds(5)); 
        authCode.setCreatedAt(Instant.now());
        
        authCodeRepository.save(authCode);

        

        authCodeRepository.deleteExpiredCodes();

        List<AuthCode> retrievedAuthCodes = authCodeRepository.findByUserLogin("test.manager@test.com");

        assertTrue(retrievedAuthCodes.isEmpty(), "Expired authentication code should have been deleted but get : " + retrievedAuthCodes.size());


    }      
}
