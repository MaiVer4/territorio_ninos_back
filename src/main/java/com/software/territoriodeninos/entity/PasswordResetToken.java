package com.software.territoriodeninos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @OneToOne(targetEntity = Usuario.class, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.fechaExpiracion);
    }
}
