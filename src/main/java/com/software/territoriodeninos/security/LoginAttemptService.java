package com.software.territoriodeninos.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para protección contra ataques de fuerza bruta (RNF7).
 * Bloquea temporalmente el inicio de sesión por 15 minutos tras 5 intentos fallidos consecutivos.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCKOUT_MINUTES = 15;
    public static final int MAX_CACHE_ENTRIES = 10_000;

    private static class AttemptRecord {
        int attempts;
        Instant lockedUntil;
        Instant lastAttempt;

        AttemptRecord(int attempts) {
            this.attempts = attempts;
            this.lastAttempt = Instant.now();
        }
    }

    private final Map<String, AttemptRecord> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key.toLowerCase().trim());
    }

    public void loginFailed(String key) {
        if (attemptsCache.size() >= MAX_CACHE_ENTRIES) {
            limpiarRegistrosExpirados();
        }

        String normalized = key.toLowerCase().trim();
        attemptsCache.compute(normalized, (k, record) -> {
            if (record == null) {
                return new AttemptRecord(1);
            }
            record.lastAttempt = Instant.now();
            if (record.lockedUntil != null && Instant.now().isAfter(record.lockedUntil)) {
                record.attempts = 1;
                record.lockedUntil = null;
                return record;
            }
            record.attempts++;
            if (record.attempts >= MAX_ATTEMPTS) {
                record.lockedUntil = Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES);
            }
            return record;
        });
    }

    public boolean isBlocked(String key) {
        String normalized = key.toLowerCase().trim();
        AttemptRecord record = attemptsCache.get(normalized);
        if (record == null) {
            return false;
        }
        if (record.lockedUntil != null) {
            if (Instant.now().isBefore(record.lockedUntil)) {
                return true;
            } else {
                attemptsCache.remove(normalized);
                return false;
            }
        }
        return false;
    }

    public long getRemainingLockoutMinutes(String key) {
        String normalized = key.toLowerCase().trim();
        AttemptRecord record = attemptsCache.get(normalized);
        if (record != null && record.lockedUntil != null) {
            long seconds = ChronoUnit.SECONDS.between(Instant.now(), record.lockedUntil);
            return Math.max(1, (seconds + 59) / 60);
        }
        return 0;
    }

    public void reset(String key) {
        attemptsCache.remove(key.toLowerCase().trim());
    }

    /**
     * Tarea programada para purgar registros expirados e inactivos cada 15 minutos,
     * previniendo fugas de memoria y ataques de agotamiento de recursos (DoS).
     */
    @Scheduled(fixedRate = 900_000)
    public void limpiarRegistrosExpirados() {
        Instant limiteInactividad = Instant.now().minus(LOCKOUT_MINUTES, ChronoUnit.MINUTES);
        Instant ahora = Instant.now();
        attemptsCache.entrySet().removeIf(entry -> {
            AttemptRecord record = entry.getValue();
            if (record.lockedUntil != null) {
                return ahora.isAfter(record.lockedUntil);
            }
            return record.lastAttempt != null && record.lastAttempt.isBefore(limiteInactividad);
        });
    }
}
