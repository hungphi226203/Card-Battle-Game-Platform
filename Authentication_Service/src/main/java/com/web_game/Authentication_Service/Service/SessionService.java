package com.web_game.Authentication_Service.Service;

import org.springframework.stereotype.Service;

@Service
public interface SessionService {

    public void saveSession(String username, String token);

    public boolean isTokenValid(String username, String token);

    public void removeSession(String username);
}