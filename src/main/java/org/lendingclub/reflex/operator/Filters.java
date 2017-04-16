/**
 * Copyright 2017 Lending Club, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.reflex.operator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import io.reactivex.functions.Predicate;

public class Filters {

	public static Predicate type(Class clazz) {
		Predicate p = new Predicate() {

			@Override
			public boolean test(Object obj) throws Exception {
				if (obj == null) {
					return false;
				}
				if (clazz.isAssignableFrom(obj.getClass())) {
					return true;
				}
				return false;
			}

		};
		return p;
	}

	public static io.reactivex.functions.Predicate json(io.reactivex.functions.Function<JsonNode, Boolean> n) {

		Predicate p = new Predicate<Object>() {

			@Override
			public boolean test(Object t) throws Exception {
				if (t == null) {
					return n.apply(NullNode.instance);
				}
				if (t instanceof JsonNode) {
					return n.apply((JsonNode) t);

				}
				return false;
			}
		};

		return p;
	}

}
