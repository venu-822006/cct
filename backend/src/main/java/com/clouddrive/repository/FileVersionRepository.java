package com.clouddrive.repository;

import com.clouddrive.entity.FileItem;
import com.clouddrive.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByFileItemOrderByVersionNumberDesc(FileItem fileItem);
    void deleteByFileItem(FileItem fileItem);
}
