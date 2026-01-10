package com.ethicalsoft.ethicalsoft_complience.common.util.mapper;

public interface Mapper<T, U> {
    U map(T t);
}