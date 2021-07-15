package main;
import java.util.ArrayList;

public class Document {


    private int docId; //maybe don't need
    private String term;
    private ArrayList<Integer> locations; //maybe String
    private int tfTermFrequency; //term frequency
    private String docNo;
    //private int dfFrequency; //number of docs term is exist
    //private int maxTfFrequency; //max number of term at this doc
    //private int numberOfUniquWordsAtDoc;

    public Document(int docId, String docNo ,String term, int location){

        this.term = term;
        this.docId = docId;
        locations = new ArrayList<>();
        locations.add(location);
        tfTermFrequency = 1;
        this.docNo = docNo;
    }

    public Document(int docId, String docNo ,String term){

        this.term = term;
        this.docId = docId;
        tfTermFrequency = 1;
        this.docNo = docNo;
    }
    public void increaseTermFrequency() {

        tfTermFrequency++;
    }

    public int getDocId() {
        return docId;
    }

    public ArrayList<Integer> getLocations() {
        return locations;
    }

    public int getTfTermFrequency() {
        return tfTermFrequency;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public void setLocation(int locations) {

        this.locations.add(locations);
        tfTermFrequency++;
    }

    public void setLocations(ArrayList<Integer> locations){

        this.locations = locations;
    }

    public String getDocNo() {
        return docNo;
    }

    public void setTfTermFrequency(int tfTermFrequency) {
        this.tfTermFrequency = tfTermFrequency;
    }

    //should update
    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(term).append(",").append(docNo).append(",").append(tfTermFrequency);

        return stringBuilder.toString();
    }
}
