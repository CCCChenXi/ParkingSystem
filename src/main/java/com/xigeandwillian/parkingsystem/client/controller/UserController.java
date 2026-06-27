package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.dto.user.*;
import com.xigeandwillian.parkingsystem.client.service.Service.UserService;
import com.xigeandwillian.parkingsystem.client.service.Service.VehicleService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final UserService userService;
    private final VehicleService vehicleService;

    /**
     * 注册
     * @author xige
     * @param registerDTO
     * @return
     */
    @PostMapping("/register")
    public Result register(@Validated @RequestBody RegisterDTO registerDTO) {
        log.info("现在执行注册");
        return userService.register(registerDTO);
    }

    /**
     * 发送验证码
     * @author xige
     * @param code
     * @return
     */
    @PostMapping("/send-code")
    public Result sendCode(@RequestBody CodeDTO code){
        log.info("现在执行发送验证码");
        return userService.sendCode(code.getPhone());
    }
    /**
     * 登录
     * @author willian
     * @param userLoginDtTO
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO userLoginDtTO) {
        log.info("现在执行登录");
        return userService.login(userLoginDtTO);
    }

    /**
     * 获取用户信息
     * @author willian
     * @return
     */
    @GetMapping("/profile")
    public Result profile(){
        log.info("现在执行获取用户信息");
        return userService.userProfile();
    }
    /**
     * 修改用户信息
     * @author willian
     * @return
     */
    @PutMapping("/profile")
    public Result editProfile(@RequestBody ProfileEditDTO profileEditDTO){
        log.info("现在执行修改用户信息");
        return userService.editProfile(profileEditDTO);
    }

    /**
     * 获取用户车辆信息
     * @author willian
     * @return
     */
    @GetMapping("/vehicles")
    public Result vehiclesInfo(){
        log.info("现在执行获取用户车辆信息");
        return vehicleService.vehiclesInfo();
    }

    /**
     * 添加车辆
     * @author willian
     * @return
     */
    @PostMapping("/vehicles")
    public Result addVehicle(@Validated @RequestBody VehicleDTO vehicleDTO){
        log.info("现在执行添加车辆");
        return vehicleService.addVehicle(vehicleDTO);
    }

    /**
     * 删除车辆
     * @author willian
     * @return
     */
    @DeleteMapping("/vehicles/{id}")
    public Result deleteVehicle(@PathVariable Long id){
        log.info("现在执行删除车辆");
        return vehicleService.deleteVehicle(id);
    }


}
