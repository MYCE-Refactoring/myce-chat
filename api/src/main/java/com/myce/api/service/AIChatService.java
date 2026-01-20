package com.myce.api.service;


import com.myce.api.dto.ChatStatusResponse;
import com.myce.api.dto.ConversationSummaryResponse;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.domain.document.ChatRoom;

/**
 * AI 채팅 서비스
 * AWS Bedrock Nova Lite를 이용한 플랫폼 AI 상담 서비스
 */
public interface AIChatService {

    ChatMessageResponse sendAIMessage(ChatRoom chatRoom, String userMessage);

    ChatStatusResponse getAiChatStatus(String roomCode);

    void handoffToAdmin(ChatRoom chatRoom, String adminCode);

    ConversationSummaryResponse getConversationSummaryForAdmin(String roomCode, String userRole);

    ChatMessageResponse requestAdminHandoff(ChatRoom chatRoom);
    
    ChatMessageResponse cancelAdminHandoff(ChatRoom chatRoom);
    
    ChatMessageResponse manageAIHandoff(ChatRoom chatRoom);
}