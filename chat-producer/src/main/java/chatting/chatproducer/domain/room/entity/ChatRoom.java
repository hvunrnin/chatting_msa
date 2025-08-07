package chatting.chatproducer.domain.room.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    private String createdBy;

    public static ChatRoom of(String name, String ownerId) {
        return ChatRoom.builder()
                .roomId(java.util.UUID.randomUUID().toString())
                .name(name)
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .status(RoomStatus.ACTIVE)
                .createdBy(ownerId)
                .build();
    }

    public enum RoomStatus {
        ACTIVE,     // 활성 상태
        MERGING,    // 병합 중
        ARCHIVED    // 아카이브됨
    }
}

