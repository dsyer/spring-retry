/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.retry.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.config.EnableRetry;
import org.springframework.retry.config.Retryable;

/**
 * @author Dave Syer
 *
 */
public class EnableRetryTests {

	@Test
	public void vanilla() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfiguration.class);
		Service service = context.getBean(Service.class);
		service.service();
		assertEquals(3, service.getCount());
		context.close();
	}

	@Test
	public void type() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfiguration.class);
		RetryableService service = context.getBean(RetryableService.class);
		service.service();
		assertEquals(3, service.getCount());
		context.close();
	}

	@Test
	public void excludes() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfiguration.class);
		ExcludesService service = context.getBean(ExcludesService.class);
		try {
			service.service();
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
		}
		assertEquals(1, service.getCount());
		context.close();
	}

	@Test
	public void stateful() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestConfiguration.class);
		StatefulService service = context.getBean(StatefulService.class);
		for (int i = 0; i < 3; i++) {
			try {
				service.service(1);
			} catch (Exception e) {
				assertEquals("Planned", e.getMessage());
			}
		}
		assertEquals(3, service.getCount());
		context.close();
	}

	@Configuration
	@EnableRetry
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	protected static class TestConfiguration {

		@Bean
		public Service service() {
			return new Service();
		}

		@Bean
		public RetryableService retryable() {
			return new RetryableService();
		}

		@Bean
		public StatefulService stateful() {
			return new StatefulService();
		}

		@Bean
		public ExcludesService excludes() {
			return new ExcludesService();
		}

	}

	protected static class Service {

		private int count = 0;

		@Retryable(RuntimeException.class)
		public void service() {
			if (count++ < 2) {
				throw new RuntimeException("Planned");
			}
		}

		public int getCount() {
			return count;
		}

	}

	@Retryable(RuntimeException.class)
	protected static class RetryableService {

		private int count = 0;

		public void service() {
			if (count++ < 2) {
				throw new RuntimeException("Planned");
			}
		}

		public int getCount() {
			return count;
		}

	}

	protected static class ExcludesService {

		private int count = 0;

		@Retryable(include = RuntimeException.class, exclude = IllegalStateException.class)
		public void service() {
			if (count++ < 2) {
				throw new IllegalStateException("Planned");
			}
		}

		public int getCount() {
			return count;
		}

	}

	protected static class StatefulService {

		private int count = 0;

		@Retryable(stateful = true)
		public void service(int value) {
			if (count++ < 2) {
				throw new RuntimeException("Planned");
			}
		}

		public int getCount() {
			return count;
		}

	}
}
