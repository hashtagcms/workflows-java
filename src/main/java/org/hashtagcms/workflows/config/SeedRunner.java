package org.hashtagcms.workflows.config;

import org.hashtagcms.workflows.service.SeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** On startup, seed the directive manifest and (optionally) the example workflows. */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final SeedService seedService;
    private final WorkflowProperties properties;

    public SeedRunner(SeedService seedService, WorkflowProperties properties) {
        this.seedService = seedService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getInstall().isSeedDirectives()) {
            int n = seedService.seedDirectives();
            log.info("Seeded {} directive manifest entries.", n);
        }
        if (properties.getInstall().isSeedExamples()) {
            int n = seedService.seedExamples();
            log.info("Seeded {} example workflows.", n);
        }
    }
}
