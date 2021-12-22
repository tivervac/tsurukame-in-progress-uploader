package be.t21.tsurukame.in.progress.uploader.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Review {
	private final long assignment_id;
	private final String created_at;
	private final int incorrect_meaning_answers;
	private final int incorrect_reading_answers;

	public Review(long assignment_id, int created_at, int incorrect_meaning_answers, int incorrect_reading_answers) {
		this.assignment_id = assignment_id;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
				.withLocale(Locale.UK).withZone(ZoneId.systemDefault());
		this.created_at = formatter.format(Instant.ofEpochSecond(created_at));
		this.incorrect_meaning_answers = incorrect_meaning_answers;
		this.incorrect_reading_answers = incorrect_reading_answers;
	}

	public long getAssignment_id() {
		return assignment_id;
	}

	public String getCreated_at() {
		return created_at;
	}

	public int getIncorrect_meaning_answers() {
		return incorrect_meaning_answers;
	}

	public int getIncorrect_reading_answers() {
		return incorrect_reading_answers;
	}

	@Override
	public String toString() {
		return "Review [assignment_id=" + assignment_id + ", created_at=" + created_at + ", incorrect_meaning_answers="
				+ incorrect_meaning_answers + ", incorrect_reading_answers=" + incorrect_reading_answers + "]";
	}
}
