package chatting.chatproducer.domain.room.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseMergeEvent extends ApplicationEvent {
    private String mergeId;
    private String targetRoomId;
    private List<String> sourceRoomIds;
    private LocalDateTime timestamp;
    private MergeEventType eventType;
    
    public BaseMergeEvent(Object source, String mergeId, String targetRoomId, List<String> sourceRoomIds, LocalDateTime timestamp, MergeEventType eventType) {
        super(source);
        this.mergeId = mergeId;
        this.targetRoomId = targetRoomId;
        this.sourceRoomIds = sourceRoomIds;
        this.timestamp = timestamp;
        this.eventType = eventType;
    }
} 