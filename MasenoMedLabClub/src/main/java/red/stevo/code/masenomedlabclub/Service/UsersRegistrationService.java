package red.stevo.code.masenomedlabclub.Service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import red.stevo.code.masenomedlabclub.ControllerAdvice.custom.EntityDeletionException;
import red.stevo.code.masenomedlabclub.ControllerAdvice.custom.UserAlreadyExistException;
import red.stevo.code.masenomedlabclub.ControllerAdvice.custom.UsersCreationFailedException;
import red.stevo.code.masenomedlabclub.Entities.Roles;
import red.stevo.code.masenomedlabclub.Entities.Users;
import red.stevo.code.masenomedlabclub.Entities.tokens.RefreshTokens;
import red.stevo.code.masenomedlabclub.Models.RequestModels.LoginRequests;
import red.stevo.code.masenomedlabclub.Models.RequestModels.ResetPasswordDetails;
import red.stevo.code.masenomedlabclub.Models.RequestModels.UsersRegistrationRequests;
import red.stevo.code.masenomedlabclub.Models.ResponseModel.AuthenticationResponse;
import red.stevo.code.masenomedlabclub.Models.ResponseModel.UserGeneralResponse;
import red.stevo.code.masenomedlabclub.Repositories.users.RefreshTokensRepository;
import red.stevo.code.masenomedlabclub.Repositories.users.UsersRepository;
import red.stevo.code.masenomedlabclub.Service.DetService.EmailService;
import red.stevo.code.masenomedlabclub.Service.DetService.JWTGenService;
import red.stevo.code.masenomedlabclub.configurations.PasswordGenerator;
import red.stevo.code.masenomedlabclub.filter.CookieUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class UsersRegistrationService {

    private final UsersRepository usersRepository;
    private final JWTGenService jwtGenService;
    private final RefreshTokensRepository refreshTokensRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PasswordGenerator passwordGenerator;
    private final CookieUtils cookieUtils;
    private final EmailService emailService;
    private final HttpServletResponse response;


    public List<String> createUser(List<UsersRegistrationRequests> regRequest) {
        List<String> createdEmails = new ArrayList<>();
        List<Users> users = regRequest.stream()
                .map(usersRegistrationRequests -> {
                    Users users1 = new Users();
                    if (!isEmailValid(usersRegistrationRequests.getEmail())) {
                        try {
                            throw new InvalidPropertiesFormatException("invalid email format");
                        } catch (InvalidPropertiesFormatException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    users1.setEmail(usersRegistrationRequests.getEmail());
                    String password = passwordGenerator.generateRandomPassword(8);
                    System.out.println(password);
                    log.error("your default password is " + password);
                    users1.setPassword(passwordEncoder.encode(password));
                    //emailService.sendRegistrationEmail(users1.getEmail(),password);
                    users1.setRole(usersRegistrationRequests.getRoles());
                    users1.setEnabled(true);
                    if (usersRepository.existsByEmail(usersRegistrationRequests.getEmail())){
                        throw new UserAlreadyExistException("the user with that email already exists");
                    }
                    createdEmails.add(users1.getEmail());
                    return users1;

                }).toList();
        usersRepository.saveAll(users);
        return createdEmails;
    }

    private boolean isEmailValid(String email) {
        org.apache.commons.validator.routines.EmailValidator emailValidator = org.apache.commons.validator.routines.EmailValidator.getInstance();
        return emailValidator.isValid(email);
    }

    public ResponseEntity<AuthenticationResponse> loginUser(LoginRequests loginRequests) {
        // Authenticate the user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequests.getEmail(), loginRequests.getPassword()));

        // Fetch user details
        Users user = usersRepository.findByEmail(loginRequests.getEmail());

        // Generate access and refresh tokens
        String accessToken = jwtGenService.generateAccessToken(user);




        // Set the access token in a secure cookie


        AuthenticationResponse authResponse = new AuthenticationResponse();
        authResponse.setMessage("Authentication successful.");
        authResponse.setToken(accessToken);
        authResponse.setUserId(user.getUserId());
        authResponse.setUserRole(user.getRole().toString());

        response.setHeader("Set-Cookie", cookieUtils.responseCookie(user).toString());

        // Return an AuthenticationResponse object containing both tokens
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }


    public UserGeneralResponse resetPassword(ResetPasswordDetails resetPasswordDetails) {
        log.info("Service to reset the password");

        // Find user by email
        Users user = usersRepository.findByEmail(resetPasswordDetails.getEmail());
        if (user == null) {
            throw new UsernameNotFoundException("User does not exist");
        }

        // Validate the old password by comparing it with the encoded password in the database
        if (!passwordEncoder.matches(resetPasswordDetails.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid current password");
        }

        // Set the new password
        String newPassword = resetPasswordDetails.getNewPassword();
        if (!isPasswordStrong(resetPasswordDetails.getNewPassword())){
            throw new IllegalArgumentException("Weak password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));

        // Save the updated user with the new password
        usersRepository.save(user);

        UserGeneralResponse userGeneralResponse = new UserGeneralResponse();
        userGeneralResponse.setMessage("Password updated successfully.");
        userGeneralResponse.setDate(new Date());
        userGeneralResponse.setHttpStatus(HttpStatus.OK);

        return userGeneralResponse;
    }

    private static boolean isPasswordStrong(String password){
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$!)(}{%^&+=])(?=\\S+$).{8,}$";
        return password.matches(passwordRegex);
    }


    public void deleteUser(List<String> emails){
        log.info("Service to delete the user");
        try {

            List<Users> usersList = emails.stream().map(
                    email1 -> {
                        Users user = usersRepository.findByEmail(email1);
                        if (user == null) throw new UsernameNotFoundException("User does not exist");
                        return user;
                    }

            ).toList();
            usersRepository.deleteAll(usersList);

        }catch (Exception ex){
            throw new EntityDeletionException("could not delete the user");
        }

    }
    @Value("${default-email}")
    @Email
    private String adminEmail;
    @Value("${default-password}")
    private String adminPassword;
    @PostConstruct
    public void createAdmin(){
        try {
            Users user = new Users();

            user.setEmail(adminEmail);
            if (!isPasswordStrong(adminPassword)){
                throw new IllegalArgumentException("Weak password");
            }
            user.setPassword(passwordEncoder.encode(adminPassword));
            user.setRole(Roles.ADMIN);
            user.setEnabled(true);
            if (usersRepository.existsByEmail(adminEmail)){
                return;
            }
            usersRepository.save(user);
            log.info("default admin is created");


        }catch (Exception ex){
            throw new UsersCreationFailedException("could not create the default admin", ex.getCause());
        }
    }

}
