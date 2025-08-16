package chatting.chatproducer.domain.room.controller;

import chatting.chatproducer.domain.room.dto.MergeRequest;
import chatting.chatproducer.domain.room.dto.MergeResponse;
import chatting.chatproducer.domain.room.dto.MergeStatusResponse;
import chatting.chatproducer.domain.room.dto.ValidationResponse;
import chatting.chatproducer.domain.room.entity.MergeStatus;
import chatting.chatproducer.domain.room.service.ChatRoomMergeService;
import chatting.chatproducer.domain.room.service.MergeValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/room/merge")
@RequiredArgsConstructor
public class RoomMergeController {

    private final ChatRoomMergeService chatRoomMergeService;
    private final MergeValidationService mergeValidationService;

    /**
     * 채팅방 병합 요청
     */
    @PostMapping("/request")
    public ResponseEntity<MergeResponse> requestMerge(@RequestBody MergeRequest request) {
        log.info("병합 요청 수신: targetRoomId={}, sourceRoomIds={}, initiatedBy={}", 
                request.getTargetRoomId(), request.getSourceRoomIds(), request.getInitiatedBy());

        try {
            // 1. 입력 검증
            validateMergeRequest(request);

            // 2. 병합 시작
            String mergeId = chatRoomMergeService.initiateMerge(
                    request.getTargetRoomId(),
                    request.getSourceRoomIds(),
                    request.getInitiatedBy()
            );

            log.info("병합 요청 처리 완료: mergeId={}", mergeId);

            return ResponseEntity.ok(MergeResponse.builder()
                    .mergeId(mergeId)
                    .status("INITIATED")
                    .message("병합이 시작되었습니다.")
                    .build());

        } catch (Exception e) {
            log.error("병합 요청 처리 실패: targetRoomId={}", request.getTargetRoomId(), e);
            
            return ResponseEntity.badRequest().body(MergeResponse.builder()
                    .status("FAILED")
                    .message("병합 요청 처리 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 병합 상태 조회
     */
    @GetMapping("/status/{mergeId}")
    public ResponseEntity<MergeStatusResponse> getMergeStatus(@PathVariable String mergeId) {
        log.info("병합 상태 조회: mergeId={}", mergeId);

        try {
            MergeStatus mergeStatus = chatRoomMergeService.getMergeStatus(mergeId);
            
            return ResponseEntity.ok(MergeStatusResponse.builder()
                    .mergeId(mergeStatus.getMergeId())
                    .targetRoomId(mergeStatus.getTargetRoomId())
                    .sourceRoomIds(mergeStatus.getSourceRoomIds())
                    .currentStep(mergeStatus.getCurrentStep().name())
                    .status(mergeStatus.getStatus())
                    .failureReason(mergeStatus.getFailureReason())
                    .startedAt(mergeStatus.getStartedAt())
                    .completedAt(mergeStatus.getCompletedAt())
                    .build());

        } catch (Exception e) {
            log.error("병합 상태 조회 실패: mergeId={}", mergeId, e);
            
            return ResponseEntity.badRequest().body(MergeStatusResponse.builder()
                    .mergeId(mergeId)
                    .status("ERROR") ㅇ
                    .message("병합 상태 조회 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 병합 검증 (수동)
     */
    @PostMapping("/validate/{mergeId}")
    public ResponseEntity<ValidationResponse> validateMerge(@PathVariable String mergeId) {
        log.info("병합 검증 요청: mergeId={}", mergeId);

        try {
            MergeStatus mergeStatus = chatRoomMergeService.getMergeStatus(mergeId);
            
            mergeValidationService.validateMerge(
                    mergeId,
                    mergeStatus.getTargetRoomId(),
                    mergeStatus.getSourceRoomIds()
            );

            return ResponseEntity.ok(ValidationResponse.builder()
                    .mergeId(mergeId)
                    .status("VALID")
                    .message("병합 검증이 성공했습니다.")
                    .build());

        } catch (Exception e) {
            log.error("병합 검증 실패: mergeId={}", mergeId, e);
            
            return ResponseEntity.badRequest().body(ValidationResponse.builder()
                    .mergeId(mergeId)
                    .status("INVALID")
                    .message("병합 검증 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 병합 요청 입력 검증
     */
    private void validateMergeRequest(MergeRequest request) {
        if (request.getTargetRoomId() == null || request.getTargetRoomId().trim().isEmpty()) {
            throw new IllegalArgumentException("타겟 방 ID는 필수입니다.");
        }

        if (request.getSourceRoomIds() == null || request.getSourceRoomIds().isEmpty()) {
            throw new IllegalArgumentException("소스 방 ID 목록은 필수입니다.");
        }

        if (request.getSourceRoomIds().size() < 1) {
            throw new IllegalArgumentException("최소 1개 이상의 소스 방이 필요합니다.");
        }

        if (request.getSourceRoomIds().contains(request.getTargetRoomId())) {
            throw new IllegalArgumentException("타겟 방이 소스 방 목록에 포함될 수 없습니다.");
        }

        if (request.getInitiatedBy() == null || request.getInitiatedBy().trim().isEmpty()) {
            throw new IllegalArgumentException("요청자는 필수입니다.");
        }

        // 중복 소스 방 확인
        long uniqueSourceCount = request.getSourceRoomIds().stream().distinct().count();
        if (uniqueSourceCount != request.getSourceRoomIds().size()) {
            throw new IllegalArgumentException("소스 방 목록에 중복이 있습니다.");
        }
    }

} 