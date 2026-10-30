package com.movrit.movrit.payment.repository;

import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 주문 결제를 조회합니다. 중복 요청 방지 키와 결제사 승인 키로도 찾을 수 있습니다. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
  /** 주문의 결제를 찾습니다. 주문 한 건에는 결제가 하나만 있습니다. */
  Optional<Payment> findByOrderId(Long orderId);

  /** 같은 결제 요청 키로 이미 만든 결제를 찾습니다. 결제 버튼을 두 번 눌러도 결제가 하나만 생기게 합니다. */
  Optional<Payment> findByIdempotencyKey(String idempotencyKey);

  /** 결제사가 준 승인 키로 결제를 찾습니다. 같은 승인 키를 두 결제에 쓰지 않도록 확인합니다. */
  Optional<Payment> findByPaymentKey(String paymentKey);

  /** 구매자 본인의 결제 내역을 최근 순으로 조회합니다. */
  List<Payment> findAllByOrderMemberIdOrderByIdDesc(Long memberId);

  /** 프리오더의 결제 전체를 조회합니다. 정산 금액 계산과 실패 프리오더 환불에 사용합니다. */
  List<Payment> findAllByOrderPreorderId(Long preorderId);

  /** 지정한 상태의 프리오더에 남아 있는 지정 상태 결제를 찾습니다. 실패한 프리오더의 미환불 결제를 다시 환불할 때 사용합니다. */
  List<Payment> findAllByStatusAndOrderPreorderStatus(
      PaymentStatus status, PreorderStatus preorderStatus);
}
