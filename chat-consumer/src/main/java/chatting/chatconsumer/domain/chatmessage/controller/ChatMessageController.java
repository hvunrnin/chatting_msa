//package chatting.chatconsumer.domain.chatmessage.controller;
//
//import chatting.chatconsumer.domain.chatmessage.dto.ChatMessageDTO;
//import chatting.chatconsumer.domain.chatmessage.repository.ChatMessageMongoRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/message")
//@RequiredArgsConstructor
//public class ChatMessageController {
//    private final ChatMessageMongoRepository chatMessageMongoRepository;
//
//    @GetMapping("/history")
//    public List<ChatMessageDTO> getRecentMessages(@RequestParam String roomId) {
//        return chatMessageMongoRepository.findById(roomId)
//                .map(doc -> doc.getMessagesByDate().values().stream()
//                        .flatMap(List::stream)
//                        .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
//                        .map(m -> ChatMessageDTO.builder()
//                                .sender(m.getSender())
//                                .message(m.getMessage())
//                                .timestamp(m.getTimestamp())
//                                .build())
//                        .collect(Collectors.toList()))
//                .orElse(List.of());
//    }
//
//}
//
