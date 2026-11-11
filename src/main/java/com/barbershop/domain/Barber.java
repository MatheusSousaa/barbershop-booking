package com.barbershop.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Conta do barbeiro usada pelo Spring Security para autenticar o painel. */
@Entity
@Table(name = "barbers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barber implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Senha armazenada como hash BCrypt, codificado antes de persistir. */
    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String displayName;

    @Email
    @Size(max = 150)
    @Column(length = 150)
    private String email;

    // Getters explícitos evitam ambiguidade com Lombok ao implementar UserDetails no JDK 26.
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** A conta atual sempre acessa o painel com a role BARBER. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_BARBER"));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
