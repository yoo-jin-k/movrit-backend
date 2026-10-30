package com.movrit.movrit.preorder.service;

import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.payment.service.PaymentService;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프리오더 자동 진행의 단계 하나하나를 각자의 트랜잭션으로 처리합니다.
 *
 * <p>예전에는 판매 시작·마감·환불을 한 트랜잭션에서 처리해, 결제 한 건의 환불이 실패하면 전체가 되돌아가고 같은 오류가 1분마다 반복되었습니다. 이제 프리오더 한 건,
 * 결제 한 건마다 따로 저장하므로 한 건이 실패해도 나머지는 정상 진행됩니다. 호출과 실패 기록은 {@link PreorderLifecycleScheduler}가 맡습니다.
 */
@Service
@RequiredArgsConstructor
public class PreorderLifecycleProcessor {
  private final PreorderRepository preorders;
  private final PaymentRepository payments;
  private final PaymentService paymentService;

  /** 판매 예약 프리오더의 시작 시각이 되었고 아직 종료 전이면 판매를 시작합니다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void openIfDue(Long preorderId, LocalDateTime now) {
    Preorder preorder = preorders.findLockedById(preorderId).orElse(null);
    if (preorder == null || preorder.getStatus() != PreorderStatus.SCHEDULED) return;
    if (now.isBefore(preorder.getEndAt())) preorder.open(now);
  }

  /**
   * 판매 중 프리오더의 종료 시각이 지나면 마감하고, 목표 수량 달성 여부로 성공·실패를 확정합니다.
   *
   * @return 실패로 확정되어 결제 환불이 필요하면 true
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean closeIfDue(Long preorderId, LocalDateTime now) {
    Preorder preorder = preorders.findLockedById(preorderId).orElse(null);
    if (preorder == null
        || preorder.getStatus() != PreorderStatus.OPEN
        || now.isBefore(preorder.getEndAt())) return false;
    preorder.close();
    preorder.finalizeResult();
    return preorder.getStatus() == PreorderStatus.FAILED;
  }

  /** 실패한 프리오더의 결제 한 건을 환불합니다. 실패하면 이 결제만 되돌리고 예외를 그대로 알립니다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void refundPayment(Long paymentId) {
    payments.findById(paymentId).ifPresent(paymentService::refundForFailedPreorder);
  }
}
