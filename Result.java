public class Result implements Comparable<Result> {
    private String id;
    private double score;

    public Result(String id, double score) {
        this.id = id;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(Result other) {
        int scoreComparison = Double.compare(other.score, this.score);
        return (scoreComparison != 0) ? scoreComparison : this.id.compareTo(other.id);
    }
}