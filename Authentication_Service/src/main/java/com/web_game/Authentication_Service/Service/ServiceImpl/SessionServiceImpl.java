package com.web_game.Authentication_Service.Service.ServiceImpl;

import com.web_game.Authentication_Service.Service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SessionServiceImpl implements SessionService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${jwt.expiration}")
    private long expiration;

    public void saveSession(String username, String token) {
        redisTemplate.opsForValue().set("session:" + username, token, expiration, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenValid(String username, String token) {
        String storedToken = redisTemplate.opsForValue().get("session:" + username);
        return token.equals(storedToken);
    }

    public void removeSession(String username) {
        redisTemplate.delete("session:" + username);
    }
}