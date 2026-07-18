package com.shou.lims.security.service;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@Getter
public class SecurityUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Integer authVersion;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUserDetails(Long userId, String username, String password,
                               boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        this(userId, username, password, enabled, authorities, 0);
    }

    public SecurityUserDetails(Long userId, String username, String password,
                               boolean enabled, Collection<? extends GrantedAuthority> authorities,
                               Integer authVersion) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
        this.authVersion = authVersion == null ? 0 : authVersion;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
