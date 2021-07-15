package main;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;
import javafx.util.Pair;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Indexer {

    private String path;
    private ArrayList<String> postings;
    private HashMap<String, String> docInfo;
    private int countDocs;
    private HashMap<String, Term> dictionary;
    private HashMap<String,Integer> entities;
    private boolean stemming;
    private int index;
    private double totalIndexDocs;
    private double avgDocLength;
    private double totalDocLength;
    private HashMap<String, ArrayList<Pair<String, Integer>>> entitiesInDoc;


    public Indexer(String path, boolean stem) {

        postings = new ArrayList<>();
        countDocs = 0;
        dictionary = new HashMap<>();
        this.path = path;
        this.stemming=stem;
        docInfo = new HashMap<>();
        index = 1;
        entities = new HashMap<>();
        entitiesInDoc = new HashMap<>();

    }

    public void setPath(String path){
        this.path = path;
    }

    public Boolean getStemming(){
        return this.stemming;
    }

    public void setStemming(boolean stemming) {
        this.stemming = stemming;
    }

    public void setDictionary(HashMap<String, Term> dictionary) {
        this.dictionary = dictionary;
    }

    public HashMap<String,Term> getDictionary(){
        return dictionary;
    }

    public int getDictionarySize(){
        return dictionary.size();
    }

    //docID, terms, maxTermFrequency, terms.size()
    public void IndexFiles(String docNo, HashMap<String, Document>tableofTerms,int max_tf, int uniqueWordsSize, int docLength) {

        totalIndexDocs++;
        totalDocLength += docLength;
        countDocs++;
        docInfo.put(docNo, max_tf + "," + uniqueWordsSize + "," + docLength);

        if(!stemming){

            for (Map.Entry<String, Document> entry : tableofTerms.entrySet()) {
                String key = entry.getKey();
                Document value = entry.getValue();
                String upperCase = key.toUpperCase();
                postings.add(value.toString());
                insertTermToDicionary(key, value.getTfTermFrequency(), docNo);
            }
        }

        else {

            for (Map.Entry<String, Document> entry : tableofTerms.entrySet()) {
                String key = entry.getKey();
                Document value = entry.getValue();
                String upperCase = key.toUpperCase();
                postings.add(value.toString());
                insertTermToDicionaryInStemming(key, value.getTfTermFrequency(), docNo);
            }

        }

        if(countDocs == 7380 || (index == 64 && countDocs == 3429)) {

            Collections.sort(postings, String.CASE_INSENSITIVE_ORDER);
            writeToFile(index);
            postings = new ArrayList<>();
            index++;
            countDocs = 0;
        }

        if (index == 65){

            entities = new HashMap<>();
            mergeFiles();
            splitToFiles();
            WriteTheDictionaryToFile(sortDictionary());
            avgDocLength = totalDocLength / totalIndexDocs;
            writeCorpusInfo();
            writeEntitiesInDoc();
            return;
        }
    }

    private void insertTermToDicionaryInStemming(String key, int termFrequency, String docNo) {

        String toUpperCaseTerm = key.toUpperCase();
        String toLowerCaseTerm = key.toLowerCase();

        if(key.split(" ").length > 1) {  //it's an entity

            //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

            if (dictionary.containsKey(key)) { //already in dictionary

                //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

                dictionary.get(key).increaseNumOfTotalDocs(); //increase total docs in 1
                dictionary.get(key).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency

            }

            else if (!entities.containsKey(key)) { //not in entities

                entities.put(key, termFrequency);
                if(!entitiesInDoc.containsKey(docNo)) {

                    entitiesInDoc.put(docNo, new ArrayList<>());
                }

                entitiesInDoc.get(docNo).add(new Pair<>(key, termFrequency));
            }

            else { //entity must be in more than 2 documents

                dictionary.put(key, new Term(key, termFrequency + entities.get(key), "postingStemming." + toUpperCaseTerm.charAt(0) + ".txt"));
                dictionary.get(key).increaseNumOfTotalDocs();
                entities.remove(key);
            }

        }

        else if (ifTermIsLetters(key)) { //it's a letters term

            if (key.equals(toLowerCaseTerm)) { //lower case term

                if (!dictionary.containsKey(key)) { //not exist

                    dictionary.put(key, new Term(key, termFrequency, "postingStemming." + toUpperCaseTerm.charAt(0) + ".txt"));
                    //dictionary.put(key, new Pair(termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                }

                else { //already exist

                    //dictionary.get(key).setLastShowInDoc(docNo); //set new last doc
                    //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

                    dictionary.get(key).increaseNumOfTotalDocs(); //increase total docs in 1
                    dictionary.get(key).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency
                }

                if (dictionary.containsKey(toUpperCaseTerm)) { // insert to the exist lower case and remove toUpperCase term

                    Term upperCaseForm = dictionary.get(toUpperCaseTerm);

                    dictionary.get(key).increaseTotalFrequencyInCorpus(upperCaseForm.getTotalFrequencyInCorpus()); //increase termFrequency in the amount of exist upperCaseForm
                    dictionary.get(key).increaseNumOfTotalDocs(upperCaseForm.getNumOfTotalDocs()); //increase total docs in the amount of exist upperCaseForm

                    // dictionary.put(key, new Pair(dictionary.get(key).getKey() + dictionary.get(toUpperCaseTerm).getKey(),
                    //       "posting." + toUpperCaseTerm.charAt(0) + ".txt")); //add term frequency of UpperCase to LowerCase

                    dictionary.remove(toUpperCaseTerm); //remove Upper case Term
                }

            }

            else { //upper case term

                if (dictionary.containsKey(toLowerCaseTerm)) { //dictionary contains LoweCase so for sure won't contains UpperCase

                    // dictionary.put(toLowerCaseTerm, new Pair(dictionary.get(toLowerCaseTerm).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt")); //add 1 to term frequency of LowerCase

                    dictionary.get(toLowerCaseTerm).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency in the amount of exist upperCaseForm
                    dictionary.get(toLowerCaseTerm).increaseNumOfTotalDocs(); //increase total docs in 1. for sure can't be "one" and "ONE" on same doc!!!
                }

                else if (dictionary.containsKey(toUpperCaseTerm)) { //already contains UpperCase

                    //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                    //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

                    dictionary.get(key).increaseNumOfTotalDocs(); //increase total docs in 1
                    dictionary.get(key).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency
                }

                else {

                    dictionary.put(key, new Term(key, termFrequency, "postingStemming." + toUpperCaseTerm.charAt(0) + ".txt"));
                    // dictionary.put(key, new Pair(termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                }
            }
        }

        else { //key is a number or sign

            if (!dictionary.containsKey(key)) {

                dictionary.put(key, new Term(key, termFrequency, "postingStemming.numbers.txt"));
                //dictionary.put(key, new Pair(termFrequency, "posting.numbers.txt"));
            }

            else {

                //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting.numbers.txt"));

                dictionary.get(key).increaseNumOfTotalDocs(); //increase total docs in 1
                dictionary.get(key).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency
            }
        }
    }

    private void writeEntitiesInDoc() {

        StringBuilder line;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\writeEntitiesInDocs.txt"));
            for (Map.Entry<String, ArrayList<Pair<String,Integer>>> entry : entitiesInDoc.entrySet()) {

                line = new StringBuilder(entry.getKey());

                for(Pair<String,Integer> entityInDoc : entry.getValue()){

                    line.append(",").append(entityInDoc);
                }

                writer.write(line.append("\n").toString());
            }

            writer.close();
        }

        catch (Exception e){

        }
    }

    private TreeMap<String, Term> sortDictionary() {

        TreeMap<String, Term> sortedDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, Term> entry : dictionary.entrySet()) {

            sortedDictionary.put(entry.getKey(), entry.getValue());
            //dictionary.remove(entry.getKey());
        }

        return sortedDictionary;
    }

    private void writeCorpusInfo() {

        try {

            BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\corpusInfo.txt"));
            writer.write(totalIndexDocs + "," + avgDocLength);
            writer.close();
        }

        catch (Exception e){

        }
    }

    private boolean ifTermIsLetters(String term){

        char firstLetter = term.charAt(0);

        if(firstLetter >= 65 && firstLetter <= 90){

            return true;
        }

        if(firstLetter >= 97 && firstLetter <= 122){

            return true;
        }

        return false;
    }

    private void insertTermToDicionary(String key, int termFrequency, String docNo) {

        String toUpperCaseTerm = key.toUpperCase();
        String toLowerCaseTerm = key.toLowerCase();

        if(key.split(" ").length > 1) {  //it's an entity

            //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

            if (dictionary.containsKey(key)) { //already in dictionary

                //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

                dictionary.get(key).increaseNumOfTotalDocs(); //increase total docs in 1
                dictionary.get(key).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency

            }

            else if (!entities.containsKey(key)) { //not in entities

                entities.put(key, termFrequency);
                if(!entitiesInDoc.containsKey(docNo)) {

                    entitiesInDoc.put(docNo, new ArrayList<>());
                }

                entitiesInDoc.get(docNo).add(new Pair<>(key, termFrequency));
            }

            else { //entity must be in more than 2 documents

                //dictionary.put(key, new Pair(entities.get(key) + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

                dictionary.put(key, new Term(key, termFrequency + entities.get(key), "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                dictionary.get(key).increaseNumOfTotalDocs();
                entities.remove(key);
            }

        }

        else if (ifTermIsLetters(key)) { //it's a letters term

            if (key.equals(toLowerCaseTerm)) { //lower case term

                if (!dictionary.containsKey(key)) { //not exist

                    dictionary.put(key, new Term(key, termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                    //dictionary.put(key, new Pair(termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                }

                else { //already exist

                    //dictionary.get(key).setLastShowInDoc(docNo); //set new last doc
                    //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

                    dictionary.get(key).increaseNumOfTotalDocs(); //increase total docs in 1
                    dictionary.get(key).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency
                }

                if (dictionary.containsKey(toUpperCaseTerm)) { // insert to the exist lower case and remove toUpperCase term

                    Term upperCaseForm = dictionary.get(toUpperCaseTerm);

                    dictionary.get(key).increaseTotalFrequencyInCorpus(upperCaseForm.getTotalFrequencyInCorpus()); //increase termFrequency in the amount of exist upperCaseForm
                    dictionary.get(key).increaseNumOfTotalDocs(upperCaseForm.getNumOfTotalDocs()); //increase total docs in the amount of exist upperCaseForm

                    // dictionary.put(key, new Pair(dictionary.get(key).getKey() + dictionary.get(toUpperCaseTerm).getKey(),
                    //       "posting." + toUpperCaseTerm.charAt(0) + ".txt")); //add term frequency of UpperCase to LowerCase

                    dictionary.remove(toUpperCaseTerm); //remove Upper case Term
                }

            }

            else { //upper case term

                if (dictionary.containsKey(toLowerCaseTerm)) { //dictionary contains LoweCase so for sure won't contains UpperCase

                    // dictionary.put(toLowerCaseTerm, new Pair(dictionary.get(toLowerCaseTerm).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt")); //add 1 to term frequency of LowerCase

                    dictionary.get(toLowerCaseTerm).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency in the amount of exist upperCaseForm
                    dictionary.get(toLowerCaseTerm).increaseNumOfTotalDocs(); //increase total docs in 1. for sure can't be "one" and "ONE" on same doc!!!
                }

                else if (dictionary.containsKey(toUpperCaseTerm)) { //already contains UpperCase

                    //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                    //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));

                    dictionary.get(key).increaseNumOfTotalDocs(); //increase total docs in 1
                    dictionary.get(key).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency
                }

                else {

                    dictionary.put(key, new Term(key, termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                    // dictionary.put(key, new Pair(termFrequency, "posting." + toUpperCaseTerm.charAt(0) + ".txt"));
                }
            }
        }

        else { //key is a number or sign

            if (!dictionary.containsKey(key)) {

                dictionary.put(key, new Term(key, termFrequency, "posting.numbers.txt"));
                //dictionary.put(key, new Pair(termFrequency, "posting.numbers.txt"));
            }

            else {

                //dictionary.put(key, new Pair(dictionary.get(key).getKey() + termFrequency, "posting.numbers.txt"));

                dictionary.get(key).increaseNumOfTotalDocs(); //increase total docs in 1
                dictionary.get(key).increaseTotalFrequencyInCorpus(termFrequency); //increase termFrequency
            }
        }
    }

    //writing posting table into one file each time
    private void writeToFile(int index) {

        //write to files
        try {

            BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\" + index + ".txt", true));

            for (String element : postings) {

                writer.append(element + "\n");

            }
            writer.close();
        }

        catch (Exception e) {

        }

        if(index == 64){

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\docInformation.txt", true));

                for(Map.Entry<String, String> entry : docInfo.entrySet()) {

                    writer.append(entry.getKey() + "," + entry.getValue() + "\n");
                }

                writer.close();
            }
            catch (Exception e) {

            }

            docInfo = new HashMap<>();
        }
    }


    public void WriteTheDictionaryToFile(TreeMap<String, Term> sortedDictionary){

        try{
            FileOutputStream  fos = new FileOutputStream(path+"\\Dictionary.txt");
            //ObjectOutputStream oos = new ObjectOutputStream(fos);

            for(Map.Entry<String, Term> entry : sortedDictionary.entrySet()) {
                String key = entry.getKey();
                //Term = entry.getValue();
                //int tf = (int)pair.getKey();
                //oos.writeObject(key+" "+pair.getKey()+" "+pair.getValue()+"/n");
                String line = key+"," + entry.getValue().toString() +"\n";
                byte[] strToBytes = line.getBytes();
                fos.write(strToBytes);
                // do what you have to do here
                // In your case, another loop.
            }

            fos.close();
        }
        catch (Exception e) {
        }
    }

    public void mergeFinalFile(String inputFile){
        //// otputfile write to stemming posting files according to function getStemming
        String outputFile="";
        if(getStemming()) {

            outputFile = path + "/postingStemming.";
        }
        else{
            outputFile = path + "/posting.";

        }
        String currentTerm;
        int count = 0;
        String[] arrayOfTerms;
        String currentLine;
        String finalTerm;

        try {
            FileReader fileReader = new FileReader(path+ "/" + inputFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            FileWriter fileWriter = new FileWriter(outputFile);
            PrintWriter out = new PrintWriter(fileWriter);
            finalTerm = bufferedReader.readLine();
            arrayOfTerms = finalTerm.split(" ");
            currentTerm = arrayOfTerms[0];

            while ((currentLine = bufferedReader.readLine()) != null) {

                arrayOfTerms = currentLine.split(" ");

                //new term
                if(!currentTerm.equals(arrayOfTerms[0])){

                    out.println(finalTerm);
                    count++;
                    finalTerm = currentLine;
                    currentTerm = arrayOfTerms[0];
                }

                //same term repeated
                else {

                    finalTerm = finalTerm + " " + arrayOfTerms[1] + " " + arrayOfTerms[2];
                }
            }

            out.println(finalTerm);
            count++;
            out.flush();
            out.close();
            fileWriter.close();
        }

        catch (Exception e){
        }

    }

    public void mergeTwoFiles(String file1, String file2, String fileToWriteMergeFiles) {

        try {
            FileReader fileReader1 = new FileReader(file1);
            FileReader fileReader2 = new FileReader(file2);
            BufferedReader bufferedReader1 = new BufferedReader(fileReader1);
            BufferedReader bufferedReader2 = new BufferedReader(fileReader2);

            String inputLine1;
            List<String> lineList1 = new ArrayList<String>();
            while ((inputLine1 = bufferedReader1.readLine()) != null) {
                lineList1.add(inputLine1);
            }

            fileReader1.close();

            List<String> lineList2 = new ArrayList<String>();
            while ((inputLine1 = bufferedReader2.readLine()) != null) {
                lineList2.add(inputLine1);
            }

            fileReader2.close();

            List<String> combinedList = Stream.of(lineList1, lineList2)
                    .flatMap(x -> x.stream())
                    .collect(Collectors.toList());

            FileWriter fileWriter = new FileWriter(fileToWriteMergeFiles);
            PrintWriter out = new PrintWriter(fileWriter);

            for (String outputLine : combinedList) {
                out.println(outputLine);
            }
            out.flush();
            out.close();
            fileWriter.close();
            fileReader1.close();
            fileReader2.close();
        }

        catch (Exception e){

        }

        File fileOne = new File(file1);
        File fileTwo = new File(file2);
        fileOne.delete();
        fileTwo.delete();
    }

    public void mergeFiles() {

        mergeSortFiles(path + "\\1.txt", path + "\\2.txt", path + "\\1Merge2.txt");
        mergeSortFiles(path + "\\3.txt", path + "\\4.txt", path + "\\3Merge4.txt");
        mergeSortFiles(path + "\\5.txt", path + "\\6.txt", path + "\\5Merge6.txt");
        mergeSortFiles(path + "\\7.txt", path + "\\8.txt", path + "\\7Merge8.txt");
        mergeSortFiles(path + "\\9.txt", path + "\\10.txt", path + "\\9Merge10.txt");
        mergeSortFiles(path + "\\11.txt", path + "\\12.txt", path + "\\11Merge12.txt");
        mergeSortFiles(path + "\\13.txt", path + "\\14.txt", path + "\\13Merge14.txt");
        mergeSortFiles(path + "\\15.txt", path + "\\16.txt", path + "\\15Merge16.txt");
        mergeSortFiles(path + "\\17.txt", path + "\\18.txt", path + "\\17Merge18.txt");
        mergeSortFiles(path + "\\19.txt", path + "\\20.txt", path + "\\19Merge20.txt");
        mergeSortFiles(path + "\\21.txt", path + "\\22.txt", path + "\\21Merge22.txt");
        mergeSortFiles(path + "\\23.txt", path + "\\24.txt", path + "\\23Merge24.txt");
        mergeSortFiles(path + "\\25.txt", path + "\\26.txt", path + "\\25Merge26.txt");
        mergeSortFiles(path + "\\27.txt", path + "\\28.txt", path + "\\27Merge28.txt");
        mergeSortFiles(path + "\\29.txt", path + "\\30.txt", path + "\\29Merge30.txt");
        mergeSortFiles(path + "\\31.txt", path + "\\32.txt", path + "\\31Merge32.txt");
        mergeSortFiles(path + "\\33.txt", path + "\\34.txt", path + "\\33Merge34.txt");
        mergeSortFiles(path + "\\35.txt", path + "\\36.txt", path + "\\35Merge36.txt");
        mergeSortFiles(path + "\\37.txt", path + "\\38.txt", path + "\\37Merge38.txt");
        mergeSortFiles(path + "\\39.txt", path + "\\40.txt", path + "\\39Merge40.txt");
        mergeSortFiles(path + "\\41.txt", path + "\\42.txt", path + "\\41Merge42.txt");
        mergeSortFiles(path + "\\43.txt", path + "\\44.txt", path + "\\43Merge44.txt");
        mergeSortFiles(path + "\\45.txt", path + "\\46.txt", path + "\\45Merge46.txt");
        mergeSortFiles(path + "\\47.txt", path + "\\48.txt", path + "\\47Merge48.txt");
        mergeSortFiles(path + "\\49.txt", path + "\\50.txt", path + "\\49Merge50.txt");
        mergeSortFiles(path + "\\51.txt", path + "\\52.txt", path + "\\51Merge52.txt");
        mergeSortFiles(path + "\\53.txt", path + "\\54.txt", path + "\\53Merge54.txt");
        mergeSortFiles(path + "\\55.txt", path + "\\56.txt", path + "\\55Merge56.txt");
        mergeSortFiles(path + "\\57.txt", path + "\\58.txt", path + "\\57Merge58.txt");
        mergeSortFiles(path + "\\59.txt", path + "\\60.txt", path + "\\59Merge60.txt");
        mergeSortFiles(path + "\\61.txt", path + "\\62.txt", path + "\\61Merge62.txt");
        mergeSortFiles(path + "\\63.txt", path + "\\64.txt", path + "\\63Merge64.txt");


        mergeSortFiles(path + "\\1Merge2.txt", path + "\\3Merge4.txt", path + "\\1-4.txt");
        mergeSortFiles(path + "\\5Merge6.txt", path + "\\7Merge8.txt", path + "\\5-8.txt");
        mergeSortFiles(path + "\\9Merge10.txt", path + "\\11Merge12.txt", path + "\\9-12.txt");
        mergeSortFiles(path + "\\13Merge14.txt", path + "\\15Merge16.txt", path + "\\13-16.txt");
        mergeSortFiles(path + "\\17Merge18.txt", path + "\\19Merge20.txt", path + "\\17-20.txt");
        mergeSortFiles(path + "\\21Merge22.txt", path + "\\23Merge24.txt", path + "\\21-24.txt");
        mergeSortFiles(path + "\\25Merge26.txt", path + "\\27Merge28.txt", path + "\\25-28.txt");
        mergeSortFiles(path + "\\29Merge30.txt", path + "\\31Merge32.txt", path + "\\29-33.txt");
        mergeSortFiles(path + "\\33Merge34.txt", path + "\\35Merge36.txt", path + "\\33-36.txt");
        mergeSortFiles(path + "\\37Merge38.txt", path + "\\39Merge40.txt", path + "\\37-40.txt");
        mergeSortFiles(path + "\\41Merge42.txt", path + "\\43Merge44.txt", path + "\\41-44.txt");
        mergeSortFiles(path + "\\45Merge46.txt", path + "\\47Merge48.txt", path + "\\45-48.txt");
        mergeSortFiles(path + "\\49Merge50.txt", path + "\\51Merge52.txt", path + "\\49-52.txt");
        mergeSortFiles(path + "\\53Merge54.txt", path + "\\55Merge56.txt", path + "\\53-56.txt");
        mergeSortFiles(path + "\\57Merge58.txt", path + "\\59Merge60.txt", path + "\\57-60.txt");
        mergeSortFiles(path + "\\61Merge62.txt", path + "\\63Merge64.txt", path + "\\61-64.txt");


        mergeSortFiles(path + "\\1-4.txt", path + "\\5-8.txt", path + "\\1-8.txt");
        mergeSortFiles(path + "\\9-12.txt", path + "\\13-16.txt", path + "\\9-16.txt");
        mergeSortFiles(path + "\\17-20.txt", path + "\\21-24.txt", path + "\\17-24.txt");
        mergeSortFiles(path + "\\25-28.txt", path + "\\29-33.txt", path + "\\25-32.txt");
        mergeSortFiles(path + "\\33-36.txt", path + "\\37-40.txt", path + "\\33-40.txt");
        mergeSortFiles(path + "\\41-44.txt", path + "\\45-48.txt", path + "\\41-48.txt");
        mergeSortFiles(path + "\\49-52.txt", path + "\\53-56.txt", path + "\\49-56.txt");
        mergeSortFiles(path + "\\57-60.txt", path + "\\61-64.txt", path + "\\57-64.txt");

        mergeSortFiles(path + "\\1-8.txt", path + "\\9-16.txt", path + "\\1-16.txt");
        mergeSortFiles(path + "\\17-24.txt", path + "\\25-32.txt", path + "\\17-32.txt");
        mergeSortFiles(path + "\\33-40.txt", path + "\\41-48.txt", path + "\\33-48.txt");
        mergeSortFiles(path + "\\49-56.txt", path + "\\57-64.txt", path + "\\49-64.txt");

        mergeSortFiles(path + "\\1-16.txt", path + "\\17-32.txt", path + "\\1-32.txt");
        mergeSortFiles(path + "\\33-48.txt", path + "\\49-64.txt", path + "\\33-64.txt");

        mergeSortFiles(path + "\\1-32.txt", path + "\\33-64.txt", path + "\\1-64.txt");
    }

    public void splitToFiles() {

        if (!getStemming()) {
            try {

                BufferedReader reader = new BufferedReader(new FileReader(path + "\\1-64.txt"));
                String readline="";
                readline=reader.readLine();
                char firstChar = readline.toUpperCase().charAt(0);
                if (!(firstChar >= 65 && firstChar <= 90)) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\posting.numbers.txt", true));

                    while (!(firstChar >= 65 && firstChar <= 90)) {//is a number

                        writer.append(readline + "\n");
                        readline=reader.readLine();
                        firstChar = readline.toUpperCase().charAt(0);
                    }
                    writer.close(); //finish numbers and signs
                }


                BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\posting." + firstChar + ".txt", true));


                while (readline != null) {


                    //if (firstChar == prevchar || prevchar == '\0') {
                    //BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\posting." + firstChar + ".txt", true));
                    writer.append(readline + "\n");
                    readline = reader.readLine();

                    if(readline!=null){
                        char nextChar = readline.toUpperCase().charAt(0);
                        if (nextChar != firstChar) {
                            writer.close();
                            writer = new BufferedWriter(new FileWriter(path + "/posting." + nextChar + ".txt", true));
                        }

                        firstChar = nextChar;
                    }
                    else
                        break;;

                }
                reader.close();
                writer.close();
            } catch (Exception e) {

            }

        } else {//\\ stemming


            try {

                BufferedReader reader = new BufferedReader(new FileReader(path + "\\1-64.txt"));
                String readline="";
                readline=reader.readLine();
                char firstChar = readline.toUpperCase().charAt(0);
                if (!(firstChar >= 65 && firstChar <= 90)) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\postingStemming.numbers.txt", true));

                    while (!(firstChar >= 65 && firstChar <= 90)) {///is a number

                        writer.append(readline + "\n");
                        readline=reader.readLine();
                        firstChar = readline.toUpperCase().charAt(0);
                    }
                    writer.close(); //finish numbers and signs
                }


                BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\postingStemming." + firstChar + ".txt", true));


                while (readline != null) {


                    //if (firstChar == prevchar || prevchar == '\0') {
                    //BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\posting." + firstChar + ".txt", true));
                    writer.append(readline + "\n");
                    readline = reader.readLine();

                    if(readline!=null){
                        char nextChar = readline.toUpperCase().charAt(0);
                        if (nextChar != firstChar) {
                            writer.close();
                            writer = new BufferedWriter(new FileWriter(path + "\\postingStemming." + nextChar + ".txt", true));
                        }

                        firstChar = nextChar;
                    }
                    else
                        break;;

                }
                reader.close();
                writer.close();
            } catch (Exception e) {

            }
        }
    }



    public void mergeSortFiles(String file1, String file2, String out){

        String[] arrayOfTerms1;
        String[] arrayOfTerms2;

        String currentLine1="";
        String currentLine2="";
        String currentTerm1 = "";
        String currentTerm2 = "";
        String toLowerCaseTerm1 = "";
        String toLowerCaseTerm2 = "";

        try {

            FileReader fileReader1 = new FileReader(file1);
            FileReader fileReader2 = new FileReader(file2);
            BufferedReader bufferedReader1 = new BufferedReader(fileReader1);
            BufferedReader bufferedReader2 = new BufferedReader(fileReader2);
            BufferedWriter writer = new BufferedWriter(new FileWriter(out));

            //reading term from file1
            currentLine1 = bufferedReader1.readLine();
            if (currentLine1 != null) {

                arrayOfTerms1 = currentLine1.split(",");
                currentTerm1 = arrayOfTerms1[0];

                if (!dictionary.containsKey(currentTerm1)) {

                    while (currentLine1 != null) {

                        toLowerCaseTerm1 = currentTerm1.toLowerCase();

                        if (dictionary.containsKey(toLowerCaseTerm1)) {

                            //currentTerm1 = toLowerCaseTerm1;
                            currentLine1 = currentLine1.replace(currentTerm1, toLowerCaseTerm1);
                            currentTerm1 = toLowerCaseTerm1;
                            break;
                        }

                        currentLine1 = bufferedReader1.readLine();

                        if (currentLine1 != null) {

                            arrayOfTerms1 = currentLine1.split(",");
                            currentTerm1 = arrayOfTerms1[0];

                            if (dictionary.containsKey(currentTerm1)) {

                                break;
                            }
                        }
                    }
                }
            }


            //reading term from file2
            currentLine2 = bufferedReader2.readLine();
            if (currentLine2 != null) {

                arrayOfTerms2 = currentLine2.split(",");
                currentTerm2 = arrayOfTerms2[0];

                if (!dictionary.containsKey(currentTerm2)) {

                    while (currentLine2 != null) {

                        toLowerCaseTerm2 = currentTerm2.toLowerCase();

                        if (dictionary.containsKey(toLowerCaseTerm2)) {

                            currentLine2 = currentLine2.replace(currentTerm2, toLowerCaseTerm2);
                            currentTerm2 = toLowerCaseTerm2;
                            //currentTerm2 = toLowerCaseTerm2;
                            break;
                        }

                        currentLine2 = bufferedReader2.readLine();

                        if (currentLine2 != null) {

                            arrayOfTerms2 = currentLine2.split(",");
                            currentTerm2 = arrayOfTerms2[0];

                            if (dictionary.containsKey(currentTerm2)) {

                                break;
                            }
                        }
                    }
                }
            }



            while (currentLine1 != null && currentLine2  != null ) {


                if (currentTerm1.compareToIgnoreCase(currentTerm2) < 0) {

                    writer.write(currentLine1 + "\n");

                    //reading term from file1
                    currentLine1 = bufferedReader1.readLine();
                    if (currentLine1 != null) {

                        arrayOfTerms1 = currentLine1.split(",");
                        currentTerm1 = arrayOfTerms1[0];

                        if (!dictionary.containsKey(currentTerm1)) {

                            while (currentLine1 != null) {

                                toLowerCaseTerm1 = currentTerm1.toLowerCase();

                                if (dictionary.containsKey(toLowerCaseTerm1)) {

                                    //currentTerm1 = toLowerCaseTerm1;
                                    currentLine1 = currentLine1.replace(currentTerm1, toLowerCaseTerm1);
                                    currentTerm1 = toLowerCaseTerm1;
                                    break;
                                }

                                currentLine1 = bufferedReader1.readLine();

                                if (currentLine1 != null) {

                                    arrayOfTerms1 = currentLine1.split(",");
                                    currentTerm1 = arrayOfTerms1[0];

                                    if (dictionary.containsKey(currentTerm1)) {

                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else if (currentTerm1.compareToIgnoreCase(currentTerm2) > 0) {

                    writer.write(currentLine2 + "\n");


                    //reading term from file2
                    currentLine2 = bufferedReader2.readLine();
                    if (currentLine2 != null) {

                        arrayOfTerms2 = currentLine2.split(",");
                        currentTerm2 = arrayOfTerms2[0];

                        if (!dictionary.containsKey(currentTerm2)) {

                            while (currentLine2 != null) {

                                toLowerCaseTerm2 = currentTerm2.toLowerCase();

                                if (dictionary.containsKey(toLowerCaseTerm2)) {

                                    currentLine2 = currentLine2.replace(currentTerm2, toLowerCaseTerm2);
                                    currentTerm2 = toLowerCaseTerm2;
                                    //currentTerm2 = toLowerCaseTerm2;
                                    break;
                                }

                                currentLine2 = bufferedReader2.readLine();

                                if (currentLine2 != null) {

                                    arrayOfTerms2 = currentLine2.split(",");
                                    currentTerm2 = arrayOfTerms2[0];

                                    if (dictionary.containsKey(currentTerm2)) {

                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else { //equal terms

                    writer.write(currentLine1 + "\n");
                    writer.write(currentLine2 + "\n");


                    //reading term from file1
                    currentLine1 = bufferedReader1.readLine();
                    if (currentLine1 != null) {

                        arrayOfTerms1 = currentLine1.split(",");
                        currentTerm1 = arrayOfTerms1[0];

                        if (!dictionary.containsKey(currentTerm1)) {

                            while (currentLine1 != null) {

                                toLowerCaseTerm1 = currentTerm1.toLowerCase();

                                if (dictionary.containsKey(toLowerCaseTerm1)) {

                                    //currentTerm1 = toLowerCaseTerm1;
                                    currentLine1 = currentLine1.replace(currentTerm1, toLowerCaseTerm1);
                                    currentTerm1 = toLowerCaseTerm1;
                                    break;
                                }

                                currentLine1 = bufferedReader1.readLine();

                                if (currentLine1 != null) {

                                    arrayOfTerms1 = currentLine1.split(",");
                                    currentTerm1 = arrayOfTerms1[0];

                                    if (dictionary.containsKey(currentTerm1)) {

                                        break;
                                    }
                                }
                            }
                        }
                    }


                    //reading term from file2
                    currentLine2 = bufferedReader2.readLine();
                    if (currentLine2 != null) {

                        arrayOfTerms2 = currentLine2.split(",");
                        currentTerm2 = arrayOfTerms2[0];

                        if (!dictionary.containsKey(currentTerm2)) {

                            while (currentLine2 != null) {

                                toLowerCaseTerm2 = currentTerm2.toLowerCase();

                                if (dictionary.containsKey(toLowerCaseTerm2)) {

                                    currentLine2 = currentLine2.replace(currentTerm2, toLowerCaseTerm2);
                                    currentTerm2 = toLowerCaseTerm2;
                                    break;
                                }

                                currentLine2 = bufferedReader2.readLine();

                                if (currentLine2 != null) {

                                    arrayOfTerms2 = currentLine2.split(",");
                                    currentTerm2 = arrayOfTerms2[0];

                                    if (dictionary.containsKey(currentTerm2)) {

                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }


            while (currentLine1 != null) {

                writer.write(currentLine1 + "\n");

                while (currentLine1 != null) {

                    //reading term from file1
                    currentLine1 = bufferedReader1.readLine();

                    if(currentLine1 != null) {

                        arrayOfTerms1 = currentLine1.split(",");
                        currentTerm1 = arrayOfTerms1[0];

                        if (dictionary.containsKey(currentTerm1)) {

                            break;
                        }

                        toLowerCaseTerm1 = currentTerm1.toLowerCase();

                        if (dictionary.containsKey(toLowerCaseTerm1)) {

                            //currentTerm1 = toLowerCaseTerm1;
                            currentLine1 = currentLine1.replace(currentTerm1, toLowerCaseTerm1);
                            currentTerm1 = toLowerCaseTerm1;
                            break;
                        }
                    }
                }
            }


            while (currentLine2 != null) {

                writer.write(currentLine2 + "\n");

                while (currentLine2 != null) {

                    //reading term from file2
                    currentLine2 = bufferedReader2.readLine();

                    if(currentLine2 != null) {

                        arrayOfTerms2 = currentLine2.split(",");
                        currentTerm2 = arrayOfTerms2[0];

                        if (dictionary.containsKey(currentTerm1)) {

                            break;
                        }

                        toLowerCaseTerm2 = currentTerm2.toLowerCase();

                        if (dictionary.containsKey(toLowerCaseTerm2)) {

                            //currentTerm1 = toLowerCaseTerm1;
                            currentLine2 = currentLine2.replace(currentTerm2, toLowerCaseTerm2);
                            currentTerm2 = toLowerCaseTerm2;
                            break;
                        }
                    }
                }
            }

            fileReader1.close();
            fileReader2.close();
            bufferedReader1.close();
            bufferedReader2.close();
            writer.close();
            File fileOne = new File(file1);
            File fileTwo = new File(file2);
            fileOne.delete();
            fileTwo.delete();
        }
        catch (Exception e){

        }
    }

    public void setStemming(Boolean flag) {

        this.stemming = flag;
    }
}