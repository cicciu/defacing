import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.img.DicomImageReader;
import org.dcm4che3.img.DicomImageReaderSpi;
import org.dcm4che3.img.op.MaskArea;
import org.dcm4che3.img.stream.DicomFileInputStream;
import org.dcm4che3.img.util.Editable;
import org.dcm4che3.img.util.SupplierEx;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageProcessor;

public class Defacing {

  public static void main (String[] args){
    System.out.println("Hello World");

    try {
      String currentDirectory = System.getProperty("user.dir");
      String nativeLibsURLPath = String.format("file:%s/target/native-lib/lib", currentDirectory);
      URL resource = new URL(nativeLibsURLPath);
      NativeLibraryManager.initNativeLibs(resource);
    } catch (Exception e1) {
      throw new IllegalStateException("Cannot register DICOM native librairies", e1);
    }

    try {
      String dicomPath = System.getenv("DICOM_PATH");
      DicomImageReader reader = new DicomImageReader(new DicomImageReaderSpi());
      reader.setInput(
          new DicomFileInputStream(dicomPath));
      DicomFileInputStream in = new DicomFileInputStream(dicomPath);
      Attributes attributes = in.readDataset();

      Editable<PlanarImage> editable = transformImage(attributes, reader);

      List<SupplierEx<PlanarImage, IOException>> suppliers = reader
          .getLazyPlanarImages(null, editable);

      for (SupplierEx<PlanarImage, IOException> s: suppliers) {
        try {
          File defaceFile = new File("defaceimg.png");
          ImageProcessor.writePNG(s.get().toMat(), defaceFile);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static Editable<PlanarImage> transformImage(
      Attributes attributes, DicomImageReader reader) {
    boolean defacing = true;
    MaskArea maskArea = null;
    if (defacing) {
      return img -> {
        PlanarImage image = img;
        if (defacing) {
          try {
            image = Defacer.apply(attributes, reader.getPlanarImage());
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (maskArea != null) {
          image = MaskArea.drawShape(image.toMat(), maskArea);
        }
        return image;
      };
    }
    return null;
  }

}
