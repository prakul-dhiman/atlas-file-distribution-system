package com.sdfds.repository;

import com.sdfds.entity.User;
import com.sdfds.entity.UserBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBrandingRepository extends JpaRepository<UserBranding, Long> {
    Optional<UserBranding> findByUser(User user);
}
