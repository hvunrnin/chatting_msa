package chatting.chatproducer.domain.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "message_migration_log",
    uniqueConstraints = @UniqueConstraint(columnNames = {"mergeId", "messageId", "sourceRoomId", "targetRoomId"})
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageMigrationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mergeId;
    private String sourceRoomId;
    private String targetRoomId;
    private String messageId;  // MongoDB의 _id
    private LocalDateTime migratedAt;
    private LocalDateTime rolledBackAt;

    @Enumerated(EnumType.STRING)
    private MigrationStatus status;

    // 복원 판단을 위한 이전 상태 스냅샷
    private boolean wasInTarget;  // 타겟 방에 원래 있었는지
    private String prevRoomId;    // 원래 있던 방 ID (null이면 새로 추가된 메시지)

    public enum MigrationStatus {
        MIGRATED, ROLLED_BACK
    }

    public static MessageMigrationLog of(String mergeId, String sourceRoomId, String targetRoomId, 
                                       String messageId, boolean wasInTarget, String prevRoomId) {
        return MessageMigrationLog.builder()
                .mergeId(mergeId)
                .sourceRoomId(sourceRoomId)
                .targetRoomId(targetRoomId)
                .messageId(messageId)
                .migratedAt(LocalDateTime.now())
                .status(MigrationStatus.MIGRATED)
                .wasInTarget(wasInTarget)
                .prevRoomId(prevRoomId)
                .build();
    }

    public void markAsRolledBack() {
        this.status = MigrationStatus.ROLLED_BACK;
        this.rolledBackAt = LocalDateTime.now();
    }

    public boolean isMigrated() {
        return this.status == MigrationStatus.MIGRATED;
    }

    public boolean isRolledBack() {
        return this.status == MigrationStatus.ROLLED_BACK;
    }
} 