package chatting.chatproducer.domain.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    private UserRole role;

    public static RoomUser of(String roomId, String userId) {
        return RoomUser.builder()
                .roomId(roomId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .role(UserRole.MEMBER)
                .build();
    }

    public static RoomUser of(String roomId, String userId, UserRole role) {
        return RoomUser.builder()
                .roomId(roomId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .role(role)
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

    public enum UserRole {
        OWNER(3),      // 방장 (최고 권한)
        ADMIN(2),      // 관리자
        MEMBER(1);     // 일반 멤버

        private final int priority;

        UserRole(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }
}
