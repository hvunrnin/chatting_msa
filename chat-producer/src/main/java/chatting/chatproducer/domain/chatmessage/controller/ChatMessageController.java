package chatting.chatproducer.domain.chatmessage.controller;

import chatting.chatproducer.domain.chatmessage.dto.ChatMessageDTO;
import chatting.chatproducer.domain.chatmessage.repository.ChatMessageMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageMongoRepository chatMessageMongoRepository;

    @GetMapping("/history")
    public List<ChatMessageDTO> getRecentMessages(@RequestParam String roomId) {
        return chatMessageMongoRepository
                .findByRoomIdOrderByTimestampAsc(roomId)
                .stream()
                .map(doc -> ChatMessageDTO.builder()
                        .sender(doc.getSender())
                        .message(doc.getMessage())
                        .timestamp(doc.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }
}
