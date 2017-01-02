package org.lendingclub.reflex.operator;


import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class FlatMapFilters {

	/**
	 * Filters out values that do not match the given type and emits an observable sequence with only the items of the given type.
	 * @param clazz
	 * @return
	 */
	public static <T> io.reactivex.functions.Function<Object,Observable<T>> type(Class<? extends T> clazz) {
		io.reactivex.functions.Function<Object, Observable<T>> f = new io.reactivex.functions.Function<Object, Observable<T>>() {

			@SuppressWarnings("unchecked")
			@Override
			public Observable<T> apply(Object t) throws Exception {
				
				if (Filters.type(clazz).test(t)) {
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
				
				if (Filters.json(x).test(t)) {
					JsonNode jn = (JsonNode) t;
					return (Observable<JsonNode>) Observable.just(jn);
				}
				return Observable.empty();
			}
		};
		return f;
	}

}
