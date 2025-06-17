package chatting.chatproducer.domain.failedmessage.service;

import chatting.chatproducer.domain.failedmessage.document.FailedChatMessageDocument;
import chatting.chatproducer.domain.failedmessage.repository.FailedChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FailedChatMessageService {

    private final FailedChatMessageRepository repository;

    public void save(FailedChatMessageDocument document) {
        repository.save(document);
    }
}
