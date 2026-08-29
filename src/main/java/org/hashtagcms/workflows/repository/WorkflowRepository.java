package org.hashtagcms.workflows.repository;

import org.hashtagcms.workflows.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    /**
     * Resolve a published workflow by alias, preferring the request site over the
     * master-site fallback (mirrors the PHP engine's multisite resolution).
     */
    @Query("""
        select w from Workflow w
        where w.alias = :alias and w.publishStatus = true and w.siteId in :sites
        order by case when w.siteId = :siteId then 0 else 1 end
        """)
    List<Workflow> resolve(@Param("alias") String alias,
                           @Param("sites") Collection<Long> sites,
                           @Param("siteId") Long siteId);

    Optional<Workflow> findBySiteIdAndAlias(Long siteId, String alias);
}
