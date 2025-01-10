public class Document {
    private int id;
    private String docno;
    private int length;

    public Document(int id, String docno, int length) {
        this.id = id;
        this.docno = docno;
        this.length = length;
    }

    public int getId() {
        return id;
    }

    public String getDocno() {
        return docno;
    }

    public int getLength() {
        return length;
    }
}
