import application.STPApp;
import org.w3c.dom.Document;

public class STPClient {
    public static void main(String args[]) {
        STPApp stpApp = new STPApp(args[0], new STPApp.CustomModification() {
            @Override
            public Document customWork(Document document) {
                return document;
            }
        }) ;

        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            }
            catch (InterruptedException ie){
                System.out.println("STPClient has been interrupted.");
            }
        }

    }
}
