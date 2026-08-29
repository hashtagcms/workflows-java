package org.hashtagcms.workflows.security;

/** Thrown when a workflow marked {@code auth_required} is executed with no user. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
