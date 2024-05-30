/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.autoconfigure.kubernetes.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.kubernetes.KubernetesAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.kubernetes.SecurityResponse;
import org.springframework.boot.actuate.autoconfigure.kubernetes.Token;
import org.springframework.boot.actuate.autoconfigure.kubernetes.KubernetesAuthorizationException.Reason;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;

/**
 * Security interceptor to validate the kubernetes token.
 *
 * @author Madhura Bhave
 */
class KubernetesSecurityInterceptor {

	private static final Log logger = LogFactory.getLog(KubernetesSecurityInterceptor.class);

	private final TokenValidator tokenValidator;

	private final KubernetesSecurityService kubernetesSecurityService;

	private final String applicationId;

	private static final SecurityResponse SUCCESS = SecurityResponse.success();

	KubernetesSecurityInterceptor(TokenValidator tokenValidator,
			KubernetesSecurityService kubernetesSecurityService, String applicationId) {
		this.tokenValidator = tokenValidator;
		this.kubernetesSecurityService = kubernetesSecurityService;
		this.applicationId = applicationId;
	}

	SecurityResponse preHandle(HttpServletRequest request, EndpointId endpointId) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return SecurityResponse.success();
		}
		try {
			if (!StringUtils.hasText(this.applicationId)) {
				// throw new KubernetesAuthorizationException(Reason.SERVICE_UNAVAILABLE,
				// 		"Application id is not available");
			}
			if (this.kubernetesSecurityService == null) {
				throw new KubernetesAuthorizationException(Reason.SERVICE_UNAVAILABLE,
						"Kubernetes URL is not available");
			}
			if (HttpMethod.OPTIONS.matches(request.getMethod())) {
				return SUCCESS;
			}
			check(request, endpointId);
		}
		catch (Exception ex) {
			logger.error(ex);
			if (ex instanceof KubernetesAuthorizationException apiException) {
				return new SecurityResponse(apiException.getStatusCode(),
						"{\"security_error\":\"" + apiException.getMessage() + "\"}");
			}
			return new SecurityResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
		}
		return SecurityResponse.success();
	}

	private void check(HttpServletRequest request, EndpointId endpointId) {
		Token token = getToken(request);
		this.tokenValidator.validate(token);
		AccessLevel accessLevel = this.kubernetesSecurityService.getAccessLevel(token.toString(), this.applicationId);
		if (!accessLevel.isAccessAllowed((endpointId != null) ? endpointId.toLowerCaseString() : "")) {
			throw new KubernetesAuthorizationException(Reason.ACCESS_DENIED, "Access denied");
		}
		request.setAttribute(AccessLevel.REQUEST_ATTRIBUTE, accessLevel);
	}

	private Token getToken(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		String bearerPrefix = "bearer ";
		if (authorization == null || !authorization.toLowerCase(Locale.ENGLISH).startsWith(bearerPrefix)) {
			throw new KubernetesAuthorizationException(Reason.MISSING_AUTHORIZATION,
					"Authorization header is missing or invalid");
		}
		return new Token(authorization.substring(bearerPrefix.length()));
	}

}
