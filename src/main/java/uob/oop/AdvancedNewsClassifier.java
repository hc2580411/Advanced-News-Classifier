package uob.oop;

import org.apache.commons.lang3.time.StopWatch;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdvancedNewsClassifier {
    public Toolkit myTK = null;
    public static List<NewsArticles> listNews = null;
    public static List<Glove> listGlove = null;
    public List<ArticlesEmbedding> listEmbedding = null;
    public MultiLayerNetwork myNeuralNetwork = null;

    public final int BATCHSIZE = 10;

    public int embeddingSize = 0;
    private static StopWatch mySW = new StopWatch();

    public AdvancedNewsClassifier() throws IOException {
        myTK = new Toolkit();
        myTK.loadGlove();
        listNews = myTK.loadNews();
        listGlove = createGloveList();
        listEmbedding = loadData();
    }

    public static void main(String[] args) throws Exception {
        mySW.start();
        AdvancedNewsClassifier myANC = new AdvancedNewsClassifier();

        myANC.embeddingSize = myANC.calculateEmbeddingSize(myANC.listEmbedding);
        myANC.populateEmbedding();
        myANC.myNeuralNetwork = myANC.buildNeuralNetwork(2);
        myANC.predictResult(myANC.listEmbedding);
        myANC.printResults();
        mySW.stop();
        System.out.println("Total elapsed time: " + mySW.getTime());
    }

    public List<Glove> createGloveList() {
        List<Glove> listResult = new ArrayList<>();
        //  TODO Task 6.1 - 5 Marks

        String[] stopwords = Toolkit.STOPWORDS;
        List<String> vocabulary = Toolkit.getListVocabulary();
        List<double[]> vectors = Toolkit.getlistVectors();
        for (int i = 0; i < vocabulary.size(); i++) {
            String word = vocabulary.get(i);

            boolean isStopword = false;
            for (String stopword : stopwords) {
                if (word.equals(stopword)) {
                    isStopword = true;
                    break;
                }
            }

            if (!isStopword) {
                double[] vectorArray = vectors.get(i);
                Vector vector = new uob.oop.Vector(vectorArray);
                Glove glove = new Glove(word, vector);
                listResult.add(glove);
            }
        }

        return listResult;
    }


    public static List<ArticlesEmbedding> loadData() {
        List<ArticlesEmbedding> listEmbedding = new ArrayList<>();
        for (NewsArticles news : listNews) {
            ArticlesEmbedding myAE = new ArticlesEmbedding(news.getNewsTitle(), news.getNewsContent(), news.getNewsType(), news.getNewsLabel());
            listEmbedding.add(myAE);
        }
        return listEmbedding;
    }

    public int calculateEmbeddingSize(List<ArticlesEmbedding> _listEmbedding) {
        int intMedian = -1;
        //  TODO Task 6.2 - 5 Marks
        List<Integer> documentLengths = new ArrayList<>();


        for (ArticlesEmbedding embedding : _listEmbedding){
            String content = embedding.getNewsContent();
            String[] wordsList = content.split("\\s+");
            int wordCount = 0;
            for (String word : wordsList){
                if (Toolkit.getListVocabulary().contains(word) && GloveForCorrespondingWord(word) != null){
                    wordCount++;
                }
            }
            documentLengths.add(wordCount);
        }

        int size = documentLengths.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (documentLengths.get(j) > documentLengths.get(j + 1)) {
                    int temp = documentLengths.get(j);
                    documentLengths.set(j, documentLengths.get(j + 1));
                    documentLengths.set(j + 1, temp);
                }
            }
        }


        int middleIndex = size / 2;
        if (size % 2 == 0) {
            intMedian = (documentLengths.get(middleIndex) + documentLengths.get(middleIndex+1)) / 2;
        } else {
            intMedian = documentLengths.get(middleIndex);
        }

        return intMedian;

    }

    private static Glove GloveForCorrespondingWord(String word) {
        for (Glove glove : AdvancedNewsClassifier.listGlove) {
            if ((glove.getVocabulary()).equalsIgnoreCase(word)) {
                return glove;
            }
        }
        return null;
    }

    public void populateEmbedding() {
        //  TODO Task 6.3 - 10 Marks
        for (ArticlesEmbedding embedding : listEmbedding) {
            try {
                embedding.getEmbedding();
            } catch (InvalidSizeException e) {
                embedding.setEmbeddingSize(embeddingSize);
            } catch (InvalidTextException e) {
                embedding.getNewsContent();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }

    public DataSetIterator populateRecordReaders(int _numberOfClasses) throws Exception {
        ListDataSetIterator myDataIterator = null;
        List<DataSet> listDS = new ArrayList<>();
        INDArray inputNDArray = null;
        INDArray outputNDArray = null;

        //  TODO Task 6.4 - 8 Marks

        try{
            for (ArticlesEmbedding embedding : listEmbedding) {
                String newsType = embedding.getNewsType().toString();
                if (newsType.equals("Training")){
                    inputNDArray = embedding.getEmbedding();
                    outputNDArray = Nd4j.zeros(1,_numberOfClasses);

                    int IndexOfValue = Integer.parseInt(embedding.getNewsLabel());
                    int[] ArrayOfValueIndexes = {0,IndexOfValue -1};

                    outputNDArray.putScalar(ArrayOfValueIndexes,1);

                    DataSet newsDataSet = new DataSet(inputNDArray,outputNDArray);
                    listDS.add(newsDataSet);
                }
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
        }

        return new ListDataSetIterator(listDS, BATCHSIZE);
    }


    public MultiLayerNetwork buildNeuralNetwork(int _numOfClasses) throws Exception {
        DataSetIterator trainIter = populateRecordReaders(_numOfClasses);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(42)
                .trainingWorkspaceMode(WorkspaceMode.ENABLED)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .updater(Adam.builder().learningRate(0.02).beta1(0.9).beta2(0.999).build())
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(embeddingSize).nOut(15)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.HINGE)
                        .activation(Activation.SOFTMAX)
                        .nIn(15).nOut(_numOfClasses).build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        for (int n = 0; n < 100; n++) {
            model.fit(trainIter);
            trainIter.reset();
        }
        return model;
    }

    public List<Integer> predictResult(List<ArticlesEmbedding> _listEmbedding) throws Exception {
        List<Integer> listResult = new ArrayList<>();
        //TODO Task 6.5 - 8 Marks

        try{
            for (ArticlesEmbedding embedding : _listEmbedding){
                if (!(embedding.getNewsType().toString().equals("Training"))) {
                    INDArray inputNDArray = embedding.getEmbedding();
                    int[] predictedLabel = myNeuralNetwork.predict(inputNDArray);
                    listResult.add(predictedLabel[0]);
                    embedding.setNewsLabel(String.valueOf(predictedLabel[0]));
                }
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }


        return listResult;

    }

    public void printResults() {
        //TODO Task 6.6 - 6.5 Marks
        List<List<String>> groups = new ArrayList<>();

        for (ArticlesEmbedding embedding : listEmbedding) {
            if (embedding.getNewsType().toString().equals("Testing")) {
                int newsLabel = Integer.parseInt(embedding.getNewsLabel());
                while (groups.size() <= newsLabel) {
                    groups.add(new ArrayList<>());
                }

                groups.get(newsLabel).add(embedding.getNewsTitle());
            }
        }

        for (int i = 0; i < groups.size(); i++){
            System.out.println("Group " + (i + 1));
            for (String title : groups.get(i)) {
                System.out.println(title);
            }
        }


    }
}
