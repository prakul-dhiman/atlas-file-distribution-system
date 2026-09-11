package com.sdfds.repository;

import com.sdfds.entity.ShareDownloadEvent;
import com.sdfds.entity.SharedLink;
import com.sdfds.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ShareDownloadEventRepository extends JpaRepository<ShareDownloadEvent, Long> {
    List<ShareDownloadEvent> findBySharedLinkOrderByDownloadedAtDesc(SharedLink sharedLink);

    @Query("SELECT e FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user ORDER BY e.downloadedAt DESC")
    List<ShareDownloadEvent> findByUserOrderByDownloadedAtDesc(@Param("user") User user);

    @Query("SELECT COUNT(e) FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT COUNT(DISTINCT e.ipAddress) FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user")
    long countUniqueByUser(@Param("user") User user);

    @Query("SELECT COUNT(e) FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user AND e.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") String status);

    @Query("SELECT e.countryCode AS countryCode, e.country AS country, COUNT(e) AS count " +
           "FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user GROUP BY e.countryCode, e.country ORDER BY COUNT(e) DESC")
    List<Object[]> countByCountryForUser(@Param("user") User user);

    @Query("SELECT e.device AS device, COUNT(e) AS count " +
           "FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user GROUP BY e.device ORDER BY COUNT(e) DESC")
    List<Object[]> countByDeviceForUser(@Param("user") User user);

    @Query("SELECT e.browser AS browser, COUNT(e) AS count " +
           "FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user GROUP BY e.browser ORDER BY COUNT(e) DESC")
    List<Object[]> countByBrowserForUser(@Param("user") User user);

    @Query("SELECT e.operatingSystem AS os, COUNT(e) AS count " +
           "FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user GROUP BY e.operatingSystem ORDER BY COUNT(e) DESC")
    List<Object[]> countByOsForUser(@Param("user") User user);

    @Query("SELECT e.file.id AS fileId, e.file.name AS fileName, COUNT(e) AS count, SUM(COALESCE(e.fileSizeBytes, 0)) AS totalBytes " +
           "FROM ShareDownloadEvent e WHERE e.sharedLink.user = :user AND e.file IS NOT NULL " +
           "GROUP BY e.file.id, e.file.name ORDER BY COUNT(e) DESC")
    List<Object[]> findTopDownloadedFilesForUser(@Param("user") User user);
}

