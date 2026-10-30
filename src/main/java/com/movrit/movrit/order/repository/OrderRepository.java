package com.movrit.movrit.order.repository;

import com.movrit.movrit.order.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 프리오더 주문을 조회합니다. 회원·업체·프리오더 기준 조회와 중복 요청 방지용 요청 키 조회를 제공합니다. */
public interface OrderRepository extends JpaRepository<Order, Long> {
  /** 같은 요청 키로 이미 만들어진 주문을 찾습니다. 주문 버튼을 두 번 눌러도 주문이 하나만 생기게 합니다. */
  Optional<Order> findByRequestKey(String requestKey);

  /** 구매자 본인의 주문을 최근 순으로 조회합니다. */
  List<Order> findAllByMemberIdOrderByIdDesc(Long memberId);

  /** 업체 회원이 소유한 브랜드 상품의 주문을 최근 순으로 조회합니다. */
  List<Order> findAllByPreorderProductBrandSellerMemberIdOrderByIdDesc(Long memberId);

  /** 한 회원이 같은 프리오더에 넣은 주문 중 지정한 상태를 뺀 건수를 셉니다. */
  long countByMemberIdAndPreorderIdAndStatusNot(
      Long memberId, Long preorderId, com.movrit.movrit.order.entity.OrderStatus status);

  /** 크리에이터 추천 링크(협업)로 들어온 주문을 조회합니다. 협업 성과 집계에 사용합니다. */
  List<Order> findAllByAttributionId(Long attributionId);

  /** 프리오더에 들어온 주문 전체를 조회합니다. 프리오더 마감·실패 처리에 사용합니다. */
  List<Order> findAllByPreorderId(Long preorderId);
}
