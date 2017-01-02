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
