package com.toolrental.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toolrental.entity.OtpCode;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByPhoneOrderByIdDesc(String phone);

    void deleteByPhone(String phone);
}
