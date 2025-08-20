package chatting.chatproducer.domain.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_migration_log",
    uniqueConstraints = @UniqueConstraint(columnNames = {"mergeId", "userId", "sourceRoomId", "targetRoomId"})
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMigrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mergeId;
    private String sourceRoomId;
    private String targetRoomId;
    private String userId;
    private LocalDateTime migratedAt;
    private LocalDateTime rolledBackAt;

    @Enumerated(EnumType.STRING)
    private MigrationStatus status;

    // 복원 판단을 위한 이전 상태 스냅샷
    private boolean wasMemberInFrom;
    private boolean wasMemberInTo;
    private String prevRoleInTo;
    private String prevRoleInFrom;

    public enum MigrationStatus {
        MIGRATED, ROLLED_BACK
    }

    public static UserMigrationLog of(String mergeId, String sourceRoomId, String targetRoomId, String userId,
                                    boolean wasMemberInFrom, boolean wasMemberInTo, 
                                    String prevRoleInFrom, String prevRoleInTo) {
        return UserMigrationLog.builder()
                .mergeId(mergeId)
                .sourceRoomId(sourceRoomId)
                .targetRoomId(targetRoomId)
                .userId(userId)
                .migratedAt(LocalDateTime.now())
                .status(MigrationStatus.MIGRATED)
                .wasMemberInFrom(wasMemberInFrom)
                .wasMemberInTo(wasMemberInTo)
                .prevRoleInFrom(prevRoleInFrom)
                .prevRoleInTo(prevRoleInTo)
                .build();
    }

    /**
     * 롤백 상태로 변경
     */
    public void markAsRolledBack() {
        this.status = MigrationStatus.ROLLED_BACK;
        this.rolledBackAt = LocalDateTime.now();
    }

    /**
     * 마이그레이션된 상태인지 확인
     */
    public boolean isMigrated() {
        return this.status == MigrationStatus.MIGRATED;
    }

    /**
     * 롤백된 상태인지 확인
     */
    public boolean isRolledBack() {
        return this.status == MigrationStatus.ROLLED_BACK;
    }
} 