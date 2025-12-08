package com.sirdarey.factpulse.repo;

import com.sirdarey.factpulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNo(String phoneNo);
}