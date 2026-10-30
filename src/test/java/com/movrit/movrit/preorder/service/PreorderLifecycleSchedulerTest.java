package com.movrit.movrit.preorder.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.movrit.movrit.payment.entity.Payment;
import com.movrit.movrit.payment.entity.PaymentStatus;
import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 프리오더 자동 진행이 한 건의 실패로 멈추지 않는지 확인합니다. */
class PreorderLifecycleSchedulerTest {
  private final PreorderRepository preorders = mock(PreorderRepository.class);
  private final PaymentRepository payments = mock(PaymentRepository.class);
  private final PreorderLifecycleProcessor processor = mock(PreorderLifecycleProcessor.class);
  private final PreorderLifecycleScheduler scheduler =
      new PreorderLifecycleScheduler(preorders, payments, processor);

  @Test
  void oneFailedCloseDoesNotStopOtherPreorders() {
    when(preorders.findAllByStatusAndEndAtLessThanEqual(eq(PreorderStatus.OPEN), any()))
        .thenReturn(List.of(preorder(1L), preorder(2L)));
    doThrow(new IllegalStateException("마감 실패")).when(processor).closeIfDue(eq(1L), any());

    scheduler.advance();

    // 1번이 실패해도 2번 마감은 시도합니다.
    verify(processor).closeIfDue(eq(2L), any(LocalDateTime.class));
  }

  @Test
  void refundFailureIsRetriedButDoesNotStopOtherRefunds() {
    when(payments.findAllByStatusAndOrderPreorderStatus(PaymentStatus.PAID, PreorderStatus.FAILED))
        .thenReturn(List.of(payment(10L), payment(11L)));
    doThrow(new IllegalStateException("환불할 판매 수량이 부족합니다.")).when(processor).refundPayment(10L);

    scheduler.advance();
    scheduler.advance();

    // 실패한 결제는 다음 실행에서도 다시 시도하고, 다른 결제 환불은 계속 진행합니다.
    verify(processor, times(2)).refundPayment(10L);
    verify(processor, times(2)).refundPayment(11L);
  }

  private static Preorder preorder(Long id) {
    return Preorder.builder().id(id).status(PreorderStatus.OPEN).build();
  }

  private static Payment payment(Long id) {
    return Payment.builder().id(id).status(PaymentStatus.PAID).build();
  }
}
