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

import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.autoconfigure.kubernetes.KubernetesAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.kubernetes.Token;
import org.springframework.boot.actuate.autoconfigure.kubernetes.KubernetesAuthorizationException.Reason;

/**
 * Validator used to ensure that a signed {@link Token} has not been tampered with.
 *
 * @author Madhura Bhave
 */
class TokenValidator {

	void validate(Token token) {
		validateAlgorithm(token);
		validateExpiry(token);
		validateAudience(token);
	}

	private void validateAlgorithm(Token token) {
		String algorithm = token.getSignatureAlgorithm();
		if (algorithm == null) {
			throw new KubernetesAuthorizationException(Reason.INVALID_SIGNATURE, "Signing algorithm cannot be null");
		}
		if (!algorithm.equals("RS256")) {
			throw new KubernetesAuthorizationException(Reason.UNSUPPORTED_TOKEN_SIGNING_ALGORITHM,
					"Signing algorithm " + algorithm + " not supported");
		}
	}

	private void validateExpiry(Token token) {
	}

	private void validateAudience(Token token) {
	}

}
