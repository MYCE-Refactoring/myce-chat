package com.myce.api.service.ai;

import com.myce.api.ai.context.PublicContext;
import com.myce.api.ai.context.UserContext;
import com.myce.api.dto.message.type.SystemMessage;
import com.myce.api.service.AIChatContextService;
import com.myce.api.service.AIChatPromptService;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.domain.repository.ChatMessageRepository;
import com.myce.domain.repository.ChatRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatGenerateService {

    private static final String[] STRONG_KEYWORD = {
            "결제", "환불", "취소", "계좌", "카드", "billing", "payment",
            "오류", "에러", "버그", "작동", "안됨", "문제",
            "불만", "항의", "컴플레인", "complaint",
            "법적", "소송", "변호사", "legal",
            "사람", "상담원", "담당자", "직원", "매니저", "human", "person", "staff", "manager"
            // "어디", "언제", "누가" 등 일반적인 의문사는 제거 - AI가 충분히 답변 가능
    };

    private static final String[] COMPLEX_KEYWORD = {
            "여러", "계속", "몇번", "자꾸"
    };

    private final ChatClient chatClient;
    private final ChatRoomRepository chatRoomRepository;
    private final AIChatPromptService aiChatPromptService;
    private final AIChatContextService aiChatContextService;
    private final ChatMessageRepository chatMessageRepository;

    public String generateAIResponse(String userMessage, String roomCode) {
        // 1. 채팅방 상태 확인
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));
        boolean isWaitingForAdmin = chatRoom.isWaitingForAdmin();

        // 2. 대화 이력 조회
        List<ChatMessage> recentMessages = chatMessageRepository
                .findTop50ByRoomCodeOrderBySentAtDesc(roomCode);

        // 3. 컨텍스트 수집
        UserContext userContext = aiChatContextService.buildUserContext(roomCode);

        // 4. 사람 상담 필요 여부 감지
        boolean shouldSuggestHuman = detectNeedForHumanAssistance(userMessage, recentMessages);

        // 5. AI 프롬프트 구성 (대기 상태 고려)
        String aiResponse = getAiResponse(recentMessages, userContext, isWaitingForAdmin, shouldSuggestHuman, userMessage);

        log.info("Success to create AI response with context. "
                + "roomCode={}, userId={}, isWaitForAdmin={}, shouldSuggestHuman={}",
                roomCode, userContext.userId(), isWaitingForAdmin, shouldSuggestHuman);

        return aiResponse;
    }

    private String getAiResponse(List<ChatMessage> recentMessages, UserContext userContext,
            boolean isWaitingForAdmin, boolean shouldSuggestHuman, String userMessage) {

        PublicContext publicContext = aiChatContextService.buildPublicContext();
        String conversationHistory = chatMessagesConvertToString(recentMessages);

        String systemPrompt = aiChatPromptService.createSystemPromptWithContext(
                userContext, publicContext, isWaitingForAdmin, shouldSuggestHuman);
        String aiPrompt = aiChatPromptService.createAIPromptWithHistoryAndUserMessage(
                systemPrompt, conversationHistory, userMessage);

        return chatClient.prompt(aiPrompt).call().content();
    }

    public String generateConversationSummary(String roomCode) {
        // 전체 대화 이력 조회 (최근 50개)
        List<ChatMessage> messages = chatMessageRepository
                .findTop50ByRoomCodeOrderBySentAtDesc(roomCode);
        if (messages.isEmpty()) return SystemMessage.NOT_EXIST_SUMMARY_MESSAGE;

        UserContext userContext = aiChatContextService.buildUserContext(roomCode);
        String chatHistory = chatMessagesConvertToString(messages);
        // AI 요약 프롬프트 구성 (사용자와 관리자 모두 볼 수 있도록 전문적이고 친화적으로)
        String summaryPrompt = aiChatPromptService
                .createSummaryPromptWithContextAndLog(userContext, chatHistory);
        String summary = chatClient.prompt(summaryPrompt).call().content();

        log.info("Success to create chat summary. roomCode={}, messageCount={}", roomCode, messages.size());
        return summary;
    }

    /**
     * 대화 이력을 문자열로 변환
     */
    private String chatMessagesConvertToString(List<ChatMessage> chatMessages) {
        if (chatMessages.isEmpty()) return SystemMessage.NEW_CHAT;

        StringBuilder history = new StringBuilder();

        // 메시지를 시간순으로 정렬 (오래된 것부터)
        for (ChatMessage message : chatMessages) {
            history.append(String.format("[%s] %s: %s\n",
                            message.getSentAt(),
                            message.getSenderType().getDescription(),
                            message.getContent()))
                    .append("\n");
        }

        return history.toString();
    }

    /**
     * 사람 상담 필요 여부 감지
     */
    private boolean detectNeedForHumanAssistance(String userMessage, List<ChatMessage> recentMessages) {
        String currentMessage = userMessage.toLowerCase();

        // 1. 명시적 키워드 감지 (강한 신호) - 진짜 문제 상황만
        // 2. 복잡성 감지 (긴 메시지 + 복잡한 상황 설명)
        if (isContainsStrongKeyword(currentMessage) ||
                isContainsComplexKeyword(currentMessage)) {
            return true;
        }

        // 2. 반복적 문의 감지 (같은 문제를 3번 이상 물어봄)
        List<String> recentUserMessages = recentMessages.stream()
                .filter(message -> MessageSenderType.USER.equals(message.getSenderType()))
                .limit(6)
                .map(message -> message.getContent().toLowerCase())
                .toList();

        int strongMessageCount = 0;
        for (String content : recentUserMessages) {
            if(isContainsStrongKeyword(content)) strongMessageCount += 1;
        }

        return strongMessageCount >= 3;
    }

    private boolean isContainsStrongKeyword(String message) {
        for (String keyword : STRONG_KEYWORD) {
            if (message.contains(keyword)) return true;
        }

        return false;
    }

    private boolean isContainsComplexKeyword(String message) {
        for (String keyword : COMPLEX_KEYWORD) {
            if (message.contains(keyword)) return true;
        }

        return false;
    }

}
