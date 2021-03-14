package com.github.tornaia.jimglabel.tf;

import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.StdArrays;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.proto.framework.ConfigProto;
import org.tensorflow.proto.framework.GPUOptions;
import org.tensorflow.proto.framework.MetaGraphDef;
import org.tensorflow.proto.framework.SignatureDef;
import org.tensorflow.proto.framework.TensorInfo;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TUint8;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TFServiceDefaultImpl implements TFService {

    private static final Logger LOG = LoggerFactory.getLogger(TFServiceDefaultImpl.class);

    private static final String SAVED_MODEL_DIRECTORY = "C:/workspace/tensorflow2/workspace/training_demo/exported-models/my_model/saved_model/";
    private static final String LABEL_MAP = "C:/workspace/tensorflow2/workspace/training_demo/annotations/label_map.pbtxt";
    private static final String CARD_MAP = "C:/temp/!source_images/classes.json";

    private final SerializerUtils serializerUtils;
    private Map<Integer, String> labels;
    private SavedModelBundle savedModel;

    @Autowired
    public TFServiceDefaultImpl(SerializerUtils serializerUtils) {
        this.serializerUtils = serializerUtils;

        try {
            long start = System.currentTimeMillis();
            this.labels = loadIdToCardIdLabelMap();
            printLabels();

            this.savedModel = SavedModelBundle
                    .loader(SAVED_MODEL_DIRECTORY)
                    .withConfigProto(ConfigProto.newBuilder().setGpuOptions(GPUOptions.newBuilder().setAllowGrowth(true)).build())
                    .withTags("serve")
                    .load();
            printSignature(savedModel);

            BufferedImage testImage = ImageIO.read(new ClassPathResource("tf_test_image.jpg").getInputStream());
            List<Detection> detections = detect(testImage);
            Set<String> actualTopDetections = detections
                    .stream()
                    .filter(e -> e.getScore() > 0.99F)
                    .map(Detection::getLabel)
                    .collect(Collectors.toSet());

            Set<String> expectedTopDetections = Set.of("0453_Dorani varázsló", "0511_Dakul kán", "0521_Mohran wra Garruda");
            boolean success = actualTopDetections.equals(expectedTopDetections);
            if (!success) {
                throw new IllegalStateException("Failed to detect objects, expected: " + expectedTopDetections + ", actual: " + actualTopDetections);
            }
            LOG.info("TensorFlow initialized in {} ms", (System.currentTimeMillis() - start));
        } catch (Exception e) {
            LOG.error("Failed to initialize TFService", e);
        }
    }

    @Override
    public List<Detection> detect(BufferedImage inputImage) {
        Tensor<TUint8> input = createTensor(inputImage);

        List<Tensor<?>> outputs = savedModel
                .session()
                .runner()
                .feed(getInputNodeName(savedModel, "input_tensor"), input)
                .fetch(getOutputNodeName(savedModel, "num_detections"))
                .fetch(getOutputNodeName(savedModel, "detection_scores"))
                .fetch(getOutputNodeName(savedModel, "detection_classes"))
                .fetch(getOutputNodeName(savedModel, "detection_boxes"))
                .run();

        try (Tensor<TFloat32> detectionsT = outputs.get(0).expect(TFloat32.DTYPE);
             Tensor<TFloat32> scoresT = outputs.get(1).expect(TFloat32.DTYPE);
             Tensor<TFloat32> classesT = outputs.get(2).expect(TFloat32.DTYPE);
             Tensor<TFloat32> boxesT = outputs.get(3).expect(TFloat32.DTYPE)) {
            // All these tensors have:
            // - 1 as the first dimension
            // - maxObjects as the second dimension
            // While boxesT will have 4 as the third dimension (2 sets of (x, y) coordinates).
            // This can be verified by looking at scoresT.shape() etc.
            int maxObjects = (int) scoresT.shape().asArray()[1];
            float detections = detectionsT.data().copyTo(NdArrays.ofFloats(Shape.of(1))).getFloat(0);

            // float[] scores = scoresT.copyTo(new float[1][maxObjects])[0];
            float[] scores = StdArrays.array1dCopyOf(scoresT.data().copyTo(NdArrays.ofFloats(Shape.of(1, maxObjects))).get(0));

            // float[] classes = classesT.copyTo(new float[1][maxObjects])[0];
            float[] classes = StdArrays.array1dCopyOf(classesT.data().copyTo(NdArrays.ofFloats(Shape.of(1, maxObjects))).get(0));

            // float[][] boxes = boxesT.copyTo(new float[1][maxObjects][4])[0];
            float[][] boxes = StdArrays.array2dCopyOf(boxesT.data().copyTo(NdArrays.ofFloats(Shape.of(1, maxObjects, 4))).get(0));

            // Print all objects whose score is at least 0.25
            List<Detection> result = new ArrayList<>();
            for (int i = 0; i < scores.length; ++i) {
                // System.out.printf("\tFound %-20s (score: %.4f)\n", labels.get((int) classes[i]), scores[i]);
                result.add(new Detection(boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3], labels.get((int) classes[i]), scores[i]));
                // drawCard(inputImage, boxes[i], (int) classes[i], scores[i]);
            }

            // ImageIO.write(inputImage, "png", new File("C:/temp/out.png"));
            return result;
        }
    }

    private static Tensor<TUint8> createTensor(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_BYTE_INDEXED ||
                img.getType() == BufferedImage.TYPE_BYTE_BINARY ||
                img.getType() == BufferedImage.TYPE_BYTE_GRAY ||
                img.getType() == BufferedImage.TYPE_USHORT_GRAY ||
                img.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            BufferedImage bgr = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            bgr.getGraphics().drawImage(img, 0, 0, null);
            img = bgr;
        }

        if (img.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            throw new IllegalStateException(String.format("Expected 3-byte BGR encoding in BufferedImage, found %d. This code could be made more robust", img.getType()));
        }
        byte[] data = ((DataBufferByte) img.getData().getDataBuffer()).getData();
        // ImageIO.read seems to produce BGR-encoded images, but the model expects RGB.
        bgr2rgb(data);
        final long BATCH_SIZE = 1;
        final long CHANNELS = 3;
        Shape shape = Shape.of(BATCH_SIZE, img.getHeight(), img.getWidth(), CHANNELS);
        return Tensor.of(TUint8.DTYPE, shape, DataBuffers.of(data, true, false));
    }

    private static void bgr2rgb(byte[] data) {
        for (int i = 0; i < data.length; i += 3) {
            byte tmp = data[i];
            data[i] = data[i + 2];
            data[i + 2] = tmp;
        }
    }

    private void printLabels() throws Exception {
        Map<Integer, String> labelMap = loadIdToCardIdLabelMap();
        StringBuilder sb = new StringBuilder("Objects");
        sb.append(System.lineSeparator());
        sb.append(String.format("\tLabels (%d)", labelMap.size()));
        sb.append(System.lineSeparator());
        sb.append(String.format("\t%-10s %s", "ID", "Name"));
        sb.append(System.lineSeparator());
        for (Map.Entry<Integer, String> entry : labelMap.entrySet()) {
            sb.append(String.format("\t%-10s %s", entry.getKey(), entry.getValue()));
            sb.append(System.lineSeparator());
        }
        LOG.info(sb.toString());
    }

    private static void printSignature(SavedModelBundle model) {
        MetaGraphDef m = model.metaGraphDef();
        SignatureDef sig = m.getSignatureDefOrThrow("serving_default");
        StringBuilder sb = new StringBuilder("Model signature");
        sb.append(System.lineSeparator());
        sb.append(String.format("\tInputs (%d)", sig.getInputsCount()));
        sb.append(System.lineSeparator());
        sb.append(String.format("\t%-10s %-30s %-30s %s", "#", "Key", "Name", "Type"));
        sb.append(System.lineSeparator());
        int i = 1;
        for (Map.Entry<String, TensorInfo> entry : sig.getInputsMap().entrySet()) {
            TensorInfo t = entry.getValue();
            sb.append(String.format("\t%-10s %-30s %-30s %s", i++, entry.getKey(), t.getName(), t.getDtype()));
            sb.append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        sb.append(String.format("\tOutputs (%d)", sig.getOutputsCount()));
        sb.append(System.lineSeparator());
        sb.append(String.format("\t%-10s %-30s %-30s %s", "#", "Key", "Name", "Type"));
        sb.append(System.lineSeparator());
        i = 1;
        for (Map.Entry<String, TensorInfo> entry : sig.getOutputsMap().entrySet()) {
            TensorInfo t = entry.getValue();
            sb.append(String.format("\t%-10s %-30s %-30s %s", i++, entry.getKey(), t.getName(), t.getDtype()));
            sb.append(System.lineSeparator());
        }
        LOG.info(sb.toString());
    }

    private static String getInputNodeName(SavedModelBundle model, String name) {
        MetaGraphDef m = model.metaGraphDef();
        SignatureDef sig = m.getSignatureDefOrThrow("serving_default");
        for (Map.Entry<String, TensorInfo> entry : sig.getInputsMap().entrySet()) {
            TensorInfo t = entry.getValue();
            if (entry.getKey().equals(name)) {
                return t.getName();
            }
        }

        throw new IllegalStateException("Must not happen, name: " + name);
    }

    private static String getOutputNodeName(SavedModelBundle model, String name) {
        MetaGraphDef m = model.metaGraphDef();
        SignatureDef sig = m.getSignatureDefOrThrow("serving_default");
        for (Map.Entry<String, TensorInfo> entry : sig.getOutputsMap().entrySet()) {
            TensorInfo t = entry.getValue();
            if (entry.getKey().equals(name)) {
                return t.getName();
            }
        }

        throw new IllegalStateException("Must not happen, name: " + name);
    }

    private Map<Integer, String> loadIdToCardIdLabelMap() throws Exception {
        Path path = Paths.get(LABEL_MAP);

        List<Integer> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        Files.lines(path).forEach(line -> {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("id: ")) {
                int id = Integer.parseInt(trimmedLine.split(": ")[1]);
                ids.add(id);
            } else if (trimmedLine.startsWith("name: '")) {
                String name = trimmedLine.split(": '")[1];
                if (!name.endsWith("'")) {
                    throw new IllegalStateException("Must not happen, line: " + line);
                }
                name = name.substring(0, name.length() - 1);
                names.add(name);
            }
        });
        if (ids.size() != names.size()) {
            throw new IllegalStateException("Must not happen, ids: " + ids.size() + ", names: " + names.size());
        }

        Map<Integer, String> labels = new HashMap<>();
        Map<String, String> cardMap = loadCardIdToCardNameMap();
        for (int i = 0; i < ids.size(); i++) {
            labels.put(ids.get(i), cardMap.get(names.get(i)));
        }
        return labels;
    }

    private Map<String, String> loadCardIdToCardNameMap() throws Exception {
        String s = Files.readString(Path.of(CARD_MAP));
        Map<?, ?> map = serializerUtils.toObject(s, Map.class);
        List<Map<String, String>> classes = (List<Map<String, String>>) map.get("classes");
        return classes
                .stream()
                .collect(Collectors.toMap(e -> e.get("id"), e -> e.get("cardId") + "_" + e.get("name")));
    }

    private void drawCard(BufferedImage bufferedImage, float[] box, int clazz, float score) {
        String cardName = labels.get(clazz);
        String label = String.format("%s %.4f", cardName, score);

        int imageHeight = bufferedImage.getHeight();
        int imageWidth = bufferedImage.getWidth();

        // normalized_coordinates
        int top = (int) (imageHeight * box[0]);
        int left = (int) (imageWidth * box[1]);
        int bottom = (int) (imageHeight * box[2]);
        int right = (int) (imageWidth * box[3]);

        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(left, top, right - left, bottom - top);

        g2d.setPaint(Color.RED);
        g2d.setFont(new Font("Serif", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(label, left + 4, Math.max(fm.getHeight(), top + fm.getHeight()));
        g2d.dispose();
    }
}
