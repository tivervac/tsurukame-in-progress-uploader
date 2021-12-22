package be.t21.tsurukame.in.progress.uploader;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
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
import reactor.util.Loggers;

public class TsurukameInProgressUploader {

	private static final String WK_URL = "https://api.wanikani.com/v2";
	private static final Logger logger = LoggerFactory.getLogger(TsurukameInProgressUploader.class);
	private static final boolean DEBUG = false;

	public static void main(String[] args) {
		CliParams params = new CliParams();
		new CommandLine(params).parseArgs(args);
		Loggers.useJdkLoggers();
		
		readFromDb(params.getApiKey(), params.getFile());
	}

	private static void readFromDb(String apiKey, File database) {
		String url = String.format("jdbc:sqlite:%s", database.getAbsoluteFile().toString());
		String query = "SELECT pb FROM pending_progress;";
		Parser<Progress> parser = WanikaniApi.Progress.parser();
		try (Connection c = DriverManager.getConnection(url);
				var s = c.createStatement();
				var rs = s.executeQuery(query)) {
			while (rs.next()) {
				var pb = rs.getBytes(1);
				Progress progress = parser.parseFrom(pb);
				Assignment assignment = progress.getAssignment();
				upload(apiKey, new ReviewWrapper(assignment.getId(), progress.getCreatedAt(),
						progress.getMeaningWrongCount(), progress.getReadingWrongCount()));
			}
		} catch (SQLException | InvalidProtocolBufferException e) {
			logger.error(e.getMessage(), e);
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
		try {
			logger.info("Sending " + id);
			client.post().uri("/reviews") //
					.contentType(MediaType.APPLICATION_JSON) //
					.body(Mono.just(rw), ReviewWrapper.class) //
					.retrieve() //
					.bodyToMono(Void.class) //
					.block();
		} catch (WebClientResponseException e) {
			logger.warn("Failed to send " + id + ". " + e.getMessage(), e);
		}
	}
}
