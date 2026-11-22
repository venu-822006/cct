package com.clouddrive.repository;

import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.User;
import com.clouddrive.entity.UserShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserShareRepository extends JpaRepository<UserShare, Long> {
    List<UserShare> findBySharedWithOrderByCreatedAtDesc(User user);
    List<UserShare> findBySharedByOrderByCreatedAtDesc(User user);
    Optional<UserShare> findByFileItemAndSharedWith(FileItem fileItem, User user);
    void deleteByFileItem(FileItem fileItem);
}
