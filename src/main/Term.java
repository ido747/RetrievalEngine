package main;
public class Term {

    private String name;
    private int numOfTotalDocs;
    private int totalFrequencyInCorpus;
    private String postingFilrPointer;

    public Term(String name,int totalFrequencyInCorpus, String postingFilrPointer){

        this.postingFilrPointer = postingFilrPointer;
        this.name = name;
        this.totalFrequencyInCorpus = totalFrequencyInCorpus;
        this.numOfTotalDocs = 1;
    }

    public Term(String name,int totalFrequencyInCorpus, String postingFilrPointer, int numOfTotalDocs){

        this.postingFilrPointer = postingFilrPointer;
        this.name = name;
        this.totalFrequencyInCorpus = totalFrequencyInCorpus;
        this.numOfTotalDocs = numOfTotalDocs;
    }

    public String getName() {
        return name;
    }

    public String getPostingFilrPointer() {
        return postingFilrPointer;
    }

    public int getNumOfTotalDocs() {
        return numOfTotalDocs;
    }

    public int getTotalFrequencyInCorpus() {
        return totalFrequencyInCorpus;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void increaseNumOfTotalDocs(){

        this.numOfTotalDocs++;
    }

    public void increaseNumOfTotalDocs(int numToIncrease){

        this.numOfTotalDocs += numToIncrease;
    }

    public void increaseTotalFrequencyInCorpus(int numToIncrease) {

        this.totalFrequencyInCorpus += numToIncrease;
    }

    @Override
    public String toString() {

        return getPostingFilrPointer() + "," + this.getNumOfTotalDocs() + "," + getTotalFrequencyInCorpus();
    }
}
