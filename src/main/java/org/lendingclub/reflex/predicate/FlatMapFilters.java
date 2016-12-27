package org.lendingclub.reflex.predicate;


import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class FlatMapFilters {

	public static <T> io.reactivex.functions.Function<Object,Observable<T>> type(Class<? extends T> clazz) {
		io.reactivex.functions.Function<Object, Observable<T>> f = new io.reactivex.functions.Function<Object, Observable<T>>() {

			@Override
			public Observable<T> apply(Object t) throws Exception {
				if (Predicates.type(clazz).test(t)) {
					return (Observable<T>) Observable.just(t);
				}
				return Observable.empty();
			}
		};
		return f;
	}
	
	public static  io.reactivex.functions.Function<Object,Observable<JsonNode>> json(Function<JsonNode, Boolean> x) {
		io.reactivex.functions.Function<Object, Observable<JsonNode>> f = new io.reactivex.functions.Function<Object, Observable<JsonNode>>() {

			@Override
			public Observable<JsonNode> apply(Object t) throws Exception {
				
				if (Predicates.json(x).test(t)) {
					JsonNode jn = (JsonNode) t;
					return (Observable<JsonNode>) Observable.just(jn);
				}
				return Observable.empty();
			}
		};
		return f;
	}

}
