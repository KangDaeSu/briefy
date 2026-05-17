package com.briefy.infra.security;

import com.briefy.domain.user.AuthProvider;
import com.briefy.domain.user.User;
import com.briefy.domain.user.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        existing -> {
                            existing.linkOAuth(AuthProvider.GOOGLE, providerId);
                            userRepository.save(existing);
                        },
                        () -> userRepository.save(
                                new User(email, name != null ? name : email,
                                        AuthProvider.GOOGLE, providerId))
                );

        // email을 nameAttributeKey로 사용하여 OAuth2SuccessHandler에서 조회 가능하게 함
        return new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(oAuth2User.getAttributes())),
                oAuth2User.getAttributes(),
                "email"
        );
    }
}
