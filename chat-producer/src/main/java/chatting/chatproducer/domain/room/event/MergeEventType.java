package chatting.chatproducer.domain.room.event;

public enum MergeEventType {
    MERGE_INITIATED,
    ROOMS_LOCKED,
    MESSAGES_MIGRATED,
    USERS_MIGRATED,
    MERGE_COMPLETED,
    MERGE_FAILED
} 