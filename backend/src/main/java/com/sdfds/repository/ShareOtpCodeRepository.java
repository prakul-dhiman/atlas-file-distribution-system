package com.sdfds.repository;

import com.sdfds.entity.ShareOtpCode;
import com.sdfds.entity.SharedLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ShareOtpCodeRepository extends JpaRepository<ShareOtpCode, Long> {

    Optional<ShareOtpCode> findTopBySharedLinkAndEmailAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            SharedLink link, String email, Instant now);

    @Modifying
    @Query("DELETE FROM ShareOtpCode o WHERE o.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
