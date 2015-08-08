package net.gmbx.w2v;

import java.util.HashMap;

public class Vocabulary
{
    private final String[]                 vocab;
    private final HashMap<String, Integer> termMap;

    private static final String            OPTIONAL_ASTERISK = "*";

    public Vocabulary(String[] vocab)
    {
        this.vocab = vocab;
        this.termMap = loadTermIndex(vocab);
    }

    private HashMap<String, Integer> loadTermIndex(String[] vocab)
    {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < vocab.length; i++)
        {
            map.put(vocab[i], i);
        }
        return map;
    }

    public String getTerm(int i)
    {
        return vocab[i];
    }

    public Integer getIndex(String term)
    {

        Integer id = termMap.get(term);
        if (id == null)
        {
            id = termMap.get(OPTIONAL_ASTERISK.concat(term));
        }
        return id;
    }
}
