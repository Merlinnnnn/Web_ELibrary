package com.spkt.libraSys.service.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {
    Page<UserEntity> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<UserEntity> findByRoleEntities_RoleName(String roleName, Pageable pageable);
    Page<UserEntity> findByUsernameContainingIgnoreCaseAndRoleEntities_RoleName(String username, String roleName, Pageable pageable);


    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) FROM users u WHERE u.isActive = :status")
    long countByStatus(@Param("status") UserStatus status);

    @Query("SELECT COUNT(u) FROM users u WHERE u.registrationDate > :date")
    long countNewUsers(@Param("date") LocalDate date);

    List<UserEntity> findByStudentBatch(int studentBatch);

    List<UserEntity> findByMajorCode(String majorCode);
}
