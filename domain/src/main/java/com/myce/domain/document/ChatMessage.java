package com.myce.domain.document;

import com.myce.domain.document.type.MessageSenderType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@NoArgsConstructor
@Document(collection = "chat_messages")
@CompoundIndexes({
    @CompoundIndex(name = "room_time_idx", def = "{'roomCode': 1, 'sentAt': -1}"),
    @CompoundIndex(name = "sender_time_idx", def = "{'senderId': 1, 'sentAt': -1}")
})
public class ChatMessage {
    @Id
    private String id;

    @Indexed
    private String roomCode;

    private MessageSenderType senderType;

    @Indexed
    private Long senderId;

    private String senderName;

    private String content;

    @CreatedDate
    @Indexed
    private LocalDateTime sentAt;

    private Boolean isSystemMessage;

    /**
     * 메시지 타입 (추가 필드)
     */
    private String messageType;

    /**
     * 메시지 읽음 상태 (JSON 형태)
     */
    private String readStatusJson;

    /**
     * 메시지 편집 여부
     */
    private Boolean isEdited;

    /**
     * 실제 발송자 (관리자용)
     * AdminCode: "CODE123A", Super Admin: "SUPER_ADMIN"
     */
    private String actualSender;

    /**
     * 메시지 삭제 여부
     */
    private Boolean isDeleted;

    /**
     * 메시지 생성 시 기본값 설정
     * MongoDB ObjectId를 미리 생성하여 ID 일관성 보장
     */
    @Builder
    public ChatMessage(String roomCode, MessageSenderType senderType, Long senderId, String senderName,
                      String content, boolean isSystemMessage, String messageType) {
        this.id = new ObjectId().toString();
        this.roomCode = roomCode;
        this.senderType = senderType;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.isSystemMessage = isSystemMessage;
        this.messageType = messageType;
        this.isEdited = false;
        this.isDeleted = false;
        this.readStatusJson = "{}";
        this.sentAt = LocalDateTime.now();
    }

}
