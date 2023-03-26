package com.codesoom.assignment.application;

import com.codesoom.assignment.domain.User;
import com.codesoom.assignment.domain.UserRepository;
import com.codesoom.assignment.dto.SessionResponseData;
import com.codesoom.assignment.errors.InvalidTokenException;
import com.codesoom.assignment.errors.LoginFailException;
import com.codesoom.assignment.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class AuthenticationService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository,
                                 JwtUtil jwtUtil,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public SessionResponseData login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new LoginFailException(email));

        if (!user.authenticate(password,passwordEncoder)) {
            throw new LoginFailException(email);
        }
        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);
        user.registRefreshToken(refreshToken);

        return SessionResponseData.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

    }

    public Claims parseToken(String accessToken) {
        return jwtUtil.decode(accessToken);
    }


    public SessionResponseData reissueAccessToken(String refreshToken) {
        User user = userRepository.findByRefreshTokenAndDeletedIsFalse(refreshToken)
                .orElseThrow(() -> new InvalidTokenException(refreshToken));

        String accessToken = jwtUtil.createAccessToken(user);

        return SessionResponseData.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
