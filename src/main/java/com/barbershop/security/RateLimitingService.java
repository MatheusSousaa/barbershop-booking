package com.barbershop.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Buckets por IP mantêm limites de login e agendamento independentes. */
@Component
public class RateLimitingService {

    private static final long LOGIN_REQUESTS_PER_MINUTE = 5;
    private static final long APPOINTMENT_REQUESTS_PER_MINUTE = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    private final Map<String, Bucket> appointmentBuckets = new ConcurrentHashMap<>();

    public boolean tryConsumeLogin(String ip) {
        Bucket bucket = loginBuckets.computeIfAbsent(ip,
                key -> createBucket(LOGIN_REQUESTS_PER_MINUTE, RATE_LIMIT_WINDOW));
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeAppointment(String ip) {
        Bucket bucket = appointmentBuckets.computeIfAbsent(ip,
                key -> createBucket(APPOINTMENT_REQUESTS_PER_MINUTE, RATE_LIMIT_WINDOW));
        return bucket.tryConsume(1);
    }

    private Bucket createBucket(long capacity, Duration duration) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, duration)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
