package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 업체가 여러 신청을 한 번에 선정·미선정합니다.
 *
 * <p>신청마다 {@link CampaignApplicationService#select}·{@link CampaignApplicationService#reject}를 따로
 * 불러 각각 저장합니다. 한 건이 실패해도(모집 인원 초과, 이미 처리됨 등) 나머지는 계속 처리하고, 건별 결과와 사유를 돌려줍니다. 모집 인원 확인·알림·감사 기록은 한
 * 건씩 처리할 때와 같습니다.
 */
@Service
@RequiredArgsConstructor
public class CampaignApplicationBulkService {
  private final CampaignApplicationService applications;

  public CampaignApplicationDtos.BulkResponse process(
      Long memberId, CampaignApplicationDtos.BulkRequest request) {
    String action = request.action() == null ? "" : request.action().trim();
    if (!action.equals("select") && !action.equals("reject")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "처리 방식은 select 또는 reject만 쓸 수 있습니다.");
    }
    List<CampaignApplicationDtos.BulkItemResult> results = new ArrayList<>();
    // 같은 신청을 두 번 보내도 한 번만 처리하고, 보낸 순서(화면 순서)대로 처리합니다.
    for (Long id : new LinkedHashSet<>(request.applicationIds())) {
      try {
        CampaignApplicationDtos.Response response =
            action.equals("select")
                ? applications.select(memberId, id)
                : applications.reject(memberId, id);
        results.add(
            new CampaignApplicationDtos.BulkItemResult(id, true, response.status().name(), null));
      } catch (ResponseStatusException exception) {
        results.add(
            new CampaignApplicationDtos.BulkItemResult(id, false, null, exception.getReason()));
      }
    }
    int success =
        (int) results.stream().filter(CampaignApplicationDtos.BulkItemResult::success).count();
    return new CampaignApplicationDtos.BulkResponse(success, results.size() - success, results);
  }
}
