package com.movrit.movrit.memberaddress.service;

import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.memberaddress.dto.MemberAddressDtos;
import com.movrit.movrit.memberaddress.entity.MemberAddress;
import com.movrit.movrit.memberaddress.repository.MemberAddressRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAddressService {
  private static final long MAX_ADDRESS_COUNT = 10;

  private final MemberAddressRepository repository;
  private final MemberService memberService;

  public List<MemberAddressDtos.Response> findMine(Long memberId) {
    memberService.getMember(memberId);
    return repository.findAllByMemberIdOrderByDefaultAddressDescIdDesc(memberId).stream()
        .map(MemberAddressDtos.Response::from)
        .toList();
  }

  /** 회원별 등록 한도를 확인하고, 첫 주소 또는 요청한 주소를 기본 주소로 저장합니다. */
  @Transactional
  public MemberAddressDtos.Response create(Long memberId, MemberAddressDtos.SaveRequest request) {
    Member member = memberService.getMemberForUpdate(memberId);
    long count = repository.countByMemberId(memberId);
    if (count >= MAX_ADDRESS_COUNT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "주소는 최대 10개까지 등록할 수 있습니다.");
    }
    boolean makeDefault = request.defaultAddress() || count == 0;
    if (makeDefault) {
      clearDefault(memberId);
      // 부분 유니크 인덱스와 충돌하지 않도록 기존 기본값 해제를 먼저 DB에 반영합니다.
      repository.flush();
    }
    MemberAddress address =
        MemberAddress.builder()
            .member(member)
            .label(request.label().trim())
            .recipientName(request.recipientName().trim())
            .recipientPhone(request.recipientPhone().trim())
            .postalCode(request.postalCode().trim())
            .address1(request.address1().trim())
            .address2(normalize(request.address2()))
            .defaultAddress(makeDefault)
            .build();
    return MemberAddressDtos.Response.from(repository.save(address));
  }

  /** 본인 주소만 수정하며, defaultAddress가 false이면 현재 기본 주소 설정은 유지합니다. */
  @Transactional
  public MemberAddressDtos.Response update(
      Long memberId, Long addressId, MemberAddressDtos.SaveRequest request) {
    memberService.getMemberForUpdate(memberId);
    MemberAddress address = getMine(memberId, addressId);
    address.update(
        request.label().trim(),
        request.recipientName().trim(),
        request.recipientPhone().trim(),
        request.postalCode().trim(),
        request.address1().trim(),
        normalize(request.address2()));
    if (request.defaultAddress() && !address.isDefaultAddress()) {
      clearDefault(memberId);
      repository.flush();
      address.markDefault();
    }
    return MemberAddressDtos.Response.from(address);
  }

  /** 회원 행 잠금 안에서 기본 주소를 하나만 남기도록 변경합니다. */
  @Transactional
  public MemberAddressDtos.Response setDefault(Long memberId, Long addressId) {
    memberService.getMemberForUpdate(memberId);
    MemberAddress address = getMine(memberId, addressId);
    clearDefault(memberId);
    repository.flush();
    address.markDefault();
    return MemberAddressDtos.Response.from(address);
  }

  /** 기본 주소를 삭제하면 남은 주소 중 가장 최근 생성된 주소를 새 기본값으로 지정합니다. */
  @Transactional
  public void delete(Long memberId, Long addressId) {
    memberService.getMemberForUpdate(memberId);
    MemberAddress address = getMine(memberId, addressId);
    boolean wasDefault = address.isDefaultAddress();
    repository.delete(address);
    repository.flush();
    if (wasDefault) {
      repository.findFirstByMemberIdOrderByIdDesc(memberId).ifPresent(MemberAddress::markDefault);
    }
  }

  private MemberAddress getMine(Long memberId, Long addressId) {
    return repository
        .findByIdAndMemberId(addressId, memberId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주소를 찾을 수 없습니다."));
  }

  private void clearDefault(Long memberId) {
    repository
        .findFirstByMemberIdAndDefaultAddressTrue(memberId)
        .ifPresent(MemberAddress::clearDefault);
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
