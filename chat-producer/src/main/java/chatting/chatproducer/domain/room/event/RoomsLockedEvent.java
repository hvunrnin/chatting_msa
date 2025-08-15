package chatting.chatproducer.domain.room.event;

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
public class RoomsLockedEvent {
    private String mergeId;
    private String targetRoomId;
    private List<String> sourceRoomIds;
    private LocalDateTime timestamp;
    private MergeEventType eventType;
} 