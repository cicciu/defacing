import org.dcm4che3.data.Attributes;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
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
    PlanarImage faceDetectionImg = faceDetection(srcImg);
    PlanarImage defaceImage = addRandPxlLine(srcImg, faceDetectionImg);

    //PlanarImage mergeImage = ImageProcessor.combineTwoImages(srcImg.toImageCV(), defaceImage.toMat(), 100);

    PlanarImage imageForVisualizing = DefacingUtil.rescaleForVisualizing(defaceImage, 100.0, 50.0);
    return imageForVisualizing;
  }

  public static PlanarImage faceDetection(PlanarImage srcImg) {
    ImageCV faceDetectionImg = new ImageCV();
    srcImg.toMat().copyTo(faceDetectionImg);

    MinMaxLocResult minMaxLocResult = ImageProcessor.findMinMaxValues(faceDetectionImg.toMat());

    // THRESHOLD
    Imgproc.threshold(faceDetectionImg.toImageCV(), faceDetectionImg.toMat(), 200, minMaxLocResult.maxVal, Imgproc.THRESH_BINARY);

    // ERODE
    Mat kernel = new Mat();
    int kernel_size = 3;
    Mat ones = Mat.ones( kernel_size, kernel_size, CvType.CV_32F);
    Core.multiply(ones, new Scalar(1/(double)(kernel_size*kernel_size)), kernel);
    Imgproc.erode(faceDetectionImg.toImageCV(), faceDetectionImg.toMat(), kernel);

    // FILL BLACK HOLE
    Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(30,30));
    Imgproc.morphologyEx(faceDetectionImg.toImageCV(), faceDetectionImg.toMat(), Imgproc.MORPH_CLOSE, kernel2);

    // RESCALE 8BIT
    faceDetectionImg = DefacingUtil.transformToByte(faceDetectionImg).toImageCV();

    // CANNY DETECT CONTOUR
    Imgproc.Canny(faceDetectionImg.toImageCV(), faceDetectionImg.toMat(), 240, 260);

    // DRAW BLACK RECT 1/3
    Rect rect = new Rect(0, faceDetectionImg.height()/3, faceDetectionImg.width(),faceDetectionImg.height());
    Imgproc.rectangle(faceDetectionImg.toImageCV(), rect, new Scalar(0,0,0), Imgproc.FILLED);

    return faceDetectionImg;
  }

  public static PlanarImage addRandPxlLine(PlanarImage srcImg, PlanarImage faceDetectImg) {
    ImageCV randPxlLineImg = new ImageCV();
    srcImg.toMat().copyTo(randPxlLineImg);

    // DRAW A LINE WITH RANDOM VALUE WHEN FACE DETECTED
    int marge = 10;
    // scan the image from left to right and bottom to top until the face is detected in Y
    for (int x = 0; x < faceDetectImg.width(); x++) {
      boolean faceDetected = false;
      int yPositionFaceDetected = 0;

      for (int y = faceDetectImg.height()-1; y > 0; y--) {
        double faceDetectPixelValue = faceDetectImg.toMat().get(y, x)[0];
        if(faceDetectPixelValue != 0.0 ) {
          faceDetected = true;
          yPositionFaceDetected = y;

          // Put random points before the first 10 lines of the face detection
          int yR = DefacingUtil.randomY(yPositionFaceDetected, yPositionFaceDetected+marge, 1);
          for (int yy = yPositionFaceDetected; yy < yR; yy++) {
            int yR2 = DefacingUtil.randomY(yPositionFaceDetected, yPositionFaceDetected+marge, 1);
            double randomPixelColor = srcImg.toMat().get(yR2, x)[0];
            randPxlLineImg.toMat().put(yy ,x, randomPixelColor);
          }
        }

        if(faceDetected) {
          int yRand = DefacingUtil.randomY(yPositionFaceDetected, yPositionFaceDetected+marge, 1);
          double randomPixelColor = srcImg.toMat().get(yRand, x)[0];
          randPxlLineImg.toMat().put(y ,x, randomPixelColor);
        }
      }
    }

    // BLUR THIS IMAGE
    ImageCV bluredImgRandPxlLine = new ImageCV();
    randPxlLineImg.toMat().copyTo(bluredImgRandPxlLine);
    Imgproc.blur(randPxlLineImg.toImageCV(), bluredImgRandPxlLine.toMat(), new Size(3,3), new Point(-3, -3), Core.BORDER_DEFAULT);


    // MERGE SRC AND BLURED IMAGE
    ImageCV newImg = new ImageCV();
    srcImg.toMat().copyTo(newImg);

    for (int x = 0; x < faceDetectImg.width(); x++) {
      for (int y = faceDetectImg.height() - 1; y > 0; y--) {
        if(randPxlLineImg.toMat().get(y,x)[0] != 0.0 ) {
          newImg.toMat().put(y ,x, bluredImgRandPxlLine.toMat().get(y,x)[0]);
        } else {
          newImg.toMat().put(y, x, srcImg.toMat().get(y,x)[0]);
        }
      }
    }
    return newImg;
  }
}
