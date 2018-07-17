/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import hal.tokyo.rd4c.bocco4j.BoccoAPI;
import hal.tokyo.rd4c.nfc.NFCReader;
import hal.tokyo.rd4c.speech2text.MicroPhone;
import hal.tokyo.rd4c.speech2text.Speaker;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author gn5r
 */
public class Main {

    /* 正常値の値 */
    public static final int FLAG_OK = 100;
    public static final int ERROR = -1;
    /* mode:CardSet, CardScan, RecVoice */
    private static String mode = "";
    private static String sendText = new String();
    private static NFCReader nfcReader = new NFCReader();
    private static String setupSound = "setup/setup.wav";
    private static MicroPhone microPhone;
    private static Speaker speaker;
    /* BOCCOと接続用String */
    private static String GOOGLE_API_KEY = "";
    /* BoocoAPI(String APIKey, String EMAIL, String PASSWORD) */
    public static BoccoAPI boccoApi;
    public static TextMessage textMessage = new TextMessage();
    /* 変換後文字列を格納するためのファイル名を格納 */
    public static String recFileName = new String();
    /* GPIO */
    private static GpioPinDigitalInput startSE, event1SE, event2SE, endingSE;
    private static GpioPinDigitalOutput startSELED, event1SELED, event2SELED, endingSELED;
    private static GpioController gpio;

    /* メイン */
    public static void main(String[] args) throws Exception {
        /* ステージ回数0-3 */
        int stage = 0;

        /*初期化 */
        init(args);

        while (true) {
            Thread.sleep(500);
        }

    }

    /* 初期化 */
    public static void init(String[] args) throws Exception {


        /* args[0]:BOCCOAPI args[1]:Email args[2]:PassWord */
        boccoApi = new BoccoAPI(args[0], args[1], args[2]);
        GOOGLE_API_KEY = args[3];
        microPhone = new MicroPhone();
        speaker = new Speaker();

        microPhone.init();
        mode = "CardSet";

        /* 起動音を鳴らす */
        speaker.openFile(setupSound);
        speaker.playSE();
        speaker.stopSE();

        gpio = GpioFactory.getInstance();

        /*    各SEボタンとLED    */
        startSE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_UP);
        startSE.setShutdownOptions(true);

        startSELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "startSE", PinState.LOW);
        startSELED.setShutdownOptions(true, PinState.LOW);

        event1SE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_UP);
        event1SE.setShutdownOptions(true);

        event1SELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "event1SE", PinState.LOW);
        event1SELED.setShutdownOptions(true, PinState.LOW);

        event2SE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_26, PinPullResistance.PULL_UP);
        event2SE.setShutdownOptions(true);

        event2SELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27, "Sevent2SE", PinState.LOW);
        event2SELED.setShutdownOptions(true, PinState.LOW);

        endingSE = gpio.provisionDigitalInputPin(RaspiPin.GPIO_28, PinPullResistance.PULL_UP);
        endingSE.setShutdownOptions(true);

        endingSELED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29, "endingSE", PinState.LOW);
        endingSELED.setShutdownOptions(true, PinState.LOW);

        /* BOCCOの接続が確立できた場合 */
        if (boccoApi.createSessions() == true) {
            boccoApi.getFirstRooID();
            boccoApi.postMessage(textMessage.readText(TextMessage.SESSION_OK));
        }

        /* ファイル作成 */
        recFileName = createFile();
    }

    /* どのカードをセットするかBOCCOに喋ってもらう */
    public static void cardSet(int stage) throws Exception {

        switch (stage) {
            /* 一枚目 */
            case 0:
                sendText = textMessage.readText(TextMessage.FIRST_SET);
                break;
            /* 二枚目,三枚目 */
            case 1:
            case 2:
                sendText = textMessage.readText(TextMessage.NOT_FIRST);
                break;
            /* 四枚目 */
            case 3:
                sendText = textMessage.readText(TextMessage.LAST_SET);
                break;
            /* 例外処理, 終了 */
            default:
                System.out.println("想定外の数値が入力、終了します。");
                return;
        }

        /* ステップ押下の誘導メッセージ */
        sendText += textMessage.readText(TextMessage.CARD_STEP);
        boccoApi.postMessage(sendText);
    }

    /* NFCリーダーでデータを読み込む.   ERROR:エラー   0 - 14:正常 */
    public static int cardScan() throws InterruptedException {
        int BGMNum = ERROR;
        /* 正常な値が読み取れるまで */
        while (BGMNum == ERROR) {
            BGMNum = nfcReader.readBGMNum();
        }
        return BGMNum;
    }

    /*  */
    public static int CardJudge(int stage, int BGMNum) {
        /* 異常値：ERROR, 正常値：FLAG_OK */
        int flag = ERROR;
        switch (stage) {
            /* 一枚目 */
            case 0:
                if (0 <= BGMNum && BGMNum < 3) {
                    flag = FLAG_OK;
                }
                break;
            /* 二枚目,三枚目 */
            case 1:
            case 2:
                if (3 <= BGMNum && BGMNum < 12) {
                    flag = FLAG_OK;
                }
                break;
            /* 四枚目 */
            case 3:
                if (12 <= BGMNum && BGMNum < 15) {
                    flag = FLAG_OK;
                }
                break;
            /* 例外処理 */
            default:
                System.out.println("想定外の数値が入力されました。");
                break;
        }
        return flag;
    }

    /* 変換後文字列を格納するためのtxtファイル */
    private static String createFile() throws IOException {
        String strDate = "Story.txt";
        File recorFile = new File(strDate);

        try {
            if (recorFile.createNewFile()) {
                System.out.println("ファイルの作成に成功しました。");
            } else {
                System.out.println("既に同じ名前のファイルがあります。");
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        return strDate;
    }
}
