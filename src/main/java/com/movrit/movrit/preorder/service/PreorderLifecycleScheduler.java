package com.movrit.movrit.preorder.service;

import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 1분마다 프리오더 상태를 시간에 맞춰 진행합니다.
 *
 * <ol>
 *   <li>판매 예약(SCHEDULED) 프리오더의 시작 시각이 되면 판매를 시작합니다.
 *   <li>판매 중(OPEN) 프리오더의 종료 시각이 지나면 마감하고 성공·실패를 확정합니다.
 *   <li>실패(FAILED)한 프리오더에 남아 있는 승인 결제를 환불합니다. 지난 실행에서 환불하지 못한 결제도 다시 시도합니다.
 * </ol>
 *
 * <p>각 단계는 {@link PreorderLifecycleProcessor}가 건별 트랜잭션으로 처리합니다. 한 건이 실패하면 경고 로그를 남기고 다음 건을 계속
 * 처리합니다. 같은 결제의 환불 실패는 서버 실행 중 한 번만 경고합니다(데이터 확인이 필요한 경우 로그가 1분마다 쌓이지 않도록).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreorderLifecycleScheduler {
  private final PreorderRepository preorders;
  private final PaymentRepository payments;
  private final PreorderLifecycleProcessor processor;

  /** 이미 경고한 환불 실패 결제 ID입니다. 서버를 다시 시작하면 비워집니다. */
  private final Set<Long> warnedRefundFailures = ConcurrentHashMap.newKeySet();

  /** 예약 실행 진입점입니다. 실행 간격은 app.preorder-lifecycle-delay-ms(기본 60초)로 바꿀 수 있습니다. */
  @Scheduled(fixedDelayString = "${app.preorder-lifecycle-delay-ms:60000}")
  public void advance() {
    LocalDateTime now = LocalDateTime.now();
    for (Preorder p :
        preorders.findAllByStatusAndStartAtLessThanEqual(PreorderStatus.SCHEDULED, now)) {
      run("판매 시작", p.getId(), () -> processor.openIfDue(p.getId(), now));
    }
    for (Preorder p : preorders.findAllByStatusAndEndAtLessThanEqual(PreorderStatus.OPEN, now)) {
      run("마감", p.getId(), () -> processor.closeIfDue(p.getId(), now));
    }
    for (Payment payment :
        payments.findAllByStatusAndOrderPreorderStatus(PaymentStatus.PAID, PreorderStatus.FAILED)) {
      refund(payment.getId());
    }
  }

  /** 프리오더 한 건의 단계를 실행하고, 실패하면 경고만 남기고 다음 건으로 넘어갑니다. */
  private void run(String step, Long preorderId, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      log.warn("프리오더 자동 {} 실패: preorderId={}, 사유={}", step, preorderId, e.getMessage());
    }
  }

  /** 결제 한 건을 환불합니다. 성공하면 경고 기록을 지우고, 실패하면 결제마다 한 번만 경고합니다. */
  private void refund(Long paymentId) {
    try {
      processor.refundPayment(paymentId);
      warnedRefundFailures.remove(paymentId);
    } catch (RuntimeException e) {
      if (warnedRefundFailures.add(paymentId)) {
        log.warn("실패 프리오더 자동 환불 실패(운영자 확인 필요): paymentId={}, 사유={}", paymentId, e.getMessage());
      }
    }
  }
}
