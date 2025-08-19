package chatting.chatproducer.domain.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeStatus {

    @Id
    private String mergeId;

    private String targetRoomId;

    @ElementCollection
    @CollectionTable(name = "merge_source_rooms", 
        joinColumns = @JoinColumn(name = "merge_id"))
    @Column(name = "source_room_id")
    private List<String> sourceRoomIds;

    @Enumerated(EnumType.STRING)
    private MergeStep currentStep;

    private String status; // IN_PROGRESS, COMPLETED, FAILED

    private String failureReason;
//
//    private LocalDateTime startedAt;
//
//    private LocalDateTime completedAt;

    public enum MergeStep {
        INITIATED,          // 병합 시작
        ROOMS_LOCKED,       // 소스 방 잠금
        MESSAGES_MIGRATED,  // 메시지 마이그레이션
        USERS_MIGRATED,     // 유저 마이그레이션
        COMPLETED           // 완료
    }

    public static MergeStatus of(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        return MergeStatus.builder()
                .mergeId(mergeId)
                .targetRoomId(targetRoomId)
                .sourceRoomIds(sourceRoomIds)
                .currentStep(MergeStep.INITIATED)
                .status("IN_PROGRESS")
                //.startedAt(LocalDateTime.now())
                .build();
    }
} 