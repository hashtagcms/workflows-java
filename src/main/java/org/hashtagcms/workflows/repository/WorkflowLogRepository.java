package org.hashtagcms.workflows.repository;

import org.hashtagcms.workflows.model.WorkflowLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowLogRepository extends JpaRepository<WorkflowLog, Long> {

    Page<WorkflowLog> findAllByOrderByIdDesc(Pageable pageable);

    Page<WorkflowLog> findByWorkflowAliasOrderByIdDesc(String workflowAlias, Pageable pageable);
}
