/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.test;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public abstract class MYSQLTest {
	
	private static final Logger logger = LoggerFactory.getLogger(MYSQLTest.class);
	
	protected static Integer MYSQL_PORT;
	
	@Container
	public MySQLContainer<?> mysqlcontainer = new MySQLContainer<>(DockerImageName.parse("mysql:5.6")).withDatabaseName("openmrs")
	        .withUsername("test").withPassword("test");
	
	@Before
	public void beforeMysqlTestSample() throws SQLException {
		System.out.println("initializing the testcontainer");
		logger.info("\n\nStarting MySQL container");
		mysqlcontainer.waitingFor(Wait.forHttp("/auth").forStatusCode(200));
		mysqlcontainer.withCopyFileToContainer(MountableFile.forClasspathResource("file.cnf"), "/etc/mysql/file.cnf");
		mysqlcontainer.withEnv("MYSQL_ROOT_PASSWORD", "test");
		mysqlcontainer.withDatabaseName("openmrs");
		Startables.deepStart(Stream.of(mysqlcontainer)).join();
		MYSQL_PORT = mysqlcontainer.getMappedPort(3306);
	}
	
	@Test
	public void testSimple() throws SQLException {
		try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:5.6").withConfigurationOverride("/etc/mysql/file.cnf")
		        .withLogConsumer(new Slf4jLogConsumer(logger))) {
			
			mysql.start();
			
			ResultSet resultSet = performQuery(mysql, "SELECT 1");
			int resultSetInt = resultSet.getInt(1);
			
			assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
		}
	}
	
	protected ResultSet performQuery(JdbcDatabaseContainer<?> container, String sql) throws SQLException {
		DataSource ds = getDataSource(container);
		Statement statement = ds.getConnection().createStatement();
		statement.execute(sql);
		ResultSet resultSet = statement.getResultSet();
		resultSet.next();
		return resultSet;
	
	}
	protected abstract DataSource getDataSource(JdbcDatabaseContainer<?> container);
	
	@AfterClass
	public void afterMysqlTestClass() {
		logger.info("\n\nStopping MySQL container");
		mysqlcontainer.close();
	}
	
}
