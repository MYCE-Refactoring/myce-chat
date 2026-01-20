package com.myce.api.auth.dto;

import com.myce.common.type.LoginType;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails, Principal {

    private final LoginType loginType;
    private final Long memberId;
    private final String role;

    @Builder
    public CustomUserDetails(LoginType loginType, Long memberId, String role) {
        this.loginType = loginType;
        this.memberId = memberId;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return "";
    }

    public Long getUserId(){
        return this.memberId;
    }

    @Override
    public String getName() {
        return "";
    }
}
