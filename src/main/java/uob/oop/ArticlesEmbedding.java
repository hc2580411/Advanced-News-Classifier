package uob.oop;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.Properties;


public class ArticlesEmbedding extends NewsArticles {
    private int intSize = -1;
    private String processedText = "";

    private INDArray newsEmbedding = Nd4j.create(0);

    public ArticlesEmbedding(String _title, String _content, NewsArticles.DataType _type, String _label) {
        //TODO Task 5.1 - 1 Mark
        super(_title,_content,_type,_label);
    }

    public void setEmbeddingSize(int _size) {
        //TODO Task 5.2 - 0.5 Marks
        intSize = _size;
    }

    public int getEmbeddingSize(){
        return intSize;
    }

    @Override
    public String getNewsContent() {
        // TODO Task 5.3 - 10 Marks
        if (processedText == null) {
            processedText = "";
        }
        if (processedText.isEmpty()) {
            String cleanedText = textCleaning(super.getNewsContent());

            Properties props = new Properties();
            props.setProperty("annotators", "tokenize,pos,lemma");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            CoreDocument document = pipeline.processToCoreDocument(cleanedText);

            StringBuilder lemmatizedText = new StringBuilder(cleanedText.length());

            for (CoreLabel token : document.tokens()) {
                lemmatizedText.append(token.lemma()).append(" ");
            }

            String stopwordPattern = String.join("|", Toolkit.STOPWORDS);

            processedText = lemmatizedText.toString().toLowerCase().replaceAll("\\b(" + stopwordPattern + ")\\b\\s*", "");

            processedText = processedText.replaceAll("\\s+", " ").trim();
            processedText = processedText.replaceAll("(\\d+)\\s+(\\d+)", "$1$2");


            return processedText;
        } else {
            return processedText.trim();
        }
    }

    public INDArray getEmbedding() throws Exception {
        // TODO Task 5.4 - 20 Marks

        int countNum = 0;
        String[] words = processedText.split(" ");


        while (newsEmbedding.isEmpty()){

            if (intSize == -1){
                throw new InvalidSizeException("Invalid size");
            }

            if (processedText.isEmpty()){
                throw new InvalidTextException("Invalid text");
            }

            newsEmbedding = Nd4j.zeros(intSize,50);

            for (int i = 0; i < words.length; i++) {
                Glove glove = GloveForCorrespondingWord(words[i]);
                if (countNum < intSize && glove != null){
                    INDArray repeatedVec = Nd4j.create(glove.getVector().getAllElements());
                    newsEmbedding.putRow(countNum,repeatedVec);
                    countNum++;
                }
            }


            return Nd4j.vstack(newsEmbedding.mean(1));

        }

        return Nd4j.vstack(newsEmbedding.mean(1));

    }

    private static Glove GloveForCorrespondingWord(String word){
        for (Glove glove : AdvancedNewsClassifier.listGlove){
            if ((glove.getVocabulary()).equalsIgnoreCase(word)){
                return glove;
            }
        }
        return null;
    }

    /***
     * Clean the given (_content) text by removing all the characters that are not 'a'-'z', '0'-'9' and white space.
     * @param _content Text that need to be cleaned.
     * @return The cleaned text.
     */
    private static String textCleaning(String _content) {
        StringBuilder sbContent = new StringBuilder();

        for (char c : _content.toLowerCase().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || Character.isWhitespace(c)) {
                sbContent.append(c);
            }
        }

        return sbContent.toString().trim();
    }
}
