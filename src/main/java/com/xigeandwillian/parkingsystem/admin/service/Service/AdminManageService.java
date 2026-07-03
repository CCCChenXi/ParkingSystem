package com.xigeandwillian.parkingsystem.admin.service.Service;

import com.xigeandwillian.parkingsystem.admin.dto.auth.AdminCreateDTO;
import com.xigeandwillian.parkingsystem.common.result.Result;

public interface AdminManageService {
    Result list();

    Result detail(Long id);

    Result create(AdminCreateDTO dto);
}
