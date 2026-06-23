package com.xigeandwillian.parkingsystem.result;

import lombok.Data;

import java.util.List;

/**
 * @author xige
 * @param <T>
 */

@Data
public class PageResult<T> {
    private long total;
    private List<T> dataList;
}
