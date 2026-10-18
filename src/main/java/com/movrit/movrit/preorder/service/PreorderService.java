package com.movrit.movrit.preorder.service;

import com.movrit.movrit.preorder.dto.PreorderDtos;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.entity.PreorderStatus;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import com.movrit.movrit.product.entity.Product;
import com.movrit.movrit.product.entity.ProductVariant;
import com.movrit.movrit.product.service.ProductService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreorderService {
  private final PreorderRepository preorderRepository;
  private final ProductService productService;

  @Transactional
  public PreorderDtos.Response create(Long memberId, PreorderDtos.CreateRequest request) {
    Product product = productService.getOwnedProduct(memberId, request.productId());
    Set<ProductVariant> saleVariants =
        productService.getOwnedVariants(memberId, product.getId(), request.variantIds());
    validatePeriod(request.startAt(), request.endAt());
    validateQuantityRules(
        request.targetQuantity(),
        request.maxQuantity(),
        request.minOrderQuantity(),
        request.maxOrderQuantityPerMember());
    validateStockPolicy(saleVariants, request.targetQuantity(), request.maxQuantity());

    Preorder preorder =
        Preorder.builder()
            .product(product)
            .title(request.title().trim())
            .description(request.description())
            .preorderPrice(request.preorderPrice())
            .currency(request.currency().trim().toUpperCase())
            .targetQuantity(request.targetQuantity())
            .maxQuantity(request.maxQuantity())
            .orderedQuantity(0L)
            .minOrderQuantity(request.minOrderQuantity())
            .maxOrderQuantityPerMember(request.maxOrderQuantityPerMember())
            .startAt(request.startAt())
            .endAt(request.endAt())
            .productionExpectedAt(request.productionExpectedAt())
            .shippingExpectedAt(request.shippingExpectedAt())
            .status(PreorderStatus.DRAFT)
            .build();
    preorder.replaceSaleVariants(saleVariants);

    return PreorderDtos.Response.from(preorderRepository.save(preorder));
  }

  public List<PreorderDtos.Response> findMine(Long memberId) {
    return preorderRepository.findAll().stream()
        .filter(preorder -> preorder.getDeletedAt() == null)
        .filter(
            preorder ->
                preorder.getProduct().getBrand().getSeller().getMember().getId().equals(memberId))
        .map(PreorderDtos.Response::from)
        .toList();
  }

  public List<PreorderDtos.Response> findOpen() {
    return preorderRepository.findAll().stream()
        .filter(preorder -> preorder.getDeletedAt() == null)
        .filter(preorder -> preorder.getStatus() == PreorderStatus.OPEN)
        .map(PreorderDtos.Response::from)
        .toList();
  }

  public List<PreorderDtos.Response> browse(
      String keyword, String category, PreorderStatus status) {
    Set<PreorderStatus> publicStatuses =
        Set.of(
            PreorderStatus.SCHEDULED,
            PreorderStatus.OPEN,
            PreorderStatus.SUCCESS,
            PreorderStatus.FAILED,
            PreorderStatus.PRODUCTION,
            PreorderStatus.SHIPPING,
            PreorderStatus.COMPLETED);
    if (status != null && !publicStatuses.contains(status))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공개 조회할 수 없는 상태야.");
    String q = keyword == null ? "" : keyword.trim().toLowerCase();
    return preorderRepository.findAll().stream()
        .filter(p -> p.getDeletedAt() == null && publicStatuses.contains(p.getStatus()))
        .filter(p -> status == null || p.getStatus() == status)
        .filter(
            p ->
                category == null
                    || category.isBlank()
                    || category.equalsIgnoreCase(p.getProduct().getCategory()))
        .filter(
            p ->
                q.isBlank()
                    || p.getTitle().toLowerCase().contains(q)
                    || p.getProduct().getName().toLowerCase().contains(q)
                    || p.getProduct().getBrand().getName().toLowerCase().contains(q))
        .map(PreorderDtos.Response::from)
        .toList();
  }

  public PreorderDtos.Response findById(Long id) {
    return PreorderDtos.Response.from(getPreorder(id));
  }

  @Transactional
  public PreorderDtos.Response update(Long memberId, Long id, PreorderDtos.UpdateRequest request) {
    Preorder preorder = getOwnedPreorder(memberId, id);
    Set<ProductVariant> saleVariants =
        productService.getOwnedVariants(
            memberId, preorder.getProduct().getId(), request.variantIds());
    validatePeriod(request.startAt(), request.endAt());
    validateQuantityRules(
        request.targetQuantity(),
        request.maxQuantity(),
        request.minOrderQuantity(),
        request.maxOrderQuantityPerMember());
    validateStockPolicy(saleVariants, request.targetQuantity(), request.maxQuantity());
    try {
      preorder.update(
          request.title().trim(),
          request.description(),
          request.preorderPrice(),
          request.currency().trim().toUpperCase(),
          request.targetQuantity(),
          request.maxQuantity(),
          request.minOrderQuantity(),
          request.maxOrderQuantityPerMember(),
          request.startAt(),
          request.endAt(),
          request.productionExpectedAt(),
          request.shippingExpectedAt());
      preorder.replaceSaleVariants(saleVariants);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
    return PreorderDtos.Response.from(preorder);
  }

  @Transactional
  public PreorderDtos.Response schedule(Long memberId, Long id) {
    return applyTransition(getOwnedPreorder(memberId, id), Preorder::schedule);
  }

  @Transactional
  public PreorderDtos.Response open(Long memberId, Long id) {
    Preorder preorder = getOwnedPreorder(memberId, id);
    return applyTransition(preorder, value -> value.open(LocalDateTime.now()));
  }

  @Transactional
  public PreorderDtos.Response close(Long memberId, Long id) {
    return applyTransition(getOwnedPreorder(memberId, id), Preorder::close);
  }

  @Transactional
  public PreorderDtos.Response finalizeResult(Long memberId, Long id) {
    return applyTransition(getOwnedPreorder(memberId, id), Preorder::finalizeResult);
  }

  @Transactional
  public PreorderDtos.Response cancel(Long memberId, Long id, String reason) {
    Preorder preorder = getOwnedPreorder(memberId, id);
    return applyTransition(preorder, value -> value.cancel(reason.trim()));
  }

  @Transactional
  public PreorderDtos.Response startProduction(Long memberId, Long id) {
    return applyTransition(getOwnedPreorder(memberId, id), Preorder::startProduction);
  }

  @Transactional
  public PreorderDtos.Response startShipping(Long memberId, Long id) {
    return applyTransition(getOwnedPreorder(memberId, id), Preorder::startShipping);
  }

  @Transactional
  public PreorderDtos.Response complete(Long memberId, Long id) {
    return applyTransition(getOwnedPreorder(memberId, id), Preorder::complete);
  }

  @Transactional
  public void delete(Long memberId, Long id) {
    Preorder preorder = getOwnedPreorder(memberId, id);
    try {
      preorder.softDelete();
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  public Preorder getPreorder(Long id) {
    Preorder preorder =
        preorderRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없습니다."));
    if (preorder.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없습니다.");
    }
    return preorder;
  }

  /** 주문 수량을 검사하고 늘리는 동안 다른 주문이 같은 프리오더 수량을 바꾸지 못하게 한다. */
  public Preorder getPreorderForOrder(Long id) {
    Preorder preorder =
        preorderRepository
            .findLockedById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없습니다."));
    if (preorder.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없습니다.");
    }
    return preorder;
  }

  @Transactional
  public void addOrderQuantity(Preorder preorder, long quantity) {
    try {
      preorder.increaseOrderedQuantity(quantity);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  @Transactional
  public void removeOrderQuantity(Preorder preorder, long quantity) {
    preorder.decreaseOrderedQuantity(quantity);
  }

  private Preorder getOwnedPreorder(Long memberId, Long id) {
    Preorder preorder = getPreorder(id);
    productService.getOwnedProduct(memberId, preorder.getProduct().getId());
    return preorder;
  }

  private PreorderDtos.Response applyTransition(
      Preorder preorder, java.util.function.Consumer<Preorder> transition) {
    try {
      transition.accept(preorder);
      return PreorderDtos.Response.from(preorder);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
    }
  }

  private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
    if (!startAt.isBefore(endAt)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 뒤여야 해.");
    }
  }

  private void validateQuantityRules(Long target, Long max, Long minOrder, Long maxPerMember) {
    if (max != null && max < target) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최대 판매 수량은 목표 수량보다 작을 수 없습니다.");
    }
    if (maxPerMember != null && maxPerMember < minOrder) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "회원별 최대 주문 수량은 최소 주문 수량보다 작을 수 없습니다.");
    }
  }

  private void validateStockPolicy(Set<ProductVariant> variants, Long target, Long max) {
    long available = variants.stream().mapToLong(ProductVariant::getAvailableQuantity).sum();
    if (available < target) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 SKU의 판매 가능 재고가 목표 수량보다 적습니다.");
    }
    if (max != null && max > available) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "최대 판매 수량이 선택한 SKU의 판매 가능 재고보다 많아.");
    }
  }
}
