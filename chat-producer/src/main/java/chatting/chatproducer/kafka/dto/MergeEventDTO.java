package chatting.chatproducer.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeEventDTO {
    private String mergeId;
    private String targetRoomId;
    private List<String> sourceRoomIds;
    private String eventType;
//    private Instant timestamp;
    
    // 추가 필드들 (이벤트 타입별로 사용)
    private String initiatedBy;
    private int migratedMessageCount;
    private int migratedUserCount;
    private int totalMigratedMessages;
    private int totalMigratedUsers;
    private String failureReason;
    private String failedStep;
} 