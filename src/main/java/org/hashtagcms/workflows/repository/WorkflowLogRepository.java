package org.hashtagcms.workflows.repository;

import org.hashtagcms.workflows.model.WorkflowLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowLogRepository extends JpaRepository<WorkflowLog, Long> {
}
