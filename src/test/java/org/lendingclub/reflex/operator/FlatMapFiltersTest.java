package org.lendingclub.reflex.operator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.lendingclub.reflex.operator.FlatMapFilters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

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
			return x.path("fizz").asText().equals("buzz");
		})).blockingFirst();

		Assertions.assertThat(match).isSameAs(n1);
	}

	@Test
	public void testJsonFilter() {
		ObjectMapper m = new ObjectMapper();

		Object a = m.createObjectNode().put("a", "1");
		Object b = new Integer(2);
		Object c = m.createObjectNode().put("c", "3");

		Assertions.assertThat(Observable.just(a, b, c).flatMap(FlatMapFilters.json(n -> {
			return n.path("c").asText().equals("3");
		})).blockingFirst()).isSameAs(c);

	}

	@Test
	public void testSequence() {
		ObjectMapper m = new ObjectMapper();
		
		Object a = m.createObjectNode().put("a", "1");
		Object b = new Integer(2);
		Object c = m.createObjectNode().put("c", "3");
		
	
		List<String> calls = Lists.newArrayList();
		Observer<Object> observer  = new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
				calls.add("onSubscribe");
				
			}

			@Override
			public void onNext(Object t) {
				calls.add("onNext");
				
			}

			@Override
			public void onError(Throwable e) {
				calls.add("onError");
				
			}

			@Override
			public void onComplete() {
				calls.add("onComplete");
				
			}
		};
				
		Observable.just(a,b,c).flatMap(FlatMapFilters.json(n -> {return true;
		})).subscribe(observer);
		
		Assertions.assertThat(calls).containsExactly("onSubscribe","onNext","onNext","onComplete");
		
		
	}
}
