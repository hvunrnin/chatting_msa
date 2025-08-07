package chatting.chatproducer.domain.room.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MergeCompletedEvent extends BaseMergeEvent {
    private int totalMigratedMessages;
    private int totalMigratedUsers;
} 