package com.clouddrive.repository;

import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileItemRepository extends JpaRepository<FileItem, Long> {

    List<FileItem> findByOwnerAndPathAndIsTrashedFalseOrderByIdDesc(User owner, String path);

    List<FileItem> findByOwnerAndIsTrashedTrueOrderByTrashedAtDesc(User owner);

    List<FileItem> findByOwnerAndIsStarredTrueAndIsTrashedFalseOrderByUpdatedAtDesc(User owner);

    List<FileItem> findTop20ByOwnerAndIsTrashedFalseAndIsDirectoryFalseOrderByUpdatedAtDesc(User owner);

    List<FileItem> findByOwnerAndNameContainingIgnoreCaseAndIsTrashedFalse(User owner, String name);

    Optional<FileItem> findByIdAndOwner(Long id, User owner);

    Optional<FileItem> findByOwnerAndPathAndNameAndIsTrashedFalse(User owner, String path, String name);

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileItem f WHERE f.owner = :owner AND f.isDirectory = false AND f.isTrashed = false")
    Long sumUsedBytesByOwner(@Param("owner") User owner);

    List<FileItem> findByOwnerAndPathStartingWith(User owner, String pathPrefix);
}
