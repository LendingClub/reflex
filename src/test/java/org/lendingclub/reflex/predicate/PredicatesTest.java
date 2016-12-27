package org.lendingclub.reflex.predicate;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.lendingclub.reflex.predicate.FlatMapFilters;
import org.lendingclub.reflex.predicate.Predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.Observable;

public class PredicatesTest {

	ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testIt() {

		Assertions.assertThat(Lists.newArrayList(
				Observable.just(1, "2", 33.3, "4").filter(Predicates.type(String.class)).blockingIterable()))
				.containsExactly("2", "4");
		Assertions.assertThat(Lists.newArrayList(
				Observable.just(1, "2", 33.3, "4").filter(Predicates.type(Integer.class)).blockingIterable()))
				.containsExactly(1);
	}

	@Test
	public void testJson() {

		ObjectNode n0 = mapper.createObjectNode().put("foo", "bar");
		ObjectNode n1 = mapper.createObjectNode().put("fizz", "buzz");

		Observable.just(n0).flatMap(FlatMapFilters.json(json -> {
			return json.path("foo").asText().equals("bar");
		}));

		Assertions.assertThat(((JsonNode) Observable.just(n0, n1).filter(Predicates.json(x -> {
			return x.path("fizz").asText().equals("buzz");
		})).blockingFirst()).path("fizz").asText()).isEqualTo("buzz");

	}
}
