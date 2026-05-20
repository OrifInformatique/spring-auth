package ch.sectioninformatique.auth.auth;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;

import java.time.Duration;
import java.time.Instant;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.sectioninformatique.auth.auth.AuthExceptions.AuthCodeNotFoundException;
import ch.sectioninformatique.auth.user.UserDto;

import ch.sectioninformatique.auth.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService {

    private final UserAuthenticationProvider userAuthenticationProvider;
    private final UserService userService;
    private final AuthCodeRepository authCodeRepository;

    @Value("${SECURITY_AUTHENTICATION_CODES_LIFETIME}")
    private Duration lifetime;

    /**
     * Methods to retrieve and delete a code.
     * This method :
     * - First deletes all expired codes with the authCodeRepository.deleteExpiredCodes()
     * - Then, retrieve all codes from the user's login
     * - Then, check that the code is valid
     * - Then, creates a JWT token and returns it
     * 
     * @param userLogin The user's login
     * @param code the code to retrieve
     * @param redirectUrl The redirect url
     * 
     * @return a JWT token
     */
    public String retrieveAndDeleteAuthCode(String code, String userLogin){


        authCodeRepository.deleteExpiredCodes();
        Optional<AuthCode> authCode = authCodeRepository.findByCodeAndUserLogin(code, userLogin);
        
        if(authCode.isEmpty()){
            throw new AuthCodeNotFoundException();
        }
        UserDto user = userService.findByLogin(userLogin);
            String jwt = userAuthenticationProvider.createToken(user);
            return jwt;

    }
    
    /**
     * Generates, hashes, and stores a new AuthCode in database.
     * This method:
     * -Clear expired codes with authCodeRepository.deleteExpiredCodes()
     * -Creates new AuthCode and hashes it
     * -Store the hashed code in the database
     * 
     * @param userLogin the user's login
     * @param redirectUrl the redirect url
     * @return the non-hasshed code
     */
    public String generateAndStoreAuthCode(String userLogin, String redirectUrl){   
    
    authCodeRepository.deleteExpiredCodes();

        String code = UUID.randomUUID().toString();
        String hash = userService.hash(code);

        Instant expiredAt = Instant.now().plus(lifetime);
        
        AuthCode authCode = new AuthCode();
        authCode.setCode(hash);
        authCode.setUserLogin(userLogin);
        authCode.setRedirectUrl(redirectUrl);
        authCode.setCreatedAt(Instant.now());
        authCode.setExpiresAt(expiredAt);
        
        authCodeRepository.save(authCode);
        
        return code;

    }

}
