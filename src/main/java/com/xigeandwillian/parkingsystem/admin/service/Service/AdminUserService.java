package com.xigeandwillian.parkingsystem.admin.service.Service;

import com.xigeandwillian.parkingsystem.common.result.Result;

public interface AdminUserService {
    Result page(Integer page, Integer size, String keyword);

    Result detail(Long id);
}
