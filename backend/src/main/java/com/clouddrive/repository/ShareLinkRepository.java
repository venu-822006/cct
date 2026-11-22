package com.clouddrive.repository;

import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.ShareLink;
import com.clouddrive.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByToken(String token);
    Optional<ShareLink> findByFileItemAndCreatedBy(FileItem fileItem, User user);
    List<ShareLink> findByCreatedByOrderByCreatedAtDesc(User user);
    void deleteByFileItem(FileItem fileItem);
}
