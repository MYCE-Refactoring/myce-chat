package com.myce.domain.repository;

import com.myce.domain.document.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * ChatMessage MongoDB Repository
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String>, ChatMessageRepositoryCustom {
    
    /**
     * 채팅방별 메시지 조회 (페이징)
     */
    Page<ChatMessage> findByRoomCodeOrderBySentAtDesc(String roomCode, Pageable pageable);
    
    /**
     * 채팅방별 최근 메시지 조회
     */
    List<ChatMessage> findTop50ByRoomCodeOrderBySentAtDesc(String roomCode);

    Optional<ChatMessage> findTop1ByRoomCodeOrderBySentAtDesc(String roomCode);

    Optional<ChatMessage> findTop1ByRoomCodeOrderBySeqDesc(String roomCode);
    
    /**
     * 채팅방에서 특정 발송자 타입의 메시지 개수 (안읽은 메시지 계산용)
     */
    long countByRoomCodeAndSenderType(String roomCode, String senderType);
    
    /**
     * 채팅방에서 특정 seq 이후의 특정 발송자 타입 메시지 개수
     */
    long countByRoomCodeAndSenderTypeAndSeqGreaterThan(String roomCode, String senderType, Long messageSeq);


}
