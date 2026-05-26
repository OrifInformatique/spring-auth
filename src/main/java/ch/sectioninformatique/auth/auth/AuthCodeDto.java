package ch.sectioninformatique.auth.auth;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthCodeDto {

    private String login;
    private String code;
    
}
