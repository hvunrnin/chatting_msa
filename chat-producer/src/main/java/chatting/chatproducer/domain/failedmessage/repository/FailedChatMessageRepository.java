package chatting.chatproducer.domain.failedmessage.repository;

import chatting.chatproducer.domain.failedmessage.document.FailedChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedChatMessageRepository extends MongoRepository<FailedChatMessageDocument, String> {
}
