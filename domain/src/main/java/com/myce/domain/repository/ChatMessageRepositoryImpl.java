package com.myce.domain.repository;

import com.mongodb.client.result.UpdateResult;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.type.MessageSenderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {

    private final MongoOperations mongoOperations;

    @Override
    public void decreaseUnreadCountBeforeSeq(String roomCode, MessageSenderType senderType, Long lastReadSeq) {
        if (lastReadSeq == null) {
            return;
        }

        Criteria criteria = Criteria.where("roomCode").is(roomCode)
                .and("senderType").ne(senderType)
                .and("unreadCount").gt(0)
                .and("seq").lte(lastReadSeq);

        Query query = new Query(criteria);
        Update update = new Update().inc("unreadCount", -1);
        UpdateResult result = mongoOperations.updateMulti(query, update, ChatMessage.class);
        result.getModifiedCount();
    }

    public void updateUnreadCountEqualSeq(String roomCode, String messageId) {
        Criteria criteria = Criteria.where("roomCode").is(roomCode)
                .and("_id").is(messageId)
                .and("unreadCount").gt(0);

        Query query = new Query(criteria);
        Update update = new Update().inc("unreadCount", -1);
        mongoOperations.updateMulti(query, update, ChatMessage.class);
    }
}
