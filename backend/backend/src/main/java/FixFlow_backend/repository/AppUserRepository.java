package FixFlow_backend.repository;

import FixFlow_backend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.technician WHERE u.username = :username")
    Optional<AppUser> findByUsernameWithTechnician(@Param("username") String username);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.technician ORDER BY u.username ASC")
    List<AppUser> findAllWithTechnician();

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.technician WHERE u.id = :id")
    Optional<AppUser> findByIdWithTechnician(@Param("id") Long id);

    boolean existsByUsername(String username);

    long countByRole(AppUser.Role role);
}
