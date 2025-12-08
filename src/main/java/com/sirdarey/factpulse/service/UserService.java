package com.sirdarey.factpulse.service;

import com.sirdarey.factpulse.entity.User;
import com.sirdarey.factpulse.enums.ErrorCode;
import com.sirdarey.factpulse.exception.CustomException;
import com.sirdarey.factpulse.repo.UserRepo;
import com.sirdarey.factpulse.util.GeneralUtil;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;


    @Nonnull
    public User getUserByPhoneNo(String phoneNo) throws CustomException {
        return userRepo.findByPhoneNo(phoneNo)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.USER_NOT_FOUND,
                        "😕We couldn't find your profile in our records; kindly onboard properly")
                );

    }

    public void saveUser(String phoneNo, String name){
        log.info("saveUser[{}] :: saving user...", phoneNo);
        userRepo.save(new User(
                phoneNo,
                name,
                GeneralUtil.getTimezoneFromPhoneNumber(phoneNo))
        );

        log.info("saveUser[{}] :: user saved successfully", phoneNo);
    }
}