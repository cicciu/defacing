import org.dcm4che3.data.Attributes;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageProcessor;

public class Defacer {

  public static final String APPLY_DEFACING = "defacing";

  private Defacer() {}

  public static PlanarImage apply(Attributes attributes, PlanarImage srcImg) {

    PlanarImage faceDetectImg = faceDetect(srcImg);

    return faceDetectImg;
  }

  public static PlanarImage faceDetect(PlanarImage srcImg) {
    ImageCV faceDetectImg = new ImageCV();
    srcImg.toMat().copyTo(faceDetectImg);

    MinMaxLocResult minMaxLocResult = ImageProcessor.findMinMaxValues(faceDetectImg.toMat());

    // THRESHOLD
    Imgproc.threshold(faceDetectImg.toImageCV(), faceDetectImg.toMat(), 200, minMaxLocResult.maxVal, Imgproc.THRESH_BINARY);

    // ERODE
    Mat kernel = new Mat();
    int kernel_size = 3;
    Mat ones = Mat.ones( kernel_size, kernel_size, CvType.CV_32F);
    Core.multiply(ones, new Scalar(1/(double)(kernel_size*kernel_size)), kernel);
    Imgproc.erode(faceDetectImg.toImageCV(), faceDetectImg.toMat(), kernel);

    // FILL BLACK HOLE
    Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(30,30));
    Imgproc.morphologyEx(faceDetectImg.toImageCV(), faceDetectImg.toMat(), Imgproc.MORPH_CLOSE, kernel2);

    // RESCALE 8BIT
    double min = minMaxLocResult.minVal;
    double max = minMaxLocResult.maxVal;
    double slope = 255.0 / (max - min);
    double yint = 255.0 - slope * max;
    faceDetectImg = ImageProcessor.rescaleToByte(faceDetectImg.toImageCV(), slope, yint);

    // CANNY DETECT CONTOUR
    Imgproc.Canny(faceDetectImg.toImageCV(), faceDetectImg.toMat(), 240, 260);

    // DRAW BLACK RECT 1/3
    Rect rect = new Rect(0, faceDetectImg.height()/3, faceDetectImg.width(),faceDetectImg.height());
    Imgproc.rectangle(faceDetectImg.toImageCV(), rect, new Scalar(0,0,0), Imgproc.FILLED);

    return faceDetectImg;
  }
}
