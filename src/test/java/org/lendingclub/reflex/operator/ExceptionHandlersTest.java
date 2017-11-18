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

import com.google.common.collect.Lists;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.reflex.operator.ExceptionHandlers.UncaughtSubscriberExceptionHandler;
import org.slf4j.LoggerFactory;

public class ExceptionHandlersTest {

  @Test
  public void testSafeObserver() {

    Observer<Integer> x =
        new Observer<Integer>() {

          @Override
          public void onSubscribe(Disposable d) {}

          @Override
          public void onNext(Integer t) {

            if (t == 2) {
              throw new RuntimeException("simulated");
            }
            System.out.println("onNext(" + t + ")");
          }

          @Override
          public void onError(Throwable e) {
            // TODO Auto-generated method stub

          }

          @Override
          public void onComplete() {
            // TODO Auto-generated method stub

          }
        };

    Observable.just(1, 2, 3).subscribe(ExceptionHandlers.safeObserver(x));
  }

  @Test
  public void testIt() {

    List<Integer> list = Lists.newCopyOnWriteArrayList();

    Observer<Integer> x =
        new Observer<Integer>() {

          @Override
          public void onSubscribe(Disposable d) {
            // TODO Auto-generated method stub

          }

          @Override
          public void onNext(Integer t) {
            if (t == 3) {
              throw new RuntimeException();
            }
            list.add(t);
          }

          @Override
          public void onError(Throwable e) {
            System.out.println(e);
          }

          @Override
          public void onComplete() {
            // TODO Auto-generated method stub

          }
        };

    list.clear();
    Observable.range(0, 5).subscribe(ExceptionHandlers.safeObserver(x));
    Assertions.assertThat(list).containsExactly(0, 1, 2, 4);

    list.clear();
    Observable.range(0, 5)
        .subscribe(ExceptionHandlers.safeObserver(x, LoggerFactory.getLogger("foo")));
    Assertions.assertThat(list).containsExactly(0, 1, 2, 4);

    UncaughtSubscriberExceptionHandler handler =
        new UncaughtSubscriberExceptionHandler() {

          @Override
          public void accept(Throwable t1, Object t2) throws Exception {
            // TODO Auto-generated method stub

          }
        };
    list.clear();
    Observable.range(0, 5).subscribe(ExceptionHandlers.safeObserver(x, handler));
    Assertions.assertThat(list).containsExactly(0, 1, 2, 4);

    UncaughtSubscriberExceptionHandler brokenHandler =
        new UncaughtSubscriberExceptionHandler() {

          @Override
          public void accept(Throwable t1, Object t2) {

            throw new RuntimeException("broken handler!");
          }
        };
    list.clear();
    Observable.range(0, 5).subscribe(ExceptionHandlers.safeObserver(x, brokenHandler));
    Assertions.assertThat(list).containsExactly(0, 1, 2, 4);
  }
}
