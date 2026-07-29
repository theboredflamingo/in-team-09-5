import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.BackOffExecution;
public class T {
  public static void main(String[] a) {
    ExponentialBackOff b = new ExponentialBackOff(1000L, 2.0);
    b.setMaxElapsedTime(8000L);
    BackOffExecution ex = b.start();
    int i=0; long w; while ((w = ex.nextBackOff()) != BackOffExecution.STOP) { System.out.println("retry "+(++i)+" wait="+w+"ms"); }
    System.out.println("total retries="+i);
  }
}
