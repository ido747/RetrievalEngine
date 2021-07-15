package main;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {

    private boolean stemming;
    private HashSet<String> stopWords;
    private DateForParser date;
    private HashMap<String, Document> terms;
    private Indexer indexer;
    private String folderPath;
    private int countExceptions; // deleteAfter
    private boolean ifTokenIsNumber;
    private String docNo;
    private Stemmer stm;


    public Parser(DateForParser dateForParser, String folderPath, HashSet<String> stopWords) {

        ifTokenIsNumber = false;
        this.folderPath = folderPath;
        stemming = false;
        this.stopWords = stopWords;
        this.date = dateForParser;
        terms = new HashMap<>();
        indexer = new Indexer(folderPath, stemming); //ido change this
        stm = new Stemmer();

    }

    public void setStemming(Boolean flag) {
        this.stemming = flag;
        indexer.setStemming(flag);
    }

    public boolean getStemming() {
        return this.stemming;
    }

    public HashMap<String, Document> parse(String doc, int docID, String docNo) throws IOException {

        this.docNo = docNo;
        HashMap<String, Document> terms = new HashMap<>();
        long duration;
        int maxTermFrequency = 0;
        int i;
        int index;
        long startTime = System.nanoTime();
        String sToken;
        String term = "";
        index = 0;

        ArrayList<String> tokens = new ArrayList<>();

        for (String token : doc.split("[ \n]")) {

            if (token.equals("") || token.charAt(0) == '<' || token.contains("_")) {
                continue;
            }

            token = token.replace("...", "");
            token = token.replaceAll("[`,()*&'#@;:>!+?}{|~�]", ""); //� if its good or making problems
            token = token.replace("\"", "");
            token = token.replace("--", "-");
            token = token.replaceAll("[\\[\\]]", "");
            int sizeToken = token.length();
            if (token.length() == 0) {
                continue;
            }

            char lastChar = token.charAt(sizeToken - 1);
            while (token.length() > 0 && //remove '-' and '.' only from the end
                    (lastChar == '.' || lastChar == '-' || lastChar == '/' || lastChar == '=')) {

                token = token.substring(0, sizeToken - 1);
                sizeToken--;
                if (sizeToken > 0) {
                    lastChar = token.charAt(sizeToken - 1);
                }
            }

            if (token.length() > 0) {

                char firstChar = token.charAt(0);

                while (token.length() > 0 && //remove '-' and '.' only from the end
                        (firstChar == '.' || firstChar == '/' || firstChar == '=')) {

                    token = token.substring(1);
                    sizeToken--;
                    if (sizeToken > 0) {
                        firstChar = token.charAt(0);
                    }
                }

            }

            if (token.length() > 1) {

                char charAtOne = token.charAt(1);

                if (token.charAt(0) == '-') {

                    if (charAtOne == '-') {
                        token = token.replace("-", "");
                    } else if (charAtOne >= 65 && charAtOne <= 90) {

                        token = token.substring(1);
                    } else if (charAtOne >= 97 && charAtOne <= 122) {

                        token = token.substring(1);
                    }

                }
            }

            if (token.length() != 0) {
                tokens.add(token);
            }
        }

        //iterate all tokens in document which optional to be term
        for (i = 0; i < tokens.size(); i++) {

            sToken = tokens.get(i); //get current token
            try {

                if (sToken.length() == 0 || (!sToken.equals("between") && !sToken.equals("Between") && ifStopWord(sToken, stopWords))) {
                    index++;
                    continue;
                }


                //check if token is a number
                else if ((!sToken.contains("-") && sToken.length() > 1 && sToken.charAt(0) == '$' && isNumeric(sToken.substring(1))) ||
                        (!sToken.contains("E") && !sToken.contains("e") && isNumeric(sToken))) {

                    ifTokenIsNumber = true;


                    //check if token is weight units
                    if ((tokens.size() > i + 1) && (tokens.get(i + 1).equals("Kilogram") ||
                            tokens.get(i + 1).equals("kilogram") || (tokens.get(i + 1).equals("Milligram") ||
                            tokens.get(i + 1).equals("milligram")) || (tokens.get(i + 1).equals("Gram") ||
                            tokens.get(i + 1).equals("gram")))) {

                        addTerm(term = gramParse(sToken + " " + tokens.get(i + 1)), index, terms, docID);
                        index += 2;
                        i++;

                    }

                    //token is price type
                    else if (sToken.charAt(0) == '$') {

                        //                        startTime = System.nanoTime();
                        if ((tokens.size() > i + 1) && (tokens.get(i + 1).equals("million") ||
                                tokens.get(i + 1).equals("billion"))) {

                            addTerm(term = PriceParse(sToken + " " + tokens.get(i + 1)), index, terms, docID);
                            index += 2;
                            i++;

                        } else {

                            addTerm(term = PriceParse(sToken), index, terms, docID);
                            index++;
                        }

                    } else if ((tokens.size() > i + 1) && (tokens.get(i + 1).equals("Dollars") ||
                            tokens.get(i + 1).equals("dollars"))) {

                        addTerm(term = PriceParse(sToken + " " + tokens.get(i + 1)), index, terms, docID);
                        index += 2;
                        i++;

                    } else if ((tokens.size() > i + 2) && (tokens.get(i + 1).equals("m") ||
                            tokens.get(i + 1).equals("bn")) && (tokens.get(i + 2).equals("Dollars") ||
                            tokens.get(i + 2).equals("dollars"))) {

                        addTerm(term = PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2)), index, terms, docID);
                        index += 3;
                        i = i + 2;

                        //for example: price trillion U.S dollars
                    } else if ((tokens.size() > i + 3) && (!isNumeric(tokens.get(i + 1))) && (tokens.get(i + 2).equals("U.S")) && (tokens.get(i + 3).equals("Dollars") ||
                            tokens.get(i + 3).equals("dollars"))) {

                        addTerm(term = PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2) + " "
                                + tokens.get(i + 3)), index, terms, docID);
                        index += 4;
                        i = i + 3;

                    }//end of price type


                    //token is percentage type
                    else if (sToken.charAt(sToken.length() - 1) == '%') {

                        //                        startTime = System.nanoTime();
                        addTerm(term = PercentParse(sToken), index, terms, docID);
                        index++;
                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("percentage parse time is: " + duration);

                    } else if ((tokens.size() > i + 1) && (tokens.get(i + 1).equals("percent") ||
                            tokens.get(i + 1).equals("percentage"))) {

                        //                        startTime = System.nanoTime();
                        addTerm(term = PercentParse(sToken + " " + tokens.get(i + 1)), index, terms, docID);
                        index += 2;
                        i++;
                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("percentage parse time is: " + duration);
                    }//end of percentage type


                    //token is Date type
                    else if ((tokens.size() > i + 1) && (ifDate((String) tokens.get(i + 1))) &&
                            ((int) Double.parseDouble(sToken) == Double.parseDouble(sToken))) {

                        //                        startTime = System.nanoTime();
                        //int temp = (int)Double.parseDouble(sToken);
                        addTerm(term = DateParse(sToken + " " + tokens.get(i + 1)), index, terms, docID);
                        index += 2;
                        i++;
                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("Date parse time is: " + duration);
                    }//end of Date type


                    else {//regular number without units

                        if ((tokens.size() > i + 1) && ((tokens.get(i + 1)).equals("Thousand") ||
                                tokens.get(i + 1).equals("Million") ||
                                tokens.get(i + 1).equals("Billion"))) {

                            //                            startTime = System.nanoTime();
                            addTerm(term = NumeericParse(sToken + " " + tokens.get(i + 1)), index, terms, docID);
                            index += 2;
                            i++;
                            //                            duration = System.nanoTime() - startTime;
                            //                            System.out.println("NumeericParse parse time is: " + duration);
                        } else {

                            //                            startTime = System.nanoTime();
                            addTerm(term = NumeericParse(sToken), index, terms, docID);
                            index++;
                            //                            duration = System.nanoTime() - startTime;
                            //                            System.out.println("NumeericParse parse time is: " + duration);
                        }

                    }//end of regular number without units

                }//end of numeric type


                if (!ifTokenIsNumber) {//token is a word

                    //token is Date type
                    if (ifDate(sToken) && tokens.size() > i + 1 && isNumeric((String) tokens.get(i + 1)) &&
                            (int) Double.parseDouble((String) tokens.get(i + 1)) == Double.parseDouble((String) tokens.get(i + 1))) {

                        //                        startTime = System.nanoTime();
                        //int temp = (int)Double.parseDouble(tokens.get(i + 1));
                        addTerm(term = DateParse(sToken + " " + tokens.get(i + 1)), index, terms, docID);
                        index += 2;
                        i++;
                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("Date parse time is: " + duration);
                    }//end of date type


                    //token is price type

                    else if ((!((sToken.charAt(0) >= 65 && sToken.charAt(0) <= 90) || (sToken.charAt(0) >= 97 && sToken.charAt(0) <= 122))) &&
                            tokens.size() > i + 1 && sToken.length() >= 2 &&
                            (sToken.charAt(sToken.length() - 1) == 'm' || sToken.substring(sToken.length() - 2).equals("bn")) &&
                            (tokens.get(i + 1).equals("Dollars") || tokens.get(i + 1).equals("dollars"))) {

                        //                        startTime = System.nanoTime();
                        addTerm(term = PriceParse(sToken + " " + tokens.get(i + 1)), index, terms, docID);
                        index += 2;
                        i++;
                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("price parse time is: " + duration);
                        //System.out.println(sToken + " " + tokens.get(i + 1) + " " + i);

                    } else if ((tokens.size() > i + 2) && (tokens.get(i + 1).equals("m") ||
                            tokens.get(i + 1).equals("bn")) && (tokens.get(i + 2).equals("Dollars") ||
                            tokens.get(i + 2).equals("dollars"))) {

                        //                        startTime = System.nanoTime();
                        addTerm(term = PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2)), index, terms, docID);
                        //System.out.println(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2) + " " + i);
                        index += 3;
                        i = i + 2;
                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("PriceParse time is: " + duration);

                    }


                    //token is an email
                    else if (sToken.contains("@") && (sToken.contains(".com") || sToken.contains(".COM"))) {

                        //                        startTime = System.nanoTime();
                        addTerm(term = mailParse(sToken), index, terms, docID);
                        index++;
                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("email parse time is: " + duration);
                    }//end of email type


                    //token is expression or a range
                    else if (sToken.contains("-") || sToken.equals("Between") || sToken.equals("between")) {

                        if (sToken.contains("-")) {

                            //                            startTime = System.nanoTime();
                            addTerm(term = sToken, index, terms, docID);

                            String[] expressions = sToken.split("-");

                            for (int j = 0; j < expressions.length; j++) {

                                if (!ifStopWord(expressions[j], stopWords)) {

                                    i++;
                                    addTerm(term = expressions[j], index, terms, docID);
                                    index++;
                                }
                            }
                            //                            duration = System.nanoTime() - startTime;
                            //                            System.out.println("expression or a range parse time is: " + duration);
                        } else if ((tokens.size() > i + 3) && (sToken.equals("Between") || sToken.equals("between")) &&
                                (isNumeric((String) tokens.get(i + 1)) && tokens.get(i + 2).equals("and") &&
                                        isNumeric((String) tokens.get(i + 3)))) {

                            //                            startTime = System.nanoTime();
                            int M = i;
                            addTerm(term = tokens.get(M + 1) + "-" + tokens.get(M + 3), index + 1, terms, docID);
                            if (terms.get(term) != null && terms.get(term).getTfTermFrequency() > maxTermFrequency) {

                                maxTermFrequency = terms.get(term).getTfTermFrequency();
                            }

                            i++;
                            addTerm(term = tokens.get(M + 1), index + 1, terms, docID);
                            if (terms.get(term) != null && terms.get(term).getTfTermFrequency() > maxTermFrequency) {

                                maxTermFrequency = terms.get(term).getTfTermFrequency();
                            }
                            i++;

                            addTerm(term = tokens.get(M + 3), index + 3, terms, docID);
                            if (terms.get(term) != null && terms.get(term).getTfTermFrequency() > maxTermFrequency) {

                                maxTermFrequency = terms.get(term).getTfTermFrequency();
                            }
                            i++;

                            index += 4;

                        }
                    }


                    //start with Capital letter
                    else if (sToken.charAt(0) >= 65 && sToken.charAt(0) <= 90) {

                        if ((tokens.size() == i + 1) || ((tokens.size() > i + 1) && !((tokens.get(i + 1)).charAt(0) >= 65 && (tokens.get(i + 1)).charAt(0) <= 90))) {

                            if (!ifStopWord(sToken.toLowerCase(), stopWords)) {

                                term = this.addTerm(sToken, index, terms, docID);
                                index++;

                            }

                        } else {

                            if (!ifStopWord(sToken.toLowerCase(), stopWords)) {

                                term = this.addTerm(sToken, index, terms, docID);

                                if (terms.get(term) != null && terms.get(term).getTfTermFrequency() > maxTermFrequency) {

                                    maxTermFrequency = terms.get(term).getTfTermFrequency();
                                }
                            }

                            //String entity = sToken + " " + tokens.get(i + 1);
                            //i = i + 2;
                            //int temp = index;
                            //index += 2;


                            String entity = tokens.get(i).toUpperCase();
                            i++;
                            index++;
                            int counter = 1;
                            //sToken = tokens.get(i);

                            while (counter < 5 && tokens.size() > i && (tokens.get(i)).charAt(0) >= 65 && (tokens.get(i)).charAt(0) <= 90) {

                                sToken = tokens.get(i);

                                if (!ifStopWord(sToken.toLowerCase(), stopWords)) {

                                    term = this.addTerm(sToken, index, terms, docID);
                                    //tempIForSingleTerms++;

                                    if (terms.get(term) != null && terms.get(term).getTfTermFrequency() > maxTermFrequency) {

                                        maxTermFrequency = terms.get(term).getTfTermFrequency();
                                    }
                                }

                                entity = entity + " " + sToken.toUpperCase();
                                i++;
                                index++;
                                counter++;
                            }

                            i--;

                            term = this.addTerm(entity, index, terms, docID);
                        }
                    } else {

                        addTerm(term = sToken, index, terms, docID);
                        index++;

                    }
                }//end of word option


                ifTokenIsNumber = false;
            } catch (IndexOutOfBoundsException e) {
                System.out.println("doc is: " + doc);
                countExceptions++;
                System.out.println("tokens before: " + tokens.get(i - 12) + " " + tokens.get(i - 11) + tokens.get(i - 10) + " " + tokens.get(i - 9)
                        + tokens.get(i - 8) + " " + tokens.get(i - 7) + tokens.get(i - 6) + " " + tokens.get(i - 5) +
                        " " + tokens.get(i - 4) + " " + tokens.get(i - 3) + tokens.get(i - 2) + " " + tokens.get(i - 1));
                System.out.println(e + "the problem is in: " + sToken + " index is: " + index);

                System.out.println("this is: " + countExceptions + "exception");

            } catch (NumberFormatException e) {
//                System.out.println("doc is: " + doc);
//                countExceptions++;
//                System.out.println("tokens before: " + tokens.get(i - 12) + " " + tokens.get(i - 11) + tokens.get(i - 10) + " " + tokens.get(i - 9)
//                        + tokens.get(i - 8) + " " + tokens.get(i - 7) + tokens.get(i - 6) + " " + tokens.get(i - 5) +
//                        " " + tokens.get(i - 4) + " " + tokens.get(i - 3) + tokens.get(i - 2) + " " + tokens.get(i - 1));
//                System.out.println(e + "the problem is in: " + sToken + " index is: " + index);
//                System.out.println("this is: " + countExceptions + "exception");
            }

            if (terms.get(term) != null && terms.get(term).getTfTermFrequency() > maxTermFrequency) {

                maxTermFrequency = terms.get(term).getTfTermFrequency();
            }
        }//end of for loop

        System.out.println("**************** finish parse doc: " + docID + " *******************");
        indexer.IndexFiles(docNo, terms, maxTermFrequency, terms.size(), tokens.size());

        return terms;
    }


    public void addTermToCapitalLettersTable(String term, int index, HashMap<String, Document> terms, int docId) {

        addTerm(term, index, terms, docId);
    }


    public boolean ifDate(String token) {


        if (date.getDate().containsKey(token)) {

            return true;
        }

        return false;
    }

    private String addTerm(String term, int index, HashMap<String, Document> terms, int docId) {


        boolean ifCapital;

        if (!ifTokenIsNumber) {

            if (getStemming() == true) {

                stm.add(term.toCharArray(), term.length());
                stm.stem();
                term = stm.toString();
            }
            String toUpperCaseTerm = term.toUpperCase();
            String toLowerCaseTerm = term.toLowerCase();

            if (term.equals(toLowerCaseTerm)) { //lower case term

                if (!terms.containsKey(term)) { //not exist

                    terms.put(term, new Document(docId, docNo, term, index));
                } else {

                    terms.get(term).setLocation(index);
                    //terms.put(term, terms.get(term).setLocation(index));
                }

                if (terms.containsKey(toUpperCaseTerm)) { // insert to the exist lower case and remove toUpperCase term

                    terms.get(term).setTfTermFrequency(terms.get(term).getTfTermFrequency() + terms.get(toUpperCaseTerm).getTfTermFrequency()); //update term frequency
                    ArrayList<Integer> temp = (ArrayList<Integer>) Stream.of(terms.get(term).getLocations(), terms.get(toUpperCaseTerm).getLocations()) //merge arrayList locations
                            .flatMap(x -> x.stream())
                            .collect(Collectors.toList());
                    Collections.sort(temp); //sort arrayList locations

                    terms.get(term).setLocations(temp);

                    terms.remove(toUpperCaseTerm); //remove Upper case Term
                }

                return term;

            } else { //upper case term

                if (getStemming() == true) {

                    stm.add(toLowerCaseTerm.toCharArray(), toLowerCaseTerm.length());
                    stm.stem();
                    toLowerCaseTerm = stm.toString();
                    toUpperCaseTerm = stm.toString().toUpperCase();
                }
                if (terms.containsKey(toLowerCaseTerm)) {

                    terms.get(toLowerCaseTerm).setLocation(index);

                    return toLowerCaseTerm;

                } else if (terms.containsKey(toUpperCaseTerm)) {

                    terms.get(toUpperCaseTerm).setLocation(index);

                    return toUpperCaseTerm;

                } else {

                    terms.put(toUpperCaseTerm, new Document(docId, docNo, toUpperCaseTerm, index));

                    return toUpperCaseTerm;
                }
            }
        }

        else { //term is a number

            if (getStemming() == true) {

                stm.add(term.toCharArray(), term.length());
                stm.stem();
                term = stm.toString();
            }

            if (!terms.containsKey(term)) { //not exist

                terms.put(term, new Document(docId, docNo, term, index));
            } else {

                terms.get(term).setLocation(index);
                //terms.put(term, terms.get(term).setLocation(index));
            }

            return term;
        }
    }

    public Indexer getIndexer() {
        return indexer;
    }

    //creating hash set which contains all the stop words
    public HashSet<String> createStopWordsTable(String stopWordsFilePath) {

        HashSet<String> stopWordsTable = new HashSet<>();

        String word;

        try {

            BufferedReader bufferedReader = new BufferedReader(new FileReader(stopWordsFilePath));

            while ((word = bufferedReader.readLine()) != null) {

                stopWordsTable.add(word);
            }

            bufferedReader.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

        return stopWordsTable;
    }

    public boolean ifStopWord(String token, HashSet<String> stopWordsTable) {

        if (stopWordsTable.contains(token)) {

            return true;
        } else {

            return false;
        }
    }

    public ArrayList<String> Tokenize(String document, char[] aDelimiters) {

        ArrayList<String> lTokens = new ArrayList<>(); //maybe change linked list

        while (document.length() > 0) {
            String sToken = "";
            //StringBuilder sToken = new StringBuilder();

            int i;

            for (i = 0; i < document.length(); i++) {

                if (Contains(aDelimiters, document.charAt(i))) {

                    //remove ',' '.' '-' from the beginning and start of token if there is any of them
                    sToken = checkAndRemovePeriodCommaHyphen(sToken);

                    if (checkIfToRemoveOrSymbol(sToken, lTokens)) {

                        if (sToken.length() > 0) {

                            lTokens.add(sToken);
                            //System.out.println(i);
                        }
                    }

                    break;

                } else {

                    sToken += document.charAt(i);
                }
            }

            if (i == document.length()) {

                sToken = checkAndRemovePeriodCommaHyphen(sToken);

                if (checkIfToRemoveOrSymbol(sToken, lTokens) && sToken.length() != 0) {

                    lTokens.add(sToken);
                }

                document = "";

            } else
                document = document.substring(i + 1);
        }

        return lTokens;
    }

    //Checks whether char c is in array of chars
    public boolean Contains(char[] a, char c) {
        for (char c1 : a)

            if (c1 == c) {

                return true;
            }

        return false;
    }

    //remove ',' '.' '-','/' from the beginning and start of token if there is any of them
    public String checkAndRemovePeriodCommaHyphen(String sToken) {

        int startIndex = 0;

        //remove ',' '.' '-' from the beginning of token
        while (sToken.length() > 0 && (sToken.charAt(0) == '.' || sToken.charAt(0) == ',' ||
                sToken.charAt(0) == '-' || sToken.charAt(0) == '/')) {

            sToken = sToken.substring(1);
        }

        int lastIndexOfToken = sToken.length() - 1;

        //remove ',' '.' '-' from the end of token
        while (sToken.length() > 0 && (sToken.charAt(lastIndexOfToken) == '.' ||
                sToken.charAt(lastIndexOfToken) == ',' ||
                sToken.charAt(lastIndexOfToken) == '-' ||
                sToken.charAt(lastIndexOfToken) == '/')) {

            sToken = sToken.substring(0, lastIndexOfToken);

            lastIndexOfToken--;
        }

        return sToken;
    }

    //remove '/' from the middle two or more tokens if they are no numbers
    public boolean checkIfToRemoveOrSymbol(String sToken, ArrayList<String> lTokens) {

        String copy = sToken;
        boolean flag = true;

        String[] tokens = null;

        if (copy.contains("/")) {

            tokens = copy.split("\\/");

            for (String element : tokens) {

                if (!isNumeric(element)) {

                    flag = false;

                    break;

                    //return true;
                }
            }

            if (!flag) {

                for (String element : tokens) {

                    if (element.length() != 0) {

                        lTokens.add(element);
                    }

                }
            }
        }

        return flag;
    }


    public String NumeericParse(String str) {

        String number = "";
        double valueDouble = 0;
        int valueInt = 0;
        int div;
        int mod;
        if (str.contains(" ") && str.contains("/")) {
            //System.out.println(str);
            return str;
        }
        if (str.contains("Thousand") || str.contains("thousand") || str.contains("THOUSAND")) {
            String intString = str.replaceAll("[^0-9?!\\.]", "");
            if (intString.isEmpty()) {
                number = "1K";
            } else {
                number = "" + intString + "K";
            }
            //System.out.println(number);
            return number;

        } else if (str.contains("Million") || str.contains("million") || str.contains("MILLION")) {
            String intString = str.replaceAll("[^0-9?!\\.]", "");
            if (intString.isEmpty()) {
                number = "1M";
            } else {
                number = "" + intString + "M";
            }
            //System.out.println(number);
            return number;

        } else if (str.contains("Billion") || str.contains("billion") || str.contains("BILLION")) {
            String intString = str.replaceAll("[^0-9?!\\.]", "");
            if (intString.isEmpty()) {
                number = "1B";
            } else {
                number = "" + intString + "B";
            }
            // System.out.println(number);
            return number;

        } else {

            valueDouble = Double.parseDouble(str);
            if (valueDouble < 1000) {
                // System.out.println(str);
                return str;
            } else if (valueDouble >= 1000 && valueDouble < 1000000) {
                div = (int) (valueDouble / 1000);
                String intString = String.valueOf(div);
                String modString = str.substring(intString.length(), str.length());
                if (modString.charAt(0) == '0' && modString.charAt(1) == '0' && modString.charAt(2) == '0') {
                    number = "" + div + 'K';
                    //System.out.println(number);
                    return number;
                } else if (modString.charAt(1) != '0' && modString.charAt(2) == '0') {
                    number = "" + div + '.' + modString.substring(0, 2) + 'K';
                    //System.out.println(number);
                    return number;
                } else if (modString.charAt(0) != '0' && modString.charAt(1) == '0' && modString.charAt(2) == '0') {
                    number = "" + div + '.' + modString.substring(0, 1) + 'K';
                    //System.out.println(number);
                    return number;
                } else {
                    number = "" + div + '.' + modString.substring(0, 3) + 'K';
                    //System.out.println(number);
                    return number;
                }
            } else if (valueDouble >= 1000000 && valueDouble < 1000000000) {
                div = (int) (valueDouble / 1000000);
                String intString = String.valueOf(div);
                String modString = str.substring(intString.length(), str.length());
                if (modString.charAt(0) == '0' && modString.charAt(1) == '0' && modString.charAt(2) == '0') {
                    number = "" + div + 'M';
                    //System.out.println(number);
                    return number;
                } else if (modString.charAt(1) != '0' && modString.charAt(2) == '0') {
                    number = "" + div + '.' + modString.substring(0, 2) + 'M';
                    //System.out.println(number);
                    return number;
                } else if (modString.charAt(0) != '0' && modString.charAt(1) == '0' && modString.charAt(2) == '0') {
                    number = "" + div + '.' + modString.substring(0, 1) + 'M';
                    // System.out.println(number);
                    return number;
                } else {
                    number = "" + div + '.' + modString.substring(0, 3) + 'M';
                    //System.out.println(number);
                    return number;
                }

            } else if (valueDouble >= 1000000000) {
                div = (int) (valueDouble / 1000000000);
                String intString = String.valueOf(div);
                String modString = str.substring(intString.length(), str.length());
                if (modString.charAt(0) == '0' && modString.charAt(1) == '0' && modString.charAt(2) == '0') {
                    number = "" + div + 'B';
                    //System.out.println(number);
                    return number;
                } else if (modString.charAt(1) != '0' && modString.charAt(2) == '0') {
                    number = "" + div + '.' + modString.substring(0, 2) + 'B';
                    // System.out.println(number);
                    return number;
                } else if (modString.charAt(0) != '0' && modString.charAt(1) == '0' && modString.charAt(2) == '0') {
                    number = "" + div + '.' + modString.substring(0, 1) + 'B';
                    //System.out.println(number);
                    return number;
                } else {
                    number = "" + div + '.' + modString.substring(0, 3) + 'B';
                    //System.out.println(number);
                    return number;
                }

            }


        }
        return number;
    }


    public String PercentParse(String str) {
        String numberPercent = "";
        if (str.contains("%")) {
            //          System.out.println(str);
            return str;
        } else if (str.contains("percent") || str.contains("Percent") || str.contains("PERCENT") || str.contains("Percentage") || str.contains("PERCENTAGE") || str.contains("percentage")) {
            String intString = str.replaceAll("[^0-9?!\\.]", "");
            numberPercent = "" + intString + "%";
            //System.out.println(numberPercent);
            return numberPercent;
        }


        return str;
    }

    public String PriceParse(String str) {

        if (str.contains("/")) {
            //System.out.println(str);
            return str;
        }

        //String lowerSrting = str.toLowerCase();

        else if (str.contains("thousand U.S Dollars") || str.contains("thousand U.S dollars") || str.contains("Thousand U.S Dollars") || str.contains("THOUSAND U.S Dollars")) {
            String newString = str.replaceAll("[^0-9?!\\.]", "");
            //newString =  newString.replace(".","");
            if (newString.charAt(newString.length() - 1) == '.') {
                newString = newString.substring(0, newString.length() - 1);
            }
            str = "" + newString + " K Dollars";
            // System.out.println(str);
            return str;
        } else if (str.contains("million U.S Dollars") || str.contains("million U.S dollars") || str.contains("Million U.S Dollars") || str.contains("Million U.S dollars") || str.contains("MILLION U.S Dollars")) {
            String newString = str.replaceAll("[^0-9?!\\.]", "");
            //newString =  newString.replace(".","");
            if (newString.charAt(newString.length() - 1) == '.') {
                newString = newString.substring(0, newString.length() - 1);
            }
            str = "" + newString + " M Dollars";
            //System.out.println(str);
            return str;
        } else if (str.contains("billion U.S dollars") || str.contains("billion U.S Dollars") || str.contains("Billion U.S Dollars") || str.contains("BILLION U.S Dollars")) {
            String newString = str.replaceAll("[^0-9?!\\.]", "");
            newString = newString.replace(".", "");
            str = "" + newString + "000 M Dollars";
            //System.out.println(str);
            return str;
        } else if (str.contains("trillion U.S dollars") || str.contains("Trillion U.S Dollars") || str.contains("TRILLION U.S Dollars")) {
            String newString = str.replaceAll("[^0-9?!\\.]", "");
            newString = newString.replace(".", "");
            str = "" + newString + "000000 M Dollars";
            //System.out.println(str);
            return str;
        } else if (str.contains("m") && str.contains("Dollars")) {
            String newString = str.replaceAll("[^0-9?!\\.]", "");
            str = "" + newString + " M Dollars";
            //System.out.println(str);
            return str;
        } else if (str.contains("bn") && str.contains("Dollars")) {
            String newString = str.replaceAll("[^0-9?!\\.]", "");
            str = "" + newString + "000 M Dollars";
            //System.out.println(str);
            return str;
        } else if (str.contains("$") && (str.contains("million") || str.contains("Million") || str.contains("MILLION"))) {
            String intStringMil = str.replaceAll("[^0-9?!\\.]", "");
            String millionString = "";
            millionString = "" + intStringMil + " M Dollars";
            //System.out.println(millionString);
            return millionString;
        } else if (str.contains("$") && (str.contains("billion") || str.contains("Billion") || str.contains("BILLION"))) {
            String intStringMil = str.replaceAll("[^0-9?!\\.]", "");
            String millionString = "";
            millionString = "" + intStringMil + "000 M Dollars";
            //System.out.println(millionString);
            return millionString;
        } else if (str.contains("$") && (str.contains("trillion") || str.contains("Trillion") || str.contains("TRILLION"))) {
            String intStringMil = str.replaceAll("[^0-9?!\\.]", "");
            String millionString = "";
            millionString = "" + intStringMil + "000000 M Dollars";
            //System.out.println(millionString);
            return millionString;
        }

        String priceResult = "";
        String intString = str.replaceAll("[^0-9?!\\.]", "");
        if (str.contains(".")) {
            double priceDouble = Double.parseDouble(intString);
            if (priceDouble < 1000000) {
                if (str.contains("$")) {
                    str = changeSign(str);
                } else {
                    // System.out.println(str);
                    return str;
                }
            }
        } else {
            if (intString.length() > 10 && str.contains("$")) {

                str = intString.substring(0, (intString.length() / 2) + 3);
                str = str + " M Dollars";
                // System.out.println(str);
                return str;

            } else if (intString.length() == 10 && str.contains("$")) {
                String newstring = "";
                for (int i = 0; i < 4; i++) {
                    newstring = newstring + intString.charAt(i);
                }
                newstring = newstring + " M Dollars";
                //System.out.println(newstring);
                return newstring;
            }
            //int
            int price = Integer.parseInt(intString);
            if (price < 1000000) {
                if (str.contains("$")) {
                    str = changeSign(str);
                } else {
                    //System.out.println(str);
                    return str;
                }
            } else {//price is more than 1 million

                str = PriceMillionParse(price);
                //System.out.println(str);
                return str;
            }
        }

        return str;
    }
//
//    "aba".concat("yty")

    public String PriceMillionParse(int num) {
        String milionString = "";
        double div = (double) num / 1000000;
        milionString = "" + div + " M Dollars";
        if (milionString.contains(".")) {
            int index = milionString.indexOf(".");
            if (milionString.charAt(index + 1) == '0') {
                milionString = "" + milionString.substring(0, index) + "" + milionString.substring(index + 2, milionString.length());

            }
        }
        return milionString;
    }

    public String DateParse(String str) {

        String[] temp = str.split(" ");
        String month;
        //        char firstLetter = temp[1].charAt(0);
        //        if((firstLetter >=65 && firstLetter <= 90) ||
        //                (firstLetter >= 97 && firstLetter <= 122)){
        //
        //            month = date.get(temp[1]);
        //            return month + "-" + temp[0];
        //        }

        if (!isNumeric(temp[1])) { //14 May = 05-14

            month = date.getDate().get(temp[1]);
            return month + "-" + temp[0];
        } else if (temp[1].length() == 4) { //May 1994 = 1994-05

            month = date.getDate().get(temp[0]);
            return temp[1] + "-" + month;
        } else { //June 4

            month = date.getDate().get(temp[0]);
            return month + "-" + temp[1];
        }
    }


    public String changeSign(String str) {
        String priceResult = "";
        str = str.replace("$", "");
        priceResult = "" + str + " Dollars";
        //System.out.println(priceResult);
        return priceResult;
    }

    public boolean isNumeric(String strNum) {

        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
            //System.out.println(d);
        } catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }

    public String mailParse(String str) {
        String res = "";
        if (str.contains("@") && (str.contains(".COM") || str.contains(".com"))) {
            res = str.toLowerCase();
            System.out.println(res);
            return res;
        }
        return str;


    }

    public String gramParse(String str) {
        String res = "";
        String numString = str.replaceAll("[^0-9?!\\.]", "");
        if (str.contains("Kilogram") || str.contains("kilogram")) {
            res = "" + numString + " kg";
            //System.out.println(res);
            return res;
        }
        if (str.contains("milligram") || str.contains("Milligram")) {
            if (Integer.parseInt(numString) < 1000) {
                res = "" + numString + " mg";
                //System.out.println(res);
                return res;
            } else {
                Double gram = Double.parseDouble(numString) / 1000;
                res = "" + gram + " g";
                //System.out.println(res);
                return res;
            }
        }
        if (str.contains("gram") || str.contains("Gram")) {
            if (Double.parseDouble(numString) < 1000) {
                res = "" + numString + " g";
                //System.out.println(res);
                return res;
            } else {
                Double kilo = Double.parseDouble(numString) / 1000;
                res = "" + kilo + " kg";
            }
        }


        return str;
    }

    public String splitToText(String doc) {

        if (doc.contains("<TEXT>")) {

        }
        String newDoc = "";
        int indexStart = doc.indexOf("<TEXT>");
        int indexLast = doc.indexOf("</TEXT>");
        newDoc = doc.substring(indexStart + 6, indexLast);

        return newDoc;

    }

    public HashMap<String,Integer> parseQuery(String query){
        //String fixQuery="";
        HashMap <String, Integer> words = new HashMap<>();

        ArrayList<String> tokens = new ArrayList<>();

        for (String token : query.split("[ \n]")) {

            if (token.equals("") || token.charAt(0) == '<' || token.contains("_")) {
                continue;
            }

            token = token.replace("...","");
            token = token.replaceAll("[`,()*&'#@;:>!+?}{|~�]", ""); //� if its good or making problems
            token = token.replace("\"", "");
            token = token.replace("--", "-");
            token = token.replaceAll("[\\[\\]]","");
            int sizeToken = token.length();
            if(token.length() == 0){
                continue;
            }

            char lastChar = token.charAt(sizeToken - 1);
            while (token.length() > 0 && //remove '-' and '.' only from the end
                    (lastChar == '.' || lastChar == '-' || lastChar == '/' || lastChar == '=')){

                token = token.substring(0, sizeToken - 1);
                sizeToken--;
                if(sizeToken > 0){
                    lastChar = token.charAt(sizeToken - 1);
                }
            }

            if (token.length() >0){

                char firstChar = token.charAt(0);

                while (token.length() > 0 && //remove '-' and '.' only from the end
                        (firstChar == '.' || firstChar == '/' || firstChar == '=')){

                    token = token.substring(1);
                    sizeToken--;
                    if(sizeToken > 0){
                        firstChar = token.charAt(0);
                    }
                }

            }

            if(token.length() > 1){

                char charAtOne = token.charAt(1);

                if(token.charAt(0) == '-'){

                    if(charAtOne == '-'){
                        token = token.replace("-", "");
                    }
                    else if(charAtOne >= 65 && charAtOne <= 90){

                        token = token.substring(1);
                    }

                    else if(charAtOne >= 97 && charAtOne <= 122){

                        token = token.substring(1);
                    }

                }
            }

            if (token.length() != 0){
                tokens.add(token);
            }
        }

        //iterate all tokens in document which optional to be term
        for(int i = 0; i < tokens.size(); i++){

            String sToken = tokens.get(i); //get current token
            try {

                //if stop word dont add token to terms and continue.. fix!!!!!!!!
                if (sToken.length() == 0 || (!sToken.equals("between") && !sToken.equals("Between") && ifStopWord(sToken, stopWords))) {
                    //index++;
                    continue;
                }


                //check if token is a number
                else if ((!sToken.contains("-") && sToken.length() > 1 && sToken.charAt(0) == '$' && isNumeric(sToken.substring(1))) ||
                        (!sToken.contains("E") && !sToken.contains("e") && isNumeric(sToken))){

                    ifTokenIsNumber = true;



                    //check if token is weight units
                    if((tokens.size() > i + 1) && (tokens.get(i + 1).equals("Kilogram") ||
                            tokens.get(i + 1).equals("kilogram") || (tokens.get(i + 1).equals("Milligram") ||
                            tokens.get(i + 1).equals("milligram"))|| (tokens.get(i + 1).equals("Gram") ||
                            tokens.get(i + 1).equals("gram")))){

                        //                        startTime = System.nanoTime();

                        if(words.containsKey(gramParse(sToken + " " + tokens.get(i + 1)))){
                            words.put(gramParse(sToken + " " + tokens.get(i + 1)),words.get(sToken + " " + tokens.get(i + 1))+1);

                        }
                        else {
                            words.put(gramParse(sToken + " " + tokens.get(i + 1)), 1);
                        }



                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("gram parse time is: " + duration);

                    }

                    //                    startTime = System.nanoTime();
                    //                    duration = System.nanoTime() - startTime;
                    //                    System.out.println("gram parse time is: " + duration);

                    //token is price type
                    else if (sToken.charAt(0) == '$') {

                        //                        startTime = System.nanoTime();
                        if ((tokens.size() > i + 1) && (tokens.get(i + 1).equals("million") ||
                                tokens.get(i + 1).equals("billion"))) {

                            if(words.containsKey(PriceParse(sToken + " " + tokens.get(i + 1)))) {

                                words.put(PriceParse(sToken + " " + tokens.get(i + 1)),words.get(PriceParse(sToken + " " + tokens.get(i + 1)))+1);
                                i++;
                            }
                            else{
                                words.put(PriceParse(sToken + " " + tokens.get(i + 1)),1);
                                i++;
                            }
                            //System.out.println(sToken + " " + tokens.get(i + 1) + " " + i);
                        } else {
                            if(words.containsKey(PriceParse(sToken))) {
                                words.put(PriceParse(sToken),words.get(PriceParse(sToken))+1);
                            }
                            else{
                                words.put(PriceParse(sToken),1);

                            }

                            //System.out.println(sToken + " " + i);
                        }

                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("price parse time is: " + duration);

                    } else if ((tokens.size() > i + 1) && (tokens.get(i + 1).equals("Dollars") ||
                            tokens.get(i + 1).equals("dollars"))) {

                        //                        startTime = System.nanoTime();
                        if(words.containsKey(PriceParse(sToken + " " + tokens.get(i + 1)))) {
                            words.put(PriceParse(sToken + " " + tokens.get(i + 1)),words.get(PriceParse(sToken + " " + tokens.get(i + 1)))+1);
                            i++;
                        }
                        else{
                            words.put(PriceParse(sToken + " " + tokens.get(i + 1)),1);
                            i++;
                        }

                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("price parse time is: " + duration);
                        //System.out.println(sToken + " " + tokens.get(i + 1) + " " + i);

                    } else if ((tokens.size() > i + 2) && (tokens.get(i + 1).equals("m") ||
                            tokens.get(i + 1).equals("bn")) && (tokens.get(i + 2).equals("Dollars") ||
                            tokens.get(i + 2).equals("dollars"))) {

                        //                        startTime = System.nanoTime();
                        if(words.containsKey(PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2)))) {
                            words.put(PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2)),words.get(PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2)))+1);
                            i = i + 2;
                        }
                        else{
                            words.put(PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2)),1);
                            i = i + 2;
                        }
                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("price parse time is: " + duration);

                        //for example: price trillion U.S dollars
                    } else if ((tokens.size() > i + 3) && (!isNumeric(tokens.get(i + 1))) && (tokens.get(i + 2).equals("U.S")) && (tokens.get(i + 3).equals("Dollars") ||
                            tokens.get(i + 3).equals("dollars"))) {

                        //                        startTime = System.nanoTime();

                        String str = PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2) + " "
                                + tokens.get(i + 3));
                        if(words.containsKey(str)){
                            words.put(str,words.get(str)+1);
                            i=i+3;
                        }
                        else{
                            words.put(str,1);
                            i=i+3;
                        }



                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("price parse time is: " + duration);
                    }//end of price type



                    //token is percentage type
                    else if (sToken.charAt(sToken.length() - 1) == '%') {

                        //                        startTime = System.nanoTime();
                        if(words.containsKey(PercentParse(sToken))){
                            words.put(PercentParse(sToken),words.get(PercentParse(sToken)+1));
                        }
                        else{
                            words.put(PercentParse(sToken),1);

                        }


                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("percentage parse time is: " + duration);

                    } else if ((tokens.size() > i + 1) && (tokens.get(i + 1).equals("percent") ||
                            tokens.get(i + 1).equals("percentage"))) {

                        //                        startTime = System.nanoTime();
                        String str = PercentParse(sToken + " " + tokens.get(i + 1));
                        if(words.containsKey(str)){
                            words.put(str,words.get(str)+1);
                            i++;
                        }
                        else{
                            words.put(str,1);
                            i++;
                        }

                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("percentage parse time is: " + duration);
                    }//end of percentage type



                    //token is Date type
                    else if ((tokens.size() > i + 1) && (ifDate((String) tokens.get(i + 1))) &&
                            ((int)Double.parseDouble(sToken) == Double.parseDouble(sToken))) {

                        String str =DateParse(sToken + " " + tokens.get(i + 1));
                        if(words.containsKey(str)){
                            words.put(str,words.get(str)+1);
                            i++;
                        }
                        else{
                            words.put(str,1);
                            i++;
                        }
                        //                        startTime = System.nanoTime();
                        //int temp = (int)Double.parseDouble(sToken);


                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("Date parse time is: " + duration);
                    }//end of Date type


                    else {//regular number without units

                        if ((tokens.size() > i + 1) && ((tokens.get(i + 1)).equals("Thousand") ||
                                tokens.get(i + 1).equals("Million") ||
                                tokens.get(i + 1).equals("Billion"))) {

                            //                            startTime = System.nanoTime();
                            String str =NumeericParse(sToken + " " + tokens.get(i + 1));
                            if(words.containsKey(str)){
                                words.put(str,words.get(str)+1);
                                i++;
                            }
                            else{
                                words.put(str,1);
                                i++;
                            }


                            //                            duration = System.nanoTime() - startTime;
                            //                            System.out.println("NumeericParse parse time is: " + duration);
                        } else {

                            //                            startTime = System.nanoTime();
                            String str =NumeericParse(sToken);
                            if(words.containsKey(str)){
                                words.put(str,words.get(str)+1);

                            }
                            else{
                                words.put(str,1);

                            }

                            //                            duration = System.nanoTime() - startTime;
                            //                            System.out.println("NumeericParse parse time is: " + duration);
                        }

                    }//end of regular number without units

                }//end of numeric type




                if (!ifTokenIsNumber) {//token is a word

                    //token is Date type
                    if (ifDate(sToken) && tokens.size() > i + 1 && isNumeric((String) tokens.get(i + 1)) &&
                            (int)Double.parseDouble((String) tokens.get(i + 1)) == Double.parseDouble((String) tokens.get(i + 1))) {

                        //                        startTime = System.nanoTime();
                        //int temp = (int)Double.parseDouble(tokens.get(i + 1));
                        String str =DateParse(sToken + " " + tokens.get(i + 1));
                        if(words.containsKey(str)){
                            words.put(str,words.get(str)+1);
                            i++;

                        }
                        else{
                            words.put(str,1);
                            i++;

                        }


                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("Date parse time is: " + duration);
                    }//end of date type





                    //token is price type

                    else if ((!((sToken.charAt(0) >= 65 && sToken.charAt(0) <= 90) || (sToken.charAt(0) >= 97 && sToken.charAt(0) <= 122))) &&
                            tokens.size() > i + 1 && sToken.length() >= 2 &&
                            (sToken.charAt(sToken.length() - 1) == 'm' || sToken.substring(sToken.length() - 2).equals("bn")) &&
                            (tokens.get(i + 1).equals("Dollars") || tokens.get(i + 1).equals("dollars"))) {

                        //                        startTime = System.nanoTime();
                        String str = PriceParse(sToken + " " + tokens.get(i + 1));
                        if(words.containsKey(str)){
                            words.put(str,words.get(str)+1);
                            i++;
                        }
                        else{
                            words.put(str,1);
                            i++;
                        }

                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("price parse time is: " + duration);
                        //System.out.println(sToken + " " + tokens.get(i + 1) + " " + i);

                    } else if ((tokens.size() > i + 2) && (tokens.get(i + 1).equals("m") ||
                            tokens.get(i + 1).equals("bn")) && (tokens.get(i + 2).equals("Dollars") ||
                            tokens.get(i + 2).equals("dollars"))) {

                        //                        startTime = System.nanoTime();
                        String str =PriceParse(sToken + " " + tokens.get(i + 1) + " " + tokens.get(i + 2));
                        if(words.containsKey(str)) {
                            words.put(str,words.get(str)+1);
                            i = i + 2;
                        }
                        else{
                            words.put(str,1);
                            i = i + 2;
                        }

                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("PriceParse time is: " + duration);

                    }




                    //token is an email
                    else if(sToken.contains("@") && (sToken.contains(".com") || sToken.contains(".COM"))){

                        //                        startTime = System.nanoTime();
                        String str = mailParse(sToken);
                        if(words.containsKey(str)){
                            words.put(str,words.get(str)+1);
                        }
                        else{
                            words.put(str,1);

                        }

                        //                        duration = System.nanoTime() - startTime;
                        //                        System.out.println("email parse time is: " + duration);
                    }//end of email type



                    //token is expression or a range
                    else if (sToken.contains("-") || sToken.equals("Between") || sToken.equals("between")) {

                        if (sToken.contains("-")) {

                            //                            startTime = System.nanoTime();

                            if(words.containsKey(sToken)){
                                words.put(sToken,words.get(sToken)+1);

                            }
                            else{
                                words.put(sToken,1);

                            }


                            String[] expressions = sToken.split("-");

                            for (int j = 0; j < expressions.length; j++) {

                                if (!ifStopWord(expressions[j], stopWords)) {
                                    if(words.containsKey(expressions[j])){
                                        i++;
                                        words.put(expressions[ j],words.get(expressions[j])+1);


                                    }
                                    else{
                                        i++;
                                        words.put(expressions[ j],1);
                                    }






                                }
                            }
                            //                            duration = System.nanoTime() - startTime;
                            //                            System.out.println("expression or a range parse time is: " + duration);
                        } else if ((tokens.size() > i + 3) && (sToken.equals("Between") || sToken.equals("between")) &&
                                (isNumeric((String) tokens.get(i + 1)) && tokens.get(i + 2).equals("and") &&
                                        isNumeric((String) tokens.get(i + 3)))) {

                            //                            startTime = System.nanoTime();
                            int M = i;
                            String str = tokens.get(M + 1) + "-" + tokens.get(M + 3);
                            if(words.containsKey(str)) {
                                words.put(str,words.get(str)+1);
                                i++;
                            }
                            else{
                                words.put(str,1);
                                i++;
                            }
                            String str2 = tokens.get(M + 1);
                            if(words.containsKey(str2)){
                                words.put(str2,words.get(str2)+1);
                                i++;
                            }
                            else{
                                words.put(str2,1);
                                i++;
                            }

                            String str3 = tokens.get(M + 1);
                            if(words.containsKey(str3)){
                                words.put(str3,words.get(str3)+1);
                                i++;
                            }
                            else{
                                words.put(str3,1);
                                i++;
                            }





                        }
                    }


                    //start with Capital letter
                    else if (sToken.charAt(0) >= 65 && sToken.charAt(0) <= 90) {

                        if ((tokens.size() == i + 1) || ((tokens.size() > i + 1) && !((tokens.get(i + 1)).charAt(0) >= 65 && (tokens.get(i + 1)).charAt(0) <= 90))) {

                            if (!ifStopWord(sToken.toLowerCase(), stopWords)) {

                                sToken = sToken.toLowerCase();
                                if(words.containsKey(sToken)){
                                    words.put(sToken,words.get(sToken)+1);

                                }
                                else{
                                    words.put(sToken,1);

                                }

                            }

                        }

                        else {

                            if (!ifStopWord(sToken.toLowerCase(), stopWords)) {

                                sToken = sToken.toLowerCase();
                                if(words.containsKey(sToken)){
                                    words.put(sToken,words.get(sToken)+1);

                                }
                                else{
                                    words.put(sToken,1);

                                }




                            }

                            //String entity = sToken + " " + tokens.get(i + 1);
                            //i = i + 2;
                            //int temp = index;
                            //index += 2;


                            String entity = tokens.get(i).toUpperCase();
                            i++;
                            //sToken = tokens.get(i);

                            while (tokens.size() > i && (tokens.get(i)).charAt(0) >= 65 && (tokens.get(i)).charAt(0) <= 90) {

                                sToken = tokens.get(i);

                                if (!ifStopWord(sToken.toLowerCase(), stopWords)) {

                                    sToken = sToken.toLowerCase();
                                    if(words.containsKey(sToken)){
                                        words.put(sToken,words.get(sToken)+1);

                                    }
                                    else{

                                        words.put(sToken,1);

                                    }

                                    //tempIForSingleTerms++;


                                }

                                entity = entity + " " + sToken.toUpperCase();
                                i++;
                            }
                            entity = entity.toLowerCase();
                            i--;
                            //  addTerm(term = PriceParse(sToken), index, terms, docID);
                            //  index++;
                            if(words.containsKey(entity)){
                                words.put(entity,words.get(entity)+1);

                            }
                            else{
                                words.put(entity,1);

                            }

                        }
                    }

                    else {
                        if(words.containsKey(sToken)){
                            words.put(sToken,words.get(sToken)+1);

                        }
                        else{
                            words.put(sToken,1);

                        }



                    }
                }//end of word option


                ifTokenIsNumber = false;
            }

            catch (IndexOutOfBoundsException e){
                //System.out.println("doc is: " + doc);
                countExceptions++;
                System.out.println("tokens before: " + tokens.get(i - 12) + " " + tokens.get(i-11) + tokens.get(i - 10) + " " + tokens.get(i-9)
                        + tokens.get(i - 8) + " " + tokens.get(i-7) + tokens.get(i - 6) + " " + tokens.get(i-5) +
                        " " + tokens.get(i - 4) + " " + tokens.get(i-3) + tokens.get(i - 2) + " " + tokens.get(i-1));
                //System.out.println(e + "the problem is in: " + sToken + " index is: " + index);

                System.out.println("this is: " + countExceptions + "exception");

            }
            catch (NumberFormatException e){
                // System.out.println("doc is: " + doc);
                countExceptions++;
                System.out.println("tokens before: " + tokens.get(i - 12) + " " + tokens.get(i-11) + tokens.get(i - 10) + " " + tokens.get(i-9)
                        + tokens.get(i - 8) + " " + tokens.get(i-7) + tokens.get(i - 6) + " " + tokens.get(i-5) +
                        " " + tokens.get(i - 4) + " " + tokens.get(i-3) + tokens.get(i - 2) + " " + tokens.get(i-1));
                // System.out.println(e + "the problem is in: " + sToken + " index is: " + index);
                System.out.println("this is: " + countExceptions + "exception");
            }


        }//end of for loop

        if(stemming==true){
            HashMap<String,Integer>wordsWithStemming = new HashMap<>();

            for(Map.Entry<String,Integer>map:words.entrySet()) {
                int value = map.getValue();
                int length =map.getKey().length();
                String key = map.getKey();
                stm.add(key.toCharArray(),length);
                stm.stem();
                key = stm.toString();
                wordsWithStemming.put(key,value);
            }
            return wordsWithStemming;
        }


        return words;


    }
}

