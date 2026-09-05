package com.hsf.hotel.common.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPT = 5;
    private static final long LOCK_TIME_DURATION = TimeUnit.MINUTES.toMillis(15);
    private final ConcurrentHashMap<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    private static class Attempt {
        int count;
        long lastAttemptTime;
        Attempt(int count, long lastAttemptTime) {
            this.count = count;
            this.lastAttemptTime = lastAttemptTime;
        }
    }

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        long now = System.currentTimeMillis();
        Attempt attempt = attemptsCache.get(key);
        if (attempt == null || (now - attempt.lastAttemptTime) > LOCK_TIME_DURATION) {
            attemptsCache.put(key, new Attempt(1, now));
        } else {
            attempt.count++;
            attempt.lastAttemptTime = now;
            attemptsCache.put(key, attempt);
        }
    }

    public boolean isBlocked(String key) {
        Attempt attempt = attemptsCache.get(key);
        if (attempt == null) return false;
        if ((System.currentTimeMillis() - attempt.lastAttemptTime) > LOCK_TIME_DURATION) {
            attemptsCache.remove(key);
            return false;
        }
        return attempt.count >= MAX_ATTEMPT;
    }
}
