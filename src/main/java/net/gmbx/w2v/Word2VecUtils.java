package net.gmbx.w2v;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;

public class Word2VecUtils
{
    // max length larger than google's word2vec code will output
    private static final int   MAX_TERM_LENGTH     = 500;
    public static final String DEFAULT_PUNC_STRING = "#PUNC#";

    public static void main(final String[] args) throws IOException
    {

        if (args.length != 1)
        {
            System.err
                .println("Usage: path/to/vector_model");
            System.exit(1);
        }
        long t0 = 0;
        long t1 = 0;
        String time;

        t0 = System.currentTimeMillis();
        VectorModel model = loadGoogleBinary(args[0], StandardCharsets.UTF_8,
                                             true);
        t1 = System.currentTimeMillis();
        time = (t1 - t0) / 1000 + "." + (t1 - t0) % 1000 + " s";
        System.out.println(time + " to load " + model.getVocabSize() + " "
                           + model.getVectorSize()
                           + "-dimensional word vecotrs");

        Runtime runtime = Runtime.getRuntime();
        NumberFormat format = NumberFormat.getInstance();
        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        sb.append("\nfree memory: " + format.format(freeMemory / 1024) + "\n");
        sb.append("allocated memory: " + format.format(allocatedMemory / 1024)
                  + "\n");
        sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");
        sb.append("total free memory: "
                  + format
                      .format((freeMemory + (maxMemory - allocatedMemory)) / 1024)
                  + "\n");
        System.out.println(sb);
    }

    public static VectorModel loadGoogleBinary(String pathToFile, Charset cs,
                                               boolean printProgress)
        throws IOException
    {
        String[] vocab = null;
        float[][] vectors = null;
        int vocabSize = 0;
        int vectorSize = 0;
        
        BufferedInputStream in = 
                new BufferedInputStream(new FileInputStream(pathToFile));
        try
        {
            int[] dim = readDataHeader(in);
            vocabSize = dim[0];
            vectorSize = dim[1];
            vectors = new float[vocabSize][vectorSize];
            vocab = new String[vocabSize];

            byte[] bytes = new byte[4 * dim[1]];
            ByteBuffer buf =
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < dim[0]; i++)
            {
                vocab[i] = readNextWord(in, cs);

                in.read(bytes);
                double len = 0;
                for (int j = 0; j < dim[1]; j++)
                {
                    vectors[i][j] = buf.getFloat(j * 4);
                    len += vectors[i][j] * vectors[i][j];
                }
                // convert to unit vector
                len = (float) Math.sqrt(len);
                for (int k = 0; k < dim[1]; k++)
                {
                    vectors[i][k] /= len;
                }
                if (printProgress && (i % (vocabSize / 100) == 0))
                {
                    System.out.print(".");
                }
            }
        }
        finally
        {
            if (in != null) in.close();
        }

        if (printProgress) System.out.println();
        return new VectorModel(vocabSize, vectorSize, vocab, vectors);
    }

    private static String readNextWord(BufferedInputStream in, Charset cs)
    {
        byte[] buf = new byte[MAX_TERM_LENGTH];
        try
        {
            int p = 0;
            char ch = (char) in.read();
            // GoogleNews-vectors-negative300.bin dosen't include '\n' chars
            // between vectors - this check allows you to load binary files
            // created with either version of Mikolov's word2vec code
            while (Character.isWhitespace(ch))
            {
                ch = (char) in.read();
            }
            while (!Character.isWhitespace(ch))
            {
                buf[p] = (byte) ch;
                ch = (char) in.read();
                p++;
            }
            buf = Arrays.copyOf(buf, p);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read next word");
        }
        return new String(buf, cs);
    }

    private static int[] readDataHeader(BufferedInputStream in)
    {
        int[] var = null;
        try
        {
            StringBuilder sb = new StringBuilder();
            char ch = (char) in.read();
            while (ch != '\n')
            {
                sb.append(ch);
                ch = (char) in.read();
            }
            int sep = sb.indexOf(" ");
            var = new int[2];
            var[0] = (int) Long.parseLong(sb.substring(0, sep).toString());
            var[1] = (int) Long.parseLong(sb.substring(sep + 1).toString());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read header");
        }
        return var;
    }

    public static VectorModel loadVectorModelFromText(String pathToFile,
                                                      boolean printProgress)
        throws IOException
    {
        String[] vocab = null;
        float[][] vectors = null;
        int vocabSize = 0;
        int vectorSize = 0;

        BufferedReader br = new BufferedReader(new FileReader(pathToFile));
        try
        {
            int i = 0;
            String line = null;
            while ((line = br.readLine()) != null)
            {
                final String[] field = line.split("\\s+");

                if (i == 0)
                {
                    vocabSize = Integer.parseInt(field[0]);
                    vectorSize = Integer.parseInt(field[1]);
                    vectors = new float[vocabSize][vectorSize];
                    vocab = new String[vocabSize];
                    i++;
                }
                else
                {
                    vocab[i - 1] = field[0];
                    for (int j = 1; j < field.length; j++)
                    {
                        vectors[i - 1][j - 1] = Float.parseFloat(field[j]);
                    }
                    vectors[i - 1] = unitLength(vectors[i - 1]);
                    i++;
                }
                if (printProgress && (i % (vocabSize / 100) == 0))
                {
                    System.out.print(".");
                }
            }
        }
        finally
        {
            if (br != null) br.close();
        }
        if (printProgress) System.out.println();
        return new VectorModel(vocabSize, vectorSize, vocab, vectors);
    }

    public static float[] unitLength(final float[] v)
    {
        float len = 0f;
        for (int i = 0; i < v.length; i++)
        {
            len += v[i] * v[i];
        }
        len = (float) Math.sqrt(len);

        final float[] u = new float[v.length];
        for (int i = 0; i < v.length; i++)
        {
            u[i] = v[i] / len;
        }
        return u;
    }

    public static String normalizePreservingUnderscores(String text)
    {
        StringBuilder sb = new StringBuilder();
        boolean includeSpace = false;
        for (int i = 0; i < text.length(); i++)
        {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch))
            {
                if (Character.isUpperCase(ch))
                {
                    ch = Character.toLowerCase(ch);
                }
                if (includeSpace)
                {
                    includeSpace = false;
                    sb.append(' ');
                }
                sb.append(ch);
            }
            else if (Character.isWhitespace(ch))
            {
                includeSpace = true;
            }
            else if (ch == '_')
            {
                sb.append(ch);
            }
        }
        return (sb.length() < 1) ? DEFAULT_PUNC_STRING : sb.toString();
    }

    public static String normalizeText(String text)
    {
        StringBuilder sb = new StringBuilder();
        boolean includeSpace = false;
        for (int i = 0; i < text.length(); i++)
        {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch))
            {
                if (Character.isUpperCase(ch))
                {
                    ch = Character.toLowerCase(ch);
                }
                if (includeSpace)
                {
                    includeSpace = false;
                    sb.append(' ');
                }
                sb.append(ch);
            }
            else if (Character.isWhitespace(ch))
            {
                includeSpace = true;
            }
        }
        return (sb.length() < 1) ? DEFAULT_PUNC_STRING : sb.toString();
    }

}
