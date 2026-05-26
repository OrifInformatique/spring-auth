package ch.sectioninformatique.auth.auth;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserService;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthCodeRepository authCodeRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserAuthenticationProvider userAuthenticationProvider;

    private static final Logger log = LoggerFactory.getLogger(AuthServiceTest.class);

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(
            authService,
            "lifetime",
            Duration.ofMinutes(10));

    }

    @Test
    public void generateAndStoreAuthCode_Success(){


        String userLogin = "test.user@test.com";
        String redirectUrl = "redirectUrl";
        
        String code = authService.generateAndStoreAuthCode(userLogin, redirectUrl);
        log.error("Code {}", code);

        List<AuthCode> authCodeRetrieve = authCodeRepository.findByUserLogin(userLogin);

        assertTrue(!authCodeRetrieve.isEmpty(), "The list is empty");
        assertTrue(!code.isEmpty(), "The auth code's value is : " + code);
    }

    /**
     * Test : retrieve and delete a code
     * Verify that the code is found, and deleted, and return a JWT
     * 
     * Test data:
     * - a user's login
     * - A redirect Url
     */

    @Test
    public void retrieveAndDelete_Success(){
        String userLogin = "test.user@test.com";
        String redirectUrl = "redirectUrl";

        String code = authService.generateAndStoreAuthCode(userLogin, redirectUrl);

        String jwt = authService.retrieveAndDeleteAuthCode(code, userLogin);
        log.error("jwt :  {}", jwt);

        //Assert
        assertTrue(!jwt.isEmpty());
        

    }
    
}
