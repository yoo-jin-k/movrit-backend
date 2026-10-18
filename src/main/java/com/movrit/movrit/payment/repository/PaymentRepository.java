package com.movrit.movrit.payment.repository;

import com.movrit.movrit.payment.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByOrderId(Long orderId);

  Optional<Payment> findByIdempotencyKey(String idempotencyKey);

  Optional<Payment> findByPaymentKey(String paymentKey);

  List<Payment> findAllByOrderMemberIdOrderByIdDesc(Long memberId);

  List<Payment> findAllByOrderPreorderId(Long preorderId);
}
