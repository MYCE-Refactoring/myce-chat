package com.myce.api.service;


import com.myce.api.ai.context.PublicContext;
import com.myce.api.ai.context.UserContext;

public interface AIChatPromptService {
    String createSystemPromptWithContext(UserContext userContext, PublicContext publicContext, boolean isWaitingForAdmin, boolean shouldSuggestHuman);
    String createAIPromptWithHistoryAndUserMessage(String systemPrompt, String conversationHistory, String userMessage);
    String createSummaryPromptWithContextAndLog(UserContext userContext, String conversationLog);
}
