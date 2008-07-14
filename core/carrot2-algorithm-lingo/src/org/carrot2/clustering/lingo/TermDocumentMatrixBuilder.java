package org.carrot2.clustering.lingo;

import org.apache.commons.lang.ArrayUtils;
import org.carrot2.core.Document;
import org.carrot2.core.attribute.Processing;
import org.carrot2.text.preprocessing.PreprocessingContext;
import org.carrot2.util.DoubleComparators;
import org.carrot2.util.IndirectSorter;
import org.carrot2.util.attribute.*;
import org.carrot2.util.attribute.constraint.*;

import bak.pcj.set.IntBitSet;
import cern.colt.GenericPermuting;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Builds a term document matrix based on the provided {@link PreprocessingContext}.
 */
@Bindable
public class TermDocumentMatrixBuilder
{
    /**
     * Term weighting.
     * 
     * @level Medium
     * @group Matrix model
     */
    @Input
    @Processing
    @Attribute
    @ImplementingClasses(classes =
    {
        LogTfIdfTermWeighting.class, LinearTfIdfTermWeighting.class,
        TfTermWeighting.class
    })
    public TermWeighting termWeighting = new LogTfIdfTermWeighting();

    /**
     * Title word boost. Gives more weight to words that appeared in
     * {@link Document#TITLE} fields.
     * 
     * @level Medium
     * @group Matrix model
     */
    /*
     * TODO: For the time being, we hardcode this parameter on title words. Ideally, we
     * should find a way to enable the user to choose from list of field names to be
     * boosted.
     */
    @Input
    @Processing
    @Attribute
    @DoubleRange(min = 0, max = 10)
    public double titleWordsBoost = 2.0;

    /**
     * Maximum matrix size.
     * 
     * @level Advanced
     * @group Matrix model
     */
    @Input
    @Processing
    @Attribute
    @IntRange(min = 50 * 100)
    public int maximumMatrixSize = 250 * 150;

    /**
     * Builds a term document matrix from data provided in the <code>context</code>,
     * stores the result in there.
     */
    void build(LingoProcessingContext lingoContext)
    {
        final PreprocessingContext preprocessingContext = lingoContext.preprocessingContext;

        final int documentCount = preprocessingContext.documents.size();
        final int [] stemsTf = preprocessingContext.allStems.tf;
        final int [][] stemsTfByDocument = preprocessingContext.allStems.tfByDocument;
        final byte [][] stemsFieldIndices = preprocessingContext.allStems.fieldIndices;

        if (documentCount == 0)
        {
            lingoContext.tdMatrix = DoubleFactory2D.dense.make(0, 0);
            lingoContext.tdMatrixStemIndices = new int [0];
            return;
        }

        // Determine the index of the title field
        int titleFieldIndex = -1;
        final String [] fieldsName = preprocessingContext.allFields.name;
        for (int i = 0; i < fieldsName.length; i++)
        {
            if (Document.TITLE.equals(fieldsName[i]))
            {
                titleFieldIndex = i;
                break;
            }
        }

        // Determine the stems we, ideally, should include in the matrix
        int [] stemsToInclude = computeRequiredStemIndices(preprocessingContext);

        // Sort stems by weight, so that stems get included in the matrix in the order
        // of frequency
        final double [] stemsWeight = new double [stemsToInclude.length];
        for (int i = 0; i < stemsToInclude.length; i++)
        {
            final int stemIndex = stemsToInclude[i];
            stemsWeight[i] = termWeighting.calculateTermWeight(stemsTf[stemIndex],
                stemsTfByDocument[stemIndex].length / 2, documentCount)
                * getWeightBoost(titleFieldIndex, stemsFieldIndices[stemIndex]);
        }
        final int [] stemWeightOrder = IndirectSorter.sort(stemsWeight,
            DoubleComparators.REVERSED_ORDER);

        // Calculate the number of terms we can include to fulfill the max matrix size
        final int maxRows = maximumMatrixSize / documentCount;
        final DoubleMatrix2D tdMatrix = DoubleFactory2D.dense.make(Math.min(maxRows,
            stemsToInclude.length), documentCount);

        for (int i = 0; i < stemWeightOrder.length && i < maxRows; i++)
        {
            final int stemIndex = stemsToInclude[stemWeightOrder[i]];
            final int [] tfByDocument = stemsTfByDocument[stemIndex];
            final int df = tfByDocument.length / 2;
            final byte [] fieldIndices = stemsFieldIndices[stemIndex];

            int tfByDocumentIndex = 0;
            for (int documentIndex = 0; documentIndex < documentCount; documentIndex++)
            {
                if (tfByDocumentIndex * 2 < tfByDocument.length
                    && tfByDocument[tfByDocumentIndex * 2] == documentIndex)
                {
                    double weight = termWeighting.calculateTermWeight(
                        tfByDocument[tfByDocumentIndex * 2 + 1], df, documentCount);

                    weight *= getWeightBoost(titleFieldIndex, fieldIndices);
                    tfByDocumentIndex++;

                    tdMatrix.set(i, documentIndex, weight);
                }
            }
        }

        // Convert stemsToInclude into tdMatrixStemIndices
        GenericPermuting.permute(stemsToInclude, stemWeightOrder);

        // Store the results
        lingoContext.tdMatrix = tdMatrix;
        lingoContext.tdMatrixStemIndices = ArrayUtils.subarray(stemsToInclude, 0,
            tdMatrix.rows());
    }

    /**
     * Calculates the boost we should apply to a stem, based on the field indices array.
     */
    private double getWeightBoost(int titleFieldIndex, final byte [] fieldIndices)
    {
        for (int fieldIndex = 0; fieldIndex < fieldIndices.length; fieldIndex++)
        {
            if (fieldIndices[fieldIndex] == titleFieldIndex)
            {
                return titleWordsBoost;
            }
        }
        return 1;
    }

    /**
     * Computes stem indices of words that are one-word label candiates or are non-stop
     * words from phrase label candidates.
     */
    private int [] computeRequiredStemIndices(PreprocessingContext context)
    {
        final int [] labelsFeatureIndex = context.allLabels.featureIndex;
        final int [] wordsStemIndex = context.allWords.stemIndex;
        final boolean [] wordsCommonTermFlag = context.allWords.commonTermFlag;
        final int [][] phrasesWordIndices = context.allPhrases.wordIndices;
        final int wordCount = wordsStemIndex.length;

        final IntBitSet requiredStemIndices = new IntBitSet(labelsFeatureIndex.length);

        for (int i = 0; i < labelsFeatureIndex.length; i++)
        {
            final int featureIndex = labelsFeatureIndex[i];
            if (featureIndex < wordCount)
            {
                requiredStemIndices.add(wordsStemIndex[featureIndex]);
            }
            else
            {
                final int [] wordIndices = phrasesWordIndices[featureIndex - wordCount];
                for (int j = 0; j < wordIndices.length; j++)
                {
                    final int wordIndex = wordIndices[j];
                    if (!wordsCommonTermFlag[wordIndex])
                    {
                        requiredStemIndices.add(wordsStemIndex[wordIndex]);
                    }
                }
            }
        }

        return requiredStemIndices.toArray();
    }
}
