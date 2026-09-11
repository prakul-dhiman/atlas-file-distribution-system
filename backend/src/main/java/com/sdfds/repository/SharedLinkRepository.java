package com.sdfds.repository;

import com.sdfds.entity.SharedLink;
import com.sdfds.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedLinkRepository extends JpaRepository<SharedLink, Long> {

    Optional<SharedLink> findByToken(String token);

    List<SharedLink> findByUser(User user);

    List<SharedLink> findByIsActiveTrueAndExpiresAtBefore(java.time.Instant now);

    List<SharedLink> findByIsActiveTrueAndMaxAccessCountIsNotNull();
}
