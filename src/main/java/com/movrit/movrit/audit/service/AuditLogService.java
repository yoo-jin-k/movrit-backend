package com.movrit.movrit.audit.service;

import com.movrit.movrit.audit.dto.AuditDtos;
import com.movrit.movrit.audit.entity.AuditLog;
import com.movrit.movrit.audit.repository.AuditLogRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영 감사 기록을 저장하고 운영자에게 최근 기록을 보여줍니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {
  private final AuditLogRepository repository;
  private final MemberService memberService;

  /** 운영 상태를 바꾸는 행위만 기록하며 비밀번호·토큰·주소 같은 민감정보는 저장하지 않습니다. */
  @Transactional
  public void record(
      Long actorMemberId,
      String action,
      String targetType,
      Long targetId,
      String beforeData,
      String afterData,
      String reason) {
    Member actor = actorMemberId == null ? null : memberService.getMember(actorMemberId);
    repository.save(
        AuditLog.builder()
            .actor(actor)
            .action(action)
            .targetType(targetType)
            .targetId(targetId)
            .beforeData(beforeData)
            .afterData(afterData)
            .reason(reason)
            .createdAt(LocalDateTime.now())
            .build());
  }

  /** 최근 감사 기록 200건을 반환합니다. 관리자만 조회할 수 있으며 관리자가 아니면 거절됩니다. */
  public List<AuditDtos.Response> findRecent(Long adminId) {
    memberService.getAdmin(adminId);
    return repository.findTop200ByOrderByIdDesc().stream().map(AuditDtos.Response::from).toList();
  }
}
