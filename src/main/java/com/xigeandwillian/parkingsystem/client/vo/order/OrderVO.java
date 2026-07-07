package com.xigeandwillian.parkingsystem.client.vo.order;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Integer status;
    private LocalDateTime startTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
