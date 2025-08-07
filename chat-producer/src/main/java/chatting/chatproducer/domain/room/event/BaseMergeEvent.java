package chatting.chatproducer.domain.room.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseMergeEvent {
    private String mergeId;
    private String targetRoomId;
    private List<String> sourceRoomIds;
    private LocalDateTime timestamp;
    private MergeEventType eventType;
} 