package com.movrit.movrit.campaignapplication.service;

import com.movrit.movrit.audit.service.AuditLogService;
import com.movrit.movrit.campaignapplication.dto.CampaignApplicationDtos;
import com.movrit.movrit.campaignapplication.entity.CampaignApplicationForm;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationFormRepository;
import com.movrit.movrit.campaignapplication.repository.CampaignApplicationRepository;
import com.movrit.movrit.collaboration.entity.Campaign;
import com.movrit.movrit.collaboration.repository.CampaignRepository;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.service.CreatorService;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.notification.service.NotificationService;
import com.movrit.movrit.product.service.ProductService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampaignApplicationEligibilityTest {

    @Test
    void verifiedEmailAllowsApplicationWithoutClaimingIdentityVerification() {
        Member member = Member.builder().emailVerifiedAt(LocalDateTime.now()).build();
        CampaignApplicationDtos.EligibilityResponse result = service(member, activeCreator(member), openCampaign(), false)
                .getEligibility(1L, 10L);

        assertThat(result.canApply()).isTrue();
        assertThat(result.emailVerified()).isTrue();
        assertThat(result.identityVerified()).isFalse();
        assertThat(result.applicationEligibilityPolicy()).isEqualTo("EMAIL_TEMPORARY");
        assertThat(result.blockingReasons()).isEmpty();
    }

    @Test
    void unverifiedEmailIsBlockedByServerState() {
        Member member = Member.builder().build();
        CampaignApplicationDtos.EligibilityResponse result = service(member, activeCreator(member), openCampaign(), false)
                .getEligibility(1L, 10L);

        assertThat(result.canApply()).isFalse();
        assertThat(result.blockingReasons()).containsExactly("EMAIL_VERIFICATION_REQUIRED");
    }

    @Test
    void requestCannotSpoofVerificationFlags() {
        assertThat(Arrays.stream(CampaignApplicationDtos.CreateRequest.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("emailVerified", "identityVerified", "channelReady");
    }

    @Test
    void existingCampaignChecksRemainVisibleInEligibility() {
        Member member = Member.builder().emailVerifiedAt(LocalDateTime.now()).build();
        Creator creator = Creator.builder()
                .id(20L)
                .member(member)
                .status("PENDING")
                .followerCount(5L)
                .snsUrl(null)
                .build();
        Campaign closed = Campaign.builder()
                .id(10L)
                .open(false)
                .startAt(LocalDateTime.now().minusDays(2))
                .endAt(LocalDateTime.now().minusDays(1))
                .minFollowerCount(100L)
                .build();

        CampaignApplicationDtos.EligibilityResponse result = service(member, creator, closed, true)
                .getEligibility(1L, 10L);

        assertThat(result.canApply()).isFalse();
        assertThat(result.blockingReasons()).containsExactly(
                "CAMPAIGN_NOT_OPEN",
                "CREATOR_NOT_APPROVED",
                "FOLLOWER_REQUIREMENT_NOT_MET",
                "CHANNEL_REQUIRED",
                "ALREADY_APPLIED"
        );
    }

    private CampaignApplicationService service(Member member, Creator creator, Campaign campaign, boolean alreadyApplied) {
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        CampaignApplicationFormRepository formRepository = mock(CampaignApplicationFormRepository.class);
        CampaignApplicationRepository applicationRepository = mock(CampaignApplicationRepository.class);
        CreatorService creatorService = mock(CreatorService.class);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        when(formRepository.findByCampaignId(10L)).thenReturn(Optional.of(CampaignApplicationForm.builder()
                .campaign(campaign)
                .version(3L)
                .build()));
        when(creatorService.getByMember(1L)).thenReturn(creator);
        when(applicationRepository.existsByCampaignIdAndCreatorId(10L, creator.getId())).thenReturn(alreadyApplied);
        return new CampaignApplicationService(
                campaignRepository,
                formRepository,
                applicationRepository,
                creatorService,
                mock(ProductService.class),
                new ObjectMapper(),
                mock(NotificationService.class),
                mock(AuditLogService.class)
        );
    }

    private Creator activeCreator(Member member) {
        return Creator.builder()
                .id(20L)
                .member(member)
                .status("ACTIVE")
                .followerCount(1_000L)
                .snsUrl("https://example.com/channel")
                .build();
    }

    private Campaign openCampaign() {
        return Campaign.builder()
                .id(10L)
                .open(true)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(1))
                .minFollowerCount(100L)
                .build();
    }
}
