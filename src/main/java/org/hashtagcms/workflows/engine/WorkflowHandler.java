package org.hashtagcms.workflows.engine;

/** A code-based workflow — the Java analogue of PHP's WorkflowHandlerInterface. */
public interface WorkflowHandler {
    WorkflowResponse handle(WorkflowContext context);
}
