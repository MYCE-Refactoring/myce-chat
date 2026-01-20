package com.myce.api.service;


import com.myce.api.ai.context.PublicContext;
import com.myce.api.ai.context.UserContext;

public interface AIChatContextService {
    UserContext buildUserContext(String roomCode);
    PublicContext buildPublicContext();
}
