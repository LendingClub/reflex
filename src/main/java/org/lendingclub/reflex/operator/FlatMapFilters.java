/**
 * Copyright 2017 Lending Club, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.reflex.operator;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class FlatMapFilters {

  /**
   * Filters out values that do not match the given type and emits an observable sequence with only
   * the items of the given type.
   *
   * @param clazz
   * @return
   */
  public static <T> io.reactivex.functions.Function<Object, Observable<T>> type(
      Class<? extends T> clazz) {
    io.reactivex.functions.Function<Object, Observable<T>> f =
        new io.reactivex.functions.Function<Object, Observable<T>>() {

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

  public static io.reactivex.functions.Function<Object, Observable<JsonNode>> json(
      Function<JsonNode, Boolean> x) {
    io.reactivex.functions.Function<Object, Observable<JsonNode>> f =
        new io.reactivex.functions.Function<Object, Observable<JsonNode>>() {

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
