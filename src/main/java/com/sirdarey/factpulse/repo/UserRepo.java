package com.sirdarey.factpulse.repo;

import com.sirdarey.factpulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {

    Optional<User> findByPhoneNo(String phoneNo);
}