package main;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Searcher {
    private Ranker ranker; //maybe ranker have searcher
    private Parser parser;
    private boolean identitiesRecognize;
    private ArrayList<String> listOfDocs;
    private HashMap<String, Pair<String, Integer>> identities;
    private String pathOfQueriesFile;

    public Searcher(Parser parser, String path) {
        this.parser = parser;
        pathOfQueriesFile = path;
        ranker = new Ranker(path);
    }

    public Ranker getRanker() {
        return ranker;
    }


    public ArrayList<String> runOneQuery(String query) {
        ArrayList<String> result = new ArrayList<>();
        HashMap<String, Integer> queryAfterParse = parser.parseQuery(query);
        result = getFirst50(ranker.rank(queryAfterParse));


        try {

            BufferedWriter writer = new BufferedWriter(new FileWriter(pathOfQueriesFile + "\\resultFrom1Query.txt"));//// need to add the path of the folder

            for (int i = 0; i < result.size(); i++) {
                writer.write("111 0 " + result.get(i) + " 1 42.38 iAm \n"); // the other parameters are for trec eval

            }
            writer.close();

        } catch (Exception e) {
        }
        return result;

    }


    public ArrayList<Pair<String, String>> runMoreThanOneQuery(String path) {
        File file = new File(path);
        String[] queries = new String[15];
        String[] queriesID = new String[15];
        ArrayList<Pair<String, String>> resultFromAllQueries = new ArrayList<>();
        BufferedWriter writer;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            int countQuery = 0;

            String line = br.readLine();
            int indexQuery = 0;
            int indexQueryNum = 0;


            while (countQuery != 15) {
                if (line != null && line.length() > 0) {
                    if (line.contains("<num>")) {
                        queriesID[indexQueryNum] = line.substring(14);
                        indexQueryNum++;

                    } else if (line.contains("<title>")) {
                        queries[indexQuery] = line.substring(8);
                        //indexQuery++;
                    } else if (line.contains("<desc>")) {
                        while (!line.contains("<narr>")) {
                            line = br.readLine();
                            if (!line.contains("<narr>")) {
                                queries[indexQuery] = queries[indexQuery] + " " + line;
                            }
                        }
                        indexQuery++;

                    } else if (line.contains("</top>")) {
                        countQuery++;
                    }
                }
                line = br.readLine();
            }
            br.close();
        } catch (Exception e) {
        }


        try {
            // String folderPath = path.substring(0, path.indexOf("queries.txt")-1);
            writer = new BufferedWriter(new FileWriter(pathOfQueriesFile + "\\results.txt"));

            for (int j = 0; j < queries.length; j++) {
                ArrayList<String> queryResult = new ArrayList<>();

                HashMap<String, Integer> queryAfterParse = parser.parseQuery(queries[j]);
                long startTime = System.nanoTime();
                queryResult = getFirst50(ranker.rank(queryAfterParse));
                long duration = System.nanoTime() - startTime;
                System.out.println("query:" + j + " duration is: " + duration);

                for (int i = 0; i < queryResult.size(); i++) {
                    writer.write(queriesID[j] + " 0 " + queryResult.get(i) + " 1 42.38 iAm \n");// the other parameters are for trec eval
                    resultFromAllQueries.add(new Pair(queryResult.get(i), queriesID[j]));

                }


            }
            writer.close();
        } catch (IOException e) {
        }
        return resultFromAllQueries;

    }

    public ArrayList<String> getFirst50(LinkedHashMap<String, Double> queryresult) {

        ArrayList<String> result = new ArrayList<>();
        for (Map.Entry<String, Double> map : queryresult.entrySet()) {
            if (result.size() != 50) {
                result.add(map.getKey());
            } else {
                break;
            }

        }
        return result;
    }
}
