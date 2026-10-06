package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    // Bulk delete: se ejecuta inmediatamente, antes del INSERT del nuevo token
    // (evita violación de la unique constraint sobre usuario_id).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken t WHERE t.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}
