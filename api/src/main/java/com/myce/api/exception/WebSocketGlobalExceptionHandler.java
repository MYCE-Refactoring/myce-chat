package com.myce.api.exception;

import com.myce.api.dto.message.WebSocketErrorMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.WebSocketDestination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class WebSocketGlobalExceptionHandler {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(CustomWebSocketException.class)
    public void handleCustomException(CustomWebSocketException e, SimpMessageHeaderAccessor headerAccessor) {
        CustomWebSocketError error = e.getError();
        String sessionId = error.getSessionId();
        String errorMessage = error.getMessage();

        try {
            WebSocketErrorMessage message = new WebSocketErrorMessage(BroadcastType.ERROR, errorMessage);
            messagingTemplate.convertAndSendToUser(sessionId, WebSocketDestination.ERROR, message);

            log.debug("Success to send error message. sessionId={}", sessionId);
        } catch (MessagingException me) {
            // TODO 에러 처리 확인
            log.debug("Fail to send error message. sessionId={}", sessionId, me);
        }
    }

}
