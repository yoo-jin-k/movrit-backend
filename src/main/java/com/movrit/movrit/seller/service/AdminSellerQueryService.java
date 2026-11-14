package com.movrit.movrit.seller.service;

import com.movrit.movrit.member.service.MemberService;
import com.movrit.movrit.seller.dto.AdminSellerDtos;
import com.movrit.movrit.seller.entity.SellerStatus;
import com.movrit.movrit.seller.repository.SellerProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 운영자 업체 목록을 심사 상태로 거르고 신청일·업체명 순으로 정렬해 조회합니다.
 *
 * <p>권한: 운영자만(DB의 회원 권한으로 다시 확인, 아니면 403). 정렬 값이 정해진 목록에 없으면 400입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSellerQueryService {

  /** 정렬 값: 신청일 최신순(기본)·신청일 오래된순·업체명순입니다. */
  public static final List<String> SORTS = List.of("newest", "oldest", "name");

  private final SellerProfileRepository sellerProfileRepository;
  private final MemberService memberService;

  /**
   * 운영자 업체 목록을 조회합니다.
   *
   * @param status 심사 상태(비우면 전체)
   * @param sort newest·oldest·name 중 하나(비우면 newest)
   * @throws ResponseStatusException 운영자가 아니면 403, 정렬 값이 잘못되면 400
   */
  public List<AdminSellerDtos.ListItem> findForAdmin(
      Long adminMemberId, SellerStatus status, String sort) {
    memberService.getAdmin(adminMemberId);
    return sellerProfileRepository.findAll(sortOf(sort)).stream()
        .filter(seller -> status == null || seller.getStatus() == status)
        .map(AdminSellerDtos.ListItem::from)
        .toList();
  }

  /** 정렬 값을 DB 정렬로 바꿉니다. 같은 시각·같은 이름이면 번호로 순서를 고정합니다. */
  static Sort sortOf(String sort) {
    String value = sort == null || sort.isBlank() ? "newest" : sort.trim();
    return switch (value) {
      case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
      case "oldest" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
      case "name" -> Sort.by(Sort.Order.asc("businessName"), Sort.Order.asc("id"));
      default ->
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "정렬은 newest·oldest·name 중 하나로 지정해 주세요.");
    };
  }
}
