package net.gmbx.w2v;

import java.util.Arrays;

public class VectorModel
{
    private final int        vocabSize;
    private final int        vectorSize;

    private final float[][]  vectors;
    private final Vocabulary vocab;

    public VectorModel(int vocabSize, int vectorSize, String[] vocab,
            float[][] vectors)
    {
        if (vocab == null || vectors == null)
        {
            throw new RuntimeException("(vocab == null || vectors == null)");
        }
        else if (vocab.length != vectors.length)
        {
            throw new RuntimeException("vocab.length != vectors.length");
        }
        else if (vocab.length != vocabSize)
        {
            throw new RuntimeException("vocab.length != vocabSize");
        }
        else if (vectors[0] == null)
        {
            throw new RuntimeException("vectors[0] == null");
        }
        else if (vectors[0].length != vectorSize)
        {
            throw new RuntimeException("vectors[0].length != dimensions");
        }
        this.vocabSize = vocabSize;
        this.vectorSize = vectorSize;
        this.vectors = vectors;
        this.vocab = new Vocabulary(vocab);
    }

    public String getTerm(int i)
    {
        return vocab.getTerm(i);
    }

    public float[] getVector(int i)
    {
        return vectors[i];
    }

    public Integer getIndex(String term)
    {
        return vocab.getIndex(term);
    }

    public int getVocabSize()
    {
        return vocabSize;
    }

    public int getVectorSize()
    {
        return vectorSize;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        VectorModel other = (VectorModel) obj;
        if (vectorSize != other.vectorSize) return false;
        if (!Arrays.deepEquals(vectors, other.vectors)) return false;
        if (vocab == null)
        {
            if (other.vocab != null) return false;
        }
        else if (!vocab.equals(other.vocab)) return false;
        if (vocabSize != other.vocabSize) return false;
        return true;
    }

    public float[] composeUnitVector(int[] searchIDs)
    {
        float[] composite = null;
        for (int i = 0; i < searchIDs.length; i++)
        {
            float[] vec = getVector(searchIDs[i]);
            if (composite == null)
            {
                composite = new float[vec.length];
            }
            for (int j = 0; j < composite.length; j++)
            {
                composite[j] += vec[j];
            }
        }
        if (composite == null)
        {
            return null;
        }
        else
        {
            return Word2VecUtils.unitLength(composite);
        }
    }

}
