package be.t21.tsurukame.in.progress.uploader.model;

public class ReviewWrapper {
	private final Review review;

	public ReviewWrapper(long assignment_id, int created_at, int incorrect_meaning_answers,
			int incorrect_reading_answers) {
		review = new Review(assignment_id, created_at, incorrect_meaning_answers, incorrect_reading_answers);
	}

	public Review getReview() {
		return review;
	}

	@Override
	public String toString() {
		return "ReviewWrapper [review=" + review + "]";
	}
}
