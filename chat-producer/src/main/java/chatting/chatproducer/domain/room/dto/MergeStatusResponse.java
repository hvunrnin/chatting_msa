package chatting.chatproducer.domain.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeStatusResponse {
    private String mergeId;
    private String targetRoomId;
    private List<String> sourceRoomIds;
    private String currentStep;
    private String status;
    private String failureReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String message;
} 