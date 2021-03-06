package application;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import quickfix.*;
import quickfix.field.MsgType;
import quickfix.field.Password;
import quickfix.field.Username;
import quickfix.fix43.Logon;
import quickfix.fix43.TradeCaptureReport;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;


public class QFJ extends MessageCracker implements Application {
    public static Logger log = Logger.getLogger(QFJ.class.getSimpleName());
    private SessionSettings sessionSettings;
    private SessionID priceSessionID = null;

    private boolean isReady = false;

    final private String client;

    public QFJ(String client, String configFile)  {
        this.client = client;
        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(configFile);

            sessionSettings = new SessionSettings(fileInputStream);
            MessageStoreFactory storeFactory = new MemoryStoreFactory();
            LogFactory logFactory = new ScreenLogFactory(true, true, false, false);
            //LogFactory logFactory = new FileLogFactory(sessionSettings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            SocketInitiator initiator = new SocketInitiator(this, storeFactory, sessionSettings, logFactory, messageFactory);
            initiator.start();

            this.waitForLogon();
        }
        catch (FileNotFoundException e) {
            log.error(client + " " + "[QFJ003] ", e);
        }
        catch (ConfigError configError) {
            log.error(client + " " + "[QFJ002] ", configError);
        } finally  {
            if (fileInputStream != null) {
                try  {
                    fileInputStream.close();
                }
                catch (Exception e) {
                    log.error(client + " " + "[QFJ008] ", e);
                }
            }
        }

    }

    public void fromAdmin(Message arg0, SessionID arg1) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        //System.out.println("Successfully called fromAdmin for sessionId : " + arg0);
    }
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }
    public void onCreate(SessionID sessionID) {
        //System.out.println("onCreate: " + sessionID);
        try {
            Properties prop = sessionSettings.getSessionProperties(sessionID);
            if (prop.getProperty("SessionName").equals("Price")) {
                priceSessionID = sessionID;
            }
        } catch (ConfigError configError) {
            log.error(client + " " + "[QFJ007] ", configError);
        }
    }
    public void onLogon(SessionID arg0) {
        //System.out.println("Viewer logged in");
        isReady = true;
    }
    public void onLogout(SessionID arg0) {
        //System.out.println("Successfully logged out for sessionId : " + arg0);
    }
    public void toAdmin(Message message, SessionID sessionId) {
        //System.out.println("toAdmin: " + message + " -- " + sessionId);

        // if it's a 35=A message, add username and password from the sessionSettings
        try {
            if (Logon.MSGTYPE.equals(message.getHeader().getString(MsgType.FIELD))) {
                message.setField(new Username(sessionSettings.getSessionProperties(sessionId).getProperty("Username")));
                message.setField(new Password(sessionSettings.getSessionProperties(sessionId).getProperty("Password")));
            }
        }
        catch ( FieldNotFound fnf ) {
            log.error(client + " " + "[QFJ006] ", fnf);
        }
        catch (ConfigError configError) {
            log.error(client + " " + "[QFJ005] ", configError);
        }
    }
    public void toApp(Message arg0, SessionID arg1) throws DoNotSend {
        //Empty
    }


    public void onMessage(Message message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        // empty, this is needed so that messages don't get rejected
    }

    public void sendTradeCaptureReport(Document doc) {
        TradeCaptureReport message = new TradeCaptureReport();
        try {
            NodeList nodeList = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i) instanceof Element) {
                    Element element = (Element) nodeList.item(i);
                    if (!element.getTextContent().equals("")) {
                        message.setField(new StringField(Integer.valueOf(element.getAttribute("tag")), element.getTextContent()));
                    }
                }
            }
        }
        catch (Exception e) {
            log.error(client + " " + "[QFJ001] ", e);
        }

        try {
            Session.sendToTarget(message, priceSessionID);
        }
        catch (SessionNotFound e) {
            log.error(client + " " + "[QFJ004] ", e);
        }
    }

    public void waitForLogon() {
        while (!isReady) {
            try {
                log.info(client + " " + "Waiting for FIX Logon, will try again in 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

