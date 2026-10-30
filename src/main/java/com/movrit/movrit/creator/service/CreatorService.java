package com.movrit.movrit.creator.service;

import com.movrit.movrit.creator.dto.CreatorDtos;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.repository.CreatorRepository;
import com.movrit.movrit.member.service.MemberService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 캠페인 참여자 활동 프로필(Creator)을 만들고 조회·수정합니다. 일반 회원이 처음 캠페인에 신청할 때 내부 프로필을 자동으로 만들어 줍니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorService {
  private final CreatorRepository repository;
  private final MemberService memberService;

  /** 로그인한 회원의 활동 프로필을 직접 만듭니다. 이미 프로필이 있으면 409로 거절합니다. */
  @Transactional
  public CreatorDtos.Response create(Long memberId, CreatorDtos.SaveRequest r) {
    if (repository.existsByMemberId(memberId))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 인플루언서 프로필이 있습니다.");
    Creator c =
        Creator.builder()
            .member(memberService.getMember(memberId))
            .nickname(r.nickname().trim())
            .bio(r.bio())
            .primaryCategory(r.primaryCategory())
            .snsUrl(r.snsUrl())
            .followerCount(r.followerCount())
            .status("ACTIVE")
            .build();
    return CreatorDtos.Response.from(repository.save(c));
  }

  /** 로그인한 회원 본인의 활동 프로필을 반환합니다. 없으면 404입니다. */
  public CreatorDtos.Response mine(Long memberId) {
    return CreatorDtos.Response.from(getByMember(memberId));
  }

  /** 활성(ACTIVE) 상태인 활동 프로필 전체를 반환합니다. */
  public List<CreatorDtos.Response> findAll() {
    return repository.findAll().stream()
        .filter(c -> "ACTIVE".equals(c.getStatus()))
        .map(CreatorDtos.Response::from)
        .toList();
  }

  /** 활동 프로필 하나를 ID로 조회합니다. 없으면 404입니다. */
  public CreatorDtos.Response findById(Long id) {
    return CreatorDtos.Response.from(get(id));
  }

  /** 로그인한 회원 본인의 활동 프로필을 수정합니다. 프로필이 없으면 404입니다. */
  @Transactional
  public CreatorDtos.Response update(Long memberId, CreatorDtos.SaveRequest r) {
    Creator c = getByMember(memberId);
    c.update(r.nickname().trim(), r.bio(), r.primaryCategory(), r.snsUrl(), r.followerCount());
    return CreatorDtos.Response.from(c);
  }

  /** 다른 서비스에서 쓰는 내부 조회입니다. 활동 프로필을 ID로 찾고 없으면 404를 냅니다. */
  public Creator get(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인플루언서를 찾을 수 없습니다."));
  }

  /** 다른 서비스에서 쓰는 내부 조회입니다. 회원 ID로 활동 프로필을 찾고 없으면 404를 냅니다. */
  public Creator getByMember(Long id) {
    return repository
        .findByMemberId(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인플루언서 프로필이 없습니다."));
  }

  /** 회원의 활동 프로필이 있으면 반환하고, 없어도 오류 없이 빈 값을 돌려줍니다. */
  public Optional<Creator> findOptionalByMember(Long memberId) {
    return repository.findByMemberId(memberId);
  }

  /**
   * 캠페인 신청 시 사용할 활동 프로필을 찾고, 없으면 회원 이름으로 새로 만듭니다. 별도 인플루언서 가입 없이도 일반 회원이 신청할 수 있게 하기 위함입니다(기존 신청
   * 테이블 연결 유지).
   */
  @Transactional
  public Creator getOrCreateCandidate(Long memberId) {
    return repository
        .findByMemberId(memberId)
        .orElseGet(
            () -> {
              var member = memberService.getMember(memberId);
              Creator candidate =
                  Creator.builder()
                      .member(member)
                      .nickname(member.getName())
                      .followerCount(0L)
                      .status("ACTIVE")
                      .build();
              return repository.save(candidate);
            });
  }
}
