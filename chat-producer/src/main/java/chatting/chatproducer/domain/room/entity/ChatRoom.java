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

    /**
     * 방을 병합 중 상태로 잠금
     */
    public void lockForMerging() {
        this.status = RoomStatus.MERGING;
    }

    /**
     * 방 잠금 해제 (활성 상태로 변경)
     */
    public void unlock() {
        this.status = RoomStatus.ACTIVE;
    }

    /**
     * 방을 아카이브 상태로 변경
     */
    public void archive() {
        this.status = RoomStatus.ARCHIVED;
    }

    /**
     * 방이 병합 중인지 확인
     */
    public boolean isMerging() {
        return this.status == RoomStatus.MERGING;
    }

    /**
     * 방이 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == RoomStatus.ACTIVE;
    }

    public enum RoomStatus {
        ACTIVE,     // 활성 상태
        MERGING,    // 병합 중
        ARCHIVED    // 아카이브됨
    }
}

