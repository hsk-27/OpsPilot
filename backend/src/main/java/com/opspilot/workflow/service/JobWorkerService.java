package com.opspilot.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.workflow.entity.WorkflowExecution;
import com.opspilot.workflow.entity.WorkflowJob;
import com.opspilot.workflow.entity.WorkflowNodeExecution;
import com.opspilot.workflow.entity.WorkflowVersion;
import com.opspilot.workflow.model.WorkflowGraph;
import com.opspilot.workflow.model.WorkflowNode;
import com.opspilot.workflow.repository.WorkflowExecutionRepository;
import com.opspilot.workflow.repository.WorkflowJobRepository;
import com.opspilot.workflow.repository.WorkflowNodeExecutionRepository;
import com.opspilot.workflow.repository.WorkflowVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class JobWorkerService {

    private static final Logger log = LoggerFactory.getLogger(JobWorkerService.class);
    private static final String WORKER_ID = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    private final WorkflowJobRepository jobRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowEngineService workflowEngineService;
    private final ObjectMapper objectMapper;

    public JobWorkerService(
            WorkflowJobRepository jobRepository,
            WorkflowExecutionRepository executionRepository,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            WorkflowVersionRepository versionRepository,
            WorkflowEngineService workflowEngineService,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.versionRepository = versionRepository;
        this.workflowEngineService = workflowEngineService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPendingJobs() {
        Instant now = Instant.now();
        List<WorkflowJob> availableJobs = jobRepository.findAvailableJobs(now);

        for (WorkflowJob job : availableJobs) {
            claimAndExecute(job);
        }
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void recoverExpiredLeases() {
        Instant now = Instant.now();
        List<WorkflowJob> expired = jobRepository.findExpiredLeaseJobs(now);
        for (WorkflowJob job : expired) {
            log.warn("Reaping abandoned job {} lease owned by {}. Retrying with backoff.", job.getId(), job.getLeaseOwner());
            job.setStatus(WorkflowJob.Status.PENDING);
            job.setLeaseOwner(null);
            job.setLeaseExpiresAt(null);
            job.setAvailableAt(Instant.now().plusSeconds(5));
            jobRepository.save(job);
        }
    }

    private void claimAndExecute(WorkflowJob job) {
        // Lease job for 60 seconds
        job.setStatus(WorkflowJob.Status.LEASED);
        job.setLeaseOwner(WORKER_ID);
        job.setLeaseExpiresAt(Instant.now().plusSeconds(60));
        job.setAttemptCount(job.getAttemptCount() + 1);
        jobRepository.save(job);

        try {
            WorkflowExecution execution = executionRepository.findById(job.getWorkflowExecutionId()).orElse(null);
            if (execution == null || execution.getStatus() == WorkflowExecution.Status.CANCELLED) {
                job.setStatus(WorkflowJob.Status.COMPLETED);
                jobRepository.save(job);
                return;
            }

            WorkflowVersion version = versionRepository.findById(execution.getWorkflowVersionId()).orElse(null);
            if (version == null) {
                job.setStatus(WorkflowJob.Status.FAILED);
                job.setLastError("Workflow version not found");
                jobRepository.save(job);
                return;
            }

            WorkflowGraph graph = objectMapper.readValue(version.getGraphDefinitionJson(), WorkflowGraph.class);
            WorkflowNode node = graph.findNode(job.getJobType());
            if (node != null) {
                workflowEngineService.executeNodeStep(execution, graph, node, new HashMap<>());
            }

            job.setStatus(WorkflowJob.Status.COMPLETED);
            jobRepository.save(job);

        } catch (Exception ex) {
            log.error("Execution failed for job {}: {}", job.getId(), ex.getMessage(), ex);
            job.setLastError(ex.getMessage());
            if (job.getAttemptCount() >= job.getMaxAttempts()) {
                job.setStatus(WorkflowJob.Status.DEAD_LETTERED);
            } else {
                job.setStatus(WorkflowJob.Status.PENDING);
                job.setAvailableAt(Instant.now().plusSeconds(10L * job.getAttemptCount())); // Exponential backoff
            }
            jobRepository.save(job);
        }
    }
}
