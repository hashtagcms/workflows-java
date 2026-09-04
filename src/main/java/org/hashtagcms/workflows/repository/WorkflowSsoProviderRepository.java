package org.hashtagcms.workflows.repository;

import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Selects the SSO provider(s) that apply to a site, mirroring how the workflow
 * engine resolves a workflow: the site's own enabled+published provider wins over
 * the master-site fallback, then the lowest id keeps the pick deterministic.
 */
public interface WorkflowSsoProviderRepository extends JpaRepository<WorkflowSsoProvider, Long> {

    /**
     * Enabled+published providers applicable to a site (its own row plus the
     * master fallback), site-first then by id. The site default is the first
     * element; the full list powers cross-site pin resolution.
     */
    @Query("""
        select p from WorkflowSsoProvider p
        where p.enabled = true and p.publishStatus = true and p.siteId in :sites
        order by case when p.siteId = :siteId then 0 else 1 end, p.id asc
        """)
    List<WorkflowSsoProvider> forSite(@Param("sites") Collection<Long> sites,
                                      @Param("siteId") Long siteId);

    /** A specific provider pinned by a workflow, resolved for the given site (site-first). */
    @Query("""
        select p from WorkflowSsoProvider p
        where p.alias = :alias and p.enabled = true and p.publishStatus = true and p.siteId in :sites
        order by case when p.siteId = :siteId then 0 else 1 end, p.id asc
        """)
    List<WorkflowSsoProvider> byAlias(@Param("alias") String alias,
                                      @Param("sites") Collection<Long> sites,
                                      @Param("siteId") Long siteId);

    /** Whether any site has an enabled, published provider (fast existence check). */
    boolean existsByEnabledTrueAndPublishStatusTrue();

    /** Alias uniqueness check for the admin API (per site). */
    boolean existsBySiteIdAndAlias(Long siteId, String alias);
}
