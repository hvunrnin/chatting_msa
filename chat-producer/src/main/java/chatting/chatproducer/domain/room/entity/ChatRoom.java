package chatting.chatproducer.domain.room.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    private String roomId;

    private String name;

    private String ownerId;

    private LocalDateTime createdAt;

    public static ChatRoom of(String name, String ownerId) {
        return ChatRoom.builder()
                .roomId(java.util.UUID.randomUUID().toString())
                .name(name)
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

