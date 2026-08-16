package com.movrit.movrit.shipping.service;

import com.movrit.movrit.shipping.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingService {
    private final ShippingRepository shippingRepository;
}
