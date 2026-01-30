package com.myce.api.auth.config;

import com.myce.api.auth.filter.JwtAuthenticationFilter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${internal.auth.value}")
    private String INTERNAL_AUTH_VALUE;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(INTERNAL_AUTH_VALUE);

        http.csrf(AbstractHttpConfigurer::disable) // CSRF 공격 방지 기능 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // 기본 로그인 폼 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            throw new CustomException(CustomErrorCode.INVALID_TOKEN);
                        }))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterAfter(jwtFilter, LogoutFilter.class);

//        http.cors(cors -> cors.configurationSource(request -> {
//            CorsConfiguration config = new CorsConfiguration();
//            config.setAllowedOriginPatterns(List.of("*"));
//            config.setAllowedMethods(List.of("*"));
//            config.setAllowedHeaders(List.of("*"));
//            return config;
//        }));

        http.authorizeHttpRequests(auth ->
                auth.requestMatchers("/ws/**", "/actuator/**").permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }
}


