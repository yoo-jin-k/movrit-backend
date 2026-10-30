package com.movrit.movrit.preorder.service;

import com.movrit.movrit.payment.repository.PaymentRepository;
import com.movrit.movrit.payment.service.PaymentService;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PreorderLifecycleScheduler {
  private final PreorderRepository preorders;
  private final PaymentRepository payments;
  private final PaymentService paymentService;

  @Scheduled(fixedDelayString = "${app.preorder-lifecycle-delay-ms:60000}")
  @Transactional
  public void advance() {
    LocalDateTime now = LocalDateTime.now();
    for (Preorder p :
        preorders.findAllByStatusAndStartAtLessThanEqual(PreorderStatus.SCHEDULED, now)) {
      if (now.isBefore(p.getEndAt())) p.open(now);
    }
    for (Preorder p : preorders.findAllByStatusAndEndAtLessThanEqual(PreorderStatus.OPEN, now)) {
      p.close();
      p.finalizeResult();
      if (p.getStatus() == PreorderStatus.FAILED)
        payments
            .findAllByOrderPreorderId(p.getId())
            .forEach(paymentService::refundForFailedPreorder);
    }
  }
}
