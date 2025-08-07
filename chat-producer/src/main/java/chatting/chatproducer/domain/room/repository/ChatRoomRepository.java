package chatting.chatproducer.domain.room.repository;

import chatting.chatproducer.domain.room.entity.ChatRoom;
import chatting.chatproducer.domain.room.entity.ChatRoom.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    
    // 활성 상태인 방들 조회
    List<ChatRoom> findByStatus(RoomStatus status);
    
    // 특정 상태의 방들 조회
    List<ChatRoom> findByStatusIn(List<RoomStatus> statuses);
    
    // 방장이 소유한 방들 조회
    List<ChatRoom> findByOwnerId(String ownerId);
    
    // 방 상태 업데이트
    @Modifying
    @Query("UPDATE ChatRoom c SET c.status = :status WHERE c.roomId IN :roomIds")
    void updateStatusByRoomIds(@Param("status") RoomStatus status, @Param("roomIds") List<String> roomIds);
}
