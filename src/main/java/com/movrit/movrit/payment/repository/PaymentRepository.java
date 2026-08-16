package com.movrit.movrit.payment.repository;

import com.movrit.movrit.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
