package org.hashtagcms.workflows.repository;

import org.hashtagcms.workflows.model.WorkflowDirective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkflowDirectiveRepository extends JpaRepository<WorkflowDirective, Long> {

    List<WorkflowDirective> findByPublishStatusIsTrueAndSiteIdIn(Collection<Long> siteIds);

    Optional<WorkflowDirective> findBySiteIdAndType(Long siteId, String type);
}
