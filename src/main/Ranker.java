package main;
import com.medallia.word2vec.Word2VecModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Ranker {

    private String postingFilesPath;
    private HashMap<String, Term> dictionary;
    private double avgDocLength;
    private double NumOfDocsInCorpus;
    private double k1;
    private double b;
    private int numOfReleventDocs;
    private HashMap<String, String> docInfo;
    private boolean ifSemanti;
    private  Word2VecModel model;
    private com.medallia.word2vec.Searcher searcher;


    public Ranker(String postingFilesPath) {

        //read dictionary and docInfo
        this.postingFilesPath = postingFilesPath;
        loadDictionary();
        loadDocInfo();

        //read the value of avgDocLength and NumOfDocsInCorpus from "corpusInfo.txt"
        loadCorpusInfo();
        this.NumOfDocsInCorpus = NumOfDocsInCorpus;
        this.avgDocLength = avgDocLength;
        k1 = 1.2;
        b = 0.75;
        ifSemanti = false;

        try {

            model = Word2VecModel.fromTextFile(new File(postingFilesPath+"\\word2vec.c.output.model.txt"));
            searcher = model.forSearch();
        }

        catch (Exception e){

        }

        System.out.println("finish build ranker");
    }

    private void loadCorpusInfo() {

        try {

            FileReader fr = new FileReader(postingFilesPath + "/corpusInfo.txt");
            BufferedReader bufferedReader = new BufferedReader(fr);
            String inputLine;
            inputLine = bufferedReader.readLine();
            String[] infoPiece = inputLine.split(",");
            NumOfDocsInCorpus = Double.parseDouble(infoPiece[0]);
            avgDocLength = Double.parseDouble(infoPiece[1]);
            bufferedReader.close();
            fr.close();
        }

        catch (Exception e) {

        }
    }

    public int getNumOfReleventDocs() {
        return numOfReleventDocs;
    }

    public boolean isIfSemanti() {
        return ifSemanti;
    }

    public void setIfSemanti(boolean ifSemanti) {
        this.ifSemanti = ifSemanti;
    }



    public LinkedHashMap<String, Double> rank(HashMap<String, Integer> queriesTerms) {

        if(!ifSemanti){

            return rankNotSemantic(queriesTerms);

        }

        else{

            return rankSemantic(queriesTerms);
        }
    }

    private LinkedHashMap<String, Double> rankSemantic(HashMap<String, Integer> queriesTerms) {
        LinkedHashMap<String, Double> retrievalDocs = new LinkedHashMap<>();
        List<com.medallia.word2vec.Searcher.Match> matches;
        int numOfResult = 3;

        String docNo = "";
        double termFrequency; //from term posting files
        double docLength; //from docInformation.txt
        double numberOfDocWhichTermIsShown; //from dictionary
        double IDF;
        double formulaResult;
        double queryFrequency;
        int countReleventDocs = 0;
        String toUpperCaseTerm;
        String toLowerCaseTerm;

        boolean ifFound = false;
        String[] arrayForPosting;
        String pointer;
        String term = "";
        String line;




        for (Map.Entry<String, Integer> entry : queriesTerms.entrySet()) {

            term = entry.getKey();

            toUpperCaseTerm = term.toUpperCase();
            toLowerCaseTerm = term.toLowerCase();
            if(dictionary.containsKey(toLowerCaseTerm)){
                term = toLowerCaseTerm;
            }

            else if(dictionary.containsKey(toUpperCaseTerm)){

                term = toUpperCaseTerm;
            }

            matches = null;

            try {

                matches = searcher.getMatches(term, numOfResult);
            }

            catch (com.medallia.word2vec.Searcher.UnknownWordException e){
                //term not known to model
            }

            if(matches == null){ //calculating only the original form

                if(dictionary.containsKey(term)){

                    queryFrequency = entry.getValue();
                    numberOfDocWhichTermIsShown = dictionary.get(term).getNumOfTotalDocs();
                    System.out.println("in term: " + term + " num of docs in dicitionary is: " + numberOfDocWhichTermIsShown);
                    pointer = dictionary.get(term).getPostingFilrPointer();

                    try {
                        FileReader fileReader1 = new FileReader(postingFilesPath + "/" + pointer);
                        BufferedReader termPosting = new BufferedReader(fileReader1);

                        while ((line = termPosting.readLine()) != null) {

                            arrayForPosting = line.split(",");

                            if (arrayForPosting[0].equals(term)) { //found the line that term is located in posting file

                                countReleventDocs++;

                                docNo = arrayForPosting[1];
                                termFrequency = Double.parseDouble(arrayForPosting[2]);

                                //docInfo.put(docNo, max_tf + "," + uniqueWordsSize + "," + docLength);
                                docLength = Integer.parseInt(docInfo.get(docNo).split(",")[2]);

                                //IDF = Math.log(NumOfDocsInCorpus - documentFrequency + 0.5) / (documentFrequency + 0.5);
                                IDF = Math.log((NumOfDocsInCorpus + 1) / (numberOfDocWhichTermIsShown));

                                formulaResult = queryFrequency * IDF * (((termFrequency) * (k1 + 1))
                                        / ((termFrequency) + k1 * (1 - b + b * (docLength / avgDocLength))));

                                if (retrievalDocs.containsKey(docNo)) {

                                    retrievalDocs.put(docNo, retrievalDocs.get(docNo) + formulaResult);
                                }

                                else {

                                    retrievalDocs.put(docNo, formulaResult);
                                }

                            }

                            else if (countReleventDocs >= numberOfDocWhichTermIsShown) {

                                countReleventDocs = 0;
                                break;
                            }
                        }

                        fileReader1.close();
                        termPosting.close();
                    }

                    catch (Exception e) {
                        System.out.println(term);
                    }
                }
            }

            else{

                for (com.medallia.word2vec.Searcher.Match match: matches){

                    term = match.match();

                    toUpperCaseTerm = term.toUpperCase();
                    toLowerCaseTerm = term.toLowerCase();

                    if(dictionary.containsKey(toLowerCaseTerm)){
                        term = toLowerCaseTerm;
                    }

                    else if(dictionary.containsKey(toUpperCaseTerm)){

                        term = toUpperCaseTerm;
                    }

                    else {
                        continue;
                    }

                    if(dictionary.containsKey(term)){

                        queryFrequency = entry.getValue();
                        numberOfDocWhichTermIsShown = dictionary.get(term).getNumOfTotalDocs();
                        System.out.println("in term: " + term + " num of docs in dicitionary is: " + numberOfDocWhichTermIsShown);
                        pointer = dictionary.get(term).getPostingFilrPointer();

                        try {
                            FileReader fileReader1 = new FileReader(postingFilesPath + "/" + pointer);
                            BufferedReader termPosting = new BufferedReader(fileReader1);

                            while ((line = termPosting.readLine()) != null) {

                                arrayForPosting = line.split(",");

                                if (arrayForPosting[0].equals(term)) { //found the line that term is located in posting file

                                    countReleventDocs++;

                                    docNo = arrayForPosting[1];
                                    termFrequency = Double.parseDouble(arrayForPosting[2]);

                                    //docInfo.put(docNo, max_tf + "," + uniqueWordsSize + "," + docLength);
                                    docLength = Integer.parseInt(docInfo.get(docNo).split(",")[2]);

                                    //IDF = Math.log(NumOfDocsInCorpus - documentFrequency + 0.5) / (documentFrequency + 0.5);
                                    IDF = Math.log((NumOfDocsInCorpus + 1) / (numberOfDocWhichTermIsShown));

                                    formulaResult = queryFrequency * IDF * (((termFrequency) * (k1 + 1))
                                            / ((termFrequency) + k1 * (1 - b + b * (docLength / avgDocLength))));

                                    if (retrievalDocs.containsKey(docNo)) {

                                        retrievalDocs.put(docNo, retrievalDocs.get(docNo) + formulaResult);
                                    }

                                    else {

                                        retrievalDocs.put(docNo, formulaResult);
                                    }

                                }

                                else if (countReleventDocs >= numberOfDocWhichTermIsShown) {

                                    countReleventDocs = 0;
                                    break;
                                }
                            }

                            fileReader1.close();
                            termPosting.close();
                        }

                        catch (Exception e) {
                            System.out.println(term);
                        }
                    }
                }
            }
        }

        orderByValue(retrievalDocs, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                if (o1.compareTo(o2) == 0) {
                    return 0;
                }
                if (o1.compareTo(o2) == 1) {

                    return -1;
                }

                return 1;

            }
        });

        numOfReleventDocs = retrievalDocs.size();
        return retrievalDocs;

    }

    private LinkedHashMap<String, Double> rankNotSemantic(HashMap<String, Integer> queriesTerms) {

        LinkedHashMap<String, Double> retrievalDocs = new LinkedHashMap<>();

        String docNo = "";
        double termFrequency; //from term posting files
        double docLength; //from docInformation.txt
        double numberOfDocWhichTermIsShown; //from dictionary
        double IDF;
        double formulaResult;
        double queryFrequency;
        int countReleventDocs = 0;

        boolean ifFound = false;
        String[] arrayForPosting;
        String pointer;
        String term = "";
        String line;


        for (Map.Entry<String, Integer> entry : queriesTerms.entrySet()) {

            term = entry.getKey();
            String toUpperCaseTerm = term.toUpperCase();
            String toLowerCaseTerm = term.toLowerCase();
            if(dictionary.containsKey(toLowerCaseTerm)){
                term = toLowerCaseTerm;
            }

            else if(dictionary.containsKey(toUpperCaseTerm)){

                term = toUpperCaseTerm;
            }

            else {

                continue;
            }

            queryFrequency = entry.getValue();
            numberOfDocWhichTermIsShown = dictionary.get(term).getNumOfTotalDocs();
            // System.out.println("in term: " + term + "num of docs in dicitionary is: " + numberOfDocWhichTermIsShown);
            pointer = dictionary.get(term).getPostingFilrPointer();

            try {
                FileReader fileReader1 = new FileReader(postingFilesPath + "/" + pointer);
                BufferedReader termPosting = new BufferedReader(fileReader1);

                while ((line = termPosting.readLine()) != null) {

                    arrayForPosting = line.split(",");

                    if (arrayForPosting[0].equals(term)) { //found the line that term is located in posting file

                        countReleventDocs++;

                        docNo = arrayForPosting[1];
                        termFrequency = Double.parseDouble(arrayForPosting[2]);

                        //docInfo.put(docNo, max_tf + "," + uniqueWordsSize + "," + docLength);
                        docLength = Integer.parseInt(docInfo.get(docNo).split(",")[2]);

                        //IDF = Math.log(NumOfDocsInCorpus - documentFrequency + 0.5) / (documentFrequency + 0.5);
                        IDF = Math.log((NumOfDocsInCorpus + 1) / (numberOfDocWhichTermIsShown));

                        formulaResult = queryFrequency * IDF * (((termFrequency) * (k1 + 1))
                                / ((termFrequency) + k1 * (1 - b + b * (docLength / avgDocLength))));

                        if (retrievalDocs.containsKey(docNo)) {

                            retrievalDocs.put(docNo, retrievalDocs.get(docNo) + formulaResult);
                        }

                        else {

                            retrievalDocs.put(docNo, formulaResult);
                        }

                    }

                    else if (countReleventDocs >= numberOfDocWhichTermIsShown) {

                        //ifFound = false;
                        //System.out.println("in term:" + term + " num of docs is: " + countReleventDocs);
                        //System.out.println("last doc: " + docNo );
                        countReleventDocs = 0;
                        break;
                    }
                }

                // System.out.println(countReleventDocs);
                //countReleventDocs = 0;
                fileReader1.close();
                termPosting.close();
            }

            catch (Exception e) {
                System.out.println(term);
            }
        }



        orderByValue(retrievalDocs, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                if (o1.compareTo(o2) == 0) {
                    return 0;
                }
                if (o1.compareTo(o2) == 1) {

                    return -1;
                }

                return 1;

            }
        });

        numOfReleventDocs = retrievalDocs.size();
        return retrievalDocs;
    }

    static <K, V> void orderByValue(
            LinkedHashMap<K, V> m, Comparator<? super V> c) {
        List<Map.Entry<K, V>> entries = new ArrayList<>(m.entrySet());
        m.clear();
        entries.stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, c))
                .forEachOrdered(e -> m.put(e.getKey(), e.getValue()));
    }

    //docInfo.put(docNo, max_tf + "," + uniqueWordsSize + "," + docLength);
    // writer.append(entry.getKey() + "," + entry.getValue() + "\n");
    public void loadDocInfo() {

        docInfo = new HashMap<>();

        try {

            FileReader fr = new FileReader(postingFilesPath + "/docInformation.txt");
            BufferedReader bufferedReader = new BufferedReader(fr);
            String inputLine;

            //System.out.println(s.substring(s.indexOf(",") + 1));

            while ((inputLine = bufferedReader.readLine()) != null) {

                String docNo = inputLine.substring(0, inputLine.indexOf(","));
                String info = inputLine.substring(inputLine.indexOf(",") + 1);
                docInfo.put(docNo, info);
            }
            bufferedReader.close();
            fr.close();
        }

        catch (Exception e) {

        }
    }

    public void loadDictionary() {

        dictionary = new HashMap<>();

        try {

            FileReader fr = new FileReader(postingFilesPath + "/Dictionary.txt");
            BufferedReader bufferedReader = new BufferedReader(fr);
            String inputLine;

            while ((inputLine = bufferedReader.readLine()) != null) {

                String[] infoPiece = inputLine.split(",");
                Term term = new Term(infoPiece[0], Integer.parseInt(infoPiece[3]), infoPiece[1], Integer.parseInt(infoPiece[2]));
                dictionary.put(term.getName(), term);
                //need to consider option of stemming
            }
            bufferedReader.close();
            fr.close();
        }

        catch (Exception e) {

        }
    }



    public LinkedHashMap<String, Integer> get5Entities(String value) {

        //int[] numOfShowInDoc = new int[];
        LinkedHashMap<String,Integer>rankEntities = new LinkedHashMap<>();
        LinkedHashMap<String,Integer>top5Entities = new LinkedHashMap<>();
        try {
            FileReader fr = new FileReader(postingFilesPath + "/writeEntitiesInDocs.txt");
            BufferedReader bufferedReader = new BufferedReader(fr);
            String inputLine;
            boolean isFound =false;

            while ((inputLine = bufferedReader.readLine()) != null ) {
                if (inputLine.contains(value)) {
                    String[] entities = inputLine.split(",");
                    for (int i = 1; i < entities.length; i++) {
                        rankEntities.put(entities[i].split("=")[0], Integer.parseInt(entities[i].split("=")[1]));
                    }

                    break;

                }
            }
            bufferedReader.close();
            fr.close();
            orderByValue(rankEntities, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    if (o1.compareTo(o2) == 0) {
                        return 0;
                    }
                    if (o1.compareTo(o2) == 1) {

                        return -1;
                    }

                    return 1;
                }
            });
            int index = 0;
            if (rankEntities.size() < 5) {
                for (Map.Entry<String, Integer> map : rankEntities.entrySet()) {
                    top5Entities.put(map.getKey(), map.getValue());

                }

            } else {
                for (Map.Entry<String, Integer> map : rankEntities.entrySet()) {

                    if (index != 5) {
                        top5Entities.put(map.getKey(), map.getValue());
                        index++;
                    } else {
                        break;
                    }

                }

            }
        }
        catch (Exception e){}
        return top5Entities;

    }
    public HashMap<String, Term> getDictionary(){
        return dictionary;
    }
}
