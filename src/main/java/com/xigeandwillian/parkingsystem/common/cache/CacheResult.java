package com.xigeandwillian.parkingsystem.common.cache;

public class CacheResult<T> {

    public enum Status { HIT, MISS, ERROR }

    private final Status status;
    private final T data;

    private CacheResult(Status status, T data) {
        this.status = status;
        this.data = data;
    }

    public static <T> CacheResult<T> hit(T data) {
        return new CacheResult<>(Status.HIT, data);
    }

    public static <T> CacheResult<T> miss() {
        return new CacheResult<>(Status.MISS, null);
    }

    public static <T> CacheResult<T> error() {
        return new CacheResult<>(Status.ERROR, null);
    }

    public boolean isHit() {
        return status == Status.HIT;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isMiss() {
        return status == Status.MISS;
    }

    public T getData() {
        return data;
    }
}