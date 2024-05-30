/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.kubernetes.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.kubernetes.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.kubernetes.KubernetesAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.kubernetes.KubernetesAuthorizationException.Reason;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Kubernetes security service to handle REST calls to the api server.
 *
 * @author Madhura Bhave
 */
class KubernetesSecurityService {

	private final RestTemplate restTemplate;

	private final String apiServerUrl;

	KubernetesSecurityService(RestTemplateBuilder restTemplateBuilder, String apiServerUrl,
			boolean skipSslValidation) {
		Assert.notNull(restTemplateBuilder, "RestTemplateBuilder must not be null");
		Assert.notNull(apiServerUrl, "ApiServerUrl must not be null");
		if (skipSslValidation) {
			restTemplateBuilder = restTemplateBuilder.requestFactory(SkipSslVerificationHttpRequestFactory.class);
		}
		this.restTemplate = restTemplateBuilder.build();
		this.apiServerUrl = apiServerUrl;
	}

	/**
	 * Return the access level that should be granted to the given token.
	 * 
	 * @param token         the token
	 * @param applicationId the kubernetes application ID
	 * @return the access level that should be granted
	 * @throws KubernetesAuthorizationException if the token is not authorized
	 */
	AccessLevel getAccessLevel(String token, String applicationId) throws KubernetesAuthorizationException {
		try {
			URI uri = getPermissionsUri(applicationId);
			Map<?, ?> review = Map.of("spec", Map.of("token", token));
			RequestEntity<?> request = RequestEntity.post(uri).header("Authorization", "bearer " + token)
					.contentType(MediaType.APPLICATION_JSON).body(review);
			Map<?, ?> body = this.restTemplate.exchange(request, Map.class).getBody();
			Map<?, ?> status = (Map<?, ?>) body.get("status");
			if (Boolean.TRUE.equals(status.get("authenticated"))) {
				return AccessLevel.FULL;
			}
			return AccessLevel.RESTRICTED;
		} catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
				throw new KubernetesAuthorizationException(Reason.ACCESS_DENIED, "Access denied");
			}
			throw new KubernetesAuthorizationException(Reason.INVALID_TOKEN, "Invalid token", ex);
		} catch (HttpServerErrorException ex) {
			throw new KubernetesAuthorizationException(Reason.SERVICE_UNAVAILABLE, "Cloud controller not reachable");
		}
	}

	private URI getPermissionsUri(String applicationId) {
		try {
			return new URI(this.apiServerUrl + "/apis/authentication.k8s.io/v1/tokenreviews");
		} catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public String getApiServerUrl() {
		return apiServerUrl;
	}

}
