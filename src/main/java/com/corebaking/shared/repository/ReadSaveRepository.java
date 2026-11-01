package com.corebaking.shared.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface ReadSaveRepository <T, ID> extends Repository<T, ID> {

    <S extends T> S save(S entity);
    long count();
}
