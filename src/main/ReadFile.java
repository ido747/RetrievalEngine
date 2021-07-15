package main;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.*;

public class ReadFile {

    private String corpusPath;
    private int startFile;
    private int endFile;
    private Parser parser;
    private int docId;
    private String docNo;
    private String filePath;


    public ReadFile(String corpusPath, String postingPath, String stopWordsPath) {

        this.filePath = filePath;
        this.corpusPath = corpusPath;
        this.startFile = startFile;
        this.endFile = endFile;
        parser = new Parser(new DateForParser(), postingPath, createStopWordsTable(stopWordsPath));

    }

    public Parser getParser() {
        return parser;
    }

    public int getDocId() {
        return docId;
    }
    public String getDocNo(){return docNo;}

    public void Read() {

        //LinkedList<String> linkedList = new LinkedList<>();
        int sum=0;
        File fatherDir = new File(corpusPath + "/corpus");
        File[] files = fatherDir.listFiles();
        // int count = 0;
        //int x = 0;
        //int serialnum = 1;
        for (int i = 0; i < files.length; i++) {//size of files number/4
            System.out.println(i);
            //x++;
            if (!(files[i].isFile())) {
                String newPath = corpusPath + "/corpus/"  + files[i].getName();
                File dir2 = new File(newPath);
                File[] contentFiles = dir2.listFiles();
                try (FileReader reader = new FileReader(contentFiles[0]);
                     BufferedReader br = new BufferedReader(reader)) {
                    String data = br.lines().collect(Collectors.joining());// it work not sure why
                    //System.out.println(data);

                    String[] arrToSplit = data.split("</DOC>");
                    //int number = arrToSplit

                    for(int J = 0; J < arrToSplit.length; J++){


                        int temp = J;
                        int id = docId;
                        ////ido add
                        int indexStartdocNo = arrToSplit[J].indexOf("<DOCNO>");
                        int indexLastdocNo = arrToSplit[J].indexOf("</DOCNO>");
                        docNo= arrToSplit[J].substring(indexStartdocNo+7,indexLastdocNo);
                        docNo = docNo.replaceAll(" ","");
                        ////

                        if(arrToSplit[J].contains("<TEXT>")){

                            int indexStart = arrToSplit[J].indexOf("<TEXT>");
                            int indexLast = arrToSplit[J].indexOf("</TEXT>");
                            arrToSplit[J] = arrToSplit[J].substring(indexStart + 6, indexLast);
                            System.out.println("**************** finish Reading doc: " + docId+ " *******************");
                            parser.parse(arrToSplit[temp], docId,docNo);

                            docId++;
                        }

                    }

                } catch (IOException e) {
                    System.err.format("IOException: %s%n", e);
                }
            }
        }
    }

    //creating hash set which contains all the stop words
    public HashSet<String> createStopWordsTable(String stopWordsFilePath){

        HashSet<String> stopWordsTable = new HashSet<>();

        String word;

        try {

            BufferedReader bufferedReader = new BufferedReader(new FileReader(stopWordsFilePath));

            while ((word = bufferedReader.readLine()) != null) {

                stopWordsTable.add(word);
            }

            bufferedReader.close();
        }

        catch (IOException e){

            e.printStackTrace();
        }

        return stopWordsTable;
    }
}












