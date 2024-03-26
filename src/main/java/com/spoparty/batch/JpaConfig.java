package com.spoparty.batch;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Configuration
public class JpaConfig {


	// 기본 db로 jpa 엔티티 매니저 빈 등록
	@PersistenceContext(unitName = "springBatchEntityManager")
	private EntityManager springBatchEntityManager;

}