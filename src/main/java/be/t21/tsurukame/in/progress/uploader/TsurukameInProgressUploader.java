package be.t21.tsurukame.in.progress.uploader;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

import be.t21.tsurukame.in.progress.uploader.cli.CliParams;
import be.t21.tsurukame.in.progress.uploader.model.ReviewWrapper;
import be.t21.tsurukame.in.progress.uploader.proto.WanikaniApi;
import be.t21.tsurukame.in.progress.uploader.proto.WanikaniApi.Assignment;
import be.t21.tsurukame.in.progress.uploader.proto.WanikaniApi.Progress;
import picocli.CommandLine;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class TsurukameInProgressUploader {

	private static final String WK_URL = "https://api.wanikani.com/v2";
	private static final Logger logger = LoggerFactory.getLogger(TsurukameInProgressUploader.class);
	private static final boolean DEBUG = true;

	public static void main(String[] args) throws InterruptedException {
		CliParams params = new CliParams();
		new CommandLine(params).parseArgs(args);

		readFromDb(params.getApiKey(), params.getFile());
	}

	private static void readFromDb(String apiKey, File database) throws InterruptedException {
		String url = String.format("jdbc:sqlite:%s", database.getAbsoluteFile().toString());
		String query = "SELECT pb FROM pending_progress;";
		Parser<Progress> parser = WanikaniApi.Progress.parser();
		ArrayList<Progress> progresses = new ArrayList<>();
		try (Connection c = DriverManager.getConnection(url);
				var s = c.createStatement();
				var rs = s.executeQuery(query)) {
			while (rs.next()) {
				var pb = rs.getBytes(1);
				Progress progress = parser.parseFrom(pb);
				progresses.add(progress);
			}
		} catch (SQLException | InvalidProtocolBufferException e) {
			logger.error(e.getMessage(), e);
		}

		int originalCount = progresses.size();
		long iterations = 0;
		// We continue as long as we still have items to upload
		while (!progresses.isEmpty()) {
			logger.info(String.format("Starting iteration %d. %d/%d reviews remaining", ++iterations, progresses.size(),
					originalCount));
			Iterator<Progress> iterator = progresses.iterator();
			// Let's try to send the current queue
			while (iterator.hasNext()) {
				Progress progress = iterator.next();
				Assignment assignment = progress.getAssignment();
				long id = assignment.getId();

				try {
					upload(apiKey, new ReviewWrapper(assignment.getId(), progress.getCreatedAt(),
							progress.getMeaningWrongCount(), progress.getReadingWrongCount()));
					iterator.remove();
				} catch (WebClientResponseException e) {
					int statusCode = e.getRawStatusCode();
					if (statusCode == 422) {
						// Most likely we already handled this review, so remove it
						iterator.remove();
						logger.info("Handled " + id);
					} else if (statusCode == 429) {
						if (iterations == 1) {
							// We want to clear the list as much as possible in the first iteration
							// and only start sleeping on the second iteration
							continue;
						}
						// We're overloading WaniKani so let's slow down
						logger.info("Sleeping for 60s to avoid overloading WaniKani.");
						Thread.sleep(60 * 1000L);
					} else {
						logger.warn("Failed to send " + id + ". " + e.getMessage(), e);
					}
				} catch (WebClientRequestException e) {
					logger.warn("Failed to send " + id + ". " + e.getMessage(), e);
				}
			}
		}
	}

	private static void upload(String apiKey, ReviewWrapper rw) {
		Builder builder = WebClient.builder() //
				.baseUrl(WK_URL) //
				.defaultHeaders(headers -> headers.setBearerAuth(apiKey));
		if (DEBUG) {
			builder.clientConnector( //
					new ReactorClientHttpConnector(HttpClient.create().wiretap(true)) //
			);
		}
		WebClient client = builder.build();

		long id = rw.getReview().getAssignment_id();
		logger.info("Sending " + id);
		client.post().uri("/reviews") //
				.contentType(MediaType.APPLICATION_JSON) //
				.body(Mono.just(rw), ReviewWrapper.class) //
				.retrieve() //
				.bodyToMono(Void.class) //
				.block();
	}
}
