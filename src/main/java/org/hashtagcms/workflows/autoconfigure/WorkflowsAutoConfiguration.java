package org.hashtagcms.workflows.autoconfigure;

import org.hashtagcms.workflows.WorkflowsApplication;
import org.hashtagcms.workflows.config.WorkflowProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Auto-configuration for the HashtagCMS Workflows engine.
 *
 * <p>Registered from {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so any Spring Boot application that puts this library on its classpath gets the
 * full engine — controllers, services, the directive manifest, seeding, and the
 * auth resolver — with no {@code @Import} or {@code @ComponentScan} of its own.
 *
 * <p>It component-scans the library's own beans and, rather than declaring its own
 * {@code @EnableJpaRepositories}/{@code @EntityScan} (which would make Spring Boot
 * stop scanning the <em>host</em> application's entities and repositories), it adds
 * the library's base package to {@link AutoConfigurationPackages}. Boot's normal JPA
 * auto-configuration then discovers this library's entities and repositories
 * <em>alongside</em> the host's — additive, never disabling the host's own.
 *
 * <p>Everything is overridable: every bean the library defines is
 * {@code @ConditionalOnMissingBean}, so declaring your own (e.g. a custom
 * {@link org.hashtagcms.workflows.security.WorkflowUserResolver}) wins.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@EnableConfigurationProperties(WorkflowProperties.class)
@ComponentScan(
        basePackages = "org.hashtagcms.workflows",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = { WorkflowsAutoConfiguration.class, WorkflowsApplication.class }))
@Import(WorkflowsAutoConfiguration.LibraryPackageRegistrar.class)
public class WorkflowsAutoConfiguration {

    /** Adds the library's package to Boot's auto-configuration packages so its JPA
     *  entities and repositories are scanned together with the host application's. */
    static class LibraryPackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, "org.hashtagcms.workflows");
        }
    }
}
