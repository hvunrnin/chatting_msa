package chatting.chatproducer.domain.room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatRoomViewController {

    @GetMapping("/chat-room")
    public String chatRoomPage() {
        return "chat-room"; // static/chat-room.html
    }
}

