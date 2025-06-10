package chatting.chatproducer.domain.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@IdClass(RoomUser.RoomUserId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUser {

    @Id
    private String roomId;

    @Id
    private String userId;

    private LocalDateTime joinedAt;

    public static RoomUser of(String roomId, String userId) {
        return RoomUser.builder()
                .roomId(roomId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    // 복합 키용 클래스
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomUserId implements Serializable {
        @Column(name = "room_id")
        private String roomId;

        @Column(name = "user_id")
        private String userId;
    }
}
