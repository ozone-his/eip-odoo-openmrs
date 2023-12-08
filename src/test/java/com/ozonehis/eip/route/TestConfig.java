package com.ozonehis.eip.route;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.openmrs.eip.Constants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

public class TestConfig {

    @Bean(name = "openmrsTestEntityManager")
    @DependsOn(Constants.COMMON_PROP_SOURCE_BEAN_NAME)
    public LocalContainerEntityManagerFactoryBean entityManager(
            final EntityManagerFactoryBuilder builder,
            @Qualifier(Constants.OPENMRS_DATASOURCE_NAME) final DataSource dataSource) {

        return builder.dataSource(dataSource)
                .packages("org.openmrs.eip")
                .persistenceUnit("openmrs")
                .build();
    }

    @Primary
    @Bean(name = "openmrsTestTxManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("openmrsTestEntityManager") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("openmrsSpringLiquibase")
    public SpringLiquibase getSpringLiquibaseForOpenmrsDB(
            @Qualifier(Constants.OPENMRS_DATASOURCE_NAME) DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:liquibase-openmrs.xml");
        liquibase.setDatabaseChangeLogTable("LIQUIBASECHANGELOG");
        liquibase.setDatabaseChangeLogLockTable("LIQUIBASECHANGELOGLOCK");
        liquibase.setShouldRun(true);

        return liquibase;
    }
}
