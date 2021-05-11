import java.util.Random;

public class DefacingUtil {

  public static int randomY(int maxY, int originY, int bound) {
    Random random = new Random();
    int newY = random.nextInt(bound + 1)  + originY; // [0...5]  + 10 = [10...15]
    if(newY > maxY) {
      return maxY;
    } else {
      return newY;
    }
  }
}
