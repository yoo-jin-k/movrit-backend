package com.movrit.movrit.notice.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.notice.dto.NoticeDtos;
import com.movrit.movrit.notice.entity.Notice;
import com.movrit.movrit.notice.repository.NoticeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 공지사항 기능입니다.
 *
 * <p>누구나 게시된 공지를 볼 수 있고, 운영자만 공지를 쓰고 고치고 지울 수 있습니다. 게시하지 않은 공지는 사용자에게 없는 공지(404)로 보입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
  private final NoticeRepository notices;
  private final MemberService memberService;

  /** 사용자 공지 목록입니다(게시된 공지만, 고정 공지 먼저). */
  public List<NoticeDtos.Response> published() {
    return notices.findAllByPublishedTrueOrderByPinnedDescPublishedAtDesc().stream()
        .map(NoticeDtos.Response::from)
        .toList();
  }

  /** 사용자 공지 상세입니다. 게시하지 않은 공지는 404입니다. */
  public NoticeDtos.Response publishedOne(Long id) {
    return NoticeDtos.Response.from(
        notices.findByIdAndPublishedTrue(id).orElseThrow(NoticeService::notFound));
  }

  /** 운영자 공지 목록입니다(게시 전 공지 포함). */
  public List<NoticeDtos.Response> all(Long adminId) {
    memberService.getAdmin(adminId);
    return notices.findAllByOrderByIdDesc().stream().map(NoticeDtos.Response::from).toList();
  }

  /** 운영자가 공지를 새로 씁니다. 바로 게시하면 게시 시각을 기록합니다. */
  @Transactional
  public NoticeDtos.Response create(Long adminId, NoticeDtos.SaveRequest request) {
    Member admin = memberService.getAdmin(adminId);
    Notice notice = Notice.builder().author(admin).build();
    notice.edit(
        request.title().trim(),
        request.body().trim(),
        request.category(),
        request.pinned(),
        request.published());
    return NoticeDtos.Response.from(notices.save(notice));
  }

  /** 운영자가 공지를 고칩니다(게시·내리기 포함). */
  @Transactional
  public NoticeDtos.Response update(Long adminId, Long id, NoticeDtos.SaveRequest request) {
    memberService.getAdmin(adminId);
    Notice notice = notices.findById(id).orElseThrow(NoticeService::notFound);
    notice.edit(
        request.title().trim(),
        request.body().trim(),
        request.category(),
        request.pinned(),
        request.published());
    return NoticeDtos.Response.from(notice);
  }

  /** 운영자가 공지를 지웁니다. 없는 공지면 404입니다. */
  @Transactional
  public void delete(Long adminId, Long id) {
    memberService.getAdmin(adminId);
    notices.delete(notices.findById(id).orElseThrow(NoticeService::notFound));
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "공지를 찾을 수 없습니다.");
  }
}
