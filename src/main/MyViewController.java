package main;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MyViewController {

    public Button pathButton;
    public Button postingpathButton;
    public Button resetButton;
    public Button showDictionaryButton;
    public Button loadDictionaryButton;
    public Button startButton;
    public CheckBox stemming;
    public TextField pathCorpus;
    public TextField pathPosting;


    public TextField queryoption1;
    public TextField queryoption2;
    public CheckBox semantic;
    public ComboBox comboChooseDoc;
    public Button showTop5entities;


    //public CheckBox searchIden;
    //public CheckBox entities;
    public Button runButton;
    public Button browseFile;


    public boolean stemmingflag;
    public boolean semanticFlag;
    public boolean entitiesFlag;
    public Parser parser;
    public Indexer indexer;
    public String choosenDoc;
    public Ranker ranker;
    public Searcher searcher;
    // public ReadFile readfile;
    //   public ArrayList<String>resultsFromMoreThanOneQuery;
    public ArrayList<String>result;




    public MyViewController() {

        stemmingflag = false;
        semanticFlag = false;
        entitiesFlag = false;
        result = new ArrayList<>();
        choosenDoc = "";




//queryoption2.getText().substring(0,queryoption2.getText().indexOf("queries.txt")-1)



        /// add this now
        /*
         readfile = new ReadFile(pathCorpus.getText(), pathPosting.getText(), pathCorpus.getText()+"\\corpus\\stop_words.txt");
        this.parser = readfile.getParser();
        parser.setStemming(stemmingflag);
        this.indexer = parser.getIndexer();

         */

    }


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


    ///////checkbox

    public void browseCorpus(ActionEvent actionEvent) {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDict = directoryChooser.showDialog(null);
        if (selectedDict != null) {
            pathCorpus.setText(selectedDict.getAbsolutePath());
        }
        // String pathOfCourpus = pathCorpus.getText();
        //Parser p = new Parser(pathOfCourpus+"\\corpus\\stop_words.txt", new List[2]);
        // setParser(p);

    }

    public void browsePosting(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDict = directoryChooser.showDialog(null);
        if (selectedDict != null) {
            pathPosting.setText(selectedDict.getAbsolutePath());

            ///new
            parser=null;
            searcher=null;
            ranker=null;
             parser = new Parser(new DateForParser(), pathPosting.getText(), createStopWordsTable(pathPosting.getText() + "\\stop_words.txt"));
            parser.setStemming(stemmingflag);
            searcher = new Searcher(parser, pathPosting.getText());
            ranker = searcher.getRanker();
            //ArrayList results = new ArrayList();
        }

    }


    public void startIndexing(ActionEvent actionEvent) {
        if (pathCorpus.getText().length() == 0 || pathPosting.getText().length() == 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(" null checkbox "); //edit
            alert.show();
        } else {
            /// start indexing!
            // String pathOfCourpus = path1.getText();


            ReadFile readFile = new ReadFile(pathCorpus.getText(), pathPosting.getText(), pathCorpus.getText()+"\\corpus\\stop_words.txt");
            this.parser = readFile.getParser();
            parser.setStemming(stemmingflag);

            this.indexer = parser.getIndexer();


            long startTime = System.nanoTime();
            readFile.Read();
            long time = System.nanoTime() - startTime;

            //setParser(p);
            //Indexer indexer = new Indexer(parser);
            //indexer.IndexFiles();
            // indexer.WriteTheDictionaryToFile();
            //String pathOfPostingFiles = path2.getText();//// what to do with this


            /// need to do this things
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            int numberOfIndexedDocs = readFile.getDocId();///// need to do this function that count the docs that indexed
            int numberOfTerms = indexer.getDictionarySize();

            /*
//
//

             */

            ;////// need to put timers at the end and count the total time
            String message = "";
            message = message + "number of docs that indexed:" + numberOfIndexedDocs + "\n";
            message = message + " number of terms:" + numberOfTerms + "\n";
            message = message + " the total time of run time:" + time;
            alert.setContentText(message); //edit
            alert.show();
        }
        //check if the text are directories


    }

    public void useStemming(ActionEvent actionEvent) {
        if (stemming.isSelected()) {
            //parser.setStemming(true);
            stemmingflag = true;
        } else {
            // parser.setStemming(false);
            stemmingflag = false;
        }

    }

    public void loadDictionary(ActionEvent actionEvent) { ///// work

        if(pathPosting.getText()==null||pathPosting.getText().length()==0){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("the posting file path is empty");
            alert.show();

        }
        else {

            HashMap<String, Term> dictionary = ranker.getDictionary();
            this.indexer = new Indexer(pathPosting.getText(), stemmingflag);
            indexer.setDictionary(dictionary);
            System.out.println(indexer.getDictionarySize());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("the dictionary is loaded");
            alert.show();

        }
       // HashMap<String,Term> dictionary = new HashMap();
        /*
        dictionary = new HashMap<>();

        try {

            FileReader fr = new FileReader(pathPosting.getText() + "/Dictionary.txt");
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
        this.indexer = new Indexer(pathPosting.getText(),stemmingflag);
        indexer.setDictionary(dictionary);
        System.out.println(indexer.getDictionarySize());
         */

        /*
        TreeMap<String, Pair<Integer, String>> dictionaeytoLoad;


        dictionaeytoLoad = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try {
            FileReader fr = new FileReader(pathPosting.getText() + "\\Dictionary.txt");
            BufferedReader bufferedReader = new BufferedReader(fr);
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {

                String term = inputLine.substring(6, inputLine.indexOf(" Term Frequency from all corpus:"));
                int index = inputLine.indexOf("corpus:") + 8;
                String frequency = inputLine.substring(index);
                int frequencynum = Integer.parseInt(frequency);
                if (stemmingflag == false) {

                    if (term.charAt(0) >= 65 && term.charAt(0) <= 90) {

                        dictionaeytoLoad.put(term, new Pair(frequencynum, "posting." + term.toUpperCase().charAt(0) + ".txt"));
                    } else { /// number
                        dictionaeytoLoad.put(term, new Pair(frequencynum, "posting.numbers.txt"));
                    }
                } else { /// stemming is true
                    if (term.charAt(0) >= 65 && term.charAt(0) <= 90) {
                        dictionaeytoLoad.put(term, new Pair(frequencynum, "postingStemming." + term.toUpperCase().charAt(0) + ".txt"));

                    } else {
                        dictionaeytoLoad.put(term, new Pair(frequencynum, "postingStemming.numbers.txt"));

                    }
                }
            }
            bufferedReader.close();
            fr.close();
        } catch (Exception e) {
        }

        //ReadFile readfile = new ReadFile(pathCorpus.getText(), pathPosting.getText(), pathCorpus.getText()+"\\corpus\\stop_words.txt");
       // this.parser = readfile.getParser();
       // parser.setStemming(stemmingflag);

        this.indexer = new Indexer(pathPosting.getText(),stemmingflag);
        indexer.setDictionary(dictionaeytoLoad);
        System.out.println(indexer.getDictionarySize());

*/
    }

    public void showDictionary(ActionEvent actionEvent) {


        ObservableList<String> data = FXCollections.observableArrayList();

        ListView<String> listView = new ListView<String>(data);
        TreeMap<String, Term> dictionary;
        dictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if(indexer==null){

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("dictionary not exist");
            alert.show();

        }
        else {
            HashMap<String, Term> copyDictionary = indexer.getDictionary();



            for (Map.Entry<String, Term> term : copyDictionary.entrySet()) {
                String key = term.getKey();
                Term value = term.getValue();
                dictionary.put(key, value);
            }


            //dictionary = indexer.getDictionary();
            dictionary.forEach((key, value) -> {
                data.add(key + " Total frequency:" + value.getTotalFrequencyInCorpus());
                listView.setItems(data);

            });
            VBox vbox = new VBox(listView);


            Scene scene = new Scene(vbox);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
        }


    }

    public void ResetDIctionary(ActionEvent actionEvent) {


        // delete the A-Z FILES AND THE POSTING FILES
        boolean ifDelete = true;
        File filesPostingTOcheck = new File((pathPosting.getText() + "//postingStemming.A.txt"));
        if (filesPostingTOcheck.exists()) {
            stemmingflag = true;
        }
        for (char c = 'A'; c <= 'Z'; c++) {

            //File file = new File(pathPosting.getText() + "//" + c + ".txt");
            //file.delete();
            if(stemmingflag==false) {
                File filesPosting = new File((pathPosting.getText() + "//posting." + c + ".txt"));
                if (!filesPosting.exists()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("posting files doesnt exist");
                    alert.show();
                    ifDelete = false;
                    break;
                } else {
                    filesPosting.delete();
                }
            }
            else {
                File fileWithStemmingPosting = new File((pathPosting.getText() + "//postingStemming." + c + ".txt"));

                if (!fileWithStemmingPosting.exists()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("posting files doesnt exist");
                    alert.show();
                    ifDelete = false;
                    break;
                } else {
                    fileWithStemmingPosting.delete();
                }

            }
        }



        /// DELETE THE NUMBERS FILE AND POSTING FILE
        //File file = new File(path2.getText() + "//numbers.txt");
        //file.delete();
        if(stemmingflag==false) {
            File filePostingNum = new File(pathPosting.getText() + "//posting.numbers.txt");
            if (!filePostingNum.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("posting files doesnt exist");
                ifDelete = false;

                //              alert.show();

            } else {
                filePostingNum.delete();
            }

        }
        else {
            File fileWithStemmingPosting = new File((pathPosting.getText() + "//postingStemming.numbers.txt"));

            if (!fileWithStemmingPosting.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("posting files doesnt exist");
                ifDelete = false;

//                alert.show();

            } else {
                fileWithStemmingPosting.delete();
            }
        }
        //delete the doctionary files
        //   File dictionary = new File(pathPosting.getText() + "//Dictionary.txt");
        // dictionary.delete();
        stemmingflag = false;
        File file1 = new File((pathPosting.getText() + "//corpusInfo.txt"));
        File file2 = new File((pathPosting.getText() + "//Dictionary.txt"));
        File file3 = new File((pathPosting.getText() + "//docInformation.txt"));
        File file4 = new File((pathPosting.getText() + "//writeEntitiesInDocs.txt"));
        File file5 = new File((pathPosting.getText() + "//1-64.txt"));

        file1.delete();
        file2.delete();
        file3.delete();
        file4.delete();
        file5.delete();

        if(ifDelete==true){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("The posting files are deleted");
            alert.show();
        }



        // reset the memory somehow


    }


    ///part B

    public void runQuery(ActionEvent actionEvent) {
        if((queryoption1.getText().length()==0||queryoption1.getText()==null)&&(queryoption2.getText().length()==0||queryoption2.getText()==null)){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("the text boxes are empty");
            alert.show();
        }

        else {

            if (pathPosting.getText() == null || pathPosting.getText().length() == 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("the path posting are empty");
                alert.show();
            } else {
                //String path = queryoption2.getText().substring(0, queryoption2.getText().indexOf(("queries.txt")));
                //path = path.substring(0, path.length() - 3);
                //System.out.println(path);
               // Parser parser = new Parser(new DateForParser(), pathPosting.getText(), createStopWordsTable(pathPosting.getText() + "\\stop_words.txt"));
               // this.parser = parser;
                parser.setStemming(stemmingflag);

                //new
                comboChooseDoc.getSelectionModel().clearSelection();
                comboChooseDoc.getItems().clear();


//queryoption2.getText().substring(0,queryoption2.getText().indexOf("queries.txt")-1)
               // Searcher searcher = new Searcher(parser, pathPosting.getText());
               // ranker = searcher.getRanker();
                ranker.setIfSemanti(semanticFlag);
                ArrayList results = new ArrayList();

                if (queryoption1.getText().length() != 0 && (queryoption2.getText().length() == 0 || queryoption2.getText() == null)) {
                    results = searcher.runOneQuery(queryoption1.getText());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    String numberOfRelevantDocs = ""+ranker.getNumOfReleventDocs();
                    alert.setContentText("the number of relavent docs: " + numberOfRelevantDocs);
                    alert.show();


                    ObservableList<String> data = FXCollections.observableArrayList();

                    ListView<String> listView = new ListView<String>(data);

                    for (int i = 0; i < results.size(); i++) {
                        data.add((String) results.get(i));
                        listView.setItems(data);
                    }

                    VBox vbox = new VBox(listView);
                    Scene scene = new Scene(vbox);
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.show();
                    result = results;





                } else if (queryoption2.getText().length() != 0 &&((queryoption1.getText().length() == 0)||queryoption1.getText()==null)) {
                    result.clear();

                    ArrayList<Pair<String, String>> resultFromAllQueries = searcher.runMoreThanOneQuery(queryoption2.getText());
                    // LinkedHashMap<String, String> resultfromAllQueries = searcher.runMoreThanOneQuery(queryoption2.getText());
                    ObservableList<String> data = FXCollections.observableArrayList();
                    String oldValue = "";
                    ListView<String> listView = new ListView<String>(data);
                    for (Pair<String, String> id : resultFromAllQueries) {
                        if (id.getValue() != oldValue) {
                            data.add((String) "-------QueryNum:" + id.getValue() + "-----------");
                            listView.setItems(data);
                            data.add(id.getKey());
                            listView.setItems(data);
                        } else {
                            data.add(id.getKey());
                            listView.setItems(data);
                        }
                        oldValue = id.getValue();


                    }
                    VBox vbox = new VBox(listView);
                    Scene scene = new Scene(vbox);
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.show();
                    for (Pair<String, String> id : resultFromAllQueries) {
                        result.add(id.getKey());
                    }

/*
            VBox vbox = new VBox(listView);
            Scene scene = new Scene(vbox);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();

            ///optional
            for (Map.Entry<String, String> id : resultfromAllQueries.entrySet()) {
                resultsFromMoreThanOneQuery.add(id.getKey());
            }

 */

/*
            Stage stage2 = new Stage();
            stage2.setTitle("ComboBox Experiment 1");

            ComboBox comboBox = new ComboBox();
            for(int i=0;i<resultsFromMoreThanOneQuery.size();i++){
                comboBox.getItems().add(resultsFromMoreThanOneQuery.get(i));
            }

            HBox hbox = new HBox(comboBox);

            Scene scene2 = new Scene(hbox, 200, 120);
            stage2.setScene(scene2);
            stage2.show();\




        }




    }
     */
                }

                else{
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("the 2 query options are full, please fill only one text box for the query");
                    alert.show();
                }

                if ((result != null)&&result.size()!=0) {
                    for (int i = 0; i < result.size(); i++) {
                        comboChooseDoc.getItems().add(result.get(i));
                    }
                }
            }
        }
    }
    public void browseQueryFile(ActionEvent actionEvent){
        FileChooser fileChooser = new FileChooser();

        File selectedfile = fileChooser.showOpenDialog(null);
        if (selectedfile != null) {
            queryoption2.setText(selectedfile.getAbsolutePath());
        }

    }






    public void semanticTreatment(ActionEvent actionEvent){
        if (semantic.isSelected()) {
            //parser.setStemming(true);
            semanticFlag = true;

        } else {
            // parser.setStemming(false);
            semanticFlag = false;
        }

    }

    /*
    public void showEntities (ActionEvent actionEvent) {
        if(entities.isSelected()){

            entitiesFlag = true;
        }
        else{
            entitiesFlag = false;

        }

    }

     */



    public void chooseDocNo(ActionEvent actionEvent){
       // comboChooseDoc.getItems().removeAll();
        if(result!=null){
            for(int i=0;i<result.size();i++){
                comboChooseDoc.getItems().add(result.get(i));
            }
            choosenDoc =(String) comboChooseDoc.getValue();
            System.out.println(choosenDoc);

        }


    }

    public void showTop5entites(ActionEvent actionEvent){
        if(choosenDoc==null||choosenDoc.length()==0){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("doc is not selected");
            alert.show();

        }

        else{
            if(ranker.get5Entities(choosenDoc)==null||ranker.get5Entities(choosenDoc).size()==0){
                Alert alertEntities = new Alert(Alert.AlertType.INFORMATION);
                alertEntities.setContentText("no entities in this doc");
                alertEntities.show();
            }
            else {
                LinkedHashMap<String, Integer> entities = ranker.get5Entities(choosenDoc);
                String entityString = "";
                for (Map.Entry<String, Integer> entity : entities.entrySet()) {
                    entityString = entityString + entity.getKey() + " grade for entity:" + entity.getValue() + "\n";
                }
                Alert alertEntities = new Alert(Alert.AlertType.INFORMATION);
                alertEntities.setContentText(entityString);
                alertEntities.show();
            }


        }

    }




}

