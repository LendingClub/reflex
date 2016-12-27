package org.lendingclub.rx.predicate;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.Observable;

public class FlatMapFiltersTest {

	ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testType() {

		Assertions.assertThat(
				Observable.just(1, "2", 33.3, "4").flatMap(FlatMapFilters.type(String.class)).toList().blockingGet())
				.containsExactly("2", "4");
		Assertions.assertThat(
				Observable.just(1, "2", 33.3, "4").flatMap(FlatMapFilters.type(Integer.class)).toList().blockingGet())
				.containsExactly(1);
	}

	@Test
	public void testJson() {

		ObjectNode n0 = mapper.createObjectNode().put("foo", "bar");
		ObjectNode n1 = mapper.createObjectNode().put("fizz", "buzz");

		JsonNode match = Observable.just(n0, n1).flatMap(FlatMapFilters.json(x -> {
			return x.path("fizz").asText().equals("buzz");})).blockingFirst();
		
		Assertions.assertThat(match).isSameAs(n1);
	}
}
