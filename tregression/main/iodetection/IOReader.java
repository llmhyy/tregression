package iodetection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class IOReader {

	public Optional<String[]> readInputs(final Path path) {
		try {
			String[] lines = getLines(path);
			String inputLine = lines[0];
			if (inputLine.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(inputLine.split(IOWriter.VAR_VAL_DELIMITER));
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public Optional<String> readOutput(final Path path) {
		try {
			String[] lines = getLines(path);
			return Optional.of(lines[1]);
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		} catch (IndexOutOfBoundsException e) {
			return Optional.empty();
		}
	}

	private String[] getLines(final Path path) throws IOException {
		String fileContents = Files.readString(path);
		return fileContents.split(System.lineSeparator());
	}
}
