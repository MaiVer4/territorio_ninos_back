package com.software.territoriodeninos.repository;

import com.software.territoriodeninos.entity.ConfiguracionParque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionParqueRepository extends JpaRepository<ConfiguracionParque, Long> {
    Optional<ConfiguracionParque> findFirstByOrderByIdAsc();
}
