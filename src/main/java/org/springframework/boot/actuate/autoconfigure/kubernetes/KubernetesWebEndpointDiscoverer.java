/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.kubernetes;

import java.util.Collection;
import java.util.List;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.autoconfigure.kubernetes.KubernetesWebEndpointDiscoverer.KubernetesWebEndpointDiscovererRuntimeHints;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * {@link WebEndpointDiscoverer} for Kubernetes that uses Kubernetes specific
 * extensions for the {@link HealthEndpoint}.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@ImportRuntimeHints(KubernetesWebEndpointDiscovererRuntimeHints.class)
public class KubernetesWebEndpointDiscoverer extends WebEndpointDiscoverer {

	/**
	 * Create a new {@link WebEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param parameterValueMapper the parameter value mapper
	 * @param endpointMediaTypes the endpoint media types
	 * @param endpointPathMappers the endpoint path mappers
	 * @param invokerAdvisors invoker advisors to apply
	 * @param filters filters to apply
	 */
	public KubernetesWebEndpointDiscoverer(ApplicationContext applicationContext,
			ParameterValueMapper parameterValueMapper, EndpointMediaTypes endpointMediaTypes,
			List<PathMapper> endpointPathMappers, Collection<OperationInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<ExposableWebEndpoint>> filters) {
		super(applicationContext, parameterValueMapper, endpointMediaTypes, endpointPathMappers, invokerAdvisors,
				filters);
	}

	@Override
	protected boolean isExtensionTypeExposed(Class<?> extensionBeanType) {
		// Filter regular health endpoint extensions so a Kubernetes version can replace them
		return !isHealthEndpointExtension(extensionBeanType)
				|| isKubernetesHealthEndpointExtension(extensionBeanType);
	}

	private boolean isHealthEndpointExtension(Class<?> extensionBeanType) {
		return MergedAnnotations.from(extensionBeanType)
			.get(EndpointWebExtension.class)
			.getValue("endpoint", Class.class)
			.map(HealthEndpoint.class::isAssignableFrom)
			.orElse(false);
	}

	private boolean isKubernetesHealthEndpointExtension(Class<?> extensionBeanType) {
		return MergedAnnotations.from(extensionBeanType).isPresent(EndpointKubernetesExtension.class);
	}

	static class KubernetesWebEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection()
				.registerType(KubernetesEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
