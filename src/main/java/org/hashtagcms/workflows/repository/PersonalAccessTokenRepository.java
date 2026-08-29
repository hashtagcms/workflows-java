package org.hashtagcms.workflows.repository;

import org.hashtagcms.workflows.model.PersonalAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, Long> {
}
