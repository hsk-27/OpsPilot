package com.opspilot.ai.service;

import com.opspilot.ai.entity.AiInvocation;
import com.opspilot.ai.gemini.GeminiAiProvider;
import com.opspilot.ai.mock.MockAiProvider;
import com.opspilot.ai.provider.AiProvider;
import com.opspilot.ai.provider.InvoiceExtractionRequest;
import com.opspilot.ai.provider.InvoiceExtractionResult;
import com.opspilot.ai.repository.AiInvocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AiExtractionService {

    private static final Logger log = LoggerFactory.getLogger(AiExtractionService.class);

    private final MockAiProvider mockAiProvider;
    private final GeminiAiProvider geminiAiProvider;
    private final AiInvocationRepository invocationRepository;
    private final String configuredProvider;

    public AiExtractionService(
            MockAiProvider mockAiProvider,
            GeminiAiProvider geminiAiProvider,
            AiInvocationRepository invocationRepository,
            @Value("${opspilot.ai.provider:mock}") String configuredProvider) {
        this.mockAiProvider = mockAiProvider;
        this.geminiAiProvider = geminiAiProvider;
        this.invocationRepository = invocationRepository;
        this.configuredProvider = configuredProvider != null ? configuredProvider.trim().toLowerCase() : "mock";
    }

    public AiProvider resolveProvider() {
        if ("gemini".equalsIgnoreCase(configuredProvider) && geminiAiProvider.isConfigured()) {
            return geminiAiProvider;
        }
        return mockAiProvider;
    }

    @Transactional
    public InvoiceExtractionResult extractInvoice(InvoiceExtractionRequest request) {
        AiProvider provider = resolveProvider();
        Instant requestTime = Instant.now();

        log.info("Invoking AI extraction with provider '{}' for org {}", provider.getMetadata().getName(), request.getOrganizationId());
        InvoiceExtractionResult result = provider.extractInvoice(request);
        Instant responseTime = Instant.now();

        // Persist invocation record
        AiInvocation invocation = new AiInvocation();
        invocation.setOrganizationId(request.getOrganizationId());
        invocation.setInvoiceId(request.getInvoiceId());
        invocation.setProvider(provider.getMetadata().getName());
        invocation.setModel(provider.getMetadata().getModel());
        invocation.setPromptVersion("v1");
        invocation.setRequestTimestamp(requestTime);
        invocation.setResponseTimestamp(responseTime);
        invocation.setLatencyMs(result.getLatencyMs());
        invocation.setInputTokens(result.getInputTokens());
        invocation.setOutputTokens(result.getOutputTokens());
        invocation.setEstimatedCost(result.getEstimatedCost());
        invocation.setStatus(result.isSuccess() ? "SUCCESS" : "FAILURE");
        invocation.setErrorCode(result.getErrorMessage());

        if (invocationRepository != null) {
            invocationRepository.save(invocation);
        }

        return result;
    }
}
