package be.t21.tsurukame.in.progress.uploader.cli;

import java.io.File;

import picocli.CommandLine.Parameters;

public class CliParams {

	@Parameters(index = "0", description = "Your API token")
	private String apiKey;
	
	@Parameters(index = "1", description = "The location of exported local database")
	private File file;

	public String getApiKey() {
		return apiKey;
	}

	public File getFile() {
		return file;
	}
}
