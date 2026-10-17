package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.campaigneligibility.dto.ConfiguredCampaignDtos;
import com.movrit.movrit.campaigneligibility.service.ConfiguredCampaignService;
import com.movrit.movrit.campaigneligibility.service.CampaignEligibilityPolicyService;
import com.movrit.movrit.campaignapplication.service.CampaignApplicationService;
import com.movrit.movrit.collaboration.dto.CollaborationDtos;
import com.movrit.movrit.collaboration.service.CollaborationService;
import com.movrit.movrit.collaboration.service.CampaignManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 실제 DB 롤백 검수가 아니라 configured 서비스의 Spring 트랜잭션 경계를 검증합니다. */
class ConfiguredMediaTransactionTest {
    private final CollaborationService collaboration = mock(CollaborationService.class);
    private final CampaignManagementService management = mock(CampaignManagementService.class);
    private final CampaignEligibilityPolicyService policies = mock(CampaignEligibilityPolicyService.class);
    private final CampaignApplicationService forms = mock(CampaignApplicationService.class);
    private final CampaignMediaService media = mock(CampaignMediaService.class);
    private final ConfiguredCampaignService target = new ConfiguredCampaignService(collaboration, management, policies, forms, media);

    @Test void lateMediaFailureRollsBackConfiguredTransactionRatherThanCommittingEarlierStages() {
        var campaign = mock(CollaborationDtos.CampaignResponse.class);
        when(campaign.id()).thenReturn(10L);
        when(collaboration.createCampaign(eq(1L), any())).thenReturn(campaign);
        var input = new com.movrit.movrit.campaignmedia.dto.CampaignMediaDtos.PresentationInput(null, List.of(), null);
        when(media.apply(1L, 10L, input, null, true)).thenThrow(new IllegalArgumentException("검수용 미디어 실패"));
        RecordingTransactions transactions = new RecordingTransactions();
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        ConfiguredCampaignService proxy = (ConfiguredCampaignService) factory.getProxy();
        assertThrows(IllegalArgumentException.class, () -> proxy.create(1L,
                new ConfiguredCampaignDtos.CreateRequest(null, null, ConfiguredCampaignDtos.RequirementMode.NONE, null, input)));
        verify(forms).saveForm(1L, 10L, null);
        assertEquals(1, transactions.rollbacks); assertEquals(0, transactions.commits);
    }

    @Test void preflightConflictPreventsAnyBodyFormOrConditionWrite() {
        doThrow(new IllegalStateException("검수용 버전 충돌")).when(media).preflightUpdate(1L, 10L, null, 0L, null);
        assertThrows(IllegalStateException.class, () -> target.update(1L, 10L,
                new ConfiguredCampaignDtos.UpdateRequest(null, null, null,
                        ConfiguredCampaignDtos.RequirementMode.NONE, null, null, null, 0L)));
        verifyNoInteractions(management, forms, policies);
    }

    static class RecordingTransactions extends AbstractPlatformTransactionManager {
        int commits, rollbacks;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
