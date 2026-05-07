package com.necronet.userregistryms.repository;

import com.necronet.userregistryms.entity.UserRole;
import com.necronet.userregistryms.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUsuarioId(Long usuarioId);
    void deleteByUsuarioIdAndRolId(Long usuarioId, Long rolId);
    boolean existsByUsuarioIdAndRolId(Long usuarioId, Long rolId);
}
