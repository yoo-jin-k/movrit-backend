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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorService {
  private final CreatorRepository repository;
  private final MemberService memberService;

  @Transactional
  public CreatorDtos.Response create(Long memberId, CreatorDtos.SaveRequest r) {
    if (repository.existsByMemberId(memberId))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 인플루언서 프로필이 있어.");
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

  public CreatorDtos.Response mine(Long memberId) {
    return CreatorDtos.Response.from(getByMember(memberId));
  }

  public List<CreatorDtos.Response> findAll() {
    return repository.findAll().stream()
        .filter(c -> "ACTIVE".equals(c.getStatus()))
        .map(CreatorDtos.Response::from)
        .toList();
  }

  public CreatorDtos.Response findById(Long id) {
    return CreatorDtos.Response.from(get(id));
  }

  @Transactional
  public CreatorDtos.Response update(Long memberId, CreatorDtos.SaveRequest r) {
    Creator c = getByMember(memberId);
    c.update(r.nickname().trim(), r.bio(), r.primaryCategory(), r.snsUrl(), r.followerCount());
    return CreatorDtos.Response.from(c);
  }

  public Creator get(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인플루언서를 찾을 수 없습니다."));
  }

  public Creator getByMember(Long id) {
    return repository
        .findByMemberId(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인플루언서 프로필이 없습니다."));
  }

  public Optional<Creator> findOptionalByMember(Long memberId) {
    return repository.findByMemberId(memberId);
  }

  /** 기존 신청 테이블의 외래 키를 유지하면서 일반 사용자도 캠페인에 신청할 수 있게 내부 프로필을 만듭니다. */
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
