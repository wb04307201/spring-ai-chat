package cn.wubo.spring.ai.loom.agent.vectorstore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.jbellis.jvector.graph.GraphIndex;
import io.github.jbellis.jvector.graph.GraphIndexBuilder;
import io.github.jbellis.jvector.graph.GraphSearcher;
import io.github.jbellis.jvector.graph.ListRandomAccessVectorValues;
import io.github.jbellis.jvector.graph.SearchResult;
import io.github.jbellis.jvector.util.Bits;
import io.github.jbellis.jvector.vector.VectorSimilarityFunction;
import io.github.jbellis.jvector.vector.VectorizationProvider;
import io.github.jbellis.jvector.vector.types.VectorFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.observation.conventions.VectorStoreProvider;
import org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.converter.SimpleVectorStoreFilterExpressionConverter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JVector-based VectorStore implementation with disk persistence.
 * Uses JVector off-heap HNSW index for similarity search and
 * ConcurrentHashMap for document metadata storage.
 */
public class JVectorStore extends AbstractObservationVectorStore {

    private static final Logger logger = LoggerFactory.getLogger(JVectorStore.class);

    private static final String METADATA_FILE = "docs.json";

    private final String indexPath;
    private final int m;
    private final int efConstruction;
    private final int efSearch;
    private final VectorSimilarityFunction similarityFunction;

    private final ConcurrentHashMap<String, Document> documentStore = new ConcurrentHashMap<>();
    // Ordered list of document IDs -- index matches JVector graph node index
    private final List<String> documentIds = Collections.synchronizedList(new ArrayList<>());
    // Vector storage keyed by document ID (as VectorFloat)
    private final Map<String, VectorFloat<?>> embeddingMap = new ConcurrentHashMap<>();
    @SuppressWarnings("rawtypes")
    private volatile GraphIndex graphIndex;
    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser;
    private final VectorizationProvider vectorizationProvider;

    public JVectorStore(JVectorStoreBuilder builder) {
        super(builder);
        this.indexPath = builder.indexPath;
        this.m = builder.m;
        this.efConstruction = builder.efConstruction;
        this.efSearch = builder.efSearch;
        this.similarityFunction = builder.similarityFunction;
        this.objectMapper = JsonMapper.builder().build();
        this.expressionParser = new SpelExpressionParser();
        this.vectorizationProvider = VectorizationProvider.getInstance();
        initialize();
    }

    public static JVectorStoreBuilder builder(EmbeddingModel embeddingModel) {
        return new JVectorStoreBuilder(embeddingModel);
    }

    private synchronized void initialize() {
        Path path = Paths.get(indexPath);
        Path metadataFile = path.resolve(METADATA_FILE);

        if (Files.exists(metadataFile)) {
            loadFromDisk(path, metadataFile);
        } else {
            createNewIndex();
        }
    }

    private void loadFromDisk(Path path, Path metadataFile) {
        try {
            TypeReference<Map<String, Document>> typeRef = new TypeReference<>() {};
            Map<String, Document> loadedDocs = objectMapper.readValue(metadataFile.toFile(), typeRef);
            documentStore.putAll(loadedDocs);

            Path idsFile = path.resolve("ids.json");
            if (Files.exists(idsFile)) {
                TypeReference<List<String>> idTypeRef = new TypeReference<>() {};
                List<String> loadedIds = objectMapper.readValue(idsFile.toFile(), idTypeRef);
                documentIds.addAll(loadedIds);
            } else {
                documentIds.addAll(loadedDocs.keySet());
            }

            for (String id : documentIds) {
                Document doc = documentStore.get(id);
                if (doc != null) {
                    float[] embedding = embeddingModel.embed(doc);
                    VectorFloat<?> vf = toVectorFloat(embedding);
                    embeddingMap.put(id, vf);
                }
            }

            if (!embeddingMap.isEmpty()) {
                rebuildGraph();
            } else {
                createNewIndex();
            }

            logger.info("Loaded {} documents from disk index at {}", documentStore.size(), indexPath);
        } catch (IOException e) {
            logger.error("Failed to load index from disk, creating new index", e);
            createNewIndex();
        }
    }

    private void createNewIndex() {
        try {
            Path path = Paths.get(indexPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            GraphIndexBuilder builder = createGraphBuilder(List.of());
            graphIndex = builder.build(new ListRandomAccessVectorValues(List.of(), embeddingModel.dimensions()));
            try {
                builder.close();
            } catch (IOException e) {
                logger.warn("Error closing GraphIndexBuilder", e);
            }
            logger.info("Created new JVector index at {} with dimensions={}", indexPath, embeddingModel.dimensions());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create JVector index directory", e);
        }
    }

    private GraphIndexBuilder createGraphBuilder(List<VectorFloat<?>> vectors) {
        int dimensions = embeddingModel.dimensions();
        ListRandomAccessVectorValues ravv = new ListRandomAccessVectorValues(vectors, dimensions);
        return new GraphIndexBuilder(ravv, similarityFunction, m, efConstruction, 1.0f, 1.4f);
    }

    private VectorFloat<?> toVectorFloat(float[] data) {
        return vectorizationProvider.getVectorTypeSupport().createFloatVector(data);
    }

    @Override
    public void doAdd(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        for (Document document : documents) {
            logger.debug("Embedding document id={}", document.getId());
            float[] embedding = embeddingModel.embed(document);
            VectorFloat<?> vf = toVectorFloat(embedding);
            documentStore.put(document.getId(), document);
            embeddingMap.put(document.getId(), vf);
            documentIds.add(document.getId());
        }

        rebuildGraph();
        persistToDisk();
    }

    private synchronized void rebuildGraph() {
        int dimensions = embeddingModel.dimensions();

        if (embeddingMap.isEmpty()) {
            GraphIndexBuilder builder = createGraphBuilder(List.of());
            graphIndex = builder.build(new ListRandomAccessVectorValues(List.of(), dimensions));
            try {
                builder.close();
            } catch (IOException e) {
                logger.warn("Error closing GraphIndexBuilder", e);
            }
            return;
        }

        // Build ordered vector list from documentIds to keep index alignment
        List<VectorFloat<?>> allVectors = new ArrayList<>(documentIds.size());
        for (String id : documentIds) {
            VectorFloat<?> vec = embeddingMap.get(id);
            if (vec != null) {
                allVectors.add(vec);
            }
        }

        GraphIndexBuilder builder = createGraphBuilder(allVectors);
        ListRandomAccessVectorValues ravv = new ListRandomAccessVectorValues(allVectors, dimensions);
        graphIndex = builder.build(ravv);
        try {
            builder.close();
        } catch (IOException e) {
            logger.warn("Error closing GraphIndexBuilder", e);
        }
    }

    private synchronized void persistToDisk() {
        try {
            Path path = Paths.get(indexPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            objectMapper.writeValue(path.resolve(METADATA_FILE).toFile(), documentStore);
            objectMapper.writeValue(path.resolve("ids.json").toFile(), documentIds);

            logger.debug("Persisted {} documents to disk index at {}", documentStore.size(), indexPath);
        } catch (IOException e) {
            logger.error("Failed to persist index to disk", e);
            throw new RuntimeException("Failed to persist JVector index", e);
        }
    }

    @Override
    public void doDelete(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }

        for (String id : idList) {
            documentStore.remove(id);
            embeddingMap.remove(id);
            documentIds.remove(id);
        }

        rebuildGraph();
        persistToDisk();
    }

    @Override
    protected void doDelete(Filter.Expression filterExpression) {
        List<String> idsToDelete = documentStore.keySet().stream()
                .filter(id -> matchesFilter(documentStore.get(id), filterExpression))
                .toList();

        if (!idsToDelete.isEmpty()) {
            doDelete(idsToDelete);
        }
    }

    @Override
    public List<Document> doSimilaritySearch(SearchRequest request) {
        float[] queryEmbedding = embeddingModel.embed(request.getQuery());

        if (graphIndex == null || graphIndex.size() == 0) {
            return Collections.emptyList();
        }

        int dimensions = queryEmbedding.length;
        VectorFloat<?> queryVector = toVectorFloat(queryEmbedding);

        SearchResult result;
        try {
            result = GraphSearcher.search(queryVector, request.getTopK() * 2,
                    null, similarityFunction, graphIndex, Bits.ALL);
        } catch (Exception e) {
            logger.error("Error during JVector search", e);
            return Collections.emptyList();
        }

        List<Document> results = new ArrayList<>();
        for (SearchResult.NodeScore nodeScore : result.getNodes()) {
            String docId = getDocIdByNodeIndex(nodeScore.node);
            if (docId != null) {
                Document doc = documentStore.get(docId);
                if (doc != null) {
                    VectorFloat<?> docEmbedding = embeddingMap.get(docId);
                    if (docEmbedding != null) {
                        double score = cosineSimilarity(queryEmbedding, docEmbedding);
                        Document scoredDoc = new Document.Builder()
                                .id(doc.getId())
                                .text(doc.getText())
                                .metadata(doc.getMetadata())
                                .score(score)
                                .build();
                        results.add(scoredDoc);
                    }
                }
            }
        }

        List<Document> filteredResults = results.stream()
                .filter(doc -> !request.hasFilterExpression() || matchesFilter(doc, request.getFilterExpression()))
                .filter(doc -> doc.getScore() >= request.getSimilarityThreshold())
                .sorted(Comparator.comparing(Document::getScore).reversed())
                .limit(request.getTopK())
                .toList();

        return filteredResults;
    }

    private double cosineSimilarity(float[] vectorX, VectorFloat<?> vectorY) {
        if (vectorX == null || vectorY == null) {
            throw new IllegalArgumentException("Vectors must not be null");
        }
        if (vectorX.length != vectorY.length()) {
            throw new IllegalArgumentException("Vectors lengths must be equal");
        }

        float dotProduct = 0;
        float normX = 0;
        float normY = 0;
        for (int i = 0; i < vectorX.length; i++) {
            dotProduct += vectorX[i] * vectorY.get(i);
            normX += vectorX[i] * vectorX[i];
            normY += vectorY.get(i) * vectorY.get(i);
        }

        if (normX == 0 || normY == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(normX) * Math.sqrt(normY));
    }

    private boolean matchesFilter(Document document, Filter.Expression filterExpression) {
        if (filterExpression == null) {
            return true;
        }
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("metadata", document.getMetadata());
        Expression expression = expressionParser.parseExpression(
                new SimpleVectorStoreFilterExpressionConverter().convertExpression(filterExpression));
        Boolean result = expression.getValue(context, Boolean.class);
        return result != null && result;
    }

    private String getDocIdByNodeIndex(int nodeIndex) {
        synchronized (documentIds) {
            if (nodeIndex >= 0 && nodeIndex < documentIds.size()) {
                return documentIds.get(nodeIndex);
            }
            return null;
        }
    }

    @Override
    public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
        VectorStoreSimilarityMetric metric = (similarityFunction == VectorSimilarityFunction.DOT_PRODUCT)
                ? VectorStoreSimilarityMetric.DOT
                : VectorStoreSimilarityMetric.COSINE;
        return VectorStoreObservationContext.builder(VectorStoreProvider.SIMPLE.value(), operationName)
                .dimensions(embeddingModel.dimensions())
                .collectionName("jvector-disk-index")
                .similarityMetric(metric.value());
    }

    public static final class JVectorStoreBuilder extends AbstractVectorStoreBuilder<JVectorStoreBuilder> {

        private String indexPath = ".local/jvector-index";
        private int m = 16;
        private int efConstruction = 100;
        private int efSearch = 10;
        private VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.COSINE;

        private JVectorStoreBuilder(EmbeddingModel embeddingModel) {
            super(embeddingModel);
        }

        public JVectorStoreBuilder indexPath(String indexPath) {
            this.indexPath = indexPath;
            return this;
        }

        public JVectorStoreBuilder m(int m) {
            this.m = m;
            return this;
        }

        public JVectorStoreBuilder efConstruction(int efConstruction) {
            this.efConstruction = efConstruction;
            return this;
        }

        public JVectorStoreBuilder efSearch(int efSearch) {
            this.efSearch = efSearch;
            return this;
        }

        public JVectorStoreBuilder similarityFunction(VectorSimilarityFunction similarityFunction) {
            this.similarityFunction = similarityFunction;
            return this;
        }

        @Override
        public JVectorStore build() {
            return new JVectorStore(this);
        }
    }
}
