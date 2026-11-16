package com.toolrental.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toolrental.entity.User;
import com.toolrental.exception.ApiException;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findBySocialProviderAndSocialId(String socialProvider, String socialId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    default User getOrThrow(Long id) {
        return findById(id).orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
    }
}
