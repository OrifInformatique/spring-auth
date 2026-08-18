package ch.sectioninformatique.auth.auth;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.sectioninformatique.auth.auth.AuthExceptions.AuthCodeNotFoundException;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
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

        log.debug("Retrieving and deleting auth code for user: {}", userLogin);
        authCodeRepository.deleteExpiredCodes(); 
        List<AuthCode> authCodes = authCodeRepository.findByUserLogin(userLogin);

        if(authCodes.isEmpty()){
            log.debug("No auth codes found for user: {}", userLogin);
            throw new AuthCodeNotFoundException();
        }
        
        for (AuthCode authCode : authCodes){
            if(userService.hash(code).equals(authCode.getCode())){
                log.debug("Found matching auth code for user: {}", userLogin);
                authCodeRepository.delete(authCode);
                UserDto user = userService.findByLogin(userLogin);
                String jwt = userAuthenticationProvider.createToken(user);
                return jwt;
            }
        }

        throw new AuthCodeNotFoundException();
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
        log.debug("Hashed auth code for user: {}", userLogin);
        
        Instant expiredAt = Instant.now().plus(lifetime);
        
        AuthCode authCode =  new AuthCode();
        authCode.setCode(hash);
        authCode.setUserLogin(userLogin);
        authCode.setRedirectUrl(redirectUrl);
        authCode.setExpiresAt(expiredAt);
        
        authCodeRepository.save(authCode);
        log.debug("Auth code saved for user: {}", userLogin);
        
        return code;
    }
}
