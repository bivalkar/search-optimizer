/**
 * 
 */
package com.anish.search;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author AnishBivalkar
 *
 */
public class FrequentWordSearcherTest {
    private static String textBlob;

    final static String rootDir = System.getProperty("user.dir");
    final static String pathToDataFile = rootDir + "/src/test/resources/data.txt";

    private static List<String> wordsToCompare = new ArrayList<String>();
    private static final String LANGUAGE = "english";

    @BeforeClass
    public static void setup() throws IOException {
        InputStream inputStream = 
                new FileInputStream(new File(pathToDataFile));

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();
        textBlob = builder.toString();
    }

    @Test
    public void testWordFrequencyForStemmer() {
        wordsToCompare.add("evernot");
        wordsToCompare.add("team");

        FrequentWordSearcher.switchOnWordStemmer(LANGUAGE);
        List<String> mostFrequentWords = FrequentWordSearcher.getMostFrequentWords(textBlob, 2);

        int i = 0;
        for (String mostFrequentWord : mostFrequentWords) {
            assertEquals("Cannot find the most frequent word", wordsToCompare.get(i++), mostFrequentWord);
        }
    }

    @Test
    public void testWordFrequencyForNonStemmer() {
        wordsToCompare.add("evernote");
        wordsToCompare.add("products");

        List<String> mostFrequentWords = FrequentWordSearcher.getMostFrequentWords(textBlob, 2);

        int i = 0;
        for (String mostFrequentWord : mostFrequentWords) {
            assertEquals("Cannot find the most frequent word", wordsToCompare.get(i++), mostFrequentWord);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWordFrequencyForNullInput() {
        FrequentWordSearcher.getMostFrequentWords(null, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWordFrequencyForEmptyInput() {
        FrequentWordSearcher.getMostFrequentWords("", 2);
    }

    @Test
    public void testWordFrequencyWithStemmer() {
        FrequentWordSearcher.switchOnWordStemmer(LANGUAGE);
        FrequentWordSearcher.getMostFrequentWords("Nokia here delivers Nokias best products ever product HERE hErE nokias", 2);
    }

    @Test
    public void testWordFrequencyForZeroWords() {
        List<String> emptyList = FrequentWordSearcher.getMostFrequentWords(textBlob, 0);
        assertTrue("List is not empty", emptyList.isEmpty());
    }

    @Test
    public void testInitializeStemmer() {
        assertFalse("Stemmer is initialized without calling it", 
                FrequentWordSearcher.isStemmerInitialized());
        FrequentWordSearcher.switchOnWordStemmer(LANGUAGE);
        assertTrue("Stemmer is not yet initialized", 
                FrequentWordSearcher.isStemmerInitialized());
    }

    @Test
    public void testInitializeStemmerWithUnknownLanguage() {
        assertFalse("Stemmer is initialized without calling it", 
                FrequentWordSearcher.isStemmerInitialized());
        FrequentWordSearcher.switchOnWordStemmer("breakit");
        assertFalse("Stemmer is not yet initialized", 
                FrequentWordSearcher.isStemmerInitialized());
    }

    @Test
    public void testextractWordFrequency() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Method method = FrequentWordSearcher.class.getDeclaredMethod("extractWordFrequency", Map.class, String[].class);
        method.setAccessible(true);

        Map<String, Integer> testWordCount = new HashMap<String, Integer>();
        String[] words = new String[] {"anish", "anish", "evernote", "anish"};

        Object[] parameters = new Object[2];
        parameters[0] = testWordCount;
        parameters[1] = words;

        int maxFreq = (int) method.invoke(null, parameters);
        assertEquals("Max occuring freq does not match the input", maxFreq, 3);
    }

    @Test
    public void testextractWordFrequencyForEmptyWords() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Method method = FrequentWordSearcher.class.getDeclaredMethod("extractWordFrequency", Map.class, String[].class);
        method.setAccessible(true);

        Map<String, Integer> testWordCount = new HashMap<String, Integer>();
        String[] words = new String[2];

        Object[] parameters = new Object[2];
        parameters[0] = testWordCount;
        parameters[1] = words;

        int maxFreq = (int) method.invoke(null, parameters);
        assertEquals("Max occuring freq does not match the input", maxFreq, 0);

        words = null;
        parameters[1] = words;
        maxFreq = (int) method.invoke(null, parameters);
        assertEquals("Max occuring freq does not match the input", maxFreq, 0);
    }

    @Test
    public void testbucketSortFrequency() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Method method = FrequentWordSearcher.class.getDeclaredMethod("bucketSortFrequency", Map.class, int.class);
        method.setAccessible(true);

        Map<String, Integer> testWordCount = new HashMap<String, Integer>();
        testWordCount.put("anish", 3);
        testWordCount.put("evernote", 1);
        testWordCount.put("best", 1);

        Object[] parameters = new Object[2];
        parameters[0] = testWordCount;
        parameters[1] = 3;

        @SuppressWarnings("unchecked")
        List<List<String>> freqBuckets = (List<List<String>>) method.invoke(null, parameters);
        assertNotNull("This list is null when it should have had 3 buckets", freqBuckets);

        List<String> firstBucket = freqBuckets.get(0);
        assertTrue("Does not contain the required word", firstBucket.contains("best"));
        assertTrue("Does not contain the required word", firstBucket.contains("evernote"));

        List<String> secondBucket = freqBuckets.get(1);
        assertNull("Second bucket should have been null", secondBucket);

        List<String> thirdBucket = freqBuckets.get(2);
        assertTrue("Does not contain the required word", thirdBucket.contains("anish"));
    }


    @Test
    public void testbucketSortFrequencyFor0FreqWord() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Method method = FrequentWordSearcher.class.getDeclaredMethod("bucketSortFrequency", Map.class, int.class);
        method.setAccessible(true);

        Map<String, Integer> testWordCount = new HashMap<String, Integer>();
        testWordCount.put("anish", 3);
        testWordCount.put("evernote", 1);
        testWordCount.put("best", 1);

        Object[] parameters = new Object[2];
        parameters[0] = testWordCount;
        parameters[1] = 0;

        try {
            method.invoke(null, parameters);
        } catch (InvocationTargetException ex) {
            assertEquals("Should have thrown assertionError", ex.getTargetException().toString(), "java.lang.AssertionError");
        }
    }

    @Test
    public void getMostFrequentWords() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Method method = FrequentWordSearcher.class.getDeclaredMethod("getMostFrequentWords", List.class, int.class, int.class);
        method.setAccessible(true);

        List<List<String>> testArray = new ArrayList<List<String>>();

        List<String> bucket = new ArrayList<String>();
        bucket.add("evernote");
        bucket.add("best");
        testArray.add(0, bucket);

        bucket = new ArrayList<String>();
        bucket.add("anish");
        testArray.add(1, bucket);

        Object[] parameters = new Object[3];
        parameters[0] = testArray;
        parameters[1] = 2;
        parameters[2] = 1;

        List<String> mostFrequentWord = (List<String>) method.invoke(null, parameters);
        assertTrue("Incorrect list size, expected 1 ", mostFrequentWord.size() == 1);
        assertTrue("Incorrect word", "anish".equals(mostFrequentWord.get(0)));
    }

    @After
    public void tearDown() throws Exception {
        wordsToCompare.clear();
        FrequentWordSearcher.switchOffWordStemmer();
    }
}
