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
	public void testIt() throws IOException {

		FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

				String absoluteFileName = file.toFile().getAbsolutePath();
				if (file.toFile().getAbsolutePath().endsWith(".java")) {
					String contents = com.google.common.io.Files.toString(file.toFile(), Charsets.UTF_8);
					if (!file.toFile().getAbsolutePath().endsWith("MetricsMonitor.java")) {
						
						if (contents.contains("codahale")) {
							Assertions.fail("source should not contain references to codahale: "+file);
						}
						
					}
					
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
