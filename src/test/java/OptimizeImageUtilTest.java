import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import com.github.tornaia.jimglabel.gui.domain.Annotation;
import com.github.tornaia.jimglabel.gui.util.OptimizeImageUtil;
import com.github.tornaia.jimglabel.gui.util.ImageWithMeta;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class OptimizeImageUtilTest {

    /*
    @Test
    public void middle() throws Exception {
        BufferedImage originalBufferedImage = ImageIO.read(new File(("C:/temp/1.original.jpg")));
        Annotation originalAnnotation = new SerializerUtils().toObject(Files.readString(Path.of("C:/temp/1.original.json")), Annotation.class);
        ImageWithMeta result = OptimizeImageUtil.optimize(new ImageWithMeta(originalBufferedImage, originalAnnotation.getObjects()));

        writeToDisk(result, "4.result");
    }

    private void writeToDisk(ImageWithMeta imageWithMeta, String name) throws Exception {
        String imageFilePath = "C:/temp/" + name + ".jpg";
        String annotationFilePath = "C:/temp/" + name + ".json";
        ImageIO.write(imageWithMeta.getImage(), "jpg", new File(imageFilePath));

        Annotation annotation = new Annotation(name + ".jpg", new File(imageFilePath).length(), imageWithMeta.getImage().getWidth(), imageWithMeta.getImage().getHeight(), imageWithMeta.getObjects());
        Files.writeString(Path.of(annotationFilePath), new SerializerUtils().toJSON(annotation));
    }
     */
}
