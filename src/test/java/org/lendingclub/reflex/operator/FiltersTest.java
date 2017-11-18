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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.Observable;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;

public class FiltersTest {

  ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testIt() {

    Assertions.assertThat(
            Lists.newArrayList(
                Observable.just(1, "2", 33.3, "4")
                    .filter(Filters.type(String.class))
                    .blockingIterable()))
        .containsExactly("2", "4");
    Assertions.assertThat(
            Lists.newArrayList(
                Observable.just(1, "2", 33.3, "4")
                    .filter(Filters.type(Integer.class))
                    .blockingIterable()))
        .containsExactly(1);
  }

  @Test
  public void testJson() {

    ObjectNode n0 = mapper.createObjectNode().put("foo", "bar");
    ObjectNode n1 = mapper.createObjectNode().put("fizz", "buzz");

    Observable.just(n0)
        .flatMap(
            FlatMapFilters.json(
                json -> {
                  return json.path("foo").asText().equals("bar");
                }));

    Assertions.assertThat(
            ((JsonNode)
                    Observable.just(n0, n1)
                        .filter(
                            Filters.json(
                                x -> {
                                  return x.path("fizz").asText().equals("buzz");
                                }))
                        .blockingFirst())
                .path("fizz")
                .asText())
        .isEqualTo("buzz");
  }
}
