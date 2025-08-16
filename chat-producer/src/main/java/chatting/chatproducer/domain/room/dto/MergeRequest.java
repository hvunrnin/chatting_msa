package chatting.chatproducer.domain.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeRequest {
    private String targetRoomId;
    private List<String> sourceRoomIds;
    private String initiatedBy;
} 