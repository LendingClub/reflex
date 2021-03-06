/**
 * Copyright 2017 Lending Club, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lendingclub.reflex.code;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.base.Charsets;

public class DependencyCheck {

	@Test
	public void checkForInvalidCodahaleRefs() throws IOException {

		FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

				String absoluteFileName = file.toFile().getAbsolutePath();
				if (file.toFile().getAbsolutePath().endsWith(".java")) {
					String contents = com.google.common.io.Files.toString(file.toFile(), Charsets.UTF_8);
					if (!file.toFile().getAbsolutePath().endsWith("ReflexMetrics.java")) {
						
						if (contents.contains("codahale")) {
							Assertions.fail("source should not contain references to codahale: "+file);
						}
						
					}
				
					
				}
				
				return super.visitFile(file, attrs);
			}

		};
		Files.walkFileTree(new File("./src/main/java").toPath(), visitor);
	}

	@Test
	public void checkForAmazonRefs() throws IOException {

		FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

				String absoluteFileName = file.toFile().getAbsolutePath();
				if (file.toFile().getAbsolutePath().endsWith(".java")) {
					String contents = com.google.common.io.Files.toString(file.toFile(), Charsets.UTF_8);
					
					
					if (!file.toFile().getAbsolutePath().contains("aws") && contents.contains("amazon")) {
						Assertions.fail("source should not contain references to amazon: "+file);
					}
					
				}
				
				return super.visitFile(file, attrs);
			}

		};
		Files.walkFileTree(new File("./src/main/java").toPath(), visitor);
	}
	@Test
	public void checkForPrintln() throws IOException {

		FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

				String absoluteFileName = file.toFile().getAbsolutePath();
				if (file.toFile().getAbsolutePath().endsWith(".java")) {
					String contents = com.google.common.io.Files.toString(file.toFile(), Charsets.UTF_8);
					
					if (contents.contains("println")) {
						Assertions.fail("source should not contain println: "+file);
					}
					
				}
				
				return super.visitFile(file, attrs);
			}

		};
		Files.walkFileTree(new File("./src/main/java").toPath(), visitor);
	}
}
