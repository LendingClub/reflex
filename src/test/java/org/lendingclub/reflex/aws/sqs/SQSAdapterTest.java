package org.lendingclub.reflex.aws.sqs;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SQSAdapterTest {



	@Test
	public void testDefaults() {
		SQSAdapter adapter = new SQSAdapter();
		Assertions.assertThat(adapter.isAutoDeleteEnabled()).isTrue();
		Assertions.assertThat(adapter.backoffMaxMillis).isEqualTo(60000);
	}

}
