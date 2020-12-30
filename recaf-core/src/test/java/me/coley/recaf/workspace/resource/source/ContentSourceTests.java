package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.TestUtils;
import me.coley.recaf.workspace.resource.Resource;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ContentSourceTests extends TestUtils {
	@Test
	void testReadEmpty() {
		assertThrows(IllegalStateException.class, () -> testRead(new EmptyContentSource()));
	}

	@Test
	void testReadJar() {
		assertDoesNotThrow(() -> testRead(new JarContentSource(sourcesDir.resolve("sample.jar"))));
	}

	@Test
	void testReadWar() {
		assertDoesNotThrow(() -> testRead(new WarContentSource(sourcesDir.resolve("sample.war"))));
	}

	@Test
	void testReadZip() {
		assertDoesNotThrow(() -> testRead(new ZipContentSource(sourcesDir.resolve("sample.zip"))));
	}

	@Test
	void testReadDirectory() {
		assertDoesNotThrow(() -> testRead(new DirectoryContentSource(sourcesDir.resolve("sample"))));
	}

	@Test
	void testReadClass() {
		assertDoesNotThrow(() -> testRead(new ClassContentSource(sourcesDir.resolve("sample.class"))));	}

	@Test
	void testReadUrl() {
		String url = "https://repo1.maven.org/maven2/org/codejargon/feather/feather/1.0/feather-1.0.jar";
		assumeUrlExists(url);
		assertDoesNotThrow(() -> testRead(new UrlContentSource(url)));
	}

	@Test
	void testReadMaven() {
		assumeUrlExists("https://repo1.maven.org/maven2/org/codejargon/feather/feather/1.0/feather-1.0.jar");
		String groupId = "org.codejargon.feather";
		String artifactId = "feather";
		String version = "1.0";
		assertDoesNotThrow(() -> testRead(new MavenContentSource(groupId, artifactId, version)));
	}

	private void testRead(ContentSource src) throws IOException {
		Resource resource = new Resource(src);
		if (!resource.getClasses().isEmpty()) {
			throw new IllegalStateException("No classes should have been read yet");
		}
		resource.read();
		if (resource.getClasses().isEmpty()) {
			throw new IllegalStateException("Classes should have been read from content source");
		}
	}
}