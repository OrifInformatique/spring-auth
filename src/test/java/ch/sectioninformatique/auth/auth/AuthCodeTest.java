package ch.sectioninformatique.auth.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ch.sectioninformatique.auth.user.User;
import ch.sectioninformatique.auth.user.UserRepository;
import jakarta.transaction.Transactional;

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
    
    private final String TEST_code = "ThisIsATestCode";
    private final String TEST_userLogin = "test.admin@test.com";
    private final Instant TEST_expiresAt = Instant.now().plusSeconds(30); // Expires in 30 seconds
    private final Instant TEST_createdAt = Instant.now();
    private final String TEST_redirectURL = "NotARealURL";

    @Autowired
    private AuthCodeRepository authCodeRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Test : Create a new code, then stock in the database and finally, retrieves it.
     * 
     * Test data:
     * -AuthCode
     */
    @Test
    void createAuthCode() {
        authCodeRepository.deleteAll();
        Instant now = Instant.now();
        Instant expiredAt = Instant.now().plusSeconds(30);

        AuthCode authCode = new AuthCode();
        authCode.setCode(TEST_code);
        authCode.setUserLogin(TEST_userLogin);
        authCode.setRedirectUrl(TEST_redirectURL);
        authCode.setExpiresAt(expiredAt);
        authCode.setCreatedAt(now);
        authCodeRepository.save(authCode);

        Optional<AuthCode> retrievedAuthCode = authCodeRepository.findById(authCode.getId()); 


        assertEquals(TEST_userLogin, retrievedAuthCode.get().getUserLogin());
        assertEquals(TEST_code, retrievedAuthCode.get().getCode());
        assertEquals(now.truncatedTo(ChronoUnit.MICROS), retrievedAuthCode.get().getCreatedAt().truncatedTo(ChronoUnit.MICROS));
        assertEquals(expiredAt.truncatedTo(ChronoUnit.MICROS), retrievedAuthCode.get().getExpiresAt().truncatedTo(ChronoUnit.MICROS));
        assertEquals(TEST_redirectURL, retrievedAuthCode.get().getRedirectUrl());        
    }

    /**
     * Test : Create 2 codes for a user, then stock them, and finally, retrieve them.
     * Verify that the list contains 2 Elements
     * 
     * Test Data:
     * - 2 AuthCode
     */
    @Test
    void getCodeByUserLogin() {
        authCodeRepository.deleteAll();
        AuthCode authCode = new AuthCode();
        authCode.setCode(TEST_code);
        authCode.setUserLogin(TEST_userLogin);
        authCode.setExpiresAt(TEST_expiresAt);
        authCode.setRedirectUrl(TEST_redirectURL);;
        authCode.setCreatedAt(TEST_createdAt);
        authCodeRepository.save(authCode);

        AuthCode secondCode = new AuthCode();
        secondCode.setCode(TEST_code + ".2");
        secondCode.setUserLogin(TEST_userLogin);
        secondCode.setExpiresAt(TEST_expiresAt);
        secondCode.setRedirectUrl(TEST_redirectURL);
        secondCode.setCreatedAt(TEST_createdAt);
        authCodeRepository.save(secondCode);

        List<AuthCode> retrievedAuthCode = authCodeRepository.findByUserLogin(TEST_userLogin);
        System.out.println("Retrieved auth codes for user " + TEST_userLogin + ": " + retrievedAuthCode.size());

        
        assertTrue(!retrievedAuthCode.isEmpty());
        assertEquals(2, retrievedAuthCode.size());
    }

    /**
     * Test : Getting codes from a user without codes.
     * Verify that the method doesn't throw an error
     * 
     * Test Data:
     * - user's login
     */
    @Test
    void getAuthCodeFromUserWithNoCode() {
        Optional<User> user = userRepository.findByLogin("test.user@test.com");
        List<AuthCode> authCode = authCodeRepository.findByUserLogin(user.get().getLogin());

        assertTrue(authCode.isEmpty());
    }

    /**
     * Test : Delete expired codes
     * Verify that expired codes are deleted
     * 
     * Test Data:
     * - expired code
     */
    @Test
    @Transactional
    void deleteExpiredCodes() {
        authCodeRepository.deleteAll();
        AuthCode authCode = new AuthCode();
        authCode.setCode("ExpiredTestCode");
        authCode.setUserLogin("test.manager@test.com");
        authCode.setRedirectUrl(TEST_redirectURL);
        authCode.setExpiresAt(Instant.now().minusSeconds(5)); 
        authCode.setCreatedAt(Instant.now());
        
        authCodeRepository.save(authCode);

        List<AuthCode> retrievedBeforeDelete = authCodeRepository.findByUserLogin(authCode.getUserLogin());

        authCodeRepository.deleteExpiredCodes();

        List<AuthCode> retrievedAuthCodes = authCodeRepository.findByUserLogin("test.manager@test.com");

        assertTrue(!retrievedBeforeDelete.isEmpty());
        assertTrue(retrievedAuthCodes.isEmpty(), "Expired authentication code should have been deleted but get : " + retrievedAuthCodes.size());


    }      
}
