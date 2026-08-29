package org.hashtagcms.workflows.repository;

import org.hashtagcms.workflows.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by the `jwt` driver's link-by-email option to map an SSO identity to a local account. */
    Optional<User> findByEmailIgnoreCase(String email);
}
