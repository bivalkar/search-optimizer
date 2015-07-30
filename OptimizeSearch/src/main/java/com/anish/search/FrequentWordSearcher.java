/**
 * 
 */
package com.anish.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;

/**
 * The class {@code FrequentWordSearcher} contains methods for performing basic
 * search in a given text for the most frequently occurring words. This class 
 * does not support internalization of strings though the porter stemmer library
 * that we use does given an option for various languages. If you have to use other
 * languages you can edit the StopWords.java file and call the initialize method
 * from the client using the language of you desire to get the frequent words for.
 * The stemmer only supports a few languages. For more information 
 * See <a href="http://snowball.tartarus.org/</a>
 * 
 * Use the {@link #initializeStemmer(String) 
 * 
 * It also provides an option to normalize the given text. For more information on 
 * this please visit
 * See <a href="http://snowball.tartarus.org/algorithms/english/stemmer.html</a>
 * 
 * This class reads and populates the stop words from a configuration file StopWords.txt
 * You can edit this list to add or delete any stop-words in the future.
 * 
 * <p>The <tt>extractWordFrequency</tt>, <tt>bucketSortFrequency</tt> operations run in 
 * O(n) time. The <tt>getMostFrequentWords</tt> runs in O(k) time where k = k = {@link Demo#numberOfFrequentWords}
 * 
 * I could have used gradle for dependency management but that was not the point of this
 * Exercise. To solve the same problem we can use various approaches whose tradeoffs can be
 * discussed like using tries and min heap simultaneously or maybe use lucene if we are dealing
 * with a large volume of data or just modify the classic word count problem using Hadoop (map-reduce)
 * to get the count of the most frequent words. These solutions can be discussed further but the
 * current implementation is easy to understand and maintain and fits the bill.
 * 
 * @author AnishBivalkar
 * @since  version.10
 *
 */
public final class FrequentWordSearcher {
    /**
     * Don't let anyone instantiate this class.
     */
    private FrequentWordSearcher() {}

    /** Logger object to log essential details */
    private static Logger logger = Logger.getLogger(FrequentWordSearcher.class);

    /** Set of words that will be ignored */
    private static Set<String> stopWords = new HashSet<String>();

    /** The stemmer object used for normalizing the text */
    private static SnowballStemmer stemmer = null;

    /** This block will populate the list of stop words only once */
    static {
        try {
            InputStream inputStream =
                    FrequentWordSearcher.class.getClassLoader().getResourceAsStream("StopWords.txt");

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] ignoreWords = line.split(",");
                for (String ignoreWord:ignoreWords) {
                    stopWords.add(ignoreWord);
                }
            }

            reader.close();
        } catch (IOException ex) {
            logger.debug("Cannot open the file for reading " + ex.getMessage());
        }
    }

    /**
     * This method is used to test if the stemmer is initialized. We only stem the
     * original word is the client wants to use the stemmer.
     * 
     * @return true, if stemmer is initialized and ready to be used
     *         false, otherwise
     */
    public static boolean isStemmerInitialized() {
        return stemmer != null;
    }

    /**
     * This method stops using the porter stemmer from this point onwards
     */
    public static void switchOffWordStemmer() {
        if (isStemmerInitialized()) {
            stemmer = null;
        }
    }

    /**
     * This method initializes an instance of porter stemmer from the client. The default
     * language that it uses is English which is currently hardcoded but it can be expanded
     * to use multiple languages.
     * See <a href="http://snowball.tartarus.org/index.php</a>
     */
    public static void switchOnWordStemmer(String lang) {
        if (logger.isDebugEnabled())
            logger.info("Initializing the porter stemmer client");
        try {
            Class<?> stemClass = Class.forName("org.tartarus.snowball.ext." + lang + "Stemmer");
            stemmer = (SnowballStemmer) stemClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            logger.warn("Class org.tartarus.snowball.ext.englishStemmer not found on classPath");
            stemmer = null;
        }
    }

    /**
     * This method validates the given input
     * @param text
     * @throws IllegalArgumentException, if text is null or empty
     */
    private static void validateInput(final String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Valid text required to find the most frequent words");
        }
    }

    /**
     * This method populates the map with the mapping from word to its frequency. It starts by first
     * checking if the given word is a stop word. In that case it will ignore and move onto the next
     * word. If the client has initialized the stemmer then the method will get the stem of the current
     * word and that would be added to the map, otherwise the original word and its count is added.
     * This method runs in linear time O(n) since we loop over each element present in the tokenized
     * string array.
     *
     * @param wordFrequency, The map containing words and their respective frequency. 
     *                       can never be empty
     * @param words,         The list of tokenized strings formed from the input text
     */
    private static int extractWordFrequency(Map<String, Integer> wordFrequency, String[] words) {
        Integer maxFreq = 0;

        if (words != null && words.length != 0) {
            for (String word : words) {
                if (word == null) {
                    if (logger.isInfoEnabled())
                        logger.info("Ignoring null values");

                    continue;
                }
                if (stopWords.contains(word)) {
                    if (logger.isDebugEnabled())
                        logger.debug("Ignore the stop word " + word);
                    continue;
                } else {
                    if (isStemmerInitialized()) {
                        stemmer.setCurrent(word);
                        if (stemmer.stem()) {
                            word = stemmer.getCurrent();
                        }

                        if (logger.isDebugEnabled())
                            logger.debug("Normalizing the english word to " + word);
                    }

                    int frequency = 1;
                    if (wordFrequency.containsKey(word)) {
                        frequency = wordFrequency.get(word) + 1;
                        if (maxFreq < frequency) {
                            maxFreq = frequency;
                        }
                    }

                    wordFrequency.put(word, frequency);
                }
            }
        } else {
            logger.info("Could not tokenize the text ");
        }

        logger.info("The count of the most occuring word in the text is " + maxFreq);
        return maxFreq;
    }

    /**
     * This method sorts the words by frequencies using a popular sorting technique 
     * called bucket sort though we do not actually sort it and use a variation of the
     * technique. We get the maxFrequency from the map and initialize an list of maxFreq
     * buckets where each bucket is a list of strings having the same frequency.
     * 
     * This method is designed to run in O(n). Space is also O(n) which is the extra
     * array we needed to store(buckets)
     * See <a href="https://en.wikipedia.org/wiki/Bucket_sort</a>
     * 
     * @param wordFrequency, The map containing words and their respective frequency. 
     *                       can never be empty
     * @param maxFreq,       Count of the most frequently seen word in the text.
     *                       can never be less than 1
     * 
     * @return list of list, containing strings sorted in order of frequencies with the lowest frequency
     *         words occurring first
     */
    private static List<List<String>> bucketSortFrequency(Map<String, Integer> wordFrequency,
            int maxFreq) {
        assertTrue(!wordFrequency.isEmpty());
        assertTrue(maxFreq > 0);

        if (logger.isInfoEnabled())
            logger.info("Sorting the words to their correct bucket locations based on frequency");

        List<List<String>> freqBucket = new ArrayList<List<String>>(maxFreq);
        for(int i = 0; i < maxFreq; i++) {
            freqBucket.add(null);
        }

        // Loop and populate bucket as per bucket sort
        for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue() - 1;

            List<String> sameFreqWords = freqBucket.get(value);
            if (sameFreqWords == null) {
                sameFreqWords = new ArrayList<String>();
            }

            sameFreqWords.add(key);

            if (!freqBucket.contains(sameFreqWords)) {
                freqBucket.set(value, sameFreqWords);
            }
        }


        return freqBucket;
    }

    /**
     * This method just iterates over the list to retrieve the most frequently occurring words
     *
     * @param freqBucket,            List of list containing strings sorted in order of frequencies with the lowest frequency
     *                               words occurring first. This can never be empty
     * @param maxFreq,               Count of the most frequently seen word in the text.
     *                               can never be less than 1
     * @param numberOfFrequentWords, Required count of most frequent words
     *                               can never be less than 1
     *
     * @return list of words, containing the {@link Demo#numberOfFrequentWords} 
     */
    private static List<String> getMostFrequentWords(List<List<String>> freqBucket, 
            int maxFreq, int numberOfFrequentWords) {
        assertTrue(numberOfFrequentWords > 0);
        assertTrue(!freqBucket.isEmpty());
        assertTrue(maxFreq > 0);

        List<String> mostFrequentWords = new ArrayList<String>();
        int wordsRequired = 0;
        bucketLoop:
        for (int i = freqBucket.size() - 1; i >= 0;) {
            List<String> sameFreqWords = freqBucket.get(i);

            if (sameFreqWords != null) {
                for (String sameFreqWord : sameFreqWords) {
                    if (sameFreqWord == null) {
                        logger.info("Ignoring null word while getting most Frequent Words");
                        continue;
                    }

                    if (wordsRequired == numberOfFrequentWords) {
                        break bucketLoop;
                    }

                    mostFrequentWords.add(sameFreqWord);
                    wordsRequired++;
                    i--;
                }
            } else {
                i--;
            }
        }

        return mostFrequentWords;
    }

    /**
     * This method computes the most frequently occurred words in the text using the following
     * algorithm
     * 
     * Step 1: Clean the text. Remove extra spaces, special characters and make string case insensitive
     * Step 2: Populate the map with count of all words and get the count of the most occurring word (O(n))
     * Step 3: Sort the words in the map and keep them in their respective bucket where (bucket = freqCount) (O(n))
     * Step 4: Then return the desired most frequent elements (O(k))
     * 
     * @param text, the blob of data
     * @param numberOfFrequentWords, the number of most frequent words
     * 
     * @throws IllegalArgumentException, if text is null or empty
     * @return a list, containing k frequent words where k = {@link Demo#numberOfFrequentWords} 
     */
    public static List<String> getMostFrequentWords(final String text, 
            int numberOfFrequentWords) {
        logger.info("Processing the list to find the most frequent occurring words");
        validateInput(text);

        if (numberOfFrequentWords <= 0) {
            return Collections.<String>emptyList();
        }

        String[] words = text.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
        Map<String, Integer> wordFrequency = new HashMap<String, Integer>();

        List<String> mostFrequentWords = null;

        int maxFreq = extractWordFrequency(wordFrequency, words);
        if (maxFreq > 0) {
            List<List<String>> freqBucket = bucketSortFrequency(wordFrequency, maxFreq);
            mostFrequentWords = getMostFrequentWords(freqBucket, 
                    maxFreq, numberOfFrequentWords);

            return mostFrequentWords;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Will return empty list as input is not in the correct format "
                        + Arrays.asList(words));
            }

            return Collections.<String>emptyList(); 
        }
    }

    public static void main(String[] args) {
        ArrayList<String> kFrequentWords = (ArrayList<String>) FrequentWordSearcher
                .getMostFrequentWords("testing for evernote anish evernote", 2);
        if (!kFrequentWords.isEmpty()) {
            System.out.println(kFrequentWords.get(0));
            System.out.println(kFrequentWords.get(1));
        }
    }
}